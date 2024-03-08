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

public class SBFUnitSerializer extends StdSerializer<SBFUnit> {

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
        jgen.writeStringField(MMUReader.TYPE, MMUReader.SBF_UNIT);
        jgen.writeStringField("name", unit.getName());

        if (hasElements) {
            jgen.writeObjectField("elements", unit.getElements());
        } else {
            if (unit.getSkill() != 4) {
                jgen.writeNumberField("skill", unit.getSkill());
            }
            jgen.writeStringField("type", unit.getType().name());
            jgen.writeNumberField("size", unit.getSize());
            jgen.writeNumberField("tmm", unit.getTmm());
            jgen.writeStringField("mv", unit.getMovement() + unit.getMovementCode());
            jgen.writeNumberField("jump", unit.getJumpMove());
            jgen.writeStringField("trspmv", unit.getTrspMovement() + unit.getTrspMovementCode());
            jgen.writeObjectField("damage", unit.getDamage());
            jgen.writeNumberField("armor", unit.getArmor());
            jgen.writeStringField("spec", unit.getSpecialAbilities().getSpecialsDisplayString(unit));
            jgen.writeNumberField("pv", unit.getPointValue());

            //TODO damage
        }

        jgen.writeEndObject();
    }
}
