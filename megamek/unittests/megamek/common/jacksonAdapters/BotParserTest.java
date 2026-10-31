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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import megamek.client.bot.AIType;
import megamek.client.bot.princess.BehaviorSettings;
import org.junit.jupiter.api.Test;

class BotParserTest {

    private static BotParser.PrincessRecord parse(String yaml) throws Exception {
        JsonNode node = BotParser.YAML_MAPPER.readTree(yaml);
        BotParser.BotInfo botInfo = BotParser.parse(node);
        assertInstanceOf(BotParser.PrincessRecord.class, botInfo);
        return (BotParser.PrincessRecord) botInfo;
    }

    @Test
    void defaultsToPrincessWhenNoAiKey() throws Exception {
        assertEquals(AIType.PRINCESS, parse("name: TestBot").aiType());
    }

    @Test
    void readsExplicitAiKeyCaseInsensitively() throws Exception {
        assertEquals(AIType.PRINCESS, parse("ai: princess").aiType());
        assertEquals(AIType.PRINCESS, parse("ai: PRINCESS").aiType());
    }

    @Test
    void unknownAiKeyFallsBackToPrincess() throws Exception {
        assertEquals(AIType.PRINCESS, parse("ai: nonsense").aiType());
    }

    @Test
    void readsPriorityTargetsWithAndWithoutAPriority() throws Exception {
        String yaml = String.join("\n",
              "priorityTargets:",
              "- unit: 9",
              "  priority: 1",
              "- unit: 11");

        BehaviorSettings settings = parse(yaml).behaviorSettings();

        assertEquals(Set.of(9, 11), settings.getPriorityUnitTargets());
        assertEquals(1, settings.getTargetPriority(9));
        assertEquals(BehaviorSettings.DEFAULT_TARGET_PRIORITY, settings.getTargetPriority(11));
    }

    @Test
    void scenariosWithoutPriorityTargetsStillHaveNone() throws Exception {
        assertEquals(Set.of(), parse("name: TestBot").behaviorSettings().getPriorityUnitTargets());
    }
}
