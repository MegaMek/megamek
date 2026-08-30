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

package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import java.util.Vector;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.options.OptionsConstants;
import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.AtmosphericTaint;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests that the ejection report actually says when a combat suit is what keeps the crew alive.
 * <p>
 * This goes through {@code ejectEntity} rather than testing the condition in isolation, because the condition was
 * never the hard part: the line was first written into the wrong method and so never appeared in a game at all.
 */
class CombatSuitEjectionReportTest {

    private static final int COMBAT_SUIT_REPORT = 6411;
    private static final String BOARD_DATA = """
          size 4 4
          hex 0101 0 "" ""
          hex 0102 0 "" ""
          hex 0103 0 "" ""
          hex 0104 0 "" ""
          end""";

    private TWGameManager gameManager;
    private Game game;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).entityUpdate(anyInt());

        game = gameManager.getGame();
        game.addPlayer(0, new Player(0, "Test"));
        Board board = BoardLoader.initializeBoard(BOARD_DATA);
        game.setBoard(board);

        game.getOptions().getOption(OptionsConstants.RPG_COMBAT_SUITS).setValue(true);
        game.getOptions().getOption(OptionsConstants.ALLOWED_YEAR).setValue(3025);
    }

    private void setCausticToxicAir() {
        game.getPlanetaryConditions().setAtmosphere(Atmosphere.STANDARD);
        game.getPlanetaryConditions().setAtmosphericTaint(AtmosphericTaint.CAUSTIC_TOXIC);
    }

    private <T extends Entity> T deploy(T entity, int entityId, boolean issueCombatSuit) {
        entity.setId(entityId);
        entity.setOwner(game.getPlayer(0));
        entity.getCrew().setHasCombatSuit(issueCombatSuit, 0);
        game.addEntity(entity);
        entity.setPosition(new Coords(0, 0));
        entity.setDeployed(true);
        return entity;
    }

    /**
     * A Mek built the way ejection expects to find one. A bare {@code BipedMek} has no critical slots, so the
     * cockpit-destruction step that follows every ejection would fail on nothing rather than on the rule.
     */
    private Mek deployedMek(int entityId, boolean issueCombatSuit) {
        Mek mek = new BipedMek();
        mek.autoSetInternal();
        mek.addCockpit();
        return deploy(mek, entityId, issueCombatSuit);
    }

    private boolean reportsTheCombatSuit(Vector<Report> reports) {
        return reports.stream().anyMatch(report -> report.messageId == COMBAT_SUIT_REPORT);
    }

    @Test
    void aSuitedMekPilotIsToldTheSuitWillKeepThemAlive() {
        setCausticToxicAir();
        Mek mek = deployedMek(1, true);

        assertTrue(reportsTheCombatSuit(gameManager.ejectEntity(mek, false, false)),
              "the report must say the suit is what saves them, or the player cannot tell it did anything");
    }

    @Test
    void aSuitedVehicleCrewIsToldTheSame() {
        setCausticToxicAir();
        Tank tank = deploy(new Tank(), 2, true);

        assertTrue(reportsTheCombatSuit(gameManager.ejectEntity(tank, false, false)),
              "a vehicle crew abandons through the same method, so it must reach the same line");
    }

    @Test
    void anUnsuitedCrewIsToldNothing() {
        setCausticToxicAir();
        Mek mek = deployedMek(3, false);

        assertFalse(reportsTheCombatSuit(gameManager.ejectEntity(mek, false, false)));
    }

    @Test
    void aSuitedCrewInOrdinaryWeatherIsToldNothing() {
        game.getPlanetaryConditions().setAtmosphere(Atmosphere.STANDARD);
        game.getPlanetaryConditions().setAtmosphericTaint(AtmosphericTaint.BREATHABLE);
        Mek mek = deployedMek(4, true);

        assertFalse(reportsTheCombatSuit(gameManager.ejectEntity(mek, false, false)),
              "there is nothing out there for the suit to save them from");
    }

    @Test
    void aSuitedCrewEjectingIntoVacuumIsNotToldTheyAreSafe() {
        game.getPlanetaryConditions().setAtmosphere(Atmosphere.VACUUM);
        Mek mek = deployedMek(5, true);

        assertFalse(reportsTheCombatSuit(gameManager.ejectEntity(mek, false, false)),
              "the kit holds no pressure, so saying they are protected would be a lie");
    }

    @Test
    void theReportStaysAwayWithTheOptionOff() {
        setCausticToxicAir();
        game.getOptions().getOption(OptionsConstants.RPG_COMBAT_SUITS).setValue(false);
        Mek mek = deployedMek(6, true);

        assertFalse(reportsTheCombatSuit(gameManager.ejectEntity(mek, false, false)));
    }
}
