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
 * JIntNumberSpinnerPreference monitors the value of a JSpinner whose number model is for an int.
 * It sets the saved value when a dialog is loaded and changes it as it changes.
 *
 * Call preferences.manage(new JIntNumberSpinnerPreference(JSpinner)) to use this preference,
 * on a JSpinner with an int valued SpinnerNumberModel that has called setName
 */
public class JIntNumberSpinnerPreference extends PreferenceElement implements ChangeListener {
    //region Variable Declarations
    private final WeakReference<JSpinner> weakReference;
    private int intValue;
    //endregion Variable Declarations

    //region Constructors
    public JIntNumberSpinnerPreference(final JSpinner spinner) throws Exception {
        super(spinner.getName());
        if (!(spinner.getModel() instanceof SpinnerNumberModel)) {
            throw new Exception("Cannot create an int spinner without using a number model");
        }
        setIntValue((Integer) spinner.getValue());
        weakReference = new WeakReference<>(spinner);
        spinner.addChangeListener(this);
    }
    //endregion Constructors

    //region Getters/Setters
    public WeakReference<JSpinner> getWeakReference() {
        return weakReference;
    }

    public int getIntValue() {
        return intValue;
    }

    public void setIntValue(final int intValue) {
        this.intValue = intValue;
    }
    //endregion Getters/Setters

    //region PreferenceElement
    @Override
    protected String getValue() {
        return Integer.toString(getIntValue());
    }

    @Override
    protected void initialize(final String value) throws Exception {
        if (StringUtility.isNullOrBlank(value)) {
            LogManager.getLogger().error("Cannot create a JIntNumberSpinnerPreference because of a null or blank input value");
            throw new Exception();
        }

        final JSpinner element = getWeakReference().get();
        if (element != null) {
            final int newValue = Integer.parseInt(value);
            final SpinnerNumberModel model = (SpinnerNumberModel) element.getModel();
            if (((Integer) model.getMinimum() <= newValue) && ((Integer) model.getMaximum() >= newValue)) {
                setIntValue(newValue);
                element.setValue(getIntValue());
            }
        }
    }

    @Override
    protected void dispose() {
        final JSpinner element = getWeakReference().get();
        if (element != null) {
            element.removeChangeListener(this);
            getWeakReference().clear();
        }
    }
    //endregion PreferenceElement

    //region ChangeListener
    @Override
    public void stateChanged(final ChangeEvent evt) {
        final JSpinner element = getWeakReference().get();
        if (element != null) {
            setIntValue((Integer) element.getValue());
        }
    }
    //endregion ChangeListener
}
