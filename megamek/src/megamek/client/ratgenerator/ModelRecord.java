/*
 * MegaMek - Copyright (C) 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ratgenerator;

import megamek.common.*;
import megamek.common.weapons.*;
import megamek.common.weapons.artillery.ArtilleryWeapon;
import megamek.common.weapons.battlearmor.BAFlamerWeapon;
import megamek.common.weapons.battlearmor.BAMGWeapon;
import megamek.common.weapons.battlearmor.CLBAMGBearhunterSuperheavy;
import megamek.common.weapons.defensivepods.BPodWeapon;
import megamek.common.weapons.flamers.FlamerWeapon;
import megamek.common.weapons.gaussrifles.CLAPGaussRifle;
import megamek.common.weapons.gaussrifles.GaussWeapon;
import megamek.common.weapons.infantry.InfantrySupportMk2PortableAAWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.common.weapons.lasers.CLPulseLaserSmall;
import megamek.common.weapons.lasers.ISPulseLaserSmall;
import megamek.common.weapons.lrms.StreakLRMWeapon;
import megamek.common.weapons.mgs.MGWeapon;
import megamek.common.weapons.missiles.ATMWeapon;
import megamek.common.weapons.missiles.MMLWeapon;
import megamek.common.weapons.missiles.RLWeapon;
import megamek.common.weapons.mortars.MekMortarWeapon;
import megamek.common.weapons.ppc.CLPlasmaCannon;
import megamek.common.weapons.ppc.ISPlasmaRifle;
import megamek.common.weapons.srms.SRMWeapon;
import megamek.common.weapons.srms.StreakSRMWeapon;

import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;


/**
 * Specific unit variants; analyzes equipment to determine suitability for certain types
 * of missions in addition to what is formally declared in the data files.
 *
 * @author Neoancient
 */
public class ModelRecord extends AbstractUnitRecord {
    public static final int NETWORK_NONE = 0;
    public static final int NETWORK_C3_SLAVE = 1;
    public static final int NETWORK_BA_C3 = 1;
    public static final int NETWORK_C3_MASTER = 1 << 1;
    public static final int NETWORK_C3I = 1 << 2;
    public static final int NETWORK_NAVAL_C3 = 1 << 2;
    public static final int NETWORK_NOVA = 1 << 3;

    public static final int NETWORK_BOOSTED = 1 << 4;
    public static final int NETWORK_COMPANY_COMMAND = 1 << 5;

    public static final int NETWORK_BOOSTED_SLAVE = NETWORK_C3_SLAVE | NETWORK_BOOSTED;
    public static final int NETWORK_BOOSTED_MASTER = NETWORK_C3_MASTER | NETWORK_BOOSTED;

    private MechSummary mechSummary;

    private boolean primitive;
    private boolean retrotech;
    private boolean starLeague;

    private int weightClass;
    private EntityMovementMode movementMode;
    private boolean isQuad;
    private boolean isTripod;
    private boolean remoteDrone;
    private boolean robotDrone;

    private EnumSet<MissionRole> roles;
    private ArrayList<String> deployedWith;
    private ArrayList<String> requiredUnits;
    private ArrayList<String> excludedFactions;
    private int networkMask;

    private double flakBVProportion;
    private double artilleryBVProportion;
    private double lrBVProportion;
    private double srBVProportion;
    private double ammoBVProportion;
    private boolean incendiary;
    private boolean apWeapons;
    private boolean unarmed;

    private int speed;

    private boolean mechanizedBA;
    private boolean magClamp;
    private boolean canAntiMek;

    public ModelRecord(String chassis, String model) {
        super(chassis);
        roles = EnumSet.noneOf(MissionRole.class);
        deployedWith = new ArrayList<>();
        requiredUnits = new ArrayList<>();
        excludedFactions = new ArrayList<>();
        networkMask = NETWORK_NONE;
    }

    public ModelRecord(MechSummary unitData) {
        this(unitData.getFullChassis(), unitData.getModel());
        mechSummary = unitData;
        introYear = unitData.getYear();

        analyzeModel(unitData);
    }

    public String getModel() {
        return mechSummary.getModel();
    }

    public int getWeightClass() {
        return weightClass;
    }

    public EntityMovementMode getMovementMode() {
        return movementMode;
    }

    public boolean isQuadMech() {
        return isQuad;
    }

    public boolean isTripodMech () {
        return isTripod;
    }

    @Override
    public boolean isClan() {
        return clan;
    }

    /**
     * Unit contains at least some primitive technology, without any advanced tech.
     * Testing is not extensive, there may be units that are not properly flagged.
     * @return   true, if unit contains no advanced tech and at least some primitive tech
     */
    public boolean isPrimitive() {
        return primitive;
    }

    /**
     * Unit consists of at least some primitive technology and some advanced/Star League/Clan
     * technology. Testing is not extensive, there may be units that are not properly flagged.
     * @return   true, if unit contains both primitive and advanced tech
     */
    public boolean isRetrotech () {
        return retrotech;
    }

    /**
     * Unit has advanced IS-base technology
     * @return
     */
    public boolean isSL() {
        return starLeague;
    }

