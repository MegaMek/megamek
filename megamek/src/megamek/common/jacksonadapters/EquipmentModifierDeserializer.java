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

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.common.modifiers.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class EquipmentModifierDeserializer extends StdDeserializer<EquipmentModifier>  {

    private static final String TYPE = "type";
    private static final String TYPE_HEAT = "heat";
    private static final String TYPE_TOHIT = "tohit";
    private static final String TYPE_DAMAGE = "damage";
    private static final String TYPE_MISFIRE = "misfire";
    private static final String DELTA = "delta";
    private static final String ON = "on";

    protected EquipmentModifierDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public EquipmentModifier deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JacksonException {
        JsonNode node = jp.getCodec().readTree(jp);
        return parseNode(node);
    }

    public static EquipmentModifier parseNode(JsonNode node) {
        String type = node.get(TYPE).asText();
        return switch (type) {
            case TYPE_HEAT -> new WeaponHeatModifier(node.get(DELTA).asInt(), EquipmentModifier.Reason.PARTIAL_REPAIR);
            case TYPE_DAMAGE -> new DamageModifier(node.get(DELTA).asInt(), EquipmentModifier.Reason.PARTIAL_REPAIR);
            case TYPE_TOHIT -> new ToHitModifier(node.get(DELTA).asInt(), EquipmentModifier.Reason.PARTIAL_REPAIR);
            case TYPE_MISFIRE -> parseMisfire(node);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    private static EquipmentModifier parseMisfire(JsonNode node) {
        Set<Integer> misfireRolls = new HashSet<>();
        node.get(ON).forEach(n -> misfireRolls.add(n.asInt()));
        return new WeaponMisfireModifier(misfireRolls, EquipmentModifier.Reason.PARTIAL_REPAIR);
    }
}
