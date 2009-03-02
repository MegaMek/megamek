/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Label;
import java.awt.Panel;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import megamek.client.Client;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.IBoardView;
import megamek.client.ui.Messages;
import megamek.client.ui.AWT.util.PlayerColors;
import megamek.client.ui.AWT.widget.BackGroundDrawer;
import megamek.client.ui.AWT.widget.BufferedPanel;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.IGame;
import megamek.common.MechSummaryCache;
import megamek.common.Player;
import megamek.common.event.GameEndEvent;
import megamek.common.event.GameListener;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.GameMapQueryEvent;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.event.GamePlayerConnectedEvent;
import megamek.common.event.GamePlayerDisconnectedEvent;
import megamek.common.event.GameReportEvent;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.util.Distractable;
import megamek.common.util.StringUtil;

public class ClientGUI extends Panel implements WindowListener, ActionListener, BoardViewListener {
    /**
     *
     */
    private static final long serialVersionUID = 8010157442415211490L;
    // Action commands.
    public static final String VIEW_MEK_DISPLAY = "viewMekDisplay"; //$NON-NLS-1$
    public static final String VIEW_MINI_MAP = "viewMiniMap"; //$NON-NLS-1$
    public static final String VIEW_LOS_SETTING = "viewLOSSetting"; //$NON-NLS-1$
    public static final String VIEW_UNIT_OVERVIEW = "viewUnitOverview"; //$NON-NLS-1$
    public static final String VIEW_ZOOM_IN = "viewZoomIn"; //$NON-NLS-1$
    public static final String VIEW_ZOOM_OUT = "viewZoomOut"; //$NON-NLS-1$

    // a frame, to show stuff in
    public Frame frame;

    // A menu bar to contain all actions.
    protected CommonMenuBar menuBar = null;
    private CommonAboutDialog about = null;
    private CommonHelpDialog help = null;
    private CommonSettingsDialog setdlg = null;
    private String helpFileName = "readme.txt"; //$NON-NLS-1$

    // keep me
    ChatterBox cb;
    public IBoardView bv;
    private Component bvc;
    public Dialog mechW;
    public MechDisplay mechD;
    public Dialog minimapW;
    public MiniMap minimap;
    public PopupMenu popup;// = new PopupMenu(Messages.getString("ClientGUI.BoardPopup")); //$NON-NLS-1$
    private UnitOverview uo;
    public Ruler ruler; // added by kenn
    protected Component curPanel;
    public ChatLounge chatlounge = null;

    // some dialogs...
    BoardSelectionDialog boardSelectionDialog;
    GameOptionsDialog gameOptionsDialog;
    private MechSelectorDialog mechSelectorDialog;
    private CustomBattleArmorDialog customBADialog;
    private CustomFighterSquadronDialog customFSDialog;
    private StartingPositionDialog startingPositionDialog;
    private PlayerListDialog playerListDialog;
    private RandomArmyDialog randomArmyDialog;
    private RandomSkillDialog randomSkillDialog;
    private CustomInitiativeDialog initDialog;
    private PlanetaryConditionsDialog conditionsDialog;

    /**
     * Save and Open dialogs for MegaMek Unit List (mul) files.
     */
    private FileDialog dlgLoadList = null;
    private FileDialog dlgSaveList = null;

    public Client client;

    /**
     * Cache for the "bing" soundclip.
     */
    AudioClip bingClip = null;

    /**
     * Map each phase to the name of the card for the main display area.
     */
    private HashMap<String, String> mainNames = new HashMap<String, String>();

    /**
     * The <code>Panel</code> containing the main display area.
     */
    private Panel panMain = new Panel();

    /**
     * The <code>CardLayout</code> of the main display area.
     */
    private CardLayout cardsMain = new CardLayout();

    /**
     * Map each phase to the name of the card for the secondary area.
     */
    private HashMap<String, String> secondaryNames = new HashMap<String, String>();

    /**
     * The <code>Panel</code> containing the secondary display area.
     */
    private Panel panSecondary = new Panel();

    /**
     * The <code>CardLayout</code> of the secondary display area.
     */
    private CardLayout cardsSecondary = new CardLayout();

    /**
     * Map phase component names to phase component objects.
     */
    HashMap<String, Component> phaseComponents = new HashMap<String, Component>();

    // TODO: there's a better place for this
    private Map<String, Client> bots = new TreeMap<String, Client>(StringUtil
            .stringComparator());

    /**
     * Current Selected entity
     */
    int selectedEntityNum = Entity.NONE;

    /**
     * Construct a client which will display itself in a new frame. It will not
     * try to connect to a server yet. When the frame closes, this client will
     * clean up after itself as much as possible, but will not call
     * System.exit().
     */
    public ClientGUI(Client client) {
        super(new BorderLayout());
        this.client = client;
        loadSoundClip();
        panMain.setLayout(cardsMain);
        panSecondary.setLayout(cardsSecondary);
        Panel panDisplay = new Panel(new BorderLayout());
        panDisplay.add(panMain, BorderLayout.CENTER);
        panDisplay.add(panSecondary, BorderLayout.SOUTH);
        this.add(panDisplay, BorderLayout.CENTER);
    }

    public IBoardView getBoardView() {
        return bv;
    }

