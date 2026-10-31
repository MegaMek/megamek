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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.Team;
import megamek.common.board.Coords;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.VTOL;
import megamek.utils.ServerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Blowing sand only damages airborne VTOLs that are on the map (issue #9391). A reinforcement waiting to enter has no
 * position yet, and used to take sand damage every End Phase because of its lobby elevation.
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class BlowingSandTargetsTest extends GameBoardTestCase {

    private static final Coords VTOL_HEX = new Coords(0, 0);

    static {
        initializeBoard("SAND_CLEAR", """
              size 1 1
              hex 0101 0 "" ""
              end""");
    }

    private TWGameManager gameManager;
    private Team team;

    @BeforeEach
    void setUp() throws IOException {
        setBoard("SAND_CLEAR");
        gameManager = new TWGameManager();
        gameManager.setGame(getGame());
        ServerFactory.createServer(gameManager);
        if (getGame().getPlayer(0) == null) {
            getGame().addPlayer(0, new Player(0, "Test"));
        }
        team = new Team(1);
        team.addPlayer(getGame().getPlayer(0));
    }

    /** A VTOL flying at elevation 1 that has not been placed on the map. */
    private VTOL addFlyingVtol(int entityId) {
        VTOL vtol = new VTOL();
        vtol.setChassis("Test");
        vtol.setModel("Sand Target " + entityId);
        vtol.setMovementMode(EntityMovementMode.VTOL);
        vtol.setCrew(new Crew(CrewType.CREW));
        vtol.setOwner(getGame().getPlayer(0));
        vtol.setId(entityId);
        getGame().addEntity(vtol);
        vtol.setElevation(1);
        return vtol;
    }

    @Test
    void undeployedVtolIsNotInTheSand() {
        VTOL reinforcement = addFlyingVtol(1);
        reinforcement.setDeployed(false);
        assertTrue(reinforcement.isAirborneVTOLorWIGE(), "Precondition: with no position, elevation 1 reads as flying");

        List<Integer> targets = gameManager.getAirborneVTOLForSand(team);

        assertTrue(targets.isEmpty(), "A VTOL that has not entered the map must not take sand damage");
    }

    @Test
    void deployedAirborneVtolIsStillInTheSand() {
        VTOL flying = addFlyingVtol(2);
        flying.setPosition(VTOL_HEX);
        flying.setDeployed(true);

        List<Integer> targets = gameManager.getAirborneVTOLForSand(team);

        assertEquals(List.of(flying.getId()), targets);
    }
}
