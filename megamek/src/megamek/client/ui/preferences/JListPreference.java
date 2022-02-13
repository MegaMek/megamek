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
package megamek.client.ui.preferences;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;

/**
 * JListPreference monitors the selected indices of a {@link JList}. It sets the saved indices when a
 * dialog is loaded and changes them when they change.
 *
 * Call preferences.manage(new JListPreference(JList)) to use this preference, on a JList that
 * has called setName
 */
public class JListPreference extends PreferenceElement implements PropertyChangeListener {
    //region Variable Declarations
    private final WeakReference<JList<?>> weakReference;
    private int[] selectedIndices;
    //endregion Variable Declarations

    //region Constructors
    public JListPreference(final JList<?> jList) {
        super(jList.getName());
        setSelectedIndices(jList.getSelectedIndices());
        weakReference = new WeakReference<>(jList);
        jList.addPropertyChangeListener(this);
    }
    //endregion Constructors

    //region Getters/Setters
    public WeakReference<JList<?>> getWeakReference() {
        return weakReference;
    }

    public int[] getSelectedIndices() {
        return selectedIndices;
    }

    public void setSelectedIndices(final int... selectedIndices) {
        this.selectedIndices = selectedIndices;
    }
    //endregion Getters/Setters

    //region PreferenceElement
    @Override
    protected String getValue() {
        return StringUtils.join(getSelectedIndices(), '|');
    }

    @Override
    protected void initialize(final String value) {
        assert (value != null) && !value.isBlank();

        final JList<?> element = getWeakReference().get();
        if (element != null) {
            final String[] strings = value.split("\\|");
            final int[] indices = new int[strings.length];
            for (int i = 0; i < strings.length; i++) {
                indices[i] = Integer.parseInt(strings[i]);
            }
            setSelectedIndices(indices);
            element.setSelectedIndices(getSelectedIndices());
        }
    }

    @Override
    protected void dispose() {
        final JList<?> element = getWeakReference().get();
        if (element != null) {
            element.removePropertyChangeListener(this);
            getWeakReference().clear();
        }
    }
    //endregion PreferenceElement

    //region PropertyChangeListener
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        final JList<?> element = getWeakReference().get();
        if (element != null) {
            setSelectedIndices(element.getSelectedIndices());
        }
    }
    //endregion PropertyChangeListener
}
