/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
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
import megamek.common.Game;
import megamek.common.alphaStrike.ASDamage;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.BattleForceSUA;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.force.Force;
import megamek.common.force.Forces;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.*;
import static megamek.common.alphaStrike.BattleForceSUA.*;

public final class SBFFormationConverter {

    private final Force force;
    private final Game game;
    private final boolean includePilots;
    private final CalculationReport report;
    private final SBFFormation formation = new SBFFormation();

    public SBFFormationConverter(Force force, Game game, boolean includePilots) {
        this.force = force;
        this.game = game;
        this.includePilots = includePilots;
        this.report = new FlexibleCalculationReport();
    }

    /**
     *  Returns an SBF Formation formed from the given force. When the force cannot be converted
     *  to an SBF Formation according to the rules, returns null. The given force must be
     *  approximately company-shaped to work, i.e. it has to contain some subforces with some entities
     *  in each subforce but no further subforces.
     */
    public SBFFormation convert() {
        if (!canConvertToSbfFormation(force, game)) {
            return null;
        }
        report.addHeader("Strategic BattleForce Conversion for ");
        report.addHeader(force.getName());

        Forces forces = game.getForces();
        for (Force subforce : forces.getFullSubForces(force)) {
            var thisUnit = new ArrayList<AlphaStrikeElement>();
            var thisUnitBaseSkill = new ArrayList<AlphaStrikeElement>();
            for (Entity entity : forces.getFullEntities(subforce)) {
                thisUnit.add(ASConverter.convert(entity, includePilots));
                thisUnitBaseSkill.add(ASConverter.convert(entity, false));
            }
            SBFUnit convertedUnit = new SBFUnitConverter(thisUnit, subforce.getName(), thisUnitBaseSkill, report).createSbfUnit();
            formation.getUnits().add(convertedUnit);
        }
        formation.setName(force.getName());
        calcSbfFormationStats();
        formation.setConversionReport(report);
        return formation;
    }

