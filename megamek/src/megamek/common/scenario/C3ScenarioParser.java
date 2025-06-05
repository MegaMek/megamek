/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.scenario;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import megamek.common.Entity;
import megamek.common.jacksonadapters.MMUReader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is used for parsing C3 network information from MM V2 scenarios (.mms) into an intermediate ParsedC3Info
 * object.
 *
 * @see megamek.common.scenario.ScenarioV2
 */
class C3ScenarioParser {

    private static final String C3 = "c3";
    private static final String C3M = "c3m";
    private static final String CONNECTED = "connected";

    static class ParsedC3Info {

        /** The master in standard C3, Entity.NONE in other C3 networks */
        int masterId = Entity.NONE;

        /** All units except the master in standard C3, all units in other C3 networks */
        Set<Integer> participants = new HashSet<>();
    }

    /**
     * Parses C3 network info from a MM V2 scenarios (.mms) file. The info is only read, but not checked for any
     * inconsistency.
     *
     * @param node The scenario node; C3 info is top level
     *
     * @return all parsed C3 type networks of all players
     *
     * @throws JsonProcessingException When malformed info is present (no game rule checks are made)
     */
    public static List<ParsedC3Info> parse(JsonNode node) throws JsonProcessingException {
        List<ParsedC3Info> result = new ArrayList<>();
        if (node.has(C3)) {
            if (node.get(C3).isArray()) {
                node.get(C3).forEach(element -> result.add(readNetwork(element)));
            } else {
                throw new ScenarioLoaderException("C3 networks must be given as a list!");
            }
        }
        return result;
    }

    private static ParsedC3Info readNetwork(JsonNode node) {
        var network = new ParsedC3Info();
        if (node.isArray()) {
            // C3i, NC3 or Nova
            node.forEach(element -> network.participants.add(element.asInt()));
        } else {
            // Standard C3
            MMUReader.requireFields("C3", node, CONNECTED, C3M);
            network.masterId = node.get(C3M).asInt();
            node.get(CONNECTED).forEach(element -> network.participants.add(element.asInt()));
        }
        return network;
    }

    private C3ScenarioParser() { }
}
