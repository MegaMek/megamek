/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
 */
package megamek.ai.dataset;

import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.MekSummary;
import megamek.common.UnitRole;
import megamek.common.enums.GamePhase;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Map;

/**
 * <p>serializer and deserializer for UnitState to/from TSV format.</p>
 * @author Luana Coppio
 */
public class UnitStateSerde extends TsvSerde<UnitState> {

    private final DecimalFormat LOG_DECIMAL = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance());

    @Override
    public String toTsv(UnitState obj) {
        String[] row = new String[UnitStateField.values().length];
        row[UnitStateField.ROUND.ordinal()] = String.valueOf(obj.round());
        row[UnitStateField.PHASE.ordinal()] = obj.phase().name();
        row[UnitStateField.PLAYER_ID.ordinal()] = String.valueOf(obj.playerId());
        row[UnitStateField.ENTITY_ID.ordinal()] = String.valueOf(obj.id());
        row[UnitStateField.CHASSIS.ordinal()] = obj.chassis();
        row[UnitStateField.MODEL.ordinal()] = obj.model();
        row[UnitStateField.TYPE.ordinal()] = obj.type();
        row[UnitStateField.ROLE.ordinal()] = obj.role() == null ? UnitRole.NONE.name() : obj.role().name();
        row[UnitStateField.X.ordinal()] = String.valueOf(obj.x());
        row[UnitStateField.Y.ordinal()] = String.valueOf(obj.y());
        row[UnitStateField.FACING.ordinal()] = String.valueOf(obj.facing());
        row[UnitStateField.MP.ordinal()] = LOG_DECIMAL.format(obj.mp());
        row[UnitStateField.HEAT.ordinal()] = LOG_DECIMAL.format(obj.heat());
        row[UnitStateField.PRONE.ordinal()] = obj.prone() ? "1" : "0";
        row[UnitStateField.AIRBORNE.ordinal()] = obj.airborne() ? "1" : "0";
        row[UnitStateField.OFF_BOARD.ordinal()] = obj.offBoard() ? "1" : "0";
        row[UnitStateField.CRIPPLED.ordinal()] = obj.crippled() ? "1" : "0";
        row[UnitStateField.DESTROYED.ordinal()] = obj.destroyed() ? "1" : "0";
        row[UnitStateField.ARMOR_P.ordinal()] = LOG_DECIMAL.format(obj.armorP());
        row[UnitStateField.INTERNAL_P.ordinal()] = LOG_DECIMAL.format(obj.internalP());
        row[UnitStateField.DONE.ordinal()] = obj.done() ? "1" : "0";
        row[UnitStateField.TEAM_ID.ordinal()] = String.valueOf(obj.teamId());
        row[UnitStateField.MAX_RANGE.ordinal()] = String.valueOf(obj.maxRange());
        row[UnitStateField.TOTAL_DAMAGE.ordinal()] = String.valueOf(obj.totalDamage());
        row[UnitStateField.ARMOR.ordinal()] = LOG_DECIMAL.format(obj.armor());
        row[UnitStateField.INTERNAL.ordinal()] = LOG_DECIMAL.format(obj.internal());
        row[UnitStateField.BV.ordinal()] = LOG_DECIMAL.format(obj.bv());

        return String.join("\t", row);
    }

    @Override
    public String getHeaderLine() {
        return UnitStateField.getHeaderLine();
    }
}
