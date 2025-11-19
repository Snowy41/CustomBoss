package com.mcbzh.custombosses.listeners;

import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.editor.EditorSession;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class EditorListener implements Listener {

    private final CustomBossesPlugin plugin;

    public EditorListener(CustomBossesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getEditorManager().removeSession(event.getPlayer());
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        EditorSession session = plugin.getEditorManager().getSession(player);

        if (!session.isActive()) return;
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        String lore = item.getItemMeta().hasLore() ?
                item.getItemMeta().getLore().get(0) : "";

        if (lore.startsWith("§8tool:")) {
            event.setCancelled(true);
            String tool = lore.substring(7);
            boolean rightClick = event.getAction() == Action.RIGHT_CLICK_AIR ||
                    event.getAction() == Action.RIGHT_CLICK_BLOCK;
            session.handleToolUse(tool, rightClick);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        EditorSession session = plugin.getEditorManager().getSession(player);

        if (session.handleChatInput(event.getMessage())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!title.contains("Custom Bosses") && !title.contains("Select Model")) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        EditorSession session = plugin.getEditorManager().getSession(player);
        String name = clicked.getItemMeta().getDisplayName();

        if (title.equals("Custom Bosses Editor")) {
            if (name.contains("Create New")) {
                player.closeInventory();
                player.sendMessage("§eEnter model name in chat:");
                session.requestChatInput(modelName -> {
                    session.editModel(modelName);
                });
            } else if (name.contains("Edit Existing")) {
                com.mcbzh.custombosses.editor.EditorGUI.openModelList(player, session);
            } else if (name.contains("Close")) {
                player.closeInventory();
            }
        } else if (title.equals("Select Model")) {
            if (name.contains("Create New")) {
                player.closeInventory();
                player.sendMessage("§eEnter model name in chat:");
                session.requestChatInput(modelName -> {
                    session.editModel(modelName);
                });
            } else {
                String modelId = name.replace("§e", "");
                player.closeInventory();
                session.editModel(modelId);
            }
        }
    }
}