    public Set<MissionRole> getRoles() {
        return roles;
    }
    public ArrayList<String> getDeployedWith() {
        return deployedWith;
    }
    public ArrayList<String> getRequiredUnits() {
        return requiredUnits;
    }
    public ArrayList<String> getExcludedFactions() {
        return excludedFactions;
    }
    public int getNetworkMask() {
        return networkMask;
    }
    public void setNetwork(int network) {
        this.networkMask = network;
    }

    /**
     * Proportion of total weapons BV that is optimized against airborne targets
     * @return   between zero (none) and 1.0 (all weapons)
     */
    public double getFlak() {
        return flakBVProportion;
    }

    /**
     * Proportion of total weapons BV that is artillery
     * @return   between zero (none) and 1.0 (all weapons)
     */
    public double getArtilleryProportion () {
        return artilleryBVProportion;
    }

    /**
     * Proportion of total weapons BV that is capable of attacking targets at longer ranges.
     * Units with values of 0.75 or higher are mostly armed with weapons that can hit targets at
     * 15+ hexes, have a minimum range, and potentially fire indirectly in ground combat; or
     * reach long/extreme range in air or space combat.
     * Complementary to getSRProportion() - where one is high and the other is low, the unit is
     * specialized for that range bracket. If both values are similar the unit is well balanced
     * between long and short ranged capabilities.
     * TODO: rename for consistency and clarity
     * @return   between zero (none) and 1.0 (all weapons)
     */
    public double getLongRange() {
        return lrBVProportion;
    }

    /**
     * Proportion of total weapons BV that is limited to attacking targets at close range.
     * Units with values of 0.75 or higher are mostly armed with weapons that have a long range
     * of less than 12 hexes and do not have a minimum range in ground combat, or are limited to
     * short range in air/space combat.
     * Complementary to getLongRange() - where one is high and the other is low, the unit is
     * specialized for that range bracket. If both values are similar the unit is well balanced
     * between long and short ranged capabilities.
     * @return   between zero (none) and 1.0 (all weapons)
     */
    public double getSRProportion() {
        return srBVProportion;
    }

    public int getSpeed() {
        return speed;
    }

    public boolean getJump() {
        return mechSummary.getJumpMp() > 0;
    }

    /**
     * Proportion of total weapons BV that is dependent on ammunition
     * @return   between zero (none) and 1.0 (all weapons)
     */
    public double getAmmoRequirement() {
        return ammoBVProportion;
    }

    public boolean hasIncendiaryWeapon() {
        return incendiary;
    }

    public boolean hasAPWeapons() {
        return apWeapons;
    }

    /***
     *
     * @return   true if unit has no BV invested in weapons
     */
    public boolean isUnarmed () {
        return unarmed;
    }

    /**
     * Unit is a remotely operated drone
     * @return   true if unit has remote drone operation equipment
     */
    public boolean isRemoteDrone () {
        return remoteDrone;
    }

    /**
     * Unit is an independently operating drone
     * @return   true if unit has robotic operations system
     */
    public boolean isRobotDrone () {
        return robotDrone;
    }

    public MechSummary getMechSummary() {
        return mechSummary;
    }

    public void addRoles(String newRoles) {
        if (newRoles.isBlank()) {
            roles.clear();
        } else {
            String[] fields = newRoles.split(",");
            for (String role : fields) {
                MissionRole mr = MissionRole.parseRole(role);
                if (mr != null) {
                    roles.add(mr);
                } else {
                    LogManager.getLogger().error("Could not parse mission role for "
                            + getChassis() + " " + getModel() + ": " + role);
                }
            }
        }
    }

    public void setRequiredUnits(String requiredUnitNames) {
        String[] subfields = requiredUnitNames.split(",");
        for (String unit : subfields) {
            if (unit.startsWith("req:")) {
                this.requiredUnits.add(unit.replace("req:", ""));
            } else {
                deployedWith.add(unit);
            }
        }
    }

    public void setExcludedFactions(String excludedFactions) {
        this.excludedFactions.clear();
        String[] fields = excludedFactions.split(",");
        for (String faction : fields) {
            this.excludedFactions.add(faction);
        }
    }

    public boolean factionIsExcluded(FactionRecord checkFaction) {
        return excludedFactions.contains(checkFaction.getKey());
    }

    public boolean factionIsExcluded(String faction, String subfaction) {
        if (subfaction == null) {
            return excludedFactions.contains(faction);
        } else {
            return excludedFactions.contains(faction + "." + subfaction);
        }
    }

    @Override
    public String getKey() {
        return mechSummary.getName();
    }

    public boolean canDoMechanizedBA() {
        return mechanizedBA;
    }

    public void setMechanizedBA(boolean flag) {
        mechanizedBA = flag;
    }

    public boolean hasMagClamp() {
        return magClamp;
    }

    public void setMagClamp(boolean flag) {
        magClamp = flag;
    }

    public boolean getAntiMek(){
        return canAntiMek;
    }


