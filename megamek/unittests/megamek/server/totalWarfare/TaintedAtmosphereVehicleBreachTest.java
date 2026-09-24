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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Vector;

import megamek.common.Player;
import megamek.common.Report;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.net.packets.Packet;
import megamek.common.planetaryConditions.AtmosphericTaint;
import megamek.common.units.Tank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Tests what a hull breach does to a vehicle crew in fouled air, TO:AR p.54.
 * <p>
 * A salvo can breach several locations of the same vehicle in one attack. A playtest saw an Ontos breached in the
 * right side, the right rear and the front, and announce "the crew is killed" all three times, so the guard against
 * harming a crew that is already lost is what most of this covers.
 */
class TaintedAtmosphereVehicleBreachTest {

    private TWGameManager gameManager;
    private Game game;
    private TaintedAtmosphereHandler handler;

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendServerChat(anyString());

        game = gameManager.getGame();
        game.addPlayer(0, new Player(0, "Test"));
        game.setPhase(GamePhase.FIRING);
        handler = new TaintedAtmosphereHandler(gameManager);
    }

    private Tank vehicleInAir(AtmosphericTaint atmosphericTaint) {
        game.getPlanetaryConditions().setAtmosphericTaint(atmosphericTaint);
        Tank tank = new Tank();
        tank.setGame(game);
        tank.setOwner(game.getPlayer(0));
        tank.setId(1);
        game.addEntity(tank);
        return tank;
    }

    @Test
    @DisplayName("The first breach in toxic air kills the crew")
    void firstBreachKillsTheCrew() {
        Tank tank = vehicleInAir(AtmosphericTaint.TOXIC_CAUSTIC);

        Vector<Report> reports = handler.resolveVehicleBreach(tank, Tank.LOC_RIGHT);

        assertFalse(reports.isEmpty(), "the breach should be reported");
        assertTrue(tank.getCrew().isDoomed() || tank.getCrew().isDead(),
              "the toxic atmosphere should have killed the crew");
        assertEquals(ILocationExposureStatus.BREACHED, tank.getLocationStatus(Tank.LOC_RIGHT),
              "the breached location should be marked so it is not rolled for again");
    }

    @Test
    @DisplayName("Later breaches in the same salvo do not kill an already dead crew again")
    void aSecondBreachDoesNotKillTheCrewTwice() {
        Tank tank = vehicleInAir(AtmosphericTaint.TOXIC_CAUSTIC);
        handler.resolveVehicleBreach(tank, Tank.LOC_RIGHT);

        Vector<Report> secondBreachReports = handler.resolveVehicleBreach(tank, Tank.LOC_FRONT);

        assertTrue(secondBreachReports.isEmpty(),
              "a crew that is already lost should not be reported killed a second time");
        assertEquals(ILocationExposureStatus.BREACHED, tank.getLocationStatus(Tank.LOC_FRONT),
              "the second hole is still real even though it harms nobody");
    }

    @Test
    @DisplayName("A breach in caustic tainted air stuns the crew instead of killing it")
    void aBreachInTaintedAirStunsTheCrew() {
        Tank tank = vehicleInAir(AtmosphericTaint.TAINTED_CAUSTIC);

        Vector<Report> reports = handler.resolveVehicleBreach(tank, Tank.LOC_RIGHT);

        assertFalse(reports.isEmpty(), "the breach should be reported");
        assertFalse(tank.getCrew().isDoomed() || tank.getCrew().isDead(),
              "tainted air stuns the crew, it does not kill them");
        assertTrue(tank.getStunnedTurns() > 0, "the crew should be stunned");
    }

    @Test
    @DisplayName("A breach in air that the rules give no breach effect for harms nobody")
    void aBreachInFlammableAirHarmsNobody() {
        Tank tank = vehicleInAir(AtmosphericTaint.TAINTED_FLAME);

        Vector<Report> reports = handler.resolveVehicleBreach(tank, Tank.LOC_RIGHT);

        assertTrue(reports.isEmpty(), "flammable air has no breach effect on a crew");
        assertFalse(tank.getCrew().isDoomed() || tank.getCrew().isDead(),
              "the crew should be untouched");
    }
}
