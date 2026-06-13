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
package megamek.client.ui.comboBoxes;

import java.util.List;
import java.util.Vector;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import megamek.common.annotations.Nullable;

/**
 * MMBuMMComboBox is an extension of JComboBox that overrides the constructors for JComboBox, albeit with the addition
 * of the name of the button as the first value for all constructors and a nicer override of getSelectedItem so that it
 * returns in the proper class.
 * <p>
 * Note: All constructors contain the name of the JComboBox in question. This MUST be provided and means that they don't
 * directly line up to the traditional JComboBox constructors.
 */
public class MMComboBox<E> extends JComboBox<E> {
    //region Constructors

    /**
     * Creates a JComboBox with the provided name and values
     *
     * @see JComboBox#JComboBox(ComboBoxModel)
     */
    public MMComboBox(final String name, final ComboBoxModel<E> model) {
        super(model);
        setName(name);
    }

    /**
     * Creates a JComboBox with the provided name and values
     */
    public MMComboBox(final String name, final E[] items) {
        super(items);
        setName(name);
    }

    /**
     * Creates a JComboBox with the provided name and values
     *
     * @see JComboBox#JComboBox(Vector)
     */
    public MMComboBox(final String name, final Vector<E> items) {
        super(items);
        setName(name);
    }

    /**
     * Creates a JComboBox with the provided name and values
     *
     * @param name  the JComboBox's name
     * @param items the list of items to include in the model
     */
    public MMComboBox(final String name, final List<E> items) {
        this(name);
        final DefaultComboBoxModel<E> model = new DefaultComboBoxModel<>();
        model.addAll(items);
        setModel(model);
    }

    /**
     * Creates a JComboBox with the provided name and values
     *
     * @see JComboBox#JComboBox()
     */
    public MMComboBox(final String name) {
        super();
        setName(name);
    }
    //endregion Constructors

    /**
     * @return the selected item, cast to the proper class stored by the combo box
     */
    @Override
    @SuppressWarnings(value = "unchecked")
    public @Nullable E getSelectedItem() {
        final Object item = super.getSelectedItem();
        return (item == null) ? null : (E) item;
    }
}
