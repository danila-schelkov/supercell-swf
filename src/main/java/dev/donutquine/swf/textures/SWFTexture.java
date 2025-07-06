package dev.donutquine.swf.textures;

import com.supercell.swf.FBTexture;
import dev.donutquine.streams.ByteStream;
import dev.donutquine.swf.Savable;
import dev.donutquine.swf.Tag;
import dev.donutquine.swf.TextureType;
import dev.donutquine.swf.exceptions.LoadingFaultException;
import dev.donutquine.utilities.BufferUtils;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

public class SWFTexture implements Savable {
    public static final int TILE_SIZE = 32;

    private Tag initialTag, tag;

    private TextureType type;
    private int width;
    private int height;
    private byte[] ktxData;
    private String textureFilename;

    private Buffer pixels;

    private int index = -1;
    private boolean hasTexture;

    /**
     * @since 1.0.0
     */
    public SWFTexture() {
    }

    /**
     * @since 1.0.0
     */
    public SWFTexture(FBTexture fb) {
        this.type = TextureType.getByType(fb.type());
        this.width = fb.width();
        this.height = fb.height();

        if (fb.dataLength() != 0) {
            byte[] data = new byte[fb.dataLength()];
            for (int i = 0; i < data.length; i++) {
                data[i] = fb.data(i);
            }

            this.ktxData = data;
        } else {
            this.textureFilename = fb.textureFile();
        }

        this.tag = determineTag();
        this.initialTag = tag;
    }

    public SWFTexture(Tag tag, TextureType type, int width, int height, Buffer pixelBuffer) {
        validateTag(tag);
        validateTextureSize(width, height);

        this.tag = tag;
        this.type = type;
        this.width = width;
        this.height = height;

        validatePixelBuffer(pixelBuffer, width, height, type);

        this.pixels = pixelBuffer;
    }

    /**
     * @since 1.0.0
     */
    public void load(ByteStream stream, Tag tag, boolean hasTexture) throws LoadingFaultException {
        this.initialTag = this.tag == null ? tag : this.tag;
        this.tag = tag;

        int khronosTextureLength = 0;
        if (tag == Tag.KHRONOS_TEXTURE) {
            khronosTextureLength = stream.readInt();
            assert khronosTextureLength > 0;
        }

        if (tag == Tag.TEXTURE_FILE_REFERENCE) {
            textureFilename = stream.readAscii();
            if (textureFilename == null) {
                throw new LoadingFaultException("Texture file reference cannot be null.");
            }
        } else {
            textureFilename = null;
        }

        type = TextureType.getByType(stream.readUnsignedChar());
        width = stream.readShort();
        height = stream.readShort();

        if (!hasTexture) return;

        // TODO: add callbacks for renderer?
        if (tag == Tag.KHRONOS_TEXTURE) {
            ktxData = stream.readByteArray(khronosTextureLength);
        } else if (tag != Tag.TEXTURE_FILE_REFERENCE) {
            pixels = loadTexture(stream, width, height, type.pixelBytes, tag.hasInterlacing());
        }

        // Note: it seems TEXTURE_3 contains mip map data along with deprecated (?) TEXTURE_2, TEXTURE_7
        // TODO: check
        // if (tag == Tag.TEXTURE_3) {
        //     int level = 1;
        //
        //     int levelWidth, levelHeight;
        //     do {
        //         levelWidth = Math.max(1, width >> level);
        //         levelHeight = Math.max(1, height >> level);
        //         mipMaps[level] = loadTexture(stream, width, height, textureInfo.pixelBytes(), tag.hasInterlacing());
        //     } while (levelWidth > 1 || levelHeight > 1);
        // }
    }

    /**
     * @since 1.0.0
     */
    @Override
    public void save(ByteStream stream) {
        if (tag == Tag.KHRONOS_TEXTURE) {
            stream.writeInt(ktxData.length);
        }

        if (tag == Tag.TEXTURE_FILE_REFERENCE) {
            stream.writeAscii(textureFilename);
        }

        // TODO: calculate type
        stream.writeUnsignedChar(type.type);
        stream.writeShort(width);
        stream.writeShort(height);

        if (!hasTexture) return;

        if (tag == Tag.KHRONOS_TEXTURE) {
            stream.write(ktxData);
        } else if (tag != Tag.TEXTURE_FILE_REFERENCE) {
            saveTexture(stream, width, height, type.pixelBytes, tag.hasInterlacing());
        }
    }

