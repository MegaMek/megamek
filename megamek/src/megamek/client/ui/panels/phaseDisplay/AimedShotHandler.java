/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.panels.phaseDisplay;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;

import megamek.client.ui.Messages;
import megamek.client.ui.clientGUI.ClientGUI;
import megamek.client.ui.dialogs.phaseDisplay.AimedShotDialog;
import megamek.client.ui.widget.IndexedRadioButton;
import megamek.common.LosEffects;
import megamek.common.ToHitData;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.compute.Compute;
import megamek.common.compute.ComputeSideTable;
import megamek.common.enums.AimingMode;
import megamek.common.equipment.GunEmplacement;
import megamek.common.equipment.WeaponMounted;
import megamek.common.units.Entity;
import megamek.common.units.LargeSupportTank;
import megamek.common.units.Mek;
import megamek.common.units.ProtoMek;
import megamek.common.units.SuperHeavyTank;
import megamek.common.units.Tank;

public class AimedShotHandler implements ActionListener, ItemListener {
    private final FiringDisplay firingDisplay;

    private int aimingAt = Entity.LOC_NONE;

    private AimingMode aimingMode = AimingMode.NONE;

    private int partialCover = LosEffects.COVER_NONE;

    private AimedShotDialog asd;

    public AimedShotHandler(FiringDisplay firingDisplay) {
        this.firingDisplay = firingDisplay;
        // ignore
    }

    public void showDialog() {
        if (asd != null) {
            AimingMode oldAimingMode = aimingMode;
            closeDialog();
            aimingMode = oldAimingMode;
        }

        if (!getAimingMode().isNone()) {
            String[] options;
            boolean[] enabled;

            if (this.firingDisplay.getTarget().isBuildingEntityOrGunEmplacement()) {
                return;
            }
            if (this.firingDisplay.getTarget() instanceof Entity) {
                options = ((Entity) this.firingDisplay.getTarget()).getLocationNames();
                enabled = createEnabledMask(options.length);
            } else {
                return;
            }
            if (this.firingDisplay.getTarget() instanceof Mek) {
                if (aimingMode.isImmobile()) {
                    aimingAt = Mek.LOC_HEAD;
                } else if (aimingMode.isTargetingComputer()) {
                    aimingAt = Mek.LOC_CENTER_TORSO;
                }
            } else if (this.firingDisplay.getTarget() instanceof Tank) {
                int side = ComputeSideTable.sideTable(this.firingDisplay.currentEntity(), this.firingDisplay.getTarget());
                if (this.firingDisplay.getTarget() instanceof LargeSupportTank) {
                    if (side == ToHitData.SIDE_FRONT_LEFT) {
                        aimingAt = LargeSupportTank.LOC_FRONT_LEFT;
                    } else if (side == ToHitData.SIDE_FRONT_RIGHT) {
                        aimingAt = LargeSupportTank.LOC_FRONT_RIGHT;
                    } else if (side == ToHitData.SIDE_REAR_RIGHT) {
                        aimingAt = LargeSupportTank.LOC_REAR_RIGHT;
                    } else if (side == ToHitData.SIDE_REAR_LEFT) {
                        aimingAt = LargeSupportTank.LOC_REAR_LEFT;
                    }
                }
                if (side == ToHitData.SIDE_LEFT) {
                    aimingAt = Tank.LOC_LEFT;
                }
                if (side == ToHitData.SIDE_RIGHT) {
                    aimingAt = Tank.LOC_RIGHT;
                }
                if (side == ToHitData.SIDE_REAR) {
                    aimingAt = (this.firingDisplay.getTarget() instanceof LargeSupportTank) ? LargeSupportTank.LOC_REAR
                          : this.firingDisplay.getTarget() instanceof SuperHeavyTank ? SuperHeavyTank.LOC_REAR
                          : Tank.LOC_REAR;
                }
                if (side == ToHitData.SIDE_FRONT) {
                    aimingAt = Tank.LOC_FRONT;
                }
            } else if (this.firingDisplay.getTarget() instanceof ProtoMek) {
                aimingAt = ProtoMek.LOC_TORSO;
            } else if (this.firingDisplay.getTarget() instanceof BattleArmor) {
                aimingAt = BattleArmor.LOC_TROOPER_1;
            } else {
                // no aiming allowed for MekWarrior or BattleArmor
                return;
            }

            asd = new AimedShotDialog(
                  this.firingDisplay.getClientGUI().getFrame(),
                  Messages.getString("FiringDisplay.AimedShotDialog.title"),
                  Messages.getString("FiringDisplay.AimedShotDialog.message"),
                  options, enabled, aimingAt,
                  (ClientGUI) this.firingDisplay.getClientGUI(), this.firingDisplay.getTarget(),
                  this, this);

            asd.setVisible(true);
            this.firingDisplay.updateTarget();
        }
    }

