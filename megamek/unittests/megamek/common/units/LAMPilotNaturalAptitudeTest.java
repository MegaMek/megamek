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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.enums.Gender;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests that a {@link LAMPilot} picks the Mek or Aero Natural Aptitude to match the skill its current mode uses.
 */
class LAMPilotNaturalAptitudeTest {

    private static LandAirMek lamIn(int conversionMode) {
        LandAirMek lam = mock(LandAirMek.class);
        when(lam.getConversionMode()).thenReturn(conversionMode);
        when(lam.isConvertingNow()).thenReturn(false);
        return lam;
    }

    /** A pilot whose Mek aptitudes and Aero aptitudes are set independently. */
    private static LAMPilot pilot(LandAirMek lam, boolean gunneryMek, boolean pilotingMek, boolean gunneryAero,
          boolean pilotingAero) {
        return new LAMPilot(lam, "Test", 4, gunneryMek, false, 5, pilotingMek, 4, gunneryAero, 5, pilotingAero,
              Gender.FEMALE, false, null);
    }

    private static Game gameWithArtillerySkill(boolean isUseArtillerySkill) {
        Game game = mock(Game.class);
        GameOptions options = mock(GameOptions.class);
        when(options.booleanOption(OptionsConstants.RPG_ARTILLERY_SKILL)).thenReturn(isUseArtillerySkill);
        when(game.getOptions()).thenReturn(options);
        return game;
    }

    private static Mounted<?> artilleryWeapon() {
        WeaponType weaponType = mock(WeaponType.class);
        when(weaponType.hasFlag(WeaponType.F_ARTILLERY)).thenReturn(true);
        Mounted<?> weapon = mock(Mounted.class);
        doReturn(weaponType).when(weapon).getType();
        return weapon;
    }

    @Nested
    class Construction {
        @Test
        void constructorKeepsEachAptitudeInItsOwnField() {
            LandAirMek lam = lamIn(LandAirMek.CONV_MODE_MEK);

            LAMPilot gunneryMekOnly = pilot(lam, true, false, false, false);
            assertTrue(gunneryMekOnly.isHasNaturalAptitudeGunnery());
            assertFalse(gunneryMekOnly.isHasNaturalAptitudePiloting());
            assertFalse(gunneryMekOnly.isHasNaturalAptitudeGunneryAero());
            assertFalse(gunneryMekOnly.isHasNaturalAptitudePilotingAero());

            LAMPilot pilotingMekOnly = pilot(lam, false, true, false, false);
            assertFalse(pilotingMekOnly.isHasNaturalAptitudeGunnery());
            assertTrue(pilotingMekOnly.isHasNaturalAptitudePiloting());

            LAMPilot gunneryAeroOnly = pilot(lam, false, false, true, false);
            assertTrue(gunneryAeroOnly.isHasNaturalAptitudeGunneryAero());
            assertFalse(gunneryAeroOnly.isHasNaturalAptitudePilotingAero());
            assertFalse(gunneryAeroOnly.isHasNaturalAptitudeGunnery());

            LAMPilot pilotingAeroOnly = pilot(lam, false, false, false, true);
            assertTrue(pilotingAeroOnly.isHasNaturalAptitudePilotingAero());
            assertFalse(pilotingAeroOnly.isHasNaturalAptitudeGunneryAero());
            assertFalse(pilotingAeroOnly.isHasNaturalAptitudePiloting());
        }

        @Test
        void artilleryAptitudeIsKeptSeparately() {
            LAMPilot lamPilot = new LAMPilot(lamIn(LandAirMek.CONV_MODE_MEK), "Test", 4, false, true, 5, false, 4,
                  false, 5, false, Gender.FEMALE, false, null);

            assertTrue(lamPilot.isHasNaturalAptitudeArtillery());
            assertFalse(lamPilot.isHasNaturalAptitudeGunnery());
            assertFalse(lamPilot.isHasNaturalAptitudeGunneryAero());
        }

        @Test
        void defaultPilotHasNoAptitudes() {
            LAMPilot lamPilot = new LAMPilot(lamIn(LandAirMek.CONV_MODE_MEK));

            assertFalse(lamPilot.isHasNaturalAptitudeGunnery());
            assertFalse(lamPilot.isHasNaturalAptitudePiloting());
            assertFalse(lamPilot.isHasNaturalAptitudeGunneryAero());
            assertFalse(lamPilot.isHasNaturalAptitudePilotingAero());
            assertFalse(lamPilot.isHasNaturalAptitudeArtillery());
        }
    }

