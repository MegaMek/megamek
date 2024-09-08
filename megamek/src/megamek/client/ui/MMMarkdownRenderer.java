/*
 * Copyright (c) 2019-2022, 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.client.ui;

import megamek.common.annotations.Nullable;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.Set;

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
     * This method renders markdown-flavored text as HTML.
     *
     * @param input - a String possible containing markdown markup (and html) to be rendered
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
