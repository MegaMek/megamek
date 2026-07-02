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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.common.annotations.Nullable;

/**
 * Builds composite victory conditions from a flat token formula - condition leaves strung together with
 * {@code and}, {@code or}, {@code not} and parentheses, as entered in the lobby victory condition builder - and
 * compiles them into the scenario {@code victory:} YAML schema that {@link VictoryDeserializer#parseList(String)}
 * parses on the server.
 *
 * <P>The token grammar matches the advanced-search formula builder: a UI offers tokens and gates them with
 * {@link #canAppend(List, Token)} so only well-formed formulas can be built; {@link #compileFormula(List)} then
 * converts the infix token list into the nested trigger tree with the precedence NOT over AND over OR.</P>
 */
public final class VictoryConditionsBuilder {

    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;

    /** A token of the victory condition formula: a condition leaf, an operator or a parenthesis. */
    public sealed interface Token permits ConditionToken, OperatorToken {

        /** @return The text shown for this token in the running formula display */
        String displayText();
    }

    /**
     * A condition leaf of the formula, carrying its display text and its trigger definition in the scenario
     * trigger schema.
     *
     * @param displayText The text shown in the formula display, e.g. "control [Relay Station] by Alice"
     * @param triggerNode The trigger definition node, as {@link TriggerDeserializer} parses it
     */
    public record ConditionToken(String displayText, JsonNode triggerNode) implements Token {}

    /** The operators and parentheses of the formula. */
    public enum OperatorToken implements Token {
        AND("and"),
        OR("or"),
        NOT("not"),
        LEFT_PAREN("("),
        RIGHT_PAREN(")");

        private final String displayText;

        OperatorToken(String displayText) {
            this.displayText = displayText;
        }

        @Override
        public String displayText() {
            return displayText;
        }
    }

    /**
     * Checks whether the given token may be appended to the formula so that it can still become well-formed. UIs use
     * this to enable/disable token buttons, making malformed formulas unbuildable.
     *
     * @param formula The formula so far
     * @param token   The token to append
     *
     * @return {@code true} when appending the token keeps the formula valid
     */
    public static boolean canAppend(List<Token> formula, Token token) {
        Token lastToken = formula.isEmpty() ? null : formula.getLast();
        boolean operandPosition = (lastToken == null)
              || (lastToken == OperatorToken.AND)
              || (lastToken == OperatorToken.OR)
              || (lastToken == OperatorToken.NOT)
              || (lastToken == OperatorToken.LEFT_PAREN);

        if ((token instanceof ConditionToken) || (token == OperatorToken.LEFT_PAREN)
              || (token == OperatorToken.NOT)) {
            return operandPosition;
        }
        if ((token == OperatorToken.AND) || (token == OperatorToken.OR)) {
            return !operandPosition;
        }
        // right parenthesis: needs a completed operand and an unclosed left parenthesis
        return !operandPosition && (openParenthesisCount(formula) > 0);
    }

    /**
     * @param formula The formula so far
     *
     * @return {@code true} when the formula is complete: non-empty, all parentheses closed and not ending in an
     *       operator or opening parenthesis
     */
    public static boolean isCompleteFormula(List<Token> formula) {
        if (formula.isEmpty() || (openParenthesisCount(formula) != 0)) {
            return false;
        }
        Token lastToken = formula.getLast();
        return (lastToken instanceof ConditionToken) || (lastToken == OperatorToken.RIGHT_PAREN);
    }

    /** @return The formula as display text, e.g. "( control [Left] and round 8 ) or not units fled" */
    public static String toDisplayText(List<Token> formula) {
        StringBuilder displayText = new StringBuilder();
        for (Token token : formula) {
            if (!displayText.isEmpty()) {
                displayText.append(" ");
            }
            displayText.append(token.displayText());
        }
        return displayText.toString();
    }

    private static int openParenthesisCount(List<Token> formula) {
        int openCount = 0;
        for (Token token : formula) {
            if (token == OperatorToken.LEFT_PAREN) {
                openCount++;
            } else if (token == OperatorToken.RIGHT_PAREN) {
                openCount--;
            }
        }
        return openCount;
    }

    /**
     * Compiles the infix token formula into a single trigger definition node (nested {@code and}/{@code or}/
     * {@code not} trees around the condition leaves), with the precedence NOT over AND over OR and parentheses
     * grouping.
     *
     * @param formula The complete formula (see {@link #isCompleteFormula(List)})
     *
     * @return The trigger definition node in the scenario trigger schema
     *
     * @throws IllegalArgumentException When the formula is not well-formed
     */
    public static JsonNode compileFormula(List<Token> formula) {
        if (!isCompleteFormula(formula)) {
            throw new IllegalArgumentException("The victory condition formula is incomplete: "
                  + toDisplayText(formula));
        }
        FormulaParser parser = new FormulaParser(formula);
        JsonNode result = parser.parseOrExpression();
        if (!parser.isExhausted()) {
            throw new IllegalArgumentException("The victory condition formula is not well-formed: "
                  + toDisplayText(formula));
        }
        return result;
    }

