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
package megamek.common.autoresolve.acar.handler;

import megamek.common.IEntityRemovalConditions;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.action.WithdrawAction;
import megamek.common.autoresolve.acar.report.IWithdrawReporter;
import megamek.common.autoresolve.acar.report.WithdrawReporter;
import megamek.common.autoresolve.damage.EntityFinalState;

import java.util.concurrent.atomic.AtomicInteger;

public class WithdrawActionHandler extends AbstractActionHandler {

    private final IWithdrawReporter reporter;

    public WithdrawActionHandler(WithdrawAction action, SimulationManager gameManager) {
        super(action, gameManager);
        this.reporter = WithdrawReporter.create(gameManager);
    }

    @Override
    public boolean cares() {
        return game().getPhase().isEnd();
    }

    @Override
    public void execute() {
        var withdraw = (WithdrawAction) getAction();
        var withdrawOpt = game().getFormation(withdraw.getEntityId());

        if (withdrawOpt.isEmpty()) {
            return;
        }

        var withdrawFormation = withdrawOpt.get();
        if (!withdrawFormation.isWithdrawing()) {
            if (withdrawFormation.isCrippled()) {
                reporter.reportStartingWithdrawForCrippled(withdrawFormation);
            } else {
                reporter.reportStartingWithdrawForOrder(withdrawFormation);
            }
            withdrawFormation.setWithdrawing(true);
        }

        var canWithdraw = (withdrawFormation.getPosition().coords().getX() == 0)
            || (withdrawFormation.getPosition().coords().getX() == (game().getBoardSize() - 1));

        if (canWithdraw) {
            // successful withdraw
            withdrawFormation.setDeployed(false);
            var formationEntity = withdrawFormation.getEntity();
            if (formationEntity != null) {
                formationEntity.setRemovalCondition(IEntityRemovalConditions.REMOVE_IN_RETREAT);
                formationEntity.setDeployed(false);
                game().applyDamageToEntityFromUnit(withdrawFormation.getUnits().get(0), formationEntity, EntityFinalState.CREW_AND_ENTITY_MUST_SURVIVE);
                game().addUnitToGraveyard(formationEntity);
            } else {
                for (var unit : withdrawFormation.getUnits()) {
                    for (var element : unit.getElements()) {
                        var optEntity = game().getEntity(element.getId());
                        if (optEntity.isPresent()) {
                            var entity = optEntity.get();
                            entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_IN_RETREAT);
                            entity.setDeployed(false);
                            entity.setSalvage(true);
                            entity.setDestroyed(false);
                            game().applyDamageToEntityFromUnit(unit, entity, EntityFinalState.CREW_AND_ENTITY_MUST_SURVIVE);
                            game().addUnitToGraveyard(entity);
                        }
                    }
                }
            }
            reporter.reportSuccessfulWithdraw(withdrawFormation);
            game().removeFormation(withdrawFormation);
        }
    }
}
