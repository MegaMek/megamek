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

import megamek.client.ui.Messages;
import megamek.client.ui.preferences.SuitePreferences;
import megamek.client.ui.swing.ButtonOrderPreferences;
import megamek.client.ui.swing.ClientGUI;
import megamek.client.ui.swing.MegaMekGUI;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.AbstractCommandLineParser;
import megamek.common.util.ClientServerCommandLineParser;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.verifier.*;
import megamek.server.DedicatedServer;
import megamek.utils.RATGeneratorEditor;
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
import java.util.Vector;
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

            if (parser.host()) {
                startHost(restArgs);
                return;
            }

            if (parser.client()) {
                startClient(restArgs);
                return;
            }

            if (parser.quick())
            {
                startQuickLoad(restArgs);
            }

            getMMPreferences().loadFromFile(MMConstants.MM_PREFERENCES_FILE);
            initializeSuiteGraphicalSetups(MMConstants.PROJECT_NAME);

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
                MegaMekCommandLineParser.MegaMekCommandLineFlag.HOST.toString(), false, false, true);
        try {
            parser.parse();
        } catch (AbstractCommandLineParser.ParseException e) {
            LogManager.getLogger().error(parser.formatErrorMessage(e));
        }

        LogManager.getLogger().info("Starting Host Server. " + Arrays.toString(args));
        MegaMekGUI mmg = new MegaMekGUI();
        mmg.start(false);
        File savegame = null;
        if (parser.getSaveGameFileName() != null ) {
            savegame = new File(parser.getSaveGameFileName());
            if (!savegame.isAbsolute()) {
                savegame = new File("./savegames", parser.getSaveGameFileName());
            }
        }

        mmg.startHost(parser.getPassword(), parser.getPort(), parser.getRegister(),
                parser.getAnnounceUrl(), savegame, parser.getPlayerName() );
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
                MegaMekCommandLineParser.MegaMekCommandLineFlag.CLIENT.toString() ,false, true, false);
        try {
            parser.parse();
        } catch (AbstractCommandLineParser.ParseException e) {
            LogManager.getLogger().error(parser.formatErrorMessage(e));
        }
        LogManager.getLogger().info("Starting Client Server. " + Arrays.toString(args));
        MegaMekGUI mmg = new MegaMekGUI();
        mmg.start(false);
        mmg.startClient(parser.getPlayerName(), parser.getHostName(), parser.getPort());
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

    /**
     * This class parses the options passed into to MegaMek from the command line.
     */
    public static class MegaMekCommandLineParser extends AbstractCommandLineParser {
        private boolean dedicatedServer = false;
        private boolean host = false;
        private boolean client = false;
        private boolean quick = false;
        private boolean ratGenEditor = false;
        private String[] restArgs = new String[0];

        public enum MegaMekCommandLineFlag {
            //region Enum Declarations
            // standard game options
            HELP(Messages.getString("MegaMek.Help")),
            DEDICATED(Messages.getString("MegaMek.Help.Dedicated")),
            HOST(Messages.getString("MegaMek.Help.Host")),
            CLIENT(Messages.getString("MegaMek.Help.Client")),
            QUICK(Messages.getFormattedString("MegaMek.Help.Quick", ClientGUI.QUICKSAVE_FILE)),
            // exporters and utilities
            EQDB("MegaMek.Help.EquipmentDB"),
            EQEDB(Messages.getString("MegaMek.Help.EquipmentExtendedDB")),
            EXPORT(Messages.getString("MegaMek.Help.UnitExport")),
            VALIDATE(Messages.getString("MegaMek.Help.UnitValidator")),
            OUL(Messages.getString("MegaMek.Help.OfficialUnitList")),
            BFC(Messages.getString("MegaMek.Help.UnitBattleforceConversion")),
            ASC(Messages.getString("MegaMek.Help.UnitAlphastrikeConversion")),
            EDITRATGEN(Messages.getString("MegaMek.Help.RatgenEdit")),
            DATADIR(Messages.getFormattedString("MegaMek.Help.DataDir",  Configuration.dataDir()));
            //endregion Enum Declarations

            private final String helpText;

            MegaMekCommandLineFlag(final String helpText) {
                this.helpText = helpText;
            }

            public static MegaMekCommandLineFlag parseFromString(final String text) {
                try {
                    return valueOf(text.toUpperCase(Locale.ROOT));
                } catch (Exception ex) {
                    LogManager.getLogger().error("Failed to parse the MegaMekCommandLineFlag from text '%s'",text);
                    throw(ex);
                }
            }
        }

        private static final String INCORRECT_ARGUMENTS_MESSAGE = "Incorrect arguments:";

        MegaMekCommandLineParser(String... args) {
            super(args);
        }

        boolean dedicatedServer() {
            return dedicatedServer;
        }

        boolean host() {
            return host;
        }

        boolean client() {
            return client;
        }

        boolean quick() {
            return quick;
        }

        /**
         * Flag that indicates the option for the RAT Generator editor
         * @return Whether the RAT Generator editor should be invoked
         */
        boolean ratGenEditor() {
            return ratGenEditor;
        }

        /**
         * @return the the <code>array</code> of the unprocessed arguments
         */
        String[] getRestArgs() {
            return restArgs;
        }

        @Override
        public String formatErrorMessage(ParseException e) {
            return (INCORRECT_ARGUMENTS_MESSAGE + e.getMessage() + '\n'
                    + help());
        }

        public void printHelp() {
            PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));
            out.print(help());
            out.flush();
            out.close();
        }

        public String help() {
            StringBuilder sb = new StringBuilder();
            sb.append(Messages.getString("MegaMek.Version") + MMConstants.VERSION+"\n");
            for( MegaMekCommandLineFlag flag : MegaMekCommandLineFlag.values() ) {
                    sb.append(String.format("-%s %s\n", flag.toString().toLowerCase(), flag.helpText));
            }
            return sb.toString();
        }

        @Override
        protected void start() throws ParseException {
            if (getTokenType() == TOK_OPTION) {
                final String tokenVal = getTokenValue();
                nextToken();
                try {
                    switch (MegaMekCommandLineFlag.parseFromString(tokenVal)) {
                        case HELP:
                            printHelp();
                            System.exit(0);
                        case EQDB:
                            processEquipmentDb();
                            break;
                        case EQEDB:
                            processExtendedEquipmentDb();
                            break;
                        case DATADIR:
                            processDataDir();
                            break;
                        case VALIDATE:
                            processUnitValidator();
                            break;
                        case EXPORT:
                            processUnitExporter();
                            break;
                        case OUL:
                            processUnitExporter(true);
                            break;
                        case BFC:
                            processUnitBattleForceConverter();
                            break;
                        case ASC:
                            processUnitAlphaStrikeConverter();
                            break;
                        case DEDICATED:
                            dedicatedServer = true;
                            break;
                        case HOST:
                            host = true;
                            break;
                        case CLIENT:
                            client = true;
                            break;
                        case QUICK:
                            quick = true;
                            break;
                        case EDITRATGEN:
                            ratGenEditor = true;
                            break;
                    }
                } catch (ParseException ex) {
                    PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));
                    out.print(formatErrorMessage(ex));
                    out.close();
                    printHelp();
                    throw ex;
                }
            }
            processRestOfInput();
            if (getTokenType() != TOK_EOF) {
                throw new ParseException("unexpected input");
            }
        }

        private void processEquipmentDb() throws ParseException {
            String filename;
            if (getTokenType() == TOK_LITERAL) {
                filename = getTokenValue();
                nextToken();
                megamek.common.EquipmentType.writeEquipmentDatabase(new File(filename));
            } else {
                throw new ParseException("file name expected");
            }
            System.exit(0);
        }

        private void processExtendedEquipmentDb() throws ParseException {
            String filename;
            if (getTokenType() == TOK_LITERAL) {
                filename = getTokenValue();
                nextToken();
                megamek.common.EquipmentType.writeEquipmentExtendedDatabase(new File(filename));
            } else {
                throw new ParseException("file name expected");
            }
            System.exit(0);
        }

        private void processDataDir() throws ParseException {
            String dataDirName;
            if (getTokenType() == TOK_LITERAL) {
                dataDirName = getTokenValue();
                nextToken();
                Configuration.setDataDir(new File(dataDirName));
            } else {
                throw new ParseException("directory name expected");
            }
        }

        private void processUnitValidator() throws ParseException {
            String filename;
            if (getTokenType() == TOK_LITERAL) {
                filename = getTokenValue();
                nextToken();
                MechSummary ms = MechSummaryCache.getInstance().getMech(filename);
                if (ms == null) {
                    MechSummary[] units = MechSummaryCache.getInstance().getAllMechs();
                    for (MechSummary unit : units) {
                        if (unit.getSourceFile().getName().equalsIgnoreCase(filename)) {
                            ms = unit;
                            break;
                        }
                    }
                }

                if (ms == null) {
                    LogManager.getLogger().error(new IOException(filename + " not found.  Try using \"chassis model\" for input."));
                } else {
                    try {
                        Entity entity = new MechFileParser(ms.getSourceFile(),
                                ms.getEntryName()).getEntity();
                        LogManager.getLogger().info("Validating Entity: " + entity.getShortNameRaw());
                        EntityVerifier entityVerifier = EntityVerifier.getInstance(
                                new MegaMekFile(Configuration.unitsDir(),
                                        EntityVerifier.CONFIG_FILENAME).getFile());
                        MechView mechView = new MechView(entity, false);
                        StringBuffer sb = new StringBuffer(mechView.getMechReadout());
                        if ((entity instanceof Mech) || (entity instanceof Tank)
                                || (entity instanceof Aero) || (entity instanceof BattleArmor)) {
                            TestEntity testEntity = null;
                            if (entity instanceof Mech) {
                                testEntity = new TestMech((Mech) entity, entityVerifier.mechOption,
                                        null);
                            } else if ((entity instanceof Tank) && !(entity instanceof GunEmplacement)) {
                                if (entity.isSupportVehicle()) {
                                    testEntity = new TestSupportVehicle(entity,
                                            entityVerifier.tankOption, null);
                                } else {
                                    testEntity = new TestTank((Tank) entity,
                                            entityVerifier.tankOption, null);
                                }
                            } else if ((entity.getEntityType() == Entity.ETYPE_AERO)
                                    && (entity.getEntityType() != Entity.ETYPE_DROPSHIP)
                                    && (entity.getEntityType() != Entity.ETYPE_SMALL_CRAFT)
                                    && (entity.getEntityType() != Entity.ETYPE_FIGHTER_SQUADRON)
                                    && (entity.getEntityType() != Entity.ETYPE_JUMPSHIP)
                                    && (entity.getEntityType() != Entity.ETYPE_SPACE_STATION)) {
                                testEntity = new TestAero((Aero) entity,
                                        entityVerifier.aeroOption, null);
                            } else if (entity instanceof BattleArmor) {
                                testEntity = new TestBattleArmor((BattleArmor) entity,
                                        entityVerifier.baOption, null);
                            }

                            if (testEntity != null) {
                                testEntity.correctEntity(sb);
                            }
                        }
                        LogManager.getLogger().info(sb.toString());
                    } catch (Exception ex) {
                        throw new ParseException("\"chassis model\" expected as input");
                    }
                }
            } else {
                throw new ParseException("\"chassis model\" expected as input");
            }
            System.exit(0);
        }

        private void processUnitBattleForceConverter() {
            String filename;
            if (getTokenType() == TOK_LITERAL) {
                filename = getTokenValue();
                nextToken();

                if (!new File("./docs").exists()) {
                    if (!new File("./docs").mkdir()) {
                        LogManager.getLogger().error(
                                "Error in creating directory ./docs. We know this is annoying, and apologise. "
                                        + "Please submit a bug report at https://github.com/MegaMek/megamek/issues "
                                        + " and we will try to resolve your issue.");
                    }
                }
                File file = new File("./docs/" + filename);
                try (Writer w = new FileWriter(file); BufferedWriter fw = new BufferedWriter(w)) {
                    fw.write("Megamek Unit BattleForce Converter");
                    fw.newLine();
                    fw.write("This file can be regenerated with java -jar MegaMek.jar -bfc filename");
                    fw.newLine();
                    fw.write("Element\tSize\tMP\tArmor\tStructure\tS/M/L\tOV\tPoint Cost\tAbilities");
                    fw.newLine();

                    MechSummary[] units = MechSummaryCache.getInstance().getAllMechs();
                    for (MechSummary unit : units) {
                        Entity entity = new MechFileParser(unit.getSourceFile(),
                                unit.getEntryName()).getEntity();

                        BattleForceElement bfe = new BattleForceElement(entity);
                        bfe.writeCsv(fw);
                    }
                } catch (Exception e) {
                    LogManager.getLogger().error("", e);
                }
            }

            System.exit(0);
        }

        private void processUnitAlphaStrikeConverter() {
            String filename;
            if (getTokenType() == TOK_LITERAL) {
                filename = getTokenValue();
                nextToken();

                if (!new File("./docs").exists()) {
                    if (!new File("./docs").mkdir()) {
                        LogManager.getLogger().error(
                                "Error in creating directory ./docs. We know this is annoying, and apologise. "
                                        + "Please submit a bug report at https://github.com/MegaMek/megamek/issues "
                                        + " and we will try to resolve your issue.");
                    }
                }

                File file = new File("./docs/" + filename);
                try (Writer w = new FileWriter(file); BufferedWriter bw = new BufferedWriter(w)) {
                    bw.write("Megamek Unit AlphaStrike Converter");
                    bw.newLine();
                    bw.write("This file can be regenerated with java -jar MegaMek.jar -asc filename");
                    bw.newLine();
                    bw.write("Element\tType\tSize\tMP\tArmor\tStructure\tS/M/L\tOV\tPoint Cost\tAbilities");
                    bw.newLine();

                    MechSummary[] units = MechSummaryCache.getInstance().getAllMechs();
                    for (MechSummary unit : units) {
                        Entity entity = new MechFileParser(unit.getSourceFile(),
                                unit.getEntryName()).getEntity();

                        AlphaStrikeElement ase = new AlphaStrikeElement(entity);
                        ase.writeCsv(bw);
                    }
                } catch (Exception ex) {
                    LogManager.getLogger().error("", ex);
                }
            }
            System.exit(0);
        }

        private void processUnitExporter() {
            processUnitExporter(false);
        }

        private void processUnitExporter(boolean officialUnitList) {
            String filename;
            if ((getTokenType() == TOK_LITERAL) || officialUnitList) {
                if (officialUnitList) {
                    filename = MechFileParser.FILENAME_OFFICIAL_UNITS;
                } else {
                    filename = getTokenValue();
                }
                nextToken();

                if (!new File("./docs").exists()) {
                    if (!new File("./docs").mkdir()) {
                        LogManager.getLogger().error(
                                "Error in creating directory ./docs. We know this is annoying, and apologise. "
                                        + "Please submit a bug report at https://github.com/MegaMek/megamek/issues "
                                        + " and we will try to resolve your issue.");
                    }
                }
                File file = new File("./docs/" + filename);
                try (Writer w = new FileWriter(file); BufferedWriter bw = new BufferedWriter(w)) {
                    if (officialUnitList) {
                        bw.write("MegaMek Official Unit List");
                        bw.newLine();
                        bw.write("This file can be regenerated with java -jar MegaMek.jar -oul");
                        bw.newLine();
                        bw.write("Format is: Chassis Model|");
                    } else {
                        bw.write("MegaMek Unit Database");
                        bw.newLine();
                        bw.write("This file can be regenerated with java -jar MegaMek.jar -export filename");
                        bw.newLine();
                        bw.write("Type,SubType,Name,Model,BV,Cost (Loaded), Cost (Unloaded),Year,TechLevel,Tonnage,Tech,Canon,Walk,Run,Jump");
                    }
                    bw.newLine();

                    MechSummary[] units = MechSummaryCache.getInstance(officialUnitList).getAllMechs();
                    for (MechSummary unit : units) {
                        String unitType = unit.getUnitType();
                        if (unitType.equalsIgnoreCase("mek")) {
                            unitType = "'Mech";
                        }

                        if (!officialUnitList) {
                            bw.write(unitType);
                            bw.write(",");
                            bw.write(unit.getUnitSubType());
                            bw.write(",");
                            bw.write(unit.getChassis());
                            bw.write(",");
                            bw.write(unit.getModel());
                            bw.write(",");
                            bw.write(Integer.toString(unit.getBV()));
                            bw.write(",");
                            bw.write(Long.toString(unit.getCost()));
                            bw.write(",");
                            bw.write(Long.toString(unit.getUnloadedCost()));
                            bw.write(",");
                            bw.write(Integer.toString(unit.getYear()));
                            bw.write(",");
                            bw.write(TechConstants.getLevelDisplayableName(unit.getType()));
                            bw.write(",");
                            bw.write(Double.toString(unit.getTons()));
                            bw.write(",");
                            if (unit.isClan()) {
                                bw.write("Clan,");
                            } else {
                                bw.write("IS,");
                            }
                            if (unit.isCanon()) {
                                bw.write("Canon,");
                            } else {
                                bw.write("Non-Canon,");
                            }
                            bw.write(Integer.toString(unit.getWalkMp()));
                            bw.write(",");
                            bw.write(Integer.toString(unit.getRunMp()));
                            bw.write(",");
                            bw.write(Integer.toString(unit.getJumpMp()));
                        } else {
                            bw.write(unit.getChassis()
                                    + (unit.getModel().isBlank() ? "|" : " " + unit.getModel() + "|"));
                        }
                        bw.newLine();
                    }
                } catch (Exception ex) {
                    LogManager.getLogger().error("", ex);
                }
            }
            System.exit(0);
        }

        private void processRestOfInput() {
            Vector<String> v = new Vector<>();
            while (getArgValue() != null) {
                v.addElement(getArgValue());
                nextArg();
            }
            setToken(TOK_EOF);
            setTokenValue(null);
            restArgs = v.toArray(new String[0]);
        }
    }

}
