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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * A requested distribution of Campaign Operations formation types across the formations of a generated force - more
 * Battle lances, fewer Recon.
 *
 * <p>Counted in lances rather than shares: a player asking for four Recon lances is asking something the force can
 * be checked against, where "40%" needed a denominator nobody agreed on. Formations the player has not spoken for
 * are absent from the map entirely and keep whatever the ruleset's own weighted pick gave them, which is what keeps
 * an untouched editor indistinguishable from no mix at all.</p>
 *
 * <p>A formation mapped to zero is a request rather than an absence: none of this formation, please. That is why
 * "not asked for" has to be absence from the map and cannot be represented as zero.</p>
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
            // Zero is kept, because it is a request: none of this formation. A formation the player has left to the
            // rules is not in the map at all, which is what keeps an untouched editor indistinguishable from no mix.
            if (requested >= 0) {
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

    /**
     * This request trimmed to fit a force of the given size.
     *
     * <p>A request that asks for more lances than the force has cannot be met as written. Rather than letting
     * whichever formation is dealt with first take everything, every entry is scaled back in proportion, so asking
     * for 30 Recon and 10 Fire from 20 lances yields 15 and 5 - the balance that was expressed, at the size that
     * fits. The largest-remainder method shares out the lances lost to flooring, so the trimmed counts still add up
     * to the force rather than surrendering one to a rounding artefact.</p>
     *
     * <p>Public because the editor shows the player this same result before they generate; it must be the very
     * arithmetic the allocator will use, not a second implementation that agrees with it today.</p>
     *
     * @param adjustableLances how many lances the force has that can be reassigned
     *
     * @return the trimmed request, dropping any entry that scales down to nothing; {@code this} when it already fits
     */
    public FormationMix scaledTo(int adjustableLances) {
        int totalRequested = totalLances();
        if ((totalRequested <= adjustableLances) || (adjustableLances <= 0)) {
            return this;
        }
        Map<String, Integer> trimmed = new TreeMap<>();
        Map<String, Double> remainders = new TreeMap<>();
        double scale = (double) adjustableLances / totalRequested;
        int allocated = 0;
        for (String formationName : requestedFormations()) {
            // A request for none of a formation costs nothing and survives scaling untouched; there is nothing to
            // trim, and dropping it would quietly turn "none of these" back into "as the rules like".
            if (lancesFor(formationName) == 0) {
                trimmed.put(formationName, 0);
                continue;
            }
            double exact = lancesFor(formationName) * scale;
            int whole = (int) Math.floor(exact);
            trimmed.put(formationName, whole);
            remainders.put(formationName, exact - whole);
            allocated += whole;
        }
        List<String> byRemainder = new ArrayList<>(remainders.keySet());
        byRemainder.sort(Comparator.comparingDouble((String formationName) -> remainders.get(formationName))
              .reversed());
        for (String formationName : byRemainder) {
            if (allocated >= adjustableLances) {
                break;
            }
            trimmed.merge(formationName, 1, Integer::sum);
            allocated++;
        }
        // Only the ones that scaled away are dropped; an explicit request for none stays.
        trimmed.entrySet().removeIf(entry -> (entry.getValue() == 0) && (lancesFor(entry.getKey()) > 0));
        return new FormationMix(trimmed, allowUnofferedFormations);
    }
}
