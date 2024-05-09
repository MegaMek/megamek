/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui.swing;

import megamek.client.SBFClient;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.common.InGameObject;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListener;
import megamek.common.util.Distractable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

public class SBFClientGUI extends AbstractClientGUI implements ActionListener {

    public static final String CG_BOARDVIEW = "BoardView";
    public static final String CG_CHATLOUNGE = "ChatLounge";
    public static final String CG_STARTINGSCENARIO = "JLabel-StartingScenario";
    public static final String CG_EXCHANGE = "JLabel-Exchange";
    public static final String CG_SELECTARTYAUTOHITHEXDISPLAY = "SelectArtyAutoHitHexDisplay";
    public static final String CG_DEPLOYMINEFIELDDISPLAY = "DeployMinefieldDisplay";
    public static final String CG_DEPLOYMENTDISPLAY = "DeploymentDisplay";
    public static final String CG_TARGETINGPHASEDISPLAY = "TargetingPhaseDisplay";
    public static final String CG_PREMOVEMENTDISPLAY = "PremovementDisplay";
    public static final String CG_MOVEMENTDISPLAY = "MovementDisplay";
    public static final String CG_OFFBOARDDISPLAY = "OffboardDisplay";
    public static final String CG_PREFIRING = "Prefiring";
    public static final String CG_FIRINGDISPLAY = "FiringDisplay";
    public static final String CG_POINTBLANKSHOTDISPLAY = "PointblankShotDisplay";
    public static final String CG_PHYSICALDISPLAY = "PhysicalDisplay";
    public static final String CG_REPORTDISPLAY = "ReportDisplay";
    public static final String CG_DEFAULT = "JLabel-Default";

    private final SBFClient client;

    // a frame, to show stuff in
    private final JPanel clientGuiPanel = new JPanel();

    protected JComponent curPanel;
    private final JPanel panTop = new JPanel(new BorderLayout());

    /**
     * The <code>JPanel</code> containing the main display area.
     */
    private final JPanel panMain = new JPanel();

    /**
     * The <code>CardLayout</code> of the main display area.
     */
    private final CardLayout cardsMain = new CardLayout();

    /**
     * Map each phase to the name of the card for the secondary area.
     */
    private final Map<String, String> secondaryNames = new HashMap<>();

    /**
     * The <code>JPanel</code> containing the secondary display area.
     */
    private final JPanel panSecondary = new JPanel();


    private ReportDisplay reportDisply;

    private StatusBarPhaseDisplay currPhaseDisplay;

    /**
     * The <code>CardLayout</code> of the secondary display area.
     */
    private final CardLayout cardsSecondary = new CardLayout();

    /**
     * Map phase component names to phase component objects.
     */
    private final Map<String, JComponent> phaseComponents = new HashMap<>();

    /**
     * Map each phase to the name of the card for the main display area.
     */
    private final Map<String, String> mainNames = new HashMap<>();

    private final GameListener gameListener = new SBFClientGUIGameListener(this);
    private final CommonMenuBar menuBar = CommonMenuBar.getMenuBarForGame();

    public SBFClientGUI(SBFClient client, MegaMekController c) {
        super(client);
        this.client = client;
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(clientGuiPanel, BorderLayout.CENTER);
//        frame.getContentPane().add(new UnderConstructionPanel(), BorderLayout.CENTER);
        frame.setJMenuBar(menuBar);
        menuBar.addActionListener(this);
        panMain.setLayout(cardsMain);
        panSecondary.setLayout(cardsSecondary);

        panMain.add("UnderConstruction", new UnderConstructionPanel());

        clientGuiPanel.setLayout(new BorderLayout());
        clientGuiPanel.addComponentListener(resizeListener);
        clientGuiPanel.add(panMain, BorderLayout.CENTER);
        clientGuiPanel.add(panSecondary, BorderLayout.SOUTH);
    }

    private final ComponentListener resizeListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent evt) {
            boardViewsContainer.getPanel().setPreferredSize(clientGuiPanel.getSize());
        }
    };

    @Override
    public SBFClient getClient() {
        return client;
    }

    @Override
    public JComponent turnTimerComponent() {
        return menuBar;
    }

    @Override
    public void initialize() {
        super.initialize();
        client.getGame().addGameListener(gameListener);
        frame.setVisible(true);
    }

    @Override
    protected boolean saveGame() {
        //TODO
        return true;
    }

    @Override
    public void die() {
        super.die();
        client.getGame().removeGameListener(gameListener);
    }

    @Override
    public InGameObject getSelectedUnit() {
        return null;
    }

    protected void switchPanel(GamePhase phase) {
        // Clear the old panel's listeners.
//        if (curPanel instanceof BoardViewListener) {
//            bv.removeBoardViewListener((BoardViewListener) curPanel);
//        }

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
//                // reset old report tabs and images, if any
//                ChatLounge cl = (ChatLounge) phaseComponents.get(String.valueOf(GamePhase.LOUNGE));
//                cb.setDoneButton(cl.butDone);
//                cl.setBottom(cb.getComponent());
//                getBoardView().getTilesetManager().reset();
                break;
            default:
                break;
        }

