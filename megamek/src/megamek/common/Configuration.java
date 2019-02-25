/*
* MegaMek -
* Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
* Copyright (C) 2018 The MegaMek Team
*
* This program is free software; you can redistribute it and/or modify it under
* the terms of the GNU General Public License as published by the Free Software
* Foundation; either version 2 of the License, or (at your option) any later
* version.
*
* This program is distributed in the hope that it will be useful, but WITHOUT
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
* details.
*/

package megamek.common;

import java.io.File;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*
 * TODO: Merge this with the PreferenceStore stuff and simplify/cleanup.
 * TODO: The 'common' library parts shouldn't be referring to these directories; any paths they need should be passed-in from the application.
 */

/**
 * Stores and provides access to the configuration of the MegaMek library.
 *
 * @author Edward Cullen
 *
 */
public final class Configuration {
    // **************************************************************************
    // A collection of default values.

    // **************************************************************************
    // Directories normally at the top of the game hierarchy.

    /** The default directory for user data */
    private static final String DEFAULT_USER_DATA_DIR = "userdata";

    /** The default configuration directory. */
    private static final String DEFAULT_DIR_NAME_CONFIG = "mmconf";

    /** The default data directory. */
    private static final String DEFAULT_DIR_NAME_DATA = "data";

    /** The default documentation directory. */
    private static final String DEFAULT_DIR_NAME_DOCS = "docs";

    // **************************************************************************
    // These are all directories that normally appear under 'data'.

    /** The default skin specification directory. */
    private static final String DEFAULT_DIR_NAME_SKINS = "skins";

    // **************************************************************************
    // These are all directories that normally appear under 'data'.

    /**
     * The default random army tables directory name (under the data directory).
     */
    private static final String DEFAULT_DIR_NAME_ARMY_TABLES = "rat";

    /** The default boards directory name (under the data directory). */
    private static final String DEFAULT_DIR_NAME_BOARDS = "boards";

    /** The default images directory name (under the data directory). */
    private static final String DEFAULT_DIR_NAME_IMAGES = "images";

    /** The default file that maps image filenames to locations withn an image atlas. */
    private static final String DEFAULT_FILE_NAME_IMG_FILE_ATLAS_MAP = "images/imgFileAtlasMap.xml";

    /** The default board backgrounds directory name (under the images directory). */
    private static final String DEFAULT_DIR_NAME_BOARD_BACKGROUNDS = "board_backgrounds";

    /** The default random names directory (under the data directory). */
    private static final String DEFAULT_DIR_NAME_NAMES = "names";

    /** The default unit files directory name (under the data directory). */
    private static final String DEFAULT_DIR_NAME_UNITS = "mechfiles";

    /** The default scenarios directory name (under the data directory). */
    private static final String DEFAULT_DIR_NAME_SCENARIOS = "scenarios";

    /** The default sounds directory name (under the data directory). */
    private static final String DEFAULT_DIR_NAME_SOUNDS = "sounds";

    /** The default force generator directory name (under the data directory). */
    private static final String DEFAULT_DIR_NAME_FORCE_GENERATOR = "forcegenerator";

    /** The default force generator directory name (under the data directory). */
    private static final String DEFAULT_DIR_NAME_FONTS = "fonts";

    // **************************************************************************
    // These are all directories that normally appear under 'data/images'.

    /** The default camo directory name (under the images directory). */
    private static final String DEFAULT_DIR_NAME_CAMO = "camo";

    /** The default fluff images directory name (under the images directory). */
    private static final String DEFAULT_DIR_NAME_FLUFF_IMAGES = "fluff";

    /** The default hex images directory name (under the images directory). */
    private static final String DEFAULT_DIR_NAME_HEXES = "hexes";

    /** The default misc images directory name (under the images directory). */
    private static final String DEFAULT_DIR_NAME_MISC_IMAGES = "misc";

    /** The default portrait images directory name (under the images directory). */
    private static final String DEFAULT_DIR_NAME_PORTRAIT_IMAGES = "portraits";

    /** The default unit images directory name (under the images directory). */
    private static final String DEFAULT_DIR_NAME_UNIT_IMAGES = "units";

    /** The default widgets directory name (under the images directory). */
    private static final String DEFAULT_DIR_NAME_WIDGETS = "widgets";

    // **************************************************************************
    // Static methods for accessing and modifying configuration data.

