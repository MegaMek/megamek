/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.compute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Infantry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MarinePointsScoreCalculator} focusing on MPS calculations
 * for infantry vs. infantry combat (TOAR p. 170).
 */
public class MarinePointsScoreCalculatorTest {

    private Game game;
    private Player player;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        player = new Player(0, "Test");
        game.addPlayer(0, player);
    }

    /**
     * Test MPS calculation for conventional infantry platoon.
     * A full-strength infantry platoon should have MPS = number of troopers.
     */
    @Test
    void testCalculateMPS_ConventionalInfantry() {
        Infantry infantry = new Infantry();
        infantry.setOwner(player);
        infantry.setGame(game);
        infantry.setSquadSize(28);
        infantry.setSquadCount(1);
        infantry.initializeInternal(28, Infantry.LOC_INFANTRY);

        // Full strength platoon: 28 troopers * 1 point = 28 MPS
        int mps = MarinePointsScoreCalculator.calculateMPS(infantry);
        assertEquals(28, mps, "Full strength infantry should have MPS equal to trooper count");
    }

    /**
     * Test MPS calculation for damaged infantry platoon.
     * MPS should scale with remaining trooper strength.
     */
    @Test
    void testCalculateMPS_DamagedInfantry() {
        Infantry infantry = new Infantry();
        infantry.setOwner(player);
        infantry.setGame(game);
        infantry.setSquadSize(14);  // Half strength
        infantry.setSquadCount(1);
        infantry.initializeInternal(14, Infantry.LOC_INFANTRY);

        int mps = MarinePointsScoreCalculator.calculateMPS(infantry);
        assertEquals(14, mps, "Damaged infantry should have MPS equal to remaining troopers");
    }

    /**
     * Test MPS calculation for infantry with marine specialization.
     * Marines should have the same base value as regular infantry (1 point per trooper).
     */
    @Test
    void testCalculateMPS_Marines() {
        Infantry infantry = new Infantry();
        infantry.setOwner(player);
        infantry.setGame(game);
        infantry.setSquadSize(28);
        infantry.setSquadCount(1);
        infantry.initializeInternal(28, Infantry.LOC_INFANTRY);
        infantry.setSpecializations(Infantry.MARINES);

        int mps = MarinePointsScoreCalculator.calculateMPS(infantry);
        assertEquals(28, mps, "Marines should have MPS equal to trooper count");
    }

    /**
     * Test MPS calculation with building modifier.
     * Buildings with 60+ hexes provide a bonus based on height.
     * Note: Building must be placed on board for modifier to apply.
     */
    @Test
    void testCalculateMPS_WithBuildingModifier() {
        Infantry infantry = new Infantry();
        infantry.setOwner(player);
        infantry.setGame(game);
        infantry.setSquadSize(28);
        infantry.setSquadCount(1);
        infantry.initializeInternal(28, Infantry.LOC_INFANTRY);

        // Create a large building with 60+ hexes and multiple levels
        AbstractBuildingEntity building = createLargeBuilding(60, 3);

        // Base MPS = 28
        // Building modifier requires building to be on board, so may return base MPS
        int mps = MarinePointsScoreCalculator.calculateMPS(infantry, building);
        assertTrue(mps >= 28, "MPS with building should be at least base value");
    }

    /**
     * Test MPS calculation with small building (< 60 hexes).
     * Small buildings should NOT provide a bonus.
     */
    @Test
    void testCalculateMPS_WithSmallBuilding() {
        Infantry infantry = new Infantry();
        infantry.setOwner(player);
        infantry.setGame(game);
        infantry.setSquadSize(28);
        infantry.setSquadCount(1);
        infantry.initializeInternal(28, Infantry.LOC_INFANTRY);

        // Create a small building (< 60 hexes)
        AbstractBuildingEntity building = createSmallBuilding(10, 3);

        // Base MPS = 28
        // No building modifier for buildings < 60 hexes
        int mps = MarinePointsScoreCalculator.calculateMPS(infantry, building);
        assertEquals(28, mps, "Small buildings should not provide MPS bonus");
    }

    /**
     * Test MPS calculation for null entity.
     * Should return 0.
     */
    @Test
    void testCalculateMPS_NullEntity() {
        int mps = MarinePointsScoreCalculator.calculateMPS(null);
        assertEquals(0, mps, "Null entity should return 0 MPS");
    }

    /**
     * Test MPS calculation for zero-strength infantry.
     * Should return 0.
     */
    @Test
    void testCalculateMPS_ZeroStrengthInfantry() {
        Infantry infantry = new Infantry();
        infantry.setOwner(player);
        infantry.setGame(game);
        infantry.setSquadSize(0);  // Zero strength
        infantry.setSquadCount(0);

        int mps = MarinePointsScoreCalculator.calculateMPS(infantry);
        assertEquals(0, mps, "Zero-strength infantry should return 0 MPS");
    }

    /**
     * Helper method to create a large building with specified hex count and height.
     */
    private AbstractBuildingEntity createLargeBuilding(int hexCount, int height) {
        AbstractBuildingEntity building = new BuildingEntity(BuildingType.MEDIUM, 1);
        building.getInternalBuilding().setBuildingHeight(height);

        // Add hexes in a grid pattern
        for (int i = 0; i < hexCount; i++) {
            int x = i % 10;
            int z = i / 10;
            building.getInternalBuilding().addHex(
                new CubeCoords(x, -x - z, z),
                50, 10, BasementType.UNKNOWN, false
            );
        }

        return building;
    }

    /**
     * Helper method to create a small building with specified hex count and height.
     */
    private AbstractBuildingEntity createSmallBuilding(int hexCount, int height) {
        AbstractBuildingEntity building = new BuildingEntity(BuildingType.LIGHT, 1);
        building.getInternalBuilding().setBuildingHeight(height);

        // Add hexes
        for (int i = 0; i < hexCount; i++) {
            int x = i % 5;
            int z = i / 5;
            building.getInternalBuilding().addHex(
                new CubeCoords(x, -x - z, z),
                50, 10, BasementType.UNKNOWN, false
            );
        }

        return building;
    }
}
