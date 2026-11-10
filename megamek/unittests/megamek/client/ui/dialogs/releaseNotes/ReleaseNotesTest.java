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
package megamek.client.ui.dialogs.releaseNotes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import megamek.Version;
import org.junit.jupiter.api.Test;

class ReleaseNotesTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String BODY = String.join("\n",
          "| Download | Link |",
          "| --- | --- |",
          "- not a change",
          "## What's Changed",
          "### Behind the scenes",
          "* Tidy the build by @someone in https://github.com/MegaMek/megamek/pull/9001",
          "### Bug Fixes",
          "* Fix the jump arc & range by @someone in https://github.com/MegaMek/megamek/pull/9002",
          "* Bump org.junit:junit-bom from 5.10.0 to 5.11.0 by @dependabot[bot] in "
                + "https://github.com/MegaMek/megamek/pull/9003",
          "* Bump gradle from 8.1 to 8.2 by @someone in https://github.com/MegaMek/megamek/pull/9004",
          "### Something New",
          "* A change from another repository by @someone in https://github.com/MegaMek/mekhq/pull/77",
          "### New Features",
          "* Add a What's new dialog by @someone in https://github.com/MegaMek/megamek/pull/9005",
          "",
          "## New Contributors",
          "* @someone made their first contribution in https://github.com/MegaMek/megamek/pull/9001",
          "",
          "**Full Changelog**: https://github.com/MegaMek/megamek/compare/v0.51.01.4...v0.51.01.5");

    @Test
    void tagNeedsARevision() {
        assertEquals("v0.51.01.5", ReleaseNotes.tag(new Version("0", "51", "1", "5", "main-abc1234")));
        assertNull(ReleaseNotes.tag(new Version("0", "52", "0", null, null)));
    }

    @Test
    void pageIsTheReleaseOrTheReleaseList() {
        assertEquals(URI.create("https://github.com/MegaMek/mekhq/releases/tag/v0.51.01.5"),
              ReleaseNotes.page("mekhq", new Version("0", "51", "1", "5", null)));
        assertEquals(URI.create("https://github.com/MegaMek/mekhq/releases"),
              ReleaseNotes.page("mekhq", new Version("0", "52", "0", null, null)));
    }

    @Test
    void sectionsFollowTheReleaseOrder() {
        List<ReleaseNotes.Section> sections = ReleaseNotes.parse("megamek", BODY);

        assertEquals(List.of("New Features", "Bug Fixes", "Something New", "Behind the scenes"),
              sections.stream().map(ReleaseNotes.Section::heading).toList());
    }

    @Test
    void changesKeepTheirTitleAndPullRequest() {
        ReleaseNotes.Change change = ReleaseNotes.parse("megamek", BODY).get(0).changes().get(0);

        assertEquals("Add a What's new dialog", change.title());
        assertEquals(9005, change.pullRequest());
        assertEquals(URI.create("https://github.com/MegaMek/megamek/pull/9005"), change.link());
    }

    @Test
    void dependencyBumpsAreLeftOut() {
        List<ReleaseNotes.Change> fixes = ReleaseNotes.parse("megamek", BODY).get(1).changes();

        assertEquals(List.of("Fix the jump arc & range"), fixes.stream().map(ReleaseNotes.Change::title).toList());
    }

    @Test
    void pullRequestsOfOtherRepositoriesGetNoLink() {
        ReleaseNotes.Change change = ReleaseNotes.parse("megamek", BODY).get(2).changes().get(0);

        assertEquals("A change from another repository", change.title());
        assertEquals(0, change.pullRequest());
        assertNull(change.link());
    }

    @Test
    void changesWithoutHeadingsAreGrouped() {
        List<ReleaseNotes.Section> sections = ReleaseNotes.parse("megamek",
              "## What's Changed\n* Written by hand\n* Fix it by @a in https://github.com/MegaMek/megamek/pull/1\n");

        assertEquals(1, sections.size());
        assertEquals(ReleaseNotes.UNGROUPED, sections.get(0).heading());
        assertEquals(List.of("Written by hand", "Fix it"),
              sections.get(0).changes().stream().map(ReleaseNotes.Change::title).toList());
    }

    @Test
    void notesWithoutChangesHaveNoSections() {
        assertTrue(ReleaseNotes.parse("megamek", "Just a release\n").isEmpty());
        assertTrue(ReleaseNotes.parse("megamek", null).isEmpty());
    }

    @Test
    void fromJsonReadsTheBody() throws IOException {
        Version version = new Version("0", "51", "1", "5", null);
        String json = "{\"tag_name\":\"v0.51.01.5\",\"body\":\"## What's Changed\\n"
              + "* Fix it by @a in https://github.com/MegaMek/megamek/pull/12\"}";

        ReleaseNotes notes = ReleaseNotes.fromJson("megamek", version, json.getBytes(StandardCharsets.UTF_8));

        assertEquals(URI.create("https://github.com/MegaMek/megamek/releases/tag/v0.51.01.5"), notes.page());
        assertEquals(12, notes.sections().get(0).changes().get(0).pullRequest());
    }

    @Test
    void fromJsonRejectsOtherDocuments() {
        Version version = new Version("0", "51", "1", "5", null);

        assertThrows(IOException.class,
              () -> ReleaseNotes.fromJson("megamek", version, "[1, 2]".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void releaseVersionsNeedARevisionedTag() {
        assertEquals(0, new Version("0.51.01.5").compareTo(ReleaseNotes.releaseVersion("v0.51.01.5")));
        assertNull(ReleaseNotes.releaseVersion("v0.51.01-Development"));
        assertNull(ReleaseNotes.releaseVersion("v0.51.00.1-Core Rules"));
        assertNull(ReleaseNotes.releaseVersion("0.51.01.5"));
    }

    @Test
    void anUpdateRemembersTheVersionBefore() {
        Version running = new Version("0", "51", "1", "6", "main-abc1234");

        assertEquals("0.51.01.5", ReleaseNotes.previousAfterStart("0.51.01.5", running));
        assertNull(ReleaseNotes.previousAfterStart("0.51.01.6", running));
    }

    @Test
    void aFirstStartHasNoVersionBefore() {
        Version running = new Version("0", "51", "1", "6", null);

        assertEquals("", ReleaseNotes.previousAfterStart("", running));
        assertEquals("", ReleaseNotes.previousAfterStart(null, running));
        assertEquals("", ReleaseNotes.previousAfterStart("latest", running));
    }

    @Test
    void aDevelopmentBuildRemembersNothing() {
        assertNull(ReleaseNotes.previousAfterStart("0.51.01.5", new Version("0", "52", "0", null, null)));
    }

    @Test
    void onlyAnOlderPreviousVersionIsASince() {
        Version running = new Version("0", "51", "1", "6", null);

        assertEquals(0, new Version("0.51.01.4").compareTo(ReleaseNotes.since("0.51.01.4", running)));
        assertNull(ReleaseNotes.since("0.51.01.7", running));
        assertNull(ReleaseNotes.since("", running));
        assertNull(ReleaseNotes.since("0.51", running));
    }

    @Test
    void combiningKeepsOnlyTheReleasesOfTheTargetsChannel() throws IOException {
        List<JsonNode> releases = releases(
              release("v0.51.01.7", true, "* Too new by @a in https://github.com/MegaMek/megamek/pull/7"),
              release("v0.51.01.6", false, "### Bug Fixes\n* Core fix by @a in https://github.com/MegaMek/megamek/pull/6"),
              release("v0.51.01.5", true, "### Bug Fixes\n* Fix five by @a in https://github.com/MegaMek/megamek/pull/5"
                    + "\n### New Features\n* Feature by @a in https://github.com/MegaMek/megamek/pull/3"),
              release("v0.51.01.4", true, "### Bug Fixes\n* Fix four by @a in https://github.com/MegaMek/megamek/pull/4"
                    + "\n* Feature by @a in https://github.com/MegaMek/megamek/pull/3"),
              release("v0.51.01.3", true, "* Already seen by @a in https://github.com/MegaMek/megamek/pull/2"));

        ReleaseNotes notes = ReleaseNotes.combine("megamek", releases, new Version("0.51.01.3"),
              new Version("0.51.01.5"));

        assertEquals("0.51.01.3", notes.since());
        assertEquals(URI.create("https://github.com/MegaMek/megamek/releases"), notes.page());
        assertEquals(List.of("New Features", "Bug Fixes"),
              notes.sections().stream().map(ReleaseNotes.Section::heading).toList());
        assertEquals(List.of(3), notes.sections().get(0).changes().stream()
              .map(ReleaseNotes.Change::pullRequest).toList());
        assertEquals(List.of(5, 4), notes.sections().get(1).changes().stream()
              .map(ReleaseNotes.Change::pullRequest).toList());
    }

    @Test
    void aFirstCoreReleaseHasOnlyItsOwnNotes() throws IOException {
        List<JsonNode> releases = releases(
              release("v0.51.01.6", false, "First Core release, no previous Core release to compare with.\n"),
              release("v0.51.01.5", true, "### Bug Fixes\n* Fix by @a in https://github.com/MegaMek/megamek/pull/5"));

        ReleaseNotes notes = ReleaseNotes.combine("megamek", releases, new Version("0.51.01.5"),
              new Version("0.51.01.6"));

        assertTrue(notes.sections().isEmpty());
        assertEquals(URI.create("https://github.com/MegaMek/megamek/releases/tag/v0.51.01.6"), notes.page());
    }

    @Test
    void combiningNeedsTheTargetRelease() throws IOException {
        List<JsonNode> releases = releases(release("v0.51.01.4", true, ""));

        assertNull(ReleaseNotes.combine("megamek", releases, new Version("0.51.01.3"), new Version("0.51.01.5")));
    }

    private static String release(String tag, boolean prerelease, String body) throws IOException {
        String notes = body.startsWith("First") ? body : "## What's Changed\n" + body;
        return MAPPER.writeValueAsString(Map.of("tag_name", tag, "prerelease", prerelease, "draft", false,
              "body", notes));
    }

    private static List<JsonNode> releases(String... releases) throws IOException {
        List<JsonNode> nodes = new ArrayList<>();
        for (String release : releases) {
            nodes.add(MAPPER.readTree(release));
        }
        return nodes;
    }
}
