/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

import megamek.client.ui.IMegaMekGUI;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.AbstractCommandLineParser;
import megamek.server.DedicatedServer;

/**
 * @author mev
 *
 */
public class MegaMek {

    public static String VERSION = "0.31.0-dev"; //$NON-NLS-1$

    public static long TIMESTAMP = new File(PreferenceManager.getClientPreferences().getLogDirectory() + File.separator + "timestamp").lastModified(); //$NON-NLS-1$

    private static final NumberFormat commafy = NumberFormat.getInstance();

    private static final String INCORRECT_ARGUMENTS_MESSAGE = "Incorrect arguments:";

    private static final String ARGUMENTS_DESCRIPTION_MESSAGE = "Arguments syntax:\n\t MegaMek [-log <logfile>] [(-gui <guiname>)|(-dedicated)] [<args>]";

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
            if (lf != null ) {
                if (lf.equals("none") || lf.equals("off")) { //$NON-NLS-1$ //$NON-NLS-2$
                    logFileName = null;
                } else {
                    logFileName = lf;
                }
            }

            // Redirect output to logfiles, unless turned off.
            if (logFileName != null) {
                redirectOutput(logFileName);
            }

            showInfo();

            String[] restArgs = cp.getRestArgs();
            if (cp.dedicatedServer()) {
                startDedicatedServer(restArgs);
            } else {
                String interfaceName = cp.getGuiName();
                if (interfaceName == null) {
                    interfaceName = PreferenceManager.getClientPreferences().getGUIName();
                }
                startGUI(interfaceName, restArgs);                
            }
            
        } catch (CommandLineParser.ParseException e){
            StringBuffer message = new StringBuffer(INCORRECT_ARGUMENTS_MESSAGE).append(e.getMessage()).append('\n');
            message.append(ARGUMENTS_DESCRIPTION_MESSAGE);
            displayMessageAndExit(message.toString());                    
        }
    }

    public static String getMemoryUsed() {
        long heap = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        long used = (heap - free) / 1024;
        return commafy.format(used) + " kB"; //$NON-NLS-1$
    }
    
    private static void redirectOutput(String logFileName ) {
        try {
            System.out.println("Redirecting output to " + logFileName); //$NON-NLS-1$
            String sLogDir = PreferenceManager.getClientPreferences().getLogDirectory();
            File logDir = new File(sLogDir);
            if (!logDir.exists()) {
                logDir.mkdir();
            }
            PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(sLogDir + File.separator + logFileName), 64));
            System.setOut(ps);
            System.setErr(ps);
        } catch (Exception e) {
            System.err.println("Unable to redirect output to " + logFileName); //$NON-NLS-1$
            e.printStackTrace();
        }
    }

    private static void startDedicatedServer(String[] args) {
        StringBuffer message = new StringBuffer("Starting Dedicated Server. "); //$NON-NLS-1$
        dumpArgs(message, args);
        displayMessage(message.toString());
        DedicatedServer.start(args);
    }

    private static void startGUI(String guiName, String[] args) {
        megamek.debug.Assert.assertTrue(guiName != null, "guiName must be non-null"); //$NON-NLS-1$
        megamek.debug.Assert.assertTrue(args != null, "args must be non-null");
        IMegaMekGUI mainGui =  getGui(guiName);
        if (mainGui == null) {
            displayMessageAndExit(UNKNOWN_GUI_MESSAGE+guiName);
        } else {
            StringBuffer message = new StringBuffer("Starting GUI "); //$NON-NLS-1$
            message.append(guiName).append(". "); //$NON-NLS-1$
            dumpArgs(message, args);
            displayMessage(message.toString());
            mainGui.start(args); 
        }        
    }

    private static IMegaMekGUI getGui(String guiName) {
        megamek.debug.Assert.assertTrue(guiName != null, "guiName must be non-null"); //$NON-NLS-1$
        String guiClassName = getGUIClassName(guiName);
        if (guiClassName != null) {
                try {
                    Class guiClass = Class.forName(guiClassName);
                    if (IMegaMekGUI.class.isAssignableFrom(guiClass)) {
                        IMegaMekGUI result  = (IMegaMekGUI)guiClass.newInstance();
                        return result;
                    }
                } catch (Exception e) {
                }
                displayMessage(GUI_CLASS_NOT_FOUND_MESSAGE+guiClassName);
        }
        return null;
    }

    private static String getGUIClassName(String guiName) {
        megamek.debug.Assert.assertTrue(guiName != null, "guiName must be non-null"); //$NON-NLS-1$
        Properties p = new Properties();
        String key = "gui."+guiName;
        InputStream is = MegaMek.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);
        if (is != null) {
            try {
                p.load(is);            
                return p.getProperty(key);
            } catch (IOException e) {};
        }
        return null;
    }

    private static void dumpArgs(StringBuffer buffer, String[] args) {
        megamek.debug.Assert.assertTrue(buffer != null, "buffer must be non-null"); //$NON-NLS-1$
        megamek.debug.Assert.assertTrue(args != null, "args must be non-null"); //$NON-NLS-1$
        buffer.append("args: ["); //$NON-NLS-1$
        for(int i = 0, e = args.length; i < e; i++) {
            if (i!=0) buffer.append(' ');
            buffer.append(args[i]);
        }
        buffer.append("]"); //$NON-NLS-1$
        
    }
    private static void displayMessageAndExit(String message) {
        displayMessage(message);
        System.exit(1);
    }

    private static void displayMessage(String message) {
        System.out.println(message);
        System.out.flush();
    }
    
    private static void showInfo() {
        // echo some useful stuff
        System.out.println("Starting MegaMek v" + VERSION + " ..."); //$NON-NLS-1$ //$NON-NLS-2$
        System.out.println("Timestamp " + new Date(TIMESTAMP).toString()); //$NON-NLS-1$
        System.out.println("Java vendor " + System.getProperty("java.vendor")); //$NON-NLS-1$ //$NON-NLS-2$
        System.out.println("Java version " + System.getProperty("java.version")); //$NON-NLS-1$ //$NON-NLS-2$
        System.out.println(
                           "Platform " //$NON-NLS-1$
                           + System.getProperty("os.name") //$NON-NLS-1$
                           + " " //$NON-NLS-1$
                           + System.getProperty("os.version") //$NON-NLS-1$
                           + " (" //$NON-NLS-1$
                           + System.getProperty("os.arch") //$NON-NLS-1$
                           + ")"); //$NON-NLS-1$
        /*/ BEGIN DEBUG memory
        if ( System.getProperties().getProperty( "java.version" ).charAt(2)
             >= '4' ) {
            long maxMemory = Runtime.getRuntime().maxMemory() / 1024;
            System.out.println("Total memory available to MegaMek: " 
                               + MegaMek.commafy.format(maxMemory) + " kB");
        }
        //  END  DEBUG memory */
        System.out.println();
        
    }

    private static class CommandLineParser extends AbstractCommandLineParser {

        private String logFilename;
        private String guiName;
        private boolean dedicatedServer = false; 
        private String[] restArgs = new String[0];

        //Options
        private static final String OPTION_DEDICATED = "dedicated"; //$NON-NLS-1$
        private static final String OPTION_GUI = "gui"; //$NON-NLS-1$
        private static final String OPTION_LOG = "log"; //$NON-NLS-1$

        public CommandLineParser(String[] args) {
            super(args);
        }
        
        /**
         * Returns <code>true</code> if the dedicated server option was found
         * @return
         */
        public boolean dedicatedServer() {
            return dedicatedServer;
        }

        /**
         * Returns the GUI Name option value or <code>null</code> if
         * it wasn't set
         * @return GUI Name option value or <code>null</code> if
         * it wasn't set
         */
        public String getGuiName() {
            return guiName;
        }

        /**
         * Returns the log file name option value or <code>null</code> if
         * it wasn't set
         * @return the log file name option value or <code>null</code> if
         * it wasn't set
         */
        public String getLogFilename() {
            return logFilename;
        }

        /**
         * Returns the the <code>array</code> of the unprocessed arguments
         * @return the the <code>array</code> of the unprocessed arguments 
         */
        public String[] getRestArgs() {
            return restArgs;
        }

        protected void start() throws ParseException {

            if (getToken() == TOK_OPTION && getTokenValue().equals(OPTION_LOG)) {
                nextToken();
                parseLog();
            }

            if (getToken() == TOK_OPTION && getTokenValue().equals(OPTION_DEDICATED)) {
                nextToken();
                dedicatedServer = true;
            } else if (getToken() == TOK_OPTION && getTokenValue().equals(OPTION_GUI)) {
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

        private void parseGUI() throws ParseException  {
            if (getToken() == TOK_LITERAL) {
                guiName = getTokenValue();
                nextToken();
            } else {
                error("GUI name expected"); //$NON-NLS-1$                
            }
        }

        private void processRestOfInput() {
            Vector v = new Vector();
            while(getArgValue() != null ) {
                v.addElement(getArgValue());
                nextArg();
            }
            setToken(TOK_EOF);
            setTokenValue(null);
            restArgs = (String[])v.toArray(new String[0]);
        }
    }
 }