    /** Calculates the SBF Formation stats for the SBF Units it must already contain. */
    private void calcSbfFormationStats() {
        report.addEmptyLine();
        report.addSubHeader("Formation:");
        calcFormationType();

        List<SBFUnit> units = formation.getUnits();

        int formationSize = (int) Math.round(units.stream().mapToDouble(SBFUnit::getSize).average().orElse(0));
        report.addLine("Size:",
                "Average of " + units.stream().map(u -> u.getSize() + "").collect(joining(", ")),
                "= " + formationSize);
        formation.setSize(formationSize);

        int formationMove = (int) Math.round(units.stream().mapToDouble(SBFUnit::getMovement).average().orElse(0));
        if (formation.isAerospace()) {
            formationMove = (int) Math.round(units.stream().mapToDouble(SBFUnit::getMovement).min().orElse(0));
        }
        report.addLine("Movement:",
                "Average of " + units.stream().map(u -> u.getMovement() + "").collect(joining(", ")),
                "= " + formationMove);
        formation.setMovement(formationMove);

        int trspMove = (int)Math.round(units.stream().mapToDouble(SBFUnit::getTrspMovement).average().orElse(0));
        report.addLine("Transport Movement:",
                "Average of " + units.stream().map(u -> u.getTrspMovement() + "").collect(joining(", ")),
                "= " + trspMove);
        formation.setTrspMovement(trspMove);

        int jumpMove = (int)Math.round(units.stream().mapToDouble(SBFUnit::getJumpMove).average().orElse(0));
        report.addLine("Jump:",
                "Average of " + units.stream().map(u -> u.getJumpMove() + "").collect(joining(", ")),
                "= " + jumpMove);
        formation.setJumpMove(jumpMove);

        int tmm = (int)Math.round(units.stream().mapToDouble(SBFUnit::getTmm).average().orElse(0));
        report.addLine("TMM:",
                "Average of " + units.stream().map(u -> u.getTmm() + "").collect(joining(", ")),
                "= " + tmm);
        formation.setTmm(tmm);

        int skill = (int)Math.round(units.stream().mapToDouble(SBFUnit::getSkill).average().orElse(0));
        report.addLine("Skill:",
                "Average of " + units.stream().map(u -> u.getSkill() + "").collect(joining(", ")),
                "= " + skill);
        formation.setSkill(skill);
        formation.setMorale(3 + formation.getSkill());
        report.addLine("Morale:", "", "= " + formation.getMorale());

        calcFormationSpecialAbilities();
        calcFormationTactics();

        int pv = units.stream().mapToInt(SBFUnit::getPointValue).sum();
        report.addLine("Point Value:",
                units.stream().map(u -> u.getPointValue() + "").collect(joining(" + ")),
                "= " + pv);
        formation.setPointValue(pv);
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
            SBFUnit unit = new SBFUnitConverter(elementsList, "temporary", elementsList, new DummyCalculationReport()).createSbfUnit();
            invalid |= unit.isGround() && elementsList.stream().anyMatch(AlphaStrikeElement::isAerospace);
            invalid |= unit.isAerospace() && elementsList.stream().filter(AlphaStrikeElement::isGround).anyMatch(a -> !a.hasAnySUAOf(SOA, LAM, BIM));
        }
        return !invalid;
    }

    void calcFormationSpecialAbilities() {
        report.addLine("Special Abilites:", "");
        addFormationSpasIfAny(formation, DN, XMEC, COM, HPG, LEAD, MCS, UCS, MEC, MAS, LMAS,
                MSW, MFB, SAW, SDS, TRN, FD, HELI, SDCS);
        addFormationSpasIf2Thirds(formation, AC3, PRB, AECM, ECM, ENG, LPRB, LECM, ORO, RCN, SRCH, SHLD, TAG, WAT);
        addFormationSpasIfAll(formation, AMP, BH, EE, FC, SEAL, MAG, PARA, RAIL, RBT, UMU);
        sumFormationSpas(SBF_OMNI, CAR, CK, CT, IT, CRW, DCC, MDS, MASH, RSD, VTM, VTH,
                VTS, AT, BOMB, DT, MT, PT, ST, SCR, PNT, IF, MHQ);

        var fuel = formation.getUnits().stream().filter(SBFUnit::isAerospace).mapToInt(SBFUnit::getFUEL).min();
        if (fuel.isPresent()) {
            formation.addSPA(FUEL, fuel.orElse(0));
        }
    }

    /**
     * Returns the number of the given SBFUnits that have the given spa (regardless of its
     * associated objects)
     */
    private int spaCount(SBFFormation formation, BattleForceSUA spa) {
        return (int)formation.getUnits().stream().filter(e -> e.hasSUA(spa)).count();
    }

    private void addFormationSpasIfAny(SBFFormation formation, BattleForceSUA... spas) {
        addFormationSpas(formation, 1, spas);
    }

    private void addFormationSpasIfAll(SBFFormation formation, BattleForceSUA... spas) {
        addFormationSpas(formation, formation.getUnits().size(), spas);
    }

    private void addFormationSpasIf2Thirds(SBFFormation formation, BattleForceSUA... spas) {
        int twoThirds = Math.max(formation.getUnits().size() - 1, 1);
        addFormationSpas(formation, twoThirds, spas);
    }

    private void addFormationSpas(SBFFormation formation, int minimumCount, BattleForceSUA[] spas) {
        for (BattleForceSUA spa : spas) {
            int count = spaCount(formation, spa);
            if (count >= minimumCount) {
                report.addLine("",
                        spa + ": " + count + " of " + formation.getUnits().size() + ", minimum " + minimumCount,
                        spa.toString());
                formation.addSPA(spa);
            } else if (count > 0) {
                report.addLine("",
                        spa + ": " + count + " of " + formation.getUnits().size() + ", minimum " + minimumCount,
                        "--");
            }
        }
    }

    private void sumFormationSpas(BattleForceSUA... spas) {
        for (BattleForceSUA spa : spas) {
            double suaValue = 0;
            for (SBFUnit unit : formation.getUnits()) {
                if (unit.hasSUA(spa)) {
                    if (unit.getSUA(spa) == null) {
                        suaValue++;
                    } else if (unit.getSUA(spa) instanceof Integer) {
                        suaValue += (Integer) unit.getSUA(spa);
                    } else if (unit.getSUA(spa) instanceof Double) {
                        suaValue += (Double) unit.getSUA(spa);
                    } else if (unit.getSUA(spa) instanceof ASDamage) {
                        suaValue += ((ASDamage) unit.getSUA(spa)).damage;
                    }
                }
            }
            if (suaValue > 0) {
                formation.addSPA(spa, (int) suaValue);
            }
        }
    }

    void calcFormationTactics() {
        int tactics = Math.max(0, 10 - formation.getMovement() + formation.getSkill() - 4);
        String calculation = "10 - " + formation.getMovement() + " + " + formation.getSkill() + " - 4";
        if (formation.hasSPA(MHQ)) {
            double mhqValue = (Integer) formation.getSPA(MHQ);
            long delta = Math.min(3, Math.round(mhqValue / 2));
            tactics -= Math.min(3, Math.round(mhqValue / 2));
            calculation += " - " + delta;
        }
        report.addLine("Tactics:", calculation, tactics + "");
        formation.setTactics(tactics);
    }

    void calcFormationType() {
        List<SBFUnit> units = formation.getUnits();
        int majorityCount = (int) Math.round(2.0 / 3 * units.size());
        List<SBFElementType> types = units.stream().map(SBFUnit::getType).collect(toList());
        Map<SBFElementType, Long> occurrenceCount = types.stream().collect(groupingBy(Function.identity(), counting()));
        long highestCount = occurrenceCount.values().stream().max(Long::compare).orElse(0L);
        SBFElementType highestType = types.stream()
                .filter(e -> Collections.frequency(types, e) == highestCount)
                .findFirst().orElse(SBFElementType.UNKNOWN);

        if (highestCount < majorityCount) {
            formation.setType(SBFElementType.MX);
        } else {
            formation.setType(highestType);
        }
        report.addLine("Type:",
                "Most frequent: " + highestType + ", " + highestCount + " of " + units.size(),
                formation.getType().toString());
    }
}
