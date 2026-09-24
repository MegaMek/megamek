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
package megamek.server.totalWarfare;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

import megamek.common.Player;
import megamek.common.TechConstants;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.compute.InfantryActionStrengths;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.game.Game;
import megamek.common.net.packets.Packet;
import megamek.common.units.AbstractBuildingEntity;
import megamek.common.units.BuildingEntity;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.server.totalWarfare.InfantryActionTracker.InfantryAction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Issue #8433: infantry vs. infantry casualties come from the <em>other</em> side's strength, are rounded up, and are
 * converted back to troopers by the share of the side's own strength (TO:AR pp. 172-174). Rifle platoons score one
 * Marine Point per trooper, so a 28-man platoon is worth 28 and the arithmetic can be checked by hand.
 */
class InfantryActionResolutionHandlerTest {

    private static final Coords BUILDING_HEX = new Coords(5, 5);
    private static final int PLATOON = 28;

    private Game game;
    private TWGameManager gameManager;
    private InfantryActionTracker tracker;
    private Player attackingPlayer;
    private Player defendingPlayer;
    private AbstractBuildingEntity building;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        game = new Game();
        attackingPlayer = new Player(0, "Attacker");
        defendingPlayer = new Player(1, "Defender");
        attackingPlayer.setTeam(Player.TEAM_NONE);
        defendingPlayer.setTeam(Player.TEAM_NONE);
        game.addPlayer(0, attackingPlayer);
        game.addPlayer(1, defendingPlayer);
        game.setBoard(new Board(16, 17));
        gameManager = Mockito.spy(new TWGameManager());
        Mockito.doNothing().when(gameManager).send(any(Packet.class));
        Mockito.doNothing().when(gameManager).entityUpdate(any(int.class));
        gameManager.setGame(game);
        tracker = new InfantryActionTracker();

