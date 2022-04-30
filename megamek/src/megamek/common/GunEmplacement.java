/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *           Copyright (C) 2005 Mike Gratton <mike@vee.net>
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
package megamek.common;

import megamek.client.ui.swing.calculationReport.CalculationReport;
import megamek.client.ui.swing.calculationReport.DummyCalculationReport;
import megamek.common.battlevalue.GunEmplacementBVCalculator;
import megamek.common.cost.CostCalculator;
import megamek.common.enums.AimingMode;
import org.apache.logging.log4j.LogManager;

import java.util.Vector;

/**
 * A building with weapons fitted and, optionally, a turret.
 * Taharqa: I am completely re-writing this entity to bring it up to code with TacOps rules
 * GunEmplacements will not simply be the weapon loadouts that can be attached to buildings.
 * They will not be targetable in game, but will be destroyed if their building hex is reduced.
 */
public class GunEmplacement extends Tank {

    private static final long serialVersionUID = 8561738092216598248L;

    // locations
    public static final int LOC_GUNS = 0;

    public static final String[] HIT_LOCATION_NAMES = { "guns" };

    private static int[] CRITICAL_SLOTS = new int[] { 0 };
    private static String[] LOCATION_ABBRS = { "GUN" };
    private static String[] LOCATION_NAMES = { "GUNS" };
        
    private static final TechAdvancement TA_GUN_EMPLACEMENT = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
            .setTechRating(RATING_B).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
            .setStaticTechLevel(SimpleTechLevel.INTRO);
    
    public static final TechAdvancement TA_LIGHT_BUILDING = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(DATE_PS, DATE_PS, DATE_PS)
            .setTechRating(RATING_A).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
            .setStaticTechLevel(SimpleTechLevel.INTRO);
    
    private int initialBuildingCF;
    private int initialBuildingArmor;
    
    @Override
    public TechAdvancement getConstructionTechAdvancement() {
        return TA_GUN_EMPLACEMENT;
    }

    public GunEmplacement() {
        initializeInternal(IArmorState.ARMOR_NA, LOC_GUNS);
        //give it an engine just to avoid NPE on calls to Tank
        setEngine(new Engine(0, Engine.NORMAL_ENGINE, Engine.TANK_ENGINE));
    }

    @Override
    public int getUnitType() {
        return UnitType.GUN_EMPLACEMENT;
    }

    public boolean isTurret() {
        return !hasNoTurret();
    }

    @Override
    public boolean isImmobile() {
        return true;
    }
    
    /**
     * Our gun emplacements do not support dual turrets at this time
     */
    @Override
    public boolean hasNoDualTurret() {
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
        Hex hex = game.getBoard().getHex(c);

        if (hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
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
        if (isTurret()) {
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
    public HitData rollHitLocation(int table, int side, int aimedLocation, AimingMode aimingMode,
                                   int cover) {
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

    @Override
    public int doBattleValueCalculation(boolean ignoreC3, boolean ignoreSkill, CalculationReport calculationReport) {
        return GunEmplacementBVCalculator.calculateBV(this, ignoreC3, ignoreSkill, calculationReport);
    }

    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData prd) {
        return prd;
    }

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<>();

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
        } else if (getCrew().isEjected()) {
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
        return DOES_NOT_TRACK_HEAT;
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
    public double getCost(CalculationReport calcReport, boolean ignoreAmmo) {
        CostCalculator.addNoReportNote(calcReport, this);
        return 0;
    }

    @Override
    public boolean doomedInExtremeTemp() {
        return false;
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
        if (checkCrew && (null != getCrew()) && getCrew().isDead()) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: Crew dead.");
            return true;
        } else if (isMilitary() && !hasViableWeapons()) {
            LogManager.getLogger().debug(getDisplayName() + " CRIPPLED: no viable weapons left.");
            return true;
        } else {
            return false;
        }
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
        return ((double) totalInoperable / totalWeapons) >= 0.75;
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

        return ((double) totalInoperable / totalWeapons) >= 0.25;
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

        return ((double) totalInoperable / totalWeapons) >= 0.5;
    }

    @Override
    public long getEntityType() {
        return Entity.ETYPE_TANK | Entity.ETYPE_GUN_EMPLACEMENT;
    }
    
    @Override
    public boolean hasEngine() {
        // TODO: Power generators and energy grid setup
        return false;
    }
    
    @Override
    public int getArmorType(int loc) {
        // this is a hack to get around the fact that gun emplacements don't even have armor
        return 0;
    }
    
    @Override
    public boolean hasStealth() {
        return false;
    }
    
    /**
     * Sets the deployed flag. 
     * Has the side effect of initializing building CF if deployed
     */
    @Override
    public void setDeployed(boolean deployed) {
        super.setDeployed(deployed);

        if (deployed) {
            Building occupiedStructure = getGame().getBoard().getBuildingAt(getPosition());
            
            initialBuildingCF = occupiedStructure.getCurrentCF(getPosition());
            initialBuildingArmor = occupiedStructure.getArmor(getPosition());
        } else {
            initialBuildingCF = initialBuildingArmor = 0;
        }
    }
    
    /**
     * How much more damage, percentage-wise, the gun emplacement's building can take
     */
    @Override
    public double getArmorRemainingPercent() {
        if (getPosition() == null) {
            return 1.0;
        }
        
        Building occupiedStructure = getGame().getBoard().getBuildingAt(getPosition());
        
        // we'll consider undeployed emplacements to be fully intact
        if ((occupiedStructure == null) || (initialBuildingCF + initialBuildingArmor == 0)) {
            return 1.0;
        }
        
        return (occupiedStructure.getCurrentCF(getPosition()) + occupiedStructure.getArmor(getPosition()))
                / ((double) (initialBuildingCF + initialBuildingArmor));
    }
}
