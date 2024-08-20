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

import com.fasterxml.jackson.databind.JsonNode;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.bot.princess.BehaviorSettingsFactory;
import megamek.client.bot.princess.CardinalEdge;
import megamek.client.ui.swing.ScenarioDialog;

import java.util.List;

public final class BotParser {

    public interface BotInfo {
        int type();
    }

    public record PrincessRecord(BehaviorSettings behaviorSettings) implements BotInfo {

        @Override
        public int type() {
            return ScenarioDialog.T_BOT;
        }
    }

    private static final String TYPE = "type";
    private static final String BOT_PRINCESS = "princess";
    private static final String PRINCESS_SELF_PRESERVATION = "selfpreservation";
    private static final String PRINCESS_FALL_SHAME = "fallshame";
    private static final String PRINCESS_AGGRESSION = "hyperaggression";
    private static final String PRINCESS_HERDING = "herdmentality";
    private static final String PRINCESS_BRAVERY = "bravery";
    private static final String PRINCESS_DESTINATION = "destination";
    private static final String PRINCESS_RETREAT = "retreat";
    private static final String PRINCESS_FLEE = "flee";
    private static final String PRINCESS_FORCED_WITHDRAW = "forcedwithdraw";
    private static final String STATUS = "status";

    public static BotInfo parse(JsonNode node) {
        if (!node.has(TYPE) || node.get(TYPE).asText().equals(BOT_PRINCESS)) {
            BehaviorSettings behavior = BehaviorSettingsFactory.getInstance().DEFAULT_BEHAVIOR;
            assert behavior != null;
            if (node.has(PRINCESS_SELF_PRESERVATION)) {
                behavior.setSelfPreservationIndex(node.get(PRINCESS_SELF_PRESERVATION).asInt(5));
            }
            if (node.has(PRINCESS_FALL_SHAME)) {
                behavior.setFallShameIndex(node.get(PRINCESS_FALL_SHAME).asInt(5));
            }
            if (node.has(PRINCESS_AGGRESSION)) {
                behavior.setHyperAggressionIndex(node.get(PRINCESS_AGGRESSION).asInt(5));
            }
            if (node.has(PRINCESS_HERDING)) {
                behavior.setHerdMentalityIndex(node.get(PRINCESS_HERDING).asInt(5));
            }
            if (node.has(PRINCESS_BRAVERY)) {
                behavior.setBraveryIndex(node.get(PRINCESS_BRAVERY).asInt(5));
            }
            if (node.has(PRINCESS_DESTINATION)) {
                CardinalEdge edge = CardinalEdge.parseFromString(
                        node.get(PRINCESS_DESTINATION).asText("NONE").toUpperCase());
                behavior.setDestinationEdge(edge);
            }
            if (node.has(PRINCESS_RETREAT)) {
                CardinalEdge edge = CardinalEdge.parseFromString(
                        node.get(PRINCESS_RETREAT).asText("NONE").toUpperCase());
                behavior.setRetreatEdge(edge);
            }
            List<String> statusStrings = TriggerDeserializer.parseArrayOrSingleNode(node.get(STATUS),
                    PRINCESS_FLEE, PRINCESS_FORCED_WITHDRAW);
            if (statusStrings.contains(PRINCESS_FLEE)) {
                behavior.setAutoFlee(true);
            }
            if (statusStrings.contains(PRINCESS_FORCED_WITHDRAW)) {
                behavior.setForcedWithdrawal(true);
            }
            return new PrincessRecord(behavior);
        } else {
            throw new IllegalArgumentException("Invalid bot type");
        }
    }

    private BotParser() { }
}
