/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import megamek.client.SBFClient;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.boardview.BoardView;
import megamek.client.ui.clientGUI.boardview.overlay.KeyBindingsOverlay;
import megamek.client.ui.clientGUI.boardview.overlay.PlanetaryConditionsOverlay;
import megamek.client.ui.clientGUI.boardview.spriteHandler.BoardViewSpriteHandler;
import megamek.client.ui.clientGUI.boardview.spriteHandler.MovePathSpriteHandler;
import megamek.client.ui.clientGUI.boardview.spriteHandler.MovementEnvelopeSpriteHandler;
import megamek.client.ui.clientGUI.boardview.spriteHandler.sbf.SBFFormationSpriteHandler;
import megamek.client.ui.clientGUI.boardview.toolTip.SBFBoardViewTooltip;
import megamek.client.ui.panels.ReceivingGameDataPanel;
import megamek.client.ui.panels.StartingScenarioPanel;
import megamek.client.ui.panels.UnderConstructionPanel;
import megamek.client.ui.panels.WaitingForServerPanel;
import megamek.client.ui.panels.phaseDisplay.MovementDisplay;
import megamek.client.ui.panels.phaseDisplay.SBFFiringDisplay;
import megamek.client.ui.panels.phaseDisplay.SBFMovementDisplay;
import megamek.client.ui.panels.phaseDisplay.SBFReportDisplay;
import megamek.client.ui.panels.phaseDisplay.StatusBarPhaseDisplay;
import megamek.client.ui.util.MegaMekController;
import megamek.client.ui.widget.SBFReportPanel;
import megamek.common.annotations.Nullable;
import megamek.common.board.Coords;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameListener;
import megamek.common.game.Game;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFMovePath;
import megamek.common.util.Distractable;
import megamek.logging.MMLogger;

public class SBFClientGUI extends AbstractClientGUI implements ActionListener {
    private static final MMLogger logger = MMLogger.create(SBFClientGUI.class);

    public static final String CG_BOARD_VIEW = "BoardView";
    public static final String CG_CHAT_LOUNGE = "ChatLounge";
    public static final String CG_STARTING_SCENARIO = "JLabel-StartingScenario";
    public static final String CG_EXCHANGE = "JLabel-Exchange";
    public static final String CG_SELECT_ARTY_AUTO_HIT_HEX_DISPLAY = "SelectArtyAutoHitHexDisplay";
    public static final String CG_DEPLOY_MINEFIELD_DISPLAY = "DeployMinefieldDisplay";
    public static final String CG_DEPLOYMENT_DISPLAY = "DeploymentDisplay";
    public static final String CG_TARGETING_PHASE_DISPLAY = "TargetingPhaseDisplay";
    public static final String CG_PREMOVEMENT_DISPLAY = "PremovementDisplay";
    public static final String CG_MOVEMENT_DISPLAY = "MovementDisplay";
    public static final String CG_OFFBOARD_DISPLAY = "OffboardDisplay";
    public static final String CG_PRE_FIRING = "PreFiring";
    public static final String CG_FIRING_DISPLAY = "FiringDisplay";
    public static final String CG_POINTBLANK_SHOT_DISPLAY = "PointblankShotDisplay";
    public static final String CG_PHYSICAL_DISPLAY = "PhysicalDisplay";
    public static final String CG_REPORT_DISPLAY = "ReportDisplay";
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

    private final JDialog miniReportDisplayDialog;
    private final SBFReportPanel reportPanel;

    /**
     * Map each phase to the name of the card for the main display area.
     */
    private final Map<String, String> mainNames = new HashMap<>();

    private final GameListener gameListener = new SBFClientGUIGameListener(this);
    protected final CommonMenuBar menuBar = CommonMenuBar.getMenuBarForGame();
    private BoardView bv;
    private SBFFormationSpriteHandler formationSpriteHandler;
    private MovementEnvelopeSpriteHandler movementEnvelopeHandler;
    private MovePathSpriteHandler movePathSpriteHandler;

