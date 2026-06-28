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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for Combat Vehicle Escape Pod (CVEP) functionality per TO:AUE p.121 rules.
 * <p>
 * CVEP allows combat vehicle crews to escape during the movement phase. Requirements: - Only Combat Vehicles can mount
 * CVEPs - Pod must be undamaged (not destroyed, breached, or missing) - Crew must be alive and not already ejected -
 * Vehicle must not be destroyed or doomed
 * <p>
 * Launch process: - Piloting Skill Roll +2 for launch - Pod travels up to 4 hexes behind vehicle - Landing roll
 * (MekWarrior Ejection roll) +2 - Crew becomes foot infantry after landing - Vehicle is destroyed after launch
 */
class CombatVehicleEscapePodTest {

    private Crew mockCrew;
    private MiscMounted mockCvepMount;
    private MiscType mockCvepType;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        mockCrew = mock(Crew.class);
        mockCvepMount = mock(MiscMounted.class);
        mockCvepType = mock(MiscType.class);

        when(mockCvepMount.getType()).thenReturn(mockCvepType);
        when(mockCvepType.hasFlag(MiscType.F_COMBAT_VEHICLE_ESCAPE_POD)).thenReturn(true);
    }

    @Nested
    @DisplayName("hasCombatVehicleEscapePod Detection")
    class HasCvepTests {

        @Test
        @DisplayName("Tank has CVEP when equipped with one")
        void hasCvep_WhenEquipped_ReturnsTrue() {
            Tank tank = new Tank() {
                @Override
                public java.util.List<MiscMounted> getMisc() {
                    return java.util.List.of(mockCvepMount);
                }
            };

            assertTrue(tank.hasCombatVehicleEscapePod(),
                  "Tank should report having CVEP when equipped");
        }

        @Test
        @DisplayName("Tank without CVEP returns false")
        void hasCvep_WhenNotEquipped_ReturnsFalse() {
            Tank tank = new Tank();

            assertFalse(tank.hasCombatVehicleEscapePod(),
                  "Tank should not report having CVEP when not equipped");
        }

        @Test
        @DisplayName("Tank with non-CVEP equipment returns false")
        void hasCvep_WithOtherEquipment_ReturnsFalse() {
            MiscMounted otherMount = mock(MiscMounted.class);
            MiscType otherType = mock(MiscType.class);
            when(otherMount.getType()).thenReturn(otherType);
            when(otherType.hasFlag(MiscType.F_COMBAT_VEHICLE_ESCAPE_POD)).thenReturn(false);

            Tank tank = new Tank() {
                @Override
                public java.util.List<MiscMounted> getMisc() {
                    return java.util.List.of(otherMount);
                }
            };

            assertFalse(tank.hasCombatVehicleEscapePod(),
                  "Tank should not report having CVEP when only other equipment present");
        }
    }

    @Nested
    @DisplayName("hasUndamagedCombatVehicleEscapePod Detection")
    class HasUndamagedCvepTests {

        @Test
        @DisplayName("Undamaged CVEP returns true")
        void hasUndamagedCvep_WhenUndamaged_ReturnsTrue() {
            when(mockCvepMount.isDestroyed()).thenReturn(false);
            when(mockCvepMount.isBreached()).thenReturn(false);
            when(mockCvepMount.isMissing()).thenReturn(false);

            Tank tank = new Tank() {
                @Override
                public java.util.List<MiscMounted> getMisc() {
                    return java.util.List.of(mockCvepMount);
                }
            };

            assertTrue(tank.hasUndamagedCombatVehicleEscapePod(),
                  "Should return true when CVEP is undamaged");
        }

        @Test
        @DisplayName("Destroyed CVEP returns false")
        void hasUndamagedCvep_WhenDestroyed_ReturnsFalse() {
            when(mockCvepMount.isDestroyed()).thenReturn(true);
            when(mockCvepMount.isBreached()).thenReturn(false);
            when(mockCvepMount.isMissing()).thenReturn(false);

            Tank tank = new Tank() {
                @Override
                public java.util.List<MiscMounted> getMisc() {
                    return java.util.List.of(mockCvepMount);
                }
            };

            assertFalse(tank.hasUndamagedCombatVehicleEscapePod(),
                  "Should return false when CVEP is destroyed");
        }

        @Test
        @DisplayName("Breached CVEP returns false")
        void hasUndamagedCvep_WhenBreached_ReturnsFalse() {
            when(mockCvepMount.isDestroyed()).thenReturn(false);
            when(mockCvepMount.isBreached()).thenReturn(true);
            when(mockCvepMount.isMissing()).thenReturn(false);

            Tank tank = new Tank() {
                @Override
                public java.util.List<MiscMounted> getMisc() {
                    return java.util.List.of(mockCvepMount);
                }
            };

            assertFalse(tank.hasUndamagedCombatVehicleEscapePod(),
                  "Should return false when CVEP is breached");
        }

        @Test
        @DisplayName("Missing CVEP returns false")
        void hasUndamagedCvep_WhenMissing_ReturnsFalse() {
            when(mockCvepMount.isDestroyed()).thenReturn(false);
            when(mockCvepMount.isBreached()).thenReturn(false);
            when(mockCvepMount.isMissing()).thenReturn(true);

            Tank tank = new Tank() {
                @Override
                public java.util.List<MiscMounted> getMisc() {
                    return java.util.List.of(mockCvepMount);
                }
            };

            assertFalse(tank.hasUndamagedCombatVehicleEscapePod(),
                  "Should return false when CVEP is missing");
        }

        @Test
        @DisplayName("No CVEP returns false")
        void hasUndamagedCvep_WhenNoCvep_ReturnsFalse() {
            Tank tank = new Tank();

            assertFalse(tank.hasUndamagedCombatVehicleEscapePod(),
                  "Should return false when no CVEP equipped");
        }
    }

    @Nested
    @DisplayName("canLaunchEscapePod Conditions")
    class CanLaunchEscapePodTests {

        @Test
        @DisplayName("Can launch when all conditions met")
        void canLaunch_AllConditionsMet_ReturnsTrue() {
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(false);
            when(mockCvepMount.isDestroyed()).thenReturn(false);
            when(mockCvepMount.isBreached()).thenReturn(false);
            when(mockCvepMount.isMissing()).thenReturn(false);

            Tank tank = new Tank() {
                @Override
                public java.util.List<MiscMounted> getMisc() {
                    return java.util.List.of(mockCvepMount);
                }
            };
            tank.setCrew(mockCrew);

            assertTrue(tank.canLaunchEscapePod(),
                  "Should be able to launch when all conditions met");
        }

        @Test
        @DisplayName("Cannot launch when no crew")
        void canLaunch_NoCrew_ReturnsFalse() {
            when(mockCvepMount.isDestroyed()).thenReturn(false);
            when(mockCvepMount.isBreached()).thenReturn(false);
            when(mockCvepMount.isMissing()).thenReturn(false);

            Tank tank = new Tank() {
                @Override
                public java.util.List<MiscMounted> getMisc() {
                    return java.util.List.of(mockCvepMount);
                }
            };
            tank.setCrew(null);  // Explicitly remove the default crew

            assertFalse(tank.canLaunchEscapePod(),
                  "Cannot launch when no crew");
        }

        @Test
        @DisplayName("Cannot launch when crew already ejected")
        void canLaunch_CrewEjected_ReturnsFalse() {
            when(mockCrew.isEjected()).thenReturn(true);
            when(mockCrew.isDead()).thenReturn(false);
            when(mockCvepMount.isDestroyed()).thenReturn(false);
            when(mockCvepMount.isBreached()).thenReturn(false);
            when(mockCvepMount.isMissing()).thenReturn(false);

            Tank tank = new Tank() {
                @Override
                public java.util.List<MiscMounted> getMisc() {
                    return java.util.List.of(mockCvepMount);
                }
            };
            tank.setCrew(mockCrew);

            assertFalse(tank.canLaunchEscapePod(),
                  "Cannot launch when crew already ejected");
        }

        @Test
        @DisplayName("Cannot launch when crew is dead")
        void canLaunch_CrewDead_ReturnsFalse() {
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(true);
            when(mockCvepMount.isDestroyed()).thenReturn(false);
            when(mockCvepMount.isBreached()).thenReturn(false);
            when(mockCvepMount.isMissing()).thenReturn(false);

            Tank tank = new Tank() {
                @Override
                public java.util.List<MiscMounted> getMisc() {
                    return java.util.List.of(mockCvepMount);
                }
            };
            tank.setCrew(mockCrew);

            assertFalse(tank.canLaunchEscapePod(),
                  "Cannot launch when crew is dead");
        }

        @Test
        @DisplayName("Cannot launch when CVEP is destroyed")
        void canLaunch_CvepDestroyed_ReturnsFalse() {
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(false);
            when(mockCvepMount.isDestroyed()).thenReturn(true);
            when(mockCvepMount.isBreached()).thenReturn(false);
            when(mockCvepMount.isMissing()).thenReturn(false);

            Tank tank = new Tank() {
                @Override
                public java.util.List<MiscMounted> getMisc() {
                    return java.util.List.of(mockCvepMount);
                }
            };
            tank.setCrew(mockCrew);

            assertFalse(tank.canLaunchEscapePod(),
                  "Cannot launch when CVEP is destroyed");
        }

        @Test
        @DisplayName("Cannot launch when no CVEP equipped")
        void canLaunch_NoCvep_ReturnsFalse() {
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(false);

            Tank tank = new Tank();
            tank.setCrew(mockCrew);

            assertFalse(tank.canLaunchEscapePod(),
                  "Cannot launch when no CVEP equipped");
        }

        @Test
        @DisplayName("Cannot launch when vehicle is destroyed")
        void canLaunch_VehicleDestroyed_ReturnsFalse() {
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(false);
            when(mockCvepMount.isDestroyed()).thenReturn(false);
            when(mockCvepMount.isBreached()).thenReturn(false);
            when(mockCvepMount.isMissing()).thenReturn(false);

            Tank tank = new Tank() {
                @Override
                public java.util.List<MiscMounted> getMisc() {
                    return java.util.List.of(mockCvepMount);
                }

                @Override
                public boolean isDestroyed() {
                    return true;
                }
            };
            tank.setCrew(mockCrew);

            assertFalse(tank.canLaunchEscapePod(),
                  "Cannot launch when vehicle is destroyed");
        }

        @Test
        @DisplayName("Cannot launch when vehicle is doomed")
        void canLaunch_VehicleDoomed_ReturnsFalse() {
            when(mockCrew.isEjected()).thenReturn(false);
            when(mockCrew.isDead()).thenReturn(false);
            when(mockCvepMount.isDestroyed()).thenReturn(false);
            when(mockCvepMount.isBreached()).thenReturn(false);
            when(mockCvepMount.isMissing()).thenReturn(false);

            Tank tank = new Tank() {
                @Override
                public java.util.List<MiscMounted> getMisc() {
                    return java.util.List.of(mockCvepMount);
                }

                @Override
                public boolean isDoomed() {
                    return true;
                }
            };
            tank.setCrew(mockCrew);

            assertFalse(tank.canLaunchEscapePod(),
                  "Cannot launch when vehicle is doomed");
        }
    }

    @Nested
    @DisplayName("CVEP Damage Model Tests (TO:AUE p.121)")
    class CvepDamageModelTests {

        /**
         * Creates a CVEP using the default constructor for testing damage mechanics. This avoids the complexity of
         * setting up a full Tank with crew.
         */
        private CombatVehicleEscapePod createTestCvep() {
            CombatVehicleEscapePod cvep = new CombatVehicleEscapePod();
            cvep.setPosition(new Coords(5, 5));
            return cvep;
        }

        @Test
        @DisplayName("New CVEP has zero cumulative damage")
        void newCvep_HasZeroDamage() {
            CombatVehicleEscapePod cvep = createTestCvep();

            assertEquals(0, cvep.getCumulativeDamage(),
                  "New CVEP should have zero cumulative damage");
            assertFalse(cvep.isBreached(),
                  "New CVEP should not be breached");
        }

        @Test
        @DisplayName("CVEP survives 1 point of damage")
        void cvep_Survives1Damage() {
            CombatVehicleEscapePod cvep = createTestCvep();

            boolean breached = cvep.applyDamage(1);

            assertFalse(breached, "CVEP should not be breached by 1 damage");
            assertEquals(1, cvep.getCumulativeDamage(), "CVEP should track 1 damage");
            assertFalse(cvep.isBreached(), "CVEP should not be breached");
        }

        @Test
        @DisplayName("CVEP survives 2 points of damage")
        void cvep_Survives2Damage() {
            CombatVehicleEscapePod cvep = createTestCvep();

            boolean breached = cvep.applyDamage(2);

            assertFalse(breached, "CVEP should not be breached by exactly 2 damage");
            assertEquals(2, cvep.getCumulativeDamage(), "CVEP should track 2 damage");
            assertFalse(cvep.isBreached(), "CVEP should not be breached");
        }

        @Test
        @DisplayName("CVEP is breached by 3 points of damage")
        void cvep_BreachedBy3Damage() {
            CombatVehicleEscapePod cvep = createTestCvep();

            boolean breached = cvep.applyDamage(3);

            assertTrue(breached, "CVEP should be breached by 3 damage (more than 2)");
            assertEquals(3, cvep.getCumulativeDamage(), "CVEP should track 3 damage");
            assertTrue(cvep.isBreached(), "CVEP should be breached");
        }

        @Test
        @DisplayName("CVEP cumulative damage - 1 + 1 = survives")
        void cvep_CumulativeDamage_1Plus1_Survives() {
            CombatVehicleEscapePod cvep = createTestCvep();

            cvep.applyDamage(1);
            boolean breached = cvep.applyDamage(1);

            assertFalse(breached, "CVEP should survive 1+1=2 cumulative damage");
            assertEquals(2, cvep.getCumulativeDamage(), "CVEP should track 2 cumulative damage");
            assertFalse(cvep.isBreached(), "CVEP should not be breached");
        }

        @Test
        @DisplayName("CVEP cumulative damage - 1 + 1 + 1 = breached")
        void cvep_CumulativeDamage_1Plus1Plus1_Breached() {
            CombatVehicleEscapePod cvep = createTestCvep();

            cvep.applyDamage(1);
            cvep.applyDamage(1);
            boolean breached = cvep.applyDamage(1);

            assertTrue(breached, "CVEP should be breached by 1+1+1=3 cumulative damage");
            assertEquals(3, cvep.getCumulativeDamage(), "CVEP should track 3 cumulative damage");
            assertTrue(cvep.isBreached(), "CVEP should be breached");
        }

        @Test
        @DisplayName("CVEP cumulative damage - 2 + 1 = breached")
        void cvep_CumulativeDamage_2Plus1_Breached() {
            CombatVehicleEscapePod cvep = createTestCvep();

            cvep.applyDamage(2);
            boolean breached = cvep.applyDamage(1);

            assertTrue(breached, "CVEP should be breached by 2+1=3 cumulative damage");
            assertTrue(cvep.isBreached(), "CVEP should be breached");
        }

        @Test
        @DisplayName("Breached CVEP ignores additional damage")
        void cvep_BreachedIgnoresAdditionalDamage() {
            CombatVehicleEscapePod cvep = createTestCvep();

            cvep.applyDamage(3); // Breach
            int damageAfterBreach = cvep.getCumulativeDamage();

            boolean result = cvep.applyDamage(5); // Additional damage after breach

            assertFalse(result, "applyDamage should return false when already breached");
            assertEquals(damageAfterBreach, cvep.getCumulativeDamage(),
                  "Cumulative damage should not increase after breach");
        }

        @Test
        @DisplayName("Zero damage does not affect CVEP")
        void cvep_ZeroDamage_NoEffect() {
            CombatVehicleEscapePod cvep = createTestCvep();

            boolean breached = cvep.applyDamage(0);

            assertFalse(breached, "Zero damage should not breach CVEP");
            assertEquals(0, cvep.getCumulativeDamage(), "Zero damage should not be tracked");
        }

        @Test
        @DisplayName("Negative damage does not affect CVEP")
        void cvep_NegativeDamage_NoEffect() {
            CombatVehicleEscapePod cvep = createTestCvep();

            boolean breached = cvep.applyDamage(-5);

            assertFalse(breached, "Negative damage should not breach CVEP");
            assertEquals(0, cvep.getCumulativeDamage(), "Negative damage should not be tracked");
        }

        @Test
        @DisplayName("CVEP is always immobile per TO:AUE p.121")
        void cvep_AlwaysImmobile() {
            CombatVehicleEscapePod cvep = createTestCvep();

            assertTrue(cvep.isImmobile(), "CVEP should always be immobile for targeting purposes");

            // Even if crew is outside, the pod itself is still immobile
            cvep.setCrewInside(false);
            assertTrue(cvep.isImmobile(), "Empty CVEP should still be immobile");
        }

        @Test
        @DisplayName("Breached CVEP cannot have crew exit")
        void cvep_Breached_CrewCannotExit() {
            CombatVehicleEscapePod cvep = createTestCvep();
            cvep.applyDamage(3); // Breach the pod

            assertFalse(cvep.canCrewExit(),
                  "Crew cannot exit a breached CVEP (they are dead)");
        }

        @Test
        @DisplayName("BREACH_THRESHOLD constant is 2")
        void cvep_BreachThresholdIs2() {
            assertEquals(2, CombatVehicleEscapePod.BREACH_THRESHOLD,
                  "BREACH_THRESHOLD should be 2 per TO:AUE p.121");
        }
    }

    @Nested
    @DisplayName("CVEP Critical Hit Vulnerability Tests (TO:AUE p.121)")
    class CvepCriticalHitTests {

        /**
         * Per TO:AUE p.121: "CVEP is treated as a weapon item in the rear and may thus be damaged by any weapon
         * critical hits to that location."
         *
         * <p>This requires tankSlots = 1 so it occupies a hittable slot.</p>
         */
        @Test
        @DisplayName("CVEP MiscType has tankSlots = 1 for critical hit vulnerability")
        void cvepMiscType_HasTankSlots() {
            MiscType cvepType = (MiscType) EquipmentType.get("ISCombatVehicleEscapePod");

            assertEquals(1, cvepType.getTankSlots(null),
                  "CVEP should have tankSlots = 1 to be hittable by rear criticals per TO:AUE p.121");
        }

        @Test
        @DisplayName("CVEP MiscType has F_COMBAT_VEHICLE_ESCAPE_POD flag")
        void cvepMiscType_HasCorrectFlag() {
            MiscType cvepType = (MiscType) EquipmentType.get("ISCombatVehicleEscapePod");

            assertTrue(cvepType.hasFlag(MiscType.F_COMBAT_VEHICLE_ESCAPE_POD),
                  "CVEP should have F_COMBAT_VEHICLE_ESCAPE_POD flag");
        }

        @Test
        @DisplayName("CVEP MiscType has F_TANK_EQUIPMENT flag")
        void cvepMiscType_IsTankEquipment() {
            MiscType cvepType = (MiscType) EquipmentType.get("ISCombatVehicleEscapePod");

            assertTrue(cvepType.hasFlag(MiscType.F_TANK_EQUIPMENT),
                  "CVEP should have F_TANK_EQUIPMENT flag");
        }

        @Test
        @DisplayName("CVEP MiscType has correct tonnage")
        void cvepMiscType_HasCorrectTonnage() {
            MiscType cvepType = (MiscType) EquipmentType.get("ISCombatVehicleEscapePod");

            assertEquals(4.0, cvepType.getTonnage(null),
                  "CVEP should weigh 4 tons per TO:AUE p.121");
        }

        @Test
        @DisplayName("CVEP MiscType references TO:AUE p.121")
        void cvepMiscType_HasCorrectRulesRef() {
            MiscType cvepType = (MiscType) EquipmentType.get("ISCombatVehicleEscapePod");

            assertEquals("121, TO:AUE", cvepType.getRulesRefs(),
                  "CVEP should reference TO:AUE p.121");
        }
    }
}
