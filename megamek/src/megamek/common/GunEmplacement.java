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

import java.util.Vector;

import megamek.common.preference.PreferenceManager;

/**
 * A building with weapons fitted and, optionally, a turret.
 * Taharqa: I am completely re-writing this entity to bring it up to code with TacOps rules
 * GunEmplacements will not simply be the weapon loadouts that can be attached to buildings.
 * They will not be targetable in game, but will be destroyed if their building hex is reduced.
 */
public class GunEmplacement extends Tank {

    /**
     *
     */
    private static final long serialVersionUID = 8561738092216598248L;

    // locations
    public static final int LOC_GUNS = 0;

    public static final String[] HIT_LOCATION_NAMES = { "guns" };

    private static int[] CRITICAL_SLOTS = new int[] { 0 };
    private static String[] LOCATION_ABBRS = { "GUN" };
    private static String[] LOCATION_NAMES = { "GUNS" };

    public GunEmplacement() {
        initializeInternal(IArmorState.ARMOR_NA, LOC_GUNS);
        //give it an engine just to avoid NPE on calls to Tank
        setEngine(new Engine(0, Engine.NORMAL_ENGINE, Engine.TANK_ENGINE));
    }

    public boolean isTurret() {
        return !hasNoTurret();
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
    public String getMovementString(EntityMovementType mtype) {
        return "Not possible!";
    }

    @Override
    public String getMovementAbbr(EntityMovementType mtype) {
        return "!";
    }

    @Override
    public boolean isLocationProhibited(Coords c, int currElevation) {
        IHex hex = game.getBoard().getHex(c);

        if(hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }
        //gun emplacements must be placed on a building
        return !hex.containsTerrain(Terrains.BUILDING);

    }

    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat) {
        return 0;
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
        return 1;
    }

    @Override
    public boolean hasRearArmor(int loc) {
        return false;
    }

    @Override
    public int getWeaponArc(int weaponId) {
        if(isTurret()) {
            return Compute.ARC_TURRET;
        }
        return Compute.ARC_FORWARD;
    }

    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        return isTurret();
    }

    @Override
    public int sideTable(Coords src) {
        return ToHitData.SIDE_FRONT;
    }

    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation,
            int aimingMode, int cover) {
        return rollHitLocation(table, side);
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        return new HitData(LOC_GUNS, false, HitData.EFFECT_NONE);
    }

    /**
     * Gets the location that excess damage transfers to
     */
    @Override
    public HitData getTransferLocation(HitData hit) {
        return new HitData(LOC_DESTROYED);
    }

    /**
     * Gets the location that is destroyed recursively
     */
    @Override
    public int getDependentLocation(int loc) {
        return LOC_NONE;
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
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                    dBV *= 1.3;
                }
                if ((mLinker.getType() instanceof MiscType)
                        && mLinker.getType().hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                    dBV *= 1.25;
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

            weaponBV += dBV;
        }
        obv += weaponBV;

        // add ammo bv
        double ammoBV = 0;
        for (Mounted mounted : getAmmo()) {
            AmmoType atype = (AmmoType) mounted.getType();

            // don't count depleted ammo
            if (mounted.getUsableShotsLeft() == 0) {
                continue;
            }

            // don't count AMS, it's defensive
            if (atype.getAmmoType() == AmmoType.T_AMS) {
                continue;
            }

            ammoBV += atype.getBV(this);
        }
        obv += ammoBV;

        // structure modifier
        obv *= 0.44;
        double finalBV;
        if (useGeometricMeanBV()) {
            finalBV = 2 * Math.sqrt(obv * dbv);
            if (finalBV == 0) {
                finalBV = obv + dbv;
            }
        } else {
            finalBV = obv + dbv;
        }
        double xbv = 0.0;
        if (!ignoreC3 && (game != null)) {
            xbv += getExtraC3BV((int)Math.round(finalBV));
        }

        finalBV += xbv;

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignorePilot) {
            pilotFactor = getCrew().getBVSkillMultiplier(game);
        }

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
        vDesc.addAll(getCrew().getDescVector(false));
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
        } else if (getCrew().isEjected()){
            r = new Report(7071, Report.PUBLIC);
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
    public int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat, boolean ignoremodulararmor) {
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
        initializeInternal(0, LOC_GUNS);
    }

    @Override
    public int getMaxElevationChange() {
        return 0;
    }

    @Override
    public boolean isRepairable() {
        return isSalvage();
    }

    @Override
    public boolean isTargetable() {
        return false;
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
    public double getCost(boolean ignoreAmmo) {
        // XXX no idea
        return 0;
    }

    @Override
    public boolean doomedInVacuum() {
        return false;
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
    public void addEquipment(Mounted mounted, int loc, boolean rearMounted)
            throws LocationFullException {
        super.addEquipment(mounted, loc, rearMounted);
        // Add the piece equipment to our slots.
        addCritical(loc, new CriticalSlot(mounted));
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getTotalCommGearTons()
     */
    @Override
    public int getTotalCommGearTons() {
        return getExtraCommGearTons();
    }

    @Override
    public boolean isCrippled(boolean checkCrew) {
        return isCrippled();
    }

    @Override
    public boolean isCrippled() {
        if (isMilitary() && !hasViableWeapons()) {
            if (PreferenceManager.getClientPreferences().debugOutputOn()) {
                System.out.println(getDisplayName()
                        + " CRIPPLED: no viable weapons left.");
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isDmgHeavy() {
        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted weap : getTotalWeaponList()) {
            if (weap.isCrippled()) {
                totalInoperable++;
            }
        }
        return ((double)totalInoperable / totalWeapons) >= 0.75;
    }

    @Override
    public boolean isDmgLight() {
        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted weap : getTotalWeaponList()) {
            if (weap.isCrippled()) {
                totalInoperable++;
            }
        }

        return ((double)totalInoperable / totalWeapons) >= 0.25;
    }

    @Override
    public boolean isDmgModerate() {
        int totalWeapons = getTotalWeaponList().size();
        int totalInoperable = 0;
        for (Mounted weap : getTotalWeaponList()) {
            if (weap.isCrippled()) {
                totalInoperable++;
            }
        }

        return ((double)totalInoperable / totalWeapons) >= 0.5;
    }


    @Override
    public long getEntityType(){
        return Entity.ETYPE_TANK | Entity.ETYPE_GUN_EMPLACEMENT;
    }
    
    @Override
    public boolean hasEngine() {
        // TODO: Power generators and energy grid setup
        return false;
    }
}
