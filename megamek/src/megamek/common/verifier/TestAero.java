/*
 * MegaMek -
 * Copyright (C) 2000-2005 Ben Mazur (bmazur@sev.org)
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
package megamek.common.verifier;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.ArmorType;
import megamek.common.options.OptionsConstants;
import megamek.common.util.StringUtil;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.common.weapons.capitalweapons.ScreenLauncherWeapon;
import megamek.common.weapons.flamers.VehicleFlamerWeapon;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.common.weapons.lasers.CLChemicalLaserWeapon;
import megamek.common.weapons.lrms.LRMWeapon;
import megamek.common.weapons.lrms.LRTWeapon;
import megamek.common.weapons.missiles.MRMWeapon;
import megamek.common.weapons.missiles.RLWeapon;
import megamek.common.weapons.srms.SRMWeapon;
import megamek.common.weapons.srms.SRTWeapon;

import java.util.*;
import java.util.function.Function;

/**
 * Class for testing and validating instantiations for Conventional Fighters and
 * Aerospace Fighters.
 *
 * @author arlith
 * @author Reinhard Vicinus
 */
public class TestAero extends TestEntity {
    private Aero aero;

    /**
     * Filters all fighter armor according to given tech constraints
     *
     * @param techManager
     * @return A list of all armors that meet the tech constraints
     */
    public static List<ArmorType> legalArmorsFor(ITechManager techManager) {
        List<ArmorType> retVal = new ArrayList<>();
        for (ArmorType armor : ArmorType.allArmorTypes()) {
            if (armor.hasFlag(MiscType.F_FIGHTER_EQUIPMENT) && techManager.isLegal(armor)) {
                retVal.add(armor);
            }
        }
        return retVal;
    }

    /**
     * Defines how many spaces each arc has for weapons. Large units can add more by increasing weight
     * of master fire control systems.
     */
    public static int slotsPerArc(Aero aero) {
        if (aero.hasETypeFlag(Entity.ETYPE_WARSHIP)
                || aero.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            return 20;
        } else if (aero.hasETypeFlag(Entity.ETYPE_JUMPSHIP)
                || aero.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
            return 12;
        } else {
            return 5;
        }
    }

    /**
     * @param aero A large craft
     * @return     The maximum number of bay doors. Aerospace units that are not large craft have
     *             a maximum of zero.
     */
    public static int maxBayDoors(Aero aero) {
        if (aero.hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            return 8 + (int) Math.ceil(aero.getWeight() / 100000);
        } else if (aero.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            return 8 + (int) Math.ceil(aero.getWeight() / 75000);
        } else if (aero.hasETypeFlag(Entity.ETYPE_JUMPSHIP)
                || (aero.hasETypeFlag(Entity.ETYPE_DROPSHIP))
                || (aero.hasETypeFlag(Entity.ETYPE_FIXED_WING_SUPPORT))
        ) {
            return 7 + (int) Math.ceil(aero.getWeight() / 50000);
        } else if (aero.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
            return aero.isSpheroid() ? 4 : 2;
        } else {
            return 0;
        }
    }

    public enum Quarters {
        FIRST_CLASS (10, FirstClassQuartersCargoBay.class, size -> new FirstClassQuartersCargoBay(size, 0)),
        STANDARD (7, CrewQuartersCargoBay.class, size -> new CrewQuartersCargoBay(size, 0)),
        SECOND_CLASS (7, SecondClassQuartersCargoBay.class, size -> new SecondClassQuartersCargoBay(size, 0)),
        STEERAGE (5, SteerageQuartersCargoBay.class, size -> new SteerageQuartersCargoBay(size, 0));

        private int tonnage;
        private Class<? extends Bay> bayClass;
        private Function<Integer, Bay> init;

        Quarters(int tonnage, Class<? extends Bay> bayClass, Function<Integer, Bay> init) {
            this.tonnage = tonnage;
            this.bayClass = bayClass;
            this.init = init;
        }

        public int getTonnage() {
            return tonnage;
        }

        public static @Nullable Quarters getQuartersForBay(Bay bay) {
            for (Quarters q : values()) {
                if (bay.getClass() == q.bayClass) {
                    return q;
                }
            }
            return null;
        }

        public Bay newQuarters(int size) {
            return init.apply(size * tonnage);
        }

        public static Map<Quarters, Integer> getQuartersByType(Aero aero) {
            EnumMap<TestAero.Quarters, Integer> sizes = new EnumMap<>(TestAero.Quarters.class);
            for (Quarters q : values()) {
                sizes.put(q, 0);
            }
            for (Bay bay : aero.getTransportBays()) {
                Quarters q = getQuartersForBay(bay);
                if (null != q) {
                    sizes.merge(q, (int) bay.getCapacity(), Integer::sum);
                }
            }
            return sizes;
        }
    }

    /**
     *  Computes the maximum number of armor points for a given Aero
     *  at the given tonnage.
     *
     * @param aero
     * @param tonnage
     * @return
     */
    public static int maxArmorPoints(Entity aero, double tonnage) {
        long eType = aero.getEntityType();
        if (aero.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
            return TestSmallCraft.maxArmorPoints((SmallCraft) aero);
        } else if (aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
                return (int) (tonnage * 1);
        } else if (aero.hasETypeFlag(Entity.ETYPE_AERO)) {
            return (int) (tonnage * 8);
        } else {
            return 0;
        }
    }

