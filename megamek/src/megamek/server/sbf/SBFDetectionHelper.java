/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.server.sbf;

import java.util.List;

import megamek.codeUtilities.MathUtility;
import megamek.common.Compute;
import megamek.common.Player;
import megamek.common.Roll;
import megamek.common.TargetRoll;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.planetaryconditions.Fog;
import megamek.common.planetaryconditions.Light;
import megamek.common.planetaryconditions.PlanetaryConditions;
import megamek.common.planetaryconditions.Weather;
import megamek.common.strategicBattleSystems.SBFElementType;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFVisibilityStatus;
import megamek.logging.MMLogger;

/**
 * This class performs detection of formations in the Detection and Recon phase,
 * IO:BF p.195 for the
 * SBFGameManager.
 */
record SBFDetectionHelper(SBFGameManager gameManager) implements SBFGameManagerHelper {
    private static final MMLogger logger = MMLogger.create(SBFDetectionHelper.class);

    /**
     * Performs sensor detection for all formations of all players and updates the
     * visibility status in the
     * game accordingly. Does not send anything.
     */
    void performSensorDetection() {
        if (game().usesDoubleBlind()) {
            for (Player player : game().getPlayersList()) {
                logger.info("Detection for " + player.getName()); // TODO remove or move to protocol
                performSensorDetection(player);
            }
        }
    }

    private void performSensorDetection(Player viewingPlayer) {
        List<SBFFormation> viewingFormations = game().getActiveFormations().stream()
                .filter(u -> u.getOwnerId() == viewingPlayer.getId())
                .filter(this::canDetect)
                .toList();

        List<SBFFormation> hostileFormations = game().getActiveFormations().stream()
                .filter(u -> game().areHostile(u, viewingPlayer))
                .filter(this::canBeDetected)
                .toList();

        for (SBFFormation hostileFormation : hostileFormations) {
            SBFVisibilityStatus visibilityStatus = SBFVisibilityStatus.INVISIBLE;
            for (SBFFormation viewingFormation : viewingFormations) {
                if (!game().onSameBoard(viewingFormation, hostileFormation)) {
                    continue;
                }
                // TODO: aero units need special treatment
                var detectionModifiers = new SBFDetectionModifiers(viewingFormation, hostileFormation);
                if (detectionModifiers.getValue() != TargetRoll.CHECK_FALSE) {
                    Roll diceRoll = Compute.rollD6(2);
                    int rollResult = diceRoll.getIntValue() + detectionModifiers.getValue();
                    SBFVisibilityStatus detectionResult = sensorDetectionResult(rollResult);
                    // TODO remove or move to protocol:
                    logger.info("Detected from " + viewingFormation.getId() + " to "
                            + hostileFormation.getId() + " result " + detectionResult);
                    visibilityStatus = visibilityStatus.bestOf(detectionResult);
                }
            }
            game().visibilityHelper().setVisibility(viewingPlayer.getId(), hostileFormation.getId(), visibilityStatus);
        }
    }

    private boolean canDetect(SBFFormation formation) {
        return formation.isDeployed() && (formation.getPosition() != null)
                && (formation.getPosition().coords() != null);
    }

    private boolean canBeDetected(SBFFormation formation) {
        return formation.isDeployed() && (formation.getPosition() != null)
                && (formation.getPosition().coords() != null);
    }

    /**
     * IO:BF p.197
     *
     * @param rollWithModifiers The 2d6 roll inlcuding modifiers
     * @return The visibility status from sensor scan
     */
    private SBFVisibilityStatus sensorDetectionResult(int rollWithModifiers) {
        return switch (MathUtility.clamp(rollWithModifiers, 2, 12)) {
            case 2 -> SBFVisibilityStatus.INVISIBLE;
            case 3, 4 -> SBFVisibilityStatus.SENSOR_GHOST;
            case 5, 6 -> SBFVisibilityStatus.SENSOR_PING;
            case 7, 8, 9 -> SBFVisibilityStatus.SOMETHING_OUT_THERE;
            case 10, 11 -> SBFVisibilityStatus.PARTIAL_SCAN;
            default -> SBFVisibilityStatus.VISIBLE;
        };
    }

    private int visualRange(SBFFormation formation) {
        int srchModifier = (formation.hasSUA(BattleForceSUA.SRCH) &&
                game().getPlanetaryConditions().getLight().isDuskOrFullMoonOrMoonlessOrPitchBack()) ? 1 : 0;
        return switch (visualLevel()) {
            case 4 -> srchModifier;
            case 3 -> 1 + srchModifier;
            case 2 -> srchModifier + (formation.isAnyTypeOf(SBFElementType.BM, SBFElementType.V) ? 2 : 1);
            default -> srchModifier + (formation.isAnyTypeOf(SBFElementType.BM, SBFElementType.V) ? 4 : 2);
        };
    }

    private int visualLevel() {
        PlanetaryConditions conditions = game().getPlanetaryConditions();
        Light light = conditions.getLight();
        Weather weather = conditions.getWeather();
        Fog fog = conditions.getFog();
        boolean sand = conditions.isBlowingSand();
        // TODO: this is missing quite a few conditions
        if (light.isMoonlessOrPitchBack()) {
            return 4;
        } else if (light.isDusk() || weather.isSleet() || sand) {
            return 3;
        } else if (weather.isAnyRain() || weather.isAnySnowfall() || !fog.isFogNone()) {
            return 2;
        } else {
            return 1;
        }
    }
}
