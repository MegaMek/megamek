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
import megamek.common.*;

public class ModifiedTotalWarfareSkillGenerator extends TotalWarfareSkillGenerator {
    //region Constructors
    public ModifiedTotalWarfareSkillGenerator() {
        super(SkillGeneratorMethod.MODIFIED_TOTAL_WARFARE);
    }
    //endregion Constructors

    @Override
    protected int determineBonus(final Entity entity, final boolean clanPilot,
                                 final boolean forceClan) {
        final SkillGeneratorType type = (forceClan && clanPilot) ? SkillGeneratorType.CLAN : getType();

        int bonus = 0;
        if (type.isClan()) {
            if ((entity instanceof Mech) || (entity instanceof BattleArmor)) {
                bonus += 2;
            } else if ((entity instanceof Tank) || (entity instanceof Infantry)) {
                bonus -= 2;
            }
        } else if (type.isManeiDomini()) {
            bonus++;
        } else if (type.isSociety()) {
            if ((entity instanceof Mech) || (entity instanceof Tank)) {
                bonus -= 1;
            }
        }

        // Demands of dual training
        if (entity instanceof LandAirMech) {
            bonus -= 2;
        }

        return bonus;
    }
}
