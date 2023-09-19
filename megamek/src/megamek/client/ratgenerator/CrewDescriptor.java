/*
 * MegaMek - Copyright (C) 2016 The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ratgenerator;

import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.common.Compute;
import megamek.common.Crew;
import megamek.common.CrewType;
import megamek.common.UnitType;
import megamek.common.enums.Gender;

/**
 * Description of crew.
 * 
 * @author Neoancient
 */
public class CrewDescriptor {
    public static final int SKILL_GREEN = 0;
    public static final int SKILL_REGULAR = 1;
    public static final int SKILL_VETERAN = 2;
    public static final int SKILL_ELITE = 3;

    private String name;
    private String bloodname;
    private Gender gender;
    private int rank;
    private ForceDescriptor assignment;
    private int gunnery;
    private int piloting;
    private String title;

    public CrewDescriptor(ForceDescriptor assignment) {
        this.assignment = assignment;
        gender = RandomGenderGenerator.generate();
        name = generateName(gender);
        rank = assignment.getCoRank() == null ? 0 : assignment.getCoRank();
        title = null;
        setSkills();
    }

    private String generateName(Gender gender) {
        if (assignment.getFactionRec().isClan()) {
            return RandomNameGenerator.getInstance().generate(gender, true, RandomNameGenerator.KEY_DEFAULT_CLAN);
        } else if (!assignment.getFaction().contains(".")) {
            // Try to match our faction to one of the rng settings.
            for (String faction : RandomNameGenerator.getInstance().getFactions()) {
                if (assignment.getFaction().equalsIgnoreCase(faction)) {
                    return RandomNameGenerator.getInstance().generate(gender, false, faction);
                }
            }
        }
        // Go up one parent level and try again
        for (String parent : assignment.getFactionRec().getParentFactions()) {
            for (String faction : RandomNameGenerator.getInstance().getFactions()) {
                if (parent.equalsIgnoreCase(faction)) {
                    return RandomNameGenerator.getInstance().generate(gender, false, faction);
                }
            }
        }

        //Give up and use the default
        return RandomNameGenerator.getInstance().generate(gender, false, RandomNameGenerator.KEY_DEFAULT_FACTION);
    }

    /**
     * Assigns skills based on the tables in TW, p. 271-3, with supplemental mods based on the
     * BattleForce rules, StratOps, p. 320-1
     */
    private void setSkills() {
        boolean clan = RATGenerator.getInstance().getFaction(assignment.getFaction()).isClan();

        int experience;
        if (null == assignment.getExperience()) {
            experience = randomExperienceLevel();
        } else {
            experience = SKILL_GREEN + assignment.getExperience();
        }

        int bonus = 0;
        int ratingLevel = assignment.getRatingLevel();
        // StratOps gives a +1 for A and -1 for F. There are a few IS factions that don't have
        // A-F ratings, so we give +1 to the best and -1 to the worst, unless there is only one.
        // For Clan units we give a +/-1 for each rating level above or below second line. This
        // is an expansion of the StratOps table which only included FL, SL, and Solahma.
        int levels = assignment.getFactionRec().getRatingLevels().size();
        if (clan) {
            bonus = ratingLevel - levels / 2;
        } else if (levels > 1) {
            if (ratingLevel == 0) {
                bonus--;
            }
            if (ratingLevel == levels - 1) {
                bonus++;
            }
        }
        if (clan) {
            if (assignment.getUnitType() != null) {
                switch (assignment.getUnitType()) {
                    case UnitType.MEK:
                    case UnitType.BATTLE_ARMOR:
                        bonus += 2;
                        break;
                    case UnitType.TANK:
                    case UnitType.VTOL:
                    case UnitType.NAVAL:
                    case UnitType.INFANTRY:
                    case UnitType.CONV_FIGHTER:
                        bonus--;
                        break;
                }
            }
            if (assignment.getRoles().contains(MissionRole.SUPPORT)) {
                bonus--;
            }
        } else {
            if (assignment.getRoles().contains(MissionRole.SUPPORT)) {
                bonus--;
            }
            if (assignment.getFaction().equals("WOB.SD")) {
                bonus++;
            }
        }

        gunnery = randomSkillRating(GUNNERY_SKILL_TABLE, experience, bonus);
        if (assignment.getUnitType() != null && assignment.getUnitType().equals(UnitType.INFANTRY)
                && !assignment.getRoles().contains(MissionRole.ANTI_MEK)) {
            piloting = 8;
        } else {
            piloting = randomSkillRating(PILOTING_SKILL_TABLE, experience, bonus);
        }
    }

