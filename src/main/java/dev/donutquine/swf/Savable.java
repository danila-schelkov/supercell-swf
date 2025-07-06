package dev.donutquine.swf;

import dev.donutquine.streams.ByteStream;

public interface Savable {
    void save(ByteStream stream);

    Tag getTag();
}
