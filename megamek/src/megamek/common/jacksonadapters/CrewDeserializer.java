/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.jacksonadapters;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import megamek.common.Crew;
import megamek.common.Entity;
import megamek.common.icons.Portrait;

/**
 * This class parses the Crew from a YAML (scenario) file. This requires the entity's base crew as input, therefore it's
 * not possible (or better: I don't know how) to implement it as a subclass of Jackson's deserializer.
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
