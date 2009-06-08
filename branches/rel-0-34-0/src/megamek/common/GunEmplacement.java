/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *           Copyright (C) 2005 Mike Gratton <mike@vee.net>
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

package megamek.common;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

/**
 * A building with weapons fitted and, optionally, a turret.
 */
public class GunEmplacement extends Entity implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 8561738092216598248L;
    private String name = null;
    private int cf = 40; // default is a medium building w/ CF 40
    private int height = 2; // default height is 2
    private boolean turretNotExists = false;
    private boolean turretLocked = false;
    private int turretOffset = 0;
    private boolean burning = false;

    // locations
    public static final int LOC_BUILDING = 0;
    public static final int LOC_NORTH = 1;
    public static final int LOC_EAST = 2;
    public static final int LOC_WEST = 3;
    public static final int LOC_TURRET = 4;

    public static final String[] HIT_LOCATION_NAMES = { "Building", "Turret" };

    private static int[] CRITICAL_SLOTS = new int[] { 0, 0, 0, 0, 0 };
    private static String[] LOCATION_ABBRS = { "BU", "N", "E", "W", "TU" };
    private static String[] LOCATION_NAMES = { "Building", "North", "East",
            "West", "Turret" };

    public GunEmplacement() {
        // set defaults as specified in BMRr
        initConstructionFactor(40);
        setHeight(2);
        // not actually specfied defaults, but hey, it seems reasonable
        setTurret(false);
        initTurretArmor(0);
        initializeInternal(IArmorState.ARMOR_NA, LOC_NORTH);
        initializeInternal(IArmorState.ARMOR_NA, LOC_EAST);
        initializeInternal(IArmorState.ARMOR_NA, LOC_WEST);
    }

    public boolean hasTurret() {
        return !turretNotExists;
    }

    public void setTurret(boolean turret) {
        turretNotExists = !turret;
        if (!turret) {
            super.setSecondaryFacing(-1);
        }
    }

    public boolean isTurretLocked() {
        return turretLocked;
    }

    public void setTurretLocked(boolean locked) {
        turretLocked = locked;
    }

    public int getConstructionFactor() {
        return cf;
    }

    public void setConstructionFactor(int cf) {
        this.cf = cf;
        setWeight(cf);
    }

    public void initConstructionFactor(int cf) {
        setConstructionFactor(cf);
        initializeArmor(cf, GunEmplacement.LOC_BUILDING);
        setArmorType(EquipmentType.T_ARMOR_STANDARD);
        setArmorTechLevel(TechConstants.T_INTRO_BOXSET);
        initializeInternal(IArmorState.ARMOR_NA, LOC_BUILDING);
    }

    public void initTurretArmor(int af) {
        initializeArmor(af, GunEmplacement.LOC_TURRET);
        initializeInternal(IArmorState.ARMOR_NA, LOC_TURRET);
    }

    public int getCurrentTurretArmor() {
        return getArmor(LOC_TURRET);
    }

    // ///////// Building methods ///////////

    public String getName() {
        return name;
    }

    public boolean isIn(Coords coords) {
        return getPosition().equals(coords);
    }

    public Enumeration<Coords> getCoords() {
        // XXX yuck!
        Vector<Coords> coords = new Vector<Coords>(1);
        coords.add(getPosition());
        return coords.elements();
    }

    public int getConstructionType() {
        if (cf <= 15) {
            return Building.LIGHT;
        }
        if (cf <= 40) {
            return Building.MEDIUM;
        }
        if (cf <= 90) {
            return Building.HEAVY;
        }
        if (cf <= 150) {
            return Building.HARDENED;
        }
        return Building.UNKNOWN;
    }

    public int getCurrentCF() {
        return getArmor(LOC_BUILDING);
    }

    // XXX how to handle this?
    public void setCurrentCF(int cf) {
        if (cf < 0) {
            throw new IllegalArgumentException(
                    "Invalid value for Construction Factor: " + cf);
        }

        // this.currentCF = cf;
    }

    // XXX how to handle this?
    public int getPhaseCF() {
        return getArmor(LOC_BUILDING);
    }

    // XXX how to handle this?
    public void setPhaseCF(int cf) {
        if (cf < 0) {
            throw new IllegalArgumentException(
                    "Invalid value for Construction Factor: " + cf);
        }

        // this.phaseCF = cf;
    }

    public boolean isBurning() {
        return burning;
    }

    public void setBurning(boolean burning) {
        this.burning = burning;
    }

    // ///////// Entity methods ///////////

    @Override
    public int height() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public boolean isImmobile() {
        return true;
    }

    @Override
    public boolean isEligibleForMovement() {
        return false;
    }

    @Override
    public String getMovementString(int mtype) {
        return "Not possible!";
    }

    @Override
    public String getMovementAbbr(int mtype) {
        return "!";
    }

    @Override
    public boolean isHexProhibited(IHex hex) {

        if(hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }

        // can't put on top of an existing building
        return hex.containsTerrain(Terrains.BUILDING);
    }

    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat) {
        return 0;
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return hasTurret() && !turretLocked;
    }

    @Override
    public boolean isValidSecondaryFacing(int n) {
        return hasTurret() && !turretLocked;
    }

    @Override
    public int clipSecondaryFacing(int n) {
        return n;
    }

    @Override
    public void setSecondaryFacing(int sec_facing) {
        if (!turretLocked) {
            super.setSecondaryFacing(sec_facing);
            if (turretNotExists) {
                turretOffset = sec_facing - getFacing();
            }
        }
    }

    @Override
    public void setFacing(int facing) {
        super.setFacing(facing);
        if (isTurretLocked()) {
            int nTurretFacing = (facing + turretOffset + 6) % 6;
            super.setSecondaryFacing(nTurretFacing);
        }
    }

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    @Override
    public int locations() {
        return hasTurret() ? LOCATION_ABBRS.length : LOCATION_ABBRS.length - 1;
    }

    @Override
    public boolean hasRearArmor(int loc) {
        return false;
    }

    @Override
    public int getWeaponArc(int weaponId) {
        switch (getEquipment(weaponId).getLocation()) {
            case LOC_NORTH:
                return Compute.ARC_NORTH;

            case LOC_EAST:
                return Compute.ARC_EAST;

            case LOC_WEST:
                return Compute.ARC_WEST;

            case LOC_TURRET:
                return Compute.ARC_FORWARD;

            default:
                return Compute.ARC_360;
        }
    }

    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        if (getEquipment(weaponId).getLocation() == LOC_TURRET) {
            return true;
        }
        return false;
    }

    @Override
    public int sideTable(Coords src) {
        return ToHitData.SIDE_FRONT;
    }

    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation,
            int aimingMode) {

        if ((aimedLocation != LOC_NONE)
                && (aimingMode == IAimingModes.AIM_MODE_IMMOBILE)) {
            switch (Compute.d6(2)) {
                case 6:
                case 7:
                case 8:
                    return new HitData(
                            (aimedLocation == LOC_BUILDING) ? LOC_BUILDING
                                    : LOC_TURRET, false, true);
            }
        }
        return rollHitLocation(table, side);
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        int armorLoc = LOC_BUILDING;
        int effect = HitData.EFFECT_NONE;
        switch (Compute.d6(2)) {
            case 2:
                // ASSUMTION: damage goes to main building
                effect = HitData.EFFECT_GUN_EMPLACEMENT_WEAPONS;
                break;

            case 3:
            case 11:
                if (hasTurret()) {
                    armorLoc = LOC_TURRET;
                    effect = HitData.EFFECT_GUN_EMPLACEMENT_TURRET;
                }
                break;

            case 4:
            case 5:
            case 9:
            case 10:
                if (hasTurret()) {
                    armorLoc = LOC_TURRET;
                }
                break;

            case 12:
                // ASSUMTION: damage goes to main building
                effect = HitData.EFFECT_GUN_EMPLACEMENT_CREW;
                break;
        }
        return new HitData(armorLoc, false, effect);
    }

    /**
     * Gets the location that excess damage transfers to
     */
    @Override
    public HitData getTransferLocation(HitData hit) {
        return (hit.getLocation() == LOC_TURRET) ? new HitData(LOC_BUILDING)
                : new HitData(LOC_DESTROYED);
    }

    /**
     * Gets the location that is destroyed recursively
     */
    @Override
    public int getDependentLocation(int loc) {
        return (loc == LOC_BUILDING) ? LOC_TURRET : LOC_NONE;
    }

    /**
     * Calculates the battle value of this emplacement
     */
    @Override
    public int calculateBattleValue() {
        return calculateBattleValue(false, false);
    }

    /**
     * Calculates the battle value of this emplacement
     */
    @Override
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot) {
        // using structures BV rules from MaxTech

        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        // total armor points
        dbv += getTotalArmor();

        // add defensive equipment
        double dEquipmentBV = 0;
        for (Mounted mounted : getEquipment()) {
            EquipmentType etype = mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (((etype instanceof WeaponType) && etype.hasFlag(WeaponType.F_AMS))
                    || ((etype instanceof AmmoType) && (((AmmoType) etype)
                            .getAmmoType() == AmmoType.T_AMS))
                    || etype.hasFlag(MiscType.F_ECM)) {
                dEquipmentBV += etype.getBV(this);
            }
        }
        dbv += dEquipmentBV;

        dbv *= 0.5; // structure modifier

        double weaponBV = 0;

        // figure out base weapon bv
        // double weaponsBVFront = 0;
        // double weaponsBVRear = 0;
        boolean hasTargComp = hasTargComp();
        for (Mounted mounted : getWeaponList()) {
            WeaponType wtype = (WeaponType) mounted.getType();
            double dBV = wtype.getBV(this);

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            // don't count AMS, it's defensive
            if (wtype.hasFlag(WeaponType.F_AMS)) {
                continue;
            }

            // artemis bumps up the value
            if (mounted.getLinkedBy() != null) {
                Mounted mLinker = mounted.getLinkedBy();
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS)) {
                    dBV *= 1.2;
                }
            }

            if (mounted.getLinkedBy() != null) {
                Mounted mLinker = mounted.getLinkedBy();
                if ((mLinker.getType() instanceof MiscType) && mLinker.getType().hasFlag(MiscType.F_APOLLO)) {
                    dBV *= 1.15;
                }
            }


            // and we'll add the tcomp here too
            if (wtype.hasFlag(WeaponType.F_DIRECT_FIRE) && hasTargComp) {
                dBV *= 1.2;
            }

            // if not turret mounted, 1/2 BV
            if (mounted.getLocation() != LOC_TURRET) {
                dBV *= 0.5;
            }

            weaponBV += dBV;
        }
        obv += weaponBV;

        // add ammo bv
        double ammoBV = 0;
        for (Mounted mounted : getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();

            // don't count depleted ammo
            if (mounted.getShotsLeft() == 0) {
                continue;
            }

            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }

            ammoBV += atype.getBV(this);
        }
        obv += ammoBV;

        // we get extra bv from c3 networks. a valid network requires at least 2
        // members
        // some hackery and magic numbers here. could be better
        // also, each 'has' loops through all equipment. inefficient to do it 3
        // times
        double xbv = 0.0;
        if (((hasC3MM() && (calculateFreeC3MNodes() < 2))
                || (hasC3M() && (calculateFreeC3Nodes() < 3))
                || (hasC3S() && (c3Master > NONE)) || (hasC3i() && (calculateFreeC3Nodes() < 5)))
                && !ignoreC3 && (game != null)) {
            int totalForceBV = 0;
            totalForceBV += this.calculateBattleValue(true, true);
            for (Entity e : game.getC3NetworkMembers(this)) {
                if (!equals(e) && onSameC3NetworkAs(e)) {
                    totalForceBV += e.calculateBattleValue(true, true);
                }
            }
            xbv += totalForceBV *= 0.05;
        }

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignorePilot) {
            pilotFactor = crew.getBVSkillMultiplier();
        }

        // structure modifier
        obv *= 0.44;

        // return (int)Math.round((dbv + obv + xbv) * pilotFactor);
        int finalBV = (int) Math.round(dbv + obv + xbv);

        int retVal = (int) Math.round((finalBV) * pilotFactor);
        return retVal;
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData prd) {
        return prd;
    }

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<Report>();

        Report r = new Report(7025);
        r.type = Report.PUBLIC;
        r.addDesc(this);
        vDesc.addElement(r);

        r = new Report(7035);
        r.type = Report.PUBLIC;
        r.newlines = 0;
        vDesc.addElement(r);
        vDesc.addAll(crew.getDescVector(false));
        r = new Report(7070, Report.PUBLIC);
        r.add(getKillNumber());
        vDesc.addElement(r);

        if (isDestroyed()) {
            Entity killer = game.getEntity(killerId);
            if (killer == null) {
                killer = game.getOutOfGameEntity(killerId);
            }
            if (killer != null) {
                r = new Report(7072, Report.PUBLIC);
                r.addDesc(killer);
            } else {
                r = new Report(7073, Report.PUBLIC);
            }
            vDesc.addElement(r);
        }
        r.newlines = 2;

        return vDesc;
    }

    @Override
    public int[] getNoOfSlots() {
        return CRITICAL_SLOTS;
    }

    @Override
    public int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat) {
        return 0;
    }

    @Override
    public int getHeatCapacity() {
        return 999;
    }

    @Override
    public int getHeatCapacityWithWater() {
        return getHeatCapacity();
    }

    @Override
    public int getEngineCritHeat() {
        return 0;
    }

    @Override
    public void autoSetInternal() {
        initializeInternal(0, LOC_BUILDING);
        if (hasTurret()) {
            initializeInternal(0, LOC_TURRET);
        }
    }

    @Override
    public int getMaxElevationChange() {
        return 0;
    }

    @Override
    public boolean isRepairable() {
        boolean retval = isSalvage();
        int loc = 0;
        while (retval && (loc < LOC_TURRET)) {
            int loc_is = this.getInternal(loc);
            loc++;
            retval = (loc_is != IArmorState.ARMOR_DOOMED)
                    && (loc_is != IArmorState.ARMOR_DESTROYED);
        }
        return retval;
    }

    @Override
    public boolean canCharge() {
        return false;
    }

    @Override
    public boolean canDFA() {
        return false;
    }

    @Override
    public boolean canFlee() {
        return false;
    }

    @Override
    public boolean canFlipArms() {
        return false;
    }

    @Override
    public boolean canGoDown() {
        return false;
    }

    @Override
    public boolean canGoDown(int assumed, Coords coords) {
        return false;
    }

    @Override
    public double getCost() {
        // XXX no idea
        return 0;
    }

    @Override
    public boolean doomedInVacuum() {
        for (Mounted m : getEquipment()) {
            if ((m.getType() instanceof MiscType)
                    && m.getType().hasFlag(MiscType.F_VACUUM_PROTECTION)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean doomedOnGround() {
        return false;
    }

    @Override
    public boolean doomedInAtmosphere() {
        return true;
    }

    @Override
    public boolean doomedInSpace() {
        return true;
    }

    @Override
    public boolean isNuclearHardened() {
        return true;
    }

    @Override
    protected void addEquipment(Mounted mounted, int loc, boolean rearMounted)
            throws LocationFullException {
        super.addEquipment(mounted, loc, rearMounted);
        // Add the piece equipment to our slots.
        addCritical(loc, new CriticalSlot(CriticalSlot.TYPE_EQUIPMENT,
                getEquipmentNum(mounted), true, mounted));
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getTotalCommGearTons()
     */
    @Override
    public int getTotalCommGearTons() {
        return getExtraCommGearTons();
    }
}
