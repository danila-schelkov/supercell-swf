package com.vorono4ka.swf.textfields;

import com.supercell.swf.FBResources;
import com.supercell.swf.FBTextField;
import com.vorono4ka.math.ShortRect;
import com.vorono4ka.streams.ByteStream;
import com.vorono4ka.swf.DisplayObjectOriginal;
import com.vorono4ka.swf.Tag;

import java.util.function.Function;

public class TextFieldOriginal extends DisplayObjectOriginal {
    private Tag tag;

    private String fontName;

    private ShortRect bounds;

    private int color;
    private int outlineColor;

    private String defaultText;
    private String anotherText;

    private boolean useDeviceFont;  // styles | 1
    private boolean isOutlineEnabled;  // styles | 2
    private boolean isBold;  // styles | 4
    private boolean isItalic;  // styles | 8
    private boolean isMultiline;  // styles | 16
    private boolean unkBoolean;  // styles | 32
    private boolean autoAdjustFontSize;  // styles | 64

    private byte align;
    private byte fontSize;

    private int unk32;
    private short bendAngle;

    public TextFieldOriginal() {
    }

    public TextFieldOriginal(FBTextField fb, FBResources resources) {
        id = fb.id();
        fontName = resources.strings(fb.fontNameRefId());
        bounds = new ShortRect(fb.left(), fb.top(), fb.right(), fb.bottom());
        color = fb.color();
        outlineColor = fb.outlineColor();
        defaultText = resources.strings(fb.defaultTextRefId());
        anotherText = resources.strings(fb.anotherTextRefId());
        align = (byte) fb.align();
        fontSize = (byte) fb.fontSize();
        setStyles((byte) fb.styles());

        this.tag = determineTag();
    }

    public void setStyles(byte styles) {
        useDeviceFont = (styles & 0x1) != 0;
        isOutlineEnabled = (styles & 0x2) != 0;
        isBold = (styles & 0x4) != 0;
        isItalic = (styles & 0x8) != 0;
        isMultiline = (styles & 0x10) != 0;
        unkBoolean = (styles & 0x20) != 0;
        autoAdjustFontSize = (styles & 0x40) != 0;
    }

    public int load(ByteStream stream, Tag tag, Function<ByteStream, String> fontNameReader) {
        this.tag = tag;

        this.id = stream.readShort();
        this.fontName = fontNameReader.apply(stream);
        this.color = stream.readInt();

        this.isBold = stream.readBoolean();
        this.isItalic = stream.readBoolean();
        this.isMultiline = stream.readBoolean();  // unused since BS v58

        stream.readBoolean();  // unused

        this.align = (byte) stream.readUnsignedChar();
        this.fontSize = (byte) stream.readUnsignedChar();

        this.bounds = new ShortRect(
            (short) stream.readShort(),
            (short) stream.readShort(),
            (short) stream.readShort(),
            (short) stream.readShort()
        );

        this.isOutlineEnabled = stream.readBoolean();

        this.defaultText = stream.readAscii();

        if (tag == Tag.TEXT_FIELD) {
            return this.id;
        }

        this.useDeviceFont = stream.readBoolean();

        switch (tag) {
            case TEXT_FIELD_2 -> {
                return this.id;
            }
            case TEXT_FIELD_3 -> {
                this.unkBoolean = true;
                return this.id;
            }
            case TEXT_FIELD_4 -> {
                this.unkBoolean = true;
                this.outlineColor = stream.readInt();
                return this.id;
            }
            case TEXT_FIELD_5 -> {
                this.outlineColor = stream.readInt();
                return this.id;
            }
            case TEXT_FIELD_6, TEXT_FIELD_7, TEXT_FIELD_8, TEXT_FIELD_9 -> {
                this.outlineColor = stream.readInt();
                this.unk32 = stream.readShort();
                stream.readShort();  // unused

                this.unkBoolean = true;

                if (tag == Tag.TEXT_FIELD_6) {
                    return this.id;
                }

                this.bendAngle = (short) stream.readShort();

                if (tag == Tag.TEXT_FIELD_7) {
                    return this.id;
                }

                this.autoAdjustFontSize = stream.readBoolean();

                if (tag == Tag.TEXT_FIELD_8) {
                    return this.id;
                }

                this.anotherText = stream.readAscii();

                return this.id;
            }
        }

        return this.id;
    }

    @Override
    public void save(ByteStream stream) {
        stream.writeShort(this.id);
        stream.writeAscii(this.fontName);
        stream.writeInt(this.color);

        stream.writeBoolean(this.isBold);
        stream.writeBoolean(this.isItalic);
        stream.writeBoolean(this.isMultiline);

        stream.writeBoolean(false);  // unused

        stream.writeUnsignedChar(this.align);
        stream.writeUnsignedChar(this.fontSize);

        stream.writeShort(this.bounds.left());
        stream.writeShort(this.bounds.top());
        stream.writeShort(this.bounds.right());
        stream.writeShort(this.bounds.bottom());

        stream.writeBoolean(this.isOutlineEnabled);

        stream.writeAscii(this.defaultText);

        if (this.tag == Tag.TEXT_FIELD) return;

        stream.writeBoolean(this.useDeviceFont);

        if (this.tag == Tag.TEXT_FIELD_2 || this.tag == Tag.TEXT_FIELD_3) return;

        stream.writeInt(this.outlineColor);

        if (this.tag == Tag.TEXT_FIELD_4 || this.tag == Tag.TEXT_FIELD_5) return;

        stream.writeShort(this.unk32);
        stream.writeShort(0);  // unused

        if (this.tag == Tag.TEXT_FIELD_6) return;

        stream.writeShort(this.bendAngle);

        if (this.tag == Tag.TEXT_FIELD_7) return;

        stream.writeBoolean(this.autoAdjustFontSize);

        if (this.tag == Tag.TEXT_FIELD_8) return;

        stream.writeAscii(this.anotherText);
    }

    @Override
    public Tag getTag() {
        return tag;
    }

    public float getBendAngle() {
        return (float) bendAngle / Short.MAX_VALUE * 360f;
    }

    public void getBendAngle(float bendAngle) {
        this.bendAngle = (short) (bendAngle * Short.MAX_VALUE / 360f);
    }

    private Tag determineTag() {
        Tag tag = Tag.TEXT_FIELD;
        if (this.useDeviceFont) {
            tag = Tag.TEXT_FIELD_2;
        }

        if (!this.unkBoolean) {
            if (this.outlineColor != 0) {
                return Tag.TEXT_FIELD_5;
            }

            return tag;
        }

        tag = Tag.TEXT_FIELD_3;

        if (this.outlineColor != 0) {
            tag = Tag.TEXT_FIELD_4;
        }

        if (this.unk32 != 0) {
            tag = Tag.TEXT_FIELD_6;
        }

        if (this.bendAngle != 0) {
            tag = Tag.TEXT_FIELD_7;
        }

        if (this.autoAdjustFontSize) {
            tag = Tag.TEXT_FIELD_8;
        }

        if (this.anotherText != null) {
            tag = Tag.TEXT_FIELD_9;
        }

        return tag;
    }
}
