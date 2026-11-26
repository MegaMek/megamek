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
package megamek.client.ui.panels.phaseDisplay.lobby.sorters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import megamek.client.Client;
import megamek.common.Player;
import megamek.common.force.Force;
import megamek.common.force.Forces;
import megamek.common.game.Game;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The lobby's Sortable View keeps each formation together whatever the sort, so a lance whose units have scattered ids
 * is still one block of rows.
 */
class FormationGroupingTest {

    private static final int PLAYER_ID = 0;
    private static final Comparator<Entity> BY_ID = Comparator.comparingInt(Entity::getId);
    private static final Comparator<Entity> HEAVIEST_FIRST =
          Comparator.comparingDouble(Entity::getWeight).reversed();

    private Game game;
    private Forces forces;
    private Player player;
    private final List<Entity> entities = new ArrayList<>();

    @BeforeEach
    void setUp() {
        game = new Game();
        player = new Player(PLAYER_ID, "Player");
        player.setTeam(1);
        game.addPlayer(PLAYER_ID, player);
        forces = game.getForces();
    }

    private Force topLevel(String name) {
        return forces.getForce(forces.addTopLevelForce(Force.createToplevelForce(name, player), player));
    }

    private Force subForce(String name, Force parent) {
        return forces.getForce(forces.addSubForce(Force.createSubforce(name, parent), parent));
    }

    private Entity unit(int id, double tons, Force force) {
        Entity entity = mock(Entity.class);
        when(entity.getId()).thenReturn(id);
        when(entity.getOwnerId()).thenReturn(PLAYER_ID);
        when(entity.getWeight()).thenReturn(tons);
        when(entity.getTransportId()).thenReturn(Entity.NONE);
        when(entity.getTractor()).thenReturn(Entity.NONE);
        if (force != null) {
            forces.addEntity(entity, force.getId());
        }
        entities.add(entity);
        return entity;
    }

    private Entity unit(int id, double tons, int battleValue, int walkMP, Force force) {
        Entity entity = unit(id, tons, force);
        when(entity.calculateBattleValue()).thenReturn(battleValue);
        when(entity.getWalkMP()).thenReturn(walkMP);
        return entity;
    }

    private FormationSorter formationSorter(FormationSorter.Metric metric, Sorting sorting) {
        Client client = mock(Client.class);
        when(client.getGame()).thenReturn(game);
        when(client.getLocalPlayer()).thenReturn(player);
        return new FormationSorter(client, metric, sorting);
    }

    private List<Integer> sortedIds(Comparator<Entity> baseSorter, boolean followTreeOrder) {
        FormationGrouping grouping = new FormationGrouping(entities, forces, baseSorter, followTreeOrder);
        List<Entity> sorted = new ArrayList<>(entities);
        sorted.sort(grouping.comparator());
        List<Integer> ids = new ArrayList<>();
        for (Entity entity : sorted) {
            ids.add(entity.getId());
        }
        return ids;
    }

    @Test
    void unitsOfALanceStayTogetherWhateverTheirIds() {
        Force alpha = topLevel("Alpha Lance");
        Force bravo = topLevel("Bravo Lance");
        unit(1, 50, alpha);
        unit(2, 50, bravo);
        unit(3, 50, alpha);
        unit(4, 50, bravo);

        // Sorting by id alone would give 1, 2, 3, 4 and split both lances
        assertEquals(List.of(1, 3, 2, 4), sortedIds(BY_ID, true));
    }

    @Test
    void theDefaultSortFollowsTheTopLevelOrder() {
        Force alpha = topLevel("Alpha Lance");
        Force bravo = topLevel("Bravo Lance");
        unit(1, 50, alpha);
        unit(2, 50, bravo);
        unit(3, 50, alpha);
        unit(4, 50, bravo);
        forces.moveUp(bravo);

        assertEquals(List.of(2, 4, 1, 3), sortedIds(BY_ID, true));
    }

    @Test
    void theDefaultSortFollowsTheUnitOrderInsideALance() {
        Force alpha = topLevel("Alpha Lance");
        unit(1, 50, alpha);
        Entity second = unit(2, 50, alpha);
        forces.moveUp(second);

        assertEquals(List.of(2, 1), sortedIds(BY_ID, true));
    }