    /**
     * Computes the available space for each location in the supplied Aero.
     * Aeros can only have so many weapons in each location, and this available
     * space is reduced by the armor type.
     *
     * @param a  The aero in question
     * @return   Returns an int array, where each element corresponds to a
     *           location and the value is the number of weapons the Aero can
     *           have in that location. Returns null if the space cannot be determined
     *           due to illegal armor type value.
     */
    public static @Nullable int[] availableSpace(Aero a) {
        // Keep track of the max space we have in each arc
        int slots = slotsPerArc(a);
        int[] availSpace = { slots, slots, slots, slots };

        if (!a.hasPatchworkArmor()) {
            // Get the armor type, to determine how much space it uses
            ArmorType armor = ArmorType.of(a.getArmorType(Aero.LOC_NOSE), a.isClanArmor(Aero.LOC_NOSE));
            if (armor == null) {
                return null;
            }
            // Remove space for each location until we've allocated the armor
            int spaceUsedByArmor = armor.getFighterSlots();
            int loc = (spaceUsedByArmor != 2) ? Aero.LOC_AFT : Aero.LOC_RWING;
            while (spaceUsedByArmor > 0) {
                availSpace[loc]--;
                spaceUsedByArmor--;
                loc--;
                if (loc < 0) {
                    loc = Aero.LOC_AFT;
                }
            }
        } else {
            for (int loc = a.firstArmorIndex(); loc < Aero.LOC_WINGS; loc++) {
                ArmorType armor = ArmorType.of(a.getArmorType(loc), a.isClanArmor(loc));
                if (null == armor) {
                    return null;
                } else {
                    availSpace[loc] -= armor.getPatchworkSlotsCVFtr();
                }
            }
        }
        // Blue shield particle field dampener takes one slot in each arc.
        if (a.hasMisc(MiscType.F_BLUE_SHIELD)) {
            for (int i = 0; i < availSpace.length; i++) {
                availSpace[i]--;
            }
        }

        // Large engines take up extra space in the aft in conventional fighters
        if (((a.getEntityType() & Entity.ETYPE_CONV_FIGHTER) != 0)
                && a.hasEngine() && (a.getEngine().hasFlag(Engine.LARGE_ENGINE))) {
            availSpace[Aero.LOC_AFT] -= 1; // same for ICE and fusion
        }
        return availSpace;
    }

    public static boolean usesWeaponSlot(Entity en, EquipmentType eq) {
        if (eq instanceof WeaponType) {
            return !(eq instanceof BayWeapon);
        }
        if (eq instanceof MiscType) {
            // Equipment that takes up a slot on fighters and small craft, but not large craft.
            if (!en.hasETypeFlag(Entity.ETYPE_DROPSHIP) && !en.hasETypeFlag(Entity.ETYPE_JUMPSHIP)
                    && (eq.hasFlag(MiscType.F_BAP)
                            || eq.hasFlag(MiscType.F_WATCHDOG)
                            || eq.hasFlag(MiscType.F_ECM)
                            || eq.hasFlag(MiscType.F_ANGEL_ECM)
                            || eq.hasFlag(MiscType.F_EW_EQUIPMENT)
                            || eq.hasFlag(MiscType.F_BOOBY_TRAP)
                            || eq.hasFlag(MiscType.F_SENSOR_DISPENSER))) {
                return true;

            }
            // Equipment that takes a slot on all aerospace units
            return  eq.hasFlag(MiscType.F_CHAFF_POD)
                    || eq.hasFlag(MiscType.F_SPACE_MINE_DISPENSER)
                    || eq.hasFlag(MiscType.F_MOBILE_HPG)
                    || eq.hasFlag(MiscType.F_RECON_CAMERA)
                    || eq.hasFlag(MiscType.F_HIRES_IMAGER)
                    || eq.hasFlag(MiscType.F_HYPERSPECTRAL_IMAGER)
                    || eq.hasFlag(MiscType.F_INFRARED_IMAGER)
                    || eq.hasFlag(MiscType.F_LOOKDOWN_RADAR);
        }
        return false;
    }

    /**
     * Computes the engine rating for the given entity type.
     *
     * @param unit
     * @param tonnage
     * @param desiredSafeThrust
     * @return
     */
    public static int calculateEngineRating(Aero unit, int tonnage, int desiredSafeThrust) {
        int rating;
        if (unit.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            rating = (tonnage * desiredSafeThrust);
        } else if (unit.hasETypeFlag(Entity.ETYPE_AEROSPACEFIGHTER)) {
            rating = (tonnage * (desiredSafeThrust - 2));
        } else {
            rating = 0;
        }

        if (unit.isPrimitive()) {
            double dRating = rating;
            dRating *= 1.2;
            if ((dRating % 5) != 0) {
                dRating = (dRating - (dRating % 5)) + 5;
            }
            rating = (int) dRating;
        }
        return rating;
    }

    public static int weightFreeHeatSinks(final Aero aero) {
        if (aero.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
            return TestSmallCraft.weightFreeHeatSinks((SmallCraft) aero);
        } else if (aero.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
            return TestAdvancedAerospace.weightFreeHeatSinks((Jumpship) aero);
        } else if (aero.hasEngine()) {
            return aero.getEngine().getWeightFreeEngineHeatSinks();
        } else {
            return 0;
        }
    }

    /**
     * @return the number of days the unit can spend accelerating at 1G
     */
    public static double calculateDaysAt1G(final Aero aero) {
        final double strategicFuelUse = aero.getStrategicFuelUse();
        return (strategicFuelUse > 0) ? aero.getFuelTonnage() / aero.getStrategicFuelUse() : 0d;
    }

    /**
     * @return the number of days the unit can spend accelerating at maximum thrust.
     */
    public static double calculateDaysAtMax(final Aero aero) {
        if (aero.getStrategicFuelUse() > 0) {
            double maxMP = aero.getRunMP();
            // check for station-keeping drive
            if (maxMP == 0) {
                maxMP = 0.2;
            }
            return aero.getFuelTonnage() / (aero.getStrategicFuelUse() * maxMP / 2.0);
        } else {
            return 0.0;
        }
    }

