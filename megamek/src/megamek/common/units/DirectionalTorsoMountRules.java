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
package megamek.common.units;

import java.util.Optional;

import megamek.common.Report;
import megamek.common.compute.Compute;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.rolls.Roll;
import megamek.logging.MMLogger;

/**
 * Static helpers for the Directional Torso Mount quirk (BMM p.83). Keeps the server-side resolution in one focused
 * place rather than expanding the damage manager; callers invoke a single method.
 */
public final class DirectionalTorsoMountRules {

    private static final MMLogger LOGGER = MMLogger.create(DirectionalTorsoMountRules.class);

    /** Report id for "the Directional Torso Mount is destroyed and locked" (report-messages.properties). */
    private static final int DIRECTIONAL_MOUNT_LOCKED_REPORT = 5349;

    /** Report id for "the Directional Torso Mount survives the hit" (report-messages.properties). */
    private static final int DIRECTIONAL_MOUNT_HOLDS_REPORT = 5350;

    /** A 2D6 result of this or higher destroys the mount and locks the weapon's arc (BMM p.83). */
    private static final int LOCK_TARGET_NUMBER = 9;

    private DirectionalTorsoMountRules() {}

    /**
     * Resolves the Directional Torso Mount lock check after a Mek location takes a hit (BMM p.83): if the location
     * holds at least one weapon in a Directional Torso Mount whose arc is not already locked, rolls 2D6 and, on a
     * result of 9 or higher, destroys the mount's rotation mechanism - locking every Directional Torso Mount weapon in
     * that location into its current arc. The weapons remain able to fire. Other results, and locations with no
     * (still-rotatable) directional mount, leave the mounts untouched.
     *
     * @param mek      the Mek whose location took a hit
     * @param location the location that took a hit
     *
     * @return a {@link Report} for the check when the location holds a still-rotatable directional mount - describing
     *       either the destroyed/locked mount (9+) or that the mount held (under 9), so the player always sees the
     *       roll; empty when the location has no (still-rotatable) directional mount and no roll is made
     */
    public static Optional<Report> rollLockFromLocationDamage(Mek mek, int location) {
        boolean hasRotatableMount = mek.getWeaponList().stream()
              .anyMatch(weapon -> isRotatableDirectionalMountInLocation(weapon, location));
        if (!hasRotatableMount) {
            return Optional.empty();
        }

        Roll diceRoll = Compute.rollD6(2);
        boolean locked = diceRoll.getIntValue() >= LOCK_TARGET_NUMBER;
        LOGGER.debug("[DirTorsoMount] {}: hit to Directional Torso Mount location {} - lock roll {} ({})",
              mek.getShortName(), location, diceRoll.getIntValue(), locked ? "locked" : "intact");
        if (!locked) {
            // Always report the check so the player can see the roll happened, even when the mount holds (BMM p.83).
            Report holds = new Report(DIRECTIONAL_MOUNT_HOLDS_REPORT);
            holds.subject = mek.getId();
            holds.addDesc(mek);
            holds.add(diceRoll);
            return Optional.of(holds);
        }

        for (WeaponMounted weapon : mek.getWeaponList()) {
            if (isRotatableDirectionalMountInLocation(weapon, location)) {
                weapon.setDirectionalMountLocked(true);
            }
        }
        LOGGER.info("[DirTorsoMount] {}: Directional Torso Mount in location {} destroyed; weapons locked"
              + " in their current arc", mek.getShortName(), location);

        Report report = new Report(DIRECTIONAL_MOUNT_LOCKED_REPORT);
        report.subject = mek.getId();
        report.addDesc(mek);
        report.add(diceRoll);
        return Optional.of(report);
    }

