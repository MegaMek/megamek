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

package megamek.client.ui.clientGUI;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import megamek.client.Client;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.game.Game;
import megamek.common.units.BuildingDesign;
import megamek.common.units.BuildingEntity;
import megamek.common.units.IBuilding;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.Test;

class BuildingDoorsMenuTest {
    @Test
    void linkedOpeningOffersOneActionAndSharesItsDisabledState() {
        var building = new BuildingEntity(BuildingType.MEDIUM, IBuilding.STANDARD);
        building.configureConstruction(BuildingType.MEDIUM, IBuilding.STANDARD, 2, 40, 0, List.of(CubeCoords.ZERO));
        building.getDesign().getDoors().addAll(List.of(
              new BuildingDesign.Door(new BuildingDesign.Position(CubeCoords.ZERO, 0), 0, 2, 1),
              new BuildingDesign.Door(new BuildingDesign.Position(CubeCoords.ZERO, 0), 1, 2, 1),
              new BuildingDesign.Door(new BuildingDesign.Position(CubeCoords.ZERO, 0), 3, 1)));
        var game = new Game();
        game.setBoard(BoardLoader.initializeBoard("size 10 10\nend\n"));
        var owner = new Player(0, "Owner");
        game.addPlayer(0, owner);
        building.setOwner(owner);
        building.setId(1);
        game.addEntity(building);
        var coords = new Coords(4, 4);
        building.setPosition(coords);
        var board = mock(Board.class);
        when(board.getBuildingsAt(coords)).thenReturn(List.of(building));
        var client = mock(Client.class);
        when(client.getLocalPlayer()).thenReturn(owner);
        game.setPhase(GamePhase.END);
        var menu = MapMenu.buildingDoorsMenu(game, board, coords, client);
        assertEquals(2, menu.getItemCount());
        assertTrue(menu.getItem(0).getText().startsWith("Open N/NE large door:"));
        assertTrue(menu.getItem(0).isEnabled());
        menu.getItem(0).doClick();
        verify(client).sendBuildingDoor(1, 0, true);
        assertTrue(building.getBuildingRuntimeState().changeDoor(building, building.getDesign().getDoors().get(1), owner, true));
        menu = MapMenu.buildingDoorsMenu(game, board, coords, client);
        assertTrue(menu.getItem(0).getText().startsWith("Close N/NE large door:"));
        assertFalse(menu.getItem(0).isEnabled());
        assertTrue(menu.getItem(1).isEnabled());
        game.setPhase(GamePhase.MOVEMENT);
        assertEquals(0, MapMenu.buildingDoorsMenu(game, board, coords, client).getItemCount());
    }
}
