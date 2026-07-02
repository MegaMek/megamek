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

package megamek.server.trigger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.common.Player;
import megamek.common.board.Coords;
import megamek.common.equipment.ObjectiveMarker;
import megamek.common.game.Game;
import megamek.common.interfaces.IEntityRemovalConditions;
import megamek.common.jacksonAdapters.TriggerDeserializer;
import megamek.common.units.Entity;
import megamek.server.victory.VictoryPointTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the objective-based victory triggers: objective control, destruction, confirmation, capture and the Victory
 * Point threshold, plus their scenario-file parsing.
 */
class ObjectiveTriggersTest {

    private Game game;
    private Player teamOnePlayer;
    private ObjectiveMarker marker;

    @BeforeEach
    void setUp() {
        game = new Game();
        game.setVictoryContext(new HashMap<>());
        teamOnePlayer = new Player(0, "Alice");
        teamOnePlayer.setTeam(1);
        Player teamTwoPlayer = new Player(1, "Bob");
        teamTwoPlayer.setTeam(2);
        game.addPlayer(0, teamOnePlayer);
        game.addPlayer(1, teamTwoPlayer);

        marker = new ObjectiveMarker();
        marker.setName("Relay Station");
        game.placeGroundObject(new Coords(3, 3), marker);
    }

    @Test
    void testObjectiveControlTrigger() {
        ObjectiveControlTrigger aliceControls = new ObjectiveControlTrigger("Relay Station", "Alice");
        ObjectiveControlTrigger bobControls = new ObjectiveControlTrigger("Relay Station", "Bob");
        ObjectiveControlTrigger anyoneControls = new ObjectiveControlTrigger("Relay Station", null);

        assertFalse(aliceControls.isTriggered(game, TriggerSituation.ROUND_END));
        assertFalse(anyoneControls.isTriggered(game, TriggerSituation.ROUND_END));

        marker.setController(1, ObjectiveMarker.NO_CONTROLLER);

        assertTrue(aliceControls.isTriggered(game, TriggerSituation.ROUND_END));
        assertFalse(bobControls.isTriggered(game, TriggerSituation.ROUND_END));
        assertTrue(anyoneControls.isTriggered(game, TriggerSituation.ROUND_END));
    }

    @Test
    void testObjectiveControlTriggerFalseForMissingOrDestroyedObjective() {
        ObjectiveControlTrigger missingObjective = new ObjectiveControlTrigger("No Such Thing", null);
        assertFalse(missingObjective.isTriggered(game, TriggerSituation.ROUND_END));

        marker.setController(1, ObjectiveMarker.NO_CONTROLLER);
        marker.setDestroyed(true);
        ObjectiveControlTrigger destroyedObjective = new ObjectiveControlTrigger("Relay Station", null);
        assertFalse(destroyedObjective.isTriggered(game, TriggerSituation.ROUND_END));
    }

    @Test
    void testObjectiveDestroyedTrigger() {
        ObjectiveDestroyedTrigger trigger = new ObjectiveDestroyedTrigger("Relay Station");

        assertFalse(trigger.isTriggered(game, TriggerSituation.ROUND_END));
        marker.setDestroyed(true);
        assertTrue(trigger.isTriggered(game, TriggerSituation.ROUND_END));
    }

    @Test
    void testObjectiveConfirmedTrigger() {
        marker.setPotential(true);
        ObjectiveConfirmedTrigger trigger = new ObjectiveConfirmedTrigger("Relay Station");

        assertFalse(trigger.isTriggered(game, TriggerSituation.ROUND_END));
        marker.setConfirmed(true);
        assertTrue(trigger.isTriggered(game, TriggerSituation.ROUND_END));
    }

