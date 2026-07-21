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
package megamek.client.ui.dialogs.unitEditor;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.client.ui.Messages;
import megamek.client.ui.util.UIUtil;
import megamek.common.CriticalSlot;
import megamek.common.bays.Bay;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentMode;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.DockingCollar;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.options.OptionsConstants;
import megamek.common.units.*;
import megamek.common.weapons.attacks.InfantryAttack;

/**
 * Builds the controls of the unit damage editor: a panel per unit location holding that location's armor and
 * structure, the systems that sit in it, and the equipment mounted in it. Systems that belong to no single
 * location, such as a tank's engine or an aero's avionics, go in a general panel.
 * <p>
 * Every unit type carries its systems differently, which is why there is a builder per type.
 * </p>
 */
public class UnitDamagePanelBuilder {

    /** The most heat that can be set, matching the range the lobby's heat menu offers. */
    public static final int MAX_HEAT = 40;

    /**
     * The most hits a crew member can be given. Six hits kill (TW p.41), and a unit whose whole crew is dead is
     * destroyed by the server when the edit reaches it (crew death, {@code destroyEntityIfFatallyDamaged}).
     */
    public static final int MAX_CREW_HITS = Crew.DEATH;

    /** The most rows a panel column may hold before the next rows wrap into a new column beside it. */
    private static final int MAX_ROWS_PER_COLUMN = 20;

    /** The rounds a fresh skill modifier is offered with, before the gamemaster sets a duration of their own. */
    public static final int DEFAULT_MODIFIER_ROUNDS = 3;
    /**
     * The farthest the initiative modifier can move a roll either way, matching the /skillMod command. The skill
     * modifiers do not use it: their spinners are bounded by the crew's skill instead.
     */
    private static final int MAX_SKILL_DELTA = 8;
    /** The longest a timed skill modifier can last, matching the /skillMod command. */
    private static final int MAX_MODIFIER_ROUNDS = 100;

    private final Entity entity;
    private final UnitDamageControls controls;
    /** Whether the gamemaster-only controls are offered: refilling ammo bins and temporary skill modifiers. */
    private final boolean offerGameMasterTools;

    public UnitDamagePanelBuilder(Entity entity, UnitDamageControls controls) {
        this(entity, controls, false);
    }

    /**
     * @param entity   the unit to build the controls for
     * @param controls the controls to fill in
     * @param offerGameMasterTools whether the gamemaster-only controls are offered: setting an ammo bin's shots
     *                 and temporary skill modifiers. These belong to a gamemaster in MegaMek only: MekHQ keeps its
     *                 own count of what is in a bin, as campaign stock, and has no game to run modifiers in.
     */
    public UnitDamagePanelBuilder(Entity entity, UnitDamageControls controls, boolean offerGameMasterTools) {
        this.entity = entity;
        this.controls = controls;
        this.offerGameMasterTools = offerGameMasterTools;
    }

    /** Builds every control of the editor for the unit, filling in the controls this builder was given. */
    public void build() {
        initLocationPanels();
        initSystemCrits();
        initEquipCrits();
    }

    private void initLocationPanels() {
        controls.locationPanels = new JPanel[entity.locations()];
        controls.spnArmor = new JSpinner[entity.locations()];
        controls.spnInternal = new JSpinner[entity.locations()];
        controls.spnRear = new JSpinner[entity.locations()];
        controls.locationLabels = new JLabel[entity.locations()];

        boolean isAero = entity instanceof Aero;
        for (int location = 0; location < entity.locations(); location++) {
            int originalArmor = entity.getOArmor(location);
            // Aero keeps its structure as one structural-integrity value, edited on the general panel rather than
            // per location. Other units edit per-location internal where they have it.
            boolean editsInternal = !isAero && (entity.getOInternal(location) > 0);
            boolean editsArmor = originalArmor > 0;
            // Skip a location with nothing to edit: a hidden location, or one with neither armor nor structure (a
            // handheld weapon's armor-only gun, a gun emplacement's guns).
            if (!editsArmor && !editsInternal) {
                continue;
            }
            controls.locationLabels[location] = new JLabel(entity.getLocationName(location));
            controls.locationPanels[location] = createTitledPanel(controls.locationLabels[location]);

            if (editsInternal) {
                int originalInternal = entity.getOInternal(location);
                int internal = Math.min(Math.max(entity.getInternal(location), 0), originalInternal);
                controls.spnInternal[location] = new JSpinner(new SpinnerNumberModel(internal, 0, originalInternal, 1));
                addLabeledRow(controls.locationPanels[location],
                      Messages.getString("UnitEditorDialog.internal"),
                      controls.spnInternal[location]);
            }

            if (editsArmor) {
                int armor = Math.min(Math.max(entity.getArmor(location, false), 0), originalArmor);
                controls.spnArmor[location] = new JSpinner(new SpinnerNumberModel(armor, 0, originalArmor, 1));
                boolean hasRear = entity.hasRearArmor(location);
                addLabeledRow(controls.locationPanels[location],
                      Messages.getString(hasRear ? "UnitEditorDialog.armorFront" : "UnitEditorDialog.armor"),
                      controls.spnArmor[location]);
                if (hasRear) {
                    int originalRear = Math.max(entity.getOArmor(location, true), 0);
                    int rear = Math.min(Math.max(entity.getArmor(location, true), 0), originalRear);
                    controls.spnRear[location] = new JSpinner(new SpinnerNumberModel(rear, 0, originalRear, 1));
                    addLabeledRow(controls.locationPanels[location],
                          Messages.getString("UnitEditorDialog.armorRear"),
                          controls.spnRear[location]);
                }
            }
        }

        if (isAero) {
            Aero aero = (Aero) entity;
            int structuralIntegrity = Math.max(aero.getSI(), 0);
            controls.spnInternal[0] = new JSpinner(new SpinnerNumberModel(structuralIntegrity, 0, aero.getOSI(), 1));
            controls.structuralIntegrityLabel = new JLabel("<html><b>" +
                  Messages.getString("UnitEditorDialog.structuralIntegrity") +
                  "</b></html>");
            // A capital ship's SI sits in the paperdoll's centre, over the hull, which has no armor and so no panel
            // of its own. Give the hull a panel there and put the SI in it, so the hull's own equipment lands in it
            // rather than the general panel. Other aero have no hull location, so their SI stays in general.
            JPanel structuralIntegrityPanel = generalPanel();
            if (entity instanceof Jumpship) {
                controls.locationLabels[Jumpship.LOC_HULL] = new JLabel(entity.getLocationName(Jumpship.LOC_HULL));
                controls.locationPanels[Jumpship.LOC_HULL] = createTitledPanel(controls.locationLabels[Jumpship.LOC_HULL]);
                structuralIntegrityPanel = controls.locationPanels[Jumpship.LOC_HULL];
            }
            addRow(structuralIntegrityPanel, controls.structuralIntegrityLabel, controls.spnInternal[0]);
        }

        initCrewHits();
        initHeat();
        initStatus();
    }

