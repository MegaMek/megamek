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

package megamek.common.jacksonAdapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import megamek.common.jacksonAdapters.VictoryConditionsBuilder.ConditionToken;
import megamek.common.jacksonAdapters.VictoryConditionsBuilder.OperatorToken;
import megamek.common.jacksonAdapters.VictoryConditionsBuilder.Token;
import megamek.server.scriptedEvents.TriggeredEvent;
import megamek.server.scriptedEvents.VictoryTriggeredEvent;
import megamek.server.trigger.AndTrigger;
import megamek.server.trigger.NotTrigger;
import megamek.server.trigger.ObjectiveControlTrigger;
import megamek.server.trigger.OrTrigger;
import megamek.server.trigger.VictoryPointsTrigger;
import org.junit.jupiter.api.Test;

/**
 * Tests the lobby victory condition formula builder: grammar gating, formula compilation with operator precedence,
 * and the full round trip through the YAML that the server-side victory deserializer parses.
 */
class VictoryConditionsBuilderTest {

    private final ConditionToken controlLeaf =
          VictoryConditionsBuilder.objectiveControlled("Relay Station", "Alice");
    private final ConditionToken roundLeaf = VictoryConditionsBuilder.roundEndReached(8);
    private final ConditionToken pointsLeaf = VictoryConditionsBuilder.victoryPointsReached("Alice", 5);

    @Test
    void testGrammarGating() {
        List<Token> formula = new ArrayList<>();

        // an empty formula accepts operands, ( and NOT but no binary operators or )
        assertTrue(VictoryConditionsBuilder.canAppend(formula, controlLeaf));
        assertTrue(VictoryConditionsBuilder.canAppend(formula, OperatorToken.LEFT_PAREN));
        assertTrue(VictoryConditionsBuilder.canAppend(formula, OperatorToken.NOT));
        assertFalse(VictoryConditionsBuilder.canAppend(formula, OperatorToken.AND));
        assertFalse(VictoryConditionsBuilder.canAppend(formula, OperatorToken.RIGHT_PAREN));

        formula.add(controlLeaf);
        // after an operand: binary operators yes, another operand no, ) only with an open (
        assertTrue(VictoryConditionsBuilder.canAppend(formula, OperatorToken.AND));
        assertTrue(VictoryConditionsBuilder.canAppend(formula, OperatorToken.OR));
        assertFalse(VictoryConditionsBuilder.canAppend(formula, roundLeaf));
        assertFalse(VictoryConditionsBuilder.canAppend(formula, OperatorToken.RIGHT_PAREN));

        assertFalse(VictoryConditionsBuilder.isCompleteFormula(List.of(controlLeaf, (Token) OperatorToken.AND)));
        assertTrue(VictoryConditionsBuilder.isCompleteFormula(List.of(controlLeaf)));
    }

    @Test
    void testDisplayText() {
        List<Token> formula = List.of(OperatorToken.LEFT_PAREN, controlLeaf, OperatorToken.AND, roundLeaf,
              OperatorToken.RIGHT_PAREN, OperatorToken.OR, OperatorToken.NOT, pointsLeaf);

        assertEquals("( control [Relay Station] by Alice and end of round 8 ) or not 5+ VP by Alice",
              VictoryConditionsBuilder.toDisplayText(formula));
    }

    @Test
    void testAndOverOrPrecedence() throws Exception {
        // a or b and c must parse as a or (b and c)
        List<Token> formula = List.of(pointsLeaf, OperatorToken.OR, controlLeaf, OperatorToken.AND, roundLeaf);
        String yaml = VictoryConditionsBuilder.toYaml(
              List.of(VictoryConditionsBuilder.compileCondition(formula, "Alice", false)));

        List<TriggeredEvent> events = VictoryDeserializer.parseList(yaml);
        VictoryTriggeredEvent victoryEvent = (VictoryTriggeredEvent) events.getFirst();
        assertInstanceOf(OrTrigger.class, victoryEvent.trigger());
        OrTrigger orTrigger = (OrTrigger) victoryEvent.trigger();
        assertEquals(2, orTrigger.triggers().size());
        assertInstanceOf(VictoryPointsTrigger.class, orTrigger.triggers().get(0));
        assertInstanceOf(AndTrigger.class, orTrigger.triggers().get(1));
    }

    @Test
    void testParenthesesOverridePrecedence() throws Exception {
        // (a or b) and c
        List<Token> formula = List.of(OperatorToken.LEFT_PAREN, pointsLeaf, OperatorToken.OR, controlLeaf,
              OperatorToken.RIGHT_PAREN, OperatorToken.AND, roundLeaf);
        String yaml = VictoryConditionsBuilder.toYaml(
              List.of(VictoryConditionsBuilder.compileCondition(formula, "Alice", false)));

        List<TriggeredEvent> events = VictoryDeserializer.parseList(yaml);
        VictoryTriggeredEvent victoryEvent = (VictoryTriggeredEvent) events.getFirst();
        assertInstanceOf(AndTrigger.class, victoryEvent.trigger());
        AndTrigger andTrigger = (AndTrigger) victoryEvent.trigger();
        assertInstanceOf(OrTrigger.class, andTrigger.triggers().get(0));
    }

