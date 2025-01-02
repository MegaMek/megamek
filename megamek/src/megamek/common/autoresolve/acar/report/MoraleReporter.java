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

import megamek.common.IGame;
import megamek.common.Roll;
import mekhq.campaign.autoresolve.acar.report.RollReportEntry;
import mekhq.campaign.autoresolve.component.Formation;

import java.util.function.Consumer;

public class MoraleReporter {

    private final IGame game;
    private final Consumer<PublicReportEntry> reportConsumer;

    public MoraleReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) {
        this.game = game;
        this.reportConsumer = reportConsumer;
    }

    public void reportMoraleCheckStart(Formation formation, int toHitValue) {
        // 4500: Start of morale check
        var startReport = new PublicReportEntry(4500)
            .add(new FormationReportEntry(formation, game).reportText())
            .add(toHitValue);
        reportConsumer.accept(startReport);
    }

    public void reportMoraleCheckRoll(Formation formation, Roll roll) {
        // 4501: Roll result
        var rollReport = new PublicReportEntry(4501)
            .add(new FormationReportEntry(formation, game).reportText())
            .add(new RollReportEntry(roll).reportText());
        reportConsumer.accept(rollReport);
    }

    public void reportMoraleCheckSuccess(Formation formation) {
        // 4502: Success - morale does not worsen
        var successReport = new PublicReportEntry(4502)
            .add(new FormationReportEntry(formation, game).text());
        reportConsumer.accept(successReport);
    }

    public void reportMoraleCheckFailure(Formation formation, Formation.MoraleStatus oldStatus, Formation.MoraleStatus newStatus) {
        // 4503: Failure - morale worsens
        var failReport = new PublicReportEntry(4503)
            .add(new FormationReportEntry(formation, game).text())
            .add(oldStatus.name())
            .add(newStatus.name());

        reportConsumer.accept(failReport);
    }
}
