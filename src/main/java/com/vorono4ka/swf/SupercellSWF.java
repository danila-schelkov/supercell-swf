package com.vorono4ka.swf;

import com.vorono4ka.FlatSupercellSWFLoader;
import com.vorono4ka.ProgressTracker;
import com.vorono4ka.math.Rendering;
import com.vorono4ka.streams.ByteStream;
import com.vorono4ka.swf.exceptions.*;
import com.vorono4ka.swf.file.ScFileInfo;
import com.vorono4ka.swf.file.ScFilePacker;
import com.vorono4ka.swf.file.ScFileUnpacker;
import com.vorono4ka.swf.file.exceptions.FileVerificationException;
import com.vorono4ka.swf.file.exceptions.UnknownFileVersionException;
import com.vorono4ka.swf.movieclips.MovieClipModifierOriginal;
import com.vorono4ka.swf.movieclips.MovieClipOriginal;
import com.vorono4ka.swf.shapes.ShapeDrawBitmapCommand;
import com.vorono4ka.swf.shapes.ShapeOriginal;
import com.vorono4ka.swf.textfields.TextFieldOriginal;
import com.vorono4ka.swf.textures.SWFTexture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SupercellSWF {
    public static final String TEXTURE_EXTENSION = "_tex.sc";

    private static final Logger LOGGER = LoggerFactory.getLogger(SupercellSWF.class);

    private final List<String> fontsNames = new ArrayList<>();
    private final List<ScMatrixBank> matrixBanks = new ArrayList<>();

    private List<Export> exports;

    private List<SWFTexture> textures;
    private List<ShapeOriginal> shapes;
    private List<MovieClipOriginal> movieClips;
    private List<TextFieldOriginal> textFields;

    private List<MovieClipModifierOriginal> movieClipModifiers;

    private boolean isHalfScalePossible;
    private boolean useUncommonResolution;
    private boolean useExternalTexture;
    private String uncommonResolutionTexturePath;

    private String filename;
    private Path path;

    public static SupercellSWF createEmpty() {
        SupercellSWF swf = new SupercellSWF();

        swf.exports = new ArrayList<>();
        swf.textures = new ArrayList<>();
        swf.shapes = new ArrayList<>();
        swf.movieClips = new ArrayList<>();
        swf.textFields = new ArrayList<>();
        swf.movieClipModifiers = new ArrayList<>();

        swf.addMatrixBank(new ScMatrixBank());

        return swf;
    }

    public boolean load(String filepath, String filename) throws LoadingFaultException, UnableToFindObjectException, UnsupportedCustomPropertyException, TextureFileNotFound {
        this.filename = filename;
        this.path = Path.of(filepath);

        if (this.loadInternal(filepath, false)) {
            if (!this.useExternalTexture) return true;

            if (this.useUncommonResolution) {
                filepath = this.uncommonResolutionTexturePath;
            } else {
                filepath = filepath.substring(0, filepath.length() - 3) + TEXTURE_EXTENSION;
            }

            return this.loadInternal(filepath, true);
        }

        return false;
    }

    public void save(String path, ProgressTracker tracker) {
        this.saveInternal(path, false, tracker);
        // Add an option "Save textures as external files" when saving the whole project
    }

    public MovieClipOriginal getOriginalMovieClip(int id, String name) throws UnableToFindObjectException {
        for (MovieClipOriginal movieClip : this.movieClips) {
            if (movieClip.getId() == id) {
                return movieClip;
            }
        }

        String message = String.format("Unable to find some MovieClip id from %s", this.filename);
        if (name != null) {
            message += String.format(" needed by export name %s", name);
        }

        throw new UnableToFindObjectException(message);
    }

    public DisplayObjectOriginal getOriginalDisplayObject(int id, String name) throws UnableToFindObjectException {
        for (ShapeOriginal shape : this.shapes) {
            if (shape.getId() == id) {
                return shape;
            }
        }

        for (MovieClipOriginal movieClip : this.movieClips) {
            if (movieClip.getId() == id) {
                return movieClip;
            }
        }

        for (TextFieldOriginal textField : textFields) {
            if (textField.getId() == id) {
                return textField;
            }
        }

        for (MovieClipModifierOriginal movieClipModifier : movieClipModifiers) {
            if (movieClipModifier.getId() == id) {
                return movieClipModifier;
            }
        }

        String message = String.format("Unable to find some DisplayObject id %d, %s", id, this.filename);
        if (name != null) {
            message += String.format(" needed by export name %s", name);
        }

        throw new UnableToFindObjectException(message);
    }

    public int getMovieClipCount() {
        return this.movieClips != null ? this.movieClips.size() : 0;
    }

    public int getTextureCount() {
        return this.textures != null ? this.textures.size() : 0;
    }

    public int[] getShapesIds() {
        return shapes.stream().mapToInt(DisplayObjectOriginal::getId).toArray();
    }

    public int[] getMovieClipsIds() {
        return movieClips.stream().mapToInt(DisplayObjectOriginal::getId).toArray();
    }

    public int[] getTextFieldsIds() {
        return textFields.stream().mapToInt(DisplayObjectOriginal::getId).toArray();
    }

    public List<ScMatrixBank> getMatrixBanks() {
        return Collections.unmodifiableList(this.matrixBanks);
    }

    public int getMatrixBankCount() {
        return this.matrixBanks.size();
    }

    public ScMatrixBank getMatrixBank(int index) {
        return this.matrixBanks.get(index);
    }

    public void addMatrixBank(ScMatrixBank matrixBank) {
        this.matrixBanks.add(matrixBank);
    }

    public List<SWFTexture> getTextures() {
        return Collections.unmodifiableList(this.textures);
    }

    public SWFTexture getTexture(int textureIndex) {
        return this.textures.get(textureIndex);
    }

    /**
     * @return path, containing a filename
     */
    public Path getPath() {
        return path;
    }

    public String getFilename() {
        return filename;
    }

    public boolean isHalfScalePossible() {
        return isHalfScalePossible;
    }

    public List<ShapeDrawBitmapCommand> getDrawBitmapsOfTexture(int textureIndex) {
        List<ShapeDrawBitmapCommand> bitmapCommands = new ArrayList<>();

        for (ShapeOriginal shape : this.shapes) {
            for (ShapeDrawBitmapCommand command : shape.getCommands()) {
                if (command.getTextureIndex() == textureIndex) {
                    if (!bitmapCommands.contains(command)) {
                        bitmapCommands.add(command);
                    }
                }
            }
        }

        return bitmapCommands;
    }

    private boolean loadInternal(String path, boolean isTextureFile) throws LoadingFaultException, UnableToFindObjectException, UnsupportedCustomPropertyException, TextureFileNotFound {
        try {
            byte[] data;
            try (FileInputStream fis = new FileInputStream(path)) {
                data = fis.readAllBytes();
            } catch (IOException e) {
                throw new TextureFileNotFound(path);
            }
            ScFileInfo unpacked = ScFileUnpacker.unpack(data);
            byte[] decompressedData = unpacked.data();

            if (unpacked.version() == 5) {
                boolean result = loadSc2(decompressedData);

                for (ShapeOriginal shape : shapes) {
                    for (ShapeDrawBitmapCommand command : shape.getCommands()) {
                        command.setTriangulator(Rendering.TRIANGULATOR_FUNCTION_2);
                    }
                }

                return result;
            }

            return loadSc1(path, isTextureFile, decompressedData);
        } catch (UnknownFileVersionException | FileVerificationException |
                 IOException exception) {
            LOGGER.error("An error occurred while decompressing the file: {}", path, exception);
            return false;
        }
    }

    private boolean loadSc2(byte[] decompressedData) {
        FlatSupercellSWFLoader loader = new FlatSupercellSWFLoader(decompressedData, true);

        this.exports = loader.exports;
        this.matrixBanks.addAll(loader.matrixBanks);
        this.textFields = loader.textFields;
        this.movieClipModifiers = loader.modifiers;
        this.movieClips = loader.movieClips;
        this.shapes = loader.shapes;
        this.textures = loader.textures;

        for (int i = 0; i < this.textures.size(); i++) {
            this.textures.get(i).setIndex(i);
        }

        return true;
    }

    private boolean loadSc1(String path, boolean isTextureFile, byte[] decompressedData) throws LoadingFaultException, UnsupportedCustomPropertyException, UnableToFindObjectException {
        ByteStream stream = new ByteStream(decompressedData);

        if (isTextureFile) {
            return this.loadTags(stream, true, path);
        }

        int shapeCount = stream.readShort();
        int movieClipCount = stream.readShort();
        int textureCount = stream.readShort();
        int textFieldCount = stream.readShort();
        int matrixCount = stream.readShort();
        int colorTransformCount = stream.readShort();

        ScMatrixBank matrixBank = new ScMatrixBank(matrixCount, colorTransformCount);
        this.addMatrixBank(matrixBank);

        stream.skip(5);

        int exportCount = stream.readShort();

        short[] exportIds = stream.readShortArray(exportCount);

        String[] exportNames = new String[exportCount];
        for (int i = 0; i < exportCount; i++) {
            exportNames[i] = stream.readAscii();
        }

        this.exports = new ArrayList<>(exportCount);
        for (int i = 0; i < exportCount; i++) {
            exports.add(new Export(exportIds[i] & 0xFFFF, exportNames[i]));
        }

        this.shapes = new ArrayList<>(shapeCount);
        for (int i = 0; i < shapeCount; i++) {
            this.shapes.add(new ShapeOriginal());
        }

        this.movieClips = new ArrayList<>(movieClipCount);
        for (int i = 0; i < movieClipCount; i++) {
            this.movieClips.add(new MovieClipOriginal());
        }

        this.textures = new ArrayList<>(textureCount);
        for (int i = 0; i < textureCount; i++) {
            this.textures.add(new SWFTexture());
        }

        this.textFields = new ArrayList<>(textFieldCount);
        for (int i = 0; i < textFieldCount; i++) {
            this.textFields.add(new TextFieldOriginal());
        }

        if (this.loadTags(stream, false, path)) {
            for (Export export : exports) {
                MovieClipOriginal movieClip = this.getOriginalMovieClip(export.id(), export.name());
                movieClip.setExportName(export.name());
            }

            for (ShapeOriginal shape : shapes) {
                for (ShapeDrawBitmapCommand command : shape.getCommands()) {
                    command.setTriangulator(Rendering.TRIANGULATOR_FUNCTION_1);
                }
            }

            return true;
        }

        return false;
    }

    private boolean loadTags(ByteStream stream, boolean isTextureFile, String path) throws LoadingFaultException, UnsupportedCustomPropertyException {
        String highresSuffix = "_highres";
        String lowresSuffix = "_lowres";

        ScMatrixBank matrixBank = this.matrixBanks.get(0);

        int loadedShapes = 0;
        int loadedMovieClips = 0;
        int loadedTextures = 0;
        int loadedTextFields = 0;
        int loadedMatrices = 0;
        int loadedColorTransforms = 0;

        int loadedMovieClipsModifiers = 0;

        while (true) {
            int tag = stream.readUnsignedChar();
            int length = stream.readInt();

            if (length < 0) {
                throw new NegativeTagLengthException(String.format("Negative tag length. Tag %d, %s", tag, this.filename));
            }

            if (tag > Tag.values().length) {
                try {
                    throw new UnsupportedTagException(String.format("Encountered unknown tag %d, %s", tag, this.filename));
                } catch (UnsupportedTagException exception) {
                    LOGGER.error("An error occurred while loading the file: {}", path, exception);
                }

                if (length > 0) {
                    stream.skip(length);
                    continue;
                }
            }

            Tag tagValue = Tag.values()[tag];
            switch (tagValue) {
                case EOF -> {
                    if (isTextureFile) {
                        if (loadedTextures != this.textures.size()) {
                            throw new LoadingFaultException(String.format("Texture count in .sc and _tex.sc doesn't match: %s", this.filename));
                        }
                    } else {
                        if (loadedMatrices != matrixBank.getMatrixCount() ||
                            loadedColorTransforms != matrixBank.getColorTransformCount() ||
                            loadedMovieClips != this.movieClips.size() ||
                            loadedShapes != this.shapes.size() ||
                            loadedTextFields != this.textFields.size()) {
                            throw new LoadingFaultException("Didn't load whole .sc properly. " + filename);
                        }
                    }

                    return true;
                }
                case TEXTURE, TEXTURE_2, TEXTURE_3, TEXTURE_4, TEXTURE_5, TEXTURE_6,
                     TEXTURE_7, TEXTURE_8, KHRONOS_TEXTURE,
                     TEXTURE_FILE_REFERENCE -> {
                    if (loadedTextures >= this.textures.size()) {
                        throw new TooManyObjectsException("Trying to load too many textures from " + filename);
                    }
                    this.textures.get(loadedTextures).setIndex(loadedTextures);
                    this.textures.get(loadedTextures++).load(stream, tagValue, !this.useExternalTexture || isTextureFile);
                }
                case SHAPE, SHAPE_2 -> {
                    if (loadedShapes >= this.shapes.size()) {
                        throw new TooManyObjectsException("Trying to load too many shapes from " + filename);
                    }

                    this.shapes.get(loadedShapes++).load(stream, tagValue, this::getTexture, filename);
                }
                case MOVIE_CLIP, MOVIE_CLIP_2, MOVIE_CLIP_3, MOVIE_CLIP_4, MOVIE_CLIP_5,
                     MOVIE_CLIP_6 -> {
                    if (loadedMovieClips >= this.movieClips.size()) {
                        throw new TooManyObjectsException("Trying to load too many MovieClips from " + filename);
                    }

                    this.movieClips.get(loadedMovieClips++).load(stream, tagValue, filename);
                }
                case TEXT_FIELD, TEXT_FIELD_2, TEXT_FIELD_3, TEXT_FIELD_4, TEXT_FIELD_5,
                     TEXT_FIELD_6, TEXT_FIELD_7, TEXT_FIELD_8, TEXT_FIELD_9 -> {
                    if (loadedTextFields >= this.textFields.size()) {
                        throw new TooManyObjectsException("Trying to load too many TextFields from " + filename);
                    }

                    this.textFields.get(loadedTextFields++).load(stream, tagValue, this::readFontName);
                }
                case MATRIX ->
                    matrixBank.getMatrix(loadedMatrices++).load(stream, false);
                case COLOR_TRANSFORM ->
                    matrixBank.getColorTransform(loadedColorTransforms++).read(stream);
                case TAG_TIMELINE_INDEXES -> {
                    try {
                        throw new UnsupportedTagException("TAG_TIMELINE_INDEXES no longer in use");
                    } catch (UnsupportedTagException exception) {
                        LOGGER.error("An error occurred while loading the file: {}", path, exception);
                    }

                    int indicesLength = stream.readInt();
                    stream.skip(indicesLength);
                }
                case HALF_SCALE_POSSIBLE -> this.isHalfScalePossible = true;
                case USE_EXTERNAL_TEXTURE -> this.useExternalTexture = true;
                case USE_UNCOMMON_RESOLUTION -> {
                    this.useUncommonResolution = true;

                    String withoutExtension = path.substring(0, path.length() - 3);
                    String highresPath = withoutExtension + highresSuffix + TEXTURE_EXTENSION;
                    String lowresPath = withoutExtension + lowresSuffix + TEXTURE_EXTENSION;

                    this.isHalfScalePossible = true;
                    String uncommonPath = highresPath;
                    if (!doesFileExist(highresPath)) {
                        if (doesFileExist(lowresPath)) {
                            uncommonPath = lowresPath;
                        }
                    }

                    this.uncommonResolutionTexturePath = uncommonPath;
                }
                case EXTERNAL_FILES_SUFFIXES -> {
                    highresSuffix = stream.readAscii();
                    lowresSuffix = stream.readAscii();
                }
                case MATRIX_PRECISE ->
                    matrixBank.getMatrix(loadedMatrices++).load(stream, true);
                case MOVIE_CLIP_MODIFIERS -> {
                    int movieClipModifierCount = stream.readShort();

                    this.movieClipModifiers = new ArrayList<>(movieClipModifierCount);
                    for (int i = 0; i < movieClipModifierCount; i++) {
                        this.movieClipModifiers.add(new MovieClipModifierOriginal());
                    }
                }
                case MODIFIER_STATE_2, MODIFIER_STATE_3, MODIFIER_STATE_4 ->
                    this.movieClipModifiers.get(loadedMovieClipsModifiers++).load(stream, tagValue);
                case EXTRA_MATRIX_BANK -> {
                    int matrixCount = stream.readShort();
                    int colorTransformCount = stream.readShort();

                    matrixBank = new ScMatrixBank(matrixCount, colorTransformCount);
                    this.matrixBanks.add(matrixBank);

                    loadedMatrices = 0;
                    loadedColorTransforms = 0;
                }
                default -> {
                    // TODO: add strict mode which crashes on errors and probably enable it by default
                    // TODO: also add properties and settings for the app
                    try {
                        throw new UnsupportedTagException(String.format("Encountered unknown tag %d, %s", tag, this.filename));
                    } catch (UnsupportedTagException exception) {
                        LOGGER.error("An error occurred while loading the file: {}", path, exception);
                    }

                    if (length > 0) {
                        stream.skip(length);
                    }
                }
            }
        }
    }

    private String readFontName(ByteStream stream) {
        String fontName = stream.readAscii();
        if (fontName != null) {
            if (!this.fontsNames.contains(fontName)) {
                this.fontsNames.add(fontName);
            }
        }

        return fontName;
    }

    private void saveInternal(String path, boolean isTextureFile, ProgressTracker tracker) {
        ByteStream stream = new ByteStream();

        if (!isTextureFile) {
            saveObjectsInfo(stream);
        }

        this.saveTags(stream, tracker);

        byte[] data = stream.getData();

        try {
            data = ScFilePacker.pack(data, new byte[0], 3);
        } catch (IOException | UnknownFileVersionException e) {
            throw new RuntimeException(e);
        }

        File file = new File(path);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        } catch (IOException exception) {
            LOGGER.error("An error occurred while saving the file: {}", path, exception);
        }
    }

    private void saveObjectsInfo(ByteStream stream) {
        stream.writeShort(this.shapes.size());
        stream.writeShort(this.movieClips.size());
        stream.writeShort(this.textures.size());
        stream.writeShort(this.textFields.size());
        stream.writeShort(this.matrixBanks.get(0).getMatrixCount());
        stream.writeShort(this.matrixBanks.get(0).getColorTransformCount());

        stream.write(new byte[5]);  // unused

        stream.writeShort(this.exports.size());
        for (Export export : exports) {
            stream.writeShort(export.id());
        }

        for (Export export : exports) {
            stream.writeAscii(export.name());
        }
    }

    private void saveTags(ByteStream stream, ProgressTracker tracker) {
        List<Savable> savables = this.getSavableObjects();

        int i = 0;
        for (Savable object : savables) {
            stream.writeSavable(object);
            if (tracker != null) {
                tracker.setProgress(++i, savables.size());
            }
        }

        stream.writeBlock(Tag.EOF, null);
    }

    private List<Savable> getSavableObjects() {
        List<Savable> objects = new ArrayList<>();

        if (this.isHalfScalePossible) {
            objects.add(new Savable() {
                @Override
                public void save(ByteStream stream) {

                }

                @Override
                public Tag getTag() {
                    return Tag.HALF_SCALE_POSSIBLE;
                }
            });
        }

        if (this.useExternalTexture) {
            objects.add(new Savable() {
                @Override
                public void save(ByteStream stream) {

                }

                @Override
                public Tag getTag() {
                    return Tag.USE_EXTERNAL_TEXTURE;
                }
            });
        }

        objects.addAll(this.textures);
        objects.addAll(this.shapes);
        for (int i = 0; i < this.matrixBanks.size(); i++) {
            ScMatrixBank matrixBank = this.matrixBanks.get(i);

            if (i != 0) {
                objects.add(new Savable() {
                    @Override
                    public void save(ByteStream stream) {
                        stream.writeShort(matrixBank.getMatrixCount());
                        stream.writeShort(matrixBank.getColorTransformCount());
                    }

                    @Override
                    public Tag getTag() {
                        return Tag.EXTRA_MATRIX_BANK;
                    }
                });
            }

            objects.addAll(matrixBank.getMatrices());
            objects.addAll(matrixBank.getColorTransforms());
        }
        objects.addAll(this.textFields);
        objects.addAll(this.movieClips);

        if (this.movieClipModifiers != null && !this.movieClipModifiers.isEmpty()) {
            objects.add(new Savable() {
                @Override
                public void save(ByteStream stream) {
                    stream.writeShort(movieClipModifiers.size());
                }

                @Override
                public Tag getTag() {
                    return Tag.MOVIE_CLIP_MODIFIERS;
                }
            });

            objects.addAll(this.movieClipModifiers);
        }

        return objects;
    }

    private static boolean doesFileExist(String path) {
        return Files.exists(Path.of(path));
    }

    public List<Export> getExports() {
        return Collections.unmodifiableList(exports);
    }

    public void addTexture(SWFTexture texture) {
        this.textures.add(texture);
    }

    public void addObject(DisplayObjectOriginal objectOriginal) {
        if (objectOriginal instanceof MovieClipOriginal movieClipOriginal) {
            this.movieClips.add(movieClipOriginal);
        } else if (objectOriginal instanceof ShapeOriginal shapeOriginal) {
            this.shapes.add(shapeOriginal);
        } else if (objectOriginal instanceof TextFieldOriginal textFieldOriginal) {
            this.textFields.add(textFieldOriginal);
        } else if (objectOriginal instanceof MovieClipModifierOriginal movieClipModifierOriginal) {
            this.movieClipModifiers.add(movieClipModifierOriginal);
        } else {
            throw new RuntimeException("Object not recognized: " + objectOriginal);
        }
    }

    public void addExport(int movieClipId, String name) {
        this.exports.add(new Export(movieClipId, name));
    }
}
