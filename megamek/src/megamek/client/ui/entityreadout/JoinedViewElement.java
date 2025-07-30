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
package megamek.client.ui.entityreadout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This ViewElement allows assembling multiple ViewElements into a single one to use, e.g., as an element in a table.
 * For output, the contained elements are chained without anything else added.
 */
class JoinedViewElement implements ViewElement {

    private final List<ViewElement> values = new ArrayList<>();

    /**
     * Constructs a new joined element with the given elements as content.
     *
     * @param values The view elements to chain
     */
    JoinedViewElement(ViewElement... values) {
        if (values != null) {
            this.values.addAll(Arrays.stream(values).toList());
        }
    }

    /**
     * Constructs a new joined element with the given strings as content. Internally the strings are converted to
     * PlainElements.
     *
     * @param values The view elements to chain
     */
    JoinedViewElement(String... values) {
        if (values != null) {
            for (String value : values) {
                add(value);
            }
        }
    }

    /**
     * Constructs a new empty joined element.
     */
    JoinedViewElement() {
        // Nothing to do
    }

    /**
     * Appends the given element to the end of this joined element. Calls can be chained.
     *
     * @param value The view element to append.
     *
     * @return this joined element
     */
    public JoinedViewElement add(ViewElement value) {
        values.add(value);
        return this;
    }

    /**
     * Appends the given string as a PlainElement to the end of this joined element. Calls can be chained.
     *
     * @param value The String to append
     *
     * @return this joined element
     */
    public JoinedViewElement add(String value) {
        values.add(new PlainElement(value));
        return this;
    }

    /**
     * Appends the given int as a PlainElement to the end of this joined element. Calls can be chained.
     *
     * @param value The int number to append (uses String.valueof)
     *
     * @return this joined element
     */
    public JoinedViewElement add(int value) {
        values.add(new PlainElement(value));
        return this;
    }

    @Override
    public String toPlainText() {
        return values.stream().map(ViewElement::toPlainText).collect(Collectors.joining());
    }

    @Override
    public String toHTML() {
        return values.stream().map(ViewElement::toHTML).collect(Collectors.joining());
    }

    @Override
    public String toDiscord() {
        return values.stream().map(ViewElement::toDiscord).collect(Collectors.joining());
    }
}
