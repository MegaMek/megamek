/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui;

import java.util.Set;

import megamek.common.annotations.Nullable;
import org.commonmark.node.Block;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.Heading;
import org.commonmark.node.ListBlock;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/***
 * This is a class with a single static instance that will take markdown flavored text and parse it
 * back out as HTML, using the commonmark-java library. It is intended for allowing users to mix
 * markdown and html elements in various descriptions of units, people, etc.
 * @author aarong
 */
public class MMMarkdownRenderer {
    private static final Set<Class<? extends Block>> USED_BLOCKS =
          Set.of(Heading.class, ListBlock.class, ThematicBreak.class, BlockQuote.class);
    private static final MMMarkdownRenderer RENDERER = new MMMarkdownRenderer();

    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

    private MMMarkdownRenderer() {
        // We don't use this to show code blocks, therefore don't enable those
        parser = Parser.builder().enabledBlockTypes(USED_BLOCKS).build();
        htmlRenderer = HtmlRenderer.builder().build();
    }

    /**
     * This method renders markdown-flavored text as HTML. The result does not include HTML or BODY tags.
     *
     * @param input - a String possible containing Markdown markup (and html) to be rendered
     *
     * @return a string rendered to html
     */
    public static String getRenderedHtml(@Nullable String input) {
        if (null == input) {
            return "";
        } else {
            return RENDERER.htmlRenderer.render(RENDERER.parser.parse(input));
        }
    }
}
