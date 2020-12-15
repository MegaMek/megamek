/*  
* MegaMek - Copyright (C) 2020 - The MegaMek Team  
*  
* This program is free software; you can redistribute it and/or modify it under  
* the terms of the GNU General Public License as published by the Free Software  
* Foundation; either version 2 of the License, or (at your option) any later  
* version.  
*  
* This program is distributed in the hope that it will be useful, but WITHOUT  
* ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS  
* FOR A PARTICULAR PURPOSE. See the GNU General Public License for more  
* details.  
*/ 
package megamek.client.ui.swing.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.*;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import megamek.client.ui.Messages;
import megamek.client.ui.swing.ClientDialog;
import megamek.client.ui.swing.util.UIUtil;

/** 
 * A simple modal confirmation dialog showing a single question and YES
 * and NO buttons.
 *  
 * @author Juliez
 */
public class MMConfirmDialog {
    
    public enum Response { YES, NO };
    
    private final static Dimension MINIMUM_SIZE = new Dimension(444, 180); 

    /** Shows a modal confirmation dialog with a YES and a NO button. The String title
     * is shown as the window title. The given message is shown as the question to be confirmed
     * in the center of the dialog.
     * 
     * <BR><BR>Returns Response.YES when the user pressed ENTER or selected YES,
     * Response.NO otherwise.
     * 
     * <BR><BR>The dialog scales itself with the current GUI scale. It closes when ESC is pressed.
     */
    public static Response confirm(JFrame owner, String title, String message) {
        ConfirmDialog dialog = new ConfirmDialog(owner, title, message);
        dialog.center();
        dialog.setVisible(true);
        return dialog.userResponse;
    }
    
    // PRIVATE
    
    private static class ConfirmDialog extends ClientDialog {
        
        private static final long serialVersionUID = -2877691301521648979L;

        private Response userResponse = Response.NO;
        
        private JPanel panButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        private JButton butYes = new DialogButton(Messages.getString("Yes"));
        private JButton butNo = new DialogButton(Messages.getString("No"));
        
        public ConfirmDialog(JFrame owner, String title, String message) {
            super(owner, title, true, true);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    respondNo();
                    super.windowClosed(e);
                }
            });
            add(panButtons, BorderLayout.PAGE_END);
            panButtons.add(butYes);
            panButtons.add(butNo);
            butYes.addActionListener(e -> respondYes());
            butNo.addActionListener(e -> respondNo());
            JLabel lblMain = new JLabel(message);
            lblMain.setVerticalAlignment(JLabel.CENTER);
            lblMain.setHorizontalAlignment(JLabel.CENTER);
            add(lblMain, BorderLayout.CENTER);
            setMinimumSize(UIUtil.scaleForGUI(MINIMUM_SIZE));
            center();
            // Make the dialog take ENTER as Yes and ESC as No
            getRootPane().setDefaultButton(butYes);
            addKeyListener(k);
            butYes.addKeyListener(k);
            butNo.addKeyListener(k);
        }
        
        
        private void respondNo() {
            setVisible(false);
        }
        
        private void respondYes() {
            userResponse = Response.YES;
            setVisible(false);
        }

        KeyListener k = new KeyAdapter() { 
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    respondNo();
                }
            };
        };
        
    }
}
