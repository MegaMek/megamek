/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004,2006 Ben Mazur (bmazur@sev.org)
 * Copyright © 2015 Nicholas Walczak (walczak@cs.umn.edu)
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
package megamek.client.ui.swing.skinEditor;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;

import megamek.client.TimerSingleton;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.IBoardView;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.ChatLounge;
import megamek.client.ui.swing.ChoiceDialog;
import megamek.client.ui.swing.CommonMenuBar;
import megamek.client.ui.swing.ConfirmDialog;
import megamek.client.ui.swing.DeployMinefieldDisplay;
import megamek.client.ui.swing.DeploymentDisplay;
import megamek.client.ui.swing.FiringDisplay;
import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.MovementDisplay;
import megamek.client.ui.swing.PhysicalDisplay;
import megamek.client.ui.swing.ReportDisplay;
import megamek.client.ui.swing.SelectArtyAutoHitHexDisplay;
import megamek.client.ui.swing.StatusBarPhaseDisplay;
import megamek.client.ui.swing.TargetingPhaseDisplay;
import megamek.client.ui.swing.UnitLoadingDialog;
import megamek.client.ui.swing.boardview.BoardView1;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Configuration;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.IGame;
import megamek.common.IPlayer;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.loaders.EntityLoadingException;
import megamek.common.util.Distractable;

