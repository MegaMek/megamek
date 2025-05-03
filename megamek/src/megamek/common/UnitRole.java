/*
 * Copyright (C) 2023-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.common;

import static megamek.common.UnitRole.Availability.AERO;
import static megamek.common.UnitRole.Availability.ALL;
import static megamek.common.UnitRole.Availability.GROUND;

import java.util.Arrays;
import java.util.function.Predicate;

import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.logging.MMLogger;

/**
 * Unit roles as defined by Alpha Strike Companion, used in formation building
 * rules in ASC and Campaign Operations
 *
 * @author Neoancient, Luana Coppio
 */
public enum UnitRole {

    /**
     * This is the default role; given to units where the role definition is
     * missing.
     */
    UNDETERMINED(ALL),

    /** Shows that this unit intentionally has no combat role. */
    NONE(ALL),

    /** Ground unit roles */
    AMBUSHER(GROUND),
    BRAWLER(GROUND),
    JUGGERNAUT(GROUND),
    MISSILE_BOAT(GROUND),
    SCOUT(GROUND),
    SKIRMISHER(GROUND),
    SNIPER(GROUND),
    STRIKER(GROUND),

    /** Aerospace unit roles */
    ATTACK_FIGHTER(AERO),
    DOGFIGHTER(AERO),
    FAST_DOGFIGHTER(AERO),
    FIRE_SUPPORT(AERO),
    INTERCEPTOR(AERO),
    TRANSPORT(AERO);

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

    /**
     * @param unit The unit to check.
     * @return True when the given unit may use this role. Used in MML.
     */
    public boolean isAvailableTo(BTObject unit) {
        return availableTo.fits(unit) && !unit.isUnitGroup();
    }

    /**
     * Checks if this role is the same as the given role or any of the other
     * @param role Role to check against.
     * @param otherRoles The other roles to check against.
     * @return True when this role is the same as any of the given roles.
     */
    public boolean isAnyOf(UnitRole role, UnitRole... otherRoles) {
        return (this == role) || Arrays.stream(otherRoles).anyMatch(otherRole -> this == otherRole);
    }

    /** @return True when this role is not UNDETERMINED. (Returns true for NONE.) */
    public boolean hasRole() {
        return this != UNDETERMINED;
    }

    /**
     * Parses the given string into the UnitRole if possible and returns it. If it
     * can't parse the string, logs an error and returns UNDETERMINED. Does not
     * return null.
     *
     * @return The UnitRole given as a string or UNDETERMINED.
     */
    public static UnitRole parseRole(String role) {
        return switch (role.strip().toLowerCase()) {
            case "ambusher" -> AMBUSHER;
            case "brawler" -> BRAWLER;
            case "juggernaut" -> JUGGERNAUT;
            case "missile_boat", "missile boat" -> MISSILE_BOAT;
            case "scout" -> SCOUT;
            case "skirmisher" -> SKIRMISHER;
            case "sniper" -> SNIPER;
            case "striker" -> STRIKER;
            case "attack_fighter", "attack fighter", "attack" -> ATTACK_FIGHTER;
            case "dogfighter" -> DOGFIGHTER;
            case "fast_dogfighter", "fast dogfighter" -> FAST_DOGFIGHTER;
            case "fire_support", "fire support", "fire-support" -> FIRE_SUPPORT;
            case "interceptor" -> INTERCEPTOR;
            case "transport" -> TRANSPORT;
            case "none" -> NONE;
            default -> {
                MMLogger.create(UnitRole.class).warn("Could not parse role: {}", role);
                yield UNDETERMINED;
            }
        };
    }

    /**
     * Applies the criteria from Alpha Strike Companion to determine whether a unit
     * qualifies for a particular role. As the canon unit roles do not themselves
     * adhere strictly to the guidelines, there is some allowance for fuzziness in
     * applying the criteria by computing a score. Stats outside the given ranges
     * lower the score, and special abilities that are useful for a role raise the
     * score.
     * This method calculates AlphaStrike statistics for the Entity as the first
     * step in the calculation.
     *
     * @param entity The unit to be checked for role qualification
     * @return Boolean value indicating whether the unit meets the qualifications
     *         for this role.
     */
    public boolean qualifiesForRole(Entity entity) {
        return qualifiesForRole(ASConverter.convert(entity), 0);
    }

    /**
     * <p>Applies the criteria from Alpha Strike Companion to determine whether a unit
     * qualifies for a particular role. As the canon unit roles do not themselves
     * adhere strictly to the guidelines, there is some allowance for fuzziness in
     * applying the criteria by computing a score. Stats outside the given ranges
     * lower the score, and special abilities that are useful for a role raise the
     * score.</p>
     * <p>This method calculates AlphaStrike statistics for the Entity as the first step in the calculation.</p>
     * @param entity    The unit to be checked for role qualification
     * @param tolerance A measure of how strictly to apply the qualifications. A value of zero is more or less by the
     *                 book, while values below 0 are more liberal and above 0 are stricter.
     * @return Boolean value indicating whether the unit meets the qualifications
     *         for this role.
     */
    public boolean qualifiesForRole(Entity entity, double tolerance) {
        return qualifiesForRole(ASConverter.convert(entity), tolerance);
    }

