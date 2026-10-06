/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.entityreadout;

import java.util.ArrayList;
import java.util.List;

import megamek.client.ui.Messages;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;

/**
 * Summary readout for a Battlefield Support Asset's card-based stat block.
 */
class BattlefieldSupportAssetReadout extends GeneralEntityReadout {

    private final BattlefieldSupportAsset asset;

    protected BattlefieldSupportAssetReadout(BattlefieldSupportAsset asset, boolean showDetail,
          boolean useAlternateCost, boolean ignorePilotBV) {
        super(asset, showDetail, useAlternateCost, ignorePilotBV);
        this.asset = asset;
    }

    @Override
    protected List<ViewElement> createBaseSection() {
        List<ViewElement> result = new ArrayList<>();
        result.add(new PlainLine());
        result.add(new LabeledLine(Messages.getString("MekView.BFSAssetType"),
              asset.getAssetType().displayName()));
        result.add(new LabeledLine(Messages.getString("MekView.BFSCrewGrade"),
              asset.getCrewSkillLevel().toString()));
        result.add(new LabeledLine(Messages.getString("MekView.BFSBV"), bvDisplay()));
        result.add(createRoleElement());
        return result;
    }

    @Override
    protected List<ViewElement> createSourceSection() {
        List<ViewElement> result = new ArrayList<>();
        result.add(new PlainLine());
        result.add(new LabeledLine(Messages.getString("MekView.BSPCost"), asset.getCostDisplay()));
        result.add(createSourceElement());
        return result;
    }

    @Override
    protected List<ViewElement> createSystemsSection() {
        List<ViewElement> result = new ArrayList<>();
        result.add(new PlainLine());
        result.add(new LabeledLine(Messages.getString("MekView.Movement"), asset.getMovementDisplay()));
        result.add(new LabeledLine(Messages.getString("MekView.TMM"), asset.getTmmDisplay()));
        result.add(new LabeledLine(Messages.getString("MekView.Range"), asset.getRangeDisplay()));
        result.add(new LabeledLine(Messages.getString("MekView.Skill"), asset.getSkillDisplay()));
        result.add(new LabeledLine(Messages.getString("MekView.Damage"), asset.getDamageDisplay()));
        result.add(new LabeledLine(Messages.getString("MekView.DestroyCheck"), destroyCheckDisplay()));
        result.add(new LabeledLine(Messages.getString("MekView.Threshold"),
              Integer.toString(asset.getThreshold())));
        return result;
    }

    @Override
    protected List<ViewElement> createLoadoutBlock() {
        return List.of(
              new PlainLine(),
              new LabeledLine(Messages.getString("MekView.Specials"), asset.getSpecialsDisplay()));
    }

    private ViewElement destroyCheckDisplay() {
        if (asset.getDestroyCheck() == asset.getODestroyCheck()) {
            return new PlainElement(asset.getDestroyCheck());
        }
        return new JoinedViewElement()
              .add(asset.getODestroyCheck())
              .add(" -> ")
              .add(new DamagedElement(Integer.toString(asset.getDestroyCheck())));
    }

    private String bvDisplay() {
        Integer veteranBv = asset.getVeteranBv();
        return (veteranBv == null) ? Integer.toString(asset.getBv()) : asset.getBv() + "(" + veteranBv + ")";
    }
}
