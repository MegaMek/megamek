/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.baseComponents;

import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * MMButton is an extension of JButton that overrides the constructors for JButton, albeit with the
 * addition of the name of the button as the first value for all constructors, and adds two new
 * constructors to create standardized JButtons with all of the required information.
 *
 * Note: All constructors contain the name of the JButton in question. This MUST be provided and
 * means that they don't directly line up to the traditional JButton constructors.
 */
public class MMButton extends JButton {
    //region Constructors
    /**
     * Creates a JButton with the provided name
     * @see JButton#JButton()
     */
    public MMButton(final String name) {
        super();
        setName(name);
    }

    /**
     * Creates a JButton with the provided name and icon
     * @see JButton#JButton(Icon)
     */
    public MMButton(final String name, final Icon icon) {
        super(icon);
        setName(name);
    }

    /**
     * Creates a JButton with the provided text and name
     * @see JButton#JButton(String)
     */
    public MMButton(final String name, final String text) {
        super(text);
        setName(name);
    }

    /**
     * Creates a JButton with the provided name and action
     * @see JButton#JButton(Action)
     */
    public MMButton(final String name, final Action action) {
        super(action);
        setName(name);
    }

    /**
     * creates a JButton with the provided text, name, and icon
     * @see JButton#JButton(String, Icon)
     */
    public MMButton(final String name, final String text, final Icon icon) {
        super(text, icon);
        setName(name);
    }

    /**
     * This creates a JButton without any toolTipText.
     * @param name the name of the button
     * @param text the localized text string
     * @param actionListener the {@link ActionListener} to assign to the button
     */
    public MMButton(final String name, final String text, final ActionListener actionListener) {
        this(text, null, name, actionListener);
    }

    /**
     * This creates a standard JButton for use in MegaMek.
     * @param name the name of the button
     * @param text the localized text string
     * @param toolTipText the localized toolTipText string, or null if there is no tool tip (not recommended)
     * @param actionListener the {@link ActionListener} to assign to the button
     */
    public MMButton(final String name, final String text, final @Nullable String toolTipText,
                    final ActionListener actionListener) {
        super(text);
        setToolTipText(toolTipText);
        setName(name);
        addActionListener(actionListener);
    }
    //endregion Constructors
}