    /**
     * Whether the gamemaster's skill modifier controls are offered: only in the gamemaster's editor, only in a
     * running game - in the lobby the crew's real skills are edited directly - and only for a unit with a crew.
     */
    private boolean offersSkillModifiers() {
        Crew crew = entity.getCrew();
        boolean inPlay = (entity.getGame() != null) && !entity.getGame().getPhase().isLounge();
        return offerGameMasterTools && inPlay && (crew != null) && (crew.getSlotCount() >= 1);
    }

    /**
     * Adds the gamemaster's skill modifier controls to the general panel, as a column of their own so they stand
     * apart from the unit's systems and are always in view, unlike a location panel that only shows when its
     * location is chosen. Called by the dialog after every other general row is in place, so the column is the
     * panel's last.
     */
    public void addSkillModifiersColumn() {
        if (!offersSkillModifiers()) {
            return;
        }
        startNewColumn(generalPanel());
        initSkillModifiers(generalPanel());
    }

    /** Pads the given panel's row count so the next row added to it starts at the top of a fresh column. */
    private void startNewColumn(JPanel panel) {
        int itemCount = controls.panelRows.getOrDefault(panel, 1) - 1;
        int padding = (MAX_ROWS_PER_COLUMN - (itemCount % MAX_ROWS_PER_COLUMN)) % MAX_ROWS_PER_COLUMN;
        controls.panelRows.merge(panel, padding, Integer::sum);
    }

    /**
     * Adds the gamemaster's temporary skill modifier controls: a delta to gunnery, piloting and the unit's
     * individual initiative roll, and how long they last. They are applied through {@link TemporarySkillModifiers},
     * so the crew's stored skills are never touched and the change reverses itself when it expires.
     */
    private void initSkillModifiers(JPanel panel) {
        Crew crew = entity.getCrew();
        TemporarySkillModifiers modifiers = crew.getSkillModifiers();

        // The spinners run from what improves the skill to 0 to what worsens it to 8, so every value they offer
        // applies in full and no part of a modifier is ever lost to the skill range. They start at the applied
        // modifier for the same reason: an oversized delta set through /skillMod reads back as what counts.
        int rawGunnery = crew.getGunnery() - crew.appliedGunneryModifier();
        controls.spnGunneryModifier = skillDeltaSpinner(crew.appliedGunneryModifier(), rawGunnery,
              "UnitEditorDialog.skillModifier.gunnery.tooltip");
        controls.spnGunneryRounds = modifierRoundsSpinner(modifiers.getGunneryRounds());
        controls.chkGunneryPermanent = modifierPermanentCheckbox(modifiers.getGunneryRounds(),
              controls.spnGunneryRounds);
        addLabeledRow(panel, Messages.getString("UnitEditorDialog.skillModifier.gunnery"),
              modifierRow(controls.spnGunneryModifier, controls.spnGunneryRounds, controls.chkGunneryPermanent));

        int rawPiloting = crew.getPiloting() - crew.appliedPilotingModifier();
        controls.spnPilotingModifier = skillDeltaSpinner(crew.appliedPilotingModifier(), rawPiloting,
              "UnitEditorDialog.skillModifier.piloting.tooltip");
        controls.spnPilotingRounds = modifierRoundsSpinner(modifiers.getPilotingRounds());
        controls.chkPilotingPermanent = modifierPermanentCheckbox(modifiers.getPilotingRounds(),
              controls.spnPilotingRounds);
        addLabeledRow(panel, Messages.getString("UnitEditorDialog.skillModifier.piloting"),
              modifierRow(controls.spnPilotingModifier, controls.spnPilotingRounds, controls.chkPilotingPermanent));

        // the initiative delta only exists where each unit rolls its own initiative, so the row is only offered
        // where it would do something; everywhere else it would be a control that silently does nothing
        if (entity.getGame().getOptions().booleanOption(OptionsConstants.RPG_INDIVIDUAL_INITIATIVE)) {
            controls.spnInitiativeModifier = initiativeDeltaSpinner(modifiers.getInitiativeDelta());
            controls.spnInitiativeRounds = modifierRoundsSpinner(modifiers.getInitiativeRounds());
            controls.chkInitiativePermanent = modifierPermanentCheckbox(modifiers.getInitiativeRounds(),
                  controls.spnInitiativeRounds);
            addLabeledRow(panel, Messages.getString("UnitEditorDialog.skillModifier.initiative"),
                  modifierRow(controls.spnInitiativeModifier, controls.spnInitiativeRounds,
                        controls.chkInitiativePermanent));
        }
    }

