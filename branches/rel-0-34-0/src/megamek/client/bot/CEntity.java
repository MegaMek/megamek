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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;

import megamek.client.Client;
import megamek.client.ui.SharedUtility;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.Terrains;
import megamek.common.ToHitData;
import megamek.common.WeaponType;

public class CEntity {

    static class Table extends HashMap<Integer, CEntity> {

        /**
         *
         */
        private static final long serialVersionUID = 6437109733397107056L;
        private TestBot tb;

        public Table(TestBot tb) {
            this.tb = tb;
        }

        public void put(CEntity es) {
            this.put(es.getKey(), es);
        }

        public CEntity get(Entity es) {
            CEntity result = null;
            if ((result = super.get(new Integer(es.getId()))) == null) {
                result = new CEntity(es, tb);
                this.put(result);
            }
            return result;
        }

        public CEntity get(int id) {
            return get(new Integer(id));
        }
    }

    // Armor values based on [ToHitData.SIDE_XXX][location static int]
    // Locations are as defined in static variables in various unit type classes
    // Values are the odds (out of 1.00) of rolling that location on the to-hit
    // table

    // Tank armor is either the side hit or the turret
    final static double TANK_ARMOR[][] = { { 0, 1.0, 0, 0, 0 },
            { 0, 0, 0, 0, 1.0 }, { 0, 0, 0, 1.0, 0 }, { 0, 0, 1.0, 0, 0 } };
    final static double TANK_WT_ARMOR[][] = {
            { 0, 31.0 / 36, 0, 0, 0, 5.0 / 36 },
            { 0, 0, 0, 0, 31.0 / 36, 5.0 / 36 },
            { 0, 0, 0, 31.0 / 36, 0, 5.0 / 36 },
            { 0, 0, 31.0 / 36, 0, 0, 5.0 / 36 } };

    // Infantry don't have a facing. In fact, they don't have armor...
    final static double INFANTRY_ARMOR[][] = { { 1.0 }, { 1.0 }, { 1.0 },
            { 1.0 } };

    // Battle armor units have multiple suits
    final static double ISBA_ARMOR[][] = { { 0.25, 0.25, 0.25, 0.25 },
            { 0.25, 0.25, 0.25, 0.25 }, { 0.25, 0.25, 0.25, 0.25 },
            { 0.25, 0.25, 0.25, 0.25 } };
    final static double CLBA_ARMOR[][] = { { 0.2, 0.2, 0.2, 0.2, 0.2 },
            { 0.2, 0.2, 0.2, 0.2, 0.2 }, { 0.2, 0.2, 0.2, 0.2, 0.2 },
            { 0.2, 0.2, 0.2, 0.2, 0.2 } };
    final static double PROTOMECH_ARMOR[][] = {
            { 1.0 / 31, 16.0 / 31, 3.0 / 31, 3.0 / 31, 8.0 / 31 },
            { 1.0 / 31, 16.0 / 31, 3.0 / 31, 3.0 / 31, 8.0 / 31 },
            { 1.0 / 31, 16.0 / 31, 3.0 / 31, 3.0 / 31, 8.0 / 31 },
            { 1.0 / 31, 16.0 / 31, 3.0 / 31, 3.0 / 31, 8.0 / 31 } };
    final static double PROTOMECH_MG_ARMOR[][] = {
            { 1.0 / 32, 16.0 / 32, 3.0 / 32, 3.0 / 32, 8.0 / 32, 1.0 / 32 },
            { 1.0 / 31, 16.0 / 32, 3.0 / 32, 3.0 / 32, 8.0 / 32, 1.0 / 32 },
            { 1.0 / 31, 16.0 / 32, 3.0 / 32, 3.0 / 32, 8.0 / 32, 1.0 / 32 },
            { 1.0 / 31, 16.0 / 32, 3.0 / 32, 3.0 / 32, 8.0 / 32, 1.0 / 32 } };
    final static double MECH_ARMOR[][] = {
            { 1.0 / 36, 7.0 / 36, 6.0 / 36, 6.0 / 36, 4.0 / 36, 4.0 / 36,
                    4.0 / 36, 4.0 / 36 },
            { 1.0 / 36, 7.0 / 36, 6.0 / 36, 6.0 / 36, 4.0 / 36, 4.0 / 36,
                    4.0 / 36, 4.0 / 36 },
            { 1.0 / 36, 6.0 / 36, 4.0 / 36, 7.0 / 36, 2.0 / 36, 6.0 / 36,
                    2.0 / 36, 8.0 / 36 },
            { 1.0 / 36, 6.0 / 36, 7.0 / 36, 4.0 / 36, 6.0 / 36, 2.0 / 36,
                    8.0 / 36, 2.0 / 36 } };
    final static double GUN_EMPLACEMENT_ARMOR[][] = { { 1.0 / 4, 0, 0, 0 },
            { 1.0 / 4, 0, 0, 0 }, { 1.0 / 4, 0, 0, 0 }, { 1.0 / 4, 0, 0, 0 } };
    final static double GUN_EMPLACEMENT_TURRET_ARMOR[][] = {
            { 1.0 / 3, 0, 0, 0, 5.0 / 36 }, { 1.0 / 3, 0, 0, 0, 5.0 / 36 },
            { 1.0 / 3, 0, 0, 0, 5.0 / 36 }, { 1.0 / 3, 0, 0, 0, 5.0 / 36 } };

    public static final int MAX_RANGE = 36; // Updated to reflect longer ranges
                                            // of level 2 equipment
    public static final int MIN_BRACKET = 6;

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
    // For MASC/supercharger useage. Set to true if failure is bad.
    public boolean masc_threat = false;

