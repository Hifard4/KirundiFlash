package com.kirundiflash.flashcards.model;

/**
 * A single flashcard entry, loaded from one of the four CSV files
 * (known_verbs, unknown_verbs, known_words, unknown_words).
 */
public class FlashcardItem {

    public enum Type { VERB, WORD }

    private String english;
    private String kirundi;
    private Type type;

    public FlashcardItem(String english, String kirundi, Type type) {
        this.english = english;
        this.kirundi = kirundi;
        this.type = type;
    }

    public String getEnglish() { return english; }
    public String getKirundi() { return kirundi; }
    public Type getType() { return type; }

    /** Used to find/match this exact entry when moving it between CSV files. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlashcardItem)) return false;
        FlashcardItem other = (FlashcardItem) o;
        return english.equalsIgnoreCase(other.english)
                && kirundi.equalsIgnoreCase(other.kirundi);
    }

    @Override
    public int hashCode() {
        return (english.toLowerCase() + "|" + kirundi.toLowerCase()).hashCode();
    }
}
