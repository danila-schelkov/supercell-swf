package com.vorono4ka;

@FunctionalInterface
public interface ProgressTracker {
    void setProgress(long current, long max);
}
