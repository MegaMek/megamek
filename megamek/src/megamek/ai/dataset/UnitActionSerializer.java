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

import java.util.stream.Collectors;

import megamek.common.UnitRole;

/**
 * <p>serializer and deserializer for UnitAction to/from TSV format.</p>
 * @author Luana Coppio
 */
public class UnitActionSerializer extends TabSeparatedValueSerializer<UnitAction> {

    @Override
    public String serialize(UnitAction obj) {
        String[] row = new String[UnitActionField.values().length];
        row[UnitActionField.ENTITY_ID.ordinal()] = String.valueOf(obj.id());
        row[UnitActionField.PLAYER_ID.ordinal()] = String.valueOf(obj.playerId());
        row[UnitActionField.TEAM_ID.ordinal()] = String.valueOf(obj.teamId());
        row[UnitActionField.CHASSIS.ordinal()] = obj.chassis();
        row[UnitActionField.MODEL.ordinal()] = obj.model();
        row[UnitActionField.FACING.ordinal()] = String.valueOf(obj.facing());
        row[UnitActionField.FROM_X.ordinal()] = String.valueOf(obj.fromX());
        row[UnitActionField.FROM_Y.ordinal()] = String.valueOf(obj.fromY());
        row[UnitActionField.TO_X.ordinal()] = String.valueOf(obj.toX());
        row[UnitActionField.TO_Y.ordinal()] = String.valueOf(obj.toY());
        row[UnitActionField.HEXES_MOVED.ordinal()] = String.valueOf(obj.hexesMoved());
        row[UnitActionField.DISTANCE.ordinal()] = String.valueOf(obj.distance());
        row[UnitActionField.MP_USED.ordinal()] = String.valueOf(obj.mpUsed());
        row[UnitActionField.MAX_MP.ordinal()] = String.valueOf(obj.maxMp());
        row[UnitActionField.MP_P.ordinal()] = LOG_DECIMAL.format(obj.mpP());
        row[UnitActionField.HEAT_P.ordinal()] = LOG_DECIMAL.format(obj.heatP());
        row[UnitActionField.ARMOR_P.ordinal()] = LOG_DECIMAL.format(obj.armorP());
        row[UnitActionField.INTERNAL_P.ordinal()] = LOG_DECIMAL.format(obj.internalP());
        row[UnitActionField.JUMPING.ordinal()] = obj.jumping() ? "1" : "0";
        row[UnitActionField.PRONE.ordinal()] = obj.prone() ? "1" : "0";
        row[UnitActionField.LEGAL.ordinal()] = obj.legal() ? "1" : "0";
        row[UnitActionField.CHANCE_OF_FAILURE.ordinal()] = LOG_DECIMAL.format(obj.chanceOfFailure());
        row[UnitActionField.IS_BOT.ordinal()] = obj.bot() ? "1" : "0";
        row[UnitActionField.STEPS.ordinal()] = obj.steps().stream()
                                                     .map(Enum::name)
                                                     .collect(Collectors.joining(" "));
        row[UnitActionField.HAS_ECM.ordinal()] = obj.hasEcm() ? "1" : "0";
        row[UnitActionField.ARMOR.ordinal()] = String.valueOf(obj.armor());
        row[UnitActionField.INTERNAL.ordinal()] = String.valueOf(obj.internal());
        row[UnitActionField.BV.ordinal()] = String.valueOf(obj.bv());
        row[UnitActionField.MAX_RANGE.ordinal()] = String.valueOf(obj.maxRange());
        row[UnitActionField.TOTAL_DAMAGE.ordinal()] = String.valueOf(obj.totalDamage());
        row[UnitActionField.ARMOR_FRONT_P.ordinal()] = LOG_DECIMAL.format(obj.armorFrontP());
        row[UnitActionField.ARMOR_LEFT_P.ordinal()] = LOG_DECIMAL.format(obj.armorLeftP());
        row[UnitActionField.ARMOR_RIGHT_P.ordinal()] = LOG_DECIMAL.format(obj.armorRightP());
        row[UnitActionField.ARMOR_BACK_P.ordinal()] = LOG_DECIMAL.format(obj.armorBackP());
        row[UnitStateField.ROLE.ordinal()] = obj.role() == null ? UnitRole.NONE.name() : obj.role().name();
        row[UnitActionField.WEAPON_DMG_FACING_SHORT_MEDIUM_LONG_RANGE.ordinal()] =
                obj.weaponDamageFacingShortMediumLongRange().stream()
                      .map(Object::toString)
                      .collect(Collectors.joining(" "));

        return String.join("\t", row);
    }

    @Override
    public String getHeaderLine() {
        return UnitActionField.getHeaderLine();
    }
}
