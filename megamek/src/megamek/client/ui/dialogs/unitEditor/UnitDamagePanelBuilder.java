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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import megamek.client.ui.Messages;
import megamek.common.CriticalSlot;
import megamek.common.bays.Bay;
import megamek.common.equipment.DockingCollar;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
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
     * The most hits a crew member can be given. Six hits kill (TW p.41), so the editor stops one short: it wounds
     * and revives crew, and destroying a unit is a separate, deliberate act.
     */
    public static final int MAX_CREW_HITS = Crew.DEATH - 1;

    private final Entity entity;
    private final UnitDamageControls controls;

    public UnitDamagePanelBuilder(Entity entity, UnitDamageControls controls) {
        this.entity = entity;
        this.controls = controls;
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
            // some units have hidden locations, skip these
            if (isAero ? (entity.getOArmor(location) <= 0) : (entity.getOInternal(location) <= 0)) {
                continue;
            }
            controls.locationLabels[location] = new JLabel(entity.getLocationName(location));
            controls.locationPanels[location] = createTitledPanel(controls.locationLabels[location]);

            if (!isAero) {
                int internal = Math.max(entity.getInternal(location), 0);
                controls.spnInternal[location] = new JSpinner(new SpinnerNumberModel(internal,
                      0,
                      entity.getOInternal(location),
                      1));
                addLabeledRow(controls.locationPanels[location],
                      Messages.getString("UnitEditorDialog.internal"),
                      controls.spnInternal[location]);
            }

            int armor = Math.max(entity.getArmor(location, false), 0);
            controls.spnArmor[location] = new JSpinner(new SpinnerNumberModel(armor, 0, entity.getOArmor(location), 1));
            boolean hasRear = entity.hasRearArmor(location);
            addLabeledRow(controls.locationPanels[location],
                  Messages.getString(hasRear ? "UnitEditorDialog.armorFront" : "UnitEditorDialog.armor"),
                  controls.spnArmor[location]);
            if (hasRear) {
                int rear = Math.max(entity.getArmor(location, true), 0);
                controls.spnRear[location] = new JSpinner(new SpinnerNumberModel(rear,
                      0,
                      entity.getOArmor(location, true),
                      1));
                addLabeledRow(controls.locationPanels[location],
                      Messages.getString("UnitEditorDialog.armorRear"),
                      controls.spnRear[location]);
            }
        }

        if (isAero) {
            Aero aero = (Aero) entity;
            int structuralIntegrity = Math.max(aero.getSI(), 0);
            controls.spnInternal[0] = new JSpinner(new SpinnerNumberModel(structuralIntegrity, 0, aero.getOSI(), 1));
            controls.structuralIntegrityLabel = new JLabel("<html><b>" +
                  Messages.getString("UnitEditorDialog.structuralIntegrity") +
                  "</b></html>");
            addRow(generalPanel(), controls.structuralIntegrityLabel, controls.spnInternal[0]);
        }

        initCrewHits();
        initHeat();
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
        int row = controls.panelRows.merge(panel, 1, Integer::sum) - 1;
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = row;
        gridBagConstraints.insets = new Insets(1, 5, 1, 5);
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.0;
        panel.add(label, gridBagConstraints);
        gridBagConstraints.gridx = 1;
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
        for (Mounted<?> mounted : entity.getEquipment()) {
            if ((mounted.getLocation() == Entity.LOC_NONE) ||
                  !mounted.getType().isHittable() ||
                  mounted.isWeaponGroup()) {
                continue;
            }
            if (mounted.getType() instanceof InfantryAttack) {
                continue;
            }
            int nCrits = mounted.getNumCriticalSlots();
            int eqNum = entity.getEquipmentNum(mounted);
            int hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, eqNum, mounted.getLocation());
            if (mounted.isSplit()) {
                hits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, eqNum, mounted.getSecondLocation());
            }
            if ((mounted.getType() instanceof MiscType) && (mounted.getType().hasFlag(MiscType.F_PARTIAL_WING))) {
                hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, eqNum, Mek.LOC_LEFT_TORSO);
                hits += entity.getDamagedCriticalSlots(CriticalSlot.TYPE_EQUIPMENT, eqNum, Mek.LOC_RIGHT_TORSO);
            }

            if (!(entity instanceof Mek)) {
                nCrits = 1;
                if (hits > 1) {
                    hits = 1;
                }
            }
            CheckCritPanel crit = new CheckCritPanel(nCrits, hits);
            controls.equipCrits.put(eqNum, crit);
            String label = mounted.getName();
            if (mounted.isSplit()) {
                label += " (" + entity.getLocationAbbr(mounted.getLocation()) + "/"
                      + entity.getLocationAbbr(mounted.getSecondLocation()) + ")";
            }
            addLabeledRow(targetPanel(mounted.getLocation()), label, crit);
        }
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
            for (int loc = 0; loc < entity.locations(); loc++) {
                int crits = entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_AVIONICS, loc);
                if (crits > 0) {
                    int hits = entity.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_AVIONICS, loc);
                    CheckCritPanel critPanel = new CheckCritPanel(crits, hits);
                    controls.lamAvionicsCrit.put(loc, critPanel);
                    addCritRow(loc, Messages.getString("UnitEditorDialog.avionics"), critPanel);
                }
                crits = entity.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_LANDING_GEAR, loc);
                if (crits > 0) {
                    int hits = entity.getBadCriticalSlots(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_LANDING_GEAR, loc);
                    CheckCritPanel critPanel = new CheckCritPanel(crits, hits);
                    controls.lamLandingGearCrit.put(loc, critPanel);
                    addCritRow(loc, Messages.getString("UnitEditorDialog.landingGear"), critPanel);
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

        for (int loc = Mek.LOC_RIGHT_ARM; loc <= (tripod ? Mek.LOC_CENTER_LEG : Mek.LOC_LEFT_LEG); loc++) {
            int start = Mek.ACTUATOR_SHOULDER;
            int end = Mek.ACTUATOR_HAND;
            if ((loc >= Mek.LOC_RIGHT_LEG) || (entity instanceof QuadMek)) {
                start = Mek.ACTUATOR_HIP;
                end = Mek.ACTUATOR_FOOT;
            }

            for (int i = start; i <= end; i++) {
                if (!entity.hasSystem(i, loc)) {
                    continue;
                }
                CheckCritPanel actuatorCrit = new CheckCritPanel(1,
                      entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, i, loc));
                controls.actuatorCrits[loc - Mek.LOC_RIGHT_ARM][i - start] = actuatorCrit;
                addCritRow(loc, ((Mek) entity).getSystemName(i), actuatorCrit);
            }

            if (entity instanceof QuadVee) {
                CheckCritPanel actuatorCrit = new CheckCritPanel(1,
                      entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, QuadVee.SYSTEM_CONVERSION_GEAR, loc));
                controls.actuatorCrits[loc - Mek.LOC_RIGHT_ARM][Mek.ACTUATOR_FOOT - Mek.ACTUATOR_HIP + 1] = actuatorCrit;
                addCritRow(loc, ((Mek) entity).getSystemName(QuadVee.SYSTEM_CONVERSION_GEAR),
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
        for (int loc = 0; loc < tank.locations(); loc++) {
            if ((loc == Tank.LOC_BODY) || (loc == tank.getLocTurret()) || (loc == tank.getLocTurret2())) {
                continue;
            }
            int hits = 0;
            if (tank.isStabiliserHit(loc)) {
                hits = 1;
            }
            CheckCritPanel stabCrit = new CheckCritPanel(1, hits);
            controls.stabilizerCrits[loc] = stabCrit;
            addCritRow(loc, Messages.getString("UnitEditorDialog.stabilizer"), stabCrit);
        }
    }

    private void setupProtoSystemCrits() {
        ProtoMek proto = (ProtoMek) entity;

        controls.protoCrits = new CheckCritPanel[proto.locations()];

        for (int loc = 0; loc < proto.locations(); loc++) {
            if ((loc == ProtoMek.LOC_MAIN_GUN) || (loc == ProtoMek.LOC_NEAR_MISS)) {
                continue;
            }
            int hits = 0;
            if ((loc == ProtoMek.LOC_LEFT_ARM) || (loc == ProtoMek.LOC_RIGHT_ARM)) {
                hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_ARM_CRIT, loc);
            }
            if (loc == ProtoMek.LOC_LEG) {
                hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_LEG_CRIT, loc);
            }
            if (loc == ProtoMek.LOC_HEAD) {
                hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_HEAD_CRIT, loc);
            }
            if (loc == ProtoMek.LOC_TORSO) {
                hits = entity.getDamagedCriticalSlots(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSO_CRIT, loc);
            }
            int nCrits = 2;
            if (loc == ProtoMek.LOC_LEG) {
                nCrits = 3;
            }
            CheckCritPanel protoCrit = new CheckCritPanel(nCrits, hits);
            controls.protoCrits[loc] = protoCrit;
            addCritRow(loc, Messages.getString("UnitEditorDialog.crits"), protoCrit);
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
        for (int loc = 0; loc < vtol.locations(); loc++) {
            if ((loc == Tank.LOC_BODY) || (loc == VTOL.LOC_ROTOR)) {
                continue;
            }
            int hits = 0;
            if (vtol.isStabiliserHit(loc)) {
                hits = 1;
            }
            CheckCritPanel stabCrit = new CheckCritPanel(1, hits);
            controls.stabilizerCrits[loc] = stabCrit;
            addCritRow(loc, Messages.getString("UnitEditorDialog.stabilizer"), stabCrit);
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
                addLabeledRow(generalPanel(),
                      String.format(Messages.getString("UnitEditorDialog.bayCrit"),
                            nextbay.getTransporterType(),
                            nextbay.getBayNumber()),
                      bayCrit);

                CheckCritPanel doorCrit = new CheckCritPanel(nextbay.getDoors(),
                      (nextbay.getDoors() - nextbay.getCurrentDoors()));
                controls.bayDoorCrit[b] = doorCrit;
                addLabeledRow(generalPanel(),
                      String.format(Messages.getString("UnitEditorDialog.bayDoorCrit"), nextbay.getBayNumber()),
                      doorCrit);
                b++;
            }
        }
    }

    /**
     * Applies the given number of total crits to a Super-Cooled Myomer (which is spread over 6 locations).
     */
}
