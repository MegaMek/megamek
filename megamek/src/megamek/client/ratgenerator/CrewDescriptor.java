/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ratgenerator;

import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.Gender;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.UnitType;

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

    /**
     * How much better a Clan warrior-caste crew is than the same rating elsewhere, which is what puts
     * a Clan Regular Mek crew at 3/4 where an Inner Sphere one sits at 4/5.
     *
     * <p>Doubles as the ceiling on the whole Clan modifier: the rating scaling may pull a crew below
     * it but not push one past it. See {@code setSkills}.</p>
     */
    private static final int CLAN_WARRIOR_CASTE_BONUS = 2;

    /** The Word of Blake Shadow Divisions, whose crews are a cut above the rest of the faction. */
    private static final String SHADOW_DIVISION_FACTION = "WOB.SD";

    // Skill values for the two levels above elite, matching megamek.common.enums.SkillLevel's
    // gunnery/piloting pairs. Kept here rather than derived so the escalation below cannot drift from
    // the thresholds the rest of the codebase reports against.
    private static final int HEROIC_GUNNERY = 1;
    private static final int HEROIC_PILOTING = 2;
    private static final int LEGENDARY_GUNNERY = 0;
    private static final int LEGENDARY_PILOTING = 1;

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

        // Give up and use the default
        return RandomNameGenerator.getInstance().generate(gender, false, RandomNameGenerator.KEY_DEFAULT_FACTION);
    }

    /**
     * Assigns skills based on the tables in TW, p. 271-3, with supplemental mods based on the BattleForce rules,
     * StratOps, p. 320-1
     */
    private void setSkills() {
        boolean clan = RATGenerator.getInstance().getFaction(assignment.getFaction()).isClan();

        int experience;
        if (null == assignment.getExperience()) {
            experience = randomExperienceLevel();
        } else {
            experience = SKILL_GREEN + assignment.getExperience();
        }

        int ratingLevel = assignment.getRatingLevel();
        int ratingLevels = assignment.getFactionRec().getRatingLevels().size();
        boolean isSupportRole = assignment.getRoles().contains(MissionRole.SUPPORT);
        int bonus = clan
                          ? clanSkillBonus(ratingLevel, ratingLevels, assignment.getUnitType(),
                                isSupportRole)
                          : innerSphereSkillBonus(ratingLevel, ratingLevels, isSupportRole,
                                assignment.getFaction());

        gunnery = randomSkillRating(GUNNERY_SKILL_TABLE, experience, bonus);
        boolean hasPilotingSkill = (assignment.getUnitType() == null)
              || !assignment.getUnitType().equals(UnitType.INFANTRY)
              || assignment.getRoles().contains(MissionRole.ANTI_MEK);
        if (hasPilotingSkill) {
            piloting = randomSkillRating(PILOTING_SKILL_TABLE, experience, bonus);
        } else {
            piloting = 8;
        }

        int[] escalated = escalateExceptionalCrew(experience, gunnery, piloting, hasPilotingSkill);
        gunnery = escalated[0];
        piloting = escalated[1];
    }

    /**
     * Gives an elite crew a rare chance of being genuinely exceptional.
     *
     * <p>The skill tables stop at the elite row, and a plain 1d6 into that row reaches Heroic at best -
     * Legendary needs the highest columns, which only a force with the top equipment rating can roll
     * into. That made Legendary crews unreachable for most commands and guaranteed-ish for a few,
     * rather than rare everywhere.</p>
     *
     * <p>An elite crew therefore rolls once to escalate to Heroic, and again to reach Legendary, giving
     * roughly one Heroic in six elite crews and one Legendary in thirty-six. This mirrors how MekHQ
     * already produces exceptional support staff, so a standout MekWarrior is as plausible as a
     * standout technician.</p>
     *
     * <p>Skills are only ever improved: a crew that already rolled into the top columns keeps what it
     * earned.</p>
     *
     * @param experience       the crew's experience row
     * @param gunnery          the gunnery skill rolled from the tables
     * @param piloting         the piloting skill rolled from the tables
     * @param hasPilotingSkill whether this crew has a real piloting skill; foot infantry carry a fixed
     *                         value that must not be improved
     *
     * @return the possibly improved {@code { gunnery, piloting }} pair
     */
    static int[] escalateExceptionalCrew(int experience, int gunnery, int piloting,
          boolean hasPilotingSkill) {
        if ((experience < SKILL_ELITE) || (Compute.d6() < 6)) {
            return new int[] { gunnery, piloting };
        }
        int escalatedGunnery = Math.min(gunnery, HEROIC_GUNNERY);
        int escalatedPiloting = hasPilotingSkill ? Math.min(piloting, HEROIC_PILOTING) : piloting;

        if (Compute.d6() < 6) {
            return new int[] { escalatedGunnery, escalatedPiloting };
        }
        return new int[] { Math.min(escalatedGunnery, LEGENDARY_GUNNERY),
                           hasPilotingSkill ? Math.min(escalatedPiloting, LEGENDARY_PILOTING)
                                            : escalatedPiloting };
    }

    /**
     * The modifier added to a Clan crew's skill roll.
     *
     * <p>The warrior caste's advantage puts a Clan Regular Mek crew at 3/4 where an Inner Sphere one
     * sits at 4/5, and the rating scaling moves a crew around that - Solahma below it, front-line
     * formations towards it. That scaling is an expansion of the StratOps table, which named only
     * front-line, second-line and Solahma.</p>
     *
     * <p>The caste advantage is the ceiling rather than another step to climb on top of. Left
     * uncapped the two stacked, a front-line or Keshik formation reached +3 or +4, and that walks
     * clean off the good end of whichever experience row was asked for: at +4 a force generated as
     * Regular could not produce a single Regular crew, every one of them coming out Veteran or
     * better. Capped, the ladder lands where it should - Green 4/5, Regular 3/4, Veteran 2/3,
     * Elite 1/2 - while a poor rating still pulls a crew below it, which is the half of the scaling
     * that was doing real work.</p>
     *
     * @param ratingLevel   the formation's equipment rating
     * @param ratingLevels  how many ratings the faction has
     * @param unitType      what the crew fights in, or {@code null} where the force has not said
     * @param isSupportRole whether the formation is a support formation
     *
     * @return the modifier to add to the crew's skill roll
     */
    static int clanSkillBonus(int ratingLevel, int ratingLevels, @Nullable Integer unitType,
          boolean isSupportRole) {
        int bonus = ratingLevel - (ratingLevels / 2);
        if (unitType != null) {
            switch (unitType) {
                case UnitType.MEK, UnitType.BATTLE_ARMOR -> bonus += CLAN_WARRIOR_CASTE_BONUS;
                case UnitType.TANK, UnitType.VTOL, UnitType.NAVAL, UnitType.INFANTRY,
                      UnitType.CONV_FIGHTER -> bonus--;
                default -> {
                    // Every other unit type takes the rating scaling alone.
                }
            }
        }
        // Capped before the support penalty rather than after, so that a support formation is still a
        // step below a line one. Capping last swallowed the penalty whole for any formation already
        // at the ceiling, which quietly crewed a front-line support star as well as the line stars.
        bonus = Math.min(bonus, CLAN_WARRIOR_CASTE_BONUS);
        if (isSupportRole) {
            bonus--;
        }
        return bonus;
    }

    /**
     * The modifier added to an Inner Sphere crew's skill roll.
     *
     * <p>StratOps gives +1 for an A rating and -1 for an F. A few factions have no A-F ratings, so
     * the best rating takes the +1 and the worst the -1, unless there is only one rating to have.</p>
     *
     * @param ratingLevel   the formation's equipment rating
     * @param ratingLevels  how many ratings the faction has
     * @param isSupportRole whether the formation is a support formation
     * @param faction       the faction the force is generated for
     *
     * @return the modifier to add to the crew's skill roll
     */
    static int innerSphereSkillBonus(int ratingLevel, int ratingLevels, boolean isSupportRole,
          String faction) {
        int bonus = 0;
        if (ratingLevels > 1) {
            if (ratingLevel == 0) {
                bonus--;
            }
            if (ratingLevel == (ratingLevels - 1)) {
                bonus++;
            }
        }
        if (isSupportRole) {
            bonus--;
        }
        if (SHADOW_DIVISION_FACTION.equals(faction)) {
            bonus++;
        }
        return bonus;
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
          { 7, 7, 6, 6, 6, 6, 5, 5, 4 },
          { 6, 6, 6, 5, 5, 4, 4, 3, 3 },
          { 6, 5, 5, 4, 4, 3, 3, 2, 2 },
          { 5, 4, 4, 3, 3, 2, 2, 1, 1 }

    };

    private static final int[][] GUNNERY_SKILL_TABLE = {
          { 7, 6, 5, 5, 4, 4, 4, 4, 3 },
          { 5, 4, 4, 4, 4, 3, 3, 2, 2 },
          { 4, 4, 4, 3, 3, 2, 2, 1, 1 },
          { 4, 3, 3, 2, 2, 1, 1, 0, 0 }

    };

    /**
     * Selects the piloting or gunnery skill rating based on overall unit experience level and modifiers.
     *
     * @param table      Either the piloting or the gunnery skill table
     * @param experience The overall experience rating of the force
     * @param mod        Situational modifiers to the skill roll
     *
     * @return The skill rating
     */
    private int randomSkillRating(int[][] table, int experience, int mod) {
        int column = Math.clamp(experience, 0, table.length - 1);
        int roll = Compute.d6() + mod;
        if (roll < 0) {
            return table[column][0];
        } else {
            return table[column][Math.min(roll, table[column].length - 1)];
        }
    }

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
        Crew crew = new Crew(crewType, name, crewType.getCrewSlots(), gunnery, piloting, gender,
              assignment.getFactionRec().isClan(), null);
        // Randomize names and skills of crew, then assign the piloting and
        // gunnery skills generated for the unit to the correct slot.
        if (crewType.getCrewSlots() > 1) {
            int oldPiloting = crew.getPiloting();
            int oldGunnery = crew.getGunnery();
            setSkills();
            crew.setPiloting(piloting, 0);
            crew.setGunnery(gunnery, 0);
            for (int i = 1; i < crew.getSlotCount(); i++) {
                crew.setName(generateName(Gender.RANDOMIZE), i);
                setSkills();
                crew.setPiloting(piloting, i);
                crew.setGunnery(gunnery, i);
            }
            crew.setPiloting(oldPiloting, crew.getCurrentPilotIndex());
            crew.setGunnery(oldGunnery, crew.getCurrentGunnerIndex());
            setPiloting(oldPiloting);
            setGunnery(oldGunnery);
        }
        return crew;
    }
}
