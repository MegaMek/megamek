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
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.icons.Portrait;

import java.io.File;

/**
 * This class parses the Crew from a YAML (scenario) file. This requires the entity's base crew as
 * input, therefore it's not possible (or better: I don't know how) to implement it as a subclass
 * of Jackson's deserializer.
 */
public class CrewDeserializer {

    private static final String CREW = "crew";
    private static final String HITS = "hits";
    private static final String GUNNERY = "gunnery";
    private static final String PILOTING = "piloting";
    private static final String NAME = "name";
    private static final String CALLSIGN = "callsign";
    private static final String PORTRAIT = "portrait";

    public static void parseCrew(JsonNode entityNode, Entity entity) {
        if (entityNode.has(CREW)) {
            Crew crew = entity.getCrew();
            if (crew == null) {
                throw new IllegalArgumentException("Entity " + entity + " has no crew; cannot parse crew keyword");
            }
            JsonNode crewNode = entityNode.get(CREW);
            assignHits(crew, crewNode);
            assignGunnery(crew, crewNode);
            assignPiloting(crew, crewNode);
            assignName(crew, crewNode);
            assignCallsign(crew, crewNode);
            assignPortrait(crew, crewNode);
        }
    }

    private static void assignHits(Crew crew, JsonNode crewNode) {
        if (crewNode.has(HITS)) {
            int hits = crewNode.get(HITS).asInt();
            if (hits < 0 || hits > 6) {
                throw new IllegalArgumentException("Invalid hits value " + hits);
            }
            crew.setHits(hits, 0);
        }
    }

    private static void assignGunnery(Crew crew, JsonNode crewNode) {
        if (crewNode.has(GUNNERY)) {
            int gunnery = crewNode.get(GUNNERY).asInt();
            if (gunnery < 0 || gunnery > 8) {
                throw new IllegalArgumentException("Invalid gunnery value " + gunnery);
            }
            crew.setGunnery(gunnery, 0);
        }
    }

    private static void assignPiloting(Crew crew, JsonNode crewNode) {
        if (crewNode.has(PILOTING)) {
            int piloting = crewNode.get(PILOTING).asInt();
            if (piloting < 0 || piloting > 8) {
                throw new IllegalArgumentException("Invalid piloting value " + piloting);
            }
            crew.setPiloting(piloting, 0);
        }
    }

    private static void assignName(Crew crew, JsonNode crewNode) {
        if (crewNode.has(NAME)) {
            crew.setName(crewNode.get(NAME).textValue(), 0);
        }
    }

    private static void assignCallsign(Crew crew, JsonNode crewNode) {
        if (crewNode.has(CALLSIGN)) {
            crew.setNickname(crewNode.get(CALLSIGN).textValue(), 0);
        }
    }

    private static void assignPortrait(Crew crew, JsonNode crewNode) {
        if (crewNode.has(PORTRAIT)) {
            String portraitPath = crewNode.get(PORTRAIT).textValue();
            crew.setPortrait(new Portrait(new File(portraitPath)), 0);
        }
    }
}
