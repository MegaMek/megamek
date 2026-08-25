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

import java.awt.BorderLayout;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
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
import megamek.client.ratgenerator.C3NetworkConfigurator;
import megamek.client.ratgenerator.CrewDescriptor;
import megamek.client.ratgenerator.ForceDescriptor;
import megamek.client.ratgenerator.FormationType;
import megamek.client.ratgenerator.GenerationContext;
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

    // The chosen-units table below the controls. Standalone Random Army collects units to add to a running game
    // here; a host that commits the preview tree never reads it, so it is hidden for them.
    private JScrollPane chosenUnitsPane;

    // When set by a host (e.g. MekHQ) that commits the preview tree into a TOE, the tree's right-click
    // menu offers Include/Exclude instead of "Add to game", and excluded nodes render struck out.
    private boolean toeExclusionMode = false;

    // Optional host-supplied display names for formation (non-unit) tree nodes, so the preview can show
    // the names the committed TOE will actually use. Null (or a null/blank result for a node) falls back
    // to the descriptor's own parseName().
    private Function<ForceDescriptor, String> formationNameProvider = null;

    // Notified after anything that changes what a commit would produce: a Generate (including an
    // accumulated roll) and an Include/Exclude toggle. Hosts use this to invalidate cached previews.
    private Runnable toeChangeListener = null;

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
        // The lower slot holds the chosen-units table for standalone Random Army, and the formation mix editor for a
        // host that commits the tree instead and so never reads that table. Both live in the panel; one is shown.
        chosenUnitsPane = scroll;
        leftPanel.add(chosenUnitsPane);
    }

    /**
     * Swaps the lower panel from the chosen-units table to an inline formation mix editor.
     *
     * <p>For a host that commits the preview tree rather than collecting units to add to a running game, the
     * chosen-units table below the controls is inert - nothing reads it - so the space is better spent on the mix,
     * which that host has room to show inline rather than behind a button.</p>
     *
     * @param visible {@code true} to show the mix editor in place of the chosen-units table
     */
    public void setFormationMixEditorVisible(boolean visible) {
        chosenUnitsPane.setVisible(!visible);
        // The editor lives in the options panel itself, above Transport and the Composition Summary, so it sits with
        // the settings that shape the force rather than below the ones that describe it.
        panControls.setFormationMixInline(visible);
        leftPanel.revalidate();
        leftPanel.repaint();
    }

    /**
     * Rebuilds the inline mix editor for the force the options currently describe, keeping whatever the player has
     * already asked for. Cheap enough to call whenever those options change: it builds the force's structure and
     * stops before drawing a unit.
     */
    public void refreshFormationMixEditor() {
        panControls.refreshInlineFormationMixEditor();
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
     * Points the generator at the options of the game the force is being generated for.
     *
     * @param gameOptions the options of the game being generated for
     */
    public void setGameOptions(GameOptions gameOptions) {
        panControls.setGameOptions(gameOptions);
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
     * Supplies display names for the formation (non-unit) nodes of the preview tree, overriding each
     * descriptor's own {@link ForceDescriptor#parseName()}. Hosts that rename formations on commit (for
     * example MekHQ's Command Designer callsigns) use this so the preview shows the final TOE names. A
     * {@code null} provider - or a {@code null}/blank result for a node - falls back to
     * {@code parseName()}.
     *
     * @param provider maps a formation descriptor to its display name, or {@code null} to disable
     */
    public void setFormationNameProvider(@Nullable Function<ForceDescriptor, String> provider) {
        this.formationNameProvider = provider;
        if (forceTree != null) {
            forceTree.repaint();
        }
    }

    /**
     * Registers a listener notified after anything that changes what a commit would produce: each
     * Generate (including rolls accumulated into the Model) and each Include/Exclude toggle. Hosts use
     * this to invalidate caches backing {@link #setFormationNameProvider}.
     *
     * @param listener the callback, or {@code null} to clear
     */
    public void setToeChangeListener(@Nullable Runnable listener) {
        this.toeChangeListener = listener;
    }

    /**
     * Repaints the force tree so display-affecting host state (for example a changed
     * {@link #setFormationNameProvider} backing) is re-rendered without a structural change.
     */
    public void repaintForceTree() {
        if (forceTree != null) {
            forceTree.repaint();
        }
    }

    /** Fires the TOE-change listener, if any. */
    private void fireToeChanged() {
        if (toeChangeListener != null) {
            toeChangeListener.run();
        }
    }

    /**
     * The display name for a formation node: the host-supplied provider's answer when one is set and
     * returns a usable name, otherwise the descriptor's own {@link ForceDescriptor#parseName()}.
     */
    private String resolveFormationName(ForceDescriptor descriptor) {
        if (formationNameProvider != null) {
            String provided = formationNameProvider.apply(descriptor);
            if (provided != null && !provided.isBlank()) {
                return provided;
            }
        }
        return descriptor.parseName();
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
            // Only the units the user actually took are wired; the rest of the model is not going
            // into the game.
            C3NetworkConfigurator.configure((ForceDescriptor) forceTree.getModel().getRoot(),
                  modelChosen::hasEntity);
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


    /**
     * @return what the generated force was rolled for - faction, command, year and rating - or
     *       {@code null} when nothing has been generated yet
     */
    public @Nullable GenerationContext getGenerationContext() {
        if (!(forceTree.getModel().getRoot() instanceof ForceDescriptor root)) {
            return null;
        }
        return new GenerationContext(root.getFaction(), root.getYear(), root.getRating(),
              GenerationContext.Source.FORCE_GENERATOR);
    }

    private void setGeneratedForce(ForceDescriptor fd) {
        // A null descriptor means Clear Force. The accumulated Command Model has to go with the tree:
        // leaving it behind lets the next Generate fold the new roll into the command the player just
        // cleared, so the cleared units reappear alongside the newly generated ones.
        if (fd == null) {
            clearAccumulatedModel();
        }
        // In accumulate mode each roll is appended to the Model root and the tree shows the whole
        // accumulating command; otherwise the roll replaces the tree (standalone behavior).
        ForceDescriptor displayRoot = fd;
        if (accumulateModel && fd != null) {
            if (modelRoot == null) {
                modelRoot = new ForceDescriptor();
                modelRoot.setName("Command Model");
            }
            // A rolled command carries no name of its own, so its formation would commit under the bare
            // echelon name ("Battalion"); stamp the unit type in so it reads "Battle Armor Battalion".
            ensureDescriptiveName(fd);
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
        fireToeChanged();
    }

    /**
     * Discards the accumulated Command Model so the next Generate starts a fresh command. Called when
     * the force is cleared; without it the next roll is folded into the model that was just cleared
     * and the discarded units reappear in the tree beside the newly generated ones.
     */
    private void clearAccumulatedModel() {
        if (modelRoot == null && modelTop == null && modelCommandCount == 0) {
            return;
        }
        logger.debug("[ForceGen] Clear Force: discarding accumulated Command Model ({} command(s))",
              modelCommandCount);
        modelRoot = null;
        modelTop = null;
        modelCommandCount = 0;
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
        ensureDescriptiveName(parent);
        if (parent.getName() == null || parent.getName().isBlank()) {
            // The ruleset has no echelon name at this level; fall back to a generic label.
            parent.setName("Command");
        }
        return parent;
    }

    /**
     * Stamps a unit-type-qualified name onto a command whose displayed name is just the bare echelon,
     * so the model tree and committed TOE read "Battle Armor Battalion" instead of "Battalion". This
     * looks at the *resolved* display name ({@link ForceDescriptor#parseName()}), not the raw name,
     * because a rolled command often carries a template like {@code "{ordinal} Battalion"} that resolves
     * to just "Battalion"; a name that already includes the unit type (or a descriptor with no unit
     * type / no echelon name) is left untouched.
     *
     * @param descriptor the command to name in place
     */
    private void ensureDescriptiveName(ForceDescriptor descriptor) {
        String echelonName = Ruleset.findRuleset(descriptor).getEschelonName(descriptor);
        if (echelonName == null || echelonName.isBlank()) {
            logger.info("[ForceGen] ensureDescriptiveName: no echelon name for id={} echelon={}; leaving name='{}'",
                  System.identityHashCode(descriptor), descriptor.getEchelon(), descriptor.parseName());
            return;
        }
        Integer unitType = descriptor.getUnitType();
        if (unitType == null) {
            // No unit type to prepend; the bare echelon name is the best available.
            return;
        }
        String unitTypeName = UnitType.getTypeDisplayableName(unitType);
        String displayName = descriptor.parseName();
        if (displayName != null && displayName.contains(unitTypeName)) {
            // The resolved name already carries the unit type (or a meaningful name), so keep it.
            return;
        }
        String descriptiveName = unitTypeName + " " + echelonName;
        logger.info("[ForceGen] ensureDescriptiveName: id={} '{}' -> '{}'",
              System.identityHashCode(descriptor), displayName, descriptiveName);
        descriptor.setName(descriptiveName);
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
                        // Exclusions change what a commit produces (dropped formations, shifted
                        // callsigns), so let the host refresh before the repaint renders names.
                        fireToeChanged();
                        forceTree.repaint();
                        // The status line counts included units, so re-tally after a toggle.
                        refreshCommandModelChrome();
                    });
                    menu.add(toggleItem);

                    menu.add(buildChangeFormationMenu(fd));
                    menu.add(buildAddSubForceMenu(fd));

                    // A unit in the tree gets the same readouts the lobby offers, so a player can look a Mek over
                    // where they are rather than adding it to the chosen list first.
                    if (fd.getEntity() != null) {
                        menu.addSeparator();
                        menu.add(unitReadoutItem("RandomArmyDialog.View", fd.getEntity(),
                              entities -> LobbyUtility.mekReadoutAction(entities, true, true, parentFrame)));
                        menu.add(unitReadoutItem("RandomArmyDialog.ViewBV", fd.getEntity(),
                              entities -> LobbyUtility.mekBVAction(entities, true, true, parentFrame)));
                        menu.add(unitReadoutItem("RandomArmyDialog.ViewCost", fd.getEntity(),
                              entities -> LobbyUtility.mekCostAction(entities, true, true, parentFrame)));
                    }

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

    /**
     * The formations this node could be given instead of the one it has.
     *
     * <p>Offered from the node's own rule, so nothing here is a formation the ruleset would have refused it. A node
     * whose rule allowed only one formation - command lances, mostly - has no choice to offer and gets no menu.</p>
     *
     * @param formation the node the player right-clicked
     *
     * @return the submenu, or a disabled item explaining why this node has nothing to choose between
     */
    private JMenuItem buildChangeFormationMenu(ForceDescriptor formation) {
        String selected = panControls.getSelectedFormation();
        Map<String, Integer> offered = formation.getEligibleFormations();
        String current = (formation.getFormation() == null) ? null : formation.getFormation().getName();
        logger.debug("[ChangeFormation] '{}' echelon={} formation={} offers {}; palette selection is '{}'",
              formation.parseName(), formation.getEchelon(), current, offered.keySet(), selected);

        JMenuItem item = new JMenuItem(Messages.getString("ForceGeneratorDialog.changeFormation"));
        // Shown even when it cannot be used, with the reason, rather than vanishing: a menu item that appears on
        // some nodes and not others reads as a broken feature, and the player cannot tell which it is.
        if (selected == null) {
            item.setEnabled(false);
            item.setToolTipText(Messages.getString("ForceGeneratorDialog.changeFormation.noSelection"));
            logger.debug("[ChangeFormation] '{}': disabled - no formation picked in the palette",
                  formation.parseName());
            return item;
        }
        if (offered.isEmpty()) {
            item.setEnabled(false);
            item.setToolTipText(Messages.getString("ForceGeneratorDialog.changeFormation.notAFormation"));
            logger.debug("[ChangeFormation] '{}': disabled - not a formation node", formation.parseName());
            return item;
        }
        FormationType formationType = FormationType.getFormationType(selected);
        if (formationType == null) {
            item.setEnabled(false);
            logger.warn("[ChangeFormation] palette holds '{}', which is not a registered formation type", selected);
            return item;
        }

        item.setText(Messages.getString("ForceGeneratorDialog.changeFormation.to", formationType.getName()));
        boolean offersIt = offered.containsKey(selected);
        item.setToolTipText(offersIt
              ? null
              : Messages.getString("ForceGeneratorDialog.changeFormation.notOffered", formationType.getName()));
        item.addActionListener(ev -> confirmAndChangeFormation(formation, formationType, offersIt));
        return item;
    }

    /**
     * Offers to add another formation of the picked type under this one.
     *
     * <p>The new formation copies the shape of a sibling - its echelon, how many units it holds and the rule it
     * generates by - so it is legal by construction rather than assembled from guesses about what the ruleset
     * would have allowed here.</p>
     *
     * @param parent the node right-clicked
     *
     * @return the menu item, disabled with a reason when nothing can be added
     */
    private JMenuItem buildAddSubForceMenu(ForceDescriptor parent) {
        String selected = panControls.getSelectedFormation();
        JMenuItem item = new JMenuItem(Messages.getString("ForceGeneratorDialog.addFormation"));
        ForceDescriptor template = subForceTemplate(parent);
        logger.debug("[AddFormation] '{}' echelon={} holds {} subforce(s); template={}; palette selection '{}'",
              parent.parseName(), parent.getEchelon(), parent.getSubForces().size(),
              (template == null) ? "none" : template.parseName(), selected);

        if (selected == null) {
            item.setEnabled(false);
            item.setToolTipText(Messages.getString("ForceGeneratorDialog.changeFormation.noSelection"));
            return item;
        }
        if (template == null) {
            item.setEnabled(false);
            item.setToolTipText(Messages.getString("ForceGeneratorDialog.addFormation.noTemplate"));
            logger.debug("[AddFormation] '{}': disabled - no sibling formation whose shape could be copied",
                  parent.parseName());
            return item;
        }
        FormationType formationType = FormationType.getFormationType(selected);
        if (formationType == null) {
            item.setEnabled(false);
            return item;
        }
        item.setText(Messages.getString("ForceGeneratorDialog.addFormation.of", formationType.getName()));
        item.addActionListener(ev -> confirmAndAddSubForce(parent, template, formationType));
        return item;
    }

    /**
     * A child of this node whose shape a new one can copy.
     *
     * <p>Formation-bearing children only: the units inside a lance are children too, and copying one of those
     * would add a single unit rather than a formation.</p>
     *
     * @param parent the node to add under
     *
     * @return a child to copy, or {@code null} when this node holds no formations
     */
    private static @Nullable ForceDescriptor subForceTemplate(ForceDescriptor parent) {
        for (ForceDescriptor child : parent.getSubForces()) {
            if (!child.getEligibleFormations().isEmpty() || (child.getFormation() != null)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Asks before adding a formation, then adds and generates it.
     *
     * @param parent        the node to add under
     * @param template      the sibling whose shape the new formation copies
     * @param formationType the formation to give it
     */
    private void confirmAndAddSubForce(ForceDescriptor parent, ForceDescriptor template,
          FormationType formationType) {
        int answer = JOptionPane.showConfirmDialog(parentFrame,
              Messages.getString("ForceGeneratorDialog.addFormation.confirm", formationType.getName(),
                    parent.parseName()),
              Messages.getString("ForceGeneratorDialog.addFormation"), JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            logger.debug("[AddFormation] '{}': cancelled at the confirmation", parent.parseName());
            return;
        }

        ForceDescriptor added = parent.createChild(parent.getSubForces().size());
        // createChild inherits the parent's weight class, which is the weight of the units the parent already
        // holds. Keeping it refuses any formation that needs different units - a Heavy Recon lance asked for
        // inside a medium company came out an ordinary lance. The formation's own requirements decide instead.
        added.setWeightClass(null);
        added.setEchelon(template.getEchelon());
        // The sibling's name pattern, not its parsed name: the pattern is what turns into "C-4 Recon Lance" once
        // the new formation has a position among its siblings.
        added.setName(template.getName());
        added.setEligibleFormations(template.getEligibleFormations());
        added.setGenerationRule(template.getGenerationRule());
        added.setFormationType(formationType);
        // As many unit slots as its sibling holds, so a lance comes out a lance and a Clan star a star.
        for (int slot = 0; slot < template.getSubForces().size(); slot++) {
            added.addSubForce(added.createChild(slot));
        }
        parent.addSubForce(added);
        // Renumber the siblings so the new formation is given a position and the others still read correctly.
        parent.assignPositions();
        logger.info("[AddFormation] added a {} under '{}' with {} unit slot(s)", formationType.getName(),
              parent.parseName(), template.getSubForces().size());

        added.generateUnits(null, 0);
        added.assignCommanders();
        added.loadEntities(null, 0);
        warnIfFormationWasDropped(added, formationType);
        refreshTreeAfterEdit();
    }

    /**
     * Asks before re-rolling a formation, because the units it holds are replaced.
     *
     * @param formation     the node to change
     * @param formationType the formation to give it
     * @param offeredByRules {@code true} when the node's own rule already allowed this formation
     */
    private void confirmAndChangeFormation(ForceDescriptor formation, FormationType formationType,
          boolean offeredByRules) {
        String question = Messages.getString(offeredByRules
                    ? "ForceGeneratorDialog.changeFormation.confirm"
                    : "ForceGeneratorDialog.changeFormation.confirmUnoffered",
              formation.parseName(), formationType.getName());
        int answer = JOptionPane.showConfirmDialog(parentFrame, question,
              Messages.getString("ForceGeneratorDialog.changeFormation"), JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.QUESTION_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) {
            logger.debug("[ChangeFormation] '{}': cancelled at the confirmation", formation.parseName());
            return;
        }
        changeFormation(formation, formationType);
    }

    /**
     * Gives one formation a different type and draws it again.
     *
     * <p>Only this branch of the tree is redrawn. Regenerating the whole force would replace every other unit the
     * player has, which is not what changing one lance asks for.</p>
     *
     * @param formation     the node to change
     * @param formationType the formation to give it
     */
    private void changeFormation(ForceDescriptor formation, FormationType formationType) {
        logger.info("[ForceGen][ChangeFormation] '{}' {} -> {}", formation.parseName(),
              (formation.getFormation() == null) ? "(none)" : formation.getFormation().getName(),
              formationType.getName());
        formation.setFormationType(formationType);
        formation.clearGeneratedUnits();
        formation.generateUnits(null, 0);
        formation.assignCommanders();
        formation.loadEntities(null, 0);
        warnIfFormationWasDropped(formation, formationType);

        refreshTreeAfterEdit();
    }

    /**
     * Says so when a formation could not be built and quietly became an ordinary lance.
     *
     * <p>A formation whose requirements cannot be met from the units this faction and year can field is dropped by
     * the generator and the lance is built normally instead. Nothing announced that: the only outward sign was the
     * name keeping its plain form, which reads as the request having been ignored rather than as the force being
     * unable to supply it.</p>
     *
     * @param formation the node that was just generated
     * @param requested the formation that was asked for
     */
    private void warnIfFormationWasDropped(ForceDescriptor formation, FormationType requested) {
        boolean kept = (formation.getFormation() != null)
              && formation.getFormation().getName().equals(requested.getName());
        if (kept) {
            logger.info("[ChangeFormation] '{}' built as {}", formation.parseName(), requested.getName());
            return;
        }
        logger.warn("[ChangeFormation] '{}' could not be built as {} for faction={} year={}; it was generated as"
                    + " an ordinary formation instead", formation.parseName(), requested.getName(),
              formation.getFaction(), formation.getYear());
        JOptionPane.showMessageDialog(parentFrame,
              Messages.getString("ForceGeneratorDialog.formationDropped", requested.getName(),
                    formation.getFaction(), formation.getYear()),
              Messages.getString("ForceGeneratorDialog.formationDropped.title"),
              JOptionPane.WARNING_MESSAGE);
    }

    /**
     * A menu item that opens one of the lobby's readouts for a single unit.
     *
     * @param messageKey the resource key for the item's text
     * @param entity     the unit to show
     * @param readout    the lobby action to run for it
     *
     * @return the menu item
     */
    private static JMenuItem unitReadoutItem(String messageKey, Entity entity,
          Consumer<Collection<Entity>> readout) {
        JMenuItem item = new JMenuItem(Messages.getString(messageKey));
        item.addActionListener(ev -> readout.accept(Set.of(entity)));
        return item;
    }

    /**
     * Rebuilds the tree after an edit, leaving it looking as it did.
     *
     * <p>The model has to be replaced for the tree to see the change, and that alone would collapse everything back
     * to the root - so an edit three levels down would cost the player the whole view they were working in. The
     * open branches and the selection are taken before and put back after.</p>
     *
     * <p>Branches inside the part that changed cannot come back, because their nodes no longer exist; a re-rolled
     * lance holds different units than the ones that were on screen. Everything outside it is the same object it
     * was, so its path still matches.</p>
     */
    private void refreshTreeAfterEdit() {
        List<TreePath> expanded = new ArrayList<>();
        for (int row = 0; row < forceTree.getRowCount(); row++) {
            TreePath path = forceTree.getPathForRow(row);
            if ((path != null) && forceTree.isExpanded(path)) {
                expanded.add(path);
            }
        }
        TreePath selected = forceTree.getSelectionPath();

        Object root = forceTree.getModel().getRoot();
        if (root instanceof ForceDescriptor displayRoot) {
            forceTree.setModel(new ForceTreeModel(displayRoot));
        }

        // Collected in row order, so a parent is always restored before the children hanging off it.
        int restored = 0;
        for (TreePath path : expanded) {
            forceTree.expandPath(path);
            if (forceTree.isExpanded(path)) {
                restored++;
            }
        }
        if (selected != null) {
            forceTree.setSelectionPath(selected);
            forceTree.scrollPathToVisible(selected);
        }
        logger.debug("[ForceGen][Tree] rebuilt after an edit; reopened {} of {} branch(es)",
              restored, expanded.size());

        fireToeChanged();
        refreshCommandModelChrome();
    }

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

    // Non-static so the formation branch can consult the host-supplied formationNameProvider.
    private class UnitRenderer extends DefaultTreeCellRenderer {
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

        /**
         * The unit's Campaign Operations role, for the end of its line in the tree.
         *
         * <p>Read from the unit summary rather than the entity, because the role is what the summary carries and
         * what the formation criteria are tested against.</p>
         *
         * @param entity the unit, may be {@code null}
         *
         * @return the role suffix, or an empty string when the unit has no role recorded
         */
        private static String unitRoleSuffix(@Nullable Entity entity) {
            if (entity == null) {
                return "";
            }
            MekSummary summary = MekSummaryCache.getInstance().getMek(entity.getShortNameRaw());
            if ((summary == null) || (summary.getRole() == null)) {
                return "";
            }
            return " - " + summary.getRole();
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
                    // The unit's own role is what a formation's requirements are written against, so showing it
                    // here is what lets a glance at the tree say whether a lance really is what it claims to be.
                    setText("<html>" + commander + ", " + uname + unitRoleSuffix(en) + "</html>");
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
                String parsedName = resolveFormationName(fd);
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
