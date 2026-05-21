# Build Integration Notes

`Game.java` da noi truc tiep voi `buildsystem.core.BuildManager` va `BuildController`.

## Runtime flow trong game chinh
- Hotbar selection -> `BuildController.onToolbarSlotSelected(...)`.
- Q rotate -> `BuildController.onRotatePressed()`.
- Mouse move moi frame -> `BuildController.onCursorMoved(...)`.
- Click place -> `BuildController.onPrimaryClickPlace(...)`.
- Consume item duoc xu ly ben trong `BuildManager`, khong con do `Game` tru thu cong.
- Collision runtime cua player/enemy voi build object dung `build.CollisionManager.intersectsPlacedBuildObject(...)`.

## Save/load
- `Game.saveWorldSnapshot()` luu them `buildObjects`.
- `Game.applyLoadedSaveIfAny()` goi `buildManager.restoreFromSaveData(...)`.

## UI
- `UIManager` va `HotbarOverlay` doc `BuildToolbar` tu `BuildManager`.
- Them build item moi qua registry se tu di vao toolbar model, khong can hardcode them `stone_wall` o UI.
