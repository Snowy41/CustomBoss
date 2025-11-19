package com.mcbzh.custombosses.commands;

import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.editor.EditorSession;
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

            case "debug" -> {
                handleDebugCommand(player, args);
            }

            case "gizmo" -> {
                handleGizmoCommand(player, args);
            }

            default -> sendHelp(player);
        }

        return true;
    }

    private void handleDebugCommand(Player player, String[] args) {
        EditorSession session = plugin.getEditorManager().getSession(player);

        if (args.length < 2) {
            player.sendMessage("§e=== Debug Options ===");
            player.sendMessage("§7/cb debug hitbox §f- Toggle hitbox display");
            player.sendMessage("§7/cb debug names §f- Toggle part names");
            player.sendMessage("§7/cb debug hierarchy §f- Toggle parent-child lines");
            player.sendMessage("§7/cb debug transforms §f- Toggle transform axes");
            player.sendMessage("§7/cb debug all §f- Show all debug info");
            player.sendMessage("§7/cb debug off §f- Hide all debug info");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "hitbox" -> {
                boolean current = toggleDebugOption(session, "hitbox");
                player.sendMessage("§7Hitbox display: " + (current ? "§aON" : "§cOFF"));
            }
            case "names" -> {
                boolean current = toggleDebugOption(session, "names");
                player.sendMessage("§7Part names: " + (current ? "§aON" : "§cOFF"));
            }
            case "hierarchy" -> {
                boolean current = toggleDebugOption(session, "hierarchy");
                player.sendMessage("§7Hierarchy lines: " + (current ? "§aON" : "§cOFF"));
            }
            case "transforms" -> {
                boolean current = toggleDebugOption(session, "transforms");
                player.sendMessage("§7Transform axes: " + (current ? "§aON" : "§cOFF"));
            }
            case "all" -> {
                session.getDebugVisualizer().setShowHitboxes(true);
                session.getDebugVisualizer().setShowPartNames(true);
                session.getDebugVisualizer().setShowHierarchy(true);
                session.getDebugVisualizer().setShowTransforms(true);
                refreshDebugDisplay(session, player);
                player.sendMessage("§aAll debug displays enabled");
            }
            case "off" -> {
                session.getDebugVisualizer().setShowHitboxes(false);
                session.getDebugVisualizer().setShowPartNames(false);
                session.getDebugVisualizer().setShowHierarchy(false);
                session.getDebugVisualizer().setShowTransforms(false);
                session.getDebugVisualizer().clearAll();
                player.sendMessage("§cAll debug displays disabled");
            }
            default -> {
                player.sendMessage("§cUnknown debug option: " + args[1]);
            }
        }
    }

    private boolean toggleDebugOption(EditorSession session, String option) {
        boolean current = false;
        switch (option) {
            case "hitbox" -> {
                current = !session.getDebugVisualizer().toString().contains("hitbox"); // Simplified
                session.getDebugVisualizer().setShowHitboxes(current);
            }
            case "names" -> {
                current = !session.getDebugVisualizer().toString().contains("names");
                session.getDebugVisualizer().setShowPartNames(current);
            }
            case "hierarchy" -> {
                current = !session.getDebugVisualizer().toString().contains("hierarchy");
                session.getDebugVisualizer().setShowHierarchy(current);
            }
            case "transforms" -> {
                current = !session.getDebugVisualizer().toString().contains("transforms");
                session.getDebugVisualizer().setShowTransforms(current);
            }
        }
        refreshDebugDisplay(session, session.getPlayer());
        return current;
    }

    private void refreshDebugDisplay(EditorSession session, Player player) {
        if (session.getCurrentInstance() != null) {
            session.getDebugVisualizer().visualizeModelInstance(
                    session.getCurrentInstance(),
                    player
            );
        }
    }

    private void handleGizmoCommand(Player player, String[] args) {
        EditorSession session = plugin.getEditorManager().getSession(player);

        if (args.length < 2) {
            player.sendMessage("§e=== Gizmo Options ===");
            player.sendMessage("§7/cb gizmo axes §f- Toggle XYZ axes");
            player.sendMessage("§7/cb gizmo grid §f- Toggle ground grid");
            player.sendMessage("§7/cb gizmo labels §f- Toggle axis labels");
            player.sendMessage("§7/cb gizmo all §f- Show all gizmos");
            player.sendMessage("§7/cb gizmo off §f- Hide all gizmos");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "axes" -> {
                // Toggle logic here
                player.sendMessage("§7Axes: §aToggled");
            }
            case "grid" -> {
                // Toggle logic here
                player.sendMessage("§7Grid: §aToggled");
            }
            case "labels" -> {
                // Toggle logic here
                player.sendMessage("§7Labels: §aToggled");
            }
            case "all" -> {
                session.getGizmoManager().setAxesEnabled(true);
                session.getGizmoManager().setGridEnabled(true);
                session.getGizmoManager().setLabelsEnabled(true);
                player.sendMessage("§aAll gizmos enabled");
            }
            case "off" -> {
                session.getGizmoManager().hideGizmo();
                player.sendMessage("§cGizmos hidden");
            }
            default -> {
                player.sendMessage("§cUnknown gizmo option: " + args[1]);
            }
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage("§e=== Custom Bosses ===");
        player.sendMessage("§7/cb editor §f- Open editor");
        player.sendMessage("§7/cb spawn <model> §f- Spawn boss");
        player.sendMessage("§7/cb list §f- List models");
        player.sendMessage("§7/cb clear §f- Remove all bosses");
        player.sendMessage("§7/cb reload §f- Reload models");
        player.sendMessage("§7/cb undo/redo §f- Undo/redo edit");
        player.sendMessage("§7/cb debug [option] §f- Debug visualizations");
        player.sendMessage("§7/cb gizmo [option] §f- Gizmo controls");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("editor", "spawn", "list", "clear", "reload",
                            "undo", "redo", "debug", "gizmo")
                    .stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("spawn")) {
                return plugin.getModelStorage().getAllModels().stream()
                        .map(ModelData::getId)
                        .filter(s -> s.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("debug")) {
                return Arrays.asList("hitbox", "names", "hierarchy", "transforms", "all", "off")
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("gizmo")) {
                return Arrays.asList("axes", "grid", "labels", "all", "off")
                        .stream()
                        .filter(s -> s.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}