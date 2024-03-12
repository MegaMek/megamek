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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import megamek.common.*;
import megamek.common.alphaStrike.*;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.loaders.EntityLoadingException;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static megamek.common.jacksonadapters.ASElementSerializer.*;
import static megamek.common.jacksonadapters.MMUReader.*;

/**
 * This Jackson deserializer reads an AlphaStrikeElement from an MMU file. When the MMU file
 * has the "fullname:" field, the unit is assumed to be canon and converted from the cache.
 * Otherwise, the MMU file must list the stats; then the element will be constructed from the stats.
 */
public class ASElementDeserializer extends StdDeserializer<AlphaStrikeElement> {

    private static final List<String> movementModes = List.of("qt", "qw", "t", "w",
            "h", "v", "n", "s", "m", "j", "f", "g", "a", "p", "k");

    public ASElementDeserializer() {
        this(null);
    }

    public ASElementDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public AlphaStrikeElement deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);
        if (!node.has(MMUReader.TYPE) || !node.get(MMUReader.TYPE).textValue().equalsIgnoreCase(AS_ELEMENT)) {
            throw new IllegalArgumentException("ASElementDeserializer: Wrong Deserializer chosen!");
        }

        AlphaStrikeElement element;
        if (node.has(FULL_NAME)) {
            // This is a canon unit and can be converted
            String fullName = node.get(FULL_NAME).textValue();
            MechSummary unit = MechSummaryCache.getInstance().getMech(fullName);
            try {
                if (unit != null) {
                    Entity entity = new MechFileParser(unit.getSourceFile(), unit.getEntryName()).getEntity();
                    element = ASConverter.convert(entity);
                } else {
                    throw new IllegalArgumentException("Could not retrieve unit " + fullName + " from cache!");
                }
            } catch (EntityLoadingException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            // This is a non-canon unit; its values are set from the YAML info
            requireFields(AS_ELEMENT, node, CHASSIS, SIZE, STRUCTURE, AS_TYPE, MOVE);
            element = new AlphaStrikeElement();
            element.setChassis(node.get(CHASSIS).textValue());
            if (node.has(MODEL)) {
                element.setModel(node.get(MODEL).textValue());
            }
            element.setName((element.getChassis() + " " + element.getModel()).trim());
            element.setSize(node.get(SIZE).intValue());
            if (node.has(ARMOR)) {
                element.setFullArmor(node.get(ARMOR).intValue());
                element.setCurrentArmor(element.getFullArmor());
            }
            element.setFullStructure(node.get(STRUCTURE).intValue());
            element.setCurrentStructure(element.getFullStructure());
            element.setType(ASUnitType.valueOf(node.get(AS_TYPE).textValue()));
            element.setOverheat(node.has(OVERHEAT) ? node.get(OVERHEAT).intValue() : 0);
            if (node.has(DAMAGE)) {
                element.setStandardDamage(ASDamageVector.parse(node.get(DAMAGE).textValue()));
            }
            parseMovement(node, element);

            if (node.has(SPECIALS)) {
                String specials = node.get(SPECIALS).textValue();
                specials = specials.replaceAll(" ", ""); // remove empty spaces
                // separate out a turret ability
                String noTurret = specials.replaceAll("[, ]*TUR\\(.*\\)", "");
                String turret = "";
                Pattern pattern = Pattern.compile("\\(.*\\)");
                Matcher matcher = pattern.matcher(specials);
                if (matcher.find()) {
                    turret = matcher.group().replaceAll("[)(]", "");
                }
                readSpecials(element.getSpecialAbilities(), noTurret);
                if (!turret.isBlank()) {
                    element.getSpecialAbilities().replaceSUA(BattleForceSUA.TUR, new ASTurretSummary());
                    readSpecials(element.getTUR(), turret);
                }
            }

            if (node.has(NOSE_ARC)) {
                readSpecials(element.getFrontArc(), node.get(NOSE_ARC).textValue());
            }
            if (node.has(AFT_ARC)) {
                readSpecials(element.getRearArc(), node.get(AFT_ARC).textValue());
            }
            if (node.has(SIDE_ARC)) {
                readSpecials(element.getLeftArc(), node.get(SIDE_ARC).textValue());
                readSpecials(element.getRightArc(), node.get(SIDE_ARC).textValue());
            }

            if (node.has(FORCE)) {
                element.setForceString(node.get(FORCE).textValue());
            }

            if (node.has(ROLE)) {
                element.setRole(UnitRole.parseRole(node.get(ROLE).textValue()));
            }

            if (element.isBattleArmor() && node.has(SQUADSIZE)) {
                element.setSquadSize((Integer) node.get(SQUADSIZE).numberValue());
            }
        }
        element.setSkill(node.has(SKILL) ? node.get(SKILL).intValue() : 4);
        ASConverter.updateCalculatedValues(element);

        // Transient values:
        //TODO: position, crits...

        if (node.has(ARMORDAMAGE)) {
            int armor = element.getFullArmor() - (Integer) node.get(ARMORDAMAGE).numberValue();
            element.setCurrentArmor(Math.max(armor, 0));
        }

        if (node.has(STRUCTUREDAMAGE)) {
            int structure = element.getFullStructure() - (Integer) node.get(STRUCTUREDAMAGE).numberValue();
            element.setCurrentStructure(Math.max(structure, 0));
        }
        return element;
    }

    /** It is assumed that the primary movement mode is the first listed mode */
    private void parseMovement(JsonNode node, AlphaStrikeElement element) {
        Map<String, Integer> moves = new HashMap<>();
        if (node.get(MOVE).isInt()) {
            moves.put("", node.get(MOVE).intValue());

        } else {
            String movement = node.get(MOVE).textValue();
            String[] parsedModes = movement.split("/");
            for (String mode : movementModes) {
                if (parsedModes[0].endsWith(mode)) {
                    element.setPrimaryMovementMode(mode);
                }
            }
            for (String modeText : parsedModes) {
                String currentMode = "";
                for (String mode : movementModes) {
                    if (modeText.endsWith(mode)) {
                        currentMode = mode;
                        modeText = modeText.replaceFirst(mode, "");
                        break;
                    }
                }
                int currentMove = Integer.parseInt(modeText);
                moves.put(currentMode, currentMove);
            }
        }
        element.setMovement(moves);
    }

    public static void readSpecials(ASSpecialAbilityCollection collection, String specials) {
        Map<BattleForceSUA, Object> parsedSpecials = parseSpecials(specials);
        for (Map.Entry<BattleForceSUA, Object> entry : parsedSpecials.entrySet()) {
            collection.replaceSUA(entry.getKey(), entry.getValue());
        }
    }

    public static Map<BattleForceSUA, Object> parseSpecials(String specials) {
        if (specials.isBlank()) {
            return Collections.emptyMap();
        }
        Map<BattleForceSUA, Object> result = new HashMap<>();
        specials = specials.replaceAll(" ", ""); // remove empty spaces
        String[] separatedSpecials = specials.split(",");
        for (String special : separatedSpecials) {
            result.putAll(BattleForceSUA.parseAlphaStrikeFull(special));
        }
        result.remove(BattleForceSUA.UNKNOWN);
        return result;
    }

}