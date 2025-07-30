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

package megamek.client.ui.entityreadout;

class PlainElement implements ViewElement {

    private final String text;

    public PlainElement(String text) {
        this.text = text;
    }

    public PlainElement(int number) {
        text = String.valueOf(number);
    }

    @Override
    public String toPlainText() {
        return text;
    }

    @Override
    public String toHTML() {
        return text;
    }

    @Override
    public String toDiscord() {
        return text;
    }
}
