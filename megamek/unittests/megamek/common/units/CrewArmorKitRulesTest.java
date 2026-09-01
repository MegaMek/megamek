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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.TechConstants;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.AtmosphericTaint;
import megamek.common.planetaryConditions.PlanetaryConditions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the optional crew personal equipment rule.
 * <p>
 * The point of carrying the kit itself rather than a flag is that most kits need no rule written for them: the
 * conventional infantry code already reads their flags. Only the MekWarrior Combat Suit needs an interpretation,
 * because A Time of War gives the sealing to a neurohelmet that MegaMek does not model.
 */
class CrewArmorKitRulesTest {

    private static final String COMBAT_SUIT = CrewArmorKitRules.COMBAT_SUIT_NAME;
    private static final String SPACESUIT = "Spacesuit";
    private static final String SNOWSUIT = "Snowsuit";
    private static final String HOSTILE_ENVIRONMENT_SUIT = "Environment Suit, Hostile";
    private static final String COVERALLS = "Clothing, Fatigues/Civilian/Non-Armored";

    private Game game;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        game.getOptions().getOption(OptionsConstants.RPG_COMBAT_SUITS).setValue(true);
        game.getOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(3025);
    }

    private static EquipmentType kit(String internalName) {
        EquipmentType armorKit = EquipmentType.get(internalName);
        assertNotNull(armorKit, internalName + " must exist in the equipment tables");
        return armorKit;
    }

    private PlanetaryConditions conditions(Atmosphere atmosphere, AtmosphericTaint taint, int temperature) {
        PlanetaryConditions planetaryConditions = new PlanetaryConditions();
        planetaryConditions.setAtmosphere(atmosphere);
        planetaryConditions.setAtmosphericTaint(taint);
        planetaryConditions.setTemperature(temperature);
        return planetaryConditions;
    }

    private Mek mekWearing(String armorKitName) {
        Mek mek = new BipedMek();
        mek.setGame(game);
        mek.getCrew().setArmorKitName(armorKitName, 0);
        return mek;
    }

    @Test
    void mekVehicleAndAerospaceCrewsMayBeIssuedAKit() {
        assertTrue(CrewArmorKitRules.canWearArmorKit(new BipedMek()));
        assertTrue(CrewArmorKitRules.canWearArmorKit(new Tank()));
    }

    @Test
    void infantryAndProtoMeksMayNot() {
        assertFalse(CrewArmorKitRules.canWearArmorKit(new ConvInfantry()),
              "infantry carry their armor on the unit, so the crew slot would be a second copy of it");
        assertFalse(CrewArmorKitRules.canWearArmorKit(new ProtoMek()),
              "a ProtoMek has no ejection system, so there is nothing to wear a kit out of");
        assertFalse(CrewArmorKitRules.canWearArmorKit(null));
    }

    @Test
    void everyArmorKitIsOfferedToChooseFrom() {
        assertTrue(CrewArmorKitRules.availableArmorKits().size() > 40,
              "the chooser draws on MegaMek's own armor kit list rather than a list kept here");
    }

    @Test
    void theKitIsIgnoredWithTheRuleOff() {
        game.getOptions().getOption(OptionsConstants.RPG_COMBAT_SUITS).setValue(false);

        assertNull(CrewArmorKitRules.crewArmorKit(mekWearing(SPACESUIT), game));
    }

    @Test
    void theKitComesBackWithTheRuleOn() {
        assertEquals(kit(SPACESUIT), CrewArmorKitRules.crewArmorKit(mekWearing(SPACESUIT), game));
    }

    @Test
    void aCrewWearingNothingHasNoKit() {
        assertNull(CrewArmorKitRules.crewArmorKit(mekWearing(null), game));
    }

    @Test
    void aKitNotInventedYetIsNotWorn() {
        game.getOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(2600);

        assertNull(CrewArmorKitRules.crewArmorKit(mekWearing(COMBAT_SUIT), game),
              "nobody had a combat suit in 2600, whatever the campaign file says");
    }

    @Test
    void aClanCrewMayWearTheCombatSuitToo() {
        Mek clanMek = mekWearing(COMBAT_SUIT);
        clanMek.setTechLevel(TechConstants.T_CLAN_TW);

        assertNotNull(CrewArmorKitRules.crewArmorKit(clanMek, game),
              "the suit is All tech base and never goes extinct");
    }

    /**
     * The heart of it. A kit is worth reporting only where it answers the danger actually out there, and the two
     * halves of the table come from different places: the sealed kits answer vacuum through their own flags, while
     * the combat suit answers air and heat by interpretation and must not claim vacuum.
     */
    @Test
    void whatEachKitAnswers() {
        PlanetaryConditions vacuum = conditions(Atmosphere.VACUUM, AtmosphericTaint.BREATHABLE, 20);
        PlanetaryConditions toxicAir = conditions(Atmosphere.STANDARD, AtmosphericTaint.TOXIC_CAUSTIC, 20);
        PlanetaryConditions extremeHeat = conditions(Atmosphere.STANDARD, AtmosphericTaint.BREATHABLE, 60);
        PlanetaryConditions extremeCold = conditions(Atmosphere.STANDARD, AtmosphericTaint.BREATHABLE, -40);
        PlanetaryConditions fairWeather = conditions(Atmosphere.STANDARD, AtmosphericTaint.BREATHABLE, 20);

        assertTrue(CrewArmorKitRules.coversSomethingIn(kit(SPACESUIT), vacuum),
              "a spacesuit is sealed, which is what vacuum asks for");
        assertFalse(CrewArmorKitRules.coversSomethingIn(kit(COMBAT_SUIT), vacuum),
              "the combat suit carries air but holds no pressure");
        assertFalse(CrewArmorKitRules.coversSomethingIn(kit(COVERALLS), vacuum));

        assertTrue(CrewArmorKitRules.coversSomethingIn(kit(COMBAT_SUIT), toxicAir),
              "the combat suit's own air supply is the interpretation this rule exists for");
        assertTrue(CrewArmorKitRules.coversSomethingIn(kit(HOSTILE_ENVIRONMENT_SUIT), toxicAir),
              "an environment suit carries the toxic flag itself and needs no interpretation");
        assertFalse(CrewArmorKitRules.coversSomethingIn(kit(COVERALLS), toxicAir));

        assertTrue(CrewArmorKitRules.coversSomethingIn(kit(COMBAT_SUIT), extremeHeat),
              "it is armored cooling gear");
        assertFalse(CrewArmorKitRules.coversSomethingIn(kit(COMBAT_SUIT), extremeCold),
              "cooling gear is not described as keeping anyone warm");
        assertTrue(CrewArmorKitRules.coversSomethingIn(kit(SNOWSUIT), extremeCold),
              "a snowsuit carries the cold weather flag");

        assertFalse(CrewArmorKitRules.coversSomethingIn(kit(SPACESUIT), fairWeather),
              "nobody needs telling their kit made no difference");
        assertFalse(CrewArmorKitRules.coversSomethingIn(null, toxicAir));
    }

    @Test
    void aFullCombatSuitKitCostsTheSumOfItsThreePieces() {
        assertEquals(20000 + 1400 + 175, CrewArmorKitRules.COMBAT_SUIT_KIT_COST_C_BILLS,
              "combat suit, combat neurohelmet and plasteel boots (A Time of War p.294)");
    }
}
