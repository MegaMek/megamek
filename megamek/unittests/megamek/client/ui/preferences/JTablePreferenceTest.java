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
package megamek.client.ui.preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JTable;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.Test;

class JTablePreferenceTest {
    private static final String TABLE_NAME = "personnelTable";

    private static JTable createTable() {
        JTable table = new JTable(new DefaultTableModel(3, 5));
        table.setName(TABLE_NAME);
        table.setAutoCreateRowSorter(true);
        return table;
    }

    private static PreferencesNode createNode(String storedValue) throws Exception {
        PreferencesNode node = new PreferencesNode(JTablePreferenceTest.class);
        Map<String, String> stored = new HashMap<>();
        if (storedValue != null) {
            stored.put(TABLE_NAME, storedValue);
        }
        node.initialize(stored);
        return node;
    }

    @Test
    void everySortKeyIsSaved() throws Exception {
        // sorting by rank and then by profession sorts by profession first, and by rank within it
        JTable table = createTable();
        PreferencesNode node = createNode(null);
        node.manage(new JTablePreference(table));

        table.getRowSorter().toggleSortOrder(2);
        table.getRowSorter().toggleSortOrder(4);
        table.getRowSorter().toggleSortOrder(4);

        assertEquals("4|DESCENDING|2|ASCENDING", node.getFinalValues().get(TABLE_NAME));
    }

    @Test
    void everySortKeyIsRestored() throws Exception {
        JTable table = createTable();
        PreferencesNode node = createNode("4|DESCENDING|2|ASCENDING|1|ASCENDING");
        node.manage(new JTablePreference(table));

        assertEquals(List.of(new SortKey(4, SortOrder.DESCENDING),
                    new SortKey(2, SortOrder.ASCENDING),
                    new SortKey(1, SortOrder.ASCENDING)),
              table.getRowSorter().getSortKeys());
    }

    @Test
    void theSingleKeyFormatOfOlderVersionsIsRestored() throws Exception {
        JTable table = createTable();
        PreferencesNode node = createNode("3|DESCENDING");
        node.manage(new JTablePreference(table));

        assertEquals(List.of(new SortKey(3, SortOrder.DESCENDING)), table.getRowSorter().getSortKeys());
        assertEquals("3|DESCENDING", node.getFinalValues().get(TABLE_NAME));
    }

    @Test
    void brokenOrMissingColumnsAreSkipped() {
        List<SortKey> keys = JTablePreference.parseSortKeys(
              "9|ASCENDING|x|ASCENDING|2|SIDEWAYS|1|DESCENDING|1|ASCENDING|0|UNSORTED|3", 5);

        assertEquals(List.of(new SortKey(1, SortOrder.DESCENDING)), keys);
    }

    @Test
    void anUnsortedTableKeepsTheFirstColumnAscending() throws Exception {
        JTable table = createTable();
        PreferencesNode node = createNode(null);
        node.manage(new JTablePreference(table));

        assertEquals("0|ASCENDING", node.getFinalValues().get(TABLE_NAME));
    }
}
