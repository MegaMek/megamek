package megamek.client.bot;

import java.util.Enumeration;
import java.util.Vector;

import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.Hex;
import megamek.common.LosEffects;
import megamek.common.Mech;
import megamek.common.MovePath;
import megamek.common.MoveStep;
import megamek.common.Targetable;
import megamek.common.Terrain;
import megamek.common.ToHitData;

import com.sun.java.util.collections.ArrayList;
import com.sun.java.util.collections.Comparator;
import com.sun.java.util.collections.HashMap;
import com.sun.java.util.collections.List;

/**
 * TODO: The hashcode/equals must be over the movement set and the initial
 * starting location/facing.
 * 
 * TODO: add the notion of a dependent state (at least a first pass estimate of
 * worst case threat) for when psr's are made.
 * 
 * TODO: add a notion of a blocked move, something that could open up after
 * another mech moves.
 */
public class MoveOption extends MovePath {

    public static final int STANDING = 1;
    public static final int JUMPING = 2;
    public static final int PRONE = 3;

    public static class Key {
        private Coords coords;
        private int facing;
        private int state;

        public Key(Coords coords, int facing, int state) {
            this.coords = coords;
            this.facing = facing;
            this.state = state;
        }

        public boolean equals(Object obj) {
            Key s1 = (Key) obj;
            if (s1 != null) {
                return state == state && facing == s1.facing && coords.equals(s1.coords);
            }
            return false;
        }

        public int hashCode() {
            return state + 7 * (facing + 31 * coords.hashCode());
        }
    }

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

