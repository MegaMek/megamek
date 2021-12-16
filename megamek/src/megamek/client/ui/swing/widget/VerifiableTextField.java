/*
 * MegaMek - Copyright (C) 2000-2003 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.client.ui.swing.widget;

import megamek.client.ui.swing.GUIPreferences;
import megamek.client.ui.swing.util.DataVerifier;
import megamek.client.ui.swing.util.VerifyNotNullOrEmpty;
import megamek.common.util.StringUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.HashSet;
import java.util.Set;

/**
 * This is an extension of the {@link JTextField} that includes the capacity for {@link DataVerifier} objects to be
 * added.  These verifiers will be checked when the component loses focus and if any fail, the background of the field
 * will be turned red and the tool tip updated with information on what verifier was failed.
 *
 * @author Deric "Netzilla" Page (deric dot page at usa dot net)
 * @version %Id%
 * @since 3/14/14 1:08 PM
 */
public class VerifiableTextField extends JTextField implements FocusListener {

    private static final long serialVersionUID = -4169356645839508584L;

    private boolean selectAllTextOnGotFocus = false;
    private final Set<DataVerifier> verifiers = new HashSet<>();
    private Boolean required = null;
    private String oldToolTip = null;

    public VerifiableTextField() throws HeadlessException {
        this(null, 0);
    }

    public VerifiableTextField(String text) throws HeadlessException {
        this(text, 0);
    }

    public VerifiableTextField(int columns) throws HeadlessException {
        this(null, columns);
    }

    public VerifiableTextField(String text, int columns) throws HeadlessException {
        super(text, columns);
        addFocusListener(this);
    }
    
    public VerifiableTextField(int columns, boolean isReqd, boolean selectOnFoc, DataVerifier ver) 
            throws HeadlessException {
        this(null, columns);
        setRequired(isReqd);
        setSelectAllTextOnGotFocus(selectOnFoc);
        addVerifier(ver);
    }

    /**
     * @return TRUE if all the text in this control will be automatically selected when it receives the focus.
     */
    public boolean isSelectAllTextOnGotFocus() {
        return selectAllTextOnGotFocus;
    }

    /**
     * @param selectAllTextOnGotFocus Set TRUE if all the text in this control will be automatically selected when it
     *                                receives the focus.
     */
    public void setSelectAllTextOnGotFocus(boolean selectAllTextOnGotFocus) {
        this.selectAllTextOnGotFocus = selectAllTextOnGotFocus;
    }

    /**
     * @return TRUE if the field's text value is NULL or an empty {@link String}.
     */
    public boolean isTextNullOrEmpty() {
        return StringUtil.isNullOrEmpty(getText());
    }

    /**
     * @return TRUE if the field's text value is a valid number.
     */
    public boolean isTextNumeric() {
        if (getText().startsWith("\\+")) {
            setText(getText().replaceFirst("\\+", ""));
        }
        return StringUtil.isNumeric(getText());
    }

    /**
     * @return TRUE if this field is not allowed to be null or empty.
     */
    public boolean isRequired() {
        if (required != null) {
            return required;
        }
        required = false;
        for (DataVerifier v : verifiers) {
            if (v instanceof VerifyNotNullOrEmpty) {
                required = true;
            }
        }
        return required;
    }

    /**
     * Marks this text field as required/unrequired and sets up/removes a {@link VerifyNotNullOrEmpty} verifier.
     * Also sets the background color blue if it is required.
     *
     * @param required TRUE if this field is required to have data.
     */
    public void setRequired(boolean required) {
        this.required = required;
        if (!required && !isRequired()) {
            return;
        }

        // Add a new VerifyNotNullOrEmpty verifier and exit.
        if (required) {
            verifiers.add(new VerifyNotNullOrEmpty());
//            setBackground(BK_REQUIRED);
            return;
        }

        // Remove the VerifyNotNullOrEmpty verifier and exit.
        DataVerifier v = null;
        while (verifiers.iterator().hasNext()) {
            v = verifiers.iterator().next();
            if (v instanceof VerifyNotNullOrEmpty) {
                break;
            }
        }
        verifiers.remove(v);
        setBackground(UIManager.getColor("TextField.background"));
    }

