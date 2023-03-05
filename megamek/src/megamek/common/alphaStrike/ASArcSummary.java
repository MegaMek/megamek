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

import megamek.common.strategicBattleSystems.BattleForceSUAFormatter;

import java.util.stream.Collectors;

import static megamek.common.alphaStrike.BattleForceSUA.*;

/**
 * This class holds the AlphaStrike information for a firing arc of any element that uses arcs
 * such as a WarShip (WS)..
 * It is a standard ASSpecialAbilityCollection except that
 * {@link #getSpecialsExportString(String, BattleForceSUAFormatter)} can be used to export the arc information
 * including the STD, CAP, SCAP and MSL damage values which are otherwise not listed among the special abilities.
 *
 * @author Simon (Juliez)
 */
public class ASArcSummary extends ASSpecialAbilityCollection {

    /** @return A string formatted for export (listing the damage values of STD, SCAP, MSL and CAP for arcs). */
    public String getSpecialsExportString(String delimiter, BattleForceSUAFormatter element) {
        String damage = getStdDamage() + delimiter + CAP + getCAP().toString() + delimiter + SCAP + getSCAP() + delimiter
                + MSL + getMSL();
        String specials = specialAbilities.keySet().stream()
                .filter(element::showSUA)
                .filter(sua -> !sua.isAnyOf(STD, CAP, SCAP, MSL))
                .map(sua -> element.formatSUA(sua, delimiter, this))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(delimiter));
        return damage + (!specials.isBlank() ? delimiter + specials : "");
    }
}