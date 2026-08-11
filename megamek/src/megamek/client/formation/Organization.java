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
package megamek.client.formation;

import java.util.List;

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.common.annotations.Nullable;

/**
 * The organizational doctrine a player's force is assembled under: it sets the element size the
 * {@link FormationAssembler} aims for and the element word its formation names carry (Campaign Operations
 * p. 60+ - Inner Sphere lances of 4, Clan stars of 5, ComStar and Word of Blake Level IIs of 6).
 *
 * <p>{@link #AUTO} is the default: the assembler inspects the force and picks {@link #CLAN} when the
 * majority of units are Clan tech, {@link #INNER_SPHERE} otherwise. Detection proposes, the player
 * disposes - an explicit choice in the player settings always wins, and ComStar and Word of Blake are
 * only ever reached by choice.</p>
 */
public enum Organization {

    AUTO("Lance", 4),
    INNER_SPHERE("Lance", 4),
    CLAN("Star", 5),
    COMSTAR("Level II", 6),
    WORD_OF_BLAKE("Level II", 6);

    private final String elementWord;
    private final int elementSize;

    Organization(String elementWord, int elementSize) {
        this.elementWord = elementWord;
        this.elementSize = elementSize;
    }

    /** @return the word formation names are built around, e.g. "Lance", "Star" or "Level II" */
    public String getElementWord() {
        return elementWord;
    }

    /** @return the number of units a full element holds under this doctrine */
    public int getElementSize() {
        return elementSize;
    }

    /**
     * ComStar and Word of Blake name their elements "Level II Alpha" with no formation-type prefix;
     * the others lead with the type ("Battle Lance Alpha").
     *
     * @return true when formation names carry the qualified formation type as a prefix
     */
    public boolean usesTypePrefix() {
        return (this != COMSTAR) && (this != WORD_OF_BLAKE);
    }

    /** The generator faction keys for the two powers that organize into Level IIs. */
    private static final String COMSTAR_FACTION = "CS";
    private static final String WORD_OF_BLAKE_FACTION = "WOB";

    /**
     * Resolves {@link #AUTO} against the actual force: majority Clan tech means Clan stars, anything
     * else means Inner Sphere lances. An explicit organization returns itself unchanged.
     *
     * @param units the units about to be assembled
     *
     * @return the concrete organization to assemble under, never {@link #AUTO}
     */
    public Organization resolve(List<AssemblyUnit> units) {
        return resolve(units, null);
    }

    /**
     * Resolves {@link #AUTO} for a force whose faction is known. The faction is the better answer
     * where there is one: ComStar and Word of Blake organize into Level IIs and field Inner Sphere
     * equipment, so nothing about their units could ever reveal it, and a Clan faction fielding
     * salvage still fights in stars. Tech base is only the fallback for a player who never said.
     *
     * @param units      the units about to be assembled
     * @param factionKey the force's generator faction key, dotted for a sub-command; may be null
     *
     * @return the concrete organization to assemble under, never {@link #AUTO}
     */
    public Organization resolve(List<AssemblyUnit> units, @Nullable String factionKey) {
        if (this != AUTO) {
            return this;
        }
        if ((factionKey != null) && !factionKey.isBlank()) {
            // A sub-command organizes like its parent, so compare the parent key. Compare it whole:
            // CSA, CSJ, CSR and CSV are Clan Star Adder, Smoke Jaguar, Snow Raven and Star Viper, and
            // a prefix test would file four Clans under ComStar.
            String parentKey = factionKey.contains(".")
                  ? factionKey.substring(0, factionKey.indexOf('.'))
                  : factionKey;
            if (COMSTAR_FACTION.equals(parentKey)) {
                return COMSTAR;
            }
            if (WORD_OF_BLAKE_FACTION.equals(parentKey)) {
                return WORD_OF_BLAKE;
            }
            if (FactionRecord.CL_GENERAL_KEY.equals(parentKey)) {
                return CLAN;
            }
            // The generator data knows which factions are Clans; it is only consulted once it has
            // finished loading, since asking early answers null for every faction.
            if (RATGenerator.getInstance().isInitialized()) {
                FactionRecord factionRecord = RATGenerator.getInstance().getFaction(parentKey);
                if (factionRecord != null) {
                    return factionRecord.isClan() ? CLAN : INNER_SPHERE;
                }
            }
        }
        int clanCount = 0;
        for (AssemblyUnit unit : units) {
            if (unit.clan()) {
                clanCount++;
            }
        }
        return (clanCount * 2 > units.size()) ? CLAN : INNER_SPHERE;
    }
}