    @Test
    void anExplicitSortOrdersFormationsAndTheUnitsInside() {
        Force alpha = topLevel("Alpha Lance");
        Force bravo = topLevel("Bravo Lance");
        unit(1, 20, alpha);
        unit(2, 80, bravo);
        unit(3, 100, alpha);
        unit(4, 90, bravo);

        // Alpha holds the heaviest unit so it comes first; inside each lance, heaviest first
        assertEquals(List.of(3, 1, 4, 2), sortedIds(HEAVIEST_FIRST, false));
    }

    @Test
    void lancesStayTogetherInsideTheirCompany() {
        Force company = topLevel("A Company");
        Force alpha = subForce("Alpha Lance", company);
        Force bravo = subForce("Bravo Lance", company);
        unit(1, 20, bravo);
        unit(2, 100, alpha);
        unit(3, 90, bravo);
        unit(4, 30, alpha);

        assertEquals(List.of(2, 4, 3, 1), sortedIds(HEAVIEST_FIRST, false));
        assertEquals(List.of(2, 4, 1, 3), sortedIds(BY_ID, true));
    }

    @Test
    void aFormationsOwnUnitsComeBeforeItsSubFormations() {
        Force company = topLevel("A Company");
        Force alpha = subForce("Alpha Lance", company);
        unit(1, 100, alpha);
        unit(2, 20, company);

        // As in the force tree; the commander's own unit is listed above the lances even when lighter
        assertEquals(List.of(2, 1), sortedIds(HEAVIEST_FIRST, false));
        assertEquals(List.of(2, 1), sortedIds(BY_ID, true));
    }

    @Test
    void unitsOutsideAnyFormationComeAfterTheFormations() {
        Force alpha = topLevel("Alpha Lance");
        unit(1, 50, null);
        unit(2, 50, alpha);

        assertEquals(List.of(2, 1), sortedIds(BY_ID, true));
    }

    @Test
    void aCarriedUnitStaysWithItsCarrierInAnotherFormation() {
        Game carrierGame = mock(Game.class);
        Force transports = topLevel("Transports");
        Force infantry = topLevel("Infantry");
        Entity carrier = unit(1, 50, transports);
        unit(2, 50, transports);
        Entity carried = unit(3, 5, infantry);
        unit(4, 5, infantry);
        when(carried.getGame()).thenReturn(carrierGame);
        when(carried.getTransportId()).thenReturn(1);
        when(carrierGame.getEntity(1)).thenReturn(carrier);

        // The carried platoon is listed in its carrier's block, right after the carrier
        assertEquals(List.of(1, 3, 2, 4), sortedIds(BY_ID, true));
    }

    @Test
    void aC3NetworkAcrossFormationsIsReportedSplit() {
        Force alpha = topLevel("Alpha Lance");
        Force bravo = topLevel("Bravo Lance");
        Entity master = unit(1, 50, alpha);
        Entity slave = unit(2, 50, bravo);
        Entity sameLanceSlave = unit(3, 50, alpha);
        for (Entity entity : List.of(master, slave, sameLanceSlave)) {
            when(entity.hasAnyC3System()).thenReturn(true);
        }
        when(master.getC3NetId()).thenReturn("C3.1");
        when(slave.getC3NetId()).thenReturn("C3.1");
        when(sameLanceSlave.getC3NetId()).thenReturn("C3.1");

        Set<String> split = new FormationGrouping(entities, forces, BY_ID, true).splitC3NetIds();

        assertEquals(Set.of("C3.1"), split);
    }

    @Test
    void aC3NetworkInsideOneFormationIsNotSplit() {
        Force alpha = topLevel("Alpha Lance");
        Entity master = unit(1, 50, alpha);
        Entity slave = unit(2, 50, alpha);
        for (Entity entity : List.of(master, slave)) {
            when(entity.hasAnyC3System()).thenReturn(true);
            when(entity.getC3NetId()).thenReturn("C3.1");
        }

        assertTrue(new FormationGrouping(entities, forces, BY_ID, true).splitC3NetIds().isEmpty());
    }

    @Test
    void theLogDescriptionListsOneEntryPerBlock() {
        Force company = topLevel("A Company");
        Force alpha = subForce("Alpha Lance", company);
        unit(1, 50, alpha);
        unit(2, 50, null);
        unit(3, 50, alpha);
        FormationGrouping grouping = new FormationGrouping(entities, forces, BY_ID, true);
        List<Entity> sorted = new ArrayList<>(entities);
        sorted.sort(grouping.comparator());

        assertEquals("[A Company > Alpha Lance x2, no formation x1]", grouping.blocksDescription(sorted));
    }

