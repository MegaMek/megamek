/*
 * MegaMek -
 * Copyright (C) 2000,2001,2002,2003,2004,2005 Ben Mazur (bmazur@sev.org)
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

/*
 * Mounted.java
 *
 * Created on April 1, 2002, 1:29 PM
 */

package megamek.common;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import megamek.common.actions.WeaponAttackAction;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.OptionsConstants;
import megamek.common.options.WeaponQuirks;
import megamek.common.weapons.AmmoBayWeapon;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.GaussWeapon;
import megamek.common.weapons.WeaponHandler;

/**
 * This describes equipment mounted on a mech.
 *
 * @author Ben
 * @version
 */
public class Mounted implements Serializable, RoundUpdated, PhaseUpdated {

    private static final long serialVersionUID = 6438017987074691566L;
    private boolean usedThisRound = false;
    private boolean destroyed = false;
    private boolean hit = false;
    private boolean missing = false;
    private boolean jammed = false;
    private boolean jammedThisPhase = false;
    private boolean useless = false;
    private boolean fired = false; // Only true for used OS stuff and TSEMP.
    private boolean tsempDowntime = false; // Needed for "every other turn"
                                           // TSEMP.
    private boolean rapidfire = false; // MGs in rapid-fire mode
    private boolean kindRapidFire = false; // Reduced jam chance for rapid fired
                                           // ACs.
    private boolean hotloaded = false; // Hotloading for ammoType
    private boolean repairable = true; // can the equipment mounted here be
    // repaired
    private boolean mechTurretMounted = false; // is this mounted in a
                                               // mechturret
    private boolean sponsonTurretMounted = false; // is this mounted in a
                                                  // sponsonturret
    private boolean pintleTurretMounted = false; // is this mounted in a
                                                 // pintleturret
    private int facing = -1; // facing for turrets

    private int mode; // Equipment's current state. On or Off. Sixshot or
    // Fourshot, etc
    private int pendingMode = -1; // if mode changes happen at end of turn
    private boolean modeSwitchable = true; // disallow mode switching

    private int location;
    private boolean rearMounted;

    private Mounted linked = null; // for ammo, or artemis
    private Mounted linkedBy = null; // reverse link for convenience

    private Mounted crossLinkedBy = null; // Weapons with crossLinked capacitors

    private Entity entity; // what I'm mounted on

    private WeaponQuirks quirks = new WeaponQuirks();

    private transient EquipmentType type;
    private String typeName;

    // ammo-specific stuff. Probably should be a subclass
    private int shotsLeft;
    // how many shots did we have originally?
    // only used for by-shot ammo
    private int originalShots;
    private boolean m_bPendingDump;
    private boolean m_bDumping;

    // A list of ids (equipment numbers) for the weapons and ammo linked to
    // this bay (if the mounted is of the BayWeapon type)
    // I can also use this for weapons of the same type on a capital fighter
    //and now Machine Gun Arrays too!
    private Vector<Integer> bayWeapons = new Vector<Integer>();
    private Vector<Integer> bayAmmo = new Vector<Integer>();

    // on capital fighters and squadrons some weapon mounts actually represent
    // multiple weapons of the same type
    // provide a boolean indicating this type of mount and the number of weapons
    // represented
    private boolean weaponGroup = false;
    private int nweapons = 1;

    // for ammo loaded by shot rather than ton, a boolean
    private boolean byShot = false;

    // handle split weapons
    private boolean bSplit = false;
    private int nFoundCrits = 0;
    private int secondLocation = 0;

    // bomb stuff
    private boolean bombMounted = false;

    // mine type
    private int mineType = MINE_NONE;
    // vibrabomb mine setting
    private int vibraSetting = 20;

    //These arrays are used to track individual missing modular components on BA for MHQ
    //in MM they probably shouldn't need to be touched. They are used to keep track of
    //whether a modular mount is in use or not for a particular trooper.
    private boolean[] missingForTrooper = {false, false, false, false, false, false};

    /**
     * Armor value, used for applicable equipment types like minesweepers.
     */
    private int armorValue = 0;

    private IGame.Phase phase = IGame.Phase.PHASE_UNKNOWN;

    public static final int MINE_NONE = -1;
    public static final int MINE_CONVENTIONAL = 0;
    public static final int MINE_VIBRABOMB = 1;
    public static final int MINE_ACTIVE = 2;
    public static final int MINE_INFERNO = 3;
    public static final int MINE_EMP = 4;
    public static final int MINE_COMMAND_DETONATED = 5;

    // New stuff for shields
    protected int baseDamageAbsorptionRate = 0;
    protected int baseDamageCapacity = 0;
    protected int damageTaken = 0;

    // this is a hack but in the case of Killer Whale ammo
    // I need some way of tracking how many missiles are Santa Annas
    private int nSantaAnna = 0;

    /**
     * BA use locations for troopers, so we need a way to keep track of where
     *  a piece of equipment is moutned on BA
     */
    private int baMountLoc = BattleArmor.MOUNT_LOC_NONE;

    /**
     *  For BA weapons, is this in a detachable weapon pack?
     */
    private boolean isDWPMounted = false;

    /**
     * Does this Mounted represent a weapon that is mounted in an anti-personnel
     * weapon mount?
     */
    private boolean isAPMMounted = false;

    // for Armored components
    private boolean armoredComponent = false;

    // called shots status, sort of like another mode
    private CalledShot called = new CalledShot();

    /**
     * Flag that keeps track of whether this <code>Mounted</code> is mounted as
     * a squad support weapon on <code>BattleArmor</code>.
     */
    private boolean squadSupportWeapon;

