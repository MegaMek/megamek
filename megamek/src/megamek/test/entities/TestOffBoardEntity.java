/*
  Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.test.entities;

import megamek.common.BipedMek;
import megamek.common.Board;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.OffBoardDirection;

public class TestOffBoardEntity {
    public static void main(String... args) {
        // Give the game a blank map.
        Game game = new Game();
        game.setBoardDirect(new Board(16, 17));

        // Now create an entity in the game.
        Entity entity = new BipedMek();
        entity.setGame(game);
        entity.setDeployRound(1);

        // Deploy the entity 30 hexes north of
        // the board and check it's position.
        entity.setOffBoard(30, OffBoardDirection.NORTH);
        entity.deployOffBoard(1);
        Coords north = new Coords(8, -30);
        testCoords(north, entity.getPosition());

        // Deploy the entity 45 hexes south of
        // the board and check it's position.
        entity.setOffBoard(45, OffBoardDirection.SOUTH);
        entity.deployOffBoard(1);
        Coords south = new Coords(8, 62);
        testCoords(south, entity.getPosition());

        // Deploy the entity 105 hexes east of
        // the board and check it's position.
        entity.setOffBoard(105, OffBoardDirection.EAST);
        entity.deployOffBoard(1);
        Coords east = new Coords(121, 9);
        testCoords(east, entity.getPosition());

        // Deploy the entity 3200 hexes west of
        // the board and check it's position.
        entity.setOffBoard(3200, OffBoardDirection.WEST);
        entity.deployOffBoard(1);
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
