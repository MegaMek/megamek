/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 */
package megamek.client.bot;

import java.util.Enumeration;

import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.WeaponType;

import com.sun.java.util.collections.ArrayList;
import com.sun.java.util.collections.Arrays;
import com.sun.java.util.collections.Collections;
import com.sun.java.util.collections.Hashtable;
import com.sun.java.util.collections.Iterator;

public class CEntity {

    static class Table extends Hashtable {

        private TestBot tb;

        public Table(TestBot tb) {
            this.tb = tb;
        }

        public void put(CEntity es) {
            this.put(es.getKey(), es);
        }

        public CEntity get(Entity es) {
            CEntity result = null;
            if ((result = (CEntity) super.get(new Integer(es.getId()))) == null) {
                result = new CEntity(es, tb);
                this.put(result);
            }
            return result;
        }

        public CEntity get(int id) {
            return (CEntity) get(new Integer(id));
        }
    }

    final static double TANK_ARMOR[][] = { { 0, 1.0, 0, 0, 0 }, {
            0, 0, 0, 0, 1.0 }, {
            0, 0, 0, 1.0, 0 }, {
            0, 0, 1.0, 0, 0 }
    };
    final static double TANK_WT_ARMOR[][] = { { 0, 31.0 / 36, 0, 0, 0, 5.0 / 36 }, {
            0, 0, 0, 0, 31.0 / 36, 5.0 / 36 }, {
            0, 0, 0, 31.0 / 36, 0, 5.0 / 36 }, {
            0, 0, 31.0 / 36, 0, 0, 5.0 / 36 }
    };
    final static double INFANTRY_ARMOR[][] = { { 1.0 }, {
            1.0 }, {
            1.0 }, {
            1.0 }
    };
    final static double PROTOMECH_ARMOR[][] = { { 1.0 / 31, 16.0 / 31, 3.0 / 31, 3.0 / 31, 8.0 / 31 }, {
        1.0 /31, 16.0 / 31, 3.0 / 31, 3.0 / 31, 8.0 / 31 }, {
        1.0 /31, 16.0 / 31, 3.0 / 31, 3.0 / 31, 8.0 / 31 }, {
        1.0 /31, 16.0 / 31, 3.0 / 31, 3.0 / 31, 8.0 / 31 }
    };
    final static double PROTOMECH_MG_ARMOR[][] = { { 1.0 / 32, 16.0 / 32, 3.0 / 32, 3.0 / 32, 8.0 / 32, 1.0 / 32 }, {
        1.0 / 31, 16.0 / 32, 3.0 / 32, 3.0 / 32, 8.0 / 32, 1.0 / 32 }, {
        1.0 / 31, 16.0 / 32, 3.0 / 32, 3.0 / 32, 8.0 / 32, 1.0 / 32 }, {
        1.0 / 31, 16.0 / 32, 3.0 / 32, 3.0 / 32, 8.0 / 32, 1.0 / 32 }
    };
    final static double MECH_ARMOR[][] =
        { { 1.0 / 36, 7.0 / 36, 6.0 / 36, 6.0 / 36, 4.0 / 36, 4.0 / 36, 4.0 / 36, 4.0 / 36 }, {
            1.0 / 36, 7.0 / 36, 6.0 / 36, 6.0 / 36, 4.0 / 36, 4.0 / 36, 4.0 / 36, 4.0 / 36 }, {
            1.0 / 36, 6.0 / 36, 4.0 / 36, 7.0 / 36, 2.0 / 36, 6.0 / 36, 2.0 / 36, 8.0 / 36 }, {
            1.0 / 36, 6.0 / 36, 7.0 / 36, 4.0 / 36, 6.0 / 36, 2.0 / 36, 8.0 / 36, 2.0 / 36 }
    };

    public static final int MAX_RANGE = 24;

    public static final int OVERHEAT_NONE = 0;
    public static final int OVERHEAT_LOW = 1;
    public static final int OVERHEAT_HIGH = 2;

