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
import megamek.common.strategicBattleSystems.SBFUnit;

import java.io.IOException;

import static megamek.common.jacksonadapters.MMUReader.*;

/**
 * This Jackson serializer writes an SBF Unit (part of a formation) to YAML output. This can take two forms:
 * When the unit knows its underlying Alpha Strike elements, it will write the elements but not write its
 * stats as those can be calculated from the elements. If it does not know its elements (In Battle for Tukayyid,
 * there are example SBF Units that have stats without listing their ASE), the stats are written instead.
 *
 * <P>In addition, any transients like damage are written if present (2024: only partly implemented).</P>
 */
public class SBFUnitSerializer extends StdSerializer<SBFUnit> {

    static final String SBF_TYPE = "sbftype";
    static final String ELEMENTS = "elements";
    static final String PV = "pv";

    public SBFUnitSerializer() {
        this(null);
    }

    public SBFUnitSerializer(Class<SBFUnit> t) {
        super(t);
    }

    @Override
    public void serialize(SBFUnit unit, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {

        boolean hasElements = !unit.getElements().isEmpty();

        jgen.writeStartObject();
        jgen.writeStringField(TYPE, SBF_UNIT);
        jgen.writeStringField(GENERAL_NAME, unit.getName());

        if (hasElements) {
            jgen.writeObjectField(ELEMENTS, unit.getElements());
        } else {
            if (unit.getSkill() != 4) {
                jgen.writeNumberField(SKILL, unit.getSkill());
            }
            jgen.writeStringField(SBF_TYPE, unit.getType().name());
            jgen.writeNumberField(SIZE, unit.getSize());
            jgen.writeStringField(MOVE, unit.getMovement() + unit.getMovementCode());
            jgen.writeNumberField(JUMP, unit.getJumpMove());
            jgen.writeStringField(TRSP_MOVE, unit.getTrspMovement() + unit.getTrspMovementCode());
            jgen.writeObjectField(DAMAGE, unit.getDamage());
            jgen.writeNumberField(ARMOR, unit.getArmor());
            jgen.writeStringField(SPECIALS, unit.getSpecialAbilities().getSpecialsDisplayString(unit));
            jgen.writeNumberField(PV, unit.getPointValue());
        }

        //TODO damage

        jgen.writeEndObject();
    }
}
