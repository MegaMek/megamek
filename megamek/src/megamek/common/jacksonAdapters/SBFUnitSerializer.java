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
import megamek.common.strategicBattleSystems.SBFUnit;

/**
 * This Jackson serializer writes an SBF Unit (part of a formation) to YAML output. This can take two forms: When the
 * unit knows its underlying Alpha Strike elements, it will write the elements but not write its stats as those can be
 * calculated from the elements. If it does not know its elements (In Battle for Tukayyid, there are example SBF Units
 * that have stats without listing their ASE), the stats are written instead.
 * <p>
 * In addition, any transients like damage are written if present (2024: only partially implemented).
 */
public class SBFUnitSerializer extends StdSerializer<SBFUnit> {

    static final String SBF_TYPE = "sbftype";
    static final String ELEMENTS = "elements";
    static final String PV = "pv";
    static final String TMM = "tmm";

    public SBFUnitSerializer() {
        this(null);
    }

    public SBFUnitSerializer(Class<SBFUnit> t) {
        super(t);
    }

    @Override
    public void serialize(SBFUnit unit, JsonGenerator jsonGenerator, SerializerProvider provider)
          throws IOException {

        boolean hasElements = !unit.getElements().isEmpty();
        boolean writeFullStats = MMUWriter.Views.FullStats.class.equals(provider.getActiveView());

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField(TYPE, SBF_UNIT);
        jsonGenerator.writeStringField(GENERAL_NAME, unit.getName());

        if (!hasElements || writeFullStats) {
            if (unit.getSkill() != 4) {
                jsonGenerator.writeNumberField(SKILL, unit.getSkill());
            }
            jsonGenerator.writeStringField(SBF_TYPE, unit.getType().name());
            jsonGenerator.writeNumberField(SIZE, unit.getSize());
            jsonGenerator.writeNumberField(TMM, unit.getTmm());
            // Separating move and mode because the move code is ambiguous, and it is unclear to me
            // if the rules require the exact mode to be known, e.g. if SUBMARINE, MEK_UMU or BA_UMU must be distinct
            jsonGenerator.writeNumberField(MOVE, unit.getMovement());
            jsonGenerator.writeObjectField(MOVE_MODE, unit.getMovementMode());
            if (unit.getMovement() != unit.getTrspMovement()) {
                jsonGenerator.writeNumberField(TRSP_MOVE, unit.getTrspMovement());
            }
            if (unit.getMovementMode() != unit.getTrspMovementMode()) {
                jsonGenerator.writeObjectField(TRSP_MOVE_MODE, unit.getTrspMovementMode());
            }
            if (unit.getJumpMove() != 0) {
                jsonGenerator.writeNumberField(JUMP, unit.getJumpMove());
            }
            if (unit.getDamage().hasDamage()) {
                jsonGenerator.writeObjectField(DAMAGE, unit.getDamage());
            }
            jsonGenerator.writeNumberField(ARMOR, unit.getArmor());
            jsonGenerator.writeStringField(SPECIALS, unit.getSpecialAbilities().getSpecialsDisplayString(unit));
            jsonGenerator.writeNumberField(PV, unit.getPointValue());
        }

        if (hasElements) {
            provider.defaultSerializeField(ELEMENTS, unit.getElements(), jsonGenerator);
        }

        //TODO damage

        jsonGenerator.writeEndObject();
    }
}