public class SkinEditorMainGUI extends JPanel implements WindowListener,
        BoardViewListener, ActionListener, ComponentListener {

    /**
     * 
     */
    private static final long serialVersionUID = 5625499617779156289L;

    private static final String FILENAME_ICON_16X16 = "megamek-icon-16x16.png"; //$NON-NLS-1$
    private static final String FILENAME_ICON_32X32 = "megamek-icon-32x32.png"; //$NON-NLS-1$
    private static final String FILENAME_ICON_48X48 = "megamek-icon-48x48.png"; //$NON-NLS-1$
    private static final String FILENAME_ICON_256X256 = "megamek-icon-256x256.png"; //$NON-NLS-1$

    // a frame, to show stuff in
    private JFrame frame;

    // A menu bar to contain all actions.
    protected CommonMenuBar menuBar;

    private BoardView1 bv;
    private Component bvc;
    private JDialog skinSpecEditorD;
    private SkinSpecEditor skinSpecEditor;

    public JDialog mechW;
    public UnitDisplay unitDisplay;

    protected JComponent curPanel;
    private ChatLounge chatlounge;

    /**
     * Test entity to display in UnitDisplay.
     */
    private Entity testEntity;

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

    private StatusBarPhaseDisplay currPhaseDisplay;

    /**
     * The <code>CardLayout</code> of the secondary display area.
     */
    private CardLayout cardsSecondary = new CardLayout();

    /**
     * Map phase component names to phase component objects.
     */
    HashMap<String, JComponent> phaseComponents = new HashMap<String, JComponent>();

    public SkinEditorMainGUI() {
        super(new BorderLayout());
        this.addComponentListener(this);

        panMain.setLayout(cardsMain);
        panSecondary.setLayout(cardsSecondary);
        JPanel panDisplay = new JPanel(new BorderLayout());
        panDisplay.add(panMain, BorderLayout.CENTER);
        panDisplay.add(panSecondary, BorderLayout.SOUTH);
        add(panDisplay, BorderLayout.CENTER);

        UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(frame);
        if (!MechSummaryCache.getInstance().isInitialized()) {
            unitLoadingDialog.setVisible(true);
        }

        try {
            MechSummary ms = MechSummaryCache.getInstance().getMech(
                    "Archer ARC-2W");
            testEntity = new MechFileParser(ms.getSourceFile(),
                    ms.getEntryName()).getEntity();
        } catch (EntityLoadingException e) {
            e.printStackTrace();
        }
    }

    public IBoardView getBoardView() {
        return bv;
    }

    /**
     * Initializes a number of things about this frame.
     */
    private void initializeFrame() {
        frame = new JFrame(Messages.getString("ClientGUI.title")); //$NON-NLS-1$
        frame.setJMenuBar(menuBar);
        Rectangle virtualBounds = getVirtualBounds();
        int x, y, w, h;
        if (GUIPreferences.getInstance().getWindowSizeHeight() != 0) {
            x = GUIPreferences.getInstance().getWindowPosX();
            y = GUIPreferences.getInstance().getWindowPosY();
            w = GUIPreferences.getInstance().getWindowSizeWidth();
            h = GUIPreferences.getInstance().getWindowSizeHeight();
            if ((x < virtualBounds.getMinX())
                    || ((x + w) > virtualBounds.getMaxX())) {
                x = 0;
            }
            if ((y < virtualBounds.getMinY())
                    || ((y + h) > virtualBounds.getMaxY())) {
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
        frame.setMinimumSize(new Dimension(640, 480));
        frame.setBackground(SystemColor.menu);
        frame.setForeground(SystemColor.menuText);
        List<Image> iconList = new ArrayList<Image>();
        iconList.add(frame.getToolkit().getImage(
                new File(Configuration.miscImagesDir(), FILENAME_ICON_16X16)
                        .toString()));
        iconList.add(frame.getToolkit().getImage(
                new File(Configuration.miscImagesDir(), FILENAME_ICON_32X32)
                        .toString()));
        iconList.add(frame.getToolkit().getImage(
                new File(Configuration.miscImagesDir(), FILENAME_ICON_48X48)
                        .toString()));
        iconList.add(frame.getToolkit().getImage(
                new File(Configuration.miscImagesDir(), FILENAME_ICON_256X256)
                        .toString()));
        frame.setIconImages(iconList);

        mechW = new JDialog(frame, Messages.getString("ClientGUI.MechDisplay"), false);
        x = GUIPreferences.getInstance().getDisplayPosX();
        y = GUIPreferences.getInstance().getDisplayPosY();
        h = GUIPreferences.getInstance().getDisplaySizeHeight();
        w = GUIPreferences.getInstance().getDisplaySizeWidth();
        if ((x + w) > virtualBounds.getWidth()) {
            x = 0;
            w = Math.min(w, (int)virtualBounds.getWidth());
        }
        if ((y + h) > virtualBounds.getHeight()) {
            y = 0;
            h = Math.min(h, (int)virtualBounds.getHeight());
        }
        mechW.setLocation(x, y);
        mechW.setSize(w, h);
        mechW.setResizable(true);
        unitDisplay = new UnitDisplay(null);
        mechW.add(unitDisplay);
        mechW.setVisible(true);
        unitDisplay.displayEntity(testEntity);
    }

    /**
     * Lays out the frame by setting this Client object to take up the full
     * frame display area.
     */
    private void layoutFrame() {
        frame.setTitle(Messages.getString("MegaMek.SkinEditor.label") //$NON-NLS-1$ 
                + Messages.getString("ClientGUI.clientTitleSuffix")); //$NON-NLS-1$
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this, BorderLayout.CENTER);
        frame.validate();
    }

    public void updateBorder() {
        phaseComponents.clear();
        panMain.removeAll();
        panSecondary.removeAll();
        mainNames.clear();
        secondaryNames.clear();

        try {
            bv = new BoardView1(new Game(), null, null);
            bv.setPreferredSize(getSize());
            bvc = bv.getComponent();
            bvc.setName("BoardView");
        } catch (IOException e) {
            e.printStackTrace();
            doAlertDialog(Messages.getString("ClientGUI.FatalError.title"), //$NON-NLS-1$
                    Messages.getString("ClientGUI.FatalError.message") + e); //$NON-NLS-1$
            die();
        }
        switchPanel(IGame.Phase.PHASE_MOVEMENT);
        frame.validate();

        // This is a horrible hack
        // Essentially, UnitDisplay (I think specifically ArmorPanel), relies
        // upon addNotify being called, so I need to way to set the
        // isDisplayable state to true.  However, if I create a new JDialog, or
        // called JDialog.setVisible(true), focus will get stolen from the
        // Skin Spec Editor, which causes undesirable behavior, particularly
        // with the path JTextFields
        Dimension sz = mechW.getSize();
        mechW.remove(unitDisplay);
        // UnitDisplay has no way to update the skin without being recreated
        unitDisplay = new UnitDisplay(null);
        mechW.add(unitDisplay);
        if (mechW.isVisible()) {
            // This will cause the isDisplayable state to be true, in effect
            // ensuring addNotify has been called.
            mechW.pack();
        } else {
            mechW.setVisible(true);
        }
        // Packing is going to change the dimensions, so we'll restore old sz
        mechW.setSize(sz);
        unitDisplay.displayEntity(testEntity);
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
        phaseComponents.clear();
        panMain.removeAll();
        panSecondary.removeAll();
        mainNames.clear();
        secondaryNames.clear();
        menuBar = new CommonMenuBar(null);
        initializeFrame();
        try {
            // Create the board viewer.
            bv = new BoardView1(new Game(), null, null);
            bv.setPreferredSize(getSize());
            bvc = bv.getComponent();
            bvc.setName("BoardView");
        } catch (Exception e) {
            e.printStackTrace();
            doAlertDialog(Messages.getString("ClientGUI.FatalError.title"), //$NON-NLS-1$
                    Messages.getString("ClientGUI.FatalError.message") + e); //$NON-NLS-1$
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

        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice();
        int x;
        int y;
        int h;
        int w;
        skinSpecEditorD = new JDialog(frame,
                Messages.getString("SkinEditor.SkinEditorDialog.Title"), false); //$NON-NLS-1$
        x = GUIPreferences.getInstance().getDisplayPosX();
        y = GUIPreferences.getInstance().getDisplayPosY();
        h = 480;
        w = 640;
        if ((x + w) > gd.getDisplayMode().getWidth()) {
            x = 0;
            w = Math.min(w, gd.getDisplayMode().getWidth());
        }
        if ((y + h) > gd.getDisplayMode().getHeight()) {
            y = 0;
            h = Math.min(h, gd.getDisplayMode().getHeight());
        }
        skinSpecEditorD.setLocation(x, y);
        skinSpecEditorD.setSize(w, h);
        skinSpecEditorD.setResizable(true);
        skinSpecEditorD.addWindowListener(this);
        skinSpecEditor = new SkinSpecEditor(this);
        skinSpecEditorD.add(skinSpecEditor);
        skinSpecEditorD.setVisible(true);

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
        frame.pack();
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
     * Implement the <code>ActionListener</code> interface.
     */
    public void actionPerformed(ActionEvent event) {
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
    }

    /**
     * Shuts down threads and sockets
     */
    void die() {
        // Tell all the displays to remove themselves as listeners.
        boolean reportHandled = false;
        if (bv != null) {
            //cleanup our timers first
            bv.die();
        }
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

        // This is required because the ChatLounge adds the listener to the
        // MechSummaryCache that must be removed explicitly.
        if (chatlounge != null) {
            chatlounge.die();
        }
        TimerSingleton.getInstance().killTimer();
        
        if (menuBar != null) {
            menuBar.die();
            menuBar = null;
        }
    }

    public void switchPanel(IGame.Phase phase) {
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
                ReportDisplay rD = (ReportDisplay) phaseComponents.get(String
                        .valueOf(IGame.Phase.PHASE_INITIATIVE_REPORT));
                if (rD != null) {
                    rD.resetTabs();
                }
                //ChatLounge cl = (ChatLounge) phaseComponents.get(
                //        String.valueOf(IGame.Phase.PHASE_LOUNGE));
                //cb.setDoneButton(cl.butDone);
                //cl.add(cb.getComponent(), BorderLayout.SOUTH);
                getBoardView().getTilesetManager().reset();
                break;
            case PHASE_DEPLOY_MINEFIELDS:
            case PHASE_DEPLOYMENT:
            case PHASE_TARGETING:
            case PHASE_MOVEMENT:
            case PHASE_OFFBOARD:
            case PHASE_FIRING:
            case PHASE_PHYSICAL:
                break;
            case PHASE_INITIATIVE_REPORT:
            case PHASE_TARGETING_REPORT:
            case PHASE_MOVEMENT_REPORT:
            case PHASE_OFFBOARD_REPORT:
            case PHASE_FIRING_REPORT:
            case PHASE_PHYSICAL_REPORT:
            case PHASE_END_REPORT:
            case PHASE_VICTORY:
                rD = (ReportDisplay) phaseComponents.get(String
                        .valueOf(IGame.Phase.PHASE_INITIATIVE_REPORT));
                //cb.setDoneButton(rD.butDone);
                //rD.add(cb.getComponent(), GBC.eol().fill(
                //        GridBagConstraints.HORIZONTAL));
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
        if (GUIPreferences.getInstance().getFocus() ) {
            curPanel.requestFocus();
        }
    }

    public void updateButtonPanel(IGame.Phase phase) {
        if ((currPhaseDisplay != null)) {
            currPhaseDisplay.setupButtonPanel();
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
                component = new ChatLounge(null);
                chatlounge = (ChatLounge) component;
                main = "ChatLounge"; //$NON-NLS-1$
                component.setName(main);
                panMain.add(component, main);
                break;
            case PHASE_STARTING_SCENARIO:
                component = new JLabel(
                        Messages.getString("ClientGUI.StartingScenario")); //$NON-NLS-1$
                main = "JLabel-StartingScenario"; //$NON-NLS-1$
                component.setName(main);
                panMain.add(component, main);
                break;
            case PHASE_EXCHANGE:
                component = new JLabel(
                        Messages.getString("ClientGUI.TransmittingData")); //$NON-NLS-1$
                main = "JLabel-Exchange"; //$NON-NLS-1$
                component.setName(main);
                panMain.add(component, main);
                break;
            case PHASE_SET_ARTYAUTOHITHEXES:
                component = new SelectArtyAutoHitHexDisplay(null);
                main = "BoardView"; //$NON-NLS-1$
                secondary = "SelectArtyAutoHitHexDisplay"; //$NON-NLS-1$
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay)(component);
                panSecondary.add(component, secondary);
                break;
            case PHASE_DEPLOY_MINEFIELDS:
                component = new DeployMinefieldDisplay(null);
                main = "BoardView"; //$NON-NLS-1$
                secondary = "DeployMinefieldDisplay"; //$NON-NLS-1$
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay)(component);
                panSecondary.add(component, secondary);
                break;
            case PHASE_DEPLOYMENT:
                component = new DeploymentDisplay(null);                
                main = "BoardView"; //$NON-NLS-1$
                secondary = "DeploymentDisplay"; //$NON-NLS-1$
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay)(component);
                panSecondary.add(component, secondary);
                break;
            case PHASE_TARGETING:
                component = new TargetingPhaseDisplay(null, false);
                ((TargetingPhaseDisplay) component).initializeListeners();
                main = "BoardView"; //$NON-NLS-1$
                secondary = "TargetingPhaseDisplay"; //$NON-NLS-1$
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay)(component);
                panSecondary.add(component, secondary);
                break;
            case PHASE_MOVEMENT:
                component = new MovementDisplay(null);
                main = "BoardView"; //$NON-NLS-1$
                secondary = "MovementDisplay"; //$NON-NLS-1$
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay)(component);
                panSecondary.add(component, secondary);
                break;
            case PHASE_OFFBOARD:
                component = new TargetingPhaseDisplay(null, true);
                ((TargetingPhaseDisplay) component).initializeListeners();
                main = "BoardView"; //$NON-NLS-1$
                secondary = "OffboardDisplay"; //$NON-NLS-1$
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay)(component);
                panSecondary.add(component, secondary);
                break;
            case PHASE_FIRING:
                component = new FiringDisplay(null);
                main = "BoardView"; //$NON-NLS-1$
                secondary = "FiringDisplay"; //$NON-NLS-1$
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay)(component);
                panSecondary.add(component, secondary);
                break;
            case PHASE_PHYSICAL:
                component = new PhysicalDisplay(null);
                main = "BoardView"; //$NON-NLS-1$
                secondary = "PhysicalDisplay"; //$NON-NLS-1$
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay)(component);
                panSecondary.add(component, secondary);
                break;
            case PHASE_INITIATIVE_REPORT:
                component = new ReportDisplay(null);
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
                component = phaseComponents.get(String
                        .valueOf(IGame.Phase.PHASE_INITIATIVE_REPORT));
                if (component == null) {
                    // no ReportDisplay to reuse -- get a new one
                    component = initializePanel(
                            IGame.Phase.PHASE_INITIATIVE_REPORT);
                }
                main = "ReportDisplay"; //$NON-NLS-1$
                break;
            default:
                component = new JLabel(
                        Messages.getString("ClientGUI.waitingOnTheServer")); //$NON-NLS-1$
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
  
    /**
     * Sets the visibility of the entity display window
     */
    public void setDisplayVisible(boolean visible) {
        skinSpecEditorD.setVisible(visible);
        if (visible) {
            frame.requestFocus();
        }
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
        JScrollPane scrollPane = new JScrollPane(textArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        textArea.setText("<pre>" + message + "</pre>");
        scrollPane.setPreferredSize(new Dimension(
                (int) (getSize().getWidth() / 1.5), (int) (getSize()
                        .getHeight() / 1.5)));
        JOptionPane.showMessageDialog(frame, scrollPane, title,
                JOptionPane.ERROR_MESSAGE);
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

    //
    // WindowListener
    //
    public void windowActivated(WindowEvent windowEvent) {
        // TODO: this is a kludge to fix a window iconify issue
        // For some reason when I click on the window button, the main UI 
        // window doesn't deiconify.  This fix doesn't allow me to iconify the
        // window by clicking the window button, but it's better than the
        // alternative
        frame.setState(Frame.NORMAL);
    }

    public void windowClosed(WindowEvent windowEvent) {
        // ignored
    }

    public void windowClosing(WindowEvent windowEvent) {
        if (windowEvent.getWindow().equals(skinSpecEditorD)) {
            setDisplayVisible(false);
            die();
        }
    }

    public void windowDeactivated(WindowEvent windowEvent) {
        // ignored
    }

    public void windowDeiconified(WindowEvent windowEvent) {
        // TODO: this is a kludge to fix a window iconify issue
        // For some reason when I click on the window button, the main UI 
        // window doesn't deiconify.  This fix doesn't allow me to iconify the
        // window by clicking the window button, but it's better than the
        // alternative
        frame.setState(Frame.NORMAL);
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
   
    public void loadPreviewImage(JLabel bp, Entity entity, IPlayer player) {
        Image camo = null;
        if (entity.getCamoFileName() != null) {
            camo = bv.getTilesetManager().getEntityCamo(entity);
        } else {
            camo = bv.getTilesetManager().getPlayerCamo(player);
        }
        int tint = PlayerColors.getColorRGB(player.getColorIndex());
        bp.setIcon(new ImageIcon(bv.getTilesetManager().loadPreviewImage(
                entity, camo, tint, bp)));
    }

   
    public void hexMoused(BoardViewEvent b) {
        if (b.getType() == BoardViewEvent.BOARD_HEX_POPUP) {
            
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

	@Override
	public void componentHidden(ComponentEvent arg0) {
	}

	@Override
	public void componentMoved(ComponentEvent arg0) {
	}

	@Override
	public void componentResized(ComponentEvent arg0) {
		bv.setPreferredSize(getSize());		
	}

	@Override
	public void componentShown(ComponentEvent arg0) {
	}

    /**
     * Returns the panel for the current phase. The ClientGUI is split into the
     * main panel (view) at the top, which takes up the majority of the view and
     * the the "current panel" which has different controls based on the phase.
     * 
     * @return
     */
    public JComponent getCurrentPanel() {
        return curPanel;
    }

    /**
     * Returns the 'virtual bounds' of the screen.  That is, the union of the
     * displayable space on all available screen devices.
     *
     * @return
     */
    private Rectangle getVirtualBounds() {
        Rectangle virtualBounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (GraphicsDevice gd : gs) {
            GraphicsConfiguration[] gc = gd.getConfigurations();
            for (GraphicsConfiguration element : gc) {
                virtualBounds = virtualBounds.union(element.getBounds());
            }
        }
        return virtualBounds;
    }
}
