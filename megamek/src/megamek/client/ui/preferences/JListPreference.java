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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import javax.swing.JList;

import megamek.codeUtilities.StringUtility;
import megamek.logging.MMLogger;
import org.apache.commons.lang3.StringUtils;

/**
 * JListPreference monitors the selected indices of a {@link JList}. It sets the saved indices when a dialog is loaded
 * and changes them when they change.
 * <p>
 * Call preferences.manage(new JListPreference(JList)) to use this preference, on a JList that has called setName
 */
public class JListPreference extends PreferenceElement implements PropertyChangeListener {
    private final static MMLogger logger = MMLogger.create(JListPreference.class);

    // region Variable Declarations
    private final WeakReference<JList<?>> weakReference;
    private int[] selectedIndices;
    // endregion Variable Declarations

    // region Constructors
    public JListPreference(final JList<?> jList) throws Exception {
        super(jList.getName());
        setSelectedIndices(jList.getSelectedIndices());
        weakReference = new WeakReference<>(jList);
        jList.addPropertyChangeListener(this);
    }
    // endregion Constructors

    // region Getters/Setters
    public WeakReference<JList<?>> getWeakReference() {
        return weakReference;
    }

    public int[] getSelectedIndices() {
        return selectedIndices;
    }

    public void setSelectedIndices(final int... selectedIndices) {
        this.selectedIndices = selectedIndices;
    }
    // endregion Getters/Setters

    // region PreferenceElement
    @Override
    protected String getValue() {
        return StringUtils.join(getSelectedIndices(), '|');
    }

    @Override
    protected void initialize(final String value) throws Exception {
        if (StringUtility.isNullOrBlank(value)) {
            logger.error("Cannot create a JListPreference because of a null or blank input value");
            throw new Exception();
        }

        final JList<?> element = getWeakReference().get();
        if (element != null) {
            final String[] strings = value.split("\\|");
            setSelectedIndices(Arrays.stream(strings).mapToInt(Integer::parseInt).toArray());
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
    // endregion PreferenceElement

    // region PropertyChangeListener
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        final JList<?> element = getWeakReference().get();
        if (element != null) {
            setSelectedIndices(element.getSelectedIndices());
        }
    }
    // endregion PropertyChangeListener
}
