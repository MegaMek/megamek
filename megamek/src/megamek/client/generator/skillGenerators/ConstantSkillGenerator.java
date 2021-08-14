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
import megamek.common.Entity;

public class ConstantSkillGenerator extends AbstractSkillGenerator {
    //region Variable Declarations
    private static final long serialVersionUID = -7927373286417045956L;
    //endregion Variable Declarations

    //region Constructors
    public ConstantSkillGenerator() {
        this(SkillGeneratorMethod.CONSTANT);
    }

    protected ConstantSkillGenerator(final SkillGeneratorMethod method) {
        super(method);
    }
    //endregion Constructors

    /**
     * This returns the unmodified default random skill value, which is a set of constants
     *
     * @param entity the Entity to generate a random skill array for
     * @param forceClan forces the type to be clan if the entity is a clan unit
     * @return an integer array containing the (Gunnery, Piloting) skill values, or an alternative
     * pairing if applicable [(Gunnery, Anti-'Mech) for infantry]
     */
    @Override
    public int[] generateRandomSkills(final Entity entity, final boolean forceClan) {
        return cleanReturn(entity, getLevel().getDefaultSkillValues());
    }
}
