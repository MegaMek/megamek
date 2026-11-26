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
package megamek.client.ui.panels.phaseDisplay.lobby.sorters;

import java.util.ArrayList;
import java.util.List;

import megamek.client.Client;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.panels.phaseDisplay.lobby.MekTableModel;
import megamek.common.internationalization.I18n;
import megamek.common.options.IGameOptions;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;

/**
 * A Lobby Mek Table sorter for the Formation column. It orders whole formations, not single units: by their place in
 * the force tree, or by their total BV, total tonnage or slowest unit. {@link FormationGrouping} does the ordering and
 * keeps the units inside each formation in force tree order; this sorter only names the metric and the direction.
 */
public class FormationSorter extends MekTableSorter {

    /** What formations are ordered by. */
    public enum Metric {
        /** The force tree order, as Force View lists them. */
        TREE_ORDER("FormationSorter.TreeOrder"),
        /** The sum of the BV of the formation's units, at any depth. */
        TOTAL_BV("FormationSorter.TotalBV"),
        /** The sum of the tonnage of the formation's units, at any depth. */
        TOTAL_TONNAGE("FormationSorter.TotalTonnage"),
        /** The lowest walking MP among the formation's units: a formation moves at its slowest unit's speed. */
        SLOWEST_UNIT("FormationSorter.SlowestUnit");

        private final String displayNameKey;

        Metric(String displayNameKey) {
            this.displayNameKey = displayNameKey;
        }
    }

    private final Client client;
    private final Metric metric;

    /**
     * @param client  the client, for listing the local player's formations first
     * @param metric  what the formations are ordered by
     * @param sorting {@link Sorting#ASCENDING} for the lowest value first
     */
    public FormationSorter(Client client, Metric metric, Sorting sorting) {
        super(I18n.getTextAt(RESOURCE_BUNDLE, metric.displayNameKey), MekTableModel.COL_FORMATION, sorting);
        this.client = client;
        this.metric = metric;
    }

    /**
     * Returns the Formation column's sorters in the order a click on the column header cycles through them: force
     * tree order, then each metric highest first and lowest first.
     *
     * @param clientGUI the client GUI
     *
     * @return the sorters
     */
    public static List<MekTableSorter> allFor(ClientGUI clientGUI) {
        Client client = clientGUI.getClient();
        List<MekTableSorter> result = new ArrayList<>();
        result.add(new FormationSorter(client, Metric.TREE_ORDER, Sorting.ASCENDING));
        for (Metric metric : List.of(Metric.TOTAL_BV, Metric.TOTAL_TONNAGE, Metric.SLOWEST_UNIT)) {
            result.add(new FormationSorter(client, metric, Sorting.DESCENDING));
            result.add(new FormationSorter(client, metric, Sorting.ASCENDING));
        }
        return result;
    }

    /** Returns one sentence saying what this order puts first, for the Formation column header's tooltip. */
    public String description() {
        String key = metric.displayNameKey + ".tip";
        if (metric != Metric.TREE_ORDER) {
            key += (getSortingDirection() == Sorting.ASCENDING) ? ".ascending" : ".descending";
        }
        return I18n.getTextAt(RESOURCE_BUNDLE, key);
    }

    /** Returns {@code true} when this sort may be used in the present game (blind drop hides BV, tonnage and speed). */
    public boolean isAllowedNow() {
        return isAllowed(client.getGame().getOptions());
    }

    /** Returns what this sorter orders formations by. */
    public Metric getMetric() {
        return metric;
    }

    /**
     * Returns 1 when the lowest value comes first, -1 when the highest does.
     */
    int direction() {
        return getSortingDirectionInt();
    }

    /**
     * Compares two units by player only, the local player first. Formations are ordered by {@link FormationGrouping}.
     */
    @Override
    public int compare(final Entity first, final Entity second) {
        return getPlayerTeamIndexPosition(client, first, second).orElse(0);
    }

    /** The BV, tonnage and speed orders would give away hidden units' values in blind drop. */
    @Override
    public boolean isAllowed(IGameOptions opts) {
        return (metric == Metric.TREE_ORDER) || !opts.booleanOption(OptionsConstants.BASE_BLIND_DROP);
    }
}
