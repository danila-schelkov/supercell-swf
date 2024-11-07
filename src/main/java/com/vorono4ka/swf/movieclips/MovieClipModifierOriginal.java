package com.vorono4ka.swf.movieclips;

import com.supercell.swf.FBMovieClipModifier;
import com.vorono4ka.streams.ByteStream;
import com.vorono4ka.swf.DisplayObjectOriginal;
import com.vorono4ka.swf.Tag;

public class MovieClipModifierOriginal extends DisplayObjectOriginal {
    private Tag tag;
    private int id;

    public MovieClipModifierOriginal() {
    }

    public MovieClipModifierOriginal(FBMovieClipModifier fb) {
        id = fb.id();
        tag = Tag.values()[fb.tag()];
    }

    public int load(ByteStream stream, Tag tag) {
        this.tag = tag;
        this.id = stream.readShort();
        return this.id;
    }

    public void save(ByteStream stream) {
        stream.writeShort(this.id);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public Tag getTag() {
        return tag;
    }

    @Override
    public String toString() {
        return "MovieClipModifierOriginal{" +
            "tag=" + tag +
            ", id=" + id +
            '}';
    }
}
