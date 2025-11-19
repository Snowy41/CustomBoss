package com.mcbzh.custombosses.editor.gui;

import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.editor.EditorSession;
import com.mcbzh.custombosses.model.ModelData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ModelSelectorHub implements Listener {

    private final EditorSession session;
    private final Inventory inventory;

    public ModelSelectorHub(EditorSession session) {
        this.session = session;
        this.inventory = Bukkit.createInventory(null, 54, "Select Model to Edit");

        setupItems();

        Bukkit.getPluginManager().registerEvents(this, CustomBossesPlugin.getInstance());
    }

    private void setupItems() {
        int slot = 0;
        for (ModelData model : CustomBossesPlugin.getInstance().getConfigManager().getAllModels()) {
            inventory.setItem(slot++, createItem(Material.ARMOR_STAND, "§e" + model.getId()));
        }

        // Add "Create New" option at the end or specific slot
        inventory.setItem(53, createItem(Material.EMERALD_BLOCK, "§aCreate New via Chat"));
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

        if (event.getSlot() == 53) {
            // Create New via Chat
            player.closeInventory();
            player.sendMessage("§eEnter new model ID in chat:");
            session.setWaitingForChatInput(true, (input) -> {
                session.enterModelEdit(input);
            });
        } else {
            // Select existing model
            String modelId = itemName.replace("§e", "");
            player.closeInventory();
            session.enterModelEdit(modelId);
        }
    }
}
