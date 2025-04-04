/*
 * MegaMek - Copyright (C) 2010 Ben Mazur (bmazur@sev.org)
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

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingConstants;

import megamek.client.ui.Messages;
import megamek.codeUtilities.MathUtility;
import megamek.common.Hex;
import megamek.common.Mek;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.Tank;

/**
 * @author beerockxs
 */
public class TurretFacingDialog extends JDialog implements ActionListener {
    @Serial
    private static final long serialVersionUID = -4509638026655222982L;
    private final JButton butOkay = new JButton(Messages.getString("Okay"));
    private final JButton butCancel = new JButton(Messages.getString("Cancel"));
    Mek mek;
    Tank tank;
    Mounted<?> turret;
    ButtonGroup buttonGroup = new ButtonGroup();
    ClientGUI clientgui;

    ArrayList<JRadioButton> facings = new ArrayList<>();

    public TurretFacingDialog(JFrame parent, Mek mek, Mounted<?> turret, ClientGUI clientgui) {
        super(parent, "Turret facing", false);
        super.setResizable(false);
        this.mek = mek;
        this.turret = turret;
        this.clientgui = clientgui;
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);

        for (int i = 0; i <= 5; i++) {
            JRadioButton button = new JRadioButton();
            button.setActionCommand(i + "");
            facings.add(button);
            buttonGroup.add(button);
        }
        int turretFacing = 0;
        if (turret.getType().hasFlag(MiscType.F_SHOULDER_TURRET) || turret.getType().hasFlag(MiscType.F_QUAD_TURRET)) {
            if (turret.getLocation() == Mek.LOC_LT) {
                for (Mounted<?> mount : mek.getEquipment()) {
                    if ((mount.getLocation() == Mek.LOC_LT) && mount.isMekTurretMounted()) {
                        turretFacing = mount.getFacing();
                        break;
                    }
                }
            } else if (turret.getLocation() == Mek.LOC_RT) {
                for (Mounted<?> mount : mek.getEquipment()) {
                    if ((mount.getLocation() == Mek.LOC_RT) && mount.isMekTurretMounted()) {
                        turretFacing = mount.getFacing();
                        break;
                    }
                }
            }
        } else if (turret.getType().hasFlag(MiscType.F_HEAD_TURRET)) {
            for (Mounted<?> mount : mek.getEquipment()) {
                if ((mount.getLocation() == Mek.LOC_HEAD) && mount.isMekTurretMounted()) {
                    turretFacing = mount.getFacing();
                    break;
                }
            }
        }
        int frontFacing = mek.getFacing();
        // select appropriate button if we already have a facing
        for (JRadioButton button : facings) {
            if (button.getActionCommand().equals(((frontFacing + turretFacing) % 6) + "")) {
                button.setSelected(true);
            }
        }
        setLayout(new BorderLayout());
        JPanel tempPanel = new JPanel(new BorderLayout());
        JPanel panNorth = new JPanel(new GridBagLayout());
        JPanel panWest = new JPanel(new BorderLayout());
        JPanel panEast = new JPanel(new BorderLayout());
        JPanel panSouth = new JPanel(new GridBagLayout());
        panNorth.add(facings.get(0));
        panSouth.add(facings.get(3));
        panWest.add(facings.get(5), BorderLayout.NORTH);
        panWest.add(facings.get(4), BorderLayout.SOUTH);
        panEast.add(facings.get(1), BorderLayout.NORTH);
        panEast.add(facings.get(2), BorderLayout.SOUTH);
        // for shoulder turrets, we need to disable the appropriate facings
        // opposite of the shoulder the turret is mounted on
        if (turret.getType().hasFlag(MiscType.F_SHOULDER_TURRET)) {
            if (turret.getLocation() == Mek.LOC_LT) {
                facings.get((frontFacing + 1) % 6).setEnabled(false);
                facings.get((frontFacing + 2) % 6).setEnabled(false);
            } else if (turret.getLocation() == Mek.LOC_RT) {
                facings.get((frontFacing + 4) % 6).setEnabled(false);
                facings.get((frontFacing + 5) % 6).setEnabled(false);
            }
        }
        if (turret.isHit()) {
            for (JRadioButton button : facings) {
                button.setEnabled(false);
            }
        }
        tempPanel.add(panNorth, BorderLayout.NORTH);
        tempPanel.add(panWest, BorderLayout.WEST);

        JLabel labImage = new JLabel();
        clientgui.loadPreviewImage(labImage, mek);
        Image mekImage = ((ImageIcon) labImage.getIcon()).getImage();
        Image hexImage = clientgui.getBoardView().getTilesetManager().baseFor(new Hex());
        BufferedImage toDraw = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = toDraw.createGraphics();
        g2.drawImage(hexImage, 0, 0, null);
        g2.drawImage(mekImage, 0, 0, null);
        labImage.setIcon(new ImageIcon(toDraw));
        labImage.setHorizontalAlignment(SwingConstants.CENTER);
        tempPanel.add(labImage, BorderLayout.CENTER);
        labImage.setOpaque(false);
        tempPanel.add(labImage, BorderLayout.CENTER);
        tempPanel.add(panEast, BorderLayout.EAST);
        tempPanel.add(panSouth, BorderLayout.SOUTH);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(butOkay);
        buttonPanel.add(butCancel);
        add(tempPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setLocation((parent.getLocation().x + (parent.getSize().width / 2)) - (getSize().width / 2),
              (parent.getLocation().y + (parent.getSize().height / 2)) - (getSize().height / 2));
    }

    public TurretFacingDialog(JFrame parent, Tank tank, ClientGUI clientgui) {
        super(parent, "Turret facing", false);
        super.setResizable(false);
        this.tank = tank;
        this.clientgui = clientgui;
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);

