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

import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;

import megamek.common.annotations.Nullable;
import megamek.common.compute.damage.CritAssignment;
import megamek.common.compute.damage.PreExistingDamageApplier;
import megamek.common.compute.damage.PreExistingDamageLevel;
import megamek.common.compute.damage.PreExistingDamageResult;
import megamek.common.units.Aero;
import megamek.common.units.Entity;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.common.units.QuadMek;
import megamek.common.units.QuadVee;
import megamek.common.units.VTOL;

/**
 * Rolls pre-existing damage (FSW p.144) into a unit editor's controls, and restores them. The roll writes into the
 * editor's spinners and crit controls rather than the unit, so the editor's diagram and coloring update through the
 * controls and nothing is committed until the user presses Okay.
 */
public class PreExistingDamageRoller {

    private final Entity entity;
    private final UnitDamageControls controls;

    /** The control values captured when the editor opened, so each roll starts from the same state. */
    private int[] snapshotArmor;
    private int[] snapshotRear;
    private int[] snapshotInternal;
    private Map<CheckCritPanel, Integer> snapshotCritHits = new HashMap<>();

    public PreExistingDamageRoller(Entity entity, UnitDamageControls controls) {
        this.entity = entity;
        this.controls = controls;
    }

    /** Remembers the control values at editor open, so each pre-existing damage roll starts from the same state. */
    public void captureSnapshot(Container root) {
        snapshotArmor = new int[entity.locations()];
        snapshotRear = new int[entity.locations()];
        snapshotInternal = new int[entity.locations()];
        for (int location = 0; location < entity.locations(); location++) {
            if (controls.spnArmor[location] != null) {
                snapshotArmor[location] = (Integer) controls.spnArmor[location].getValue();
            }
            if (controls.spnRear[location] != null) {
                snapshotRear[location] = (Integer) controls.spnRear[location].getValue();
            }
            if (controls.spnInternal[location] != null) {
                snapshotInternal[location] = (Integer) controls.spnInternal[location].getValue();
            }
        }
        snapshotCritHits = new HashMap<>();
        collectCritPanels(root);
    }

