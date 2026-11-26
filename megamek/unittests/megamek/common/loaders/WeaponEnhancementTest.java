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

package megamek.common.loaders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.options.GameOptions;
import megamek.common.units.BipedMek;
import megamek.common.units.Mek;
import megamek.common.weapons.lasers.LaserWeapon;
import megamek.common.weapons.lasers.clan.CLChemicalLaserWeapon;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import megamek.common.weapons.lasers.innerSphere.small.ISLaserSmall;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Weapon enhancements (laser insulators, RISC laser pulse modules, Artemis and Apollo FCS, PPC capacitors) belong to
 * the weapon they follow in the unit file, and a weapon can have several of them.
 */
class WeaponEnhancementTest {
    private static final String MEDIUM_LASER = "Medium Laser";
    private static final String INSULATOR = "ISLaserInsulator";
    private static final String PULSE_MODULE = "ISRISCLaserPulseModule";
    private static final String LRM = "LRM 15";
    private static final String ARTEMIS = "ISArtemisIV";

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    private static Mek newMek() {
        Mek mek = new BipedMek();
        mek.setWeight(100.0);
        mek.setEngine(new Engine(300, Engine.NORMAL_ENGINE, 0));
        return mek;
    }

    /**
     * @return The mounts of the given equipment, as a Mek loads them from the critical rows of its right torso
     */
    private static List<Mounted<?>> loadRightTorso(String... names) throws Exception {
        Mek mek = newMek();
        for (String name : names) {
            mek.addEquipment(EquipmentType.get(name), Mek.LOC_RIGHT_TORSO);
        }
        Mek loaded = (Mek) new MekFileParser(mek.getMtf()).getEntity();
        return MtfFile.rowMounts(loaded, Mek.LOC_RIGHT_TORSO).stream().distinct().toList();
    }

    /**
     * @return A medium laser with the given enhancements, which has its pulse mode as in a game
     */
    private static WeaponMounted laserWith(String... enhancements) throws Exception {
        return laserWith(new ISLaserMedium(), enhancements);
    }

    /**
     * @return A laser of the given type with the given enhancements, which has its pulse mode as in a game
     */
    private static WeaponMounted laserWith(LaserWeapon laserType, String... enhancements) throws Exception {
        Mek mek = newMek();
        laserType.adaptToGameOptions(new GameOptions());
        WeaponMounted laser = (WeaponMounted) Mounted.createMounted(mek, laserType);
        mek.addEquipment(laser, Mek.LOC_RIGHT_TORSO, false);
        for (String enhancement : enhancements) {
            mek.addEquipment(EquipmentType.get(enhancement), Mek.LOC_RIGHT_TORSO);
        }
        MekFileParser.postLoadInit(mek);
        return laser;
    }

    /** A capacitor is in its PPC's turret (TO:AUE p.148), so it adds to the turret's weight (TO:AUE p.158) */
    @Test
    void anUnmarkedCapacitorWeighsInItsPpcsTurret() throws Exception {
        Mek mek = newMek();
        Mounted<?> turret = mek.addEquipment(EquipmentType.get("ISShoulderTurret"), Mek.LOC_RIGHT_TORSO);
        Mounted<?> ppc = mek.addEquipment(EquipmentType.get("ISHeavyPPC"), Mek.LOC_RIGHT_TORSO);
        ppc.setMekTurretMounted(true);
        mek.addEquipment(EquipmentType.get("ISPPCCapacitor"), Mek.LOC_RIGHT_TORSO).setLinked(ppc);

        // A tenth of the 10-ton heavy PPC and 1-ton capacitor, rounded up to the half ton
        assertEquals(1.5, turret.getTonnage());
    }

    @Test
    void enhancementsFollowTheirWeapon() throws Exception {
        List<Mounted<?>> mounts = loadRightTorso(MEDIUM_LASER, INSULATOR, INSULATOR, MEDIUM_LASER, INSULATOR,
              PULSE_MODULE, MEDIUM_LASER);

        assertEquals(List.of(mounts.get(1), mounts.get(2)), mounts.get(0).getEnhancements());
        assertEquals(List.of(mounts.get(4), mounts.get(5)), mounts.get(3).getEnhancements());
        assertTrue(mounts.get(6).getEnhancements().isEmpty());
    }

    @Test
    void insulatorsStackOnTheNearestLaserAbove() throws Exception {
        List<Mounted<?>> mounts = loadRightTorso(MEDIUM_LASER, MEDIUM_LASER, INSULATOR, INSULATOR);

        assertTrue(mounts.get(0).getEnhancements().isEmpty());
        assertEquals(List.of(mounts.get(2), mounts.get(3)), mounts.get(1).getEnhancements());
    }

    @Test
    void insulatorsBeforeAnyLaserStackOnTheFirstLaser() throws Exception {
        List<Mounted<?>> mounts = loadRightTorso(INSULATOR, INSULATOR, MEDIUM_LASER);

        assertEquals(List.of(mounts.get(0), mounts.get(1)), mounts.get(2).getEnhancements());
    }

    @Test
    void anEnhancementMovedToAnotherWeaponLeavesTheFirst() throws Exception {
        List<Mounted<?>> mounts = loadRightTorso(MEDIUM_LASER, INSULATOR, MEDIUM_LASER);

        mounts.get(1).setLinked(mounts.get(2));

        assertTrue(mounts.get(0).getLinkedEquipment().isEmpty());
        assertEquals(List.of(mounts.get(1)), mounts.get(2).getLinkedEquipment());
    }

