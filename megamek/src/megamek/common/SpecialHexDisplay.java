/*
 * Copyright (C) 2007 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2008-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common;

import java.awt.Image;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.annotations.Nullable;
import megamek.common.enums.GamePhase;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;

/**
 * @author dirk
 */
public class SpecialHexDisplay implements Serializable {
    @Serial
    private static final long serialVersionUID = 27470795993329492L;
    public static final int LARGE_EXPLOSION_IMAGE_RADIUS = 4;

    public enum Type {
        ARTILLERY_AUTO_HIT(new MegaMekFile(Configuration.hexesDir(), "artyauto.gif")) {
            @Override
            public boolean drawBefore() {
                return false;
            }

            @Override
            public boolean drawAfter() {
                return true;
            }
        },
        ARTILLERY_ADJUSTED(new MegaMekFile(Configuration.hexesDir(), "artyadj.gif")) {
            @Override
            public boolean drawBefore() {
                return false;
            }

            @Override
            public boolean drawAfter() {
                return true;
            }
        },
        ARTILLERY_INCOMING(new MegaMekFile(Configuration.hexesDir(), "artyinc.gif")),
        ARTILLERY_TARGET(new MegaMekFile(Configuration.hexesDir(), "artytarget.gif")) {
            @Override
            public boolean drawBefore() {
                return false;
            }
        },
        ARTILLERY_HIT(new MegaMekFile(Configuration.hexesDir(), "artyhit.gif")) {
            @Override
            public boolean drawBefore() {
                return false;
            }
        },
        ARTILLERY_DRIFT(new MegaMekFile(Configuration.hexesDir(), "artydrift.gif")) {
            @Override
            public boolean drawBefore() {
                return false;
            }
        },
        ARTILLERY_MISS(new MegaMekFile(Configuration.hexesDir(), "artymiss.gif")) {
            @Override
            public boolean drawBefore() {
                return false;
            }
        },
        BOMB_HIT(new MegaMekFile(Configuration.hexesDir(), "bombhit.gif")) {
            @Override
            public boolean drawBefore() {
                return false;
            }
        },
        BOMB_DRIFT(new MegaMekFile(Configuration.hexesDir(), "bombdrift.gif")) {
            @Override
            public boolean drawBefore() {
                return false;
            }
        },
        BOMB_MISS(new MegaMekFile(Configuration.hexesDir(), "bombmiss.gif")) {
            @Override
            public boolean drawBefore() {
                return false;
            }
        },
        PLAYER_NOTE(new MegaMekFile(Configuration.hexesDir(), "note.png")) {
            @Override
            public boolean drawAfter() {
                return true;
            }
        },
        NUKE_INCOMING(new MegaMekFile(Configuration.hexesDir(), "nukeinc.gif")),
        NUKE_HIT(new MegaMekFile(Configuration.nukeHexesDir(), "hit")) {
            @Override
            public boolean drawBefore() {
                return false;
            }

            @Override
            public boolean useFolderStructure() {
                return true;
            }
        },
        ORBITAL_BOMBARDMENT_INCOMING(new MegaMekFile(Configuration.hexesDir(), "artyinc.gif")),
        ORBITAL_BOMBARDMENT(new MegaMekFile(Configuration.orbitalBombardmentHexesDir(), "hit")) {
            @Override
            public boolean drawBefore() {
                return false;
            }

            @Override
            public boolean useFolderStructure() {
                return true;
            }
        };

        private transient Image defaultImage;
        private final MegaMekFile defaultImagePath;

        Type(MegaMekFile iconPath) {
            defaultImagePath = iconPath;
        }

        public void init() {
            if (defaultImagePath == null) {
                return;
            }
            defaultImage = ImageUtil.loadImageFromFile(defaultImagePath);
        }

        public boolean useFolderStructure() {
            return false;
        }

        public Image getDefaultImage() {
            return defaultImage;
        }

        /**
         * Get the image for this type of special hex display.
         *
         * @param imageName The name of the image to get
         *
         * @return The image
         */
        public Image getImage(String imageName) {
            if (useFolderStructure()) {
                return ImageUtil.loadImageFromFile(new MegaMekFile(defaultImagePath.getFile(), imageName));
            }
            return defaultImage;
        }

        public void setDefaultImage(Image defaultImage) {
            this.defaultImage = defaultImage;
        }

