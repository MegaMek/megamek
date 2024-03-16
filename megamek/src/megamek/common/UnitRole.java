/*
 * Copyright (c) 2023 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.BattleForceSUA;
import org.apache.logging.log4j.LogManager;

import java.util.Arrays;
import java.util.function.Predicate;

import static megamek.common.UnitRole.Availability.*;

/**
 * Unit roles as defined by Alpha Strike Companion, used in formation building rules
 * in ASC and Campaign Operations
 * 
 * @author Neoancient
 */
public enum UnitRole {

    /** This is the default role; given to units where the role definition is missing. */
    UNDETERMINED (ALL),

    /** Shows that this unit intentionally has no combat role. */
	NONE (ALL),

    AMBUSHER (GROUND),
    BRAWLER (GROUND),
    JUGGERNAUT (GROUND),
    MISSILE_BOAT (GROUND),
    SCOUT (GROUND),
    SKIRMISHER (GROUND),
    SNIPER (GROUND),
    STRIKER (GROUND),

    ATTACK_FIGHTER (AERO),
    DOGFIGHTER (AERO),
    FAST_DOGFIGHTER (AERO),
    FIRE_SUPPORT (AERO),
    INTERCEPTOR (AERO),
    TRANSPORT (AERO);


    /** @return True when the given unit may use this role. Used in MML. */
    @SuppressWarnings("unused")
    public boolean isAvailableTo(BTObject unit) {
        return availableTo.fits(unit) && !unit.isUnitGroup();
    }

    public boolean isAnyOf(UnitRole role, UnitRole... otherRoles) {
        return (this == role) || Arrays.stream(otherRoles).anyMatch(otherRole -> this == otherRole);
    }

    /** @return True when this role is not UNDETERMINED. (Returns true for NONE.) */
    public boolean hasRole() {
        return this != UNDETERMINED;
    }

    /**
     * Parses the given string into the UnitRole if possible and returns it. If it can't parse the string,
     * logs an error and returns UNDETERMINED. Does not return null.
     *
     * @return The UnitRole given as a string or UNDETERMINED.
     */
    public static UnitRole parseRole(String role) {
        switch (role.toLowerCase()) {
            case "ambusher":
                return AMBUSHER;
            case "brawler":
                return BRAWLER;
            case "juggernaut":
                return JUGGERNAUT;
            case "missile_boat":
            case "missile boat":
                return MISSILE_BOAT;
            case "scout":
                return SCOUT;
            case "skirmisher":
                return SKIRMISHER;
            case "sniper":
                return SNIPER;
            case "striker":
                return STRIKER;
            case "attack_fighter":
            case "attack fighter":
            case "attack":
                return ATTACK_FIGHTER;
            case "dogfighter":
                return DOGFIGHTER;
            case "fast_dogfighter":
            case "fast dogfighter":
                return FAST_DOGFIGHTER;
            case "fire_support":
            case "fire support":
            case "fire-support":
                return FIRE_SUPPORT;
            case "interceptor":
                return INTERCEPTOR;
            case "transport":
                return TRANSPORT;
            case "none":
            	return NONE;
            default:
                LogManager.getLogger().error("Could not parse role " + role);
                return UNDETERMINED;
        }
    }

    /**
     * Applies the criteria from Alpha Strike Companion to determine whether a unit
     * qualifies for a particular role. As the canon unit roles do not themselves adhere
     * strictly to the guidelines, there is some allowance for fuzziness in applying the
     * criteria by computing a score. Stats outside the given ranges lower the score, and
     * special abilities that are useful for a role raise the score.
     *
     * This method calculates AlphaStrike statistics for the Entity as the first step in the calculation.
     *
     * @param entity      The unit to be checked for role qualification
     * @return          Boolean value indicating whether the unit meets the qualifications for this role.
     */
    public boolean qualifiesForRole(Entity entity) {
        return qualifiesForRole(ASConverter.convert(entity), 0);
    }

