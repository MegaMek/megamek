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

package megamek.common.jacksonAdapters;

import static megamek.common.jacksonAdapters.MMUReader.*;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import megamek.common.alphaStrike.ASArcSummary;
import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.AlphaStrikeHelper;
import megamek.common.interfaces.ForceAssignable;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.Entity;
import megamek.common.units.UnitRole;

/**
 * This Jackson serializer writes AlphaStrikeElements to YAML output.
 * <p>
 * When the unit is canon (found in the MekSummaryCache and marked as canon), then the full name (chassis+model) is
 * written in addition to the unit type (ASElement); for deserialization, the unit is re-converted from the TW Entity.
 * <p>
 * For a non-canon unit, the chassis, model and AS values are written as far as needed to reconstruct the unit without
 * the cache (this does not include TMM, Threshold and PV which can be calculated from the other AS values).
 * <p>
 * The pilot skill is written unless it is 4. When the skill is missing, deserialization assumes 4.
 * <p>
 * In addition, any transients like damage are written if present (2024: only partially implemented).
 */
public class ASElementSerializer extends StdSerializer<ASCardDisplayable> {

    static final String FULL_NAME = "fullname";
    static final String AS_TYPE = "astype";
    static final String STRUCTURE = "structure";
    static final String SQUAD_SIZE = "squadsize";
    static final String STRUCTURE_DAMAGE = "structuredamage";
    static final String OVERHEAT = "overheat";
    static final String NOSE_ARC = "nose";
    static final String AFT_ARC = "aft";
    static final String SIDE_ARC = "side";

    public ASElementSerializer() {
        this(null);
    }

    public ASElementSerializer(Class<ASCardDisplayable> t) {
        super(t);
    }

    @Override
    public void serialize(ASCardDisplayable element, JsonGenerator jsonGenerator, SerializerProvider provider)
          throws IOException {

        String fullName = (element.getFullChassis() + " " + element.getModel()).trim();
        MekSummary unit = MekSummaryCache.getInstance().getMek(fullName);
        boolean writeCacheLink = (unit != null) && unit.isCanon();
        writeCacheLink &= !MMUWriter.Views.FullStats.class.equals(provider.getActiveView());

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(TYPE, AS_ELEMENT);
        if (element instanceof AlphaStrikeElement && ((AlphaStrikeElement) element).getId() != Entity.NONE) {
            jsonGenerator.writeNumberField(ID, ((AlphaStrikeElement) element).getId());
        }
        if (writeCacheLink) {
            jsonGenerator.writeStringField(FULL_NAME, fullName);
        } else {
            jsonGenerator.writeStringField(CHASSIS, element.getFullChassis());
            if (!element.getModel().isBlank()) {
                jsonGenerator.writeStringField(MODEL, element.getModel());
            }
        }
        if (element instanceof ForceAssignable && ((ForceAssignable) element).partOfForce()) {
            jsonGenerator.writeStringField(FORCE, ((ForceAssignable) element).getForceString());
        }
        if (element.getSkill() != 4) {
            jsonGenerator.writeNumberField(SKILL, element.getSkill());
        }

        if (!writeCacheLink) {
            jsonGenerator.writeStringField(AS_TYPE, element.getASUnitType().name());
            jsonGenerator.writeNumberField(SIZE, element.getSize());
            if (element.getRole() != UnitRole.UNDETERMINED) {
                jsonGenerator.writeStringField(ROLE, element.getRole().toString());
            }

            // Remove the inch (") sign from movement to avoid escaping; this doesn't lose any information
            // Also remove "0." from station-keeping (k) movement to simplify parsing
            String movement = AlphaStrikeHelper.getMovementAsString(element);
            jsonGenerator.writeStringField(MOVE, movement.replace("\"", "").replace("0.", ""));
            if (!element.usesArcs()) {
                if (element.getStandardDamage().hasDamage()) {
                    jsonGenerator.writeObjectField(DAMAGE, element.getStandardDamage());
                }
            } else {
                writeArc(element.getFrontArc(), jsonGenerator, element, NOSE_ARC);
                writeArc(element.getRearArc(), jsonGenerator, element, AFT_ARC);
                writeArc(element.getLeftArc(), jsonGenerator, element, SIDE_ARC);
            }
            if (element.getOV() != 0) {
                jsonGenerator.writeNumberField(OVERHEAT, element.getOV());
            }
            jsonGenerator.writeNumberField(ARMOR, element.getFullArmor());
            jsonGenerator.writeNumberField(STRUCTURE, element.getFullStructure());
            if (!element.getSpecialAbilities().getSpecialsDisplayString(element).isBlank()) {
                jsonGenerator.writeStringField(SPECIALS,
                      element.getSpecialAbilities().getSpecialsDisplayString(element));
            }
            if (element.isBattleArmor()) {
                jsonGenerator.writeNumberField(SQUAD_SIZE, element.getSquadSize());
            }

            if (element.getFullArmor() > element.getCurrentArmor()) {
                jsonGenerator.writeNumberField(ARMOR_DAMAGE, element.getFullArmor() - element.getCurrentArmor());
            }
            if (element.getFullStructure() > element.getCurrentStructure()) {
                jsonGenerator.writeNumberField(STRUCTURE_DAMAGE,
                      element.getFullStructure() - element.getCurrentStructure());
            }
            //TODO crits
            //TODO position and facing
        }
        jsonGenerator.writeEndObject();
    }

    private void writeArc(ASArcSummary arc, JsonGenerator jsonGenerator, ASCardDisplayable element, String specName)
          throws IOException {
        if (!arc.getSpecialsShortExportString(", ", element).isBlank()) {
            jsonGenerator.writeObjectField(specName, arc.getSpecialsShortExportString(", ", element));
        }
    }
}
