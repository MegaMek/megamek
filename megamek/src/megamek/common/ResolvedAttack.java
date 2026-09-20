/* Copyright (C) 2026 The MegaMek Team. SPDX-License-Identifier: GPL-3.0-or-later */
package megamek.common;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponMounted;
import megamek.common.units.Entity;
import megamek.common.units.IAero;
import megamek.common.units.UnitLocation;

/** A transient, server-confirmed visual event. It is not an attack order or another damage/rules calculation. */
public record ResolvedAttack(UUID id, Kind kind, UnitLocation attacker, UnitLocation target, int targetType,
      int equipmentIndex, String equipmentName, int limb, boolean hit, List<Mount> mounts) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public ResolvedAttack {
        mounts = mounts == null ? singleMount(attacker, equipmentIndex) : List.copyOf(mounts);
    }

    public ResolvedAttack(UUID id, Kind kind, UnitLocation attacker, UnitLocation target, int targetType,
          int equipmentIndex, String equipmentName, int limb, boolean hit) {
        this(id, kind, attacker, target, targetType, equipmentIndex, equipmentName, limb, hit, singleMount(attacker, equipmentIndex));
    }

    /** Identity of a physical gun, including the owning fighter when a squadron fires a logical group. */
    public record Mount(int entityId, int equipmentIndex) implements Serializable { }

    private static List<Mount> singleMount(UnitLocation attacker, int equipmentIndex) {
        return equipmentIndex < 0 ? List.of() : List.of(new Mount(attacker.entityId(), equipmentIndex));
    }

    /** Resolve once while handling the confirmed attack. Presentation never rebuilds group membership. */
    public static List<Mount> captureMounts(Entity attacker, int equipmentIndex) {
        return physicalMounts(equipmentIndex < 0 ? null : attacker.getEquipment(equipmentIndex))
              .map(mount -> new Mount(mount.getEntity().getId(), mount.getEquipmentNum())).distinct().toList();
    }

    private static Stream<? extends Mounted<?>> physicalMounts(Mounted<?> mount) {
        if (mount == null) {
            return Stream.empty();
        }
        if (mount instanceof WeaponMounted weapon) {
            if (weapon.isWeaponGroup() && mount.getEntity() instanceof IAero aero) {
                return aero.getWeaponGroupMembers(weapon).stream().flatMap(ResolvedAttack::physicalMounts);
            }
            if (!weapon.getBayWeapons().isEmpty()) {
                return weapon.getBayWeapons().stream().flatMap(ResolvedAttack::physicalMounts);
            }
        }
        return Stream.of(mount);
    }

    public enum Kind { SHOT, PUNCH, KICK, PUSH, CLUB }
}
