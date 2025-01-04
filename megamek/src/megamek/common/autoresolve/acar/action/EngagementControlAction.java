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
import megamek.common.autoresolve.acar.handler.EngagementAndControlActionHandler;
import megamek.common.autoresolve.component.EngagementControl;

public class EngagementControlAction implements Action {

    private final int formationId;
    private final int targetFormationId;
    private final EngagementControl engagementControl;

    public EngagementControlAction(int formationId, int targetFormationId, EngagementControl engagementControl) {
        this.formationId = formationId;
        this.targetFormationId = targetFormationId;
        this.engagementControl = engagementControl;
    }

    @Override
    public int getEntityId() {
        return formationId;
    }

    @Override
    public ActionHandler getHandler(SimulationManager gameManager) {
        return new EngagementAndControlActionHandler(this, gameManager);
    }

    @Override
    public boolean isDataValid(SimulationContext context) {
        return (context.getFormation(formationId).isPresent()
            && context.getFormation(targetFormationId).isPresent());
    }

    public EngagementControl getEngagementControl() {
        return engagementControl;
    }

    public int getTargetFormationId() {
        return targetFormationId;
    }

    @Override
    public String toString() {
        return "[EngagementControlAction]: ID: " + formationId + "; engagementControl: " + engagementControl;
    }
}