    @Test
    void oneArtemisForEachLauncher() throws Exception {
        List<Mounted<?>> mounts = loadRightTorso(LRM, LRM, ARTEMIS, ARTEMIS);

        assertSame(mounts.get(1), mounts.get(2).getLinked());
        assertSame(mounts.get(0), mounts.get(3).getLinked());
        assertSame(mounts.get(3), mounts.get(0).getEnhancement(MiscType.F_ARTEMIS));
        assertSame(mounts.get(2), mounts.get(1).getEnhancement(MiscType.F_ARTEMIS));
    }

    @Test
    void aPpcTakesOneCapacitor() throws Exception {
        List<Mounted<?>> mounts = loadRightTorso("ISPPC", "ISPPCCapacitor", "ISPPCCapacitor");

        assertEquals(List.of(mounts.get(1)), mounts.get(0).getEnhancements());
        assertNull(mounts.get(2).getLinked());
    }

    @Test
    void enhancementWithoutAWeaponBeforeItGetsTheFirstWithoutOne() throws Exception {
        List<Mounted<?>> mounts = loadRightTorso(ARTEMIS, INSULATOR, LRM, MEDIUM_LASER, LRM, MEDIUM_LASER, ARTEMIS,
              ARTEMIS);

        assertSame(mounts.get(2), mounts.get(0).getLinked());
        assertSame(mounts.get(3), mounts.get(1).getLinked());
        assertSame(mounts.get(4), mounts.get(6).getLinked());
        // both launchers have their Artemis, so there is no weapon for another
        assertNull(mounts.get(7).getLinked());
    }

    @Test
    void insulatorAndPulseModuleBothApply() throws Exception {
        for (List<String> enhancements : List.of(List.of(INSULATOR, PULSE_MODULE), List.of(PULSE_MODULE, INSULATOR))) {
            WeaponMounted laser = laserWith(enhancements.toArray(String[]::new));
            int heat = laser.getType().getHeat();
            assertNotNull(laser.getEnhancement(MiscType.F_LASER_INSULATOR));
            assertNotNull(laser.getEnhancement(MiscType.F_RISC_LASER_PULSE_MODULE));

            // the insulator saves a point of heat and the pulse module gives the laser its pulse mode
            assertEquals(heat - 1, laser.getCurrentHeat());
            assertEquals(2, laser.getType().getModesCount(laser));
            laser.setModeImmediately("Pulse");
            assertEquals("Pulse", laser.curMode().getName());
            assertEquals(-2, laser.getType().getToHitModifier(laser));
            assertEquals(heat - 1 + 2, laser.getCurrentHeat());
        }
    }

    @Test
    void insulatorLowersThePulseModeHeatToAMinimumOfOne() throws Exception {
        // a small laser makes 1 heat, 3 in pulse mode (IO:AE p.87). TO:AUE p.134 doesn't say which comes first; we take
        // the insulator's point off the total, to a minimum of 1
        WeaponMounted laser = laserWith(new ISLaserSmall(), PULSE_MODULE, INSULATOR);
        assertEquals(1, laser.getCurrentHeat());
        laser.setModeImmediately("Pulse");
        assertEquals(2, laser.getCurrentHeat());
    }

    @Test
    void laserStaysInsulatedWhileOneInsulatorWorks() throws Exception {
        WeaponMounted laser = laserWith(INSULATOR, INSULATOR);
        int heat = laser.getType().getHeat();
        assertEquals(2, laser.getEnhancements().size());
        assertEquals(heat - 1, laser.getCurrentHeat());

        laser.getEnhancements().getFirst().setDestroyed(true);
        assertEquals(heat - 1, laser.getCurrentHeat());

        laser.getEnhancements().getLast().setDestroyed(true);
        assertEquals(heat, laser.getCurrentHeat());
    }

    @Test
    void unlinkingAnEnhancementLeavesTheWeaponLinkedByTheOther() throws Exception {
        WeaponMounted laser = laserWith(INSULATOR, PULSE_MODULE);
        Mounted<?> insulator = laser.getEnhancement(MiscType.F_LASER_INSULATOR);
        Mounted<?> pulseModule = laser.getEnhancement(MiscType.F_RISC_LASER_PULSE_MODULE);
        assertSame(pulseModule, laser.getLinkedBy());

        pulseModule.setLinked(null);

        assertSame(insulator, laser.getLinkedBy());
        assertEquals(List.of(insulator), laser.getEnhancements());
    }

    /**
     * Code that asks {@link Mounted#getLinkedBy()} for the Artemis or Apollo FCS of a launcher or the capacitor of a
     * PPC relies on this: only lasers can have more than one enhancement.
     */
    @Test
    void onlyLasersTakeMoreThanOneKindOfEnhancement() {
        for (EquipmentType type : Collections.list(EquipmentType.getAllTypes())) {
            if (type instanceof WeaponType weapon) {
                boolean laser = weapon.hasFlag(WeaponType.F_LASER) || (weapon instanceof CLChemicalLaserWeapon);
                long others = List.of(weapon.hasFlag(WeaponType.F_ARTEMIS_COMPATIBLE), weapon.hasFlag(WeaponType.F_PPC),
                      weapon.getAmmoType() == AmmoType.AmmoTypeEnum.MRM).stream().filter(kind -> kind).count();
                assertTrue(others + (laser ? 1 : 0) <= 1, weapon.getInternalName());
            }
        }
    }
}
