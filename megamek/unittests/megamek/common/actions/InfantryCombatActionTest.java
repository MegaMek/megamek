/*
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

package megamek.common.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.Player;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingEntity;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InfantryCombatAction} focusing on action validation
 * and interaction with real BuildingEntity targets (TOAR p. 172-174).
 */
public class InfantryCombatActionTest {

    private Game game;
    private Player player1;
    private Player player2;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        player1 = new Player(0, "Player 1");
        player2 = new Player(1, "Player 2");
        game.addPlayer(0, player1);
        game.addPlayer(1, player2);
    }

    /**
     * Test valid infantry combat action against a real BuildingEntity.
     */
    @Test
    void testToHit_ValidAction_RealBuildingEntity() {
        Infantry infantry = createInfantry(player1, new Coords(5, 5));
        AbstractBuildingEntity building = createBuilding(player2, new Coords(5, 5));

        game.addEntity(infantry);
        game.addEntity(building);

        InfantryCombatAction action = new InfantryCombatAction(infantry.getId(), building.getId());
        ToHitData toHit = action.toHit(game);

        assertEquals(TargetRoll.AUTOMATIC_SUCCESS, toHit.getValue(),
            "Infantry in same hex as building should be able to initiate combat");
    }

    /**
     * Test infantry must be in same hex as building.
     */
    @Test
    void testToHit_DifferentHex_RealBuildingEntity() {
        Infantry infantry = createInfantry(player1, new Coords(5, 5));
        AbstractBuildingEntity building = createBuilding(player2, new Coords(6, 6));

        game.addEntity(infantry);
        game.addEntity(building);

        InfantryCombatAction action = new InfantryCombatAction(infantry.getId(), building.getId());
        ToHitData toHit = action.toHit(game);

        assertEquals(TargetRoll.IMPOSSIBLE, toHit.getValue(),
            "Infantry not in same hex as building should not be able to initiate combat");
        assertTrue(toHit.getDesc().contains("same hex"));
    }

    /**
     * Test infantry already in combat cannot initiate new combat.
     */
    @Test
    void testToHit_AlreadyInCombat_RealBuildingEntity() {
        Infantry infantry = createInfantry(player1, new Coords(5, 5));
        AbstractBuildingEntity building1 = createBuilding(player2, new Coords(5, 5));
        AbstractBuildingEntity building2 = createBuilding(player2, new Coords(5, 5));

        game.addEntity(infantry);
        game.addEntity(building1);
        game.addEntity(building2);

        // Mark infantry as already in combat with building1
        infantry.setInfantryCombatTargetId(building1.getId());
        infantry.setInfantryCombatAttacker(true);

        // Try to initiate combat with building2
        InfantryCombatAction action = new InfantryCombatAction(infantry.getId(), building2.getId());
        ToHitData toHit = action.toHit(game);

        assertEquals(TargetRoll.IMPOSSIBLE, toHit.getValue(),
            "Infantry already in combat should not be able to initiate new combat");
        assertTrue(toHit.getDesc().contains("engaged"));
    }

    /**
     * Test withdrawing from combat as attacker.
     */
    @Test
    void testToHit_WithdrawAsAttacker_RealBuildingEntity() {
        Infantry infantry = createInfantry(player1, new Coords(5, 5));
        AbstractBuildingEntity building = createBuilding(player2, new Coords(5, 5));

        game.addEntity(infantry);
        game.addEntity(building);

        // Mark infantry as in combat as attacker
        infantry.setInfantryCombatTargetId(building.getId());
        infantry.setInfantryCombatAttacker(true);

        InfantryCombatAction action = new InfantryCombatAction(infantry.getId(), building.getId(), true);
        ToHitData toHit = action.toHit(game);

        assertEquals(TargetRoll.AUTOMATIC_SUCCESS, toHit.getValue(),
            "Attacker should be able to withdraw from combat");
        assertTrue(toHit.getDesc().contains("Withdrawing"));
    }

    /**
     * Test defender cannot withdraw from combat.
     */
    @Test
    void testToHit_WithdrawAsDefender_RealBuildingEntity() {
        Infantry infantry = createInfantry(player1, new Coords(5, 5));
        AbstractBuildingEntity building = createBuilding(player2, new Coords(5, 5));

        game.addEntity(infantry);
        game.addEntity(building);

        // Mark infantry as in combat as defender
        infantry.setInfantryCombatTargetId(building.getId());
        infantry.setInfantryCombatAttacker(false);

        InfantryCombatAction action = new InfantryCombatAction(infantry.getId(), building.getId(), true);
        ToHitData toHit = action.toHit(game);

        assertEquals(TargetRoll.IMPOSSIBLE, toHit.getValue(),
            "Defender should not be able to withdraw from combat");
        assertTrue(toHit.getDesc().contains("Only attackers"));
    }

    /**
     * Test action getters and setters.
     */
    @Test
    void testActionProperties() {
        InfantryCombatAction action = new InfantryCombatAction(1, 2);
        assertFalse(action.isWithdrawing());

        action.setWithdrawing(true);
        assertTrue(action.isWithdrawing());

        action.setWithdrawing(false);
        assertFalse(action.isWithdrawing());
    }

    /**
     * Test action summary string.
     */
    @Test
    void testToSummaryString_Initiate() {
        Infantry infantry = createInfantry(player1, new Coords(5, 5));
        AbstractBuildingEntity building = createBuilding(player2, new Coords(5, 5));

        game.addEntity(infantry);
        game.addEntity(building);

        InfantryCombatAction action = new InfantryCombatAction(infantry.getId(), building.getId());
        String summary = action.toSummaryString(game);

        assertTrue(summary.contains("Board") || summary.toLowerCase().contains("initiat"));
    }

    /**
     * Test action summary string for withdrawal.
     */
    @Test
    void testToSummaryString_Withdraw() {
        Infantry infantry = createInfantry(player1, new Coords(5, 5));
        AbstractBuildingEntity building = createBuilding(player2, new Coords(5, 5));

        game.addEntity(infantry);
        game.addEntity(building);

        infantry.setInfantryCombatTargetId(building.getId());
        infantry.setInfantryCombatAttacker(true);

        InfantryCombatAction action = new InfantryCombatAction(infantry.getId(), building.getId(), true);
        String summary = action.toSummaryString(game);

        assertTrue(summary.contains("Withdraw") || summary.toLowerCase().contains("withdraw"));
    }

    /**
     * Test attacker must be infantry.
     */
    @Test
    void testToHit_NonInfantryAttacker() {
        // Create a BipedMek instead of infantry
        Entity mek = new megamek.common.units.BipedMek();
        mek.setOwner(player1);
        mek.setId(0);
        mek.setPosition(new Coords(5, 5));

        AbstractBuildingEntity building = createBuilding(player2, new Coords(5, 5));

        game.addEntity(mek);
        game.addEntity(building);

        InfantryCombatAction action = new InfantryCombatAction(mek.getId(), building.getId());
        ToHitData toHit = action.toHit(game);

        assertEquals(TargetRoll.IMPOSSIBLE, toHit.getValue(),
            "Non-infantry attacker should not be able to initiate boarding combat");
        assertTrue(toHit.getDesc().contains("infantry"));
    }

    // ==================== Helper Methods ====================

    private Infantry createInfantry(Player owner, Coords position) {
        Infantry infantry = new Infantry();
        infantry.setOwner(owner);
        infantry.setGame(game);
        infantry.setPosition(position);
        infantry.setSquadSize(28);
        infantry.setSquadCount(1);
        infantry.initializeInternal(28, Infantry.LOC_INFANTRY);
        return infantry;
    }

    private AbstractBuildingEntity createBuilding(Player owner, Coords position) {
        AbstractBuildingEntity building = new BuildingEntity(BuildingType.MEDIUM, 1);
        building.setOwner(owner);
        building.setPosition(position);
        building.getInternalBuilding().setBuildingHeight(3);
        building.getInternalBuilding().addHex(
            new CubeCoords(0, 0, 0),
            50, 10, BasementType.UNKNOWN, false
        );
        return building;
    }
}
