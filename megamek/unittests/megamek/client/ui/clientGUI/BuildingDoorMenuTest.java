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
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import megamek.client.Client;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BuildingType;
import megamek.common.enums.GamePhase;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.BuildingDesign;
import megamek.common.units.BuildingEntity;
import megamek.common.units.IBuilding;
import megamek.utils.BoardLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BuildingDoorMenuTest {
    private static final Coords COORDS = new Coords(1, 1);

    @BeforeAll static void equipment() { EquipmentType.initializeTypes(); }

    private BuildingEntity add(Game game, Player owner, int id, String name, int classification, int base) {
        var building = new BuildingEntity(BuildingType.MEDIUM, classification);
        building.configureConstruction(BuildingType.MEDIUM, classification, 2, 30, 0, List.of(CubeCoords.ZERO));
        building.setId(id);
        building.setChassis(name);
        building.setModel("Door");
        building.setOwner(owner);
        building.setDeployed(true);
        building.getDesign().setBaseLevel(base);
        if (base < 0) { building.getDesign().setSite(BuildingDesign.Site.UNDERGROUND); }
        building.getDesign().getDoors().add(new BuildingDesign.Door(new BuildingDesign.Position(CubeCoords.ZERO, 0), 0, 1));
        game.addEntity(building);
        building.setPosition(COORDS);
        game.getBoard(0).addBuildingToBoard(building);
        return building;
    }

    private JMenuItem entry(JMenu menu, String name) {
        return java.util.stream.IntStream.range(0, menu.getItemCount()).mapToObj(menu::getItem)
              .filter(item -> item.getText().contains(name)).findFirst().orElseThrow();
    }

    @Test void colocatedSurfaceUndergroundAndWallDoorsKeepTheirOwnControlsAndBuildingIds() {
        var game = new Game();
        game.setBoard(BoardLoader.initializeBoard("size 4 4\nend\n"));
        game.setRoundCount(1);
        game.setPhase(GamePhase.END);
        var aboveOwner = new Player(0, "Above");
        var belowOwner = new Player(1, "Below");
        game.addPlayer(0, aboveOwner);
        game.addPlayer(1, belowOwner);
        var surface = add(game, aboveOwner, 1, "Surface", IBuilding.STANDARD, 0);
        var underground = add(game, belowOwner, 2, "Underground", IBuilding.STANDARD, -2);
        var wall = add(game, aboveOwner, 3, "Wall", IBuilding.WALL, 0);
        var client = mock(Client.class);
        when(client.getLocalPlayer()).thenReturn(aboveOwner);
        JMenu menu = MapMenu.buildingDoorsMenu(game, game.getBoard(0), COORDS, client);
        assertEquals(3, menu.getItemCount());
        assertTrue(entry(menu, "Surface").isEnabled());
        assertTrue(entry(menu, "Wall").isEnabled());
        assertFalse(entry(menu, "Underground").isEnabled());
        assertTrue(entry(menu, "Surface").getText().contains("Level G"));
        assertTrue(entry(menu, "Underground").getText().contains("Level -2"));
        entry(menu, "Surface").doClick();
        entry(menu, "Wall").doClick();
        verify(client).sendBuildingDoor(surface.getId(), 0, true);
        verify(client).sendBuildingDoor(wall.getId(), 0, true);
        verify(client, never()).sendBuildingDoor(underground.getId(), 0, true);
        when(client.getLocalPlayer()).thenReturn(belowOwner);
        menu = MapMenu.buildingDoorsMenu(game, game.getBoard(0), COORDS, client);
        assertTrue(entry(menu, "Underground").isEnabled());
        assertFalse(entry(menu, "Surface").isEnabled());
        entry(menu, "Underground").doClick();
        verify(client).sendBuildingDoor(underground.getId(), 0, true);
        assertFalse(underground.getBuildingRuntimeState().changeDoor(underground,
              underground.getDesign().getDoors().getFirst(), aboveOwner, true));
        assertTrue(underground.getBuildingRuntimeState().changeDoor(underground,
              underground.getDesign().getDoors().getFirst(), belowOwner, true));
        game.setPhase(GamePhase.MOVEMENT);
        assertEquals(0, MapMenu.buildingDoorsMenu(game, game.getBoard(0), COORDS, client).getItemCount());
    }
}
