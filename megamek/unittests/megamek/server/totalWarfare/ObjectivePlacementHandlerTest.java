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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.ICarryable;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.game.Game;
import megamek.common.options.GameOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ObjectivePlacementHandlerTest {

    private Game game;
    private TWGameManager gameManager;
    private ObjectivePlacementHandler handler;
    private Player alice;
    private Player bob;

    @BeforeEach
    void setUp() {
        game = mock(Game.class);
        when(game.getOptions()).thenReturn(new GameOptions());
        gameManager = mock(TWGameManager.class);
        when(gameManager.getGame()).thenReturn(game);
        handler = new ObjectivePlacementHandler(gameManager);

        alice = new Player(0, "Alice");
        bob = new Player(1, "Bob");
        when(game.getPlayersList()).thenReturn(List.of(alice, bob));
    }

    @Test
    void testDesignatedMarkerIsPlacedAtGameStart() {
        Map<Coords, List<ICarryable>> groundMap = installRealGroundObjectMap();
        boardContainingEverything();
        Coords lobbyPosition = new Coords(4, 4);
        ObjectiveMarker marker = markerFor(alice, lobbyPosition);
        alice.getGroundObjectsToPlace().add(marker);

        handler.placeLobbyObjectives();

        assertTrue(groundMap.get(lobbyPosition).contains(marker));
        assertTrue(alice.getGroundObjectsToPlace().isEmpty());
        assertNull(marker.getLobbyPosition());
        verify(gameManager).sendGroundObjectUpdate();
    }

    @Test
    void testMarkersOfEveryPlayerArePlaced() {
        Map<Coords, List<ICarryable>> groundMap = installRealGroundObjectMap();
        boardContainingEverything();
        ObjectiveMarker aliceMarker = markerFor(alice, new Coords(2, 2));
        ObjectiveMarker bobMarker = markerFor(bob, new Coords(7, 7));
        alice.getGroundObjectsToPlace().add(aliceMarker);
        bob.getGroundObjectsToPlace().add(bobMarker);

        handler.placeLobbyObjectives();

        assertTrue(groundMap.get(new Coords(2, 2)).contains(aliceMarker));
        assertTrue(groundMap.get(new Coords(7, 7)).contains(bobMarker));
    }

    @Test
    void testOffBoardMarkerStaysInTheToPlaceList() {
        installRealGroundObjectMap();
        Board board = mock(Board.class);
        when(board.contains(any(Coords.class))).thenReturn(false);
        when(game.getBoard()).thenReturn(board);
        ObjectiveMarker marker = markerFor(alice, new Coords(500, 500));
        alice.getGroundObjectsToPlace().add(marker);

        handler.placeLobbyObjectives();

        // stays in the to-place list, placeable during the Deploy Minefields phase instead
        assertEquals(1, alice.getGroundObjectsToPlace().size());
        verify(gameManager, never()).sendGroundObjectUpdate();
    }

    @Test
    void testSecondMarkerInTheSameHexStaysUnplaced() {
        Map<Coords, List<ICarryable>> groundMap = installRealGroundObjectMap();
        boardContainingEverything();
        Coords position = new Coords(4, 4);
        ObjectiveMarker existingMarker = markerFor(bob, null);
        groundMap.put(position, new ArrayList<>(List.of(existingMarker)));
        ObjectiveMarker marker = markerFor(alice, position);
        alice.getGroundObjectsToPlace().add(marker);

        handler.placeLobbyObjectives();

        assertEquals(1, alice.getGroundObjectsToPlace().size());
        assertFalse(groundMap.get(position).contains(marker));
    }

    @Test
    void testCarryableWithoutLobbyPositionIsLeftAlone() {
        installRealGroundObjectMap();
        boardContainingEverything();
        // a marker without a designated position and a plain (non-objective) carryable both stay untouched
        ObjectiveMarker undesignatedMarker = markerFor(alice, null);
        ICarryable briefcase = mock(ICarryable.class);
        alice.getGroundObjectsToPlace().add(undesignatedMarker);
        alice.getGroundObjectsToPlace().add(briefcase);

        handler.placeLobbyObjectives();

        assertEquals(2, alice.getGroundObjectsToPlace().size());
        verify(gameManager, never()).sendGroundObjectUpdate();
    }

    @Test
    void testResetReturnsPlacedMarkersToTheirOwners() {
        Map<Coords, List<ICarryable>> groundMap = installRealGroundObjectMap();
        when(game.getPlayer(0)).thenReturn(alice);
        when(game.getPlayer(1)).thenReturn(bob);
        ObjectiveMarker aliceMarker = markerFor(alice, null);
        ObjectiveMarker bobMarker = markerFor(bob, null);
        groundMap.put(new Coords(2, 2), new ArrayList<>(List.of(aliceMarker)));
        groundMap.put(new Coords(7, 7), new ArrayList<>(List.of(bobMarker)));

        handler.returnObjectivesToLobby();

        assertTrue(alice.getGroundObjectsToPlace().contains(aliceMarker));
        assertEquals(new Coords(2, 2), aliceMarker.getLobbyPosition());
        assertTrue(bob.getGroundObjectsToPlace().contains(bobMarker));
        assertEquals(new Coords(7, 7), bobMarker.getLobbyPosition());
    }

    @Test
    void testResetDropsMarkerOfMissingOwner() {
        Map<Coords, List<ICarryable>> groundMap = installRealGroundObjectMap();
        ObjectiveMarker orphanedMarker = new ObjectiveMarker();
        orphanedMarker.setName("Objective");
        orphanedMarker.setOwnerId(99);
        groundMap.put(new Coords(3, 3), new ArrayList<>(List.of(orphanedMarker)));

        handler.returnObjectivesToLobby();

        assertTrue(alice.getGroundObjectsToPlace().isEmpty());
        assertTrue(bob.getGroundObjectsToPlace().isEmpty());
    }

    @Test
    void testResetLeavesPlainCarryablesOnTheGround() {
        Map<Coords, List<ICarryable>> groundMap = installRealGroundObjectMap();
        when(game.getPlayer(0)).thenReturn(alice);
        ICarryable briefcase = mock(ICarryable.class);
        groundMap.put(new Coords(5, 5), new ArrayList<>(List.of(briefcase)));

        handler.returnObjectivesToLobby();

        assertTrue(alice.getGroundObjectsToPlace().isEmpty());
        assertTrue(bob.getGroundObjectsToPlace().isEmpty());
    }

    private ObjectiveMarker markerFor(Player owner, Coords lobbyPosition) {
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName("Objective");
        marker.setOwnerId(owner.getId());
        marker.setLobbyPosition(lobbyPosition);
        return marker;
    }

    private Map<Coords, List<ICarryable>> installRealGroundObjectMap() {
        Map<Coords, List<ICarryable>> groundMap = new HashMap<>();
        when(game.getGroundObjects()).thenReturn(groundMap);
        when(game.getGroundObjects(any(Coords.class))).thenAnswer(invocation ->
              groundMap.getOrDefault(invocation.getArgument(0), new ArrayList<>()));
        doAnswer(invocation -> {
            groundMap.computeIfAbsent(invocation.getArgument(0), key -> new ArrayList<>())
                  .add(invocation.getArgument(1));
            return null;
        }).when(game).placeGroundObject(any(Coords.class), any(ICarryable.class));
        return groundMap;
    }

    private void boardContainingEverything() {
        Board board = mock(Board.class);
        when(board.contains(any(Coords.class))).thenReturn(true);
        when(game.getBoard()).thenReturn(board);
    }
}
