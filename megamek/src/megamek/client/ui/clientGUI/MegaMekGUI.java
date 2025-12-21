/*
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.clientGUI;

import static megamek.common.compute.Compute.d6;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.GZIPInputStream;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;

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
import megamek.client.ui.boardeditor.BoardEditorPanel;
import megamek.client.ui.clientGUI.tooltip.PilotToolTip;
import megamek.client.ui.dialogs.CommonAboutDialog;
import megamek.client.ui.dialogs.ConfirmDialog;
import megamek.client.ui.dialogs.ScenarioDialog;
import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.client.ui.dialogs.buttonDialogs.BotConfigDialog;
import megamek.client.ui.dialogs.buttonDialogs.CommonSettingsDialog;
import megamek.client.ui.dialogs.buttonDialogs.GameOptionsDialog;
import megamek.client.ui.dialogs.clientDialogs.PlanetaryConditionsDialog;
import megamek.client.ui.dialogs.gameConnectionDialogs.ConnectDialog;
import megamek.client.ui.dialogs.gameConnectionDialogs.HostDialog;
import megamek.client.ui.dialogs.helpDialogs.HelpDialog;
import megamek.client.ui.dialogs.helpDialogs.MMReadMeHelpDialog;
import megamek.client.ui.dialogs.scenario.ScenarioChooserDialog;
import megamek.client.ui.dialogs.unitSelectorDialogs.MainMenuUnitBrowserDialog;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.panels.skinEditor.SkinEditorMainGUIPanel;
import megamek.client.ui.util.MegaMekController;
import megamek.client.ui.util.UIUtil;
import megamek.client.ui.widget.MegaMekButton;
import megamek.client.ui.widget.RawImagePanel;
import megamek.client.ui.widget.SkinSpecification;
import megamek.client.ui.widget.SkinSpecification.UIComponents;
import megamek.client.ui.widget.SkinXMLHandler;
import megamek.client.ui.widget.SkinnedJPanel;
import megamek.codeUtilities.StringUtility;
import megamek.common.Configuration;
import megamek.common.KeyBindParser;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.game.Game;
import megamek.common.game.GameType;
import megamek.common.game.IGame;
import megamek.common.interfaces.PlanetaryConditionsUsing;
import megamek.common.jacksonAdapters.BotParser;
import megamek.common.loaders.MekFileParser;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.options.IBasicOption;
import megamek.common.options.IOption;
import megamek.common.preference.IPreferenceChangeListener;
import megamek.common.preference.PreferenceChangeEvent;
import megamek.common.preference.PreferenceManager;
import megamek.common.scenario.Scenario;
import megamek.common.scenario.ScenarioLoader;
import megamek.common.util.EmailService;
import megamek.common.util.ImageUtil;
import megamek.common.util.ManagedVolatileImage;
import megamek.common.util.TipOfTheDay;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.weapons.handlers.WeaponOrderHandler;
import megamek.logging.MMLogger;
import megamek.server.IGameManager;
import megamek.server.Server;
import megamek.server.sbf.SBFGameManager;
import megamek.server.totalWarfare.TWGameManager;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MegaMekGUI implements IPreferenceChangeListener {
    private static final MMLogger LOGGER = MMLogger.create(MegaMekGUI.class);

    private static final String FILENAME_MEGAMEK_SPLASH = "../misc/mm-background.jpg";
    private static final String FILENAME_MEDAL = "../misc/mm-medal.png";
    private static final String FILENAME_LOGO = "../misc/mm-logo.png";
    private static final String FILENAME_ICON_16X16 = "megamek-icon-16x16.png";
    private static final String FILENAME_ICON_32X32 = "megamek-icon-32x32.png";
    private static final String FILENAME_ICON_48X48 = "megamek-icon-48x48.png";
    private static final String FILENAME_ICON_256X256 = "megamek-icon-256x256.png";

    private JFrame frame;
    private IClient client;
    private Server server;
    private IGameManager gameManager;
    private CommonSettingsDialog settingsDialog;
    private ManagedVolatileImage logoImage;
    private ManagedVolatileImage medalImage;
    private TipOfTheDay tipOfTheDay;

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

        // TODO : Move Theme setup to MegaMek::initializeSuiteSetups as part of implementing it in SuiteOptions
        updateGuiScaling();

        // TODO : Move ToolTip setup to MegaMek::initializeSuiteSetups as part of implementing them in SuiteOptions
        ToolTipManager.sharedInstance().setInitialDelay(GUIPreferences.getInstance().getTooltipDelay());
        if (GUIPreferences.getInstance().getTooltipDismissDelay() >= 0) {
            ToolTipManager.sharedInstance().setDismissDelay(GUIPreferences.getInstance().getTooltipDismissDelay());
        }

        frame = new JFrame("MegaMek");
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });

        tipOfTheDay = new TipOfTheDay(Messages.getString("TipOfTheDay.title.text"),
              "megamek.client.TipOfTheDay",
              frame);
        frame.setContentPane(new SkinnedJPanel(UIComponents.MainMenuBorder, 1));

        List<Image> iconList = new ArrayList<>();
        iconList.add(frame.getToolkit()
              .getImage(new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_16X16).toString()));
        iconList.add(frame.getToolkit()
              .getImage(new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_32X32).toString()));
        iconList.add(frame.getToolkit()
              .getImage(new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_48X48).toString()));
        iconList.add(frame.getToolkit()
              .getImage(new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_256X256).toString()));
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
                  Messages.getString("MegaMek.welcome.message"),
                  true);
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
        controller.megaMekGUI = this;
        KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        keyboardFocusManager.addKeyEventDispatcher(controller);
        KeyBindParser.parseKeyBindings(controller);
    }

    public static MegaMekController getKeyDispatcher() {
        return controller;
    }

    private int drawMedal(Graphics2D g2d, int panelWidth, int panelHeight, int padding) {
        int targetMedalWidth = 0;
        if (medalImage == null) {
            return targetMedalWidth; // Skip drawing if medalImage is not initialized
        }
        VolatileImage image = medalImage.getImage();
        if (image.getWidth(null) > 0 && image.getHeight(null) > 0) {
            double medalHeightScalePercent = 0.15; // Medal height as 15% of panel height
            int originalMedalWidth = image.getWidth(null);
            int originalMedalHeight = image.getHeight(null);

            int targetMedalHeight = (int) (panelHeight * medalHeightScalePercent);
            if (targetMedalHeight < 1) {
                targetMedalHeight = 1; // Ensure minimum size
            }

            double scaleFactor = (double) targetMedalHeight / originalMedalHeight;
            targetMedalWidth = (int) (originalMedalWidth * scaleFactor);
            if (targetMedalWidth < 1) {targetMedalWidth = 1;}

            // Position: bottom-right corner with padding
            int medalX = panelWidth - targetMedalWidth - padding;
            int medalY = panelHeight - targetMedalHeight - padding;

            if (medalX < 0) {medalX = 0;}
            if (medalY < 0) {medalY = 0;}

            g2d.drawImage(image, medalX, medalY, targetMedalWidth, targetMedalHeight, null);
        }
        return targetMedalWidth;
    }

    private void drawLogo(Graphics2D g2d, int panelWidth, int panelHeight, int targetMedalWidth, int padding) {
        VolatileImage image = logoImage.getImage();
        if (image.getWidth(null) > 0 && image.getHeight(null) > 0) {
            double logoWidthScalePercent = 0.25; // Logo width as 25% of panel width

            int originalLogoWidth = image.getWidth(null);
            int originalLogoHeight = image.getHeight(null);

            int targetLogoWidth = (int) (panelWidth * logoWidthScalePercent);
            if (targetLogoWidth < 1) {
                targetLogoWidth = 1; // Ensure minimum size
            }

            double scaleFactor = (double) targetLogoWidth / originalLogoWidth;
            int targetLogoHeight = (int) (originalLogoHeight * scaleFactor);
            if (targetLogoHeight < 1) {targetLogoHeight = 1;}

            // Position: bottom-right corner with padding (after the medal)
            int logoX = panelWidth - targetLogoWidth - padding - targetMedalWidth - padding;
            int logoY = panelHeight - targetLogoHeight - padding;

            if (logoX < 0) {logoX = 0;}
            if (logoY < 0) {logoY = 0;}

            g2d.drawImage(image, logoX, logoY, targetLogoWidth, targetLogoHeight, null);
        }
    }

    /**
     * Display the main menu.
     */
    private void showMainMenu() {
        SkinSpecification skinSpec = SkinXMLHandler.getSkin(UIComponents.MainMenuBorder.getComp(), true);
        frame.getContentPane().removeAll();
        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);
        frame.setResizable(false);

        JLabel labVersion = new JLabel(Messages.getString("MegaMek.Version") + MMConstants.VERSION, JLabel.CENTER);
        labVersion.setPreferredSize(new Dimension(250, 15));
        if (!skinSpec.fontColors.isEmpty()) {
            labVersion.setForeground(skinSpec.fontColors.get(0));
        }
        MegaMekButton hostB = new MegaMekButton(Messages.getString("MegaMek.hostNewGame.label"),
              UIComponents.MainMenuButton.getComp(),
              true);
        hostB.setActionCommand(ClientGUI.FILE_GAME_NEW);
        hostB.addActionListener(actionListener);
        MegaMekButton scenarioB = new MegaMekButton(Messages.getString("MegaMek.hostScenario.label"),
              UIComponents.MainMenuButton.getComp(),
              true);
        scenarioB.setActionCommand(ClientGUI.FILE_GAME_SCENARIO);
        scenarioB.addActionListener(actionListener);
        MegaMekButton loadB = new MegaMekButton(Messages.getString("MegaMek.hostSavedGame.label"),
              UIComponents.MainMenuButton.getComp(),
              true);
        loadB.setActionCommand(ClientGUI.FILE_GAME_LOAD);
        loadB.addActionListener(actionListener);
        MegaMekButton connectB = new MegaMekButton(Messages.getString("MegaMek.Connect.label"),
              UIComponents.MainMenuButton.getComp(),
              true);
        connectB.setActionCommand(ClientGUI.FILE_GAME_CONNECT);
        connectB.addActionListener(actionListener);
        MegaMekButton connectSBF = new MegaMekButton("Connect to SBF", UIComponents.MainMenuButton.getComp(), true);
        connectSBF.setActionCommand(ClientGUI.FILE_GAME_CONNECT_SBF);
        connectSBF.addActionListener(actionListener);
        MegaMekButton botB = new MegaMekButton(Messages.getString("MegaMek.ConnectAsBot.label"),
              UIComponents.MainMenuButton.getComp(),
              true);
        botB.setActionCommand(ClientGUI.FILE_GAME_CONNECT_BOT);
        botB.addActionListener(actionListener);
        MegaMekButton editB = new MegaMekButton(Messages.getString("MegaMek.MapEditor.label"),
              UIComponents.MainMenuButton.getComp(),
              true);
        editB.setActionCommand(ClientGUI.BOARD_NEW);
        editB.addActionListener(actionListener);
        MegaMekButton skinEditB = new MegaMekButton(Messages.getString("MegaMek.SkinEditor.label"),
              UIComponents.MainMenuButton.getComp(),
              true);
        skinEditB.setActionCommand(ClientGUI.MAIN_SKIN_NEW);
        skinEditB.addActionListener(actionListener);
        MegaMekButton quitB = new MegaMekButton(Messages.getString("MegaMek.Quit.label"),
              UIComponents.MainMenuButton.getComp(),
              true);
        quitB.setActionCommand(ClientGUI.MAIN_QUIT);
        quitB.addActionListener(actionListener);

        // Use the current monitor, so we don't "overflow" computers whose primary
        // displays aren't as large as their secondary displays.
        Dimension scaledMonitorSize = UIUtil.getScaledScreenSize(frame);
        Image splashImage = getImage(FILENAME_MEGAMEK_SPLASH, scaledMonitorSize.width, scaledMonitorSize.height);
        logoImage = new ManagedVolatileImage(getImage(FILENAME_LOGO, scaledMonitorSize.width, scaledMonitorSize.height),
              Transparency.TRANSLUCENT);
        medalImage = new ManagedVolatileImage(getImage(FILENAME_MEDAL,
              scaledMonitorSize.width,
              scaledMonitorSize.height), Transparency.TRANSLUCENT);
        Dimension splashPanelPreferredSize = calculateSplashPanelPreferredSize(scaledMonitorSize, splashImage);
        // This is an empty panel that will contain the splash image
        // Draw background, border, and children first
        // Draw Tip of the Day
        // Absolute drawing position
        // Draw medalImage
        // Draw logoImage
        RawImagePanel splashPanel = getRawImagePanel(splashImage, splashPanelPreferredSize);

        FontMetrics metrics = hostB.getFontMetrics(loadB.getFont());
        int width = metrics.stringWidth(hostB.getText());
        int height = metrics.getHeight();
        Dimension textDim = new Dimension(width + 50, height + 10);

        // Strive for no more than ~90% of the screen and use golden ratio to make
        // the button width "look" reasonable.
        int maximumWidth = (int) (0.9 * scaledMonitorSize.width) - splashPanel.getPreferredSize().width;

        //no more than 50% of image width
        if (maximumWidth > (int) (0.5 * splashPanel.getPreferredSize().width)) {
            maximumWidth = (int) (0.5 * splashPanel.getPreferredSize().width);
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
        scenarioB.setPreferredSize(minButtonDim);
        loadB.setPreferredSize(minButtonDim);
        quitB.setPreferredSize(minButtonDim);
        hostB.setPreferredSize(minButtonDim);

        connectB.setMinimumSize(minButtonDim);
        botB.setMinimumSize(minButtonDim);
        editB.setMinimumSize(minButtonDim);
        skinEditB.setMinimumSize(minButtonDim);
        scenarioB.setMinimumSize(minButtonDim);
        loadB.setMinimumSize(minButtonDim);
        quitB.setMinimumSize(minButtonDim);

        // layout
        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        frame.getContentPane().setLayout(gridBagLayout);
        // Left Column
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 0, 10);
        c.gridx = 0;
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 3.0;
        c.weighty = 1.0;
        c.gridwidth = 1;
        c.gridheight = 9;
        addBag(splashPanel, gridBagLayout, c);
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
        addBag(labVersion, gridBagLayout, c);
        c.gridy++;
        addBag(hostB, gridBagLayout, c);
        c.gridy++;
        addBag(loadB, gridBagLayout, c);
        c.gridy++;
        addBag(scenarioB, gridBagLayout, c);
        c.gridy++;
        addBag(connectB, gridBagLayout, c);
        c.gridy++;
        addBag(editB, gridBagLayout, c);
        c.gridy++;
        addBag(skinEditB, gridBagLayout, c);
        c.gridy++;
        c.insets = new Insets(4, 4, 15, 12);
        addBag(quitB, gridBagLayout, c);
        frame.validate();
        frame.pack();
        // center window in screen
        frame.setLocationRelativeTo(null);
    }

    private RawImagePanel getRawImagePanel(Image splashImage, Dimension splashPanelPreferredSize) {
        RawImagePanel splashPanel = new RawImagePanel(splashImage) {
            @Override
            public void paint(Graphics g) {
                super.paint(g); // Draw background, border, and children first
                Graphics2D g2d = (Graphics2D) g.create();
                try {
                    int panelWidth = this.getWidth();
                    int panelHeight = this.getHeight();
                    int padding = 20;
                    int targetMedalWidth;

                    // Draw Tip of the Day
                    if (tipOfTheDay != null) {
                        // Absolute drawing position
                        Rectangle bounds = this.getBounds();
                        bounds.x = 0;
                        bounds.y = 0;
                        tipOfTheDay.drawTipOfTheDay(g2d, bounds, TipOfTheDay.Position.BOTTOM_LEFT_CORNER, false);
                    }

                    // Draw medalImage
                    targetMedalWidth = drawMedal(g2d, panelWidth, panelHeight, padding);

                    // Draw logoImage
                    drawLogo(g2d, panelWidth, panelHeight, targetMedalWidth, padding);
                } finally {
                    g2d.dispose();
                }
            }
        };
        splashPanel.setPreferredSize(splashPanelPreferredSize);
        return splashPanel;
    }

    /**
     * Calculates the preferred size for the splash panel
     *
     * @param scaledMonitorSize the scaled monitor dimensions
     * @param splashImage       the reference image for the aspect ratio
     *
     * @return the calculated preferred size for the splash panel
     */
    private Dimension calculateSplashPanelPreferredSize(Dimension scaledMonitorSize, Image splashImage) {
        // Calculate max dimensions (75% of screen)
        int maxWidth = (int) (scaledMonitorSize.width * 0.75);
        int maxHeight = (int) (scaledMonitorSize.height * 0.75);

        if (splashImage != null && splashImage.getWidth(null) > 0 && splashImage.getHeight(null) > 0) {
            // Calculate aspect ratio preserving dimensions
            double imageWidth = splashImage.getWidth(null);
            double imageHeight = splashImage.getHeight(null);
            double imageAspectRatio = imageWidth / imageHeight;

            int targetWidth = maxWidth;
            int targetHeight = (int) (targetWidth / imageAspectRatio);

            if (targetHeight > maxHeight) {
                targetHeight = maxHeight;
                targetWidth = (int) (targetHeight * imageAspectRatio);
            }

            return new Dimension(targetWidth, targetHeight);
        } else {
            // Fallback to original calculation if image is not available
            return new Dimension(maxWidth, maxHeight);
        }
    }

    /**
     * Display the board editor.
     */
    void showEditor() {
        BoardEditorPanel editor = new BoardEditorPanel(controller);
        controller.boardEditor = editor;
        launch(editor.getFrame());
        editor.boardNew(GUIPreferences.getInstance().getBoardEdRndStart());
    }

    /**
     * Display the board editor and load the given board
     */
    void showEditor(String boardFile) {
        BoardEditorPanel editor = new BoardEditorPanel(controller);
        controller.boardEditor = editor;
        launch(editor.getFrame());
        editor.loadBoard(new File(boardFile));
    }

    void showSkinEditor() {
        int response = JOptionPane.showConfirmDialog(frame,
              "The skin editor is currently " +
                    "in beta and is a work in progress.  There are likely to " +
                    "be issues. \nContinue?",
              "Continue?",
              JOptionPane.OK_CANCEL_OPTION);
        if (response == JOptionPane.CANCEL_OPTION) {
            return;
        }
        SkinEditorMainGUIPanel skinEditor = new SkinEditorMainGUIPanel();
        skinEditor.initialize();
        launch(skinEditor.getFrame());
    }

    /**
     * Display the board editor and open an "open" dialog.
     */
    void showEditorOpen() {
        BoardEditorPanel editor = new BoardEditorPanel(controller);
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

        startHost(hd.getServerPass(),
              hd.getPort(),
              hd.isRegister(),
              hd.isRegister() ? hd.getMetaserver() : "",
              null,
              null,
              hd.getPlayerName());
    }

    public void startHost(@Nullable String serverPassword, int port, boolean isRegister, @Nullable String metaServer,
          @Nullable String mailPropertiesFileName, @Nullable File saveGame, String playerName) {
        if (!startServer(serverPassword, port, isRegister, metaServer, mailPropertiesFileName, saveGame)) {
            return;
        }

        startClient(playerName, MMConstants.LOCALHOST, server.getPort());
    }

    public boolean startServer(@Nullable String serverPassword, int port, boolean isRegister,
          @Nullable String metaServer, @Nullable String mailPropertiesFileName, @Nullable File saveGameFile) {
        return startServer(serverPassword,
              port,
              isRegister,
              metaServer,
              mailPropertiesFileName,
              saveGameFile,
              GameType.TW);
    }

    public boolean startServer(@Nullable String serverPassword, int port, boolean isRegister,
          @Nullable String metaServer, @Nullable String mailPropertiesFileName, @Nullable File saveGameFile,
          GameType gameType) {
        try {
            serverPassword = Server.validatePassword(serverPassword);
            port = Server.validatePort(port);
        } catch (Exception ex) {
            LOGGER.error("Failed to start Server", ex);
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
                LOGGER.errorDialog(ex,
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
            LOGGER.errorDialog(ex,
                  Messages.getFormattedString("MegaMek.StartServerError", port, ex.getMessage()),
                  Messages.getString("MegaMek.LoadGameAlert.title"));
            return false;
        }

        if (saveGameFile != null && !server.loadGame(saveGameFile)) {
            LOGGER.errorDialog(Messages.getString("MegaMek.LoadGameAlert.title"),
                  Messages.getFormattedString("MegaMek.LoadGameAlert.message", saveGameFile.getAbsolutePath()));
            server.die();
            server = null;
            return false;
        }

        return true;
    }

    private IGameManager getGameManager(GameType gameType) {
        if (gameType == GameType.SBF) {
            return new SBFGameManager();
        }

        return new TWGameManager();
    }

    private IClientGUI getClientGUI(GameType gameType, IClient client, MegaMekController controller) {
        if (gameType == GameType.SBF) {
            return new SBFClientGUI((SBFClient) client, controller);
        }

        return new ClientGUI((Client) client, controller);
    }

    private IClient getClient(GameType gameType, String playerName, String host, int port) {
        if (gameType == GameType.SBF) {
            return new SBFClient(playerName, host, port);
        }

        return new Client(playerName, host, port);
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
            LOGGER.errorDialog(ex,
                  Messages.getFormattedString("MegaMek.ServerConnectionError", serverAddress, port),
                  Messages.getString("MegaMek.LoadGameAlert.title"));
            return;
        }

        // delete PilotToolTip cache
        PilotToolTip.deleteImageCache();
        client = getClient(gameType, playerName, serverAddress, port);
        IClientGUI gui = getClientGUI(gameType, client, controller);
        controller.clientGUI = gui;
        frame.setCursor(new Cursor(Cursor.WAIT_CURSOR));
        gui.initialize();
        frame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        if (!client.connect()) {
            LOGGER.errorDialog(Messages.getString("MegaMek.LoadGameAlert.title"),
                  Messages.getFormattedString("MegaMek.ServerConnectionError", serverAddress, port));
            client.die();
            gui.die();
            return;
        }

        // free some memory that's only needed in lounge This normally happens in the deployment phase in Client, but
        // if we are loading a game, this phase may not be reached
        MekFileParser.dispose();
        // We must do this last, as the name and unit generators can create a new instance if they are running
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
                return dir.getName().endsWith(MMConstants.SAVE_FILE_EXT) ||
                      dir.getName().endsWith(MMConstants.SAVE_FILE_GZ_EXT) ||
                      dir.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Savegames";
            }
        });
        int returnVal = fc.showOpenDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
            return;
        }

        final Vector<String> playerNames = new Vector<>();

        // Hand rolled extraction, as we require Server initialization to use XStream and don't need the additional
        // overhead of initializing everything twice
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
            LOGGER.errorDialog(ex,
                  Messages.getFormattedString("MegaMek.LoadGameAlert.message", fc.getSelectedFile().getAbsolutePath()),
                  Messages.getString("MegaMek.LoadGameAlert.title"));
            return;
        }

        HostDialog hd = new HostDialog(frame, playerNames);
        hd.setVisible(true);

        if (!hd.dataValidation("MegaMek.LoadGameAlert.title")) {
            return;
        }

        startHost(hd.getServerPass(),
              hd.getPort(),
              hd.isRegister(),
              hd.isRegister() ? hd.getMetaserver() : "",
              null,
              fc.getSelectedFile(),
              hd.getPlayerName());
    }

    private boolean validateSaveVersion(final Node n) {
        String ignoreVersionValidation = System.getenv("IGNORE_VERSION_VALIDATION");
        if (ignoreVersionValidation != null && ignoreVersionValidation.equalsIgnoreCase("true")) {
            return true;
        }

        if (!n.hasChildNodes()) {
            final String message = String.format(Messages.getString("MegaMek.LoadGameMissingVersion.message"),
                  SuiteConstants.VERSION);
            JOptionPane.showMessageDialog(frame,
                  message,
                  Messages.getString("MegaMek.LoadGameAlert.title"),
                  JOptionPane.ERROR_MESSAGE);
            LOGGER.error(message);
            return false;
        }

        final Version version = getVersion(n);
        if (SuiteConstants.VERSION.is(version)) {
            return true;
        } else if (version.toString().toLowerCase().contains("nightly") &&
              (version.getMajor() == SuiteConstants.VERSION.getMajor() &&
                    version.getMinor() == SuiteConstants.VERSION.getMinor() &&
                    version.getPatch() == SuiteConstants.VERSION.getPatch())
              && version.toString().contains(SuiteConstants.VERSION.toString())) {
            // Nightly version of current development version
            return true;
        } else {
            LOGGER.errorDialog(Messages.getString("MegaMek.LoadGameAlert.title"),
                  Messages.getString("MegaMek.LoadGameIncorrectVersion.message"),
                  version,
                  SuiteConstants.VERSION);
            return false;
        }
    }

    private static Version getVersion(Node n) {
        final NodeList nl = n.getChildNodes();
        String major = null;
        String minor = null;
        String patch = null;
        String extra = null;
        for (int i = 0; i < nl.getLength(); i++) {
            final Node n2 = nl.item(i);
            if (n2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            switch (n2.getNodeName()) {
                case "major":
                    major = n2.getTextContent();
                    break;
                case "minor":
                    minor = n2.getTextContent();
                    break;
                case "patch":
                    patch = n2.getTextContent();
                    break;
                case "snapshot":
                case "extra":
                    extra = n2.getTextContent();
                    break;
                default:
                    break;
            }
        }

        return new Version(major, minor, patch, extra);
    }

    private void parsePlayerNames(final Node nodePlayers, final Vector<String> playerNames) {
        if (!nodePlayers.hasChildNodes()) {
            return;
        }

        final NodeList nodePlayersChildren = nodePlayers.getChildNodes();
        for (int i = 0; i < nodePlayersChildren.getLength(); i++) {
            final Node nodeEntry = nodePlayersChildren.item(i);
            if ((nodeEntry.getNodeType() != Node.ELEMENT_NODE) ||
                  !nodeEntry.hasChildNodes() ||
                  !"entry".equals(nodeEntry.getNodeName())) {
                continue;
            }

            final NodeList nodeEntryChildren = nodeEntry.getChildNodes();
            for (int k = 0; k < nodeEntryChildren.getLength(); k++) {
                final Node nodePlayerClass = nodeEntryChildren.item(k);
                if ((nodePlayerClass.getNodeType() != Node.ELEMENT_NODE) ||
                      !nodePlayerClass.hasChildNodes() ||
                      !Player.class.getName().equals(nodePlayerClass.getNodeName())) {
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
     * Developer Utility: Loads "quicksave.sav.gz" with the last used connection settings.
     */
    public void quickLoadGame() {
        File file = MegaMek.getQuickSaveFile();
        if (!file.exists() || !file.canRead()) {
            JOptionPane.showMessageDialog(frame,
                  Messages.getFormattedString("MegaMek.LoadGameAlert.message", file.getAbsolutePath()),
                  Messages.getString("MegaMek.LoadGameAlert.title"),
                  JOptionPane.ERROR_MESSAGE);
            frame.setVisible(true);
            return;
        }

        startHost("", 0, false, "", null, file, PreferenceManager.getClientPreferences().getLastPlayerName());
    }

    /**
     * Loads a specific save game file without showing the HostDialog. Used when loading from the lobby where server
     * settings are not needed.
     *
     * @param file The save game file to load
     */
    public void loadGameFile(File file) {
        if (file == null || !file.exists() || !file.canRead()) {
            JOptionPane.showMessageDialog(frame,
                  Messages.getFormattedString("MegaMek.LoadGameAlert.message",
                        file != null ? file.getAbsolutePath() : "null"),
                  Messages.getString("MegaMek.LoadGameAlert.title"),
                  JOptionPane.ERROR_MESSAGE);
            frame.setVisible(true);
            return;
        }

        startHost("", 0, false, "", null, file, PreferenceManager.getClientPreferences().getLastPlayerName());
    }

    /**
     * Host a game constructed from a scenario file
     */
    void scenario(String fileName) {
        String chosenFileName;
        if (fileName.isBlank()) {
            ScenarioChooserDialog scenarioChooser = new ScenarioChooserDialog(frame);
            scenarioChooser.setVisible(true);
            chosenFileName = scenarioChooser.getSelectedScenarioFilename();
            if (chosenFileName == null) {
                return;
            }
            PreferenceManager.getClientPreferences().setLastScenario(chosenFileName);
        } else {
            chosenFileName = fileName;
        }

        Scenario scenario;
        IGame game;
        try {
            ScenarioLoader sl = new ScenarioLoader(new File(chosenFileName));
            scenario = sl.load();
            game = scenario.createGame();
        } catch (Exception e) {
            LOGGER.errorDialog(e,
                  Messages.getString("MegaMek.HostScenarioAlert.message", e.getMessage()),
                  Messages.getString("MegaMek.HostScenarioAlert.title"));
            return;
        }

        // popup options dialog
        if (!scenario.hasFixedGameOptions() && game instanceof Game twGame) {
            GameOptionsDialog god = new GameOptionsDialog(frame, twGame.getOptions(), false);
            god.update(twGame.getOptions());
            god.setEditable(true);
            god.setVisible(true);
            for (IBasicOption opt : god.getOptions()) {
                IOption orig = game.getOptions().getOption(opt.getName());
                orig.setValue(opt.getValue());
            }
        }

        // popup planetary conditions dialog
        if ((game instanceof PlanetaryConditionsUsing plGame) && !scenario.hasFixedPlanetaryConditions()) {
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
        if (!startServer(serverPW, port, false, null, null, null, scenario.getGameType())) {
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
                    LOGGER.info("Adding bot {} as Princess", pa[x].getName());
                    Princess c = new Princess(pa[x].getName(), MMConstants.LOCALHOST, port);
                    c.startPrecognition();
                    if (scenario.hasBotInfo(pa[x].getName()) &&
                          scenario.getBotInfo(pa[x].getName()) instanceof BotParser.PrincessRecord princessRecord) {
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
        client = Princess.createPrincess(bcd.getBotName(),
              cd.getServerAddress(),
              cd.getPort(),
              bcd.getBehaviorSettings());
        client.getGame().addGameListener(new BotGUI(frame, (BotClient) client));
        ClientGUI gui = new ClientGUI((Client) client, controller);
        controller.clientGUI = gui;
        gui.initialize();
        if (!client.connect()) {
            JOptionPane.showMessageDialog(frame,
                  Messages.getFormattedString("MegaMek.ServerConnectionError", cd.getServerAddress(), cd.getPort()),
                  Messages.getString("MegaMek.ConnectDialog.title"),
                  JOptionPane.ERROR_MESSAGE);
            frame.setVisible(false);
            client.die();
        }
        launch(gui.getFrame());
    }

    private void addBag(JComponent jComponent, GridBagLayout gridBagLayout, GridBagConstraints gridBagConstraints) {
        gridBagLayout.setConstraints(jComponent, gridBagConstraints);
        frame.getContentPane().add(jComponent);
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
            HelpDialog helpDialog = new HelpDialog(Messages.getString("ClientGUI.skinningHelpPath.title"),
                  helpUrl,
                  frame);
            helpDialog.setVisible(true);
        } catch (MalformedURLException e) {
            LOGGER.error(e);
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
            LOGGER.error(e, "Error saving custom weapon orders!");
        }
        System.exit(0);
    }

    /**
     * Hides this window for later. Listens to the frame until it closes, then calls unlaunch().
     */
    private void launch(JFrame launched) {
        // Stop tip cycling when launching another window
        if (tipOfTheDay != null) {
            tipOfTheDay.stopCycling();
        }
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
        // Stop tip cycling when leaving main menu
        if (tipOfTheDay != null) {
            tipOfTheDay.stopCycling();
        }
        // show menu frame
        frame.setVisible(true);

        // Check for post-unlaunch action (e.g., load game from lobby)
        Runnable postAction = controller.consumePostUnlaunchAction();
        if (postAction != null) {
            SwingUtilities.invokeLater(postAction);
        }

        // just to free some memory
        client = null;
        System.gc();
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
                if ((ev.getModifiers() & Event.CTRL_MASK) != 0) {
                    // As a dev convenience, start the last scenario again when clicked with CTRL
                    scenario(PreferenceManager.getClientPreferences().getLastScenario());
                } else {
                    scenario("");
                }
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
            case ClientGUI.FILE_GAME_QUICK_LOAD:
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

    private @Nullable Image getImage(final String filename, final int screenWidth, final int screenHeight) {
        File file = new MegaMekFile(Configuration.widgetsDir(), filename).getFile();
        if (!file.exists()) {
            LOGGER.error("MainMenu Error: Image doesn't exist: {}", file.getAbsolutePath());
            return null;
        }
        Image img = ImageUtil.loadImageFromFile(file.toString());
        // wait for image to load completely
        MediaTracker tracker = new MediaTracker(frame);
        tracker.addImage(img, 0);
        try {
            tracker.waitForID(0);
        } catch (InterruptedException ignored) {
            // really should never come here
        }

        return img;
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
            LOGGER.error("setLookAndFeel() Exception", ex);
        }
    }
}
