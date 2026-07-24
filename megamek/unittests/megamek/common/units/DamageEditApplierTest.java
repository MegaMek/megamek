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

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;
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
}
