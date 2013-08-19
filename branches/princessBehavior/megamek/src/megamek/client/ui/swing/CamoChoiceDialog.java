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
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
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
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.util.ImageFileFactory;
import megamek.client.ui.swing.util.PlayerColors;
import megamek.common.Configuration;
import megamek.common.Entity;
import megamek.common.IPlayer;
import megamek.common.util.DirectoryItems;

/**
 * This dialog allows players to select the camo pattern (or color) used by
 * their units during the game. It automatically fills itself with all the color
 * choices in <code>Settings</code> and all the camo patterns in the
 * {@link Configuration#camoDir()} directory tree.
 * <p/>
 * Created on January 19, 2004
 *
 * @author James Damour
 * @version 1
 */
public class CamoChoiceDialog extends JDialog {

    private static final long serialVersionUID = 9220162367683378065L;

    private JFrame frame;
    private DirectoryItems camos;
    private JScrollPane scrCamo;
    JButton sourceButton;
    private JButton btnCancel;
    private JButton btnSelect;
    private JComboBox comboCategories;
    private CamoTableModel camoModel;
    private CamoTableMouseAdapter camoMouseAdapter;
    private JTable tableCamo;

    String category;
    String filename;
    private int colorIndex;
    private IPlayer player;
    private Entity entity;

    private boolean select;

