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
package megamek.common;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.BipedMek;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A Mek inside a building is inside on the level it stands on: its extra level of height is not added for line of
 * sight purposes (TW p. 175). A Mek whose head reaches the roof line, such as any Mek in a one-level building, must
 * still share the building with the infantry it fires on. A Mek standing on the roof is outside.
 */
class MekInsideBuildingLineOfSightTest extends GameBoardTestCase {

    private static final Coords ONE_LEVEL_HEX = new Coords(0, 0);
    private static final Coords TWO_LEVEL_HEX = new Coords(1, 0);

    static {
        initializeBoard("MEK_INSIDE_BUILDING_LOS", """
              size 3 3
              hex 0101 0 "building:3;bldg_cf:90;bldg_elev:1" ""
              hex 0201 0 "building:3;bldg_cf:90;bldg_elev:2" ""
              end"""
        );
    }

    private Game game;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        Board board = getBoard("MEK_INSIDE_BUILDING_LOS");
        game = new Game();
        game.setBoard(board);
        game.addPlayer(0, new Player(0, "Test"));
    }

    private <T extends Entity> T place(T entity, Coords position, int elevation) {
        entity.setGame(game);
        entity.setId(game.getNextEntityId());
        entity.setOwner(game.getPlayer(0));
        game.addEntity(entity);
        entity.setDeployed(true);
        entity.setPosition(position);
        entity.setElevation(elevation);
        return entity;
    }

    private LosEffects mekFiresAtPlatoon(Coords hex, int mekLevel, int platoonLevel) {
        BipedMek mek = place(new BipedMek(), hex, mekLevel);
        ConvInfantry platoon = place(new ConvInfantry(), hex, platoonLevel);
        return LosEffects.calculateLOS(game, mek, platoon);
    }

    @Test
    void mekInAOneLevelBuildingSharesItWithThePlatoon() {
        LosEffects los = mekFiresAtPlatoon(ONE_LEVEL_HEX, 0, 0);

        assertNotNull(los.getThruBldg(), "a Mek on the only level of a building is inside it");
    }

    @Test
    void mekOnTheTopLevelOfATwoLevelBuildingIsStillInside() {
        LosEffects los = mekFiresAtPlatoon(TWO_LEVEL_HEX, 1, 0);

        assertNotNull(los.getThruBldg(), "a Mek whose head reaches the roof line is still inside");
    }

    @Test
    void mekBelowTheTopLevelWasAlreadyInside() {
        LosEffects los = mekFiresAtPlatoon(TWO_LEVEL_HEX, 0, 0);

        assertNotNull(los.getThruBldg());
    }

    @Test
    void mekOnTheRoofIsOutside() {
        LosEffects los = mekFiresAtPlatoon(TWO_LEVEL_HEX, 2, 0);

        assertNull(los.getThruBldg(), "the roof is not inside the building (TW p. 175, Rooftops)");
    }
}
