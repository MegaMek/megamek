/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.jacksonadapters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.common.enums.GamePhase;
import megamek.common.hexarea.HexArea;
import megamek.server.trigger.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static megamek.common.jacksonadapters.MMUReader.requireFields;

public class TriggerDeserializer extends StdDeserializer<Trigger> {

    private static final String TRIGGERS = "triggers";
    private static final String TYPE = "type";
    private static final String TYPE_AND = "and";
    private static final String TYPE_OR = "or";
    private static final String TYPE_GAMESTART = "gamestart";
    private static final String TYPE_ROUNDSTART = "roundstart";
    private static final String TYPE_ROUNDEND = "roundend";
    private static final String TYPE_PHASESTART = "phasestart";
    private static final String TYPE_FLEDUNITS = "fledunits";
    private static final String TYPE_ACTIVEUNITS = "activeunits";
    private static final String TYPE_KILLEDUNITS = "killedunits";
    private static final String TYPE_KILLEDUNIT = "killedunit";
    private static final String TYPE_UNIT_POSITION = "position";
    private static final String TYPE_UNITS_POSITION = "positions";
    private static final String TYPE_BATTLEFIELD_CONTROL = "battlefieldcontrol";
    private static final String PLAYER = "player";
    private static final String COUNT = "count";
    private static final String AT_MOST = "atmost";
    private static final String AT_LEAST = "atleast";
    private static final String ROUND = "round";
    private static final String PHASE = "phase";
    private static final String UNITS = "units";
    private static final String UNIT = "unit";
    private static final String MODIFY = "modify";
    private static final String ONCE = "once";
    private static final String NOT = "not";
    private static final String AT_END = "atend";
    private static final String AREA = "area";

    public TriggerDeserializer() {
        this(null);
    }

