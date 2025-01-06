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
package megamek.common.autoresolve.acar.phase;

import megamek.common.Entity;
import megamek.common.alphaStrike.ASRange;
import megamek.common.autoresolve.acar.SimulationManager;
import megamek.common.autoresolve.acar.action.EngagementControlAction;
import megamek.common.autoresolve.component.EngagementControl;
import megamek.common.autoresolve.component.Formation;
import megamek.common.autoresolve.component.FormationTurn;
import megamek.common.enums.GamePhase;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.util.weightedMaps.WeightedDoubleMap;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MovementPhase extends PhaseHandler {

    private static final WeightedDoubleMap<EngagementControl> normal = WeightedDoubleMap.of(
        EngagementControl.FORCED_ENGAGEMENT, 1.0,
        EngagementControl.EVADE, 0.0,
        EngagementControl.STANDARD, 1.0,
        EngagementControl.OVERRUN, 0.5,
        EngagementControl.NONE, 0.0
    );

    private static final WeightedDoubleMap<EngagementControl> unsteady =  WeightedDoubleMap.of(
        EngagementControl.FORCED_ENGAGEMENT, 0.5,
        EngagementControl.EVADE, 0.02,
        EngagementControl.STANDARD, 1.0,
        EngagementControl.OVERRUN, 0.1,
        EngagementControl.NONE, 0.01
    );

    private static final WeightedDoubleMap<EngagementControl> shaken =  WeightedDoubleMap.of(
        EngagementControl.FORCED_ENGAGEMENT, 0.2,
        EngagementControl.EVADE, 0.1,
        EngagementControl.STANDARD, 0.8,
        EngagementControl.OVERRUN, 0.05,
        EngagementControl.NONE, 0.01
    );

    private static final WeightedDoubleMap<EngagementControl> broken = WeightedDoubleMap.of(
        EngagementControl.FORCED_ENGAGEMENT, 0.05,
        EngagementControl.EVADE, 1.0,
        EngagementControl.STANDARD, 0.5,
        EngagementControl.OVERRUN, 0.05,
        EngagementControl.NONE, 0.3
    );

    private static final WeightedDoubleMap<EngagementControl> routed = WeightedDoubleMap.of(
        EngagementControl.NONE, 1.0
    );

    private static final Map<Formation.MoraleStatus, WeightedDoubleMap<EngagementControl>> engagementControlOptions = Map.of(
        Formation.MoraleStatus.NORMAL, normal,
        Formation.MoraleStatus.UNSTEADY, unsteady,
        Formation.MoraleStatus.SHAKEN, shaken,
        Formation.MoraleStatus.BROKEN, broken,
        Formation.MoraleStatus.ROUTED, routed
    );

    public MovementPhase(SimulationManager gameManager) {
        super(gameManager, GamePhase.MOVEMENT);
    }

    @Override
    protected void executePhase() {

        while (getSimulationManager().getGame().hasMoreTurns()) {

            var optTurn = getSimulationManager().getGame().changeToNextTurn();
            if (optTurn.isEmpty()) {
                break;
            }
            var turn = optTurn.get();

            if (turn instanceof FormationTurn formationTurn) {
                var player = getSimulationManager().getGame().getPlayer(formationTurn.playerId());
                getSimulationManager().getGame().getActiveFormations(player)
                    .stream()
                    .filter(f -> f.isEligibleForPhase(getSimulationManager().getGame().getPhase())) // only eligible formations
                    .findAny()
                    .map(this::engage)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .ifPresent(this::engagementAndControl);
            }
        }
    }

    private Optional<EngagementControlRecord> engage(Formation activeFormation) {
        var target = this.selectTarget(activeFormation);
        return target.map(sbfFormation -> new EngagementControlRecord(activeFormation, sbfFormation));
    }

    private record EngagementControlRecord(Formation actingFormation, Formation target) { }

    private static final Map<Integer, EngagementControl[]> engagementAndControlExclusions = Map.of(
        0, new EngagementControl[] { EngagementControl.NONE },
        1, new EngagementControl[] { EngagementControl.FORCED_ENGAGEMENT },
        2, new EngagementControl[] { EngagementControl.OVERRUN },
        3, new EngagementControl[] { EngagementControl.OVERRUN, EngagementControl.FORCED_ENGAGEMENT },
        4, new EngagementControl[] { EngagementControl.STANDARD, EngagementControl.OVERRUN, EngagementControl.FORCED_ENGAGEMENT },
        5, new EngagementControl[] { EngagementControl.STANDARD, EngagementControl.OVERRUN, EngagementControl.FORCED_ENGAGEMENT }
    );

    private static final EngagementControl[] EMPTY_EAC = new EngagementControl[0];

    private void engagementAndControl(EngagementControlRecord engagement) {
        var eco = engagementControlOptions.get(engagement.target.moraleStatus());
        var engagementControlExclusion = 0;

        if (engagement.actingFormation.getStdDamage().usesDamage(ASRange.LONG)) {
            engagementControlExclusion += 1;
        }
        if (engagement.actingFormation.getStdDamage().getDamage(ASRange.SHORT).damage < 2) {
            engagementControlExclusion += 2;
        }
        if (engagement.actingFormation.isCrippled()) {
            engagementControlExclusion += 2;
        }

        var engagementControl = eco.randomOptionalItem(engagementAndControlExclusions.getOrDefault(engagementControlExclusion, EMPTY_EAC));

        getSimulationManager().addEngagementControl(
            new EngagementControlAction(
                engagement.actingFormation.getId(),
                engagement.target.getId(),
                engagementControl.orElse(EngagementControl.NONE)),
            engagement.actingFormation);
    }

    private Optional<Formation> selectTarget(Formation actingFormation) {
        var game = getSimulationManager().getGame();
        var player = game.getPlayer(actingFormation.getOwnerId());
        var canBeTargets = getSimulationManager().getGame().getActiveDeployedFormations().stream()
            .filter(f -> actingFormation.getTargetFormationId() == Entity.NONE || f.getId() == actingFormation.getTargetFormationId())
            .filter(SBFFormation::isDeployed)
            .filter(f -> f.isGround() == actingFormation.isGround())
            .filter(f -> game.getPlayer(f.getOwnerId()).isEnemyOf(player))
            .collect(Collectors.toList());
        Collections.shuffle(canBeTargets);

        if (canBeTargets.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(canBeTargets.get(0));
    }
}