    /** Creates new Mounted */
    public Mounted(Entity entity, EquipmentType type) {
        this.entity = entity;
        this.type = type;
        typeName = type.getInternalName();

        if (type instanceof AmmoType) {
            shotsLeft = ((AmmoType) type).getShots();
        }
        if ((type instanceof MiscType) && type.hasFlag(MiscType.F_MINE)) {
            mineType = MINE_CONVENTIONAL;
            // Used to keep track of the # of mines
            shotsLeft = 1;
        }
        if ((type instanceof MiscType) &&
                type.hasFlag(MiscType.F_VEHICLE_MINE_DISPENSER)) {
            mineType = MINE_CONVENTIONAL;
            // Used to keep track of the # of mines
            shotsLeft = 2;
        }
        if ((type instanceof MiscType)
                && type.hasFlag(MiscType.F_SENSOR_DISPENSER)) {
            setShotsLeft(30);
        }
        if ((type instanceof MiscType)
                && ((((MiscType) type).isShield() || type
                        .hasFlag(MiscType.F_MODULAR_ARMOR)))) {
            MiscType shield = (MiscType) type;
            baseDamageAbsorptionRate = shield.baseDamageAbsorptionRate;
            baseDamageCapacity = shield.baseDamageCapacity;
            damageTaken = shield.damageTaken;
        }
        if ((type instanceof MiscType)
                && type.hasFlag(MiscType.F_MINESWEEPER)) {
            armorValue = 30;
        }

        quirks.initialize();
    }

    /**
     * Changing ammo loadouts allows updating AmmoTypes of existing bins. This
     * is the only circumstance under which this should happen.
     */

    public void changeAmmoType(AmmoType at) {
        if (!(type instanceof AmmoType)) {
            System.out.println("Attempted to change ammo type of non-ammo");
            return;
        }
        type = at;
        typeName = at.getInternalName();
        if (location == Entity.LOC_NONE) {
            // Oneshot launcher
            shotsLeft = 1;
        } else {
            // Regular launcher
            shotsLeft = at.getShots();
        }
    }

    /**
     * Restores the equipment from the name
     */
    public void restore() {
        if (typeName == null) {
            typeName = type.getName();
        } else {
            type = EquipmentType.get(typeName);
        }

        if (type == null) {
            System.err
                    .println("Mounted.restore: could not restore equipment type \""
                            + typeName + "\"");
        }
    }

    public EquipmentType getType() {
        return type;
    }

    /**
     * @return the current mode of the equipment, or <code>null</code> if it's
     *         not available.
     */
    public EquipmentMode curMode() {
        if ((mode >= 0) && (mode < type.getModesCount())) {
            return type.getMode(mode);
        }
        return EquipmentMode.getMode("None");
    }

    /**
     * @return the pending mode of the equipment.
     */
    public EquipmentMode pendingMode() {
        if ((pendingMode < 0) || (pendingMode >= type.getModesCount())) {
            return EquipmentMode.getMode("None");
        }
        return type.getMode(pendingMode);
    }

    /**
     * Switches the equipment mode to the next available.
     *
     * @return new mode number, or <code>-1</code> if it's not available.
     */
    public int switchMode() {
        if (type.hasModes()) {
            int nMode = 0;
            if (pendingMode > -1) {
                nMode = (pendingMode + 1) % type.getModesCount();
            } else {
                nMode = (mode + 1) % type.getModesCount();
            }
            setMode(nMode);
            return nMode;
        }
        return -1;
    }

    /**
     * Sets the equipment mode to the mode denoted by the given mode name
     *
     * @param newMode
     *            the name of the desired new mode
     * @return new mode number on success, <code>-1<code> otherwise.
     */
    public int setMode(String newMode) {
        for (int x = 0, e = type.getModesCount(); x < e; x++) {
            if (type.getMode(x).equals(newMode)) {
                setMode(x);
                return x;
            }
        }
        return -1;
    }

    /**
     * Sets the equipment mode to the mode denoted by the given mode number
     *
     * @param newMode
     *            the number of the desired new mode
     */
    public boolean setMode(int newMode) {
        if (type.hasModes()) {

            if (newMode >= type.getModesCount()) {
                return false;
            }
            /*
             * megamek.debug.Assert.assertTrue(newMode >= 0 && newMode <
             * type.getModesCount(), "Invalid mode, mode=" + newMode +
             * ", modesCount=" + type.getModesCount());
             */

            if (canInstantSwitch(newMode)) {
                mode = newMode;
                pendingMode = -1;
            } else if (pendingMode != newMode) {
                pendingMode = newMode;
            }
        }
        // all communicationsequipment mounteds need to have the same mode at
        // all times
        if ((getType() instanceof MiscType)
                && getType().hasFlag(MiscType.F_COMMUNICATIONS)) {
            for (Mounted m : entity.getMisc()) {
                if (!m.equals(this)
                        && m.getType().hasFlag(MiscType.F_COMMUNICATIONS)) {
                    m.setMode(newMode);
                }
            }
        }
        return true;
    }

    /**
     * Can the switch from the current mode to the new mode happen instantly?
     *
     * @param newMode
     *            - integer for the new mode
     * @return
     */
    public boolean canInstantSwitch(int newMode) {
        String newModeName = type.getMode(newMode).getName();
        String curModeName = curMode().getName();
        return getType().hasInstantModeSwitch()
                && !type.isNextTurnModeSwitch(newModeName)
                && !type.isNextTurnModeSwitch(curModeName);
    }

    public void newRound(int roundNumber) {
        setUsedThisRound(false);

        if ((type != null) && (type.hasModes() && (pendingMode != -1))) {
            mode = pendingMode;
            pendingMode = -1;
        }
        called.reset();
    }

    public void newPhase(IGame.Phase phase) {
        jammed = jammedThisPhase;
    }

    /**
     * Shortcut to type.getName()
     */
    public String getName() {
        return type.getName();
    }