    private boolean[] createEnabledMask(int length) {
        boolean[] mask = new boolean[length];

        Arrays.fill(mask, true);

        int side = ComputeSideTable.sideTable(firingDisplay.currentEntity(), firingDisplay.getTarget());

        // on a tank, remove turret if its missing
        // also, remove body
        if (firingDisplay.getTarget() instanceof Tank) {
            mask[Tank.LOC_BODY] = false;
            Tank tank = (Tank) this.firingDisplay.getTarget();
            if (tank.hasNoTurret()) {
                int turretLoc = tank.getLocTurret();
                mask[turretLoc] = false;
            }
            // remove non-visible sides
            if (firingDisplay.getTarget() instanceof LargeSupportTank) {
                if (side == ToHitData.SIDE_FRONT) {
                    mask[LargeSupportTank.LOC_FRONT_LEFT] = false;
                    mask[LargeSupportTank.LOC_REAR_LEFT] = false;
                    mask[LargeSupportTank.LOC_REAR_RIGHT] = false;
                    mask[LargeSupportTank.LOC_REAR] = false;
                }
                if (side == ToHitData.SIDE_FRONT_LEFT) {
                    mask[LargeSupportTank.LOC_FRONT_RIGHT] = false;
                    mask[LargeSupportTank.LOC_REAR_LEFT] = false;
                    mask[LargeSupportTank.LOC_REAR_RIGHT] = false;
                    mask[LargeSupportTank.LOC_REAR] = false;
                }
                if (side == ToHitData.SIDE_FRONT_RIGHT) {
                    mask[LargeSupportTank.LOC_FRONT_LEFT] = false;
                    mask[LargeSupportTank.LOC_REAR_LEFT] = false;
                    mask[LargeSupportTank.LOC_REAR_RIGHT] = false;
                    mask[LargeSupportTank.LOC_REAR] = false;
                }
                if (side == ToHitData.SIDE_REAR_RIGHT) {
                    mask[Tank.LOC_FRONT] = false;
                    mask[LargeSupportTank.LOC_FRONT_LEFT] = false;
                    mask[LargeSupportTank.LOC_FRONT_RIGHT] = false;
                    mask[LargeSupportTank.LOC_REAR_LEFT] = false;
                }
                if (side == ToHitData.SIDE_REAR_LEFT) {
                    mask[Tank.LOC_FRONT] = false;
                    mask[LargeSupportTank.LOC_FRONT_LEFT] = false;
                    mask[LargeSupportTank.LOC_FRONT_RIGHT] = false;
                    mask[LargeSupportTank.LOC_REAR_RIGHT] = false;
                }
                if (side == ToHitData.SIDE_REAR) {
                    mask[Tank.LOC_FRONT] = false;
                    mask[LargeSupportTank.LOC_FRONT_LEFT] = false;
                    mask[LargeSupportTank.LOC_FRONT_RIGHT] = false;
                    mask[LargeSupportTank.LOC_REAR_RIGHT] = false;
                }
            } else if (this.firingDisplay.getTarget() instanceof SuperHeavyTank) {
                if (side == ToHitData.SIDE_FRONT) {
                    mask[SuperHeavyTank.LOC_FRONT_LEFT] = false;
                    mask[SuperHeavyTank.LOC_REAR_LEFT] = false;
                    mask[SuperHeavyTank.LOC_REAR_RIGHT] = false;
                    mask[SuperHeavyTank.LOC_REAR] = false;
                }
                if (side == ToHitData.SIDE_FRONT_LEFT) {
                    mask[SuperHeavyTank.LOC_FRONT_RIGHT] = false;
                    mask[SuperHeavyTank.LOC_REAR_LEFT] = false;
                    mask[SuperHeavyTank.LOC_REAR_RIGHT] = false;
                    mask[SuperHeavyTank.LOC_REAR] = false;
                }
                if (side == ToHitData.SIDE_FRONT_RIGHT) {
                    mask[SuperHeavyTank.LOC_FRONT_LEFT] = false;
                    mask[SuperHeavyTank.LOC_REAR_LEFT] = false;
                    mask[SuperHeavyTank.LOC_REAR_RIGHT] = false;
                    mask[SuperHeavyTank.LOC_REAR] = false;
                }
                if (side == ToHitData.SIDE_REAR_RIGHT) {
                    mask[Tank.LOC_FRONT] = false;
                    mask[SuperHeavyTank.LOC_FRONT_LEFT] = false;
                    mask[SuperHeavyTank.LOC_FRONT_RIGHT] = false;
                    mask[SuperHeavyTank.LOC_REAR_LEFT] = false;
                }
                if (side == ToHitData.SIDE_REAR_LEFT) {
                    mask[Tank.LOC_FRONT] = false;
                    mask[SuperHeavyTank.LOC_FRONT_LEFT] = false;
                    mask[SuperHeavyTank.LOC_FRONT_RIGHT] = false;
                    mask[SuperHeavyTank.LOC_REAR_RIGHT] = false;
                }
                if (side == ToHitData.SIDE_REAR) {
                    mask[Tank.LOC_FRONT] = false;
                    mask[SuperHeavyTank.LOC_FRONT_LEFT] = false;
                    mask[SuperHeavyTank.LOC_FRONT_RIGHT] = false;
                    mask[SuperHeavyTank.LOC_REAR_RIGHT] = false;
                }
            } else {
                if (side == ToHitData.SIDE_LEFT) {
                    mask[Tank.LOC_RIGHT] = false;
                }
                if (side == ToHitData.SIDE_RIGHT) {
                    mask[Tank.LOC_LEFT] = false;
                }
                if (side == ToHitData.SIDE_REAR) {
                    mask[Tank.LOC_FRONT] = false;
                }
                if (side == ToHitData.SIDE_FRONT) {
                    mask[Tank.LOC_REAR] = false;
                }
            }
        }

        // remove main gun on protos that don't have one
        if (this.firingDisplay.getTarget() instanceof ProtoMek) {
            if (!((ProtoMek) this.firingDisplay.getTarget()).hasMainGun()) {
                mask[ProtoMek.LOC_MAIN_GUN] = false;
            }
        }

        // remove squad location on BAs
        // also remove dead troopers
        if (this.firingDisplay.getTarget() instanceof BattleArmor) {
            mask[BattleArmor.LOC_SQUAD] = false;
        }

        // remove locations hidden by partial cover
        if ((partialCover & LosEffects.COVER_HORIZONTAL) != 0) {
            mask[Mek.LOC_LEFT_LEG] = false;
            mask[Mek.LOC_RIGHT_LEG] = false;
        }
        if (side == ToHitData.SIDE_FRONT) {
            if ((partialCover & LosEffects.COVER_LOW_LEFT) != 0) {
                mask[Mek.LOC_RIGHT_LEG] = false;
            }
            if ((partialCover & LosEffects.COVER_LOW_RIGHT) != 0) {
                mask[Mek.LOC_LEFT_LEG] = false;
            }
            if ((partialCover & LosEffects.COVER_LEFT) != 0) {
                mask[Mek.LOC_RIGHT_ARM] = false;
                mask[Mek.LOC_RIGHT_TORSO] = false;
            }
            if ((partialCover & LosEffects.COVER_RIGHT) != 0) {
                mask[Mek.LOC_LEFT_ARM] = false;
                mask[Mek.LOC_LEFT_TORSO] = false;
            }
        } else {
            if ((partialCover & LosEffects.COVER_LOW_LEFT) != 0) {
                mask[Mek.LOC_LEFT_LEG] = false;
            }
            if ((partialCover & LosEffects.COVER_LOW_RIGHT) != 0) {
                mask[Mek.LOC_RIGHT_LEG] = false;
            }
            if ((partialCover & LosEffects.COVER_LEFT) != 0) {
                mask[Mek.LOC_LEFT_ARM] = false;
                mask[Mek.LOC_LEFT_TORSO] = false;
            }
            if ((partialCover & LosEffects.COVER_RIGHT) != 0) {
                mask[Mek.LOC_RIGHT_ARM] = false;
                mask[Mek.LOC_RIGHT_TORSO] = false;
            }
        }

        if (aimingMode.isTargetingComputer()) {
            // Can't target head with targeting computer.
            mask[Mek.LOC_HEAD] = false;
        }
        return mask;
    }

