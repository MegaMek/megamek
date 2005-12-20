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
import java.util.*;

import megamek.common.*;
import megamek.common.options.GameOptions;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.StringUtil;
import megamek.client.*;
import megamek.client.ui.AWT.AlertDialog;
import megamek.client.ui.AWT.BoardEditor;
import megamek.client.ui.AWT.CamoChoiceDialog;
import megamek.client.ui.AWT.CamoChoiceListener;
import megamek.client.ui.AWT.ClientGUI;
import megamek.client.ui.AWT.CommonAboutDialog;
import megamek.client.ui.AWT.CommonHelpDialog;
import megamek.client.ui.AWT.CommonMenuBar;
import megamek.client.ui.AWT.CommonSettingsDialog;
import megamek.client.ui.AWT.ConfirmDialog;
import megamek.client.ui.AWT.GUIPreferences;
import megamek.client.ui.AWT.GameOptionsDialog;
import megamek.client.ui.AWT.Messages;
import megamek.client.ui.AWT.widget.AdvancedLabel;
import megamek.client.ui.AWT.util.PlayerColors;
import megamek.client.ui.AWT.widget.*;
import megamek.client.bot.*;
import megamek.client.bot.ui.AWT.BotGUI;
import megamek.server.*;
import megamek.test.*;

