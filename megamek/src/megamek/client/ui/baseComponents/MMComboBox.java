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
import java.util.List;
import java.util.Vector;

/**
 * MMBuMMComboBox is an extension of JComboBox that overrides the constructors for JComboBox,
 * albeit with the addition of the name of the button as the first value for all constructors and
 * a nicer override of getSelectedItem so that it returns in the proper class.
 *
 * Note: All constructors contain the name of the JComboBox in question. This MUST be provided and
 * means that they don't directly line up to the traditional JComboBox constructors.
 */
public class MMComboBox<E> extends JComboBox<E> {
    //region Constructors
    /**
     * Creates a JComboBox with the provided name and values
     * @see JComboBox#JComboBox(ComboBoxModel)
     */
    public MMComboBox(final String name, final ComboBoxModel<E> model) {
        super(model);
        setName(name);
    }

    /**
     * Creates a JComboBox with the provided name and values
     * @see JComboBox#JComboBox(E[])
     */
    public MMComboBox(final String name, final E[] items) {
        super(items);
        setName(name);
    }

    /**
     * Creates a JComboBox with the provided name and values
     * @see JComboBox#JComboBox(Vector)
     */
    public MMComboBox(final String name, final Vector<E> items) {
        super(items);
        setName(name);
    }

    /**
     * Creates a JComboBox with the provided name and values
     *
     * @param name the JComboBox's name
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
