package dev.donutquine.swf.movieclips;

import com.supercell.swf.FBMovieClipFrameElement;

public record MovieClipFrameElement(
    int childIndex,
    int matrixIndex,
    int colorTransformIndex
) {
    public MovieClipFrameElement(FBMovieClipFrameElement frameElement) {
        this(frameElement.childIndex(), frameElement.matrixIndex(), frameElement.colorTransformIndex());
    }
}
