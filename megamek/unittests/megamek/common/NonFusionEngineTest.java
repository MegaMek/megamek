/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.equipment.Engine;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for non-fusion engine mechanics per TacOps rules (pg 85). ICE and Fuel Cell powered BattleMeks have special
 * rules: - Generate movement heat (1/2/3 for walk/run/sprint) - Engine crits do NOT generate additional heat (unlike
 * fusion) - Engine crits cause explosion checks instead
 */
class NonFusionEngineTest {

    @Nested
    @DisplayName("Movement Heat Tests - TacOps pg 85")
    class MovementHeatTests {

        @Test
        @DisplayName("ICE Mek walking generates 1 heat")
        void iceMekWalkingGenerates1Heat() {
            Mek mek = mock(Mek.class);
            Engine engine = new Engine(200, Engine.COMBUSTION_ENGINE, 0);

            assertEquals(1, engine.getWalkHeat(mek),
                  "ICE Mek walking should generate 1 heat (per TacOps pg 85)");
        }

        @Test
        @DisplayName("ICE Mek running generates 2 heat")
        void iceMekRunningGenerates2Heat() {
            Mek mek = mock(Mek.class);
            Engine engine = new Engine(200, Engine.COMBUSTION_ENGINE, 0);

            assertEquals(2, engine.getRunHeat(mek),
                  "ICE Mek running should generate 2 heat (per TacOps pg 85)");
        }

        @Test
        @DisplayName("ICE Mek sprinting generates 3 heat")
        void iceMekSprintingGenerates3Heat() {
            Mek mek = mock(Mek.class);
            Engine engine = new Engine(200, Engine.COMBUSTION_ENGINE, 0);

            assertEquals(3, engine.getSprintHeat(mek),
                  "ICE Mek sprinting should generate 3 heat (per TacOps pg 85)");
        }

        @Test
        @DisplayName("Fuel Cell Mek generates same movement heat as ICE")
        void fuelCellMekGeneratesSameHeatAsICE() {
            Mek mek = mock(Mek.class);
            Engine engine = new Engine(200, Engine.FUEL_CELL, 0);

            assertEquals(1, engine.getWalkHeat(mek), "Fuel Cell walk heat");
            assertEquals(2, engine.getRunHeat(mek), "Fuel Cell run heat");
            assertEquals(3, engine.getSprintHeat(mek), "Fuel Cell sprint heat");
        }

        @Test
        @DisplayName("Fusion Mek generates same movement heat as ICE")
        void fusionMekGeneratesSameMovementHeat() {
            Mek mek = mock(Mek.class);
            Engine engine = new Engine(200, Engine.NORMAL_ENGINE, 0);

            assertEquals(1, engine.getWalkHeat(mek), "Fusion walk heat");
            assertEquals(2, engine.getRunHeat(mek), "Fusion run heat");
            assertEquals(3, engine.getSprintHeat(mek), "Fusion sprint heat");
        }

        @Test
        @DisplayName("ICE Vehicle generates 0 movement heat")
        void iceVehicleGeneratesNoMovementHeat() {
            Tank tank = mock(Tank.class);
            Engine engine = new Engine(200, Engine.COMBUSTION_ENGINE, 0);

            assertEquals(0, engine.getWalkHeat(tank),
                  "ICE Vehicle should generate 0 walk heat");
            assertEquals(0, engine.getRunHeat(tank),
                  "ICE Vehicle should generate 0 run heat");
            assertEquals(0, engine.getSprintHeat(tank),
                  "ICE Vehicle should generate 0 sprint heat");
        }

        @Test
        @DisplayName("Fusion Vehicle generates 0 movement heat")
        void fusionVehicleGeneratesNoMovementHeat() {
            Tank tank = mock(Tank.class);
            Engine engine = new Engine(200, Engine.NORMAL_ENGINE, 0);

            assertEquals(0, engine.getWalkHeat(tank),
                  "Fusion Vehicle should generate 0 walk heat");
            assertEquals(0, engine.getRunHeat(tank),
                  "Fusion Vehicle should generate 0 run heat");
        }

