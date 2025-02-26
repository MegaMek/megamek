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

import com.fasterxml.jackson.annotation.JsonValue;
import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Represents an AlphaStrike damage value combination of between 1 and 4 damage values which
 * typically correspond to S/M/L/E ranges. Each damage value is an ASDamage field and they can be 
 * directly accessed as S, M, L and E. The ASDamage fields store integer damage values
 * with the option of being minimal damage. 
 * The ASDamageVector remembers how many damage values
 * are used and if it is special damage (using - for no damage) or standard damage (using 0 for 
 * no damage) and toString() writes the damage values accordingly.
 * ASDamageVector is immutable.
 *
 * @author Simon (Juliez)
 */
public class ASDamageVector implements Serializable {

    /**
     * A constant that represents zero damage, written as 0/0/0/0 in toString(). May be used
     * as a return value instead of null.
     */
    public static final ASDamageVector ZERO = new ASDamageVector(ASDamage.ZERO, ASDamage.ZERO,
            ASDamage.ZERO, ASDamage.ZERO, 4, true);

    /**
     * A constant that represents zero damage as special damage, written as -/-/-/- in toString().
     * May be used as a return value instead of null.
     */
    public static final ASDamageVector ZEROSPECIAL = new ASDamageVector(ASDamage.ZERO, ASDamage.ZERO,
            ASDamage.ZERO, ASDamage.ZERO, 4, false);

    /**
     * The number of damage values used by this damage vector. 2 indicates that only the S and M
     * damage values are used, as with SRM. 4 indicates all S/M/L/E damage values are used such
     * as with Aero.
     */
    public final int rangeBands;

    public final ASDamage S;
    public final ASDamage M;
    public final ASDamage L;
    public final ASDamage E;

    /** True for standard damage that uses 0 (e.g. 2/2/0), false for specials that use a dash (AC2/2/-) */
    private final boolean isStandard;

    /** Returns true if this ASDamageVector represents any damage at any range, minimal or 1 or more. */
    public boolean hasDamage() {
        return S.hasDamage() || M.hasDamage() || L.hasDamage() || E.hasDamage();
    }

    /**
     * Returns true when this ASDamageVector uses the given range; e.g. Aero standard damage will return
     * true for all ranges while SRM damage will return true only for SHORT and MEDIUM. Note that this
     * does not check if unused ranges happen to contain damage values of more than null, only if this
     * ASDamageVector was constructed to include the given range.
     */
    public boolean usesDamage(ASRange range) {
        return switch (range) {
            case EXTREME -> rangeBands == 4;
            case LONG -> rangeBands >= 3;
            case MEDIUM -> rangeBands >= 2;
            default -> true;
        };
    }

    /** Returns the ASDamage of this ASDamageVector for the given range, so the S, M, L, or E damage. */
    public ASDamage getDamage(ASRange range) {
        return switch (range) {
            case EXTREME -> E;
            case LONG -> L;
            case MEDIUM -> M;
            default -> S;
        };
    }

    /**
     * Creates an ASDamageVector using the given integer values as damage and the number of
     * ranges. Negative damage values are set to 0; ranges is kept between 1 and 4. The
     * values actually used depend on the given ranges value; when ranges is 2, only the
     * s and m values are used.
     */
    public static ASDamageVector create(int s, int m, int l, int e, int ranges) {
        return new ASDamageVector(new ASDamage(s), new ASDamage(m), new ASDamage(l), new ASDamage(e),
                ranges, true);
    }

    /**
     *
     * Creates an ASDamageVector for special damage (e.g. IF2) from the given double value. 
     * The value is rounded first up to the nearest tenth, then normally to the nearest integer
     * and values between 0 and 0.5 excl. end up as minimal damage.
     */
    public static ASDamageVector createNormRndDmg(double s) {
        return create(s, 0, 0, 0, ASDamage::createDualRoundedNormal, 1, false);
    }

    /**
     * Creates an ASDamageVector for special damage from the given double values. 
     * The values are rounded first up to the nearest tenth, then normally to the nearest integer
     * and values between 0 and 0.5 excl. end up as minimal damage. 0 values are printed as - (dash)
     */
    public static ASDamageVector createNormRndDmg(double s, double m) {
        return create(s, m, 0, 0, ASDamage::createDualRoundedNormal, 2, false);
    }

