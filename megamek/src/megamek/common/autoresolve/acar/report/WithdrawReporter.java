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

package megamek.common.autoresolve.acar.report;

import megamek.client.ui.swing.util.UIUtil;
import megamek.common.IGame;
import megamek.common.Roll;
import megamek.common.TargetRoll;
import mekhq.campaign.autoresolve.acar.report.PlayerNameReportEntry;
import mekhq.campaign.autoresolve.acar.report.RollReportEntry;
import mekhq.campaign.autoresolve.component.Formation;

import java.util.function.Consumer;

import static megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip.ownerColor;

public class WithdrawReporter {

    private final IGame game;
    private final Consumer<PublicReportEntry> reportConsumer;

    public WithdrawReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.game = game;
        this.reportConsumer = reportConsumer;
    }

    public void reportStartWithdraw(Formation withdrawingFormation, TargetRoll toHitData) {
        // Formation trying to withdraw
        var report = new PublicReportEntry(3330).noNL()
            .add(new FormationReportEntry(
                withdrawingFormation.generalName(),
                UIUtil.hexColor(ownerColor(withdrawingFormation, game))
            ).text())
            .add(withdrawingFormation.moraleStatus().name().toLowerCase())
            .indent();
        reportConsumer.accept(report);

        // To-Hit Value
        reportConsumer.accept(new PublicReportEntry(3331).add(toHitData.getValue()).add(toHitData.toString()).indent(2));
    }

    public void reportWithdrawRoll(Formation withdrawingFormation, Roll withdrawRoll) {
        var report = new PublicReportEntry(3332).noNL();
        report.add(new PlayerNameReportEntry(game.getPlayer(withdrawingFormation.getOwnerId())).text());
        report.add(new RollReportEntry(withdrawRoll).reportText()).indent(2);
        reportConsumer.accept(report);
    }

    public void reportSuccessfulWithdraw() {
        reportConsumer.accept(new PublicReportEntry(3333).indent(3));
    }

    public void reportFailedWithdraw() {
        reportConsumer.accept(new PublicReportEntry(3334).indent(3));
    }
}
