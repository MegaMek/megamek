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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import megamek.client.bot.princess.PriorityTargetChoices.Candidate;
import megamek.client.bot.princess.PriorityTargetChoices.ForceRole;
import megamek.common.OffBoardDirection;
import megamek.common.Player;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.game.Game;
import megamek.common.orders.LanceRole;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers what the priority target picker lists (#9174): grouping, the enemies filter, the convoy units the bot always
 * targets, and the search box.
 */
class PriorityTargetChoicesTest {

    private static final int HAMMER_ID = 1;
    private static final int PRINCESS_ID = 2;
    private static final int SUPPLY_COLUMN = 10;
    private static final int ESCORT_LANCE = 11;
    private static final int STRIKE_LANCE = 12;

    private Game game;
    private Forces forces;
    private Player hammer;
    private Player princess;
    private final List<Entity> units = new ArrayList<>();

    @BeforeEach
    void setUp() {
        game = mock(Game.class);
        forces = mock(Forces.class);
        when(game.getForces()).thenReturn(forces);
        hammer = player(HAMMER_ID, "Hammer", 1);
        princess = player(PRINCESS_ID, "Princess", 2);
        force(SUPPLY_COLUMN, "Supply Column");
        force(ESCORT_LANCE, "Escort Lance");
        force(STRIKE_LANCE, "Strike Lance");

        LanceRole convoy = LanceRole.convoy(OffBoardDirection.NORTH);
        LanceRole escort = LanceRole.defaultEscort(SUPPLY_COLUMN);
        unit(3, "Sherpa Armored Truck", "", HAMMER_ID, SUPPLY_COLUMN, convoy);
        unit(4, "Sherpa Armored Truck", "", HAMMER_ID, SUPPLY_COLUMN, convoy);
        unit(9, "Phoenix Hawk", "PXH-3M", HAMMER_ID, ESCORT_LANCE, escort);
        unit(11, "Locust", "LCT-1V", HAMMER_ID, Force.NO_FORCE, null);
        unit(0, "Catapult", "CPLT-C1", PRINCESS_ID, STRIKE_LANCE, null);
        when(game.getEntitiesVector()).thenReturn(units);
    }

    private Player player(int id, String name, int team) {
        Player player = new Player(id, name);
        player.setTeam(team);
        when(game.getPlayer(id)).thenReturn(player);
        return player;
    }

    private void force(int forceId, String name) {
        Force force = mock(Force.class);
        when(force.getName()).thenReturn(name);
        when(forces.getForce(forceId)).thenReturn(force);
    }

    private void unit(int id, String chassis, String model, int ownerId, int forceId, LanceRole role) {
        Entity unit = mock(Entity.class);
        when(unit.getId()).thenReturn(id);
        when(unit.getChassis()).thenReturn(chassis);
        when(unit.getModel()).thenReturn(model);
        when(unit.getOwnerId()).thenReturn(ownerId);
        when(unit.getForceId()).thenReturn(forceId);
        when(unit.getLanceRole()).thenReturn(role);
        units.add(unit);
    }

    private static Candidate find(List<Candidate> candidates, int unitId) {
        return candidates.stream().filter(candidate -> candidate.unitId() == unitId).findFirst().orElseThrow();
    }

    @Test
    void unitsAreGroupedByOwnerThenForceWithUnitsInNoForceLast() {
        List<Integer> order = PriorityTargetChoices.candidatesFor(game, princess).stream()
              .map(Candidate::unitId)
              .toList();

        // Hammer first (lower player ID): Escort Lance and Supply Column by name, then the Locust in no force
        assertEquals(List.of(9, 3, 4, 11, 0), order);
    }

    @Test
    void theBotsOwnUnitsAreNotEnemies() {
        List<Candidate> candidates = PriorityTargetChoices.candidatesFor(game, princess);

        assertTrue(find(candidates, 9).isEnemyOfBot());
        assertFalse(find(candidates, 0).isEnemyOfBot());
    }

    @Test
    void enemyConvoyUnitsAreAlwaysTargetsButEscortsAreNot() {
        List<Candidate> candidates = PriorityTargetChoices.candidatesFor(game, princess);

        assertTrue(find(candidates, 3).isAlwaysTarget());
        assertEquals(ForceRole.CONVOY, find(candidates, 3).forceRole());
        assertFalse(find(candidates, 9).isAlwaysTarget());
        assertEquals(ForceRole.ESCORT, find(candidates, 9).forceRole());
    }

    @Test
    void forABotNotYetInTheGameEveryUnitCountsAsAnEnemyAndNoneIsLocked() {
        List<Candidate> candidates = PriorityTargetChoices.candidatesFor(game, null);

        assertTrue(find(candidates, 0).isEnemyOfBot());
        assertFalse(find(candidates, 3).isAlwaysTarget());
    }

    @Test
    void aBotsOwnConvoyIsNotLocked() {
        princess.setTeam(1);

        assertFalse(find(PriorityTargetChoices.candidatesFor(game, princess), 3).isAlwaysTarget());
    }

    @Test
    void searchMatchesNameOrModelIgnoringCase() {
        Candidate phoenixHawk = find(PriorityTargetChoices.candidatesFor(game, princess), 9);

        assertTrue(PriorityTargetChoices.matchesSearch(phoenixHawk, "phoenix"));
        assertTrue(PriorityTargetChoices.matchesSearch(phoenixHawk, "PXH"));
        assertFalse(PriorityTargetChoices.matchesSearch(phoenixHawk, "sherpa"));
        assertTrue(PriorityTargetChoices.matchesSearch(phoenixHawk, "  "));
    }

    @Test
    void searchMadeOfIdsShowsExactlyThoseUnits() {
        List<Candidate> candidates = PriorityTargetChoices.candidatesFor(game, princess);

        assertTrue(PriorityTargetChoices.matchesSearch(find(candidates, 9), "3-9"));
        assertTrue(PriorityTargetChoices.matchesSearch(find(candidates, 11), "0, 11"));
        assertFalse(PriorityTargetChoices.matchesSearch(find(candidates, 11), "3-9"));
        List<Integer> matchedIds = PriorityTargetChoices.matchingSearch(candidates, "3-9").stream()
              .map(Candidate::unitId)
              .toList();
        assertEquals(List.of(9, 3, 4), matchedIds, "the whole list filtered against one parsed ID list, in order");
    }
    @Test
    void inAThreeWayBattleBothOtherSidesAreTargetsAndTeammatesAreNot() {
        // Team 1 is the player, Team 2 is Princess and a teammate, Team 3 is another bot: Princess shoots at 1 and 3.
        player(3, "Wolves", 3);
        player(4, "Ally", 2);
        unit(30, "Jenner", "JR7-D", 3, Force.NO_FORCE, null);
        unit(40, "Panther", "PNT-9R", 4, Force.NO_FORCE, null);

        List<Integer> targets = PriorityTargetChoices.targetsFor(game, princess).stream()
              .map(Candidate::unitId)
              .toList();

        assertTrue(targets.containsAll(List.of(9, 3, 4, 11, 30)));
        assertFalse(targets.contains(0), "the bot's own unit");
        assertFalse(targets.contains(40), "a teammate's unit");
    }

    @Test
    void aBotNotYetInTheGameCanTargetEveryUnit() {
        assertEquals(units.size(), PriorityTargetChoices.targetsFor(game, null).size());
    }
}