    /**
     * Compiles a full victory condition into a single entry of the scenario {@code victory:} schema.
     *
     * @param formula    The complete condition formula
     * @param playerName The player whose side wins when the condition holds, or {@code null}/blank for a draw
     *                   condition (draw conditions are game-ending)
     * @param onlyAtEnd  When {@code true}, the condition does not end the game; it is only checked when the game
     *                   ends for another reason (the scenario {@code onlyatend} modifier)
     *
     * @return The victory entry node
     */
    public static ObjectNode compileCondition(List<Token> formula, @Nullable String playerName, boolean onlyAtEnd) {
        ObjectNode victoryEntry = NODE_FACTORY.objectNode();
        if ((playerName != null) && !playerName.isBlank()) {
            victoryEntry.put("player", playerName);
        }
        if (onlyAtEnd) {
            victoryEntry.put("modify", "onlyatend");
        }
        victoryEntry.set("trigger", compileFormula(formula));
        return victoryEntry;
    }

    /**
     * Renders a list of compiled victory entries (see {@link #compileCondition(List, String, boolean)}) as the YAML
     * text sent to the server, parseable by {@link VictoryDeserializer#parseList(String)}.
     *
     * @param victoryEntries The compiled victory entries
     *
     * @return The YAML text
     */
    public static String toYaml(List<ObjectNode> victoryEntries) {
        ArrayNode listNode = NODE_FACTORY.arrayNode();
        victoryEntries.forEach(listNode::add);
        try {
            return new ObjectMapper(new YAMLFactory()).writeValueAsString(listNode);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not render the victory conditions as YAML", exception);
        }
    }

    // --- Condition leaf factories (the lobby builder's leaf set) ---

    /** @return A leaf: the named objective is controlled by the given player's side (or any side when blank) */
    public static ConditionToken objectiveControlled(String objectiveName, @Nullable String playerName) {
        ObjectNode triggerNode = leafNode("objectivecontrol", playerName);
        triggerNode.put("objective", objectiveName);
        return new ConditionToken("control [" + objectiveName + "]" + bySuffix(playerName), triggerNode);
    }

    /** @return A leaf: the named objective has been destroyed */
    public static ConditionToken objectiveDestroyed(String objectiveName) {
        ObjectNode triggerNode = leafNode("objectivedestroyed", null);
        triggerNode.put("objective", objectiveName);
        return new ConditionToken("destroyed [" + objectiveName + "]", triggerNode);
    }

    /** @return A leaf: the named Potential Objective candidate has been confirmed */
    public static ConditionToken objectiveConfirmed(String objectiveName) {
        ObjectNode triggerNode = leafNode("objectiveconfirmed", null);
        triggerNode.put("objective", objectiveName);
        return new ConditionToken("confirmed [" + objectiveName + "]", triggerNode);
    }

    /** @return A leaf: the named Mobile Objective was carried off the battlefield by the given player (or anyone) */
    public static ConditionToken objectiveCaptured(String objectiveName, @Nullable String playerName) {
        ObjectNode triggerNode = leafNode("objectivecaptured", playerName);
        triggerNode.put("objective", objectiveName);
        return new ConditionToken("captured [" + objectiveName + "]" + bySuffix(playerName), triggerNode);
    }

    /** @return A leaf: the given player's side (or any side when blank) has at least the given Victory Points */
    public static ConditionToken victoryPointsReached(@Nullable String playerName, int minimumPoints) {
        ObjectNode triggerNode = leafNode("victorypoints", playerName);
        triggerNode.put("points", minimumPoints);
        return new ConditionToken(minimumPoints + "+ VP" + bySuffix(playerName), triggerNode);
    }

    /** @return A leaf: the end of the given game round has been reached */
    public static ConditionToken roundEndReached(int gameRound) {
        ObjectNode triggerNode = leafNode("roundend", null);
        triggerNode.put("round", gameRound);
        return new ConditionToken("end of round " + gameRound, triggerNode);
    }

    /** @return A leaf: at least the given number of the player's units (or all units when blank) have been killed */
    public static ConditionToken unitsKilled(@Nullable String playerName, int atLeastCount) {
        ObjectNode triggerNode = leafNode("killedunits", playerName);
        triggerNode.put("atLeast", atLeastCount);
        return new ConditionToken(atLeastCount + "+ units killed" + ofSuffix(playerName), triggerNode);
    }

    /** @return A leaf: at least the given number of the player's units (or all units when blank) have fled */
    public static ConditionToken unitsFled(@Nullable String playerName, int atLeastCount) {
        ObjectNode triggerNode = leafNode("fledunits", playerName);
        triggerNode.put("atLeast", atLeastCount);
        return new ConditionToken(atLeastCount + "+ units fled" + ofSuffix(playerName), triggerNode);
    }

    /** @return A leaf: only one team has units with players remaining on the battlefield */
    public static ConditionToken battlefieldControl() {
        return new ConditionToken("battlefield control", leafNode("battlefieldcontrol", null));
    }