    public static final int RANGE_SHORT = 0;
    public static final int RANGE_MEDIUM = 1;
    public static final int RANGE_LONG = 2;
    public static final int RANGE_ALL = 3;

    public static final int FIRST_ARC = 0;
    public static final int LAST_PRIMARY_ARC = 3;
    public static final int LAST_ARC = 4;

    public static final int TT = 4;

    public static final int LEFT_LEG = 0;
    public static final int RIGHT_LEG = 1;

    Entity entity;
    MoveOption current;
    MoveOption last; // set only after movement

    private MoveOption.Table moves;
    MoveOption.Table pass = new MoveOption.Table();
    public int runMP;
    public int jumpMP;

    boolean isPhysicalTarget = false;

    int overheat = OVERHEAT_NONE;
    int range = RANGE_ALL;
    int long_range = 0;
    double range_damages[] = new double[4];

    double base_psr_odds = 1.0;

    boolean hasTakenDamage = false;
    public Strategy strategy = new Strategy();

    //a subjective measure of the armor quality
    double[] armor_health = { 0, 0, 0, 0 };
    double[] armor_percent = { 0, 0, 0, 0 };
    double avg_armor = 0;
    double avg_iarmor = 0;

    //used to determine the utility of combining attacks
    double[] expected_damage = { 0, 0, 0, 0 };
    double[] possible_damage = { 0, 0, 0, 0 };

    double[] leg_health = { 0, 0 };

    double overall_armor_percent = 0;
    double[][] damages = new double[6][MAX_RANGE + 1];

    //the battle value of the mech
    int bv;

    //relative position in the enemy array
    int enemy_num;

    private TestBot tb;

    boolean engaged = false; //am i fighting
    boolean moved = false;
    boolean justMoved = false;

    // TSM equipped Mechs work better at 9+ heat, so flag if mounted
    boolean tsm_offset = false;

    int[] minRangeMods = new int[7];

    public CEntity(Entity en, TestBot tb) {
        this.entity = en;
        this.tb = tb;
        this.reset();
    }

    public Entity getEntity() {
        return entity;
    }

    public boolean canMove() {
        return (
            entity.isSelectableThisTurn() && !(entity.isProne() && base_psr_odds < .2) && !entity.isImmobile());
    }

    public boolean justMoved() {
        return (!moved && !entity.isSelectableThisTurn()) || justMoved;
    }

    public void reset() {
        this.entity = tb.game.getEntity(this.entity.getId()); //fresh entity
        for (int a = FIRST_ARC; a <= LAST_ARC; a++) {
            Arrays.fill(this.damages[a], 0);
        }
        this.characterize();
        this.resetPossibleDamage();
        this.moves = null;
        this.hasTakenDamage = false;
        Arrays.fill(expected_damage, 0);
        this.engaged = false;
        this.moved = false;
        this.isPhysicalTarget = false;
    }

    public void refresh() {
        this.entity = tb.game.getEntity(this.entity.getId());
        if (justMoved()) {
            for (int a = FIRST_ARC; a <= LAST_ARC; a++) {
                Arrays.fill(this.damages[a], 0);
            }
            this.characterize();
            this.resetPossibleDamage();
        }
    }

    public void resetPossibleDamage() {
        Arrays.fill(possible_damage, 0);
    }

