package dev.donutquine.swf;

/**
 * Describes a movie clip as an Export entry that can be accessed from the game library.
 *
 * @param id movie clip id
 * @param name string clip identifier, which is a key for the game library
 */
public record Export(int id, String name) {
}
