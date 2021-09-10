/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** 
 * Represents an AlphaStrike damage value combination of between 1 and 4 damage values which
 * typically correspond to S/M/L/E ranges. Each damage value is an ASDamage field and they can be 
 * directly accessed as S, M, L and E. The ASDamage fields store integer damage values
 * with the option of being minimal damage. 
 * <P>The ASDamageVector remembers how many damage values
 * are used and if it is special damage (using - for no damage) or standard damage (using 0 for 
 * no damage) and toString() writes the damage values accordingly.
 * <P>Note: The stored ASDamage values need not correspond to S, M, L or E
 * ranges. The single value of the IF special ability is stored in the S field even though
 * it represents a long range damage.
 * 
 * @author: Simon (Juliez)
 */
public class ASDamageVector {
    
    /** The number of ranges used, 2 = only S/M as in SRM, 4 = S/M/L/E as in Aero */
    private final int rangeBands;
    /** True for standard damage that uses 0 (e.g. 2/2/0), false for specials that use a dash (AC2/2/-) */
    private final boolean isStandard;
    public ASDamage S;
    public ASDamage M;
    public ASDamage L;
    public ASDamage E;
    
    /** 
     * Returns the number of damage values used by this damage vector. E.g., a result of 3 
     * indicates that the S, M and L damage values are used. 
     */
    public int getRangeBands() {
        return rangeBands;
    }
    
    /** 
     * Creates an ASDamageVector for special damage (e.g. IF2) from the given double value. 
     * The value is rounded first up to the nearest tenth, then normally to the nearest integer
     * and values between 0 and 0.5 excl. end up as minimal damage.
     */
    public static ASDamageVector createSpecialDamage(double s) {
        return create(s, 0, 0, 0, ASDamage::createDualRoundedNormal, 1, false);
    }
    
    /** 
     * Creates an ASDamageVector for special damage from the given double values. 
     * The values are rounded first up to the nearest tenth, then normally to the nearest integer
     * and values between 0 and 0.5 excl. end up as minimal damage. 0 values are printed as - (dash)
     */
    public static ASDamageVector createSpecialDamage(double s, double m) {
        return create(s, m, 0, 0, ASDamage::createDualRoundedNormal, 2, false);
    }
    
    /** 
     * Creates an ASDamageVector for special damage (e.g. LRM1/1/2) from the given double values. 
     * The values are rounded first up to the nearest tenth, then normally to the nearest integer
     * and values between 0 and 0.5 excl. end up as minimal damage. 0 values are printed as - (dash)
     */
    public static ASDamageVector createSpecialDamage(double s, double m, double l) {
        return create(s, m, l, 0, ASDamage::createDualRoundedNormal, 3, false);
    }
    
    /** 
     * Creates an ASDamageVector for special damage (e.g. REAR1/1/-/-) from the given double values. 
     * The values are rounded first up to the nearest tenth, then normally to the nearest integer
     * and values between 0 and 0.5 excl. end up as minimal damage. 0 values are printed as - (dash)
     */
    public static ASDamageVector createSpecialDamage(double s, double m, double l, double e) {
        return create(s, m, l, e, ASDamage::createDualRoundedNormal, 4, false);
    }
    
    /** 
     * Creates an ASDamageVector for special damage from the given double value. 
     * The value is rounded first up to the nearest tenth, then normally to the nearest integer.
     * Minimal damage is not used. 0 values are printed as - (dash)
     */
    public static ASDamageVector createSpecialDamageNoMinimal(double s) {
        return create(s, 0, 0, 0, ASDamage::createDualRoundedNormalNoMinimal, 1, false);
    }
    
    /** 
     * Creates an ASDamageVector for special damage from the given double values. 
     * The values are rounded first up to the nearest tenth, then normally to the nearest integer.
     * Minimal damage is not used. 0 values are printed as - (dash)
     */
    public static ASDamageVector createSpecialDamageNoMinimal(double s, double m) {
        return create(s, m, 0, 0, ASDamage::createDualRoundedNormalNoMinimal, 2, false);
    }
    
    /** 
     * Creates an ASDamageVector for special damage from the given double values. 
     * The values are rounded first up to the nearest tenth, then normally to the nearest integer.
     * Minimal damage is not used. 0 values are printed as - (dash)
     */
    public static ASDamageVector createSpecialDamageNoMinimal(double s, double m, double l) {
        return create(s, m, l, 0, ASDamage::createDualRoundedNormalNoMinimal, 3, false);
    }
    
    /** 
     * Creates an ASDamageVector for special damage from the given double values. 
     * The values are rounded first up to the nearest tenth, then normally to the nearest integer.
     * Minimal damage is not used. 0 values are printed as - (dash)
     */
    public static ASDamageVector createSpecialDamageNoMinimal(double s, double m, double l, double e) {
        return create(s, m, l, e, ASDamage::createDualRoundedNormalNoMinimal, 4, false);
    }
    