    public void characterize() {
        this.entity = tb.game.getEntity(this.entity.getId());
        this.current = new MoveOption(tb.game, this);
        this.bv = entity.calculateBattleValue();
        this.runMP = entity.getRunMP();
        this.jumpMP = entity.getJumpMP();
        this.overall_armor_percent = entity.getArmorRemainingPercent();
        this.base_psr_odds = Compute.oddsAbove(entity.getBasePilotingRoll().getValue()) / 100;

        // If this is a Mech equipped with TSM, push for the sweet spot at 9 heat
        if (this.entity instanceof Mech){
            if (((Mech)tb.game.getEntity(this.entity.getId())).hasTSM()){
                this.tsm_offset = true;
            }
        }

        //begin weapons characterization
        double heat_mod = .9; //these estimates are consistently too high
        if (entity.heat > 7)
            heat_mod = .8; //reduce effectiveness
        if (entity.heat > 12)
            heat_mod = .5;
        if (this.tsm_offset){
            if (entity.heat == 9){
                heat_mod = 1.0;
            }
            if (entity.heat < 12 && entity.heat > 9){
                heat_mod = 0.8;
            }
        }
        if (entity.heat > 16)
            heat_mod = .35;
        int capacity = entity.getHeatCapacity();
        int heat_total = 0;
        Enumeration weapons = entity.getWeapons();
        int num_weapons = 0;
        this.minRangeMods = new int[7];
        while (weapons.hasMoreElements()) {
            num_weapons++;
            Mounted m = (Mounted) weapons.nextElement();
            int arc = entity.getWeaponArc(entity.getEquipmentNum(m));
            WeaponType weapon = (WeaponType) m.getType();
            final boolean usesAmmo = weapon.getAmmoType() != AmmoType.T_NA;
            final Mounted ammo = usesAmmo ? m.getLinked() : null;
            if (m.isDestroyed())
                continue;
            if (usesAmmo && (ammo == null || ammo.getShotsLeft() == 0))
                continue;
            heat_total += weapon.getHeat();
            int min = weapon.getMinimumRange();
            int sr = weapon.getShortRange();
            int mr = weapon.getMediumRange();
            int lr = weapon.getLongRange();
            double ed = CEntity.getExpectedDamage(weapon);
            double odds = 0;
            int gunnery = entity.getCrew().getGunnery();
            for (int curRange = 1; curRange <= lr && curRange <= MAX_RANGE;
                 curRange++) {
                if (curRange <= min) {
                    if (curRange < 7)
                        this.minRangeMods[curRange] += 1 + min - curRange;
                    odds = Compute.oddsAbove(gunnery + 1 + min - curRange)
                        / 100.0;
                } else if (curRange <= sr) {
                    odds = Compute.oddsAbove(gunnery) / 100.0;
                } else if (curRange <= mr) {
                    odds = Compute.oddsAbove(gunnery + 2) / 100.0;
                } else if (curRange <= lr) {
                    odds = Compute.oddsAbove(gunnery + 4) / 100.0;
                }
                //weapons unaffected by heat don't get penalized
                this.addDamage(
                    arc,
                    entity.isSecondaryArcWeapon(entity.getEquipmentNum(m)),
                    curRange,
                    ed * odds * ((weapon.getHeat() > 0) ? heat_mod : 1));
                this.long_range = Math.max(this.long_range, curRange);
            }
        }
        for (int r = 1; r < this.minRangeMods.length; r++) {
            if (num_weapons > 0)
                this.minRangeMods[r] = (int) Math.round(((double) this.minRangeMods[r]) / (double) num_weapons);
        }
        //what type of overheater am I
        int heat = heat_total - capacity;
        // Include heat from active stealth armor
        if (entity instanceof Mech && entity.isStealthActive()){
            heat += 10;
        }
        // Include heat from infernos
        if (entity.infernos.isStillBurning()){
            heat += 6;
        }
        // Include heat from engine hits
        if (entity instanceof Mech){
            heat += entity.getEngineCritHeat();
        }
        // Include heat for standing in a fire
        if (entity.getPosition() != null){
            if (tb.game.getBoard().getHex(entity.getPosition()) != null) {
	            if (tb.game.getBoard().getHex(entity.getPosition()).
	                    terrainLevel(Terrains.FIRE) == 2) {
	                heat += 5;
	            }
            }
        }
        // Include heat from ambient temperature
        heat += tb.game.getTemperatureDifference();
        if (heat < 8 && heat > 3) {
            this.overheat = OVERHEAT_LOW;
        } else if (heat > 12) {
            this.overheat = OVERHEAT_HIGH;
        }
        //only worries about external armor
        double max = 1;
        double armor[][] = MECH_ARMOR;
        if (this.entity instanceof Tank) {
            if (((Tank) entity).hasNoTurret()) {
                armor = TANK_ARMOR;
            } else {
                armor = TANK_WT_ARMOR;
            }
        } else if (this.entity instanceof Infantry) {
            armor = INFANTRY_ARMOR;
        } else if (this.entity instanceof Protomech) {
            if (((Protomech) entity).hasMainGun()) {
                armor = PROTOMECH_MG_ARMOR;
            } else {
                armor = PROTOMECH_ARMOR;
            }
        }
        for (int arc = FIRST_ARC; arc <= LAST_PRIMARY_ARC; arc++) {
            double total = 0;
            for (int i = 0; i < armor[arc].length; i++) {
                total += armor[arc][i] * getArmorValue(i, arc == ToHitData.SIDE_REAR);
            }
            this.armor_health[arc] = total;
            max = Math.max(armor_health[arc], max);
        }
        this.avg_armor = (armor_health[0] + armor_health[1] + armor_health[2] + armor_health[3]) / 4;
        this.avg_iarmor = this.entity.getTotalInternal() / 5;
        for (int arc = FIRST_ARC; arc <= LAST_PRIMARY_ARC; arc++) {
            this.armor_percent[arc] = this.armor_health[arc] / max;
        }
        this.computeRange();
    }

