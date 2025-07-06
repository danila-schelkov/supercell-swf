package dev.donutquine.swf;

import dev.donutquine.streams.ByteStream;

import java.util.Objects;

/**
 * Affine transformation matrix 2x3. <br>
 * <p>
 * a b x <br>
 * c d y
 */
public class Matrix2x3 implements Savable {
    private static final float PRECISE_MULTIPLIER = 65535f;
    private static final float DEFAULT_MULTIPLIER = 1024f;
    private static final float PRECISE_FLOAT = 0.0009765f;
    private static final float TWIP_MULTIPLIER = 20f;

    private float a;  // m00
    private float b;  // m01
    private float c;  // m10
    private float d;  // m11
    private float x;  // m02
    private float y;  // m12

    public Matrix2x3() {
        // Setting scale to defaults
        this.a = 1.0f;
        this.d = 1.0f;
    }

    public Matrix2x3(float a, float b, float c, float d, float x, float y) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.x = x;
        this.y = y;
    }

    public Matrix2x3(Matrix2x3 matrix) {
        this.a = matrix.a;
        this.b = matrix.b;
        this.c = matrix.c;
        this.d = matrix.d;
        this.x = matrix.x;
        this.y = matrix.y;
    }

    public void set(float a, float b, float c, float d, float x, float y) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.x = x;
        this.y = y;
    }

    public void set(short a, short b, short c, short d, short x, short y) {
        this.a = a / DEFAULT_MULTIPLIER;
        this.b = b / DEFAULT_MULTIPLIER;
        this.c = c / DEFAULT_MULTIPLIER;
        this.d = d / DEFAULT_MULTIPLIER;
        this.x = x / TWIP_MULTIPLIER;
        this.y = y / TWIP_MULTIPLIER;
    }

    public void load(ByteStream stream, boolean isPrecise) {
        float divider = isPrecise ? PRECISE_MULTIPLIER : DEFAULT_MULTIPLIER;

        this.a = stream.readInt() / divider;
        this.b = stream.readInt() / divider;
        this.c = stream.readInt() / divider;
        this.d = stream.readInt() / divider;
        this.x = stream.readTwip();
        this.y = stream.readTwip();
    }

    @Override
    public void save(ByteStream stream) {
        float multiplier = isPrecise() ? PRECISE_MULTIPLIER : DEFAULT_MULTIPLIER;

        stream.writeInt((int) (this.a * multiplier));
        stream.writeInt((int) (this.b * multiplier));
        stream.writeInt((int) (this.c * multiplier));
        stream.writeInt((int) (this.d * multiplier));
        stream.writeTwip(this.x);
        stream.writeTwip(this.y);
    }

    @Override
    public Tag getTag() {
        return isPrecise() ? Tag.MATRIX_PRECISE : Tag.MATRIX;
    }

    public void multiply(Matrix2x3 matrix) {
        float a = (this.a * matrix.a) + (this.b * matrix.c);
        float b = (this.a * matrix.b) + (this.b * matrix.d);
        float c = (this.d * matrix.c) + (this.c * matrix.a);
        float d = (this.d * matrix.d) + (this.c * matrix.b);
        float x = matrix.applyX(this.x, this.y);
        float y = matrix.applyY(this.x, this.y);

        this.a = a;
        this.b = b;
        this.d = d;
        this.c = c;
        this.x = x;
        this.y = y;
    }

    public float applyX(float x, float y) {
        return x * this.a + y * this.c + this.x;
    }

    public float applyY(float x, float y) {
        return y * this.d + x * this.b + this.y;
    }

    public void scaleMultiply(float scaleX, float scaleY) {
        this.a *= scaleX;
        this.b *= scaleY;
        this.c *= scaleX;
        this.d *= scaleY;
    }

    public void setRotation(float angle, float scaleX, float scaleY) {
        this.setRotationRadians((float) Math.toRadians(angle), scaleX, scaleY);
    }

    public void setRotationRadians(float angle, float scaleX, float scaleY) {
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);

        this.a = cos * scaleX;
        this.b = -(sin * scaleY);
        this.c = sin * scaleX;
        this.d = cos * scaleY;
    }

    public void rotate(float angle) {
        this.rotateRadians((float) Math.toRadians(angle));
    }

    public void rotateRadians(float angle) {
        float sin = (float) Math.sin(angle);
        float cos = (float) Math.cos(angle);

        float tmp00 = this.a * cos + this.b * sin;
        float tmp01 = this.a * -sin + this.b * cos;
        float tmp10 = this.c * cos + this.d * sin;
        float tmp11 = this.c * -sin + this.d * cos;

        this.a = tmp00;
        this.b = tmp01;
        this.c = tmp10;
        this.d = tmp11;
    }

    public void inverse() {
        float determinant = getDeterminant();
        if (determinant == 0.0f) return;

        float a = this.a;
        float b = this.b;
        float x = this.x;
        float c = this.c;
        float d = this.d;
        float y = this.y;

        this.x = ((y * c) - (x * d)) / determinant;
        this.y = ((x * b) - (y * a)) / determinant;
        this.a = d / determinant;
        this.b = -b / determinant;
        this.c = -c / determinant;
        this.d = a / determinant;
    }

    public void move(float x, float y) {
        this.x += x;
        this.y += y;
    }

    public void setXY(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public float getA() {
        return a;
    }

    public float getB() {
        return b;
    }

    public float getC() {
        return c;
    }

    public float getD() {
        return d;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Matrix2x3 matrix2x3 = (Matrix2x3) other;
        return a == matrix2x3.a && b == matrix2x3.b && c == matrix2x3.c && d == matrix2x3.d && x == matrix2x3.x && y == matrix2x3.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(a, b, c, d, x, y);
    }

    @Override
    public String toString() {
        return "Matrix2x3{" + a + ", " + b + ", " + c + ", " + d + ", " + x + ", " + y + '}';
    }

    public DecomposedMatrix2x3 decompose() {
        double scaleX = Math.hypot(a, b);
        double theta = Math.atan2(b, a);

        double sin = Math.sin(theta);
        double cos = Math.cos(theta);

        // Note: double is too precise structure, sin may be around 0, but not equal to it
        double scaleY = Math.abs(sin) > 0.01f ? c / sin : d / cos;

        return new DecomposedMatrix2x3(scaleX, scaleY, theta, x, y);
    }

    public record DecomposedMatrix2x3(double scaleX, double scaleY,
                                      double rotationRadians, double x, double y) {
        public double rotationDegrees() {
            return Math.toDegrees(rotationRadians);
        }
    }

    private float getDeterminant() {
        return (this.d * this.a) - (this.c * this.b);
    }

    private boolean isPrecise() {
        return (this.a != 0 && Math.abs(this.a) < PRECISE_FLOAT) || (this.d != 0 && Math.abs(this.d) < PRECISE_FLOAT) || (this.b != 0 && Math.abs(this.b) < PRECISE_FLOAT) || (this.c != 0 && Math.abs(this.c) < PRECISE_FLOAT);
    }
}