    /**
     * Applies the criteria from Alpha Strike Companion to determine whether a unit
     * qualifies for a particular role. As the canon unit roles do not themselves adhere
     * strictly to the guidelines, there is some allowance for fuzziness in applying the
     * criteria by computing a score. Stats outside the given ranges lower the score, and
     * special abilities that are useful for a role raise the score.
     *
     * This method calculates AlphaStrike statistics for the Entity as the first step in the calculation.
     *
     * @param entity      The unit to be checked for role qualification
     * @param tolerance A measure of how strictly to apply the qualifications. A value of zero is
     *                  more or less by the book, while values below 0 are more liberal and above 0 are
     *                  more strict.
     * @return          Boolean value indicating whether the unit meets the qualifications for this role.
     */
    public boolean qualifiesForRole(Entity entity, double tolerance) {
        return qualifiesForRole(ASConverter.convert(entity), tolerance);
    }

    /**
     * Applies the criteria from Alpha Strike Companion to determine whether a unit
     * qualifies for a particular role. As the canon unit roles do not themselves adhere
     * strictly to the guidelines, there is some allowance for fuzziness in applying the
     * criteria by computing a score. Stats outside the given ranges lower the score, and
     * special abilities that are useful for a role raise the score.
     *
     * @param unit      The unit to be checked for role qualification
     * @return          Boolean value indicating whether the unit meets the qualifications for this role.
     */
    public boolean qualifiesForRole(AlphaStrikeElement unit) {
        return qualifiesForRole(unit, 0);
    }

