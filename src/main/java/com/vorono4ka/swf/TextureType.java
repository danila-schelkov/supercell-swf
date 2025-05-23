package com.vorono4ka.swf;

import static com.vorono4ka.swf.TextureGlConstants.*;

public enum TextureType {
    TYPE_0(0, GL_RGBA, GL_UNSIGNED_BYTE, 4),
    // Check if unused
    TYPE_1(1, GL_RGBA, GL_UNSIGNED_BYTE, 4),
    TYPE_2(2, GL_RGBA, GL_UNSIGNED_SHORT_4_4_4_4, 2),
    TYPE_3(3, GL_RGBA, GL_UNSIGNED_SHORT_5_5_5_1, 2),
    TYPE_4(4, GL_RGB, GL_UNSIGNED_SHORT_5_6_5, 2),
    // Check if unused
    TYPE_5(5, GL_RGBA, GL_UNSIGNED_BYTE, 4),
    TYPE_6(6, GL_LUMINANCE_ALPHA, GL_UNSIGNED_BYTE, 2),
    // Check if unused
    TYPE_7(7, GL_RGBA, GL_UNSIGNED_BYTE, 4),
    TYPE_8(8, GL_RGBA, GL_UNSIGNED_SHORT_4_4_4_4, 2),
    // Check if unused
    TYPE_9(9, GL_RGBA, GL_UNSIGNED_BYTE, 4),
    TYPE_10(10, GL_LUMINANCE, GL_UNSIGNED_BYTE, 1),
    ;

    public final int type;
    public final int glFormat;
    public final int glType;
    public final int pixelBytes;

    TextureType(int type, int glFormat, int glType, int pixelBytes) {
        this.type = type;
        this.glFormat = glFormat;
        this.glType = glType;
        this.pixelBytes = pixelBytes;
    }

    public static TextureType getByType(int type) {
        for (TextureType info : values()) {
            if (info.type == type) {
                return info;
            }
        }

        throw new IllegalArgumentException("Unknown texture type: " + type);
    }
}