        public boolean drawBefore() {
            return true;
        }

        public boolean drawAfter() {
            return false;
        }
    }

    /**
     * Only the owner may see this display
     */
    public static int SHD_VISIBLE_TO_OWNER = 0;

    /**
     * The owner and members of his team can see this display
     */
    public static int SHD_VISIBLE_TO_TEAM = 1;

    /**
     * Everyone can see this display
     */
    public static int SHD_VISIBLE_TO_ALL = 2;

    private String info;
    private Type type;
    private int round;
    private Player owner;
    private String imageSignature;

    private int obscured = SHD_VISIBLE_TO_ALL;

    public static int NO_ROUND = -99;

    public SpecialHexDisplay(Type type, int round, Player owner, String info) {
        this.type = type;
        this.info = info;
        this.round = round;
        this.owner = owner;
    }

    public SpecialHexDisplay(Type type, int round, Player owner, String info, int obscured) {
        this.type = type;
        this.info = info;
        this.round = round;
        this.owner = owner;
        this.obscured = obscured;
    }

    public SpecialHexDisplay(Type type, int round, Player owner, String info, int obscured, String imageSignature) {
        this.type = type;
        this.info = info;
        this.round = round;
        this.owner = owner;
        this.obscured = obscured;
        this.imageSignature = imageSignature;
    }

    /**
     * Creates an Artillery Auto hit marker for the given player. It has no round limitation and is visible to team
     * members of the owner.
     *
     * @param owner The owner of this auto hit hex
     *
     * @return A SpecialHexDisplay auto hit marker
     */
    public static SpecialHexDisplay createArtyAutoHit(Player owner) {
        return new SpecialHexDisplay(Type.ARTILLERY_AUTO_HIT, NO_ROUND, owner,
              "Artillery auto hit for player " + owner.getName(), SHD_VISIBLE_TO_TEAM);
    }

    /**
     * Creates an Incoming Artillery marker for the given owner and the given round in which it will land. It has no
     * round limitation and is visible to team members of the owner.
     *
     * @param owner The owner of this artillery attack
     *
     * @return A SpecialHexDisplay Incoming marker
     */
    public static SpecialHexDisplay createIncomingArty(Player owner, int landingGameRound) {
        String artyMsg = "Artillery bay fire incoming, landing on round %d, fired by %s"
              .formatted(landingGameRound, owner.getName());
        return createIncomingFire(owner, landingGameRound, artyMsg);
    }

    /**
     * Creates an Incoming Artillery marker for the given owner and the given round in which it will land. It has no
     * round limitation and is visible to team members of the owner.
     *
     * @param owner The owner of this artillery attack
     *
     * @return A SpecialHexDisplay Incoming marker
     */
    public static SpecialHexDisplay createIncomingFire(Player owner, int landingGameRound, String message) {
        return new SpecialHexDisplay(Type.ARTILLERY_INCOMING, landingGameRound, owner, message, SHD_VISIBLE_TO_TEAM);
    }

    /**
     * Creates an Artillery miss marker for the given owner and the given round in which it landed. It has no round
     * limitation and is visible to everyone.
     *
     * @param owner   The owner of this artillery attack
     * @param round   The game round in which the attack landed and scattered
     * @param message The message to display for this board marker
     *
     * @return A SpecialHexDisplay Artillery Miss marker
     */
    public static SpecialHexDisplay createArtyMiss(Player owner, int round, String message) {
        return new SpecialHexDisplay(Type.ARTILLERY_MISS, round, owner, message, SHD_VISIBLE_TO_ALL);
    }

    public boolean thisRound(int testRound) {
        if (NO_ROUND == round) {
            return true;
        }
        return testRound == round;
    }

    /** Does this SpecialHexDisplayObjet concern a round in the future? */
    public boolean futureRound(int testRound) {
        if (NO_ROUND == round) {
            return true;
        }
        return testRound > round;
    }

