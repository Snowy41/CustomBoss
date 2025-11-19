# Custom BlockDisplay Boss Plugin Plan

## Goal
Create a Spigot 1.21.10 plugin that adds custom bosses using `BlockDisplay` entities for models and animations. The plugin will include **In-Game Editor Tools** (Model Designer & Animation Creator) to allow admins to create custom Models and Animations (for idle, walking, hurt, abilities, etc.) in-game. **Note**: Actual ability logic (what the boss does) will require Java coding, but can trigger these custom animations. Bosses will be interactive with variable hitboxes.

## User Review Required
> [!IMPORTANT]
> **Hitbox & AI Implementation**:
> - **Movement/AI**: We will still use an invisible **Zombie** (or Silverfish for small bosses) as the "Brain" to handle pathfinding and gravity.
> - **Loot System**: Bosses will have a `LootTable` defined in their JSON.
>   - **Vanilla**: Standard materials and amounts.
>   - **CustomItems**: We will create an interface `ItemProvider` to allow hooking into your `CustomItems` plugin later. For now, it will just support vanilla.

> **In-Game Editor Complexity**: Creating an in-game editor is complex.
> - **Editor Hub**: A central menu (GUI) to navigate between creating/editing models and animations.
> - **Toolbar UI**: Players will use items in their hotbar to interact with the editor.
> - **Model Creation (3D Mode)**: Instead of converting blocks, players will place "Primitive Cuboids" (BlockDisplays) and use tools to **Scale**, **Rotate**, and **Move** them to create complex shapes.
> - **Bone System**: Parts can be "parented" to other parts. Moving/Rotating a parent will move/rotate all its children. This is essential for jointed animations (arms, legs).
> - **Editing Existing**: Users can load an existing model and tweak the parts.
> - **Animation Creation**: Players will enter "Edit Mode" on a model, select individual parts (BlockDisplays), move/rotate them, and save the state as a "Keyframe".
> - **Storage**: Models and Animations will be saved as JSON files.
> - **Auto-Reload**: Models and Animations will automatically reload from disk when saved or before being listed in the Editor Hub. No manual `/cb reload` needed.
> - **QoL Features**:
>   - **Undo/Redo**: The editor will track the last 10 actions for easy mistake correction.
>   - **Highlighting**: Selected parts will glow to indicate selection.
>   - **Visual Guides**: We will spawn **BlockDisplay Gizmos** (thin, stretched blocks of Red/Green/Blue concrete) to mimic Blender's axis tools. These are solid and much clearer than particles.
>   - **Mirroring**: A tool to duplicate and flip parts across the center axis. Great for creating left/right arms quickly.
>   - **Precision Editing**: Sometimes dragging is hard. Shift-clicking with a tool will let you type exact numbers (e.g., "45.0" degrees) in chat.
>   - **Boss Bars & Sounds**: Full configuration for the boss's health bar and sound effects (Hurt, Death, Ambient) in the editor.
>   - **Animation Preview**: A "Play/Stop" button in the hotbar to loop the animation while you work on it.
>   - **Safe Shutdown**: All bosses will be despawned on server stop to prevent "ghost" entities.

## Proposed Changes

### Project Structure
- Maven project with `spigot-api` 1.21.1-R0.1-SNAPSHOT.
- Package: `com.mcbzh.custombosses`

### Core Components

#### [NEW] [CustomBossesPlugin.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/CustomBossesPlugin.java)
- Main entry point.
- Registers listeners (`BossListener`, `EditorListener`) and commands (`EditorCommand`).
- Initializes `BossManager` and `EditorManager`.

#### [NEW] [BossManager.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/BossManager.java)
- **Map<UUID, CustomBoss> activeBosses**: Tracks all alive bosses.
- **Map<String, ModelData> modelRegistry**: Cache of loaded model templates.
- **Map<String, AnimationData> animationRegistry**: Cache of loaded animations.
- `spawnBoss(String modelId, Location loc)`: Creates a new boss instance.
- `tick()`: Called every tick to update all active bosses.
- **Auto-Reload**: `saveModel` updates the cache immediately. `getAvailableModels` checks disk for new files.

#### [NEW] [CustomBoss.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/boss/CustomBoss.java)
- **LivingEntity core**: The invisible Zombie/Silverfish.
- **Interaction interaction**: The hitbox entity.
- **ModelInstance model**: The visual representation.
- `tick()`: Syncs `interaction` to `core`, updates `model` animations.

#### [NEW] [ModelData.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/model/ModelData.java)
- **List<ModelPartData> parts**: The blueprint for the model.
- **Vector hitboxSize**: Width/Height for the Interaction entity.
- **BossBarSettings bossBar**: Color, Style, Title.
- **SoundSettings sounds**: Map of Event -> Sound (Pitch/Volume).

#### [NEW] [ModelPartData.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/model/ModelPartData.java)
- **String id**: Unique name for this part (e.g., "arm_left").
- **String parentId**: ID of the parent part (null if root).
- **Material material**: The block type.
- **Vector offset**: Position relative to PARENT (or center if root).
- **Vector rotation**: Rotation relative to PARENT.
- **ItemDisplayTransform transform**: Fixed display scale/transform.

