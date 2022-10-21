/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.alphaStrike;

import java.io.Serializable;
import java.util.stream.Collectors;

import static megamek.common.alphaStrike.BattleForceSUA.STD;

/**
 * This class holds the AlphaStrike information for a turret (TUR) special ability.
 * It is a standard ASSpecialAbilityCollection except that for displaying the contents,
 * it places the standard damage in front.
 *
 * @author Simon (Juliez)
 */
public class ASTurretSummary extends ASSpecialAbilityCollection implements Serializable {

    @Override
    public String getSpecialsDisplayString(String delimiter, ASCardDisplayable element) {
        String result = getStdDamage().hasDamage() ? getStdDamage() + "" : "";
        String furtherSUAs = specialAbilities.keySet().stream()
                .filter(sua -> !AlphaStrikeHelper.hideSpecial(sua, element))
                .filter(sua -> sua != STD)
                .map(sua -> AlphaStrikeHelper.formatAbility(sua, this, element, delimiter))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(delimiter));
        if (result.isBlank()) {
            return furtherSUAs;
        } else if (furtherSUAs.isBlank()) {
            return result;
        } else {
            return result + delimiter + furtherSUAs;
        }
    }
}