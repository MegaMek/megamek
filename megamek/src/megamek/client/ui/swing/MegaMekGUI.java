/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing;

import com.thoughtworks.xstream.XStream;
import megamek.MegaMek;
import megamek.MegaMekConstants;
import megamek.client.Client;
import megamek.client.bot.BotClient;
import megamek.client.bot.TestBot;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.ui.swing.BotGUI;
import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.BotConfigDialog;
import megamek.client.ui.dialogs.helpDialogs.MMReadMeHelpDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.swing.gameConnectionDialogs.ConnectDialog;
import megamek.client.ui.swing.gameConnectionDialogs.HostDialog;
import megamek.client.ui.swing.skinEditor.SkinEditorMainGUI;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.widget.MegamekButton;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.common.*;
import megamek.common.enums.GamePhase;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.ImageUtil;
import megamek.common.util.SerializationHelper;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.server.ScenarioLoader;
import megamek.server.Server;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static megamek.common.Compute.d6;

public class MegaMekGUI implements IPreferenceChangeListener {
    private static final String FILENAME_MEGAMEK_SPLASH = "../misc/megamek-splash.jpg";
    private static final String FILENAME_ICON_16X16 = "megamek-icon-16x16.png";
    private static final String FILENAME_ICON_32X32 = "megamek-icon-32x32.png";
    private static final String FILENAME_ICON_48X48 = "megamek-icon-48x48.png";
    private static final String FILENAME_ICON_256X256 = "megamek-icon-256x256.png";

    private static final String FILENAME_BT_CLASSIC_FONT = "btclassic/BTLogo_old.ttf";

    private JFrame frame;
    private Client client;
    private Server server;
    private CommonAboutDialog about;
    private CommonSettingsDialog settingsDialog;

    private MegaMekController controller;

    BufferedImage backgroundIcon = null;

    public void start() {
        createGUI();
    }

    /**
     * Construct a MegaMek, and display the main menu in the specified frame.
     */
    private void createGUI() {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            File btFontFile = new MegaMekFile(Configuration.fontsDir(), FILENAME_BT_CLASSIC_FONT).getFile();
            Font btFont = Font.createFont(Font.TRUETYPE_FONT, btFontFile);
            LogManager.getLogger().info("Loaded Font: " + btFont.getName());
            ge.registerFont(btFont);
        } catch (Exception e) {
            LogManager.getLogger().error("Failed to Register BT Classic Font", e);
        }

        createController();

        GUIPreferences.getInstance().addPreferenceChangeListener(this);

