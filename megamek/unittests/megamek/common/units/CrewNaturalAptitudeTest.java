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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;

import megamek.common.actions.WeaponAttackAction;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests the per-slot Natural Aptitudes tracked by {@link Crew}: construction, per-slot storage, which slot answers
 * when the pilot or gunner changes, which aptitude a weapon attack uses, and survival across serialization.
 */
class CrewNaturalAptitudeTest {

    private static Game gameWithArtillerySkill(boolean isUseArtillerySkill) {
        Game game = mock(Game.class);
        GameOptions options = mock(GameOptions.class);
        when(options.booleanOption(OptionsConstants.RPG_ARTILLERY_SKILL)).thenReturn(isUseArtillerySkill);
        when(game.getOptions()).thenReturn(options);
        return game;
    }

    private static Mounted<?> weapon(boolean isArtillery) {
        WeaponType weaponType = mock(WeaponType.class);
        when(weaponType.hasFlag(WeaponType.F_ARTILLERY)).thenReturn(isArtillery);
        Mounted<?> weapon = mock(Mounted.class);
        doReturn(weaponType).when(weapon).getType();
        return weapon;
    }

    /** A crew with only the aptitudes given, in slot 0. */
    private static Crew crewWith(boolean gunnery, boolean artillery, boolean piloting, boolean smallArms) {
        Crew crew = new Crew(CrewType.SINGLE);
        crew.setHasNaturalAptitudeGunnery(gunnery, 0);
        crew.setHasNaturalAptitudeArtillery(artillery, 0);
        crew.setHasNaturalAptitudePiloting(piloting, 0);
        crew.setHasNaturalAptitudeSmallArms(smallArms, 0);
        return crew;
    }

    /** Puts the crew on foot with a Small Arms skill, so it fires with Small Arms instead of gunnery. */
    private static void putOnFoot(Crew crew) {
        crew.setEjected(true);
        crew.setSmallArmsInPlay(true);
        crew.setSmallArms(5, 0);
    }

    @Nested
    class Construction {
        @Test
        void defaultCrewHasNoAptitudes() {
            Crew crew = new Crew(CrewType.SINGLE);

            assertFalse(crew.isHasNaturalAptitudeGunnery());
            assertFalse(crew.isHasNaturalAptitudeArtillery());
            assertFalse(crew.isHasNaturalAptitudePiloting());
            assertFalse(crew.isHasNaturalAptitudeSmallArms(0));
        }

        @Test
        void constructorWithoutAptitudesHasNoAptitudes() {
            Crew crew = new Crew(CrewType.SINGLE, "Test", 1, 4, 5, Gender.FEMALE, false, null);

            assertFalse(crew.isHasNaturalAptitudeGunnery());
            assertFalse(crew.isHasNaturalAptitudeArtillery());
            assertFalse(crew.isHasNaturalAptitudePiloting());
        }

        @Test
        void constructorKeepsEachAptitudeInItsOwnField() {
            // Guards against the parameters being passed along in the wrong order
            Crew gunneryOnly = new Crew(CrewType.SINGLE, "Test", 1, 4, true, false, 5, false, Gender.FEMALE, false,
                  null);
            Crew artilleryOnly = new Crew(CrewType.SINGLE, "Test", 1, 4, false, true, 5, false, Gender.FEMALE, false,
                  null);
            Crew pilotingOnly = new Crew(CrewType.SINGLE, "Test", 1, 4, false, false, 5, true, Gender.FEMALE, false,
                  null);

            assertTrue(gunneryOnly.isHasNaturalAptitudeGunnery());
            assertFalse(gunneryOnly.isHasNaturalAptitudeArtillery());
            assertFalse(gunneryOnly.isHasNaturalAptitudePiloting());

            assertFalse(artilleryOnly.isHasNaturalAptitudeGunnery());
            assertTrue(artilleryOnly.isHasNaturalAptitudeArtillery());
            assertFalse(artilleryOnly.isHasNaturalAptitudePiloting());

            assertFalse(pilotingOnly.isHasNaturalAptitudeGunnery());
            assertFalse(pilotingOnly.isHasNaturalAptitudeArtillery());
            assertTrue(pilotingOnly.isHasNaturalAptitudePiloting());
        }

