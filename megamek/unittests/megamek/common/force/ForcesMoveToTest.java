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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Player;
import megamek.common.game.Game;
import megamek.common.interfaces.ForceAssignable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Dragging a force or unit between two rows of the lobby tree moves it to a given place in its list, through
 * {@link Forces#moveTo(Force, int)} and {@link Forces#moveTo(ForceAssignable, int)}.
 */
class ForcesMoveToTest {

    private static final int PLAYER_ID = 0;
    private static final int TEAMMATE_ID = 1;

    private Forces forces;
    private Player player;
    private Player teammate;

    @BeforeEach
    void setUp() {
        Game game = new Game();
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

    private Force addSubForce(String name, Force parent) {
        return forces.getForce(forces.addSubForce(Force.createSubforce(name, parent), parent));
    }

    private ForceAssignable addUnit(int unitId, Force force) {
        ForceAssignable unit = mock(ForceAssignable.class);
        when(unit.getId()).thenReturn(unitId);
        forces.addEntity(unit, force.getId());
        return unit;
    }

    private List<String> topLevelNames(int ownerId) {
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

    private List<String> subForceNames(Force parent) {
        List<String> names = new ArrayList<>();
        for (int subForceId : parent.getSubForces()) {
            names.add(forces.getForce(subForceId).getName());
        }
        return names;
    }

    @Test
    void movesATopLevelForceToTheFirstPlace() {
        addTopLevel("Alpha", player);
        addTopLevel("Bravo", player);
        Force charlie = addTopLevel("Charlie", player);

        forces.moveTo(charlie, 0);

        assertEquals(List.of("Charlie", "Alpha", "Bravo"), topLevelNames(PLAYER_ID));
    }

    @Test
    void movesATopLevelForceToTheLastPlace() {
        Force alpha = addTopLevel("Alpha", player);
        addTopLevel("Bravo", player);
        addTopLevel("Charlie", player);

        forces.moveTo(alpha, 2);

        assertEquals(List.of("Bravo", "Charlie", "Alpha"), topLevelNames(PLAYER_ID));
    }

    @Test
    void aPlacePastTheEndMeansTheEnd() {
        Force alpha = addTopLevel("Alpha", player);
        addTopLevel("Bravo", player);

        forces.moveTo(alpha, 99);

        assertEquals(List.of("Bravo", "Alpha"), topLevelNames(PLAYER_ID));
    }

    @Test
    void movingToTheSamePlaceChangesNothing() {
        addTopLevel("Alpha", player);
        Force bravo = addTopLevel("Bravo", player);

        assertTrue(forces.moveTo(bravo, 1).isEmpty());
        assertEquals(List.of("Alpha", "Bravo"), topLevelNames(PLAYER_ID));
    }

    @Test
    void aTopLevelMoveLeavesOtherOwnersAlone() {
        addTopLevel("Alpha", player);
        addTopLevel("Teammate First", teammate);
        Force bravo = addTopLevel("Bravo", player);
        addTopLevel("Teammate Second", teammate);

        List<Force> changed = forces.moveTo(bravo, 0);

        assertEquals(List.of("Bravo", "Alpha"), topLevelNames(PLAYER_ID));
        assertEquals(List.of("Teammate First", "Teammate Second"), topLevelNames(TEAMMATE_ID));
        for (Force force : changed) {
            assertEquals(PLAYER_ID, force.getOwnerId(), "only the mover's own forces may change");
        }
    }

    @Test
    void movesASubForceWithinItsParent() {
        Force battalion = addTopLevel("Battalion", player);
        addSubForce("Alpha Lance", battalion);
        addSubForce("Bravo Lance", battalion);
        Force charlieLance = addSubForce("Charlie Lance", battalion);

        List<Force> changed = forces.moveTo(charlieLance, 1);

        assertEquals(List.of("Alpha Lance", "Charlie Lance", "Bravo Lance"), subForceNames(battalion));
        assertEquals(List.of(battalion), changed, "the parent holds the order and is what reaches the server");
    }

    @Test
    void movesAUnitWithinItsForce() {
        Force lance = addTopLevel("Lance", player);
        addUnit(10, lance);
        addUnit(11, lance);
        ForceAssignable third = addUnit(12, lance);

        List<Force> changed = forces.moveTo(third, 0);

        assertEquals(List.of(12, 10, 11), lance.getEntities());
        assertEquals(List.of(lance), changed);
    }

    @Test
    void aUnitInNoForceDoesNotMove() {
        ForceAssignable loose = mock(ForceAssignable.class);
        when(loose.getId()).thenReturn(20);

        assertTrue(forces.moveTo(loose, 0).isEmpty());
    }
}
