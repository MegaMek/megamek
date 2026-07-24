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
package megamek.common.battlefieldSupport;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import megamek.common.annotations.Nullable;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.UnitRole;

/**
 * Reads and writes {@link BattlefieldSupportAssetData} stat blocks in the {@code .bfs} YAML format.
 * <p>
 * The format stores the Asset's real identity ({@code chassis}/{@code model}), its rules category
 * ({@code assetType}), structured stats (movement, range, skill, damage, cost) and its Specials as a list of card
 * tokens (for example {@code IF2}, {@code Artillery (LT)}). Optional card-name overrides, role and fluff image are
 * written only when set. Unknown Special tokens are preserved verbatim. There is no format version field: if the
 * format changes, the data files are migrated rather than supporting multiple versions.
 */
public final class BattlefieldSupportAssetYaml {

    // Top-level keys
    private static final String UUID = "uuid";
    private static final String LINKED_UNIT_ID = "linkedUnitId";
    private static final String CHASSIS = "chassis";
    private static final String MODEL = "model";
    private static final String ASSET_TYPE = "assetType";
    private static final String CARD_TITLE = "cardTitle";
    private static final String CARD_SUBTITLE = "cardSubtitle";
    private static final String YEAR = "year";
    private static final String TECH_BASE = "techBase";
    private static final String SOURCE = "source";
    private static final String MOVEMENT = "movement";
    private static final String TMM = "tmm";
    private static final String RANGE = "range";
    private static final String SKILL = "skill";
    private static final String DAMAGE = "damage";
    private static final String DESTROY_CHECK = "destroyCheck";
    private static final String THRESHOLD = "threshold";
    private static final String COST = "cost";
    private static final String SPECIALS = "specials";
    private static final String ROLE = "role";
    private static final String FLUFF_IMAGE = "fluffImage";
    private static final String ICON = "icon";

    // Nested keys
    private static final String MP = "mp";
    private static final String MODE = "mode";
    private static final String STANDARD = "standard";
    private static final String VETERAN = "veteran";
    private static final String PER_HIT = "perHit";
    private static final String HITS = "hits";

    private static YAMLMapper mapper() {
        YAMLMapper mapper = new YAMLMapper();
        mapper.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        return mapper;
    }

    /**
     * Determines whether the given string is a {@code .bfs} document: it must parse as YAML and carry a top-level
     * {@code assetType} field (which is unique to this format - no {@code .mtf} or {@code .blk} file parses as a YAML
     * mapping with that key). The individual fields are not validated; this only routes a raw, filename-less unit
     * string (for example an undo/redo memento) to the correct loader.
     *
     * @param content the unit representation to test
     *
     * @return {@code true} if the content is a YAML mapping with an {@code assetType} key
     */
    public static boolean isAssetDocument(@Nullable String content) {
        if ((content == null) || content.isBlank()) {
            return false;
        }
        try {
            JsonNode root = mapper().readTree(content);
            return (root != null) && root.isObject() && root.has(ASSET_TYPE);
        } catch (Exception ex) {
            return false;
        }
    }

    private BattlefieldSupportAssetYaml() { }

    // region Serialization

    /**
     * Serializes the given Asset to a YAML string in the {@code .bfs} format.
     *
     * @param asset the Asset to serialize
     *
     * @return the YAML document
     *
     * @throws IOException if serialization fails
     */
    public static String toYaml(BattlefieldSupportAssetData asset) throws IOException {
        return mapper().writeValueAsString(toMap(asset));
    }

    /**
     * Builds the ordered map representation of the given Asset used for YAML serialization.
     *
     * @param asset the Asset to convert
     *
     * @return an ordered map matching the {@code .bfs} schema
     */
    public static Map<String, Object> toMap(BattlefieldSupportAssetData asset) {
        Map<String, Object> data = new LinkedHashMap<>();
        putIfNotBlank(data, UUID, asset.getUuid());
        putIfNotBlank(data, LINKED_UNIT_ID, asset.getLinkedUnitId());
        data.put(CHASSIS, asset.getChassis());
        data.put(MODEL, asset.getModel());
        data.put(ASSET_TYPE, asset.getAssetType().displayName());

        putIfNotBlank(data, CARD_TITLE, asset.getCardTitle());
        putIfNotBlank(data, CARD_SUBTITLE, asset.getCardSubtitle());

        data.put(YEAR, asset.getYear());
        data.put(TECH_BASE, asset.getTechBase());
        putIfNotBlank(data, SOURCE, asset.getSource());

        Map<String, Object> movement = new LinkedHashMap<>();
        movement.put(MP, asset.getMp());
        movement.put(MODE, asset.getMovementMode().name());
        data.put(MOVEMENT, movement);

        data.put(TMM, asset.getTmm());

        BFSRange range = asset.getRange();
        data.put(RANGE, List.of(range.shortRange(), range.mediumRange(), range.longRange()));

        data.put(SKILL, valuePair(asset.getSkill(), asset.getVeteranSkill()));

        Map<String, Object> damage = new LinkedHashMap<>();
        damage.put(PER_HIT, asset.getDamage().perHit());
        damage.put(HITS, asset.getDamage().hits());
        data.put(DAMAGE, damage);

        data.put(DESTROY_CHECK, asset.getDestroyCheck());
        data.put(THRESHOLD, asset.getThreshold());
        data.put(COST, valuePair(asset.getCost(), asset.getVeteranCost()));

        List<String> specials = new ArrayList<>();
        for (BFSSpecial special : asset.getSpecials()) {
            specials.add(special.displayString());
        }
        data.put(SPECIALS, specials);

        if (asset.getRole().hasRole()) {
            data.put(ROLE, asset.getRole().name());
        }
        putIfNotBlank(data, FLUFF_IMAGE, asset.getFluffImageEncoded());
        putIfNotBlank(data, ICON, asset.getIconEncoded());

        return data;
    }

