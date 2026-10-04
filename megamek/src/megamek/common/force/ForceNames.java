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
package megamek.common.force;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;

/**
 * Force names that sound right on the radio: the lobby offers them when a force is created or renamed, and a bot's
 * force left with a blank or generic name is called by one of the callsign names instead ("Charlie Lance, proceeding
 * to Nav Point Gamma").
 */
public final class ForceNames {

    /** The naming a side uses, from its owner's name and units. */
    public enum Style {
        /** Lances and companies. */
        INNER_SPHERE,
        /** Stars, Binaries and Trinaries. */
        CLAN,
        /** ComStar and the Word of Blake: Level IIs and Level IIIs. */
        COMSTAR
    }

    private static final List<String> INNER_SPHERE_NAMES = List.of("Command Lance", "Battle Lance", "Fire Lance",
          "Striker Lance", "Recon Lance", "Assault Lance", "Pursuit Lance", "Support Lance", "Alpha Lance",
          "Bravo Lance", "Charlie Lance", "Delta Lance", "Alpha Company", "Bravo Company", "Charlie Company");

    private static final List<String> CLAN_NAMES = List.of("Command Star", "Battle Star", "Striker Star", "Alpha Star",
          "Beta Star", "Gamma Star", "Delta Star", "Epsilon Star", "Command Trinary", "Alpha Trinary", "Beta Trinary",
          "Command Binary");

    private static final List<String> COMSTAR_NAMES = List.of("Command Level II", "Level II Alpha", "Level II Beta",
          "Level II Gamma", "Level II Delta", "Level II Epsilon", "Level III Alpha", "Level III Beta");

    // the callsign names given, in order, to a side's forces that have no usable name of their own
    private static final List<String> PHONETIC = List.of("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot",
          "Golf", "Hotel", "India", "Juliet", "Kilo", "Lima", "Mike", "November", "Oscar", "Papa", "Quebec",
          "Romeo", "Sierra", "Tango", "Uniform", "Victor", "Whiskey", "X-Ray", "Yankee", "Zulu");

    private static final List<String> GREEK = List.of("Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta",
          "Theta", "Iota", "Kappa", "Lambda", "Mu", "Nu", "Xi", "Omicron", "Pi", "Rho", "Sigma", "Tau", "Upsilon",
          "Phi", "Chi", "Psi", "Omega");

    // names that say nothing about the force, so the radio calls it by a callsign name instead
    private static final Set<String> GENERIC_NAMES = Set.of("force", "new force", "lance", "star", "company",
          "unnamed", "level ii");

    private static final List<String> COMSTAR_OWNER_NAMES = List.of("comstar", "word of blake", "com guard", "wob");

    private ForceNames() {}

    /**
     * @param style the side's naming
     *
     * @return the names the lobby offers for a new or renamed force, the side's own first
     */
    public static List<String> suggestions(Style style) {
        return switch (style) {
            case CLAN -> CLAN_NAMES;
            case COMSTAR -> COMSTAR_NAMES;
            case INNER_SPHERE -> INNER_SPHERE_NAMES;
        };
    }

    /**
     * @param ownerName the name of the player the force belongs to
     * @param units     that player's units
     *
     * @return ComStar naming for a player named for ComStar or the Word of Blake, Clan naming for a player named for
     *       a Clan or with mostly Clan units, else Inner Sphere naming
     */
    public static Style styleFor(@Nullable String ownerName, Collection<Entity> units) {
        String name = (ownerName == null) ? "" : ownerName.toLowerCase(Locale.ROOT);
        for (String comstarName : COMSTAR_OWNER_NAMES) {
            if (name.contains(comstarName)) {
                return Style.COMSTAR;
            }
        }
        if (name.startsWith("clan ") || name.contains(" clan ")) {
            return Style.CLAN;
        }
        int clanUnits = 0;
        for (Entity unit : units) {
            if (unit.isClan()) {
                clanUnits++;
            }
        }
        return (!units.isEmpty() && ((clanUnits * 2) > units.size())) ? Style.CLAN : Style.INNER_SPHERE;
    }

    /**
     * @param forceName the force's name
     * @param ownerName the name of the player the force belongs to
     *
     * @return {@code true} if the name says nothing a radio call could use: blank, the owner's own name, or a word
     *       like "Force" or "Lance" alone
     */
    public static boolean isGeneric(@Nullable String forceName, @Nullable String ownerName) {
        if ((forceName == null) || forceName.isBlank()) {
            return true;
        }
        String name = forceName.trim().toLowerCase(Locale.ROOT);
        return GENERIC_NAMES.contains(name) || ((ownerName != null) && name.equalsIgnoreCase(ownerName.trim()))
              || name.matches("force \\d+");
    }

    /**
     * @param index the force's place among its side's forces with no usable name, from 0
     * @param style the side's naming
     *
     * @return the callsign name the radio calls it by: {@code Charlie Lance}, {@code Gamma Star} or
     *       {@code Level II Gamma} for the third
     */
    public static String callsignName(int index, Style style) {
        int safeIndex = Math.max(0, index);
        return switch (style) {
            case CLAN -> GREEK.get(safeIndex % GREEK.size()) + " Star";
            case COMSTAR -> "Level II " + GREEK.get(safeIndex % GREEK.size());
            case INNER_SPHERE -> PHONETIC.get(safeIndex % PHONETIC.size()) + " Lance";
        };
    }
}
