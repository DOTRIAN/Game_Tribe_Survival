# Tribe Survival Game

## Mục tiêu chức năng
- Chạy world survival 2D bằng JavaFX.
- World render trên `Canvas`, UI render bằng JavaFX overlay node thật.
- Hỗ trợ resize, maximize, fullscreen không còn khoảng trắng quanh game.
- Có HUD gọn hơn, hotbar nhỏ hơn, minimap nhỏ hơn.
- Có shop, inventory, settings, main menu và name input theo CSS riêng.
- Giữ hệ build wall, hotbar chọn item và inventory hiện có.

## Cấu trúc UI mới
- `Canvas`:
  - Chỉ render world, entity, build preview, wall, hiệu ứng.
- `AnchorPane` overlay:
  - `HudOverlay`: góc trái trên.
  - `ResourcePanel`: góc phải trên.
  - `MinimapOverlay`: góc phải dưới.
  - `HotbarOverlay`: giữa dưới.
  - `ShopOverlay`: giữa màn hình.
  - `InventoryOverlay`: giữa màn hình.
  - `MainMenuScreen`: overlay menu chính.
  - `NameInputScreen`: overlay nhập tên.
  - `SettingsScreen`: overlay cài đặt.
  - `Guide/Pause/GameOver/Victory`: overlay trạng thái.

## Cấu trúc file chính
- [Main.java](d:/IT/Game_TEST/src/main/Main.java)
- [Game.java](d:/IT/Game_TEST/src/core/Game.java)
- [Renderer.java](d:/IT/Game_TEST/src/ui/Renderer.java)
- [UIManager.java](d:/IT/Game_TEST/src/ui/UIManager.java)
- [HudOverlay.java](d:/IT/Game_TEST/src/ui/HudOverlay.java)
- [ResourcePanel.java](d:/IT/Game_TEST/src/ui/ResourcePanel.java)
- [MinimapOverlay.java](d:/IT/Game_TEST/src/ui/MinimapOverlay.java)
- [HotbarOverlay.java](d:/IT/Game_TEST/src/ui/HotbarOverlay.java)
- [ShopOverlay.java](d:/IT/Game_TEST/src/ui/ShopOverlay.java)
- [InventoryOverlay.java](d:/IT/Game_TEST/src/ui/InventoryOverlay.java)
- [MainMenuScreen.java](d:/IT/Game_TEST/src/ui/MainMenuScreen.java)
- [NameInputScreen.java](d:/IT/Game_TEST/src/ui/NameInputScreen.java)
- [SettingsScreen.java](d:/IT/Game_TEST/src/ui/SettingsScreen.java)
- [ResponsiveLayoutManager.java](d:/IT/Game_TEST/src/ui/ResponsiveLayoutManager.java)
- [GameSettings.java](d:/IT/Game_TEST/src/ui/GameSettings.java)
- [SettingsManager.java](d:/IT/Game_TEST/src/ui/SettingsManager.java)
- [game-ui.css](d:/IT/Game_TEST/src/main/resources/styles/game-ui.css)

## Vai trò từng class
- `Game`
  - Giữ state gameplay, input flow, save/load, spawn, shop purchase, hotbar selection.
- `Renderer`
  - Tạo `Scene`, bind `Canvas` theo cửa sổ, render world, đẩy state sang `UIManager`.
- `UIManager`
  - Điều phối tất cả overlay JavaFX.
- `HudOverlay`
  - Hiển thị HP, Energy, XP, Level và nút Shop.
- `ResourcePanel`
  - Hiển thị coin, stone wall, wood, stone, fiber...
- `MinimapOverlay`
  - Vẽ minimap lên canvas con nhỏ hơn.
- `HotbarOverlay`
  - Hiển thị 9 slot, icon item, số lượng và slot đang chọn.
- `ShopOverlay`
  - Panel mua item bằng coin.
- `InventoryOverlay`
  - Grid inventory có tooltip.
- `MainMenuScreen`
  - Panel menu chính.
- `NameInputScreen`
  - Panel nhập tên và validate.
- `SettingsScreen`
  - Slider volume, toggle fullscreen, FPS, debug grid.
- `ResponsiveLayoutManager`
  - Helper đặt anchor layout theo góc màn hình.
- `GameSettings`
  - Model settings runtime.
- `SettingsManager`
  - Lưu/nap settings vào `data/ui_settings.properties`.

## CSS hoạt động như thế nào
- Toàn bộ UI dùng file:
  - [game-ui.css](d:/IT/Game_TEST/src/main/resources/styles/game-ui.css)
- Các class CSS chính:
  - `.hud-panel`
  - `.hud-title`
  - `.status-bar`
  - `.hp-bar`
  - `.energy-bar`
  - `.xp-bar`
  - `.hotbar`
  - `.hotbar-slot`
  - `.hotbar-slot-selected`
  - `.shop-panel`
  - `.shop-item-card`
  - `.shop-buy-button`
  - `.menu-panel`
  - `.menu-button`
  - `.input-panel`
  - `.inventory-panel`

## Fullscreen / responsive
- `Canvas` bind trực tiếp theo `StackPane` root:
  - `canvas.widthProperty().bind(root.widthProperty())`
  - `canvas.heightProperty().bind(root.heightProperty())`
