/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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

import megamek.MMConstants;
import megamek.MegaMek;
import megamek.SuiteConstants;
import megamek.Version;
import megamek.client.Client;
import megamek.client.IClient;
import megamek.client.SBFClient;
import megamek.client.bot.BotClient;
import megamek.client.bot.princess.Princess;
import megamek.client.bot.ui.swing.BotGUI;
import megamek.client.ui.Messages;
import megamek.client.ui.dialogs.BotConfigDialog;
import megamek.client.ui.dialogs.helpDialogs.MMReadMeHelpDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.swing.ai.editor.AiProfileEditor;
import megamek.client.ui.swing.dialog.MainMenuUnitBrowserDialog;
import megamek.client.ui.swing.gameConnectionDialogs.ConnectDialog;
import megamek.client.ui.swing.gameConnectionDialogs.HostDialog;
import megamek.client.ui.swing.scenario.ScenarioChooser;
import megamek.client.ui.swing.skinEditor.SkinEditorMainGUI;
import megamek.client.ui.swing.tooltip.PilotToolTip;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.util.UIUtil;
import megamek.client.ui.swing.widget.MegaMekButton;
import megamek.client.ui.swing.widget.SkinSpecification;
import megamek.client.ui.swing.widget.SkinSpecification.UIComponents;
import megamek.client.ui.swing.widget.SkinXMLHandler;
import megamek.client.ui.swing.widget.SkinnedJPanel;
import megamek.codeUtilities.StringUtility;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.jacksonadapters.BotParser;
import megamek.common.options.GameOptions;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.preference.PreferenceManager;
import megamek.common.scenario.Scenario;
import megamek.common.scenario.ScenarioLoader;
import megamek.common.util.EmailService;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;
import megamek.server.IGameManager;
import megamek.server.Server;
import megamek.server.sbf.SBFGameManager;
import megamek.server.totalwarfare.TWGameManager;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.*;
import java.util.zip.GZIPInputStream;

import static megamek.common.Compute.d6;

public class MegaMekGUI implements IPreferenceChangeListener {
    private static final MMLogger logger = MMLogger.create(MegaMekGUI.class);

    private static final String FILENAME_MEGAMEK_SPLASH = "../misc/megamek_splash_spooky_hd.png";
    private static final String FILENAME_ICON_16X16 = "megamek-icon-16x16.png";
    private static final String FILENAME_ICON_32X32 = "megamek-icon-32x32.png";
    private static final String FILENAME_ICON_48X48 = "megamek-icon-48x48.png";
    private static final String FILENAME_ICON_256X256 = "megamek-icon-256x256.png";

    private JFrame frame;
    private IClient client;
    private Server server;
    private IGameManager gameManager;
    private CommonSettingsDialog settingsDialog;

    private static MegaMekController controller;

    public void start(boolean show) {
        createGUI(show);
    }

