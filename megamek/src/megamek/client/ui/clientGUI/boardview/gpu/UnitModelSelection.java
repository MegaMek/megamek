/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.clientGUI.boardview.gpu;

import megamek.client.ui.tileset.MekTileset;
import megamek.client.ui.tileset.UnitModelKey;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;

/** Presentation only: the tileset owns identity and the unit owns its surviving personnel. */
final class UnitModelSelection {
    private UnitModelSelection() { }

    static BoardScene.UnitModel capture(Entity entity, int part, boolean sensor, MekTileset tileset) {
        if (sensor) {
            return null;
        }
        String asset = tileset.modelFor(entity, part);
        if (asset == null) {
            return null;
        }
        // Support assets may reuse an infantry tileset entry without exposing a personnel count.
        int count = 1;
        if (entity instanceof BattleArmor armor) {
            int survivors = 0;
            for (int location = 1; location < armor.locations(); location++) {
                if (armor.getInternal(location) > 0) {
                    survivors++;
                }
            }
            count = figures(survivors, 4);
        } else if (entity instanceof Infantry infantry) {
            count = figures(infantry.getActiveTroopers(), 6);
        }
        return new BoardScene.UnitModel(asset, tileset.genericModelFor(entity, part),
              entity instanceof Mek ? UnitModelKey.forEntity(entity) : entity.getShortNameRaw(), count);
    }

    /** Square-root compression: 28 soldiers become six figures; five armored troopers become three. */
    static int figures(int survivors, int limit) {
        return Math.min(limit, (int) Math.ceil(Math.sqrt(Math.max(0, survivors))));
    }
}
