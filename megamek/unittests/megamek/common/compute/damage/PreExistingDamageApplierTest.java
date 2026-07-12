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
package megamek.common.compute.damage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.CriticalSlot;
import megamek.common.compute.damage.CritAssignment.EquipmentCrit;
import megamek.common.compute.damage.CritAssignment.MekSystemCrit;
import megamek.common.compute.damage.CritAssignment.VehicleCrit;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.GunEmplacement;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.common.units.VTOL;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for the First Succession War pre-existing damage rules (FSW p.144-145). The applier rolls dice, so most tests
 * run many simulations and check invariants that must hold on every run: total damage matches the table, the unit is
 * never destroyed or immobilized, and forbidden critical hits (cockpit, ammo, engine or gyro destruction) never
 * appear.
 */
class PreExistingDamageApplierTest {

    private static final int SIMULATION_RUNS = 200;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    // ----- fixtures -----

    private Mek buildMek() throws Exception {
        Mek mek = new BipedMek();
        mek.setWeight(50.0);
        mek.setEngine(new Engine(250, Engine.NORMAL_ENGINE, 0));
        mek.addCockpit();
        mek.addGyro();
        mek.addEngineCrits();
        mek.autoSetInternal();
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(12, location);
            if (mek.hasRearArmor(location)) {
                mek.initializeRearArmor(4, location);
            }
        }
        mek.initializeArmor(9, Mek.LOC_HEAD);
        mek.addEquipment(EquipmentType.get("Medium Laser"), Mek.LOC_RIGHT_ARM);
        mek.addEquipment(EquipmentType.get("ISUltraAC5"), Mek.LOC_RIGHT_TORSO);
        mek.addEquipment(EquipmentType.get("ISUltraAC5 Ammo"), Mek.LOC_LEFT_TORSO);
        return mek;
    }

    /**
     * A Light engine puts two engine slots in each side torso, so destroying one side torso costs only two engine
     * hits and leaves the Mek alive. This is the case where double-counting a simulation-assigned engine hit would
     * wrongly forbid the destruction.
     */
    private Mek buildLightEngineMek() throws Exception {
        Mek mek = new BipedMek();
        mek.setWeight(50.0);
        mek.setEngine(new Engine(250, Engine.LIGHT_ENGINE, 0));
        mek.addCockpit();
        mek.addGyro();
        mek.addEngineCrits();
        mek.autoSetInternal();
        for (int location = 0; location < mek.locations(); location++) {
            mek.initializeArmor(12, location);
            if (mek.hasRearArmor(location)) {
                mek.initializeRearArmor(4, location);
            }
        }
        mek.addEquipment(EquipmentType.get("Medium Laser"), Mek.LOC_RIGHT_ARM);
        return mek;
    }

    private Tank buildTank() throws Exception {
        Tank tank = new Tank();
        tank.setMovementMode(EntityMovementMode.TRACKED);
        tank.setWeight(50.0);
        for (int location = 0; location < tank.locations(); location++) {
            if (location == Tank.LOC_BODY) {
                continue;
            }
            tank.initializeInternal(12, location);
            tank.initializeArmor(20, location);
        }
        tank.addEquipment(EquipmentType.get("Medium Laser"), Tank.LOC_FRONT);
        return tank;
    }

    private VTOL buildVtol() throws Exception {
        VTOL vtol = new VTOL();
        vtol.setMovementMode(EntityMovementMode.VTOL);
        vtol.setWeight(20.0);
        for (int location = 0; location < vtol.locations(); location++) {
            if (location == Tank.LOC_BODY) {
                continue;
            }
            vtol.initializeInternal((location == VTOL.LOC_ROTOR) ? 2 : 6, location);
            vtol.initializeArmor((location == VTOL.LOC_ROTOR) ? 2 : 12, location);
        }
        vtol.addEquipment(EquipmentType.get("Medium Laser"), Tank.LOC_FRONT);
        return vtol;
    }

    private AeroSpaceFighter buildFighter() throws Exception {
        AeroSpaceFighter fighter = new AeroSpaceFighter();
        fighter.setWeight(50.0);
        fighter.setOSI(15);
        fighter.setSI(15);
        for (int location = 0; location < fighter.locations(); location++) {
            fighter.initializeArmor(20, location);
        }
        fighter.addEquipment(EquipmentType.get("Medium Laser"), AeroSpaceFighter.LOC_NOSE);
        return fighter;
    }

    // ----- helpers -----

    private int totalRemaining(PreExistingDamageResult result) {
        int remaining = result.structuralIntegrity();
        for (int location = 0; location < result.armor().length; location++) {
            remaining += result.armor()[location] + result.rearArmor()[location] + result.internal()[location];
        }
        return remaining;
    }

    private int totalRemaining(Entity entity) {
        int remaining = 0;
        if (entity instanceof megamek.common.units.Aero aero) {
            remaining += Math.max(aero.getSI(), 0);
        }
        for (int location = 0; location < entity.locations(); location++) {
            remaining += Math.max(entity.getArmor(location, false), 0);
            if (entity.hasRearArmor(location)) {
                remaining += Math.max(entity.getArmor(location, true), 0);
            }
            if (!(entity instanceof megamek.common.units.Aero)) {
                remaining += Math.max(entity.getInternal(location), 0);
            }
        }
        return remaining;
    }

    /** Asserts the invariants that must hold for every simulation on a Mek. */
    private void assertMekInvariants(Mek mek, PreExistingDamageResult result) {
        assertTrue(result.internal()[Mek.LOC_HEAD] > 0, "head must never be destroyed");
        assertTrue(result.internal()[Mek.LOC_CENTER_TORSO] > 0, "center torso must never be destroyed");
        assertTrue(result.internal()[Mek.LOC_RIGHT_LEG] > 0, "legs must never be destroyed");
        assertTrue(result.internal()[Mek.LOC_LEFT_LEG] > 0, "legs must never be destroyed");

        int engineCrits = 0;
        int gyroCrits = 0;
        for (CritAssignment assignment : result.critAssignments()) {
            if (assignment instanceof EquipmentCrit(int equipmentNumber)) {
                assertFalse(mek.getEquipment(equipmentNumber).getType() instanceof AmmoType,
                      "ammo crits are forbidden");
            }
            if (assignment instanceof MekSystemCrit mekSystemCrit) {
                assertFalse(mekSystemCrit.system() == Mek.SYSTEM_COCKPIT, "cockpit crits are forbidden");
                if (mekSystemCrit.system() == Mek.SYSTEM_ENGINE) {
                    engineCrits++;
                }
                if (mekSystemCrit.system() == Mek.SYSTEM_GYRO) {
                    gyroCrits++;
                }
            }
        }
        assertTrue(engineCrits < 3, "three engine crits would destroy the Mek");
        assertTrue(gyroCrits < 2, "two gyro crits would destroy a standard gyro");
    }

    private void assertEntityUntouched(Entity entity, int expectedTotalRemaining) {
        assertEquals(expectedTotalRemaining, totalRemaining(entity), "the simulation must never modify the entity");
        for (int location = 0; location < entity.locations(); location++) {
            for (int slotIndex = 0; slotIndex < entity.getNumberOfCriticalSlots(location); slotIndex++) {
                CriticalSlot slot = entity.getCritical(location, slotIndex);
                if (slot != null) {
                    assertFalse(slot.isHit(), "the simulation must never mark crit slots on the entity");
                }
            }
        }
    }

    // ----- level math -----

    @Test
    void totalDamageMatchesTheTable() {
        assertEquals(0, PreExistingDamageLevel.NONE.totalDamage(50.0));
        assertEquals(10, PreExistingDamageLevel.LIGHT.totalDamage(50.0));
        assertEquals(20, PreExistingDamageLevel.MODERATE.totalDamage(50.0));
        assertEquals(40, PreExistingDamageLevel.HEAVY.totalDamage(50.0));
        assertEquals(16, PreExistingDamageLevel.HEAVY.totalDamage(20.0));
        // fractional tonnage rounds the 5-ton blocks up
        assertEquals(10, PreExistingDamageLevel.LIGHT.totalDamage(47.5));
    }

    @Test
    void supportedUnitTypesMatchTheRules() throws Exception {
        assertTrue(PreExistingDamageApplier.isSupported(buildMek()));
        assertTrue(PreExistingDamageApplier.isSupported(buildTank()));
        assertTrue(PreExistingDamageApplier.isSupported(buildVtol()));
        assertTrue(PreExistingDamageApplier.isSupported(buildFighter()));
        assertFalse(PreExistingDamageApplier.isSupported(new GunEmplacement()));
    }

    // ----- simulations -----

    @Test
    void noneLevelChangesNothing() throws Exception {
        Mek mek = buildMek();
        int startTotal = totalRemaining(mek);
        PreExistingDamageResult result = PreExistingDamageApplier.simulate(mek, PreExistingDamageLevel.NONE);
        assertEquals(startTotal, totalRemaining(result));
        assertTrue(result.critAssignments().isEmpty());
    }

    @Test
    void lightDamageDealsExactTotalAndNoCrits() throws Exception {
        for (int run = 0; run < SIMULATION_RUNS; run++) {
            Mek mek = buildMek();
            int startTotal = totalRemaining(mek);
            PreExistingDamageResult result = PreExistingDamageApplier.simulate(mek, PreExistingDamageLevel.LIGHT);
            assertEquals(10, startTotal - totalRemaining(result), "light damage on 50 tons is exactly 10 points");
            assertTrue(result.critAssignments().isEmpty(), "light damage never rolls crits");
            assertMekInvariants(mek, result);
            assertEntityUntouched(mek, startTotal);
        }
    }

    @Test
    void moderateDamageOnMekHoldsAllInvariants() throws Exception {
        for (int run = 0; run < SIMULATION_RUNS; run++) {
            Mek mek = buildMek();
            int startTotal = totalRemaining(mek);
            PreExistingDamageResult result = PreExistingDamageApplier.simulate(mek, PreExistingDamageLevel.MODERATE);
            int dealt = startTotal - totalRemaining(result);
            assertTrue(dealt >= 20, "moderate damage on 50 tons is at least 20 points, was " + dealt);
            assertEquals(0, (dealt - 20) % 5, "extra damage only arrives in 5-point disregarded-crit groups");
            assertTrue(!result.critAssignments().isEmpty() || (dealt > 20),
                  "the guaranteed crit is either assigned or converted to extra damage");
            assertMekInvariants(mek, result);
            assertEntityUntouched(mek, startTotal);
        }
    }

    @Test
    void heavyDamageOnMekHoldsAllInvariants() throws Exception {
        for (int run = 0; run < SIMULATION_RUNS; run++) {
            Mek mek = buildMek();
            int startTotal = totalRemaining(mek);
            PreExistingDamageResult result = PreExistingDamageApplier.simulate(mek, PreExistingDamageLevel.HEAVY);
            int dealt = startTotal - totalRemaining(result);
            assertTrue(dealt >= 40, "heavy damage on 50 tons is at least 40 points, was " + dealt);
            assertMekInvariants(mek, result);
            assertEntityUntouched(mek, startTotal);
        }
    }

    /**
     * A side torso on a Light-engine Mek holds two engine slots, so losing one costs two engine hits and the Mek
     * survives on two. A third hit anywhere would destroy the engine, so the simulation must count the slots lost with
     * a destroyed location against the engine before it rolls any further crit.
     */
    @Test
    void lightEngineSideTorsoLossStaysAllowed() throws Exception {
        for (int run = 0; run < SIMULATION_RUNS; run++) {
            Mek mek = buildLightEngineMek();
            int startTotal = totalRemaining(mek);
            PreExistingDamageResult result = PreExistingDamageApplier.simulate(mek, PreExistingDamageLevel.HEAVY);
            int dealt = startTotal - totalRemaining(result);
            assertTrue(dealt >= 40, "heavy damage on 50 tons is at least 40 points, was " + dealt);
            assertMekInvariants(mek, result);
            assertTrue(engineHitsAfter(mek, result) < 3, "the engine must survive with fewer than three hits");
        }
    }

    /**
     * Counts the engine hits the Mek ends up with. Destroying a location destroys every engine slot it carries, so
     * those count in full; a location that survives only counts the engine crits the simulation assigned to it. A slot
     * in a destroyed location must not be counted twice.
     *
     * @param mek    the Mek that was simulated on
     * @param result the simulation output
     *
     * @return the total number of engine critical slots that would be hit
     */
    private int engineHitsAfter(Mek mek, PreExistingDamageResult result) {
        int engineHits = 0;
        for (int location = 0; location < mek.locations(); location++) {
            if (result.internal()[location] == 0) {
                engineHits += mek.getNumberOfCriticalSlots(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, location);
            }
        }
        for (CritAssignment assignment : result.critAssignments()) {
            if ((assignment instanceof MekSystemCrit mekSystemCrit)
                  && (mekSystemCrit.system() == Mek.SYSTEM_ENGINE)
                  && (result.internal()[mekSystemCrit.location()] > 0)) {
                engineHits++;
            }
        }
        return engineHits;
    }

    @Test
    void vehicleLocationsAreNeverDestroyed() throws Exception {
        for (int run = 0; run < SIMULATION_RUNS; run++) {
            Tank tank = buildTank();
            int startTotal = totalRemaining(tank);
            PreExistingDamageResult result = PreExistingDamageApplier.simulate(tank, PreExistingDamageLevel.HEAVY);
            for (int location = 0; location < tank.locations(); location++) {
                if (location == Tank.LOC_BODY) {
                    continue;
                }
                assertTrue(result.internal()[location] > 0,
                      "destroying any combat vehicle location is forbidden");
            }
            int motiveCrits = 0;
            for (CritAssignment assignment : result.critAssignments()) {
                if ((assignment instanceof VehicleCrit vehicleCrit)
                      && (vehicleCrit.kind() == CritAssignment.VehicleCritKind.MOTIVE)) {
                    motiveCrits++;
                }
            }
            assertTrue(motiveCrits <= 3, "a fourth motive hit would immobilize the vehicle");
            assertEntityUntouched(tank, startTotal);
        }
    }

    @Test
    void vtolRotorIsNeverDestroyed() throws Exception {
        for (int run = 0; run < SIMULATION_RUNS; run++) {
            VTOL vtol = buildVtol();
            int startTotal = totalRemaining(vtol);
            PreExistingDamageResult result = PreExistingDamageApplier.simulate(vtol, PreExistingDamageLevel.HEAVY);
            assertTrue(result.internal()[VTOL.LOC_ROTOR] > 0, "destroying the rotor would down the VTOL");
            assertEntityUntouched(vtol, startTotal);
        }
    }

    @Test
    void fighterIsNeverDestroyed() throws Exception {
        for (int run = 0; run < SIMULATION_RUNS; run++) {
            AeroSpaceFighter fighter = buildFighter();
            int startTotal = totalRemaining(fighter);
            PreExistingDamageResult result = PreExistingDamageApplier.simulate(fighter, PreExistingDamageLevel.HEAVY);
            int dealt = startTotal - totalRemaining(result);
            assertTrue(result.structuralIntegrity() > 0, "reducing SI to zero would destroy the fighter");
            assertTrue(dealt >= 40, "heavy damage on 50 tons is at least 40 points, was " + dealt);
            int engineCrits = 0;
            for (CritAssignment assignment : result.critAssignments()) {
                if (assignment instanceof CritAssignment.AeroFighterCrit(CritAssignment.AeroFighterCritKind kind)
                      && (kind == CritAssignment.AeroFighterCritKind.ENGINE)) {
                    engineCrits++;
                }
            }
            assertTrue(engineCrits < 3, "three engine crits would destroy the fighter");
            assertEntityUntouched(fighter, startTotal);
        }
    }

    /**
     * The vehicle and fighter crit pools still contain the unit-killing results (crew killed, engine destroyed, ammo,
     * fuel tank) so those keep their share of the roll probability. They must never reach the output: a forbidden pick
     * is rerolled once and then discarded in favour of a fallback damage group.
     */
    @Test
    void unitKillingCritsNeverReachTheResult() throws Exception {
        for (int run = 0; run < SIMULATION_RUNS; run++) {
            for (Entity entity : new Entity[] { buildTank(), buildVtol(), buildFighter() }) {
                PreExistingDamageResult result = PreExistingDamageApplier.simulate(entity,
                      PreExistingDamageLevel.HEAVY);
                for (CritAssignment assignment : result.critAssignments()) {
                    if (assignment instanceof CritAssignment.EquipmentCrit(int equipmentNumber)) {
                        assertFalse(entity.getEquipment(equipmentNumber).getType() instanceof AmmoType,
                              "an ammo crit would explode the unit");
                    }
                }
            }
        }
    }
}
