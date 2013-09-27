/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
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
package megamek.client.ui.swing;

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import keypoint.PngEncoder;
import megamek.client.Client;
import megamek.client.bot.TestBot;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.GBC;
import megamek.client.ui.IBoardView;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Configuration;
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
import megamek.common.preference.PreferenceManager;
import megamek.common.util.Distractable;
import megamek.common.util.StringUtil;

public class ClientGUI extends JPanel implements WindowListener, BoardViewListener, ActionListener {
    private static final String FILENAME_ICON_16X16 = "megamek-icon-16x16.png"; //$NON-NLS-1$
    private static final String FILENAME_ICON_32X32 = "megamek-icon-32x32.png"; //$NON-NLS-1$
    private static final String FILENAME_ICON_48X48 = "megamek-icon-48x48.png"; //$NON-NLS-1$
    private static final String FILENAME_ICON_256X256 = "megamek-icon-256x256.png"; //$NON-NLS-1$

    private static final long serialVersionUID = 3913466735610109147L;

    // Action commands.
    public static final String VIEW_MEK_DISPLAY = "viewMekDisplay"; //$NON-NLS-1$
    public static final String VIEW_MINI_MAP = "viewMiniMap"; //$NON-NLS-1$
    public static final String VIEW_LOS_SETTING = "viewLOSSetting"; //$NON-NLS-1$
    public static final String VIEW_UNIT_OVERVIEW = "viewUnitOverview"; //$NON-NLS-1$
    public static final String VIEW_ZOOM_IN = "viewZoomIn"; //$NON-NLS-1$
    public static final String VIEW_ZOOM_OUT = "viewZoomOut"; //$NON-NLS-1$
    public static final String VIEW_TOGGLE_ISOMETRIC = "viewToggleIsometric"; //$NON-NLS-1$

    // a frame, to show stuff in
    public JFrame frame;

    // A menu bar to contain all actions.
    protected CommonMenuBar menuBar;
    private CommonAboutDialog about;
    private CommonHelpDialog help;
    private CommonSettingsDialog setdlg;
    private String helpFileName = "docs/readme.txt"; //$NON-NLS-1$

    // keep me
    ChatterBox cb;
    ChatterBox2 cb2;
    public IBoardView bv;
    private Component bvc;
    public JDialog mechW;
    public MechDisplay mechD;
    public JDialog minimapW;
    public MiniMap minimap;
    private MapMenu popup;
    private UnitOverview uo;
    private Ruler ruler;
    protected JComponent curPanel;
    public ChatLounge chatlounge;

    // some dialogs...
    GameOptionsDialog gameOptionsDialog;
    private MechSelectorDialog mechSelectorDialog;
    private StartingPositionDialog startingPositionDialog;
    private PlayerListDialog playerListDialog;
    private RandomArmyDialog randomArmyDialog;
    private RandomSkillDialog randomSkillDialog;
    private RandomNameDialog randomNameDialog;
    private PlanetaryConditionsDialog conditionsDialog;
    /**
     * Save and Open dialogs for MegaMek Unit List (mul) files.
     */
    private JFileChooser dlgLoadList;
    private JFileChooser dlgSaveList;
    private Client client;

    private File curfileBoardImage;
    private File curfileBoard;

    /**
     * Cache for the "bing" soundclip.
     */
    private AudioClip bingClip;

    /**
     * Map each phase to the name of the card for the main display area.
     */
    private HashMap<String, String> mainNames = new HashMap<String, String>();

    /**
     * The <code>JPanel</code> containing the main display area.
     */
    private JPanel panMain = new JPanel();

    /**
     * The <code>CardLayout</code> of the main display area.
     */
    private CardLayout cardsMain = new CardLayout();

    /**
     * Map each phase to the name of the card for the secondary area.
     */
    private HashMap<String, String> secondaryNames = new HashMap<String, String>();

    /**
     * The <code>JPanel</code> containing the secondary display area.
     */
    private JPanel panSecondary = new JPanel();

    /**
     * The <code>CardLayout</code> of the secondary display area.
     */
    private CardLayout cardsSecondary = new CardLayout();

    /**
     * Map phase component names to phase component objects.
     */
    HashMap<String, JComponent> phaseComponents = new HashMap<String, JComponent>();

