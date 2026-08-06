/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.units;

import java.io.Serial;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.GunEmplacement;
import megamek.common.equipment.HandheldWeapon;
import megamek.common.game.Game;
import megamek.common.game.GameTurn;

/**
 * A type of game turn that allows only certain types of units to move.
 */
public class EntityClassTurn extends GameTurn {
    @Serial
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
    public static final int CLASS_AERO = 32;

    /** The constant to represent space station entities. */
    public static final int CLASS_SPACE_STATION = 64;

    /** The constant to represent jumpship entities. */
    public static final int CLASS_JUMPSHIP = 128;

    /** The constant to represent warship entities. */
    public static final int CLASS_WARSHIP = 256;

    /** The constant to represent dropship entities. */
    public static final int CLASS_DROPSHIP = 512;

    /** The constant to represent warship entities. */
    public static final int CLASS_SMALL_CRAFT = 1024;

    /** The constant to represent handheld weapon entities. */
    public static final int CLASS_HANDHELD_WEAPON = 2048;

    /** The constant to represent building entities. */
    public static final int CLASS_BUILDING_ENTITY = 4096;

    /**
     * Only allow entities for the given player which have types in the class mask to move.
     *
     * @param playerId  the <code>int</code> ID of the player
     * @param classMask the <code>int</code> bitmask containing all the valid class types for this move.
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
     *
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
     *
     * @return <code>true</code> if entities of that class can move.
     */
    public boolean isValidClass(int classCode) {
        return (classCode & mask) != 0;
    }

    /**
     * Get the class code of this turn
     *
     * @return the class code of this turn
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
     *
     * @return the <code>int</code> code for the entity's class.
     */
    public static int getClassCode(final Entity entity) {
        // Start with subclasses of Aero
        if (entity instanceof SpaceStation) {
            return CLASS_SPACE_STATION;
        } else if (entity instanceof Warship) {
            return CLASS_WARSHIP;
        } else if (entity instanceof Jumpship) {
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
        } else if (entity instanceof HandheldWeapon) {
            return CLASS_HANDHELD_WEAPON;
        } else if (entity instanceof AbstractBuildingEntity) {
            return CLASS_BUILDING_ENTITY;
        } else {
            return 0;
        }
    }
}
