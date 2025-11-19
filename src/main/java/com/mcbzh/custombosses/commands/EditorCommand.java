package com.mcbzh.custombosses.commands;

import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.model.ModelData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EditorCommand implements CommandExecutor, TabCompleter {

    private final CustomBossesPlugin plugin;

    public EditorCommand(CustomBossesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "editor" -> {
                plugin.getEditorManager().getSession(player).openHub();
                return true;
            }

            case "spawn" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /cb spawn <modelId>");
                    return true;
                }

                String modelId = args[1];
                ModelData model = plugin.getConfigManager().getModel(modelId);

                if (model == null) {
                    player.sendMessage("§cModel not found: " + modelId);
                    player.sendMessage("§eAvailable models: " +
                            plugin.getConfigManager().getAllModels().stream()
                                    .map(ModelData::getId)
                                    .collect(Collectors.joining(", ")));
                    return true;
                }

                plugin.getBossManager().spawnBoss(model, player.getLocation());
                player.sendMessage("§aSpawned boss: " + modelId);
                return true;
            }

            case "reload" -> {
                plugin.getConfigManager().reload();
                player.sendMessage("§aConfiguration reloaded.");
                return true;
            }

            case "undo" -> {
                plugin.getEditorManager().getSession(player).undo();
                return true;
            }

            case "redo" -> {
                plugin.getEditorManager().getSession(player).redo();
                return true;
            }

            case "list" -> {
                player.sendMessage("§e=== Available Models ===");
                for (ModelData m : plugin.getConfigManager().getAllModels()) {
                    player.sendMessage("§7- §f" + m.getId() + " §8(" + m.getParts().size() + " parts)");
                }
                return true;
            }

            case "clear" -> {
                plugin.getBossManager().removeAllBosses();
                player.sendMessage("§aCleared all active bosses.");
                return true;
            }

            case "debug" -> {
                if (args.length > 1 && args[1].equalsIgnoreCase("spawn")) {
                    if (args.length < 3) {
                        player.sendMessage("§cUsage: /cb debug spawn <modelId>");
                        return true;
                    }

                    String modelId = args[2];
                    ModelData model = plugin.getConfigManager().getModel(modelId);

                    if (model == null) {
                        player.sendMessage("§cModel not found: " + modelId);
                        return true;
                    }

                    com.mcbzh.custombosses.model.ModelInstance instance =
                            new com.mcbzh.custombosses.model.ModelInstance(model, player.getLocation());
                    instance.spawn();

                    player.sendMessage("§aDebug spawned model: " + modelId);
                    return true;
                }
                player.sendMessage("§cUsage: /cb debug spawn <modelId>");
                return true;
            }

            default -> {
                sendHelp(player);
                return true;
            }
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("§e=== Custom Bosses Commands ===");
        player.sendMessage("§7/cb editor §f- Open the editor GUI");
        player.sendMessage("§7/cb spawn <model> §f- Spawn a boss");
        player.sendMessage("§7/cb list §f- List all models");
        player.sendMessage("§7/cb reload §f- Reload configurations");
        player.sendMessage("§7/cb clear §f- Remove all bosses");
        player.sendMessage("§7/cb undo §f- Undo last editor action");
        player.sendMessage("§7/cb redo §f- Redo last editor action");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("editor", "spawn", "list", "reload", "clear", "undo", "redo", "debug")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            return plugin.getConfigManager().getAllModels().stream()
                    .map(ModelData::getId)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}