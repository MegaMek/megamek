/*
 * MegaMek - Copyright (C) 2005, 2006 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

import megamek.client.ui.IMegaMekGUI;
import megamek.client.ui.preferences.MMPreferences;
import megamek.client.ui.swing.ButtonOrderPreferences;
import megamek.common.Aero;
import megamek.common.AlphaStrikeElement;
import megamek.common.BattleArmor;
import megamek.common.BattleForceElement;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.MechView;
import megamek.common.Tank;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.common.logging.DefaultMmLogger;
import megamek.common.logging.LogConfig;
import megamek.common.logging.MMLogger;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.AbstractCommandLineParser;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestAero;
import megamek.common.verifier.TestBattleArmor;
import megamek.common.verifier.TestEntity;
import megamek.common.verifier.TestMech;
import megamek.common.verifier.TestSupportVehicle;
import megamek.common.verifier.TestTank;
import megamek.server.DedicatedServer;
import megamek.utils.RATGeneratorEditor;

/**
 * @author mev This is the class where the execution of the megamek game starts.
 */
public class MegaMek {
    private static MMLogger logger = null;
    private static MMPreferences preferences = null;

    public static String VERSION = "0.49.3-SNAPSHOT";
    public static long TIMESTAMP = new File(PreferenceManager.getClientPreferences().getLogDirectory()
            + File.separator + "timestamp").lastModified();

    private static final NumberFormat commafy = NumberFormat.getInstance();
    private static final String INCORRECT_ARGUMENTS_MESSAGE = "Incorrect arguments:";
    private static final String ARGUMENTS_DESCRIPTION_MESSAGE = "Arguments syntax:\n\t MegaMek "
            + "[-log <logfile>] [(-gui <guiname>)|(-dedicated)|(-validate)|(-export)|(-eqdb)|"
            + "(-eqedb) (-oul)] [<args>]";
    private static final String UNKNOWN_GUI_MESSAGE = "Unknown GUI:";
    private static final String GUI_CLASS_NOT_FOUND_MESSAGE = "Couldn't find the GUI Class:";
    public static final String PREFERENCES_FILE = "mmconf/megamek.preferences";
    public static final String DEFAULT_LOG_FILE_NAME = "megameklog.txt";

    public static void main(String[] args) {
        String logFileName = DEFAULT_LOG_FILE_NAME;
        CommandLineParser cp = new CommandLineParser(args);

        try {
            cp.parse();
            String lf = cp.getLogFilename();
            if (lf != null) {
                if (lf.equals("none") || lf.equals("off")) {
                    logFileName = null;
                } else {
                    logFileName = lf;
                }
            }

            configureLogging(logFileName);

            MegaMek.showInfo();

            String[] restArgs = cp.getRestArgs();
            if (cp.dedicatedServer()) {
                MegaMek.startDedicatedServer(restArgs);
            } else {
                getPreferences().loadFromFile(PREFERENCES_FILE);

                if (cp.ratGenEditor()) {
                    RATGeneratorEditor.main(restArgs);
                } else {
                    // Load button ordering
                    ButtonOrderPreferences.getInstance().setButtonPriorities();

                    String interfaceName = cp.getGuiName();
                    if (interfaceName == null) {
                        interfaceName = PreferenceManager.getClientPreferences().getGUIName();
                    }
                    MegaMek.startGUI(interfaceName, restArgs);
                }
            }
        } catch (CommandLineParser.ParseException e) {
            String message = INCORRECT_ARGUMENTS_MESSAGE + e.getMessage() + '\n'
                    + ARGUMENTS_DESCRIPTION_MESSAGE;
            getLogger().fatal(message);
            System.exit(1);
        }
    }

    private static void configureLegacyLogging(@Nullable final String logFileName) {
        // Redirect output to log files, unless turned off.
        if (logFileName == null) {
            return;
        }
        MegaMek.redirectOutput(logFileName);
    }

    private static void configureLog4j(@Nullable final String logFileName) {
        if (null == logFileName) {
            LogConfig.getInstance().disableAll();
            return;
        }
        LogConfig.getInstance().enableSimplifiedLogging();
    }