        @Test
        void fullConstructorKeepsEachAptitudeInItsOwnField() {
            Crew crew = new Crew(CrewType.SINGLE, "Test", 1, 4, 4, 4, false, true, 5, false, Gender.FEMALE, false,
                  null);

            assertFalse(crew.isHasNaturalAptitudeGunnery());
            assertTrue(crew.isHasNaturalAptitudeArtillery());
            assertFalse(crew.isHasNaturalAptitudePiloting());
        }

        @ParameterizedTest
        @EnumSource(value = CrewType.class, names = { "SINGLE", "TRIPOD", "SUPERHEAVY_TRIPOD", "QUADVEE", "DUAL",
                                                      "COMMAND_CONSOLE" })
        void constructorGivesEverySlotTheAptitudes(CrewType crewType) {
            Crew crew = new Crew(crewType, "Test", crewType.getCrewSlots(), 4, true, true, 5, true, Gender.FEMALE,
                  false, null);

            for (int slot = 0; slot < crew.getSlotCount(); slot++) {
                assertTrue(crew.isHasNaturalAptitudeGunnery(slot), "gunnery, slot " + slot);
                assertTrue(crew.isHasNaturalAptitudeArtillery(slot), "artillery, slot " + slot);
                assertTrue(crew.isHasNaturalAptitudePiloting(slot), "piloting, slot " + slot);
                assertFalse(crew.isHasNaturalAptitudeSmallArms(slot), "small arms is never set by a constructor");
            }
        }
    }

    @Nested
    class PerSlotStorage {
        @Test
        void settingOneSlotLeavesTheOthersAlone() {
            Crew crew = new Crew(CrewType.SUPERHEAVY_TRIPOD);

            crew.setHasNaturalAptitudeGunnery(true, 1);
            crew.setHasNaturalAptitudeArtillery(true, 2);
            crew.setHasNaturalAptitudePiloting(true, 0);
            crew.setHasNaturalAptitudeSmallArms(true, 2);

            assertTrue(crew.isHasNaturalAptitudeGunnery(1));
            assertFalse(crew.isHasNaturalAptitudeGunnery(0));
            assertFalse(crew.isHasNaturalAptitudeGunnery(2));

            assertTrue(crew.isHasNaturalAptitudeArtillery(2));
            assertFalse(crew.isHasNaturalAptitudeArtillery(0));
            assertFalse(crew.isHasNaturalAptitudeArtillery(1));

            assertTrue(crew.isHasNaturalAptitudePiloting(0));
            assertFalse(crew.isHasNaturalAptitudePiloting(1));
            assertFalse(crew.isHasNaturalAptitudePiloting(2));

            assertTrue(crew.isHasNaturalAptitudeSmallArms(2));
            assertFalse(crew.isHasNaturalAptitudeSmallArms(0));
            assertFalse(crew.isHasNaturalAptitudeSmallArms(1));
        }

        @Test
        void aptitudesCanBeCleared() {
            Crew crew = crewWith(true, true, true, true);

            crew.setHasNaturalAptitudeGunnery(false, 0);
            crew.setHasNaturalAptitudeArtillery(false, 0);
            crew.setHasNaturalAptitudePiloting(false, 0);
            crew.setHasNaturalAptitudeSmallArms(false, 0);

            assertFalse(crew.isHasNaturalAptitudeGunnery(0));
            assertFalse(crew.isHasNaturalAptitudeArtillery(0));
            assertFalse(crew.isHasNaturalAptitudePiloting(0));
            assertFalse(crew.isHasNaturalAptitudeSmallArms(0));
        }
    }

