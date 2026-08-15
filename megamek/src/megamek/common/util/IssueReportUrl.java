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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

import megamek.SuiteConstants;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;

/**
 * Builds a GitHub issue-form URL with the reporter's environment details already filled in.
 *
 * <p>The suite's bug report template asks for the MegaMek version, operating system and Java version, all of them
 * required, and instructs the player to find them by opening {@code megamek.log} in a text editor and reading its
 * header. MegaMek already knows all three, so this class puts them into the form's URL and spares the player the
 * transcription.</p>
 *
 * <p>MegaMek, MegaMekLab and MekHQ all use GitHub issue forms whose field ids are identical, so a single query
 * string serves all three and only the repository differs. The field ids below are a contract with
 * {@code .github/ISSUE_TEMPLATE/bug_report.yml} in each of those repositories: if a field id is renamed there, the
 * corresponding field here silently stops being filled in, without any error.</p>
 *
 * <p><b>The {@code labels} query parameter is deliberately never set.</b> GitHub requires the visitor to hold
 * permission for any action a query parameter performs, and serves a 404 page when they do not. Since ordinary
 * players have no triage permission on the suite's repositories, adding a {@code labels} parameter would replace the
 * issue form with a 404 for exactly the people this feature exists to help, while continuing to work for every
 * maintainer who tested it. Labelling belongs to the issue template, which applies its labels for every reporter
 * regardless of permission.</p>
 *
 * @see megamek.common.util.BugReportBundle
 */
public final class IssueReportUrl {
    private static final MMLogger LOGGER = MMLogger.create(IssueReportUrl.class);

    /** Field ids from bug_report.yml. Renaming one there silently disables that field's prefill. */
    private static final String FIELD_VERSION = "custom-megamek-version";
    private static final String FIELD_OPERATING_SYSTEM = "operating-system";
    private static final String FIELD_JAVA_VERSION = "java-version";
    private static final String FIELD_ATTACHED_FILES = "attached-files";

    /** The suite's repositories disable blank issues, so the template has to be named explicitly. */
    private static final String TEMPLATE_PARAMETER = "template=bug_report.yml";

    /** The trailing path on the plain repository links, replaced by the prefilled form path. */
    private static final String CHOOSER_PATH = "/issues/new/choose";
    private static final String FORM_PATH = "/issues/new";

    /** Kept well inside what browsers and GitHub accept, so a long note can never break the link. */
    private static final int MAX_URL_LENGTH = 6000;

    private IssueReportUrl() {}

    /**
     * Builds a prefilled issue-form URL for one of the repositories that uses the suite bug report template.
     *
     * <p>If including the attachment note would push the URL past {@link #MAX_URL_LENGTH}, the note is dropped; if
     * the URL is still too long without it, the plain issue-chooser link is returned instead. A truncated URL is
     * never produced, since that would land the player on a 404.</p>
     *
     * @param repositoryIssuesUrl the repository's issue link, in either the {@code /issues/new/choose} or
     *                            {@code /issues/new} form
     * @param attachmentNote      a note naming the bug report archive the player just built, or {@code null} when no
     *                            archive has been created yet
     *
     * @return a URL that opens the issue form with the environment fields populated
     */
    public static String forIssueForm(String repositoryIssuesUrl, @Nullable String attachmentNote) {
        String formUrl = repositoryIssuesUrl.endsWith(CHOOSER_PATH)
              ? repositoryIssuesUrl.substring(0, repositoryIssuesUrl.length() - CHOOSER_PATH.length()) + FORM_PATH
              : repositoryIssuesUrl;

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(FIELD_VERSION, SuiteConstants.VERSION.toString());
        fields.put(FIELD_OPERATING_SYSTEM, operatingSystemDescription());
        fields.put(FIELD_JAVA_VERSION, javaVersionDescription());
        if ((attachmentNote != null) && !attachmentNote.isBlank()) {
            fields.put(FIELD_ATTACHED_FILES, attachmentNote);
        }

        String candidateUrl = assemble(formUrl, fields);
        if (candidateUrl.length() <= MAX_URL_LENGTH) {
            return candidateUrl;
        }

        LOGGER.debug("[BugReport] Prefilled issue URL was {} characters; dropping the attachment note",
              candidateUrl.length());
        fields.remove(FIELD_ATTACHED_FILES);
        candidateUrl = assemble(formUrl, fields);
        if (candidateUrl.length() <= MAX_URL_LENGTH) {
            return candidateUrl;
        }

        LOGGER.warn("[BugReport] Could not build a prefilled issue URL within {} characters; "
              + "falling back to the plain issue chooser", MAX_URL_LENGTH);
        return repositoryIssuesUrl;
    }

    /**
     * Joins the template selector and the encoded fields onto the form URL.
     *
     * @param formUrl the repository's {@code /issues/new} URL
     * @param fields  the field ids and values to prefill, in insertion order
     *
     * @return the assembled URL
     */
    private static String assemble(String formUrl, Map<String, String> fields) {
        StringJoiner queryString = new StringJoiner("&");
        queryString.add(TEMPLATE_PARAMETER);
        for (Map.Entry<String, String> field : fields.entrySet()) {
            queryString.add(field.getKey() + "=" + encode(field.getValue()));
        }
        return formUrl + "?" + queryString;
    }

    /**
     * @param value the raw field value
     *
     * @return the value encoded for use in a query string
     */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * @return the operating system name, version and architecture, matching the detail level the bug report template
     *       asks for
     */
    private static String operatingSystemDescription() {
        return "%s %s (%s)".formatted(System.getProperty("os.name"),
              System.getProperty("os.version"),
              System.getProperty("os.arch"));
    }

    /**
     * @return the Java vendor and version, in the same shape as the example in the bug report template
     */
    private static String javaVersionDescription() {
        return "%s %s".formatted(System.getProperty("java.vendor"), System.getProperty("java.version"));
    }
}
