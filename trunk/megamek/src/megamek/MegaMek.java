/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.io.*;
import java.net.URL;
import java.util.*;

import megamek.common.*;
import megamek.client.*;
import megamek.client.util.AdvancedLabel;
import megamek.client.util.widget.*;
import megamek.client.bot.*;
import megamek.server.*;
import megamek.test.*;

public class MegaMek implements ActionListener {
    public static String VERSION = "0.29.71";
    public static long TIMESTAMP = new File("timestamp").lastModified();

    private static final NumberFormat commafy = NumberFormat.getInstance();

    public Frame frame;

    public Client client = null;
    public Server server = null;
    private CommonAboutDialog about = null;
    private CommonHelpDialog help = null;
    private GameOptionsDialog optdlg = null;
    private CommonSettingsDialog setdlg = null;

    /**
     * Contruct a MegaMek, and display the main menu in the
     * specified frame.
     */
    public MegaMek() {
        this.frame = new Frame("MegaMek");
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });

        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);

        frame.setIconImage(frame.getToolkit().getImage("data/images/megamek-icon.gif"));

        CommonMenuBar menuBar = new CommonMenuBar();
        menuBar.addActionListener(this);
        frame.setMenuBar(menuBar);
        showMainMenu();

        // echo some useful stuff
        System.out.println("Starting MegaMek v" + VERSION + " ...");
        System.out.println("Timestamp " + new Date(TIMESTAMP).toString());
        System.out.println("Java vendor " + System.getProperty("java.vendor"));
        System.out.println("Java version " + System.getProperty("java.version"));
        System.out.println(
                           "Platform "
                           + System.getProperty("os.name")
                           + " "
                           + System.getProperty("os.version")
                           + " ("
                           + System.getProperty("os.arch")
                           + ")");
        /*/ BEGIN DEBUG memory
        if ( System.getProperties().getProperty( "java.version" ).charAt(2)
             >= '4' ) {
            long maxMemory = Runtime.getRuntime().maxMemory() / 1024;
            System.out.println("Total memory available to MegaMek: " 
                               + MegaMek.commafy.format(maxMemory) + " kB");
        }
        //  END  DEBUG memory */
        System.out.println();

        // set visible on middle of screen
        Dimension screenSize = frame.getToolkit().getScreenSize();
        frame.pack();
        frame.setLocation(
            screenSize.width / 2 - frame.getSize().width / 2,
            screenSize.height / 2 - frame.getSize().height / 2);

        // Apparently, the MSJDK doesn't handle the menu bar very well,
        //  so we'll try this hack.
        if (System.getProperty("java.vendor").indexOf("Microsoft") != -1) {
            Dimension windowSize = frame.getSize();
            windowSize.height += 25;
            frame.setSize(windowSize);
        }
        //init the cache
        MechSummaryCache.getInstance();

        // Show the window.
        frame.setVisible(true);

        // tell the user about the readme...
        if (true == Settings.nagForReadme) {
            String title = "Welcome to MegaMek " + VERSION;
            String body =
                "MegaMek is a complex application -- if you ever need any help you\n"
                    + "should start by reading the documentation, accessible through\n"
                    + "the Help menu.\n"
                    + " \nWould you like to view the documentation now?\n";
            ConfirmDialog confirm = new ConfirmDialog(frame, title, body, true);
            confirm.show();

            if (!confirm.getShowAgain()) {
                Settings.nagForReadme = false;
                Settings.save();
            };

            if (confirm.getAnswer()) {
                showHelp();
            };
        };
    }

    /**
     * Display the main menu.
     */
    public void showMainMenu() {
        Button hostB, connectB, botB, editB, scenB, loadB, quitB;
        Label labVersion = new Label();

        labVersion.setText("MegaMek version " + VERSION);

        hostB = new Button("Host a New Game...");
        hostB.setActionCommand("fileGameNew");
        hostB.addActionListener(this);

        scenB = new Button("Host a Scenario...");
        scenB.setActionCommand("fileGameScenario");
        scenB.addActionListener(this);

        loadB = new Button("Host a Saved Game...");
        loadB.setActionCommand("fileGameOpen");
        loadB.addActionListener(this);

        connectB = new Button("Connect to a Game...");
        connectB.setActionCommand("fileGameConnect");
        connectB.addActionListener(this);

        botB = new Button("Connect as a Bot...");
        botB.setActionCommand("fileGameConnectBot");
        botB.addActionListener(this);

        editB = new Button("Map Editor");
        editB.setActionCommand("fileBoardNew");
        editB.addActionListener(this);

        quitB = new Button("Quit");
        quitB.setActionCommand("quit");
        quitB.addActionListener(this);

        // initialize splash image
        Image imgSplash = frame.getToolkit().getImage("data/images/megamek-splash.jpg");

        // wait for splash image to load completely
        MediaTracker tracker = new MediaTracker(frame);
        tracker.addImage(imgSplash, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException e) {
            ;
        }
        // make splash image panel
        BufferedPanel panTitle = new BufferedPanel();
        BackGroundDrawer bgdTitle = new BackGroundDrawer(imgSplash);
        panTitle.addBgDrawer(bgdTitle);
        panTitle.setPreferredSize(imgSplash.getWidth(null), imgSplash.getHeight(null));

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        frame.setLayout(gridbag);

        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 4, 1, 1);
        c.ipadx = 10;
        c.ipady = 5;

        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;

        c.gridwidth = 1;
        c.gridheight = 8;
        addBag(panTitle, gridbag, c);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = .05;
        c.weighty = 1.0;
        c.gridx = 1;
        c.gridheight = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridy = 0;
        addBag(labVersion, gridbag, c);
        c.gridy++;
        addBag(hostB, gridbag, c);
        c.gridy++;
        addBag(loadB, gridbag, c);
        c.gridy++;
        addBag(scenB, gridbag, c);
        c.gridy++;
        addBag(connectB, gridbag, c);
        c.gridy++;
        addBag(botB, gridbag, c);
        c.gridy++;
        addBag(editB, gridbag, c);
        c.gridy++;
        addBag(quitB, gridbag, c);

        frame.validate();
    }

    /**
     * Display the game options dialog.
     */
    public void showGameOptions() {
        GameOptions options = new GameOptions();
        options.initialize();
        options.loadOptions(null, null);
        if (optdlg == null) {
            optdlg = new GameOptionsDialog(frame, options);
        }
        optdlg.update(options);
        optdlg.show();
    }

    /**
     * Display the board editor.
     */
    public void showEditor() {
        BoardEditor editor = new BoardEditor();
        launch(editor.getFrame());
        editor.boardNew();
    }

    /**
     * Display the board editor and open an "open" dialog.
     */
    public void showEditorOpen() {
        BoardEditor editor = new BoardEditor();
        launch(editor.getFrame());
        editor.boardLoad();
    }

    /**
     * Start instances of both the client and the server.
     */
    public void host() {
        HostDialog hd;

        hd = new HostDialog(frame);
        hd.show();
        // verify dialog data
        if (hd.name == null || hd.serverPass == null || hd.port == 0) {
            return;
        }

        /* WORK IN PROGRESS **
        // Register the game, if appropriate.
        String metaserver = null;
        if (hd.register) {
            StringBuffer buff = new StringBuffer (hd.metaserver);
            buff.append ("?action=register")
                .append ("&port=")
                .append (hd.port)
                .append ("&owner=")
                .append (hd.name)
                .append ("&goalPlayers=")
                .append (hd.goalPlayers)
                .append ("&version=")
                .append (MegaMek.VERSION)
                ;
            metaserver = buff.toString();
            try {
                URL metaURL = new URL (metaserver);
                BufferedReader reader = new BufferedReader
                    (new InputStreamReader (metaURL.openStream()));
                String line = reader.readLine();
                while (null != line) {
                    System.out.println (line);
                    line = reader.readLine();
                }
            }
            catch (Exception except) {
                except.printStackTrace();
            }
        }
        /* WORK IN PROGRESS **/

        // Players should have to enter a non-blank, non-whitespace name.
        boolean foundValid = false;
        char[] nameChars = hd.name.toCharArray();
        for (int loop = 0; !foundValid && loop < nameChars.length; loop++) {
            if (!Character.isWhitespace(nameChars[loop])) {
                foundValid = true;
            }
        }
        if (!foundValid) {
            new AlertDialog(frame, "Host a Game", "Error: enter a player name.").show();
            return;
        }

        // kick off a RNG check
        megamek.common.Compute.d6();
        // start server
        server = new Server(hd.serverPass, hd.port);
        // initialize client
        client = new Client(hd.name, "localhost", hd.port);
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();
        try {
            client.connect();
        } catch (Exception e) {
            ;
        }
        launch(gui.getFrame());

        server.getGame().getOptions().loadOptions(client, hd.serverPass);
        optdlg = null;
    }

    public void loadGame() {
        FileDialog fd = new FileDialog(frame, "Select saved game...", FileDialog.LOAD);
        fd.setDirectory(".");
        // limit file-list to savedgames only
        fd.setFilenameFilter(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (null != name && name.endsWith(".sav"));
            }
        });
        //Using the FilenameFilter class would be the appropriate way to
        // filter for certain extensions, but it's broken under windoze.  See
        // http://developer.java.sun.com/developer/bugParade/bugs/4031440.html
        // for details.  The hack below is better than nothing.
        fd.setFile("*.sav");

        fd.show();
        if (fd.getFile() == null) {
            return;
        }

        HostDialog hd = new HostDialog(frame);
        hd.show();
        if (hd.name == null || hd.serverPass == null || hd.port == 0) {
            return;
        }

        // Players should have to enter a non-blank, non-whitespace name.
        boolean foundValid = false;
        char[] nameChars = hd.name.toCharArray();
        for (int loop = 0; !foundValid && loop < nameChars.length; loop++) {
            if (!Character.isWhitespace(nameChars[loop])) {
                foundValid = true;
            }
        }
        if (!foundValid) {
            new AlertDialog(frame, "Load a Game", "Error: enter a player name.").show();
            return;
        }

        // kick off a RNG check
        megamek.common.Compute.d6();
        // start server
        server = new Server(hd.serverPass, hd.port);
        if (!server.loadGame(new File(fd.getDirectory(), fd.getFile()))) {
            new AlertDialog(frame, "Load a Game", "Error: unable to load game file.").show();
            server = null;
            return;
        }
        client = new Client(hd.name, "localhost", hd.port);
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();
        try {
            client.connect();
        } catch (Exception e) {
            ;
        }
        optdlg = null;
		launch(gui.getFrame());
    }

    /**
     * Host a game constructed from a scenario file
     */
    public void scenario() {
        FileDialog fd = new FileDialog(frame, "Select a scenario file...", FileDialog.LOAD);
        fd.setDirectory("data" + File.separatorChar + "scenarios");

        // the filter doesn't seem to do anything in windows.  oh well
        FilenameFilter ff = new FilenameFilter() {
            public boolean accept(File f, String s) {
                return s.endsWith(".mms");
            }
        };
        fd.setFilenameFilter(ff);
        fd.setFile("*.mms"); //see comment above for load game dialog
        fd.show();
        if (fd.getFile() == null) {
            return;
        }
        ScenarioLoader sl = new ScenarioLoader(new File(fd.getDirectory(), fd.getFile()));
        Game g = null;
        try {
            g = sl.createGame();
        } catch (Exception e) {
            new AlertDialog(frame, "Host Scenario", "Error: " + e.getMessage()).show();
            return;
        }

        // get player types and colors set
        Player[] pa = new Player[g.getPlayersVector().size()];
        g.getPlayersVector().copyInto(pa);

        ScenarioDialog sd = new ScenarioDialog(frame, pa);
        sd.show();
        if (!sd.bSet) {
            return;
        }

        // host with the scenario.  essentially copied from host()
        HostDialog hd = new HostDialog(frame);
        hd.yourNameF.setText(sd.localName);
        hd.show();
        // verify dialog data
        if (hd.name == null || hd.serverPass == null || hd.port == 0) {
            return;
        }

        // Players should have to enter a non-blank, non-whitespace name.
        boolean foundValid = false;
        char[] nameChars = hd.name.toCharArray();
        for (int loop = 0; !foundValid && loop < nameChars.length; loop++) {
            if (!Character.isWhitespace(nameChars[loop])) {
                foundValid = true;
            }
        }
        if (!foundValid) {
            new AlertDialog(frame, "Host Scenario", "Error: enter a player name.").show();
            return;
        }

        // kick off a RNG check
        megamek.common.Compute.d6();
        // start server
        server = new Server(hd.serverPass, hd.port);
        server.setGame(g);

        // apply any scenario damage
        sl.applyDamage(server);
		ClientGUI gui = null;
        if (sd.localName != "") {
            // initialize game
            client = new Client(hd.name, "localhost", hd.port);
            gui = new ClientGUI(client);
            gui.initialize();
            try {
                client.connect();
            } catch (Exception e) {
                ;
            }
            server.getGame().getOptions().loadOptions(client, hd.serverPass);

            // popup options dialog
            gui.getGameOptionsDialog().update(client.game.getOptions());
            gui.getGameOptionsDialog().show();
        }
        optdlg = null;

        // setup any bots
        for (int x = 0; x < pa.length; x++) {
            if (sd.playerTypes[x] == ScenarioDialog.T_BOT) {
                BotClient c = new TestBot(pa[x].getName(), "localhost", hd.port);
                c.addGameListener(new BotGUI(c));
                try {
                    c.connect();
                } catch (Exception e) {
                    ;
                }
                c.retrieveServerInfo(); 
            }
        }
        launch(gui.getFrame());
    }

    /**
     * Connect to to a game and then launch the chat lounge.
     */
    public void connect() {
        ConnectDialog cd;

        cd = new ConnectDialog(frame);
        cd.show();
        // verify dialog data
        if (cd.name == null || cd.serverAddr == null || cd.port == 0) {
            return;
        }

        // Players should have to enter a non-blank, non-whitespace name.
        boolean foundValid = false;
        char[] nameChars = cd.name.toCharArray();
        for (int loop = 0; !foundValid && loop < nameChars.length; loop++) {
            if (!Character.isWhitespace(nameChars[loop])) {
                foundValid = true;
            }
        }
        if (!foundValid) {
            new AlertDialog(frame, "Connect to Game", "Error: enter a player name.").show();
            return;
        }

        // initialize game
        client = new Client(cd.name, cd.serverAddr, cd.port);
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();
        try {
            client.connect();
        } catch (Exception e) {
            StringBuffer error = new StringBuffer();
            error.append("Error: could not connect to server at ").append(cd.serverAddr).append(":").append(
                cd.port).append(
                ".");
            new AlertDialog(frame, "Host a Game", error.toString()).show();
            frame.setVisible(false);
            client.die();
        }
        launch(gui.getFrame());
    }

    public void connectBot() {
        ConnectDialog cd;

        cd = new ConnectDialog(frame);
        cd.show();
        // verify dialog data
        if (cd.name == null || cd.serverAddr == null || cd.port == 0) {
            return;
        }

        // Players should have to enter a non-blank, non-whitespace name.
        boolean foundValid = false;
        char[] nameChars = cd.name.toCharArray();
        for (int loop = 0; !foundValid && loop < nameChars.length; loop++) {
            if (!Character.isWhitespace(nameChars[loop])) {
                foundValid = true;
            }
        }
        if (!foundValid) {
            new AlertDialog(frame, "Connect to Game", "Error: enter a player name.").show();
            return;
        }

        // initialize game
        client = new TestBot(cd.name, cd.serverAddr, cd.port);
        client.addGameListener(new BotGUI((BotClient)client));
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();
        try {
            client.connect();
        } catch (Exception e) {
            StringBuffer error = new StringBuffer();
            error.append("Error: could not connect to server at ").append(cd.serverAddr).append(":").append(
                cd.port).append(
                ".");
            new AlertDialog(frame, "Host a Game", error.toString()).show();
            frame.setVisible(false);
            client.die();
        }
        launch(gui.getFrame());
        client.retrieveServerInfo();
    }

    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        frame.add(comp);
    }

    /**
     * Called when the user selects the "Help->About" menu item.
     */
    private void showAbout() {
        // Do we need to create the "about" dialog?
        if (this.about == null) {
            this.about = new CommonAboutDialog(this.frame);
        }

        // Show the about dialog.
        this.about.show();
    }

    /**
     * Called when the user selects the "Help->Contents" menu item.
     */
    private void showHelp() {
        if (this.help == null) {
			help = showHelp(this.frame, "readme");
        }
        // Show the help dialog.
        this.help.show();
    }

    /**
     * display the filename in a CommonHelpDialog
     */
    private static CommonHelpDialog showHelp(Frame frame, String filename) {
		Locale l = Locale.getDefault();
		File helpfile = new File(filename + "-" + l.getDisplayLanguage(Locale.ENGLISH) + ".txt");
        if (!helpfile.exists()) {
			helpfile = new File(filename + ".txt");
        }
        return new CommonHelpDialog(frame, helpfile);
    }

    /**
     * Called when the user selects the "View->Client Settings" menu item.
     */
    private void showSettings() {
        // Do we need to create the "settings" dialog?
        if (this.setdlg == null) {
            this.setdlg = new CommonSettingsDialog(this.frame);
        }

        // Show the settings dialog.
        this.setdlg.show();
    }

    /**
     * Called when the quit buttons is pressed or the main menu is closed.
     */
    private void quit() {
        System.exit(0);
    }

    /**
     * Hides this window for later.  Listens to the frame until it closes,
     * then calls unlaunch().
     */
    private void launch(Frame launched) {
        // listen to new frame
        launched.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                unlaunch();
            }
            public void windowClosed(WindowEvent e) {
                unlaunch();
            }
        });
        // hide menu frame
        frame.setVisible(false);
    }

    /**
     * Un-hides the main menu and tries to clean up the client or server.
     */
    private void unlaunch() {
        // clean up server, if we have one
        if (server != null) {
            server.die();
            server = null;
        }
        // show menu frame
        frame.setVisible(true);
    }

    public static void main(String[] args) {

        String logFileName = "MegaMek.log";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-testdice")) {
                TestDice.testDice();
                return;
            } else if (args[i].equals("-dedicated")) {
                // Next argument may be the savegame file's name.
                String savegameFileName = null;
                i++;
                if (i >= args.length || args[i].startsWith("-")) {
                    // no filename -- bump the argument processing back up
                    i--;
                } else {
                    savegameFileName = args[i];
                }
                Settings.load();
                // kick off a RNG check
                megamek.common.Compute.d6();
                // start server
                Server dedicated = new Server(Settings.lastServerPass,
                                              Settings.lastServerPort);
                // load game options from xml file if available
                dedicated.getGame().getOptions().loadOptions(null, null);
                if (null != savegameFileName) {
                    dedicated.loadGame(new File(savegameFileName));
                };
                return;
            } else if (args[i].equals("-log")) {
                // Next argument is the log file's name.
                i++;
                if (i >= args.length || args[i].equals("none") || args[i].equals("off")) {
                    logFileName = null;
                } else {
                    logFileName = args[i];
                }
            } else if (args[i].equals("-testxml")) {
                // Next argument is the log file's name.
                i++;
                if (i >= args.length) {
                    System.err.println("The '-testxml' flag requires a file name.");
                } else {
                    new TinyXMLTest("xml", args[i]);
                }
                return;
            }
        }

        // Redirect output to logfiles, unless turned off.
        if (logFileName != null) {
            try {
                System.out.println("Redirecting output to " + logFileName);
                PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(logFileName), 64));
                System.setOut(ps);
                System.setErr(ps);
            } catch (Exception e) {
                System.err.println("Unable to redirect output to " + logFileName);
                e.printStackTrace();
            }
        } // End log-to-file

        Settings.load();
        new MegaMek();
    }

    public static String getMemoryUsed() {
        long heap = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        long used = (heap - free) / 1024;
        return MegaMek.commafy.format(used) + " kB";
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {
        if (ev.getActionCommand().equalsIgnoreCase("fileBoardNew")) {
            showEditor();
        }
        if (ev.getActionCommand().equalsIgnoreCase("fileBoardOpen")) {
            showEditorOpen();
        }
        if (ev.getActionCommand().equalsIgnoreCase("fileGameNew")) {
            host();
        }
        if (ev.getActionCommand().equalsIgnoreCase("fileGameScenario")) {
            scenario();
        }
        if (ev.getActionCommand().equalsIgnoreCase("fileGameConnect")) {
            connect();
        }
        if (ev.getActionCommand().equalsIgnoreCase("fileGameConnectBot")) {
            connectBot();
        }
        if (ev.getActionCommand().equalsIgnoreCase("fileGameOpen")) {
            loadGame();
        }
        if (ev.getActionCommand().equalsIgnoreCase("viewGameOptions")) {
            showGameOptions();
        }
        if (ev.getActionCommand().equalsIgnoreCase("helpAbout")) {
            showAbout();
        }
        if (ev.getActionCommand().equalsIgnoreCase("helpContents")) {
            showHelp();
        }
        if (ev.getActionCommand().equalsIgnoreCase("viewClientSettings")) {
            showSettings();
        }
        if (ev.getActionCommand().equalsIgnoreCase("quit")) {
            quit();
        }
    }
}