        @Test
        @DisplayName("Non-Mek non-Tank entity generates 0 heat from ICE")
        void otherEntityGeneratesNoHeatFromICE() {
            // Generic entity that isn't a Mek
            Entity entity = mock(Entity.class);
            Engine engine = new Engine(200, Engine.COMBUSTION_ENGINE, 0);

            assertEquals(0, engine.getWalkHeat(entity),
                  "Non-Mek entity should generate 0 walk heat from ICE");
            assertEquals(0, engine.getRunHeat(entity),
                  "Non-Mek entity should generate 0 run heat from ICE");
        }
    }

    @Nested
    @DisplayName("Engine Type Detection Tests")
    class EngineTypeTests {

        @Test
        @DisplayName("Standard fusion engine is detected as fusion")
        void standardFusionIsFusion() {
            Engine engine = new Engine(200, Engine.NORMAL_ENGINE, 0);
            assertTrue(engine.isFusion(), "Standard engine should be fusion");
        }

        @Test
        @DisplayName("XL fusion engine is detected as fusion")
        void xlFusionIsFusion() {
            Engine engine = new Engine(200, Engine.XL_ENGINE, 0);
            assertTrue(engine.isFusion(), "XL engine should be fusion");
        }

        @Test
        @DisplayName("Light fusion engine is detected as fusion")
        void lightFusionIsFusion() {
            Engine engine = new Engine(200, Engine.LIGHT_ENGINE, 0);
            assertTrue(engine.isFusion(), "Light engine should be fusion");
        }

        @Test
        @DisplayName("XXL fusion engine is detected as fusion")
        void xxlFusionIsFusion() {
            Engine engine = new Engine(200, Engine.XXL_ENGINE, 0);
            assertTrue(engine.isFusion(), "XXL engine should be fusion");
        }

        @Test
        @DisplayName("Compact fusion engine is detected as fusion")
        void compactFusionIsFusion() {
            Engine engine = new Engine(200, Engine.COMPACT_ENGINE, 0);
            assertTrue(engine.isFusion(), "Compact engine should be fusion");
        }

        @Test
        @DisplayName("ICE engine is not fusion")
        void iceIsNotFusion() {
            Engine engine = new Engine(200, Engine.COMBUSTION_ENGINE, 0);
            assertFalse(engine.isFusion(), "ICE should not be fusion");
        }

        @Test
        @DisplayName("Fuel Cell engine is not fusion")
        void fuelCellIsNotFusion() {
            Engine engine = new Engine(200, Engine.FUEL_CELL, 0);
            assertFalse(engine.isFusion(), "Fuel Cell should not be fusion");
        }

        @Test
        @DisplayName("Fission engine is not fusion")
        void fissionIsNotFusion() {
            Engine engine = new Engine(200, Engine.FISSION, 0);
            assertFalse(engine.isFusion(), "Fission should not be fusion");
        }
    }

    @Nested
    @DisplayName("Engine Crit Heat Behavior Tests")
    class EngineCritHeatBehaviorTests {

        /**
         * Verifies that the isFusion() check in getEngineCritHeat() will correctly exclude ICE engines from generating
         * heat. This tests the conditional branch that guards engine crit heat generation.
         */
        @Test
        @DisplayName("ICE engine isFusion() returns false - guards engine crit heat")
        void iceEngineFusionCheckReturnsFalse() {
            Engine iceEngine = new Engine(200, Engine.COMBUSTION_ENGINE, 0);
            Engine fusionEngine = new Engine(200, Engine.NORMAL_ENGINE, 0);

            // The isFusion() check is what prevents ICE engines from generating crit heat
            // in Mek.getEngineCritHeat(): if (!isShutDown() && getEngine().isFusion())
            assertFalse(iceEngine.isFusion(),
                  "ICE isFusion() should return false, preventing engine crit heat generation");
            assertTrue(fusionEngine.isFusion(),
                  "Fusion isFusion() should return true, allowing engine crit heat generation");
        }

        @Test
        @DisplayName("Fuel Cell engine isFusion() returns false - guards engine crit heat")
        void fuelCellEngineFusionCheckReturnsFalse() {
            Engine fuelCellEngine = new Engine(200, Engine.FUEL_CELL, 0);

            assertFalse(fuelCellEngine.isFusion(),
                  "Fuel Cell isFusion() should return false, preventing engine crit heat generation");
        }

