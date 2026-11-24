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

package megamek.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceBooksTest {

    @Test
    void unknownSourcebookIsNonCanon(@TempDir Path sourcebookDirectory) {
        SourceBooks sourceBooks = new SourceBooks(sourcebookDirectory.toString());

        assertTrue(sourceBooks.isNonCanonBySource("Definitely Missing Sourcebook", ""));
    }

    @Test
    void emptySourcebookListsAreNonCanon(@TempDir Path sourcebookDirectory) {
        SourceBooks sourceBooks = new SourceBooks(sourcebookDirectory.toString());

        assertTrue(sourceBooks.isNonCanonBySource(null, null));
        assertTrue(sourceBooks.isNonCanonBySource("", ""));
        assertTrue(sourceBooks.isNonCanonBySource("  ", " , "));
    }

    @Test
    void mixedCanonAndNonCanonSourcebooksAreCanon(@TempDir Path sourcebookDirectory) throws IOException {
        SourceBooks sourceBooks = new SourceBooks(sourcebookDirectory.toString());
        SourceBook canonSourceBook = new SourceBook();
        canonSourceBook.setAbbrev("CANON");
        canonSourceBook.setTitle("Canon Sourcebook");
        canonSourceBook.setCanon(true);
        sourceBooks.saveSourceBook("CANON", canonSourceBook);

        SourceBook nonCanonSourceBook = new SourceBook();
        nonCanonSourceBook.setAbbrev("NONCANON");
        nonCanonSourceBook.setTitle("Non-Canon Sourcebook");
        nonCanonSourceBook.setCanon(false);
        sourceBooks.saveSourceBook("NONCANON", nonCanonSourceBook);

        assertFalse(sourceBooks.isNonCanonBySource("CANON,NONCANON", ""));
        assertFalse(sourceBooks.isNonCanonBySource("NONCANON", "CANON"));
        assertTrue(sourceBooks.isNonCanonBySource("NONCANON", "Definitely Missing Sourcebook"));
    }

    @Test
    void saveSourceBookInvalidatesNonCanonCache(@TempDir Path sourcebookDirectory) throws IOException {
        SourceBooks sourceBooks = new SourceBooks(sourcebookDirectory.toString());
        SourceBook sourceBook = new SourceBook();
        sourceBook.setAbbrev("TEST");
        sourceBook.setTitle("Test Sourcebook");
        sourceBook.setCanon(true);
        sourceBooks.saveSourceBook("TEST", sourceBook);

        assertFalse(sourceBooks.isNonCanonBySource("TEST", ""));

        sourceBook.setCanon(false);
        sourceBooks.saveSourceBook("TEST", sourceBook);

        assertTrue(sourceBooks.isNonCanonBySource("TEST", ""));
        assertFalse(sourceBooks.loadSourceBook("TEST").orElseThrow().isCanon());
    }

    @Test
    void sourcebookWithUnknownFieldStillLoads(@TempDir Path sourcebookDirectory) throws IOException {
        Files.writeString(sourcebookDirectory.resolve("FUTURE.yaml"), """
              title: Future Sourcebook
              abbrev: FUTURE
              canon: true
              a_field_added_later: 42
              """);
        SourceBooks sourceBooks = new SourceBooks(sourcebookDirectory.toString());

        SourceBook sourceBook = sourceBooks.loadSourceBook("FUTURE").orElseThrow();

        assertEquals("Future Sourcebook", sourceBook.getTitle());
        assertTrue(sourceBook.isCanon());
    }

    @Test
    void publishedDateIsReadFromFile(@TempDir Path sourcebookDirectory) throws IOException {
        Files.writeString(sourcebookDirectory.resolve("DATED.yaml"), """
              title: 'Touring the Stars: Lone Star'
              abbrev: TtS:LoneStar
              published: 2016-02-10
              """);
        SourceBooks sourceBooks = new SourceBooks(sourcebookDirectory.toString());

        assertEquals("2016-02-10", sourceBooks.loadSourceBook("DATED").orElseThrow().getPublished());
    }

    @Test
    void publishedDateSurvivesSaveAndLoad(@TempDir Path sourcebookDirectory) throws IOException {
        SourceBooks sourceBooks = new SourceBooks(sourcebookDirectory.toString());
        SourceBook sourceBook = new SourceBook();
        sourceBook.setTitle("Round Trip Sourcebook");
        sourceBook.setAbbrev("ROUNDTRIP");
        sourceBook.setPublished("1987-11");
        sourceBooks.saveSourceBook("ROUNDTRIP", sourceBook);

        assertEquals("1987-11", sourceBooks.loadSourceBook("ROUNDTRIP").orElseThrow().getPublished());
    }

    @Test
    void linkUrlPrefersShopPage() {
        SourceBook sourceBook = new SourceBook();
        sourceBook.setUrl("https://store.example/book");
        sourceBook.setMul_url("https://mul.example/book");

        assertEquals("https://store.example/book", sourceBook.getLinkUrl());
    }

    @Test
    void linkUrlFallsBackToMulPage() {
        SourceBook sourceBook = new SourceBook();
        sourceBook.setUrl(" ");
        sourceBook.setMul_url("https://mul.example/book");

        assertEquals("https://mul.example/book", sourceBook.getLinkUrl());
    }

    @Test
    void linkUrlIsNullWithoutAnyLink() {
        assertNull(new SourceBook().getLinkUrl());
    }

    @Test
    void linkUrlIsNotWrittenToFile(@TempDir Path sourcebookDirectory) throws IOException {
        SourceBooks sourceBooks = new SourceBooks(sourcebookDirectory.toString());
        SourceBook sourceBook = new SourceBook();
        sourceBook.setTitle("Linked Sourcebook");
        sourceBook.setUrl("https://store.example/book");
        sourceBooks.saveSourceBook("LINKED", sourceBook);

        String saved = Files.readString(sourceBooks.sourceBookFile("LINKED").toPath());

        assertTrue(saved.contains("https://store.example/book"));
        assertFalse(saved.contains("linkUrl"));
    }
}
