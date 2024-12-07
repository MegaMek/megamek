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
import megamek.common.modifiers.*;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class EquipmentModifierDeserializer extends StdDeserializer<EquipmentModifier>  {

    private static final String TYPE = "type";
    private static final String TYPE_HEAT = "heat";
    private static final String TYPE_TOHIT = "tohit";
    private static final String TYPE_DAMAGE = "damage";
    private static final String TYPE_JAM = "jam";
    private static final String TYPE_NOTWIST = "notwist";
    private static final String TYPE_WALKMP = "walkmp";
    private static final String TYPE_RUNMP = "runmp";
    private static final String DELTA = "delta";
    private static final String ON = "on";
    private static final String SYSTEM = "system";

    protected EquipmentModifierDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public EquipmentModifier deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        return parseNode(node);
    }

    public static EquipmentModifier parseNode(JsonNode node) {
        String type = node.get(TYPE).asText();
        SystemModifier.EntitySystem system = SystemModifier.EntitySystem.NONE;
        if (node.has(SYSTEM)) {
            system = SystemModifier.EntitySystem.valueOf(node.get(SYSTEM).asText().toUpperCase(Locale.ROOT));
        }
        return switch (type) {
            case TYPE_HEAT -> new HeatModifier(node.get(DELTA).asInt(), EquipmentModifier.Reason.DAMAGED);
            case TYPE_DAMAGE -> new DamageModifier(node.get(DELTA).asInt(), EquipmentModifier.Reason.DAMAGED);
            case TYPE_TOHIT -> new ToHitModifier(node.get(DELTA).asInt(), EquipmentModifier.Reason.DAMAGED);
            case TYPE_JAM -> new WeaponJamModifier(parseRollValues(node.get(ON)), EquipmentModifier.Reason.DAMAGED);
            case TYPE_NOTWIST -> new NoTwistModifier(EquipmentModifier.Reason.DAMAGED, SystemModifier.EntitySystem.GYRO);
            case TYPE_WALKMP -> new WalkMPEquipmentModifier(node.get(DELTA).asInt(), EquipmentModifier.Reason.DAMAGED);
            case TYPE_RUNMP -> new RunMPEquipmentModifier(node.get(DELTA).asInt(), EquipmentModifier.Reason.DAMAGED, system);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    /**
     * Parses the given array node that is a list of Integers, e.g. "numbers: [ 2, 3, 5 ]" and returns the numbers as a Set.
     *
     * @param node The node to parse
     * @return The set of Integers given by the node
     */
    private static Set<Integer> parseRollValues(JsonNode node) {
        return StreamSupport.stream(node.spliterator(), false).map(JsonNode::asInt).collect(Collectors.toSet());
    }
}