    boolean isPhysicalTarget = false;

    // Heat characterization of this unit
    int overheat = OVERHEAT_NONE;

    // Ideal engagement ranges
    int range = RANGE_ALL;
    int long_range = 0;
    double range_damages[] = new double[4];
    int rd_bracket = 0;

    double base_psr_odds = 1.0;

    boolean hasTakenDamage = false;
    public Strategy strategy = new Strategy();

    // A subjective measure of the armor quality indexed by ToHitData
    // location static variables (front, rear, left, right)
    double[] armor_health = { 0, 0, 0, 0, 0, 0, 0, 0 };
    double[] armor_percent = { 0, 0, 0, 0, 0, 0, 0, 0 };
    // Armor averaged over all locations
    // TODO: replace with array, one element per arc
    double avg_armor = 0;
    // Average internal structure
    // TODO: replace with array, one element per arc
    double avg_iarmor = 0;

    // used to determine the utility of combining attacks
    double[] expected_damage = { 0, 0, 0, 0, 0, 0, 0, 0 };
    double[] possible_damage = { 0, 0, 0, 0, 0, 0, 0, 0 };

    double[] leg_health = { 0, 0 };

    double overall_armor_percent = 0.0;
    double[][] damages = new double[6][MAX_RANGE + 1];

    // the battle value of the mech
    int bv;

    // relative position in the enemy array
    int enemy_num;

    private TestBot tb;

    boolean engaged = false; // am i fighting
    boolean moved = false;
    boolean justMoved = false;

    // TSM equipped Mechs work better at 9+ heat, so flag if mounted
    boolean tsm_offset = false;

    int[] minRangeMods = new int[MIN_BRACKET + 1];

    public CEntity(Entity en, TestBot tb) {
        entity = en;
        this.tb = tb;
        reset();
    }

    public Entity getEntity() {
        return entity;
    }

    public boolean canMove() {
        return (entity.isSelectableThisTurn()
                && !(entity.isProne() && (base_psr_odds < .2)) && !entity
                .isImmobile());
    }

    public boolean justMoved() {
        return (!moved && !entity.isSelectableThisTurn()) || justMoved;
    }

    public void reset() {
        entity = tb.game.getEntity(entity.getId()); // fresh entity
        for (int a = FIRST_ARC; a <= LAST_ARC; a++) {
            Arrays.fill(damages[a], 0);
        }
        characterize();
        resetPossibleDamage();
        moves = null;
        hasTakenDamage = false;
        Arrays.fill(expected_damage, 0);
        engaged = false;
        moved = false;
        isPhysicalTarget = false;
    }

    public void refresh() {
        entity = tb.game.getEntity(entity.getId());
        if (justMoved()) {
            for (int a = FIRST_ARC; a <= LAST_ARC; a++) {
                Arrays.fill(damages[a], 0);
            }
            characterize();
            resetPossibleDamage();
        }
    }

    public void resetPossibleDamage() {
        Arrays.fill(possible_damage, 0);
    }

