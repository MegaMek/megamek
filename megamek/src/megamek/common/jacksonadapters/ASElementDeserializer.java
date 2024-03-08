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

public class ASElementDeserializer extends StdDeserializer<AlphaStrikeElement> {

    private static final List<String> movementModes = List.of("qt", "qw", "t", "w",
            "h", "v", "n", "s", "m", "j", "f", "g", "a", "p");

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

        AlphaStrikeElement element = null;
        int skill = 4;
        if (node.has(SKILL)) {
            skill = (Integer) node.get("skill").numberValue();
        }

        if (node.has(FULL_NAME)) {
            // This is a canon unit and can be converted
            String fullName = node.get(FULL_NAME).textValue();
            MechSummary unit = MechSummaryCache.getInstance().getMech(fullName);
            try {
                if (unit != null) {
                    Entity entity = new MechFileParser(unit.getSourceFile(), unit.getEntryName()).getEntity();
                    element = ASConverter.convert(entity);
                    element.setSkill(skill);
                }
            } catch (EntityLoadingException e) {
                throw new IOException(e);
            }
        } else {
            // This is a non-canon unit; its values are set from the YAML info
            element = new AlphaStrikeElement();
            element.setChassis(node.get(CHASSIS).textValue());
            element.setModel(node.get(MODEL).textValue());
            element.setSkill(skill);
            element.setSize((Integer) node.get(SIZE).numberValue());
            element.setFullArmor((Integer) node.get(ARMOR).numberValue());
            element.setCurrentArmor(element.getFullArmor());
            element.setFullStructure((Integer) node.get(STRUCTURE).numberValue());
            element.setCurrentStructure(element.getFullStructure());
            element.setType(ASUnitType.valueOf(node.get(AS_TYPE).textValue()));
            element.setOverheat(node.has(OVERHEAT) ? (Integer) node.get(OVERHEAT).numberValue() : 0);
            element.setStandardDamage(ASDamageVector.parse(node.get(DAMAGE).textValue()));
            element.setMovement(parseMovement(node));
            element.setPrimaryMovementMode(parsePrimaryMoveMode(node));

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

                Map<BattleForceSUA, Object> noTurretSpecials = parseSpecials(noTurret);
                for (Map.Entry<BattleForceSUA, Object> entry : noTurretSpecials.entrySet()) {
                    element.getSpecialAbilities().replaceSUA(entry.getKey(), entry.getValue());
                }
                if (!turret.isBlank()) {
                    ASTurretSummary turretSummary = element.getTUR();
                    Map<BattleForceSUA, Object> turretSpecials = parseSpecials(turret);
                    for (Map.Entry<BattleForceSUA, Object> entry : turretSpecials.entrySet()) {
                        turretSummary.replaceSUA(entry.getKey(), entry.getValue());
                    }
                    element.getSpecialAbilities().replaceSUA(BattleForceSUA.TUR, turretSummary);
                }
            }

            if (node.has(FORCE)) {
                element.setForceString(node.get(FORCE).textValue());
            }

            if (node.has(ROLE)) {
                element.setRole(UnitRole.valueOf(node.get(ROLE).textValue()));
            }

            if (element.isBattleArmor() && node.has(SQUADSIZE)) {
                element.setSquadSize((Integer) node.get(SQUADSIZE).numberValue());
            }
        }
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
    private String parsePrimaryMoveMode(JsonNode node) {
        String movement = node.get(MOVE).textValue();
        String[] modes = movement.split("/");
        if (modes.length > 0) {
            for (String mode : movementModes) {
                if (modes[0].endsWith(mode)) {
                    return mode;
                }
            }
        }
        return "";
    }

    private Map<String, Integer> parseMovement(JsonNode node) {
        Map<String, Integer> moves = new HashMap<>();
        String movement = node.get(MOVE).textValue();
        String[] modes = movement.split("/");
        for (String modeText : modes) {
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
        return moves;
    }

    private Map<BattleForceSUA, Object> parseSpecials(String specials) {
        if (specials.isBlank()) {
            return Collections.emptyMap();
        }
        Map<BattleForceSUA, Object> result = new HashMap<>();
        String[] separatedSpecials = specials.split(",");
        for (String special : separatedSpecials) {
            if (ASDamageVector.canParse(special)) {
                // A lone damage vector must be the standard damage in a turret
                result.put(BattleForceSUA.STD, ASDamageVector.parse(special));
                continue;
            }
            BattleForceSUA sua = BattleForceSUA.parse(special);
            String remainder = special.replace(sua.name(), "");
            Object suaObject = parseSUAObject(remainder);
            if (sua == BattleForceSUA.IF && suaObject instanceof Integer) {
                // Integer and ASDamage cannot be separated automatically except with 0*; IF requires an ASDamage
                suaObject = new ASDamage((Integer) suaObject, false);
            }
            result.put(sua, suaObject);
        }
        result.remove(BattleForceSUA.UNKNOWN);
        return result;
    }

    private Object parseSUAObject(String asText) {
        if (asText.isBlank()) {
            return null;
        } else if (asText.contains("/")) {
            return ASDamageVector.parse(asText);
        } else if (asText.contains(".")) {
            return Double.parseDouble(asText);
        } else if (asText.contains("*")) {
            return ASDamage.parse(asText);
        } else {
            return Integer.parseInt(asText);
        }
    }
}