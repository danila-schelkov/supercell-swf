package dev.donutquine;

@FunctionalInterface
public interface ProgressTracker {
    void setProgress(long current, long max);
}
