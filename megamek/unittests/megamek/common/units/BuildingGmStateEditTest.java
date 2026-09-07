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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.loaders.MekFileParser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Advanced Building state a gamemaster can set from the damage editor: the power switch, and the
 * critical results of the Advanced Building Critical Hits Table that persist (TO:AR p. 119).
 *
 * <p>The rules only ever inflict these, so the editor needs to be able to clear them too. These tests drive
 * {@link DamageEditApplier}, which is the path both the lobby and the server commit through.</p>
 */
class BuildingGmStateEditTest {

    private static final String LARGE_BUILDING_FILE = "Simple Large Building Entity.blk";
    private static final String GUN_EMPLACEMENT_FILE = "Simple Gun Emplacement (Gauss).blk";

    private static final int FIRST_HEX_GROUND_FLOOR = 0;
    private static final int FIRST_HEX_TOP_FLOOR = 2;
    private static final int SECOND_HEX_GROUND_FLOOR = 3;

    @BeforeAll
    static void initialize() {
        EquipmentType.initializeTypes();
    }

    private AbstractBuildingEntity load(String filename) throws Exception {
        File file = new File("testresources/megamek/common/units/" + filename);
        return (AbstractBuildingEntity) new MekFileParser(file).getEntity();
    }

    private void apply(AbstractBuildingEntity building, DamageEditSpec spec) {
        spec.entityId = building.getId();
        new DamageEditApplier(building, spec).applyToEntity();
    }

    @Nested
    @DisplayName("the power switch")
    class PowerSwitchTests {

        @Test
        @DisplayName("switching the power off takes a structure off line and shuts it down")
        void switchingPowerOffShutsTheBuildingDown() throws Exception {
            BuildingEntity building = (BuildingEntity) load(LARGE_BUILDING_FILE);
            assertTrue(building.hasPower(), "the test building has a generator sized to carry it");

            DamageEditSpec spec = new DamageEditSpec();
            spec.buildingPowerSwitchedOff = true;
            apply(building, spec);

            assertTrue(building.isPowerSwitchedOff());
            assertFalse(building.hasPower(), "a structure switched off has no power however healthy its generator");
            assertTrue(building.isShutDown(), "a structure without power is shut down");
        }

        @Test
        @DisplayName("switching the power back on restarts the structure")
        void switchingPowerOnRestartsTheBuilding() throws Exception {
            BuildingEntity building = (BuildingEntity) load(LARGE_BUILDING_FILE);
            DamageEditSpec offSpec = new DamageEditSpec();
            offSpec.buildingPowerSwitchedOff = true;
            apply(building, offSpec);

            DamageEditSpec onSpec = new DamageEditSpec();
            onSpec.buildingPowerSwitchedOff = false;
            apply(building, onSpec);

            assertTrue(building.hasPower());
            assertFalse(building.isShutDown(), "the switch must bring it back, not only take it down");
        }
    }

    @Nested
    @DisplayName("gunners")
    class GunnerTests {

        @Test
        @DisplayName("stun turns can be set and cleared")
        void stunTurnsCanBeSetAndCleared() throws Exception {
            AbstractBuildingEntity building = load(LARGE_BUILDING_FILE);

            DamageEditSpec stunSpec = new DamageEditSpec();
            stunSpec.buildingStunnedTurns = 3;
            apply(building, stunSpec);
            assertEquals(3, building.getStunnedTurns());
            assertTrue(building.isStunned());

            DamageEditSpec clearSpec = new DamageEditSpec();
            clearSpec.buildingStunnedTurns = 0;
            apply(building, clearSpec);
            assertFalse(building.isStunned(), "a gamemaster must be able to bring the gunners round again");
        }

        @Test
        @DisplayName("killing gunners silences every floor of that hex but leaves other hexes alone")
        void killingGunnersAppliesToTheWholeHex() throws Exception {
            AbstractBuildingEntity building = load(LARGE_BUILDING_FILE);
            building.setPosition(new Coords(5, 5));

            DamageEditSpec spec = new DamageEditSpec();
            spec.buildingGunnersKilled.put(FIRST_HEX_GROUND_FLOOR, true);
            apply(building, spec);

            assertTrue(building.hasDeadGunners(FIRST_HEX_GROUND_FLOOR));
            assertTrue(building.hasDeadGunners(FIRST_HEX_TOP_FLOOR), "gunners are killed a hex at a time");
            assertFalse(building.hasDeadGunners(SECOND_HEX_GROUND_FLOOR), "a neighbouring hex keeps its gunners");
        }

        @Test
        @DisplayName("killed gunners can be restored, which also lifts the doomed flag")
        void killedGunnersCanBeRestored() throws Exception {
            AbstractBuildingEntity building = load(GUN_EMPLACEMENT_FILE);
            building.setPosition(new Coords(5, 5));

            DamageEditSpec killSpec = new DamageEditSpec();
            killSpec.buildingGunnersKilled.put(FIRST_HEX_GROUND_FLOOR, true);
            apply(building, killSpec);
            assertTrue(building.allGunnersDead(), "a single hex building loses all its gunners at once");
            assertTrue(building.getCrew().isDoomed());

            DamageEditSpec reviveSpec = new DamageEditSpec();
            reviveSpec.buildingGunnersKilled.put(FIRST_HEX_GROUND_FLOOR, false);
            apply(building, reviveSpec);

            assertFalse(building.hasDeadGunners(FIRST_HEX_GROUND_FLOOR));
            assertFalse(building.allGunnersDead());
            assertFalse(building.getCrew().isDoomed(), "restoring the gunners must lift the doomed flag too");
        }
    }

    @Nested
    @DisplayName("weapons")
    class WeaponTests {

        @Test
        @DisplayName("a weapon can be jammed and unjammed")
        void weaponCanBeJammedAndUnjammed() throws Exception {
            AbstractBuildingEntity building = load(GUN_EMPLACEMENT_FILE);
            WeaponMounted weapon = building.getWeaponList().getFirst();
            int equipmentNumber = building.getEquipmentNum(weapon);

            DamageEditSpec jamSpec = new DamageEditSpec();
            jamSpec.buildingWeaponJammed.put(equipmentNumber, true);
            apply(building, jamSpec);
            assertTrue(weapon.isJammed());

            DamageEditSpec clearSpec = new DamageEditSpec();
            clearSpec.buildingWeaponJammed.put(equipmentNumber, false);
            apply(building, clearSpec);
            assertFalse(weapon.isJammed(), "a gamemaster must be able to clear a jam");
        }

        @Test
        @DisplayName("a turret can be locked and freed")
        void turretCanBeLockedAndFreed() throws Exception {
            AbstractBuildingEntity building = load(GUN_EMPLACEMENT_FILE);
            WeaponMounted weapon = building.getWeaponList().getFirst();
            assertTrue(building.isTurretMounted(weapon), "the gun emplacement guns are turret mounted");
            int equipmentNumber = building.getEquipmentNum(weapon);

            DamageEditSpec lockSpec = new DamageEditSpec();
            lockSpec.buildingTurretLocked.put(equipmentNumber, true);
            apply(building, lockSpec);
            assertTrue(building.isTurretLocked(weapon));
            assertTrue(building.hasLockedTurret());

            DamageEditSpec freeSpec = new DamageEditSpec();
            freeSpec.buildingTurretLocked.put(equipmentNumber, false);
            apply(building, freeSpec);
            assertFalse(building.isTurretLocked(weapon), "a gamemaster must be able to free a locked turret");
            assertFalse(building.hasLockedTurret());
        }
    }
}