        @Test
        @DisplayName("Fission engine isFusion() returns false - guards engine crit heat")
        void fissionEngineFusionCheckReturnsFalse() {
            Engine fissionEngine = new Engine(200, Engine.FISSION, 0);

            assertFalse(fissionEngine.isFusion(),
                  "Fission isFusion() should return false, preventing engine crit heat generation");
        }
    }

    @Nested
    @DisplayName("XXL Engine Heat Tests")
    class XXLEngineHeatTests {

        @Test
        @DisplayName("XXL engine generates 4 heat when walking (2x normal)")
        void xxlEngineWalkHeatIsDoubled() {
            Mek mek = mock(Mek.class);
            Engine engine = new Engine(200, Engine.XXL_ENGINE, 0);

            assertEquals(4, engine.getWalkHeat(mek),
                  "XXL engine should generate 4 heat when walking (2x normal)");
        }

        @Test
        @DisplayName("XXL engine generates 6 heat when running (2x normal)")
        void xxlEngineRunHeatIsDoubled() {
            Mek mek = mock(Mek.class);
            Engine engine = new Engine(200, Engine.XXL_ENGINE, 0);

            assertEquals(6, engine.getRunHeat(mek),
                  "XXL engine should generate 6 heat when running (2x normal)");
        }

        @Test
        @DisplayName("XXL engine generates 9 heat when sprinting (3x normal)")
        void xxlEngineSprintHeatIsTripled() {
            Mek mek = mock(Mek.class);
            Engine engine = new Engine(200, Engine.XXL_ENGINE, 0);

            assertEquals(9, engine.getSprintHeat(mek),
                  "XXL engine should generate 9 heat when sprinting (3x normal)");
        }

        @Test
        @DisplayName("XXL engine with SCM generates 0 heat")
        void xxlEngineWithSCMGeneratesNoHeat() {
            Mek mek = mock(Mek.class);
            when(mek.hasWorkingSCM()).thenReturn(true);
            Engine engine = new Engine(200, Engine.XXL_ENGINE, 0);

            assertEquals(0, engine.getWalkHeat(mek),
                  "XXL engine with SCM should generate 0 walk heat");
            assertEquals(0, engine.getRunHeat(mek),
                  "XXL engine with SCM should generate 0 run heat");
            assertEquals(0, engine.getSprintHeat(mek),
                  "XXL engine with SCM should generate 0 sprint heat");
        }
    }

    @Nested
    @DisplayName("Super-Cooled Myomer (SCM) Heat Tests")
    class SCMHeatTests {

        @Test
        @DisplayName("Fusion Mek with SCM generates 0 movement heat")
        void fusionMekWithSCMGeneratesNoHeat() {
            Mek mek = mock(Mek.class);
            when(mek.hasWorkingSCM()).thenReturn(true);
            Engine engine = new Engine(200, Engine.NORMAL_ENGINE, 0);

            assertEquals(0, engine.getWalkHeat(mek),
                  "Fusion Mek with SCM should generate 0 walk heat");
            assertEquals(0, engine.getRunHeat(mek),
                  "Fusion Mek with SCM should generate 0 run heat");
            assertEquals(0, engine.getSprintHeat(mek),
                  "Fusion Mek with SCM should generate 0 sprint heat");
        }

        @Test
        @DisplayName("ICE Mek with SCM still generates movement heat")
        void iceMekWithSCMStillGeneratesHeat() {
            // SCM doesn't help ICE because ICE heat isn't from the engine running hot
            // It's from mechanical friction. But let's verify the code behavior.
            Mek mek = mock(Mek.class);
            when(mek.hasWorkingSCM()).thenReturn(true);
            Engine engine = new Engine(200, Engine.COMBUSTION_ENGINE, 0);

            // ICE doesn't use SCM check - it always generates heat for Meks
            assertEquals(1, engine.getWalkHeat(mek),
                  "ICE Mek generates walk heat regardless of SCM");
            assertEquals(2, engine.getRunHeat(mek),
                  "ICE Mek generates run heat regardless of SCM");
            assertEquals(3, engine.getSprintHeat(mek),
                  "ICE Mek generates sprint heat regardless of SCM");
        }
    }
}
