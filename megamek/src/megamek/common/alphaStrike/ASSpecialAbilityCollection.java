/*
 *
 *  * Copyright (c) 05.07.22, 22:56 - The MegaMek Team. All Rights Reserved.
 *  *
 *  * This file is part of MegaMek.
 *  *
 *  * MegaMek is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * MegaMek is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package megamek.common.alphaStrike;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static megamek.common.alphaStrike.BattleForceSUA.*;

/**
 * This class encapsulates a block of AlphaStrike or Battleforce or SBF special abilities. Most
 * Alphastrike elements or BF or SBF formations have a single such block of special abilities which
 * may be empty or contain one or many SPAs. AlphaStrike elements that use arcs have an additional special
 * ability block per arc. Units with turret(s) have one or more such blocks as part of the ASArcSummaries
 * representing the turret(s).
 */
public class ASSpecialAbilityCollection implements Serializable {

    public static final String INCH = "\"";

    /**
     * This contains all the SPAs of this special ability block.
     *
     * This covers all SpAs, including the damage values such as SRM2/2.
     * SpAs not associated with a number, e.g. RCN, have null assigned as Object.
     * SpAs associated with a number, such as MHQ5 (but not IF2), have an Integer or Double as Object.
     * SpAs assoicated with one or more damage numbers, such as IF2 or AC2/2/-,
     * have an ASDamageVector or ASDamage as Object.
     * TUR has a List<List<Object>> wherein each List<Object> contains a
     * ASDamageVector as the first item and a Map<BattleForceSPA, ASDamageVector> as the second item.
     * This represents multiple turrets, each with a standard damage value and SpA damage values.
     * If TUR is present, none of the objects is null and the outer List must contain one item (one turret)
     * with standard damage at least.
     * BIM and LAM have a Map<String, Integer> as Object similar to the element's movement field.
     */
    private final EnumMap<BattleForceSUA, Object> specialAbilities = new EnumMap<>(BattleForceSUA.class);

    /** The AlphaStrike element this collection is part of. Used to format string output. */
    private final AlphaStrikeElement element;

    public ASSpecialAbilityCollection(AlphaStrikeElement element) {
        this.element = element;
    }

    public boolean isEmpty() {
        return specialAbilities.isEmpty();
    }

    /**
     * Returns a formatted SPA string for this AS element. The string is formatted in the way SPAs are
     * printed on an AS element's card or summary with a ', ' between SPAs.
     *
     * @return A formatted Special Unit Ability string for this AS element
     */
    public String getSpecialsString() {
        return getSpecialsString(", ");
    }

    @Override
    public String toString() {
        return getSpecialsString();
    }