    /**
     * @since 1.0.0
     */
    public Tag getTag() {
        return tag;
    }

    /**
     * @since 1.0.5
     */
    public Tag getInitialTag() {
        return initialTag;
    }

    /**
     * @since 1.0.0
     */
    public TextureType getType() {
        return type;
    }

    /**
     * @since 1.0.0
     */
    public int getWidth() {
        return width;
    }

    /**
     * @since 1.0.0
     */
    public int getHeight() {
        return height;
    }

    /**
     * @since 1.0.0
     */
    public byte[] getKtxData() {
        return ktxData;
    }

    /**
     * @since 1.0.0
     */
    public String getTextureFilename() {
        return textureFilename;
    }

    /**
     * @since 1.0.0
     */
    public Buffer getPixels() {
        return pixels;
    }

    /**
     * @since 1.0.2
     */
    public int getIndex() {
        return index;
    }

    /**
     * @since 1.0.2
     */
    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "SWFTexture{" + "tag=" + tag + ", type=" + type + ", width=" + width + ", height=" + height + ", ktxData=" + Arrays.toString(ktxData) + ", textureFilename='" + textureFilename + '\'' + ", pixels=" + pixels + ", index=" + index + '}';
    }

    private Buffer loadTexture(ByteStream stream, int width, int height, int pixelBytes, boolean hasInterlacing) {
        return switch (pixelBytes) {
            case 1 -> loadTextureAsChar(stream, width, height, hasInterlacing);
            case 2 -> loadTextureAsShort(stream, width, height, hasInterlacing);
            case 4 -> loadTextureAsInt(stream, width, height, hasInterlacing);
            default ->
                throw new IllegalStateException("Unexpected value: " + pixelBytes);
        };
    }

    /**
     * @since 1.0.7
     */
    private void saveTexture(ByteStream stream, int width, int height, int pixelBytes, boolean hasInterlacing) {
        switch (pixelBytes) {
            case 1 -> saveTextureAsChar(stream, width, height, hasInterlacing);
            case 2 -> saveTextureAsShort(stream, width, height, hasInterlacing);
            case 4 -> saveTextureAsInt(stream, width, height, hasInterlacing);
            default ->
                throw new IllegalStateException("Unexpected value: " + pixelBytes);
        }
    }

    private Buffer loadTextureAsChar(ByteStream stream, int width, int height, boolean separatedByTiles) {
        byte[] pixels = stream.readByteArray(width * height);

        if (separatedByTiles) {
            interlaceTexture(width, height, pixels.clone(), pixels, true);
        }

        return BufferUtils.wrapDirect(pixels);
    }

    /**
     * @since 1.0.7
     */
    private void saveTextureAsChar(ByteStream stream, int width, int height, boolean separatedByTiles) {
        byte[] pixels = BufferUtils.toArray((ByteBuffer) this.pixels);

        if (separatedByTiles) {
            interlaceTexture(width, height, pixels.clone(), pixels, false);
        }

        stream.writeByteArray(pixels);
    }

    private Buffer loadTextureAsShort(ByteStream stream, int width, int height, boolean separatedByTiles) {
        short[] pixels = stream.readShortArray(width * height);

        if (separatedByTiles) {
            interlaceTexture(width, height, pixels.clone(), pixels, true);
        }

        return BufferUtils.wrapDirect(pixels);
    }

    /**
     * @since 1.0.7
     */
    private void saveTextureAsShort(ByteStream stream, int width, int height, boolean separatedByTiles) {
        short[] pixels = BufferUtils.toArray((ShortBuffer) this.pixels);

        if (separatedByTiles) {
            interlaceTexture(width, height, pixels.clone(), pixels, false);
        }

        stream.writeShortArray(pixels);
    }

    private Buffer loadTextureAsInt(ByteStream stream, int width, int height, boolean separatedByTiles) {
        int[] pixels = stream.readIntArray(width * height);

        if (separatedByTiles) {
            interlaceTexture(width, height, pixels.clone(), pixels, true);
        }

        return BufferUtils.wrapDirect(pixels);
    }

    /**
     * @since 1.0.7
     */
    private void saveTextureAsInt(ByteStream stream, int width, int height, boolean separatedByTiles) {
        int[] pixels = BufferUtils.toArray((IntBuffer) this.pixels);

        if (separatedByTiles) {
            interlaceTexture(width, height, pixels.clone(), pixels, false);
        }

        stream.writeIntArray(pixels);
    }

    /**
     * @since 1.0.7
     */
    private void interlaceTexture(int width, int height, Object srcArray, Object dstArray, boolean reverseIndex) {
        if (srcArray.getClass() != dstArray.getClass()) {
            throw new IllegalArgumentException("srcArray and dstArray must be of type " + srcArray.getClass());
        }

        VarHandle arrayElementHandle = MethodHandles.arrayElementVarHandle(dstArray.getClass());

        int xTileCount = width / TILE_SIZE;
        int yTileCount = height / TILE_SIZE;

        int offset = 0;

        for (int tileY = 0; tileY < yTileCount + 1; tileY++) {
            for (int tileX = 0; tileX < xTileCount + 1; tileX++) {
                int tileWidth = Math.min(width - (tileX * TILE_SIZE), TILE_SIZE);
                int tileHeight = Math.min(height - (tileY * TILE_SIZE), TILE_SIZE);

                for (int y = 0; y < tileHeight; y++) {
                    int pixelY = (tileY * TILE_SIZE) + y;

                    for (int x = 0; x < tileWidth; x++) {
                        int pixelX = (tileX * TILE_SIZE) + x;

                        int index = pixelY * width + pixelX;
                        int tilePixelIndex = y * tileWidth + x + offset;

                        if (reverseIndex) {
                            arrayElementHandle.set(dstArray, index, arrayElementHandle.get(srcArray, tilePixelIndex));
                        } else {
                            arrayElementHandle.set(dstArray, tilePixelIndex, arrayElementHandle.get(srcArray, index));
                        }
                    }
                }

                offset += tileWidth * tileHeight;
            }
        }
    }

    private Tag determineTag() {
        if (this.ktxData != null) {
            return Tag.KHRONOS_TEXTURE;
        }

        if (this.textureFilename != null) {
            return Tag.TEXTURE_FILE_REFERENCE;
        }

        throw new IllegalStateException("This type of texture is not supported yet.");
    }

    /**
     * @since 1.0.7
     */
    public void setHasTexture(boolean hasTexture) {
        this.hasTexture = hasTexture;
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("unused")
    public static final class Builder {
        private Tag tag;
        private TextureType type;
        private int width = -1, height = -1;
        private Buffer pixelBuffer;

        private Builder() {
        }

        public Builder tag(Tag tag) {
            validateTag(tag);

            this.tag = tag;
            return this;
        }

        public Builder type(TextureType type) {
            this.type = type;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder height(int height) {
            this.height = height;
            return this;
        }

        public Builder pixels(Buffer pixelBuffer) {
            validatePixelBuffer(pixelBuffer, this.width, this.height, this.type);

            this.pixelBuffer = pixelBuffer;
            return this;
        }

        public SWFTexture build() {
            return new SWFTexture(tag, type, width, height, pixelBuffer);
        }
    }

    private static void validatePixelBuffer(Buffer pixelBuffer, int width, int height, TextureType type) {
        if (pixelBuffer == null) {
            // TODO: remove as soon as possible when other texture types added.
            throw new NullPointerException("pixelBuffer must be set");
        }

        if (width == -1 || height == -1 || type == null) {
            throw new IllegalStateException("Width, height and type must be set before pixels");
        }

        int bytesExpected = width * height * type.pixelBytes;
        if (bytesExpected != BufferUtils.getByteCapacity(pixelBuffer)) {
            throw new IllegalArgumentException("Expected " + bytesExpected + " bytes but got " + pixelBuffer.capacity());
        }
    }

    private static void validateTag(Tag tag) {
        if (!tag.isTexture()) {
            throw new IllegalArgumentException("Tag must be a Texture tag, but is " + tag);
        }
    }

    private static void validateTextureSize(int width, int height) {
        if (width == -1 || height == -1) {
            throw new IllegalStateException("width and height must be set.");
        }

        if (width < 0) {
            throw new IllegalArgumentException("width must be greater than zero.");
        }

        if (height < 0) {
            throw new IllegalArgumentException("height must be greater than zero.");
        }
    }
}