    /**
     * Checks the equipment carried by this unit and summarizes it in a variety of easy to access
     * data
     * @param unitData   Data for unit
     */
    private void analyzeModel (MechSummary unitData) {

        // Basic unit type and movement
        unitType = parseUnitType(unitData.getUnitType());
        if (unitType == UnitType.MEK) {

            movementMode = EntityMovementMode.BIPED;
            if (unitData.isQuadMek()) {
                isQuad = true;
                movementMode = EntityMovementMode.QUAD;
            } else if (unitData.isTripodMek()) {
                isTripod = true;
                movementMode = EntityMovementMode.TRIPOD;
            }

        } else {
            movementMode = EntityMovementMode.parseFromString(unitData.getUnitSubType().toLowerCase());
        }

        if (unitType == UnitType.MEK ||
                unitType == UnitType.TANK ||
                unitType == UnitType.VTOL ||
                unitType == UnitType.CONV_FIGHTER ||
                unitType == UnitType.AEROSPACEFIGHTER) {
            omni = unitData.getOmni();
        }

        speed = unitData.getWalkMp();
        // Limit jump checks to units which can actually jump
        if (unitType <= UnitType.PROTOMEK &&
                unitData.getJumpMp() > 0) {
            int jumpDistance = unitData.getJumpMp();
            if (unitType == UnitType.INFANTRY || unitType == UnitType.BATTLE_ARMOR) {
                speed = Math.max(speed, jumpDistance);
            } else {
                speed = Math.max(jumpDistance, (speed + 1));
            }
        }

        weightClass = unitData.getWeightClass();

        // Adjust weight class for support vehicles
        if ((unitType == UnitType.TANK ||
                unitType == UnitType.VTOL ||
                unitType == UnitType.NAVAL ||
                unitType == UnitType.CONV_FIGHTER) &&
                weightClass >= EntityWeightClass.WEIGHT_SMALL_SUPPORT) {
            if (unitData.getTons() <= 39) {
                weightClass = EntityWeightClass.WEIGHT_LIGHT;
            } else if (unitData.getTons() <= 59) {
                weightClass = EntityWeightClass.WEIGHT_MEDIUM;
            } else if (unitData.getTons() <= 79) {
                weightClass = EntityWeightClass.WEIGHT_HEAVY;
            } else if (unitData.getTons() <= 100) {
                weightClass = EntityWeightClass.WEIGHT_ASSAULT;
            } else {
                weightClass = EntityWeightClass.WEIGHT_COLOSSAL;
            }
        }

        double totalWeaponBV = 0.0;
        double flakBV = 0.0;
        double artilleryBV = 0.0;
        double longRangeBV = 0.0;
        double shortRangeBV = 0.0;
        int apRating = 0;
        int apThreshold = 6;
        double ammoBV = 0.0;

        // Base technology checks

        boolean losTech = false;
        boolean basePrimitive = false;

        clan = unitData.isClan();

        // Check if the base unit is primitive
        if (!clan &&
                unitType != UnitType.INFANTRY &&
                unitType != UnitType.BATTLE_ARMOR &&
                unitType != UnitType.PROTOMEK &&
                unitType != UnitType.WARSHIP) {
            basePrimitive = isUnitPrimitive(unitData);
        }
        // If the unit is not Clan or primitive, then check for if it is lostech (advanced)
        if (!clan &&
                !basePrimitive &&
                unitType <= UnitType.AEROSPACEFIGHTER &&
                unitType != UnitType.INFANTRY) {
            losTech = unitHasLostech(unitData, false);
        }

        for (int i = 0; i < unitData.getEquipmentNames().size(); i++) {

            //EquipmentType.get is throwing an NPE intermittently, and the only possibility I can see
            //is that there is a null equipment name.
            if (null == unitData.getEquipmentNames().get(i)) {
                LogManager.getLogger().error(
                        "RATGenerator ModelRecord encountered null equipment name in MechSummary for "
                                + unitData.getName() + ", index " + i);
                continue;
            }
            EquipmentType eq = EquipmentType.get(unitData.getEquipmentNames().get(i));
            if (eq == null) {
                continue;
            }

            // Only check for lostech equipment if it hasn't already been found
            if (!losTech && !eq.isAvailableIn(3000, false)) {
                losTech = true;
            }

            if (eq instanceof WeaponType) {

                // Flag units that are capable of making anti-Mech attacks. Don't bother making
                // any other tests for these.
                if (unitType == UnitType.INFANTRY || unitType == UnitType.BATTLE_ARMOR) {
                    boolean isAntiMechAttack = eq instanceof SwarmAttack ||
                            eq instanceof SwarmWeaponAttack ||
                            eq instanceof LegAttack ||
                            eq instanceof StopSwarmAttack;
                    canAntiMek = canAntiMek || isAntiMechAttack;
                    if (isAntiMechAttack) {
                        continue;
                    }
                }

                // Add the spotter role to all units which carry TAG
                if (unitType <= UnitType.AEROSPACEFIGHTER && eq.hasFlag(WeaponType.F_TAG)) {
                    roles.add(MissionRole.SPOTTER);
                    losTech = true;
                    continue;
                }

                totalWeaponBV += eq.getBV(null) * unitData.getEquipmentQuantities().get(i);

                // Check for C3 master units. These are bit-masked values.
                if (eq.hasFlag(WeaponType.F_C3M)) {
                    networkMask |= NETWORK_C3_MASTER;
                    if (unitData.getEquipmentQuantities().get(i) > 1) {
                        networkMask |= NETWORK_COMPANY_COMMAND;
                    }
                    losTech = true;
                    continue;
                } else if (eq.hasFlag(WeaponType.F_C3MBS)) {
                    networkMask |= NETWORK_BOOSTED_MASTER;
                    if (unitData.getEquipmentQuantities().get(i) > 1) {
                        networkMask |= NETWORK_COMPANY_COMMAND;
                    }
                    losTech = true;
                    continue;
                }

                // Check for use against airborne targets. Ignore small craft, DropShips, and other
                // large space craft.
                if (unitType < UnitType.SMALL_CRAFT) {
                    flakBV += getFlakBVModifier((WeaponType) eq) * eq.getBV(null) *
                            unitData.getEquipmentQuantities().get(i);
                }

                // Check for artillery weapons. Ignore aerospace fighters, small craft, and large
                // space craft.
                if (unitType <= UnitType.CONV_FIGHTER && eq instanceof ArtilleryWeapon) {
                    artilleryBV += eq.getBV(null) * unitData.getEquipmentQuantities().get(i);
                }

                // Don't check incendiary weapons for conventional infantry, fixed wing aircraft,
                // and space-going units
                if (!incendiary &&
                        (unitType < UnitType.CONV_FIGHTER && unitType != UnitType.INFANTRY)) {
                    if (eq instanceof FlamerWeapon ||
                            eq instanceof BAFlamerWeapon ||
                            eq instanceof ISPlasmaRifle ||
                            eq instanceof CLPlasmaCannon) {
                        incendiary = true;
                    }
                    // Some missile types are capable of being loaded with infernos, which are
                    // highly capable of setting fires
                    if (((WeaponType) eq).getAmmoType() == AmmoType.T_SRM ||
                            ((WeaponType) eq).getAmmoType() == AmmoType.T_SRM_IMP ||
                            ((WeaponType) eq).getAmmoType() == AmmoType.T_MML ||
                            ((WeaponType) eq).getAmmoType() == AmmoType.T_IATM) {
                        incendiary = true;
                    }
                }

                // Don't check anti-personnel weapons for conventional infantry, fixed wing
                // aircraft, and space-going units. Also, don't bother checking if we're
                // already high enough.
                if (apRating < apThreshold &&
                        (unitType < UnitType.CONV_FIGHTER && unitType != UnitType.INFANTRY)) {
                    apRating += getAPRating((WeaponType) eq);
                }

                // Total up BV for weapons that require ammo. Streak-type missile systems get a
                // discount. Ignore small craft, DropShips, and large space craft. Ignore infantry
                // weapons except for field guns.
                if (unitType < UnitType.SMALL_CRAFT &&
                        ((WeaponType) eq).getAmmoType() > AmmoType.T_NA &&
                        !(eq instanceof InfantryWeapon)) {
                    double ammoFactor = 1.0;

                    if (eq instanceof StreakSRMWeapon ||
                            eq instanceof StreakLRMWeapon ||
                            ((WeaponType) eq).getAmmoType() == AmmoType.T_IATM) {
                        ammoFactor = 0.4;
                    }

                    if (eq.hasFlag(WeaponType.F_ONESHOT)) {
                        ammoFactor = 0.1;
                    }

                    ammoBV += eq.getBV(null) * ammoFactor *
                            unitData.getEquipmentQuantities().get(i);

                }

                // Total up BV for weapons capable of attacking at the longest ranges or using
                // indirect fire. Ignore small craft, DropShips, and other space craft.
                if (unitType < UnitType.SMALL_CRAFT) {
                    longRangeBV += getLongRangeModifier((WeaponType) eq) * eq.getBV(null) *
                            unitData.getEquipmentQuantities().get(i);
                }

                // Total up BV of weapons suitable for attacking at close range. Ignore small craft,
                // DropShips, and other space craft. Also skip anti-Mech attacks.
                if (unitType < UnitType.SMALL_CRAFT) {
                    shortRangeBV += getShortRangeModifier((WeaponType) eq) * eq.getBV(null) *
                            unitData.getEquipmentQuantities().get(i);
                }

            // Various non-weapon equipment
            } else if (eq instanceof MiscType) {

                if (eq.hasFlag(MiscType.F_DOUBLE_HEAT_SINK) ||
                        eq.hasFlag(MiscType.F_IS_DOUBLE_HEAT_SINK_PROTOTYPE) ||
                        eq.hasFlag(MiscType.F_LASER_HEAT_SINK) ||
                        eq.hasFlag(MiscType.F_COMPACT_HEAT_SINK) ||
                        eq.hasFlag(MiscType.F_ECM) ||
                        eq.hasFlag(MiscType.F_ANGEL_ECM) ||
                        eq.hasFlag(MiscType.F_BAP) ||
                        eq.hasFlag(MiscType.F_BLOODHOUND) ||
                        eq.hasFlag(MiscType.F_TARGCOMP) ||
                        eq.hasFlag(MiscType.F_ARTEMIS) ||
                        eq.hasFlag(MiscType.F_ARTEMIS_V) ||
                        eq.hasFlag(MiscType.F_APOLLO) ||
                        eq.hasFlag((MiscType.F_MASC))) {
                    losTech = true;
                } else if (eq.hasFlag(MiscType.F_CLUB)) {
                    shortRangeBV += unitData.getTons() * 0.3;
                    totalWeaponBV += unitData.getTons() * 0.3;
                } else if (eq.hasFlag(MiscType.F_C3S)) {
                    networkMask |= NETWORK_C3_SLAVE;
                    losTech = true;
                } else if (eq.hasFlag(MiscType.F_C3I)) {
                    networkMask |= NETWORK_C3I;
                    losTech = true;
                } else if (eq.hasFlag(MiscType.F_C3SBS)) {
                    networkMask |= NETWORK_BOOSTED_SLAVE;
                    losTech = true;
                } else if (eq.hasFlag(MiscType.F_NOVA)) {
                    networkMask |= NETWORK_NOVA;
                    losTech = true;
                } else if (eq.hasFlag(MiscType.F_AP_POD)) {
                    apRating++;
                } else if (eq.hasFlag(MiscType.F_MAGNETIC_CLAMP)) {
                    magClamp = true;
                    losTech = true;
                } else if (eq.hasFlag(MiscType.F_VIBROCLAW)) {
                    apRating += 2;
                } else if (eq.hasFlag(MiscType.F_PROTOMECH_MELEE)) {
                    shortRangeBV += 2.5;
                    totalWeaponBV += 2.5;
                } else if (eq.hasFlag(MiscType.F_DRONE_OPERATING_SYSTEM)) {
                    remoteDrone = true;
                    losTech = true;
                } else if (eq.hasFlag(MiscType.F_SRCS) ||
                        eq.hasFlag(MiscType.F_SASRCS)) {
                    remoteDrone = true;
                    robotDrone = true;
                    losTech = true;
                } else if (eq.hasFlag(MiscType.F_SPACE_ADAPTATION)) {
                    roles.add(MissionRole.MARINE);
                // Save a bit of time, anything introduced after this date is assumed to be
                // advanced
                } else if (!losTech && (eq.getIntroductionDate() >= 3067)) {
                    losTech = true;
                }

            }
        }

        // Calculate BV proportions for all ground units, VTOL, blue water naval, gun emplacements
        // and fixed wing aircraft. Exclude Small craft, DropShips, and large space craft.
        if (unitType <= UnitType.AEROSPACEFIGHTER) {
            if (totalWeaponBV > 0) {
                flakBVProportion = flakBV / totalWeaponBV;
                artilleryBVProportion = artilleryBV / totalWeaponBV;
                lrBVProportion = longRangeBV / totalWeaponBV;
                srBVProportion = shortRangeBV / totalWeaponBV;
                ammoBVProportion = ammoBV / totalWeaponBV;

                apWeapons = apRating >= apThreshold;
            } else {
                unarmed = true;
            }
        }

        // Categorize by technology type
        starLeague = losTech && !clan;
        primitive = basePrimitive && !losTech && !clan;
        retrotech = basePrimitive && (losTech || clan);

    }

