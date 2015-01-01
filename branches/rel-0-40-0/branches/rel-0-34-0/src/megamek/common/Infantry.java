/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
import java.util.ArrayList;
import java.util.Vector;

/**
 * This class represents the lowest of the low, the ground pounders, the city
 * rats, the PBI (Poor Bloody Infantry). <p/> PLEASE NOTE!!! This class just
 * represents unarmored infantry platoons as described by CitiTech (c) 1986.
 * I've never seen the rules for powered armor, "anti-mech" troops, or
 * Immortals.
 *
 * @author Suvarov454@sourceforge.net (James A. Damour )
 * @version $revision:$
 */
/*
 * PLEASE NOTE!!! My programming style is to put constants first in tests so the
 * compiler catches my "= for ==" errors.
 */
public class Infantry extends Entity implements Serializable {
    // Private attributes and helper functions.

    /**
     *
     */
    private static final long serialVersionUID = -8706716079307721282L;

    /**
     * The number of men originally in this platoon.
     */
    private int menStarting = 0;

    /**
     * The number of men alive in this platoon at the beginning of the phase,
     * before it begins to take damage.
     */
    private int menShooting = 0;

    /**
     * The number of men left alive in this platoon.
     */
    private int men = 0;

    /**
     * Infantry have no critical slot limitations or locations.
     */
    private static final int[] NUM_OF_SLOTS = { 0 };
    private static final String[] LOCATION_ABBRS = { "Men" };
    private static final String[] LOCATION_NAMES = { "Men" };

    /**
     * Identify this platoon as anti-mek trained.
     */
    private boolean antiMek = false;

    public int turnsLayingExplosives = -1;

    public static final int DUG_IN_NONE = 0;
    public static final int DUG_IN_WORKING = 1; // no protection, can't attack
    public static final int DUG_IN_COMPLETE = 2; // protected, restricted arc
    public static final int DUG_IN_FORTIFYING1 = 3; // no protection, can't
                                                    // attack
    public static final int DUG_IN_FORTIFYING2 = 4; // no protection, can't
                                                    // attack
    private int dugIn = DUG_IN_NONE;

    // Public and Protected constants, constructors, and methods.

    /**
     * The maximum number of men in an infantry platoon.
     */
    public static final int INF_PLT_MAX_MEN = 30;

    /**
     * The maximum number of men in an infantry platoon.
     */
    public static final int INF_PLT_FOOT_MAX_MEN = 21;

    /**
     * The maximum number of men in an infantry platoon.
     */
    public static final int INF_PLT_JUMP_MAX_MEN = 21;

    /**
     * The maximum number of men in an infantry platoon.
     */
    public static final int INF_PLT_CLAN_MAX_MEN = 25;


    /**
     * The location for infantry equipment.
     */
    public static final int LOC_INFANTRY = 0;

    /**
     * The internal names of the anti-Mek attacks.
     */
    public static final String LEG_ATTACK = "LegAttack";
    public static final String SWARM_MEK = "SwarmMek";
    public static final String STOP_SWARM = "StopSwarm";

    @Override
    public String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    /**
     * Returns the number of locations in this platoon (i.e. one).
     */
    @Override
    public int locations() {
        return 1;
    }

    /**
     * Generate a new, blank, infantry platoon. Hopefully, we'll be loaded from
     * somewhere.
     */
    public Infantry() {
        // Instantiate the superclass.
        super();
        // Create a "dead" leg rifle platoon.
        menStarting = 0;
        menShooting = 0;
        men = 0;
        setMovementMode(IEntityMovementMode.INF_LEG);
        // Determine the number of MPs.
        setOriginalWalkMP(1);
    }

    /**
     * Infantry can face freely (except when dug in)
     */
    @Override
    public boolean canChangeSecondaryFacing() {
        return (dugIn == DUG_IN_NONE);
    }

    /**
     * Infantry can face freely
     */
    @Override
    public boolean isValidSecondaryFacing(int dir) {
        return true;
    }

