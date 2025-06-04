package dev.donutquine.swf.shapes;

import dev.donutquine.swf.SupercellSWF;
import com.supercell.swf.FBResources;
import com.supercell.swf.FBShapeDrawBitmapCommand;
import com.supercell.swf.FBShapePoint;
import dev.donutquine.streams.ByteStream;
import dev.donutquine.swf.Savable;
import dev.donutquine.swf.Tag;
import dev.donutquine.swf.exceptions.UnsupportedTagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ShapeDrawBitmapCommand implements Savable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShapeDrawBitmapCommand.class);

    private Tag tag;

    private int textureIndex;
    private List<ShapePoint> shapePoints;

    /**
     * @since 1.0.0
     */
    public ShapeDrawBitmapCommand() {
    }

    /**
     * @since 1.0.9
     */
    public ShapeDrawBitmapCommand(int textureIndex, List<ShapePoint> points) {
        if (textureIndex == -1) {
            throw new IllegalStateException("Texture index must be set");
        }

        if (textureIndex < 0) {
            throw new IllegalArgumentException("Texture index cannot be negative");
        }

        if (points.size() < 3) {
            throw new IllegalArgumentException("Shape draw command must have at least 3 points!");
        }

        if (points.size() > 255) {
            // NOTE: make flatbuffer saving and enforce user to use it for more points
            throw new IllegalArgumentException("Too many points: " + (points.size()));
        }

        this.textureIndex = textureIndex;
        this.shapePoints = new ArrayList<>(points);

        this.tag = determineTag();
    }

    /**
     * @since 1.0.0
     */
    public ShapeDrawBitmapCommand(FBShapeDrawBitmapCommand fb, FBResources resources) {
//        unk = fb.unknown0();
        textureIndex = fb.textureIndex();

        int vertexCount = fb.pointCount();

        shapePoints = new ArrayList<>(vertexCount);
        for (int i = 0; i < vertexCount; i++) {
            FBShapePoint sbPoint = resources.shapePoints(fb.startingPointIndex() + i);
            shapePoints.add(i, new ShapePoint(sbPoint));
        }

        this.tag = determineTag();
    }

    /**
     * @since 1.0.0
     */
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

        this.shapePoints = new ArrayList<>(vertexCount);
        for (int i = 0; i < vertexCount; i++) {
            this.shapePoints.add(i, new ShapePoint());
        }

        for (ShapePoint shapePoint : this.shapePoints) {
            shapePoint.setX(stream.readTwip());
            shapePoint.setY(stream.readTwip());
        }

        for (ShapePoint shapePoint : this.shapePoints) {
            shapePoint.setU(stream.readShort());
            shapePoint.setV(stream.readShort());
        }
    }

    /**
     * @since 1.0.0
     */
    public void save(ByteStream stream) {
        stream.writeUnsignedChar(this.textureIndex);

        if (this.getTag() != Tag.SHAPE_DRAW_BITMAP_COMMAND) {
            stream.writeUnsignedChar(this.shapePoints.size());
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

    /**
     * @since 1.0.0
     */
    public float getX(int pointIndex) {
        return this.shapePoints.get(pointIndex).getX();
    }

    /**
     * @since 1.0.0
     */
    public float getY(int pointIndex) {
        return this.shapePoints.get(pointIndex).getY();
    }

    /**
     * @since 1.0.0
     */
    public void setXY(int pointIndex, float x, float y) {
        ShapePoint point = this.shapePoints.get(pointIndex);

        point.setX(x);
        point.setY(y);
    }

    /**
     * @since 1.0.0
     */
    public float getU(int pointIndex) {
        return this.shapePoints.get(pointIndex).getU() / 65535f;
    }

    /**
     * @since 1.0.0
     */
    public float getV(int pointIndex) {
        return this.shapePoints.get(pointIndex).getV() / 65535f;
    }

    /**
     * @since 1.0.0
     */
    public void setUV(int pointIndex, float u, float v) {
        ShapePoint point = this.shapePoints.get(pointIndex);

        point.setU((int) (u * 65535f));
        point.setV((int) (v * 65535f));
    }

    // TODO: add methods for adding points to the polygon.

    /**
     * @since 1.0.0
     */
    public Tag getTag() {
        return tag;
    }

    /**
     * @since 1.0.0
     */
    public int getTextureIndex() {
        return textureIndex;
    }

    /**
     * Sets the index of the texture in {@link SupercellSWF SupercellSWF} object to be drawn from.
     *
     * @param textureIndex texture index
     * @since 1.0.7
     */
    public void setTextureIndex(int textureIndex) {
        this.textureIndex = textureIndex;
    }

    /**
     * @since 1.0.0
     */
    public int getVertexCount() {
        return shapePoints.size();
    }

    /**
     * @since 1.0.0
     */
    public int getTriangleCount() {
        return this.getVertexCount() - 2;
    }

    private Tag determineTag() {
        // Note: determining tag due to state (data)
        boolean isQuadShapeAllowed = false;
        return shapePoints.size() == 4 && isQuadShapeAllowed ? Tag.SHAPE_DRAW_BITMAP_COMMAND : Tag.SHAPE_DRAW_BITMAP_COMMAND_3;
    }

    /**
     * @since 1.0.9
     * */
    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("unused")
    public static final class Builder {
        private final List<ShapePoint> points = new ArrayList<>();
        private int textureIndex;

        private Builder() {
        }

        public Builder withTextureIndex(int textureIndex) {
            if (textureIndex == -1) {
                throw new IllegalArgumentException("Texture index must be set");
            }

            if (textureIndex < 0) {
                throw new IllegalArgumentException("Texture index cannot be negative");
            }

            this.textureIndex = textureIndex;
            return this;
        }

        public Builder addPoint(ShapePoint point) {
            if (points.size() >= 255) {
                // NOTE: make flatbuffer saving and enforce user to use it for more points
                throw new IllegalArgumentException("Too many points: " + (points.size()));
            }

            points.add(point);
            return this;
        }

        public ShapeDrawBitmapCommand build() {
            return new ShapeDrawBitmapCommand(textureIndex, this.points);
        }
    }
}
