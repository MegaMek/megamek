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
import megamek.common.strategicBattleSystems.SBFFormation;

public record RecoveringNerveActionReporter(IGame game, Consumer<PublicReportEntry> reportConsumer)
      implements IRecoveringNerveActionReporter {

    public static IRecoveringNerveActionReporter create(SimulationManager manager) {
        if (manager.isLogSuppressed()) {
            return DummyRecoveringNerveActionReporter.instance();
        }
        return new RecoveringNerveActionReporter(manager.getGame(), manager::addReport);
    }

    @Override
    public void reportRecoveringNerveStart(Formation formation, int toHitValue) {
        reportConsumer.accept(new PublicReportEntry("acar.morale.recoveryAttempt")
              .add(new FormationReportEntry(formation, game).reportText())
              .add(toHitValue)
        );
    }

    @Override
    public void reportMoraleStatusChange(Formation.MoraleStatus newMoraleStatus, Roll roll) {
        reportConsumer.accept(new PublicReportEntry("acar.morale.recoveryRoll")
              .indent().add(new RollReportEntry(roll).reportText()).add(newMoraleStatus.name().toLowerCase()));
    }

    @Override
    public void reportFailureRoll(Roll roll, SBFFormation.MoraleStatus moraleStatus) {
        reportConsumer.accept(new PublicReportEntry("acar.morale.recoveryFail")
              .add(roll.toString()).add(moraleStatus.name().toLowerCase()));
    }
}
