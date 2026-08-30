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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.actions.WeaponAttackAction;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Coords;
import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.units.LargeSupportTank;
import megamek.common.units.ProtoMek;
import megamek.common.units.Tank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Partial cover is a Mek rule: only a standing Mek receives it (TW p.102), including from a Level 1 building
 * (TW p.171). Large support vehicles, small craft and DropShips never do even though they are tall, and ProtoMeks
 * never do. These tests pin the rule itself and the short-building check that used to hand cover to any tall unit.
 */
@DisplayName("Partial cover eligibility")
class PartialCoverEligibilityTest extends GameBoardTestCase {

    static {
        initializeBoard("BOARD_LEVEL_ONE_BUILDING", """
              size 1 2
              hex 0101 0 "bldg_elev:1;building:2:8;bldg_cf:50" ""
              hex 0102 0 "" ""
              end""");
        initializeBoard("BOARD_LEVEL_TWO_BUILDING", """
              size 1 2
              hex 0101 0 "bldg_elev:2;building:2:8;bldg_cf:50" ""
              hex 0102 0 "" ""
              end""");
    }

    /** Puts the unit on the ground inside the building in hex 0101. */
    private <T extends Entity> T placeInBuilding(T unit) {
        return placeInBuilding(unit, 0);
    }

    /** Puts the unit inside the building in hex 0101 at the given elevation above the ground. */
    private <T extends Entity> T placeInBuilding(T unit, int elevation) {
        unit.setId(1);
        unit.setWeight(50.0);
        unit.setGame(getGame());
        getGame().addEntity(unit);
        unit.setPosition(new Coords(0, 0));
        unit.setElevation(elevation);
        return unit;
    }

    @Test
    @DisplayName("Only a Mek can receive partial cover")
    void onlyMeksReceivePartialCover() {
        assertTrue(PartialCover.canReceive(new BipedMek()), "a standing Mek receives partial cover");
        assertFalse(PartialCover.canReceive(new ProtoMek()), "ProtoMeks never benefit (TW p.102)");
        assertFalse(PartialCover.canReceive(new Tank()), "vehicles do not receive partial cover");
        assertFalse(PartialCover.canReceive(new LargeSupportTank()),
              "large support vehicles never receive partial cover, however tall (TW p.102)");
        assertFalse(PartialCover.canReceive(new BattleArmor()), "battle armor does not receive cover");
        assertFalse(PartialCover.canReceive(null), "no target, no cover");
    }

    @Test
    @DisplayName("A Mek in a Level 1 building gets short-building cover")
    void mekInLevelOneBuildingHasShortBuildingCover() {
        setBoard("BOARD_LEVEL_ONE_BUILDING");
        BipedMek mek = placeInBuilding(new BipedMek());

        assertTrue(WeaponAttackAction.targetInShortCoverBuilding(mek),
              "a standing Mek in a Level 1 building receives partial cover (TW p.171)");
    }

    @Test
    @DisplayName("A Mek at the bottom of a Level 2 building is fully inside and gets no cover")
    void mekAtGroundLevelOfTallBuildingHasNoCover() {
        setBoard("BOARD_LEVEL_TWO_BUILDING");
        BipedMek mek = placeInBuilding(new BipedMek(), 0);

        assertFalse(WeaponAttackAction.targetInShortCoverBuilding(mek),
              "two levels of building over a standing Mek is no partial cover (TW p.171)");
    }

    @Test
    @DisplayName("A Mek one level below the roof of a Level 2 building gets cover")
    void mekOneLevelBelowRoofHasShortBuildingCover() {
        setBoard("BOARD_LEVEL_TWO_BUILDING");
        BipedMek mek = placeInBuilding(new BipedMek(), 1);

        assertTrue(WeaponAttackAction.targetInShortCoverBuilding(mek),
              "a standing Mek one level below the roof receives partial cover (TW p.171)");
    }

    @Test
    @DisplayName("A large support tank in the same building does not")
    void largeSupportTankInLevelOneBuildingHasNoCover() {
        setBoard("BOARD_LEVEL_ONE_BUILDING");
        LargeSupportTank tank = placeInBuilding(new LargeSupportTank());

        assertFalse(WeaponAttackAction.targetInShortCoverBuilding(tank),
              "a large support vehicle never receives partial cover, however tall (TW p.102)");
    }
}
