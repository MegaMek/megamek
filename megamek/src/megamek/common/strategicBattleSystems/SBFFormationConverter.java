/*
 *
 *  * Copyright (c) 26.09.21, 08:50 - The MegaMek Team. All Rights Reserved.
 *  *
 *  * This file is part of MegaMek.
 *  *
 *  * MegaMek is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * MegaMek is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package megamek.common.strategicBattleSystems;

import megamek.common.*;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.force.Force;
import megamek.common.force.Forces;

import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

public final class SBFFormationConverter {

    /**
     *  Returns an SBF Formation formed from the given force. When the force cannot be converted
     *  to an SBF Formation according to the rules, returns null. The given force must be
     *  approximately company-shaped to work, i.e. it has to contain some subforces with some entities
     *  in each subforce but no further subforces.
     */
    public static SBFFormation convert(Force force, Game game, boolean includePilots) {
        if (!canConvertToSbfFormation(force, game)) {
            return null;
        }
        var result = new SBFFormation();
        Forces forces = game.getForces();
        for (Force subforce : forces.getFullSubForces(force)) {
            var thisUnit = new ArrayList<AlphaStrikeElement>();
            var thisUnitBaseSkill = new ArrayList<AlphaStrikeElement>();
            for (Entity entity : forces.getFullEntities(subforce)) {
                thisUnit.add(ASConverter.convert(entity, includePilots));
                thisUnitBaseSkill.add(ASConverter.convert(entity, false));
            }
            result.getUnits().add(SBFUnitConverter.createSbfUnit(thisUnit, subforce.getName(), thisUnitBaseSkill));
        }
        calcSbfFormationStats(result, force.getName());
        return result;
    }

    /** Calculates the SBF Formation stats for the SBF Units it must already contain. */
    private static void calcSbfFormationStats(SBFFormation result, String name) {
        result.setName(name);
        result.setType(calcFormationType(result.getUnits()));
        result.setSize((int)Math.round(result.getUnits().stream().mapToDouble(SBFUnit::getSize).average().orElse(0)));
        result.setMovement((int)Math.round(result.getUnits().stream().mapToDouble(SBFUnit::getMovement).average().orElse(0)));
        result.setTrspMovement((int)Math.round(result.getUnits().stream().mapToDouble(SBFUnit::getTrspMovement).average().orElse(0)));
        result.setJumpMove((int)Math.round(result.getUnits().stream().mapToDouble(SBFUnit::getJumpMove).average().orElse(0)));
        result.setTmm((int)Math.round(result.getUnits().stream().mapToDouble(SBFUnit::getTmm).average().orElse(0)));
        result.setSkill((int)Math.round(result.getUnits().stream().mapToDouble(SBFUnit::getSkill).average().orElse(0)));
        calcFormationSpecialAbilities(result);
        result.setTactics(getFormationTactics(result));
        result.setMorale(3 + result.getSkill());
        result.setPointValue(result.getUnits().stream().mapToInt(SBFUnit::getPointValue).sum());
    }

    /** Returns true if the given force can be converted to an SBF Formation. */
    public static boolean canConvertToSbfFormation(Force force, Game game) {
        if ((force == null) || (game == null) || force.isEmpty()
                || (game.getForces().getForce(force.getId()) != force)) {
            return false;
        }
        Forces forces = game.getForces();
        // The force must not have direct subordinate entities
        boolean invalid = !force.getEntities().isEmpty();
        List<Entity> entities = forces.getFullEntities(force);
        List<Force> subforces = forces.getFullSubForces(force);
        invalid |= entities.size() > 20;
        invalid |= (subforces.size() > 4) || subforces.isEmpty();
        invalid |= subforces.stream().anyMatch(f -> f.getEntities().isEmpty());
        invalid |= subforces.stream().anyMatch(f -> f.getEntities().size() > 6);
        invalid |= subforces.stream().anyMatch(f -> !f.getSubForces().isEmpty());
        invalid |= entities.stream().anyMatch(e -> !ASConverter.canConvert(e));
        // Avoid some checks in the code below
        if (invalid) {
            return false;
        }
        for (Force subforce : subforces) {
            var elementsList = new ArrayList<AlphaStrikeElement>();
            forces.getFullEntities(subforce).stream().map(ASConverter::convert).forEach(elementsList::add);
            invalid |= elementsList.stream().anyMatch(a -> a.hasSUA(LG)) && elementsList.size() > 2;
            invalid |= elementsList.stream().anyMatch(a -> a.hasAnySUAOf(VLG, SLG)) && elementsList.size() > 1;
            SBFUnit unit = SBFUnitConverter.createSbfUnit(elementsList, "temporary", elementsList);
            invalid |= unit.isGround() && elementsList.stream().anyMatch(AlphaStrikeElement::isAerospace);
            invalid |= unit.isAerospace() && elementsList.stream().anyMatch(a -> !a.hasAnySUAOf(SOA, LAM, BIM));
        }
        return !invalid;
    }

    static void calcFormationSpecialAbilities(SBFFormation formation) {
        addFormationSpasIfAny(formation, DN, XMEC, COM, HPG, LEAD, MCS, UCS, MEC, MAS, LMAS,
                MSW, MFB, SAW, SDS, TRN, FD, HELI, SDCS);
        addFormationSpasIf2Thirds(formation, AC3, PRB, AECM, ECM, ENG, LPRB, LECM, ORO, RCN, SRCH, SHLD, TAG, WAT);
        addFormationSpasIfAll(formation, AMP, BH, EE, FC, SEAL, MAG, PARA, RAIL, RBT, UMU);
        sumFormationSpas(formation, OMNI, CAR, CK, CT, IT, CRW, DCC, MDS, MASH, RSD, VTM, VTH,
                VTS, AT, BOMB, DT, MT, PT, ST, SCR, PNT, IF, MHQ);
    }

    /**
     * Returns the number of the given SBFUnits that have the given spa (regardless of its
     * associated objects)
     */
    private static int spaCount(SBFFormation formation, BattleForceSUA spa) {
        return (int)formation.getUnits().stream().filter(e -> e.hasSPA(spa)).count();
    }

    private static void addFormationSpasIfAny(SBFFormation formation, BattleForceSUA... spas) {
        addFormationSpas(formation, 1, spas);
    }

    private static void addFormationSpasIfAll(SBFFormation formation, BattleForceSUA... spas) {
        addFormationSpas(formation, formation.getUnits().size(), spas);
    }

    private static void addFormationSpasIf2Thirds(SBFFormation formation, BattleForceSUA... spas) {
        int twoThirds = Math.max(formation.getUnits().size() - 1, 1);
        addFormationSpas(formation, twoThirds, spas);
    }

    private static void addFormationSpas(SBFFormation formation, int threshold, BattleForceSUA[] spas) {
        for (BattleForceSUA spa : spas) {
            if (spaCount(formation, spa) >= threshold) {
                formation.addSPA(spa);
            }
        }
    }

    private static void sumFormationSpas(SBFFormation formation, BattleForceSUA... spas) {
        for (BattleForceSUA spa : spas) {
            for (SBFUnit unit : formation.getUnits()) {
                if (unit.hasSPA(spa)) {
                    if (unit.getSPA(spa) == null) {
                        formation.addSPA(spa, 1);
                    } else if (unit.getSPA(spa) instanceof Integer) {
                        formation.addSPA(spa, (Integer) unit.getSPA(spa));
                    } else if (unit.getSPA(spa) instanceof Double) {
                        formation.addSPA(spa, (Double) unit.getSPA(spa));
                    }
                }
            }
        }
    }

    static int getFormationTactics(SBFFormation formation) {
        int tactics = Math.max(0, 10 - formation.getMovement() + formation.getSkill() - 4);
        if (formation.hasSPA(MHQ)) {
            double mhqValue = (Integer)formation.getSPA(MHQ);
            tactics -= Math.min(3, Math.round(mhqValue / 2));
        }
        return tactics;
    }

    static SBFElementType calcFormationType(Collection<SBFUnit> units) {
        int majority = (int) Math.round(2.0 / 3 * units.size());
        List<SBFElementType> types = units.stream().map(SBFUnit::getType).collect(toList());
        Map<SBFElementType, Long> frequencies = types.stream().collect(groupingBy(Function.identity(), counting()));
        long highestOccurrence = frequencies.values().stream().max(Long::compare).orElse(0L);
        if (highestOccurrence < majority) {
            return SBFElementType.MX;
        } else {
            return types.stream()
                    .filter(e -> Collections.frequency(types, e) == highestOccurrence)
                    .findFirst().get();
        }
    }
}
