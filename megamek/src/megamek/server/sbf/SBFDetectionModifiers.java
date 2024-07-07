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

import megamek.common.TargetRoll;
import megamek.common.TargetRollModifier;
import megamek.common.strategicBattleSystems.SBFElementType;
import megamek.common.strategicBattleSystems.SBFFormation;

import java.util.ArrayList;
import java.util.List;

import static megamek.common.alphaStrike.BattleForceSUA.*;

public class SBFDetectionModifiers extends TargetRoll {

    public SBFDetectionModifiers(SBFFormation viewer, SBFFormation target) {
        sensorTargetRollModifiers(viewer, target).forEach(this::addModifier);
    }

    /**
     * Returns the sensor range for sensor detection, IO:BF p.197
     *
     * @param formation The detecting formation
     * @return the sensor range
     */
    private int sensorRange(SBFFormation formation) {
        //TODO: this is incomplete as the table is quite unclear
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

    private List<TargetRollModifier> sensorTargetRollModifiers(SBFFormation viewer, SBFFormation target) {
        List<TargetRollModifier> result = new ArrayList<>();
        int range = sensorRange(viewer);
        int distance = viewer.getPosition().coords()
                .distance(target.getPosition().coords());
        if (distance < range) {
            result.add(new TargetRollModifier(Math.min(4, range - distance), "distance below sensor range"));
        } else if (distance > range) {
            result.add(new TargetRollModifier(TargetRoll.CHECK_FALSE, "distance above sensor range"));
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