    /**
     * Applies the criteria from Alpha Strike Companion to determine whether a unit
     * qualifies for a particular role. As the canon unit roles do not themselves adhere
     * strictly to the guidelines, there is some allowance for fuzziness in applying the
     * criteria by computing a score. Stats outside the given ranges lower the score, and
     * special abilities that are useful for a role raise the score.
     *
     * @param unit		The unit to be checked for role qualification
     * @param tolerance	A measure of how strictly to apply the qualifications. A value of zero is
     * 					more or less by the book, while values &lt; 0 are more liberal and &gt; 0 are
     * 					stricter.
     * @return			Boolean value indicating whether the unit meets the qualifications for this role.
     */
    public boolean qualifiesForRole(AlphaStrikeElement unit, double tolerance) {
        if (!isAvailableTo(unit)) {
            return false;
        }
        double score = 0;
        int speed = unit.getPrimaryMovementValue();
        switch (this) {
            case AMBUSHER:
                /* Slow, light armor, preference for short range */
                score -= Math.max(0, speed - 6) * 0.5;
                score -= Math.max(0, unit.getFullArmor() - 5);
                if (unit.hasSUA(BattleForceSUA.ECM)
                        || unit.hasSUA(BattleForceSUA.LECM)
                        || unit.hasSUA(BattleForceSUA.WAT)) {
                    score++;
                }
                if (unit.hasSUA(BattleForceSUA.STL)
                        || unit.hasSUA(BattleForceSUA.MAS)
                        || unit.hasSUA(BattleForceSUA.LMAS)) {
                    score++;
                }
                if (unit.getStandardDamage().S.damage >
                        unit.getStandardDamage().M.damage) {
                    score++;
                } else if (unit.getStandardDamage().S.damage >
                        unit.getStandardDamage().L.damage) {
                    score += 0.5;
                }
                break;
            case BRAWLER:
                /* Not too slow, preference for medium range */
                score += Math.min(0, speed - 8);
                if (unit.getStandardDamage().M.damage >=
                        unit.getStandardDamage().S.damage) {
                    score += 0.5;
                }
                if (unit.getStandardDamage().M.damage >
                        unit.getStandardDamage().L.damage) {
                    score += 0.5;
                }
                break;
            case JUGGERNAUT:
                /* Slow and heavily armored and armed, preference for short range */
                score -= Math.max(0, speed - 6) * 0.5;
                /* Per ASC, a Juggernaut should have an armor value of 7, but there are a large number
                 * of smaller units with lower armor values that have an official role of juggernaut.*/
                score += Math.min(0,  unit.getFullArmor() - (unit.getSize() + 4));
                if (Math.max(unit.getStandardDamage().S.damage,
                            unit.getStandardDamage().M.damage)* 2 >= unit.getFullArmor()) {
                    score++;
                }
                if (unit.getStandardDamage().S.damage >
                        unit.getStandardDamage().M.damage) {
                    score++;
                } else if (unit.getStandardDamage().S.damage >
                        unit.getStandardDamage().L.damage) {
                    score += 0.5;
                }
                if (unit.hasSUA(BattleForceSUA.MEL)
                        || unit.hasSUA(BattleForceSUA.TSM)) {
                    score++;
                }
                if (unit.hasSUA(BattleForceSUA.CR)
                        || unit.hasSUA(BattleForceSUA.RCA)
                        || unit.hasSUA(BattleForceSUA.RFA)) {
                    score++;
                }
                if (unit.hasSUA(BattleForceSUA.AMS)
                        || unit.hasSUA(BattleForceSUA.RAMS)) {
                    score++;
                }
                break;
            case MISSILE_BOAT:
                /* Any artillery piece or can do damage by indirect fire at long range */
                return (unit.getStandardDamage().L.damage > 0 && unit.hasSUA(BattleForceSUA.IF))
                        || unit.hasSUA(BattleForceSUA.ARTAIS)
                        || unit.hasSUA(BattleForceSUA.ARTAC)
                        || unit.hasSUA(BattleForceSUA.ARTBA)
                        || unit.hasSUA(BattleForceSUA.ARTCM5)
                        || unit.hasSUA(BattleForceSUA.ARTCM7)
                        || unit.hasSUA(BattleForceSUA.ARTCM9)
                        || unit.hasSUA(BattleForceSUA.ARTCM12)
                        || unit.hasSUA(BattleForceSUA.ARTT)
                        || unit.hasSUA(BattleForceSUA.ARTS)
                        || unit.hasSUA(BattleForceSUA.ARTLT)
                        || unit.hasSUA(BattleForceSUA.ARTTC)
                        || unit.hasSUA(BattleForceSUA.ARTSC)
                        || unit.hasSUA(BattleForceSUA.ARTLTC);
            case SCOUT:
                /* Fast (jump, WiGE, or VTOL helpful but not required), lightly armored, preference for short range */
                score += Math.min(0, speed - 8) * 0.5;
                score -= Math.max(0, unit.getFullArmor() - 4);
                if (unit.getMovementModes().contains("j") || unit.getMovementModes().contains("g")
                        || unit.getMovementModes().contains("v")) {
                    score++;
                }
                if (unit.hasSUA(BattleForceSUA.RCN)) {
                    score++;
                }
                if (unit.hasSUA(BattleForceSUA.BH)
                        || unit.hasSUA(BattleForceSUA.PRB)
                        || unit.hasSUA(BattleForceSUA.LPRB)
                        || unit.hasSUA(BattleForceSUA.WAT)) {
                    score++;
                }
                if (unit.hasSUA(BattleForceSUA.ECM)) {
                    score++;
                }
                if (unit.getStandardDamage().S.damage >
                        unit.getStandardDamage().M.damage) {
                    score++;
                } else if (unit.getStandardDamage().S.damage >
                        unit.getStandardDamage().L.damage) {
                    score += 0.5;
                }
                break;
            case SKIRMISHER:
                /* Fast, medium-heavy armor with preference for medium range */
                if (unit.getMovementModes().contains("j")) {
                    score += Math.min(0, speed - 8) * 0.5;
                } else {
                    score += Math.min(0, speed - 9) * 0.5;
                }
                score += Math.min(0, unit.getFullArmor() - 4) + Math.min(0, 8 - unit.getFullArmor());
                if (unit.getStandardDamage().M.damage >=
                        unit.getStandardDamage().S.damage) {
                    score += 0.5;
                }
                if (unit.getStandardDamage().M.damage >
                        unit.getStandardDamage().L.damage) {
                    score += 0.5;
                }
                break;
            case SNIPER:
                /* Can do damage at long range without LRMs */
                return unit.getStandardDamage().L.damage - unit.getLRM().L.damage > 0;
            case STRIKER:
                /* Fast and light-medium armor, preference for short range */
                score += Math.min(0, speed - 9) * 0.5;
                score -= Math.max(0, unit.getFullArmor() - 5);
                if (unit.getStandardDamage().S.damage >
                        unit.getStandardDamage().M.damage) {
                    score++;
                } else if (unit.getStandardDamage().S.damage >
                        unit.getStandardDamage().L.damage) {
                    score += 0.5;
                }
                break;
            case ATTACK_FIGHTER:
                /* Slow, preference for short range */
                score -= Math.max(0, speed - 5);
                if (unit.getStandardDamage().S.damage >
                        unit.getStandardDamage().M.damage) {
                    score++;
                } else if (unit.getStandardDamage().S.damage >
                        unit.getStandardDamage().L.damage) {
                    score += 0.5;
                }
                break;
            case DOGFIGHTER:
                /* Medium speed, preference for medium range */
                score += Math.min(0, speed - 5) + Math.min(0, 7 - speed) * 0.5;
                if (unit.getStandardDamage().M.damage >=
                        unit.getStandardDamage().S.damage) {
                    score += 0.5;
                }
                if (unit.getStandardDamage().M.damage >
                        unit.getStandardDamage().L.damage) {
                    score += 0.5;
                }
                break;
            case FAST_DOGFIGHTER:
                /* Fast with preference for medium range */
                score += Math.min(0, speed - 7) + Math.min(0, 9 - speed) * 0.5;
                if (unit.getStandardDamage().M.damage >=
                        unit.getStandardDamage().S.damage) {
                    score += 0.5;
                }
                if (unit.getStandardDamage().M.damage >
                        unit.getStandardDamage().L.damage) {
                    score += 0.5;
                }
                break;
            case FIRE_SUPPORT:
                /* Not too slow and can do damage at long range */
                if (unit.getStandardDamage().L.damage < 0.5) {
                    return false;
                }
                score += Math.min(0, speed - 5) + Math.min(0, 7 - speed);
                break;
            case INTERCEPTOR:
                /* Very fast, preference for damage at medium range */
                score += Math.min(0, speed - 10);
                if (unit.getStandardDamage().M.damage >=
                        unit.getStandardDamage().S.damage) {
                    score += 0.5;
                }
                if (unit.getStandardDamage().M.damage >
                        unit.getStandardDamage().L.damage) {
                    score += 0.5;
                }
                break;
            case TRANSPORT:
                /* Has transport capacity */
                return unit.hasSUA(BattleForceSUA.CK)
                        || unit.hasSUA(BattleForceSUA.IT)
                        || unit.hasSUA(BattleForceSUA.AT)
                        || unit.hasSUA(BattleForceSUA.PT)
                        || unit.hasSUA(BattleForceSUA.VTM)
                        || unit.hasSUA(BattleForceSUA.VTH)
                        || unit.hasSUA(BattleForceSUA.VTS)
                        || unit.hasSUA(BattleForceSUA.MT)
                        || unit.hasSUA(BattleForceSUA.CT)
                        || unit.hasSUA(BattleForceSUA.ST);
            default:
                break;
        }
        return score >= tolerance;
    }

    /* Convert all but initial letter(s) to lower case */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String word : name().split("_")) {
            sb.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    // PRIVATE

    private final Availability availableTo;

    UnitRole(Availability availableTo) {
        this.availableTo = availableTo;
    }

    enum Availability {
        GROUND(BTObject::isGround),
        AERO(BTObject::isAerospace),
        ALL(unit -> true);

        private final Predicate<BTObject> fits;

        Availability(Predicate<BTObject> fits) {
            this.fits = fits;
        }

        boolean fits(BTObject unit) {
            return fits.test(unit);
        }
    }
}
