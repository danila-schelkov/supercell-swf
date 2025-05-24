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

    private static final String DEFAULT_HIGHRES_SUFFIX = "_highres";
    private static final String DEFAULT_LOWRES_SUFFIX = "_lowres";

    private final List<String> fontsNames = new ArrayList<>();
    private final List<ScMatrixBank> matrixBanks = new ArrayList<>();

    private List<Export> exports;

    private List<SWFTexture> textures;
    private List<ShapeOriginal> shapes;
    private List<MovieClipOriginal> movieClips;
    private List<TextFieldOriginal> textFields;

    private List<MovieClipModifierOriginal> movieClipModifiers;

    // TODO: half-scale
    private boolean isHalfScalePossible;
    private boolean useExternalTexture;
    private boolean useUncommonResolution;
    private String highresSuffix = DEFAULT_HIGHRES_SUFFIX;
    private String lowresSuffix = DEFAULT_LOWRES_SUFFIX;
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

    public boolean load(String filepath, String filename, boolean preferLowres) throws LoadingFaultException, UnableToFindObjectException, UnsupportedCustomPropertyException, TextureFileNotFound {
        this.filename = filename;
        this.path = Path.of(filepath);

        if (this.loadInternal(filepath, false, preferLowres)) {
            if (!this.useExternalTexture) return true;

            return this.loadInternal(getTextureFilepath(filepath), true, preferLowres);
        }

        return false;
    }

    public void save(String filepath, ProgressTracker tracker) {
        this.saveInternal(filepath, !this.useExternalTexture, tracker);

        // TODO: Add an option "Save textures as external files" when saving the whole project
        if (this.useExternalTexture) {
            this.saveInternal(getTextureFilepath(filepath), true, tracker);
        }
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

    public List<SWFTexture> getTextures() {
        return Collections.unmodifiableList(this.textures);
    }

    public SWFTexture getTexture(int textureIndex) {
        return this.textures.get(textureIndex);
    }

    public void addTexture(SWFTexture texture) {
        this.textures.add(texture);
    }

    public List<ShapeOriginal> getShapes() {
        return Collections.unmodifiableList(shapes);
    }

    public int[] getShapeIds() {
        return shapes.stream().mapToInt(DisplayObjectOriginal::getId).toArray();
    }

    public List<MovieClipOriginal> getMovieClips() {
        return Collections.unmodifiableList(movieClips);
    }

    public int[] getMovieClipIds() {
        return movieClips.stream().mapToInt(DisplayObjectOriginal::getId).toArray();
    }

    public List<TextFieldOriginal> getTextFields() {
        return Collections.unmodifiableList(textFields);
    }

    public int[] getTextFieldIds() {
        return textFields.stream().mapToInt(DisplayObjectOriginal::getId).toArray();
    }

    public List<MovieClipModifierOriginal> getMovieClipModifiers() {
        return Collections.unmodifiableList(movieClipModifiers);
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

    /**
     * @return path, containing a filename
     * @since 1.0.0
     */
    public Path getPath() {
        return path;
    }

    /**
     * @return filename of loaded info file.
     * @since 1.0.0
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Returns whether external textures saving is enabled.
     *
     * @since 1.0.7
     */
    public boolean isUseExternalTexture() {
        return useExternalTexture;
    }

    /**
     * Enables or disables saving the textures using an external files.
     *
     * <p>This only affects how the file is saved and does not alter file reading behavior.</p>
     *
     * <p><strong>Warning:</strong> Set this flag only if you understand the file format requirements.
     * Incorrect use may result in files that cannot be saved correctly.</p>
     *
     * @since 1.0.7
     */
    public void setUseExternalTexture(boolean useExternalTexture) {
        this.useExternalTexture = useExternalTexture;
    }


    /**
     * Returns whether half-resolution textures is allowed.
     *
     * <p>This only affects how the file is saved and does not alter file reading behavior.</p>
     *
     * @since 1.0.7
     */
    public boolean isHalfScalePossible() {
        return isHalfScalePossible;
    }


    /**
     * Enables or disables saving the texture at half resolution.
     *
     * <p>This only affects how the file is saved and does not alter file reading behavior.</p>
     *
     * <p><strong>Warning:</strong> Use this only if the format supports half-resolution storage.
     * Otherwise, the saved file may be invalid.</p>
     *
     * @since 1.0.7
     */
    public void setHalfScalePossible(boolean halfScalePossible) {
        isHalfScalePossible = halfScalePossible;
    }

    /**
     * Returns whether textures are split into high-resolution and low-resolution texture files.
     *
     * @since 1.0.7
     */
    public boolean isUseUncommonResolution() {
        return useUncommonResolution;
    }

    /**
     * Sets whether to split textures into high-resolution and low-resolution texture files.
     *
     * <p>Currently, the library supports saving only textures resolutions from read files. </p>
     *
     * <p><strong>Warning:</strong> This method is intended to affect file saving behavior,
     * but currently has no effect, as its saving logic is not yet implemented. </p>
     *
     * <p><strong>Warning:</strong> It does not alter file reading behavior. </p>
     *
     * @since 1.0.7
     */
    public void setUseUncommonResolution(boolean useUncommonResolution) {
        this.useUncommonResolution = useUncommonResolution;
    }

    /**
     * <strong>Warning:</strong> This method is intended to affect file saving behavior, but currently has no effect,
     * as its saving logic is not yet implemented. It does not alter file reading behavior.
     *
     * @since 1.0.7
     */
    public void setExternalFileSuffixes(String highresSuffix, String lowresSuffix) {
        if (!this.useExternalTexture) {
            throw new IllegalStateException("Cannot use external texture suffixes when using internal texture");
        }

        this.highresSuffix = highresSuffix;
        this.lowresSuffix = lowresSuffix;
    }

    public String getTextureFilepath(String filepath) {
        if (this.useUncommonResolution) {
            return this.uncommonResolutionTexturePath;
        }

        return filepath.substring(0, filepath.length() - 3) + TEXTURE_EXTENSION;
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

    private boolean loadInternal(String path, boolean isTextureFile, boolean preferLowres) throws LoadingFaultException, UnableToFindObjectException, UnsupportedCustomPropertyException, TextureFileNotFound {
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
                boolean result = loadSc2(decompressedData, preferLowres);

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

    private boolean loadSc2(byte[] decompressedData, boolean preferLowres) {
        FlatSupercellSWFLoader loader = new FlatSupercellSWFLoader(decompressedData, preferLowres);

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
        highresSuffix = DEFAULT_HIGHRES_SUFFIX;
        lowresSuffix = DEFAULT_LOWRES_SUFFIX;

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
                     TEXTURE_7, TEXTURE_8, KHRONOS_TEXTURE, TEXTURE_FILE_REFERENCE -> {
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

        this.saveTags(stream, isTextureFile, tracker);

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

    private void saveTags(ByteStream stream, boolean isTextureFile, ProgressTracker tracker) {
        List<Savable> savables = this.getSavableObjects(isTextureFile);

        int i = 0;
        for (Savable object : savables) {
            stream.writeSavable(object);
            if (tracker != null) {
                tracker.setProgress(++i, savables.size());
            }
        }

        stream.writeBlock(Tag.EOF, null);
    }

    private List<Savable> getSavableObjects(boolean isTextureFile) {
        List<Savable> objects = new ArrayList<>();

        if (!isTextureFile) {
            if (this.isHalfScalePossible) {
                objects.add(new FlagSavable(Tag.HALF_SCALE_POSSIBLE));
            }

            if (this.useExternalTexture) {
                objects.add(new FlagSavable(Tag.USE_EXTERNAL_TEXTURE));

                if (!this.highresSuffix.equals(DEFAULT_HIGHRES_SUFFIX) || !this.lowresSuffix.equals(DEFAULT_LOWRES_SUFFIX)) {
                    objects.add(new ExternalFilesSuffixesSavable(this.highresSuffix, this.lowresSuffix));
                }
            }
        }

        this.textures.forEach(texture -> texture.setHasTexture(isTextureFile));

        objects.addAll(this.textures);

        if (isTextureFile) {
            return objects;
        }

        objects.addAll(this.shapes);

        for (int i = 0; i < this.matrixBanks.size(); i++) {
            ScMatrixBank matrixBank = this.matrixBanks.get(i);

            if (i != 0) {
                objects.add(new ExtraMatrixBankInfo(matrixBank));
            }

            objects.addAll(matrixBank.getMatrices());
            objects.addAll(matrixBank.getColorTransforms());
        }

        objects.addAll(this.textFields);
        objects.addAll(this.movieClips);

        if (this.movieClipModifiers != null && !this.movieClipModifiers.isEmpty()) {
            objects.add(new MovieClipModifiersInfo(this.movieClipModifiers));
            objects.addAll(this.movieClipModifiers);
        }

        return objects;
    }

    private static boolean doesFileExist(String path) {
        return Files.exists(Path.of(path));
    }

    public List<Export> getExports() {
        return exports;
    }

    public void addExport(int movieClipId, String name) {
        this.exports.add(new Export(movieClipId, name));
    }

    /**
     * Adds object to corresponding object list, returning its identifier.
     *
     * @param object display object to be added
     * @return new object id
     */
    public int addObject(DisplayObjectOriginal object) {
        int nextId = this.movieClips.size() + this.shapes.size() + this.textFields.size() + this.movieClipModifiers.size() + 1;

        if (object instanceof MovieClipOriginal movieClipOriginal) {
            this.movieClips.add(movieClipOriginal);
        } else if (object instanceof ShapeOriginal shapeOriginal) {
            this.shapes.add(shapeOriginal);
        } else if (object instanceof TextFieldOriginal textFieldOriginal) {
            this.textFields.add(textFieldOriginal);
        } else if (object instanceof MovieClipModifierOriginal movieClipModifierOriginal) {
            this.movieClipModifiers.add(movieClipModifierOriginal);
        } else {
            throw new RuntimeException("Object not recognized: " + object);
        }

        return nextId;
    }

    private record ExtraMatrixBankInfo(ScMatrixBank matrixBank) implements Savable {
        @Override
        public void save(ByteStream stream) {
            stream.writeShort(matrixBank.getMatrixCount());
            stream.writeShort(matrixBank.getColorTransformCount());
        }

        @Override
        public Tag getTag() {
            return Tag.EXTRA_MATRIX_BANK;
        }
    }

    private record MovieClipModifiersInfo(
        List<MovieClipModifierOriginal> movieClipModifiers
    ) implements Savable {
        @Override
        public void save(ByteStream stream) {
            stream.writeShort(movieClipModifiers.size());
        }

        @Override
        public Tag getTag() {
            return Tag.MOVIE_CLIP_MODIFIERS;
        }
    }

    private record FlagSavable(Tag tag) implements Savable {
        @Override
        public void save(ByteStream stream) {

        }

        @Override
        public Tag getTag() {
            return tag;
        }
    }

    private record ExternalFilesSuffixesSavable(String highresSuffix,
                                                String lowresSuffix) implements Savable {
        @Override
        public void save(ByteStream stream) {
            stream.writeAscii(highresSuffix);
            stream.writeAscii(lowresSuffix);
        }

        @Override
        public Tag getTag() {
            return Tag.EXTERNAL_FILES_SUFFIXES;
        }
    }
}