    public TestAero(Aero a, TestEntityOption option, String fs) {
        super(option, a.getEngine(), getStructure(a));
        aero = a;
        fileString = fs;
    }

    private static Structure getStructure(Aero aero) {
        int type = aero.getStructureType();
        return new Structure(type, false, aero.getMovementMode());
    }

    @Override
    public Entity getEntity() {
        return aero;
    }

    @Override
    public boolean isTank() {
        return false;
    }

    @Override
    public boolean isMech() {
        return false;
    }

    @Override
    public boolean isAero() {
        return true;
    }

    @Override
    public boolean isSmallCraft() {
        return false;
    }

    @Override
    public boolean isAdvancedAerospace() {
        return false;
    }

    @Override
    public boolean isProtomech() {
        return false;
    }

    @Override
    public double getWeightMisc() {
        // VSTOL equipment weighs extra for conventional fighters
        if ((aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) && aero.isVSTOL()) {
            // Weight = tonnage * 0.05 rounded up to nearest half ton
            return Math.ceil(0.05 * aero.getWeight() * 2) / 2.0;
        }
        return 0.0f;
    }

    @Override
    public double getWeightPowerAmp() {
        // Conventional Fighters with ICE engines may need a power amp
        if ((aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) && aero.hasEngine()
                && (aero.getEngine().getEngineType() == Engine.COMBUSTION_ENGINE)) {
            double weight = 0;
            for (Mounted m : aero.getWeaponList()) {
                WeaponType wt = (WeaponType) m.getType();
                if (wt.hasFlag(WeaponType.F_ENERGY) &&
                        !(wt instanceof CLChemicalLaserWeapon) &&
                        !(wt instanceof VehicleFlamerWeapon)) {
                    weight += m.getTonnage();
                }
                Mounted linkedBy = m.getLinkedBy();
                if ((linkedBy != null) &&
                        (linkedBy.getType() instanceof MiscType) &&
                        linkedBy.getType().hasFlag(MiscType.F_PPC_CAPACITOR)) {
                    weight += linkedBy.getTonnage();
                }
            }
            // Power amp weighs:
            //   energy weapon tonnage * 0.1 rounded to nearest half ton
            return Math.ceil(0.1 * weight * 2) / 2.0;
        }
        return 0;
    }

