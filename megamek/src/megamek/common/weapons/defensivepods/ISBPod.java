/*
 * MegaMek - Copyright (C) 2004, 2005 Ben Mazur (bmazur@sev.org)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common.weapons.defensivepods;

/**
 * @author Jason Tighe
 * @since Oct 20, 2004
 */
public class ISBPod extends BPodWeapon {
    private static final long serialVersionUID = -7533678162058325256L;

    public ISBPod() {
        super();
        this.name = "Anti-BattleArmor Pods (B-Pods)";
        this.setInternalName("ISBPod");
        this.addLookupName("ISB-Pod");
        this.addLookupName("CLBPod");
        this.addLookupName("CLB-Pod");
    }
}
