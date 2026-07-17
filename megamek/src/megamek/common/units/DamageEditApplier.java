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
package megamek.common.units;

import java.util.Map;

import megamek.common.CriticalSlot;
import megamek.common.bays.ASFBay;
import megamek.common.bays.Bay;
import megamek.common.bays.SmallCraftBay;
import megamek.common.equipment.DockingCollar;
import megamek.common.equipment.EquipmentMode;
import megamek.common.equipment.EquipmentTypeLookup;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.logging.MMLogger;

/**
 * Writes a {@link DamageEditSpec} onto a unit. The damage editor builds the spec from its controls; in the lobby
 * (and outside a game, as in MekHQ) the dialog applies it to its local unit directly, while in play it sends the
 * spec to the server, which applies it here to its own authoritative copy of the unit. Both paths run this same
 * class, so an edit means the same thing wherever it is applied.
 */
public class DamageEditApplier {
    private static final MMLogger LOGGER = MMLogger.create(DamageEditApplier.class);

    private final Entity entity;
    private final DamageEditSpec spec;

    public DamageEditApplier(Entity entity, DamageEditSpec spec) {
        this.entity = entity;
        this.spec = spec;
    }

    /**
     * Applies the given number of total crits to a Super-Cooled Myomer, which is spread over 6 locations.
     */
    private void damageSCM(Entity entity, int equipmentNumber, int hits) {
        int numHits = 0;
        Mounted<?> mounted = entity.getEquipment(equipmentNumber);
        for (int location = 0; location < entity.locations(); location++) {
            for (int i = 0; i < entity.getNumberOfCriticalSlots(location); i++) {
                CriticalSlot criticalSlot = entity.getCritical(location, i);
                if ((criticalSlot == null) ||
                      (criticalSlot.getType() != CriticalSlot.TYPE_EQUIPMENT) ||
                      ((mounted != criticalSlot.getMount()) && (mounted != criticalSlot.getMount2()))) {
                    continue;
                }

                if (numHits < hits) {
                    criticalSlot.setHit(true);
                    criticalSlot.setDestroyed(true);
                    numHits++;
                } else {
                    criticalSlot.setHit(false);
                    criticalSlot.setDestroyed(false);
                    criticalSlot.setRepairable(true);
                }
            }
        }
    }