    /**
     * Units are considered primitive if they have no advanced tech and at least some primitive
     * tech. The check is not exhaustive so some niche units may not be flagged properly.
     * @param unitData   Unit data
     * @return   true if unit has primitive tech and no advanced tech
     */
    private boolean isUnitPrimitive (MechSummary unitData) {

        boolean hasPrimitive = false;

        // Some unit types will not be built with primitive technology
        if (unitType == UnitType.INFANTRY ||
                unitType == UnitType.BATTLE_ARMOR ||
                unitType == UnitType.PROTOMEK ||
                unitType == UnitType.WARSHIP) {
            return false;
        }

        // Primitive engines are not identified, so check for use of advanced types
        int engine_type = unitData.getEngineType();
        if (unitType != UnitType.GUN_EMPLACEMENT &&
                unitType != UnitType.SMALL_CRAFT &&
                unitType != UnitType.DROPSHIP &&
                unitType != UnitType.JUMPSHIP &&
                (engine_type == Engine.XL_ENGINE ||
                engine_type == Engine.LIGHT_ENGINE ||
                engine_type == Engine.XXL_ENGINE ||
                engine_type == Engine.COMPACT_ENGINE)) {
            return false;
        }

        // Primitive gyros are not identified, so check for use of advanced types
        if (unitType == UnitType.MEK) {
            if (unitData.getGyroType() >= Mech.GYRO_COMPACT) {
                return false;
            }
        }

        // Primitive structure is not identified, so check for use of advanced types
        if (unitType == UnitType.MEK) {
            if (unitData.getInternalsType() >= EquipmentType.T_STRUCTURE_ENDO_STEEL &&
                unitData.getInternalsType() <= EquipmentType.T_STRUCTURE_ENDO_COMPOSITE) {
                return false;
            }
            hasPrimitive = (unitData.getInternalsType() == EquipmentType.T_STRUCTURE_INDUSTRIAL);
        }

        // If standard, industrial, or primitive armor is not present, then it must be advanced
        int checkArmor = (int) (unitData.getArmorType().toArray()[0]);
        if (unitType <= UnitType.NAVAL &&
                checkArmor != EquipmentType.T_ARMOR_STANDARD &&
                checkArmor != EquipmentType.T_ARMOR_PRIMITIVE &&
                checkArmor != EquipmentType.T_ARMOR_INDUSTRIAL &&
                checkArmor != EquipmentType.T_ARMOR_HEAVY_INDUSTRIAL) {
            return false;
        } else if ((unitType == UnitType.CONV_FIGHTER ||
                unitType == UnitType.AEROSPACEFIGHTER ||
                unitType == UnitType.SMALL_CRAFT ||
                unitType == UnitType.DROPSHIP) &&
                checkArmor != EquipmentType.T_ARMOR_STANDARD &&
                checkArmor != EquipmentType.T_ARMOR_AEROSPACE &&
                checkArmor != EquipmentType.T_ARMOR_PRIMITIVE_FIGHTER &&
                checkArmor != EquipmentType.T_ARMOR_PRIMITIVE_AERO) {
            return false;
        }
        if (checkArmor == EquipmentType.T_ARMOR_PRIMITIVE ||
                checkArmor == EquipmentType.T_ARMOR_PRIMITIVE_FIGHTER ||
                checkArmor == EquipmentType.T_ARMOR_PRIMITIVE_AERO) {
            hasPrimitive = true;
        }

        // Cockpit control systems
        int checkCockpit = unitData.getCockpitType();
        if (unitType == UnitType.MEK) {
            if (checkCockpit != Mech.COCKPIT_STANDARD &&
                    checkCockpit != Mech.COCKPIT_INDUSTRIAL &&
                    checkCockpit != Mech.COCKPIT_PRIMITIVE &&
                    checkCockpit != Mech.COCKPIT_PRIMITIVE_INDUSTRIAL) {
                return false;
            } else if (checkCockpit == Mech.COCKPIT_PRIMITIVE ||
                    checkCockpit == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL) {
                hasPrimitive = true;
            }
        } else if (unitType == UnitType.CONV_FIGHTER || unitType == UnitType.AEROSPACEFIGHTER) {
            if (checkCockpit == Aero.COCKPIT_SMALL ||
                    checkCockpit == Aero.COCKPIT_COMMAND_CONSOLE) {
                return false;
            } else if (checkCockpit == Aero.COCKPIT_PRIMITIVE) {
                hasPrimitive = true;
            }
        }

        // If the unit has any primitive tech and nothing but standard tech, it is considered
        // primitive
        return hasPrimitive;
    }