        for (int i = 0; i <= 5; i++) {
            JRadioButton button = new JRadioButton();
            button.setActionCommand(i + "");
            facings.add(button);
            buttonGroup.add(button);
        }
        int turretFacing = tank.getDualTurretFacing();
        int frontFacing = tank.getFacing();
        // select appropriate button if we already have a facing
        for (JRadioButton button : facings) {
            if (button.getActionCommand().equals(turretFacing + "")) {
                button.setSelected(true);
            }
        }
        setLayout(new BorderLayout());
        JPanel tempPanel = new JPanel(new BorderLayout());
        JPanel panNorth = new JPanel(new GridBagLayout());
        JPanel panWest = new JPanel(new BorderLayout());
        JPanel panEast = new JPanel(new BorderLayout());
        JPanel panSouth = new JPanel(new GridBagLayout());
        panNorth.add(facings.get(0));
        panSouth.add(facings.get(3));
        panWest.add(facings.get(5), BorderLayout.NORTH);
        panWest.add(facings.get(4), BorderLayout.SOUTH);
        panEast.add(facings.get(1), BorderLayout.NORTH);
        panEast.add(facings.get(2), BorderLayout.SOUTH);
        // for shoulder turrets, we need to disable the appropriate facings
        // opposite of the shoulder the turret is mounted on
        facings.get((frontFacing + 3) % 6).setEnabled(false);

        if (tank.isTurretLocked(tank.getLocTurret2())) {
            for (JRadioButton button : facings) {
                button.setEnabled(false);
            }
        }
        tempPanel.add(panNorth, BorderLayout.NORTH);
        tempPanel.add(panWest, BorderLayout.WEST);

        JLabel labImage = new JLabel();
        clientgui.loadPreviewImage(labImage, tank);
        Image mekImage = ((ImageIcon) labImage.getIcon()).getImage();
        Image hexImage = clientgui.getBoardView().getTilesetManager().baseFor(new Hex());
        BufferedImage toDraw = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = toDraw.createGraphics();
        g2.drawImage(hexImage, 0, 0, null);
        g2.drawImage(mekImage, 0, 0, null);
        labImage.setIcon(new ImageIcon(toDraw));
        labImage.setHorizontalAlignment(SwingConstants.CENTER);
        tempPanel.add(labImage, BorderLayout.CENTER);
        labImage.setOpaque(false);
        tempPanel.add(labImage, BorderLayout.CENTER);
        tempPanel.add(panEast, BorderLayout.EAST);
        tempPanel.add(panSouth, BorderLayout.SOUTH);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(butOkay);
        buttonPanel.add(butCancel);
        add(tempPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        pack();
        setLocation((parent.getLocation().x + (parent.getSize().width / 2)) - (getSize().width / 2),
              (parent.getLocation().y + (parent.getSize().height / 2)) - (getSize().height / 2));
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(butCancel)) {
            dispose();
        } else if (ae.getSource().equals(butOkay)) {
            int facing = MathUtility.parseInt(buttonGroup.getSelection().getActionCommand(), 0);
            int locToChange;
            if (mek != null) {
                facing = ((6 - mek.getFacing()) + facing) % 6;
                turret.setFacing(facing);
                clientgui.getClient().sendMountFacingChange(mek.getId(), mek.getEquipmentNum(turret), facing);
                if (turret.getLocation() == Mek.LOC_CT) {
                    locToChange = Mek.LOC_HEAD;
                } else {
                    locToChange = turret.getLocation();
                }

                Mounted<?> firstMountedWeapon = null; // Take note of the first weapon mounted on this turret.
                Mounted<?> currentSelectedWeapon = null; // Take note of current selected weapon.
                if (clientgui.getUnitDisplay() != null) {
                    currentSelectedWeapon = clientgui.getUnitDisplay().wPan.getSelectedWeapon();
                }

                for (Mounted<?> weapon : mek.getWeaponList()) {
                    if ((weapon.getLocation() == locToChange) && weapon.isMekTurretMounted()) {
                        weapon.setFacing(facing);
                        clientgui.getClient().sendMountFacingChange(mek.getId(), mek.getEquipmentNum(weapon), facing);

                        // Tag the first mounted weapon as a backup option to refresh after the turret
                        // rotation.
                        if (firstMountedWeapon == null) {
                            firstMountedWeapon = weapon;
                        }

                        // If the currently selected weapon is in the turret, refresh it by default.
                        if (mek.getEquipmentNum(currentSelectedWeapon) == mek.getEquipmentNum(weapon)) {
                            firstMountedWeapon = currentSelectedWeapon;
                        }
                    }
                }

                // Select the mounted weapon in the unit display to refresh the firing arch.
                if (clientgui.getUnitDisplay() != null) {
                    clientgui.getUnitDisplay().wPan.selectWeapon(mek.getEquipmentNum(firstMountedWeapon));
                }
            } else if (tank != null) {
                tank.setDualTurretOffset(((6 - tank.getFacing()) + facing) % 6);
                clientgui.getClient().sendUpdateEntity(tank);

                // `turret` is null here - need to find the first weapon ID of the 2nd turret.
                for (Mounted<?> weapon : tank.getWeaponList()) {
                    if (weapon.getLocation() == tank.getLocTurret2()) {
                        turret = weapon;
                        break;
                    }
                }

                // Select the turret in the unit display.
                if (clientgui.getUnitDisplay() != null) {
                    clientgui.getUnitDisplay().wPan.selectWeapon(tank.getEquipmentNum(turret));
                }
            }

            dispose();
        }
    }
}
