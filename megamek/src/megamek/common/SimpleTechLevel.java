/**
 * 
 */
package megamek.common;

import megamek.common.options.OptionsConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * An enum for the various rules levels
 * 
 * @author Neoancient
 *
 */
public enum SimpleTechLevel {
    INTRO ("Introductory"),
    STANDARD ("Standard"), 
    ADVANCED ("Advanced"),
    EXPERIMENTAL ("Experimental"),
    UNOFFICIAL ("Unofficial");

    private String strVal;

    SimpleTechLevel(String strVal) {
        this.strVal = strVal;
    }

    public static SimpleTechLevel parse(String strVal) {
        for (SimpleTechLevel lvl : values()) {
            if (strVal.equals(lvl.strVal)) {
                return lvl;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return strVal;
    }

    /**
     * @return The more advanced of two tech levels.
     */
    public static SimpleTechLevel max(SimpleTechLevel l1, SimpleTechLevel l2) {
        if (l1.compareTo(l2) < 0) {
            return l2;
        } else {
            return l1;
        }
    }

    /**
     * @return The less advanced of two tech levels.
     */
    public static SimpleTechLevel min(SimpleTechLevel l1, SimpleTechLevel l2) {
        if (l1.compareTo(l2) > 0) {
            return l2;
        } else {
            return l1;
        }
    }

    /**
     * @return The corresponding TechConstants.T_* value.
     */
    public int getCompoundTechLevel(boolean clan) {
        switch (this) {
            case INTRO:
                return TechConstants.T_INTRO_BOXSET;
            case STANDARD:
                return clan? TechConstants.T_CLAN_TW : TechConstants.T_IS_TW_NON_BOX;
            case ADVANCED:
                return clan? TechConstants.T_CLAN_ADVANCED : TechConstants.T_IS_ADVANCED;
            case EXPERIMENTAL:
                return clan? TechConstants.T_CLAN_EXPERIMENTAL : TechConstants.T_IS_EXPERIMENTAL;
            case UNOFFICIAL:
                return clan? TechConstants.T_CLAN_UNOFFICIAL: TechConstants.T_IS_UNOFFICIAL;
            default:
                return TechConstants.T_INTRO_BOXSET;
        }
    }

    /**
     * Finds simple tech level equivalent of compound tech base/rules level constant
     * 
     * @param level A TechConstants tech level constant
     * @return
     */
    public static SimpleTechLevel convertCompoundToSimple(int level) {
        switch (level) {
            case TechConstants.T_INTRO_BOXSET:
                return SimpleTechLevel.INTRO;
            case TechConstants.T_IS_TW_NON_BOX:
            case TechConstants.T_CLAN_TW:
            case TechConstants.T_IS_TW_ALL:
            case TechConstants.T_TW_ALL:
                return SimpleTechLevel.STANDARD;
            case TechConstants.T_IS_ADVANCED:
            case TechConstants.T_CLAN_ADVANCED:
                return SimpleTechLevel.ADVANCED;
            case TechConstants.T_IS_EXPERIMENTAL:
            case TechConstants.T_CLAN_EXPERIMENTAL:
                return SimpleTechLevel.EXPERIMENTAL;
            case TechConstants.T_IS_UNOFFICIAL:
            case TechConstants.T_CLAN_UNOFFICIAL:
                return SimpleTechLevel.UNOFFICIAL;
            default:
                return SimpleTechLevel.STANDARD;
        }
    }
    
    public static SimpleTechLevel getGameTechLevel(Game game) {
        return SimpleTechLevel.parse(game.getOptions().stringOption(OptionsConstants.ALLOWED_TECHLEVEL));
    }

    public static Map<Integer, String> getAllSimpleTechLevelCodeName() {
        Map<Integer, String> result = new HashMap();

        for (SimpleTechLevel simpleTechLevel : SimpleTechLevel.values()) {
            result.put(simpleTechLevel.ordinal(), simpleTechLevel.strVal);
        }

        return result;
    }
}