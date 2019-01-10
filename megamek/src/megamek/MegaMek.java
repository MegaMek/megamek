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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

import megamek.client.TimerSingleton;
import megamek.client.ui.IMegaMekGUI;
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
import megamek.common.logging.LogLevel;
import megamek.common.logging.MMLogger;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.AbstractCommandLineParser;
import megamek.common.util.MegaMekFile;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestAero;
import megamek.common.verifier.TestBattleArmor;
import megamek.common.verifier.TestEntity;
import megamek.common.verifier.TestMech;
import megamek.common.verifier.TestSupportVehicle;
import megamek.common.verifier.TestTank;
import megamek.server.DedicatedServer;

/**
 * @author mev This is the class where the execution of the megamek game starts.
 */
public class MegaMek {

    private static final MMLogger logger = DefaultMmLogger.getInstance();

    public static String VERSION = "0.45.3-SNAPSHOT"; //$NON-NLS-1$
    public static long TIMESTAMP = new File(PreferenceManager
            .getClientPreferences().getLogDirectory()
            + File.separator
            + "timestamp").lastModified(); //$NON-NLS-1$

    private static final NumberFormat commafy = NumberFormat.getInstance();
    private static final String INCORRECT_ARGUMENTS_MESSAGE = "Incorrect arguments:"; //$NON-NLS-1$
    private static final String ARGUMENTS_DESCRIPTION_MESSAGE = "Arguments syntax:\n\t MegaMek [-log <logfile>] [(-gui <guiname>)|(-dedicated)|(-validate)|(-export)|(-eqdb)|(-eqedb) (-oul)] [<args>]"; //$NON-NLS-1$
    private static final String UNKNOWN_GUI_MESSAGE = "Unknown GUI:"; //$NON-NLS-1$
    private static final String GUI_CLASS_NOT_FOUND_MESSAGE = "Couldn't find the GUI Class:"; //$NON-NLS-1$
    private static final String DEFAULT_LOG_FILE_NAME = "megameklog.txt"; //$NON-NLS-1$