public class MegaMek implements ActionListener {
    public static String VERSION = "0.30.7-dev"; //$NON-NLS-1$
    public static long TIMESTAMP = new File(PreferenceManager.getClientPreferences().getLogDirectory() + File.separator + "timestamp").lastModified(); //$NON-NLS-1$

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
        this.frame = new Frame("MegaMek"); //$NON-NLS-1$
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });

        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);

        frame.setIconImage(frame.getToolkit().getImage("data/images/misc/megamek-icon.gif")); //$NON-NLS-1$

        CommonMenuBar menuBar = new CommonMenuBar();
        menuBar.addActionListener(this);
        frame.setMenuBar(menuBar);
        showMainMenu();

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

        // set visible on middle of screen
        Dimension screenSize = frame.getToolkit().getScreenSize();
        frame.pack();
        frame.setLocation(
            screenSize.width / 2 - frame.getSize().width / 2,
            screenSize.height / 2 - frame.getSize().height / 2);

        // Apparently, the MSJDK doesn't handle the menu bar very well,
        //  so we'll try this hack.
        if (System.getProperty("java.vendor").indexOf("Microsoft") != -1) { //$NON-NLS-1$ //$NON-NLS-2$
            Dimension windowSize = frame.getSize();
            windowSize.height += 25;
            frame.setSize(windowSize);
        }
        //init the cache
        MechSummaryCache.getInstance();

        // Show the window.
        frame.setVisible(true);

        // tell the user about the readme...
        if (true == GUIPreferences.getInstance().getNagForReadme()) {
            ConfirmDialog confirm = new ConfirmDialog(frame, 
                    Messages.getString("MegaMek.welcome.title") + VERSION, //$NON-NLS-1$ 
                    Messages.getString("MegaMek.welcome.message"), //$NON-NLS-1$
                    true);
            confirm.show();

            if (!confirm.getShowAgain()) {
                GUIPreferences.getInstance().setNagForReadme(false);
            }

            if (confirm.getAnswer()) {
                showHelp();
            }
        }
    }

    /**
     * Display the main menu.
     */
    public void showMainMenu() {
        Button hostB, connectB, botB, editB, scenB, loadB, quitB;
        Label labVersion = new Label();

        labVersion.setText(Messages.getString("MegaMek.Version") + VERSION); //$NON-NLS-1$

        hostB = new Button(Messages.getString("MegaMek.hostNewGame.label")); //$NON-NLS-1$
        hostB.setActionCommand("fileGameNew"); //$NON-NLS-1$
        hostB.addActionListener(this);

        scenB = new Button(Messages.getString("MegaMek.hostScenario.label")); //$NON-NLS-1$
        scenB.setActionCommand("fileGameScenario"); //$NON-NLS-1$
        scenB.addActionListener(this);

        loadB = new Button(Messages.getString("MegaMek.hostSavedGame.label")); //$NON-NLS-1$
        loadB.setActionCommand("fileGameOpen"); //$NON-NLS-1$
        loadB.addActionListener(this);

        connectB = new Button(Messages.getString("MegaMek.Connect.label")); //$NON-NLS-1$
        connectB.setActionCommand("fileGameConnect"); //$NON-NLS-1$
        connectB.addActionListener(this);

        botB = new Button(Messages.getString("MegaMek.ConnectAsBot.label")); //$NON-NLS-1$
        botB.setActionCommand("fileGameConnectBot"); //$NON-NLS-1$
        botB.addActionListener(this);

        editB = new Button(Messages.getString("MegaMek.MapEditor.label")); //$NON-NLS-1$
        editB.setActionCommand("fileBoardNew"); //$NON-NLS-1$
        editB.addActionListener(this);

        quitB = new Button(Messages.getString("MegaMek.Quit.label")); //$NON-NLS-1$
        quitB.setActionCommand("quit"); //$NON-NLS-1$
        quitB.addActionListener(this);

        // initialize splash image
        Image imgSplash = frame.getToolkit().getImage("data/images/misc/megamek-splash.jpg"); //$NON-NLS-1$

        // wait for splash image to load completely
        MediaTracker tracker = new MediaTracker(frame);
        tracker.addImage(imgSplash, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException e) {
            
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
        options.loadOptions(null);
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
            new AlertDialog(frame, Messages.getString("MegaMek.PlayerNameAlert.title"), Messages.getString("MegaMek.PlayerNameAlert.message")).show(); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // kick off a RNG check
        megamek.common.Compute.d6();
        // start server
        server = new Server(hd.serverPass, hd.port);
        // initialize client
        client = new Client(hd.name, "localhost", hd.port); //$NON-NLS-1$
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();
        try {
            client.connect();
        } catch (Exception e) {
        
        }
        launch(gui.getFrame());

        Vector changedOptions = server.getGame().getOptions().loadOptions(hd.serverPass);
        if ( changedOptions.size() > 0 ) {
            client.sendGameOptions(hd.serverPass, changedOptions);
        }
        optdlg = null;
    }

    public void loadGame() {
        FileDialog fd = new FileDialog(frame, Messages.getString("MegaMek.SaveGameDialog.title"), FileDialog.LOAD); //$NON-NLS-1$
        fd.setDirectory("savegames"); //$NON-NLS-1$
        // limit file-list to savedgames only
        fd.setFilenameFilter(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return (null != name && name.endsWith(".sav")); //$NON-NLS-1$
            }
        });
        //Using the FilenameFilter class would be the appropriate way to
        // filter for certain extensions, but it's broken under windoze.  See
        // http://developer.java.sun.com/developer/bugParade/bugs/4031440.html
        // for details.  The hack below is better than nothing.
        //New note: Since we have a dedicated save dir now, I'm commenting
        // this out.
        //fd.setFile("*.sav"); //$NON-NLS-1$

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
            new AlertDialog(frame, Messages.getString("MegaMek.PlayerNameAlert1.title"), Messages.getString("MegaMek.PlayerNameAlert1.message")).show(); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // kick off a RNG check
        megamek.common.Compute.d6();
        // start server
        server = new Server(hd.serverPass, hd.port);
        if (!server.loadGame(new File(fd.getDirectory(), fd.getFile()))) {
            new AlertDialog(frame, Messages.getString("MegaMek.LoadGameAlert.title"), Messages.getString("MegaMek.LoadGameAlert.message")).show(); //$NON-NLS-1$ //$NON-NLS-2$
            server = null;
            return;
        }
        client = new Client(hd.name, "localhost", hd.port); //$NON-NLS-1$
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();
        try {
            client.connect();
        } catch (Exception e) {
        
        }
        optdlg = null;
        launch(gui.getFrame());
    }

    /**
     * Host a game constructed from a scenario file
     */
    public void scenario() {
        FileDialog fd = new FileDialog(frame, Messages.getString("MegaMek.SelectScenarioDialog.title"), FileDialog.LOAD); //$NON-NLS-1$
        fd.setDirectory("data" + File.separatorChar + "scenarios"); //$NON-NLS-1$ //$NON-NLS-2$

        // the filter doesn't seem to do anything in windows.  oh well
        FilenameFilter ff = new FilenameFilter() {
            public boolean accept(File f, String s) {
                return s.endsWith(".mms"); //$NON-NLS-1$
            }
        };
        fd.setFilenameFilter(ff);
        fd.setFile("*.mms"); //see comment above for load game dialog //$NON-NLS-1$
        fd.show();
        if (fd.getFile() == null) {
            return;
        }
        ScenarioLoader sl = new ScenarioLoader(new File(fd.getDirectory(), fd.getFile()));
        IGame g = null;
        try {
            g = sl.createGame();
        } catch (Exception e) {
            new AlertDialog(frame, Messages.getString("MegaMek.HostScenarioAllert.title"), Messages.getString("MegaMek.HostScenarioAllert.message") + e.getMessage()).show(); //$NON-NLS-1$ //$NON-NLS-2$
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
        boolean hasSlot = false;
        if (!(sd.localName.equals("")))
            hasSlot = true;
        hd.yourNameF.setText(sd.localName);
        hd.show();
        // verify dialog data
        if (hd.name == null || hd.serverPass == null || hd.port == 0) {
            return;
        }
        sd.localName = hd.name;

        // Players should have to enter a non-blank, non-whitespace name.
        boolean foundValid = false;
        char[] nameChars = hd.name.toCharArray();
        for (int loop = 0; !foundValid && loop < nameChars.length; loop++) {
            if (!Character.isWhitespace(nameChars[loop])) {
                foundValid = true;
            }
        }
        if (!foundValid) {
            new AlertDialog(frame, Messages.getString("MegaMek.HostScenarioAllert1.title"), Messages.getString("MegaMek.HostScenarioAllert1.message")).show(); //$NON-NLS-1$ //$NON-NLS-2$
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
        if (sd.localName != "") { //$NON-NLS-1$
            // initialize game
            client = new Client(hd.name, "localhost", hd.port); //$NON-NLS-1$
            gui = new ClientGUI(client);
            gui.initialize();
            try {
                client.connect();
            } catch (Exception e) {
            
            }
            Vector changedOptions = server.getGame().getOptions().loadOptions(hd.serverPass);
            if ( changedOptions.size() > 0 ) {
                client.sendGameOptions(hd.serverPass, changedOptions);
            }

            // popup options dialog
            gui.getGameOptionsDialog().update(client.game.getOptions());
            gui.getGameOptionsDialog().show();
        }
        optdlg = null;

        // setup any bots
        for (int x = 0; x < pa.length; x++) {
            if (sd.playerTypes[x] == ScenarioDialog.T_BOT) {
                BotClient c = new TestBot(pa[x].getName(), "localhost", hd.port); //$NON-NLS-1$
                c.game.addGameListener(new BotGUI(c));
                try {
                    c.connect();
                } catch (Exception e) {
                    
                }
                c.retrieveServerInfo(); 
            }
        }

        // If he didn't have a name when hasSlot was set, then the host should be
        // an observer.
        if (!hasSlot) {
            Enumeration pE = server.getGame().getPlayers();
            while (pE.hasMoreElements()) {
                Player tmpP = (Player)pE.nextElement();
                if (tmpP.getName().equals(sd.localName))
                    tmpP.setObserver(true);
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
            new AlertDialog(frame, Messages.getString("MegaMek.ConnectAllert.title"), Messages.getString("MegaMek.ConnectAllert.message")).show(); //$NON-NLS-1$ //$NON-NLS-2$
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
            new AlertDialog(frame, Messages.getString("MegaMek.HostGameAllert.title"), error.toString()).show(); //$NON-NLS-1$
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
            new AlertDialog(frame, Messages.getString("MegaMek.ConnectGameAllert.title"), Messages.getString("MegaMek.ConnectGameAllert.message")).show(); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // initialize game
        client = new TestBot(cd.name, cd.serverAddr, cd.port);
        client.game.addGameListener(new BotGUI((BotClient)client));
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();
        try {
            client.connect();
        } catch (Exception e) {
            StringBuffer error = new StringBuffer();
            error.append("Error: could not connect to server at ").append(cd.serverAddr).append(":").append(
                cd.port).append(
                ".");
            new AlertDialog(frame, Messages.getString("MegaMek.HostGameAllert.title"), error.toString()).show(); //$NON-NLS-1$
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
            help = showHelp(this.frame, "readme"); //$NON-NLS-1$
        }
        // Show the help dialog.
        this.help.show();
    }

    /**
     * display the filename in a CommonHelpDialog
     */
    private static CommonHelpDialog showHelp(Frame frame, String filename) {
        Locale l = Locale.getDefault();
        File helpfile = new File(filename + "-" + l.getDisplayLanguage(Locale.ENGLISH) + ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        if (!helpfile.exists()) {
            helpfile = new File(filename + ".txt"); //$NON-NLS-1$
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
        PreferenceManager.getInstance().save();
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

        //just to free some memory
        client = null;        
        System.gc();
        System.runFinalization();
    }

    public static void main(String[] args) {
        int usePort = 2346;

        String logFileName = "megameklog.txt"; //$NON-NLS-1$
        if (PreferenceManager.getClientPreferences().stampFilenames()) {
            logFileName = StringUtil.addDateTimeStamp(logFileName);
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-testdice")) { //$NON-NLS-1$
                TestDice.testDice();
                return;
            } else if (args[i].equals("-dedicated")) { //$NON-NLS-1$
                // Next argument may be the savegame file's name.
                String savegameFileName = null;
                i++;
                if (i >= args.length || args[i].startsWith("-")) { //$NON-NLS-1$
                    // no filename -- bump the argument processing back up
                    i--;
                } else {
                    savegameFileName = args[i];
                }

                // Next argument may be "-port <number>"
                i++;
                if (i<args.length) {
                    if (args[i].equals("-port")) { //$NON-NLS-1$
                        i++;
                        if (i<args.length) {
                            usePort = Integer.decode(args[i]).intValue();
                        } else {
                            i--;
                            usePort = PreferenceManager.getClientPreferences().getLastServerPort();
                        }
                    } else {
                        i--;
                        usePort = PreferenceManager.getClientPreferences().getLastServerPort();
                    }
                }
                // kick off a RNG check
                megamek.common.Compute.d6();
                // start server
                Server dedicated = new Server(PreferenceManager.getClientPreferences().getLastServerPass(),
                        usePort);
                // load game options from xml file if available
                dedicated.getGame().getOptions().loadOptions(null);
                if (null != savegameFileName) {
                    dedicated.loadGame(new File(savegameFileName));
                }
                return;
            } else if (args[i].equals("-log")) { //$NON-NLS-1$
                // Next argument is the log file's name.
                i++;
                if (i >= args.length || args[i].equals("none") || args[i].equals("off")) { //$NON-NLS-1$ //$NON-NLS-2$
                    logFileName = null;
                } else {
                    logFileName = args[i];
                }
            } else if (args[i].equals("-testxml")) { //$NON-NLS-1$
                // Next argument is the log file's name.
                i++;
                if (i >= args.length) {
                    System.err.println("The '-testxml' flag requires a file name."); //$NON-NLS-1$
                } else {
                    new TinyXMLTest("xml", args[i]); //$NON-NLS-1$
                }
                return;
            }
        }

        // Redirect output to logfiles, unless turned off.
        if (logFileName != null) {
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
        } // End log-to-file

        new MegaMek();
    }

    public static String getMemoryUsed() {
        long heap = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        long used = (heap - free) / 1024;
        return MegaMek.commafy.format(used) + " kB"; //$NON-NLS-1$
    }

    //
    // ActionListener
    //
    public void actionPerformed(ActionEvent ev) {
        if (ev.getActionCommand().equalsIgnoreCase("fileBoardNew")) { //$NON-NLS-1$
            showEditor();
        }
        if (ev.getActionCommand().equalsIgnoreCase("fileBoardOpen")) { //$NON-NLS-1$
            showEditorOpen();
        }
        if (ev.getActionCommand().equalsIgnoreCase("fileGameNew")) { //$NON-NLS-1$
            host();
        }
        if (ev.getActionCommand().equalsIgnoreCase("fileGameScenario")) { //$NON-NLS-1$
            scenario();
        }
        if (ev.getActionCommand().equalsIgnoreCase("fileGameConnect")) { //$NON-NLS-1$
            connect();
        }
        if (ev.getActionCommand().equalsIgnoreCase("fileGameConnectBot")) { //$NON-NLS-1$
            connectBot();
        }
        if (ev.getActionCommand().equalsIgnoreCase("fileGameOpen")) { //$NON-NLS-1$
            loadGame();
        }
        if (ev.getActionCommand().equalsIgnoreCase("viewGameOptions")) { //$NON-NLS-1$
            showGameOptions();
        }
        if (ev.getActionCommand().equalsIgnoreCase("helpAbout")) { //$NON-NLS-1$
            showAbout();
        }
        if (ev.getActionCommand().equalsIgnoreCase("helpContents")) { //$NON-NLS-1$
            showHelp();
        }
        if (ev.getActionCommand().equalsIgnoreCase("viewClientSettings")) { //$NON-NLS-1$
            showSettings();
        }
        if (ev.getActionCommand().equalsIgnoreCase("quit")) { //$NON-NLS-1$
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
        super(frame, Messages.getString("MegaMek.HostDialog.title"), true); //$NON-NLS-1$

        yourNameL = new Label(Messages.getString("MegaMek.yourNameL"), Label.RIGHT); //$NON-NLS-1$
        serverPassL = new Label(Messages.getString("MegaMek.serverPassL"), Label.RIGHT); //$NON-NLS-1$
        portL = new Label(Messages.getString("MegaMek.portL"), Label.RIGHT); //$NON-NLS-1$

        yourNameF = new TextField(PreferenceManager.getClientPreferences().getLastPlayerName(), 16);
        yourNameF.addActionListener(this);
        serverPassF = new TextField(PreferenceManager.getClientPreferences().getLastServerPass(), 16);
        serverPassF.addActionListener(this);
        portF = new TextField(PreferenceManager.getClientPreferences().getLastServerPort() + "", 4); //$NON-NLS-1$
        portF.addActionListener(this);
 
        IClientPreferences cs = PreferenceManager.getClientPreferences();
        metaserver = cs.getMetaServerName();
        metaserverL = new Label (Messages.getString("MegaMek.metaserverL"), Label.RIGHT); //$NON-NLS-1$
        metaserverF = new TextField (metaserver);
        metaserverL.setEnabled (register);
        metaserverF.setEnabled (register);

        int goalNumber = cs.getGoalPlayers();
        goalL = new Label (Messages.getString("MegaMek.goalL"), Label.RIGHT); //$NON-NLS-1$
        goalF = new TextField (Integer.toString(goalNumber), 2);
        goalL.setEnabled (register);
        goalF.setEnabled (register);

        registerC = new Checkbox (Messages.getString("MegaMek.registerC")); //$NON-NLS-1$
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

        okayB = new Button(Messages.getString("Okay")); //$NON-NLS-1$
        okayB.setActionCommand("done"); //$NON-NLS-1$
        okayB.addActionListener(this);
        okayB.setSize(80, 24);

        cancelB = new Button(Messages.getString("Cancel")); //$NON-NLS-1$
        cancelB.setActionCommand("cancel"); //$NON-NLS-1$
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
        if (!e.getActionCommand().equals("cancel")) { //$NON-NLS-1$
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
            PreferenceManager.getClientPreferences().setLastPlayerName(name);
            PreferenceManager.getClientPreferences().setLastServerPass(serverPass);
            PreferenceManager.getClientPreferences().setLastServerPort(port);
            PreferenceManager.getClientPreferences().setValue("megamek.megamek.metaservername", //$NON-NLS-1$
                                        metaserver);
            PreferenceManager.getClientPreferences().setValue("megamek.megamek.goalplayers", //$NON-NLS-1$
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
        super(frame, Messages.getString("MegaMek.ConnectDialog.title"), true); //$NON-NLS-1$

        yourNameL = new Label(Messages.getString("MegaMek.yourNameL"), Label.RIGHT); //$NON-NLS-1$
        serverAddrL = new Label(Messages.getString("MegaMek.serverAddrL"), Label.RIGHT); //$NON-NLS-1$
        portL = new Label(Messages.getString("MegaMek.portL"), Label.RIGHT); //$NON-NLS-1$

        yourNameF = new TextField(PreferenceManager.getClientPreferences().getLastPlayerName(), 16);
        yourNameF.addActionListener(this);
        serverAddrF = new TextField(PreferenceManager.getClientPreferences().getLastConnectAddr(), 16);
        serverAddrF.addActionListener(this);
        portF = new TextField(PreferenceManager.getClientPreferences().getLastConnectPort() + "", 4); //$NON-NLS-1$
        portF.addActionListener(this);

        okayB = new Button(Messages.getString("Okay")); //$NON-NLS-1$
        okayB.setActionCommand("done"); //$NON-NLS-1$
        okayB.addActionListener(this);
        okayB.setSize(80, 24);

        cancelB = new Button(Messages.getString("Cancel")); //$NON-NLS-1$
        cancelB.setActionCommand("cancel"); //$NON-NLS-1$
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
        if (!e.getActionCommand().equals("cancel")) { //$NON-NLS-1$
            try {
                name = yourNameF.getText();
                serverAddr = serverAddrF.getText();
                port = Integer.decode(portF.getText()).intValue();

            } catch (NumberFormatException ex) {
                System.err.println(ex.getMessage());
            }

            // update settings
            PreferenceManager.getClientPreferences().setLastPlayerName(name);
            PreferenceManager.getClientPreferences().setLastConnectAddr(serverAddr);
            PreferenceManager.getClientPreferences().setLastConnectPort(port);
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
    public String localName = ""; //$NON-NLS-1$

    public ScenarioDialog(Frame frame, Player[] pa) {
        super(frame, Messages.getString("MegaMek.ScenarioDialog.title"), true); //$NON-NLS-1$
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
            m_typeChoices[x].add(Messages.getString("MegaMek.ScenarioDialog.me")); //$NON-NLS-1$
            m_typeChoices[x].add(Messages.getString("MegaMek.ScenarioDialog.otherh")); //$NON-NLS-1$
            m_typeChoices[x].add(Messages.getString("MegaMek.ScenarioDialog.bot")); //$NON-NLS-1$
            final Color defaultBackground = m_typeChoices[x].getBackground();

            m_camoButtons[x] = new ImageButton();
            final ImageButton curButton = m_camoButtons[x];
            curButton.setLabel(Messages.getString("MegaMek.NoCamoBtn")); //$NON-NLS-1$
            curButton.setPreferredSize(84, 72);
            curButton.setBackground(PlayerColors.getColor(x));
            curButton.setActionCommand("camo"); //$NON-NLS-1$

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
        choicePanel.add(new AdvancedLabel(Messages.getString("MegaMek.ScenarioDialog.pNameType"))); //$NON-NLS-1$
        choicePanel.add(new Label(Messages.getString("MegaMek.ScenarioDialog.Camo"))); //$NON-NLS-1$
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
        Button bOkay = new Button(Messages.getString("Okay")); //$NON-NLS-1$
        bOkay.setActionCommand("okay"); //$NON-NLS-1$
        bOkay.addActionListener(this);
        Button bCancel = new Button(Messages.getString("Cancel")); //$NON-NLS-1$
        bCancel.setActionCommand("cancel"); //$NON-NLS-1$
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
        if (e.getActionCommand().equals("okay")) { //$NON-NLS-1$
            boolean bMeSet = false;
            for (int x = 0; x < m_players.length; x++) {
                playerTypes[x] = m_typeChoices[x].getSelectedIndex();
                if (playerTypes[x] == T_ME) {
                    if (bMeSet) {
                        new AlertDialog(m_frame, Messages.getString("MegaMek.ScenarioErrorAllert.title"), Messages.getString("MegaMek.ScenarioErrorAllert.message")).show(); //$NON-NLS-1$ //$NON-NLS-2$
                        return;
                    }
                    bMeSet = true;
                    localName = m_players[x].getName();
                }
            }
            bSet = true;
            setVisible(false);
        } else if (e.getActionCommand().equals("cancel")) { //$NON-NLS-1$
            setVisible(false);
        }
    }
}
