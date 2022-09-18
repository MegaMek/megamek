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

package megamek.common.strategicBattleSystems;

import megamek.common.alphaStrike.ASSpecialAbilityCollection;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.annotations.Nullable;

import java.util.stream.Collectors;

import static megamek.common.alphaStrike.BattleForceSUA.*;

public class SBFSpecialAbilityCollection extends ASSpecialAbilityCollection {

    public String getSpecialsDisplayString(String delimiter, SBFUnit sbfUnit) {
        return specialAbilities.keySet().stream()
                .filter(sua -> sbfUnit.showSpecial(sua))
                .map(sua -> formatAbility(sua, this, sbfUnit, delimiter))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(delimiter));
    }

    /**
     * Creates the formatted SPA string for the given spa. For turrets this includes everything in that
     * turret. The given collection can be the specials of the AlphaStrikeElement itself, a turret or
     * an arc of a large aerospace unit.
     *
     * @param sua The Special Unit Ability to process
     * @param collection The SUA collection that the SUA is part of
     * @param element The AlphaStrikeElement that the collection is part of
     * @param delimiter The delimiter to insert between entries (only relevant for TUR)
     * @return The complete formatted Special Unit Ability string such as "LRM1/1/-" or "CK15D2".
     */
    public String formatAbility(BattleForceSUA sua, ASSpecialAbilityCollection collection,
                                       @Nullable SBFUnit element, String delimiter) {
        Object suaObject = collection.getSUA(sua);
        if (!sua.isValidAbilityObject(suaObject)) {
            return "ERROR - wrong ability object (" + sua + ")";
        }
        if (sua == TUR) {
            return "";
//            return "TUR(" + collection.getTUR().getSpecialsDisplayString(delimiter, element) + ")";
//        } else if (sua == BIM) {
//            return lamString(sua, collection.getBIM());
//        } else if (sua == LAM) {
//            return lamString(sua, collection.getLAM());
        } else if (sua.isAnyOf(C3BSS, C3M, C3BSM, C3EM, INARC, CNARC, SNARC)) {
            return sua.toString() + ((int) suaObject == 1 ? "" : (int) suaObject);
        } else if (sua.isAnyOf(CAP, SCAP, MSL)) {
            return sua.toString();
        } else if (sua.isTransport()) {
            String result = sua + suaObject.toString();
            BattleForceSUA door = sua.getDoor();
            if ((element == null || element.isType(SBFElementType.LA))
                    && collection.hasSUA(door) && ((int) collection.getSUA(door) > 0)) {
                result += door.toString() + collection.getSUA(door);
            }
            return result;
        } else {
            return sua.toString() + (suaObject != null ? suaObject : "");
        }
    }


}
