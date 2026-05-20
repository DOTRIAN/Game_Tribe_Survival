# Build Render Flow

`Renderer.java` ve build object thong qua `BuildObject`:
- `renderPlacedBuildObjects(...)`: lap qua `buildManager.getPlacedObjects()`.
- Lay sprite theo `spriteKey` tu `AssetManager`.
- `renderBuildPreview(...)`: dung `buildsystem.core.BuildPreview`.

Muc tieu: renderer khong con phu thuoc class `build.Wall` cu.
