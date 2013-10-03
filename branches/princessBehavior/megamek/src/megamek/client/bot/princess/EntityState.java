/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
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
package megamek.client.bot.princess;

import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.EntityMovementType;
import megamek.common.IGame;
import megamek.common.IHex;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.MovePath;
import megamek.common.RangeType;
import megamek.common.Targetable;
import megamek.common.WeaponType;
import megamek.common.util.LogLevel;
import megamek.common.util.Logger;
import megamek.common.weapons.MMLWeapon;
import megamek.common.weapons.PulseLaserBayWeapon;

import java.text.DecimalFormat;

/**
 * EntityState describes a hypothetical situation an entity could be in when firing
 *
 * @version %Id%
 * @author: Deric "Netzilla" Page (deric dot page at usa dot net)
 * @since: 9/8/13 1:19 PM
 */
public class EntityState {
    private Coords position;
    private int facing;
    private int secondaryFacing; // to account for torso twists
    private int heat;
    private int hexesMoved;
    private boolean isProne;
    private boolean isImmobile;
    private boolean isJumping;
    private EntityMovementType movementType;
    private Targetable me;

    protected EntityState() {}

    /**
     * Initialize an entity state from the state an entity is actually in
     * (or something that isn't an entity)
     */
    EntityState(Targetable t) {
        me = t;
        if (t instanceof Entity) { // mechs and planes and tanks etc
            Entity e = (Entity)t;
            init(e);
        } else { // for buildings and such
            init(t);
        }
    }

    protected void init(Entity entity) {
        position = entity.getPosition();
        facing = entity.getFacing();
        hexesMoved = entity.getDeltaDistance();
        heat = entity.getHeat();
        isProne = entity.isProne() || entity.isHullDown();
        isImmobile = entity.isImmobile();
        isJumping = (entity.getMoved() == EntityMovementType.MOVE_JUMP);
        movementType = entity.getMoved();
        secondaryFacing = entity.getSecondaryFacing();
    }

    protected void init(Targetable t) {
        position = t.getPosition();
        facing = 0;
        hexesMoved = 0;
        heat = 0;
        isProne = false;
        isImmobile = true;
        isJumping = false;
        movementType = EntityMovementType.MOVE_NONE;
        secondaryFacing = 0;
    }

    /**
     * Initialize an entity state from a movement path
     */
    EntityState(MovePath path) {
        me = path.getEntity();
        init(path);
    }

    protected void init(MovePath path) {
        position = path.getFinalCoords();
        facing = path.getFinalFacing();
        hexesMoved = path.getHexesMoved();
        heat = path.getEntity().getHeat();
        if (path.getLastStepMovementType() == EntityMovementType.MOVE_WALK) {
            heat += 1;
        } else if (path.getLastStepMovementType() == EntityMovementType.MOVE_RUN) {
            heat += 2;
        } else if ((path.getLastStepMovementType() == EntityMovementType.MOVE_JUMP)
                   && (hexesMoved <= 3)) {
            heat += 3;
        } else if ((path.getLastStepMovementType() == EntityMovementType.MOVE_JUMP)
                   && (hexesMoved > 3)) {
            heat += hexesMoved;
        }
        isProne = path.getFinalProne() || path.getFinalHullDown();
        isImmobile = path.getEntity().isImmobile();
        isJumping = path.isJumping();
        movementType = path.getLastStepMovementType();
        secondaryFacing = facing;
    }

    public Coords getPosition() {
        return position;
    }

    public int getFacing() {
        return facing;
    }

    public int getSecondaryFacing() {
        return secondaryFacing;
    }

    public void setSecondaryFacing(int secondaryFacing) {
        this.secondaryFacing = secondaryFacing;
    }

    public int getHeat() {
        return heat;
    }

    public int getHexesMoved() {
        return hexesMoved;
    }

    public boolean isProne() {
        return isProne;
    }

    public boolean isImmobile() {
        return isImmobile;
    }

    public boolean isJumping() {
        return isJumping;
    }

    public EntityMovementType getMovementType() {
        return movementType;
    }

    public IHex getHex(IGame game) {
        return game.getBoard().getHex(getPosition());
    }

    public int getTotalElevation(IGame game) {
        return getHex(game).getElevation() + me.getElevation();
    }

    public int getTotalHeight(IGame game) {
        return getHex(game).getElevation() + me.absHeight();
    }

    public boolean isCommander() {
        return (me instanceof Entity) && ((Entity) me).isCommander();
    }

