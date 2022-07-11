package megamek.common;

import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.BattleForceSPA;
import org.apache.logging.log4j.LogManager;

/**
 * Unit roles as defined by Alpha Strike Companion, used in formation building rules
 * in ASC and Campaign Operations
 * 
 * @author Neoancient
 */
public enum UnitRole {
    UNDETERMINED (false),
    AMBUSHER (true),
    BRAWLER (true),
    JUGGERNAUT (true),
    MISSILE_BOAT (true),
    SCOUT (true),
    SKIRMISHER (true),
    SNIPER (true),
    STRIKER (true),
    ATTACK_FIGHTER (false),
    DOGFIGHTER (false),
    FAST_DOGFIGHTER (false),
    FIRE_SUPPORT (false),
    INTERCEPTOR (false),
    TRANSPORT (false);

    private boolean ground;

    UnitRole(boolean ground) {
        this.ground = ground;
    }

    public boolean isGroundRole() {
        return ground;
    }

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
            case "attack figher":
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
            default:
                LogManager.getLogger().error("Could not parse AS Role " + role);
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
     *                  more or less by the book, while values &lt; 0 are more liberal and &gt; 0 are
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
        double score = 0;
        int speed = unit.getPrimaryMovementValue();
        switch (this) {
            case AMBUSHER:
                /* Slow, light armor, preference for short range */
                score -= Math.max(0, speed - 6) * 0.5;
                score -= Math.max(0, unit.getFullArmor() - 5);
                if (unit.hasSUA(BattleForceSPA.ECM)
                        || unit.hasSUA(BattleForceSPA.LECM)
                        || unit.hasSUA(BattleForceSPA.WAT)) {
                    score++;
                }
                if (unit.hasSUA(BattleForceSPA.STL)
                        || unit.hasSUA(BattleForceSPA.MAS)
                        || unit.hasSUA(BattleForceSPA.LMAS)) {
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
                if (unit.hasSUA(BattleForceSPA.MEL)
                        || unit.hasSUA(BattleForceSPA.TSM)) {
                    score++;
                }
                if (unit.hasSUA(BattleForceSPA.CR)
                        || unit.hasSUA(BattleForceSPA.RCA)
                        || unit.hasSUA(BattleForceSPA.RFA)) {
                    score++;
                }
                if (unit.hasSUA(BattleForceSPA.AMS)
                        || unit.hasSUA(BattleForceSPA.RAMS)) {
                    score++;
                }
                break;
            case MISSILE_BOAT:
                /* Any artillery piece or can do damage by indirect fire at long range */
                return (unit.getStandardDamage().L.damage > 0 && unit.hasSUA(BattleForceSPA.IF))
                        || unit.hasSUA(BattleForceSPA.ARTAIS)
                        || unit.hasSUA(BattleForceSPA.ARTAC)
                        || unit.hasSUA(BattleForceSPA.ARTBA)
                        || unit.hasSUA(BattleForceSPA.ARTCM5)
                        || unit.hasSUA(BattleForceSPA.ARTCM7)
                        || unit.hasSUA(BattleForceSPA.ARTCM9)
                        || unit.hasSUA(BattleForceSPA.ARTCM12)
                        || unit.hasSUA(BattleForceSPA.ARTT)
                        || unit.hasSUA(BattleForceSPA.ARTS)
                        || unit.hasSUA(BattleForceSPA.ARTLT)
                        || unit.hasSUA(BattleForceSPA.ARTTC)
                        || unit.hasSUA(BattleForceSPA.ARTSC)
                        || unit.hasSUA(BattleForceSPA.ARTLTC);
            case SCOUT:
                /* Fast (jump, WiGE, or VTOL helpful but not required), lightly armored, preference for short range */
                score += Math.min(0, speed - 8) * 0.5;
                score -= Math.max(0, unit.getFullArmor() - 4);
                if (unit.getMovementModes().contains("j") || unit.getMovementModes().contains("g")
                        || unit.getMovementModes().contains("v")) {
                    score++;
                }
                if (unit.hasSUA(BattleForceSPA.RCN)) {
                    score++;
                }
                if (unit.hasSUA(BattleForceSPA.BH)
                        || unit.hasSUA(BattleForceSPA.PRB)
                        || unit.hasSUA(BattleForceSPA.LPRB)
                        || unit.hasSUA(BattleForceSPA.WAT)) {
                    score++;
                }
                if (unit.hasSUA(BattleForceSPA.ECM)) {
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
                return unit.hasSUA(BattleForceSPA.CK)
                        || unit.hasSUA(BattleForceSPA.IT)
                        || unit.hasSUA(BattleForceSPA.AT)
                        || unit.hasSUA(BattleForceSPA.PT)
                        || unit.hasSUA(BattleForceSPA.VTM)
                        || unit.hasSUA(BattleForceSPA.VTH)
                        || unit.hasSUA(BattleForceSPA.VTS)
                        || unit.hasSUA(BattleForceSPA.MT)
                        || unit.hasSUA(BattleForceSPA.CT)
                        || unit.hasSUA(BattleForceSPA.ST);
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
}
