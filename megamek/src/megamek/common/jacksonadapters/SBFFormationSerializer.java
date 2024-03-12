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

import static megamek.common.jacksonadapters.MMUReader.*;
import static megamek.common.jacksonadapters.MMUReader.SPECIALS;
import static megamek.common.jacksonadapters.SBFUnitSerializer.*;

/**
 * This Jackson serializer writes an SBF Formation to YAML output. Since the base values can be calculated from the
 * stats of the SBF formations, only the formations are written.
 *
 * <P>In addition, any transients like damage, crits and position are written if present (2024: only partly
 * implemented).</P>
 */
public class SBFFormationSerializer extends StdSerializer<SBFFormation> {

    static final String UNITS = "units";
    static final String TACTICS = "tactics";
    static final String MORALE = "morale";

    public SBFFormationSerializer() {
        this(null);
    }

    public SBFFormationSerializer(Class<SBFFormation> t) {
        super(t);
    }

    @Override
    public void serialize(SBFFormation formation, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {

        boolean writeFullStats = MMUWriter.Views.FullStats.class.equals(provider.getActiveView());
        jgen.writeStartObject();
        jgen.writeStringField(MMUReader.TYPE, MMUReader.SBF_FORMATION);
        jgen.writeStringField(MMUReader.GENERAL_NAME, formation.getName());
        if (!formation.specificName().isBlank()) {
            jgen.writeStringField(MMUReader.SPECIFIC_NAME, formation.specificName());
        }
        if (writeFullStats) {
            if (formation.getSkill() != 4) {
                jgen.writeNumberField(SKILL, formation.getSkill());
            }
            jgen.writeStringField(SBF_TYPE, formation.getType().name());
            jgen.writeNumberField(SIZE, formation.getSize());
            jgen.writeNumberField(TMM, formation.getTmm());
            // Separating move and mode because the move code is ambiguous and it is unclear to me
            // if the rules require the exact mode to be known, e.g. if SUBMARINE, MEK_UMU or BA_UMU must be distinct
            jgen.writeNumberField(MOVE, formation.getMovement());
            jgen.writeObjectField(MOVE_MODE, formation.getMovementMode());
            if (formation.getMovement() != formation.getTrspMovement()) {
                jgen.writeNumberField(TRSP_MOVE, formation.getTrspMovement());
            }
            if (formation.getTrspMovementMode() != formation.getTrspMovementMode()) {
                jgen.writeObjectField(TRSP_MOVE_MODE, formation.getTrspMovementMode());
            }
            if (formation.getJumpMove() != 0) {
                jgen.writeNumberField(JUMP, formation.getJumpMove());
            }
            jgen.writeNumberField(TACTICS, formation.getTactics());
            jgen.writeNumberField(MORALE, formation.getMorale());
            if (!formation.getSpecialAbilities().getSpecialsDisplayString(formation).isBlank()) {
                jgen.writeStringField(SPECIALS, formation.getSpecialAbilities().getSpecialsDisplayString(formation));
            }
            jgen.writeNumberField(PV, formation.getPointValue());
        }
        provider.defaultSerializeField(UNITS, formation.getUnits(), jgen);

        //TODO damage
        jgen.writeEndObject();
    }
}

