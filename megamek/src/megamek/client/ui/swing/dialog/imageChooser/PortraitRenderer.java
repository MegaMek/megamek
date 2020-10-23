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
package megamek.client.ui.swing.dialog.imageChooser;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.common.util.fileUtils.DirectoryItem;

/** 
 * A renderer for the list of portraits. Shows (only) the portrait.
 * Sets the tooltip to the filename of the portrait. 
 * 
 * @author Juliez
 */
public class PortraitRenderer extends JPanel implements ListCellRenderer<DirectoryItem> {

    private static final long serialVersionUID = 7406410632181369275L;
    
    /** This JLabel displays the selectable image */
    private JLabel lblImage = new JLabel();
    
    /** The tooltip to be displayed */
    private String tip = "";

    /** 
     * A renderer for the list of portraits. Shows (only) the portrait.
     * Sets the tooltip to the filename of the portrait. 
     */
    public PortraitRenderer() {
        setBorder(new EmptyBorder(10, 10, 10, 10));
        add(lblImage);
    }
    
    /** 
     * Sets the image based on the passed category and name from 
     * the displayed portrait directory
     */
    public void setImage(String category, String name) {
        lblImage.setIcon(MMStaticDirectoryManager.getPortraitIcon(category, name));
        tip = name;
    }
    
    @Override
    public String getToolTipText() {
        return tip;
    }
    
    @Override
    public Component getListCellRendererComponent(JList<? extends DirectoryItem> list, 
            DirectoryItem value, int index, boolean isSelected, boolean cellHasFocus) {

        if (isSelected) {
            setBackground(UIManager.getColor("Table.selectionBackground"));
            setForeground(UIManager.getColor("Table.selectionForeground"));
        } else {
            setBackground(UIManager.getColor("Table.background"));
            setForeground(UIManager.getColor("Table.foreground"));
        }
        setImage(value.getCategory(), value.getItem());

        return this;
    }
}
