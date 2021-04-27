/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.swing;

import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

@SuppressWarnings("serial") // Same-version serialization only (See Swing base classes)
public class ColourSelectorButton extends JButton {
    //region Variable Declarations
    private Color colour;
    //endregion Variable Declarations

    //region Constructors
    public ColourSelectorButton(String text) {
        this(null, text);
    }

    public ColourSelectorButton(@Nullable Color colour, String text) {
        initialize(colour, text);
    }
    //endregion Constructors

    //region Initialization
    private void initialize(@Nullable Color colour, String text) {
        setText(text);
        setColour(colour);

        addActionListener(evt -> setColour(JColorChooser.showDialog(this,
                "Choose Color", getColour())));
    }
    //endregion Initialization

    //region Getters/Setters
    public Color getColour() {
        return colour;
    }

    public void setColour(@Nullable Color colour) {
        if (colour != null) {
            this.colour = colour;
            setIcon(getColourIcon());
        }
    }
    //endregion Getters/Setters

    private ImageIcon getColourIcon() {
        BufferedImage result = new BufferedImage(36, 36, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setColor(getColour());
        graphics.fillRect(0, 0, 84, 72);
        return new ImageIcon(result);
    }
}
