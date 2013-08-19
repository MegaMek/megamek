/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

package megamek.client.ui.AWT;

import java.awt.Button;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Label;
import java.awt.MediaTracker;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import megamek.MegaMek;
import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.TestBot;
import megamek.client.bot.ui.AWT.BotGUI;
import megamek.client.ui.IMegaMekGUI;
import megamek.client.ui.Messages;
import megamek.client.ui.AWT.widget.BackGroundDrawer;
import megamek.client.ui.AWT.widget.BufferedPanel;
import megamek.common.Configuration;
import megamek.common.IGame;
import megamek.common.MechSummaryCache;
import megamek.common.IPlayer;
import megamek.common.Player;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.preference.PreferenceManager;
import megamek.server.ScenarioLoader;
import megamek.server.Server;

public class MegaMekGUI implements IMegaMekGUI {
    private static final String FILENAME_MEGAMEK_SPLASH = "megamek-splash.jpg"; //$NON-NLS-1$
    private static final String FILENAME_ICON_16X16 = "megamek-icon-16x16.png"; //$NON-NLS-1$
    private static final String FILENAME_ICON_32X32 = "megamek-icon-32x32.png"; //$NON-NLS-1$
    private static final String FILENAME_ICON_48X48 = "megamek-icon-48x48.png"; //$NON-NLS-1$
    private static final String FILENAME_ICON_256X256 = "megamek-icon-256x256.png"; //$NON-NLS-1$

    public Frame frame;

    public Client client = null;
    public Server server = null;
    private CommonAboutDialog about = null;
    private CommonHelpDialog help = null;
    private GameOptionsDialog optdlg = null;
    private CommonSettingsDialog setdlg = null;

    public void start(String[] args) {
        createGUI();
    }