    /**
     * Returns a formatted SPA string for this AS element. The given delimiter is inserted between SPAs.
     *
     * @return A formatted Special Unit Ability string for this AS element
     */
    public String getSpecialsString(String delimiter) {
        return specialAbilities.keySet().stream()
                .filter(element::showSpecial)
                .map(this::formatSUAString)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(delimiter));
    }

    /**
     * @return The value associated with the given Special Unit Ability. Depending on the given spa, this
     * value can be null or of different types.
     */
    public Object getSPA(BattleForceSUA spa) {
        return specialAbilities.get(spa);
    }

    /**
     * Creates the formatted SPA string for the given spa. For turrets this includes everything in that
     * turret.
     *
     * @return The complete formatted Special Unit Ability string such as "LRM1/1/-" or "CK15D2".
     */
    public String formatSUAString(BattleForceSUA sua) {
        Object spaObject = getSPA(sua);
        if (sua == TUR) {
            return "TUR(" + spaObject + ")";
        } else if (sua == STD) {
            return spaObject + "";
        } else if (sua.isAnyOf(MSL, SCAP, CAP)) {
            return sua + "";
        } else if (sua == BIM || sua == LAM) {
            return lamString(sua, spaObject);
        } else if ((sua == C3BSS) || (sua == C3M) || (sua == C3BSM) || (sua == C3EM)
                || (sua == INARC) || (sua == CNARC) || (sua == SNARC)) {
            return sua.toString() + ((int) spaObject == 1 ? "" : (int) spaObject);
        } else if (sua.isTransport()) {
            String result = sua + spaObject.toString();
            if (element.isLargeAerospace()
                    && hasSPA(sua.getDoor()) && ((int) getSPA(sua.getDoor()) > 0)) {
                result += sua.getDoor().toString() + getSPA(sua.getDoor());
            }
            return result;
        } else {
            return sua.toString() + (spaObject != null ? spaObject : "");
        }
    }

    /** @return The formatted LAM/BIM Special Ability string such as LAM(36"g/4a). */
    private static String lamString(BattleForceSUA spa, Object spaObject) {
        String result = spa.toString() + "(";
        if (spa == LAM) {
            result += ((Map<String, Integer>)spaObject).get("g") + INCH + "g/";
        }
        result += ((Map<String, Integer>)spaObject).get("a") + "a)";
        return result;
    }

    /**
     * NEW version - Adds a Special Unit Ability that is not associated with any
     * additional information or number, e.g. RCN.
     */
    public void addSPA(BattleForceSUA spa) {
        specialAbilities.put(spa, null);
    }

    /**
     * NEW version - Adds a Special Unit Ability associated with an integer number such as C3M#. If
     * that SPA is already present, the given number is added to the one already present. If the present
     * number is a Double type value, that type is preserved.
     */
    public void addSPA(BattleForceSUA spa, int intAbilityValue) {
        if (!specialAbilities.containsKey(spa)) {
            specialAbilities.put(spa, intAbilityValue);
        } else {
            if (specialAbilities.get(spa) instanceof Integer) {
                specialAbilities.put(spa, (int) specialAbilities.get(spa) + intAbilityValue);
            } else if (specialAbilities.get(spa) instanceof Double) {
                specialAbilities.put(spa, (double) specialAbilities.get(spa) + intAbilityValue);
            }
        }
    }

    /**
     * NEW version - Adds a Special Unit Ability associated with a possibly non-integer number such
     * as MHQ2. If that SPA is already present, the given number is added to the one already present.
     * if the previosly present number was an integer, it will be converted to a Double type value.
     */
    public void addSPA(BattleForceSUA spa, double doubleValue) {
        if (!specialAbilities.containsKey(spa)) {
            specialAbilities.put(spa, doubleValue);
        } else {
            if (specialAbilities.get(spa) instanceof Integer) {
                specialAbilities.put(spa, (int) specialAbilities.get(spa) + doubleValue);
            } else if (specialAbilities.get(spa) instanceof Double) {
                specialAbilities.put(spa, (double) specialAbilities.get(spa) + doubleValue);
            }
        }
    }

    /**
     * NEW version - Replaces the value associated with a Special Unit Ability with the given Object.
     * The previously present associated Object, if any, is discarded. If the ability was not present,
     * it is added.
     */
    public void replaceSPA(BattleForceSUA spa, Object newValue) {
        specialAbilities.put(spa, newValue);
    }

    /**
     * NEW version - Adds a Special Unit Ability associated with a single damage value such as IF2. If
     * that SPA is already present, the new damage value replaces the former.
     */
    public void addSPA(BattleForceSUA spa, ASDamage damage) {
        specialAbilities.put(spa, damage);
    }

    /**
     * NEW version - Adds a Special Unit Ability associated with a full damage vector such as LRM1/2/2. If
     * that SPA is already present, the new damage value replaces the former.
     */
    public void addSPA(BattleForceSUA spa, ASDamageVector damage) {
        specialAbilities.put(spa, damage);
    }

    /**
     * NEW version - Adds a Special Unit Ability associated with a whole ASArcSummary such as TUR. If
     * that SPA is already present, the new value replaces the former.
     */
    public void addSPA(BattleForceSUA spa, ASArcSummary value) {
        specialAbilities.put(spa, value);
    }

    /** NEW version - Adds the LAM Special Unit Ability with a LAM movement map. */
    public void addLamSPA(Map<String, Integer> specialMoves) {
        specialAbilities.put(LAM, specialMoves);
    }

    /** NEW version - Adds the BIM Special Unit Ability with a LAM movement map. */
    public void addBimSPA(Map<String, Integer> specialMoves) {
        specialAbilities.put(BIM, specialMoves);
    }

    /**
     * @return True when this AS element has the given Special Unit Ability. When the element has
     * the spa and the spa is associated with some value, this value can be assumed to be non-empty
     * or greater than zero. E.g., if an element has the MHQ spa, then MHQ >= 1. If it has IF, then
     * the IF value is at least 0*.
     */
    public boolean hasSPA(BattleForceSUA spa) {
        return specialAbilities.containsKey(spa);
    }

    public void removeSPA(BattleForceSUA spa) {
        specialAbilities.remove(spa);
    }
}
