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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.Vector;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.net.packets.Packet;
import megamek.common.units.Tank;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * A vehicle driven into water with a location already stripped of armour is breached there and destroyed, TW p.121:
 * a location with no armour left is automatically breached "whether this occurred before the unit entered the water
 * hex or while it is submerged".
 * <p>
 * It should be told that once. A moving unit has its exposure set twice - once per step while it moves, and again
 * when movement ends - and a playtest saw the resulting report announce "RS BREACHED", destroy the tank, and then
 * announce "RS BREACHED" a second time underneath.
 */
class WaterBreachReportedOnceTest {

    private static final int BREACH_REPORT = 6350;

    private TWGameManager gameManager;
    private Game game;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).sendServerChat(anyString());
        game = gameManager.getGame();
        game.addPlayer(0, new Player(0, "Test"));
        game.setPhase(GamePhase.MOVEMENT);
    }

    private Tank tankWithStrippedRightSide() {
        Tank tank = new Tank();
        tank.setGame(game);
        tank.setOwner(game.getPlayer(0));
        tank.setId(1);
        game.addEntity(tank);
        for (int location = 0; location < tank.locations(); location++) {
            tank.initializeArmor(10, location);
        }
        tank.initializeArmor(0, Tank.LOC_RIGHT);
        tank.setElevation(-1);
        return tank;
    }

    private static Hex waterHex() {
        Hex hex = new Hex();
        hex.addTerrain(new Terrain(Terrains.WATER, 1));
        return hex;
    }

    private static long breachReports(Vector<Report> reports) {
        return reports.stream().filter(report -> report.messageId == BREACH_REPORT).count();
    }

    @Test
    @DisplayName("A stripped location is breached on entering water")
    void aStrippedLocationBreachesOnEnteringWater() {
        Tank tank = tankWithStrippedRightSide();

        assertEquals(1, breachReports(gameManager.doSetLocationsExposure(tank, waterHex(), false, -1)),
              "the right side has no armour left, so the water gets in");
    }

    @Test
    @DisplayName("The same breach is not announced twice")
    void theSameBreachIsAnnouncedOnce() {
        Tank tank = tankWithStrippedRightSide();
        Hex hex = waterHex();

        gameManager.doSetLocationsExposure(tank, hex, false, -1);
        Vector<Report> secondPass = gameManager.doSetLocationsExposure(tank, hex, false, -1);

        assertEquals(0, breachReports(secondPass),
              "movement sets exposure once per step and again at the end; the second pass must not re-announce "
                    + "a hole the first one already reported");
    }

    @Test
    @DisplayName("A breached vehicle location stays breached")
    void aBreachedLocationStaysBreached() {
        Tank tank = tankWithStrippedRightSide();

        gameManager.doSetLocationsExposure(tank, waterHex(), false, -1);

        assertEquals(ILocationExposureStatus.BREACHED, tank.getLocationStatus(Tank.LOC_RIGHT),
              "a breach does not heal by moving, and leaving it marked is what stops the repeat");
    }
}
