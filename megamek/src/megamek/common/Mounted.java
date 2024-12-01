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
package megamek.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import megamek.common.enums.GamePhase;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.BombMounted;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.modifiers.EquipmentModifier;
import megamek.common.options.GameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.options.WeaponQuirks;
import megamek.common.weapons.AmmoWeapon;
import megamek.common.weapons.Weapon;
import megamek.common.weapons.bayweapons.AmmoBayWeapon;
import megamek.common.weapons.bayweapons.BayWeapon;
import megamek.logging.MMLogger;

/**
 * This describes equipment mounted on a Mek.
 *
 * @author Ben
 * @since April 1, 2002, 1:29 PM
 */
public class Mounted<T extends EquipmentType> implements Serializable, RoundUpdated, PhaseUpdated, Modifiable {
    private static final MMLogger logger = MMLogger.create(Mounted.class);

    private static final long serialVersionUID = 6438017987074691566L;
    private boolean usedThisRound = false;
    private boolean destroyed = false;
    private boolean hit = false;
    private boolean missing = false;
    private boolean jammed = false;
    private boolean jammedThisPhase = false;
    private boolean useless = false;
    private boolean fired = false; // Only true for used OS stuff and TSEMP.
    private boolean tsempDowntime = false; // Needed for "every other turn" TSEMP.
    private boolean rapidfire = false; // MGs in rapid-fire mode
    private boolean hotloaded = false; // Hotloading for ammoType
    private boolean repairable = true; // can the equipment mounted here be
    // repaired
    private boolean mekTurretMounted = false; // is this mounted in a mekturret
    private boolean sponsonTurretMounted = false; // is this mounted in a sponsonturret
    private boolean pintleTurretMounted = false; // is this mounted in a pintleturret
    private int facing = -1; // facing for turrets

    private int mode; // Equipment's current state. On or Off. Sixshot or
    // Fourshot, etc
    private int pendingMode = -1; // if mode changes happen at end of turn
    private boolean modeSwitchable = true; // disallow mode switching

    private int location;
    private boolean rearMounted;

    private Mounted<?> linked = null; // for ammo, or artemis
    private Mounted<?> linkedBy = null; // reverse link for convenience

    private Mounted<?> crossLinkedBy = null; // Weapons with crossLinked capacitors
    private int linkedBayId = -1;

    private final Entity entity; // what I'm mounted on

    private WeaponQuirks quirks = new WeaponQuirks();

    private transient T type;
    private String typeName;
    private double size = 1.0;

    // ammo-specific stuff. Probably should be a subclass
    private int shotsLeft;
    // how many shots did we have originally?
    // only used for by-shot ammo
    private int originalShots;
    private boolean m_bPendingDump;
    private boolean m_bDumping;

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
    private boolean isInternalBomb = false;

    // These arrays are used to track individual missing modular components on BA
    // for MHQ
    // in MM they probably shouldn't need to be touched. They are used to keep track
    // of
    // whether a modular mount is in use or not for a particular trooper.
    private boolean[] missingForTrooper = { false, false, false, false, false, false };

    /**
     * Armor value, used for applicable equipment types like minesweepers.
     */
    private int armorValue = 0;

    private GamePhase phase = GamePhase.UNKNOWN;

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

    // New stuff for ammo switching
    protected int switchedReason = 0;

    /**
     * BattleArmor use the standard locations to track troopers. On BA, this field
     * keeps track of where
     * a piece of equipment is mounted.
     */
    private int baMountLoc = BattleArmor.MOUNT_LOC_NONE;

    /**
     * For BA weapons, is this in a detachable weapon pack?
     */
    private boolean isDWPMounted = false;

    /**
     * Does this Mounted represent a weapon that is mounted in an anti-personnel
     * weapon mount?
     */
    private boolean isAPMMounted = false;

    /**
     * Does this Mounted represent equipmented that is pod mounted in an omni unit?
     */
    private boolean omniPodMounted = false;

    // for Armored components
    private boolean armoredComponent = false;

    // called shots status, sort of like another mode
    private CalledShot called = new CalledShot();

    /**
     * Flag that keeps track of whether this <code>Mounted</code> is mounted as
     * a squad support weapon on <code>BattleArmor</code>.
     */
    private boolean squadSupportWeapon;