        public void put(MoveOption es) {
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
    
    public MoveOption(Game game, CEntity centity) {
        super(game, centity.entity);
        this.centity = centity;
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
    }

    public Key getKey() {
        return new Key(getFinalCoords(), getFinalFacing(), getState());
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
	
    public int getState() {
        if (isJumping()) {
            return JUMPING;
        }
        if (getFinalProne()) {
            return PRONE;
        }
        return STANDING;
    }

    CEntity getCEntity() {
        return centity;
    }

    public MovePath addStep(int step_type) {
        super.addStep(step_type);
        MoveStep current = getLastStep();
        // running with gyro or hip hit is dangerous!
        if (current.getMovementType() == Entity.MOVE_RUN
            && (entity.getDestroyedCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT) > 0
                || entity.hasHipCrit())) {
            this.getStep(0).setDanger(true);
            current.setDanger(true);
        }
        if (current.isDanger()) {
            if (getCEntity().base_psr_odds < .1) {
                current.setMovementType(Entity.MOVE_ILLEGAL);
            } else {
                double cur_threat =
                    getCEntity().getThreatUtility(.2 * this.entity.getWeight(), CEntity.SIDE_REAR)
                        * (1 - Math.pow(getCEntity().base_psr_odds, 2));
                this.movement_threat += cur_threat;
                this.tv.add(cur_threat + " Movement Threat \n");
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
            case Entity.MOVE_WALK :
                move = 1;
                break;
            case Entity.MOVE_RUN :
                move = 2;
                break;
            case Entity.MOVE_JUMP :
                move = Math.max(3, last.getMpUsed());
                break;
            default :
                move = 1000;
        }
        return heat + move; // illegal?
    }

    /**
	 * Returns a list of possible moves that result in a
	 * facing/position/(jumping|prone) change, special steps (mine clearing and
	 * such) must be handled elsewhere.
	 */
    public List getNextMoves() {
        ArrayList result = new ArrayList();
        MoveStep last = getLastStep();
        if (isJumping()) {
            MoveOption next = new MoveOption(this);
            for (int i = 0; i < 5; i++) {
                result.add(next);
                result.add(new MoveOption(next).addStep(MovePath.STEP_FORWARDS));
                next = new MoveOption(next);
                next.addStep(MovePath.STEP_TURN_RIGHT);
            }
            return result;
        }
        if (getFinalProne()) {
            if (last != null && last.getType() != STEP_TURN_RIGHT) {
                result.add(new MoveOption(this).addStep(MovePath.STEP_TURN_LEFT));
            }
            if (last != null && last.getType() != STEP_TURN_LEFT) {
                result.add(new MoveOption(this).addStep(MovePath.STEP_TURN_RIGHT));
            }
            result.add(new MoveOption(this).addStep(MovePath.STEP_GET_UP));
            return result;
        } else if (last == null && entity.getJumpMPWithTerrain() > 0) {
            result.add(new MoveOption(this).addStep(STEP_START_JUMP));
        }
        if (canShift()) {
            if (last == null || last.getType() != MovePath.STEP_LATERAL_LEFT) {
                result.add(new MoveOption(this).addStep(STEP_LATERAL_RIGHT));
            }
            if (last == null || last.getType() != MovePath.STEP_LATERAL_RIGHT) {
                result.add(new MoveOption(this).addStep(MovePath.STEP_LATERAL_LEFT));
            }
            if (last == null || last.getType() != MovePath.STEP_LATERAL_LEFT_BACKWARDS) {
                result.add(new MoveOption(this).addStep(MovePath.STEP_LATERAL_RIGHT_BACKWARDS));
            }
            if (last == null || last.getType() != MovePath.STEP_LATERAL_RIGHT_BACKWARDS) {
                result.add(new MoveOption(this).addStep(MovePath.STEP_LATERAL_LEFT_BACKWARDS));
            }
        }
        if (last == null || last.getType() != MovePath.STEP_BACKWARDS) {
            result.add(new MoveOption(this).addStep(MovePath.STEP_FORWARDS));
        }
        if (last == null || last.getType() != MovePath.STEP_TURN_LEFT) {
            result.add(new MoveOption(this).addStep(MovePath.STEP_TURN_RIGHT));
        }
        if (last == null || last.getType() != MovePath.STEP_TURN_RIGHT) {
            result.add(new MoveOption(this).addStep(MovePath.STEP_TURN_LEFT));
        }
        if (last == null || last.getType() != MovePath.STEP_FORWARDS) {
            result.add(new MoveOption(this).addStep(MovePath.STEP_BACKWARDS));
        }
        return result;
    }

    public boolean changeToPhysical() {
        MoveStep last = getLastStep();
        if (last == null && last.getMovementType() == Entity.MOVE_ILLEGAL) {
            return false;
        }
        if (last.getType() != STEP_FORWARDS || isInfantry()) {
            return false;
        }
        Enumeration e = game.getEntities(last.getPosition());
        //TODO: this just takes the first target
        while (e.hasMoreElements()) {
            Entity en = (Entity) e.nextElement();
            if (!en.isSelectableThisTurn(game) && en.isEnemyOf(this.entity)) {
                this.isPhysical = true;
                this.removeLastStep();
                if (isJumping()) {
                    addStep(MovePath.STEP_DFA, en);
                } else {
                    addStep(MovePath.STEP_CHARGE, en);
                }
                compileLastStep();
                return true;
            }
        }
        return false;
    }

    public void setState() {
        this.entity = this.centity.entity;
        this.entity.setPosition(getFinalCoords());
        this.entity.setFacing(getFinalFacing());
        this.entity.setSecondaryFacing(getFinalFacing());
        this.entity.moved = getLastStepMovementType();
        this.entity.setProne(getFinalProne());
        this.entity.delta_distance = getHexesMoved();
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

        Hex attHex = game.board.getHex(ae.getPosition());
        if (attHex.contains(Terrain.WATER) && attHex.surface() > attEl) {
            toHita.addModifier(ToHitData.IMPOSSIBLE, "Attacker in depth 2+ water");
            toHitd.addModifier(ToHitData.IMPOSSIBLE, "Defender in depth 2+ water");
        } else if (attHex.surface() == attEl && ae.height() > 0) {
            apc = true;
        }
        Hex targHex = game.board.getHex(te.getPosition());
        if (targHex.contains(Terrain.WATER)) {
            if (targHex.surface() == targEl && te.height() > 0) {
                pc = true;
            } else if (targHex.surface() > targEl) {
                toHita.addModifier(ToHitData.IMPOSSIBLE, "Attacker in depth 2+ water");
                toHitd.addModifier(ToHitData.IMPOSSIBLE, "Defender in depth 2+ water");
            }
        }

        // calc & add attacker los mods
        LosEffects los = LosEffects.calculateLos(game, ae.getId(), te);
        toHita.append(los.losModifiers());
        // save variables
        pc = los.isTargetCover();
        apc = los.isAttackerCover();
        // reverse attacker & target partial cover & calc defender los mods
        los.setTargetCover(apc);
        los.setAttackerCover(pc);
        toHitd.append(los.losModifiers());

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
        return (temp_threat - temp_damage);
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
        int enemy_firing_arcs[] = {0, MovePath.STEP_TURN_LEFT, MovePath.STEP_TURN_RIGHT};
        for (int i = 0; i < enemy_firing_arcs.length; i++) {
            enemy_firing_arcs[i] =
                CEntity.getThreatHitArc(
                    enemy.getFinalCoords(),
                    MovePath.getAdjustedFacing(enemy.getFinalFacing(), enemy_firing_arcs[i]),
                    getFinalCoords());
        }
        max = enemy.centity.getModifiedDamage((apc == 1) ? CEntity.TT : enemy_firing_arcs[0], distance, modifier);

        if (enemy_firing_arcs[1] == CEntity.SIDE_FRONT) {
            max = Math.max(max, enemy.centity.getModifiedDamage(CEntity.TT, distance, modifier));
        } else {
            max = Math.max(max, enemy.centity.getModifiedDamage(enemy_firing_arcs[1], distance, modifier));
        }
        if (enemy_firing_arcs[2] == CEntity.SIDE_FRONT) {
            max = Math.max(max, enemy.centity.getModifiedDamage(CEntity.TT, distance, modifier));
        } else {
            max = Math.max(max, enemy.centity.getModifiedDamage(enemy_firing_arcs[2], distance, modifier));
        }
        //TODO this is not quite right, but good enough for now...
        //ideally the pa charaterization should be in centity
        max *= mod;
        if (!enemy.getFinalProne() && distance == 1 && enemy_firing_arcs[0] != CEntity.SIDE_REAR) {
            Hex h = game.board.getHex(getFinalCoords());
            Hex h1 = game.board.getHex(enemy.getFinalCoords());
            if (Math.abs(h.getElevation() - h1.getElevation()) < 2) {
                max += ((h1.getElevation() - h.getElevation() == 1 || getFinalProne()) ? 5 : 1)
                    * ((enemy_firing_arcs[0] == CEntity.SIDE_FRONT) ? .2 : .05)
                    * centity.entity.getWeight()
                    * Compute.oddsAbove(3 + modifier)
                    / 100
                    + (1 - enemy.centity.base_psr_odds) * enemy.entity.getWeight() / 10.0;
            }
        }
        return max;
    }
    
    public DamageInfo getDamageInfo(CEntity cen, boolean create) {
        DamageInfo result = (DamageInfo)damageInfos.get(cen);
        if (create && result == null) {
            result = new DamageInfo();
            damageInfos.put(cen, result);
        }
        return result;
    }

    public double getDistUtility() {
        return getMpUsed() + movement_threat * 100 / centity.bv;
    }

    int getPhysicalTargetId() {
        Targetable target = getLastStep().getTarget(game);
        if (target == null) {
            return -1;
        }
        return target.getTargetId();
    }
}
