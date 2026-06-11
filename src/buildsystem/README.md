# BuildSystem Module

BuildSystem la module dung chung cho game chinh va demo. Module nay khong phai mini-game rieng cho wall.

## Architecture
- `buildsystem/core/BuildManager`
  - Dieu phoi toan bo build flow: select -> preview -> rotate -> validate -> consume -> place -> refresh neighbors -> save/load.
- `buildsystem/core/BuildController`
  - Facade cho input layer goi BuildManager ma khong can biet noi bo.
- `buildsystem/core/BuildRegistry`
  - Dang ky tat ca `BuildDefinition`.
- `buildsystem/core/BuildFactory`
  - Tao `BuildObject` qua `BuildDefinition.ObjectBuilder`, tranh `new` truc tiep o gameplay code.
- `buildsystem/object/BuildObject`
  - Base class chung cho wall, trap, torch, chest, campfire, turret...
- `buildsystem/core/PlacementValidator`
  - Validate placement chung cho grid, free placement, collision, terrain, water, near object, blocking entity.
- `buildsystem/sprite/AutoTileResolver`
  - Auto connect theo `autoTileGroup`, khong chi rieng wall.
- `buildsystem/ui/BuildToolbar`
  - Model toolbar build dung chung cho icon/count/cost/selected.

## Build flow
1. UI hotbar/tool bar chon slot -> `BuildController.onToolbarSlotSelected(...)`.
2. `BuildManager` tim `BuildDefinition` trong `BuildRegistry`.
3. Mouse move -> `BuildController.onCursorMoved(...)`.
4. `PlacementStrategy` snap cursor vao tile/world phu hop.
5. `PlacementValidator` kiem tra:
   - bounds
   - terrain
   - water restriction
   - flat ground
   - blocking world object
   - player blocking
   - placed object collision
   - near object restriction
6. `BuildSpriteResolver` + `AutoTileResolver` chon sprite/rotation/neighbor mask.
7. `GhostPreviewRenderer` ghi material vao `BuildPreview`.
8. Click place -> `BuildManager.tryPlaceSelected(...)`.
9. `BuildManager` consume inventory, tao `BuildObject`, add vao world, refresh sprite cua object va neighbors.

## Preview flow
- Mọi object build deu dung chung `BuildPreview`.
- Preview luon giu:
  - type
  - tile
  - spriteKey
  - rotation
  - opacity
  - validation message
- Renderer/demo chi viec doc `BuildPreview` de ve ghost.

## Save/load ready
- Moi `BuildObject` deu co:
  - `id`
  - `type`
  - `tileX`
  - `tileY`
  - `rotation`
  - `health`
  - `spriteKey`
- `BuildManager.exportSaveData()` tra ve list snapshot de `WorldSaveService` serialize.
- `BuildManager.restoreFromSaveData(...)` rebuild lai object tu snapshot + registry.

## Them build object moi
1. Tao subclass trong `buildsystem/object` neu can.
2. Register `BuildDefinition` moi trong `BuildRegistry`.
3. Khai bao:
   - `itemId`
   - `displayName`
   - `placementStrategy`
   - `rotatable`
   - `collisionEnabled`
   - `waterRestricted`
   - `requiresFlatTerrain`
   - `health`
   - `buildCost`
   - `autoTileGroup` neu can auto connect
   - `ObjectBuilder`
4. Neu object can tinh nang moi, them component trong `buildsystem/component`.
5. UI va demo khong can viet them `placeXxx()` rieng.
