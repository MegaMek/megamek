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

import static megamek.client.bot.princess.EmplacementDeployment.MINIMUM_SPACING_HEXES;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import megamek.common.Hex;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies where a TO:AR gun emplacement is built: never in water while dry ground is legal, spaced from the other
 * emplacements where the zone allows, and otherwise on the hex with the widest field of fire.
 */
class EmplacementDeploymentTest {

    private static final Coords OPEN = new Coords(10, 10);
    private static final Coords BOXED_IN = new Coords(14, 10);
    private static final Coords LAKE = new Coords(18, 10);

    private Entity emplacement;
    private Board board;
    private Map<Coords, Hex> hexes;

    @BeforeEach
    void setUp() {
        emplacement = mock(Entity.class);
        when(emplacement.getShortName()).thenReturn("Test Emplacement");
        hexes = new HashMap<>();
        board = mock(Board.class);
        when(board.getHex(any(Coords.class))).thenAnswer(invocation -> hexes.getOrDefault(invocation.getArgument(0),
              new Hex(0)));
        hexes.put(LAKE, new Hex(0, "water:2", ""));
    }

    // ---------------------------------------------------------------- which units

    @Test
    void anAdvancedBuildingOfTheGunEmplacementClassIsAnEmplacement() {
        AbstractBuildingEntity building = mock(AbstractBuildingEntity.class);
        when(building.getBldgClass()).thenReturn(IBuilding.GUN_EMPLACEMENT);
        assertTrue(EmplacementDeployment.isGunEmplacementBuilding(building));
    }

    @Test
    void otherBuildingsAndUnitsAreNot() {
        AbstractBuildingEntity hangar = mock(AbstractBuildingEntity.class);
        when(hangar.getBldgClass()).thenReturn(IBuilding.HANGAR);
        assertFalse(EmplacementDeployment.isGunEmplacementBuilding(hangar));
        assertFalse(EmplacementDeployment.isGunEmplacementBuilding(mock(Entity.class)));
    }

    // ---------------------------------------------------------------- the site

    @Test
    void waterAndMagmaAreUnsuitable() {
        assertTrue(EmplacementDeployment.isUnsuitableSite(new Hex(0, "water:1", "")));
        assertTrue(EmplacementDeployment.isUnsuitableSite(new Hex(0, "water:2;ice:1", "")));
        assertTrue(EmplacementDeployment.isUnsuitableSite(new Hex(0, "magma:1", "")));
        assertTrue(EmplacementDeployment.isUnsuitableSite(null));
    }

    @Test
    void dryGroundIsSuitable() {
        assertFalse(EmplacementDeployment.isUnsuitableSite(new Hex(0)));
        assertFalse(EmplacementDeployment.isUnsuitableSite(new Hex(2, "rough:1", "")));
        assertFalse(EmplacementDeployment.isUnsuitableSite(new Hex(0, "water:0", "")),
              "depth 0 water is only a puddle");
    }

    @Test
    void neverBuildsInWaterWhileDryGroundIsLegal() {
        Coords chosen = EmplacementDeployment.choose(emplacement, List.of(LAKE, BOXED_IN), board, List.of(),
              c -> c.equals(LAKE) ? 36 : 2);
        assertEquals(BOXED_IN, chosen, "open water must not win on its field of fire");
    }

    @Test
    void buildsInWaterOnlyWhenNothingElseIsLegal() {
        assertEquals(LAKE, EmplacementDeployment.choose(emplacement, List.of(LAKE), board, List.of(), c -> 0));
    }

    @Test
    void noCandidatesMeansNoHex() {
        assertNull(EmplacementDeployment.choose(emplacement, List.of(), board, List.of(), c -> 0));
    }

    // ---------------------------------------------------------------- field of fire

    @Test
    void theHexThatSeesMoreWins() {
        Coords chosen = EmplacementDeployment.choose(emplacement, List.of(BOXED_IN, OPEN), board, List.of(),
              c -> c.equals(OPEN) ? 30 : 4);
        assertEquals(OPEN, chosen);
    }

    @Test
    void higherGroundBreaksATie() {
        hexes.put(BOXED_IN, new Hex(2));
        Coords chosen = EmplacementDeployment.choose(emplacement, List.of(OPEN, BOXED_IN), board, List.of(),
              c -> 10);
        assertEquals(BOXED_IN, chosen);
    }

    // ---------------------------------------------------------------- spacing

    @Test
    void crowdingIsTheSpacingStolenFromTheNearestEmplacement() {
        List<Coords> other = List.of(new Coords(10, 1));
        assertEquals(MINIMUM_SPACING_HEXES, EmplacementDeployment.crowding(new Coords(10, 1), other));
        assertEquals(MINIMUM_SPACING_HEXES - 1, EmplacementDeployment.crowding(new Coords(11, 1), other));
        assertEquals(0, EmplacementDeployment.crowding(new Coords(10 + MINIMUM_SPACING_HEXES, 1), other));
        assertEquals(0, EmplacementDeployment.crowding(new Coords(10, 1), List.of()));
    }

    @Test
    void aSpacedHexBeatsABetterViewBesideAnotherEmplacement() {
        Coords besideIt = OPEN.translated(0);
        Coords chosen = EmplacementDeployment.choose(emplacement, List.of(besideIt, BOXED_IN), board,
              List.of(OPEN), c -> c.equals(besideIt) ? 30 : 4);
        assertEquals(BOXED_IN, chosen, "two emplacements side by side cover the same ground");
    }

    @Test
    void spacingGivesWayWhenNoHexHasRoom() {
        Coords north = OPEN.translated(0);
        Coords northEast = OPEN.translated(1);
        Coords chosen = EmplacementDeployment.choose(emplacement, List.of(north, northEast), board,
              List.of(OPEN), c -> c.equals(northEast) ? 20 : 5);
        assertEquals(northEast, chosen, "when every hex is equally crowded, the view decides");
    }

    // ---------------------------------------------------------------- sightline samples

    @Test
    void samplesLieOnTwelvePointsOfEachRing() {
        Coords origin = new Coords(20, 20);
        List<Coords> samples = EmplacementDeployment.sightlineSamples(origin, 12);

        Set<Integer> distances = samples.stream().map(origin::distance).collect(Collectors.toSet());
        assertEquals(Set.of(3, 6, 12), distances);
        for (int ring : distances) {
            assertEquals(12, samples.stream().filter(s -> origin.distance(s) == ring).distinct().count(),
                  "ring " + ring);
        }
    }

    @Test
    void aShortReachDoesNotRepeatRings() {
        Coords origin = new Coords(20, 20);
        List<Coords> samples = EmplacementDeployment.sightlineSamples(origin, 3);
        assertEquals(Set.of(3), samples.stream().map(origin::distance).collect(Collectors.toSet()));
        assertEquals(12, samples.size());
    }

    @Test
    void anUnarmedEmplacementLooksTheDefaultDistance() {
        assertEquals(EmplacementDeployment.DEFAULT_REACH, EmplacementDeployment.reach(emplacement));
    }
}
