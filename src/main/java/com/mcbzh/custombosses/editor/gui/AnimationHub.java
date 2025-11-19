package com.mcbzh.custombosses.editor.gui;

import com.mcbzh.custombosses.editor.EditorSession;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AnimationHub implements Listener {

    private final EditorSession session;
    private final Inventory inventory;

    public AnimationHub(EditorSession session) {
        this.session = session;
        this.inventory = Bukkit.createInventory(null, 27, "Animation Editor");
        setupItems();
        Bukkit.getPluginManager().registerEvents(this, com.mcbzh.custombosses.CustomBossesPlugin.getInstance());
    }

    private void setupItems() {
        inventory.setItem(11,
                createItem(Material.FILLED_MAP, "§aCreate New Animation", "§7Click to create a new animation"));
        inventory.setItem(13, createItem(Material.BOOK, "§eEdit Animation", "§7Click to edit an existing animation"));
        inventory.setItem(15, createItem(Material.BARRIER, "§cBack", "§7Return to Main Hub"));
    }

    private ItemStack createItem(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(java.util.Collections.singletonList(lore));
        item.setItemMeta(meta);
        return item;
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().equals(inventory)) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null)
                return;

            Player player = (Player) event.getWhoClicked();
            String name = event.getCurrentItem().getItemMeta().getDisplayName();

            if (name.contains("Create New Animation")) {
                player.closeInventory();
                player.sendMessage("§eEnter animation ID in chat:");
                session.setWaitingForChatInput(true, (input) -> {
                    player.sendMessage("§aCreated animation: " + input);
                    // TODO: Create animation logic
                });
            } else if (name.contains("Edit Animation")) {
                player.sendMessage("§cNot implemented yet.");
            } else if (name.contains("Back")) {
                session.openHub();
            }
        }
    }
}