    public static void main(String[] args) {

        String logFileName = DEFAULT_LOG_FILE_NAME;

        CommandLineParser cp = new CommandLineParser(args);

        try {
            cp.parse();
            String lf = cp.getLogFilename();
            if (lf != null) {
                if (lf.equals("none") || lf.equals("off")) { //$NON-NLS-1$ //$NON-NLS-2$
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
                // Load button ordering
                ButtonOrderPreferences.getInstance().setButtonPriorities();

                String interfaceName = cp.getGuiName();
                if (interfaceName == null) {
                    interfaceName = PreferenceManager.getClientPreferences()
                            .getGUIName();
                }
                MegaMek.startGUI(interfaceName, restArgs);
            }

        } catch (CommandLineParser.ParseException e) {
            StringBuilder message = new StringBuilder(INCORRECT_ARGUMENTS_MESSAGE)
                    .append(e.getMessage()).append('\n');
            message.append(ARGUMENTS_DESCRIPTION_MESSAGE);
            MegaMek.displayMessageAndExit(message.toString(),
                                          "main(String[])");
        }
    }

    private static void configureLegacyLogging(@Nullable final String logFileName) {
        // Redirect output to logfiles, unless turned off.
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
     * <p>
     * Alternatively, consider rolling the log file over instead.
     * <p>
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
            try {
                PrintWriter writer = new PrintWriter(file);
                writer.print("");
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private static void configureLogging(@Nullable final String logFileName) {
        final String qualifiedLogFilename =
                PreferenceManager.getClientPreferences().getLogDirectory() +
                File.separator +
                logFileName;
        resetLogFile(qualifiedLogFilename);
        configureLegacyLogging(logFileName);
        configureLog4j(logFileName);
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
        DigestInputStream in = null;

        // Assume UNIX/Linux, which has the jar in the root folder
        String filename = "MegaMek.jar";
        // If it isn't UNIX/Linux, maybe it's Windows where we've stashed it in the lib folder
        if (new File("lib/"+filename).exists()) {
            filename = "lib/"+filename;
        // And if it isn't either UNIX/Linux or Windows it's got to be Mac, where it's buried inside the app
        } else if (new File("MegaMek.app/Contents/Resources/Java/"+filename).exists()) {
            filename = "MegaMek.app/Contents/Resources/Java/"+filename;
        }

        // Calculate the digest for the given file.
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            in = new DigestInputStream(new FileInputStream(filename), md);
            while (0 < in.read(buffer)) {}
            // gets digest
            byte[] digest = md.digest();
            // convert the byte to hex format
            for (byte d : digest) {
                sb.append(String.format("%02x", d));
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            logger.error(MegaMek.class,
                       "getMegaMekSHA256()",
                       e);
            return null;
        } finally {
            try {
                if (null != in) {
                    in.close();
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                logger.error(MegaMek.class,
                           "getMegaMekSHA256()",
                           e);
                return null;
            }
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
        long used = (heap - free) / 1024;
        return commafy.format(used) + " kB"; //$NON-NLS-1$
    }

    /**
     * This function redirects the standard error and output streams to the
     * given File name.
     *
     * @param logFileName
     *            The file name to redirect to.
     */
    private static void redirectOutput(String logFileName) {
        try {
            System.out.println("Redirecting output to " + logFileName); //$NON-NLS-1$
            String sLogDir = PreferenceManager.getClientPreferences()
                    .getLogDirectory();
            File logDir = new File(sLogDir);
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            PrintStream ps = new PrintStream(
                    new BufferedOutputStream(new FileOutputStream(sLogDir
                                                                  + File.separator + logFileName, true) {
                        @Override
                        public void flush() throws IOException {
                            super.flush();
                            getFD().sync();
                        };
                    }
                            , 64));
            System.setOut(ps);
            System.setErr(ps);
        } catch (Exception e) {
            System.err.println("Unable to redirect output to " + logFileName); //$NON-NLS-1$
            e.printStackTrace();
        }
    }

    /**
     * Starts a dedicated server with the arguments in args. See
     * {@link megamek.server.DedicatedServer#start(String[])} for more
     * information.
     *
     * @param args
     *            the arguments to the dedicated server.
     */
    private static void startDedicatedServer(String[] args) {
        StringBuffer message = new StringBuffer("Starting Dedicated Server. "); //$NON-NLS-1$
        MegaMek.dumpArgs(message, args);
        MegaMek.displayMessage(message.toString(),
                               "startDedicatedServer(String[])");
        DedicatedServer.start(args);
    }

    /**
     * Attempts to start the GUI with the given name. If the GUI is unknown the
     * program will exit.
     *
     * @param guiName
     *            The name of the GUI, usually AWT or swing
     * @param args
     *            the arguments to be passed onto the GUI.
     */
    private static void startGUI(String guiName, String[] args) {
        final String METHOD_NAME = "startGUI(String, String[])";
        if (null == guiName) {
            logger.log(MegaMek.class, METHOD_NAME, LogLevel.ERROR,
                       "guiName must be non-null");
            return;
        }
        if (null == args) {
            logger.log(MegaMek.class, METHOD_NAME, LogLevel.ERROR,
                       "args must be non-null");
            return;
        }
        IMegaMekGUI mainGui = MegaMek.getGui(guiName);
        if (mainGui == null) {
            MegaMek.displayMessageAndExit(UNKNOWN_GUI_MESSAGE + guiName,
                                          METHOD_NAME);
        } else {
            StringBuffer message = new StringBuffer("Starting GUI "); //$NON-NLS-1$
            message.append(guiName).append(". "); //$NON-NLS-1$
            MegaMek.dumpArgs(message, args);
            MegaMek.displayMessage(message.toString(),
                                   METHOD_NAME);
            mainGui.start(args);
        }
    }

    /**
     * Return the Interface to the GUI specified by the name in guiName.
     *
     * @param guiName
     *            the name of the GUI, will be passed on to
     *            {@link #getGUIClassName(String)}.
     * @return An that can start a GUI such as
     *         {@link IMegaMekGUI}.
     */
    @SuppressWarnings({ "rawtypes" })
    private static IMegaMekGUI getGui(String guiName) {
        assert (guiName != null) : "guiName must be non-null"; //$NON-NLS-1$
        String guiClassName = MegaMek.getGUIClassName(guiName);
        if (guiClassName != null) {
            try {
                Class guiClass = Class.forName(guiClassName);
                if (IMegaMekGUI.class.isAssignableFrom(guiClass)) {
                    IMegaMekGUI result = (IMegaMekGUI) guiClass.newInstance();
                    return result;
                }
            } catch (Exception e) {
                MegaMek.displayMessage(GUI_CLASS_NOT_FOUND_MESSAGE
                                       + guiClassName, "getGui(String)");
            }
        }
        return null;
    }

    private static String getGUIClassName(String guiName) {
        assert (guiName != null) : "guiName must be non-null"; //$NON-NLS-1$
        Properties p = new Properties();
        String key = "gui." + guiName; //$NON-NLS-1$
        final String PROPERTIES_FILE = "megamek/MegaMek.properties";
        try(InputStream is = MegaMek.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (is != null) {
                p.load(is);
                return p.getProperty(key);
            }
        } catch (IOException e) {
            MegaMek.displayMessage("Property file load failed.",
                                   "getGUIClassName(String)"); //$NON-NLS-1$
        }
        return null;
    }

    /**
     * This function appends 'agrs: []', to the buffer, with a space separated
     * list of args[] elements between the brackets.
     *
     * @param buffer
     *            the buffer to append the list to.
     * @param args
     *            the array of strings to copy into a space seperated list.
     */
    private static void dumpArgs(StringBuffer buffer, String[] args) {
        assert (buffer != null) : "buffer must be non-null"; //$NON-NLS-1$
        assert (args != null) : "args must be non-null"; //$NON-NLS-1$
        buffer.append("args: ["); //$NON-NLS-1$
        for (int i = 0, e = args.length; i < e; i++) {
            if (i != 0) {
                buffer.append(' ');
            }
            buffer.append(args[i]);
        }
        buffer.append("]"); //$NON-NLS-1$
    }

    /**
     * Prints the message to stdout and then exits with errorcode 1.
     *
     * @param message
     *            the message to be displayed.
     */
    private static void displayMessageAndExit(String message,
                                              String methodName) {
        MegaMek.displayMessage(message, methodName);
        TimerSingleton.getInstance().killTimer();
        System.exit(1);
    }

    /**
     * Prints the message and flushes the output stream.
     *
     * @param message
     */
    private static void displayMessage(String message, String methodName) {
        logger.log(MegaMek.class, methodName, LogLevel.INFO, message);
    }

    /**
     * Prints some information about MegaMek. Used in logfiles to figure out the
     * JVM and version of MegaMek.
     */
    private static void showInfo() {
        final String METHOD_NAME = "showInfo";
        // echo some useful stuff
        String msg = "Starting MegaMek v" + VERSION + " ..."; //$NON-NLS-1$ //$NON-NLS-2$
        msg += "\n\tCompiled on " + new Date(TIMESTAMP).toString(); //$NON-NLS-1$
        msg += "\n\tToday is " + new Date().toString(); //$NON-NLS-1$
        msg += "\n\tJava vendor " + System.getProperty("java.vendor"); //$NON-NLS-1$ //$NON-NLS-2$
        msg += "\n\tJava version " + System.getProperty("java.version"); //$NON-NLS-1$ //$NON-NLS-2$
        msg += "\n\tPlatform " //$NON-NLS-1$
               + System.getProperty("os.name") //$NON-NLS-1$
               + " " //$NON-NLS-1$
               + System.getProperty("os.version") //$NON-NLS-1$
               + " (" //$NON-NLS-1$
               + System.getProperty("os.arch") //$NON-NLS-1$
               + ")"; //$NON-NLS-1$
        long maxMemory = Runtime.getRuntime().maxMemory() / 1024;
        msg += "\n\tTotal memory available to MegaMek: " + MegaMek.commafy.format(maxMemory) + " kB"; //$NON-NLS-1$ //$NON-NLS-2$
        displayMessage(msg, METHOD_NAME);
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
     * This class parses the options passed into to magamek from the command
     * line.
     */
    private static class CommandLineParser extends AbstractCommandLineParser {

        private String logFilename;
        private String guiName;
        private boolean dedicatedServer = false;
        private String[] restArgs = new String[0];

        // Options
        private static final String OPTION_DEDICATED = "dedicated"; //$NON-NLS-1$
        private static final String OPTION_GUI = "gui"; //$NON-NLS-1$
        private static final String OPTION_LOG = "log"; //$NON-NLS-1$
        private static final String OPTION_EQUIPMENT_DB = "eqdb"; //$NON-NLS-1$
        private static final String OPTION_EQUIPMENT_EXTENDED_DB = "eqedb"; //$NON-NLS-1$
        private static final String OPTION_UNIT_VALIDATOR = "validate"; //$NON-NLS-1$
        private static final String OPTION_UNIT_EXPORT = "export"; //$NON-NLS-1$
        private static final String OPTION_OFFICAL_UNIT_LIST = "oul"; //$NON-NLS-1$
        private static final String OPTION_UNIT_BATTLEFORCE_CONVERSION = "bfc"; //$NON-NLS-1$
        private static final String OPTION_UNIT_ALPHASTRIKE_CONVERSION = "asc"; //$NON-NLS-1$
        private static final String OPTION_DATADIR = "data"; //$NON-NLS-1$

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
         * Returns the GUI Name option value or <code>null</code> if it wasn't
         * set
         *
         * @return GUI Name option value or <code>null</code> if it wasn't set
         */
        String getGuiName() {
            return guiName;
        }

        /**
         * Returns the log file name option value or <code>null</code> if it
         * wasn't set
         *
         * @return the log file name option value or <code>null</code> if it
         *         wasn't set
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

            if ((getToken() == TOK_OPTION)
                    && getTokenValue().equals(OPTION_LOG)) {
                nextToken();
                parseLog();
            }
            if ((getToken() == TOK_OPTION)
                    && getTokenValue().equals(OPTION_EQUIPMENT_DB)) {
                nextToken();
                processEquipmentDb();
            }

            if ((getToken() == TOK_OPTION)
                    && getTokenValue().equals(OPTION_EQUIPMENT_EXTENDED_DB)) {
                nextToken();
                processExtendedEquipmentDb();
            }
            if ((getToken() == TOK_OPTION)
                    && getTokenValue().equals(OPTION_DATADIR)) {
                nextToken();
                processDataDir();
            }
            if ((getToken() == TOK_OPTION)
                    && getTokenValue().equals(OPTION_UNIT_VALIDATOR)) {
                nextToken();
                processUnitValidator();
            }

            if ((getToken() == TOK_OPTION)
                    && getTokenValue().equals(OPTION_UNIT_EXPORT)) {
                nextToken();
                processUnitExporter();
            }

            if ((getToken() == TOK_OPTION)
                    && getTokenValue().equals(OPTION_OFFICAL_UNIT_LIST)) {
                nextToken();
                processUnitExporter(true);
            }

            if ((getToken() == TOK_OPTION)
                    && getTokenValue().equals(
                            OPTION_UNIT_BATTLEFORCE_CONVERSION)) {
                nextToken();
                processUnitBattleForceConverter();
            }

            if ((getToken() == TOK_OPTION)
                    && getTokenValue().equals(
                            OPTION_UNIT_ALPHASTRIKE_CONVERSION)) {
                nextToken();
                processUnitAlphaStrikeConverter();
            }

            if ((getToken() == TOK_OPTION)
                    && getTokenValue().equals(OPTION_DEDICATED)) {
                nextToken();
                dedicatedServer = true;
            } else if ((getToken() == TOK_OPTION)
                    && getTokenValue().equals(OPTION_GUI)) {
                nextToken();
                parseGUI();
            }
            processRestOfInput();
            if (getToken() != TOK_EOF) {
                error("unexpected input"); //$NON-NLS-1$
            }
        }

        private void parseLog() throws ParseException {
            if (getToken() == TOK_LITERAL) {
                logFilename = getTokenValue();
                nextToken();
            } else {
                error("log file name expected"); //$NON-NLS-1$
            }
        }

        private void parseGUI() throws ParseException {
            if (getToken() == TOK_LITERAL) {
                guiName = getTokenValue();
                nextToken();
            } else {
                error("GUI name expected"); //$NON-NLS-1$
            }
        }

        private void processEquipmentDb() throws ParseException {
            String filename;
            if (getToken() == TOK_LITERAL) {
                filename = getTokenValue();
                nextToken();
                megamek.common.EquipmentType.writeEquipmentDatabase(new File(
                        filename));
            } else {
                error("file name expected"); //$NON-NLS-1$
            }
            System.exit(0);
        }

        private void processExtendedEquipmentDb() throws ParseException {
            String filename;
            if (getToken() == TOK_LITERAL) {
                filename = getTokenValue();
                nextToken();
                megamek.common.EquipmentType
                        .writeEquipmentExtendedDatabase(new File(filename));
            } else {
                error("file name expected"); //$NON-NLS-1$
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
                error("directory name expected"); // $NON-NLS-1$
            }
        }

        private void processUnitValidator() throws ParseException {
            final String METHOD_NAME = "processUnitValidator()";

            String filename;
            if (getToken() == TOK_LITERAL) {
                filename = getTokenValue();
                nextToken();
                MechSummary ms = MechSummaryCache.getInstance().getMech(
                        filename);
                if (ms == null) {
                    MechSummary[] units = MechSummaryCache.getInstance()
                                                          .getAllMechs();
                    // System.err.println("units: "+units.length);
                    for (MechSummary unit : units) {
                        // System.err.println(unit.getSourceFile().getName());
                        if (unit.getSourceFile().getName()
                                .equalsIgnoreCase(filename)) {
                            ms = unit;
                            break;
                        }
                    }
                }

                if (ms == null) {
                    logger.error(MegaMek.class, METHOD_NAME,
                               new IOException(filename + " not found.  Try using \"cassis model\" for input."));
                } else {
                    try {
                        Entity entity = new MechFileParser(ms.getSourceFile(),
                                                           ms.getEntryName()).getEntity();
                        displayMessage("Validating Entity: " +
                                       entity.getShortNameRaw(), METHOD_NAME); //$NON-NLS-1$
                        EntityVerifier entityVerifier = EntityVerifier.getInstance(
                                new MegaMekFile(Configuration.unitsDir(),
                                                EntityVerifier.CONFIG_FILENAME).getFile());
                        MechView mechView = new MechView(entity, false);
                        StringBuffer sb = new StringBuffer(
                                mechView.getMechReadout());
                        if ((entity instanceof Mech)
                            || (entity instanceof Tank)
                            || (entity instanceof Aero)
                            || (entity instanceof BattleArmor)) {
                            TestEntity testEntity = null;
                            if (entity instanceof Mech) {
                                testEntity = new TestMech((Mech) entity,
                                                          entityVerifier.mechOption, null);
                            }
                            if ((entity instanceof Tank)
                                && !(entity instanceof GunEmplacement)) {
                                if (entity.isSupportVehicle()) {
                                    testEntity = new TestSupportVehicle(
                                            (Tank) entity,
                                            entityVerifier.tankOption, null);
                                } else {
                                    testEntity = new TestTank((Tank) entity,
                                                              entityVerifier.tankOption, null);
                                }
                            }
                            if ((entity.getEntityType() == Entity.ETYPE_AERO)
                                && (entity.getEntityType() !=
                                    Entity.ETYPE_DROPSHIP)
                                && (entity.getEntityType() !=
                                    Entity.ETYPE_SMALL_CRAFT)
                                && (entity.getEntityType() !=
                                    Entity.ETYPE_FIGHTER_SQUADRON)
                                && (entity.getEntityType() !=
                                    Entity.ETYPE_JUMPSHIP)
                                && (entity.getEntityType() !=
                                    Entity.ETYPE_SPACE_STATION)) {
                                testEntity = new TestAero((Aero)entity,
                                                          entityVerifier.aeroOption, null);
                            }
                            if (entity instanceof BattleArmor){
                                testEntity = new TestBattleArmor(
                                        (BattleArmor) entity,
                                        entityVerifier.baOption, null);
                            }

                            if (testEntity != null) {
                                testEntity.correctEntity(sb);
                            }
                        }
                        displayMessage(sb.toString(), METHOD_NAME);
                    } catch (Exception ex) {
                        // ex.printStackTrace();
                        error("\"chassis model\" expected as input"); //$NON-NLS-1$
                    }
                }

            } else {
                error("\"chassis model\" expected as input"); //$NON-NLS-1$
            }
            System.exit(0);
        }

        private void processUnitBattleForceConverter() {

            String filename;
            if (getToken() == TOK_LITERAL) {
                filename = getTokenValue();
                nextToken();

                if (!new File("./docs").exists()) {
                    new File("./docs").mkdir();
                }

                try {
                    File file = new File("./docs/" + filename);
                    BufferedWriter w = new BufferedWriter(new FileWriter(file));
                    w.write("Megamek Unit BattleForce Converter");
                    w.newLine();
                    w.write("This file can be regenerated with java -jar MegaMek.jar -bfc filename");
                    w.newLine();
                    w.write("Element\tSize\tMP\tArmor\tStructure\tS/M/L\tOV\tPoint Cost\tAbilites");
                    w.newLine();

                    MechSummary[] units = MechSummaryCache.getInstance()
                            .getAllMechs();
                    for (MechSummary unit : units) {
                        Entity entity = new MechFileParser(
                                unit.getSourceFile(), unit.getEntryName())
                                .getEntity();
                        
                        BattleForceElement bfe = new BattleForceElement(entity);
                        bfe.writeCsv(w);
                    }
                    w.close();
                } catch (Exception ex) {
                    logger.error(getClass(),
                               "processUnitBattleForceConverter()",
                               ex);
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
                    new File("./docs").mkdir();
                }

                try {
                    File file = new File("./docs/" + filename);
                    BufferedWriter w = new BufferedWriter(new FileWriter(file));
                    w.write("Megamek Unit AlphaStrike Converter");
                    w.newLine();
                    w.write("This file can be regenerated with java -jar MegaMek.jar -asc filename");
                    w.newLine();
                    w.write("Element\tType\tSize\tMP\tArmor\tStructure\tS/M/L\tOV\tPoint Cost\tAbilites");
                    w.newLine();

                    MechSummary[] units = MechSummaryCache.getInstance()
                            .getAllMechs();
                    for (MechSummary unit : units) {
                        Entity entity = new MechFileParser(
                                unit.getSourceFile(), unit.getEntryName())
                                .getEntity();
                        
                        AlphaStrikeElement ase = new AlphaStrikeElement(entity);
                        ase.writeCsv(w);
                   }
                    w.close();
                } catch (Exception ex) {
                    logger.error(getClass(),
                               "processUnitAlphaStrikeConverter()",
                               ex);
                }
            }

            System.exit(0);

        }

        @SuppressWarnings("nls")
        private void processUnitExporter() {
            processUnitExporter(false);
        }

        @SuppressWarnings("nls")
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
                    new File("./docs").mkdir();
                }

                try {
                    File file = new File("./docs/" + filename);
                    BufferedWriter w = new BufferedWriter(new FileWriter(file));
                    if (officialUnitList) {
                        w.write("Megamek Official Unit List");
                        w.newLine();
                        w.write("This file can be regenerated with java -jar MegaMek.jar -oul");
                        w.newLine();
                        w.write("Format is: Chassis Model|");
                        w.newLine();
                    } else {
                        w.write("Megamek Unit Database");
                        w.newLine();
                        w.write("This file can be regenerated with java -jar MegaMek.jar -export filename");
                        w.newLine();
                        w.write("Type,SubType,Name,Model,BV,Cost (Loaded), Cost (Unloaded),Year,Techlevel,Tonnage,Tech,Canon,Walk,Run,Jump");
                        w.newLine();
                    }

                    MechSummary[] units = MechSummaryCache.getInstance(
                            officialUnitList).getAllMechs();
                    for (MechSummary unit : units) {
                        String unitType = unit.getUnitType();
                        if (unitType.equalsIgnoreCase("mek")) {
                            unitType = "'Mech";
                        }

                        if (!officialUnitList) {
                            w.write(unitType);
                            w.write(",");
                            w.write(unit.getUnitSubType());
                            w.write(",");
                            w.write(unit.getChassis());
                            w.write(",");
                            w.write(unit.getModel());
                            w.write(",");
                            w.write(Integer.toString(unit.getBV()));
                            w.write(",");
                            w.write(Long.toString(unit.getCost()));
                            w.write(",");
                            w.write(Long.toString(unit.getUnloadedCost()));
                            w.write(",");
                            w.write(Integer.toString(unit.getYear()));
                            w.write(",");
                            w.write(TechConstants.getLevelDisplayableName(unit
                                    .getType()));
                            w.write(",");
                            w.write(Double.toString(unit.getTons()));
                            w.write(",");
                            if (unit.isClan()) {
                                w.write("Clan,");
                            } else {
                                w.write("IS,");
                            }
                            if (unit.isCanon()) {
                                w.write("Canon,");
                            } else {
                                w.write("Non-Canon,");
                            }
                            w.write(Integer.toString(unit.getWalkMp()));
                            w.write(",");
                            w.write(Integer.toString(unit.getRunMp()));
                            w.write(",");
                            w.write(Integer.toString(unit.getJumpMp()));
                        } else {
                            w.write(unit.getChassis()
                                    + (unit.getModel().equals("") ? "|" : " "
                                            + unit.getModel() + "|"));
                        }
                        w.newLine();
                    }
                    w.close();
                } catch (Exception ex) {
                    logger.error(getClass(),
                               "processUnitExporter(boolean)",
                               ex);
                }
            }

            System.exit(0);

        }

        private void processRestOfInput() {
            Vector<String> v = new Vector<String>();
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
