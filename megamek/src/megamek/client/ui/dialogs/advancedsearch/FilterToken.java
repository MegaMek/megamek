/*
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.advancedsearch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import megamek.common.equipment.EquipmentType;

/**
 * Marker interface for different tokens that can be in a filter expression.
 *
 * @author Arlith
 */
interface FilterToken {

    String TOKEN_SEPARATOR = "__";
    String TOKEN_ATLEAST = ">=";

    @JsonCreator
    static FilterToken parse(String expression) {
        FilterToken token = switch (expression) {
            case "(" -> new LeftParensFilterToken();
            case ")" -> new RightParensFilterToken();
            case "or" -> new OrFilterToken();
            case "and" -> new AndFilterToken();
            default -> null;
        };
        if (token == null) {
            String[] tokens = expression.split(TOKEN_SEPARATOR);
            boolean atLeast = tokens[2].equals(TOKEN_ATLEAST);
            int count = Integer.parseInt(tokens[1]);

            try {
                var equipmentClass = AdvancedSearchEquipmentClass.valueOf(tokens[0]);
                return new WeaponClassFT(equipmentClass, count, atLeast);
            } catch (IllegalArgumentException e) {
                // apparently not an equipment class
                String internalName = tokens[0];
                EquipmentType type = EquipmentType.get(internalName);
                return new EquipmentTypeFT(internalName, type.getName(), count, atLeast);
            }
        }
        return token;
    }

    @JsonValue
    String toJson();
}
