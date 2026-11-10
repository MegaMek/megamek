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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import megamek.Version;

/**
 * The published release notes of one version of a MegaMek suite application, as GitHub generates them from the titles
 * of the merged pull requests.
 *
 * @param repository The repository name, without the organization, e.g. "megamek"
 * @param page       The release page on GitHub
 * @param sections   The changes, grouped by kind in the order the release notes use
 */
public record ReleaseNotes(String repository, URI page, List<Section> sections) {

    /** Heading for changes listed before any heading, as in notes from before the categories. */
    static final String UNGROUPED = "Changes";
    private static final int MAX_BYTES = 2_000_000;
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    /** The order of the headings in .github/release.yml of the suite repositories. */
    private static final List<String> HEADINGS = List.of("new features", "rules", "improvements", "bug fixes",
          "other changes", "behind the scenes");
    private static final Pattern SECTION = Pattern.compile("^##\\s+(.+?)\\s*#*\\s*$");
    private static final Pattern SUBSECTION = Pattern.compile("^###\\s+(.+?)\\s*#*\\s*$");
    private static final Pattern ITEM = Pattern.compile("^[*-]\\s+(.+)$");
    private static final Pattern GENERATED = Pattern.compile(
          "^(.+?) by @([A-Za-z0-9-]+(?:\\[bot])?) in (https://\\S+)$");
    /** A dependency bump in Dependabot's wording, even when a person merged it. */
    private static final Pattern BUMP = Pattern.compile("^Bump \\S+ from \\S+ to \\S+$");

    public ReleaseNotes {
        sections = List.copyOf(sections);
    }

    /**
     * One change, usually a pull request title.
     *
     * @param title       The change as listed
     * @param pullRequest The pull request number, or 0 when the change has none
     * @param link        The pull request page, or {@code null} when there is no safe link
     */
    public record Change(String title, int pullRequest, URI link) {
    }

    /**
     * The changes listed under one heading.
     *
     * @param heading The heading, as GitHub wrote it
     * @param changes The changes, in the order listed
     */
    public record Section(String heading, List<Change> changes) {
        public Section {
            changes = List.copyOf(changes);
        }
    }

    /**
     * Returns the GitHub tag of a suite release, e.g. "v0.51.01.5", or {@code null} when the version has no revision and so
     * was never released.
     *
     * @param version The version
     *
     * @return The tag, or {@code null}
     */
    public static String tag(Version version) {
        if (!version.hasRevision()) {
            return null;
        }
        return String.format("v%d.%02d.%02d.%d", version.getMajor(), version.getMinor(), version.getPatch(),
              version.getRevision());
    }

    /**
     * Returns the release page of the given version, or the list of all releases when it has no release.
     *
     * @param repository The repository name, without the organization
     * @param version    The version
     *
     * @return The page address
     */
    public static URI page(String repository, Version version) {
        String releases = "https://github.com/MegaMek/" + repository + "/releases";
        String tag = tag(version);
        return URI.create(tag == null ? releases : releases + "/tag/" + tag);
    }