    @Nested
    class Gunnery {
        @Test
        void mekModeUsesMekAptitude() {
            LandAirMek lam = lamIn(LandAirMek.CONV_MODE_MEK);

            assertTrue(pilot(lam, true, false, false, false).isUseNaturalAptitudeGunnery());
            assertFalse(pilot(lam, false, false, true, false).isUseNaturalAptitudeGunnery());
        }

        @Test
        void airMekModeUsesMekAptitude() {
            LandAirMek lam = lamIn(LandAirMek.CONV_MODE_AIR_MEK);

            assertTrue(pilot(lam, true, false, false, false).isUseNaturalAptitudeGunnery());
            assertFalse(pilot(lam, false, false, true, false).isUseNaturalAptitudeGunnery());
        }

        @Test
        void fighterModeUsesAeroAptitude() {
            LandAirMek lam = lamIn(LandAirMek.CONV_MODE_FIGHTER);

            assertTrue(pilot(lam, false, false, true, false).isUseNaturalAptitudeGunnery());
            assertFalse(pilot(lam, true, false, false, false).isUseNaturalAptitudeGunnery());
        }

        @Test
        void convertingFromFighterStillUsesAeroAptitude() {
            LandAirMek lam = lamIn(LandAirMek.CONV_MODE_MEK);
            when(lam.isConvertingNow()).thenReturn(true);
            when(lam.getPreviousConversionMode()).thenReturn(LandAirMek.CONV_MODE_FIGHTER);

            assertTrue(pilot(lam, false, false, true, false).isUseNaturalAptitudeGunnery());
            assertFalse(pilot(lam, true, false, false, false).isUseNaturalAptitudeGunnery());
        }

        @Test
        void convertingToFighterStillUsesMekAptitude() {
            LandAirMek lam = lamIn(LandAirMek.CONV_MODE_FIGHTER);
            when(lam.isConvertingNow()).thenReturn(true);
            when(lam.getPreviousConversionMode()).thenReturn(LandAirMek.CONV_MODE_MEK);

            assertTrue(pilot(lam, true, false, false, false).isUseNaturalAptitudeGunnery());
            assertFalse(pilot(lam, false, false, true, false).isUseNaturalAptitudeGunnery());
        }

        @Test
        void artilleryUsesArtilleryAptitudeInAnyMode() {
            Game game = gameWithArtillerySkill(true);
            for (int mode : new int[] { LandAirMek.CONV_MODE_MEK, LandAirMek.CONV_MODE_AIR_MEK,
                                        LandAirMek.CONV_MODE_FIGHTER }) {
                LAMPilot artilleryOnly = new LAMPilot(lamIn(mode), "Test", 4, false, true, 5, false, 4, false, 5,
                      false, Gender.FEMALE, false, null);
                LAMPilot gunneryOnly = pilot(lamIn(mode), true, false, true, false);

                assertTrue(artilleryOnly.isUseNaturalAptitudeGunnery(game, artilleryWeapon()), "mode " + mode);
                assertFalse(gunneryOnly.isUseNaturalAptitudeGunnery(game, artilleryWeapon()), "mode " + mode);
            }
        }

        @Test
        void pilotOnFootUsesSmallArmsAptitude() {
            LAMPilot lamPilot = pilot(lamIn(LandAirMek.CONV_MODE_FIGHTER), true, false, true, false);
            lamPilot.setEjected(true);
            lamPilot.setSmallArmsInPlay(true);
            lamPilot.setSmallArms(5, 0);

            assertFalse(lamPilot.isUseNaturalAptitudeGunnery(), "Mek and Aero gunnery don't apply on foot");

            lamPilot.setHasNaturalAptitudeSmallArms(true, 0);

            assertTrue(lamPilot.isUseNaturalAptitudeGunnery());
        }
    }

    @Nested
    class Piloting {
        @Test
        void mekModeUsesMekAptitude() {
            LandAirMek lam = lamIn(LandAirMek.CONV_MODE_MEK);

            assertTrue(pilot(lam, false, true, false, false).isUseNaturalAptitudePiloting(lam));
            assertFalse(pilot(lam, false, false, false, true).isUseNaturalAptitudePiloting(lam));
        }

