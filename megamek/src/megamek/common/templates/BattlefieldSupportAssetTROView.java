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
package megamek.common.templates;

import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.verifier.EntityVerifier;

/**
 * TRO template model for a Battlefield Support Asset's card-based stat block.
 */
public class BattlefieldSupportAssetTROView extends TROView {

    private final BattlefieldSupportAsset asset;

    public BattlefieldSupportAssetTROView(BattlefieldSupportAsset asset) {
        this.asset = asset;
    }

    @Override
    protected String getTemplateFileName(boolean html) {
        return html ? "bfs_asset.ftlh" : "bfs_asset.ftl";
    }

    @Override
    protected void initModel(EntityVerifier verifier) {
        addBasicData(asset);
        addEntityFluff(asset);
        setModelData("assetType", asset.getAssetType().displayName());
        setModelData("movement", asset.getMovementDisplay());
        setModelData("tmm", asset.getTmmDisplay());
        setModelData("range", asset.getRangeDisplay());
        setModelData("skill", asset.getSkillDisplay());
        setModelData("damage", asset.getDamageDisplay());
        setModelData("destroyCheck", asset.getDestroyCheck());
        setModelData("originalDestroyCheck", asset.getODestroyCheck());
        setModelData("damaged", asset.getDestroyCheck() != asset.getODestroyCheck());
        setModelData("threshold", asset.getThreshold());
        setModelData("specials", asset.getSpecialsDisplay());
        setModelData("bsp", asset.getCostDisplay());
        setModelData("bfsBv", bvDisplay());
        setModelData("crewGrade", asset.getCrewSkillLevel().toString());
        setModelData("source", asset.getSource());
    }

    private String bvDisplay() {
        Integer veteranBv = asset.getVeteranBv();
        return (veteranBv == null) ? Integer.toString(asset.getBv()) : asset.getBv() + "(" + veteranBv + ")";
    }
}
