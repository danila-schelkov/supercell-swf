package com.vorono4ka.swf.textures;

import com.supercell.swf.FBResources;
import com.supercell.swf.FBTexture;
import com.vorono4ka.streams.ByteStream;
import com.vorono4ka.swf.Savable;
import com.vorono4ka.swf.Tag;
import com.vorono4ka.swf.TextureInfo;
import com.vorono4ka.swf.exceptions.LoadingFaultException;
import com.vorono4ka.utilities.BufferUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

public class SWFTexture implements Savable {
    public static final int TILE_SIZE = 32;

    private Tag initialTag, tag;

    private byte type;
    private int width;
    private int height;
    private byte[] ktxData;
    private String textureFilename;

    private Buffer pixels;

    private int index = -1;
    private TextureInfo textureInfo;

    public SWFTexture() {
    }

    public SWFTexture(FBTexture fb, FBResources resources) {
        this.type = fb.type();
        this.width = fb.width();
        this.height = fb.height();

        if (fb.dataLength() != 0) {
            byte[] data = new byte[fb.dataLength()];
            for (int i = 0; i < data.length; i++) {
                data[i] = fb.data(i);
            }

            this.ktxData = data;
        } else {
            this.textureFilename = fb.textureFileRefId() != 0 ? resources.strings(fb.textureFileRefId()) : null;
        }

        textureInfo = TextureInfo.getTextureInfoByType(type);
        this.tag = determineTag();
    }

    private static boolean hasInterlacing(Tag tag) {
        return tag == Tag.TEXTURE_5 || tag == Tag.TEXTURE_6 || tag == Tag.TEXTURE_7;
    }

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
                throw new LoadingFaultException("Compressed texture filename cannot be null.");
            }
        } else {
            textureFilename = null;
        }

        type = (byte) stream.readUnsignedChar();
        width = stream.readShort();
        height = stream.readShort();

        if (!hasTexture) return;

        textureInfo = TextureInfo.getTextureInfoByType(type);

        // TODO: add callbacks for renderer generating
        if (tag == Tag.KHRONOS_TEXTURE) {
            ktxData = stream.readByteArray(khronosTextureLength);
        } else if (tag != Tag.TEXTURE_FILE_REFERENCE) {
            pixels = loadTexture(stream, width, height, textureInfo.pixelBytes(), hasInterlacing(tag));
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
        //         mipMaps[level] = loadTexture(stream, width, height, textureInfo.pixelBytes(), hasInterlacing(tag));
        //     } while (levelWidth > 1 || levelHeight > 1);
        // }
    }

    @Override
    public void save(ByteStream stream) {
        if (tag == Tag.KHRONOS_TEXTURE) {
            stream.writeInt(ktxData.length);
        }

        if (tag == Tag.TEXTURE_FILE_REFERENCE) {
            stream.writeAscii(textureFilename);
        }

        stream.writeUnsignedChar(type);  // TODO: calculate type
        stream.writeShort(width);
        stream.writeShort(height);

        if (tag == Tag.KHRONOS_TEXTURE) {
            stream.write(ktxData);
        }
    }

    public Tag getTag() {
        return tag;
    }

    public Tag getInitialTag() {
        return initialTag;
    }

    public int getType() {
        return type;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public byte[] getKtxData() {
        return ktxData;
    }

    public String getTextureFilename() {
        return textureFilename;
    }

    public Buffer getPixels() {
        return pixels;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public TextureInfo getTextureInfo() {
        return textureInfo;
    }

    @Override
    public String toString() {
        return "SWFTexture{" + "tag=" + tag + ", type=" + type + ", width=" + width + ", height=" + height + ", ktxData=" + Arrays.toString(ktxData) + ", textureFilename='" + textureFilename + '\'' + ", pixels=" + pixels + ", index=" + index + ", textureInfo=" + textureInfo + '}';
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

    private Buffer loadTextureAsChar(ByteStream stream, int width, int height, boolean separatedByTiles) {
        if (separatedByTiles) {
            ByteBuffer pixels = BufferUtils.allocateDirect(width * height);

            loadInterlacedTexture(stream, width, height, this::readTileAsChar, (index, value) -> pixels.put(index, (byte) value));

            return pixels;
        } else {
            return BufferUtils.wrapDirect(stream.readByteArray(width * height));
        }
    }

    private Buffer loadTextureAsShort(ByteStream stream, int width, int height, boolean separatedByTiles) {
        if (separatedByTiles) {
            ShortBuffer pixels = BufferUtils.allocateDirect(width * height * Short.BYTES).asShortBuffer();

            loadInterlacedTexture(stream, width, height, this::readTileAsShort, (index, value) -> pixels.put(index, (short) value));

            return pixels;
        } else {
            return BufferUtils.wrapDirect(stream.readShortArray(width * height));
        }
    }

    private Buffer loadTextureAsInt(ByteStream stream, int width, int height, boolean separatedByTiles) {
        if (separatedByTiles) {
            IntBuffer pixels = BufferUtils.allocateDirect(width * height * Integer.BYTES).asIntBuffer();

            loadInterlacedTexture(stream, width, height, this::readTileAsInt, pixels::put);

            return pixels;
        } else {
            return BufferUtils.wrapDirect(stream.readIntArray(width * height));
        }
    }

    private int[] readTileAsChar(ByteStream stream, int width, int height) {
        byte[] tile = stream.readByteArray(width * height);

        int[] ints = new int[tile.length];
        for (int i = 0; i < tile.length; i++) {
            ints[i] = tile[i];
        }

        return ints;
    }

    private int[] readTileAsShort(ByteStream stream, int width, int height) {
        short[] tile = stream.readShortArray(width * height);

        int[] ints = new int[tile.length];
        for (int i = 0; i < tile.length; i++) {
            ints[i] = tile[i];
        }

        return ints;
    }

    private int[] readTileAsInt(ByteStream stream, int width, int height) {
        return stream.readIntArray(width * height);
    }

    private void loadInterlacedTexture(ByteStream stream, int width, int height, TileReader tileReader, IntBiConsumer pixelConsumer) {
        int xTileCount = width / TILE_SIZE;
        int yTileCount = height / TILE_SIZE;

        for (int tileY = 0; tileY < yTileCount + 1; tileY++) {
            for (int tileX = 0; tileX < xTileCount + 1; tileX++) {
                int tileWidth = Math.min(width - (tileX * TILE_SIZE), TILE_SIZE);
                int tileHeight = Math.min(height - (tileY * TILE_SIZE), TILE_SIZE);

                int[] tilePixels = tileReader.readTile(stream, tileWidth, tileHeight);

                for (int y = 0; y < tileHeight; y++) {
                    int pixelY = (tileY * TILE_SIZE) + y;

                    for (int x = 0; x < tileWidth; x++) {
                        int pixelX = (tileX * TILE_SIZE) + x;

                        int index = pixelY * width + pixelX;
                        int tilePixelIndex = y * tileWidth + x;
                        int tilePixel = tilePixels[tilePixelIndex];
                        pixelConsumer.accept(index, tilePixel);
                    }
                }
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

    @FunctionalInterface
    private interface TileReader {
        int[] readTile(ByteStream stream, int tileWidth, int tileHeight);
    }

    @FunctionalInterface
    private interface IntBiConsumer {
        void accept(int a, int b);
    }
}