    /**
     * Creates an ASDamageVector for special damage (e.g. LRM1/1/2) from the given double values. 
     * The values are rounded first up to the nearest tenth, then normally to the nearest integer
     * and values between 0 and 0.5 excl. end up as minimal damage. 0 values are printed as - (dash)
     */
    public static ASDamageVector createNormRndDmg(double s, double m, double l) {
        return create(s, m, l, 0, ASDamage::createDualRoundedNormal, 3, false);
    }

    /**
     * Creates an ASDamageVector for special damage (e.g. REAR1/1/-/-) from the given double values. 
     * The values are rounded first up to the nearest tenth, then normally to the nearest integer
     * and values between 0 and 0.5 excl. end up as minimal damage. 0 values are printed as - (dash)
     */
    public static ASDamageVector createNormRndDmg(double s, double m, double l, double e) {
        return create(s, m, l, e, ASDamage::createDualRoundedNormal, 4, false);
    }

    /**
     * Creates an ASDamageVector for special damage from the given double value. 
     * The value is rounded first up to the nearest tenth, then normally to the nearest integer.
     * Minimal damage is not used. 0 values are printed as - (dash)
     */
    public static ASDamageVector createNormRndDmgNoMin(double s) {
        return create(s, 0, 0, 0, ASDamage::createDualRoundedNormalNoMinimal, 1, false);
    }

    /**
     * Creates an ASDamageVector for special damage from the given double values. 
     * The values are rounded first up to the nearest tenth, then normally to the nearest integer.
     * Minimal damage is not used. 0 values are printed as - (dash)
     */
    public static ASDamageVector createNormRndDmgNoMin(double s, double m) {
        return create(s, m, 0, 0, ASDamage::createDualRoundedNormalNoMinimal, 2, false);
    }

    /**
     * Creates an ASDamageVector for special damage from the given double values. 
     * The values are rounded first up to the nearest tenth, then normally to the nearest integer.
     * Minimal damage is not used. 0 values are printed as - (dash)
     */
    public static ASDamageVector createNormRndDmgNoMin(double s, double m, double l) {
        return create(s, m, l, 0, ASDamage::createDualRoundedNormalNoMinimal, 3, false);
    }

    /**
     * Creates an ASDamageVector for special damage from the given double values. 
     * The values are rounded first up to the nearest tenth, then normally to the nearest integer.
     * Minimal damage is not used. 0 values are printed as - (dash)
     */
    public static ASDamageVector createNormRndDmgNoMin(double s, double m, double l, double e) {
        return create(s, m, l, e, ASDamage::createDualRoundedNormalNoMinimal, 4, false);
    }

