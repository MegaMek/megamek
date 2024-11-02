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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.client.bot.princess.BehaviorSettings;
import megamek.client.ui.swing.ScenarioDialog;

public final class BotParser {

    static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    public interface BotInfo {
        int type();
    }

    public record PrincessRecord(BehaviorSettings behaviorSettings) implements BotInfo {

        @Override
        public int type() {
            return ScenarioDialog.T_BOT;
        }
    }

    public static BotInfo parse(JsonNode node) throws JsonProcessingException {
        PrincessSettingsBuilder builder = YAML_MAPPER.treeToValue(node, PrincessSettingsBuilder.class);
        return new PrincessRecord(builder.build());
    }

    private BotParser() { }
}