    public void applyToEntity() {
        for (int i = 0; i < entity.locations(); i++) {
            if ((null != spec.internal) && (null != spec.internal[i])) {
                int internal = spec.internal[i];
                if (internal <= 0) {
                    internal = IArmorState.ARMOR_DESTROYED;
                }
                if ((entity instanceof Aero) && (i == 0)) {
                    ((Aero) entity).setSI(internal);
                } else {
                    entity.setInternal(internal, i);
                }
            }
            if ((null != spec.armor) && (null != spec.armor[i])) {
                int armor = spec.armor[i];
                if (armor <= 0) {
                    armor = IArmorState.ARMOR_DESTROYED;
                }
                entity.setArmor(armor, i);
            }
            if (entity.hasRearArmor(i) && (null != spec.rearArmor) && (null != spec.rearArmor[i])) {
                int rear = spec.rearArmor[i];
                if (rear <= 0) {
                    rear = IArmorState.ARMOR_DESTROYED;
                }
                entity.setArmor(rear, i, true);
            }
        }
        for (Map.Entry<Integer, Integer> equipmentHit : spec.equipmentHits.entrySet()) {
            int equipmentNumber = equipmentHit.getKey();
            Mounted<?> mounted = entity.getEquipment(equipmentNumber);
            if (null == mounted) {
                continue;
            }
            int hits = equipmentHit.getValue();
            if (mounted.is(EquipmentTypeLookup.SCM)) {
                mounted.setDestroyed(hits >= 6);
                mounted.setHit(hits >= 6);
                damageSCM(entity, equipmentNumber, hits);
            } else {
                mounted.setDestroyed(hits > 0);
                mounted.setHit(hits > 0);
                entity.damageSystem(CriticalSlot.TYPE_EQUIPMENT, equipmentNumber, hits);
            }
        }
        if (entity instanceof ConvInfantry infantry) {
            infantry.damageOrRestoreFieldWeapons();
            entity.applyDamage();
        }

        // now systems
        if (entity instanceof Mek) {
            if (null != spec.centerEngineHits) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_ENGINE,
                      Mek.LOC_CENTER_TORSO,
                      spec.centerEngineHits);
            }
            if (null != spec.leftEngineHits) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_ENGINE,
                      Mek.LOC_LEFT_TORSO,
                      spec.leftEngineHits);
            }
            if (null != spec.rightEngineHits) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                      Mek.SYSTEM_ENGINE,
                      Mek.LOC_RIGHT_TORSO,
                      spec.rightEngineHits);
            }
            if (null != spec.gyroHits) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_GYRO, spec.gyroHits);
            }
            if (null != spec.sensorHits) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_SENSORS, spec.sensorHits);
            }
            if (null != spec.lifeSupportHits) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_LIFE_SUPPORT, spec.lifeSupportHits);
            }
            if (null != spec.cockpitHits) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, spec.cockpitHits);
            }
            for (Map.Entry<Integer, Integer> avionicsHit : spec.lamAvionicsHits.entrySet()) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                      LandAirMek.LAM_AVIONICS,
                      avionicsHit.getKey(),
                      avionicsHit.getValue());
            }
            for (Map.Entry<Integer, Integer> landingGearHit : spec.lamLandingGearHits.entrySet()) {
                entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                      LandAirMek.LAM_LANDING_GEAR,
                      landingGearHit.getKey(),
                      landingGearHit.getValue());
            }

            if (null != spec.actuatorHits) {
                for (int i = 0; i < spec.actuatorHits.length; i++) {
                    for (int j = 0; j < spec.actuatorHits[i].length; j++) {
                        Integer actuatorHit = spec.actuatorHits[i][j];
                        if (null == actuatorHit) {
                            continue;
                        }
                        int location = i + Mek.LOC_RIGHT_ARM;
                        int actuator = j + Mek.ACTUATOR_SHOULDER;
                        if ((location >= Mek.LOC_RIGHT_LEG) || (entity instanceof QuadMek)) {
                            actuator = j + Mek.ACTUATOR_HIP;
                        }
                        entity.damageSystem(CriticalSlot.TYPE_SYSTEM, actuator, location, actuatorHit);
                    }

                    if (entity instanceof QuadVee) {
                        // A leg carries one conversion gear, so it is applied once. This used to be written as a
                        // loop that ran four times over the same control, applying the same hits to the same gear.
                        Integer conversionGearHit = spec.actuatorHits[i][DamageEditSpec.CONVERSION_GEAR_INDEX];
                        if (null != conversionGearHit) {
                            entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                                  QuadVee.SYSTEM_CONVERSION_GEAR,
                                  i + Mek.LOC_RIGHT_ARM,
                                  conversionGearHit);
                        }
                    }
                }
            }
        } else if (entity instanceof ProtoMek) {
            if (null != spec.protoHits) {
                for (int location = 0; location < entity.locations(); location++) {
                    if (null == spec.protoHits[location]) {
                        continue;
                    }
                    if ((location == ProtoMek.LOC_LEFT_ARM) || (location == ProtoMek.LOC_RIGHT_ARM)) {
                        entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                              ProtoMek.SYSTEM_ARM_CRIT,
                              location,
                              spec.protoHits[location]);
                    }
                    if (location == ProtoMek.LOC_LEG) {
                        entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                              ProtoMek.SYSTEM_LEG_CRIT,
                              location,
                              spec.protoHits[location]);
                    }
                    if (location == ProtoMek.LOC_HEAD) {
                        entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                              ProtoMek.SYSTEM_HEAD_CRIT,
                              location,
                              spec.protoHits[location]);
                    }
                    if (location == ProtoMek.LOC_TORSO) {
                        entity.damageSystem(CriticalSlot.TYPE_SYSTEM,
                              ProtoMek.SYSTEM_TORSO_CRIT,
                              location,
                              spec.protoHits[location]);
                    }
                }
            }
        } else if (entity instanceof Tank tank) {
            if (null != spec.engineHits) {
                if (spec.engineHits > 0) {
                    tank.engineHit();
                } else {
                    tank.engineFix();
                }
            }
            if (null != spec.turretLockHits) {
                if (spec.turretLockHits > 0) {
                    tank.lockTurret(0);
                } else {
                    tank.unlockTurret();
                }
            }
            if (null != spec.sensorHits) {
                tank.setSensorHits(spec.sensorHits);
            }
            if (null != spec.motiveHits) {
                tank.resetMovementDamage();
                tank.addMovementDamage(spec.motiveHits);

                // Apply movement damage immediately in case we've decided to immobilize the
                // tank
                tank.applyMovementDamage();
            }
            if ((tank instanceof VTOL) && (null != spec.flightStabilizerHits)) {
                if (spec.flightStabilizerHits > 0) {
                    tank.setStabiliserHit(VTOL.LOC_ROTOR);
                } else {
                    tank.clearStabiliserHit(VTOL.LOC_ROTOR);
                }
            }
            if (null != spec.stabilizerHits) {
                for (int location = 0; location < tank.locations(); location++) {
                    Integer stabilizerHit = spec.stabilizerHits[location];
                    if (null == stabilizerHit) {
                        continue;
                    }
                    if (stabilizerHit > 0) {
                        tank.setStabiliserHit(location);
                    } else {
                        tank.clearStabiliserHit(location);
                    }
                }
            }
        } else if (entity instanceof Aero aero) {
            if (null != spec.avionicsHits) {
                aero.setAvionicsHits(spec.avionicsHits);
            }
            if (null != spec.fcsHits) {
                aero.setFCSHits(spec.fcsHits);
            }
            if (null != spec.cicHits) {
                aero.setCICHits(spec.cicHits);
            }
            if (null != spec.engineHits) {
                aero.setEngineHits(spec.engineHits);
            }
            if (null != spec.sensorHits) {
                aero.setSensorHits(spec.sensorHits);
            }
            if (null != spec.gearHits) {
                aero.setGearHit(spec.gearHits > 0);
            }
            if (null != spec.lifeSupportHits) {
                aero.setLifeSupport(spec.lifeSupportHits == 0);
            }
            if (null != spec.leftThrusterHits) {
                aero.setLeftThrustHits(spec.leftThrusterHits);
            }
            if (null != spec.rightThrusterHits) {
                aero.setRightThrustHits(spec.rightThrusterHits);
            }
            if ((null != spec.dockCollarHits) && (aero instanceof Dropship)) {
                ((Dropship) aero).setDamageDockCollar(spec.dockCollarHits > 0);
            }
            if ((null != spec.kfBoomHits) && (aero instanceof Dropship)) {
                ((Dropship) aero).setDamageKFBoom(spec.kfBoomHits > 0);
            }
            // cargo bays and bay doors
            if (((aero instanceof Dropship) || (aero instanceof Jumpship)) && (null != spec.bayCapacityRemaining)) {
                int b = 0;
                for (Bay bay : aero.getTransportBays()) {
                    Double bayCapacity = spec.bayCapacityRemaining[b];
                    if (null == bayCapacity) {
                        continue;
                    }
                    bay.setBayDamage(bay.getCapacity() - bayCapacity);
                    Integer doorHits = spec.bayDoorHits[b];
                    if (null == doorHits) {
                        continue;
                    }
                    if ((bay.getCurrentDoors() > 0) && (doorHits > 0)) {
                        bay.setCurrentDoors(bay.getDoors() - doorHits);

                    } else if (doorHits == 0) {
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
                double damagedCollars = 0.0;
                int damagedDecks = 0;
                if (null != spec.workingDockingCollars) {
                    damagedCollars = aero.getDockingCollars().size() - (double) spec.workingDockingCollars;
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
                if (null != spec.gravDeckHits) {
                    damagedDecks = spec.gravDeckHits;
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
                if (null != spec.kfIntegrity) {
                    jumpship.setKFIntegrity(spec.kfIntegrity);
                }
                if (null != spec.chargingSystemHits) {
                    jumpship.setKFChargingSystemHit(spec.chargingSystemHits > 0);
                }
                if (null != spec.driveCoilHits) {
                    jumpship.setKFDriveCoilHit(spec.driveCoilHits > 0);
                }
                if (null != spec.driveControllerHits) {
                    jumpship.setKFDriveControllerHit(spec.driveControllerHits > 0);
                }
                if (null != spec.fieldInitiatorHits) {
                    jumpship.setKFFieldInitiatorHit(spec.fieldInitiatorHits > 0);
                }
                if (null != spec.heliumTankHits) {
                    jumpship.setKFHeliumTankHit(spec.heliumTankHits > 0);
                }
                if (null != spec.lfBatteryHits) {
                    jumpship.setLFBatteryHit(spec.lfBatteryHits > 0);
                }
                if (null != spec.sailIntegrity) {
                    jumpship.setSailIntegrity(spec.sailIntegrity);
                }
            }
        }

        applyCrewHits();
        applySkillModifiers();
        applyHeat();
        applyAmmoShots();
        applyEquipmentSettings();
        applyStatus();
        logAppliedEdits();
    }

    /**
     * Writes the gamemaster's per-equipment settings back: burst fire on single machine guns and hot-loading on
     * single ammo bins, so a game can start without them and have a gamemaster switch them per launcher in play.
     */
    private void applyEquipmentSettings() {
        for (Map.Entry<Integer, Boolean> mgBurst : spec.mgBurst.entrySet()) {
            Mounted<?> machineGun = entity.getEquipment(mgBurst.getKey());
            if (machineGun != null) {
                machineGun.setRapidFire(mgBurst.getValue());
            }
        }
        for (Map.Entry<Integer, Boolean> hotLoaded : spec.hotLoadedAmmo.entrySet()) {
            Mounted<?> ammoBin = entity.getEquipment(hotLoaded.getKey());
            if (ammoBin == null) {
                continue;
            }
            // the same call pair the lobby uses: setHotLoad tracks the state, the mode is what the rules read
            ammoBin.setHotLoad(hotLoaded.getValue());
            if (hotLoaded.getValue()) {
                ammoBin.setMode("HotLoad");
            } else if (ammoBin.hasModeType("HotLoad")) {
                ammoBin.setMode("");
            }
        }
    }

    /**
     * Logs what the spec wrote onto the unit, and on which side it was applied: on the server this is the edit
     * taking effect, on a client it is a local apply that still has to be sent somewhere.
     */
    private void logAppliedEdits() {
        StringBuilder summary = new StringBuilder();
        for (int location = 0; location < entity.locations(); location++) {
            if ((null == spec.armor) || (null == spec.armor[location])) {
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
        if ((null == crew) || (null == spec.crewHits)) {
            return;
        }
        for (int slot = 0; slot < spec.crewHits.length; slot++) {
            if (null == spec.crewHits[slot]) {
                continue;
            }
            // Revive the crew member first, then set the hits. Fewer than six revives a wounded or dead member;
            // six re-kills it, since setHits marks a member with the killing number of hits dead again.
            crew.setDead(false, slot);
            crew.setUnconscious(false, slot);
            crew.setKoThisRound(false, slot);
            crew.setHits(spec.crewHits[slot], slot);
        }
    }

    /**
     * Writes the gamemaster's temporary skill modifiers back to the crew, through the same state object the
     * /skillMod command sets. Each modifier carries its own duration, and a delta at zero clears its modifier,
     * which is also how a gamemaster takes a change back before it runs out.
     */
    private void applySkillModifiers() {
        Crew crew = entity.getCrew();
        if ((null == crew) || (null == spec.gunneryModifier)) {
            return;
        }
        TemporarySkillModifiers modifiers = crew.getSkillModifiers();
        modifiers.setGunnery(spec.gunneryModifier,
              spec.gunneryPermanent ? TemporarySkillModifiers.PERMANENT : spec.gunneryRounds);
        modifiers.setPiloting(spec.pilotingModifier,
              spec.pilotingPermanent ? TemporarySkillModifiers.PERMANENT : spec.pilotingRounds);
        // absent where the editor had no initiative row, which is any game without individual initiative;
        // an active initiative modifier is left alone there rather than silently cleared
        if (null != spec.initiativeModifier) {
            modifiers.setInitiative(spec.initiativeModifier,
                  spec.initiativePermanent ? TemporarySkillModifiers.PERMANENT : spec.initiativeRounds);
        }
    }

    /**
     * Writes the unit's conditions back: shut down, prone, hidden and so on. These use the same calls the Configure
     * dialog uses, so that a unit shut down here is shut down the same way as one shut down there.
     */
    private void applyStatus() {
        if (null != spec.shutdown) {
            if (spec.shutdown) {
                entity.performManualShutdown();
            } else {
                entity.performManualStartup();
            }
        }
        if (null != spec.prone) {
            entity.setProne(spec.prone);
        }
        if (null != spec.hullDown) {
            entity.setHullDown(spec.hullDown);
        }
        if (null != spec.hidden) {
            entity.setHidden(spec.hidden);
        }
        if (null != spec.stealth) {
            setStealth(spec.stealth);
        }
        if ((null != spec.dugIn) && (entity instanceof Infantry infantry)) {
            infantry.setDugIn(spec.dugIn ? Infantry.DUG_IN_COMPLETE : Infantry.DUG_IN_NONE);
        }
        if ((null != spec.fuel) && (entity instanceof Aero aero)) {
            aero.setCurrentFuel(spec.fuel);
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
        for (Map.Entry<Integer, Integer> ammoShots : spec.ammoShots.entrySet()) {
            Mounted<?> ammoBin = entity.getEquipment(ammoShots.getKey());
            if (ammoBin != null) {
                ammoBin.setShotsLeft(ammoShots.getValue());
            }
        }
    }

    private void applyHeat() {
        if (null != spec.heat) {
            entity.heat = spec.heat;
        }
    }

}
