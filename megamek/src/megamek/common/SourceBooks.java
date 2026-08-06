/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This class manages sourcebook information, usually loaded from the data/sourcebooks folder. Sourcebooks are not
 * automatically loaded into memory when instantiating this class but are loaded on-demand only.
 */
public class SourceBooks {

    private static final String SOURCEBOOKS_PATH = "data/sourcebooks";
    private static final Map<String, Optional<SourceBook>> SOURCE_BOOK_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> NON_CANON_BY_SOURCE_CACHE = new ConcurrentHashMap<>();

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final File baseDirectory;
    private final String cacheKeyPrefix;

    private static class StandardSourceBooksHolder {
        private static final SourceBooks INSTANCE = new SourceBooks();
    }

    public static SourceBooks getStandardSourceBooks() {
        return StandardSourceBooksHolder.INSTANCE;
    }

    /**
     * Creates a sourcebooks manager for any sourcebooks in the given directory. This constructor is for
     * testing/debugging.
     *
     * @param directoryPath The sourcebooks path to use
     */
    public SourceBooks(String directoryPath) {
        baseDirectory = new File(directoryPath);
        cacheKeyPrefix = baseDirectory.toPath().toAbsolutePath().normalize().toString();
        yamlMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
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
     * String firstBook = allBooks.getFirst() // assuming there are sourcebooks
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
        if ((fileName == null) || fileName.isBlank()) {
            return Optional.empty();
        }
        return SOURCE_BOOK_CACHE.computeIfAbsent(cacheKey(sourceBookKey(fileName)),
              ignored -> readSourceBook(fileName));
    }

    private Optional<SourceBook> readSourceBook(String fileName) {
        try {
            return Optional.of(yamlMapper.readValue(sourceBookFile(fileName), SourceBook.class));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Saves a sourcebook as a YAML file in the sourcebook directory.
     *
     * @param fileName   The sourcebook filename or key. The .yaml extension is optional.
     * @param sourceBook The sourcebook data to save.
     *
     * @throws IOException if the sourcebook cannot be written.
     */
    public void saveSourceBook(String fileName, SourceBook sourceBook) throws IOException {
        yamlMapper.writeValue(sourceBookFile(fileName), sourceBook);
        clearCaches();
    }

    public static void clearCaches() {
        SOURCE_BOOK_CACHE.clear();
        NON_CANON_BY_SOURCE_CACHE.clear();
    }

    public File sourceBookFile(String fileName) {
        return new File(baseDirectory, prepareFilename(fileName));
    }

    public String sourceBookKey(String fileName) {
        String preparedFilename = prepareFilename(fileName);
        return preparedFilename.substring(0, preparedFilename.length() - ".yaml".length());
    }

    /**
     * Loads any sourcebooks listed in a comma-separated source field.
     *
     * @param sourceList A comma-separated source list, such as "TR:3039,RG29"
     *
     * @return The sourcebooks that could be loaded, in source-list order
     */
    public List<SourceBook> loadSourceBooks(String sourceList) {
        return splitSourceList(sourceList).stream()
              .map(this::loadSourceBook)
              .flatMap(Optional::stream)
              .toList();
    }

    /**
     * @return true when no sourcebook entries are present or all listed sourcebooks are non-canon or cannot be loaded.
     */
    public boolean isNonCanonBySource(String source, String published) {
        Set<String> sourceNames = new LinkedHashSet<>();
        sourceNames.addAll(splitSourceList(source));
        sourceNames.addAll(splitSourceList(published));
        if (sourceNames.isEmpty()) {
            return true;
        }

        String normalizedSourceList = formatSourceList(sourceNames);
        return NON_CANON_BY_SOURCE_CACHE.computeIfAbsent(cacheKey(normalizedSourceList),
              ignored -> isNonCanonBySourceList(normalizedSourceList));
    }

    private boolean isNonCanonBySourceList(String sourceList) {
        return splitSourceList(sourceList).stream()
              .allMatch(this::isNonCanonSourceBook);
    }

    private boolean isNonCanonSourceBook(String sourceName) {
        return loadSourceBook(sourceName)
              .map(sourceBook -> !sourceBook.isCanon())
              .orElse(true);
    }

    public static List<String> splitSourceList(String sourceList) {
        if ((sourceList == null) || sourceList.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(sourceList.split(","))
              .map(String::trim)
              .filter(source -> !source.isEmpty())
              .toList();
    }

    public static String normalizeSourceList(String sourceList) {
        return formatSourceList(splitSourceList(sourceList));
    }

    public static String formatSourceList(Collection<String> sources) {
        if (sources == null) {
            return "";
        }
        return sources.stream()
              .filter(Objects::nonNull)
              .flatMap(source -> splitSourceList(source).stream())
              .collect(Collectors.joining(","));
    }

    private String prepareFilename(String input) {
        String filename = input.replaceAll("[/*?:\"<>|]", "_").trim();
        if (!filename.endsWith(".yaml")) {
            filename += ".yaml";
        }
        return filename;
    }

    private String cacheKey(String value) {
        return cacheKeyPrefix + "|" + value;
    }

}
