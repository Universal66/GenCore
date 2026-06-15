package universal66.gencore;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Logger;

public final class GenCore extends JavaPlugin implements Listener {
    private static final Logger LOGGER = PluginLogger.getLogger("GenCore");
    private Runnable updatePending = null;

    @Override
    public void onLoad() {
        try {
            var website = URI.create("https://github.com/Universal66/GenCore/releases/latest/download/GenCore.jar").toURL();
            var rbc = Channels.newChannel(website.openStream());
            var fos = new FileOutputStream("plugins/GenCore-update.jar");

            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.getChannel().close();
            fos.close();

            rbc.close();

            var path = Path.of("plugins/GenCore-update.jar");
            var classLoader = new URLClassLoader(new URL[]{
                    path.toUri().toURL()
            });

            var newVersion = getPropertyFromYAML(classLoader.getResourceAsStream("plugin.yml"), "version");
            var oldVersion = "1.0";

            if (newVersion.equalsIgnoreCase(oldVersion)) {
                classLoader.close();
                Files.delete(path);
                LOGGER.warning("No new update available.");
            } else {
                Path self = findThis();
                if (self == null) {
                    LOGGER.severe("Self plugin JAR couldn't be found!");
                    return;
                }

                try {
                    Class<?> cls = classLoader.loadClass("universal66.gencore.UpdateDynamic");
                    var method = cls.getDeclaredMethod("any");
                    method.invoke(null);
                } catch (Exception e) {
//                    LOGGER.warning("Failed to execute on-update code.");
                }

                classLoader.close();

                this.updatePending = () -> {
                    if (Files.notExists(self)) {
                        LOGGER.severe("Update missing!");
                        return;
                    }

                    try {
                        while (true) {
                            try {
                                Files.delete(self);
                            } catch (IOException e) {
                                try {
                                    Thread.sleep(70);
                                } catch (InterruptedException ex) {
                                    LOGGER.severe("Update interrupted!");
                                    return;
                                }

                                continue;
                            } catch (Exception e) {
                                LOGGER.severe("Update interrupted!");
                                return;
                            }

                            break;
                        }

                        Files.move(path, self);

                        LOGGER.info("Update successful.");
                    } catch (IOException e) {
                        LOGGER.severe("Failed to update!");
                    }
                };

                LOGGER.info("Update pending.");
            }
        } catch (UnknownHostException ex) {
            LOGGER.warning("Omitting auto-update (no internet connection).");
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.severe("Cannot auto-update!");
        }
    }

