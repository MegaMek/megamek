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
import megamek.client.generator.enums.SkillGeneratorType;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.common.units.Tank;

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
            if ((entity instanceof Mek) || (entity instanceof BattleArmor)) {
                bonus += 2;
            } else if ((entity instanceof Tank) || (entity instanceof Infantry)) {
                bonus -= 2;
            }
        } else if (type.isManeiDomini()) {
            bonus++;
        } else if (type.isSociety()) {
            if ((entity instanceof Mek) || (entity instanceof Tank)) {
                bonus -= 1;
            }
        }

        // Demands of dual training
        if (entity instanceof LandAirMek) {
            bonus -= 2;
        }

        return bonus;
    }
}
