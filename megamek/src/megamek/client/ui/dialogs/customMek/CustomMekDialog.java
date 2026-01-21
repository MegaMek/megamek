/*
 * Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.customMek;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import megamek.client.Client;
import megamek.client.ui.GBC;
import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.clientGUI.DialogOptionListener;
import megamek.client.ui.dialogs.SliderDialog;
import megamek.client.ui.dialogs.buttonDialogs.AbstractButtonDialog;
import megamek.client.ui.panels.DialogOptionComponentYPanel;
import megamek.client.ui.util.UIUtil;
import megamek.codeUtilities.MathUtility;
import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.TechConstants;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Board;
import megamek.common.enums.Gender;
import megamek.common.enums.ProstheticEnhancementType;
import megamek.common.equipment.EquipmentMode;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.loaders.MapSettings;
import megamek.common.options.GameOptions;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PartialRepairs;
import megamek.common.options.PilotOptions;
import megamek.common.options.Quirks;
import megamek.common.options.WeaponQuirks;
import megamek.common.units.*;
import megamek.common.verifier.TestEntity;
import megamek.common.weapons.bayWeapons.ArtilleryBayWeapon;
import megamek.common.weapons.bayWeapons.capital.CapitalMissileBayWeapon;
import megamek.server.ServerBoardHelper;

/**
 * A dialog that a player can use to customize his mek before battle. Currently, changing pilots, setting up C3
 * networks, changing ammunition, deploying artillery offboard, setting MGs to rapid fire, setting auto-eject is
 * supported.
 *
 * @author Ben
 * @since March 18, 2002, 2:56 PM
 */
