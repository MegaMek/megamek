/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels.phaseDisplay.lobby;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import jakarta.annotation.Nullable;
import megamek.client.ui.Messages;
import megamek.client.ui.panels.phaseDisplay.lobby.C3FormationPlanner.ConfigurationShape;
import megamek.client.ui.panels.phaseDisplay.lobby.C3FormationPlanner.ForcePlan;
import megamek.client.ui.panels.phaseDisplay.lobby.C3FormationPlanner.LeftOutLance;
import megamek.client.ui.panels.phaseDisplay.lobby.C3FormationPlanner.PlannedLance;
import megamek.client.ui.panels.phaseDisplay.lobby.C3FormationPlanner.TemplateAssignment;
import megamek.client.ui.util.UIUtil;
import megamek.codeUtilities.MathUtility;
import megamek.common.event.GameListenerAdapter;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.event.entity.GameEntityNewEvent;
import megamek.common.event.entity.GameEntityRemoveEvent;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.common.util.C3Util;
import megamek.logging.MMLogger;

/**
 * Lobby dialog for viewing and building C3 networks at scale. The left side lists the local player's unassigned
 * C3-equipped units; the right side shows every network on the team as a tree, drawn the way the rulebook's C3
 * Configuration Diagram draws them (CR p.199): company node on top, lance masters and direct slaves below, slaves of
 * lance masters one level deeper. Masterless systems (C3i, NC3, Nova CEWS) show as flat member lists.
 *
 * <p>Actions apply immediately through {@link LobbyActions} - the same path as the right-click menu - so the server
 * and all clients stay in sync, and this dialog refreshes itself from entity update events.</p>
 */
public class C3NetworkManagerDialog extends JDialog {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final MMLogger LOGGER = MMLogger.create(C3NetworkManagerDialog.class);

    private static final int PADDING = UIUtil.scaleForGUI(10);
    private static final int PADDING_SMALL = UIUtil.scaleForGUI(5);

    private final ChatLounge lobby;
    private final Game game;
    private final int localPlayerId;

    private final DefaultListModel<Object> rosterModel = new DefaultListModel<>();
    private final JList<Object> rosterList = new JList<>(rosterModel);
    private JButton formPeerNetworkButton;
    private JButton buildButton;
    private JButton discardPlanButton;
    private JButton autoAssignPlanButton;
    private JButton disconnectButton;
    private JToggleButton autoFormButton;
    private final JCheckBox autoAssignCheckBox =
          new JCheckBox(Messages.getString("C3NetworkManagerDialog.chkAutoAssign"), true);
    private final JLabel forcePlanLabel = new JLabel(" ");
    private final JToggleButton[] configStartButtons = new JToggleButton[4];

    /** The staged network plan cards, filled by the template buttons or Plan Force and applied on Build. */
    private final List<Blueprint> plans = new ArrayList<>();

    /** True while the plan cards came from Plan Force rather than a single configuration button. */
    private boolean plansFromForcePlan;

    /** True while any plan card holds the given unit. */
    private boolean planContainsUnit(int entityId) {
        for (Blueprint plan : plans) {
            if (plan.containsUnit(entityId)) {
                return true;
            }
        }
        return false;
    }

    /** Removes the unit from every plan card, so it can never occupy two slots across cards. */
    private void unassignFromPlans(int entityId) {
        for (Blueprint plan : plans) {
            plan.unassignUnit(entityId);
        }
    }

    /** Filled slots across all plan cards. */
    private int plansFilledCount() {
        int filled = 0;
        for (Blueprint plan : plans) {
            filled += plan.filledCount();
        }
        return filled;
    }

    /** The plan card a slot reference points into. */
    private Blueprint planAt(BlueprintSlotRef ref) {
        return plans.get(ref.planIndex());
    }
    private final DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode();
    private final DefaultTreeModel treeModel = new DefaultTreeModel(treeRoot);
    private final JTree networkTree = new JTree(treeModel);
    private final JLabel statusLabel = new JLabel(" ");

    private final transient GameListenerAdapter gameListener = new GameListenerAdapter() {
        @Override
        public void gameEntityNew(GameEntityNewEvent event) {
            refreshLater();
        }

        @Override
        public void gameEntityRemove(GameEntityRemoveEvent event) {
            refreshLater();
        }

        @Override
        public void gameEntityChange(GameEntityChangeEvent event) {
            refreshLater();
        }
    };

    /** Units to select when the dialog first opens (right-click flow); emptied after the first refresh. */
    private final Set<Integer> initialSelectionIds = new HashSet<>();

    /** Masters the player dragged onto the network side to start a network; shown as seeds until connected. */
    private final Set<Integer> seededMasterIds = new HashSet<>();

    public C3NetworkManagerDialog(ChatLounge lobby) {
        this(lobby, List.of());
    }