    protected final List<EquipmentModifier> modifiers = new ArrayList<>();

    /** Creates new Mounted */
    protected Mounted(Entity entity, T type) {
        this.entity = entity;
        this.type = type;
        typeName = type.getInternalName();

        quirks.initialize();
    }

    public static Mounted<?> createMounted(Entity entity, EquipmentType type) {
        if (type instanceof BayWeapon bayWeapon) {
            return new WeaponMounted(entity, bayWeapon);
        } else if (type instanceof WeaponType weaponType) {
            return new WeaponMounted(entity, weaponType);
        } else if (type instanceof BombType bombType) {
            return new BombMounted(entity, bombType);
        } else if (type instanceof AmmoType ammoType) {
            return new AmmoMounted(entity, ammoType);
        } else if (type instanceof MiscType miscType) {
            return new MiscMounted(entity, miscType);
        } else {
            return new Mounted<>(entity, type);
        }
    }

    /**
     * Restores the equipment from the name
     */
    @SuppressWarnings("unchecked")
    public void restore() {
        if (typeName == null) {
            typeName = type.getName();
        } else {
            type = (T) EquipmentType.get(typeName);
        }

        if (type == null) {
            String message = String.format("Could not restore equipment type \"%s\"", typeName);
            logger.error(message);
        }
    }

    @SuppressWarnings("unchecked")
    public T getType() {
        return (null != type) ? type : (type = (T) EquipmentType.get(typeName));
    }

    protected void setType(T type) {
        this.type = type;
        this.typeName = type.getInternalName();
    }

    public int getModesCount() {
        return getType().getModesCount(this);
    }

    protected EquipmentMode getMode(int mode) {
        return getType().getMode(mode);
    }

    public boolean hasModes() {
        return getType().hasModes();
    }

    public boolean hasModeType(String mode) {
        return getType().hasModeType(mode);
    }

    public void adaptToGameOptions(GameOptions options) {
        if (getType() instanceof Weapon) {
            ((Weapon) getType()).adaptToGameOptions(options);
        }
    }

    /**
     * @return the current mode of the equipment, or <code>null</code> if it's
     *         not available.
     */
    public EquipmentMode curMode() {
        if ((mode >= 0) && (mode < getModesCount())) {
            return getMode(mode);
        }
        return EquipmentMode.getMode("None");
    }

    /**
     * @return the pending mode of the equipment.
     */
    public EquipmentMode pendingMode() {
        if ((pendingMode < 0) || (pendingMode >= getModesCount())) {
            return EquipmentMode.getMode("None");
        }
        return type.getMode(pendingMode);
    }

