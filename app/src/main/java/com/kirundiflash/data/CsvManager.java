package com.kirundiflash.data;

import android.content.Context;

import com.kirundiflash.flashcards.model.FlashcardItem;
import com.kirundiflash.flashcards.model.FlashcardItem.Type;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages the four local CSV files that track learning progress:
 *   known_verbs.csv, unknown_verbs.csv, known_words.csv, unknown_words.csv
 *
 * Files live in the app's private filesDir (NOT external storage), so no
 * runtime storage permission is needed.
 *
 * On first launch, seed_verbs.csv and seed_words.csv (bundled in assets)
 * are copied entirely into unknown_verbs.csv / unknown_words.csv, and the
 * known_* files are created empty.
 *
 * NOTE: This uses plain BufferedReader/FileWriter rather than a CSV library.
 * Our format is simple (two columns, no embedded commas/quotes in the
 * Kirundi/English text), so a library is unnecessary overhead. If your
 * content ever needs commas or quotes inside a field, tell me and I'll
 * swap this for proper CSV escaping (or the opencsv library already in
 * build.gradle).
 */
public class CsvManager {

    private static final String KNOWN_VERBS = "known_verbs.csv";
    private static final String UNKNOWN_VERBS = "unknown_verbs.csv";
    private static final String KNOWN_WORDS = "known_words.csv";
    private static final String UNKNOWN_WORDS = "unknown_words.csv";

    private static final String HEADER = "english,kirundi";

    private final Context context;

    public CsvManager(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Call once, e.g. from a splash screen or on first app launch.
     * Safe to call every launch -- it no-ops if files already exist.
     */
    public void initializeIfNeeded() {
        File knownVerbs = new File(context.getFilesDir(), KNOWN_VERBS);
        File unknownVerbs = new File(context.getFilesDir(), UNKNOWN_VERBS);
        File knownWords = new File(context.getFilesDir(), KNOWN_WORDS);
        File unknownWords = new File(context.getFilesDir(), UNKNOWN_WORDS);

        try {
            if (!unknownVerbs.exists()) {
                copyAssetToFile("seed_verbs.csv", unknownVerbs);
            }
            if (!knownVerbs.exists()) {
                writeHeaderOnly(knownVerbs);
            }
            if (!unknownWords.exists()) {
                copyAssetToFile("seed_words.csv", unknownWords);
            }
            if (!knownWords.exists()) {
                writeHeaderOnly(knownWords);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize CSV files", e);
        }
    }

    /** Returns all entries the user has NOT yet marked as known, for the given type. */
    public List<FlashcardItem> getUnknown(Type type) {
        return readFile(fileFor(type, false), type);
    }

    /** Returns all entries the user HAS marked as known, for the given type. */
    public List<FlashcardItem> getKnown(Type type) {
        return readFile(fileFor(type, true), type);
    }

    /**
     * Moves an item from the "unknown" file to the "known" file.
     * Call this when the user taps "I know" on a flashcard.
     */
    public synchronized void markAsKnown(FlashcardItem item) {
        removeFromFile(fileFor(item.getType(), false), item);
        appendToFile(fileFor(item.getType(), true), item);
    }

    /**
     * Moves an item from the "known" file back to the "unknown" file.
     * Call this when the user taps "I don't know" on a flashcard that
     * was previously known (so it reappears sooner), OR when a card in
     * the unknown deck is tapped "I don't know" again (no-op move, but
     * harmless -- it just stays / re-appends).
     */
    public synchronized void markAsUnknown(FlashcardItem item) {
        removeFromFile(fileFor(item.getType(), true), item);
        // Avoid duplicate entries if it's already in the unknown file
        List<FlashcardItem> current = readFile(fileFor(item.getType(), false), item.getType());
        if (!current.contains(item)) {
            appendToFile(fileFor(item.getType(), false), item);
        }
    }

    /** Counts for dashboard cards, e.g. "120/450 Mastered". */
    public int countKnown(Type type) {
        return getKnown(type).size();
    }

    public int countTotal(Type type) {
        return getKnown(type).size() + getUnknown(type).size();
    }

    // ---------------- internal helpers ----------------

    private File fileFor(Type type, boolean known) {
        String name;
        if (type == Type.VERB) {
            name = known ? KNOWN_VERBS : UNKNOWN_VERBS;
        } else {
            name = known ? KNOWN_WORDS : UNKNOWN_WORDS;
        }
        return new File(context.getFilesDir(), name);
    }

    private void copyAssetToFile(String assetName, File destination) throws IOException {
        try (InputStream in = context.getAssets().open(assetName);
             FileWriter writer = new FileWriter(destination)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.write("\n");
            }
        }
    }

    private void writeHeaderOnly(File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(HEADER);
            writer.write("\n");
        }
    }

    private List<FlashcardItem> readFile(File file, Type type) {
        List<FlashcardItem> items = new ArrayList<>();
        if (!file.exists()) return items;

        try (BufferedReader reader = new BufferedReader(new java.io.FileReader(file))) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                int commaIndex = line.indexOf(',');
                if (commaIndex < 0) continue;
                String english = line.substring(0, commaIndex).trim();
                String kirundi = line.substring(commaIndex + 1).trim();
                items.add(new FlashcardItem(english, kirundi, type));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + file.getName(), e);
        }
        return items;
    }

    private void appendToFile(File file, FlashcardItem item) {
        try (FileWriter writer = new FileWriter(file, true)) { // append mode
            writer.write(item.getEnglish() + "," + item.getKirundi() + "\n");
        } catch (IOException e) {
            throw new RuntimeException("Failed to append to " + file.getName(), e);
        }
    }

    private void removeFromFile(File file, FlashcardItem item) {
        List<FlashcardItem> remaining = new ArrayList<>();
        for (FlashcardItem existing : readFile(file, item.getType())) {
            if (!existing.equals(item)) {
                remaining.add(existing);
            }
        }
        try (FileWriter writer = new FileWriter(file, false)) { // overwrite
            writer.write(HEADER + "\n");
            for (FlashcardItem existing : remaining) {
                writer.write(existing.getEnglish() + "," + existing.getKirundi() + "\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to rewrite " + file.getName(), e);
        }
    }
}