    @Test
    void testObjectiveCapturedTrigger() {
        ObjectiveMarker mobileMarker = new ObjectiveMarker();
        mobileMarker.setName("MacGuffin");
        mobileMarker.setMobile(true);
        Entity fledCarrier = mock(Entity.class);
        when(fledCarrier.getRemovalCondition()).thenReturn(IEntityRemovalConditions.REMOVE_IN_RETREAT);
        when(fledCarrier.getOwner()).thenReturn(teamOnePlayer);
        when(fledCarrier.getDistinctCarriedObjects()).thenReturn(List.of(mobileMarker));
        Game gameWithGraveyard = mock(Game.class);
        when(gameWithGraveyard.getGraveyard()).thenReturn(List.of(fledCarrier));

        ObjectiveCapturedTrigger capturedByAlice = new ObjectiveCapturedTrigger("MacGuffin", "Alice");
        ObjectiveCapturedTrigger capturedByBob = new ObjectiveCapturedTrigger("MacGuffin", "Bob");
        ObjectiveCapturedTrigger capturedByAnyone = new ObjectiveCapturedTrigger("MacGuffin", null);

        assertTrue(capturedByAlice.isTriggered(gameWithGraveyard, TriggerSituation.ROUND_END));
        assertFalse(capturedByBob.isTriggered(gameWithGraveyard, TriggerSituation.ROUND_END));
        assertTrue(capturedByAnyone.isTriggered(gameWithGraveyard, TriggerSituation.ROUND_END));
    }

    @Test
    void testObjectiveCapturedTriggerIgnoresDestroyedCarriers() {
        ObjectiveMarker mobileMarker = new ObjectiveMarker();
        mobileMarker.setName("MacGuffin");
        Entity destroyedCarrier = mock(Entity.class);
        when(destroyedCarrier.getRemovalCondition()).thenReturn(IEntityRemovalConditions.REMOVE_SALVAGEABLE);
        when(destroyedCarrier.getDistinctCarriedObjects()).thenReturn(List.of(mobileMarker));
        Game gameWithGraveyard = mock(Game.class);
        when(gameWithGraveyard.getGraveyard()).thenReturn(List.of(destroyedCarrier));

        ObjectiveCapturedTrigger trigger = new ObjectiveCapturedTrigger("MacGuffin", null);

        assertFalse(trigger.isTriggered(gameWithGraveyard, TriggerSituation.ROUND_END));
    }

    @Test
    void testVictoryPointsTrigger() {
        VictoryPointTracker tracker = VictoryPointTracker.getTracker(game);
        tracker.awardToTeam(1, 3, 2, "controls objectives");

        VictoryPointsTrigger aliceAtThree = new VictoryPointsTrigger("Alice", 3);
        VictoryPointsTrigger aliceAtFive = new VictoryPointsTrigger("Alice", 5);
        VictoryPointsTrigger bobAtThree = new VictoryPointsTrigger("Bob", 3);
        VictoryPointsTrigger anyoneAtThree = new VictoryPointsTrigger(null, 3);

        assertTrue(aliceAtThree.isTriggered(game, TriggerSituation.ROUND_END));
        assertFalse(aliceAtFive.isTriggered(game, TriggerSituation.ROUND_END));
        assertFalse(bobAtThree.isTriggered(game, TriggerSituation.ROUND_END));
        assertTrue(anyoneAtThree.isTriggered(game, TriggerSituation.ROUND_END));
    }

    @Test
    void testVictoryPointsTriggerWithoutTracker() {
        VictoryPointsTrigger trigger = new VictoryPointsTrigger(null, 1);

        assertFalse(trigger.isTriggered(game, TriggerSituation.ROUND_END));
    }

