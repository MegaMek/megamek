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
package megamek.client.bot.princess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import megamek.client.bot.princess.FightMemory.OddsRecord;
import megamek.common.InfantryActionDeclaration;
import megamek.common.Player;
import megamek.common.board.Board;
import megamek.common.board.Coords;
import megamek.common.board.CubeCoords;
import megamek.common.enums.BasementType;
import megamek.common.enums.BuildingType;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponMounted;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.units.BuildingEntity;
import megamek.common.units.ConvInfantry;
import megamek.common.units.Entity;
import megamek.common.weapons.lasers.innerSphere.medium.ISLaserMedium;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * What a bot declares for a building: start, reinforce, withdraw, or defend with units and crew.
 */
@DisplayName("Bot infantry action planner")
class InfantryActionPlannerTest {

    private static final Coords HEX = new Coords(5, 5);
    private static final Coords FAR_AWAY = new Coords(1, 1);
    private static final int BRAVEST = 10;
    private static final int MOST_CAUTIOUS = 0;
    /** A bravery that starts a fight at 1.75 to 1 and leaves one below 1.31 to 1. */
    private static final int MIDDLING = 5;

    private Game game;
    private Player bot;
    private Player enemy;
    private BuildingEntity building;
    private int nextId = 1;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() throws Exception {
        game = new Game();
        bot = new Player(0, "Bot");
        enemy = new Player(1, "Enemy");
        bot.setTeam(Player.TEAM_NONE);
        enemy.setTeam(Player.TEAM_NONE);
        game.addPlayer(0, bot);
        game.addPlayer(1, enemy);
        game.setBoard(new Board(16, 17));

        // A one-level, one-hex emplacement with a ten-strong crew; the modifier is 1.0
        building = new BuildingEntity(BuildingType.MEDIUM, 3);
        building.setGame(game);
        building.getInternalBuilding().setBuildingHeight(1);
        building.getInternalBuilding().addHex(CubeCoords.ZERO, 40, 20, BasementType.NONE, false);
        building.refreshLocations();
        building.refreshAdditionalLocations();
        building.addEquipment(new WeaponMounted(building, new ISLaserMedium()), 0, false);
        building.getCrew().setSize(10);
        building.getCrew().setCurrentSize(10);
        building.setId(0);
        game.addEntity(building);
        building.setPosition(HEX);
    }

    private ConvInfantry platoon(Player owner, Coords position) {
        ConvInfantry infantry = new ConvInfantry();
        infantry.setOwner(owner);
        infantry.setGame(game);
        infantry.setSquadSize(28);
        infantry.setSquadCount(1);
        infantry.initializeInternal(28, ConvInfantry.LOC_INFANTRY);
        infantry.setId(nextId++);
        game.addEntity(infantry);
        infantry.setPosition(position);
        return infantry;
    }

    private void engage(ConvInfantry unit, boolean attacker) {
        unit.setInfantryCombatTargetId(building.getId());
        unit.setInfantryCombatAttacker(attacker);
    }

    private static BehaviorSettings behavior(int bravery) {
        BehaviorSettings settings = new BehaviorSettings();
        settings.setBraveryIndex(bravery);
        settings.setHyperAggressionIndex(0);
        return settings;
    }

    @Test
    @DisplayName("Overwhelming odds start an attack with every unit inside; hopeless odds wait")
    void startsWhenTheOddsAreGood() {
        building.setOwner(enemy);
        ConvInfantry first = platoon(bot, HEX);
        ConvInfantry second = platoon(bot, HEX);
        ConvInfantry third = platoon(bot, HEX);

        InfantryActionDeclaration attack = InfantryActionPlanner.planAttack(game, bot, building,
              behavior(MOST_CAUTIOUS), null);

        assertNotNull(attack, "63 points against half of ten crew is an attack even for the most cautious");
        assertEquals(List.of(first.getId(), second.getId(), third.getId()), attack.committedUnitIds());
        assertFalse(attack.withdraw());

        for (int platoons = 0; platoons < 6; platoons++) {
            platoon(enemy, HEX);
        }
        assertNull(InfantryActionPlanner.planAttack(game, bot, building, behavior(BRAVEST), null),
              "one platoon against six waits, however brave");
    }

