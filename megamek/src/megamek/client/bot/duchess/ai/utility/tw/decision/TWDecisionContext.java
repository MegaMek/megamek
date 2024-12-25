/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
 *
 */
package megamek.client.bot.duchess.ai.utility.tw.decision;

import megamek.ai.utility.Agent;
import megamek.ai.utility.DecisionContext;
import megamek.ai.utility.World;
import megamek.common.Entity;
import megamek.common.options.OptionsConstants;

import java.util.*;

import static megamek.client.bot.princess.FireControl.getMaxDamageAtRange;


public class TWDecisionContext extends DecisionContext<Entity, Entity> {

    public TWDecisionContext(Agent<Entity, Entity> agent, World<Entity, Entity> world) {
        super(agent, world);
    }

    public TWDecisionContext(Agent<Entity, Entity> agent, World<Entity, Entity> world, Entity currentUnit) {
        super(agent, world, currentUnit);
    }

    public TWDecisionContext(Agent<Entity, Entity> agent, World<Entity, Entity> world, Entity currentUnit, List<Entity> targetUnits) {
        super(agent, world, currentUnit, targetUnits);
    }

    @Override
    public double calculateUnitMaxDamageAtRange(Entity unit, int enemyRange) {
        return getMaxDamageAtRange(unit, enemyRange,
            getWorld().useBooleanOption(OptionsConstants.ADVCOMBAT_TACOPS_LOS_RANGE),
            getWorld().useBooleanOption(OptionsConstants.ADVCOMBAT_TACOPS_RANGE));
    }

    @Override
    public double getBonusFactor(DecisionContext<Entity, Entity> lastContext) {
        return 0;
    }
}