/**
 * here's a quick class for the host new game diaglogue box
 */
class HostDialog extends Dialog implements ActionListener {
    public String name;
    public String serverPass;
    public int port;
    public boolean register;
    public String metaserver;
    public int goalPlayers;

    protected Label yourNameL, serverPassL, portL;
    protected TextField yourNameF, serverPassF, portF;
    protected Checkbox registerC;
    protected Label     metaserverL;
    protected TextField metaserverF;
    protected Label     goalL;
    protected TextField goalF;
    protected Button okayB, cancelB;

    public HostDialog(Frame frame) {
        super(frame, "Host New Game...", true);

        yourNameL = new Label("Your Name:", Label.RIGHT);
        serverPassL = new Label("Server Password:", Label.RIGHT);
        portL = new Label("Port:", Label.RIGHT);

        yourNameF = new TextField(Settings.lastPlayerName, 16);
        yourNameF.addActionListener(this);
        serverPassF = new TextField(Settings.lastServerPass, 16);
        serverPassF.addActionListener(this);
        portF = new TextField(Settings.lastServerPort + "", 4);
        portF.addActionListener(this);
 
        metaserver = Settings.getInstance().get
            ("megamek.megamek.metaservername",
             "http://www.damour.info/cgi-bin/james/metaserver");
        metaserverL = new Label ("Megaserver Name:", Label.RIGHT);
        metaserverF = new TextField (metaserver);
        metaserverL.setEnabled (register);
        metaserverF.setEnabled (register);

        String goalNumber = Settings.getInstance().get
            ("megamek.megamek.goalplayers", "2");
        goalL = new Label ("# Players:", Label.RIGHT);
        goalF = new TextField (goalNumber, 2);
        goalL.setEnabled (register);
        goalF.setEnabled (register);

        registerC = new Checkbox ("Register Game");
        register = false;
        registerC.setState (register);
        registerC.addItemListener( new ItemListener() {
                public void itemStateChanged (ItemEvent event) {
                    boolean state = false;
                    if (ItemEvent.SELECTED == event.getStateChange()) {
                        state = true;
                    }
                    metaserverL.setEnabled (state);
                    metaserverF.setEnabled (state);
                    goalL.setEnabled (state);
                    goalF.setEnabled (state);
                }
            });

        okayB = new Button("Okay");
        okayB.setActionCommand("done");
        okayB.addActionListener(this);
        okayB.setSize(80, 24);

        cancelB = new Button("Cancel");
        cancelB.setActionCommand("cancel");
        cancelB.addActionListener(this);
        cancelB.setSize(80, 24);

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(yourNameL, c);
        add(yourNameL);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(yourNameF, c);
        add(yourNameF);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(serverPassL, c);
        add(serverPassL);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(serverPassF, c);
        add(serverPassF);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(portL, c);
        add(portL);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(portF, c);
        add(portF);

        /* WORK IN PROGRESS **
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(registerC, c);
        add(registerC);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(metaserverL, c);
        add(metaserverL);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(metaserverF, c);
        add(metaserverF);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(goalL, c);
        add(goalL);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(goalF, c);
        add(goalF);
        /* WORK IN PROGRESS **/

        c.ipadx = 20;
        c.ipady = 5;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(okayB, c);
        add(okayB);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(cancelB, c);
        add(cancelB);

        pack();
        setResizable(false);
        setLocation(
            frame.getLocation().x + frame.getSize().width / 2 - getSize().width / 2,
            frame.getLocation().y + frame.getSize().height / 2 - getSize().height / 2);
    }

