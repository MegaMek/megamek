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

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import megamek.client.ui.swing.tileset.MMStaticDirectoryManager;
import megamek.common.util.fileUtils.DirectoryItem;

/** 
 * A renderer for the list of camos. Shows the camo and the filename
 * minus the extension below the image. The filename will be shortened
 * by the JList if it's too long.
 * Sets the tooltip to show both the directory and the filename of the camo.
 * 
 * @author Juliez
 */
public class CamoRenderer extends JPanel implements ListCellRenderer<DirectoryItem> {

    private static final long serialVersionUID = -8141491753114750665L;
    
    /** Image file extensions, .jpg .jpeg .png .gif */ 
    private static final String[] EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif"};
    
    /** This JLabel displays the selectable image */
    private JLabel lblImage = new JLabel();
    
    /** This JLabel displays the name of the selectable image */
    private JLabel lblText = new JLabel();
    
    /** The tooltip to be displayed */
    private String tip = "";
    
    /** 
     * Creates a renderer for the list of camos. Shows the camo and the filename
     * minus the extension below the image. The filename will be shortened
     * by the JList if it's too long. Sets the tooltip to show both the 
     * directory and the filename of the camo.
     */ 
    public CamoRenderer() {
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        add(lblImage);
        add(lblText);
    }
    
    /** 
     * Sets the image based on the passed category and name from 
     * the displayed camouflage directory
     */
    private void setImage(String category, String name) {
        lblImage.setIcon(MMStaticDirectoryManager.getCamoIcon(category, name));
        tip = "<HTML><BODY>" + category + "<BR>" + name;
    }
    
    @Override
    public String getToolTipText() {
        return tip;
    }

    /** Sets the label text of the image, removing the file extension. */
    private void setText(String text) {
        // Remove the file extension
        for (String ext: EXTENSIONS) {
            if (text.toLowerCase().contains(ext)) {
                text = text.replace(ext, "");
                break;
            }
        }
        lblText.setText(text);
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
        setText(value.getItem());

        return this;
    }

}