    /**
     * Create a dialog that allows players to choose a camo pattern.
     *
     * @param parent - the <code>Frame</code> that displays this dialog.
     */
    public CamoChoiceDialog(JFrame parent, JButton button) {

        // Initialize our superclass and record our parent frame.
        super(parent, Messages.getString("CamoChoiceDialog.select_camo_pattern"), true); //$NON-NLS-1$
        frame = parent;
        sourceButton = button;

        // Parse the camo directory.
        try {
            camos = new DirectoryItems(
                    Configuration.camoDir(),
                    "", //$NON-NLS-1$
                    ImageFileFactory.getInstance()
            );
        } catch (Exception e) {
            camos = null;
        }

        category = IPlayer.ROOT_CAMO;
        filename = IPlayer.NO_CAMO;
        colorIndex = -1;

        scrCamo = new JScrollPane();
        tableCamo = new JTable();
        camoModel = new CamoTableModel();
        camoMouseAdapter = new CamoTableMouseAdapter();
        tableCamo.setModel(camoModel);
        tableCamo.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tableCamo.setRowHeight(76);
        tableCamo.getColumnModel().getColumn(0).setCellRenderer(camoModel.getRenderer());
        tableCamo.addMouseListener(camoMouseAdapter);
        scrCamo.setViewportView(tableCamo);

        comboCategories = new JComboBox();
        DefaultComboBoxModel categoryModel = new DefaultComboBoxModel();
        categoryModel.addElement(IPlayer.NO_CAMO);
        if (camos != null) {
            if (camos.getItemNames("").hasNext()) { //$NON-NLS-1$
                categoryModel.addElement(IPlayer.ROOT_CAMO);
            }
            Iterator<String> names = camos.getCategoryNames();
            while (names.hasNext()) {
                String name = names.next();
                if (!"".equals(name)) { //$NON-NLS-1$
                    categoryModel.addElement(name);
                }
            }
        }
        comboCategories.setModel(categoryModel);
        comboCategories.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
                comboCategoriesItemStateChanged(evt);
            }
        });

        btnSelect = new JButton();
        btnSelect.setText(Messages.getString("CamoChoiceDialog.Select"));
        btnSelect.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                select();
            }
        });

        btnCancel = new JButton();
        btnCancel.setText(Messages.getString("CamoChoiceDialog.Cancel"));
        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                cancel();
            }
        });

        //set layout
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
        add(scrCamo, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1.0;
        getContentPane().add(comboCategories, c);


        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0.5;
        getContentPane().add(btnSelect, c);


        c = new GridBagConstraints();
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 0.5;
        getContentPane().add(btnCancel, c);

        pack();
    }

    private void cancel() {
        setVisible(false);
    }

    private void select() {
        category = camoModel.getCategory();
        if (category.equals(IPlayer.NO_CAMO) && entity == null) {
            colorIndex = tableCamo.getSelectedRow();
        }
        if (tableCamo.getSelectedRow() != -1) {
            filename = (String) camoModel.getValueAt(tableCamo.getSelectedRow(), 0);
        }
        if (sourceButton == null && entity != null) {
            if (category.equals(IPlayer.NO_CAMO)) {
                entity.setCamoCategory(null);
                entity.setCamoFileName(null);
            } else {
                entity.setCamoCategory(category);
                entity.setCamoFileName(filename);
            }
        } else {
            player.setColorIndex(colorIndex);
            player.setCamoCategory(category);
            player.setCamoFileName(filename);
            sourceButton.setIcon(generateIcon(category, filename));
        }
        select = true;
        setVisible(false);
    }

    private void comboCategoriesItemStateChanged(ItemEvent evt) {
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

    public int getColorIndex() {
        return colorIndex;
    }

    private void fillTable(String category) {
        camoModel.reset();
        camoModel.setCategory(category);
        if (IPlayer.NO_CAMO.equals(category)) {
            for (String color : IPlayer.colorNames) {
                camoModel.addCamo(color);
            }
        } else {
            // Translate the "root camo" category name.
            Iterator<String> camoNames;
            if (IPlayer.ROOT_CAMO.equals(category)) {
                camoNames = camos.getItemNames(""); //$NON-NLS-1$
            } else {
                camoNames = camos.getItemNames(category);
            }

            // Get the camo names for this category.
            while (camoNames.hasNext()) {
                camoModel.addCamo(camoNames.next());
            }
        }
        if (camoModel.getRowCount() > 0) {
            tableCamo.setRowSelectionInterval(0, 0);
        }
    }

    public void setPlayer(IPlayer p) {
        player = p;
        colorIndex = player.getColorIndex();
        category = player.getCamoCategory();
        filename = player.getCamoFileName();
        if (sourceButton != null) {
            sourceButton.setIcon(generateIcon(category, filename));
        }
        comboCategories.getModel().setSelectedItem(category);
        fillTable(category);
        int rowIndex = 0;
        for (int i = 0; i < camoModel.getRowCount(); i++) {
            if (((String) camoModel.getValueAt(i, 0)).equals(filename)) {
                rowIndex = i;
                break;
            }
        }
        tableCamo.setRowSelectionInterval(rowIndex, rowIndex);
    }

    public void setEntity(Entity e) {
        entity = e;
        if (entity == null) {
            return;
        }
        category = entity.getCamoCategory() == null ? player.getCamoCategory() : entity.getCamoCategory();
        filename = entity.getCamoFileName() == null ? player.getCamoFileName() : entity.getCamoFileName();
        comboCategories.getModel().setSelectedItem(category);
        fillTable(category);
        int rowIndex = 0;
        for (int i = 0; i < camoModel.getRowCount(); i++) {
            if (((String) camoModel.getValueAt(i, 0)).equals(filename)) {
                rowIndex = i;
                break;
            }
        }
        tableCamo.setRowSelectionInterval(rowIndex, rowIndex);
    }

    Icon generateIcon(String cat, String item) {
        String actualCat = cat;
        // Replace the ROOT_CAMO string with "".
        if (IPlayer.ROOT_CAMO.equals(actualCat)) {
            actualCat = ""; //$NON-NLS-1$
        }

        int colorInd = -1;
        //no camo, just color
        if (IPlayer.NO_CAMO.equals(actualCat)) {
            for (int color = 0; color < IPlayer.colorNames.length; color++) {
                if (IPlayer.colorNames[color].equals(item)) {
                    colorInd = color;
                    break;
                }
            }
            if (colorInd == -1) {
                colorInd = 0;
            }
            BufferedImage tempImage = new BufferedImage(84, 72,
                                                        BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = tempImage.createGraphics();
            graphics.setColor(PlayerColors.getColor(colorInd));
            graphics.fillRect(0, 0, 84, 72);
            return new ImageIcon(tempImage);
        }

        //an actual camo
        try {
            // We need to copy the image to make it appear.
            Image image = (Image) camos.getItem(actualCat, item);

            return new ImageIcon(image);
        } catch (Exception err) {
            // Print the stack trace and display the message.
            err.printStackTrace();
            JOptionPane
                    .showMessageDialog(
                            frame,
                            err.getMessage(),
                            Messages
                                    .getString("CamoChoiceDialog.error_getting_camo"), JOptionPane.ERROR_MESSAGE); //$NON-NLS-1$
            return null;
        }
    }


    /**
     * A table model for displaying camos
     */
    public class CamoTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 7298823592090412589L;

        private String[] columnNames;
        private String category;
        private ArrayList<String> names;
        private ArrayList<Image> images;

        public CamoTableModel() {
            columnNames = new String[]{"Camos"};
            category = IPlayer.NO_CAMO;
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
            category = IPlayer.NO_CAMO;
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

        public void addCamo(String name) {
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

        public CamoTableModel.Renderer getRenderer() {
            return new CamoTableModel.Renderer();
        }


        public class Renderer extends CamoPanel implements TableCellRenderer {

            private static final long serialVersionUID = 7483367362943393067L;

            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = this;
                setOpaque(true);
                String name = getValueAt(row, column).toString();
                setText(getValueAt(row, column).toString());
                setImage(category, name, row);
                if (isSelected) {
                    setBackground(new Color(220, 220, 220));
                } else {
                    setBackground(Color.WHITE);
                }

                return c;
            }
        }
    }

    public class CamoPanel extends JPanel {

        private static final long serialVersionUID = 6850715473654649719L;

        private JLabel lblImage;

        /**
         * Creates new form CamoPanel
         */
        public CamoPanel() {
            GridBagConstraints c = new GridBagConstraints();
            lblImage = new JLabel();

            setLayout(new GridBagLayout());

            lblImage.setText(""); // NOI18N

            c.gridx = 0;
            c.gridy = 0;
            c.fill = java.awt.GridBagConstraints.BOTH;
            c.weightx = 1.0;
            c.weighty = 1.0;
            add(lblImage, c);
        }

        public void setText(String text) {
            lblImage.setText(text);
        }

        public void setImage(String category, String name, int colorInd) {

            if (null == category) {
                return;
            }

            if (IPlayer.NO_CAMO.equals(category)) {
                if (colorInd == -1) {
                    colorInd = 0;
                }
                BufferedImage tempImage = new BufferedImage(84, 72,
                                                            BufferedImage.TYPE_INT_RGB);
                Graphics2D graphics = tempImage.createGraphics();
                graphics.setColor(PlayerColors.getColor(colorInd));
                graphics.fillRect(0, 0, 84, 72);
                lblImage.setIcon(new ImageIcon(tempImage));
                return;
            }

            // Try to get the camo file.
            try {

                // Translate the root camo directory name.
                if (IPlayer.ROOT_CAMO.equals(category))
                    category = ""; //$NON-NLS-1$
                Image camo = (Image) camos.getItem(category, name);
                lblImage.setIcon(new ImageIcon(camo));
            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    public class CamoTableMouseAdapter extends MouseInputAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {

            if (e.getClickCount() == 2) {
                select();
            }
        }
    }

    public boolean isSelect() {
        return select;
    }

}
