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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.CriticalSlot;
import megamek.common.enums.ChargeLevel;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.exceptions.LocationFullException;
import megamek.common.game.Game;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.Weapon;
import megamek.testUtilities.MMTestUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DamageEditApplier} writing a {@link DamageEditSpec} onto a unit: the values the spec carries
 * land on the unit, and the values it does not carry leave the unit alone.
 */
class DamageEditApplierTest {

    private Entity mek;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        mek = MMTestUtilities.getEntityForUnitTesting("Atlas AS7-D", false);
        assertNotNull(mek, "Test unit could not be loaded");
    }

    /** A spec with location arrays sized for the unit and everything else absent. */
    private DamageEditSpec emptySpec() {
        DamageEditSpec spec = new DamageEditSpec();
        spec.entityId = mek.getId();
        spec.internal = new Integer[mek.locations()];
        spec.armor = new Integer[mek.locations()];
        spec.rearArmor = new Integer[mek.locations()];
        return spec;
    }

    private void apply(DamageEditSpec spec) {
        new DamageEditApplier(mek, spec).applyToEntity();
    }

    @Test
    void armorValuesLandOnTheirLocations() {
        DamageEditSpec spec = emptySpec();
        spec.armor[Mek.LOC_LEFT_ARM] = 3;
        spec.rearArmor[Mek.LOC_CENTER_TORSO] = 2;

        apply(spec);

        assertEquals(3, mek.getArmor(Mek.LOC_LEFT_ARM));
        assertEquals(2, mek.getArmor(Mek.LOC_CENTER_TORSO, true));
    }

    @Test
    void zeroInternalStructureDestroysTheLocation() {
        DamageEditSpec spec = emptySpec();
        spec.internal[Mek.LOC_LEFT_LEG] = 0;

        apply(spec);

        assertEquals(IArmorState.ARMOR_DESTROYED, mek.getInternal(Mek.LOC_LEFT_LEG));
    }

    @Test
    void restoringStructureBringsBackABlownOffLimb() {
        int location = Mek.LOC_LEFT_ARM;
        mek.destroyLocation(location, true);
        assertTrue(mek.isLocationBlownOff(location));
        assertTrue(mek.getInternal(location) < 0,
              "A blown-off location reads as gone whatever its structure value");

        DamageEditSpec spec = emptySpec();
        spec.internal[location] = mek.getOInternal(location);
        spec.armor[location] = mek.getOArmor(location);
        apply(spec);

        assertFalse(mek.isLocationBlownOff(location), "The limb is back on");
        assertFalse(mek.isLocationBad(location));
        assertEquals(mek.getOInternal(location), mek.getInternal(location));
        assertEquals(mek.getOArmor(location), mek.getArmor(location));
        for (int slot = 0; slot < mek.getNumberOfCriticalSlots(location); slot++) {
            CriticalSlot criticalSlot = mek.getCritical(location, slot);
            assertTrue((criticalSlot == null) || !criticalSlot.isMissing(), "No slot of the limb stays missing");
        }
        for (Mounted<?> mounted : mek.getEquipment()) {
            if (mounted.getLocation() == location) {
                assertFalse(mounted.isMissing(), mounted.getName() + " is back with the limb");
            }
        }
    }

    @Test
    void restoringStructureBringsBackADestroyedLocation() {
        int location = Mek.LOC_LEFT_TORSO;
        mek.destroyLocation(location, false);
        assertTrue(mek.getInternal(location) < 0, "The location is destroyed");

        DamageEditSpec spec = emptySpec();
        spec.internal[location] = mek.getOInternal(location);
        apply(spec);

        assertEquals(mek.getOInternal(location), mek.getInternal(location));
        assertFalse(mek.isLocationBad(location));
    }

    @Test
    void absentValuesLeaveTheUnitAlone() {
        int armorBefore = mek.getArmor(Mek.LOC_RIGHT_ARM);
        int heatBefore = mek.heat;

        apply(emptySpec());

        assertEquals(armorBefore, mek.getArmor(Mek.LOC_RIGHT_ARM));
        assertEquals(heatBefore, mek.heat);
    }

    @Test
    void heatLandsOnTheUnit() {
        DamageEditSpec spec = emptySpec();
        spec.heat = 12;

        apply(spec);

        assertEquals(12, mek.heat);
    }

    @Test
    void loweringCrewHitsRevivesADeadCrewMember() {
        Crew crew = mek.getCrew();
        crew.setHits(6, 0);
        assertTrue(crew.isDead());

        DamageEditSpec spec = emptySpec();
        spec.crewHits = new Integer[] { 2 };
        apply(spec);

        assertFalse(crew.isDead());
        assertEquals(2, crew.getHits());
    }

    @Test
    void skillModifiersLandOnTheCrewEachWithItsOwnDuration() {
        int baseGunnery = mek.getCrew().getGunnery();
        int basePiloting = mek.getCrew().getPiloting();

        DamageEditSpec spec = emptySpec();
        spec.gunneryModifier = 1;
        spec.gunneryRounds = 3;
        spec.pilotingModifier = -1;
        spec.pilotingRounds = 1;
        spec.pilotingPermanent = true;
        spec.initiativeModifier = 0;
        spec.initiativeRounds = 3;
        apply(spec);

        assertEquals(baseGunnery + 1, mek.getCrew().getGunnery());
        assertEquals(basePiloting - 1, mek.getCrew().getPiloting());
        assertEquals(3, mek.getCrew().getSkillModifiers().getGunneryRounds());
        assertEquals(TemporarySkillModifiers.PERMANENT, mek.getCrew().getSkillModifiers().getPilotingRounds());
        assertEquals(0, mek.getCrew().getSkillModifiers().getInitiativeRounds(),
              "a zero delta must not start an initiative modifier");
    }

    @Test
    void zeroedSkillModifiersClearAnActiveModifier() {
        mek.getCrew().getSkillModifiers().set(2, 0, 0, TemporarySkillModifiers.PERMANENT);

        DamageEditSpec spec = emptySpec();
        spec.gunneryModifier = 0;
        spec.gunneryRounds = 3;
        spec.pilotingModifier = 0;
        spec.pilotingRounds = 3;
        spec.initiativeModifier = 0;
        spec.initiativeRounds = 3;
        apply(spec);

        assertFalse(mek.getCrew().getSkillModifiers().isActive());
    }

    @Test
    void equipmentCritDestroysTheEquipment() {
        Mounted<?> weapon = mek.getWeaponList().get(0);
        int equipmentNumber = mek.getEquipmentNum(weapon);

        DamageEditSpec spec = emptySpec();
        spec.equipmentHits.put(equipmentNumber, 1);
        apply(spec);

        assertTrue(weapon.isDestroyed());
    }

    @Test
    void removingAnEquipmentCritRepairsTheEquipment() {
        Mounted<?> weapon = mek.getWeaponList().get(0);
        int equipmentNumber = mek.getEquipmentNum(weapon);

        DamageEditSpec damage = emptySpec();
        damage.equipmentHits.put(equipmentNumber, 1);
        apply(damage);

        DamageEditSpec repair = emptySpec();
        repair.equipmentHits.put(equipmentNumber, 0);
        apply(repair);

        assertFalse(weapon.isDestroyed());
    }

    @Test
    void ammoShotsLandInTheBin() {
        Mounted<?> ammoBin = mek.getAmmo().get(0);
        int equipmentNumber = mek.getEquipmentNum(ammoBin);

        DamageEditSpec spec = emptySpec();
        spec.ammoShots.put(equipmentNumber, 1);
        apply(spec);

        assertEquals(1, ammoBin.getBaseShotsLeft());
    }

    @Test
    void burstFireLandsOnTheMachineGun() {
        // the test Atlas carries no machine gun, so this test brings its own unit
        Entity tank = MMTestUtilities.getEntityForUnitTesting("Bulldog Medium Tank", true);
        assertNotNull(tank, "Test unit could not be loaded");
        Mounted<?> machineGun = null;
        for (Mounted<?> weapon : tank.getWeaponList()) {
            if (weapon.getType().hasFlag(WeaponType.F_MG)) {
                machineGun = weapon;
                break;
            }
        }
        assertNotNull(machineGun, "the test unit carries no machine gun");
        int equipmentNumber = tank.getEquipmentNum(machineGun);

        DamageEditSpec burstOn = new DamageEditSpec();
        burstOn.entityId = tank.getId();
        burstOn.mgBurst.put(equipmentNumber, true);
        new DamageEditApplier(tank, burstOn).applyToEntity();
        assertTrue(machineGun.isRapidFire());

        DamageEditSpec burstOff = new DamageEditSpec();
        burstOff.entityId = tank.getId();
        burstOff.mgBurst.put(equipmentNumber, false);
        new DamageEditApplier(tank, burstOff).applyToEntity();
        assertFalse(machineGun.isRapidFire());
    }

    @Test
    void hotLoadingLandsOnTheAmmoBin() {
        Mounted<?> lrmBin = null;
        for (Mounted<?> ammoBin : mek.getAmmo()) {
            if (ammoBin.getType().hasFlag(AmmoType.F_HOTLOAD)) {
                lrmBin = ammoBin;
                break;
            }
        }
        assertNotNull(lrmBin, "the test unit carries no hot-loadable ammo");
        int equipmentNumber = mek.getEquipmentNum(lrmBin);

        DamageEditSpec hotLoadOn = emptySpec();
        hotLoadOn.hotLoadedAmmo.put(equipmentNumber, true);
        apply(hotLoadOn);
        assertTrue(lrmBin.isHotLoaded());

        DamageEditSpec hotLoadOff = emptySpec();
        hotLoadOff.hotLoadedAmmo.put(equipmentNumber, false);
        apply(hotLoadOff);
        assertFalse(lrmBin.isHotLoaded());
    }

    /** Adds the equipment of the given internal name to the unit, failing the test if the type is unknown. */
    private static Mounted<?> addEquipment(Mek target, String internalName, int location)
          throws LocationFullException {
        EquipmentType equipmentType = EquipmentType.get(internalName);
        assertNotNull(equipmentType, "Equipment type " + internalName + " must exist");
        return target.addEquipment(equipmentType, location);
    }

    /** A spec holding only the given equipment mode choice, applied to the given unit. */
    private static void applyModeSwitch(Entity target, int equipmentNumber, String modeName) {
        DamageEditSpec spec = new DamageEditSpec();
        spec.entityId = target.getId();
        spec.equipmentMode.put(equipmentNumber, modeName);
        new DamageEditApplier(target, spec).applyToEntity();
    }

    /** A spec holding only the given charge switch, applied to the given unit. */
    private static void applyChargeSwitch(Entity target, int equipmentNumber, boolean charged) {
        DamageEditSpec spec = new DamageEditSpec();
        spec.entityId = target.getId();
        spec.equipmentCharged.put(equipmentNumber, charged);
        new DamageEditApplier(target, spec).applyToEntity();
    }

    @Test
    void equipmentSwitchLandsImmediatelyWithoutAnEndPhase() throws LocationFullException {
        BipedMek bipedMek = new BipedMek();
        Mounted<?> ecm = addEquipment(bipedMek, "ISGuardianECMSuite", Mek.LOC_RIGHT_TORSO);
        int equipmentNumber = bipedMek.getEquipmentNum(ecm);
        assertTrue(bipedMek.hasActiveECM(), "A freshly mounted Guardian ECM starts on");

        applyModeSwitch(bipedMek, equipmentNumber, Mounted.MODE_OFF);
        assertTrue(ecm.isModeTurnedOff(), "The gamemaster's switch needs no End Phase");
        assertFalse(bipedMek.hasActiveECM(), "A switched-off ECM suite projects no field");

        applyModeSwitch(bipedMek, equipmentNumber, MiscType.MODE_ECM);
        assertEquals(MiscType.MODE_ECM, ecm.curMode().getName(),
              "Switching back on restores the equipment's active mode");
        assertTrue(bipedMek.hasActiveECM());
    }

    @Test
    void matchingSwitchLeavesAPendingPlayerModeChangeAlone() throws LocationFullException {
        BipedMek bipedMek = new BipedMek();
        Mounted<?> ecm = addEquipment(bipedMek, "ISGuardianECMSuite", Mek.LOC_RIGHT_TORSO);
        int equipmentNumber = bipedMek.getEquipmentNum(ecm);
        ecm.setMode(Mounted.MODE_OFF);
        assertTrue(ecm.isModeTurnedOffNextRound(), "The player's switch to Off is pending");

        // the switch was prefilled with the current mode and the gamemaster did not touch it
        applyModeSwitch(bipedMek, equipmentNumber, MiscType.MODE_ECM);

        assertTrue(ecm.isModeTurnedOffNextRound(), "An untouched switch leaves the pending change in place");
    }

    @Test
    void gaussPowerSwitchLandsImmediately() throws LocationFullException {
        BipedMek bipedMek = new BipedMek();
        Mounted<?> gaussRifle = addEquipment(bipedMek, "ISGaussRifle", Mek.LOC_RIGHT_TORSO);
        // the powered up/down modes are added when a game's options are adapted, as happens on game setup
        gaussRifle.adaptToGameOptions(new GameOptions());
        int equipmentNumber = bipedMek.getEquipmentNum(gaussRifle);

        applyModeSwitch(bipedMek, equipmentNumber, Weapon.MODE_GAUSS_POWERED_DOWN);
        assertEquals(Weapon.MODE_GAUSS_POWERED_DOWN, gaussRifle.curMode().getName());

        applyModeSwitch(bipedMek, equipmentNumber, Weapon.MODE_GAUSS_POWERED_UP);
        assertEquals(Weapon.MODE_GAUSS_POWERED_UP, gaussRifle.curMode().getName());
    }

    @Test
    void rulesLockedModeStaysLockedEvenForTheGamemaster() throws LocationFullException {
        BipedMek bipedMek = new BipedMek();
        Mounted<?> ecm = addEquipment(bipedMek, "ISGuardianECMSuite", Mek.LOC_RIGHT_TORSO);
        // the per-mount lock the rules use, e.g. an aero's rotary autocannon forced to 6-shot
        ecm.setModeSwitchable(false);

        applyModeSwitch(bipedMek, bipedMek.getEquipmentNum(ecm), Mounted.MODE_OFF);

        assertFalse(ecm.isModeTurnedOff(), "A rules-locked mode is not switchable, even by the gamemaster");
    }

    @Test
    void brokenSpecValuesFromTheNetworkAreIgnored() throws LocationFullException {
        BipedMek bipedMek = new BipedMek();
        Mounted<?> ecm = addEquipment(bipedMek, "ISGuardianECMSuite", Mek.LOC_RIGHT_TORSO);
        Mounted<?> bombastLaser = addEquipment(bipedMek, "ISBombastLaser", Mek.LOC_RIGHT_ARM);

        // the spec travels in a packet, so a broken client may fill its maps with nulls
        DamageEditSpec brokenSpec = new DamageEditSpec();
        brokenSpec.entityId = bipedMek.getId();
        brokenSpec.equipmentMode.put(bipedMek.getEquipmentNum(ecm), null);
        brokenSpec.equipmentCharged.put(bipedMek.getEquipmentNum(bombastLaser), null);
        new DamageEditApplier(bipedMek, brokenSpec).applyToEntity();

        assertTrue(bipedMek.hasActiveECM(), "A null mode name is ignored, not applied or crashed on");
        assertEquals(ChargeLevel.CHARGE_NONE, bombastLaser.getChargeState(), "A null charge state is ignored");
    }

    @Test
    void unknownModeNameChangesNothing() throws LocationFullException {
        BipedMek bipedMek = new BipedMek();
        Mounted<?> ecm = addEquipment(bipedMek, "ISGuardianECMSuite", Mek.LOC_RIGHT_TORSO);
        Mounted<?> stealth = addEquipment(bipedMek, "IS Stealth", Mek.LOC_LEFT_TORSO);
        stealth.setModeImmediately(Mounted.MODE_ON);

        applyModeSwitch(bipedMek, bipedMek.getEquipmentNum(ecm), "No Such Mode");

        assertEquals(MiscType.MODE_ECM, ecm.curMode().getName(), "An unknown mode name changes nothing");
        assertEquals(Mounted.MODE_ON, stealth.curMode().getName(),
              "A failed switch must not trigger the ECM/stealth follow-up");
    }

    @Test
    void multiModeEquipmentTakesAnyOfItsModes() throws LocationFullException {
        BipedMek bipedMek = new BipedMek();
        Mounted<?> shield = addEquipment(bipedMek, "ISMediumShield", Mek.LOC_LEFT_ARM);
        int equipmentNumber = bipedMek.getEquipmentNum(shield);

        applyModeSwitch(bipedMek, equipmentNumber, MiscType.S_ACTIVE_SHIELD);
        assertEquals(MiscType.S_ACTIVE_SHIELD, shield.curMode().getName());

        applyModeSwitch(bipedMek, equipmentNumber, MiscType.S_PASSIVE_SHIELD);
        assertEquals(MiscType.S_PASSIVE_SHIELD, shield.curMode().getName());
    }

    @Test
    void capacitorChargeSwitchLandsImmediately() throws LocationFullException {
        BipedMek bipedMek = new BipedMek();
        Mounted<?> capacitor = addEquipment(bipedMek, "ISPPCCapacitor", Mek.LOC_RIGHT_ARM);
        int equipmentNumber = bipedMek.getEquipmentNum(capacitor);

        applyChargeSwitch(bipedMek, equipmentNumber, true);
        assertEquals(Mounted.MODE_CAPACITOR_CHARGE, capacitor.curMode().getName(),
              "The gamemaster's charge lands at once, with no charging round");

        applyChargeSwitch(bipedMek, equipmentNumber, false);
        assertEquals(Mounted.MODE_OFF, capacitor.curMode().getName(), "Emptying the capacitor drops its charge");
    }

    @Test
    void bombastLaserChargeSwitchLandsImmediately() throws LocationFullException {
        BipedMek bipedMek = new BipedMek();
        Mounted<?> bombastLaser = addEquipment(bipedMek, "ISBombastLaser", Mek.LOC_RIGHT_ARM);
        int equipmentNumber = bipedMek.getEquipmentNum(bombastLaser);
        assertEquals(ChargeLevel.CHARGE_NONE, bombastLaser.getChargeState(), "A fresh bombast laser is uncharged");

        applyChargeSwitch(bipedMek, equipmentNumber, true);
        assertEquals(ChargeLevel.CHARGED, bombastLaser.getChargeState(),
              "The gamemaster's charge lands at once, with no charging round");

        applyChargeSwitch(bipedMek, equipmentNumber, false);
        assertEquals(ChargeLevel.CHARGE_NONE, bombastLaser.getChargeState());
    }

    @Test
    void switchingTheLastEcmOffTakesStealthArmorDown() throws LocationFullException {
        BipedMek bipedMek = new BipedMek();
        Mounted<?> ecm = addEquipment(bipedMek, "ISGuardianECMSuite", Mek.LOC_RIGHT_TORSO);
        Mounted<?> stealth = addEquipment(bipedMek, "IS Stealth", Mek.LOC_LEFT_TORSO);
        stealth.setModeImmediately(Mounted.MODE_ON);

        applyModeSwitch(bipedMek, bipedMek.getEquipmentNum(ecm), Mounted.MODE_OFF);

        assertTrue(ecm.isModeTurnedOff());
        assertEquals(Mounted.MODE_OFF, stealth.curMode().getName(),
              "Stealth armor cannot run without an operating ECM, so it goes down with it");
    }

    @Test
    void jamLandsOnTheWeaponAtOnce() {
        WeaponMounted weapon = jammableWeapon();
        int equipmentNumber = mek.getEquipmentNum(weapon);
        assertFalse(weapon.isJammed(), "A freshly loaded weapon is not jammed");

        DamageEditSpec jamSpec = emptySpec();
        jamSpec.weaponJammed.put(equipmentNumber, true);
        apply(jamSpec);
        assertTrue(weapon.isJammed(), "The gamemaster's jam bites at once, with no phase turnover");

        DamageEditSpec clearSpec = emptySpec();
        clearSpec.weaponJammed.put(equipmentNumber, false);
        apply(clearSpec);
        assertFalse(weapon.isJammed(), "A gamemaster must be able to clear a jam");
        assertFalse(weapon.jammedThisPhase(), "A cleared jam does not come back at the next phase");
    }

    @Test
    void jamIsRefusedOnAWeaponThatCannotJam() {
        WeaponMounted missileRack = null;
        for (WeaponMounted weapon : mek.getWeaponList()) {
            if (weapon.getType().getAmmoType() == AmmoType.AmmoTypeEnum.LRM) {
                missileRack = weapon;
            }
        }
        assertNotNull(missileRack, "The test unit carries an LRM rack");
        assertFalse(missileRack.canJam(), "No rule jams a missile rack");

        DamageEditSpec spec = emptySpec();
        spec.weaponJammed.put(mek.getEquipmentNum(missileRack), true);
        apply(spec);

        assertFalse(missileRack.isJammed(), "A jam is refused on a weapon that cannot jam");
    }

    @Test
    void firedLandsOnAOneShotLauncher() throws LocationFullException {
        BipedMek bipedMek = new BipedMek();
        Mounted<?> launcher = addEquipment(bipedMek, "ISSRM2OS", Mek.LOC_LEFT_TORSO);
        int equipmentNumber = bipedMek.getEquipmentNum(launcher);
        assertTrue(launcher.isOneShot(), "The test launcher must be a one-shot weapon");

        applyWeaponFired(bipedMek, equipmentNumber, true);
        assertTrue(launcher.isFired());

        applyWeaponFired(bipedMek, equipmentNumber, false);
        assertFalse(launcher.isFired(), "Clearing Fired reloads the launcher");
    }

    @Test
    void firedIsRefusedOnAWeaponThatIsNotOneShot() {
        Mounted<?> weapon = mek.getWeaponList().get(0);
        assertFalse(weapon.isOneShot());

        applyWeaponFired(mek, mek.getEquipmentNum(weapon), true);

        assertFalse(weapon.isFired(), "Fired is a lasting state on one-shot weapons alone");
    }

    @Test
    void directionalMountLockLandsOnAMountedWeapon() throws LocationFullException {
        BipedMek bipedMek = new BipedMek();
        Game game = new Game();
        game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);
        bipedMek.setGame(game);
        Mounted<?> laser = addEquipment(bipedMek, "ISERLargeLaser", Mek.LOC_RIGHT_TORSO);
        laser.getQuirks().getOption(OptionsConstants.QUIRK_WEAPON_POS_DIRECT_TORSO_MOUNT).setValue(true);
        assertTrue(laser.hasDirectionalTorsoMount(), "The test laser must sit in a Directional Torso Mount");
        int equipmentNumber = bipedMek.getEquipmentNum(laser);

        applyMountLock(bipedMek, equipmentNumber, true);
        assertTrue(laser.isDirectionalMountLocked());

        applyMountLock(bipedMek, equipmentNumber, false);
        assertFalse(laser.isDirectionalMountLocked(), "A gamemaster must be able to free a locked mount");
    }

    @Test
    void directionalMountLockIsRefusedWithoutAMount() {
        Mounted<?> weapon = mek.getWeaponList().get(0);
        assertFalse(weapon.hasDirectionalTorsoMount());

        applyMountLock(mek, mek.getEquipmentNum(weapon), true);

        assertFalse(weapon.isDirectionalMountLocked(), "Only a Directional Torso Mount can be locked");
    }

    @Test
    void breachMarksTheLocationAndEverythingInIt() {
        applyBreach(Mek.LOC_LEFT_TORSO, true);

        assertEquals(ILocationExposureStatus.BREACHED, mek.getLocationStatus(Mek.LOC_LEFT_TORSO));
        assertEquals(ILocationExposureStatus.NORMAL, mek.getLocationStatus(Mek.LOC_RIGHT_TORSO),
              "Only the edited location is breached");
        for (Mounted<?> mounted : mek.getEquipment()) {
            if (mounted.getLocation() == Mek.LOC_LEFT_TORSO) {
                assertTrue(mounted.isBreached(), mounted.getName() + " in the breached location is out of action");
            } else if (mounted.getLocation() == Mek.LOC_RIGHT_TORSO) {
                assertFalse(mounted.isBreached(), mounted.getName() + " outside the breach is untouched");
            }
        }
        assertTrue(allCriticalSlotsBreached(Mek.LOC_LEFT_TORSO, true));
    }

    @Test
    void clearingABreachPutsTheLocationBackInService() {
        applyBreach(Mek.LOC_LEFT_TORSO, true);

        applyBreach(Mek.LOC_LEFT_TORSO, false);

        assertEquals(ILocationExposureStatus.NORMAL, mek.getLocationStatus(Mek.LOC_LEFT_TORSO),
              "A breached location is pinned in play, so the gamemaster's clear has to force it back");
        for (Mounted<?> mounted : mek.getEquipment()) {
            if (mounted.getLocation() == Mek.LOC_LEFT_TORSO) {
                assertFalse(mounted.isBreached(), mounted.getName() + " is back in service");
            }
        }
        assertTrue(allCriticalSlotsBreached(Mek.LOC_LEFT_TORSO, false));
    }

    /** A weapon of the test Mek that some rule can jam: its AC/20. */
    private WeaponMounted jammableWeapon() {
        for (WeaponMounted weapon : mek.getWeaponList()) {
            if (weapon.canJam()) {
                return weapon;
            }
        }
        throw new AssertionError("The test unit must carry a weapon that can jam");
    }

    /** A spec holding only the given Fired state, applied to the given unit. */
    private static void applyWeaponFired(Entity target, int equipmentNumber, boolean fired) {
        DamageEditSpec spec = new DamageEditSpec();
        spec.entityId = target.getId();
        spec.weaponFired.put(equipmentNumber, fired);
        new DamageEditApplier(target, spec).applyToEntity();
    }

    /** A spec holding only the given Directional Torso Mount lock, applied to the given unit. */
    private static void applyMountLock(Entity target, int equipmentNumber, boolean locked) {
        DamageEditSpec spec = new DamageEditSpec();
        spec.entityId = target.getId();
        spec.directionalMountLocked.put(equipmentNumber, locked);
        new DamageEditApplier(target, spec).applyToEntity();
    }

    /** A spec breaching or sealing one location of the test Mek and nothing else. */
    private void applyBreach(int location, boolean breached) {
        DamageEditSpec spec = emptySpec();
        spec.locationBreached = new Boolean[mek.locations()];
        spec.locationBreached[location] = breached;
        apply(spec);
    }

    /** Whether every occupied critical slot of the location carries the given breach mark. */
    private boolean allCriticalSlotsBreached(int location, boolean breached) {
        for (int slot = 0; slot < mek.getNumberOfCriticalSlots(location); slot++) {
            CriticalSlot criticalSlot = mek.getCritical(location, slot);
            if ((criticalSlot != null) && (criticalSlot.isBreached() != breached)) {
                return false;
            }
        }
        return true;
    }
}