    @Test
    @DisplayName("A running attack withdraws at collapsed odds and reinforces with newcomers at good ones")
    void withdrawsOrReinforcesARunningAttack() {
        building.setOwner(enemy);
        ConvInfantry attacker = platoon(bot, HEX);
        engage(attacker, true);
        for (int platoons = 0; platoons < 6; platoons++) {
            engage(platoon(enemy, HEX), false);
        }

        InfantryActionDeclaration withdrawal = InfantryActionPlanner.planAttack(game, bot, building,
              behavior(BRAVEST), null);
        assertNotNull(withdrawal);
        assertTrue(withdrawal.withdraw(), "one platoon against six leaves");

        // A fresh building: two platoons already in against one defender, a third just arrived
        game = new Game();
        beforeEachAgain();
        building.setOwner(enemy);
        engage(platoon(bot, HEX), true);
        engage(platoon(bot, HEX), true);
        engage(platoon(enemy, HEX), false);
        ConvInfantry newcomer = platoon(bot, HEX);

        InfantryActionDeclaration reinforcement = InfantryActionPlanner.planAttack(game, bot, building,
              behavior(BRAVEST), null);
        assertNotNull(reinforcement);
        assertEquals(List.of(newcomer.getId()), reinforcement.committedUnitIds(), "three to one reinforces");
        assertFalse(reinforcement.withdraw());
    }

