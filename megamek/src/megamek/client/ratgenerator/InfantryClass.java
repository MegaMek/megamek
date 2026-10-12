/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ratgenerator;

import java.util.Set;

import megamek.client.ui.Messages;
import megamek.common.annotations.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitType;

/**
 * The kinds of conventional infantry a player can ask the force generator for, in place of a weight class.
 *
 * <p>Conventional infantry has no weight class: the engine reports every platoon as Light. What does tell platoons
 * apart is how they move, so a class is defined by movement mode, plus whether the troops ride animals. A beast-mounted
 * platoon takes its movement from the animal (a horse moves as Leg, a Branth flies as VTOL), so the mount is checked
 * first and a mounted platoon is always {@link #BEAST}, whatever its movement.</p>
 *
 * <p>Picking a class is strict: every line platoon the generator builds must match it.</p>
 */
public enum InfantryClass {
    /** Foot infantry. */
    LIGHT("ForceGeneratorDialog.infantryClass.light"),
    /** Infantry on motorcycles, cars and similar light transport. */
    MOTORIZED("ForceGeneratorDialog.infantryClass.motorized"),
    /** Infantry carried in tracked, wheeled or hover vehicles. */
    MECHANIZED("ForceGeneratorDialog.infantryClass.mechanized"),
    /** Jump infantry, and microcopter or microlite infantry. */
    JUMP("ForceGeneratorDialog.infantryClass.jump"),
    /** Infantry riding animals. */
    BEAST("ForceGeneratorDialog.infantryClass.beast"),
    /** Scuba and submarine infantry. */
    AQUATIC("ForceGeneratorDialog.infantryClass.aquatic");

    /** Faction option narrowing {@link #BEAST} to land animals (mounts that move as Leg). */
    public static final String FLAG_BEAST_LAND = "beastLand";
    /** Faction option narrowing {@link #BEAST} to flying animals (mounts that move as VTOL). */
    public static final String FLAG_BEAST_FLYING = "beastFlying";
    /** Faction option narrowing {@link #BEAST} to swimming animals (mounts that move as Submarine). */
    public static final String FLAG_BEAST_SWIMMING = "beastSwimming";

    private final String messageKey;

    InfantryClass(String messageKey) {
        this.messageKey = messageKey;
    }

    /**
     * @return the name shown to the player, e.g. "Mechanized"
     */
    public String getDisplayName() {
        return Messages.getString(messageKey);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    /**
     * Works out which class a platoon belongs to.
     *
     * @param movementMode the platoon's movement mode (for a beast-mounted platoon, the animal's)
     * @param isMounted    {@code true} when the troops ride animals
     *
     * @return the class, or {@code null} when the movement mode is not one conventional infantry uses
     */
    public static @Nullable InfantryClass classify(@Nullable EntityMovementMode movementMode, boolean isMounted) {
        if (isMounted) {
            return BEAST;
        }
        if (movementMode == null) {
            return null;
        }
        return switch (movementMode) {
            case INF_LEG -> LIGHT;
            case INF_MOTORIZED -> MOTORIZED;
            case TRACKED, WHEELED, HOVER -> MECHANIZED;
            case INF_JUMP, VTOL -> JUMP;
            case INF_UMU, SUBMARINE -> AQUATIC;
            default -> null;
        };
    }

    /**
     * Works out which class a unit from the unit cache belongs to.
     *
     * @param unit a unit summary
     *
     * @return the class, or {@code null} when the unit is not conventional infantry or moves in a way no class covers
     */
    public static @Nullable InfantryClass classify(MekSummary unit) {
        if (!UnitType.getTypeName(UnitType.INFANTRY).equals(unit.getUnitType())) {
            return null;
        }
        // The cache stores an infantry platoon's movement as text ("Leg", "Motorized SCUBA", "Microcopter").
        return classify(EntityMovementMode.parseFromString(unit.getUnitSubType()), unit.getMountedInfantry());
    }

    /**
     * Whether a unit from the unit cache belongs to this class and, for {@link #BEAST}, to the kind of animal the
     * faction options ask for.
     *
     * @param unit  a unit summary
     * @param flags the force's faction options; only the beast options are read
     *
     * @return {@code true} when the unit may fill a slot of this class
     */
    public boolean matches(MekSummary unit, Set<String> flags) {
        if (classify(unit) != this) {
            return false;
        }
        if (this != BEAST) {
            return true;
        }
        return matchesBeastKind(EntityMovementMode.parseFromString(unit.getUnitSubType()), flags);
    }

    /**
     * Whether a beast-mounted platoon's animal is the kind the faction options ask for. With no beast option set, any
     * animal will do.
     *
     * @param mountMovementMode the animal's movement mode
     * @param flags             the force's faction options
     *
     * @return {@code true} when the animal is acceptable
     */
    static boolean matchesBeastKind(@Nullable EntityMovementMode mountMovementMode, Set<String> flags) {
        if (flags.contains(FLAG_BEAST_LAND)) {
            return mountMovementMode == EntityMovementMode.INF_LEG;
        }
        if (flags.contains(FLAG_BEAST_FLYING)) {
            return mountMovementMode == EntityMovementMode.VTOL;
        }
        if (flags.contains(FLAG_BEAST_SWIMMING)) {
            return mountMovementMode == EntityMovementMode.SUBMARINE;
        }
        return true;
    }
}
