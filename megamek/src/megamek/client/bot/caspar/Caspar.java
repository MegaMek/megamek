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
package megamek.client.bot.caspar;

import java.util.List;

import megamek.client.bot.AIType;
import megamek.client.bot.princess.FormationGeometry;
import megamek.client.bot.princess.MutualSupportDeployment;
import megamek.client.bot.princess.MutualSupportPathRanker;
import megamek.client.bot.princess.PathRanker.PathRankerType;
import megamek.client.bot.princess.Princess;
import megamek.common.board.Coords;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * The CASPAR bot: the experimental successor to {@link Princess}. It shares Princess's entire
 * network/phase/state plumbing; experimental behavior is added by overriding Princess's wiring seams (for
 * example {@code initializePathRankers()} and {@code initializeFireControls()}) so that each divergence can
 * be compared against Princess head-to-head.
 *
 * <p>Current divergences from Princess:</p>
 * <ul>
 *     <li><b>Mutual Support movement doctrine</b> ({@link MutualSupportPathRanker}): supporting-range
 *     cohesion instead of center-of-mass herding, a cover bonus for advancing inside a set friend's
 *     engagement envelope, and a uniform closing tempo so slow and fast elements commit together.</li>
 *     <li><b>Mutual Support deployment</b> ({@link MutualSupportDeployment}): a force comes onto the board
 *     as a formation instead of scattering across its whole deployment zone.</li>
 * </ul>
 */
public class Caspar extends Princess {
    private static final MMLogger LOGGER = MMLogger.create(Caspar.class);

    /**
     * Creates a new CASPAR bot with the given display name, configured for the given host and port.
     *
     * @param name The display name
     * @param host The host address to which to connect
     * @param port The port on the host where to connect
     */
    public Caspar(final String name, final String host, final int port) {
        super(name, host, port);
    }

    @Override
    public AIType getAIType() {
        return AIType.CASPAR;
    }

    @Override
    public void initializePathRankers() {
        super.initializePathRankers();
        // CASPAR divergence: the Mutual Support doctrine replaces the stock ground-unit path ranker.
        registerPathRanker(PathRankerType.Basic, new MutualSupportPathRanker(this));
        // Info-level receipt so any headless run's logs prove which ranker this bot is actually using.
        LOGGER.info("[MutualSupport] {}: CASPAR registered MutualSupportPathRanker for ground units",
              getLocalPlayer() != null ? getLocalPlayer().getName() : getName());
    }

    @Override
    protected List<Coords> prioritizeDeploymentCoords(Entity deployedUnit, List<Coords> possibleDeployCoords) {
        // CASPAR divergence: deploy as a formation rather than scattering across the whole zone.
        int formationRadius = FormationGeometry.formationRadius(getEntitiesOwned(), mutualSupportMultiplier());
        LOGGER.info("[MutualSupport] {}: forming up {} within {} hexes of the force centre",
              getLocalPlayer() != null ? getLocalPlayer().getName() : getName(),
              deployedUnit.getChassis(),
              formationRadius);
        return MutualSupportDeployment.prioritize(deployedUnit,
              possibleDeployCoords,
              getFriendEntities(),
              getGame(),
              formationRadius);
    }

    /**
     * The player's mutual support setting: how tightly this bot keeps its force together, as a multiplier around 1.0.
     *
     * <p>The doctrine's single read of the underlying behavior setting, so no other Mutual Support code has to know
     * what it is currently called.</p>
     *
     * @return the multiplier, where higher means a tighter formation
     */
    private double mutualSupportMultiplier() {
        return getBehaviorSettings().getMutualSupportValue();
    }
}