    public SBFClientGUI(SBFClient client, MegaMekController megaMekController) {
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
        ComponentListener resizeListener = new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent evt) {
                boardViewsContainer.getPanel().setPreferredSize(clientGuiPanel.getSize());
            }
        };
        clientGuiPanel.addComponentListener(resizeListener);
        clientGuiPanel.add(panMain, BorderLayout.CENTER);
        clientGuiPanel.add(panSecondary, BorderLayout.SOUTH);

        miniReportDisplayDialog = new JDialog(getFrame());
        reportPanel = new SBFReportPanel(this);
    }

    /**
     * Initializes a number of things about this frame.
     */
    @Override
    protected void initializeFrame() {
        super.initializeFrame();
    }

    protected Game bvGame = new Game();

    @Override
    public void initialize() {
        initializeFrame();
        super.initialize();
        try {
            client.getGame().addGameListener(gameListener);
            bvGame.setBoard(bvGame.getBoard(0));

            bv = new BoardView(bvGame, MegaMekGUI.getKeyDispatcher(), null, 0);
            bv.setTooltipProvider(new SBFBoardViewTooltip(client.getGame(), bv));
            boardViews.put(0, bv);
            bv.addOverlay(new KeyBindingsOverlay(bv));
            bv.addOverlay(new PlanetaryConditionsOverlay(bv));
            bv.getPanel().setPreferredSize(clientGuiPanel.getSize());
            boardViewsContainer.setName(CG_BOARD_VIEW);
            boardViewsContainer.updateMapTabs();
            initializeSpriteHandlers();

            JPanel panA1 = new JPanel();
            panA1.setVisible(false);
            JPanel panA2 = new JPanel();
            panA2.setVisible(false);
            panA2.add(boardViewsContainer.getPanel());
            panA2.setVisible(true);

            JSplitPane splitPaneA = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

            splitPaneA.setDividerSize(10);
            splitPaneA.setResizeWeight(0.5);

            splitPaneA.setLeftComponent(panA1);
            splitPaneA.setRightComponent(panA2);

            panTop.add(splitPaneA, BorderLayout.CENTER);

        } catch (Exception ex) {
            logger.fatal(ex, "");
            die();
        }

        menuBar.addActionListener(this);
        client.changePhase(GamePhase.UNKNOWN);
        frame.setVisible(true);
    }

    private void initializeSpriteHandlers() {
        movementEnvelopeHandler = new MovementEnvelopeSpriteHandler(this, client.getGame());
        formationSpriteHandler = new SBFFormationSpriteHandler(this, client);
        movePathSpriteHandler = new MovePathSpriteHandler(this);
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
        // TODO
    }

    @Override
    public void clearChatBox() {
        // TODO
    }

    @Override
    protected boolean saveGame() {
        // TODO
        // client.setAwaitingSave(true) // necessary to avoid exit/save race condition
        return true;
    }

    @Override
    public void die() {
        // Will prevent race condition once saveGame() is implemented
        if (client.isAwaitingSave()) {
            SwingUtilities.invokeLater(this::die);
            return;
        }
        client.getGame().removeGameListener(gameListener);
        super.die();
    }

    protected void switchPanel(GamePhase phase) {
        // Clear the old panel's listeners.

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

        showReportPanel();

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

        if (curPanel instanceof ActionListener) {
            menuBar.addActionListener((ActionListener) curPanel);
        }

        if (curPanel instanceof Distractable) {
            ((Distractable) curPanel).setIgnoringEvents(false);
        }

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
        String identifier = CG_BOARD_VIEW;
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
        String main = CG_BOARD_VIEW;
        switch (phase) {
            case LOUNGE, SET_ARTILLERY_AUTO_HIT_HEXES, DEPLOY_MINEFIELDS, DEPLOYMENT, POINTBLANK_SHOT, PHYSICAL:
                break;
            case STARTING_SCENARIO:
                initializeSingleComponent(phase, new StartingScenarioPanel(), CG_STARTING_SCENARIO);
                break;
            case EXCHANGE:
                initializeSingleComponent(phase, new ReceivingGameDataPanel(), CG_EXCHANGE);
                break;
            case TARGETING:
                secondary = CG_TARGETING_PHASE_DISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case PREMOVEMENT:
                secondary = CG_PREMOVEMENT_DISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case MOVEMENT:
                initializeWithBoardView(phase, new SBFMovementDisplay(this), CG_MOVEMENT_DISPLAY);
                break;
            case OFFBOARD:
                secondary = CG_OFFBOARD_DISPLAY;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case PRE_FIRING:
                secondary = CG_PRE_FIRING;
                component.setName(secondary);
                if (!mainNames.containsValue(main)) {
                    panMain.add(panTop, main);
                }
                currPhaseDisplay = (StatusBarPhaseDisplay) component;
                panSecondary.add(component, secondary);
                break;
            case FIRING:
                initializeWithBoardView(phase, new SBFFiringDisplay(this), CG_FIRING_DISPLAY);
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
                    reportDisplay.setName(CG_REPORT_DISPLAY);
                }
                initializeWithBoardView(phase, reportDisplay, CG_REPORT_DISPLAY);
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
        if (GUIP.getAutoCenter() && (formation != null) && (formation.getPosition() != null)) {
            bv.centerOnHex(formation.getPosition().coords());
        }
    }

    /**
     * Shows the movement envelope in the BoardView for the given entity. The movement envelope data is a map of move
     * end Coords to movement points used.
     *
     * @param formation The entity for which the movement envelope is
     * @param mvEnvData The movement envelope data
     */
    public void showMovementEnvelope(SBFFormation formation, Map<Coords, Integer> mvEnvData) {
        movementEnvelopeHandler.setMovementEnvelope(mvEnvData, 0, formation.getMovement(),
              formation.getMovement(), formation.getMovement(), MovementDisplay.GEAR_JUMP);
    }

    public void clearMovementEnvelope() {
        movementEnvelopeHandler.clear();
    }

    public void showMovePath(@Nullable SBFMovePath movePath) {
        movePathSpriteHandler.update(movePath);
    }

}
