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

package megamek.common.autoResolve.acar.report;

import java.util.function.Consumer;

import megamek.common.autoResolve.acar.SimulationManager;
import megamek.common.autoResolve.component.Formation;
import megamek.common.game.IGame;
import megamek.common.rolls.Roll;

public record MoraleReporter(IGame game, Consumer<PublicReportEntry> reportConsumer) implements IMoraleReporter {

    public static IMoraleReporter create(SimulationManager manager) {
        if (manager.isLogSuppressed()) {
            return DummyMoraleReporter.instance();
        }
        return new MoraleReporter(manager.getGame(), manager::addReport);
    }

    @Override
    public void reportMoraleCheckStart(Formation formation, int toHitValue) {
        var startReport = new PublicReportEntry("acar.morale.checkAttempt")
              .add(new FormationReportEntry(formation, game).reportText())
              .add(toHitValue);
        reportConsumer.accept(startReport);
    }

    @Override
    public void reportMoraleCheckRoll(Formation formation, Roll roll) {
        var rollReport = new PublicReportEntry("acar.morale.checkRolled")
              .add(new FormationReportEntry(formation, game).reportText())
              .add(new RollReportEntry(roll).reportText());
        reportConsumer.accept(rollReport);
    }

    @Override
    public void reportMoraleCheckSuccess(Formation formation) {
        var successReport = new PublicReportEntry("acar.morale.checkSuccess")
              .add(new FormationReportEntry(formation, game).text());
        reportConsumer.accept(successReport);
    }

    @Override
    public void reportMoraleCheckFailure(Formation formation, Formation.MoraleStatus oldStatus,
          Formation.MoraleStatus newStatus) {
        var failReport = new PublicReportEntry("acar.morale.checkFail")
              .add(new FormationReportEntry(formation, game).text())
              .add(oldStatus.name())
              .add(newStatus.name());

        reportConsumer.accept(failReport);
    }
}
