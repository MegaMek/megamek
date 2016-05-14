package megamek.client.ui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import megamek.client.ui.Messages;
import megamek.client.ui.swing.widget.IndexedRadioButton;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.GunEmplacement;
import megamek.common.IAimingModes;
import megamek.common.LargeSupportTank;
import megamek.common.LosEffects;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Protomech;
import megamek.common.SuperHeavyTank;
import megamek.common.Tank;
import megamek.common.ToHitData;

class AimedShotHandler implements ActionListener, ItemListener {
    /**
     * 
     */
    private final FiringDisplay firingDisplay;

    private int aimingAt = Entity.LOC_NONE;

    private int aimingMode = IAimingModes.AIM_MODE_NONE;

    private int partialCover = LosEffects.COVER_NONE;

    private AimedShotDialog asd;

    public AimedShotHandler(FiringDisplay firingDisplay) {
        this.firingDisplay = firingDisplay;
        // ignore
    }

    public void showDialog() {
        if (asd != null) {
            int oldAimingMode = aimingMode;
            closeDialog();
            aimingMode = oldAimingMode;
        }

        if (inAimingMode()) {
            String[] options;
            boolean[] enabled;

            if (this.firingDisplay.target instanceof GunEmplacement) {
                return;
            }
            if (this.firingDisplay.target instanceof Entity) {
                options = ((Entity) this.firingDisplay.target).getLocationNames();
                enabled = createEnabledMask(options.length);
            } else {
                return;
            }
            if (this.firingDisplay.target instanceof Mech) {
                if (aimingMode == IAimingModes.AIM_MODE_IMMOBILE) {
                    aimingAt = Mech.LOC_HEAD;
                } else if (aimingMode == IAimingModes.AIM_MODE_TARG_COMP) {
                    aimingAt = Mech.LOC_CT;
                }
            } else if (this.firingDisplay.target instanceof Tank) {
                int side = Compute.targetSideTable(this.firingDisplay.ce(), this.firingDisplay.target);
                if (this.firingDisplay.target instanceof LargeSupportTank) {
                    if (side == ToHitData.SIDE_FRONTLEFT) {
                        aimingAt = LargeSupportTank.LOC_FRONTLEFT;
                    } else if (side == ToHitData.SIDE_FRONTRIGHT) {
                        aimingAt = LargeSupportTank.LOC_FRONTRIGHT;
                    } else if (side == ToHitData.SIDE_REARRIGHT) {
                        aimingAt = LargeSupportTank.LOC_REARRIGHT;
                    } else if (side == ToHitData.SIDE_REARLEFT) {
                        aimingAt = LargeSupportTank.LOC_REARLEFT;
                    }
                }
                if (side == ToHitData.SIDE_LEFT) {
                    aimingAt = Tank.LOC_LEFT;
                }
                if (side == ToHitData.SIDE_RIGHT) {
                    aimingAt = Tank.LOC_RIGHT;
                }
                if (side == ToHitData.SIDE_REAR) {
                    aimingAt = (this.firingDisplay.target instanceof LargeSupportTank) ? LargeSupportTank.LOC_REAR
                            : this.firingDisplay.target instanceof SuperHeavyTank ? SuperHeavyTank.LOC_REAR
                                    : Tank.LOC_REAR;
                }
                if (side == ToHitData.SIDE_FRONT) {
                    aimingAt = Tank.LOC_FRONT;
                }
            } else if (this.firingDisplay.target instanceof Protomech) {
                aimingAt = Protomech.LOC_TORSO;
            } else if (this.firingDisplay.target instanceof BattleArmor) {
                aimingAt = BattleArmor.LOC_TROOPER_1;
            } else {
                // no aiming allowed for MechWarrior or BattleArmor
                return;
            }

            asd = new AimedShotDialog(
                    this.firingDisplay.clientgui.frame,
                    Messages.getString("FiringDisplay.AimedShotDialog.title"), //$NON-NLS-1$
                    Messages.getString("FiringDisplay.AimedShotDialog.message"), //$NON-NLS-1$
                    options, enabled, aimingAt, this, this);

            asd.setVisible(true);
            this.firingDisplay.updateTarget();
        }
    }

