/*
  Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
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

package megamek.common.game;

import static megamek.common.compute.Compute.d6;
import static megamek.common.options.OptionsConstants.ATOW_COMBAT_PARALYSIS;
import static megamek.common.options.OptionsConstants.ATOW_COMBAT_SENSE;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

/**
 * A roll, or sequence of rolls, made by the player to determine initiative order. Also contains some methods for
 * ordering players by initiative.
 *
 * @author Ben
 * @since April 25, 2002, 12:21 PM
 */
public class InitiativeRoll implements Comparable<InitiativeRoll>, Serializable {
    @Serial
    private static final long serialVersionUID = -1850190415242027657L;

    private final Vector<Integer> rolls = new Vector<>();
    private final Vector<Integer> originalRolls = new Vector<>();
    private final Vector<Boolean> wasRollReplaced = new Vector<>();
    private final Vector<InitiativeBonusBreakdown> bonuses = new Vector<>();

    public InitiativeRoll() {

    }

    public void clear() {
        rolls.removeAllElements();
        originalRolls.removeAllElements();
        bonuses.removeAllElements();
        wasRollReplaced.removeAllElements();
    }

    /**
     * Adds a new initiative roll with a detailed bonus breakdown.
     *
     * @param breakdown             The breakdown of all bonus components
     * @param initiativeAptitudeSPA The initiative aptitude SPA (Combat Sense or Combat Paralysis), or empty string
     */
    public void addRoll(InitiativeBonusBreakdown breakdown, String initiativeAptitudeSPA) {
        int roll = getInitiativeRoll(initiativeAptitudeSPA);

        rolls.addElement(roll);
        originalRolls.addElement(roll);
        bonuses.addElement(breakdown);
        wasRollReplaced.addElement(Boolean.FALSE);
    }

    /**
     * Adds a new initiative roll with a single bonus value (for backwards compatibility).
     *
     * @param bonus                 The total bonus value
     * @param initiativeAptitudeSPA The initiative aptitude SPA (Combat Sense or Combat Paralysis), or empty string
     */
    public void addRoll(int bonus, String initiativeAptitudeSPA) {
        addRoll(InitiativeBonusBreakdown.fromTotal(bonus), initiativeAptitudeSPA);
    }

    /**
     * Set observers to -1, and don't ever let them be anything else!
     */
    public void observerRoll() {
        rolls.addElement(-1);
        originalRolls.addElement(-1);
        bonuses.addElement(InitiativeBonusBreakdown.zero());
        wasRollReplaced.addElement(Boolean.FALSE);
    }

    /**
     * Replace the previous init roll with a new one, and make a note that it was replaced. Used for Tactical Genius
     * special pilot ability (lvl 3).
     *
     * @param breakdown The breakdown of all bonus components
     * @param initiativeAptitudeSPA The initiative aptitude SPA (Combat Sense or Combat Paralysis), or empty string
     */
    public void replaceRoll(InitiativeBonusBreakdown breakdown, String initiativeAptitudeSPA) {
        int roll = getInitiativeRoll(initiativeAptitudeSPA);

        rolls.setElementAt(roll, size() - 1);
        bonuses.setElementAt(breakdown, size() - 1);
        wasRollReplaced.setElementAt(Boolean.TRUE, size() - 1);
    }

    /**
     * Replace the previous init roll with a new one (for backwards compatibility).
     *
     * @param bonus                 The total bonus value
     * @param initiativeAptitudeSPA The initiative aptitude SPA (Combat Sense or Combat Paralysis), or empty string
     */
    public void replaceRoll(int bonus, String initiativeAptitudeSPA) {
        replaceRoll(InitiativeBonusBreakdown.fromTotal(bonus), initiativeAptitudeSPA);
    }

    private int getInitiativeRoll(String initiativeAptitudeSPA) {
        int roll = d6(2);
        if (!initiativeAptitudeSPA.isBlank()) {
            List<Integer> rolls = Arrays.asList(d6(), d6(), d6());

            if (initiativeAptitudeSPA.equals(ATOW_COMBAT_SENSE)) {
                // Sort from highest to lowest
                rolls.sort(Comparator.reverseOrder());
            } else if (initiativeAptitudeSPA.equals(ATOW_COMBAT_PARALYSIS)) {
                // Sort from lowest to highest
                rolls.sort(Comparator.naturalOrder());
            }

            return rolls.get(0) + rolls.get(1);
        }

        return roll;
    }

    public int size() {
        return rolls.size();
    }

    public int getRoll(int index) {
        return rolls.elementAt(index) + bonuses.elementAt(index).total();
    }

    /**
     * Two initiative rolls are equal if they match, roll by roll
     */
    @Override
    public boolean equals(Object possibleOther) {
        if (possibleOther instanceof InitiativeRoll other) {
            if (size() != other.size()) {
                return false;
            }

            for (int i = 0; i < size(); i++) {
                if (getRoll(i) != other.getRoll(i)) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    @Override
    public int compareTo(InitiativeRoll other) {
        int minSize = Math.min(size(), other.size());
        int compare = 0;
        for (int i = 0; i < minSize; i++) {
            compare = getRoll(i) - other.getRoll(i);
            if (compare != 0) {
                return compare;
            }
        }
        return compare;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        boolean tacticalGenius = false;
        for (int i = 0; i < rolls.size(); i++) {
            Integer roll = rolls.elementAt(i);
            Integer originalRoll = originalRolls.elementAt(i);
            InitiativeBonusBreakdown breakdown = bonuses.elementAt(i);
            int bonusTotal = breakdown.total();
            int total = roll + bonusTotal;
            int originalTotal = originalRoll + bonusTotal;

            String breakdownStr = breakdown.toBreakdownString();

            if (wasRollReplaced.elementAt(i)) {
                // Tactical Genius: show original roll then replacement
                stringBuilder.append(originalTotal)
                      .append("[")
                      .append(originalRoll)
                      .append("+")
                      .append(bonusTotal);
                if (bonusTotal != 0) {
                    stringBuilder.append(" (").append(breakdownStr).append(")");
                }
                stringBuilder.append("](")
                      .append(total)
                      .append("[")
                      .append(roll)
                      .append("+")
                      .append(bonusTotal);
                if (bonusTotal != 0) {
                    stringBuilder.append(" (").append(breakdownStr).append(")");
                }
                stringBuilder.append("])");
                tacticalGenius = true;
            } else {
                stringBuilder.append(total)
                      .append("[")
                      .append(roll)
                      .append("+")
                      .append(bonusTotal);
                if (bonusTotal != 0) {
                    stringBuilder.append(" (").append(breakdownStr).append(")");
                }
                stringBuilder.append("]");
            }

            if (i != rolls.size() - 1) {
                stringBuilder.append(" / ");
            }
        }

        if (tacticalGenius) {
            stringBuilder.append(" (Tactical Genius ability used)");
        }
        return stringBuilder.toString();
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
