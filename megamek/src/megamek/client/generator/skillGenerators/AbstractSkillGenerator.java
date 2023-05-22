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
package megamek.client.generator.skillGenerators;

import megamek.client.generator.enums.SkillGeneratorMethod;
import megamek.client.generator.enums.SkillGeneratorType;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.LAMPilot;
import megamek.common.enums.SkillLevel;

public abstract class AbstractSkillGenerator {
    //region Variable Declarations
    private final SkillGeneratorMethod method;
    private SkillLevel level;
    private SkillGeneratorType type;
    private boolean forceClose; // Forces Piloting to be one worse than Gunnery
    //endregion Variable Declarations

    //region Constructors
    protected AbstractSkillGenerator(final SkillGeneratorMethod method) {
        this.method = method;
        setLevel(SkillLevel.REGULAR);
        setType(SkillGeneratorType.INNER_SPHERE);
        setForceClose(false);
    }
    //endregion Constructors

    //region Getters/Setters
    public SkillGeneratorMethod getMethod() {
        return method;
    }

    public SkillLevel getLevel() {
        return level;
    }

    public void setLevel(final SkillLevel level) {
        this.level = level;
    }

    public SkillGeneratorType getType() {
        return type;
    }

    public void setType(final SkillGeneratorType type) {
        this.type = type;
    }

    public boolean isForceClose() {
        return forceClose;
    }

    public void setForceClose(final boolean forceClose) {
        this.forceClose = forceClose;
    }
    //endregion Getters/Setters

    /**
     * Generates random skills based on an entity crewmember by crewmember, and then assigns the
     * values to the crew before sorting them.
     * @param entity the Entity whose skills are to be randomly set
     */
    public void setRandomSkills(final Entity entity) {
        setRandomSkills(entity, false);
    }

    /**
     * Generates random skills based on an entity crewmember by crewmember, and then assigns the
     * values to the crew before sorting them.
     * @param entity the Entity whose skills are to be randomly set
     * @param forceClan forces the type to be clan if the crew are led by a clan pilot
     */
    public void setRandomSkills(final Entity entity, final boolean forceClan) {
        for (int i = 0; i < entity.getCrew().getSlotCount(); i++) {
            int[] skills = generateRandomSkills(entity, forceClan);
            entity.getCrew().setGunnery(skills[0], i);
            entity.getCrew().setGunneryL(skills[0], i);
            entity.getCrew().setGunneryM(skills[0], i);
            entity.getCrew().setGunneryB(skills[0], i);
            entity.getCrew().setPiloting(skills[1], i);
            if (entity.getCrew() instanceof LAMPilot) {
                skills = generateRandomSkills(entity, forceClan);
                ((LAMPilot) entity.getCrew()).setGunneryAero(skills[0]);
                ((LAMPilot) entity.getCrew()).setPilotingAero(skills[1]);
            }
        }
        entity.getCrew().sortRandomSkills();
    }

    /**
     * Generates random skills for an entity based on the current settings of the random skill
     * generator, but does not assign those new skills to that entity
     * @param entity the Entity to generate a random skill array for
     * @return an integer array containing the (Gunnery, Piloting) skill values, or an alternative
     * pairing if applicable [(Gunnery, Anti-'Mech) for infantry]
     */
    public int[] generateRandomSkills(final Entity entity) {
        return generateRandomSkills(entity, false);
    }

    /**
     * Generates random skills for an entity based on the current settings of the random skill
     * generator, but does not assign those new skills to that entity. The return value MUST be
     * cleaned with cleanReturn for this setup to work properly.
     * @param entity the Entity to generate a random skill array for
     * @param forceClan forces the type to be clan if the entity is a clan unit
     * @return an integer array containing the (Gunnery, Piloting) skill values, or an alternative
     * pairing if applicable [(Gunnery, Anti-'Mech) for infantry]
     */
    public int[] generateRandomSkills(final Entity entity, final boolean forceClan) {
        return generateRandomSkills(entity, entity.getCrew().isClanPilot(), forceClan);
    }

    /**
     * Generates random skills for an entity based on the current settings of the random skill
     * generator, but does not assign those new skills to that entity. The return value MUST be
     * cleaned with cleanReturn for this setup to work properly.
     * @param entity the Entity to generate a random skill array for
     * @param clanPilot if the crew to generate a random skills array for are a clan crew
     * @param forceClan forces the type to be clan if the crew are a clan crew
     * @return an integer array containing the (Gunnery, Piloting) skill values, or an alternative
     * pairing if applicable [(Gunnery, Anti-'Mech) for infantry]
     */
    public abstract int[] generateRandomSkills(final Entity entity, final boolean clanPilot,
                                               final boolean forceClan);

    /**
     * This cleans up the return value before the final return, and by doing so to handling two
     * specific use cases. The first is handling Infantry Anti-'Mech skill generation properly by
     * directly tying it into entity being Anti-'Mech trained. The second is the force close option,
     * which forces piloting to be gunnery plus one
     * @param entity the entity the skill is being generated for
     * @param skills the skill array to cleanup before the final return
     * @return the modified skill array
     */
    protected int[] cleanReturn(final Entity entity, final int... skills) {
        // For conventional infantry, piloting doubles as anti-mek skill, and this is set
        // based on whether the unit has anti-mek training, which gets set in the BLK file.
        // We therefore check if they are anti-mek trained before setting
        if (entity.isConventionalInfantry() && !((Infantry) entity).isAntiMekTrained()) {
            skills[1] = Infantry.ANTI_MECH_SKILL_UNTRAINED;
        } else if (isForceClose()) {
            skills[1] = skills[0] + 1;
        }

        return skills;
    }
}