    private boolean[] createEnabledMask(int length) {
        boolean[] mask = new boolean[length];

        for (int i = 0; i < length; i++) {
            mask[i] = true;
        }

        int side = Compute.targetSideTable(this.firingDisplay.ce(), this.firingDisplay.target);

        // on a tank, remove turret if its missing
        // also, remove body
        if (this.firingDisplay.target instanceof Tank) {
            mask[Tank.LOC_BODY] = false;
            Tank tank = (Tank) this.firingDisplay.target;
            if (tank.hasNoTurret()) {
                int turretLoc = tank.getLocTurret();
                mask[turretLoc] = false;
            }
            // remove non-visible sides
            if (this.firingDisplay.target instanceof LargeSupportTank) {
                if (side == ToHitData.SIDE_FRONT) {
                    mask[LargeSupportTank.LOC_FRONTLEFT] = false;
                    mask[LargeSupportTank.LOC_REARLEFT] = false;
                    mask[LargeSupportTank.LOC_REARRIGHT] = false;
                    mask[LargeSupportTank.LOC_REAR] = false;
                }
                if (side == ToHitData.SIDE_FRONTLEFT) {
                    mask[LargeSupportTank.LOC_FRONTRIGHT] = false;
                    mask[LargeSupportTank.LOC_REARLEFT] = false;
                    mask[LargeSupportTank.LOC_REARRIGHT] = false;
                    mask[LargeSupportTank.LOC_REAR] = false;
                }
                if (side == ToHitData.SIDE_FRONTRIGHT) {
                    mask[LargeSupportTank.LOC_FRONTLEFT] = false;
                    mask[LargeSupportTank.LOC_REARLEFT] = false;
                    mask[LargeSupportTank.LOC_REARRIGHT] = false;
                    mask[LargeSupportTank.LOC_REAR] = false;
                }
                if (side == ToHitData.SIDE_REARRIGHT) {
                    mask[Tank.LOC_FRONT] = false;
                    mask[LargeSupportTank.LOC_FRONTLEFT] = false;
                    mask[LargeSupportTank.LOC_FRONTRIGHT] = false;
                    mask[LargeSupportTank.LOC_REARLEFT] = false;
                }
                if (side == ToHitData.SIDE_REARLEFT) {
                    mask[Tank.LOC_FRONT] = false;
                    mask[LargeSupportTank.LOC_FRONTLEFT] = false;
                    mask[LargeSupportTank.LOC_FRONTRIGHT] = false;
                    mask[LargeSupportTank.LOC_REARRIGHT] = false;
                }
                if (side == ToHitData.SIDE_REAR) {
                    mask[Tank.LOC_FRONT] = false;
                    mask[LargeSupportTank.LOC_FRONTLEFT] = false;
                    mask[LargeSupportTank.LOC_FRONTRIGHT] = false;
                    mask[LargeSupportTank.LOC_REARRIGHT] = false;
                }
            } else if (this.firingDisplay.target instanceof SuperHeavyTank) {
                if (side == ToHitData.SIDE_FRONT) {
                    mask[SuperHeavyTank.LOC_FRONTLEFT] = false;
                    mask[SuperHeavyTank.LOC_REARLEFT] = false;
                    mask[SuperHeavyTank.LOC_REARRIGHT] = false;
                    mask[SuperHeavyTank.LOC_REAR] = false;
                }
                if (side == ToHitData.SIDE_FRONTLEFT) {
                    mask[SuperHeavyTank.LOC_FRONTRIGHT] = false;
                    mask[SuperHeavyTank.LOC_REARLEFT] = false;
                    mask[SuperHeavyTank.LOC_REARRIGHT] = false;
                    mask[SuperHeavyTank.LOC_REAR] = false;
                }
                if (side == ToHitData.SIDE_FRONTRIGHT) {
                    mask[SuperHeavyTank.LOC_FRONTLEFT] = false;
                    mask[SuperHeavyTank.LOC_REARLEFT] = false;
                    mask[SuperHeavyTank.LOC_REARRIGHT] = false;
                    mask[SuperHeavyTank.LOC_REAR] = false;
                }
                if (side == ToHitData.SIDE_REARRIGHT) {
                    mask[Tank.LOC_FRONT] = false;
                    mask[SuperHeavyTank.LOC_FRONTLEFT] = false;
                    mask[SuperHeavyTank.LOC_FRONTRIGHT] = false;
                    mask[SuperHeavyTank.LOC_REARLEFT] = false;
                }
                if (side == ToHitData.SIDE_REARLEFT) {
                    mask[Tank.LOC_FRONT] = false;
                    mask[SuperHeavyTank.LOC_FRONTLEFT] = false;
                    mask[SuperHeavyTank.LOC_FRONTRIGHT] = false;
                    mask[SuperHeavyTank.LOC_REARRIGHT] = false;
                }
                if (side == ToHitData.SIDE_REAR) {
                    mask[Tank.LOC_FRONT] = false;
                    mask[SuperHeavyTank.LOC_FRONTLEFT] = false;
                    mask[SuperHeavyTank.LOC_FRONTRIGHT] = false;
                    mask[SuperHeavyTank.LOC_REARRIGHT] = false;
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
        if (this.firingDisplay.target instanceof Protomech) {
            if (!((Protomech) this.firingDisplay.target).hasMainGun()) {
                mask[Protomech.LOC_MAINGUN] = false;
            }
        }

        // remove squad location on BAs
        // also remove dead troopers
        if (this.firingDisplay.target instanceof BattleArmor) {
            mask[BattleArmor.LOC_SQUAD] = false;
        }

        // remove locations hidden by partial cover
        if ((partialCover & LosEffects.COVER_HORIZONTAL) != 0) {
            mask[Mech.LOC_LLEG] = false;
            mask[Mech.LOC_RLEG] = false;
        }
        if (side == ToHitData.SIDE_FRONT) {
            if ((partialCover & LosEffects.COVER_LOWLEFT) != 0) {
                mask[Mech.LOC_RLEG] = false;
            }
            if ((partialCover & LosEffects.COVER_LOWRIGHT) != 0) {
                mask[Mech.LOC_LLEG] = false;
            }
            if ((partialCover & LosEffects.COVER_LEFT) != 0) {
                mask[Mech.LOC_RARM] = false;
                mask[Mech.LOC_RT] = false;
            }
            if ((partialCover & LosEffects.COVER_RIGHT) != 0) {
                mask[Mech.LOC_LARM] = false;
                mask[Mech.LOC_LT] = false;
            }
        } else {
            if ((partialCover & LosEffects.COVER_LOWLEFT) != 0) {
                mask[Mech.LOC_LLEG] = false;
            }
            if ((partialCover & LosEffects.COVER_LOWRIGHT) != 0) {
                mask[Mech.LOC_RLEG] = false;
            }
            if ((partialCover & LosEffects.COVER_LEFT) != 0) {
                mask[Mech.LOC_LARM] = false;
                mask[Mech.LOC_LT] = false;
            }
            if ((partialCover & LosEffects.COVER_RIGHT) != 0) {
                mask[Mech.LOC_RARM] = false;
                mask[Mech.LOC_RT] = false;
            }
        }

        if (aimingMode == IAimingModes.AIM_MODE_TARG_COMP) {
            // Can't target head with targeting computer.
            mask[Mech.LOC_HEAD] = false;
        }
        return mask;
    }

    public void closeDialog() {
        if (asd != null) {
            aimingAt = Entity.LOC_NONE;
            aimingMode = IAimingModes.AIM_MODE_NONE;
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

    public int getAimingMode() {
        return aimingMode;
    }

    /**
     * Returns the name of aimed location.
     */
    public String getAimingLocation() {
        if ((this.firingDisplay.target != null) && (aimingAt != Entity.LOC_NONE)
            && (aimingMode != IAimingModes.AIM_MODE_NONE)) {
            if (this.firingDisplay.target instanceof GunEmplacement) {
                return GunEmplacement.HIT_LOCATION_NAMES[aimingAt];
            } else if (this.firingDisplay.target instanceof Entity) {
                return ((Entity) this.firingDisplay.target).getLocationAbbrs()[aimingAt];
            }
        }
        return null;
    }

    /**
     * Sets the aiming mode, depending on the target and the attacker.
     * Against immobile mechs, targeting computer aiming mode will be used
     * if turned on. (This is a hack, but it's the resolution suggested by
     * the bug submitter, and I don't think it's half bad.
     */

    public void setAimingMode() {
        boolean allowAim;

        // TC against a mech
        allowAim = ((this.firingDisplay.target != null) && (this.firingDisplay.ce() != null)
                && this.firingDisplay.ce().hasAimModeTargComp() && ((this.firingDisplay.target instanceof Mech)
                || (this.firingDisplay.target instanceof Tank)
                || (this.firingDisplay.target instanceof BattleArmor) || (this.firingDisplay.target instanceof Protomech)));
        if (allowAim) {
            aimingMode = IAimingModes.AIM_MODE_TARG_COMP;
            return;
        }
        // immobile mech or gun emplacement
        allowAim = ((this.firingDisplay.target != null) && ((this.firingDisplay.target.isImmobile() && ((this.firingDisplay.target instanceof Mech) || (this.firingDisplay.target instanceof Tank))) || (this.firingDisplay.target instanceof GunEmplacement)));
        if (allowAim) {
            aimingMode = IAimingModes.AIM_MODE_IMMOBILE;
            return;
        }

        aimingMode = IAimingModes.AIM_MODE_NONE;
    }

    /**
     * @return if are we in aiming mode
     */
    public boolean inAimingMode() {
        return aimingMode != IAimingModes.AIM_MODE_NONE;
    }

    /**
     * @return if a hit location currently selected.
     */
    public boolean isAimingAtLocation() {
        return aimingAt != Entity.LOC_NONE;
    }

    /**
     * should aimned shoots be allowed with the passed weapon
     *
     * @param weapon
     * @return
     */
    public boolean allowAimedShotWith(Mounted weapon) {
        return Compute.allowAimedShotWith(weapon, aimingMode);
    }

    /**
     * ActionListener, listens to the button in the dialog.
     */
    public void actionPerformed(ActionEvent ev) {
        closeDialog();
    }

    /**
     * ItemListener, listens to the radiobuttons in the dialog.
     */
    public void itemStateChanged(ItemEvent ev) {
        IndexedRadioButton icb = (IndexedRadioButton) ev.getSource();
        aimingAt = icb.getIndex();
        this.firingDisplay.updateTarget();
    }
}