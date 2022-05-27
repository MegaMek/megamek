/*
 * MegaMek - Copyright (C) 2005, 2006 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (c) 2014-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek;

import megamek.client.ui.preferences.SuitePreferences;
import megamek.client.ui.swing.ButtonOrderPreferences;
import megamek.client.ui.swing.MegaMekGUI;
import megamek.common.annotations.Nullable;
import megamek.common.commandline.AbstractCommandLineParser;
import megamek.common.commandline.ClientServerCommandLineParser;
import megamek.common.commandline.MegaMekCommandLineFlag;
import megamek.common.commandline.MegaMekCommandLineParser;
import megamek.common.preference.PreferenceManager;
import megamek.server.DedicatedServer;
import megamek.utilities.RATGeneratorEditor;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.io.*;
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
 * @author mev
 */
public class MegaMek {
    private static final SuitePreferences mmPreferences = new SuitePreferences();
    private static final MMOptions mmOptions = new MMOptions();

    private static final NumberFormat commafy = NumberFormat.getInstance();


    public static void main(String... args) {
        // First, create a global default exception handler
        Thread.setDefaultUncaughtExceptionHandler((thread, t) -> {
            LogManager.getLogger().error("Uncaught Exception Detected", t);
            final String name = t.getClass().getName();
            JOptionPane.showMessageDialog(null,
                    String.format("Uncaught %s detected. Please open up an issue containing all logs, the game save file, and customs at https://github.com/MegaMek/megamek/issues", name),
                    "Uncaught " + name, JOptionPane.ERROR_MESSAGE);
        });

        // Second, let's handle logging
        initializeLogging(MMConstants.PROJECT_NAME);

        // Third, Command Line Arguments and Startup
        MegaMekCommandLineParser parser = new MegaMekCommandLineParser(args);

        try {
            parser.parse();

            String[] restArgs = parser.getRestArgs();

            if (parser.dedicatedServer()) {
                startDedicatedServer(restArgs);
                return;
            }

            getMMPreferences().loadFromFile(MMConstants.MM_PREFERENCES_FILE);
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

            if (parser.ratGenEditor()) {
                RATGeneratorEditor.main(restArgs);
            } else {
                startGUI();
            }
        } catch (MegaMekCommandLineParser.ParseException e) {
            LogManager.getLogger().fatal(parser.formatErrorMessage(e));
            System.exit(1);
        }
    }

    public static void initializeLogging(final String originProject) {
        final String initialMessage = getUnderlyingInformation(originProject);
        LogManager.getLogger().info(initialMessage);
        handleLegacyLogging();
        System.out.println(initialMessage);
    }

