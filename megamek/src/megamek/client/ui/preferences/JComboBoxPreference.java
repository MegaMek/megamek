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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.ref.WeakReference;
import javax.swing.JComboBox;

import megamek.codeUtilities.StringUtility;
import megamek.logging.MMLogger;

/**
 * JComboBoxPreference monitors the selected index of a JComboBox. It sets the saved index when a dialog is loaded and
 * changes it when it changes.
 * <p>
 * Call preferences.manage(new JComboBoxPreference(JComboBox)) to use this preference, on a JComboBox that has called
 * setName
 */
public class JComboBoxPreference extends PreferenceElement implements ItemListener {
    private final static MMLogger logger = MMLogger.create(JComboBoxPreference.class);

    // region Variable Declarations
    private final WeakReference<JComboBox<?>> weakReference;
    private int selectedIndex;
    // endregion Variable Declarations

    // region Constructors
    public JComboBoxPreference(final JComboBox<?> comboBox) throws Exception {
        super(comboBox.getName());
        setSelectedIndex(comboBox.getSelectedIndex());
        weakReference = new WeakReference<>(comboBox);
        comboBox.addItemListener(this);
    }
    // endregion Constructors

    // region Getters/Setters
    public WeakReference<JComboBox<?>> getWeakReference() {
        return weakReference;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(final int selectedIndex) {
        this.selectedIndex = selectedIndex;
    }
    // endregion Getters/Setters

    // region PreferenceElement
    @Override
    protected String getValue() {
        return Integer.toString(getSelectedIndex());
    }

    @Override
    protected void initialize(final String value) throws Exception {
        if (StringUtility.isNullOrBlank(value)) {
            logger.error("Cannot create a JComboBoxPreference because of a null or blank input value");
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
    // endregion PreferenceElement

    // region ItemListener
    @Override
    public void itemStateChanged(final ItemEvent evt) {
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            final JComboBox<?> element = getWeakReference().get();
            if (element != null) {
                setSelectedIndex(element.getSelectedIndex());
            }
        }
    }
    // endregion ItemListener
}
