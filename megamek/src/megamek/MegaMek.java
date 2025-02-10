/*
 * MegaMek - Copyright (C) 2005, 2006 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (c) 2014-2024 - The MegaMek Team. All Rights Reserved.
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
package megamek;

import io.sentry.Sentry;
import megamek.client.ui.preferences.SuitePreferences;
import megamek.client.ui.swing.ButtonOrderPreferences;
import megamek.client.ui.swing.MegaMekGUI;
import megamek.client.ui.swing.util.FontHandler;
import megamek.common.annotations.Nullable;
import megamek.common.commandline.AbstractCommandLineParser;
import megamek.common.commandline.ClientServerCommandLineParser;
import megamek.common.commandline.MegaMekCommandLineFlag;
import megamek.common.commandline.MegaMekCommandLineParser;
import megamek.common.net.marshalling.SanityInputFilter;
import megamek.common.preference.PreferenceManager;
import megamek.logging.MMLogger;
import megamek.server.DedicatedServer;
import megamek.utilities.GifWriter;
import megamek.utilities.PrincessFineTuning;
import megamek.utilities.RATGeneratorEditor;

import javax.swing.*;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * This is the primary MegaMek class.
 *
 * @author mev
 */
public class MegaMek {
    private static final SuitePreferences mmPreferences = new SuitePreferences();
    private static final MMOptions mmOptions = new MMOptions();

    private static final NumberFormat numberFormatter = NumberFormat.getInstance();

    private static final MMLogger logger = MMLogger.create(MegaMek.class);
    private static final SanityInputFilter sanityInputFilter = new SanityInputFilter();

    public static boolean isDevelopment() {
        // env variable mm.profile=dev
        return "dev".equals(System.getenv("mm.profile"));
    }

    public static void main(String... args) {
        ObjectInputFilter.Config.setSerialFilter(sanityInputFilter);

        // Configure Sentry with defaults. Although the client defaults to enabled, the
        // properties file is used to disable it and additional configuration can be
        // done inside of the sentry.properties file. The defaults for everything else
        // is set here.
        Sentry.init(options -> {
            options.setEnableExternalConfiguration(true);
            options.setDsn("https://b1720cb789ec56df7df9610dfa463c09@sentry.tapenvy.us/8");
            options.setEnvironment("production");
            options.setTracesSampleRate(0.2);
            options.setDebug(true);
            options.setServerName("MegaMekClient");
            options.setRelease(SuiteConstants.VERSION.toString());
        });

        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

        // First, create a global default exception handler
        Thread.setDefaultUncaughtExceptionHandler((thread, t) -> {
            final String name = t.getClass().getName();
            final String message = String.format(MMLoggingConstants.UNHANDLED_EXCEPTION, name);
            final String title = String.format(MMLoggingConstants.UNHANDLED_EXCEPTION_TITLE, name);
            logger.error(t, message, title);
        });

        // Second, let's handle logging
        initializeLogging(MMConstants.PROJECT_NAME);

        // Third, Command Line Arguments and Startup
        MegaMekCommandLineParser parser = new MegaMekCommandLineParser(args);

        // Parse the command line arguments and throw an error if needed.
        try {
            parser.parse();
        } catch (AbstractCommandLineParser.ParseException e) {
            logger.fatal(e, String.format(MMLoggingConstants.AP_INCORRECT_ARGUMENTS, e.getMessage(),
                    parser.help()));
            System.exit(1);
        }

        // log jvm parameters
        logger.info(ManagementFactory.getRuntimeMXBean().getInputArguments());

        String[] restArgs = parser.getRestArgs();

        if (parser.dedicatedServer()) {
            startDedicatedServer(restArgs);
            return;
        }

        getMMPreferences().loadFromFile(SuiteConstants.MM_PREFERENCES_FILE);
        initializeSuiteGraphicalSetups(MMConstants.PROJECT_NAME);

        if (parser.host()) {
            startHost(restArgs);
            return;
        }

        if (parser.client()) {
            startClient(restArgs);
            return;
        }

        if (parser.quick()) {
            startQuickLoad(restArgs);
            return;
        }
        if (parser.writeGif()) {
            startGifWriter(restArgs);
            return;
        }
        if (parser.aiFineTuning()) {
            startPrincessFineTuning(restArgs);
            return;
        }
        if (parser.ratGenEditor()) {
            RATGeneratorEditor.main(restArgs);
        } else {
            startGUI();
        }
    }

    public static void initializeLogging(final String originProject) {
        logger.info(getUnderlyingInformation(originProject));
    }

    public static SuitePreferences getMMPreferences() {
        return mmPreferences;
    }

