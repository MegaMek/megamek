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

import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.alphaStrike.conversion.ASDamageConverter;

import java.io.Serializable;

/**
 * Represents a single AlphaStrike damage value that may be minimal damage (0*).
 * Minimal Damage is repesented by isMinimal() returning true, all other values
 * by getDamage() being their damage value and isMinimal() returning false.
 * ASDamage is immutable.
 */
public class ASDamage implements Serializable {
    
    /**
     * The value of this damage. Is 0 for both zero damage and minimal damage.
     * When using this for damage resolution, make sure to check for minimal
     * damage separately.
     */
    public final int damage;

    /** True if this is minimal damage, i.e. 0*  */
    public final boolean minimal;

    /** A constant that represents zero damage. May be used as a return value instead of null. */
    public static final ASDamage ZERO = new ASDamage(0, false);

    /**
     * Creates an AlphaStrike damage value that may be minimal damage, i.e. 0*.
     * When 0 < damageValue < 0.5, the result will be minimal damage.
     * Otherwise, damageValue is rounded normally (a negative damageValue is set to 0).
     */
    public ASDamage(double damageValue) {
        this(damageValue, true);
    }

    /**
     * Creates an AlphaStrike damage value. It may be minimal damage, i.e. 0*, only if
     * allowMinimal is true. In that case, when 0 < damageValue < 0.5, the result will
     * be minimal damage. When allowMinimal is false or damageValue >= 0.5, damageValue
     * is rounded normally (a negative damageValue is set to 0).
     */
    public ASDamage(double damageValue, boolean allowMinimal) {
        this((int) Math.round(damageValue), (damageValue > 0) && (damageValue < 0.5) && allowMinimal);
    }

    /**
     * Creates an AlphaStrike damage value that may be minimal damage, i.e. 0*.
     * When the given isMinimal is true, this overrides any damageValue given and
     * the resulting ASDamage will be 0*. Otherwise, the resulting ASDamage will be
     * equal to the damageValue (a negative damageValue is set to 0).
     */
    public ASDamage(int damageValue, boolean isMinimal) {
        minimal = isMinimal;
        damage = (isMinimal || damageValue < 0) ? 0 : damageValue;
    }

    /**
     * Creates an AlphaStrike damage value that may be minimal damage, i.e. 0*.
     * When 0 < damageValue < 0.5, the result will be minimal damage.
     * Otherwise, damageValue is rounded normally (a negative damageValue is set to 0).
     */
    public static ASDamage createRoundedNormal(double dmg) {
        return new ASDamage(dmg);
    }
    
    /** 
     * Creates an AlphaStrike damage value that may be minimal damage, i.e. 0*. The value
     * is first rounded up to the nearest tenth, then assigned minimal damage if < 0.5,  
     * otherwise rounded normally (i.e. up or down depending on the tenth) to the nearest integer.
     */
    public static ASDamage createDualRoundedNormal(double dmg) {
        return new ASDamage(ASDamageConverter.roundUpToTenth(dmg));
    }

    /** 
     * Creates an AlphaStrike damage value from the given double value. The value
     * is first rounded up to the nearest tenth, then assigned minimal damage if < 0.5,  
     * otherwise rounded up (i.e. up or down depending on the tenth) to the nearest integer.
     */
    public static ASDamage createDualRoundedUp(double dmg) {
        double intermediate = ASDamageConverter.roundUpToTenth(dmg);
        return intermediate < 0.5 ? new ASDamage(intermediate) : new ASDamage(ASConverter.roundUp(intermediate));
    }

    /**
     * Creates an AlphaStrike damage value from the given double value. The value
     * is first rounded up to the nearest tenth, then rounded normally (i.e. up or
     * down depending on the tenth) to the nearest integer. There is no minimal damage, 
     * i.e. dmg < 0.41 becomes 0. 
     */
    public static ASDamage createDualRoundedNormalNoMinimal(double dmg) {
        return new ASDamage((int)Math.round(ASDamageConverter.roundUpToTenth(dmg)), false);
    }
    
    /** Returns true if this ASDamage represents any damage, minimal or 1 or more. */
    public boolean hasDamage() {
        return (damage > 0) || minimal;
    }
    
    @Override
    public String toString() {
        if (minimal) {
            return "0*";
        } else if (damage == 0) {
            return "-";
        } else {
            return damage + "";
        }
    }
    
    public String toStringWithZero() {
        if (minimal) {
            return "0*";
        } else {
            return damage + "";
        }
    }

    public double asDoubleValue() {
        return minimal ? 0.5 : damage;
    }
}
