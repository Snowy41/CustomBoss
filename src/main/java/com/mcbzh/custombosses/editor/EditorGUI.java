package com.mcbzh.custombosses.editor;

import com.mcbzh.custombosses.CustomBossesPlugin;
import com.mcbzh.custombosses.model.ModelData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class EditorGUI {

    public static void openMainMenu(Player player, EditorSession session) {
        Inventory inv = Bukkit.createInventory(null, 27, "Custom Bosses Editor");

        inv.setItem(11, createItem(Material.EMERALD_BLOCK, "§aCreate New Model"));
        inv.setItem(13, createItem(Material.BOOKSHELF, "§eEdit Existing Model"));
        inv.setItem(15, createItem(Material.ENDER_EYE, "§bClose"));

        player.openInventory(inv);
    }

    public static void openModelList(Player player, EditorSession session) {
        CustomBossesPlugin plugin = CustomBossesPlugin.getInstance();
        Inventory inv = Bukkit.createInventory(null, 54, "Select Model");

        int slot = 0;
        for (ModelData model : plugin.getModelStorage().getAllModels()) {
            ItemStack item = createItem(Material.ARMOR_STAND, "§e" + model.getId());
            ItemMeta meta = item.getItemMeta();
            meta.setLore(java.util.Arrays.asList(
                    "§7Parts: " + model.getParts().size(),
                    "§7Click to edit"
            ));
            item.setItemMeta(meta);
            inv.setItem(slot++, item);

            if (slot >= 45) break;
        }

        inv.setItem(53, createItem(Material.EMERALD, "§aCreate New"));

        player.openInventory(inv);
    }

    private static ItemStack createItem(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}