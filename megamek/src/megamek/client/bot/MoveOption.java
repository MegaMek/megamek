/**
 * MegaMek -
 * Copyright (C) 2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

package megamek.client.bot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import megamek.client.ui.SharedUtility;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.Infantry;
import megamek.common.LosEffects;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.PilotingRollData;
import megamek.common.TargetRoll;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.options.OptionsConstants;

/**
 * TODO: add the notion of a dependent state (at least a first pass estimate of
 * worst case threat) for when psr's are made. TODO: add a notion of a blocked
 * move, something that could open up after another mech moves.
 */
public class MoveOption extends MovePath {
    private static final long serialVersionUID = -4517093562444861980L;

    public static class WeightedComparator implements Comparator<MoveOption> {

        private double utility_weight;
        private double damage_weight;

        public WeightedComparator(double utility, double damage) {
            utility_weight = utility;
            damage_weight = damage;
        }

        public int compare(MoveOption e0, MoveOption e1) {
            if (((damage_weight * e0.damage) - (utility_weight * e0.getUtility())) > ((damage_weight
                    * e1.damage) - (utility_weight * e1.getUtility()))) {
                return -1;
            }
            else if (((damage_weight * e0.damage) - (utility_weight * e0.getUtility())) < ((damage_weight
                    * e1.damage) - (utility_weight * e1.getUtility()))) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public static class Table extends HashMap<MovePath.Key, MoveOption> {
        private static final long serialVersionUID = 5926883297848807149L;

        public void put(MoveOption es) {
            this.put(es.getKey(), es);
        }

        public MoveOption get(MoveOption es) {
            return super.get(es.getKey());
        }

        public MoveOption remove(MoveOption es) {
            return super.remove(es.getKey());
        }

        public ArrayList<MoveOption> getArray() {
            return new ArrayList<MoveOption>(values());
        }
    }

    public static class DistanceComparator implements Comparator<MoveOption> {

        public int compare(MoveOption e0, MoveOption e1) {
            return e0.getDistUtility() < e1.getDistUtility() ? -1 : 1;
        }
    }

    public static class DamageInfo {
        double threat;
        double damage;
        double max_threat;
        double min_damage;
    }

    public static final DistanceComparator DISTANCE_COMPARATOR = new DistanceComparator();

    public static final int ATTACK_MOD = 0;
    public static final int DEFENCE_MOD = 1;
    public static final int ATTACK_PC = 2;
    public static final int DEFENCE_PC = 3;

    boolean utilityCalculated = false;
    boolean inDanger = false;
    boolean doomed = false;
    boolean isPhysical = false;

    double utility = 0;
    double self_threat = 0;
    double movement_threat = 0;
    double self_damage = 0;

    double damage = 0;
    double threat = 0;

    private transient CEntity centity;
    transient ArrayList<String> tv = new ArrayList<String>();
    transient HashMap<CEntity, DamageInfo> damageInfos = new HashMap<CEntity, DamageInfo>();
    private Coords pos;
    private int facing;
    private boolean prone;

    public MoveOption(IGame game, CEntity centity) {
        super(game, centity.entity);
        this.centity = centity;
        pos = centity.entity.getPosition();
        facing = centity.entity.getFacing();
        prone = centity.entity.isProne();
    }

    public MoveOption(MoveOption base) {
        this(base.getGame(), base.centity);
        replaceSteps(base.getStepVector());
        threat = base.threat;
        damage = base.damage;
        movement_threat = base.movement_threat;
        tv = new ArrayList<String>(base.tv);
        self_threat = base.self_threat;
        inDanger = base.inDanger;
        doomed = base.doomed;
        isPhysical = base.isPhysical;
        self_damage = base.self_damage;
        pos = base.pos;
        facing = base.facing;
        prone = base.prone;
    }

    @Override
    public MoveOption clone() {
        return new MoveOption(this);
    }

    public double getThreat(CEntity e) {
        return getDamageInfo(e, true).threat;
    }

    public void setThreat(CEntity e, double value) {
        getDamageInfo(e, true).threat = value;
    }

    public double getMinDamage(CEntity e) {
        return getDamageInfo(e, true).min_damage;
    }

    public double getDamage(CEntity e) {
        return getDamageInfo(e, true).damage;
    }

    public void setDamage(CEntity e, double value) {
        getDamageInfo(e, true).damage = value;
    }

    CEntity getCEntity() {
        return centity;
    }

    @Override
    public MoveOption addStep(MoveStepType step_type) {
        super.addStep(step_type);
        MoveStep current = getLastStep();
        // running with gyro or hip hit is dangerous!
        PilotingRollData rollTarget = getEntity().checkRunningWithDamage(current.getMovementType(true));
        if (rollTarget.getValue() != TargetRoll.CHECK_FALSE) {
            getStep(0).setDanger(true);
            current.setDanger(true);
        }

        //Don't jump onto a building with CF < weight
        IHex h = getGame().getBoard().getHex(getFinalCoords());
        if((h != null) && (h.getTerrain(Terrains.BLDG_CF) != null)) {
            int cf = h.getTerrain(Terrains.BLDG_CF).getTerrainFactor();
            if (cf < getEntity().getWeight()) {
                current.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            }
        }
        if (current.isDanger()) {
            if (getCEntity().base_psr_odds < .1) {
                current.setMovementType(EntityMovementType.MOVE_ILLEGAL);
            } else {
                double cur_threat = getCEntity().getThreatUtility(
                        .2 * getEntity().getWeight(), ToHitData.SIDE_REAR)
                        * (1 - Math.pow(getCEntity().base_psr_odds, 2));
                movement_threat += cur_threat;
                if (centity.getTb().debug) {
                    tv.add(cur_threat + " Movement Threat \r\n");
                }
            }
        }
        return this;
    }

    public int getMovementheatBuildup() {
        MoveStep last = getLastStep();
        if (last == null) {
            return 0;
        }
        int heat = last.getTotalHeat();
        int move = 0;
        switch (last.getMovementType(true)) {
        case MOVE_WALK:
        case MOVE_VTOL_WALK:
            move = 1;
            break;
        case MOVE_RUN:
        case MOVE_VTOL_RUN:
            move = 2;
            break;
        case MOVE_SPRINT:
            move = 3;
            break;
        case MOVE_JUMP:
            move = getEntity().getJumpHeat(last.getMpUsed());
            break;
        default:
            move = 1000;
        }
        return heat + move; // illegal?
    }

    public boolean changeToPhysical() {
        if (getEntity() instanceof Infantry) {
            return false;
        }
        MoveStep last = getLastStep();
        if (isJumping()) {
            if (getEntity().canCharge()) {
                return false;
            }
            if (!getEntity().canDFA()) {
                return false;
            }
        } else {
            if (getEntity().canDFA()) {
                return false;
            }
        }
        boolean isClan = getEntity().isClan();
        if ((last == null)
                || (last.getMovementType(true) == EntityMovementType.MOVE_ILLEGAL)) {
            return false;
        }
        if ((last.getType() != MoveStepType.FORWARDS)
                || (isClan
                    && getGame().getOptions().booleanOption(OptionsConstants.ALLOWED_NO_CLAN_PHYSICAL) && (getEntity()
                        .getSwarmAttackerId() == Entity.NONE))) {
            return false;
        }
        // TODO: this just takes the first target
        for (Entity en : getGame().getEntitiesVector(last.getPosition())) {
            if (!en.isSelectableThisTurn() && en.isEnemyOf(getEntity())) {
                isPhysical = true;
                removeLastStep();
                if (isJumping()) {
                    addStep(MoveStepType.DFA, en);
                } else {
                    addStep(MoveStepType.CHARGE, en);
                }
                return true;
            }
        }
        return false;
    }

    // it would be nice to have a stand still move...
    public void setState() {
        setEntity(centity.entity);
        if (getStepVector().isEmpty()) {
            getEntity().setPosition(pos);
            getEntity().setFacing(facing);
            getEntity().setSecondaryFacing(facing);
            getEntity().delta_distance = 0;
            getEntity().setProne(prone);
        } else {
            getEntity().setPosition(getFinalCoords());
            getEntity().setFacing(getFinalFacing());
            getEntity().setSecondaryFacing(getFinalFacing());
            getEntity().setProne(getFinalProne());
            getEntity().delta_distance = getHexesMoved();
        }
        getEntity().moved = getLastStepMovementType();
    }

    /**
     * TODO: replace with more common logic approximates the attack and
     * defensive modifies assumes that set state has been called
     */
    public int[] getModifiers(final Entity te) {
        // set them at the appropriate positions
        final Entity ae = getEntity();

        int attHeight = ae.isProne() ? 0 : 1;
        int targHeight = te.isProne() ? 0 : 1;
        int attEl = 0;
        int targEl = 0;
        attEl = ae.getElevation() + attHeight;
        targEl = te.getElevation() + targHeight;

        boolean pc = false;
        boolean apc = false;

        // get all relevent modifiers
        ToHitData toHita = new ToHitData();
        ToHitData toHitd = new ToHitData();

        toHita.append(Compute.getAttackerMovementModifier(getGame(), ae.getId()));
        toHita.append(Compute.getTargetMovementModifier(getGame(), te.getId()));
        toHita.append(Compute.getTargetTerrainModifier(getGame(), te));
        toHita.append(Compute.getAttackerTerrainModifier(getGame(), ae.getId()));

        toHitd.append(Compute.getAttackerMovementModifier(getGame(), te.getId()));
        toHitd.append(Compute.getTargetMovementModifier(getGame(), ae.getId()));
        if (!(isPhysical && isJumping())) {
            toHitd.append(Compute.getTargetTerrainModifier(getGame(), ae));
        }
        toHitd.append(Compute.getAttackerTerrainModifier(getGame(), te.getId()));

        IHex attHex = getGame().getBoard().getHex(ae.getPosition());
        if (attHex.containsTerrain(Terrains.WATER) && (attHex.surface() > attEl)) {
            toHita.addModifier(TargetRoll.IMPOSSIBLE,
                    "Attacker in depth 2+ water");
            toHitd.addModifier(TargetRoll.IMPOSSIBLE,
                    "Defender in depth 2+ water");
        } else if ((attHex.surface() == attEl) && (ae.height() > 0)) {
            apc = true;
        }
        IHex targHex = getGame().getBoard().getHex(te.getPosition());
        if (targHex.containsTerrain(Terrains.WATER)) {
            if ((targHex.surface() == targEl) && (te.height() > 0)) {
                pc = true;
            } else if (targHex.surface() > targEl) {
                toHita.addModifier(TargetRoll.IMPOSSIBLE,
                        "Attacker in depth 2+ water");
                toHitd.addModifier(TargetRoll.IMPOSSIBLE,
                        "Defender in depth 2+ water");
            }
        }

        // calc & add attacker los mods
        LosEffects los = LosEffects.calculateLos(getGame(), ae.getId(), te);
        toHita.append(los.losModifiers(getGame()));
        // save variables
        pc = los.isTargetCover();
        apc = los.isAttackerCover();
        // reverse attacker & target partial cover & calc defender los mods
        int temp = los.getTargetCover();
        los.setTargetCover(los.getAttackerCover());
        los.setAttackerCover(temp);
        toHitd.append(los.losModifiers(getGame()));

        // heatBuildup
        if (ae.getHeatFiringModifier() != 0) {
            toHita.addModifier(ae.getHeatFiringModifier(), "heatBuildup");
        }
        if (te.getHeatFiringModifier() != 0) {
            toHitd.addModifier(te.getHeatFiringModifier(), "heatBuildup");
        }
        // target immobile
        if (te.isImmobile()) {
            toHita.addModifier(-4, "target immobile");
        }
        if (ae.isImmobile()) {
            toHitd.addModifier(-4, "target immobile");
        }
        final int range = ae.getPosition().distance(te.getPosition());
        // target prone
        if (te.isProne()) {
            // easier when point-blank
            if (range == 1) {
                toHita.addModifier(-2, "target prone and adjacent");
            }
            // harder at range
            if (range > 1) {
                toHita.addModifier(1, "target prone and at range");
            }
        }
        if (ae.isProne()) {
            // easier when point-blank
            if (range == 1) {
                toHitd.addModifier(-2, "target prone and adjacent");
            }
            // harder at range
            if (range > 1) {
                toHitd.addModifier(1, "target prone and at range");
            }
        }
        return new int[] { toHita.getValue(), toHitd.getValue(), apc ? 1 : 0,
                pc ? 1 : 0 };
    }

    /**
     * TODO: the result of this calculation should be cached...
     */
    public double getUtility() {
        if (utilityCalculated) {
            return utility;
        }
        // self threat and self damage are considered transient
        double temp_threat = (threat + movement_threat + self_threat + ((double) getMovementheatBuildup() / 20))
                / getCEntity().strategy.attack;
        double temp_damage = (damage + self_damage) * centity.strategy.attack;
        if ((threat + movement_threat) > (4 * centity.avg_armor)) {
            double ratio = (threat + movement_threat)
                    / (centity.avg_armor + (.25 * centity.avg_iarmor));
            if (ratio > 2) {
                temp_threat += centity.bv / 15.0; // likely to die
                doomed = true;
                inDanger = true;
            } else if (ratio > 1) {
                temp_threat += centity.bv / 30.0; // in danger
                inDanger = true;
            } else {
                temp_threat += centity.bv / 75.0; // in danger
                inDanger = true;
            }
        } else if ((threat + movement_threat) > 30) {
            temp_threat += centity.entity.getWeight();
        }
        double retVal = temp_threat - temp_damage;

        List<TargetRoll> psrList = SharedUtility.getPSRList(this);
        boolean aptPiloting = getEntity().getCrew().getOptions().booleanOption(OptionsConstants
                                                                                       .PILOT_APTITUDE_PILOTING);
        for (TargetRoll roll : psrList) {
            double multiple = Compute.oddsAbove(roll.getValue(), aptPiloting) / 100;
            retVal *= (multiple > 0) ? multiple : 0.01;
        }
        utility = retVal;
        utilityCalculated = true;
        return retVal;
    }

    /**
     * get maximum damage in this current state from enemy accounting for torso
     * twisting and slightly for heat -- the ce passed in is supposed to be the
     * enemy mech
     */
    public double getMaxModifiedDamage(MoveOption enemy, int modifier, int apc) {
        double max = 0;
        int distance = getFinalCoords().distance(enemy.getFinalCoords());
        double mod = 1;
        // heat effect modifiers
        if (enemy.isJumping()
            || ((enemy.getEntity().heat + enemy.getEntity().heatBuildup) > 4)) {
            if (enemy.centity.overheat == CEntity.OVERHEAT_LOW) {
                mod = .75;
            } else if (enemy.centity.overheat == CEntity.OVERHEAT_HIGH) {
                mod = .5;
            } else {
                mod = .9;
            }
        }
        boolean aptGunnery = enemy.getEntity().getCrew().getOptions()
                                  .booleanOption(OptionsConstants.PILOT_APTITUDE_PILOTING);
        int enemy_firing_arcs[] = { 0, 0, 0};
        enemy_firing_arcs[0] =CEntity.getThreatHitArc(enemy
                .getFinalCoords(), MovePath.getAdjustedFacing(enemy
                        .getFinalFacing(), MoveStepType.NONE), getFinalCoords());
        enemy_firing_arcs[0] =CEntity.getThreatHitArc(enemy
                .getFinalCoords(), MovePath.getAdjustedFacing(enemy
                        .getFinalFacing(), MoveStepType.TURN_LEFT), getFinalCoords());
        enemy_firing_arcs[0] =CEntity.getThreatHitArc(enemy
                .getFinalCoords(), MovePath.getAdjustedFacing(enemy
                        .getFinalFacing(), MoveStepType.TURN_RIGHT), getFinalCoords());
        max = enemy.centity.getModifiedDamage((apc == 1) ? CEntity.TT
                                                         : enemy_firing_arcs[0], distance, modifier, aptGunnery);

        if (enemy_firing_arcs[1] == ToHitData.SIDE_FRONT) {
            max = Math.max(max, enemy.centity.getModifiedDamage(CEntity.TT,
                                                                distance, modifier, aptGunnery));
        } else {
            max = Math.max(max, enemy.centity.getModifiedDamage(
                    enemy_firing_arcs[1], distance, modifier, aptGunnery));
        }
        if (enemy_firing_arcs[2] == ToHitData.SIDE_FRONT) {
            max = Math.max(max, enemy.centity.getModifiedDamage(CEntity.TT,
                                                                distance, modifier, aptGunnery));
        } else {
            max = Math.max(max, enemy.centity.getModifiedDamage(
                    enemy_firing_arcs[2], distance, modifier, aptGunnery));
        }
        // TODO this is not quite right, but good enough for now...
        // ideally the pa charaterization should be in centity
        max *= mod;
        if (!enemy.getFinalProne() && (distance == 1)
                && (enemy_firing_arcs[0] != ToHitData.SIDE_REAR)) {
            IHex h = getGame().getBoard().getHex(getFinalCoords());
            IHex h1 = getGame().getBoard().getHex(enemy.getFinalCoords());
            if (Math.abs(h.getLevel() - h1.getLevel()) < 2) {
                max += ((((((h1.getLevel() - h.getLevel()) == 1) || getFinalProne()) ? 5
                        : 1)
                        * ((enemy_firing_arcs[0] == ToHitData.SIDE_FRONT) ? .2
                                : .05)
                        * centity.entity.getWeight()
                        * Compute.oddsAbove(3 + modifier,
                                            getEntity().getCrew().getOptions()
                                                       .booleanOption(OptionsConstants.PILOT_APTITUDE_PILOTING)))
                        / 100)
                        + (((1 - enemy.centity.base_psr_odds)
                            * enemy.getEntity().getWeight()) / 10.0);
            }
        }
        return max;
    }

    public DamageInfo getDamageInfo(CEntity cen, boolean create) {
        DamageInfo result = damageInfos.get(cen);
        if (create && (result == null)) {
            result = new DamageInfo();
            damageInfos.put(cen, result);
        }
        return result;
    }

    public double getDistUtility() {
        return getMpUsed() + ((movement_threat * 100) / centity.bv);
    }

    /**
     * There could still be a problem here, but now it's the callers problem
     */
    int getPhysicalTargetId() {
        MoveStep step = getLastStep();
        if (step == null) {
            return -1;
        }
        Targetable target = step.getTarget(getGame());
        if (target == null) {
            return -1;
        }
        return target.getTargetId();
    }

    @Override
    public String toString() {
        return getEntity().getShortName() + " " + getEntity().getId() + " "
                + getFinalCoords() + " " + super.toString() + "\r\n Utility: "
                + getUtility() + " \r\n" + tv + "\r\n";
    }
}
