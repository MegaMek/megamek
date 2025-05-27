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
package megamek.common.autoresolve.acar.phase;

import megamek.common.BoardLocation;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.IEntityRemovalConditions;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.report.DeploymentReport;
import megamek.common.autoresolve.acar.report.IDeploymentReport;
import megamek.common.autoresolve.component.Formation;
import megamek.common.autoresolve.damage.EntityFinalState;
import megamek.common.enums.GamePhase;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static megamek.common.Board.*;

public class DeploymentPhase extends PhaseHandler {
    private final int boardNorthSide;
    private final int boardOneThird;
    private final int boardTwoThirds;
    private final int deployZone = 3;
    private final int boardSouthSide = 0;
    private final IDeploymentReport deploymentReporter;
    private final AtomicBoolean headerLatch = new AtomicBoolean(false);
    public DeploymentPhase(SimulationManager simulationManager) {
        super(simulationManager, GamePhase.DEPLOYMENT);
        this.deploymentReporter = DeploymentReport.create(simulationManager);

        this.boardNorthSide = getContext().getBoardSize() - 1;
        this.boardOneThird = boardNorthSide / 3;
        this.boardTwoThirds = boardOneThird * 2;
    }

    @Override
    protected void executePhase() {
        headerLatch.set(false);
        // Automatically deploy all formations that are set to deploy this round
        getSimulationManager().getGame().getActiveFormations().stream()
            .filter( f-> !f.isDeployed())
            .filter( f-> f.getDeployRound() == getSimulationManager().getGame().getCurrentRound())
            .forEach(this::deployUnit);
    }

    private void deployUnit(Formation formation) {

        if (!headerLatch.getAndSet(true)) {
            deploymentReporter.deploymentRoundHeader(getSimulationManager().getGame().getCurrentRound());
        }

        var player = getContext().getPlayer(formation.getOwnerId());

        int startingPos;

        switch (player.getStartingPos()) {
            case START_NE:
            case START_NW:
                startingPos = boardNorthSide - deployZone - Compute.randomInt(deployZone);
                break;
            case START_N:
                startingPos = boardNorthSide - Compute.randomInt(deployZone);
                break;
            case START_E:
                startingPos = boardTwoThirds + Compute.randomInt((boardOneThird / 2)) - (boardOneThird / 2);
                break;


            case START_W:
                startingPos = boardOneThird + Compute.randomInt((boardOneThird / 2)) - (boardOneThird / 2);
                break;
            case START_SE:
            case START_SW:
                startingPos = boardSouthSide + deployZone + Compute.randomInt(deployZone);
                break;
            case START_S:
                startingPos = boardSouthSide + Compute.randomInt(deployZone);
                break;

            case START_ANY:
                startingPos = Compute.randomIntInclusive(boardNorthSide);
                break;
            case START_EDGE:
                startingPos = Compute.randomIntInclusive(boardNorthSide);
                if (startingPos < boardOneThird) {
                    startingPos = Compute.randomInt(boardOneThird);
                } else if (startingPos > boardTwoThirds) {
                    startingPos = boardTwoThirds + Compute.randomInt(boardOneThird);
                }
                break;
            case START_CENTER:
                startingPos = boardOneThird + Compute.randomInt(boardOneThird);
                break;
            case START_NONE:
            default:
                startingPos = -1;
                break;
        }

        getSimulationManager().setFormationAt(formation, BoardLocation.of(new Coords(startingPos, 0), 0));
        formation.setDeployed(true);
        var formationEntity = formation.getEntity();
        if (formationEntity != null) {
            formationEntity.setDeployed(true);
        }
        for (var unit : formation.getUnits()) {
            for (var element : unit.getElements()) {
                var optEntity = getSimulationManager().getGame().getEntity(element.getId());
                if (optEntity.isPresent()) {
                    var entity = optEntity.get();
                    entity.setDeployed(true);
                }
            }
        }

        deploymentReporter.reportDeployment(formation, new Coords(startingPos, 0));
    }
}
