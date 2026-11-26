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

package megamek.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class IssueReportUrlTest {

    private static final String MEGAMEK_ISSUES = "https://github.com/MegaMek/megamek/issues/new/choose";

    @Test
    void prefillsTheThreeRequiredEnvironmentFields() {
        String url = IssueReportUrl.forIssueForm(MEGAMEK_ISSUES);

        assertTrue(url.contains("custom-megamek-version="), url);
        assertTrue(url.contains("operating-system="), url);
        assertTrue(url.contains("java-version="), url);
    }

    @Test
    void prefillsTheHardwareAndBuildFields() {
        String url = IssueReportUrl.forIssueForm(MEGAMEK_ISSUES);

        assertTrue(url.contains("&cpu="), url);
        assertTrue(url.contains("&graphics-cards="), url);
        assertTrue(url.contains("&system-memory="), url);
        assertTrue(url.contains("&java-max-memory="), url);
        assertTrue(url.contains("&locale="), url);
        assertTrue(url.contains("&build="), url);
    }

    /** The graphics field is a textarea with one card per line, so its newlines must survive as {@code %0A}. */
    @Test
    void neverPutsARawNewlineInTheUrl() {
        String url = IssueReportUrl.forIssueForm(MEGAMEK_ISSUES);

        assertFalse(url.contains("\n"), "a raw newline would break the link: " + url);
    }

    @Test
    void javaMaxMemoryIsShownInGigabytes() {
        assertEquals("4 GB", IssueReportUrl.describeJavaMaxMemory(4L * 1024L * 1024L * 1024L));
    }

    /** A 512 MB limit would round to "1 GB" or "0 GB", hiding the most likely cause of an out-of-memory report. */
    @Test
    void javaMaxMemoryUnderOneGigabyteIsShownInMegabytes() {
        assertEquals("512 MB", IssueReportUrl.describeJavaMaxMemory(512L * 1024L * 1024L));
    }

    @Test
    void buildLineShortensRevisionsAndKeepsPlaceholders() {
        assertEquals("2026-11-20 | MM eba3a49e10, MML Unknown, MHQ Unknown",
              IssueReportUrl.describeBuild(LocalDateTime.of(2026, 11, 20, 14, 5),
                    "eba3a49e1006e51b16db12446caf5e43f57a18a7", "Unknown", "Unknown"));
    }

    @Test
    void buildLineSaysWhenTheBuildDateIsMissing() {
        assertEquals("N/A | MM Unknown, MML Unknown, MHQ Unknown",
              IssueReportUrl.describeBuild(null, "Unknown", "Unknown", "Unknown"));
    }

    @Test
    void namesTheTemplateBecauseTheRepositoriesDisableBlankIssues() {
        String url = IssueReportUrl.forIssueForm(MEGAMEK_ISSUES);

        assertTrue(url.contains("template=bug_report.yml"), url);
        assertTrue(url.startsWith("https://github.com/MegaMek/megamek/issues/new?"), url);
    }

    /**
     * GitHub serves a 404 when a query parameter names a label the visitor cannot apply, and ordinary players hold no
     * triage permission on the suite's repositories. The template applies the {@code bug} label itself, so this
     * parameter must never appear.
     */
    @Test
    void neverSetsTheLabelsParameter() {
        String url = IssueReportUrl.forIssueForm(MEGAMEK_ISSUES);

        assertFalse(url.contains("labels="), "a labels parameter would 404 for non-maintainers: " + url);
    }

    /** Both the operating system and Java descriptions are built with spaces in them on every platform. */
    @Test
    void encodesValuesSoSpacesCannotBreakTheUrl() {
        String url = IssueReportUrl.forIssueForm(MEGAMEK_ISSUES);

        assertFalse(url.contains(" "), "an unencoded space would truncate the link: " + url);
    }

    @Test
    void acceptsARepositoryUrlThatIsAlreadyInFormRatherThanChooserShape() {
        String url = IssueReportUrl.forIssueForm("https://github.com/MegaMek/mekhq/issues/new");

        assertTrue(url.startsWith("https://github.com/MegaMek/mekhq/issues/new?"), url);
        assertFalse(url.contains("/choose"), url);
    }

    /**
     * The bug report form now takes files through an {@code upload} element named {@code report-files}, which no
     * query parameter can fill. The old {@code attached-files} text field no longer exists in any of the templates.
     */
    @Test
    void neverSetsTheRetiredAttachmentField() {
        String url = IssueReportUrl.forIssueForm(MEGAMEK_ISSUES);

        assertFalse(url.contains("attached-files="), "that field was retired when the templates were reworked: " + url);
    }

    @Test
    void buildsTheSameFieldSetForEverySuiteRepository() {
        String megaMekUrl = IssueReportUrl.forIssueForm(MEGAMEK_ISSUES);
        String mekHqUrl = IssueReportUrl.forIssueForm("https://github.com/MegaMek/mekhq/issues/new/choose");

        assertEquals(queryStringOf(megaMekUrl), queryStringOf(mekHqUrl),
              "the three suite repositories share identical issue-form field ids");
    }

    /**
     * @param url the URL to split
     *
     * @return everything after the first question mark
     */
    private static String queryStringOf(String url) {
        return url.substring(url.indexOf('?') + 1);
    }
}
