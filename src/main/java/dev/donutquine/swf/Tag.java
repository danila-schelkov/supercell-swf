package dev.donutquine.swf;

public enum Tag {
    EOF,
    TEXTURE,
    /**
     * This type of shape doesn't have point count, so it's calculated as {@code 4 * commandCount}.
     * Can contain only commands of type SHAPE_DRAW_BITMAP_COMMAND. <br>
     * <br>
     * It isn't supported since SC2 appeared.
     */
    SHAPE,
    MOVIE_CLIP,
    /**
     * This command contains quad points. Only 4 points allowed. <br>
     * <br>
     * It isn't supported since SC2 appeared.
     */
    SHAPE_DRAW_BITMAP_COMMAND,
    MOVIE_CLIP_FRAME,
    SHAPE_DRAW_COLOR_FILL_COMMAND,
    TEXT_FIELD,
    MATRIX,
    COLOR_TRANSFORM,
    MOVIE_CLIP_2,
    MOVIE_CLIP_FRAME_2,
    MOVIE_CLIP_3,
    TAG_TIMELINE_INDEXES,
    MOVIE_CLIP_4,
    TEXT_FIELD_2,
    TEXTURE_2,  // TEXTURE with mipmaps
    /**
     * This type of command had broken UV coordinates (division and multiplication was mixed up). <br>
     * <br>
     * It isn't supported since SC2 appeared.
     */
    SHAPE_DRAW_BITMAP_COMMAND_2,
    /**
     * This type of shape has point count in the structure.
     */
    SHAPE_2,
    TEXTURE_3,
    TEXT_FIELD_3,
    // if TextField tag >= TEXT_FIELD_4, it has an outline
    TEXT_FIELD_4,
    /**
     * Fixes SHAPE_DRAW_BITMAP_COMMAND_2 UV coordinates issue.
     */
    SHAPE_DRAW_BITMAP_COMMAND_3,
    HALF_SCALE_POSSIBLE,
    TEXTURE_4,
    TEXT_FIELD_5,
    USE_EXTERNAL_TEXTURE,
    // Textures separated by tiles (interlaced)
    TEXTURE_5,
    TEXTURE_6,
    TEXTURE_7,  // TEXTURE_6 with mipmaps
    USE_UNCOMMON_RESOLUTION,
    SCALING_GRID,
    EXTERNAL_FILES_SUFFIXES,
    TEXT_FIELD_6,
    TEXTURE_8,
    MOVIE_CLIP_5,
    MATRIX_PRECISE,
    MOVIE_CLIP_MODIFIERS,
    // StencilRenderingState
    /**
     * All next frame children are the mask
     */
    MODIFIER_STATE_2,
    /**
     * All next frame children are masked
     */
    MODIFIER_STATE_3,
    /**
     * Mask cleaning
     */
    MODIFIER_STATE_4,
    MATRIX_BANK_INDEX,
    EXTRA_MATRIX_BANK,
    TEXT_FIELD_7,
    TEXT_FIELD_8,
    /**
     * Contains KTX file data
     */
    KHRONOS_TEXTURE,
    TEXT_FIELD_9,
    /**
     * Contains a filename of the compressed with zstd texture "zktx"
     */
    TEXTURE_FILE_REFERENCE,
    UNKNOWN_48,  // probably, custom properties for movie clips
    /**
     * Has custom properties
     */
    MOVIE_CLIP_6,
    ;

    public boolean isTexture() {
        return this == TEXTURE || this == TEXTURE_2 || this == TEXTURE_3
            || this == TEXTURE_4 || this == TEXTURE_5 || this == TEXTURE_6
            || this == TEXTURE_7 || this == TEXTURE_8 || this == KHRONOS_TEXTURE
            || this == TEXTURE_FILE_REFERENCE;
    }

    public boolean isShape() {
        return this == SHAPE || this == SHAPE_2;
    }

    public boolean isMovieClip() {
        return this == MOVIE_CLIP || this == MOVIE_CLIP_2 || this == MOVIE_CLIP_3
            || this == MOVIE_CLIP_4 || this == MOVIE_CLIP_5 || this == MOVIE_CLIP_6;
    }

    public boolean isShapeDrawCommand() {
        return isShapeDrawBitmapCommand() || this == SHAPE_DRAW_COLOR_FILL_COMMAND;
    }

    public boolean isShapeDrawBitmapCommand() {
        return this == SHAPE_DRAW_BITMAP_COMMAND || this == SHAPE_DRAW_BITMAP_COMMAND_2 || this == SHAPE_DRAW_BITMAP_COMMAND_3;
    }

    public boolean isMovieClipFrame() {
        return this == MOVIE_CLIP_FRAME || this == MOVIE_CLIP_FRAME_2;
    }

    public boolean isMovieClipModifier() {
        return this == MODIFIER_STATE_2 || this == MODIFIER_STATE_3 || this == MODIFIER_STATE_4;
    }

    public boolean isTextField() {
        return this == TEXT_FIELD || this == TEXT_FIELD_2 || this == TEXT_FIELD_3
            || this == TEXT_FIELD_4 || this == TEXT_FIELD_5 || this == TEXT_FIELD_6
            || this == TEXT_FIELD_7 || this == TEXT_FIELD_8 || this == TEXT_FIELD_9;
    }

    public boolean isColorTransform() {
        return this == COLOR_TRANSFORM;
    }

    public boolean hasInterlacing() {
        return this == Tag.TEXTURE_5 || this == Tag.TEXTURE_6 || this == Tag.TEXTURE_7;
    }

    public boolean hasBlendData() {
        return this == MOVIE_CLIP_3 || this == MOVIE_CLIP_5 || this == MOVIE_CLIP_6;
    }

    public boolean hasCustomProperties() {
        return this == MOVIE_CLIP_6;
    }

    public int getTextureFilter() {
        return switch (this) {
            case TEXTURE, TEXTURE_4, TEXTURE_5, TEXTURE_6 -> 1;
            case TEXTURE_2, TEXTURE_3, TEXTURE_7 -> 2;
            case TEXTURE_8, KHRONOS_TEXTURE, TEXTURE_FILE_REFERENCE -> 0;
            default ->
                throw new IllegalStateException("Unsupported texture tag: " + this);
        };
    }
}
