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
package megamek.common.universe;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import megamek.MMConstants;
import megamek.common.annotations.Nullable;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;
import megamek.utilities.xml.MMXMLUtility;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Lightweight loader for {@code data/universe/ranks.xml}. Surfaces faction-specific officer titles to MegaMek consumers
 * that need to display them without depending on the full MekHQ rank-system stack.
 *
 * <p>The integer rank value used by the ratgen pipeline ({@code CrewDescriptor.getRank()}) is the same scheme MekHQ
 * stores on Persons, so a lookup by {@code (rankSystemIndex, rankInt)} returns the canonical short title — e.g.
 * {@code (8, 34)} → "Tai-i" for a DCMS Captain-equivalent.</p>
 *
 * <p>The lookup is lazy and fault-tolerant: callers should fall back to a generic prefix when the loader returns
 * empty (the data file is optional from MegaMek's perspective — it is canonically owned by mm-data).</p>
 */
public final class Ranks {

    private static final MMLogger LOGGER = MMLogger.create(Ranks.class);

    /**
     * Maps the ratgen {@code RANK_*} integer constants from
     * {@code mm-data/data/forcegenerator/faction_rules/constants.txt} onto the {@code <code>} values in
     * {@code ranks.xml}. The integers are emitted into ForceDescriptors via the ruleset's
     * {@code <defaults><rankSystem>%RANK_LA%</rankSystem></defaults>} declaration. Index 12 (RANK_CUSTOM) is a
     * MekHQ-only placeholder with no XML counterpart and intentionally maps to {@code null}.
     */
    private static final String[] RATGEN_INDEX_TO_CODE = {
          "SSLDF", // 0  RANK_SL    (Second Star League Defense Force)
          "AFFS",  // 1  RANK_FS    (Armed Forces of the Federated Suns)
          "AFFC",  // 2  RANK_FC    (Armed Forces of the Federated Commonwealth)
          "LCAF",  // 3  RANK_LC    (Lyran Commonwealth Armed Forces — pre-3035)
          "LAAF",  // 4  RANK_LA    (Lyran Alliance Armed Forces — post-3035)
          "FWLM",  // 5  RANK_FWL   (Free Worlds League Military)
          "CCAF",  // 6  RANK_CC    (Capellan Confederation Armed Forces)
          "CCWH",  // 7  RANK_CC_WH (Capellan Confederation Warrior House)
          "DCMS",  // 8  RANK_DC    (Draconis Combine Mustered Soldiery)
          "CLAN",  // 9  RANK_CLAN
          "CG",    // 10 RANK_CS    (ComStar Guards)
          "WOBM",  // 11 RANK_WOB   (Word of Blake Militia)
          null,    // 12 RANK_CUSTOM — MekHQ-side user customs, no XML entry
          "MAF",   // 13 RANK_MOC   (Magistracy Armed Forces)
          "TDF",   // 14 RANK_TC    (Taurian Defense Force)
          "MHAF",  // 15 RANK_MH    (Marian Hegemony Armed Forces)
          "AMC",   // 16 RANK_OA    (Outworlds Alliance Militia Corps)
          "KA"     // 17 RANK_FRR   (Kungsarme, Free Rasalhague Republic)
    };

    private static Ranks instance;

    private final Map<String, RankSystem> byCode = new HashMap<>();

    private Ranks() {
        try {
            loadFromXml();
        } catch (Exception ex) {
            // Loader failures are non-fatal: callers fall back to their own defaults.
            LOGGER.warn("Failed to load {}; rank lookups will return empty.", MMConstants.RANKS_FILE_PATH, ex);
        }
    }

    public static synchronized Ranks getInstance() {
        if (instance == null) {
            instance = new Ranks();
        }
        return instance;
    }

    /**
     * @param code A rank-system code from ranks.xml, e.g. "LAAF" or "DCMS".
     *
     * @return The matching rank system, or empty if none.
     */
    public Optional<RankSystem> getByCode(@Nullable String code) {
        return Optional.ofNullable(code == null ? null : byCode.get(code));
    }

    /**
     * @param ratgenIndex The integer the ratgen ruleset stores in {@code <rankSystem>} (0-17 in the current data).
     *
     * @return The matching rank system, or empty if the index is unknown or unmapped (e.g. RANK_CUSTOM).
     */
    public Optional<RankSystem> getByRatgenIndex(int ratgenIndex) {
        if (ratgenIndex < 0 || ratgenIndex >= RATGEN_INDEX_TO_CODE.length) {
            return Optional.empty();
        }
        return getByCode(RATGEN_INDEX_TO_CODE[ratgenIndex]);
    }

    /**
     * Convenience accessor combining {@link #getByRatgenIndex(int)} with {@link RankSystem#nameAt(int)}.
     *
     * @param ratgenIndex The ratgen rank-system integer; tolerates {@code null} so callers can pass
     *                    {@code fd.getRankSystem()} directly.
     * @param rankInt     The zero-based rank index.
     *
     * @return The short title (e.g. "Tai-i"), or empty if the system or rank cannot be resolved.
     */
    public Optional<String> resolveRankName(@Nullable Integer ratgenIndex, int rankInt) {
        if (ratgenIndex == null) {
            return Optional.empty();
        }
        return getByRatgenIndex(ratgenIndex).map(system -> system.nameAt(rankInt));
    }

    private void loadFromXml() throws Exception {
        File file = new MegaMekFile(MMConstants.RANKS_FILE_PATH).getFile();
        if (file == null || !file.exists()) {
            LOGGER.info("Ranks file {} not present; rank lookups will return empty.",
                  MMConstants.RANKS_FILE_PATH);
            return;
        }

        Document xmlDoc;
        try (InputStream is = new FileInputStream(file)) {
            xmlDoc = MMXMLUtility.newSafeDocumentBuilder().parse(is);
        }
        Element root = xmlDoc.getDocumentElement();
        root.normalize();
        NodeList systems = root.getElementsByTagName("rankSystem");
        for (int i = 0; i < systems.getLength(); i++) {
            Node systemNode = systems.item(i);
            if (systemNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            RankSystem system = parseRankSystem((Element) systemNode);
            if (system != null) {
                byCode.put(system.code(), system);
            }
        }
        LOGGER.info("Loaded {} rank system(s) from {}", byCode.size(), MMConstants.RANKS_FILE_PATH);
    }

    private static @Nullable RankSystem parseRankSystem(Element systemElement) {
        String code = childText(systemElement, "code");
        if (code == null || code.isBlank()) {
            return null;
        }
        String name = childText(systemElement, "name");
        List<String[]> ranks = new ArrayList<>();
        NodeList rankNodes = systemElement.getElementsByTagName("rank");
        for (int i = 0; i < rankNodes.getLength(); i++) {
            Node rankNode = rankNodes.item(i);
            if (rankNode.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String rankNames = childText((Element) rankNode, "rankNames");
            ranks.add(rankNames == null ? new String[0] : rankNames.split(","));
        }
        return new RankSystem(code, name == null ? code : name, List.copyOf(ranks));
    }

    private static @Nullable String childText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getParentNode() == parent) {
                return node.getTextContent();
            }
        }
        return null;
    }
}