    /** Does this SpecialHexDisplayObjet concern a round in the past? */
    public boolean pastRound(int testRound) {
        if (NO_ROUND == round) {
            return true;
        }
        return testRound < round;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public Type getType() {
        return type;
    }

    public Image getDefaultImage() {
        if (type.useFolderStructure()) {
            return type.getImage(imageSignature);
        }
        return type.getDefaultImage();
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Player getOwner() {
        return owner;
    }

    public void setOwner(Player owner) {
        this.owner = owner;
    }

    public void setObscuredLevel(int o) {
        if (o >= SHD_VISIBLE_TO_OWNER && o <= SHD_VISIBLE_TO_ALL) {
            obscured = o;
        }
    }

    public int getObscuredLevel() {
        return obscured;
    }

    /**
     * Determines whether this special hex should be obscured from the given
     * <code>Player</code>.
     *
     * @param other The player to check for
     *
     * @return True if the special hex should be obscured
     */
    public boolean isObscured(@Nullable Player other) {
        if (owner == null) {
            return false;
        }
        if ((obscured == SHD_VISIBLE_TO_OWNER) && owner.equals(other)) {
            return false;
        } else if ((obscured == SHD_VISIBLE_TO_TEAM) && (other != null)
              && (owner.getTeam() == other.getTeam())) {
            return false;
        }

        return obscured != SHD_VISIBLE_TO_ALL;
    }

    public void setObscured(int obscured) {
        this.obscured = obscured;
    }

    /**
     * Determine whether the current SpecialHexDisplay should be displayed Note Artillery Hits and Bomb Hits (direct
     * hits on their targets) will always display in the appropriate phase. Other bomb- or artillery-related graphics
     * are optional.
     *
     * @param phase          The current phase of the game
     * @param curRound       The current round
     * @param playerChecking The player checking the display
     * @param guiPref        The GUI preferences
     *
     * @return True if the image should be displayed
     */
    public boolean drawNow(GamePhase phase, int curRound, Player playerChecking, GUIPreferences guiPref) {
        boolean shouldDisplay = thisRound(curRound)
              || (pastRound(curRound) && type.drawBefore())
              || (futureRound(curRound) && type.drawAfter());

        if (phase.isBefore(GamePhase.OFFBOARD)
              && ((type == Type.ARTILLERY_TARGET)
              || type == Type.ARTILLERY_MISS
              || (type == Type.ARTILLERY_HIT))) {
            shouldDisplay = shouldDisplay || thisRound(curRound - 1);
        }

        // Arty icons for the owner are drawn in BoardView1.drawArtillery
        // and shouldn't be drawn twice
        if (isOwner(playerChecking) &&
              (type == Type.ARTILLERY_ADJUSTED ||
                    type == Type.ARTILLERY_INCOMING ||
                    type == Type.ARTILLERY_TARGET)) {
            return false;
        }

        // Only display obscured hexes to owner
        if (isObscured(playerChecking)) {
            return false;
        }

        // Hide icons the player doesn't want to see
        // Check user settings and Hide some "hits" because they are actually drifts that did damage
        if (guiPref != null) {
            switch (type) {
                case ARTILLERY_HIT -> shouldDisplay &= !info.contains(Messages.getString("ArtilleryMessage.drifted"));
                case ARTILLERY_MISS -> shouldDisplay &= guiPref.getBoolean(GUIPreferences.SHOW_ARTILLERY_MISSES);
                case ARTILLERY_DRIFT -> shouldDisplay &= guiPref.getBoolean(GUIPreferences.SHOW_ARTILLERY_DRIFTS);
                case BOMB_MISS -> shouldDisplay &= guiPref.getBoolean(GUIPreferences.SHOW_BOMB_MISSES);
                case BOMB_DRIFT -> shouldDisplay &= guiPref.getBoolean(GUIPreferences.SHOW_BOMB_DRIFTS);
                default -> { } // intentionally ignored
            }
        }

        return shouldDisplay;
    }

    /**
     * @param toPlayer The player to check
     *
     * @return True if the player is the owner of this Special Hex Display
     */
    public boolean isOwner(Player toPlayer) {
        return (owner == null) || owner.equals(toPlayer);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if ((null == obj) || (getClass() != obj.getClass())) {
            return false;
        }
        final SpecialHexDisplay other = (SpecialHexDisplay) obj;
        return (type == other.type) && Objects.equals(owner, other.owner) && (round == other.round)
              && info.equals(other.info);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, owner, round);
    }

    @Override
    public String toString() {
        return "SHD: " + type.name() + ", " + "round " + round + (owner != null ? ", by "
              + owner.getName() : "");
    }
}
