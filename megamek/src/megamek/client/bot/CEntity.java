/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
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

import megamek.client.Client;
import megamek.client.ui.SharedUtility;
import megamek.common.*;
import megamek.common.MovePath.MoveStepType;
import megamek.common.enums.MPBoosters;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.gaussrifles.ISImpHGaussRifle;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.common.weapons.lasers.VariableSpeedPulseLaserWeapon;
import megamek.common.weapons.ppc.ISSnubNosePPC;
import org.apache.logging.log4j.LogManager;

import java.util.*;

public class CEntity {
    static class Table extends HashMap<Integer, CEntity> {
        private static final long serialVersionUID = 6437109733397107056L;
        private TestBot tb;

        public Table(TestBot tb) {
            this.tb = tb;
        }

        public void put(CEntity es) {
            this.put(es.getKey(), es);
        }

        public CEntity get(Entity es) {
            CEntity result;
            if ((result = super.get(es.getId())) == null) {
                result = new CEntity(es, tb);
                this.put(result);
            }
            return result;
        }

        public CEntity get(int id) {
            return get(Integer.valueOf(id));
        }
    }

    // Armor values based on [ToHitData.SIDE_XXX][location static int]
    // Locations are as defined in static variables in various unit type classes
    // Values are the odds (out of 1.00) of rolling that location on the to-hit
    // table

    // Tank armor is either the side hit or the turret
    static final double[][] TANK_ARMOR = { { 0, 1.0, 0, 0, 0 },
            { 0, 0, 0, 0, 1.0 }, { 0, 0, 0, 1.0, 0 }, { 0, 0, 1.0, 0, 0 } };
    static final double[][] TANK_WT_ARMOR = {
            { 0, 31.0 / 36, 0, 0, 0, 5.0 / 36 },
            { 0, 0, 0, 0, 31.0 / 36, 5.0 / 36 },
            { 0, 0, 0, 31.0 / 36, 0, 5.0 / 36 },
            { 0, 0, 31.0 / 36, 0, 0, 5.0 / 36 } };

    // Infantry don't have a facing. In fact, they don't have armor...
    static final double[][] INFANTRY_ARMOR = { { 1.0 }, { 1.0 }, { 1.0 },
            { 1.0 } };

