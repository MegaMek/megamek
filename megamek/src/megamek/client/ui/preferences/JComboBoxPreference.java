/*
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.codeUtilities.StringUtility;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.ref.WeakReference;

/**
 * JComboBoxPreference monitors the selected index of a JComboBox. It sets the saved index when a
 * dialog is loaded and changes it when it changes.
 *
 * Call preferences.manage(new JComboBoxPreference(JComboBox)) to use this preference, on a
 * JComboBox that has called setName
 */
public class JComboBoxPreference extends PreferenceElement implements ItemListener {
    //region Variable Declarations
    private final WeakReference<JComboBox<?>> weakReference;
    private int selectedIndex;
    //endregion Variable Declarations

    //region Constructors
    public JComboBoxPreference(final JComboBox<?> comboBox) throws Exception {
        super(comboBox.getName());
        setSelectedIndex(comboBox.getSelectedIndex());
        weakReference = new WeakReference<>(comboBox);
        comboBox.addItemListener(this);
    }
    //endregion Constructors

    //region Getters/Setters
    public WeakReference<JComboBox<?>> getWeakReference() {
        return weakReference;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(final int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }
    //endregion Getters/Setters

    //region PreferenceElement
    @Override
    protected String getValue() {
        return Integer.toString(getSelectedIndex());
    }

    @Override
    protected void initialize(final String value) throws Exception {
        if (StringUtility.isNullOrBlank(value)) {
            LogManager.getLogger().error("Cannot create a JComboBoxPreference because of a null or blank input value");
            throw new Exception();
        }

        final JComboBox<?> element = getWeakReference().get();
        if (element != null) {
            final int index = Integer.parseInt(value);
            if ((index >= 0) && (index < element.getItemCount())) {
                setSelectedIndex(index);
                element.setSelectedIndex(getSelectedIndex());
            }
        }
    }

    @Override
    protected void dispose() {
        final JComboBox<?> element = getWeakReference().get();
        if (element != null) {
            element.removeItemListener(this);
            getWeakReference().clear();
        }
    }
    //endregion PreferenceElement

    //region ItemListener
    @Override
    public void itemStateChanged(final ItemEvent evt) {
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            final JComboBox<?> element = getWeakReference().get();
            if (element != null) {
                setSelectedIndex(element.getSelectedIndex());
            }
        }
    }
    //endregion ItemListener
}