    public void characterize() {
        entity = tb.game.getEntity(entity.getId());
        current = new MoveOption(tb.game, this);
        bv = entity.calculateBattleValue();
        runMP = entity.getRunMP();
        jumpMP = entity.getJumpMP();
        overall_armor_percent = entity.getArmorRemainingPercent();
        base_psr_odds = Compute.oddsAbove(entity.getBasePilotingRoll()
                .getValue()) / 100;

        // If this is a Mech equipped with TSM, push for the sweet spot at 9
        // heat
        if (entity instanceof Mech) {
            if (((Mech) tb.game.getEntity(entity.getId())).hasTSM()) {
                tsm_offset = true;
            }
        }

        // Calculate a modifer to damage based on the units current heat level
        double heat_mod = .9; // these estimates are consistently too high
        if (entity.heat > 7) {
            heat_mod = .8; // reduce effectiveness
        }
        if (entity.heat > 12) {
            heat_mod = .5;
        }
        if (tsm_offset) {
            if (entity.heat == 9) {
                heat_mod = 1.0;
            }
            if ((entity.heat < 12) && (entity.heat > 9)) {
                heat_mod = 0.8;
            }
        }
        if (entity.heat > 16) {
            heat_mod = .35;
        }
        int capacity = entity.getHeatCapacity();

        int heat_total = 0;
        int num_weapons = 0;

        // Offensive characterization - damage potentials
        for (Mounted m : entity.getWeaponList()) {

            int arc = entity.getWeaponArc(entity.getEquipmentNum(m));

            WeaponType weapon = (WeaponType) m.getType();

            // Don't count weapons that don't have ammo, are destroyed/jammed
            if (m.isDestroyed()) {
                continue;
            }
            if (m.isJammed()) {
                continue;
            }
            if (weapon.getName() == "Stop Swarm Attack") {
                // attack
                continue;
            }

            if ((m.getLinked() == null)
                    & (weapon.getAmmoType() != AmmoType.T_NA)) {
                continue;
            }

            num_weapons++;
            heat_total += weapon.getHeat();

            int min = weapon.getMinimumRange();
            int lr = weapon.getExtremeRange();

            double ed;
            int gunnery = entity.getCrew().getGunnery();
            if (entity.getTaserFeedBackRounds() > 0) {
                gunnery += 1;
            }
            // Range used to start at 1; updated to account for stacking
            for (int curRange = 0; (curRange <= lr) && (curRange <= MAX_RANGE); curRange++) {

                ed = CEntity.getExpectedDamage(entity, m, curRange, gunnery);
                if ((curRange <= min) & (curRange <= MIN_BRACKET)) {
                    minRangeMods[curRange] += 1 + min - curRange;
                }

                // Weapons that generate heat are derated based on the
                // units current heat level, 'cause building more heat is bad
                addDamage(arc, entity.isSecondaryArcWeapon(entity
                        .getEquipmentNum(m)), curRange, ed
                        * ((weapon.getHeat() > 0) ? heat_mod : 1));
            }
            long_range = Math
                    .max(long_range, Math.min(lr, MAX_RANGE));
        }

        // Add in approximate physical damages from Mechs

        if (entity instanceof Mech) {
            addDamage(Compute.ARC_LEFTARM, true, 1, (tsm_offset ? 1.0
                    : 0.5)
                    * entity.getWeight()
                    / 10
                    * Compute.oddsAbove(entity.getCrew().getPiloting())
                    / 100);
            addDamage(Compute.ARC_RIGHTARM, true, 1, (tsm_offset ? 1.0
                    : 0.5)
                    * entity.getWeight()
                    / 10
                    * Compute.oddsAbove(entity.getCrew().getPiloting())
                    / 100);
        }

        // Average out the minimum range modifiers
        for (int r = 1; r < minRangeMods.length; r++) {
            if (num_weapons > 0) {
                minRangeMods[r] = (int) Math
                        .round(((double) minRangeMods[r])
                                / (double) num_weapons);
            }
        }

        computeRange(Compute.ARC_FORWARD);

        // Heat characterization - how badly will it overheat this round

        int heat = 0;
        if (entity instanceof Mech) {
            heat = heat_total - capacity;
            // Include heat from active stealth armor
            if ((entity instanceof Mech) && (entity.isStealthActive()
                    || entity.isNullSigActive()
                    || entity.isVoidSigActive())) {
                heat += 10;
            }
            if ((entity instanceof Mech) && entity.isChameleonShieldActive()) {
                heat += 6;
            }
            // Include heat from infernos
            if (entity.infernos.isStillBurning()) {
                heat += 6;
            }
            // Include heat from engine hits
            if (entity instanceof Mech) {
                heat += entity.getEngineCritHeat();
            }
            // Include heat for standing in a fire
            if (entity.getPosition() != null) {
                if (tb.game.getBoard().getHex(entity.getPosition()) != null) {
                    if (tb.game.getBoard().getHex(entity.getPosition())
                            .containsTerrain(Terrains.FIRE) &&
                            (tb.game.getBoard().getHex(entity.getPosition()).getFireTurn() > 0)) {
                        heat += 5;
                    }
                }
            }
            // Include heat from ambient temperature
            heat += tb.game.getPlanetaryConditions().getTemperatureDifference(50,-30);
        }

        if (heat <= 4) {
            overheat = OVERHEAT_NONE;
        }
        if (heat > 3) {
            overheat = OVERHEAT_LOW;
        }
        if ((heat > 9) & !tsm_offset) {
            overheat = OVERHEAT_HIGH;
        }
        if ((heat > 12) & tsm_offset) {
            overheat = OVERHEAT_HIGH;
        }

        // Make a guess as to whether MASC should be turned on or off
        // TODO: Link this to a Bot configuration file

        if (entity instanceof Mech) {
            if (((Mech) entity).hasMASC()) {
                if (((Mech) entity).getMASCTarget() <= 5 + Compute.randomInt(6)) {
                    masc_threat = false;
                } else {
                    masc_threat = true;
                }
            }
        }

        // Defensive characterization - protection values

        double max = 1.0;

        // Initialize armor values
        double armor[][] = MECH_ARMOR;

        if (entity instanceof Tank) {
            if (((Tank) entity).hasNoTurret()) {
                armor = TANK_ARMOR;
            } else {
                armor = TANK_WT_ARMOR;
            }
        }

        if (entity instanceof Infantry) {
            if (!(entity instanceof BattleArmor)) {
                armor = INFANTRY_ARMOR;
            } else {
                if (entity.isClan()) {
                    armor = CLBA_ARMOR;
                } else {
                    armor = ISBA_ARMOR;
                }
            }
        }

        if (entity instanceof Protomech) {
            if (((Protomech) entity).hasMainGun()) {
                armor = PROTOMECH_MG_ARMOR;
            } else {
                armor = PROTOMECH_ARMOR;
            }
        }

        if (entity instanceof GunEmplacement) {
            armor = ((GunEmplacement) entity).hasTurret() ? GUN_EMPLACEMENT_TURRET_ARMOR
                    : GUN_EMPLACEMENT_ARMOR;
        }

        // Arcs for the outside loop are those used for incoming damage:
        // front, rear, left, right as per static variables in ToHitData
        for (int arc = FIRST_ARC; arc <= LAST_PRIMARY_ARC; arc++) {
            armor_health[arc] = 0.0;
            // "i" is a location index. It matches the location static variables
            // for each sub-entity type (Mech, Tank, etc)
            for (int i = 0; i < armor[arc].length; i++) {
                armor_health[arc] += armor[arc][i]
                        * getArmorValue(i, arc == ToHitData.SIDE_REAR);
            }

            // ProtoMechs have a "near miss" location that isn't accounted for
            // in
            // the hit-table-probability array. Rolling 3 or 11 on 2d6 is 4/36.

            if (entity instanceof Protomech) {
                armor_health[arc] *= 1.22;
            }
            max = Math.max(armor_health[arc], max);
            armor_percent[arc] = armor_health[arc] / max;
        }

        // Calculate an average armor value for the entire entity
        // TODO: Change to array, with an entry for each arc
        avg_armor = (armor_health[0] + armor_health[1] + armor_health[2] + armor_health[3]) / 4;

        // Calculate average internal structure across the unit
        // Default to Mech unit, which has 7 locations plus the head (ignored
        // due to low
        // hit probability and low standard IS)
        avg_iarmor = entity.getTotalInternal() / 7.0;

        if (entity instanceof Infantry) {
            avg_iarmor = entity instanceof BattleArmor ? ((BattleArmor) entity)
                    .getShootingStrength()
                    : 1.0;
        }
        if (entity instanceof Tank) {
            avg_iarmor = entity.getTotalInternal()
                    / (((Tank) entity).hasNoTurret() ? 4.0 : 5.0);
        }
        if (entity instanceof Protomech) {
            avg_iarmor = entity.getTotalInternal()
                    / (((Protomech) entity).hasMainGun() ? 5.0 : 6.0);
        }
        if (entity instanceof GunEmplacement) {
            avg_iarmor = 1.0;
        }

    }