    /**
     * Add a statistical damage into the damage table. the arc is based upon
     * firing arc Compute.XXXX --this is not yet exact, rear tt not accounted
     * for, and arm flipping is ignored
     */
    private void addDamage(int arc, boolean secondary, int r, double ed) {
        this.damages[firingArcToHitArc(arc)][r] += ed;
        if (arc != Compute.ARC_REAR && arc != Compute.ARC_360) {
            damages[ToHitData.SIDE_FRONT][r] += ed;
            if (secondary) {
                damages[TT][r] += ed;
            }
        }
        if (arc == Compute.ARC_360) {
            for (int i = FIRST_ARC; i <= LAST_ARC; i++)
                damages[i][r] += ed;
        }
    }

    /**
     * Computes the range (short, meduim, long, all) of the mec.
     */
    private void computeRange() {
        Arrays.fill(range_damages, 0);
        for (int base = 0; base < 3; base++) {
            for (int i = 1 + 6 * base; i < 8 + 6 * base; i++) {
                range_damages[base] += this.damages[ToHitData.SIDE_FRONT][i];
            }
            range_damages[base] /= 8;
            range_damages[3] += range_damages[base];
        }
        range_damages[3] /= 3;
        if (range_damages[0] > 2.5 * range_damages[1]) {
            this.range = RANGE_SHORT;
        } else if (range_damages[1] > 2.5 * range_damages[2]) {
            this.range = RANGE_MEDIUM;
        } else if (range_damages[2] > .25 * range_damages[0]) {
            this.range = RANGE_LONG;
        } else {
            this.range = RANGE_ALL;
        }
    }

    protected double getArmorValue(int loc, boolean rear) {
        double result = entity.getArmor(loc, rear);
        double percent = result / entity.getOArmor(loc, rear);
        if (percent < .25) {
            result *= .5;
        } else if (percent < .60) {
            result *= .8;
        }
        return result;
    }