    public void closeDialog() {
        if (asd != null) {
            aimingAt = Entity.LOC_NONE;
            aimingMode = AimingMode.NONE;
            asd.dispose();
            asd = null;
            this.firingDisplay.updateTarget();
        }
    }

    /**
     * Enables the radiobuttons in the dialog.
     */
    public void setEnableAll(boolean enableAll) {
        if (asd != null) {
            asd.setEnableAll(enableAll);
        }
    }

    public void setPartialCover(int partialCover) {
        this.partialCover = partialCover;
    }

    public int getAimingAt() {
        return aimingAt;
    }

    public AimingMode getAimingMode() {
        return aimingMode;
    }

    /**
     * Returns the name of aimed location.
     */
    public String getAimingLocation() {
        if ((this.firingDisplay.getTarget() != null) && (aimingAt != Entity.LOC_NONE)
              && !getAimingMode().isNone()) {
            if (this.firingDisplay.getTarget().isBuildingEntityOrGunEmplacement()) {
                return GunEmplacement.HIT_LOCATION_NAMES[aimingAt];
            } else if (this.firingDisplay.getTarget() instanceof Entity) {
                return ((Entity) this.firingDisplay.getTarget()).getLocationName(aimingAt);
            }
        }
        return null;
    }

    /**
     * Sets the aiming mode, depending on the target and the attacker. Against immobile meks, targeting computer aiming
     * mode will be used if turned on. This is a hack, but it's the resolution suggested by the bug submitter, and I
     * don't think it's half bad.
     */

