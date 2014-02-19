/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

/*
 * TargetRoll.java
 *
 * Created on April 19, 2002, 1:05 AM
 */

package megamek.common;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Keeps track of a target for a roll. Allows adding modifiers with
 * descriptions, including appending the modifiers in another TargetRoll.
 * Intended for rolls like a to-hit roll or a piloting skill check.
 *
 * @author Ben
 */
public class TargetRoll implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -7453086182585457422L;
    public final static int IMPOSSIBLE = Integer.MAX_VALUE;
    public final static int AUTOMATIC_FAIL = Integer.MAX_VALUE - 1;
    public final static int AUTOMATIC_SUCCESS = Integer.MIN_VALUE;
    /*
     * The CHECK_FALSE value is returned when a function that normally would
     * return a target roll number determines that the roll wasn't needed after
     * all.
     */
    public final static int CHECK_FALSE = Integer.MIN_VALUE + 1;

    private ArrayList<TargetRollModifier> modifiers = new ArrayList<TargetRollModifier>();

    private int total;

    /**
     * Creates new TargetRoll
     */
    public TargetRoll() {

    }

    /**
     * Creates a new TargetRoll with a base value & desc
     */
    public TargetRoll(int value, String desc) {
        addModifier(value, desc);
    }

    /**
     * Creates a new TargetRoll with a base value & desc, which is possibly
     * not cumulative
     *
     * @param value
     * @param desc
     * @param cumulative
     */
    public TargetRoll(int value, String desc, boolean cumulative) {
        addModifier(value, desc, cumulative);
    }

    /**
     * Returns the total value of all modifiers
     */
    public int getValue() {
        return total;
    }

    /**
     * Returns the total value of all modifiers
     */
    public String getValueAsString() {
        switch (total) {
            case IMPOSSIBLE:
                return "Impossible";
            case AUTOMATIC_FAIL:
                return "Automatic Failure";
            case AUTOMATIC_SUCCESS:
                return "Automatic Success";
            case CHECK_FALSE:
                return "Did not need to roll";
            default:
                return Integer.toString(total);
        }
    }

    /**
     * Returns a description of all applicable modifiers
     */
    public String getDesc() {
        boolean first = true;
        StringBuffer allDesc = new StringBuffer();

        for (TargetRollModifier modifier : modifiers) {

            // check for break condition
            if ((modifier.getValue() == IMPOSSIBLE)
                    || (modifier.getValue() == AUTOMATIC_FAIL)
                    || (modifier.getValue() == AUTOMATIC_SUCCESS)
                    || (modifier.getValue() == CHECK_FALSE)) {
                return modifier.getDesc();
            }

            // add desc
            if (first) {
                first = false;
            } else {
                allDesc.append((modifier.getValue() < 0 ? " - " : " + "));
            }
            allDesc.append(Math.abs(modifier.getValue()));
            allDesc.append(" (");
            allDesc.append(modifier.getDesc());
            allDesc.append(")");
        }

        return allDesc.toString();
    }

    /**
     * Returns the first description found
     */
    public String getPlainDesc() {
        return modifiers.get(0).getDesc();
    }

    /**
     * Returns the description of the first cumulative TargetRollModifier
     *
     * @return
     */
    public String getCumulativePlainDesc() {
        for (TargetRollModifier mod : modifiers) {
            if (mod.isCumulative()) {
                return mod.getDesc();
            }
        }
        return "";
    }

    /**
     * Returns the last description found
     */
    public String getLastPlainDesc() {
        TargetRollModifier last = modifiers.get(modifiers.size() - 1);
        return last.getDesc();
    }

    public void addModifier(int value, String desc) {
        addModifier(new TargetRollModifier(value, desc));
    }

    public void addModifier(int value, String desc, boolean cumulative) {
        addModifier(new TargetRollModifier(value, desc, cumulative));
    }

    public void addModifier(TargetRollModifier modifier) {
        if (modifier.getValue() == CHECK_FALSE) {
            removeAutos(true);
        }
        modifiers.add(modifier);
        recalculate();
    }

    /**
     * Append another TargetRoll to the end of this one
     */
    public void append(TargetRoll other) {
        append(other, true);
    }

    /**
     * Append another TargetRoll to the end of this one,
     * possibly ignoring non-cumulative Modifier in the other
     * one
     *
     * @param other
     * @param appendNonCumulative
     */
    public void append(TargetRoll other, boolean appendNonCumulative) {
        if (other == null) {
            return;
        }
        for (TargetRollModifier modifier : other.modifiers) {
            // possibly only add cumulative mods
            if (appendNonCumulative || modifier.isCumulative()) {
                addModifier(modifier);
            }
        }
    }

    /**
     * Remove all automatic failures or successes, but leave impossibles intact
     */

    public void removeAutos() {
        removeAutos(false);
    }

    /**
     * Remove all automatic failures or successes, and possibly also remove
     * impossibles
     *
     * @param removeImpossibles <code>boolean</code> value wether or not
     *                          impossibles should be removed
     */

    public void removeAutos(boolean removeImpossibles) {
        ArrayList<TargetRollModifier> toKeep = new ArrayList<TargetRollModifier>();
        for (TargetRollModifier modifier : modifiers) {
            if (!removeImpossibles) {
                if ((modifier.getValue() != AUTOMATIC_FAIL)
                        && (modifier.getValue() != AUTOMATIC_SUCCESS)) {
                    toKeep.add(modifier);
                }
            } else if ((modifier.getValue() != AUTOMATIC_FAIL)
                    && (modifier.getValue() != AUTOMATIC_SUCCESS)
                    && (modifier.getValue() != IMPOSSIBLE)) {
                toKeep.add(modifier);
            }
        }
        modifiers = toKeep;
        recalculate();
    }

    /**
     * Recalculate the target number & desc for all modifiers. If any of them
     * indicates an automatic result, stop and just return that modifier. Treat
     * the first modifier listed as a base
     */
    private void recalculate() {
        total = 0;

        for (TargetRollModifier modifier : modifiers) {
            // check for break condition
            if ((modifier.getValue() == IMPOSSIBLE)
                    || (modifier.getValue() == AUTOMATIC_FAIL)
                    || (modifier.getValue() == AUTOMATIC_SUCCESS)
                    || (modifier.getValue() == CHECK_FALSE)) {
                total = modifier.getValue();
                break;
            }

            // add modifier
            total += modifier.getValue();
        }
    }

}
