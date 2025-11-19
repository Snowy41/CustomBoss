package com.mcbzh.custombosses.editor.tools;

import com.mcbzh.custombosses.animation.AnimationData;
import com.mcbzh.custombosses.animation.Keyframe;
import com.mcbzh.custombosses.editor.EditorSession;
import com.mcbzh.custombosses.model.ModelPart;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KeyframeTool extends EditorTool {

    public KeyframeTool(EditorSession session) {
        super(session);
    }

    @Override
    public ItemStack getIcon() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Keyframe Tool");
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onUse(Player player, Action action) {
        Entity selected = session.getSelectedPart();
        if (selected == null) {
            player.sendMessage("§cSelect a part first!");
            return;
        }

        ModelPart part = getPart(selected);
        if (part == null)
            return;

        // Check if we have an active animation
        String animId = "test_anim";
        AnimationData anim = session.getActiveAnimation();
        if (anim == null) {
            anim = new AnimationData(animId, 5.0f, true);
            session.setActiveAnimation(anim);
            player.sendMessage("§eCreated temporary animation 'test_anim'");
        }

        // Add keyframe at current time (placeholder time)
        float time = 0.0f; // TODO: Get from timeline

        Keyframe kf = new Keyframe(time,
                part.getData().getOffset().clone(),
                part.getData().getRotation().clone(),
                part.getData().getScale().clone());

        anim.addKeyframe(part.getData().getId(), kf);
        player.sendMessage("§aAdded keyframe for " + part.getData().getId() + " at " + time + "s");
    }

    private ModelPart getPart(Entity entity) {
        if (session.getActiveInstance() != null) {
            for (ModelPart p : session.getActiveInstance().getParts().values()) {
                if (p.getEntity().equals(entity)) {
                    return p;
                }
            }
        }
        return null;
    }

    @Override
    public void onTick() {

    }
}
