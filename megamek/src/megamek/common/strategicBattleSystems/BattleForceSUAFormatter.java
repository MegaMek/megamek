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

import megamek.common.alphaStrike.ASSpecialAbilityCollector;
import megamek.common.alphaStrike.BattleForceSUA;

/**
 * This interface is implemented by classes that have knowledge on which BattleForce Special Abilities
 * like SRCH, IT75, FLK2/2/0/0 or CK12D4 they need to display for themselves and how to format them. This is used
 * to control formatting of the specials string. For example, when an implementing object is a WS or DA, it
 * will write the doors for its transport abilities, but as a ground unit it will not. Whenever
 * a {@link ASSpecialAbilityCollector} is called to write its special abilities using
 * {@link ASSpecialAbilityCollector#getSpecialsDisplayString(String, BattleForceSUAFormatter)}, it requires a
 * BattleForceSUAFormatter to supply the formatting information.
 */
public interface BattleForceSUAFormatter {

    /**
     * Returns true when this object wants to have the given SUA displayed among its special abilities. This method
     * defaults to returning true and must be overridden when some SUAs are not to displayed.
     *
     * @return True when this object wants to have the given sua displayed among its special abilities.
     */
    default boolean showSUA(BattleForceSUA sua) {
        return true;
    }

    /**
     * Returns the formatted SUA string for the given sua. The given collection can be the specials of
     * the AlphaStrikeElement itself, a turret or an arc of a large aerospace unit. It is required to access
     * related SUAs such as the doors for a transport ability like MT.
     *
     * @param sua The Special Unit Ability to process
     * @param delimiter The delimiter to insert between entries (only relevant for TUR)
     * @param collection The SUA collection that the given SUA is part of
     * @return The complete formatted Special Unit Ability string such as "LRM1/1/-" or "CK15D2".
     */
    String formatSUA(BattleForceSUA sua, String delimiter, ASSpecialAbilityCollector collection);
}
