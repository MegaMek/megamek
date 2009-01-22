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
package megamek.client.ui.swing;

import static megamek.common.Compute.d6;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileFilter;

import megamek.MegaMek;
import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.TestBot;
import megamek.client.bot.ui.swing.BotGUI;
import megamek.client.ui.IMegaMekGUI;
import megamek.client.ui.Messages;
import megamek.common.IGame;
import megamek.common.MechSummaryCache;
import megamek.common.Player;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.preference.PreferenceManager;
import megamek.server.ScenarioLoader;
import megamek.server.Server;

public class MegaMekGUI implements IMegaMekGUI {
    private JFrame frame;
    private Client client;
    private Server server;
    private CommonAboutDialog about;
    private CommonHelpDialog help;
    private GameOptionsDialog optdlg;
    private CommonSettingsDialog setdlg;

    public MegaMekGUI() {
    }

    public void start(String[] args) {
        createGUI();
    }

    /**
     * Contruct a MegaMek, and display the main menu in the specified frame.
     */
    private void createGUI() {
        //Set a couple of things to make the Swing GUI look more "Mac-like" on Macs
        //Taken from: http://www.devdaily.com/apple/mac/java-mac-native-look/Introduction.shtml
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "MegaMek");
        
        //this should also help to make MegaMek look more system-specific
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        ToolTipManager.sharedInstance().setInitialDelay(
                GUIPreferences.getInstance().getTooltipDelay());
        frame = new JFrame("MegaMek"); //$NON-NLS-1$
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });
        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);
        frame.setIconImage(frame.getToolkit().getImage(
                "data/images/misc/megamek-icon.gif")); //$NON-NLS-1$
        CommonMenuBar menuBar = new CommonMenuBar();
        menuBar.addActionListener(actionListener);
        frame.setJMenuBar(menuBar);
        showMainMenu();

        // set visible on middle of screen
        Dimension screenSize = frame.getToolkit().getScreenSize();
        frame.pack();
        frame.setLocation(screenSize.width / 2 - frame.getSize().width / 2,
                screenSize.height / 2 - frame.getSize().height / 2);

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
    private void showMainMenu() {
        JButton hostB;
        JButton connectB;
        JButton botB;
        JButton editB;
        JButton scenB;
        JButton loadB;
        JButton quitB;
        JLabel labVersion = new JLabel();
        labVersion
                .setText(Messages.getString("MegaMek.Version") + MegaMek.VERSION); //$NON-NLS-1$
        hostB = new JButton(Messages.getString("MegaMek.hostNewGame.label")); //$NON-NLS-1$
        hostB.setActionCommand("fileGameNew"); //$NON-NLS-1$
        hostB.addActionListener(actionListener);
        scenB = new JButton(Messages.getString("MegaMek.hostScenario.label")); //$NON-NLS-1$
        scenB.setActionCommand("fileGameScenario"); //$NON-NLS-1$
        scenB.addActionListener(actionListener);
        loadB = new JButton(Messages.getString("MegaMek.hostSavedGame.label")); //$NON-NLS-1$
        loadB.setActionCommand("fileGameOpen"); //$NON-NLS-1$
        loadB.addActionListener(actionListener);
        connectB = new JButton(Messages.getString("MegaMek.Connect.label")); //$NON-NLS-1$
        connectB.setActionCommand("fileGameConnect"); //$NON-NLS-1$
        connectB.addActionListener(actionListener);
        botB = new JButton(Messages.getString("MegaMek.ConnectAsBot.label")); //$NON-NLS-1$
        botB.setActionCommand("fileGameConnectBot"); //$NON-NLS-1$
        botB.addActionListener(actionListener);
        editB = new JButton(Messages.getString("MegaMek.MapEditor.label")); //$NON-NLS-1$
        editB.setActionCommand("fileBoardNew"); //$NON-NLS-1$
        editB.addActionListener(actionListener);
        quitB = new JButton(Messages.getString("MegaMek.Quit.label")); //$NON-NLS-1$
        quitB.setActionCommand("quit"); //$NON-NLS-1$
        quitB.addActionListener(actionListener);

        // initialize splash image
        Image imgSplash = frame.getToolkit().getImage(
                "data/images/misc/megamek-splash.jpg"); //$NON-NLS-1$

        // wait for splash image to load completely
        MediaTracker tracker = new MediaTracker(frame);
        tracker.addImage(imgSplash, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException e) {
            // really should never come here
        }
        // make splash image panel
        ImageIcon icon = new ImageIcon(imgSplash);
        JLabel panTitle = new JLabel(icon);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        frame.getContentPane().setLayout(gridbag);
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
    private void showGameOptions() {
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
    private void showEditor() {
        BoardEditor editor = new BoardEditor();
        launch(editor.getFrame());
        editor.boardNew();
    }

    /**
     * Display the board editor and open an "open" dialog.
     */
    private void showEditorOpen() {
        BoardEditor editor = new BoardEditor();
        launch(editor.getFrame());
        editor.boardLoad();
    }

    /**
     * Start instances of both the client and the server.
     */
    private void host() {
        HostDialog hd;
        hd = new HostDialog(frame);
        hd.setVisible(true);
        // verify dialog data
        if (hd.playerName == null || hd.serverPass == null || hd.port == 0) {
            return;
        }

        /***********************************************************************
         * WORK IN PROGRESS // Register the game, if appropriate. String
         * metaserver = null; if (hd.register) { StringBuffer buff = new
         * StringBuffer (hd.metaserver); buff.append ("?action=register")
         * .append ("&port=") .append (hd.port) .append ("&owner=") .append
         * (hd.name) .append ("&goalPlayers=") .append (hd.goalPlayers) .append
         * ("&version=") .append (MegaMek.VERSION) ; metaserver =
         * buff.toString(); try { URL metaURL = new URL (metaserver);
         * BufferedReader reader = new BufferedReader (new InputStreamReader
         * (metaURL.openStream())); String line = reader.readLine(); while (null !=
         * line) { System.out.println (line); line = reader.readLine(); } }
         * catch (Exception except) { except.printStackTrace(); } } /* WORK IN
         * PROGRESS
         **********************************************************************/

        // Players should have to enter a non-blank, non-whitespace name.
        boolean foundValid = false;
        char[] nameChars = hd.playerName.toCharArray();
        for (int loop = 0; !foundValid && loop < nameChars.length; loop++) {
            if (!Character.isWhitespace(nameChars[loop])) {
                foundValid = true;
            }
        }
        if (!foundValid) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages
                                    .getString("MegaMek.PlayerNameAlert.message"), Messages.getString("MegaMek.PlayerNameAlert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // kick off a RNG check
        d6();
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
            JOptionPane
                    .showMessageDialog(
                            frame,
                            error.toString(),
                            Messages.getString("MegaMek.HostGameAllert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            return;
        }
        // initialize client
        client = new Client(hd.playerName, "localhost", hd.port); //$NON-NLS-1$
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();
        if (!client.connect()) {
            StringBuffer error = new StringBuffer();
            error.append("Error: could not connect to server at localhost")
                    .append(":").append(hd.port).append(".");
            JOptionPane
                    .showMessageDialog(
                            frame,
                            error.toString(),
                            Messages.getString("MegaMek.HostGameAllert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            frame.setVisible(false);
            client.die();
        }
        launch(gui.getFrame());

        optdlg = null;
    }

    private void loadGame() {
        JFileChooser fc = new JFileChooser("savegames");
        fc
                .setLocation(frame.getLocation().x + 150,
                        frame.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("MegaMek.SaveGameDialog.title"));
        fc.setFileFilter(new FileFilter() {
            public boolean accept(File dir) {
                return (dir.getName() != null && dir.getName().endsWith(".sav")); //$NON-NLS-1$
            }

            public String getDescription() {
                return ".sav";
            }
        });
        int returnVal = fc.showOpenDialog(frame);
        if (returnVal != JFileChooser.APPROVE_OPTION
                || fc.getSelectedFile() == null) {
            // I want a file, y'know!
            return;
        }
        HostDialog hd = new HostDialog(frame);
        hd.setVisible(true);
        if (hd.playerName == null || hd.serverPass == null || hd.port == 0) {
            return;
        }

        // Players should have to enter a non-blank, non-whitespace name.
        boolean foundValid = false;
        char[] nameChars = hd.playerName.toCharArray();
        for (int loop = 0; !foundValid && loop < nameChars.length; loop++) {
            if (!Character.isWhitespace(nameChars[loop])) {
                foundValid = true;
            }
        }
        if (!foundValid) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages
                                    .getString("MegaMek.PlayerNameAlert1.message"), Messages.getString("MegaMek.PlayerNameAlert1.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // kick off a RNG check
        d6();
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
            JOptionPane
                    .showMessageDialog(
                            frame,
                            error.toString(),
                            Messages.getString("MegaMek.HostGameAllert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            return;
        }
        if (!server.loadGame(fc.getSelectedFile())) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages.getString("MegaMek.LoadGameAlert.message"), Messages.getString("MegaMek.LoadGameAlert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
            server = null;
            return;
        }
        client = new Client(hd.playerName, "localhost", hd.port); //$NON-NLS-1$
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();
        if (!client.connect()) {
            StringBuffer error = new StringBuffer();
            error.append("Error: could not connect to server at localhost")
                    .append(":").append(hd.port).append(".");
            JOptionPane
                    .showMessageDialog(
                            frame,
                            error.toString(),
                            Messages.getString("MegaMek.HostGameAllert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            frame.setVisible(false);
            client.die();
        }
        optdlg = null;
        launch(gui.getFrame());
    }

    /**
     * Host a game constructed from a scenario file
     */
    private void scenario() {
        JFileChooser fc = new JFileChooser("data" + File.separatorChar
                + "scenarios");
        fc
                .setLocation(frame.getLocation().x + 150,
                        frame.getLocation().y + 100);
        fc.setDialogTitle(Messages
                .getString("MegaMek.SelectScenarioDialog.title"));
        fc.setFileFilter(new FileFilter() {
            public boolean accept(File dir) {
                return (dir.getName() != null && dir.getName().endsWith(".mms")); //$NON-NLS-1$
            }

            public String getDescription() {
                return ".mms";
            }
        });
        int returnVal = fc.showOpenDialog(frame);
        if (returnVal != JFileChooser.APPROVE_OPTION
                || fc.getSelectedFile() == null) {
            // I want a file, y'know!
            return;
        }

        ScenarioLoader sl = new ScenarioLoader(fc.getSelectedFile());
        IGame g;
        try {
            g = sl.createGame();
        } catch (Exception e) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages
                                    .getString("MegaMek.HostScenarioAllert.message") + e.getMessage(), Messages.getString("MegaMek.HostScenarioAllert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
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
        if (!("".equals(sd.localName)))
            hasSlot = true;
        hd.yourNameF.setText(sd.localName);
        hd.setVisible(true);
        // verify dialog data
        if (hd.playerName == null || hd.serverPass == null || hd.port == 0) {
            return;
        }
        sd.localName = hd.playerName;

        // Players should have to enter a non-blank, non-whitespace name.
        boolean foundValid = false;
        char[] nameChars = hd.playerName.toCharArray();
        for (int loop = 0; !foundValid && loop < nameChars.length; loop++) {
            if (!Character.isWhitespace(nameChars[loop])) {
                foundValid = true;
            }
        }
        if (!foundValid) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages
                                    .getString("MegaMek.HostScenarioAllert1.message"), Messages.getString("MegaMek.HostScenarioAllert1.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // kick off a RNG check
        d6();
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
            JOptionPane
                    .showMessageDialog(
                            frame,
                            error.toString(),
                            Messages.getString("MegaMek.HostGameAllert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            return;
        }
        server.setGame(g);

        // apply any scenario damage
        sl.applyDamage(server);
        ClientGUI gui = null;
        if (!"".equals(sd.localName)) { //$NON-NLS-1$
            // initialize game
            client = new Client(hd.playerName, "localhost", hd.port); //$NON-NLS-1$
            gui = new ClientGUI(client);
            gui.initialize();
            if (!client.connect()) {
                StringBuffer error = new StringBuffer();
                error.append("Error: could not connect to server at localhost")
                        .append(":").append(hd.port).append(".");
                JOptionPane
                        .showMessageDialog(
                                frame,
                                error.toString(),
                                Messages
                                        .getString("MegaMek.HostScenarioAllert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
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
                }
                c.retrieveServerInfo();
            }
        }

        // If he didn't have a name when hasSlot was set, then the host should
        // be an observer.
        if (!hasSlot) {
            Enumeration<Player> pE = server.getGame().getPlayers();
            while (pE.hasMoreElements()) {
                Player tmpP = pE.nextElement();
                if (tmpP.getName().equals(sd.localName))
                    tmpP.setObserver(true);
            }
        }
        launch(gui.getFrame());
    }

    /**
     * Connect to to a game and then launch the chat lounge.
     */
    private void connect() {
        ConnectDialog cd;
        cd = new ConnectDialog(frame);
        cd.setVisible(true);
        // verify dialog data
        if (cd.playerName == null || cd.serverAddr == null || cd.port == 0) {
            return;
        }

        // Players should have to enter a non-blank, non-whitespace name.
        boolean foundValid = false;
        char[] nameChars = cd.playerName.toCharArray();
        for (int loop = 0; !foundValid && loop < nameChars.length; loop++) {
            if (!Character.isWhitespace(nameChars[loop])) {
                foundValid = true;
            }
        }
        if (!foundValid) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages.getString("MegaMek.ConnectAllert.message"), Messages.getString("MegaMek.ConnectAllert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // initialize game
        client = new Client(cd.playerName, cd.serverAddr, cd.port);
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();
        if (!client.connect()) {
            StringBuffer error = new StringBuffer();
            error.append("Error: could not connect to server at ").append(
                    cd.serverAddr).append(':').append(cd.port).append('.');
            JOptionPane
                    .showMessageDialog(
                            frame,
                            error.toString(),
                            Messages.getString("MegaMek.ConnectAllert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            frame.setVisible(false);
            client.die();
        }
        launch(gui.getFrame());
    }

    private void connectBot() {
        ConnectDialog cd;
        cd = new ConnectDialog(frame);
        cd.setVisible(true);
        // verify dialog data
        if (cd.playerName == null || cd.serverAddr == null || cd.port == 0) {
            return;
        }

        // Players should have to enter a non-blank, non-whitespace name.
        boolean foundValid = false;
        char[] nameChars = cd.playerName.toCharArray();
        for (int loop = 0; !foundValid && loop < nameChars.length; loop++) {
            if (!Character.isWhitespace(nameChars[loop])) {
                foundValid = true;
            }
        }
        if (!foundValid) {
            JOptionPane
                    .showMessageDialog(
                            frame,
                            Messages
                                    .getString("MegaMek.ConnectGameAllert.message"), Messages.getString("MegaMek.ConnectGameAllert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // initialize game
        client = new TestBot(cd.playerName, cd.serverAddr, cd.port);
        client.game.addGameListener(new BotGUI((BotClient) client));
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();
        if (!client.connect()) {
            StringBuffer error = new StringBuffer();
            error.append("Error: could not connect to server at ").append(
                    cd.serverAddr).append(':').append(cd.port).append('.');
            JOptionPane
                    .showMessageDialog(
                            frame,
                            error.toString(),
                            Messages.getString("MegaMek.ConnectAllert.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            frame.setVisible(false);
            client.die();
        }
        launch(gui.getFrame());
        client.retrieveServerInfo();
    }

    private void addBag(JComponent comp, GridBagLayout gridbag,
            GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        frame.getContentPane().add(comp);
    }

    /**
     * Called when the user selects the "Help->About" menu item.
     */
    private void showAbout() {
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
    private void showHelp() {
        if (help == null) {
            help = showHelp(frame, "readme"); //$NON-NLS-1$
        }
        // Show the help dialog.
        help.setVisible(true);
    }

    /**
     * display the filename in a CommonHelpDialog
     */
    private static CommonHelpDialog showHelp(JFrame frame, String filename) {
        Locale l = Locale.getDefault();
        File helpfile = new File(filename + '-'
                + l.getDisplayLanguage(Locale.ENGLISH) + ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
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
        if (setdlg == null) {
            setdlg = new CommonSettingsDialog(frame);
        }

        // Show the settings dialog.
        setdlg.setVisible(true);
    }

    /**
     * Called when the quit buttons is pressed or the main menu is closed.
     */
    private static void quit() {
        PreferenceManager.getInstance().save();
        System.exit(0);
    }

    /**
     * Hides this window for later. Listens to the frame until it closes, then
     * calls unlaunch().
     */
    private void launch(JFrame launched) {
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

        // just to free some memory
        client = null;
        System.gc();
        System.runFinalization();
    }

    private ActionListener actionListener = new ActionListener() {
        //
        // ActionListener
        //
        public void actionPerformed(ActionEvent ev) {
            if ("fileBoardNew".equalsIgnoreCase(ev.getActionCommand())) { //$NON-NLS-1$
                showEditor();
            }
            if ("fileBoardOpen".equalsIgnoreCase(ev.getActionCommand())) { //$NON-NLS-1$
                showEditorOpen();
            }
            if ("fileGameNew".equalsIgnoreCase(ev.getActionCommand())) { //$NON-NLS-1$
                host();
            }
            if ("fileGameScenario".equalsIgnoreCase(ev.getActionCommand())) { //$NON-NLS-1$
                scenario();
            }
            if ("fileGameConnect".equalsIgnoreCase(ev.getActionCommand())) { //$NON-NLS-1$
                connect();
            }
            if ("fileGameConnectBot".equalsIgnoreCase(ev.getActionCommand())) { //$NON-NLS-1$
                connectBot();
            }
            if ("fileGameOpen".equalsIgnoreCase(ev.getActionCommand())) { //$NON-NLS-1$
                loadGame();
            }
            if ("viewGameOptions".equalsIgnoreCase(ev.getActionCommand())) { //$NON-NLS-1$
                showGameOptions();
            }
            if ("helpAbout".equalsIgnoreCase(ev.getActionCommand())) { //$NON-NLS-1$
                showAbout();
            }
            if ("helpContents".equalsIgnoreCase(ev.getActionCommand())) { //$NON-NLS-1$
                showHelp();
            }
            if ("viewClientSettings".equalsIgnoreCase(ev.getActionCommand())) { //$NON-NLS-1$
                showSettings();
            }
            if ("quit".equalsIgnoreCase(ev.getActionCommand())) { //$NON-NLS-1$
                quit();
            }
        }
    };
}