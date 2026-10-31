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
package megamek.client.ui.entityreadout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.StringReader;
import javax.swing.text.AttributeSet;
import javax.swing.text.ElementIterator;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.junit.jupiter.api.Test;

class HyperLinkLineTest {

    /** A real store link from mm-data (Lethal Lessons), with {@code &} in its query string. */
    private static final String STORE_URL =
          "https://store.catalystgamelabs.com/products/battletech-lethal-lessons?_pos=1&_sid=b9bf90640&_ss=r";

    @Test
    void hrefIsQuotedAndEscaped() {
        assertEquals("HREF=\"https://example.com/a?x=1&amp;y=&quot;2&quot;\"",
              HyperLinkLine.hrefAttribute("https://example.com/a?x=1&y=\"2\""));
    }

    @Test
    void linkWithAmpersandReadsBackAsTheSameAddress() throws Exception {
        String html = new HyperLinkLine("Source", STORE_URL, "Lethal Lessons").toHTML();

        assertEquals(STORE_URL, firstLinkTarget(html));
    }

    @Test
    void linkWithQuoteCannotAddAnAttribute() throws Exception {
        String address = "https://example.com/\" onclick=\"bad";
        String html = new HyperLinkLine("Source", address, "Book").toHTML();

        assertEquals(address, firstLinkTarget(html));
        assertFalse(html.contains("onclick=\"bad"), html);
    }

    /** Parses the HTML the way the readout's Swing pane does and returns the first link's target. */
    private static String firstLinkTarget(String html) throws Exception {
        HTMLEditorKit kit = new HTMLEditorKit();
        HTMLDocument document = (HTMLDocument) kit.createDefaultDocument();
        kit.read(new StringReader("<html><body>" + html + "</body></html>"), document, 0);
        ElementIterator iterator = new ElementIterator(document);
        for (var element = iterator.first(); element != null; element = iterator.next()) {
            Object anchor = element.getAttributes().getAttribute(HTML.Tag.A);
            if (anchor instanceof AttributeSet anchorAttributes) {
                return (String) anchorAttributes.getAttribute(HTML.Attribute.HREF);
            }
        }
        return null;
    }
}
