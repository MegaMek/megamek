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
import megamek.common.enums.SkillLevel;

import java.io.Serializable;

public abstract class AbstractSkillGenerator implements Serializable {
    //region Variable Declarations
    private static final long serialVersionUID = 8475341940660043659L;

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
     * Generates a random skill array
     * @param entity the Entity to generate a random skill array for
     * @return an integer array containing the (Gunnery, Piloting) skill values, or an alternative
     * pairing if applicable [(Gunnery, Anti-'Mech) for infantry]
     */
    public int[] generateRandomSkills(final Entity entity) {
        return generateRandomSkills(entity, false);
    }

    /**
     * Generates random skills for an entity based on the current settings of the random skills
     * generator, but does not assign those new skills to that entity
     * @param entity the Entity to generate a random skill array for
     * @param forceClan forces the type to be clan if the entity is a clan unit
     * @return an integer array containing the (Gunnery, Piloting) skill values, or an alternative
     * pairing if applicable [(Gunnery, Anti-'Mech) for infantry]
     */
    public abstract int[] generateRandomSkills(final Entity entity, final boolean forceClan);
}