public class CustomMekDialog extends AbstractButtonDialog
      implements ActionListener, DialogOptionListener, ItemListener {

    public static final int DONE = 0;
    public static final int NEXT = 1;
    public static final int PREV = 2;

    private CustomPilotViewPanel[] panCrewMember;
    private QuirksPanel panQuirks;
    private JPanel panPartReps;

    private JPanel panOptions;
    // private JScrollPane scrOptions;

    private JTabbedPane tabAll;

    private final JTextField fldInit = new JTextField(3);
    private final JTextField fldCommandInit = new JTextField(3);
    private final JCheckBox chCommander = new JCheckBox();

    private final JLabel labDeploymentRound = new JLabel(Messages.getString("CustomMekDialog.labDeployment"),
          SwingConstants.RIGHT);
    private final JLabel labDeploymentZone = new JLabel(Messages.getString("CustomMekDialog.labDeploymentZone"),
          SwingConstants.RIGHT);
    private final JLabel labDeploymentOffset = new JLabel(Messages.getString("CustomMekDialog.labDeploymentOffset"),
          SwingConstants.RIGHT);
    private final JLabel labDeploymentWidth = new JLabel(Messages.getString("CustomMekDialog.labDeploymentWidth"),
          SwingConstants.RIGHT);
    private final JComboBox<String> choDeploymentRound = new JComboBox<>();
    private final JComboBox<String> choDeploymentZone = new JComboBox<>();

    // this might seem like kind of a dumb way to declare it, but JFormattedTextField doesn't have an overload that
    // takes both a number formatter and a default value.
    private final NumberFormatter numFormatter = new NumberFormatter();
    private final DefaultFormatterFactory formatterFactory = new DefaultFormatterFactory(numFormatter);

    private final JFormattedTextField txtDeploymentOffset = new JFormattedTextField(formatterFactory);
    private final JFormattedTextField txtDeploymentWidth = new JFormattedTextField(formatterFactory);

    private JSpinner spinStartingAnyNWx;
    private JSpinner spinStartingAnyNWy;
    private JSpinner spinStartingAnySEx;
    private JSpinner spinStartingAnySEy;

    private final JLabel labDeployShutdown = new JLabel(Messages.getString("CustomMekDialog.labDeployShutdown"),
          SwingConstants.RIGHT);
    private final JCheckBox chDeployShutdown = new JCheckBox();
    private final JLabel labDeployProne = new JLabel(Messages.getString("CustomMekDialog.labDeployProne"),
          SwingConstants.RIGHT);
    private final JCheckBox chDeployProne = new JCheckBox();
    private final JLabel labDeployHullDown = new JLabel(Messages.getString("CustomMekDialog.labDeployHullDown"),
          SwingConstants.RIGHT);
    private final JCheckBox chDeployHullDown = new JCheckBox();
    private final JLabel labHidden = new JLabel(Messages.getString("CustomMekDialog.labHidden"), SwingConstants.RIGHT);
    private final JCheckBox chHidden = new JCheckBox();

    private final JLabel labDeployStealth = new JLabel(Messages.getString("CustomMekDialog.labDeployStealth"),
          SwingConstants.RIGHT);
    private final JCheckBox chDeployStealth = new JCheckBox();

    private final JLabel labOffBoard = new JLabel(Messages.getString("CustomMekDialog.labOffBoard"),
          SwingConstants.RIGHT);
    private final JCheckBox chOffBoard = new JCheckBox();
    private final JLabel labOffBoardDirection = new JLabel(Messages.getString("CustomMekDialog.labOffBoardDirection"),
          SwingConstants.RIGHT);
    private final JComboBox<String> choOffBoardDirection = new JComboBox<>();
    private final JLabel labOffBoardDistance = new JLabel(Messages.getString("CustomMekDialog.labOffBoardDistance"),
          SwingConstants.RIGHT);
    private final JTextField fldOffBoardDistance = new JTextField(4);
    private final JButton butOffBoardDistance = new JButton("0");
    private final JLabel labStartingMode = new JLabel(Messages.getString("CustomMekDialog.labStartingMode"),
          SwingConstants.RIGHT);
    private final JComboBox<String> choStartingMode = new JComboBox<>();
    private final JLabel labCurrentFuel = new JLabel(Messages.getString("CustomMekDialog.labCurrentFuel"),
          SwingConstants.RIGHT);
    private final JTextField fldCurrentFuel = new JTextField(7);
    private final JLabel labStartVelocity = new JLabel(Messages.getString("CustomMekDialog.labStartVelocity"),
          SwingConstants.RIGHT);
    private final JTextField fldStartVelocity = new JTextField(3);
    private final JLabel labStartAltitude = new JLabel(Messages.getString("CustomMekDialog.labStartAltitude"),
          SwingConstants.RIGHT);
    private final JTextField fldStartAltitude = new JTextField(3);
    private final JLabel labStartHeight = new JLabel(Messages.getString("CustomMekDialog.labStartHeight"),
          SwingConstants.RIGHT);
    private final JTextField fldStartHeight = new JTextField(3);
    private final JCheckBox chDeployAirborne = new JCheckBox();
    private final JPanel panButtons = new JPanel();
    private final JButton butOkay = new JButton(Messages.getString("Okay"));
    private final JButton butCancel = new JButton(Messages.getString("Cancel"));
    private final JButton butNext = new JButton(Messages.getString("Next"));
    private final JButton butPrev = new JButton(Messages.getString("Previous"));
    private EquipChoicePanel m_equip;
    private final JPanel panEquip = new JPanel();
    private final List<Entity> entities;
    private boolean okay;
    private int status = CustomMekDialog.DONE;

    private final ClientGUI clientGUI;
    private final Client client;
    private final boolean space;

    private PilotOptions options;
    private PartialRepairs partReps;
    private final HashMap<Integer, WeaponQuirks> h_wpnQuirks = new HashMap<>();
    private ArrayList<DialogOptionComponentYPanel> optionComps = new ArrayList<>();
    private ArrayList<DialogOptionComponentYPanel> partRepsComps = new ArrayList<>();

    private final boolean editable;
    private final boolean editableDeployment;

    // Prosthetic Enhancement UI components (inline with checkbox, for conventional infantry only)
    // Standard Enhanced (MD_PL_ENHANCED) - inline controls for slot 1 only
    private JComboBox<String> choProstheticTypeStd;
    private JSpinner spinProstheticCountStd;
    // Improved Enhanced (MD_PL_I_ENHANCED) - inline controls for slot 1 and slot 2
    private JComboBox<String> choProstheticType1Imp;
    private JSpinner spinProstheticCount1Imp;
    private JComboBox<String> choProstheticType2Imp;
    private JSpinner spinProstheticCount2Imp;
    // Extraneous Limbs (MD_PL_EXTRA_LIMBS) - inline controls for pair 1 and pair 2 (no count, always 2)
    private JLabel lblExtraneousPair1;
    private JComboBox<String> choExtraneousPair1;
    private JLabel lblExtraneousPair2;
    private JComboBox<String> choExtraneousPair2;

    private int distance = 17;
    private int fuel = 0;

    /**
     * Creates new CustomMekDialog
     */
    public CustomMekDialog(ClientGUI clientgui, Client client, List<Entity> entities, boolean editable) {
        this(clientgui, client, entities, editable, true);
    }

    /**
     * Creates new CustomMekDialog
     */
    public CustomMekDialog(ClientGUI clientgui, Client client, List<Entity> entities, boolean editable,
          boolean editableDeployment) {

        super(clientgui.getFrame(), "CustomizeMekDialog", "CustomMekDialog.title");
        this.entities = entities;
        this.clientGUI = clientgui;
        this.client = client;
        this.space = clientgui.getClient().getMapSettings().getMedium() == MapSettings.MEDIUM_SPACE;
        this.editable = editable;
        this.editableDeployment = editableDeployment;

        // Ensure we have at least one passed entity, anything less makes no sense
        if (entities.isEmpty()) {
            throw new IllegalStateException("Must pass at least one Entity!");
        }

        initialize();
    }

    public CustomMekDialog(JFrame frame, Client client, List<Entity> entities, boolean editable,
          boolean editableDeployment) {
        super(frame, "CustomizeMekDialog", "CustomMekDialog.title");
        this.entities = entities;
        this.clientGUI = null;
        this.client = client;
        this.space = false;
        this.editable = editable;
        this.editableDeployment = editableDeployment;

        // Ensure we have at least one passed entity, anything less makes no sense
        if (entities.isEmpty()) {
            throw new IllegalStateException("Must pass at least one Entity!");
        }

        initialize();
    }

    public String getSelectedTab() {
        return tabAll.getTitleAt(tabAll.getSelectedIndex());
    }

    public void setSelectedTab(String tabName) {
        for (int i = 0; i < tabAll.getTabCount(); i++) {
            if (tabAll.getTitleAt(i).equals(tabName)) {
                tabAll.setSelectedIndex(i);
            }
        }
    }

    public ClientGUI getClientGUI() {
        return clientGUI;
    }

    public Client getClient() {
        if (clientGUI != null) {
            return clientGUI.getClient();
        } else {
            return client;
        }
    }

    private void setOptions() {
        Entity entity = entities.get(0);
        IOption option;
        for (final DialogOptionComponentYPanel newVar : optionComps) {
            option = newVar.getOption();
            if ((newVar.getValue() == Messages.getString("CustomMekDialog.None"))) {
                entity.getCrew().getOptions().getOption(option.getName()).setValue("None");
            } else {
                entity.getCrew().getOptions().getOption(option.getName()).setValue(newVar.getValue());
            }
        }
    }

    public void refreshOptions() {
        panOptions.removeAll();
        optionComps = new ArrayList<>();

        GridBagLayout gridBagLayout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        panOptions.setLayout(gridBagLayout);

        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        c.ipadx = 0;
        c.ipady = 0;

        for (Enumeration<IOptionGroup> i = options.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();

            if (group.getKey().equalsIgnoreCase(PilotOptions.LVL3_ADVANTAGES) &&
                  !gameOptions().booleanOption(OptionsConstants.RPG_PILOT_ADVANTAGES)) {
                continue;
            }

            if (group.getKey().equalsIgnoreCase(PilotOptions.EDGE_ADVANTAGES) &&
                  !gameOptions().booleanOption(OptionsConstants.EDGE)) {
                continue;
            }

            if (group.getKey().equalsIgnoreCase(PilotOptions.MD_ADVANTAGES) &&
                  !gameOptions().booleanOption(OptionsConstants.RPG_MANEI_DOMINI)) {
                continue;
            }

            addGroup(group, gridBagLayout, c);

            Entity entity = entities.get(0);
            for (Enumeration<IOption> j = group.getOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();

                if (entity.isBuildingEntityOrGunEmplacement()) {
                    continue;
                }

                // a bunch of stuff should get disabled for conv infantry
                if (entity.isConventionalInfantry() &&
                      (option.getName().equals(OptionsConstants.MD_VDNI) ||
                            option.getName().equals(OptionsConstants.MD_BVDNI))) {
                    continue;
                }

                // a bunch of stuff should get disabled for all but conventional infantry
                // Prosthetic enhancements (Enhanced/Improved Enhanced) are infantry-only (IO p.84)
                // Sensory implants (audio, visual, laser, tele) are infantry-only
                // Gas Effuser (Pheromone/Toxin) is infantry-only (IO pg 79)
                String optionName = option.getName();
                boolean isInfantryOnlyOption = switch (optionName) {
                    case OptionsConstants.MD_PL_ENHANCED,
                         OptionsConstants.MD_PL_I_ENHANCED,
                         OptionsConstants.MD_PL_MASC,
                         OptionsConstants.MD_CYBER_IMP_AUDIO,
                         OptionsConstants.MD_CYBER_IMP_VISUAL,
                         OptionsConstants.MD_CYBER_IMP_LASER,
                         OptionsConstants.MD_CYBER_IMP_TELE,
                         OptionsConstants.MD_GAS_EFFUSER_PHEROMONE,
                         OptionsConstants.MD_GAS_EFFUSER_TOXIN -> true;
                    default -> false;
                };
                if (!entity.isConventionalInfantry() && isInfantryOnlyOption) {
                    continue;
                }

                addOption(option, gridBagLayout, c, editable);
            }
        }

        validate();
    }

    private void setPartReps() {
        Entity entity = entities.get(0);
        IOption option;
        for (final DialogOptionComponentYPanel newVar : partRepsComps) {
            option = newVar.getOption();
            if ((newVar.getValue() == Messages.getString("CustomMekDialog.None"))) {
                entity.getPartialRepairs().getOption(option.getName()).setValue("None");
            } else {
                entity.getPartialRepairs().getOption(option.getName()).setValue(newVar.getValue());
            }
        }
    }

    private void setQuirks() {
        panQuirks.setQuirks();
    }

    public void refreshPartReps() {
        Entity entity = entities.get(0);
        panPartReps.removeAll();
        partRepsComps = new ArrayList<>();
        for (Enumeration<IOptionGroup> i = partReps.getGroups(); i.hasMoreElements(); ) {
            IOptionGroup group = i.nextElement();
            panPartReps.add(new JLabel(group.getDisplayableName()), GBC.eol());

            for (Enumeration<IOption> j = group.getSortedOptions(); j.hasMoreElements(); ) {
                IOption option = j.nextElement();

                if (!PartialRepairs.isPartRepLegalFor(option, entity)) {
                    continue;
                }

                addPartRep(option, editable);
            }
        }
        validate();
    }

    public void refreshQuirks() {
        panQuirks.refreshQuirks();
    }

    private void addGroup(IOptionGroup group, GridBagLayout gridBagLayout, GridBagConstraints gridBagConstraints) {
        JLabel groupLabel = new JLabel(group.getDisplayableName());
        gridBagLayout.setConstraints(groupLabel, gridBagConstraints);
        panOptions.add(groupLabel);
    }

    private void addOption(IOption option, GridBagLayout gridBagLayout, GridBagConstraints gridBagConstraints,
          boolean editable) {
        Entity entity = entities.get(0);
        DialogOptionComponentYPanel optionComp = new DialogOptionComponentYPanel(this, option, editable);

        if ((OptionsConstants.GUNNERY_WEAPON_SPECIALIST).equals(option.getName())) {
            optionComp.addValue(Messages.getString("CustomMekDialog.None"));
            PilotSPAHelper.weaponSpecialistValidWeaponNames(entity, gameOptions()).forEach(optionComp::addValue);
            optionComp.setSelected(option.stringValue());
        }

        if ((OptionsConstants.GUNNERY_SANDBLASTER).equals(option.getName())) {
            optionComp.addValue(Messages.getString("CustomMekDialog.None"));
            PilotSPAHelper.sandblasterValidWeaponNames(entity, gameOptions()).forEach(optionComp::addValue);
            optionComp.setSelected(option.stringValue());
        }

        if (OptionsConstants.GUNNERY_SPECIALIST.equals(option.getName())) {
            optionComp.addValue(Crew.SPECIAL_NONE);
            optionComp.addValue(Crew.SPECIAL_ENERGY);
            optionComp.addValue(Crew.SPECIAL_BALLISTIC);
            optionComp.addValue(Crew.SPECIAL_MISSILE);
            optionComp.setSelected(option.stringValue());
        }

        if (OptionsConstants.GUNNERY_RANGE_MASTER.equals(option.getName())) {
            optionComp.addValue(Crew.RANGEMASTER_NONE);
            optionComp.addValue(Crew.RANGEMASTER_MEDIUM);
            optionComp.addValue(Crew.RANGEMASTER_LONG);
            optionComp.addValue(Crew.RANGEMASTER_EXTREME);
            optionComp.setSelected(option.stringValue());
        }

        if (OptionsConstants.MISC_HUMAN_TRO.equals(option.getName())) {
            optionComp.addValue(Crew.HUMAN_TRO_NONE);
            optionComp.addValue(Crew.HUMAN_TRO_MEK);
            optionComp.addValue(Crew.HUMAN_TRO_AERO);
            optionComp.addValue(Crew.HUMAN_TRO_VEE);
            optionComp.addValue(Crew.HUMAN_TRO_BA);
            optionComp.setSelected(option.stringValue());
        }

        if (OptionsConstants.MISC_ENV_SPECIALIST.equals(option.getName())) {
            optionComp.addValue(Crew.ENVIRONMENT_SPECIALIST_NONE);
            optionComp.addValue(Crew.ENVIRONMENT_SPECIALIST_FOG);
            optionComp.addValue(Crew.ENVIRONMENT_SPECIALIST_LIGHT);
            optionComp.addValue(Crew.ENVIRONMENT_SPECIALIST_RAIN);
            optionComp.addValue(Crew.ENVIRONMENT_SPECIALIST_SNOW);
            optionComp.addValue(Crew.ENVIRONMENT_SPECIALIST_WIND);
        }

        // Prosthetic Limbs, Enhanced - add inline dropdown and spinner for slot 1
        if (OptionsConstants.MD_PL_ENHANCED.equals(option.getName()) && entity.isConventionalInfantry()) {
            addInlineProstheticControls(optionComp, entity, true);
        }

        // Prosthetic Limbs, Improved Enhanced - add inline dropdowns and spinners for both slots
        if (OptionsConstants.MD_PL_I_ENHANCED.equals(option.getName()) && entity.isConventionalInfantry()) {
            addInlineProstheticControls(optionComp, entity, false);
        }

        // Prosthetic Limbs, Extraneous (Enhanced) - add inline dropdowns for pair 1 and pair 2
        if (OptionsConstants.MD_PL_EXTRA_LIMBS.equals(option.getName()) && entity.isConventionalInfantry()) {
            addInlineExtraneousControls(optionComp, entity);
        }

        gridBagLayout.setConstraints(optionComp, gridBagConstraints);
        panOptions.add(optionComp);
        optionComps.add(optionComp);
    }

    private void addPartRep(IOption option, boolean editable) {
        DialogOptionComponentYPanel optionComp = new DialogOptionComponentYPanel(this, option, editable);
        panPartReps.add(optionComp, GBC.eol());
        partRepsComps.add(optionComp);
    }

    /**
     * Adds inline prosthetic enhancement controls (dropdown + spinner) to the option component panel.
     * For Standard Enhanced: adds slot 1 controls only.
     * For Improved Enhanced: adds slot 1 and slot 2 controls.
     *
     * @param optionComp The DialogOptionComponentYPanel to add controls to
     * @param entity The entity being configured
     * @param isStandardEnhanced True for MD_PL_ENHANCED (slot 1 only), false for MD_PL_I_ENHANCED (both slots)
     */
    private void addInlineProstheticControls(DialogOptionComponentYPanel optionComp, Entity entity,
          boolean isStandardEnhanced) {
        Infantry infantry = (entity instanceof Infantry) ? (Infantry) entity : null;
        String typeTooltip = Messages.getString("CustomMekDialog.ProstheticTypeTooltip");
        String countTooltip = Messages.getString("CustomMekDialog.ProstheticCountTooltip");

        if (isStandardEnhanced) {
            // Standard Enhanced: Create slot 1 controls only
            choProstheticTypeStd = new JComboBox<>();
            populateProstheticDropdown(choProstheticTypeStd);
            SpinnerNumberModel countModel = new SpinnerNumberModel(1, 1, 2, 1);
            spinProstheticCountStd = new JSpinner(countModel);

            // Set initial values from entity slot 1
            if ((infantry != null) && infantry.hasProstheticEnhancement1()) {
                ProstheticEnhancementType type1 = infantry.getProstheticEnhancement1();
                String itemText = type1.getCategory().getDisplayName() + ": " + type1.getDisplayName();
                choProstheticTypeStd.setSelectedItem(itemText);
                spinProstheticCountStd.setValue(infantry.getProstheticEnhancement1Count());
            } else {
                choProstheticTypeStd.setSelectedIndex(0);
                spinProstheticCountStd.setValue(1);
            }

            // Enable/disable spinner based on dropdown selection
            spinProstheticCountStd.setEnabled(choProstheticTypeStd.getSelectedIndex() > 0);
            choProstheticTypeStd.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    spinProstheticCountStd.setEnabled(choProstheticTypeStd.getSelectedIndex() > 0);
                }
            });

            // Set tooltips
            choProstheticTypeStd.setToolTipText(typeTooltip);
            spinProstheticCountStd.setToolTipText(countTooltip);

            // Add controls inline
            optionComp.add(choProstheticTypeStd);
            optionComp.add(spinProstheticCountStd);

            // Disable if not editable
            if (!editable) {
                choProstheticTypeStd.setEnabled(false);
                spinProstheticCountStd.setEnabled(false);
            }

            // Set initial visibility based on current checkbox state
            boolean isChecked = entity.hasAbility(OptionsConstants.MD_PL_ENHANCED);
            choProstheticTypeStd.setVisible(isChecked);
            spinProstheticCountStd.setVisible(isChecked);
        } else {
            // Improved Enhanced: Create controls for both slot 1 and slot 2
            choProstheticType1Imp = new JComboBox<>();
            populateProstheticDropdown(choProstheticType1Imp);
            SpinnerNumberModel countModel1 = new SpinnerNumberModel(1, 1, 2, 1);
            spinProstheticCount1Imp = new JSpinner(countModel1);

            choProstheticType2Imp = new JComboBox<>();
            populateProstheticDropdown(choProstheticType2Imp);
            SpinnerNumberModel countModel2 = new SpinnerNumberModel(1, 1, 2, 1);
            spinProstheticCount2Imp = new JSpinner(countModel2);

            // Set initial values from entity
            if ((infantry != null) && infantry.hasProstheticEnhancement1()) {
                ProstheticEnhancementType type1 = infantry.getProstheticEnhancement1();
                String itemText = type1.getCategory().getDisplayName() + ": " + type1.getDisplayName();
                choProstheticType1Imp.setSelectedItem(itemText);
                spinProstheticCount1Imp.setValue(infantry.getProstheticEnhancement1Count());
            } else {
                choProstheticType1Imp.setSelectedIndex(0);
                spinProstheticCount1Imp.setValue(1);
            }

            if ((infantry != null) && infantry.hasProstheticEnhancement2()) {
                ProstheticEnhancementType type2 = infantry.getProstheticEnhancement2();
                String itemText = type2.getCategory().getDisplayName() + ": " + type2.getDisplayName();
                choProstheticType2Imp.setSelectedItem(itemText);
                spinProstheticCount2Imp.setValue(infantry.getProstheticEnhancement2Count());
            } else {
                choProstheticType2Imp.setSelectedIndex(0);
                spinProstheticCount2Imp.setValue(1);
            }

            // Enable/disable spinners based on dropdown selection
            spinProstheticCount1Imp.setEnabled(choProstheticType1Imp.getSelectedIndex() > 0);
            choProstheticType1Imp.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    spinProstheticCount1Imp.setEnabled(choProstheticType1Imp.getSelectedIndex() > 0);
                }
            });

            spinProstheticCount2Imp.setEnabled(choProstheticType2Imp.getSelectedIndex() > 0);
            choProstheticType2Imp.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    spinProstheticCount2Imp.setEnabled(choProstheticType2Imp.getSelectedIndex() > 0);
                }
            });

            // Set tooltips
            choProstheticType1Imp.setToolTipText(typeTooltip);
            spinProstheticCount1Imp.setToolTipText(countTooltip);
            choProstheticType2Imp.setToolTipText(typeTooltip);
            spinProstheticCount2Imp.setToolTipText(countTooltip);

            // Add controls inline
            optionComp.add(choProstheticType1Imp);
            optionComp.add(spinProstheticCount1Imp);
            optionComp.add(choProstheticType2Imp);
            optionComp.add(spinProstheticCount2Imp);

            // Disable if not editable
            if (!editable) {
                choProstheticType1Imp.setEnabled(false);
                spinProstheticCount1Imp.setEnabled(false);
                choProstheticType2Imp.setEnabled(false);
                spinProstheticCount2Imp.setEnabled(false);
            }

            // Set initial visibility based on current checkbox state
            boolean isChecked = entity.hasAbility(OptionsConstants.MD_PL_I_ENHANCED);
            choProstheticType1Imp.setVisible(isChecked);
            spinProstheticCount1Imp.setVisible(isChecked);
            choProstheticType2Imp.setVisible(isChecked);
            spinProstheticCount2Imp.setVisible(isChecked);
        }
    }

    /**
     * Populates a prosthetic enhancement dropdown with all enhancement types grouped by category.
     */
    private void populateProstheticDropdown(JComboBox<String> dropdown) {
        dropdown.addItem(Messages.getString("CustomMekDialog.None"));
        for (ProstheticEnhancementType.EnhancementCategory category : ProstheticEnhancementType.EnhancementCategory.values()) {
            for (ProstheticEnhancementType type : ProstheticEnhancementType.values()) {
                if (type.getCategory() == category) {
                    dropdown.addItem(category.getDisplayName() + ": " + type.getDisplayName());
                }
            }
        }
    }

    /**
     * Adds inline extraneous limb controls (2 dropdowns for pair 1 and pair 2) to the option component panel. Each pair
     * always provides 2 items, so no count spinner is needed.
     *
     * @param optionComp The DialogOptionComponentYPanel to add controls to
     * @param entity     The entity being configured
     */
    private void addInlineExtraneousControls(DialogOptionComponentYPanel optionComp, Entity entity) {
        Infantry infantry = (entity instanceof Infantry) ? (Infantry) entity : null;
        String pair1Tooltip = Messages.getString("CustomMekDialog.ExtraneousPair1Tooltip");
        String pair2Tooltip = Messages.getString("CustomMekDialog.ExtraneousPair2Tooltip");

        // Create pair 1 dropdown
        choExtraneousPair1 = new JComboBox<>();
        populateProstheticDropdown(choExtraneousPair1);

        // Create pair 2 dropdown
        choExtraneousPair2 = new JComboBox<>();
        populateProstheticDropdown(choExtraneousPair2);

        // Set initial values from entity
        if ((infantry != null) && infantry.hasExtraneousPair1()) {
            ProstheticEnhancementType pair1Type = infantry.getExtraneousPair1();
            String itemText = pair1Type.getCategory().getDisplayName() + ": " + pair1Type.getDisplayName();
            choExtraneousPair1.setSelectedItem(itemText);
        } else {
            choExtraneousPair1.setSelectedIndex(0);
        }

        if ((infantry != null) && infantry.hasExtraneousPair2()) {
            ProstheticEnhancementType pair2Type = infantry.getExtraneousPair2();
            String itemText = pair2Type.getCategory().getDisplayName() + ": " + pair2Type.getDisplayName();
            choExtraneousPair2.setSelectedItem(itemText);
        } else {
            choExtraneousPair2.setSelectedIndex(0);
        }

        // Set tooltips
        choExtraneousPair1.setToolTipText(pair1Tooltip);
        choExtraneousPair2.setToolTipText(pair2Tooltip);

        // Create labels
        lblExtraneousPair1 = new JLabel(Messages.getString("CustomMekDialog.labExtraneousPair1"));
        lblExtraneousPair2 = new JLabel(Messages.getString("CustomMekDialog.labExtraneousPair2"));

        // Add labels and controls inline
        optionComp.add(lblExtraneousPair1);
        optionComp.add(choExtraneousPair1);
        optionComp.add(lblExtraneousPair2);
        optionComp.add(choExtraneousPair2);

        // Disable if not editable
        if (!editable) {
            choExtraneousPair1.setEnabled(false);
            choExtraneousPair2.setEnabled(false);
        }

        // Set initial visibility based on current checkbox state
        boolean isChecked = entity.hasAbility(OptionsConstants.MD_PL_EXTRA_LIMBS);
        lblExtraneousPair1.setVisible(isChecked);
        choExtraneousPair1.setVisible(isChecked);
        lblExtraneousPair2.setVisible(isChecked);
        choExtraneousPair2.setVisible(isChecked);

        // Glider or powered flight wings limit extraneous limbs to one pair (IO p.85)
        // Disable pair 2 if any wing type is already enabled
        boolean hasWings = entity.hasAbility(OptionsConstants.MD_PL_GLIDER)
              || entity.hasAbility(OptionsConstants.MD_PL_FLIGHT);
        if (hasWings && editable) {
            choExtraneousPair2.setEnabled(false);
            lblExtraneousPair2.setEnabled(false);
            // Clear pair 2 if it was set (shouldn't happen with valid data, but be safe)
            if (choExtraneousPair2.getSelectedIndex() > 0) {
                choExtraneousPair2.setSelectedIndex(0);
            }
        }

        // Add listener to prevent pair 2 selection when any wing type is enabled
        choExtraneousPair2.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED
                  && choExtraneousPair2.getSelectedIndex() > 0
                  && (isOptionSelected(OptionsConstants.MD_PL_GLIDER)
                  || isOptionSelected(OptionsConstants.MD_PL_FLIGHT))) {
                // Revert to None
                choExtraneousPair2.setSelectedIndex(0);
                JOptionPane.showMessageDialog(this,
                      Messages.getString("CustomMekDialog.GliderWingsLimitExtraneousLimbs"),
                      Messages.getString("CustomMekDialog.GliderWingsLimitExtraneousLimbsTitle"),
                      JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    /**
     * Updates the visibility of inline prosthetic enhancement controls when checkbox state changes.
     *
     * @param optionName The option that was toggled (MD_PL_ENHANCED, MD_PL_I_ENHANCED, or MD_PL_EXTRA_LIMBS)
     * @param isChecked Whether the checkbox is now checked
     */
    private void updateInlineProstheticVisibility(String optionName, boolean isChecked) {
        if (OptionsConstants.MD_PL_ENHANCED.equals(optionName)) {
            // Standard Enhanced uses its own slot 1 controls
            if (choProstheticTypeStd != null) {
                choProstheticTypeStd.setVisible(isChecked);
                spinProstheticCountStd.setVisible(isChecked);
            }
        } else if (OptionsConstants.MD_PL_I_ENHANCED.equals(optionName)) {
            // Improved Enhanced uses its own slot 1 and slot 2 controls
            if (choProstheticType1Imp != null) {
                choProstheticType1Imp.setVisible(isChecked);
                spinProstheticCount1Imp.setVisible(isChecked);
            }
            if (choProstheticType2Imp != null) {
                choProstheticType2Imp.setVisible(isChecked);
                spinProstheticCount2Imp.setVisible(isChecked);
            }
        } else if (OptionsConstants.MD_PL_EXTRA_LIMBS.equals(optionName)) {
            // Extraneous Limbs uses pair 1 and pair 2 dropdowns
            if (lblExtraneousPair1 != null) {
                lblExtraneousPair1.setVisible(isChecked);
            }
            if (choExtraneousPair1 != null) {
                choExtraneousPair1.setVisible(isChecked);
            }
            if (lblExtraneousPair2 != null) {
                lblExtraneousPair2.setVisible(isChecked);
            }
            if (choExtraneousPair2 != null) {
                choExtraneousPair2.setVisible(isChecked);
            }
        }
    }

    /**
     * Applies prosthetic enhancement settings from the UI to the entity.
     * Reads from the controls of whichever prosthetic option (Standard or Improved) is currently selected.
     */
    private void applyProstheticEnhancement(Entity entity) {
        if (!(entity instanceof Infantry infantry)) {
            return;
        }

        // Check which prosthetic option is selected (they are mutually exclusive)
        boolean hasStandardEnhanced = entity.hasAbility(OptionsConstants.MD_PL_ENHANCED);
        boolean hasImprovedEnhanced = entity.hasAbility(OptionsConstants.MD_PL_I_ENHANCED);

        if (hasStandardEnhanced && (choProstheticTypeStd != null)) {
            // Standard Enhanced: Apply slot 1 from Standard controls
            applyProstheticSlot(infantry, choProstheticTypeStd, spinProstheticCountStd, true);
            // Clear slot 2 (not used by Standard Enhanced)
            infantry.setProstheticEnhancement2(null);
            infantry.setProstheticEnhancement2Count(0);
        } else if (hasImprovedEnhanced && (choProstheticType1Imp != null)) {
            // Improved Enhanced: Apply slot 1 and slot 2 from Improved controls
            applyProstheticSlot(infantry, choProstheticType1Imp, spinProstheticCount1Imp, true);
            if (choProstheticType2Imp != null) {
                applyProstheticSlot(infantry, choProstheticType2Imp, spinProstheticCount2Imp, false);
            }
        } else {
            // Neither option selected - clear both slots
            infantry.setProstheticEnhancement1(null);
            infantry.setProstheticEnhancement1Count(0);
            infantry.setProstheticEnhancement2(null);
            infantry.setProstheticEnhancement2Count(0);
        }
    }

    /**
     * Applies a single prosthetic enhancement slot from UI to entity.
     */
    private void applyProstheticSlot(Infantry infantry, JComboBox<String> typeDropdown,
          JSpinner countSpinner, boolean isSlot1) {
        int selectedIndex = typeDropdown.getSelectedIndex();
        if (selectedIndex <= 0) {
            // "None" selected - clear this slot
            if (isSlot1) {
                infantry.setProstheticEnhancement1(null);
                infantry.setProstheticEnhancement1Count(0);
            } else {
                infantry.setProstheticEnhancement2(null);
                infantry.setProstheticEnhancement2Count(0);
            }
        } else {
            // Parse the selected item to find the enhancement type
            String selectedText = (String) typeDropdown.getSelectedItem();
            ProstheticEnhancementType selectedType = null;

            for (ProstheticEnhancementType type : ProstheticEnhancementType.values()) {
                String itemText = type.getCategory().getDisplayName() + ": " + type.getDisplayName();
                if (itemText.equals(selectedText)) {
                    selectedType = type;
                    break;
                }
            }

            if (selectedType != null) {
                if (isSlot1) {
                    infantry.setProstheticEnhancement1(selectedType);
                    infantry.setProstheticEnhancement1Count((Integer) countSpinner.getValue());
                } else {
                    infantry.setProstheticEnhancement2(selectedType);
                    infantry.setProstheticEnhancement2Count((Integer) countSpinner.getValue());
                }
            }
        }
    }

    /**
     * Applies extraneous limb settings from the UI to the entity. Each pair always provides 2 items, so no count is
     * needed.
     */
    private void applyExtraneousLimbs(Entity entity) {
        if (!(entity instanceof Infantry infantry)) {
            return;
        }

        boolean hasExtraneousLimbs = entity.hasAbility(OptionsConstants.MD_PL_EXTRA_LIMBS);

        if (hasExtraneousLimbs && (choExtraneousPair1 != null)) {
            // Apply pair 1
            applyExtraneousPair(infantry, choExtraneousPair1, true);
            // Apply pair 2
            if (choExtraneousPair2 != null) {
                applyExtraneousPair(infantry, choExtraneousPair2, false);
            }
        } else {
            // Option not selected - clear both pairs
            infantry.setExtraneousPair1(null);
            infantry.setExtraneousPair2(null);
        }
    }

    /**
     * Applies a single extraneous limb pair from UI to entity.
     */
    private void applyExtraneousPair(Infantry infantry, JComboBox<String> typeDropdown, boolean isPair1) {
        int selectedIndex = typeDropdown.getSelectedIndex();
        if (selectedIndex <= 0) {
            // "None" selected - clear this pair
            if (isPair1) {
                infantry.setExtraneousPair1(null);
            } else {
                infantry.setExtraneousPair2(null);
            }
        } else {
            // Parse the selected item to find the enhancement type
            String selectedText = (String) typeDropdown.getSelectedItem();
            ProstheticEnhancementType selectedType = null;

            for (ProstheticEnhancementType type : ProstheticEnhancementType.values()) {
                String itemText = type.getCategory().getDisplayName() + ": " + type.getDisplayName();
                if (itemText.equals(selectedText)) {
                    selectedType = type;
                    break;
                }
            }

            if (selectedType != null) {
                if (isPair1) {
                    infantry.setExtraneousPair1(selectedType);
                } else {
                    infantry.setExtraneousPair2(selectedType);
                }
            }
        }
    }

    @Override
    public void optionClicked(DialogOptionComponentYPanel comp, IOption option, boolean state) {
        // Enforce max 2 sensory implants rule for infantry
        // Defensive check for isConventionalInfantry in case options are set through other means
        Entity entity = entities.get(0);
        if (state && entity.isConventionalInfantry() && isSensoryImplant(option.getName())) {
            int count = countSelectedSensoryImplants(comp);
            if (count >= 2) {
                // Revert the selection
                comp.setSelected(false);
                JOptionPane.showMessageDialog(this,
                      Messages.getString("CustomMekDialog.MaxSensoryImplants"),
                      Messages.getString("CustomMekDialog.MaxSensoryImplantsTitle"),
                      JOptionPane.WARNING_MESSAGE);
            }
        }

        // Enhanced and Improved Enhanced are mutually exclusive
        // When one is selected, deselect the other
        if (state && option.getName().equals(OptionsConstants.MD_PL_ENHANCED)) {
            deselectOption(OptionsConstants.MD_PL_I_ENHANCED);
        } else if (state && option.getName().equals(OptionsConstants.MD_PL_I_ENHANCED)) {
            deselectOption(OptionsConstants.MD_PL_ENHANCED);
        }

        // Glider Wings and Powered Flight Wings are mutually exclusive (IO p.85)
        // When one is selected, deselect the other
        if (state && option.getName().equals(OptionsConstants.MD_PL_GLIDER)) {
            deselectOption(OptionsConstants.MD_PL_FLIGHT);
            updateExtraneousPair2ForWings(true);
        } else if (state && option.getName().equals(OptionsConstants.MD_PL_FLIGHT)) {
            deselectOption(OptionsConstants.MD_PL_GLIDER);
            updateExtraneousPair2ForWings(true);
        }

        // When wings are deselected, re-enable extraneous pair 2 if no other wing type is active
        if (!state && (option.getName().equals(OptionsConstants.MD_PL_GLIDER)
              || option.getName().equals(OptionsConstants.MD_PL_FLIGHT))) {
            boolean anyWingsActive = isOptionSelected(OptionsConstants.MD_PL_GLIDER)
                  || isOptionSelected(OptionsConstants.MD_PL_FLIGHT);
            updateExtraneousPair2ForWings(anyWingsActive);
        }

        // Update prosthetic enhancement inline control visibility when Enhanced/Improved Enhanced/Extraneous is toggled
        if (option.getName().equals(OptionsConstants.MD_PL_ENHANCED)
              || option.getName().equals(OptionsConstants.MD_PL_I_ENHANCED)
              || option.getName().equals(OptionsConstants.MD_PL_EXTRA_LIMBS)) {
            updateInlineProstheticVisibility(option.getName(), state);
        }

        // Gas Effuser (Pheromone/Toxin) is only for Conventional Infantry (IO pg 79)
        if (state && !entity.isConventionalInfantry()
              && (option.getName().equals(OptionsConstants.MD_GAS_EFFUSER_PHEROMONE)
              || option.getName().equals(OptionsConstants.MD_GAS_EFFUSER_TOXIN))) {
            comp.setSelected(false);
            JOptionPane.showMessageDialog(this,
                  Messages.getString("CustomMekDialog.GasEffuserInfantryOnly"),
                  Messages.getString("CustomMekDialog.GasEffuserInfantryOnlyTitle"),
                  JOptionPane.WARNING_MESSAGE);
        }

        // Can only have one Gas Effuser type at a time (IO pg 79)
        if (state && option.getName().equals(OptionsConstants.MD_GAS_EFFUSER_PHEROMONE)
              && hasOtherGasEffuserSelected(comp, OptionsConstants.MD_GAS_EFFUSER_TOXIN)) {
            comp.setSelected(false);
            JOptionPane.showMessageDialog(this,
                  Messages.getString("CustomMekDialog.GasEffuserOnlyOne"),
                  Messages.getString("CustomMekDialog.GasEffuserOnlyOneTitle"),
                  JOptionPane.WARNING_MESSAGE);
        }

        if (state && option.getName().equals(OptionsConstants.MD_GAS_EFFUSER_TOXIN)
              && hasOtherGasEffuserSelected(comp, OptionsConstants.MD_GAS_EFFUSER_PHEROMONE)) {
            comp.setSelected(false);
            JOptionPane.showMessageDialog(this,
                  Messages.getString("CustomMekDialog.GasEffuserOnlyOne"),
                  Messages.getString("CustomMekDialog.GasEffuserOnlyOneTitle"),
                  JOptionPane.WARNING_MESSAGE);
        }

        // DNI types are mutually exclusive - can only have one of VDNI, BVDNI, or Proto DNI
        if (state && isDniOption(option.getName())) {
            deselectOtherDniOptions(comp);
        }

        // Glider wings limit extraneous limbs to one pair (IO p.85)
        if (state && option.getName().equals(OptionsConstants.MD_PL_GLIDER)
              && entity.isConventionalInfantry()) {
            updateExtraneousPair2ForGliderWings(true);
        } else if (!state && option.getName().equals(OptionsConstants.MD_PL_GLIDER)
              && entity.isConventionalInfantry()) {
            updateExtraneousPair2ForGliderWings(false);
        }

        // Proto DNI is BattleMek only (IO pg 83)
        if (state && option.getName().equals(OptionsConstants.MD_PROTO_DNI)
              && !isValidForProtoDni(entity)) {
            comp.setSelected(false);
            JOptionPane.showMessageDialog(this,
                  Messages.getString("CustomMekDialog.ProtoDniBattleMekOnly"),
                  Messages.getString("CustomMekDialog.ProtoDniBattleMekOnlyTitle"),
                  JOptionPane.WARNING_MESSAGE);
        }

        // VDNI/BVDNI valid for BM, IM, BA, CV, SV, AF, CF (IO pg 71)
        if (state && (option.getName().equals(OptionsConstants.MD_VDNI)
              || option.getName().equals(OptionsConstants.MD_BVDNI))
              && !isValidForVdni(entity)) {
            comp.setSelected(false);
            JOptionPane.showMessageDialog(this,
                  Messages.getString("CustomMekDialog.VdniInvalidUnitType"),
                  Messages.getString("CustomMekDialog.VdniInvalidUnitTypeTitle"),
                  JOptionPane.WARNING_MESSAGE);
        }

        // EI Implant pilot option automatically adds/removes EI Interface equipment (IO p.69)
        // The pilot needs the implant AND the unit needs the interface hardware for EI to function
        // ProtoMeks already have EI built-in, so no action needed for them
        if (option.getName().equals(OptionsConstants.MD_EI_IMPLANT)) {
            boolean anyChanged = false;
            for (Entity e : entities) {
                if (canHaveEIInterface(e)) {
                    boolean hadEI = e.hasEiCockpit();
                    setEIInterface(e, state);
                    if (hadEI != state) {
                        anyChanged = true;
                    }
                }
            }
            // Show feedback message - use invokeLater to avoid interfering with checkbox event handling
            if (anyChanged) {
                final boolean added = state;
                javax.swing.SwingUtilities.invokeLater(() -> {
                    String msg = added
                          ? Messages.getString("CustomMekDialog.EIInterfaceAdded")
                          : Messages.getString("CustomMekDialog.EIInterfaceRemoved");
                    String title = Messages.getString("CustomMekDialog.EIInterfaceTitle");
                    JOptionPane.showMessageDialog(this, msg, title, JOptionPane.INFORMATION_MESSAGE);
                });
            }
        }
    }

    /**
     * Checks if the given option name is a sensory implant.
     */
    private boolean isSensoryImplant(String optionName) {
        return optionName.equals(OptionsConstants.MD_CYBER_IMP_AUDIO)
              || optionName.equals(OptionsConstants.MD_CYBER_IMP_VISUAL)
              || optionName.equals(OptionsConstants.MD_CYBER_IMP_LASER)
              || optionName.equals(OptionsConstants.MD_CYBER_IMP_TELE);
    }

    /**
     * Counts the number of sensory implants currently selected, excluding the given component.
     */
    private int countSelectedSensoryImplants(DialogOptionComponentYPanel excludeComp) {
        int count = 0;
        for (DialogOptionComponentYPanel optComp : optionComps) {
            if (optComp == excludeComp) {
                continue;
            }
            if (isSensoryImplant(optComp.getOption().getName())
                  && Boolean.TRUE.equals(optComp.getValue())) {
                count++;
            }
        }
        return count;
    }

    /**
     * Checks if another gas effuser of the specified type is already selected.
     */
    private boolean hasOtherGasEffuserSelected(DialogOptionComponentYPanel excludeComp, String otherEffuserName) {
        for (DialogOptionComponentYPanel optComp : optionComps) {
            if (optComp == excludeComp) {
                continue;
            }
            if (optComp.getOption().getName().equals(otherEffuserName)
                  && Boolean.TRUE.equals(optComp.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deselects the option with the given name.
     */
    private void deselectOption(String optionName) {
        for (DialogOptionComponentYPanel optComp : optionComps) {
            if (optComp.getOption().getName().equals(optionName)) {
                optComp.setSelected(false);
                break;
            }
        }
    }

    /**
     * Checks if a boolean option is currently selected.
     */
    private boolean isOptionSelected(String optionName) {
        for (DialogOptionComponentYPanel optComp : optionComps) {
            if (optComp.getOption().getName().equals(optionName)) {
                Object value = optComp.getValue();
                return (value instanceof Boolean) && (Boolean) value;
            }
        }
        return false;
    }

    /**
     * Updates extraneous limbs pair 2 availability based on glider wings state. Per IO p.85, glider wings limit
     * extraneous limbs to one pair.
     *
     * @param gliderWingsEnabled True if glider wings is being enabled
     */
    private void updateExtraneousPair2ForGliderWings(boolean gliderWingsEnabled) {
        if (choExtraneousPair2 == null) {
            return;
        }

        if (gliderWingsEnabled) {
            // Clear and disable pair 2
            if (choExtraneousPair2.getSelectedIndex() > 0) {
                choExtraneousPair2.setSelectedIndex(0);
                JOptionPane.showMessageDialog(this,
                      Messages.getString("CustomMekDialog.GliderWingsLimitExtraneousLimbs"),
                      Messages.getString("CustomMekDialog.GliderWingsLimitExtraneousLimbsTitle"),
                      JOptionPane.INFORMATION_MESSAGE);
            }
            choExtraneousPair2.setEnabled(false);
            if (lblExtraneousPair2 != null) {
                lblExtraneousPair2.setEnabled(false);
            }
        } else {
            // Re-enable pair 2
            choExtraneousPair2.setEnabled(editable);
            if (lblExtraneousPair2 != null) {
                lblExtraneousPair2.setEnabled(true);
            }
        }
    }

    /**
     * Checks if the given option name is a Direct Neural Interface type. VDNI, BVDNI, and Proto DNI are mutually
     * exclusive.
     */
    private boolean isDniOption(String optionName) {
        return optionName.equals(OptionsConstants.MD_VDNI)
              || optionName.equals(OptionsConstants.MD_BVDNI)
              || optionName.equals(OptionsConstants.MD_PROTO_DNI);
    }

    /**
     * Deselects other DNI options when one is selected (they are mutually exclusive).
     */
    private void deselectOtherDniOptions(DialogOptionComponentYPanel selectedComp) {
        for (DialogOptionComponentYPanel optComp : optionComps) {
            if (optComp == selectedComp) {
                continue;
            }
            if (isDniOption(optComp.getOption().getName())
                  && Boolean.TRUE.equals(optComp.getValue())) {
                optComp.setSelected(false);
            }
        }
    }

    /**
     * Checks if entity is valid for Proto DNI (BattleMek only, not IndustrialMek).
     */
    private boolean isValidForProtoDni(Entity entity) {
        return entity.isMek() && !entity.isIndustrialMek();
    }

    /**
     * Checks if entity is valid for VDNI/BVDNI (BM, IM, BA, CV, SV, AF, CF).
     */
    private boolean isValidForVdni(Entity entity) {
        return entity.isMek()
              || entity.isBattleArmor()
              || entity.isCombatVehicle()
              || entity.isSupportVehicle()
              || entity.isAerospaceFighter()
              || entity.isConventionalFighter();
    }

    /**
     * Updates the extraneous limbs pair 2 control based on wing selection. Per IO p.85, when any wing type (glider or
     * powered flight) is installed, only one pair of extraneous limbs is allowed.
     *
     * @param hasWings true if any wing option is selected
     */
    private void updateExtraneousPair2ForWings(boolean hasWings) {
        if (choExtraneousPair2 != null) {
            choExtraneousPair2.setEnabled(!hasWings && editable);
            if (hasWings) {
                choExtraneousPair2.setSelectedIndex(0); // Reset to "None"
            }
        }
        if (lblExtraneousPair2 != null) {
            lblExtraneousPair2.setEnabled(!hasWings);
        }
    }

    @Override
    public void optionSwitched(DialogOptionComponentYPanel clickedComp, IOption option, int i) {
        // nothing implemented yet
    }

    public boolean isOkay() {
        return okay;
    }

    public int getStatus() {
        return status;
    }

    private void refreshDeployment() {
        if (this.clientGUI == null) {
            return;
        }
        Entity entity = entities.get(0);

        if (entity instanceof QuadVee) {
            choStartingMode.removeItemListener(this);
            choStartingMode.removeAllItems();
            choStartingMode.addItem(Messages.getString("CustomMekDialog.ModeQuad"));
            choStartingMode.addItem(Messages.getString("CustomMekDialog.ModeVehicle"));
            if (entity.getConversionMode() == QuadVee.CONV_MODE_VEHICLE) {
                choStartingMode.setSelectedIndex(1);
            }
            updateStartingModeOptions();
            choStartingMode.addItemListener(this);
        } else if (entity instanceof LandAirMek) {
            choStartingMode.removeItemListener(this);
            choStartingMode.removeAllItems();
            choStartingMode.addItem(Messages.getString("CustomMekDialog.ModeBiped"));
            if (((LandAirMek) entity).getLAMType() != LandAirMek.LAM_BIMODAL) {
                choStartingMode.addItem(Messages.getString("CustomMekDialog.ModeAirMek"));
            }
            choStartingMode.addItem(Messages.getString("CustomMekDialog.ModeFighter"));
            if (entity.getConversionMode() == LandAirMek.CONV_MODE_AIR_MEK) {
                choStartingMode.setSelectedIndex(1);
            } else if (entity.getConversionMode() == LandAirMek.CONV_MODE_FIGHTER) {
                choStartingMode.setSelectedIndex(choStartingMode.getItemCount() - 1);
            }
            updateStartingModeOptions();
            choStartingMode.addItemListener(this);
        }

        choDeploymentZone.removeItemListener(this);
        txtDeploymentOffset.setEnabled(false);
        txtDeploymentWidth.setEnabled(false);

        choDeploymentRound.removeAllItems();
        choDeploymentRound.addItem(Messages.getString("CustomMekDialog.StartOfGame"));

        if (entity.getDeployRound() < 1) {
            choDeploymentRound.setSelectedIndex(0);
        }

        for (int i = 1; i <= 40; i++) {
            choDeploymentRound.addItem(Messages.getString("CustomMekDialog.AfterRound") + i);
            if (entity.getDeployRound() == i) {
                choDeploymentRound.setSelectedIndex(i);
            }
        }

        if (entity.getTransportId() != Entity.NONE) {
            choDeploymentRound.setEnabled(false);
        }

        choDeploymentZone.removeAllItems();
        choDeploymentZone.addItem(Messages.getString("CustomMekDialog.useOwners"));
        choDeploymentZone.addItem(Messages.getString("CustomMekDialog.deployAny"));
        choDeploymentZone.addItem(Messages.getString("CustomMekDialog.deployNW"));
        choDeploymentZone.addItem(Messages.getString("CustomMekDialog.deployN"));
        choDeploymentZone.addItem(Messages.getString("CustomMekDialog.deployNE"));
        choDeploymentZone.addItem(Messages.getString("CustomMekDialog.deployE"));
        choDeploymentZone.addItem(Messages.getString("CustomMekDialog.deploySE"));
        choDeploymentZone.addItem(Messages.getString("CustomMekDialog.deployS"));
        choDeploymentZone.addItem(Messages.getString("CustomMekDialog.deploySW"));
        choDeploymentZone.addItem(Messages.getString("CustomMekDialog.deployW"));
        choDeploymentZone.addItem(Messages.getString("CustomMekDialog.deployEdge"));
        choDeploymentZone.addItem(Messages.getString("CustomMekDialog.deployCenter"));

        if (client.getGame().getPhase().isLounge()) {
            for (int zoneID : ServerBoardHelper.getPossibleGameBoard(clientGUI.getClient().getMapSettings(), true)
                  .getCustomDeploymentZones()) {
                choDeploymentZone.addItem("Zone " + zoneID);
            }
        }

        int startingPos = entity.getStartingPos(false);

        if (entity.getStartingPos(false) < Board.NUM_ZONES) {
            choDeploymentZone.setSelectedIndex(startingPos + 1);
        } else {
            choDeploymentZone.setSelectedItem("Zone " + startingPos);
        }

        choDeploymentZone.addItemListener(this);

        txtDeploymentOffset.setText(Integer.toString(entity.getStartingOffset(false)));
        txtDeploymentWidth.setText(Integer.toString(entity.getStartingWidth(false)));

        MapSettings mapSettings = clientGUI.getClient().getMapSettings();
        int boardHeight = mapSettings.getBoardHeight() * mapSettings.getMapHeight();
        int boardWidth = mapSettings.getBoardWidth() * mapSettings.getMapWidth();

        int x = Math.min(entity.getStartingAnyNWx(false) + 1, boardWidth);
        spinStartingAnyNWx.setValue(x);
        int y = Math.min(entity.getStartingAnyNWy(false) + 1, boardHeight);
        spinStartingAnyNWy.setValue(y);
        x = Math.min(entity.getStartingAnySEx(false) + 1, boardWidth);
        spinStartingAnySEx.setValue(x);
        y = Math.min(entity.getStartingAnySEy(false) + 1, boardHeight);
        spinStartingAnySEy.setValue(y);

        boolean enableDeploymentZoneControls = choDeploymentZone.isEnabled() &&
              (choDeploymentZone.getSelectedIndex() > 0) &&
              (choDeploymentZone.getSelectedIndex() < Board.NUM_ZONES);
        txtDeploymentOffset.setEnabled(enableDeploymentZoneControls);
        txtDeploymentWidth.setEnabled(enableDeploymentZoneControls);

        // disable some options if not allowed to edit deployment
        choStartingMode.setEnabled(editableDeployment);
        choDeploymentZone.setEnabled(editableDeployment);
        txtDeploymentOffset.setEnabled(editableDeployment);
        txtDeploymentWidth.setEnabled(editableDeployment);
        choDeploymentRound.setEnabled(editableDeployment);

        chHidden.removeActionListener(this);
        boolean enableHidden = !(entity instanceof Dropship) && !entity.isAirborne() && !entity.isAirborneVTOLorWIGE();
        labHidden.setEnabled(enableHidden);
        chHidden.setEnabled(enableHidden);
        chHidden.addActionListener(this);

        chDeployStealth.removeActionListener(this);
        boolean enabledStealth = (entity.hasStealth());
        labDeployStealth.setEnabled(enabledStealth);
        chDeployStealth.setEnabled(enabledStealth);
        chDeployStealth.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {

        if (actionEvent.getSource().equals(butOffBoardDistance)) {
            // We'll allow the player to deploy at the maximum possible
            // effective range, even if many of the unit's weapons would be out of range
            int maxDistance = 0;
            for (Entity entity : entities) {
                for (WeaponMounted wep : entity.getWeaponList()) {
                    WeaponType w = wep.getType();
                    int nDistance = 0;
                    if (w.hasFlag(WeaponType.F_ARTILLERY)) {
                        if (w instanceof ArtilleryBayWeapon) {
                            // Artillery bays can mix and match, so limit the bay
                            // to the shortest range of the weapons in it
                            nDistance = getBayShortestRange(wep);
                        } else {
                            // Max TO range in map sheets - 1 for the actual play area
                            nDistance = (w.getLongRange() - 1);
                        }
                    } else if (w.isCapital() || w.isSubCapital()) {
                        // Capital weapons use their maximum space hex range as the map sheet range
                        if (w.getMaxRange(wep) == WeaponType.RANGE_EXT) {
                            nDistance = 50;
                        }
                        if (w.getMaxRange(wep) == WeaponType.RANGE_LONG) {
                            nDistance = 40;
                        }
                        if (w.getMaxRange(wep) == WeaponType.RANGE_MED) {
                            nDistance = 24;
                        }
                        if (w.getMaxRange(wep) == WeaponType.RANGE_SHORT) {
                            nDistance = 12;
                        }
                    }
                    // Now, convert to map sheets
                    nDistance = nDistance * Board.DEFAULT_BOARD_HEIGHT;
                    // And set our maximum slider hex distance based on the calculations
                    if (nDistance > maxDistance) {
                        maxDistance = nDistance;
                    }
                }

            }
            SliderDialog sl = new SliderDialog(clientGUI.getFrame(),
                  Messages.getString("CustomMekDialog.offboardDistanceTitle"),
                  Messages.getString("CustomMekDialog.offboardDistanceQuestion"),
                  Math.min(Math.max(entities.get(0).getOffBoardDistance(), 17), maxDistance),
                  17,
                  maxDistance);
            if (!sl.showDialog()) {
                return;
            }
            distance = sl.getValue();
            butOffBoardDistance.setText(Integer.toString(distance));
            return;
        }

        if (actionEvent.getActionCommand().equals("missing")) {
            // If we're down to a single crew member, do not allow anymore to be removed.
            final long remaining = Arrays.stream(panCrewMember).filter(p -> !p.getMissing()).count();
            for (CustomPilotViewPanel v : panCrewMember) {
                v.enableMissing(remaining > 1 || v.getMissing());
            }
            return;
        }

        if (actionEvent.getSource() == butPrev) {
            status = PREV;
            okButtonActionPerformed(actionEvent);
        } else if (actionEvent.getSource() == butNext) {
            status = NEXT;
            okButtonActionPerformed(actionEvent);
        } else if (actionEvent.getSource() == butOkay) {
            status = DONE;
            okButtonActionPerformed(actionEvent);
        }
    }

    private static int getBayShortestRange(WeaponMounted wep) {
        int bayShortestRange = 150; // Cruise missile/120
        for (WeaponMounted bayWeapons : wep.getBayWeapons()) {
            // Max TO range in map sheets - 1 for the actual play area
            int currentDistance = (bayWeapons.getType().getLongRange() - 1);
            if (currentDistance < bayShortestRange) {
                bayShortestRange = currentDistance;
            }
        }
        return bayShortestRange;
    }

    @Override
    protected void okAction() {
        // Set instanceof flags
        String msg, title;
        boolean isAero = true;
        boolean isShip = true;
        boolean isVTOL = true;
        boolean isWiGE = true;
        boolean isQuadVee = true;
        boolean isLAM = true;
        boolean isAirMek = true;
        boolean isGlider = true;
        for (Entity e : entities) {
            isAero &= ((e instanceof Aero) && !((e instanceof SmallCraft) || (e instanceof Jumpship))) ||
                  ((e instanceof LandAirMek) &&
                        (choStartingMode.getSelectedIndex() == 2 ||
                              ((LandAirMek) e).getLAMType() == LandAirMek.LAM_BIMODAL &&
                                    choStartingMode.getSelectedIndex() == 1));
            isShip &= (e instanceof SmallCraft) || (e instanceof Jumpship);
            isVTOL &= (e.getMovementMode() == EntityMovementMode.VTOL);
            isWiGE &= (e instanceof Tank) && (e.getMovementMode() == EntityMovementMode.WIGE);
            isQuadVee &= (e instanceof QuadVee);
            isLAM &= (e instanceof LandAirMek);
            isAirMek &= (e instanceof LandAirMek) &&
                  (((LandAirMek) e).getLAMType() == LandAirMek.LAM_STANDARD) &&
                  (choStartingMode.getSelectedIndex() == 1);
            isGlider &= (e instanceof ProtoMek) && (e.getMovementMode() == EntityMovementMode.WIGE);
        }

        // get values
        int init;
        int command;
        int velocity = 0;
        int altitude = 0;
        int currentFuel = 0;
        int height = 0;
        int offBoardDistance;
        init = MathUtility.parseInt(fldInit.getText(), 0);
        command = MathUtility.parseInt(fldCommandInit.getText(), 0);
        if (isAero || isShip) {
            velocity = MathUtility.parseInt(fldStartVelocity.getText(), 0);
            altitude = MathUtility.parseInt(fldStartAltitude.getText(), 0);
            currentFuel = MathUtility.parseInt(fldCurrentFuel.getText(), 0);
        }
        if (isVTOL || isAirMek) {
            height = MathUtility.parseInt(fldStartHeight.getText(), 0);
        }
        if (isWiGE) {
            height = chDeployAirborne.isSelected() ? 1 : 0;
        }

        if (isAero || isShip) {
            if ((velocity > (2 * entities.get(0).getWalkMP())) || (velocity < 0)) {
                msg = Messages.getString("CustomMekDialog.EnterCorrectVelocity");
                title = Messages.getString("CustomMekDialog.NumberFormatError");
                JOptionPane.showMessageDialog(clientGUI == null ? this : clientGUI.getFrame(),
                      msg,
                      title,
                      JOptionPane.ERROR_MESSAGE);
                return;
            } else if ((altitude < 0) || (altitude > 10)) {
                msg = Messages.getString("CustomMekDialog.EnterCorrectAltitude");
                title = Messages.getString("CustomMekDialog.NumberFormatError");
                JOptionPane.showMessageDialog(clientGUI == null ? this : clientGUI.getFrame(),
                      msg,
                      title,
                      JOptionPane.ERROR_MESSAGE);
                return;
            } else if ((currentFuel < 0) || (currentFuel > fuel)) {
                msg = (Messages.getString("CustomMekDialog.EnterCorrectFuel") + fuel + ".");
                title = Messages.getString("CustomMekDialog.NumberFormatError");
                JOptionPane.showMessageDialog(clientGUI == null ? this : clientGUI.getFrame(),
                      msg,
                      title,
                      JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        if ((isVTOL && height > 50) || (isAirMek && height > 25) || (isGlider && height > 12)) {
            msg = Messages.getString("CustomMekDialog.EnterCorrectHeight");
            title = Messages.getString("CustomMekDialog.NumberFormatError");
            JOptionPane.showMessageDialog(clientGUI == null ? this : clientGUI.getFrame(),
                  msg,
                  title,
                  JOptionPane.ERROR_MESSAGE);
            return;
        }
        // Apply single-entity settings
        if (entities.size() == 1) {
            Entity entity = entities.get(0);

            for (int i = 0; i < entities.get(0).getCrew().getSlotCount(); i++) {
                String name = panCrewMember[i].getPilotName();
                String nick = panCrewMember[i].getNickname();
                String hits = panCrewMember[i].getHits();
                Gender gender = panCrewMember[i].getGender();
                if (gender == Gender.RANDOMIZE) {
                    gender = entities.get(0).getCrew().getGender(i);
                }
                boolean missing = panCrewMember[i].getMissing();
                int gunnery;
                int gunneryL;
                int gunneryM;
                int gunneryB;
                int artillery;
                int piloting;
                int gunneryAero;
                int gunneryAeroL;
                int gunneryAeroM;
                int gunneryAeroB;
                int pilotingAero;
                int tough;
                int fatigue;
                int backup = panCrewMember[i].getBackup();

                gunnery = panCrewMember[i].getGunnery();
                gunneryL = panCrewMember[i].getGunneryL();
                gunneryM = panCrewMember[i].getGunneryM();
                gunneryB = panCrewMember[i].getGunneryB();
                piloting = panCrewMember[i].getPiloting();
                gunneryAero = panCrewMember[i].getGunneryAero();
                gunneryAeroL = panCrewMember[i].getGunneryAeroL();
                gunneryAeroM = panCrewMember[i].getGunneryAeroM();
                gunneryAeroB = panCrewMember[i].getGunneryAeroB();
                pilotingAero = panCrewMember[i].getPilotingAero();
                artillery = panCrewMember[i].getArtillery();
                tough = panCrewMember[i].getToughness();
                fatigue = panCrewMember[i].getCrewFatigue();

                // keep these reasonable, please
                if ((gunnery < 0) ||
                      (gunnery > 8) ||
                      (piloting < 0) ||
                      (piloting > 8) ||
                      (gunneryL < 0) ||
                      (gunneryL > 8) ||
                      (gunneryM < 0) ||
                      (gunneryM > 8) ||
                      (gunneryB < 0) ||
                      (gunneryB > 8) ||
                      (gunneryAero < 0) ||
                      (gunneryAero > 8) ||
                      (pilotingAero < 0) ||
                      (pilotingAero > 8) ||
                      (gunneryAeroL < 0) ||
                      (gunneryAeroL > 8) ||
                      (gunneryAeroM < 0) ||
                      (gunneryAeroM > 8) ||
                      (gunneryAeroB < 0) ||
                      (gunneryAeroB > 8) ||
                      (artillery < 0) ||
                      (artillery > 8)) {
                    msg = Messages.getString("CustomMekDialog.EnterSkillsBetween0_8");
                    title = Messages.getString("CustomMekDialog.NumberFormatError");
                    JOptionPane.showMessageDialog(clientGUI == null ? this : clientGUI.getFrame(),
                          msg,
                          title,
                          JOptionPane.ERROR_MESSAGE);
                    return;
                }

                long gunneryRounded = Math.round((gunneryL + gunneryB + gunneryM) / 3.0);
                if (entity.getCrew() instanceof LAMPilot pilot) {
                    if (client.getGame().getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY)) {
                        pilot.setGunneryMekL(gunneryL);
                        pilot.setGunneryMekB(gunneryB);
                        pilot.setGunneryMekM(gunneryM);
                        pilot.setGunneryMek((int) gunneryRounded);
                        pilot.setGunneryAeroL(gunneryAeroL);
                        pilot.setGunneryAeroB(gunneryAeroB);
                        pilot.setGunneryAeroM(gunneryAeroM);
                        pilot.setGunneryAero((int) Math.round((gunneryAeroL + gunneryAeroB + gunneryAeroM) / 3.0));
                    } else {
                        pilot.setGunneryMekL(gunnery);
                        pilot.setGunneryMekB(gunnery);
                        pilot.setGunneryMekM(gunnery);
                        pilot.setGunneryMek(gunnery);
                        pilot.setGunneryAeroL(gunneryAero);
                        pilot.setGunneryAeroB(gunneryAero);
                        pilot.setGunneryAeroM(gunneryAero);
                        pilot.setGunneryAero(gunneryAero);
                    }
                    pilot.setPilotingMek(piloting);
                    pilot.setPilotingAero(pilotingAero);
                } else {
                    if (client.getGame().getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY)) {
                        entity.getCrew().setGunneryL(gunneryL, i);
                        entity.getCrew().setGunneryB(gunneryB, i);
                        entity.getCrew().setGunneryM(gunneryM, i);
                        entity.getCrew().setGunnery((int) gunneryRounded, i);
                    } else {
                        entity.getCrew().setGunnery(gunnery, i);
                        entity.getCrew().setGunneryL(gunnery, i);
                        entity.getCrew().setGunneryB(gunnery, i);
                        entity.getCrew().setGunneryM(gunnery, i);
                    }
                    entity.getCrew().setPiloting(piloting, i);
                }
                if (gameOptions().booleanOption(OptionsConstants.RPG_ARTILLERY_SKILL)) {
                    entity.getCrew().setArtillery(artillery, i);
                } else {
                    entity.getCrew().setArtillery(entity.getCrew().getGunnery(i), i);
                }
                entity.getCrew().setMissing(missing, i);
                entity.getCrew().setToughness(tough, i);
                entity.getCrew().setCrewFatigue(fatigue, i);
                entity.getCrew().setName(name, i);
                entity.getCrew().setNickname(nick, i);
                entity.getCrew().setHits(MathUtility.parseInt(hits, 0), i);
                entity.getCrew().setGender(gender, i);
                entity.getCrew().setClanPilot(panCrewMember[i].isClanPilot(), i);
                if (clientGUI != null) {
                    entity.getCrew().setPortrait(panCrewMember[i].getPortrait().clone(), i);
                }
                if (backup >= 0) {
                    if (i == entity.getCrew().getCrewType().getPilotPos()) {
                        entity.getCrew().setBackupPilotPos(backup);
                    } else if (i == entity.getCrew().getCrewType().getGunnerPos()) {
                        entity.getCrew().setBackupGunnerPos(backup);
                    }
                }

                // If the player wants to swap unit numbers, update both entities and send an update packet for the
                // other entity.
                Entity other = panCrewMember[i].getEntityUnitNumSwap();
                if (null != other) {
                    short temp = entity.getUnitNumber();
                    entity.setUnitNumber(other.getUnitNumber());
                    other.setUnitNumber(temp);
                    client.sendUpdateEntity(other);
                }
            }
            entity.getCrew().setInitBonus(init);
            entity.getCrew().setCommandBonus(command);

            // update commander status
            entity.setCommander(chCommander.isSelected());

            setOptions();
            setQuirks();
            setPartReps();
            m_equip.applyChoices();

            // Apply prosthetic enhancement and extraneous limbs for conventional infantry
            if (entity.isConventionalInfantry()) {
                applyProstheticEnhancement(entity);
                applyExtraneousLimbs(entity);
            }

            if (entity instanceof BattleArmor) {
                // have to reset internals because of dermal armor option
                if (entity.hasAbility(OptionsConstants.MD_DERMAL_ARMOR)) {
                    ((BattleArmor) entity).setInternal(2);
                } else {
                    ((BattleArmor) entity).setInternal(1);
                }
            }
        }

        // Apply multiple-entity settings
        for (Entity entity : entities) {
            entity.setHidden(chHidden.isSelected());
            setStealth(entity, chDeployStealth.isSelected());

            if (chOffBoard.isSelected()) {
                offBoardDistance = distance;
                if (offBoardDistance < 17) {
                    msg = Messages.getString("CustomMekDialog.OffboardDistance");
                    title = Messages.getString("CustomMekDialog.NumberFormatError");
                    JOptionPane.showMessageDialog(clientGUI == null ? this : clientGUI.getFrame(),
                          msg,
                          title,
                          JOptionPane.ERROR_MESSAGE);
                    return;
                }
                entity.setOffBoard(offBoardDistance,
                      OffBoardDirection.getDirection(choOffBoardDirection.getSelectedIndex()));
            } else {
                entity.setOffBoard(0, OffBoardDirection.NONE);
            }

            if (isAero || isShip) {
                IAero a = (IAero) entity;
                a.setCurrentVelocity(velocity);
                a.setNextVelocity(velocity);
                a.setCurrentFuel(currentFuel);
                if (!space) {
                    // we need to determine whether this aero is airborne or not in order for prohibited terrain and
                    // stacking to work right in the deployment phase. This is very tricky because in atmosphere,
                    // zero altitude does not necessarily mean grounded
                    if (altitude == 0) {
                        a.land();
                    } else {
                        a.liftOff(altitude);
                    }
                }
            }

            if (isVTOL || isWiGE || isAirMek || isGlider) {
                entity.setElevation(height);
            }

            // Set the entity's starting mode
            if (isQuadVee) {
                entity.setConversionMode(choStartingMode.getSelectedIndex());
            } else if (isLAM) {
                if (choStartingMode.getSelectedIndex() == 2) {
                    entity.setConversionMode(LandAirMek.CONV_MODE_FIGHTER);
                } else if (choStartingMode.getSelectedIndex() == 1) {
                    entity.setConversionMode(LandAirMek.CONV_MODE_FIGHTER);
                    entity.setConversionMode(((LandAirMek) entity).getLAMType() == LandAirMek.LAM_BIMODAL ?
                          LandAirMek.CONV_MODE_FIGHTER :
                          LandAirMek.CONV_MODE_AIR_MEK);
                } else {
                    entity.setConversionMode(LandAirMek.CONV_MODE_MEK);
                }
            }

            // Set the entity's deployment position and round.
            // parse the non-standard deployment zones
            int zoneID = choDeploymentZone.getSelectedIndex() - 1;
            if (zoneID >= Board.NUM_ZONES) {
                zoneID = MathUtility.parseInt(Objects.requireNonNull(choDeploymentZone.getSelectedItem())
                      .toString()
                      .substring(5), 0);
                zoneID = Board.encodeCustomDeploymentZoneID(zoneID);
            }

            entity.setStartingPos(zoneID);
            entity.setDeployRound(choDeploymentRound.getSelectedIndex());
            entity.setStartingOffset(MathUtility.parseInt(txtDeploymentOffset.getText(), 0));
            entity.setStartingWidth(MathUtility.parseInt(txtDeploymentWidth.getText(), 0));

            int x = Math.min((Integer) spinStartingAnyNWx.getValue(), (Integer) spinStartingAnySEx.getValue());
            int y = Math.min((Integer) spinStartingAnyNWy.getValue(), (Integer) spinStartingAnySEy.getValue());
            entity.setStartingAnyNWx(x - 1);
            entity.setStartingAnyNWy(y - 1);
            x = Math.max((Integer) spinStartingAnyNWx.getValue(), (Integer) spinStartingAnySEx.getValue());
            y = Math.max((Integer) spinStartingAnyNWy.getValue(), (Integer) spinStartingAnySEy.getValue());
            entity.setStartingAnySEx(x - 1);
            entity.setStartingAnySEy(y - 1);

            // Should the entity begin the game shutdown?
            if (chDeployShutdown.isSelected() && gameOptions().booleanOption(OptionsConstants.RPG_BEGIN_SHUTDOWN)) {
                entity.performManualShutdown();
            } else { // We need to else this in case someone turned the option
                // on, set their units, and then turned the option off.
                entity.performManualStartup();
            }

            // LAMs in fighter mode or airborne AirMeks ignore the prone and hull down
            // selections.
            if (!isLAM || (!isAero && entity.getElevation() == 0)) {
                // Should the entity begin the game prone?
                entity.setProne(chDeployProne.isSelected());

                // Should the entity begin the game prone?
                entity.setHullDown(chDeployHullDown.isSelected());
            }
        }

        okay = true;
        if ((clientGUI != null) && (clientGUI.chatlounge != null)) {
            clientGUI.chatlounge.refreshEntities();
        }

        // Check validity of units after customization
        for (Entity entity : entities) {
            TestEntity testEntity = TestEntity.getEntityVerifier(entity);
            int gameTL = TechConstants.getGameTechLevel(client.getGame(), entity.isClan());
            entity.setDesignValid((testEntity == null) || testEntity.correctEntity(new StringBuffer(), gameTL));
        }

        setVisible(false);
    }

    @Override
    public void itemStateChanged(ItemEvent itemEvent) {
        if (itemEvent.getSource().equals(choStartingMode)) {
            updateStartingModeOptions();
        }
        if (itemEvent.getSource().equals(chDeployProne)) {
            chDeployHullDown.setSelected(false);
            return;
        }
        if (itemEvent.getSource().equals(chDeployHullDown)) {
            chDeployProne.setSelected(false);
            return;
        }

        if (itemEvent.getSource().equals(choDeploymentZone)) {
            boolean enableDeploymentZoneControls = choDeploymentZone.isEnabled() &&
                  (choDeploymentZone.getSelectedIndex() > 0) &&
                  choDeploymentZone.getSelectedIndex() < Board.NUM_ZONES;
            txtDeploymentOffset.setEnabled(enableDeploymentZoneControls);
            txtDeploymentWidth.setEnabled(enableDeploymentZoneControls);
        }
    }

    private void updateStartingModeOptions() {
        final int index = choStartingMode.getSelectedIndex();
        if (entities.get(0) instanceof QuadVee) {
            labDeployProne.setEnabled(index == 0);
            chDeployProne.setEnabled(index == 0);
        } else if (entities.get(0) instanceof LandAirMek) {
            int mode = index;
            if (((LandAirMek) entities.get(0)).getLAMType() == LandAirMek.LAM_BIMODAL &&
                  mode == LandAirMek.CONV_MODE_AIR_MEK) {
                mode = LandAirMek.CONV_MODE_FIGHTER;
            }
            labDeployProne.setEnabled(mode < LandAirMek.CONV_MODE_FIGHTER);
            chDeployProne.setEnabled(mode < LandAirMek.CONV_MODE_FIGHTER);
            labDeployHullDown.setEnabled(mode == LandAirMek.CONV_MODE_MEK);
            chDeployHullDown.setEnabled(mode == LandAirMek.CONV_MODE_MEK);
            labStartHeight.setEnabled(mode == LandAirMek.CONV_MODE_AIR_MEK);
            fldStartHeight.setEnabled(mode == LandAirMek.CONV_MODE_AIR_MEK);
            labStartVelocity.setEnabled(mode == LandAirMek.CONV_MODE_FIGHTER);
            fldStartVelocity.setEnabled(mode == LandAirMek.CONV_MODE_FIGHTER);
            labStartAltitude.setEnabled(mode == LandAirMek.CONV_MODE_FIGHTER);
            fldStartAltitude.setEnabled(mode == LandAirMek.CONV_MODE_FIGHTER);
        }
    }

    public Entity getNextEntity(boolean forward) {
        Game game = client.getGame();
        boolean bd = game.getOptions().booleanOption(OptionsConstants.BASE_BLIND_DROP);
        boolean rbd = game.getOptions().booleanOption(OptionsConstants.BASE_REAL_BLIND_DROP);
        Player p = client.getLocalPlayer();

        Entity nextOne;
        Entity entity;
        if (forward) {
            entity = entities.get(entities.size() - 1);
            nextOne = game.getNextEntityFromList(entity);
        } else {
            entity = entities.get(0);
            nextOne = game.getPreviousEntityFromList(entity);
        }
        while ((nextOne != null) && !entities.contains(nextOne)) {
            if (nextOne.getOwner().equals(p) || (!(bd || rbd) && nextOne.getOwner().equals(entity.getOwner()))) {
                return nextOne;
            }
            if (forward) {
                nextOne = game.getNextEntityFromList(nextOne);
            } else {
                nextOne = game.getPreviousEntityFromList(nextOne);
            }
        }
        return null;
    }

    private void setupEquip() {
        Entity entity = entities.get(0);
        GridBagLayout gbl = new GridBagLayout();
        panEquip.setLayout(gbl);
        m_equip = new EquipChoicePanel(entity, clientGUI, client);
        panEquip.add(m_equip, GBC.eol());
        // EI Interface is automatically added/removed based on pilot EI Implant option
        // No checkbox needed - the pilot option drives it (IO p.69)
    }

    private void setStealth(Entity e, boolean stealthEnabled) {
        int newStealth = (stealthEnabled) ? 1 : 0;
        EquipmentMode newMode = (stealthEnabled) ? EquipmentMode.getMode("On") : EquipmentMode.getMode("Off");
        for (MiscMounted m : e.getMiscEquipment(MiscType.F_STEALTH)) {
            if (m.curMode() == newMode) {
                continue;
            }

            m.setMode(newStealth);
            m.newRound(-1);
        }
    }

    /**
     * Checks if the entity is eligible for an EI Interface. Per IO p.69, EI Interface may be installed in any BattleMek
     * or Battle Armor built using a Clan technology base. ProtoMeks always have EI built-in and cannot toggle it. EI
     * Interface was introduced in 3040.
     *
     * @param entity the entity to check
     *
     * @return true if the entity can have an EI Interface toggled
     */
    private boolean canHaveEIInterface(Entity entity) {
        // EI Interface introduced in 3040
        int gameYear = client.getGame().getOptions().intOption(OptionsConstants.ALLOWED_YEAR);
        if (gameYear < 3040) {
            return false;
        }
        // ProtoMeks always have EI - cannot toggle
        if (entity.isProtoMek()) {
            return false;
        }
        // Only Meks and Battle Armor can have EI Interface
        if (!entity.isMek() && !(entity instanceof BattleArmor)) {
            return false;
        }
        // Must have Clan or Mixed tech base (IO p.69)
        return entity.isClan() || entity.isMixedTech();
    }

    /**
     * Sets the EI Interface equipment on an entity. Adds the equipment if enabled, removes it if disabled.
     *
     * @param entity  the entity to modify
     * @param enabled true to add EI Interface, false to remove it
     */
    private void setEIInterface(Entity entity, boolean enabled) {
        boolean hasEI = entity.hasEiCockpit();

        if (enabled && !hasEI) {
            // Add EI Interface equipment
            // Game year validation is done in canHaveEIInterface() - if we get here, the game year allows EI
            // EI is retrofittable equipment (IO p.69), so unit intro year doesn't matter
            try {
                EquipmentType eiType = EquipmentType.get("EIInterface");
                if (eiType != null) {
                    entity.addEquipment(eiType, Entity.LOC_NONE);
                }
            } catch (LocationFullException e) {
                // Should not happen for 0-slot equipment
            }
        } else if (!enabled && hasEI) {
            // Remove EI Interface equipment
            List<Mounted<?>> toRemove = new ArrayList<>();
            for (Mounted<?> mounted : entity.getEquipment()) {
                if ((mounted.getType() instanceof MiscType) &&
                      mounted.getType().hasFlag(MiscType.F_EI_INTERFACE)) {
                    toRemove.add(mounted);
                }
            }
            entity.getEquipment().removeAll(toRemove);
            entity.getMisc().removeAll(toRemove);
        }
    }

    @Override
    protected Container createCenterPane() {
        final Entity entity = entities.get(0);
        boolean multipleEntities = (entities.size() > 1) || (entity instanceof FighterSquadron);
        boolean quirksEnabled = gameOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS);
        boolean partialRepairsEnabled = gameOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_PARTIAL_REPAIRS);
        final boolean isMek = entities.stream().allMatch(e -> e instanceof Mek);
        final boolean isShip = entities.stream().allMatch(Entity::isLargeAerospace);
        final boolean isAero = entities.stream().allMatch(e -> e.isAero() && !e.isLargeAerospace());
        final boolean isVTOL = entities.stream().allMatch(e -> e.getMovementMode().isVTOL());
        final boolean isWiGE = entities.stream().allMatch(e -> (e instanceof Tank) && e.getMovementMode().isWiGE());
        final boolean isQuadVee = entities.stream().allMatch(e -> e instanceof QuadVee);
        final boolean isLAM = entities.stream().allMatch(e -> e instanceof LandAirMek);
        final boolean isGlider = entities.stream()
              .allMatch(e -> (e instanceof ProtoMek) && e.getMovementMode().isWiGE());
        final boolean hasStealth = entities.stream().allMatch(Entity::hasStealth);

        boolean eligibleForOffBoard = true;
        int boardHeight;
        int boardWidth;
        MapSettings mapSettings;

        if (this.clientGUI != null) {
            mapSettings = clientGUI.getClient().getMapSettings();
        } else {
            mapSettings = client.getMapSettings();
        }

        boardHeight = mapSettings.getMapHeight() * mapSettings.getBoardHeight();
        boardWidth = mapSettings.getMapWidth() * mapSettings.getBoardWidth();

        int x = Math.min(entity.getStartingAnyNWx(false) + 1, boardWidth);
        SpinnerNumberModel mStartingAnyNWx = new SpinnerNumberModel(x, 0, boardWidth, 1);
        spinStartingAnyNWx = new JSpinner(mStartingAnyNWx);
        spinStartingAnyNWx.setValue(x);
        int y = Math.min(entity.getStartingAnyNWy(false) + 1, boardHeight);
        SpinnerNumberModel mStartingAnyNWy = new SpinnerNumberModel(y, 0, boardHeight, 1);
        spinStartingAnyNWy = new JSpinner(mStartingAnyNWy);
        spinStartingAnyNWy.setValue(y);
        x = Math.min(entity.getStartingAnySEx(false) + 1, boardWidth);
        SpinnerNumberModel mStartingAnySEx = new SpinnerNumberModel(x, 0, boardWidth, 1);
        spinStartingAnySEx = new JSpinner(mStartingAnySEx);
        spinStartingAnySEx.setValue(x);
        y = Math.min(entity.getStartingAnySEy(false) + 1, boardHeight);
        SpinnerNumberModel mStartingAnySEy = new SpinnerNumberModel(y, 0, boardHeight, 1);
        spinStartingAnySEy = new JSpinner(mStartingAnySEy);
        spinStartingAnySEy.setValue(y);

        for (Entity e : entities) {
            // TODO : This check is good for now, but at some point we want atmospheric flying droppers to be able to
            //  lob offboard missiles and we could use it in space for extreme range bearings-only fights, plus
            //  Ortillery. Further, this should be revisited with a rules query when it comes to handling offboard
            //  gun emplacements, especially if they are allowed
            final boolean entityEligibleForOffBoard = !space &&
                  (e.getAltitude() == 0) &&
                  !(e.isBuildingEntityOrGunEmplacement()) &&
                  e.getWeaponList()
                        .stream()
                        .map(Mounted::getType)
                        .anyMatch(weaponType -> weaponType.hasFlag(WeaponType.F_ARTILLERY) ||
                              (weaponType instanceof CapitalMissileBayWeapon));
            eligibleForOffBoard &= entityEligibleForOffBoard;
        }
        // set up the panels
        JPanel mainPanel = new JPanel(new GridBagLayout());
        tabAll = new JTabbedPane();

        JPanel panCrew = new JPanel(new GridBagLayout());
        panCrewMember = new CustomPilotViewPanel[entity.getCrew().getSlotCount()];
        for (int i = 0; i < panCrewMember.length; i++) {
            panCrewMember[i] = new CustomPilotViewPanel(this, entity, i, editable);
        }
        JPanel panDeploy = new JPanel(new GridBagLayout());
        Quirks quirks = entity.getQuirks();
        panQuirks = new QuirksPanel(entity, quirks, editable, this, h_wpnQuirks);
        panPartReps = new JPanel(new GridBagLayout());
        setupEquip();

        mainPanel.add(tabAll, GBC.eol().fill(GridBagConstraints.BOTH).insets(5, 5, 5, 5));
        mainPanel.add(panButtons, GBC.eol().anchor(GridBagConstraints.CENTER));

        JScrollPane scrEquip = new JScrollPane(panEquip);
        // Don't show the crew panel if there's multiple entities or no crew to show
        if (!multipleEntities && panCrewMember.length > 0) {
            if (panCrewMember.length > 1) {
                for (int i = 0; i < panCrewMember.length; i++) {
                    JScrollPane memberScrollPane = new JScrollPane(panCrewMember[i]);
                    memberScrollPane.getVerticalScrollBar().setUnitIncrement(16);
                    tabAll.addTab(entity.getCrew().getCrewType().getRoleName(i), memberScrollPane);
                }
                JScrollPane crewScrollPane = new JScrollPane(panCrew);
                crewScrollPane.getVerticalScrollBar().setUnitIncrement(16);
                tabAll.addTab(Messages.getString("CustomMekDialog.tabCrew"), crewScrollPane);
            } else {
                panCrew.add(panCrewMember[0], GBC.eop());
                JScrollPane memberScrollPane = new JScrollPane(panCrew);
                memberScrollPane.getVerticalScrollBar().setUnitIncrement(16);
                tabAll.addTab(Messages.getString("CustomMekDialog.tabPilot"), memberScrollPane);
            }
            tabAll.addTab(Messages.getString("CustomMekDialog.tabEquipment"), scrEquip);
        }

        if (this.clientGUI != null) {
            tabAll.addTab(Messages.getString(editableDeployment ?
                  "CustomMekDialog.tabDeployment" :
                  "CustomMekDialog.tabState"), new JScrollPane(panDeploy));
            if (quirksEnabled && !multipleEntities) {
                JScrollPane scrQuirks = new JScrollPane(panQuirks);
                scrQuirks.getVerticalScrollBar().setUnitIncrement(16);
                scrQuirks.setPreferredSize(scrEquip.getPreferredSize());
                tabAll.addTab("Quirks", scrQuirks);
            }
            if (partialRepairsEnabled && !multipleEntities) {
                tabAll.addTab(Messages.getString("CustomMekDialog.tabPartialRepairs"), new JScrollPane(panPartReps));
            }
        }

        options = entity.getCrew().getOptions();
        partReps = entity.getPartialRepairs();
        for (Mounted<?> m : entity.getWeaponList()) {
            h_wpnQuirks.put(entity.getEquipmentNum(m), m.getQuirks());
        }
        // Also need to consider melee weapons
        for (Mounted<?> m : entity.getMisc()) {
            if (m.getType().hasFlag(MiscType.F_CLUB)) {
                h_wpnQuirks.put(entity.getEquipmentNum(m), m.getQuirks());
            }
        }

        // **CREW TAB**//
        if (gameOptions().booleanOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)) {
            panCrew.add(new JLabel(Messages.getString("CustomMekDialog.labInit"), SwingConstants.RIGHT), GBC.std());
            panCrew.add(fldInit, GBC.eop());
        }
        fldInit.setText(Integer.toString(entity.getCrew().getInitBonus()));

        if (gameOptions().booleanOption(OptionsConstants.RPG_COMMAND_INIT)) {
            panCrew.add(new JLabel(Messages.getString("CustomMekDialog.labCommandInit"), SwingConstants.RIGHT),
                  GBC.std());
            panCrew.add(fldCommandInit, GBC.eop());
        }
        fldCommandInit.setText(Integer.toString(entity.getCrew().getCommandBonus()));

        // Set up commanders for commander killed victory condition & SPA
        panCrew.add(new JLabel(Messages.getString("CustomMekDialog.labCommander"), SwingConstants.RIGHT), GBC.std());
        panCrew.add(chCommander, GBC.eol());
        chCommander.setSelected(entity.isCommander());
        panOptions = new JPanel(new GridBagLayout());
        panCrew.add(panOptions, GBC.eop());

        // **DEPLOYMENT TAB**//

        if (isQuadVee || isLAM) {
            panDeploy.add(labStartingMode, GBC.std());
            panDeploy.add(choStartingMode, GBC.eol());
            choStartingMode.addItemListener(this);
            labStartingMode.setToolTipText(Messages.getString("CustomMekDialog.startingModeToolTip"));
            choStartingMode.setToolTipText(Messages.getString("CustomMekDialog.startingModeToolTip"));
            refreshDeployment();
            // Disable conversions for loaded units so we don't get fighter LAMs in mek bays
            // and vice versa
            choStartingMode.setEnabled(entities.get(0).getTransportId() == Entity.NONE);
        }
        if (isVTOL || isLAM || isGlider) {
            panDeploy.add(labStartHeight, GBC.std());
            panDeploy.add(fldStartHeight, GBC.eol());
        }
        if (isWiGE) {
            panDeploy.add(new JLabel(Messages.getString("CustomMekDialog.labDeployAirborne"), SwingConstants.RIGHT),
                  GBC.std());
            panDeploy.add(chDeployAirborne, GBC.eol());
        }
        if (isAero || isLAM || isShip) {
            panDeploy.add(labStartVelocity, GBC.std());
            panDeploy.add(fldStartVelocity, GBC.eol());

            if (!space) {
                panDeploy.add(labStartAltitude, GBC.std());
                panDeploy.add(fldStartAltitude, GBC.eol());
            }

            panDeploy.add(labCurrentFuel, GBC.std());
            panDeploy.add(fldCurrentFuel, GBC.eol());
        }

        choDeploymentRound.addItemListener(this);

        panDeploy.add(labDeploymentRound, GBC.std());
        panDeploy.add(choDeploymentRound, GBC.eol());
        panDeploy.add(labDeploymentZone, GBC.std());
        panDeploy.add(choDeploymentZone, GBC.eol());
        panDeploy.add(labDeploymentOffset, GBC.std());
        panDeploy.add(txtDeploymentOffset, GBC.eol());
        panDeploy.add(labDeploymentWidth, GBC.std());
        panDeploy.add(txtDeploymentWidth, GBC.eol());

        panDeploy.add(new JLabel(Messages.getString("CustomMekDialog.labDeploymentAnyNW")), GBC.std());
        panDeploy.add(spinStartingAnyNWx, GBC.std());
        panDeploy.add(spinStartingAnyNWy, GBC.eol());
        panDeploy.add(new JLabel(Messages.getString("CustomMekDialog.labDeploymentAnySE")), GBC.std());
        panDeploy.add(spinStartingAnySEx, GBC.std());
        panDeploy.add(spinStartingAnySEy, GBC.eol());

        numFormatter.setMinimum(0);
        numFormatter.setCommitsOnValidEdit(true);

        labDeploymentOffset.setToolTipText(Messages.getString("CustomMekDialog.labDeploymentOffsetTip"));
        labDeploymentWidth.setToolTipText(Messages.getString("CustomMekDialog.labDeploymentWidthTip"));
        txtDeploymentOffset.setColumns(4);
        txtDeploymentWidth.setColumns(4);

        if (gameOptions().booleanOption(OptionsConstants.RPG_BEGIN_SHUTDOWN) &&
              !(entity instanceof Infantry) &&
              !(entity.isBuildingEntityOrGunEmplacement())) {
            panDeploy.add(labDeployShutdown, GBC.std());
            panDeploy.add(chDeployShutdown, GBC.eol());
            chDeployShutdown.setSelected(entity.isManualShutdown());
        }

        if (isMek) {
            panDeploy.add(labDeployHullDown, GBC.std());
            panDeploy.add(chDeployHullDown, GBC.eol());
            chDeployHullDown.setSelected(entity.isHullDown() && !entity.isProne());
            chDeployHullDown.addItemListener(this);

            panDeploy.add(labDeployProne, GBC.std());
            panDeploy.add(chDeployProne, GBC.eol());
            chDeployProne.setSelected(entity.isProne() && !entity.isHullDown());
            chDeployProne.addItemListener(this);
        }

        refreshDeployment();

        if (gameOptions().booleanOption(OptionsConstants.ADVANCED_HIDDEN_UNITS)) {
            panDeploy.add(labHidden, GBC.std());
            panDeploy.add(chHidden, GBC.eol());
            chHidden.setSelected(entity.isHidden());
        }

        if (hasStealth) {
            panDeploy.add(labDeployStealth, GBC.std());
            panDeploy.add(chDeployStealth, GBC.std());
            chDeployStealth.setSelected(entity.isStealthOn());
        }

        if (eligibleForOffBoard) {
            panDeploy.add(labOffBoard, GBC.std());
            panDeploy.add(chOffBoard, GBC.eol());
            chOffBoard.setSelected(entity.isOffBoard());

            panDeploy.add(labOffBoardDirection, GBC.std());

            choOffBoardDirection.addItem(Messages.getString("CustomMekDialog.North"));
            choOffBoardDirection.addItem(Messages.getString("CustomMekDialog.South"));
            choOffBoardDirection.addItem(Messages.getString("CustomMekDialog.East"));
            choOffBoardDirection.addItem(Messages.getString("CustomMekDialog.West"));
            OffBoardDirection direction = entity.getOffBoardDirection();
            if (OffBoardDirection.NONE == direction) {
                direction = OffBoardDirection.NORTH;
            }
            choOffBoardDirection.setSelectedIndex(direction.getValue());
            panDeploy.add(choOffBoardDirection, GBC.eol());

            panDeploy.add(labOffBoardDistance, GBC.std());

            butOffBoardDistance.addActionListener(this);
            butOffBoardDistance.setText(Integer.toString(distance));
            panDeploy.add(butOffBoardDistance, GBC.eol());
        }

        if (isAero || isLAM || isShip) {
            IAero a = (IAero) entity;

            fldStartVelocity.setText(Integer.valueOf(a.getCurrentVelocity()).toString());
            fldStartVelocity.addActionListener(this);

            fldStartAltitude.setText(Integer.valueOf(entity.getAltitude()).toString());
            fldStartAltitude.addActionListener(this);

            fuel = a.getFuel();
            fldCurrentFuel.setText(Integer.valueOf(a.getCurrentFuel()).toString());
            fldCurrentFuel.addActionListener(this);
        }

        if (isVTOL || isLAM || isGlider) {
            fldStartHeight.setText(Integer.valueOf(entity.getElevation()).toString());
            fldStartHeight.addActionListener(this);
        }

        if (isWiGE) {
            chDeployAirborne.setSelected(entity.getElevation() > 0);
        }

        if (!editable) {
            fldInit.setEnabled(false);
            fldCommandInit.setEnabled(false);
            chCommander.setEnabled(false);
            choDeploymentRound.setEnabled(false);
            chDeployShutdown.setEnabled(false);
            chDeployProne.setEnabled(false);
            chDeployHullDown.setEnabled(false);
            chCommander.setEnabled(false);
            chHidden.setEnabled(false);
            chDeployStealth.setEnabled(false);
            chOffBoard.setEnabled(false);
            choOffBoardDirection.setEnabled(false);
            fldOffBoardDistance.setEnabled(false);
            fldStartVelocity.setEnabled(false);
            fldStartAltitude.setEnabled(false);
            fldCurrentFuel.setEnabled(false);
            fldStartHeight.setEnabled(false);
            chDeployAirborne.setEnabled(false);
            m_equip.initialize();
        }

        setResizable(true);
        return mainPanel;
    }

    @Override
    protected JPanel createButtonPanel() {
        butOkay.addActionListener(this);
        butCancel.addActionListener(this::cancelActionPerformed);
        butNext.addActionListener(this);
        butPrev.addActionListener(this);
        butNext.setEnabled(getNextEntity(true) != null);
        butPrev.setEnabled(getNextEntity(false) != null);

        UIUtil.WrappingButtonPanel panButtons = new UIUtil.WrappingButtonPanel();
        panButtons.add(butPrev);
        panButtons.add(butOkay);
        panButtons.add(butCancel);
        panButtons.add(butNext);
        return panButtons;
    }

    private GameOptions gameOptions() {
        if (clientGUI == null) {
            return client.getGame().getOptions();
        }
        return clientGUI.getClient().getGame().getOptions();
    }
}