    public TriggerDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Trigger deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        return parseNode(node);
    }

    public static Trigger parseNode(JsonNode node) {
        requireFields("Trigger", node, TYPE);

        String type = node.get(TYPE).asText();
        Trigger trigger = switch (type) {
            case TYPE_AND -> parseAndTrigger(node);
            case TYPE_OR -> parseOrTrigger(node);
            case TYPE_GAMESTART -> new GameStartTrigger();
            case TYPE_ROUNDSTART -> parseRoundStartTrigger(node);
            case TYPE_ROUNDEND -> parseRoundEndTrigger(node);
            case TYPE_PHASESTART -> parsePhaseStartTrigger(node);
            case TYPE_FLEDUNITS -> parseFledUnitsTrigger(node);
            case TYPE_ACTIVEUNITS -> parseActiveUnitsTrigger(node);
            case TYPE_KILLEDUNITS -> parseKilledUnitsTrigger(node);
            case TYPE_KILLEDUNIT -> parseKilledUnitTrigger(node);
            case TYPE_BATTLEFIELD_CONTROL -> new BattlefieldControlTrigger();
            case TYPE_UNIT_POSITION -> parseUnitPositionTrigger(node);
            case TYPE_UNITS_POSITION -> parseUnitsPositionTrigger(node);
            case NOT -> parseNotTrigger(node);
            case ROUND -> new RoundTrigger(node.get(ROUND).asInt());
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
        if (node.has(MODIFY)) {
            List<String> modifiers = parseArrayOrSingleNode(node.get(MODIFY), NOT, ONCE, AT_END);
            // the order of the following two is important; also, both are possible
            if (modifiers.contains(NOT)) {
                trigger = new NotTrigger(trigger);
            }
            if (modifiers.contains(ONCE)) {
                trigger = new OnceTrigger(trigger);
            }
            if (modifiers.contains(AT_END)) {
                trigger = new AndTrigger(new GameEndTrigger(), trigger);
            }
        }
        return trigger;
    }

    private static Trigger parseNotTrigger(JsonNode triggerNode) {
        return new NotTrigger(parseNode(triggerNode.get(TRIGGERS)));
    }

    private static Trigger parseAndTrigger(JsonNode triggerNode) {
        JsonNode subTriggersNode = triggerNode.get(TRIGGERS);
        List<Trigger> subTriggers = new ArrayList<>();
        subTriggersNode.iterator().forEachRemaining(n -> subTriggers.add(parseNode(n)));
        return new AndTrigger(subTriggers);
    }

    private static Trigger parseOrTrigger(JsonNode triggerNode) {
        JsonNode subTriggersNode = triggerNode.get(TRIGGERS);
        List<Trigger> subTriggers = new ArrayList<>();
        subTriggersNode.iterator().forEachRemaining(n -> subTriggers.add(parseNode(n)));
        return new OrTrigger(subTriggers);
    }

    private static Trigger parseRoundStartTrigger(JsonNode triggerNode) {
        if (triggerNode.has(ROUND)) {
            return new SpecificRoundStartTrigger(triggerNode.get(ROUND).asInt());
        } else {
            return new AnyRoundStartTrigger();
        }
    }

    private static Trigger parseRoundEndTrigger(JsonNode triggerNode) {
        if (triggerNode.has(ROUND)) {
            return new SpecificRoundEndTrigger(triggerNode.get(ROUND).asInt());
        } else {
            return new AnyRoundEndTrigger();
        }
    }

    private static Trigger parseFledUnitsTrigger(JsonNode triggerNode) {
        int minCount = Integer.MIN_VALUE;
        int maxCount = Integer.MAX_VALUE;
        if (triggerNode.has(AT_MOST)) {
            maxCount = triggerNode.get(AT_MOST).asInt();
        }
        if (triggerNode.has(AT_LEAST)) {
            minCount = triggerNode.get(AT_LEAST).asInt();
        }
        if (triggerNode.has(COUNT)) {
            minCount = triggerNode.get(COUNT).asInt();
            maxCount = triggerNode.get(COUNT).asInt();
        }
        String player = "";
        List<Integer> unitIds = new ArrayList<>();
        if (triggerNode.has(PLAYER)) {
            player = triggerNode.get(PLAYER).asText();
        }
        if (triggerNode.has(UNITS)) {
            triggerNode.get(UNITS).iterator().forEachRemaining(id -> unitIds.add(id.asInt()));
        }
        return new FledUnitsTrigger(player, unitIds, minCount, maxCount);
    }

    private static Trigger parseActiveUnitsTrigger(JsonNode triggerNode) {
        int minCount = Integer.MIN_VALUE;
        int maxCount = Integer.MAX_VALUE;
        if (triggerNode.has(AT_MOST)) {
            maxCount = triggerNode.get(AT_MOST).asInt();
        }
        if (triggerNode.has(AT_LEAST)) {
            minCount = triggerNode.get(AT_LEAST).asInt();
        }
        if (triggerNode.has(COUNT)) {
            minCount = triggerNode.get(COUNT).asInt();
            maxCount = triggerNode.get(COUNT).asInt();
        }
        String player = "";
        List<Integer> unitIds = new ArrayList<>();
        if (triggerNode.has(PLAYER)) {
            player = triggerNode.get(PLAYER).asText();
        }
        if (triggerNode.has(UNITS)) {
            triggerNode.get(UNITS).iterator().forEachRemaining(id -> unitIds.add(id.asInt()));
        }
        return new ActiveUnitsTrigger(player, unitIds, minCount, maxCount);
    }

    private static Trigger parseKilledUnitsTrigger(JsonNode triggerNode) {
        int minCount = Integer.MIN_VALUE;
        int maxCount = Integer.MAX_VALUE;
        if (triggerNode.has(AT_MOST)) {
            maxCount = triggerNode.get(AT_MOST).asInt();
        }
        if (triggerNode.has(AT_LEAST)) {
            minCount = triggerNode.get(AT_LEAST).asInt();
        }
        if (triggerNode.has(COUNT)) {
            minCount = triggerNode.get(COUNT).asInt();
            maxCount = triggerNode.get(COUNT).asInt();
        }
        String player = "";
        List<Integer> unitIds = new ArrayList<>();
        if (triggerNode.has(PLAYER)) {
            player = triggerNode.get(PLAYER).asText();
        }
        if (triggerNode.has(UNITS)) {
            triggerNode.get(UNITS).iterator().forEachRemaining(id -> unitIds.add(id.asInt()));
        }
        return new KilledUnitsTrigger(player, unitIds, minCount, maxCount);
    }

    private static Trigger parseUnitPositionTrigger(JsonNode triggerNode) {
        HexArea area = HexAreaDeserializer.parseShape(triggerNode.get(AREA));
        return new UnitPositionTrigger(area, triggerNode.get(UNIT).asInt());
    }

    private static Trigger parseUnitsPositionTrigger(JsonNode triggerNode) {
        HexArea area = HexAreaDeserializer.parseShape(triggerNode.get(AREA));
        int minCount = Integer.MIN_VALUE;
        int maxCount = Integer.MAX_VALUE;
        if (triggerNode.has(AT_MOST)) {
            maxCount = triggerNode.get(AT_MOST).asInt();
        }
        if (triggerNode.has(AT_LEAST)) {
            minCount = triggerNode.get(AT_LEAST).asInt();
        }
        if (triggerNode.has(COUNT)) {
            minCount = triggerNode.get(COUNT).asInt();
            maxCount = triggerNode.get(COUNT).asInt();
        }
        String player = "";
        List<Integer> unitIds = new ArrayList<>();
        if (triggerNode.has(PLAYER)) {
            player = triggerNode.get(PLAYER).asText();
        }
        if (triggerNode.has(UNITS)) {
            triggerNode.get(UNITS).iterator().forEachRemaining(id -> unitIds.add(id.asInt()));
        }
        return new UnitPositionTrigger(area, player, unitIds, minCount, maxCount);
    }

    private static Trigger parseKilledUnitTrigger(JsonNode triggerNode) {
        return new KilledUnitsTrigger(triggerNode.get(UNIT).asInt());
    }

    private static Trigger parsePhaseStartTrigger(JsonNode triggerNode) {
        String phase = triggerNode.get(PHASE).asText();
        return new PhaseStartTrigger(parsePhase(phase));
    }

    private static GamePhase parsePhase(String phaseName) {
        return GamePhase.valueOf(phaseName.toUpperCase());
    }

    /**
     * Returns all Strings of a node as a List. The node may be either of the form "node: singleString", in
     * which case the List will only contain "singleString", or it may be an array node of the form
     * "node: [ firstString, secondString ]" (or the multi-line form using dashes) in which case the list
     * contains all the given Strings.
     * When no allowedStrings are given, no check is performed on the resulting Strings. If at least one
     * allowedString is given, all found Strings must match the allowedStrings or an exception is thrown.
     *
     * @throws IllegalArgumentException if allowed Strings are given and any of the Strings is not part
     * of the allowed Strings
     * @param node The node to parse
     * @param allowedStrings All Strings that are allowed as values of the node
     * @return A list of the given String values of the node
     */
    public static List<String> parseArrayOrSingleNode(JsonNode node, String... allowedStrings) {
        List<String> result = new ArrayList<>();
        List<String> allowed = Arrays.asList(allowedStrings);
        if (node.isArray()) {
            node.iterator().forEachRemaining(n -> result.add(n.asText()));
        } else {
            result.add(node.asText());
        }
        if (!allowed.isEmpty() && result.stream().anyMatch(s -> !allowed.contains(s))) {
            throw new IllegalArgumentException("invalid modifier");
        }
        return result;
    }
}