    // Battle armor units have multiple suits
    static final double[][] ISBA_ARMOR = { { 0.25, 0.25, 0.25, 0.25 },
            { 0.25, 0.25, 0.25, 0.25 }, { 0.25, 0.25, 0.25, 0.25 },
            { 0.25, 0.25, 0.25, 0.25 } };
    static final double[][] CLBA_ARMOR = { { 0.2, 0.2, 0.2, 0.2, 0.2 },
            { 0.2, 0.2, 0.2, 0.2, 0.2 }, { 0.2, 0.2, 0.2, 0.2, 0.2 },
            { 0.2, 0.2, 0.2, 0.2, 0.2 } };
    static final double[][] PROTOMECH_ARMOR = {
            { 1.0 / 31, 16.0 / 31, 3.0 / 31, 3.0 / 31, 8.0 / 31 },
            { 1.0 / 31, 16.0 / 31, 3.0 / 31, 3.0 / 31, 8.0 / 31 },
            { 1.0 / 31, 16.0 / 31, 3.0 / 31, 3.0 / 31, 8.0 / 31 },
            { 1.0 / 31, 16.0 / 31, 3.0 / 31, 3.0 / 31, 8.0 / 31 } };
    static final double[][] PROTOMECH_MG_ARMOR = {
            { 1.0 / 32, 16.0 / 32, 3.0 / 32, 3.0 / 32, 8.0 / 32, 1.0 / 32 },
            { 1.0 / 31, 16.0 / 32, 3.0 / 32, 3.0 / 32, 8.0 / 32, 1.0 / 32 },
            { 1.0 / 31, 16.0 / 32, 3.0 / 32, 3.0 / 32, 8.0 / 32, 1.0 / 32 },
            { 1.0 / 31, 16.0 / 32, 3.0 / 32, 3.0 / 32, 8.0 / 32, 1.0 / 32 } };
    static final double[][] MECH_ARMOR = {
            { 1.0 / 36, 7.0 / 36, 6.0 / 36, 6.0 / 36, 4.0 / 36, 4.0 / 36,
                    4.0 / 36, 4.0 / 36 },
            { 1.0 / 36, 7.0 / 36, 6.0 / 36, 6.0 / 36, 4.0 / 36, 4.0 / 36,
                    4.0 / 36, 4.0 / 36 },
            { 1.0 / 36, 6.0 / 36, 4.0 / 36, 7.0 / 36, 2.0 / 36, 6.0 / 36,
                    2.0 / 36, 8.0 / 36 },
            { 1.0 / 36, 6.0 / 36, 7.0 / 36, 4.0 / 36, 6.0 / 36, 2.0 / 36,
                    8.0 / 36, 2.0 / 36 } };
    static final double[][] GUN_EMPLACEMENT_ARMOR = { { 1.0 / 4, 0, 0, 0 },
            { 1.0 / 4, 0, 0, 0 }, { 1.0 / 4, 0, 0, 0 }, { 1.0 / 4, 0, 0, 0 } };
    static final double[][] GUN_EMPLACEMENT_TURRET_ARMOR = {
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
    public static final int LAST_ARC = 5;

    public static final int TT = 4;

    public static final int LEFT_LEG = 0;
    public static final int RIGHT_LEG = 1;

    // Weighted averages of the cluster hits table. Note the actual
    // table does skip entries from 31 through 39.
    private static final double[] hits_by_racksize = { 0.0, 1.0, 1.58, 2.0,
            2.63, 3.17, 4.0, 4.49, 4.98, 5.47, 6.31, 7.23, 8.14, 8.59, 9.04,
            9.5, 10.1, 10.8, 11.42, 12.1, 12.7, 13.6, 14.4, 15.7, 16.3, 16.6,
            17.4, 17.6, 17.9, 18.7, 19.0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 25.4 };

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

    // Current heat status of the unit
    int overheat = OVERHEAT_NONE;
    // Weapons heat for ideal range bracket
    int heat_at_range = 0;
    // Heat for each range bracket
    int[] heat_estimates = new int[4];

    // Index of the ideal engagement range from range_damages
    int range = RANGE_ALL;
    int long_range = 0;
    // Damage at short/medium/long/all(?) ranges
    double[] range_damages = new double[4];
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
    double[][] damages = new double[6][MAX_RANGE];

    // the battle value of the unit
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
        entity = tb.getGame().getEntity(entity.getId()); // fresh entity
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
        entity = tb.getGame().getEntity(entity.getId());
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

    /**
     * Simplifies the many game stats into something that can be quickly
     * calculated into a units combat effectiveness. The concept is similar to
     * creating BattleForce stats.
     */

    public void characterize() {
        entity = tb.getGame().getEntity(entity.getId());
        current = new MoveOption(tb.getGame(), this);
        bv = entity.calculateBattleValue();

        // Make a guess as to whether MASC should be turned on or off
        // TODO : Link this to a Bot configuration file
        runMP = entity.getRunMP();
        if (entity instanceof Mech) {
            MPBoosters mpBoosters = entity.getMPBoosters();
            if (!mpBoosters.isNone()) {
                // do a check for each system
                masc_threat = false;
                if (mpBoosters.hasMASC()) {
                    if (entity.getMASCTarget() <= (5 + Compute.randomInt(6))) {
                        masc_threat = false;
                    } else {
                        masc_threat = true;
                        runMP = entity.getRunMPwithoutMASC();
                    }
                }

                if ((masc_threat == false) && mpBoosters.hasSupercharger()) {
                    //we passed masc, but test for supercharger
                    if (entity.getSuperchargerTarget() <= (5 + Compute.randomInt(6))) {
                        masc_threat = false;
                    } else {
                        masc_threat = true;
                        runMP = entity.getRunMPwithoutMASC();
                    }
                }
            } else {
                // If this is a Mech equipped with TSM, push for the sweet
                // spot at 9 heat
                if (((Mech) entity).hasTSM(false)) {
                    tsm_offset = true;
                }
            }
        }
        jumpMP = entity.getJumpMP();

        overall_armor_percent = entity.getArmorRemainingPercent();
        base_psr_odds = Compute.oddsAbove(entity.getBasePilotingRoll()
                .getValue(), entity.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)) / 100;

        // Heat characterisation - how badly will a Mech overheat this round
        int heat_capacity = entity.getHeatCapacity();
        int heat = entity.heat;
        if (entity instanceof Mech) {
            // Include heat from active stealth armor systems
            if (entity.isStealthActive() || entity.isNullSigActive()
                    || entity.isVoidSigActive()) {
                heat += 10;
            }

            if (entity.isChameleonShieldActive()) {
                heat += 6;
            }

            // Infernos no longer track heat over multiple rounds

            // Include heat from engine hits
            heat += entity.getEngineCritHeat();

            // Include heat for standing in a fire
            if (entity.getPosition() != null) {
                if (tb.getGame().getBoard().getHex(entity.getPosition()) != null) {
                    if (tb.getGame().getBoard().getHex(entity.getPosition())
                            .containsTerrain(Terrains.FIRE)
                            && (tb.getGame().getBoard().getHex(entity.getPosition())
                                    .getFireTurn() > 0)) {
                        heat += 5;
                    }
                }
            }

            // Include heat from ambient temperature
            heat += tb.getGame().getPlanetaryConditions().getTemperatureDifference(
                    50, -30);
        }

        // Offensive characterisation - damage potentials

        ArrayList<Mounted> ammo_list = entity.getAmmo();

        double[][] overall_damage = new double[6][MAX_RANGE];
        double[] cur_weapon_damage = new double[MAX_RANGE];

        ArrayList<Integer> cur_weapon_arcs = new ArrayList<>();
        int[][] overall_heat = new int[6][MAX_RANGE];
        int cur_weapon_heat, weapons_count = 0;
        int cur_weapon_arc;

        // Mainly for BA and conventional infantry support
        int number_of_shooters = 1;

        boolean[] ammo_ranges = { false, false, false };

        int gunnery = entity.getCrew().getGunnery();
        if (entity.getTaserFeedBackRounds() > 0) {
            gunnery += 1;
        }

        // Most units, including BA, are equipped with conventional weapons
        if (!entity.isConventionalInfantry()) {

            if (entity instanceof BattleArmor) {
                number_of_shooters = ((BattleArmor) entity)
                        .getShootingStrength();
            }

            // Physical attacks - punch and/or kicking for Mechs
            if (entity instanceof Mech) {

                // Base damage, adjusted for odds of hitting
                cur_weapon_damage[1] = (tsm_offset ? 1.0 : 0.5)
                        * (entity.getWeight() / 10)
                        * (Compute.oddsAbove(entity.getCrew().getPiloting(),
                                             entity.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING)) / 100);

                // Either a kick or double-punch to the front
                overall_damage[Compute.ARC_FORWARD][1] = 2.0 * cur_weapon_damage[1];

                // If the Mech can flip arms, don't consider arm arcs
                if (!((Mech) entity).canFlipArms()) {
                    overall_damage[Compute.ARC_LEFTARM][1] = cur_weapon_damage[1];
                    overall_damage[Compute.ARC_RIGHTARM][1] = cur_weapon_damage[1];
                }
            }

            // Physical attacks - vibroclaws for BA
            if (entity instanceof BattleArmor) {

                overall_damage[Compute.ARC_360][0] = (hits_by_racksize[number_of_shooters]
                        * ((BattleArmor) entity).getVibroClaws()
                        * Compute.oddsAbove(gunnery,
                                            entity.hasAbility(OptionsConstants.PILOT_APTITUDE_GUNNERY))) / 100.0;
            }

            // Iterate over each weapon, calculating average damage
            // for each weapon, over each hex of range
            for (Mounted cur_weapon : entity.getWeaponList()) {

                WeaponType weapon = (WeaponType) cur_weapon.getType();
                int cur_weapon_id = entity.getEquipmentNum(cur_weapon);

                // Don't count weapons that are destroyed/jammed or are out
                // of ammo
                if (!cur_weapon.canFire()
                        || cur_weapon.isJammed()
                        || ((cur_weapon.getLinked() == null) && (weapon
                                .getAmmoType() != AmmoType.T_NA))) {
                    continue;
                }

                // Anti-Mech attacks are difficult to set up and have bad
                // odds most of the time. This means they have little
                // strategic impact.
                if (weapon.getInternalName().equals(Infantry.SWARM_MEK)
                        || weapon.getInternalName().equals(Infantry.LEG_ATTACK)
                        || weapon.getInternalName().equals(Infantry.SWARM_MEK)
                        || weapon.getInternalName().equals(Infantry.STOP_SWARM)) {
                    continue;
                }

                // Get basic stats for the current weapon
                cur_weapon_arc = entity.getWeaponArc(cur_weapon_id);
                cur_weapon_arcs = getWeaponArcs(cur_weapon_arc,
                        entity.isSecondaryArcWeapon(cur_weapon_id));
                cur_weapon_heat = cur_weapon.getCurrentHeat();

                // Get the damage the weapon will do at each range bracket.
                // This includes average cluster hits but not to-hit averages.
                ammo_ranges = getAmmoRanges(weapon, ammo_list);
                cur_weapon_damage = getRawDamage(cur_weapon, ammo_ranges);

                boolean aptGunnery = entity.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING);

                // Apply to-hit modifiers to the damage values
                cur_weapon_damage = getExpectedDamage(weapon, gunnery,
                                                      cur_weapon_damage, ammo_ranges, aptGunnery);

                // If the heat generated by the weapon is a significant
                // portion of the Mechs remaining heatsink capacity, it needs
                // to do some significant damage before being counted. Under
                // most circumstances this will only happen if the Mech is
                // seriously overheating.

                if (entity instanceof Mech) {

                    int overheat = (cur_weapon_heat + heat)
                            - (heat_capacity + (tsm_offset ? 9 : 4));
                    if (overheat > 0) {

                        for (int i = 0; i < cur_weapon_damage.length; i++) {
                            if (cur_weapon_damage[i] < overheat) {
                                cur_weapon_damage[i] = 0.0;
                            }
                        }

                    }

                }

                // Only BA can fire into their own hex
                if (!(entity instanceof BattleArmor)) {
                    cur_weapon_damage[0] = 0.0;
                }

                // Increment the total weapons count if the weapon was
                // allocated to the forward arc. No point counting weapons
                // that don't get used much.
                if (cur_weapon_arcs.contains(Compute.ARC_FORWARD)) {
                    weapons_count++;
                }

                int weapon_min_range = weapon.getMinimumRange();

                // Add the final damage and heat values into the overall damage
                // array for the appropriate arcs
                for (int i = 0; i < cur_weapon_damage.length; i++) {

                    // Skip any range where no damage is done
                    if (cur_weapon_damage[i] == 0.0) {
                        continue;
                    }

                    // Adjust weapon damage by the number of shooters. This
                    // normally applies only to Battle Armor.
                    if (number_of_shooters > 1) {
                        cur_weapon_damage[i] *= hits_by_racksize[number_of_shooters];
                    }

                    for (int firing_arc : cur_weapon_arcs) {

                        // Some error control to catch non-standard arcs
                        if (firing_arc > LAST_ARC) {
                            continue;
                        }

                        overall_damage[firing_arc][i] += cur_weapon_damage[i];

                        // Only Mechs need track heat. The heat values will
                        // be used to derate damage at each hex of range.
                        if ((entity instanceof Mech)
                                && (overall_damage[firing_arc][i] > 0)) {
                            overall_heat[firing_arc][i] += cur_weapon_heat;
                        }

                        // Track minimum range modifiers for weapons that
                        // fire into the forward arc
                        if ((firing_arc == Compute.ARC_FORWARD)
                                && (i <= weapon_min_range)
                                && (i < minRangeMods.length)) {
                            minRangeMods[i] += (1 + weapon_min_range) - i;
                        }

                    }

                }

                // Next weapon
            }

        } else {

            // Conventional infantry use infantry weapons, which require
            // special handling

            number_of_shooters = ((Infantry) entity).getShootingStrength();

            cur_weapon_damage = getExpectedDamage((Infantry) entity, gunnery);

            // Add the current damage values into the overall damage array for
            // the 360 degree arc. Adjust for number of troopers hitting.
            for (int i = 0; i < cur_weapon_damage.length; i++) {
                overall_damage[Compute.ARC_360][i] += cur_weapon_damage[i];
                overall_damage[Compute.ARC_360][i] *= hits_by_racksize[number_of_shooters];
            }

            // Check for field guns
            for (Mounted cur_weapon : entity.getWeaponList()) {

                WeaponType weapon = (WeaponType) cur_weapon.getType();

                if (cur_weapon.getLocation() != Infantry.LOC_FIELD_GUNS) {
                    continue;
                }

                // Don't count weapons that are destroyed/jammed or are out
                // of ammo
                if (!cur_weapon.canFire()
                        || cur_weapon.isJammed()
                        || ((cur_weapon.getLinked() == null) && (weapon
                                .getAmmoType() != AmmoType.T_NA))) {
                    continue;
                }

                boolean aptGunnery = entity.hasAbility(OptionsConstants.PILOT_APTITUDE_PILOTING);

                cur_weapon_damage = CEntity.getRawDamage(cur_weapon, null);
                cur_weapon_damage = CEntity.getExpectedDamage(weapon, gunnery,
                                                              cur_weapon_damage, ammo_ranges, aptGunnery);

                // Push the field gun damages into the overall damage array
                for (int i = 0; i < cur_weapon_damage.length; i++) {

                    // Skip 0-damage entries
                    if (cur_weapon_damage[i] == 0) {
                        continue;
                    }

                    overall_damage[Compute.ARC_360][i] += cur_weapon_damage[i];
                }
            }

            // Copy the total damage into each arc
            for (int cur_arc = FIRST_ARC + 1; cur_arc <= LAST_ARC; cur_arc++) {
                overall_damage[cur_arc] = overall_damage[Compute.ARC_360];
            }

        }

        // Push the accumulated damages into the CEntity array
        for (int cur_arc = FIRST_ARC; cur_arc <= LAST_ARC; cur_arc++) {

            // For each hex
            for (int cur_range = 0; cur_range < MAX_RANGE; cur_range++) {

                // If no damage, then skip the calculations
                if (overall_damage[cur_arc][cur_range] == 0.0) {
                    continue;
                }

                // If the weapons heat and the miscellaneous heat is more than
                // the Mech can sink, lower the damage proportionally e.g.
                // (sink capacity + buffer) / (weapon heat + base heat)
                if ((entity instanceof Mech)
                        && ((overall_heat[cur_arc][cur_range] + heat) > (heat_capacity
                                + (tsm_offset ? 9 : 4)))) {
                    overall_damage[cur_arc][cur_range] *= (heat_capacity + (tsm_offset ? 9
                            : 4));
                    overall_damage[cur_arc][cur_range] /= (overall_heat[cur_arc][cur_range] + heat);
                }

                damages[cur_arc][cur_range] = overall_damage[cur_arc][cur_range];

                // Next hex
            }

            // Next arc
        }

        // Average out the minimum range modifiers
        for (int cur_range = 1; cur_range < minRangeMods.length; cur_range++) {
            if (weapons_count > 0) {
                minRangeMods[cur_range] = (int) Math
                        .round(((double) minRangeMods[cur_range])
                                / (double) weapons_count);
            }
        }

        // Change the damage from a per-hex array to a single "weapon"
        // with short/medium/long ranges. For now, just do the forward
        // arc.
        computeRange(Compute.ARC_FORWARD, overall_heat);

        // Overheating will be based on the optimum firing range
        heat = (heat + heat_at_range) - heat_capacity;

        if (heat <= 4) {
            overheat = OVERHEAT_NONE;
        }
        if (heat > 4) {
            overheat = OVERHEAT_LOW;
        }
        if ((heat > 9) & !tsm_offset) {
            overheat = OVERHEAT_HIGH;
        }
        if ((heat > 12) & tsm_offset) {
            overheat = OVERHEAT_HIGH;
        }

        // Defensive characterization - protection values

        double max = 1.0;

        // Initialize armor values
        double[][] armor = MECH_ARMOR;

        if (entity instanceof Tank) {
            if (((Tank) entity).hasNoTurret()) {
                armor = TANK_ARMOR;
            } else {
                armor = TANK_WT_ARMOR;
            }
        }

        if (entity instanceof Infantry) {
            if (entity.isConventionalInfantry()) {
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
            armor = ((GunEmplacement) entity).isTurret() ? GUN_EMPLACEMENT_TURRET_ARMOR
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
                    .getShootingStrength() : 1.0;
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
     * Weapons mounted in one arc can be applied over several depending on the
     * type of unit due to torso or turret twisting
     *
     * @param mounted_arc
     *            arc for weapon
     * @param is_secondary
     *            true if weapon can fire into another arc
     * @return ArrayList of Compute.ARC_XXX integers
     */
    private ArrayList<Integer> getWeaponArcs(int mounted_arc,
            boolean is_secondary) {

        ArrayList<Integer> arc_list = new ArrayList<>(1);

        // Weapons which can fire in any direction
        if ((mounted_arc == Compute.ARC_360)
                || (mounted_arc == Compute.ARC_MAINGUN)
                || (mounted_arc == Compute.ARC_TURRET)) {
            for (int i = FIRST_ARC; i <= LAST_ARC; i++) {
                arc_list.add(i);
            }
        } else {

            arc_list.add(mounted_arc);

            // If the weapon can be applied to a secondary arc, add it there
            // as well
            if (is_secondary) {

                if (mounted_arc == Compute.ARC_FORWARD) {

                    // Mech torso twist
                    arc_list.add(Compute.ARC_LEFTARM);
                    arc_list.add(Compute.ARC_RIGHTARM);

                    // Vehicle turrets
                    if (entity instanceof Tank) {
                        arc_list.add(Compute.ARC_360);
                    }

                }

                // Left arm fires into the front and left side,
                // right arm to the front and right. If the arms
                // can flip, they both go to the rear as well.
                if ((mounted_arc == Compute.ARC_LEFTARM)
                        || (mounted_arc == Compute.ARC_RIGHTARM)) {

                    arc_list.add(Compute.ARC_FORWARD);
                    if ((entity instanceof Mech) && entity.canFlipArms()) {
                        arc_list.add(Compute.ARC_REAR);
                    }

                }

            }

        }

        return arc_list;
    }

    /**
     * Fills the CEntity damage and range brackets, plus the preferred range and
     * the heat for that range.
     *
     * @param arc
     *            Compute.ARC index for main damage array
     * @param est_heat
     *            Estimated heat of unit for each hex of range
     */
    private void computeRange(int arc, int[][] est_heat) {

        double[] damage_by_bracket = { 0.0, 0.0, 0.0, 0.0 };
        double[] heat_by_bracket = { 0, 0, 0, 0 };

        long_range = MAX_RANGE - 1;
        int bracket_start, bracket_end;

        // Get the longest range and use it to calculate an average range
        // bracket. Extreme range is used when estimating damage but only
        // the official "long" range is kept.
        // TODO: add some math to account for non-linear brackets
        while ((damages[arc][long_range] == 0.0) && (long_range >= 4)) {
            long_range--;
        }

        rd_bracket = long_range / 4;
        long_range = 3 * rd_bracket;
        bracket_start = 0;
        bracket_end = rd_bracket;

        // For each range bracket
        for (int cur_bracket = RANGE_SHORT; cur_bracket <= RANGE_LONG; cur_bracket++) {

            // Get the start and end ranges. Allocate any leftover hexes to
            // to the end of the long range bracket.
            switch (cur_bracket) {
                case RANGE_SHORT:
                    bracket_start = (entity instanceof Infantry ? 0 : 1);
                    bracket_end = rd_bracket;
                    break;
                case RANGE_MEDIUM:
                    bracket_start = rd_bracket + 1;
                    bracket_end = 2 * rd_bracket;
                    break;
                case RANGE_LONG:
                    bracket_start = (2 * rd_bracket) + 1;
                    bracket_end = Math.min(long_range, MAX_RANGE - 1);
                    break;
            }

            // For each hex in the current bracket
            for (int cur_range = bracket_start; cur_range <= bracket_end; cur_range++) {

                // Add up the damage and heat estimates
                damage_by_bracket[cur_bracket] += damages[arc][cur_range];
                damage_by_bracket[RANGE_ALL] += damages[arc][cur_range];

                heat_by_bracket[cur_bracket] += est_heat[arc][cur_range];
                heat_by_bracket[RANGE_ALL] += est_heat[arc][cur_range];

                // Next hex
            }

            // Next range bracket
        }

        // Average out the damage and heat value totals over the number of
        // hexes for the range bracket
        for (int cur_range = RANGE_SHORT; cur_range <= RANGE_LONG; cur_range++) {
            if ((damages[arc][0] != 0.0) && (cur_range == RANGE_SHORT)) {
                damage_by_bracket[cur_range] /= (rd_bracket + 1);
                heat_by_bracket[cur_range] /= (rd_bracket + 1);
            } else {
                damage_by_bracket[cur_range] /= rd_bracket;
                heat_by_bracket[cur_range] /= rd_bracket;
            }

        }
        damage_by_bracket[RANGE_ALL] /= long_range;
        heat_by_bracket[RANGE_ALL] /= long_range;

        // Push the average damage and heat values to the CEntity arrays
        range_damages = damage_by_bracket;
        for (int cur_bracket = RANGE_SHORT; cur_bracket <= RANGE_ALL; cur_bracket++) {
            heat_estimates[cur_bracket] = (int) heat_by_bracket[cur_bracket];
        }

        // Pick the best range, which will likely be "short" due to short
        // range secondary weapons and better to-hit modifiers
        range = RANGE_SHORT;
        for (int cur_bracket = RANGE_SHORT; cur_bracket <= RANGE_ALL; cur_bracket++) {
            if (range_damages[cur_bracket] > range_damages[range]) {
                range = cur_bracket;
            }
        }

        // Get the weapons heat for the optimum range
        heat_at_range = heat_estimates[range];

    }

    /**
     * Fills range damage values for short, medium, long, and all, plus sets the
     * range bracket for the entity, which is 1/3 of long range or 1/4 extreme
     * range. The arc argument follows Compute.ARC_XXX format.
     */
    public void computeRange(int arc, boolean aptGunnery) {

        double[] optimizer = { 0, 0, 0, 0 };

        Arrays.fill(range_damages, 0);

        // Create short, medium, and long range values for each arc

        rd_bracket = long_range / 4;

        for (int range_walk = (entity instanceof Infantry ? 0 : 1); range_walk < long_range; range_walk++) {
            if (range_walk <= rd_bracket) {
                optimizer[RANGE_SHORT] += damages[arc][range_walk];
                range_damages[RANGE_SHORT] += damages[arc][range_walk];
            }
            if ((range_walk > rd_bracket) & (range_walk <= (2 * rd_bracket))) {
                optimizer[RANGE_MEDIUM] += getModifiedDamage(arc, range_walk, -2, aptGunnery);
                range_damages[RANGE_MEDIUM] += damages[arc][range_walk];
            }
            if ((range_walk > (2 * rd_bracket)) & (range_walk <= (3 * rd_bracket))) {
                optimizer[RANGE_LONG] += getModifiedDamage(arc, range_walk, -4, aptGunnery);
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
        if (entity.isConventionalInfantry()) {
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
        if ((t2 + expected_damage[arc]) > armor_health[arc]) {

            // expected_damage[] is set on the fly; it tracks damage
            // the entity expects to take
            if ((((t2 + expected_damage[0] + expected_damage[1]
                    + expected_damage[2] + expected_damage[3]) > (3 * (avg_armor + avg_iarmor))) || (entity
                    .isProne() && (base_psr_odds < .1) && !entity.isImmobile()))) { // If
                                                                                    // I
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
        return entity.getId();
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
        // New array of movement options
        ArrayList<MoveOption> possible = new ArrayList<>();
        MoveOption.Table discovered = new MoveOption.Table();

        // Add the seed for jumping if allowed
        if (entity.getJumpMPWithTerrain() > 0) {
            possible.add((base.clone()).addStep(MoveStepType.START_JUMP));
        }

        possible.add(base); // Add the base movement option to the arraylist of possibles
        discovered.put(base); // Add the base movement option to the movement option table

        while (!possible.isEmpty()) {
            // Keep going until the arraylist is empty (why?)

            // Get the first movement option, while stripping it from the arraylist
            MoveOption min = possible.remove(0);
            Iterator<MovePath> adjacent = min.getNextMoves(true, true).iterator();
            while (adjacent.hasNext()) {
                MoveOption next = (MoveOption) adjacent.next();
                if ((entity instanceof Mech) && (((Mech) entity).countBadLegs() >= 1)
                        && (entity.isLocationBad(Mech.LOC_LARM) && entity.isLocationBad(Mech.LOC_RARM))) {
                    MoveOption eject = next.clone();
                    eject.addStep(MoveStepType.EJECT);
                    discovered.put(eject.clone());
                }
                if (next.changeToPhysical() && next.isMoveLegal()) {
                    discovered.put(next);
                } else if (next.isMoveLegal()) {
                    // relax edges;
                    if ((discovered.get(next) == null)
                            || (next.getDistUtility() < discovered.get(next).getDistUtility())) {
                        discovered.put(next);
                        if (next.isJumping()) {
                            MoveOption left = next.clone();
                            MoveOption right = next.clone();
                            // Think about skipping this for infantry, which have no facing
                            for (int turn = 0; turn < 2; turn++) {
                                left.addStep(MoveStepType.TURN_LEFT);
                                right.addStep(MoveStepType.TURN_RIGHT);
                                discovered.put((left.clone()));
                                discovered.put((right.clone()));
                            }
                            // Accounts for a 180 degree turn
                            right.addStep(MoveStepType.TURN_RIGHT);
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
        // Final check for illegal and extra weighting for heat
        for (Iterator<MoveOption> i = discovered.values().iterator(); i
                .hasNext();) {
            MoveOption next = i.next();
            next.clipToPossible();
            if (!next.isMoveLegal()) {
                i.remove();
            }
            if (entity.heat > 4) {
                next.movement_threat += (bv / 1000)
                        * next.getMovementheatBuildup();
                if (entity.heat > 7) {
                    next.movement_threat += (bv / 500)
                            * next.getMovementheatBuildup();
                }
                if (tsm_offset) {
                    if (entity.heat == 9) {
                        next.movement_threat -= (bv / 100)
                                * next.getMovementheatBuildup();
                    }
                    if ((entity.heat < 12) && (entity.heat > 9)) {
                        next.movement_threat -= (bv / 500)
                                * next.getMovementheatBuildup();
                    }
                }
                if (entity.heat > 12) {
                    next.movement_threat += (bv / 100)
                            * next.getMovementheatBuildup();
                }
            }
            String pilotChecks = SharedUtility.doPSRCheck(next);
            if (!pilotChecks.isBlank()) {
                next.inDanger = true;
            }
        }
        return discovered;
    }

    /**
     * find all moves that get into dest
     */
    public ArrayList<MoveOption> findMoves(Coords dest, Client client) {
        ArrayList<MoveOption> result = new ArrayList<>();
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
    public double getModifiedDamage(int arc, int a_range, int modifier, boolean aptGunnery) {
        if (a_range >= MAX_RANGE) {
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
        if (a_range > (2 * range_bracket)) {
            dist_mod += 2;
        }

        // Anything past 3 "range brackets" is extreme range +8,
        // or +4 on top of previous modifiers
        if (a_range > (3 * range_bracket)) {
            dist_mod += 4;
        }

        if (((base + dist_mod + modifier) > tb.ignore)
                | ((base + dist_mod + modifier) > 12)) {
            return 0.0;
        }
        if ((base + dist_mod + modifier) == tb.ignore) {
            damage *= 0.5;
        }

        // Factor out the to-hit odds and re-factor in new odds with the passed
        // modifier
        double old_odds = Compute.oddsAbove(base + dist_mod, aptGunnery) / 100;
        double new_odds = Compute.oddsAbove(dist_mod + modifier + base, aptGunnery) / 100;

        return (new_odds * damage) / old_odds;
    }

    /**
     * Generates an approximation of damage done at each of the major range
     * brackets, including minimum range. Also handles weapons with damage and
     * range that varies by ammo type. Adds line to debug log for any variable
     * damage weapons that aren't handled.
     *
     * @param weapon
     * @param applicable_ranges
     *            only counts for MMLs and ATMs
     * @return 4-element array with min/short/medium/long range damage
     */

    private static double[] getRawDamage(Mounted weapon,
            boolean[] applicable_ranges) {
        WeaponType wt = (WeaponType) weapon.getType();
        Mounted linked_guidance;

        double damage_value = wt.getDamage();
        double[] raw_damage_array = { damage_value, damage_value, damage_value,
                damage_value };

        int rack_size = 1;

        boolean use_table = false;

        // Some weapons use the cluster hits table:
        // - non-Thunderbolt missiles
        // - LBX cannons are assumed to have cluster ammo
        // - Ultra cannons as two-shot, rotary cannons as 4-shot
        // - HAGs

        if (wt.hasFlag(WeaponType.F_MISSILE)
                && ((wt.getAmmoType() != AmmoType.T_TBOLT_5)
                        || (wt.getAmmoType() != AmmoType.T_TBOLT_10)
                        || (wt.getAmmoType() != AmmoType.T_TBOLT_15) || (wt
                        .getAmmoType() != AmmoType.T_TBOLT_20))) {
            use_table = true;
            rack_size = wt.getRackSize();
        }

        if ((wt.getAmmoType() == AmmoType.T_AC_LBX)
                || (wt.getAmmoType() == AmmoType.T_AC_LBX_THB)
                || (wt.getAmmoType() == AmmoType.T_HAG)) {
            use_table = true;
            rack_size = wt.getRackSize();
        }

        if ((wt.getAmmoType() == AmmoType.T_AC_ULTRA)
                || (wt.getAmmoType() == AmmoType.T_AC_ULTRA_THB)) {
            use_table = true;
            rack_size = 2;
        }

        if (wt.getAmmoType() == AmmoType.T_AC_ROTARY) {
            use_table = true;
            rack_size = 4;
        }

        if (use_table) {
            int linked_ammo = wt.getAmmoType();

            // MMLs and ATMS change damage and range by ammo type,
            // which is going to require some serious gymnastics

            // ATMs, which include built-in Artemis
            if (linked_ammo == AmmoType.T_ATM) {

                damage_value = hits_by_racksize[rack_size] * 1.2;

                // Use ER ammo damage as a default
                Arrays.fill(raw_damage_array, damage_value);

                // All three types: use ER ranges, with HE for "short" range,
                // std for "medium" range, and ER for "long" range damages
                if (applicable_ranges[0] && applicable_ranges[1]
                        && applicable_ranges[2]) {
                    raw_damage_array[0] *= 3.0;
                    raw_damage_array[1] *= 3.0;
                    raw_damage_array[2] *= 2.0;
                }

                // HE only
                if (applicable_ranges[0] && !applicable_ranges[1]
                        && !applicable_ranges[2]) {
                    for (int i = 0; i < raw_damage_array.length; i++) {
                        raw_damage_array[i] *= 3.0;
                    }
                }

                // Standard only
                if (!applicable_ranges[0] && applicable_ranges[1]
                        && !applicable_ranges[2]) {
                    for (int i = 0; i < raw_damage_array.length; i++) {
                        raw_damage_array[i] *= 2.0;
                    }
                }

                // HE and standard only: use std ranges, with HE for the
                // "short" range damage
                if (applicable_ranges[0] && applicable_ranges[1]
                        && !applicable_ranges[2]) {

                    raw_damage_array[0] *= 3.0;
                    raw_damage_array[1] *= 3.0;
                    raw_damage_array[2] *= 3.0;
                    raw_damage_array[3] *= 2.0;

                }

                // HE and ER only: use ER ranges, with HE for the
                // "short" range damage
                if (applicable_ranges[0] && !applicable_ranges[1]
                        && applicable_ranges[2]) {

                    raw_damage_array[0] *= 3.0;
                    raw_damage_array[1] *= 3.0;
                    raw_damage_array[2] *= 1.0;
                    raw_damage_array[3] *= 1.0;

                }

                // Standard and ER only: use ER ranges, with std for "short"
                // and "medium" range damage
                if (!applicable_ranges[0] && applicable_ranges[1]
                        && applicable_ranges[2]) {

                    raw_damage_array[0] *= 2.0;
                    raw_damage_array[1] *= 2.0;
                    raw_damage_array[2] *= 2.0;
                    raw_damage_array[3] *= 1.0;

                }

            }

            // MMLs, which may have Artemis
            if (linked_ammo == AmmoType.T_MML) {

                damage_value = hits_by_racksize[rack_size];
                linked_guidance = weapon.getLinkedBy();
                if ((linked_guidance != null) && !linked_guidance.isDestroyed()
                        && !linked_guidance.isMissing()
                        && !linked_guidance.isBreached()
                        && (linked_guidance.getType() instanceof MiscType)) {

                    if (linked_guidance.getType().hasFlag(MiscType.F_ARTEMIS)) {
                        damage_value *= 1.2;
                    }

                }

                // Use LRM damage as a default
                Arrays.fill(raw_damage_array, damage_value);

                // If SRM ammo is available, use it for short and medium range
                if (applicable_ranges[0]) {

                    raw_damage_array[0] *= 2.0;
                    raw_damage_array[1] *= 2.0;
                    raw_damage_array[2] *= 2.0;

                    // If LRM ammo is not available, use SRM for long range too
                    if (!applicable_ranges[2]) {
                        raw_damage_array[3] *= 2.0;
                    }

                }

            }

            // LRMs, SRMs, which may have Artemis
            if ((linked_ammo == AmmoType.T_SRM)
                    || (linked_ammo == AmmoType.T_SRM_IMP) 
                    || (linked_ammo == AmmoType.T_LRM_IMP)
                    || (linked_ammo == AmmoType.T_LRM)) {

                damage_value = hits_by_racksize[rack_size];
                linked_guidance = weapon.getLinkedBy();
                if ((linked_guidance != null) && !linked_guidance.isDestroyed()
                        && !linked_guidance.isMissing()
                        && !linked_guidance.isBreached()
                        && (linked_guidance.getType() instanceof MiscType)) {

                    if (linked_guidance.getType().hasFlag(MiscType.F_ARTEMIS)) {
                        damage_value *= 1.2;
                    } else {
                        if (linked_guidance.getType().hasFlag(
                                MiscType.F_ARTEMIS_V)) {
                            damage_value *= 1.4;
                        }
                    }

                }

                if ((linked_ammo == AmmoType.T_SRM) || (linked_ammo == AmmoType.T_SRM_IMP)) {
                    damage_value *= 2.0;
                }

                Arrays.fill(raw_damage_array, damage_value);

            }

            // MRMs, which may have Apollo
            if (linked_ammo == AmmoType.T_MRM) {

                damage_value = hits_by_racksize[rack_size];
                linked_guidance = weapon.getLinkedBy();
                if ((linked_guidance != null) && !linked_guidance.isDestroyed()
                        && !linked_guidance.isMissing()
                        && !linked_guidance.isBreached()
                        && (linked_guidance.getType() instanceof MiscType)) {
                    damage_value *= 0.9;
                }

                Arrays.fill(raw_damage_array, damage_value);

            }

            // Streak SRMs and LRMs use full rack size
            if (linked_ammo == AmmoType.T_SRM_STREAK) {

                damage_value = rack_size * 2.0;

                Arrays.fill(raw_damage_array, damage_value);

            }
            if (linked_ammo == AmmoType.T_LRM_STREAK) {
                damage_value = rack_size;

                Arrays.fill(raw_damage_array, damage_value);
            }

            // HAGs get a bonus at short range and a penalty at long range
            if (linked_ammo == AmmoType.T_HAG) {
                damage_value = hits_by_racksize[rack_size];
                raw_damage_array[0] = damage_value * 1.2;
                raw_damage_array[1] = damage_value * 1.2;
                raw_damage_array[2] = damage_value;
                raw_damage_array[3] = damage_value * 0.8;
            }

            // LBX cannons are assumed to be firing cluster rounds.
            // TODO: extend ammo bin check from MMLs & ATMs
            if ((linked_ammo == AmmoType.T_AC_LBX)
                    || (linked_ammo == AmmoType.T_AC_LBX_THB)) {
                damage_value = hits_by_racksize[rack_size];

                for (int i = 0; i < raw_damage_array.length; i++) {
                    raw_damage_array[i] = damage_value;
                }
            }

            // Ultra and rotary cannons return damage values properly

            if ((linked_ammo == AmmoType.T_AC_ULTRA)
                    || (linked_ammo == AmmoType.T_AC_ULTRA_THB)
                    || (linked_ammo == AmmoType.T_AC_ROTARY)) {

                damage_value *= hits_by_racksize[rack_size];

                for (int i = 0; i < raw_damage_array.length; i++) {
                    raw_damage_array[i] = damage_value;
                }
            }

        } else {

            // Heavy Gauss, Snubnose PPC, variable speed lasers change
            // damage with range
            if ((wt instanceof ISImpHGaussRifle) || (wt instanceof ISSnubNosePPC)
                    || (wt instanceof VariableSpeedPulseLaserWeapon)) {
                raw_damage_array[0] = wt.getDamage(wt.getShortRange());
                raw_damage_array[1] = wt.getDamage(wt.getShortRange());
                raw_damage_array[2] = wt.getDamage(wt.getMediumRange());
                raw_damage_array[3] = wt.getDamage(wt.getLongRange());
            }

            // IS plasma rifle
            if (wt.getInternalName().equals("ISPlasmaRifle")) {

                damage_value = 12.0;

                for (int i = 0; i < raw_damage_array.length; i++) {
                    raw_damage_array[i] = damage_value;
                }

            }

            // Clan plasma cannon
            if (wt.getInternalName().equals("CLPlasmaCannon")) {

                damage_value = 10.5;

                for (int i = 0; i < raw_damage_array.length; i++) {
                    raw_damage_array[i] = damage_value;
                }

            }

            // Thunderbolt missiles are half damage inside minimum range
            if ((wt.getAmmoType() == AmmoType.T_TBOLT_5)
                    || (wt.getAmmoType() == AmmoType.T_TBOLT_10)
                    || (wt.getAmmoType() == AmmoType.T_TBOLT_15)
                    || (wt.getAmmoType() == AmmoType.T_TBOLT_20)) {

                for (int i = 0; i < raw_damage_array.length; i++) {
                    raw_damage_array[i] = damage_value;
                }
                raw_damage_array[0] = damage_value / 2.0;
            }
        }

        // Zero damage is acceptable, but negative damage means
        // something is wrong (typically a variable damage weapon
        // hasn't been handled). Push a line to the log file
        // to help developers and reset the damage value.

        if ((raw_damage_array[0] < 0) || (raw_damage_array[1] < 0)
                || (raw_damage_array[2] < 0) || (raw_damage_array[3] < 0)) {
            LogManager.getLogger().debug("Weapons characterization: negative damage for weapon "
                    + weapon.getName() + ".");

            raw_damage_array[0] = 1.0;
            raw_damage_array[1] = 1.0;
            raw_damage_array[2] = 1.0;
            raw_damage_array[3] = 1.0;

        }

        return raw_damage_array;
    }

    /**
     * Certain weapons, mostly ATMs and MMLs, can change both range and damage
     * based on what ammo is selected. This will go through the ammo bins that
     * are applicable to the specified weapon and set flags based on whether the
     * ammo is short, medium, or long range. ATMs can set all three; MMLs can
     * only set short and long range flags.
     *
     * @param weapon
     *            weapon being checked
     * @param ammo_list
     *            ArrayList of Mounted returned by Entity.getAmmo()
     * @return 3-element array indicating short/medium/long ranges available
     */
    private static boolean[] getAmmoRanges(WeaponType weapon,
            ArrayList<Mounted> ammo_list) {

        AmmoType bin_type = new AmmoType();
        int ammo_filter = weapon.getAmmoType();

        // Short, medium, and long range, respectively
        boolean[] range_flags = { false, false, false };

        // Filter for ATM and MML only. Add more weapons as needed.
        if ((ammo_filter != AmmoType.T_ATM) && (ammo_filter != AmmoType.T_MML)) {
            return range_flags;
        }

        // For each ammo bin
        for (Mounted ammo_bin : ammo_list) {

            // If all types are accounted for, no point in checking anything
            // else
            if (((ammo_filter == AmmoType.T_MML) && range_flags[0] && range_flags[2])
                    || ((ammo_filter == AmmoType.T_ATM) && range_flags[0]
                            && range_flags[1] && range_flags[2])) {
                return range_flags;
            }

            // If the bin isn't empty, isn't destroyed, and isn't breached
            if (ammo_bin.isAmmoUsable()) {

                // If the bin is the correct type for the weapon, and has the
                // proper rack size
                bin_type = (AmmoType) ammo_bin.getType();
                if ((bin_type.getAmmoType() == ammo_filter)
                        && (bin_type.getRackSize() == weapon.getRackSize())) {

                    // If the weapon is an ATM
                    if (ammo_filter == AmmoType.T_ATM) {

                        if (bin_type.getMunitionType() == AmmoType.M_HIGH_EXPLOSIVE) {
                            range_flags[0] = true;
                        } else {
                            if (bin_type.getMunitionType() == AmmoType.M_EXTENDED_RANGE) {
                                range_flags[2] = true;
                            } else {
                                range_flags[1] = true;
                            }
                        }

                    }

                    // If the weapon is an MML
                    if (ammo_filter == AmmoType.T_MML) {

                        // Really hate to depend on string comparisons but
                        // nothing else is available
                        if (bin_type.getShortName().contains("SRM")) {
                            range_flags[0] = true;
                        } else {
                            if (bin_type.getShortName().contains("LRM")
                                    && !AmmoType.canDeliverMinefield(bin_type)) {
                                range_flags[2] = true;
                            }
                        }

                    }

                }

            }

            // Next bin
        }

        return range_flags;
    }

    /**
     * Applies to-hit odds for a weapon at each range to raw damage numbers.
     * This provides an estimate of damage under average circumstances.
     *
     * @param weapon
     *            weapon being checked
     * @param gunskill
     *            Gunnery skill of attacker
     * @param raw_damage
     *            damage for min/short/medium/long range
     * @param ammo_ranges
     *            flags for short/medium/long range ammo available
     * @return array with the estimated damage values at each range
     */

    private static double[] getExpectedDamage(WeaponType weapon, int gunskill,
                                              double[] raw_damage, boolean[] ammo_ranges, boolean aptGunnery) {

        // Preset the modified damages
        double[] modified_damages = new double[MAX_RANGE];

        // Grab the weapon ranges here, outside the range loop
        int[] range_brackets = new int[5];
        range_brackets[RANGE_SHORT] = weapon.getShortRange();
        range_brackets[RANGE_MEDIUM] = weapon.getMediumRange();
        range_brackets[RANGE_LONG] = weapon.getLongRange();
        range_brackets[3] = weapon.getExtremeRange();
        range_brackets[4] = weapon.getMinimumRange();

        // ATM ranges depend on ammo types available. Use the longest range
        // ammo available.
        // TODO: figure out how to get ranges directly from ATM ammo
        if (weapon.getAmmoType() == AmmoType.T_ATM) {
            if (ammo_ranges[2]) {

                range_brackets[RANGE_SHORT] = 9;
                range_brackets[RANGE_MEDIUM] = 18;
                range_brackets[RANGE_LONG] = 27;
                range_brackets[3] = Math.min(36, MAX_RANGE);
                range_brackets[4] = ammo_ranges[0] ? -1 : 4;

            } else {
                if (ammo_ranges[1]) {

                    range_brackets[RANGE_SHORT] = 5;
                    range_brackets[RANGE_MEDIUM] = 10;
                    range_brackets[RANGE_LONG] = 15;
                    range_brackets[3] = 20;
                    range_brackets[4] = ammo_ranges[0] ? -1 : 4;

                } else {

                    range_brackets[RANGE_SHORT] = 3;
                    range_brackets[RANGE_MEDIUM] = 6;
                    range_brackets[RANGE_LONG] = 9;
                    range_brackets[3] = 12;
                    range_brackets[4] = -1;

                }
            }
        }

        // MML ranges depend on ammo types available
        // TODO: figure out how to get ranges directly from MML ammo
        if (weapon.getAmmoType() == AmmoType.T_MML) {
            if (ammo_ranges[2]) {

                // If LRM ammo is available, use LRM ranges. If SRM ammo
                // is also available it will be used at short range.
                range_brackets[RANGE_SHORT] = 7;
                range_brackets[RANGE_MEDIUM] = 14;
                range_brackets[RANGE_LONG] = 21;
                range_brackets[3] = 28;
                range_brackets[4] = ammo_ranges[0] ? -1 : 6;

            } else {
                if (ammo_ranges[0]) {

                    // If only SRM ammo is available, use SRM ranges
                    range_brackets[RANGE_SHORT] = 3;
                    range_brackets[RANGE_MEDIUM] = 6;
                    range_brackets[RANGE_LONG] = 9;
                    range_brackets[3] = 12;
                    range_brackets[4] = -1;

                }
            }
        }

        int total_mod;
        int weapon_mod = weapon.getToHitModifier();

        // Consider LBX cannons to always be firing cluster rounds
        if ((weapon.getAmmoType() == AmmoType.T_AC_LBX)
                || (weapon.getAmmoType() == AmmoType.T_AC_LBX_THB)) {
            weapon_mod = -1;
        }

        // For each hex of range out to extreme range or the extent of the
        // range array
        for (int cur_range = 0; (cur_range < MAX_RANGE)
                && (cur_range <= range_brackets[3]); cur_range++) {

            // Calculate the total to-hit modifier using the units
            // gunnery skill, the current range, and any weapon-specific
            // modifiers
            total_mod = gunskill + weapon_mod;
            if (cur_range > range_brackets[4]) {

                if (cur_range > range_brackets[RANGE_SHORT]) {
                    total_mod += 2;
                }
                if (cur_range > range_brackets[RANGE_MEDIUM]) {
                    total_mod += 2;
                }
                if (cur_range > range_brackets[RANGE_LONG]) {
                    total_mod += 4;
                }

            } else {

                // Inside minimum range
                total_mod += (1 + range_brackets[4]) - cur_range;
            }

            // Find the odds of hitting a target and apply them to the raw
            // damage
            if (cur_range <= range_brackets[RANGE_SHORT]) {
                if (cur_range > range_brackets[4]) {
                    modified_damages[cur_range] = (raw_damage[1]
                                                   * Compute.oddsAbove(total_mod, aptGunnery)) / 100;
                } else {
                    modified_damages[cur_range] = (raw_damage[0]
                                                   * Compute.oddsAbove(total_mod, aptGunnery)) / 100;
                }
            }
            if (cur_range > range_brackets[RANGE_SHORT]) {
                modified_damages[cur_range] = (raw_damage[2]
                                               * Compute.oddsAbove(total_mod, aptGunnery)) / 100;
            }
            if (cur_range > range_brackets[RANGE_MEDIUM]) {
                modified_damages[cur_range] = (raw_damage[3]
                                               * Compute.oddsAbove(total_mod, aptGunnery)) / 100;
            }

        }

        return modified_damages;
    }

    /**
     * Applies to-hit odds for a conventional infantry unit at each range to raw
     * damage numbers. This provides an estimate of damage under average
     * circumstances.
     *
     * @param attacker
     *            Infantry unit
     * @param gunskill
     * @return
     */
    private static double[] getExpectedDamage(Infantry attacker, int gunskill) {

        InfantryWeapon primary_weapon = attacker.getPrimaryWeapon();
        InfantryWeapon secondary_weapon = attacker.getSecondaryWeapon();

        // Preset the modified damages
        double raw_damage = 0.0;
        double[] modified_damages = new double[MAX_RANGE];

        // Base range for conventional infantry weapons
        int base_range = 0;
        int total_mod;

        // Unarmed infantry unit doesn't do any damage
        if (null == primary_weapon) {
            return modified_damages;
        }

        // Base damage for a single trooper. Number of troopers will be
        // accounted for later.
        raw_damage = attacker.getDamagePerTrooper();

        // If there are two secondary weapons per squad then use that weapons
        // range. Otherwise use the primary weapons range.
        if ((null != secondary_weapon) && (attacker.getSecondaryN() >= 2)) {
            base_range = secondary_weapon.getInfantryRange();
        } else {
            base_range = primary_weapon.getInfantryRange();
        }
        for (int cur_range = 0; (cur_range < MAX_RANGE)
                && (cur_range <= (base_range * 4)); cur_range++) {

            // Range modifiers are a little screwey. This is just a rough
            // estimate.
            total_mod = gunskill;
            if (cur_range == 0) {
                if ((base_range > 0) && (base_range <= 4)) {
                    total_mod -= 2;
                }
                if ((base_range > 0) && (base_range > 4)) {
                    total_mod -= 1;
                }
            }
            if (cur_range > base_range) {
                total_mod += 2;
            }
            if (cur_range > (base_range * 2)) {
                total_mod += 2;
            }
            if (cur_range > (base_range * 3)) {
                total_mod += 4;
            }

            boolean aptGunnery = attacker.hasAbility(OptionsConstants.PILOT_APTITUDE_GUNNERY);
            modified_damages[cur_range] = (raw_damage
                                           * Compute.oddsAbove(total_mod, aptGunnery)) / 100.0;

        }

        return modified_damages;
    }

    public static int getFiringAngle(final Coords dest, int dest_facing,
            final Coords src) {
        int fa = dest.degree(src) - ((dest_facing % 6) * 60);
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Entity) {
            return ((Entity) obj).getId() == entity.getId();
        }
        if (obj instanceof CEntity) {
            return ((CEntity) obj).entity.getId() == entity.getId();
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
