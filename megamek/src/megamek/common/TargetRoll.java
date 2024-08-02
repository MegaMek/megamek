/*
 * Copyright (c) 2000-2002 Ben Mazur (bmazur@sev.org)
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Keeps track of a target for a roll. Allows adding modifiers with
 * descriptions, including appending the modifiers in another TargetRoll.
 * Intended for rolls like a to-hit roll or a piloting skill check.
 *
 * @author Ben
 */
public class TargetRoll implements Serializable {

    // The finalizers

    public static final int IMPOSSIBLE = Integer.MAX_VALUE;
    public static final int AUTOMATIC_FAIL = Integer.MAX_VALUE - 1;
    public static final int AUTOMATIC_SUCCESS = Integer.MIN_VALUE;

    /** The CHECK_FALSE value is returned when a function that normally would return a target roll number
     determines that the roll wasn't needed after all. */
    public static final int CHECK_FALSE = Integer.MIN_VALUE + 1;

    private static final Set<Integer> FINALIZERS = Set.of(IMPOSSIBLE, AUTOMATIC_FAIL, AUTOMATIC_SUCCESS, CHECK_FALSE);
    private static final Set<Integer> AUTOS = Set.of(AUTOMATIC_FAIL, AUTOMATIC_SUCCESS);
    private static final Set<Integer> AUTOS_AND_IMPOSSIBLE = Set.of(IMPOSSIBLE, AUTOMATIC_FAIL, AUTOMATIC_SUCCESS);

    /**
     * This list of roll modifiers. Always call recalculate() after modifying it. This is clearly an unsafe
     * way to implement it. It *may be* done like that for performance reasons for Princess.
     */
    private final List<TargetRollModifier> modifiers = new ArrayList<>();
    private int total;

    /**
     * Creates a new, empty TargetRoll. This is by itself not yet useful.
     */
    public TargetRoll() { }

    /**
     * Creates a new TargetRoll with a base value and desc
     */
    public TargetRoll(int value, String desc) {
        this(new TargetRollModifier(value, desc));
    }

    /**
     * Creates a new TargetRoll with a base value and desc, which is possibly not cumulative
     */
    public TargetRoll(int value, String desc, boolean cumulative) {
        this(new TargetRollModifier(value, desc, cumulative));
    }

    public TargetRoll(TargetRollModifier baseValue) {
        addModifier(baseValue);
    }

    /**
     * @return The sum of all modifiers. When a finalizer is present, the value equals that finalizer.
     */
    public int getValue() {
        return total;
    }

    /**
     * @return The total value of all modifiers as a text.
     */
    public String getValueAsString() {
        return switch (total) {
            case IMPOSSIBLE -> "Impossible";
            case AUTOMATIC_FAIL -> "Automatic Failure";
            case AUTOMATIC_SUCCESS -> "Automatic Success";
            case CHECK_FALSE -> "Did not need to roll";
            default -> Integer.toString(total);
        };
    }

    /**
     * @return All modifiers of this TargetRoll. The list is a copy and may be modified.
     */
    public List<TargetRollModifier> getModifiers() {
        return new ArrayList<>(modifiers);
    }

    /**
     * Returns a description of all applicable modifiers
     */
    public String getDesc() {
        boolean first = true;
        StringBuilder allDesc = new StringBuilder();

        for (TargetRollModifier modifier : modifiers) {
            if (isFinalizer(modifier.getValue())) {
                return modifier.getDesc();
            }

            if (first) {
                first = false;
                allDesc.append(modifier.getValue());
            } else {
                allDesc.append((modifier.getValue() < 0 ? " - " : " + "))
                        .append(Math.abs(modifier.getValue()));
            }
            allDesc.append(" (").append(modifier.getDesc()).append(")");
        }

        return allDesc.toString();
    }

    /**
     * @return False when this TargetRoll has any finalizers (automatic fail or success, impossible or no check
     * required). True otherwise (even if the modifiers add up to very high or low values).
     */
    public boolean needsRoll() {
        return !isFinalizer(total);
    }

    @Override
    public String toString() {
        return getDesc();
    }

    /**
     * Returns the first description found
     */
    public String getPlainDesc() {
        return modifiers.get(0).getDesc();
    }

    /**
     * @return the description of the first cumulative TargetRollModifier
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

    /**
     * Adds a new cumulative modifier of the given value and description.
     *
     * @param value The modifier value, e.g. +2 or -1
     * @param desc A short description of the modifier
     */
    public void addModifier(int value, String desc) {
        addModifier(new TargetRollModifier(value, desc));
    }

    /**
     * Adds a new modifier of the given value and description, which is or is not cumulative.
     *
     * @param value The modifier value, e.g. +2 or -1
     * @param desc A short description of the modifier
     * @param cumulative True when this modifier is cumulative
     */
    public void addModifier(int value, String desc, boolean cumulative) {
        addModifier(new TargetRollModifier(value, desc, cumulative));
    }

    /**
     * Adds the given new modifier to this TargetRoll.
     *
     * @param modifier The new modifier
     */
    public void addModifier(TargetRollModifier modifier) {
        addModifierImpl(modifier);
    }

    /**
     * Append another TargetRoll to the end of this one
     */
    public void append(TargetRoll other) {
        append(other, true);
    }

    /**
     * Append another TargetRoll to the end of this one, possibly discarding non-cumulative modifier
     * in the other one.
     *
     * @param other the TargetRoll to append
     * @param appendNonCumulative True to append all modifiers, false to append only cumulative modifiers
     */
    public void append(TargetRoll other, boolean appendNonCumulative) {
        if (other != null) {
            for (TargetRollModifier modifier : other.modifiers) {
                if (appendNonCumulative || modifier.isCumulative()) {
                    addModifier(modifier);
                }
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
     * impossibles.
     *
     * @param removeImpossibles When true, IMPOSSIBLEs are also removed
     */
    public void removeAutos(boolean removeImpossibles) {
        if (removeImpossibles) {
            modifiers.removeIf(this::isAutomaticOrImpossible);
        } else {
            modifiers.removeIf(this::isAutomatic);
        }
        recalculate();
    }

    private boolean isAutomaticOrImpossible(TargetRollModifier modifier) {
        return AUTOS_AND_IMPOSSIBLE.contains(modifier.getValue());
    }

    private boolean isAutomatic(TargetRollModifier modifier) {
        return AUTOS.contains(modifier.getValue());
    }

    /**
     * Recalculates the target number of this TargetRoll. Automatic results supersede any normal modifiers.
     */
    private void recalculate() {
        total = 0;
        for (TargetRollModifier modifier : modifiers) {
            if (isFinalizer(modifier.getValue())) {
                total = modifier.getValue();
                return;
            } else {
                total += modifier.getValue();
            }
        }
    }

    private boolean isFinalizer(int value) {
        return FINALIZERS.contains(value);
    }

    private void addModifierImpl(TargetRollModifier modifier) {
        if (modifier.getValue() == CHECK_FALSE) {
            // When the check is no longer necessary, remove other finalizers that would come first
            removeAutos(true);
        }
        modifiers.add(modifier);
        recalculate();
    }

    /**
     * @return True when this roll cannot succeed regardless of numbers, i.e. is impossible or automatically fails.
     * Returns false otherwise, i.e. when the roll does not contain these finalizing conditions,
     * even if the total roll modifier is above 12.
     */
    public boolean cannotSucceed() {
        return (getValue() == IMPOSSIBLE) || (getValue() == AUTOMATIC_FAIL);
    }
}
