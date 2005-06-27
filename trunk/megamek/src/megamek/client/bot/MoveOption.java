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

import java.util.Enumeration;
import java.util.Vector;

import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.IEntityMovementType;
import megamek.common.Infantry;
import megamek.common.LosEffects;
import megamek.common.Mech;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.Protomech;
import megamek.common.Targetable;
import megamek.common.Terrains;
import megamek.common.ToHitData;

import com.sun.java.util.collections.ArrayList;
import com.sun.java.util.collections.Comparator;
import com.sun.java.util.collections.HashMap;

/**
 * TODO: add the notion of a dependent state (at least a first pass estimate of
 * worst case threat) for when psr's are made.
 * 
 * TODO: add a notion of a blocked move, something that could open up after
 * another mech moves.
 */
public class MoveOption extends MovePath implements Cloneable {

    public static class WeightedComparator implements Comparator {

        private double utility_weight;
        private double damage_weight;

        public WeightedComparator(double utility, double damage) {
            utility_weight = utility;
            damage_weight = damage;
        }

        public int compare(Object arg0, Object arg1) {
            MoveOption e0 = (MoveOption) arg0;
            MoveOption e1 = (MoveOption) arg1;
            if (damage_weight * e0.damage - utility_weight * e0.getUtility()
                > damage_weight * e1.damage - utility_weight * e1.getUtility()) {
                return -1;
            }
            return 1;
        }
    }

    public static class Table extends HashMap {

        public void put(MovePath es) {
            this.put(es.getKey(), es);
        }

        public MoveOption get(MoveOption es) {
            return (MoveOption) super.get(es.getKey());
        }

        public MoveOption remove(MoveOption es) {
            return (MoveOption) super.remove(es.getKey());
        }
    }

    public static class DistanceComparator implements Comparator {