    /**
     * The utility of something done against me. -- uses the arcs defined by
     * ToHitData
     */
    public double getThreatUtility(double threat, int arc) {
        double t1 = threat;
        double t2 = threat;
        //relative bonus for weak side
        if (armor_percent[arc] < .75) {
            t1 *= 1.1;
        } else if (armor_percent[arc] < .5) {
            t1 *= 1.3;
        } else if (armor_percent[arc] < .25) {
            t1 *= 1.5;
        }
        //absolute bonus for damage that is likely to do critical
        if (t2 + this.expected_damage[arc] > this.armor_health[arc]) {
            //damage saturation check, only if we have more mechs
            if ((t2
                + this.expected_damage[0]
                + this.expected_damage[1]
                + this.expected_damage[2]
                + this.expected_damage[3]
                > 3 * (this.avg_armor + this.avg_iarmor)
                || (this.entity.isProne() && this.base_psr_odds < .1 && !this.entity.isImmobile()))) {
                if (this.entity.isEnemyOf((Entity) tb.getEntitiesOwned().get(0))) {
                    return Math.sqrt(t2) * this.strategy.target;
                }
            }
            t2 *= 1.5;
        } else if (this.expected_damage[arc] > 0) {
            t2 *= 1.3; //for clustering damage
        } else if (this.hasTakenDamage) {
            t2 *= 1.1; //for coordinating fire
        }
        return Math.max(t1, t2) * this.strategy.target;
    }

    public Integer getKey() {
        return new Integer(this.entity.getId());
    }

    public MoveOption.Table getAllMoves() {
        if (moves == null) {
            moves = calculateMoveOptions(this.current);
        }
        return moves;
    }

    /**
     * From the current state, explore based upon an implementation of
     * Dijkstra's algorithm.
     */
    protected MoveOption.Table calculateMoveOptions(MoveOption base) {
        ArrayList possible = new ArrayList();
        MoveOption.Table discovered = new MoveOption.Table();

        if (entity.getJumpMPWithTerrain() > 0) {
            possible.add(((MovePath) base.clone()).addStep(MovePath.STEP_START_JUMP));
        }

        possible.add(base);
        discovered.put(base);

        while (possible.size() > 0) {
            MoveOption min = (MoveOption) possible.remove(0);
            Iterator adjacent = min.getNextMoves(true, true).iterator();
            while (adjacent.hasNext()) {
                MoveOption next = (MoveOption) adjacent.next();
                if (next.changeToPhysical() && next.isMoveLegal()) {
                    discovered.put(next);
                } else if (next.isMoveLegal()) {
                    //relax edges;
                    if (discovered.get(next) == null
                        || (next.getDistUtility() < discovered.get(next).getDistUtility())) {
                        discovered.put(next);
                        if (next.isJumping()) {
                            MovePath left = (MoveOption) next.clone();
                            MovePath right = (MoveOption) next.clone();
                            for (int turn = 0; turn < 2; turn++) {
                                left.addStep(MovePath.STEP_TURN_LEFT);
                                right.addStep(MovePath.STEP_TURN_RIGHT);
                                discovered.put(((MovePath) left.clone()));
                                discovered.put(((MovePath) right.clone()));
                            }
                            right.addStep(MovePath.STEP_TURN_RIGHT);
                            discovered.put(right);
                        }
                        int index = Collections.binarySearch(possible, next, MoveOption.DISTANCE_COMPARATOR);
                        if (index < 0) {
                            index = -index - 1;
                        }
                        possible.add(index, next);
                    }
                }
            }
        }
        //Final check for illeagal and extra weighting for heat
        for (Iterator i = discovered.values().iterator(); i.hasNext();) {
            MoveOption next = (MoveOption) i.next();
            next.clipToPossible();
            if (!next.isMoveLegal()) {
                i.remove();
            }
            if (entity.heat > 4) {
                next.movement_threat += this.bv / 1000 * next.getMovementheatBuildup();
                if (entity.heat > 7) {
                    next.movement_threat += this.bv / 500 * next.getMovementheatBuildup();
                }
                if (this.tsm_offset){
                    if (entity.heat == 9){
                        next.movement_threat -= this.bv / 100 * next.getMovementheatBuildup();
                    }
                    if (entity.heat < 12 && entity.heat > 9){
                        next.movement_threat -= this.bv / 500 * next.getMovementheatBuildup();
                    }
                }
                if (entity.heat > 12) {
                    next.movement_threat += this.bv / 100 * next.getMovementheatBuildup();
                }
            }
        }
        return discovered;
    }

