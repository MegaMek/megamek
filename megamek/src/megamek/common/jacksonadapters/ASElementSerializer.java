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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import megamek.common.ForceAssignable;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.UnitRole;
import megamek.common.alphaStrike.ASCardDisplayable;
import megamek.common.alphaStrike.AlphaStrikeHelper;

import static megamek.common.jacksonadapters.MMUReader.*;

import java.io.IOException;

/**
 * This Jackson serializer writes AlphaStrikeElements to YAML output.
 *
 * <P>When the unit is canon (found in the
 * MechSummaryCache and marked as canon), then the full name (chassis+model) is written in
 * addition to the unit type (ASElement); for deserialization, the unit is re-converted from the TW Entity.</P>
 *
 * <P>For a non-canon unit, the chassis, model and AS values are written as far as needed to reconstruct the unit
 * without the cache (this does not include TMM, Threshold and PV which can be calculated from the other
 * AS values).</P>
 *
 * <P>The pilot skill is written unless it is 4. When the skill is missing, deserialization assumes 4.</P>
 *
 * <P>In addition, any transients like damage, crits and position are written if present (2024: only partly
 * implemented).</P>
 */
public class ASElementSerializer extends StdSerializer<ASCardDisplayable> {

    static final String FULL_NAME = "fullname";
    static final String AS_TYPE = "astype";
    static final String STRUCTURE = "structure";
    static final String SQUADSIZE = "squadsize";
    static final String STRUCTUREDAMAGE = "structuredamage";
    static final String OVERHEAT = "overheat";



    //TODO: add constants
    //TODO: add comments
    //TODO: add copyrights
    //TODO: test mixture of ASE and SBF
    //TODO: load SBFunit from numbers
    //TODO: write example files

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
        MechSummary unit = MechSummaryCache.getInstance().getMech(fullName);
        boolean isCanon = (unit != null) && unit.isCanon();

        jgen.writeStartObject();
        jgen.writeStringField(TYPE, AS_ELEMENT);
        if (isCanon) {
            jgen.writeStringField(FULL_NAME, fullName);
        } else {
            jgen.writeStringField(CHASSIS, element.getFullChassis());
            jgen.writeStringField(MODEL, element.getModel());
        }
        if (element instanceof ForceAssignable && ((ForceAssignable)element).partOfForce()) {
            jgen.writeStringField(FORCE, ((ForceAssignable) element).getForceString());
        }
        if (element.getSkill() != 4) {
            jgen.writeNumberField(SKILL, element.getSkill());
        }

        if (!isCanon) {
            jgen.writeStringField(AS_TYPE, element.getASUnitType().name());
            jgen.writeNumberField(SIZE, element.getSize());
            if (element.getRole() != UnitRole.UNDETERMINED) {
                jgen.writeStringField(ROLE, element.getRole().toString());
            }

            // Remove the inch (") sign from movement to avoid escaping; this doesn't lose any information
            String movement = AlphaStrikeHelper.getMovementAsString(element);
            jgen.writeStringField(MOVE, movement.replace("\"", ""));
            jgen.writeObjectField(DAMAGE, element.getStandardDamage());
            if (element.getOV() != 0) {
                jgen.writeNumberField(OVERHEAT, element.getOV());
            }
            jgen.writeNumberField(ARMOR, element.getFullArmor());
            jgen.writeNumberField(STRUCTURE, element.getFullStructure());
            jgen.writeStringField(SPECIALS, element.getSpecialAbilities().getSpecialsDisplayString(element));
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
            //TODO Quirks? AS quirks arent implemented
        }

        jgen.writeEndObject();
    }
}
