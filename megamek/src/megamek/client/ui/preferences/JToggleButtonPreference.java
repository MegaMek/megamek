/*
 * Copyright (c) 2019-2021 - The MegaMek Team. All Rights Reserved.
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.lang.ref.WeakReference;

/**
 * JTextFieldPreference monitors the selected state of a JToggleButton (which JCheckbox extends).
 * It sets the saved value when a dialog is loaded and changes it as it changes.
 *
 * Call preferences.manage(new JToggleButtonPreference(JToggleButton)) to use this preference, on a
 * JToggleButton that has called setName
 */
public class JToggleButtonPreference extends PreferenceElement implements ChangeListener {
    //region Variable Declarations
    private final WeakReference<JToggleButton> weakReference;
    private boolean selected;
    //endregion Variable Declarations

    //region Constructors
    public JToggleButtonPreference(final JToggleButton toggleButton) throws Exception {
        super(toggleButton.getName());
        setSelected(toggleButton.isSelected());
        weakReference = new WeakReference<>(toggleButton);
        toggleButton.addChangeListener(this);
    }
    //endregion Constructors

    //region Getters/Setters
    public WeakReference<JToggleButton> getWeakReference() {
        return weakReference;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(final boolean selected) {
        this.selected = selected;
    }
    //endregion Getters/Setters

    //region PreferenceElement
    @Override
    protected String getValue() {
        return Boolean.toString(isSelected());
    }

    @Override
    protected void initialize(final String value) throws Exception {
        if (StringUtility.isNullOrBlank(value)) {
            LogManager.getLogger().error("Cannot create a JToggleButtonPreference because of a null or blank input value");
            throw new Exception();
        }

        final JToggleButton element = getWeakReference().get();
        if (element != null) {
            setSelected(Boolean.parseBoolean(value));
            if (element.isSelected() != isSelected()) {
                element.doClick();
            }
        }
    }

    @Override
    protected void dispose() {
        final JToggleButton element = getWeakReference().get();
        if (element != null) {
            element.removeChangeListener(this);
            getWeakReference().clear();
        }
    }
    //endregion PreferenceElement

    //region ChangeListener
    @Override
    public void stateChanged(final ChangeEvent evt) {
        final JToggleButton element = getWeakReference().get();
        if (element != null) {
            setSelected(element.isSelected());
        }
    }
    //endregion ChangeListener
}
