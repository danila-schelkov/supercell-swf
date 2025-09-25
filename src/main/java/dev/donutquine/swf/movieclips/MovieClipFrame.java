package dev.donutquine.swf.movieclips;

import com.supercell.swf.FBMovieClipFrame;
import com.supercell.swf.FBResources;
import dev.donutquine.streams.ByteStream;
import dev.donutquine.swf.Savable;
import dev.donutquine.swf.Tag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MovieClipFrame implements Savable {
    private Tag tag;

    private String label;

    private List<MovieClipFrameElement> elements;

    public MovieClipFrame() {
    }

    public MovieClipFrame(String label, List<MovieClipFrameElement> elements, boolean includeElements) {
        this.label = label;
        this.elements = new ArrayList<>(elements);

        this.tag = includeElements ? Tag.MOVIE_CLIP_FRAME : Tag.MOVIE_CLIP_FRAME_2;
    }

    public MovieClipFrame(FBMovieClipFrame fb, FBResources resources, int offset) {
        label = fb.labelRefId() != 0 ? resources.strings(fb.labelRefId()) : null;
        elements = new ArrayList<>(fb.frameElementCount());
        for (int i = 0; i < fb.frameElementCount(); i++) {
            elements.add(new MovieClipFrameElement(resources.movieClipFrameElements(offset + i)));
        }

        tag = Tag.MOVIE_CLIP_FRAME_2;
    }

    public int load(ByteStream stream, Tag tag) {
        this.tag = tag;

        int elementCount = stream.readShort();
        this.label = stream.readAscii();

        if (tag == Tag.MOVIE_CLIP_FRAME) {
            this.elements = new ArrayList<>(elementCount);
            for (int i = 0; i < elementCount; i++) {
                int childIndex = stream.readShort() & 0xFFFF;
                int matrixIndex = stream.readShort() & 0xFFFF;
                int colorTransformIndex = stream.readShort() & 0xFFFF;
                this.elements.add(new MovieClipFrameElement(childIndex, matrixIndex, colorTransformIndex));
            }
        }

        return elementCount;
    }

    public void save(ByteStream stream) {
        stream.writeShort(this.elements.size());
        stream.writeAscii(this.label);

        if (tag == Tag.MOVIE_CLIP_FRAME) {
            for (MovieClipFrameElement element : this.elements) {
                stream.writeShort(element.childIndex());
                stream.writeShort(element.matrixIndex());
                stream.writeShort(element.colorTransformIndex());
            }
        }
    }

    @Override
    public Tag getTag() {
        return tag;
    }

    public String getLabel() {
        return label;
    }

    public int getElementCount() {
        return elements.size();
    }

    public List<MovieClipFrameElement> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public void setElements(List<MovieClipFrameElement> elements) {
        this.elements = elements;
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("unused")
    public static final class Builder {
        private final List<MovieClipFrameElement> elements;

        private String label;
        private boolean includeElements;

        private Builder() {
            elements = new ArrayList<>();
        }

        public Builder addElement(MovieClipFrameElement element) {
            this.elements.add(element);
            return this;
        }

        public Builder withLabel(String label) {
            this.label = label;
            return this;
        }

        public Builder setIncludeElements(boolean includeElements) {
            this.includeElements = includeElements;
            return this;
        }

        public MovieClipFrame build() {
            return new MovieClipFrame(label, elements, includeElements);
        }
    }
}
