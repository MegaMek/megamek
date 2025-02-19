/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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

package megamek.client.bot.caspar.ai.utility.tw.context;

import megamek.ai.utility.StructOfUnitArrays;
import megamek.common.Entity;

import java.util.Comparator;
import java.util.List;

public class StructOfArraysEntity extends StructOfUnitArrays {

    public StructOfArraysEntity(List<Entity> entities) {
        super(entities.size());
        entities.sort(Comparator.comparingInt(Entity::getId));

        for (int i = 0; i < length; i++) {
            Entity entity = entities.get(i);
            id[i] = entity.getId();
            if (entity.getPosition() == null) {
                x[i] = Integer.MAX_VALUE;
                y[i] = Integer.MAX_VALUE;
            } else {
                x[i] = entity.getPosition().getX();
                y[i] = entity.getPosition().getY();
            }
            facing[i] = entity.getFacing();
            ownerId[i] = entity.getOwner().getId();
            teamId[i] = entity.getOwner().getTeam();
            maxRange[i] = entity.getMaxWeaponRange();
            role[i] = entity.getRole().ordinal();
        }
    }
}