    /**
     * Opens the manager with the given units pre-selected, so the right-click flow ("select units, open the
     * manager") continues where the player left off: unassigned units are selected in the roster, networked units
     * in the network tree.
     */
    public C3NetworkManagerDialog(ChatLounge lobby, Collection<Entity> preselectedUnits) {
        super(lobby.getClientGUI().getFrame(), Messages.getString("C3NetworkManagerDialog.title"), false);
        this.lobby = lobby;
        this.game = lobby.game();
        this.localPlayerId = lobby.getClientGUI().getClient().getLocalPlayer().getId();
        for (Entity unit : preselectedUnits) {
            initialSelectionIds.add(unit.getId());
        }

        initializeUI();
        refresh();

        game.addGameListener(gameListener);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                game.removeGameListener(gameListener);
            }
        });

        setMinimumSize(UIUtil.scaleForGUI(750, 500));
        pack();
        setLocationRelativeTo(lobby.getClientGUI().getFrame());
    }

    private void initializeUI() {
        setLayout(new BorderLayout(PADDING, PADDING));

        // Left: unassigned units of the local player
        JPanel rosterPanel = new JPanel(new BorderLayout(PADDING_SMALL, PADDING_SMALL));
        rosterPanel.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("C3NetworkManagerDialog.unassignedUnits")));
        rosterList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        rosterList.setCellRenderer(new RosterRenderer());
        rosterList.setVisibleRowCount(18);
        rosterList.setDragEnabled(true);
        rosterList.setDropMode(DropMode.ON_OR_INSERT);
        rosterList.setTransferHandler(new UnitTransferHandler(false));
        JScrollPane rosterScroll = new JScrollPane(rosterList);
        rosterScroll.setPreferredSize(UIUtil.scaleForGUI(280, 420));
        rosterPanel.add(rosterScroll, BorderLayout.CENTER);

        // Right: the networks as trees, headed by the configuration template picker
        JPanel networksPanel = new JPanel(new BorderLayout(PADDING_SMALL, PADDING_SMALL));
        networksPanel.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("C3NetworkManagerDialog.networks")));

        JPanel configPickerPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, PADDING_SMALL, 0));
        configPickerPanel.add(new JLabel(Messages.getString("C3NetworkManagerDialog.startFromConfig")));
        for (int configNumber = 1; configNumber <= 4; configNumber++) {
            JToggleButton configButton = new JToggleButton(String.valueOf(configNumber));
            configButton.setToolTipText(Messages.getString("C3NetworkManagerDialog.configStart.tooltip",
                  configNumber));
            configButton.setPreferredSize(UIUtil.scaleForGUI(44, 28));
            final int chosenConfiguration = configNumber;
            configButton.addActionListener(event -> startBlueprint(chosenConfiguration));
            configStartButtons[configNumber - 1] = configButton;
            configPickerPanel.add(configButton);
        }
        autoFormButton = new JToggleButton(Messages.getString("C3NetworkManagerDialog.btnAutoForm"));
        autoFormButton.setToolTipText(Messages.getString("C3NetworkManagerDialog.btnAutoForm.tooltip"));
        autoFormButton.addActionListener(event -> createForcePlanTemplates());
        configPickerPanel.add(autoFormButton);
        autoAssignCheckBox.setToolTipText(Messages.getString("C3NetworkManagerDialog.chkAutoAssign.tooltip"));
        configPickerPanel.add(autoAssignCheckBox);

        // Above the tree: what the whole force can field, so multiple networks read as the expected outcome
        JPanel networksHeaderPanel = new JPanel();
        networksHeaderPanel.setLayout(new BoxLayout(networksHeaderPanel, BoxLayout.PAGE_AXIS));
        networksHeaderPanel.add(configPickerPanel);
        JPanel forcePlanRow = new JPanel(new FlowLayout(FlowLayout.LEADING, PADDING_SMALL, 0));
        forcePlanRow.add(forcePlanLabel);
        networksHeaderPanel.add(forcePlanRow);
        networksPanel.add(networksHeaderPanel, BorderLayout.NORTH);
        networkTree.setRootVisible(false);
        networkTree.setShowsRootHandles(true);
        networkTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        networkTree.setCellRenderer(new NetworkTreeRenderer());
        networkTree.setDragEnabled(true);
        networkTree.setDropMode(DropMode.ON);
        networkTree.setTransferHandler(new UnitTransferHandler(true));
        JScrollPane treeScroll = new JScrollPane(networkTree);
        treeScroll.setPreferredSize(UIUtil.scaleForGUI(430, 420));
        networksPanel.add(treeScroll, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, rosterPanel, networksPanel);
        splitPane.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, 0, PADDING));
        add(splitPane, BorderLayout.CENTER);

        // Bottom: actions and status
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.PAGE_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, PADDING, PADDING_SMALL, PADDING));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, PADDING_SMALL, PADDING_SMALL));

        formPeerNetworkButton = new JButton(Messages.getString("C3NetworkManagerDialog.btnFormPeerNetwork"));
        formPeerNetworkButton.setToolTipText(
              Messages.getString("C3NetworkManagerDialog.btnFormPeerNetwork.tooltip"));
        formPeerNetworkButton.addActionListener(event -> formPeerNetwork());

        disconnectButton = new JButton(Messages.getString("C3NetworkManagerDialog.btnDisconnect"));
        disconnectButton.setToolTipText(Messages.getString("C3NetworkManagerDialog.btnDisconnect.tooltip"));
        disconnectButton.addActionListener(event -> disconnectSelection());

        JButton resetButton = new JButton(Messages.getString("C3NetworkManagerDialog.btnReset"));
        resetButton.setToolTipText(Messages.getString("C3NetworkManagerDialog.btnReset.tooltip"));
        resetButton.addActionListener(event -> resetNetworks());

        buildButton = new JButton(Messages.getString("C3NetworkManagerDialog.btnBuild"));
        buildButton.setToolTipText(Messages.getString("C3NetworkManagerDialog.btnBuild.tooltip"));
        buildButton.addActionListener(event -> buildNetworkFromBlueprint());

        autoAssignPlanButton = new JButton(Messages.getString("C3NetworkManagerDialog.btnAutoAssignPlan"));
        autoAssignPlanButton.setToolTipText(Messages.getString("C3NetworkManagerDialog.btnAutoAssignPlan.tooltip"));
        autoAssignPlanButton.addActionListener(event -> autoAssignEmptyPlans());

        // Everything still in planning goes: the plan cards and masters dragged onto the panel. Built networks stay.
        discardPlanButton = new JButton(Messages.getString("C3NetworkManagerDialog.btnDiscardPlan"));
        discardPlanButton.setToolTipText(Messages.getString("C3NetworkManagerDialog.btnDiscardPlan.tooltip"));
        discardPlanButton.addActionListener(event -> {
            plans.clear();
            seededMasterIds.clear();
            refresh();
        });

        JButton closeButton = new JButton(Messages.getString("Close"));
        closeButton.addActionListener(event -> {
            game.removeGameListener(gameListener);
            dispose();
        });

        buttonPanel.add(formPeerNetworkButton);
        buttonPanel.add(buildButton);
        buttonPanel.add(autoAssignPlanButton);
        buttonPanel.add(discardPlanButton);
        buttonPanel.add(disconnectButton);
        buttonPanel.add(resetButton);
        buttonPanel.add(closeButton);

        bottomPanel.add(buttonPanel);
        bottomPanel.add(statusLabel);
        add(bottomPanel, BorderLayout.PAGE_END);
    }

    /** Rebuilds roster and tree on the EDT after a game change arrives. */
    private void refreshLater() {
        SwingUtilities.invokeLater(this::refresh);
    }

    /** A non-selectable section header row in the roster, one per C3 system present. */
    private record RosterHeader(String labelKey) {
    }

    /** Appends a section header and its units to the roster; empty groups are skipped entirely. */
    private void addRosterGroup(String labelKey, List<Entity> units) {
        if (units.isEmpty()) {
            return;
        }
        rosterModel.addElement(new RosterHeader(labelKey));
        for (Entity unit : units) {
            rosterModel.addElement(unit);
        }
    }

    /** The units selected in the roster, skipping section headers. */
    private List<Entity> selectedRosterEntities() {
        List<Entity> selected = new ArrayList<>();
        for (Object value : rosterList.getSelectedValuesList()) {
            if (value instanceof Entity entity) {
                selected.add(entity);
            }
        }
        return selected;
    }

    /** Rebuilds the roster of unassigned units and the network tree from the current game state. */
    private void refresh() {
        List<Entity> selectedRoster = selectedRosterEntities();

        // A master dragged onto the network side becomes a new-network seed and leaves the roster; everything
        // else stays on the left until connected, grouped by C3 system. Designated company masters seed
        // automatically.
        rosterModel.clear();
        List<Entity> seedMasters = new ArrayList<>();
        List<Entity> masterUnits = new ArrayList<>();
        List<Entity> slaveUnits = new ArrayList<>();
        List<Entity> c3iUnits = new ArrayList<>();
        List<Entity> navalC3Units = new ArrayList<>();
        List<Entity> novaUnits = new ArrayList<>();
        for (Entity entity : sortedById(game.inGameTWEntities())) {
            if ((entity.getOwnerId() != localPlayerId) || !entity.hasAnyC3System() || isNetworked(entity)) {
                continue;
            }
            if (entity.getOperableC3MCount() > 0) {
                if (seededMasterIds.contains(entity.getId()) || entity.isC3CompanyCommander()) {
                    seedMasters.add(entity);
                } else {
                    masterUnits.add(entity);
                }
            } else if (entity.hasC3i()) {
                c3iUnits.add(entity);
            } else if (entity.hasNavalC3()) {
                navalC3Units.add(entity);
            } else if (entity.hasNovaCEWS()) {
                novaUnits.add(entity);
            } else {
                slaveUnits.add(entity);
            }
        }
        addRosterGroup("C3NetworkManagerDialog.groupMasters", masterUnits);
        addRosterGroup("C3NetworkManagerDialog.groupSlaves", slaveUnits);
        addRosterGroup("C3NetworkManagerDialog.groupC3i", c3iUnits);
        addRosterGroup("C3NetworkManagerDialog.groupNC3", navalC3Units);
        addRosterGroup("C3NetworkManagerDialog.groupNova", novaUnits);
        restoreRosterSelection(selectedRoster);

        // Peer networks only exist for these systems - hide the button when none are on the field
        formPeerNetworkButton.setVisible(!c3iUnits.isEmpty() || !navalC3Units.isEmpty() || !novaUnits.isEmpty());

        treeRoot.removeAllChildren();
        if (!plans.isEmpty()) {
            pruneStaleBlueprintAssignments();
            for (int planIndex = 0; planIndex < plans.size(); planIndex++) {
                treeRoot.add(buildBlueprintNode(planIndex));
            }
        }
        Map<String, List<Entity>> builtNetworks = collectNetworks();
        for (List<Entity> members : builtNetworks.values()) {
            treeRoot.add(buildNetworkNode(members));
        }
        for (Entity seedMaster : seedMasters) {
            treeRoot.add(buildNetworkNode(List.of(seedMaster)));
        }
        for (PeerSystem system : PeerSystem.values()) {
            int unassignedPeers = 0;
            for (int index = 0; index < rosterModel.size(); index++) {
                if ((rosterModel.get(index) instanceof Entity entity) && system.matches(entity)) {
                    unassignedPeers++;
                }
            }
            if (unassignedPeers >= 2) {
                treeRoot.add(new DefaultMutableTreeNode(new PeerSeed(system)));
            }
        }
        if (treeRoot.getChildCount() == 0) {
            treeRoot.add(new DefaultMutableTreeNode(new StartHint()));
        }
        updateConfigurationTooltips(masterUnits, seedMasters, slaveUnits);
        updateForcePlanSummary();
        buildButton.setVisible(!plans.isEmpty());
        discardPlanButton.setVisible(!plans.isEmpty() || !seedMasters.isEmpty());
        // Auto-assign only offers itself while the plan is laid out but holds no units yet
        autoAssignPlanButton.setVisible(!plans.isEmpty() && (plansFilledCount() == 0));
        updatePlanButtonHighlight();
        // Disconnect acts only on built networks - plan cards and seeds are cleared with Discard Plan and Reset All
        disconnectButton.setVisible(!builtNetworks.isEmpty());
        treeModel.reload();
        for (int row = 0; row < networkTree.getRowCount(); row++) {
            networkTree.expandRow(row);
        }

        if (!initialSelectionIds.isEmpty()) {
            applyInitialSelection();
            initialSelectionIds.clear();
        }

        int unassignedUnitCount = 0;
        for (int index = 0; index < rosterModel.size(); index++) {
            if (rosterModel.get(index) instanceof Entity) {
                unassignedUnitCount++;
            }
        }
        // Count networks, not tree rows: the start hint, plan cards and peer drop rows are not networks
        int networkCount = builtNetworks.size() + seedMasters.size();
        statusLabel.setText(Messages.getString("C3NetworkManagerDialog.status",
              unassignedUnitCount, networkCount));
    }

    /** Selects the units handed over by the right-click flow: roster rows and network tree nodes alike. */
    private void applyInitialSelection() {
        List<Integer> rosterIndices = new ArrayList<>();
        for (int index = 0; index < rosterModel.size(); index++) {
            if ((rosterModel.get(index) instanceof Entity rowEntity)
                  && initialSelectionIds.contains(rowEntity.getId())) {
                rosterIndices.add(index);
            }
        }
        int[] rosterIndexArray = new int[rosterIndices.size()];
        for (int position = 0; position < rosterIndices.size(); position++) {
            rosterIndexArray[position] = rosterIndices.get(position);
        }
        rosterList.setSelectedIndices(rosterIndexArray);

        List<TreePath> treePaths = new ArrayList<>();
        for (int row = 0; row < networkTree.getRowCount(); row++) {
            TreePath path = networkTree.getPathForRow(row);
            Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if ((userObject instanceof MemberRow memberRow)
                  && initialSelectionIds.contains(memberRow.entity().getId())) {
                treePaths.add(path);
            }
        }
        if (!treePaths.isEmpty()) {
            networkTree.setSelectionPaths(treePaths.toArray(new TreePath[0]));
        }
    }

    /** True when the unit shares its network with at least one other unit. */
    private boolean isNetworked(Entity entity) {
        return game.getC3NetworkMembers(entity).size() > 1;
    }

    /** All networks (2+ members) among the local player's team, keyed by net id, members sorted by id. */
    private Map<String, List<Entity>> collectNetworks() {
        Map<String, List<Entity>> networks = new LinkedHashMap<>();
        for (Entity entity : sortedById(game.inGameTWEntities())) {
            String netId = entity.getC3NetId();
            if ((netId == null) || !entity.hasAnyC3System()
                  || entity.getOwner().isEnemyOf(game.getPlayer(localPlayerId))) {
                continue;
            }
            networks.computeIfAbsent(netId, unused -> new ArrayList<>()).add(entity);
        }
        networks.values().removeIf(members -> members.size() < 2);
        return networks;
    }

    /** The company node's color, matching the orange center of the rulebook's configuration diagram (CR p.199). */
    private static final String COMPANY_COLOR = "#E8912D";

    /** One color per lance; slaves inherit their lance master's color so a lance reads as one block. */
    private static final String[] LANCE_COLORS = { "#D9534F", "#3F8FD2", "#4CAF7D", "#B15FC2" };

    /** The company's own lance, matching the yellow lance beside the orange center of the diagram (CR p.199). */
    private static final String COMPANY_LANCE_COLOR = "#E8C22D";

    /** Color for masterless peer networks (C3i, NC3, Nova CEWS). */
    private static final String PEER_COLOR = "#C0704D";

    private static final String MUTED_COLOR = "#8A8A8A";

    /**
     * Builds the tree node for one network: hierarchical C3 as a master tree, peer systems as a flat list. The
     * layout follows the rulebook's configuration diagram: a master's own slaves come first, then its free slave
     * slots, then its subordinate lances as nested blocks, company master slots last. Each lance carries its own
     * color; masters with free links get drop-target placeholder rows.
     */
    private DefaultMutableTreeNode buildNetworkNode(List<Entity> members) {
        DefaultMutableTreeNode networkNode = new DefaultMutableTreeNode(new NetworkGroup(members));

        Entity first = members.get(0);
        if (first.hasNhC3()) {
            for (Entity member : members) {
                // Peer systems have their own size caps (6 or 3) already reflected in calculateFreeC3Nodes
                networkNode.add(new DefaultMutableTreeNode(
                      new MemberRow(member, "C3NetworkManagerDialog.rolePeer", PEER_COLOR, Integer.MAX_VALUE)));
            }
            int freePeerSlots = first.calculateFreeC3Nodes();
            if (freePeerSlots > 0) {
                networkNode.add(new DefaultMutableTreeNode(
                      new SlotPlaceholder(first.getId(), false, freePeerSlots, PEER_COLOR)));
            }
            return networkNode;
        }

        // Hierarchical: group members under their master; roots are members whose master is not listed
        Map<Integer, List<Entity>> childrenByParent = new LinkedHashMap<>();
        List<Entity> roots = new ArrayList<>();
        for (Entity member : members) {
            Entity master = member.getC3Master();
            boolean hasListedParent = false;
            if ((master != null) && (master.getId() != member.getId())) {
                for (Entity other : members) {
                    if (other.getId() == master.getId()) {
                        hasListedParent = true;
                        break;
                    }
                }
            }
            if (hasListedParent) {
                childrenByParent.computeIfAbsent(master.getId(), unused -> new ArrayList<>()).add(member);
            } else {
                roots.add(member);
            }
        }
        // Free counts everywhere are capped by the 12-unit network limit (CR p.198): a master's mounts may have
        // links left while the network as a whole is full, and showing mount capacity alone promises room that a
        // connect would then reject.
        int networkRemaining = Math.max(0, Entity.MAX_C3_NODES - members.size());
        int[] lanceColorCounter = { 0 };
        for (Entity root : roots) {
            String rootColor = root.isC3CompanyCommander() ? COMPANY_COLOR
                  : LANCE_COLORS[lanceColorCounter[0]++ % LANCE_COLORS.length];
            networkNode.add(buildMemberSubtree(root, childrenByParent, rootColor, lanceColorCounter,
                  networkRemaining));
        }
        return networkNode;
    }

    /**
     * Builds a company master's subtree, colored like its template card and the rulebook's configuration diagram
     * (CR p.199): the slaves the head runs itself in blocks of three, one lance color per block, then its free
     * slave slots, then its lance masters' lances, then its free company slots.
     */
    private DefaultMutableTreeNode buildCompanySubtree(Entity company, Map<Integer, List<Entity>> childrenByParent,
          int[] lanceColorCounter, int networkRemaining) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(
              new MemberRow(company, "C3NetworkManagerDialog.roleCompanyMaster", COMPANY_COLOR, networkRemaining));
        List<Entity> headSlaves = new ArrayList<>();
        List<Entity> lanceMasters = new ArrayList<>();
        for (Entity child : childrenByParent.getOrDefault(company.getId(), List.of())) {
            if (child.getOperableC3MCount() > 0) {
                lanceMasters.add(child);
            } else {
                headSlaves.add(child);
            }
        }
        for (int slaveIndex = 0; slaveIndex < headSlaves.size(); slaveIndex++) {
            String blockColor = headLanceColor(slaveIndex / Entity.MAX_C3M_SUBORDINATES, lanceMasters.size());
            node.add(buildMemberSubtree(headSlaves.get(slaveIndex), childrenByParent, blockColor, lanceColorCounter,
                  networkRemaining));
        }
        int freeSlaveSlots = Math.min(company.calculateFreeC3Nodes(), networkRemaining);
        if (freeSlaveSlots > 0) {
            String nextBlockColor = headLanceColor(headSlaves.size() / Entity.MAX_C3M_SUBORDINATES,
                  lanceMasters.size());
            node.add(new DefaultMutableTreeNode(
                  new SlotPlaceholder(company.getId(), false, freeSlaveSlots, nextBlockColor)));
        }
        // Configuration 1's company lance goes first, next to the head as in its template card
        Entity configuration1CompanyLanceMaster = findConfiguration1CompanyLanceMaster(headSlaves, lanceMasters,
              childrenByParent);
        if (configuration1CompanyLanceMaster != null) {
            node.add(buildMemberSubtree(configuration1CompanyLanceMaster, childrenByParent, COMPANY_LANCE_COLOR,
                  lanceColorCounter, networkRemaining));
        }
        int otherLanceCount = 0;
        for (Entity lanceMaster : lanceMasters) {
            if (lanceMaster == configuration1CompanyLanceMaster) {
                continue;
            }
            String lanceColor = LANCE_COLORS[otherLanceCount++ % LANCE_COLORS.length];
            node.add(buildMemberSubtree(lanceMaster, childrenByParent, lanceColor, lanceColorCounter,
                  networkRemaining));
        }
        int freeMasterSlots = Math.min(company.calculateFreeC3MNodes(), networkRemaining);
        if (freeMasterSlots > 0) {
            node.add(new DefaultMutableTreeNode(
                  new SlotPlaceholder(company.getId(), true, freeMasterSlots, COMPANY_COLOR)));
        }
        return node;
    }

    /**
     * The lance master whose lance is the company's own in a Configuration 1 network: with no slaves under the head
     * and all three lance masters present, the diagram's short (two-slave) lance - the one with the fewest slaves,
     * the last of them on a tie. {@code null} for any other shape, where the company's own lance is the head's
     * slaves or cannot be told apart yet.
     */
    @Nullable
    private Entity findConfiguration1CompanyLanceMaster(List<Entity> headSlaves, List<Entity> lanceMasters,
          Map<Integer, List<Entity>> childrenByParent) {
        if (!headSlaves.isEmpty() || (lanceMasters.size() != Entity.MAX_C3M_SUBORDINATES)) {
            return null;
        }
        Entity shortestLanceMaster = null;
        int fewestSlaves = Integer.MAX_VALUE;
        for (Entity lanceMaster : lanceMasters) {
            int slaveCount = childrenByParent.getOrDefault(lanceMaster.getId(), List.of()).size();
            if (slaveCount <= fewestSlaves) {
                shortestLanceMaster = lanceMaster;
                fewestSlaves = slaveCount;
            }
        }
        return shortestLanceMaster;
    }

    /**
     * The color of one block of three slaves a company head runs itself: the first block is the company's own lance
     * (yellow in the diagram), later blocks are lances of their own and take the lance colors after those of the
     * head's lance masters - so Configuration 3 reads red lance master, blue head lance, and Configuration 4 yellow,
     * red, blue.
     */
    private static String headLanceColor(int headLanceIndex, int lanceMasterCount) {
        if (headLanceIndex == 0) {
            return COMPANY_LANCE_COLOR;
        }
        return LANCE_COLORS[(lanceMasterCount + headLanceIndex - 1) % LANCE_COLORS.length];
    }

    /** Builds one member's subtree: slaves, free slave slots, subordinate lances. Company masters go elsewhere. */
    private DefaultMutableTreeNode buildMemberSubtree(Entity entity, Map<Integer, List<Entity>> childrenByParent,
          String color, int[] lanceColorCounter, int networkRemaining) {
        if (entity.isC3CompanyCommander()) {
            return buildCompanySubtree(entity, childrenByParent, lanceColorCounter, networkRemaining);
        }
        boolean isMasterUnit = entity.getOperableC3MCount() > 0;
        String roleKey;
        if (isMasterUnit) {
            roleKey = "C3NetworkManagerDialog.roleLanceMaster";
        } else {
            roleKey = "C3NetworkManagerDialog.roleSlave";
        }
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(
              new MemberRow(entity, roleKey, color, networkRemaining));

        List<Entity> children = childrenByParent.getOrDefault(entity.getId(), List.of());
        for (Entity child : children) {
            if (child.getOperableC3MCount() == 0) {
                // A slave inherits its lance's color
                node.add(buildMemberSubtree(child, childrenByParent, color, lanceColorCounter, networkRemaining));
            }
        }
        if (isMasterUnit) {
            int freeSlaveSlots = Math.min(entity.calculateFreeC3Nodes(), networkRemaining);
            if (freeSlaveSlots > 0) {
                node.add(new DefaultMutableTreeNode(
                      new SlotPlaceholder(entity.getId(), false, freeSlaveSlots, color)));
            }
        }
        for (Entity child : children) {
            if (child.getOperableC3MCount() > 0) {
                String lanceColor = LANCE_COLORS[lanceColorCounter[0]++ % LANCE_COLORS.length];
                node.add(buildMemberSubtree(child, childrenByParent, lanceColor, lanceColorCounter,
                      networkRemaining));
            }
        }
        return node;
    }

    /** One unit's row in the network tree: its computed role, its lance's color and the network's remaining room. */
    private record MemberRow(Entity entity, String roleKey, String colorHex, int networkRemaining) {
    }

    /** A drop-target row under a master with free links: says what fits and connects drops to that master. */
    private record SlotPlaceholder(int targetId, boolean masterSlots, int freeCount, String colorHex) {
    }

    /** Masterless C3 systems; a seed row per system lets players start a network by dropping members on it. */
    private enum PeerSystem {
        C3I("C3i") {
            @Override
            boolean matches(Entity entity) {
                return entity.hasC3i();
            }
        },
        NC3("Naval C3") {
            @Override
            boolean matches(Entity entity) {
                return entity.hasNavalC3();
            }
        },
        NOVA("Nova CEWS") {
            @Override
            boolean matches(Entity entity) {
                return entity.hasNovaCEWS();
            }
        };

        private final String displayName;

        PeerSystem(String displayName) {
            this.displayName = displayName;
        }

        String displayName() {
            return displayName;
        }

        abstract boolean matches(Entity entity);
    }

    /** The "New C3i/NC3/Nova network" drop row shown while two or more matching units are unassigned. */
    private record PeerSeed(PeerSystem system) {
    }

    /** The kinds of slot a configuration template offers. */
    private enum SlotKind { COMPANY, DIRECT_SLAVE, LANCE_MASTER, LANCE_SLAVE }

    /** One slot of a plan card; lanceIndex/slotIndex are -1 where not applicable. */
    private record BlueprintSlotRef(int planIndex, SlotKind kind, int lanceIndex, int slotIndex) {
    }

    /** The header row of one plan card. */
    private record BlueprintHeader(int planIndex) {
    }

    /**
     * A fillable plan of one of the four book configurations (CR p.199). Filling slots changes nothing in the
     * game - the plan is applied in dependency order (company, then lance masters, then slaves) only on Build.
     */
    private static final class Blueprint {
        private final int configurationNumber;
        private Integer companyId;
        private final Integer[] directSlaves;
        private final Integer[] lanceMasters;
        private final Integer[][] lanceSlaves;

        private Blueprint(int configurationNumber, int directSlaveCount, int[] lanceSlaveCounts) {
            this.configurationNumber = configurationNumber;
            this.directSlaves = new Integer[directSlaveCount];
            this.lanceMasters = new Integer[lanceSlaveCounts.length];
            this.lanceSlaves = new Integer[lanceSlaveCounts.length][];
            for (int lance = 0; lance < lanceSlaveCounts.length; lance++) {
                this.lanceSlaves[lance] = new Integer[lanceSlaveCounts[lance]];
            }
        }

        /** The canonical shapes: 12, 12, 11 and 10 units (CR p.199 C3 Configuration Diagram). */
        static Blueprint forConfiguration(int configurationNumber) {
            ConfigurationShape shape = ConfigurationShape.of(configurationNumber);
            return new Blueprint(shape.configurationNumber(), shape.directSlaveCount(), shape.lanceSlaveCounts());
        }

        /** A remainder lance: one master (in the company slot) with up to three slaves. */
        static Blueprint slaveLance() {
            return new Blueprint(0, Entity.MAX_C3M_SUBORDINATES, new int[0]);
        }

        /** An all-master lance: one master heading up to three masters in slave roles (CR p.199). */
        static Blueprint allMasterLance() {
            return new Blueprint(-1, 0, new int[] { 0, 0, 0 });
        }

        /** True for the four company configurations; false for the remainder lance cards. */
        boolean isCompanyConfiguration() {
            return configurationNumber >= 1;
        }

        int totalSlots() {
            int total = 1 + directSlaves.length + lanceMasters.length;
            for (Integer[] lance : lanceSlaves) {
                total += lance.length;
            }
            return total;
        }

        int filledCount() {
            int filled = (companyId == null) ? 0 : 1;
            for (Integer slot : directSlaves) {
                filled += (slot == null) ? 0 : 1;
            }
            for (Integer slot : lanceMasters) {
                filled += (slot == null) ? 0 : 1;
            }
            for (Integer[] lance : lanceSlaves) {
                for (Integer slot : lance) {
                    filled += (slot == null) ? 0 : 1;
                }
            }
            return filled;
        }

        boolean containsUnit(int entityId) {
            if ((companyId != null) && (companyId == entityId)) {
                return true;
            }
            for (Integer slot : directSlaves) {
                if ((slot != null) && (slot == entityId)) {
                    return true;
                }
            }
            for (Integer slot : lanceMasters) {
                if ((slot != null) && (slot == entityId)) {
                    return true;
                }
            }
            for (Integer[] lance : lanceSlaves) {
                for (Integer slot : lance) {
                    if ((slot != null) && (slot == entityId)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /** Empties every slot, keeping the card's layout. */
        void clearSlots() {
            companyId = null;
            Arrays.fill(directSlaves, null);
            Arrays.fill(lanceMasters, null);
            for (Integer[] lance : lanceSlaves) {
                Arrays.fill(lance, null);
            }
        }

        /** Removes the unit from whatever slot holds it, so a unit can never occupy two slots. */
        void unassignUnit(int entityId) {
            if ((companyId != null) && (companyId == entityId)) {
                companyId = null;
            }
            for (int index = 0; index < directSlaves.length; index++) {
                if ((directSlaves[index] != null) && (directSlaves[index] == entityId)) {
                    directSlaves[index] = null;
                }
            }
            for (int index = 0; index < lanceMasters.length; index++) {
                if ((lanceMasters[index] != null) && (lanceMasters[index] == entityId)) {
                    lanceMasters[index] = null;
                }
            }
            for (Integer[] lance : lanceSlaves) {
                for (int index = 0; index < lance.length; index++) {
                    if ((lance[index] != null) && (lance[index] == entityId)) {
                        lance[index] = null;
                    }
                }
            }
        }

        /** The entity id assigned to the slot, or {@code null} while it is empty. */
        @Nullable
        Integer slotValue(BlueprintSlotRef ref) {
            return switch (ref.kind()) {
                case COMPANY -> companyId;
                case DIRECT_SLAVE -> directSlaves[ref.slotIndex()];
                case LANCE_MASTER -> lanceMasters[ref.lanceIndex()];
                case LANCE_SLAVE -> lanceSlaves[ref.lanceIndex()][ref.slotIndex()];
            };
        }

        void setSlotValue(BlueprintSlotRef ref, Integer entityId) {
            switch (ref.kind()) {
                case COMPANY -> companyId = entityId;
                case DIRECT_SLAVE -> directSlaves[ref.slotIndex()] = entityId;
                case LANCE_MASTER -> lanceMasters[ref.lanceIndex()] = entityId;
                case LANCE_SLAVE -> lanceSlaves[ref.lanceIndex()][ref.slotIndex()] = entityId;
            }
        }
    }

    /** The hint row shown while the network side is empty: drag a master here to start. */
    private record StartHint() {
    }

    /** Lance masters each configuration needs beyond the company unit (CR p.199): Configs 1-4. */
    private static final int[] LANCE_MASTERS_NEEDED = { 3, 2, 1, 0 };

    /** Slaves each configuration needs (CR p.199): 8 for Config 1 (3+3+2), 9 for Configs 2-4. */
    private static final int[] SLAVES_NEEDED = { 8, 9, 9, 9 };

    /**
     * Reports on each template button how far the unassigned force can fill that configuration: no unit to head
     * it, short of lance masters or slaves, or every slot fillable. The buttons themselves stay available - a
     * template is a layout the player fills, and Build skips the slots left empty.
     */
    private void updateConfigurationTooltips(List<Entity> masterUnits, List<Entity> seedMasters,
          List<Entity> slaveUnits) {
        List<Entity> allMasters = new ArrayList<>(masterUnits);
        allMasters.addAll(seedMasters);
        List<String> buttonStates = new ArrayList<>();
        for (int configNumber = 1; configNumber <= 4; configNumber++) {
            JToggleButton configButton = configStartButtons[configNumber - 1];
            // Extra computers idle, so any unit with at least N computers can head Configuration N
            int companyCandidates = 0;
            for (Entity master : allMasters) {
                if (master.getOperableC3MCount() >= configNumber) {
                    companyCandidates++;
                }
            }
            if (companyCandidates == 0) {
                configButton.setToolTipText(
                      Messages.getString("C3NetworkManagerDialog.configNoCompany", configNumber));
                buttonStates.add(configNumber + "=no company unit");
                continue;
            }
            // Units, not computers: every master slot needs its own unit (the company unit fills none of the
            // lance master slots), and every slave slot needs a slave unit
            int mastersShort = Math.max(0,
                  LANCE_MASTERS_NEEDED[configNumber - 1] - (allMasters.size() - 1));
            int slavesShort = Math.max(0, SLAVES_NEEDED[configNumber - 1] - slaveUnits.size());
            boolean fullyBuildable = (mastersShort == 0) && (slavesShort == 0);
            buttonStates.add(configNumber + (fullyBuildable ? "=ready"
                  : ("=short " + mastersShort + " masters/" + slavesShort + " slaves")));
            if (fullyBuildable) {
                configButton.setToolTipText(Messages.getString("C3NetworkManagerDialog.configReady",
                      configNumber, 1 + LANCE_MASTERS_NEEDED[configNumber - 1]
                            + SLAVES_NEEDED[configNumber - 1]));
            } else {
                configButton.setToolTipText(Messages.getString("C3NetworkManagerDialog.configShort",
                      configNumber, mastersShort, slavesShort));
            }
        }
        LOGGER.debug("[C3Manager] Configuration templates for {} masters, {} slaves: {}", allMasters.size(),
              slaveUnits.size(), buttonStates);
    }

    /**
     * Lays out the whole force plan as staged template cards - one per network - without building anything.
     * With auto-assign the slots come pre-filled with the planned units, lobby lances kept whole; without, they
     * arrive empty and the player drags units in. Build Network applies the cards.
     */
    private void createForcePlanTemplates() {
        createForcePlanTemplates(autoAssignCheckBox.isSelected());
    }

    private void createForcePlanTemplates(boolean autoAssign) {
        ForcePlan plan = newFormationPlanner().planForce();
        if (plan.isEmpty()) {
            updatePlanButtonHighlight();
            return;
        }
        if (plansFilledCount() > 0) {
            int result = JOptionPane.showConfirmDialog(this,
                  Messages.getString("C3NetworkManagerDialog.replaceForcePlan"),
                  Messages.getString("C3NetworkManagerDialog.title"),
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.YES_OPTION) {
                updatePlanButtonHighlight();
                return;
            }
        }
        plans.clear();
        plansFromForcePlan = true;
        for (TemplateAssignment company : plan.companies()) {
            Blueprint card = Blueprint.forConfiguration(company.configurationNumber());
            if (autoAssign) {
                applyAssignment(card, company);
            }
            plans.add(card);
        }
        for (PlannedLance lance : plan.lances()) {
            Blueprint card = lance.allMasters() ? Blueprint.allMasterLance() : Blueprint.slaveLance();
            if (autoAssign) {
                card.companyId = lance.master().getId();
                for (int slotIndex = 0; slotIndex < lance.members().size(); slotIndex++) {
                    if (lance.allMasters()) {
                        card.lanceMasters[slotIndex] = lance.members().get(slotIndex).getId();
                    } else {
                        card.directSlaves[slotIndex] = lance.members().get(slotIndex).getId();
                    }
                }
            }
            plans.add(card);
        }
        refresh();
        if (autoAssign) {
            showLeftOutLances(plan.leftOut(), 0);
        }
    }

    /** Updates the force-plan line and the Auto-Form button from the current unassigned pool. */
    private void updateForcePlanSummary() {
        ForcePlan plan = newFormationPlanner().planForce();
        autoFormButton.setEnabled(!plan.isEmpty());
        if (plan.isEmpty()) {
            forcePlanLabel.setText(" ");
            return;
        }
        List<String> planParts = new ArrayList<>();
        for (TemplateAssignment company : plan.companies()) {
            planParts.add(Messages.getString("C3NetworkManagerDialog.planPartConfig",
                  company.configurationNumber(), company.filledCount()));
        }
        for (PlannedLance lance : plan.lances()) {
            String key = lance.allMasters() ? "C3NetworkManagerDialog.planPartAllMasterLance"
                  : "C3NetworkManagerDialog.planPartLance";
            planParts.add(Messages.getString(key, 1 + lance.members().size()));
        }
        forcePlanLabel.setText(Messages.getString("C3NetworkManagerDialog.forcePlan",
              String.join(" + ", planParts), plan.leftoverUnits()));
    }

    /**
     * Lays out the configuration template; partially filled existing plans ask before being replaced. With
     * auto-assign checked the slots are pre-filled from the unassigned force as far as it reaches, lobby lances kept
     * whole; unchecked, the template arrives empty and the player drags units in.
     */
    private void startBlueprint(int configurationNumber) {
        if (plansFilledCount() > 0) {
            int result = JOptionPane.showConfirmDialog(this,
                  Messages.getString("C3NetworkManagerDialog.replacePlan", configurationNumber),
                  Messages.getString("C3NetworkManagerDialog.title"),
                  JOptionPane.YES_NO_OPTION,
                  JOptionPane.WARNING_MESSAGE);
            if (result != JOptionPane.YES_OPTION) {
                updatePlanButtonHighlight();
                return;
            }
        }
        plans.clear();
        plansFromForcePlan = false;
        Blueprint blueprint = Blueprint.forConfiguration(configurationNumber);
        plans.add(blueprint);
        List<LeftOutLance> leftOut = List.of();
        if (autoAssignCheckBox.isSelected()) {
            TemplateAssignment assignment = newFormationPlanner().planTemplate(configurationNumber);
            applyAssignment(blueprint, assignment);
            leftOut = assignment.leftOut();
        }
        refresh();
        showLeftOutLances(leftOut, configurationNumber);
    }

    /**
     * Fills the empty plan cards with units, as if auto-assign had been checked when they were laid out: a single
     * configuration card is planned again for its configuration, a force plan is laid out again filled.
     */
    private void autoAssignEmptyPlans() {
        boolean isSingleCard = plans.size() == 1;
        boolean isConfigurationCard = isSingleCard && plans.getFirst().isCompanyConfiguration();
        if (plansFromForcePlan || !isConfigurationCard) {
            createForcePlanTemplates(true);
            return;
        }
        Blueprint blueprint = plans.getFirst();
        TemplateAssignment assignment = newFormationPlanner().planTemplate(blueprint.configurationNumber);
        applyAssignment(blueprint, assignment);
        refresh();
        showLeftOutLances(assignment.leftOut(), blueprint.configurationNumber);
    }

    private C3FormationPlanner newFormationPlanner() {
        return new C3FormationPlanner(game, localPlayerId);
    }

    /**
     * Lights the button that laid out the plan on screen - a configuration number or Plan Force - and no other.
     * The buttons toggle on click, so this also undoes a click whose plan was then not laid out (a declined
     * replace prompt, an empty force plan).
     */
    private void updatePlanButtonHighlight() {
        boolean hasConfigurationPlan = !plans.isEmpty() && !plansFromForcePlan;
        for (int configNumber = 1; configNumber <= 4; configNumber++) {
            configStartButtons[configNumber - 1].setSelected(hasConfigurationPlan
                  && (plans.getFirst().configurationNumber == configNumber));
        }
        autoFormButton.setSelected(!plans.isEmpty() && plansFromForcePlan);
    }

    /** Copies planned units into a card's slots; the card and the assignment share the configuration's layout. */
    private void applyAssignment(Blueprint card, TemplateAssignment assignment) {
        card.companyId = idOrNull(assignment.head());
        for (int slotIndex = 0; slotIndex < card.directSlaves.length; slotIndex++) {
            card.directSlaves[slotIndex] = idOrNull(assignment.directSlaves()[slotIndex]);
        }
        for (int lanceIndex = 0; lanceIndex < card.lanceMasters.length; lanceIndex++) {
            card.lanceMasters[lanceIndex] = idOrNull(assignment.lanceMasters()[lanceIndex]);
            for (int slotIndex = 0; slotIndex < card.lanceSlaves[lanceIndex].length; slotIndex++) {
                card.lanceSlaves[lanceIndex][slotIndex] = idOrNull(assignment.lanceSlaves()[lanceIndex][slotIndex]);
            }
        }
    }

    @Nullable
    private static Integer idOrNull(@Nullable Entity unit) {
        return (unit == null) ? null : unit.getId();
    }

    /**
     * Names the lobby lances auto-assign kept out of the plan, with the reason, on the status line. Lances are
     * never split, so a lance that fits no block of the template stays out whole.
     *
     * @param configurationNumber the template's configuration, or 0 for a force plan
     */
    private void showLeftOutLances(List<LeftOutLance> leftOut, int configurationNumber) {
        if (leftOut.isEmpty()) {
            return;
        }
        List<String> descriptions = new ArrayList<>();
        for (LeftOutLance lance : leftOut) {
            descriptions.add(Messages.getString("C3NetworkManagerDialog.leftOut." + lance.reason().name(),
                  lance.lanceName(), lance.masterCount(), lance.slaveCount(), configurationNumber));
        }
        statusLabel.setText(Messages.getString("C3NetworkManagerDialog.leftOutLances",
              String.join("; ", descriptions)));
    }

    /** Drops slots whose unit was deleted or got networked outside the plans since the last refresh. */
    private void pruneStaleBlueprintAssignments() {
        for (Entity entity : game.inGameTWEntities()) {
            if (isNetworked(entity) && planContainsUnit(entity.getId())) {
                unassignFromPlans(entity.getId());
            }
        }
    }

    /**
     * One plan card as a tree node: company/master slot on top, its slaves, then the lance blocks, all typed. The
     * company's own lance is listed first, next to the head as in the diagram (CR p.199) - the head's own slaves,
     * or in Configuration 1 the short lance.
     */
    private DefaultMutableTreeNode buildBlueprintNode(int planIndex) {
        Blueprint blueprint = plans.get(planIndex);
        DefaultMutableTreeNode planNode = new DefaultMutableTreeNode(new BlueprintHeader(planIndex));
        DefaultMutableTreeNode companyNode = new DefaultMutableTreeNode(
              new BlueprintSlotRef(planIndex, SlotKind.COMPANY, -1, -1));
        planNode.add(companyNode);
        for (int slotIndex = 0; slotIndex < blueprint.directSlaves.length; slotIndex++) {
            companyNode.add(new DefaultMutableTreeNode(
                  new BlueprintSlotRef(planIndex, SlotKind.DIRECT_SLAVE, -1, slotIndex)));
        }
        for (int lanceIndex : lanceDisplayOrder(blueprint)) {
            DefaultMutableTreeNode lanceMasterNode = new DefaultMutableTreeNode(
                  new BlueprintSlotRef(planIndex, SlotKind.LANCE_MASTER, lanceIndex, -1));
            companyNode.add(lanceMasterNode);
            for (int slotIndex = 0; slotIndex < blueprint.lanceSlaves[lanceIndex].length; slotIndex++) {
                lanceMasterNode.add(new DefaultMutableTreeNode(
                      new BlueprintSlotRef(planIndex, SlotKind.LANCE_SLAVE, lanceIndex, slotIndex)));
            }
        }
        return planNode;
    }

    /** The card's lance indices in display order: Configuration 1's short company lance first, then the others. */
    private List<Integer> lanceDisplayOrder(Blueprint blueprint) {
        int companyLanceIndex = blueprint.isCompanyConfiguration()
              ? ConfigurationShape.of(blueprint.configurationNumber).companyLanceIndex() : -1;
        List<Integer> order = new ArrayList<>();
        if (companyLanceIndex >= 0) {
            order.add(companyLanceIndex);
        }
        for (int lanceIndex = 0; lanceIndex < blueprint.lanceMasters.length; lanceIndex++) {
            if (lanceIndex != companyLanceIndex) {
                order.add(lanceIndex);
            }
        }
        return order;
    }

    /** True when the unit's equipment fits the slot: company computer count, master, or slave. */
    private boolean unitFitsSlot(Entity unit, BlueprintSlotRef ref) {
        if (unit.hasNhC3() || (unit.getOwnerId() != localPlayerId) || isNetworked(unit)) {
            return false;
        }
        Blueprint blueprint = planAt(ref);
        return switch (ref.kind()) {
            // At least N computers: extra computers idle, so a triple-master can head Configuration 1 or 2 too.
            // On a lance card the company slot is simply the lance master - any master fits.
            case COMPANY -> blueprint.isCompanyConfiguration()
                  ? (unit.getOperableC3MCount() >= blueprint.configurationNumber)
                  : (unit.getOperableC3MCount() > 0);
            case LANCE_MASTER -> unit.getOperableC3MCount() > 0;
            case DIRECT_SLAVE, LANCE_SLAVE -> unit.hasC3S() && (unit.getOperableC3MCount() == 0);
        };
    }

    /** Assigns the first dropped unit to the slot, then auto-fills remaining units into empty fitting slots. */
    private void assignDroppedToSlot(List<Entity> droppedUnits, BlueprintSlotRef ref) {
        Entity firstUnit = droppedUnits.get(0);
        if (!unitFitsSlot(firstUnit, ref)) {
            statusLabel.setText(Messages.getString("C3NetworkManagerDialog.wrongSlotUnit"));
            return;
        }
        unassignFromPlans(firstUnit.getId());
        planAt(ref).setSlotValue(ref, firstUnit.getId());
        if (droppedUnits.size() > 1) {
            autoFillBlueprint(droppedUnits.subList(1, droppedUnits.size()));
        }
        refresh();
    }

    /** Fills each unit into the first empty slot it fits, across all plan cards in order. */
    private void autoFillBlueprint(List<Entity> droppedUnits) {
        for (Entity unit : droppedUnits) {
            BlueprintSlotRef emptySlot = firstEmptyFittingSlot(unit);
            if (emptySlot != null) {
                unassignFromPlans(unit.getId());
                planAt(emptySlot).setSlotValue(emptySlot, unit.getId());
            }
        }
        refresh();
    }

    /** The first empty slot the unit fits, in fill order across all cards, or {@code null} when none is open. */
    @Nullable
    private BlueprintSlotRef firstEmptyFittingSlot(Entity unit) {
        List<BlueprintSlotRef> slotOrder = new ArrayList<>();
        for (int planIndex = 0; planIndex < plans.size(); planIndex++) {
            Blueprint blueprint = plans.get(planIndex);
            slotOrder.add(new BlueprintSlotRef(planIndex, SlotKind.COMPANY, -1, -1));
            for (int lanceIndex = 0; lanceIndex < blueprint.lanceMasters.length; lanceIndex++) {
                slotOrder.add(new BlueprintSlotRef(planIndex, SlotKind.LANCE_MASTER, lanceIndex, -1));
            }
            for (int slotIndex = 0; slotIndex < blueprint.directSlaves.length; slotIndex++) {
                slotOrder.add(new BlueprintSlotRef(planIndex, SlotKind.DIRECT_SLAVE, -1, slotIndex));
            }
            for (int lanceIndex = 0; lanceIndex < blueprint.lanceSlaves.length; lanceIndex++) {
                for (int slotIndex = 0; slotIndex < blueprint.lanceSlaves[lanceIndex].length; slotIndex++) {
                    slotOrder.add(new BlueprintSlotRef(planIndex, SlotKind.LANCE_SLAVE, lanceIndex, slotIndex));
                }
            }
        }
        for (BlueprintSlotRef ref : slotOrder) {
            if ((planAt(ref).slotValue(ref) == null) && unitFitsSlot(unit, ref)) {
                return ref;
            }
        }
        return null;
    }

    /**
     * Applies every plan card in dependency order - company master designation, direct slaves, lance masters,
     * then each lance's slaves - through the same lobby actions as everything else, and discards the plans when
     * done. Cards without their company/master unit and slaves whose lance master slot is empty are skipped and
     * reported.
     */
    private void buildNetworkFromBlueprint() {
        pruneStaleBlueprintAssignments();
        int orphanSlaves = 0;
        boolean builtAnything = false;
        for (Blueprint blueprint : plans) {
            Entity company = (blueprint.companyId == null) ? null : game.getEntity(blueprint.companyId);
            if (company == null) {
                orphanSlaves += blueprint.filledCount();
                continue;
            }
            builtAnything = true;
            if (blueprint.isCompanyConfiguration()) {
                lobby.lobbyActions.c3SetCompanyMaster(List.of(company));
            }
            List<Entity> directSlaveUnits = resolveUnits(blueprint.directSlaves);
            if (!directSlaveUnits.isEmpty()) {
                lobby.lobbyActions.c3Connect(directSlaveUnits, company.getId(), false);
            }
            List<Entity> lanceMasterUnits = resolveUnits(blueprint.lanceMasters);
            if (!lanceMasterUnits.isEmpty()) {
                lobby.lobbyActions.c3Connect(lanceMasterUnits, company.getId(), false);
            }
            for (int lanceIndex = 0; lanceIndex < blueprint.lanceMasters.length; lanceIndex++) {
                List<Entity> lanceSlaveUnits = resolveUnits(blueprint.lanceSlaves[lanceIndex]);
                Integer lanceMasterId = blueprint.lanceMasters[lanceIndex];
                if (lanceMasterId == null) {
                    orphanSlaves += lanceSlaveUnits.size();
                } else if (!lanceSlaveUnits.isEmpty()) {
                    lobby.lobbyActions.c3Connect(lanceSlaveUnits, lanceMasterId, false);
                }
            }
        }
        if (!builtAnything) {
            statusLabel.setText(Messages.getString("C3NetworkManagerDialog.buildNeedsCompany"));
            return;
        }
        plans.clear();
        refresh();
        if (orphanSlaves > 0) {
            statusLabel.setText(Messages.getString("C3NetworkManagerDialog.builtOrphans", orphanSlaves));
        }
    }

    private List<Entity> resolveUnits(Integer[] slotValues) {
        List<Entity> units = new ArrayList<>();
        for (Integer entityId : slotValues) {
            if (entityId != null) {
                Entity entity = game.getEntity(entityId);
                if (entity != null) {
                    units.add(entity);
                }
            }
        }
        return units;
    }

    /** Turns masters dropped on empty network space into new-network seeds. */
    private void seedDroppedMasters(List<Entity> droppedUnits) {
        boolean seededAny = false;
        for (Entity unit : droppedUnits) {
            if ((unit.getOperableC3MCount() > 0) && (unit.getOwnerId() == localPlayerId) && !isNetworked(unit)) {
                seededMasterIds.add(unit.getId());
                seededAny = true;
            }
        }
        if (seededAny) {
            refresh();
        } else {
            statusLabel.setText(Messages.getString("C3NetworkManagerDialog.dropMasterToStart"));
        }
    }

    /** Forms a peer network from the units dropped on a peer seed row; needs at least two matching units. */
    private void formPeerNetworkFrom(List<Entity> droppedUnits, PeerSystem system) {
        List<Entity> matchingUnits = new ArrayList<>();
        for (Entity unit : droppedUnits) {
            if (system.matches(unit)) {
                matchingUnits.add(unit);
            }
        }
        if (matchingUnits.size() < 2) {
            statusLabel.setText(Messages.getString("C3NetworkManagerDialog.selectTwoPeers"));
            return;
        }
        lobby.lobbyActions.c3JoinNh(matchingUnits, matchingUnits.get(0).getId(), true);
        refresh();
    }

    private List<Entity> sortedById(List<Entity> entities) {
        List<Entity> sorted = new ArrayList<>(entities);
        sorted.sort(Comparator.comparingInt(Entity::getId));
        return sorted;
    }

    private void restoreRosterSelection(List<Entity> previouslySelected) {
        List<Integer> indices = new ArrayList<>();
        for (int index = 0; index < rosterModel.size(); index++) {
            if (!(rosterModel.get(index) instanceof Entity rowEntity)) {
                continue;
            }
            for (Entity entity : previouslySelected) {
                if (rowEntity.getId() == entity.getId()) {
                    indices.add(index);
                }
            }
        }
        int[] indexArray = new int[indices.size()];
        for (int position = 0; position < indices.size(); position++) {
            indexArray[position] = indices.get(position);
        }
        rosterList.setSelectedIndices(indexArray);
    }

    /** The entities selected in the network tree (network group nodes resolve to all their members). */
    private List<Entity> selectedTreeEntities() {
        List<Entity> selected = new ArrayList<>();
        TreePath[] paths = networkTree.getSelectionPaths();
        if (paths == null) {
            return selected;
        }
        for (TreePath path : paths) {
            Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
            if (userObject instanceof MemberRow row) {
                selected.add(row.entity());
            } else if (userObject instanceof NetworkGroup group) {
                selected.addAll(group.members());
            }
        }
        return selected;
    }

    /** Resolves a tree path to the unit that drops and connects go to, or {@code null} for non-unit rows. */
    @Nullable
    private Entity targetOf(TreePath path) {
        Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
        if (userObject instanceof MemberRow row) {
            return row.entity();
        }
        if (userObject instanceof NetworkGroup group) {
            return group.topUnit();
        }
        if (userObject instanceof SlotPlaceholder placeholder) {
            return game.getEntity(placeholder.targetId());
        }
        return null;
    }

    /**
     * Connects the units to the target through the same lobby actions the right-click menu uses. Roles are
     * computed, not picked: an independent multi-master target, or any independent master receiving other
     * masters, is designated company master automatically so its full capacity opens up.
     */
    private void connectUnitsTo(List<Entity> joiners, Entity target) {
        joiners.remove(target);
        if (joiners.isEmpty()) {
            return;
        }
        if (target.hasNhC3()) {
            lobby.lobbyActions.c3JoinNh(joiners, target.getId(), false);
            refresh();
            return;
        }
        boolean joiningMasters = false;
        for (Entity joiner : joiners) {
            if (joiner.getOperableC3MCount() > 0) {
                joiningMasters = true;
                break;
            }
        }
        boolean targetIsSubordinate = (target.getC3Master() != null) && !target.isC3CompanyCommander();
        if (!target.isC3CompanyCommander() && !targetIsSubordinate
              && (joiningMasters || (target.getOperableC3MCount() >= 2))) {
            lobby.lobbyActions.c3SetCompanyMaster(List.of(target));
        }
        lobby.lobbyActions.c3Connect(joiners, target.getId(), false);
        refresh();
    }

    private void formPeerNetwork() {
        List<Entity> selection = selectedRosterEntities();
        if (selection.size() < 2) {
            statusLabel.setText(Messages.getString("C3NetworkManagerDialog.selectTwoPeers"));
            return;
        }
        lobby.lobbyActions.c3JoinNh(selection, selection.get(0).getId(), true);
        refresh();
    }

    private void disconnectSelection() {
        List<Entity> selection = selectedTreeEntities();
        if (selection.isEmpty()) {
            statusLabel.setText(Messages.getString("C3NetworkManagerDialog.selectNetworkUnits"));
            return;
        }
        lobby.lobbyActions.c3DisconnectFromNetwork(selection);
        refresh();
    }

    /**
     * Disconnects every C3 network of the local player after confirmation, demotes company masters left without
     * a network, clears dragged-over seeds and empties the plan cards - the chosen plan stays laid out for the units
     * to be placed again. Discard Plan is what removes the plan itself. Peer-net name units are skipped - they fall
     * back to solo automatically once their partners leave. Allied units in shared networks are not touched.
     */
    private void resetNetworks() {
        List<Entity> unitsToDisconnect = new ArrayList<>();
        List<Entity> loneCompanyMasters = new ArrayList<>();
        for (Entity entity : game.inGameTWEntities()) {
            if ((entity.getOwnerId() != localPlayerId) || !entity.hasAnyC3System()) {
                continue;
            }
            if (!isNetworked(entity)) {
                // A company master designated by Build keeps showing as a network even with nobody under it
                if (entity.isC3CompanyCommander()) {
                    loneCompanyMasters.add(entity);
                }
                continue;
            }
            String netId = entity.getC3NetId();
            boolean isPeerNetNameUnit = entity.hasNhC3() && (netId != null)
                  && netId.endsWith(Entity.C3_NETWORK_ID_SEPARATOR + entity.getId());
            if (!isPeerNetNameUnit) {
                unitsToDisconnect.add(entity);
            }
        }
        boolean hasNetworksToClear = !unitsToDisconnect.isEmpty() || !loneCompanyMasters.isEmpty();
        boolean hasPlanningToClear = !seededMasterIds.isEmpty() || (plansFilledCount() > 0);
        if (!hasNetworksToClear && !hasPlanningToClear) {
            statusLabel.setText(Messages.getString("C3NetworkManagerDialog.nothingToReset"));
            return;
        }
        int result = JOptionPane.showConfirmDialog(this,
              Messages.getString("C3NetworkManagerDialog.confirmReset"),
              Messages.getString("C3NetworkManagerDialog.title"),
              JOptionPane.YES_NO_OPTION,
              JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        seededMasterIds.clear();
        // The chosen plan stays laid out, emptied, so the player can fill it again
        for (Blueprint plan : plans) {
            plan.clearSlots();
        }
        if (!unitsToDisconnect.isEmpty()) {
            lobby.lobbyActions.c3DisconnectFromNetwork(unitsToDisconnect);
        }
        if (!loneCompanyMasters.isEmpty()) {
            lobby.lobbyActions.c3SetLanceMaster(loneCompanyMasters);
        }
        LOGGER.debug("[C3Manager] Reset All: disconnected {} units, demoted {} lone company masters",
              unitsToDisconnect.size(), loneCompanyMasters.size());
        refresh();
    }

    /** One network in the tree: its members and a display summary with size and BV surcharge. */
    private record NetworkGroup(List<Entity> members) {

        /** The unit heading the network: the top of the master chain, or simply the first member for peer nets. */
        Entity topUnit() {
            for (Entity member : members) {
                Entity master = member.getC3Master();
                if ((master == null) || (master.getId() == member.getId())) {
                    if (member.hasC3M() || member.hasC3MM() || member.hasNhC3()) {
                        return member;
                    }
                }
            }
            return members.get(0);
        }

        String description() {
            Entity top = topUnit();
            if (members.size() == 1) {
                // A new-network seed: a master dragged over (or a designated company master), no members yet
                return "<html><b>"
                      + Messages.getString("C3NetworkManagerDialog.seedSummary", top.getShortNameRaw(), top.getId())
                      + "</b>" + configurationSummary() + "</html>";
            }
            int bvPercent = Math.min(5 * members.size(), 40);
            String systemName;
            if (top.hasC3i()) {
                systemName = "C3i";
            } else if (top.hasNavalC3()) {
                systemName = "NC3";
            } else if (top.hasNovaCEWS()) {
                systemName = "Nova CEWS";
            } else {
                systemName = "C3";
            }
            return "<html><b>"
                  + Messages.getString("C3NetworkManagerDialog.networkSummary",
                        top.getShortNameRaw(), systemName, members.size(), bvPercent)
                  + "</b>" + configurationSummary() + "</html>";
        }

        /**
         * Names the rulebook configuration this network is building toward and what is still missing. The company
         * node's C3 Master count is the configuration number (CR p.199): one master is the dedicated company master
         * of Configuration 1, two the dual-computer node of Configuration 2, three and four the consolidated nodes
         * of Configurations 3 and 4. Canonical full sizes are 12, 12, 11 and 10 units.
         */
        private String configurationSummary() {
            Entity commander = null;
            for (Entity member : members) {
                if (!member.hasNhC3() && member.isC3CompanyCommander()) {
                    commander = member;
                    break;
                }
            }
            if (commander == null) {
                return "";
            }
            // The configuration is read from the structure, not from carried computers: extra computers idle, so
            // a triple-master heading three lance masters is Configuration 1. Each block of 3 direct slaves
            // occupies one of the commander's own lance computers (Config 1: none, 2: one, 3: two, 4: three).
            int directSlaveCount = 0;
            for (Entity member : members) {
                if (!member.equals(commander) && member.C3MasterIs(commander) && member.hasC3S()) {
                    directSlaveCount++;
                }
            }
            int configurationNumber = Math.min(1 + ((directSlaveCount + 2) / 3),
                  Math.min(commander.getOperableC3MCount(), 4));
            int canonicalSize = switch (configurationNumber) {
                case 3 -> 11;
                case 4 -> 10;
                default -> 12;
            };
            if (members.size() > canonicalSize) {
                // Larger than the canonical shape - not one of the four book configurations, so claim none
                return "";
            }
            if (members.size() == canonicalSize) {
                return " <b><font color='#4CAF7D'>"
                      + Messages.getString("C3NetworkManagerDialog.configComplete",
                            configurationNumber, members.size())
                      + "</font></b>";
            }
            int missingMasters = commander.calculateFreeC3MNodes();
            int missingSlaves = Math.max(0, canonicalSize - members.size() - missingMasters);
            List<String> missingParts = new ArrayList<>();
            if (missingMasters > 0) {
                missingParts.add(Messages.getString("C3NetworkManagerDialog.configNeedsMasters", missingMasters));
            }
            if (missingSlaves > 0) {
                missingParts.add(Messages.getString("C3NetworkManagerDialog.configNeedsSlaves", missingSlaves));
            }
            return " <b><font color='" + COMPANY_COLOR + "'>"
                  + Messages.getString("C3NetworkManagerDialog.configProgress",
                        configurationNumber, members.size(), canonicalSize, String.join(", ", missingParts))
                  + "</font></b>";
        }
    }

    /** Renders roster rows as "name #id - equipment", marking units already placed in the plan. */
    private class RosterRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
              boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof Entity entity) {
                String plannedSuffix = planContainsUnit(entity.getId())
                      ? " <font color='" + MUTED_COLOR + "'>"
                            + Messages.getString("C3NetworkManagerDialog.plannedSuffix") + "</font>"
                      : "";
                setText("<html>" + Messages.getString("C3NetworkManagerDialog.rosterLabel",
                      entity.getShortNameRaw(), entity.getId(),
                      "<b>" + equipmentSummary(entity) + "</b>") + plannedSuffix + "</html>");
            } else if (value instanceof RosterHeader header) {
                setText("<html><b><font color='#8A8A8A'>" + Messages.getString(header.labelKey())
                      + "</font></b></html>");
            }
            return this;
        }

        /** A short description of the unit's C3 gear, e.g. "Master x3" or "C3i". */
        private static String equipmentSummary(Entity entity) {
            if (entity.hasC3i()) {
                return "C3i";
            }
            if (entity.hasNavalC3()) {
                return "NC3";
            }
            if (entity.hasNovaCEWS()) {
                return "Nova CEWS";
            }
            int masterCount = entity.getOperableC3MCount();
            if (masterCount > 1) {
                return Messages.getString("C3NetworkManagerDialog.equipMasters", masterCount);
            }
            if (masterCount == 1) {
                return Messages.getString("C3NetworkManagerDialog.equipMaster");
            }
            return Messages.getString("C3NetworkManagerDialog.equipSlave");
        }
    }

    private class NetworkTreeRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
              boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            if (userObject instanceof NetworkGroup group) {
                setText(group.description());
            } else if (userObject instanceof MemberRow memberRow) {
                setText(memberLabel(memberRow));
            } else if (userObject instanceof SlotPlaceholder placeholder) {
                String key = placeholder.masterSlots()
                      ? "C3NetworkManagerDialog.slotMasters" : "C3NetworkManagerDialog.slotSlaves";
                setText("<html><i><font color='" + placeholder.colorHex() + "'>"
                      + Messages.getString(key, placeholder.freeCount()) + "</font></i></html>");
                setIcon(null);
            } else if (userObject instanceof PeerSeed seed) {
                setText("<html><i><font color='" + PEER_COLOR + "'>"
                      + Messages.getString("C3NetworkManagerDialog.peerSeed", seed.system().displayName())
                      + "</font></i></html>");
                setIcon(null);
            } else if (userObject instanceof StartHint) {
                setText("<html><i><font color='" + MUTED_COLOR + "'>"
                      + Messages.getString("C3NetworkManagerDialog.startHint") + "</font></i></html>");
                setIcon(null);
            } else if ((userObject instanceof BlueprintHeader header) && (header.planIndex() < plans.size())) {
                Blueprint blueprint = plans.get(header.planIndex());
                String headerText;
                if (blueprint.isCompanyConfiguration()) {
                    headerText = Messages.getString("C3NetworkManagerDialog.planHeader",
                          blueprint.configurationNumber, blueprint.filledCount(), blueprint.totalSlots());
                } else if (blueprint.configurationNumber == 0) {
                    headerText = Messages.getString("C3NetworkManagerDialog.planHeaderLance",
                          blueprint.filledCount(), blueprint.totalSlots());
                } else {
                    headerText = Messages.getString("C3NetworkManagerDialog.planHeaderAllMaster",
                          blueprint.filledCount(), blueprint.totalSlots());
                }
                setText("<html><b><font color='" + COMPANY_COLOR + "'>" + headerText + "</font></b></html>");
                setIcon(null);
            } else if ((userObject instanceof BlueprintSlotRef slotRef) && (slotRef.planIndex() < plans.size())) {
                setText(blueprintSlotLabel(slotRef));
                setIcon(null);
            }
            return this;
        }
    }

    /** The label for one plan slot: the assigned unit in its lance color, or the typed empty-slot prompt. */
    private String blueprintSlotLabel(BlueprintSlotRef slotRef) {
        Blueprint blueprint = planAt(slotRef);
        String color = blueprintSlotColor(blueprint, slotRef);
        Integer assignedId = blueprint.slotValue(slotRef);
        Entity assigned = (assignedId == null) ? null : game.getEntity(assignedId);
        if (assigned != null) {
            String roleKey = switch (slotRef.kind()) {
                case COMPANY -> blueprint.isCompanyConfiguration()
                      ? "C3NetworkManagerDialog.roleCompanyMaster" : "C3NetworkManagerDialog.roleLanceMaster";
                case LANCE_MASTER -> "C3NetworkManagerDialog.roleLanceMaster";
                case DIRECT_SLAVE, LANCE_SLAVE -> "C3NetworkManagerDialog.roleSlave";
            };
            return "<html><b><font color='" + color + "'>[" + Messages.getString(roleKey) + "]</font></b> "
                  + assigned.getShortNameRaw()
                  + " <font color='" + MUTED_COLOR + "'>#" + assigned.getId()
                  + Messages.getString("C3NetworkManagerDialog.plannedSuffix") + "</font></html>";
        }
        String emptyText = switch (slotRef.kind()) {
            case COMPANY -> Messages.getString("C3NetworkManagerDialog.slotCompanyEmpty",
                  blueprint.configurationNumber);
            case LANCE_MASTER -> Messages.getString("C3NetworkManagerDialog.slotLanceMasterEmpty");
            case DIRECT_SLAVE, LANCE_SLAVE -> Messages.getString("C3NetworkManagerDialog.slotSlaveEmpty");
        };
        return "<html><i><font color='" + color + "'>" + emptyText + "</font></i></html>";
    }

    /**
     * The color of one plan slot. Company templates are colored like the rulebook's configuration diagram (CR
     * p.199): the head unit orange, the company's own lance yellow, the other lances red then blue. The company's
     * own lance is the first three slaves the head runs itself, or in Configuration 1 the two-slave lance under its
     * third lance master. Built networks use the same colors (see {@link #buildCompanySubtree}).
     */
    private String blueprintSlotColor(Blueprint blueprint, BlueprintSlotRef slotRef) {
        if (!blueprint.isCompanyConfiguration()) {
            return ((slotRef.kind() == SlotKind.COMPANY) || (slotRef.kind() == SlotKind.DIRECT_SLAVE))
                  ? COMPANY_COLOR : LANCE_COLORS[slotRef.lanceIndex() % LANCE_COLORS.length];
        }
        return switch (slotRef.kind()) {
            case COMPANY -> COMPANY_COLOR;
            case DIRECT_SLAVE -> headLanceColor(slotRef.slotIndex() / Entity.MAX_C3M_SUBORDINATES,
                  blueprint.lanceMasters.length);
            case LANCE_MASTER, LANCE_SLAVE -> {
                boolean isConfiguration1CompanyLance = (blueprint.configurationNumber == 1)
                      && (slotRef.lanceIndex() == (blueprint.lanceMasters.length - 1));
                yield isConfiguration1CompanyLance ? COMPANY_LANCE_COLOR
                      : LANCE_COLORS[slotRef.lanceIndex() % LANCE_COLORS.length];
            }
        };
    }

    /**
     * Moves units by drag and drop: dragging from the roster or the tree onto a network node, member or empty-slot
     * row connects the dragged units there; dragging network members onto the roster disconnects them. Units travel
     * as a comma-separated id list, so drags stay within this dialog's game.
     */
    private class UnitTransferHandler extends TransferHandler {
        private final boolean treeSide;

        UnitTransferHandler(boolean treeSide) {
            this.treeSide = treeSide;
        }

        @Override
        public int getSourceActions(JComponent component) {
            return COPY;
        }

        @Override
        protected Transferable createTransferable(JComponent component) {
            List<Entity> selection = treeSide ? selectedTreeEntities() : selectedRosterEntities();
            if (selection.isEmpty()) {
                return null;
            }
            StringBuilder ids = new StringBuilder(DRAG_PAYLOAD_MARKER);
            for (Entity entity : selection) {
                if (ids.length() > DRAG_PAYLOAD_MARKER.length()) {
                    ids.append(",");
                }
                ids.append(entity.getId());
            }
            return new StringSelection(ids.toString());
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop() || !support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return false;
            }
            if (treeSide) {
                JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
                TreePath path = dropLocation.getPath();
                if ((path == null) || (path.getLastPathComponent() == treeRoot)) {
                    // Empty area: dropping a master here starts a new network
                    return true;
                }
                Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                return (userObject instanceof PeerSeed) || (userObject instanceof StartHint)
                      || (userObject instanceof BlueprintSlotRef) || (userObject instanceof BlueprintHeader)
                      || (targetOf(path) != null);
            }
            return true;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }
            List<Entity> droppedUnits = droppedUnits(support);
            if (droppedUnits.isEmpty()) {
                return false;
            }
            if (treeSide) {
                JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
                TreePath path = dropLocation.getPath();
                if ((path == null) || (path.getLastPathComponent() == treeRoot)) {
                    seedDroppedMasters(droppedUnits);
                    return true;
                }
                Object userObject = ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
                if (userObject instanceof StartHint) {
                    seedDroppedMasters(droppedUnits);
                    return true;
                }
                if (userObject instanceof PeerSeed seed) {
                    formPeerNetworkFrom(droppedUnits, seed.system());
                    return true;
                }
                if ((userObject instanceof BlueprintSlotRef slotRef) && !plans.isEmpty()) {
                    assignDroppedToSlot(droppedUnits, slotRef);
                    return true;
                }
                if ((userObject instanceof BlueprintHeader) && !plans.isEmpty()) {
                    autoFillBlueprint(droppedUnits);
                    return true;
                }
                Entity target = targetOf(path);
                if (target == null) {
                    return false;
                }
                connectUnitsTo(droppedUnits, target);
            } else {
                JList.DropLocation dropLocation = (JList.DropLocation) support.getDropLocation();
                int dropIndex = dropLocation.getIndex();
                if (!dropLocation.isInsert() && (dropIndex >= 0) && (dropIndex < rosterModel.size())
                      && (rosterModel.get(dropIndex) instanceof Entity targetUnit)) {
                    // Unit-to-unit drop: connect the dragged units to the unit they were dropped on
                    connectDroppedToRosterUnit(droppedUnits, targetUnit);
                    return true;
                }
                // Blank-space drop: disconnect members dragged back from a network, clear planned units out of
                // their blueprint slots, and demote seeds/solo company commanders back to ordinary roster units
                boolean anythingCleared = false;
                for (Entity unit : droppedUnits) {
                    if (planContainsUnit(unit.getId())) {
                        unassignFromPlans(unit.getId());
                        anythingCleared = true;
                    }
                }
                List<Entity> networkedUnits = new ArrayList<>();
                List<Entity> soloCommandersToDemote = new ArrayList<>();
                for (Entity unit : droppedUnits) {
                    if (seededMasterIds.remove(unit.getId())) {
                        anythingCleared = true;
                    }
                    if (isNetworked(unit)) {
                        networkedUnits.add(unit);
                    } else if (unit.isC3CompanyCommander()) {
                        soloCommandersToDemote.add(unit);
                    }
                }
                if (networkedUnits.isEmpty() && soloCommandersToDemote.isEmpty() && !anythingCleared) {
                    return false;
                }
                if (!networkedUnits.isEmpty()) {
                    lobby.lobbyActions.c3DisconnectFromNetwork(networkedUnits);
                }
                if (!soloCommandersToDemote.isEmpty()) {
                    lobby.lobbyActions.c3SetLanceMaster(soloCommandersToDemote);
                }
                refresh();
            }
            return true;
        }
    }

    /**
     * Handles dropping units directly onto another unit in the roster - the "drag units to each other" gesture. A
     * master target forms (or grows) its network; two or more units of the same masterless system form a peer
     * network; anything else gets a hint instead of a silent failure.
     */
    private void connectDroppedToRosterUnit(List<Entity> droppedUnits, Entity target) {
        droppedUnits.remove(target);
        if (droppedUnits.isEmpty()) {
            return;
        }
        if (target.getOperableC3MCount() > 0) {
            connectUnitsTo(droppedUnits, target);
            return;
        }
        if (target.hasNhC3()) {
            List<Entity> peerUnits = new ArrayList<>();
            peerUnits.add(target);
            for (Entity unit : droppedUnits) {
                if (C3Util.sameNhC3System(target, unit)) {
                    peerUnits.add(unit);
                }
            }
            if (peerUnits.size() >= 2) {
                lobby.lobbyActions.c3JoinNh(peerUnits, target.getId(), true);
                refresh();
                return;
            }
        }
        statusLabel.setText(Messages.getString("C3NetworkManagerDialog.dropOnMaster"));
    }

    /** Marks drag payloads created by this dialog, so text dragged in from elsewhere is never taken as ids. */
    private static final String DRAG_PAYLOAD_MARKER = "C3NetworkManager:";

    /** The units named in a drop's id list; empty when the payload is not one of ours. */
    private List<Entity> droppedUnits(TransferHandler.TransferSupport support) {
        List<Entity> units = new ArrayList<>();
        try {
            String payload = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
            if (!payload.startsWith(DRAG_PAYLOAD_MARKER)) {
                return List.of();
            }
            for (String idToken : payload.substring(DRAG_PAYLOAD_MARKER.length()).split(",")) {
                Entity entity = game.getEntity(MathUtility.parseInt(idToken.trim(), Entity.NONE));
                if (entity != null) {
                    units.add(entity);
                }
            }
        } catch (UnsupportedFlavorException | IOException exception) {
            return List.of();
        }
        return units;
    }

    /**
     * The label for one network member, styled like the rulebook diagram: the role chip bold in its lance's color,
     * the free capacity muted and capped at the network's remaining room (a master's mounts may have links left
     * while the 12-unit network is full).
     */
    private String memberLabel(MemberRow row) {
        Entity entity = row.entity();
        String role = Messages.getString(row.roleKey());
        int masterCount = entity.getOperableC3MCount();
        if (!entity.hasNhC3() && (masterCount > 1)) {
            // Mirror the diagram's stacked MM/MMM letters: show the computer count in the chip
            role = role + " x" + masterCount;
        }
        String capacity = "";
        if (entity.isC3CompanyCommander()) {
            capacity = Messages.getString("C3NetworkManagerDialog.capacity",
                  Math.min(entity.calculateFreeC3Nodes(), row.networkRemaining()),
                  Math.min(entity.calculateFreeC3MNodes(), row.networkRemaining()));
        } else if (!entity.hasNhC3() && (entity.getOperableC3MCount() > 0)) {
            capacity = Messages.getString("C3NetworkManagerDialog.capacitySlaves",
                  Math.min(entity.calculateFreeC3Nodes(), row.networkRemaining()));
        }
        return "<html><b><font color='" + row.colorHex() + "'>[" + role + "]</font></b> "
              + entity.getShortNameRaw()
              + " <font color='" + MUTED_COLOR + "'>#" + entity.getId() + capacity + "</font></html>";
    }
}
