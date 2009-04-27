/*
 * MegaMek - Copyright (C) 2004 Ben Mazur (bmazur@sev.org)
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

package megamek.test.client;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import megamek.client.ui.AWT.CamoChoiceDialog;
import megamek.client.ui.AWT.util.PlayerColors;
import megamek.client.ui.AWT.widget.ImageButton;
import megamek.common.Player;

/**
 * This class will test the <code>CamoChoiceDialog</code> by displaying it.
 * Created on January 19, 2004
 * 
 * @author James Damour
 * @version 1
 */
public class TestCamoChoice extends Frame implements ActionListener,
        ItemListener {

    /**
     * 
     */
    private static final long serialVersionUID = 8394784210464718865L;
    CamoChoiceDialog dialog = null;
    final Color defaultBG;
    ImageButton butCamo = null;

    public static void main(String[] args) {

        final TestCamoChoice frame = new TestCamoChoice();

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                frame.dispose();
                System.exit(1);
            }
        });
        frame.setVisible(true);

    }

    public void dispose() {
        dialog.dispose();
        super.dispose();
    }

    private TestCamoChoice() {
        super("Test Camo Choice");

        dialog = new CamoChoiceDialog(this);
        dialog.addItemListener(this);
        dialog.setCategory("flashhawk2k.zip");
        dialog.setItemName("camo1.jpg");

        butCamo = new ImageButton();
        Image[] array = (Image[]) dialog.getSelectedObjects();
        if (null != array)
            butCamo.setImage(array[0]);
        butCamo.setLabel("Choose Camo...");
        butCamo.setPreferredSize(84, 72);
        butCamo.addActionListener(this);
        defaultBG = butCamo.getBackground();
        this.add(butCamo);
        this.pack();
    }

    public void actionPerformed(ActionEvent event) {
        dialog.setVisible(true);
    }

    public void itemStateChanged(ItemEvent event) {

        // Get the camo that was selected.
        Image image = (Image) event.getItem();

        // If the image is null, a color was selected instead.
        if (null == image) {
            String item = dialog.getItemName();
            for (int color = 0; color < Player.colorNames.length; color++) {
                if (Player.colorNames[color].equals(item)) {
                    butCamo.setBackground(PlayerColors.getColor(color));
                    break;
                }
            }
        }

        // We need to copy the image to make it appear.
        else {
            butCamo.setBackground(defaultBG);
        }

        // Update the butCamo's image.
        butCamo.setImage(image);
    }

}
