package dev.donutquine.math;

public interface ReadonlyRect {
    float getWidth();

    float getHeight();

    float getMidX();

    float getMidY();

    float getLeft();

    float getTop();

    float getRight();

    float getBottom();

    boolean containsPoint(float x, float y);

    boolean overlaps(float left, float top, float right, float bottom);
    boolean overlaps(ReadonlyRect other);
}
