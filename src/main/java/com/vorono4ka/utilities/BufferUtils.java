package com.vorono4ka.utilities;

import java.nio.*;

@SuppressWarnings("unused")
public final class BufferUtils {
    public static final ByteOrder NATIVE_ORDER = ByteOrder.nativeOrder();

    /**
     * Calculates buffer byte capacity.
     *
     * @param buffer buffer
     * @return capacity in bytes
     * @since 1.0.9
     */
    public static int getByteCapacity(Buffer buffer) {
        if (buffer instanceof ByteBuffer) {
            return buffer.capacity(); // 1 byte per element
        } else if (buffer instanceof ShortBuffer) {
            return buffer.capacity() * Short.BYTES;
        } else if (buffer instanceof CharBuffer) {
            return buffer.capacity() * Character.BYTES;
        } else if (buffer instanceof IntBuffer) {
            return buffer.capacity() * Integer.BYTES;
        } else if (buffer instanceof FloatBuffer) {
            return buffer.capacity() * Float.BYTES;
        } else if (buffer instanceof LongBuffer) {
            return buffer.capacity() * Long.BYTES;
        } else if (buffer instanceof DoubleBuffer) {
            return buffer.capacity() * Double.BYTES;
        } else {
            throw new IllegalArgumentException("Unsupported buffer type: " + buffer.getClass());
        }
    }

    /**
     * @since 1.0.0
     * */
    public static byte[] toArray(ByteBuffer buffer) {
        byte[] array = new byte[buffer.capacity()];
        buffer.rewind();
        buffer.get(array);
        return array;
    }

    /**
    * @since 1.0.7
    * */
    public static short[] toArray(ShortBuffer buffer) {
        short[] array = new short[buffer.capacity()];
        buffer.rewind();
        buffer.get(array);
        return array;
    }

    /**
     * @since 1.0.0
     * */
    public static int[] toArray(IntBuffer buffer) {
        int[] array = new int[buffer.capacity()];
        buffer.rewind();
        buffer.get(array);
        return array;
    }

    /**
     * @since 1.0.0
     * */
    public static ByteBuffer wrapDirect(byte... bytes) {
        ByteBuffer byteBuffer = allocateDirect(bytes.length * Byte.BYTES);
        byteBuffer.put(bytes);
        byteBuffer.position(0);
        return byteBuffer;
    }

    /**
     * @since 1.0.0
     * */
    public static ShortBuffer wrapDirect(short... shorts) {
        ByteBuffer byteBuffer = allocateDirect(shorts.length * Short.BYTES);
        ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
        shortBuffer.put(0, shorts);
        return shortBuffer;
    }

    /**
     * @since 1.0.0
     * */
    public static IntBuffer wrapDirect(int... ints) {
        ByteBuffer byteBuffer = allocateDirect(ints.length * Integer.BYTES);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(0, ints);
        return intBuffer;
    }

    /**
     * @since 1.0.0
     * */
    public static FloatBuffer wrapDirect(float... floats) {
        ByteBuffer byteBuffer = allocateDirect(floats.length * Float.BYTES);
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(0, floats);
        return floatBuffer;
    }

    /**
     * @since 1.0.0
     * */
    public static ByteBuffer allocateDirect(int size) {
        return ByteBuffer.allocateDirect(size).order(NATIVE_ORDER);
    }

    /**
     * @since 1.0.0
     * */
    public static FloatBuffer allocateDirectFloat(int size) {
        return allocateDirect(size * Float.BYTES).asFloatBuffer();
    }

    /**
     * @since 1.0.0
     * */
    public static IntBuffer allocateDirectInt(int size) {
        return allocateDirect(size * Integer.BYTES).asIntBuffer();
    }
}
