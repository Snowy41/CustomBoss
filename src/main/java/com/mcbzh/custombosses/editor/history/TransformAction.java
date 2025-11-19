package com.mcbzh.custombosses.editor.history;

import com.mcbzh.custombosses.model.ModelPartData;
import org.bukkit.util.Vector;

public class TransformAction implements EditorAction {
    private final ModelPartData part;
    private final Vector oldOffset;
    private final Vector oldRotation;
    private final Vector oldScale;
    private final Vector newOffset;
    private final Vector newRotation;
    private final Vector newScale;

    public TransformAction(ModelPartData part, Vector oldOffset, Vector oldRotation, Vector oldScale,
            Vector newOffset, Vector newRotation, Vector newScale) {
        this.part = part;
        this.oldOffset = oldOffset.clone();
        this.oldRotation = oldRotation.clone();
        this.oldScale = oldScale.clone();
        this.newOffset = newOffset.clone();
        this.newRotation = newRotation.clone();
        this.newScale = newScale.clone();
    }

    @Override
    public void undo() {
        part.setOffset(oldOffset.clone());
        part.setRotation(oldRotation.clone());
        part.setScale(oldScale.clone());
    }

    @Override
    public void redo() {
        part.setOffset(newOffset.clone());
        part.setRotation(newRotation.clone());
        part.setScale(newScale.clone());
    }
}
