package dev.donutquine.utilities;

public class BitUtils {
    public static final int INTEGER_BITS = Integer.BYTES * 8;

    /// Reads `length` bits from `offset` given number and returns them as signed integer.
    public static int getBitInteger(int number, int offset, int length) {
        return (number << (INTEGER_BITS - offset - length)) >> (INTEGER_BITS - length);
    }

    /// Reads `length` bits from `offset` given number and returns them as unsigned integer.
    ///
    /// Must be positive, so max `length` is only 31 bits from `offset` 0.
    public static int getUnsignedBitInteger(int number, int offset, int length) {
        assert number >= 0;

        if (offset == 0) {
            return number & ((1 << length) - 1);
        }

        return (number >> offset) & ((1 << length) - 1);
    }
}