    @Test
    void testNotCompilesToNotTrigger() throws Exception {
        List<Token> formula = List.of(OperatorToken.NOT, controlLeaf);
        String yaml = VictoryConditionsBuilder.toYaml(
              List.of(VictoryConditionsBuilder.compileCondition(formula, "Bob", false)));

        List<TriggeredEvent> events = VictoryDeserializer.parseList(yaml);
        VictoryTriggeredEvent victoryEvent = (VictoryTriggeredEvent) events.getFirst();
        assertInstanceOf(NotTrigger.class, victoryEvent.trigger());
        assertInstanceOf(ObjectiveControlTrigger.class, ((NotTrigger) victoryEvent.trigger()).trigger());
    }

    @Test
    void testFullConditionListRoundTrip() throws Exception {
        List<Token> aliceFormula = List.of(controlLeaf, OperatorToken.AND, roundLeaf);
        List<Token> drawFormula = List.of(VictoryConditionsBuilder.roundEndReached(12));
        String yaml = VictoryConditionsBuilder.toYaml(List.of(
              VictoryConditionsBuilder.compileCondition(aliceFormula, "Alice", true),
              VictoryConditionsBuilder.compileCondition(drawFormula, null, false)));

        List<TriggeredEvent> events = VictoryDeserializer.parseList(yaml);

        assertEquals(2, events.size());
        VictoryTriggeredEvent aliceVictory = (VictoryTriggeredEvent) events.get(0);
        // onlyatend: the condition does not itself end the game
        assertFalse(aliceVictory.isGameEnding());
        assertTrue(events.get(1).isGameEnding());
    }

    @Test
    void testIncompleteFormulaIsRejected() {
        List<Token> danglingOperator = List.of(controlLeaf, OperatorToken.AND);
        assertThrows(IllegalArgumentException.class,
              () -> VictoryConditionsBuilder.compileFormula(danglingOperator));

        List<Token> unbalancedParenthesis = List.of(OperatorToken.LEFT_PAREN, controlLeaf);
        assertThrows(IllegalArgumentException.class,
              () -> VictoryConditionsBuilder.compileFormula(unbalancedParenthesis));
    }

    @Test
    void testClassicVictoryOptionFormula() throws Exception {
        // the motivating example: (destroy 50% enemy BV or 300% BV ratio) and objective held
        List<Token> formula = List.of(
              OperatorToken.LEFT_PAREN,
              VictoryConditionsBuilder.enemyBvDestroyed("Alice", 50),
              OperatorToken.OR,
              VictoryConditionsBuilder.bvRatioReached("Alice", 300),
              OperatorToken.RIGHT_PAREN,
              OperatorToken.AND,
              VictoryConditionsBuilder.objectiveControlled("Relay Station", "Alice"));
        String yaml = VictoryConditionsBuilder.toYaml(
              List.of(VictoryConditionsBuilder.compileCondition(formula, "Alice", false)));

        List<TriggeredEvent> events = VictoryDeserializer.parseList(yaml);
        VictoryTriggeredEvent victoryEvent = (VictoryTriggeredEvent) events.getFirst();
        assertInstanceOf(AndTrigger.class, victoryEvent.trigger());
        AndTrigger andTrigger = (AndTrigger) victoryEvent.trigger();
        assertInstanceOf(OrTrigger.class, andTrigger.triggers().get(0));
        assertInstanceOf(ObjectiveControlTrigger.class, andTrigger.triggers().get(1));
    }

    @Test
    void testAllLeafFactoriesParse() throws Exception {
        List<ObjectNode> victoryEntries = new ArrayList<>();
        List<ConditionToken> allLeaves = List.of(
              VictoryConditionsBuilder.objectiveControlled("Left", null),
              VictoryConditionsBuilder.objectiveDestroyed("Left"),
              VictoryConditionsBuilder.objectiveConfirmed("Left"),
              VictoryConditionsBuilder.objectiveCaptured("MacGuffin", "Alice"),
              VictoryConditionsBuilder.victoryPointsReached(null, 3),
              VictoryConditionsBuilder.roundEndReached(10),
              VictoryConditionsBuilder.unitsKilled("Bob", 4),
              VictoryConditionsBuilder.unitsFled(null, 2),
              VictoryConditionsBuilder.battlefieldControl(),
              VictoryConditionsBuilder.enemyBvDestroyed(null, 50),
              VictoryConditionsBuilder.bvRatioReached("Alice", 300),
              VictoryConditionsBuilder.killCountReached("Bob", 4),
              VictoryConditionsBuilder.enemyCommandersKilled(null));
        for (ConditionToken leaf : allLeaves) {
            victoryEntries.add(VictoryConditionsBuilder.compileCondition(List.of(leaf), "Alice", false));
        }

        List<TriggeredEvent> events = VictoryDeserializer.parseList(
              VictoryConditionsBuilder.toYaml(victoryEntries));

        assertEquals(allLeaves.size(), events.size());
    }
}
