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

import static java.util.stream.Collectors.joining;
import static megamek.common.alphaStrike.ASUnitType.*;

/**
 * This class contains static helper methods for AlphaStrike
 */
public class AlphaStrikeHelper {

    public static final String INCH = "\"";

    /**
     * Returns a formatted String for the standard movement capability of the given AS element, e.g. 4"/6"j. This
     * includes all movement modes of the element that are typically printed as MV on an AS card.
     * As the only exception, this does not include the a and g movement modes of LandAirMeks, which are
     * printed as special unit abilities.
     *
     * @return A formatted standard movement string, e.g. 4"/6"j.
     */
    public static String getMovementAsString(ASCardDisplayable element) {
        if (element.getASUnitType().isBattleMek()) {
            return element.getMovement().entrySet().stream()
                    .filter(e -> !e.getKey().equals("a") && !e.getKey().equals("g"))
                    .map(entry -> moveString(entry.getKey(), entry.getValue(), element))
                    .collect(joining("/"));
        } else {
            return element.getMovement().entrySet().stream()
                    .map(entry -> moveString(entry.getKey(), entry.getValue(), element))
                    .collect(joining("/"));
        }
    }

    /**
     * Returns the formatted String for a single movement mode of the given AS element. E.g., for the movemode
     * "j" and the moveValue 8, returns 8"j.
     *
     * @param element The AS element (or MechSummary) having the movement mode
     * @param moveMode The movement mode string, such as "" or "j" or "a"
     * @param moveValue The movement value, e.g. in inches for ground movement
     * @return The formatted String for a single movement mode entry, e.g. 4a or 12"j.
     */
    public static String moveString(String moveMode, int moveValue, ASCardDisplayable element) {
        if (moveMode.equals("k")) {
            return "0." + moveValue + "k";
        } else if (moveMode.equals("a")) {
            return moveValue + "a";
        } else if (moveMode.equals("p")) {
            return moveValue + "p";
        } else if (element.getASUnitType().isAnyOf(DS, WS, DA, JS, SS) && moveMode.isBlank()) {
            return moveValue + "";
        } else {
            return moveValue + INCH + moveMode;
        }
    }

    // Do not instantiate
    private AlphaStrikeHelper() { }
}