    @Nested
    class CurrentPilotAndGunner {
        @Test
        void defaultGettersAnswerForTheDefaultPilotAndGunner() {
            Crew crew = new Crew(CrewType.TRIPOD);
            crew.setHasNaturalAptitudePiloting(true, CrewType.TRIPOD.getPilotPos());
            crew.setHasNaturalAptitudeGunnery(true, CrewType.TRIPOD.getGunnerPos());

            assertTrue(crew.isHasNaturalAptitudePiloting());
            assertTrue(crew.isHasNaturalAptitudeGunnery());
        }

        @Test
        void pilotAptitudeDoesNotLeakToTheGunnerAndViceVersa() {
            Crew crew = new Crew(CrewType.TRIPOD);
            crew.setHasNaturalAptitudePiloting(true, CrewType.TRIPOD.getGunnerPos());
            crew.setHasNaturalAptitudeGunnery(true, CrewType.TRIPOD.getPilotPos());

            assertFalse(crew.isHasNaturalAptitudePiloting(), "the gunner's piloting aptitude isn't the pilot's");
            assertFalse(crew.isHasNaturalAptitudeGunnery(), "the pilot's gunnery aptitude isn't the gunner's");
        }

        @Test
        void backupPilotBringsTheirOwnAptitude() {
            Crew crew = new Crew(CrewType.TRIPOD);
            crew.setHasNaturalAptitudePiloting(false, 0);
            crew.setHasNaturalAptitudePiloting(true, 1);

            crew.setUnconscious(true, 0);

            assertEquals(1, crew.getCurrentPilotIndex(), "the gunner should have taken over piloting");
            assertTrue(crew.isHasNaturalAptitudePiloting(), "the backup pilot rolls with their own aptitude");
        }

        @Test
        void backupPilotWithoutAptitudeLosesThePilotsAptitude() {
            Crew crew = new Crew(CrewType.TRIPOD);
            crew.setHasNaturalAptitudePiloting(true, 0);
            crew.setHasNaturalAptitudePiloting(false, 1);

            crew.setUnconscious(true, 0);

            assertFalse(crew.isHasNaturalAptitudePiloting());
        }

        @Test
        void commandConsoleSwapMovesBothPilotAndGunnerAptitudes() {
            Crew crew = new Crew(CrewType.COMMAND_CONSOLE);
            crew.setHasNaturalAptitudePiloting(true, 1);
            crew.setHasNaturalAptitudeGunnery(true, 1);

            assertFalse(crew.isHasNaturalAptitudePiloting());
            assertFalse(crew.isHasNaturalAptitudeGunnery());

            crew.setCurrentPilot(1);

            assertTrue(crew.isHasNaturalAptitudePiloting());
            assertTrue(crew.isHasNaturalAptitudeGunnery());
        }
    }

    @Nested
    class GunneryAptitudeSelection {
        @Test
        void noWeaponUsesGunneryAptitude() {
            assertTrue(crewWith(true, false, false, false).isUseNaturalAptitudeGunnery());
            assertFalse(crewWith(false, true, false, false).isUseNaturalAptitudeGunnery());
        }

        @Test
        void directFireWeaponUsesGunneryAptitudeEvenWithTheArtillerySkill() {
            Game game = gameWithArtillerySkill(true);

            assertTrue(crewWith(true, false, false, false).isUseNaturalAptitudeGunnery(game, weapon(false)));
            assertFalse(crewWith(false, true, false, false).isUseNaturalAptitudeGunnery(game, weapon(false)));
        }

        @Test
        void artilleryWeaponUsesArtilleryAptitudeWithTheArtillerySkill() {
            Game game = gameWithArtillerySkill(true);

            assertTrue(crewWith(false, true, false, false).isUseNaturalAptitudeGunnery(game, weapon(true)));
            assertFalse(crewWith(true, false, false, false).isUseNaturalAptitudeGunnery(game, weapon(true)));
        }

        @Test
        void artilleryWeaponUsesGunneryAptitudeWithoutTheArtillerySkill() {
            Game game = gameWithArtillerySkill(false);

            assertTrue(crewWith(true, false, false, false).isUseNaturalAptitudeGunnery(game, weapon(true)));
            assertFalse(crewWith(false, true, false, false).isUseNaturalAptitudeGunnery(game, weapon(true)));
        }

