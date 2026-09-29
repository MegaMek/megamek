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
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 */
package megamek.release;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Validates a complete, immutable suite release record (contract version 1). This validates the
 * record's internal consistency, not the existence or contents of remote commits and assets.
 */
public final class SuiteRecordValidator {
    private static final ObjectMapper MAPPER = new ObjectMapper()
          .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    private static final Pattern VERSION = Pattern.compile("(?:0|[1-9][0-9]*)\\."
          + "(?:[0-9]{2}|[1-9][0-9]{2,})\\."
          + "(?:[0-9]{2}|[1-9][0-9]{2,})");
    private static final Pattern LAUNCHER_VERSION = Pattern.compile("(?:0|[1-9][0-9]*)\\."
          + "(?:0|[1-9][0-9]*)\\.(?:0|[1-9][0-9]*)");
    private static final Pattern COMMIT = Pattern.compile("[0-9a-f]{40}");
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final Map<String, String> REPOSITORIES = Map.of(
          "MegaMek", "megamek",
          "MegaMekLab", "megameklab",
          "MekHQ", "mekhq");

    private SuiteRecordValidator() {
    }

    /** Reads and validates a standalone JSON file; rejects duplicate fields and trailing JSON. */
    public static void validate(Path path) throws IOException {
        try (JsonParser parser = MAPPER.getFactory().createParser(path.toFile())) {
            JsonNode root = MAPPER.readTree(parser);
            if (root == null || parser.nextToken() != null) {
                throw new IllegalArgumentException("Expected exactly one JSON record");
            }
            validate(root);
        }
    }

    /** Validates a parsed record; callers parsing themselves must enable duplicate detection. */
    public static void validate(JsonNode root) {
        fields(root, "record", Set.of("schemaVersion", "version", "membership", "tag",
              "products", "mmData"), Set.of("minimumLauncherVersion"));
        if (!root.get("schemaVersion").isIntegralNumber() || !root.get("schemaVersion").canConvertToInt()
              || root.get("schemaVersion").intValue() != 1) {
            throw new IllegalArgumentException("Unsupported schemaVersion");
        }
        String version = version(root.get("version"), "version");
        String tag = text(root.get("tag"), "tag");
        if (!tag.equals("v" + version)) {
            throw new IllegalArgumentException("Suite tag must be v" + version);
        }
        if (!Set.of("weekly", "development", "milestone").contains(text(root.get("membership"), "membership"))) {
            throw new IllegalArgumentException("Unknown membership");
        }
        if (root.has("minimumLauncherVersion")) {
            match(root.get("minimumLauncherVersion"), LAUNCHER_VERSION, "minimumLauncherVersion");
        }
        JsonNode products = root.get("products");
        fields(products, "products", REPOSITORIES.keySet(), Set.of());
        Set<String> assetNames = new HashSet<>();
        for (Map.Entry<String, String> entry : REPOSITORIES.entrySet()) {
            String name = entry.getKey();
            JsonNode product = products.get(name);
            fields(product, name, Set.of("repository", "commit", "version", "tag", "releaseId", "asset"),
                  Set.of());
            if (!text(product.get("repository"), name + ".repository")
                  .equals("https://github.com/MegaMek/" + entry.getValue())) {
                throw new IllegalArgumentException(name + " repository mismatch");
            }
            match(product.get("commit"), COMMIT, name + ".commit");
            String productVersion = version(product.get("version"), name + ".version");
            if (compareVersions(productVersion, version) > 0
                  || !text(product.get("tag"), name + ".tag").equals("v" + productVersion)) {
                throw new IllegalArgumentException(name + " identity mismatch");
            }
            positiveLong(product.get("releaseId"), name + ".releaseId");
            JsonNode asset = product.get("asset");
            fields(asset, name + ".asset", Set.of("assetId", "name", "sha256", "size"), Set.of());
            positiveLong(asset.get("assetId"), name + ".asset.assetId");
            String filename = text(asset.get("name"), name + ".asset.name");
            if (!filename.equals(name + "-" + productVersion + ".tar.gz") || !assetNames.add(filename)) {
                throw new IllegalArgumentException(name + " asset name mismatch or duplicate");
            }
            match(asset.get("sha256"), SHA256, name + ".asset.sha256");
            positiveLong(asset.get("size"), name + ".asset.size");
        }
        String gameVersion = products.get("MegaMek").get("version").textValue();
        String labVersion = products.get("MegaMekLab").get("version").textValue();
        String hqVersion = products.get("MekHQ").get("version").textValue();
        if (compareVersions(gameVersion, labVersion) > 0 || compareVersions(labVersion, hqVersion) > 0) {
            throw new IllegalArgumentException("Bundled product version cannot predate its dependency");
        }
        JsonNode data = root.get("mmData");
        fields(data, "mmData", Set.of("repository", "commit"), Set.of());
        if (!text(data.get("repository"), "mmData.repository").equals("https://github.com/MegaMek/mm-data")) {
            throw new IllegalArgumentException("mmData repository mismatch");
        }
        match(data.get("commit"), COMMIT, "mmData.commit");
    }

    private static int compareVersions(String left, String right) {
        String[] a = left.split("\\.");
        String[] b = right.split("\\.");
        for (int i = 0; i < a.length; i++) {
            int comparison = Integer.compare(Integer.parseInt(a[i]), Integer.parseInt(b[i]));
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }

    private static String version(JsonNode value, String field) {
        String result = match(value, VERSION, field);
        for (String component : result.split("\\.")) {
            try {
                Integer.parseInt(component);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid " + field + ": version component exceeds Java int range",
                      e);
            }
        }
        return result;
    }

    private static void positiveLong(JsonNode value, String field) {
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong()
              || value.longValue() <= 0) {
            throw new IllegalArgumentException(field + " must be a positive integer");
        }
    }

    private static String match(JsonNode value, Pattern pattern, String field) {
        String result = text(value, field);
        if (!pattern.matcher(result).matches()) {
            throw new IllegalArgumentException("Invalid " + field + ": " + result);
        }
        return result;
    }

    private static String text(JsonNode value, String field) {
        if (value == null || !value.isTextual() || value.textValue().isEmpty()) {
            throw new IllegalArgumentException("Missing or invalid " + field);
        }
        return value.textValue();
    }

    private static void fields(JsonNode node, String field, Set<String> required, Set<String> optional) {
        if (node == null || !node.isObject()) {
            throw new IllegalArgumentException(field + " must be an object");
        }
        for (String key : required) {
            if (!node.has(key)) {
                throw new IllegalArgumentException("Missing " + field + "." + key);
            }
        }
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String key = names.next();
            if (!required.contains(key) && !optional.contains(key)) {
                throw new IllegalArgumentException("Unknown " + field + "." + key);
            }
        }
    }

    /** Exit nonzero on malformed records: run with -P suiteRecordFile=... :megamek:validateSuiteRecord. */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: SuiteRecordValidator <record.json>");
        }
        validate(Path.of(args[0]));
    }
}
