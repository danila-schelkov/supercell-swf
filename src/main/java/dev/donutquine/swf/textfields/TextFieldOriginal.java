package dev.donutquine.swf.textfields;

import com.supercell.swf.FBResources;
import com.supercell.swf.FBTextField;
import dev.donutquine.math.Rect;
import dev.donutquine.streams.ByteStream;
import dev.donutquine.swf.DisplayObjectOriginal;
import dev.donutquine.swf.Tag;

import java.util.function.Function;

public class TextFieldOriginal extends DisplayObjectOriginal {
    private Tag tag;

    private String fontName;

    private Rect bounds;

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
        fontName = fb.fontNameRefId() != 0 ? resources.strings(fb.fontNameRefId()) : null;
        bounds = new Rect(fb.left(), fb.top(), fb.right(), fb.bottom());
        color = fb.color();
        outlineColor = fb.outlineColor();
        defaultText = fb.defaultTextRefId() != 0 ? resources.strings(fb.defaultTextRefId()) : null;
        anotherText = fb.anotherTextRefId() != 0 ? resources.strings(fb.anotherTextRefId()) : null;
        align = (byte) fb.align();
        fontSize = (byte) fb.fontSize();
        setStyles((byte) fb.styles());

        this.tag = determineTag();
    }

    private TextFieldOriginal(Builder builder) {
        this.fontName = builder.fontName;
        this.bounds = builder.bounds;
        this.color = builder.color;
        this.outlineColor = builder.outlineColor;
        this.defaultText = builder.defaultText;
        this.anotherText = builder.anotherText;
        this.useDeviceFont = builder.useDeviceFont;
        this.isOutlineEnabled = builder.isOutlineEnabled;
        this.isBold = builder.isBold;
        this.isItalic = builder.isItalic;
        this.isMultiline = builder.isMultiline;
        this.unkBoolean = builder.unkBoolean;
        this.autoAdjustFontSize = builder.autoAdjustFontSize;
        this.align = builder.align;
        this.fontSize = builder.fontSize;
        this.unk32 = builder.unk32;
        this.bendAngle = builder.bendAngle;

        this.tag = this.determineTag();
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

        this.bounds = new Rect(
            stream.readShort(),
            stream.readShort(),
            stream.readShort(),
            stream.readShort()
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

        // Note: maybe not to use float Rect for field but return and let set using it
        stream.writeShort((int) this.bounds.getLeft());
        stream.writeShort((int) this.bounds.getTop());
        stream.writeShort((int) this.bounds.getRight());
        stream.writeShort((int) this.bounds.getBottom());

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

    public void setBendAngle(float bendAngle) {
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

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("unused")
    public static final class Builder {
        private String fontName;

        private Rect bounds;

        private int color;
        private int outlineColor;

        private String defaultText;
        private String anotherText;

        private boolean useDeviceFont;
        private boolean isOutlineEnabled;
        private boolean isBold;
        private boolean isItalic;
        private boolean isMultiline;
        private boolean unkBoolean;
        private boolean autoAdjustFontSize;

        private byte align;
        private byte fontSize;

        private int unk32;
        private short bendAngle;

        private Builder() {}

        public Builder withFontName(String fontName) {
            this.fontName = fontName;
            return this;
        }

        public Builder withBounds(int left, int top, int right, int bottom) {
            this.bounds = new Rect(left, top, right, bottom);
            return this;
        }

        public Builder withColor(int color) {
            this.color = color;
            return this;
        }

        public Builder withOutlineColor(int outlineColor) {
            this.outlineColor = outlineColor;
            return this;
        }

        public Builder withDefaultText(String defaultText) {
            this.defaultText = defaultText;
            return this;
        }

        public Builder withAnotherText(String anotherText) {
            this.anotherText = anotherText;
            return this;
        }

        public Builder withUseDeviceFont(boolean useDeviceFont) {
            this.useDeviceFont = useDeviceFont;
            return this;
        }

        public Builder withOutlineEnabled(boolean outlineEnabled) {
            isOutlineEnabled = outlineEnabled;
            return this;
        }

        public Builder withBold(boolean bold) {
            isBold = bold;
            return this;
        }

        public Builder withItalic(boolean italic) {
            isItalic = italic;
            return this;
        }

        public Builder withMultiline(boolean multiline) {
            isMultiline = multiline;
            return this;
        }

        public Builder withUnkBoolean(boolean unkBoolean) {
            this.unkBoolean = unkBoolean;
            return this;
        }

        public Builder withAutoAdjustFontSize(boolean autoAdjustFontSize) {
            this.autoAdjustFontSize = autoAdjustFontSize;
            return this;
        }

        public Builder withAlign(byte align) {
            this.align = align;
            return this;
        }

        public Builder withFontSize(byte fontSize) {
            this.fontSize = fontSize;
            return this;
        }

        public Builder withUnk32(int unk32) {
            this.unk32 = unk32;
            return this;
        }

        public Builder withBendAngle(short bendAngle) {
            this.bendAngle = bendAngle;
            return this;
        }

        public TextFieldOriginal build() {
            return new TextFieldOriginal(this);
        }
    }
}
