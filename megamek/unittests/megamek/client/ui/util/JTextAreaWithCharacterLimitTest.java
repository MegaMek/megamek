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
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.junit.jupiter.api.Test;

class JTextAreaWithCharacterLimitTest {

    @Test
    void createLimitedTextArea_withNonPositiveMaxChars_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
              () -> JTextAreaWithCharacterLimit.createLimitedTextArea(0, 10));
        assertThrows(IllegalArgumentException.class,
              () -> JTextAreaWithCharacterLimit.createLimitedTextArea(-5, 10));
    }

    @Test
    void insertWithinLimit_insertsAllCharacters() throws Exception {
        int maxChars = 10;
        JTextArea area = JTextAreaWithCharacterLimit.createLimitedTextArea(maxChars, 10);
        Document doc = area.getDocument();

        doc.insertString(0, "12345", null);

        assertEquals("12345", getText(doc));
        assertEquals(5, doc.getLength());
    }

    @Test
    void insertExceedingLimit_truncatesToMaxCharacters() throws Exception {
        int maxChars = 10;
        JTextArea area = JTextAreaWithCharacterLimit.createLimitedTextArea(maxChars, 10);
        Document doc = area.getDocument();

        String input = "0123456789ABCDEF"; // 16 chars
        doc.insertString(0, input, null);

        String text = getText(doc);
        assertEquals(maxChars, text.length());
        assertEquals(input.substring(0, maxChars), text);
    }

    @Test
    void secondInsertExceedingRemainingSpace_truncatesToAvailableSpace() throws Exception {
        int maxChars = 10;
        JTextArea area = JTextAreaWithCharacterLimit.createLimitedTextArea(maxChars, 10);
        Document doc = area.getDocument();

        doc.insertString(0, "1234567", null); // 7 chars, 3 remaining
        doc.insertString(doc.getLength(), "890123", null); // 6 chars requested, 3 allowed

        String text = getText(doc);
        assertEquals(maxChars, text.length());
        assertEquals("1234567890", text);
    }

    @Test
    void insertWhenAtCapacity_insertsNothing() throws Exception {
        int maxChars = 5;
        JTextArea area = JTextAreaWithCharacterLimit.createLimitedTextArea(maxChars, 10);
        Document doc = area.getDocument();

        doc.insertString(0, "12345", null); // at capacity
        doc.insertString(doc.getLength(), "678", null); // should be ignored

        String text = getText(doc);
        assertEquals(maxChars, text.length());
        assertEquals("12345", text);
    }

    @Test
    void insertNullString_doesNothing() throws Exception {
        int maxChars = 10;
        JTextArea area = JTextAreaWithCharacterLimit.createLimitedTextArea(maxChars, 10);
        Document doc = area.getDocument();

        doc.insertString(0, "abc", null);
        doc.insertString(1, null, null); // should no-op

        String text = getText(doc);
        assertEquals("abc", text);
    }

    @Test
    void insertEmptyString_doesNothing() throws Exception {
        int maxChars = 10;
        JTextArea area = JTextAreaWithCharacterLimit.createLimitedTextArea(maxChars, 10);
        Document doc = area.getDocument();

        doc.insertString(0, "abc", null);
        doc.insertString(1, "", null); // should no-op

        String text = getText(doc);
        assertEquals("abc", text);
        assertEquals(3, doc.getLength());
    }

    private static String getText(Document document) throws BadLocationException {
        return document.getText(0, document.getLength());
    }
}

