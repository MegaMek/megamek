/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
 
 /**
 * DialogOptionComponent.java
 *
 * @author Cord Awtry
 */

package megamek.client;

import java.awt.*;

import java.awt.event.MouseListener;
import java.awt.event.ItemListener;
import megamek.common.options.GameOption;

public class DialogOptionComponent extends Panel implements MouseListener, ItemListener
{
    GameOption option;
    
    private Checkbox checkbox;
    private TextField textField;
    private Label label;
    private DialogOptionListener parent;
    
    public DialogOptionComponent(DialogOptionListener parent, GameOption option) {
      this(parent, option, true);
    }
    
    public DialogOptionComponent(DialogOptionListener parent, GameOption option, boolean editable) {
        this.parent = parent;
        this.option = option;
        
        addMouseListener(this);
        
        setLayout(new BorderLayout());
        switch(option.getType()) {
            case GameOption.BOOLEAN :
                checkbox = new Checkbox(option.getFullName(), option.booleanValue());
                checkbox.addMouseListener(this);
                checkbox.addItemListener(this);
                add(checkbox, BorderLayout.CENTER);
                
                if ( !editable )
                  checkbox.setEnabled(false);
                  
                break;
            default :
                textField = new TextField(option.stringValue(), option.getTextFieldLength());
                textField.addMouseListener(this);
                label = new Label(option.getFullName());
                label.addMouseListener(this);

                if ( option.isLabelBeforeTextField() ) {
                  add(label, BorderLayout.CENTER);
                  add(textField, BorderLayout.WEST);
                } else {
                  add(textField, BorderLayout.WEST);
                  add(label, BorderLayout.CENTER);
                }

                if ( !editable )
                  textField.setEnabled(false);

                break;
        }
        
    }
    
    public boolean hasChanged() {
        return !option.getValue().equals(getValue());
    }
    
    public Object getValue() {
        switch(option.getType()) {
            case GameOption.BOOLEAN :
                return new Boolean(checkbox.getState());
            case GameOption.INTEGER :
                return Integer.valueOf(textField.getText());
            case GameOption.FLOAT :
                return Float.valueOf(textField.getText());
            case GameOption.STRING :
                return textField.getText();
            default :
                return null;
        }
    }
    
    public GameOption getOption() {
      return option;
    }

    /**
     * Update the option component so that it is editable or view-only.
     *
     * @param   editable - <code>true</code> if the contents of the component
     *          are editable, <code>false</code> if they are view-only.
     */
    public void setEditable( boolean editable ) {

        // Update the correct control.
        switch(option.getType()) {
        case GameOption.BOOLEAN :
            checkbox.setEnabled( editable );
            break;
        default :
            textField.setEnabled( editable );
            break;
        }
    }

    public void setState(boolean state) {
        checkbox.setState(state);
    }

    /**
     * Returns a new option, representing the option in it's changed state.
     */
    public GameOption changedOption() {
        return new GameOption(option.getShortName(), "", "", option.getType(), getValue());
    }
    
    public void mousePressed(java.awt.event.MouseEvent mouseEvent) {
    }
    public void mouseEntered(java.awt.event.MouseEvent mouseEvent) {
        parent.showDescFor(option);
    }
    public void mouseReleased(java.awt.event.MouseEvent mouseEvent) {
    }
    public void mouseClicked(java.awt.event.MouseEvent mouseEvent) {
    }
    public void mouseExited(java.awt.event.MouseEvent mouseEvent) {
    }
    
    public void itemStateChanged(java.awt.event.ItemEvent itemEvent) {
        parent.optionClicked(this, option, checkbox.getState());
    }
    
}