- Camera dùng viewport động:
  - `renderer.getViewportWidth() / CAMERA_ZOOM`
  - `renderer.getViewportHeight() / CAMERA_ZOOM`
- UI neo theo `AnchorPane`:
  - HUD trái trên: `20px`
  - Resource panel phải trên: `20px`
  - Minimap phải dưới: `20px`
  - Hotbar giữa dưới: `20px`
- Nền menu/world được vẽ kiểu cover để lấp kín cửa sổ, tránh khoảng trắng.

## Cách thêm item vào shop
1. Thêm metadata item trong [UIManager.java](d:/IT/Game_TEST/src/ui/UIManager.java) ở `createItemMetaMap(...)`.
2. Thêm item vào `shopItems`.
3. Thêm giá và logic cộng inventory trong [Game.java](d:/IT/Game_TEST/src/core/Game.java) ở `purchaseShopItem(...)`.

## Cách thêm item vào hotbar
1. Thêm item metadata trong `UIManager`.
2. Mở rộng mapping slot trong [Game.java](d:/IT/Game_TEST/src/core/Game.java) ở `setSelectedHotbarIndex(...)`.
3. Mở rộng hiển thị icon/amount trong [HotbarOverlay.java](d:/IT/Game_TEST/src/ui/HotbarOverlay.java).

## Cách thêm asset/icon mới
- Stone wall icon đang dùng crop từ `tuong.jpg`.
- Item khác hiện dùng placeholder text ngắn như `PT`, `PX`, `TR`.
- Nếu có icon thật:
  1. load `Image`
  2. gán vào `ItemUiMeta`
  3. `ShopOverlay`, `InventoryOverlay`, `ResourcePanel`, `HotbarOverlay` sẽ dùng lại ảnh đó.

## Shop hoạt động ra sao
- Mở shop bằng `B` hoặc nút `Shop`.
- Shop đọc coin từ inventory.
- Click `Buy`:
  - `Game.purchaseShopItem(...)`
  - trừ `coin`
  - cộng item vào `Inventory`
  - UI tự cập nhật ở frame kế tiếp.
- Nếu không đủ coin:
  - hiện toast `Not enough coins`.

## Inventory hoạt động ra sao
- Mở bằng `I`.
- Hiển thị toàn bộ `inventory.snapshot()`.
- Mỗi slot có:
  - icon hoặc placeholder
  - số lượng
  - tooltip tên + mô tả

## Hotbar hoạt động ra sao
- Có 9 slot.
- Slot `1` hiện map với `stone_wall`.
- Chọn bằng click hoặc phím `1-9`.
- Slot đang chọn có viền sáng rõ.
- Số lượng hiện góc phải dưới của slot.

## Phím tắt
- `1-9`: chọn hotbar
- `B`: mở/đóng shop
- `I`: mở/đóng inventory
- `M`: bật/tắt minimap
- `Q`: xoay hướng tường
- `ESC`: đóng overlay trước, nếu không có overlay thì pause/build cancel
- `F11`: fullscreen
- `P`: resume khi pause
- `R`: restart khi game over

## Lưu ý về build wall
- Preview và wall thật vẫn đi qua `BuildManager`.
- UI mới không thay logic build cốt lõi.
- Click lên UI sẽ không kích attack hay place wall.

## Tài nguyên giao diện đã dùng
- Nền menu:
  - `assets/backgrounds/menu_bg1.png`
- Nền world fallback:
  - `assets/backgrounds/grass03.png`
- Wall icon:
  - crop từ `assets/stone_wall/tuong.jpg`
- Bạn đang có thêm local asset pack khá lớn trong:
  - `assets/tilesets/Pixel Crawler - Free Pack`
  - Có thể lấy icon/tool/station thật để thay placeholder text sau này.

## Cách chạy
1. Mở project bằng IDE JavaFX.
2. Chạy [Main.java](d:/IT/Game_TEST/src/main/Main.java).
3. Nếu dùng CLI, cần cấu hình `--module-path` cho JavaFX SDK.

## Kiểm tra sau khi sửa
- Resize cửa sổ nhỏ/lớn.
- Maximize.
- Fullscreen `F11`.
- Vào menu, mở settings, back ra.
- Vào game, mở shop `B`, inventory `I`, minimap `M`.
- Mua `Stone Wall`.
- Kiểm tra `coin` giảm, `stone_wall` tăng, hotbar cập nhật.
- Click lên HUD/hotbar/shop không được đánh hoặc đặt wall.

## Giới hạn hiện tại
- Một số file Java cũ ngoài phạm vi UI vẫn đang có lỗi BOM/encoding khi compile toàn repo bằng `javac`.
- Các item ngoài `stone_wall` hiện có placeholder icon, chưa phải pixel art thật.
- Shop/inventory đã hoạt động ở mức gameplay cơ bản, chưa có equip system riêng.

## TODO sau này
1. Thêm icon pixel art thật cho potion, torch, sword, pickaxe.
2. Cho phép hotbar chứa nhiều item loại khác nhau, không chỉ wall.
3. Thêm pause menu node-based riêng.
4. Thêm animation mở panel và toast tự tắt theo thời gian.
5. Lưu cả wall đã đặt và inventory expanded metadata vào save file.