        @Test
        void unknownGameIsTreatedAsNoArtillerySkill() {
            assertTrue(crewWith(true, false, false, false).isUseNaturalAptitudeGunnery(null, weapon(true)));
        }

        @Test
        void weaponAttackActionLooksUpTheWeaponFired() {
            Game game = gameWithArtillerySkill(true);
            Mounted<?> artillery = weapon(true);
            Entity attacker = mock(Entity.class);
            doReturn(artillery).when(attacker).getEquipment(7);
            WeaponAttackAction attack = mock(WeaponAttackAction.class);
            when(attack.getEntity(game)).thenReturn(attacker);
            when(attack.getWeaponId()).thenReturn(7);

            assertTrue(crewWith(false, true, false, false).isUseNaturalAptitudeGunnery(game, attack));
            assertFalse(crewWith(true, false, false, false).isUseNaturalAptitudeGunnery(game, attack));
        }

        @Test
        void weaponAttackActionWithMissingAttackerFallsBackToGunnery() {
            Game game = gameWithArtillerySkill(true);
            WeaponAttackAction attack = mock(WeaponAttackAction.class);
            when(attack.getEntity(game)).thenReturn(null);

            assertTrue(crewWith(true, false, false, false).isUseNaturalAptitudeGunnery(game, attack));
        }

        @Test
        void usesTheCurrentGunnersAptitude() {
            Crew crew = new Crew(CrewType.TRIPOD);
            crew.setHasNaturalAptitudeGunnery(true, 0);

            assertFalse(crew.isUseNaturalAptitudeGunnery(), "slot 0 is the pilot, not the gunner");

            crew.setHasNaturalAptitudeGunnery(true, 1);

            assertTrue(crew.isUseNaturalAptitudeGunnery());
        }
    }

    @Nested
    class SmallArmsAptitude {
        @Test
        void crewOnFootUsesSmallArmsAptitude() {
            Crew crew = crewWith(false, false, false, true);
            putOnFoot(crew);

            assertTrue(crew.isUseNaturalAptitudeGunnery());
        }

        @Test
        void crewOnFootIgnoresGunneryAptitude() {
            Crew crew = crewWith(true, false, false, false);
            putOnFoot(crew);

            assertFalse(crew.isUseNaturalAptitudeGunnery());
        }

        @Test
        void crewOnFootIgnoresArtilleryAptitudeEvenForArtillery() {
            Crew crew = crewWith(false, true, false, false);
            putOnFoot(crew);

            assertFalse(crew.isUseNaturalAptitudeGunnery(gameWithArtillerySkill(true), weapon(true)));
        }

        @Test
        void smallArmsAptitudeIsUnusedAboardTheUnit() {
            Crew crew = crewWith(false, false, false, true);

            assertFalse(crew.isUseNaturalAptitudeGunnery(), "not ejected, so gunnery is used");
        }

        @Test
        void ejectedCrewWithoutTheRuleKeepsGunnery() {
            Crew crew = crewWith(true, false, false, true);
            crew.setEjected(true);
            crew.setSmallArms(5, 0);

            assertTrue(crew.isUseNaturalAptitudeGunnery(), "without the rule, crews fire on foot with gunnery");
        }
    }

    @Nested
    class PilotingAptitudeSelection {
        @Test
        void usesTheCurrentPilotsAptitude() {
            Entity entity = mock(Entity.class);

            assertTrue(crewWith(false, false, true, false).isUseNaturalAptitudePiloting(entity));
            assertFalse(crewWith(true, true, false, true).isUseNaturalAptitudePiloting(entity));
        }

        @Test
        void specificSlotUsesThatSlotsAptitude() {
            Entity entity = mock(Entity.class);
            Crew crew = new Crew(CrewType.TRIPOD);
            crew.setHasNaturalAptitudePiloting(true, 1);

            assertFalse(crew.isUseNaturalAptitudePiloting(entity, 0));
            assertTrue(crew.isUseNaturalAptitudePiloting(entity, 1));
        }

