/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.equipment;

import java.util.List;

import megamek.common.game.Game;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Transporter for Lift Hoists as described TW p. 136
 */
public class LiftHoist extends ExternalCargo {
    private final static MMLogger logger = MMLogger.create(LiftHoist.class);

    private transient Entity entity;
    private int entityId = Entity.NONE;
    private int mountedId;

    public LiftHoist(Mounted<?> mounted, double tonnage) {
        super(tonnage, List.of(Entity.LOC_NONE));
        entity = mounted.getEntity();
        if (entity != null) {
            entityId = mounted.getEntity().getId();
        }
        mountedId = mounted.getEquipmentNum();
    }

    @Override
    public double getUnused() {
        if (isOperable()) {
            return super.getUnused();
        }
        return 0;
    }

    @Override
    public boolean canLoad(Entity unit) {
        return isOperable() && super.canLoad(unit);
    }

    @Override
    public void setGame(Game game) {
        super.setGame(game);
        if (entity != null) {
            entityId = entity.getId();
        } else if (entityId != Entity.NONE) {
            entity = game.getEntity(entityId);
        } else {
            logger.warn("LiftHoist has no entity or entityId");
        }
    }

    private boolean isOperable() {
        Mounted<?> mounted = getMounted();
        return mounted != null && !mounted.getEntity().isLocationBad(mounted.getLocation()) && mounted.isOperable();
    }

    private Mounted<?> getMounted() {
        Entity entity = game.getEntity(entityId);
        if (entity == null) {
            return null;
        }
        return entity.getEquipment(mountedId);
    }
}