    /**
     * Determines random experience level using the table on TW p. 273.
     * 
     * @return The experience rating index, starting at green as zero.
     */
    public static int randomExperienceLevel() {
        int roll = Compute.d6(2);
        if (roll < 6) {
            return SKILL_GREEN;
        } else if (roll < 10) {
            return SKILL_REGULAR;
        } else if (roll < 12) {
            return SKILL_VETERAN;
        } else {
            return SKILL_ELITE;
        }
    }

    private static final int[][] PILOTING_SKILL_TABLE = {
            {7, 7, 6, 6, 6, 6, 5, 5, 4},
            {6, 6, 6, 5, 5, 4, 4, 3, 3},
            {6, 5, 5, 4, 4, 3, 3, 2, 2},
            {5, 4, 4, 3, 3, 2, 2, 1, 1}

    };

    private static final int[][] GUNNERY_SKILL_TABLE = {
            {7, 6, 5, 5, 4, 4, 4, 4, 3},
            {5, 4, 4, 4, 4, 3, 3, 2, 2},
            {4, 4, 4, 3, 3, 2, 2, 1, 1},
            {4, 3, 3, 2, 2, 1, 1, 0, 0}

    };

    /**
     * Selects the piloting or gunnery skill rating based on overall unit experience level and
     * modifiers.
     * 
     * @param table      Either the piloting or the gunnery skill table
     * @param experience The overall experience rating of the force
     * @param mod        Situational modifiers to the skill roll
     * @return           The skill rating
     */
    private int randomSkillRating(int[][] table, int experience, int mod) {
        int column = Math.max(0, Math.min(experience, table.length - 1));
        int roll = Compute.d6() + mod;
        if (roll < 0) {
            return table[column][0];
        } else {
            return table[column][Math.min(roll, table[column].length - 1)];
        }
    }

    /*
    public void assignBloodname() {
        final int[] ratingMods = {-3, -2, -1, 1, 4};
        int mod = 0;
        if (assignment.getRatingLevel() >= 0) {
            mod = ratingMods[assignment.getRatingLevel()];
        }
        if (assignment.getFaction().equals("BAN")) {
            mod -= 2;
        }

        int type = Bloodname.P_GENERAL;
        if (assignment.isElement()) {
            switch (assignment.getUnitType()) {
                case "Mek":
                    type = Bloodname.P_MECHWARRIOR;
                    break;
                case "Aero":
                    type = Bloodname.P_AEROSPACE;
                    break;
                case "Conventional Fighter":
                    type = Bloodname.P_AEROSPACE;
                    mod -= 2;
                    break;
                case "BattleArmor":
                    type = Bloodname.P_ELEMENTAL;
                    break;
                case "Infantry":
                    type = Bloodname.P_ELEMENTAL;
                    mod -= 2;
                    break;
                case "ProtoMek":
                    type = Bloodname.P_PROTOMECH;
                    break;
                case "Dropship":
                case "Warship":
                    if (assignment.getFaction().startsWith("CSR")) {
                        type = Bloodname.P_NAVAL;
                    } else {
                        mod -= 2;
                    }
                    break;
                case "Tank":
                case "VTOL":
                case "Jumpship":
                    return;
            }
        }
        int roll = Compute.d6(2) + mod -
                getGunnery() -
                getPiloting();
        if (assignment.getYear() <= 2950) roll++;
        if (assignment.getYear() > 3055) roll--;
        if (assignment.getYear() > 3065) roll--;
        if (assignment.getYear() > 3080) roll--;
        if (getRank() >= 30) {
            roll += getRank() - 30;
        }
        if (roll >= 6) {
            setBloodname(Bloodname.randomBloodname(assignment.getFaction().split("\\.")[0],
                    type, assignment.getYear()));
        }
    }
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBloodname() {
        return bloodname;
    }

    public void setBloodname(String bloodname) {
        this.bloodname = bloodname;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        if (rank > this.rank) { 
            this.rank = rank;
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ForceDescriptor getAssignment() {
        return assignment;
    }

    public void setAssignment(ForceDescriptor assignment) {
        this.assignment = assignment;
    }

    public int getGunnery() {
        return gunnery;
    }

    public void setGunnery(int gunnery) {
        this.gunnery = gunnery;
    }

    public int getPiloting() {
        return piloting;
    }

    public void setPiloting(int piloting) {
        this.piloting = piloting;
    }

    public Crew createCrew(CrewType crewType) {
        return new Crew(crewType, name, crewType.getCrewSlots(), gunnery, piloting, gender, null);
    }
}
