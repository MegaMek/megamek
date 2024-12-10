/*
 * Copyright (c) 2022, 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.strategicBattleSystems;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.client.ui.swing.calculationReport.FlexibleCalculationReport;
import megamek.common.Entity;
import megamek.common.ForceAssignable;
import megamek.common.IGame;
import megamek.common.alphaStrike.ASDamage;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.force.Force;
import megamek.common.force.Forces;

import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

public final class SBFFormationConverter extends BaseFormationConverter<SBFFormation> {

    public SBFFormationConverter(Force force, IGame game) {
        super(force, game, new SBFFormation());
    }

    public static void calculateStatsFromUnits(SBFFormation formation) {
        SBFFormationConverter converter = new SBFFormationConverter(null, null);
        converter.formation = formation;
        converter.calcSbfFormationStats();
        formation.setConversionReport(converter.report);
    }
}
