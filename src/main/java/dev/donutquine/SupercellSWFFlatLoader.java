package dev.donutquine;

import com.supercell.swf.*;
import dev.donutquine.swf.Export;
import dev.donutquine.swf.Matrix2x3;
import dev.donutquine.swf.ScCompressedMatrixBank;
import dev.donutquine.swf.ScMatrixBank;
import dev.donutquine.swf.file.compression.Zstandard;
import dev.donutquine.swf.movieclips.MovieClipModifierOriginal;
import dev.donutquine.swf.movieclips.MovieClipOriginal;
import dev.donutquine.swf.shapes.ShapeOriginal;
import dev.donutquine.swf.textfields.TextFieldOriginal;
import dev.donutquine.swf.textures.SWFTexture;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class SupercellSWFFlatLoader {
    // Actually, table of interners (like String.intern)
    public final FBResources resources;
    public final List<ScMatrixBank> matrixBanks;
    public final List<Export> exports;
    public final List<TextFieldOriginal> textFields;
    public final List<ShapeOriginal> shapes;
    public final List<MovieClipOriginal> movieClips;
    public final List<MovieClipModifierOriginal> modifiers;
    public final List<SWFTexture> textures;

    public SupercellSWFFlatLoader(InputStream inputStream, boolean preferLowres) throws IOException {
        this(inputStream.readAllBytes(), preferLowres);
    }

    public SupercellSWFFlatLoader(byte[] data, boolean preferLowres) {
        // Note: Actually, it is possible to parse without metadata,
        // but you have to look for each zstd frame size

        // Metadata
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        Metadata metadata = Metadata.getRootAsMetadata(getNestedFlatbufferBytes(byteBuffer));

        byte[] decompressed = Zstandard.decompress(data, byteBuffer.position());

        // Main container
        ByteBuffer mainBuffer = ByteBuffer.wrap(decompressed);
        mainBuffer.order(ByteOrder.LITTLE_ENDIAN);

        this.resources = FBResources.getRootAsFBResources(getNestedFlatbufferBytes(mainBuffer));

        ExternalMatrixBanks externalMatrixBanks = getExternalMatrixBanks(metadata, byteBuffer);
        if (externalMatrixBanks == null) {
            this.matrixBanks = deserializeMatrixBanks();
        } else {
            int matrixBankDataPosition = byteBuffer.position();
            this.matrixBanks = deserializeExternalMatrixBanks(externalMatrixBanks, data, matrixBankDataPosition);
        }

        this.exports = deserializeExports(getNestedFlatbufferBytes(mainBuffer));
        this.textFields = deserializeTextFields(getNestedFlatbufferBytes(mainBuffer));
        this.shapes = deserializeShapes(getNestedFlatbufferBytes(mainBuffer));
        this.movieClips = deserializeMovieClips(getNestedFlatbufferBytes(mainBuffer));
        this.modifiers = deserializeModifiers(getNestedFlatbufferBytes(mainBuffer));
        this.textures = deserializeTextures(getNestedFlatbufferBytes(mainBuffer), preferLowres);
    }

    private static List<SWFTexture> deserializeTextures(ByteBuffer chunkBuffer, boolean preferLowres) {
        FBTextureSets fbTextureSets = FBTextureSets.getRootAsFBTextureSets(chunkBuffer);

        List<SWFTexture> textures = new ArrayList<>(fbTextureSets.textureSetsLength());
        for (int i = 0; i < fbTextureSets.textureSetsLength(); i++) {
            FBTextureSet fbTextureSet = fbTextureSets.textureSets(i);
            FBTexture fbHighresTexture = fbTextureSet.highresTexture();
            FBTexture fbLowresTexture = fbTextureSet.lowresTexture();

            FBTexture fbTexture;

            if ((fbHighresTexture == null || preferLowres) && fbLowresTexture != null) {
                fbTexture = fbLowresTexture;
            } else if (fbHighresTexture != null) {
                fbTexture = fbHighresTexture;
            } else {
                throw new IllegalArgumentException("FBTextureSet doesn't contain any textures.");
            }

            textures.add(new SWFTexture(fbTexture));
        }

        return textures;
    }

    private List<MovieClipModifierOriginal> deserializeModifiers(ByteBuffer chunkBuffer) {
        FBMovieClipModifiers fbModifiers = FBMovieClipModifiers.getRootAsFBMovieClipModifiers(chunkBuffer);
        List<MovieClipModifierOriginal> modifiers = new ArrayList<>(fbModifiers.modifiersLength());
        for (int i = 0; i < fbModifiers.modifiersLength(); i++) {
            modifiers.add(new MovieClipModifierOriginal(fbModifiers.modifiers(i)));
        }
        return modifiers;
    }

    private List<MovieClipOriginal> deserializeMovieClips(ByteBuffer chunkBuffer) {
        FBMovieClips fbMovieClips = FBMovieClips.getRootAsFBMovieClips(chunkBuffer);

        List<MovieClipOriginal> movieClips = new ArrayList<>(fbMovieClips.clipsLength());

        for (int i = 0; i < fbMovieClips.clipsLength(); i++) {
            movieClips.add(new MovieClipOriginal(fbMovieClips.clips(i), resources));
        }

        return movieClips;
    }

    private List<ShapeOriginal> deserializeShapes(ByteBuffer chunkBuffer) {
        FBShapes fbShapes = FBShapes.getRootAsFBShapes(chunkBuffer);
        List<ShapeOriginal> shapes = new ArrayList<>(fbShapes.shapesLength());
        for (int i = 0; i < fbShapes.shapesLength(); i++) {
            shapes.add(new ShapeOriginal(fbShapes.shapes(i), resources));
        }
        return shapes;
    }

    private List<TextFieldOriginal> deserializeTextFields(ByteBuffer chunkBuffer) {
        FBTextFields fbTextFields = FBTextFields.getRootAsFBTextFields(chunkBuffer);
        List<TextFieldOriginal> textFields = new ArrayList<>(fbTextFields.textFieldsLength());
        for (int i = 0; i < fbTextFields.textFieldsLength(); i++) {
            textFields.add(new TextFieldOriginal(fbTextFields.textFields(i), resources));
        }
        return textFields;
    }

    private List<Export> deserializeExports(ByteBuffer chunkBuffer) {
        FBExports fbExports = FBExports.getRootAsFBExports(chunkBuffer);
        if (fbExports.exportIdsLength() != fbExports.exportNameIdsLength()) {
            throw new IllegalArgumentException("Export ids and name ids count must be equal!");
        }

        List<Export> exports = new ArrayList<>(fbExports.exportIdsLength());
        for (int i = 0; i < fbExports.exportIdsLength(); i++) {
            int id = fbExports.exportIds(i);
            int exportNameId = fbExports.exportNameIds(i);

            if (exportNameId == 0) {
                throw new RuntimeException("Export name not found! Movie clip id: " + id);
            }

            exports.add(new Export(id, resources.strings(exportNameId)));
        }

        return exports;
    }

    private List<ScMatrixBank> deserializeMatrixBanks() {
        List<ScMatrixBank> matrixBanks = new ArrayList<>(resources.matrixBanksLength());
        for (int i = 0; i < resources.matrixBanksLength(); i++) {
            FBMatrixBank fbMatrixBank = resources.matrixBanks(i);

            int matrixCount = fbMatrixBank.matricesLength();
            if (matrixCount == 0) {
                matrixCount = fbMatrixBank.shortMatricesLength();
            }

            ScMatrixBank matrixBank = new ScMatrixBank(matrixCount, fbMatrixBank.colorTransformsLength());

            if (fbMatrixBank.matricesLength() > 0) {
                for (int j = 0; j < matrixCount; j++) {
                    FBMatrix2x3 fbMatrix2x3 = fbMatrixBank.matrices(j);
                    matrixBank.getMatrix(j).set(fbMatrix2x3.a(), fbMatrix2x3.b(), fbMatrix2x3.c(), fbMatrix2x3.d(), fbMatrix2x3.x(), fbMatrix2x3.y());
                }
            } else {
                for (int j = 0; j < fbMatrixBank.shortMatricesLength(); j++) {
                    FBShortMatrix2x3 fbMatrix2x3 = fbMatrixBank.shortMatrices(j);
                    matrixBank.getMatrix(j).set(fbMatrix2x3.a(), fbMatrix2x3.b(), fbMatrix2x3.c(), fbMatrix2x3.d(), fbMatrix2x3.x(), fbMatrix2x3.y());
                }
            }

            for (int j = 0; j < fbMatrixBank.colorTransformsLength(); j++) {
                FBColorTransform fbColorTransform = fbMatrixBank.colorTransforms(j);
                matrixBank.getColorTransform(j).set(fbColorTransform.r(), fbColorTransform.g(), fbColorTransform.b(), fbColorTransform.a(), fbColorTransform.ra(), fbColorTransform.ga(), fbColorTransform.ba());
            }

            matrixBanks.add(matrixBank);
        }

        return matrixBanks;
    }

    private static ExternalMatrixBanks getExternalMatrixBanks(Metadata metadata, ByteBuffer byteBuffer) {
        if (metadata.externalMatrixBanksSize() != 0) {
            if (metadata.compressedSize() == 0) {
                throw new IllegalStateException("compressed size is unknown");
            }

            byteBuffer.position((int) (byteBuffer.position() + metadata.compressedSize()));
            return ExternalMatrixBanks.getRootAsExternalMatrixBanks(getNestedFlatbufferBytes(byteBuffer));
        }

        return null;
    }

    private static List<ScMatrixBank> deserializeExternalMatrixBanks(ExternalMatrixBanks externalMatrixBanks, byte[] data, int matrixBankDataPosition) {
        List<ScMatrixBank> matrixBanks = new ArrayList<>();

        for (int i = 0; i < externalMatrixBanks.matrixBanksLength(); i++) {
            ExternalMatrixBank externalMatrixBank = externalMatrixBanks.matrixBanks(i);

            int uncompressedMatrixCount = (int) (externalMatrixBank.floatMatrixCount() + externalMatrixBank.shortMatrixCount());
            int totalMatrixCount = uncompressedMatrixCount;
            totalMatrixCount = Math.max(totalMatrixCount, (int) (externalMatrixBank.matrixBlockCount() * ScCompressedMatrixBank.BLOCK_SIZE));
            int colorTransformCount = (int) externalMatrixBank.colorTransformCount();
            ScMatrixBank matrixBank = new ScMatrixBank(totalMatrixCount, colorTransformCount);

            // Parsing
            byte[] decompressed = Zstandard.decompress(data, (int) (matrixBankDataPosition + externalMatrixBank.offset()));
            ByteBuffer byteBuffer = ByteBuffer.wrap(decompressed);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

            for (int j = 0; j < externalMatrixBank.floatMatrixCount(); j++) {
                float a = byteBuffer.getFloat();
                float b = byteBuffer.getFloat();
                float c = byteBuffer.getFloat();
                float d = byteBuffer.getFloat();
                float x = byteBuffer.getFloat();
                float y = byteBuffer.getFloat();

                matrixBank.getMatrix(j).set(a, b, c, d, x, y);
            }

            ScCompressedMatrixBank compressedMatrixBank = new ScCompressedMatrixBank(byteBuffer, (int) externalMatrixBank.floatMatrixCount(), (int) externalMatrixBank.shortMatrixCount(), (int) externalMatrixBank.matrixBlockCount());
            for (int j = uncompressedMatrixCount; j < externalMatrixBank.matrixBlockCount() * ScCompressedMatrixBank.BLOCK_SIZE; j++) {
                Matrix2x3 matrix = compressedMatrixBank.getMatrix(j);
                matrixBank.getMatrix(j).set(matrix.getA(), matrix.getB(), matrix.getC(), matrix.getD(), matrix.getX(), matrix.getY());
            }

            // TODO: move to compressed matrix bank get matrix
            byteBuffer.position((int) (externalMatrixBank.floatMatrixCount() * Float.BYTES * 6 + externalMatrixBank.matrixBlockCount() * Integer.BYTES));

            for (int j = (int) externalMatrixBank.floatMatrixCount(); j < uncompressedMatrixCount; j++) {
                matrixBank.getMatrix(j).set(
                    byteBuffer.getShort(),
                    byteBuffer.getShort(),
                    byteBuffer.getShort(),
                    byteBuffer.getShort(),
                    byteBuffer.getShort(),
                    byteBuffer.getShort()
                );
            }

            for (int j = 0; j < externalMatrixBank.colorTransformCount(); j++) {
                int r = byteBuffer.get() & 0xFF;
                int g = byteBuffer.get() & 0xFF;
                int b = byteBuffer.get() & 0xFF;
                int a = byteBuffer.get() & 0xFF;
                int ra = byteBuffer.get() & 0xFF;
                int ga = byteBuffer.get() & 0xFF;
                int ba = byteBuffer.get() & 0xFF;
                matrixBank.getColorTransform(j).set(r, g, b, a, ra, ga, ba);
            }

            matrixBanks.add(matrixBank);
        }

        return matrixBanks;
    }

    private static ByteBuffer getNestedFlatbufferBytes(ByteBuffer byteBuffer) {
        int length = byteBuffer.getInt();
        byte[] bytes = new byte[length];
        byteBuffer.get(bytes);
        return ByteBuffer.wrap(bytes);
    }
}