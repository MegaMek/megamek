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
import megamek.client.event.BoardViewListener;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.InGameObject;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListener;
import megamek.common.util.Distractable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

    protected JComponent curPanel;
    private JPanel panTop;

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
        frame.getContentPane().add(new UnderConstructionPanel(), BorderLayout.CENTER);
        frame.setJMenuBar(menuBar);
        menuBar.addActionListener(this);
    }

    @Override
    public void initialize() {
        super.initialize();
        client.getIGame().addGameListener(gameListener);
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
        client.getIGame().removeGameListener(gameListener);
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
            case POINTBLANK_SHOT:
            case SET_ARTILLERY_AUTOHIT_HEXES:
            case DEPLOY_MINEFIELDS:
            case DEPLOYMENT:
            case TARGETING:
            case PREMOVEMENT:
            case MOVEMENT:
            case OFFBOARD:
            case PREFIRING:
            case FIRING:
            case PHYSICAL:
            case INITIATIVE_REPORT:
            case TARGETING_REPORT:
            case MOVEMENT_REPORT:
            case OFFBOARD_REPORT:
            case FIRING_REPORT:
            case PHYSICAL_REPORT:
            case END_REPORT:
            case VICTORY:
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

    private JComponent initializePanel(GamePhase phase) {
        // Create the components for this phase.
        String name = String.valueOf(phase);
        JComponent component = new ReceivingGameDataPanel();
        String secondary = null;
        String main;
        switch (phase) {
            case LOUNGE:
//                component = new ChatLounge(this);
//                chatlounge = (ChatLounge) component;
                main = CG_CHATLOUNGE;
                component.setName(main);
                panMain.add(component, main);
                break;
            case STARTING_SCENARIO:
                component = new JLabel(Messages.getString("ClientGUI.StartingScenario"));
                UIUtil.scaleComp(component, UIUtil.FONT_SCALE1);
                main = CG_STARTINGSCENARIO;
                component.setName(main);
                panMain.add(component, main);
                break;
            case EXCHANGE:
//                chatlounge.killPreviewBV();
                component = new ReceivingGameDataPanel();
                UIUtil.scaleComp(component, UIUtil.FONT_SCALE1);
                main = CG_EXCHANGE;
                component.setName(main);
                panMain.add(component, main);
                break;
            case SET_ARTILLERY_AUTOHIT_HEXES:
//                component = new SelectArtyAutoHitHexDisplay(this);
                main = CG_BOARDVIEW;
                secondary = CG_SELECTARTYAUTOHITHEXDISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case DEPLOY_MINEFIELDS:
//                component = new DeployMinefieldDisplay(this);
                main = CG_BOARDVIEW;
                secondary = CG_DEPLOYMINEFIELDDISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case DEPLOYMENT:
//                component = new DeploymentDisplay(this);
                main = CG_BOARDVIEW;
                secondary = CG_DEPLOYMENTDISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case TARGETING:
//                component = new TargetingPhaseDisplay(this, false);
//                ((TargetingPhaseDisplay) component).initializeListeners();
                main = CG_BOARDVIEW;
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
                main = CG_BOARDVIEW;
                secondary = CG_PREMOVEMENTDISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case MOVEMENT:
//                component = new MovementDisplay(this);
                main = CG_BOARDVIEW;
                secondary = CG_MOVEMENTDISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case OFFBOARD:
//                component = new TargetingPhaseDisplay(this, true);
//                ((TargetingPhaseDisplay) component).initializeListeners();
                main = CG_BOARDVIEW;
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
                main = CG_BOARDVIEW;
                secondary = CG_PREFIRING;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case FIRING:
//                component = new FiringDisplay(this);
                main = CG_BOARDVIEW;
                secondary = CG_FIRINGDISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case POINTBLANK_SHOT:
//                component = new PointblankShotDisplay(this);
                main = CG_BOARDVIEW;
                secondary = CG_POINTBLANKSHOTDISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case PHYSICAL:
//                component = new PhysicalDisplay(this);
                main = CG_BOARDVIEW;
                secondary = CG_PHYSICALDISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case INITIATIVE_REPORT:
            case TARGETING_REPORT:
            case MOVEMENT_REPORT:
            case OFFBOARD_REPORT:
            case FIRING_REPORT:
            case PHYSICAL_REPORT:
            case END_REPORT:
            case VICTORY:
                main = CG_BOARDVIEW;
                secondary = CG_REPORTDISPLAY;
                if (reportDisply == null) {
//                    reportDisply = new ReportDisplay(this);
//                    reportDisply.setName(secondary);
                }
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = reportDisply;
                component = reportDisply;
                if (!secondaryNames.containsValue(secondary)) {
                    panSecondary.add(reportDisply, secondary);
                }
                break;
            default:
                component = new WaitingForServerPanel();
                main = CG_DEFAULT;
                secondary = main;
                component.setName(main);
                panMain.add(main, component);
                break;
        }
        phaseComponents.put(name, component);
        mainNames.put(name, main);
        if (secondary != null) {
            secondaryNames.put(name, secondary);
        }

        return component;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

    }
}
