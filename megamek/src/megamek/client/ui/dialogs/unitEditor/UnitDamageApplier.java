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
import javax.swing.JSpinner;

import megamek.common.CriticalSlot;
import megamek.common.bays.ASFBay;
import megamek.common.bays.Bay;
import megamek.common.bays.SmallCraftBay;
import megamek.common.equipment.DockingCollar;
import megamek.common.equipment.EquipmentMode;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.Mounted;
import megamek.common.units.*;
import megamek.logging.MMLogger;

/**
 * Writes the values in the damage editor's controls back onto the unit. This runs when the user presses Okay;
 * until then the unit is untouched, so cancelling the dialog leaves it as it was.
 */
public class UnitDamageApplier {
    private static final MMLogger LOGGER = MMLogger.create(UnitDamageApplier.class);

    private final Entity entity;
    private final UnitDamageControls controls;

    public UnitDamageApplier(Entity entity, UnitDamageControls controls) {
        this.entity = entity;
        this.controls = controls;
    }

    private void damageSCM(Entity entity, int eqNum, int hits) {
        int numHits = 0;
        Mounted<?> m = entity.getEquipment(eqNum);
        for (int loc = 0; loc < entity.locations(); loc++) {
            for (int i = 0; i < entity.getNumberOfCriticalSlots(loc); i++) {
                CriticalSlot cs = entity.getCritical(loc, i);
                if ((cs == null) ||
                      (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) ||
                      ((m != cs.getMount()) && (m != cs.getMount2()))) {
                    continue;
                }

                if (numHits < hits) {
                    cs.setHit(true);
                    cs.setDestroyed(true);
                    numHits++;
                } else {
                    cs.setHit(false);
                    cs.setDestroyed(false);
                    cs.setRepairable(true);
                }
            }
        }
    }

