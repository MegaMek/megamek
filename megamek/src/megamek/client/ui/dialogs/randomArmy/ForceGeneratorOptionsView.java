/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
    private JCheckBox chkAttachments;

    private final DefaultListCellRenderer factionRenderer = new CBRenderer<FactionRecord>(Messages.getString(
          "ForceGeneratorDialog.general"), fRec -> fRec.getName(currentYear));

    private final HashMap<String, String> ratingDisplayNames = new HashMap<>();
    private final HashMap<String, String> formationDisplayNames = new HashMap<>();
    private final HashMap<String, String> flagDisplayNames = new HashMap<>();

    private JPanel panGroundRole;
    private JPanel panInfRole;
    private JPanel panAirRole;

    private JCheckBox chkRoleRecon;
    private JCheckBox chkRoleFireSupport;
    private JCheckBox chkRoleUrban;
    private JCheckBox chkRoleInfantrySupport;
    private JCheckBox chkRoleCavalry;
    private JCheckBox chkRoleRaider;
    private JCheckBox chkRoleIncendiary;
    private JCheckBox chkRoleAntiAircraft;
    private JCheckBox chkRoleAntiInfantry;
    private JCheckBox chkRoleArtillery;
    private JCheckBox chkRoleMissileArtillery;
    private JCheckBox chkRoleTransport;
    private JCheckBox chkRoleEngineer;

    private JCheckBox chkRoleFieldGun;
    private JCheckBox chkRoleFieldArtillery;
    private JCheckBox chkRoleFieldMissileArtillery;

    private JCheckBox chkRoleAirRecon;
    private JCheckBox chkRoleGroundSupport;
    private JCheckBox chkRoleInterceptor;
    private JCheckBox chkRoleAssault;
    private JCheckBox chkRoleAirTransport;

    private JTextField txtDropshipPct;
    private JTextField txtJumpshipPct;
    private JTextField txtWarshipPct;
    private JTextField txtCargo;
    private JCheckBox chkFighterComplement;

    /** Post-generation summary: unit type rows, Light/Medium/Heavy/Assault columns. */
    private JTable tblSummary;
    private DefaultTableModel summaryModel;

    private JButton btnGenerate;
    private JButton btnExportMUL;
    private JButton btnClear;

    private final GameOptions gameOptions;

    public ForceGeneratorOptionsView(Consumer<ForceDescriptor> onGenerate, GameOptions gameOptions) {
        this.onGenerate = onGenerate;
        this.gameOptions = gameOptions;
        if (!Ruleset.isInitialized()) {
            Ruleset.loadData();
        }
        initUi();
    }

    private void initUi() {
        currentYear = gameOptions.intOption(OptionsConstants.ALLOWED_YEAR);
        forceDesc.setYear(currentYear);
        RATGenerator rg = RATGenerator.getInstance();
        rg.loadYear(currentYear);

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int inset = UIUtil.scaleForGUI(5);
        gbc.insets = new Insets(inset, inset, inset, inset);

        int y = 0;

        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.year")), gbc);
        txtYear = new JTextField();
        txtYear.setEditable(true);
        txtYear.setText(Integer.toString(currentYear));
        txtYear.setToolTipText(Messages.getString("ForceGeneratorDialog.year.tooltip"));
        gbc.gridx = 1;
        gbc.gridy = y++;
        add(txtYear, gbc);
        txtYear.addFocusListener(this);
        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.faction")), gbc);
        cbFaction = new JComboBox<>();
        cbFaction.setRenderer(factionRenderer);
        gbc.gridx = 1;
        gbc.gridy = y;
        add(cbFaction, gbc);
        cbFaction.setToolTipText(Messages.getString("ForceGeneratorDialog.faction.tooltip"));
        cbFaction.addActionListener(this);

        gbc.gridx = 2;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.subfaction")), gbc);
        cbSubFaction = new JComboBox<>();
        cbSubFaction.setRenderer(factionRenderer);
        gbc.gridx = 3;
        gbc.gridy = y++;
        add(cbSubFaction, gbc);
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

        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.unitType")), gbc);
        cbUnitType = new JComboBox<>();
        cbUnitType.setRenderer(new CBRenderer<>(Messages.getString("ForceGeneratorDialog.combined"),
              UnitType::getTypeName));
        gbc.gridx = 1;
        gbc.gridy = y;
        add(cbUnitType, gbc);
        cbUnitType.setToolTipText(Messages.getString("ForceGeneratorDialog.unitType.tooltip"));
        cbUnitType.addActionListener(this);

        gbc.gridx = 2;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.formation")), gbc);
        cbFormation = new JComboBox<>();
        cbFormation.setRenderer(new CBRenderer<String>(Messages.getString("ForceGeneratorDialog.random"),
              formationDisplayNames::get));
        gbc.gridx = 3;
        gbc.gridy = y++;
        add(cbFormation, gbc);
        cbFormation.setToolTipText(Messages.getString("ForceGeneratorDialog.formation.tooltip"));
        cbFormation.addActionListener(this);

        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.rating")), gbc);
        cbRating = new JComboBox<>();
        cbRating.setRenderer(new CBRenderer<String>(Messages.getString("ForceGeneratorDialog.random"),
              ratingDisplayNames::get));
        gbc.gridx = 1;
        gbc.gridy = y;
        add(cbRating, gbc);
        cbRating.setToolTipText(Messages.getString("ForceGeneratorDialog.rating.tooltip"));
        cbRating.addActionListener(this);

        gbc.gridx = 2;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.weight")), gbc);
        cbWeightClass = new JComboBox<>();
        cbWeightClass.setRenderer(new CBRenderer<Integer>(Messages.getString("ForceGeneratorDialog.random"),
              EntityWeightClass::getClassName));
        cbWeightClass.addItem(null);
        cbWeightClass.addItem(EntityWeightClass.WEIGHT_LIGHT);
        cbWeightClass.addItem(EntityWeightClass.WEIGHT_MEDIUM);
        cbWeightClass.addItem(EntityWeightClass.WEIGHT_HEAVY);
        cbWeightClass.addItem(EntityWeightClass.WEIGHT_ASSAULT);
        gbc.gridx = 3;
        gbc.gridy = y++;
        add(cbWeightClass, gbc);
        cbWeightClass.setToolTipText(Messages.getString("ForceGeneratorDialog.weight.tooltip"));
        cbWeightClass.addActionListener(this);

        gbc.gridx = 0;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.other")), gbc);
        cbFlags = new JComboBox<>();
        cbFlags.setRenderer(new CBRenderer<String>("---", flagDisplayNames::get));
        gbc.gridx = 1;
        gbc.gridy = y;
        add(cbFlags, gbc);
        cbFlags.setToolTipText(Messages.getString("ForceGeneratorDialog.other.tooltip"));
        cbFlags.addActionListener(this);

        gbc.gridx = 2;
        gbc.gridy = y;
        add(new JLabel(Messages.getString("ForceGeneratorDialog.experience")), gbc);
        cbExperience = new JComboBox<>();
        cbExperience.addItem(Messages.getString("ForceGeneratorDialog.random"));
        cbExperience.addItem(Messages.getString("ForceGeneratorDialog.green"));
        cbExperience.addItem(Messages.getString("ForceGeneratorDialog.regular"));
        cbExperience.addItem(Messages.getString("ForceGeneratorDialog.veteran"));
        cbExperience.addItem(Messages.getString("ForceGeneratorDialog.elite"));
        gbc.gridx = 3;
        gbc.gridy = y++;
        add(cbExperience, gbc);
        cbExperience.setToolTipText(Messages.getString("ForceGeneratorDialog.experience.tooltip"));
        cbExperience.addActionListener(this);

        gbc.gridx = 0;
        gbc.gridy = y++;
        gbc.gridwidth = 2;
        chkAttachments = new JCheckBox(Messages.getString("ForceGeneratorDialog.includeSupportForces"));
        chkAttachments.setToolTipText(Messages.getString("ForceGeneratorDialog.includeSupportForces.tooltip"));
        chkAttachments.setSelected(true);
        add(chkAttachments, gbc);

        gbc.gridwidth = 4;
        panGroundRole = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = y++;
        add(panGroundRole, gbc);

        panInfRole = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = y++;
        add(panInfRole, gbc);
        panInfRole.setVisible(false);

        panAirRole = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = y++;
        add(panAirRole, gbc);
        panAirRole.setVisible(false);

        gbc.gridx = 0;
        gbc.gridy = y++;

        JPanel panTransport = new JPanel(new GridLayout(5, 2));
        txtDropshipPct = new JTextField("0");
        txtDropshipPct.setToolTipText(Messages.getString("ForceGeneratorDialog.dropshipPercentage.tooltip"));
        txtJumpshipPct = new JTextField("0");
        txtJumpshipPct.setToolTipText(Messages.getString("ForceGeneratorDialog.jumpshipPercentage.tooltip"));
        txtWarshipPct = new JTextField("0");
        txtWarshipPct.setToolTipText(Messages.getString("ForceGeneratorDialog.warshipPercentage.tooltip"));
        txtCargo = new JTextField("0");
        txtCargo.setToolTipText(Messages.getString("ForceGeneratorDialog.cargo.tooltip"));
        panTransport.add(new JLabel(Messages.getString("ForceGeneratorDialog.dropshipPercentage")));
        panTransport.add(txtDropshipPct, gbc);
        panTransport.add(new JLabel(Messages.getString("ForceGeneratorDialog.jumpshipPercentage")));
        panTransport.add(txtJumpshipPct, gbc);
        panTransport.add(new JLabel(Messages.getString("ForceGeneratorDialog.warshipPercentage")));
        panTransport.add(txtWarshipPct, gbc);
        panTransport.add(new JLabel(Messages.getString("ForceGeneratorDialog.cargo")));
        panTransport.add(txtCargo, gbc);
        chkFighterComplement = new JCheckBox(Messages.getString("ForceGeneratorDialog.fighterComplement"));
        chkFighterComplement.setToolTipText(Messages.getString("ForceGeneratorDialog.fighterComplement.tooltip"));
        panTransport.add(chkFighterComplement);
        panTransport.add(new JLabel(""));
        panTransport.setBorder(BorderFactory.createTitledBorder(Messages.getString("ForceGeneratorDialog.transport")));

        // Pair the Transport panel with the post-generation Composition Summary table inside a single
        // BorderLayout container so the summary absorbs whatever horizontal slack the GridBag's
        // column-driven layout would otherwise leave between them. Transport sits at its preferred
        // width on the WEST; the summary fills the rest of the row in the CENTER. Spans gridwidth=4
        // to occupy the full dialog row, matching how panGroundRole / panInfRole are laid out above.
        JPanel transportAndSummary = new JPanel(new BorderLayout(10, 0));
        transportAndSummary.add(panTransport, BorderLayout.WEST);
        JScrollPane panSummary = createSummaryTable();
        transportAndSummary.add(panSummary, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.gridy = y++;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1.0;
        add(transportAndSummary, gbc);
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0;

        btnGenerate = new JButton(Messages.getString("ForceGeneratorDialog.generate"));
        btnGenerate.setToolTipText(Messages.getString("ForceGeneratorDialog.generate.tooltip"));
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.weighty = 1.0;
        add(btnGenerate, gbc);
        btnGenerate.addActionListener(this);

        btnExportMUL = new JButton(Messages.getString("ForceGeneratorDialog.exportMUL"));
        btnExportMUL.setToolTipText(Messages.getString("ForceGeneratorDialog.exportMUL.tooltip"));
        gbc.gridx = 1;
        gbc.gridy = y;
        add(btnExportMUL, gbc);
        btnExportMUL.addActionListener(this);
        btnExportMUL.setEnabled(false);

        btnClear = new JButton(Messages.getString("ForceGeneratorDialog.clear"));
        btnClear.setToolTipText(Messages.getString("ForceGeneratorDialog.clear.tooltip"));
        gbc.gridx = 2;
        gbc.gridy = y;
        gbc.weighty = 1.0;
        add(btnClear, gbc);
        btnClear.addActionListener(this);
        btnClear.setEnabled(false);

        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;

        chkRoleRecon = createMissionRoleCheck(MissionRole.RECON);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panGroundRole.add(chkRoleRecon, gbc);

        chkRoleFireSupport = createMissionRoleCheck(MissionRole.FIRE_SUPPORT);
        gbc.gridx = 1;
        gbc.gridy = 0;
        panGroundRole.add(chkRoleFireSupport, gbc);

        chkRoleUrban = createMissionRoleCheck(MissionRole.URBAN);
        gbc.gridx = 2;
        gbc.gridy = 0;
        panGroundRole.add(chkRoleUrban, gbc);

        chkRoleCavalry = createMissionRoleCheck(MissionRole.CAVALRY);
        gbc.gridx = 3;
        gbc.gridy = 0;
        panGroundRole.add(chkRoleCavalry, gbc);

        chkRoleRaider = createMissionRoleCheck(MissionRole.RAIDER);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panGroundRole.add(chkRoleRaider, gbc);

        chkRoleIncendiary = createMissionRoleCheck(MissionRole.INCENDIARY);
        gbc.gridx = 1;
        gbc.gridy = 1;
        panGroundRole.add(chkRoleIncendiary, gbc);

        chkRoleAntiAircraft = createMissionRoleCheck(MissionRole.ANTI_AIRCRAFT);
        gbc.gridx = 2;
        gbc.gridy = 1;
        panGroundRole.add(chkRoleAntiAircraft, gbc);

        chkRoleAntiInfantry = createMissionRoleCheck(MissionRole.ANTI_INFANTRY);
        gbc.gridx = 3;
        gbc.gridy = 1;
        panGroundRole.add(chkRoleAntiInfantry, gbc);

        chkRoleArtillery = createMissionRoleCheck(MissionRole.ARTILLERY);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panGroundRole.add(chkRoleArtillery, gbc);

        chkRoleMissileArtillery = createMissionRoleCheck(MissionRole.MISSILE_ARTILLERY);
        gbc.gridx = 1;
        gbc.gridy = 2;
        panGroundRole.add(chkRoleMissileArtillery, gbc);

        chkRoleInfantrySupport = createMissionRoleCheck(MissionRole.INF_SUPPORT);
        gbc.gridx = 2;
        gbc.gridy = 2;
        panGroundRole.add(chkRoleInfantrySupport, gbc);

        chkRoleTransport = createMissionRoleCheck(MissionRole.CARGO);
        gbc.gridx = 0;
        gbc.gridy = 3;
        panGroundRole.add(chkRoleTransport, gbc);

        chkRoleEngineer = createMissionRoleCheck(MissionRole.ENGINEER);
        gbc.gridx = 1;
        gbc.gridy = 3;
        panGroundRole.add(chkRoleEngineer, gbc);

        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;

        chkRoleFieldGun = createMissionRoleCheck(MissionRole.FIELD_GUN);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panInfRole.add(chkRoleFieldGun, gbc);

        chkRoleFieldArtillery = createMissionRoleCheck(MissionRole.ARTILLERY);
        gbc.gridx = 1;
        gbc.gridy = 0;
        panInfRole.add(chkRoleFieldArtillery, gbc);

        chkRoleFieldMissileArtillery = createMissionRoleCheck(MissionRole.MISSILE_ARTILLERY);
        gbc.gridx = 2;
        gbc.gridy = 0;
        panInfRole.add(chkRoleFieldMissileArtillery, gbc);

        gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;

        chkRoleAirRecon = createMissionRoleCheck(MissionRole.RECON);
        gbc.gridx = 0;
        gbc.gridy = 0;
        panAirRole.add(chkRoleAirRecon, gbc);

        chkRoleGroundSupport = createMissionRoleCheck(MissionRole.GROUND_SUPPORT);
        gbc.gridx = 1;
        gbc.gridy = 0;
        panAirRole.add(chkRoleGroundSupport, gbc);

        chkRoleInterceptor = createMissionRoleCheck(MissionRole.INTERCEPTOR);
        gbc.gridx = 2;
        gbc.gridy = 0;
        panAirRole.add(chkRoleInterceptor, gbc);

        JCheckBox chkRoleEscort = createMissionRoleCheck(MissionRole.ESCORT);
        gbc.gridx = 0;
        gbc.gridy = 1;
        panAirRole.add(chkRoleEscort, gbc);

        JCheckBox chkRoleBomber = createMissionRoleCheck(MissionRole.BOMBER);
        gbc.gridx = 1;
        gbc.gridy = 1;
        panAirRole.add(chkRoleBomber, gbc);

        chkRoleAssault = createMissionRoleCheck(MissionRole.ASSAULT);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panAirRole.add(chkRoleAssault, gbc);

        chkRoleAirTransport = createMissionRoleCheck(MissionRole.CARGO);
        gbc.gridx = 1;
        gbc.gridy = 2;
        panAirRole.add(chkRoleAirTransport, gbc);

        refreshFactions();
    }

    private JCheckBox createMissionRoleCheck(MissionRole role) {
        String key = "MissionRole." + role.toString().toLowerCase();
        JCheckBox chk = new JCheckBox(Messages.getString(key));
        chk.setToolTipText(Messages.getString(key + ".tooltip"));
        return chk;
    }

    /**
     * Builds a {@link ForceDescriptor} from the current state of this options view's controls. Public so embedders
     * (e.g. MekHQ's company-generation dialog) can reuse the same input mapping without going through
     * {@link #generateForce()}'s SwingWorker plumbing. Does not run {@link Ruleset#processRoot} or any IO.
     */
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
        fd.setAttachments(chkAttachments.isSelected());
        if (forceDesc.getUnitType() != null) {
            switch (forceDesc.getUnitType()) {
                case UnitType.MEK:
                case UnitType.TANK:
                    if (chkRoleRecon.isSelected()) {
                        fd.getRoles().add(MissionRole.RECON);
                    }
                    if (chkRoleFireSupport.isSelected()) {
                        fd.getRoles().add(MissionRole.FIRE_SUPPORT);
                    }
                    if (chkRoleUrban.isSelected()) {
                        fd.getRoles().add(MissionRole.URBAN);
                    }
                    if (chkRoleInfantrySupport.isSelected()) {
                        fd.getRoles().add(MissionRole.INF_SUPPORT);
                    }
                    if (chkRoleCavalry.isSelected()) {
                        fd.getRoles().add(MissionRole.CAVALRY);
                    }
                    if (chkRoleRaider.isSelected()) {
                        fd.getRoles().add(MissionRole.RAIDER);
                    }
                    if (chkRoleIncendiary.isSelected()) {
                        fd.getRoles().add(MissionRole.INCENDIARY);
                    }
                    if (chkRoleAntiAircraft.isSelected()) {
                        fd.getRoles().add(MissionRole.ANTI_AIRCRAFT);
                    }
                    if (chkRoleAntiInfantry.isSelected()) {
                        fd.getRoles().add(MissionRole.ANTI_INFANTRY);
                    }
                    if (chkRoleArtillery.isSelected()) {
                        fd.getRoles().add(MissionRole.ARTILLERY);
                    }
                    if (chkRoleMissileArtillery.isSelected()) {
                        fd.getRoles().add(MissionRole.MISSILE_ARTILLERY);
                    }
                    if (chkRoleTransport.isSelected()) {
                        fd.getRoles().add(MissionRole.CARGO);
                    }
                    if (chkRoleEngineer.isSelected()) {
                        fd.getRoles().add(MissionRole.ENGINEER);
                    }
                    break;
                case UnitType.INFANTRY:
                case UnitType.BATTLE_ARMOR:
                    if (chkRoleFieldGun.isSelected()) {
                        fd.getRoles().add(MissionRole.FIELD_GUN);
                    }
                    if (chkRoleFieldArtillery.isSelected()) {
                        fd.getRoles().add(MissionRole.ARTILLERY);
                    }
                    if (chkRoleFieldMissileArtillery.isSelected()) {
                        fd.getRoles().add(MissionRole.MISSILE_ARTILLERY);
                    }
                    break;
                case UnitType.AERO:
                case UnitType.AEROSPACE_FIGHTER:
                    if (chkRoleAirRecon.isSelected()) {
                        fd.getRoles().add(MissionRole.RECON);
                    }
                    if (chkRoleGroundSupport.isSelected()) {
                        fd.getRoles().add(MissionRole.GROUND_SUPPORT);
                    }
                    if (chkRoleInterceptor.isSelected()) {
                        fd.getRoles().add(MissionRole.INTERCEPTOR);
                    }
                    if (chkRoleAssault.isSelected()) {
                        fd.getRoles().add(MissionRole.ASSAULT);
                    }
                    if (chkRoleAirTransport.isSelected()) {
                        fd.getRoles().add(MissionRole.CARGO);
                    }
                    break;
            }
        }

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

        double cargo = MathUtility.parseDouble(txtCargo.getText(), 0.0);
        fd.setCargo(cargo);
        txtCargo.setText(String.valueOf(cargo));

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

    private void clearSummaryTable() {
        if (summaryModel != null) {
            summaryModel.setRowCount(0);
        }
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
                panGroundRole.setVisible(unitType == UnitType.MEK || unitType == UnitType.TANK);
                panInfRole.setVisible(unitType == UnitType.INFANTRY || unitType == UnitType.BATTLE_ARMOR);
                panAirRole.setVisible(unitType == UnitType.AEROSPACE_FIGHTER || unitType == UnitType.CONV_FIGHTER);
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
                cbRating.addItem(null);
                for (String rating : n.getContent().split(",")) {
                    if (rating.contains(":")) {
                        String[] fields = rating.split(":");
                        cbRating.addItem(fields[0]);
                        ratingDisplayNames.put(fields[0], fields[1]);
                    } else {
                        cbRating.addItem(rating);
                        ratingDisplayNames.put(rating, rating);
                    }
                }
            } else {
                logger.warn("No rating found.");
            }
        }

        Ruleset rs = Ruleset.findRuleset(forceDesc.getFaction());
        String rating = rs.getDefaultRating(forceDesc);
        if (rating == null && cbRating.getItemCount() > 0) {
            rating = cbRating.getItemAt(0);
        }
        if (rating != null) {
            cbRating.setSelectedItem(rating);
            forceDesc.setRating(rating);
        }
        refreshFlags();
        cbRating.addActionListener(this);
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
        } else if (ev.getSource() == cbSubFaction) {
            logger.debug("cbSubFaction action: selected={}", cbSubFaction.getSelectedItem());
            if (cbSubFaction.getSelectedItem() != null) {
                forceDesc.setFaction(((FactionRecord) cbSubFaction.getSelectedItem()).getKey());
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
     * Makes the year text field read-only. Use this when an embedder anchors the year to an external value
     * (e.g. MekHQ's campaign year) and doesn't want the user editing it on this panel.
     */
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
        configureNetworks(fd);

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

    /**
     * Searches recursively for nodes that are flagged with C3 networks and configures them.
     *
     * @param fd {@link ForceDescriptor} Object
     */
    private void configureNetworks(ForceDescriptor fd) {
        if (fd.getFlags().contains("c3")) {
            Entity master = fd.getSubForces()
                  .stream()
                  .map(ForceDescriptor::getEntity)
                  .filter(en -> (null != en) && (en.hasC3M() || en.hasC3MM()))
                  .findFirst()
                  .orElse(null);
            if (null != master) {
                int c3s = 0;
                for (ForceDescriptor sf : fd.getSubForces()) {
                    if ((null != sf.getEntity()) &&
                          (sf.getEntity().getId() != master.getId()) &&
                          sf.getEntity().hasC3S()) {
                        sf.getEntity().setC3Master(master, false);
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
            Entity first = null;
            int nodes = 0;
            for (ForceDescriptor sf : fd.getSubForces()) {
                if ((null != sf.getEntity()) && sf.getEntity().hasC3i()) {
                    sf.getEntity().setC3UUID();
                    if (null == first) {
                        sf.getEntity().setC3NetIdSelf();
                        first = sf.getEntity();
                    } else {
                        sf.getEntity().setC3NetId(first);
                    }
                    nodes++;
                }
                if (nodes >= Entity.MAX_C3i_NODES) {
                    break;
                }
            }
        }
        fd.getSubForces().forEach(this::configureNetworks);
        fd.getAttached().forEach(this::configureNetworks);
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
            return fd;
        }

        @Override
        protected void done() {
            try {
                forceDesc = get();
                updateSummaryTable(forceDesc);
                if (onGenerate != null) {
                    onGenerate.accept(forceDesc);
                }
            } catch (InterruptedException ignored) {

            } catch (ExecutionException ex) {
                logger.error(ex, "");
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
