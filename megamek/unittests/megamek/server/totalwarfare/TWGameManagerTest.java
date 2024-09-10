/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.totalwarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Vector;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import megamek.common.AeroSpaceFighter;
import megamek.common.EquipmentType;
import megamek.common.Game;
import megamek.common.LandAirMek;
import megamek.common.PilotingRollData;
import megamek.common.Player;

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
