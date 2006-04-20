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

import megamek.MegaMek;
import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.TestBot;
import megamek.client.bot.ui.swing.BotGUI;
import megamek.client.ui.IMegaMekGUI;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.IGame;
import megamek.common.MechSummaryCache;
import megamek.common.Player;
import megamek.common.options.GameOptions;
import megamek.common.preference.IClientPreferences;
import megamek.common.preference.PreferenceManager;
import megamek.server.ScenarioLoader;
import megamek.server.Server;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Vector;

import static megamek.common.Compute.d6;

public class MegaMekGUI implements IMegaMekGUI {
    public JFrame frame;
    public Client client = null;
    public Server server = null;
    private CommonAboutDialog about = null;
    private CommonHelpDialog help = null;
    private GameOptionsDialog optdlg = null;
    private CommonSettingsDialog setdlg = null;

    public MegaMekGUI() {
    }

    public void start(String[] args) {
        createGUI();
    }

    /**
     * Contruct a MegaMek, and display the main menu in the
     * specified frame.
     */
    private void createGUI() {
        frame = new JFrame("MegaMek"); //$NON-NLS-1$
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });
        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);
        frame.setIconImage(frame.getToolkit().getImage("data/images/misc/megamek-icon.gif")); //$NON-NLS-1$
        CommonMenuBar menuBar = new CommonMenuBar();
        menuBar.addActionListener(actionListener);
        frame.setMenuBar(menuBar);
        showMainMenu();

        // set visible on middle of screen
        Dimension screenSize = frame.getToolkit().getScreenSize();
        frame.pack();
        frame.setLocation(screenSize.width / 2 - frame.getSize().width / 2,
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
        if (GUIPreferences.getInstance().getNagForReadme()) {
            ConfirmDialog confirm = new ConfirmDialog(frame,
                    Messages.getString("MegaMek.welcome.title") + MegaMek.VERSION, //$NON-NLS-1$
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
        JButton hostB, connectB, botB, editB, scenB, loadB, quitB;
        JLabel labVersion = new JLabel();
        labVersion.setText(Messages.getString("MegaMek.Version") + MegaMek.VERSION); //$NON-NLS-1$
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
        Image imgSplash = frame.getToolkit().getImage("data/images/misc/megamek-splash.jpg"); //$NON-NLS-1$

        // wait for splash image to load completely
        MediaTracker tracker = new MediaTracker(frame);
        tracker.addImage(imgSplash, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException e) {
            //really should never come here
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
    public void showGameOptions() {
        GameOptions options = new GameOptions();
        options.initialize();
        options.loadOptions(null);
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
        if (hd.playerName == null || hd.serverPass == null || hd.port == 0) {
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
        char[] nameChars = hd.playerName.toCharArray();
        for (int loop = 0; !foundValid && loop < nameChars.length; loop++) {
            if (!Character.isWhitespace(nameChars[loop])) {
                foundValid = true;
            }
        }
        if (!foundValid) {
            new AlertDialog(frame, Messages.getString("MegaMek.PlayerNameAlert.title"), Messages.getString("MegaMek.PlayerNameAlert.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // kick off a RNG check
        d6();
        // start server
        server = new Server(hd.serverPass, hd.port);
        // initialize client
        client = new Client(hd.playerName, "localhost", hd.port); //$NON-NLS-1$
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();
        if (!client.connect()) {
        }
        launch(gui.getFrame());
        Vector changedOptions = server.getGame().getOptions().loadOptions(hd.serverPass);
        if (changedOptions.size() > 0) {
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

        fd.setVisible(true);
        if (fd.getFile() == null) {
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
            new AlertDialog(frame, Messages.getString("MegaMek.PlayerNameAlert1.title"), Messages.getString("MegaMek.PlayerNameAlert1.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // kick off a RNG check
        d6();
        // start server
        server = new Server(hd.serverPass, hd.port);
        if (!server.loadGame(new File(fd.getDirectory(), fd.getFile()))) {
            new AlertDialog(frame, Messages.getString("MegaMek.LoadGameAlert.title"), Messages.getString("MegaMek.LoadGameAlert.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            server = null;
            return;
        }
        client = new Client(hd.playerName, "localhost", hd.port); //$NON-NLS-1$
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();
        if (!client.connect()) {
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
        fd.setVisible(true);
        if (fd.getFile() == null) {
            return;
        }
        ScenarioLoader sl = new ScenarioLoader(new File(fd.getDirectory(), fd.getFile()));
        IGame g = null;
        try {
            g = sl.createGame();
        } catch (Exception e) {
            new AlertDialog(frame, Messages.getString("MegaMek.HostScenarioAllert.title"), Messages.getString("MegaMek.HostScenarioAllert.message") + e.getMessage()).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // get player types and colors set
        Player[] pa = new Player[g.getPlayersVector().size()];
        g.getPlayersVector().copyInto(pa);
        ScenarioDialog sd = new ScenarioDialog(frame, pa);
        sd.setVisible(true);
        if (!sd.bSet) {
            return;
        }

        // host with the scenario.  essentially copied from host()
        HostDialog hd = new HostDialog(frame);
        boolean hasSlot = false;
        if (!(sd.localName.equals("")))
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
            new AlertDialog(frame, Messages.getString("MegaMek.HostScenarioAllert1.title"), Messages.getString("MegaMek.HostScenarioAllert1.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // kick off a RNG check
        d6();
        // start server
        server = new Server(hd.serverPass, hd.port);
        server.setGame(g);

        // apply any scenario damage
        sl.applyDamage(server);
        ClientGUI gui = null;
        if (!sd.localName.equals("")) { //$NON-NLS-1$
            // initialize game
            client = new Client(hd.playerName, "localhost", hd.port); //$NON-NLS-1$
            gui = new ClientGUI(client);
            gui.initialize();
            if (!client.connect()) {
            }
            Vector changedOptions = server.getGame().getOptions().loadOptions(hd.serverPass);
            if (changedOptions.size() > 0) {
                client.sendGameOptions(hd.serverPass, changedOptions);
            }

            // popup options dialog
            gui.getGameOptionsDialog().update(client.game.getOptions());
            gui.getGameOptionsDialog().setVisible(true);
        }
        optdlg = null;

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

        // If he didn't have a name when hasSlot was set, then the host should be
        // an observer.
        if (!hasSlot) {
            Enumeration pE = server.getGame().getPlayers();
            while (pE.hasMoreElements()) {
                Player tmpP = (Player) pE.nextElement();
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
            new AlertDialog(frame, Messages.getString("MegaMek.ConnectAllert.title"), Messages.getString("MegaMek.ConnectAllert.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // initialize game
        client = new Client(cd.playerName, cd.serverAddr, cd.port);
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();
        if (!client.connect()) {
            StringBuffer error = new StringBuffer();
            error.append("Error: could not connect to server at ").append(cd.serverAddr).append(":").append(cd.port).append(".");
            new AlertDialog(frame, Messages.getString("MegaMek.HostGameAllert.title"), error.toString()).setVisible(true); //$NON-NLS-1$
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
            new AlertDialog(frame, Messages.getString("MegaMek.ConnectGameAllert.title"), Messages.getString("MegaMek.ConnectGameAllert.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }

        // initialize game
        client = new TestBot(cd.playerName, cd.serverAddr, cd.port);
        client.game.addGameListener(new BotGUI((BotClient) client));
        ClientGUI gui = new ClientGUI(client);
        gui.initialize();
        if (!client.connect()) {
            StringBuffer error = new StringBuffer();
            error.append("Error: could not connect to server at ").append(cd.serverAddr).append(":").append(cd.port).append(".");
            new AlertDialog(frame, Messages.getString("MegaMek.HostGameAllert.title"), error.toString()).setVisible(true); //$NON-NLS-1$
            frame.setVisible(false);
            client.die();
        }
        launch(gui.getFrame());
        client.retrieveServerInfo();
    }

    private void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
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
     * Hides this window for later.  Listens to the frame until it closes,
     * then calls unlaunch().
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

        //just to free some memory
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

/**
 * here's a quick class for the host new game diaglogue box
 */
class HostDialog extends JDialog implements ActionListener {
    public String playerName;
    public String serverPass;
    public int port;
    public boolean register;
    public String metaserver;
    public int goalPlayers;
    protected JLabel yourNameL, serverPassL, portL;
    protected JTextField yourNameF, serverPassF, portF;
    protected JCheckBox registerC;
    protected JLabel metaserverL;
    protected JTextField metaserverF;
    protected JLabel goalL;
    protected JTextField goalF;
    protected JButton okayB, cancelB;

    public HostDialog(JFrame frame) {
        super(frame, Messages.getString("MegaMek.HostDialog.title"), true); //$NON-NLS-1$
        yourNameL = new JLabel(Messages.getString("MegaMek.yourNameL"), JLabel.RIGHT); //$NON-NLS-1$
        serverPassL = new JLabel(Messages.getString("MegaMek.serverPassL"), JLabel.RIGHT); //$NON-NLS-1$
        portL = new JLabel(Messages.getString("MegaMek.portL"), JLabel.RIGHT); //$NON-NLS-1$
        yourNameF = new JTextField(PreferenceManager.getClientPreferences().getLastPlayerName(), 16);
        yourNameF.addActionListener(this);
        serverPassF = new JTextField(PreferenceManager.getClientPreferences().getLastServerPass(), 16);
        serverPassF.addActionListener(this);
        portF = new JTextField(PreferenceManager.getClientPreferences().getLastServerPort() + "", 4); //$NON-NLS-1$
        portF.addActionListener(this);
        IClientPreferences cs = PreferenceManager.getClientPreferences();
        metaserver = cs.getMetaServerName();
        metaserverL = new JLabel(Messages.getString("MegaMek.metaserverL"), JLabel.RIGHT); //$NON-NLS-1$
        metaserverF = new JTextField(metaserver);
        metaserverL.setEnabled(register);
        metaserverF.setEnabled(register);
        int goalNumber = cs.getGoalPlayers();
        goalL = new JLabel(Messages.getString("MegaMek.goalL"), JLabel.RIGHT); //$NON-NLS-1$
        goalF = new JTextField(Integer.toString(goalNumber), 2);
        goalL.setEnabled(register);
        goalF.setEnabled(register);
        registerC = new JCheckBox(Messages.getString("MegaMek.registerC")); //$NON-NLS-1$
        register = false;
        registerC.setSelected(register);
        registerC.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                boolean state = false;
                if (ItemEvent.SELECTED == event.getStateChange()) {
                    state = true;
                }
                metaserverL.setEnabled(state);
                metaserverF.setEnabled(state);
                goalL.setEnabled(state);
                goalF.setEnabled(state);
            }
        });
        okayB = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
        okayB.setActionCommand("done"); //$NON-NLS-1$
        okayB.addActionListener(this);
        okayB.setSize(80, 24);
        cancelB = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$
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
        setLocation(frame.getLocation().x + frame.getSize().width / 2 - getSize().width / 2,
                frame.getLocation().y + frame.getSize().height / 2 - getSize().height / 2);
    }

    public void actionPerformed(ActionEvent e) {
        if (!e.getActionCommand().equals("cancel")) { //$NON-NLS-1$
            try {
                playerName = yourNameF.getText();
                serverPass = serverPassF.getText();
                register = registerC.isSelected();
                metaserver = metaserverF.getText();
                port = Integer.parseInt(portF.getText());
                goalPlayers = Integer.parseInt(goalF.getText());
            } catch (NumberFormatException ex) {
                System.err.println(ex.getMessage());
                port = 2346;
                goalPlayers = 2;
            }

            // update settings
            PreferenceManager.getClientPreferences().setLastPlayerName(playerName);
            PreferenceManager.getClientPreferences().setLastServerPass(serverPass);
            PreferenceManager.getClientPreferences().setLastServerPort(port);
            PreferenceManager.getClientPreferences().setValue("megamek.megamek.metaservername", //$NON-NLS-1$
                    metaserver);
            PreferenceManager.getClientPreferences().setValue("megamek.megamek.goalplayers", //$NON-NLS-1$
                    Integer.toString(goalPlayers));
        }
        setVisible(false);
    }
}

/**
 * here's a quick class for the connect to game diaglogue box
 */
class ConnectDialog extends JDialog implements ActionListener {
    public String playerName, serverAddr;
    public int port;
    protected JLabel yourNameL, serverAddrL, portL;
    protected JTextField yourNameF, serverAddrF, portF;
    protected JButton okayB, cancelB;

    public ConnectDialog(JFrame frame) {
        super(frame, Messages.getString("MegaMek.ConnectDialog.title"), true); //$NON-NLS-1$
        yourNameL = new JLabel(Messages.getString("MegaMek.yourNameL"), JLabel.RIGHT); //$NON-NLS-1$
        serverAddrL = new JLabel(Messages.getString("MegaMek.serverAddrL"), JLabel.RIGHT); //$NON-NLS-1$
        portL = new JLabel(Messages.getString("MegaMek.portL"), JLabel.RIGHT); //$NON-NLS-1$
        yourNameF = new JTextField(PreferenceManager.getClientPreferences().getLastPlayerName(), 16);
        yourNameF.addActionListener(this);
        serverAddrF = new JTextField(PreferenceManager.getClientPreferences().getLastConnectAddr(), 16);
        serverAddrF.addActionListener(this);
        portF = new JTextField(PreferenceManager.getClientPreferences().getLastConnectPort() + "", 4); //$NON-NLS-1$
        portF.addActionListener(this);
        okayB = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
        okayB.setActionCommand("done"); //$NON-NLS-1$
        okayB.addActionListener(this);
        okayB.setSize(80, 24);
        cancelB = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$
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
        setLocation(frame.getLocation().x + frame.getSize().width / 2 - getSize().width / 2,
                frame.getLocation().y + frame.getSize().height / 2 - getSize().height / 2);
    }

    public void actionPerformed(ActionEvent e) {
        if (!e.getActionCommand().equals("cancel")) { //$NON-NLS-1$
            try {
                playerName = yourNameF.getText();
                serverAddr = serverAddrF.getText();
                port = Integer.decode(portF.getText().trim()).intValue();
            } catch (NumberFormatException ex) {
                System.err.println(ex.getMessage());
            }

            // update settings
            PreferenceManager.getClientPreferences().setLastPlayerName(playerName);
            PreferenceManager.getClientPreferences().setLastConnectAddr(serverAddr);
            PreferenceManager.getClientPreferences().setLastConnectPort(port);
        }
        setVisible(false);
    }
}

/**
 * Allow a user to set types and colors for scenario players
 */
class ScenarioDialog extends JDialog implements ActionListener {
    public static final int T_ME = 0;
    public static final int T_HUMAN = 1;
    public static final int T_BOT = 2;
    private Player[] m_players;
    private JLabel[] m_labels;
    private Choice[] m_typeChoices;
    private JButton[] m_camoButtons;
    private JFrame m_frame;
    /**
     * The camo selection dialog.
     */
    private CamoChoiceDialog camoDialog;
    private ItemListener prevListener = null;
    public boolean bSet = false;
    public int[] playerTypes;
    public String localName = ""; //$NON-NLS-1$

    public ScenarioDialog(JFrame frame, Player[] pa) {
        super(frame, Messages.getString("MegaMek.ScenarioDialog.title"), true); //$NON-NLS-1$
        m_frame = frame;
        camoDialog = new CamoChoiceDialog(frame);
        m_players = pa;
        m_labels = new JLabel[pa.length];
        m_typeChoices = new Choice[pa.length];
        m_camoButtons = new JButton[pa.length];
        playerTypes = new int[pa.length];
        for (int x = 0; x < pa.length; x++) {
            final Player curPlayer = m_players[x];
            curPlayer.setColorIndex(x);
            m_labels[x] = new JLabel(pa[x].getName(), JLabel.LEFT);
            m_typeChoices[x] = new Choice();
            m_typeChoices[x].add(Messages.getString("MegaMek.ScenarioDialog.me")); //$NON-NLS-1$
            m_typeChoices[x].add(Messages.getString("MegaMek.ScenarioDialog.otherh")); //$NON-NLS-1$
            m_typeChoices[x].add(Messages.getString("MegaMek.ScenarioDialog.bot")); //$NON-NLS-1$
            final Color defaultBackground = m_typeChoices[x].getBackground();
            m_camoButtons[x] = new JButton();
            final JButton curButton = m_camoButtons[x];
            curButton.setText(Messages.getString("MegaMek.NoCamoBtn")); //$NON-NLS-1$
            curButton.setPreferredSize(new Dimension(84, 72));
            curButton.setBackground(PlayerColors.getColor(x));
            curButton.setActionCommand("camo"); //$NON-NLS-1$

            // When a camo button is pressed, remove any previous
            // listener from the dialog, update the dialog for the
            // button's player, and add a new listener.
            curButton.addActionListener(new ActionListener() {
                private final CamoChoiceDialog dialog = camoDialog;
                private final JButton button = curButton;
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
                    dialog.setVisible(true);
                }
            });
        }
        setLayout(new BorderLayout());
        JPanel choicePanel = new JPanel();
        choicePanel.setLayout(new GridLayout(pa.length + 1, 0));
        choicePanel.add(new JLabel(Messages.getString("MegaMek.ScenarioDialog.pNameType"))); //$NON-NLS-1$
        choicePanel.add(new JLabel(Messages.getString("MegaMek.ScenarioDialog.Camo"))); //$NON-NLS-1$
        for (int x = 0; x < pa.length; x++) {
            JPanel typePanel = new JPanel();
            typePanel.setLayout(new GridLayout(0, 1));
            typePanel.add(m_labels[x]);
            typePanel.add(m_typeChoices[x]);
            choicePanel.add(typePanel);
            choicePanel.add(m_camoButtons[x]);
        }
        add(choicePanel, BorderLayout.CENTER);
        JPanel butPanel = new JPanel();
        butPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton bOkay = new JButton(Messages.getString("Okay")); //$NON-NLS-1$
        bOkay.setActionCommand("okay"); //$NON-NLS-1$
        bOkay.addActionListener(this);
        JButton bCancel = new JButton(Messages.getString("Cancel")); //$NON-NLS-1$
        bCancel.setActionCommand("cancel"); //$NON-NLS-1$
        bCancel.addActionListener(this);
        butPanel.add(bOkay);
        butPanel.add(bCancel);
        add(butPanel, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocation(frame.getLocation().x + frame.getSize().width / 2 - getSize().width / 2,
                frame.getLocation().y + frame.getSize().height / 2 - getSize().height / 2);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("okay")) { //$NON-NLS-1$
            boolean bMeSet = false;
            for (int x = 0; x < m_players.length; x++) {
                playerTypes[x] = m_typeChoices[x].getSelectedIndex();
                if (playerTypes[x] == T_ME) {
                    if (bMeSet) {
                        new AlertDialog(m_frame, Messages.getString("MegaMek.ScenarioErrorAllert.title"), Messages.getString("MegaMek.ScenarioErrorAllert.message")).setVisible(true); //$NON-NLS-1$ //$NON-NLS-2$
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
