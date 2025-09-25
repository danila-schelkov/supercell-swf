package dev.donutquine.swf;

import dev.donutquine.utilities.BitUtils;

import java.nio.ByteBuffer;
import java.util.HashSet;

public class ScCompressedMatrixBank {
    public static final int BLOCK_SIZE = 16;  // 1 << 4

    private static final int BASE_SIZE = 13;
    private static final int DELTA_SIZE = BitUtils.INTEGER_BITS - BASE_SIZE;
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
            throw new IllegalArgumentException("Matrix index " + index + " is out of bounds");
        } else {  // Short matrices
            // MUST NEVER BE CALLED
            throw new IllegalArgumentException("Matrix index " + index + " is out of bounds");
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
        int metadata = buffer.getInt();

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
                // TODO: maybe change to some bit stream?
                switch (flag & 0xF) {
                    case 1 -> {
                        int data1 = getShort(buffer);
                        x += BitUtils.getBitInteger((data1 << 16) | flag, 4, 14);
                        y += BitUtils.getBitInteger(data1, 2, 14);
                        deltaIndex += 2;
                    }
                    case 2 -> {
                        int data1 = getShort(buffer);
                        a += BitUtils.getBitInteger(flag, 4, 7);
                        d += BitUtils.getBitInteger((data1 << 16) | flag, 11, 7);
                        x += BitUtils.getBitInteger(data1, 2, 7);
                        y += BitUtils.getBitInteger(data1, 9, 7);
                        deltaIndex += 2;
                    }
                    case 3 -> {
                        int data1 = getShort(buffer);
                        int data2 = getShort(buffer);
                        a += BitUtils.getBitInteger(flag, 4, 11);
                        d += BitUtils.getBitInteger((data1 << 16) | flag, 15, 11);
                        x += BitUtils.getBitInteger((data2 << 16) | data1, 10, 11);
                        y += BitUtils.getBitInteger(data2, 5, 11);
                        deltaIndex += 3;
                    }
                    case 5 -> {
                        int data1 = getShort(buffer);
                        int data2 = getShort(buffer);

                        a += BitUtils.getBitInteger(flag, 4, 7);
                        b += BitUtils.getBitInteger((data1 << 16) | flag, 11, 7);
                        c += BitUtils.getBitInteger(data1, 2, 7);
                        d += BitUtils.getBitInteger(data1, 9, 7);
                        x += BitUtils.getBitInteger(data2, 0, 8);
                        y += BitUtils.getBitInteger(data2, 8, 8);
                        deltaIndex += 3;
                    }
                    case 6 -> {
                        int data1 = getShort(buffer);
                        int data2 = getShort(buffer);
                        int data3 = getShort(buffer);
                        a += BitUtils.getBitInteger(flag, 4, 10);
                        b += BitUtils.getBitInteger((data1 << 16) | flag, 14, 10);
                        c += BitUtils.getBitInteger((data2 << 16) | data1, 8, 10);
                        d += BitUtils.getBitInteger(data2, 2, 10);
                        x += BitUtils.getBitInteger((data3 << 16) | data2, 12, 10);
                        y += BitUtils.getBitInteger(data3, 6, 10);
                        deltaIndex += 4;
                    }
                    case 7 -> {
                        int data1 = getShort(buffer);
                        int data2 = getShort(buffer);
                        int data3 = getShort(buffer);
                        int data4 = getShort(buffer);
                        a += BitUtils.getBitInteger(flag, 4, 12);
                        b += BitUtils.getBitInteger((data2 << 16) | data1, 0, 12);
                        c += BitUtils.getBitInteger((data2 << 16) | data1, 12, 12);
                        d += BitUtils.getBitInteger((data3 << 16) | data2, 8, 12);
                        x += BitUtils.getBitInteger((data4 << 16) | data3, 4, 14);
                        y += BitUtils.getBitInteger(data4, 2, 14);
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
                        throw new IllegalStateException("Unexpected value: " + Integer.toHexString(flag));
                    }
                }
            } else {
                x += BitUtils.getBitInteger(flag, 2, 7);
                y += BitUtils.getBitInteger(flag, 9, 7);
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