#### [NEW] [ModelSerializer.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/storage/ModelSerializer.java)
- Uses GSON or Jackson.
- `saveModel(ModelData)`: Writes to `plugins/CustomBosses/models/{name}.json`.
- `loadModel(String name)`: Reads from file.

### Editor System (Toolbar UI)

#### [NEW] [EditorManager.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/editor/EditorManager.java)
- **Map<UUID, EditorSession> sessions**: Tracks players in editor mode.
- `onInteract(PlayerInteractEvent)`: Routes clicks to the player's active tool.

#### [NEW] [GizmoManager.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/editor/GizmoManager.java)
- Manages the 3 axis `BlockDisplay` entities for a player.
- `showGizmo(Location center, Rotation rot)`: Spawns/Teleports the RGB lines.
- `hideGizmo()`: Removes them.

#### [NEW] [EditorSession.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/editor/EditorSession.java)
- **EditorState state**: `HUB`, `MODEL_SELECT`, `MODEL_EDIT`, `ANIM_SELECT`, `ANIM_EDIT`.
- **Map<Integer, EditorTool> hotbar**: Maps slot index to a tool.
- **Stack<EditorAction> undoStack**: History of actions.
- `openHub()`: Opens the main GUI.
- `enterModelEdit(String modelId)`: Sets state and gives tools.
- `undo()`: Reverts last action.

#### [NEW] [EditorAction.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/editor/EditorAction.java)
- Interface for reversible actions (`MovePartAction`, `ScalePartAction`, `CreatePartAction`).

#### [NEW] [EditorHub.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/editor/gui/EditorHub.java)
- Inventory GUI with buttons:
  - "Create New Model"
  - "Edit Model" -> Opens list of models
  - "Create/Edit Animation" -> Opens list of models -> list of animations

#### [NEW] [EditorTool.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/editor/tools/EditorTool.java)
- Abstract class.
- `ItemStack getIcon()`: The item to show in hotbar.
- `onUse(Player, Action)`: Called when clicked.
- `onTick()`: Optional, for rendering particles (Visual Guides).

#### [NEW] [ParentTool.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/editor/tools/ParentTool.java)
- **Icon**: Lead.
- **Logic**:
  - Click Child -> Click Parent -> Links them.
  - Updates `ModelPartData` and re-calculates relative transforms.

#### [NEW] [SpawnTool.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/editor/tools/SpawnTool.java)
- **Icon**: Emerald.
- **Logic**: Spawns a default white concrete `BlockDisplay` at the target location.

#### [NEW] [MirrorTool.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/editor/tools/MirrorTool.java)
- **Icon**: Glass Pane.
- **Logic**: Duplicates the selected part and flips its X or Z coordinate relative to the model center.

#### [NEW] [TransformTool.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/editor/tools/TransformTool.java)
- **Icon**: Stick (Move) / Blaze Rod (Rotate) / Slime Ball (Scale).
- **Logic**: Raycasts to find the `BlockDisplay` looked at.
- **Modes**:
  - **Move**: Drag part relative to player view or axes.
  - **Rotate**: Scroll to rotate part.
  - **Scale**: Scroll to stretch/shrink part on selected axis (X/Y/Z).
- **Precision Input**: Shift-Right-Click opens a SignGUI or Chat Prompt to enter exact Vector/Rotation values.

#### [NEW] [MaterialTool.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/editor/tools/MaterialTool.java)
- **Icon**: Magma Cream.
- **Logic**: Applies the material from the player's offhand to the selected part.

### Loot System

#### [NEW] [LootManager.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/loot/LootManager.java)
- Handles dropping items when a boss dies.
- Parses `LootTable` from JSON.

#### [NEW] [LootTable.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/loot/LootTable.java)
- List of `LootEntry` (ItemStack + Chance).

#### [NEW] [ItemProvider.java](file:///c:/Users/tobia/test/src/main/java/com/mcbzh/custombosses/loot/ItemProvider.java)
- Interface to get an ItemStack from a string ID.
- Default implementation: `VanillaItemProvider`.
- Future implementation: `CustomItemProvider` (hooks into your other plugin).

## Verification Plan

### Automated Tests
- *Note: Spigot plugins are hard to unit test without mocking the entire server. We will rely on manual verification.*

### Manual Verification
1.  **Build & Deploy**: Run `mvn clean package`, put jar in `plugins/`, start server.
2.  **Editor Test**:
    - Run `/cb editor`. Verify Hub GUI opens.
    - Create New Model -> "TestModel".
    - Use **Spawn Tool** to create a cube.
    - Use **Transform Tool (Scale)** to stretch it into a limb.
    - Use **Parent Tool** to link two parts.
    - Save.
3.  **Animation Test**:
    - Open Hub -> Create Animation -> "TestModel".
    - Move a part. Save Frame 0.
    - Move it again. Save Frame 20.
    - Playback.
4.  **Combat Test**:
    - Spawn boss `/cb spawn TestModel`.
    - Toggle debug `/cb debug`. Verify hitbox size.
    - Hit boss. Verify damage.
