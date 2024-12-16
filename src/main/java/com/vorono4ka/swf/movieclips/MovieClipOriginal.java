package com.vorono4ka.swf.movieclips;

import com.supercell.swf.FBMovieClip;
import com.supercell.swf.FBMovieClipFrame;
import com.supercell.swf.FBResources;
import com.vorono4ka.math.MathHelper;
import com.vorono4ka.math.Rect;
import com.vorono4ka.streams.ByteStream;
import com.vorono4ka.swf.DisplayObjectOriginal;
import com.vorono4ka.swf.Savable;
import com.vorono4ka.swf.SupercellSWF;
import com.vorono4ka.swf.Tag;
import com.vorono4ka.swf.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MovieClipOriginal extends DisplayObjectOriginal {
    private static final Logger LOGGER = LoggerFactory.getLogger(MovieClipOriginal.class);

    private Tag tag;

    private String exportName;
    private byte fps;
    /**
     * By default, true in the game.
     */
    private boolean customPropertyBoolean = true;
    private List<MovieClipChild> children = new ArrayList<>();
    private List<MovieClipFrame> frames;
    private short matrixBankIndex;
    private Rect scalingGrid;

    private DisplayObjectOriginal[] timelineChildren;

    public MovieClipOriginal() {
    }

    public MovieClipOriginal(FBMovieClip fb, FBResources resources) {
        id = fb.id();
        exportName = resources.strings(fb.exportNameRefId());
        fps = (byte) fb.fps();
        customPropertyBoolean = (fb.property() != 0);
        children = new ArrayList<>(fb.childIdsLength());
        for (int i = 0; i < fb.childIdsLength(); i++) {
            children.add(new MovieClipChild(fb.childIds(i), (byte) fb.childBlends(i), fb.childNameRefIdsLength() > 0 ? resources.strings(fb.childNameRefIds(i)) : null));
        }
        frames = new ArrayList<>(fb.framesLength());
        int frameElementOffset = fb.frameElementOffset() / 3;
        for (int i = 0; i < fb.framesLength(); i++) {
            FBMovieClipFrame fbFrame = fb.frames(i);
            MovieClipFrame frame = new MovieClipFrame(fbFrame, resources, frameElementOffset);
            frameElementOffset += frame.getElementCount();
            frames.add(frame);
        }
        matrixBankIndex = (short) fb.matrixBankIndex();
        int scalingGridIndex = fb.scalingGridIndex();
        if (scalingGridIndex != -1) {
            scalingGrid = new Rect(resources.scalingGrids(scalingGridIndex));
        }

        tag = determineTag();
    }

    public int load(ByteStream stream, Tag tag, String filename) throws LoadingFaultException, UnsupportedCustomPropertyException {
        this.tag = tag;

        this.id = stream.readShort();
        this.fps = (byte) stream.readUnsignedChar();
        // *(a1 + 54) = *(a1 + 54) & 0xFF80 | ZN12SupercellSWF16readUnsignedCharEv(a2) & 0x7F;

        int frameCount = stream.readShort();
        this.frames = new ArrayList<>(frameCount);
        for (int i = 0; i < frameCount; i++) {
            this.frames.add(new MovieClipFrame());
        }

        if (tag.hasCustomProperties()) {
            int propertyCount = stream.readUnsignedChar();
            for (int i = 0; i < propertyCount; i++) {
                int propertyType = stream.readUnsignedChar();
                switch (propertyType) {
                    case 0 -> {
                        this.customPropertyBoolean = stream.readBoolean();
                        // *(a1 + 54) = *(a1 + 54) & 0xFF7F | (unknown ? 128 : 0);
                    }
                    default ->
                        throw new UnsupportedCustomPropertyException("Unsupported custom property type: " + propertyType);
                }
            }
        }

        short[] frameElements = null;
        switch (Objects.requireNonNull(tag)) {
            case MOVIE_CLIP -> {
            }  // TAG_MOVIE_CLIP no longer supported
            case MOVIE_CLIP_4 -> {
                try {
                    throw new UnsupportedTagException("TAG_MOVIE_CLIP_4 no longer supported\n");
                } catch (UnsupportedTagException exception) {
                    LOGGER.error(exception.getMessage(), exception);
                }
            }
            default -> {
                int elementCount = stream.readInt();
                frameElements = stream.readShortArray(elementCount * 3);
            }
        }

        int childCount = stream.readShort();
        short[] childIds = stream.readShortArray(childCount);

        byte[] childBlends;
        if (tag.hasBlendData()) {
            childBlends = stream.readByteArray(childCount);
        } else {
            childBlends = new byte[childCount];
        }

        String[] childNames = new String[childCount];
        for (int i = 0; i < childCount; i++) {
            childNames[i] = stream.readAscii();
        }

        for (int i = 0; i < childCount; i++) {
            children.add(new MovieClipChild(childIds[i], childBlends[i], childNames[i]));
        }

        int loadedCommands = 0;
        int usedElements = 0;

        while (true) {
            int frameTag = stream.readUnsignedChar();
            int length = stream.readInt();

            if (length < 0) {
                throw new NegativeTagLengthException(String.format("Negative tag length in MovieClip. Tag %d, %s", frameTag, filename));
            }

            Tag tagValue = Tag.values()[frameTag];
            switch (tagValue) {
                case EOF -> {
                    return this.id;
                }
                case MOVIE_CLIP_FRAME,
                     MOVIE_CLIP_FRAME_2 -> {  // TAG_MOVIE_CLIP_FRAME no longer supported
                    MovieClipFrame frame = this.frames.get(loadedCommands++);
                    int elementCount = frame.load(stream, tagValue);

                    if (tagValue != Tag.MOVIE_CLIP_FRAME) {
                        if (frameElements == null) {
                            throw new IllegalStateException("Frame elements cannot be null.");
                        }

                        List<MovieClipFrameElement> elements = new ArrayList<>(elementCount);
                        for (int i = 0; i < elementCount; i++) {
                            elements.add(new MovieClipFrameElement(frameElements[usedElements * 3] & 0xFFFF, frameElements[usedElements * 3 + 1] & 0xFFFF, frameElements[usedElements * 3 + 2] & 0xFFFF));

                            usedElements++;
                        }
                        frame.setElements(elements);
                    }
                }
                case SCALING_GRID -> {
                    if (this.scalingGrid != null) {
                        throw new LoadingFaultException("multiple scaling grids");
                    }

                    float left = stream.readTwip();
                    float top = stream.readTwip();
                    float width = stream.readTwip();
                    float height = stream.readTwip();
                    float right = MathHelper.round(left + width, 2);
                    float bottom = MathHelper.round(top + height, 2);

                    this.scalingGrid = new Rect(left, top, right, bottom);
                }
                case
                    MATRIX_BANK_INDEX -> // (a1 + 54) & 0x80FF | ((ZN12SupercellSWF16readUnsignedCharEv(a2) & 0x7F) << 8);
                    this.matrixBankIndex = (short) stream.readUnsignedChar();
                default -> {
                    try {
                        throw new UnsupportedTagException(String.format("Unknown tag %d in MovieClip, %s", frameTag, filename));
                    } catch (UnsupportedTagException exception) {
                        LOGGER.error(exception.getMessage(), exception);
                    }
                }
            }
        }
    }

    @Override
    public void save(ByteStream stream) {
        stream.writeShort(this.id);
        stream.writeUnsignedChar(this.fps);

        stream.writeShort(this.frames.size());

        if (tag.hasCustomProperties()) {
            stream.writeUnsignedChar(1);  // custom property count
            {
                stream.writeUnsignedChar(0);  // custom property type
                {  // custom property 0 data
                    stream.writeBoolean(this.customPropertyBoolean);
                }
            }
        }

        if (tag != Tag.MOVIE_CLIP && tag != Tag.MOVIE_CLIP_4) {
            List<MovieClipFrameElement> frameElements = new ArrayList<>();
            for (MovieClipFrame frame : this.frames) {
                frameElements.addAll(frame.getElements());
            }

            stream.writeInt(frameElements.size());
            for (MovieClipFrameElement element : frameElements) {
                stream.writeShort(element.childIndex());
                stream.writeShort(element.matrixIndex());
                stream.writeShort(element.colorTransformIndex());
            }
        }

        stream.writeShort(this.children.size());
        for (MovieClipChild child : this.children) {
            stream.writeShort(child.id());
        }

        if (this.tag.hasBlendData()) {
            for (MovieClipChild child : this.children) {
                stream.writeUnsignedChar(child.blend());
            }
        }

        for (MovieClipChild child : this.children) {
            stream.writeAscii(child.name());
        }

        List<Savable> savableObjects = getSavableObjects();

        for (Savable savable : savableObjects) {
            stream.writeSavable(savable);
        }

        stream.writeBlock(Tag.EOF, null);
    }

    @Override
    public Tag getTag() {
        return tag;
    }

    public DisplayObjectOriginal[] createTimelineChildren(SupercellSWF swf) throws UnableToFindObjectException {
        if (this.timelineChildren == null) {
            this.timelineChildren = new DisplayObjectOriginal[this.children.size()];
            for (int i = 0; i < this.children.size(); i++) {
                this.timelineChildren[i] = swf.getOriginalDisplayObject(this.children.get(i).id() & 0xFFFF, this.exportName);
            }
        }

        return this.timelineChildren;
    }

    public int getFps() {
        return fps;
    }

    public List<MovieClipFrame> getFrames() {
        return frames;
    }

    public List<MovieClipChild> getChildren() {
        return children;
    }

    public Rect getScalingGrid() {
        return scalingGrid;
    }

    public int getMatrixBankIndex() {
        return matrixBankIndex;
    }

    public void setMatrixBankIndex(short matrixBankIndex) {
        this.matrixBankIndex = matrixBankIndex;
    }

    public DisplayObjectOriginal[] getTimelineChildren() {
        return timelineChildren;
    }

    public String getExportName() {
        return exportName;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    @Override
    public String toString() {
        return "MovieClipOriginal{" + "tag=" + tag + ", id=" + id + ", exportName='" + exportName + '\'' + ", fps=" + fps + ", customPropertyBoolean=" + customPropertyBoolean + ", children=" + children + ", frames=" + frames + ", matrixBankIndex=" + matrixBankIndex + ", scalingGrid=" + scalingGrid + ", children=" + Arrays.toString(timelineChildren) + '}';
    }

    private Tag determineTag() {
        if (!customPropertyBoolean) {
            return Tag.MOVIE_CLIP_6;
        }

        if (!children.isEmpty()) {
            boolean hasNotZero = false;
            for (MovieClipChild child : children) {
                if (child.blend() != 0) {
                    hasNotZero = true;
                    break;
                }
            }

            if (hasNotZero) {
                return Tag.MOVIE_CLIP_5;
            }
        }

        return Tag.MOVIE_CLIP_2;
    }

    private List<Savable> getSavableObjects() {
        List<Savable> savableObjects = new ArrayList<>(this.frames);

        if (this.scalingGrid != null) {
            Savable scalingGridObject = new Savable() {
                @Override
                public void save(ByteStream stream) {
                    stream.writeTwip(scalingGrid.getLeft());
                    stream.writeTwip(scalingGrid.getTop());
                    stream.writeTwip(scalingGrid.getWidth());
                    stream.writeTwip(scalingGrid.getHeight());
                }

                @Override
                public Tag getTag() {
                    return Tag.SCALING_GRID;
                }
            };
            savableObjects.add(scalingGridObject);
        }

        if (this.matrixBankIndex != 0) {
            Savable matrixBankIndexObject = new Savable() {
                @Override
                public void save(ByteStream stream) {
                    stream.writeUnsignedChar(matrixBankIndex);
                }

                @Override
                public Tag getTag() {
                    return Tag.MATRIX_BANK_INDEX;
                }
            };
            savableObjects.add(matrixBankIndexObject);
        }
        return savableObjects;
    }
}