    /**
     * @return The text of the field with XML reserved characters properly escaped.
     */
    public String getXmlSafeText() {
        if (isTextNullOrEmpty()) {
            return "";
        }
        return StringUtil.makeXmlSafe(getText());
    }

    @Override
    public void focusGained(FocusEvent e) {
        if ((this != e.getSource()) || !selectAllTextOnGotFocus || isTextNullOrEmpty()) {
            return;
        }

        int end = getText().length();
        setSelectionStart(0);
        setSelectionEnd(end);
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (this != e.getSource()) {
            return;
        }

//        Color goodBackground = isRequired() ? BK_REQUIRED : BK_DEFAULT;
        String verifyResult = verifyTextS();

        // If verifyResult is null, no problems were found.
        if (verifyResult == null) {
            setBackground(UIManager.getColor("TextField.background"));
            if (oldToolTip != null) {
                setToolTipText(oldToolTip);
                oldToolTip = null;
            }
            return;
        }

        // Something failed validation.  Set the background color red and update the tooltip to inform the user.
        
        setBackground(getInvalidColor());
        oldToolTip = getToolTipText();
        setToolTipText(verifyResult);
    }

    /**
     * Adds a new {@link DataVerifier} to validate this field's data.
     *
     * @param verifier The new {@link DataVerifier} to be added.
     */
    public void addVerifier(DataVerifier verifier) {
        verifiers.add(verifier);
    }

    /**
     * Removes the given {@link DataVerifier} from this field.
     *
     * @param verifier The {@link DataVerifier} to be removed.
     */
    public void removeVerifier(DataVerifier verifier) {
        verifiers.remove(verifier);
    }

    /**
     * Compares the text field's value to the list of {@link DataVerifier} objects to ensure the validity of the data.
     * If the text value passes all validation checks, a NULL value will be returned.  Otherwise a description of
     * the failed validation will be returned.
     *
     * @return NULL if the text in the field is valid. A description of the failure otherwise.
     */
    public String verifyTextS() {
        if (verifiers.isEmpty()) {
            return null;
        }

        String result = null;
        for (DataVerifier v : verifiers) {
            result = v.verify(getText());
            if (result != null) {
                break;
            }
        }
        return result;
    }
    
    /**
     * Compares the text field's value to the list of {@link DataVerifier} objects to ensure the validity of the data.
     *
     * @return true if the text in the field is valid.
     */
    public boolean verifyText() {
        return verifyTextS() == null;
    }

    public Integer getAsInt() {
        if (!isTextNumeric()) {
            return null;
        }
        return Integer.parseInt(getText().trim());
    }

    public String getOldToolTip() {
        return oldToolTip;
    }

    public void setOldToolTip(String oldToolTip) {
        this.oldToolTip = oldToolTip;
    }

    /** 
     * Returns an "invalid background" color. It is mixed from the
     * GUIPreferences WarningColor and the UIManager textfield 
     * background color. 
     */
    public static Color getInvalidColor() {
        Color bgColor = UIManager.getColor("TextField.background");
        Color warnColor = GUIPreferences.getInstance().getWarningColor();
        double part = 0.1;
        int r = (int) (part * warnColor.getRed() + (1 - part) * bgColor.getRed());  
        int g = (int) (part * warnColor.getGreen() + (1 - part) * bgColor.getGreen());
        int b = (int) (part * warnColor.getBlue() + (1 - part) * bgColor.getBlue());
        return new Color(r, g, b);
    }
    
    @Override
    public Dimension getMaximumSize() {
        // Make this TextField not stretch vertically
        Dimension size = getPreferredSize();
        Dimension maxSize = super.getMaximumSize();
        return new Dimension(maxSize.width, size.height);
    }
}
