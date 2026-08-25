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
package megamek.common.universe;

import java.util.Map;

import megamek.logging.MMLogger;

/**
 * Flattens the {@link Faction2#getSubunits() subunits} declared inside a faction file into standalone factions.
 *
 * <p>Commands are frequently a parent formation with several subordinate regiments - the St. Ives Lancers and their
 * 1st through 7th regiments, or the Lyran Guards and their forty-five. Giving every regiment its own file makes the
 * relationship invisible in the data and multiplies the file count, so a parent may instead declare them inline:</p>
 *
 * <pre>
 * key: CC.SIL
 * name: St. Ives Lancers
 * fallBackFactions:
 *   - CC.SIAC
 * subunits:
 *   1st:
 *     name: 1st St. Ives Lancers
 *     yearsActive:
 *       - start: 2300
 * </pre>
 *
 * <p>MegaMek's runtime model is a flat map of factions, because a regiment has to be selectable and generatable in
 * its own right. This class therefore registers each subunit under the composed key {@code CC.SIL.1st}, so the
 * nesting is purely a convenience in the file and every consumer continues to see one flat list.</p>
 *
 * <p>A subunit inherits from its parent only what it cannot otherwise obtain:</p>
 * <ul>
 *     <li>{@code fallBackFactions} defaults to the parent, which is what makes rating levels, formation sizes and the
 *     rank system resolve up the chain - those already recurse through the fallback factions.</li>
 *     <li>{@code tags} and {@code nameGenerator} are copied down when the subunit declares none, because
 *     {@link Faction2#isClan()} and {@link Faction2#getNameGenerator()} read their field directly with no fallback.
 *     Without this a Clan regiment would be treated as Inner Sphere and get a lance of four rather than a point of
 *     five.</li>
 * </ul>
 *
 * <p>Anything the subunit declares for itself always wins over the inherited value.</p>
 */
public class SubunitRegistrar {

    private static final MMLogger LOGGER = MMLogger.create(SubunitRegistrar.class);

    private final Map<String, Faction2> factions;

    /**
     * @param factions The flat faction map to register subunits into; typically the map owned by {@link Factions2}
     */
    public SubunitRegistrar(Map<String, Faction2> factions) {
        this.factions = factions;
    }

    /**
     * Registers every subunit of the given faction, and their subunits in turn, into the faction map.
     *
     * <p>Nesting may go as deep as the data requires: a brigade may declare regiments which themselves declare
     * battalions. Each level composes its key from the level above it.</p>
     *
     * @param parent The faction whose subunits should be registered; its own entry must already be in the map
     */
    public void registerSubunits(Faction2 parent) {
        for (Map.Entry<String, Faction2> subunitEntry : parent.getSubunits().entrySet()) {
            Faction2 subunit = subunitEntry.getValue();
            if (subunit == null) {
                LOGGER.warn("[Subunit] {} declares an empty subunit under '{}' - ignoring.",
                      parent.getKey(), subunitEntry.getKey());
                continue;
            }

            String subunitKey = composeSubunitKey(parent, subunitEntry.getKey(), subunit);
            subunit.setKey(subunitKey);
            subunit.setParentCommand(parent.getKey());
            inheritFromParent(parent, subunit);

            // Whether the key may be taken over is decided from the occupant itself: a faction
            // with a file of its own is never displaced, while an earlier subunit registration
            // is, which is how a user directory overrides shipped data. Asking the occupant
            // rather than tracking keys separately keeps the answer correct no matter what order
            // the files happened to load in.
            Faction2 existingFaction = factions.get(subunitKey);
            if ((existingFaction != null) && !existingFaction.isSubunit()) {
                LOGGER.warn("[Subunit] Subunit key {} of {} collides with a faction that has its own file; " +
                            "keeping that faction.", subunitKey, parent.getKey());
                continue;
            }
            factions.put(subunitKey, subunit);
            LOGGER.debug("[Subunit] {} registered as a subunit of {}", subunitKey, parent.getKey());

            registerSubunits(subunit);
        }
    }

    /**
     * Builds the full key for a subunit.
     *
     * <p>A subunit normally takes its key from its position in the file, so the {@code 1st} entry of {@code CC.SIL}
     * becomes {@code CC.SIL.1st}. A subunit that spells out its own key keeps it, which lets imported data preserve
     * identifiers that do not follow the parent's naming.</p>
     *
     * @param parent           The declaring faction
     * @param subunitIdentifier The subunit's identifier within its parent's file
     * @param subunit          The subunit itself
     *
     * @return The full key to register the subunit under
     */
    private String composeSubunitKey(Faction2 parent, String subunitIdentifier, Faction2 subunit) {
        if ((subunit.getKey() != null) && !subunit.getKey().isBlank()) {
            return subunit.getKey();
        }
        return parent.getKey() + "." + subunitIdentifier;
    }

    /**
     * Copies down the values a subunit cannot resolve for itself. Values the subunit declares are left untouched.
     *
     * @param parent  The declaring faction
     * @param subunit The subunit to complete
     */
    private void inheritFromParent(Faction2 parent, Faction2 subunit) {
        if (subunit.getFallBackFactions().isEmpty()) {
            subunit.getFallBackFactions().add(parent.getKey());
        }
        if (subunit.getTags().isEmpty()) {
            subunit.getTags().addAll(parent.getTags());
        }
        if ((subunit.getNameGenerator() == null) || subunit.getNameGenerator().isBlank()) {
            subunit.setNameGenerator(parent.getNameGenerator());
        }
    }
}