    public static MMOptions getMMOptions() {
        return mmOptions;
    }

    /**
     * Calculates the SHA-256 hash of the MegaMek.jar file
     * Used primarily for purposes of checksum comparison when connecting a new
     * client.
     *
     * @return String representing the SHA-256 hash
     */
    public static @Nullable String getMegaMekSHA256() {
        StringBuilder sb = new StringBuilder();

        String filename = "MegaMek.jar";
        if (new File("lib/" + filename).exists()) {
            filename = "lib/" + filename;
        }

        if (!new File(filename).exists()) {
            logger.warn("MegaMek.jar not found. Returning null checksum.");
            return null;
        }

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            logger.error(e, "SHA-256 Algorithm Can't be Found");
            return null;
        }

        try (InputStream is = new FileInputStream(filename); InputStream dis = new DigestInputStream(is, md)) {
            while (0 < dis.read()) {
                // Idle Timer...
            }

            byte[] digest = md.digest();
            for (byte d : digest) {
                sb.append(String.format("%02x", d));
            }
        } catch (Exception e) {
            logger.error(e, "Error Calculating Hash");
            return null;
        }

        return sb.toString();
    }

    /**
     * This function returns the memory used in the heap (heap memory - free
     * memory).
     *
     * @return memory used in kB
     */
    public static String getMemoryUsed() {
        long heap = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        return numberFormatter.format((heap - free) / 1024) + " kB";
    }

    /**
     * Starts a dedicated server with the arguments in args. See
     * {@link DedicatedServer#start(String[])} for more information.
     *
     * @param args the arguments to the dedicated server.
     */
    private static void startDedicatedServer(String... args) {
        logger.info(MMLoggingConstants.SC_STARTING_DEDICATED_SERVER, Arrays.toString(args));
        DedicatedServer.start(args);
    }

    /**
     * Skip splash GUI, starts a host
     * :megamek:run --args='-host'
     */
    private static void startHost(String... args) {
        ClientServerCommandLineParser parser = new ClientServerCommandLineParser(
                args,
                MegaMekCommandLineFlag.HOST.toString(),
                false,
                false,
                true);

        try {
            parser.parse();
        } catch (AbstractCommandLineParser.ParseException e) {
            final String message = String.format(MMLoggingConstants.AP_INCORRECT_ARGUMENTS, e.getMessage(),
                    parser.help());
            logger.error(e, message);
            System.exit(1);
        }

        ClientServerCommandLineParser.Resolver resolver = parser.getResolver(
                null,
                MMConstants.DEFAULT_PORT,
                MMConstants.LOCALHOST,
                PreferenceManager.getClientPreferences().getLastPlayerName());

        logger.info(MMLoggingConstants.SC_STARTING_HOST_SERVER, Arrays.toString(args));

        SwingUtilities.invokeLater(() -> {
            MegaMekGUI mmg = new MegaMekGUI();
            mmg.start(false);

            File gameFile = resolver.getSaveGameFile();
            mmg.startHost(
                    resolver.password,
                    resolver.port,
                    resolver.registerServer,
                    resolver.announceUrl,
                    resolver.mailPropertiesFile,
                    gameFile,
                    resolver.playerName);
        });
    }

    /**
     * Skip splash GUI, starts a host with using quicksave file
     */
    private static void startQuickLoad(String... args) {
        ClientServerCommandLineParser parser = new ClientServerCommandLineParser(
                args,
                MegaMekCommandLineFlag.HOST.toString(),
                false,
                false,
                true);

        try {
            parser.parse();
        } catch (AbstractCommandLineParser.ParseException e) {
            logger.error(e, String.format(MMLoggingConstants.AP_INCORRECT_ARGUMENTS, e.getMessage(),
                    parser.help()));
            System.exit(1);
        }

        ClientServerCommandLineParser.Resolver resolver = parser.getResolver(
                null,
                MMConstants.DEFAULT_PORT,
                MMConstants.LOCALHOST,
                PreferenceManager.getClientPreferences().getLastPlayerName());

        logger.info(MMLoggingConstants.SC_STARTING_HOST_SERVER, Arrays.toString(args));

        SwingUtilities.invokeLater(() -> {
            MegaMekGUI mmg = new MegaMekGUI();
            mmg.start(false);

            File gameFile = getQuickSaveFile();

            mmg.startHost(
                    resolver.password,
                    resolver.port,
                    resolver.registerServer,
                    resolver.announceUrl,
                    resolver.mailPropertiesFile,
                    gameFile,
                    resolver.playerName);
        });
    }

    /**
     * Skip splash GUI, starts a client session
     */
    private static void startClient(String... args) {
        ClientServerCommandLineParser parser = new ClientServerCommandLineParser(
                args,
                MegaMekCommandLineFlag.CLIENT.toString(),
                false,
                true,
                false);

        try {
            parser.parse();
        } catch (AbstractCommandLineParser.ParseException e) {
            logger.error(e, String.format(MMLoggingConstants.AP_INCORRECT_ARGUMENTS, e.getMessage(),
                    parser.help()));
            System.exit(1);
        }

        ClientServerCommandLineParser.Resolver resolver = parser.getResolver(
                null, MMConstants.DEFAULT_PORT, MMConstants.LOCALHOST,
                PreferenceManager.getClientPreferences().getLastPlayerName());

        logger.info(MMLoggingConstants.SC_STARTING_CLIENT_SERVER, Arrays.toString(args));

        SwingUtilities.invokeLater(() -> {
            MegaMekGUI mmg = new MegaMekGUI();
            mmg.start(false);
            mmg.startClient(resolver.playerName, resolver.serverAddress, resolver.port);
        });
    }

    private static void startGifWriter(String... args) {
        try {
            GifWriter.createGifFromGameSummary(args[0]);
        } catch (IOException e) {
            logger.error(e, "Error creating GIF");
        }
    }

    private static void startPrincessFineTuning(String... args) {
        PrincessFineTuning.main(args);
    }

    /**
     * Starts MegaMek's splash GUI
     */
    private static void startGUI() {
        logger.info("Starting MegaMekGUI.");
        SwingUtilities.invokeLater(() -> new MegaMekGUI().start(true));
    }

    /**
     * @param originProject the project that launched MegaMek
     * @return the underlying information for this launch of MegaMek
     */
    public static String getUnderlyingInformation(final String originProject) {
        return getUnderlyingInformation(originProject, MMConstants.PROJECT_NAME);
    }

    /**
     * @param originProject  the launching project
     * @param currentProject the currently described project
     * @return the underlying information for this launch
     */
    public static String getUnderlyingInformation(final String originProject, final String currentProject) {
        final LocalDateTime buildDate = getBuildDate();
        return String.format("""
                Starting %s v%s
                    Build Date: %s
                    Today: %s
                    Origin Project: %s
                    Java Vendor: %s
                    Java Version: %s
                    Platform: %s %s (%s)
                    System Locale: %s
                    Total memory available to %s: %,.0f GB
                    MM Code Revision: %s
                    MML Code Revision: %s
                    MHQ Code Revision: %s
                """,
                currentProject,
                SuiteConstants.VERSION,
                ((buildDate == null) ? "N/A" : buildDate),
                LocalDate.now(),
                originProject,
                System.getProperty("java.vendor"),
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"),
                Locale.getDefault(), currentProject,
                Runtime.getRuntime().maxMemory() / Math.pow(2, 30),
                Revision.mmRevision(),
                Revision.mmlRevision(),
                Revision.mhqRevision());
    }

    public static @Nullable LocalDateTime getBuildDate() {
        try {
            final URL url = Thread.currentThread().getContextClassLoader().getResource(JarFile.MANIFEST_NAME);

            if (url == null) {
                return null;
            }

            final Attributes attributes = new Manifest(url.openStream()).getMainAttributes();

            return LocalDateTime.parse(attributes.getValue("Build-Date"));
        } catch (Exception ignored) {
            return null;
        }
    }

    public static File getQuickSaveFile() {
        return new File(MMConstants.QUICKSAVE_PATH, MMConstants.QUICKSAVE_FILE + MMConstants.SAVE_FILE_GZ_EXT);
    }

    /**
     * This is used to initialize suite-wide graphical setups.
     *
     * @param currentProject the currently described project
     */
    public static void initializeSuiteGraphicalSetups(final String currentProject) {
        // Setup Fonts
        FontHandler.initialize();

        // Setup Themes
        UIManager.installLookAndFeel("Flat Light", "com.formdev.flatlaf.FlatLightLaf");
        UIManager.installLookAndFeel("Flat IntelliJ", "com.formdev.flatlaf.FlatIntelliJLaf");
        UIManager.installLookAndFeel("Flat Dark", "com.formdev.flatlaf.FlatDarkLaf");
        UIManager.installLookAndFeel("Flat Darcula", "com.formdev.flatlaf.FlatDarculaLaf");

        // Set a couple of things to make the Swing GUI look more "Mac-like" on Macs
        // Taken from:
        // http://www.devdaily.com/apple/mac/java-mac-native-look/Introduction.shtml
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", currentProject);

        // Setup Button Order Preferences
        ButtonOrderPreferences.getInstance().setButtonPriorities();
    }
}
