package com.vorono4ka.swf.movieclips;

import com.supercell.swf.FBMovieClipFrame;
import com.supercell.swf.FBResources;
import com.vorono4ka.streams.ByteStream;
import com.vorono4ka.swf.Tag;

public class MovieClipFrame {
    private int elementCount;
    private String label;

    private MovieClipFrameElement[] elements;

    public MovieClipFrame() {
    }

    public MovieClipFrame(FBMovieClipFrame fb, FBResources resources, int offset) {
        elementCount = fb.frameElementCount();
        label = resources.strings(fb.labelRefId());
        elements = new MovieClipFrameElement[elementCount];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = new MovieClipFrameElement(resources.movieClipFrameElements(offset + i));
        }
    }

    public int load(ByteStream stream, Tag tag) {
        this.elementCount = stream.readShort();
        this.label = stream.readAscii();

        if (tag == Tag.MOVIE_CLIP_FRAME) {
            this.elements = new MovieClipFrameElement[this.elementCount];
            for (int i = 0; i < this.elementCount; i++) {
                int childIndex = stream.readShort() & 0xFFFF;
                int matrixIndex = stream.readShort() & 0xFFFF;
                int colorTransformIndex = stream.readShort() & 0xFFFF;
                this.elements[i] = new MovieClipFrameElement(childIndex, matrixIndex, colorTransformIndex);
            }
        }

        return this.elementCount;
    }

    public void save(ByteStream stream) {
        stream.writeShort(this.elements.length);
        stream.writeAscii(this.label);
    }

    public String getLabel() {
        return label;
    }

    public MovieClipFrameElement[] getElements() {
        return elements;
    }

    public void setElements(MovieClipFrameElement[] elements) {
        this.elements = elements;
    }

    public int getElementCount() {
        return elementCount;
    }
}
