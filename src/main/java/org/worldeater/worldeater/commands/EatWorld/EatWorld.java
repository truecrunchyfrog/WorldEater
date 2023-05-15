package org.worldeater.worldeater.commands.EatWorld;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.worldeater.worldeater.PlayerState;
import org.worldeater.worldeater.WorldEater;

import java.util.ArrayList;
import java.util.Collection;

public class EatWorld implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(strings.length > 0) {
            if(strings[0].equalsIgnoreCase("play")) {
                if(!(commandSender instanceof Player)) {
                    WorldEater.sendMessage(commandSender, "§cOnly players can play.");
                    return true;
                }

                WorldEater.sendMessage(commandSender, "§eFinding a game for you...");

                ArrayList<Game> gameInstances = Game.getInstances();

                Game joinGame = null;

                for(Game eachGame : gameInstances) {
                    if(eachGame.players.contains((Player) commandSender) || eachGame.spectators.contains((Player) commandSender)) {
                        WorldEater.sendMessage(commandSender, "§cYou are already in a game.");
                        return true;
                    }

                    if(eachGame.status == Game.GameStatus.AWAITING_START && eachGame.players.size() < Game.maxPlayers && (joinGame == null || joinGame.players.size() < eachGame.players.size()))
                        joinGame = eachGame;
                }

                if(joinGame == null)
                    joinGame = new Game(((Player) commandSender).getWorld(), false);

                joinGame.playerJoin((Player) commandSender, false);
            } else if(strings[0].equalsIgnoreCase("stop")) {
                if(!commandSender.isOp()) {
                    WorldEater.sendMessage(commandSender, "§cOnly operators may stop games.");
                    return true;
                }

                ArrayList<Game> gameInstances = Game.getInstances();
                Game autoSelectedGame = null;

                if(gameInstances.size() == 0) {
                    WorldEater.sendMessage(commandSender, "§cNo game is running.");
                    return true;
                }

                if(strings.length < 2) {
                    if(commandSender instanceof Player)
                        for(Game eachGame : gameInstances)
                            if(eachGame.world == ((Player) commandSender).getWorld()) {
                                autoSelectedGame = eachGame;
                                break;
                            }

                    if(autoSelectedGame == null) {
                        WorldEater.sendMessage(commandSender, "§cNo game chosen! Please provide a game ID together with the command: §e/eatworld stop <ID>§c, or §e/eatworld stop all§c to stop all games. Alternatively, run the game when in a game.");
                        return true;
                    }
                }

                if(autoSelectedGame == null && strings[1].equalsIgnoreCase("all")) {
                    WorldEater.sendMessage(commandSender, "Stopping all " + gameInstances.size() + " games...");

                    for(Game eachGame : gameInstances) {
                        WorldEater.sendMessage(commandSender, "Stopping game: #" + eachGame.gameId + " ...");
                        eachGame.stopHard(false);
                    }

                    WorldEater.sendMessage(commandSender, "§aAll games have been stopped.");
                } else {
                    Game instance = null;
                    if(autoSelectedGame == null) {
                        int gameId;

                        try {
                            gameId = Integer.parseInt(strings[1]);
                        } catch (NumberFormatException e) {
                            WorldEater.sendMessage(commandSender, "§cBad input. An ID (number) must be given.");
                            return true;
                        }

                        for (Game eachGame : gameInstances) {
                            if (eachGame.gameId == gameId) {
                                instance = eachGame;
                                break;
                            }
                        }
                    } else {
                        instance = autoSelectedGame;
                    }

                    if(instance != null) {
                        instance.stopHard(false);
                        WorldEater.sendMessage(commandSender, "§aGame #" + instance.gameId + " has been stopped.");
                    } else {
                        WorldEater.sendMessage(commandSender, "§cInvalid game ID.");
                    }
                }
            } else if(strings[0].equalsIgnoreCase("join")) {
                if(!(commandSender instanceof Player)) {
                    WorldEater.sendMessage(commandSender, "§cOnly players can run this command!");
                    return true;
                }

                ArrayList<Game> gameInstances = Game.getInstances();

                if(gameInstances.size() == 0) {
                    WorldEater.sendMessage(commandSender, "§cThere are no running games!");
                    return true;
                }

                if(strings.length < 2) {
                    WorldEater.sendMessage(commandSender, "§cNo game chosen! Please provide a game ID together with the command: §e/eatworld join <ID>§c.");
                    return true;
                }

                int gameId;

                try {
                    gameId = Integer.parseInt(strings[1]);
                } catch (NumberFormatException e) {
                    WorldEater.sendMessage(commandSender, "§cBad input. An ID (number) must be given.");
                    return true;
                }

                Game joinGame = null;

                for(Game eachGame : gameInstances) {
                    if(eachGame.players.contains((Player) commandSender) || eachGame.spectators.contains((Player) commandSender)) {
                        WorldEater.sendMessage(commandSender, "§cYou are already in a game.");
                        return true;
                    }

                    if(joinGame == null && eachGame.gameId == gameId)
                        joinGame = eachGame;
                }

                if(joinGame == null) {
                    WorldEater.sendMessage(commandSender, "§cInvalid game ID.");
                    return true;
                }

                joinGame.playerJoin((Player) commandSender, strings.length > 2 && strings[2].equalsIgnoreCase("spectate"));
            } else if(strings[0].equalsIgnoreCase("leave")) {
                if(!(commandSender instanceof Player)) {
                    WorldEater.sendMessage(commandSender, "§cOnly players can leave games.");
                    return true;
                }

                Player player = (Player) commandSender;

                ArrayList<Game> gameInstances = Game.getInstances();

                for(Game eachGame : gameInstances) {
                    if(eachGame.players.contains(player) || eachGame.spectators.contains(player)) {
                        eachGame.playerLeave(player);
                        break;
                    }
                }
            } else if(strings[0].equalsIgnoreCase("create")) {
                if(!(commandSender instanceof Player)) {
                    WorldEater.sendMessage(commandSender, "§cOnly players can create games.");
                    return true;
                }

                if(!commandSender.isOp()) {
                    WorldEater.sendMessage(commandSender, "§cOnly operators may create games.");
                    return true;
                }

                if(Game.getInstances().size() >= 50) {
                    WorldEater.sendMessage(commandSender, "§cToo many games are already running!");
                    return true;
                }

                WorldEater.sendMessage(commandSender, "Creating a game of WorldEater!");

                Game game = new Game(((Player) commandSender).getWorld(), strings.length > 1 && strings[1].equalsIgnoreCase("debug"));

                if(game.debug)
                    WorldEater.sendMessage(commandSender, "§3Debug mode enabled. Adding all online players.");

                if(game.debug || (strings.length > 1 && strings[1].equalsIgnoreCase("serverwide"))) {
                    int skippedPlayers = 0;

                    WorldEater.sendMessage(commandSender, "Adding all non-busy players to the game...");

                    Collection<? extends Player> serverPlayers = WorldEater.getPlugin().getServer().getOnlinePlayers();

                    for(Player eachPlayer : serverPlayers) {
                        boolean shouldInvitePlayer = true;

                        for(Game eachGame : Game.getInstances())
                            if(eachGame.players.contains(eachPlayer) || eachGame.spectators.contains(eachPlayer)) {
                                shouldInvitePlayer = false; // Don't invite players already in other games.
                                skippedPlayers++;
                                WorldEater.sendMessage(eachPlayer, "§cA server-wide game was to invite you.");
                                break;
                            }

                        if(shouldInvitePlayer) {
                            game.playerJoin(eachPlayer, false);
                            WorldEater.sendMessage(eachPlayer, "§eYou were automatically put in a game of WorldEater.");
                        }
                    }

                    if(skippedPlayers > 0)
                        WorldEater.sendMessage(commandSender, "§cSkipped " + skippedPlayers + " busy player(s).");
                    WorldEater.sendMessage(commandSender, "§aInvited " + (serverPlayers.size() - skippedPlayers) + " player(s) to game.");
                }
            }  else if(strings[0].equalsIgnoreCase("list")) {
                if(!commandSender.isOp()) {
                    WorldEater.sendMessage(commandSender, "§cOnly operators may list games.");
                    return true;
                }

                ArrayList<Game> gameInstances = Game.getInstances();

                if(gameInstances.size() == 0) {
                    WorldEater.sendMessage(commandSender, "§cThere are no running games!");
                    return true;
                }

                WorldEater.sendMessage(commandSender, "§aListing all §e" + gameInstances.size() + "§a games of WorldEater...");

                for(Game eachGame : gameInstances)
                    WorldEater.sendMessage(commandSender, "§3§l[ §3#" + eachGame.gameId + " §3§l]§e " + eachGame.players.size() + " players §6|§e Status: §o" + eachGame.status.name() + (eachGame.debug ? " §9(debugging mode)" : ""));
            } else if(strings[0].equalsIgnoreCase("resetme")) {
                if(!(commandSender instanceof Player)) {
                    WorldEater.sendMessage(commandSender, "§cOnly players can reset their state.");
                    return true;
                }


                Player player = (Player) commandSender;

                for(Game eachGame : Game.getInstances()) {
                    if(eachGame.players.contains(player) || eachGame.spectators.contains(player)) {
                        WorldEater.sendMessage(commandSender, "§cMust not be in a game.");
                        return true;
                    }
                }

                PlayerState.prepareNormal(player, false);
                WorldEater.sendMessage(commandSender, "§aYour state has been reset.");
                return true;
            } else {
                WorldEater.sendMessage(commandSender, "§cInvalid WorldEater command: §4§l" + strings[0] + "§c!");
                return true;
            }

            return true;
        }

        WorldEater.sendMessage(commandSender, "§cRun this command with an action! §o(join, leave, create, stop, list)");
        return true;
    }
}