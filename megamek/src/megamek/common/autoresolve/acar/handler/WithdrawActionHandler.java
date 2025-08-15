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

package megamek.common.autoresolve.acar.handler;

import megamek.common.IEntityRemovalConditions;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.action.WithdrawAction;
import megamek.common.autoresolve.acar.report.IWithdrawReporter;
import megamek.common.autoresolve.acar.report.WithdrawReporter;
import megamek.common.autoresolve.damage.EntityFinalState;

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
                game().applyDamageToEntityFromUnit(withdrawFormation.getUnits().get(0),
                      formationEntity,
                      EntityFinalState.CREW_AND_ENTITY_MUST_SURVIVE);
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
                            game().applyDamageToEntityFromUnit(unit,
                                  entity,
                                  EntityFinalState.CREW_AND_ENTITY_MUST_SURVIVE);
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
