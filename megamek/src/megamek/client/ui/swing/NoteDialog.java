/*
 * MegaMek - Copyright (C) 2003, 2004 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.client.ui.swing;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import megamek.client.ui.Messages;
import megamek.common.SpecialHexDisplay;

/**
 * A dialog for creating/editing a note that is attached to a hex via the 
 * <code>SpecialHexDisplay</code> framework.
 * 
 * @author arlith
 */
public class NoteDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -3126840102187553386L;
    
    private JLabel noteLbl, visibilityLbl;
    
    private JComboBox<String> visibility = new JComboBox<>();
    
    private JTextArea noteText = new JTextArea("");
  
    private JButton butDone = new JButton(Messages.getString("NoteDialog.Done"));
    private JButton butCancel = new JButton(Messages.getString("NoteDialog.Cancel"));
    
    boolean accepted = false;
    
    SpecialHexDisplay note;

    NoteDialog(JFrame frame, SpecialHexDisplay note) {
        super(frame, Messages.getString("NoteDialog.title"), true);
        this.note = note;
        setResizable(false);
        butDone.addActionListener(this);
        butCancel.addActionListener(this);
        
        JPanel layout;
        
        noteText.setLineWrap(true);
        noteText.setMinimumSize(new Dimension(getWidth(),200));
        noteText.setPreferredSize(new Dimension(getWidth(),200));
        
        noteLbl = new JLabel(Messages.getString("NoteDialog.note"));
        visibilityLbl = new JLabel(Messages.getString("NoteDialog.visibility"));
        
        visibility.addItem(Messages.getString("NoteDialog.owner"));
        visibility.addItem(Messages.getString("NoteDialog.team"));
        visibility.addItem(Messages.getString("NoteDialog.all"));
        visibility.setSelectedIndex(0);
        
        if (note != null) {
            noteText.setText(note.getInfo());
            visibility.setSelectedIndex(note.getObscuredLevel());
        }
        
        getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        
        getContentPane().add(noteLbl);
        getContentPane().add(noteText);
        
        layout = new JPanel();
        layout.add(visibilityLbl);
        layout.add(visibility);
        getContentPane().add(layout);
        
        layout = new JPanel();
        layout.add(butDone);
        layout.add(butCancel);
        getContentPane().add(layout);
        
        pack();
        setLocation(frame.getLocation().x + frame.getSize().width / 2
                - getSize().width / 2, frame.getLocation().y
                + frame.getSize().height / 2 - getSize().height / 2);
    }

    public boolean isAccepted() {
        return accepted;
    }
  
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        if (actionEvent.getSource().equals(butDone)) {
            note.setInfo(noteText.getText());
            note.setObscuredLevel(visibility.getSelectedIndex());
            accepted = true;
        }
        setVisible(false);
    }
}
