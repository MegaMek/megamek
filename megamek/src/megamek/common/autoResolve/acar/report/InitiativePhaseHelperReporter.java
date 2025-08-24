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

import megamek.common.Player;
import megamek.common.Team;
import megamek.common.autoResolve.acar.SimulationContext;
import megamek.common.autoResolve.acar.SimulationManager;

public record InitiativePhaseHelperReporter(SimulationContext context, Consumer<PublicReportEntry> reportConsumer)
      implements IInitiativePhaseHelperReporter {

    public static IInitiativePhaseHelperReporter create(SimulationManager manager) {
        if (manager.isLogSuppressed()) {
            return DummyInitiativePhaseHelperReporter.instance();
        }
        return new InitiativePhaseHelperReporter(manager.getGame(), manager::addReport);
    }

    @Override
    public void writeInitiativeRolls() {
        for (Team team : context.getTeams()) {
            // Teams with no active players can be ignored
            if (team.isObserverTeam()) {
                continue;
            }

            var teamInitBonus = team.getTotalInitBonus(false);
            var reportKey = "acar.initiative.rollAnnouncement";
            if (teamInitBonus > 0) {
                reportKey = "acar.initiative.rollAnnouncementWithBonus";
            }

            reportConsumer.accept(new PublicReportEntry(reportKey).add(Player.TEAM_NAMES[team.getId()])
                  .add(teamInitBonus));

            // Multiple players. List the team, then break it down.
            for (Player player : team.nonObserverPlayers()) {
                reportConsumer.accept(new PublicReportEntry("acar.initiative.rollAnnouncementCustom")
                      .indent()
                      .add(player.getName())
                      .add(player.getInitiative().toString())
                );
            }
        }
    }

    @Override
    public void writeInitiativeHeader() {
        var currentRound = context.getCurrentRound();
        reportConsumer.accept(new DividerEntry());
        if (context.getLastPhase().isDeployment() || context.isDeploymentComplete()
              || !context.shouldDeployThisRound()) {
            reportConsumer.accept(new ReportEntryWithAnchor("acar.initiative.roundHeader", "round-" + currentRound).add(
                  currentRound));
            reportConsumer.accept(new LinkEntry("acar.link.backRef", "summary-round-" + currentRound));
        } else {
            if (currentRound == 0) {
                reportConsumer.accept(new ReportEntryWithAnchor("acar.initiative.startOfGameHeader",
                      "round-" + currentRound));
                reportConsumer.accept(new LinkEntry("acar.link.backRef", "summary-round-" + currentRound));
            } else {
                reportConsumer.accept(new ReportEntryWithAnchor("acar.initiative.roundHeader",
                      "round-" + currentRound).add(currentRound));
                reportConsumer.accept(new LinkEntry("acar.link.backRef", "summary-round-" + currentRound));
            }
        }
    }
}