    public String getDesc() {
        StringBuffer desc;
        switch (getMineType()) {
            case MINE_CONVENTIONAL:
                desc = new StringBuffer(
                        Messages.getString("Mounted.ConventionalMine"));
                break;
            case MINE_VIBRABOMB:
                desc = new StringBuffer(
                        Messages.getString("Mounted.VibraBombMine"));
                break;
            case MINE_COMMAND_DETONATED:
                desc = new StringBuffer(
                        Messages.getString("Mounted.CommandDetonatedMine"));
                break;
            case MINE_ACTIVE:
                desc = new StringBuffer(
                        Messages.getString("Mounted.ActiveMine"));
                break;
            case MINE_INFERNO:
                desc = new StringBuffer(
                        Messages.getString("Mounted.InfernoMine"));
                break;
            case -1:
            default:
                desc = new StringBuffer(type.getDesc());
        }
        if (isWeaponGroup()) {
            desc.append(" (").append(getNWeapons()).append(")");
        }
        if (destroyed) {
            if (!repairable) {
                desc.insert(0, "**");
            } else {
                desc.insert(0, "*");
            }
        } else if (missing) {
            desc.insert(0, "x ");
        } else if (useless) {
            desc.insert(0, "x ");
        } else if (usedThisRound) {
            desc.insert(0, "+");
        } else if (jammed) {
            desc.insert(0, "j ");
        } else if (fired) {
            desc.insert(0, "- ");
        } else if (isPendingDump()) {
            desc.insert(0, "d ");
        }
        if (rearMounted) {
            desc.append(" (R)");
        }
        if (mechTurretMounted) {
            desc.append(" (T)");
        }
        if (sponsonTurretMounted) {
            desc.append(" (ST)");
        }
        if (pintleTurretMounted) {
            desc.append(" (PT)");
        }
        // Append the facing for VGLs
        if (getType().hasFlag(WeaponType.F_VGL)) {
            switch (facing) {
            case 0:
                desc.append(" (F)");
                break;
            case 1:
                desc.append(" (FR)");
                break;
            case 2:
                desc.append(" (RR)");
                break;
            case 3:
                desc.append(" (R)");
                break;
            case 4:
                desc.append(" (RL)");
                break;
            case 5:
                desc.append(" (FL)");
                break;
            }
        }
        if ((type instanceof AmmoType) && (location != Entity.LOC_NONE)) {

            desc.append(" (");
            desc.append(shotsLeft);
            desc.append(")");
        }
        if (getEntity() instanceof BattleArmor) {
            if (getBaMountLoc() == BattleArmor.MOUNT_LOC_BODY) {
                desc.append(" (Body)");
            }
            if (getBaMountLoc() == BattleArmor.MOUNT_LOC_LARM) {
                desc.append(" (Left arm)");
            }
            if (getBaMountLoc() == BattleArmor.MOUNT_LOC_RARM) {
                desc.append(" (Right arm)");
            }
            if (isDWPMounted()) {
                desc.append(" (DWP)");
            }
            if (isSquadSupportWeapon()) {
                desc.append(" (SSWM)");
            }
            if (isAPMMounted()) {
                desc.append(" (APM)");
            }
        }
        if (isDumping()) {
            desc.append(" (dumping)");
        }

        if (isArmored()){
            desc.append(" (armored)");
        }
        return desc.toString();
    }

    public boolean isReady() {
        return isReady(false);
    }

    public boolean isReady(boolean isStrafing) {
        return (!usedThisRound || isStrafing) && !destroyed && !missing
                && !jammed && !useless && !fired
                && (!isDWPMounted || (isDWPMounted && (getLinkedBy() != null)));
    }

    public boolean isUsedThisRound() {
        return usedThisRound;
    }

    public void setUsedThisRound(boolean usedThisRound) {
        this.usedThisRound = usedThisRound;
        if (usedThisRound) {
            phase = entity.game.getPhase();
        } else {
            phase = IGame.Phase.PHASE_UNKNOWN;
        }
    }

    public IGame.Phase usedInPhase() {
        if (usedThisRound) {
            return phase;
        }
        return IGame.Phase.PHASE_UNKNOWN;
    }

    public boolean isBreached() {
        return useless;
    }

    public void setBreached(boolean breached) {
        useless = breached;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Set this Mounted's destroyed status NOTE: only set this if this Mounted
     * cannot be used in the current phase anymore. If it still can, use setHit
     * instead
     *
     * @param destroyed
     * @see #setHit(boolean)
     */
    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
        if ((destroyed == true)
                && getType().hasFlag(MiscType.F_RADICAL_HEATSINK)){
            if (entity != null){
                entity.setHasDamagedRHS(true);
            }
        }
    }

    public boolean isInoperable() {
        return destroyed || missing || useless;
    }

    public boolean isHit() {
        return hit;
    }

    /**
     * set that this mounted was or was not hit with a crit this phase Note:
     * stuff that was hit in a phase can still be used in that phase, if that's
     * not desired, use setDestroyed instead
     *
     * @param hit
     * @see #setDestroyed(boolean)
     */
    public void setHit(boolean hit) {
        this.hit = hit;
        if ((hit == true)
                && getType().hasFlag(MiscType.F_RADICAL_HEATSINK)){
            if (entity != null){
                entity.setHasDamagedRHS(true);
            }
        }
    }

    public boolean isMissing() {
        return missing;
    }

    public void setMissing(boolean missing) {
        this.missing = missing;
    }

    public boolean isJammed() {
        return jammed;
    }

    public void setJammed(boolean j) {
        jammedThisPhase = j;
    }

    public boolean jammedThisPhase() {
        return jammedThisPhase;
    }

    /**
     * Clear all jam statuses - used by MHQ, because phase resetting doesn't work
     */
    public void resetJam() {
        jammed = false;
        jammedThisPhase = false;
    }

    /**
     * The number of shots of ammunition currently stored in this Mounted
     * irregardless of its operational status. Or in other words, the straight
     * value last set by {@link #setShotsLeft(int)}, even if this ammo slot is
     * no longer functional or indeed notionally no longer part of the unit
     * formerly carrying it. This is the 'general' base value that should be
     * used for display, reporting, entity encoding and salvage purposes.
     *
     * @see #getHittableShotsLeft()
     * @see #getUsableShotsLeft()
     * @return The base 'true' number of shots in this slot.
     */
    public int getBaseShotsLeft() {
        return shotsLeft;
    }

    /**
     * Convenience method returning the number of shots of ammunition in this
     * Mounted that may be affected by a critical hit. This method returns 0 if
     * this Mounted is marked as destroyed or missing and the same value as
     * {@link #getBaseShotsLeft()} otherwise.
     *
     * @see #getBaseShotsLeft()
     * @see #getUsableShotsLeft()
     * @return The number of 'hittable' shots in this slot.
     */
    public int getHittableShotsLeft() {
        if (destroyed || missing) {
            return 0;
        }
        return shotsLeft;
    }

