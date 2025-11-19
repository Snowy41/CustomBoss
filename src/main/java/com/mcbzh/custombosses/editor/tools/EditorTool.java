package com.mcbzh.custombosses.editor.tools;

import com.mcbzh.custombosses.editor.EditorSession;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

public abstract class EditorTool {
    
    protected final EditorSession session;

    public EditorTool(EditorSession session) {
        this.session = session;
    }

    public abstract ItemStack getIcon();
    
    public abstract void onUse(Player player, Action action);
    
    public void onTick() {
        // Optional: Render particles/gizmos
    }
}