    private static Map<String, Object> valuePair(int standard, @Nullable Integer veteran) {
        Map<String, Object> pair = new LinkedHashMap<>();
        pair.put(STANDARD, standard);
        if (veteran != null) {
            pair.put(VETERAN, veteran);
        }
        return pair;
    }

    private static void putIfNotBlank(Map<String, Object> data, String key, @Nullable String value) {
        if (value != null && !value.isBlank()) {
            data.put(key, value);
        }
    }

    // endregion

    // region Deserialization

    /**
     * Reads an Asset from a {@code .bfs} YAML input stream.
     *
     * @param is the input stream to read
     *
     * @return the parsed Asset
     *
     * @throws IOException if the stream cannot be read or parsed
     */
    public static BattlefieldSupportAssetData fromYaml(InputStream is) throws IOException {
        return fromNode(mapper().readTree(is));
    }

    /**
     * Reads an Asset from a {@code .bfs} YAML string.
     *
     * @param yaml the YAML document to read
     *
     * @return the parsed Asset
     *
     * @throws IOException if the string cannot be parsed
     */
    public static BattlefieldSupportAssetData fromYaml(String yaml) throws IOException {
        return fromNode(mapper().readTree(yaml));
    }

    private static BattlefieldSupportAssetData fromNode(@Nullable JsonNode node) throws IOException {
        if (node == null || node.isNull()) {
            throw new IOException("Empty or invalid .bfs document");
        }
        BattlefieldSupportAssetData asset = new BattlefieldSupportAssetData();

        asset.setUuid(text(node, UUID, null));
        asset.setLinkedUnitId(text(node, LINKED_UNIT_ID, null));
        asset.setChassis(text(node, CHASSIS, ""));
        asset.setModel(text(node, MODEL, ""));

        BFSAssetType assetType = BFSAssetType.fromString(text(node, ASSET_TYPE, null));
        if (assetType != null) {
            asset.setAssetType(assetType);
        }

        asset.setCardTitle(text(node, CARD_TITLE, null));
        asset.setCardSubtitle(text(node, CARD_SUBTITLE, null));

        asset.setYear(intValue(node, YEAR, asset.getYear()));
        String techBase = text(node, TECH_BASE, null);
        if (techBase != null) {
            asset.setTechBase(techBase);
        }
        asset.setSource(text(node, SOURCE, ""));

        JsonNode movement = node.get(MOVEMENT);
        if (movement != null) {
            asset.setMp(intValue(movement, MP, 0));
            String modeText = text(movement, MODE, null);
            if (modeText != null) {
                asset.setMovementMode(EntityMovementMode.parseFromString(modeText));
            }
        }

        asset.setTmm(intValue(node, TMM, 0));

        JsonNode range = node.get(RANGE);
        if (range != null && range.isArray() && range.size() == 3) {
            asset.setRange(new BFSRange(range.get(0).asInt(), range.get(1).asInt(), range.get(2).asInt()));
        }

        JsonNode skill = node.get(SKILL);
        if (skill != null) {
            asset.setSkill(intValue(skill, STANDARD, asset.getSkill()));
            asset.setVeteranSkill(optionalInt(skill, VETERAN));
        }

        JsonNode damage = node.get(DAMAGE);
        if (damage != null) {
            asset.setDamage(new BFSDamage(intValue(damage, PER_HIT, 0), intValue(damage, HITS, 0)));
        }

        asset.setDestroyCheck(intValue(node, DESTROY_CHECK, asset.getDestroyCheck()));
        asset.setThreshold(intValue(node, THRESHOLD, asset.getThreshold()));

        JsonNode cost = node.get(COST);
        if (cost != null) {
            asset.setCost(intValue(cost, STANDARD, 0));
            asset.setVeteranCost(optionalInt(cost, VETERAN));
        }

        JsonNode specials = node.get(SPECIALS);
        if (specials != null && specials.isArray()) {
            List<BFSSpecial> parsed = new ArrayList<>();
            for (JsonNode entry : specials) {
                BFSSpecial special = BFSSpecial.parse(entry.asText());
                if (special != null) {
                    parsed.add(special);
                }
            }
            asset.setSpecials(parsed);
        }

        String role = text(node, ROLE, null);
        if (role != null) {
            asset.setRole(UnitRole.parseRole(role));
        }
        asset.setFluffImageEncoded(text(node, FLUFF_IMAGE, null));
        asset.setIconEncoded(text(node, ICON, null));

        return asset;
    }

    private static @Nullable String text(JsonNode node, String key, @Nullable String defaultValue) {
        JsonNode child = node.get(key);
        return (child == null || child.isNull()) ? defaultValue : child.asText();
    }

    private static int intValue(JsonNode node, String key, int defaultValue) {
        JsonNode child = node.get(key);
        return (child == null || child.isNull()) ? defaultValue : child.asInt(defaultValue);
    }

    private static @Nullable Integer optionalInt(JsonNode node, String key) {
        JsonNode child = node.get(key);
        return (child == null || child.isNull()) ? null : child.asInt();
    }

    // endregion
}
