package dev.donutquine.swf.movieclips;

import com.supercell.swf.FBMovieClip;
import com.supercell.swf.FBMovieClipFrame;
import com.supercell.swf.FBResources;
import dev.donutquine.math.MathHelper;
import dev.donutquine.math.Rect;
import dev.donutquine.streams.ByteStream;
import dev.donutquine.swf.*;
import dev.donutquine.swf.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MovieClipOriginal extends DisplayObjectOriginal {
    private static final Logger LOGGER = LoggerFactory.getLogger(MovieClipOriginal.class);

    private Tag tag;

    private String exportName;
    private int fps;
    /**
     * By default, true in the game.
     */
    private boolean customPropertyBoolean = true;
    private List<MovieClipChild> children;
    private List<MovieClipFrame> frames;
    private int matrixBankIndex;
    private ScMatrixBank inlineMatrixBank;
    private Rect scalingGrid;

    private DisplayObjectOriginal[] timelineChildren;

    public MovieClipOriginal() { }

    public MovieClipOriginal(FBMovieClip fb, FBResources resources, ByteBuffer frameDataBuffer) {
        id = fb.id();
        exportName = fb.exportNameRefId() != 0 ? resources.strings(fb.exportNameRefId()) : null;
        fps = fb.fps();
        customPropertyBoolean = fb.property() != 0;
        children = new ArrayList<>(fb.childIdsLength());
        for (int i = 0; i < fb.childIdsLength(); i++) {
            children.add(new MovieClipChild(fb.childIds(i), fb.childBlends(i), fb.childNameRefIdsLength() != 0 ? resources.strings(fb.childNameRefIds(i)) : null));
        }
        frames = new ArrayList<>(fb.framesLength());
        int frameElementOffset = fb.frameElementOffset() / 3;
        if (fb.framesLength() > 0) {
            for (int i = 0; i < fb.framesLength(); i++) {
                FBMovieClipFrame fbFrame = fb.frames(i);
                MovieClipFrame frame = new MovieClipFrame(fbFrame, resources, frameElementOffset);
                frameElementOffset += frame.getElementCount();
                frames.add(frame);
            }
        } else {
            throw new IllegalStateException("Movie clip frame must have at least one frame");
        }

        int frameDataOffset = fb.frameDataOffset();
        // FIXME: needed for compatibility between new and old structure, remove later
        if (frameDataOffset == -1) {
            frameDataOffset = fb.frameDataOffset0();
        }

        // TODO: make an assert for detecting versions with short_frames

        if (frameDataOffset != -1) {
            ExternalMovieClipFrameElementDecoder decoder = new ExternalMovieClipFrameElementDecoder();
            List<List<MovieClipFrameElement>> frameElements = decoder.decodeMovieClipFrames(frameDataBuffer, frameDataOffset);
            for (int i = 0; i < frames.size(); i++) {
                frames.get(i).setElements(frameElements.get(i));
            }
        }

        matrixBankIndex = fb.matrixBankIndex();
        int scalingGridIndex = fb.scalingGridIndex();
        if (scalingGridIndex != -1) {
            scalingGrid = new Rect(resources.scalingGrids(scalingGridIndex));
        }

        tag = determineTag();
    }

    public MovieClipOriginal(List<MovieClipChild> children, List<MovieClipFrame> frames, int fps, int matrixBankIndex, Rect scalingGrid, boolean customPropertyBoolean) {
        validateFps(fps);
        validateMatrixBankIndex(matrixBankIndex);

        this.children = new ArrayList<>(children);
        this.frames = new ArrayList<>(frames);
        this.fps = fps;
        this.matrixBankIndex = matrixBankIndex;
        this.scalingGrid = scalingGrid;
        this.customPropertyBoolean = customPropertyBoolean;

        this.tag = determineTag();
    }

    public int load(ByteStream stream, Tag tag, String filename) throws LoadingFaultException, UnsupportedCustomPropertyException {
        this.tag = tag;

        this.id = stream.readShort();
        this.fps = stream.readUnsignedChar();
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
        List<MovieClipFrameElement> newElements = null;
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
            case MOVIE_CLIP_7 -> {
                int elementCount = stream.readInt();
                newElements = new ArrayList<>(elementCount);
                for (int i = 0; i < elementCount; i++) {
                    int childIndex = stream.readShort() & 0xFFFF;
                    int matrixIndex = stream.readInt();
                    int colorIndex = stream.readInt();
                    newElements.add(new MovieClipFrameElement(childIndex, matrixIndex, colorIndex));
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

        children = new ArrayList<>(childCount);
        for (int i = 0; i < childCount; i++) {
            children.add(new MovieClipChild(childIds[i], childBlends[i], childNames[i]));
        }

        int loadedCommands = 0;
        int loadedMatrices = 0;
        int loadedColorTransforms = 0;
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

                        List<MovieClipFrameElement> elements;
                        if (newElements != null) {
                            elements = newElements.subList(usedElements, usedElements + elementCount);
                            usedElements += elementCount;
                        } else {
                            elements = new ArrayList<>(elementCount);
                            for (int i = 0; i < elementCount; i++) {
                                int childIndex = frameElements[usedElements * 3] & 0xFFFF;
                                int matrixIndex = frameElements[usedElements * 3 + 1] & 0xFFFF;
                                int colorTransformIndex = frameElements[usedElements * 3 + 2] & 0xFFFF;

                                elements.add(new MovieClipFrameElement(childIndex, matrixIndex, colorTransformIndex));

                                usedElements++;
                            }
                        }

                        frame.setElements(elements);
                    }
                }
                case MATRIX, MATRIX_PRECISE -> {
                    if (this.inlineMatrixBank == null) {
                        throw new IllegalStateException(String.format("Matrix tag %d outside inline bank in MovieClip", tagValue));  // , filename
                    }

                    this.inlineMatrixBank.getMatrix(loadedMatrices++).load(stream, tagValue == Tag.MATRIX_PRECISE);
                }
                case COLOR_TRANSFORM -> {
                    if (this.inlineMatrixBank == null) {
                        throw new IllegalStateException(String.format("ColorTransform tag %d outside inline bank in MovieClip", tagValue));  // , filename
                    }


                    this.inlineMatrixBank.getColorTransform(loadedColorTransforms++).read(stream);
                }
                case SCALING_GRID -> {
                    if (this.scalingGrid != null) {
                        throw new LoadingFaultException("multiple scaling grids, id=" + this.id);
                    }

                    float left = stream.readTwip();
                    float top = stream.readTwip();
                    float width = stream.readTwip();
                    float height = stream.readTwip();
                    float right = MathHelper.round(left + width, 2);
                    float bottom = MathHelper.round(top + height, 2);

                    this.scalingGrid = new Rect(left, top, right, bottom);
                }
                case MATRIX_BANK_INDEX -> // (a1 + 54) & 0x80FF | ((ZN12SupercellSWF16readUnsignedCharEv(a2) & 0x7F) << 8);
                    this.matrixBankIndex = stream.readUnsignedChar();
                case MOVIE_CLIP_INLINE_MATRIX_BANK -> {
                    int matrixCount = stream.readShort();
                    int colorTransformCount = stream.readShort();

                    this.inlineMatrixBank = new ScMatrixBank(matrixCount, colorTransformCount);
                    
                    loadedMatrices = 0;
                    loadedColorTransforms = 0;
                }
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

        if (tag == Tag.MOVIE_CLIP_7) {
            List<MovieClipFrameElement> frameElements = collectFrameElements();

            stream.writeInt(frameElements.size());
            for (MovieClipFrameElement element : frameElements) {
                stream.writeShort(element.childIndex());
                stream.writeInt(element.matrixIndex());
                stream.writeInt(element.colorTransformIndex());
            }
        } else if (tag != Tag.MOVIE_CLIP && tag != Tag.MOVIE_CLIP_4) {
            List<MovieClipFrameElement> frameElements = collectFrameElements();

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

    public void setChildren(List<MovieClipChild> children) {
        this.children = children;
    }

    public List<MovieClipChild> getChildren() {
        return children;
    }

    public Rect getScalingGrid() {
        return scalingGrid;
    }

    public void setScalingGrid(Rect scalingGrid) {
        this.scalingGrid = scalingGrid;
    }

    public int getMatrixBankIndex() {
        return matrixBankIndex;
    }

    public void setMatrixBankIndex(short matrixBankIndex) {
        if (this.inlineMatrixBank != null) {
            throw new IllegalStateException("You cannot set matrix bank index until you have an inlined matrix bank");
        }

        this.matrixBankIndex = matrixBankIndex;
    }

    public ScMatrixBank getInlineMatrixBank() {
        return inlineMatrixBank;
    }

    public void setInlineMatrixBank(ScMatrixBank inlineMatrixBank) {
        if (this.matrixBankIndex != 0) {
            throw new IllegalStateException("You cannot set inline matrix bank until you have non-zero matrix bank index");
        }

        this.inlineMatrixBank = inlineMatrixBank;
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
        // Note: check if frame elements in a new format (-1 instead of 0xFFFF for null index)
        for (MovieClipFrame frame : this.frames) {
            for (MovieClipFrameElement frameElement : frame.getElements()) {
                if (frameElement.matrixIndex() == -1 || frameElement.matrixIndex() > ScMatrixBank.MAX_MATRIX_CAPACITY || 
                    frameElement.colorTransformIndex() == -1 || frameElement.colorTransformIndex() > ScMatrixBank.MAX_COLOR_CAPACITY) {
                    return Tag.MOVIE_CLIP_7;
                }
            }
        }

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

    private List<MovieClipFrameElement> collectFrameElements() {
        List<MovieClipFrameElement> frameElements = new ArrayList<>();
        for (MovieClipFrame frame : this.frames) {
            frameElements.addAll(frame.getElements());
        }

        return frameElements;
    }

    private List<Savable> getSavableObjects() {
        List<Savable> objects = new ArrayList<>(this.frames);

        if (this.scalingGrid != null) {
            objects.add(new ScalingGridObject(scalingGrid));
        }

        if (this.inlineMatrixBank != null) {
            List<Matrix2x3> matrices = this.inlineMatrixBank.getMatrices();
            if (matrices.size() > ScMatrixBank.MAX_MATRIX_CAPACITY) {
                matrices = new ArrayList<>(matrices);
                List<Matrix2x3> toTruncate = matrices.subList(ScMatrixBank.MAX_MATRIX_CAPACITY, matrices.size());
                // Note: it is completely fine to truncate 1 matrix from SC2-loaded file with "compressed" matrices
                LOGGER.warn("Too many matrices for inline matrix bank. Truncated {} matrices.", toTruncate.size());
                toTruncate.clear();
            }

            List<ColorTransform> colorTransforms = this.inlineMatrixBank.getColorTransforms();
            if (colorTransforms.size() > ScMatrixBank.MAX_COLOR_CAPACITY) {
                colorTransforms = new ArrayList<>(colorTransforms);
                List<ColorTransform> toTruncate = colorTransforms.subList(ScMatrixBank.MAX_COLOR_CAPACITY, colorTransforms.size());
                LOGGER.warn("Too many colors for inline matrix bank. Truncated {} colors.", toTruncate.size());
                toTruncate.clear();
            }

            objects.add(new InlineMatrixBankInfo(matrices.size(), colorTransforms.size()));
            objects.addAll(matrices);
            objects.addAll(colorTransforms);
        } else if (this.matrixBankIndex != 0) {
            objects.add(new MatrixBankIndexObject(matrixBankIndex));
        }

        return objects;
    }

    private static void validateMatrixBankIndex(int matrixBankIndex) {
        if (matrixBankIndex < 0 || matrixBankIndex > 255) {
            throw new IllegalArgumentException("Matrix bank index must be between 0 and 255");
        }
    }

    private static void validateFps(int fps) {
        if (fps < 0 || fps > 255) {
            throw new IllegalArgumentException("FPS must be between 0 and 255, but was " + fps);
        }
    }

    private record MatrixBankIndexObject(int matrixBankIndex) implements Savable {
        private MatrixBankIndexObject {
            validateMatrixBankIndex(matrixBankIndex);
        }

        @Override
        public void save(ByteStream stream) {
            stream.writeUnsignedChar(matrixBankIndex);
        }

        @Override
        public Tag getTag() {
            return Tag.MATRIX_BANK_INDEX;
        }
    }

    private record ScalingGridObject(Rect scalingGrid) implements Savable {
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
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("unused")
    public static final class Builder {
        private final List<MovieClipChild> children;
        private final List<MovieClipFrame> frames;

        private int fps;
        private boolean customPropertyBoolean = true;
        private int matrixBankIndex;
        private Rect scalingGrid;

        private Builder() {
            children = new ArrayList<>();
            frames = new ArrayList<>();
        }

        public Builder addChild(MovieClipChild child) {
            this.children.add(child);
            return this;
        }

        public Builder addFrame(MovieClipFrame frame) {
            this.frames.add(frame);
            return this;
        }

        public Builder withCustomPropertyBoolean(boolean customPropertyBoolean) {
            this.customPropertyBoolean = customPropertyBoolean;
            return this;
        }

        public Builder withFps(int fps) {
            validateFps(fps);

            this.fps = fps;
            return this;
        }

        public Builder withMatrixBankIndex(int matrixBankIndex) {
            validateMatrixBankIndex(matrixBankIndex);

            this.matrixBankIndex = matrixBankIndex;
            return this;
        }

        public Builder withScalingGrid(Rect scalingGrid) {
            this.scalingGrid = scalingGrid;
            return this;
        }

        public MovieClipOriginal build() {
            return new MovieClipOriginal(children, frames, fps, matrixBankIndex, scalingGrid, customPropertyBoolean);
        }
    }

    private record InlineMatrixBankInfo(int matrixCount, int colorTransformCount) implements Savable {
        @Override
        public void save(ByteStream stream) {
            stream.writeShort(matrixCount);
            stream.writeShort(colorTransformCount);
        }

        @Override
        public Tag getTag() {
            return Tag.MOVIE_CLIP_INLINE_MATRIX_BANK;
        }
    }
}
