package dev.donutquine.swf;

import java.nio.ByteBuffer;
import java.util.HashSet;

public class ScCompressedMatrixBank {
    public static final int BLOCK_SIZE = 16;  // 1 << 4

    private static final int BASE_SIZE = 13;
    private static final int DELTA_SIZE = Integer.BYTES * 8 - BASE_SIZE;
    private static final int DELTA_MASK = (1 << DELTA_SIZE) - 1;
    private static final int BASE_MASK = (1 << BASE_SIZE) - 1;

    private final ByteBuffer buffer;
    private final int metadataPosition;
    private final int matrixDataPosition;
    private final int floatMatrixCount;
    private final int shortMatrixCount;
    private final int uncompressedMatrixCount;

    private final HashSet<Integer> decompressedBlockIndices = new HashSet<>();
    private final Matrix2x3[] decompressedMatrices;

    public ScCompressedMatrixBank(ByteBuffer buffer, int floatMatrixCount, int shortMatrixCount, int compressedBlockCount) {
        this.buffer = buffer;
        this.floatMatrixCount = floatMatrixCount;
        this.shortMatrixCount = shortMatrixCount;
        this.uncompressedMatrixCount = floatMatrixCount + shortMatrixCount;
        this.metadataPosition = floatMatrixCount * Float.BYTES * 6;
        this.matrixDataPosition = this.metadataPosition + compressedBlockCount * Integer.BYTES;

        this.decompressedMatrices = new Matrix2x3[BLOCK_SIZE * compressedBlockCount];
        for (int i = 0; i < decompressedMatrices.length; i++) {
            decompressedMatrices[i] = new Matrix2x3();
        }
    }

    public Matrix2x3 getMatrix(int index) {
        if (index >= uncompressedMatrixCount) {
            int blockIndex = index >> 4;
            if (!decompressedBlockIndices.contains(blockIndex)) {
                decodeBlock(blockIndex, decompressedMatrices, blockIndex << 4);
                decompressedBlockIndices.add(blockIndex);
            }

            return decompressedMatrices[index];
        } else if (index < floatMatrixCount) {
            // MUST NEVER BE CALLED
            return new Matrix2x3();
        } else {  // Short matrices
            // MUST NEVER BE CALLED
            return new Matrix2x3();
        }
    }

    public Matrix2x3[] decodeBlock(int blockIndex) {
        Matrix2x3[] matrices = new Matrix2x3[BLOCK_SIZE];
        for (int i = 0; i < BLOCK_SIZE; i++) {
            matrices[i] = new Matrix2x3();
        }

        decodeBlock(blockIndex, matrices, 0);
        return matrices;
    }

    public void decodeBlock(int blockIndex, Matrix2x3[] matrices, int offset) {
        assert offset >= 0;
        assert offset + BLOCK_SIZE <= matrices.length;

        buffer.position(metadataPosition + blockIndex * Integer.BYTES);
        int metadata = buffer.asIntBuffer().get();

        int baseMatrixIndex = metadata & BASE_MASK;
        int deltaIndex = (metadata >> BASE_SIZE) & DELTA_MASK;
        buffer.position(matrixDataPosition + baseMatrixIndex * Short.BYTES * 6);

        int a = getShort(buffer);
        int b = getShort(buffer);
        int c = getShort(buffer);
        int d = getShort(buffer);
        int x = getShort(buffer);
        int y = getShort(buffer);

        for (int i = 0; i < BLOCK_SIZE; i++) {
            buffer.position(matrixDataPosition + deltaIndex * Short.BYTES);

            int flag = getShort(buffer);
            if ((flag & 3) != 0) {
                // TODO: fix casts
                switch (flag & 0xF) {
                    case 1 -> {
                        int data1 = getShort(buffer);
                        x += ((flag << 14) & 0x3FFFFFFF | (data1 << 30)) >> 18;
                        y += data1 >> 2;
                        deltaIndex += 2;
                    }
                    case 2 -> {
                        int data1 = getShort(buffer);
                        a += (flag << 21) >> 25;
                        d += ((flag << 14) & 0x3FFFFFFF | (data1 << 30)) >> 25;
                        x += (data1 << 23) >> 25;
                        y += (data1 << 16) >> 25;
                        deltaIndex += 2;
                    }
                    case 3 -> {
                        int data1 = getShort(buffer);
                        int data2 = getShort(buffer);
                        a += (flag << 17) >> 21;
                        d += ((flag << 6) & 0x3FFFFF | (data1 << 22)) >> 21;
                        x += ((data1 << 11) & 0x7FFFFFF | (data2 << 27)) >> 21;
                        y += data2 >> 5;
                        deltaIndex += 3;
                    }
                    case 5 -> {
                        int data1 = getShort(buffer);
                        int data2 = getShort(buffer);

                        a += (flag << 21) >> 25;
                        b += (flag << 14) & 0x3FFFFFFF | (data1 << 30) >> 25;
                        c += data1 << 23 >> 25;
                        d += (data1 << 16) >> 25;
                        x += data2 & 0xFF;
                        y += data2 >> 8;
                        deltaIndex += 3;
                    }
                    case 6 -> {
                        int data1 = getShort(buffer);
                        int data2 = getShort(buffer);
                        int data3 = getShort(buffer);
                        a += (flag << 18) >> 22;
                        b += ((flag << 8) & 0xFFFFFF | (data1 << 24)) >> 22;
                        c += ((data1 << 14) & 0x3FFFFFFF | (data2 << 30)) >> 22;
                        d += data2 << 20 >> 22;
                        x += ((data2 << 10) & 0x3FFFFFF | (data3 << 26)) >> 22;
                        y += data3 >> 6;
                        deltaIndex += 4;
                    }
                    case 7 -> {
                        int data1 = getShort(buffer);
                        int data2 = getShort(buffer);
                        int data3 = getShort(buffer);
                        int data4 = getShort(buffer);
                        a += flag >> 4;
                        b += (data1 << 20) >> 20;
                        c += ((data1 << 8) & 0xFFFFFF | (data2 << 24)) >> 20;
                        d += ((data2 << 12) & 0xFFFFFFF | (data3 << 28)) >> 20;
                        x += ((data3 << 14) & 0x3FFFFFFF | (data4 << 30)) >> 18;
                        y += data4 >> 2;
                        deltaIndex += 5;
                    }
                    case 0xF -> {
                        a += getShort(buffer);
                        b += getShort(buffer);
                        c += getShort(buffer);
                        d += getShort(buffer);
                        x += getShort(buffer);
                        y += getShort(buffer);
                        deltaIndex += 7;
                    }
                    default -> {
                        // nothing, really nothing
                    }
                }
            } else {
                x += flag << 23 >> 25;
                y += flag << 16 >> 25;
                deltaIndex += 1;
            }

            matrices[offset + i].set(
                (short) (a & 0xFFFF),
                (short) (b & 0xFFFF),
                (short) (c & 0xFFFF),
                (short) (d & 0xFFFF),
                (short) (x & 0xFFFF),
                (short) (y & 0xFFFF)
            );
        }
    }

    private static int getShort(ByteBuffer buffer) {
        return buffer.getShort() & 0xFFFF;
    }
}