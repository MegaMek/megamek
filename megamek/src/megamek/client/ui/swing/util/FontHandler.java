/*
 * Copyright (c) 2023-2024 - The MegaMek Team. All Rights Reserved.
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
import megamek.client.ui.swing.CommonSettingsDialog;
import megamek.common.preference.PreferenceManager;
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

    /**
     * @return A standardized symbols font ("Material Symbols"). This font has a load of useful
     * icons. They are centered in a way that makes them less aesthetic to use within normal text, so
     * their primary use is for standalone symbols like marking hexes. Look for the symbol unicodes on
     * the linked website.
     *
     * @see <a href="https://fonts.google.com/icons">(Google) Material Symbols</a>
     */
    public static Font symbolFont() {
        return new Font("Material Symbols Rounded", Font.PLAIN, 12);
    }

    /**
     * @return The Noto Sans font which is included with the distribution and can be safely used everywhere.
     * It is advertised to have a wide language support.
     *
     * @see <a href="https://fonts.google.com/icons">(Google) Material Symbols</a>
     */
    public static Font getNotoFont() {
        return new Font("Noto Sans", Font.PLAIN, 14);
    }

    private void initializeFonts() {
        LogManager.getLogger().info("Loading fonts from " + MMConstants.FONT_DIRECTORY);
        parseFontsInDirectory(new File(MMConstants.FONT_DIRECTORY));

        String userDir = PreferenceManager.getClientPreferences().getUserDir();
        if (!userDir.isBlank()) {
            LogManager.getLogger().info("Loading fonts from " + userDir);
            parseFontsInDirectory(userDir);
        }

        LogManager.getLogger().info("Loading fonts from Java's GraphicsEnvironment");
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
    public static void parseFontsInDirectory(String directory) {
        parseFontsInDirectory(new File(directory));
    }

    /**
     * Searches the provided directory and all subdirectories and registers any truetype
     * fonts from .ttf files it finds.
     *
     * @param directory the directory to parse
     */
    public static void parseFontsInDirectory(final File directory) {
        for (String fontFile : CommonSettingsDialog.filteredFilesWithSubDirs(directory, MMConstants.TRUETYPE_FONT)) {
            try (InputStream fis = new FileInputStream(fontFile)) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, fis);
                if (!GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font)) {
                    LogManager.getLogger().error("Failed to register font " + fontFile);
                }
            } catch (Exception ex) {
                LogManager.getLogger().error("Failed to read font ", ex);
            }
        }
    }
}