    /**
     * Add a statistical damage into the arc/range table. The arc is based upon
     * firing arcs defined in Compute. If the unit is a Mech capable of flipping
     * arms, full arm damages are applied to the rear arc; otherwise only add
     * half as the Mech can only twist in one direction.
     */
    private void addDamage(int arc, boolean secondary, int r, double ed) {

        if ((arc == Compute.ARC_360) | (arc == Compute.ARC_MAINGUN)) {
            for (int i = FIRST_ARC; i <= LAST_ARC; i++) {
                damages[i][r] += ed;
            }
        } else {

            if (secondary) { // Weapon can be applied to a secondary arc,
                // such as a torso twist or turret rotate
                if (arc == Compute.ARC_FORWARD) {
                    damages[Compute.ARC_FORWARD][r] += ed;
                    damages[Compute.ARC_LEFTARM][r] += ed;
                    damages[Compute.ARC_RIGHTARM][r] += ed;
                }
                // Flipping arms allows both arms to fire into the rear arc.
                // Otherwise, its limited to one. Approximate with a 50/50
                // split.
                if ((arc == Compute.ARC_LEFTARM) | (arc == Compute.ARC_LEFTSIDE)) {
                    damages[Compute.ARC_FORWARD][r] += ed;
                    damages[Compute.ARC_LEFTARM][r] += ed;
                    damages[Compute.ARC_REAR][r] += entity.canFlipArms() ? ed
                            : (0.5 * ed);
                }
                if ((arc == Compute.ARC_RIGHTARM) | (arc == Compute.ARC_RIGHTSIDE)) {
                    damages[Compute.ARC_FORWARD][r] += ed;
                    damages[Compute.ARC_RIGHTARM][r] += ed;
                    damages[Compute.ARC_REAR][r] += entity.canFlipArms() ? ed
                            : (0.5 * ed);
                }
                if (arc == Compute.ARC_REAR) {
                    damages[Compute.ARC_REAR][r] += ed;
                    damages[Compute.ARC_LEFTARM][r] += ed;
                    damages[Compute.ARC_RIGHTARM][r] += ed;
                }

            } else {
                if (arc == Compute.ARC_FORWARD) {
                    damages[Compute.ARC_FORWARD][r] += ed;
                }
                if ((arc == Compute.ARC_LEFTARM) | (arc == Compute.ARC_LEFTSIDE)) {
                    damages[Compute.ARC_FORWARD][r] += ed;
                    damages[Compute.ARC_LEFTARM][r] += ed;
                }
                if ((arc == Compute.ARC_RIGHTARM) | (arc == Compute.ARC_RIGHTSIDE)) {
                    damages[Compute.ARC_FORWARD][r] += ed;
                    damages[Compute.ARC_RIGHTARM][r] += ed;
                }
                if (arc == Compute.ARC_REAR) {
                    damages[Compute.ARC_REAR][r] += ed;
                }
            }
        }
    }

    /**
     * Fills range damage values for short, medium, long, and all, plus sets the
     * range bracket for the entity, which is 1/3 of long range or 1/4 extreme
     * range. The arc argument follows Compute.ARC_XXX format.
     */
    public void computeRange(int arc) {

        double optimizer[] = { 0, 0, 0, 0 };

        Arrays.fill(range_damages, 0);

        // Create short, medium, and long range values for each arc

        rd_bracket = long_range / 4;

        for (int range_walk = (entity instanceof Infantry ? 0 : 1); range_walk < long_range; range_walk++) {
            if (range_walk <= rd_bracket) {
                optimizer[RANGE_SHORT] += damages[arc][range_walk];
                range_damages[RANGE_SHORT] += damages[arc][range_walk];
            }
            if ((range_walk > rd_bracket)
                    & (range_walk <= 2 * rd_bracket)) {
                optimizer[RANGE_MEDIUM] += getModifiedDamage(arc,
                        range_walk, -2);
                range_damages[RANGE_MEDIUM] += damages[arc][range_walk];
            }
            if ((range_walk > 2 * rd_bracket)
                    & (range_walk <= 3 * rd_bracket)) {
                optimizer[RANGE_LONG] += getModifiedDamage(arc,
                        range_walk, -4);
                range_damages[RANGE_LONG] += damages[arc][range_walk];
            }
            range_damages[RANGE_ALL] += damages[arc][range_walk];
        }

        if (rd_bracket > 0) {
            range_damages[RANGE_SHORT] /= rd_bracket;
            optimizer[RANGE_SHORT] /= rd_bracket;
            range_damages[RANGE_MEDIUM] /= rd_bracket;
            optimizer[RANGE_MEDIUM] /= rd_bracket;
            range_damages[RANGE_LONG] /= rd_bracket;
            optimizer[RANGE_LONG] /= rd_bracket;
            range_damages[RANGE_ALL] /= (rd_bracket * 3);
        } else {
            range_damages[RANGE_SHORT] = damages[arc][0];
            optimizer[RANGE_SHORT] = damages[arc][0];
            range_damages[RANGE_MEDIUM] = damages[arc][0];
            optimizer[RANGE_MEDIUM] = damages[arc][0];
            range_damages[RANGE_LONG] = damages[arc][0];
            optimizer[RANGE_LONG] = damages[arc][0];
            range_damages[RANGE_ALL] = damages[arc][0];
        }

        // Now determine the preferred range. Use damage values based on no
        // range modifiers, but retain the range-based damage values for
        // further use.

        int best_range = RANGE_ALL;

        for (range = RANGE_SHORT; range <= RANGE_LONG; range++) {
            if (optimizer[range] > optimizer[best_range]) {
                best_range = range;
            }
        }
        range = best_range;
    }

