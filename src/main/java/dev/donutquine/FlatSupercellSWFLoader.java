package dev.donutquine;

import com.supercell.swf.*;
import dev.donutquine.swf.Export;
import dev.donutquine.swf.ScMatrixBank;
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

public class FlatSupercellSWFLoader {
    public final FBResources fbResources;
    public final List<ScMatrixBank> matrixBanks;
    public final List<Export> exports;
    public final List<TextFieldOriginal> textFields;
    public final List<ShapeOriginal> shapes;
    public final List<MovieClipOriginal> movieClips;
    public final List<MovieClipModifierOriginal> modifiers;
    public final List<SWFTexture> textures;

    public FlatSupercellSWFLoader(InputStream inputStream, boolean preferLowres) throws IOException {
        this(inputStream.readAllBytes(), preferLowres);
    }

    public FlatSupercellSWFLoader(byte[] data, boolean preferLowres) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);

        this.fbResources = FBResources.getRootAsFBResources(getChunkBytes(byteBuffer));

        this.matrixBanks = deserializeMatrixBanks();

        this.exports = deserializeExports(getChunkBytes(byteBuffer));
        this.textFields = deserializeTextFields(getChunkBytes(byteBuffer));
        this.shapes = deserializeShapes(getChunkBytes(byteBuffer));
        this.movieClips = deserializeMovieClips(getChunkBytes(byteBuffer));
        this.modifiers = deserializeModifiers(getChunkBytes(byteBuffer));
        this.textures = deserializeTextures(getChunkBytes(byteBuffer), preferLowres);
    }

    private List<SWFTexture> deserializeTextures(ByteBuffer chunkBuffer, boolean preferLowres) {
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

            textures.add(new SWFTexture(fbTexture, fbResources));
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
            movieClips.add(new MovieClipOriginal(fbMovieClips.clips(i), fbResources));
        }

        return movieClips;
    }

    private List<ShapeOriginal> deserializeShapes(ByteBuffer chunkBuffer) {
        FBShapes fbShapes = FBShapes.getRootAsFBShapes(chunkBuffer);
        List<ShapeOriginal> shapes = new ArrayList<>(fbShapes.shapesLength());
        for (int i = 0; i < fbShapes.shapesLength(); i++) {
            shapes.add(new ShapeOriginal(fbShapes.shapes(i), fbResources));
        }
        return shapes;
    }

    private List<TextFieldOriginal> deserializeTextFields(ByteBuffer chunkBuffer) {
        FBTextFields fbTextFields = FBTextFields.getRootAsFBTextFields(chunkBuffer);
        List<TextFieldOriginal> textFields = new ArrayList<>(fbTextFields.textFieldsLength());
        for (int i = 0; i < fbTextFields.textFieldsLength(); i++) {
            textFields.add(new TextFieldOriginal(fbTextFields.textFields(i), fbResources));
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

            exports.add(new Export(id, fbResources.strings(exportNameId)));
        }

        return exports;
    }

    private List<ScMatrixBank> deserializeMatrixBanks() {
        List<ScMatrixBank> matrixBanks = new ArrayList<>(fbResources.matrixBanksLength());
        for (int j = 0; j < fbResources.matrixBanksLength(); j++) {
            FBMatrixBank fbMatrixBank = fbResources.matrixBanks(j);

            int matrixCount = fbMatrixBank.matricesLength();
            if (fbMatrixBank.matricesLength() == 0) {
                matrixCount = fbMatrixBank.shortMatricesLength();
            }

            ScMatrixBank matrixBank = new ScMatrixBank(matrixCount, fbMatrixBank.colorTransformsLength());

            if (fbMatrixBank.matricesLength() > 0) {
                for (int i = 0; i < matrixCount; i++) {
                    FBMatrix2x3 fbMatrix2x3 = fbMatrixBank.matrices(i);
                    matrixBank.getMatrix(i).initFromFlatBuffer(fbMatrix2x3);
                }
            } else {
                for (int i = 0; i < fbMatrixBank.shortMatricesLength(); i++) {
                    FBShortMatrix2x3 fbMatrix2x3 = fbMatrixBank.shortMatrices(i);
                    matrixBank.getMatrix(i).initFromFlatBuffer(fbMatrix2x3);
                }
            }

            for (int i = 0; i < fbMatrixBank.colorTransformsLength(); i++) {
                FBColorTransform fbColorTransform = fbMatrixBank.colorTransforms(i);
                matrixBank.getColorTransform(i).initFromFlatBuffer(fbColorTransform);
            }

            matrixBanks.add(matrixBank);
        }

        return matrixBanks;
    }

    private static ByteBuffer getChunkBytes(ByteBuffer byteBuffer) {
        int length = byteBuffer.getInt();
        byte[] bytes = new byte[length];
        byteBuffer.get(bytes);
        return ByteBuffer.wrap(bytes);
    }
}