    public void actionPerformed(ActionEvent e) {
        if (!e.getActionCommand().equals("cancel")) {
            try {
                name = yourNameF.getText();
                serverPass = serverPassF.getText();
                register = registerC.getState();
                metaserver = metaserverF.getText();
                port = Integer.parseInt (portF.getText());
                goalPlayers = Integer.parseInt (goalF.getText());
            } catch (NumberFormatException ex) {
                System.err.println(ex.getMessage());
                port = 2346;
                goalPlayers = 2;
            }

            // update settings
            Settings.lastPlayerName = name;
            Settings.lastServerPass = serverPass;
            Settings.lastServerPort = port;
            Settings.getInstance().set ("megamek.megamek.metaservername",
                                        metaserver);
            Settings.getInstance().set ("megamek.megamek.goalplayers",
                                        Integer.toString (goalPlayers));
        }
        setVisible(false);
    }
}

/**
 * here's a quick class for the connect to game diaglogue box
 */
class ConnectDialog extends Dialog implements ActionListener {
    public String name, serverAddr;
    public int port;

    protected Label yourNameL, serverAddrL, portL;
    protected TextField yourNameF, serverAddrF, portF;
    protected Button okayB, cancelB;

    public ConnectDialog(Frame frame) {
        super(frame, "Connect To Game...", true);

        yourNameL = new Label("Your Name:", Label.RIGHT);
        serverAddrL = new Label("Server Address:", Label.RIGHT);
        portL = new Label("Port:", Label.RIGHT);

        yourNameF = new TextField(Settings.lastPlayerName, 16);
        yourNameF.addActionListener(this);
        serverAddrF = new TextField(Settings.lastConnectAddr, 16);
        serverAddrF.addActionListener(this);
        portF = new TextField(Settings.lastConnectPort + "", 4);
        portF.addActionListener(this);

        okayB = new Button("Okay");
        okayB.setActionCommand("done");
        okayB.addActionListener(this);
        okayB.setSize(80, 24);

        cancelB = new Button("Cancel");
        cancelB.setActionCommand("cancel");
        cancelB.addActionListener(this);
        cancelB.setSize(80, 24);

        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        setLayout(gridbag);

        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(5, 5, 5, 5);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(yourNameL, c);
        add(yourNameL);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(yourNameF, c);
        add(yourNameF);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(serverAddrL, c);
        add(serverAddrL);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(serverAddrF, c);
        add(serverAddrF);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        gridbag.setConstraints(portL, c);
        add(portL);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.WEST;
        gridbag.setConstraints(portF, c);
        add(portF);

        c.ipadx = 20;
        c.ipady = 5;
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        gridbag.setConstraints(okayB, c);
        add(okayB);

        c.gridwidth = GridBagConstraints.REMAINDER;
        gridbag.setConstraints(cancelB, c);
        add(cancelB);

        pack();
        setResizable(false);
        setLocation(
            frame.getLocation().x + frame.getSize().width / 2 - getSize().width / 2,
            frame.getLocation().y + frame.getSize().height / 2 - getSize().height / 2);
    }

