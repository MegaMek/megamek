/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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

import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

import megamek.common.annotations.Nullable;

/**
 * MMButton is an extension of JButton that overrides the constructors for JButton, albeit with the addition of the name
 * of the button as the first value for all constructors, and adds two new constructors to create standardized JButtons
 * with all the required information.
 * <p>
 * Note: All constructors contain the name of the JButton in question. This MUST be provided and means that they don't
 * directly line up to the traditional JButton constructors.
 */
public class MMButton extends JButton {
    //region Constructors

    /**
     * Creates a JButton with the provided name
     *
     * @see JButton#JButton()
     */
    public MMButton(final String name) {
        super();
        setName(name);
    }

    /**
     * Creates a JButton with the provided name and icon
     *
     * @see JButton#JButton(Icon)
     */
    public MMButton(final String name, final Icon icon) {
        super(icon);
        setName(name);
    }

    /**
     * Creates a JButton with the provided text and name, localized using the provided resource bundle
     *
     * @see JButton#JButton(String)
     */
    public MMButton(final String name, final ResourceBundle resources, final String text) {
        this(name, resources.getString(text));
    }

    /**
     * Creates a JButton with the provided text and name
     *
     * @see JButton#JButton(String)
     */
    public MMButton(final String name, final String text) {
        super(text);
        setName(name);
    }

    /**
     * Creates a JButton with the provided name and action
     *
     * @see JButton#JButton(Action)
     */
    public MMButton(final String name, final Action action) {
        super(action);
        setName(name);
    }

    /**
     * Creates a JButton with the provided text, name, and icon, localized using the provided resource bundle
     *
     * @see JButton#JButton(String)
     */
    public MMButton(final String name, final ResourceBundle resources, final String text, final Icon icon) {
        this(name, resources.getString(text), icon);
    }

    /**
     * creates a JButton with the provided text, name, and icon
     *
     * @see JButton#JButton(String, Icon)
     */
    public MMButton(final String name, final String text, final Icon icon) {
        super(text, icon);
        setName(name);
    }

    /**
     * Creates a JButton with the provided text, name, and action listener, localized using the provided resource
     * bundle
     *
     * @see JButton#JButton(String)
     */
    public MMButton(final String name, final ResourceBundle resources, final String text,
          final ActionListener actionListener) {
        this(name, resources.getString(text), actionListener);
    }

    /**
     * This creates a JButton without any toolTipText.
     *
     * @param name           the name of the button
     * @param text           the localized text string
     * @param actionListener the {@link ActionListener} to assign to the button
     */
    public MMButton(final String name, final String text, final ActionListener actionListener) {
        this(name, text, null, actionListener);
    }

    /**
     * Creates a JButton with the provided text, toolTipText, name, and action listener, localized using the provided
     * resource bundle
     *
     * @see JButton#JButton(String)
     */
    public MMButton(final String name, final ResourceBundle resources, final String text,
          final @Nullable String toolTipText, final ActionListener actionListener) {
        this(name, resources.getString(text), (toolTipText == null) ? null : resources.getString(toolTipText),
              actionListener);
    }

    /**
     * This creates a standard JButton for use in MegaMek.
     *
     * @param name           the name of the button
     * @param text           the localized text string
     * @param toolTipText    the localized toolTipText string, or null if there is no tool tip (not recommended)
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
