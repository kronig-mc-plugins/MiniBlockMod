# Eigene Item-Grafik

Der Shrinkling verwendet `src/main/resources/assets/miniblock/textures/item/shrink_device.png`: ein originales, transparentes 64 × 64 Pixel-Asset. Das Itemmodell referenziert diese Mod-Textur; es verwendet keine bestehende Minecraft-Itemgrafik.

Die Grafik wurde mit dem eingebauten Imagegen-Werkzeug erzeugt und für die Spieltextur mit Nearest-Neighbor auf 64 × 64 Pixel exportiert. Der Export erhält den Alphakanal.

## Generierungsprompt

```text
Use case: stylized-concept
Asset type: Minecraft mod item texture, single original Shrinkling device icon.
Primary request: Create a brand-new, distinctive handheld shrinking device sprite suitable for a Minecraft inventory item. A chunky little techno wand with an amethyst-purple handle/body, a vivid turquoise/cyan central energy capsule, and a compact squared emitter at its upper tip. It should read instantly at tiny inventory size. It is a completely original silhouette and does not use, trace or imitate existing Minecraft item textures.
Style/medium: strict low-resolution pixel art, coherent 32 by 32 pixel sprite enlarged with nearest-neighbor to a square output. Each sprite pixel must be an aligned square; solid discrete colors, no antialiasing, no smooth gradients, no blurry pixels. Use a restrained palette of dark navy outline, purple shell, lavender highlights and turquoise emitter.
Composition/framing: one centered device, angled diagonally from bottom-left to top-right, occupies most of the square with several pixels of transparent margin, one readable clean silhouette.
Scene/backdrop: genuine fully transparent alpha background.
Constraints: no text, no watermark, no hands, no people, no shadows outside sprite, no other items, no framing border, no checkerboard drawn into image. Pixel-art sprite only.
```
