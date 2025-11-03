package dev.donutquine.math;

import com.supercell.swf.FBRect;

public class Rect implements ReadonlyRect {
    private float left;
    private float top;
    private float right;
    private float bottom;

    public Rect() {
    }

    public Rect(ReadonlyRect rect) {
        this.left = rect.getLeft();
        this.top = rect.getTop();
        this.right = rect.getRight();
        this.bottom = rect.getBottom();
    }

    public Rect(float width, float height) {
        this.right = width;
        this.bottom = height;
    }

    public Rect(float left, float top, float right, float bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public Rect(FBRect fbRect) {
        loadFromFlatBuffer(fbRect);
    }

    public static Rect createFromSizes(float x, float y, float width, float height) {
        return new Rect(x, y, x + width, y + height);
    }

    public void loadFromFlatBuffer(FBRect fbRect) {
        this.left = fbRect.left();
        this.top = fbRect.top();
        this.right = fbRect.right();
        this.bottom = fbRect.bottom();
    }

    public void movePosition(float x, float y) {
        this.left += x;
        this.right += x;
        this.top += y;
        this.bottom += y;
    }

    public void addPoint(float x, float y) {
        if (this.left > x)
            this.left = x;
        else if (this.right < x)
            this.right = x;

        if (this.top > y)
            this.top = y;
        else if (this.bottom < y)
            this.bottom = y;
    }

    public void mergeBounds(ReadonlyRect rect) {
        if (this.left > rect.getLeft())
            this.left = rect.getLeft();
        if (this.top > rect.getTop())
            this.top = rect.getTop();
        if (this.right < rect.getRight())
            this.right = rect.getRight();
        if (this.bottom < rect.getBottom())
            this.bottom = rect.getBottom();
    }

    public void clamp(ReadonlyRect clampingRect) {
        if (this.left < clampingRect.getLeft())
            this.left = clampingRect.getLeft();
        if (this.right > clampingRect.getRight())
            this.right = clampingRect.getRight();
        if (this.top < clampingRect.getTop())
            this.top = clampingRect.getTop();
        if (this.bottom > clampingRect.getBottom())
            this.bottom = clampingRect.getBottom();
    }

    public boolean containsPoint(float x, float y) {
        return x >= this.left && x <= this.right && y >= this.top && y <= this.bottom;
    }

    public float getWidth() {
        return this.right - this.left;
    }

    public float getHeight() {
        return this.bottom - this.top;
    }

    public float getMidX() {
        return this.left + this.getWidth() / 2f;
    }

    public float getMidY() {
        return this.top + this.getHeight() / 2f;
    }

    // getMinX
    public float getLeft() {
        return this.left;
    }

    // getMinY
    public float getTop() {
        return this.top;
    }

    // getMaxX
    public float getRight() {
        return this.right;
    }

    // getMaxY
    public float getBottom() {
        return this.bottom;
    }

    public boolean overlaps(ReadonlyRect other) {
        return this.left < other.getRight() &&
            this.top < other.getBottom() &&
            this.right > other.getLeft() &&
            this.bottom > other.getTop();
    }

    public void scale(float scaleFactor) {
        this.left *= scaleFactor;
        this.right *= scaleFactor;
        this.top *= scaleFactor;
        this.bottom *= scaleFactor;
    }

    public void copyFrom(ReadonlyRect rect) {
        this.left = rect.getLeft();
        this.top = rect.getTop();
        this.right = rect.getRight();
        this.bottom = rect.getBottom();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof Rect rect)) return false;

        return this.left == rect.left && this.top == rect.top && this.right == rect.right && this.bottom == rect.bottom;
    }

    @Override
    public String toString() {
        return "Rect{" +
            "left=" + left +
            ", top=" + top +
            ", right=" + right +
            ", bottom=" + bottom +
            '}';
    }
}
