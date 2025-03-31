/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 2 or (at your option) any later version,
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
 */
package megamek.client.bot.caspar.axis;

import megamek.client.bot.common.GameState;
import megamek.client.bot.common.Pathing;
import megamek.common.Entity;
import megamek.common.UnitRole;

import java.util.List;

import static megamek.common.UnitRole.AMBUSHER;
import static megamek.common.UnitRole.BRAWLER;
import static megamek.common.UnitRole.JUGGERNAUT;
import static megamek.common.UnitRole.MISSILE_BOAT;
import static megamek.common.UnitRole.SCOUT;
import static megamek.common.UnitRole.SKIRMISHER;
import static megamek.common.UnitRole.SNIPER;
import static megamek.common.UnitRole.STRIKER;


/**
 * Calculates the unit role
 * @author Luana Coppio
 */
public class UnitRoleBehaviorCalculator extends BaseAxisCalculator {

    private static final List<UnitRole> UNIT_ROLES = List.of(AMBUSHER,
          BRAWLER, JUGGERNAUT, MISSILE_BOAT, SCOUT, SKIRMISHER, SNIPER, STRIKER);

    @Override
    public float[] axis() {
        return new float[UNIT_ROLES.size()];
    }

    @Override
    public float[] calculateAxis(Pathing pathing, GameState gameState) {
        // This calculates the unit role
        float[] unitRole = axis();
        Entity unit = pathing.getEntity();
        int index = UNIT_ROLES.indexOf(unit.getRole());
        if (index != -1) {
            unitRole[index] = 1.0f;
        }
        return unitRole;
    }
}
