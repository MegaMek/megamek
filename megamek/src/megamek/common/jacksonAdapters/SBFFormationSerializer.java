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

import static megamek.common.jacksonAdapters.MMUReader.ID;
import static megamek.common.jacksonAdapters.MMUReader.JUMP;
import static megamek.common.jacksonAdapters.MMUReader.MOVE;
import static megamek.common.jacksonAdapters.MMUReader.MOVE_MODE;
import static megamek.common.jacksonAdapters.MMUReader.SIZE;
import static megamek.common.jacksonAdapters.MMUReader.SKILL;
import static megamek.common.jacksonAdapters.MMUReader.SPECIALS;
import static megamek.common.jacksonAdapters.MMUReader.TRSP_MOVE;
import static megamek.common.jacksonAdapters.MMUReader.TRSP_MOVE_MODE;
import static megamek.common.jacksonAdapters.SBFUnitSerializer.PV;
import static megamek.common.jacksonAdapters.SBFUnitSerializer.SBF_TYPE;
import static megamek.common.jacksonAdapters.SBFUnitSerializer.TMM;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.units.Entity;

/**
 * This Jackson serializer writes an SBF Formation to YAML output. Since the base values can be calculated from the
 * stats of the SBF units, only the units are written unless the "FullStats" View is used (see {@link MMUWriter}).
 * <p>
 * In addition, any transients like damage are written if present (2024: only partially implemented).
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
    public void serialize(SBFFormation formation, JsonGenerator generator, SerializerProvider provider)
          throws IOException {

        boolean writeFullStats = MMUWriter.Views.FullStats.class.equals(provider.getActiveView());
        generator.writeStartObject();
        generator.writeStringField(MMUReader.TYPE, MMUReader.SBF_FORMATION);
        generator.writeStringField(MMUReader.GENERAL_NAME, formation.getName());
        if (!formation.specificName().isBlank()) {
            generator.writeStringField(MMUReader.SPECIFIC_NAME, formation.specificName());
        }
        if (formation.getId() != Entity.NONE) {
            generator.writeNumberField(ID, formation.getId());
        }
        if (writeFullStats) {
            if (formation.getSkill() != 4) {
                generator.writeNumberField(SKILL, formation.getSkill());
            }
            generator.writeStringField(SBF_TYPE, formation.getType().name());
            generator.writeNumberField(SIZE, formation.getSize());
            generator.writeNumberField(TMM, formation.getTmm());
            // Separating move and mode because the move code is ambiguous, and it is unclear to me
            // if the rules require the exact mode to be known, e.g. if SUBMARINE, MEK_UMU or BA_UMU must be distinct
            generator.writeNumberField(MOVE, formation.getMovement());
            generator.writeObjectField(MOVE_MODE, formation.getMovementMode());
            if (formation.getMovement() != formation.getTrspMovement()) {
                generator.writeNumberField(TRSP_MOVE, formation.getTrspMovement());
            }
            if (formation.getMovementMode() != formation.getTrspMovementMode()) {
                generator.writeObjectField(TRSP_MOVE_MODE, formation.getTrspMovementMode());
            }
            if (formation.getJumpMove() != 0) {
                generator.writeNumberField(JUMP, formation.getJumpMove());
            }
            generator.writeNumberField(TACTICS, formation.getTactics());
            generator.writeNumberField(MORALE, formation.getMorale());
            if (!formation.getSpecialAbilities().getSpecialsDisplayString(formation).isBlank()) {
                generator.writeStringField(SPECIALS,
                      formation.getSpecialAbilities().getSpecialsDisplayString(formation));
            }
            generator.writeNumberField(PV, formation.getPointValue());
        }
        provider.defaultSerializeField(UNITS, formation.getUnits(), generator);

        //TODO damage
        generator.writeEndObject();
    }
}

