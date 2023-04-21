package org.worldeater.worldeater.commands.EatWorld;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.worldeater.worldeater.WorldEater;

import java.util.ArrayList;

public class EatWorld implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(strings.length > 0) {
            if(strings[0].equalsIgnoreCase("stop")) {
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
                    if(commandSender instanceof Player) {
                        for(Game eachGame : gameInstances) {
                            if(eachGame.world == ((Player) commandSender).getWorld()) {
                                autoSelectedGame = eachGame;
                                break;
                            }
                        }
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
                    if(eachGame.players.contains((Player) commandSender)) {
                        WorldEater.sendMessage(commandSender, "§cYou are already in another game.");
                        return true;
                    }

                    if(joinGame == null && eachGame.gameId == gameId) {
                        joinGame = eachGame;
                    }
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
                if(!commandSender.isOp()) {
                    WorldEater.sendMessage(commandSender, "§cOnly operators may create games.");
                    return true;
                }

                if(Game.getInstances().size() >= 50) {
                    WorldEater.sendMessage(commandSender, "§cToo many games are already running!");
                    return true;
                }

                WorldEater.sendMessage(commandSender, "Creating a game of WorldEater!");

                Game game = new Game();

                if(strings.length > 1 && strings[1].equalsIgnoreCase("debug")) {
                    game.debug = true;
                    WorldEater.sendMessage(commandSender, "§3Debug mode enabled.");
                }
            } else {
                WorldEater.sendMessage(commandSender, "§cInvalid WorldEater command: §4§l" + strings[0] + "§c!");
                return true;
            }

            return true;
        }

        WorldEater.sendMessage(commandSender, "§cRun this command with an action! (join, leave, create, stop)");
        return true;
    }
}