    /**
     *  find all moves that get into dest
     */
    public ArrayList findMoves(Coords dest) {
        ArrayList result = new ArrayList();
        for (int i = 0; i < 6; i++) {
            for (int j = 1; j < 2; j++) {
                MoveOption.Key key = new MoveOption.Key(dest, i, j);
                MoveOption es = null;
                if ((es = (MoveOption) moves.get(key)) != null) {
                    result.add(es);
                }
            }
        }
        return result;
    }

    /**
     * given my skill and the present modifiers, what is a better estimate of
     * my damage dealing -- actual and not utility
     */
    public double getModifiedDamage(int arc, int a_range, int modifier) {
        if (a_range > MAX_RANGE)
            return 0;
        double damage = this.damages[arc][a_range];
        int base = entity.getCrew().getPiloting();
        int dist_mod = 0;
        if (a_range < 7) {
            dist_mod = this.minRangeMods[a_range];
        } else if (a_range < 13) {
            dist_mod = 2;
        } else if (a_range < MAX_RANGE) {
            dist_mod = 4;
        } else { //will need to be changed for extended range weapons
            dist_mod = 20;
        }
        if (base + dist_mod + modifier > tb.ignore)
            return 0;
        if (base + dist_mod + modifier == tb.ignore)
            damage *= .5;
        return (damage / Compute.oddsAbove(base + dist_mod) * Compute.oddsAbove(dist_mod + modifier + base));
    }

    public static double getExpectedDamage(WeaponType weap) {
        if (weap.getDamage() != WeaponType.DAMAGE_MISSILE) {
            // normal weapon
            return weap.getDamage();
        } else {
            // hard-coded expected missile numbers
            if (weap.getAmmoType() == AmmoType.T_SRM) {
                switch (weap.getRackSize()) {
                    case 2 :
                        return 1.41666 * 2;
                    case 4 :
                        return 2.63888 * 2;
                    case 6 :
                        return 4 * 2;
                }
            } else {
                switch (weap.getRackSize()) {
                    case 5 :
                        return 3.16666;
                    case 10 :
                        return 6.30555;
                    case 15 :
                        return 9.5;
                    case 20 :
                        return 12.69444;
                }
            }
        }
        return 0;
    }

    public static int getFiringAngle(final Coords dest, int dest_facing, final Coords src) {
        int fa = dest.degree(src) - (dest_facing % 6) * 60;
        if (fa < 0) {
            fa += 360;
        } else if (fa >= 360) {
            fa -= 360;
        }
        return fa;
    }

    public static int getThreatHitArc(Coords dest, int dest_facing, Coords src) {
        int fa = getFiringAngle(dest, dest_facing, src);
        if (fa >= 300 || fa <= 60)
            return ToHitData.SIDE_FRONT;
        if (fa >= 60 && fa <= 120)
            return ToHitData.SIDE_RIGHT;
        if (fa >= 240 && fa <= 300)
            return ToHitData.SIDE_LEFT;
        else
            return ToHitData.SIDE_REAR;
    }

    public static int firingArcToHitArc(int arc) {
        switch (arc) {
            case Compute.ARC_FORWARD :
                return ToHitData.SIDE_FRONT;
            case Compute.ARC_LEFTARM :
                return ToHitData.SIDE_LEFT;
            case Compute.ARC_RIGHTARM :
                return ToHitData.SIDE_RIGHT;
            case Compute.ARC_REAR :
                return ToHitData.SIDE_REAR;
            case Compute.ARC_LEFTSIDE:
                return ToHitData.SIDE_LEFT;
            case Compute.ARC_RIGHTSIDE:
                return ToHitData.SIDE_RIGHT;
        }
        return 0;
    }

    public boolean equals(Object obj) {
        if (obj instanceof Entity || obj instanceof CEntity) {
            return obj.hashCode() == hashCode();
        }
        return false;
    }

    public int hashCode() {
        return entity.getId();
    }

}
