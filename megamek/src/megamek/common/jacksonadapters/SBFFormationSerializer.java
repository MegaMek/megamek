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
import megamek.common.strategicBattleSystems.SBFFormation;

import java.io.IOException;

/**
 * This Jackson serializer writes an SBF Formation to YAML output. Since the base values can be calculated from the
 * stats of the SBF Units, only the units are written.
 *
 * <P>In addition, any transients like damage, crits and position are written if present (2024: only partly
 * implemented).</P>
 */
public class SBFFormationSerializer extends StdSerializer<SBFFormation> {

    static final String UNITS = "units";

    public SBFFormationSerializer() {
        this(null);
    }

    public SBFFormationSerializer(Class<SBFFormation> t) {
        super(t);
    }

    @Override
    public void serialize(SBFFormation formation, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {

        jgen.writeStartObject();
        jgen.writeStringField(MMUReader.TYPE, MMUReader.SBF_FORMATION);
        jgen.writeStringField(MMUReader.GENERAL_NAME, formation.getName());
        if (!formation.specificName().isBlank()) {
            jgen.writeStringField(MMUReader.SPECIFIC_NAME, formation.specificName());
        }
        jgen.writeObjectField(UNITS, formation.getUnits());
        //TODO damage
        jgen.writeEndObject();
    }
}

