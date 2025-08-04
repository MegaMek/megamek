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

package megamek.common.autoresolve.acar.report;

import java.util.function.Consumer;

import megamek.common.IGame;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.component.Formation;

public class WithdrawReporter implements IWithdrawReporter {

    private final IGame game;
    private final Consumer<PublicReportEntry> reportConsumer;

    public WithdrawReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.game = game;
        this.reportConsumer = reportConsumer;
    }

    public static IWithdrawReporter create(SimulationManager manager) {
        if (manager.isLogSuppressed()) {
            return DummyWithdrawReporter.instance();
        }
        return new WithdrawReporter(manager.getGame(), manager::addReport);
    }

    @Override
    public void reportStartingWithdrawForCrippled(Formation withdrawingFormation) {
        reportConsumer.accept(
              new PublicReportEntry("acar.endPhase.withdrawAnnouncement")
                    .add(new FormationReportEntry(withdrawingFormation, game).reportText())
                    .add(new PublicReportEntry("acar.endPhase.withdrawMotive.crippled").reportText()));
    }

    @Override
    public void reportStartingWithdrawForOrder(Formation withdrawingFormation) {
        reportConsumer.accept(
              new PublicReportEntry("acar.endPhase.withdrawAnnouncement")
                    .add(new FormationReportEntry(withdrawingFormation, game).reportText())
                    .add(new PublicReportEntry("acar.endPhase.withdrawMotive.metOrderCondition").reportText()));
    }

    @Override
    public void reportSuccessfulWithdraw(Formation withdrawingFormation) {
        reportConsumer.accept(
              new PublicReportEntry("acar.endPhase.withdrawSuccess")
                    .add(new FormationReportEntry(withdrawingFormation, game).reportText()));
    }
}
