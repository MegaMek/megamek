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
import java.util.Vector;

import megamek.common.weapons.GaussWeapon;

/**
 * This describes equipment mounted on a mech.
 *
 * @author Ben
 * @version
 */
public class Mounted implements Serializable, RoundUpdated {

    private static final long serialVersionUID = 6438017987074691566L;
    private boolean usedThisRound = false;
    private boolean destroyed = false;
    private boolean hit = false;
    private boolean missing = false;
    private boolean jammed = false;
    private boolean useless = false;
    private boolean fired = false; // Only true for used OS stuff.
    private boolean rapidfire = false; // MGs in rapid-fire mode
    private boolean hotloaded = false; // Hotloading for ammoType

    private int mode; // Equipment's current state. On or Off. Sixshot or
    // Fourshot, etc
    private int pendingMode = -1; // if mode changes happen at end of turn

    private int location;
    private boolean rearMounted;

    private Mounted linked = null; // for ammo, or artemis
    private Mounted linkedBy = null; // reverse link for convenience

    private Entity entity; // what I'm mounted on

    private transient EquipmentType type;
    private String typeName;

    // ammo-specific stuff. Probably should be a subclass
    private int shotsLeft;
    private boolean m_bPendingDump;
    private boolean m_bDumping;

    //A list of ids (equipment numbers) for the weapons  and ammo linked to
    //this bay (if the mounted is of the BayWeapon type)
    //I can also use this for weapons of the same type on a capital fighter
    private Vector<Integer> bayWeapons = new Vector<Integer>();
    private Vector<Integer> bayAmmo = new Vector<Integer>();

    //on capital fighters and squadrons some weapon mounts actually represent multiple weapons of the same type
    //provide a boolean indicating this type of mount and the number of weapons represented
    private boolean weaponGroup = false;
    private int nweapons = 1;

    //for ammo loaded by shot rather than ton, a boolean
    private boolean byShot = false;

    // handle split weapons
    private boolean bSplit = false;
    private int nFoundCrits = 0;
    private int secondLocation = 0;

    //  bomb stuff
    private boolean bombMounted = false;

    // mine type
    private int mineType = MINE_NONE;
    // vibrabomb mine setting
    private int vibraSetting = 20;

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

    //this is a hack but in the case of Killer Whale ammo
    //I need some way of tracking how many missiles are Santa Annas
    private int nSantaAnna = 0;

    // for BA weapons, is this on the body of a trooper?
    private boolean bodyMounted = false;

