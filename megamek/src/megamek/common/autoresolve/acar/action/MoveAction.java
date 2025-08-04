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

package megamek.common.autoresolve.acar.action;

import megamek.common.Coords;
import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.handler.MoveActionHandler;

public class MoveAction implements Action {

    private final int formationId;
    private final int targetFormationId;
    private final Coords destination;

    public MoveAction(int formationId, int targetFormationId, Coords destination) {
        this.formationId = formationId;
        this.targetFormationId = targetFormationId;
        this.destination = destination;
    }

    @Override
    public int getEntityId() {
        return formationId;
    }

    public int getTargetFormationId() {
        return targetFormationId;
    }

    @Override
    public MoveActionHandler getHandler(SimulationManager gameManager) {
        return new MoveActionHandler(this, gameManager);
    }

    @Override
    public boolean isDataValid(SimulationContext context) {
        return context.getFormation(formationId).isPresent();
    }

    public Coords getDestination() {
        return destination;
    }

    @Override
    public String toString() {
        return "[MoveAction]: ID: " + formationId;
    }
}
