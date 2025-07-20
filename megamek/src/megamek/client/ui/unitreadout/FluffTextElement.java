/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.unitreadout;

/**
 * This Element is a LabeledElement that emphasizes its label rather than its data. To be used for potentially longer
 * texts.
 */
class FluffTextElement extends LabeledElement {

    FluffTextElement(String label, String value) {
        super(label, value);
    }

    @Override
    public String toHTML() {
        return "<B>%s:</B> %s<BR>".formatted(label, value);
    }
}
