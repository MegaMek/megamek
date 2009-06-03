/*
 * MegaMek - Copyright (C) 2005, 2006 Ben Mazur (bmazur@sev.org)
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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

import megamek.client.ui.IMegaMekGUI;
import megamek.client.ui.MechView;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Tank;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.AbstractCommandLineParser;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestEntity;
import megamek.common.verifier.TestMech;
import megamek.common.verifier.TestTank;
import megamek.server.DedicatedServer;

/**
 * @author mev This is the class where the execution of the megamek game starts.
 */
public class MegaMek {

    public static String VERSION = "0.34.3"; //$NON-NLS-1$
    public static long TIMESTAMP = new File(PreferenceManager
            .getClientPreferences().getLogDirectory()
            + File.separator + "timestamp").lastModified(); //$NON-NLS-1$

    private static final NumberFormat commafy = NumberFormat.getInstance();
    private static final String INCORRECT_ARGUMENTS_MESSAGE = "Incorrect arguments:";
    private static final String ARGUMENTS_DESCRIPTION_MESSAGE = "Arguments syntax:\n\t MegaMek [-log <logfile>] [(-gui <guiname>)|(-dedicated)|(-validate)|(-export)|(-eqdb)|(-eqedb] [<args>]";
    private static final String UNKNOWN_GUI_MESSAGE = "Unknown GUI:";
    private static final String GUI_CLASS_NOT_FOUND_MESSAGE = "Couldn't find the GUI Class:";
    private static final String DEFAULT_LOG_FILE_NAME = "megameklog.txt"; //$NON-NLS-1$
    private static String PROPERTIES_FILE = "megamek/MegaMek.properties";

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

            // Redirect output to logfiles, unless turned off.
            if (logFileName != null) {
                MegaMek.redirectOutput(logFileName);
            }

            MegaMek.showInfo();

