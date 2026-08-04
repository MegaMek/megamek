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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A requested distribution of Campaign Operations formation types across the formations of a generated force - more
 * Battle lances, fewer Recon.
 *
 * <p>Counted in lances rather than shares: a player asking for four Recon lances is asking something the force can
 * be checked against, where "40%" needed a denominator nobody agreed on. The counts are a floor rather than a
 * partition - they need not use up the force, and every lance not claimed keeps whatever the ruleset's own weighted
 * pick gave it. A mix in which every entry is zero is therefore indistinguishable from no mix at all, which is what
 * keeps an untouched control generating exactly as before.</p>
 *
 * <p>Formations are named rather than enumerated because that is how the rulesets and the {@link FormationType}
 * registry already identify them; there is no enum to key on and inventing one would duplicate the registry.</p>
 *
 * <p>A mix is expressed against one force. Which formations that force offers depends on its faction, era, unit type
 * and echelon, so {@link #restrictedTo(FormationMixPreview)} and {@link #unavailableIn(FormationMixPreview)} exist to
 * reconcile a mix with what a particular force can actually supply.</p>
 *
 * <p>This is a transient build-time value; it is never serialized into a saved game.</p>
 *
 * @param lances                   requested number of lances per formation type name; only positive entries are
 *                                 retained
 * @param allowUnofferedFormations {@code true} to place a formation on any lance that can hold it, rather than only
 *                                 on lances the ruleset offered it to
 */
public record FormationMix(Map<String, Integer> lances, boolean allowUnofferedFormations) {

    /** A mix that requests nothing, leaving every formation to the ruleset's own weighted pick. */
    public static final FormationMix EMPTY = new FormationMix(Map.of(), false);

    /**
     * A mix held to what the ruleset offers, which is the default.
     *
     * @param lances requested number of lances per formation type name
     */
    public FormationMix(Map<String, Integer> lances) {
        this(lances, false);
    }

    /**
     * Canonical constructor. Trims names, drops non-positive entries so an untouched control is indistinguishable
     * from an empty mix, and takes an immutable copy ordered by name.
     */
    public FormationMix {
        Map<String, Integer> retained = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : lances.entrySet()) {
            if ((entry.getKey() == null) || entry.getKey().isBlank()) {
                continue;
            }
            int requested = (entry.getValue() == null) ? 0 : entry.getValue();
            if (requested > 0) {
                retained.put(entry.getKey().trim(), requested);
            }
        }
        lances = Collections.unmodifiableMap(retained);
    }

    /**
     * @return {@code true} when nothing is requested, so the allocator should leave the force alone entirely
     */
    public boolean isEmpty() {
        return lances.isEmpty();
    }

    /**
     * How many of a force's adjustable lances this mix asks for in total. Short of the force leaves the remainder to
     * the ruleset; beyond it over-subscribes, and the allocator scales the request back proportionally.
     *
     * @return the sum of all requested lance counts
     */
    public int totalLances() {
        return lances.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * @param formationName the formation type to look up
     *
     * @return the number of lances asked for, or {@code 0} if this formation was not asked for
     */
    public int lancesFor(String formationName) {
        return (formationName == null) ? 0 : lances.getOrDefault(formationName.trim(), 0);
    }

    /**
     * @return the formation types this mix asks for, in name order
     */
    public Set<String> requestedFormations() {
        return lances.keySet();
    }

    /**
     * The requested formations the given force never offers, and so cannot supply however much is asked for.
     *
     * <p>This is what a pre-flight warning is built from: a player who asks for a formation this faction and era do
     * not field should be told before generating, not left to wonder why it never appeared.</p>
     *
     * @param preview what the force actually offers
     *
     * @return the requested formations that force cannot supply, in name order
     */
    public Set<String> unavailableIn(FormationMixPreview preview) {
        Set<String> unavailable = new LinkedHashSet<>();
        for (String formationName : lances.keySet()) {
            if (!preview.offeredFormations().contains(formationName)) {
                unavailable.add(formationName);
            }
        }
        return Collections.unmodifiableSet(unavailable);
    }

    /**
     * Narrows this mix to the formations the given force actually offers.
     *
     * <p>Dropping an unsupportable request rather than carrying it means the allocator's arithmetic is done against
     * what can really be placed, so the remaining formations get the share they were promised instead of quietly
     * losing part of it to one that was never achievable.</p>
     *
     * @param preview what the force actually offers
     *
     * @return a mix holding only the supportable entries; {@code this} when every entry is supportable
     */
    public FormationMix restrictedTo(FormationMixPreview preview) {
        Map<String, Integer> retained = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : lances.entrySet()) {
            if (preview.offeredFormations().contains(entry.getKey())) {
                retained.put(entry.getKey(), entry.getValue());
            }
        }
        return (retained.size() == lances.size())
              ? this
              : new FormationMix(retained, allowUnofferedFormations);
    }
}
