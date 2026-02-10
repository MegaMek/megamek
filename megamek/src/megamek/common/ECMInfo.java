/*
 * Copyright (C) 2015 Nicholas Walczak (walczak@cs.umn.edu)
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common;

import java.util.Objects;

import megamek.common.board.Coords;
import megamek.common.units.Entity;

/**
 * A class that keeps track of information related to an ECM field.
 *
 * @author arlith
 */
public class ECMInfo {

    /**
     * The radius of the field.
     */
    int range;

    /**
     * The center location of the field.
     */
    Coords pos;

    /**
     * Used in rare cases that E(C)CM is directional, like active probes on Aerospace Fighters in space.
     */
    int direction = -1;

    /**
     * The id of the owning player, used to determine information about who are allies and enemies when looking at
     * other
     * <code>ECMInfo</code>s.
     */
    Player owner = null;

    /**
     * The Entity that is generating the ECM field.
     */
    Entity owningEntity = null;

    /**
     * The strength of the ECM field, counted in number of fields.  This could be the number of friendly ECM fields, or
     * the number of enemy ECM fields depending upon context.
     */
    double strength = 0;

    /**
     * The strength of the Angel ECM field, counted in number of fields.  This is necessary as Angel ECM fields are
     * inherently stronger than those from other ECM.
     */
    int angelStrength = 0;

    /**
     * The strength of the ECCM field, counted in number of fields.  This could be the number of friendly ECCM fields,
     * or the number of enemy ECCM fields depending upon context.
     */
    int eccmStrength = 0;

    /**
     * The strength of the Angel ECCM field, counted in number of fields.  This is necessary as Angel ECCM fields are
     * inherently stronger than those from other ECCM.
     */
    int angelECCMStrength = 0;

    /**
     * Indicates whether the ECM strength includes ECM from a Nova CEWS.  From what I can see, Nova ECM acts like
     * regular ECM, except that it's the only ECM that can disrupt the Nova C3i system.
     */
    boolean isECMNova = false;

    public ECMInfo() {
    }

    public ECMInfo(int range, double strength, Player o, Coords p) {
        owner = o;
        pos = p;
        this.range = range;
        this.strength = strength;
    }

    public ECMInfo(int range, double strength, Entity e) {
        owningEntity = e;
        owner = e.getOwner();
        pos = e.getPosition();
        this.range = range;
        this.strength = strength;
    }

    public ECMInfo(int range, Coords pos, Player owner, double strength, int angelStrength) {
        this.range = range;
        this.pos = pos;
        this.owner = owner;
        this.strength = strength;
        this.angelStrength = angelStrength;
    }

    public ECMInfo(ECMInfo other) {
        this.range = other.range;
        this.pos = other.pos;
        this.owner = other.owner;
        this.strength = other.strength;
        this.angelStrength = other.angelStrength;
        this.eccmStrength = other.eccmStrength;
        this.angelECCMStrength = other.angelECCMStrength;
        this.direction = other.direction;
        this.owningEntity = other.owningEntity;
        this.isECMNova = other.isECMNova;
    }

    public boolean isAngel() {
        return angelStrength > 0;
    }

    /**
     * @param range    Range of ECM
     * @param position {@link Coords} for center
     * @param strength Strength of ECM
     */
    public ECMInfo(int range, Coords position, double strength) {
        this.range = range;
        pos = position;
        this.strength = strength;
    }

    /**
     * @return true if this ECMInfo is considered to be ECCM;  that is, if the strength of ECCM is greater than the
     *       strength of ECCM.
     */
    public boolean isECCM() {
        return (angelECCMStrength > angelStrength) || (angelStrength == 0 && eccmStrength > strength);
    }

    public boolean isAngelECCM() {
        return (angelECCMStrength > angelStrength);
    }

    /**
     * @return True if the number of ECM fields is greater than the number of ECCM fields (which default to 0).
     */
    public boolean isECM() {
        return (angelStrength > angelECCMStrength) || (angelECCMStrength == angelStrength && strength > eccmStrength);
    }

    public boolean isAngelECM() {
        return (angelStrength > angelECCMStrength);
    }


    /**
     * Compute the ECMInfo from another instance into this one, where this ECMInfo contains information about fields
     * opposed to the owner. All enemy ECM strength is added, and all allied ECCM strength is added.
     *
     * @param other Other {@link ECMInfo} Instance
     */
    public void addOpposingECMEffects(ECMInfo other) {
        // Enemy ECM (ECM without an owner is always considered an enemy)
        // If this.owner is null, treat all others as enemies (hostile-to-everyone ECM like chaff or EMP mines)
        if ((owner == null) || (other.owner == null) || owner.isEnemyOf(other.owner)) {
            strength += other.strength;
            angelStrength += other.angelStrength;
            isECMNova |= other.isECMNova;
            // Allied ECCM
        } else if (!owner.isEnemyOf(other.owner)) {
            eccmStrength += other.eccmStrength;
            angelECCMStrength += other.angelECCMStrength;
        }
    }

