/*
 * MegaMek - Copyright (C) 2003,2004 Ben Mazur (bmazur@sev.org)
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

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import megamek.common.preference.PreferenceManager;

/**
 * Protomechs. Level 2 Clan equipment.
 */
public class Protomech extends Entity implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = -1376410042751538158L;

    public static final int NUM_PMECH_LOCATIONS = 6;

    public static final String[] LOCATION_NAMES = { "Head", "Torso",
            "Right Arm", "Left Arm", "Legs", "Main Gun" };

    public static final String[] LOCATION_ABBRS = { "HD", "T", "RA", "LA", "L",
            "MG" };

    // weapon bools
    private boolean bHasMainGun;
    private boolean bHasRArmGun;
    private boolean bHasLArmGun;
    private boolean bHasTorsoAGun;
    private boolean bHasTorsoBGun;

    // weapon indices
    private int torsoAGunNum;
    private int torsoBGunNum;

    // locations

    // Pilot damage caused so far by crits to this location.
    // Needed for location destruction pilot damage.
    private int PilotDamageTaken[] = { 0, 0, 0, 0, 0, 0 };

    /**
     * Not every Protomech has a main gun.
     * N.B. Regardless of the value set here, the variable is initialized to
     * <code>false</code> until after the <code>Entity</code> is initialized,
     * which is too late to allow main gun armor, hence the convoluted reverse
     * logic.
     */
    private boolean m_bHasNoMainGun = false;

    public static final int LOC_HEAD = 0;
    public static final int LOC_TORSO = 1;

    public static final int LOC_RARM = 2;
    public static final int LOC_LARM = 3;

    public static final int LOC_LEG = 4;
    public static final int LOC_MAINGUN = 5;

    // Near miss reprs.
    public static final int LOC_NMISS = 6;

    // "Systems". These represent protomech critical hits; which remain constant
    // regardless of proto.
    // doesn't matter what gets hit in a proto section, just the number of times
    // it's been critted
    // so just have the right number of these systems and it works.
    public static final int SYSTEM_ARMCRIT = 0;
    public static final int SYSTEM_LEGCRIT = 1;
    public static final int SYSTEM_HEADCRIT = 2;
    public static final int SYSTEM_TORSOCRIT = 3;
    public static final int SYSTEM_TORSO_WEAPON_A = 4;
    public static final int SYSTEM_TORSO_WEAPON_B = 5;

    private static final int[] NUM_OF_SLOTS = { 2, 3, 2, 2, 3, 0 };

    public static final int[] POSSIBLE_PILOT_DAMAGE = { 1, 3, 1, 1, 1, 0 };

    public static final String systemNames[] = { "Arm", "Leg", "Head", "Torso" };

    // For grapple attacks
    private int grappled_id = Entity.NONE;

    private boolean isGrappleAttacker = false;

    /**
     * Construct a new, blank, pmech.
     */
    public Protomech() {
        super();
        setCritical(LOC_HEAD, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_HEADCRIT));
        setCritical(LOC_HEAD, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_HEADCRIT));
        setCritical(LOC_RARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_ARMCRIT));
        setCritical(LOC_RARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_ARMCRIT));
        setCritical(LOC_LARM, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_ARMCRIT));
        setCritical(LOC_LARM, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_ARMCRIT));
        setCritical(LOC_TORSO, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_TORSOCRIT));
        setCritical(LOC_TORSO, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_TORSOCRIT));
        setCritical(LOC_TORSO, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_TORSOCRIT));
        setCritical(LOC_LEG, 0, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LEGCRIT));
        setCritical(LOC_LEG, 1, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LEGCRIT));
        setCritical(LOC_LEG, 2, new CriticalSlot(CriticalSlot.TYPE_SYSTEM,
                SYSTEM_LEGCRIT));
        bHasMainGun = false;
        bHasRArmGun = false;
        bHasLArmGun = false;
        bHasTorsoAGun = false;
        bHasTorsoBGun = false;
        m_bHasNoMainGun = true;
    }

    @Override
    protected int[] getNoOfSlots() {
        return NUM_OF_SLOTS;
    }

    /**
     * Returns # of pilot damage points taken due to crits to the location so
     * far.
     */
    public int getPilotDamageTaken(int loc) {
        return PilotDamageTaken[loc];
    }

    /**
     * Get the weapon in the given torso location (if any).
     *
     * @param isTorsoA - a <code>boolean</code> that is <code>true</code> if
     *            the weapon in "Torso A" is needed; <code>false</code> if the
     *            weapon in "Torso B" is needed.
     * @return the <code>Mounted</code> weapon at the needed location. This
     *         value will be <code>null</code> if no weapon is in the
     *         indicated location.
     */
    public Mounted getTorsoWeapon(boolean isTorsoA) {
        Mounted weapon = null;
        if (isTorsoA && bHasTorsoAGun) {
            weapon = getEquipment(torsoAGunNum);
        } else if (!isTorsoA && bHasTorsoBGun) {
            weapon = getEquipment(torsoBGunNum);
        }
        return weapon;
    }

    /**
     * Tells the Protomech to note pilot damage taken from crit damage to the
     * location
     */
    public void setPilotDamageTaken(int loc, int damage) {
        PilotDamageTaken[loc] = damage;
    }

    /**
     * Protos don't take piloting skill rolls.
     */
    //TODO: this is no longer true in TacOps. Protos sometimes make PSRs using their gunnery skill
    @Override
    public PilotingRollData getBasePilotingRoll() {
        return new PilotingRollData(getId(), TargetRoll.CHECK_FALSE,
                "Protomeks never take PSRs.");
    }

    /**
     * A "shaded" critical is a box shaded on the record sheet, implies pilot
     * damage when hit. Returns whether shaded.
     */
    public boolean shaded(int loc, int numHit) {
        switch (loc) {
            case LOC_HEAD:
            case LOC_LARM:
            case LOC_RARM:
                return (2 == numHit);
            case LOC_TORSO:
                return (0 < numHit);
            case LOC_MAINGUN:
            case LOC_NMISS:
                return false;
            case LOC_LEG:
                return (3 == numHit);
        }
        return false;
    }

    @Override
    public int getWalkMP(boolean gravity, boolean ignoreheat) {
        int wmp = getOriginalWalkMP();
        int legCrits = getCritsHit(LOC_LEG);
        int j;
        if(null != game) {
            int weatherMod = game.getPlanetaryConditions().getMovementMods(this);
            if(weatherMod != 0) {
                wmp = Math.max(wmp + weatherMod, 0);
            }
        }
        // Gravity, Protos can't get faster
        if (gravity) {
            j = applyGravityEffectsOnMP(wmp);
        } else {
            j = wmp;
        }
        if (j < wmp) {
            wmp = j;
        }
        switch (legCrits) {
            case 0:
                break;
            case 1:
                wmp--;
                break;
            case 2:
                wmp = wmp / 2;
                break;
            case 3:
                wmp = 0;
                break;
        }
        return wmp;
    }

    /**
     * Counts the # of crits taken by proto in the location. Needed in several
     * places, due to proto set criticals.
     */
    public int getCritsHit(int loc) {
        int count = 0;
        int numberOfCriticals = this.getNumberOfCriticals(loc);
        for (int i = 0; i < numberOfCriticals; i++) {
            CriticalSlot ccs = getCritical(loc, i);
            if (ccs.isDamaged() || ccs.isBreached()) {
                count++;
            }
        }
        return count;
    }

    public static int getInnerLocation(int location) {
        return LOC_TORSO;
    }

    /**
     * Add in any piloting skill mods
     */
    @Override
    public PilotingRollData addEntityBonuses(PilotingRollData roll) {
        return roll;
    }

    /**
     * Returns the number of total critical slots in a location
     */
    @Override
    public int getNumberOfCriticals(int loc) {
        switch (loc) {
            case LOC_MAINGUN:
                return 0;
            case LOC_HEAD:
            case LOC_LARM:
            case LOC_RARM:
                return 2;
            case LOC_LEG:
            case LOC_TORSO:
                return 3;
        }
        return 0;
    }

    /**
     * Override Entity#newRound() method.
     */
    @Override
    public void newRound(int roundNumber) {

        setSecondaryFacing(getFacing());
        super.newRound(roundNumber);

    } // End public void newRound()

    /**
     * This pmech's jumping MP modified for missing jump jets and gravity.
     */
    @Override
    public int getJumpMP() {
        int jump = jumpMP;
        int torsoCrits = getCritsHit(LOC_TORSO);
        switch (torsoCrits) {
            case 0:
                break;
            case 1:
                if (jump > 0) {
                    jump--;
                }
                break;
            case 2:
                jump = jump / 2;
                break;
        }
        if (applyGravityEffectsOnMP(jump) > jump) {
            return jump;
        }
        return applyGravityEffectsOnMP(jump);
    }

    /**
     * Returns this mech's jumping MP, modified for missing & underwater jets.
     */
    @Override
    public int getJumpMPWithTerrain() {
        if (getPosition() == null) {
            return getJumpMP();
        }

        int waterLevel = game.getBoard().getHex(getPosition()).terrainLevel(
                Terrains.WATER);
        if ((waterLevel <= 0) || (getElevation() >= 0)) {
            return getJumpMP();
        }
        return 0;
    }

    @Override
    public int getHeatCapacityWithWater() {
        return getHeatCapacity();
    }

    /**
     * Returns the amount of heat that the entity can sink each turn. Pmechs
     * have no heat. //FIXME However, the number of heat sinks they have IS
     * importnat... For cost and validation purposes.
     */
    @Override
    public int getHeatCapacity() {

        return 999;
    }

    @Override
    public String[] getLocationNames() {
        return LOCATION_NAMES;
    }

    /**
     * Returns the name of the type of movement used. This is pmech-specific.
     */
    @Override
    public String getMovementString(int mtype) {
        switch (mtype) {
            case IEntityMovementType.MOVE_NONE:
                return "None";
            case IEntityMovementType.MOVE_WALK:
                return "Walked";
            case IEntityMovementType.MOVE_RUN:
                return "Ran";
            case IEntityMovementType.MOVE_JUMP:
                return "Jumped";
            default:
                return "Unknown!";
        }
    }

    /**
     * Returns the name of the type of movement used. This is pmech-specific.
     */
    @Override
    public String getMovementAbbr(int mtype) {
        switch (mtype) {
            case IEntityMovementType.MOVE_NONE:
                return "N";
            case IEntityMovementType.MOVE_WALK:
                return "W";
            case IEntityMovementType.MOVE_RUN:
                return "R";
            case IEntityMovementType.MOVE_JUMP:
                return "J";
            default:
                return "?";
        }
    }

    @Override
    public boolean canChangeSecondaryFacing() {
        return !(getCritsHit(LOC_LEG) > 2);
    }

    @Override
    public int getEngineCritHeat() {
        return 0;
    }

    /**
     * Can this pmech torso twist in the given direction?
     */
    @Override
    public boolean isValidSecondaryFacing(int dir) {
        int rotate = dir - getFacing();
        if (canChangeSecondaryFacing()) {
            return (rotate == 0) || (rotate == 1) || (rotate == -1) || (rotate == -5);
        }
        return rotate == 0;
    }

    /**
     * Return the nearest valid direction to torso twist in
     */
    @Override
    public int clipSecondaryFacing(int dir) {
        if (isValidSecondaryFacing(dir)) {
            return dir;
        }
        // otherwise, twist once in the appropriate direction
        final int rotate = (dir + (6 - getFacing())) % 6;
        return rotate >= 3 ? (getFacing() + 5) % 6 : (getFacing() + 1) % 6;
    }

    @Override
    public boolean hasRearArmor(int loc) {
        return false;
    }

    @Override
    public int getRunMPwithoutMASC(boolean gravity, boolean ignoreheat) {
        return getRunMP(gravity, ignoreheat);
    }

    /**
     * Returns the Compute.ARC that the weapon fires into.
     */
    @Override
    public int getWeaponArc(int wn) {
        final Mounted mounted = getEquipment(wn);
        // rear mounted?
        if (mounted.isRearMounted()) {
            return Compute.ARC_REAR;
        }
        // front mounted
        switch (mounted.getLocation()) {
            case LOC_TORSO:
                return Compute.ARC_FORWARD;
            case LOC_RARM:
                return Compute.ARC_RIGHTARM;
            case LOC_LARM:
                return Compute.ARC_LEFTARM;
            case LOC_MAINGUN:
                return Compute.ARC_MAINGUN;
            default:
                return Compute.ARC_360;
        }
    }

    /**
     * Returns true if this weapon fires into the secondary facing arc. If
     * false, assume it fires into the primary.
     */
    @Override
    public boolean isSecondaryArcWeapon(int weaponId) {
        return true;
    }

    /**
     * Rolls up a hit location
     */
    @Override
    public HitData rollHitLocation(int table, int side) {
        return rollHitLocation(table, side, LOC_NONE,
                IAimingModes.AIM_MODE_NONE);
    }

    @Override
    public HitData rollHitLocation(int table, int side, int aimedLocation,
            int aimingMode) {
        int roll = -1;

        if ((aimedLocation != LOC_NONE)
                && (aimingMode == IAimingModes.AIM_MODE_IMMOBILE)) {
            roll = Compute.d6(2);

            if ((5 < roll) && (roll < 9)) {
                return new HitData(aimedLocation, side == ToHitData.SIDE_REAR,
                        true);
            }

        }

        roll = Compute.d6(2);
        try {
            PrintWriter pw = PreferenceManager.getClientPreferences()
                    .getMekHitLocLog();

            if (pw != null) {
                pw.print(table);
                pw.print("\t");
                pw.print(side);
                pw.print("\t");
                pw.println(roll);
            }
        } catch (Throwable thrown) {
            thrown.printStackTrace();
        }

        switch (roll) {
            case 2:
                return new HitData(Protomech.LOC_MAINGUN);
            case 3:
            case 11:
                return new HitData(Protomech.LOC_NMISS);
            case 4:
                return new HitData(Protomech.LOC_RARM);
            case 5:
            case 9:
                return new HitData(Protomech.LOC_LEG);
            case 6:
            case 7:
            case 8:
                return new HitData(Protomech.LOC_TORSO);
            case 10:
                return new HitData(Protomech.LOC_RARM);
            case 12:
                return new HitData(Protomech.LOC_HEAD);

        }

        return null;
    }

    /**
     * Protos can't transfer crits.
     */

    @Override
    public boolean canTransferCriticals(int loc) {
        return false;
    }

    /**
     * Gets the location that excess damage transfers to
     */
    @Override
    public HitData getTransferLocation(HitData hit) {
        switch (hit.getLocation()) {
            case LOC_NMISS:
                return new HitData(LOC_NONE);
            case LOC_LARM:
            case LOC_LEG:
            case LOC_RARM:
            case LOC_HEAD:
            case LOC_MAINGUN:
                return new HitData(LOC_TORSO, hit.isRear(), hit.getEffect(),
                        hit.hitAimedLocation(), hit.getSpecCritMod(), hit
                                .isFromFront(), hit.getGeneralDamageType(), hit
                                .glancingMod());
            case LOC_TORSO:
            default:
                return new HitData(LOC_DESTROYED);
        }
    }

    /**
     * Gets the location that is destroyed recursively
     */
    @Override
    public int getDependentLocation(int loc) {

        return LOC_NONE;
    }

    /**
     * Sets the internal structure for the pmech.
     *
     * @param head head
     * @param torso center torso
     * @param arm right/left arm
     * @param legs right/left leg
     * @param mainGun main gun
     */
    public void setInternal(int head, int torso, int arm, int legs, int mainGun) {
        initializeInternal(head, LOC_HEAD);
        initializeInternal(torso, LOC_TORSO);
        initializeInternal(arm, LOC_RARM);
        initializeInternal(arm, LOC_LARM);
        initializeInternal(legs, LOC_LEG);
        initializeInternal(mainGun, LOC_MAINGUN);
    }

    /**
     * Set the internal structure to the appropriate value for the pmech's
     * weight class
     */
    @Override
    public void autoSetInternal() {
        int mainGunIS = hasMainGun() ? 1 : IArmorState.ARMOR_NA;
        switch ((int) weight) {
            // H, TSO,ARM,LEGS,MainGun
            case 2:
                setInternal(1, 2, 1, 2, mainGunIS);
                break;
            case 3:
                setInternal(1, 3, 1, 2, mainGunIS);
                break;
            case 4:
                setInternal(1, 4, 1, 3, mainGunIS);
                break;
            case 5:
                setInternal(1, 5, 1, 3, mainGunIS);
                break;
            case 6:
                setInternal(2, 6, 2, 4, mainGunIS);
                break;
            case 7:
                setInternal(2, 7, 2, 4, mainGunIS);
                break;
            case 8:
                setInternal(2, 8, 2, 5, mainGunIS);
                break;
            case 9:
                setInternal(2, 9, 2, 5, mainGunIS);
                break;
        }
    }

    /**
     * Creates a new mount for this equipment and adds it in.
     */
    @Override
    public Mounted addEquipment(EquipmentType etype, int loc)
            throws LocationFullException {
        return addEquipment(etype, loc, false, -1);
    }

    @Override
    public Mounted addEquipment(EquipmentType etype, int loc,
            boolean rearMounted) throws LocationFullException {
        Mounted mounted = new Mounted(this, etype);
        addEquipment(mounted, loc, rearMounted, -1);
        return mounted;
    }

    @Override
    public Mounted addEquipment(EquipmentType etype, int loc,
            boolean rearMounted, int shots) throws LocationFullException {
        Mounted mounted = new Mounted(this, etype);
        addEquipment(mounted, loc, rearMounted, shots);
        return mounted;

    }

    /**
     * Mounts the specified weapon in the specified location.
     */
    @Override
    protected void addEquipment(Mounted mounted, int loc, boolean rearMounted,
            int shots) throws LocationFullException {
        if (mounted.getType() instanceof AmmoType) {
            // Damn protomech ammo; nasty hack, should be cleaner
            if (-1 != shots) {
                mounted.setShotsLeft(shots);
                super.addEquipment(mounted, loc, rearMounted);
                return;
            }
        }

        if (mounted.getType() instanceof WeaponType) {
            switch (loc) {
                case LOC_HEAD:
                case LOC_LEG:
                case LOC_NMISS:
                    throw new LocationFullException("Weapon "
                            + mounted.getName() + " can't be mounted in "
                            + getLocationAbbr(loc));
                case LOC_MAINGUN:
                    if (bHasMainGun) {
                        throw new LocationFullException("Already has Main Gun");
                    }
                    bHasMainGun = true;
                    mounted.setLocation(loc, rearMounted);
                    equipmentList.add(mounted);
                    weaponList.add(mounted);
                    totalWeaponList.add(mounted);
                    break;
                case LOC_LARM:
                    if (bHasLArmGun) {
                        throw new LocationFullException("Already has LArm Gun");
                    }
                    bHasLArmGun = true;
                    mounted.setLocation(loc, rearMounted);
                    equipmentList.add(mounted);
                    weaponList.add(mounted);
                    totalWeaponList.add(mounted);
                    break;
                case LOC_RARM:
                    if (bHasRArmGun) {
                        throw new LocationFullException("Already has RArm Gun");
                    }
                    bHasRArmGun = true;
                    mounted.setLocation(loc, rearMounted);
                    equipmentList.add(mounted);
                    weaponList.add(mounted);
                    totalWeaponList.add(mounted);
                    break;
                case LOC_TORSO:
                    if (bHasTorsoAGun) {
                        if (bHasTorsoBGun) {
                            throw new LocationFullException(
                                    "Already has both torso guns");
                        }
                        bHasTorsoBGun = true;
                        mounted.setLocation(loc, rearMounted);
                        equipmentList.add(mounted);
                        weaponList.add(mounted);
                        totalWeaponList.add(mounted);
                        torsoBGunNum = getEquipmentNum(mounted);
                    } else {
                        bHasTorsoAGun = true;
                        mounted.setLocation(loc, rearMounted);
                        equipmentList.add(mounted);
                        weaponList.add(mounted);
                        totalWeaponList.add(mounted);
                        torsoAGunNum = getEquipmentNum(mounted);
                    }
                    break;
            }
        } else {
            super.addEquipment(mounted, loc, rearMounted);
        }
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
     * Calculates the battle value of this pmech.
     */
    @Override
    public int calculateBattleValue(boolean ignoreC3, boolean ignorePilot) {
        double dbv = 0; // defensive battle value
        double obv = 0; // offensive bv

        // total armor points
        dbv += getTotalArmor() * 2.5;

        // total internal structure
        dbv += getTotalInternal() * 1.5;

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
                    || ((etype instanceof MiscType) && (etype
                            .hasFlag(MiscType.F_ECM) || etype
                            .hasFlag(MiscType.F_BAP)))) {
                dEquipmentBV += etype.getBV(this);
            }
        }
        dbv += dEquipmentBV;

        // adjust for target movement modifier
        double tmmRan = Compute.getTargetMovementModifier(getRunMP(false, true),
                false, false).getValue();
        double tmmFactor = 1 + (tmmRan / 10) + 0.1;
        dbv *= tmmFactor;

        double weaponBV = 0;

        // figure out base weapon bv
        boolean hasTargComp = hasTargComp();
        // and add up BVs for ammo-using weapon types for excessive ammo rule
        Map<String, Double> weaponsForExcessiveAmmo = new HashMap<String, Double>();
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
                dBV *= 1.25;
            }
            weaponBV += dBV;
            // add up BV of ammo-using weapons for each type of weapon,
            // to compare with ammo BV later for excessive ammo BV rule
            if (!(wtype.hasFlag(WeaponType.F_ENERGY)
                    || wtype.hasFlag(WeaponType.F_ONESHOT)
                    || wtype.hasFlag(WeaponType.F_INFANTRY) || (wtype
                    .getAmmoType() == AmmoType.T_NA))) {
                String key = wtype.getAmmoType() + ":" + wtype.getRackSize();
                if (!weaponsForExcessiveAmmo.containsKey(key)) {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this));
                } else {
                    weaponsForExcessiveAmmo.put(key, wtype.getBV(this)
                            + weaponsForExcessiveAmmo.get(key));
                }
            }
        }

        // add ammo bv
        double ammoBV = 0;
        // extra BV for when we have semiguided LRMs and someone else has TAG on
        // our team
        double tagBV = 0;
        Map<String, Double> ammo = new HashMap<String, Double>();
        ArrayList<String> keys = new ArrayList<String>();
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

            // don't count oneshot ammo, it's considered part of the launcher.
            if (mounted.getLocation() == Entity.LOC_NONE) {
                // assumption: ammo without a location is for a oneshot weapon
                continue;
            }
            // semiguided or homing ammo might count double
            if ((atype.getMunitionType() == AmmoType.M_SEMIGUIDED)
                    || (atype.getMunitionType() == AmmoType.M_HOMING)) {
                Player tmpP = getOwner();
                // Okay, actually check for friendly TAG.
                if (tmpP.hasTAG()) {
                    tagBV += atype.getBV(this);
                } else if ((tmpP.getTeam() != Player.TEAM_NONE) && (game != null)) {
                    for (Enumeration<Team> e = game.getTeams(); e
                            .hasMoreElements();) {
                        Team m = e.nextElement();
                        if (m.getId() == tmpP.getTeam()) {
                            if (m.hasTAG(game)) {
                                tagBV += atype.getBV(this);
                            }
                            // A player can't be on two teams.
                            // If we check his team and don't give the penalty,
                            // that's it.
                            break;
                        }
                    }
                }
            }
            String key = atype.getAmmoType() + ":" + atype.getRackSize();
            if (!keys.contains(key)) {
                keys.add(key);
            }
            if (!ammo.containsKey(key)) {
                ammo.put(key, atype.getProtoBV(mounted.getShotsLeft()));
            } else {
                ammo.put(key, atype.getProtoBV(mounted.getShotsLeft()) + ammo.get(key));
            }
        }
        // excessive ammo rule:
        // only count BV for ammo for a weapontype until the BV of all weapons
        // of that
        // type on the mech is reached
        for (String key : keys) {
            if (weaponsForExcessiveAmmo.containsKey(key)
                    && (ammo.get(key) > weaponsForExcessiveAmmo.get(key))) {
                ammoBV += weaponsForExcessiveAmmo.get(key);
            } else {
                ammoBV += ammo.get(key);
            }
        }
        weaponBV += ammoBV;

        // add offensive misc. equipment BV (everything except AMS, A-Pod, ECM -
        // BMR p152)
        double oEquipmentBV = 0;
        for (Mounted mounted : getMisc()) {
            MiscType mtype = (MiscType) mounted.getType();

            // don't count destroyed equipment
            if (mounted.isDestroyed()) {
                continue;
            }

            if (mtype.hasFlag(MiscType.F_ECM)
                    || mtype.hasFlag(MiscType.F_AP_POD)
                    || mtype.hasFlag(MiscType.F_BAP)
                    || mtype.hasFlag(MiscType.F_TARGCOMP)) {
                // weapons
                continue;
            }
            oEquipmentBV += mtype.getBV(this);
        }

        weaponBV += oEquipmentBV;

        // adjust further for speed factor
        double speedFactor = Math.pow(1 + (((double) getRunMP(false, true)
                + (Math.round((double) getJumpMP(false) / 2)) - 5) / 10), 1.2);
        speedFactor = Math.round(speedFactor * 100) / 100.0;

        obv = weaponBV * speedFactor;

        // we get extra bv from some stuff
        double xbv = 0.0;
        // extra BV for semi-guided lrm when TAG in our team
        xbv += tagBV;

        int finalBV = (int) Math.round(dbv + obv + xbv);

        // and then factor in pilot
        double pilotFactor = 1;
        if (!ignorePilot) {
            pilotFactor = crew.getBVSkillMultiplier();
        }

        int retVal = (int) Math.round((finalBV) * pilotFactor);
        return retVal;
    }

    @Override
    public Vector<Report> victoryReport() {
        Vector<Report> vDesc = new Vector<Report>();

        Report r = new Report(7025);
        r.type = Report.PUBLIC;
        r.addDesc(this);
        vDesc.addElement(r);

        r = new Report(7030);
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

    @Override
    public int getMaxElevationChange() {
        return 1;
    }

    @Override
    public int getArmor(int loc, boolean rear) {
        if (loc == LOC_NMISS) {
            return IArmorState.ARMOR_NA;
        }
        return super.getArmor(loc, rear);
    }

    @Override
    public int getInternal(int loc) {
        if (loc == LOC_NMISS) {
            return IArmorState.ARMOR_NA;
        }
        return super.getInternal(loc);
    }

    @Override
    protected String[] getLocationAbbrs() {
        return LOCATION_ABBRS;
    }

    @Override
    public String getLocationAbbr(int loc) {
        if (loc == LOC_NMISS) {
            return "a near miss";
        }
        return super.getLocationAbbr(loc);
    }

    /*
     * * Not every Protomech has a main gun.
     */
    public boolean hasMainGun() {
        return !m_bHasNoMainGun;
    }

    /*
     * * Not every Protomech has a main gun.
     */
    public void setHasMainGun(boolean b) {
        m_bHasNoMainGun = !b;
    }

    /**
     * Returns the number of locations in the entity
     */
    @Override
    public int locations() {
        if (m_bHasNoMainGun) {
            return NUM_PMECH_LOCATIONS - 1;
        }
        return NUM_PMECH_LOCATIONS;
    }

    /**
     * Protomechs have no piloting skill (set to 5 for BV purposes)
     */
    @Override
    public void setCrew(Pilot p) {
        super.setCrew(p);
        getCrew().setPiloting(5);
    }

    @Override
    public boolean canCharge() {
        // Protos can't Charge
        return false;
    }

    @Override
    public boolean canDFA() {
        // Protos can't DFA
        return false;
    }

    /**
     * @return The cost in C-Bills of the ProtoMech in question.
     */
    @Override
    public double getCost() {
        double retVal = 0;

        // Add the cockpit, a constant cost.
        retVal += 500000;

        // Add life support, a constant cost.
        retVal += 75000;

        // Sensor cost is based on tonnage.
        retVal += 2000 * weight;

        // Musculature cost is based on tonnage.
        retVal += 2000 * weight;

        // Internal Structure cost is based on tonnage.
        retVal += 400 * weight;

        // Arm actuators are based on tonnage.
        // Their cost is listed separately?
        retVal += 2 * 180 * weight;

        // Leg actuators are based on tonnage.
        retVal += 540 * weight;

        // Engine cost is based on tonnage and rating.
        if (getEngine() != null) {
            retVal += (5000 * weight * getEngine().getRating()) / 75;
        }

        // Jump jet cost is based on tonnage and jump MP.
        retVal += weight * getJumpMP() * getJumpMP() * 200;

        // Heat sinks is constant per sink.
        // per the construction rules, we need enough sinks to sink all energy
        // weapon heat, so we just calculate the cost that way.
        int sinks = 0;
        for (Mounted mount : getWeaponList()) {
            if (mount.getType().hasFlag(WeaponType.F_ENERGY)) {
                WeaponType wtype = (WeaponType)mount.getType();
                sinks += wtype.getHeat();
            }
        }
        retVal += 2000*sinks;

        // Armor is linear on the armor value of the Protomech
        retVal += getTotalArmor() * 625;

        // Add in equipment cost.
        retVal += getWeaponsAndEquipmentCost();

        // Finally, apply the Final ProtoMech Cost Multiplier
        retVal *= 1 + (weight / 100.0);

        return retVal;
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
    public boolean hasActiveEiCockpit() {
        return (super.hasActiveEiCockpit() && (getCritsHit(LOC_HEAD) == 0));
    }

    @Override
    public boolean canAssaultDrop() {
        return true;
    }

    @Override
    public boolean isHexProhibited(IHex hex) {
        if (hex.containsTerrain(Terrains.IMPASSABLE)) {
            return true;
        }

        if (hex.containsTerrain(Terrains.SPACE) && doomedInSpace()) {
            return true;
        }

        return (hex.terrainLevel(Terrains.WOODS) > 2) || (hex.terrainLevel(Terrains.JUNGLE) > 2);
    }

    @Override
    public boolean isNuclearHardened() {
        return true;
    }
    @Override
    public void setGrappled(int id, boolean attacker) {
        grappled_id = id;
        isGrappleAttacker = attacker;
    }

    @Override
    public boolean isGrappleAttacker() {
        return isGrappleAttacker;
    }

    @Override
    public int getGrappled() {
        return grappled_id;
    }

    @Override
    public boolean isEligibleForMovement() {
        if (grappled_id != Entity.NONE) {
            return false;
        }
        return super.isEligibleForMovement();
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#getTotalCommGearTons()
     */
    @Override
    public int getTotalCommGearTons() {
        return 0;
    }

    /*
     * (non-Javadoc)
     * @see megamek.common.Entity#checkSkid(int, megamek.common.IHex, int, megamek.common.MoveStep, int, int, megamek.common.Coords, megamek.common.Coords, boolean, int)
     */
    @Override
    public PilotingRollData checkSkid(int moveType, IHex prevHex, int overallMoveType, MoveStep prevStep, int prevFacing, int curFacing, Coords lastPos, Coords curPos, boolean isInfantry, int distance) {
        return new PilotingRollData(getId(), TargetRoll.CHECK_FALSE, "ProtoMechs can't skid");
    }
}
