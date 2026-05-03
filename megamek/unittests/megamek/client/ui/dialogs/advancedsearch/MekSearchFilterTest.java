package megamek.client.ui.dialogs.advancedsearch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import megamek.common.loaders.MekSummary;
import org.junit.jupiter.api.Test;

class MekSearchFilterTest {

    @Test
    void matchesSourceFilterMatchesSource() {
        MekSummary mek = new MekSummary();
        mek.setSource("Technical Readout: 3039");
        mek.setPublished("Record Sheets: 3050 Upgrade");

        assertTrue(MekSearchFilter.matchesSourceFilter(mek, "readout 3039"));
    }

    @Test
    void matchesSourceFilterMatchesPublished() {
        MekSummary mek = new MekSummary();
        mek.setSource("Technical Readout: 3039");
        mek.setPublished("Record Sheets: 3050 Upgrade");

        assertTrue(MekSearchFilter.matchesSourceFilter(mek, "3050 upgrade"));
    }

    @Test
    void matchesSourceFilterMatchesAnySourceInList() {
        MekSummary mek = new MekSummary();
        mek.setSource("Technical Readout: 3039");
        mek.setPublished("Record Sheets: 3050 Upgrade");

        assertTrue(MekSearchFilter.matchesSourceFilter(mek, "Interstellar Operations, readout 3039"));
        assertTrue(MekSearchFilter.matchesSourceFilter(mek, "Interstellar Operations, 3050 upgrade"));
    }

    @Test
    void matchesSourceFilterRejectsWhenNeitherFieldMatches() {
        MekSummary mek = new MekSummary();
        mek.setSource("Technical Readout: 3039");
        mek.setPublished("Record Sheets: 3050 Upgrade");

        assertFalse(MekSearchFilter.matchesSourceFilter(mek, "Interstellar Operations"));
    }

    @Test
    void matchesSourceFilterRejectsWhenNoSourceInListMatches() {
        MekSummary mek = new MekSummary();
        mek.setSource("Technical Readout: 3039");
        mek.setPublished("Record Sheets: 3050 Upgrade");

        assertFalse(MekSearchFilter.matchesSourceFilter(mek, "Interstellar Operations, Tactical Operations"));
    }
}