    /**
     * Loads the release notes of the given version from GitHub. Call this off the event thread.
     *
     * @param repository The repository name, without the organization
     * @param version    The version
     *
     * @return The notes
     *
     * @throws IOException          when the version has no release or the notes cannot be read
     * @throws InterruptedException when interrupted while waiting for GitHub
     */
    public static ReleaseNotes load(String repository, Version version) throws IOException, InterruptedException {
        String tag = tag(version);
        if (tag == null) {
            throw new IOException("Version " + version + " has no release");
        }
        URI api = URI.create("https://api.github.com/repos/MegaMek/" + repository + "/releases/tags/" + tag);
        HttpRequest request = HttpRequest.newBuilder(api)
              .timeout(TIMEOUT)
              .header("Accept", "application/vnd.github+json")
              .header("User-Agent", "MegaMek-Suite/" + version)
              .GET()
              .build();
        try (HttpClient client = HttpClient.newBuilder()
              .connectTimeout(TIMEOUT)
              .followRedirects(HttpClient.Redirect.NORMAL)
              .build()) {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream body = response.body()) {
                if (response.statusCode() != 200) {
                    throw new IOException("GitHub returned HTTP " + response.statusCode() + " for " + tag);
                }
                byte[] bytes = body.readNBytes(MAX_BYTES + 1);
                if (bytes.length > MAX_BYTES) {
                    throw new IOException("The release notes of " + tag + " exceed the size limit");
                }
                return fromJson(repository, version, bytes);
            }
        }
    }

    static ReleaseNotes fromJson(String repository, Version version, byte[] json) throws IOException {
        JsonNode release = MAPPER.readTree(json);
        if ((release == null) || !release.isObject()) {
            throw new IOException("GitHub returned an unexpected release");
        }
        return new ReleaseNotes(repository, page(repository, version),
              parse(repository, release.path("body").asText("")));
    }

    /**
     * Reads the "What's Changed" part of GitHub's generated notes: its headings and the changes under them. The
     * download table above it, the new contributors and the full changelog link below it, and Dependabot's dependency
     * bumps are left out. Known headings are put in the order of the suite's release.yml.
     */
    static List<Section> parse(String repository, String body) {
        Map<String, List<Change>> sections = new LinkedHashMap<>();
        boolean changes = false;
        String heading = UNGROUPED;
        for (String raw : (body == null ? "" : body).split("\\R")) {
            String line = raw.strip();
            Matcher section = SECTION.matcher(line);
            if (section.matches() && !line.startsWith("###")) {
                changes = section.group(1).toLowerCase(Locale.ROOT).startsWith("what");
                heading = UNGROUPED;
                continue;
            }
            if (!changes) {
                continue;
            }
            if (line.startsWith("**Full Changelog**")) {
                break;
            }
            Matcher subsection = SUBSECTION.matcher(line);
            if (subsection.matches()) {
                heading = subsection.group(1).strip();
                continue;
            }
            Matcher item = ITEM.matcher(line);
            if (!item.matches()) {
                continue;
            }
            Change change = change(repository, item.group(1).strip());
            if (change != null) {
                sections.computeIfAbsent(heading, ignored -> new ArrayList<>()).add(change);
            }
        }
        List<String> order = new ArrayList<>(sections.keySet());
        order.sort(Comparator.comparingInt(ReleaseNotes::rank));
        List<Section> parsed = new ArrayList<>();
        for (String name : order) {
            parsed.add(new Section(name, sections.get(name)));
        }
        return parsed;
    }

    private static Change change(String repository, String text) {
        Matcher generated = GENERATED.matcher(text);
        if (!generated.matches()) {
            return new Change(text, 0, null);
        }
        if (generated.group(2).toLowerCase(Locale.ROOT).startsWith("dependabot")) {
            return null;
        }
        String title = generated.group(1).strip();
        if (BUMP.matcher(title).matches()) {
            return null;
        }
        Matcher pull = Pattern.compile("^https://github\\.com/MegaMek/" + Pattern.quote(repository)
              + "/pull/([0-9]{1,9})$", Pattern.CASE_INSENSITIVE).matcher(generated.group(3));
        if (!pull.matches()) {
            return new Change(title, 0, null);
        }
        int number;
        try {
            number = Integer.parseInt(pull.group(1));
        } catch (NumberFormatException ignored) {
            return new Change(title, 0, null);
        }
        return new Change(title, number, URI.create("https://github.com/MegaMek/" + repository + "/pull/" + number));
    }

    /** Known headings keep the release notes' order; others go just before "Behind the scenes". */
    private static int rank(String heading) {
        int known = HEADINGS.indexOf(heading.toLowerCase(Locale.ROOT));
        return known >= 0 ? known * 2 : HEADINGS.indexOf("behind the scenes") * 2 - 1;
    }
}
