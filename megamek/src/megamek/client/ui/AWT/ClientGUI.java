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

package megamek.client;

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.sun.java.util.collections.Iterator;
import com.sun.java.util.collections.Map;
import com.sun.java.util.collections.TreeMap;

import megamek.client.util.widget.BufferedPanel;
import megamek.common.*;
import megamek.common.util.Distractable;
import megamek.common.util.StringUtil;

import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

public class ClientGUI
    extends Panel
    implements MouseListener, WindowListener, ActionListener, KeyListener, GameListener {
    // Action commands.
    public static final String VIEW_MEK_DISPLAY = "viewMekDisplay";
    public static final String VIEW_MINI_MAP = "viewMiniMap";
    public static final String VIEW_LOS_SETTING = "viewLOSSetting";
    public static final String VIEW_UNIT_OVERVIEW = "viewUnitOverview";
    public static final String VIEW_ZOOM_IN = "viewZoomIn";
    public static final String VIEW_ZOOM_OUT = "viewZoomOut";

    // a frame, to show stuff in
    public Frame frame;

    // A menu bar to contain all actions.
    protected CommonMenuBar menuBar = null;
    private CommonAboutDialog about = null;
    private CommonHelpDialog help = null;
    private CommonSettingsDialog setdlg = null;
    private String helpFileName = "readme.txt";

    // keep me
    private ChatterBox cb;
    public BoardView1 bv;
    private Panel scroller;
    public BoardComponent bc;
    public Dialog mechW;
    public MechDisplay mechD;
    public Dialog minimapW;
    public MiniMap minimap;
    public PopupMenu popup = new PopupMenu("Board Popup...");
    private UnitOverview uo;
    public Ruler ruler; // added by kenn
    protected Component curPanel;

    // some dialogs...
    private BoardSelectionDialog boardSelectionDialog;
    private GameOptionsDialog gameOptionsDialog;
    private MechSelectorDialog mechSelectorDialog;
    private StartingPositionDialog startingPositionDialog;
    private PlayerListDialog playerListDialog;

    /**
     * Save and Open dialogs for MegaMek Unit List (mul) files.
     */
    private FileDialog dlgLoadList = null;
    private FileDialog dlgSaveList = null;

    private Client client;

    /**
     * Cache for the "bing" soundclip.
     */
    AudioClip bingClip = null;

    /** Map each phase to the name of the card for the main display area. */
    private Hashtable mainNames = new Hashtable();

    /** The <code>Panel</code> containing the main display area. */
    private Panel panMain = new Panel();

    /** The <code>CardLayout</code> of the main display area. */
    private CardLayout cardsMain = new CardLayout();

    /** Map each phase to the name of the card for the secondary area. */
    private Hashtable secondaryNames = new Hashtable();

    /** The <code>Panel</code> containing the secondary display area. */
    private Panel panSecondary = new Panel();

    /** The <code>CardLayout</code> of the secondary display area. */
    private CardLayout cardsSecondary = new CardLayout();

    /** Map phase component names to phase component objects. */
    private Hashtable phaseComponents = new Hashtable();

    //TODO: there's a better place for this
    private Map bots = new TreeMap(StringUtil.stringComparator());
    /**
     * Construct a client which will display itself in a new frame.  It will
     * not try to connect to a server yet.  When the frame closes, this client
     * will clean up after itself as much as possible, but will not call
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

    /*
     * Try to load the "bing" sound clip.
     */
    public void loadSoundClip() {
        if (Settings.soundBingFilename == null)
            return;
        try {
            File file = new File(Settings.soundBingFilename);
            if (!file.exists()) {
                System.err.println("Failed to load audio file: " + Settings.soundBingFilename);
                return;
            }
            bingClip = Applet.newAudioClip(file.toURL());
        } catch (NoSuchMethodError e) {
            //Ok, that didn't work.  We will fall back on our other
            // sound class.
            System.out.println("Failed to find AudioClip class, using AudioPlayer instead.");
            if (!Settings.soundBingFilename.endsWith(".au")) {
                //The older sound class only understands .au files
                Settings.soundBingFilename =
                    new String(
                        Settings.soundBingFilename.substring(0, Settings.soundBingFilename.lastIndexOf(".")) + ".au");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void keyPressed(KeyEvent ke) {
        switch (ke.getKeyCode()) {
            case KeyEvent.VK_PAGE_DOWN :
                bv.zoomIn();
                break;
            case KeyEvent.VK_PAGE_UP :
                bv.zoomOut();
                break;
        }
    }

    public void keyTyped(KeyEvent ke) {
        ;
    }

    public void keyReleased(KeyEvent ke) {
        ;
    }

    /**
     * Display a system message in the chat box.
     *
     * @param   message the <code>String</code> message to be shown.
     */
    public void systemMessage(String message) {
        this.cb.systemMessage(message);
    }

    /**
     * Initializes a number of things about this frame.
     */
    private void initializeFrame() {
        this.frame = new Frame("MegaMek Client");
        menuBar.setGame(client.game);
        frame.setMenuBar(menuBar);
        if (Settings.windowSizeHeight != 0) {
            frame.setLocation(Settings.windowPosX, Settings.windowPosY);
            frame.setSize(Settings.windowSizeWidth, Settings.windowSizeHeight);
        } else {
            frame.setSize(800, 600);
        }

        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);

        frame.setIconImage(frame.getToolkit().getImage("data/images/megamek-icon.gif"));
    }

    /**
     * Lays out the frame by setting this Client object to take up the full
     * frame display area.
     */
    private void layoutFrame() {
        frame.setTitle(client.getName() + " - MegaMek");
        frame.setLayout(new BorderLayout());
        frame.add(this, BorderLayout.CENTER);
        frame.validate();
    }

    /**
     * Have the client register itself as a listener wherever it's needed.
     * <p/>
     * According to http://www-106.ibm.com/developerworks/java/library/j-jtp0618.html
     * it is a major bad no-no to perform these registrations before the
     * constructor finishes, so this function has to be called after the
     * <code>Client</code> is created.
     */
    public void initialize() {
        menuBar = new CommonMenuBar(this.getClient());
        initializeFrame();

        try {
            // Create the board viewer.
            bv = new BoardView1(client.game, frame);

            // Place the board viewer in a set of scrollbars.
            scroller = new Panel();
            scroller.setLayout (new BorderLayout());
            Scrollbar vertical = new Scrollbar (Scrollbar.VERTICAL);
            Scrollbar horizontal = new Scrollbar (Scrollbar.HORIZONTAL);
            scroller.add (bv, BorderLayout.CENTER);
            // Scrollbars are broken for "Brandon Drew" <brandx0@hotmail.com>
            if (Settings.getInstance().get
                ("megamek.client.clientgui.hidescrollbars", "false").equals
                ("false")) {
                // Assign the scrollbars to the board viewer.
                scroller.add (vertical, BorderLayout.EAST);
                scroller.add (horizontal, BorderLayout.SOUTH);
                bv.setScrollbars (vertical, horizontal);
            }

        } catch (IOException e) {
            doAlertDialog("Fatal Error", "Could not initialise:\n" + e);
            die();
        }

        layoutFrame();

        frame.setVisible(true);

        menuBar.addActionListener(this);
        frame.addKeyListener(this);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frame.setVisible(false);
                saveSettings();
                die();
            }
        });

        UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(frame);
        if (!MechSummaryCache.isInitialized()) {
            unitLoadingDialog.show();
        }

        uo = new UnitOverview(this);
        bv.addDisplayable(uo);

        bv.addMouseListener(this);
        bv.addKeyListener(this);

        bv.add(popup);

        mechW = new Dialog(frame, "Mech Display", false);
        mechW.setLocation(Settings.displayPosX, Settings.displayPosY);
        mechW.setSize(Settings.displaySizeWidth, Settings.displaySizeHeight);
        mechW.setResizable(true);
        mechW.addWindowListener(this);
        mechW.addKeyListener(this);
        mechD = new MechDisplay(this);
        mechW.add(mechD);

        // added by kenn
        Ruler.color1 = Settings.rulerColor1;
        Ruler.color2 = Settings.rulerColor2;
        ruler = new Ruler(frame, this.client, bv);
        ruler.setLocation(Settings.rulerPosX, Settings.rulerPosY);
        ruler.setSize(Settings.rulerSizeWidth, Settings.rulerSizeHeight);
        // end kenn

        // minimap
        minimapW = new Dialog(frame, "MiniMap", false);
        minimapW.setLocation(Settings.minimapPosX, Settings.minimapPosY);
        try {
            minimap = new MiniMap(minimapW, this, bv);
        } catch (IOException e) {
            doAlertDialog("Fatal Error", "Could not initialise minimap:\n" + e);
            die();
        };
        minimap.addKeyListener(this);
        minimapW.addWindowListener(this);
        minimapW.addKeyListener(this);
        minimapW.add(minimap);

        cb = new ChatterBox(this);
        this.add(cb.getComponent(), BorderLayout.SOUTH);
        client.changePhase(Game.PHASE_UNKNOWN);

        mechSelectorDialog = new MechSelectorDialog(this, unitLoadingDialog);
        new Thread(mechSelectorDialog).start();

        client.addGameListener(this);
    }

    /**
     * Get the menu bar for this client.
     *
     * @return  the <code>CommonMenuBar</code> of this client.
     */
    public CommonMenuBar getMenuBar() {
        return this.menuBar;
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
     * Change the default help file name for this client.
     * <p/>
     * This method should only be called by the constructor
     * of subclasses.
     *
     * @param   fileName the <code>String</code> name of the help file
     *          for this <code>Client</code> subclass.  This value should
     *          not be <code>null</code>.
     */
    protected void setHelpFileName(String fileName) {
        if (null != fileName) {
            this.helpFileName = fileName;
        }
    }

    /**
     * Called when the user selects the "Help->Contents" menu item.
     * <p/>
     * This method can be called by subclasses.
     */
    public void showHelp() {
        // Do we need to create the "help" dialog?
        if (this.help == null) {
            this.help = new CommonHelpDialog(this.frame, new File(helpFileName));
        }
        // Show the help dialog.
        this.help.show();
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
     * Called when the user selects the "View->Game Options" menu item.
     */
    private void showOptions() {
        if (client.game.getPhase() == Game.PHASE_LOUNGE) {
            getGameOptionsDialog().setEditable(true);
        } else {
            getGameOptionsDialog().setEditable(false);
        }
        // Display the game options dialog.
        getGameOptionsDialog().update(client.game.getOptions());
        getGameOptionsDialog().show();
    }

    /**
     * Called when the user selects the "View->Player List" menu item.
     */
    private void showPlayerList() {
        if (playerListDialog == null) {
            playerListDialog = new PlayerListDialog(frame, client);
        }
        playerListDialog.show();
    }

    /**
     * Called when the user selects the "View->Turn Report" menu item.
     */
    private void showTurnReport() {
        new MiniReportDisplay(frame, client.eotr).show();
    }

    /**
     * Implement the <code>ActionListener</code> interface.
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equalsIgnoreCase("fileGameSave")) {
            FileDialog fd = new FileDialog(frame, "Select file to save to...", FileDialog.LOAD);
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

            if (null!=fd.getFile()) {
                client.sendChat("/save "+fd.getFile());
            };
        }

        if (event.getActionCommand().equalsIgnoreCase("helpAbout")) {
            showAbout();
        }
        if (event.getActionCommand().equalsIgnoreCase("helpContents")) {
            showHelp();
        }
        if (event.getActionCommand().equalsIgnoreCase("viewClientSettings")) {
            showSettings();
        }
        if (event.getActionCommand().equalsIgnoreCase("viewGameOptions")) {
            showOptions();
        }
        if (event.getActionCommand().equalsIgnoreCase("viewPlayerList")) {
            showPlayerList();
        }
        if (event.getActionCommand().equalsIgnoreCase("viewTurnReport")) {
            showTurnReport();
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
        Settings.windowPosX = frame.getLocation().x;
        Settings.windowPosY = frame.getLocation().y;
        Settings.windowSizeWidth = frame.getSize().width;
        Settings.windowSizeHeight = frame.getSize().height;

        // also minimap
        if (minimapW != null && (minimapW.getSize().width * minimapW.getSize().height) > 0) {
            Settings.minimapPosX = minimapW.getLocation().x;
            Settings.minimapPosY = minimapW.getLocation().y;
            Settings.minimapZoom = minimap.getZoom();
        }

        // also mech display
        if (mechW != null && (mechW.getSize().width * mechW.getSize().height) > 0) {
            Settings.displayPosX = mechW.getLocation().x;
            Settings.displayPosY = mechW.getLocation().y;
            Settings.displaySizeWidth = mechW.getSize().width;
            Settings.displaySizeHeight = mechW.getSize().height;
        }

        // added by kenn
        // also ruler display
        if (ruler != null && ruler.getSize().width != 0 && ruler.getSize().height != 0) {
            Settings.rulerPosX = ruler.getLocation().x;
            Settings.rulerPosY = ruler.getLocation().y;
            Settings.rulerSizeWidth = ruler.getSize().width;
            Settings.rulerSizeHeight = ruler.getSize().height;
        }
        // end kenn

        // save settings to disk
        Settings.save();
    }

    /**
     * Shuts down threads and sockets
     */
    public void die() {
        //Tell all the displays to remove themselves as listeners.
        boolean reportHandled = false;
        Enumeration names = phaseComponents.keys();
        while (names.hasMoreElements()) {
            Component component = (Component) phaseComponents.get(names.nextElement());
            if (component instanceof ReportDisplay) {
                if (reportHandled) {
                    continue;
                } else {
                    reportHandled = true;
                }
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

    public StartingPositionDialog getStartingPositionDialog() {
        if (startingPositionDialog == null) {
            startingPositionDialog = new StartingPositionDialog(this);
        }
        return startingPositionDialog;
    }

    private void switchPanel(int phase) {

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
        curPanel = (Component) phaseComponents.get(name);
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
            this.cb.setDoneButton(done);
            done.setVisible(true);
        }
        if (curPanel instanceof ReportDisplay) {
            //            ((ReportDisplay) curPanel).resetButtons();
        }

        // Make the new panel the focus, if the Client option says so
        if (Settings.getFocus) curPanel.requestFocus();
    }

    private Component initializePanel(int phase) {

        // Create the components for this phase.
        String name = String.valueOf(phase);
        Component component = null;
        String secondary = null;
        String main = null;
        switch (phase) {
            case Game.PHASE_LOUNGE :
                component = new ChatLounge(this);
                main = "ChatLounge";
                secondary = main;
                panMain.add(main, component);
                panSecondary.add(secondary, ((ChatLounge) component).getSecondaryDisplay());
                break;
            case Game.PHASE_STARTING_SCENARIO :
                component = new Label("Starting scenario...");
                main = "Label-StartingScenario";
                secondary = main;
                panMain.add(main, component);
                panSecondary.add(secondary, new Label(""));
                break;
            case Game.PHASE_EXCHANGE :
                component = new Label("Transmitting game data...");
                main = "Label-Exchange";
                secondary = main;
                panMain.add(main, component);
                panSecondary.add(secondary, new Label(""));
                break;
            case Game.PHASE_SET_ARTYAUTOHITHEXES:
                component = new SelectArtyAutoHitHexDisplay(this);

                main = "BoardView";
                secondary = "SelectArtyAutoHitHexDisplay";
                if (!mainNames.contains(main)) {
                    panMain.add(main, this.scroller);
                }
                panSecondary.add(secondary, component);
                break;
            case Game.PHASE_DEPLOY_MINEFIELDS :
                component = new DeployMinefieldDisplay(this);

                main = "BoardView";
                secondary = "DeployMinefieldDisplay";
                if (!mainNames.contains(main)) {
                    panMain.add(main, this.scroller);
                }
                panSecondary.add(secondary, component);
                break;
            case Game.PHASE_DEPLOYMENT :
                component = new DeploymentDisplay(this);
                main = "BoardView";
                secondary = "DeploymentDisplay";
                if (!mainNames.contains(main)) {
                    panMain.add(main, this.scroller);
                }
                panSecondary.add(secondary, component);
                break;
            case Game.PHASE_TARGETING :
                component = new TargetingPhaseDisplay(this, false);
                ((TargetingPhaseDisplay) component).initializeListeners();
                main = "BoardView";
                secondary = "TargetingPhaseDisplay";
                if (!mainNames.contains(main)) {
                    panMain.add(main, this.scroller);
                }
                panSecondary.add(secondary, component);
                break;
            case Game.PHASE_MOVEMENT :
                component = new MovementDisplay(this);
                main = "BoardView";
                secondary = "MovementDisplay";
                if (!mainNames.contains(main)) {
                    panMain.add(main, this.scroller);
                }
                panSecondary.add(secondary, component);
                break;
            case Game.PHASE_OFFBOARD :
                component = new TargetingPhaseDisplay(this, true);
                ((TargetingPhaseDisplay) component).initializeListeners();
                main = "BoardView";
                secondary = "OffboardDisplay";
                if (!mainNames.contains(main)) {
                    panMain.add(main, this.scroller);
                }
                panSecondary.add(secondary, component);
                break;
            case Game.PHASE_FIRING :
                component = new FiringDisplay(this);
                main = "BoardView";
                secondary = "FiringDisplay";
                if (!mainNames.contains(main)) {
                    panMain.add(main, this.scroller);
                }
                panSecondary.add(secondary, component);
                break;
            case Game.PHASE_PHYSICAL :
                component = new PhysicalDisplay(this);
                main = "BoardView";
                secondary = "PhysicalDisplay";
                if (!mainNames.contains(main)) {
                    panMain.add(main, this.scroller);
                }
                panSecondary.add(secondary, component);
                break;
            case Game.PHASE_INITIATIVE :
                component = new ReportDisplay(client);
                main = "ReportDisplay";
                secondary = main;
                panMain.add(main, component);
                panSecondary.add(secondary, ((ReportDisplay) component).getSecondaryDisplay());
                break;
            case Game.PHASE_MOVEMENT_REPORT :
            case Game.PHASE_OFFBOARD_REPORT :
            case Game.PHASE_FIRING_REPORT :
            case Game.PHASE_END :
            case Game.PHASE_VICTORY :
                // Try to reuse the ReportDisplay for other phases...
                component = (Component) phaseComponents.get(String.valueOf(Game.PHASE_INITIATIVE));
                if (null == component) {
                    // no ReportDisplay to reuse -- get a new one
                    component = initializePanel(Game.PHASE_INITIATIVE);
                }
                main = "ReportDisplay";
                secondary = main;
                break;
            default :
                component = new Label("Waiting on the server...");
                main = "Label-Default";
                secondary = main;
                panMain.add(main, component);
                panSecondary.add(secondary, new Label(""));
        }
        phaseComponents.put(name, component);
        mainNames.put(name, main);
        secondaryNames.put(name, secondary);

        return component;
    }

    protected void addBag(Component comp, GridBagLayout gridbag, GridBagConstraints c) {
        gridbag.setConstraints(comp, c);
        add(comp);
    }

    protected void showBoardPopup(Point point) {
    	if (!bv.mayDrawPopup())
    		return;
        fillPopup(bv.getCoordsAt(point));

        if (popup.getItemCount() > 0) {
            popup.show(bv, point.x, point.y);
        }
    }

    private boolean canTargetEntities() {
        return client.isMyTurn()
            && (curPanel instanceof FiringDisplay
                || curPanel instanceof PhysicalDisplay
                || curPanel instanceof TargetingPhaseDisplay);
    }

    private boolean canSelectEntities() {
        return client.isMyTurn()
            && (curPanel instanceof FiringDisplay
                || curPanel instanceof PhysicalDisplay
                || curPanel instanceof MovementDisplay
                || curPanel instanceof TargetingPhaseDisplay);
    }

    /** Toggles the entity display window
     */
    public void toggleDisplay() {
        mechW.setVisible(!mechW.isVisible());
        if (mechW.isVisible()) {
            frame.requestFocus();
        }
    }

    /** Sets the visibility of the entity display window
     */
    public void setDisplayVisible(boolean visible) {
        mechW.setVisible(visible);
        if (visible) {
            frame.requestFocus();
        }
    }

    public void toggleUnitOverview() {
        uo.setVisible(!uo.isVisible());
        bv.repaint();
    }

    /** Toggles the minimap window
         Also, toggles the minimap enabled setting
     */
    public void toggleMap() {
        if (minimapW.isVisible()) {
            Settings.minimapEnabled = false;
        } else {
            Settings.minimapEnabled = true;
        }
        minimapW.setVisible(!minimapW.isVisible());
        if (minimapW.isVisible()) {
            frame.requestFocus();
        }
    }

    /** Sets the visibility of the minimap window
     */
    public void setMapVisible(boolean visible) {
        minimapW.setVisible(visible);
        if (visible) {
            frame.requestFocus();
        }
    }

    protected void fillPopup(Coords coords) {
        popup.removeAll();

        // add select options
        if (canSelectEntities()) {
            for (Enumeration i = client.game.getEntities(coords); i.hasMoreElements();) {
                final Entity entity = (Entity) i.nextElement();
                if (client.game.getTurn().isValidEntity(entity, client.game)) {
                    popup.add(new SelectMenuItem(entity));
                }
            }
        }

        if (popup.getItemCount() > 0) {
            popup.addSeparator();
        }

        // add view options
        for (Enumeration i = client.game.getEntities(coords); i.hasMoreElements();) {
            final Entity entity = (Entity) i.nextElement();
            popup.add(new ViewMenuItem(entity));
        }

        // add target options
        if (canTargetEntities()) {
            if (popup.getItemCount() > 0) {
                popup.addSeparator();
            }
            for (Enumeration i = client.game.getEntities(coords); i.hasMoreElements();) {
                final Entity entity = (Entity) i.nextElement();
                popup.add(new TargetMenuItem(entity));
            }
            // Can target weapons at the hex if it contains woods or building.
            // Can target physical attacks at the hex if it contains building.
            if (curPanel instanceof FiringDisplay
                || curPanel instanceof PhysicalDisplay
                || curPanel instanceof TargetingPhaseDisplay) {
                Hex h = client.game.board.getHex(coords);
                if (h != null && h.contains(Terrain.WOODS) && curPanel instanceof FiringDisplay) {
                    popup.add(new TargetMenuItem(new HexTarget(coords, client.game.board, Targetable.TYPE_HEX_CLEAR)));
                    if (client.game.getOptions().booleanOption("fire")) {
                        popup.add(
                            new TargetMenuItem(new HexTarget(coords, client.game.board, Targetable.TYPE_HEX_IGNITE)));
                    }
                } else if (h != null && h.contains(Terrain.BUILDING)) {
                    popup.add(new TargetMenuItem(new BuildingTarget(coords, client.game.board, false)));
                    if (client.game.getOptions().booleanOption("fire")) {
                        popup.add(new TargetMenuItem(new BuildingTarget(coords, client.game.board, true)));
                    }
                }
                if (h != null && client.game.containsMinefield(coords) && curPanel instanceof FiringDisplay) {
                    popup.add(new TargetMenuItem(new MinefieldTarget(coords, client.game.board)));
                }
                if (h != null && curPanel instanceof FiringDisplay) {
                    popup.add(
                        new TargetMenuItem(new HexTarget(coords, client.game.board, Targetable.TYPE_MINEFIELD_DELIVER)));
                    popup.add(
                        new TargetMenuItem(new HexTarget(coords, client.game.board, Targetable.TYPE_HEX_ARTILLERY)));
                    popup.add(
                        new TargetMenuItem(new HexTarget(coords, client.game.board, Targetable.TYPE_HEX_FASCAM)));
                    popup.add(
                        new TargetMenuItem(new HexTarget(coords, client.game.board, Targetable.TYPE_HEX_INFERNO_IV)));
                    popup.add(
                        new TargetMenuItem(new HexTarget(coords, client.game.board, Targetable.TYPE_HEX_VIBRABOMB_IV)));
                    
                }
                if (h != null && curPanel instanceof TargetingPhaseDisplay) {
                    popup.add(
                        new TargetMenuItem(new HexTarget(coords, client.game.board, Targetable.TYPE_HEX_ARTILLERY)));
                    popup.add(
                        new TargetMenuItem(new HexTarget(coords, client.game.board, Targetable.TYPE_HEX_FASCAM)));
                    popup.add(
                        new TargetMenuItem(new HexTarget(coords, client.game.board, Targetable.TYPE_HEX_INFERNO_IV)));
                    popup.add(
                        new TargetMenuItem(new HexTarget(coords, client.game.board, Targetable.TYPE_HEX_VIBRABOMB_IV)));                }
            }

        }
    }

    /**
     * Pops up a dialog box giving the player a series of choices that
     * are not mutually exclusive.
     *
     * @param   title the <code>String</code> title of the dialog box.
     * @param   question the <code>String</code> question that has a
     *          "Yes" or "No" answer.  The question will be split across
     *          multiple line on the '\n' characters.
     * @param   choices the array of <code>String</code> choices that
     *          the player can select from.
     * @return  The array of the <code>int</code> indexes of the from the
     *          input array that match the selected choices.  If no choices
     *          were available, if the player did not select a choice, or
     *          if the player canceled the choice, a <code>null</code> value
     *          is returned.
     */
    public int[] doChoiceDialog(String title, String question, String[] choices) {
        ChoiceDialog choice = new ChoiceDialog(frame, title, question, choices);
        choice.show();
        return choice.getChoices();
    }

    /**
     * Pops up a dialog box showing an alert
     */
    public void doAlertDialog(String title, String message) {
        AlertDialog alert = new AlertDialog(frame, title, message);
        alert.show();
    }

    /**
     * Pops up a dialog box asking a yes/no question
     *
     * @param   title the <code>String</code> title of the dialog box.
     * @param   question the <code>String</code> question that has a
     *          "Yes" or "No" answer.  The question will be split across
     *          multiple line on the '\n' characters.
     * @return <code>true</code> if yes
     */
    public boolean doYesNoDialog(String title, String question) {
        ConfirmDialog confirm = new ConfirmDialog(frame, title, question);
        confirm.show();
        return confirm.getAnswer();
    }

    /**
     * Pops up a dialog box asking a yes/no question
     * <p/>
     * The player will be given a chance to not show the dialog again.
     *
     * @param   title the <code>String</code> title of the dialog box.
     * @param   question the <code>String</code> question that has a
     *          "Yes" or "No" answer.  The question will be split across
     *          multiple line on the '\n' characters.
     * @param   bother a <code>Boolean</code> that will be set to match
     *          the player's response to "Do not bother me again".
     * @return  the <code>ConfirmDialog</code> containing the player's
     *          responses.  The dialog will already have been shown to
     *          the player, and is only being returned so the calling
     *          function can see the answer to the question and the state
     *          of the "Show again?" question.
     */
    public ConfirmDialog doYesNoBotherDialog(String title, String question) {
        ConfirmDialog confirm = new ConfirmDialog(frame, title, question, true);
        confirm.show();
        return confirm;
    }

    public void mouseClicked(java.awt.event.MouseEvent mouseEvent) {
    }

    public void mouseEntered(java.awt.event.MouseEvent mouseEvent) {
    }

    public void mouseExited(java.awt.event.MouseEvent mouseEvent) {
    }

    public void mousePressed(java.awt.event.MouseEvent mouseEvent) {
        if (mouseEvent.isPopupTrigger()) {
            showBoardPopup(mouseEvent.getPoint());
        }
    }

    public void mouseReleased(java.awt.event.MouseEvent mouseEvent) {
        if (mouseEvent.isPopupTrigger()) {
            showBoardPopup(mouseEvent.getPoint());
        }
    }

    /**
     * Allow the player to select a MegaMek Unit List file to load.  The
     * <code>Entity</code>s in the file will replace any that the player
     * has already selected.  As such, this method should only be called
     * in the chat lounge.  The file can record damage sustained, non-
     * standard munitions selected, and ammunition expended in a prior
     * engagement.
     */
    protected void loadListFile() {

        // Build the "load unit" dialog, if necessary.
        if (null == dlgLoadList) {
            dlgLoadList = new FileDialog(frame, "Open Unit List File", FileDialog.LOAD);

            // Add a filter for MUL files
            dlgLoadList.setFilenameFilter(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return (null != name && name.endsWith(".mul"));
                }
            });

            // use base directory by default
            dlgLoadList.setDirectory(".");

            // Default to the player's name.
            //dlgLoadList.setFile( getLocalPlayer().getName() + ".mul" );
            // Instead, use setFile as a windoze hack, see Server.java
            //  (search for "setFile") for details.
            dlgLoadList.setFile("*.mul");
        }

        // Display the "load unit" dialog.
        dlgLoadList.show();

        // Did the player select a file?
        String unitPath = dlgLoadList.getDirectory();
        String unitFile = dlgLoadList.getFile();
        if (null != unitFile) {
            try {
                // Read the units from the file.
                Vector loadedUnits = EntityListFile.loadFrom(unitPath, unitFile);

                // Add the units from the file.
                for (Enumeration iter = loadedUnits.elements(); iter.hasMoreElements();) {
                    final Entity entity = (Entity) iter.nextElement();
                    entity.setOwner(client.getLocalPlayer());
                    client.sendAddEntity(entity);
                }
            } catch (IOException excep) {
                excep.printStackTrace(System.err);
                doAlertDialog("Error Loading File", excep.getMessage());
            }
        }
    }

    /**
     * Allow the player to save a list of entities to a MegaMek Unit List
     * file.  A "Save As" dialog will be displayed that allows the user to
     * select the file's name and directory. The player can later load this
     * file to quickly select the units for a new game.  The file will record
     * damage sustained, non-standard munitions selected, and ammunition
     * expended during the course of the current engagement.
     *
     * @param   unitList - the <code>Vector</code> of <code>Entity</code>s
     *          to be saved to a file.  If this value is <code>null</code>
     *          or empty, the "Save As" dialog will not be displayed.
     */
    protected void saveListFile(Vector unitList) {

        // Handle empty lists.
        if (null == unitList || unitList.isEmpty()) {
            return;
        }

        // Build the "save unit" dialog, if necessary.
        if (null == dlgSaveList) {
            dlgSaveList = new FileDialog(frame, "Save Unit List As", FileDialog.SAVE);

            // Add a filter for MUL files
            dlgSaveList.setFilenameFilter(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return (null != name && name.endsWith(".mul"));
                }
            });

            // use base directory by default
            dlgSaveList.setDirectory(".");

            // Default to the player's name.
            dlgSaveList.setFile(client.getLocalPlayer().getName() + ".mul");
        }

        // Display the "save unit" dialog.
        dlgSaveList.show();

        // Did the player select a file?
        String unitPath = dlgSaveList.getDirectory();
        String unitFile = dlgSaveList.getFile();
        if (null != unitFile) {
            if (!(unitFile.toLowerCase().endsWith(".mul")
                  || unitFile.toLowerCase().endsWith(".xml"))) {
                unitFile += ".mul";
            }
            try {
                // Save the player's entities to the file.
                EntityListFile.saveTo(unitPath, unitFile, unitList);
            } catch (IOException excep) {
                excep.printStackTrace(System.err);
                doAlertDialog("Error Saving File", excep.getMessage());
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
    private class ViewMenuItem extends MenuItem implements ActionListener {
        Entity entity;

        public ViewMenuItem(Entity entity) {
            super("View " + entity.getDisplayName());
            this.entity = entity;
            addActionListener(this);
        }

        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            setDisplayVisible(true);
            mechD.displayEntity(entity);
        }
    }

    /**
     * A menu item that would really like to select an entity.  You can use
     * this during movement, firing & physical phases.  (Deployment would
     * just be silly.)
     */
    private class SelectMenuItem extends MenuItem implements ActionListener {
        Entity entity;

        public SelectMenuItem(Entity entity) {
            super("Select " + entity.getDisplayName());
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

    /**
     * A menu item that will target an entity, provided that it's sensible to
     * do so
     */
    private class TargetMenuItem extends MenuItem implements ActionListener {
        Targetable target;

        public TargetMenuItem(Targetable t) {
            super("Target " + t.getDisplayName());
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

    /**
     * @return the frame this client is displayed in
     */
    public Frame getFrame() {
        return frame;
    }

    // Shows a dialg where the player can select the entity types
    // used in the LOS tool.
    public void showLOSSettingDialog() {
        LOSDialog ld = new LOSDialog(frame, client.game.getMechInFirst(), client.game.getMechInSecond());
        ld.show();

        client.game.setMechInFirst(ld.getMechInFirst());
        client.game.setMechInSecond(ld.getMechInSecond());
    }

    // Loads a preview image of the unit into the BufferedPanel.
    public void loadPreviewImage(BufferedPanel bp, Entity entity) {
        Player player = client.game.getPlayer(entity.getOwnerId());
        loadPreviewImage(bp, entity, player);
    }

    public void loadPreviewImage(BufferedPanel bp, Entity entity, Player player) {
        Image camo = bv.getTilesetManager().getPlayerCamo(player);
        int tint = player.getColorRGB();
        bv.getTilesetManager().loadPreviewImage(entity, camo, tint, bp);
    }

    /**
     * Make a "bing" sound.
     * This tries to use the newer AudioClip class first, then falls
     * back on a Java 1.1 friendly (but undocumented!) class.
     */
    public void bing() {
        if (!Settings.soundMute) {
            if (null != bingClip) {
                bingClip.play();
            } else {
                try {
                    File file = new File(Settings.soundBingFilename);
                    InputStream in = new FileInputStream(file);
                    AudioStream bing = new AudioStream(in);
                    AudioPlayer.player.start(bing);
                } catch (Exception err) {
                    err.printStackTrace();
                }
            }
        }
    }

    public void gameDisconnected(GameEvent e) {
        AlertDialog alert = new AlertDialog(frame, "Disconnected!", "You have become disconnected from the server.");
        alert.show();
        frame.setVisible(false);
        die();
    }

    public void gameNewEntities(GameEvent e) {
        Object o = e.getSource();
        //		XXX Hack alert!
        if (o instanceof Packet) {
            Packet c = (Packet) o;
            int eindex = c.getIntValue(0);
            Entity entity = (Entity) c.getObject(1);
            Vector movePath = (Vector) c.getObject(2);
            Coords oc = entity.getPosition();
            if (client.game.getEntity(eindex) != null) {
                oc = client.game.getEntity(eindex).getPosition();
            }
            // Replace this entity in the game.
            client.game.setEntity(eindex, entity);
            if (movePath.size() > 0 && Settings.showMoveStep) {
                bv.addMovingUnit(entity, movePath);
            } else {
                bv.boardChangedEntity(new BoardEvent(client.game.board, oc, entity, 0, 0)); //XXX
            }
        } else {
            //XXX Hack alert!
            bv.boardNewEntities(new BoardEvent(client.game.board, null, null, 0, 0)); //XXX
            //XXX
        }
    }

    public void gameNewSettings(GameEvent e) {
        if (boardSelectionDialog != null && boardSelectionDialog.isVisible()) {
            boardSelectionDialog.update(client.getMapSettings(), true);
        }
        if (gameOptionsDialog != null && gameOptionsDialog.isVisible()) {
            gameOptionsDialog.update(client.game.getOptions());
        }
        if (curPanel instanceof ChatLounge) {
            ChatLounge cl = (ChatLounge) curPanel;
            boolean useMinefields = client.game.getOptions().booleanOption("minefields");
            cl.enableMinefields(useMinefields);

            if (!useMinefields) {
                client.getLocalPlayer().setNbrMFConventional(0);
                client.getLocalPlayer().setNbrMFCommand(0);
                client.getLocalPlayer().setNbrMFVibra(0);
                client.sendPlayerInfo();
            }
        }
    }

    public void gamePhaseChange(GameEvent e) {

        boolean showRerollButton = false;

        //This is a really lame place for this, but I couldn't find a
        //better one without making massive changes (which didn't seem
        //worth it for one little feature).
        if (bv.getLocalPlayer() == null) {
            bv.setLocalPlayer(client.getLocalPlayer());
        }

        // Swap to this phase's panel.
        switchPanel(client.game.getPhase());

        // Hide tooltip (thanks Thrud Cowslayer, C.O.R.E, and others for helping test this! :)
        bv.hideTooltip();
        
        // Handle phase-specific items.
        switch (client.game.getPhase()) {
            case Game.PHASE_DEPLOY_MINEFIELDS :
                if (Settings.minimapEnabled && !minimapW.isVisible()) {
                    setMapVisible(true);
                }
                break;
            case Game.PHASE_DEPLOYMENT :
                if (Settings.minimapEnabled && !minimapW.isVisible()) {
                    setMapVisible(true);
                }
                break;
            case Game.PHASE_TARGETING :
                if (Settings.minimapEnabled && !minimapW.isVisible()) {
                    setMapVisible(true);
                }
                break;
            case Game.PHASE_MOVEMENT :
                if (Settings.minimapEnabled && !minimapW.isVisible()) {
                    setMapVisible(true);
                }
                break;
            case Game.PHASE_OFFBOARD :
                if (Settings.minimapEnabled && !minimapW.isVisible()) {
                    setMapVisible(true);
                }
                break;
            case Game.PHASE_FIRING :
                if (Settings.minimapEnabled && !minimapW.isVisible()) {
                    setMapVisible(true);
                }
                break;
            case Game.PHASE_PHYSICAL :
                bv.refreshAttacks();
                if (Settings.minimapEnabled && !minimapW.isVisible()) {
                    setMapVisible(true);
                }
                break;
            case Game.PHASE_INITIATIVE :
                bv.clearAllAttacks();
                showRerollButton = client.game.hasTacticalGenius(client.getLocalPlayer());
            case Game.PHASE_MOVEMENT_REPORT :
            case Game.PHASE_OFFBOARD_REPORT :
            case Game.PHASE_FIRING_REPORT :
            case Game.PHASE_END :
            case Game.PHASE_VICTORY :
                setMapVisible(false);

                // nemchenk, 2004-01-01 -- hide MechDisplay at the end
                mechW.setVisible(false);
                break;
        }

        menuBar.setPhase(client.game.getPhase());
        this.cb.getComponent().setVisible(true);
        this.validate();
        this.doLayout();
        this.cb.moveToEnd();
    }

    public void gamePlayerChat(GameEvent e) {
        bing();
    }

    public void gamePlayerStatusChange(GameEvent e) {
        ;
    }

    public void gameTurnChange(GameEvent e) {
        ;
    }

    public void gameBoardChanged(GameEvent e) {
        bv.update(bv.getGraphics());
    }

    public Client getClient() {
        return client;
    }

    public void gameEnd(GameEvent e) {
        this.bv.clearMovementData();

        for (Iterator i = getBots().values().iterator(); i.hasNext();) {
            ((Client) i.next()).die();
        }
        getBots().clear();

        // Make a list of the player's living units.
        Vector living = client.game.getPlayerEntities(client.getLocalPlayer());

        // Be sure to include all units that have retreated.
        for (Enumeration iter = client.game.getRetreatedEntities(); iter.hasMoreElements();) {
            living.addElement(iter.nextElement());
        }

        // Allow players to save their living units to a file.
        // Don't bother asking if none survived.
        if (!living.isEmpty()
            && doYesNoDialog(
                "Save Units?",
                "Do you want to save all surviving units\n" + "(including retreated units) to a file?")) {

            // Allow the player to save the units to a file.
            saveListFile(living);

        } // End user-wants-a-MUL
    }

    public void gameReport(GameEvent e) {
        // Normally the Report Display is updated when the panel is
        //  switched during a phase change.  This update is for
        //  Tactical Genius reroll requests, and therefore only
        //  resets the done button.
        if (curPanel instanceof ReportDisplay) {
            ((ReportDisplay) curPanel).refresh();
            ((ReportDisplay) curPanel).resetReadyButton();
        }
    }

    public void gameMapQuery(GameEvent e) {
        if (boardSelectionDialog != null && boardSelectionDialog.isVisible()) {
            boardSelectionDialog.update((MapSettings) e.getSource(), false);
        }
    }

    public Map getBots() {
        return bots;
    }

}
