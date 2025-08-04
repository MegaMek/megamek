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
import javax.swing.JSplitPane;

import megamek.codeUtilities.StringUtility;
import megamek.logging.MMLogger;

/**
 * JSplitPanePreference monitors the location of the split divider on a JSplitPane. It sets the saved value when a
 * dialog is loaded and changes it as it changes.
 * <p>
 * Call preferences.manage(new JSplitPanePreference(JSplitPane)) to use this preference, on a JSplitPane that has called
 * setName
 */
public class JSplitPanePreference extends PreferenceElement implements PropertyChangeListener {
    private final static MMLogger logger = MMLogger.create(JSplitPanePreference.class);

    // region Variable Declarations
    private final WeakReference<JSplitPane> weakReference;
    private int dividerLocation;
    // endregion Variable Declarations

    // region Constructors
    public JSplitPanePreference(final JSplitPane splitPane) throws Exception {
        super(splitPane.getName());
        setDividerLocation(splitPane.getDividerLocation());
        weakReference = new WeakReference<>(splitPane);
        splitPane.addPropertyChangeListener(this);
    }
    // endregion Constructors

    // region Getters/Setters
    public WeakReference<JSplitPane> getWeakReference() {
        return weakReference;
    }

    public int getDividerLocation() {
        return dividerLocation;
    }

    public void setDividerLocation(final int dividerLocation) {
        this.dividerLocation = dividerLocation;
    }
    // endregion Getters/Setters

    // region PreferenceElement
    @Override
    protected String getValue() {
        return Integer.toString(getDividerLocation());
    }

    @Override
    protected void initialize(final String value) throws Exception {
        if (StringUtility.isNullOrBlank(value)) {
            logger.error("Cannot create a JSplitPanePreference because of a null or blank input value");
            throw new Exception();
        }

        final JSplitPane element = getWeakReference().get();
        if (element != null) {
            setDividerLocation(Integer.parseInt(value));
            element.setDividerLocation(getDividerLocation());
        }
    }

    @Override
    protected void dispose() {
        final JSplitPane element = getWeakReference().get();
        if (element != null) {
            element.removePropertyChangeListener(this);
            getWeakReference().clear();
        }
    }
    // endregion PreferenceElement

    // region PropertyChangeListener
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        final JSplitPane element = getWeakReference().get();
        if (element != null) {
            setDividerLocation(element.getDividerLocation());
        }
    }
    // endregion PropertyChangeListener
}