    /**
     * Convenience method returning the number of shots of ammunition in this
     * Mounted that can actually be used <em>as</em> ammunition. This method
     * returns 0 if this Mounted is marked as destroyed, missing, or breached
     * and thus nonfunctional and the same value as {@link #getBaseShotsLeft()}
     * otherwise.
     *
     * @see #getBaseShotsLeft()
     * @see #getHittableShotsLeft()
     * @return The number of usable shots in this slot.
     */
    public int getUsableShotsLeft() {
        if (destroyed || missing || useless) {
            return 0;
        }
        return shotsLeft;
    }

    public void setShotsLeft(int shotsLeft) {
        if (shotsLeft < 0) {
            shotsLeft = 0;
        }
        this.shotsLeft = shotsLeft;
    }

    /**
     * Returns how many shots the weapon is using
     */
    public int getCurrentShots() {
        WeaponType wtype = (WeaponType) getType();
        int nShots = getNumShots(wtype, curMode(), false);
        // sets number of shots for MG arrays
        if (wtype.hasFlag(WeaponType.F_MGA)) {
            nShots = 0;
            for(int eqn : getBayWeapons()) {
                Mounted m = entity.getEquipment(eqn);
                if(null == m) {
                    continue;
                }
                if ((m.getLocation() == getLocation())
                        && !m.isDestroyed()
                        && !m.isBreached()
                        && m.getType().hasFlag(WeaponType.F_MG)
                        && (((WeaponType) m.getType()).getRackSize() == ((WeaponType) getType())
                                .getRackSize())) {
                    nShots++;
                }
            }
        }
        return nShots;
    }

    /**
     * Returns how many shots a weapon type would use.  This can be used without
     * an instantiation of Mounted, which is useful for computing Aero heat.
     * If ignoreMode is true, then mode can be null.
     */
    public static int getNumShots(WeaponType wtype, EquipmentMode mode,
            boolean ignoreMode) {
        int nShots = 1;
        // figure out # of shots for variable-shot weapons
        if (((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype
                .getAmmoType() == AmmoType.T_AC_ULTRA_THB))
                && (ignoreMode || mode.equals("Ultra"))) {
            nShots = 2;
        }
        // sets number of shots for AC rapid mode
        else if (((wtype.getAmmoType() == AmmoType.T_AC) || (wtype
                .getAmmoType() == AmmoType.T_LAC))
                && wtype.hasModes()
                && (ignoreMode || mode.equals("Rapid"))) {
            nShots = 2;
        } else if ((wtype.getAmmoType() == AmmoType.T_AC_ROTARY)
                || wtype.getInternalName().equals(BattleArmor.MINE_LAUNCHER)) {
            if ((mode != null) && mode.equals("2-shot")) {
                nShots = 2;
            } else if ((mode != null) && mode.equals("3-shot")) {
                nShots = 3;
            } else if ((mode != null) && mode.equals("4-shot")) {
                nShots = 4;
            } else if ((mode != null) && mode.equals("5-shot")) {
                nShots = 5;
            } else if ((ignoreMode || mode.equals("6-shot"))) {
                nShots = 6;
            }
        }
        return nShots;
    }

    public boolean isPendingDump() {
        return m_bPendingDump;
    }

    public void setPendingDump(boolean b) {
        m_bPendingDump = b;
    }

    public boolean isDumping() {
        return m_bDumping;
    }

    public void setDumping(boolean b) {
        m_bDumping = b;
    }

    public boolean isRapidfire() {
        return rapidfire;
    }

    public void setRapidfire(boolean rapidfire) {
        this.rapidfire = rapidfire;
    }

    /**
     * Checks to see if the current ammo for this weapon is hotloaded
     *
     * @return <code>true</code> if ammo is hotloaded or <code>false</code> if
     *         not
     */
    public boolean isHotLoaded() {

        boolean isHotLoaded = false;

        if (getType() instanceof WeaponType) {
            Mounted link = getLinked();
            if ((link == null) || !(link.getType() instanceof AmmoType)) {
                return false;
            }

            isHotLoaded = link.hotloaded;
            if (((AmmoType) link.getType()).getMunitionType() == AmmoType.M_DEAD_FIRE) {
                return true;
            }

            // Check to see if the ammo has its mode set to hotloaded.
            // This is for vehicles that can change hotload status during
            // combat.
            if (!isHotLoaded && link.getType().hasModes()
                    && link.curMode().equals("HotLoad")) {
                isHotLoaded = true;
            }

            return isHotLoaded;
        }

        if (getType() instanceof AmmoType) {
            isHotLoaded = hotloaded;

            if (((AmmoType) getType()).getMunitionType() == AmmoType.M_DEAD_FIRE) {
                return true;
            }

            // Check to see if the ammo has its mode set to hotloaded.
            // This is for vehicles that can change hotload status during
            // combat.
            if (!isHotLoaded && getType().hasModes()
                    && curMode().equals("HotLoad")) {
                isHotLoaded = true;
            }

            return isHotLoaded;
        }

        return false;
    }

    /**
     * Sets the hotloading parameter for this weapons ammo.
     *
     * @param hotload
     */
    public void setHotLoad(boolean hotload) {

        if (getType() instanceof WeaponType) {
            Mounted link = getLinked();
            if ((link == null) || !(link.getType() instanceof AmmoType)) {
                return;
            }
            if (((AmmoType) link.getType()).hasFlag(AmmoType.F_HOTLOAD)) {
                link.hotloaded = hotload;
            }
        }
        if (getType() instanceof AmmoType) {
            if (((AmmoType) getType()).hasFlag(AmmoType.F_HOTLOAD)) {
                hotloaded = hotload;
            }
        }

    }

    /**
     * does this <code>Mounted</code> have a linked and charged PPC Capacitor?
     */

