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

import java.util.Map;
import javax.swing.JCheckBox;
import javax.swing.JSpinner;

import megamek.common.annotations.Nullable;
import megamek.common.units.DamageEditSpec;
import megamek.common.units.Entity;

/**
 * Reads the damage editor's controls into a {@link DamageEditSpec}, the plain-value form of the edits that can be
 * applied to a unit anywhere: locally in the lobby, or on the server in play. This is the only class that reads the
 * controls, so the values leave Swing here and nowhere else.
 */
public class UnitDamageSpecBuilder {

    private final Entity entity;
    private final UnitDamageControls controls;

    public UnitDamageSpecBuilder(Entity entity, UnitDamageControls controls) {
        this.entity = entity;
        this.controls = controls;
    }

    /** Builds the spec from the current state of the controls. */
    public DamageEditSpec build() {
        DamageEditSpec spec = new DamageEditSpec();
        spec.entityId = entity.getId();

        spec.internal = spinnerValues(controls.spnInternal);
        spec.armor = spinnerValues(controls.spnArmor);
        spec.rearArmor = spinnerValues(controls.spnRear);
        spec.crewHits = spinnerValues(controls.spnCrewHits);
        spec.heat = spinnerValue(controls.spnHeat);

        spec.shutdown = checkboxValue(controls.chkShutdown);
        spec.prone = checkboxValue(controls.chkProne);
        spec.hullDown = checkboxValue(controls.chkHullDown);
        spec.hidden = checkboxValue(controls.chkHidden);
        spec.stealth = checkboxValue(controls.chkStealth);
        spec.dugIn = checkboxValue(controls.chkDugIn);
        spec.fuel = spinnerValue(controls.spnFuel);

        for (Map.Entry<Integer, JSpinner> ammoShots : controls.ammoShots.entrySet()) {
            spec.ammoShots.put(ammoShots.getKey(), (Integer) ammoShots.getValue().getValue());
        }
        for (Map.Entry<Integer, CheckCritPanel> equipCrit : controls.equipCrits.entrySet()) {
            spec.equipmentHits.put(equipCrit.getKey(), equipCrit.getValue().getHits());
        }
        for (Map.Entry<Integer, JCheckBox> mgBurst : controls.mgBurst.entrySet()) {
            spec.mgBurst.put(mgBurst.getKey(), mgBurst.getValue().isSelected());
        }
        for (Map.Entry<Integer, JCheckBox> hotLoaded : controls.hotLoadedAmmo.entrySet()) {
            spec.hotLoadedAmmo.put(hotLoaded.getKey(), hotLoaded.getValue().isSelected());
        }

        if (null != controls.spnGunneryModifier) {
            spec.gunneryModifier = (Integer) controls.spnGunneryModifier.getValue();
            spec.gunneryRounds = (Integer) controls.spnGunneryRounds.getValue();
            spec.gunneryPermanent = controls.chkGunneryPermanent.isSelected();
            spec.pilotingModifier = (Integer) controls.spnPilotingModifier.getValue();
            spec.pilotingRounds = (Integer) controls.spnPilotingRounds.getValue();
            spec.pilotingPermanent = controls.chkPilotingPermanent.isSelected();
        }
        // offered separately: the initiative row only exists under individual initiative
        if (null != controls.spnInitiativeModifier) {
            spec.initiativeModifier = (Integer) controls.spnInitiativeModifier.getValue();
            spec.initiativeRounds = (Integer) controls.spnInitiativeRounds.getValue();
            spec.initiativePermanent = controls.chkInitiativePermanent.isSelected();
        }

        spec.centerEngineHits = critHits(controls.centerEngineCrit);
        spec.leftEngineHits = critHits(controls.leftEngineCrit);
        spec.rightEngineHits = critHits(controls.rightEngineCrit);
        spec.gyroHits = critHits(controls.gyroCrit);
        spec.sensorHits = critHits(controls.sensorCrit);
        spec.lifeSupportHits = critHits(controls.lifeSupportCrit);
        spec.cockpitHits = critHits(controls.cockpitCrit);
        if (null != controls.lamAvionicsCrit) {
            for (Map.Entry<Integer, CheckCritPanel> avionicsCrit : controls.lamAvionicsCrit.entrySet()) {
                spec.lamAvionicsHits.put(avionicsCrit.getKey(), avionicsCrit.getValue().getHits());
            }
        }
        if (null != controls.lamLandingGearCrit) {
            for (Map.Entry<Integer, CheckCritPanel> landingGearCrit : controls.lamLandingGearCrit.entrySet()) {
                spec.lamLandingGearHits.put(landingGearCrit.getKey(), landingGearCrit.getValue().getHits());
            }
        }
        if (null != controls.actuatorCrits) {
            spec.actuatorHits = new Integer[controls.actuatorCrits.length][];
            for (int i = 0; i < controls.actuatorCrits.length; i++) {
                spec.actuatorHits[i] = critHitsRow(controls.actuatorCrits[i]);
            }
        }

        spec.engineHits = critHits(controls.engineCrit);
        spec.turretLockHits = critHits(controls.turretLockCrit);
        spec.motiveHits = critHits(controls.motiveCrit);
        spec.stabilizerHits = critHitsRow(controls.stabilizerCrits);
        spec.flightStabilizerHits = critHits(controls.flightStabilizerCrit);

        spec.avionicsHits = critHits(controls.avionicsCrit);
        spec.fcsHits = critHits(controls.fcsCrit);
        spec.cicHits = critHits(controls.cicCrit);
        spec.gearHits = critHits(controls.gearCrit);
        spec.leftThrusterHits = critHits(controls.leftThrusterCrit);
        spec.rightThrusterHits = critHits(controls.rightThrusterCrit);
        spec.kfBoomHits = critHits(controls.kfBoomCrit);
        spec.dockCollarHits = critHits(controls.dockCollarCrit);
        spec.gravDeckHits = critHits(controls.gravDeckCrit);
        if (null != controls.bayDamage) {
            spec.bayCapacityRemaining = new Double[controls.bayDamage.length];
            for (int i = 0; i < controls.bayDamage.length; i++) {
                if (null != controls.bayDamage[i]) {
                    spec.bayCapacityRemaining[i] = ((Number) controls.bayDamage[i].getValue()).doubleValue();
                }
            }
        }
        spec.bayDoorHits = critHitsRow(controls.bayDoorCrit);
        spec.workingDockingCollars = numberValue(controls.collarDamage);
        spec.kfIntegrity = numberValue(controls.kfDamage);
        spec.chargingSystemHits = critHits(controls.chargingSystemCrit);
        spec.driveCoilHits = critHits(controls.driveCoilCrit);
        spec.driveControllerHits = critHits(controls.driveControllerCrit);
        spec.fieldInitiatorHits = critHits(controls.fieldInitiatorCrit);
        spec.heliumTankHits = critHits(controls.heliumTankCrit);
        spec.lfBatteryHits = critHits(controls.lfBatteryCrit);
        spec.sailIntegrity = numberValue(controls.sailDamage);

        spec.protoHits = critHitsRow(controls.protoCrits);

        return spec;
    }