    @Test
    void totalBvOrdersWholeFormationsBothWays() {
        Force alpha = topLevel("Alpha Lance");
        Force bravo = topLevel("Bravo Lance");
        unit(1, 50, 1000, 4, alpha);
        unit(2, 50, 1000, 4, alpha);
        unit(3, 50, 3000, 4, bravo);

        // Alpha totals 2,000 and Bravo 3,000, although Alpha has more units
        assertEquals(List.of(3, 1, 2),
              sortedIds(formationSorter(FormationSorter.Metric.TOTAL_BV, Sorting.DESCENDING), false));
        assertEquals(List.of(1, 2, 3),
              sortedIds(formationSorter(FormationSorter.Metric.TOTAL_BV, Sorting.ASCENDING), false));
    }

    @Test
    void totalTonnageOrdersWholeFormationsBothWays() {
        Force alpha = topLevel("Alpha Lance");
        Force bravo = topLevel("Bravo Lance");
        unit(1, 100, 1000, 3, alpha);
        unit(2, 35, 1000, 6, bravo);
        unit(3, 80, 1000, 4, bravo);

        // Bravo totals 115 tons, Alpha 100
        assertEquals(List.of(2, 3, 1),
              sortedIds(formationSorter(FormationSorter.Metric.TOTAL_TONNAGE, Sorting.DESCENDING), false));
        assertEquals(List.of(1, 2, 3),
              sortedIds(formationSorter(FormationSorter.Metric.TOTAL_TONNAGE, Sorting.ASCENDING), false));
    }

    @Test
    void slowestUnitSetsAFormationsSpeed() {
        Force alpha = topLevel("Alpha Lance");
        Force bravo = topLevel("Bravo Lance");
        unit(1, 50, 1000, 6, alpha);
        unit(2, 50, 1000, 3, alpha);
        unit(3, 50, 1000, 4, bravo);
        unit(4, 50, 1000, 4, bravo);

        // Alpha moves at walk 3 because of unit 2, Bravo at walk 4
        assertEquals(List.of(3, 4, 1, 2),
              sortedIds(formationSorter(FormationSorter.Metric.SLOWEST_UNIT, Sorting.DESCENDING), false));
        assertEquals(List.of(1, 2, 3, 4),
              sortedIds(formationSorter(FormationSorter.Metric.SLOWEST_UNIT, Sorting.ASCENDING), false));
    }

    @Test
    void equalFormationsKeepForceViewOrder() {
        Force alpha = topLevel("Alpha Lance");
        Force bravo = topLevel("Bravo Lance");
        unit(1, 50, 1000, 3, alpha);
        unit(2, 50, 1000, 3, bravo);
        forces.moveUp(bravo);

        assertEquals(List.of(2, 1),
              sortedIds(formationSorter(FormationSorter.Metric.SLOWEST_UNIT, Sorting.DESCENDING), false));
    }

    @Test
    void unitsInsideAFormationKeepForceViewOrderUnderAFormationSort() {
        Force alpha = topLevel("Alpha Lance");
        unit(1, 20, 500, 6, alpha);
        unit(2, 100, 2500, 3, alpha);

        // Unit 2 is heavier and stronger, but formation sorts never reorder units inside a formation
        assertEquals(List.of(1, 2),
              sortedIds(formationSorter(FormationSorter.Metric.TOTAL_TONNAGE, Sorting.DESCENDING), false));
    }

    @Test
    void lancesAreOrderedByTheMetricInsideTheirCompany() {
        Force company = topLevel("A Company");
        Force alpha = subForce("Alpha Lance", company);
        Force bravo = subForce("Bravo Lance", company);
        unit(1, 50, 1000, 4, alpha);
        unit(2, 50, 4000, 4, bravo);

        assertEquals(List.of(2, 1),
              sortedIds(formationSorter(FormationSorter.Metric.TOTAL_BV, Sorting.DESCENDING), false));
    }

    @Test
    void forceViewOrderSortFollowsTheTree() {
        Force alpha = topLevel("Alpha Lance");
        Force bravo = topLevel("Bravo Lance");
        unit(1, 50, 9000, 4, alpha);
        unit(2, 50, 1000, 4, bravo);
        forces.moveUp(bravo);

        assertEquals(List.of(2, 1),
              sortedIds(formationSorter(FormationSorter.Metric.TREE_ORDER, Sorting.ASCENDING), false));
    }
}
