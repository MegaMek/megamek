/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.client.ui.tileset;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import megamek.common.units.Entity;

/** Shared offline/runtime identity: a refit retaining a stock name must not show the stock weapons. */
public final class UnitModelKey {
    private UnitModelKey() { }

    public static String forEntity(Entity entity) {
        StringBuilder equipment = new StringBuilder();
        for (var mount : entity.getEquipment()) {
            String name = mount.getType().getInternalName();
            equipment.append(name.length()).append(':').append(name).append(':')
                  .append(mount.getLocation()).append(':').append(mount.getSecondLocation()).append(':')
                  .append(mount.isRearMounted()).append(':').append(mount.getSize()).append(';');
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(equipment.toString().getBytes(StandardCharsets.UTF_8));
            return entity.getShortNameRaw() + "#" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("The JVM must provide SHA-256", impossible);
        }
    }
}
