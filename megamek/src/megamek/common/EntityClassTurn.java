/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common;

import megamek.common.annotations.Nullable;

/**
 * A type of game turn that allows only certain types of units to move.
 */
public class EntityClassTurn extends GameTurn {
    private static final long serialVersionUID = 1305684619846966124L;
    private final int mask;

    /** The constant to represent Infantry (and Battle Armor) entities. */
    public static final int CLASS_INFANTRY = 1;

    /** The constant to represent Protomek entities. */
    public static final int CLASS_PROTOMEK = 2;

    /** The constant to represent Tank entities. */
    public static final int CLASS_TANK = 4;

    /** The constant to represent Mek entities. */
    public static final int CLASS_MEK = 8;

    /** The constant to represent Gun Emplacement entities. */
    public static final int CLASS_GUN_EMPLACEMENT = 16;

    /** The constant to represent Aero entities. */
    public static final int CLASS_AERO   = 32;

    /** The constant to represent space station entities. */
    public static final int CLASS_SPACE_STATION   = 64;

    /** The constant to represent jumpship entities. */
    public static final int CLASS_JUMPSHIP   = 128;

    /** The constant to represent warship entities. */
    public static final int CLASS_WARSHIP   = 256;

    /** The constant to represent dropship entities. */
    public static final int CLASS_DROPSHIP   = 512;

    /** The constant to represent warship entities. */
    public static final int CLASS_SMALL_CRAFT   = 1024;

    /**
     * Only allow entities for the given player which have types in the
     * class mask to move.
     *
     * @param playerId  the <code>int</code> ID of the player
     * @param classMask the <code>int</code> bitmask containing all the
     *                  valid class types for this move.
     */
    public EntityClassTurn(int playerId, int classMask) {
        super(playerId);
        mask = classMask;
    }

    /**
     * Determine if the given entity is a valid one to use for this turn.
     *
     * @param entity the <code>Entity</code> being tested for the move.
     * @param game   The {@link Game} the entity belongs to
     * @return <code>true</code> if the entity can be moved.
     */
    @Override
    public boolean isValidEntity(final @Nullable Entity entity, final Game game,
                                 final boolean useValidNonInfantryCheck) {
        // The entity must pass the requirements of the parent class and be in the mask.
        return super.isValidEntity(entity, game, useValidNonInfantryCheck)
                && isValidClass(getClassCode(entity));
    }

    /**
     * Determine if entities of the given class get to move.
     *
     * @param classCode the <code>int</code> class code being tested
     * @return <code>true</code> if entities of that class can move.
     */
    public boolean isValidClass(int classCode) {
        return (classCode & mask) != 0;
    }

    /**
     * Get the class code of this turn
     *
     * @return the classcode of this turn
     */
    public int getTurnCode() {
        return mask;
    }

    @Override
    public String toString() {
        return super.toString() + "; Mask: " + ((mask < 0) ? "NOT " + ~mask : mask);
    }

    /**
     * Get the class code for the given entity.
     *
     * @param entity the <code>Entity</code> whose class code is needed.
     * @return the <code>int</code> code for the entity's class.
     */
    public static int getClassCode(final Entity entity) {
        // Start with subclasses of Aero
        if (entity instanceof SpaceStation) {
            return CLASS_SPACE_STATION;
        } else if (entity instanceof Warship) {
            return CLASS_WARSHIP;
        }  else if (entity instanceof Jumpship) {
            return CLASS_JUMPSHIP;
        } else if (entity instanceof Dropship) {
            return entity.isAirborne() ? CLASS_DROPSHIP : CLASS_TANK;
        } else if ((entity instanceof SmallCraft) && entity.isAirborne()) {
            return CLASS_SMALL_CRAFT;
            // Anything else that's still airborne is treated as an Aero
            // (VTOLs aren't considered airborne, since it's based on altitude and not elevation)
        } else if (entity.isAirborne()) {
            return CLASS_AERO;
        } else if (entity instanceof Infantry) {
            return CLASS_INFANTRY;
        } else if (entity instanceof ProtoMek) {
            return CLASS_PROTOMEK;
        } else if (entity instanceof GunEmplacement) {
            return CLASS_GUN_EMPLACEMENT;
        } else if ((entity instanceof Tank) || entity.isAero()) {
            return CLASS_TANK;
        } else if (entity instanceof Mek) {
            return CLASS_MEK;
        } else {
            return 0;
        }
    }
}
