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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SuiteRecordValidatorTest {
    private static final Path COMPLETE = Path.of("testresources/suite-records/complete.json");

    @TempDir
    Path temp;

    @Test
    void completeFixture() {
        assertDoesNotThrow(() -> SuiteRecordValidator.validate(COMPLETE));
    }

    @Test
    void reusedProductKeepsItsOwnTagAndFilename() throws IOException {
        String good = Files.readString(COMPLETE);
        String[] bad = {
              good.replace("MegaMek-0.51.99.tar.gz", "MegaMek-0.51.100.tar.gz"),
              good.replace("\"tag\": \"v0.51.99\"", "\"tag\": \"v0.51.100\""),
              good.replace("\"version\": \"0.51.99\"", "\"version\": \"0.51.101\"")
                    .replace("v0.51.99", "v0.51.101")
                    .replace("MegaMek-0.51.99", "MegaMek-0.51.101")
        };
        for (int i = 0; i < bad.length; i++) {
            Path file = temp.resolve("mixed-" + i + ".json");
            Files.writeString(file, bad[i]);
            assertThrows(IllegalArgumentException.class, () -> SuiteRecordValidator.validate(file));
        }
    }

    @Test
    void rejectsBundleOlderThanChangedDependency() throws IOException {
        String good = Files.readString(COMPLETE).replace("\r\n", "\n");
        String staleHq = good.replace("\"version\": \"0.51.100\",\n      \"tag\": \"v0.51.100\",\n      \"releaseId\": 103",
              "\"version\": \"0.51.99\",\n      \"tag\": \"v0.51.99\",\n      \"releaseId\": 103")
              .replace("MekHQ-0.51.100.tar.gz", "MekHQ-0.51.99.tar.gz");
        assertTrue(staleHq.contains("\"version\": \"0.51.99\",\n      \"tag\": \"v0.51.99\",\n      \"releaseId\": 103"));
        Path file = temp.resolve("stale-hq.json");
        Files.writeString(file, staleHq);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
              () -> SuiteRecordValidator.validate(file));
        assertTrue(error.getMessage().contains("Bundled product version cannot predate its dependency"));
    }

    @Test
    void acceptsOptionalLauncherVersion() throws IOException {
        String good = Files.readString(COMPLETE);
        Path withoutMinimum = temp.resolve("no-minimum.json");
        Files.writeString(withoutMinimum, good.replace("\"minimumLauncherVersion\": \"0.14.5\",", ""));
        assertDoesNotThrow(() -> SuiteRecordValidator.validate(withoutMinimum));
        Path numeric = temp.resolve("numeric-launcher.json");
        Files.writeString(numeric, good.replace("\"0.14.5\"", "\"1.0.14\""));
        assertDoesNotThrow(() -> SuiteRecordValidator.validate(numeric));
        Path longIds = temp.resolve("long-ids.json");
        Files.writeString(longIds, good.replace("\"releaseId\": 101", "\"releaseId\": 2147483648")
              .replace("\"assetId\": 201", "\"assetId\": 2147483649"));
        assertDoesNotThrow(() -> SuiteRecordValidator.validate(longIds));
    }

    @Test
    void versionComponentsMustFitJavaIntForSuiteAndProducts() throws IOException {
        String good = Files.readString(COMPLETE).replace("\r\n", "\n");
        String[] tooLarge = {"2147483648.51.100", "0.2147483648.100", "0.51.2147483648"};
        String[] largest = {"2147483647.51.100", "0.2147483647.100", "0.51.2147483647"};
        for (int i = 0; i < tooLarge.length; i++) {
            // Keep tags and filenames in sync to isolate the numeric validation.
            String suite = good.replace("0.51.100", tooLarge[i]);
            Path suiteFile = temp.resolve("suite-" + i + ".json");
            Files.writeString(suiteFile, suite);
            IllegalArgumentException suiteError = assertThrows(IllegalArgumentException.class,
                  () -> SuiteRecordValidator.validate(suiteFile));
            assertTrue(suiteError.getMessage().contains("Invalid version:"));

            String product = good.replace("\"version\": \"0.51.99\",\n      \"tag\"",
                  "\"version\": \"" + tooLarge[i] + "\",\n      \"tag\"");
            Path productFile = temp.resolve("product-" + i + ".json");
            Files.writeString(productFile, product);
            IllegalArgumentException productError = assertThrows(IllegalArgumentException.class,
                  () -> SuiteRecordValidator.validate(productFile));
            assertTrue(productError.getMessage().contains(".version:"));

            Path maximumFile = temp.resolve("maximum-" + i + ".json");
            Files.writeString(maximumFile, good.replace("0.51.100", largest[i]));
            assertDoesNotThrow(() -> SuiteRecordValidator.validate(maximumFile));
        }
    }

    @Test
    void rejectsMalformedAndIncompleteRecords() throws IOException {
        String good = Files.readString(COMPLETE);
        String[] bad = {
              good.replace("\"version\": \"0.51.100\"", "\"version\": \"0.51.0100\""),
              good.replace("\"version\": \"0.51.100\"", "\"version\": \"0.51.101\""),
              good.replace("\"version\": \"0.51.100\"", "\"version\": \"0.51.1\""),
              good.replace("\"membership\": \"milestone\"", "\"membership\": \"stable\""),
              good.replace("\"tag\": \"v0.51.100\"", "\"tag\": \"v0.51.99\""),
              good.replace("\"MegaMekLab\": {", "\"Other\": {"),
              good.replace("\"mmData\": {", "\"otherData\": {"),
              good.replace("https://github.com/MegaMek/mekhq", "https://github.com/MegaMek/megamek"),
              good.replace("\"commit\": \"4444444444444444444444444444444444444444\"",
                    "\"commit\": \"short\""),
              good.replace("\"sha256\": \"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"",
                    "\"sha256\": \"not-a-digest\""),
              good.replace("\"size\": 123456", "\"size\": 0"),
              good.replace("\"size\": 123456", "\"size\": 1.5"),
              good.replace("MegaMek-0.51.99.tar.gz", "../MegaMek-0.51.99.tar.gz"),
              good.replace("MegaMek-0.51.99.tar.gz", "MegaMek-0.51.99.txt"),
              good.replace("MegaMek-0.51.99.tar.gz", "MegaMek-0.51.99.tar.gz.bak"),
              good.replace("\"releaseId\": 101,", ""),
              good.replace("\"assetId\": 201,", ""),
              good.replace("\"releaseId\": 101", "\"releaseId\": 0"),
              good.replace("\"releaseId\": 101", "\"releaseId\": -1"),
              good.replace("\"releaseId\": 101", "\"releaseId\": 1.5"),
              good.replace("\"releaseId\": 101", "\"releaseId\": \"101\""),
              good.replace("\"releaseId\": 101", "\"releaseId\": 9223372036854775808"),
              good.replace("\"assetId\": 201", "\"assetId\": 0"),
              good.replace("\"assetId\": 201", "\"assetId\": 1.5"),
              good.replace("\"assetId\": 201", "\"assetId\": \"201\""),
              good.replace("\"assetId\": 201", "\"assetId\": 9223372036854775808"),
              good.replace("\"schemaVersion\": 1,", "\"schemaVersion\": 1, \"schemaVersion\": 1,"),
              good.replace("\"schemaVersion\": 1,", "\"schemaVersion\": 1, \"surprise\": true,"),
              good.replace("\"minimumLauncherVersion\": \"0.14.5\"", "\"minimumLauncherVersion\": \"0.14.05\""),
              good.replace("\"minimumLauncherVersion\": \"0.14.5\"", "\"minimumLauncherVersion\": \"0.014.5\""),
              good.replace("\"minimumLauncherVersion\": \"0.14.5\"", "\"minimumLauncherVersion\": \"0.14.5-beta\""),
              good.replace("\"minimumLauncherVersion\": \"0.14.5\"", "\"minimumLauncherVersion\": \"0.14.5.1\""),
              good.replace("\"minimumLauncherVersion\": \"0.14.5\"", "\"minimumLauncherVersion\": \"01.14.5\""),
              good.replace("\"minimumLauncherVersion\": \"0.14.5\"", "\"minimumLauncherVersion\": 0.14"),
              good + "{}"
        };
        for (int i = 0; i < bad.length; i++) {
            Path file = temp.resolve(i + ".json");
            Files.writeString(file, bad[i]);
            assertThrows(Exception.class, () -> SuiteRecordValidator.validate(file), "malformed fixture " + i);
        }
    }
}
