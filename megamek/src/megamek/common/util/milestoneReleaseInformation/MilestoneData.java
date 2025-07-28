/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.common.util.milestoneReleaseInformation;

import megamek.Version;

/**
 * Represents data about a milestone relating to MegaMek, MegaMekLab, or MekHQ releases.
 *
 * <p> Each {@code MilestoneData} instance contains a descriptive label, a {@link Version} object, and URLs pointing
 * to the relevant release pages for each application. If any URL is {@code null} or blank, it is assigned a default
 * value corresponding to each application's 'releases' page.</p>
 *
 * @param label                Descriptive label for the milestone. Should correspond to the tail of the release's
 *                             GitHub Url.
 * @param version              Version associated with the milestone.
 * @param useFallbackHyperlink {@code true} if the url fetchers should all return their project releases page,
 *                                         instead of a page specific to that Milestone.
 *
 * @author Illiani
 * @since 0.50.07
 */
public record MilestoneData(String label, Version version, boolean useFallbackHyperlink) {
    private final static String DEFAULT_MEGAMEK_URL = "https://github.com/MegaMek/megamek/releases";
    private final static String DEFAULT_MEGAMEKLAB_URL = "https://github.com/MegaMek/megameklab/releases";
    private final static String DEFAULT_MEKHQ_URL = "https://github.com/MegaMek/mekhq/releases";

    private final static String URL_PRE_PROJECT_NAME = "https://github.com/MegaMek/";
    private final static String URL_PROJECT_NAME_MEGAMEK = "megamek";
    private final static String URL_PROJECT_NAME_MEGAMEKLAB = "megameklab";
    private final static String URL_PROJECT_NAME_MEKHQ = "mekhq";
    private final static String URL_POST_PROJECT_NAME = "/releases/tag/";

    /**
     * Returns the full URL for the MegaMek release associated with this milestone.
     *
     * <p>The URL is constructed using predefined URL segments and the MegaMek project name, along with the stored
     * MegaMek URL.</p>
     *
     * @return a complete URL string for the MegaMek release page.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getMegaMekUrl() {
        return useFallbackHyperlink
              ? DEFAULT_MEGAMEK_URL
              : URL_PRE_PROJECT_NAME + URL_PROJECT_NAME_MEGAMEK + URL_POST_PROJECT_NAME + label;
    }

    /**
     * Returns the full URL for the MegaMekLab release associated with this milestone.
     *
     * <p>The URL is constructed dynamically using predefined URL segments, the MegaMekLab project name, and the
     * {@code label} parameter.</p>
     *
     * @return a complete URL string for the MegaMekLab release page.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getMegaMekLabUrl() {
        return useFallbackHyperlink
              ? DEFAULT_MEGAMEKLAB_URL
              : URL_PRE_PROJECT_NAME + URL_PROJECT_NAME_MEGAMEKLAB + URL_POST_PROJECT_NAME + label;
    }

    /**
     * Returns the full URL for the MekHQ release associated with this milestone.
     *
     * <p>The URL is dynamically constructed using predefined URL segments, the MekHQ project name, and the
     * {@code label} parameter.</p>
     *
     * @return a complete URL string for the MekHQ release page.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getMekHQUrl() {
        return useFallbackHyperlink
              ? DEFAULT_MEKHQ_URL
              : URL_PRE_PROJECT_NAME + URL_PROJECT_NAME_MEKHQ + URL_POST_PROJECT_NAME + label;
    }
}

