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

import megamek.common.annotations.Nullable;

import java.util.Map;

import static java.util.stream.Collectors.joining;
import static megamek.common.alphaStrike.ASUnitType.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

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
    public static String formatAbility(BattleForceSUA sua, ASSpecialAbilityCollection collection,
                                       @Nullable ASCardDisplayable element, String delimiter) {
        Object suaObject = collection.getSUA(sua);
        if (!sua.isValidAbilityObject(suaObject)) {
            return "ERROR - wrong ability object (" + sua + ")";
        }
        if (sua == TUR) {
            return "TUR(" + collection.getTUR().getSpecialsDisplayString(delimiter, element) + ")";
        } else if (sua == BIM) {
            return lamString(sua, collection.getBIM());
        } else if (sua == LAM) {
            return lamString(sua, collection.getLAM());
        } else if (sua.isAnyOf(C3BSS, C3M, C3BSM, C3EM, INARC, CNARC, SNARC)) {
            return sua.toString() + ((int) suaObject == 1 ? "" : (int) suaObject);
        } else if (sua.isAnyOf(CAP, SCAP, MSL)) {
            return sua.toString();
        } else if (sua.isTransport()) {
            String result = sua.toString() + suaObject;
            BattleForceSUA door = sua.getDoor();
            if ((element == null || element.isLargeAerospace())
                    && collection.hasSUA(door) && ((int) collection.getSUA(door) > 0)) {
                result += door.toString() + collection.getSUA(door);
            }
            return result;
        } else {
            return sua.toString() + (suaObject != null ? suaObject : "");
        }
    }

    /** @return The formatted LAM/BIM Special Ability string such as LAM(36"g/4a). */
    private static String lamString(BattleForceSUA sua, Map<String, Integer> suaObject) {
        StringBuilder result = new StringBuilder(sua.toString());
        result.append("(");
        if (sua == LAM) {
            result.append(suaObject.get("g")).append(INCH).append("g/");
        }
        return result.append(suaObject.get("a")).append("a)").toString();
    }

    /**
     * Returns true if the given Special Unit Ability should be shown on this AS element's card or summary.
     * This is usually true but false for some, e.g. BM automatically have SOA and do not need to
     * show this on the unit card.
     *
     * @param sua The Special Unit Ability to check
     * @return True when the given Special Unit Ability should be listed on the element's card
     */
    public static boolean hideSpecial(BattleForceSUA sua, ASCardDisplayable element) {
        return sua.isDoor()
                || (element.isLargeAerospace() && (sua == STD))
                || (element.usesCapitalWeapons() && sua.isAnyOf(MSL, SCAP, CAP))
                || (element.isType(BM, PM) && (sua == SOA))
                || (element.isBattleMek() && (sua == SRCH))
                || (!element.isLargeAerospace() && sua.isDoor())
                || (hasAutoSeal(element) && (sua == SEAL));
    }

    /** @return True when this AS element automatically gets the SEAL Special Unit Ability. */
    public static boolean hasAutoSeal(ASCardDisplayable element) {
        return element.isSubmarine()
                || element.isType(BM, AF, SC, DS, JS, WS, SS, DA);
        // TODO               || isType(BA) Exoskeleton??
    }

    /** @return A string containing the special abilities of the unit for export (including arcs on large aero units). */
    public static String getSpecialsExportString(String delimiter, ASCardDisplayable element) {
        StringBuilder dataLine = new StringBuilder();
        if (element.usesArcs()) {
            dataLine.append(element.getSpecialAbilities().getSpecialsDisplayString(delimiter, element)).append(delimiter);
            dataLine.append("FRONT(").append(element.getFrontArc().getSpecialsExportString(delimiter, element)).append(")").append(delimiter);
            dataLine.append("LEFT(").append(element.getLeftArc().getSpecialsExportString(delimiter, element)).append(")").append(delimiter);
            dataLine.append("RIGHT(").append(element.getRightArc().getSpecialsExportString(delimiter, element)).append(")").append(delimiter);
            dataLine.append("REAR(").append(element.getRearArc().getSpecialsExportString(delimiter, element)).append(")");
        } else {
            dataLine.append(element.getSpecialAbilities().getSpecialsDisplayString(delimiter, element));
        }
        return dataLine.toString();
    }

    // Do not instantiate
    private AlphaStrikeHelper() { }
}