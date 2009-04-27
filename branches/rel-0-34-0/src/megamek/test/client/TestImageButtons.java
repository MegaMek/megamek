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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.List;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import megamek.client.ui.AWT.widget.ImageButton;
import megamek.client.ui.AWT.widget.SizedButton;

public class TestImageButtons {

    private ImageButton camo;
    private Dialog dialog;
    private List list;
    private ImageButton imgButton;
    private SizedButton color;

    private TestImageButtons() {

        // Construct a window.
        Frame frame = new Frame("Testing Image Buttons");

        // Close the window at WM_CLOSE.
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        // Display an image button to show the camo selection dialog.
        camo = new ImageButton();
        camo.setLabel("Choose...");
        camo.setPreferredSize(84, 72);
        camo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                dialog.setVisible(true);
            }
        });
        camo.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                if (KeyEvent.VK_ENTER == event.getKeyCode()) {
                    dialog.setVisible(true);
                }
            }
        });
        frame.add(camo);

        // Construct a dialog.
        dialog = new Dialog(frame, "Choose a pattern", true);
        dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dialog.setVisible(false);
            }
        });

        // Create a main panel for the dialog.
        Panel main = new Panel();
        main.setLayout(new BorderLayout());
        dialog.add(main);

        // Create a list as the center of the dialog.
        list = new List(5);
        list.add("data/images/camo/Wood1.jpg");
        list.add("data/images/camo/Wood2.jpg");
        list.add("data/images/camo/Urban.jpg");
        list.add("data/images/camo/Winter.jpg");
        list.select(0);
        list.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                imgButton.setImage(Toolkit.getDefaultToolkit()
                        .getImage(
                                (String) event.getItemSelectable()
                                        .getSelectedObjects()[0]));
            }
        });
        list.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                if (KeyEvent.VK_ENTER == event.getKeyCode()
                        || KeyEvent.VK_SPACE == event.getKeyCode()) {
                    Dimension size = imgButton.getPreferredSize();
                    camo.setBackground(imgButton.getBackground());
                    camo.setLabel(imgButton.getLabel());
                    camo.setImage(imgButton.getImage().getScaledInstance(
                            size.width, size.height, Image.SCALE_FAST));
                    dialog.setVisible(false);
                }
            }
        });
        main.add(list, BorderLayout.CENTER);

        // Create a panel for the buttons.
        Panel panel = new Panel();
        panel.setLayout(new GridLayout(0, 1));
        dialog.add(panel, BorderLayout.EAST);

        // Create a "camo" button on the panel.
        imgButton = new ImageButton();
        imgButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                Dimension size = imgButton.getPreferredSize();
                camo.setBackground(imgButton.getBackground());
                camo.setLabel(imgButton.getLabel());
                camo.setImage(imgButton.getImage().getScaledInstance(
                        size.width, size.height, Image.SCALE_FAST));
                dialog.setVisible(false);
            }
        });
        imgButton.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                if (KeyEvent.VK_ENTER == event.getKeyCode()) {
                    Dimension size = imgButton.getPreferredSize();
                    camo.setBackground(imgButton.getBackground());
                    camo.setLabel(imgButton.getLabel());
                    camo.setImage(imgButton.getImage().getScaledInstance(
                            size.width, size.height, Image.SCALE_FAST));
                    dialog.setVisible(false);
                }
            }
        });
        imgButton.setImage(Toolkit.getDefaultToolkit().getImage(
                "data/images/camo/Wood1.jpg"));
        panel.add(imgButton);

        // Create a "no camo" button on the panel.
        color = new SizedButton("No Camo", new Dimension(84, 72));
        color.setBackground(Color.green);
        color.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                camo.setBackground(color.getBackground());
                camo.setLabel(color.getLabel());
                camo.setImage(null);
                dialog.setVisible(false);
            }
        });
        color.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                if (KeyEvent.VK_ENTER == event.getKeyCode()) {
                    camo.setBackground(color.getBackground());
                    camo.setLabel(color.getLabel());
                    camo.setImage(null);
                    dialog.setVisible(false);
                }
            }
        });
        panel.add(color);

        // Show the window.
        dialog.pack();
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        new TestImageButtons();
    }

}
