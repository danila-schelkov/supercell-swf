package com.vorono4ka.math;

import java.util.function.IntFunction;

public final class Rendering {
    private Rendering() {
    }

    public static final IntFunction<int[]> TRIANGULATOR_FUNCTION_1 = (triangleCount) -> {
        int[] indices = new int[triangleCount * 3];
        for (int i = 0; i < triangleCount; i++) {
            indices[i * 3] = 0;
            indices[i * 3 + 1] = i + 1;
            indices[i * 3 + 2] = i + 2;
        }
        return indices;
    };

    public static final IntFunction<int[]> TRIANGULATOR_FUNCTION_2 = (triangleCount) -> {
        int[] indices = new int[triangleCount * 3];
        for (int i = 0; i < triangleCount; i++) {
            indices[i * 3] = i;
            indices[i * 3 + 1] = i + 1;
            indices[i * 3 + 2] = i + 2;
        }
        return indices;
    };
}
