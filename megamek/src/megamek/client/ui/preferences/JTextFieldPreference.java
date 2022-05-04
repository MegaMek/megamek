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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.lang.ref.WeakReference;

/**
 * JTextFieldPreference monitors the text value of a JTextField. It sets the saved value when a
 * dialog is loaded and changes it as it changes.
 *
 * Call preferences.manage(new JTextFieldPreference(JTextField)) to use this preference, on a
 * JTextField that has called setName
 */
public class JTextFieldPreference extends PreferenceElement implements DocumentListener {
    //region Variable Declarations
    private final WeakReference<JTextField> weakReference;
    private String text;
    //endregion Variable Declarations

    //region Constructors
    public JTextFieldPreference(final JTextField textField) throws Exception {
        super(textField.getName());
        setText(textField.getText());
        weakReference = new WeakReference<>(textField);
        textField.getDocument().addDocumentListener(this);
    }
    //endregion Constructors

    //region Getters/Setters
    public WeakReference<JTextField> getWeakReference() {
        return weakReference;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }
    //endregion Getters/Setters

    //region PreferenceElement
    @Override
    protected String getValue() {
        return getText();
    }

    @Override
    protected void initialize(final String value) throws Exception {
        if (StringUtility.isNullOrBlank(value)) {
            LogManager.getLogger().error("Cannot create a JTextFieldPreference because of a null or blank input value");
            throw new Exception();
        }

        final JTextField element = getWeakReference().get();
        if (element != null) {
            element.setText(value);
        }
    }

    @Override
    protected void dispose() {
        final JTextField element = getWeakReference().get();
        if (element != null) {
            element.getDocument().removeDocumentListener(this);
            getWeakReference().clear();
        }
    }
    //endregion PreferenceElement

    //region DocumentListener
    @Override
    public void insertUpdate(final DocumentEvent evt) {
        final JTextField element = getWeakReference().get();
        if (element != null) {
            setText(element.getText());
        }
    }

    @Override
    public void removeUpdate(final DocumentEvent evt) {
        final JTextField element = getWeakReference().get();
        if (element != null) {
            setText(element.getText());
        }
    }

    @Override
    public void changedUpdate(final DocumentEvent evt) {
        final JTextField element = getWeakReference().get();
        if (element != null) {
            setText(element.getText());
        }
    }
    //endregion DocumentListener
}
