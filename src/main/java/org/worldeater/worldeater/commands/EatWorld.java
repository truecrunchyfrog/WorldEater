package org.worldeater.worldeater.commands;

import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;
import org.worldeater.worldeater.WorldEater;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class EatWorld implements CommandExecutor, Listener {
    private static boolean playing = false;
    private static boolean allowPlayerJoin = false;
    private static EatWorld instance;
    private static ArrayList<Player> players;
    private static ArrayList<Player> unmovable_players;
    private static Player hider;
    private static World world;
    private static ArrayList<BukkitTask> bukkitTasks;
    private static boolean debug;

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if(strings.length > 0 && strings[0].equalsIgnoreCase("stop")) {
            if(!playing) {
                commandSender.sendMessage("§cGame is not running!");
                return false;
            }

            endGameHard(false);
            return true;
        }

        if(strings.length > 0 && strings[0].equalsIgnoreCase("join")) {
            if(!(commandSender instanceof Player)) {
                commandSender.sendMessage("§cGames are only joinable as players!");
                return false;
            }

            if(!playing) {
                commandSender.sendMessage("§cNo game to join.");
                return false;
            }

            if(players.contains((Player) commandSender)) {
                commandSender.sendMessage("§cYou already joined this game!");
                return false;
            }

            if(!allowPlayerJoin) {
                commandSender.sendMessage("§cThis game has already started!");
                return false;
            }

            int playerLimit = 5;

            if(players.size() >= playerLimit) {
                commandSender.sendMessage("§cGame is full!");
                return false;
            }

            players.add((Player) commandSender);
            commandSender.sendMessage("§aYou joined! Please wait for the game to start.");
            WorldEater.sendBroadcast("§a" + commandSender.getName() + "§8 joined the game queue (§6" + players.size() + "§8/§6" + playerLimit + "§8).");

            if(players.size() == playerLimit) {
                WorldEater.sendBroadcast("§aGame has been filled!");
            }

            return true;
        }

        if(playing) {
            commandSender.sendMessage("§cGame is already being played!");
            return false;
        }

        if(strings.length > 0 && strings[0].equalsIgnoreCase("debug")) {
            debug = true;
            commandSender.sendMessage("§aDebug mode enabled.");
        }

        playing = true;
        instance = this;

        WorldEater.sendBroadcast("§6A game of WorldEater is starting in §c20§6 seconds.");
        WorldEater.sendBroadcast("§6To join the game, type §f/eatworld join§6.");

        players = new ArrayList<Player>();

        WorldEater.getPlugin().getServer().getPluginManager().registerEvents(EatWorld.this, WorldEater.getPlugin());

        allowPlayerJoin = true;

        new BukkitRunnable() {
            @Override
            public void run() {
                allowPlayerJoin = false;

                if(!debug && players.size() < 2) {
                    players = null;
                    playing = false;
                    instance = null;

                    WorldEater.sendBroadcast("Not enough players joined. Game aborted.");
                    return;
                }

                unmovable_players = new ArrayList<>();
                bukkitTasks = new ArrayList<>();

                sendGameMessage("Ready to start game!");
                sendGameMessage("Creating normal world...");

                String worldName = "WorldEater_Game";

                WorldCreator normalWorldCreator = new WorldCreator(worldName + "T");
                normalWorldCreator.type(WorldType.NORMAL);
                normalWorldCreator.biomeProvider(new BiomeProvider() {
                    @Override
                    public Biome getBiome(WorldInfo worldInfo, int i, int i1, int i2) {
                        return Biome.DARK_FOREST;
                    }

                    @Override
                    public List<Biome> getBiomes(WorldInfo worldInfo) {
                        List<Biome> result = new ArrayList<Biome>();
                        result.add(Biome.DARK_FOREST);
                        return result;
                    }
                });
                normalWorldCreator.generateStructures(true);
                World normalWorld = normalWorldCreator.createWorld();

                sendGameMessage("Creating void world...");

                WorldCreator worldCreator = new WorldCreator(worldName);

                worldCreator.generator(new ChunkGenerator() {
                    @Override
                    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome) {
                        return createChunkData(world);
                    }
                });

                world = worldCreator.createWorld();

                // Clone chunk

                Chunk normalChunk = normalWorld.getChunkAt(new Location(world, 0, 0, 0));
                Chunk chunk = world.getChunkAt(0, 0);

                sendGameMessage("Cloning chunk...");

                for(int x = 0; x < 16; x++) {
                    for(int z = 0; z < 16; z++) {
                        for(int y = -64; y <= normalWorld.getHighestBlockYAt(x, z); y++) {
                            Material sourceMaterial = normalChunk.getBlock(x, y, z).getType();
                            if(!sourceMaterial.isAir()) {
                                chunk.getBlock(x, y, z).setType(sourceMaterial);
                            }
                        }
                    }
                }

                WorldEater.getPlugin().getServer().unloadWorld(normalWorld, false);

                sendGameMessage("Unloading normal world...");

                try {
                    FileUtils.deleteDirectory(normalWorld.getWorldFolder());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                sendGameMessage("Deleting normal world...");

                world.setPVP(false);

                sendGameMessage("Game world created!");
                sendGameMessage("Moving players...");

                for(Player eachPlayer : players) {
                    eachPlayer.teleport(new Location(world, 8, world.getHighestBlockYAt(8, 8) + 2, 8));

                    Scoreboard score = Bukkit.getScoreboardManager().getMainScoreboard();

                    Team t = score.getTeam("nhide");
                    if(t == null) {
                        t = score.registerNewTeam("nhide");
                        t.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
                    }
                    t.addEntry(eachPlayer.getName());

                    eachPlayer.setGameMode(GameMode.ADVENTURE);
                    eachPlayer.setHealth(20);
                    eachPlayer.setFoodLevel(20);

                    eachPlayer.getInventory().clear();

                    for(PotionEffect potionEffect : eachPlayer.getActivePotionEffects()) {
                        eachPlayer.removePotionEffect(potionEffect.getType());
                    }
                }

                hider = (Player) players.toArray()[(int) (players.size() * Math.random())];

                for(Player eachPlayer : players) {
                    eachPlayer.sendTitle(
                            !eachPlayer.equals(hider) ? "§c§lSEEKER" : "§a§lHIDER",
                            !eachPlayer.equals(hider) ? "§eFind and eliminate §9" + hider.getName() + "§e, the hider." : "§eEndure the seekers attempts to kill you.", 0, 20 * 5, 0);
                }

                sendGameMessage("§aHider is §9§l" + hider.getName() + "§a!");
                sendGameMessage("§eGet ready! Game starts in 10 seconds...");

                bukkitTasks.add(new BukkitRunnable() {
                    @Override
                    public void run() {
                        sendGameMessage("§eHider §9§l" + hider.getName() + "§e is given a 2 minute head start.");

                        for(Player eachPlayer : players) {
                            if(!eachPlayer.equals(hider)) {
                                eachPlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 2 * 60, 10, true, false, false));
                                unmovable_players.add(eachPlayer);
                                eachPlayer.setAllowFlight(true);
                                eachPlayer.setFlying(true);
                                eachPlayer.setInvisible(true);
                                eachPlayer.teleport(new Location(world, 8, world.getHighestBlockYAt(8, 8) + 100, 8));
                                eachPlayer.sendTitle("§ePlease wait", "§ewhile hider is getting ready.", 0, 20 * 10, 0);
                            }
                        }

                        hider.setGameMode(GameMode.SURVIVAL);
                        for(int i = 2 * 60; i > 0; i--) {
                            int finalI = i;
                            bukkitTasks.add(new BukkitRunnable() {
                                @Override
                                public void run() {
                                    String timeLeftString = (finalI >= 60 ? "§c" + (int) (finalI / 60) + "§em " : "") + "§c" + finalI % 60 + "§es";

                                    if(finalI % 5 == 0)
                                        sendGameMessage("§eSeekers are released in " + timeLeftString + ".");

                                    for(Player eachPlayer : players) {
                                        if(!eachPlayer.equals(hider)) {
                                            eachPlayer.sendTitle(timeLeftString, "§euntil released...", 0, 20 * 6, 0);
                                        }
                                    }
                                }
                            }.runTaskLater(WorldEater.getPlugin(), 20 * (2 * 60 - i)));
                        }

                        bukkitTasks.add(new BukkitRunnable() {
                            @Override
                            public void run() {
                                world.setPVP(true);

                                sendGameMessage("§8---------------------------");
                                sendGameMessage("§c§lSEEKERS HAVE BEEN RELEASED!");
                                unmovable_players.clear();

                                for(Player eachPlayer : players) {
                                    if(!eachPlayer.equals(hider)) {
                                        eachPlayer.removePotionEffect(PotionEffectType.BLINDNESS);
                                        eachPlayer.setAllowFlight(false);
                                        eachPlayer.setFlying(false);
                                        eachPlayer.setInvisible(false);
                                        eachPlayer.teleport(new Location(world, 8, world.getHighestBlockYAt(8, 8) + 2, 8));
                                        eachPlayer.setGameMode(GameMode.SURVIVAL);
                                    }
                                    eachPlayer.playSound(eachPlayer, Sound.BLOCK_ANVIL_LAND, 2, 2);
                                }

                                sendGameMessage("§c(!) Starting from the top of the island to the bottom, each layer of blocks will be removed at an increasing rate.");

                                int startY = 0;

                                for(int x = 0; x < 16; x++) {
                                    for(int z = 0; z < 16; z++) {
                                        startY = Math.max(startY, world.getHighestBlockYAt(x, z));
                                    }
                                }

                                long progress = 0;

                                for(int y = startY; y > -64; y--) {
                                    int finalY = y;
                                    progress += 20L * 30 * Math.pow(0.97D, 1 + startY - y);
                                    bukkitTasks.add(new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            for(Player eachPlayer : players) {
                                                eachPlayer.playSound(eachPlayer, Sound.BLOCK_BAMBOO_WOOD_PRESSURE_PLATE_CLICK_OFF, 1, 1);
                                            }

                                            for(int x = 0; x < 16; x++) {
                                                for(int z = 0; z < 16; z++) {
                                                    world.setBlockData(chunk.getBlock(x, finalY, z).getLocation(), Material.AIR.createBlockData());
                                                }
                                            }
                                        }
                                    }.runTaskLater(WorldEater.getPlugin(), progress));
                                }

                                sendGameMessage("§eIf the hider survives until the game is over, they win. Otherwise the seekers win.");

                                int gameDuration = 30;
                                for(int i = gameDuration; i >= 0; i -= 5) {
                                    int finalI = i;
                                    bukkitTasks.add(new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            if(finalI > 0) {
                                                sendGameMessage("§eThe game has §c" + finalI + "§e minutes remaining.");

                                                switch(finalI) {
                                                    case 10:
                                                        sendGameMessage("§eThe world border will shrink in §c30§e seconds!");

                                                        for(Player eachPlayer : players) {
                                                            eachPlayer.playSound(eachPlayer, Sound.ITEM_GOAT_HORN_SOUND_6, 4, 4);
                                                        }

                                                        bukkitTasks.add(new BukkitRunnable() {
                                                            @Override
                                                            public void run() {
                                                                for(Player eachPlayer : players) {
                                                                    eachPlayer.playSound(eachPlayer, Sound.ITEM_GOAT_HORN_SOUND_6, 5, 5);
                                                                }

                                                                world.getWorldBorder().setSize(32);
                                                                sendGameMessage("§eWorld border has shrunk!");
                                                            }
                                                        }.runTaskLater(WorldEater.getPlugin(), 20 * 30));
                                                        break;
                                                    case 5:
                                                        sendGameMessage("§8<§k-§8> §4§lSUDDEN DEATH! §cExploding horses will summon randomly and approach players.");

                                                        for(Player eachPlayer : players) {
                                                            eachPlayer.playSound(eachPlayer, Sound.ENTITY_HORSE_ANGRY, 5, 5);
                                                        }

                                                        for(int i = 0; i < 50; i++) {
                                                            bukkitTasks.add(new BukkitRunnable() {
                                                                @Override
                                                                public void run() {
                                                                    if (Math.random() * 10 < 2) {
                                                                        Player unluckyPlayer = (Player) players.toArray()[(int) (players.size() * Math.random())];

                                                                        Location targetLocation = unluckyPlayer.getLocation();
                                                                        Location horseLocation = targetLocation.add(
                                                                                new Vector().
                                                                                        setX(Math.random() * 15 + 10).
                                                                                        setY(Math.random() * 5).
                                                                                        setZ(Math.random() * 15 + 10)
                                                                        );

                                                                        Entity horse = world.spawnEntity(horseLocation, EntityType.HORSE);
                                                                        Vector horseApproach = new Vector().
                                                                                setX(targetLocation.getX() - horseLocation.getX()).
                                                                                setY(targetLocation.getY() - horseLocation.getY()).
                                                                                setZ(targetLocation.getZ() - horseLocation.getZ());
                                                                        horse.setVelocity(horseApproach.multiply(0.3));
                                                                        horse.getLocation().setDirection(horseApproach);
                                                                        //horse.setGravity(false);
                                                                        bukkitTasks.add(new BukkitRunnable() {
                                                                            @Override
                                                                            public void run() {
                                                                                horse.remove();
                                                                                world.createExplosion(horse.getLocation(), 2);
                                                                            }
                                                                        }.runTaskLater(WorldEater.getPlugin(), 20 * 3));
                                                                    }
                                                                }
                                                            }.runTaskLater(WorldEater.getPlugin(), 20 * 3 * i));
                                                        }
                                                        break;
                                                }
                                            } else {
                                                sendGameMessage("§aTime has gone out! Hider wins.");
                                                endGame(true);
                                            }
                                        }
                                    }.runTaskLater(WorldEater.getPlugin(), 20L * 60 * (gameDuration - i)));
                                }
                            }
                        }.runTaskLater(WorldEater.getPlugin(), 20 * 2 * 60));
                    }
                }.runTaskLater(WorldEater.getPlugin(), 20 * 10));
            }
        }.runTaskLater(WorldEater.getPlugin(), 20 * 20);


        return true;
    }

    private void sendGameMessage(String s) {
        if(players == null)
            return;

        for(Player eachPlayer : players) {
            eachPlayer.sendMessage(WorldEater.messagePrefix + s);
        }
    }

    @EventHandler
    private void onPlayerMove(PlayerMoveEvent e) {
        if(unmovable_players != null && unmovable_players.contains(e.getPlayer()) && e.getFrom() != e.getTo()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    private void onPlayerDeath(PlayerDeathEvent e) {
        final Player p = e.getEntity();
        Location realSpawn = p.getBedSpawnLocation();
        p.setBedSpawnLocation(new Location(world, 8, world.getHighestBlockYAt(8, 8) + 2, 8));
        Bukkit.getScheduler().scheduleSyncDelayedTask(WorldEater.getPlugin(), () -> {
            p.spigot().respawn();
            p.setBedSpawnLocation(realSpawn);

            if(!p.equals(hider)) {
                p.setAllowFlight(true);
                p.setFlying(true);
                p.setInvulnerable(true);
                p.setInvisible(true);

                unmovable_players.add(p);

                p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 10, 10, true, false));

                for (int i = 10; i > 0; i--) {
                    int finalI = i;
                    bukkitTasks.add(new BukkitRunnable() {
                        @Override
                        public void run() {
                            p.sendTitle("§c" + finalI, "§euntil you can continue...", 0, 20 * 2, 0);
                        }
                    }.runTaskLater(WorldEater.getPlugin(), 20L * (10 - i)));
                }

                bukkitTasks.add(new BukkitRunnable() {
                    @Override
                    public void run() {
                        p.setAllowFlight(false);
                        p.setFlying(false);
                        p.setInvulnerable(false);
                        p.setInvisible(false);

                        unmovable_players.remove(p);
                    }
                }.runTaskLater(WorldEater.getPlugin(), 20 * 10));
            }
        }, 2);

        if(p.equals(hider)) { // Hider died and lost
            endGame(false);
        }
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent e) {
        if(allowPlayerJoin) {
            players.remove(e.getPlayer());
            WorldEater.sendBroadcast("§cA player in queue left.");
            return;
        }

        if(players.contains(e.getPlayer())) {
            players.remove(e.getPlayer());
            sendGameMessage("§cA player quit, so the game is over.");
            endGame(!e.getPlayer().equals(hider));
        }
    }

    @EventHandler
    private void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        if(players != null && players.contains(e.getPlayer()) && !e.getPlayer().getWorld().equals(world))
            e.getPlayer().teleport(new Location(world, 8, world.getHighestBlockYAt(8, 8) + 2, 8));
    }

    private static void endGameHard(boolean wait) {
        for(BukkitTask bukkitTask : bukkitTasks)
            bukkitTask.cancel();

        Bukkit.getScoreboardManager().getMainScoreboard().resetScores("nhide");

        PlayerMoveEvent.getHandlerList().unregister(instance);
        PlayerDeathEvent.getHandlerList().unregister(instance);
        PlayerQuitEvent.getHandlerList().unregister(instance);

        new BukkitRunnable() {
            @Override
            public void run() {
                instance.sendGameMessage("Moving players from world...");

                for(Player player : world.getPlayers()) {
                    player.setInvulnerable(false);
                    player.setInvisible(false);
                    Location movePlayerTo = player.getBedSpawnLocation();
                    if(movePlayerTo == null)
                        movePlayerTo = new Location(WorldEater.getPlugin().getServer().getWorlds().get(0), 0, 0, 0);
                    player.teleport(movePlayerTo);
                }

                instance.sendGameMessage("Unloading world...");

                WorldEater.getPlugin().getServer().unloadWorld(world, false);

                instance.sendGameMessage("Deleting world...");

                try {
                    FileUtils.deleteDirectory(world.getWorldFolder());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                instance.sendGameMessage("Goodbye!");

                world = null;
                players = null;
                unmovable_players = null;
                hider = null;
                bukkitTasks = null;
                debug = false;
                allowPlayerJoin = false;

                playing = false;
                instance = null;
            }
        }.runTaskLater(WorldEater.getPlugin(), wait ? 20 * 10 : 0);
    }

    private static void endGame(boolean hiderWins) {
        endGameHard(true);

        for(Player eachPlayer : players) {
            eachPlayer.sendTitle((!hiderWins && !eachPlayer.equals(hider)) || (hiderWins && eachPlayer.equals(hider)) ? "§a§lVictory!" : "§c§lLost!", !hiderWins ? "§eThe seekers won the game." : "§eThe hider won the game.", 0, 20 * 6, 0);
            eachPlayer.playSound(eachPlayer, Sound.BLOCK_BELL_USE, 3, 3);

            eachPlayer.setGameMode(GameMode.SURVIVAL);
        }

        instance.sendGameMessage("§aThe game has ended. Thanks for playing.");

        for(int i = 0; i < 10; i++) {
            double x = Math.random() * 48 - 16, z = Math.random() * 48 - 16;
            world.spawnEntity(new Location(world, x, world.getHighestBlockYAt((int) x, (int) z), z), EntityType.FIREWORK);
        }
    }
}