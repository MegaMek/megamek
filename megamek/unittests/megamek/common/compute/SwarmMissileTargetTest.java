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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

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
import megamek.common.units.Terrain;
import megamek.common.units.Terrains;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Which units leftover swarm missiles may pick as a secondary target (TO:AUE p.183).
 *
 * <p>The rule is deliberately wide: any unit, friendly or enemy, in the target's hex or an adjacent one, nearest
 * first, chosen at random among those at the same distance, and "no form of line of sight from the attacker to the
 * secondary target is required". A unit sheltering in a building is therefore a legal target, and so is the
 * launching unit itself.</p>
 */
class SwarmMissileTargetTest {

    private static final int BOARD_WIDTH = 16;
    private static final int BOARD_HEIGHT = 17;
    private static final Coords BUILDING_HEX = new Coords(5, 5);
    private static final Coords OPEN_HEX = new Coords(5, 4);
    private static final Coords FAR_HEX = new Coords(10, 10);
    private static final int WEAPON_ID = 0;

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

        attacker = addUnit(new BipedMek(), FAR_HEX);
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

    private Entity swarmTargetAt(Coords coords) {
        return Compute.getSwarmMissileTarget(game, attacker.getId(), coords, WEAPON_ID);
    }

    @Test
    void infantryShelteringInABuildingIsStillAValidTarget() {
        // TO:AUE p.183: no line of sight to the secondary target is required, so the building does not protect the
        // platoon from being chosen. Whether the building then absorbs the damage is a separate question.
        ConvInfantry sheltering = addUnit(new ConvInfantry(), BUILDING_HEX);
        assertEquals(sheltering.getId(), swarmTargetAt(BUILDING_HEX).getId(),
              "a platoon inside a building may be picked as a secondary target");
    }

    @Test
    void aUnitAlreadyHitByThisFlightIsNotPickedAgain() {
        // "Neither the original primary target nor any secondary targets may be attacked more than once."
        ConvInfantry platoon = addUnit(new ConvInfantry(), BUILDING_HEX);
        platoon.addTargetedBySwarm(attacker.getId(), WEAPON_ID);
        assertNull(swarmTargetAt(BUILDING_HEX),
              "a unit this flight has already attacked must not be picked again");
    }

    @Test
    void theTargetsOwnHexIsPreferredOverAnAdjacentOne() {
        // "starting from the nearest unit (beginning with any units in the target's hex and moving outward)"
        Entity inTheHex = addUnit(new BipedMek(), BUILDING_HEX);
        addUnit(new BipedMek(), OPEN_HEX);
        assertEquals(inTheHex.getId(), swarmTargetAt(BUILDING_HEX).getId(),
              "a unit in the target's own hex outranks one in an adjacent hex");
    }

    @RepeatedTest(20)
    void everyUnitInAnAdjacentHexIsACandidate() {
        // "If multiple secondary targets lie within the same distance, the secondary target is chosen at random."
        // The scan used to look at one unit per adjacent hex, so the rest could never be picked.
        Entity first = addUnit(new BipedMek(), BUILDING_HEX);
        Entity second = addUnit(new BipedMek(), BUILDING_HEX);
        Set<Integer> everPicked = new HashSet<>();
        for (int attempt = 0; attempt < 40; attempt++) {
            Entity picked = swarmTargetAt(OPEN_HEX);
            assertNotNull(picked, "a candidate in the adjacent hex must be found");
            everPicked.add(picked.getId());
        }
        assertTrue(everPicked.contains(first.getId()) && everPicked.contains(second.getId()),
              "both units in the adjacent hex must be reachable, picked: " + everPicked);
    }

    @Test
    void nothingNearbyMeansTheMissilesAreLost() {
        assertNull(swarmTargetAt(OPEN_HEX), "with no unit in range the flight finds no target");
    }
}