    public void setAimingMode() {
        boolean allowAim;

        // BA cannot use aimed shots - their anti-mek attacks (swarm/leg) don't support
        // aimed shots, and EI's +2 aimed shot penalty makes it impractical for ranged weapons.
        // This prevents the confusing aimed shot dialog from appearing for BA.
        Entity attacker = this.firingDisplay.currentEntity();
        if (attacker instanceof BattleArmor) {
            aimingMode = AimingMode.NONE;
            return;
        }

        // TC against a mek
        allowAim = ((this.firingDisplay.getTarget() != null) && (attacker != null)
              && attacker.hasAimModeTargComp() && ((this.firingDisplay.getTarget() instanceof Mek)
              || (this.firingDisplay.getTarget() instanceof Tank)
              || (this.firingDisplay.getTarget() instanceof BattleArmor)
              || (this.firingDisplay.getTarget() instanceof ProtoMek)));
        if (allowAim) {
            aimingMode = AimingMode.TARGETING_COMPUTER;
            return;
        }
        // immobile mek or gun emplacement
        allowAim = ((this.firingDisplay.getTarget() != null)
              && ((this.firingDisplay.getTarget().isImmobile()
              && ((this.firingDisplay.getTarget() instanceof Mek)
              || (this.firingDisplay.getTarget() instanceof Tank)))
              || (this.firingDisplay.getTarget().isBuildingEntityOrGunEmplacement())));
        if (allowAim) {
            aimingMode = AimingMode.IMMOBILE;
            return;
        }

        aimingMode = AimingMode.NONE;
    }

    /**
     * @return if a hit location currently selected.
     */
    public boolean isAimingAtLocation() {
        return aimingAt != Entity.LOC_NONE;
    }

    /**
     * should aimed shots be allowed with the passed weapon
     *
     */
    public boolean allowAimedShotWith(WeaponMounted weapon) {
        return Compute.allowAimedShotWith(weapon, aimingMode);
    }

    /**
     * ActionListener, listens to the button in the dialog.
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        closeDialog();
    }

    /**
     * ItemListener, listens to the radiobuttons in the dialog.
     */
    @Override
    public void itemStateChanged(ItemEvent ev) {
        IndexedRadioButton icb = (IndexedRadioButton) ev.getSource();
        aimingAt = icb.getIndex();
        this.firingDisplay.updateTarget();
    }
}
