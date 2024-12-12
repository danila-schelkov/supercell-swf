package com.vorono4ka.swf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScMatrixBank {
    private List<Matrix2x3> matrices;
    private List<ColorTransform> colorTransforms;

    public void init(int matrixCount, int colorTransformCount) {
        this.matrices = new ArrayList<>(matrixCount);
        for (int i = 0; i < matrixCount; i++) {
            this.matrices.add(new Matrix2x3());
        }

        this.colorTransforms = new ArrayList<>(colorTransformCount);
        for (int i = 0; i < colorTransformCount; i++) {
            this.colorTransforms.add(new ColorTransform());
        }
    }

    public void clearMatrices() {
        this.matrices = new ArrayList<>();
    }

    public void addMatrix(Matrix2x3 matrix) {
        this.matrices.add(matrix);
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