    /**
     * Return the configured userdata directory.
     *
     * @return {@link File} containing the path to the userdata directory.
     */
    public static File userdataDir() {
        lock.readLock().lock();
        try {
            return userdata_dir;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Return the configured configuration file directory.
     *
     * @return {@link File} containing the path to the config directory.
     */
    public static File configDir() {
        lock.readLock().lock();
        try {
            return config_dir;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Set the configuration directory.
     *
     * @param config_dir_path
     *            The path to the config directory.
     */
    public static void setConfigDir(final File config_dir_path) {
        lock.writeLock().lock();
        config_dir = (config_dir_path == null) ? new File(
                DEFAULT_DIR_NAME_CONFIG) : config_dir_path;
        lock.writeLock().unlock();
    }

    /**
     * Return the configured data directory.
     *
     * @return {@link File} containing the path to the data directory.
     */
    public static File dataDir() {
        lock.readLock().lock();
        try {
            return data_dir;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Set the data directory.
     *
     * @param data_dir_path
     *            The path to the data directory.
     */
    public static void setDataDir(final File data_dir_path) {
        lock.writeLock().lock();
        data_dir = (data_dir_path == null) ? new File(DEFAULT_DIR_NAME_DATA)
                : data_dir_path;
        lock.writeLock().unlock();
    }

    /**
     * Return the configured documentation directory.
     *
     * @return {@link File} containing the path to the documentation directory.
     */
    public static File docsDir() {
        lock.readLock().lock();
        try {
            return docs_dir;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Set the documentation directory.
     *
     * @param docs_dir_path
     *            The path to the documentation directory.
     */
    public static void setDocsDir(final File docs_dir_path) {
        lock.writeLock().lock();
        docs_dir = (docs_dir_path == null) ? new File(DEFAULT_DIR_NAME_DOCS)
                : docs_dir_path;
        lock.writeLock().unlock();
    }

    /**
     * Return the configured skin specification files directory.
     *
     * @return {@link File} containing the path to the skins directory.
     */
    public static File skinsDir() {
        lock.readLock().lock();
        try {
            return skins_dir;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Set the skins directory.
     *
     * @param skin_dir_path
     *            The path to the skins directory.
     */
    public static void setSkinDir(final File skin_dir_path) {
        lock.writeLock().lock();
        skins_dir = (skin_dir_path == null) ? new File(DEFAULT_DIR_NAME_CONFIG,
                DEFAULT_DIR_NAME_SKINS) : skin_dir_path;
        lock.writeLock().unlock();
    }

    /**
     * Return the configured army tables directory, if set, otherwise return the
     * default path, relative to the configured data directory.
     *
     * @return {@link File} containing the path to the army tables directory.
     */
    public static File armyTablesDir() {
        lock.readLock().lock();
        try {
            return (army_tables_dir != null) ? army_tables_dir : new File(
                    dataDir(), DEFAULT_DIR_NAME_ARMY_TABLES);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Set the army tables directory to an arbitrary location (<b>not</b>
     * relative to the data directory).
     *
     * @param army_tables_dir_path
     *            The path to the army tables directory.
     */
    public static void setArmyTablesDir(final File army_tables_dir_path) {
        lock.writeLock().lock();
        army_tables_dir = army_tables_dir_path;
        lock.writeLock().unlock();
    }

    /**
     * Return the configured boards directory, if set, otherwise return the
     * default path, relative to the configured data directory.
     *
     * @return {@link File} containing the path to the boards directory.
     */
    public static File boardsDir() {
        lock.readLock().lock();
        try {
            return (boards_dir != null) ? boards_dir : new File(dataDir(),
                    DEFAULT_DIR_NAME_BOARDS);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Set the boards directory to an arbitrary location (<b>not</b> relative to
     * the data directory).
     *
     * @param boards_dir_path
     *            dir path The path to the boards directory.
     */
    public static void setBoardsDir(final File boards_dir_path) {
        lock.writeLock().lock();
        boards_dir = boards_dir_path;
        lock.writeLock().unlock();
    }

    /**
     * Return the camo directory, which is relative to the images directory.
     *
     * @return {@link File} containing the path to the camo directory.
     */
    public static File camoDir() {
        return new File(imagesDir(), DEFAULT_DIR_NAME_CAMO);
    }

    /**
     * Return the hexes directory, which is relative to the images directory.
     *
     * @return {@link File} containing the path to the camo directory.
     */
    public static File hexesDir() {
        return new File(imagesDir(), DEFAULT_DIR_NAME_HEXES);
    }

    /**
     * Get the fluff images directory, which is relative to the images
     * directory.
     *
     * @return {@link File} containing the path to the fluff images directory.
     */
    public static File fluffImagesDir() {
        return new File(imagesDir(), DEFAULT_DIR_NAME_FLUFF_IMAGES);
    }

    /**
     * Return the configured images directory, if set, otherwise return the
     * default path, relative to the configured data directory.
     *
     * @return {@link File} containing the path to the images directory.
     */
    public static File imagesDir() {
        lock.readLock().lock();
        try {
            return (images_dir != null) ? images_dir : new File(dataDir(),
                    DEFAULT_DIR_NAME_IMAGES);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Set the images directory to an arbitrary location (<b>not</b> relative to
     * the data directory).
     *
     * @param images_dir_path
     *            The path to the images directory.
     */
    public static void setImagesDir(final File images_dir_path) {
        lock.writeLock().lock();
        images_dir = images_dir_path;
        lock.writeLock().unlock();
    }

    /**
     * Return the configured file that maps an image file to a location within
     * an image atlas, if set, otherwise return the default path, relative to
     * the configured data directory.
     *
     * @return {@link File} containing the path to the image file to atlas loc file.
     */
    public static File imageFileAtlasMapFile() {
        lock.readLock().lock();
        try {
            return (imgFileAtlasMapFile != null) ? imgFileAtlasMapFile : new File(dataDir(),
                    DEFAULT_FILE_NAME_IMG_FILE_ATLAS_MAP);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Set the  image file to atlas loc file to an arbitrary location (<b>not</b> relative to
     * the data directory).
     *
     * @param imgFileAtlasMapFilePath
     *            The path to the images directory.
     */
    public static void setImageFileAtlasMapFile(final File imgFileAtlasMapFilePath) {
        lock.writeLock().lock();
        imgFileAtlasMapFile = imgFileAtlasMapFilePath;
        lock.writeLock().unlock();
    }

    /**
     * Return the configured board backgrounds directory, if set, otherwise
     * return the default path, relative to the configured images directory.
     *
     * @return {@link File} containing the path to the images directory.
     */
    public static File boardBackgroundsDir() {
        lock.readLock().lock();
        try {
            return (board_backgrounds_dir != null) ? board_backgrounds_dir
                    : new File(imagesDir(), DEFAULT_DIR_NAME_BOARD_BACKGROUNDS);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Set the board backgrounds directory to an arbitrary location (<b>not</b>
     * relative to the images directory).
     *
     * @param board_background_dir_path
     *            The path to the images directory.
     */
    public static void setboardBackgroundsDir(
            final File board_background_dir_path) {
        lock.writeLock().lock();
        board_backgrounds_dir = board_background_dir_path;
        lock.writeLock().unlock();
    }

    /**
     * Return the configured units directory, if set, otherwise return the
     * default path, relative to the configured data directory.
     *
     * @return {@link File} containing the path to the units directory.
     */
    public static File unitsDir() {
        lock.readLock().lock();
        try {
            return (units_dir != null) ? units_dir : new File(dataDir(),
                    DEFAULT_DIR_NAME_UNITS);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Set the units directory to an arbitrary location (<b>not</b> relative to
     * the data directory).
     *
     * @param units_dir_path
     *            The path to the units directory.
     */
    public static void setUnitsDir(final File units_dir_path) {
        lock.writeLock().lock();
        units_dir = units_dir_path;
        lock.writeLock().unlock();
    }

    /**
     * Return the misc images directory, which is relative to the images
     * directory.
     *
     * @return {@link File} containing the path to the misc directory.
     */
    public static File miscImagesDir() {
        return new File(imagesDir(), DEFAULT_DIR_NAME_MISC_IMAGES);
    }

    /**
     * Return the portrait images directory, which is relative to the images
     * directory.
     *
     * @return {@link File} containing the path to the portrait directory.
     */
    public static File portraitImagesDir() {
        return new File(imagesDir(), DEFAULT_DIR_NAME_PORTRAIT_IMAGES);
    }

    /**
     * Return the configured names directory, if set, otherwise return the
     * default path, relative to the configured data directory.
     *
     * @return {@link File} containing the path to the names directory.
     */
    public static File namesDir() {
        lock.readLock().lock();
        try {
            return (names_dir != null) ? names_dir : new File(dataDir(),
                    DEFAULT_DIR_NAME_NAMES);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Set the names directory to an arbitrary location (<b>not</b> relative to
     * the data directory).
     *
     * @param names_dir_path
     *            The path to the names directory.
     */
    public static void setNamesDir(final File names_dir_path) {
        lock.writeLock().lock();
        names_dir = names_dir_path;
        lock.writeLock().unlock();
    }

    /**
     * Return the configured scenarios directory, if set, otherwise return the
     * default path, relative to the configured data directory.
     *
     * @return {@link File} containing the path to the scenarios directory.
     */
    public static File scenariosDir() {
        lock.readLock().lock();
        try {
            return (scenarios_dir != null) ? scenarios_dir : new File(
                    dataDir(), DEFAULT_DIR_NAME_SCENARIOS);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Set the scenarios directory to an arbitrary location (<b>not</b> relative
     * to the data directory).
     *
     * @param scenarios_dir_path
     *            The path to the scenarios directory.
     */
    public static void setScenariosDir(final File scenarios_dir_path) {
        lock.writeLock().lock();
        scenarios_dir = scenarios_dir_path;
        lock.writeLock().unlock();
    }

    /**
     * Return the configured sounds directory, if set, otherwise return the
     * default path, relative to the configured data directory.
     *
     * @return {@link File} containing the path to the sounds directory.
     */
    public static File soundsDir() {
        lock.readLock().lock();
        try {
            return (sounds_dir != null) ? sounds_dir : new File(dataDir(),
                    DEFAULT_DIR_NAME_SOUNDS);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Set the sounds directory to an arbitrary location (<b>not</b> relative to
     * the data directory).
     *
     * @param sounds_dir_path
     *            The path to the sounds directory.
     */
    public static void setSoundsDir(final File sounds_dir_path) {
        lock.writeLock().lock();
        sounds_dir = sounds_dir_path;
        lock.writeLock().unlock();
    }

    /**
     * Return the configured force generator data directory, if set, otherwise return the
     * default path, relative to the configured data directory.
     *
     * @return {@link File} containing the path to the force generator directory.
     */
    public static File forceGeneratorDir() {
        lock.readLock().lock();
        try {
            return (force_generator_dir != null) ? force_generator_dir : new File(
                    dataDir(), DEFAULT_DIR_NAME_FORCE_GENERATOR);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Set the force generator directory to an arbitrary location (<b>not</b>
     * relative to the data directory).
     *
     * @param force_generator_dir_path
     *            The path to the force generator directory.
     */
    public static void setForceGeneratorDir(final File force_generator_dir_path) {
        lock.writeLock().lock();
        force_generator_dir = force_generator_dir_path;
        lock.writeLock().unlock();
    }


    /**
     * Return the configured fonts data directory, if set, otherwise return the
     * default path, relative to the configured data directory.
     *
     * @return {@link File} containing the path to the force generator directory.
     */
    public static File fontsDir() {
        lock.readLock().lock();
        try {
            return (fonts_dir != null) ? fonts_dir : new File(
                    dataDir(), DEFAULT_DIR_NAME_FONTS);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Set the force generator directory to an arbitrary location (<b>not</b>
     * relative to the data directory).
     *
     * @param fontsDir
     *            The path to the force generator directory.
     */
    public static void setFontsDir(final File fontsDir) {
        lock.writeLock().lock();
        fonts_dir = fontsDir;
        lock.writeLock().unlock();
    }

    /**
     * Get the unit images directory, which is relative to the images directory.
     *
     * @return {@link File} containing the path to the unit images directory.
     */
    public static File unitImagesDir() {
        return new File(imagesDir(), DEFAULT_DIR_NAME_UNIT_IMAGES);
    }

    /**
     * Get the widgets directory, which is relative to the images directory.
     *
     * @return {@link File} containing the path to the widgets directory.
     */
    public static File widgetsDir() {
        return new File(imagesDir(), DEFAULT_DIR_NAME_WIDGETS);
    }

    // **************************************************************************
    // These are the mutable configuration items.

    /**
     * Read/write lock for the static data.
     *
     * This is a little paranoid, but at least I know it will work...
     */
    private static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /** The configured configuration directory. */
    private static File userdata_dir = new File(DEFAULT_USER_DATA_DIR);

    /** The configured configuration directory. */
    private static File config_dir = new File(DEFAULT_DIR_NAME_CONFIG);

    /** The configured data directory. */
    private static File data_dir = new File(DEFAULT_DIR_NAME_DATA);

    /** The configured documentation directory. */
    private static File docs_dir = new File(DEFAULT_DIR_NAME_DOCS);

    /** The configured skins directory. */
    private static File skins_dir = new File(DEFAULT_DIR_NAME_CONFIG,
            DEFAULT_DIR_NAME_SKINS);

    /** The configured army tables directory. */
    private static File army_tables_dir = null;

    /** The configured boards directory. */
    private static File boards_dir = null;

    /** The configured images directory. */
    private static File images_dir = null;

    /** The path to the imgFileAtlasMapFile. */
    private static File imgFileAtlasMapFile = null;

    /** The configured images directory. */
    private static File board_backgrounds_dir = null;

    /** The configured unit files directory. */
    private static File units_dir = null;

    /** The configured names directory. */
    private static File names_dir = null;

    /** The configured scenarios directory. */
    private static File scenarios_dir = null;

    /** The configured sounds directory. */
    private static File sounds_dir = null;

    /** The configured force generator directory. */
    private static File force_generator_dir = null;

    /** The configured force generator directory. */
    private static File fonts_dir = null;
}
