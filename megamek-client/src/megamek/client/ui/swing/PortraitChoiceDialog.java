/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
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

package megamek.client.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ItemEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.ImageFileFactory;
import megamek.common.Configuration;
import megamek.common.Crew;
import megamek.common.util.DirectoryItems;

/**
 * This dialog allows players to select a portrait for your pilot.
 *  It automatically fills itself with all of the images in the
 * {@link Configuration#portraitImagesDir()} directory tree.
 * <p/>
 * Created on September 17, 2009
 *
 * @author Jay Lawson
 * @version 2
 */

public class PortraitChoiceDialog extends JDialog {

    private static final long serialVersionUID = -4495461837182817406L;
    
    private JFrame frame;
    private JButton sourceButton;
    private JButton btnCancel;
    private JButton btnSelect;
    private JComboBox<String> comboCategories;
    private JScrollPane scrPortraits;
    private JTable tablePortrait;
    private DirectoryItems portraits;
    private PortraitTableModel portraitModel;
    private String category;
    private String filename;
    private PortraitTableMouseAdapter portraitMouseAdapter;


   /** Creates new form CamoChoiceDialog */
   public PortraitChoiceDialog(JFrame parent, JButton button) {
       
    // Initialize our superclass and record our parent frame.
       super(parent, Messages
               .getString("PortraitChoiceDialog.select_portrait"), true); //$NON-NLS-1$
       frame = parent;
       sourceButton = button;
     
       // Parse the camo directory.
       try {
           portraits = new DirectoryItems(
                   Configuration.portraitImagesDir(),
                   "", //$NON-NLS-1$
                   ImageFileFactory.getInstance()
           );
       } catch (Exception e) {
           portraits = null;
       }
       
       portraitMouseAdapter = new PortraitTableMouseAdapter();
       portraitModel = new PortraitTableModel();
       
       scrPortraits = new JScrollPane();
       tablePortrait = new JTable();
       tablePortrait.setModel(portraitModel);
       tablePortrait.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
       tablePortrait.setRowHeight(76);
       tablePortrait.getColumnModel().getColumn(0).setCellRenderer(portraitModel.getRenderer());
       tablePortrait.addMouseListener(portraitMouseAdapter);
       scrPortraits.setViewportView(tablePortrait);
       comboCategories = new JComboBox<String>();
       DefaultComboBoxModel<String> categoryModel = new DefaultComboBoxModel<String>();
       categoryModel.addElement(Crew.ROOT_PORTRAIT);
       if (portraits != null) {
           Iterator<String> names = portraits.getCategoryNames();
           while (names.hasNext()) {
               String name = names.next();
               if (!"".equals(name)) { //$NON-NLS-1$
                   categoryModel.addElement(name);
               }
           }
       }
       comboCategories.setModel(categoryModel);
       comboCategories.setName("comboCategories"); // NOI18N
       comboCategories.addItemListener(new java.awt.event.ItemListener() {
           public void itemStateChanged(java.awt.event.ItemEvent evt) {
               comboCategoriesItemStateChanged(evt);
           }
       });
       btnSelect = new JButton();
       btnSelect.setText(Messages.getString("PortraitChoiceDialog.Select"));
       btnSelect.addActionListener(new java.awt.event.ActionListener() {
           public void actionPerformed(java.awt.event.ActionEvent evt) {
               select();
           }
       });
       btnCancel = new JButton();
       btnCancel.setText(Messages.getString("PortraitChoiceDialog.Cancel"));
       btnCancel.addActionListener(new java.awt.event.ActionListener() {
           public void actionPerformed(java.awt.event.ActionEvent evt) {
               cancel();
           }
       });
       
       setLayout(new GridBagLayout());
       GridBagConstraints c;

       c = new GridBagConstraints();
       c.gridx = 0;
       c.gridy = 1;
       c.gridwidth = 2;
       c.fill = GridBagConstraints.BOTH;
       c.anchor = GridBagConstraints.NORTHWEST;
       c.weightx = 1.0;
       c.weighty = 1.0;
       add(scrPortraits, c);

       c = new GridBagConstraints();
       c.gridx = 0;
       c.gridy = 0;
       c.gridwidth = 2;
       c.anchor = GridBagConstraints.NORTHWEST;
       c.weightx = 1.0;
       add(comboCategories, c);

       c = new GridBagConstraints();
       c.gridx = 0;
       c.gridy = 2;
       c.weightx = 0.5;
       getContentPane().add(btnSelect, c);
       
       c = new GridBagConstraints();
       c.gridx = 1;
       c.gridy = 2;
       c.weightx = 0.5;
       add(btnCancel, c);

       pack();
   }                

    private void cancel() {                                          
       setVisible(false);
    }                                         

    private void select() {                                          
       category = portraitModel.getCategory();
       if(tablePortrait.getSelectedRow() != -1) {
           filename = (String) portraitModel.getValueAt(tablePortrait.getSelectedRow(), 0);
       } else {
           filename = Crew.PORTRAIT_NONE;
       }
       sourceButton.setIcon(generateIcon(category, filename));
       setVisible(false);
    }                                         
    
    private void comboCategoriesItemStateChanged(java.awt.event.ItemEvent evt) {                                                 
       if (evt.getStateChange() == ItemEvent.SELECTED) {
           fillTable((String) evt.getItem());
       }
    }                                                

   public String getCategory() {
       return category;
   }

   public String getFileName() {
       return filename;
   }
   