    /**
     * This needs to be done as we are currently using two different loggers.
     * Both loggers must be set to append in order to prevent them from over-
     * writing each other.  So, in order to get a clean log file each run,
     * the existing log file must be cleared.
     * <p>Alternatively, consider rolling the log file over instead.</p>
     * If we ever manage to completely get rid of the legacy logger, we can
     * get rid of this method.
     *
     * @param logFileName The name of the log file to reset.
     */
    public static void resetLogFile(@Nullable final String logFileName) {
        if (null == logFileName) {
            return;
        }
        File file = new File(logFileName);
        if (file.exists()) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.print("");
            } catch (FileNotFoundException e) {
                getLogger().error(e);
            }
        }
    }

    private static void configureLogging(@Nullable final String logFileName) {
        final String qualifiedLogFilename = PreferenceManager.getClientPreferences().getLogDirectory()
                + File.separator + logFileName;
        resetLogFile(qualifiedLogFilename);
        configureLegacyLogging(logFileName);
        configureLog4j(logFileName);
    }

    /**
     * @param logger The logger to be used.
     */
    public static void setLogger(final MMLogger logger) {
        MegaMek.logger = logger;
    }

    /**
     * @return The logger that will handle log file output. Will return the
     * {@link DefaultMmLogger} if a different logger has not been set.
     */
    public static MMLogger getLogger() {
        if (null == logger) {
            logger = DefaultMmLogger.getInstance();
        }
        return logger;
    }

    public static MMPreferences getPreferences() {
        if (preferences == null) {
            preferences = new MMPreferences();
        }

        return preferences;
    }

    /**
     * Calculates the SHA-256 hash of the MegaMek.jar file
     * Used primarily for purposes of checksum comparison when
     * connecting a new client.
     * @return String representing the SHA-256 hash
     */
    public static String getMegaMekSHA256() {
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
            getLogger().error(e);
            return null;
        }
        try (InputStream is = new FileInputStream(filename);
             InputStream dis = new DigestInputStream(is, md)) {
            while (0 < dis.read(buffer)) { }
            // gets digest
            byte[] digest = md.digest();
            // convert the byte to hex format
            for (byte d : digest) {
                sb.append(String.format("%02x", d));
            }
        } catch (IOException e) {
            getLogger().error(e);
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
        return commafy.format((heap - free) / 1024) + " kB";
    }

    /**
     * This function redirects the standard error and output streams to the
     * given File name.
     *
     * @param logFileName The file name to redirect to.
     */
    private static void redirectOutput(String logFileName) {
        getLogger().info("Redirecting output to " + logFileName);
        String sLogDir = PreferenceManager.getClientPreferences().getLogDirectory();
        File logDir = new File(sLogDir);
        if (!logDir.exists()) {
            if (!logDir.mkdir()) {
                getLogger().error("Error in creating directory ./logs. We know this is annoying, and apologise. "
                                + "Please submit a bug report at https://github.com/MegaMek/megamek/issues "
                                + " and we will try to resolve your issue.");
            }
        }
        try {
            PrintStream ps = new PrintStream(
                    new BufferedOutputStream(new FileOutputStream(sLogDir
                            + File.separator + logFileName, true) {
                        @Override
                        public void flush() throws IOException {
                            super.flush();
                            getFD().sync();
                        }
                    }, 64));
            System.setOut(ps);
            System.setErr(ps);
        } catch (Exception e) {
            getLogger().error("Unable to redirect output to " + logFileName, e);
        }
    }

    /**
     * Starts a dedicated server with the arguments in args. See
     * {@link megamek.server.DedicatedServer#start(String[])} for more
     * information.
     *
     * @param args the arguments to the dedicated server.
     */
    private static void startDedicatedServer(String[] args) {
        StringBuffer message = new StringBuffer("Starting Dedicated Server. ");
        MegaMek.dumpArgs(message, args);
        getLogger().info(message.toString());
        DedicatedServer.start(args);
    }

    /**
     * Attempts to start the GUI with the given name. If the GUI is unknown the
     * program will exit.
     *
     * @param guiName The name of the GUI, usually AWT or swing
     * @param args    The arguments to be passed onto the GUI.
     */
    private static void startGUI(String guiName, String[] args) {
        if (null == guiName) {
            getLogger().error("guiName must be non-null");
            return;
        }
        if (null == args) {
            getLogger().error("args must be non-null");
            return;
        }
        IMegaMekGUI mainGui = MegaMek.getGui(guiName);
        if (mainGui == null) {
            getLogger().fatal(UNKNOWN_GUI_MESSAGE + guiName);
            System.exit(1);
        } else {
            StringBuffer message = new StringBuffer("Starting GUI ");
            message.append(guiName).append(". ");
            MegaMek.dumpArgs(message, args);
            getLogger().info(message.toString());
            mainGui.start(args);
        }
    }

    /**
     * Return the Interface to the GUI specified by the name in guiName.
     *
     * @param guiName the name of the GUI, will be passed on to
     *                {@link #getGUIClassName(String)}.
     * @return An that can start a GUI such as {@link IMegaMekGUI}.
     */
    @SuppressWarnings({ "rawtypes" })
    private static IMegaMekGUI getGui(String guiName) {
        assert (guiName != null) : "guiName must be non-null";
        String guiClassName = MegaMek.getGUIClassName(guiName);
        if (guiClassName != null) {
            try {
                Class guiClass = Class.forName(guiClassName);
                if (IMegaMekGUI.class.isAssignableFrom(guiClass)) {
                    return (IMegaMekGUI) guiClass.newInstance();
                }
            } catch (Exception e) {
                getLogger().info(GUI_CLASS_NOT_FOUND_MESSAGE + guiClassName);
            }
        }
        return null;
    }

    private static String getGUIClassName(String guiName) {
        assert (guiName != null) : "guiName must be non-null";
        Properties p = new Properties();
        String key = "gui." + guiName;
        final String PROPERTIES_FILE = "megamek/MegaMek.properties";
        try (InputStream is = MegaMek.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (is != null) {
                p.load(is);
                return p.getProperty(key);
            }
        } catch (IOException e) {
            getLogger().info("Property file load failed.");
        }
        return null;
    }

    /**
     * This function appends 'args: []', to the buffer, with a space separated
     * list of args[] elements between the brackets.
     *
     * @param buffer the buffer to append the list to.
     * @param args   the array of strings to copy into a space separated list.
     */
    private static void dumpArgs(StringBuffer buffer, String[] args) {
        assert (buffer != null) : "buffer must be non-null";
        assert (args != null) : "args must be non-null";
        buffer.append("args: [");
        for (int i = 0, e = args.length; i < e; i++) {
            if (i != 0) {
                buffer.append(' ');
            }
            buffer.append(args[i]);
        }
        buffer.append("]");
    }

    /**
     * Prints some information about MegaMek. Used in log files to figure out the JVM and
     * version of MegaMek.
     */
    private static void showInfo() {
        // echo some useful stuff
        String msg = "Starting MegaMek v" + VERSION + " ..." + "\n\tCompiled on " +
                new Date(TIMESTAMP).toString() + "\n\tToday is " + LocalDate.now().toString() +
                "\n\tJava vendor " + System.getProperty("java.vendor") + "\n\tJava version "
                + System.getProperty("java.version") + "\n\tPlatform " + System.getProperty("os.name")
                + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")"
                + "\n\tTotal memory available to MegaMek: "
                + MegaMek.commafy.format(Runtime.getRuntime().maxMemory() / 1024) + " kB";
        getLogger().info(msg);
    }

    /**
     * Returns the version of Megamek
     *
     * @return the version of Megamek as a string.
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     * This class parses the options passed into to MegaMek from the command line.
     */
    private static class CommandLineParser extends AbstractCommandLineParser {
        private String logFilename;
        private String guiName;
        private boolean dedicatedServer = false;
        private boolean ratGenEditor = false;
        private String[] restArgs = new String[0];

        // Options
        private static final String OPTION_DEDICATED = "dedicated";
        private static final String OPTION_GUI = "gui";
        private static final String OPTION_LOG = "log";
        private static final String OPTION_EQUIPMENT_DB = "eqdb";
        private static final String OPTION_EQUIPMENT_EXTENDED_DB = "eqedb";
        private static final String OPTION_UNIT_VALIDATOR = "validate";
        private static final String OPTION_UNIT_EXPORT = "export";
        private static final String OPTION_OFFICAL_UNIT_LIST = "oul";
        private static final String OPTION_UNIT_BATTLEFORCE_CONVERSION = "bfc";
        private static final String OPTION_UNIT_ALPHASTRIKE_CONVERSION = "asc";
        private static final String OPTION_DATADIR = "data";
        private static final String OPTION_RATGEN_EDIT = "editratgen";

        CommandLineParser(String[] args) {
            super(args);
        }

        /**
         * Returns <code>true</code> if the dedicated server option was found
         *
         * @return true iff this is a dedicated server.
         */
        boolean dedicatedServer() {
            return dedicatedServer;
        }

        /**
         * Flag that indicates the option for the RAT Generator editor
         * @return Whether the RAT Generator editor should be invoked
         */
        boolean ratGenEditor() {
            return ratGenEditor;
        }

        /**
         * Returns the GUI Name option value or <code>null</code> if it wasn't set
         *
         * @return GUI Name option value or <code>null</code> if it wasn't set
         */
        String getGuiName() {
            return guiName;
        }

        /**
         * Returns the log file name option value or <code>null</code> if it wasn't set
         *
         * @return the log file name option value or <code>null</code> if it wasn't set
         */
        String getLogFilename() {
            return logFilename;
        }

        /**
         * Returns the the <code>array</code> of the unprocessed arguments
         *
         * @return the the <code>array</code> of the unprocessed arguments
         */
        String[] getRestArgs() {
            return restArgs;
        }

        @Override
        protected void start() throws ParseException {
            if (getToken() == TOK_OPTION) {
                final String tokenVal = getTokenValue();
                nextToken();
                switch (tokenVal) {
                    case OPTION_LOG:
                        parseLog();
                        break;
                    case OPTION_EQUIPMENT_DB:
                        processEquipmentDb();
                        break;
                    case OPTION_EQUIPMENT_EXTENDED_DB:
                        processExtendedEquipmentDb();
                        break;
                    case OPTION_DATADIR:
                        processDataDir();
                        break;
                    case OPTION_UNIT_VALIDATOR:
                        processUnitValidator();
                        break;
                    case OPTION_UNIT_EXPORT:
                        processUnitExporter();
                        break;
                    case OPTION_OFFICAL_UNIT_LIST:
                        processUnitExporter(true);
                        break;
                    case OPTION_UNIT_BATTLEFORCE_CONVERSION:
                        processUnitBattleForceConverter();
                        break;
                    case OPTION_UNIT_ALPHASTRIKE_CONVERSION:
                        processUnitAlphaStrikeConverter();
                        break;
                    case OPTION_DEDICATED:
                        dedicatedServer = true;
                        break;
                    case OPTION_GUI:
                        parseGUI();
                        break;
                    case OPTION_RATGEN_EDIT:
                        ratGenEditor = true;
                        break;
                }
            }
            processRestOfInput();
            if (getToken() != TOK_EOF) {
                throw new ParseException("unexpected input");
            }
        }

        private void parseLog() throws ParseException {
            if (getToken() == TOK_LITERAL) {
                logFilename = getTokenValue();
                nextToken();
            } else {
                throw new ParseException("log file name expected");
            }
        }

        private void parseGUI() throws ParseException {
            if (getToken() == TOK_LITERAL) {
                guiName = getTokenValue();
                nextToken();
            } else {
                throw new ParseException("GUI name expected");
            }
        }

        private void processEquipmentDb() throws ParseException {
            String filename;
            if (getToken() == TOK_LITERAL) {
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
            if (getToken() == TOK_LITERAL) {
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
            if (getToken() == TOK_LITERAL) {
                dataDirName = getTokenValue();
                nextToken();
                Configuration.setDataDir(new File(dataDirName));
            } else {
                throw new ParseException("directory name expected");
            }
        }

        private void processUnitValidator() throws ParseException {
            String filename;
            if (getToken() == TOK_LITERAL) {
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
                    getLogger().error(new IOException(filename + " not found.  Try using \"chassis model\" for input."));
                } else {
                    try {
                        Entity entity = new MechFileParser(ms.getSourceFile(),
                                ms.getEntryName()).getEntity();
                        getLogger().info("Validating Entity: " + entity.getShortNameRaw());
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
                            } else if (entity instanceof BattleArmor){
                                testEntity = new TestBattleArmor((BattleArmor) entity,
                                        entityVerifier.baOption, null);
                            }

                            if (testEntity != null) {
                                testEntity.correctEntity(sb);
                            }
                        }
                        getLogger().info(sb.toString());
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
            if (getToken() == TOK_LITERAL) {
                filename = getTokenValue();
                nextToken();

                if (!new File("./docs").exists()) {
                    if (!new File("./docs").mkdir()) {
                        getLogger().error(
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
                    getLogger().error(e);
                }
            }

            System.exit(0);
        }

        private void processUnitAlphaStrikeConverter() {
            String filename;
            if (getToken() == TOK_LITERAL) {
                filename = getTokenValue();
                nextToken();

                if (!new File("./docs").exists()) {
                    if (!new File("./docs").mkdir()) {
                        getLogger().error(
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
                    getLogger().error(ex);
                }
            }
            System.exit(0);
        }

        private void processUnitExporter() {
            processUnitExporter(false);
        }

        private void processUnitExporter(boolean officialUnitList) {
            String filename;
            if ((getToken() == TOK_LITERAL) || officialUnitList) {
                if (officialUnitList) {
                    filename = MechFileParser.FILENAME_OFFICIAL_UNITS;
                } else {
                    filename = getTokenValue();
                }
                nextToken();

                if (!new File("./docs").exists()) {
                    if (!new File("./docs").mkdir()) {
                        getLogger().error(
                                "Error in creating directory ./docs. We know this is annoying, and apologise. "
                                        + "Please submit a bug report at https://github.com/MegaMek/megamek/issues "
                                        + " and we will try to resolve your issue.");
                    }
                }
                File file = new File("./docs/" + filename);
                try (Writer w = new FileWriter(file); BufferedWriter bw = new BufferedWriter(w)) {
                    if (officialUnitList) {
                        bw.write("Megamek Official Unit List");
                        bw.newLine();
                        bw.write("This file can be regenerated with java -jar MegaMek.jar -oul");
                        bw.newLine();
                        bw.write("Format is: Chassis Model|");
                    } else {
                        bw.write("Megamek Unit Database");
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
                                    + (unit.getModel().equals("") ? "|" : " "
                                    + unit.getModel() + "|"));
                        }
                        bw.newLine();
                    }
                } catch (Exception ex) {
                    getLogger().error(ex);
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
