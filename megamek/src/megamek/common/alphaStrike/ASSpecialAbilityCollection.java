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
import java.util.EnumMap;
import java.util.stream.Collectors;

import static megamek.common.alphaStrike.BattleForceSUA.*;

/**
 * This class encapsulates a block of AlphaStrike or Battleforce or SBF special abilities. Most
 * Alphastrike elements or BF or SBF formations have a single such block of special abilities which
 * may be empty or contain one or many SPAs. AlphaStrike elements that use arcs have an additional special
 * ability block per arc. Units with turret(s) have one or more such blocks as part of the ASTurretSummary
 * representing the turret(s).
 */
public class ASSpecialAbilityCollection implements Serializable, ASSpecialAbilityCollector {

    /** The map holding all the Special Unit Abilities and their associated objects (or null).  */
    protected final EnumMap<BattleForceSUA, Object> specialAbilities = new EnumMap<>(BattleForceSUA.class);

    /**
     * @return True when there is no special ability present. Note that for arcs and turrets this includes
     * Standard damage which is stored as STD with an ASDamageVector, so TUR is only empty when there is
     * nothing in the turret (and the TUR ability isn't present) and an arc is only empty when it has no
     * damage at all and no abilities. For AlphaStrikeElements, the standard damage is stored separately
     * and their special abilities may be empty even though there is standard damage. Note however, that most
     * units have abilities such as SEAL or SRCH that are present but not shown on the card. E.g., the
     * special abilities of a BM element is never empty, as it has at least SRCH, SOA and SEAL.
     */
    public boolean isEmpty() {
        return specialAbilities.isEmpty();
    }

    @Override
    public String toString() {
        return specialAbilities.keySet().stream()
                .map(sua -> AlphaStrikeHelper.formatAbility(sua, this, null, ", "))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
    }

    @Override
    public String getSpecialsDisplayString(String delimiter, ASCardDisplayable element) {
        return specialAbilities.keySet().stream()
                .filter(sua -> !AlphaStrikeHelper.hideSpecial(sua, element))
                .map(sua -> AlphaStrikeHelper.formatAbility(sua, this, element, delimiter))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(delimiter));
    }

    /** @return A string formatted for export (listing the damage values of STD, SCAP, MSL and CAP for arcs). */
    public String getSpecialsExportString(String delimiter, ASCardDisplayable element) {
        if (element.usesArcs()) {
            String damage = getStdDamage() + delimiter + CAP + getCAP().toString() + delimiter + SCAP + getSCAP() + delimiter
                    + MSL + getMSL();
            String specials = specialAbilities.keySet().stream()
                    .filter(sua -> !AlphaStrikeHelper.hideSpecial(sua, element))
                    .filter(sua -> !sua.isAnyOf(STD, CAP, SCAP, MSL))
                    .map(sua -> AlphaStrikeHelper.formatAbility(sua, this, element, delimiter))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .collect(Collectors.joining(delimiter));
            return damage + (!specials.isBlank() ? delimiter + specials : "");
        } else {
            return getSpecialsDisplayString(delimiter, element);
        }
    }

    @Override
    public Object getSUA(BattleForceSUA spa) {
        return specialAbilities.get(spa);
    }

    /**
     * Adds a Special Unit Ability that is not associated with any additional information
     * or number, e.g. RCN. Has no effect when the SUA is already present.
     */
    public void setSUA(BattleForceSUA sua) {
        specialAbilities.put(sua, null);
    }

    /**
     * Adds a Special Unit Ability associated with an integer number such as C3M#. If
     * that SPA is already present, the given number is added to the one already present. If the present
     * number is a Double type value, that type is preserved.
     */
    public void mergeSUA(BattleForceSUA sua, int intAbilityValue) {
        if (!specialAbilities.containsKey(sua)) {
            specialAbilities.put(sua, intAbilityValue);
        } else {
            if (specialAbilities.get(sua) instanceof Integer) {
                specialAbilities.put(sua, (int) specialAbilities.get(sua) + intAbilityValue);
            } else if (specialAbilities.get(sua) instanceof Double) {
                specialAbilities.put(sua, (double) specialAbilities.get(sua) + intAbilityValue);
            }
        }
    }

    /**
     * Adds a Special Unit Ability associated with a possibly non-integer number such
     * as CT1.5. If that SUA is already present, the given number is added to the one already present.
     * If the previously present number was an integer, it will be converted to a Double type value.
     * If the resulting value would be 0, the SUA is removed.
     */
    public void mergeSUA(BattleForceSUA sua, double doubleValue) {
        double resultingValue = doubleValue;
        if (specialAbilities.containsKey(sua)) {
            if (specialAbilities.get(sua) instanceof Integer) {
                resultingValue += (int) specialAbilities.get(sua);
            } else if (specialAbilities.get(sua) instanceof Double) {
                resultingValue += (double) specialAbilities.get(sua);
            }
        }
        if (resultingValue <= 0) {
            specialAbilities.remove(sua);
        } else {
            if ((int) resultingValue == resultingValue) {
                specialAbilities.put(sua, (int) resultingValue);
            } else {
                specialAbilities.put(sua, resultingValue);
            }
        }
    }

    /**
     * Replaces the value associated with a Special Unit Ability with the given Object.
     * The previously associated Object, if any, is discarded. If the ability was not present,
     * it is added.
     */
    public void replaceSUA(BattleForceSUA sua, Object newValue) {
        specialAbilities.put(sua, newValue);
    }

    /**
     * Adds a Special Unit Ability associated with a single damage value such as IF2. If
     * that SUA is already present, the new damage value replaces the former.
     */
    public void setSUA(BattleForceSUA sua, ASDamage damage) {
        specialAbilities.put(sua, damage);
    }

    /**
     * Adds a Special Unit Ability associated with a full damage vector such as LRM1/2/2. If
     * that SUA is already present, the new damage value replaces the former.
     */
    public void setSUA(BattleForceSUA sua, ASDamageVector damage) {
        specialAbilities.put(sua, damage);
    }

    @Override
    public boolean hasSUA(BattleForceSUA sua) {
        return specialAbilities.containsKey(sua);
    }

    /** Removes the given Special Unit Ability if it is present. */
    public void removeSUA(BattleForceSUA sua) {
        specialAbilities.remove(sua);
    }
}