    private void collectCritPanels(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof CheckCritPanel critPanel) {
                snapshotCritHits.put(critPanel, critPanel.getHits());
            } else if (component instanceof Container childContainer) {
                collectCritPanels(childContainer);
            }
        }
    }

    private void restoreSnapshot() {
        for (int location = 0; location < entity.locations(); location++) {
            if (controls.spnArmor[location] != null) {
                controls.spnArmor[location].setValue(snapshotArmor[location]);
            }
            if (controls.spnRear[location] != null) {
                controls.spnRear[location].setValue(snapshotRear[location]);
            }
            if (controls.spnInternal[location] != null) {
                controls.spnInternal[location].setValue(snapshotInternal[location]);
            }
        }
        snapshotCritHits.forEach(CheckCritPanel::setHits);
    }

    /**
     * Rolls pre-existing damage at the given level and shows it by filling the editor's controls, so the result
     * appears on the armor diagram. The unit itself is untouched; Apply or Okay commits the values and Cancel
     * discards them. Each roll rerolls from the state the unit had when the editor opened.
     */
    public void roll(@Nullable PreExistingDamageLevel level) {
        restoreSnapshot();
        if ((level == null) || (level == PreExistingDamageLevel.NONE)) {
            return;
        }
        PreExistingDamageResult result = PreExistingDamageApplier.simulate(entity, level);
        for (int location = 0; location < entity.locations(); location++) {
            if (controls.spnArmor[location] != null) {
                controls.spnArmor[location].setValue(result.armor()[location]);
            }
            if (controls.spnRear[location] != null) {
                controls.spnRear[location].setValue(result.rearArmor()[location]);
            }
        }
        if (entity instanceof Aero) {
            if (controls.spnInternal[0] != null) {
                controls.spnInternal[0].setValue(result.structuralIntegrity());
            }
        } else {
            for (int location = 0; location < entity.locations(); location++) {
                if (controls.spnInternal[location] != null) {
                    controls.spnInternal[location].setValue(result.internal()[location]);
                }
            }
        }
        for (CritAssignment assignment : result.critAssignments()) {
            applyCritAssignment(assignment);
        }
    }

    /**
     * Sets the editor controls to a fully repaired unit: every spinner (armor, structure, bay capacity, drive
     * integrity) to its maximum and every critical hit cleared. Only pressing Okay commits the values.
     */
    public void restoreToFactoryNew(Container root) {
        restoreSpinnersToMaximum(root);
        snapshotCritHits.keySet().forEach(critPanel -> critPanel.setHits(0));
        // heat and crew hits are damage counting up, not health counting down, so a repaired unit has none of them
        setSpinnerToZero(controls.spnHeat);
        if (controls.spnCrewHits != null) {
            for (JSpinner crewHits : controls.spnCrewHits) {
                setSpinnerToZero(crewHits);
            }
        }
        // a factory-new unit carries no gamemaster skill modifiers either; every delta at zero clears them on Okay,
        // and without this the sweep above would leave the modifier spinners at their maximum instead
        setSpinnerToZero(controls.spnGunneryModifier);
        setSpinnerToZero(controls.spnPilotingModifier);
        setSpinnerToZero(controls.spnInitiativeModifier);
        resetModifierDuration(controls.spnGunneryRounds, controls.chkGunneryPermanent);
        resetModifierDuration(controls.spnPilotingRounds, controls.chkPilotingPermanent);
        resetModifierDuration(controls.spnInitiativeRounds, controls.chkInitiativePermanent);
        clearDamageStates();
    }

    /**
     * Unticks every state that is damage by another name: breached and blown-off locations, jammed weapons, spent
     * one-shot launchers, locked directional mounts, and a building's locked turrets and killed gunners. The
     * settings switches - ejection, burst fire, hot-loading, modes - are how the unit is set up rather than what
     * has happened to it, so they stay as they are.
     */
    private void clearDamageStates() {
        untickAll(controls.chkLocationBreached);
        untickAll(controls.chkLocationBlownOff);
        untickAll(controls.weaponJammed.values());
        untickAll(controls.weaponFired.values());
        untickAll(controls.directionalMountLocked.values());
        untickAll(controls.buildingTurretLocked.values());
        untickAll(controls.buildingGunnersKilled.values());
        setSpinnerToZero(controls.spnBuildingStunnedTurns);
    }

    /** Unticks each checkbox of a row that may be missing or hold gaps. */
    private static void untickAll(@Nullable JCheckBox[] checkboxes) {
        if (checkboxes == null) {
            return;
        }
        for (JCheckBox checkbox : checkboxes) {
            if (checkbox != null) {
                checkbox.setSelected(false);
            }
        }
    }

    /** Unticks each checkbox in the collection. */
    private static void untickAll(Collection<JCheckBox> checkboxes) {
        for (JCheckBox checkbox : checkboxes) {
            checkbox.setSelected(false);
        }
    }

    /** Puts one modifier's duration controls back to their fresh state: the default rounds, not permanent. */
    private void resetModifierDuration(JSpinner roundsSpinner, JCheckBox permanentCheckbox) {
        if (roundsSpinner != null) {
            roundsSpinner.setValue(UnitDamagePanelBuilder.DEFAULT_MODIFIER_ROUNDS);
        }
        if (permanentCheckbox != null) {
            permanentCheckbox.setSelected(false);
        }
    }

    private void restoreSpinnersToMaximum(Container container) {
        for (Component component : container.getComponents()) {
            if ((component instanceof JSpinner spinner)
                  && (spinner.getModel() instanceof SpinnerNumberModel model)) {
                spinner.setValue(model.getMaximum());
            } else if (component instanceof Container childContainer) {
                restoreSpinnersToMaximum(childContainer);
            }
        }
    }

    private void setSpinnerToZero(@Nullable JSpinner spinner) {
        if (spinner != null) {
            spinner.setValue(0);
        }
    }

    private void applyCritAssignment(CritAssignment assignment) {
        switch (assignment) {
            case CritAssignment.EquipmentCrit(int equipmentNumber) -> incrementCrit(controls.equipCrits.get(equipmentNumber));
            case CritAssignment.MekSystemCrit(int system, int location) -> applyMekSystemCrit(system, location);
            case CritAssignment.VehicleCrit(CritAssignment.VehicleCritKind kind, int location) ->
                  applyVehicleCrit(kind, location);
            case CritAssignment.AeroFighterCrit(CritAssignment.AeroFighterCritKind kind) -> applyFighterCrit(kind);
        }
    }

    private void applyMekSystemCrit(int system, int location) {
        if (entity instanceof LandAirMek) {
            if (system == LandAirMek.LAM_AVIONICS) {
                incrementCrit(controls.lamAvionicsCrit.get(location));
                return;
            }
            if (system == LandAirMek.LAM_LANDING_GEAR) {
                incrementCrit(controls.lamLandingGearCrit.get(location));
                return;
            }
        }
        if ((entity instanceof QuadVee) && (system == QuadVee.SYSTEM_CONVERSION_GEAR)) {
            incrementCrit(controls.actuatorCrits[location - Mek.LOC_RIGHT_ARM][Mek.ACTUATOR_FOOT - Mek.ACTUATOR_HIP + 1]);
            return;
        }
        switch (system) {
            case Mek.SYSTEM_ENGINE -> {
                switch (location) {
                    case Mek.LOC_LEFT_TORSO -> incrementCrit(controls.leftEngineCrit);
                    case Mek.LOC_RIGHT_TORSO -> incrementCrit(controls.rightEngineCrit);
                    default -> incrementCrit(controls.centerEngineCrit);
                }
            }
            case Mek.SYSTEM_GYRO -> incrementCrit(controls.gyroCrit);
            case Mek.SYSTEM_SENSORS -> incrementCrit(controls.sensorCrit);
            case Mek.SYSTEM_LIFE_SUPPORT -> incrementCrit(controls.lifeSupportCrit);
            default -> applyActuatorCrit(system, location);
        }
    }

    private void applyActuatorCrit(int actuator, int location) {
        if ((actuator < Mek.ACTUATOR_SHOULDER) || (actuator > Mek.ACTUATOR_FOOT)) {
            return;
        }
        int row = location - Mek.LOC_RIGHT_ARM;
        if ((row < 0) || (row >= controls.actuatorCrits.length)) {
            return;
        }
        int start = ((location >= Mek.LOC_RIGHT_LEG) || (entity instanceof QuadMek))
              ? Mek.ACTUATOR_HIP : Mek.ACTUATOR_SHOULDER;
        int column = actuator - start;
        if ((column < 0) || (column >= controls.actuatorCrits[row].length)) {
            return;
        }
        incrementCrit(controls.actuatorCrits[row][column]);
    }

    private void applyVehicleCrit(CritAssignment.VehicleCritKind kind, int location) {
        switch (kind) {
            case TURRET_LOCK -> incrementCrit(controls.turretLockCrit);
            case SENSORS -> incrementCrit(controls.sensorCrit);
            case MOTIVE -> incrementCrit(controls.motiveCrit);
            case STABILIZER -> {
                if ((entity instanceof VTOL) && (location == VTOL.LOC_ROTOR)) {
                    incrementCrit(controls.flightStabilizerCrit);
                } else if ((controls.stabilizerCrits != null) && (location >= 0) && (location < controls.stabilizerCrits.length)) {
                    incrementCrit(controls.stabilizerCrits[location]);
                }
            }
        }
    }

    private void applyFighterCrit(CritAssignment.AeroFighterCritKind kind) {
        switch (kind) {
            case AVIONICS -> incrementCrit(controls.avionicsCrit);
            case FIRE_CONTROL_SYSTEM -> incrementCrit(controls.fcsCrit);
            case SENSORS -> incrementCrit(controls.sensorCrit);
            case ENGINE -> incrementCrit(controls.engineCrit);
            case LANDING_GEAR -> incrementCrit(controls.gearCrit);
        }
    }

    private void incrementCrit(@Nullable CheckCritPanel critPanel) {
        if (critPanel != null) {
            critPanel.setHits(critPanel.getHits() + 1);
        }
    }
}
