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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import megamek.client.ratgenerator.*;
import megamek.client.ratgenerator.Ruleset.ProgressListener;
import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.codeUtilities.MathUtility;
import megamek.common.Player;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.EntityListFile;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.UnitType;
import megamek.logging.MMLogger;

/**
 * Controls to set options for force generator.
 *
 * @author Neoancient
 */
public class ForceGeneratorOptionsView extends JPanel implements FocusListener, ActionListener {
    private final static MMLogger logger = MMLogger.create(ForceGeneratorOptionsView.class);

    private int currentYear;
    private final Consumer<ForceDescriptor> onGenerate;
    /**
     * Optional override for the Export-MUL button action. When set, replaces the built-in {@link #exportMUL}
     * call so embedders can route the descriptor through their own export path. Null means use the default.
     */
    private Consumer<ForceDescriptor> onExportMUL;
    private Consumer<FactionRecord> onFactionChanged;

    private ForceDescriptor forceDesc = new ForceDescriptor();

    private JTextField txtYear;
    private JComboBox<FactionRecord> cbFaction;
    private JComboBox<FactionRecord> cbSubFaction;
    private JComboBox<Integer> cbUnitType;
    private JComboBox<String> cbFormation;
    private JComboBox<String> cbRating;
    private JComboBox<String> cbFlags;

    private JComboBox<String> cbExperience;
    private JComboBox<Integer> cbWeightClass;
    private JCheckBox chkDetachments;
    private JPanel panGenerateOptions;

    private final DefaultListCellRenderer factionRenderer = new CBRenderer<FactionRecord>(Messages.getString(
          "ForceGeneratorDialog.general"), fRec -> fRec.getName(currentYear));

    private final HashMap<String, String> ratingDisplayNames = new HashMap<>();
    private final HashMap<String, String> formationDisplayNames = new HashMap<>();
    private final HashMap<String, String> flagDisplayNames = new HashMap<>();

    private MissionRoleFilterPanel panMissionRoleFilters;

    private JTextField txtDropshipPct;
    private JTextField txtJumpshipPct;
    private JTextField txtWarshipPct;
    private JTextField txtCargoPct;
    private JCheckBox chkFighterComplement;

    /** Post-generation summary: unit type rows, Light/Medium/Heavy/Assault columns. */
    private JTable tblSummary;
    private DefaultTableModel summaryModel;
    /** What the formation mix delivered against what it asked for. */
    private JLabel lblFormationMixResult;
    /** Opens the formation mix editor; the label beside it shows any request in force. */
    private JButton btnFormationMix;
    private JLabel lblFormationMixSummary;
    /** The requested distribution of formation types, empty until the player asks for one. */
    private FormationMix formationMix = FormationMix.EMPTY;
    /** Holds the mix editor when a host shows it inline rather than opening it from the button. */
    private JPanel panFormationMixInline;
    private boolean formationMixInline = false;

    private JButton btnGenerate;
    private JButton btnExportMUL;
    private JButton btnClear;

    /**
     * The options the generated force is built for. Not final: the hosting dialog is created before the
     * game it belongs to is known, so it starts on defaults and is handed the real options later.
     */
    private GameOptions gameOptions;

    public ForceGeneratorOptionsView(Consumer<ForceDescriptor> onGenerate, GameOptions gameOptions) {
        this.onGenerate = onGenerate;
        this.gameOptions = gameOptions;
        if (!Ruleset.isInitialized()) {
            Ruleset.loadData();
        }
        initUi();
    }

    /**
     * Points this panel at the options of the game the force is actually being generated for.
     *
     * <p>The dialog is built before the game is known, so it starts on a default set. Until this was
     * propagated the generator kept reading those defaults, and a rule the player had switched on -
     * Manei Domini, say - was invisible to generation while being plainly on in the lobby.</p>
     *
     * @param gameOptions the options of the game being generated for
     */
    public void setGameOptions(GameOptions gameOptions) {
        this.gameOptions = gameOptions;
    }

    /**
     * Assembles the panel from its sections, in the order they appear on screen.
     *
     * <p>Each section adds its own rows and hands back the next free one, so the running row index stays the single
     * thing they share.</p>
     */
    private void initUi() {
        currentYear = gameOptions.intOption(OptionsConstants.ALLOWED_YEAR);
        forceDesc.setYear(currentYear);
        RATGenerator.getInstance().loadYear(currentYear);

        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        int inset = UIUtil.scaleForGUI(5);
        constraints.insets = new Insets(inset, inset, inset, inset);

        int row = 0;
        row = addForceDescriptionFields(constraints, row);
        JPanel transportPanel = buildTransportPanel(constraints);
        row = addFormationMixPanel(constraints, row);
        row = addMissionRoleFilters(constraints, row);
        row = addTransportAndSummary(constraints, row, transportPanel);
        addGenerateControls(constraints, row);

        refreshFactions();
    }