    /**
     * Compute the ECMInfo from another instance into this one, where this ECMInfo contains information about fields
     * allied to the owner. All allied ECM strength is added, and all enemy ECCM strength is added.
     *
     * @param other Other {@link ECMInfo} Instance
     */
    public void addAlliedECMEffects(ECMInfo other) {
        // Enemy ECCM (ECCM without an owner is always considered an enemy)
        // If this.owner is null, treat all others as enemies (hostile-to-everyone ECM like chaff or EMP mines)
        if ((owner == null) || (other.owner == null) || owner.isEnemyOf(other.owner)) {
            eccmStrength += other.eccmStrength;
            angelECCMStrength += other.angelECCMStrength;
            // Allied ECM
        } else if (!owner.isEnemyOf(other.owner)) {
            strength += other.strength;
            angelStrength += other.angelStrength;
            isECMNova |= other.isECMNova;
        }
    }

    @Override
    public String toString() {
        String ownerString;
        String strengthString = "";
        String eccmString = "";

        if (owner != null) {
            ownerString = owner.getName();
        } else {
            ownerString = "none";
        }
        if (angelStrength != 0) {
            strengthString = ", aS: " + angelStrength;
        } else if (strength != 0) {
            if (isECMNova) {
                strengthString = ", nS: " + strength;
            } else {
                strengthString = ", s: " + strength;
            }
        }

        if (angelECCMStrength != 0) {
            eccmString = ", cAS: " + angelECCMStrength;
        } else if (eccmStrength != 0) {
            eccmString = ", cS: " + eccmStrength;
        }
        return "(" + pos.toString() + ", " + ownerString + ", r:" + range + strengthString + eccmString + ")";
    }

    /**
     * @param other Other {@link ECMInfo} Instance
     *
     * @return true if the supplied ECMInfo is opposed to this one.
     */
    public boolean isOpposed(ECMInfo other) {
        return (owner == null) || (other.getOwner() == null) || owner.isEnemyOf(other.getOwner());
    }

    /**
     * @param other Other {@link ECMInfo} Instance
     *
     * @return true if the supplied ECMInfo is opposed to this one.
     */
    public boolean isOpposed(Player other) {
        return (owner == null) || (other == null) || owner.isEnemyOf(other);
    }

    /**
     * @param object Other Object to compare to.
     *
     * @return Equality is based on whether position, owner, range and all strengths match.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if ((null == object) || (getClass() != object.getClass())) {
            return false;
        }

        final ECMInfo other = (ECMInfo) object;
        return Objects.equals(owner, other.owner) &&
              Objects.equals(pos, other.pos) &&
              (strength == other.strength) &&
              (angelStrength == other.angelStrength) &&
              (eccmStrength == other.eccmStrength) &&
              (angelECCMStrength == other.angelECCMStrength) &&
              (isECMNova == other.isECMNova) &&
              (range == other.range);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, pos, strength, angelStrength, eccmStrength, angelECCMStrength, isECMNova, range);
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = Math.max(0, range);
    }

    public Coords getPos() {
        return pos;
    }

    public void setPos(Coords pos) {
        this.pos = pos;
    }

    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public double getECMStrength() {
        return strength;
    }

    public int getAngelECMStrength() {
        return angelStrength;
    }

    public void setAngelECMStrength(int angelStrength) {
        this.angelStrength = Math.max(0, angelStrength);
    }

    public int getECCMStrength() {
        return eccmStrength;
    }

    public void setECCMStrength(int eccmStrength) {
        this.eccmStrength = Math.max(0, eccmStrength);
    }

    public int getAngelECCMStrength() {
        return angelECCMStrength;
    }

    public void setAngelECCMStrength(int angelECCMStrength) {
        this.angelECCMStrength = Math.max(0, angelECCMStrength);
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
    }

    public boolean isNovaECM() {
        return isECM() && isECMNova;
    }

    public boolean isNova() {
        return isECMNova;
    }

    public void setECMNova(boolean isECMNova) {
        this.isECMNova = isECMNova;
    }

    public Entity getEntity() {
        return owningEntity;
    }

    public void setEntity(Entity owningEntity) {
        this.owningEntity = owningEntity;
    }
}