            String[] restArgs = cp.getRestArgs();
            if (cp.dedicatedServer()) {
                MegaMek.startDedicatedServer(restArgs);
            } else {
                String interfaceName = cp.getGuiName();
                if (interfaceName == null) {
                    interfaceName = PreferenceManager.getClientPreferences()
                            .getGUIName();
                }
                MegaMek.startGUI(interfaceName, restArgs);
            }

        } catch (CommandLineParser.ParseException e) {
            StringBuffer message = new StringBuffer(INCORRECT_ARGUMENTS_MESSAGE)
                    .append(e.getMessage()).append('\n');
            message.append(ARGUMENTS_DESCRIPTION_MESSAGE);
            MegaMek.displayMessageAndExit(message.toString());
        }
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
     * @param logFileName The file name to redirect to.
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
                            + File.separator + logFileName), 64));
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
     * @param args the arguments to the dedicated server.
     */
    private static void startDedicatedServer(String[] args) {
        StringBuffer message = new StringBuffer("Starting Dedicated Server. "); //$NON-NLS-1$
        MegaMek.dumpArgs(message, args);
        MegaMek.displayMessage(message.toString());
        DedicatedServer.start(args);
    }

    /**
     * Attempts to start the GUI with the given name. If the GUI is unknown the
     * program will exit.
     *
     * @param guiName The name of the GUI, usually AWT or swing
     * @param args the arguments to be passed onto the GUI.
     */
    private static void startGUI(String guiName, String[] args) {
        megamek.debug.Assert.assertTrue(guiName != null,
                "guiName must be non-null"); //$NON-NLS-1$
        megamek.debug.Assert.assertTrue(args != null, "args must be non-null");
        IMegaMekGUI mainGui = MegaMek.getGui(guiName);
        if (mainGui == null) {
            MegaMek.displayMessageAndExit(UNKNOWN_GUI_MESSAGE + guiName);
        } else {
            StringBuffer message = new StringBuffer("Starting GUI "); //$NON-NLS-1$
            message.append(guiName).append(". "); //$NON-NLS-1$
            MegaMek.dumpArgs(message, args);
            MegaMek.displayMessage(message.toString());
            mainGui.start(args);
        }
    }

    /**
     * Return the Interface to the GUI specified by the name in guiName.
     *
     * @param guiName the name of the GUI, will be passed on to
     *            {@link #getGUIClassName(String)}.
     * @return An that can start a GUI such as
     *         {@link megamek.client.ui.AWT.MegaMekGUI}.
     */
    @SuppressWarnings("unchecked")
    private static IMegaMekGUI getGui(String guiName) {
        megamek.debug.Assert.assertTrue(guiName != null,
                "guiName must be non-null"); //$NON-NLS-1$
        String guiClassName = MegaMek.getGUIClassName(guiName);
        if (guiClassName != null) {
            try {
                Class guiClass = Class.forName(guiClassName);
                if (IMegaMekGUI.class.isAssignableFrom(guiClass)) {
                    IMegaMekGUI result = (IMegaMekGUI) guiClass.newInstance();
                    return result;
                }
            } catch (Exception e) {
            }
            MegaMek.displayMessage(GUI_CLASS_NOT_FOUND_MESSAGE + guiClassName);
        }
        return null;
    }

    private static String getGUIClassName(String guiName) {
        megamek.debug.Assert.assertTrue(guiName != null,
                "guiName must be non-null"); //$NON-NLS-1$
        Properties p = new Properties();
        String key = "gui." + guiName;
        InputStream is = MegaMek.class.getClassLoader().getResourceAsStream(
                PROPERTIES_FILE);
        if (is != null) {
            try {
                p.load(is);
                return p.getProperty(key);
            } catch (IOException e) {
            }
        }
        return null;
    }

    /**
     * This function appends 'agrs: []', to the buffer, with a space separated
     * list of args[] elements between the brackets.
     *
     * @param buffer the buffer to append the list to.
     * @param args the array of strings to copy into a space seperated list.
     */
    private static void dumpArgs(StringBuffer buffer, String[] args) {
        megamek.debug.Assert.assertTrue(buffer != null,
                "buffer must be non-null"); //$NON-NLS-1$
        megamek.debug.Assert.assertTrue(args != null, "args must be non-null"); //$NON-NLS-1$
        if (buffer != null) {
            buffer.append("args: ["); //$NON-NLS-1$
            if (args != null) {
                for (int i = 0, e = args.length; i < e; i++) {
                    if (i != 0) {
                        buffer.append(' ');
                    }
                    buffer.append(args[i]);
                }
            }
            buffer.append("]"); //$NON-NLS-1$
        }

    }

    /**
     * Prints the message to stdout and then exits with errorcode 1.
     *
     * @param message the message to be displayed.
     */
    private static void displayMessageAndExit(String message) {
        MegaMek.displayMessage(message);
        System.exit(1);
    }

    /**
     * Prints the message and flushes the output stream.
     *
     * @param message
     */
    private static void displayMessage(String message) {
        System.out.println(message);
        System.out.flush();
    }

    /**
     * Prints some information about MegaMek. Used in logfiles to figure out the
     * JVM and version of MegaMek.
     */
    private static void showInfo() {
        // echo some useful stuff
        System.out.println("Starting MegaMek v" + VERSION + " ..."); //$NON-NLS-1$ //$NON-NLS-2$
        System.out.println("Compiled on " + new Date(TIMESTAMP).toString()); //$NON-NLS-1$
        System.out.println("Today is " + new Date().toString());
        System.out.println("Java vendor " + System.getProperty("java.vendor")); //$NON-NLS-1$ //$NON-NLS-2$
        System.out
                .println("Java version " + System.getProperty("java.version")); //$NON-NLS-1$ //$NON-NLS-2$
        System.out.println("Platform " //$NON-NLS-1$
                + System.getProperty("os.name") //$NON-NLS-1$
                + " " //$NON-NLS-1$
                + System.getProperty("os.version") //$NON-NLS-1$
                + " (" //$NON-NLS-1$
                + System.getProperty("os.arch") //$NON-NLS-1$
                + ")"); //$NON-NLS-1$
        long maxMemory = Runtime.getRuntime().maxMemory() / 1024;
        System.out.println("Total memory available to MegaMek: "
                + MegaMek.commafy.format(maxMemory) + " kB");
        System.out.println();
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

        public CommandLineParser(String[] args) {
            super(args);
        }

        /**
         * Returns <code>true</code> if the dedicated server option was found
         *
         * @return true iff this is a dedicated server.
         */
        public boolean dedicatedServer() {
            return dedicatedServer;
        }

        /**
         * Returns the GUI Name option value or <code>null</code> if it wasn't
         * set
         *
         * @return GUI Name option value or <code>null</code> if it wasn't set
         */
        public String getGuiName() {
            return guiName;
        }

        /**
         * Returns the log file name option value or <code>null</code> if it
         * wasn't set
         *
         * @return the log file name option value or <code>null</code> if it
         *         wasn't set
         */
        public String getLogFilename() {
            return logFilename;
        }

        /**
         * Returns the the <code>array</code> of the unprocessed arguments
         *
         * @return the the <code>array</code> of the unprocessed arguments
         */
        public String[] getRestArgs() {
            return restArgs;
        }

        @Override
        protected void start() throws ParseException {

            if ((getToken() == TOK_OPTION) && getTokenValue().equals(OPTION_LOG)) {
                nextToken();
                parseLog();
            }
            if ((getToken() == TOK_OPTION) && getTokenValue().equals(OPTION_EQUIPMENT_DB)) {
                nextToken();
                processEquipmentDb();
            }

            if ((getToken() == TOK_OPTION) && getTokenValue().equals(OPTION_EQUIPMENT_EXTENDED_DB)) {
                nextToken();
                processExtendedEquipmentDb();
            }

            if ((getToken() == TOK_OPTION) && getTokenValue().equals(OPTION_UNIT_VALIDATOR)) {
                nextToken();
                processUnitValidator();
            }

            if ((getToken() == TOK_OPTION) && getTokenValue().equals(OPTION_UNIT_EXPORT)) {
                nextToken();
                processUnitExporter();
            }

            if ((getToken() == TOK_OPTION) && getTokenValue().equals(OPTION_DEDICATED)) {
                nextToken();
                dedicatedServer = true;
            } else if ((getToken() == TOK_OPTION) && getTokenValue().equals(OPTION_GUI)) {
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
                megamek.common.EquipmentType.writeEquipmentExtendedDatabase(new File(filename));
            } else {
                error("file name expected"); //$NON-NLS-1$
            }
            System.exit(0);
        }

        private void processUnitValidator() throws ParseException {
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
                        if (unit.getSourceFile().getName().equalsIgnoreCase(
                                filename)) {
                            ms = unit;
                            break;
                        }
                    }
                }

                if (ms == null) {
                    System.err
                            .println(filename
                                    + " not found try using \"chassis model\" for input.");
                } else {
                    try {
                        Entity entity = new MechFileParser(ms.getSourceFile(),
                                ms.getEntryName()).getEntity();
                        System.err.println("Validating Entity: "
                                + entity.getShortNameRaw());
                        EntityVerifier entityVerifier = new EntityVerifier(
                                new File(
                                        "data/mechfiles/UnitVerifierOptions.xml"));
                        MechView mechView = new MechView(entity, false);
                        StringBuffer sb = new StringBuffer(mechView.getMechReadout());
                        if ((entity instanceof Mech) || (entity instanceof Tank)) {
                            TestEntity testEntity = null;
                            if (entity instanceof Mech) {
                                testEntity = new TestMech((Mech) entity,entityVerifier.mechOption, null);
                            }
                            if (entity instanceof Tank) {
                                testEntity = new TestTank((Tank) entity,entityVerifier.tankOption, null);
                            }

                            if (testEntity != null) {
                                testEntity.correctEntity(sb, true);
                            }
                        }
                        System.err.println(sb.toString());

                        // new EntityVerifier(new
                        // File("data/mechfiles/UnitVerifierOptions.xml")).checkEntity(entity,
                        // ms.getSourceFile().toString(), true);
                    } catch (Exception ex) {
                        // ex.printStackTrace();
                        error("\"chassie model\" expected as input"); //$NON-NLS-1$
                    }
                }

            } else {
                error("\"chassie model\" expected as input"); //$NON-NLS-1$
            }
            System.exit(0);
        }

        private void processUnitExporter() throws ParseException {
            String filename;
            if (getToken() == TOK_LITERAL) {
                filename = getTokenValue();
                nextToken();

                if (!new File("./docs").exists()) {
                    new File("./docs").mkdir();
                }

                try {
                    File file = new File("./docs/"+filename);
                    BufferedWriter w = new BufferedWriter(new FileWriter(file));
                    w.write("Megamek Unit Database");
                    w.newLine();
                    w.write("This file can be regenerated with java -jar MegaMek.jar -export filename");
                    w.newLine();
                    w.write("Type,Name,BV,Cost,Year,Tonnage,Canon");
                    w.newLine();

                    MechSummary[] units = MechSummaryCache.getInstance().getAllMechs();
                    for (MechSummary unit : units) {
                        w.write(unit.getUnitType());
                        w.write(",");
                        w.write(unit.getName());
                        w.write(",");
                        w.write(Integer.toString(unit.getBV()));
                        w.write(",");
                        w.write(Integer.toString(unit.getCost()));
                        w.write(",");
                        w.write(Integer.toString(unit.getYear()));
                        w.write(",");
                        w.write(Integer.toString(unit.getTons()));
                        w.write(",");
                        if ( unit.isCanon() ) {
                            w.write("Canon");
                        }else {
                            w.write("Non-Canon");
                        }
                        w.newLine();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
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
