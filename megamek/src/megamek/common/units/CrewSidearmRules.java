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

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.WeaponType;
import megamek.common.game.Game;
import megamek.common.options.OptionsConstants;
import megamek.common.weapons.infantry.InfantryWeapon;
import megamek.logging.MMLogger;

/**
 * The half of the crew personal equipment rule that covers what a crew carries in hand.
 * <p>
 * A crew that ejects or abandons arrives on the board as conventional infantry, and until now every one of them was
 * handed the same generic Auto-Rifle, or nothing. This lets a crew be issued a weapon of their own in the Configure
 * Pilots tab, or by MekHQ through the same crew attribute the armor kit travels on, and the ejected crew fires that
 * instead. Nothing here changes a crew that names no weapon: they get the rifle exactly as before.
 * <p>
 * The rule shares its switch with the armor kit, {@link OptionsConstants#RPG_COMBAT_SUITS}, because both are the
 * same idea: a crew member owns something, and keeps it when they leave their unit. Whether a MekWarrior may be
 * armed at all is still the older, unofficial {@link OptionsConstants#ADVANCED_ARMED_MEKWARRIORS}; this rule only
 * decides what the weapon is once they may carry one.
 */
public final class CrewSidearmRules {

    private static final MMLogger LOGGER = MMLogger.create(CrewSidearmRules.class);

    private CrewSidearmRules() {
    }

    /**
     * Whether this unit's crew can be issued a sidearm at all. The same crews that can wear a kit: those who might
     * end up outside their unit on foot.
     *
     * @param entity the unit whose crew is being configured, or {@code null}
     *
     * @return {@code true} if the crew may be issued a sidearm
     */
    public static boolean canCarrySidearm(@Nullable Entity entity) {
        return CrewArmorKitRules.canWearArmorKit(entity);
    }

    /**
     * Whether the optional rule is in force. It is the crew personal equipment rule, shared with the armor kit.
     *
     * @param game the game to ask, or {@code null}
     *
     * @return {@code true} if a named sidearm reaches the board in this game
     */
    public static boolean isRuleInPlay(@Nullable Game game) {
        return CrewArmorKitRules.isRuleInPlay(game);
    }

    /**
     * Whether one person could carry and fire this on their own.
     * <p>
     * MegaMek has no flag that says "pistol", so the line is drawn by what a weapon needs rather than by its
     * name: a crew of one, not a support weapon that takes a team to serve, and not a disposable one-shot. That
     * admits knives, pistols, rifles, shotguns and submachine guns and refuses mortars, machine guns and rocket
     * launchers. A campaign may narrow the list further; MegaMek does not.
     *
     * @param equipment the equipment to check, or {@code null}
     *
     * @return {@code true} if a crew member may be issued it as a sidearm
     */
    public static boolean isSidearmCandidate(@Nullable EquipmentType equipment) {
        if (!(equipment instanceof InfantryWeapon weapon)) {
            return false;
        }
        boolean needsAWeaponTeam = weapon.getCrew() > 1;
        boolean isSupportWeapon = weapon.hasFlag(WeaponType.F_INF_SUPPORT);
        boolean isDisposable = weapon.hasFlag(WeaponType.F_INF_DISPOSABLE);
        return !needsAWeaponTeam && !isSupportWeapon && !isDisposable;
    }

    /**
     * Every weapon MegaMek knows that one person could carry, which is the list a crew can be issued from.
     *
     * @return the weapons, in the order the equipment tables hold them
     */
    public static List<InfantryWeapon> availableSidearms() {
        List<InfantryWeapon> sidearms = new ArrayList<>();
        for (EquipmentType equipment : EquipmentType.allTypes()) {
            if (isSidearmCandidate(equipment)) {
                sidearms.add((InfantryWeapon) equipment);
            }
        }
        return sidearms;
    }

