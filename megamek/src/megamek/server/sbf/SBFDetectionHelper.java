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

import megamek.codeUtilities.MathUtility;
import megamek.common.*;
import megamek.common.strategicBattleSystems.SBFElementType;
import megamek.common.strategicBattleSystems.SBFFormation;
import megamek.common.strategicBattleSystems.SBFVisibilityStatus;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;

import static megamek.common.alphaStrike.BattleForceSUA.*;

record SBFDetectionHelper(SBFGameManager gameManager) implements SBFGameManagerHelper {

    void performSensorDetection() {
        if (game().usesDoubleBlind()) {
            for (Player player : game().getPlayersList()) {
                LogManager.getLogger().info("Detection for " + player.getName());
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
                //TODO: aero units need special treatment
                int range = sensorRange(viewingFormation);
                int distance = viewingFormation.getPosition().getCoords()
                        .distance(hostileFormation.getPosition().getCoords());
                if (range >= distance) {
                    //roll and report
                    Roll diceRoll = Compute.rollD6(2);
                    List<TargetRollModifier> modifiers = sensorTargetRollModifiers(viewingFormation, hostileFormation);
                    visibilityStatus = visibilityStatus.betterOf(sensorDetectionResult(diceRoll));
                    LogManager.getLogger().info("Detected from "+viewingFormation.getId()+" to "+hostileFormation.getId()+" result "+sensorDetectionResult(diceRoll));
                }
            }
            game().visibilityHelper().setVisibility(viewingPlayer.getId(), hostileFormation.getId(), visibilityStatus);
        }
    }


    /**
     * Returns the sensor range for sensor detection, IO:BF p.197
     *
     * @param formation The detecting formation
     * @return the sensor range
     */
    private int sensorRange(SBFFormation formation) {
        //TODO: this is heavily incomplete as the table is quite unclear
        int rcnExtension = formation.hasSUA(RCN) ? 2 : 0;
        if (formation.hasSUA(PRB)) {
            return 8;
        } else if (formation.hasSUA(BH)) {
            return 10;
        } else if (formation.isType(SBFElementType.BM)) {
            return 6 + rcnExtension;
        } else if (formation.isType(SBFElementType.V)) {
            return 5 + rcnExtension;
        } else {
            return 0;
        }
    }

    private boolean canDetect(SBFFormation formation) {
        return formation.isDeployed() && (formation.getPosition() != null)
                && (formation.getPosition().getCoords() != null);
    }

    private boolean canBeDetected(SBFFormation formation) {
        return formation.isDeployed() && (formation.getPosition() != null)
                && (formation.getPosition().getCoords() != null);
    }

    private SBFVisibilityStatus sensorDetectionResult(Roll roll) {
        return switch (MathUtility.clamp(roll.getIntValue(), 2, 12)) {
            case 2 -> SBFVisibilityStatus.INVISIBLE;
            case 3, 4 -> SBFVisibilityStatus.SENSOR_GHOST;
            case 5, 6 -> SBFVisibilityStatus.SENSOR_PING;
            case 7, 8, 9 -> SBFVisibilityStatus.SOMETHING_OUT_THERE;
            case 10, 11 -> SBFVisibilityStatus.PARTIAL_SCAN;
            default -> SBFVisibilityStatus.VISIBLE;
        };
    }

    private List<TargetRollModifier> sensorTargetRollModifiers(SBFFormation viewer, SBFFormation target) {
        List<TargetRollModifier> result = new ArrayList<>();
        int range = sensorRange(viewer);
        int distance = viewer.getPosition().getCoords()
                .distance(target.getPosition().getCoords());
        if (distance < range) {
            result.add(new TargetRollModifier(Math.min(4, range - distance), "distance below sensor range"));
        } else if (distance > range) {
            //TODO make this automatic fail like in ToHit
            result.add(new TargetRollModifier(-1000, "distance above sensor range"));
        }
        if (target.hasSUA(AECM) && target.hasAnySUAOf(STL, MAS, LMAS)) {
            result.add(new TargetRollModifier(-3, "target has AECM and STL, MAS or LMAS"));
        } else if (target.hasAnySUAOf(STL, MAS, LMAS)) {
            result.add(new TargetRollModifier(-2, "target has STL, MAS or LMAS"));
        } else if (target.hasSUA(AECM) && viewer.hasSUA(BH)) {
            result.add(new TargetRollModifier(-1, "target has AECM, viewer has BH"));
        } else if (target.hasSUA(AECM)) {
            result.add(new TargetRollModifier(-2, "target has AECM"));
        } else if (target.hasAnySUAOf(ECM, WAT, LECM) && !viewer.hasSUA(BH)) {
            result.add(new TargetRollModifier(-1, "target has ECM, WAT or LECM"));
        }
        return result;
    }
}