   public void setPilot(Crew pilot) {
       category = pilot.getPortraitCategory();
       filename = pilot.getPortraitFileName();
       sourceButton.setIcon(generateIcon(category, filename));
       comboCategories.getModel().setSelectedItem(category);
           fillTable(category);
           int rowIndex = 0;
           for(int i = 0; i < portraitModel.getRowCount(); i++) {
           if(((String) portraitModel.getValueAt(i, 0)).equals(filename)) {
               rowIndex = i;
               break;
           }
       }
       tablePortrait.setRowSelectionInterval(rowIndex, rowIndex);     
   }

    private void fillTable(String category) {
       portraitModel.reset();
       portraitModel.setCategory(category);
       // Translate the "root camo" category name.
       Iterator<String> portraitNames;
       if (Crew.ROOT_PORTRAIT.equals(category)) {
           portraitModel.addPortrait(Crew.PORTRAIT_NONE);
           portraitNames = portraits.getItemNames(""); //$NON-NLS-1$
       } else {
           portraitNames = portraits.getItemNames(category);
       }

       // Get the camo names for this category.
       while (portraitNames.hasNext()) {
           String name = portraitNames.next();
           if(!"default.gif".equals(name)) {
               portraitModel.addPortrait(name);
           }
       }
       if(portraitModel.getRowCount() > 0) {
           tablePortrait.setRowSelectionInterval(0, 0);
       }
   }
    
    Icon generateIcon(String cat, String item) {
        if((null == cat) || (null == item)) {
            return null;
        }
        
        String actualItem = item;
        if (Crew.PORTRAIT_NONE.equals(actualItem)) {
            actualItem = "default.gif"; //$NON-NLS-1$
        }
        
        String actualCat = cat;
        // Replace the ROOT_PORTRAIT string with "".
        if (Crew.ROOT_PORTRAIT.equals(actualCat)) {
            actualCat = ""; //$NON-NLS-1$
        }

        //an actual portrait
        try {
            // We need to copy the image to make it appear.
            Image image = (Image) portraits.getItem(actualCat, actualItem);
            if(null == image) {
                //the image could not be found so switch to default one
                category = Crew.ROOT_PORTRAIT;
                actualCat = "";
                filename = Crew.PORTRAIT_NONE;
                actualItem = "default.gif";
                image = (Image) portraits.getItem(actualCat, actualItem);
            }
            image = image.getScaledInstance(-1, 72, Image.SCALE_DEFAULT);
            return new ImageIcon(image);
        } catch (Exception err) {
            // Print the stack trace and display the message.
            err.printStackTrace();
            JOptionPane
                    .showMessageDialog(
                            frame,
                            err.getMessage(),
                            Messages
                                    .getString("PortraitChoiceDialog.error_getting_portrait"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            return null;
        }
    }

    /**
       * A table model for displaying camos
    */
   public class PortraitTableModel extends AbstractTableModel {

       private static final long serialVersionUID = -992524169822797473L;
       private String[] columnNames;
       private String category;
       private ArrayList<String> names;
       private ArrayList<Image> images;

       public PortraitTableModel() {
           columnNames = new String[] {"Portraits"};
           category = Crew.ROOT_PORTRAIT;
           names = new ArrayList<String>();
           images = new ArrayList<Image>();
       }

       public int getRowCount() {
           return names.size();
       }

       public int getColumnCount() {
           return 1;
       }

       public void reset() {
           category = Crew.ROOT_PORTRAIT;
           names = new ArrayList<String>();
           images = new ArrayList<Image>();
       }

       @Override
       public String getColumnName(int column) {
           return columnNames[column];
       }

       public Object getValueAt(int row, int col) {
           return names.get(row);
       }

       public Object getImageAt(int row) {
           return images.get(row);
       }

       public void setCategory(String c) {
           category = c;
       }

       public String getCategory() {
           return category;
       }

       public void addPortrait(String name) {
           names.add(name);
           fireTableDataChanged();
       }

       @Override
       public Class<?> getColumnClass(int c) {
           return getValueAt(0, c).getClass();
       }

       @Override
       public boolean isCellEditable(int row, int col) {
           return false;
       }

       public PortraitTableModel.Renderer getRenderer() {
           return new PortraitTableModel.Renderer();
       }


       public class Renderer extends PortraitPanel implements TableCellRenderer {

               private static final long serialVersionUID = 7916914665407121264L;

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                   Component c = this;
                   setOpaque(true);
                   String name = getValueAt(row, column).toString();
                   setText(getValueAt(row, column).toString());
                   setImage(category, name);
                   if(isSelected) {
                       setBackground(new Color(220,220,220));
                   } else {
                       setBackground(Color.WHITE);
                   }
    
                   return c;
            }      
       }
   }
   
   public class PortraitPanel extends JPanel {

       private static final long serialVersionUID = -6497926619314613457L;
       private JLabel lblImage;
        
        public PortraitPanel() {
            GridBagConstraints c = new GridBagConstraints();

            lblImage = new JLabel();

            setLayout(new GridBagLayout());
            
            c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1.0;
            c.weighty = 1.0;
            add(lblImage, c);
        }

        public void setText(String text) {
            lblImage.setText(text);
        }
        
        public void setImage(String category, String name) {

            if (null == category || null == name) {
                return;
            }
            
            if(name.equals(Crew.PORTRAIT_NONE)) {
                name = "default.gif";
            }

            // Try to get the portrait file.
            try {

                // Translate the root portrait directory name.
                if (Crew.ROOT_PORTRAIT.equals(category))
                    category = ""; //$NON-NLS-1$
                Image portrait = (Image) portraits.getItem(category, name);
                lblImage.setIcon(new ImageIcon(portrait));
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

   public class PortraitTableMouseAdapter extends MouseInputAdapter {

       public void mouseClicked(MouseEvent e) {

           if (e.getClickCount() == 2) {
               select();
           }
       }
   }

}