    /**
     * Construct a MegaMek, and display the main menu in the specified frame.
     */
    private void createGUI(boolean show) {
        createController();

        GUIPreferences.getInstance().addPreferenceChangeListener(this);

        // TODO : Move Theme setup to MegaMek::initializeSuiteSetups as part of
        // implementing it in
        // TODO : SuiteOptions
        updateGuiScaling();

        // TODO : Move ToolTip setup to MegaMek::initializeSuiteSetups as part of
        // implementing them
        // TODO : in SuiteOptions
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

        frame.setContentPane(new SkinnedJPanel(UIComponents.MainMenuBorder, 1));

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

        CommonMenuBar menuBar = CommonMenuBar.getMenuBarForMainMenu();
        menuBar.addActionListener(actionListener);
        frame.setJMenuBar(menuBar);
        showMainMenu();

        // set visible on middle of screen
        frame.setLocationRelativeTo(null);
        // init the cache
        MekSummaryCache.getInstance();

        // Show the window.
        frame.setVisible(show);

        // tell the user about the readme...
        if (show && GUIPreferences.getInstance().getNagForReadme()) {
            ConfirmDialog confirm = new ConfirmDialog(frame,
                    Messages.getString("MegaMek.welcome.title") + MMConstants.VERSION,
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

    public static MegaMekController getKeyDispatcher() {
        return controller;
    }

    /**
     * Display the main menu.
     */
    private void showMainMenu() {
        SkinSpecification skinSpec = SkinXMLHandler.getSkin(UIComponents.MainMenuBorder.getComp(),
                true);
        frame.getContentPane().removeAll();
        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);
        frame.setResizable(false);

        JLabel labVersion = new JLabel(Messages.getString("MegaMek.Version") + MMConstants.VERSION,
                JLabel.CENTER);
        labVersion.setPreferredSize(new Dimension(250, 15));
        if (!skinSpec.fontColors.isEmpty()) {
            labVersion.setForeground(skinSpec.fontColors.get(0));
        }
        MegaMekButton hostB = new MegaMekButton(Messages.getString("MegaMek.hostNewGame.label"),
                UIComponents.MainMenuButton.getComp(), true);
        hostB.setActionCommand(ClientGUI.FILE_GAME_NEW);
        hostB.addActionListener(actionListener);
        MegaMekButton scenB = new MegaMekButton(Messages.getString("MegaMek.hostScenario.label"),
                UIComponents.MainMenuButton.getComp(), true);
        scenB.setActionCommand(ClientGUI.FILE_GAME_SCENARIO);
        scenB.addActionListener(actionListener);
        MegaMekButton loadB = new MegaMekButton(Messages.getString("MegaMek.hostSavedGame.label"),
                UIComponents.MainMenuButton.getComp(), true);
        loadB.setActionCommand(ClientGUI.FILE_GAME_LOAD);
        loadB.addActionListener(actionListener);
        MegaMekButton connectB = new MegaMekButton(Messages.getString("MegaMek.Connect.label"),
                UIComponents.MainMenuButton.getComp(), true);
        connectB.setActionCommand(ClientGUI.FILE_GAME_CONNECT);
        connectB.addActionListener(actionListener);
        MegaMekButton editB = new MegaMekButton(Messages.getString("MegaMek.MapEditor.label"),
                UIComponents.MainMenuButton.getComp(), true);
        editB.setActionCommand(ClientGUI.BOARD_NEW);
        editB.addActionListener(actionListener);
        MegaMekButton skinEditB = new MegaMekButton(Messages.getString("MegaMek.SkinEditor.label"),
                UIComponents.MainMenuButton.getComp(), true);
        skinEditB.setActionCommand(ClientGUI.MAIN_SKIN_NEW);
        skinEditB.addActionListener(actionListener);
        MegaMekButton quitB = new MegaMekButton(Messages.getString("MegaMek.Quit.label"),
            UIComponents.MainMenuButton.getComp(), true);
        quitB.setActionCommand(ClientGUI.MAIN_QUIT);
        quitB.addActionListener(actionListener);

        // only if under development
        MegaMekButton editAi = new MegaMekButton(Messages.getString("MegaMek.AiEditor.label"),
            UIComponents.MainMenuButton.getComp(), true);
        editAi.setActionCommand(ClientGUI.NEW_AI);
        editAi.addActionListener(actionListener);
        MegaMekButton connectSBF = new MegaMekButton("Connect to SBF",
            UIComponents.MainMenuButton.getComp(), true);
        connectSBF.setActionCommand(ClientGUI.FILE_GAME_CONNECT_SBF);
        connectSBF.addActionListener(actionListener);
        MegaMekButton botB = new MegaMekButton(Messages.getString("MegaMek.ConnectAsBot.label"),
            UIComponents.MainMenuButton.getComp(), true);
        botB.setActionCommand(ClientGUI.FILE_GAME_CONNECT_BOT);
        botB.addActionListener(actionListener);


        // Use the current monitor, so we don't "overflow" computers whose primary
        // displays aren't as large as their secondary displays.
        Dimension scaledMonitorSize = UIUtil.getScaledScreenSize(frame);
        Image imgSplash = getSplashScreen(skinSpec.backgrounds, scaledMonitorSize.width, scaledMonitorSize.height);
        JLabel splash = UIUtil.createSplashComponent(imgSplash, frame, scaledMonitorSize);

        FontMetrics metrics = hostB.getFontMetrics(loadB.getFont());
        int width = metrics.stringWidth(hostB.getText());
        int height = metrics.getHeight();
        Dimension textDim = new Dimension(width + 50, height + 10);

        // Strive for no more than ~90% of the screen and use golden ratio to make
        // the button width "look" reasonable.
        int maximumWidth = (int) (0.9 * scaledMonitorSize.width) - splash.getPreferredSize().width;

        //no more than 50% of image width
        if (maximumWidth > (int) (0.5 * splash.getPreferredSize().width)) {
            maximumWidth = (int) (0.5 * splash.getPreferredSize().width);
        }

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
        editAi.setPreferredSize(minButtonDim);
        hostB.setPreferredSize(minButtonDim);
        quitB.setPreferredSize(minButtonDim);

        connectB.setMinimumSize(minButtonDim);
        botB.setMinimumSize(minButtonDim);
        editB.setMinimumSize(minButtonDim);
        skinEditB.setMinimumSize(minButtonDim);
        scenB.setMinimumSize(minButtonDim);
        loadB.setMinimumSize(minButtonDim);
        editAi.setMinimumSize(minButtonDim);
        quitB.setMinimumSize(minButtonDim);

        // layout
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        frame.getContentPane().setLayout(gridbag);
        // Left Column
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(10, 5, 10, 10);
        c.ipadx = 10;
        c.ipady = 5;
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.gridheight = 9;
        addBag(splash, gridbag, c);
        // Right Column
        c.insets = new Insets(4, 4, 1, 12);
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.ipadx = 0;
        c.ipady = 0;
        c.gridheight = 1;
        c.gridx = 1;
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

        addBag(editB, gridbag, c);
        c.gridy++;
        addBag(skinEditB, gridbag, c);

        if (MegaMek.isDevelopment()) {
//            c.gridy++;
//            addBag(botB, gridbag, c);
//
//            c.gridy++;
//            addBag(connectSBF, gridbag, c);

        }

        c.gridy++;
        addBag(editAi, gridbag, c);
        c.gridy++;
        c.insets = new Insets(4, 4, 15, 12);
        addBag(quitB, gridbag, c);
        frame.validate();
        frame.pack();
        // center window in screen
        frame.setLocationRelativeTo(null);
    }

    /**
     * Display the AI editor.
     */
    void showAiEditor() {
        AiProfileEditor editor = new AiProfileEditor(controller);
        controller.aiEditor = editor;
        launch(editor.getFrame());
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

    /**
     * Display the board editor and load the given board
     */
    void showEditor(String boardFile) {
        BoardEditor editor = new BoardEditor(controller);
        controller.boardEditor = editor;
        launch(editor.getFrame());
        editor.loadBoard(new File(boardFile));
    }

    void showSkinEditor() {
        int response = JOptionPane.showConfirmDialog(frame,
                "The skin editor is currently "
                        + "in beta and is a work in progress.  There are likely to "
                        + "be issues. \nContinue?",
                "Continue?",
                JOptionPane.OK_CANCEL_OPTION);
        if (response == JOptionPane.CANCEL_OPTION) {
            return;
        }
        SkinEditorMainGUI skinEditor = new SkinEditorMainGUI();
        skinEditor.initialize();
        launch(skinEditor.getFrame());
    }

    /**
     * Display the board editor and open an "open" dialog.
     */
    void showEditorOpen() {
        BoardEditor editor = new BoardEditor(controller);
        controller.boardEditor = editor;
        launch(editor.getFrame());
        editor.loadBoard();
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

        startHost(hd.getServerPass(), hd.getPort(), hd.isRegister(),
                hd.isRegister() ? hd.getMetaserver() : "", null, null,
                hd.getPlayerName());
    }

    public void startHost(@Nullable String serverPassword, int port, boolean isRegister,
            @Nullable String metaServer, @Nullable String mailPropertiesFileName,
            @Nullable File savegame, String playerName) {
        if (!startServer(serverPassword, port, isRegister, metaServer, mailPropertiesFileName, savegame)) {
            return;
        }

        startClient(playerName, MMConstants.LOCALHOST, server.getPort());
    }

    public boolean startServer(@Nullable String serverPassword, int port, boolean isRegister,
            @Nullable String metaServer, @Nullable String mailPropertiesFileName,
            @Nullable File saveGameFile) {
        return startServer(serverPassword, port, isRegister, metaServer, mailPropertiesFileName, saveGameFile,
                GameType.TW);
    }

    public boolean startServer(@Nullable String serverPassword, int port, boolean isRegister,
            @Nullable String metaServer, @Nullable String mailPropertiesFileName,
            @Nullable File saveGameFile, GameType gameType) {
        try {
            serverPassword = Server.validatePassword(serverPassword);
            port = Server.validatePort(port);
        } catch (Exception ex) {
            logger.error("Failed to start Server", ex);
            frame.setVisible(true);
            return false;
        }

        EmailService mailer = null;
        if (!StringUtility.isNullOrBlank(mailPropertiesFileName)) {
            File propsFile = new File(mailPropertiesFileName);
            try (var propsReader = new FileReader(propsFile)) {
                var mailProperties = new Properties();
                mailProperties.load(propsReader);
                mailer = new EmailService(mailProperties);
            } catch (Exception ex) {
                logger.error(ex,
                        Messages.getFormattedString("MegaMek.StartServerError", port, ex.getMessage()),
                        Messages.getString("MegaMek.LoadGameAlert.title"));
                return false;
            }
        }

        // kick off a RNG check
        d6();

        // start server
        try {
            gameManager = getGameManager(gameType);
            server = new Server(serverPassword, port, gameManager, isRegister, metaServer, mailer, false);
        } catch (Exception ex) {
            logger.error(ex,
                    Messages.getFormattedString("MegaMek.StartServerError", port, ex.getMessage()),
                    Messages.getString("MegaMek.LoadGameAlert.title"));
            return false;
        }

        if (saveGameFile != null && !server.loadGame(saveGameFile)) {
            logger.error(Messages.getFormattedString("MegaMek.LoadGameAlert.message", saveGameFile.getAbsolutePath()),
                    Messages.getString("MegaMek.LoadGameAlert.title"));
            server.die();
            server = null;
            return false;
        }

        return true;
    }

    private IGameManager getGameManager(GameType gameType) {
        return switch (gameType) {
            /*
             * Not implemented:
             * case AS-> new ASGameManager();
             * case BF-> new BFGameManager();
             */
            case SBF -> new SBFGameManager();
            default -> new TWGameManager();
        };
    }

    private IClientGUI getClientGUI(GameType gameType, IClient client, MegaMekController controller) {
        return switch (gameType) {
            /*
             * Not implemented:
             * case AS-> new ASGameManager();
             * case BF-> new BFGameManager();
             */
            case SBF -> new SBFClientGUI((SBFClient) client, controller);
            default -> new ClientGUI((Client) client, controller);
        };
    }

    private IClient getClient(GameType gameType, String playerName, String host, int port) {
        return switch (gameType) {
            /*
             * Not implemented:
             * case AS-> new ASClient();
             * case BF-> new BFClient();
             */
            case SBF -> new SBFClient(playerName, host, port);
            default -> new Client(playerName, host, port);
        };
    }

    public void startClient(String playerName, String serverAddress, int port) {
        startClient(playerName, serverAddress, port, GameType.TW);
    }

    public void startClient(String playerName, String serverAddress, int port, GameType gameType) {
        try {
            playerName = Server.validatePlayerName(playerName);
            serverAddress = Server.validateServerAddress(serverAddress);
            port = Server.validatePort(port);
        } catch (Exception ex) {
            logger.error(ex,
                    Messages.getFormattedString("MegaMek.ServerConnectionError", serverAddress, port),
                    Messages.getString("MegaMek.LoadGameAlert.title"));
            return;
        }

        // delete PilotToolTip cache
        PilotToolTip.deleteImageCache();
        client = getClient(gameType, playerName, serverAddress, port);
        IClientGUI gui = getClientGUI(gameType, client, controller);
        controller.clientgui = gui;
        frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        gui.initialize();
        frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        if (!client.connect()) {
            logger.error(Messages.getFormattedString("MegaMek.ServerConnectionError", serverAddress, port),
                    Messages.getString("MegaMek.LoadGameAlert.title"));
            client.die();
            gui.die();
            return;
        }

        // free some memory that's only needed in lounge
        // This normally happens in the deployment phase in Client, but
        // if we are loading a game, this phase may not be reached
        MekFileParser.dispose();
        // We must do this last, as the name and unit generators can create
        // a new instance if they are running
        MekSummaryCache.dispose();

        launch(gui.getFrame());
    }

    void loadGame() {
        JFileChooser fc = new JFileChooser(MMConstants.SAVEGAME_DIR);
        fc.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("MegaMek.SaveGameDialog.title"));
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                return dir.getName().endsWith(MMConstants.SAVE_FILE_EXT)
                        || dir.getName().endsWith(MMConstants.SAVE_FILE_GZ_EXT) || dir.isDirectory();
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

        final Vector<String> playerNames = new Vector<>();

        // Handrolled extraction, as we require Server initialization to use XStream and
        // don't need the additional overhead of initializing everything twice
        try (InputStream is = new FileInputStream(fc.getSelectedFile())) {
            InputStream gzi;

            if (fc.getSelectedFile().getName().toLowerCase().endsWith(".gz")) {
                gzi = new GZIPInputStream(is);
            } else {
                gzi = is;
            }

            // Using factory get an instance of document builder
            final DocumentBuilder documentBuilder = MMXMLUtility.newSafeDocumentBuilder();
            // Parse using builder to get DOM representation of the XML file
            final Document xmlDocument = documentBuilder.parse(gzi);

            final Element gameElement = xmlDocument.getDocumentElement();
            gameElement.normalize();

            final NodeList nl = gameElement.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
                final Node n = nl.item(i);
                if (n.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                switch (n.getNodeName()) {
                    case "version":
                        if (!validateSaveVersion(n)) {
                            return;
                        }
                        break;
                    case "players":
                        parsePlayerNames(n, playerNames);
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception ex) {
            logger.error(ex,
                    Messages.getFormattedString("MegaMek.LoadGameAlert.message",
                            fc.getSelectedFile().getAbsolutePath()),
                    Messages.getString("MegaMek.LoadGameAlert.title"));
            return;
        }

        HostDialog hd = new HostDialog(frame, playerNames);
        hd.setVisible(true);

        if (!hd.dataValidation("MegaMek.LoadGameAlert.title")) {
            return;
        }

        startHost(hd.getServerPass(), hd.getPort(),
                hd.isRegister(), hd.isRegister() ? hd.getMetaserver() : "", null,
                fc.getSelectedFile(), hd.getPlayerName());
    }

    private boolean validateSaveVersion(final Node n) {
        if (!n.hasChildNodes()) {
            final String message = String.format(
                    Messages.getString("MegaMek.LoadGameMissingVersion.message"),
                    SuiteConstants.VERSION);
            JOptionPane.showMessageDialog(frame, message,
                    Messages.getString("MegaMek.LoadGameAlert.title"),
                    JOptionPane.ERROR_MESSAGE);
            logger.error(message);
            return false;
        }

        final Version version = getVersion(n);
        if (SuiteConstants.VERSION.is(version)) {
            return true;
        } else {
            final String message = String.format(
                    Messages.getString("MegaMek.LoadGameIncorrectVersion.message"),
                    version, SuiteConstants.VERSION);
            logger.error(message, Messages.getString("MegaMek.LoadGameAlert.title"));
            return false;
        }
    }

    private static Version getVersion(Node n) {
        final NodeList nl = n.getChildNodes();
        String release = null;
        String major = null;
        String minor = null;
        String snapshot = null;
        for (int i = 0; i < nl.getLength(); i++) {
            final Node n2 = nl.item(i);
            if (n2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            switch (n2.getNodeName()) {
                case "release":
                    release = n2.getTextContent();
                    break;
                case "major":
                    major = n2.getTextContent();
                    break;
                case "minor":
                    minor = n2.getTextContent();
                    break;
                case "snapshot":
                    snapshot = n2.getTextContent();
                    break;
                default:
                    break;
            }
        }

        return new Version(release, major, minor, snapshot);
    }

    private void parsePlayerNames(final Node nodePlayers, final Vector<String> playerNames) {
        if (!nodePlayers.hasChildNodes()) {
            return;
        }

        final NodeList nodePlayersChildren = nodePlayers.getChildNodes();
        for (int i = 0; i < nodePlayersChildren.getLength(); i++) {
            final Node nodeEntry = nodePlayersChildren.item(i);
            if ((nodeEntry.getNodeType() != Node.ELEMENT_NODE) || !nodeEntry.hasChildNodes()
                    || !"entry".equals(nodeEntry.getNodeName())) {
                continue;
            }

            final NodeList nodeEntryChildren = nodeEntry.getChildNodes();
            for (int k = 0; k < nodeEntryChildren.getLength(); k++) {
                final Node nodePlayerClass = nodeEntryChildren.item(k);
                if ((nodePlayerClass.getNodeType() != Node.ELEMENT_NODE) || !nodePlayerClass.hasChildNodes()
                        || !Player.class.getName().equals(nodePlayerClass.getNodeName())) {
                    continue;
                }

                final NodeList nodePlayerClassChildren = nodePlayerClass.getChildNodes();
                for (int j = 0; j < nodePlayerClassChildren.getLength(); j++) {
                    final Node n3 = nodePlayerClassChildren.item(j);
                    if (n3.getNodeType() != Node.ELEMENT_NODE) {
                        continue;
                    }

                    if ("name".equals(n3.getNodeName())) {
                        playerNames.add(n3.getTextContent());
                    }
                }
            }
        }
    }

    /**
     * Developer Utility: Loads "quicksave.sav.gz" with the last used connection
     * settings.
     */
    public void quickLoadGame() {
        File file = MegaMek.getQuickSaveFile();
        if (!file.exists() || !file.canRead()) {
            JOptionPane.showMessageDialog(frame,
                    Messages.getFormattedString("MegaMek.LoadGameAlert.message", file.getAbsolutePath()),
                    Messages.getString("MegaMek.LoadGameAlert.title"), JOptionPane.ERROR_MESSAGE);
            frame.setVisible(true);
            return;
        }

        startHost("", 0, false, "", null, file,
                PreferenceManager.getClientPreferences().getLastPlayerName());
    }

    /**
     * Host a game constructed from a scenario file
     */
    void scenario() {
        ScenarioChooser scenarioChooser = new ScenarioChooser(frame);
        scenarioChooser.setVisible(true);
        if (scenarioChooser.getSelectedScenarioFilename() == null) {
            return;
        }

        Scenario scenario;
        IGame game;
        try {
            ScenarioLoader sl = new ScenarioLoader(new File(scenarioChooser.getSelectedScenarioFilename()));
            scenario = sl.load();
            game = scenario.createGame();
        } catch (Exception e) {
            logger.error(e, Messages.getString("MegaMek.HostScenarioAlert.message", e.getMessage()),
                    Messages.getString("MegaMek.HostScenarioAlert.title"));
            return;
        }

        // popup options dialog
        if (!scenario.hasFixedGameOptions() && game instanceof Game twGame) {
            GameOptionsDialog god = new GameOptionsDialog(frame, (GameOptions) twGame.getOptions(), false);
            god.update((GameOptions) twGame.getOptions());
            god.setEditable(true);
            god.setVisible(true);
            for (IBasicOption opt : god.getOptions()) {
                IOption orig = game.getOptions().getOption(opt.getName());
                orig.setValue(opt.getValue());
            }
        }

        // popup planetary conditions dialog
        if ((game instanceof PlanetaryConditionsUsing) && !scenario.hasFixedPlanetaryConditions()) {
            PlanetaryConditionsUsing plGame = (PlanetaryConditionsUsing) game;
            PlanetaryConditionsDialog pcd = new PlanetaryConditionsDialog(frame, plGame.getPlanetaryConditions());
            pcd.update(plGame.getPlanetaryConditions());
            pcd.setVisible(true);
            plGame.setPlanetaryConditions(pcd.getConditions());
        }

        int port = MMConstants.DEFAULT_PORT;
        String serverPW = "";
        Player[] pa = game.getPlayersList().toArray(new Player[0]);
        int[] playerTypes = new int[pa.length];
        String playerName = pa[0].getName();
        String localName = playerName;
        boolean hasSlot = scenario.isSinglePlayer();

        // get player types and colors set
        if (!scenario.isSinglePlayer()) {
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
            playerTypes[0] = 0;
            for (int i = 1; i < playerTypes.length; i++) {
                playerTypes[i] = ScenarioDialog.T_BOT;
            }
        }

        // kick off a RNG check
        Compute.d6();

        // start server
        if (!startServer(serverPW, port, false, null,
                null, null, scenario.getGameType())) {
            return;
        }
        server.setGame(game);
        scenario.applyDamage(gameManager);

        if (!localName.isBlank()) {
            startClient(playerName, MMConstants.LOCALHOST, port, scenario.getGameType());
        }

        gameManager.calculatePlayerInitialCounts();

        // Setup bots; currently, we have no bot that supports anything other than TW
        if (scenario.getGameType() == GameType.TW) {
            for (int x = 0; x < pa.length; x++) {
                if (playerTypes[x] == ScenarioDialog.T_BOT) {
                    logger.info("Adding bot " + pa[x].getName() + " as Princess");
                    Princess c = new Princess(pa[x].getName(), MMConstants.LOCALHOST, port);
                    if (scenario.hasBotInfo(pa[x].getName())
                            && scenario.getBotInfo(pa[x].getName()) instanceof BotParser.PrincessRecord princessRecord) {
                        c.setBehaviorSettings(princessRecord.behaviorSettings());
                    }
                    c.getGame().addGameListener(new BotGUI(frame, c));
                    c.connect();
                }
            }
        }

        // If he didn't have a name when hasSlot was set, then the host should be an
        // observer.
        if (!hasSlot) {
            for (Player player : server.getGame().getPlayersList()) {
                if (player.getName().equals(localName)) {
                    player.setObserver(true);
                }
            }
        }
    }

    /**
     * Connect to an existing game
     */
    void connect() {
        var cd = new ConnectDialog(frame);
        cd.setVisible(true);
        if (cd.isConfirmed() && cd.dataValidation("MegaMek.ConnectDialog.title")) {
            startClient(cd.getPlayerName(), cd.getServerAddress(), cd.getPort());
        }
    }

    /**
     * Connect to an existing game
     */
    void connectSbf() {
        var cd = new ConnectDialog(frame);
        cd.setVisible(true);
        if (cd.isConfirmed() && cd.dataValidation("MegaMek.ConnectDialog.title")) {
            startClient(cd.getPlayerName(), cd.getServerAddress(), cd.getPort(), GameType.SBF);
        }
    }

    /**
     * Connect to a game as Princess
     */
    void connectBot() {
        var cd = new ConnectDialog(frame);
        cd.setVisible(true);
        if (!cd.isConfirmed() || !cd.dataValidation("MegaMek.ConnectDialog.title")) {
            return;
        }

        // initialize game
        var bcd = new BotConfigDialog(frame, cd.getPlayerName());
        bcd.setVisible(true);
        if (bcd.getResult() == DialogResult.CANCELLED) {
            return;
        }
        client = Princess.createPrincess(bcd.getBotName(), cd.getServerAddress(), cd.getPort(),
                bcd.getBehaviorSettings());
        client.getGame().addGameListener(new BotGUI(frame, (BotClient) client));
        ClientGUI gui = new ClientGUI((Client) client, controller);
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
                    helpUrl, frame);
            helpDialog.setVisible(true);
        } catch (MalformedURLException e) {
            logger.error(e);
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
        MegaMek.getMMPreferences().saveToFile(SuiteConstants.MM_PREFERENCES_FILE);
        PreferenceManager.getInstance().save();

        try {
            WeaponOrderHandler.saveWeaponOrderFile();
        } catch (IOException e) {
            logger.error(e, "Error saving custom weapon orders!");
        }
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
        // noinspection CallToSystemGC
        System.gc();
        // noinspection CallToRunFinalization
        System.runFinalization();
    }

    private final ActionListener actionListener = ev -> {
        if (ev.getActionCommand().startsWith(ClientGUI.BOARD_RECENT)) {
            String recentBoard = ev.getActionCommand().substring(ClientGUI.BOARD_RECENT.length() + 1);
            showEditor(recentBoard);
        }
        switch (ev.getActionCommand()) {
            case ClientGUI.BOARD_NEW:
                showEditor();
                break;
            case ClientGUI.NEW_AI:
                showAiEditor();
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
            case ClientGUI.FILE_GAME_CONNECT_SBF:
                connectSbf();
                break;
            case ClientGUI.FILE_GAME_LOAD:
                loadGame();
                break;
            case ClientGUI.FILE_GAME_QLOAD:
                quickLoadGame();
                break;
            case ClientGUI.HELP_ABOUT:
                new CommonAboutDialog(frame).setVisible(true);
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
            case ClientGUI.FILE_UNITS_BROWSE:
                UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(frame);
                if (!MekSummaryCache.getInstance().isInitialized()) {
                    unitLoadingDialog.setVisible(true);
                }
                MainMenuUnitBrowserDialog unitSelectorDialog = new MainMenuUnitBrowserDialog(frame, unitLoadingDialog);
                new Thread(unitSelectorDialog, "Mek Selector Dialog").start();
                unitSelectorDialog.setVisible(true);
                break;
        }
    };

    @Override
    public void preferenceChange(PreferenceChangeEvent evt) {
        switch (evt.getName()) {
            case GUIPreferences.SKIN_FILE:
                showMainMenu();
                frame.repaint();
                break;
            case GUIPreferences.UI_THEME:
                setLookAndFeel();
                break;
            case GUIPreferences.GUI_SCALE:
                updateGuiScaling();
                break;
        }
    }

    /**
     * Method used to determine the appropriate splash screen to use. This method
     * looks
     * at both the height and the width of the main monitor.
     *
     * @param splashScreens List of available splash screens.
     * @param screenWidth   Width of the current monitor.
     * @param screenHeight  Height of the current monitor.
     * @return String that represents the splash screen that should be displayed.
     */
    private String determineSplashScreen(final List<String> splashScreens,
            final int screenWidth, final int screenHeight) {
        // Ensure that the list is of appropriate size to contain HD, FHD, and UHD
        // splash screens.
        if (splashScreens.size() > 3) {
            // Default to the HD splash screen.
            String splashFileName = splashScreens.get(3);
            if ((screenWidth > 1920) && (screenHeight > 1080)) {
                // If both height and width is greater than 1080p use the UHD splash screen.
                splashFileName = splashScreens.get(2);
            } else if ((screenWidth > 1280) && (screenHeight > 720)) {
                // If both height and width is greater than 720p then use the FHD splash screen.
                splashFileName = splashScreens.get(0);
            }
            return splashFileName;
        }
        // List of splash screens is not complete so default to the first splash screen.
        return splashScreens.get(0);
    }

    private @Nullable Image getSplashScreen(final List<String> splashScreens,
            final int screenWidth, final int screenHeight) {
        String filename = determineSplashScreen(splashScreens, screenWidth, screenHeight);
        File file = new MegaMekFile(Configuration.widgetsDir(), filename).getFile();
        if (!file.exists()) {
            logger.error("MainMenu Error: Splash screen doesn't exist: " + file.getAbsolutePath());
            file = new MegaMekFile(Configuration.widgetsDir(), FILENAME_MEGAMEK_SPLASH).getFile();
        }

        if (!file.exists()) {
            logger
                    .error("MainMenu Error: Backup splash screen doesn't exist: " + file.getAbsolutePath());
            return null;
        }

        Image img = ImageUtil.loadImageFromFile(file.toString());
        // wait for splash image to load completely
        MediaTracker tracker = new MediaTracker(frame);
        tracker.addImage(img, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException ignored) {
            // really should never come here
        }

        return img;
    }

    /**
     * MultiResolutionImage is supposed to allow Swing to choose the right res to
     * display based
     * on DPI, but does not work as expected for ImageIcon
     *
     * @param splashScreens
     * @return
     */
    private BaseMultiResolutionImage getMultiResolutionSplashScreen(final List<String> splashScreens) {
        List<String> filenames = new ArrayList<>();
        if (splashScreens.size() > 3) {
            filenames.add(splashScreens.get(0));
            filenames.add(splashScreens.get(2));
            filenames.add(splashScreens.get(3));
        } else {
            filenames.add(splashScreens.get(0));
        }

        List<Image> images = new ArrayList<>();

        for (String filename : filenames) {
            File file = new MegaMekFile(Configuration.widgetsDir(), filename).getFile();
            if (!file.exists()) {
                logger.error("MainMenu Error: background icon doesn't exist: {}", file.getAbsolutePath());
            } else {
                BufferedImage img = (BufferedImage) ImageUtil.loadImageFromFile(file.toString());
                images.add(img);
                // wait for splash image to load completely
                MediaTracker tracker = new MediaTracker(frame);
                tracker.addImage(img, 0);
                try {
                    tracker.waitForID(0);
                } catch (InterruptedException ignored) {
                    // really should never come here
                }
            }
        }
        return new BaseMultiResolutionImage(images.toArray(new Image[0]));
    }

    public static void updateGuiScaling() {
        System.setProperty("flatlaf.uiScale", Double.toString(GUIPreferences.getInstance().getGUIScale()));
        setLookAndFeel();
    }

    private static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(GUIPreferences.getInstance().getUITheme());
            UIUtil.updateAfterUiChange();
        } catch (Exception ex) {
            logger.error("setLookAndFeel() Exception", ex);
        }
    }
}