    /*
     * Try to load the "bing" sound clip.
     */
    public void loadSoundClip() {
        if (GUIPreferences.getInstance().getSoundBingFilename() == null) {
            return;
        }
        try {
            File file = new File(GUIPreferences.getInstance()
                    .getSoundBingFilename());
            if (!file.exists()) {
                System.err
                        .println("Failed to load audio file: " + GUIPreferences.getInstance().getSoundBingFilename()); //$NON-NLS-1$
                return;
            }
            bingClip = Applet.newAudioClip(file.toURI().toURL());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Display a system message in the chat box.
     *
     * @param message the <code>String</code> message to be shown.
     */
    public void systemMessage(String message) {
        cb.systemMessage(message);
    }

    /**
     * Initializes a number of things about this frame.
     */
    private void initializeFrame() {
        frame = new Frame(Messages.getString("ClientGUI.title")); //$NON-NLS-1$
        menuBar.setGame(client.game);
        frame.setMenuBar(menuBar);
        Rectangle virtualBounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (int j = 0; j < gs.length; j++) {
            GraphicsDevice gd = gs[j];
            GraphicsConfiguration[] gc = gd.getConfigurations();
            for (int i = 0; i < gc.length; i++) {
                virtualBounds = virtualBounds.union(gc[i].getBounds());
            }
        }
        if (GUIPreferences.getInstance().getWindowSizeHeight() != 0) {
            int x = GUIPreferences.getInstance().getWindowPosX();
            int y = GUIPreferences.getInstance().getWindowPosY();
            int w = GUIPreferences.getInstance().getWindowSizeWidth();
            int h = GUIPreferences.getInstance().getWindowSizeHeight();
            if ((x < virtualBounds.getMinX()) || (x + w > virtualBounds.getMaxX())) {
                x = 0;
            }
            if ((y < virtualBounds.getMinY()) || (y + h > virtualBounds.getMaxY())) {
                y = 0;
            }
            if (w > virtualBounds.getWidth()) {
                w = (int) virtualBounds.getWidth();
            }
            if (h > virtualBounds.getHeight()) {
                h = (int) virtualBounds.getHeight();
            }
            frame.setLocation(x, y);
            frame.setSize(w, h);
        } else {
            frame.setSize(800, 600);
        }

        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);

        frame.setIconImage(frame.getToolkit().getImage(
                "data/images/misc/megamek-icon.gif")); //$NON-NLS-1$
    }

    /**
     * Lays out the frame by setting this Client object to take up the full
     * frame display area.
     */
    private void layoutFrame() {
        frame.setTitle(client.getName()
                + Messages.getString("ClientGUI.clientTitleSuffix")); //$NON-NLS-1$
        frame.setLayout(new BorderLayout());
        frame.add(this, BorderLayout.CENTER);
        frame.validate();
    }

    /**
     * Have the client register itself as a listener wherever it's needed. <p/>
     * According to
     * http://www-106.ibm.com/developerworks/java/library/j-jtp0618.html it is a
     * major bad no-no to perform these registrations before the constructor
     * finishes, so this function has to be called after the <code>Client</code>
     * is created.
     */
    public void initialize() {
        menuBar = new CommonMenuBar(getClient());
        initializeFrame();

        try {
            client.game.addGameListener(gameListener);
            // Create the board viewer.
            Class<?> c = getClass().getClassLoader().loadClass(System.getProperty("megamek.client.ui.AWT.boardView", "megamek.client.ui.AWT.BoardView1"));
            bv = (IBoardView)c.getConstructor(IGame.class).newInstance(client.game);
            bvc = bv.getComponent();
            bv.addBoardViewListener(this);

        } catch (Exception e) {
            e.printStackTrace();
            doAlertDialog(
                    Messages.getString("ClientGUI.FatalError.title"), Messages.getString("ClientGUI.FatalError.message") + e); //$NON-NLS-1$ //$NON-NLS-2$
            die();
        }
        layoutFrame();

        frame.setVisible(true);

        menuBar.addActionListener(this);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                frame.setVisible(false);
                saveSettings();
                die();
            }
        });

        UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(frame);
        if (!MechSummaryCache.getInstance().isInitialized()) {
            unitLoadingDialog.setVisible(true);
        }

        uo = new UnitOverview(this);
        bv.addDisplayable(uo);

        Dimension screenSize = frame.getToolkit().getScreenSize();
        int x, y, h, w;

        mechW = new Dialog(frame,
                Messages.getString("ClientGUI.MechDisplay"), false); //$NON-NLS-1$
        x = GUIPreferences.getInstance().getDisplayPosX();
        y = GUIPreferences.getInstance().getDisplayPosY();
        h = GUIPreferences.getInstance().getDisplaySizeHeight();
        w = GUIPreferences.getInstance().getDisplaySizeWidth();
        if (x + w > screenSize.width) {
            x = 0;
            w = Math.min(w, screenSize.width);
        }
        if (y + h > screenSize.height) {
            y = 0;
            h = Math.min(h, screenSize.height);
        }
        mechW.setLocation(x, y);
        mechW.setSize(w, h);
        mechW.setResizable(true);
        mechW.addWindowListener(this);
        mechD = new MechDisplay(this);
        mechD.addMechDisplayListener(bv);
        mechW.add(mechD);

        // added by kenn
        Ruler.color1 = GUIPreferences.getInstance().getRulerColor1();
        Ruler.color2 = GUIPreferences.getInstance().getRulerColor2();
        ruler = new Ruler(frame, client, bv);
        x = GUIPreferences.getInstance().getRulerPosX();
        y = GUIPreferences.getInstance().getRulerPosY();
        h = GUIPreferences.getInstance().getRulerSizeHeight();
        w = GUIPreferences.getInstance().getRulerSizeWidth();
        if (x + w > screenSize.width) {
            x = 0;
            w = Math.min(w, screenSize.width);
        }
        if (y + h > screenSize.height) {
            y = 0;
            h = Math.min(h, screenSize.height);
        }
        ruler.setLocation(x, y);
        ruler.setSize(w, h);
        // end kenn

        // minimap
        minimapW = new Dialog(frame,
                Messages.getString("ClientGUI.MiniMap"), false); //$NON-NLS-1$
        x = GUIPreferences.getInstance().getMinimapPosX();
        y = GUIPreferences.getInstance().getMinimapPosY();
        try {
            minimap = new MiniMap(minimapW, this, bv);
        } catch (IOException e) {
            doAlertDialog(
                    Messages.getString("ClientGUI.FatalError.title"), Messages.getString("ClientGUI.FatalError.message1") + e); //$NON-NLS-1$ //$NON-NLS-2$
            die();
        }
        h = minimap.getSize().height;
        w = minimap.getSize().width;
        if (((x + 10) >= screenSize.width) || ((x + w) < 10)) {
            x = screenSize.width - w;
        }
        if (((y + 10) > screenSize.height) || ((y + h) < 10)) {
            y = screenSize.height - h;
        }
        minimapW.setLocation(x, y);
        minimapW.addWindowListener(this);
        minimapW.add(minimap);

        cb = new ChatterBox(this);
        this.add(cb.getComponent(), BorderLayout.SOUTH);
        client.changePhase(IGame.Phase.PHASE_UNKNOWN);

        mechSelectorDialog = new MechSelectorDialog(this, unitLoadingDialog);
        randomArmyDialog = new RandomArmyDialog(this);
        randomSkillDialog = new RandomSkillDialog(this);
        customBADialog = new CustomBattleArmorDialog(this);
        customFSDialog = new CustomFighterSquadronDialog(this, unitLoadingDialog);
        new Thread(mechSelectorDialog, "Mech Selector Dialog").start(); //$NON-NLS-1$
        new Thread(customBADialog, "Custom Battle Armor Dialog").start();
    }

    /**
     * Get the menu bar for this client.
     *
     * @return the <code>CommonMenuBar</code> of this client.
     */
    public CommonMenuBar getMenuBar() {
        return menuBar;
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
     * Change the default help file name for this client. <p/> This method
     * should only be called by the constructor of subclasses.
     *
     * @param fileName the <code>String</code> name of the help file for this
     *            <code>Client</code> subclass. This value should not be
     *            <code>null</code>.
     */
    protected void setHelpFileName(String fileName) {
        if (null != fileName) {
            helpFileName = fileName;
        }
    }

    /**
     * Called when the user selects the "Help->Contents" menu item. <p/> This
     * method can be called by subclasses.
     */
    public void showHelp() {
        // Do we need to create the "help" dialog?
        if (help == null) {
            help = new CommonHelpDialog(frame, new File(helpFileName));
        }
        // Show the help dialog.
        help.setVisible(true);
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
     * Called when the user selects the "View->Game Options" menu item.
     */
    private void showOptions() {
        if (client.game.getPhase() == IGame.Phase.PHASE_LOUNGE) {
            getGameOptionsDialog().setEditable(true);
        } else {
            getGameOptionsDialog().setEditable(false);
        }
        // Display the game options dialog.
        getGameOptionsDialog().update(client.game.getOptions());
        getGameOptionsDialog().setVisible(true);
    }

    /**
     * Called when the user selects the "View->Player List" menu item.
     */
    private void showPlayerList() {
        if (playerListDialog == null) {
            playerListDialog = new PlayerListDialog(frame, client);
        }
        playerListDialog.setVisible(true);
    }

    /**
     * Called when the user selects the "View->Round Report" menu item.
     */
    private void showRoundReport() {
        new MiniReportDisplay(frame, client.roundReport).setVisible(true);
    }

    /**
     * Implement the <code>ActionListener</code> interface.
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equalsIgnoreCase("fileGameSave")) { //$NON-NLS-1$
            FileDialog fd = new FileDialog(
                    frame,
                    Messages.getString("ClientGUI.FileSaveDialog.title"), FileDialog.SAVE); //$NON-NLS-1$
            fd.setDirectory("."); //$NON-NLS-1$

            fd.setVisible(true);

            if (null != fd.getFile()) {
                String file = fd.getDirectory() + fd.getFile();
                // stupid hack to allow for savegames in folders with spaces in the name
                file = file.replace(" ", "|");
                client.sendChat("/save " + file); //$NON-NLS-1$
            }
        }

        if (event.getActionCommand().equalsIgnoreCase("helpAbout")) { //$NON-NLS-1$
            showAbout();
        }
        if (event.getActionCommand().equalsIgnoreCase("helpContents")) { //$NON-NLS-1$
            showHelp();
        }
        if (event.getActionCommand().equalsIgnoreCase("viewClientSettings")) { //$NON-NLS-1$
            showSettings();
        }
        if (event.getActionCommand().equalsIgnoreCase("viewGameOptions")) { //$NON-NLS-1$
            showOptions();
        }
        if (event.getActionCommand().equalsIgnoreCase("viewPlayerList")) { //$NON-NLS-1$
            showPlayerList();
        }
        if (event.getActionCommand().equalsIgnoreCase("viewRoundReport")) { //$NON-NLS-1$
            showRoundReport();
        }
        if (event.getActionCommand().equals(VIEW_MEK_DISPLAY)) {
            toggleDisplay();
        } else if (event.getActionCommand().equals(VIEW_MINI_MAP)) {
            toggleMap();
        } else if (event.getActionCommand().equals(VIEW_UNIT_OVERVIEW)) {
            toggleUnitOverview();
        } else if (event.getActionCommand().equals(VIEW_ZOOM_IN)) {
            bv.zoomIn();
        } else if (event.getActionCommand().equals(VIEW_ZOOM_OUT)) {
            bv.zoomOut();
        } else if (event.getActionCommand().equals(VIEW_LOS_SETTING)) {
            showLOSSettingDialog();
        }
    }

    /**
     * Saves the current settings to the cfg file.
     */
    public void saveSettings() {
        // save frame location
        GUIPreferences.getInstance().setWindowPosX(frame.getLocation().x);
        GUIPreferences.getInstance().setWindowPosY(frame.getLocation().y);
        GUIPreferences.getInstance().setWindowSizeWidth(frame.getSize().width);
        GUIPreferences.getInstance()
                .setWindowSizeHeight(frame.getSize().height);

        // also minimap
        if ((minimapW != null)
                && ((minimapW.getSize().width * minimapW.getSize().height) > 0)) {
            GUIPreferences.getInstance().setMinimapPosX(
                    minimapW.getLocation().x);
            GUIPreferences.getInstance().setMinimapPosY(
                    minimapW.getLocation().y);
            GUIPreferences.getInstance().setMinimapZoom(minimap.getZoom());
        }

        // also mech display
        if ((mechW != null)
                && ((mechW.getSize().width * mechW.getSize().height) > 0)) {
            GUIPreferences.getInstance().setDisplayPosX(mechW.getLocation().x);
            GUIPreferences.getInstance().setDisplayPosY(mechW.getLocation().y);
            GUIPreferences.getInstance().setDisplaySizeWidth(
                    mechW.getSize().width);
            GUIPreferences.getInstance().setDisplaySizeHeight(
                    mechW.getSize().height);
        }

        // added by kenn
        // also ruler display
        if ((ruler != null) && (ruler.getSize().width != 0)
                && (ruler.getSize().height != 0)) {
            GUIPreferences.getInstance().setRulerPosX(ruler.getLocation().x);
            GUIPreferences.getInstance().setRulerPosY(ruler.getLocation().y);
            GUIPreferences.getInstance().setRulerSizeWidth(
                    ruler.getSize().width);
            GUIPreferences.getInstance().setRulerSizeHeight(
                    ruler.getSize().height);
        }
        // end kenn
    }

    /**
     * Shuts down threads and sockets
     */
    public void die() {
        // Tell all the displays to remove themselves as listeners.
        boolean reportHandled = false;
        for (Component component : phaseComponents.values()) {
            if (component instanceof ReportDisplay) {
                if (reportHandled) {
                    continue;
                }
                reportHandled = true;
            }
            if (component instanceof Distractable) {
                ((Distractable) component).removeAllListeners();
            }

        } // Handle the next component

        frame.removeAll();
        frame.setVisible(false);
        try {
            frame.dispose();
        } catch (Throwable error) {
            error.printStackTrace();
        }

        client.die();

        // TODO Is there a better solution?
        // This is required because the ChatLounge adds the listener to the
        // MechSummaryCache that must be removed explicitly.
        if (chatlounge != null) {
            chatlounge.die();
        }
    }

    /**
     * Returns the board selection dialog, creating it on the first call
     */
    public BoardSelectionDialog getBoardSelectionDialog() {
        if (boardSelectionDialog == null) {
            boardSelectionDialog = new BoardSelectionDialog(this);
        }
        return boardSelectionDialog;
    }

    public GameOptionsDialog getGameOptionsDialog() {
        if (gameOptionsDialog == null) {
            gameOptionsDialog = new GameOptionsDialog(this);
        }
        return gameOptionsDialog;
    }

    public MechSelectorDialog getMechSelectorDialog() {
        return mechSelectorDialog;
    }

    public CustomBattleArmorDialog getCustomBADialog() {
        return customBADialog;
    }

    public CustomFighterSquadronDialog getCustomFSDialog() {
        return customFSDialog;
    }

    public StartingPositionDialog getStartingPositionDialog() {
        if (startingPositionDialog == null) {
            startingPositionDialog = new StartingPositionDialog(this);
        }
        return startingPositionDialog;
    }

    public CustomInitiativeDialog getCustomInitiativeDialog() {
        if (initDialog == null) {
            initDialog = new CustomInitiativeDialog(this);
        }
        return initDialog;
    }

    public PlanetaryConditionsDialog getPlanetaryConditionsDialog() {
        if (conditionsDialog == null) {
            conditionsDialog = new PlanetaryConditionsDialog(this);
        }
        return conditionsDialog;
    }

    void switchPanel(IGame.Phase phase) {

        // Clear the old panel's listeners.
        if (curPanel instanceof BoardViewListener) {
            bv.removeBoardViewListener((BoardViewListener) curPanel);
        }
        if (curPanel instanceof ActionListener) {
            menuBar.removeActionListener((ActionListener) curPanel);
        }
        if (curPanel instanceof Distractable) {
            ((Distractable) curPanel).setIgnoringEvents(true);
        }

        // Get the new panel.
        String name = String.valueOf(phase);
        curPanel = phaseComponents.get(name);
        if (null == curPanel) {
            curPanel = initializePanel(phase);
        }
        cardsMain.show(panMain, mainNames.get(name).toString());
        cardsSecondary.show(panSecondary, secondaryNames.get(name).toString());

        // Set the new panel's listeners
        if (curPanel instanceof BoardViewListener) {
            bv.addBoardViewListener((BoardViewListener) curPanel);
        }
        if (curPanel instanceof ActionListener) {
            menuBar.addActionListener((ActionListener) curPanel);
        }
        if (curPanel instanceof Distractable) {
            ((Distractable) curPanel).setIgnoringEvents(false);
        }
        if (curPanel instanceof DoneButtoned) {
            Button done = ((DoneButtoned) curPanel).getDoneButton();
            cb.setDoneButton(done);
            done.setVisible(true);
        }

        // Make the new panel the focus, if the Client option says so
        if (GUIPreferences.getInstance().getFocus()
                && !(client instanceof megamek.client.bot.TestBot)) {
            curPanel.requestFocus();
        }
    }

    private Component initializePanel(IGame.Phase phase) {

        // Create the components for this phase.
        String name = String.valueOf(phase);
        Component component = null;
        String secondary = null;
        String main = null;
        switch (phase) {
            case PHASE_LOUNGE:
                component = new ChatLounge(this);
                chatlounge = (ChatLounge) component;
                main = "ChatLounge"; //$NON-NLS-1$
                secondary = main;
                panMain.add(main, component);
                panSecondary.add(secondary, ((ChatLounge) component)
                        .getSecondaryDisplay());
                break;
            case PHASE_STARTING_SCENARIO:
                component = new Label(Messages
                        .getString("ClientGUI.StartingScenario")); //$NON-NLS-1$
                main = "Label-StartingScenario"; //$NON-NLS-1$
                secondary = main;
                panMain.add(main, component);
                panSecondary.add(secondary, new Label("")); //$NON-NLS-1$
                break;
            case PHASE_EXCHANGE:
                component = new Label(Messages
                        .getString("ClientGUI.TransmittingData")); //$NON-NLS-1$
                main = "Label-Exchange"; //$NON-NLS-1$
                secondary = main;
                panMain.add(main, component);
                panSecondary.add(secondary, new Label("")); //$NON-NLS-1$
                break;
            case PHASE_SET_ARTYAUTOHITHEXES:
                component = new SelectArtyAutoHitHexDisplay(this);

                main = "BoardView"; //$NON-NLS-1$
                secondary = "SelectArtyAutoHitHexDisplay"; //$NON-NLS-1$
                if (!mainNames.keySet().contains(main)) {
                    panMain.add(main, bvc);
                }
                panSecondary.add(secondary, component);
                break;
            case PHASE_DEPLOY_MINEFIELDS:
                component = new DeployMinefieldDisplay(this);

                main = "BoardView"; //$NON-NLS-1$
                secondary = "DeployMinefieldDisplay"; //$NON-NLS-1$
                if (!mainNames.keySet().contains(main)) {
                    panMain.add(main, bvc);
                }
                panSecondary.add(secondary, component);
                break;
            case PHASE_DEPLOYMENT:
                component = new DeploymentDisplay(this);
                main = "BoardView"; //$NON-NLS-1$
                secondary = "DeploymentDisplay"; //$NON-NLS-1$
                if (!mainNames.keySet().contains(main)) {
                    panMain.add(main, bvc);
                }
                panSecondary.add(secondary, component);
                break;
            case PHASE_TARGETING:
                component = new TargetingPhaseDisplay(this, false);
                ((TargetingPhaseDisplay) component).initializeListeners();
                main = "BoardView"; //$NON-NLS-1$
                secondary = "TargetingPhaseDisplay"; //$NON-NLS-1$
                if (!mainNames.keySet().contains(main)) {
                    panMain.add(main, bvc);
                }
                panSecondary.add(secondary, component);
                break;
            case PHASE_MOVEMENT:
                component = new MovementDisplay(this);
                main = "BoardView"; //$NON-NLS-1$
                secondary = "MovementDisplay"; //$NON-NLS-1$
                if (!mainNames.keySet().contains(main)) {
                    panMain.add(main, bvc);
                }
                panSecondary.add(secondary, component);
                break;
            case PHASE_OFFBOARD:
                component = new TargetingPhaseDisplay(this, true);
                ((TargetingPhaseDisplay) component).initializeListeners();
                main = "BoardView"; //$NON-NLS-1$
                secondary = "OffboardDisplay"; //$NON-NLS-1$
                if (!mainNames.keySet().contains(main)) {
                    panMain.add(main, bvc);
                }
                panSecondary.add(secondary, component);
                break;
            case PHASE_FIRING:
                component = new FiringDisplay(this);
                main = "BoardView"; //$NON-NLS-1$
                secondary = "FiringDisplay"; //$NON-NLS-1$
                if (!mainNames.keySet().contains(main)) {
                    panMain.add(main, bvc);
                }
                panSecondary.add(secondary, component);
                break;
            case PHASE_PHYSICAL:
                component = new PhysicalDisplay(this);
                main = "BoardView"; //$NON-NLS-1$
                secondary = "PhysicalDisplay"; //$NON-NLS-1$
                if (!mainNames.keySet().contains(main)) {
                    panMain.add(main, bvc);
                }
                panSecondary.add(secondary, component);
                break;
            case PHASE_INITIATIVE_REPORT:
                component = new ReportDisplay(client);
                main = "ReportDisplay"; //$NON-NLS-1$
                secondary = main;
                panMain.add(main, component);
                panSecondary.add(secondary, ((ReportDisplay) component)
                        .getSecondaryDisplay());
                break;
            case PHASE_TARGETING_REPORT:
            case PHASE_MOVEMENT_REPORT:
            case PHASE_OFFBOARD_REPORT:
            case PHASE_FIRING_REPORT:
            case PHASE_PHYSICAL_REPORT:
            case PHASE_END_REPORT:
            case PHASE_VICTORY:
                // Try to reuse the ReportDisplay for other phases...
                component = phaseComponents.get(String
                        .valueOf(IGame.Phase.PHASE_INITIATIVE_REPORT));
                if (null == component) {
                    // no ReportDisplay to reuse -- get a new one
                    component = initializePanel(IGame.Phase.PHASE_INITIATIVE_REPORT);
                }
                main = "ReportDisplay"; //$NON-NLS-1$
                secondary = main;
                break;
            default:
                component = new Label(Messages
                        .getString("ClientGUI.waitingOnTheServer")); //$NON-NLS-1$
                main = "Label-Default"; //$NON-NLS-1$
                secondary = main;
                panMain.add(main, component);
                panSecondary.add(secondary, new Label("")); //$NON-NLS-1$
        }
        phaseComponents.put(name, component);
        mainNames.put(name, main);
        secondaryNames.put(name, secondary);

        return component;
    }

    protected void addBag(Component comp, GridBagLayout gridbag,
            GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
    }

    protected void showBoardPopup(Coords c) {
        fillPopup(c);

        if (popup.getItemCount() > 0) {
            bv.showPopup(popup, c);
        }
    }

/*    private boolean canTargetEntities() {
        return client.isMyTurn()
                && (curPanel instanceof FiringDisplay
                        || curPanel instanceof PhysicalDisplay || curPanel instanceof TargetingPhaseDisplay);
    }

    private boolean canSelectEntities() {
        return client.isMyTurn()
                && (curPanel instanceof FiringDisplay
                        || curPanel instanceof PhysicalDisplay
                        || curPanel instanceof MovementDisplay || curPanel instanceof TargetingPhaseDisplay);
    }
*/
    /**
     * Toggles the entity display window
     */
    public void toggleDisplay() {
        mechW.setVisible(!mechW.isVisible());
        if (mechW.isVisible()) {
            frame.requestFocus();
        }
    }

    /**
     * Sets the visibility of the entity display window
     */
    public void setDisplayVisible(boolean visible) {
        mechW.setVisible(visible);
        if (visible) {
            frame.requestFocus();
        }
    }

    public void toggleUnitOverview() {
        uo.setVisible(!uo.isVisible());
        bv.refreshDisplayables();
    }

    /**
     * Toggles the minimap window Also, toggles the minimap enabled setting
     */
    public void toggleMap() {
        if (minimapW.isVisible()) {
            GUIPreferences.getInstance().setMinimapEnabled(false);
        } else {
            GUIPreferences.getInstance().setMinimapEnabled(true);
        }
        minimapW.setVisible(!minimapW.isVisible());
        if (minimapW.isVisible()) {
            frame.requestFocus();
        }
    }

    /**
     * Sets the visibility of the minimap window
     */
    public void setMapVisible(boolean visible) {
        minimapW.setVisible(visible);
        if (visible) {
            frame.requestFocus();
        }
    }

    protected void fillPopup(Coords coords) {
        popup = new MapMenu(coords,client,curPanel,this);
    }

    /**
     * Pops up a dialog box giving the player a series of choices that are not
     * mutually exclusive.
     *
     * @param title the <code>String</code> title of the dialog box.
     * @param question the <code>String</code> question that has a "Yes" or
     *            "No" answer. The question will be split across multiple line
     *            on the '\n' characters.
     * @param choices the array of <code>String</code> choices that the player
     *            can select from.
     * @return The array of the <code>int</code> indexes of the from the input
     *         array that match the selected choices. If no choices were
     *         available, if the player did not select a choice, or if the
     *         player canceled the choice, a <code>null</code> value is
     *         returned.
     */
    public int[] doChoiceDialog(String title, String question, String[] choices) {
        ChoiceDialog choice = new ChoiceDialog(frame, title, question, choices);
        choice.setVisible(true);
        return choice.getChoices();
    }

    /**
     * Pops up a dialog box showing an alert
     */
    public void doAlertDialog(String title, String message) {
        AlertDialog alert = new AlertDialog(frame, title, message);
        alert.setVisible(true);
    }

    /**
     * Pops up a dialog box asking a yes/no question
     *
     * @param title the <code>String</code> title of the dialog box.
     * @param question the <code>String</code> question that has a "Yes" or
     *            "No" answer. The question will be split across multiple line
     *            on the '\n' characters.
     * @return <code>true</code> if yes
     */
    public boolean doYesNoDialog(String title, String question) {
        ConfirmDialog confirm = new ConfirmDialog(frame, title, question);
        confirm.setVisible(true);
        return confirm.getAnswer();
    }

    /**
     * Pops up a dialog box asking a yes/no question <p/> The player will be
     * given a chance to not show the dialog again.
     *
     * @param title the <code>String</code> title of the dialog box.
     * @param question the <code>String</code> question that has a "Yes" or
     *            "No" answer. The question will be split across multiple line
     *            on the '\n' characters.
     * @return the <code>ConfirmDialog</code> containing the player's
     *         responses. The dialog will already have been shown to the player,
     *         and is only being returned so the calling function can see the
     *         answer to the question and the state of the "Show again?"
     *         question.
     */
    public ConfirmDialog doYesNoBotherDialog(String title, String question) {
        ConfirmDialog confirm = new ConfirmDialog(frame, title, question, true);
        confirm.setVisible(true);
        return confirm;
    }

    /**
     * Allow the player to select a MegaMek Unit List file to load. The
     * <code>Entity</code>s in the file will replace any that the player has
     * already selected. As such, this method should only be called in the chat
     * lounge. The file can record damage sustained, non- standard munitions
     * selected, and ammunition expended in a prior engagement.
     */
    protected void loadListFile() {

        // Build the "load unit" dialog, if necessary.
        if (null == dlgLoadList) {
            dlgLoadList = new FileDialog(
                    frame,
                    Messages
                            .getString("ClientGUI.openUnitListFileDialog.title"), FileDialog.LOAD); //$NON-NLS-1$

            // Add a filter for MUL files
            dlgLoadList.setFilenameFilter(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return ((null != name) && name.endsWith(".mul")); //$NON-NLS-1$
                }
            });

            // use base directory by default
            dlgLoadList.setDirectory("."); //$NON-NLS-1$

            // Default to the player's name.
            // dlgLoadList.setFile( getLocalPlayer().getName() + ".mul" );
            // Instead, use setFile as a windoze hack, see Server.java
            // (search for "setFile") for details.
            dlgLoadList.setFile("*.mul"); //$NON-NLS-1$
        }

        // Display the "load unit" dialog.
        dlgLoadList.setVisible(true);

        // Did the player select a file?
        String unitPath = dlgLoadList.getDirectory();
        String unitFile = dlgLoadList.getFile();
        if (null != unitFile) {
            try {
                // Read the units from the file.
                Vector<Entity> loadedUnits = EntityListFile.loadFrom(new File(
                        unitPath, unitFile));

                // Add the units from the file.
                for (Enumeration<Entity> iter = loadedUnits.elements(); iter
                        .hasMoreElements();) {
                    final Entity entity = iter.nextElement();
                    entity.setOwner(client.getLocalPlayer());
                    client.sendAddEntity(entity);
                }
            } catch (IOException excep) {
                excep.printStackTrace(System.err);
                doAlertDialog(
                        Messages.getString("ClientGUI.errorLoadingFile"), excep.getMessage()); //$NON-NLS-1$
            }
        }
    }

    /**
     * Allow the player to save a list of entities to a MegaMek Unit List file.
     * A "Save As" dialog will be displayed that allows the user to select the
     * file's name and directory. The player can later load this file to quickly
     * select the units for a new game. The file will record damage sustained,
     * non-standard munitions selected, and ammunition expended during the
     * course of the current engagement.
     *
     * @param unitList - the <code>Vector</code> of <code>Entity</code>s to
     *            be saved to a file. If this value is <code>null</code> or
     *            empty, the "Save As" dialog will not be displayed.
     */
    protected void saveListFile(ArrayList<Entity> unitList) {

        // Handle empty lists.
        if ((null == unitList) || unitList.isEmpty()) {
            return;
        }

        // Build the "save unit" dialog, if necessary.
        if (null == dlgSaveList) {
            dlgSaveList = new FileDialog(
                    frame,
                    Messages
                            .getString("ClientGUI.saveUnitListFileDialog.title"), FileDialog.SAVE); //$NON-NLS-1$

            // Add a filter for MUL files
            dlgSaveList.setFilenameFilter(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return ((null != name) && name.endsWith(".mul")); //$NON-NLS-1$
                }
            });

            // use base directory by default
            dlgSaveList.setDirectory("."); //$NON-NLS-1$

            // Default to the player's name.
            dlgSaveList.setFile(client.getLocalPlayer().getName() + ".mul"); //$NON-NLS-1$
        }

        // Display the "save unit" dialog.
        dlgSaveList.setVisible(true);

        // Did the player select a file?
        String unitPath = dlgSaveList.getDirectory();
        String unitFile = dlgSaveList.getFile();
        if (null != unitFile) {
            if (!(unitFile.toLowerCase().endsWith(".mul") //$NON-NLS-1$
            || unitFile.toLowerCase().endsWith(".xml"))) { //$NON-NLS-1$
                unitFile += ".mul"; //$NON-NLS-1$
            }
            try {
                // Save the player's entities to the file.
                EntityListFile.saveTo(new File(unitPath, unitFile), unitList);
            } catch (IOException excep) {
                excep.printStackTrace(System.err);
                doAlertDialog(
                        Messages.getString("ClientGUI.errorSavingFile"), excep.getMessage()); //$NON-NLS-1$
            }
        }
    }

    //
    // WindowListener
    //
    public void windowActivated(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowClosed(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
        if (windowEvent.getWindow() == minimapW) {
            setMapVisible(false);
        } else if (windowEvent.getWindow() == mechW) {
            setDisplayVisible(false);
        }
    }

    public void windowDeactivated(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowDeiconified(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowIconified(java.awt.event.WindowEvent windowEvent) {
    }

    public void windowOpened(java.awt.event.WindowEvent windowEvent) {
    }

    /**
     * A menu item that lives to view an entity.
     */
/*    private class ViewMenuItem extends MenuItem implements ActionListener {
        private static final long serialVersionUID = 3756822619315859847L;
        Entity entity;

        public ViewMenuItem(Entity entity) {
            super(
                    Messages.getString("ClientGUI.viewMenuItem")
                            + entity.getDisplayName()
                            + (entity.isDone() ? " (" + Messages.getString("ClientGUI.doneMenuItem").trim() + ")" : "")); //$NON-NLS-1$
            this.entity = entity;
            addActionListener(this);
        }

        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            setDisplayVisible(true);
            mechD.displayEntity(entity);
        }
    }
*/
    /**
     * A menu item that would really like to select an entity. You can use this
     * during movement, firing & physical phases. (Deployment would just be
     * silly.)
     */
/*    private class SelectMenuItem extends MenuItem implements ActionListener {
        private static final long serialVersionUID = -2472642242836893482L;
        Entity entity;

        public SelectMenuItem(Entity entity) {
            super(
                    Messages.getString("ClientGUI.selectMenuItem") + entity.getDisplayName()); //$NON-NLS-1$
            this.entity = entity;
            addActionListener(this);
        }

        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            if (curPanel instanceof MovementDisplay) {
                ((MovementDisplay) curPanel).selectEntity(entity.getId());
            } else if (curPanel instanceof FiringDisplay) {
                ((FiringDisplay) curPanel).selectEntity(entity.getId());
            } else if (curPanel instanceof PhysicalDisplay) {
                ((PhysicalDisplay) curPanel).selectEntity(entity.getId());
            }
        }
    }
*/
    /**
     * A menu item that will target an entity, provided that it's sensible to do
     * so
     */
  /*  private class TargetMenuItem extends MenuItem implements ActionListener {
        private static final long serialVersionUID = -3854766135227390453L;
        Targetable target;

        public TargetMenuItem(Targetable t) {
            super(
                    Messages.getString("ClientGUI.targetMenuItem") + t.getDisplayName()); //$NON-NLS-1$
            target = t;
            addActionListener(this);
        }

        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            if (curPanel instanceof FiringDisplay) {
                ((FiringDisplay) curPanel).target(target);
            } else if (curPanel instanceof PhysicalDisplay) {
                ((PhysicalDisplay) curPanel).target(target);
            } else if (curPanel instanceof TargetingPhaseDisplay) {
                ((TargetingPhaseDisplay) curPanel).target(target);
            }
        }
    }
*/
    /**
     * @return the frame this client is displayed in
     */
    public Frame getFrame() {
        return frame;
    }

    // Shows a dialg where the player can select the entity types
    // used in the LOS tool.
    public void showLOSSettingDialog() {
        GUIPreferences gp = GUIPreferences.getInstance();
        LOSDialog ld = new LOSDialog(frame, gp.getMechInFirst(), gp
                .getMechInSecond());
        ld.setVisible(true);

        gp.setMechInFirst(ld.getMechInFirst());
        gp.setMechInSecond(ld.getMechInSecond());
    }

    // Loads a preview image of the unit into the BufferedPanel.
    public void loadPreviewImage(BufferedPanel bp, Entity entity) {
        Player player = client.game.getPlayer(entity.getOwnerId());
        loadPreviewImage(bp, entity, player);
    }

    public void loadPreviewImage(BufferedPanel bp, Entity entity, Player player) {
        Image camo = bv.getTilesetManager().getPlayerCamo(player);
        int tint = PlayerColors.getColorRGB(player.getColorIndex());
        BackGroundDrawer bgdPreview = new BackGroundDrawer(bv.getTilesetManager().loadPreviewImage(entity, camo, tint, bp));
        bp.removeBgDrawers();
        bp.addBgDrawer(bgdPreview);
    }

    /**
     * Make a "bing" sound.
     */
    public void bing() {
        if (!GUIPreferences.getInstance().getSoundMute() && (null != bingClip)) {
            bingClip.play();
        }
    }

    protected GameListener gameListener = new GameListenerAdapter() {

        @Override
        public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
            AlertDialog alert = new AlertDialog(
                    frame,
                    Messages.getString("ClientGUI.Disconnected.title"), Messages.getString("ClientGUI.Disconnected.message")); //$NON-NLS-1$ //$NON-NLS-2$
            alert.setVisible(true);
            frame.setVisible(false);
            die();
        }

        @Override
        public void gamePlayerChat(GamePlayerChatEvent e) {
            bing();
        }

        @Override
        public void gamePhaseChange(GamePhaseChangeEvent e) {
            // This is a really lame place for this, but I couldn't find a
            // better one without making massive changes (which didn't seem
            // worth it for one little feature).
            if (bv.getLocalPlayer() == null) {
                bv.setLocalPlayer(client.getLocalPlayer());
            }

            // Swap to this phase's panel.
            switchPanel(client.game.getPhase());

            // Hide tooltip (thanks Thrud Cowslayer, C.O.R.E, and others for
            // helping test this! :)
            bv.hideTooltip();

            // Handle phase-specific items.
            switch (e.getNewPhase()) {
                case PHASE_LOUNGE:
                    // this will get rid of old report tabs
                    ReportDisplay rD = (ReportDisplay) phaseComponents
                            .get(String.valueOf(IGame.Phase.PHASE_INITIATIVE_REPORT));
                    if (rD != null) {
                        rD.resetTabs();
                    }
                    break;
                case PHASE_DEPLOY_MINEFIELDS:
                case PHASE_DEPLOYMENT:
                case PHASE_TARGETING:
                case PHASE_MOVEMENT:
                case PHASE_OFFBOARD:
                case PHASE_FIRING:
                case PHASE_PHYSICAL:
                    if (GUIPreferences.getInstance().getMinimapEnabled()
                            && !minimapW.isVisible()) {
                        setMapVisible(true);
                    }
                    break;
                case PHASE_INITIATIVE_REPORT:
                case PHASE_TARGETING_REPORT:
                case PHASE_MOVEMENT_REPORT:
                case PHASE_OFFBOARD_REPORT:
                case PHASE_FIRING_REPORT:
                case PHASE_END:
                case PHASE_VICTORY:
                    setMapVisible(false);
                    // nemchenk, 2004-01-01 -- hide MechDisplay at the end
                    mechW.setVisible(false);
                    break;
            }

            menuBar.setPhase(client.game.getPhase());
            cb.getComponent().setVisible(true);
            validate();
            doLayout();
            cb.moveToEnd();
        }

        @Override
        public void gamePlayerConnected(GamePlayerConnectedEvent e) {
            System.err.println("gamePlayerConnected");
            System.err.flush();
            if (curPanel instanceof ReportDisplay) {
                ((ReportDisplay) curPanel).resetReadyButton();
                System.err.println("resetReadyButton");
                System.err.flush();
            }
        }

        @Override
        public void gameReport(GameReportEvent e) {
            // Normally the Report Display is updated when the panel is
            // switched during a phase change.
            // This update is for reports that get sent at odd times,
            // currently Tactical Genius reroll requests and when
            // a player wishes to continue moving after a fall.
            if ((e.getReport() == null) && (curPanel instanceof ReportDisplay)) {
                // Tactical Genius
                ((ReportDisplay) curPanel).appendReportTab(client.phaseReport);
                ((ReportDisplay) curPanel).resetReadyButton();
                // Check if the player deserves an active reroll button
                // (possible, if he gets one which he didn't use, and his
                // opponent got and used one) and if so activates it.
                if (client.game.hasTacticalGenius(client.getLocalPlayer())) {
                    if (!((ReportDisplay) curPanel).hasRerolled()) {
                        ((ReportDisplay) curPanel).resetRerollButton();
                    }
                }
            } else {
                // Continued movement after getting up
                if (!(client instanceof megamek.client.bot.TestBot)) {
                    doAlertDialog("Movement Report", e.getReport());
                }
            }
        }

        @Override
        public void gameEnd(GameEndEvent e) {
            bv.clearMovementData();

            for (Iterator<Client> i = getBots().values().iterator(); i
                    .hasNext();) {
                i.next().die();
            }
            getBots().clear();

            // Make a list of the player's living units.
            ArrayList<Entity> living = client.game.getPlayerEntities(client
                    .getLocalPlayer(), false);

            // Be sure to include all units that have retreated.
            for (Enumeration<Entity> iter = client.game.getRetreatedEntities(); iter
                    .hasMoreElements();) {
                living.add(iter.nextElement());
            }

            // Allow players to save their living units to a file.
            // Don't bother asking if none survived.
            if (!living.isEmpty()
                    && doYesNoDialog(
                            Messages
                                    .getString("ClientGUI.SaveUnitsDialog.title"), //$NON-NLS-1$
                            Messages
                                    .getString("ClientGUI.SaveUnitsDialog.message"))) { //$NON-NLS-1$

                // Allow the player to save the units to a file.
                saveListFile(living);

            } // End user-wants-a-MUL
        }

        @Override
        public void gameSettingsChange(GameSettingsChangeEvent e) {
            if ((boardSelectionDialog != null)
                    && boardSelectionDialog.isVisible()) {
                boardSelectionDialog.update(client.getMapSettings(), true);
            }
            if ((gameOptionsDialog != null) && gameOptionsDialog.isVisible()) {
                gameOptionsDialog.update(client.game.getOptions());
            }
            if (curPanel instanceof ChatLounge) {
                ChatLounge cl = (ChatLounge) curPanel;
                boolean useMinefields = client.game.getOptions().booleanOption(
                        "minefields"); //$NON-NLS-1$
                cl.enableMinefields(useMinefields);

                if (!useMinefields) {
                    client.getLocalPlayer().setNbrMFConventional(0);
                    client.getLocalPlayer().setNbrMFCommand(0);
                    client.getLocalPlayer().setNbrMFVibra(0);
                    client.sendPlayerInfo();
                }
            }
        }

        @Override
        public void gameMapQuery(GameMapQueryEvent e) {
            if ((boardSelectionDialog != null)
                    && boardSelectionDialog.isVisible()) {
                boardSelectionDialog.update(e.getSettings(), false);
            }
        }

    };

    public Client getClient() {
        return client;
    }

    public Map<String, Client> getBots() {
        return bots;
    }

    /**
     * @return Returns the selectedEntityNum.
     */
    public int getSelectedEntityNum() {
        return selectedEntityNum;
    }

    /**
     * @param selectedEntityNum The selectedEntityNum to set.
     */
    public void setSelectedEntityNum(int selectedEntityNum) {
        this.selectedEntityNum = selectedEntityNum;
    }

    public RandomArmyDialog getRandomArmyDialog() {
        return randomArmyDialog;
    }

    public RandomSkillDialog getRandomSkillDialog() {
        return randomSkillDialog;
    }

    public void hexMoused(BoardViewEvent b) {
        if (b.getType() == BoardViewEvent.BOARD_HEX_POPUP) {
            showBoardPopup(b.getCoords());
        }
    }

    public void hexCursor(BoardViewEvent b) {}
    public void boardHexHighlighted(BoardViewEvent b) {}
    public void hexSelected(BoardViewEvent b) {}
    public void firstLOSHex(BoardViewEvent b) {}
    public void secondLOSHex(BoardViewEvent b, Coords c) {}
    public void finishedMovingUnits(BoardViewEvent b) {}
    public void unitSelected(BoardViewEvent b) {}
}