    public void applyToEntity() {
        for (int i = 0; i < entity.locations(); i++) {
            if (null != controls.spnInternal[i]) {
                int internal = (Integer) controls.spnInternal[i].getModel().getValue();
                if (internal <= 0) {
                    internal = IArmorState.ARMOR_DESTROYED;
                }
                if ((entity instanceof Aero) && (i == 0)) {
                    ((Aero) entity).setSI(internal);
                } else {
                    entity.setInternal(internal, i);
                }
            }
            if (null != controls.spnArmor[i]) {
                int armor = (Integer) controls.spnArmor[i].getModel().getValue();
                if (armor <= 0) {
                    armor = IArmorState.ARMOR_DESTROYED;
                }
                entity.setArmor(armor, i);
            }
            if (entity.hasRearArmor(i) && (null != controls.spnRear[i])) {
                int rear = (Integer) controls.spnRear[i].getModel().getValue();
                if (rear <= 0) {
                    rear = IArmorState.ARMOR_DESTROYED;
                }
                entity.setArmor(rear, i, true);
            }
        }
        for (Mounted<?> m : entity.getEquipment()) {
            int eqNum = entity.getEquipmentNum(m);
            CheckCritPanel crit = controls.equipCrits.get(eqNum);
            if (null != crit) {
                int hits = crit.getHits();
                if (m.is(EquipmentTypeLookup.SCM)) {
                    m.setDestroyed(hits >= 6);
                    m.setHit(hits >= 6);
                    damageSCM(entity, eqNum, hits);
                } else {
                    m.setDestroyed(hits > 0);
                    m.setHit(hits > 0);
                    entity.damageSystem(CriticalSlot.TYPE_EQUIPMENT, eqNum, hits);
                }
            }
        }
        if (entity instanceof ConvInfantry infantry) {
            infantry.damageOrRestoreFieldWeapons();
            entity.applyDamage();
        }

        // now systems
        if (entity instanceof Mek) {
            if (null != controls.centerEngineCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_ENGINE,
                      Mek.LOC_CENTER_TORSO,
                      controls.centerEngineCrit.getHits());
            }
            if (null != controls.leftEngineCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_ENGINE,
                      Mek.LOC_LEFT_TORSO,
                      controls.leftEngineCrit.getHits());
            }
            if (null != controls.rightEngineCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_ENGINE,
                      Mek.LOC_RIGHT_TORSO,
                      controls.rightEngineCrit.getHits());
            }
            if (null != controls.gyroCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, controls.gyroCrit.getHits());
            }
            if (null != controls.sensorCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, controls.sensorCrit.getHits());
            }
            if (null != controls.lifeSupportCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, controls.lifeSupportCrit.getHits());
            }
            if (null != controls.cockpitCrit) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, controls.cockpitCrit.getHits());
            }
            if (null != controls.lamAvionicsCrit && !controls.lamAvionicsCrit.isEmpty()) {
                for (int loc : controls.lamAvionicsCrit.keySet()) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          LandAirMek.LAM_AVIONICS,
                          loc,
                          controls.lamAvionicsCrit.get(loc).getHits());
                }
            }
            if (null != controls.lamLandingGearCrit && !controls.lamLandingGearCrit.isEmpty()) {
                for (int loc : controls.lamLandingGearCrit.keySet()) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          LandAirMek.LAM_LANDING_GEAR,
                          loc,
                          controls.lamLandingGearCrit.get(loc).getHits());
                }
            }

            for (int i = 0; i < controls.actuatorCrits.length; i++) {
                for (int j = 0; j < controls.actuatorCrits[i].length; j++) {
                    CheckCritPanel actuatorCrit = controls.actuatorCrits[i][j];
                    if (null == actuatorCrit) {
                        continue;
                    }
                    int loc = i + Mek.LOC_RIGHT_ARM;
                    int actuator = j + Mek.ACTUATOR_SHOULDER;
                    if ((loc >= Mek.LOC_RIGHT_LEG) || (entity instanceof QuadMek)) {
                        actuator = j + Mek.ACTUATOR_HIP;
                    }
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM, actuator, loc, actuatorCrit.getHits());
                }

                if (entity instanceof QuadVee) {
                    for (int j = 0; j < controls.actuatorCrits.length; j++) {
                        CheckCritPanel actuatorCrit = controls.actuatorCrits[i][4];
                        if (null == actuatorCrit) {
                            continue;
                        }
                        int loc = i + Mek.LOC_RIGHT_ARM;
                        int actuator = QuadVee.SYSTEM_CONVERSION_GEAR;
                        entity.damageSystem(CriticalSlot.TYPE_SYSTEM, actuator, loc, actuatorCrit.getHits());
                    }
                }
            }
        } else if (entity instanceof ProtoMek) {
            for (int loc = 0; loc < entity.locations(); loc++) {
                if (null == controls.protoCrits[loc]) {
                    continue;
                }
                if ((loc == ProtoMek.LOC_LEFT_ARM) || (loc == ProtoMek.LOC_RIGHT_ARM)) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          ProtoMek.SYSTEM_ARM_CRIT,
                          loc,
                          controls.protoCrits[loc].getHits());
                }
                if (loc == ProtoMek.LOC_LEG) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          ProtoMek.SYSTEM_LEG_CRIT,
                          loc,
                          controls.protoCrits[loc].getHits());
                }
                if (loc == ProtoMek.LOC_HEAD) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          ProtoMek.SYSTEM_HEAD_CRIT,
                          loc,
                          controls.protoCrits[loc].getHits());
                }
                if (loc == ProtoMek.LOC_TORSO) {
                    entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                          ProtoMek.SYSTEM_TORSO_CRIT,
                          loc,
                          controls.protoCrits[loc].getHits());
                }
            }
        } else if (entity instanceof Tank tank) {
            if (null != controls.engineCrit) {
                if (controls.engineCrit.getHits() > 0) {
                    tank.engineHit();
                } else {
                    tank.engineFix();
                }
            }
            if (null != controls.turretLockCrit) {
                if (controls.turretLockCrit.getHits() > 0) {
                    tank.lockTurret(0);
                } else {
                    tank.unlockTurret();
                }
            }
            if (null != controls.sensorCrit) {
                tank.setSensorHits(controls.sensorCrit.getHits());
            }
            if (null != controls.motiveCrit) {
                tank.resetMovementDamage();
                tank.addMovementDamage(controls.motiveCrit.getHits());

                // Apply movement damage immediately in case we've decided to immobilize the
                // tank
                tank.applyMovementDamage();
            }
            if ((tank instanceof VTOL) && (null != controls.flightStabilizerCrit)) {
                if (controls.flightStabilizerCrit.getHits() > 0) {
                    tank.setStabiliserHit(VTOL.LOC_ROTOR);
                } else {
                    tank.clearStabiliserHit(VTOL.LOC_ROTOR);
                }
            }
            for (int loc = 0; loc < tank.locations(); loc++) {
                CheckCritPanel stabCrit = controls.stabilizerCrits[loc];
                if (null == stabCrit) {
                    continue;
                }
                if (stabCrit.getHits() > 0) {
                    tank.setStabiliserHit(loc);
                } else {
                    tank.clearStabiliserHit(loc);
                }
            }
        } else if (entity instanceof Aero aero) {
            if (null != controls.avionicsCrit) {
                aero.setAvionicsHits(controls.avionicsCrit.getHits());
            }
            if (null != controls.fcsCrit) {
                aero.setFCSHits(controls.fcsCrit.getHits());
            }
            if (null != controls.cicCrit) {
                aero.setCICHits(controls.cicCrit.getHits());
            }
            if (null != controls.engineCrit) {
                aero.setEngineHits(controls.engineCrit.getHits());
            }
            if (null != controls.sensorCrit) {
                aero.setSensorHits(controls.sensorCrit.getHits());
            }
            if (null != controls.gearCrit) {
                aero.setGearHit(controls.gearCrit.getHits() > 0);
            }
            if (null != controls.lifeSupportCrit) {
                aero.setLifeSupport(controls.lifeSupportCrit.getHits() == 0);
            }
            if (null != controls.leftThrusterCrit) {
                aero.setLeftThrustHits(controls.leftThrusterCrit.getHits());
            }
            if (null != controls.rightThrusterCrit) {
                aero.setRightThrustHits(controls.rightThrusterCrit.getHits());
            }
            if ((null != controls.dockCollarCrit) && (aero instanceof Dropship)) {
                ((Dropship) aero).setDamageDockCollar(controls.dockCollarCrit.getHits() > 0);
            }
            if ((null != controls.kfBoomCrit) && (aero instanceof Dropship)) {
                ((Dropship) aero).setDamageKFBoom(controls.kfBoomCrit.getHits() > 0);
            }
            // cargo bays and bay doors
            if ((aero instanceof Dropship) || (aero instanceof Jumpship)) {
                int b = 0;
                for (Bay bay : aero.getTransportBays()) {
                    JSpinner bayCrit = controls.bayDamage[b];
                    if (null == bayCrit) {
                        continue;
                    }
                    bay.setBayDamage(bay.getCapacity() - (Double) bayCrit.getModel().getValue());
                    CheckCritPanel doorCrit = controls.bayDoorCrit[b];
                    if (null == doorCrit) {
                        continue;
                    }
                    if ((bay.getCurrentDoors() > 0) && (doorCrit.getHits() > 0)) {
                        bay.setCurrentDoors(bay.getDoors() - doorCrit.getHits());

                    } else if (doorCrit.getHits() == 0) {
                        bay.setCurrentDoors(bay.getDoors());
                    }
                    // for ASF and SC bays, we have to update recovery slots as doors are changed
                    if (bay instanceof ASFBay asfBay) {
                        asfBay.initializeRecoverySlots();
                    }
                    if (bay instanceof SmallCraftBay smallCraftBay) {
                        smallCraftBay.initializeRecoverySlots();
                    }
                    b++;
                }
            }
            // Jumpship Docking Collars, KF Drive, Sail and Grav Decks
            if (aero instanceof Jumpship jumpship) {
                JSpinner collarCrit = controls.collarDamage;
                CheckCritPanel deckCrit = controls.gravDeckCrit;
                double damagedCollars = 0.0;
                int damagedDecks = 0;
                if (null != collarCrit) {
                    damagedCollars = (aero.getDockingCollars().size() - (double) collarCrit.getModel().getValue());
                }
                // First, reset damaged collars to undamaged. Otherwise, you get weirdness when
                // running this dialogue multiple times
                for (DockingCollar collar : aero.getDockingCollars()) {
                    collar.setDamaged(false);
                }
                // Otherwise, run through the list and damage one until the spinner value is
                // satisfied
                for (DockingCollar collar : aero.getDockingCollars()) {
                    if (damagedCollars <= 0) {
                        break;
                    }
                    collar.setDamaged(true);
                    damagedCollars--;
                }
                if (null != deckCrit) {
                    damagedDecks = deckCrit.getHits();
                }
                // reset all grav decks to undamaged
                for (int i = 0; i < jumpship.getTotalGravDeck(); i++) {
                    jumpship.setGravDeckDamageFlag(i, 0);
                }
                if (damagedDecks > 0) {
                    // loop through the grav decks from #1 and damage them
                    for (int i = 0; i < damagedDecks; i++) {
                        jumpship.setGravDeckDamageFlag(i, 1);
                    }
                }
                // KF Drive and Sail
                if (null != controls.kfDamage) {
                    double kfIntegrity = (double) controls.kfDamage.getModel().getValue();
                    jumpship.setKFIntegrity((int) kfIntegrity);
                }
                if (null != controls.chargingSystemCrit) {
                    jumpship.setKFChargingSystemHit(controls.chargingSystemCrit.getHits() > 0);
                }
                if (null != controls.driveCoilCrit) {
                    jumpship.setKFDriveCoilHit(controls.driveCoilCrit.getHits() > 0);
                }
                if (null != controls.driveControllerCrit) {
                    jumpship.setKFDriveControllerHit(controls.driveControllerCrit.getHits() > 0);
                }
                if (null != controls.fieldInitiatorCrit) {
                    jumpship.setKFFieldInitiatorHit(controls.fieldInitiatorCrit.getHits() > 0);
                }
                if (null != controls.heliumTankCrit) {
                    jumpship.setKFHeliumTankHit(controls.heliumTankCrit.getHits() > 0);
                }
                if (null != controls.lfBatteryCrit) {
                    jumpship.setLFBatteryHit(controls.lfBatteryCrit.getHits() > 0);
                }
                if (null != controls.sailDamage) {
                    double sailIntegrity = (double) controls.sailDamage.getModel().getValue();
                    jumpship.setSailIntegrity((int) sailIntegrity);
                }
            }
        }

        applyCrewHits();
        applyHeat();
        applyAmmoShots();
        applyStatus();
        logAppliedEdits();
    }

    /**
     * Logs what the dialog wrote into the unit. The unit still has to reach the server, which drops updates it does
     * not accept, so a log of what was applied here tells apart an edit that never happened from one that was
     * refused on the way.
     */
    private void logAppliedEdits() {
        StringBuilder summary = new StringBuilder();
        for (int location = 0; location < entity.locations(); location++) {
            if (null == controls.spnArmor[location]) {
                continue;
            }
            summary.append(' ')
                  .append(entity.getLocationAbbr(location))
                  .append(':')
                  .append(entity.getInternal(location))
                  .append('/')
                  .append(entity.getArmor(location, false));
            if (entity.hasRearArmor(location)) {
                summary.append('/').append(entity.getArmor(location, true));
            }
        }
        LOGGER.info("Applied damage edits to {} (id {}): heat {}, crew hits {}, destroyed {}, structure/armor{}",
              entity.getDisplayName(),
              entity.getId(),
              entity.heat,
              (entity.getCrew() == null) ? "none" : entity.getCrew().getHits(),
              entity.isDestroyed(),
              summary);
    }

    /**
     * Writes the crew hits back to the unit. A crew member who is brought back below six hits is revived, which
     * {@link Crew#setHits} does not do on its own: it kills at six hits but never undoes it, and a dead crew
     * member would otherwise stay dead however far their hits were lowered.
     */
    private void applyCrewHits() {
        Crew crew = entity.getCrew();
        if ((null == crew) || (null == controls.spnCrewHits)) {
            return;
        }
        for (int slot = 0; slot < controls.spnCrewHits.length; slot++) {
            if (null == controls.spnCrewHits[slot]) {
                continue;
            }
            // the control cannot reach the six hits that kill, so every value here revives a dead crew member
            crew.setDead(false, slot);
            crew.setUnconscious(false, slot);
            crew.setKoThisRound(false, slot);
            crew.setHits((Integer) controls.spnCrewHits[slot].getValue(), slot);
        }
    }

    /**
     * Writes the unit's conditions back: shut down, prone, hidden and so on. These use the same calls the Configure
     * dialog uses, so that a unit shut down here is shut down the same way as one shut down there.
     */
    private void applyStatus() {
        if (null != controls.chkShutdown) {
            if (controls.chkShutdown.isSelected()) {
                entity.performManualShutdown();
            } else {
                entity.performManualStartup();
            }
        }
        if (null != controls.chkProne) {
            entity.setProne(controls.chkProne.isSelected());
        }
        if (null != controls.chkHullDown) {
            entity.setHullDown(controls.chkHullDown.isSelected());
        }
        if (null != controls.chkHidden) {
            entity.setHidden(controls.chkHidden.isSelected());
        }
        if (null != controls.chkStealth) {
            setStealth(controls.chkStealth.isSelected());
        }
        if ((null != controls.chkDugIn) && (entity instanceof Infantry infantry)) {
            infantry.setDugIn(controls.chkDugIn.isSelected() ? Infantry.DUG_IN_COMPLETE : Infantry.DUG_IN_NONE);
        }
        if ((null != controls.spnFuel) && (entity instanceof Aero aero)) {
            aero.setCurrentFuel((Integer) controls.spnFuel.getValue());
        }
    }

    /** Switches the unit's stealth armor on or off, which means every piece of stealth equipment it carries. */
    private void setStealth(boolean stealthOn) {
        int stealthMode = stealthOn ? 1 : 0;
        EquipmentMode newMode = EquipmentMode.getMode(stealthOn ? "On" : "Off");
        for (MiscMounted stealth : entity.getMiscEquipment(MiscType.F_STEALTH)) {
            if (!newMode.equals(stealth.curMode())) {
                stealth.setMode(stealthMode);
            }
        }
    }

    /**
     * Refills the ammo bins to what the gamemaster set. This runs after the equipment crits, so a bin whose crit
     * was taken off is a working bin again by the time its shots are put back into it.
     */
    private void applyAmmoShots() {
        for (Map.Entry<Integer, JSpinner> ammoShots : controls.ammoShots.entrySet()) {
            Mounted<?> ammoBin = entity.getEquipment(ammoShots.getKey());
            if (ammoBin != null) {
                ammoBin.setShotsLeft((Integer) ammoShots.getValue().getValue());
            }
        }
    }

    private void applyHeat() {
        if (null != controls.spnHeat) {
            entity.heat = (Integer) controls.spnHeat.getValue();
        }
    }

}
