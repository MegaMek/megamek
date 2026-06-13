/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import megamek.codeUtilities.StringUtility;
import megamek.logging.MMLogger;

/**
 * JTextFieldPreference monitors the selected state of a JToggleButton (which JCheckbox extends). It sets the saved
 * value when a dialog is loaded and changes it as it changes.
 * <p>
 * Call preferences.manage(new JToggleButtonPreference(JToggleButton)) to use this preference, on a JToggleButton that
 * has called setName
 */
public class JToggleButtonPreference extends PreferenceElement implements ChangeListener {
    private final static MMLogger logger = MMLogger.create(JToggleButtonPreference.class);

    // region Variable Declarations
    private final WeakReference<JToggleButton> weakReference;
    private boolean selected;
    // endregion Variable Declarations

    // region Constructors
    public JToggleButtonPreference(final JToggleButton toggleButton) throws Exception {
        super(toggleButton.getName());
        setSelected(toggleButton.isSelected());
        weakReference = new WeakReference<>(toggleButton);
        toggleButton.addChangeListener(this);
    }
    // endregion Constructors

    // region Getters/Setters
    public WeakReference<JToggleButton> getWeakReference() {
        return weakReference;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(final boolean selected) {
        this.selected = selected;
    }
    // endregion Getters/Setters

    // region PreferenceElement
    @Override
    protected String getValue() {
        return Boolean.toString(isSelected());
    }

    @Override
    protected void initialize(final String value) throws Exception {
        if (StringUtility.isNullOrBlank(value)) {
            logger.error("Cannot create a JToggleButtonPreference because of a null or blank input value");
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
    // endregion PreferenceElement

    // region ChangeListener
    @Override
    public void stateChanged(final ChangeEvent evt) {
        final JToggleButton element = getWeakReference().get();
        if (element != null) {
            setSelected(element.isSelected());
        }
    }
    // endregion ChangeListener
}
