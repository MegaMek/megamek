/*
 * Copyright (C) 2010 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2010-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package megamek.client.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.ArrayList;
import java.util.function.IntConsumer;
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
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.codeUtilities.MathUtility;
import megamek.common.Hex;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.units.DirectionalTorsoMountRules;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;

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
    boolean directionalMount;
    /**
     * When set, the dialog only picks a facing and hands it to this consumer on OK (vehicle main-turret mode - the
     * rotation is a turret twist declared by the attack display); when {@code null}, OK applies the facing itself.
     */
    private IntConsumer facingConsumer;
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
            if (turret.getLocation() == Mek.LOC_LEFT_TORSO) {
                for (Mounted<?> mount : mek.getEquipment()) {
                    if ((mount.getLocation() == Mek.LOC_LEFT_TORSO) && mount.isMekTurretMounted()) {
                        turretFacing = mount.getFacing();
                        break;
                    }
                }
            } else if (turret.getLocation() == Mek.LOC_RIGHT_TORSO) {
                for (Mounted<?> mount : mek.getEquipment()) {
                    if ((mount.getLocation() == Mek.LOC_RIGHT_TORSO) && mount.isMekTurretMounted()) {
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
        panNorth.add(facings.getFirst());
        panSouth.add(facings.get(3));
        panWest.add(facings.get(5), BorderLayout.NORTH);
        panWest.add(facings.get(4), BorderLayout.SOUTH);
        panEast.add(facings.get(1), BorderLayout.NORTH);
        panEast.add(facings.get(2), BorderLayout.SOUTH);
        // for shoulder turrets, we need to disable the appropriate facings
        // opposite of the shoulder the turret is mounted on
        if (turret.getType().hasFlag(MiscType.F_SHOULDER_TURRET)) {
            if (turret.getLocation() == Mek.LOC_LEFT_TORSO) {
                facings.get((frontFacing + 1) % 6).setEnabled(false);
                facings.get((frontFacing + 2) % 6).setEnabled(false);
            } else if (turret.getLocation() == Mek.LOC_RIGHT_TORSO) {
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
        Image hexImage = clientgui.getTilesetManager().baseFor(new Hex());
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
        panNorth.add(facings.getFirst());
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
        Image hexImage = clientgui.getTilesetManager().baseFor(new Hex());
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

    /**
     * Facing picker for a vehicle's main turret, whose rotation is a turret twist - the turret follows the unit's
     * secondary facing. The dialog only picks the facing: the chosen facing is handed to {@code facingConsumer}, which
     * declares the twist through the attack display (clearing pending attacks like any other twist). On a dual-turret
     * vehicle this is the rear turret; the front turret uses {@link #TurretFacingDialog(JFrame, Tank, ClientGUI)}.
     *
     * @param parent         the parent frame
     * @param tank           the vehicle whose main turret is being rotated
     * @param clientgui      the client GUI, used for the unit preview image
     * @param facingConsumer receives the chosen absolute facing (0-5) when the player confirms
     */
    public TurretFacingDialog(JFrame parent, Tank tank, ClientGUI clientgui, IntConsumer facingConsumer) {
        super(parent, "Turret facing", false);
        super.setResizable(false);
        this.tank = tank;
        this.clientgui = clientgui;
        this.facingConsumer = facingConsumer;
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);

        for (int i = 0; i <= 5; i++) {
            JRadioButton button = new JRadioButton();
            button.setActionCommand(i + "");
            facings.add(button);
            buttonGroup.add(button);
        }
        // Preselect the turret's current facing - the main turret follows the unit's secondary facing.
        for (JRadioButton button : facings) {
            if (button.getActionCommand().equals(tank.getSecondaryFacing() + "")) {
                button.setSelected(true);
            }
        }
        // The main turret rotates by turret twist; when the twist is unavailable (turret locked/jammed, or the
        // unit already twisted in an earlier phase this turn) the facing cannot be changed.
        if (!tank.canChangeSecondaryFacing()) {
            for (JRadioButton button : facings) {
                button.setEnabled(false);
            }
        }
        layoutFacingPicker(parent, tank);
    }

    /**
     * Lays out the shared six-facing picker around the unit's preview image and places the dialog. Expects the six
     * radio buttons in {@link #facings} to be created, preselected and enabled/disabled by the caller.
     *
     * @param parent the parent frame the dialog is centered on
     * @param unit   the unit whose preview image is shown in the center
     */
    private void layoutFacingPicker(JFrame parent, Entity unit) {
        setLayout(new BorderLayout());
        JPanel tempPanel = new JPanel(new BorderLayout());
        JPanel panNorth = new JPanel(new GridBagLayout());
        JPanel panWest = new JPanel(new BorderLayout());
        JPanel panEast = new JPanel(new BorderLayout());
        JPanel panSouth = new JPanel(new GridBagLayout());
        panNorth.add(facings.getFirst());
        panSouth.add(facings.get(3));
        panWest.add(facings.get(5), BorderLayout.NORTH);
        panWest.add(facings.get(4), BorderLayout.SOUTH);
        panEast.add(facings.get(1), BorderLayout.NORTH);
        panEast.add(facings.get(2), BorderLayout.SOUTH);
        tempPanel.add(panNorth, BorderLayout.NORTH);
        tempPanel.add(panWest, BorderLayout.WEST);

        JLabel labImage = new JLabel();
        clientgui.loadPreviewImage(labImage, unit);
        Image unitImage = ((ImageIcon) labImage.getIcon()).getImage();
        Image hexImage = clientgui.getTilesetManager().baseFor(new Hex());
        BufferedImage toDraw = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = toDraw.createGraphics();
        graphics.drawImage(hexImage, 0, 0, null);
        graphics.drawImage(unitImage, 0, 0, null);
        graphics.dispose();
        labImage.setIcon(new ImageIcon(toDraw));
        labImage.setHorizontalAlignment(SwingConstants.CENTER);
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

    /**
     * Facing picker for a Directional Torso Mount (BMM p.83). The 2-point mount allows only the forward and rear
     * facings; the 3-point quad turret allows all six. Reuses the same six-facing layout as the turret dialogs.
     *
     * @param parent                 the parent frame
     * @param mek                    the unit carrying the mount
     * @param directionalMountWeapon the weapon whose mount facing is being set
     * @param clientgui              the client GUI, used to send the facing change and refresh the firing arc
     * @param isDirectionalMount     marker distinguishing this from the mek-turret constructor; always {@code true}
     */
    public TurretFacingDialog(JFrame parent, Mek mek, WeaponMounted directionalMountWeapon, ClientGUI clientgui,
          boolean isDirectionalMount) {
        super(parent, "Directional Torso Mount facing", false);
        super.setResizable(false);
        this.mek = mek;
        this.turret = directionalMountWeapon;
        this.clientgui = clientgui;
        this.directionalMount = isDirectionalMount;
        butOkay.addActionListener(this);
        butCancel.addActionListener(this);

        for (int i = 0; i <= 5; i++) {
            JRadioButton button = new JRadioButton();
            button.setActionCommand(i + "");
            facings.add(button);
            buttonGroup.add(button);
        }
        int frontFacing = directionalMountBaseFacing();
        int mountFacing = directionalMountWeapon.getDirectionalMountFacing();
        for (JRadioButton button : facings) {
            if (button.getActionCommand().equals(((frontFacing + mountFacing) % 6) + "")) {
                button.setSelected(true);
            }
        }
        setLayout(new BorderLayout());
        JPanel tempPanel = new JPanel(new BorderLayout());
        JPanel panNorth = new JPanel(new GridBagLayout());
        JPanel panWest = new JPanel(new BorderLayout());
        JPanel panEast = new JPanel(new BorderLayout());
        JPanel panSouth = new JPanel(new GridBagLayout());
        panNorth.add(facings.getFirst());
        panSouth.add(facings.get(3));
        panWest.add(facings.get(5), BorderLayout.NORTH);
        panWest.add(facings.get(4), BorderLayout.SOUTH);
        panEast.add(facings.get(1), BorderLayout.NORTH);
        panEast.add(facings.get(2), BorderLayout.SOUTH);
        // The 2-point mount may only face forward or rear; disable the other four absolute facings.
        if (!directionalMountWeapon.hasDirectional360TorsoMount()) {
            for (int offset = 1; offset <= 5; offset++) {
                if (offset != 3) {
                    facings.get((frontFacing + offset) % 6).setEnabled(false);
                }
            }
        }
        // Unavailable when destroyed by damage or already refaced in an earlier phase this turn (once per turn).
        if (directionalMountWeapon.isDirectionalMountLocked()
              || directionalMountWeapon.isDirectionalMountAlreadyFlipped()) {
            for (JRadioButton button : facings) {
                button.setEnabled(false);
            }
        }
        tempPanel.add(panNorth, BorderLayout.NORTH);
        tempPanel.add(panWest, BorderLayout.WEST);

        JLabel labImage = new JLabel();
        clientgui.loadPreviewImage(labImage, mek);
        Image mekImage = ((ImageIcon) labImage.getIcon()).getImage();
        Image hexImage = clientgui.getTilesetManager().baseFor(new Hex());
        BufferedImage toDraw = new BufferedImage(84, 72, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = toDraw.createGraphics();
        g2.drawImage(hexImage, 0, 0, null);
        // The preview image always points north; rotate it to the Mek's actual board facing so the six facing
        // buttons line up with what the player sees on the map (otherwise front/rear appears flipped).
        g2.rotate(Math.toRadians(mek.getFacing() * 60.0), toDraw.getWidth() / 2.0, toDraw.getHeight() / 2.0);
        g2.drawImage(mekImage, 0, 0, null);
        g2.dispose();
        labImage.setIcon(new ImageIcon(toDraw));
        labImage.setHorizontalAlignment(SwingConstants.CENTER);
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

    /**
     * @return the base facing a Directional Torso Mount weapon's arc is measured against - the secondary facing when
     *       the weapon is a secondary-arc (torso-twist) weapon, otherwise the primary facing. This matches
     *       {@link megamek.common.compute.TurretFacing#weaponFacing(megamek.common.units.Entity, int)}, so the radio
     *       selection and the offset sent to the server stay consistent with the effective arc even when twisted.
     */
    private int directionalMountBaseFacing() {
        int weaponNumber = mek.getEquipmentNum(turret);
        return mek.isSecondaryArcWeapon(weaponNumber) ? mek.getSecondaryFacing() : mek.getFacing();
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource().equals(butCancel)) {
            dispose();
        } else if (ae.getSource().equals(butOkay)) {
            int facing = MathUtility.parseInt(buttonGroup.getSelection().getActionCommand(), 0);
            int locToChange;
            if (facingConsumer != null) {
                // Vehicle main-turret mode: the attack display declares the facing as a turret twist; nothing is
                // sent from the dialog itself.
                facingConsumer.accept(facing);
                dispose();
                return;
            }
            if (directionalMount && (mek != null)) {
                // The mount offset composes with torso twist, so it is measured against the weapon's base facing
                // (secondary facing when twisted) - the same base TurretFacing.weaponFacing() uses for the arc.
                int offset = ((6 - directionalMountBaseFacing()) + facing) % 6;
                // The whole mount (every directional weapon in this location) shares one facing; rotate them together.
                DirectionalTorsoMountRules.setMountFacing(mek, turret.getLocation(), offset);
                clientgui.getClient().sendMountFacingChange(mek.getId(), mek.getEquipmentNum(turret), offset);
                if (clientgui.getUnitDisplay() != null) {
                    clientgui.getUnitDisplay().wPan.selectWeapon(mek.getEquipmentNum(turret));
                }
                dispose();
                return;
            }
            if (mek != null) {
                facing = ((6 - mek.getFacing()) + facing) % 6;
                turret.setFacing(facing);
                clientgui.getClient().sendMountFacingChange(mek.getId(), mek.getEquipmentNum(turret), facing);
                if (turret.getLocation() == Mek.LOC_CENTER_TORSO) {
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
