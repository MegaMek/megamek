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
import megamek.common.autoresolve.acar.report.WithdrawReporter;

import java.util.concurrent.atomic.AtomicInteger;

public class WithdrawActionHandler extends AbstractActionHandler {

    private final WithdrawReporter reporter;

    public WithdrawActionHandler(WithdrawAction action, SimulationManager gameManager) {
        super(action, gameManager);
        this.reporter = new WithdrawReporter(gameManager.getGame(), this::addReport);
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
            withdrawFormation.setWithdrawing(true);
        }

        var canWithdraw = (withdrawFormation.getPosition().coords().getX() == 0)
            || (withdrawFormation.getPosition().coords().getX() == (game().getBoardSize() - 1));

        if (canWithdraw) {
            // successful withdraw
            withdrawFormation.setDeployed(false);
            AtomicInteger unitWithdrawn = new AtomicInteger();
            for (var unit : withdrawFormation.getUnits()) {
                for (var element : unit.getElements()) {
                    game().getEntity(element.getId()).ifPresent(entity -> {
                        // only withdraw live units
                        if (entity.getRemovalCondition() != IEntityRemovalConditions.REMOVE_IN_RETREAT &&
                            entity.getRemovalCondition() != IEntityRemovalConditions.REMOVE_DEVASTATED &&
                            entity.getRemovalCondition() != IEntityRemovalConditions.REMOVE_SALVAGEABLE &&
                            entity.getRemovalCondition() != IEntityRemovalConditions.REMOVE_PUSHED &&
                            entity.getRemovalCondition() != IEntityRemovalConditions.REMOVE_CAPTURED &&
                            entity.getRemovalCondition() != IEntityRemovalConditions.REMOVE_EJECTED &&
                            entity.getRemovalCondition() != IEntityRemovalConditions.REMOVE_NEVER_JOINED)
                        {
                            entity.setRemovalCondition(IEntityRemovalConditions.REMOVE_IN_RETREAT);
                            unitWithdrawn.getAndIncrement();
                        }
                        game().addUnitToGraveyard(entity);
                    });
                }
            }
            if (unitWithdrawn.get() > 0) {
                reporter.reportSuccessfulWithdraw(withdrawFormation);
            }
            game().removeFormation(withdrawFormation);
        }
    }
}
