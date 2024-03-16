/*
 * MegaMek - Copyright (C) 2000-2004, 2006 Ben Mazur (bmazur@sev.org)
 * Copyright © 2015 Nicholas Walczak (walczak@cs.umn.edu)
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
package megamek.client.ui.swing.skinEditor;

import megamek.client.TimerSingleton;
import megamek.client.event.BoardViewEvent;
import megamek.client.event.BoardViewListener;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.*;
import megamek.client.ui.swing.boardview.BoardView;
import megamek.client.ui.swing.lobby.ChatLounge;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.*;
import megamek.common.enums.GamePhase;
import megamek.common.icons.Camouflage;
import megamek.common.util.Distractable;
import megamek.common.util.fileUtils.MegaMekFile;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class SkinEditorMainGUI extends JPanel implements WindowListener, BoardViewListener,
        ActionListener, ComponentListener {
    private static final long serialVersionUID = 5625499617779156289L;

    private static final String FILENAME_ICON_16X16 = "megamek-icon-16x16.png";
    private static final String FILENAME_ICON_32X32 = "megamek-icon-32x32.png";
    private static final String FILENAME_ICON_48X48 = "megamek-icon-48x48.png";
    private static final String FILENAME_ICON_256X256 = "megamek-icon-256x256.png";

    // a frame, to show stuff in
    private JFrame frame;

    // A menu bar to contain all actions.
    protected CommonMenuBar menuBar;

    private BoardView bv;
    private Component bvc;
    private JDialog skinSpecEditorD;
    private SkinSpecEditor skinSpecEditor;

    private UnitDisplay unitDisplay;
    public JDialog mechW;

    protected JComponent curPanel;

    /**
     * Test entity to display in UnitDisplay.
     */
    private Entity testEntity;

    /**
     * Map each phase to the name of the card for the main display area.
     */
    private HashMap<String, String> mainNames = new HashMap<>();

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
    private HashMap<String, String> secondaryNames = new HashMap<>();

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
    HashMap<String, JComponent> phaseComponents = new HashMap<>();

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
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
        }
    }

    public BoardView getBoardView() {
        return bv;
    }

    /**
     * Initializes a number of things about this frame.
     */
    private void initializeFrame() {
        frame = new JFrame(Messages.getString("ClientGUI.title"));
        frame.setJMenuBar(menuBar);

        Rectangle virtualBounds = UIUtil.getVirtualBounds();
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
        List<Image> iconList = new ArrayList<>();
        iconList.add(frame.getToolkit().getImage(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_16X16)
                        .toString()));
        iconList.add(frame.getToolkit().getImage(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_32X32)
                        .toString()));
        iconList.add(frame.getToolkit().getImage(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_48X48)
                        .toString()));
        iconList.add(frame.getToolkit().getImage(
                new MegaMekFile(Configuration.miscImagesDir(), FILENAME_ICON_256X256)
                        .toString()));
        frame.setIconImages(iconList);

        unitDisplay = new UnitDisplay(null);

        mechW = new JDialog(frame, Messages.getString("ClientGUI.MechDisplay"), false);
        mechW.setResizable(true);
        mechW.add(unitDisplay);
        mechW.setVisible(true);
        unitDisplay.displayEntity(testEntity);
    }

    /**
     * Lays out the frame by setting this Client object to take up the full
     * frame display area.
     */
    private void layoutFrame() {
        frame.setTitle(Messages.getString("MegaMek.SkinEditor.label")
                + Messages.getString("ClientGUI.clientTitleSuffix"));
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

        final Game testGame = new Game();
        testEntity.setGame(testGame);

        try {
            bv = new BoardView(testGame, null, null);
            bv.setPreferredSize(getSize());
            bvc = bv.getComponent();
            bvc.setName("BoardView");
        } catch (Exception e) {
            LogManager.getLogger().error("", e);
            doAlertDialog(Messages.getString("ClientGUI.FatalError.title"),
                    Messages.getString("ClientGUI.FatalError.message") + e);
            die();
        }
        switchPanel(GamePhase.MOVEMENT);
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
     * <p>
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
        menuBar = new CommonMenuBar();

        final Game testGame = new Game();
        testEntity.setGame(testGame);

        initializeFrame();

        try {
            // Create the board viewer.
            bv = new BoardView(testGame, null, null);
            bv.setPreferredSize(getSize());
            bvc = bv.getComponent();
            bvc.setName("BoardView");
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
            doAlertDialog(Messages.getString("ClientGUI.FatalError.title"),
                    Messages.getString("ClientGUI.FatalError.message") + ex);
            die();
        }

        layoutFrame();
        menuBar.addActionListener(this);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                frame.setVisible(false);
                saveSettings();
                die();
            }
        });

        skinSpecEditor = new SkinSpecEditor(this);

        skinSpecEditorD = new JDialog(frame,
                Messages.getString("SkinEditor.SkinEditorDialog.Title"), false);
        skinSpecEditorD.setSize(640, 480);
        skinSpecEditorD.setResizable(true);

        skinSpecEditorD.addWindowListener(this);
        skinSpecEditorD.add(skinSpecEditor);
        skinSpecEditorD.setVisible(true);

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
    @Override
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
            // cleanup our timers first
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
        } catch (Throwable t) {
            LogManager.getLogger().error("", t);
        }

        TimerSingleton.getInstance().killTimer();

        if (menuBar != null) {
            menuBar.die();
            menuBar = null;
        }
    }

    public void switchPanel(GamePhase phase) {
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
            case LOUNGE:
                getBoardView().getTilesetManager().reset();
                break;
            case DEPLOY_MINEFIELDS:
            case DEPLOYMENT:
            case TARGETING:
            case MOVEMENT:
            case OFFBOARD:
            case FIRING:
            case PHYSICAL:
                break;
            case INITIATIVE_REPORT:
            case TARGETING_REPORT:
            case MOVEMENT_REPORT:
            case OFFBOARD_REPORT:
            case FIRING_REPORT:
            case PHYSICAL_REPORT:
            case END_REPORT:
            case VICTORY:
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
        if (GUIPreferences.getInstance().getFocus()) {
            curPanel.requestFocus();
        }
    }

    public void updateButtonPanel(GamePhase phase) {
        if ((currPhaseDisplay != null)) {
            currPhaseDisplay.setupButtonPanel();
        }
    }

    private JComponent initializePanel(GamePhase phase) {
        // Create the components for this phase.
        String name = String.valueOf(phase);
        JComponent component;
        String secondary = null;
        String main;
        switch (phase) {
            case LOUNGE:
                component = new ChatLounge(null);
                main = "ChatLounge";
                component.setName(main);
                panMain.add(component, main);
                break;
            case STARTING_SCENARIO:
                component = new JLabel(Messages.getString("ClientGUI.StartingScenario"));
                main = "JLabel-StartingScenario";
                component.setName(main);
                panMain.add(component, main);
                break;
            case EXCHANGE:
                component = new JLabel(Messages.getString("ClientGUI.TransmittingData"));
                main = "JLabel-Exchange";
                component.setName(main);
                panMain.add(component, main);
                break;
            case SET_ARTILLERY_AUTOHIT_HEXES:
                component = new SelectArtyAutoHitHexDisplay(null);
                main = "BoardView";
                secondary = "SelectArtyAutoHitHexDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case DEPLOY_MINEFIELDS:
                component = new DeployMinefieldDisplay(null);
                main = "BoardView";
                secondary = "DeployMinefieldDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case DEPLOYMENT:
                component = new DeploymentDisplay(null);
                main = "BoardView";
                secondary = "DeploymentDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case TARGETING:
                component = new TargetingPhaseDisplay(null, false);
                ((TargetingPhaseDisplay) component).initializeListeners();
                main = "BoardView";
                secondary = "TargetingPhaseDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case MOVEMENT:
                component = new MovementDisplay(null);
                main = "BoardView";
                secondary = "MovementDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case OFFBOARD:
                component = new TargetingPhaseDisplay(null, true);
                ((TargetingPhaseDisplay) component).initializeListeners();
                main = "BoardView";
                secondary = "OffboardDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case FIRING:
                component = new FiringDisplay(null);
                main = "BoardView";
                secondary = "FiringDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case PHYSICAL:
                component = new PhysicalDisplay(null);
                main = "BoardView";
                secondary = "PhysicalDisplay";
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(bvc, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case INITIATIVE_REPORT:
                component = new ReportDisplay(null);
                main = "ReportDisplay";
                component.setName(main);
                panMain.add(main, component);
                break;
            case TARGETING_REPORT:
            case MOVEMENT_REPORT:
            case OFFBOARD_REPORT:
            case FIRING_REPORT:
            case PHYSICAL_REPORT:
            case END_REPORT:
            case VICTORY:
                // Try to reuse the ReportDisplay for other phases...
                component = phaseComponents.get(String.valueOf(GamePhase.INITIATIVE_REPORT));
                if (component == null) {
                    // no ReportDisplay to reuse -- get a new one
                    component = initializePanel(GamePhase.INITIATIVE_REPORT);
                }
                main = "ReportDisplay";
                break;
            default:
                component = new JLabel(Messages.getString("ClientGUI.waitingOnTheServer"));
                main = "JLabel-Default";
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
        Report.setupStylesheet(textArea);

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
     * <p>
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
    @Override
    public void windowActivated(WindowEvent evt) {
        // TODO: this is a kludge to fix a window iconify issue
        // For some reason when I click on the window button, the main UI
        // window doesn't deiconify. This fix doesn't allow me to iconify the
        // window by clicking the window button, but it's better than the
        // alternative
        frame.setState(Frame.NORMAL);
    }

    @Override
    public void windowClosed(WindowEvent evt) {

    }

    @Override
    public void windowClosing(WindowEvent evt) {
        if (evt.getWindow().equals(skinSpecEditorD)) {
            setDisplayVisible(false);
            die();
        }
    }

    @Override
    public void windowDeactivated(WindowEvent evt) {

    }

    @Override
    public void windowDeiconified(WindowEvent evt) {
        // TODO : this is a kludge to fix a window iconify issue
        // For some reason when I click on the window button, the main UI
        // window doesn't deiconify. This fix doesn't allow me to iconify the
        // window by clicking the window button, but it's better than the
        // alternative
        frame.setState(Frame.NORMAL);
    }

    @Override
    public void windowIconified(WindowEvent evt) {

    }

    @Override
    public void windowOpened(WindowEvent evt) {

    }

    /**
     * @return the frame this client is displayed in
     */
    public JFrame getFrame() {
        return frame;
    }

    public void loadPreviewImage(JLabel bp, Entity entity, Player player) {
        final Camouflage camouflage = entity.getCamouflageOrElse(player.getCamouflage());
        bp.setIcon(new ImageIcon(bv.getTilesetManager().loadPreviewImage(entity, camouflage, bp)));
    }

    @Override
    public void hexMoused(BoardViewEvent evt) {

    }

    @Override
    public void hexCursor(BoardViewEvent evt) {

    }

    @Override
    public void boardHexHighlighted(BoardViewEvent evt) {

    }

    @Override
    public void hexSelected(BoardViewEvent evt) {

    }

    @Override
    public void firstLOSHex(BoardViewEvent evt) {

    }

    @Override
    public void secondLOSHex(BoardViewEvent evt, Coords c) {

    }

    @Override
    public void finishedMovingUnits(BoardViewEvent evt) {

    }

    @Override
    public void unitSelected(BoardViewEvent evt) {

    }

    @Override
    public void componentHidden(ComponentEvent evt) {

    }

    @Override
    public void componentMoved(ComponentEvent evt) {

    }

    @Override
    public void componentResized(ComponentEvent evt) {
        bv.setPreferredSize(getSize());
    }

    @Override
    public void componentShown(ComponentEvent evt) {

    }

    /**
     * The ClientGUI is split into the main panel (view) at the top, which takes up the majority of
     * the view and the "current panel" which has different controls based on the phase.
     *
     * @return the panel for the current phase
     */
    public JComponent getCurrentPanel() {
        return curPanel;
    }
}