//        maybeShowMinimap();
//        maybeShowUnitDisplay();
//        maybeShowForceDisplay();
//        maybeShowMiniReport();
//        maybeShowPlayerList();

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
//        if (curPanel instanceof BoardViewListener) {
//            bv.addBoardViewListener((BoardViewListener) curPanel);
//        }

        if (curPanel instanceof ActionListener) {
            menuBar.addActionListener((ActionListener) curPanel);
        }

        if (curPanel instanceof Distractable) {
            ((Distractable) curPanel).setIgnoringEvents(false);
        }

        // Make the new panel the focus, if the Client option says so
//        if (GUIP.getFocus() && !(client instanceof BotClient)) {
//            curPanel.requestFocus();
//        }
    }

    private void initializeSingleComponent(GamePhase phase, JComponent component, String identifier) {
        component.setName(identifier);
        panMain.add(component, identifier);
        String phaseName = String.valueOf(phase);
        phaseComponents.put(phaseName, component);
        mainNames.put(phaseName, identifier);
    }

    private void initializeWithBoardView(GamePhase phase, StatusBarPhaseDisplay component, String secondary) {
        String identifier = CG_BOARDVIEW;
        String phaseName = String.valueOf(phase);
        component.setName(identifier);
        panMain.add(component, identifier);
        if (!mainNames.containsValue(identifier)) {
            panMain.add(panTop, identifier);
        }
        currPhaseDisplay = component;
        panSecondary.add(component, secondary);
        phaseComponents.put(phaseName, component);
        mainNames.put(phaseName, identifier);
        if (secondary != null) {
            secondaryNames.put(phaseName, secondary);
        }
    }

    private JComponent initializePanel(GamePhase phase) {
        // Create the components for this phase.
        JComponent component = new ReceivingGameDataPanel();
        String secondary;
        String main = CG_BOARDVIEW;
        switch (phase) {
            case LOUNGE:
//                initializeSingleComponent(phase, new ChatLounge(this), CG_CHATLOUNGE);
                break;
            case STARTING_SCENARIO:
                initializeSingleComponent(phase, new StartingScenarioPanel(), CG_STARTINGSCENARIO);
                break;
            case EXCHANGE:
                initializeSingleComponent(phase, new ReceivingGameDataPanel(), CG_EXCHANGE);
                break;
            case SET_ARTILLERY_AUTOHIT_HEXES:
//                initializeWithBoardView(phase, new SelectArtyAutoHitHexDisplay(this), CG_SELECTARTYAUTOHITHEXDISPLAY);
                break;
            case DEPLOY_MINEFIELDS:
//                initializeWithBoardView(phase, new DeployMinefieldDisplay(this), CG_DEPLOYMINEFIELDDISPLAY);
                break;
            case DEPLOYMENT:
//                initializeWithBoardView(phase, new DeploymentDisplay(this), CG_DEPLOYMINEFIELDDISPLAY);
                break;
            case TARGETING:
//                initializeWithBoardView(phase, new TargetingPhaseDisplay(this, false), CG_DEPLOYMINEFIELDDISPLAY);
//                component = new TargetingPhaseDisplay(this, false);
//                ((TargetingPhaseDisplay) component).initializeListeners();
                secondary = CG_TARGETINGPHASEDISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
//                offBoardOverlay.setTargetingPhaseDisplay((TargetingPhaseDisplay) component);
                break;
            case PREMOVEMENT:
//                component = new PrephaseDisplay(this, GamePhase.PREMOVEMENT);
//                ((PrephaseDisplay) component).initializeListeners();
                secondary = CG_PREMOVEMENTDISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case MOVEMENT:
//                initializeWithBoardView(phase, new MovementDisplay(this), CG_MOVEMENTDISPLAY);
                break;
            case OFFBOARD:
//                component = new TargetingPhaseDisplay(this, true);
//                ((TargetingPhaseDisplay) component).initializeListeners();
                secondary = CG_OFFBOARDDISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case PREFIRING:
//                component = new PrephaseDisplay(this, GamePhase.PREFIRING);
//                ((PrephaseDisplay) component).initializeListeners();
                secondary = CG_PREFIRING;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case FIRING:
//                initializeWithBoardView(phase, new FiringDisplay(this), CG_FIRINGDISPLAY);
                break;
            case POINTBLANK_SHOT:
//                initializeWithBoardView(phase, new PointblankShotDisplay(this), CG_POINTBLANKSHOTDISPLAY);
                break;
            case PHYSICAL:
//                initializeWithBoardView(phase, new PhysicalDisplay(this), CG_PHYSICALDISPLAY);
                break;
            case INITIATIVE_REPORT:
            case TARGETING_REPORT:
            case MOVEMENT_REPORT:
            case OFFBOARD_REPORT:
            case FIRING_REPORT:
            case PHYSICAL_REPORT:
            case END_REPORT:
            case VICTORY:
//                initializeWithBoardView(phase, new JPanel(), CG_PHYSICALDISPLAY);
                secondary = CG_REPORTDISPLAY;
                if (reportDisply == null) {
//                    reportDisply = new JPanel();
//                    reportDisply = new ReportDisplay(this);
//                    reportDisply.setName(secondary);
                }
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = reportDisply;
                component = reportDisply;
//                if (!secondaryNames.containsValue(secondary)) {
//                    panSecondary.add(reportDisply, secondary);
//                }
                break;
            default:
                component = new WaitingForServerPanel();
                main = CG_DEFAULT;
                component.setName(main);
                panMain.add(main, component);
                break;
        }
        return component;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }
}
