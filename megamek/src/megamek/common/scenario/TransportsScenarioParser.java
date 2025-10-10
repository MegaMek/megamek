/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import megamek.common.jacksonAdapters.MMUReader;
import megamek.common.units.Entity;

/**
 * This class is used for parsing transports information from MM V2 scenarios (.mms) into an intermediate
 * ParsedTransportsInfo object.
 *
 * @see ScenarioV2
 */
class TransportsScenarioParser {

    private static final String TRANSPORTS = "transports";

    static class ParsedTransportsInfo {

        /** The master in standard C3, Entity.NONE in other C3 networks */
        int carrierId = Entity.NONE;

        /** All units except the master in standard C3, all units in other C3 networks */
        Set<Integer> participants = new HashSet<>();

        @Override
        public String toString() {
            return "TransportsInfo: C = " + carrierId + ", P = " + participants;
        }
    }

    /**
     * Parses transport info from an MM V2 scenarios (.mms) file. The info is only read, but not checked for any
     * inconsistency.
     *
     * @param node The scenario node; transports info is top level
     *
     * @return all parsed transports of all players
     *
     * @throws JsonProcessingException When malformed info is present (no game rule checks are made)
     */
    public static List<ParsedTransportsInfo> parse(JsonNode node) throws JsonProcessingException {
        List<ParsedTransportsInfo> result = new ArrayList<>();
        if (node.has(TRANSPORTS)) {
            for (Iterator<String> it = node.get(TRANSPORTS).fieldNames(); it.hasNext(); ) {
                String carrierId = it.next();
                result.add(readTransport(node.get(TRANSPORTS), carrierId));
            }
        }
        return result;
    }

    private static ParsedTransportsInfo readTransport(JsonNode transportsNode, String carrierId) {
        var transport = new ParsedTransportsInfo();
        transport.carrierId = Integer.parseInt(carrierId);
        if (transportsNode.isArray()) {
            // multiple carried units (or single as array)
            transportsNode.get(carrierId).forEach(element -> transport.participants.add(element.asInt()));
        } else {
            // single carried unit
            transport.participants.add(transportsNode.get(carrierId).asInt());
        }
        return transport;
    }

    private TransportsScenarioParser() {}
}