    /**
     * This function redirects the standard error and output streams... It's a legacy method that
     * is currently being used as a fallback while migrating the last of our legacy logging and
     * testing out the new global exception handling
     */
    @Deprecated
    public static void handleLegacyLogging() {
        try {
            LogManager.getLogger().info("Redirecting System.out, System.err, and Throwable::printStackTrace output to legacy.log");
            File logDir = new File("logs");
            if (!logDir.exists()) {
                logDir.mkdir();
            }

            File file = new File(PreferenceManager.getClientPreferences().getLogDirectory()
                    + File.separator + "legacy.log");
            if (file.exists()) {
                try (PrintWriter writer = new PrintWriter(file)) {
                    writer.print("");
                } catch (Exception ex) {
                    LogManager.getLogger().error("Failed to write to legacy.log", ex);
                }
            }

            // Note: these are not closed on purpose
            PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(file, true), 64));
            System.setOut(ps);
            System.setErr(ps);
        } catch (Exception ex) {
            LogManager.getLogger().error("Unable to redirect output to legacy.log", ex);
        }
    }

    public static void printToOut(String text) {
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));
        out.print(text);
        out.flush();
        out.close();
    }

    public static void printToErr(String text) {
        PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.err));
        out.print(text);
        out.flush();
        out.close();
    }

    public static SuitePreferences getMMPreferences() {
        return mmPreferences;
    }

    public static MMOptions getMMOptions() {
        return mmOptions;
    }

    /**
     * Calculates the SHA-256 hash of the MegaMek.jar file
     * Used primarily for purposes of checksum comparison when connecting a new client.
     * @return String representing the SHA-256 hash
     */
    public static @Nullable String getMegaMekSHA256() {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[8192];

        // Assume UNIX/Linux, which has the jar in the root folder
        String filename = "MegaMek.jar";
        // If it isn't UNIX/Linux, maybe it's Windows where we've stashed it in the lib folder
        if (new File("lib/" + filename).exists()) {
            filename = "lib/" + filename;
            // And if it isn't either UNIX/Linux or Windows it's got to be Mac, where it's buried inside the app
        } else if (new File("MegaMek.app/Contents/Resources/Java/" + filename).exists()) {
            filename = "MegaMek.app/Contents/Resources/Java/" + filename;
        }

        MessageDigest md;
        // Calculate the digest for the given file.
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            LogManager.getLogger().error("", e);
            return null;
        }
        try (InputStream is = new FileInputStream(filename);
             InputStream dis = new DigestInputStream(is, md)) {
            while (0 < dis.read(buffer)) {

            }
            // gets digest
            byte[] digest = md.digest();
            // convert the byte to hex format
            for (byte d : digest) {
                sb.append(String.format("%02x", d));
            }
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            return null;
        }
        return sb.toString();
    }

    /**
     * This function returns the memory used in the heap (heap memory - free memory).
     *
     * @return memory used in kB
     */
    public static String getMemoryUsed() {
        long heap = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        return commafy.format((heap - free) / 1024) + " kB";
    }

    /**
     * Starts a dedicated server with the arguments in args. See
     * {@link DedicatedServer#start(String[])} for more information.
     *
     * @param args the arguments to the dedicated server.
     */
    private static void startDedicatedServer(String... args) {
        LogManager.getLogger().info("Starting Dedicated Server. " + Arrays.toString(args));
        DedicatedServer.start(args);
    }

    /**
     * Skip splash GUI, starts a host
     * :megamek:run --args='-host'
     */
    private static void startHost(String... args) {
        ClientServerCommandLineParser parser = new ClientServerCommandLineParser(args,
                MegaMekCommandLineFlag.HOST.toString(), false, false, true);
        try {
            parser.parse();
        } catch (AbstractCommandLineParser.ParseException e) {
            LogManager.getLogger().error(parser.formatErrorMessage(e));
            MegaMek.printToErr(parser.formatErrorMessage(e) + "\n");
            System.exit(1);
        }

        ClientServerCommandLineParser.Resolver resolver = parser.getResolver(
                null, MMConstants.DEFAULT_PORT, MMConstants.LOCALHOST,
                PreferenceManager.getClientPreferences().getLastPlayerName() );
        LogManager.getLogger().info("Starting Host Server. " + Arrays.toString(args));

        MegaMekGUI mmg = new MegaMekGUI();
        mmg.start(false);
        File gameFile = null;
        if (resolver.saveGameFileName != null ) {
            gameFile = new File(resolver.saveGameFileName);
            if (!gameFile.isAbsolute()) {
                gameFile = new File("./savegames", resolver.saveGameFileName);
            }
        }

        mmg.startHost(resolver.password, resolver.port, resolver.registerServer,
                resolver.announceUrl, resolver.mailPropertiesFile, gameFile,
                resolver.playerName );
    }

    /**
     * Skip splash GUI, starts a host with using quicksave file
     */
    private static void startQuickLoad(String... args) {
        LogManager.getLogger().info("Starting Quick Load Host Server. " + Arrays.toString(args));
        MegaMekGUI mmg = new MegaMekGUI();
        mmg.start(false);
        mmg.quickLoadGame();
    }

    /**
     * Skip splash GUI, starts a client session
     */
    private static void startClient(String... args) {
        ClientServerCommandLineParser parser = new ClientServerCommandLineParser(args,
                MegaMekCommandLineFlag.CLIENT.toString(), false, true, false);
        try {
            parser.parse();
        } catch (AbstractCommandLineParser.ParseException e) {
            LogManager.getLogger().error(parser.formatErrorMessage(e));
            MegaMek.printToErr(parser.formatErrorMessage(e) + "\n");
            System.exit(1);
        }

        ClientServerCommandLineParser.Resolver resolver = parser.getResolver(
                null, MMConstants.DEFAULT_PORT, MMConstants.LOCALHOST,
                PreferenceManager.getClientPreferences().getLastPlayerName());

        LogManager.getLogger().info("Starting Client Server. " + Arrays.toString(args));
        MegaMekGUI mmg = new MegaMekGUI();
        mmg.start(false);
        mmg.startClient(resolver.playerName, resolver.serverAddress, resolver.port);
    }

    /**
     * Starts MegaMek's splash GUI
     */
    private static void startGUI() {
        LogManager.getLogger().info("Starting MegaMekGUI.");
        new MegaMekGUI().start(true);
    }

    /**
     * @param originProject the project that launched MegaMek
     * @return the underlying information for this launch of MegaMek
     */
    public static String getUnderlyingInformation(final String originProject) {
        return getUnderlyingInformation(originProject, MMConstants.PROJECT_NAME);
    }

    /**
     * @param originProject the launching project
     * @param currentProject the currently described project
     * @return the underlying information for this launch
     */
    public static String getUnderlyingInformation(final String originProject,
                                                  final String currentProject) {
        final LocalDateTime buildDate = getBuildDate();
        return String.format("Starting %s v%s\n\tBuild Date: %s\n\tRelease Date: %s\n\tToday: %s\n\tOrigin Project: %s\n\tJava Vendor: %s\n\tJava Version: %s\n\tPlatform: %s %s (%s)\n\tSystem Locale: %s\n\tTotal memory available to %s: %,.0f GB",
                currentProject, MMConstants.VERSION, ((buildDate == null) ? "N/A" : buildDate),
                MMConstants.RELEASE_DATE, LocalDate.now(), originProject,
                System.getProperty("java.vendor"), System.getProperty("java.version"),
                System.getProperty("os.name"), System.getProperty("os.version"),
                System.getProperty("os.arch"), Locale.getDefault(), currentProject,
                Runtime.getRuntime().maxMemory() / Math.pow(2, 30));
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

    /**
     * This is used to initialize suite-wide graphical setups.
     * @param currentProject the currently described project
     */
    public static void initializeSuiteGraphicalSetups(final String currentProject) {
        // Setup Fonts
        parseFontDirectory(new File(MMConstants.FONT_DIRECTORY));

        // Setup Themes
        UIManager.installLookAndFeel("Flat Light", "com.formdev.flatlaf.FlatLightLaf");
        UIManager.installLookAndFeel("Flat IntelliJ", "com.formdev.flatlaf.FlatIntelliJLaf");
        UIManager.installLookAndFeel("Flat Dark", "com.formdev.flatlaf.FlatDarkLaf");
        UIManager.installLookAndFeel("Flat Darcula", "com.formdev.flatlaf.FlatDarculaLaf");

        // Set a couple of things to make the Swing GUI look more "Mac-like" on Macs
        // Taken from: http://www.devdaily.com/apple/mac/java-mac-native-look/Introduction.shtml
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", currentProject);

        // Setup Button Order Preferences
        ButtonOrderPreferences.getInstance().setButtonPriorities();
    }

    /**
     * Recursively search the provided directory, attempting to create and then register truetype
     * fonts from .ttf files
     * @param directory the directory to parse
     */
    private static void parseFontDirectory(final File directory) {
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
                    parseFontDirectory(file);
                }
            }
        }
    }
}