    /**
     * Creates an ASDamageVector for special damage from the given double values and 
     * the number of ranges to be used. The values are rounded first up to the nearest tenth, then normally 
     * (i.e. up or down depending on the tenth) the nearest integer. Minimal damage will be used.
     * When printed, the ASDamageVector will use - for zero values.
     */
    public static ASDamageVector createNormRndDmg(@Nullable List<Double> values, int ranges) {
        if (values == null) {
            return ZERO;
        }
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
    public static ASDamageVector createNormRndDmgNoMin(@Nullable List<Double> values, int ranges) {
        if (values == null) {
            return ZERO;
        }
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
    public static ASDamageVector createUpRndDmg(@Nullable List<Double> values, int ranges) {
        if (values == null) {
            return ZERO;
        }
        List<Double> copy = new ArrayList<>(values);
        while (copy.size() < 4) {
            copy.add(0.0);
        }
        return create(copy.get(0), copy.get(1), copy.get(2), copy.get(3),
                ASDamage::createDualRoundedUp, ranges, true);
    }

    /**
     * Creates an ASDamageVector for special damage from the given double values and
     * the number of ranges to be used (usually 3 or 4). The values are rounded first up to the
     * nearest tenth, then up to the nearest integer and values between 0 and 0.5 excl.
     * end up as minimal damage.
     * When printed, the ASDamageVector will use "-" for zero values.
     */
    public static ASDamageVector createUpRndDmgMinus(@Nullable List<Double> values, int ranges) {
        if (values == null) {
            return ZEROSPECIAL;
        }
        List<Double> copy = new ArrayList<>(values);
        while (copy.size() < 4) {
            copy.add(0.0);
        }
        return create(copy.get(0), copy.get(1), copy.get(2), copy.get(3),
                ASDamage::createDualRoundedUp, ranges, false);
    }

    /**
     * Creates an ASDamageVector for S/M/L standard damage from the given 
     * double values. The values are rounded first up to the nearest tenth, then up  
     * to the nearest integer and values between 0 and 0.5 excl. end up as minimal damage.
     * When printed, the ASDamageVector will use 0 (not a dash) for zero values.
     */
    public static ASDamageVector createUpRndDmg(double s, double m, double l) {
        return create(s, m, l, 0, ASDamage::createDualRoundedUp, 3, true);
    }

    /**
     * Creates an ASDamageVector for S/M/L/E standard damage from the given 
     * double values. The values are rounded first up to the nearest tenth, then up  
     * to the nearest integer and values between 0 and 0.5 excl. end up as minimal damage.
     * When printed, the ASDamageVector will use 0 (not a dash) for zero values.
     */
    public static ASDamageVector createUpRndDmg(double s, double m, double l, double e) {
        return create(s, m, l, e, ASDamage::createDualRoundedUp, 4, true);
    }

    /** Only for internal use. Finally creates the object from the gathered data. */
    private static ASDamageVector create(double s, double m, double l, double e,
            Function<Double, ASDamage> func, int ranges, boolean std) {
        return new ASDamageVector(func.apply(s), func.apply(m), func.apply(l), func.apply(e), ranges, std);
    }

    /**
     * Construct an ASDamageVector from ASDamage values. Damage is set to 0 for unused ranges.
     * When std is true, creates a standard damage type vector that uses "0" for zero damage,
     * otherwise prints "-" for zero damage.
     */
    public ASDamageVector(ASDamage s, ASDamage m, ASDamage l, ASDamage e, int ranges, boolean std) {
        rangeBands = MathUtility.clamp(ranges, 1, 4);
        S = (s == null) ? new ASDamage(0) : s;
        M = (rangeBands < 2) || (m == null) ? new ASDamage(0) : m;
        L = (rangeBands < 3) || (l == null) ? new ASDamage(0) : l;
        E = (rangeBands < 4) || (e == null) ? new ASDamage(0) : e;
        isStandard = std;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
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

    /**
     * @return A string representation of this ASDamageVector for serialization. This should not be used for
     * display or other purposes and not be changed unless the rules change.
     */
    @JsonValue
    public String toStringForSerialization() {
        String result = S.toStringWithZero();
        result += (rangeBands >= 2) ? "/" + M.toStringWithZero() : "";
        result += (rangeBands >= 3) ? "/" + L.toStringWithZero() : "";
        result += (rangeBands == 4) ? "/" + E.toStringWithZero() : "";
        return result;
    }

    /**
     * Tries to parse the given text to the appropriate ASDamageVector. Acceptable values are combinations of
     * four or fewer "0", "0*", "-" (equal to "0") and positive Integers separated by slashes, e.g. "2/0* /0/0"
     * (without the space) or "11/-". Other values will result in an IllegalArgumentException.
     *
     * @param asText The text to translate to an ASDamageVector value
     * @return The ASDamageVector represented by the given text
     * @throws IllegalArgumentException When the text cannot be parsed
     */
    public static ASDamageVector parse(String asText) {
        if (asText == null) {
            throw new IllegalArgumentException("Cannot parse null text.");
        }
        String[] values = asText.split("/");
        if ((values.length < 1) || (values.length > 4)) {
            throw new IllegalArgumentException("Malformed AS damage vector text.");
        }
        ASDamage s = ASDamage.parse(values[0]);
        ASDamage m = values.length >= 2 ? ASDamage.parse(values[1]) : ASDamage.ZERO;
        ASDamage l = values.length >= 3 ? ASDamage.parse(values[2]) : ASDamage.ZERO;
        ASDamage e = values.length == 4 ? ASDamage.parse(values[3]) : ASDamage.ZERO;
        return new ASDamageVector(s, m, l, e, values.length, true);
    }

    public static boolean canParse(String asText) {
        try {
            parse(asText);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Returns a new damage vector where the damage value of all ranges are reduced by the given reduction, down
     * to a minimum of 0. A 5/2/1 reduced by 1 becomes 4/1/0. A 5/2/1 reduced by 3 becomes 2/0/0. Note that
     * negative reduction can be used but will also increase 0 values.
     *
     * @param damageReduction The amount of damage to deduct.
     * @return A new ASDamageVector with reduced damage
     */
    public ASDamageVector reducedBy(int damageReduction) {
        return create(S.damage - damageReduction, M.damage - damageReduction, L.damage - damageReduction,
                E.damage - damageReduction, rangeBands);
    }
}