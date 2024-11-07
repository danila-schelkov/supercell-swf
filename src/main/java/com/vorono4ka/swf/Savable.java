package com.vorono4ka.swf;

import com.vorono4ka.streams.ByteStream;

public interface Savable {
    void save(ByteStream stream);

    Tag getTag();
}