    /**
     * Current Selected entity
     */
    private int selectedEntityNum = Entity.NONE;

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
        JPanel panDisplay = new JPanel(new BorderLayout());
        panDisplay.add(panMain, BorderLayout.CENTER);
        panDisplay.add(panSecondary, BorderLayout.SOUTH);
        add(panDisplay, BorderLayout.CENTER);
    }

    public IBoardView getBoardView() {
        return bv;
    }

    /**
     * Try to load the "bing" sound clip.
     */
    private void loadSoundClip() {
        if (GUIPreferences.getInstance().getSoundBingFilename() == null) {
            return;
        }
        try {
            File file = new File(GUIPreferences.getInstance().getSoundBingFilename());
            if (!file.exists()) {
                System.err.println("Failed to load audio file: " + GUIPreferences.getInstance().getSoundBingFilename()); //$NON-NLS-1$
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
     * @param message
     *            the <code>String</code> message to be shown.
     */
    public void systemMessage(String message) {
        cb.systemMessage(message);
        cb2.addChatMessage("Megamek: " + message);
    }

    /**
     * Initializes a number of things about this frame.
     */
    private void initializeFrame() {
        frame = new JFrame(Messages.getString("ClientGUI.title")); //$NON-NLS-1$
        menuBar.setGame(client.game);
        frame.setJMenuBar(menuBar);
        Rectangle virtualBounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (GraphicsDevice gd : gs) {
            GraphicsConfiguration[] gc = gd.getConfigurations();
            for (GraphicsConfiguration element : gc) {
                virtualBounds = virtualBounds.union(element.getBounds());
            }
        }
        if (GUIPreferences.getInstance().getWindowSizeHeight() != 0) {
            int x = GUIPreferences.getInstance().getWindowPosX();
            int y = GUIPreferences.getInstance().getWindowPosY();
            int w = GUIPreferences.getInstance().getWindowSizeWidth();
            int h = GUIPreferences.getInstance().getWindowSizeHeight();
            if ((x < virtualBounds.getMinX()) || ((x + w) > virtualBounds.getMaxX())) {
                x = 0;
            }
            if ((y < virtualBounds.getMinY()) || ((y + h) > virtualBounds.getMaxY())) {
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
        List<Image> iconList = new ArrayList<Image>();
        iconList.add(frame.getToolkit().getImage(
                new File(Configuration.miscImagesDir(), FILENAME_ICON_16X16).toString()
        ));
        iconList.add(frame.getToolkit().getImage(
                new File(Configuration.miscImagesDir(), FILENAME_ICON_32X32).toString()
        ));
        iconList.add(frame.getToolkit().getImage(
                new File(Configuration.miscImagesDir(), FILENAME_ICON_48X48).toString()
        ));
        iconList.add(frame.getToolkit().getImage(
                new File(Configuration.miscImagesDir(), FILENAME_ICON_256X256).toString()
        ));
        frame.setIconImages(iconList);
    }

    /**
     * Lays out the frame by setting this Client object to take up the full
     * frame display area.
     */
    private void layoutFrame() {
        frame.setTitle(client.getName() + Messages.getString("ClientGUI.clientTitleSuffix")); //$NON-NLS-1$
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this, BorderLayout.CENTER);
        frame.validate();
    }

    /**
     * Have the client register itself as a listener wherever it's needed.
     * <p/>
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
            Class<?> c = getClass().getClassLoader().loadClass(System.getProperty("megamek.client.ui.AWT.boardView", "megamek.client.ui.swing.BoardView1"));
            bv = (IBoardView) c.getConstructor(IGame.class).newInstance(client.game);
            bvc = bv.getComponent();
            bvc.setName("BoardView");
            bv.addBoardViewListener(this);

        } catch (Exception e) {
            e.printStackTrace();
            doAlertDialog(Messages.getString("ClientGUI.FatalError.title"), Messages.getString("ClientGUI.FatalError.message") + e); //$NON-NLS-1$ //$NON-NLS-2$
            die();
        }

        layoutFrame();
        menuBar.addActionListener(this);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                frame.setVisible(false);
                saveSettings();
                die();
            }
        });
        cb2 = new ChatterBox2(this, bv);
        bv.addDisplayable(cb2);
        bv.addKeyListener(cb2);
        uo = new UnitOverview(this);
        bv.addDisplayable(uo);
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int x;
        int y;
        int h;
        int w;
        mechW = new JDialog(frame, Messages.getString("ClientGUI.MechDisplay"), false){
                /**
                 *
                 */
                private static final long serialVersionUID = 1L;

                /**
                 * In addition to the default Dialog processKeyEvent, this method
                 * dispatches a KeyEvent to the client gui.
                 * This enables all of the gui hotkeys.
                 */
                @Override
                protected void processKeyEvent(KeyEvent e) {
                    //menuBar.dispatchEvent(e);
                    curPanel.dispatchEvent(e);
                    if (!e.isConsumed()) {
                        super.processKeyEvent(e);
                    }
                }
            }; //$NON-NLS-1$
        x = GUIPreferences.getInstance().getDisplayPosX();
        y = GUIPreferences.getInstance().getDisplayPosY();
        h = GUIPreferences.getInstance().getDisplaySizeHeight();
        w = GUIPreferences.getInstance().getDisplaySizeWidth();
        if ((x + w) > gd.getDisplayMode().getWidth()) {
            x = 0;
            w = Math.min(w, gd.getDisplayMode().getWidth());
        }
        if ((y + h) > gd.getDisplayMode().getHeight()) {
            y = 0;
            h = Math.min(h, gd.getDisplayMode().getHeight());
        }
        mechW.setLocation(x, y);
        mechW.setSize(w, h);
        mechW.setResizable(true);
        mechW.addWindowListener(this);
        mechD = new MechDisplay(this);
        mechD.addMechDisplayListener(bv);
        mechW.add(mechD);

        Ruler.color1 = GUIPreferences.getInstance().getRulerColor1();
        Ruler.color2 = GUIPreferences.getInstance().getRulerColor2();
        ruler = new Ruler(frame, client, bv);
        x = GUIPreferences.getInstance().getRulerPosX();
        y = GUIPreferences.getInstance().getRulerPosY();
        h = GUIPreferences.getInstance().getRulerSizeHeight();
        w = GUIPreferences.getInstance().getRulerSizeWidth();
        if ((x + w) > gd.getDisplayMode().getWidth()) {
            x = 0;
            w = Math.min(w, gd.getDisplayMode().getWidth());
        }
        if ((y + h) > gd.getDisplayMode().getHeight()) {
            y = 0;
            h = Math.min(h, gd.getDisplayMode().getHeight());
        }
        ruler.setLocation(x, y);
        ruler.setSize(w, h);
        // minimap
        minimapW = new JDialog(frame, Messages.getString("ClientGUI.MiniMap"), false){
                /**
                 *
                 */
                private static final long serialVersionUID = 1L;

                /**
                 * In addition to the default Dialog processKeyEvent, this method
                 * dispatches a KeyEvent to the client gui.
                 * This enables all of the gui hotkeys.
                 */
                @Override
                protected void processKeyEvent(KeyEvent e) {
                    //menuBar.dispatchEvent(e);
                    curPanel.dispatchEvent(e);
                    if (!e.isConsumed()) {
                        super.processKeyEvent(e);
                    }
                }
            }; //$NON-NLS-1$

        x = GUIPreferences.getInstance().getMinimapPosX();
        y = GUIPreferences.getInstance().getMinimapPosY();
        try {
            minimap = new MiniMap(minimapW, this, bv);
        } catch (IOException e) {
            e.printStackTrace();
            doAlertDialog(Messages.getString("ClientGUI.FatalError.title"), Messages.getString("ClientGUI.FatalError.message1") + e); //$NON-NLS-1$ //$NON-NLS-2$
            die();
        }
        h = minimap.getSize().height;
        w = minimap.getSize().width;
        if (((x + 10) >= gd.getDisplayMode().getWidth()) || ((x + w) < 10)) {
            x = gd.getDisplayMode().getWidth() - w;
        }
        if (((y + 10) > gd.getDisplayMode().getHeight()) || ((y + h) < 10)) {
            y = gd.getDisplayMode().getHeight() - h;        }
        minimapW.setLocation(x, y);
        minimapW.addWindowListener(this);
        minimapW.add(minimap);
        cb = new ChatterBox(this);
        cb.setChatterBox2(cb2);
        cb2.setChatterBox(cb);
        client.changePhase(IGame.Phase.PHASE_UNKNOWN);
        UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(frame);
        if (!MechSummaryCache.getInstance().isInitialized()) {
            unitLoadingDialog.setVisible(true);
        }
        mechSelectorDialog = new MechSelectorDialog(this, unitLoadingDialog);      
        randomArmyDialog = new RandomArmyDialog(this);
        randomSkillDialog = new RandomSkillDialog(this);
        randomNameDialog = new RandomNameDialog(this);
        new Thread(mechSelectorDialog, "Mech Selector Dialog").start(); //$NON-NLS-1$        
        frame.setVisible(true);
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
     * Called when the user selects the "Help->Contents" menu item.
     * <p/>
     * This method can be called by subclasses.
     */
    private void showHelp() {
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
        if ("fileGameSave".equalsIgnoreCase(event.getActionCommand())) { //$NON-NLS-1$
            JFileChooser fc = new JFileChooser("./savegames");
            fc.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
            fc.setDialogTitle(Messages.getString("ClientGUI.FileSaveDialog.title"));

            int returnVal = fc.showSaveDialog(frame);
            if ((returnVal != JFileChooser.APPROVE_OPTION) || (fc.getSelectedFile() == null)) {
                // I want a file, y'know!
                return;
            }
            if (fc.getSelectedFile() != null) {
                String file = fc.getSelectedFile().getName();
                // stupid hack to allow for savegames in folders with spaces in
                // the name
                String path = fc.getSelectedFile().getParentFile().getPath();
                path = path.replace(" ", "|");
                client.sendChat("/localsave " + file + " " + path); //$NON-NLS-1$
            }
        }
        if ("fileGameSaveServer".equalsIgnoreCase(event.getActionCommand())) { //$NON-NLS-1$
            String filename = (String) JOptionPane.showInputDialog(frame, Messages.getString("ClientGUI.FileSaveServerDialog.message"), Messages.getString("ClientGUI.FileSaveServerDialog.title"), JOptionPane.QUESTION_MESSAGE, null, null, "savegame.sav");
            client.sendChat("/save " + filename);
        }
        if ("helpAbout".equalsIgnoreCase(event.getActionCommand())) { //$NON-NLS-1$
            showAbout();
        }
        if ("helpContents".equalsIgnoreCase(event.getActionCommand())) { //$NON-NLS-1$
            showHelp();
        }
        if ("fileUnitsSave".equalsIgnoreCase(event.getActionCommand())) { //$NON-NLS-1$
            doSaveUnit();
        }
        if ("fileUnitsOpen".equalsIgnoreCase(event.getActionCommand())) { //$NON-NLS-1$
            loadListFile();
        }
        if ("fileUnitsClear".equalsIgnoreCase(event.getActionCommand())) { //$NON-NLS-1$
            deleteAllUnits(client);
        }
        if ("fileUnitsReinforce".equalsIgnoreCase(event.getActionCommand())) { //$NON-NLS-1$
            loadListFile(client.getLocalPlayer(), true);
        }
        if ("viewClientSettings".equalsIgnoreCase(event.getActionCommand())) { //$NON-NLS-1$
            showSettings();
        }
        if ("viewGameOptions".equalsIgnoreCase(event.getActionCommand())) { //$NON-NLS-1$
            showOptions();
        }
        if ("viewPlayerList".equalsIgnoreCase(event.getActionCommand())) { //$NON-NLS-1$
            showPlayerList();
        }
        if ("viewRoundReport".equalsIgnoreCase(event.getActionCommand())) { //$NON-NLS-1$
            showRoundReport();
        }
        if ("fileBoardSave".equalsIgnoreCase(event.getActionCommand())) { //$NON-NLS-1$
            boardSave();
        } else if ("fileBoardSaveAs".equalsIgnoreCase(event.getActionCommand())) { //$NON-NLS-1$
            boardSaveAs();
        } else if ("fileBoardSaveAsImage".equalsIgnoreCase(event.getActionCommand())) { //$NON-NLS-1$
            boardSaveAsImage();
        }
        if (event.getActionCommand().equals(VIEW_MEK_DISPLAY)) {
            toggleDisplay();
        } else if (event.getActionCommand().equals(VIEW_MINI_MAP)) {
            toggleMap();
        } else if (event.getActionCommand().equals(VIEW_UNIT_OVERVIEW)) {
            toggleUnitOverview();
        } else if (event.getActionCommand().equals(VIEW_LOS_SETTING)) {
            showLOSSettingDialog();
        } else if (event.getActionCommand().equals(VIEW_ZOOM_IN)) {
            bv.zoomIn();
        } else if (event.getActionCommand().equals(VIEW_ZOOM_OUT)) {
            bv.zoomOut();
        } else if (event.getActionCommand().equals(VIEW_TOGGLE_ISOMETRIC)) {
            GUIPreferences.getInstance().setIsometricEnabled(bv.toggleIsometric());
        }
    }

    /**
     * Save all the current in use Entities each grouped by
     * player name
     *
     * and a file for salvage
     */
    public void doSaveUnit() {
         for(Enumeration<Player> iter= getClient().game.getPlayers();iter.hasMoreElements();) {
                Player p=iter.nextElement();
                ArrayList<Entity> l = getClient().game.getPlayerEntities(p, false);
                // Be sure to include all units that have retreated.
                for (Enumeration<Entity> iter2 = getClient().game.getRetreatedEntities(); iter2.hasMoreElements();) {
                  Entity e= iter2.nextElement();
                  if(e.getOwnerId()==p.getId()) {
                    l.add(e);
                }
              }
                saveListFile(l,p.getName());
            }

            // save all destroyed units in a separate "salvage MUL"
            ArrayList<Entity> destroyed = new ArrayList<Entity>();
            Enumeration<Entity> graveyard = getClient().game.getGraveyardEntities();
            while (graveyard.hasMoreElements()) {
                Entity entity = graveyard.nextElement();
                if (entity.isSalvage()) {
                    destroyed.add(entity);
                }
            }
            if (destroyed.size() > 0) {
                String sLogDir = PreferenceManager.getClientPreferences().getLogDirectory();
                File logDir = new File(sLogDir);
                if (!logDir.exists()) {
                    logDir.mkdir();
                }
                String fileName = "salvage.mul";
                if (PreferenceManager.getClientPreferences().stampFilenames()) {
                    fileName = StringUtil.addDateTimeStamp(fileName);
                }
                File unitFile = new File(sLogDir + File.separator + fileName);
                try {
                    // Save the destroyed entities to the file.
                    EntityListFile.saveTo(unitFile, destroyed);
                } catch (IOException excep) {
                    excep.printStackTrace(System.err);
                    doAlertDialog(Messages.getString("ClientGUI.errorSavingFile"), excep.getMessage()); //$NON-NLS-1$
                }
            }
        }


    /**
     * Saves the current settings to the cfg file.
     */
    void saveSettings() {
        // save frame location
        GUIPreferences.getInstance().setWindowPosX(frame.getLocation().x);
        GUIPreferences.getInstance().setWindowPosY(frame.getLocation().y);
        GUIPreferences.getInstance().setWindowSizeWidth(frame.getSize().width);
        GUIPreferences.getInstance().setWindowSizeHeight(frame.getSize().height);

        // also minimap
        if ((minimapW != null) && ((minimapW.getSize().width * minimapW.getSize().height) > 0)) {
            GUIPreferences.getInstance().setMinimapPosX(minimapW.getLocation().x);
            GUIPreferences.getInstance().setMinimapPosY(minimapW.getLocation().y);
            GUIPreferences.getInstance().setMinimapZoom(minimap.getZoom());
        }

        // also mech display
        if ((mechW != null) && ((mechW.getSize().width * mechW.getSize().height) > 0)) {
            GUIPreferences.getInstance().setDisplayPosX(mechW.getLocation().x);
            GUIPreferences.getInstance().setDisplayPosY(mechW.getLocation().y);
            GUIPreferences.getInstance().setDisplaySizeWidth(mechW.getSize().width);
            GUIPreferences.getInstance().setDisplaySizeHeight(mechW.getSize().height);
        }

        // also ruler display
        if ((ruler != null) && (ruler.getSize().width != 0) && (ruler.getSize().height != 0)) {
            GUIPreferences.getInstance().setRulerPosX(ruler.getLocation().x);
            GUIPreferences.getInstance().setRulerPosY(ruler.getLocation().y);
            GUIPreferences.getInstance().setRulerSizeWidth(ruler.getSize().width);
            GUIPreferences.getInstance().setRulerSizeHeight(ruler.getSize().height);
        }
    }

    /**
     * Shuts down threads and sockets
     */
    void die() {
        // Tell all the displays to remove themselves as listeners.
        boolean reportHandled = false;
        Iterator<String> names = phaseComponents.keySet().iterator();
        while (names.hasNext()) {
            JComponent component = phaseComponents.get(names.next());
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

    public GameOptionsDialog getGameOptionsDialog() {
        if (gameOptionsDialog == null) {
            gameOptionsDialog = new GameOptionsDialog(this);
        }
        return gameOptionsDialog;
    }

    public MechSelectorDialog getMechSelectorDialog() {
        return mechSelectorDialog;
    }

    public StartingPositionDialog getStartingPositionDialog() {
        if (startingPositionDialog == null) {
            startingPositionDialog = new StartingPositionDialog(this);
        }
        return startingPositionDialog;
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
        if (curPanel == null) {
            curPanel = initializePanel(phase);
        }

        // Handle phase-specific items.
        switch (phase) {
            case PHASE_LOUNGE:
                // reset old report tabs and images, if any
                ReportDisplay rD = (ReportDisplay) phaseComponents.get(String.valueOf(IGame.Phase.PHASE_INITIATIVE_REPORT));
                if (rD != null) {
                    rD.resetTabs();
                }
                ChatLounge cl = (ChatLounge) phaseComponents.get(String.valueOf(IGame.Phase.PHASE_LOUNGE));
                cb.setDoneButton(cl.butDone);
                cl.add(cb.getComponent(), BorderLayout.SOUTH);
                getBoardView().getTilesetManager().reset();
                this.mechW.setVisible(false);
                break;
            case PHASE_DEPLOY_MINEFIELDS:
            case PHASE_DEPLOYMENT:
            case PHASE_TARGETING:
            case PHASE_MOVEMENT:
            case PHASE_OFFBOARD:
            case PHASE_FIRING:
            case PHASE_PHYSICAL:
                if (GUIPreferences.getInstance().getMinimapEnabled() && !minimapW.isVisible()) {
                    setMapVisible(true);
                }
                break;
            case PHASE_INITIATIVE_REPORT:
            case PHASE_TARGETING_REPORT:
            case PHASE_MOVEMENT_REPORT:
            case PHASE_OFFBOARD_REPORT:
            case PHASE_FIRING_REPORT:
            case PHASE_PHYSICAL_REPORT:
            case PHASE_END_REPORT:
            case PHASE_VICTORY:
                rD = (ReportDisplay) phaseComponents.get(String.valueOf(IGame.Phase.PHASE_INITIATIVE_REPORT));
                cb.setDoneButton(rD.butDone);
                rD.add(cb.getComponent(), GBC.eol().fill(GridBagConstraints.HORIZONTAL));
                setMapVisible(false);
                mechW.setVisible(false);
                break;
            default:
                break;
        }

        cardsMain.show(panMain, mainNames.get(name));
        String secondaryToShow = secondaryNames.get(name);
        // only show the secondary component if there is one to show
        if (secondaryToShow != null) {
            panSecondary.setVisible(true);
            cardsSecondary.show(panSecondary, secondaryNames.get(name));
        } else {
            // otherwise, hide the panel
            panSecondary.setVisible(false);
        }

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

        // Make the new panel the focus, if the Client option says so
        if (GUIPreferences.getInstance().getFocus() && !(client instanceof TestBot)) {
            curPanel.requestFocus();
        }
    }

    private JComponent initializePanel(IGame.Phase phase) {
        // Create the components for this phase.
        String name = String.valueOf(phase);
        JComponent component;
        String secondary = null;
        String main;
        switch (phase) {
            case PHASE_LOUNGE:
                component = new ChatLounge(this);
                chatlounge = (ChatLounge) component;
                main = "ChatLounge"; //$NON-NLS-1$
                component.setName(main);
                panMain.add(component, main);
                break;
            case PHASE_STARTING_SCENARIO:
                component = new JLabel(Messages.getString("ClientGUI.StartingScenario")); //$NON-NLS-1$
                main = "JLabel-StartingScenario"; //$NON-NLS-1$
                component.setName(main);
                panMain.add(component, main);
                break;
            case PHASE_EXCHANGE:
                component = new JLabel(Messages.getString("ClientGUI.TransmittingData")); //$NON-NLS-1$
                main = "JLabel-Exchange"; //$NON-NLS-1$
                component.setName(main);
                panMain.add(component, main);
                break;
            case PHASE_SET_ARTYAUTOHITHEXES:
                component = new SelectArtyAutoHitHexDisplay(this);
                main = "BoardView"; //$NON-NLS-1$
                secondary = "SelectArtyAutoHitHexDisplay"; //$NON-NLS-1$
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                panSecondary.add(component, secondary);
                break;
            case PHASE_DEPLOY_MINEFIELDS:
                component = new DeployMinefieldDisplay(this);
                main = "BoardView"; //$NON-NLS-1$
                secondary = "DeployMinefieldDisplay"; //$NON-NLS-1$
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                panSecondary.add(component, secondary);
                break;
            case PHASE_DEPLOYMENT:
                component = new DeploymentDisplay(this);
                main = "BoardView"; //$NON-NLS-1$
                secondary = "DeploymentDisplay"; //$NON-NLS-1$
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                panSecondary.add(component, secondary);
                break;
            case PHASE_TARGETING:
                component = new TargetingPhaseDisplay(this, false);
                ((TargetingPhaseDisplay) component).initializeListeners();
                main = "BoardView"; //$NON-NLS-1$
                secondary = "TargetingPhaseDisplay"; //$NON-NLS-1$
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                panSecondary.add(component, secondary);
                break;
            case PHASE_MOVEMENT:
                component = new MovementDisplay(this);
                main = "BoardView"; //$NON-NLS-1$
                secondary = "MovementDisplay"; //$NON-NLS-1$
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                panSecondary.add(component, secondary);
                break;
            case PHASE_OFFBOARD:
                component = new TargetingPhaseDisplay(this, true);
                ((TargetingPhaseDisplay) component).initializeListeners();
                main = "BoardView"; //$NON-NLS-1$
                secondary = "OffboardDisplay"; //$NON-NLS-1$
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                panSecondary.add(component, secondary);
                break;
            case PHASE_FIRING:
                component = new FiringDisplay(this);
                main = "BoardView"; //$NON-NLS-1$
                secondary = "FiringDisplay"; //$NON-NLS-1$
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                panSecondary.add(component, secondary);
                break;
            case PHASE_PHYSICAL:
                component = new PhysicalDisplay(this);
                main = "BoardView"; //$NON-NLS-1$
                secondary = "PhysicalDisplay"; //$NON-NLS-1$
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                panSecondary.add(component, secondary);
                break;
            case PHASE_INITIATIVE_REPORT:
                component = new ReportDisplay(this);
                main = "ReportDisplay"; //$NON-NLS-1$
                component.setName(main);
                panMain.add(main, component);
                break;
            case PHASE_TARGETING_REPORT:
            case PHASE_MOVEMENT_REPORT:
            case PHASE_OFFBOARD_REPORT:
            case PHASE_FIRING_REPORT:
            case PHASE_PHYSICAL_REPORT:
            case PHASE_END_REPORT:
            case PHASE_VICTORY:
                // Try to reuse the ReportDisplay for other phases...
                component = phaseComponents.get(String.valueOf(IGame.Phase.PHASE_INITIATIVE_REPORT));
                if (component == null) {
                    // no ReportDisplay to reuse -- get a new one
                    component = initializePanel(IGame.Phase.PHASE_INITIATIVE_REPORT);
                }
                main = "ReportDisplay"; //$NON-NLS-1$
                break;
            default:
                component = new JLabel(Messages.getString("ClientGUI.waitingOnTheServer")); //$NON-NLS-1$
                main = "JLabel-Default"; //$NON-NLS-1$
                secondary = main;
                component.setName(main);
                panMain.add(main, component);
        }
        phaseComponents.put(name, component);
        mainNames.put(name, main);
        if (secondary != null) {
            secondaryNames.put(name, secondary);
        }
        return component;
    }

    protected void showBoardPopup(Coords c) {
        if (fillPopup(c)) {
            bv.showPopup(popup, c);
        }
    }

    /**
     * Toggles the entity display window
     */
    private void toggleDisplay() {
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

    private void toggleUnitOverview() {
        uo.setVisible(!uo.isVisible());
        GUIPreferences.getInstance().setShowUnitOverview(uo.isVisible());
        bv.refreshDisplayables();
    }

    /**
     * Toggles the minimap window Also, toggles the minimap enabled setting
     */
    private void toggleMap() {
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
    void setMapVisible(boolean visible) {
        minimapW.setVisible(visible);
        if (visible) {
            frame.requestFocus();
        }
    }

    private boolean fillPopup(Coords coords) {
        popup = new MapMenu(coords, client, curPanel, this);
        return popup.getHasMenu();
    }

    /**
     * Pops up a dialog box giving the player a series of choices that are not
     * mutually exclusive.
     *
     * @param title
     *            the <code>String</code> title of the dialog box.
     * @param question
     *            the <code>String</code> question that has a "Yes" or "No"
     *            answer. The question will be split across multiple line on the
     *            '\n' characters.
     * @param choices
     *            the array of <code>String</code> choices that the player can
     *            select from.
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
        JTextPane textArea = new JTextPane();
        ReportDisplay.setupStylesheet(textArea);

        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        textArea.setText("<pre>"+message+"</pre>");
        JOptionPane.showMessageDialog(frame, scrollPane, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Pops up a dialog box asking a yes/no question
     *
     * @param title
     *            the <code>String</code> title of the dialog box.
     * @param question
     *            the <code>String</code> question that has a "Yes" or "No"
     *            answer. The question will be split across multiple line on the
     *            '\n' characters.
     * @return <code>true</code> if yes
     */
    public boolean doYesNoDialog(String title, String question) {
        ConfirmDialog confirm = new ConfirmDialog(frame, title, question);
        confirm.setVisible(true);
        return confirm.getAnswer();
    }

    /**
     * Pops up a dialog box asking a yes/no question
     * <p/>
     * The player will be given a chance to not show the dialog again.
     *
     * @param title
     *            the <code>String</code> title of the dialog box.
     * @param question
     *            the <code>String</code> question that has a "Yes" or "No"
     *            answer. The question will be split across multiple line on the
     *            '\n' characters.
     * @return the <code>ConfirmDialog</code> containing the player's responses.
     *         The dialog will already have been shown to the player, and is
     *         only being returned so the calling function can see the answer to
     *         the question and the state of the "Show again?" question.
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
        loadListFile(client.getLocalPlayer());
    }

    /**
     * Allow the player to select a MegaMek Unit List file to load. The
     * <code>Entity</code>s in the file will replace any that the player has
     * already selected. As such, this method should only be called in the chat
     * lounge. The file can record damage sustained, non- standard munitions
     * selected, and ammunition expended in a prior engagement.
     *
     * @param Client
     */
    protected void loadListFile(Client c) {
        loadListFile(c.getLocalPlayer());
    }

    /**
     * Allow the player to select a MegaMek Unit List file to load. The
     * <code>Entity</code>s in the file will replace any that the player has
     * already selected. As such, this method should only be called in the chat
     * lounge. The file can record damage sustained, non- standard munitions
     * selected, and ammunition expended in a prior engagement.
     *
     * @param Player
     */
    protected void loadListFile(Player player) {
    	loadListFile(player, false);
    }
    
    /**
     * Allow the player to select a MegaMek Unit List file to load. The
     * <code>Entity</code>s in the file will replace any that the player has
     * already selected. As such, this method should only be called in the chat
     * lounge. The file can record damage sustained, non- standard munitions
     * selected, and ammunition expended in a prior engagement.
     *
     * @param Player
     */
    protected void loadListFile(Player player, boolean reinforce) {
    	boolean addedUnits = false;
    	
        // Build the "load unit" dialog, if necessary.
        if (dlgLoadList == null) {
            dlgLoadList = new JFileChooser(".");
            dlgLoadList.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
            dlgLoadList.setDialogTitle(Messages.getString("ClientGUI.openUnitListFileDialog.title"));
            dlgLoadList.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File dir) {
                    return ((dir.getName() != null) && (dir.getName().endsWith(
                            ".mul") || dir.isDirectory())); //$NON-NLS-1$
                }

                @Override
                public String getDescription() {
                    return "*.mul";
                }
            });
            // Default to the player's name.
            dlgLoadList.setSelectedFile(new File(player.getName() + ".mul")); //$NON-NLS-1$
        }
        
        int returnVal = dlgLoadList.showOpenDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (dlgLoadList.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        // Did the player select a file?
        File unitFile = dlgLoadList.getSelectedFile();
        if (unitFile != null) {
            try {
                // Read the units from the file.
                Vector<Entity> loadedUnits = EntityListFile.loadFrom(unitFile);

                // Add the units from the file.
                for (Entity entity : loadedUnits) {
                    entity.setOwner(player);
                    if (reinforce) {
                    	entity.setDeployRound(client.game.getRoundCount()+1);
                    }
                    client.sendAddEntity(entity);
                }
            } catch (IOException excep) {
                excep.printStackTrace(System.err);
                doAlertDialog(Messages.getString("ClientGUI.errorLoadingFile"), excep.getMessage()); //$NON-NLS-1$
            }
        }
        
        // If we've added reinforcements, then we need to set the round deployment up again.
        if (addedUnits && reinforce) {
        	client.game.setupRoundDeployment();
        	client.sendResetRoundDeployment();
        }
    }
    
    public void deleteAllUnits(Client c) {
    	ArrayList<Entity> currentUnits = c.game.getPlayerEntities(
                c.getLocalPlayer(), false);

        // Walk through the vector, deleting the entities.
        Iterator<Entity> entities = currentUnits.iterator();
        while (entities.hasNext()) {
            final Entity entity = entities.next();
            c.sendDeleteEntity(entity.getId());
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
     * @param unitList
     *            - the <code>Vector</code> of <code>Entity</code>s to be saved
     *            to a file. If this value is <code>null</code> or empty, the
     *            "Save As" dialog will not be displayed.
     */
    protected void saveListFile(ArrayList<Entity> unitList) {
         saveListFile(unitList,client.getLocalPlayer().getName());
    }
    protected void saveListFile(ArrayList<Entity> unitList,String filename) {

        // Handle empty lists.
        if ((unitList == null) || unitList.isEmpty()) {
            return;
        }

        // Build the "save unit" dialog, if necessary.
        if (dlgSaveList == null) {
            dlgSaveList = new JFileChooser(".");
            dlgSaveList.setLocation(frame.getLocation().x + 150, frame.getLocation().y + 100);
            dlgSaveList.setDialogTitle(Messages.getString("ClientGUI.saveUnitListFileDialog.title"));
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Mul Files", "mul");
            dlgSaveList.setFileFilter(filter);
        }
        // Default to the player's name.
        dlgSaveList.setSelectedFile(new File(filename + ".mul")); //$NON-NLS-1$

        int returnVal = dlgSaveList.showSaveDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION) || (dlgSaveList.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }

        // Did the player select a file?
        File unitFile = dlgSaveList.getSelectedFile();
        if (unitFile != null) {
            if (!(unitFile.getName().toLowerCase().endsWith(".mul") //$NON-NLS-1$
            || unitFile.getName().toLowerCase().endsWith(".xml"))) { //$NON-NLS-1$
                try {
                    unitFile = new File(unitFile.getCanonicalPath() + ".mul"); //$NON-NLS-1$
                } catch (IOException ie) {
                    // nothing needs to be done here
                    return;
                }
            }
            try {
                // Save the player's entities to the file.
                EntityListFile.saveTo(unitFile, unitList);
            } catch (IOException excep) {
                excep.printStackTrace(System.err);
                doAlertDialog(Messages.getString("ClientGUI.errorSavingFile"), excep.getMessage()); //$NON-NLS-1$
            }
        }
    }

    //
    // WindowListener
    //
    public void windowActivated(WindowEvent windowEvent) {
        // ignored
    }

    public void windowClosed(WindowEvent windowEvent) {
        // ignored
    }

    public void windowClosing(WindowEvent windowEvent) {
        if (windowEvent.getWindow().equals(minimapW)) {
            setMapVisible(false);
        } else if (windowEvent.getWindow().equals(mechW)) {
            setDisplayVisible(false);
        }
    }

    public void windowDeactivated(WindowEvent windowEvent) {
        // ignored
    }

    public void windowDeiconified(WindowEvent windowEvent) {
        // ignored
    }

    public void windowIconified(WindowEvent windowEvent) {
        // ignored
    }

    public void windowOpened(WindowEvent windowEvent) {
        // ignored
    }

    /**
     * @return the frame this client is displayed in
     */
    public JFrame getFrame() {
        return frame;
    }

    /**
     *  Shows a dialog where the player can select the entity types
     *  used in the LOS tool.
     */
    private void showLOSSettingDialog() {
        GUIPreferences gp = GUIPreferences.getInstance();
        LOSDialog ld = new LOSDialog(frame, gp.getMechInFirst(), gp.getMechInSecond());
        ld.setVisible(true);
        gp.setMechInFirst(ld.getMechInFirst());
        gp.setMechInSecond(ld.getMechInSecond());
    }

    /**
     *  Loads a preview image of the unit into the BufferedPanel.
     * @param bp
     * @param entity
     */
    public void loadPreviewImage(JLabel bp, Entity entity) {
        Player player = client.game.getPlayer(entity.getOwnerId());
        loadPreviewImage(bp, entity, player);
    }

    public void loadPreviewImage(JLabel bp, Entity entity, Player player) {
        Image camo = null;
        if (entity.getCamoFileName() != null) {
            camo = bv.getTilesetManager().getEntityCamo(entity);
        } else {
            camo = bv.getTilesetManager().getPlayerCamo(player);
        }
        int tint = PlayerColors.getColorRGB(player.getColorIndex());
        bp.setIcon(new ImageIcon(bv.getTilesetManager().loadPreviewImage(entity, camo, tint, bp)));
    }

    /**
     * Make a "bing" sound.
     */
    void bing() {
        if (!GUIPreferences.getInstance().getSoundMute() && (bingClip != null)) {
            bingClip.play();
        }
    }

    private GameListener gameListener = new GameListenerAdapter() {
        @Override
        public void gamePlayerDisconnected(GamePlayerDisconnectedEvent e) {
            JOptionPane.showMessageDialog(frame, Messages.getString("ClientGUI.Disconnected.message"), Messages.getString("ClientGUI.Disconnected.title"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$ //$NON-NLS-2$
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
            if (bv.getLocalPlayer() != client.getLocalPlayer()) {
                // The adress based comparison is somewhat important.  
                //  Use of the /reset command can cause the player to get reset,
                //  and the equals function of Player isn't powerful enough.
                bv.setLocalPlayer(client.getLocalPlayer());
            }

            // Swap to this phase's panel.
            switchPanel(getClient().game.getPhase());

            menuBar.setPhase(getClient().game.getPhase());
            validate();
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
            if (curPanel instanceof ReportDisplay) {
                // Tactical Genius
                ((ReportDisplay) curPanel).appendReportTab(getClient().phaseReport);
                ((ReportDisplay) curPanel).resetReadyButton();
                // Check if the player deserves an active reroll button
                // (possible, if he gets one which he didn't use, and his
                // opponent got and used one) and if so activates it.
                if (getClient().game.hasTacticalGenius(getClient().getLocalPlayer())) {
                    if (!((ReportDisplay) curPanel).hasRerolled()) {
                        ((ReportDisplay) curPanel).resetRerollButton();
                    }
                }
                // Show a popup to the players so that we know whats up!
                if (!(getClient() instanceof TestBot)) {
                    doAlertDialog("Tactical Genius Report", e.getReport());
                }
            } else {
                // Continued movement after getting up
                if (!(getClient() instanceof TestBot)) {
                    doAlertDialog("Movement Report", e.getReport());
                }
            }
        }


        @Override
        public void gameEnd(GameEndEvent e) {
            bv.clearMovementData();
            for (Client client2 : getBots().values()) {
                client2.die();
            }
            getBots().clear();

            // Make a list of the player's living units.
            ArrayList<Entity> living = getClient().game.getPlayerEntities(getClient().getLocalPlayer(), false);

            // Be sure to include all units that have retreated.
            for (Enumeration<Entity> iter = getClient().game.getRetreatedEntities(); iter.hasMoreElements();) {
                Entity ent = iter.nextElement();
                if (ent.getOwnerId() == getClient().getLocalPlayer().getId()) {
                    living.add(ent);
                }
            }

            // Allow players to save their living units to a file.
            // Don't bother asking if none survived.
            if (!living.isEmpty() && doYesNoDialog(Messages.getString("ClientGUI.SaveUnitsDialog.title"), //$NON-NLS-1$
                    Messages.getString("ClientGUI.SaveUnitsDialog.message"))) { //$NON-NLS-1$

                // Allow the player to save the units to a file.
                saveListFile(living);
            } // End user-wants-a-MUL

            // save all destroyed units in a separate "salvage MUL"
            ArrayList<Entity> destroyed = new ArrayList<Entity>();
            Enumeration<Entity> graveyard = getClient().game.getGraveyardEntities();
            while (graveyard.hasMoreElements()) {
                Entity entity = graveyard.nextElement();
                if (entity.isSalvage()) {
                    destroyed.add(entity);
                }
            }
            if (destroyed.size() > 0) {
                String sLogDir = PreferenceManager.getClientPreferences().getLogDirectory();
                File logDir = new File(sLogDir);
                if (!logDir.exists()) {
                    logDir.mkdir();
                }
                String fileName = "salvage.mul";
                if (PreferenceManager.getClientPreferences().stampFilenames()) {
                    fileName = StringUtil.addDateTimeStamp(fileName);
                }
                File unitFile = new File(sLogDir + File.separator + fileName);
                try {
                    // Save the destroyed entities to the file.
                    EntityListFile.saveTo(unitFile, destroyed);
                } catch (IOException excep) {
                    excep.printStackTrace(System.err);
                    doAlertDialog(Messages.getString("ClientGUI.errorSavingFile"), excep.getMessage()); //$NON-NLS-1$
                }
            }

        }

        @Override
        public void gameSettingsChange(GameSettingsChangeEvent e) {
            if ((gameOptionsDialog != null) && gameOptionsDialog.isVisible() && 
                    !e.isMapSettingsOnlyChange()) {
                gameOptionsDialog.update(getClient().game.getOptions());
            }
            if (curPanel instanceof ChatLounge) {
                ChatLounge cl = (ChatLounge) curPanel;
                cl.updateMapSettings(getClient().getMapSettings());
            }
        }

        @Override
        public void gameMapQuery(GameMapQueryEvent e) {

        }
    };

    public Client getClient() {
        return client;
    }

    public Map<String, Client> getBots() {
        return client.bots;
    }

    /**
     * @return Returns the selectedEntityNum.
     */
    public int getSelectedEntityNum() {
        return selectedEntityNum;
    }

    /**
     * @param selectedEntityNum
     *            The selectedEntityNum to set.
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

    public RandomNameDialog getRandomNameDialog() {
        return randomNameDialog;
    }

    /**
     * Checks to see if there is already a path and name stored; if not, calls
     * "save as"; otherwise, saves the board to the specified file.
     */
    private void boardSave() {
        if (curfileBoard == null) {
            boardSaveAs();
            return;
        }
        // save!
        try {
            OutputStream os = new FileOutputStream(curfileBoard);
            // tell the board to save!
            client.game.getBoard().save(os);
            // okay, done!
            os.close();
        } catch (IOException ex) {
            System.err.println("error opening file to save!"); //$NON-NLS-1$
            System.err.println(ex);
        }
    }

    /**
     * Saves the board in PNG image format.
     */
    private void boardSaveImage() {
        if (curfileBoardImage == null) {
            boardSaveAsImage();
            return;
        }
        JDialog waitD = new JDialog(frame, Messages
                .getString("BoardEditor.waitDialog.title")); //$NON-NLS-1$
        waitD.add(new JLabel(Messages
                .getString("BoardEditor.waitDialog.message"))); //$NON-NLS-1$
        waitD.setSize(250, 130);
        // move to middle of screen
        waitD.setLocation(
                (frame.getSize().width / 2) - (waitD.getSize().width / 2), (frame
                        .getSize().height
                        / 2) - (waitD.getSize().height / 2));
        waitD.setVisible(true);
        frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        waitD.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        // save!
        int filter = 0; // 0 - no filter; 1 - sub; 2 - up
        int compressionLevel = 9; // 0 to 9 with 0 being no compression
        PngEncoder png = new PngEncoder(bv.getEntireBoardImage(),
                PngEncoder.NO_ALPHA, filter, compressionLevel);
        try {
            FileOutputStream outfile = new FileOutputStream(curfileBoardImage);
            byte[] pngbytes;
            pngbytes = png.pngEncode();
            if (pngbytes == null) {
                System.out.println("Failed to save board as image:Null image"); //$NON-NLS-1$
            } else {
                outfile.write(pngbytes);
            }
            outfile.flush();
            outfile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        waitD.setVisible(false);
        frame.setCursor(Cursor.getDefaultCursor());
    }

    /**
     * Opens a file dialog box to select a file to save as; saves the board to
     * the file.
     */
    private void boardSaveAs() {
        JFileChooser fc = new JFileChooser("data" + File.separator + "boards");
        fc
                .setLocation(frame.getLocation().x + 150,
                        frame.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("BoardEditor.saveBoardAs"));
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                return ((null != dir.getName())
                        && (dir.getName().endsWith(".board") || dir.isDirectory())); //$NON-NLS-1$
            }

            @Override
            public String getDescription() {
                return "*.board";
            }
        });
        int returnVal = fc.showSaveDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (fc.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }
        curfileBoard = fc.getSelectedFile();

        // make sure the file ends in board
        if (!curfileBoard.getName().toLowerCase().endsWith(".board")) { //$NON-NLS-1$
            try {
                curfileBoard = new File(curfileBoard.getCanonicalPath() + ".board"); //$NON-NLS-1$
            } catch (IOException ie) {
                // failure!
                return;
            }
        }
        boardSave();
    }

    /**
     * Opens a file dialog box to select a file to save as; saves the board to
     * the file as an image. Useful for printing boards.
     */
    private void boardSaveAsImage() {
        JFileChooser fc = new JFileChooser(".");
        fc
                .setLocation(frame.getLocation().x + 150,
                        frame.getLocation().y + 100);
        fc.setDialogTitle(Messages.getString("BoardEditor.saveAsImage"));
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File dir) {
                return (null != dir.getName()) && (dir.getName().endsWith(".png") || dir.isDirectory()); //$NON-NLS-1$
            }

            @Override
            public String getDescription() {
                return ".png";
            }
        });
        int returnVal = fc.showSaveDialog(frame);
        if ((returnVal != JFileChooser.APPROVE_OPTION)
                || (fc.getSelectedFile() == null)) {
            // I want a file, y'know!
            return;
        }
        curfileBoardImage = fc.getSelectedFile();

        // make sure the file ends in png
        if (!curfileBoardImage.getName().toLowerCase().endsWith(".png")) { //$NON-NLS-1$
            try {
                curfileBoardImage = new File(curfileBoardImage.getCanonicalPath()
                        + ".png"); //$NON-NLS-1$
            } catch (IOException ie) {
                // failure!
                return;
            }
        }
        boardSaveImage();
    }

    public void hexMoused(BoardViewEvent b) {
        if (b.getType() == BoardViewEvent.BOARD_HEX_POPUP) {
            showBoardPopup(b.getCoords());
        }
    }

    public void hexCursor(BoardViewEvent b) {
        // ignored
    }

    public void boardHexHighlighted(BoardViewEvent b) {
        // ignored
    }

    public void hexSelected(BoardViewEvent b) {
        // ignored
    }

    public void firstLOSHex(BoardViewEvent b) {
        // ignored
    }

    public void secondLOSHex(BoardViewEvent b, Coords c) {
        // ignored
    }

    public void finishedMovingUnits(BoardViewEvent b) {
        // ignored
    }

    public void unitSelected(BoardViewEvent b) {
        // ignored
    }

}