    /**
     * A spinner for one skill delta, running from what improves the given stored skill to 0 up to what worsens it
     * to {@link Crew#MAX_SKILL}, so every offered value applies in full. Starts at the modifier the crew already
     * carries, so an open edit reads back.
     */
    private JSpinner skillDeltaSpinner(int appliedDelta, int rawSkill, String tooltipKey) {
        // an applied modifier always fits the skill range by construction, but the model throws on a starting
        // value outside its bounds, so the value is held to them rather than trusting every caller forever
        int startingDelta = Math.clamp(appliedDelta, -rawSkill, Crew.MAX_SKILL - rawSkill);
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(startingDelta, -rawSkill,
              Crew.MAX_SKILL - rawSkill, 1));
        spinner.setToolTipText(UIUtil.formatSideTooltip(Messages.getString(tooltipKey)));
        return spinner;
    }

    /** A spinner for the initiative delta, which is added to a roll rather than a skill and has no skill range. */
    private JSpinner initiativeDeltaSpinner(int delta) {
        int startingDelta = Math.clamp(delta, -MAX_SKILL_DELTA, MAX_SKILL_DELTA);
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(startingDelta, -MAX_SKILL_DELTA, MAX_SKILL_DELTA, 1));
        spinner.setToolTipText(UIUtil.formatSideTooltip(
              Messages.getString("UnitEditorDialog.skillModifier.initiative.tooltip")));
        return spinner;
    }

    /** A spinner for one modifier's duration, prefilled with what remains of an active countdown. */
    private JSpinner modifierRoundsSpinner(int roundsRemaining) {
        int rounds = Math.clamp((roundsRemaining > 0) ? roundsRemaining : DEFAULT_MODIFIER_ROUNDS,
              1, MAX_MODIFIER_ROUNDS);
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(rounds, 1, MAX_MODIFIER_ROUNDS, 1));
        spinner.setToolTipText(UIUtil.formatSideTooltip(
              Messages.getString("UnitEditorDialog.skillModifier.rounds.tooltip")));
        spinner.setEnabled(roundsRemaining != TemporarySkillModifiers.PERMANENT);
        return spinner;
    }

    /** A checkbox that makes one modifier permanent, disabling its rounds spinner while it is on. */
    private JCheckBox modifierPermanentCheckbox(int roundsRemaining, JSpinner roundsSpinner) {
        JCheckBox checkbox = new JCheckBox(Messages.getString("UnitEditorDialog.skillModifier.permanent"),
              roundsRemaining == TemporarySkillModifiers.PERMANENT);
        checkbox.setToolTipText(UIUtil.formatSideTooltip(
              Messages.getString("UnitEditorDialog.skillModifier.permanent.tooltip")));
        checkbox.addItemListener(event -> roundsSpinner.setEnabled(!checkbox.isSelected()));
        return checkbox;
    }

    /** Lays out one modifier's row: the delta, then its own duration - "for N rounds" or Permanent. */
    private JPanel modifierRow(JSpinner deltaSpinner, JSpinner roundsSpinner, JCheckBox permanentCheckbox) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row.add(deltaSpinner);
        row.add(new JLabel(Messages.getString("UnitEditorDialog.skillModifier.forLabel")));
        row.add(roundsSpinner);
        row.add(new JLabel(Messages.getString("UnitEditorDialog.skillModifier.roundsLabel")));
        row.add(permanentCheckbox);
        return row;
    }

    /**
     * Adds the unit's conditions to the general panel: whether it is shut down, prone, hidden and so on, and how
     * much fuel it has left. These say what state the unit is in, which is what this dialog is for, rather than how
     * it was built or where it deploys, which is what Configure is for.
     */
    private void initStatus() {
        controls.chkShutdown = addStatusRow("UnitEditorDialog.status.shutdown", entity.isShutDown());

        // a LAM in fighter mode, or an airborne unit, is neither prone nor hull down
        boolean canLieDown = !entity.isAero() && !entity.isAirborne() && !entity.isAirborneVTOLorWIGE();
        if (canLieDown && entity.isMek()) {
            controls.chkProne = addStatusRow("UnitEditorDialog.status.prone", entity.isProne());
        }
        if (canLieDown && (entity.isMek() || entity.isVehicle())) {
            controls.chkHullDown = addStatusRow("UnitEditorDialog.status.hullDown", entity.isHullDown());
        }

        if (entity.canHide()) {
            controls.chkHidden = addStatusRow("UnitEditorDialog.status.hidden", entity.isHidden());
        }

        if (entity.hasStealth()) {
            controls.chkStealth = addStatusRow("UnitEditorDialog.status.stealth", isStealthOn());
        }

        // mechanized infantry cannot dig in (TO:AR p.106)
        if ((entity instanceof Infantry infantry) && !infantry.isMechanized()) {
            controls.chkDugIn = addStatusRow("UnitEditorDialog.status.dugIn",
                  infantry.getDugIn() != Infantry.DUG_IN_NONE);
        }

        if (entity instanceof Aero aero) {
            controls.spnFuel = new JSpinner(new SpinnerNumberModel(Math.max(aero.getCurrentFuel(), 0),
                  0,
                  Math.max(aero.getFuel(), aero.getCurrentFuel()),
                  1));
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.status.fuel"), controls.spnFuel);
        }
    }

    private JCheckBox addStatusRow(String labelKey, boolean selected) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(selected);
        // A status checkbox only shows the current state, so its tooltip names the state and the action the next
        // click performs - "The unit is prone. Click to stand it up." - and follows the checkbox as it is toggled.
        String onTooltip = Messages.getString(labelKey + ".tooltip.on");
        String offTooltip = Messages.getString(labelKey + ".tooltip.off");
        checkBox.setToolTipText(selected ? onTooltip : offTooltip);
        checkBox.addItemListener(event -> checkBox.setToolTipText(checkBox.isSelected() ? onTooltip : offTooltip));
        addLabeledRow(generalPanel(), Messages.getString(labelKey), checkBox);
        return checkBox;
    }

    /** Whether the unit's stealth armor is switched on, which it is when any of its stealth equipment is. */
    private boolean isStealthOn() {
        for (MiscMounted stealth : entity.getMiscEquipment(MiscType.F_STEALTH)) {
            if (EquipmentMode.getMode("On").equals(stealth.curMode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a hits control per crew member to the panel of the location the crew sits in, which is the head of a
     * Mek. Hits stop one short of the six that kill a crew member (TW p.41): the editor wounds and revives crew,
     * and destroying a unit outright is what the Destroy Unit button is for. Revive is the reason the control
     * matters as much as wounding, since a crew member killed in play can be brought back below six hits.
     */
    private void initCrewHits() {
        Crew crew = entity.getCrew();
        if ((crew == null) || (crew.getSlotCount() < 1)) {
            return;
        }
        controls.spnCrewHits = new JSpinner[crew.getSlotCount()];
        for (int slot = 0; slot < crew.getSlotCount(); slot++) {
            if (crew.isMissing(slot)) {
                continue;
            }
            controls.spnCrewHits[slot] = new JSpinner(new SpinnerNumberModel(Math.min(crew.getHits(slot), MAX_CREW_HITS),
                  0,
                  MAX_CREW_HITS,
                  1));
            String label = (crew.getSlotCount() > 1)
                  ? String.format(Messages.getString("UnitEditorDialog.crewHitsFor"), crew.getNameAndRole(slot))
                  : Messages.getString("UnitEditorDialog.crewHits");
            addLabeledRow(targetPanel(crewLocation()), label, controls.spnCrewHits[slot]);
        }
    }

    /** Adds a heat control to the panel of the location that carries the engine, which is a Mek's center torso. */
    private void initHeat() {
        if (!entity.tracksHeat()) {
            return;
        }
        controls.spnHeat = new JSpinner(new SpinnerNumberModel(Math.max(entity.heat, 0), 0, MAX_HEAT, 1));
        addLabeledRow(targetPanel(heatLocation()), Messages.getString("UnitEditorDialog.heat"), controls.spnHeat);
    }

    /** The location the crew sits in, or {@code LOC_NONE} to put the crew in the general panel. */
    private int crewLocation() {
        return (entity instanceof Mek) ? Mek.LOC_HEAD : Entity.LOC_NONE;
    }

    /** The location the engine sits in, or {@code LOC_NONE} to put heat in the general panel. */
    private int heatLocation() {
        return (entity instanceof Mek) ? Mek.LOC_CENTER_TORSO : Entity.LOC_NONE;
    }

    public JPanel initInfantryPanel() {
        Infantry infantry = (Infantry) entity;

        controls.spnArmor = new JSpinner[entity.locations()];
        controls.spnInternal = new JSpinner[entity.locations()];
        controls.spnRear = new JSpinner[entity.locations()];

        int men = Math.max(infantry.getShootingStrength(), 0);
        controls.spnInternal[0] = new JSpinner(new SpinnerNumberModel(men,
              0,
              infantry.getSquadCount() * infantry.getSquadSize(),
              1));
        JPanel panel = createTitledPanel(new JLabel(Messages.getString("UnitEditorDialog.troopersLeft")));
        addLabeledRow(panel, Messages.getString("UnitEditorDialog.menLeft"), controls.spnInternal[0]);
        if (offersSkillModifiers()) {
            initSkillModifiers(panel);
        }
        return panel;
    }

    /** Creates an empty location-style panel with the given label as its bold title row. */
    private JPanel createTitledPanel(JLabel titleLabel) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = new Insets(2, 5, 2, 5);
        gridBagConstraints.anchor = GridBagConstraints.CENTER;
        panel.add(titleLabel, gridBagConstraints);
        controls.panelRows.put(panel, 1);
        return panel;
    }

    /** Returns the panel for systems that have no single location, creating it on first use. */
    public JPanel generalPanel() {
        if (controls.panGeneral == null) {
            controls.panGeneral = createTitledPanel(new JLabel(Messages.getString("UnitEditorDialog.general")));
        }
        return controls.panGeneral;
    }

    /** Returns the panel for the given location, or the general panel when that location has none. */
    public JPanel targetPanel(int location) {
        if ((location >= 0) && (location < controls.locationPanels.length) && (controls.locationPanels[location] != null)) {
            return controls.locationPanels[location];
        }
        return generalPanel();
    }

    /**
     * Returns the panel a location's equipment belongs in, creating one for the location when it has none of its
     * own yet. A location can carry weapons without carrying armor - a warship's hull, or an arc with no armor - so
     * its equipment would otherwise fall back to the general panel, mixing the unit's weapons in with its systems.
     */
    private JPanel equipmentPanel(int location) {
        if ((location < 0) || (location >= controls.locationPanels.length)) {
            return generalPanel();
        }
        if (controls.locationPanels[location] == null) {
            controls.locationLabels[location] = new JLabel(entity.getLocationName(location));
            controls.locationPanels[location] = createTitledPanel(controls.locationLabels[location]);
        }
        return controls.locationPanels[location];
    }

    /** Adds a crit control to a location's panel, and remembers which location it belongs to. */
    private void addCritRow(int location, String labelText, CheckCritPanel crit) {
        controls.addCritOfLocation(location, crit);
        addLabeledRow(targetPanel(location), labelText, crit);
    }

    /** Appends a bold label and a control as the next row of the given panel. */
    public void addLabeledRow(JPanel panel, String labelText, JComponent control) {
        addRow(panel, new JLabel("<html><b>" + labelText + "</b></html>"), control);
    }

    public void addRow(JPanel panel, JLabel label, JComponent control) {
        // Fill the panel column by column, so a unit with many rows - a warship, say - wraps into further columns
        // rather than a single column that runs off the screen. Each column is filled top to bottom in order before
        // the next begins, so the items still read a, b, c... down one column and then the next.
        int itemIndex = controls.panelRows.merge(panel, 1, Integer::sum) - 2;
        int column = itemIndex / MAX_ROWS_PER_COLUMN;
        int rowInColumn = itemIndex % MAX_ROWS_PER_COLUMN;
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        // a wider left inset on later columns sets them apart from the column before
        gridBagConstraints.gridx = column * 2;
        gridBagConstraints.gridy = rowInColumn + 1;
        gridBagConstraints.insets = new Insets(1, (column > 0) ? 15 : 5, 1, 5);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.0;
        panel.add(label, gridBagConstraints);
        gridBagConstraints.gridx = column * 2 + 1;
        gridBagConstraints.weightx = 1.0;
        panel.add(control, gridBagConstraints);
    }

    /**
     * Lays out the armor diagram on the left and the location panels on the right, where one location shows at a
     * time. The diagram is the same one the unit display uses, so every unit type that has an armor readout gets
     * one. Double-clicking a location on the diagram brings up that location's panel; the chooser above the panel
     * does the same for units whose diagram has no clickable locations, and reaches the general panel.
     */

    private void initEquipCrits() {
        // On a capital ship the weapon bay is the crit unit: its member weapons are destroyed with the bay, so
        // listing each of them alongside the bay only repeats it, once per bay across every arc. Show the bay (and
        // its ammo, which still tracks shots) but skip its member weapons.
        Set<Integer> bayMemberWeapons = new HashSet<>();
        for (WeaponMounted bay : entity.getWeaponBayList()) {
            for (WeaponMounted member : bay.getBayWeapons()) {
                bayMemberWeapons.add(entity.getEquipmentNum(member));
            }
        }

        for (Mounted<?> mounted : entity.getEquipment()) {
            if ((mounted.getLocation() == Entity.LOC_NONE) ||
                  !mounted.getType().isHittable() ||
                  mounted.isWeaponGroup()) {
                continue;
            }
            if (mounted.getType() instanceof InfantryAttack) {
                continue;
            }
            if (bayMemberWeapons.contains(entity.getEquipmentNum(mounted))) {
                continue;
            }
            int nCrits = mounted.getNumCriticalSlots();
            int equipmentNumber = entity.getEquipmentNum(mounted);
            int hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, equipmentNumber, mounted.getLocation());
            if (mounted.isSplit()) {
                hits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, equipmentNumber, mounted.getSecondLocation());
            }
            if ((mounted.getType() instanceof MiscType) && (mounted.getType().hasFlag(MiscType.F_PARTIAL_WING))) {
                hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, equipmentNumber, Mek.LOC_LEFT_TORSO);
                hits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, equipmentNumber, Mek.LOC_RIGHT_TORSO);
            }

            if (!(entity instanceof Mek)) {
                nCrits = 1;
                if (hits > 1) {
                    hits = 1;
                }
            }
            CheckCritPanel crit = new CheckCritPanel(nCrits, hits);
            controls.equipCrits.put(equipmentNumber, crit);
            String label = mounted.getName();
            if (mounted.isSplit()) {
                label += " (" + entity.getLocationAbbr(mounted.getLocation()) + "/"
                      + entity.getLocationAbbr(mounted.getSecondLocation()) + ")";
            }

            JComponent control = crit;
            if (offerGameMasterTools && (mounted instanceof AmmoMounted ammoBin)) {
                control = ammoControl(equipmentNumber, ammoBin, crit);
            }
            if (offersEquipmentSettings() && (mounted.getType() instanceof WeaponType weaponType)
                  && weaponType.hasFlag(WeaponType.F_MG)
                  && entity.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_BURST)) {
                control = withMgBurstCheckbox(equipmentNumber, mounted, control);
            }
            controls.addCritOfLocation(mounted.getLocation(), crit);
            addLabeledRow(equipmentPanel(mounted.getLocation()), label, control);
        }
    }

    /**
     * Whether the in-game equipment settings (burst MG fire, hot-loading) are offered: only in the gamemaster's
     * editor and only in a running game. In the lobby the Configure dialog owns these settings.
     */
    private boolean offersEquipmentSettings() {
        return offerGameMasterTools && (entity.getGame() != null) && !entity.getGame().getPhase().isLounge();
    }

    /** Appends a burst fire checkbox to a machine gun's row, prefilled with the gun's current setting. */
    private JComponent withMgBurstCheckbox(int equipmentNumber, Mounted<?> machineGun, JComponent control) {
        JCheckBox burstCheckbox = new JCheckBox(Messages.getString("UnitEditorDialog.mgBurst"),
              machineGun.isRapidFire());
        burstCheckbox.setToolTipText(UIUtil.formatSideTooltip(
              Messages.getString("UnitEditorDialog.mgBurst.tooltip")));
        controls.mgBurst.put(equipmentNumber, burstCheckbox);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row.add(control);
        row.add(burstCheckbox);
        return row;
    }

    /**
     * Builds the controls of an ammo bin: its crits, and the shots left in it. The two are tied together, because a
     * destroyed bin holds no ammo: critting the bin empties it and locks the count, and taking the crit off again
     * restores the bin and the shots that were in it, so a gamemaster can undo a mistake rather than having to
     * reopen the dialog.
     */
    private JComponent ammoControl(int equipmentNumber, AmmoMounted ammoBin, CheckCritPanel crit) {
        int fullShots = fullShots(ammoBin);
        JSpinner shots = new JSpinner(new SpinnerNumberModel(Math.min(ammoBin.getBaseShotsLeft(), fullShots),
              0,
              fullShots,
              1));
        controls.ammoShots.put(equipmentNumber, shots);

        // remembered so that taking the crit off can put the shots back
        int[] shotsBeforeCrit = { (Integer) shots.getValue() };
        boolean[] wasCritted = { crit.getHits() > 0 };
        if (wasCritted[0]) {
            // a bin already destroyed in play holds nothing, so there is a full bin to restore rather than none
            shotsBeforeCrit[0] = fullShots;
            shots.setValue(0);
            shots.setEnabled(false);
        }

        crit.addHitsChangedListener(() -> {
            boolean critted = crit.getHits() > 0;
            if (critted == wasCritted[0]) {
                return;
            }
            wasCritted[0] = critted;
            if (critted) {
                shotsBeforeCrit[0] = (Integer) shots.getValue();
                shots.setValue(0);
            } else {
                shots.setValue(shotsBeforeCrit[0]);
            }
            shots.setEnabled(!critted);
        });

        JPanel control = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        control.add(crit);
        control.add(shots);
        control.add(new JLabel(Messages.getString("UnitEditorDialog.shots")));
        if (offersEquipmentSettings() && ammoBin.getType().hasFlag(AmmoType.F_HOTLOAD)
              && entity.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_COMBAT_TAC_OPS_HOT_LOAD)) {
            JCheckBox hotLoadCheckbox = new JCheckBox(Messages.getString("UnitEditorDialog.hotLoad"),
                  ammoBin.isHotLoaded());
            hotLoadCheckbox.setToolTipText(UIUtil.formatSideTooltip(
                  Messages.getString("UnitEditorDialog.hotLoad.tooltip")));
            controls.hotLoadedAmmo.put(equipmentNumber, hotLoadCheckbox);
            control.add(hotLoadCheckbox);
        }
        return control;
    }

    /**
     * The shots a bin holds when full. A bin's original shots are only recorded for some unit types, so the ammo's
     * own shot count stands in when they are not, and a bin that somehow holds more than either keeps what it has.
     */
    private int fullShots(AmmoMounted ammoBin) {
        if (ammoBin.isOneShot()) {
            return 1;
        }
        int fullShots = ammoBin.getType().getShots();
        if (ammoBin.getOriginalShots() > 0) {
            fullShots = ammoBin.getOriginalShots();
        }
        return Math.max(fullShots, ammoBin.getBaseShotsLeft());
    }

    /** Adds the unit-type specific system crits to the location panels they belong to. */
    private void initSystemCrits() {
        if (entity instanceof Mek) {
            setupMekSystemCrits();
        } else if (entity instanceof VTOL) {
            setupVtolSystemCrits();
        } else if (entity instanceof Tank) {
            setupTankSystemCrits();
        } else if (entity instanceof Aero) {
            setupAeroSystemCrits();
        } else if (entity instanceof ProtoMek) {
            setupProtoSystemCrits();
        }
    }

    private void setupMekSystemCrits() {
        /*
         * For the moment, I am going to cap out the number of hits at what the
         * record sheets show (i.e. 3 for engines). If we want to switch this to
         * the actual number then we can, see
         * enginePart.updateConditionFromEntity in MekHQ for an example of how
         * to retrieve all the available system crits
         */
        int centerEngineHits = 0;
        int leftEngineHits = 0;
        int rightEngineHits = 0;
        int gyroHits = 0;
        int cockpitHits = 0;
        int sensorHits = 0;
        int lifeSupportHits = 0;

        int centerEngineCrits = 0;
        int leftEngineCrits = 0;
        int rightEngineCrits = 0;
        int gyroCrits = 0;
        int cockpitCrits = 0;
        int sensorCrits = 0;
        int lifeSupportCrits = 0;
        for (int i = 0; i < entity.locations(); i++) {
            if (i == Mek.LOC_CENTER_TORSO) {
                centerEngineHits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
                centerEngineCrits = entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
            }
            if (i == Mek.LOC_LEFT_TORSO) {
                leftEngineHits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
                leftEngineCrits = entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
            }
            if (i == Mek.LOC_RIGHT_TORSO) {
                rightEngineHits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
                rightEngineCrits = entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i);
            }
            gyroHits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, i);
            gyroCrits += entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, i);
            cockpitHits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, i);
            cockpitCrits += entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, i);
            sensorHits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, i);
            sensorCrits += entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, i);
            lifeSupportHits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, i);
            lifeSupportCrits += entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, i);
        }
        controls.centerEngineCrit = new CheckCritPanel(centerEngineCrits, centerEngineHits);
        addCritRow(Mek.LOC_CENTER_TORSO, Messages.getString("UnitEditorDialog.engine"),
              controls.centerEngineCrit);

        if (entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, Mek.LOC_RIGHT_TORSO) > 0) {
            controls.leftEngineCrit = new CheckCritPanel(leftEngineCrits, leftEngineHits);
            addCritRow(Mek.LOC_LEFT_TORSO, Messages.getString("UnitEditorDialog.engine"),
                  controls.leftEngineCrit);

            controls.rightEngineCrit = new CheckCritPanel(rightEngineCrits, rightEngineHits);
            addCritRow(Mek.LOC_RIGHT_TORSO, Messages.getString("UnitEditorDialog.engine"),
                  controls.rightEngineCrit);
        }

        controls.gyroCrit = new CheckCritPanel(gyroCrits, gyroHits);
        addCritRow(Mek.LOC_CENTER_TORSO, Messages.getString("UnitEditorDialog.gyro"), controls.gyroCrit);

        controls.sensorCrit = new CheckCritPanel(sensorCrits, sensorHits);
        addCritRow(Mek.LOC_HEAD, Messages.getString("UnitEditorDialog.sensor"), controls.sensorCrit);

        controls.lifeSupportCrit = new CheckCritPanel(lifeSupportCrits, lifeSupportHits);
        addCritRow(Mek.LOC_HEAD, Messages.getString("UnitEditorDialog.lifeSupport"), controls.lifeSupportCrit);

        controls.cockpitCrit = new CheckCritPanel(cockpitCrits, cockpitHits);
        addCritRow(Mek.LOC_HEAD, Messages.getString("UnitEditorDialog.cockpit"), controls.cockpitCrit);

        if (entity instanceof LandAirMek) {
            controls.lamAvionicsCrit = new TreeMap<>();
            controls.lamLandingGearCrit = new TreeMap<>();
            for (int location = 0; location < entity.locations(); location++) {
                int crits = entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_AVIONICS, location);
                if (crits > 0) {
                    int hits = entity.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_AVIONICS, location);
                    CheckCritPanel critPanel = new CheckCritPanel(crits, hits);
                    controls.lamAvionicsCrit.put(location, critPanel);
                    addCritRow(location, Messages.getString("UnitEditorDialog.avionics"), critPanel);
                }
                crits = entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_LANDING_GEAR, location);
                if (crits > 0) {
                    int hits = entity.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_LANDING_GEAR, location);
                    CheckCritPanel critPanel = new CheckCritPanel(crits, hits);
                    controls.lamLandingGearCrit.put(location, critPanel);
                    addCritRow(location, Messages.getString("UnitEditorDialog.landingGear"), critPanel);
                }
            }
        }

        final boolean tripod = entity.hasETypeFlag(Entity.ETYPE_TRIPOD_MEK);
        if (tripod) {
            controls.actuatorCrits = new CheckCritPanel[5][4];
        } else if (entity instanceof QuadVee) {
            controls.actuatorCrits = new CheckCritPanel[4][5];
        } else {
            controls.actuatorCrits = new CheckCritPanel[4][4];
        }

        for (int location = Mek.LOC_RIGHT_ARM; location <= (tripod ? Mek.LOC_CENTER_LEG : Mek.LOC_LEFT_LEG); location++) {
            int start = Mek.ACTUATOR_SHOULDER;
            int end = Mek.ACTUATOR_HAND;
            if ((location >= Mek.LOC_RIGHT_LEG) || (entity instanceof QuadMek)) {
                start = Mek.ACTUATOR_HIP;
                end = Mek.ACTUATOR_FOOT;
            }

            for (int i = start; i <= end; i++) {
                if (!entity.hasSystem(i, location)) {
                    continue;
                }
                CheckCritPanel actuatorCrit = new CheckCritPanel(1,
                      entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, i, location));
                controls.actuatorCrits[location - Mek.LOC_RIGHT_ARM][i - start] = actuatorCrit;
                addCritRow(location, ((Mek) entity).getSystemName(i), actuatorCrit);
            }

            if (entity instanceof QuadVee) {
                CheckCritPanel actuatorCrit = new CheckCritPanel(1,
                      entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, QuadVee.SYSTEM_CONVERSION_GEAR, location));
                controls.actuatorCrits[location - Mek.LOC_RIGHT_ARM][UnitDamageControls.CONVERSION_GEAR_INDEX] = actuatorCrit;
                addCritRow(location, ((Mek) entity).getSystemName(QuadVee.SYSTEM_CONVERSION_GEAR),
                      actuatorCrit);
            }
        }
    }

    private void setupTankSystemCrits() {
        Tank tank = (Tank) entity;

        int lock = 0;
        if (tank.isTurretLocked(0)) {
            lock = 1;
        }
        controls.turretLockCrit = new CheckCritPanel(1, lock);
        int turretLocation = tank.hasNoTurret() ? Entity.LOC_NONE : tank.getLocTurret();
        addCritRow(turretLocation, Messages.getString("UnitEditorDialog.turretLock"), controls.turretLockCrit);

        controls.engineCrit = new CheckCritPanel(1, tank.getEngineHits());
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.engine"), controls.engineCrit);

        controls.sensorCrit = new CheckCritPanel(Tank.CRIT_SENSOR_MAX, tank.getSensorHits());
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.sensor"), controls.sensorCrit);

        int motiveHits = 0;
        // Do not check the crew when determining if we're immobile here
        if (tank.isImmobile(false)) {
            motiveHits = 4;
        } else if (tank.hasHeavyMovementDamage()) {
            motiveHits = 3;
        } else if (tank.hasModerateMovementDamage()) {
            motiveHits = 2;
        } else if (tank.hasMinorMovementDamage()) {
            motiveHits = 1;
        }
        controls.motiveCrit = new CheckCritPanel(4, motiveHits);
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.motiveDamage"), controls.motiveCrit);

        controls.stabilizerCrits = new CheckCritPanel[tank.locations()];
        for (int location = 0; location < tank.locations(); location++) {
            if ((location == Tank.LOC_BODY) || (location == tank.getLocTurret()) || (location == tank.getLocTurret2())) {
                continue;
            }
            int hits = 0;
            if (tank.isStabiliserHit(location)) {
                hits = 1;
            }
            CheckCritPanel stabCrit = new CheckCritPanel(1, hits);
            controls.stabilizerCrits[location] = stabCrit;
            addCritRow(location, Messages.getString("UnitEditorDialog.stabilizer"), stabCrit);
        }
    }

    private void setupProtoSystemCrits() {
        ProtoMek proto = (ProtoMek) entity;

        controls.protoCrits = new CheckCritPanel[proto.locations()];

        for (int location = 0; location < proto.locations(); location++) {
            if ((location == ProtoMek.LOC_MAIN_GUN) || (location == ProtoMek.LOC_NEAR_MISS)) {
                continue;
            }
            int hits = 0;
            if ((location == ProtoMek.LOC_LEFT_ARM) || (location == ProtoMek.LOC_RIGHT_ARM)) {
                hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_ARM_CRIT, location);
            }
            if (location == ProtoMek.LOC_LEG) {
                hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_LEG_CRIT, location);
            }
            if (location == ProtoMek.LOC_HEAD) {
                hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_HEAD_CRIT, location);
            }
            if (location == ProtoMek.LOC_TORSO) {
                hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSO_CRIT, location);
            }
            int nCrits = 2;
            if (location == ProtoMek.LOC_LEG) {
                nCrits = 3;
            }
            CheckCritPanel protoCrit = new CheckCritPanel(nCrits, hits);
            controls.protoCrits[location] = protoCrit;
            addCritRow(location, Messages.getString("UnitEditorDialog.crits"), protoCrit);
        }
    }

    private void setupVtolSystemCrits() {
        VTOL vtol = (VTOL) entity;

        int flightStabHit = 0;
        if (vtol.isStabiliserHit(VTOL.LOC_ROTOR)) {
            flightStabHit = 1;
        }
        controls.flightStabilizerCrit = new CheckCritPanel(1, flightStabHit);
        addCritRow(VTOL.LOC_ROTOR, Messages.getString("UnitEditorDialog.flightStabilizer"),
              controls.flightStabilizerCrit);

        controls.engineCrit = new CheckCritPanel(1, vtol.getEngineHits());
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.engine"), controls.engineCrit);

        controls.sensorCrit = new CheckCritPanel(Tank.CRIT_SENSOR_MAX, vtol.getSensorHits());
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.sensor"), controls.sensorCrit);

        controls.stabilizerCrits = new CheckCritPanel[vtol.locations()];
        for (int location = 0; location < vtol.locations(); location++) {
            if ((location == Tank.LOC_BODY) || (location == VTOL.LOC_ROTOR)) {
                continue;
            }
            int hits = 0;
            if (vtol.isStabiliserHit(location)) {
                hits = 1;
            }
            CheckCritPanel stabCrit = new CheckCritPanel(1, hits);
            controls.stabilizerCrits[location] = stabCrit;
            addCritRow(location, Messages.getString("UnitEditorDialog.stabilizer"), stabCrit);
        }
    }

    private void setupAeroSystemCrits() {
        Aero aero = (Aero) entity;

        controls.avionicsCrit = new CheckCritPanel(3, aero.getAvionicsHits());
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.avionics"), controls.avionicsCrit);

        if (aero instanceof Jumpship) {
            controls.cicCrit = new CheckCritPanel(3, aero.getCICHits());
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.cic"), controls.cicCrit);
        } else {
            controls.fcsCrit = new CheckCritPanel(3, aero.getFCSHits());
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.fcs"), controls.fcsCrit);
        }

        controls.sensorCrit = new CheckCritPanel(3, aero.getSensorHits());
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.sensor"), controls.sensorCrit);

        controls.engineCrit = new CheckCritPanel(3, aero.getEngineHits());
        if ((aero instanceof Dropship) || (aero instanceof Jumpship)) {
            controls.engineCrit = new CheckCritPanel(6, aero.getEngineHits());
        }
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.engine"), controls.engineCrit);

        if (!(aero instanceof Jumpship)) {
            int gearHits = 0;
            if (aero.isGearHit()) {
                gearHits = 1;
            }
            controls.gearCrit = new CheckCritPanel(1, gearHits);
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.landingGear"), controls.gearCrit);
        }

        int lifeHits = 0;
        if (!aero.hasLifeSupport()) {
            lifeHits = 1;
        }
        controls.lifeSupportCrit = new CheckCritPanel(1, lifeHits);
        addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.lifeSupport"), controls.lifeSupportCrit);

        if ((aero instanceof SmallCraft) || (aero instanceof Jumpship)) {
            controls.leftThrusterCrit = new CheckCritPanel(4, aero.getLeftThrustHits());
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.leftThruster"), controls.leftThrusterCrit);

            controls.rightThrusterCrit = new CheckCritPanel(4, aero.getRightThrustHits());
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.rightThruster"), controls.rightThrusterCrit);
        }

        if (aero instanceof Jumpship js) {
            controls.gravDeckCrit = new CheckCritPanel(js.getTotalGravDeck(), js.getTotalDamagedGravDeck());
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.gravDecks"), controls.gravDeckCrit);

            Vector<DockingCollar> collars = aero.getDockingCollars();
            int damagedCollars = 0;
            for (DockingCollar nextDC : aero.getDockingCollars()) {
                if (nextDC.isDamaged()) {
                    damagedCollars++;
                }
            }
            controls.collarDamage = new JSpinner(new SpinnerNumberModel(collars.size() - damagedCollars,
                  0,
                  collars.size(),
                  1.0));
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.dockingCollars"), controls.collarDamage);

            controls.kfDamage = new JSpinner(new SpinnerNumberModel(js.getKFIntegrity(), 0, js.getOKFIntegrity(), 1.0));
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.kfIntegrity"), controls.kfDamage);

            // K-F Drive Components (Optional)
            if (entity.getGame()
                  .getOptions()
                  .booleanOption(OptionsConstants.ADVANCED_AERO_RULES_EXPANDED_KF_DRIVE_DAMAGE)) {
                int driveCoilHits = 0;
                if (js.getKFDriveCoilHit()) {
                    driveCoilHits = 1;
                }
                controls.driveCoilCrit = new CheckCritPanel(1, driveCoilHits);
                addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.driveCoil"), controls.driveCoilCrit);

                int chargingSystemHits = 0;
                if (js.getKFChargingSystemHit()) {
                    chargingSystemHits = 1;
                }
                controls.chargingSystemCrit = new CheckCritPanel(1, chargingSystemHits);
                addLabeledRow(generalPanel(),
                      Messages.getString("UnitEditorDialog.chargingSystem"),
                      controls.chargingSystemCrit);

                int fieldInitiatorHits = 0;
                if (js.getKFFieldInitiatorHit()) {
                    fieldInitiatorHits = 1;
                }
                controls.fieldInitiatorCrit = new CheckCritPanel(1, fieldInitiatorHits);
                addLabeledRow(generalPanel(),
                      Messages.getString("UnitEditorDialog.fieldInitiator"),
                      controls.fieldInitiatorCrit);

                int driveControllerHits = 0;
                if (js.getKFDriveControllerHit()) {
                    driveControllerHits = 1;
                }
                controls.driveControllerCrit = new CheckCritPanel(1, driveControllerHits);
                addLabeledRow(generalPanel(),
                      Messages.getString("UnitEditorDialog.driveController"),
                      controls.driveControllerCrit);

                int heliumTankHits = 0;
                if (js.getKFHeliumTankHit()) {
                    heliumTankHits = 1;
                }
                controls.heliumTankCrit = new CheckCritPanel(1, heliumTankHits);
                addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.heliumTank"), controls.heliumTankCrit);

                if (js.hasLF()) {
                    int lfBatteryHits = 0;
                    if (js.getLFBatteryHit()) {
                        lfBatteryHits = 1;
                    }
                    controls.lfBatteryCrit = new CheckCritPanel(1, lfBatteryHits);
                    addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.lfBattery"), controls.lfBatteryCrit);
                }
            }

            controls.sailDamage = new JSpinner(new SpinnerNumberModel(js.getSailIntegrity(), 0, js.getOSailIntegrity(), 1.0));
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.sailIntegrity"), controls.sailDamage);
        }

        if (aero instanceof Dropship) {
            int collarHits = 0;
            if (((Dropship) aero).isDockCollarDamaged()) {
                collarHits = 1;
            }
            controls.dockCollarCrit = new CheckCritPanel(1, collarHits);
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.dropshipCollar"), controls.dockCollarCrit);

            int kfBoomHits = 0;
            if (((Dropship) aero).isKFBoomDamaged()) {
                kfBoomHits = 1;
            }
            controls.kfBoomCrit = new CheckCritPanel(1, kfBoomHits);
            addLabeledRow(generalPanel(), Messages.getString("UnitEditorDialog.kfBoom"), controls.kfBoomCrit);
        }

        if ((aero instanceof SmallCraft) || (aero instanceof Jumpship)) {
            int b = 0;
            Vector<Bay> bays = aero.getTransportBays();
            controls.bayDamage = new JSpinner[bays.size()];
            controls.bayDoorCrit = new CheckCritPanel[bays.size()];
            for (Bay nextbay : bays) {
                JSpinner bayCrit = new JSpinner(new SpinnerNumberModel(nextbay.getCapacity() - nextbay.getBayDamage(),
                      0,
                      nextbay.getCapacity(),
                      nextbay.isCargo() ? 0.5 : 1.0));
                controls.bayDamage[b] = bayCrit;

                CheckCritPanel doorCrit = new CheckCritPanel(nextbay.getDoors(),
                      (nextbay.getDoors() - nextbay.getCurrentDoors()));
                controls.bayDoorCrit[b] = doorCrit;

                // Put the bay's capacity and its door checkboxes on one row, so a bay reads as a single line
                // rather than a separate capacity row and doors row.
                JPanel bayControl = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
                bayControl.add(bayCrit);
                if (nextbay.getDoors() > 0) {
                    bayControl.add(new JLabel(Messages.getString("UnitEditorDialog.bayDoors")));
                    bayControl.add(doorCrit);
                }
                addLabeledRow(generalPanel(),
                      String.format(Messages.getString("UnitEditorDialog.bayCrit"),
                            nextbay.getTransporterType(),
                            nextbay.getBayNumber()),
                      bayControl);
                b++;
            }
        }
    }

}
