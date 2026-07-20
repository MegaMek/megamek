/*
 * Copyright (C) 2016-2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.randomArmy;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import megamek.client.Client;
import megamek.client.ratgenerator.CrewDescriptor;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.Ruleset;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.calculationReport.FlexibleCalculationReport;
import megamek.client.ui.panels.phaseDisplay.lobby.LobbyUtility;
import megamek.client.ui.tileset.MMStaticDirectoryManager;
import megamek.client.ui.util.UIUtil;
import megamek.common.Player;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.options.GameOptions;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import megamek.common.universe.Ranks;
import megamek.logging.MMLogger;

/**
 * Presents controls for selecting parameters of the force to generate and a tree structure showing the generated force.
 * The left and right sides of the view are made available separately for use by RandomArmyDialog.
 *
 * @author Neoancient
 */
public class ForceGeneratorViewUi implements ActionListener {
    private final static MMLogger logger = MMLogger.create(ForceGeneratorViewUi.class);

    private final JFrame parentFrame;
    
    private JPanel leftPanel;
    private JPanel rightPanel;

    private final ForceGeneratorOptionsView panControls;
    private JLabel lblOrganization;
    private JLabel lblFaction;
    private JLabel lblRating;
    private JScrollPane paneForceTree;
    private JTree forceTree;
    private JTextField txtSearch;
    private JLabel lblSearchStatus;
    private final List<TreePath> searchMatches = new ArrayList<>();
    private int searchIndex = -1;

    private JTable tblChosen;
    private ChosenEntityModel modelChosen;

    // When set by a host (e.g. MekHQ) that commits the preview tree into a TOE, the tree's right-click
    // menu offers Include/Exclude instead of "Add to game", and excluded nodes render struck out.
    private boolean toeExclusionMode = false;

    // When set by a host (e.g. MekHQ's Command Designer), each Generate appends its rolled force to an
    // accumulating Model root rather than replacing the tree, so the player can mix-and-match several
    // rolls into one command before committing. modelRoot holds the accumulated command.
    private boolean accumulateModel = false;
    // Thin wrapper root that always holds exactly one child: the current top command (modelTop). The
    // wrapper exists so the commit walker (which merges the passed root into the campaign's own
    // formation and flattens its children) preserves modelTop as a distinct formation - so a rolled
    // regiment keeps its "regiment" tag rather than dissolving into the campaign's top formation.
    private ForceDescriptor modelRoot;
    // The current top of the accumulated command; new rolls nest under it, replace it, or get a
    // synthesized parent, all by echelon (see accumulateIntoModel).
    private ForceDescriptor modelTop;
    // Number of generated commands the player has accumulated into the model, regardless of how they
    // nest. Reported in the status line; a plain counter because the model has no flat command list
    // once commands nest by echelon.
    private int modelCommandCount = 0;

    // Design-stage status line under the tree in accumulate mode: reassures the player the model is a
    // draft ("... - not yet committed.") and reports its running size. Hidden in standalone mode.
    private JLabel lblModelStatus;

    protected TableRowSorter<ChosenEntityModel> sorterChosen;

    static final String FGV_BV = "FGV_BV";
    static final String FGV_COST = "FGV_COST";
    static final String FGV_VIEW = "FGV_VIEW";

    protected static MekSummaryCache mscInstance = MekSummaryCache.getInstance();

    public ForceGeneratorViewUi(JFrame parentFrame, GameOptions gameOptions) {
        this.parentFrame = parentFrame;
        panControls = new ForceGeneratorOptionsView(this::setGeneratedForce, gameOptions);
        initUi();
    }