    /**
     * Applies a player-declared Directional Torso Mount arc change (BMM p.83). A Directional Torso Mount is a single
     * mount that holds every directional-mount weapon in one torso location, so the change is applied to the whole
     * mount: the referenced weapon and every other Directional Torso Mount weapon sharing its location rotate together.
     * The change is ignored - with a logged reason - if the weapon is not in a directional mount or the mount has been
     * locked by damage.
     *
     * @param entity       the acting entity
     * @param weaponNumber the equipment number of any weapon in the mount whose arc is being set
     * @param facing       the mount facing offset to apply (0-5; a 2-point mount accepts only 0/front or 3/rear)
     */
    public static void applyMountFacing(Mek entity, int weaponNumber, int facing) {
        Mounted<?> weapon = entity.getEquipment(weaponNumber);
        if (weapon == null) {
            LOGGER.info("[DirTorsoMount] {}: server cannot reface mount - no equipment #{}",
                  entity.getShortName(), weaponNumber);
            return;
        }
        if (!weapon.hasDirectionalTorsoMount()) {
            LOGGER.info("[DirTorsoMount] {}: server ignored facing - equipment #{} ({}) is not a directional mount",
                  entity.getShortName(), weaponNumber, weapon.getName());
            return;
        }
        if (weapon.isDirectionalMountLocked()) {
            LOGGER.info("[DirTorsoMount] {}: server ignored facing - {} mount is locked",
                  entity.getShortName(), weapon.getName());
            return;
        }
        if (weapon.isDirectionalMountAlreadyFlipped()) {
            LOGGER.info("[DirTorsoMount] {}: server ignored facing - {} mount already changed facing in an earlier"
                  + " phase this turn (once per turn, BMM p.83)", entity.getShortName(), weapon.getName());
            return;
        }
        int normalizedFacing = ((facing % 6) + 6) % 6;
        // The 2-point mount may only face forward (0) or rear (3); only the 3-point quad turret rotates freely.
        if (!weapon.hasDirectional360TorsoMount() && (normalizedFacing != 0) && (normalizedFacing != 3)) {
            LOGGER.info("[DirTorsoMount] {}: server rejected facing {} - {} is a 2-point mount (front/rear only)",
                  entity.getShortName(), normalizedFacing, weapon.getName());
            return;
        }
        int refaced = setMountFacing(entity, weapon.getLocation(), normalizedFacing);
        LOGGER.info("[DirTorsoMount] {}: server applied {} mount facing offset -> {} ({} weapon(s) in location {})",
              entity.getShortName(), weapon.getName(), normalizedFacing, refaced, weapon.getLocation());
    }

    /**
     * Sets the facing offset of every unlocked Directional Torso Mount weapon in a single location. All weapons in a
     * Directional Torso Mount share one facing (they occupy the same mount), so this is the shared entry point used by
     * the client controls and the server to keep them in lockstep. A 2-point mount weapon only accepts the front (0) or
     * rear (3) offset; a locked weapon, or one whose facing was already changed in an earlier phase this turn (once per
     * turn, BMM p.83), is left untouched.
     *
     * @param entity   the unit carrying the mount
     * @param location the torso location whose Directional Torso Mount is being rotated
     * @param facing   the facing offset to apply (0-5)
     *
     * @return the number of weapons whose facing was updated
     */
    public static int setMountFacing(Entity entity, int location, int facing) {
        int normalizedFacing = ((facing % 6) + 6) % 6;
        int updated = 0;
        for (WeaponMounted weapon : entity.getWeaponList()) {
            if ((weapon.getLocation() != location) || !weapon.hasDirectionalTorsoMount()
                  || weapon.isDirectionalMountLocked() || weapon.isDirectionalMountAlreadyFlipped()) {
                continue;
            }
            // A 2-point mount may only face front or rear; skip any other offset for it.
            if (!weapon.hasDirectional360TorsoMount() && (normalizedFacing != 0) && (normalizedFacing != 3)) {
                continue;
            }
            weapon.setDirectionalMountFacing(normalizedFacing);
            updated++;
        }
        return updated;
    }

    /**
     * @param weapon   the weapon to test
     * @param location the location that took a hit
     *
     * @return {@code true} if the weapon is in the given location, is in a Directional Torso Mount, and its arc is not
     *       already locked
     */
    private static boolean isRotatableDirectionalMountInLocation(WeaponMounted weapon, int location) {
        return (weapon.getLocation() == location)
              && weapon.hasDirectionalTorsoMount()
              && !weapon.isDirectionalMountLocked();
    }
}
