package dev.donutquine.swf.movieclips;

import com.supercell.swf.FBMovieClipModifier;
import dev.donutquine.streams.ByteStream;
import dev.donutquine.swf.DisplayObjectOriginal;
import dev.donutquine.swf.Tag;

public class MovieClipModifierOriginal extends DisplayObjectOriginal {
    private Tag tag;

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
