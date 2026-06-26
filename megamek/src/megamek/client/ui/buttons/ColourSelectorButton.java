/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.buttons;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;

import megamek.common.annotations.Nullable;

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
