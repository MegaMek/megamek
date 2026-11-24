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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    void acceptsBothChannels() throws IOException {
        Path experimental = temp.resolve("experimental.json");
        Files.writeString(experimental, Files.readString(COMPLETE)
              .replace("\"channel\": \"core\"", "\"channel\": \"experimental\""));
        assertDoesNotThrow(() -> SuiteRecordValidator.validate(experimental));
    }

    @Test
    void productsShareTheSuiteVersionAndTag() throws IOException {
        String good = Files.readString(COMPLETE).replace("\r\n", "\n");
        String hqOnly = "\"version\": \"0.51.02.7\",\n      \"tag\": \"v0.51.02.7\",\n      \"releaseId\": 103";
        assertTrue(good.contains(hqOnly));
        String[] bad = {
              good.replace(hqOnly, hqOnly.replace("0.51.02.7", "0.51.02.6"))
                    .replace("MekHQ-0.51.02.7.tar.gz", "MekHQ-0.51.02.6.tar.gz"),
              good.replace(hqOnly, hqOnly.replace("\"tag\": \"v0.51.02.7\"", "\"tag\": \"v0.51.02.6\"")),
              good.replace("MegaMek-0.51.02.7.tar.gz", "MegaMek-0.51.02.6.tar.gz")
        };
        for (int i = 0; i < bad.length; i++) {
            Path file = temp.resolve("mixed-" + i + ".json");
            Files.writeString(file, bad[i]);
            assertThrows(IllegalArgumentException.class, () -> SuiteRecordValidator.validate(file));
        }
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
    void versionsAlwaysHaveAnUnpaddedBuildNumber() throws IOException {
        String good = Files.readString(COMPLETE);
        Path large = temp.resolve("large.json");
        Files.writeString(large, good.replace("0.51.02.7", "0.51.102.123"));
        assertDoesNotThrow(() -> SuiteRecordValidator.validate(large));
        String[] bad = {"0.51.02", "0.51.02.0", "0.51.02.07", "0.51.02.7.1", "0.51.2.7", "0.051.02.7"};
        for (int i = 0; i < bad.length; i++) {
            Path file = temp.resolve("bad-version-" + i + ".json");
            Files.writeString(file, good.replace("0.51.02.7", bad[i]));
            assertThrows(IllegalArgumentException.class, () -> SuiteRecordValidator.validate(file), bad[i]);
        }
    }

    @Test
    void versionComponentsMustFitJavaInt() throws IOException {
        String good = Files.readString(COMPLETE);
        String[] tooLarge = {"2147483648.51.02.7", "0.2147483648.02.7", "0.51.2147483648.7", "0.51.02.2147483648"};
        String[] largest = {"2147483647.51.02.7", "0.2147483647.02.7", "0.51.2147483647.7", "0.51.02.2147483647"};
        for (int i = 0; i < tooLarge.length; i++) {
            // Keep tags and filenames in sync to isolate the numeric validation.
            Path suiteFile = temp.resolve("suite-" + i + ".json");
            Files.writeString(suiteFile, good.replace("0.51.02.7", tooLarge[i]));
            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                  () -> SuiteRecordValidator.validate(suiteFile));
            assertTrue(error.getMessage().contains("Invalid version:"));

            Path maximumFile = temp.resolve("maximum-" + i + ".json");
            Files.writeString(maximumFile, good.replace("0.51.02.7", largest[i]));
            assertDoesNotThrow(() -> SuiteRecordValidator.validate(maximumFile));
        }
    }

    @Test
    void rejectsMalformedAndIncompleteRecords() throws IOException {
        String good = Files.readString(COMPLETE).replace("\r\n", "\n");
        String[] bad = {
              good.replace("\"schemaVersion\": 2", "\"schemaVersion\": 1"),
              good.replace("\"channel\": \"core\"", "\"channel\": \"weekly\""),
              good.replace("\"channel\": \"core\"", "\"channel\": \"Core\""),
              good.replace("\"channel\": \"core\",", ""),
              good.replace("\"channel\": \"core\"", "\"membership\": \"milestone\""),
              good.replace("\"tag\": \"v0.51.02.7\",\n  \"minimum", "\"tag\": \"v0.51.02.6\",\n  \"minimum"),
              good.replaceFirst("\"tag\": \"v0.51.02.7\"", "\"tag\": \"0.51.02.7\""),
              good.replace("\"MegaMekLab\": {", "\"Other\": {"),
              good.replace("\"mmData\": {", "\"otherData\": {"),
              good.replace("https://github.com/MegaMek/mekhq", "https://github.com/MegaMek/megamek"),
              good.replace("\"commit\": \"4444444444444444444444444444444444444444\"",
                    "\"commit\": \"short\""),
              good.replace("\"sha256\": \"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\"",
                    "\"sha256\": \"not-a-digest\""),
              good.replace("\"size\": 123456", "\"size\": 0"),
              good.replace("\"size\": 123456", "\"size\": 1.5"),
              good.replace("MegaMek-0.51.02.7.tar.gz", "../MegaMek-0.51.02.7.tar.gz"),
              good.replace("MegaMek-0.51.02.7.tar.gz", "MegaMek-0.51.02.7.txt"),
              good.replace("MegaMek-0.51.02.7.tar.gz", "MegaMek-0.51.02.7.tar.gz.bak"),
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
              good.replace("\"schemaVersion\": 2,", "\"schemaVersion\": 2, \"schemaVersion\": 2,"),
              good.replace("\"schemaVersion\": 2,", "\"schemaVersion\": 2, \"surprise\": true,"),
              good.replace("\"minimumLauncherVersion\": \"0.14.5\"", "\"minimumLauncherVersion\": \"0.14.05\""),
              good.replace("\"minimumLauncherVersion\": \"0.14.5\"", "\"minimumLauncherVersion\": \"0.014.5\""),
              good.replace("\"minimumLauncherVersion\": \"0.14.5\"", "\"minimumLauncherVersion\": \"0.14.5-beta\""),
              good.replace("\"minimumLauncherVersion\": \"0.14.5\"", "\"minimumLauncherVersion\": \"0.14.5.1\""),
              good.replace("\"minimumLauncherVersion\": \"0.14.5\"", "\"minimumLauncherVersion\": \"01.14.5\""),
              good.replace("\"minimumLauncherVersion\": \"0.14.5\"", "\"minimumLauncherVersion\": 0.14"),
              good + "{}"
        };
        for (int i = 0; i < bad.length; i++) {
            assertNotEquals(good, bad[i], "fixture " + i + " must differ from the good record");
            Path file = temp.resolve(i + ".json");
            Files.writeString(file, bad[i]);
            assertThrows(Exception.class, () -> SuiteRecordValidator.validate(file), "malformed fixture " + i);
        }
    }
}