    /**
     * Applies the criteria from Alpha Strike Companion to determine whether a unit qualifies for a particular role.
     * As the canon unit roles do not themselves adhere strictly to the guidelines, there is some allowance for
     * fuzziness in applying the criteria by computing a score. Stats outside the given ranges lower the score, and
     * special abilities that are useful for a role raise the score.
     *
     * @param unit The unit to be checked for role qualification
     * @return Boolean value indicating whether the unit meets the qualifications
     *         for this role.
     */
    public boolean qualifiesForRole(AlphaStrikeElement unit) {
        return qualifiesForRole(unit, 0);
    }

    /**
     * Applies the criteria from Alpha Strike Companion to determine whether a unit qualifies for a particular role.
     * As the canon unit roles do not themselves adhere strictly to the guidelines, there is some allowance for
     * fuzziness in applying the criteria by computing a score. Stats outside the given ranges lower the score, and
     * special abilities that are useful for a role raise the score.
     *
     * @param unit      The unit to be checked for role qualification
     * @param tolerance A measure of how strictly to apply the qualifications. A value of zero is more or less by the
     *                 book, while values &lt; 0 are more liberal and &gt; 0 are stricter.
     * @return Boolean value indicating whether the unit meets the qualifications for this role.
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
                if (unit.getStandardDamage().S.damage > unit.getStandardDamage().M.damage) {
                    score++;
                } else if (unit.getStandardDamage().S.damage > unit.getStandardDamage().L.damage) {
                    score += 0.5;
                }
                break;
            case BRAWLER:
                /* Not too slow, preference for medium range */
                score += Math.min(0, speed - 8);
                if (unit.getStandardDamage().M.damage >= unit.getStandardDamage().S.damage) {
                    score += 0.5;
                }
                if (unit.getStandardDamage().M.damage > unit.getStandardDamage().L.damage) {
                    score += 0.5;
                }
                break;
            case JUGGERNAUT:
                /* Slow and heavily armored and armed, preference for short range */
                score -= Math.max(0, speed - 6) * 0.5;
                /*
                 * Per ASC, a Juggernaut should have an armor value of 7, but there are a large
                 * number of smaller units with lower armor values that have an official role of
                 * juggernaut.
                 */
                score += Math.min(0, unit.getFullArmor() - (unit.getSize() + 4));
                if (Math.max(unit.getStandardDamage().S.damage,
                        unit.getStandardDamage().M.damage) * 2 >= unit.getFullArmor()) {
                    score++;
                }
                if (unit.getStandardDamage().S.damage > unit.getStandardDamage().M.damage) {
                    score++;
                } else if (unit.getStandardDamage().S.damage > unit.getStandardDamage().L.damage) {
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
                /*
                 * Fast (jump, WiGE, or VTOL helpful but not required), lightly armored,
                 * preference for short range
                 */
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
                if (unit.getStandardDamage().S.damage > unit.getStandardDamage().M.damage) {
                    score++;
                } else if (unit.getStandardDamage().S.damage > unit.getStandardDamage().L.damage) {
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
                if (unit.getStandardDamage().M.damage >= unit.getStandardDamage().S.damage) {
                    score += 0.5;
                }
                if (unit.getStandardDamage().M.damage > unit.getStandardDamage().L.damage) {
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
                if (unit.getStandardDamage().S.damage > unit.getStandardDamage().M.damage) {
                    score++;
                } else if (unit.getStandardDamage().S.damage > unit.getStandardDamage().L.damage) {
                    score += 0.5;
                }
                break;
            case ATTACK_FIGHTER:
                /* Slow, preference for short range */
                score -= Math.max(0, speed - 5);
                if (unit.getStandardDamage().S.damage > unit.getStandardDamage().M.damage) {
                    score++;
                } else if (unit.getStandardDamage().S.damage > unit.getStandardDamage().L.damage) {
                    score += 0.5;
                }
                break;
            case DOGFIGHTER:
                /* Medium speed, preference for medium range */
                score += Math.min(0, speed - 5) + Math.min(0, 7 - speed) * 0.5;
                if (unit.getStandardDamage().M.damage >= unit.getStandardDamage().S.damage) {
                    score += 0.5;
                }
                if (unit.getStandardDamage().M.damage > unit.getStandardDamage().L.damage) {
                    score += 0.5;
                }
                break;
            case FAST_DOGFIGHTER:
                /* Fast with preference for medium range */
                score += Math.min(0, speed - 7) + Math.min(0, 9 - speed) * 0.5;
                if (unit.getStandardDamage().M.damage >= unit.getStandardDamage().S.damage) {
                    score += 0.5;
                }
                if (unit.getStandardDamage().M.damage > unit.getStandardDamage().L.damage) {
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
                if (unit.getStandardDamage().M.damage >= unit.getStandardDamage().S.damage) {
                    score += 0.5;
                }
                if (unit.getStandardDamage().M.damage > unit.getStandardDamage().L.damage) {
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

    @Override
    public String toString() {
        return capitalizedName;
    }

    private final Availability availableTo;
    private final String capitalizedName;

    UnitRole(Availability availableTo) {
        this.availableTo = availableTo;
        this.capitalizedName = capitalizeName();
    }

    private String capitalizeName() {
        StringBuilder sb = new StringBuilder();
        for (String word : name().split("_")) {
            sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString();
    }
}
