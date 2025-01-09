/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.common.autoresolve.acar.action;

import megamek.common.autoresolve.acar.SimulationContext;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.handler.MoraleCheckActionHandler;

public class MoraleCheckAction implements Action {

    private final int formationId;

    public MoraleCheckAction(int formationId) {
        this.formationId = formationId;
    }

    @Override
    public int getEntityId() {
        return formationId;
    }

    @Override
    public ActionHandler getHandler(SimulationManager gameManager) {
        return new MoraleCheckActionHandler(this, gameManager);
    }

    @Override
    public boolean isDataValid(SimulationContext context) {
        return context.getFormation(formationId).isPresent();
    }

    @Override
    public String toString() {
        return "[MoraleCheckAction]: ID: " + formationId;
    }
}
