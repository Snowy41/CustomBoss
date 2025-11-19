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

public class EditorHub implements Listener {

    private final EditorSession session;
    private final Inventory inventory;

    public EditorHub(EditorSession session) {
        this.session = session;
        this.inventory = Bukkit.createInventory(null, 27, "Custom Bosses Editor");

        setupItems();

        // Register self as listener (Need to unregister later or handle globally)
        // Ideally, EditorManager handles all clicks, but for simplicity here:
        Bukkit.getPluginManager().registerEvents(this, com.mcbzh.custombosses.CustomBossesPlugin.getInstance());
    }

    private void setupItems() {
        inventory.setItem(11, createItem(Material.EMERALD_BLOCK, "§aCreate New Model"));
        inventory.setItem(13, createItem(Material.BOOKSHELF, "§eEdit Existing Model"));
        inventory.setItem(15, createItem(Material.CLOCK, "§bAnimations"));
    }

    private ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public void open() {
        session.getPlayer().openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory() != inventory)
            return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta() || clickedItem.getItemMeta().getDisplayName() == null) {
            return;
        }

        String itemName = clickedItem.getItemMeta().getDisplayName();

        if (event.getSlot() == 11) {
            // Create New
            player.closeInventory();
            player.sendMessage("§aCreating new model 'NewModel'...");
            session.enterModelEdit("NewModel");
        } else if (event.getSlot() == 13) {
            // Edit Existing Model - Open Selector
            new ModelSelectorHub(session).open();
        } else if (event.getSlot() == 15) {
            // Animations
            session.openAnimationHub();
        }
    }
}
