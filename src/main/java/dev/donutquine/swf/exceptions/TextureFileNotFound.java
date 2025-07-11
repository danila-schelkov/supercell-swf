package dev.donutquine.swf.exceptions;

public class TextureFileNotFound extends Exception {
    public TextureFileNotFound(String filepath) {
        super("Texture file is not found. Expected filepath: " + filepath);
    }
}
