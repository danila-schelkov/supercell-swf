package dev.donutquine.swf;

import dev.donutquine.swf.movieclips.MovieClipFrameElement;
import dev.donutquine.utilities.BitUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class ExternalMovieClipFrameElementDecoder {
    /// Used to copy matrices without modification.
    ///
    /// Saved in class instance to resume parsing in case of matrix array underflow.
    private int unmodifiedElementMask;

    public ExternalMovieClipFrameElementDecoder() {
        unmodifiedElementMask = 0;
    }

    public List<List<MovieClipFrameElement>> decodeMovieClipFrames(ByteBuffer buffer, int movieClipDataPosition) {
        buffer.position(movieClipDataPosition);

        int frameCount = buffer.getInt();
        int elementCount = getShort(buffer);
        int u3 = getShort(buffer);

        List<List<MovieClipFrameElement>> frames = new ArrayList<>(frameCount);
        for (int i = 0; i < frameCount; i++) {
            frames.add(i, decodeFrame(buffer, movieClipDataPosition));
        }

        return frames;
    }

    private List<MovieClipFrameElement> decodeFrame(ByteBuffer buffer, int movieClipDataPosition) {
        int frameDataOffset = buffer.getInt();
        // measured in shorts, so to set position multiply by 2
        int frameElementDataIndex = getShort(buffer);
        int frameElementDataEndIndex = getShort(buffer);

        int frameDataPosition = movieClipDataPosition + frameDataOffset;

        ByteBuffer frameDataBuffer = buffer.duplicate();
        frameDataBuffer.order(ByteOrder.LITTLE_ENDIAN);
        frameDataBuffer.position(frameDataPosition);

        ByteBuffer frameElementDataBuffer = buffer.duplicate();
        frameElementDataBuffer.order(ByteOrder.LITTLE_ENDIAN);
        frameElementDataBuffer.position(frameDataPosition + frameElementDataIndex * 2);

        List<MovieClipFrameElement> frameElements;

        if (frameElementDataIndex == 0) {
            int frameElementCount = (frameElementDataEndIndex - frameElementDataIndex) / 3;
            frameElements = new ArrayList<>(frameElementCount);
            for (int j = 0; j < frameElementCount; j++) {
                frameElements.add(new MovieClipFrameElement(getShort(frameDataBuffer), getShort(frameDataBuffer), getShort(frameDataBuffer)));
            }
        } else {
            frameElements = decodeFrameElements(frameDataPosition + frameElementDataEndIndex * 2, frameElementDataBuffer, frameDataBuffer);
        }

        return frameElements;
    }

    private List<MovieClipFrameElement> decodeFrameElements(int endIndex, ByteBuffer frameDataBuffer, ByteBuffer frameElementDataBuffer) {
        List<MovieClipFrameElement> frameElements = new ArrayList<>();

        while (frameDataBuffer.position() < endIndex) {
            if ((unmodifiedElementMask & 1) != 0) {
                frameElements.add(new MovieClipFrameElement(
                    getShort(frameElementDataBuffer),
                    getShort(frameElementDataBuffer),
                    getShort(frameElementDataBuffer)
                ));
                unmodifiedElementMask >>= 1;
                continue;
            }

            unmodifiedElementMask >>= 1;

            int metadata = getShort(frameDataBuffer);

            if (BitUtils.getUnsignedBitInteger(metadata, 0, 2) != 0) {
                switch (BitUtils.getUnsignedBitInteger(metadata, 0, 3)) {
                    case 1 -> {
                        frameElements.add(new MovieClipFrameElement(
                            getShort(frameElementDataBuffer),
                            (getShort(frameElementDataBuffer) + BitUtils.getBitInteger(metadata, 3, 13)) & 0xFFFF,
                            getShort(frameElementDataBuffer)
                        ));
                    }
                    case 2 -> {
                        frameElements.add(new MovieClipFrameElement(
                            getShort(frameElementDataBuffer),
                            (getShort(frameElementDataBuffer) + BitUtils.getBitInteger(metadata, 3, 4)) & 0xFFFF,  // (((metadata << 9) & 0xFFFF) >> 12),
                            (getShort(frameElementDataBuffer) + BitUtils.getBitInteger(metadata, 7, 9)) & 0xFFFF
                        ));
                    }
                    case 3 -> {
                        frameElements.add(new MovieClipFrameElement(
                            getShort(frameElementDataBuffer),
                            (getShort(frameElementDataBuffer) + getShort(frameDataBuffer)) & 0xFFFF,
                            (getShort(frameElementDataBuffer) + BitUtils.getBitInteger(metadata, 3, 13)) & 0xFFFF
                        ));
                    }
                    // 4 is impossible
                    case 5 -> {
                        frameElements.add(new MovieClipFrameElement(
                            getShort(frameElementDataBuffer),
                            getShort(frameElementDataBuffer),
                            getShort(frameElementDataBuffer)
                        ));
                        unmodifiedElementMask = BitUtils.getUnsignedBitInteger(metadata, 3, 13);
                    }
                    case 6 -> {
                        int elementsToSkip = BitUtils.getBitInteger(metadata, 3, 13);
                        frameElementDataBuffer.position(frameElementDataBuffer.position() + elementsToSkip * 6);
                    }
                    case 7 -> {
                        int elementsToSkip = BitUtils.getUnsignedBitInteger(metadata, 15, 1);  // or just (metadata & 0x8000) != 0
                        frameElementDataBuffer.position(frameElementDataBuffer.position() + 6 * elementsToSkip);

                        int childIndex = BitUtils.getBitInteger(metadata, 3, 12); // ((metadata << 17) >> 20) & 0xFFFF;
                        int matrixIndex = getShort(frameDataBuffer);
                        int transformIndex = getShort(frameDataBuffer);

                        frameElements.add(new MovieClipFrameElement(childIndex, matrixIndex, transformIndex));
                    }
                }
            } else {
                frameElements.add(new MovieClipFrameElement(getShort(frameElementDataBuffer), (getShort(frameElementDataBuffer) + BitUtils.getBitInteger(metadata, 2, 7)) & 0xFFFF, getShort(frameElementDataBuffer)));
                frameElements.add(new MovieClipFrameElement(getShort(frameElementDataBuffer), (getShort(frameElementDataBuffer) + BitUtils.getBitInteger(metadata, 9, 7)) & 0xFFFF, getShort(frameElementDataBuffer)));

                unmodifiedElementMask >>= 1;
            }
        }

        while (unmodifiedElementMask != 0) {
            frameElements.add(new MovieClipFrameElement(getShort(frameElementDataBuffer), getShort(frameElementDataBuffer), getShort(frameElementDataBuffer)));
            unmodifiedElementMask >>= 1;
        }

        return frameElements;
    }

    private static int getShort(ByteBuffer buffer) {
        return buffer.getShort() & 0xFFFF;
    }
}
