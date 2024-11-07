package com.vorono4ka.swf;

public record TextureInfo(int pixelFormat, int pixelType, int pixelBytes) {
    private static final int GL_RGB = 6407;
    private static final int GL_RGBA = 6408;
    private static final int GL_UNSIGNED_BYTE = 5121;
    private static final int GL_UNSIGNED_SHORT_4_4_4_4 = 32819;
    private static final int GL_UNSIGNED_SHORT_5_5_5_1 = 32820;
    private static final int GL_UNSIGNED_SHORT_5_6_5 = 33635;
    private static final int GL_LUMINANCE_ALPHA = 6410;
    private static final int GL_LUMINANCE = 6409;

    public static TextureInfo getTextureInfoByType(int type) {
        int pixelFormat = GL_RGBA;
        int pixelType = GL_UNSIGNED_BYTE;
        int pixelBytes = 4;

        switch (type) {
            case 2, 8 -> {
                pixelType = GL_UNSIGNED_SHORT_4_4_4_4;
                pixelBytes = 2;
            }
            case 3 -> {
                pixelType = GL_UNSIGNED_SHORT_5_5_5_1;
                pixelBytes = 2;
            }
            case 4 -> {
                pixelType = GL_UNSIGNED_SHORT_5_6_5;
                pixelFormat = GL_RGB;
                pixelBytes = 2;
            }
            case 6 -> {
                pixelFormat = GL_LUMINANCE_ALPHA;
                pixelBytes = 2;
            }
            case 10 -> {
                pixelFormat = GL_LUMINANCE;
                pixelBytes = 1;
            }
        }

        return new TextureInfo(pixelFormat, pixelType, pixelBytes);
    }
}