    public void actionPerformed(ActionEvent e) {
        if (!e.getActionCommand().equals("cancel")) {
            try {
                name = yourNameF.getText();
                serverAddr = serverAddrF.getText();
                port = Integer.decode(portF.getText()).intValue();

            } catch (NumberFormatException ex) {
                System.err.println(ex.getMessage());
            }

            // update settings
            Settings.lastPlayerName = name;
            Settings.lastConnectAddr = serverAddr;
            Settings.lastConnectPort = port;
        }
        setVisible(false);
    }
}

/**
 * Allow a user to set types and colors for scenario players
 */
class ScenarioDialog extends Dialog implements ActionListener {
    public static final int T_ME = 0;
    public static final int T_HUMAN = 1;
    public static final int T_BOT = 2;

    private Player[] m_players;
    private Label[] m_labels;
    private Choice[] m_typeChoices;
    private ImageButton[] m_camoButtons;
    private Frame m_frame;

    /** The camo selection dialog.
     */
    private CamoChoiceDialog camoDialog;
    private ItemListener prevListener = null;

    public boolean bSet = false;
    public int[] playerTypes;
    public String localName = "";

    public ScenarioDialog(Frame frame, Player[] pa) {
        super(frame, "Set Scenario Players...", true);
        m_frame = frame;
        camoDialog = new CamoChoiceDialog(frame);
        m_players = pa;
        m_labels = new Label[pa.length];
        m_typeChoices = new Choice[pa.length];
        m_camoButtons = new ImageButton[pa.length];

        playerTypes = new int[pa.length];

        for (int x = 0; x < pa.length; x++) {
            final Player curPlayer = m_players[x];
            curPlayer.setColorIndex(x);

            m_labels[x] = new Label(pa[x].getName(), Label.LEFT);

            m_typeChoices[x] = new Choice();
            m_typeChoices[x].add("Me");
            m_typeChoices[x].add("Other Human");
            m_typeChoices[x].add("Bot");
            final Color defaultBackground = m_typeChoices[x].getBackground();

            m_camoButtons[x] = new ImageButton();
            final ImageButton curButton = m_camoButtons[x];
            curButton.setLabel("No Camo");
            curButton.setPreferredSize(84, 72);
            curButton.setBackground(new Color(Player.colorRGBs[x]));
            curButton.setActionCommand("camo");

            // When a camo button is pressed, remove any previous
            // listener from the dialog, update the dialog for the
            // button's player, and add a new listener.
            curButton.addActionListener(new ActionListener() {
                private final CamoChoiceDialog dialog = camoDialog;
                private final ImageButton button = curButton;
                private final Color background = defaultBackground;
                private final Player player = curPlayer;
                public void actionPerformed(ActionEvent e) {
                    if (null != prevListener) {
                        dialog.removeItemListener(prevListener);
                    }
                    if (null == player.getCamoFileName()) {
                        dialog.setCategory(Player.NO_CAMO);
                        dialog.setItemName(Player.colorNames[player.getColorIndex()]);
                    } else {
                        dialog.setCategory(player.getCamoCategory());
                        dialog.setItemName(player.getCamoFileName());
                    }
                    prevListener = new CamoChoiceListener(dialog, button, background, player);
                    dialog.addItemListener(prevListener);
                    dialog.show();
                }
            });

        }

        setLayout(new BorderLayout());
        Panel choicePanel = new Panel();
        choicePanel.setLayout(new GridLayout(pa.length + 1, 0));
        choicePanel.add(new AdvancedLabel("Player Name\nPlayer Type"));
        choicePanel.add(new Label("Camo"));
        for (int x = 0; x < pa.length; x++) {
            Panel typePanel = new Panel();
            typePanel.setLayout(new GridLayout(0, 1));
            typePanel.add(m_labels[x]);
            typePanel.add(m_typeChoices[x]);
            choicePanel.add(typePanel);
            choicePanel.add(m_camoButtons[x]);
        }
        add(choicePanel, BorderLayout.CENTER);

        Panel butPanel = new Panel();
        butPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        Button bOkay = new Button("Okay");
        bOkay.setActionCommand("okay");
        bOkay.addActionListener(this);
        Button bCancel = new Button("Cancel");
        bCancel.setActionCommand("cancel");
        bCancel.addActionListener(this);
        butPanel.add(bOkay);
        butPanel.add(bCancel);
        add(butPanel, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocation(
            frame.getLocation().x + frame.getSize().width / 2 - getSize().width / 2,
            frame.getLocation().y + frame.getSize().height / 2 - getSize().height / 2);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("okay")) {
            boolean bMeSet = false;
            for (int x = 0; x < m_players.length; x++) {
                playerTypes[x] = m_typeChoices[x].getSelectedIndex();
                if (playerTypes[x] == T_ME) {
                    if (bMeSet) {
                        new AlertDialog(m_frame, "Scenario Error", "Only one faction can be set to 'Me'.").show();
                        return;
                    }
                    bMeSet = true;
                    localName = m_players[x].getName();
                }
            }
            bSet = true;
            setVisible(false);
        } else if (e.getActionCommand().equals("cancel")) {
            setVisible(false);
        }
    }
}
