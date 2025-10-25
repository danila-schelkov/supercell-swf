package dev.donutquine.swf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScMatrixBank {
    // A little obscure name, actually last index
    public static final int MAX_MATRIX_CAPACITY = 0xFFFF;
    public static final int MAX_COLOR_CAPACITY = 0xFFFF;

    private List<Matrix2x3> matrices;
    private List<ColorTransform> colorTransforms;

    public ScMatrixBank() {
        this(0, 0);
    }

    public ScMatrixBank(int matrixCount, int colorTransformCount) {
        this.init(matrixCount, colorTransformCount);
    }

    public void init(int matrixCount, int colorTransformCount) {
        assert matrixCount > 0 && matrixCount <= MAX_MATRIX_CAPACITY;
        assert colorTransformCount > 0 && colorTransformCount <= MAX_COLOR_CAPACITY;

        this.matrices = new ArrayList<>(matrixCount);
        for (int i = 0; i < matrixCount; i++) {
            this.matrices.add(new Matrix2x3());
        }

        this.colorTransforms = new ArrayList<>(colorTransformCount);
        for (int i = 0; i < colorTransformCount; i++) {
            this.colorTransforms.add(new ColorTransform());
        }
    }

    public void addMatrix(Matrix2x3 matrix) {
        this.matrices.add(matrix);
    }

    public void setMatrix(int index, Matrix2x3 matrix) {
        this.matrices.set(index, matrix);
    }

    public void addColorTransform(ColorTransform colorTransform) {
        this.colorTransforms.add(colorTransform);
    }

    public void setColorTransform(int index, ColorTransform colorTransform) {
        this.colorTransforms.set(index, colorTransform);
    }

    public List<Matrix2x3> getMatrices() {
        return Collections.unmodifiableList(this.matrices);
    }

    public List<ColorTransform> getColorTransforms() {
        return Collections.unmodifiableList(colorTransforms);
    }

    public Matrix2x3 getMatrix(int index) {
        return this.matrices.get(index);
    }

    public ColorTransform getColorTransform(int index) {
        return this.colorTransforms.get(index);
    }

    public int getMatrixCount() {
        return this.matrices.size();
    }

    public int getColorTransformCount() {
        return this.colorTransforms.size();
    }
}