    /** The values of a row of spinners; {@code null} for a missing row, with null elements for missing spinners. */
    private @Nullable Integer[] spinnerValues(@Nullable JSpinner[] spinners) {
        if (null == spinners) {
            return null;
        }
        Integer[] values = new Integer[spinners.length];
        for (int i = 0; i < spinners.length; i++) {
            values[i] = spinnerValue(spinners[i]);
        }
        return values;
    }

    /** A spinner's value; {@code null} for a missing spinner, which means the unit has nothing for it to edit. */
    private @Nullable Integer spinnerValue(@Nullable JSpinner spinner) {
        return (null == spinner) ? null : (Integer) spinner.getValue();
    }

    /** A spinner's value as an integer whatever its model's number type, for the models that hold doubles. */
    private @Nullable Integer numberValue(@Nullable JSpinner spinner) {
        return (null == spinner) ? null : ((Number) spinner.getValue()).intValue();
    }

    /** A checkbox's state; {@code null} for a missing checkbox, which means the unit has nothing for it to edit. */
    private @Nullable Boolean checkboxValue(@Nullable JCheckBox checkbox) {
        return (null == checkbox) ? null : checkbox.isSelected();
    }

    /** A crit control's hits; {@code null} for a missing control, which means the unit has no such system. */
    private @Nullable Integer critHits(@Nullable CheckCritPanel crit) {
        return (null == crit) ? null : crit.getHits();
    }

    /** The hits of a row of crit controls; {@code null} for a missing row, with null elements for missing crits. */
    private @Nullable Integer[] critHitsRow(@Nullable CheckCritPanel[] crits) {
        if (null == crits) {
            return null;
        }
        Integer[] hits = new Integer[crits.length];
        for (int i = 0; i < crits.length; i++) {
            hits[i] = critHits(crits[i]);
        }
        return hits;
    }
}
