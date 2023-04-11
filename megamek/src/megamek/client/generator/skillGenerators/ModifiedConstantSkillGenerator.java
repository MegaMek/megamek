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
import megamek.common.*;
import megamek.common.enums.SkillLevel;

public class ModifiedConstantSkillGenerator extends ConstantSkillGenerator {
    //region Variable Declarations
    private static final long serialVersionUID = -3276792082665884815L;
    //endregion Variable Declarations

    //region Constructors
    public ModifiedConstantSkillGenerator() {
        super(SkillGeneratorMethod.MODIFIED_CONSTANT);
    }
    //endregion Constructors

    @Override
    public int[] generateRandomSkills(final Entity entity, final boolean forceClan) {
        if (getType().isManeiDomini()) {
            // JHS72 pg. 121, they are always considered elite
            return SkillLevel.ELITE.getDefaultSkillValues();
        }

        final int[] skills = super.generateRandomSkills(entity, forceClan);

        // Now we need to make all kinds of adjustments based on the table on pg. 40 of TW
        // Infantry Anti-'Mech skill should be one higher unless foot
        if (entity.isConventionalInfantry() && !entity.getMovementMode().isLegInfantry()) {
            skills[1]++;
        }

        // Gunnery is worse for support vehicles
        if (entity instanceof SupportTank) {
            skills[0]++;
        }

        // Now lets handle clanners
        if (getType().isClan() || (forceClan && entity.isClan())) {
            // 'Mechs and Battle Armour are better (but not ProtoMechs),
            // Tanks are worse, while Gunnery is worse for Infantry, Conventional Fighters
            // and Small Craft
            if ((entity instanceof Mech) || (entity instanceof BattleArmor)) {
                skills[0]--;
                skills[1]--;
            } else if (entity instanceof Tank) {
                skills[0]++;
                skills[1]++;
            } else if (entity.isConventionalInfantry() || (entity instanceof ConvFighter)
                    || ((entity instanceof SmallCraft) && !(entity instanceof Dropship))) {
                skills[0]++;
            }
        }
        
        //And finally, The Society, per WoRS p. 3
        
        if (getType().isSociety()) {
            // 'Mechs are Veteran with a -1 modifier to skills (simulated by dropping Piloting by 1).
            // Tanks are Regular with the same -1 modifier.
            // Infantry and ProtoMechs are both Regular.
            // Aerospace Fighters are Veteran.
            if (entity instanceof Mech) {
                skills[0]=3;
                skills[1]=5;
            } else if (entity instanceof Tank) {
                skills[0]=4;
                skills[1]=6;
            } else if (entity.isConventionalInfantry() || (entity instanceof Protomech)) {
                return SkillLevel.REGULAR.getDefaultSkillValues();
            } else if (entity instanceof Aero) {
                return SkillLevel.VETERAN.getDefaultSkillValues();
            }
        }

        return cleanReturn(entity, skills);
    }
}
