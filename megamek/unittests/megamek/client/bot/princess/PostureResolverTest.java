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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import megamek.common.board.Coords;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostureResolverTest {

    private PostureResolver resolver;
    private BehaviorSettings settings;

    // Our lance stands around (5,5); distances are measured from its centre.
    private final List<Coords> ownPositions = List.of(new Coords(5, 4), new Coords(5, 6));

    @BeforeEach
    void beforeEach() {
        resolver = new PostureResolver();
        settings = new BehaviorSettings();
    }

    private List<Coords> enemyAtDistance(int distance) {
        return List.of(new Coords(5, 5 + distance));
    }

    /** Two enemies at different ranges, for mean distances that land between whole hexes. */
    private List<Coords> enemiesAtDistances(int firstDistance, int secondDistance) {
        return List.of(new Coords(5, 5 + firstDistance), new Coords(5, 5 + secondDistance));
    }

    @Test
    void anExplicitPostureIsObeyedWithoutLookingAtAnything() {
        settings.setCombatPosture(CombatPosture.DEFEND);
        assertEquals(CombatPosture.DEFEND,
              resolver.resolve(settings, 1, List.of(), List.of()));

        settings.setCombatPosture(CombatPosture.ATTACK);
        assertEquals(CombatPosture.ATTACK,
              resolver.resolve(settings, 1, List.of(), List.of()));
    }

    @Test
    void aFleeOrderWithADestinationEdgeMeansTheMissionRequiresMovementSoTheForceAttacks() {
        settings.setAutoFlee(true);
        settings.setDestinationEdge(CardinalEdge.NORTH);
        // Even with the enemy charging at us round after round, a force that must reach an edge attacks.
        assertEquals(CombatPosture.ATTACK, resolver.resolve(settings, 1, ownPositions, enemyAtDistance(20)));
        assertEquals(CombatPosture.ATTACK, resolver.resolve(settings, 2, ownPositions, enemyAtDistance(15)));
        assertEquals(CombatPosture.ATTACK, resolver.resolve(settings, 3, ownPositions, enemyAtDistance(10)));
    }

    /**
     * The config dialog stores the flee-edge dropdown even when fleeing is off, so an edge alone is a
     * leftover setting, not a mission - the engine's own MoveToDestination condition requires both. A
     * force carrying such a leftover must still read the battle and stand on the defensive. Found in a
     * live game: a defending company on AUTO could never resolve to DEFEND because its saved config
     * carried a flee edge with fleeing off.
     */
    @Test
    void aDestinationEdgeWithoutAFleeOrderIsALeftoverSettingNotAMission() {
        settings.setDestinationEdge(CardinalEdge.NORTH);
        assertEquals(CombatPosture.ATTACK, resolver.resolve(settings, 1, ownPositions, enemyAtDistance(20)));
        assertEquals(CombatPosture.DEFEND, resolver.resolve(settings, 2, ownPositions, enemyAtDistance(17)),
              "the closing enemy flips the force to the defensive despite the stale edge setting");
    }

    @Test
    void aClosingEnemyFlipsTheForceToTheDefensive() {
        assertEquals(CombatPosture.ATTACK, resolver.resolve(settings, 1, ownPositions, enemyAtDistance(20)),
              "the first reading has nothing to compare against");
        assertEquals(CombatPosture.DEFEND, resolver.resolve(settings, 2, ownPositions, enemyAtDistance(17)),
              "three hexes closer in a round is an advance");
    }

    @Test
    void anEnemyHoldingItsDistanceLeavesTheForceAttacking() {
        assertEquals(CombatPosture.ATTACK, resolver.resolve(settings, 1, ownPositions, enemyAtDistance(20)));
        assertEquals(CombatPosture.ATTACK, resolver.resolve(settings, 2, ownPositions, enemyAtDistance(20)),
              "an enemy that is not coming to us will not be waited for");
        assertEquals(CombatPosture.ATTACK, resolver.resolve(settings, 3, ownPositions, enemyAtDistance(21)),
              "a retreating enemy even less so");
    }

    /**
     * Entering and leaving the defensive are different decisions. Measured on a 30-game river run, a single
     * threshold left the closing rate hovering around it during a mutual approach and the posture flipped
     * round to round. Once standing on the defensive, the force holds it until the advance actually stops.
     */
    @Test
    void aDefenseHoldsUntilTheAdvanceActuallyStops() {
        assertEquals(CombatPosture.ATTACK, resolver.resolve(settings, 1, ownPositions, enemyAtDistance(20)));
        assertEquals(CombatPosture.DEFEND, resolver.resolve(settings, 2, ownPositions, enemyAtDistance(17)),
              "three hexes closer in a round stands the force on the defensive");
        assertEquals(CombatPosture.DEFEND,
              resolver.resolve(settings, 3, ownPositions, enemiesAtDistances(16, 17)));
        assertEquals(CombatPosture.DEFEND,
              resolver.resolve(settings, 4, ownPositions, enemiesAtDistances(16, 17)));
        // Closing rate is now 0.33 hexes a round - under the entry threshold, so a single-threshold
        // resolver would flip back to attack here mid-assault. The defense holds.
        assertEquals(CombatPosture.DEFEND, resolver.resolve(settings, 5, ownPositions, enemyAtDistance(16)),
              "an advance that has slackened but not stopped does not end the defense");
        assertEquals(CombatPosture.DEFEND, resolver.resolve(settings, 6, ownPositions, enemyAtDistance(16)));
        assertEquals(CombatPosture.DEFEND, resolver.resolve(settings, 7, ownPositions, enemyAtDistance(16)));
        assertEquals(CombatPosture.ATTACK, resolver.resolve(settings, 8, ownPositions, enemyAtDistance(16)),
              "three rounds at the same distance: the advance has stopped, go and get them");
    }

    @Test
    void noVisibleEnemyMeansGoFindThem() {
        assertEquals(CombatPosture.ATTACK, resolver.resolve(settings, 1, ownPositions, List.of()));
    }

    @Test
    void theCallIsCachedForTheRound() {
        resolver.resolve(settings, 1, ownPositions, enemyAtDistance(20));
        CombatPosture secondCallSameRound = resolver.resolve(settings, 1, ownPositions, enemyAtDistance(2));
        assertEquals(CombatPosture.ATTACK, secondCallSameRound,
              "a second resolve in the same round returns the round's answer, whatever the positions say now");
    }

    @Test
    void parseFallsBackToAuto() {
        assertEquals(CombatPosture.DEFEND, CombatPosture.parse("defend"));
        assertEquals(CombatPosture.ATTACK, CombatPosture.parse(" Attack "));
        assertEquals(CombatPosture.AUTO, CombatPosture.parse("hold"));
        assertEquals(CombatPosture.AUTO, CombatPosture.parse(null));
    }
}