        // Set a couple of things to make the Swing GUI look more "Mac-like" on
        // Macs
        // Taken from:
        // http://www.devdaily.com/apple/mac/java-mac-native-look/Introduction.shtml
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "MegaMek");

        // Add additional themes
        UIManager.installLookAndFeel("Flat Light", "com.formdev.flatlaf.FlatLightLaf");
        UIManager.installLookAndFeel("Flat IntelliJ", "com.formdev.flatlaf.FlatIntelliJLaf");
        UIManager.installLookAndFeel("Flat Dark", "com.formdev.flatlaf.FlatDarkLaf");
        UIManager.installLookAndFeel("Flat Darcula", "com.formdev.flatlaf.FlatDarculaLaf");

        // this should also help to make MegaMek look more system-specific
        try {
            UIManager.setLookAndFeel(GUIPreferences.getInstance().getUITheme());
        } catch (Exception e) {
            System.err.println("Error setting look and feel!");
            e.printStackTrace();
        }

        ToolTipManager.sharedInstance().setInitialDelay(
                GUIPreferences.getInstance().getTooltipDelay());
        if (GUIPreferences.getInstance().getTooltipDismissDelay() >= 0) {
            ToolTipManager.sharedInstance().setDismissDelay(
                    GUIPreferences.getInstance().getTooltipDismissDelay());
        }
        frame = new JFrame("MegaMek");
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });

        frame.setContentPane(new JPanel() {
            private static final long serialVersionUID = 5174313603291016012L;

            @Override
            protected void paintComponent(Graphics g) {
                if (backgroundIcon == null) {
                    super.paintComponent(g);
                    return;
                }
                int w = getWidth();
                int h = getHeight();
                int iW = backgroundIcon.getWidth();
                int iH = backgroundIcon.getHeight();
                // If the image isn't loaded, prevent an infinite loop
                if ((iW < 1) || (iH < 1)) {
                    return;
                }
                for (int x = 0; x < w; x += iW) {
                    for (int y = 0; y < h; y += iH) {
                        g.drawImage(backgroundIcon, x, y,null);
                    }
                }
            }
        });

        List<Image> iconList = new ArrayList<>();
        iconList.add(frame.getToolkit().getImage(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_16X16).toString()));
        iconList.add(frame.getToolkit().getImage(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_32X32).toString()));
        iconList.add(frame.getToolkit().getImage(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_48X48).toString()));
        iconList.add(frame.getToolkit().getImage(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_256X256).toString()));
        frame.setIconImages(iconList);
        CommonMenuBar menuBar = new CommonMenuBar(this);
        menuBar.addActionListener(actionListener);
        frame.setJMenuBar(menuBar);
        showMainMenu();

        // set visible on middle of screen
        frame.setLocationRelativeTo(null);
        // init the cache
        MechSummaryCache.getInstance();

        // Show the window.
        frame.setVisible(true);

        // tell the user about the readme...
        if (GUIPreferences.getInstance().getNagForReadme()) {
            ConfirmDialog confirm = new ConfirmDialog(frame,
                    Messages.getString("MegaMek.welcome.title") + MegaMekConstants.VERSION,
                    Messages.getString("MegaMek.welcome.message"), true);
            confirm.setVisible(true);
            if (!confirm.getShowAgain()) {
                GUIPreferences.getInstance().setNagForReadme(false);
            }
            if (confirm.getAnswer()) {
                new MMReadMeHelpDialog(frame).setVisible(true);
            }
        }
    }

    public void createController() {
        controller = new MegaMekController();
        KeyboardFocusManager kbfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kbfm.addKeyEventDispatcher(controller);

        KeyBindParser.parseKeyBindings(controller);
    }

    /**
     * Display the main menu.
     */
    private void showMainMenu() {
        SkinSpecification skinSpec = SkinXMLHandler.getSkin(SkinSpecification.UIComponents.MainMenuBorder.getComp(),
                true);
        frame.getContentPane().removeAll();
        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);
        frame.setResizable(false);

        MegamekButton hostB;
        MegamekButton connectB;
        MegamekButton botB;
        MegamekButton editB;
        MegamekButton skinEditB;
        MegamekButton scenB;
        MegamekButton loadB;
        MegamekButton quitB;
        JLabel labVersion = new JLabel(Messages.getString("MegaMek.Version") + MegaMekConstants.VERSION,
                JLabel.CENTER);
        labVersion.setPreferredSize(new Dimension(250,15));
        if (skinSpec.fontColors.size() > 0) {
            labVersion.setForeground(skinSpec.fontColors.get(0));
        }
        hostB = new MegamekButton(Messages.getString("MegaMek.hostNewGame.label"),
                SkinSpecification.UIComponents.MainMenuButton.getComp(), true);
        hostB.setActionCommand(ClientGUI.FILE_GAME_NEW);
        hostB.addActionListener(actionListener);
        scenB = new MegamekButton(Messages.getString("MegaMek.hostScenario.label"),
                SkinSpecification.UIComponents.MainMenuButton.getComp(), true);
        scenB.setActionCommand(ClientGUI.FILE_GAME_SCENARIO);
        scenB.addActionListener(actionListener);
        loadB = new MegamekButton(Messages.getString("MegaMek.hostSavedGame.label"),
                SkinSpecification.UIComponents.MainMenuButton.getComp(), true);
        loadB.setActionCommand(ClientGUI.FILE_GAME_LOAD);
        loadB.addActionListener(actionListener);
        connectB = new MegamekButton(Messages.getString("MegaMek.Connect.label"),
                SkinSpecification.UIComponents.MainMenuButton.getComp(), true);
        connectB.setActionCommand(ClientGUI.FILE_GAME_CONNECT);
        connectB.addActionListener(actionListener);
        botB = new MegamekButton(Messages.getString("MegaMek.ConnectAsBot.label"),
                SkinSpecification.UIComponents.MainMenuButton.getComp(), true);
        botB.setActionCommand(ClientGUI.FILE_GAME_CONNECT_BOT);
        botB.addActionListener(actionListener);
        editB = new MegamekButton(Messages.getString("MegaMek.MapEditor.label"),
                SkinSpecification.UIComponents.MainMenuButton.getComp(), true);
        editB.setActionCommand(ClientGUI.BOARD_NEW);
        editB.addActionListener(actionListener);
        skinEditB = new MegamekButton(Messages.getString("MegaMek.SkinEditor.label"),
                SkinSpecification.UIComponents.MainMenuButton.getComp(), true);
        skinEditB.setActionCommand(ClientGUI.MAIN_SKIN_NEW);
        skinEditB.addActionListener(actionListener);
        quitB = new MegamekButton(Messages.getString("MegaMek.Quit.label"),
                SkinSpecification.UIComponents.MainMenuButton.getComp(), true);
        quitB.setActionCommand(ClientGUI.MAIN_QUIT);
        quitB.addActionListener(actionListener);

        // Use the current monitor so we don't "overflow" computers whose primary
        // displays aren't as large as their secondary displays.
        DisplayMode currentMonitor = frame.getGraphicsConfiguration().getDevice().getDisplayMode();

        String splashFilename;
        if (skinSpec.hasBackgrounds()) {
            splashFilename = determineSplashScreen(skinSpec.backgrounds, 
                    currentMonitor.getWidth(), currentMonitor.getHeight());
            if (skinSpec.backgrounds.size() > 1) {
                File file = new MegaMekFile(Configuration.widgetsDir(),
                        skinSpec.backgrounds.get(1)).getFile();
                if (!file.exists()) {
                    LogManager.getLogger().error("MainMenu Error: background icon doesn't exist: "
                            + file.getAbsolutePath());
                } else {
                    backgroundIcon = (BufferedImage) ImageUtil.loadImageFromFile(file.toString());
                }
            }
        } else {
            splashFilename = FILENAME_MEGAMEK_SPLASH;
            backgroundIcon = null;
        }
        File splashFile = new File(Configuration.widgetsDir(), splashFilename);
        // Ensure we have a splash image, even if we have to fall back to default.
        if (!splashFile.exists()) {
            splashFilename = FILENAME_MEGAMEK_SPLASH;
        }
        // initialize splash image
        Image imgSplash = frame.getToolkit()
                .getImage(new MegaMekFile(Configuration.widgetsDir(), splashFilename).toString());

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

        FontMetrics metrics = hostB.getFontMetrics(loadB.getFont());
        int width = metrics.stringWidth(hostB.getText());
        int height = metrics.getHeight();
        Dimension textDim =  new Dimension(width+50, height+10);

        // Strive for no more than ~90% of the screen and use golden ratio to make
        // the button width "look" reasonable.
        int imageWidth = imgSplash.getWidth(frame);
        int maximumWidth = (int) (0.9 * currentMonitor.getWidth()) - imageWidth;
        Dimension minButtonDim = new Dimension((int) (maximumWidth / 1.618), 25);
        if (textDim.getWidth() > minButtonDim.getWidth()) {
            minButtonDim = textDim;
        }

        hostB.setPreferredSize(minButtonDim);
        connectB.setPreferredSize(minButtonDim);
        botB.setPreferredSize(minButtonDim);
        editB.setPreferredSize(minButtonDim);
        skinEditB.setPreferredSize(minButtonDim);
        scenB.setPreferredSize(minButtonDim);
        loadB.setPreferredSize(minButtonDim);
        quitB.setPreferredSize(minButtonDim);
        hostB.setPreferredSize(minButtonDim);

        connectB.setMinimumSize(minButtonDim);
        botB.setMinimumSize(minButtonDim);
        editB.setMinimumSize(minButtonDim);
        skinEditB.setMinimumSize(minButtonDim);
        scenB.setMinimumSize(minButtonDim);
        loadB.setMinimumSize(minButtonDim);
        quitB.setMinimumSize(minButtonDim);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        frame.getContentPane().setLayout(gridbag);
        // Left Column
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(10, 5, 10, 10);
        c.ipadx = 10; c.ipady = 5;
        c.gridx = 0;  c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0; c.weighty = 0.0;
        c.gridwidth = 1;
        c.gridheight = 9;
        addBag(panTitle, gridbag, c);
        // Right Column
        c.insets = new Insets(4, 4, 1, 12);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0; c.weighty = 1.0;
        c.ipadx = 0; c.ipady = 0;
        c.gridheight = 1;
        c.gridx = 1; c.gridy = 0;
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
        addBag(skinEditB, gridbag, c);
        c.gridy++;
        c.insets = new Insets(4, 4, 15, 12);
        addBag(quitB, gridbag, c);
        frame.validate();
        frame.pack();
    }

    /**
     * Display the board editor.
     */
    void showEditor() {
        BoardEditor editor = new BoardEditor(controller);
        controller.boardEditor = editor;
        launch(editor.getFrame());
        editor.boardNew(GUIPreferences.getInstance().getBoardEdRndStart());
    }
    
    void showSkinEditor() {
        int response = JOptionPane.showConfirmDialog(frame, 
                "The skin editor is currently "
                + "in beta and is a work in progress.  There are likely to "
                + "be issues. \nContinue?", "Continue?",
                JOptionPane.OK_CANCEL_OPTION);
        if (response == JOptionPane.CANCEL_OPTION) {
            return;
        }
        SkinEditorMainGUI skinEditor = new SkinEditorMainGUI();
        skinEditor.initialize();
        skinEditor.switchPanel(GamePhase.MOVEMENT);
        launch(skinEditor.getFrame());        
    }

    /**
     * Display the board editor and open an "open" dialog.
     */
    void showEditorOpen() {
        BoardEditor editor = new BoardEditor(controller);
        controller.boardEditor = editor;
        launch(editor.getFrame());
        editor.boardLoad();
    }

    /**
     * Start instances of both the client and the server.
     */
    void host() {
        HostDialog hd = new HostDialog(frame);
        hd.setVisible(true);

        if (!hd.dataValidation("MegaMek.HostGameAlert.title")) {
            return;
        }

        // kick off a RNG check
        d6();

        // start server
        try {
            server = new Server(hd.getServerPass(), hd.getPort(), hd.isRegister(),
                    hd.isRegister() ? hd.getMetaserver() : "");
        } catch (Exception e) {
            LogManager.getLogger().error("Could not create server socket on port " + hd.getPort(), e);
            JOptionPane.showMessageDialog(frame,
                    Messages.getFormattedString("MegaMek.StartServerError", hd.getPort(), e.getMessage()),
                    Messages.getString("MegaMek.HostGameAlert.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        // initialize client
        client = new Client(hd.getPlayerName(), "localhost", hd.getPort());
        ClientGUI gui = new ClientGUI(client, controller);
        controller.clientgui = gui;
        frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        gui.initialize();
        frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        if (!client.connect()) {
            JOptionPane.showMessageDialog(frame,
                    Messages.getFormattedString("MegaMek.ServerConnectionError", "localhost", hd.getPort()),
                    Messages.getString("MegaMek.HostGameAlert.title"), JOptionPane.ERROR_MESSAGE);
            frame.setVisible(false);
            client.die();
        }
        launch(gui.getFrame());
    }

    void loadGame() {
        JFileChooser fc = new JFileChooser("savegames");
        fc.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("MegaMek.SaveGameDialog.title"));
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                return ((dir.getName().endsWith(".sav") || dir.getName().endsWith(".sav.gz") || dir.isDirectory()));
            }

            @Override
            public String getDescription() {
                return "Savegames";
            }
        });
        int returnVal = fc.showOpenDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        Game newGame;
        try (InputStream is = new FileInputStream(fc.getSelectedFile()); InputStream gzi = new GZIPInputStream(is)) {
            XStream xstream = SerializationHelper.getXStream();
            newGame = (Game) xstream.fromXML(gzi);
        } catch (Exception e) {
            LogManager.getLogger().error("Unable to load file: " + fc.getSelectedFile(), e);
            JOptionPane.showMessageDialog(frame, Messages.getString("MegaMek.LoadGameAlert.message"),
            Messages.getString("MegaMek.LoadGameAlert.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!MegaMekConstants.VERSION.is(newGame.getVersion())) {
            final String message = String.format(Messages.getString("MegaMek.LoadGameIncorrectVersion.message"),
                    newGame.getVersion(), MegaMekConstants.VERSION);
            JOptionPane.showMessageDialog(frame, message,
                    Messages.getString("MegaMek.LoadGameAlert.title"), JOptionPane.ERROR_MESSAGE);
            LogManager.getLogger().error(message);
            return;
        }

        Vector<String> playerNames = new Vector<>();
        for (Player player : newGame.getPlayersVector()) {
            playerNames.add(player.getName());
        }

        HostDialog hd = new HostDialog(frame, playerNames);
        hd.setVisible(true);

        if (!hd.dataValidation("MegaMek.LoadGameAlert.title")) {
            return;
        }

        // kick off a RNG check
        d6();
        // start server
        try {
            server = new Server(hd.getServerPass(), hd.getPort(), hd.isRegister(), hd.isRegister() ? hd.getMetaserver() : "");
        } catch (IOException ex) {
            LogManager.getLogger().error("Could not create server socket on port " + hd.getPort(), ex);
            JOptionPane.showMessageDialog(frame,
                    Messages.getFormattedString("MegaMek.StartServerError", hd.getPort(), ex.getMessage()),
                    Messages.getString("MegaMek.LoadGameAlert.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!server.loadGame(fc.getSelectedFile())) {
            JOptionPane.showMessageDialog(frame, Messages.getString("MegaMek.LoadGameAlert.message"),
                    Messages.getString("MegaMek.LoadGameAlert.title"), JOptionPane.ERROR_MESSAGE);
            server.die();
            server = null;
            return;
        }

        client = new Client(hd.getPlayerName(), "localhost", hd.getPort());
        ClientGUI gui = new ClientGUI(client, controller);
        controller.clientgui = gui;
        frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        gui.initialize();
        frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        if (!client.connect()) {
            JOptionPane.showMessageDialog(frame,
                    Messages.getFormattedString("MegaMek.ServerConnectionError", "localhost", hd.getPort()),
                    Messages.getString("MegaMek.LoadGameAlert.title"), JOptionPane.ERROR_MESSAGE);
            frame.setVisible(false);
            client.die();
        }
        // free some memory that's only needed in lounge
        // This normally happens in the deployment phase in Client, but
        // if we are loading a game, this phase may not be reached
        MechFileParser.dispose();
        // We must do this last, as the name and unit generators can create
        // a new instance if they are running
        MechSummaryCache.dispose();

        launch(gui.getFrame());
    }
    
    /** Developer Utility: Loads "quicksave.sav.gz" with the last used connection settings. */
    void quickLoadGame() {
        // kick off a RNG check
        d6();
        // start server
        int port = PreferenceManager.getClientPreferences().getLastServerPort();
        try {
            server = new Server("", port, false, "");
        } catch (IOException ex) {
            LogManager.getLogger().error("Could not create server socket on port " + port, ex);
            JOptionPane.showMessageDialog(frame,
                    Messages.getFormattedString("MegaMek.StartServerError", port, ex.getMessage()),
                    Messages.getString("MegaMek.LoadGameAlert.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!server.loadGame(new File("./savegames", "quicksave.sav.gz"))) {
            JOptionPane.showMessageDialog(frame, Messages.getString("MegaMek.LoadGameAlert.message"),
                    Messages.getString("MegaMek.LoadGameAlert.title"), JOptionPane.ERROR_MESSAGE);
            server.die();
            server = null;
            return;
        }

        client = new Client(PreferenceManager.getClientPreferences().getLastPlayerName(), "localhost", port);
        ClientGUI gui = new ClientGUI(client, controller);
        controller.clientgui = gui;
        frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        gui.initialize();
        frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        if (!client.connect()) {
            JOptionPane.showMessageDialog(frame,
                    Messages.getFormattedString("MegaMek.ServerConnectionError", "localhost", port),
                    Messages.getString("MegaMek.LoadGameAlert.title"), JOptionPane.ERROR_MESSAGE);
            frame.setVisible(false);
            client.die();
        }
        // free some memory that's only needed in lounge
        // This normally happens in the deployment phase in Client, but
        // if we are loading a game, this phase may not be reached
        MechFileParser.dispose();
        // We must do this last, as the name and unit generators can create
        // a new instance if they are running
        MechSummaryCache.dispose();

        launch(gui.getFrame());
    }

    /**
     * Host a game constructed from a scenario file
     */
    void scenario() {
        JFileChooser fc = new JFileChooser("data" + File.separatorChar + "scenarios");
        fc.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("MegaMek.SelectScenarioDialog.title"));

        FileFilter filter = new FileFilter() {

            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }

                String ext = null;
                String s = f.getName();
                int i = s.lastIndexOf('.');

                if ((i > 0) && (i < (s.length() - 1))) {
                    ext = s.substring(i + 1).toLowerCase();
                }

                if (ext != null) {
                    return ext.equalsIgnoreCase("mms");
                }

                return false;
            }

            @Override
            public String getDescription() {
                return "MegaMek Scenario Files";
            }

        };
        fc.setFileFilter(filter);

        int returnVal = fc.showOpenDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        ScenarioLoader sl = new ScenarioLoader(fc.getSelectedFile());
        Game g;
        try {
            g = sl.createGame();
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            JOptionPane.showMessageDialog(frame,
                    Messages.getString("MegaMek.HostScenarioAlert.message", e.getMessage()),
                    Messages.getString("MegaMek.HostScenarioAlert.title"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // popup options dialog
        if (!sl.hasFixedGameOptions()) {
            GameOptionsDialog god = new GameOptionsDialog(frame, g.getOptions(), false);
            god.update(g.getOptions());
            god.setEditable(true);
            god.setVisible(true);
            for (IBasicOption opt : god.getOptions()) {
                IOption orig = g.getOptions().getOption(opt.getName());
                orig.setValue(opt.getValue());
            }
        }

        // popup planetary conditions dialog
        if (!sl.hasFixedPlanetCond()) {
            PlanetaryConditionsDialog pcd = new PlanetaryConditionsDialog(frame, g.getPlanetaryConditions());
            pcd.update(g.getPlanetaryConditions());
            pcd.setVisible(true);
            g.setPlanetaryConditions(pcd.getConditions());
        }
        
        String playerName;
        int port;
        String serverPW;
        String localName;
        Player[] pa = new Player[g.getPlayersVector().size()];
        int[] playerTypes = new int[pa.length];
        g.getPlayersVector().copyInto(pa);
        boolean hasSlot = false;

        // get player types and colors set
        if (!sl.isSinglePlayer()) {
            ScenarioDialog sd = new ScenarioDialog(frame, pa);
            sd.setVisible(true);
            if (!sd.bSet) {
                return;
            }

            HostDialog hd = new HostDialog(frame);
            if (!("".equals(sd.localName))) {
                hasSlot = true;
            }
            hd.setPlayerName(sd.localName);
            hd.setVisible(true);

            if (!hd.dataValidation("MegaMek.HostScenarioAlert.title")) {
                return;
            }

            sd.localName = hd.getPlayerName();
            localName = hd.getPlayerName();
            playerName = hd.getPlayerName();
            port = hd.getPort();
            serverPW = hd.getServerPass();
            playerTypes = Arrays.copyOf(sd.playerTypes, playerTypes.length);

        } else {
            hasSlot = true;
            playerName = pa[0].getName();
            localName = playerName;
            port = 2346;
            serverPW = "";
            playerTypes[0] = 0;
            for (int i = 1; i < playerTypes.length; i++) {
                playerTypes[i] = ScenarioDialog.T_BOT;
            }
        }

        // kick off a RNG check
        Compute.d6();

        // start server
        try {
            server = new Server(serverPW, port);
        } catch (Exception ex) {
            LogManager.getLogger().error("Could not create server socket on port " + port, ex);
            JOptionPane.showMessageDialog(frame,
                    Messages.getFormattedString("MegaMek.StartServerError", port, ex.getMessage()),
                    Messages.getString("MegaMek.HostScenarioAlert.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        server.setGame(g);
        
        // apply any scenario damage
        sl.applyDamage(server);
        ClientGUI gui = null;
        if (!"".equals(localName)) {
            // initialize game
            client = new Client(playerName, "localhost", port);
            gui = new ClientGUI(client, controller);
            controller.clientgui = gui;
            gui.initialize();
            if (!client.connect()) {
                JOptionPane.showMessageDialog(frame,
                        Messages.getFormattedString("MegaMek.ServerConnectionError", "localhost", port),
                        Messages.getString("MegaMek.HostScenarioAlert.title"), JOptionPane.ERROR_MESSAGE);
                frame.setVisible(false);
                client.die();
            }
        }

        // calculate initial BV
        server.calculatePlayerInitialCounts();
        
        // setup any bots
        for (int x = 0; x < pa.length; x++) {
            if (playerTypes[x] == ScenarioDialog.T_BOT) {
                LogManager.getLogger().info("Adding bot "  + pa[x].getName() + " as Princess");
                BotClient c = new Princess(pa[x].getName(), "localhost", port);
                c.getGame().addGameListener(new BotGUI(c));
                c.connect();                
            } else if (playerTypes[x] == ScenarioDialog.T_OBOT) {
                LogManager.getLogger().info("Adding bot "  + pa[x].getName() + " as TestBot");
                BotClient c = new TestBot(pa[x].getName(), "localhost", port);
                c.getGame().addGameListener(new BotGUI(c));
                c.connect();
            }
        }

        // If he didn't have a name when hasSlot was set, then the host should
        // be an observer.
        if (!hasSlot) {
            Enumeration<Player> pE = server.getGame().getPlayers();
            while (pE.hasMoreElements()) {
                Player tmpP = pE.nextElement();
                if (tmpP.getName().equals(localName)) {
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
    void connect() {
        ConnectDialog cd = new ConnectDialog(frame);
        cd.setVisible(true);

        if (!cd.dataValidation("MegaMek.ConnectDialog.title")) {
            return;
        }

        // initialize game
        client = new Client(cd.getPlayerName(), cd.getServerAddress(), cd.getPort());
        ClientGUI gui = new ClientGUI(client, controller);
        controller.clientgui = gui;
        frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        gui.initialize();
        frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        if (!client.connect()) {
            JOptionPane.showMessageDialog(frame,
                    Messages.getFormattedString("MegaMek.ServerConnectionError", cd.getServerAddress(), cd.getPort()),
                    Messages.getString("MegaMek.ConnectDialog.title"), JOptionPane.ERROR_MESSAGE);
            frame.setVisible(false);
            client.die();
        }
        launch(gui.getFrame());
    }

    void connectBot() {
        var cd = new ConnectDialog(frame);
        cd.setVisible(true);
        if (!cd.dataValidation("MegaMek.ConnectDialog.title")) {
            return;
        }

        // initialize game
        var bcd = new BotConfigDialog(frame, cd.getPlayerName());
        bcd.setVisible(true);
        if (bcd.getResult() == DialogResult.CANCELLED) {
            return; 
        }
        client = Princess.createPrincess(bcd.getBotName(), cd.getServerAddress(), cd.getPort(), bcd.getBehaviorSettings());
        client.getGame().addGameListener(new BotGUI((BotClient) client));
        ClientGUI gui = new ClientGUI(client, controller);
        controller.clientgui = gui;
        gui.initialize();
        if (!client.connect()) {
            JOptionPane.showMessageDialog(frame,
                    Messages.getFormattedString("MegaMek.ServerConnectionError", cd.getServerAddress(), cd.getPort()),
                    Messages.getString("MegaMek.ConnectDialog.title"), JOptionPane.ERROR_MESSAGE);
            frame.setVisible(false);
            client.die();
        }
        launch(gui.getFrame());
    }

    private void addBag(JComponent comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        frame.getContentPane().add(comp);
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

    private void showSkinningHowTo() {
        try {
            // Get the correct help file.
            StringBuilder helpPath = new StringBuilder("file:///");
            helpPath.append(System.getProperty("user.dir"));
            if (!helpPath.toString().endsWith(File.separator)) {
                helpPath.append(File.separator);
            }
            helpPath.append(Messages.getString("ClientGUI.skinningHelpPath"));
            URL helpUrl = new URL(helpPath.toString());

            // Launch the help dialog.
            HelpDialog helpDialog = new HelpDialog(
                    Messages.getString("ClientGUI.skinningHelpPath.title"),
                    helpUrl);
            helpDialog.setVisible(true);
        } catch (MalformedURLException e) {
            LogManager.getLogger().error("", e);
        }
    }

    /**
     * Called when the user selects the "View->Client Settings" menu item.
     */
    void showSettings() {
        // Do we need to create the "settings" dialog?
        if (settingsDialog == null) {
            settingsDialog = new CommonSettingsDialog(frame);
        }

        // Show the settings dialog.
        settingsDialog.setVisible(true);
    }

    /**
     * Called when the quit buttons is pressed or the main menu is closed.
     */
    static void quit() {
        MegaMek.getPreferences().saveToFile(MegaMek.PREFERENCES_FILE);
        PreferenceManager.getInstance().save();

        try {
            WeaponOrderHandler.saveWeaponOrderFile();
        } catch (IOException e) {
            LogManager.getLogger().error("Error saving custom weapon orders!", e);
        }
        QuirksHandler.saveCustomQuirksList();
        System.exit(0);
    }

    /**
     * Hides this window for later. Listens to the frame until it closes, then
     * calls unlaunch().
     */
    private void launch(JFrame launched) {
        // listen to new frame
        launched.addWindowListener(new WindowAdapter() {
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
    
    private final ActionListener actionListener = ev -> {
        switch (ev.getActionCommand()) {
            case ClientGUI.BOARD_NEW:
                showEditor();
                break;
            case ClientGUI.MAIN_SKIN_NEW:
                showSkinEditor();
                break;
            case ClientGUI.BOARD_OPEN:
                showEditorOpen();
                break;
            case ClientGUI.FILE_GAME_NEW:
                host();
                break;
            case ClientGUI.FILE_GAME_SCENARIO:
                scenario();
                break;
            case ClientGUI.FILE_GAME_CONNECT:
                connect();
                break;
            case ClientGUI.FILE_GAME_CONNECT_BOT:
                connectBot();
                break;
            case ClientGUI.FILE_GAME_LOAD:
                loadGame();
                break;
            case ClientGUI.FILE_GAME_QLOAD:
                quickLoadGame();
                break;
            case ClientGUI.HELP_ABOUT:
                showAbout();
                break;
            case ClientGUI.HELP_CONTENTS:
                new MMReadMeHelpDialog(frame).setVisible(true);
                break;
            case ClientGUI.HELP_SKINNING:
                showSkinningHowTo();
                break;
            case ClientGUI.VIEW_CLIENT_SETTINGS:
                showSettings();
                break;
            case ClientGUI.MAIN_QUIT:
                quit();
                break;
        }
    };

    @Override
    public void preferenceChange(PreferenceChangeEvent e) {
        // Update to reflect new skin
        if (e.getName().equals(GUIPreferences.SKIN_FILE)) {
            showMainMenu();
            frame.repaint();
        } else if (e.getName().equals(GUIPreferences.UI_THEME)) {
            try {
                UIManager.setLookAndFeel((String) e.getNewValue());
                // We went all Oprah and gave everybody frames...
                // so now we have to let everybody who got a frame
                // under their chair know that we updated our look
                // and feel.
                for (Frame f : Frame.getFrames()) {
                    SwingUtilities.updateComponentTreeUI(f);
                }
                // ...and also all of our windows and dialogs, etc.
                for (Window w : Window.getWindows()) {
                    SwingUtilities.updateComponentTreeUI(w);
                }
            } catch (Exception ex) {
                LogManager.getLogger().error("", ex);
            }
        }
    }
    
    /**
     * Method used to determine the appropriate splash screen to use. This method looks 
     * at both the height and the width of the main monitor.
     * 
     * @param splashScreens
     * 		List of available splash screens.
     * @param screenWidth
     * 		Width of the current monitor.
     * @param screenHeight
     * 		Height of the current monitor.
     * @return
     * 		String that represents the splash screen that should be displayed.
     */
    private String determineSplashScreen(final List<String> splashScreens, 
            final int screenWidth, final int screenHeight) {
        // Ensure that the list is of appropriate size to contain HD, FHD, and UHD splash
        // screens.
        if (splashScreens.size() > 3) {
            // Default to the HD splash screen.
            String splashFileName = splashScreens.get(3);
            // If both height and width is greater than 1080p use the UHD splash screen.
            if (screenWidth > 1920 && screenHeight > 1080) {
                splashFileName = splashScreens.get(2);
            }
            // If both height and width is greater than 720p then use the FHD splash screen.
            else if (screenWidth > 1280 && screenHeight > 720)
            {
                splashFileName = splashScreens.get(0);
            }
            return splashFileName;
        }
        // List of splash screens is not complete so default to the first splash screen.
        return splashScreens.get(0);
    }
}
