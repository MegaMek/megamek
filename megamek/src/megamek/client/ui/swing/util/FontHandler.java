/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing.util;

import megamek.MMConstants;
import org.apache.logging.log4j.LogManager;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This singleton class reads in the available fonts when first called, including those in
 * {@link MMConstants#FONT_DIRECTORY}. Lists of available fonts can be obtained by
 * calling {@link #getAvailableNonSymbolFonts()} and {@link #getAvailableFonts()}.
 */
public final class FontHandler {

    private static final FontHandler instance = new FontHandler();
    private static final String SYMBOL_TEST_STRING = "abcdefgnzABCDEFGNZ1234567890/()[]";

    private final List<String> nonSymbolFontNames = new ArrayList<>();
    private final List<String> allFontNames = new ArrayList<>();
    volatile boolean initialized = false;

    /**
     * Returns a list of available font names excluding some symbol fonts (specifically, excluding fonts that
     * cannot display any of the characters "abcdefgnzABCDEFGNZ1234567890/()[]"). This list is only
     * read from the GraphicsEnvironment once and then not updated while the application is running.
     */
    public static List<String> getAvailableNonSymbolFonts() {
        if (!instance.initialized) {
            initialize();
        }
        return instance.nonSymbolFontNames;
    }

    /**
     * Returns a list of available font names. This list is only read from the GraphicsEnvironment once and
     * then not updated while the application is running.
     */
    public static List<String> getAvailableFonts() {
        if (!instance.initialized) {
            initialize();
        }
        return instance.allFontNames;
    }

    /**
     * Initializes the FontHandler, reading in and storing the available fonts for retrieval. Also reads in any
     * fonts in {@link MMConstants#FONT_DIRECTORY}.
     */
    public static void initialize() {
        synchronized(instance) {
            if (!instance.initialized) {
                instance.initializeFonts();
            }
        }
    }

    private void initializeFonts() {
        parseFontsInDirectory(new File(MMConstants.FONT_DIRECTORY));
        for (String fontName : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            allFontNames.add(fontName);
            Font font = Font.decode(fontName);
            if (font.canDisplayUpTo(SYMBOL_TEST_STRING) == -1) {
                nonSymbolFontNames.add(fontName);
            }
        }
        initialized = true;
    }

    /**
     * Searches the provided directory and all subdirectories and registers any truetype
     * fonts from .ttf files it finds.
     *
     * @param directory the directory to parse
     */
    public static void parseFontsInDirectory(final File directory) {
        final String[] filenames = directory.list();
        if (filenames == null) {
            return;
        }

        for (final String filename : filenames) {
            if (filename.toLowerCase().endsWith(MMConstants.TRUETYPE_FONT)) {
                try (InputStream fis = new FileInputStream(directory.getPath() + '/' + filename)) {
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(
                            Font.createFont(Font.TRUETYPE_FONT, fis));
                } catch (Exception ex) {
                    LogManager.getLogger().error("Failed to parse font", ex);
                }
            } else {
                final File file = new File(directory, filename);
                if (file.isDirectory()) {
                    parseFontsInDirectory(file);
                }
            }
        }
    }
}