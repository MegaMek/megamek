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

import javax.swing.JTextArea;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

/**
 * Utility class for creating {@link JTextArea} instances with a maximum character limit.
 *
 * <p>This class provides a document implementation that enforces a character limit and a static method to quickly
 * create restricted {@code JTextArea} components.</p>
 *
 * @author Illiani
 * @since 0.50.11
 */
public class JTextAreaWithCharacterLimit {
    /**
     * Creates a single-line {@link JTextArea} with a maximum number of characters.
     *
     * <p>The returned text area is configured to line wrap, and wrap words.</p>
     *
     * @param maxChars the maximum number of characters that can be entered in the text area
     * @param columns  the number of columns to use to calculate the preferred width
     *
     * @return a configured {@link JTextArea} with the applied character limit
     *
     * @author Illiani
     * @since 0.50.11
     */
    public static JTextArea createLimitedTextArea(int maxChars, int columns) {
        if (maxChars <= 0) {
            throw new IllegalArgumentException("maxChars must be positive");
        }

        JTextArea area = new JTextArea(1, columns);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setDocument(new LimitedDocument(maxChars));

        return area;
    }

    /**
     * A document implementation that enforces a maximum character limit for any text component (such as
     * {@link JTextArea}) that uses it.
     *
     * @author Illiani
     * @since 0.50.11
     */
    private static class LimitedDocument extends PlainDocument {
        private final int maxChars;

        /**
         * Constructs a {@link LimitedDocument} with the specified maximum character count.
         *
         * @param maxChars the maximum number of characters allowed in this document
         *
         * @author Illiani
         * @since 0.50.11
         */
        public LimitedDocument(int maxChars) {
            this.maxChars = maxChars;
        }

        /**
         * Inserts some content into the document, enforcing the character limit.
         *
         * <p>If the content to be inserted exceeds the allowed maximum, only the maximum possible substring is
         * inserted.</p>
         *
         * @param offset       the offset into the document to insert the content &ge; 0
         * @param string       the string to insert; does nothing with {@code null}
         * @param attributeSet the attributes to associate with the inserted content
         *
         * @throws BadLocationException if the given insert position is not a valid position within the document
         * @author Illiani
         * @since 0.50.11
         */
        @Override
        public void insertString(int offset, String string, AttributeSet attributeSet) throws BadLocationException {
            if (string == null) {
                return;
            }
            if ((getLength() + string.length()) <= maxChars) {
                super.insertString(offset, string, attributeSet);
            } else {
                int available = maxChars - getLength();
                if (available > 0) {
                    super.insertString(offset, string.substring(0, available), attributeSet);
                }
            }
        }
    }
}