    /**
     * The sidearm this unit's crew would leave carrying, with the optional rule in play.
     *
     * @param entity the unit the crew is aboard, or {@code null}
     * @param game   the game whose options say whether the rule is in force, or {@code null}
     *
     * @return the weapon, or {@code null} if the crew carry none, the rule is off, or this crew cannot carry one
     */
    public static @Nullable InfantryWeapon crewSidearm(@Nullable Entity entity, @Nullable Game game) {
        boolean hasNoCrewToAskAbout = (entity == null) || (entity.getCrew() == null);
        if (hasNoCrewToAskAbout) {
            return null;
        }
        if (!canCarrySidearm(entity)) {
            LOGGER.debug("[CrewSidearm] {}: crew cannot carry a sidearm, this kind of unit never leaves on foot",
                  entity.getDisplayName());
            return null;
        }
        return crewSidearm(entity.getCrew(), entity.isClan(), game);
    }

    /**
     * The sidearm this crew would leave carrying, asked of the crew itself rather than the unit it is aboard. For
     * the crew of a Combat Vehicle Escape Pod, whose ride is already gone by the time they step out, this is the
     * only form that can be asked.
     *
     * @param crew       the crew, or {@code null}
     * @param isClanCrew whether Clan dates decide what has been invented
     * @param game       the game whose options say whether the rule is in force, or {@code null}
     *
     * @return the weapon, or {@code null} if the crew carry none or the rule is off
     */
    public static @Nullable InfantryWeapon crewSidearm(@Nullable Crew crew, boolean isClanCrew,
          @Nullable Game game) {
        if (crew == null) {
            return null;
        }
        if (!isRuleInPlay(game)) {
            LOGGER.debug("[CrewSidearm] {}: Crew Personal Equipment is off, so any named sidearm is ignored",
                  crew.getName());
            return null;
        }
        String sidearmName = crew.getAnySidearmName();
        if ((sidearmName == null) || sidearmName.isBlank()) {
            LOGGER.debug("[CrewSidearm] {}: no sidearm named, so the default applies", crew.getName());
            return null;
        }
        EquipmentType equipment = EquipmentType.get(sidearmName);
        if (!isSidearmCandidate(equipment)) {
            // A name a campaign or a unit list wrote that MegaMek cannot honour: unknown, or not a one-person
            // weapon. Say so at WARN, because the player asked for something and is about to get the rifle.
            LOGGER.warn("[CrewSidearm] {}: named sidearm '{}' is not a weapon one person can carry, ignoring it",
                  crew.getName(), sidearmName);
            return null;
        }
        InfantryWeapon sidearm = (InfantryWeapon) equipment;
        if (!isAvailableIn(sidearm, isClanCrew, game)) {
            // isRuleInPlay already refused a null game above, so the year can be read here.
            LOGGER.debug("[CrewSidearm] {}: {} is not available in {}, so the default applies",
                  crew.getName(), sidearm.getName(), game.getOptions().intOption(OptionsConstants.ALLOWED_YEAR));
            return null;
        }
        return sidearm;
    }

    /**
     * Whether this weapon has been invented yet, and not gone extinct, in the year being played. The dates are the
     * equipment's own, so this defers to the tech progression already recorded rather than repeating it.
     *
     * @param sidearm the weapon to check, or {@code null}
     * @param entity  the unit whose crew would carry it, which decides whether Clan dates apply, or {@code null}
     * @param game    the game whose year applies, or {@code null}
     *
     * @return {@code true} if a crew may have this weapon in this year
     */
    public static boolean isAvailableIn(@Nullable InfantryWeapon sidearm, @Nullable Entity entity,
          @Nullable Game game) {
        boolean isClanCrew = (entity != null) && entity.isClan();
        return isAvailableIn(sidearm, isClanCrew, game);
    }

    /**
     * @param sidearm    the weapon to check, or {@code null}
     * @param isClanCrew whether Clan dates apply
     * @param game       the game whose year applies, or {@code null}
     *
     * @return {@code true} if a crew may have this weapon in this year
     *
     * @see #isAvailableIn(InfantryWeapon, Entity, Game)
     */
    public static boolean isAvailableIn(@Nullable InfantryWeapon sidearm, boolean isClanCrew, @Nullable Game game) {
        if ((sidearm == null) || (game == null)) {
            return false;
        }
        int year = game.getOptions().intOption(OptionsConstants.ALLOWED_YEAR);
        return sidearm.isAvailableIn(year, isClanCrew, false);
    }
}
