package com.mcbzh.custombosses.editor.history;

import java.util.Stack;

public class UndoStack {
    private final Stack<EditorAction> undoStack = new Stack<>();
    private final Stack<EditorAction> redoStack = new Stack<>();
    private static final int MAX_HISTORY = 50;

    public void push(EditorAction action) {
        undoStack.push(action);
        if (undoStack.size() > MAX_HISTORY) {
            undoStack.remove(0);
        }
        redoStack.clear();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            EditorAction action = undoStack.pop();
            action.undo();
            redoStack.push(action);
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            EditorAction action = redoStack.pop();
            action.redo();
            undoStack.push(action);
        }
    }
}
