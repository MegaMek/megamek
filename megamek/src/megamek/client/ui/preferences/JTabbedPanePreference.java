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

import megamek.codeUtilities.StringUtility;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.lang.ref.WeakReference;

/**
 * JTabbedPanePreference monitors the selected tab of a JTabbedPane. It sets the saved value when a
 * dialog is loaded and changes it as it changes.
 *
 * Call preferences.manage(new JTabbedPanePreference(JTabbedPane)) to use this preference, on a
 * JTabbedPane that has called setName
 */
public class JTabbedPanePreference extends PreferenceElement implements ChangeListener {
    //region Variable Declarations
    private final WeakReference<JTabbedPane> weakReference;
    private int selectedIndex;
    //endregion Variable Declarations

    //region Constructors
    public JTabbedPanePreference(final JTabbedPane tabbedPane) throws Exception {
        super(tabbedPane.getName());
        setSelectedIndex(tabbedPane.getSelectedIndex());
        weakReference = new WeakReference<>(tabbedPane);
        tabbedPane.addChangeListener(this);
    }
    //endregion Constructors

    //region Getters/Setters
    public WeakReference<JTabbedPane> getWeakReference() {
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
            LogManager.getLogger().error("Cannot create a JTabbedPanePreference because of a null or blank input value");
            throw new Exception();
        }

        final JTabbedPane element = getWeakReference().get();
        if (element != null) {
            final int index = Integer.parseInt(value);
            if ((index > 0) && (index < element.getTabCount())) {
                setSelectedIndex(index);
                element.setSelectedIndex(getSelectedIndex());
            }
        }
    }

    @Override
    protected void dispose() {
        final JTabbedPane element = getWeakReference().get();
        if (element != null) {
            element.removeChangeListener(this);
            getWeakReference().clear();
        }
    }
    //endregion PreferenceElement

    //region ChangeListener
    @Override
    public void stateChanged(final ChangeEvent evt) {
        final JTabbedPane element = getWeakReference().get();
        if (element != null) {
            setSelectedIndex(element.getSelectedIndex());
        }
    }
    //endregion ChangeListener
}
