/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.server.totalwarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Vector;

import megamek.common.Player;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.rolls.PilotingRollData;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.LandAirMek;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TWGameManagerTest {
    private Player player;
    private TWGameManager gameManager;
    private Game game;

    @BeforeAll
    static void before() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        player = new Player(0, "Test");
        gameManager = new TWGameManager();
        game = gameManager.getGame();
        game.addPlayer(0, player);
    }

    @Test
    void testAddControlWithAdvAtmosphericMergesIntoOneRollAero() {
        AeroSpaceFighter aero = new AeroSpaceFighter();
        game.addEntity(aero);
        Vector<PilotingRollData> rolls = new Vector<>();
        StringBuilder reasons = new StringBuilder();

        game.addControlRoll(new PilotingRollData(aero.getId(), 0, "critical hit"));
        game.addControlRoll(new PilotingRollData(aero.getId(), 0, "avionics hit"));
        game.addControlRoll(new PilotingRollData(aero.getId(), 0, "threshold"));
        game.addControlRoll(new PilotingRollData(aero.getId(), 0, "highest damage threshold exceeded"));
        gameManager.addControlWithAdvAtmospheric(aero, rolls, reasons);
        assertEquals(1, rolls.size());
    }

    @Test
    void testAddControlWithAdvAtmosphericIncludesAllReasonsAero() {
        AeroSpaceFighter aero = new AeroSpaceFighter();
        game.addEntity(aero);
        Vector<PilotingRollData> rolls = new Vector<>();
        StringBuilder reasons = new StringBuilder();

        game.addControlRoll(new PilotingRollData(aero.getId(), 0, "critical hit"));
        game.addControlRoll(new PilotingRollData(aero.getId(), 0, "avionics hit"));
        game.addControlRoll(new PilotingRollData(aero.getId(), 0, "threshold"));
        game.addControlRoll(new PilotingRollData(aero.getId(), 0, "highest damage threshold exceeded"));
        gameManager.addControlWithAdvAtmospheric(aero, rolls, reasons);
        assertTrue(reasons.toString().contains("critical hit"));
        assertTrue(reasons.toString().contains("avionics hit"));
        assertTrue(reasons.toString().contains("threshold"));
        assertTrue(reasons.toString().contains("highest damage threshold exceeded"));
    }

    @Test
    void testAddControlWithAdvAtmosphericMergesIntoOneRollLAM() {
        LandAirMek mek = new LandAirMek(LandAirMek.GYRO_STANDARD, LandAirMek.COCKPIT_STANDARD,
              LandAirMek.LAM_STANDARD);
        game.addEntity(mek);
        Vector<PilotingRollData> rolls = new Vector<>();
        StringBuilder reasons = new StringBuilder();

        game.addControlRoll(new PilotingRollData(mek.getId(), 0, "avionics hit"));
        game.addControlRoll(new PilotingRollData(mek.getId(), 0, "threshold"));
        game.addControlRoll(new PilotingRollData(mek.getId(), 0, "highest damage threshold exceeded"));
        gameManager.addControlWithAdvAtmospheric(mek, rolls, reasons);
        assertEquals(1, rolls.size());
    }

    @Test
    void testAddControlWithAdvAtmosphericIncludesAllReasonsLAM() {
        LandAirMek mek = new LandAirMek(LandAirMek.GYRO_STANDARD, LandAirMek.COCKPIT_STANDARD,
              LandAirMek.LAM_STANDARD);
        game.addEntity(mek);
        Vector<PilotingRollData> rolls = new Vector<>();
        StringBuilder reasons = new StringBuilder();

        game.addControlRoll(new PilotingRollData(mek.getId(), 0, "avionics hit"));
        game.addControlRoll(new PilotingRollData(mek.getId(), 0, "threshold"));
        game.addControlRoll(new PilotingRollData(mek.getId(), 0, "highest damage threshold exceeded"));
        gameManager.addControlWithAdvAtmospheric(mek, rolls, reasons);
        assertTrue(reasons.toString().contains("avionics hit"));
        assertTrue(reasons.toString().contains("threshold"));
        assertTrue(reasons.toString().contains("highest damage threshold exceeded"));
    }
}
