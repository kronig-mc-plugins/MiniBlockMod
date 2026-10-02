# Mitarbeit an MiniBlock

Fehlerberichte, Ideen und Pull Requests sind willkommen. Das Projekt verwendet Minecraft Java 26.3, Forge 66.0.9 und ein JDK 25. Installation, Bedienung und Entwicklungsbefehle stehen in der [README](README.md).

## Fehler und Ideen melden

Für Fehler bitte ein [GitHub-Issue](https://github.com/kronig-mc-plugins/MiniBlockMod/issues/new/choose) mit diesen Angaben öffnen:

- Minecraft-, Forge- und MiniBlock-Version; bei anderen Mods auch deren Namen und Versionen.
- Einzelspieler oder Server, Betriebssystem und Java-Version.
- Schritte zum Nachstellen sowie erwartetes und tatsächliches Verhalten.
- Relevante Stellen aus `logs/latest.log` oder dem Crash-Bericht; Screenshots helfen bei Darstellungsfehlern.

Persönliche Daten, Adressen und Zugangsdaten vor dem Hochladen aus Logs entfernen. Für eine Idee kurz beschreiben, was du im Spiel tun möchtest und welches Problem die Funktion lösen würde.

## Branch-Ablauf

`main` ist der Standardbranch für freigegebene Projektstände. `dev` sammelt Änderungen vor der Übernahme nach `main`.

1. Einen Feature-Branch von `dev` erstellen, zum Beispiel `feature/neues-material` oder `fix/kollision`.
2. Eine zusammenhängende Änderung umsetzen und bestehende Konventionen beibehalten.
3. Die für die Änderung passenden Prüfungen ausführen.
4. Bei Änderungen für Spieler den [Changelog](CHANGELOG.md) und gegebenenfalls die README ergänzen.
5. Einen Pull Request gegen **`dev`** öffnen. Zweck, Verhalten und Prüfergebnisse kurz beschreiben.

Nach der Integration und Prüfung wird ein Pull Request von `dev` nach `main` mit einem Merge-Commit übernommen. Anschließend wird `dev` per Fast-Forward auf den neuen `main`-Commit gesetzt. Damit zeigen beide Branches nach der Übernahme auf denselben Commit.

## Prüfen

Unter Windows im Projektordner:

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
```

Unter Linux oder macOS dieselben Aufgaben mit `./gradlew` ausführen. Bei Änderungen an Bewegung, Spielergröße, Kollision oder Darstellung zusätzlich den Client mit `runClient` starten und den manuellen Spieltest aus der README durchführen. Ergebnisse genau angeben: Ein erfolgreicher Clientstart ersetzt keinen Spieltest in einer Welt.

Die Unit-Tests decken Rastergeometrie und Koordinaten ab. GameTests prüfen die Minecraft-Integration. Für neue Fehlerkorrekturen einen passenden Regressionstest ergänzen, wenn das Verhalten automatisiert zuverlässig prüfbar ist.

Die [GitHub-Actions-Prüfung](https://github.com/kronig-mc-plugins/MiniBlockMod/actions/workflows/build.yml) führt Build, Unit-Tests und GameTests mit Java 25 aus und stellt die gebaute JAR nach einem erfolgreichen Lauf als Artefakt bereit.

## Hinweise zum Code

- Java-Quellcode liegt unter `src/main/java/de/niklas/miniblock/`.
- Rezepte und andere Serverdaten liegen unter `src/main/resources/data/miniblock/`.
- Client-Ressourcen und Übersetzungen liegen unter `src/main/resources/assets/miniblock/`.
- Das Weltformat speichert die Minecraft-Blockstates der Mini-Blöcke. Änderungen an Paletten und Rastergröße müssen bestehende Welten berücksichtigen; Bauwerke aus dem 8er-Raster behalten bei der Umstellung ihre Außenmaße.
- Kleine Bauplätze verwenden normale Block-Items und Standardwerkzeuge. Material, Abbaudauer, Drops und Tool-Verschleiß gehören zur einzelnen anvisierten Zelle. Benachbarte Zellen dürfen beim Abbau nicht verloren gehen.
- Blöcke mit Blockentities und mehrteilige Blöcke können nicht verkleinert platziert werden. Eine Erweiterung dieses Umfangs braucht passende Speicher-, Render- und Interaktionstests.
- Änderungen an Speicherung oder Netzwerkprotokoll müssen Weltstände und Client/Server-Verhalten berücksichtigen.
- Build-Ausgaben, lokale Welten und Laufzeit-Logs gehören nicht in einen Pull Request.
