package com.vorono4ka.swf.shapes;

import com.supercell.swf.FBResources;
import com.supercell.swf.FBShapeDrawBitmapCommand;
import com.supercell.swf.FBShapePoint;
import com.vorono4ka.streams.ByteStream;
import com.vorono4ka.swf.Savable;
import com.vorono4ka.swf.Tag;
import com.vorono4ka.swf.exceptions.UnsupportedTagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.IntFunction;

public class ShapeDrawBitmapCommand implements Savable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShapeDrawBitmapCommand.class);

    private Tag tag;

    private int textureIndex;
    private ShapePoint[] shapePoints;

    private IntFunction<int[]> triangulator;
    private int[] indices;

    public ShapeDrawBitmapCommand() {
    }

    public ShapeDrawBitmapCommand(FBShapeDrawBitmapCommand fb, FBResources resources) {
//        unk = fb.unknown0();
        textureIndex = fb.textureIndex();

        int vertexCount = fb.pointCount();

        shapePoints = new ShapePoint[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            FBShapePoint sbPoint = resources.shapePoints(fb.startingPointIndex() + i);
            shapePoints[i] = new ShapePoint(sbPoint);
        }

        this.tag = determineTag();
    }

    public void load(ByteStream stream, Tag tag) {
        this.tag = tag;

        this.textureIndex = stream.readUnsignedChar();

        int vertexCount = 4;
        if (tag != Tag.SHAPE_DRAW_BITMAP_COMMAND) {
            vertexCount = stream.readUnsignedChar();
            if (tag == Tag.SHAPE_DRAW_BITMAP_COMMAND_2) {
                try {
                    throw new UnsupportedTagException("ShapeDrawBitmapCommand: only TAG_SHAPE_DRAW_BITMAP_COMMAND_3 supported");
                } catch (UnsupportedTagException exception) {
                    LOGGER.error(exception.getMessage(), exception);
                }
            }
        }

        this.shapePoints = new ShapePoint[vertexCount];
        for (int i = 0; i < this.shapePoints.length; i++) {
            this.shapePoints[i] = new ShapePoint();
        }

        for (int i = 0; i < vertexCount; i++) {
            ShapePoint shapePoint = this.shapePoints[i];
            shapePoint.setX(stream.readTwip());
            shapePoint.setY(stream.readTwip());
        }

        for (int i = 0; i < vertexCount; i++) {
            ShapePoint shapePoint = this.shapePoints[i];
            shapePoint.setU(stream.readShort());
            shapePoint.setV(stream.readShort());
        }
    }

    public void save(ByteStream stream) {
        stream.writeUnsignedChar(this.textureIndex);

        if (this.getTag() != Tag.SHAPE_DRAW_BITMAP_COMMAND) {
            stream.writeUnsignedChar(this.shapePoints.length);
        }

        for (ShapePoint point : this.shapePoints) {
            stream.writeTwip(point.getX());
            stream.writeTwip(point.getY());
        }

        for (ShapePoint point : this.shapePoints) {
            stream.writeShort(point.getU());
            stream.writeShort(point.getV());
        }
    }

    public float getX(int pointIndex) {
        return this.shapePoints[pointIndex].getX();
    }

    public float getY(int pointIndex) {
        return this.shapePoints[pointIndex].getY();
    }

    public void setXY(int pointIndex, float x, float y) {
        ShapePoint point = this.shapePoints[pointIndex];

        point.setX(x);
        point.setY(y);
    }

    public float getU(int pointIndex) {
        return this.shapePoints[pointIndex].getU() / 65535f;
    }

    public float getV(int pointIndex) {
        return this.shapePoints[pointIndex].getV() / 65535f;
    }

    public void setUV(int pointIndex, float u, float v) {
        ShapePoint point = this.shapePoints[pointIndex];

        point.setU((int) (u * 65535f));
        point.setV((int) (v * 65535f));
    }

    public Tag getTag() {
        return tag;
    }

    public int getTextureIndex() {
        return textureIndex;
    }

    public int getVertexCount() {
        return shapePoints.length;
    }

    public int getTriangleCount() {
        return this.getVertexCount() - 2;
    }

    public void setTriangulator(IntFunction<int[]> triangulator) {
        this.triangulator = triangulator;
    }

    public int[] getIndices() {
        if (indices != null) {
            return indices;
        }

        return indices = triangulator.apply(getTriangleCount());
    }

    private Tag determineTag() {
        // Note: determining tag due to state (data)
        boolean isQuadShapeAllowed = false;
        return shapePoints.length == 4 && isQuadShapeAllowed ? Tag.SHAPE_DRAW_BITMAP_COMMAND : Tag.SHAPE_DRAW_BITMAP_COMMAND_3;
    }
}