    private void initUi() {
        forceTree = new JTree(new ForceTreeModel(null));
        forceTree.setCellRenderer(new UnitRenderer());
        // JTree setRowHeight(0) the height for each row is determined by the renderer
        forceTree.setRowHeight(0);
        forceTree.setVisibleRowCount(12);
        forceTree.addTreeExpansionListener(new TreeExpansionListener() {
            @Override
            public void treeCollapsed(TreeExpansionEvent evt) {

            }

            @Override
            public void treeExpanded(TreeExpansionEvent evt) {
                if (forceTree.getPreferredSize().getWidth() > paneForceTree.getSize().getWidth()) {
                    rightPanel.setMinimumSize(
                          new Dimension(forceTree.getMinimumSize().width, rightPanel.getMinimumSize().height));
                    rightPanel.setPreferredSize(
                          new Dimension(forceTree.getPreferredSize().width, rightPanel.getPreferredSize().height));
                }
                rightPanel.revalidate();
            }
        });
        forceTree.addMouseListener(treeMouseListener);

        rightPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        rightPanel.add(new JLabel(Messages.getString("ForceGeneratorDialog.organization")), gbc);
        lblOrganization = new JLabel();
        gbc.gridx = 1;
        gbc.gridy = 0;
        rightPanel.add(lblOrganization, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        rightPanel.add(new JLabel(Messages.getString("ForceGeneratorDialog.faction")), gbc);
        lblFaction = new JLabel();
        gbc.gridx = 1;
        gbc.gridy = 1;
        rightPanel.add(lblFaction, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        rightPanel.add(new JLabel(Messages.getString("ForceGeneratorDialog.rating")), gbc);
        lblRating = new JLabel();
        gbc.gridx = 1;
        gbc.gridy = 2;
        rightPanel.add(lblRating, gbc);

        // ToE search bar: a live, non-destructive find that highlights and steps through nodes
        // whose unit name, pilot, ship name, or formation/cluster name matches the query.
        gbc.gridx = 0;
        gbc.gridy = 3;
        rightPanel.add(new JLabel(Messages.getString("ForceGeneratorDialog.search")), gbc);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        txtSearch = new JTextField(18);
        txtSearch.setToolTipText(Messages.getString("ForceGeneratorDialog.search.tooltip"));
        JButton btnSearchPrev = new JButton(Messages.getString("ForceGeneratorDialog.search.prev"));
        JButton btnSearchNext = new JButton(Messages.getString("ForceGeneratorDialog.search.next"));
        lblSearchStatus = new JLabel();
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearchPrev);
        searchPanel.add(btnSearchNext);
        searchPanel.add(lblSearchStatus);
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        rightPanel.add(searchPanel, gbc);
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                runToeSearch();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                runToeSearch();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                runToeSearch();
            }
        });
        // Enter in the field, and the buttons, step through matches.
        txtSearch.addActionListener(e -> gotoToeMatch(1));
        btnSearchNext.addActionListener(e -> gotoToeMatch(1));
        btnSearchPrev.addActionListener(e -> gotoToeMatch(-1));

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        paneForceTree = new JScrollPane();
        paneForceTree.setViewportView(forceTree);
        paneForceTree.setPreferredSize(new Dimension(600, 800));
        paneForceTree.setMinimumSize(new Dimension(600, 800));
        rightPanel.add(paneForceTree, gbc);

        // Design-stage status line beneath the tree. Present in every host but only made visible in
        // accumulate mode (see refreshCommandModelChrome); it stays hidden for standalone Random Army.
        gbc.gridy = 5;
        gbc.weighty = 0.0;
        lblModelStatus = new JLabel();
        lblModelStatus.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));
        lblModelStatus.setVisible(false);
        rightPanel.add(lblModelStatus, gbc);

        modelChosen = new ChosenEntityModel();
        tblChosen = new JTable(modelChosen);
        sorterChosen = new TableRowSorter<>(modelChosen);
        tblChosen.setRowSorter(sorterChosen);
        tblChosen.setIntercellSpacing(new Dimension(0, 0));
        tblChosen.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scroll = new JScrollPane(tblChosen);
        scroll.setBorder(BorderFactory.createTitledBorder(Messages.getString("RandomArmyDialog.Army")));
        tblChosen.addMouseListener(tableMouseListener);
        tblChosen.addKeyListener(tableKeyListener);

        leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.add(panControls);
        leftPanel.add(scroll);
    }

    public Component getLeftPanel() {
        return new JScrollPane(leftPanel);
    }

    public Component getRightPanel() {
        return rightPanel;
    }

    /**
     * The embedded options panel (inputs, transport, composition summary, and the Generate button).
     * Exposed so hosts (e.g. MekHQ's Force Generator tab) can seed the faction / year and read back
     * the user's selections without re-implementing the controls.
     */
    public ForceGeneratorOptionsView getOptionsView() {
        return panControls;
    }

    /**
     * Enables TOE exclusion mode for hosts that commit the preview tree into a table of organization.
     * In this mode the tree's right-click menu offers Include/Exclude (instead of "Add to game") and
     * excluded nodes are struck out in red. Defaults to {@code false} (the standalone Random Army
     * behavior).
     *
     * @param enabled {@code true} to enable the Include/Exclude menu and struck-out excluded nodes
     */
    public void setToeExclusionMode(boolean enabled) {
        this.toeExclusionMode = enabled;
    }

    /**
     * Enables Model-accumulation mode: each Generate appends its rolled force to an in-dialog Model
     * root rather than replacing the tree, so a host (e.g. MekHQ's Command Designer) can let the player
     * build one command from several rolls before committing. Defaults to {@code false} (standalone
     * Random Army replaces the tree on each Generate).
     *
     * @param enabled {@code true} to accumulate rolls into a Model
     */
    public void setAccumulateModel(boolean enabled) {
        this.accumulateModel = enabled;
        logger.info("[ForceGen] setAccumulateModel({})", enabled);
        refreshCommandModelChrome();
    }

    /**
     * Applies (or clears) the Command Designer's design-stage chrome around the tree. In accumulate
     * mode the tree gets a "Command Model (Design)" titled border - so it never reads as the live TOE -
     * and the status line under it shows either the empty-state hint or the running model size with a
     * "not yet committed" reminder. In standalone mode the border and status line are removed.
     */
    private void refreshCommandModelChrome() {
        if (paneForceTree == null || lblModelStatus == null) {
            return;
        }
        if (!accumulateModel) {
            paneForceTree.setBorder(null);
            lblModelStatus.setVisible(false);
            return;
        }
        paneForceTree.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("ForceGeneratorDialog.commandModel.title")));
        if (modelRoot == null) {
            lblModelStatus.setText(Messages.getString("ForceGeneratorDialog.commandModel.empty"));
        } else {
            int unitCount = countModelUnits(modelRoot);
            lblModelStatus.setText(Messages.getString("ForceGeneratorDialog.commandModel.status",
                  unitCount, modelCommandCount));
        }
        lblModelStatus.setVisible(true);
    }

    /**
     * Counts the included combat-unit leaves under {@code descriptor} - the units that will actually be
     * committed. A leaf counts only when it is {@link ForceDescriptor#isIncluded() included} and has an
     * {@link ForceDescriptor#getEntity() entity}, so struck-out (excluded) units are not tallied.
     *
     * @param descriptor the model (or subtree) to count
     *
     * @return the number of included combat-unit leaves
     */
    private int countModelUnits(ForceDescriptor descriptor) {
        boolean hasChildren = !descriptor.getSubForces().isEmpty() || !descriptor.getAttached().isEmpty();
        if (!hasChildren) {
            return (descriptor.isIncluded() && descriptor.getEntity() != null) ? 1 : 0;
        }
        int count = 0;
        for (ForceDescriptor child : descriptor.getSubForces()) {
            count += countModelUnits(child);
        }
        for (ForceDescriptor child : descriptor.getAttached()) {
            count += countModelUnits(child);
        }
        return count;
    }

    /** The accumulated Model root in accumulate mode, or {@code null} if nothing has been rolled yet. */
    public ForceDescriptor getModelRoot() {
        return modelRoot;
    }

    /**
     * The force rolled by the most recent Generate, or {@code null} if nothing has been generated yet.
     * The tree root holds the rolled {@link ForceDescriptor}; hosts can commit exactly what the player
     * previewed.
     */
    public ForceDescriptor getGeneratedForce() {
        Object root = (forceTree == null) ? null : forceTree.getModel().getRoot();
        return (root instanceof ForceDescriptor fd) ? fd : null;
    }

    public void setYear(int year) {
        panControls.setCurrentYear(year);
    }

    public List<Entity> getChosenUnits() {
        return Collections.unmodifiableList(modelChosen.allEntities());
    }

    /**
     * Adds the chosen units to the game
     */
    public void addChosenUnits(String playerName, ClientGUI clientGui) {
        if ((null != forceTree.getModel().getRoot())
              && (forceTree.getModel().getRoot() instanceof ForceDescriptor)) {
            configureNetworks((ForceDescriptor) forceTree.getModel().getRoot());
        }

        List<Entity> entities = new ArrayList<>(modelChosen.allEntities().size());
        Client c = null;
        if (null != playerName) {
            c = (Client) clientGui.getLocalBots().get(playerName);
        }
        if (null == c) {
            c = clientGui.getClient();
        }
        for (Entity e : modelChosen.allEntities()) {
            e.setOwner(c.getLocalPlayer());
            if (!c.getGame().getPhase().isLounge()) {
                e.setDeployRound(c.getGame().getRoundCount() + 1);
                e.setGame(c.getGame());
                // Set these to true, otherwise units reinforced in
                // the movement turn are considered selectable
                e.setDone(true);
                e.setUnloaded(true);
            }
            if (e.getForceString().isBlank()) {
                logger.warn("[ForceGen][ToE] add-to-game '{}' has a BLANK force string; ToE structure will be lost",
                      e.getShortName());
            } else {
                logger.debug("[ForceGen][ToE] add-to-game '{}' forceString='{}'", e.getShortName(),
                      e.getForceString());
            }
            entities.add(e);
        }
        c.sendAddEntity(entities);

        String msg = clientGui.getClient().getLocalPlayer() + " loaded Units from Random Army for player: " + playerName
              + " [" + entities.size() + " units]";
        clientGui.getClient().sendServerChat(Player.PLAYER_NONE, msg);

        modelChosen.clearData();
    }

    private void configureNetworks(ForceDescriptor fd) {
        if (fd.getFlags().contains("c3")) {
            Entity master = fd.getSubForces().stream().map(ForceDescriptor::getEntity)
                  .filter(en -> modelChosen.hasEntity(en)
                        && (en.hasC3M() || en.hasC3MM()))
                  .findFirst().orElse(null);
            if (null != master) {
                master.setC3UUID();
                int c3s = 0;
                for (ForceDescriptor sf : fd.getSubForces()) {
                    if (modelChosen.hasEntity(sf.getEntity())
                          && !sf.getEntity().getExternalIdAsString().equals(master.getExternalIdAsString())
                          && sf.getEntity().hasC3S()) {
                        sf.getEntity().setC3UUID();
                        sf.getEntity().setC3MasterIsUUIDAsString(master.getC3UUIDAsString());
                        c3s++;
                        if (c3s == 3) {
                            break;
                        }
                    }
                }
            }
        } else {
            // Even if we haven't reworked this into a full C3i network, we can still
            // connect
            // any C3i units that happen to be present.
            String netId = null;
            int nodes = 0;
            for (ForceDescriptor sf : fd.getSubForces()) {
                if (modelChosen.hasEntity(sf.getEntity())
                      && sf.getEntity().hasC3i()) {
                    sf.getEntity().setC3UUID();
                    if (null == netId) {
                        netId = sf.getEntity().getC3UUIDAsString();
                        nodes++;
                    } else {
                        int pos = sf.getEntity().getFreeC3iUUID();
                        if (pos >= 0) {
                            sf.getEntity().setC3iNextUUIDAsString(pos, netId);
                            nodes++;
                        }
                    }
                }
                if (nodes >= Entity.MAX_C3i_NODES) {
                    break;
                }
            }
        }
        fd.getSubForces().forEach(this::configureNetworks);
        fd.getAttached().forEach(this::configureNetworks);
    }

    private void setGeneratedForce(ForceDescriptor fd) {
        // In accumulate mode each roll is appended to the Model root and the tree shows the whole
        // accumulating command; otherwise the roll replaces the tree (standalone behavior).
        ForceDescriptor displayRoot = fd;
        if (accumulateModel && fd != null) {
            if (modelRoot == null) {
                modelRoot = new ForceDescriptor();
                modelRoot.setName("Command Model");
            }
            modelTop = accumulateIntoModel(modelTop, fd);
            // Keep the wrapper holding exactly the current top command.
            modelRoot.getSubForces().clear();
            modelRoot.addSubForce(modelTop);
            modelCommandCount++;
            displayRoot = modelRoot;
            logger.info("[ForceGen] accumulated roll id={} (echelon={}) into Model; model top='{}' echelon={}, {} command(s) total",
                  System.identityHashCode(fd), fd.getEchelon(), modelTop.parseName(),
                  modelTop.getEchelon(), modelCommandCount);
            logModelTree(modelTop, 0);
        } else {
            logger.info("[ForceGen] setGeneratedForce (accumulate={}, fd={}) - replacing tree",
                  accumulateModel, fd != null);
        }
        forceTree.setModel(new ForceTreeModel(displayRoot));
        // A new force invalidates the previous search; clearing the field re-runs the (now empty)
        // search via the document listener, resetting the match list and status.
        if (txtSearch != null) {
            txtSearch.setText("");
        }

        if (null != fd) {
            lblOrganization.setText(Ruleset.findRuleset(fd).getEschelonNames(fd.getUnitType() == null
                  ? ""
                  : UnitType.getTypeName(fd.getUnitType())).get(fd.getEchelonCode()));
            lblFaction.setText(RATGenerator.getInstance().getFaction(fd.getFaction()).getName(fd.getYear()));
            lblRating.setText(SkillLevel.values()[fd.getExperience() + SkillLevel.GREEN.ordinal()].toString()
                  + ((fd.getRating() == null) ? "" : "/" + fd.getRating()));
        } else {
            lblOrganization.setText("");
            lblFaction.setText("");
            lblRating.setText("");
        }

        // Update the design-stage status line for the model's new size.
        refreshCommandModelChrome();
    }

    /**
     * Folds a freshly rolled command into the running model by echelon, so the model reads as one
     * command structure rather than a flat pile of rolls:
     *
     * <ul>
     *   <li><b>Smaller than the current top</b> (lower echelon) - tucked under the current top command
     *       (for example a Battle Armor company generated after a regiment nests inside that
     *       regiment).</li>
     *   <li><b>Larger than the current top</b> (higher echelon) - becomes the new top and the previous
     *       top nests inside it.</li>
     *   <li><b>Same echelon</b> - a synthetic parent one echelon up is created and both peers nest
     *       inside it (for example two regiments end up under a synthesized brigade).</li>
     * </ul>
     *
     * @param currentTop the current model top, or {@code null} if this is the first roll
     * @param fd         the newly rolled command
     *
     * @return the model top after folding {@code fd} in
     */
    private ForceDescriptor accumulateIntoModel(ForceDescriptor currentTop, ForceDescriptor fd) {
        if (currentTop == null) {
            return fd;
        }
        int topEchelon = echelonOf(currentTop);
        int newEchelon = echelonOf(fd);
        if (newEchelon < topEchelon) {
            currentTop.addSubForce(fd);
            return currentTop;
        }
        if (newEchelon > topEchelon) {
            fd.addSubForce(currentTop);
            return fd;
        }
        ForceDescriptor parent = synthesizeParentCommand(currentTop, topEchelon + 1);
        parent.addSubForce(currentTop);
        parent.addSubForce(fd);
        return parent;
    }

    /** The descriptor's echelon, treating a {@code null} echelon as 0 (the smallest) for comparison. */
    private int echelonOf(ForceDescriptor descriptor) {
        Integer echelon = descriptor.getEchelon();
        return (echelon == null) ? 0 : echelon;
    }

    /**
     * Builds an empty container command one echelon above two same-sized peers (for example a brigade
     * over two regiments). The container borrows the child's faction, unit type, and year so the
     * ruleset can resolve the correct echelon name for the campaign's faction; if the ruleset has no
     * name for that echelon, a generic "Command" label is used.
     *
     * @param child   a command being placed under the new container, used for faction/context
     * @param echelon the echelon for the new container (one above the peers)
     *
     * @return the synthesized parent command
     */
    private ForceDescriptor synthesizeParentCommand(ForceDescriptor child, int echelon) {
        ForceDescriptor parent = new ForceDescriptor();
        parent.setEchelon(echelon);
        parent.setFaction(child.getFaction());
        parent.setUnitType(child.getUnitType());
        parent.setYear(child.getYear());
        String echelonName = Ruleset.findRuleset(parent).getEschelonName(parent);
        parent.setName((echelonName == null || echelonName.isBlank()) ? "Command" : echelonName);
        return parent;
    }

    /**
     * Logs the model's command structure to depth 1 (the top command and its direct children) for
     * diagnostics. Bounded on purpose so a large model does not flood the log with every leaf unit.
     *
     * @param node  the model node to log
     * @param depth the current depth; recursion stops after depth 1
     */
    private void logModelTree(ForceDescriptor node, int depth) {
        logger.info("[ForceGen]   {}id={} name='{}' echelon={} unitType={} desc='{}'",
              "  ".repeat(depth), System.identityHashCode(node), node.parseName(),
              node.getEchelon(), node.getUnitType(), node.getDescription());
        if (depth >= 1) {
            return;
        }
        for (ForceDescriptor child : node.getSubForces()) {
            logModelTree(child, depth + 1);
        }
    }

    /**
     * Runs the order-of-battle search against the current field text and jumps to the first match. Matches are
     * case-insensitive substring hits on each node's unit name/chassis/model, pilot name, ship fluff name, and
     * formation/cluster name. Non-destructive: the tree is only expanded and selected, never rebuilt or filtered.
     */
    private void runToeSearch() {
        searchMatches.clear();
        searchIndex = -1;
        String query = txtSearch.getText().trim().toLowerCase();
        Object root = forceTree.getModel().getRoot();
        if (!query.isEmpty() && (root instanceof ForceDescriptor rootForce)) {
            collectToeMatches(rootForce, new ArrayList<>(), query, searchMatches);
        }
        if (searchMatches.isEmpty()) {
            forceTree.clearSelection();
            lblSearchStatus.setText(query.isEmpty()
                  ? ""
                  : Messages.getString("ForceGeneratorDialog.search.noMatches"));
        } else {
            gotoToeMatch(1);
        }
    }

    /** Depth-first walk that records the {@link TreePath} of every node matching {@code query}. */
    private void collectToeMatches(ForceDescriptor node, List<Object> ancestors, String query,
          List<TreePath> out) {
        List<Object> path = new ArrayList<>(ancestors);
        path.add(node);
        if (matchesToeQuery(node, query)) {
            out.add(new TreePath(path.toArray()));
        }
        for (Object child : node.getAllChildren()) {
            if (child instanceof ForceDescriptor childForce) {
                collectToeMatches(childForce, path, query, out);
            }
        }
    }

    /** True when {@code query} (already lower-case) is a substring of any of the node's display text. */
    private boolean matchesToeQuery(ForceDescriptor fd, String query) {
        StringBuilder haystack = new StringBuilder();
        appendSearchable(haystack, fd.parseName());
        appendSearchable(haystack, fd.getDescription());
        appendSearchable(haystack, fd.getFluffName());
        appendSearchable(haystack, fd.getModelName());
        if (fd.getCo() != null) {
            appendSearchable(haystack, fd.getCo().getName());
        }
        if (fd.getXo() != null) {
            appendSearchable(haystack, fd.getXo().getName());
        }
        Entity en = fd.getEntity();
        if (en != null) {
            appendSearchable(haystack, en.getShortName());
            appendSearchable(haystack, en.getChassis());
            appendSearchable(haystack, en.getModel());
        }
        return haystack.toString().toLowerCase().contains(query);
    }

    private static void appendSearchable(StringBuilder haystack, String value) {
        if ((value != null) && !value.isBlank()) {
            haystack.append(value).append(' ');
        }
    }

    /**
     * Steps the selection to the next ({@code delta > 0}) or previous ({@code delta < 0}) match, wrapping around,
     * scrolls it into view, and updates the "k / N" status. No-op with no matches.
     */
    private void gotoToeMatch(int delta) {
        if (searchMatches.isEmpty()) {
            return;
        }
        int size = searchMatches.size();
        searchIndex = (((searchIndex + delta) % size) + size) % size;
        TreePath path = searchMatches.get(searchIndex);
        forceTree.setSelectionPath(path);
        forceTree.scrollPathToVisible(path);
        lblSearchStatus.setText((searchIndex + 1) + " / " + size);
    }

    private final MouseListener treeMouseListener = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent evt) {
            showPopup(evt);
        }

        @Override
        public void mouseReleased(MouseEvent evt) {
            showPopup(evt);
        }

        private void showPopup(MouseEvent evt) {
            if (evt.isPopupTrigger()) {
                TreePath path = forceTree.getPathForLocation(evt.getX(), evt.getY());
                if (path == null) {
                    return;
                }
                Object node = path.getLastPathComponent();
                if (node instanceof ForceDescriptor fd) {
                    JPopupMenu menu = new JPopupMenu();

                    // Include/exclude is available in both hosts. MekHQ skips excluded nodes when it
                    // commits the tree into a TOE; standalone Random Army skips them in "Add to game"
                    // (see ChosenEntityModel.addEntities). The wording follows the host.
                    String target = toeExclusionMode ? "TOE" : "force";
                    String toggleText = fd.isIncluded() ? "Exclude from " + target : "Include in " + target;
                    JMenuItem toggleItem = new JMenuItem(toggleText);
                    toggleItem.addActionListener(ev -> {
                        fd.setIncludedRecursively(!fd.isIncluded());
                        forceTree.repaint();
                        // The status line counts included units, so re-tally after a toggle.
                        refreshCommandModelChrome();
                    });
                    menu.add(toggleItem);

                    if (!toeExclusionMode) {
                        JMenuItem addItem = new JMenuItem("Add to game");
                        addItem.addActionListener(ev -> modelChosen.addEntities(fd));
                        menu.add(addItem);
                    }

                    JMenuItem exportItem = new JMenuItem("Export as MUL");
                    exportItem.addActionListener(ev -> panControls.exportMUL(fd));
                    menu.add(exportItem);
                    menu.show(evt.getComponent(), evt.getX(), evt.getY());
                }
            }
        }
    };

    private final MouseListener tableMouseListener = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent evt) {
            showPopup(evt);
        }

        @Override
        public void mouseReleased(MouseEvent evt) {
            showPopup(evt);
        }

        private void showPopup(MouseEvent evt) {
            if (evt.isPopupTrigger()) {
                if (tblChosen.getSelectedRowCount() > 0) {
                    JPopupMenu menu = new JPopupMenu();

                    List<Integer> entities = LobbyUtility.getSelectedEntities(tblChosen);
                    int[] entityIDs = entities.stream().mapToInt(Integer::intValue).toArray();

                    JMenuItem item = new JMenuItem("Remove");
                    item.addActionListener(ev -> modelChosen.removeEntities(entityIDs));
                    menu.add(item);

                    // All command strings should follow the layout COMMAND|INFO|ID1,ID2,I3...
                    // and use -1 when something is not needed (COMMAND|-1|-1)
                    String eIds = LobbyUtility.enToken(entities);

                    String msg_view = Messages.getString("RandomArmyDialog.View");
                    String msgViewBV = Messages.getString("RandomArmyDialog.ViewBV");
                    String msgViewCost = Messages.getString("RandomArmyDialog.ViewCost");

                    menu.add(
                          UIUtil.menuItem(msg_view, FGV_VIEW + eIds, true, ForceGeneratorViewUi.this, KeyEvent.VK_V));
                    menu.add(
                          UIUtil.menuItem(msgViewBV, FGV_BV + eIds, true, ForceGeneratorViewUi.this, KeyEvent.VK_B));
                    menu.add(UIUtil.menuItem(msgViewCost, FGV_COST + eIds, true, ForceGeneratorViewUi.this,
                          Integer.MIN_VALUE));

                    menu.show(evt.getComponent(), evt.getX(), evt.getY());
                }
            }
        }
    };

    @Override
    public void actionPerformed(ActionEvent ev) {
        StringTokenizer st = new StringTokenizer(ev.getActionCommand(), "|");
        String command = "";

        if (st.hasMoreTokens()) {
            command = st.nextToken();
        }

        switch (command) {
            case FGV_VIEW -> {
                // The entities list may be empty
                Set<Entity> entities = LobbyUtility.getEntities(st.nextToken(), modelChosen);
                LobbyUtility.mekReadoutAction(entities, true, true, parentFrame);
            }
            case FGV_BV -> {
                // The entities list may be empty
                Set<Entity> entities = LobbyUtility.getEntities(st.nextToken(), modelChosen);
                LobbyUtility.mekBVAction(entities, true, true, parentFrame);
            }
            case FGV_COST -> {
                // The entities list may be empty
                Set<Entity> entities = LobbyUtility.getEntities(st.nextToken(), modelChosen);
                LobbyUtility.mekCostAction(entities, true, true, parentFrame);
            }
        }
    }

    private final KeyListener tableKeyListener = new KeyListener() {
        @Override
        public void keyTyped(KeyEvent evt) {

        }

        @Override
        public void keyPressed(KeyEvent evt) {

        }

        @Override
        public void keyReleased(KeyEvent evt) {
            if ((evt.getKeyCode() == KeyEvent.VK_DELETE) && (tblChosen.getSelectedRowCount() > 0)) {
                modelChosen.removeEntities(tblChosen.getSelectedRows());
            }
        }
    };

    static class ForceTreeModel implements TreeModel {
        private final ForceDescriptor root;
        private final ArrayList<TreeModelListener> listeners;

        public ForceTreeModel(ForceDescriptor root) {
            this.root = root;
            listeners = new ArrayList<>();
        }

        @Override
        public void addTreeModelListener(TreeModelListener listener) {
            if (null != listener && !listeners.contains(listener)) {
                listeners.add(listener);
            }
        }

        @Override
        public Object getChild(Object parent, int index) {
            if (parent instanceof ForceDescriptor forceDescriptor) {
                return forceDescriptor.getAllChildren().get(index);
            }
            return null;
        }

        @Override
        public int getChildCount(Object parent) {
            if (parent instanceof ForceDescriptor forceDescriptor) {
                return forceDescriptor.getAllChildren().size();
            }
            return 0;
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            if (parent instanceof ForceDescriptor forceDescriptor) {
                return forceDescriptor.getAllChildren().indexOf(child);
            }
            return 0;
        }

        @Override
        public Object getRoot() {
            return root;
        }

        @Override
        public boolean isLeaf(Object node) {
            return (getChildCount(node) == 0)
                  || ((node instanceof ForceDescriptor forceDescriptor)
                  && (forceDescriptor.getEchelon() != null)
                  && (forceDescriptor.getEchelon() == 0));
        }

        @Override
        public void removeTreeModelListener(TreeModelListener listener) {
            if (null != listener) {
                listeners.remove(listener);
            }
        }

        @Override
        public void valueForPathChanged(TreePath arg0, Object arg1) {

        }
    }

    private static class UnitRenderer extends DefaultTreeCellRenderer {
        // HTML color for nodes the user has excluded from the TOE (rendered struck out).
        private static final String EXCLUDED_COLOR_HTML = "#C84B4B";

        // Fallback rank-int -> short title used when the ruleset XML did not set an explicit
        // title= attribute on the <co>/<xo> element (the typical case — mm-data only sets title
        // for special honorifics like "Aide" or "ovKhan"). Values match the integer constants in
        // mm-data/data/forcegenerator/faction_rules/constants.txt. Where IS and Clan share the
        // same int the IS officer title is preferred since rulesets that need Clan/CS variants
        // already populate title= explicitly.
        private static final Map<Integer, String> DEFAULT_RANK_TITLES = Map.ofEntries(
              Map.entry(12, "Sergeant"),
              Map.entry(32, "Lieutenant JG"),
              Map.entry(33, "Lieutenant"),
              Map.entry(34, "Captain"),
              Map.entry(35, "Major"),
              Map.entry(37, "Lt. Colonel"),
              Map.entry(38, "Colonel"),
              Map.entry(39, "Lt. General"),
              Map.entry(42, "Maj. General"),
              Map.entry(43, "General"),
              Map.entry(46, "Loremaster"),
              Map.entry(47, "saKhan"),
              Map.entry(48, "Khan"));

        public UnitRenderer() {

        }

        /**
         * Builds the "Captain " / "CO: " prefix that precedes a commander's name in the tree. Resolution order:
         * <ol>
         *   <li>An explicit {@code title=} attribute from the ruleset XML (honorifics like "Aide", "ovKhan").</li>
         *   <li>The faction-specific rank from {@code data/universe/ranks.xml} (e.g. "Tai-i" for DCMS, "Star Captain"
         *       for CLAN) — looked up using the ratgen rank-system integer the ruleset assigned to this force.</li>
         *   <li>A generic IS-leaning rank-int → title map as a safety net if {@code ranks.xml} is unavailable.</li>
         *   <li>The {@code "CO: "} / {@code "XO: "} role marker as a last resort.</li>
         * </ol>
         */
        private static String commanderPrefix(CrewDescriptor crew, String roleFallback) {
            String title = crew.getTitle();
            if (title != null && !title.isBlank()) {
                return title.endsWith(" ") ? title : title + " ";
            }
            Integer rankSystemIndex = (crew.getAssignment() == null)
                  ? null : crew.getAssignment().getRankSystem();
            String factionRankName = Ranks.getInstance()
                  .resolveRankName(rankSystemIndex, crew.getRank())
                  .orElse(null);
            if (factionRankName != null && !factionRankName.isBlank()) {
                return factionRankName + " ";
            }
            String rankName = DEFAULT_RANK_TITLES.get(crew.getRank());
            if (rankName != null) {
                return rankName + " ";
            }
            return roleFallback + ": ";
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
              boolean expanded, boolean leaf, int row,
              boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            setBackground(UIManager.getColor("Tree.textBackground"));
            setForeground(UIManager.getColor("Tree.textForeground"));
            if (sel) {
                setBackground(UIManager.getColor("Tree.selectionBackground"));
                setForeground(UIManager.getColor("Tree.selectionForeground"));
            }

            ForceDescriptor fd = (ForceDescriptor) value;
            if (fd.isElement()) {
                String commander;
                if (fd.getCo() == null) {
                    commander = "<font color='red'>"
                          + Messages.getString("ForceGeneratorDialog.noCrew") + "</font>";
                } else {
                    commander = fd.getCo().getName()
                          + " (" + fd.getCo().getGunnery() + "/" + fd.getCo().getPiloting() + ")";
                }
                Entity en = fd.getEntity();
                if ((en != null) && en.isLargeCraft()) {
                    // Large craft (WarShip, DropShip, JumpShip, Space Station) read better
                    // ship-first, the way a fleet roster is listed: ship name and class on
                    // the top line, commander (skill) beneath.
                    String shipClass = "<i>" + en.getChassis() + "</i>";
                    String shipName = fd.getFluffName();
                    String topLine = ((shipName != null) && !shipName.isBlank())
                          ? "<b>" + shipName + "</b>, " + shipClass
                          : shipClass;
                    setText("<html>" + topLine + "<br />" + commander + "</html>");
                } else {
                    String uname = "<i>" + fd.getModelName() + "</i>";
                    if (fd.getFluffName() != null) {
                        uname += "<br /><i>" + fd.getFluffName() + "</i>";
                    }
                    setText("<html>" + commander + ", " + uname + "</html>");
                }
                if (fd.getEntity() != null) {
                    try {
                        setIcon(new ImageIcon(MMStaticDirectoryManager.getMekTileset().imageFor(fd.getEntity())));
                    } catch (NullPointerException ex) {
                        logger.warn("No image found for {}", fd.getEntity().getShortNameRaw());
                    }
                }
            } else {
                StringBuilder desc = new StringBuilder("<html>");
                String parsedName = fd.parseName();
                String description = fd.getDescription();
                boolean hasName = parsedName != null && !parsedName.isBlank();
                boolean hasDescription = description != null && !description.isBlank();
                // Collapse "A Company" + "Heavy Mek Company" onto one row as
                // "<b>A Company</b> (Heavy Mek Company)". Formation name is bolded so it pops at
                // a glance when scrolling a battalion-sized tree; the descriptor (weight + unit
                // type + role) is italicized to read as a supplementary label. When only one
                // side is populated, it is rendered bold as the row's primary identifier.
                if (hasName && hasDescription) {
                    desc.append("<b>").append(parsedName).append("</b>")
                          .append(" <i>(").append(description).append(")</i>");
                } else if (hasName) {
                    desc.append("<b>").append(parsedName).append("</b>");
                } else if (hasDescription) {
                    desc.append("<b>").append(description).append("</b>");
                }
                if (fd.getCo() != null) {
                    desc.append("<br />").append(commanderPrefix(fd.getCo(), "CO"));
                    desc.append(fd.getCo().getName());
                }
                if (fd.getXo() != null) {
                    desc.append("<br />").append(commanderPrefix(fd.getXo(), "XO"));
                    desc.append(fd.getXo().getName());
                }
                setText(desc.append("</html>").toString());
            }

            // Excluded nodes: strike out the whole label in red so it's clear it won't be committed.
            if (!fd.isIncluded()) {
                String current = getText();
                if (current != null && current.startsWith("<html>") && current.endsWith("</html>")) {
                    String inner = current.substring("<html>".length(), current.length() - "</html>".length());
                    setText("<html><strike><font color='" + EXCLUDED_COLOR_HTML + "'>"
                          + inner + "</font></strike></html>");
                }
            }
            return this;
        }
    }

    public static class ChosenEntityModel extends AbstractTableModel {
        public static final int COL_ENTITY = 0;
        public static final int COL_BV = 1;
        public static final int COL_MOVE = 2;
        private static final int COL_TECH_BASE = 3;
        private static final int COL_UNIT_ROLE = 4;
        public static final int NUM_COLS = 5;

        private List<Entity> entities = new ArrayList<>();
        private final Set<String> entityIds = new HashSet<>();

        public boolean hasEntity(final @Nullable Entity en) {
            return (en != null) && entityIds.contains(en.getExternalIdAsString());
        }

        public void addEntity(Entity en) {
            if (!entityIds.contains(en.getExternalIdAsString())) {
                entities.add(en);
                entityIds.add(en.getExternalIdAsString());
            }
            fireTableDataChanged();
        }

        public void clearData() {
            entityIds.clear();
            entities.clear();
            fireTableDataChanged();
        }

        public void removeEntities(int... selectedRows) {
            for (int r : selectedRows) {
                if ((r >= 0) && (r < entities.size())) {
                    entityIds.remove(entities.get(r).getExternalIdAsString());
                }
            }
            entities = entities.stream().filter(e -> entityIds.contains(e.getExternalIdAsString()))
                  .collect(Collectors.toList());
            fireTableDataChanged();
        }

        public void addEntities(ForceDescriptor fd) {
            // Skip nodes the user excluded in the tree (and their subtree), so "Add to game" adds only
            // the included units.
            if (!fd.isIncluded()) {
                return;
            }
            if (fd.isElement()) {
                if (fd.getEntity() != null) {
                    addEntity(fd.getEntity());
                }
            }
            fd.getSubForces().forEach(this::addEntities);
            fd.getAttached().forEach(this::addEntities);
        }

        public List<Entity> allEntities() {
            return entities;
        }

        @Override
        public int getRowCount() {
            return entities.size();
        }

        @Override
        public int getColumnCount() {
            return NUM_COLS;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            final Entity en = entities.get(rowIndex);
            switch (columnIndex) {
                case COL_ENTITY:
                    return en.getShortNameRaw();
                case COL_BV:
                    return en.calculateBattleValue();
                case COL_MOVE:
                    return en.getWalkMP() + "/" + en.getRunMPasString() + "/" + en.getAnyTypeMaxJumpMP();
                case COL_TECH_BASE:
                    return en.getTechBaseDescription();
                case COL_UNIT_ROLE:
                    FlexibleCalculationReport report = new FlexibleCalculationReport();
                    AlphaStrikeElement element = ASConverter.convert(en, false, report);
                    return element.getRole();
                default:
                    return "";
            }
        }

        @Override
        public String getColumnName(int column) {
            return switch (column) {
                case COL_ENTITY -> Messages.getString("RandomArmyDialog.colUnit");
                case COL_MOVE -> Messages.getString("RandomArmyDialog.colMove");
                case COL_BV -> Messages.getString("RandomArmyDialog.colBV");
                case COL_TECH_BASE -> Messages.getString("RandomArmyDialog.colTechBase");
                case COL_UNIT_ROLE -> Messages.getString("RandomArmyDialog.colUnitRole");
                default -> "??";
            };
        }

        public MekSummary getUnitAt(int row) {
            Entity e = entities.get(row);

            return mscInstance.getMek(e.getShortNameRaw());
        }
    }
}