    public int hasChargedCapacitor() {

        if ((getCrossLinkedBy() != null)
                && (getCrossLinkedBy().getType() instanceof MiscType)
                && !getCrossLinkedBy().isDestroyed()) {

            MiscType cap = (MiscType) getCrossLinkedBy().getType();
            if (cap.hasFlag(MiscType.F_PPC_CAPACITOR)
                    && getCrossLinkedBy().curMode().equals("Charge")) {
                return 2;
            }
        }

        if ((getLinkedBy() != null)
                && (getLinkedBy().getType() instanceof MiscType)
                && !getLinkedBy().isDestroyed()) {

            MiscType cap = (MiscType) getLinkedBy().getType();
            if (cap.hasFlag(MiscType.F_PPC_CAPACITOR)
                    && getLinkedBy().curMode().equals("Charge")) {
                return 1;
            }
        }
        return 0;
    }

    public int getLocation() {
        return location;
    }

    public int getSecondLocation() {
        if (bSplit) {
            return secondLocation;
        }
        return -1;
    }

    public boolean isRearMounted() {
        return rearMounted;
    }

    public void setLocation(int location) {
        setLocation(location, false);
    }

    public void setSecondLocation(int location) {
        setSecondLocation(location, false);
    }

    public void setLocation(int location, boolean rearMounted) {
        this.location = location;
        this.rearMounted = rearMounted;
    }

    public void setSecondLocation(int location, boolean rearMounted) {
        secondLocation = location;
        this.rearMounted = rearMounted;
    }

    public Mounted getLinked() {
        return linked;
    }

    public Mounted getLinkedBy() {
        return linkedBy;
    }

    public Mounted getCrossLinkedBy() {
        return crossLinkedBy;
    }

    public void setLinked(Mounted linked) {
        this.linked = linked;
        if (linked != null) {
            linked.setLinkedBy(this);
        }
    }

    public void setCrossLinked(Mounted linked) {
        this.linked = linked;
        linked.setCrossLinkedBy(this);
    }

    // should only be called by setLinked(), or when dumping a DWP
    // in the case of a many-to-one relationship (like ammo) this is meaningless
    public void setLinkedBy(Mounted linker) {
        if ((linker != null) && (linker.getLinked() != this)) {
            // liar
            return;
        }
        linkedBy = linker;
    }

    // called by setCrossLinked() when using cross-linked capacitors.
    public void setCrossLinkedBy(Mounted linker) {
        if ((linker != null) && (linker.getLinked() != this)) {
            // liar
            return;
        }
        crossLinkedBy = linker;
    }

    public int getFoundCrits() {
        return nFoundCrits;
    }

    public void setFoundCrits(int n) {
        nFoundCrits = n;
    }

    public boolean isSplit() {
        return bSplit;
    }

    public boolean isSplitable() {
        return (((getType() instanceof WeaponType) && ((WeaponType)getType()).isSplitable()) || ((getType() instanceof MiscType) && getType()
                .hasFlag(MiscType.F_SPLITABLE)));
    }

    public void setSplit(boolean b) {
        bSplit = b;
    }

    public int getExplosionDamage() {
        if (type instanceof AmmoType) {
            AmmoType atype = (AmmoType) type;
            int rackSize = atype.getRackSize();
            int damagePerShot = atype.getDamagePerShot();

            long mType = atype.getMunitionType();
            // both Dead-Fire and Tandem-charge SRM's do 3 points of damage per
            // shot when critted
            // Dead-Fire LRM's do 2 points of damage per shot when critted.
            if ((mType == AmmoType.M_DEAD_FIRE)
                    || (mType == AmmoType.M_TANDEM_CHARGE)) {
                damagePerShot++;
            } else if (atype.getAmmoType() == AmmoType.T_TASER) {
                damagePerShot = 6;
            }

            if (atype.getAmmoType() == AmmoType.T_MEK_MORTAR) {
                if ((mType == AmmoType.M_AIRBURST)
                        || (mType == AmmoType.M_FLARE)
                        || (mType == AmmoType.M_SMOKE)) {
                    damagePerShot = 1;
                } else {
                    damagePerShot = 2;
                }
            }

            return damagePerShot * rackSize * shotsLeft;
        }

        if (type instanceof WeaponType) {
            WeaponType wtype = (WeaponType) type;
            // TacOps Gauss Weapon rule p. 102
            if ((type instanceof GaussWeapon) && type.hasModes()
                    && curMode().equals("Powered Down")) {
                return 0;
            }
            if (isHotLoaded() && (getLinked().getUsableShotsLeft() > 0)) {
                Mounted link = getLinked();
                AmmoType atype = ((AmmoType) link.getType());
                int damagePerShot = atype.getDamagePerShot();
                // Launchers with Dead-Fire missles in them do an extra point of
                // damage per shot when critted
                if (atype.getAmmoType() == AmmoType.M_DEAD_FIRE) {
                    damagePerShot++;
                }

                int damage = wtype.getRackSize() * damagePerShot;
                return damage;
            }

            if (wtype.hasFlag(WeaponType.F_PPC) && (hasChargedCapacitor() != 0)) {
                if (isFired()) {
                    if (hasChargedCapacitor() == 2) {
                        return 15;
                    }
                    return 0;
                }
                if (hasChargedCapacitor() == 2) {
                    return 30;
                }
                return 15;
            }

            if ((wtype.getAmmoType() == AmmoType.T_MPOD) && isFired()) {
                return 0;
            }

            return wtype.getExplosionDamage();

        }

        if (type instanceof MiscType) {
            MiscType mtype = (MiscType) type;
            if (mtype.hasFlag(MiscType.F_PPC_CAPACITOR)) {
                if (curMode().equals("Charge") && (linked != null)
                        && !linked.isFired()) {
                    return 15;
                }
            }
            if (mtype.hasFlag(MiscType.F_FUEL)) {
                return 20;
            }
            if (mtype.hasFlag(MiscType.F_BLUE_SHIELD)) {
                return 5;
            }
            if (mtype.hasFlag(MiscType.F_JUMP_JET) && mtype.hasSubType(MiscType.S_PROTOTYPE) && mtype.hasSubType(MiscType.S_IMPROVED)) {
                return 10;
            }
            if (mtype.hasFlag(MiscType.F_RISC_LASER_PULSE_MODULE)) {
                return 2;
            }

            if (mtype.hasFlag(MiscType.F_EMERGENCY_COOLANT_SYSTEM)) {
                return 5;
            }
            return 0;
        }
        // um, otherwise, I'm not sure
        System.err.println("mounted: unable to determine explosion damage for "
                + getName());
        return 0;
    }