        @Test
        void fighterModeUsesAeroAptitude() {
            LandAirMek lam = lamIn(LandAirMek.CONV_MODE_FIGHTER);

            assertTrue(pilot(lam, false, false, false, true).isUseNaturalAptitudePiloting(lam));
            assertFalse(pilot(lam, false, true, false, false).isUseNaturalAptitudePiloting(lam));
        }

        @Test
        void airborneAirMekUsesAeroAptitude() {
            LandAirMek lam = lamIn(LandAirMek.CONV_MODE_AIR_MEK);
            when(lam.isAirborneVTOLorWIGE()).thenReturn(true);

            assertTrue(pilot(lam, false, false, false, true).isUseNaturalAptitudePiloting(lam));
            assertFalse(pilot(lam, false, true, false, false).isUseNaturalAptitudePiloting(lam));
        }

        @Test
        void groundedAirMekUsesMekAptitude() {
            LandAirMek lam = lamIn(LandAirMek.CONV_MODE_AIR_MEK);
            when(lam.isAirborneVTOLorWIGE()).thenReturn(false);

            assertTrue(pilot(lam, false, true, false, false).isUseNaturalAptitudePiloting(lam));
            assertFalse(pilot(lam, false, false, false, true).isUseNaturalAptitudePiloting(lam));
        }

        @Test
        void specificSlotFollowsTheModeToo() {
            LandAirMek lam = lamIn(LandAirMek.CONV_MODE_FIGHTER);

            assertTrue(pilot(lam, false, false, false, true).isUseNaturalAptitudePiloting(lam, 0));
            assertFalse(pilot(lam, false, true, false, false).isUseNaturalAptitudePiloting(lam, 0));
        }

        @Test
        void aptitudeFollowsTheSameModeAsTheSkill() {
            // The aptitude must switch exactly when getPiloting() switches between the Mek and Aero skill
            for (int mode : new int[] { LandAirMek.CONV_MODE_MEK, LandAirMek.CONV_MODE_AIR_MEK,
                                        LandAirMek.CONV_MODE_FIGHTER }) {
                for (boolean isAirborne : new boolean[] { false, true }) {
                    LandAirMek lam = lamIn(mode);
                    when(lam.isAirborneVTOLorWIGE()).thenReturn(isAirborne);
                    LAMPilot lamPilot = new LAMPilot(lam, "Test", 4, false, false, 3, false, 4, false, 7, true,
                          Gender.FEMALE, false, null);

                    boolean isUsingAeroSkill = lamPilot.getPiloting() == 7;
                    assertTrue(isUsingAeroSkill == lamPilot.isUseNaturalAptitudePiloting(lam),
                          "mode " + mode + ", airborne " + isAirborne);
                }
            }
        }
    }

    @Nested
    class Conversion {
        @Test
        void convertingACrewCopiesItsAptitudesToBothModes() {
            Crew crew = new Crew(CrewType.SINGLE, "Test", 1, 4, true, true, 5, true, Gender.FEMALE, false, null);
            crew.setHasNaturalAptitudeSmallArms(true, 0);

            LAMPilot lamPilot = LAMPilot.convertToLAMPilot(lamIn(LandAirMek.CONV_MODE_MEK), crew);

            assertTrue(lamPilot.isHasNaturalAptitudeGunnery());
            assertTrue(lamPilot.isHasNaturalAptitudePiloting());
            assertTrue(lamPilot.isHasNaturalAptitudeArtillery());
            assertTrue(lamPilot.isHasNaturalAptitudeGunneryAero(), "a plain crew's gunnery covers both modes");
            assertTrue(lamPilot.isHasNaturalAptitudePilotingAero(), "a plain crew's piloting covers both modes");
            assertTrue(lamPilot.isHasNaturalAptitudeSmallArms(0));
        }

        @Test
        void convertingACrewWithoutAptitudesGivesNone() {
            LAMPilot lamPilot = LAMPilot.convertToLAMPilot(lamIn(LandAirMek.CONV_MODE_MEK),
                  new Crew(CrewType.SINGLE));

            assertFalse(lamPilot.isHasNaturalAptitudeGunnery());
            assertFalse(lamPilot.isHasNaturalAptitudePiloting());
            assertFalse(lamPilot.isHasNaturalAptitudeArtillery());
            assertFalse(lamPilot.isHasNaturalAptitudeGunneryAero());
            assertFalse(lamPilot.isHasNaturalAptitudePilotingAero());
            assertFalse(lamPilot.isHasNaturalAptitudeSmallArms(0));
        }
    }
}