    /** Player.getBV() is computed from game units, so BV-based trigger tests use mocked players. */
    private Player mockSidePlayer(String name, int team, int currentBV, int initialBV) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getTeam()).thenReturn(team);
        when(player.getBV()).thenReturn(currentBV);
        when(player.getInitialBV()).thenReturn(initialBV);
        when(player.isNotObserver()).thenReturn(true);
        return player;
    }

    private Game mockTwoSidedGame(Player alice, Player bob) {
        when(alice.isEnemyOf(bob)).thenReturn(true);
        when(bob.isEnemyOf(alice)).thenReturn(true);
        Game mockedGame = mock(Game.class);
        when(mockedGame.getPlayersList()).thenReturn(List.of(alice, bob));
        return mockedGame;
    }

    @Test
    void testBVDestroyedTrigger() {
        // Alice has lost 60% of her starting BV, so Bob has destroyed 60%; Alice has destroyed only 10%
        Player alice = mockSidePlayer("Alice", 1, 400, 1000);
        Player bob = mockSidePlayer("Bob", 2, 900, 1000);
        Game bvGame = mockTwoSidedGame(alice, bob);

        assertTrue(new BVDestroyedTrigger("Bob", 50).isTriggered(bvGame, TriggerSituation.ROUND_END));
        assertFalse(new BVDestroyedTrigger("Alice", 50).isTriggered(bvGame, TriggerSituation.ROUND_END));
        assertTrue(new BVDestroyedTrigger(null, 50).isTriggered(bvGame, TriggerSituation.ROUND_END));
    }

    @Test
    void testBVRatioTrigger() {
        // Alice has three times Bob's surviving BV
        Player alice = mockSidePlayer("Alice", 1, 900, 1000);
        Player bob = mockSidePlayer("Bob", 2, 300, 1000);
        Game bvGame = mockTwoSidedGame(alice, bob);

        assertTrue(new BVRatioTrigger("Alice", 300).isTriggered(bvGame, TriggerSituation.ROUND_END));
        assertFalse(new BVRatioTrigger("Bob", 300).isTriggered(bvGame, TriggerSituation.ROUND_END));
        assertTrue(new BVRatioTrigger(null, 300).isTriggered(bvGame, TriggerSituation.ROUND_END));
        assertFalse(new BVRatioTrigger("Alice", 400).isTriggered(bvGame, TriggerSituation.ROUND_END));
    }

    @Test
    void testCommanderKilledTrigger() {
        Game gameWithCommanders = mock(Game.class);
        Player alice = teamOnePlayer;
        Player bob = new Player(1, "Bob");
        bob.setTeam(2);
        when(gameWithCommanders.getPlayersList()).thenReturn(List.of(alice, bob));
        // Bob (Alice's enemy) has no live commanders left; Alice still has one
        when(gameWithCommanders.getLiveCommandersOwnedBy(bob)).thenReturn(0);
        when(gameWithCommanders.getLiveCommandersOwnedBy(alice)).thenReturn(1);

        assertTrue(new CommanderKilledTrigger("Alice").isTriggered(gameWithCommanders, TriggerSituation.ROUND_END));
        assertFalse(new CommanderKilledTrigger("Bob").isTriggered(gameWithCommanders, TriggerSituation.ROUND_END));
        assertTrue(new CommanderKilledTrigger(null).isTriggered(gameWithCommanders, TriggerSituation.ROUND_END));
    }

    @Test
    void testTriggerDeserializerParsesObjectiveTriggers() throws Exception {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

        Trigger controlTrigger = TriggerDeserializer.parseNode(yamlMapper.readTree("""
              type: objectivecontrol
              objective: Relay Station
              player: Alice
              """));
        assertInstanceOf(ObjectiveControlTrigger.class, controlTrigger);
        assertEquals("Relay Station", ((ObjectiveControlTrigger) controlTrigger).objectiveName());

        assertInstanceOf(ObjectiveDestroyedTrigger.class, TriggerDeserializer.parseNode(yamlMapper.readTree("""
              type: objectivedestroyed
              objective: Relay Station
              """)));
        assertInstanceOf(ObjectiveConfirmedTrigger.class, TriggerDeserializer.parseNode(yamlMapper.readTree("""
              type: objectiveconfirmed
              objective: Relay Station
              """)));
        assertInstanceOf(ObjectiveCapturedTrigger.class, TriggerDeserializer.parseNode(yamlMapper.readTree("""
              type: objectivecaptured
              objective: MacGuffin
              player: Alice
              """)));

        Trigger victoryPointsTrigger = TriggerDeserializer.parseNode(yamlMapper.readTree("""
              type: victorypoints
              points: 5
              """));
        assertInstanceOf(VictoryPointsTrigger.class, victoryPointsTrigger);
        assertEquals(5, ((VictoryPointsTrigger) victoryPointsTrigger).minimumPoints());

        assertInstanceOf(BVDestroyedTrigger.class, TriggerDeserializer.parseNode(yamlMapper.readTree("""
              type: bvdestroyed
              percent: 50
              """)));
        assertInstanceOf(BVRatioTrigger.class, TriggerDeserializer.parseNode(yamlMapper.readTree("""
              type: bvratio
              percent: 300
              player: Alice
              """)));
        assertInstanceOf(KillCountTrigger.class, TriggerDeserializer.parseNode(yamlMapper.readTree("""
              type: killcount
              count: 4
              """)));
        assertInstanceOf(CommanderKilledTrigger.class, TriggerDeserializer.parseNode(yamlMapper.readTree("""
              type: commanderkilled
              """)));
    }
}
