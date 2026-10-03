# Changelog

Änderungen an MiniBlock. Die Versionsnummern beziehen sich auf die Mod.

## 0.3.0 — 2026-10-03

### Hinzugefügt

- Automatische öffentliche GitHub-Releases: Neue Versionen auf `main` erhalten nach bestandenem Build und Tests eine JAR, SHA-256-Prüfsumme und Versionshinweise.
- Allgemeine Anpassung der Minecraft-Partikeldarstellung an die kleine Spielergröße, einschließlich Abbau, Trankeffekten und bereits aktiven Partikeln beim Größenwechsel.
- Größenanpassung der Modellpartikel für Item-Pickup und Elder Guardian sowie Vererbung der Quellgröße an später erzeugte Kindpartikel.
- Materialbezogene Lauf-, Sprint-, Lande- und Abbaueffekte auf einzelnen Mini-Blöcken, einschließlich Teilformen wie Stufen.

### Behoben

- Abbaupartikel behalten Material, Farbposition und Größe auch nach dem Entfernen der letzten Mini-Zelle.
- Trank-, Ess-, Wasser- und andere körpergebundene Partikel berücksichtigen die Quellgröße des kleinen Spielers und bleiben nach dessen Vergrößern passend skaliert.
- Wasserblasen, animierte Wasserpartikel und der erste Bewegungsschritt von Krit-Partikeln berücksichtigen ebenfalls die Quellgröße.

### Prüfung

- Build erfolgreich, **35 Unit-Tests und 17 Minecraft-GameTests bestanden**.
- Regressionen für aktive und animierte Partikel beim Größenwechsel, Wasserpartikelbewegung, native Materialoberflächen und die Übertragung von Abbau- und Trankoptionen.
- Entwicklungsclient lädt die Partikelgruppen und ihre neuen Mixins; ein vollständiger manueller Spieltest in MultiMC steht noch aus.
- Release-Workflow für Versionsauswertung, JAR, Changelog-Auszug, SHA-256 und den Erhalt bereits veröffentlichter Versionen geprüft.

## 0.2.1 — 2026-10-03

### Behoben

- Creative-Flug beim kleinen Spieler: Auf- und Absteigen verwenden jetzt ebenso wie horizontaler Flug 1/16 der normalen Geschwindigkeit. Sprint-Flug und vorhandene Fluggeschwindigkeitseinstellungen bleiben erhalten.
- Braunes Reinflackern beim Sprinten: Die bisher normal großen Bodenpartikel erschienen fast auf Augenhöhe des kleinen Spielers. Die Sprintpartikel berücksichtigen jetzt die Spielergröße.

### Prüfung

- Build erfolgreich, **28 Unit-Tests und 15 Minecraft-GameTests bestanden**.
- Regressionstest für normale und schnelle Creative-Flugbahnen, unveränderte Ability-Einstellungen und die Rückkehr zur normalen Geschwindigkeit nach dem Vergrößern.
- Regressionstests für Partikelabstand zur kleinen Kamera, maximale Staubflugbahn und begrenzte Skalierung ausschließlich während der Sprintpartikelerzeugung.
- Entwicklungsclient startet erfolgreich; alle neuen Flug- und Partikel-Mixins wurden geladen und geprüft.
- Vollständiger manueller Spieltest in MultiMC steht noch aus.

## 0.2.0 — 2026-10-02

### Geändert

- Spielergröße und neu platzierte Mini-Blöcke verwenden jetzt **1/16** statt 1/8. Pro normalem Blockplatz gibt es **16 × 16 × 16** Zellen.
- Normale Minecraft-Block-Items ersetzen die separaten Mini-Baumaterial-Items: Normal groß werden sie normal platziert, klein als Mini-Blöcke. Jeder kleine Block verbraucht ein normales Block-Item.
- Normale Werkzeuge und Linksklick ersetzen den Mini-Meißel. Anvisieren und Abbauen betreffen eine einzelne Zelle; angrenzende Mini-Blöcke bleiben erhalten.
- Abbaudauer, geeignetes Werkzeug, Verschleiß und Beute werden anhand des Materials des anvisierten kleinen Blocks ausgewertet.
- Gespeicherte Bauwerke aus dem bisherigen 8er-Raster werden auf das 16er-Raster übertragen, ohne ihre Außenmaße zu ändern.
- Alte Mini-Bauitem-Stacks werden auf normale Material-Items umgestellt, alte Meißel auf Eisen-Spitzhacken.

### Hinzugefügt

- Eigene transparente **64 × 64 Pixel**-Textur für den Shrinkling; die bisherige Fernrohr-Textur entfällt.
- Platzierung normaler Blöcke mit statischen Modellen, einschließlich Glas, Stufen und Treppen. Die jeweiligen Blockformen werden mit verkleinert.
- Hinweis bei nicht unterstützten kleinen Platzierungen, etwa Kisten, Öfen, Türen und Betten. Das Item wird dabei nicht verbraucht.

### Entfernt

- Separate Mini-Bauitem- und Mini-Meißel-Registrierungen, ihre Itemmodelle und Crafting-Rezepte.

### Behoben

- Abgehackte kleine Sprünge: Schwerkraft, Sprintimpuls, Luftsteuerung und die Vanilla-Stoppschwellen werden zusammen mit dem Sprung verkleinert.
- Sicht durch Wände beim Anlaufen: Die Kamera-Clipping-Distanz berücksichtigt Spielergröße, Sichtwinkel und Bildschirmformat. Augenhöhe, Kamera-Wackeln und Third-Person-Kollisionsprüfung sind angepasst.
- Ein Wechsel zwischen zwei kleinen Blöcken innerhalb desselben normalen Blockplatzes beginnt einen neuen Abbauvorgang.
- Auswahlumrandung und Abbaurisse betreffen den einzelnen kleinen Block; der interne Speicher wird beim normalen Abbau nicht als Ganzes gelöscht.

### Prüfung

- Build erfolgreich, **25 Unit-Tests und 14 Minecraft-GameTests bestanden**.
- Clientstart, Mod-Ressourcen und Kamera-Mixins geprüft; vollständige manuelle Prüfung von Sprunggefühl und Bedienung noch offen.
- Der manuelle Spieltest in der README umfasst kleine Sprünge, Wandkontakt, einzelne Zellen, normale Materialien und Werkzeugverhalten.

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