    /**
     * Adds the fields that describe the force: year, faction, unit type, formation, rating, weight and experience.
     *
     * @param constraints the shared constraints
     * @param startRow the first free grid row
     *
     * @return the next free grid row
     */
    private int addForceDescriptionFields(GridBagConstraints constraints, int startRow) {
        int row = startRow;

        constraints.gridx = 0;
        constraints.gridy = row;
        add(describedLabel("ForceGeneratorDialog.year"), constraints);
        txtYear = new JTextField();
        txtYear.setEditable(true);
        txtYear.setText(Integer.toString(currentYear));
        txtYear.setToolTipText(Messages.getString("ForceGeneratorDialog.year.tooltip"));
        constraints.gridx = 1;
        constraints.gridy = row++;
        add(txtYear, constraints);
        txtYear.addFocusListener(this);
        constraints.gridx = 0;
        constraints.gridy = row;
        add(describedLabel("ForceGeneratorDialog.faction"), constraints);
        cbFaction = new JComboBox<>();
        cbFaction.setRenderer(factionRenderer);
        constraints.gridx = 1;
        constraints.gridy = row;
        add(cbFaction, constraints);
        cbFaction.setToolTipText(Messages.getString("ForceGeneratorDialog.faction.tooltip"));
        cbFaction.addActionListener(this);

        constraints.gridx = 2;
        constraints.gridy = row;
        add(describedLabel("ForceGeneratorDialog.subfaction"), constraints);
        cbSubFaction = new JComboBox<>();
        cbSubFaction.setRenderer(factionRenderer);
        constraints.gridx = 3;
        constraints.gridy = row++;
        add(cbSubFaction, constraints);
        cbSubFaction.setToolTipText(Messages.getString("ForceGeneratorDialog.subfaction.tooltip"));
        cbSubFaction.addActionListener(this);

        // TODO (future state) - Specific Unit picker (Option B). Add a combo here, after the
        // subfaction, populated from the `units:` block of the selected command's universe data file
        // (data/universe/commands/<KEY>.yml). Selecting a named regiment (e.g. "1st Sword of Light")
        // would pin its era-appropriate composition - battalionWeights become a fixed
        // <subforce weightClass="..."> distribution instead of the random <subforceOption> roll - plus
        // skill and commander, with any unspecified field falling back to normal generic generation.
        // The list should be year-filtered using each unit's yearsActive / history span (the Year
        // field already set drives this; no era picker is needed). Only `name` is mandatory in the
        // per-unit schema. See data/universe/commands/DC.SL.yml for the pilot data and schema notes.

        constraints.gridx = 0;
        constraints.gridy = row;
        add(describedLabel("ForceGeneratorDialog.unitType"), constraints);
        cbUnitType = new JComboBox<>();
        cbUnitType.setRenderer(new CBRenderer<>(Messages.getString("ForceGeneratorDialog.combined"),
              UnitType::getTypeName));
        constraints.gridx = 1;
        constraints.gridy = row;
        add(cbUnitType, constraints);
        cbUnitType.setToolTipText(Messages.getString("ForceGeneratorDialog.unitType.tooltip"));
        cbUnitType.addActionListener(this);

        constraints.gridx = 2;
        constraints.gridy = row;
        add(describedLabel("ForceGeneratorDialog.formation"), constraints);
        cbFormation = new JComboBox<>();
        cbFormation.setRenderer(new CBRenderer<String>(Messages.getString("ForceGeneratorDialog.random"),
              formationDisplayNames::get));
        constraints.gridx = 3;
        constraints.gridy = row++;
        add(cbFormation, constraints);
        cbFormation.setToolTipText(Messages.getString("ForceGeneratorDialog.formation.tooltip"));
        cbFormation.addActionListener(this);

        constraints.gridx = 0;
        constraints.gridy = row;
        add(describedLabel("ForceGeneratorDialog.rating"), constraints);
        cbRating = new JComboBox<>();
        cbRating.setRenderer(new CBRenderer<String>(Messages.getString("ForceGeneratorDialog.random"),
              ratingDisplayNames::get));
        constraints.gridx = 1;
        constraints.gridy = row;
        add(cbRating, constraints);
        cbRating.setToolTipText(Messages.getString("ForceGeneratorDialog.rating.tooltip"));
        cbRating.addActionListener(this);

        constraints.gridx = 2;
        constraints.gridy = row;
        add(describedLabel("ForceGeneratorDialog.weight"), constraints);
        cbWeightClass = new JComboBox<>();
        cbWeightClass.setRenderer(new CBRenderer<Integer>(Messages.getString("ForceGeneratorDialog.random"),
              EntityWeightClass::getClassName));
        cbWeightClass.addItem(null);
        cbWeightClass.addItem(EntityWeightClass.WEIGHT_LIGHT);
        cbWeightClass.addItem(EntityWeightClass.WEIGHT_MEDIUM);
        cbWeightClass.addItem(EntityWeightClass.WEIGHT_HEAVY);
        cbWeightClass.addItem(EntityWeightClass.WEIGHT_ASSAULT);
        constraints.gridx = 3;
        constraints.gridy = row++;
        add(cbWeightClass, constraints);
        cbWeightClass.setToolTipText(Messages.getString("ForceGeneratorDialog.weight.tooltip"));
        cbWeightClass.addActionListener(this);

        constraints.gridx = 0;
        constraints.gridy = row;
        add(describedLabel("ForceGeneratorDialog.other"), constraints);
        cbFlags = new JComboBox<>();
        cbFlags.setRenderer(new CBRenderer<String>("---", flagDisplayNames::get));
        constraints.gridx = 1;
        constraints.gridy = row;
        add(cbFlags, constraints);
        cbFlags.setToolTipText(Messages.getString("ForceGeneratorDialog.other.tooltip"));
        cbFlags.addActionListener(this);

        constraints.gridx = 2;
        constraints.gridy = row;
        add(describedLabel("ForceGeneratorDialog.experience"), constraints);
        cbExperience = new JComboBox<>();
        cbExperience.addItem(Messages.getString("ForceGeneratorDialog.random"));
        cbExperience.addItem(Messages.getString("ForceGeneratorDialog.green"));
        cbExperience.addItem(Messages.getString("ForceGeneratorDialog.regular"));
        cbExperience.addItem(Messages.getString("ForceGeneratorDialog.veteran"));
        cbExperience.addItem(Messages.getString("ForceGeneratorDialog.elite"));
        constraints.gridx = 3;
        constraints.gridy = row++;
        add(cbExperience, constraints);
        cbExperience.setToolTipText(Messages.getString("ForceGeneratorDialog.experience.tooltip"));
        cbExperience.addActionListener(this);

        // The sections below span the full dialog width rather than sitting in the four field columns.
        constraints.gridwidth = 4;
        constraints.gridx = 0;
        constraints.gridy = row++;
        return row;
    }

    /**
     * Builds the transport panel: the DropShip, JumpShip, WarShip and cargo percentages, plus fighter complement.
     *
     * @param constraints the shared constraints
     *
     * @return the transport panel, for the caller to place beside the composition summary
     */
    private JPanel buildTransportPanel(GridBagConstraints constraints) {
        JPanel panTransport = new JPanel(new GridLayout(5, 2));
        txtDropshipPct = new JTextField("0");
        txtDropshipPct.setToolTipText(Messages.getString("ForceGeneratorDialog.dropshipPercentage.tooltip"));
        txtJumpshipPct = new JTextField("0");
        txtJumpshipPct.setToolTipText(Messages.getString("ForceGeneratorDialog.jumpshipPercentage.tooltip"));
        txtWarshipPct = new JTextField("0");
        txtWarshipPct.setToolTipText(Messages.getString("ForceGeneratorDialog.warshipPercentage.tooltip"));
        // Default 100: provision cargo holds for everything the command has to haul. Above 100
        // buys headroom for cargo it picks up later.
        txtCargoPct = new JTextField("100");
        txtCargoPct.setToolTipText(Messages.getString("ForceGeneratorDialog.cargoPct.tooltip"));
        // The label is what a player reads and points at, so it carries the same explanation as the
        // field. With the tooltip on the input box alone, hovering the thing that names the setting
        // explained nothing.
        panTransport.add(describedLabel("ForceGeneratorDialog.dropshipPercentage"));
        panTransport.add(txtDropshipPct, constraints);
        panTransport.add(describedLabel("ForceGeneratorDialog.jumpshipPercentage"));
        panTransport.add(txtJumpshipPct, constraints);
        panTransport.add(describedLabel("ForceGeneratorDialog.warshipPercentage"));
        panTransport.add(txtWarshipPct, constraints);
        panTransport.add(describedLabel("ForceGeneratorDialog.cargoPct"));
        panTransport.add(txtCargoPct, constraints);
        chkFighterComplement = new JCheckBox(Messages.getString("ForceGeneratorDialog.fighterComplement"));
        chkFighterComplement.setToolTipText(Messages.getString("ForceGeneratorDialog.fighterComplement.tooltip"));
        panTransport.add(chkFighterComplement);
        panTransport.add(new JLabel(""));
        panTransport.setBorder(BorderFactory.createTitledBorder(Messages.getString("ForceGeneratorDialog.transport")));
        return panTransport;
    }

