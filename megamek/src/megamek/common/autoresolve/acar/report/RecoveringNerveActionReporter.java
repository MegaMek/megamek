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
import mekhq.campaign.autoresolve.acar.report.RollReportEntry;
import mekhq.campaign.autoresolve.component.Formation;

import java.util.function.Consumer;

import static megamek.client.ui.swing.tooltip.SBFInGameObjectTooltip.ownerColor;

public class RecoveringNerveActionReporter {

    private final IGame game;
    private final Consumer<PublicReportEntry> reportConsumer;

    public RecoveringNerveActionReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.reportConsumer = reportConsumer;
        this.game = game;
    }

    public void reportRecoveringNerveStart(Formation formation) {
        reportConsumer.accept(new PublicReportEntry(4000)
            .add(new FormationReportEntry(formation.generalName(), UIUtil.hexColor(ownerColor(formation, game))).text()));
    }

    public void reportToHitValue(int toHitValue) {
        reportConsumer.accept(new PublicReportEntry(4001).add(toHitValue).noNL());
    }

    public void reportSuccessRoll(Roll roll) {
        var report = new PublicReportEntry(4002).indent().noNL();
        report.add(new RollReportEntry(roll).reportText());
        reportConsumer.accept(report);
    }

    public void reportMoraleStatusChange(Formation.MoraleStatus newMoraleStatus) {
        reportConsumer.accept(new PublicReportEntry(4003).add(newMoraleStatus.name()));
    }

    public void reportFailureRoll(Roll roll) {
        reportConsumer.accept(new PublicReportEntry(4004).add(roll.toString()));
    }
}
