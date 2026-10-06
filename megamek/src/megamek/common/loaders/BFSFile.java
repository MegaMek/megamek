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
package megamek.common.loaders;

import java.io.InputStream;

import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.battlefieldSupport.BattlefieldSupportAssetData;
import megamek.common.battlefieldSupport.BattlefieldSupportAssetYaml;
import megamek.common.units.Entity;

/**
 * Loads a Battlefield Support Asset from a {@code .bfs} YAML file, producing a {@link BattlefieldSupportAsset} entity.
 * The file is parsed into a {@link BattlefieldSupportAssetData} stat block, which the entity is then built from.
 */
public class BFSFile implements IMekLoader {

    private final BattlefieldSupportAsset asset;

    public BFSFile(InputStream is) throws EntityLoadingException {
        try {
            BattlefieldSupportAssetData data = BattlefieldSupportAssetYaml.fromYaml(is);
            asset = new BattlefieldSupportAsset(data);
            // Snapshot the as-loaded identity (chassis/model/UUID) so save-time UUID rules can detect a rename, exactly
            // as BLKFile/MtfFile do for base units.
            asset.storeOriginalUnitData();
        } catch (Exception ex) {
            throw new EntityLoadingException("Error parsing .bfs file: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Entity getEntity() {
        return asset;
    }

    /**
     * Determines whether the given string is a {@code .bfs} document, used to route in-memory unit strings (for example
     * the undo/redo mementos) that carry no filename. The content must parse as YAML and carry the {@code assetType}
     * field that is unique to the {@code .bfs} format; no {@code .mtf} or {@code .blk} file satisfies both. The fields
     * are not otherwise validated.
     *
     * @param content the unit representation to test
     *
     * @return {@code true} if the content is a {@code .bfs} YAML document
     */
    public static boolean isBfsContent(String content) {
        return BattlefieldSupportAssetYaml.isAssetDocument(content);
    }
}
