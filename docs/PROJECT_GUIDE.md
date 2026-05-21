# Project Guide - Tribe Survival

## 1) Project nay dang lam gi
Project la game 2D survival viet bang JavaFX.
- Game chinh: di chuyen, chien dau, thu thap tai nguyen, build system, shop/inventory, minimap, level objective, save/load.
- Build demo: smoke test cho module build dung chung, khong con mini-game `WallBuilder` tach rieng.

## 2) Cau truc thu muc sau khi chuan hoa
- `src/`
  - Chua toan bo Java source code theo package.
  - `main/` chi con launcher `Main.java` cho game chinh.
- `resources/`
  - Chua resource khong phai code.
  - `resources/styles/game-ui.css`: stylesheet chung cho UI JavaFX.
  - `resources/levels/level_data.json`: data level.
- `assets/`
  - Chua image/map/tileset raw cho render game.
- `data/`
  - Runtime save file:
  - `survival_world.json`: save world va player session.
  - `player_progress.json`: tien do level.
  - `ui_settings.json`: cai dat UI.

## 3) Cong dung cac package chinh
- `src/main/`
  - `Main.java`: entrypoint game chinh, boot stage va game loop.
- `src/core/`
  - Dieu phoi game state, vong doi gameplay, map runtime, save/load.
- `src/buildsystem/`
  - Build system dung chung cho game chinh va demo.
- `src/build/`
  - Asset/collision helper cho build system runtime hien tai.
- `src/ui/`
  - Toan bo UI overlay JavaFX.
- `src/map/`
  - Loader/render map tiled, tileset, object/property map.
- `src/system/`
  - Damage, collision, resource, level, world save.

## 4) Data hien tai dang luu o dang gi
- `data/survival_world.json`: JSON.
- `data/player_progress.json`: JSON.
- `resources/levels/level_data.json`: JSON.
- `data/ui_settings.json`: JSON.

## 5) Cac thay doi da thuc hien trong dot nay
1. Refactor build sang `src/buildsystem`.
2. Game chinh va demo dung chung `BuildManager`.
3. Settings duoc doc/ghi bang `data/ui_settings.json`.
4. Xoa `src/wallbuilder` va cac lop build wall-only cu khong con duoc dung.

## 6) Huong mo rong tiep
1. Tach ro hon layer domain/application/infrastructure/presentation.
2. Tao `ResourceLocator` chung cho image/css/json.
3. Version hoa save schema.
4. Viet test cho `SettingsManager`, `WorldSaveService`, `BuildManager`.

## 7) Cach chay co ban
- Chay game chinh: run `src/main/Main.java`.
- Chay build demo: run `src/buildsystem/demo/BuildDemoMain.java`.
- Dam bao JavaFX SDK duoc cau hinh module-path trong IDE/CLI.
