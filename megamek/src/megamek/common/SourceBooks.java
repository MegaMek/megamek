/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.logging.MMLogger;

/**
 * This class manages sourcebook information, usually loaded from the data/sourcebooks folder. Sourcebooks are not
 * automatically loaded into memory when instantiating this class but are loaded on-demand only.
 */
public class SourceBooks {

    private static final MMLogger LOGGER = MMLogger.create(SourceBooks.class);
    private static final String SOURCEBOOKS_PATH = "data/sourcebooks";

    // Cache for abbreviation -> filename mapping (built lazily, shared across instances)
    private static Map<String, String> abbreviationIndex = null;
    private static final Object INDEX_LOCK = new Object();

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final File baseDirectory;

    /**
     * Creates a sourcebooks manager for any sourcebooks in the given directory. This constructor is for
     * testing/debugging.
     *
     * @param directoryPath The sourcebooks path to use
     */
    public SourceBooks(String directoryPath) {
        baseDirectory = new File(directoryPath);
        if (!baseDirectory.exists() || !baseDirectory.isDirectory()) {
            throw new IllegalArgumentException("Invalid directory: " + directoryPath);
        }
    }

    /**
     * Creates a sourcebooks manager for sourcebooks in the standard sourcebooks folder. This constructor does not load
     * anything and is a cheap operation; sourcebooks are only individually loaded on demand.
     */
    public SourceBooks() {
        this(SOURCEBOOKS_PATH);
    }

    /**
     * Returns a list of available sourcebook filenames (with the ".yaml" extension removed). The returned values can
     * directly be used to obtain a book. The returned list is empty if there are no source books in the folder or if
     * there is some error (IOExceptions are caught in this method).
     *
     * <pre>{@code
     *
     * SourceBooks sourceBooks = new SourceBooks();
     * List<String> allBooks = sourceBooks.availableSourcebooks();
     * String firstBook = allBooks.get(0) // assuming there are sourcebooks
     * Optional<SourceBook> book = sourceBooks.loadSourceBook(firstBook);
     *
     * }</pre>
     *
     * @return A list of available sourcebooks
     *
     * @see #loadSourceBook(String)
     */
    public List<String> availableSourcebooks() {
        try (var fileStream = Files.walk(baseDirectory.toPath())) {
            return fileStream
                  .filter(Files::isRegularFile)
                  .map(Path::getFileName)
                  .map(Path::toString)
                  .filter(s -> s.endsWith(".yaml"))
                  .map(s -> s.replace(".yaml", ""))
                  .toList();
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Tries to read a sourcebook file of the given name. The filename can include the .yaml extension but does not have
     * to.
     *
     * @param fileName The filename of the sourcebook to load
     *
     * @return The book, if it can be loaded.
     */
    public Optional<SourceBook> loadSourceBook(String fileName) {
        try {
            File yamlFile = new File(baseDirectory, prepareFilename(fileName));
            return Optional.of(yamlMapper.readValue(yamlFile, SourceBook.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private String prepareFilename(String input) {
        String filename = input.replaceAll("[/*?:\"<>|]", "_").trim();
        if (!filename.endsWith(".yaml")) {
            filename += ".yaml";
        }
        return filename;
    }

    /**
     * Finds a sourcebook by its abbreviation. This method handles common variations in abbreviation format (spaces,
     * ampersands, case differences).
     *
     * <p>Examples of abbreviations that will be matched:
     * <ul>
     *     <li>"TM" matches TechManual</li>
     *     <li>"TO: AU&amp;E" matches Tactical Operations: Advanced Units and Equipment</li>
     *     <li>"IO" matches Interstellar Operations (if YAML file exists)</li>
     * </ul>
     *
     * @param abbreviation The book abbreviation to search for
     *
     * @return The SourceBook if found, empty otherwise
     */
    public Optional<SourceBook> findByAbbreviation(String abbreviation) {
        if ((abbreviation == null) || abbreviation.isBlank()) {
            return Optional.empty();
        }

        ensureAbbrevIndexBuilt();

        String normalizedSearch = normalizeAbbrev(abbreviation);
        String filename = abbreviationIndex.get(normalizedSearch);

        if (filename != null) {
            return loadSourceBook(filename);
        }

        return Optional.empty();
    }

    /**
     * Ensures the abbreviation index is built. This loads all sourcebook YAML files to extract their abbreviations. The
     * index is cached statically to avoid rebuilding on each lookup.
     */
    private void ensureAbbrevIndexBuilt() {
        synchronized (INDEX_LOCK) {
            if (abbreviationIndex == null) {
                buildAbbrevIndex();
            }
        }
    }

    /**
     * Builds the abbreviation index by loading all sourcebook files and mapping their normalized abbreviations to
     * filenames.
     */
    private void buildAbbrevIndex() {
        abbreviationIndex = new HashMap<>();

        List<String> availableBooks = availableSourcebooks();
        for (String filename : availableBooks) {
            try {
                Optional<SourceBook> book = loadSourceBook(filename);
                if (book.isPresent() && (book.get().getAbbrev() != null)) {
                    String normalizedAbbrev = normalizeAbbrev(book.get().getAbbrev());
                    abbreviationIndex.put(normalizedAbbrev, filename);
                    LOGGER.trace("Indexed sourcebook: {} -> {}", normalizedAbbrev, filename);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to index sourcebook: {}", filename);
            }
        }

        LOGGER.debug("Built sourcebook abbreviation index with {} entries", abbreviationIndex.size());
    }

    /**
     * Normalizes an abbreviation for consistent matching. This handles common variations in how abbreviations are
     * written.
     *
     * <p>Normalization rules:
     * <ul>
     *     <li>Convert to lowercase</li>
     *     <li>Remove all spaces</li>
     *     <li>Remove ampersands (&amp;)</li>
     *     <li>Trim whitespace</li>
     * </ul>
     *
     * <p>Examples:
     * <ul>
     *     <li>"TO: AU&amp;E" becomes "to:aue"</li>
     *     <li>"TM" becomes "tm"</li>
     *     <li>"TO:AUE" becomes "to:aue"</li>
     * </ul>
     *
     * @param abbreviation The abbreviation to normalize
     *
     * @return The normalized abbreviation
     */
    private String normalizeAbbrev(String abbreviation) {
        if (abbreviation == null) {
            return "";
        }
        return abbreviation
                .toLowerCase()
                .replace(" ", "")
                .replace("&", "")
                .trim();
    }

    /**
     * Clears the cached abbreviation index. This is primarily useful for testing or when sourcebook files have been
     * modified.
     */
    public static void clearAbbrevIndex() {
        synchronized (INDEX_LOCK) {
            abbreviationIndex = null;
        }
    }

}
