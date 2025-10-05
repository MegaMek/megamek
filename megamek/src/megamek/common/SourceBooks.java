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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * This class manages sourcebook information, usually loaded from the data/sourcebooks folder. Sourcebooks are not
 * automatically loaded into memory when instantiating this class but are loaded on-demand only.
 */
public class SourceBooks {

    private static final String SOURCEBOOKS_PATH = "data/sourcebooks";

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

}