    /**
     * Contruct a MegaMek, and display the main menu in the specified frame.
     */
    private void createGUI() {
        frame = new Frame("MegaMek"); //$NON-NLS-1$
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });

        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);

        List<Image> iconList = new ArrayList<Image>();
        iconList.add(frame.getToolkit().getImage(new File(Configuration.miscImagesDir(), FILENAME_ICON_16X16).toString()));
        iconList.add(frame.getToolkit().getImage(new File(Configuration.miscImagesDir(), FILENAME_ICON_32X32).toString()));
        iconList.add(frame.getToolkit().getImage(new File(Configuration.miscImagesDir(), FILENAME_ICON_48X48).toString()));
        iconList.add(frame.getToolkit().getImage(new File(Configuration.miscImagesDir(), FILENAME_ICON_256X256).toString()));
        frame.setIconImages(iconList);

        CommonMenuBar menuBar = new CommonMenuBar();
        menuBar.addActionListener(actionListener);
        frame.setMenuBar(menuBar);
        showMainMenu();

        // set visible on middle of screen
        frame.pack();
        frame.setLocationRelativeTo(null);
        // init the cache
        MechSummaryCache.getInstance();

        // Show the window.
        frame.setVisible(true);

        // tell the user about the readme...
        if (GUIPreferences.getInstance().getNagForReadme()) {
            ConfirmDialog confirm = new ConfirmDialog(frame, Messages
                                                                     .getString("MegaMek.welcome.title") + MegaMek.VERSION, //$NON-NLS-1$
                                                      Messages.getString("MegaMek.welcome.message"), //$NON-NLS-1$
                                                      true);
            confirm.setVisible(true);

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

        labVersion
                .setText(Messages.getString("MegaMek.Version") + MegaMek.VERSION); //$NON-NLS-1$

        hostB = new Button(Messages.getString("MegaMek.hostNewGame.label")); //$NON-NLS-1$
        hostB.setActionCommand("fileGameNew"); //$NON-NLS-1$
        hostB.addActionListener(actionListener);

        scenB = new Button(Messages.getString("MegaMek.hostScenario.label")); //$NON-NLS-1$
        scenB.setActionCommand("fileGameScenario"); //$NON-NLS-1$
        scenB.addActionListener(actionListener);

        loadB = new Button(Messages.getString("MegaMek.hostSavedGame.label")); //$NON-NLS-1$
        loadB.setActionCommand("fileGameOpen"); //$NON-NLS-1$
        loadB.addActionListener(actionListener);

        connectB = new Button(Messages.getString("MegaMek.Connect.label")); //$NON-NLS-1$
        connectB.setActionCommand("fileGameConnect"); //$NON-NLS-1$
        connectB.addActionListener(actionListener);

        botB = new Button(Messages.getString("MegaMek.ConnectAsBot.label")); //$NON-NLS-1$
        botB.setActionCommand("fileGameConnectBot"); //$NON-NLS-1$
        botB.addActionListener(actionListener);

        editB = new Button(Messages.getString("MegaMek.MapEditor.label")); //$NON-NLS-1$
        editB.setActionCommand("fileBoardNew"); //$NON-NLS-1$
        editB.addActionListener(actionListener);

        quitB = new Button(Messages.getString("MegaMek.Quit.label")); //$NON-NLS-1$
        quitB.setActionCommand("quit"); //$NON-NLS-1$
        quitB.addActionListener(actionListener);

        // initialize splash image
        Image imgSplash = frame.getToolkit().getImage(
                new File(Configuration.miscImagesDir(), FILENAME_MEGAMEK_SPLASH).toString()
                                                     );

        // wait for splash image to load completely
        MediaTracker tracker = new MediaTracker(frame);
        tracker.addImage(imgSplash, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException e) {
            //should never get here
        }
        // make splash image panel
        BufferedPanel panTitle = new BufferedPanel();
        BackGroundDrawer bgdTitle = new BackGroundDrawer(imgSplash);
        panTitle.addBgDrawer(bgdTitle);
        panTitle.setPreferredSize(imgSplash.getWidth(null), imgSplash
                .getHeight(null));

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
        options.loadOptions();
        if (optdlg == null) {
            optdlg = new GameOptionsDialog(frame, options);
        }
        optdlg.update(options);
        optdlg.setVisible(true);
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
        hd.setVisible(true);
        // verify dialog data
        if ((hd.name == null) || (hd.serverPass == null) || (hd.port == 0)) {
            return;
        }

        // Players should have to enter a non-blank, non-whitespace name.
        boolean foundValid = false;
        char[] nameChars = hd.name.toCharArray();
        for (int loop = 0; !foundValid && (loop < nameChars.length); loop++) {
            if (!Character.isWhitespace(nameChars[loop])) {
                foundValid = true;
            }
        }
        if (!foundValid) {
            new AlertDialog(
                    frame,
                    Messages.getString("MegaMek.PlayerNameAlert.title"), Messages.getString("MegaMek.PlayerNameAlert.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // kick off a RNG check
        megamek.common.Compute.d6();
        // start server
        try {
            server = new Server(hd.serverPass, hd.port, hd.register, hd.register ? hd.metaserver : "");
        } catch (IOException ex) {
            System.err.println("could not create server socket on port "
                               + hd.port);
            StringBuffer error = new StringBuffer();
            error.append("Error: could not start server at localhost").append(
                    ":").append(hd.port).append(" (").append(ex.getMessage())
                 .append(").");
            new AlertDialog(
                    frame,
                    Messages.getString("MegaMek.HostGameAlert.title"), error.toString()).setVisible(true); //$NON-NLS-1$
            return;
        }
        // initialize client
        client = new Client(hd.name, "localhost", hd.port); //$NON-NLS-1$
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();

        if (!client.connect()) {
            StringBuffer error = new StringBuffer();
            error.append("Error: could not connect to server at localhost")
                 .append(":").append(hd.port).append(".");
            new AlertDialog(
                    frame,
                    Messages.getString("MegaMek.HostGameAlert.title"), error.toString()).setVisible(true); //$NON-NLS-1$
            frame.setVisible(false);
            client.die();
        }

        launch(gui.getFrame());

        optdlg = null;
    }

    public void loadGame() {
        FileDialog fd = new FileDialog(frame, Messages
                .getString("MegaMek.SaveGameDialog.title"), FileDialog.LOAD); //$NON-NLS-1$
        fd.setDirectory("savegames"); //$NON-NLS-1$
        // limit file-list to savedgames only
        fd.setFilenameFilter(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return ((null != name) && name.endsWith(".sav")); //$NON-NLS-1$
            }
        });
        // Using the FilenameFilter class would be the appropriate way to
        // filter for certain extensions, but it's broken under windoze. See
        // http://developer.java.sun.com/developer/bugParade/bugs/4031440.html
        // for details. The hack below is better than nothing.
        // New note: Since we have a dedicated save dir now, I'm commenting
        // this out.
        // fd.setFile("*.sav"); //$NON-NLS-1$

        fd.setVisible(true);
        if (fd.getFile() == null) {
            return;
        }

        HostDialog hd = new HostDialog(frame);
        hd.setVisible(true);
        if ((hd.name == null) || (hd.serverPass == null) || (hd.port == 0)) {
            return;
        }

        // Players should have to enter a non-blank, non-whitespace name.
        boolean foundValid = false;
        char[] nameChars = hd.name.toCharArray();
        for (int loop = 0; !foundValid && (loop < nameChars.length); loop++) {
            if (!Character.isWhitespace(nameChars[loop])) {
                foundValid = true;
            }
        }
        if (!foundValid) {
            new AlertDialog(
                    frame,
                    Messages.getString("MegaMek.PlayerNameAlert1.title"), Messages.getString("MegaMek.PlayerNameAlert1.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // kick off a RNG check
        megamek.common.Compute.d6();
        // start server
        try {
            server = new Server(hd.serverPass, hd.port);
        } catch (IOException ex) {
            System.err.println("could not create server socket on port "
                               + hd.port);
            StringBuffer error = new StringBuffer();
            error.append("Error: could not start server at localhost").append(
                    ":").append(hd.port).append(" (").append(ex.getMessage())
                 .append(").");
            new AlertDialog(
                    frame,
                    Messages.getString("MegaMek.HostGameAlert.title"), error.toString()).setVisible(true); //$NON-NLS-1$
            return;
        }
        if (!server.loadGame(new File(fd.getDirectory(), fd.getFile()))) {
            new AlertDialog(
                    frame,
                    Messages.getString("MegaMek.LoadGameAlert.title"), Messages.getString("MegaMek.LoadGameAlert.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            server.die();
            server = null;
            return;
        }
        client = new Client(hd.name, "localhost", hd.port); //$NON-NLS-1$
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();

        if (!client.connect()) {
            StringBuffer error = new StringBuffer();
            error.append("Error: could not connect to server at localhost")
                 .append(":").append(hd.port).append(".");
            new AlertDialog(
                    frame,
                    Messages.getString("MegaMek.HostGameAlert.title"), error.toString()).setVisible(true); //$NON-NLS-1$
            frame.setVisible(false);
            client.die();
        }

        optdlg = null;
        launch(gui.getFrame());
    }

    /**
     * Host a game constructed from a scenario file
     */
    public void scenario() {
        FileDialog fd = new FileDialog(
                frame,
                Messages.getString("MegaMek.SelectScenarioDialog.title"), FileDialog.LOAD); //$NON-NLS-1$
        fd.setDirectory("data" + File.separatorChar + "scenarios"); //$NON-NLS-1$ //$NON-NLS-2$

        // the filter doesn't seem to do anything in windows. oh well
        FilenameFilter ff = new FilenameFilter() {
            public boolean accept(File f, String s) {
                return s.endsWith(".mms"); //$NON-NLS-1$
            }
        };
        fd.setFilenameFilter(ff);
        fd.setFile("*.mms"); // see comment above for load game dialog
        // //$NON-NLS-1$
        fd.setVisible(true);
        if (fd.getFile() == null) {
            return;
        }
        ScenarioLoader sl = new ScenarioLoader(new File(fd.getDirectory(), fd
                .getFile()));
        IGame g = null;
        try {
            g = sl.createGame();
        } catch (Exception e) {
            new AlertDialog(
                    frame,
                    Messages.getString("MegaMek.HostScenarioAlert.title"), Messages.getString("MegaMek.HostScenarioAlert.message") + e.getMessage()).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // popup options dialog
        GameOptionsDialog god = new GameOptionsDialog(frame, g.getOptions());
        god.update(g.getOptions());
        god.setEditable(true);
        god.setVisible(true);
        for (IBasicOption opt : god.getOptions()) {
            IOption orig = g.getOptions().getOption(opt.getName());
            orig.setValue(opt.getValue());
        }
        god = null;

        // get player types and colors set
        Player[] pa = new Player[g.getPlayersVector().size()];
        g.getPlayersVector().copyInto(pa);

        ScenarioDialog sd = new ScenarioDialog(frame, pa);
        sd.setVisible(true);
        if (!sd.bSet) {
            return;
        }

        // host with the scenario. essentially copied from host()
        HostDialog hd = new HostDialog(frame);
        boolean hasSlot = false;
        if (!(sd.localName.equals(""))) {
            hasSlot = true;
        }
        hd.yourNameF.setText(sd.localName);
        hd.setVisible(true);
        // verify dialog data
        if ((hd.name == null) || (hd.serverPass == null) || (hd.port == 0)) {
            return;
        }
        sd.localName = hd.name;

        // Players should have to enter a non-blank, non-whitespace name.
        boolean foundValid = false;
        char[] nameChars = hd.name.toCharArray();
        for (int loop = 0; !foundValid && (loop < nameChars.length); loop++) {
            if (!Character.isWhitespace(nameChars[loop])) {
                foundValid = true;
            }
        }
        if (!foundValid) {
            new AlertDialog(
                    frame,
                    Messages.getString("MegaMek.HostScenarioAlert1.title"), Messages.getString("MegaMek.HostScenarioAlert1.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // kick off a RNG check
        megamek.common.Compute.d6();
        // start server
        try {
            server = new Server(hd.serverPass, hd.port);
        } catch (IOException ex) {
            System.err.println("could not create server socket on port "
                               + hd.port);
            StringBuffer error = new StringBuffer();
            error.append("Error: could not start server at localhost").append(
                    ":").append(hd.port).append(" (").append(ex.getMessage())
                 .append(").");
            new AlertDialog(
                    frame,
                    Messages.getString("MegaMek.HostGameAlert.title"), error.toString()).setVisible(true); //$NON-NLS-1$
            return;
        }
        server.setGame(g);

        // apply any scenario damage
        sl.applyDamage(server);
        ClientGUI gui = null;
        if (sd.localName != "") { //$NON-NLS-1$
            // initialize game
            client = new Client(hd.name, "localhost", hd.port); //$NON-NLS-1$
            gui = new ClientGUI(client);
            gui.initialize();

            if (!client.connect()) {
                StringBuffer error = new StringBuffer();
                error.append("Error: could not connect to server at localhost")
                     .append(":").append(hd.port).append(".");
                new AlertDialog(
                        frame,
                        Messages.getString("MegaMek.HostGameAlert.title"), error.toString()).setVisible(true); //$NON-NLS-1$
                frame.setVisible(false);
                client.die();
            }
        }
        optdlg = null;

        // calculate initial BV
        server.calculatePlayerBVs();

        // setup any bots
        for (int x = 0; x < pa.length; x++) {
            if (sd.playerTypes[x] == ScenarioDialog.T_BOT) {
                BotClient c = new TestBot(pa[x].getName(), "localhost", hd.port); //$NON-NLS-1$
                c.game.addGameListener(new BotGUI(c));

                if (!c.connect()) {
                    //bots should never fail
                }

                c.retrieveServerInfo();
            }
        }

        // If he didn't have a name when hasSlot was set, then the host should
        // be an observer.
        if (!hasSlot) {
            Enumeration<IPlayer> pE = server.getGame().getPlayers();
            while (pE.hasMoreElements()) {
                IPlayer tmpP = pE.nextElement();
                if (tmpP.getName().equals(sd.localName)) {
                    tmpP.setObserver(true);
                }
            }
        }

        if (gui != null) {
            launch(gui.getFrame());
        }
    }

    /**
     * Connect to to a game and then launch the chat lounge.
     */
    public void connect() {
        ConnectDialog cd;

        cd = new ConnectDialog(frame);
        cd.setVisible(true);
        // verify dialog data
        if ((cd.name == null) || (cd.serverAddr == null) || (cd.port == 0)) {
            return;
        }

        // Players should have to enter a non-blank, non-whitespace name.
        boolean foundValid = false;
        char[] nameChars = cd.name.toCharArray();
        for (int loop = 0; !foundValid && (loop < nameChars.length); loop++) {
            if (!Character.isWhitespace(nameChars[loop])) {
                foundValid = true;
            }
        }
        if (!foundValid) {
            new AlertDialog(
                    frame,
                    Messages.getString("MegaMek.ConnectAlert.title"), Messages.getString("MegaMek.ConnectAlert.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // initialize game
        client = new Client(cd.name, cd.serverAddr, cd.port);
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();
        if (!client.connect()) {
            StringBuffer error = new StringBuffer();
            error.append("Error: could not connect to server at ").append(
                    cd.serverAddr).append(":").append(cd.port).append(".");
            new AlertDialog(
                    frame,
                    Messages.getString("MegaMek.HostGameAlert.title"), error.toString()).setVisible(true); //$NON-NLS-1$
            frame.setVisible(false);
            client.die();
        }
        launch(gui.getFrame());
    }

    public void connectBot() {
        ConnectDialog cd;

        cd = new ConnectDialog(frame);
        cd.setVisible(true);
        // verify dialog data
        if ((cd.name == null) || (cd.serverAddr == null) || (cd.port == 0)) {
            return;
        }

        // Players should have to enter a non-blank, non-whitespace name.
        boolean foundValid = false;
        char[] nameChars = cd.name.toCharArray();
        for (int loop = 0; !foundValid && (loop < nameChars.length); loop++) {
            if (!Character.isWhitespace(nameChars[loop])) {
                foundValid = true;
            }
        }
        if (!foundValid) {
            new AlertDialog(
                    frame,
                    Messages.getString("MegaMek.ConnectGameAlert.title"), Messages.getString("MegaMek.ConnectGameAlert.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // initialize game
        client = new TestBot(cd.name, cd.serverAddr, cd.port);
        client.game.addGameListener(new BotGUI((BotClient) client));
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();
        if (!client.connect()) {
            StringBuffer error = new StringBuffer();
            error.append("Error: could not connect to server at ").append(
                    cd.serverAddr).append(":").append(cd.port).append(".");
            new AlertDialog(
                    frame,
                    Messages.getString("MegaMek.HostGameAlert.title"), error.toString()).setVisible(true); //$NON-NLS-1$
            frame.setVisible(false);
            client.die();
        }
        launch(gui.getFrame());
        client.retrieveServerInfo();
    }

    private void addBag(Component comp, GridBagLayout gridbag,
                        GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        frame.add(comp);
    }

    /**
     * Called when the user selects the "Help->About" menu item.
     */
    void showAbout() {
        // Do we need to create the "about" dialog?
        if (about == null) {
            about = new CommonAboutDialog(frame);
        }

        // Show the about dialog.
        about.setVisible(true);
    }

    /**
     * Called when the user selects the "Help->Contents" menu item.
     */
    void showHelp() {
        if (help == null) {
            help = showHelp(frame, "readme"); //$NON-NLS-1$
        }
        // Show the help dialog.
        help.setVisible(true);
    }

    /**
     * display the filename in a CommonHelpDialog
     */
    private static CommonHelpDialog showHelp(Frame frame, String filename) {
        Locale l = Locale.getDefault();
        File helpfile = new File(filename
                                 + "-" + l.getDisplayLanguage(Locale.ENGLISH) + ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        if (!helpfile.exists()) {
            helpfile = new File(filename + ".txt"); //$NON-NLS-1$
        }
        return new CommonHelpDialog(frame, helpfile);
    }

    /**
     * Called when the user selects the "View->Client Settings" menu item.
     */
    void showSettings() {
        // Do we need to create the "settings" dialog?
        if (setdlg == null) {
            setdlg = new CommonSettingsDialog(frame);
        }

        // Show the settings dialog.
        setdlg.setVisible(true);
    }

    /**
     * Called when the quit buttons is pressed or the main menu is closed.
     */
    void quit() {
        PreferenceManager.getInstance().save();
        System.exit(0);
    }

    /**
     * Hides this window for later. Listens to the frame until it closes, then
     * calls unlaunch().
     */
    private void launch(Frame launched) {
        // listen to new frame
        launched.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                unlaunch();
            }

            @Override
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
    void unlaunch() {
        // clean up server, if we have one
        if (server != null) {
            server.die();
            server = null;
        }
        // show menu frame
        frame.setVisible(true);

        // just to free some memory
        client = null;
        System.gc();
        System.runFinalization();
    }

    protected ActionListener actionListener = new ActionListener() {
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
    };
}