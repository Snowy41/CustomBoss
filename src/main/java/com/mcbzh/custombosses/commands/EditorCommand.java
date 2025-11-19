package com.mcbzh.custombosses.commands;

import com.mcbzh.custombosses.CustomBossesPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EditorCommand implements CommandExecutor {

    private final CustomBossesPlugin plugin;

    public EditorCommand(CustomBossesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("editor")) {
                // Open Editor Hub
                com.mcbzh.custombosses.editor.EditorSession session = com.mcbzh.custombosses.CustomBossesPlugin
                        .getInstance().getEditorManager().getSession(player);
                session.openHub();
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                com.mcbzh.custombosses.CustomBossesPlugin.getInstance().getConfigManager().reload();
                player.sendMessage("§aConfiguration reloaded.");
                return true;
            }
            if (args[0].equalsIgnoreCase("undo")) {
                com.mcbzh.custombosses.editor.EditorSession session = com.mcbzh.custombosses.CustomBossesPlugin
                        .getInstance().getEditorManager().getSession(player);
                session.undo();
                return true;
            }
            if (args[0].equalsIgnoreCase("redo")) {
                com.mcbzh.custombosses.editor.EditorSession session = com.mcbzh.custombosses.CustomBossesPlugin
                        .getInstance().getEditorManager().getSession(player);
                session.redo();
                return true;
            }
            if (args[0].equalsIgnoreCase("debug")) {
                if (args.length > 2 && args[1].equalsIgnoreCase("spawn")) {
                    String modelId = args[2];
                    com.mcbzh.custombosses.model.ModelData model = com.mcbzh.custombosses.CustomBossesPlugin
                            .getInstance().getConfigManager().getModel(modelId);

                    if (model == null) {
                        player.sendMessage("§cModel not found: " + modelId);
                        return true;
                    }

                    com.mcbzh.custombosses.model.ModelInstance instance = new com.mcbzh.custombosses.model.ModelInstance(
                            model, player.getLocation());
                    instance.spawn();

                    player.sendMessage("§aDebug spawned model: " + modelId);
                    return true;
                }
                if (args.length > 1 && args[1].equalsIgnoreCase("list")) {
                    player.sendMessage("§eAvailable Models:");
                    for (com.mcbzh.custombosses.model.ModelData m : com.mcbzh.custombosses.CustomBossesPlugin
                            .getInstance().getConfigManager().getAllModels()) {
                        player.sendMessage(" - " + m.getId());
                    }
                    return true;
                }
                if (args.length > 1 && args[1].equalsIgnoreCase("clear")) {
                    com.mcbzh.custombosses.CustomBossesPlugin.getInstance().getBossManager().removeAllBosses();
                    player.sendMessage("§aCleared all active bosses/models.");
                    return true;
                }
            }
        }
        return false;
    }
}