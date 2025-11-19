package com.mcbzh.custombosses.editor.tools;

import com.mcbzh.custombosses.editor.EditorSession;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ExitTool extends EditorTool {

    public ExitTool(EditorSession session) {
        super(session);
    }

    @Override
    public ItemStack getIcon() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cExit Editor");
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onUse(Player player, Action action) {
        player.sendMessage("§cExiting editor...");
        session.exitEditor();
        session.openHub();
    }
}
