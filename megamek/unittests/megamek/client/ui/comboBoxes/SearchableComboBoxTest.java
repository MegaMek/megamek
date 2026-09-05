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
package megamek.client.ui.comboBoxes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

/**
 * Covers the search behaviour behind {@link SearchableComboBox}: the model narrows to matching entries without
 * losing the selection, and the box only ever selects real entries no matter what text the editor holds.
 */
class SearchableComboBoxTest {

    /** A stand-in for a munition: the display text deliberately differs from {@code toString()}. */
    private record Munition(String code, String label) {
        @Override
        public String toString() {
            return code;
        }
    }

    private static final Munition STANDARD = new Munition("LRM-STD", "LRM 20 Standard");
    private static final Munition INFERNO = new Munition("LRM-INF", "LRM 20 Inferno");
    private static final Munition SWARM = new Munition("LRM-SWM", "LRM 20 Swarm");
    private static final Munition THUNDER_INFERNO = new Munition("LRM-THI", "LRM 20 Thunder-Inferno");
    private static final List<Munition> MUNITIONS = List.of(STANDARD, INFERNO, SWARM, THUNDER_INFERNO);

    private static FilteredComboBoxModel<Munition> newModel() {
        return new FilteredComboBoxModel<>(MUNITIONS, Munition::label);
    }

    private static SearchableComboBox<Munition> newComboBox() {
        return new SearchableComboBox<>("munitions", MUNITIONS, Munition::label);
    }

    private static String editorText(SearchableComboBox<Munition> comboBox) {
        return ((JTextField) comboBox.getEditor().getEditorComponent()).getText();
    }

    @Test
    void filterNarrowsToEntriesContainingTextIgnoringCase() {
        FilteredComboBoxModel<Munition> model = newModel();

        model.setFilter("INFerno");

        assertEquals(2, model.getSize());
        assertEquals(INFERNO, model.getElementAt(0));
        assertEquals(THUNDER_INFERNO, model.getElementAt(1));
        assertTrue(model.isFiltered());
    }

    @Test
    void clearingTheFilterShowsEveryEntryAgain() {
        FilteredComboBoxModel<Munition> model = newModel();
        model.setFilter("swarm");

        model.clearFilter();

        assertEquals(MUNITIONS.size(), model.getSize());
        assertFalse(model.isFiltered());
    }

    @Test
    void selectionSurvivesBeingFilteredOutOfView() {
        FilteredComboBoxModel<Munition> model = newModel();
        model.setSelectedItem(STANDARD);

        model.setFilter("inferno");

        assertEquals(STANDARD, model.getSelectedItem());
        assertEquals(Optional.of(INFERNO), model.getFirstVisibleItem());
    }

    @Test
    void modelIgnoresValuesThatAreNotEntries() {
        FilteredComboBoxModel<Munition> model = newModel();
        model.setSelectedItem(SWARM);

        model.setSelectedItem("LRM 20 Inferno");
        model.setSelectedItem(new Munition("LRM-XXX", "LRM 20 Swarm"));

        assertEquals(SWARM, model.getSelectedItem());
    }

    @Test
    void displayTextUsesTheDisplayFunctionNotToString() {
        FilteredComboBoxModel<Munition> model = newModel();

        assertEquals("LRM 20 Inferno", model.displayTextOf(INFERNO));
        assertEquals("typed text", model.displayTextOf("typed text"));
        assertEquals("", model.displayTextOf(null));
        assertEquals(Optional.of(SWARM), model.findByDisplayText("  lrm 20 swarm "));
        assertTrue(model.findByDisplayText("no such munition").isEmpty());
    }

    @Test
    void refreshingContentsPicksUpRenamedEntriesAndReappliesTheFilter() {
        Map<Munition, String> names = new HashMap<>();
        MUNITIONS.forEach(munition -> names.put(munition, munition.label()));
        FilteredComboBoxModel<Munition> model = new FilteredComboBoxModel<>(MUNITIONS, names::get);
        model.setFilter("inferno");
        assertEquals(2, model.getSize());

        names.put(SWARM, "LRM 20 Inferno Swarm");
        model.refreshContents();

        assertEquals("LRM 20 Inferno Swarm", model.displayTextOf(SWARM));
        assertEquals(3, model.getSize());
        assertEquals(Optional.of(SWARM), model.findByDisplayText("lrm 20 inferno swarm"));
    }

    @Test
    void comboBoxShowsTheNewTextOfARenamedSelection() {
        Map<Munition, String> names = new HashMap<>();
        MUNITIONS.forEach(munition -> names.put(munition, munition.label()));
        SearchableComboBox<Munition> comboBox = new SearchableComboBox<>("munitions", MUNITIONS, names::get);
        comboBox.setSelectedItem(STANDARD);

        names.put(STANDARD, "LRM 20 Standard (half load)");
        comboBox.refreshDisplayTexts();

        assertEquals(STANDARD, comboBox.getSelectedItem());
        assertEquals("LRM 20 Standard (half load)", editorText(comboBox));
    }

    @Test
    void comboBoxResolvesExactEditorTextToTheEntry() {
        SearchableComboBox<Munition> comboBox = newComboBox();
        comboBox.setSelectedItem(STANDARD);

        comboBox.setSelectedItem("lrm 20 inferno");

        assertEquals(INFERNO, comboBox.getSelectedItem());
        assertEquals("LRM 20 Inferno", editorText(comboBox));
    }

    @Test
    void comboBoxKeepsSelectionWhenEditorTextNamesNoEntry() {
        SearchableComboBox<Munition> comboBox = newComboBox();
        comboBox.setSelectedItem(SWARM);

        comboBox.setSelectedItem("inf");

        assertEquals(SWARM, comboBox.getSelectedItem());
    }

    @Test
    void comboBoxOffersEveryEntryAndShowsDisplayTextForTheSelection() {
        SearchableComboBox<Munition> comboBox = newComboBox();

        assertEquals(MUNITIONS.size(), comboBox.getItemCount());
        assertNull(comboBox.getSelectedItem());

        comboBox.setSelectedItem(THUNDER_INFERNO);

        assertEquals("LRM 20 Thunder-Inferno", editorText(comboBox));
        assertEquals(3, comboBox.getSelectedIndex());
    }
}
