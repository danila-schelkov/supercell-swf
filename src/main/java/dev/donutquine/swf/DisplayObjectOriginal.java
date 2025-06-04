package dev.donutquine.swf;

public abstract class DisplayObjectOriginal implements Savable {
    protected int id;

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
