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
import megamek.client.ui.swing.boardview.*;
import megamek.client.ui.swing.sbf.SBFMovementDisplay;
import megamek.client.ui.swing.util.MegaMekController;
import megamek.client.ui.swing.widget.SBFReportPanel;
import megamek.common.Coords;
import megamek.common.Game;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListener;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFMovePath;
import megamek.common.util.Distractable;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
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
    private JSplitPane splitPaneA;
    private JPanel panA1;
    private JPanel panA2;

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


    private SBFReportDisplay reportDisplay;

    private StatusBarPhaseDisplay currPhaseDisplay;

    /**
     * The <code>CardLayout</code> of the secondary display area.
     */
    private final CardLayout cardsSecondary = new CardLayout();

    /**
     * Map phase component names to phase component objects.
     */
    private final Map<String, JComponent> phaseComponents = new HashMap<>();

    private JDialog miniReportDisplayDialog;
    private SBFReportPanel reportPanel;

    /**
     * Map each phase to the name of the card for the main display area.
     */
    private final Map<String, String> mainNames = new HashMap<>();

    private final GameListener gameListener = new SBFClientGUIGameListener(this);
    private final CommonMenuBar menuBar = CommonMenuBar.getMenuBarForGame();
    private BoardView bv;
    private SBFFormationSpriteHandler formationSpriteHandler;
    private MovementEnvelopeSpriteHandler movementEnvelopeHandler;
    private MovePathSpriteHandler movePathSpriteHandler;

    public SBFClientGUI(SBFClient client, MegaMekController c) {
        super(client);
        this.client = client;
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(clientGuiPanel, BorderLayout.CENTER);
        frame.setJMenuBar(menuBar);
        frame.setTitle(client.getName() + Messages.getString("ClientGUI.clientTitleSuffix"));
        menuBar.addActionListener(this);
        panMain.setLayout(cardsMain);
        panSecondary.setLayout(cardsSecondary);

        panMain.add("UnderConstruction", new UnderConstructionPanel());

        clientGuiPanel.setLayout(new BorderLayout());
        clientGuiPanel.addComponentListener(resizeListener);
        clientGuiPanel.add(panMain, BorderLayout.CENTER);
        clientGuiPanel.add(panSecondary, BorderLayout.SOUTH);

        miniReportDisplayDialog = new JDialog(getFrame());
        reportPanel = new SBFReportPanel(this);
    }

    private final ComponentListener resizeListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent evt) {
            boardViewsContainer.getPanel().setPreferredSize(clientGuiPanel.getSize());
        }
    };

    /**
     * Initializes a number of things about this frame.
     */
    @Override
    protected void initializeFrame() {
        super.initializeFrame();
        frame.setJMenuBar(menuBar);
    }

    protected Game bvGame = new Game();

    @Override
    public void initialize() {
        initializeFrame();
        super.initialize();
        try {
            client.getGame().addGameListener(gameListener);
            bv = new BoardView(bvGame, MegaMekGUI.getKeyDispatcher(), null);
            bv.setTooltipProvider(new SBFBoardViewTooltip(client.getGame(), bv));
            boardViews.put(0, bv);
            bv.addOverlay(new KeyBindingsOverlay(bv));
            bv.addOverlay(new PlanetaryConditionsOverlay(bv));
            bv.getPanel().setPreferredSize(clientGuiPanel.getSize());
            boardViewsContainer.setName(CG_BOARDVIEW);
            boardViewsContainer.updateMapTabs();
            initializeSpriteHandlers();

            panA1 = new JPanel();
            panA1.setVisible(false);
            panA2 = new JPanel();
            panA2.setVisible(false);
            panA2.add(boardViewsContainer.getPanel());
            panA2.setVisible(true);

            splitPaneA = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

            splitPaneA.setDividerSize(10);
            splitPaneA.setResizeWeight(0.5);

            splitPaneA.setLeftComponent(panA1);
            splitPaneA.setRightComponent(panA2);

            panTop.add(splitPaneA, BorderLayout.CENTER);

        } catch (Exception ex) {
            LogManager.getLogger().fatal("", ex);
            die();
        }

        menuBar.addActionListener(this);
        client.changePhase(GamePhase.UNKNOWN);
        frame.setVisible(true);
    }

    private void initializeSpriteHandlers() {
        movementEnvelopeHandler = new MovementEnvelopeSpriteHandler(bv, client.getGame());
        formationSpriteHandler = new SBFFormationSpriteHandler(bv, client);
        movePathSpriteHandler = new MovePathSpriteHandler(bv);
        spriteHandlers.addAll(List.of(formationSpriteHandler, movementEnvelopeHandler, movePathSpriteHandler));
        spriteHandlers.forEach(BoardViewSpriteHandler::initialize);
    }

    @Override
    public SBFClient getClient() {
        return client;
    }

    @Override
    public JComponent turnTimerComponent() {
        return menuBar;
    }

    @Override
    public void setChatBoxActive(boolean active) {
        //TODO
    }

    @Override
    public void clearChatBox() {
        //TODO
    }

    @Override
    protected boolean saveGame() {
        //TODO
        return true;
    }

    @Override
    public void die() {
        client.getGame().removeGameListener(gameListener);
        super.die();
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
        showReportPanel();
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
        clientGuiPanel.validate();
    }

    private void showReportPanel() {
        if (client.getGame().getPhase().isReport()) {

            miniReportDisplayDialog.add(reportPanel);
            miniReportDisplayDialog.pack();
            miniReportDisplayDialog.setVisible(true);

        }
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
                initializeWithBoardView(phase, new SBFMovementDisplay(this), CG_MOVEMENTDISPLAY);
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
                if (reportDisplay == null) {
                    reportDisplay = new SBFReportDisplay(this);
                    reportDisplay.setName(CG_REPORTDISPLAY);
                }
                initializeWithBoardView(phase, reportDisplay, CG_REPORTDISPLAY);
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

    protected void updateFormationSprites() {
        formationSpriteHandler.update();
    }

    public void selectForAction(@Nullable SBFFormation formation) {
        formationSpriteHandler.setSelectedFormation(formation);
    }

    /**
     * Shows the movement envelope in the BoardView for the given entity. The movement envelope data is
     * a map of move end Coords to movement points used.
     *
     * @param formation The entity for which the movement envelope is
     * @param mvEnvData The movement envelope data
     */
    public void showMovementEnvelope(SBFFormation formation, Map<Coords, Integer> mvEnvData) {
        movementEnvelopeHandler.setMovementEnvelope(mvEnvData, formation.getMovement(),
                formation.getMovement(), formation.getMovement(), MovementDisplay.GEAR_JUMP);
    }

    public void clearMovementEnvelope() {
        movementEnvelopeHandler.clear();
    }

    public void showMovePath(@Nullable SBFMovePath movePath) {
        movePathSpriteHandler.update(movePath);
    }
}