    /**
     * Returns the value of the armor in the indicated location. Conventional
     * infantry and battle armor behave differently than other units. Armor is
     * in integer units.
     */
    protected int getArmorValue(int loc, boolean rear) {
        int result = entity.getArmor(loc, rear);

        // Conventional infantry don't have armor (yet), so use the number of
        // troopers
        // TODO: This will probably need some revamping when Total Warfare is
        // released.
        if ((entity instanceof Infantry) & !(entity instanceof BattleArmor)) {
            result = ((Infantry) entity).getShootingStrength();
        }

        // Battle armor has armor per trooper; treat each trooper as a
        // "location"
        if (entity instanceof BattleArmor) {
            result = ((BattleArmor) entity).getArmor(loc, false);
        }

        if (result <= 0) {
            result = 0;
        }

        return result;
    }

    /**
     * The utility of something done against me. -- uses the arcs defined by
     * ToHitData (FRONT, REAR, LEFT, RIGHT). Takes a damage value and adjusts it
     * based on the characterized entities armor/internal values in that arc.
     * Also adjusts based on the strategy module being used by this entity.
     */
    public double getThreatUtility(double threat, int arc) {
        double t1 = threat;
        double t2 = threat;
        // relative bonus for weak side
        if (armor_percent[arc] < .75) {
            t1 *= 1.1;
        } else if (armor_percent[arc] < .5) {
            t1 *= 1.3;
        } else if (armor_percent[arc] < .25) {
            t1 *= 1.5;
        }
        t1 *= strategy.target;

        // absolute bonus for damage that is likely to do critical
        if (t2 + expected_damage[arc] > armor_health[arc]) {

            // expected_damage[] is set on the fly; it tracks damage
            // the entity expects to take
            if (((t2 + expected_damage[0] + expected_damage[1]
                    + expected_damage[2] + expected_damage[3] > 3 * (avg_armor + avg_iarmor)) || (entity
                    .isProne()
                    && (base_psr_odds < .1) && !entity.isImmobile()))) { // If I
                                                                                    // have
                                                                                    // more
                                                                                    // friends,
                                                                                    // this
                                                                                    // isn't
                                                                                    // so
                                                                                    // bad
                if (entity.isEnemyOf(tb.getEntitiesOwned().get(0))) {
                    return Math.sqrt(t2) * strategy.target;
                }
            }
            t2 *= 1.5; // Damage that penetrates armor is bad for me
            // Even if damage doesn't penetrate, some damage to this arc isn't
            // that great
        } else if (expected_damage[arc] > 0) {
            t2 *= 1.3;

            // Even if this arc is still good, taking additional damage isn't a
            // good thing
        } else if (hasTakenDamage) {
            t2 *= 1.1;
        }

        // Return the worst case, either massive damage or penetration to
        // internals

        return Math.max(t1, t2);
    }

    public Integer getKey() {
        return new Integer(entity.getId());
    }

    public MoveOption.Table getAllMoves(Client client) {
        if (moves == null) {
            moves = calculateMoveOptions(current, client);
        }
        return moves;
    }