    /** @return A leaf: the given player's side (or any side) has destroyed the given percent of enemy starting BV */
    public static ConditionToken enemyBvDestroyed(@Nullable String playerName, int destroyedPercent) {
        ObjectNode triggerNode = leafNode("bvdestroyed", playerName);
        triggerNode.put("percent", destroyedPercent);
        return new ConditionToken(destroyedPercent + "%+ enemy BV destroyed" + bySuffix(playerName), triggerNode);
    }

    /** @return A leaf: the given player's side (or any side) has at least the given friendly/enemy BV ratio */
    public static ConditionToken bvRatioReached(@Nullable String playerName, int ratioPercent) {
        ObjectNode triggerNode = leafNode("bvratio", playerName);
        triggerNode.put("percent", ratioPercent);
        return new ConditionToken(ratioPercent + "%+ BV ratio" + forSuffix(playerName), triggerNode);
    }

    /** @return A leaf: the given player's side (or any side) has destroyed at least the given number of enemies */
    public static ConditionToken killCountReached(@Nullable String playerName, int killCount) {
        ObjectNode triggerNode = leafNode("killcount", playerName);
        triggerNode.put("count", killCount);
        return new ConditionToken(killCount + "+ kills" + bySuffix(playerName), triggerNode);
    }

    /** @return A leaf: all commanders of the given player's enemies (or of any side's enemies) are destroyed */
    public static ConditionToken enemyCommandersKilled(@Nullable String playerName) {
        return new ConditionToken("enemy commanders destroyed" + ofSuffix(playerName),
              leafNode("commanderkilled", playerName));
    }

    private static ObjectNode leafNode(String triggerType, @Nullable String playerName) {
        ObjectNode triggerNode = NODE_FACTORY.objectNode();
        triggerNode.put("type", triggerType);
        if ((playerName != null) && !playerName.isBlank()) {
            triggerNode.put("player", playerName);
        }
        return triggerNode;
    }

    private static String bySuffix(@Nullable String playerName) {
        return ((playerName == null) || playerName.isBlank()) ? "" : " by " + playerName;
    }

    private static String forSuffix(@Nullable String playerName) {
        return ((playerName == null) || playerName.isBlank()) ? "" : " for " + playerName;
    }

    private static String ofSuffix(@Nullable String playerName) {
        return ((playerName == null) || playerName.isBlank()) ? "" : " of " + playerName;
    }

    /**
     * Recursive-descent parser over the token list with the grammar: orExpression = andExpression (OR
     * andExpression)*; andExpression = notExpression (AND notExpression)*; notExpression = NOT notExpression |
     * primary; primary = leaf | ( orExpression ).
     */
    private static final class FormulaParser {

        private final List<Token> tokens;
        private int position = 0;

        FormulaParser(List<Token> tokens) {
            this.tokens = new ArrayList<>(tokens);
        }

        JsonNode parseOrExpression() {
            JsonNode result = parseAndExpression();
            List<JsonNode> operands = new ArrayList<>(List.of(result));
            while (peek() == OperatorToken.OR) {
                position++;
                operands.add(parseAndExpression());
            }
            return (operands.size() == 1) ? result : compositeNode("or", operands);
        }

        private JsonNode parseAndExpression() {
            JsonNode result = parseNotExpression();
            List<JsonNode> operands = new ArrayList<>(List.of(result));
            while (peek() == OperatorToken.AND) {
                position++;
                operands.add(parseNotExpression());
            }
            return (operands.size() == 1) ? result : compositeNode("and", operands);
        }

        private JsonNode parseNotExpression() {
            if (peek() == OperatorToken.NOT) {
                position++;
                ObjectNode notNode = NODE_FACTORY.objectNode();
                notNode.put("type", "not");
                notNode.set("triggers", parseNotExpression());
                return notNode;
            }
            return parsePrimary();
        }

        private JsonNode parsePrimary() {
            Token token = next();
            if (token instanceof ConditionToken conditionToken) {
                return conditionToken.triggerNode();
            }
            if (token == OperatorToken.LEFT_PAREN) {
                JsonNode inner = parseOrExpression();
                if (next() != OperatorToken.RIGHT_PAREN) {
                    throw new IllegalArgumentException("Unbalanced parentheses in the victory condition formula");
                }
                return inner;
            }
            throw new IllegalArgumentException("Unexpected token in the victory condition formula: "
                  + ((token == null) ? "end of formula" : token.displayText()));
        }

        private JsonNode compositeNode(String operatorType, List<JsonNode> operands) {
            ObjectNode composite = NODE_FACTORY.objectNode();
            composite.put("type", operatorType);
            ArrayNode triggersNode = NODE_FACTORY.arrayNode();
            operands.forEach(triggersNode::add);
            composite.set("triggers", triggersNode);
            return composite;
        }

        @Nullable
        private Token peek() {
            return (position < tokens.size()) ? tokens.get(position) : null;
        }

        @Nullable
        private Token next() {
            return (position < tokens.size()) ? tokens.get(position++) : null;
        }

        boolean isExhausted() {
            return position >= tokens.size();
        }
    }

    private VictoryConditionsBuilder() {}
}
