# Tribe Survival Game

JavaFX survival game voi world 2D, combat, resource gathering, shop, inventory va build system mo rong duoc.

## Core modules
- `src/core`
  - Game loop, world state, input flow, save/load va integration giua cac system.
- `src/ui`
  - Canvas renderer + JavaFX overlay UI.
- `src/system`
  - Damage, collision, resource, save/load.
- `src/buildsystem`
  - Build system dung chung cho game chinh va demo.

## BuildSystem Architecture

Build system da duoc refactor theo huong module dung chung, khong con viet rieng `placeWall()`, `placeTrap()`, `placeTorch()`...

### 1. `BuildManager`
- Trung tam cua build flow.
- Chiu trach nhiem:
  - chon build item
  - update ghost preview
  - Q rotate
  - validate placement
  - consume inventory
  - tao `BuildObject`
  - add object vao world
  - refresh sprite cua object va neighbors
  - export/import save data

### 2. `BuildObject`
- Base class chung cho moi object dat xuong world.
- Moi subclass nhu `Wall`, `Trap`, `Torch`, `Chest`, `Campfire`, `Turret` deu di qua cung flow cua `BuildManager`.
- Save/load ready fields:
  - `id`
  - `type`
  - `tileX`
  - `tileY`
  - `rotation`
  - `health`
  - `spriteKey`

### 3. `PlacementValidator`
- Cua vao duy nhat cho placement validation.
- Ho tro san cho:
  - grid placement
  - free placement
  - collision check
  - terrain check
  - water restriction
  - flat ground requirement
  - blocking entity
  - near object rule

### 4. `AutoTileResolver`
- Auto connect theo `autoTileGroup`.
- Muc tieu la dung chung cho:
  - wall
  - fence
  - pipe
  - cable
  - road

### 5. `BuildRegistry`
- Dang ky tat ca `BuildDefinition`.
- Them build object moi theo dung huong:
  1. tao subclass neu can
  2. register `BuildDefinition`
  3. UI/preview/placement/save-load tu dong dung lai metadata do

### 6. `BuildFactory`
- Khong `new` truc tiep trong gameplay code.
- `BuildFactory` tao object that dua tren `BuildDefinition.ObjectBuilder`.

## Build flow hoan chinh
1. UI chon slot build tren toolbar.
2. `BuildController` goi `BuildManager.selectToolbarSlot(...)`.
3. `BuildManager` tim `BuildDefinition` trong `BuildRegistry`.
4. Mouse move -> `BuildManager.updatePreview(...)`.
5. `PlacementStrategy` snap cursor theo grid hoac free placement.
6. `PlacementValidator` validate object.
7. `BuildSpriteResolver` + `AutoTileResolver` chon sprite/rotation/neighbor mask.
8. `GhostPreviewRenderer` cap nhat `BuildPreview`.
9. Click place -> `BuildManager.tryPlaceSelected(...)`.
10. `BuildManager` consume inventory, tao `BuildObject`, add vao world va refresh neighbors.

## Preview system
- Preview dung chung qua `BuildPreview`.
- Ghost preview:
  - giong object that
  - opacity thap
  - do neu invalid
  - xoay theo rotation
  - snap theo placement strategy

## Rotation system
- Q rotate di qua `RotationManager`.
- Moi object co `RotationComponent` deu co the tai su dung flow nay.

## UI build system
- `BuildToolbar` la model toolbar dung chung cho:
  - icon
  - count
  - cost
  - selected state
- `HotbarOverlay` va `UIManager` doc `BuildToolbar` tu `BuildManager`, khong hardcode rieng `stone_wall`.

## World integration
- Game chinh dung truc tiep:
  - `BuildManager`
  - `BuildController`
  - `BuildToolbar`
  - `BuildPreview`
- Demo `BuildDemoMain` cung dung chinh module do, khong copy logic build sang mot code path khac.

## Save/load
- `Game.saveWorldSnapshot()` luu them `buildObjects`.
- `BuildManager.exportSaveData()` tra ve list snapshot.
- `BuildManager.restoreFromSaveData(...)` rebuild lai object tu save data + registry.

## Them build object moi
1. Them `BuildType` neu can.
2. Tao subclass trong `src/buildsystem/object` neu object can behavior/components rieng.
3. Register `BuildDefinition` moi trong `BuildRegistry`.
4. Khai bao:
  - `itemId`
  - `displayName`
  - `placementStrategy`
  - `rotatable`
  - `collisionEnabled`
  - `waterRestricted`
  - `requiresFlatTerrain`
  - `health`
  - `buildCost`
  - `autoTileGroup`
  - `ObjectBuilder`
5. Neu can mo rong tinh nang, them component trong `src/buildsystem/component`.

## Demo
- Entry demo: `src/buildsystem/demo/BuildDemoMain.java`
- Demo dung chung:
  - `BuildManager`
  - `BuildController`
  - `BuildToolbar`
  - `PlacementValidator`
  - `AutoTileResolver`

## Ghi chu verify
- Trong workspace hien tai khong co JavaFX classpath/module-path san cho `javac`, nen khong the compile verify toan repo bang lenh thuong.
- Refactor da duoc doi chieu bang usage graph, integration points va save/load flow trong codebase.
