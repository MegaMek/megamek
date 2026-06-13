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

import megamek.common.TargetRollModifier;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import megamek.common.rolls.PilotingRollData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wind Walker SPA functionality.
 */

class WindWalkerIntegrationTest {
    private Game game;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
    }

    @Test
    void testWindWalkerLandingModifierForAero() {
        AeroSpaceFighter aero = new AeroSpaceFighter();
        Crew crew = new Crew(CrewType.SINGLE);
        PilotOptions options = new PilotOptions();
        options.getOption(OptionsConstants.PILOT_WIND_WALKER).setValue(true);
        crew.setOptions(options);
        aero.setCrew(crew);
        aero.setGame(game);

        PilotingRollData roll = aero.getLandingControlRoll(5, new Coords(0, 0), 0, false);

        boolean hasWindWalkerMod = roll.getDesc().contains("Wind Walker");
        assertTrue(hasWindWalkerMod, "Landing roll should include Wind Walker modifier");

        int windWalkerMod = roll.getModifiers().stream()
              .filter(mod -> mod.getDesc().contains("Wind Walker"))
              .mapToInt(TargetRollModifier::value)
              .findFirst()
              .orElse(0);

        assertEquals(-1, windWalkerMod, "Wind Walker landing modifier should be -1");
    }

    @Test
    void testWindWalkerGliderLandingForProtoMek() {
        ProtoMek glider = new ProtoMek();
        glider.setIsGlider(true);
        Crew crew = new Crew(CrewType.SINGLE);
        PilotOptions options = new PilotOptions();
        options.getOption(OptionsConstants.PILOT_WIND_WALKER).setValue(true);
        crew.setOptions(options);
        glider.setCrew(crew);
        glider.setOriginalWalkMP(4);

        PilotingRollData roll = glider.checkGliderLanding();

        boolean hasWindWalkerMod = roll.getDesc().contains("Wind Walker");
        assertTrue(hasWindWalkerMod, "Glider ProtoMek landing roll should include Wind Walker modifier");

        int windWalkerMod = roll.getModifiers().stream()
              .filter(mod -> mod.getDesc().contains("Wind Walker"))
              .mapToInt(TargetRollModifier::value)
              .findFirst()
              .orElse(0);

        assertEquals(-1, windWalkerMod, "Wind Walker glider landing modifier should be -1");
    }

    @Test
    void testNoWindWalkerModifierWithoutSPA() {
        AeroSpaceFighter aero = new AeroSpaceFighter();
        Crew crew = new Crew(CrewType.SINGLE);
        aero.setCrew(crew);
        aero.setGame(game);

        PilotingRollData roll = aero.getLandingControlRoll(5, new Coords(0, 0), 0, false);

        boolean hasWindWalkerMod = roll.getDesc().contains("Wind Walker");
        assertFalse(hasWindWalkerMod, "Landing roll should NOT include Wind Walker modifier without the SPA option");
    }

    @Test
    void testWindWalkerDoesNotApplyToInvalidUnit() {
        BipedMek mek = new BipedMek();
        Crew crew = new Crew(CrewType.SINGLE);
        PilotOptions options = new PilotOptions();
        options.getOption(OptionsConstants.PILOT_WIND_WALKER).setValue(true);
        crew.setOptions(options);
        mek.setCrew(crew);

        assertFalse(PilotSPAHelper.isWindWalkerValid(mek), "Wind Walker should not be valid for standard BipedMeks");
    }

    @Test
    void testWindWalkerLandingModifierForConvFighter() {
        ConvFighter convFighter = new ConvFighter();
        Crew crew = new Crew(CrewType.SINGLE);
        PilotOptions options = new PilotOptions();
        options.getOption(OptionsConstants.PILOT_WIND_WALKER).setValue(true);
        crew.setOptions(options);
        convFighter.setCrew(crew);
        convFighter.setGame(game);

        PilotingRollData roll = convFighter.getLandingControlRoll(5, new Coords(0, 0), 0, false);

        boolean hasWindWalkerMod = roll.getDesc().contains("Wind Walker");
        assertTrue(hasWindWalkerMod, "Landing roll should include Wind Walker modifier for ConvFighter");

        int windWalkerMod = roll.getModifiers().stream()
              .filter(mod -> mod.getDesc().contains("Wind Walker"))
              .mapToInt(TargetRollModifier::value)
              .findFirst()
              .orElse(0);

        assertEquals(-1, windWalkerMod, "Wind Walker landing modifier should be -1 for ConvFighter");
    }

    @Test
    void testWindWalkerLandingModifierForFixedWingSupport() {
        FixedWingSupport fixedWingSupport = new FixedWingSupport();
        Crew crew = new Crew(CrewType.SINGLE);
        PilotOptions options = new PilotOptions();
        options.getOption(OptionsConstants.PILOT_WIND_WALKER).setValue(true);
        crew.setOptions(options);
        fixedWingSupport.setCrew(crew);
        fixedWingSupport.setGame(game);

        PilotingRollData roll = fixedWingSupport.getLandingControlRoll(5, new Coords(0, 0), 0, false);

        boolean hasWindWalkerMod = roll.getDesc().contains("Wind Walker");
        assertTrue(hasWindWalkerMod, "Landing roll should include Wind Walker modifier for FixedWingSupport");

        int windWalkerMod = roll.getModifiers().stream()
              .filter(mod -> mod.getDesc().contains("Wind Walker"))
              .mapToInt(TargetRollModifier::value)
              .findFirst()
              .orElse(0);

        assertEquals(-1, windWalkerMod, "Wind Walker landing modifier should be -1 for FixedWingSupport");
    }

    @Test
    void testWindWalkerLandingModifierForSmallCraft() {
        SmallCraft smallCraft = new SmallCraft();
        Crew crew = new Crew(CrewType.SINGLE);
        PilotOptions options = new PilotOptions();
        options.getOption(OptionsConstants.PILOT_WIND_WALKER).setValue(true);
        crew.setOptions(options);
        smallCraft.setCrew(crew);
        smallCraft.setGame(game);

        PilotingRollData roll = smallCraft.getLandingControlRoll(5, new Coords(0, 0), 0, false);

        boolean hasWindWalkerMod = roll.getDesc().contains("Wind Walker");
        assertTrue(hasWindWalkerMod, "Landing roll should include Wind Walker modifier for SmallCraft");

        int windWalkerMod = roll.getModifiers().stream()
              .filter(mod -> mod.getDesc().contains("Wind Walker"))
              .mapToInt(TargetRollModifier::value)
              .findFirst()
              .orElse(0);

        assertEquals(-1, windWalkerMod, "Wind Walker landing modifier should be -1 for SmallCraft");
    }

    @Test
    void testWindWalkerLandingModifierForDropShip() {
        Dropship dropShip = new Dropship();
        Crew crew = new Crew(CrewType.SINGLE);
        PilotOptions options = new PilotOptions();
        options.getOption(OptionsConstants.PILOT_WIND_WALKER).setValue(true);
        crew.setOptions(options);
        dropShip.setCrew(crew);
        dropShip.setGame(game);

        PilotingRollData roll = dropShip.getLandingControlRoll(5, new Coords(0, 0), 0, false);

        boolean hasWindWalkerMod = roll.getDesc().contains("Wind Walker");
        assertTrue(hasWindWalkerMod, "Landing roll should include Wind Walker modifier for DropShip");

        int windWalkerMod = roll.getModifiers().stream()
              .filter(mod -> mod.getDesc().contains("Wind Walker"))
              .mapToInt(TargetRollModifier::value)
              .findFirst()
              .orElse(0);

        assertEquals(-1, windWalkerMod, "Wind Walker landing modifier should be -1 for DropShip");
    }

    @Test
    void testWindWalkerLandingModifierForJumpShip() {
        Jumpship jumpShip = new Jumpship();
        Crew crew = new Crew(CrewType.SINGLE);
        PilotOptions options = new PilotOptions();
        options.getOption(OptionsConstants.PILOT_WIND_WALKER).setValue(true);
        crew.setOptions(options);
        jumpShip.setCrew(crew);
        jumpShip.setGame(game);

        PilotingRollData roll = jumpShip.getLandingControlRoll(5, new Coords(0, 0), 0, false);

        boolean hasWindWalkerMod = roll.getDesc().contains("Wind Walker");
        assertTrue(hasWindWalkerMod, "Landing roll should include Wind Walker modifier for JumpShip");

        int windWalkerMod = roll.getModifiers().stream()
              .filter(mod -> mod.getDesc().contains("Wind Walker"))
              .mapToInt(TargetRollModifier::value)
              .findFirst()
              .orElse(0);

        assertEquals(-1, windWalkerMod, "Wind Walker landing modifier should be -1 for JumpShip");
    }

    @Test
    void testWindWalkerLandingModifierForWarship() {
        Warship warship = new Warship();
        Crew crew = new Crew(CrewType.SINGLE);
        PilotOptions options = new PilotOptions();
        options.getOption(OptionsConstants.PILOT_WIND_WALKER).setValue(true);
        crew.setOptions(options);
        warship.setCrew(crew);
        warship.setGame(game);

        PilotingRollData roll = warship.getLandingControlRoll(5, new Coords(0, 0), 0, false);

        boolean hasWindWalkerMod = roll.getDesc().contains("Wind Walker");
        assertTrue(hasWindWalkerMod, "Landing roll should include Wind Walker modifier for Warship");

        int windWalkerMod = roll.getModifiers().stream()
              .filter(mod -> mod.getDesc().contains("Wind Walker"))
              .mapToInt(TargetRollModifier::value)
              .findFirst()
              .orElse(0);

        assertEquals(-1, windWalkerMod, "Wind Walker landing modifier should be -1 for Warship");
    }

    @Test
    void testWindWalkerLandingModifierForSpaceStation() {
        SpaceStation spaceStation = new SpaceStation();
        Crew crew = new Crew(CrewType.SINGLE);
        PilotOptions options = new PilotOptions();
        options.getOption(OptionsConstants.PILOT_WIND_WALKER).setValue(true);
        crew.setOptions(options);
        spaceStation.setCrew(crew);
        spaceStation.setGame(game);

        PilotingRollData roll = spaceStation.getLandingControlRoll(5, new Coords(0, 0), 0, false);

        boolean hasWindWalkerMod = roll.getDesc().contains("Wind Walker");
        assertTrue(hasWindWalkerMod, "Landing roll should include Wind Walker modifier for SpaceStation");

        int windWalkerMod = roll.getModifiers().stream()
              .filter(mod -> mod.getDesc().contains("Wind Walker"))
              .mapToInt(TargetRollModifier::value)
              .findFirst()
              .orElse(0);

        assertEquals(-1, windWalkerMod, "Wind Walker landing modifier should be -1 for SpaceStation");
    }

    @Test
    void testWindWalkerLandingModifierForLandAirMek() {
        LandAirMek landAirMek = new LandAirMek(Mek.GYRO_STANDARD, Mek.COCKPIT_STANDARD, LandAirMek.LAM_STANDARD);
        Crew crew = new Crew(CrewType.SINGLE);
        PilotOptions options = new PilotOptions();
        options.getOption(OptionsConstants.PILOT_WIND_WALKER).setValue(true);
        crew.setOptions(options);
        landAirMek.setCrew(crew);
        landAirMek.setGame(game);

        PilotingRollData roll = landAirMek.getLandingControlRoll(5, new Coords(0, 0), 0, false);

        boolean hasWindWalkerMod = roll.getDesc().contains("Wind Walker");
        assertTrue(hasWindWalkerMod, "Landing roll should include Wind Walker modifier for LandAirMek");

        int windWalkerMod = roll.getModifiers().stream()
              .filter(mod -> mod.getDesc().contains("Wind Walker"))
              .mapToInt(TargetRollModifier::value)
              .findFirst()
              .orElse(0);

        assertEquals(-1, windWalkerMod, "Wind Walker landing modifier should be -1 for LandAirMek");
    }

    @Test
    void testWindWalkerLandingModifierForConvFighterWithoutSPA() {
        ConvFighter convFighter = new ConvFighter();
        Crew crew = new Crew(CrewType.SINGLE);
        PilotOptions options = new PilotOptions();
        crew.setOptions(options);
        convFighter.setCrew(crew);
        convFighter.setGame(game);

        PilotingRollData roll = convFighter.getLandingControlRoll(5, new Coords(0, 0), 0, false);

        boolean hasWindWalkerMod = roll.getDesc().contains("Wind Walker");
        assertFalse(hasWindWalkerMod,
              "Landing roll should not include Wind Walker modifier for ConvFighter without the SPA option");
    }


}
