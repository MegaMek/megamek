/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.generator.skillGenerators;

import megamek.client.generator.enums.SkillGeneratorMethod;
import megamek.common.*;
import megamek.common.enums.SkillLevel;

public class ModifiedConstantSkillGenerator extends ConstantSkillGenerator {
    //region Constructors
    public ModifiedConstantSkillGenerator() {
        super(SkillGeneratorMethod.MODIFIED_CONSTANT);
    }
    //endregion Constructors

    @Override
    public int[] generateRandomSkills(final Entity entity, final boolean clanPilot, final boolean forceClan) {
        if (getType().isManeiDomini()) {
            // JHS72 pg. 121, they are always considered elite
            return SkillLevel.ELITE.getDefaultSkillValues();
        }

        final int[] skills = super.generateRandomSkills(entity, clanPilot, forceClan);

        // Now we need to make all kinds of adjustments based on the table on pg. 40 of TW
        // Infantry Anti-'Mek skill should be one higher unless foot
        if (entity.isConventionalInfantry() && !entity.getMovementMode().isLegInfantry()) {
            skills[1]++;
        }

        // Gunnery is worse for support vehicles
        if (entity instanceof SupportTank) {
            skills[0]++;
        }

        // Now lets handle clan pilots
        if (getType().isClan() || (forceClan && clanPilot)) {
            // 'Meks and Battle Armour are better (but not ProtoMeks),
            // Tanks are worse, while Gunnery is worse for Infantry, Conventional Fighters
            // and Small Craft
            if ((entity instanceof Mek) || (entity instanceof BattleArmor)) {
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
            // 'Meks are Veteran with a -1 modifier to skills (simulated by dropping Piloting by 1).
            // Tanks are Regular with the same -1 modifier.
            // Infantry and ProtoMeks are both Regular.
            // Aerospace Fighters are Veteran.
            if (entity instanceof Mek) {
                skills[0] = 3;
                skills[1] = 5;
            } else if (entity instanceof Tank) {
                skills[0] = 4;
                skills[1] = 6;
            } else if (entity.isConventionalInfantry() || (entity instanceof ProtoMek)) {
                return SkillLevel.REGULAR.getDefaultSkillValues();
            } else if (entity instanceof Aero) {
                return SkillLevel.VETERAN.getDefaultSkillValues();
            }
        }

        return cleanReturn(entity, skills);
    }
}