    /** 
     * Creates an ASDamageVector for special damage from the given double values and 
     * the number of ranges to be used. The values are rounded first up to the nearest tenth, then normally 
     * (i.e. up or down depending on the tenth) the nearest integer. Minimal damage will be used.
     * When printed, the ASDamageVector will use - for zero values.
     */
    public static ASDamageVector createSpecialDamage(List<Double> values, int ranges) {
        List<Double> copy = new ArrayList<>(values);
        while (copy.size() < 4) {
            copy.add(0.0);
        }
        return create(copy.get(0), copy.get(1), copy.get(2), copy.get(3), 
                ASDamage::createDualRoundedNormal, ranges, false);
    }

    /**
     * Creates an ASDamageVector for special damage from the given double values and
     * the number of ranges to be used. The values are rounded first up to the nearest tenth, then normally
     * (i.e. up or down depending on the tenth) the nearest integer. Minimal damage is not used.
     * When printed, the ASDamageVector will use - for zero values.
     */
    public static ASDamageVector createSpecialDamageNoMinimal(List<Double> values, int ranges) {
        List<Double> copy = new ArrayList<>(values);
        while (copy.size() < 4) {
            copy.add(0.0);
        }
        return create(copy.get(0), copy.get(1), copy.get(2), copy.get(3),
                ASDamage::createDualRoundedNormalNoMinimal, ranges, false);
    }
    
    /** 
     * Creates an ASDamageVector for standard damage from the given double values and 
     * the number of ranges to be used (usually 3 or 4). The values are rounded first up to the 
     * nearest tenth, then up to the nearest integer and values between 0 and 0.5 excl. 
     * end up as minimal damage.
     * When printed, the ASDamageVector will use 0 (not a dash) for zero values.
     */
    public static ASDamageVector createStandardDamage(List<Double> values, int ranges) {
        List<Double> copy = new ArrayList<Double>(values);
        while (copy.size() < 4) {
            copy.add(0.0);
        }
        return create(copy.get(0), copy.get(1), copy.get(2), copy.get(3), 
                ASDamage::createDualRoundedUp, ranges, true);
    }
    
    /** 
     * Creates an ASDamageVector for S/M/L standard damage from the given 
     * double values. The values are rounded first up to the nearest tenth, then up  
     * to the nearest integer and values between 0 and 0.5 excl. end up as minimal damage.
     * When printed, the ASDamageVector will use 0 (not a dash) for zero values.
     */
    public static ASDamageVector createStandardDamage(double s, double m, double l) {
        return create(s, m, l, 0, ASDamage::createDualRoundedUp, 3, true);
    }
    
    /** 
     * Creates an ASDamageVector for S/M/L/E standard damage from the given 
     * double values. The values are rounded first up to the nearest tenth, then up  
     * to the nearest integer and values between 0 and 0.5 excl. end up as minimal damage.
     * When printed, the ASDamageVector will use 0 (not a dash) for zero values.
     */
    public static ASDamageVector createStandardDamage(double s, double m, double l, double e) {
        return create(s, m, l, e, ASDamage::createDualRoundedUp, 4, true);
    }
    
    /** Constructor, only for internal use. */
    private ASDamageVector(int ranges, boolean std) {
        rangeBands = ranges;
        isStandard = std;
    }
    
    /** Only for internal use. Finally creates the object from the gathered data. */ 
    private static ASDamageVector create(double s, double m, double l, double e, 
            Function<Double, ASDamage> func, int ranges, boolean std) {
        var result = new ASDamageVector(ranges, std);
        result.S = func.apply(s);
        result.M = func.apply(m);
        result.L = func.apply(l);
        result.E = func.apply(e);
        return result;
    }

    /** Returns true if this ASDamageVector represents any damage at any range, minimal or 1 or more. */
    public boolean hasDamage() {
        return S.hasDamage() || M.hasDamage() || L.hasDamage() || E.hasDamage();
    }
    
    @Override
    public String toString() {
        StringBuffer result = new StringBuffer();
        if (isStandard) {
            result.append(S.toStringWithZero());
            if (rangeBands >= 2) {
                result.append("/").append(M.toStringWithZero());
            }
            if (rangeBands >= 3) {
                result.append("/").append(L.toStringWithZero());
            }
            if (rangeBands == 4) {
                result.append("/").append(E.toStringWithZero());
            }
        } else {
            result.append(S);
            if (rangeBands >= 2) {
                result.append("/").append(M);
            }
            if (rangeBands >= 3) {
                result.append("/").append(L);
            }
            if (rangeBands == 4) {
                result.append("/").append(E);
            }
        }
        return result.toString();
    }

}