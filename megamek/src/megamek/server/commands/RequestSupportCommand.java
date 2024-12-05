/*
 * MegaMek - Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.server.commands;

import megamek.client.ui.Messages;
import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.options.OptionsConstants;
import megamek.server.Server;
import megamek.server.commands.arguments.*;
import megamek.server.totalwarfare.TWGameManager;

import java.util.List;

/**
 * @author Luana Coppio
 */
public class RequestSupportCommand extends ClientServerCommand {

    private final TWGameManager gameManager;

    public RequestSupportCommand(Server server, TWGameManager gameManager) {
        super(server, gameManager, "hq", Messages.getString("Gamemaster.cmd.support.help"),
            Messages.getString("Gamemaster.cmd.support.longName"));
        this.gameManager = gameManager;
    }

    private enum SupportType {
        OAS_LightStrike(5, 2),
        OAS_LightBombing(3, 3),
        OAS_HeavyStrike(6, 3),
        OAS_HeavyBombing(7, 4),
        OAS_Strafing(7, 5),
//  implement the full BSP later
//        DAS_LAC_LightStrike(3, 1),
//        DAS_LAC_LightBombing(4, 1),
//        DAS_LAC_HeavyStrike(9, 1),
//        DAS_LAC_HeavyBombing(11, 1),
//        DAS_LAC_Strafing(11, 1),
//        DAS_HAC_LightStrike(9, 2),
//        DAS_HAC_LightBombing(9, 2),
//        DAS_HAC_HeavyStrike(5, 2),
//        DAS_HAC_HeavyBombing(6, 2),
//        DAS_HAC_Strafing(6, 2),
//        MINES_LightDensity(9, 0.5f),
//        MINES_MediumDensity(8, 2),
//        MINES_HeavyDensity(7, 4);
        ART_Thumper(8, 3),
        ART_Sniper(8, 4),
        ART_LongTom(8, 6),
        ART_ArrowIV(8, 4), // copperhead :P

        ART_Illumination(8, 1.5f),
        ART_Smoke(8, 1.5f),

        ART_Nuke(8, 22),

        ORT_bombardment(8, 15);


        private int targetNumber;
        private float bspCost;

        SupportType(int targetNumber, float bspCost) {
            this.bspCost = bspCost;
            this.targetNumber = targetNumber;
        }

        public float bspCost() {
            return this.bspCost;
        }

        public int targetNumber() {
            return this.targetNumber;
        }
    }

    @Override
    public List<Argument<?>> defineArguments() {
        return List.of(
            new CoordXArgument("x", Messages.getString("Gamemaster.cmd.x")),
            new CoordYArgument("y", Messages.getString("Gamemaster.cmd.y")),
            new EnumArgument<>("type", Messages.getString("Gamemaster.cmd.support.type"), SupportType.class, null)
        );
    }

    @Override
    protected void runCommand(int connId, Arguments args) {
        var player = server.getPlayer(connId);
        SupportType supportType = (SupportType) args.get("type").getValue();

        if (player.changeCurrentBSP(-supportType.bspCost())) {

            var hitTheCorrectLocation = Compute.d6(2);
            var x = (int) args.get("x").getValue();
            var y = (int) args.get("y").getValue();

            var coord = getScatter(hitTheCorrectLocation, supportType, x, y);

            switch (supportType) {
                case OAS_LightStrike -> new AerialSupportCommand(this.server, this.gameManager)
                    .run(connId, new String[]{"/as", coord.getX()+ "", coord.getY() + "", "light_strike"});
                case OAS_LightBombing -> new AerialSupportCommand(this.server, this.gameManager)
                    .run(connId, new String[]{"/as", coord.getX()+ "", coord.getY() + "", "light_bombing"});
                case OAS_HeavyStrike -> new AerialSupportCommand(this.server, this.gameManager)
                    .run(connId, new String[]{"/as", coord.getX()+ "", coord.getY() + "", "heavy_strike"});
                case OAS_HeavyBombing -> new AerialSupportCommand(this.server, this.gameManager)
                    .run(connId, new String[]{"/as", coord.getX()+ "", coord.getY() + "", "heavy_bombing"});
                case OAS_Strafing -> new AerialSupportCommand(this.server, this.gameManager)
                    .run(connId, new String[]{"/as", coord.getX()+ "", coord.getY() + "", "strafing"});

                case ART_Thumper -> new ArtilleryStrikeCommand(this.server, this.gameManager)
                    .run(connId, new String[]{"/art", coord.getX()+ "", coord.getY() + "", "thumper"});
                case ART_Sniper -> new ArtilleryStrikeCommand(this.server, this.gameManager)
                    .run(connId, new String[]{"/art", coord.getX()+ "", coord.getY() + "", "sniper"});
                case ART_LongTom -> new ArtilleryStrikeCommand(this.server, this.gameManager)
                    .run(connId, new String[]{"/art", coord.getX()+ "", coord.getY() + "", "longtom"});
                case ART_ArrowIV -> new ArtilleryStrikeCommand(this.server, this.gameManager)
                    .run(connId, new String[]{"/art", coord.getX()+ "", coord.getY() + "", "arrowiv"});
                case ART_Illumination -> new ArtilleryStrikeCommand(this.server, this.gameManager)
                    .run(connId, new String[]{"/art", coord.getX()+ "", coord.getY() + "", "flare"});
                case ART_Smoke -> new ArtilleryStrikeCommand(this.server, this.gameManager)
                    .run(connId, new String[]{"/art", coord.getX()+ "", coord.getY() + "", "smoke"});
                case ART_Nuke -> new ArtilleryStrikeCommand(this.server, this.gameManager)
                    .run(connId, new String[]{"/art", coord.getX()+ "", coord.getY() + "", "nuke"});

                case ORT_bombardment -> new OrbitalBombardmentCommand(this.server, this.gameManager)
                    .run(connId, new String[]{"/ob", coord.getX()+ "", coord.getY() + ""});
            }

            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.bsp.success"));
        } else {
            server.sendServerChat(connId, Messages.getString("Gamemaster.cmd.bsp.fail"));
        }
    }

    private Coords getScatter(int hitTheCorrectLocation, SupportType supportType, int x, int y) {
        var coords = new Coords(x, y);
        if (hitTheCorrectLocation < supportType.targetNumber()) {
            var delta = supportType.targetNumber() - hitTheCorrectLocation;
            return coords.translated(Compute.d6(), delta);
        } else {
            return new Coords(x, y);
        }
    }
}