    public boolean isFired() { // has a oneshot weapon been fired?
        return fired;
    }

    public void setFired(boolean val) {
        fired = val;
    }

    public boolean isTSEMPDowntime() { // is this the "downtime" turn for TSEMP?
        return tsempDowntime;
    }

    public void setTSEMPDowntime(boolean val) {
        tsempDowntime = val;
    }

    /**
     * Confirm that the given entity can fire the indicated equipment.
     *
     * @return <code>true</code> if the equipment can be fired by the entity;
     *         <code>false</code> otherwise.
     */
    public boolean canFire() {
        return canFire(false);
    }

    public boolean canFire(boolean isStrafing) {

        // Equipment operational?
        if (!isReady(isStrafing) || isBreached() || isMissing() || isFired()) {
            return false;
        }

        // Is the entity even active?
        if (entity.isShutDown()
                || ((null != entity.getCrew()) && !entity.getCrew().isActive())) {
            return false;
        }

        // Otherwise, the equipment can be fired.
        return true;
    }

    /**
     * Determines whether this weapon should be considered crippled for damage
     * level purposes.
     *
     * @return {@code true} if the weapon is at least one of: destroyed,
     *         missing, breached, jammed, a detachable weapon no longer attached
     *         to its original battle armor, or simply out of ammo (includes
     *         discharged one-shot weapons), {@code false} otherwise.
     */
    public boolean isCrippled() {
        /*
         * Have to account for jammed weapons here because we're currently not
         * distinguishing between potentially temporary and permanent jams, and
         * a perma-jammed weapon definitely _is_ crippled. Moreover, even a
         * weapon that could be unjammed again may end up never getting there
         * and isn't contributing anything in the meantime, so it might as well
         * be considered "crippled" until the jam clears, if ever.
         */
        if (destroyed || jammed || missing || useless || fired) {
            return true;
        }
        if ((type instanceof AmmoWeapon) || (type instanceof AmmoBayWeapon)) {
            if ((getLinked() == null)
                    || (entity.getTotalAmmoOfType(getLinked().getType()) < 1)) {
                return true;
            }
        }
        if (isDWPMounted && (getLinkedBy() != null)) {
            return true;
        }
        return false;
    }

    /**
     * Returns false if this ammo should not be loaded. Checks if the ammo is
     * already destroyed or missing, is being dumped, has been breached, is
     * already used up, or is locationless (oneshot ammo).
     */
    public boolean isAmmoUsable() {
        if (destroyed || missing || m_bDumping || useless || (shotsLeft <= 0)
                || (location == Entity.LOC_NONE)) {
            return false;
        }
        return true;
    }

    /**
     * @return the type of mine this mounted is, or <code>-1</code> if it isn't
     *         a mine
     */
    public int getMineType() {
        return mineType;
    }

    /**
     * set the type of mine this should be
     *
     * @param mineType
     */
    public void setMineType(int mineType) {
        this.mineType = mineType;
    }

    /**
     * set the vibrabomb sensitivity
     *
     * @param vibraSetting
     *            the <code>int</code> sensitivity to set
     */
    public void setVibraSetting(int vibraSetting) {
        this.vibraSetting = vibraSetting;
    }

    /**
     * get the vibrabomb sensitivity
     *
     * @return the <code>int</code> vibrabomb sensitity this mine is set to.
     */
    public int getVibraSetting() {
        return vibraSetting;
    }

    @Override
    public String toString() {
        return "megamek.common.Mounted (" + typeName + ")";
    }

    public int getBaseDamageAbsorptionRate() {
        return baseDamageAbsorptionRate;
    }

    public int getBaseDamageCapacity() {
        return baseDamageCapacity;
    }

