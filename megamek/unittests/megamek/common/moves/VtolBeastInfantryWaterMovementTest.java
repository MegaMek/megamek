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
package megamek.common.moves;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.board.Coords;
import megamek.common.enums.MoveStepType;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.InfantryMount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Water movement tests for VTOL beast-mounted infantry (e.g. Branth, TO:AUE p.106). A flying mount above the surface
 * is not in the water, so depth-1+ water hexes must not be prohibited terrain for it while airborne; its maximum
 * water depth only binds it at or below the surface (TW p.54 VTOL movement). Regression tests for MegaMek issue
 * #8707, where Branth platoons over deep water were unable to move at all and could not path across water.
 */
public class VtolBeastInfantryWaterMovementTest extends GameBoardTestCase {

    static {
        // Land - depth 1 water - land, in one column; units start at 0101 facing south
        initializeBoard("BOARD_1x3_WATER_CROSSING", """
              size 1 3
              hex 0101 0 "" ""
              hex 0102 0 "water:1" ""
              hex 0103 0 "" ""
              end""");

        // All water, in one column: depth 1 then depth 2
        initializeBoard("BOARD_1x2_ALL_WATER", """
              size 1 2
              hex 0101 0 "water:1" ""
              hex 0102 0 "water:2" ""
              end""");
    }

    /**
     * Creates a Branth-mounted platoon: VTOL movement mode, 6 VTOL MP, maximum water depth 0.
     */
    private ConvInfantry createBranthInfantry() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setId(7);
        infantry.setSquadSize(5);
        infantry.setSquadCount(4);
        infantry.autoSetInternal();
        infantry.setMount(InfantryMount.BRANTH);

        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        crew.setGunnery(4, crew.getCrewType().getGunnerPos());
        crew.setPiloting(5, crew.getCrewType().getPilotPos());
        crew.setName("Branth Test Crew", 0);
        infantry.setCrew(crew);

        return infantry;
    }

    /**
     * Creates a Camel-mounted platoon: ground movement mode, maximum water depth 0.
     */
    private ConvInfantry createCamelInfantry() {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setId(8);
        infantry.setSquadSize(5);
        infantry.setSquadCount(4);
        infantry.autoSetInternal();
        infantry.setMount(InfantryMount.CAMEL);

        Crew crew = new Crew(CrewType.INFANTRY_CREW);
        crew.setGunnery(4, crew.getCrewType().getGunnerPos());
        crew.setPiloting(5, crew.getCrewType().getPilotPos());
        crew.setName("Camel Test Crew", 0);
        infantry.setCrew(crew);

        return infantry;
    }

    @Test
    @DisplayName("Deep water is not prohibited terrain for a Branth platoon flying above the surface")
    void deepWaterNotProhibitedWhileAirborne() {
        setBoard("BOARD_1x2_ALL_WATER");
        ConvInfantry branth = createBranthInfantry();
        getGame().addEntity(branth);

        assertFalse(branth.isLocationProhibited(new Coords(0, 0), 0, 2),
              "Branth flying at elevation 2 over depth 1 water should not be prohibited");
        assertFalse(branth.isLocationProhibited(new Coords(0, 1), 0, 1),
              "Branth flying at elevation 1 over depth 2 water should not be prohibited");
    }

    @Test
    @DisplayName("Deep water is still prohibited for a Branth platoon at or below the surface")
    void deepWaterProhibitedAtOrBelowSurface() {
        setBoard("BOARD_1x2_ALL_WATER");
        ConvInfantry branth = createBranthInfantry();
        getGame().addEntity(branth);

        assertTrue(branth.isLocationProhibited(new Coords(0, 0), 0, 0),
              "Branth at the surface of depth 1 water should be prohibited (max water depth 0)");
        assertTrue(branth.isLocationProhibited(new Coords(0, 0), 0, -1),
              "Submerged Branth should be prohibited (max water depth 0)");
    }

    @Test
    @DisplayName("Deep water is still prohibited for a ground beast regardless of the fix")
    void deepWaterProhibitedForGroundBeast() {
        setBoard("BOARD_1x2_ALL_WATER");
        ConvInfantry camel = createCamelInfantry();
        getGame().addEntity(camel);

        assertTrue(camel.isLocationProhibited(new Coords(0, 0), 0, 0),
              "Camel in depth 1 water should be prohibited (max water depth 0)");
    }

    @Test
    @DisplayName("Branth platoon can fly across a deep water hex")
    void branthCanFlyAcrossDeepWater() {
        setBoard("BOARD_1x3_WATER_CROSSING");
        ConvInfantry branth = createBranthInfantry();

        // Lift off from land, cross the depth 1 water hex, end over land
        MovePath movePath = getMovePathFor(branth, 0, null,
              MoveStepType.UP, MoveStepType.FORWARDS, MoveStepType.FORWARDS);

        assertTrue(movePath.isMoveLegal(),
              "Branth platoon should be able to fly across a depth 1 water hex (issue #8707)");
    }

    @Test
    @DisplayName("Branth platoon hovering over deep water can move")
    void branthOverDeepWaterCanMove() {
        setBoard("BOARD_1x2_ALL_WATER");
        ConvInfantry branth = createBranthInfantry();

        // Starts airborne over depth 1 water, moves to the next water hex
        MovePath movePath = getMovePathFor(branth, 2, null, MoveStepType.FORWARDS);

        assertTrue(movePath.isMoveLegal(),
              "Branth platoon airborne over deep water should be able to move (issue #8707)");
    }

    @Test
    @DisplayName("Branth platoon still cannot land in deep water")
    void branthCannotLandInDeepWater() {
        setBoard("BOARD_1x2_ALL_WATER");
        ConvInfantry branth = createBranthInfantry();

        // Starts airborne over depth 1 water and tries to descend to the surface
        MovePath movePath = getMovePathFor(branth, 2, null, MoveStepType.DOWN, MoveStepType.DOWN);

        assertFalse(movePath.isMoveLegal(),
              "Branth platoon should not be able to land on deep water (max water depth 0)");
    }
}