    /**
     * From the current state, explore based upon an implementation of
     * Dijkstra's algorithm.
     */
    protected MoveOption.Table calculateMoveOptions(MoveOption base, Client client) {
        //New array of movement options
        ArrayList<MoveOption> possible = new ArrayList<MoveOption>();
        MoveOption.Table discovered = new MoveOption.Table();

        // Add the seed for jumping if allowed
        if (entity.getJumpMPWithTerrain() > 0) {
            possible.add((base.clone()).addStep(MovePath.STEP_START_JUMP));
        }

        possible.add(base); // Add the base movement option to the arraylist of
                            // possibles
        discovered.put(base); // Add the base movement option to the movement
                                // option table

        while (possible.size() > 0) { // Keep going until the arraylist is
                                        // empty (why?)

            // Get the first movement option, while stripping it from the
            // arraylist
            MoveOption min = possible.remove(0);
            Iterator<MovePath> adjacent = min.getNextMoves(true, true)
                    .iterator();
            while (adjacent.hasNext()) {
                MoveOption next = (MoveOption) adjacent.next();
                if (next.changeToPhysical() && next.isMoveLegal()) {
                    discovered.put(next);
                } else if (next.isMoveLegal()) {
                    // relax edges;
                    if ((discovered.get(next) == null)
                            || (next.getDistUtility() < discovered.get(next)
                                    .getDistUtility())) {
                        discovered.put(next);
                        if (next.isJumping()) {
                            MoveOption left = next.clone();
                            MoveOption right = next.clone();
                            // Think about skipping this for infantry, which
                            // have no facing
                            for (int turn = 0; turn < 2; turn++) {
                                left.addStep(MovePath.STEP_TURN_LEFT);
                                right.addStep(MovePath.STEP_TURN_RIGHT);
                                discovered.put((left.clone()));
                                discovered.put((right.clone()));
                            }
                            // Accounts for a 180 degree turn
                            right.addStep(MovePath.STEP_TURN_RIGHT);
                            discovered.put(right);
                        }
                        int index = Collections.<MoveOption> binarySearch(
                                possible, next, MoveOption.DISTANCE_COMPARATOR);
                        if (index < 0) {
                            index = -index - 1;
                        }
                        possible.add(index, next);
                    }
                }
            }
        }
        // Final check for illegal and extra weighting for heat
        for (Iterator<MoveOption> i = discovered.values().iterator(); i
                .hasNext();) {
            MoveOption next = i.next();
            next.clipToPossible();
            if (!next.isMoveLegal()) {
                i.remove();
            }
            if (entity.heat > 4) {
                next.movement_threat += bv / 1000
                        * next.getMovementheatBuildup();
                if (entity.heat > 7) {
                    next.movement_threat += bv / 500
                            * next.getMovementheatBuildup();
                }
                if (tsm_offset) {
                    if (entity.heat == 9) {
                        next.movement_threat -= bv / 100
                                * next.getMovementheatBuildup();
                    }
                    if ((entity.heat < 12) && (entity.heat > 9)) {
                        next.movement_threat -= bv / 500
                                * next.getMovementheatBuildup();
                    }
                }
                if (entity.heat > 12) {
                    next.movement_threat += bv / 100
                            * next.getMovementheatBuildup();
                }
            }
            String pilotChecks = SharedUtility.doPSRCheck(next,client);
            if (pilotChecks.length()>0) {
                next.inDanger = true;
            }
        }
        return discovered;
    }

    /**
     * find all moves that get into dest
     */
    public ArrayList<MoveOption> findMoves(Coords dest, Client client) {
        ArrayList<MoveOption> result = new ArrayList<MoveOption>();
        for (int i = 0; i < 6; i++) {
            for (int j = 1; j < 2; j++) {
                MoveOption.Key key = new MoveOption.Key(dest, i, j);
                MoveOption es = null;
                if ((es = getAllMoves(client).get(key)) != null) {
                    result.add(es);
                }
            }
        }
        return result;
    }

    /**
     * Returns an estimate of damage given the range to the target and a
     * modifier to the to-hit number. An approximation of the original to-hit
     * odds is extracted from the damage at that range leaving the damage that
     * is (hopefully) close to the original value. New odds are generated based
     * on the units gunnery skill and the modifier and factored back into the
     * returned damage value.
     */
    public double getModifiedDamage(int arc, int a_range, int modifier) {
        if (a_range > MAX_RANGE) {
            return 0.0;
        }
        double damage = damages[arc][a_range];

        // Use pilots gunnery skill, not the piloting skill...
        int base = entity.getCrew().getGunnery();
        if (entity.getTaserFeedBackRounds() > 0) {
            base += 1;
        }
        int dist_mod = 0;

        // Check range brackets based on defined maximum possible
        // range of weapon
        double range_bracket = long_range / 4.0;

        // Inside minimum range, penalties must be observed
        if (a_range <= MIN_BRACKET) {
            dist_mod += minRangeMods[a_range];
        }

        // Medium range is +2
        if (a_range > range_bracket) {
            dist_mod += 2;
        }

        // Long range is another +2 (i.e. +4)
        if (a_range > 2 * range_bracket) {
            dist_mod += 2;
        }

        // Anything past 3 "range brackets" is extreme range +8,
        // or +4 on top of previous modifiers
        if (a_range > 3 * range_bracket) {
            dist_mod += 4;
        }

        if ((base + dist_mod + modifier > tb.ignore)
                | (base + dist_mod + modifier > 12)) {
            return 0.0;
        }
        if (base + dist_mod + modifier == tb.ignore) {
            damage *= 0.5;
        }

        // Factor out the to-hit odds and re-factor in new odds with the passed
        // modifier
        double old_odds = Compute.oddsAbove(base + dist_mod) / 100;
        double new_odds = Compute.oddsAbove(dist_mod + modifier + base) / 100;

        return new_odds * damage / old_odds;
    }

