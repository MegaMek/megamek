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
package megamek.common.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import megamek.common.Hex;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers which units leftover swarm missiles may pick as a secondary target (GitHub issue #8911). Conventional
 * infantry inside a building cannot be shot at from outside it (TW p.172), so the search that hands leftover missiles
 * a nearby target has to skip them.
 */
class SwarmMissileTargetTest {

    private static final Coords BUILDING_HEX = new Coords(5, 5);
    private static final Coords OPEN_HEX = new Coords(5, 4);
    private static final Coords ATTACKER_HEX = new Coords(5, 2);
    private static final int WEAPON_ID = 0;
    private static final int BOARD_WIDTH = 16;
    private static final int BOARD_HEIGHT = 17;

    private Game game;
    private Entity attacker;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        game.addPlayer(0, new Player(0, "Test"));
        // Built here rather than loaded from a board string: the test board loader fills hexes in file order
        // rather than at the coordinates written, which would put the building in the wrong hex.
        Board board = new Board(BOARD_WIDTH, BOARD_HEIGHT);
        for (int x = 0; x < BOARD_WIDTH; x++) {
            for (int y = 0; y < BOARD_HEIGHT; y++) {
                board.setHex(x, y, new Hex());
            }
        }
        Hex buildingHex = board.getHex(BUILDING_HEX);
        buildingHex.addTerrain(new Terrain(Terrains.BUILDING, 2));
        buildingHex.addTerrain(new Terrain(Terrains.BLDG_ELEV, 2));
        buildingHex.addTerrain(new Terrain(Terrains.BLDG_CF, 40));
        game.setBoard(board);

        attacker = addUnit(new BipedMek(), ATTACKER_HEX);
    }

    private <E extends Entity> E addUnit(E entity, Coords position) {
        entity.setOwner(game.getPlayer(0));
        entity.setId(game.getNextEntityId());
        entity.setGame(game);
        entity.setCrew(new Crew(CrewType.SINGLE));
        // Position first, and deployed: the hex lookup skips anything that is not a targetable, on-board unit.
        entity.setPosition(position);
        entity.setDeployed(true);
        game.addEntity(entity);
        return entity;
    }

    /** The original target hex is scanned first, so put the candidates there. */
    private Entity swarmTargetInBuildingHex() {
        return Compute.getSwarmMissileTarget(game, attacker.getId(), BUILDING_HEX, WEAPON_ID);
    }

    @Test
    void infantryShelteringInABuildingIsNotPickedFromOutside() {
        addUnit(new ConvInfantry(), BUILDING_HEX);
        assertNull(swarmTargetInBuildingHex(),
              "missiles from outside must not be handed infantry sheltering in a building");
    }

    @Test
    void infantryInTheOpenIsStillPicked() {
        ConvInfantry inTheOpen = addUnit(new ConvInfantry(), OPEN_HEX);
        Entity picked = Compute.getSwarmMissileTarget(game, attacker.getId(), OPEN_HEX, WEAPON_ID);
        assertSame(inTheOpen, picked, "infantry standing in the open is a legal secondary target");
    }

    @Test
    void aMekInTheBuildingIsStillPicked() {
        // The building absorption rules already cover a unit that is not infantry.
        Mek insideTheBuilding = addUnit(new BipedMek(), BUILDING_HEX);
        assertSame(insideTheBuilding, swarmTargetInBuildingHex(),
              "only infantry gets the shelter, not everything standing in the hex");
    }

    @Test
    void aUnitBehindShelteringInfantryIsStillFound() {
        // The adjacent-hex scan used to look at one unit per hex, so a rejected shelterer hid whatever stood
        // behind it.
        addUnit(new ConvInfantry(), BUILDING_HEX);
        Mek alsoThere = addUnit(new BipedMek(), BUILDING_HEX);
        // Searching from the open hex reaches the building hex through the adjacent-hex scan, which is the loop
        // that only ever looked at one unit.
        Entity picked = Compute.getSwarmMissileTarget(game, attacker.getId(), OPEN_HEX, WEAPON_ID);
        assertNotNull(picked, "a legal target in the hex must still be found");
        assertEquals(alsoThere.getId(), picked.getId(), "the legal target is the one that is not sheltering");
    }
}
