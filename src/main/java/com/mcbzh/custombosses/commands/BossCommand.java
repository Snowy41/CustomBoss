package com.mcbzh.custombosses.commands;

import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.model.ModelData;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class BossCommand implements CommandExecutor, TabCompleter {

    private final CustomBossesPlugin plugin;

    public BossCommand(CustomBossesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "editor" -> {
                plugin.getEditorManager().getSession(player).openHub();
            }

            case "spawn" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /cb spawn <model>");
                    return true;
                }

                ModelData model = plugin.getModelStorage().get(args[1]);
                if (model == null) {
                    player.sendMessage("§cModel not found: " + args[1]);
                    return true;
                }

                plugin.getBossManager().spawnBoss(model, player.getLocation());
                player.sendMessage("§aSpawned boss: " + args[1]);
            }

            case "list" -> {
                player.sendMessage("§e=== Available Models ===");
                plugin.getModelStorage().getAllModels().forEach(m ->
                        player.sendMessage("§7- §f" + m.getId() + " §8(" + m.getParts().size() + " parts)")
                );
            }

            case "clear" -> {
                plugin.getBossManager().removeAll();
                player.sendMessage("§aRemoved all bosses");
            }

            case "reload" -> {
                plugin.getModelStorage().reload();
                player.sendMessage("§aReloaded models");
            }

            case "undo" -> {
                plugin.getEditorManager().getSession(player).undo();
            }

            case "redo" -> {
                plugin.getEditorManager().getSession(player).redo();
            }

            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage("§e=== Custom Bosses ===");
        player.sendMessage("§7/cb editor §f- Open editor");
        player.sendMessage("§7/cb spawn <model> §f- Spawn boss");
        player.sendMessage("§7/cb list §f- List models");
        player.sendMessage("§7/cb clear §f- Remove all bosses");
        player.sendMessage("§7/cb reload §f- Reload models");
        player.sendMessage("§7/cb undo/redo §f- Undo/redo edit");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("editor", "spawn", "list", "clear", "reload", "undo", "redo")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            return plugin.getModelStorage().getAllModels().stream()
                    .map(ModelData::getId)
                    .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}