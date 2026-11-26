/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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

import java.io.File;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import megamek.common.icons.Portrait;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Crew;
import megamek.common.units.Entity;

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
    private static final String EDGE = "edge";
    private static final String ABILITIES = "abilities";

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
            assignAbilities(crew, crewNode);
            assignEdge(crew, crewNode);
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

    /**
     * Sets the crew's Edge points from {@code edge: 3}. Edge is only spent in play when the game option {@code edge} is
     * on.
     */
    private static void assignEdge(Crew crew, JsonNode crewNode) {
        if (crewNode.has(EDGE)) {
            JsonNode edgeNode = crewNode.get(EDGE);
            if (!edgeNode.canConvertToInt() || (edgeNode.asInt() < 0)) {
                throw new IllegalArgumentException("Invalid edge value " + edgeNode.asText());
            }
            crew.getOptions().getOption(OptionsConstants.EDGE).setValue(edgeNode.asInt());
        }
    }

    /**
     * Sets pilot options by their option name, from a list under {@code abilities:}. A bare name switches an on/off
     * ability on ({@code - dodge_maneuver}); a name with a value sets that value ({@code - weapon_specialist: Medium
     * Laser}, {@code - edge_when_tac: false}). This covers SPAs, implants and Edge triggers alike.
     *
     * @throws IllegalArgumentException for an unknown option name or a value of the wrong kind
     */
    private static void assignAbilities(Crew crew, JsonNode crewNode) {
        if (!crewNode.has(ABILITIES)) {
            return;
        }
        JsonNode abilitiesNode = crewNode.get(ABILITIES);
        if (!abilitiesNode.isArray()) {
            throw new IllegalArgumentException("abilities must be a list (use dashes)");
        }
        for (JsonNode abilityNode : abilitiesNode) {
            if (abilityNode.isTextual()) {
                IOption option = findOption(crew, abilityNode.textValue());
                if (option.getType() != IOption.BOOLEAN) {
                    throw new IllegalArgumentException("Ability " + option.getName()
                          + " needs a value, as in " + option.getName() + ": <value>");
                }
                option.setValue(true);
            } else if (abilityNode.isObject()) {
                for (Map.Entry<String, JsonNode> entry : abilityNode.properties()) {
                    setOptionValue(findOption(crew, entry.getKey()), entry.getValue());
                }
            } else {
                throw new IllegalArgumentException("Invalid ability entry " + abilityNode);
            }
        }
    }

    private static IOption findOption(Crew crew, String optionName) {
        IOption option = crew.getOptions().getOption(optionName);
        if (option == null) {
            throw new IllegalArgumentException("Unknown ability " + optionName);
        }
        return option;
    }

    private static void setOptionValue(IOption option, JsonNode valueNode) {
        switch (option.getType()) {
            case IOption.BOOLEAN -> {
                if (!valueNode.isBoolean()) {
                    throw new IllegalArgumentException("Ability " + option.getName() + " takes true or false");
                }
                option.setValue(valueNode.booleanValue());
            }
            case IOption.INTEGER -> {
                if (!valueNode.canConvertToInt()) {
                    throw new IllegalArgumentException("Ability " + option.getName() + " takes a whole number");
                }
                option.setValue(valueNode.asInt());
            }
            case IOption.STRING, IOption.CHOICE -> option.setValue(valueNode.asText());
            default -> throw new IllegalArgumentException("Ability " + option.getName() + " cannot be set here");
        }
    }
}