    /**
     * Adds the inline formation mix panel, which shapes the force and so leads the sections that describe it.
     *
     * <p>Hidden unless a host asks for it inline; MegaMek opens the same editor from a button instead.</p>
     *
     * @param constraints the shared constraints
     * @param startRow the first free grid row
     *
     * @return the next free grid row
     */
    private int addFormationMixPanel(GridBagConstraints constraints, int startRow) {
        int row = startRow;
        panFormationMixInline = new JPanel(new BorderLayout());
        panFormationMixInline.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("ForceGeneratorDialog.formationMix.title")));
        panFormationMixInline.setVisible(false);
        constraints.gridx = 0;
        constraints.gridy = row++;
        constraints.gridwidth = 4;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        add(panFormationMixInline, constraints);
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;
        constraints.weighty = 0.0;

        return row;
    }

    /**
     * Adds the mission-role filters, which sit between the mix and the panels that describe and carry the force:
     * they qualify what the mix produces rather than shaping it.
     *
     * @param constraints the shared constraints
     * @param startRow the first free grid row
     *
     * @return the next free grid row
     */
    private int addMissionRoleFilters(GridBagConstraints constraints, int startRow) {
        int row = startRow;
        panMissionRoleFilters = new MissionRoleFilterPanel();
        constraints.gridx = 0;
        constraints.gridy = row++;
        add(panMissionRoleFilters, constraints);
        constraints.gridwidth = 1;
        return row;
    }

    /**
     * Adds the transport panel and the post-generation composition summary as one row.
     *
     * <p>They share a {@link BorderLayout} container so the summary absorbs whatever horizontal slack the
     * column-driven outer layout would otherwise leave between them: transport sits at its preferred width on the
     * WEST, the summary fills the rest in the CENTER.</p>
     *
     * @param constraints the shared constraints
     * @param startRow the first free grid row
     * @param panTransport the transport panel built earlier
     *
     * @return the next free grid row
     */
    private int addTransportAndSummary(GridBagConstraints constraints, int startRow, JPanel panTransport) {
        int row = startRow;
        JPanel transportAndSummary = new JPanel(new BorderLayout(10, 0));
        transportAndSummary.add(panTransport, BorderLayout.WEST);
        JPanel summaryWithMix = new JPanel(new BorderLayout(0, 2));
        summaryWithMix.setOpaque(false);
        summaryWithMix.add(createSummaryTable(), BorderLayout.CENTER);
        lblFormationMixResult = new JLabel(" ");
        summaryWithMix.add(lblFormationMixResult, BorderLayout.SOUTH);
        transportAndSummary.add(summaryWithMix, BorderLayout.CENTER);

        constraints.gridx = 0;
        constraints.gridy = row++;
        constraints.gridwidth = 4;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weighty = 1.0;
        add(transportAndSummary, constraints);
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weighty = 0;
        return row;
    }

    /**
     * Adds the button strip that drives generation: Generate and its options, Export MUL and Clear Force.
     *
     * @param constraints the shared constraints
     * @param startRow the first free grid row
     */
    private void addGenerateControls(GridBagConstraints constraints, int startRow) {
        int row = startRow;
        btnGenerate = new JButton(Messages.getString("ForceGeneratorDialog.generate"));
        btnGenerate.setToolTipText(Messages.getString("ForceGeneratorDialog.generate.tooltip"));
        constraints.gridx = 0;
        constraints.gridy = row;
        constraints.gridwidth = 1;
        constraints.weighty = 1.0;
        add(btnGenerate, constraints);
        btnGenerate.addActionListener(this);

        // Options that modify the Generate action sit directly beside it rather than among the
        // standalone settings above. They live in their own panel so a host (MekHQ's Command Designer)
        // can append its own toggles via addGenerateOption without disturbing the button columns.
        panGenerateOptions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        // Transparent so the row reads as part of the button strip; an opaque nested panel paints its
        // own theme background and shows up as a coloured box against the surrounding container.
        panGenerateOptions.setOpaque(false);
        chkDetachments = new JCheckBox(Messages.getString("ForceGeneratorDialog.generateDetachments"));
        chkDetachments.setToolTipText(Messages.getString("ForceGeneratorDialog.generateDetachments.tooltip"));
        chkDetachments.setSelected(true);
        // Turning detachments off withdraws the aerospace and infantry formations, so the editor has to be rebuilt.
        chkDetachments.addActionListener(this);
        panGenerateOptions.add(chkDetachments);
        // Behind a button rather than inline: which formations a force offers depends on the selections above, so
        // the list is a couple of dozen rows that would crowd out everything else on an already dense panel.
        btnFormationMix = new JButton(Messages.getString("ForceGeneratorDialog.formationMix.button"));
        btnFormationMix.setToolTipText(Messages.getString("ForceGeneratorDialog.formationMix.button.tooltip"));
        btnFormationMix.addActionListener(event -> showFormationMixDialog());
        panGenerateOptions.add(btnFormationMix);
        lblFormationMixSummary = new JLabel(" ");
        panGenerateOptions.add(lblFormationMixSummary);
        constraints.gridx = 1;
        constraints.gridy = row;
        add(panGenerateOptions, constraints);

        btnExportMUL = new JButton(Messages.getString("ForceGeneratorDialog.exportMUL"));
        btnExportMUL.setToolTipText(Messages.getString("ForceGeneratorDialog.exportMUL.tooltip"));
        constraints.gridx = 2;
        constraints.gridy = row;
        add(btnExportMUL, constraints);
        btnExportMUL.addActionListener(this);
        btnExportMUL.setEnabled(false);

        btnClear = new JButton(Messages.getString("ForceGeneratorDialog.clear"));
        btnClear.setToolTipText(Messages.getString("ForceGeneratorDialog.clear.tooltip"));
        constraints.gridx = 3;
        constraints.gridy = row;
        constraints.weighty = 1.0;
        add(btnClear, constraints);
        btnClear.addActionListener(this);
        btnClear.setEnabled(false);

    }

    /**
     * Opens the formation mix editor for the force the current selections describe.
     *
     * <p>The offered formations are discovered by building that force's structure and stopping before any unit is
     * drawn - a twentieth of the cost of generating it - because which formations a lance is offered depends on
     * state that only exists once the tree is being built.</p>
     */
    private void showFormationMixDialog() {
        Ruleset ruleset = Ruleset.findRuleset(buildForceDescriptor());
        if (ruleset == null) {
            JOptionPane.showMessageDialog(this,
                  Messages.getString("ForceGeneratorDialog.formationMix.noRuleset"),
                  Messages.getString("ForceGeneratorDialog.formationMix.title"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        FormationMixEditorPanel editor = new FormationMixEditorPanel(sampleFormationOffer(ruleset));
        editor.setMix(formationMix);

        JScrollPane scroll = new JScrollPane(editor);
        // The editor sizes itself to the viewport width, so a horizontal bar would never have anything to scroll.
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setPreferredSize(UIUtil.scaleForGUI(760, 420));
        int choice = JOptionPane.showConfirmDialog(this, scroll,
              Messages.getString("ForceGeneratorDialog.formationMix.title"),
              JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice == JOptionPane.OK_OPTION) {
            formationMix = editor.getMix();
            refreshFormationMixSummary();
        }
    }

    /**
     * Samples what the currently described force offers, over several structure-only builds.
     *
     * <p>One build is not representative: weight class is rolled per node and most formation options are gated on
     * it, so consecutive builds of the same regiment offer noticeably different formations. Sampling gives the
     * player a list that does not change under them every time they open the editor.</p>
     *
     * @param ruleset the ruleset for the described force
     *
     * @return the combined picture of what the force offers
     */
    public FormationMixPreview sampleFormationOffer(Ruleset ruleset) {
        List<FormationMixPreview> samples = new ArrayList<>();
        for (int sample = 0; sample < FORMATION_OFFER_SAMPLES; sample++) {
            ForceDescriptor probe = buildForceDescriptor();
            ruleset.buildStructureOnly(probe);
            samples.add(FormationMixPreview.of(probe));
        }
        return FormationMixPreview.merged(samples);
    }

    /**
     * How many structure-only builds to average when working out what a force offers. A build costs about a
     * twentieth of a generation, so this is still a fraction of one roll.
     */
    private static final int FORMATION_OFFER_SAMPLES = 15;

    /** Shows on the main panel which formations the mix is asking for, or nothing at all when it asks for none. */
    private void refreshFormationMixSummary() {
        if (lblFormationMixSummary == null) {
            return;
        }
        if (formationMix.isEmpty()) {
            lblFormationMixSummary.setText(" ");
            lblFormationMixSummary.setToolTipText(null);
            return;
        }
        String requested = formationMix.percentages()
              .entrySet()
              .stream()
              .map(entry -> entry.getKey() + " " + entry.getValue() + "%")
              .collect(Collectors.joining(", "));
        lblFormationMixSummary.setText(Messages.getString("ForceGeneratorDialog.formationMix.summary", requested));
        lblFormationMixSummary.setToolTipText(requested);
    }

    /**
     * Hides the button that opens the mix in a dialog, for a host showing the editor inline instead.
     *
     * @param visible {@code false} to hide the button and its summary
     */
    public void setFormationMixButtonVisible(boolean visible) {
        if (btnFormationMix != null) {
            btnFormationMix.setVisible(visible);
        }
        if (lblFormationMixSummary != null) {
            lblFormationMixSummary.setVisible(visible);
        }
    }

    /**
     * Shows the mix editor inline, above Transport and the Composition Summary, in place of the dialog button.
     *
     * <p>For a host with the room to keep it on screen. The editor is rebuilt from the current selections, so call
     * this again whenever those change.</p>
     *
     * @param visible {@code true} to show the editor inline
     */
    public void setFormationMixInline(boolean visible) {
        formationMixInline = visible;
        panFormationMixInline.setVisible(visible);
        setFormationMixButtonVisible(!visible);
        if (visible) {
            refreshInlineFormationMixEditor();
        }
        revalidate();
        repaint();
    }

    /** Rebuilds the inline editor for the force the current selections describe, keeping any request already made. */
    public void refreshInlineFormationMixEditor() {
        if (!formationMixInline) {
            return;
        }
        Ruleset ruleset = Ruleset.findRuleset(buildForceDescriptor());
        FormationMixEditorPanel editor = new FormationMixEditorPanel(
              (ruleset == null) ? FormationMixPreview.EMPTY : sampleFormationOffer(ruleset));
        editor.setMix(formationMix);
        // No OK button inline, so the request is read back on every change.
        editor.addMixChangeListener(() -> formationMix = editor.getMix());
        JScrollPane scroll = new JScrollPane(editor);
        // The editor sizes itself to the viewport width, so a horizontal bar would never have anything to scroll.
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        // Without a size the scroll pane grows to the editor's full height and never scrolls, pushing everything
        // below it off the dialog.
        scroll.setPreferredSize(UIUtil.scaleForGUI(720, INLINE_MIX_HEIGHT));
        panFormationMixInline.removeAll();
        panFormationMixInline.add(scroll, BorderLayout.CENTER);
        panFormationMixInline.revalidate();
        panFormationMixInline.repaint();
    }

    /** Unscaled height of the inline mix editor, past which it scrolls rather than growing. */
    private static final int INLINE_MIX_HEIGHT = 260;

    /**
     * The controls that drive Generate, so a host can move them into its own button bar.
     *
     * <p>Re-parenting them removes them from this panel, which is the intent: a host with a toolbar of its own
     * should not also show a second set of buttons mid-panel.</p>
     *
     * @return the Generate button, its options panel and the Clear Force button, in that order
     */
    public List<JComponent> getGenerateControls() {
        return List.of(btnGenerate, panGenerateOptions, btnClear);
    }

    /**
     * The formation mix the player has asked for.
     *
     * @return the requested mix, never {@code null}
     */
    public FormationMix getFormationMix() {
        return formationMix;
    }

    /**
     * Sets the formation mix, for a host restoring saved options.
     *
     * @param formationMix the mix to apply, or {@code null} to clear
     */
    public void setFormationMix(@Nullable FormationMix formationMix) {
        this.formationMix = (formationMix == null) ? FormationMix.EMPTY : formationMix;
        refreshFormationMixSummary();
    }

    public ForceDescriptor buildForceDescriptor() {
        ForceDescriptor fd = new ForceDescriptor();
        fd.setTopLevel(true);
        fd.setYear(forceDesc.getYear());
        fd.setFaction(forceDesc.getFaction());
        fd.setUnitType(forceDesc.getUnitType());
        fd.setEchelon(forceDesc.getEchelon());
        fd.setAugmented(forceDesc.isAugmented());
        fd.setSizeMod(forceDesc.getSizeMod());
        fd.getFlags().addAll(forceDesc.getFlags());
        fd.setRating(forceDesc.getRating());
        if (forceDesc.getExperience() != null) {
            fd.setExperience(forceDesc.getExperience());
        } else {
            fd.setExperience(CrewDescriptor.randomExperienceLevel());
        }
        // Read directly from the dropdown rather than the cached forceDesc field.
        // The SwingWorker's done() callback overwrites forceDesc with the engine-mutated
        // tree-root descriptor after each Generate, so the cached weightClass can drift
        // away from the user's UI selection across consecutive runs.
        Object selectedWeight = cbWeightClass.getSelectedItem();
        fd.setWeightClass(selectedWeight instanceof Integer ? (Integer) selectedWeight : null);
        fd.setAttachments(chkDetachments.isSelected());
        // Empty unless the player opened the mix editor and asked for something, in which case the allocator
        // returns immediately and the force generates exactly as it did before.
        fd.setFormationMix(formationMix);
        panMissionRoleFilters.applyTo(fd, forceDesc.getUnitType());

        // Internal storage uses fraction (0.0–N.0+); the textbox shows percentage (0–N00).
        // Preserve the user's input form in the textbox so it doesn't reset to "1.0" after Generate.
        double dropShipPct = MathUtility.parseDouble(txtDropshipPct.getText(), 0.0);
        fd.setDropshipPct(dropShipPct * 0.01);
        txtDropshipPct.setText(String.valueOf(dropShipPct));

        double jumpShipPct = MathUtility.parseDouble(txtJumpshipPct.getText(), 0.0);
        fd.setJumpshipPct(jumpShipPct * 0.01);
        txtJumpshipPct.setText(String.valueOf(jumpShipPct));

        double warShipPct = MathUtility.parseDouble(txtWarshipPct.getText(), 0.0);
        fd.setWarshipPct(warShipPct * 0.01);
        txtWarshipPct.setText(String.valueOf(warShipPct));

        double cargoPct = Math.max(0.0, MathUtility.parseDouble(txtCargoPct.getText(), 100.0));
        fd.setCargoPct(cargoPct);
        txtCargoPct.setText(String.valueOf(cargoPct));

        fd.setFighterComplement(chkFighterComplement.isSelected());

        return fd;
    }

    private void generateForce() {
        ForceDescriptor fd = buildForceDescriptor();

        ProgressMonitor monitor = new ProgressMonitor(this,
              Messages.getString("ForceGeneratorDialog.generateFormation"),
              "",
              0,
              100);
        monitor.setProgress(0);
        GenerateTask task = new GenerateTask(fd);
        task.addPropertyChangeListener(e -> {
            monitor.setProgress(task.getProgress());
            monitor.setNote(task.getMessage());
            if (monitor.isCanceled()) {
                task.cancel(true);
            }
        });
        task.execute();
    }

    private void clearForce() {
        if (null != onGenerate) {
            onGenerate.accept(null);
        }
        clearSummaryTable();
    }

    /**
     * Builds the post-generation composition summary table (rows: unit types present in the force; columns: Light /
     * Medium / Heavy / Assault counts). Empty until the first Generate.
     */
    private JScrollPane createSummaryTable() {
        String[] columns = {
              Messages.getString("ForceGeneratorDialog.summary.unitType"),
              Messages.getString("ForceGeneratorDialog.summary.light"),
              Messages.getString("ForceGeneratorDialog.summary.medium"),
              Messages.getString("ForceGeneratorDialog.summary.heavy"),
              Messages.getString("ForceGeneratorDialog.summary.assault")
        };
        summaryModel = new DefaultTableModel(columns, 0) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblSummary = new JTable(summaryModel);
        tblSummary.setAutoCreateRowSorter(false);
        tblSummary.getTableHeader().setReorderingAllowed(false);
        // Unit Type column is wider to fit the longest name (AeroSpaceFighter); numeric columns
        // are narrower since they only hold 1-3 digit counts. Total ~380px fits comfortably in
        // the 480px scroll-pane viewport with the default AUTO_RESIZE_SUBSEQUENT_COLUMNS.
        tblSummary.getColumnModel().getColumn(0).setPreferredWidth(140);
        for (int col = 1; col <= 4; col++) {
            tblSummary.getColumnModel().getColumn(col).setPreferredWidth(60);
        }
        JScrollPane scrollPane = new JScrollPane(tblSummary);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
              Messages.getString("ForceGeneratorDialog.summary.title")));
        // Preferred width sized so the BorderLayout wrapper extends most of the way to the right
        // edge of the dialog (cols 0-3 grow to accommodate this preferred when it exceeds the
        // natural column-sum width). Roughly Transport width + a combo-and-a-half on the right.
        scrollPane.setPreferredSize(UIUtil.scaleForGUI(480, 140));
        return scrollPane;
    }

    /**
     * Walks the generated force tree, buckets each entity into (unit type, weight class), and rebuilds the summary
     * table. Weight-class codes 0-1 collapse into Light and 4-5 into Assault to keep the table to a clean four
     * columns.
     * <p>For Battle Armor each entity represents one Squad/Point (5 Clan Elementals, 4-5 IS), so cells show
     * "N (M)" where N is the squad count and M is the total trooper count. Other unit types show plain N.</p>
     */
    private void updateSummaryTable(ForceDescriptor fd) {
        summaryModel.setRowCount(0);
        updateFormationMixResult(fd);
        if (fd == null) {
            return;
        }
        ArrayList<Entity> entities = new ArrayList<>();
        fd.addAllEntities(entities);
        // Per (unitType, weightClassColumn): [0]=squad/entity count, [1]=trooper count (BA only).
        Map<Integer, int[][]> counts = new TreeMap<>();
        for (Entity entity : entities) {
            int unitType = entity.getUnitType();
            int weightClass = entity.getWeightClass();
            int column;
            if (weightClass <= EntityWeightClass.WEIGHT_LIGHT) {
                column = 0;
            } else if (weightClass == EntityWeightClass.WEIGHT_MEDIUM) {
                column = 1;
            } else if (weightClass == EntityWeightClass.WEIGHT_HEAVY) {
                column = 2;
            } else {
                column = 3;
            }
            int[][] row = counts.computeIfAbsent(unitType, k -> new int[4][2]);
            row[column][0]++;
            if (entity instanceof BattleArmor ba) {
                row[column][1] += ba.getShootingStrength();
            }
        }
        // At Galaxy echelon and above (constants.txt: GALAXY/BRIGADE=7, TOUMAN/DIVISION=8, ...) a force
        // holds hundreds of units, so raw per-cell counts are unreadable. Show each weight class as a
        // percentage of that unit type's total instead. Smaller forces keep the exact counts.
        Integer echelon = fd.getEchelon();
        boolean asPercent = (echelon != null) && (echelon >= LARGE_ECHELON_PERCENT_THRESHOLD);
        for (Map.Entry<Integer, int[][]> entry : counts.entrySet()) {
            int[][] row = entry.getValue();
            boolean isBA = (entry.getKey() == UnitType.BATTLE_ARMOR);
            if (asPercent) {
                int typeTotal = row[0][0] + row[1][0] + row[2][0] + row[3][0];
                summaryModel.addRow(new Object[] {
                      UnitType.getTypeName(entry.getKey()),
                      formatSummaryPercent(row[0][0], typeTotal),
                      formatSummaryPercent(row[1][0], typeTotal),
                      formatSummaryPercent(row[2][0], typeTotal),
                      formatSummaryPercent(row[3][0], typeTotal)
                });
            } else {
                summaryModel.addRow(new Object[] {
                      UnitType.getTypeName(entry.getKey()),
                      formatSummaryCell(row[0], isBA),
                      formatSummaryCell(row[1], isBA),
                      formatSummaryCell(row[2], isBA),
                      formatSummaryCell(row[3], isBA)
                });
            }
        }
    }

    /** Echelon level at or above which the composition summary switches from counts to percentages. */
    private static final int LARGE_ECHELON_PERCENT_THRESHOLD = 7;

    /**
     * Formats a summary-table cell as a whole-number percentage of the unit type's total, e.g. "43%". An empty bucket
     * renders as "0%"; a type with no units renders blank.
     */
    private static String formatSummaryPercent(int count, int total) {
        if (total <= 0) {
            return "";
        }
        return Math.round(100.0 * count / total) + "%";
    }

    /**
     * Formats a summary-table cell. For Battle Armor with at least one squad, shows "N (M)" — squad count and total
     * trooper count in parentheses. Other unit types and empty cells render as the plain integer.
     */
    private static String formatSummaryCell(int[] squadsAndTroopers, boolean isBattleArmor) {
        int squads = squadsAndTroopers[0];
        int troopers = squadsAndTroopers[1];
        if (isBattleArmor && squads > 0) {
            return squads + " (" + troopers + ")";
        }
        return String.valueOf(squads);
    }

    /**
     * Reports what the formation mix delivered against what it asked for, and names anything it could not place.
     *
     * <p>Shown because the two often differ for reasons the player cannot see: a formation only a few of the force's
     * lances are ever offered cannot take a large share however much is requested, and one that is assigned can
     * still fail its own requirements when its units are drawn. A mix that silently delivers less than it was asked
     * for reads as a mix that did nothing.</p>
     *
     * @param forceDescriptor the generated force, or {@code null} to blank the line
     */
    private void updateFormationMixResult(@Nullable ForceDescriptor forceDescriptor) {
        if (lblFormationMixResult == null) {
            return;
        }
        FormationMixReport report = (forceDescriptor == null) ? null : forceDescriptor.getFormationMixReport();
        if ((report == null) || (report.totalRequested() == 0)) {
            lblFormationMixResult.setText(" ");
            lblFormationMixResult.setToolTipText(null);
            return;
        }
        lblFormationMixResult.setText(Messages.getString("ForceGeneratorDialog.formationMix.result",
              report.totalAssigned(), report.totalRequested(), report.preview().tweakableNodes()));
        lblFormationMixResult.setToolTipText(report.warnings().isEmpty()
              ? null
              : "<html>" + String.join("<br>", report.warnings()) + "</html>");
    }

    private void clearSummaryTable() {
        if (summaryModel != null) {
            summaryModel.setRowCount(0);
        }
        updateFormationMixResult(null);
    }

    private void refreshFactions() {
        FactionRecord oldFaction = (FactionRecord) cbFaction.getSelectedItem();
        cbFaction.removeActionListener(this);
        cbFaction.removeAllItems();
        List<FactionRecord> activePoliticalFactions = RATGenerator.getInstance().getFactionList().stream()
              .filter(fr -> !fr.getKey().contains(".") && fr.isActiveInYear(currentYear))
              .sorted(Comparator.comparing(fr -> fr.getName(currentYear))).toList();
        ((DefaultComboBoxModel<FactionRecord>) cbFaction.getModel()).addAll(activePoliticalFactions);
        cbFaction.setSelectedItem(oldFaction);
        if (cbFaction.getSelectedItem() == null ||
              !cbFaction.getSelectedItem().toString().equals(Objects.requireNonNull(oldFaction).toString())) {
            cbFaction.setSelectedItem(RATGenerator.getInstance().getFaction("IS"));
        }
        if (cbFaction.getSelectedItem() != null) {
            forceDesc.setFaction(Objects.requireNonNull(cbFaction.getSelectedItem()).toString());
            refreshSubFactions();
        }
        cbFaction.addActionListener(this);
    }

    private void refreshSubFactions() {
        logger.debug("refreshSubFactions: parentFaction={}, fdFaction={}",
              cbFaction.getSelectedItem(), forceDesc.getFaction());
        FactionRecord oldFaction = (FactionRecord) cbSubFaction.getSelectedItem();
        cbSubFaction.removeActionListener(this);
        cbSubFaction.removeAllItems();
        String currentFaction = ((FactionRecord) Objects.requireNonNull(cbFaction.getSelectedItem())).getKey();
        if (currentFaction != null) {
            List<FactionRecord> sorted = RATGenerator.getInstance()
                  .getFactionList()
                  .stream()
                  .filter(fr -> fr.getKey().startsWith(currentFaction + ".") &&
                        fr.isActiveInYear(currentYear))
                  .sorted(Comparator.comparing(fr -> fr.getName(currentYear)))
                  .toList();
            cbSubFaction.addItem(null);
            sorted.forEach(fr -> cbSubFaction.addItem(fr));
        }
        cbSubFaction.setSelectedItem(oldFaction);
        if (cbSubFaction.getSelectedItem() == null) {
            forceDesc.setFaction(cbFaction.getSelectedItem().toString());
        } else {
            forceDesc.setFaction(cbSubFaction.getSelectedItem().toString());
        }
        refreshUnitTypes();
        cbSubFaction.addActionListener(this);
    }

    private void refreshUnitTypes() {
        logger.debug("refreshUnitTypes: fdFaction={}", forceDesc.getFaction());
        cbUnitType.removeActionListener(this);
        TOCNode tocNode = findTOCNode();
        if (tocNode == null) {
            logger.warn("refreshUnitTypes: no TOC node found for faction {}", forceDesc.getFaction());
        }
        Integer currentType = forceDesc.getUnitType();
        boolean hasCurrent = false;
        cbUnitType.removeAllItems();
        if (tocNode != null) {
            ValueNode n = tocNode.findUnitTypes(forceDesc);
            if (n != null) {
                for (String unitType : n.getContent().split(",")) {
                    if (unitType.equals("null")) {
                        cbUnitType.addItem(null);
                        if (currentType == null) {
                            hasCurrent = true;
                        }
                    } else {
                        cbUnitType.addItem(AbstractUnitRecord.parseUnitType(unitType));
                        if (currentType != null && UnitType.getTypeDisplayableName(currentType).equals(unitType)) {
                            hasCurrent = true;
                        }
                    }
                }
            } else {
                logger.warn("No unit type node found.");
                cbUnitType.addItem(null);
            }
        } else {
            cbUnitType.addItem(null);
        }

        if (hasCurrent) {
            cbUnitType.setSelectedItem(currentType);
        } else {
            Ruleset rs = Ruleset.findRuleset(forceDesc.getFaction());
            Integer unitType = rs.getDefaultUnitType(forceDesc);
            if (unitType == null && cbUnitType.getItemCount() > 0) {
                unitType = cbUnitType.getItemAt(0);
            }
            cbUnitType.setSelectedItem(unitType);
            forceDesc.setUnitType(unitType);
        }
        refreshFormations();
        cbUnitType.addActionListener(this);
    }

    private void refreshFormations() {
        logger.debug("refreshFormations: fdFaction={}, unitType={}",
              forceDesc.getFaction(), cbUnitType.getSelectedItem());
        cbFormation.removeActionListener(this);
        if (cbUnitType.getSelectedItem() != null) {
            Integer unitType = (Integer) cbUnitType.getSelectedItem();
            if (unitType != null) {
                panMissionRoleFilters.showFor(unitType);
            }
        }

        TOCNode tocNode = findTOCNode();
        String currentFormation = (String) cbFormation.getSelectedItem();
        boolean hasCurrent = false;
        Ruleset ruleset = Ruleset.findRuleset(forceDesc);
        cbFormation.removeAllItems();

        if (tocNode != null) {
            ValueNode n = tocNode.findEchelons(forceDesc);
            if (n != null) {
                formationDisplayNames.clear();
                for (String formation : n.getContent().split(",")) {
                    Ruleset rs = ruleset;
                    ForceNode fn;
                    do {
                        fn = rs.findForceNode(forceDesc,
                              MathUtility.parseInt(formation.replaceAll("[^0-9]", ""), 0),
                              formation.endsWith("^"));
                        if (fn == null) {
                            if (rs.getParent() != null) {
                                rs = Ruleset.findRuleset(rs.getParent());
                            } else {
                                rs = null;
                            }
                        }
                    } while (fn == null && rs != null);
                    String formName = (fn != null) ? fn.getEchelonName() : formation;
                    if (formation.endsWith("+")) {
                        formName = Messages.getString("ForceGeneratorDialog.reinforced") + formName;
                    }
                    if (formation.endsWith("-")) {
                        formName = Messages.getString("ForceGeneratorDialog.understrength") + formName;
                    }
                    formationDisplayNames.put(formation, formName);
                    cbFormation.addItem(formation);
                    if (currentFormation != null && currentFormation.equals(formation)) {
                        hasCurrent = true;
                    }
                }
            }
        } else {
            logger.warn("No echelon node found.");
        }

        if (hasCurrent) {
            cbFormation.setSelectedItem(currentFormation);
        } else {
            Ruleset rs = Ruleset.findRuleset(forceDesc.getFaction());
            String echelon = rs.getDefaultEschelon(forceDesc);
            if ((echelon == null || !formationDisplayNames.containsKey(echelon) && cbFormation.getItemCount() > 0)) {
                echelon = cbFormation.getItemAt(0);
            }
            if (echelon != null) {
                cbFormation.setSelectedItem(echelon);
                setFormation(echelon);
            }
        }

        refreshRatings();
        cbFormation.addActionListener(this);
    }

    private void refreshRatings() {
        logger.debug("refreshRatings: fdFaction={}, echelon={}",
              forceDesc.getFaction(), forceDesc.getEchelon());
        cbRating.removeActionListener(this);
        TOCNode tocNode = findTOCNode();
        cbRating.removeAllItems();
        ratingDisplayNames.clear();
        if (tocNode != null) {
            ValueNode n = tocNode.findRatings(forceDesc);
            if (n != null && n.getContent() != null) {
                for (String rating : n.getContent().split(",")) {
                    // Display every entry as "<Brief Description> (CODE)". Clan/RotS entries carry
                    // their display name in the data ("FL:Front Line"); bare letter codes (A-F,
                    // Keshik) get theirs from the ForceGeneratorDialog.rating.* message keys. A code
                    // with no description anywhere falls back to the raw code.
                    final String code;
                    String description;
                    if (rating.contains(":")) {
                        String[] fields = rating.split(":");
                        code = fields[0];
                        description = fields[1];
                    } else {
                        code = rating;
                        description = ratingDescription(code);
                    }
                    cbRating.addItem(code);
                    ratingDisplayNames.put(code,
                          (description == null) ? code : description + " (" + code + ")");
                }
            } else {
                logger.warn("No rating found.");
            }
        }

        Ruleset rs = Ruleset.findRuleset(forceDesc.getFaction());
        String rating = rs.getDefaultRating(forceDesc);
        if (rating == null && cbRating.getItemCount() > 0) {
            // Every shipped ruleset with TOC ratings declares a default; falling back to the first
            // entry keeps a data gap visible instead of leaving the picker and descriptor split.
            rating = cbRating.getItemAt(0);
            logger.warn("Ruleset for {} offers ratings but declares no default; selecting {}",
                  forceDesc.getFaction(), rating);
        }
        if (rating != null) {
            cbRating.setSelectedItem(rating);
            forceDesc.setRating(rating);
        }
        refreshFlags();
        cbRating.addActionListener(this);
    }

    /**
     * The brief description for a bare rating code (for example {@code C} - "Standard"), read from
     * the {@code ForceGeneratorDialog.rating.<code>} message keys so it localizes with the rest of
     * the dialog.
     *
     * @param code the rating code as it appears in the ruleset TOC
     *
     * @return the description, or {@code null} when no key is defined for the code
     */
    private static @Nullable String ratingDescription(String code) {
        String text = Messages.getString("ForceGeneratorDialog.rating." + code);
        // Messages.getString returns !key! when the key is missing.
        return text.startsWith("!") ? null : text;
    }

    private void refreshFlags() {
        cbFlags.removeActionListener(this);
        TOCNode tocNode = findTOCNode();
        cbFlags.removeAllItems();
        cbFlags.addItem(null);
        if (tocNode != null) {
            ValueNode n = tocNode.findFlags(forceDesc);
            if (n != null && n.getContent() != null) {
                for (String flag : n.getContent().split(",")) {
                    if (flag.contains(":")) {
                        String[] fields = flag.split(":");
                        flagDisplayNames.put(fields[0], fields[1]);
                        cbFlags.addItem(fields[0]);
                    } else {
                        flagDisplayNames.put(flag, flag);
                        cbFlags.addItem(flag);
                    }
                }
            }
        }

        cbFlags.setSelectedIndex(0);
        forceDesc.getFlags().clear();
        if (cbFlags.getSelectedItem() != null) {
            forceDesc.getFlags().add((String) cbFlags.getSelectedItem());
        }
        cbFlags.addActionListener(this);
    }

    private TOCNode findTOCNode() {
        Ruleset rs = Ruleset.findRuleset(forceDesc);
        if (null == rs) {
            return null;
        }
        TOCNode toc;
        do {
            toc = rs.getTOCNode();
            if (toc == null) {
                if (rs.getParent() == null) {
                    rs = null;
                } else {
                    rs = Ruleset.findRuleset(rs.getParent());
                }
            }
        } while (rs != null && toc == null);
        return toc;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if (ev.getSource() == cbFaction) {
            logger.debug("cbFaction action: selected={}, year={}", cbFaction.getSelectedItem(), currentYear);
            if (cbFaction.getSelectedItem() != null) {
                forceDesc.setFaction(((FactionRecord) cbFaction.getSelectedItem()).getKey());
            }
            refreshSubFactions();
            notifyFactionChanged();
        } else if (ev.getSource() == cbSubFaction) {
            logger.debug("cbSubFaction action: selected={}", cbSubFaction.getSelectedItem());
            if (cbSubFaction.getSelectedItem() != null) {
                forceDesc.setFaction(((FactionRecord) cbSubFaction.getSelectedItem()).getKey());
                notifyFactionChanged();
            } else {
                forceDesc.setFaction(((FactionRecord) Objects.requireNonNull(cbFaction.getSelectedItem())).getKey());
            }
            refreshUnitTypes();
        } else if (ev.getSource() == cbUnitType) {
            logger.debug("cbUnitType action: selected={}", cbUnitType.getSelectedItem());
            forceDesc.setUnitType((Integer) cbUnitType.getSelectedItem());
            refreshFormations();
        } else if (ev.getSource() == cbFormation) {
            String echelon = (String) cbFormation.getSelectedItem();
            if (echelon != null) {
                setFormation(echelon);
            }
            refreshRatings();
        } else if (ev.getSource() == cbRating) {
            forceDesc.setRating((String) cbRating.getSelectedItem());
            refreshFlags();
        } else if (ev.getSource() == cbExperience) {
            if (cbExperience.getSelectedIndex() == 0) {
                forceDesc.setExperience(null);
            } else {
                forceDesc.setExperience(cbExperience.getSelectedIndex() - 1);
            }
            refreshFlags();
        } else if (ev.getSource() == cbFlags) {
            forceDesc.getFlags().clear();
            if (cbFlags.getSelectedItem() != null) {
                forceDesc.getFlags().add((String) cbFlags.getSelectedItem());
            }
        } else if (ev.getSource() == cbWeightClass) {
            // Use getSelectedItem() so the stored value is the actual EntityWeightClass
            // constant rather than the dropdown index. Index-and-value match today (1..4)
            // but only by coincidence — defensive against future re-ordering or insertion
            // of new entries like Ultra Light.
            Object item = cbWeightClass.getSelectedItem();
            forceDesc.setWeightClass(item instanceof Integer ? (Integer) item : null);
        } else if (ev.getSource() == btnGenerate) {
            generateForce();
            btnExportMUL.setEnabled(true);
            btnClear.setEnabled(true);
        } else if (ev.getSource() == btnExportMUL) {
            if (onExportMUL != null) {
                onExportMUL.accept(forceDesc);
            } else {
                exportMUL(forceDesc);
            }
        } else if (ev.getSource() == btnClear) {
            clearForce();
            btnExportMUL.setEnabled(false);
            btnClear.setEnabled(false);
        }

        if (changesFormationOffer(ev.getSource())) {
            refreshInlineFormationMixEditor();
        }
    }

    /**
     * Whether changing this control changes the formations the force offers.
     *
     * <p>The offer is read out of the ruleset for the force the selections describe, so every selection that feeds
     * that description invalidates it. Detachments are in the list because the aerospace and infantry formations are
     * only offered when the force generates detachments to put them in.</p>
     *
     * @param source the control that fired
     *
     * @return {@code true} when the inline editor needs rebuilding
     */
    private boolean changesFormationOffer(Object source) {
        return (source == cbFaction)
              || (source == cbSubFaction)
              || (source == cbUnitType)
              || (source == cbFormation)
              || (source == cbRating)
              || (source == cbFlags)
              || (source == cbExperience)
              || (source == cbWeightClass)
              || (source == chkDetachments);
    }

    /**
     * Shows or hides the Generate button. Embedders that drive generation through their own controls
     * (e.g. an OK button on a parent dialog) hide the built-in button.
     */
    public void setGenerateButtonVisible(boolean visible) {
        btnGenerate.setVisible(visible);
    }

    /**
     * Shows or hides the Export MUL button. Embedders that route the export through their own UI hide it.
     */
    public void setExportMULButtonVisible(boolean visible) {
        btnExportMUL.setVisible(visible);
    }

    /**
     * Shows or hides the Clear button.
     */
    public void setClearButtonVisible(boolean visible) {
        btnClear.setVisible(visible);
    }

    /**
     * Sets a custom handler for the Export-MUL button. When non-null, the built-in {@link #exportMUL} call is
     * replaced by this consumer; the panel passes the live {@link ForceDescriptor} for the embedder to handle.
     * Pass {@code null} to restore default behavior.
     */
    public void setOnExportMUL(Consumer<ForceDescriptor> handler) {
        this.onExportMUL = handler;
    }

    /**
     * Sets a handler notified whenever the selected faction changes, so an embedder can adjust its own
     * settings to suit - MekHQ's Command Designer uses it to switch formation naming to the Greek
     * alphabet when a Clan is picked.
     *
     * <p>Fires for both the faction and sub-faction selectors, since either can change which faction a
     * force is generated for.</p>
     *
     * @param handler the handler to notify, or {@code null} to stop notifying
     */
    public void setOnFactionChanged(@Nullable Consumer<FactionRecord> handler) {
        this.onFactionChanged = handler;
        // Report the current selection straight away. The faction is seeded before an embedder gets
        // the chance to register, so a handler that only ever heard about changes would never learn
        // the faction the panel opened on - which is the usual case, since most users generate for
        // the faction it starts on without touching the selector.
        notifyFactionChanged();
    }

    /**
     * Tells the embedder which faction is now selected, preferring the sub-faction where one is chosen
     * because that is the faction the force is actually generated for.
     */
    private void notifyFactionChanged() {
        if (onFactionChanged == null) {
            logger.debug("[FactionChanged] faction changed but no host handler is registered");
            return;
        }
        // The sub-faction is the more specific choice, but Clan-ness belongs to the parent - a sub
        // faction of a Clan is still a Clan, and its own record does not always say so. Report
        // whichever of the two actually identifies as a Clan so an embedder keying off that is not
        // misled by the refinement.
        Object subFaction = cbSubFaction.getSelectedItem();
        Object parentFaction = cbFaction.getSelectedItem();
        Object selected = (subFaction != null) ? subFaction : parentFaction;
        if ((parentFaction instanceof FactionRecord parentRecord) && parentRecord.isClan()
                  && !(selected instanceof FactionRecord chosen && chosen.isClan())) {
            selected = parentFaction;
        }
        if (selected instanceof FactionRecord factionRecord) {
            logger.debug("[FactionChanged] notifying host: faction={} isClan={}",
                  factionRecord.getKey(), factionRecord.isClan());
            onFactionChanged.accept(factionRecord);
        } else {
            logger.debug("[FactionChanged] no FactionRecord selected; nothing to notify");
        }
    }

    /**
     * Makes the year text field read-only. Use this when an embedder anchors the year to an external value
     * (e.g. MekHQ's campaign year) and doesn't want the user editing it on this panel.
     */
    /**
     * Appends a host-supplied toggle to the row of options beside the Generate button, so a host's own
     * generation options read as part of that action rather than as a separate setting elsewhere.
     *
     * <p>Used by MekHQ's Command Designer for options that belong to the campaign layer (for example
     * "Generate Company Command Lance") and therefore cannot live in this view.</p>
     *
     * @param option the control to append; ignored when {@code null}
     */
    public void addGenerateOption(@Nullable JComponent option) {
        if ((option == null) || (panGenerateOptions == null)) {
            return;
        }
        panGenerateOptions.add(option);
        panGenerateOptions.revalidate();
        panGenerateOptions.repaint();
    }

    /**
     * A label that carries its setting's explanation, so hovering the name of a control describes it
     * rather than only hovering the input box beside it.
     *
     * @param messageKey the label's message key; its tooltip is the same key suffixed {@code .tooltip}
     *
     * @return the label, with tooltip attached when one is defined
     */
    private static JLabel describedLabel(String messageKey) {
        JLabel label = new JLabel(Messages.getString(messageKey));
        label.setToolTipText(Messages.getString(messageKey + ".tooltip"));
        return label;
    }

    public void setYearFieldEditable(boolean editable) {
        txtYear.setEditable(editable);
    }

    /**
     * Programmatically picks a faction in the embedded picker. Embedders (e.g. MekHQ) call this to
     * seed the picker with their campaign's faction so the dialog opens pre-aligned instead of
     * defaulting to "IS". Looks up the FactionRecord from the loaded RATGenerator data; if the
     * code doesn't match a known faction, the picker is left unchanged and {@code false} is
     * returned.
     *
     * <p>The picker's existing {@link ActionListener} fires as a result of the
     * {@code setSelectedItem} call, so the descriptor is updated as if the user had picked the
     * faction by hand.</p>
     *
     * @param factionCode the short-name faction code (e.g. {@code "CHH"}, {@code "LC"},
     *                    {@code "FS"})
     * @return {@code true} if a matching faction was found and selected; {@code false} otherwise
     */
    public boolean setSelectedFaction(String factionCode) {
        if (factionCode == null || factionCode.isBlank()) {
            return false;
        }
        FactionRecord faction = RATGenerator.getInstance().getFaction(factionCode);
        if (faction == null) {
            return false;
        }
        cbFaction.setSelectedItem(faction);
        return true;
    }

    public void exportMUL(ForceDescriptor fd) {
        ArrayList<Entity> list = new ArrayList<>();
        fd.addAllEntities(list);
        if (list.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                  Messages.getString("ForceGeneratorDialog.exportMUL.empty"),
                  Messages.getString("ForceGeneratorDialog.exportMUL.title"),
                  JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Create a fake game so we can write the entities to a file without adding them
        // to the real game.
        Game game = new Game();
        // Add a player to prevent complaining in the log file
        Player p = new Player(1, "Observer");
        game.addPlayer(1, p);
        game.setOptions(gameOptions);
        list.forEach(en -> {
            en.setOwner(p);
            // If we don't set the id, the first unit will be left at -1, which in most
            // cases is interpreted
            // as no entity
            en.setId(game.getNextEntityId());
            game.addEntity(en);
        });
        C3NetworkConfigurator.configure(fd);

        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle(Messages.getString("ForceGeneratorDialog.exportMUL.title"));
        chooser.setFileFilter(new FileNameExtensionFilter(
              Messages.getString("ClientGUI.descriptionMULFiles"), "mul"));
        // Sanitize the force name so it works as a filename on Windows and other OSes.
        String sanitized = fd.parseName().replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (sanitized.isEmpty()) {
            sanitized = "force";
        }
        chooser.setSelectedFile(new File(sanitized + ".mul"));

        int result = chooser.showSaveDialog(this);
        if (result != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
            return;
        }
        File unitFile = chooser.getSelectedFile();
        String lowerName = unitFile.getName().toLowerCase();
        if (!lowerName.endsWith(".mul") && !lowerName.endsWith(".xml")) {
            try {
                unitFile = new File(unitFile.getCanonicalPath() + ".mul");
            } catch (IOException e) {
                logger.error(e, "exportMUL: failed to canonicalize selected file");
                JOptionPane.showMessageDialog(this,
                      Messages.getString("ForceGeneratorDialog.exportMUL.error") + "\n" + e.getMessage(),
                      Messages.getString("ForceGeneratorDialog.exportMUL.title"),
                      JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        try {
            EntityListFile.saveTo(unitFile, list);
            logger.info("exportMUL: wrote {} entities to {}", list.size(), unitFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error(e, "exportMUL: save failed");
            JOptionPane.showMessageDialog(this,
                  Messages.getString("ForceGeneratorDialog.exportMUL.error") + "\n" + e.getMessage(),
                  Messages.getString("ForceGeneratorDialog.exportMUL.title"),
                  JOptionPane.ERROR_MESSAGE);
        }
    }


    private void setFormation(String echelon) {
        forceDesc.setEchelon(MathUtility.parseInt(echelon.replaceAll("[^0-9]", ""), 0));
        forceDesc.setAugmented(echelon.contains("^"));
        if (echelon.endsWith("+")) {
            forceDesc.setSizeMod(1);
        } else if (echelon.endsWith("-")) {
            forceDesc.setSizeMod(-1);
        } else {
            forceDesc.setSizeMod(0);
        }
    }

    public void setCurrentYear(int year) {
        currentYear = year;
        yearUpdated();
    }

    /**
     * Worker function that updates various things that need to be updated when the year is changed.
     */
    private void yearUpdated() {
        txtYear.setText(String.valueOf(currentYear));
        RATGenerator.getInstance().loadYear(currentYear);
        forceDesc.setYear(currentYear);
        refreshFactions();
        refreshInlineFormationMixEditor();
    }

    @Override
    public void focusGained(FocusEvent evt) {
        // Do nothing
    }

    @Override
    public void focusLost(FocusEvent evt) {
        currentYear = MathUtility.parseInt(txtYear.getText(), RATGenerator.getInstance().getEraSet().first());
        if (currentYear < RATGenerator.getInstance().getEraSet().first()) {
            currentYear = RATGenerator.getInstance().getEraSet().first();
        } else if (currentYear > RATGenerator.getInstance().getEraSet().last()) {
            currentYear = RATGenerator.getInstance().getEraSet().last();
        }
        yearUpdated();
    }

    static class CBRenderer<T> extends DefaultListCellRenderer {
        @Serial
        private static final long serialVersionUID = 4895258839502183158L;

        private final String nullVal;
        private final Function<T, String> toString;

        public CBRenderer(String nullVal, Function<T, String> strConverter) {
            this.nullVal = nullVal;
            toString = Objects.requireNonNullElseGet(strConverter, () -> Object::toString);
        }

        @SuppressWarnings(value = "unchecked")
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object entry, int position, boolean arg3,
              boolean arg4) {
            if (entry == null) {
                setText(nullVal);
            } else {
                setText(toString.apply((T) entry));
            }
            return this;
        }
    }

    private class GenerateTask extends SwingWorker<ForceDescriptor, Double> implements ProgressListener {
        private final ForceDescriptor fd;

        private final Object progressLock = new Object();
        private double progress = 0;
        private String message = "";

        GenerateTask(ForceDescriptor fd) {
            this.fd = fd;
        }

        @Override
        protected ForceDescriptor doInBackground() {
            btnGenerate.setEnabled(false);
            Ruleset.findRuleset(fd).processRoot(fd, this);
            // Fitted as the force is generated rather than when it is added to a game, so the crews
            // carry their implants everywhere the generated force goes - the preview, an exported MUL
            // and the lobby alike.
            ManeiDominiCrewAugmentor.augment(fd, gameOptions);
            // The Clans' own augmentation, unrelated to the Manei Domini and gated on its own
            // rule. Clan-ness comes from the faction record rather than the key, sub-factions
            // of a Clan being Clans.
            FactionRecord factionRecord = RATGenerator.getInstance().getFaction(fd.getFaction());
            ClanEnhancedImagingAugmentor.augment(fd,
                  (factionRecord != null) && factionRecord.isClan(), gameOptions);
            return fd;
        }

        @Override
        protected void done() {
            try {
                // Do NOT alias the input descriptor (forceDesc) to the generated root: forceDesc is
                // mutated by the Unit Type / Formation dropdowns, so aliasing it would let a later
                // roll's dropdown changes rewrite an already-generated (and possibly accumulated) root.
                // Keep the generated force independent.
                ForceDescriptor generated = get();
                logger.info("[ForceGen] generated root id={} name='{}' unitType={} echelon={} weight={} subForces={}",
                      System.identityHashCode(generated), generated.getName(), generated.getUnitType(),
                      generated.getEchelon(), generated.getWeightClass(),
                      generated.getSubForces() == null ? 0 : generated.getSubForces().size());
                updateSummaryTable(generated);
                if (onGenerate != null) {
                    onGenerate.accept(generated);
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                logger.warn("[ForceGen] generation was interrupted; no force was produced");
            } catch (ExecutionException ex) {
                // Generation runs on a worker, so a failure here is invisible to the player - the
                // button simply appears to do nothing. Log the cause with enough context to identify
                // it, and say plainly that no force was produced.
                logger.error(ex, "[ForceGen] generation FAILED for faction={} year={} unitType={}"
                            + " echelon={}; no force was produced",
                      fd.getFaction(), fd.getYear(), fd.getUnitType(), fd.getEchelon());
            } finally {
                btnGenerate.setEnabled(true);
            }
        }

        @Override
        public void updateProgress(double progress, String message) {
            int progressPercent;
            synchronized (progressLock) {
                this.progress += progress;
                this.message = message;

                progressPercent = Math.min((int) Math.round(this.progress * 100.0), 100);
            }

            setProgress(progressPercent);
        }

        public String getMessage() {
            synchronized (progressLock) {
                return message;
            }
        }
    }
}