    @Override
    public double getWeightEngine() {
        double wt = super.getWeightEngine();
        // Conventional fighters with fusion engines require extra shielding.
        // Per TacOps fission engines require extra shielding as well.
        if (getEntity().hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)
                && (null != getEntity().getEngine())
                && (getEntity().getEngine().isFusion() || getEntity().getEngine().hasFlag(Engine.FISSION))) {
            wt = ceil(wt * 1.5, Ceil.HALFTON);
        }
        return wt;
    }

    @Override
    public double getWeightControls() {
        // Controls for Aerospace Fighters and Conventional Fighters consists
        //  of the cockpit and the fuel
        double weight;
        if (aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            // Weight = tonnage * 0.1 rounded to nearest half ton
            weight = Math.round(0.1 * aero.getWeight()*2) / 2.0;
        } else {
            weight = 3.0;
            if (aero.getCockpitType() == Aero.COCKPIT_SMALL) {
                weight = 2.0;
        } else if (aero.getCockpitType() == Aero.COCKPIT_COMMAND_CONSOLE) {
                weight = 6.0;
            } else if (aero.getCockpitType() == Aero.COCKPIT_PRIMITIVE) {
                weight = 5.0;
            }
        }
        return weight;
    }

    public double getWeightFuel() {
        return aero.getFuelTonnage();
    }

    @Override
    public int getCountHeatSinks() {
        if (aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            return heatNeutralHSRequirement();
        }
        return aero.getHeatSinks();
    }

    @Override
    public double getWeightHeatSinks() {
        if (aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            int required = heatNeutralHSRequirement();
            return Math.max(0, required - engine.getWeightFreeEngineHeatSinks());
        } else {
            return Math.max(getCountHeatSinks() - engine.getWeightFreeEngineHeatSinks(), 0);
        }
    }

    @Override
    public boolean hasDoubleHeatSinks() {
        return aero.getHeatType() == Aero.HEAT_DOUBLE;
    }

    @Override
    public String printWeightMisc() {
        double weight = getWeightMisc();
        if (weight > 0) {
            StringBuffer retVal = new StringBuffer(StringUtil.makeLength(
                    "VSTOL equipment:", getPrintSize() - 5));
            retVal.append(makeWeightString(weight));
            retVal.append("\n");
            return retVal.toString();
        }
        return "";
    }

    @Override
    public String printWeightControls() {
        return StringUtil.makeLength(aero.getCockpitTypeString() + ":", getPrintSize() - 5)
                + makeWeightString(getWeightControls()) + "\n";
    }

    public String printWeightFuel() {
        return StringUtil.makeLength("Fuel: ", getPrintSize() - 5)
                + makeWeightString(getWeightFuel()) + "\n";
    }

    public Aero getAero() {
        return aero;
    }

    public String printArmorLocProp(int loc, int wert) {
        return " is greater than " + wert + "!";
    }

    /**
     * Checks to see if this unit has valid armor assignment.
     *
     * @param buff
     * @return
     */
    public boolean correctArmor(StringBuffer buff) {
        boolean correct = true;
        int maxArmorPoints = maxArmorPoints(aero, aero.getWeight());
        int armorTotal = 0;
        for (int loc = 0; loc < aero.locations(); loc++) {
            if (aero.getOArmor(loc) > maxArmorPoints) {
                buff.append(printArmorLocation(loc)).append(printArmorLocProp(loc, maxArmorPoints)).append("\n");
                correct = false;
            }
            armorTotal += aero.getOArmor(loc);
        }
        if (armorTotal > maxArmorPoints) {
            buff.append("Total armor," + armorTotal +
                    ", is greater than the maximum: " + maxArmorPoints + "\n");
            correct = false;
        }

        return correct ;
    }

    /**
     * Checks that Conventional fighters only have a standard cockpit and that
     * Aerospace fighters have a valid cockpit (standard, small, primitive,
     * command console).
     *
     * @param buff
     * @return
     */
    public boolean correctControlSystems(StringBuffer buff) {
        if ((aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) &&
                aero.getCockpitType() != Aero.COCKPIT_STANDARD) {
            buff.append(
                    "Conventional fighters may only have standard cockpits!");
            return false;
        } else if (aero.getCockpitType() < Aero.COCKPIT_STANDARD ||
                aero.getCockpitType() > Aero.COCKPIT_PRIMITIVE) {
            buff.append(
                    "Invalid cockpit type!");
            return false;
        }
        return true;
    }

    public List<Mounted> checkCriticalSlotsForEquipment(Entity entity) {
        List<Mounted> unallocated = new ArrayList<>();
        for (Mounted m : entity.getEquipment()) {
            if ((m.getLocation() == Entity.LOC_NONE) && !m.isOneShotAmmo() && (m.getCriticals() > 0)) {
                unallocated.add(m);
            }
        }
        return unallocated;
    }

    /**
     * For Aerospace and Conventional fighters the only thing we need to ensure
     * is that they do not mount more weapons in each arc then allowed.  They
     * have boundless space for equipment.  Certain armor types reduce the
     * number of spaces available in each arc.
     *
     * @param buff  A buffer for error messages
     * @return  True if the mounted weapons are valid, else false
     */
    public boolean correctCriticals(StringBuffer buff) {
        boolean correct = true;

        List<Mounted> unallocated = checkCriticalSlotsForEquipment(aero);
        if (!unallocated.isEmpty()) {
            buff.append("Unallocated Equipment:\n");
            for (Mounted mount : unallocated) {
                buff.append(mount.getType().getInternalName()).append("\n");
            }
            correct = false;
        }
        int[] numWeapons = new int[aero.locations()];
        int numBombs = 0;

        for (Mounted m : aero.getWeaponList()) {
            if (m.getLocation() == Entity.LOC_NONE) {
                continue;
            }

            // Aeros can't use special munitions except for artemis, exceptions
            //  LBX's must use clusters
            WeaponType wt = (WeaponType) m.getType();
            boolean canHaveSpecialMunitions =
                    ((wt.getAmmoType() == AmmoType.T_MML)
                    || (wt.getAmmoType() == AmmoType.T_ATM)
                    || (wt.getAmmoType() == AmmoType.T_NARC));
            if (wt.getAmmoType() != AmmoType.T_NA
                    && m.getLinked() != null
                    && !canHaveSpecialMunitions) {
                EquipmentType linkedType = m.getLinked().getType();
                boolean hasArtemisFCS = m.getLinkedBy() != null
                        && (m.getLinkedBy().getType().hasFlag(MiscType.F_ARTEMIS)
                        || m.getLinkedBy().getType().hasFlag(MiscType.F_ARTEMIS_PROTO)
                        || m.getLinkedBy().getType().hasFlag(MiscType.F_ARTEMIS_V));
                if (linkedType instanceof AmmoType) {
                    AmmoType linkedAT = (AmmoType) linkedType;
                    // Check LBX's
                    if (wt.getAmmoType() == AmmoType.T_AC_LBX &&
                            !linkedAT.getMunitionType().contains(AmmoType.Munitions.M_CLUSTER)) {
                        correct = false;
                        buff.append("Aeros must use cluster munitions!").append(m.getType().getInternalName())
                                .append(" is using ").append(linkedAT.getInternalName()).append("\n");
                    }
                    // Allow Artemis munitions for artemis-linked launchers
                    if (hasArtemisFCS
                            && !linkedAT.getMunitionType().contains(AmmoType.Munitions.M_STANDARD)
                            && !linkedAT.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_CAPABLE)
                            && !linkedAT.getMunitionType().contains(AmmoType.Munitions.M_ARTEMIS_V_CAPABLE)) {
                        correct = false;
                        buff.append("Aero using illegal special missile type!").append(m.getType().getInternalName())
                                .append(" is using ").append(linkedAT.getInternalName()).append("\n");
                    }
                    if (!linkedAT.getMunitionType().contains(AmmoType.Munitions.M_STANDARD)
                            && !hasArtemisFCS
                            && wt.getAmmoType() != AmmoType.T_AC_LBX
                            && wt.getAmmoType() != AmmoType.T_SBGAUSS) {
                        correct = false;
                        buff.append("Aeros may not use special munitions! ").append(m.getType().getInternalName())
                                .append(" is using ").append(linkedAT.getInternalName()).append("\n");
                    }

                }
            }

            if (m.getType().hasFlag(AmmoType.F_SPACE_BOMB)
                    || m.getType().hasFlag(AmmoType.F_GROUND_BOMB)
                    || m.getType().hasFlag(WeaponType.F_DIVE_BOMB)
                    || m.getType().hasFlag(WeaponType.F_ALT_BOMB)
                    || m.getType().hasFlag(WeaponType.F_SPACE_BOMB)) {
                numBombs++;
            } else {
                numWeapons[m.getLocation()]++;
            }
        }

        if (aero.isFighter()) {
            int[] availSpace = availableSpace(aero);
            if (availSpace == null) {
                buff.append("Invalid armor type! Armor: ")
                        .append(ArmorType.forEntity(aero))
                        .append("\n");
                return false;
            }
            if (numBombs > aero.getMaxBombPoints()) {
                buff.append("Invalid number of bombs! Unit can mount ").append(aero.getMaxBombPoints())
                        .append(" but ").append(numBombs).append("are present!");
                buff.append("\n");
                return false;
            }

            String[] locNames = aero.getLocationNames();
            int loc = Aero.LOC_AFT;
            while (loc >= 0) {
                correct &= !(numWeapons[loc] > availSpace[loc]);
                if (numWeapons[loc] > availSpace[loc]) {
                    buff.append(locNames[loc]).append(" has ").append(numWeapons[loc])
                            .append(" weapons but it can only fit ").append(availSpace[loc]).append(" weapons!");
                    buff.append("\n");
                }
                loc--;
            }
        }

        return correct;
    }


    /**
     * Checks that the heatsink assignment is legal.  Conventional fighters must
     * have enough heatsinks to dissipate heat from all of their energy weapons
     * and they may only mount standard heatsinks.
     * Aerospace fighters must have at least 10 heatsinks.
     *
     * @param buff
     * @return
     */
    public boolean correctHeatSinks(StringBuffer buff) {
        if ((aero.getHeatType() != Aero.HEAT_SINGLE)
                && (aero.getHeatType() != Aero.HEAT_DOUBLE)) {
            buff.append("Invalid heatsink type!  Valid types are "
                    + Aero.HEAT_SINGLE + " and " + Aero.HEAT_DOUBLE
                    + ".  Found " + aero.getHeatType() + ".");
            return false;
        }
        return true;
    }

    @Override
    public boolean correctEntity(StringBuffer buff) {
        return correctEntity(buff, aero.getTechLevel());
    }

    @Override
    public boolean correctEntity(StringBuffer buff, int ammoTechLvl) {
        boolean correct = true;

        // We only support Conventional Fighters and ASF
        if (!aero.isFighter()) {
            System.out.println("TestAero only supports Aerospace Fighters " +
                    "and Conventional fighters.  Supplied unit was a " +
                    Entity.getEntityTypeName(aero.getEntityType()));
            return true;
        }

        if (skip()) {
            return true;
        }
        if (!correctWeight(buff)) {
            buff.insert(0, printTechLevel() + printShortMovement());
            buff.append(printWeightCalculation());
            correct = false;
        }
        if (!engine.engineValid) {
            buff.append(engine.problem.toString()).append("\n\n");
            correct = false;
        }
        if ((getCountHeatSinks() < engine.getWeightFreeEngineHeatSinks())
                && !aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            buff.append("Heat Sinks:\n");
            buff.append(" Engine    "
                    + engine.integralHeatSinkCapacity(false) + "\n");
            buff.append(" Total     " + getCountHeatSinks() + "\n");
            buff.append(" Required  " + engine.getWeightFreeEngineHeatSinks()
                    + "\n");
            correct = false;
        }

        if (showCorrectArmor() && !correctArmor(buff)) {
            correct = false;
        }
        if (showCorrectCritical() && !correctCriticals(buff)) {
            correct = false;
        }
        if (showFailedEquip() && hasFailedEquipment(buff)) {
            correct = false;
        }
        if (showIncorrectIntroYear() && hasIncorrectIntroYear(buff)) {
            correct = false;
        }

        correct &= correctControlSystems(buff);
        correct &= !hasIllegalTechLevels(buff, ammoTechLvl);
        correct &= !hasIllegalEquipmentCombinations(buff);
        correct &= !hasMismatchedLateralWeapons(buff);
        correct &= correctHeatSinks(buff);

        return correct;
    }

    /**
     * Checks that the weapon loads in the wings match each other.
     * @param buff The buffer that contains the collected error messages.
     * @return     Whether the lateral weapons are mismatched.
     */
    public boolean hasMismatchedLateralWeapons(StringBuffer buff) {
        boolean illegal = false;
        Map<EquipmentType,Integer> leftWing = new HashMap<>();
        Map<EquipmentType,Integer> rightWing = new HashMap<>();
        Map<EquipmentType,Integer> leftWingRear = new HashMap<>();
        Map<EquipmentType,Integer> rightWingRear = new HashMap<>();
        for (Mounted m : aero.getEquipment()) {
            if (m.getType() instanceof WeaponType) {
                if (m.getLocation() == Aero.LOC_LWING) {
                    if (m.isRearMounted()) {
                        leftWingRear.merge(m.getType(), 1, Integer::sum);
                    } else {
                        leftWing.merge(m.getType(), 1, Integer::sum);
                    }
                } else if (m.getLocation() == SmallCraft.LOC_RWING) {
                    if (m.isRearMounted()) {
                        rightWingRear.merge(m.getType(), 1, Integer::sum);
                    } else {
                        rightWing.merge(m.getType(), 1, Integer::sum);
                    }
                }
            }
        }
        boolean lateralMatch = true;
        for (EquipmentType eq : leftWing.keySet()) {
            if (!rightWing.containsKey(eq) || !leftWing.get(eq).equals(rightWing.get(eq))) {
                lateralMatch = false;
                break;
            }
        }
        if (lateralMatch) {
            //We've already checked counts, so in the reverse direction we only need to see if there's
            // anything not found on the other side.
            for (EquipmentType eq : rightWing.keySet()) {
                if (!leftWing.containsKey(eq)) {
                    lateralMatch = false;
                    break;
                }
            }
        }
        if (lateralMatch) {
            for (EquipmentType eq : leftWingRear.keySet()) {
                if (!rightWingRear.containsKey(eq) || !leftWingRear.get(eq).equals(rightWingRear.get(eq))) {
                    lateralMatch = false;
                    break;
                }
            }
        }
        if (lateralMatch) {
            for (EquipmentType eq : rightWingRear.keySet()) {
                if (!leftWingRear.containsKey(eq)) {
                    lateralMatch = false;
                    break;
                }
            }
        }
        if (!lateralMatch) {
            buff.append("Left and right side weapon loads do not match.\n");
            illegal = true;
        }
        return illegal;
    }

    /**
     * @param eq        The equipment
     * @param location  A location index on the Entity
     * @param buffer    If non-null and the location is invalid, will be appended with an explanation
     * @return          Whether the equipment can be mounted in the location on the aerospace fighter,
     *                  conventional fighter, or fixed wing support vehicle
     */
    public static boolean isValidAeroLocation(EquipmentType eq, int location, @Nullable StringBuffer buffer) {
        if (buffer == null) {
            buffer = new StringBuffer();
        }
        if (eq instanceof AmmoType) {
            if (location != Aero.LOC_FUSELAGE) {
                buffer.append(eq.getName()).append(" must be mounted in the fuselage.\n");
                return false;
            }
        } else if (eq instanceof MiscType) {
            // Weapon enhancements go in the same location as the weapon
            if ((eq.hasFlag(MiscType.F_ARTEMIS)
                    || eq.hasFlag(MiscType.F_ARTEMIS_V)
                    || eq.hasFlag(MiscType.F_ARTEMIS_PROTO)
                    || eq.hasFlag(MiscType.F_APOLLO)
                    || eq.hasFlag(MiscType.F_PPC_CAPACITOR)
                    || eq.hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) && (location >= Aero.LOC_WINGS)) {
                if (location != Aero.LOC_FUSELAGE) {
                    buffer.append(eq.getName()).append(" must be mounted in a location with a firing arc.\n");
                    return false;
                }
            } else if ((eq.hasFlag(MiscType.F_BLUE_SHIELD) || eq.hasFlag(MiscType.F_LIFTHOIST)
                    || (eq.hasFlag(MiscType.F_CASE) && !eq.isClan())) && (location != Aero.LOC_FUSELAGE)) {
                buffer.append(eq.getName()).append(" must be mounted in the fuselage.\n");
                return false;
            }
        } else if (eq instanceof WeaponType) {
            if ((((WeaponType) eq).getAmmoType() == AmmoType.T_GAUSS_HEAVY)
                    && (location != Aero.LOC_NOSE) && (location != Aero.LOC_AFT)) {
                buffer.append(eq.getName()).append(" must be mounted in the nose or aft.\n");
                return false;
            }
            if (!eq.hasFlag(WeaponType.F_C3M) && !eq.hasFlag(WeaponType.F_C3MBS)
                    && !eq.hasFlag(WeaponType.F_TAG) && (location == Aero.LOC_FUSELAGE)) {
                buffer.append(eq.getName()).append(" must be mounted in a location with a firing arc.\n");
                return false;
            }
        }
        return true;
    }

    public boolean isAeroWeapon(EquipmentType eq, Entity en) {
        if (eq instanceof InfantryWeapon) {
            return false;
        }

        WeaponType weapon = (WeaponType) eq;

        // small craft only; lacks aero weapon flag
        if (weapon.getAmmoType() == AmmoType.T_C3_REMOTE_SENSOR) {
            return en.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)
                    && !en.hasETypeFlag(Entity.ETYPE_DROPSHIP);
        }

        if (weapon.hasFlag(WeaponType.F_ARTILLERY) && !weapon.hasFlag(WeaponType.F_BA_WEAPON)) {
            return (weapon.getAmmoType() == AmmoType.T_ARROW_IV)
                    || en.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)
                    || en.hasETypeFlag(Entity.ETYPE_JUMPSHIP);
        }

        if (weapon.isSubCapital() || (weapon.isCapital() && (weapon.hasFlag(WeaponType.F_MISSILE)))
                || (weapon.getAtClass() == WeaponType.CLASS_SCREEN)) {
            return en.hasETypeFlag(Entity.ETYPE_DROPSHIP)
                    || en.hasETypeFlag(Entity.ETYPE_JUMPSHIP);
        }

        if (weapon.hasFlag(WeaponType.F_VGL)) {
            return !en.hasETypeFlag(Entity.ETYPE_JUMPSHIP);
        }

        if (weapon.isCapital()) {
            return en.hasETypeFlag(Entity.ETYPE_JUMPSHIP);
        }

        if (weapon instanceof BayWeapon) {
            return en.usesWeaponBays();
        }

        if (!weapon.hasFlag(WeaponType.F_AERO_WEAPON)) {
            return false;
        }

        if (((weapon instanceof LRMWeapon) || (weapon instanceof LRTWeapon))
                && (weapon.getRackSize() != 5)
                && (weapon.getRackSize() != 10)
                && (weapon.getRackSize() != 15)
                && (weapon.getRackSize() != 20)) {
            return false;
        }
        if (((weapon instanceof SRMWeapon) || (weapon instanceof SRTWeapon))
                && (weapon.getRackSize() != 2)
                && (weapon.getRackSize() != 4)
                && (weapon.getRackSize() != 6)) {
            return false;
        }
        if ((weapon instanceof MRMWeapon) && (weapon.getRackSize() < 10)) {
            return false;
        }

        if ((weapon instanceof RLWeapon) && (weapon.getRackSize() < 10)) {
            return false;
        }

        if (weapon.hasFlag(WeaponType.F_ENERGY)
                || (weapon.hasFlag(WeaponType.F_PLASMA) && (weapon
                        .getAmmoType() == AmmoType.T_PLASMA))) {

            if (weapon.hasFlag(WeaponType.F_ENERGY)
                    && weapon.hasFlag(WeaponType.F_PLASMA)
                    && (weapon.getAmmoType() == AmmoType.T_NA)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public StringBuffer printEntity() {
        StringBuffer buff = new StringBuffer();
        buff.append("Aero: ").append(aero.getDisplayName()).append("\n");
        buff.append("Found in: ").append(fileString).append("\n");
        buff.append(printTechLevel());
        buff.append("Intro year: ").append(aero.getYear()).append("\n");
        buff.append(printSource());
        buff.append(printShortMovement());
        if (correctWeight(buff, true, true)) {
            buff.append("Weight: ").append(getWeight()).append(" (")
                    .append(calculateWeight()).append(")\n");
        }
        buff.append(printWeightCalculation()).append("\n");
        buff.append(printArmorPlacement());
        correctArmor(buff);
        buff.append(printLocations());
        correctCriticals(buff);

        // printArmor(buff);
        printFailedEquipment(buff);
        return buff;
    }

    @Override
    public double calculateWeightExact() {
        double weight = 0;
        weight += getWeightEngine();
        weight += getWeightControls();
        weight += getWeightFuel();
        weight += getWeightHeatSinks();
        weight += getWeightArmor();
        weight += getWeightMisc();

        weight += getWeightMiscEquip();
        weight += getWeightWeapon();
        weight += getWeightAmmo();
        weight += getWeightPowerAmp();

        weight += getWeightCarryingSpace();

        weight += getArmoredComponentWeight();
        return weight;
    }

    @Override
    public String printWeightCalculation() {
        return printWeightEngine()
                + printWeightControls() + printWeightFuel()
                + printWeightHeatSinks()
                + printWeightArmor() + printWeightMisc()
                + printWeightCarryingSpace() + "Equipment:\n"
                + printMiscEquip() + printWeapon() + printAmmo();
    }

    @Override
    public double getWeightMiscEquip() {
        double weightSum = super.getWeightMiscEquip();
        for (Mounted m : getEntity().getMisc()) {
            MiscType mt = (MiscType) m.getType();
            if (mt.hasFlag(MiscType.F_CARGO) && aero.hasQuirk(OptionsConstants.QUIRK_POS_INTERNAL_BOMB)){
                // This equipment will get counted as a cargo bay later, for IBB compatibility.
                weightSum -= m.getTonnage();
            }
        }
        return weightSum;
    }

    @Override
    public String printLocations() {
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < getEntity().locations(); i++) {
            String locationName = getEntity().getLocationName(i);
            buff.append(locationName + ":");
            buff.append("\n");
            for (int j = 0; j < getEntity().getNumberOfCriticals(i); j++) {
                CriticalSlot slot = getEntity().getCritical(i, j);
                if (slot == null) {
                    j = getEntity().getNumberOfCriticals(i);
                } else if (slot.getType() == CriticalSlot.TYPE_SYSTEM) {
                        buff.append(j).append(". UNKNOWN SYSTEM NAME").append("\n");
                } else if (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    EquipmentType e = getEntity().getEquipmentType(slot);
                    buff.append(j).append(". ").append(e.getInternalName()).append("\n");
                }
            }
        }
        return buff.toString();
    }

    public double getWeightQuarters() {
        double quartersWeight = 0;
        for (Bay bay : getEntity().getTransportBays()) {
            if (bay.isQuarters()) {
                quartersWeight += bay.getWeight();
            }
        }
        return quartersWeight;
    }

    public String printWeightQuarters() {
        double weight = 0.0;
        for (Bay bay : aero.getTransportBays()) {
            if (bay.isQuarters()) {
                weight += bay.getWeight();
            }
        }
        if (weight > 0) {
            return StringUtil.makeLength("Crew quarters: ", getPrintSize() - 5) + weight + "\n";
        }
        return "";
    }

    @Override
    public String getName() {
        if (aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            return "Conventional Fighter: " + aero.getDisplayName();
        } else {
            return "Aerospace Fighter: " + aero.getDisplayName();
        }
    }

    /**
     * Calculate the structural integrity weight
     */
    @Override
    public double getWeightStructure() {
        double tonnage = 0;
        if (aero.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
            tonnage = aero.getSI() * aero.getWeight();
            if (aero.isSpheroid()) {
                tonnage /= 500;
            } else {
                tonnage /= 200;
            }
        } else if (aero.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            tonnage = aero.getWeight() / 100;
        } else if (aero.hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            // SI * weight / 1000, rounded up to half ton
            tonnage = aero.getSI() * aero.getWeight() / 1000;
        } else if (aero.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
            tonnage = aero.getWeight() / 150;
        } else {
            // Fighters do not allocate weight to structure
            return 0;
        }
        return Math.ceil(tonnage * 2) / 2.0;
    }

    /**
     * Get the maximum tonnage for the type of unit
     *
     * @param aero      The unit
     * @param faction   An ITechnology faction constant used for primitive jumpships. A value
     *                  of F_NONE will use the least restrictive values (TA/TH).
     * @return          The maximum tonnage for the type of unit.
     */
    public static int getMaxTonnage(Aero aero, int faction) {
        if (aero.hasETypeFlag(Entity.ETYPE_SPACE_STATION)) {
            return 2500000;
        } else if (aero.hasETypeFlag(Entity.ETYPE_WARSHIP)) {
            if (((Jumpship) aero).getDriveCoreType() == Jumpship.DRIVE_CORE_SUBCOMPACT) {
                return 25000;
            }
            return 2500000;
        } else if (aero.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
            if (aero.isPrimitive()) {
                return getPrimitiveJumpshipMaxTonnage(aero, faction);
            }
            return 500000;
        } else if (aero.hasETypeFlag(Entity.ETYPE_DROPSHIP)) {
            if (aero.isPrimitive()) {
                return getPrimitiveDropshipMaxTonnage(aero);
            }
            return aero.isSpheroid() ? 100000 : 35000;
        } else if (aero.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)
                || aero.hasETypeFlag(Entity.ETYPE_FIXED_WING_SUPPORT)) {
            return 200;
        } else if (aero.hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            return 50;
        } else {
            return 100;
        }
    }

    /**
     * @param jumpship
     * @return Max tonnage allowed by construction rules.
     */
    public static int getPrimitiveJumpshipMaxTonnage(Aero jumpship, int faction) {
        switch (faction) {
            case ITechnology.F_TA:
            case ITechnology.F_TH:
            case ITechnology.F_NONE:
                if (jumpship.getYear() < 2130) {
                    return 100000;
                } else if (jumpship.getYear() < 2150) {
                    return 150000;
                } else if (jumpship.getYear() < 2165) {
                    return 200000;
                } else if (jumpship.getYear() < 2175) {
                    return 250000;
                } else if (jumpship.getYear() < 2200) {
                    return 350000;
                } else if (jumpship.getYear() < 2300) {
                    return 500000;
                } else if (jumpship.getYear() < 2350) {
                    return 1000000;
                } else if (jumpship.getYear() < 2400) {
                    return 1600000;
                } else {
                    return 1800000;
                }
            case ITechnology.F_CC:
            case ITechnology.F_DC:
            case ITechnology.F_FS:
            case ITechnology.F_FW:
            case ITechnology.F_LC:
                if (jumpship.getYear() < 2300) {
                    return 350000;
                } else if (jumpship.getYear() < 2350) {
                    return 600000;
                } else if (jumpship.getYear() < 2400) {
                    return 800000;
                } else {
                    return 1000000;
                }
            default:
                if (jumpship.getYear() < 2300) {
                    return 300000;
                } else if (jumpship.getYear() < 2350) {
                    return 450000;
                } else if (jumpship.getYear() < 2400) {
                    return 600000;
                } else {
                    return 1000000;
                }
        }
    }

    public static int getPrimitiveDropshipMaxTonnage(Aero dropship) {
        if (dropship.getYear() < 2130) {
            return dropship.isSpheroid() ? 3000 : 1000;
        } else if (dropship.getYear() < 2150) {
            return dropship.isSpheroid() ? 4000 : 1500;
        } else if (dropship.getYear() < 2165) {
            return dropship.isSpheroid() ? 7000 : 2500;
        } else if (dropship.getYear() < 2175) {
            return dropship.isSpheroid() ? 10000 : 3000;
        } else if (dropship.getYear() < 2200) {
            return dropship.isSpheroid() ? 14000 : 5000;
        } else if (dropship.getYear() < 2250) {
            return dropship.isSpheroid() ? 15000 : 6000;
        } else if (dropship.getYear() < 2300) {
            return dropship.isSpheroid() ? 19000 : 7000;
        } else if (dropship.getYear() < 2350) {
            return dropship.isSpheroid() ? 23000 : 8000;
        } else if (dropship.getYear() < 2425) {
            return dropship.isSpheroid() ? 30000 : 10000;
        } else {
            return dropship.isSpheroid() ? 50000 : 20000;
        }
    }

    /**
     * @return Minimum crew requirements based on unit type and equipment crew requirements.
     */
    public static int minimumBaseCrew(Aero aero) {
        if (aero.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
            return TestSmallCraft.minimumBaseCrew((SmallCraft) aero);
        } else if (aero.hasETypeFlag(Entity.ETYPE_JUMPSHIP)) {
            return TestAdvancedAerospace.minimumBaseCrew((Jumpship) aero);
        } else {
            return 1;
        }
    }

    /**
     * One gunner is required for each capital weapon and each six standard scale weapons, rounding up
     * @return The vessel's minimum gunner requirements.
     */
    public static int requiredGunners(Aero aero) {
        if (!aero.isLargeCraft() && !aero.hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
            return 0;
        }
        int capitalWeapons = 0;
        int stdWeapons = 0;
        for (Mounted m : aero.getTotalWeaponList()) {
            if (m.getType() instanceof BayWeapon) {
                continue;
            }
            if ((((WeaponType) m.getType()).getLongRange() <= 1)
                    // MML range depends on ammo, and getLongRange() returns 0
                    && (((WeaponType) m.getType()).getAmmoType() != AmmoType.T_MML)) {
                continue;
            }
            if (((WeaponType) m.getType()).isCapital()
                    || (m.getType() instanceof ScreenLauncherWeapon)) {
                capitalWeapons++;
            } else {
                stdWeapons++;
            }
        }
        return capitalWeapons + (int) Math.ceil(stdWeapons / 6.0);
    }

    /**
     * Determines whether a piece of equipment should be mounted in a specific location, as opposed
     * to the fuselage.
     *
     * @param eq       The equipment
     * @param fighter  If the aero is a fighter (including fixed wing support), the ammo is mounted in the
     *                 fuselage. Otherwise, it's in the location with the weapon.
     * @return         Whether the equipment needs to be assigned to a location with a firing arc.
     */
    public static boolean eqRequiresLocation(EquipmentType eq, boolean fighter) {
        if (!fighter) {
            return (eq instanceof WeaponType)
                    || (eq instanceof AmmoType)
                    || ((eq instanceof MiscType)
                            && (eq.hasFlag(MiscType.F_ARTEMIS)
                                    || eq.hasFlag(MiscType.F_ARTEMIS_PROTO)
                                    || eq.hasFlag(MiscType.F_ARTEMIS_V)
                                    || eq.hasFlag(MiscType.F_APOLLO)
                                    || eq.hasFlag(MiscType.F_PPC_CAPACITOR)
                                    || eq.hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)
                                    || eq.hasFlag(MiscType.F_LASER_INSULATOR)));
        } else if (eq instanceof MiscType) {
            if (eq.hasFlag(MiscType.F_CASE)) {
                return eq.isClan();
            } else {
                return !eq.hasFlag(MiscType.F_BLUE_SHIELD) && !eq.hasFlag(MiscType.F_LIFTHOIST);
            }
        } else {
            return !(eq instanceof AmmoType);
        }
    }
}
