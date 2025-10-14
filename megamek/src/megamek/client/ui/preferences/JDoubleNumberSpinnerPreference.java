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
package megamek.client.ui.preferences;

import java.lang.ref.WeakReference;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import megamek.codeUtilities.StringUtility;
import megamek.logging.MMLogger;

/**
 * JDoubleNumberSpinnerPreference monitors the value of a JSpinner whose number model is for a double. It sets the saved
 * value when a dialog is loaded and changes it as it changes.
 * <p>
 * Call preferences.manage(new JDoubleNumberSpinnerPreference(JSpinner)) to use this preference, on a JSpinner with a
 * double valued SpinnerNumberModel that has called setName
 */
public class JDoubleNumberSpinnerPreference extends PreferenceElement implements ChangeListener {
    private final static MMLogger logger = MMLogger.create(JDoubleNumberSpinnerPreference.class);

    // region Variable Declarations
    private final WeakReference<JSpinner> weakReference;
    private double doubleValue;
    // endregion Variable Declarations

    // region Constructors
    public JDoubleNumberSpinnerPreference(final JSpinner spinner) throws Exception {
        super(spinner.getName());
        if (!(spinner.getModel() instanceof SpinnerNumberModel)) {
            throw new Exception("Cannot create a double spinner without using a number model");
        }
        setDoubleValue((Double) spinner.getValue());
        weakReference = new WeakReference<>(spinner);
        spinner.addChangeListener(this);
    }
    // endregion Constructors

    // region Getters/Setters
    public WeakReference<JSpinner> getWeakReference() {
        return weakReference;
    }

    public double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(final double doubleValue) {
        this.doubleValue = doubleValue;
    }
    // endregion Getters/Setters

    // region PreferenceElement
    @Override
    protected String getValue() {
        return Double.toString(getDoubleValue());
    }

    @Override
    protected void initialize(final String value) throws Exception {
        if (StringUtility.isNullOrBlank(value)) {
            logger
                  .error("Cannot create a JDoubleNumberSpinnerPreference because of a null or blank input value");
            throw new Exception();
        }

        final JSpinner element = getWeakReference().get();
        if (element != null) {
            final double newValue = Double.parseDouble(value);
            final SpinnerNumberModel model = (SpinnerNumberModel) element.getModel();
            if (((Double) model.getMinimum() <= newValue) && ((Double) model.getMaximum() >= newValue)) {
                setDoubleValue(newValue);
                element.setValue(getDoubleValue());
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
    // endregion PreferenceElement

    // region ChangeListener
    @Override
    public void stateChanged(final ChangeEvent evt) {
        final JSpinner element = getWeakReference().get();
        if (element != null) {
            setDoubleValue((Double) element.getValue());
        }
    }
    // endregion ChangeListener
}