    private static Path findThis() {
        try (var filesStream = Files.list(Path.of("plugins"))) {
            var files = filesStream.toList();
            for (Path path : files) {
                if (path.getFileName().toString().endsWith(".jar")) {
                    try (
                            URLClassLoader classLoader =
                            new URLClassLoader(new URL[]{path.toUri().toURL()})
                    ) {
                        var name = getPropertyFromYAML(classLoader.getResourceAsStream("plugin.yml"), "name");
                        if (name.equalsIgnoreCase("GenCore")) {
                            return path;
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (IOException e) {
            LOGGER.warning("Failed to find the self plugin JAR");
        }

        return null;
    }

    private static String getPropertyFromYAML(InputStream stream, String prop) throws IOException {
        try (stream) {
            var bytes = stream.readAllBytes();
            var lines = new String(bytes).split("\n");

            for (var line : lines) {
                if (line.startsWith(prop + ": ")) {
                    return line.split(":")[1].trim().replaceAll("['\"]", "");
                }
            }
        }

        return "";
    }

    private FileConfiguration config;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        config = getConfig();
        command_key = new NamespacedKey(this, "command");
        banhammer_key = new NamespacedKey(this, "ban_hammer");

        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equals("spawn")) {
            if (sender instanceof Player player) {
                if (!player.hasPermission("gencore.spawn")) {
                    sender.sendMessage("§7[§6SetSpawn§7] §cError: §4No permission");
                    return true;
                }

                if (player.hasPermission("gencore.spawn.others")) {
                    if (args.length >= 1) {
                        var name = args[0];
                        var target = getServer().getPlayer(name);

                        if (target != null) {
                            target.teleport(target.getWorld().getSpawnLocation());
                            sender.sendMessage("§7[§6SetSpawn§7] §aTeleported §e" + target.getName().replace("§", "") + "§a to spawn.");
                            return true;
                        }
                    }
                }

                player.teleport(player.getWorld().getSpawnLocation());
                sender.sendMessage("§7[§6SetSpawn§7] §aTeleported to spawn.");
            } else {
                sender.sendMessage("§7[§6SetSpawn§7] §cOnly executable by players");
            }

            return true;
        } else if (command.getName().equals("setspawn")) {
            if (sender instanceof Player player) {
                if (!player.hasPermission("gencore.setspawn")) {
                    sender.sendMessage("§7[§6SetSpawn§7] §cError: §4No permission");
                    return true;
                }

                player.getWorld().setSpawnLocation(player.getLocation());
                sender.sendMessage("§7[§6SetSpawn§7] §aSet the spawn location of your world.");
            } else {
                sender.sendMessage("§7[§6SetSpawn§7] §cOnly executable by players");
            }

            return true;
        } else if (command.getName().equals("generator")) {
            if (sender instanceof Player player) {
                if (!player.hasPermission("gencore.generator")) {
                    sender.sendMessage("§7[§6Generator§7] §cError: §4No permission");
                    return true;
                }

                if (args.length < 1) {
                    sender.sendMessage("§7[§6Generator§7] §7Usage: §n/generator <block>");
                    return true;
                }

                var block = String.join(" ", args);

                var cmdBlock = new ItemStack(Material.REPEATING_COMMAND_BLOCK);

                var meta = cmdBlock.getItemMeta();
                assert meta != null;

                var container = meta.getPersistentDataContainer();
                container.set(command_key, PersistentDataType.STRING, block);

                cmdBlock.setItemMeta(meta);
                player.getInventory().addItem(cmdBlock);

                sender.sendMessage("§7[§6Generator§7] §aYou have been given a generator block for §b" + block + "§a.");
            } else {
                sender.sendMessage("§7[§6Generator§7] §cOnly executable by players");
            }

            return true;
        } else if (command.getName().equals("up")) {
            if (sender instanceof Player player) {
                if (!player.hasPermission("gencore.up")) {
                    sender.sendMessage("§7[§6Up§7] §cError: §4No permission");
                    return true;
                }

                if (args.length < 1) {
                    sender.sendMessage("§7[§6Up§7] §7Usage: §n/up <amount>");
                    return true;
                }

                int num;

                var arg = args[0];
                try {
                    num = Integer.parseInt(arg);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§7[§6Up§7] §7Usage: §n/up <amount>");
                    return true;
                }

                player.teleport(player.getLocation().add(0, num, 0));
                sender.sendMessage("§7[§6Up§7] §aTeleported §b" + num + "§a blocks up.");
            } else {
                sender.sendMessage("§7[§6Up§7] §cOnly executable by players");
            }

            return true;
        } else if (command.getName().equals("down")) {
            if (sender instanceof Player player) {
                if (!player.hasPermission("gencore.down")) {
                    sender.sendMessage("§7[§6Down§7] §cError: §4No permission");
                    return true;
                }

                if (args.length < 1) {
                    sender.sendMessage("§7[§6Down§7] §7Usage: §n/down <amount>");
                    return true;
                }

                int num;

                var arg = args[0];
                try {
                    num = Integer.parseInt(arg);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§7[§6Down§7] §7Usage: §n/down <amount>");
                    return true;
                }

                player.teleport(player.getLocation().subtract(0, num, 0));
                sender.sendMessage("§7[§6Down§7] §aTeleported §b" + num + "§a blocks down.");
            } else {
                sender.sendMessage("§7[§6Down§7] §cOnly executable by players");
            }

            return true;
        } else if (command.getName().equals("banhammer")) {
            if (sender instanceof Player player) {
                if (!player.hasPermission("gencore.banhammer")) {
                    sender.sendMessage("§7[§6BanHammer§7] §cError: §4No permission");
                    return true;
                }

                if (args.length != 0) {
                    sender.sendMessage("§7[§6BanHammer§7] §7Usage: §n/banhammer");
                    return true;
                }

                var item = new ItemStack(Material.GOLDEN_AXE);
                var meta = item.getItemMeta();

                assert meta != null;

                meta.setUnbreakable(true);
                meta.setRarity(ItemRarity.EPIC);
                meta.setEnchantable(1);
                meta.setEnchantmentGlintOverride(true);
                meta.setDisplayName("§c§l§nBan Hammer");
                meta.getPersistentDataContainer().set(banhammer_key, PersistentDataType.BOOLEAN, true);

                item.setItemMeta(meta);

                player.getInventory().addItem(item);
                sender.sendMessage("§7[§6BanHammer§7] §aYou have been given a Ban Hammer.");
            } else {
                sender.sendMessage("§7[§6BanHammer§7] §cOnly executable by players");
            }

            return true;
        } else if (command.getName().equals("heal")) {
            if (sender instanceof Player player) {
                if (!player.hasPermission("gencore.heal")) {
                    sender.sendMessage("§7[§6Heal§7] §cError: §4No permission");
                    return true;
                }

                if (args.length > 0 && player.hasPermission("gencore.heal.others")) {
                    var plr = args[0];
                    if (!plr.isEmpty()) {
                        var target = getServer().getPlayer(plr);
                        if (target != null) {
                            target.setHealth(target.getMaxHealth());
                            target.setFoodLevel(20);

                            sender.sendMessage("§7[§6Heal§7] §aHealed §e" + target.getName().replace("§", "&") + "§a.");

                            return true;
                        }
                    }
                }

                player.setHealth(player.getMaxHealth());
                player.setFoodLevel(20);

                sender.sendMessage("§7[§6Heal§7] §aYou have been healed.");
            } else {
                sender.sendMessage("§7[§6Heal§7] §cOnly executable by players");
            }

            return true;
        } else if (command.getName().equals("enderchest")) {
            if (sender instanceof Player player) {
                if (!player.hasPermission("gencore.enderchest")) {
                    sender.sendMessage("§7[§6EnderChest§7] §cError: §4No permission");
                    return true;
                }

                if (args.length > 0 && player.hasPermission("gencore.enderchest.others")) {
                    var plr = args[0];
                    if (!plr.isEmpty()) {
                        var target = getServer().getPlayer(plr);
                        if (target != null) {
                            player.openInventory(target.getEnderChest());
                            sender.sendMessage("§7[§6EnderChest§7] §aShowing enderchest of §e" + target.getName().replace("§", "&") + "§a.");

                            return true;
                        }
                    }
                }

                player.openInventory(player.getEnderChest());
                sender.sendMessage("§7[§6EnderChest§7] §aYour enderchest has been opened.");
            } else {
                sender.sendMessage("§7[§6Heal§7] §cOnly executable by players");
            }

            return true;
        } else if (command.getName().equals("invsee")) {
            if (sender instanceof Player player) {
                if (!player.hasPermission("gencore.invsee")) {
                    sender.sendMessage("§7[§6InvSee§7] §cError: §4No permission");
                    return true;
                }

                if (args.length < 1) {
                    sender.sendMessage("§7[§6InvSee§7] §7Usage: §n/invsee <player>");
                    return true;
                }

                var plr = args[0];
                if (!plr.isEmpty()) {
                    var target = getServer().getPlayer(plr);
                    if (target != null) {
                        player.openInventory(target.getInventory());
                        sender.sendMessage("§7[§6InvSee§7] §aShowing inventory of §e" + target.getName().replace("§", "&") + "§a.");

                        return true;
                    }
                }

                sender.sendMessage("§7[§6InvSee§7] §cError: §4Player not found");
            } else {
                sender.sendMessage("§7[§6Heal§7] §cOnly executable by players");
            }

            return true;
        }

        return false;
    }

    private NamespacedKey command_key;
    private NamespacedKey banhammer_key;

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        var meta = event.getItemInHand().getItemMeta();
        assert meta != null;
        if (meta.getPersistentDataContainer().has(command_key)) {
            event.setCancelled(true);

            event.getBlockReplacedState().setType(Material.REPEATING_COMMAND_BLOCK);

            var loc = event.getBlockReplacedState().getLocation();

            getServer().dispatchCommand(getServer().getConsoleSender(), "execute at %s run gamerule command_block_output false".formatted(event.getPlayer().getUniqueId().toString()));
            getServer().dispatchCommand(getServer().getConsoleSender(), "execute at %s run gamerule command_blocks_work true".formatted(event.getPlayer().getUniqueId().toString()));

            getServer().dispatchCommand(getServer().getConsoleSender(), "data merge block %d %d %d {Command:\"setblock ~ ~2 ~ %s\",auto:1b}".formatted(
                    loc.getBlockX(),
                    loc.getBlockY(),
                    loc.getBlockZ(),
                    meta.getPersistentDataContainer().get(command_key, PersistentDataType.STRING)
            ));
        }
    }

    @SuppressWarnings("ConstantValue")
    @EventHandler
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        if (event.getPlayer() != null) {
            var inv = event.getPlayer().getInventory();
            var item = inv.getItemInMainHand();

            if (item != null) {
                var meta = item.getItemMeta();
                if (meta != null) {
                    var pdc = meta.getPersistentDataContainer();
                    if (pdc != null && Objects.equals(pdc.get(banhammer_key, PersistentDataType.BOOLEAN), true)) {
                        if (event.getRightClicked() instanceof Player victim)
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ban %s The Ban Hammer has struck!".formatted(victim.getUniqueId()));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        try {
            event.setRespawnLocation(event.getPlayer().getWorld().getSpawnLocation());
        } catch (NullPointerException ignored) {
            // That shouldn't happen
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (config.getBoolean("client_join_message.enabled")) {
            event.getPlayer().sendMessage(config.getString("client_join_message.value"));
        }

        if (config.getBoolean("join_message.custom"))
            event.setJoinMessage(config.getString("join_message.value").replace("%player%", event.getPlayer().getName().replace("§", "&")));

        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            try {
                event.getPlayer().teleport(event.getPlayer().getWorld().getSpawnLocation());
            } catch (NullPointerException ignored) {
                // That shouldn't happen
            }
        });

        if (config.getBoolean("client_join_sound.enabled"))
            event.getPlayer().playSound(
                    event.getPlayer().getLocation(),
                    config.getString("client_join_sound.sound"),
                    (float) config.getDouble("client_join_sound.volume"),
                    (float) config.getDouble("client_join_sound.pitch")
            );
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (config.getBoolean("quit_message.custom"))
            event.setQuitMessage(config.getString("quit_message.value").replace("%player%", event.getPlayer().getName().replace("§", "&")));
    }

    @Override
    public void onDisable() {
        if (updatePending != null)
            new Thread(updatePending).start();
    }
}
