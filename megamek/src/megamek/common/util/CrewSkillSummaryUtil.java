package megamek.common.util;

import java.text.MessageFormat;

import megamek.client.ui.Messages;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Tank;
import megamek.common.options.OptionsConstants;

public class CrewSkillSummaryUtil {
    private static final String SKILL_SUMMARY_PILOT = "{0}/{1}";
    private static final String SKILL_SUMMARY_RPG_PILOT = "{0}/{1}/{2}/{3}";
    private static final String SKILL_SUMMARY_LAM_PILOT = "M: {0}/{1} A: {2}/{3}";
    private static final String SKILL_SUMMARY_RPG_LAM_PILOT = "M: {0}/{1}/{2}/{3} A: {4}/{5}/{6}/{7}";
    private static final String SKILL_SUMMARY_GUNNERY = "{0}";
    private static final String SKILL_SUMMARY_RPG_GUNNERY = "{0}/{1}/{2}";
    private static final String SKILL_SUMMARY_LAM_GUNNERY = "{0}/{1}";
    private static final String SKILL_SUMMARY_RPG_LAM_GUNNERY = "M: {0}/{1}/{2} A: {3}/{4}/{5}";

    public static String getPilotSkillSummary(int gunnery, int gunneryL, int gunneryM, int gunneryB, int piloting, boolean rpgGunnery) {
        if (rpgGunnery) {
            return MessageFormat.format(SKILL_SUMMARY_RPG_PILOT, gunneryL, gunneryM, gunneryB, piloting);
        } else {
            return MessageFormat.format(SKILL_SUMMARY_PILOT, gunnery, piloting);
        }
    }

    public static String getPilotSkillSummary(String gunnery, String gunneryL, String gunneryM, String gunneryB, String piloting, boolean rpgGunnery) {
        if (rpgGunnery) {
            return MessageFormat.format(SKILL_SUMMARY_RPG_PILOT, gunneryL, gunneryM, gunneryB, piloting);
        } else {
            return MessageFormat.format(SKILL_SUMMARY_PILOT, gunnery, piloting);
        }
    }

    public static String getGunnerySkillSummary(int gunnery, int gunneryL, int gunneryM, int gunneryB, boolean rpgGunnery) {
        if (rpgGunnery) {
            return MessageFormat.format(SKILL_SUMMARY_RPG_GUNNERY, gunneryL, gunneryM, gunneryB);
        } else {
            return MessageFormat.format(SKILL_SUMMARY_GUNNERY, gunnery);
        }
    }

    public static String getGunnerySkillSummary(String gunnery, String gunneryL, String gunneryM, String gunneryB, boolean rpgGunnery) {
        if (rpgGunnery) {
            return MessageFormat.format(SKILL_SUMMARY_RPG_GUNNERY, gunneryL, gunneryM, gunneryB);
        } else {
            return MessageFormat.format(SKILL_SUMMARY_GUNNERY, gunnery);
        }
    }

    public static String getLAMPilotSkillSummary(int gunnery, int gunneryL, int gunneryM, int gunneryB, int piloting, int aeroGunnery, int aeroGunneryL, int aeroGunneryM, int aeroGunneryB, int aeroPiloting, boolean rpgGunnery) {
        if (rpgGunnery) {
            return MessageFormat.format(SKILL_SUMMARY_RPG_LAM_PILOT, gunneryL, gunneryM, gunneryB, piloting, aeroGunneryL, aeroGunneryM, aeroGunneryB, aeroPiloting);
        } else {
            return MessageFormat.format(SKILL_SUMMARY_LAM_PILOT, gunnery, piloting, aeroGunnery, aeroPiloting);
        }
    }

    public static String getLAMGunnerySkillSummary(int gunnery, int gunneryL, int gunneryM, int gunneryB, int aeroGunnery, int aeroGunneryL, int aeroGunneryM, int aeroGunneryB, boolean rpgGunnery) {
        if (rpgGunnery) {
            return MessageFormat.format(SKILL_SUMMARY_RPG_LAM_GUNNERY, gunneryL, gunneryM, gunneryB, aeroGunneryL, aeroGunneryM, aeroGunneryB);
        } else {
            return MessageFormat.format(SKILL_SUMMARY_LAM_GUNNERY, gunnery, aeroGunnery);
        }
    }
    
    /** 
     * Returns a descriptor string for the crew skills such as
     * "Gunnery / Piloting", depending on the game options and entity type.
     */
    public static String getSkillNames(final Entity entity) {
        final boolean rpgSkills = entity.getGame().getOptions().booleanOption(OptionsConstants.RPG_RPG_GUNNERY);
        
        String gunString = Messages.getString("BT.Gunnery");
        if (rpgSkills) {
            gunString = Messages.getString("CrewSkillSummary.GunneryLMB");
        }
        
        String pilotString = Messages.getString("BT.Piloting");
        if (entity instanceof Infantry) {
            pilotString = Messages.getString("BT.AntiMek");
        } else if (entity instanceof Tank) {
            pilotString = Messages.getString("BT.Driving");
        }
        
        return gunString + " / " + pilotString;
    }
}
