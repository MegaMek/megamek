/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.server.scriptedevent;

import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.IGame;
import megamek.server.trigger.Trigger;
import megamek.server.trigger.TriggerSituation;
import org.apache.logging.log4j.LogManager;

/**
 * This trigger reacts after the given unit (currently only Entity) is destroyed (note that it will react any
 * number of times).
 *
 * @see Entity#isDestroyed()
 * @see Entity#isCarcass()
 */
public class UnitKilledTrigger implements Trigger {

    private final int unitId;

    public UnitKilledTrigger(int unitId) {
        this.unitId = unitId;
    }

    @Override
    public boolean isTriggered(IGame game, TriggerSituation event) {
        if (game instanceof Game twGame) {
            Entity unit = twGame.getEntityFromAllSources(unitId);
            return (unit != null) && (unit.isDestroyed() || unit.isCarcass());
        } else {
            LogManager.getLogger().warn("UnitKilledTrigger is currently only available for TW games.");
            return false;
        }
    }
}