    /**
     * Infantry can face freely
     */
    @Override
    public int clipSecondaryFacing(int dir) {
        return dir;
    }

    /**
     * return this infantry's walk mp, adjusted for planetary conditions
     */
    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat) {
        int mp = getOriginalWalkMP();
        if(null != game) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            if(weatherMod != 0) {
                mp = Math.max(mp + weatherMod, 0);
            }
        }
        if (gravity) {
            mp = applyGravityEffectsOnMP(mp);
        }
        return mp;
    }

    /**
     * Return this Infantry's run MP, which is identical to its walk MP
     */
    @Override
    public int getRunMP(boolean gravity, boolean ignoreheat) {
        return getWalkMP(gravity, ignoreheat);
    }

    /**
     * Infantry don't have MASC
     */
    @Override
    public int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat) {
        return getRunMP(gravity, ignoreheat);
    }


    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getJumpMP(boolean)
     */
    @Override
    public int getJumpMP(boolean gravity) {
        int mp = getOriginalJumpMP();
        if (gravity) {
            mp = applyGravityEffectsOnMP(mp);
        }
        int windP = 0;
        if(null != game) {
            int windCond = game.getPlanetaryConditions().getWindStrength();
            if(windCond == PlanetaryConditions.WI_MOD_GALE) {
                windP++;
            }
            if(windCond >= PlanetaryConditions.WI_STRONG_GALE) {
                return 0;
            }
        }
        mp = Math.max(mp - windP, 0);
        return mp;
    }



    /**
     * Infantry can not enter water unless they have UMU mp or hover.
     */
    @Override
    public boolean isHexProhibited(IHex hex) {
        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }
        if (hex.containsTerrain(Terrains.MAGMA)) {
            return true;
        }
        if(hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }

        if (hex.terrainLevel(Terrains.WOODS) > 0) {
            if (hex.terrainLevel(Terrains.WOODS) > 1 && getMovementMode() == IEntityMovementMode.TRACKED) {
                return true;
            }
            if (getMovementMode() == IEntityMovementMode.HOVER
                    || getMovementMode() == IEntityMovementMode.WHEELED) {
                return true;
            }
        }
        if (hex.terrainLevel(Terrains.WATER) > 0
                && !hex.containsTerrain(Terrains.ICE)) {
            if (getMovementMode() == IEntityMovementMode.HOVER
                    || getMovementMode() == IEntityMovementMode.INF_UMU
                    || getMovementMode() == IEntityMovementMode.VTOL) {
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Returns the name of the type of movement used. This is Infantry-specific.
     */
    @Override
    public String getMovementString(int mtype) {
        switch (mtype) {
            case IEntityMovementType.MOVE_NONE:
                return "None";
            case IEntityMovementType.MOVE_WALK:
            case IEntityMovementType.MOVE_RUN:
                switch (getMovementMode()) {
                    case IEntityMovementMode.INF_LEG:
                        return "Walked";
                    case IEntityMovementMode.INF_MOTORIZED:
                        return "Biked";
                    case IEntityMovementMode.HOVER:
                    case IEntityMovementMode.TRACKED:
                    case IEntityMovementMode.WHEELED:
                        return "Drove";
                    case IEntityMovementMode.INF_JUMP:
                    default:
                        return "Unknown!";
                }
            case IEntityMovementType.MOVE_VTOL_WALK:
            case IEntityMovementType.MOVE_VTOL_RUN:
                return "Flew";
            case IEntityMovementType.MOVE_JUMP:
                return "Jumped";
            default:
                return "Unknown!";
        }
    }

    /**
     * Returns the abbreviation of the type of movement used. This is
     * Infantry-specific.
     */
    @Override
    public String getMovementAbbr(int mtype) {
        switch (mtype) {
            case IEntityMovementType.MOVE_NONE:
                return "N";
            case IEntityMovementType.MOVE_WALK:
                return "W";
            case IEntityMovementType.MOVE_RUN:
                switch (getMovementMode()) {
                    case IEntityMovementMode.INF_LEG:
                        return "R";
                    case IEntityMovementMode.INF_MOTORIZED:
                        return "B";
                    case IEntityMovementMode.HOVER:
                    case IEntityMovementMode.TRACKED:
                    case IEntityMovementMode.WHEELED:
                        return "D";
                    default:
                        return "?";
                }
            case IEntityMovementType.MOVE_JUMP:
                return "J";
            default:
                return "?";
        }
    }

    /**
     * Infantry only have one hit location.
     */
    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation,
            int aimingMode) {
        return rollHitLocation(table, side);
    }

    @Override
    public HitData rollHitLocation(int table, int side) {
        return new HitData(0);
    }

    /**
     * Infantry only have one hit location.
     */
    @Override
    public HitData getTransferLocation(HitData hit) {
        return new HitData(Entity.LOC_DESTROYED);
    }

    /**
     * Gets the location that is destroyed recursively.
     */
    @Override
    public int getDependentLocation(int loc) {
        return Entity.LOC_NONE;
    }

    /**
     * Infantry have no rear armor.
     */
    @Override
    public boolean hasRearArmor(int loc) {
        return false;
    }

    /**
     * Infantry platoons do wierd and wacky things with armor and internals, but
     * not all Infantry objects are platoons.
     *
     * @see megamek.common.BattleArmor#isPlatoon()
     */
    protected boolean isPlatoon() {
        return true;
    }

    /**
     * Returns the number of men left in the platoon, or
     * IArmorState.ARMOR_DESTROYED.
     */
    @Override
    public int getInternal(int loc) {
        if (!isPlatoon()) {
            return super.getInternal(loc);
        }
        return (men > 0 ? men : IArmorState.ARMOR_DESTROYED);
    }

    /**
     * Returns the number of men originally the platoon.
     */
    @Override
    public int getOInternal(int loc) {
        if (!isPlatoon()) {
            return super.getOInternal(loc);
        }
        return menStarting;
    }

    /**
     * Sets the amount of men remaining in the platoon.
     */
    @Override
    public void setInternal(int val, int loc) {
        super.setInternal(val, loc);
        men = val;
    }

    /**
     * Returns the percent of the men remaining in the platoon.
     */
    @Override
    public double getInternalRemainingPercent() {
        if (!isPlatoon()) {
            return super.getInternalRemainingPercent();
        }
        int menTotal = men > 0 ? men : 0; // Handle "DESTROYED"
        return ((double) menTotal / menStarting);
    }

    /**
     * Initializes the number of men in the platoon. Sets the original and
     * starting point of the platoon to the same number.
     */
    @Override
    public void initializeInternal(int val, int loc) {
        menStarting = val;
        menShooting = val;
        super.initializeInternal(val, loc);
    }

    /**
     * Set the men in the platoon to the appropriate value for the platoon's
     * movement type.
     */
    @Override
    public void autoSetInternal() {

        // Clan platoons have 25 men.
        if (isClan()) {
            initializeInternal(INF_PLT_CLAN_MAX_MEN, LOC_INFANTRY);
            return;
        }

        // IS platoon strength is based upon movement type.
        switch (getMovementMode()) {
            case IEntityMovementMode.INF_LEG:
            case IEntityMovementMode.INF_MOTORIZED:
                initializeInternal(INF_PLT_FOOT_MAX_MEN, LOC_INFANTRY);
                break;
            case IEntityMovementMode.INF_JUMP:
                initializeInternal(INF_PLT_JUMP_MAX_MEN, LOC_INFANTRY);
                break;
            default:
                throw new IllegalArgumentException("Unknown movement type: "
                        + getMovementMode());
        }

        if (hasWorkingMisc(MiscType.F_TOOLS, MiscType.S_HEAVY_ARMOR)) {
            initializeArmor(getOInternal(LOC_INFANTRY), LOC_INFANTRY);
        }
        return;
    }

    /**
     * Infantry weapons are dictated by their type.
     */
    @Override
    protected void addEquipment(Mounted mounted, int loc, boolean rearMounted)
            throws LocationFullException {
        EquipmentType equip = mounted.getType();

        // If the infantry can swarm, they're anti-mek infantry.
        if (Infantry.SWARM_MEK.equals(equip.getInternalName())) {
            antiMek = true;
        }
        // N.B. Clan Undine BattleArmor can leg attack, but aren't
        // classified as "anti-mek" in the BMRr, pg. 155).
        else if (Infantry.LEG_ATTACK.equals(equip.getInternalName())
                || Infantry.STOP_SWARM.equals(equip.getInternalName())) {
            // Do nothing.
        }
        // Update our superclass.
        super.addEquipment(mounted, loc, rearMounted);
    }

    /**
     * Infantry can fire all around themselves. But field guns are set up to a
     * facing
     */
    @Override
    public int getWeaponArc(int wn) {
        if (this instanceof BattleArmor && dugIn == DUG_IN_NONE) {
            return Compute.ARC_360;
        }
        Mounted mounted = getEquipment(wn);
        WeaponType wtype = (WeaponType) mounted.getType();
        if ((wtype.hasFlag(WeaponType.F_INFANTRY)
                || wtype.hasFlag(WeaponType.F_EXTINGUISHER)
                || wtype.getInternalName() == LEG_ATTACK
                || wtype.getInternalName() == SWARM_MEK || wtype
                .getInternalName() == STOP_SWARM)
                && dugIn == DUG_IN_NONE) {
            return Compute.ARC_360;
        }
        return Compute.ARC_FORWARD;
    }

    /**
     * Infantry can fire all around themselves. But field guns act like turret
     * mounted on a tank
     */
    @Override
    public boolean isSecondaryArcWeapon(int wn) {
        if (this instanceof BattleArmor) {
            return false;
        }
        Mounted mounted = getEquipment(wn);
        WeaponType wtype = (WeaponType) mounted.getType();
        if (wtype.hasFlag(WeaponType.F_INFANTRY)) {
            return false;
        }
        return true;
    }

    /**
     * Infantry build no heat.
     */
    @Override
    public int getHeatCapacity() {
        return 999;
    }

    /**
     * Infantry build no heat.
     */
    @Override
    public int getHeatCapacityWithWater() {
        return getHeatCapacity();
    }

    /**
     * Infantry build no heat.
     */
    @Override
    public int getEngineCritHeat() {
        return 0;
    }

    /**
     * Infantry have no critical slots.
     */
    @Override
    protected int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    /**
     * Infantry criticals can't be hit.
     */
    public boolean hasHittableCriticals(int loc) {
        return false;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#calculateBattleValue()
     */
    @Override
    public int calculateBattleValue() {
        return calculateBattleValue(false, false);
    }

    /**
     * Calculates the battle value of this platoon.
     */
    @Override
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot) {
        double dbv;
        dbv = this.getInternal(Entity.LOC_NONE) * 1.5;
        int tmmRan = Compute.getTargetMovementModifier(getRunMP(false, true), false, false)
                .getValue();
        int tmmJumped = Compute.getTargetMovementModifier(getJumpMP(false),
                true, false).getValue();
        double targetMovementModifier = Math.max(tmmRan, tmmJumped);
        double tmmFactor = 1 + (targetMovementModifier / 10);
        dbv *= tmmFactor;
        // double weaponbv;
        double obv;
        // adjust further for speed factor
        // this is a bit weird, because the formula gives
        // a different result than the table, because MASC/TSM
        // is handled differently (page 315, TM, compare
        // http://forums.classicbattletech.com/index.php/topic,20468.0.html
        double speedFactor;
        double speedFactorTableLookup = getRunMP(false, true)
                + Math.round((double) getJumpMP(false) / 2);
        if (speedFactorTableLookup > 25) {
            speedFactor = Math.pow(1 + (((double) walkMP
                    + (Math.round((double) getJumpMP(false) / 2)) - 5) / 10), 1.2);
        } else {
            speedFactor = Math
                    .pow(1 + ((speedFactorTableLookup - 5) / 10), 1.2);
        }
        speedFactor = Math.round(speedFactor * 100) / 100.0;
        ArrayList<Mounted> weapons = getWeaponList();
        double wbv = 0;
        for (Mounted weapon : weapons) {
            WeaponType wtype = (WeaponType) weapon.getType();
            if (Infantry.SWARM_MEK.equals(wtype.getInternalName())) {
                continue;
            }
            // infantry weapons get counted multiple times
            if (weapon.getType().hasFlag(WeaponType.F_INFANTRY)) {
                // stupid assumption to at least get a value:
                // each weapon is carried once by each platoon member
                // if an antiMek platoon, count twice
                wbv += wtype.getBV(this) * this.getInternal(Entity.LOC_NONE)
                        * (antiMek ? 2 : 0);
            } else {
                // field guns count only once
                wbv += wtype.getBV(this);
            }
        }
        obv = wbv * speedFactor;
        int bv = (int) Math.round(obv + dbv);
        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignorePilot) {
            pilotFactor = crew.getBVSkillMultiplier();
        }
        return (int) Math.round((bv) * pilotFactor);

    } // End public int calculateBattleValue()

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<Report>();

        Report r = new Report(7025);
        r.type = Report.PUBLIC;
        r.addDesc(this);
        vDesc.addElement(r);

        r = new Report(7040);
        r.type = Report.PUBLIC;
        r.newlines = 0;
        vDesc.addElement(r);
        vDesc.addAll(crew.getDescVector(true));
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

    /**
     * Infantry don't need piloting rolls.
     */
    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData prd) {
        return prd;
    }

    /**
     * Infantry can only change 1 elevation level at a time.
     */
    @Override
    public int getMaxElevationChange() {
        return 1;
    }

    /**
     * Update the platoon to reflect damages taken in this phase.
     */
    @Override
    public void applyDamage() {
        super.applyDamage();
        menShooting = men;
    }

    // The methods below aren't in the Entity interface.

    /**
     * Get the number of men in the platoon (before damage is applied).
     */
    public int getShootingStrength() {
        return menShooting;
    }

    @Override
    public boolean canCharge() {
        // Infantry can't Charge
        return false;
    }

    @Override
    public boolean canDFA() {
        // Infantry can't DFA
        return false;
    }

    /**
     * Checks if the entity is moving into a swamp. If so, returns the target
     * roll for the piloting skill check. now includes the level 3 terains which
     * can bog down
     */
    public PilotingRollData checkBogDown(MoveStep step, IHex curHex,
            Coords lastPos, Coords curPos, boolean isPavementStep) {
        PilotingRollData roll = new PilotingRollData(getId(), 5,
                "entering boggy terrain");
        int bgMod = curHex.getBogDownModifier(getMovementMode(), false);
        if (!lastPos.equals(curPos) && bgMod != TargetRoll.AUTOMATIC_SUCCESS && step.getMovementType() != IEntityMovementType.MOVE_JUMP && (getMovementMode() != IEntityMovementMode.HOVER) && (getMovementMode() != IEntityMovementMode.VTOL) && (getMovementMode() != IEntityMovementMode.WIGE) && step.getElevation() == 0 && !isPavementStep) {
            roll.append(new PilotingRollData(getId(), bgMod, "avoid bogging down"));
        } else {
            roll.addModifier(TargetRoll.CHECK_FALSE, "Check false: Not entering bog-down terrain, or jumping/hovering over such terrain");
        }
        return roll;
    }

    /**
     * @return The cost in C-Bills of the Infantry in question.
     */
    @Override
    public double getCost() {
        double multiplier = 0;

        if (antiMek) {
            multiplier = 5;
        } else {
            multiplier = 1;
        }
        
        switch (getMovementMode()){
        case IEntityMovementMode.INF_UMU:
            multiplier *= 2.0;
        case IEntityMovementMode.INF_LEG:
            multiplier *= 1.0;
            break;
        case IEntityMovementMode.INF_MOTORIZED:
            multiplier *= 1.6;
            break;
        case IEntityMovementMode.INF_JUMP:
            multiplier *= 2.6;
            break;
        case IEntityMovementMode.HOVER:
            multiplier *= 3.2;
            break;
        case IEntityMovementMode.WHEELED:
            multiplier *= 3.2;
            break;
        case IEntityMovementMode.TRACKED:
            multiplier *= 3.2;
            break;
        default:
            break;                
        }
        
        return Math.round(2000 * Math.sqrt(this.getWeaponsAndEquipmentCost()) * multiplier * menStarting);
    }

    @Override
    public boolean doomedInVacuum() {
        // We're assuming that infantry have environmental suits of some sort.
        // Vac suits, battle armor, whatever.
        // This isn't necessarily a true assumption.
        // FIXME
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
    public boolean canAssaultDrop() {
        return game.getOptions().booleanOption("paratroopers");
    }

    @Override
    public boolean isEligibleFor(IGame.Phase phase) {
        if (turnsLayingExplosives > 0 && phase != IGame.Phase.PHASE_PHYSICAL) {
            return false;
        }
        if (dugIn != DUG_IN_COMPLETE && dugIn != DUG_IN_NONE) {
            return false;
        }
        return super.isEligibleFor(phase);
    }

    @Override
    public void newRound(int roundNumber) {
        if (turnsLayingExplosives >= 0) {
            turnsLayingExplosives++;
            if (!(Compute.isInBuilding(game, this))) {
                turnsLayingExplosives = -1; // give up if no longer in a
                                            // building
            }
        }
        if (dugIn != DUG_IN_COMPLETE && dugIn != DUG_IN_NONE) {
            dugIn++;
            if (dugIn > DUG_IN_FORTIFYING2) {
                dugIn = DUG_IN_NONE;
            }
        }
        super.newRound(roundNumber);
    }

    @Override
    public boolean loadWeapon(Mounted mounted, Mounted mountedAmmo) {
        if (!(this instanceof BattleArmor)) {
            // field guns don't share ammo, and infantry weapons dont have ammo
            if (mounted.getLinked() != null
                    || mountedAmmo.getLinkedBy() != null) {
                return false;
            }
        }
        return super.loadWeapon(mounted, mountedAmmo);
    }

    @Override
    public boolean loadWeaponWithSameAmmo(Mounted mounted, Mounted mountedAmmo) {
        if (!(this instanceof BattleArmor)) {
            // field guns don't share ammo, and infantry weapons dont have ammo
            if (mounted.getLinked() != null
                    || mountedAmmo.getLinkedBy() != null) {
                return false;
            }
        }
        return super.loadWeaponWithSameAmmo(mounted, mountedAmmo);
    }

    public void setDugIn(int i) {
        dugIn = i;
    }

    public int getDugIn() {
        return dugIn;
    }

    @Override
    public boolean isNuclearHardened() {
        return false;
    }

    /**
     * This function is called when loading a unit into a transport. This is
     * overridden to ensure infantry are no longer considered dug in when they
     * are being transported.
     *
     * @param transportID
     */
    public void setTransportID(int transportID) {
        super.setTransportId(transportID);

        setDugIn(DUG_IN_NONE);
    }

    public boolean isAntiMek() {
        return antiMek;
    }

    public boolean isMechanized() {
        if (getMovementMode() == IEntityMovementMode.WHEELED ||
                getMovementMode() == IEntityMovementMode.HOVER ||
                getMovementMode() == IEntityMovementMode.TRACKED) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getTotalCommGearTons()
     */
    @Override
    public int getTotalCommGearTons() {
        return 0;
    }

} // End class Infantry
