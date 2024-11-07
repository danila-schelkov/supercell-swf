package com.vorono4ka.swf.shapes;

import com.supercell.swf.FBShapePoint;

import java.util.Objects;

public final class ShapePoint {
    private float x;
    private float y;
    private int u;
    private int v;

    public ShapePoint() {

    }

    public ShapePoint(
        float x,
        float y,
        int u,
        int v
    ) {
        this.x = x;
        this.y = y;
        this.u = u;
        this.v = v;
    }

    public ShapePoint(FBShapePoint fbShapePoint) {
        this.x = fbShapePoint.x();
        this.y = fbShapePoint.y();
        this.u = fbShapePoint.u();
        this.v = fbShapePoint.v();
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

    public int getU() {
        return u;
    }

    public void setU(int u) {
        this.u = u;
    }

    public int getV() {
        return v;
    }

    public void setV(int v) {
        this.v = v;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ShapePoint) obj;
        return Float.floatToIntBits(this.x) == Float.floatToIntBits(that.x) &&
            Float.floatToIntBits(this.y) == Float.floatToIntBits(that.y) &&
            this.u == that.u &&
            this.v == that.v;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, u, v);
    }

    @Override
    public String toString() {
        return "ShapePoint[" +
            "x=" + x + ", " +
            "y=" + y + ", " +
            "u=" + u + ", " +
            "v=" + v + ']';
    }
}
