# Changelog

Änderungen an MiniBlock. Die Versionsnummern beziehen sich auf die Mod.

## 0.1.0 — 2026-10-02

Erste Version für Minecraft Java 26.3, Forge 66.0.9 und Java 25.

### Hinzugefügt

- Größen-Gerät zum Wechsel zwischen normaler Spielergröße und **⅛ Größe**; Bewegung, Sprunghöhe, Stufenhöhe und Reichweite passen sich an.
- Sichere Größenänderung: Vergrößern ist nur möglich, wenn die größere Spieler-Hitbox in der aktuellen Körperhaltung Platz hat.
- Mini-Blöcke mit **⅛ Block Kantenlänge** und einem Raster aus **8 × 8 × 8** Zellen je normalem Blockplatz.
- Acht Materialien: Stein, Bruchstein, Eichenholzbretter, Ziegel, Erde sowie weißer, roter und blauer Beton.
- Genaue Kollisionen pro belegter Zelle. Hohlräume und Durchgänge bleiben beim Größenwechsel unverändert.
- Mini-Meißel zum gezielten Entfernen einzelner Zellen; vollständiger Abbau eines Mini-Bauwerks am normalen Blockplatz mit einem gewöhnlichen Werkzeug.
- Kriechen mit der umbelegbaren Taste **C**. Der Spieler bleibt in engen Gängen liegend, solange zum Aufstehen Platz fehlt.
- Crafting-Rezepte für Größen-Gerät, Meißel und alle Mini-Blockmaterialien; ein normaler Materialblock ergibt **64 Mini-Blöcke**.
- Speichern der Mini-Bauwerke und Spielergröße sowie Synchronisierung mit Clients.
- Eigener Kreativmodus-Reiter und deutsche sowie englische Texte.

### Prüfung

- Build erfolgreich.
- **20 Unit-Tests** und **5 Minecraft-GameTests** bestanden.
- Forge-Client bis zur geladenen Oberfläche gestartet; Mod-Ressourcen ohne Modellfehler geladen.
- Vollständige Sicht- und Bedienprüfung in einer Spielwelt steht noch aus.

### Umfang dieser Version

Mini-Blöcke sind feste Bauwürfel. Funktionierende Mini-Kisten, Redstone, Flüssigkeiten und Mini-Türen sind nicht enthalten. Mini-Bauwerke lassen sich nicht mit Kolben verschieben.
