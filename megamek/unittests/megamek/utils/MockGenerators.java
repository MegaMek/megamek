/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.*;
import megamek.common.options.GameOptions;
import megamek.common.options.PilotOptions;

public class MockGenerators {
	/**
	 * Generates a mock game object. Sets up some values for the passed-in entities
	 * as well (game IDs, and the game object itself)
	 *
	 * @param entities
	 * @return
	 */
	public static Game generateMockGame(List<Entity> entities, Board mockBoard) {

		final Game mockGame = mock(Game.class);

		when(mockGame.getBoard()).thenReturn(mockBoard);
		final GameOptions mockGameOptions = mock(GameOptions.class);
		when(mockGame.getOptions()).thenReturn(mockGameOptions);
		when(mockGameOptions.booleanOption(anyString())).thenReturn(false);

		for (int x = 0; x < entities.size(); x++) {
			when(mockGame.getEntity(x + 1)).thenReturn(entities.get(x));
			when(entities.get(x).getGame()).thenReturn(mockGame);
			when(entities.get(x).getId()).thenReturn(x + 1);
		}

		return mockGame;
	}

	/**
	 * Generates a MockBoard object.
	 *
	 * @return
	 */
	public static Board generateMockBoard() {
		// we'll be on a nice, empty, 20x20 board, not in space.
		final Board mockBoard = mock(Board.class);
		final Hex mockHex = new Hex();
		when(mockBoard.getHex(any(Coords.class))).thenReturn(mockHex);
		when(mockBoard.contains(any(Coords.class))).thenReturn(true);
		when(mockBoard.inSpace()).thenReturn(false);

		return mockBoard;
	}

	/**
	 * Generates an entity at specific coordinates Vital statistics:
	 * - ID: 1 Max
	 * - weapon range: 21 (LRMs, obviously)
	 * - Final path coordinates: (10, 10)
	 * - Final path facing: straight north
	 * - No SPAs
	 * - Default crew
	 *
	 * @param x          X Coord on Board
	 * @param y          Y Coord on Board
	 * @param mockEntity The Entity that we are mocking
	 * @return
	 */

	public static Entity generateMockEntity(int x, int y, Entity mockEntity) {
		when(mockEntity.getMaxWeaponRange()).thenReturn(21);

		final Crew mockCrew = mock(Crew.class);
		when(mockEntity.getCrew()).thenReturn(mockCrew);

		final PilotOptions mockOptions = mock(PilotOptions.class);
		when(mockCrew.getOptions()).thenReturn(mockOptions);
		when(mockOptions.booleanOption(anyString())).thenReturn(false);

		final Coords mockMyCoords = new Coords(x, y);
		when(mockEntity.getPosition()).thenReturn(mockMyCoords);

		when(mockEntity.getHeatCapacity()).thenReturn(20);
		when(mockEntity.getHeat()).thenReturn(0);
		when(mockEntity.isAirborne()).thenReturn(false);

		return mockEntity;
	}

	/**
	 * Generates a BipedMek at specific coordinates Vital statistics:
	 * - No SPAs
	 * - Default crew
	 *
	 * @param x X Coord on Board
	 * @param y Y Coord on Board
	 * @return
	 */

	public static Entity generateMockBipedMek(int x, int y) {
		final Entity mockEntity = mock(BipedMek.class);
		when(mockEntity.getWeight()).thenReturn(50.0);
		return generateMockEntity(x, y, mockEntity);
	}

	/**
	 * Generates an Aerospace Unit at specific coordinates Vital statistics:
	 *
	 * @param x X Coord on Board
	 * @param y Y Coord on Board
	 * @return
	 */
	public static Entity generateMockAerospace(int x, int y) {
		final Entity mockAero = mock(Aero.class);
		when(mockAero.isAero()).thenReturn(true);
		when(mockAero.isAirborne()).thenReturn(true);
		when(mockAero.isDropShip()).thenReturn(false);
		when(mockAero.isAirborneAeroOnGroundMap()).thenReturn(true);
		return generateMockEntity(x, y, mockAero);
	}

	/**
	 * Generates a {@link MockPath} object when passed a given entity and coords to
	 * move to.
	 *
	 * @param x          Where on the x axis to set the initial point.
	 * @param y          Where on the y axis to set the initial point.
	 * @param mockEntity The {@link Entity} to start with
	 * @return
	 */
	public static MovePath generateMockPath(int x, int y, Entity mockEntity) {
		final MovePath mockPath = mock(MovePath.class);
		when(mockPath.getEntity()).thenReturn(mockEntity);
		when(mockPath.clone()).thenReturn(mockPath);

		final Coords mockMyCoords = new Coords(x, y);
		when(mockPath.getFinalCoords()).thenReturn(mockMyCoords);
		when(mockPath.getFinalFacing()).thenReturn(0);

		return mockPath;
	}

	/**
	 * Generates a {@link MockPath} object when passed a given entity and coords to
	 * move to.
	 *
	 * @param coords     The defined {@see Coords} to set the entity to.
	 * @param mockEntity A mocked {@see Entity} to set a path for.
	 * @return
	 */
	public static MovePath generateMockPath(Coords coords, Entity mockEntity) {
		final MovePath mockPath = mock(MovePath.class);
		when(mockPath.getEntity()).thenReturn(mockEntity);

		final Coords mockMyCoords = new Coords(coords.getX(), coords.getY());
		when(mockPath.getFinalCoords()).thenReturn(mockMyCoords);
		when(mockPath.getFinalFacing()).thenReturn(0);

		return mockPath;
	}

	/**
	 * Mock a {@link TargetRoll} with a given value.
	 *
	 * @param value The value to set the {@link TargetRoll} to.
	 * @return A mocked {@link TargetRoll} with the requested value and "mock" as
	 *         description.
	 */
	public static TargetRoll mockTargetRoll(int value) {
		final TargetRoll mockTargetRoll = mock(TargetRoll.class);
		when(mockTargetRoll.getValue()).thenReturn(value);
		when(mockTargetRoll.getDesc()).thenReturn("mock");
		return mockTargetRoll;
	}

	public static Player mockPlayer() {
		Player mockPlayer = mock(Player.class);
		when(mockPlayer.getConstantInitBonus()).thenReturn(0);
		when(mockPlayer.getTurnInitBonus()).thenReturn(0);
		when(mockPlayer.getInitCompensationBonus()).thenReturn(0);
		when(mockPlayer.getCommandBonus()).thenReturn(0);
		return mockPlayer;
	}
}