    /**
     * Checks that unit has at least one piece of primitive tech for basic unit components such as
     * engine, frame, and armor. The check is not extensive so some units may include a niche item
     * even though they are not flagged as such.
     * @param unitData   Unit data
     * @return     true if unit contains primitive basic equipment
     */
    private boolean unitHasPrimitiveTech(MechSummary unitData) {

        // Some unit types will not be built with primitive technology
        if (unitType == UnitType.INFANTRY ||
                unitType == UnitType.BATTLE_ARMOR ||
                unitType == UnitType.PROTOMEK ||
                unitType == UnitType.WARSHIP) {
            return false;
        }

        // Check for non-advanced engines
        int checkEngine = unitData.getEngineType();
        if (unitType == UnitType.MEK &&
                (checkEngine == Engine.COMBUSTION_ENGINE ||
                        checkEngine == Engine.FISSION ||
                        checkEngine == Engine.BATTERY)) {
            return true;
        } else if ((unitType == UnitType.TANK ||
                unitType == UnitType.CONV_FIGHTER ||
                unitType == UnitType.AEROSPACEFIGHTER) &&
                checkEngine == Engine.FISSION) {
            return true;
        }

        // Armor
        int checkArmor = (int) (unitData.getArmorType().toArray()[0]);
        if (checkArmor == EquipmentType.T_ARMOR_PRIMITIVE ||
                checkArmor == EquipmentType.T_ARMOR_PRIMITIVE_FIGHTER ||
                checkArmor == EquipmentType.T_ARMOR_PRIMITIVE_AERO) {
            return true;
        }

        // Cockpit/control systems
        if (unitType == UnitType.MEK &&
                (unitData.getCockpitType() == Mech.COCKPIT_PRIMITIVE ||
                        unitData.getCockpitType() == Mech.COCKPIT_PRIMITIVE_INDUSTRIAL)) {
            return true;
        } else if ((unitType == UnitType.CONV_FIGHTER ||
                unitType == UnitType.AEROSPACEFIGHTER) &&
                unitData.getCockpitType() == Aero.COCKPIT_PRIMITIVE) {
            return true;
        }

        return false;
    }


