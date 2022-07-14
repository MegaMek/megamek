/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.*;
import megamek.common.alphaStrike.ASDamageVector;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.force.Force;
import megamek.common.force.Forces;

import java.util.ArrayList;

import static megamek.common.alphaStrike.BattleForceSUA.*;

public class ACSCombatTeamConverter {

    /**
     *  Returns an ACS Combat Team formed from the given force. When the force cannot be converted
     *  to an ACS Combat Team according to the rules, returns null. The given force must be
     *  approximately company-shaped to work, i.e. it has to contain some subforces with some entities
     *  in each subforce but no further subforces.
     */
    public static ACSCombatTeam convert(Force force, Game game, boolean includePilots) {
        if (!SBFFormationConverter.canConvertToSbfFormation(force, game)) {
            return null;
        }
        var result = new ACSCombatTeam();
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
        setTeamStats(result, force.getName());
        return result;
    }

    /** Calculates the ACS Combat Team stats for the SBF Units it must already contain. */
    private static void setTeamStats(ACSCombatTeam team, String name) {
        team.setName(name);
        team.setType(SBFFormationConverter.calcFormationType(team.getUnits()));
        team.setSize((int)Math.round(team.getUnits().stream().mapToDouble(SBFUnit::getSize).average().orElse(0)));
        team.setMovement((int)Math.round(team.getUnits().stream().mapToDouble(SBFUnit::getMovement).average().orElse(0)));
        team.setTrspMovement((int)Math.round(team.getUnits().stream().mapToDouble(SBFUnit::getTrspMovement).average().orElse(0)));
        team.setJumpMove((int)Math.round(team.getUnits().stream().mapToDouble(SBFUnit::getJumpMove).average().orElse(0)));
        team.setTmm((int)Math.round(team.getUnits().stream().mapToDouble(SBFUnit::getTmm).average().orElse(0)));
        team.setSkill((int)Math.round(team.getUnits().stream().mapToDouble(SBFUnit::getSkill).average().orElse(0)));
        SBFFormationConverter.calcFormationSpecialAbilities(team);
        setTeamArmor(team);
        setTeamDamage(team);
        team.setTactics(SBFFormationConverter.getFormationTactics(team));
        team.setMorale(3 + team.getSkill());
        team.setPointValue(team.getUnits().stream().mapToInt(SBFUnit::getPointValue).sum());
    }

    private static void setTeamArmor(ACSCombatTeam team) {
        double armor = 0;
        for (SBFUnit unit : team.getUnits()) {
            armor += unit.getArmor();
            armor += unit.hasAnySPAOf(PNT) ? (int) unit.getSPA(PNT) : 0;
            armor += unit.hasAnySPAOf(SCR) ? (int) unit.getSPA(SCR) : 0;
        }
        team.setArmor((int) Math.round(armor / 3));
    }

    private static void setTeamDamage(ACSCombatTeam team) {
        double dmgS = team.getUnits().stream().mapToInt(u -> u.getDamage().S.damage).sum();
        double dmgM = team.getUnits().stream().mapToInt(u -> u.getDamage().M.damage).sum();
        double dmgL = team.getUnits().stream().mapToInt(u -> u.getDamage().L.damage).sum();
        double dmgE = team.getUnits().stream().mapToInt(u -> u.getDamage().E.damage).sum();
        //TODO: Is IF on Combat Teams / Combat Units used in ACS?
        //TODO: Should IF be calculated per Unit?
        //TODO: Multiple FIring Arcs
//        dmgL += units.stream()    This is per Unit
//                .filter(e -> e.hasSPA(IF))
//                .map(e -> (ASDamageVector) e.getSPA(IF))
//                .mapToInt(d -> (int) Math.round(1.0 / 3 * d.S.damage))
//                .sum();
        dmgL += Math.round(team.getUnits().stream()
                .filter(e -> e.hasSPA(IF))
                .map(e -> (ASDamageVector) e.getSPA(IF))
                .mapToDouble(d -> d.S.damage)
                .sum() / 3);
        int finalS = (int) Math.round(dmgS / 3);
        int finalM = (int) Math.round(dmgM / 3);
        int finalL = (int) Math.round(dmgL / 3);
        int finalE = (int) Math.round(dmgE / 3);
        team.setDamage(ASDamageVector.create(finalS, finalM, finalL, finalE, team.isGround() ? 3 : 4));
    }




}
