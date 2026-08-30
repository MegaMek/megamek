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
package megamek.client.ui.dialogs.unitDisplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Player;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BipedMek;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Mek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the heat forecast the unit display shows: what carries over, what is capped, and that a weapon already fired
 * this phase has already spent its heat.
 */
@DisplayName("HeatForecast")
class HeatForecastTest {

    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test Player"));
        game.setPhase(GamePhase.FIRING);
    }

    private BipedMek createMek() {
        BipedMek mek = new BipedMek();
        mek.setGame(game);
        mek.setId(1);
        mek.setChassis("Test Mek");
        mek.setModel("Standard");
        mek.setWeight(50);
        mek.setCrew(new Crew(CrewType.SINGLE));
        mek.setOwner(game.getPlayer(0));
        mek.autoSetInternal();
        return mek;
    }

    @Test
    @DisplayName("Carried heat, capped external heat and capped external cooling add up")
    void carriedAndExternalHeatAddUp() {
        BipedMek mek = createMek();
        mek.heat = 5;
        mek.heatBuildup = 3;
        mek.heatFromExternal = 20; // capped at the TW p.159 standard of 15 without a game
        mek.coolFromExternal = 12; // capped at 9

        HeatForecast.Result result = HeatForecast.forecast(mek, null);

        assertEquals(5 + 3 + 15 - 9, result.buildup(), "5 carried + 3 building + 15 external - 9 cooling");
        assertFalse(result.firedThisPhase(), "no weapon has fired");
        assertEquals(result.buildup() - result.capacity(), result.overCapacity());
    }

    @Test
    @DisplayName("External cooling never takes the forecast below zero")
    void coolingClampsAtZero() {
        BipedMek mek = createMek();
        mek.coolFromExternal = 9;

        assertEquals(0, HeatForecast.forecast(mek, null).buildup(), "a cool unit forecasts zero, not negative heat");
    }

    @Test
    @DisplayName("The Combat Computer quirk takes four off")
    void combatComputerReducesHeat() {
        BipedMek mek = createMek();
        mek.heat = 10;
        // quirks only count when the StratOps quirks option is on
        game.getOptions().getOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS).setValue(true);
        mek.getQuirks().getOption(OptionsConstants.QUIRK_POS_COMBAT_COMPUTER).setValue(true);

        HeatForecast.Result result = HeatForecast.forecast(mek, game);

        assertTrue(result.combatComputer());
        assertEquals(6, result.buildup(), "10 carried - 4 for the combat computer");
    }

    @Test
    @DisplayName("A weapon fired this phase has already spent its heat")
    void firedWeaponHeatIsCommitted() throws Exception {
        BipedMek mek = createMek();
        game.addEntity(mek);
        Mounted<?> mediumLaser = mek.addEquipment(EquipmentType.get("ISMediumLaser"), Mek.LOC_RIGHT_ARM);
        int laserHeat = mediumLaser.getType().getHeat();
        assertTrue(laserHeat > 0, "the test weapon must generate heat");

        HeatForecast.Result before = HeatForecast.forecast(mek, game);
        mediumLaser.setUsedThisRound(true);
        HeatForecast.Result after = HeatForecast.forecast(mek, game);

        assertFalse(before.firedThisPhase());
        assertTrue(after.firedThisPhase());
        assertEquals(before.buildup() + laserHeat, after.buildup(),
              "declaring the shot commits its heat to the forecast at once");
    }
}