        building = new BuildingEntity(BuildingType.MEDIUM, 1);
        building.setOwner(defendingPlayer);
        building.setGame(game);
        building.setPosition(BUILDING_HEX);
        building.getInternalBuilding().setBuildingHeight(3);
        building.getInternalBuilding().addHex(
              new CubeCoords(BUILDING_HEX.getX(), -BUILDING_HEX.getX() - BUILDING_HEX.getY(), BUILDING_HEX.getY()),
              50, 10, BasementType.UNKNOWN, false);
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setId(0);
        game.addEntity(building);
    }

    private ConvInfantry platoon(Player owner) {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setOwner(owner);
        infantry.setGame(game);
        infantry.setPosition(BUILDING_HEX);
        infantry.setSquadSize(PLATOON);
        infantry.setSquadCount(1);
        infantry.initializeInternal(PLATOON, ConvInfantry.LOC_INFANTRY);
        game.addEntity(infantry);
        return infantry;
    }

    private BattleArmor elementalPoint(Player owner) {
        BattleArmor squad = new BattleArmor();
        squad.setOwner(owner);
        squad.setGame(game);
        squad.setPosition(BUILDING_HEX);
        squad.setChassisType(BattleArmor.CHASSIS_TYPE_BIPED);
        squad.setSquadSize(5);
        squad.setWeightClass(EntityWeightClass.WEIGHT_MEDIUM);
        squad.setTechLevel(TechConstants.T_CLAN_TW);
        squad.autoSetInternal();
        for (int trooper = 1; trooper <= 5; trooper++) {
            squad.initializeArmor(10, trooper);
        }
        game.addEntity(squad);
        return squad;
    }

    private InfantryAction startCombat(ConvInfantry firstAttacker, ConvInfantry firstDefender) {
        tracker.addCombat(building.getId(), firstAttacker, firstDefender);
        return tracker.getCombat(building.getId());
    }

    @Test
    void lossesComeFromTheOtherSidesStrengthAndRoundUp() {
        ConvInfantry attackerOne = platoon(attackingPlayer);
        ConvInfantry attackerTwo = platoon(attackingPlayer);
        ConvInfantry defender = platoon(defendingPlayer);
        InfantryAction combat = startCombat(attackerOne, defender);
        tracker.addReinforcement(building.getId(), attackerTwo, true);

        // 56 against 28 is the 2 to 1 column; a roll of 7 reads 40%/50%
        new InfantryActionResolutionHandler(gameManager, tracker).resolve(combat, 7);

        // attackers lose 40% of 28 = 11.2, rounded up to 12 points of their 56: each platoon loses 6 of 28
        assertEquals(PLATOON - 6, attackerOne.getInternal(ConvInfantry.LOC_INFANTRY));
        assertEquals(PLATOON - 6, attackerTwo.getInternal(ConvInfantry.LOC_INFANTRY));
        // defenders in full control lose 50% of 56 = 28, halved to 14 of their 28
        assertEquals(PLATOON - 14, defender.getInternal(ConvInfantry.LOC_INFANTRY));
        assertTrue(tracker.hasCombat(building.getId()), "nobody was eliminated, so the action continues");
    }

    @Test
    void repulsedAttackerLosesDoubleAndCanBeWipedOut() {
        ConvInfantry attacker = platoon(attackingPlayer);
        ConvInfantry defender = platoon(defendingPlayer);
        InfantryAction combat = startCombat(attacker, defender);

        // 1 to 1 column, roll of 2: 75%/25% (R). Attackers lose 28 x .75 x 2 = 42 of their 28
        new InfantryActionResolutionHandler(gameManager, tracker).resolve(combat, 2);

        assertTrue(attacker.isDoomed() || attacker.isDestroyed(),
              "42 casualties against 28 troopers wipes the platoon out");
        // defenders lose 28 x .25 = 7, halved to 3.5, rounded up to 4
        assertEquals(PLATOON - 4, defender.getInternal(ConvInfantry.LOC_INFANTRY));
        assertFalse(tracker.hasCombat(building.getId()));
        assertEquals(Entity.NONE, defender.getInfantryCombatTargetId());
    }

    @Test
    void withdrawingAttackerStillRollsAtHalfDamageAndAnEBecomesP() {
        ConvInfantry attacker = platoon(attackingPlayer);
        ConvInfantry defenderOne = platoon(defendingPlayer);
        ConvInfantry defenderTwo = platoon(defendingPlayer);
        InfantryAction combat = startCombat(attacker, defenderOne);
        tracker.addReinforcement(building.getId(), defenderTwo, false);
        attacker.setInfantryCombatWantsWithdrawal(true);

        // 28 against 56 is the 1 to 2 column; a roll of 5 reads E/15%. The E becomes the column's highest listed
        // attacker percentage, 45%, halved for the withdrawal: 56 x .45 / 2 = 12.6, rounded up to 13 of 28
        new InfantryActionResolutionHandler(gameManager, tracker).resolve(combat, 5);

        assertEquals(PLATOON - 13, attacker.getInternal(ConvInfantry.LOC_INFANTRY), "not eliminated, half damage");
        // defenders lose 28 x .15 = 4.2, halved to 2.1, rounded up to 3, spread as floor(28 x 3/56) = 1 each
        assertEquals(PLATOON - 1, defenderOne.getInternal(ConvInfantry.LOC_INFANTRY));
        assertEquals(PLATOON - 1, defenderTwo.getInternal(ConvInfantry.LOC_INFANTRY));
        assertFalse(tracker.hasCombat(building.getId()), "the action ends with the withdrawal");
        assertEquals(Entity.NONE, attacker.getInfantryCombatTargetId());
        assertFalse(attacker.isInfantryCombatWantsWithdrawal());
    }

    @Test
    void anEResultAgainstTheDefendersWipesThemOut() {
        ConvInfantry attackerOne = platoon(attackingPlayer);
        ConvInfantry attackerTwo = platoon(attackingPlayer);
        ConvInfantry defender = platoon(defendingPlayer);
        InfantryAction combat = startCombat(attackerOne, defender);
        tracker.addReinforcement(building.getId(), attackerTwo, true);

        // 2 to 1 column, roll of 10: 25%/E (P)
        new InfantryActionResolutionHandler(gameManager, tracker).resolve(combat, 10);

        assertTrue(defender.isDoomed() || defender.isDestroyed(), "an E against the defenders destroys the platoon");
        // 28 non-marines score 21; attackers lose 25% of the defenders' 21 = 6 of their 42: floor(28 x 6/42) = 4 each
        assertEquals(PLATOON - 4, attackerOne.getInternal(ConvInfantry.LOC_INFANTRY));
        assertEquals(PLATOON - 4, attackerTwo.getInternal(ConvInfantry.LOC_INFANTRY));
        assertFalse(tracker.hasCombat(building.getId()));
    }

    @Test
    void aGarrisonAtZeroPointsLosesTheBuildingAndItsUncommittedCrewSurrender() {
        building.getCrew().setSize(4);
        building.getCrew().setCurrentSize(4);
        ConvInfantry attackerOne = platoon(attackingPlayer);
        ConvInfantry attackerTwo = platoon(attackingPlayer);
        ConvInfantry defender = platoon(defendingPlayer);
        InfantryAction combat = startCombat(attackerOne, defender);
        tracker.addReinforcement(building.getId(), attackerTwo, true);
        combat.defenderIds.add(building.getId());
        combat.hasPartialControl = true;

        // 2 to 1 column, roll of 9: 30%/60%. The defenders take the full 60% of 42 = 26 against their 21: everything.
        // The building's crew were never committed, so it still stands on the defenders' list scoring nothing.
        new InfantryActionResolutionHandler(gameManager, tracker).resolve(combat, 9);

        assertTrue(defender.isDoomed() || defender.isDestroyed(), "the platoon is wiped out");
        assertEquals(0, building.getCrew().getCurrentSize(), "the uncommitted crew surrender");
        assertTrue(building.getCrew().isDoomed());
        assertFalse(tracker.hasCombat(building.getId()), "the action is over");
    }

    @Test
    @DisplayName("Battle armor losses land a point at a time on each trooper, with no hit-location criticals")
    void battleArmorDamageIsSpreadATrooperAtATime() {
        ConvInfantry attackerOne = platoon(attackingPlayer);
        ConvInfantry attackerTwo = platoon(attackingPlayer);
        BattleArmor elementals = elementalPoint(defendingPlayer);
        InfantryAction combat = new InfantryAction(building.getId());
        tracker.addCombat(building.getId(), attackerOne, elementals);
        combat = tracker.getCombat(building.getId());
        tracker.addReinforcement(building.getId(), attackerTwo, true);
        combat.hasPartialControl = true;

        // 42 against 45 is the 2 to 3 column; roll of 12 is 25%/60% (P). The Elementals lose 60% of 42 = 26 of their
        // 45 at full value: 5 x 26/45 = 2.89, two troopers' worth, 20 points spread four to each of five troopers.
        new InfantryActionResolutionHandler(gameManager, tracker).resolve(combat, 12);

        assertEquals(5, elementals.getNumberActiveTroopers(), "four points each kills nobody in ten-point armor");
        for (int trooper = 1; trooper <= 5; trooper++) {
            assertEquals(6, elementals.getArmor(trooper), "trooper " + trooper);
        }
        assertTrue(tracker.hasCombat(building.getId()), "the point is still in the fight");
    }

    @Test
    @DisplayName("House rule: a withdrawing defence takes half damage, cannot be eliminated, and gives up the building")
    void withdrawingDefendersTakeHalfDamageAndGiveUpTheBuilding() {
        building.getCrew().setSize(4);
        building.getCrew().setCurrentSize(4);
        ConvInfantry attackerOne = platoon(attackingPlayer);
        ConvInfantry attackerTwo = platoon(attackingPlayer);
        ConvInfantry defender = platoon(defendingPlayer);
        InfantryAction combat = startCombat(attackerOne, defender);
        tracker.addReinforcement(building.getId(), attackerTwo, true);
        defender.setInfantryCombatWantsWithdrawal(true);

        // 2 to 1 column, roll of 10: 25%/E (P). The E against the withdrawing defenders becomes the column's highest
        // listed defender percentage, 60% of the attackers' 42 = 26, halved to 13 of their 21: floor(28 x 13/21) = 17
        new InfantryActionResolutionHandler(gameManager, tracker).resolve(combat, 10);

        assertEquals(PLATOON - 17, defender.getInternal(ConvInfantry.LOC_INFANTRY), "half damage, not eliminated");
        assertTrue(defender.isInfantryActionLeaving(), "moved out in the next End Phase");
        assertFalse(defender.isCaptured(), "the leavers do not surrender");
        assertEquals(0, building.getCrew().getCurrentSize(), "the building falls and its crew surrender");
        assertFalse(tracker.hasCombat(building.getId()), "the action is over");
    }

    @Test
    @DisplayName("When the building is captured, the owner's infantry inside that never fought surrender")
    void uncommittedInfantryInsideSurrenderWithTheBuilding() {
        ConvInfantry attackerOne = platoon(attackingPlayer);
        ConvInfantry attackerTwo = platoon(attackingPlayer);
        ConvInfantry defender = platoon(defendingPlayer);
        ConvInfantry heldBack = platoon(defendingPlayer);
        InfantryAction combat = startCombat(attackerOne, defender);
        tracker.addReinforcement(building.getId(), attackerTwo, true);
        // This class's building has its one hex away from the anchor, so its footprint is never laid out on the
        // board; give it an anchor hex and lay it out, so "inside" means something here
        building.getInternalBuilding().addHex(CubeCoords.ZERO, 50, 10, BasementType.UNKNOWN, false);
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.setPosition(BUILDING_HEX);
        assertTrue(InfantryActionStrengths.isInside(heldBack, building), "precondition: the platoon is inside");

        // 2 to 1 column, roll of 10: 25%/E (P), the defenders are eliminated and the building captured
        new InfantryActionResolutionHandler(gameManager, tracker).resolve(combat, 10);

        assertTrue(heldBack.isCaptured(), "the platoon that sat out the fight surrenders");
        assertNull(game.getEntity(heldBack.getId()), "and leaves play");
        assertFalse(attackerOne.isCaptured(), "the attackers inside are not touched");
    }

    @Test
    void partialControlRemovesTheDefendersHalfDamageOnLaterRolls() {
        ConvInfantry attacker = platoon(attackingPlayer);
        ConvInfantry defender = platoon(defendingPlayer);
        InfantryAction combat = startCombat(attacker, defender);
        combat.hasPartialControl = true;

        // 1 to 1 column, roll of 7: 50%/50%. Defenders take the full 14 of 28, attackers 14 of 28
        new InfantryActionResolutionHandler(gameManager, tracker).resolve(combat, 7);

        assertEquals(PLATOON - 14, defender.getInternal(ConvInfantry.LOC_INFANTRY));
        assertEquals(PLATOON - 14, attacker.getInternal(ConvInfantry.LOC_INFANTRY));
    }
}
