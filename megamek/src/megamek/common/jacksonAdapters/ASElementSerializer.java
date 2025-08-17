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
import megamek.common.units.Entity;
import megamek.common.interfaces.ForceAssignable;
import megamek.common.loaders.MekSummary;
import megamek.common.loaders.MekSummaryCache;
import megamek.common.units.UnitRole;
import megamek.common.alphaStrike.ASArcSummary;
import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.AlphaStrikeHelper;

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
    static final String SQUADSIZE = "squadsize";
    static final String STRUCTUREDAMAGE = "structuredamage";
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
    public void serialize(ASCardDisplayable element, JsonGenerator jgen, SerializerProvider provider)
          throws IOException {

        String fullName = (element.getFullChassis() + " " + element.getModel()).trim();
        MekSummary unit = MekSummaryCache.getInstance().getMek(fullName);
        boolean writeCacheLink = (unit != null) && unit.isCanon();
        writeCacheLink &= !MMUWriter.Views.FullStats.class.equals(provider.getActiveView());

        jgen.writeStartObject();
        jgen.writeStringField(TYPE, AS_ELEMENT);
        if (element instanceof AlphaStrikeElement && ((AlphaStrikeElement) element).getId() != Entity.NONE) {
            jgen.writeNumberField(ID, ((AlphaStrikeElement) element).getId());
        }
        if (writeCacheLink) {
            jgen.writeStringField(FULL_NAME, fullName);
        } else {
            jgen.writeStringField(CHASSIS, element.getFullChassis());
            if (!element.getModel().isBlank()) {
                jgen.writeStringField(MODEL, element.getModel());
            }
        }
        if (element instanceof ForceAssignable && ((ForceAssignable) element).partOfForce()) {
            jgen.writeStringField(FORCE, ((ForceAssignable) element).getForceString());
        }
        if (element.getSkill() != 4) {
            jgen.writeNumberField(SKILL, element.getSkill());
        }

        if (!writeCacheLink) {
            jgen.writeStringField(AS_TYPE, element.getASUnitType().name());
            jgen.writeNumberField(SIZE, element.getSize());
            if (element.getRole() != UnitRole.UNDETERMINED) {
                jgen.writeStringField(ROLE, element.getRole().toString());
            }

            // Remove the inch (") sign from movement to avoid escaping; this doesn't lose any information
            // Also remove "0." from station-keeping (k) movement to simplify parsing
            String movement = AlphaStrikeHelper.getMovementAsString(element);
            jgen.writeStringField(MOVE, movement.replace("\"", "").replace("0.", ""));
            if (!element.usesArcs()) {
                if (element.getStandardDamage().hasDamage()) {
                    jgen.writeObjectField(DAMAGE, element.getStandardDamage());
                }
            } else {
                writeArc(element.getFrontArc(), jgen, element, NOSE_ARC);
                writeArc(element.getRearArc(), jgen, element, AFT_ARC);
                writeArc(element.getLeftArc(), jgen, element, SIDE_ARC);
            }
            if (element.getOV() != 0) {
                jgen.writeNumberField(OVERHEAT, element.getOV());
            }
            jgen.writeNumberField(ARMOR, element.getFullArmor());
            jgen.writeNumberField(STRUCTURE, element.getFullStructure());
            if (!element.getSpecialAbilities().getSpecialsDisplayString(element).isBlank()) {
                jgen.writeStringField(SPECIALS, element.getSpecialAbilities().getSpecialsDisplayString(element));
            }
            if (element.isBattleArmor()) {
                jgen.writeNumberField(SQUADSIZE, element.getSquadSize());
            }

            if (element.getFullArmor() > element.getCurrentArmor()) {
                jgen.writeNumberField(ARMORDAMAGE, element.getFullArmor() - element.getCurrentArmor());
            }
            if (element.getFullStructure() > element.getCurrentStructure()) {
                jgen.writeNumberField(STRUCTUREDAMAGE, element.getFullStructure() - element.getCurrentStructure());
            }
            //TODO crits
            //TODO position and facing
        }
        jgen.writeEndObject();
    }

    private void writeArc(ASArcSummary arc, JsonGenerator jgen, ASCardDisplayable element, String specName)
          throws IOException {
        if (!arc.getSpecialsShortExportString(", ", element).isBlank()) {
            jgen.writeObjectField(specName, arc.getSpecialsShortExportString(", ", element));
        }
    }
}
