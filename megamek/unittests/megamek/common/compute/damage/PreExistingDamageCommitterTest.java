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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.common.CriticalSlot;
import megamek.common.compute.damage.CritAssignment.AeroFighterCrit;
import megamek.common.compute.damage.CritAssignment.AeroFighterCritKind;
import megamek.common.compute.damage.CritAssignment.EquipmentCrit;
import megamek.common.compute.damage.CritAssignment.MekSystemCrit;
import megamek.common.compute.damage.CritAssignment.VehicleCrit;
import megamek.common.compute.damage.CritAssignment.VehicleCritKind;
import megamek.common.equipment.Engine;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.GunEmplacement;
import megamek.common.units.Aero;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.BipedMek;
import megamek.common.units.DamageEditApplier;
import megamek.common.units.DamageEditSpec;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PreExistingDamageCommitter}: the rolled pre-existing damage must reach the unit exactly as the unit
 * editor would commit it, and nothing else about the unit may change.
 *
 * @author Illiani
 * @since 0.51.01
 */
class PreExistingDamageCommitterTest {
    private static final int SIMULATION_RUNS = 100;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    // ----- fixtures -----

    private static Mek buildMek() throws Exception {
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

    private static Tank buildTank() throws Exception {
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

    private static AeroSpaceFighter buildFighter() throws Exception {
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

    private static int totalRemaining(PreExistingDamageResult result, Entity entity) {
        int remaining = result.structuralIntegrity();
        for (int location = 0; location < result.armor().length; location++) {
            remaining += result.armor()[location];
            if (entity.hasRearArmor(location)) {
                remaining += result.rearArmor()[location];
            }
            if (!(entity instanceof Aero)) {
                remaining += result.internal()[location];
            }
        }
        return remaining;
    }

    private static int totalRemaining(Entity entity) {
        int remaining = 0;
        if (entity instanceof Aero aero) {
            remaining += Math.max(aero.getSI(), 0);
        }
        for (int location = 0; location < entity.locations(); location++) {
            remaining += Math.max(entity.getArmor(location, false), 0);
            if (entity.hasRearArmor(location)) {
                remaining += Math.max(entity.getArmor(location, true), 0);
            }
            if (!(entity instanceof Aero)) {
                remaining += Math.max(entity.getInternal(location), 0);
            }
        }
        return remaining;
    }

    private static int countDamagedSlots(Entity entity) {
        int damagedSlots = 0;
        for (int location = 0; location < entity.locations(); location++) {
            for (int slotIndex = 0; slotIndex < entity.getNumberOfCriticalSlots(location); slotIndex++) {
                CriticalSlot slot = entity.getCritical(location, slotIndex);
                if ((slot != null) && (slot.isHit() || slot.isDestroyed())) {
                    damagedSlots++;
                }
            }
        }
        return damagedSlots;
    }

    // ----- committing a rolled result -----

    @Test
    void committedMekMatchesTheRolledValuesAndCrits() throws Exception {
        for (int run = 0; run < SIMULATION_RUNS; run++) {
            Mek mek = buildMek();
            PreExistingDamageResult result = PreExistingDamageApplier.simulate(mek, PreExistingDamageLevel.HEAVY);
            int expectedRemaining = totalRemaining(result, mek);

            new DamageEditApplier(mek, PreExistingDamageCommitter.buildSpec(mek, result)).applyToEntity();

            assertEquals(expectedRemaining, totalRemaining(mek), "armor and structure must match the roll");
            assertEquals(result.critAssignments().size(), countDamagedSlots(mek),
                  "each rolled critical hit must mark exactly one slot");
            assertFalse(mek.isDestroyed(), "pre-existing damage must never destroy the unit");
        }
    }

    @Test
    void committedTankMatchesTheRolledValues() throws Exception {
        for (int run = 0; run < SIMULATION_RUNS; run++) {
            Tank tank = buildTank();
            PreExistingDamageResult result = PreExistingDamageApplier.simulate(tank, PreExistingDamageLevel.HEAVY);
            int expectedRemaining = totalRemaining(result, tank);

            new DamageEditApplier(tank, PreExistingDamageCommitter.buildSpec(tank, result)).applyToEntity();

            assertEquals(expectedRemaining, totalRemaining(tank));
            assertFalse(tank.isImmobile(false), "pre-existing damage must never immobilize the unit");
        }
    }

    @Test
    void committedFighterMatchesTheRolledValues() throws Exception {
        for (int run = 0; run < SIMULATION_RUNS; run++) {
            AeroSpaceFighter fighter = buildFighter();
            PreExistingDamageResult result = PreExistingDamageApplier.simulate(fighter,
                  PreExistingDamageLevel.HEAVY);
            int expectedRemaining = totalRemaining(result, fighter);

            new DamageEditApplier(fighter, PreExistingDamageCommitter.buildSpec(fighter, result)).applyToEntity();

            assertEquals(expectedRemaining, totalRemaining(fighter));
        }
    }

    @Test
    void rollAndApplyDamagesASupportedUnit() throws Exception {
        Mek mek = buildMek();
        int startingRemaining = totalRemaining(mek);

        assertTrue(PreExistingDamageCommitter.rollAndApply(mek, PreExistingDamageLevel.LIGHT));

        assertEquals(startingRemaining - PreExistingDamageLevel.LIGHT.totalDamage(mek.getWeight()),
              totalRemaining(mek));
    }

    @Test
    void rollAndApplyIgnoresNoneAndUnsupportedUnits() throws Exception {
        Mek mek = buildMek();
        int startingRemaining = totalRemaining(mek);

        assertFalse(PreExistingDamageCommitter.rollAndApply(mek, PreExistingDamageLevel.NONE));
        assertEquals(startingRemaining, totalRemaining(mek));

        assertFalse(PreExistingDamageCommitter.rollAndApply(new GunEmplacement(), PreExistingDamageLevel.HEAVY));
    }

    // ----- building the spec -----

    @Test
    void unchangedLocationsAreLeftOutOfTheSpec() throws Exception {
        Mek mek = buildMek();
        int[] armor = new int[mek.locations()];
        int[] rearArmor = new int[mek.locations()];
        int[] internal = new int[mek.locations()];
        for (int location = 0; location < mek.locations(); location++) {
            armor[location] = mek.getArmor(location, false);
            rearArmor[location] = mek.hasRearArmor(location) ? mek.getArmor(location, true) : 0;
            internal[location] = mek.getInternal(location);
        }
        armor[Mek.LOC_LEFT_ARM] -= 5;
        PreExistingDamageResult result = new PreExistingDamageResult(armor, rearArmor, internal, 0, List.of());

        DamageEditSpec spec = PreExistingDamageCommitter.buildSpec(mek, result);

        assertEquals(armor[Mek.LOC_LEFT_ARM], spec.armor[Mek.LOC_LEFT_ARM]);
        assertNull(spec.armor[Mek.LOC_RIGHT_ARM]);
        assertNull(spec.rearArmor[Mek.LOC_CENTER_TORSO]);
        assertNull(spec.internal[Mek.LOC_LEFT_ARM]);
    }

    @Test
    void critAssignmentsAddToTheUnitsExistingHits() throws Exception {
        Mek mek = buildMek();
        int laserNumber = mek.getEquipmentNum(mek.getWeaponList().get(0));
        PreExistingDamageResult result = unchangedResult(mek, List.of(
              new EquipmentCrit(laserNumber),
              new MekSystemCrit(Mek.SYSTEM_ENGINE, Mek.LOC_CENTER_TORSO),
              new MekSystemCrit(Mek.ACTUATOR_UPPER_ARM, Mek.LOC_LEFT_ARM),
              new MekSystemCrit(Mek.ACTUATOR_UPPER_LEG, Mek.LOC_RIGHT_LEG)));

        DamageEditSpec spec = PreExistingDamageCommitter.buildSpec(mek, result);

        assertEquals(1, spec.equipmentHits.get(laserNumber));
        assertEquals(1, spec.centerEngineHits);
        assertEquals(1, spec.actuatorHits[Mek.LOC_LEFT_ARM - Mek.LOC_RIGHT_ARM]
              [Mek.ACTUATOR_UPPER_ARM - Mek.ACTUATOR_SHOULDER]);
        assertEquals(1, spec.actuatorHits[Mek.LOC_RIGHT_LEG - Mek.LOC_RIGHT_ARM]
              [Mek.ACTUATOR_UPPER_LEG - Mek.ACTUATOR_HIP]);
        assertNull(spec.gyroHits);
    }

    @Test
    void vehicleAndFighterCritsBecomeTheirSystemHits() throws Exception {
        Tank tank = buildTank();
        DamageEditSpec tankSpec = PreExistingDamageCommitter.buildSpec(tank, unchangedResult(tank, List.of(
              new VehicleCrit(VehicleCritKind.MOTIVE, Entity.LOC_NONE),
              new VehicleCrit(VehicleCritKind.MOTIVE, Entity.LOC_NONE),
              new VehicleCrit(VehicleCritKind.SENSORS, Entity.LOC_NONE))));

        assertEquals(2, tankSpec.motiveHits);
        assertEquals(1, tankSpec.sensorHits);

        AeroSpaceFighter fighter = buildFighter();
        DamageEditSpec fighterSpec = PreExistingDamageCommitter.buildSpec(fighter, unchangedResult(fighter, List.of(
              new AeroFighterCrit(AeroFighterCritKind.AVIONICS),
              new AeroFighterCrit(AeroFighterCritKind.LANDING_GEAR))));

        assertEquals(1, fighterSpec.avionicsHits);
        assertEquals(1, fighterSpec.gearHits);
        assertNull(fighterSpec.engineHits);
    }

    /** A result that leaves armor and structure as they are and only adds the given critical hits. */
    private static PreExistingDamageResult unchangedResult(Entity entity, List<CritAssignment> critAssignments) {
        int[] armor = new int[entity.locations()];
        int[] rearArmor = new int[entity.locations()];
        int[] internal = new int[entity.locations()];
        for (int location = 0; location < entity.locations(); location++) {
            armor[location] = Math.max(entity.getArmor(location, false), 0);
            rearArmor[location] = entity.hasRearArmor(location) ? Math.max(entity.getArmor(location, true), 0) : 0;
            internal[location] = Math.max(entity.getInternal(location), 0);
        }
        int structuralIntegrity = (entity instanceof Aero aero) ? aero.getSI() : 0;
        return new PreExistingDamageResult(armor, rearArmor, internal, structuralIntegrity, critAssignments);
    }
}