    /**
     * Rules state that every time the shield takes a crit its damage absorption
     * for each attack is reduced by 1. Also for every Arm actuator critted
     * damage absorption is reduced by 1 and finally if the shoulder is hit the
     * damage absorption is reduced by 2 making it possble to kill a shield
     * before its gone through its full damage capacity.
     *
     * @param entity
     * @param location
     * @return
     */
    public int getDamageAbsorption(Entity entity, int location) {
        // Shields can only be used in arms so if you've got a shield in a
        // location
        // other then an arm your SOL --Torren.
        if ((location != Mech.LOC_RARM) && (location != Mech.LOC_LARM)) {
            return 0;
        }

        int base = baseDamageAbsorptionRate;

        for (int slot = 0; slot < entity.getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = entity.getCritical(location, slot);

            if (cs == null) {
                continue;
            }

            if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                continue;
            }

            Mounted m = cs.getMount();
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && ((MiscType) type).isShield()) {
                if (cs.isDamaged()) {
                    base--;
                }
            }
        }

        if (!entity.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, location)) {
            base -= 2;
        }

        if (!entity.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, location)) {
            base--;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, location)) {
            base--;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_HAND, location)) {
            base--;
        }

        return Math.max(0, base);
    }

    /**
     * Rules say every time a shield is critted it loses 5 points from its
     * Damage Capacity. basically count down from the top then subtract the
     * amount of damage its already take. The damage capacity is used to
     * determine if the shield is still viable.
     *
     * @param entity
     * @param location
     * @return damage capacity(no less then 0)
     */
    public int getCurrentDamageCapacity(Entity entity, int location) {
        // Shields can only be used in arms so if you've got a shield in a
        // location
        // other then an arm your SOL --Torren.
        if ((location != Mech.LOC_RARM) && (location != Mech.LOC_LARM)) {
            return 0;
        }

        int base = baseDamageCapacity;

        for (int slot = 0; slot < entity.getNumberOfCriticals(location); slot++) {
            CriticalSlot cs = entity.getCritical(location, slot);

            if (cs == null) {
                continue;
            }

            if (cs.getType() != CriticalSlot.TYPE_EQUIPMENT) {
                continue;
            }

            Mounted m = cs.getMount();
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && ((MiscType) type).isShield()) {
                if (cs.isDamaged()) {
                    base -= 5;
                }
            }
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, location)) {
            base -= 2;
        }

        if (!entity.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, location)) {
            base--;
        }
        if (!entity.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, location)) {
            base--;
        }

        return Math.max(0, base - damageTaken);
    }

    public int getDamageTaken() {
        return damageTaken;
    }

    public void addWeaponToBay(int w) {
        bayWeapons.add(w);
    }

    public Vector<Integer> getBayWeapons() {
        return bayWeapons;
    }

    public void addAmmoToBay(int a) {
        bayAmmo.add(a);
    }

    public Vector<Integer> getBayAmmo() {
        return bayAmmo;
    }

    public void setByShot(boolean b) {
        byShot = b;
    }

    public boolean byShot() {
        return byShot;
    }

    // bomb related
    public boolean isBombMounted() {
        return bombMounted;
    }

    public void setBombMounted(boolean b) {
        bombMounted = b;
    }

    // is ammo in the same bay as the weapon
    public boolean ammoInBay(int mAmmoId) {
        for (int nextAmmoId : bayAmmo) {
            if (nextAmmoId == mAmmoId) {
                return true;
            }
        }
        return false;
    }

    /**
     * returns the heat for this weapon taking account of rapid-fire weapon
     * status
     */
    public int getCurrentHeat() {
        if (getType() instanceof WeaponType) {
            WeaponType wtype = (WeaponType) getType();
            int heat = wtype.getHeat();

            // AR10's have heat based upon the loaded missile
            if (wtype.getName().equals("AR10")){
                AmmoType ammoType = (AmmoType)getLinked().getType();
                if (ammoType.hasFlag(AmmoType.F_AR10_BARRACUDA)){
                    return 10;
                }else if (ammoType.hasFlag(AmmoType.F_AR10_WHITE_SHARK)){
                    return 15;
                } else { // AmmoType.F_AR10_KILLER_WHALTE
                    return 20;
                }
            }

            if (wtype.hasFlag(WeaponType.F_ENERGY) && wtype.hasModes()) {
                heat = Compute.dialDownHeat(this, wtype);
            }
            // multiply by number of shots and number of weapons
            heat = heat * getCurrentShots() * getNWeapons();
            if (hasQuirk(OptionsConstants.QUIRK_WEAP_POS_IMP_COOLING)) {
                heat = Math.max(1, heat - 1);
            }
            if (hasQuirk(OptionsConstants.QUIRK_WEAP_NEG_POOR_COOLING)) {
                heat += 1;
            }
            if (hasQuirk(OptionsConstants.QUIRK_WEAP_NEG_NO_COOLING)) {
                heat += 2;
            }
            if (hasChargedCapacitor() == 2) {
                heat += 10;
            }
            if (hasChargedCapacitor() == 1) {
                heat += 5;
            }
            if ((getLinkedBy() != null)
                    && !getLinkedBy().isInoperable()
                    && (getLinkedBy().getType() instanceof MiscType)
                    && getLinkedBy().getType().hasFlag(
                            MiscType.F_LASER_INSULATOR)) {
                heat -= 1;
                if (heat == 0) {
                    heat++;
                }
            }

            return heat;
        }
        return 0;
    }

    public int getNSantaAnna() {
        return nSantaAnna;
    }

    public void setNSantaAnna(int n) {
        nSantaAnna = n;
    }

    public boolean isBodyMounted() {
        return baMountLoc == BattleArmor.MOUNT_LOC_BODY;
    }

    public boolean isDWPMounted() {
        return isDWPMounted;
    }

    public void setDWPMounted(boolean dwpMounted) {
        isDWPMounted = dwpMounted;
    }

    public boolean isAPMMounted() {
        return isAPMMounted;
    }

    public void setAPMMounted(boolean apmMounted) {
        isAPMMounted = apmMounted;
    }

    public boolean isWeaponGroup() {
        return weaponGroup;
    }

    public void setWeaponGroup(boolean b) {
        weaponGroup = b;
    }

    public int getNWeapons() {
        return nweapons;
    }

    public void setNWeapons(int i) {
        // make sure this falls between 1 and 40
        if (i < 0) {
            i = 1;
        }
        if (i > 40) {
            i = 40;
        }
        nweapons = i;
    }

    public void unlink() {
        linked = null;
    }

    public void setArmored(boolean armored) {
        // Ammobins cannot be armored.
        if ((getType() instanceof AmmoType)
                && (((AmmoType) getType()).getAmmoType() != AmmoType.T_COOLANT_POD)) {
            armoredComponent = false;
        } else if ((getType() instanceof MiscType)
                && (getType().hasFlag(MiscType.F_SPIKES)
                        || getType().hasFlag(MiscType.F_REACTIVE)
                        || getType().hasFlag(MiscType.F_MODULAR_ARMOR) || ((MiscType) getType())
                            .isShield())) {
            armoredComponent = false;
        } else {
            armoredComponent = armored;
        }
    }

    public boolean isArmored() {
        return armoredComponent;
    }

    public void setQuirks(WeaponQuirks quirks) {
        this.quirks = quirks;
    }

    /**
     * Retrieves the quirks object for mounted. DO NOT USE this to check boolean
     * options, as it will not check game options for quirks. Use
     * Mounted#hasQuirk instead
     *
     * @return
     */
    public WeaponQuirks getQuirks() {
        return quirks;
    }

    public boolean hasQuirk(String name) {
        if ((null == entity)
                || (null == entity.getGame())
                || !entity.getGame().getOptions()
                        .booleanOption("stratops_quirks")) {
            return false;
        }
        return quirks.booleanOption(name);
    }

    /**
     * count all the quirks for this unit, positive and negative
     */
    public int countQuirks() {
        int count = 0;

        if ((null == entity) || (null == entity.game)
                || !entity.game.getOptions().booleanOption("stratops_quirks")) {
            return count;
        }

        for (Enumeration<IOptionGroup> i = quirks.getGroups(); i
                .hasMoreElements();) {
            IOptionGroup group = i.nextElement();
            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements();) {
                IOption quirk = j.nextElement();
                if (quirk.booleanValue()) {
                    count++;
                }
            }
        }

        return count;
    }

    /**
     * Returns a string of all the quirk "codes" for this entity, using sep as
     * the separator
     */
    public String getQuirkList(String sep) {
        StringBuffer qrk = new StringBuffer();

        if ((null == entity) || (null == entity.game)
                || !entity.game.getOptions().booleanOption("stratops_quirks")) {
            return qrk.toString();
        }

        if (null == sep) {
            sep = "";
        }

        for (Enumeration<IOptionGroup> i = quirks.getGroups(); i
                .hasMoreElements();) {
            IOptionGroup group = i.nextElement();
            for (Enumeration<IOption> j = group.getOptions(); j
                    .hasMoreElements();) {
                IOption quirk = j.nextElement();
                if (quirk.booleanValue()) {
                    if (qrk.length() > 0) {
                        qrk.append(sep);
                    }
                    qrk.append(quirk.getName());
                    if ((quirk.getType() == IOption.STRING)
                            || (quirk.getType() == IOption.CHOICE)
                            || (quirk.getType() == IOption.INTEGER)) {
                        qrk.append(" ").append(quirk.stringValue());
                    }
                }
            }
        }
        return qrk.toString();
    }

    public CalledShot getCalledShot() {
        return called;
    }

    public void setRepairable(boolean repair) {
        repairable = repair;
    }

    public boolean isRepairable() {
        return repairable;
    }

    public Entity getEntity() {
        return entity;
    }

    public boolean isMechTurretMounted() {
        return mechTurretMounted;
    }

    public void setMechTurretMounted(boolean turret) {
        mechTurretMounted = turret;
        if (mechTurretMounted) {
            setFacing(0);
        }
    }

    public void setSponsonTurretMounted(boolean turret) {
        sponsonTurretMounted = turret;
    }

    public boolean isSponsonTurretMounted() {
        return sponsonTurretMounted;
    }

    public void setPintleTurretMounted(boolean turret) {
        pintleTurretMounted = turret;
    }

    public boolean isPintleTurretMounted() {
        return pintleTurretMounted;
    }

    public void setFacing(int facing) {
        this.facing = facing;
    }

    public int getFacing() {
        return facing;
    }

    public boolean isKindRapidFire() {
        return kindRapidFire;
    }

    public void setKindRapidFire(boolean kindRapidFire) {
        this.kindRapidFire = kindRapidFire;
    }

    public int getOriginalShots() {
        return originalShots;
    }

    public void setOriginalShots(int shots) {
        originalShots = shots;
    }

    public boolean isModeSwitchable() {
        return modeSwitchable;
    }

    public void setModeSwitchable(boolean b) {
        modeSwitchable = b;
    }

    public int getBaMountLoc() {
        return baMountLoc;
    }

    public void setBaMountLoc(int baMountLoc) {
        this.baMountLoc = baMountLoc;
    }

    /**
     * Returns true if this Mounted is a one-shot launcher of some kind
     * otherwise returns false.
     *
     * @return
     */
    public boolean isOneShot(){
        return getType().hasFlag(WeaponType.F_ONESHOT);
    }

    public boolean isSquadSupportWeapon() {
        return squadSupportWeapon;
    }

    public void setSquadSupportWeapon(boolean squadSupportWeapon) {
        this.squadSupportWeapon = squadSupportWeapon;
    }

    public int getArmorValue() {
        return armorValue;
    }

    public void setArmorValue(int armorValue) {
        this.armorValue = armorValue;
    }

    public void setMissingForTrooper(int trooper, boolean b) {
        trooper = trooper-BattleArmor.LOC_TROOPER_1;
        if((trooper < 0) || (trooper >= missingForTrooper.length)) {
            return;
        }
        missingForTrooper[trooper] = b;
    }

    public boolean isMissingForTrooper(int trooper) {
        trooper = trooper-BattleArmor.LOC_TROOPER_1;
        if((trooper < 0) || (trooper >= missingForTrooper.length)) {
            return false;
        }
        return missingForTrooper[trooper];
    }

    public boolean isAnyMissingTroopers() {
        for(int i = 0; i < missingForTrooper.length; i++) {
            if(missingForTrooper[i]) {
                return true;
            }
        }
        return false;
    }

    public String getMissingTrooperString() {
        StringBuffer missings = new StringBuffer();
        for(int i = 0; i < missingForTrooper.length; i++) {
            missings.append(missingForTrooper[i]).append("::");
        }
        return missings.toString();
    }

    /**
     * Assign APDS systems to the most dangerous incoming missile attacks. This
     * should only be called once per turn, or AMS will get extra attacks
     */
    public WeaponAttackAction assignAPDS(List<WeaponHandler> vAttacks) {
        // Shouldn't have null entity, but if we do...
        if (getEntity() == null) {
            return null;
        }

        // Ensure we only target attacks in our arc & range
        List<WeaponAttackAction> vAttacksInArc = new Vector<>(vAttacks.size());
        for (WeaponHandler wr : vAttacks) {
            boolean isInArc = Compute.isInArc(getEntity().getGame(),
                    getEntity().getId(), getEntity().getEquipmentNum(this),
                    getEntity().getGame().getEntity(wr.waa.getEntityId()));
            boolean isInRange = getEntity().getPosition().distance(
                    wr.getWaa().getTarget(getEntity().getGame()).getPosition()) <= 3;
            if (isInArc && isInRange) {
                vAttacksInArc.add(wr.waa);
            }
        }
        // find the most dangerous salvo by expected damage
        WeaponAttackAction waa = Compute.getHighestExpectedDamage(getEntity()
                .getGame(), vAttacksInArc, true);
        if (waa != null) {
            waa.addCounterEquipment(this);
            return waa;
        }
        return null;
    }

    /**
     * Returns true if this Mounted is an APDS.
     * @return
     */
    public boolean isAPDS() {
        if ((getEntity() instanceof BattleArmor)
                && getType().getInternalName().equals("ISBAAPDS")) {
            return true;
        } else if (getType() instanceof WeaponType) {
            return ((WeaponType)getType()).getAmmoType() == AmmoType.T_APDS;
        } else {
            return false;
        }
    }
}
