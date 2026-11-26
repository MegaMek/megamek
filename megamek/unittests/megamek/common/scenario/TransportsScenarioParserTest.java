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
package megamek.common.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import megamek.common.scenario.TransportsScenarioParser.ParsedTransportsInfo;
import org.junit.jupiter.api.Test;

/**
 * A carrier in a V2 scenario's {@code transports:} block can list several carried units in brackets or name a
 * single one. The list form used to be read as one unit with id 0.
 */
class TransportsScenarioParserTest {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    @Test
    void listOfCarriedUnitsIsReadInFull() throws Exception {
        List<ParsedTransportsInfo> transports = parse("transports:\n  102: [ 104, 105 ]\n");

        assertEquals(1, transports.size());
        assertEquals(102, transports.getFirst().carrierId);
        assertEquals(Set.of(104, 105), transports.getFirst().carriedUnits);
    }

    @Test
    void singleCarriedUnitIsRead() throws Exception {
        List<ParsedTransportsInfo> transports = parse("transports:\n  202: 205\n");

        assertEquals(1, transports.size());
        assertEquals(202, transports.getFirst().carrierId);
        assertEquals(Set.of(205), transports.getFirst().carriedUnits);
    }

    private List<ParsedTransportsInfo> parse(String yaml) throws Exception {
        JsonNode node = YAML_MAPPER.readTree(yaml);
        return TransportsScenarioParser.parse(node);
    }
}
