package com.vorono4ka.swf.movieclips;

import com.supercell.swf.FBMovieClip;
import com.supercell.swf.FBMovieClipFrame;
import com.supercell.swf.FBResources;
import com.vorono4ka.math.MathHelper;
import com.vorono4ka.math.Rect;
import com.vorono4ka.streams.ByteStream;
import com.vorono4ka.swf.DisplayObjectOriginal;
import com.vorono4ka.swf.SupercellSWF;
import com.vorono4ka.swf.Tag;
import com.vorono4ka.swf.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class MovieClipOriginal extends DisplayObjectOriginal {
    private static final Logger LOGGER = LoggerFactory.getLogger(MovieClipOriginal.class);

    private transient Tag tag;

    private int id;

    private String exportName;

    private byte fps;
    private boolean customPropertyBoolean;
    private short[] childIds = new short[0];
    private byte[] childBlends = new byte[0];
    private String[] childNames = new String[0];
    private List<MovieClipFrame> frames;
    private short matrixBankIndex;
    private Rect scalingGrid;

    private DisplayObjectOriginal[] children;

    public MovieClipOriginal() {
    }

    public MovieClipOriginal(FBMovieClip fb, FBResources resources) {
        tag = Tag.MOVIE_CLIP_6;
        id = fb.id();
        exportName = resources.strings(fb.exportNameRefId());
        fps = (byte) fb.fps();
        customPropertyBoolean = (fb.property() != 0);
        childIds = new short[fb.childIdsLength()];
        for (int i = 0; i < childIds.length; i++) {
            childIds[i] = (short) fb.childIds(i);
        }
        childNames = new String[fb.childNameRefIdsLength()];
        for (int i = 0; i < childNames.length; i++) {
            childNames[i] = resources.strings(fb.childNameRefIds(i));
        }
        childBlends = new byte[fb.childBlendsLength()];
        for (int i = 0; i < childBlends.length; i++) {
            childBlends[i] = (byte) fb.childBlends(i);
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

        int childrenCount = stream.readShort();
        this.childIds = stream.readShortArray(childrenCount);

        if (tag.hasBlendData()) {
            this.childBlends = stream.readByteArray(childrenCount);
        } else {
            this.childBlends = new byte[childrenCount];
            Arrays.fill(this.childBlends, (byte) 0);
        }

        this.childNames = new String[childrenCount];
        for (int i = 0; i < childrenCount; i++) {
            this.childNames[i] = stream.readAscii();
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

                        MovieClipFrameElement[] elements = new MovieClipFrameElement[elementCount];
                        for (int i = 0; i < elementCount; i++) {
                            elements[i] = new MovieClipFrameElement(frameElements[usedElements * 3] & 0xFFFF, frameElements[usedElements * 3 + 1] & 0xFFFF, frameElements[usedElements * 3 + 2] & 0xFFFF);

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

        if (tag != Tag.MOVIE_CLIP && tag != Tag.MOVIE_CLIP_4) {
            List<MovieClipFrameElement> frameElements = new ArrayList<>();
            for (MovieClipFrame frame : this.frames) {
                Collections.addAll(frameElements, frame.getElements());
            }

            stream.writeInt(frameElements.size());
            for (MovieClipFrameElement element : frameElements) {
                stream.writeShort(element.childIndex());
                stream.writeShort(element.matrixIndex());
                stream.writeShort(element.colorTransformIndex());
            }
        }

        stream.writeShort(this.childIds.length);
        for (short id : this.childIds) {
            stream.writeShort(id);
        }

        if (this.tag == Tag.MOVIE_CLIP_3 || this.tag == Tag.MOVIE_CLIP_5) {
            for (byte blend : this.childBlends) {
                stream.writeUnsignedChar(blend);
            }
        }

        for (String name : this.childNames) {
            stream.writeAscii(name);
        }

        for (MovieClipFrame frame : this.frames) {
            stream.writeBlock(Tag.MOVIE_CLIP_FRAME_2, frame::save);
        }

        if (this.scalingGrid != null) {
            stream.writeBlock(Tag.SCALING_GRID, blockStream -> {
                blockStream.writeTwip(this.scalingGrid.getLeft());
                blockStream.writeTwip(this.scalingGrid.getTop());
                blockStream.writeTwip(this.scalingGrid.getWidth());
                blockStream.writeTwip(this.scalingGrid.getHeight());
            });
        }

        if (this.matrixBankIndex != 0) {
            stream.writeBlock(Tag.MATRIX_BANK_INDEX, blockStream -> blockStream.writeUnsignedChar(this.matrixBankIndex));
        }

        stream.writeBlock(Tag.EOF, ignored -> {
        });
    }

    public void createTimelineChildren(SupercellSWF swf) throws UnableToFindObjectException {
        if (this.children == null) {
            this.children = new DisplayObjectOriginal[this.childIds.length];
            for (int i = 0; i < this.childIds.length; i++) {
                this.children[i] = swf.getOriginalDisplayObject(this.childIds[i] & 0xFFFF, this.exportName);
            }
        }
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public Tag getTag() {
        return tag;
    }

    public int getFps() {
        return fps;
    }

    public List<MovieClipFrame> getFrames() {
        return frames;
    }

    public int getChildCount() {
        return childIds != null ? childIds.length : 0;
    }

    public byte[] getChildBlends() {
        return childBlends;
    }

    public short[] getChildIds() {
        return childIds;
    }

    public String[] getChildNames() {
        return childNames;
    }

    public Rect getScalingGrid() {
        return scalingGrid;
    }

    public int getMatrixBankIndex() {
        return matrixBankIndex;
    }

    public DisplayObjectOriginal[] getChildren() {
        return children;
    }

    public String getExportName() {
        return exportName;
    }

    public void setExportName(String exportName) {
        this.exportName = exportName;
    }

    @Override
    public String toString() {
        return "MovieClipOriginal{" + "tag=" + tag + ", id=" + id + ", exportName='" + exportName + '\'' + ", fps=" + fps + ", customPropertyBoolean=" + customPropertyBoolean + ", childIds=" + Arrays.toString(childIds) + ", childBlends=" + Arrays.toString(childBlends) + ", childNames=" + Arrays.toString(childNames) + ", frames=" + frames + ", matrixBankIndex=" + matrixBankIndex + ", scalingGrid=" + scalingGrid + ", children=" + Arrays.toString(children) + '}';
    }
}
