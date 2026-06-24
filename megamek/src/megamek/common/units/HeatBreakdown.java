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

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import megamek.common.Messages;

/**
 * Itemized record of how a unit's heat was built up and dissipated during a single turn, used to give the Heat Phase
 * report (report 5035) a hover/click breakdown of its "gains N heat" and "sinks N heat" values.
 * <p>
 * Both sides are keyed by a human-readable source label in the order heat was first applied; buildup sources also count
 * how many times they fired (so several of the same weapon read "PPC x2: +20 (10 each)" rather than a single misleading
 * "+20"). This object lives on {@link Entity} and is rebuilt each turn.
 *
 * @author The MegaMek Team
 */
public class HeatBreakdown implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final LinkedHashMap<String, HeatContribution> buildup = new LinkedHashMap<>();
    private final LinkedHashMap<String, Integer> dissipation = new LinkedHashMap<>();

    /**
     * One labelled heat-buildup contribution: how many times the source applied heat this turn (e.g. the number of
     * weapons of that type that fired) and the signed total it added.
     *
     * @param count     the number of times this source applied heat
     * @param totalHeat the signed total heat this source added
     */
    public record HeatContribution(int count, int totalHeat) implements Serializable {
        /**
         * @param heat additional heat from one more application of this source
         *
         * @return a new contribution with the count incremented and the heat added
         */
        public HeatContribution plus(int heat) {
            return new HeatContribution(count + 1, totalHeat + heat);
        }
    }

    /**
     * Records a contribution to this turn's heat buildup. Contributions sharing a label are summed and the source count
     * is incremented; zero heat or a missing label is ignored.
     *
     * @param heat   the signed heat added (negative for cooling/reductions)
     * @param reason a short human-readable source label, e.g. "Movement (Running)" or "PPC"
     */
    public void addBuildup(int heat, String reason) {
        if ((heat != 0) && (reason != null) && !reason.isBlank()) {
            buildup.merge(reason, new HeatContribution(1, heat), (existing, added) -> existing.plus(heat));
        }
    }

    /**
     * Records a source of this turn's heat dissipation. Contributions sharing a label are summed.
     *
     * @param heat   the heat this source dissipates (positive)
     * @param reason a short human-readable source label, e.g. "Heat sinks" or "Submerged"
     */
    public void addDissipation(int heat, String reason) {
        if ((heat != 0) && (reason != null) && !reason.isBlank()) {
            dissipation.merge(reason, heat, Integer::sum);
        }
    }

    /**
     * @return an unmodifiable, insertion-ordered view of this turn's heat-buildup contributions
     *       (source label -> count and signed total)
     */
    public Map<String, HeatContribution> buildup() {
        return Collections.unmodifiableMap(buildup);
    }

    /**
     * @return an unmodifiable, insertion-ordered view of this turn's heat-dissipation contributions
     *       (source label -> heat removed)
     */
    public Map<String, Integer> dissipation() {
        return Collections.unmodifiableMap(dissipation);
    }

    /** Clears both breakdowns so the next turn starts fresh. */
    public void clear() {
        buildup.clear();
        dissipation.clear();
    }

    /**
     * Formats the buildup breakdown into a single-line tooltip for the "gains N heat" value (e.g. "Movement (Running):
     * +2, PPC x2: +20 (10 each), Cooling (water/coolant/etc.): -9"). Any heat not itemized here is reconciled into a
     * trailing "Other" entry so the listed contributions still sum to the reported total - skipped when the net is
     * negative, since the reported buildup is then clamped to 0.
     *
     * @param reportedTotal the heat-buildup total shown on the report (used to reconcile un-itemized heat)
     *
     * @return the tooltip text, or an empty string if nothing was itemized this turn
     */
    public String buildupTooltip(int reportedTotal) {
        if (buildup.isEmpty()) {
            return "";
        }
        StringBuilder tooltip = new StringBuilder();
        int tracked = 0;
        for (Map.Entry<String, HeatContribution> contribution : buildup.entrySet()) {
            int heat = contribution.getValue().totalHeat();
            int count = contribution.getValue().count();
            tracked += heat;
            if (heat == 0) {
                continue;
            }
            if (tooltip.length() > 0) {
                tooltip.append(", ");
            }
            tooltip.append(contribution.getKey());
            if (count > 1) {
                tooltip.append(" x").append(count);
            }
            tooltip.append(": ").append(heat > 0 ? "+" : "").append(heat);
            if ((count > 1) && (heat % count == 0)) {
                tooltip.append(" (").append(Messages.getString("HeatBreakdown.each", heat / count)).append(")");
            }
        }
        int remainder = reportedTotal - tracked;
        if ((remainder != 0) && (tracked >= 0) && (reportedTotal >= 0)) {
            if (tooltip.length() > 0) {
                tooltip.append(", ");
            }
            tooltip.append(Messages.getString("HeatBreakdown.other")).append(": ").append(remainder > 0 ? "+" : "").append(remainder);
        }
        return tooltip.toString();
    }

    /**
     * Formats the dissipation breakdown into a single-line tooltip for the "sinks N heat" value (e.g. "Heat sinks: 10,
     * Submerged: 2, Radical heat sink: 3"). The figures are the unit's dissipation capacity by source; a cool unit may
     * not use all of it.
     *
     * @return the tooltip text, or an empty string if nothing recorded dissipation this turn
     */
    public String dissipationTooltip() {
        if (dissipation.isEmpty()) {
            return "";
        }
        StringBuilder tooltip = new StringBuilder();
        for (Map.Entry<String, Integer> contribution : dissipation.entrySet()) {
            if (contribution.getValue() == 0) {
                continue;
            }
            if (tooltip.length() > 0) {
                tooltip.append(", ");
            }
            tooltip.append(contribution.getKey()).append(": ").append(contribution.getValue());
        }
        return tooltip.toString();
    }
}
