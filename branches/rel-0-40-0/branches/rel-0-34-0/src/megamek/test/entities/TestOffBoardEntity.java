/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */

package megamek.test.entities;

import megamek.common.BipedMech;
import megamek.common.Board;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.IOffBoardDirections;

public class TestOffBoardEntity {

    public static void main(String[] args) {
        // Give the game a blank map.
        Game game = new Game();
        game.board = new Board(16, 17);

        // Now create an entity in the game.
        Entity entity = new BipedMech();
        entity.setGame(game);

        // Deploy the entity 30 hexes north of
        // the board and check it's position.
        entity.setOffBoard(30, IOffBoardDirections.NORTH);
        entity.deployOffBoard();
        Coords north = new Coords(8, -30);
        testCoords(north, entity.getPosition());

        // Deploy the entity 45 hexes south of
        // the board and check it's position.
        entity.setOffBoard(45, IOffBoardDirections.SOUTH);
        entity.deployOffBoard();
        Coords south = new Coords(8, 62);
        testCoords(south, entity.getPosition());

        // Deploy the entity 105 hexes east of
        // the board and check it's position.
        entity.setOffBoard(105, IOffBoardDirections.EAST);
        entity.deployOffBoard();
        Coords east = new Coords(121, 9);
        testCoords(east, entity.getPosition());

        // Deploy the entity 3200 hexes west of
        // the board and check it's position.
        entity.setOffBoard(3200, IOffBoardDirections.WEST);
        entity.deployOffBoard();
        Coords west = new Coords(-3200, 9);
        testCoords(west, entity.getPosition());

    }

    public static void testCoords(Coords expected, Coords actual) {
        System.out.print("The entity should be deployed at ");
        System.out.print(expected);
        if (expected.equals(actual)) {
            System.out.println(" and it is.");
        } else {
            System.out.print(" but it is at ");
            System.out.print(actual);
            System.out.println("!");
        }
    }
}