    // for Armored components
    private boolean armoredComponent = false;

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
        }
        if (((type instanceof MiscType) && ((MiscType) type).isShield()) || type.hasFlag(MiscType.F_MODULAR_ARMOR) ) {
            MiscType shield = (MiscType) type;
            baseDamageAbsorptionRate = shield.baseDamageAbsorptionRate;
            baseDamageCapacity = shield.baseDamageCapacity;
            damageTaken = shield.damageTaken;
        }

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
     * @param newMode the name of the desired new mode
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
     * @param newMode the number of the desired new mode
     */
    public boolean setMode(int newMode) {
        if (type.hasModes()) {

            if ( newMode >= type.getModesCount() ){
                return false;
            }
            /*megamek.debug.Assert.assertTrue(newMode >= 0
                    && newMode < type.getModesCount(), "Invalid mode, mode="
                    + newMode + ", modesCount=" + type.getModesCount());*/

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
     * @param newMode - integer for the new mode
     * @return
     */
    public boolean canInstantSwitch(int newMode) {
        String newModeName = type.getMode(newMode).getName();
        String curModeName = curMode().getName();
        return getType().hasInstantModeSwitch() && !type.isNextTurnModeSwitch(newModeName) && !type.isNextTurnModeSwitch(curModeName);
    }

    public void newRound(int roundNumber) {
        setUsedThisRound(false);
        if ((type != null) && (type.hasModes() && (pendingMode != -1))) {
            mode = pendingMode;
            pendingMode = -1;
        }
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
            desc = new StringBuffer(Messages
                    .getString("Mounted.ConventionalMine"));
            break;
        case MINE_VIBRABOMB:
            desc = new StringBuffer(Messages
                    .getString("Mounted.VibraBombMine"));
            break;
        case MINE_COMMAND_DETONATED:
            desc = new StringBuffer(Messages
                    .getString("Mounted.CommandDetonatedMine"));
            break;
        case MINE_ACTIVE:
            desc = new StringBuffer(Messages.getString("Mounted.ActiveMine"));
            break;
        case MINE_INFERNO:
            desc = new StringBuffer(Messages.getString("Mounted.InfernoMine"));
            break;
        case -1:
        default:
            desc = new StringBuffer(type.getDesc());
        }
        if(isWeaponGroup()) {
            desc.append(" (").append(getNWeapons()).append(")");
        }
        if (destroyed) {
            desc.insert(0, "*");
        } else if (useless) {
            desc.insert(0, "x ");
        } else if (usedThisRound) {
            desc.insert(0, "+");
        } else if (jammed) {
            desc.insert(0, "j ");
        } else if (fired) {
            desc.insert(0, "x ");
        }
        if (rearMounted) {
            desc.append(" (R)");
        }
        if ((type instanceof AmmoType) && (location != Entity.LOC_NONE)) {

            desc.append(" (");
            desc.append(shotsLeft);
            desc.append(")");
        }
        if (isDumping()) {
            desc.append(" (dumping)");
        }
        return desc.toString();
    }

    public boolean isReady() {
        return !usedThisRound && !destroyed && !jammed && !useless;
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

    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }

    public boolean isInoperable(){
        return destroyed || missing || useless;
    }

    public boolean isHit() {
        return hit;
    }

    public void setHit(boolean hit) {
        this.hit = hit;
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
        jammed = j;
    }

    public int getShotsLeft() {
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
        final WeaponType wtype = (WeaponType) getType();
        int nShots = 1;
        // figure out # of shots for variable-shot weapons
        if (((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype
                .getAmmoType() == AmmoType.T_AC_ULTRA_THB))
                && curMode().equals("Ultra")) {
            nShots = 2;
        }
        // sets number of shots for AC rapid mode
        else if (((wtype.getAmmoType() == AmmoType.T_AC) || (wtype.getAmmoType() == AmmoType.T_LAC))
                && wtype.hasModes() && curMode().equals("Rapid")) {
            nShots = 2;
        } else if ((wtype.getAmmoType() == AmmoType.T_AC_ROTARY)
                || wtype.getInternalName().equals(BattleArmor.MINE_LAUNCHER)) {
            if (curMode().equals("2-shot")) {
                nShots = 2;
            } else if (curMode().equals("3-shot")) {
                nShots = 3;
            } else if (curMode().equals("4-shot")) {
                nShots = 4;
            } else if (curMode().equals("5-shot")) {
                nShots = 5;
            } else if (curMode().equals("6-shot")) {
                nShots = 6;
            }
        }
        // sets number of shots for MG arrays
        else if (wtype.hasFlag(WeaponType.F_MGA)) {
            nShots = 0;
            for (Mounted m : entity.getWeaponList()) {
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
     * @return <code>true</code> if ammo is hotloaded or <code>false</code>
     *         if not
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
    public boolean hasChargedCapacitor() {
        if ((getLinkedBy() != null)
                && (getLinkedBy().getType() instanceof MiscType)
                && !getLinkedBy().isDestroyed()) {
            MiscType cap = (MiscType) getLinkedBy().getType();
            if (cap.hasFlag(MiscType.F_PPC_CAPACITOR)
                    && getLinkedBy().curMode().equals("Charge")) {
                return true;
            }
        }
        return false;
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

    public void setLinked(Mounted linked) {
        this.linked = linked;
        linked.setLinkedBy(this);
    }

    // should only be called by setLinked()
    // in the case of a many-to-one relationship (like ammo) this is meaningless
    protected void setLinkedBy(Mounted linker) {
        if (linker.getLinked() != this) {
            // liar
            return;
        }
        linkedBy = linker;
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
        return ((getType() instanceof WeaponType) && getType().hasFlag(
                WeaponType.F_SPLITABLE));
    }

    public void setSplit(boolean b) {
        bSplit = b;
    }

    public int getExplosionDamage() {
        if (type instanceof AmmoType) {
            AmmoType atype = (AmmoType) type;
            int rackSize = atype.getRackSize();
            int damagePerShot = atype.getDamagePerShot();

            // both Dead-Fire and Tandem-charge SRM's do 3 points of damage per
            // shot when critted
            // Dead-Fire LRM's do 2 points of damage per shot when critted.
            if ((atype.getMunitionType() == AmmoType.M_DEAD_FIRE)
                    || (atype.getMunitionType() == AmmoType.M_TANDEM_CHARGE)) {
                damagePerShot++;
            } else if (atype.getAmmoType() == AmmoType.T_TASER) {
                damagePerShot = 6;
            }

            return damagePerShot * rackSize * shotsLeft;
        }

        if (type instanceof WeaponType) {
            WeaponType wtype = (WeaponType) type;
            //TacOps Gauss Weapon rule p. 102
            if ( (type instanceof GaussWeapon) && type.hasModes() && curMode().equals("Powered Down") ) {
                return 0;
            }
            if (isHotLoaded() && (getLinked().getShotsLeft() > 0)) {
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

            if (wtype.hasFlag(WeaponType.F_PPC) && hasChargedCapacitor()) {
                if (isFired()) {
                    return 0;
                }
                return 15;
            }

            if ( (wtype.getAmmoType() == AmmoType.T_MPOD) && isFired() ){
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

    /**
     * Confirm that the given entity can fire the indicated equipment.
     *
     * @return <code>true</code> if the equipment can be fired by the entity;
     *         <code>false</code> otherwise.
     */
    public boolean canFire() {

        // Equipment operational?
        if (!isReady() || isBreached() || isMissing() || isFired()) {
            return false;
        }

        // Is the entity even active?
        if (entity.isShutDown() || !entity.getCrew().isActive()) {
            return false;
        }

        // Otherwise, the equipment can be fired.
        return true;
    }

    /**
     * Returns false if this ammo should not be loaded. Checks if the ammo is
     * already destroyed, is being dumped, has been breached, is already used
     * up, or is locationless (oneshot ammo).
     */
    public boolean isAmmoUsable() {
        if (destroyed || m_bDumping || useless
                || (shotsLeft <= 0) || (location == Entity.LOC_NONE)) {
            return false;
        }
        return true;
    }

    /**
     * @return the type of mine this mounted is, or <code>-1</code> if it
     *         isn't a mine
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
     * @param vibraSetting the <code>int</code> sensitivity to set
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

            Mounted m = entity.getEquipment(cs.getIndex());
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && ((MiscType) type).isShield()) {
                if (cs.isDamaged()) {
                    base--;
                }
            }
        }

        // Only damaged Actuators should effect the shields absorption rate
        // Not missing ones.
        if (entity.hasSystem(Mech.ACTUATOR_SHOULDER, location)
                && !entity.hasWorkingSystem(Mech.ACTUATOR_SHOULDER, location)) {
            base -= 2;
        }

        if (entity.hasSystem(Mech.ACTUATOR_LOWER_ARM, location)
                && !entity.hasWorkingSystem(Mech.ACTUATOR_LOWER_ARM, location)) {
            base--;
        }
        if (entity.hasSystem(Mech.ACTUATOR_UPPER_ARM, location)
                && !entity.hasWorkingSystem(Mech.ACTUATOR_UPPER_ARM, location)) {
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

            Mounted m = entity.getEquipment(cs.getIndex());
            EquipmentType type = m.getType();
            if ((type instanceof MiscType) && ((MiscType) type).isShield()) {
                if (cs.isDamaged()) {
                    base -= 5;
                }
            }
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

    //bomb related
    public boolean isBombMounted() {
        return bombMounted;
    }

    public void setBombMounted(boolean b) {
        bombMounted = b;
    }

    //is ammo in the same bay as the weapon
    public boolean ammoInBay(int mAmmoId) {
        for(int nextAmmoId : bayAmmo) {
            if(nextAmmoId == mAmmoId) {
                return true;
            }
        }
        return false;
    }

    /*
     * returns the heat for this weapon taking account of rapid-fire weapon status
     */
    public int getCurrentHeat() {
        if(getType() instanceof WeaponType) {
            WeaponType wtype = (WeaponType)getType();
            if ( wtype.hasFlag(WeaponType.F_ENERGY) && wtype.hasModes() ){
                return  Compute.dialDownHeat(this, wtype)*getCurrentShots()*getNWeapons();
            }
            return ((WeaponType)getType()).getHeat()*getCurrentShots()*getNWeapons();
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
        return bodyMounted;
    }

    public void setBodyMounted(boolean bodyMounted) {
        this.bodyMounted = bodyMounted;
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
        //make sure this falls between 1 and 40
        if(i < 0) {
            i = 1;
        }
        if(i > 40) {
            i = 40;
        }
        nweapons = i;
    }

    public void unlink() {
        linked = null;
    }

    public void setArmored(boolean armored) {
        // Ammobins cannot be armored.
        if (getType() instanceof AmmoType) {
            armoredComponent = false;
        } else if (getType() instanceof MiscType && (getType().hasFlag(MiscType.F_HARJEL) || getType().hasFlag(MiscType.F_SPIKES) || getType().hasFlag(MiscType.F_REACTIVE) || getType().hasFlag(MiscType.F_MODULAR_ARMOR) || ((MiscType) getType()).isShield())) {
                armoredComponent = false;
        } else {
            armoredComponent = armored;
        }
    }

    public boolean isArmored() {
        return armoredComponent;
    }
}
