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

import static megamek.common.jacksonAdapters.MMUReader.requireFields;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import megamek.common.board.Coords;
import megamek.common.equipment.ObjectiveMarker;

/**
 * Parses an objective marker (Standard Missions, Objectives) from a scenario file (V2 YAML format only). Objectives
 * are listed per player (the player owns the objective, making it friendly to that player's side), in an
 * {@code objectives:} list:
 *
 * <PRE>
 * objectives:
 *   - name: Left Counter
 *     at: [ 6, 5 ]
 *     controlRadius: 1
 *   - name: MacGuffin
 *     at: [ 20, 12 ]
 *     variants: [ fragile, mobile ]
 * </PRE>
 *
 * Keys: {@code name} and a position ({@code at} or {@code x}/{@code y}) are required. {@code controlRadius} (0-2,
 * default 0), {@code vp} (victory point value, default 1), {@code destructible} (objectives are destructible by
 * default per the Objectives rules; set {@code false} when the mission states that objectives cannot be destroyed)
 * and {@code variants} (any of {@code potential}, {@code false}, {@code fragile}, {@code mobile}) are optional.
 * Potential and False Objectives cannot be combined (RAW).
 */
public final class ObjectiveDeserializer {

    private static final String NAME = "name";
    private static final String AT = "at";
    private static final String X = "x";
    private static final String Y = "y";
    private static final String CONTROL_RADIUS = "controlRadius";
    private static final String VICTORY_POINTS = "vp";
    private static final String DESTRUCTIBLE = "destructible";
    private static final String VARIANTS = "variants";
    private static final String VARIANT_POTENTIAL = "potential";
    private static final String VARIANT_FALSE = "false";
    private static final String VARIANT_FRAGILE = "fragile";
    private static final String VARIANT_MOBILE = "mobile";

    /**
     * An objective marker parsed from a scenario file together with its board position.
     *
     * @param marker   The objective marker
     * @param position The position to place the marker at
     */
    public record ObjectiveInfo(ObjectiveMarker marker, Coords position) {}

    /**
     * Parses a single objective node from a scenario file.
     *
     * @param node The objective node holding the objective's keys
     *
     * @return The parsed objective marker and its position
     *
     * @throws IllegalArgumentException When required keys are missing or values are illegal
     */
    public static ObjectiveInfo parse(JsonNode node) {
        requireFields("Objective", node, NAME);
        ObjectiveMarker marker = new ObjectiveMarker();
        marker.setName(node.get(NAME).textValue());

        if (node.has(CONTROL_RADIUS)) {
            marker.setControlRadius(node.get(CONTROL_RADIUS).asInt());
        }
        if (node.has(VICTORY_POINTS)) {
            marker.setVictoryPointValue(node.get(VICTORY_POINTS).asInt());
        }
        if (node.has(DESTRUCTIBLE)) {
            marker.setInvulnerable(!node.get(DESTRUCTIBLE).asBoolean());
        }
        parseVariants(marker, node);
        if (marker.isPotential() && marker.isFalseObjective()) {
            throw new IllegalArgumentException("Objective " + marker.generalName()
                  + ": Potential Objectives cannot be used in conjunction with False Objectives");
        }

        return new ObjectiveInfo(marker, readPosition(marker, node));
    }

    private static Coords readPosition(ObjectiveMarker marker, JsonNode node) {
        try {
            if (node.has(AT)) {
                List<Integer> xyList = new ArrayList<>();
                node.get(AT).elements().forEachRemaining(coordinateNode -> xyList.add(coordinateNode.asInt()));
                return new Coords(xyList.getFirst(), xyList.get(1));
            } else if (node.has(X) || node.has(Y)) {
                requireFields("Objective", node, X, Y);
                return new Coords(node.get(X).asInt(), node.get(Y).asInt());
            }
        } catch (Exception exception) {
            throw new IllegalArgumentException("Illegal position information for objective " + marker.generalName(),
                  exception);
        }
        throw new IllegalArgumentException("Objective " + marker.generalName()
              + " requires a position (at: or x:/y:)");
    }

    private static void parseVariants(ObjectiveMarker marker, JsonNode node) {
        if (!node.has(VARIANTS)) {
            return;
        }
        node.get(VARIANTS).iterator().forEachRemaining(variantNode -> parseVariant(marker, variantNode.asText()));
    }

    private static void parseVariant(ObjectiveMarker marker, String variant) {
        switch (variant) {
            case VARIANT_POTENTIAL -> marker.setPotential(true);
            case VARIANT_FALSE -> marker.setFalseObjective(true);
            case VARIANT_FRAGILE -> marker.setFragile(true);
            case VARIANT_MOBILE -> marker.setMobile(true);
            default -> throw new IllegalArgumentException("Unknown objective variant " + variant
                  + " for objective " + marker.generalName());
        }
    }

    private ObjectiveDeserializer() {}
}
