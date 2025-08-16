/*
 * Copyright (C) 2017-2025 The MegaMek Team. All Rights Reserved.
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
package megamek.client.ui.dialogs.customMek;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.JPanel;

import megamek.common.AmmoType.AmmoTypeEnum;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.LocationFullException;
import megamek.common.Mounted;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.WeaponMounted;
import megamek.logging.MMLogger;

/**
 * @author Neoancient
 */
public class BayMunitionsChoicePanel extends JPanel {
    private final static MMLogger logger = MMLogger.create(BayMunitionsChoicePanel.class);

    @Serial
    private static final long serialVersionUID = -7741380967676720496L;

    private final Game game;
    private final Entity entity;
    private final List<AmmoRowPanel> rows = new ArrayList<>();

    private record AmmoKey(AmmoTypeEnum ammoType, int rackSize) {}

    public BayMunitionsChoicePanel(Entity entity, Game game) {
        this.entity = entity;
        this.game = game;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(10, 0, 10, 0);

        for (WeaponMounted bay : entity.getWeaponBayList()) {
            Map<AmmoKey, List<AmmoMounted>> ammoByType = new HashMap<>();
            for (AmmoMounted ammo : bay.getBayAmmo()) {
                AmmoKey key = new AmmoKey(ammo.getType().getAmmoType(), ammo.getType().getRackSize());
                ammoByType.computeIfAbsent(key, k -> new ArrayList<>()).add(ammo);
            }
            for (Entry<AmmoKey, List<AmmoMounted>> entry : ammoByType.entrySet()) {
                AmmoKey key = entry.getKey();
                List<AmmoMounted> ammoMounts = entry.getValue();
                AmmoRowPanel row = new AmmoRowPanel(this, bay, key.ammoType(), key.rackSize(), ammoMounts);
                gbc.gridy++;
                add(row, gbc);
                rows.add(row);
            }
        }
    }

    /**
     * Change the munition types of the bay ammo mounts to the selected values. If there are more munition types than
     * there were originally, additional ammo bin mounts will be added. If fewer, the unneeded ones will have their shot
     * count zeroed.
     */
    public void apply() {
        for (AmmoRowPanel row : rows) {
            int mountIndex = 0;
            double remainingWeight = row.tonnage;
            for (int i = 0; i < row.munitions.size(); i++) {
                int shots = (Integer) row.spinners.get(i).getValue();
                if (shots > 0) {
                    AmmoMounted mounted;
                    if (mountIndex >= row.ammoMounts.size()) {
                        mounted = (AmmoMounted) Mounted.createMounted(entity, row.munitions.get(i));
                        try {
                            entity.addEquipment(mounted, row.bay.getLocation(), row.bay.isRearMounted());
                            row.bay.addAmmoToBay(entity.getEquipmentNum(mounted));
                        } catch (LocationFullException e) {
                            logger.error(e, "apply");
                        }

                    } else {
                        mounted = row.ammoMounts.get(mountIndex);
                        mounted.changeAmmoType(row.munitions.get(i));
                    }
                    mounted.setShotsLeft(shots);
                    int slots = (int) Math.ceil((double) shots / row.munitions.get(i).getShots());
                    mounted.setOriginalShots(slots * row.munitions.get(i).getShots());
                    mounted.setSize(slots * row.munitions.get(i).getTonnage(entity));
                    remainingWeight -= mounted.getSize();
                    mountIndex++;
                }
            }
            // Zero out any remaining unused bins.
            while (mountIndex < row.ammoMounts.size()) {
                AmmoMounted mount = row.ammoMounts.get(mountIndex);
                mount.setSize(0);
                mount.setOriginalShots(0);
                mount.setShotsLeft(0);
                mountIndex++;
            }
            // If the unit is assigned less ammo than the capacity, assign remaining weight
            // to first mount
            // and adjust original shots.
            if (remainingWeight > 0) {
                AmmoMounted m = row.ammoMounts.get(0);
                m.setSize(m.getSize() + remainingWeight);
                m.setOriginalShots((int) Math.floor(m.getSize() / (m.getType().getShots() * m.getTonnage())));
            }
        }
    }

    public Entity getEntity() {
        return entity;
    }

    public Game getGame() {
        return game;
    }
}