    /**
     * Determines the expected damage of a weapon attack, based on to-hit, salvo
     * sizes, and so on. Note that this is against a generic target, so
     * defensive coverage, whether trees, ECM, or AMS is not considered. Built
     * in offensive equipment will be counted, such as Artemis IV/V, as will
     * minimum range modifiers and inherent weapon modifiers.
     */
    public static double getExpectedDamage(Entity attacker, Mounted weapon,
            int range, int gunskill) {

        int total_mod = gunskill;
        double[] expectedHitsByRackSize = { 0.0, 1.0, 1.58, 2.0, 2.63, 3.17,
                4.0, 4.49, 4.98, 5.47, 6.31, 7.23, 8.14, 8.59, 9.04, 9.5, 10.1,
                10.8, 11.42, 12.1, 12.7 };

        boolean use_table = false;

        Infantry inf_attacker = new Infantry();
        BattleArmor ba_attacker = new BattleArmor();
        Mounted lnk_guide;

        if (attacker instanceof BattleArmor) {
            ba_attacker = (BattleArmor) attacker;
        }
        if ((attacker instanceof Infantry)
                && !(attacker instanceof BattleArmor)) {
            inf_attacker = (Infantry) attacker;
        }

        // Only infantry can fire into their own hex

        if (!(attacker instanceof Infantry) & (range == 0)) {
            return 0.0;
        }

        double fDamage = 0.0;
        WeaponType wt = (WeaponType) weapon.getType();

        // The "Stop Swarm Attack" really isn't one

        if (wt.getName() == "Stop Swarm Attack") {
            return 0.0;
        }

        // Get the range modifier for the weapon

        if (range <= wt.getMinimumRange()) {
            total_mod = total_mod + 1 + (wt.getMinimumRange() - range);
        } else {
            if (range > wt.getShortRange()) {
                total_mod += 2;
            }
            if (range > wt.getMediumRange()) {
                total_mod += 2;
            }
            if (range > wt.getLongRange()) {
                total_mod += 4;
            }
            if (range > wt.getExtremeRange()) {
                return 0.0;
            }
        }

        // Get the weapon to-hit mod

        total_mod += wt.getToHitModifier();

        // Some weapons use multiple hits, as determined by the missile
        // hits table. This method is used for an approximation, so
        // assume LBX cannon are using cluster rounds, Ultra-cannon
        // are firing double rate, and RACs are fired in quad mode.
        if ((wt.getDamage() == WeaponType.DAMAGE_MISSILE)
                && ((wt.getAmmoType() != AmmoType.T_TBOLT_5) ||
                    (wt.getAmmoType() != AmmoType.T_TBOLT_10) ||
                    (wt.getAmmoType() != AmmoType.T_TBOLT_15) ||
                    (wt.getAmmoType() != AmmoType.T_TBOLT_20))) {
            use_table = true;
        }
        if ((wt.getAmmoType() == AmmoType.T_AC_LBX)
                || (wt.getAmmoType() == AmmoType.T_AC_LBX_THB)) {
            use_table = true;
            total_mod -= 1;
        }

        if ((wt.getAmmoType() == AmmoType.T_AC_ULTRA)
                || (wt.getAmmoType() == AmmoType.T_AC_ULTRA_THB)
                || (wt.getAmmoType() == AmmoType.T_AC_ROTARY)) {
            use_table = true;
        }

        // Kinda cheap, but lets use the missile hits table for Battle armor
        // weapons too

        if (attacker instanceof BattleArmor) {
            if ((wt.getInternalName() != Infantry.SWARM_MEK)
                    & (wt.getInternalName() != Infantry.LEG_ATTACK)
                    & !(wt.hasFlag(WeaponType.F_INFANTRY))) {
                use_table = true;
            }
        }

        if (use_table == true) {
            double fHits = 0.0;
            if (!(attacker instanceof BattleArmor)) {
                if (weapon.getLinked() == null) {
                    return 0.0;
                }
            }
            AmmoType at = null;
            if (weapon.getLinked() != null) {
                at = (AmmoType) weapon.getLinked().getType();
                // Override damage by ammo type for some items
                // Set LRM and MRM damage at 1.0, SRM types at 2.0,
                // and ATMs at 2.4 (2.0 nominal x 1.2 for built-in
                // Artemis)

                fDamage = at.getDamagePerShot();
                if ((wt.getAmmoType() == AmmoType.T_SRM_STREAK)
                        || (wt.getAmmoType() == AmmoType.T_SRM)) {
                    fDamage = 2.0;
                }
                if (wt.getAmmoType() == AmmoType.T_ATM) {
                    fDamage = 2.4;
                }
                if ((wt.getAmmoType() == AmmoType.T_LRM_STREAK)
                        || (wt.getAmmoType() == AmmoType.T_LRM)
                        || (wt.getAmmoType() == AmmoType.T_MRM)) {
                    fDamage = 1.0;
                }
            }
            if ((wt.getRackSize() != 40) && (wt.getRackSize() != 30)) {
                fHits = expectedHitsByRackSize[wt.getRackSize()];
            } else {
                fHits = 2.0f * expectedHitsByRackSize[wt.getRackSize() / 2];
            }
            if ((wt.getAmmoType() == AmmoType.T_SRM_STREAK)
                    || (wt.getAmmoType() == AmmoType.T_LRM_STREAK)) {
                fHits = wt.getRackSize();
            }
            if ((wt.getAmmoType() == AmmoType.T_AC_ULTRA)
                    || (wt.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
                fHits = expectedHitsByRackSize[2];
            }
            if (wt.getAmmoType() == AmmoType.T_AC_ROTARY) {
                fHits = expectedHitsByRackSize[4];
            }
            if ((wt.getAmmoType() == AmmoType.T_AC_LBX)
                    || (wt.getAmmoType() == AmmoType.T_AC_LBX_THB)) {
                fHits = expectedHitsByRackSize[wt.getDamage()];
            }
            if ((wt.getAmmoType() == AmmoType.T_LRM)
                    || (wt.getAmmoType() == AmmoType.T_SRM)) {
                lnk_guide = weapon.getLinkedBy();
                if ((lnk_guide != null)
                        && (lnk_guide.getType() instanceof MiscType)
                        && !lnk_guide.isDestroyed() && !lnk_guide.isMissing()
                        && !lnk_guide.isBreached()
                        && lnk_guide.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    fHits *= 1.2f;
                }
            }

            if (wt.getAmmoType() == AmmoType.T_MRM) {
                lnk_guide = weapon.getLinkedBy();
                if ((lnk_guide != null)
                        && (lnk_guide.getType() instanceof MiscType)
                        && !lnk_guide.isDestroyed() && !lnk_guide.isMissing()
                        && !lnk_guide.isBreached()
                        && lnk_guide.getType().hasFlag(MiscType.F_APOLLO)) {
                    fHits *= .9f;
                }
            }
            // Most Battle Armor units have a weapon per trooper, plus their
            // weapons do odd things when mounting multiples
            if (attacker instanceof BattleArmor) {
                // The number of troopers hitting
                fHits = expectedHitsByRackSize[ba_attacker
                        .getShootingStrength()];
                if (wt.getDamage() == WeaponType.DAMAGE_MISSILE) {
                    fHits *= expectedHitsByRackSize[wt.getRackSize()];
                }
                if (wt.getDamage() != WeaponType.DAMAGE_MISSILE) {
                    if (wt.getDamage() != WeaponType.DAMAGE_VARIABLE) {
                        fDamage = wt.getDamage();
                    } else {
                        fDamage = wt.getRackSize();
                    }
                }
                if (wt.hasFlag(WeaponType.F_MISSILE_HITS)) {
                    fHits *= expectedHitsByRackSize[wt.getRackSize()];
                }
            }

            fDamage *= fHits;

            if ((wt.getAmmoType() == AmmoType.T_AC_ULTRA)
                    || (wt.getAmmoType() == AmmoType.T_AC_ULTRA_THB)
                    || (wt.getAmmoType() == AmmoType.T_AC_ROTARY)) {
                fDamage = fHits * wt.getDamage();
            }

        } else {

            // Direct fire weapons just do a single shot
            // so they don't use the missile hits table

            fDamage = wt.getDamage();

            // Heavy gauss and Thunderbolt LRMs do odd things with range

            if (wt.getAmmoType() == AmmoType.T_GAUSS_HEAVY) {
                fDamage = 25.0;
                if (range > 6) {
                    fDamage = 20.0;
                }
                if (range > 13) {
                    fDamage = 10.0;
                }
            }
            if ((wt.getAmmoType() == AmmoType.T_TBOLT_5) ||
                (wt.getAmmoType() == AmmoType.T_TBOLT_10) ||
                (wt.getAmmoType() == AmmoType.T_TBOLT_15) ||
                (wt.getAmmoType() == AmmoType.T_TBOLT_20)) {
                if (range <= wt.getMinimumRange()) {
                    fDamage *= 0.5;
                }
            }

            // Infantry follow some special rules, but do fixed amounts of
            // damage
            if (attacker instanceof Infantry) {

                if (wt.hasFlag(WeaponType.F_INFANTRY)) {
                    // Weapons fielded by conventional infantry and light BA
                    // Field guns should be handled under the normal weapons
                    // section
                    /*
                     * fDamage = 0.6f * inf_attacker.getDamage(inf_attacker.
                     * getShootingStrength());
                     */
                    // TODO: fix me
                    fDamage = 1;
                    if (attacker instanceof BattleArmor) {
                        /*
                         * fDamage = inf_attacker.getDamage(ba_attacker.
                         * getShootingStrength());
                         */
                        // TODO: fix me
                        fDamage = 1;
                    }
                }

                if (wt.getInternalName() == Infantry.LEG_ATTACK) {
                    // Odds change based on number of troopers. Make an
                    // estimate.
                    if (!(attacker instanceof BattleArmor)) {
                        total_mod = 4 + 2 * (inf_attacker.getOInternal(0) - inf_attacker
                                .getShootingStrength()) / 5;
                    } else {
                        total_mod = 8 - ba_attacker.getShootingStrength();
                    }
                    fDamage = 10.0; // Damage is actually 5 plus chance at crits
                }

                if (wt.getInternalName() == Infantry.SWARM_MEK) {
                    // Odds change based on number of troopers. Make an
                    // estimate.
                    // Damage is same as conventional weapons, but with chance
                    // for crits
                    if (!(attacker instanceof BattleArmor)) {
                        total_mod = 7 + 2 * (inf_attacker.getOInternal(0) - inf_attacker
                                .getShootingStrength()) / 5;
                        /*
                         * fDamage = 1.5 * inf_attacker.getDamage(inf_attacker
                         * .getShootingStrength());
                         */
                        // TODO: Fix me
                        fDamage = 5;
                    } else {
                        total_mod = 11 - ba_attacker.getShootingStrength();
                        fDamage = 5.0 * ba_attacker.getShootingStrength();
                    }
                }

            }
        }
        double fChance = Compute.oddsAbove(total_mod) / 100;
        fDamage *= fChance;
        return fDamage;
    }

    public static int getFiringAngle(final Coords dest, int dest_facing,
            final Coords src) {
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
        if ((fa >= 300) || (fa <= 60)) {
            return ToHitData.SIDE_FRONT;
        }
        if ((fa >= 60) && (fa <= 120)) {
            return ToHitData.SIDE_RIGHT;
        }
        if ((fa >= 240) && (fa <= 300)) {
            return ToHitData.SIDE_LEFT;
        }
        return ToHitData.SIDE_REAR;
    }

    public static int firingArcToHitArc(int arc) {
        switch (arc) {
            case Compute.ARC_FORWARD:
                return ToHitData.SIDE_FRONT;
            case Compute.ARC_LEFTARM:
                return ToHitData.SIDE_LEFT;
            case Compute.ARC_RIGHTARM:
                return ToHitData.SIDE_RIGHT;
            case Compute.ARC_REAR:
                return ToHitData.SIDE_REAR;
            case Compute.ARC_LEFTSIDE:
                return ToHitData.SIDE_LEFT;
            case Compute.ARC_RIGHTSIDE:
                return ToHitData.SIDE_RIGHT;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if ((obj instanceof Entity) || (obj instanceof CEntity)) {
            return obj.hashCode() == hashCode();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return entity.getId();
    }

    public TestBot getTb() {
        return tb;
    }

}