        @Test
        void rollingForTheCurrentPilotChecksTheCurrentPilotsSlot() {
            Entity entity = mock(Entity.class);
            Crew crew = spy(new Crew(CrewType.TRIPOD));

            crew.rollPilotingSkill(entity);

            verify(crew).isUseNaturalAptitudePiloting(entity, CrewType.TRIPOD.getPilotPos());
        }

        @Test
        void rollingForASpecificSlotChecksThatSlot() {
            Entity entity = mock(Entity.class);
            Crew crew = spy(new Crew(CrewType.TRIPOD));

            crew.rollPilotingSkill(entity, 1);

            verify(crew).isUseNaturalAptitudePiloting(eq(entity), eq(1));
        }

        @Test
        void gunneryRollChecksTheGunneryAptitudeForTheAttack() {
            Game game = gameWithArtillerySkill(false);
            WeaponAttackAction attack = mock(WeaponAttackAction.class);
            Crew crew = spy(new Crew(CrewType.SINGLE));

            crew.rollGunnerySkill(game, attack);

            verify(crew).isUseNaturalAptitudeGunnery(game, attack);
        }
    }

    @Nested
    class RandomAptitudes {
        @Test
        void crewWithNoExperienceNeverHasAnAptitude() {
            for (int i = 0; i < 1000; i++) {
                assertFalse(Crew.rollNaturalAptitude(SkillLevel.NONE));
            }
        }

        @ParameterizedTest
        @EnumSource(value = SkillLevel.class, names = "NONE", mode = EnumSource.Mode.EXCLUDE)
        void everyExperienceLevelCanBeRolledWithoutError(SkillLevel skillLevel) {
            for (int i = 0; i < 100; i++) {
                Crew.rollNaturalAptitude(skillLevel);
            }
        }
    }

    @Nested
    class Serialization {
        private static Crew throughJavaSerialization(Crew crew) throws Exception {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
                out.writeObject(crew);
            }
            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                return (Crew) in.readObject();
            }
        }

        @Test
        void perSlotAptitudesSurviveJavaSerialization() throws Exception {
            Crew crew = new Crew(CrewType.TRIPOD);
            crew.setHasNaturalAptitudePiloting(true, 0);
            crew.setHasNaturalAptitudeGunnery(true, 1);
            crew.setHasNaturalAptitudeArtillery(true, 1);
            crew.setHasNaturalAptitudeSmallArms(true, 0);

            Crew restored = throughJavaSerialization(crew);

            assertTrue(restored.isHasNaturalAptitudePiloting(0));
            assertFalse(restored.isHasNaturalAptitudePiloting(1));
            assertTrue(restored.isHasNaturalAptitudeGunnery(1));
            assertFalse(restored.isHasNaturalAptitudeGunnery(0));
            assertTrue(restored.isHasNaturalAptitudeArtillery(1));
            assertTrue(restored.isHasNaturalAptitudeSmallArms(0));
            assertFalse(restored.isHasNaturalAptitudeSmallArms(1));
        }

        @Test
        void crewSavedBeforeAptitudesExistedHasNoneAndCanBeGivenThem() throws Exception {
            // A crew deserialized from an older save restores the aptitude arrays as null
            Crew crew = new Crew(CrewType.DUAL);
            for (String fieldName : new String[] { "naturalAptitudesGunnery", "naturalAptitudesArtillery",
                                                   "naturalAptitudesPiloting", "naturalAptitudesSmallArms" }) {
                Field field = Crew.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(crew, null);
            }

            for (int slot = 0; slot < crew.getSlotCount(); slot++) {
                assertFalse(crew.isHasNaturalAptitudeGunnery(slot));
                assertFalse(crew.isHasNaturalAptitudeArtillery(slot));
                assertFalse(crew.isHasNaturalAptitudePiloting(slot));
                assertFalse(crew.isHasNaturalAptitudeSmallArms(slot));
            }

            crew.setHasNaturalAptitudeGunnery(true, 1);
            assertTrue(crew.isHasNaturalAptitudeGunnery(1));
        }
    }
}
