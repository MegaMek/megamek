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
package megamek.common.force;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.util.SerializationHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Issue #9321: top-level forces can be moved up and down in the lobby. The order is kept per owner, is what the lobby
 * tree and unit list saves list, and survives a game save.
 */
class ForcesTopLevelOrderTest {

    private static final int PLAYER_ID = 0;
    private static final int TEAMMATE_ID = 1;

    private Game game;
    private Forces forces;
    private Player player;
    private Player teammate;

    @BeforeEach
    void setUp() {
        game = new Game();
        player = new Player(PLAYER_ID, "Player");
        player.setTeam(1);
        game.addPlayer(PLAYER_ID, player);
        teammate = new Player(TEAMMATE_ID, "Teammate");
        teammate.setTeam(1);
        game.addPlayer(TEAMMATE_ID, teammate);
        forces = game.getForces();
    }

    private Force addTopLevel(String name, Player owner) {
        return forces.getForce(forces.addTopLevelForce(Force.createToplevelForce(name, owner), owner));
    }

    /** The names of the given owner's top-level forces, in the order the lobby lists them. */
    private static List<String> topLevelNames(Forces forces, int ownerId) {
        List<Force> topLevelForces = new ArrayList<>(forces.getTopLevelForces());
        topLevelForces.sort(Forces.TOP_LEVEL_ORDER);
        List<String> names = new ArrayList<>();
        for (Force force : topLevelForces) {
            if (force.getOwnerId() == ownerId) {
                names.add(force.getName());
            }
        }
        return names;
    }

    @Test
    void newForcesAreListedInCreationOrder() {
        addTopLevel("1st Battalion", player);
        addTopLevel("Command Lance", player);

        assertEquals(List.of("1st Battalion", "Command Lance"), topLevelNames(forces, PLAYER_ID));
    }

    @Test
    void moveUpListsATopLevelForceEarlier() {
        Force battalion = addTopLevel("1st Battalion", player);
        Force commandLance = addTopLevel("Command Lance", player);

        List<Force> changed = forces.moveUp(commandLance);

        assertEquals(List.of("Command Lance", "1st Battalion"), topLevelNames(forces, PLAYER_ID));
        assertTrue(changed.contains(commandLance) && changed.contains(battalion),
              "both forces whose place changed must be sent to the server");
    }

    @Test
    void moveDownListsATopLevelForceLater() {
        Force alpha = addTopLevel("Alpha", player);
        addTopLevel("Bravo", player);
        addTopLevel("Charlie", player);

        forces.moveDown(alpha);

        assertEquals(List.of("Bravo", "Alpha", "Charlie"), topLevelNames(forces, PLAYER_ID));
    }

    @Test
    void movingPastEitherEndChangesNothing() {
        Force alpha = addTopLevel("Alpha", player);
        Force bravo = addTopLevel("Bravo", player);

        assertTrue(forces.moveUp(alpha).isEmpty());
        assertTrue(forces.moveDown(bravo).isEmpty());
        assertEquals(List.of("Alpha", "Bravo"), topLevelNames(forces, PLAYER_ID));
    }

    @Test
    void forcesFromAnOldSaveMoveOnTheFirstClick() {
        // A save made before positions were stored loads every force at position 0
        Force alpha = addTopLevel("Alpha", player);
        Force bravo = addTopLevel("Bravo", player);
        Force charlie = addTopLevel("Charlie", player);
        alpha.setPosition(0);
        bravo.setPosition(0);
        charlie.setPosition(0);
        assertEquals(List.of("Alpha", "Bravo", "Charlie"), topLevelNames(forces, PLAYER_ID),
              "precondition: ties keep the old id order");

        forces.moveUp(charlie);

        assertEquals(List.of("Alpha", "Charlie", "Bravo"), topLevelNames(forces, PLAYER_ID));
    }

    @Test
    void movingOneOwnersForceLeavesOtherOwnersAlone() {
        addTopLevel("Alpha", player);
        Force teammateFirst = addTopLevel("Teammate First", teammate);
        Force bravo = addTopLevel("Bravo", player);
        addTopLevel("Teammate Second", teammate);
        int teammateFirstPosition = teammateFirst.getPosition();

        List<Force> changed = forces.moveUp(bravo);

        assertEquals(List.of("Bravo", "Alpha"), topLevelNames(forces, PLAYER_ID));
        assertEquals(List.of("Teammate First", "Teammate Second"), topLevelNames(forces, TEAMMATE_ID));
        assertEquals(teammateFirstPosition, teammateFirst.getPosition());
        for (Force force : changed) {
            assertEquals(PLAYER_ID, force.getOwnerId(), "only the mover's own forces may change");
        }
    }

    @Test
    void movingASubForceStillWorks() {
        Force battalion = addTopLevel("Battalion", player);
        forces.addSubForce(Force.createSubforce("First Lance", battalion), battalion);
        int secondLanceId = forces.addSubForce(Force.createSubforce("Second Lance", battalion), battalion);

        forces.moveUp(forces.getForce(secondLanceId));

        assertEquals(secondLanceId, battalion.getSubForceId(0));
    }

    @Test
    void aPromotedSubForceIsListedLast() {
        Force battalion = addTopLevel("Battalion", player);
        Force lance = forces.getForce(forces.addSubForce(Force.createSubforce("Lance", battalion), battalion));
        addTopLevel("Reserve", player);

        forces.promoteForce(lance);

        assertEquals(List.of("Battalion", "Reserve", "Lance"), topLevelNames(forces, PLAYER_ID));
    }

    @Test
    void aCloneKeepsTheOrder() {
        // The server checks an incoming update against a clone of its forces before accepting it
        addTopLevel("Alpha", player);
        Force bravo = addTopLevel("Bravo", player);
        forces.moveUp(bravo);

        assertEquals(List.of("Bravo", "Alpha"), topLevelNames(forces.clone(), PLAYER_ID));
    }

    @Test
    void theOrderSurvivesAGameSave() {
        addTopLevel("Alpha", player);
        Force bravo = addTopLevel("Bravo", player);
        forces.moveUp(bravo);

        String savedXml = SerializationHelper.getSaveGameXStream().toXML(forces);
        Forces restored = (Forces) SerializationHelper.getLoadSaveGameXStream().fromXML(savedXml);

        assertEquals(List.of("Bravo", "Alpha"), topLevelNames(restored, PLAYER_ID));
    }

    @Test
    void aSaveWithoutPositionsLoadsInIdOrder() {
        addTopLevel("Alpha", player);
        Force bravo = addTopLevel("Bravo", player);
        forces.moveUp(bravo);

        String savedXml = SerializationHelper.getSaveGameXStream().toXML(forces);
        String legacyXml = savedXml.replaceAll("\\s*<position>\\d+</position>", "");
        assertTrue(!legacyXml.contains("<position>"), "precondition: the legacy save has no positions");
        Forces restored = (Forces) SerializationHelper.getLoadSaveGameXStream().fromXML(legacyXml);

        assertEquals(List.of("Alpha", "Bravo"), topLevelNames(restored, PLAYER_ID));
    }
}
