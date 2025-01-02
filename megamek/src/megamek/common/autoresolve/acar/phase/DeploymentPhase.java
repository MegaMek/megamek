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

package megamek.common.autoresolve.acar.phase;

import megamek.common.enums.GamePhase;
import mekhq.campaign.autoresolve.acar.SimulationManager;

public class DeploymentPhase extends PhaseHandler {

    public DeploymentPhase(SimulationManager simulationManager) {
        super(simulationManager, GamePhase.DEPLOYMENT);
    }

    @Override
    protected void executePhase() {
        // Automatically deploy all formations that are set to deploy this round
        getSimulationManager().getGame().getActiveFormations().stream()
            .filter( f-> !f.isDeployed())
            .filter( f-> f.getDeployRound() == getSimulationManager().getGame().getCurrentRound())
            .forEach( f-> f.setDeployed(true));
    }
}
