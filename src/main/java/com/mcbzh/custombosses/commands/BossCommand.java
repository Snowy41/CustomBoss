package com.mcbzh.custombosses.commands;

import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.abilities.impl.*;
import com.mcbzh.custombosses.animation.AnimationStateMachine;
import com.mcbzh.custombosses.boss.CustomBoss;
import com.mcbzh.custombosses.editor.EditorSession;
import com.mcbzh.custombosses.model.ModelData;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced command system with Phase 1 features
 */
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
                    player.sendMessage("§cUsage: /cb spawn <model> [abilities...]");
                    return true;
                }

                ModelData model = plugin.getModelStorage().get(args[1]);
                if (model == null) {
                    player.sendMessage("§cModel not found: " + args[1]);
                    return true;
                }

                CustomBoss boss = plugin.getBossManager().spawnBoss(model, player.getLocation());

                // FIXED: Register abilities BEFORE starting ability loop
                if (args.length > 2) {
                    for (int i = 2; i < args.length; i++) {
                        registerAbility(boss, args[i]);
                    }
                    player.sendMessage("§aSpawned boss with " + (args.length - 2) + " abilities");
                } else {
                    // Register default abilities
                    registerDefaultAbilities(boss);
                    player.sendMessage("§aSpawned boss with default abilities");
                }

                // CRITICAL: Start ability system AFTER registration
                boss.startAbilities();

                player.sendMessage("§aSpawned boss: " + args[1] + " at " + boss.getUUID());
            }

            case "ability" -> {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /cb ability <bossUUID> <abilityId>");
                    return true;
                }

                try {
                    UUID bossId = UUID.fromString(args[1]);
                    CustomBoss boss = plugin.getBossManager().getBoss(bossId);

                    if (boss == null) {
                        player.sendMessage("§cBoss not found");
                        return true;
                    }

                    String abilityId = args[2];
                    if (boss.useAbility(abilityId)) {
                        player.sendMessage("§aTriggered ability: " + abilityId);
                    } else {
                        player.sendMessage("§cAbility not ready or not found: " + abilityId);
                    }
                } catch (IllegalArgumentException e) {
                    player.sendMessage("§cInvalid boss UUID");
                }
            }

            case "animate" -> {
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /cb animate <bossUUID> <state>");
                    player.sendMessage("§7States: IDLE, WALK, ATTACK, HURT, DEATH, ABILITY_1, ABILITY_2, ABILITY_3");
                    return true;
                }

                try {
                    UUID bossId = UUID.fromString(args[1]);
                    CustomBoss boss = plugin.getBossManager().getBoss(bossId);

                    if (boss == null) {
                        player.sendMessage("§cBoss not found");
                        return true;
                    }

                    AnimationStateMachine.AnimationState state =
                            AnimationStateMachine.AnimationState.valueOf(args[2].toUpperCase());

                    boss.playAnimation(state);
                    player.sendMessage("§aPlaying animation: " + state);
                } catch (IllegalArgumentException e) {
                    player.sendMessage("§cInvalid state: " + args[2]);
                }
            }

            case "info" -> {
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /cb info <bossUUID>");
                    return true;
                }

                try {
                    UUID bossId = UUID.fromString(args[1]);
                    CustomBoss boss = plugin.getBossManager().getBoss(bossId);

                    if (boss == null) {
                        player.sendMessage("§cBoss not found");
                        return true;
                    }

                    displayBossInfo(player, boss);
                } catch (IllegalArgumentException e) {
                    player.sendMessage("§cInvalid boss UUID");
                }
            }

            case "list" -> {
                player.sendMessage("§e=== Available Models ===");
                plugin.getModelStorage().getAllModels().forEach(m ->
                        player.sendMessage("§7- §f" + m.getId() + " §8(" + m.getParts().size() + " parts)")
                );

                player.sendMessage("");
                player.sendMessage("§e=== Active Bosses ===");
                plugin.getBossManager().getAllBosses().forEach(b -> {
                    player.sendMessage("§7- §f" + b.getModelData().getId() +
                            " §8[" + b.getUUID().toString().substring(0, 8) + "...]");
                });
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

    /**
     * Register an ability by name
     */
    private void registerAbility(CustomBoss boss, String abilityName) {
        switch (abilityName.toLowerCase()) {
            case "slam" -> boss.registerAbility(new SlamAttack());
            case "projectile" -> boss.registerAbility(new ProjectileAttack());
            case "leap" -> boss.registerAbility(new LeapAttack());
            case "rage" -> boss.registerAbility(new RageMode());
            default -> {}
        }
    }

    /**
     * Register default ability set
     */
    private void registerDefaultAbilities(CustomBoss boss) {
        boss.registerAbility(new SlamAttack());
        boss.registerAbility(new ProjectileAttack());
        boss.registerAbility(new LeapAttack());
    }

    /**
     * Display boss information
     */
    private void displayBossInfo(Player player, CustomBoss boss) {
        player.sendMessage("§e=== Boss Info ===");
        player.sendMessage("§7UUID: §f" + boss.getUUID());
        player.sendMessage("§7Model: §f" + boss.getModelData().getId());
        player.sendMessage("§7Health: §f" + boss.getCoreEntity().getHealth() +
                "/" + boss.getCoreEntity().getMaxHealth());
        player.sendMessage("§7Ticks Lived: §f" + boss.getTicksLived());

        if (boss.getStateMachine() != null) {
            player.sendMessage("§7Animation: §f" + boss.getStateMachine().getCurrentState());
            player.sendMessage("§7Transitioning: §f" + boss.getStateMachine().isTransitioning());
        }

        if (boss.getAbilityManager() != null) {
            player.sendMessage("§7Abilities: §f" + boss.getAbilityManager().getAbilities().size());
            boss.getAbilityManager().getAbilities().forEach(ability -> {
                int cooldown = ability.getRemainingCooldown(boss);
                String status = cooldown > 0 ?
                        "§c[" + (cooldown / 20) + "s]" : "§a[READY]";
                player.sendMessage("  §7- §f" + ability.getName() + " " + status);
            });
        }
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
                current = !session.getDebugVisualizer().toString().contains("hitbox");
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
                player.sendMessage("§7Axes: §aToggled");
            }
            case "grid" -> {
                player.sendMessage("§7Grid: §aToggled");
            }
            case "labels" -> {
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
        player.sendMessage("§e=== Custom Bosses §7[PHASE 1] ===");
        player.sendMessage("§7/cb editor §f- Open editor");
        player.sendMessage("§7/cb spawn <model> [abilities] §f- Spawn boss");
        player.sendMessage("§7  §8Abilities: slam, projectile, leap, rage");
        player.sendMessage("§7/cb ability <uuid> <id> §f- Trigger ability");
        player.sendMessage("§7/cb animate <uuid> <state> §f- Play animation");
        player.sendMessage("§7/cb info <uuid> §f- Show boss info");
        player.sendMessage("§7/cb list §f- List models & bosses");
        player.sendMessage("§7/cb clear §f- Remove all bosses");
        player.sendMessage("§7/cb reload §f- Reload models");
        player.sendMessage("§7/cb undo/redo §f- Undo/redo edit");
        player.sendMessage("§7/cb debug [option] §f- Debug visualizations");
        player.sendMessage("§7/cb gizmo [option] §f- Gizmo controls");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("editor", "spawn", "ability", "animate", "info",
                            "list", "clear", "reload", "undo", "redo", "debug", "gizmo")
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

            if (args[0].equalsIgnoreCase("ability") || args[0].equalsIgnoreCase("animate") ||
                    args[0].equalsIgnoreCase("info")) {
                return plugin.getBossManager().getAllBosses().stream()
                        .map(b -> b.getUUID().toString())
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

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("spawn")) {
                return Arrays.asList("slam", "projectile", "leap", "rage")
                        .stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("ability")) {
                return Arrays.asList("slam", "projectile", "leap", "rage")
                        .stream()
                        .filter(s -> s.startsWith(args[2].toLowerCase()))
                        .collect(Collectors.toList());
            }

            if (args[0].equalsIgnoreCase("animate")) {
                return Arrays.asList("IDLE", "WALK", "ATTACK", "HURT", "DEATH",
                                "ABILITY_1", "ABILITY_2", "ABILITY_3")
                        .stream()
                        .filter(s -> s.startsWith(args[2].toUpperCase()))
                        .collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }
}