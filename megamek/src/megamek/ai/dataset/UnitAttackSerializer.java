/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Serializer and deserializer for UnitAttackAction to/from TSV format.
 * @author Luana Coppio
 */
public class UnitAttackSerializer extends TabSeparatedValueSerializer<UnitAttackAction> {

    private static final DecimalFormat LOG_DECIMAL = new DecimalFormat("0.00", new DecimalFormatSymbols(Locale.US));

    private enum UnitAttackField {
        ROUND,
        ENTITY_ID,
        PLAYER_ID,
        TYPE,
        ROLE,
        X,
        Y,
        FACING,
        TARGET_PLAYER_ID,
        TARGET_ID,
        TARGET_TYPE,
        TARGET_ROLE,
        TARGET_X,
        TARGET_Y,
        TARGET_FACING,
        AIMING_LOC,
        AIMING_MODE,
        WEAPON_ID,
        AMMO_ID,
        ATA,
        ATG,
        GTG,
        GTA,
        TO_HIT,
        TURNS_TO_HIT,
        SPOTTER_ID,
        ;

        /**
         * Builds the TSV header line (joined by tabs) by iterating over all enum constants.
         */
        public static String getHeaderLine() {
            return Arrays.stream(values()).map(UnitAttackField::name).collect(Collectors.joining("\t"));
        }
    }

    @Override
    public String serialize(UnitAttackAction obj) {
        String[] row = new String[UnitAttackField.values().length];

        row[UnitAttackField.ROUND.ordinal()] = String.valueOf(obj.round());
        row[UnitAttackField.ENTITY_ID.ordinal()] = String.valueOf(obj.entityId());
        row[UnitAttackField.PLAYER_ID.ordinal()] = String.valueOf(obj.playerId());
        row[UnitAttackField.TYPE.ordinal()] = obj.type();
        row[UnitAttackField.ROLE.ordinal()] = obj.role().name();
        row[UnitAttackField.X.ordinal()] = String.valueOf(obj.x());
        row[UnitAttackField.Y.ordinal()] = String.valueOf(obj.y());
        row[UnitAttackField.FACING.ordinal()] = String.valueOf(obj.facing());

        row[UnitAttackField.TARGET_PLAYER_ID.ordinal()] = String.valueOf(obj.targetPlayerId());
        row[UnitAttackField.TARGET_ID.ordinal()] = String.valueOf(obj.targetId());
        row[UnitAttackField.TARGET_TYPE.ordinal()] = obj.targetType();
        row[UnitAttackField.TARGET_ROLE.ordinal()] = obj.targetRole().name();
        row[UnitAttackField.TARGET_X.ordinal()] = String.valueOf(obj.targetX());
        row[UnitAttackField.TARGET_Y.ordinal()] = String.valueOf(obj.targetY());
        row[UnitAttackField.TARGET_FACING.ordinal()] = String.valueOf(obj.targetFacing());

        row[UnitAttackField.AIMING_LOC.ordinal()] = String.valueOf(obj.aimingLocation());
        row[UnitAttackField.AIMING_MODE.ordinal()] = obj.aimingMode().name();
        row[UnitAttackField.WEAPON_ID.ordinal()] = String.valueOf(obj.weaponId());
        row[UnitAttackField.AMMO_ID.ordinal()] = String.valueOf(obj.ammoId());

        row[UnitAttackField.ATA.ordinal()] = obj.airToAir() ? "1" : "0";
        row[UnitAttackField.ATG.ordinal()] = obj.airToGround() ? "1" : "0";
        row[UnitAttackField.GTG.ordinal()] = obj.groundToGround() ? "1" : "0";
        row[UnitAttackField.GTA.ordinal()] = obj.groundToAir() ? "1" : "0";

        row[UnitAttackField.TO_HIT.ordinal()] = LOG_DECIMAL.format(obj.toHit());
        row[UnitAttackField.TURNS_TO_HIT.ordinal()] = String.valueOf(obj.turnsToHit());
        row[UnitAttackField.SPOTTER_ID.ordinal()] = String.valueOf(obj.spotterId());

        return String.join("\t", row);
    }

    @Override
    public String getHeaderLine() {
        return UnitAttackField.getHeaderLine();
    }
}