    /**
     * Returns the estimated damage I can do at a given range.
     *
     * @param range The range to be tested.
     * @return the estimated damage I can do at a given range.
     */
    public double estimatedDamageAtRange(int range, boolean extremeRange) {
        if (!(me instanceof Entity)) {
            return 0;
        }

        double ret = 0;
        boolean artemisV = false;
        Entity shooter = (Entity)me;

        StringBuilder msg = new StringBuilder("Calculating DMG at Range (" + range + ") for ")
                .append(shooter.getDisplayName());

        // cycle through my weapons
        for (Mounted weapon : shooter.getWeaponList()) {
            double damage = 0;
            // Skip AMS.
            WeaponType weaponType = (WeaponType) weapon.getType();
            if (weaponType.hasFlag(WeaponType.F_AMS)) {
                continue;
            }
            msg.append("\n\t").append(weaponType.getShortName());

            int rangeBracket = RangeType.rangeBracket(range, weaponType.getRanges(weapon), extremeRange);
            msg.append("\n\tRange Bracket: ").append(RangeType.getBracketName(rangeBracket));

            // If we're out of range, we can't fire.
            if (rangeBracket == RangeType.RANGE_OUT) {
                continue;
            }

            // With cluster weapons, assume a 7 will be rolled on the Cluster Hits Table.
            if (WeaponType.DAMAGE_BY_CLUSTERTABLE == weaponType.getDamage()) {
                msg.append("\n\t\tCluster Weapon");
                int clusterRoll = 7;

                // Account for Artemis systems.
                if ((AmmoType.T_LRM == weaponType.getAmmoType()) || (AmmoType.T_SRM == weaponType.getAmmoType())) {
                    Mounted linked = weapon.getLinkedBy();
                    if (linked != null) {
                        // Is it usable?
                        if ((linked.getType() instanceof MiscType) && !linked.isDestroyed() && !linked.isMissing() &&
                            !linked.isBreached()) {
                            if (linked.getType().hasFlag(MiscType.F_ARTEMIS_V)) {
                                clusterRoll += 3;
                                artemisV = true;
                            } else if (linked.getType().hasFlag(MiscType.F_ARTEMIS)) {
                                clusterRoll += 2;
                            }
                        }
                    }

                // Account for streak systems.
                } else if ((AmmoType.T_SRM_STREAK == weaponType.getAmmoType()) ||
                           (AmmoType.T_LRM_STREAK == weaponType.getAmmoType())) {
                    clusterRoll = 12;
                }
                msg.append(", Cluster Roll: ").append(clusterRoll);
                int rack = weaponType.getRackSize();
                msg.append(", Rack Size: ").append(rack);
                int hits = Compute.calculateClusterHitTableAmount(clusterRoll, rack);
                msg.append(", Hits: ").append(hits);
                damage = hits;
                // todo should probably check to make sure the correct ammo type is carried for MMLs and ATMs.
                if ((weaponType instanceof MMLWeapon) && (range <= 9)) {
                    damage *= 2;
                } else if (AmmoType.T_SRM == weaponType.getAmmoType()) {
                    damage *= 2;
                } else if (AmmoType.T_ATM == weaponType.getAmmoType()) {
                    if (range <= 9) {
                        damage *= 3;
                    } else if (range <= 15) {
                        damage *= 2;
                    }
                }
                msg.append(", Damage: ").append(damage);

            } else if (WeaponType.DAMAGE_VARIABLE == weaponType.getDamage()) {
                damage = weaponType.getDamage(range);
                msg.append("\n\t\tDamage: ").append(damage);
            } else {
                damage = weaponType.getDamage();
                msg.append("\n\t\tDamage: ").append(damage);
            }

            // Get skill of shooter.
            int skill = shooter.getCrew().getGunnery();
            msg.append("\n\t\tSkill (").append(skill).append(")");

            // Account for range.
            int rangeMod = 0;
            switch (rangeBracket) {
                case RangeType.RANGE_MINIMUM:
                    rangeMod = (weaponType.getMinimumRange() + 1) - range;
                    break;
                case RangeType.RANGE_MEDIUM:
                    rangeMod = 2;
                    break;
                case RangeType.RANGE_LONG:
                    rangeMod = 4;
                    break;
                case RangeType.RANGE_EXTREME:
                    rangeMod = 6;
                    break;
            }
            skill += rangeMod;
            msg.append(" + Range Mod (").append(rangeMod).append(")");

            // Account for shooter's movement.
            int moveMod = 0;
            if (EntityMovementType.MOVE_JUMP.equals(shooter.moved)) {
                moveMod = 3;
            } else if (EntityMovementType.MOVE_RUN.equals(shooter.getMoved())
                    || EntityMovementType.MOVE_OVER_THRUST.equals(shooter.getMoved())
                    || EntityMovementType.MOVE_SUBMARINE_RUN.equals(shooter.getMoved())
                    || EntityMovementType.MOVE_VTOL_RUN.equals(shooter.getMoved())) {
                moveMod = 2;
            } else if (!EntityMovementType.MOVE_NONE.equals(shooter.getMoved())) {
                moveMod = 1;
            }
            skill += moveMod;
            msg.append(" + Attacker Move Mod (").append(moveMod).append(")");

            // Targetting Comp.
            if (shooter.hasTargComp() && weaponType.hasFlag(WeaponType.F_DIRECT_FIRE)
                    && (AmmoType.T_AC_LBX != weaponType.getAmmoType())
                    && (AmmoType.T_AC_LBX_THB != weaponType.getAmmoType())) {
                msg.append(" - TC (1)");
                skill -= 1;
            }

            // Artemis V
            if (artemisV) {
                msg.append(" - A5 (1)");
                skill -= 1;
            }

            // ToDo MRMs & Rocket Launchers.

            // Pulse
            if (weaponType instanceof PulseLaserBayWeapon) {
                skill -= 2;
                msg.append(" - Pulse (2)");
            }
            msg.append(" = ").append(skill);

            // Multiply damage by odds to hit.
            damage *= Compute.oddsAbove(skill) / 100;
            msg.append("\n\t\tEstimated Damage: ").append(DecimalFormat.getInstance().format(damage));
            ret += damage;
        }

        msg.append("\n\tTotal Estimated Damage: ").append(DecimalFormat.getInstance().format(ret));
        Logger.log(getClass(), "estimatedDamageAtRange(Entity, int)", LogLevel.DEBUG, msg.toString());
        return ret;
    }
}
