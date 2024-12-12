package com.vorono4ka.swf.shapes;

import com.supercell.swf.FBResources;
import com.supercell.swf.FBShape;
import com.vorono4ka.streams.ByteStream;
import com.vorono4ka.swf.DisplayObjectOriginal;
import com.vorono4ka.swf.Tag;
import com.vorono4ka.swf.exceptions.NegativeTagLengthException;
import com.vorono4ka.swf.exceptions.UnsupportedTagException;
import com.vorono4ka.swf.textures.SWFTexture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class ShapeOriginal extends DisplayObjectOriginal {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShapeOriginal.class);

    private Tag tag;

    private int id;
    private List<ShapeDrawBitmapCommand> commands;

    public ShapeOriginal() {
    }

    public ShapeOriginal(FBShape fb, FBResources resources) {
        id = fb.id();

        commands = new ArrayList<>(fb.commandsLength());
        for (int i = 0; i < fb.commandsLength(); i++) {
            commands.add(new ShapeDrawBitmapCommand(fb.commands(i), resources));
        }

        tag = determineTag();
    }

    private Tag determineTag() {
        boolean onlyQuadCommands = commands.stream().allMatch(shapeDrawBitmapCommand -> shapeDrawBitmapCommand.getTag() == Tag.SHAPE_DRAW_BITMAP_COMMAND);

        return onlyQuadCommands ? Tag.SHAPE : Tag.SHAPE_2;
    }

    public int load(ByteStream stream, Tag tag, Function<Integer, SWFTexture> imageFunction, String filename) throws NegativeTagLengthException {
        this.tag = tag;

        this.id = stream.readShort();
        int commandCount = stream.readShort();

        this.commands = new ArrayList<>(commandCount);
        for (int i = 0; i < commandCount; i++) {
            this.commands.add(new ShapeDrawBitmapCommand());
        }

        // Used for allocating memory for points
        int pointCount = 4 * commandCount;
        if (tag == Tag.SHAPE_2) {
            pointCount = stream.readShort();
        }

        int loadedCommands = 0;

        while (true) {
            int commandTag = stream.readUnsignedChar();
            int length = stream.readInt();

            if (length < 0) {
                throw new NegativeTagLengthException(String.format("Negative tag length in Shape. Tag %d, %s", commandTag, filename));
            }

            int startPosition = stream.getPosition();

            Tag tagValue = Tag.values()[commandTag];
            switch (tagValue) {
                case EOF -> {
                    return this.id;
                }
                case SHAPE_DRAW_BITMAP_COMMAND, SHAPE_DRAW_BITMAP_COMMAND_2,
                     SHAPE_DRAW_BITMAP_COMMAND_3 ->
                    this.commands.get(loadedCommands++).load(stream, tagValue);
                case SHAPE_DRAW_COLOR_FILL_COMMAND -> {
                    try {
                        throw new UnsupportedTagException(String.format("SupercellSWF::TAG_SHAPE_DRAW_COLOR_FILL_COMMAND not supported, %s", filename));
                    } catch (UnsupportedTagException exception) {
                        LOGGER.error(exception.getMessage(), exception);
                    }
                }
                default -> {
                    try {
                        throw new UnsupportedTagException(String.format("Unknown tag %d in Shape, %s", commandTag, filename));
                    } catch (UnsupportedTagException exception) {
                        LOGGER.error(exception.getMessage(), exception);
                    }

                    if (length > 0) {
                        stream.skip(length);
                    }
                }
            }

            int position = stream.getPosition();
            if (position - startPosition != length){
                throw new IllegalStateException("Read bytes amount doesn't equal to " + length + " vs " + (position - startPosition) + ". Tag " + tag);
            }
        }
    }

    @Override
    public void save(ByteStream stream) {
        stream.writeShort(this.id);
        stream.writeShort(this.commands.size());

        if (this.tag != Tag.SHAPE) {
            stream.writeShort(calculatePointCount());
        }

        for (ShapeDrawBitmapCommand command : this.commands) {
            stream.writeSavable(command);
        }

        stream.writeBlock(Tag.EOF, null);
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public Tag getTag() {
        return tag;
    }

    public List<ShapeDrawBitmapCommand> getCommands() {
        return Collections.unmodifiableList(this.commands);
    }

    private int calculatePointCount() {
        int pointCount = 0;
        for (ShapeDrawBitmapCommand command : this.commands) {
            pointCount += command.getVertexCount();
        }

        return pointCount;
    }
}
