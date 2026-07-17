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
package megamek.common.units;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.GameBoardTestCase;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.GunEmplacement;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for the unofficial structures-on-elevators game option: single-hex Advanced Buildings and Gun Emplacements
 * may deploy on an industrial elevator hex only when the option is enabled; multi-hex buildings never may.
 */
public class StructuresOnElevatorsDeploymentTest extends GameBoardTestCase {

    private static final Coords ELEVATOR_HEX = new Coords(0, 0);
    private static final Coords PLAIN_HEX = new Coords(1, 0);
    private static final Coords BUILDING_HEX = new Coords(0, 1);

    private Game game;

    static {
        // 0101: industrial elevator (shaft 2 down, 100 tons), 0201: plain, 0102: existing board building
        initializeBoard("BOARD_STRUCTURES_ON_ELEVATORS", """
              size 2 2
              hex 0101 0 "pavement:1;industrial_elevator:-2:10" ""
              hex 0201 0 "" ""
              hex 0102 0 "building:2;bldg_elev:2;bldg_cf:50" ""
              hex 0202 0 "" ""
              end""");
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        initializeBoard("BOARD_STRUCTURES_ON_ELEVATORS", """
              size 2 2
              hex 0101 0 "pavement:1;industrial_elevator:-2:10" ""
              hex 0201 0 "" ""
              hex 0102 0 "building:2;bldg_elev:2;bldg_cf:50" ""
              hex 0202 0 "" ""
              end""");
        Board board = getBoard("BOARD_STRUCTURES_ON_ELEVATORS");
        game.setBoard(0, board);
        game.addPlayer(0, new Player(0, "Test Player"));
    }

    private void setStructuresOnElevatorsOption(boolean enabled) {
        game.getOptions().getOption(OptionsConstants.UNOFFICIAL_STRUCTURES_ON_ELEVATORS).setValue(enabled);
    }

    private GunEmplacement buildGunEmplacement() {
        GunEmplacement gunEmplacement = new GunEmplacement();
        gunEmplacement.setOwner(game.getPlayer(0));
        gunEmplacement.setId(1);
        game.addEntity(gunEmplacement);
        return gunEmplacement;
    }

    private BuildingEntity buildBuildingEntity(int hexCount) {
        BuildingEntity buildingEntity = new BuildingEntity(BuildingType.MEDIUM, IBuilding.STANDARD);
        buildingEntity.setOwner(game.getPlayer(0));
        buildingEntity.getInternalBuilding().setBuildingHeight(2);
        buildingEntity.getInternalBuilding().addHex(new CubeCoords(0, 0, 0), 40, 0, BasementType.NONE, false);
        if (hexCount > 1) {
            buildingEntity.getInternalBuilding().addHex(new CubeCoords(1, -1, 0), 40, 0, BasementType.NONE, false);
        }
        buildingEntity.refreshLocations();
        buildingEntity.refreshAdditionalLocations();
        buildingEntity.setId(2);
        game.addEntity(buildingEntity);
        return buildingEntity;
    }

    // --- Gun Emplacements ---

    @Test
    void gunEmplacementProhibitedOnElevatorByDefault() {
        GunEmplacement gunEmplacement = buildGunEmplacement();

        assertTrue(gunEmplacement.isLocationProhibited(ELEVATOR_HEX, 0, 0),
              "Without the option, a Gun Emplacement must not deploy on an elevator hex");
    }

    @Test
    void gunEmplacementAllowedOnElevatorWithOption() {
        setStructuresOnElevatorsOption(true);
        GunEmplacement gunEmplacement = buildGunEmplacement();

        assertFalse(gunEmplacement.isLocationProhibited(ELEVATOR_HEX, 0, 0),
              "With the option, a Gun Emplacement may deploy on an elevator hex");
    }

    @Test
    void gunEmplacementStillRequiresBuildingElsewhere() {
        setStructuresOnElevatorsOption(true);
        GunEmplacement gunEmplacement = buildGunEmplacement();

        assertFalse(gunEmplacement.isLocationProhibited(BUILDING_HEX, 0, 0),
              "A Gun Emplacement may always deploy on a building hex");
        assertTrue(gunEmplacement.isLocationProhibited(PLAIN_HEX, 0, 0),
              "The option must not let a Gun Emplacement deploy on a plain hex");
    }

    // --- Advanced Buildings ---

    @Test
    void singleHexBuildingProhibitedOnElevatorByDefault() {
        BuildingEntity buildingEntity = buildBuildingEntity(1);

        assertTrue(buildingEntity.isLocationProhibited(ELEVATOR_HEX, 0, 0),
              "Without the option, an Advanced Building must not deploy on an elevator hex");
    }

    @Test
    void singleHexBuildingAllowedOnElevatorWithOption() {
        setStructuresOnElevatorsOption(true);
        BuildingEntity buildingEntity = buildBuildingEntity(1);

        assertFalse(buildingEntity.isLocationProhibited(ELEVATOR_HEX, 0, 0),
              "With the option, a single-hex Advanced Building may deploy on an elevator hex");
    }

    @Test
    void multiHexBuildingProhibitedOnElevatorEvenWithOption() {
        setStructuresOnElevatorsOption(true);
        BuildingEntity buildingEntity = buildBuildingEntity(2);

        assertTrue(buildingEntity.isLocationProhibited(ELEVATOR_HEX, 0, 0),
              "A multi-hex Advanced Building must never overlap an elevator hex, even with the option");
    }

    @Test
    void singleHexBuildingUnaffectedAwayFromElevator() {
        BuildingEntity buildingEntity = buildBuildingEntity(1);

        assertFalse(buildingEntity.isLocationProhibited(PLAIN_HEX, 0, 0),
              "A plain hex must stay deployable for buildings regardless of the option");
    }
}