        public int compare(Object arg0, Object arg1) {
            MoveOption e0 = (MoveOption) arg0;
            MoveOption e1 = (MoveOption) arg1;
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

    boolean inDanger = false;
    boolean doomed = false;
    boolean isPhysical = false;

    double self_threat = 0;
    double movement_threat = 0;
    double self_damage = 0;

    double damage = 0;
    double threat = 0;

    private transient CEntity centity;
    transient ArrayList tv = new ArrayList();
    transient HashMap damageInfos = new HashMap();
    private Coords pos;
    private int facing;
    private boolean prone;

    public MoveOption(IGame game, CEntity centity) {
        super(game, centity.entity);
        this.centity = centity;
        this.pos = centity.entity.getPosition();
        this.facing = centity.entity.getFacing();
        this.prone = centity.entity.isProne();
    }

    public MoveOption(MoveOption base) {
        this(base.game, base.centity);
        steps = (Vector) base.steps.clone();
        this.threat = base.threat;
        this.damage = base.damage;
        this.movement_threat = base.movement_threat;
        this.tv = new ArrayList(tv);
        this.self_threat = base.self_threat;
        this.inDanger = base.inDanger;
        this.doomed = base.doomed;
        this.isPhysical = base.isPhysical;
        this.self_damage = base.self_damage;
        this.pos = base.pos;
        this.facing = base.facing;
        this.prone = base.prone;
    }

    public Object clone() {
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

    public MovePath addStep(int step_type) {
        super.addStep(step_type);
        MoveStep current = getLastStep();
        // running with gyro or hip hit is dangerous!
        if (current.getMovementType() == IEntityMovementType.MOVE_RUN
            && (entity.getBadCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT) > 0
                || entity.hasHipCrit())) {
            this.getStep(0).setDanger(true);
            current.setDanger(true);
        }
        if (current.isDanger()) {
            if (getCEntity().base_psr_odds < .1) {
                current.setMovementType(IEntityMovementType.MOVE_ILLEGAL);
            } else {
                double cur_threat =
                    getCEntity().getThreatUtility(.2 * this.entity.getWeight(), ToHitData.SIDE_REAR)
                        * (1 - Math.pow(getCEntity().base_psr_odds, 2));
                this.movement_threat += cur_threat;
                this.tv.add(cur_threat + " Movement Threat \r\n");
            }
        }
        return this;
    }

    public int getMovementheatBuildup() {
        MoveStep last = this.getLastStep();
        if (last == null)
            return 0;
        int heat = last.getTotalHeat();
        int move = 0;
        switch (last.getMovementType()) {
            case IEntityMovementType.MOVE_WALK :
                move = 1;
                break;
            case IEntityMovementType.MOVE_RUN :
                move = 2;
                break;
            case IEntityMovementType.MOVE_JUMP :
                move = Math.max(3, last.getMpUsed());
                break;
            default :
                move = 1000;
        }
        return heat + move; // illegal?
    }

    public boolean changeToPhysical() {
        MoveStep last = getLastStep();
        boolean isInfantry = (getEntity() instanceof Infantry);
        boolean isProtomech = (getEntity() instanceof Protomech);
        boolean isClan = getEntity().isClan();
        if (last == null || last.getMovementType() == IEntityMovementType.MOVE_ILLEGAL) {
            return false;
        }
        if (last.getType() != STEP_FORWARDS
            || isInfantry
            || isProtomech
            || ( isClan
                 && game.getOptions().booleanOption("no_clan_physical") )) { //$NON-NLS-1$
            return false;
        }
        Enumeration e = game.getEntities(last.getPosition());
        //TODO: this just takes the first target
        while (e.hasMoreElements()) {
            Entity en = (Entity) e.nextElement();
            if (!en.isSelectableThisTurn() && en.isEnemyOf(this.entity)) {
                this.isPhysical = true;
                this.removeLastStep();
                if (isJumping()) {
                    addStep(MovePath.STEP_DFA, en);
                } else {
                    addStep(MovePath.STEP_CHARGE, en);
                }
                return true;
            }
        }
        return false;
    }

    //it would be nice to have a stand still move...
    public void setState() {
        this.entity = this.centity.entity;
        if (this.steps.size() == 0) {
            this.entity.setPosition(pos);
            this.entity.setFacing(facing);
            this.entity.setSecondaryFacing(facing);
            this.entity.delta_distance = 0;
            this.entity.setProne(prone);
        } else {
            this.entity.setPosition(getFinalCoords());
            this.entity.setFacing(getFinalFacing());
            this.entity.setSecondaryFacing(getFinalFacing());
            this.entity.setProne(getFinalProne());
            this.entity.delta_distance = getHexesMoved();
        }
        this.entity.moved = getLastStepMovementType();
    }

    /**
     * TODO: replace with more common logic
     * 
     * approximates the attack and defensive modifies assumes that set state
     * has been called
     */
    public int[] getModifiers(final Entity te) {
        //set them at the appropriate positions
        final Entity ae = this.entity;

        int attHeight = ae.isProne() ? 0 : 1;
        int targHeight = te.isProne() ? 0 : 1;
        int attEl = 0;
        int targEl = 0;
        attEl = ae.getElevation() + attHeight;
        targEl = te.getElevation() + targHeight;

        boolean pc = false;
        boolean apc = false;

        //get all relevent modifiers
        ToHitData toHita = new ToHitData();
        ToHitData toHitd = new ToHitData();

        toHita.append(Compute.getAttackerMovementModifier(game, ae.getId()));
        toHita.append(Compute.getTargetMovementModifier(game, te.getId()));
        toHita.append(Compute.getTargetTerrainModifier(game, te));
        toHita.append(Compute.getAttackerTerrainModifier(game, ae.getId()));

        toHitd.append(Compute.getAttackerMovementModifier(game, te.getId()));
        toHitd.append(Compute.getTargetMovementModifier(game, ae.getId()));
        if (!(this.isPhysical && isJumping())) {
            toHitd.append(Compute.getTargetTerrainModifier(game, ae));
        }
        toHitd.append(Compute.getAttackerTerrainModifier(game, te.getId()));

        IHex attHex = game.getBoard().getHex(ae.getPosition());
        if (attHex.containsTerrain(Terrains.WATER) && attHex.surface() > attEl) {
            toHita.addModifier(ToHitData.IMPOSSIBLE, "Attacker in depth 2+ water");
            toHitd.addModifier(ToHitData.IMPOSSIBLE, "Defender in depth 2+ water");
        } else if (attHex.surface() == attEl && ae.height() > 0) {
            apc = true;
        }
        IHex targHex = game.getBoard().getHex(te.getPosition());
        if (targHex.containsTerrain(Terrains.WATER)) {
            if (targHex.surface() == targEl && te.height() > 0) {
                pc = true;
            } else if (targHex.surface() > targEl) {
                toHita.addModifier(ToHitData.IMPOSSIBLE, "Attacker in depth 2+ water");
                toHitd.addModifier(ToHitData.IMPOSSIBLE, "Defender in depth 2+ water");
            }
        }

        // calc & add attacker los mods
        LosEffects los = LosEffects.calculateLos(game, ae.getId(), te);
        toHita.append(los.losModifiers(game));
        // save variables
        pc = los.isTargetCover();
        apc = los.isAttackerCover();
        // reverse attacker & target partial cover & calc defender los mods
        int temp = los.getTargetCover();
        los.setTargetCover(los.getAttackerCover());
        los.setAttackerCover(temp);
        toHitd.append(los.losModifiers(game));

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
        return new int[] { toHita.getValue(), toHitd.getValue(), apc ? 1 : 0, pc ? 1 : 0 };
    }

    /**
     * TODO: the result of this calculation should be cached...
     */
    public double getUtility() {
        //self threat and self damage are considered transient
        double temp_threat =
            (this.threat + this.movement_threat + this.self_threat + (double) this.getMovementheatBuildup() / 20)
                / getCEntity().strategy.attack;
        double temp_damage = (this.damage + this.self_damage) * this.centity.strategy.attack;
        if (this.threat + this.movement_threat > 4 * this.centity.avg_armor) {
            double ratio =
                (this.threat + this.movement_threat) / (this.centity.avg_armor + .25 * this.centity.avg_iarmor);
            if (ratio > 2) {
                temp_threat += this.centity.bv / 15.0; //likely to die
                this.doomed = true;
                this.inDanger = true;
            } else if (ratio > 1) {
                temp_threat += this.centity.bv / 30.0; //in danger
                this.inDanger = true;
            } else {
                temp_threat += this.centity.bv / 75.0; //in danger
                this.inDanger = true;
            }
        } else if (this.threat + this.movement_threat > 30) {
            temp_threat += this.centity.entity.getWeight();
        }
        double retVal = temp_threat - temp_damage;
        if (hasActiveMASC()) {
            int mascTN = 0;
            for (final Enumeration i = getSteps(); i.hasMoreElements();) {
                MoveStep step = (MoveStep) i.nextElement();
                if (step.isUsingMASC() && step.getTargetNumberMASC() > mascTN) {
                    mascTN = step.getTargetNumberMASC();
                }
            }
            double mascMult = Compute.oddsAbove(mascTN)/100;
            retVal *= (mascMult > 0) ? mascMult : 0.01;
        }
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
        if (enemy.isJumping() || (enemy.entity.heat + enemy.entity.heatBuildup > 4)) {
            if (enemy.centity.overheat == CEntity.OVERHEAT_LOW) {
                mod = .75;
            } else if (enemy.centity.overheat == CEntity.OVERHEAT_HIGH) {
                mod = .5;
            } else {
                mod = .9;
            }
        }
        int enemy_firing_arcs[] = { 0, MovePath.STEP_TURN_LEFT, MovePath.STEP_TURN_RIGHT };
        for (int i = 0; i < enemy_firing_arcs.length; i++) {
            enemy_firing_arcs[i] =
                CEntity.getThreatHitArc(
                    enemy.getFinalCoords(),
                    MovePath.getAdjustedFacing(enemy.getFinalFacing(), enemy_firing_arcs[i]),
                    getFinalCoords());
        }
        max = enemy.centity.getModifiedDamage((apc == 1) ? CEntity.TT : enemy_firing_arcs[0], distance, modifier);

        if (enemy_firing_arcs[1] == ToHitData.SIDE_FRONT) {
            max = Math.max(max, enemy.centity.getModifiedDamage(CEntity.TT, distance, modifier));
        } else {
            max = Math.max(max, enemy.centity.getModifiedDamage(enemy_firing_arcs[1], distance, modifier));
        }
        if (enemy_firing_arcs[2] == ToHitData.SIDE_FRONT) {
            max = Math.max(max, enemy.centity.getModifiedDamage(CEntity.TT, distance, modifier));
        } else {
            max = Math.max(max, enemy.centity.getModifiedDamage(enemy_firing_arcs[2], distance, modifier));
        }
        //TODO this is not quite right, but good enough for now...
        //ideally the pa charaterization should be in centity
        max *= mod;
        if (!enemy.getFinalProne() && distance == 1 && enemy_firing_arcs[0] != ToHitData.SIDE_REAR) {
            IHex h = game.getBoard().getHex(getFinalCoords());
            IHex h1 = game.getBoard().getHex(enemy.getFinalCoords());
            if (Math.abs(h.getElevation() - h1.getElevation()) < 2) {
                max += ((h1.getElevation() - h.getElevation() == 1 || getFinalProne()) ? 5 : 1)
                    * ((enemy_firing_arcs[0] == ToHitData.SIDE_FRONT) ? .2 : .05)
                    * centity.entity.getWeight()
                    * Compute.oddsAbove(3 + modifier)
                    / 100
                    + (1 - enemy.centity.base_psr_odds) * enemy.entity.getWeight() / 10.0;
            }
        }
        return max;
    }

    public DamageInfo getDamageInfo(CEntity cen, boolean create) {
        DamageInfo result = (DamageInfo) damageInfos.get(cen);
        if (create && result == null) {
            result = new DamageInfo();
            damageInfos.put(cen, result);
        }
        return result;
    }

    public double getDistUtility() {
        return getMpUsed() + movement_threat * 100 / centity.bv;
    }

    /**
     * There could still be a problem here, but now it's the
     * callers problem
     */
    int getPhysicalTargetId() {
        MoveStep step = getLastStep();
        if (step == null) {
            return -1;
        }
        Targetable target = step.getTarget(game);
        if (target == null) {
            return -1;
        }
        return target.getTargetId();
    }

    public String toString() {
        return getEntity().getShortName()
            + " "
            + getEntity().getId()
            + " "
            + getFinalCoords()
            + " "
            + super.toString()
            + "\r\n Utility: "
            + getUtility()
            + " \r\n"
            + tv
            + "\r\n";
    }
}
