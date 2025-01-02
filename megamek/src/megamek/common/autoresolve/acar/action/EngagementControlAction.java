/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package megamek.common.autoresolve.acar.action;

import mekhq.campaign.autoresolve.acar.SimulationContext;
import mekhq.campaign.autoresolve.acar.SimulationManager;
import mekhq.campaign.autoresolve.acar.action.Action;
import mekhq.campaign.autoresolve.acar.action.ActionHandler;
import mekhq.campaign.autoresolve.acar.handler.EngagementAndControlActionHandler;
import mekhq.campaign.autoresolve.component.EngagementControl;

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