    /**
     * Check if unit is built with advanced technology. This only checks the basic components,
     * not any mounted equipment such as weapons.
     * @param unitData         unit data
     * @param starLeagueOnly   true to only check original Star League tech - XL engine, ES internals,
     *                   FF armor
     * @return           true if unit has at least one piece of basic technology
     */
    private boolean unitHasLostech (MechSummary unitData, boolean starLeagueOnly) {

        // Some units are always considered advanced
        if (unitType == UnitType.BATTLE_ARMOR ||
                unitType == UnitType.PROTOMEK ||
                unitType == UnitType.WARSHIP) {
            return true;
        }

        // Conventional infantry are always basic tech
        if (unitType == UnitType.INFANTRY) {
            return false;
        }

        // Engine check. Star League is limited to XL.
        if (unitType == UnitType.MEK ||
                unitType == UnitType.TANK ||
                unitType == UnitType.VTOL ||
                unitType == UnitType.CONV_FIGHTER ||
                unitType == UnitType.AEROSPACEFIGHTER) {
            int checkEngine = unitData.getEngineType();
            if (starLeagueOnly) {
                if (checkEngine == Engine.XL_ENGINE) {
                    return true;
                }
            } else if (checkEngine >= Engine.XL_ENGINE &&
                    checkEngine <= Engine.COMPACT_ENGINE &&
                    checkEngine != Engine.FUEL_CELL) {
                return true;
            }
        }

        // Gyro. Star League has no advanced gyro types.
        if (unitType == UnitType.MEK && !starLeagueOnly) {
            if (unitData.getGyroType() >= Mech.GYRO_COMPACT) {
                return true;
            }
        }

        // Structure. Star League is limited endosteel.
        if (unitType == UnitType.MEK) {
            if (starLeagueOnly &&
                    unitData.getInternalsType() == EquipmentType.T_STRUCTURE_ENDO_STEEL) {
                return true;
            } else if (unitData.getInternalsType() >= EquipmentType.T_STRUCTURE_ENDO_STEEL &&
                    unitData.getInternalsType() <= EquipmentType.T_STRUCTURE_ENDO_COMPOSITE) {
                return true;
            }
        }

        // Armor. Star League is limited to simple ferro-fibrous.
        int checkArmor = (int) (unitData.getArmorType().toArray()[0]);
        if (unitType <= UnitType.NAVAL) {
            if (starLeagueOnly && checkArmor == EquipmentType.T_ARMOR_FERRO_FIBROUS) {
                return true;
            } else if ((checkArmor >= EquipmentType.T_ARMOR_FERRO_FIBROUS &&
                    checkArmor <= EquipmentType.T_ARMOR_FERRO_FIBROUS_PROTO) ||
                    checkArmor == EquipmentType.T_ARMOR_FERRO_LAMELLOR ||
                    (checkArmor >= EquipmentType.T_ARMOR_STEALTH_VEHICLE &&
                            checkArmor <= EquipmentType.T_ARMOR_BALLISTIC_REINFORCED)) {
                return true;
            }
        }

        // Cockpit. Star League is limited to command consoles.
        int checkCockpit = unitData.getCockpitType();
        if (unitType == UnitType.MEK) {
            if (starLeagueOnly && checkCockpit == Mech.COCKPIT_COMMAND_CONSOLE) {
                return true;
            } else if (checkCockpit != Mech.COCKPIT_STANDARD &&
                    checkCockpit != Mech.COCKPIT_PRIMITIVE &&
                    checkCockpit != Mech.COCKPIT_INDUSTRIAL &&
                    checkCockpit != Mech.COCKPIT_PRIMITIVE_INDUSTRIAL) {
                return true;
            }
        } else if (unitType == UnitType.CONV_FIGHTER ||
                unitType == UnitType.AEROSPACEFIGHTER) {
            if (checkCockpit != Aero.COCKPIT_STANDARD &&
                    checkCockpit != Aero.COCKPIT_PRIMITIVE) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get a BV modifier for a weapon for use against airborne targets
     * @param checkWeapon   weapon to check
     * @return              Relative value from zero (not useful) to 1
     */
    private double getFlakBVModifier(WeaponType checkWeapon) {

        double veryEffective = 1.0;
        double somewhatEffective = 0.5;
        double notEffective = 0.2;
        double ineffective = 0.0;

        // Use a limited version for checking air-to-air capability, including potential for
        // thresholding heavily armored targets
        if (unitType == UnitType.CONV_FIGHTER || unitType == UnitType.AEROSPACEFIGHTER) {
            if (checkWeapon.getAmmoType() == AmmoType.T_AC_LBX ||
                    checkWeapon.getAmmoType() == AmmoType.T_HAG ||
                    checkWeapon.getAmmoType() == AmmoType.T_SBGAUSS) {
                return veryEffective;
            } else if (checkWeapon.getMedAV() >= 10 ||
                    checkWeapon.getShortAV() >= 15) {
                return somewhatEffective;
            } else {
                return ineffective;
            }
        }

        if (checkWeapon instanceof ArtilleryWeapon) {
            return somewhatEffective;
        }
        if (checkWeapon.getAmmoType() == AmmoType.T_AC_LBX ||
                checkWeapon.getAmmoType() == AmmoType.T_HAG ||
                checkWeapon.getAmmoType() == AmmoType.T_SBGAUSS ||
                checkWeapon instanceof InfantrySupportMk2PortableAAWeapon) {
            return veryEffective;
        } else if (checkWeapon instanceof InfantryWeapon ||
                checkWeapon instanceof RLWeapon) {
            return ineffective;
        } else if (checkWeapon.getLongRange() >= 16) {
            return somewhatEffective;
        } else if (checkWeapon.getMediumRange() >= 8) {
            return notEffective;
        }

        return ineffective;
    }

    /**
     * Evaluates weapons for how effective they are against conventional infantry.
     * @param checkWeapon  Weapon to check
     * @return             Relative value, 0 is ineffective, higher is more effective
     */
    private int getAPRating(WeaponType checkWeapon) {
        int extremelyEffective = 6;
        int veryEffective = 4;
        int somewhatEffective = 2;
        int notEffective = 1;
        int ineffective = 0;

        if (checkWeapon instanceof MGWeapon) {
            return veryEffective;
        } else if (checkWeapon instanceof SRMWeapon ||
                checkWeapon instanceof MMLWeapon) {
            return somewhatEffective;
        } else if (checkWeapon instanceof FlamerWeapon) {
            return extremelyEffective;
        }

        if (checkWeapon instanceof InfantryWeapon) {
            return notEffective;
        } else if (checkWeapon instanceof BAMGWeapon) {
            return extremelyEffective;
        } else if (checkWeapon instanceof BAFlamerWeapon) {
            return extremelyEffective;
        } else if (checkWeapon instanceof GaussWeapon) {
            if (checkWeapon instanceof CLAPGaussRifle) {
                return veryEffective;
            } else {
                return ineffective;
            }
        } else if (checkWeapon instanceof CLBAMGBearhunterSuperheavy) {
            return extremelyEffective;
        } else if (checkWeapon instanceof ISPlasmaRifle ||
                checkWeapon instanceof CLPlasmaCannon) {
            return veryEffective;
        } else if (checkWeapon instanceof ISPulseLaserSmall ||
                    checkWeapon instanceof CLPulseLaserSmall) {
            return veryEffective;
        }

        if (checkWeapon instanceof BPodWeapon) {
            return notEffective;
        } else if (checkWeapon instanceof MekMortarWeapon) {
            return notEffective;
        }

        return ineffective;
    }

    /**
     * Get a BV modifier for a weapon for use at long range
     * @param checkWeapon   Weapon to check
     * @return   between zero (not a long ranged weapon) and 1
     */
    private double getLongRangeModifier(WeaponType checkWeapon) {

        double fullRange = 1.0;
        double partialRange = 0.8;
        double minRange = 0.4;
        double shortRange = 0.0;

        if (unitType == UnitType.CONV_FIGHTER || unitType == UnitType.AEROSPACEFIGHTER) {
            if (checkWeapon.getExtAV() > 0 ||
                    checkWeapon.getLongAV() > 0 ||
                    checkWeapon instanceof MMLWeapon ||
                    checkWeapon instanceof ATMWeapon) {
                return fullRange;
            } else {
                return shortRange;
            }
        }

        boolean isIndirect = checkWeapon.hasIndirectFire();
        if (checkWeapon.getLongRange() >= 20 ||
                checkWeapon instanceof ArtilleryWeapon ||
                checkWeapon instanceof MMLWeapon ||
                checkWeapon instanceof ATMWeapon) {
            return fullRange;
        } else if (checkWeapon.getMediumRange() >= 14) {
            if (isIndirect) {
                return fullRange;
            } else {
                return partialRange;
            }
        } else if (checkWeapon.getMediumRange() >= 12) {
            if (isIndirect) {
                return partialRange;
            } else {
                return minRange;
            }
        } else if (isIndirect) {
            return minRange;
        }

        return shortRange;
    }

    /**
     * Get a BV modifier for a weapon for use at short range
     * @param checkWeapon   Weapon to check
     * @return   between zero (not a short ranged weapon) and 1
     */
    private double getShortRangeModifier (WeaponType checkWeapon) {

        double shortRange = 1.0;
        double mediumRange = 0.6;
        double longRange = 0.0;

        if (unitType == UnitType.CONV_FIGHTER || unitType == UnitType.AEROSPACEFIGHTER) {
            if (checkWeapon.getMedAV() == 0 ||
                    checkWeapon instanceof MMLWeapon ||
                    checkWeapon instanceof ATMWeapon) {
                return shortRange;
            } else if (checkWeapon.getLongAV() == 0) {
                return mediumRange;
            } else {
                return longRange;
            }
        }

        if (checkWeapon.getMinimumRange() <= 0)
        {
            if (checkWeapon instanceof InfantryWeapon) {
                if (checkWeapon.getLongRange() <= 6) {
                    return shortRange;
                } else if (checkWeapon.getLongRange() <= 12) {
                    return mediumRange;
                }
            }
            if (checkWeapon.getLongRange() <= 15 ||
                    checkWeapon instanceof MMLWeapon ||
                    checkWeapon instanceof ATMWeapon) {
                return shortRange;
            }
        } else if (checkWeapon.getMinimumRange() <= 3) {
            if (checkWeapon.getLongRange() <= 15) {
                return mediumRange;
            }
        }

        return longRange;
    }

}