    private void beforeEachAgain() throws RuntimeException {
        try {
            beforeEach();
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    @Test
    @DisplayName("A defender waits for the attack, then commits every unit inside and the crew its bravery allows")
    void defendsWithUnitsAndCrew() {
        building.setOwner(bot);
        ConvInfantry mine = platoon(bot, HEX);
        ConvInfantry attackerOne = platoon(enemy, HEX);
        ConvInfantry attackerTwo = platoon(enemy, HEX);

        assertNull(InfantryActionPlanner.planDefence(game, bot, building, behavior(BRAVEST)),
              "nothing is declared until the attack is");

        engage(attackerOne, true);
        engage(attackerTwo, true);
        building.setInfantryCombatTargetId(building.getId());
        building.setInfantryCombatAttacker(false);

        // 42 against the platoon's 21: even odds would need 42 crew, so the cap decides
        InfantryActionDeclaration cautious = InfantryActionPlanner.planDefence(game, bot, building,
              behavior(MOST_CAUTIOUS));
        assertNotNull(cautious);
        assertEquals(List.of(mine.getId()), cautious.committedUnitIds(), "every unit inside fights");
        assertEquals(5, cautious.committedCrew(), "five of ten is 50 percent and +3; six would be +4");

        InfantryActionDeclaration brave = InfantryActionPlanner.planDefence(game, bot, building, behavior(BRAVEST));
        assertNotNull(brave);
        assertEquals(10, brave.committedCrew(), "the bravest bears +6 and sends everyone");
    }

    @Test
    @DisplayName("The smallest crew count that reaches even odds is the one committed")
    void commitsNoMoreCrewThanEvenOddsNeed() {
        building.setOwner(bot);
        ConvInfantry mine = platoon(bot, HEX);
        // 24 against the platoon's 21: three more points, six crew at 0.5, reach even
        ConvInfantry attacker = platoon(enemy, HEX);
        attacker.setInternal(32, ConvInfantry.LOC_INFANTRY);
        engage(attacker, true);
        building.setInfantryCombatTargetId(building.getId());
        building.setInfantryCombatAttacker(false);

        InfantryActionDeclaration declaration = InfantryActionPlanner.planDefence(game, bot, building,
              behavior(BRAVEST));

        assertNotNull(declaration);
        assertEquals(List.of(mine.getId()), declaration.committedUnitIds());
        assertEquals(6, declaration.committedCrew());
    }

    @Test
    @DisplayName("Under the house rule, a hopeless defence withdraws its infantry; without it, it fights on")
    void hopelessDefenceWithdrawsOnlyUnderTheHouseRule() {
        building.setOwner(bot);
        ConvInfantry mine = platoon(bot, HEX);
        engage(mine, false);
        building.setInfantryCombatTargetId(building.getId());
        building.setInfantryCombatAttacker(false);
        for (int platoons = 0; platoons < 5; platoons++) {
            engage(platoon(enemy, HEX), true);
        }

        InfantryActionDeclaration byTheBook = InfantryActionPlanner.planDefence(game, bot, building,
              behavior(BRAVEST));
        assertNotNull(byTheBook);
        assertFalse(byTheBook.withdraw(), "the book gives no way out; the crew go in");
        assertEquals(10, byTheBook.committedCrew());

        game.getOptions().getOption(OptionsConstants.ADVANCED_COMBAT_INFANTRY_ACTION_DEFENDER_WITHDRAWAL)
              .setValue(true);
        InfantryActionDeclaration houseRule = InfantryActionPlanner.planDefence(game, bot, building,
              behavior(BRAVEST));
        assertNotNull(houseRule);
        assertTrue(houseRule.withdraw(), "105 against 26 at best is hopeless");
    }

    @Test
    @DisplayName("The plan for a turn covers every building the bot has a stake in")
    void planCoversEveryStake() {
        building.setOwner(enemy);
        platoon(bot, HEX);
        platoon(bot, FAR_AWAY);

        List<InfantryActionDeclaration> plan = InfantryActionPlanner.plan(game, bot, behavior(BRAVEST),
              new BotMemory());

        assertEquals(1, plan.size());
        assertEquals(building.getId(), plan.getFirst().buildingId());
    }

    @Test
    @DisplayName("A building already taken is left alone; one with a platoon still inside is not")
    void leavesATakenBuildingAlone() {
        building.setOwner(enemy);
        building.getCrew().setCurrentSize(0);
        platoon(bot, HEX);
        platoon(bot, HEX);
        platoon(bot, HEX);

        assertNull(InfantryActionPlanner.planAttack(game, bot, building, behavior(BRAVEST), null),
              "no crew and nobody inside: there is nothing to attack");

        platoon(enemy, HEX);
        assertNotNull(InfantryActionPlanner.planAttack(game, bot, building, behavior(BRAVEST), null),
              "three platoons against the one still inside is a fight");
    }

    /** Three platoons against two behind a modifier of 1.0 is 1.5 to 1: between the two lines of a middling bot. */
    private void engageThreeAgainstTwo() {
        building.setOwner(enemy);
        engage(platoon(bot, HEX), true);
        engage(platoon(bot, HEX), true);
        engage(platoon(bot, HEX), true);
        engage(platoon(enemy, HEX), false);
        engage(platoon(enemy, HEX), false);
    }

    @Test
    @DisplayName("A fight below the starting bar that has fallen two rounds running is abandoned")
    void withdrawsFromALosingSlide() {
        engageThreeAgainstTwo();
        FightMemory slidingFight = new FightMemory(building.getId());
        slidingFight.rememberOdds(new OddsRecord(1, 84, 42));
        slidingFight.rememberOdds(new OddsRecord(2, 73.5, 42));
        slidingFight.rememberOdds(new OddsRecord(3, 63, 42));

        InfantryActionDeclaration cautious = InfantryActionPlanner.planAttack(game, bot, building,
              behavior(MIDDLING), slidingFight);

        assertNotNull(cautious, "2 to 1 has become 1.5 to 1 over two rounds, below the 1.75 needed to start: leave");
        assertTrue(cautious.withdraw());
    }

    @Test
    @DisplayName("The same odds with no slide behind them, or one bad round, are fought on")
    void fightsOnWithoutASlide() {
        engageThreeAgainstTwo();
        FightMemory steadyFight = new FightMemory(building.getId());
        steadyFight.rememberOdds(new OddsRecord(1, 63, 42));
        steadyFight.rememberOdds(new OddsRecord(2, 63, 42));
        steadyFight.rememberOdds(new OddsRecord(3, 63, 42));
        FightMemory oneBadRound = new FightMemory(building.getId());
        oneBadRound.rememberOdds(new OddsRecord(1, 63, 42));
        oneBadRound.rememberOdds(new OddsRecord(2, 84, 42));
        oneBadRound.rememberOdds(new OddsRecord(3, 63, 42));

        assertNull(InfantryActionPlanner.planAttack(game, bot, building, behavior(MIDDLING),
              steadyFight), "1.5 to 1 that has always been 1.5 to 1 is no reason to leave");
        assertNull(InfantryActionPlanner.planAttack(game, bot, building, behavior(MIDDLING),
              oneBadRound), "one fall is a die roll, not a slide");
        assertNull(InfantryActionPlanner.planAttack(game, bot, building, behavior(MIDDLING),
              null), "and with nothing remembered the odds alone decide");
    }

    @Test
    @DisplayName("Planning notes the odds of each running action once a round and forgets actions that ended")
    void planningKeepsTheFightPageCurrent() {
        engageThreeAgainstTwo();
        BotMemory memory = new BotMemory();

        InfantryActionPlanner.plan(game, bot, behavior(BRAVEST), memory);
        InfantryActionPlanner.plan(game, bot, behavior(BRAVEST), memory);

        FightMemory fight = memory.fight(building.getId());
        assertNotNull(fight);
        assertEquals(1, fight.roundsNoted(), "two looks in one round are one record");
        assertEquals(1.5, fight.latestRecord().odds(), 0.001);

        for (Entity unit : game.getEntitiesVector()) {
            unit.clearInfantryCombatState();
        }
        InfantryActionPlanner.plan(game, bot, behavior(BRAVEST), memory);

        assertNull(memory.fight(building.getId()), "the action is over, so its page is dropped");
    }

    @Test
    @DisplayName("A declaration lists its units lowest id first, whatever order they were found in")
    void declaredIdsAreInAscendingOrder() {
        ConvInfantry later = new ConvInfantry();
        later.setId(20);
        ConvInfantry earlier = new ConvInfantry();
        earlier.setId(10);

        assertEquals(List.of(10, 20), InfantryActionPlanner.ids(List.of(later, earlier)),
              "the server takes the first unit as the lead attacker, so the order must not depend on the entity map");
    }
}