    /**
     * Switches the equipment mode to the next or previous available.
     *
     * @param forward
     *                Flag that determines whether the mode should be advanced
     *                forward or backwards.
     * @return new mode number, or <code>-1</code> if it's not available.
     */
    public int switchMode(boolean forward) {
        if (hasModes()) {
            int nMode = 0;
            if (pendingMode > -1) {
                if (forward) {
                    nMode = (pendingMode + 1) % getModesCount();
                } else {
                    nMode = (pendingMode - 1);
                    if (nMode < 0) {
                        nMode = getModesCount() - 1;
                    }
                }
            } else {
                if (forward) {
                    nMode = (mode + 1) % getModesCount();
                } else {
                    nMode = (mode - 1);
                    if (nMode < 0) {
                        nMode = getModesCount() - 1;
                    }
                }
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
     * @return new mode number on success, <code>-1</code> otherwise.
     */
    public int setMode(String newMode) {
        for (int x = 0, e = getModesCount(); x < e; x++) {
            if (getMode(x).equals(newMode)) {
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
     *                the number of the desired new mode
     */
    public boolean setMode(int newMode) {
        if (hasModes()) {

            if (newMode >= getModesCount()) {
                return false;
            }

            if (canInstantSwitch(newMode)) {
                mode = newMode;
                pendingMode = -1;
            } else if (pendingMode != newMode) {
                if (mode == newMode) {
                    pendingMode = -1;
                } else {
                    pendingMode = newMode;
                }
            }
        }
        // all communications equipment mounteds need to have the same mode at all times
        if ((getType() instanceof MiscType)
                && getType().hasFlag(MiscType.F_COMMUNICATIONS)) {
            for (Mounted<?> m : entity.getMisc()) {
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
     *                - integer for the new mode
     * @return
     */
    public boolean canInstantSwitch(int newMode) {
        String newModeName = type.getMode(newMode).getName();
        String curModeName = curMode().getName();
        return getType().hasInstantModeSwitch()
                && !type.isNextTurnModeSwitch(newModeName)
                && !type.isNextTurnModeSwitch(curModeName);
    }

    @Override
    public void newRound(int roundNumber) {
        setUsedThisRound(false);

        if ((type != null) && (type.hasModes() && (pendingMode != -1))) {
            mode = pendingMode;
            pendingMode = -1;
        }
        called.reset();
    }

    @Override
    public void newPhase(GamePhase phase) {
        jammed = jammedThisPhase;
    }

    /**
     * Shortcut to type.getName()
     */
    public String getName() {
        return type.getName(size);
    }

    public String getShortName() {
        return type.getShortName(size);
    }

    protected String getBaseDesc() {
        return getType().getDesc(getSize());
    }

    public String getDesc() {
        StringBuilder desc = new StringBuilder(getBaseDesc());
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
        if (mekTurretMounted) {
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
            if (getBaMountLoc() == BattleArmor.MOUNT_LOC_TURRET) {
                desc.append(" (Turret)");
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

        if (isArmored()) {
            desc.append(" (armored)");
        }

        if (isInternalBomb()) {
            desc.append(" (Int. Bay)");
        }
        return desc.toString();
    }

    /**
     * @return The weight of the equipment in this mount, using the default rounding
     *         method for the {@link Entity}.
     */
    public double getTonnage() {
        return getTonnage(RoundWeight.STANDARD);
    }

    /**
     * @param defaultRounding The method to use in rounding the weight
     * @return The weight of the equipment in this mount
     */
    public double getTonnage(RoundWeight defaultRounding) {
        if ((getType() instanceof MiscType) && getType().hasFlag(MiscType.F_DUMPER)) {
            Mounted<?> cargo = getLinked();
            if (cargo != null) {
                return defaultRounding.round(cargo.getSize() * 0.05, getEntity());
            }
            logger.warn("Found dumper not linked to a Cargo equipment. Using zero for the weight.");
            return 0.0;
        }
        double retVal = getType().getTonnage(getEntity(), getLocation(), getSize(), defaultRounding);
        if (isDWPMounted()) {
            return defaultRounding.round(retVal * 0.75, getEntity());
        } else if (isSquadSupportWeapon()) {
            retVal *= getEntity().isClan() ? 0.4 : 0.5;
            return defaultRounding.round(retVal, getEntity());
        }
        return retVal;
    }

    public int getCriticals() {
        return getType().getCriticals(getEntity(), getSize());
    }

    /**
     * @return The cost of the mounted equipment
     */
    public double getCost() {
        return getType().getCost(getEntity(), isArmored(), getLocation(), getSize());
    }

    public boolean isReady() {
        return isReady(false, false);
    }

    public boolean isReady(boolean isStrafing, boolean evenIfAlreadyFired) {
        return (!usedThisRound || evenIfAlreadyFired || isStrafing) && !destroyed && !missing
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
            phase = GamePhase.UNKNOWN;
        }
    }

    public GamePhase usedInPhase() {
        if (usedThisRound) {
            return phase;
        }
        return GamePhase.UNKNOWN;
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
                && getType().hasFlag(MiscType.F_RADICAL_HEATSINK)) {
            if (entity != null) {
                entity.setHasDamagedRHS(true);
            }
        }
    }

    /**
     * Returns true when this Mounted is not destroyed nor located in a blown-off
     * or breached location. Does not check any other conditions such as DWP
     * mounting or prior use.
     * Also does not check if this mounted has been hit in this phase.
     * This is equivalent to !{@link #isInoperable()}.
     *
     * @return True when this Mounted is operable
     */
    public boolean isOperable() {
        return !isInoperable();
    }

    /**
     * Returns true when this Mounted destroyed or located in a blown-off
     * or breached location. Does not check any other conditions such as DWP
     * mounting or prior use.
     * Also does not check if this mounted has been hit in this phase.
     * Equivalent to !{@link #isOperable()}.
     *
     * @return True when this Mounted is operable
     */
    public boolean isInoperable() {
        return destroyed || missing || useless;
    }

    /**
     * Returns true if this Mounted's EquipmentType is that identified by the given
     * typeInternalName String. The
     * given typeInternalName is compared to the internal name of the EquipmentType
     * of this Mounted,
     * not the (display) name!
     * Best use the constants defined in EquipmentTypeLookup.
     *
     * @param typeInternalName An Equipment internal name to check
     * @return true if the internalName of this equipment matches the given type
     */
    public boolean is(String typeInternalName) {
        return getType().is(typeInternalName);
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
                && getType().hasFlag(MiscType.F_RADICAL_HEATSINK)) {
            if (entity != null) {
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
     * regardless of its operational status. Or in other words, the straight
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

    /**
     * @return True when this ammunition Mounted has shots left and is operable.
     * @see #getUsableShotsLeft()
     * @see #isOperable()
     */
    public boolean hasUsableShotsLeft() {
        return getUsableShotsLeft() > 0;
    }

    public void setShotsLeft(int shotsLeft) {
        if (shotsLeft < 0) {
            shotsLeft = 0;
        }
        this.shotsLeft = shotsLeft;
    }

    /**
     * Returns how many shots a weapon type would use. This can be used without
     * an instantiation of Mounted, which is useful for computing Aero heat.
     * If ignoreMode is true, then mode can be null.
     */
    public static int getNumShots(WeaponType wtype, EquipmentMode mode,
            boolean ignoreMode) {
        // figure out # of shots for variable-shot weapons
        if (((wtype.getAmmoType() == AmmoType.T_AC_ULTRA) || (wtype
                .getAmmoType() == AmmoType.T_AC_ULTRA_THB))
                && (ignoreMode || mode.equals(Weapon.MODE_UAC_ULTRA))) {
            return 2;
        }
        // sets number of shots for AC rapid mode
        else if (((wtype.getAmmoType() == AmmoType.T_AC)
                || (wtype.getAmmoType() == AmmoType.T_LAC)
                || (wtype.getAmmoType() == AmmoType.T_AC_IMP)
                || (wtype.getAmmoType() == AmmoType.T_PAC))
                && wtype.hasModes()
                && (ignoreMode || mode.equals(Weapon.MODE_AC_RAPID))) {
            return 2;
        } else if ((wtype.getAmmoType() == AmmoType.T_AC_ROTARY)
                || wtype.getInternalName().equals(BattleArmor.MINE_LAUNCHER)) {
            if (ignoreMode || (mode == null) || mode.equals(Weapon.MODE_RAC_SIX_SHOT)) {
                return 6;
            } else if (mode.equals(Weapon.MODE_RAC_TWO_SHOT)) {
                return 2;
            } else if (mode.equals(Weapon.MODE_RAC_THREE_SHOT)) {
                return 3;
            } else if (mode.equals(Weapon.MODE_RAC_FOUR_SHOT)) {
                return 4;
            } else if (mode.equals(Weapon.MODE_RAC_FIVE_SHOT)) {
                return 5;
            }
        }
        return 1;
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

    /**
     * Used for equipment that can vary in size. This is not used for equipment for
     * which
     * size is computed by outside factors such as unit weight or targeting
     * computer-linked
     * weapons, but for equipment that comes in various weights, lengths, or
     * capacities.
     * The meaning of the size varies according to the equipment. For robotic
     * control systems,
     * this is the number of drones that can be controlled. For ladders this is the
     * length in meters.
     * For most other variable equipment this is the weight in tons. Non-variable
     * equipment should return 1.0.
     *
     * @return The size of the mounted equipment
     */
    public double getSize() {
        return size;
    }

    /**
     * Sets the size of variable-sized equipment.
     *
     * @param size
     * @see #getSize()
     */
    public void setSize(double size) {
        this.size = size;
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
            Mounted<?> link = getLinked();
            if ((link == null) || !(link.getType() instanceof AmmoType)) {
                return false;
            }

            isHotLoaded = link.hotloaded;

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
            Mounted<?> link = getLinked();
            if ((link == null) || !(link.getType() instanceof AmmoType)) {
                return;
            }
            if (link.getType().hasFlag(AmmoType.F_HOTLOAD)) {
                link.hotloaded = hotload;
            }
        }
        if (getType() instanceof AmmoType) {
            if (getType().hasFlag(AmmoType.F_HOTLOAD)) {
                hotloaded = hotload;
            }
        }

    }

    /** Returns true when m is a PPC Capacitor and not destroyed. */
    private boolean isWorkingCapacitor(Mounted<?> m) {
        return !m.isDestroyed()
                && m.getType() instanceof MiscType
                && ((MiscType) m.getType()).hasFlag(MiscType.F_PPC_CAPACITOR);
    }

    /**
     * Returns 1 or 2 if this Mounted has a linked
     * and charged (= set to charge in an earlier turn)
     * PPC Capacitor.
     */
    public int hasChargedCapacitor() {
        if (getCrossLinkedBy() != null
                && isWorkingCapacitor(getCrossLinkedBy())
                && getCrossLinkedBy().curMode().equals("Charge")) {
            return 2;
        }

        if (getLinkedBy() != null
                && isWorkingCapacitor(getLinkedBy())
                && getLinkedBy().curMode().equals("Charge")) {
            return 1;
        }
        return 0;
    }

    /**
     * Returns 1 or 2 if this Mounted has a linked
     * and charged (= set to charge in an earlier turn)
     * or charging (= set to charge this turn)
     * PPC Capacitor. Used to determine heat.
     */
    public int hasChargedOrChargingCapacitor() {
        int isCharged = hasChargedCapacitor();
        if (isCharged != 0) {
            return isCharged;
        } else {
            // When it has been set to charge this turn,
            // pendingMode is set
            if (getCrossLinkedBy() != null
                    && isWorkingCapacitor(getCrossLinkedBy())
                    && getCrossLinkedBy().pendingMode().equals("Charge")) {
                return 2;
            }

            if (getLinkedBy() != null
                    && isWorkingCapacitor(getLinkedBy())
                    && getLinkedBy().pendingMode().equals("Charge")) {
                return 1;
            }
            return 0;
        }
    }

    /**
     * Returns the location this equipment is mounted in, such as
     * {@link Tank#LOC_FRONT}. May also return
     * {@link Entity#LOC_NONE} for unallocated equipment and some special equipment.
     * For BattleArmor, will
     * return one of the trooper locations, such as
     * {@link BattleArmor#LOC_TROOPER_1}. To get the equipment
     * location on BA, use {@link #getBaMountLoc()}.
     *
     * @return The location of this equipment on its unit
     */
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

    /**
     * Fetches all locations that contain this equipment. This is primarily for
     * spreadable equipment, can be placed in locations other than the primary or
     * secondary,
     * but will also work for non-spreadable equipment.
     *
     * @return A list of indices for all locations that contain this equipment.
     * @see #getLocation()
     * @see #getSecondLocation()
     */
    public List<Integer> allLocations() {
        List<Integer> locations = new ArrayList<>();
        if (getType().isSpreadable()) {
            for (int loc = 0; loc < getEntity().locations(); loc++) {
                for (int slot = 0; slot < getEntity().getNumberOfCriticals(loc); slot++) {
                    final CriticalSlot crit = getEntity().getCritical(loc, slot);
                    if ((crit != null) && ((crit.getMount() == this) || (crit.getMount2() == this))) {
                        locations.add(loc);
                        break;
                    }
                }
            }
        } else {
            if (getLocation() >= 0) {
                locations.add(getLocation());
            }
            if (getSecondLocation() >= 0) {
                locations.add(getSecondLocation());
            }
        }
        return locations;
    }

    public Mounted<?> getLinked() {
        return linked;
    }

    public Mounted<?> getLinkedBy() {
        return linkedBy;
    }

    public Mounted<?> getCrossLinkedBy() {
        return crossLinkedBy;
    }

    public void setLinked(Mounted<?> linked) {
        this.linked = linked;
        if (linked != null) {
            linked.setLinkedBy(this);
        }
    }

    public void setCrossLinked(Mounted<?> linked) {
        this.linked = linked;
        linked.setCrossLinkedBy(this);
    }

    // should only be called by setLinked(), or when dumping a DWP
    // in the case of a many-to-one relationship (like ammo) this is meaningless
    public void setLinkedBy(Mounted<?> linker) {
        if ((linker != null) && (linker.getLinked() != this)) {
            // liar
            return;
        }
        linkedBy = linker;
    }

    // called by setCrossLinked() when using cross-linked capacitors.
    public void setCrossLinkedBy(Mounted<?> linker) {
        if ((linker != null) && (linker.getLinked() != this)) {
            // liar
            return;
        }
        crossLinkedBy = linker;
    }

    /**
     * Used for associating the equipment mount with a cargo bay. This is for
     * dumpers and transient bays
     * created on load for use by 'Mek cargo equipment.
     *
     * @return The index of the bay this mount is linked to, or -1 if it is not
     *         linked.
     * @see Entity#getBayById(int)
     */
    public int getLinkedBayId() {
        return linkedBayId;
    }

    public void setLinkedBayId(int id) {
        linkedBayId = id;
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
        return (((getType() instanceof WeaponType) && ((WeaponType) getType()).isSplitable())
                || ((getType() instanceof MiscType) && getType()
                        .hasFlag(MiscType.F_SPLITABLE)));
    }

    public void setSplit(boolean b) {
        bSplit = b;
    }

    public int getExplosionDamage() {
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
            if (mtype.hasFlag(MiscType.F_JUMP_JET) && mtype.hasSubType(MiscType.S_PROTOTYPE)
                    && mtype.hasSubType(MiscType.S_IMPROVED)) {
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
        String message = String.format("mounted: unable to determine explosion damage for %s", typeName);
        logger.error(message);
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
        return canFire(false, false);
    }

    public boolean canFire(boolean isStrafing, boolean evenIfAlreadyFired) {
        if (!evenIfAlreadyFired && isFired()) {
            return false;
        }

        // Equipment operational?
        if (!isReady(isStrafing, evenIfAlreadyFired) || isBreached() || isMissing()) {
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
     *
     * @return The index of this mount in the entity's equipmentlist.
     */
    public int getEquipmentNum() {
        return getEntity().getEquipmentNum(this);
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

    /**
     * Convenience "property" to reduce typing, which returns true if the current
     * piece of equipment is a bomb capable of striking ground targets.
     *
     * @return True if
     */
    public boolean isGroundBomb() {
        return getType().hasFlag(WeaponType.F_DIVE_BOMB) || getType().hasFlag(WeaponType.F_ALT_BOMB) ||
                getType().hasFlag(AmmoType.F_GROUND_BOMB);
    }

    public void setInternalBomb(boolean internal) {
        isInternalBomb = internal;
    }

    /**
     * Convenience method to determine if a bomb munition is mounted EXternally
     * (reduces MP) or INternally (no
     * MP reduction).
     *
     * @return True if
     */
    public boolean isInternalBomb() {
        return isInternalBomb;
    }

    /**
     * returns the heat for this weapon taking account of rapid-fire weapon
     * status
     */
    public int getCurrentHeat() {
        return 0;
    }

    public boolean isBodyMounted() {
        return (baMountLoc == BattleArmor.MOUNT_LOC_BODY)
                || (baMountLoc == BattleArmor.MOUNT_LOC_TURRET);
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

    public boolean isOmniPodMounted() {
        return omniPodMounted;
    }

    public void setOmniPodMounted(boolean omniPodMounted) {
        this.omniPodMounted = omniPodMounted;
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
                        .booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            return false;
        }
        return quirks.booleanOption(name);
    }

    /**
     * Count all the weapon quirks for this Mounted. Returns 0 when this Mounted is
     * tied to an Entity and
     * this Entity's game does not use quirks! In all other cases (e.g. outside of
     * games), returns the quirk
     * count of this Mounted.
     *
     * @return The number of active weapon quirks for this Mounted unless the game
     *         does not use quirks
     */
    public int countQuirks() {
        if ((entity != null) && (entity.getGame() != null) &&
                !entity.getGame().getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            return 0;
        } else {
            return quirks.count();
        }
    }

    /**
     * Returns a string of all the quirk "codes" for this entity, using sep as
     * the separator
     */
    public String getQuirkList(String sep) {
        if ((null == entity) || (null == entity.game)
                || !entity.game.getOptions().booleanOption(OptionsConstants.ADVANCED_STRATOPS_QUIRKS)) {
            return "";
        }

        return quirks.getOptionList(sep);
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

    public boolean isMekTurretMounted() {
        return mekTurretMounted;
    }

    public void setMekTurretMounted(boolean turret) {
        mekTurretMounted = turret;
        if (mekTurretMounted) {
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

    /**
     * @return True if our capital missile bay is in bearings-only mode
     *         Only available in space games
     */
    public boolean isInBearingsOnlyMode() {
        return (curMode().equals(Weapon.MODE_CAP_MISSILE_BEARING_EXT)
                || curMode().equals(Weapon.MODE_CAP_MISSILE_BEARING_LONG)
                || curMode().equals(Weapon.MODE_CAP_MISSILE_BEARING_MED)
                || curMode().equals(Weapon.MODE_CAP_MISSILE_BEARING_SHORT)
                || curMode().equals(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_EXT)
                || curMode().equals(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_LONG)
                || curMode().equals(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_MED)
                || curMode().equals(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_SHORT))
                && getEntity().isSpaceborne();
    }

    /**
     * @return True if our capital missile bay is in waypoint launch mode
     *         Only available in space games
     */
    public boolean isInWaypointLaunchMode() {
        return (curMode().equals(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_EXT)
                || curMode().equals(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_LONG)
                || curMode().equals(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_MED)
                || curMode().equals(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_SHORT)
                || curMode().equals(Weapon.MODE_CAP_MISSILE_WAYPOINT))
                && getEntity().isSpaceborne();
    }

    /**
     * Method that adds/removes available capital missile modes as we move between
     * space and atmospheric maps
     * Called by Entity.setGameOptions(), which is in turn called during a mode
     * change by server.
     */
    // Though we can't currently switch maps, this is needed to ensure space-only
    // modes are removed on ground maps
    public void setModesForMapType() {
        // If the entity is not in space, remove these modes, which get set up based on
        // game options in Weapon before game type is known
        if (!getEntity().isSpaceborne()) {
            getType().removeMode(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_EXT);
            getType().removeMode(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_LONG);
            getType().removeMode(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_MED);
            getType().removeMode(Weapon.MODE_CAP_MISSILE_WAYPOINT_BEARING_SHORT);
            getType().removeMode(Weapon.MODE_CAP_MISSILE_WAYPOINT);
            getType().removeMode(Weapon.MODE_CAP_MISSILE_TELE_OPERATED);
            getType().removeMode(Weapon.MODE_CAP_MISSILE_BEARING_EXT);
            getType().removeMode(Weapon.MODE_CAP_MISSILE_BEARING_LONG);
            getType().removeMode(Weapon.MODE_CAP_MISSILE_BEARING_MED);
            getType().removeMode(Weapon.MODE_CAP_MISSILE_BEARING_SHORT);
        }
        /*
         * //Placeholder. This will be used to add the space modes back when we're able
         * to switch maps.
         * if (getEntity().isSpaceborne()) {
         *
         * }
         */
    }

    /**
     * @return For BattleArmor, returns the location where a piece of equipment is
     *         mounted, e.g.
     *         {@link BattleArmor#MOUNT_LOC_LARM}.
     */
    public int getBaMountLoc() {
        return baMountLoc;
    }

    public void setBaMountLoc(int baMountLoc) {
        this.baMountLoc = baMountLoc;
    }

    /**
     * Checks whether this mount is either one a one-shot weapon or ammo for a
     * one-shot weapon.
     *
     * @return Whether the equipment is one-shot
     */
    public boolean isOneShot() {
        return false;
    }

    public boolean isOneShotAmmo() {
        return false;
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
        trooper = trooper - BattleArmor.LOC_TROOPER_1;
        if ((trooper < 0) || (trooper >= missingForTrooper.length)) {
            return;
        }
        missingForTrooper[trooper] = b;
    }

    public boolean isMissingForTrooper(int trooper) {
        trooper = trooper - BattleArmor.LOC_TROOPER_1;
        if ((trooper < 0) || (trooper >= missingForTrooper.length)) {
            return false;
        }
        return missingForTrooper[trooper];
    }

    public boolean isAnyMissingTroopers() {
        for (int i = 0; i < missingForTrooper.length; i++) {
            if (missingForTrooper[i]) {
                return true;
            }
        }
        return false;
    }

    public String getMissingTrooperString() {
        StringBuffer missings = new StringBuffer();
        for (int i = 0; i < missingForTrooper.length; i++) {
            missings.append(missingForTrooper[i]).append("::");
        }
        return missings.toString();
    }

    /**
     * Returns true if this Mounted is ammunition in homing mode.
     */
    public boolean isHomingAmmoInHomingMode() {
        if (!(getType() instanceof AmmoType)) {
            return false;
        }

        AmmoType ammoType = (AmmoType) getType();
        return ammoType.getMunitionType().contains(AmmoType.Munitions.M_HOMING) &&
                curMode().equals("Homing");
    }

    public int equipmentIndex() {
        if (entity != null) {
            return entity.getEquipmentNum(this);
        } else {
            return -1;
        }
    }

    public int getSwitchedReason() {
        return switchedReason;
    }

    public void setSwitchedReason(int reason) {
        switchedReason = reason;
    }

    protected List<String> bayComponentsToString() {
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        List<String> locations = allLocations().stream().map(entity::getLocationAbbr).collect(Collectors.toList());
        String intro = getType().toString()
                + " (" + String.join("/", locations)
                + (rearMounted ? "-R" : "")
                + (mekTurretMounted ? "-MTu" : "")
                + (sponsonTurretMounted ? "-STu" : "")
                + (pintleTurretMounted ? "-PTu" : "")
                + (isDWPMounted ? "-DWP" : "")
                + (isAPMMounted ? "-APM" : "")
                + (squadSupportWeapon ? "-SSW" : "")
                + (baMountLoc != -1 ? "-" + BattleArmor.MOUNT_LOC_NAMES[baMountLoc] : "")
                + (omniPodMounted ? "-Pod" : "")

                + ")";

        List<String> state = new ArrayList<>();
        if (linked != null) {
            if (linked.getEntity().getId() != entity.getId()) {
                state.add("Linked: [" + linked.getEntity() + ":" + linked.getEntity().getEquipment().indexOf(linked)
                        + "]");
            } else {
                state.add("Linked: [" + entity.getEquipment().indexOf(linked) + "]");
            }
        }
        if (linkedBy != null) {
            if (linkedBy.getEntity().getId() != entity.getId()) {
                state.add("LinkedBy: [" + linkedBy.getEntity() + ":"
                        + linkedBy.getEntity().getEquipment().indexOf(linkedBy) + "]");
            } else {
                state.add("LinkedBy: [" + entity.getEquipment().indexOf(linkedBy) + "]");
            }
        }
        if (crossLinkedBy != null) {
            if (crossLinkedBy.getEntity().getId() != entity.getId()) {
                state.add("CrossLinkedBy: [" + crossLinkedBy.getEntity() + ":"
                        + crossLinkedBy.getEntity().getEquipment().indexOf(crossLinkedBy) + "]");
            } else {
                state.add("CrossLinkedBy: [" + entity.getEquipment().indexOf(crossLinkedBy) + "]");
            }
        }
        if (linkedBayId != -1)
            state.add("LinkedBay: [" + linkedBayId + "]");
        state.addAll(bayComponentsToString());
        if (type instanceof AmmoType) {
            state.add("Shots: " + shotsLeft);
        }
        if (destroyed)
            state.add("Destroyed");
        if (hit)
            state.add("Hit");
        if (missing)
            state.add("Missing");
        if (fired)
            state.add("Fired");
        if (rapidfire)
            state.add("Rapidfire");
        if (jammed)
            state.add("Jammed");
        if (useless)
            state.add("Useless");
        if (armoredComponent)
            state.add("Armored");
        if (facing != -1)
            state.add("Facing: " + facing);
        if (!quirks.activeQuirks().isEmpty())
            state.add("Quirks: " + quirks.getOptionList("/"));
        if (weaponGroup)
            state.add("Group");
        if (nweapons != 1 || weaponGroup)
            state.add("#Weapons: " + nweapons);
        if (size != 1)
            state.add("Size: " + size);
        return intro + " { " + String.join(", ", state) + " }";
    }

    public List<EquipmentModifier> getModifiers() {
        return modifiers;
    }
}
