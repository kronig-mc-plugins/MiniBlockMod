# MiniBlock

Schrumpfe auf ein Achtel deiner Größe und baue mit winzigen Blöcken. Die Bauwerke behalten ihre echten Maße, wenn du wieder groß wirst: Ein niedriger Durchgang bleibt niedrig und lässt dich nur dann hindurch, wenn deine aktuelle Körperhaltung hineinpasst.

Eine Minecraft-Java-Mod für **26.3**, **Forge 66.0.9** und **Java 25**. Aktuelle Mod-Version: **0.1.0**.

[![Build](https://github.com/kronig-mc-plugins/MiniBlockMod/actions/workflows/build.yml/badge.svg)](https://github.com/kronig-mc-plugins/MiniBlockMod/actions/workflows/build.yml)

- Spielergröße per Größen-Gerät zwischen normal und **⅛ Größe** wechseln.
- Acht Baumaterialien in einem **8 × 8 × 8** Raster pro normalem Blockplatz.
- Genaue Kollisionen für belegte Zellen und freie Durchgänge.
- Einzelne Mini-Blöcke mit dem Meißel entfernen.
- Mit **C** kriechen und niedrige Gänge benutzen.
- Bauwerke und Spielergröße werden gespeichert; die Mod unterstützt Client und Server.

Änderungen stehen im [Changelog](CHANGELOG.md), Hinweise zur Mitarbeit in [CONTRIBUTING.md](CONTRIBUTING.md).

## Installation

1. [Forge 66.0.9 für Minecraft 26.3](https://files.minecraftforge.net/net/minecraftforge/forge/index_26.3.html) installieren.
2. Die Mod mit den unten beschriebenen Build-Befehlen erstellen. Nach einem erfolgreichen [GitHub-Actions-Build](https://github.com/kronig-mc-plugins/MiniBlockMod/actions/workflows/build.yml) ist die JAR auch als Build-Artefakt des jeweiligen Laufs verfügbar.
3. `build/libs/miniblock-0.1.0.jar` in den `mods`-Ordner deiner Forge-Installation kopieren.
4. Minecraft mit dieser Forge-Installation starten. Im Kreativmodus gibt es den Reiter **MiniBlock**.

Auf einem Server brauchen Server und Mitspieler dieselbe Mod-Version und eine passende Forge-Installation. Minecraft Java 26.3 benötigt Java 25. Die Mod ist für Forge gebaut.

## Bedienung

| Gegenstand / Taste | Funktion |
| --- | --- |
| Größen-Gerät, Rechtsklick | Zwischen normaler Größe und einem Achtel wechseln |
| Mini-Block, Rechtsklick | Einen einzelnen Würfel mit 1/8 Block Kantenlänge platzieren |
| Mini-Meißel, Rechtsklick | Nur den anvisierten Mini-Block entfernen |
| C | Kriechen ein-/ausschalten; in den Steuerungsoptionen umbelegbar |
| Normales Werkzeug, Linksklick | Das gesamte Mini-Bauwerk an diesem normalen Blockplatz abbauen |

Das Größen-Gerät möglichst in die Luft benutzen. Beim Vergrößern wird geprüft, ob die tatsächliche Spieler-Hitbox Platz hat. Wenn du bereits kriechst, kannst du dich auch in einem passenden niedrigen Gang vergrößern. Ein zu enger Gang verhindert das Vergrößern.

## Mini-Blöcke und Kollision

Ein normaler Blockplatz enthält ein Raster aus **8 × 8 × 8** Zellen. Material, Darstellung und Kollision gehören jeweils zu diesen kleinen Zellen. Unbesetzte Zellen bleiben frei – auch nachdem sich der Spieler wieder groß gemacht hat. Die Bauwerke werden zusammen mit der Welt gespeichert und an die Clients übertragen.

Beispiel: Ein Gang mit **6 Mini-Blöcken Breite (0,75 Block)** und **5 Mini-Blöcken freier Höhe (0,625 Block)** lässt einen normalen Spieler kriechend durch. Stehend passt er nicht. Ein kleiner Spieler kann darin stehen. Die Maße ändern sich beim Größenwechsel nicht.

Kriechen endet erst dann vollständig, wenn wieder Platz zum Aufstehen ist. Normale Vanilla-Bewegung, Kamera und Spielermodelle verwenden das Größenattribut. Kleine Spieler bewegen sich langsamer, springen niedriger und haben weniger Reichweite.

Verfügbar sind Stein, Bruchstein, Eichenholzbretter, Ziegel, Erde sowie weißer, roter und blauer Beton. Diese erste Version unterstützt feste Bauwürfel; Mini-Kisten, Redstone, Flüssigkeiten und funktionierende Mini-Türen sind nicht enthalten. Mini-Bauwerke lassen sich nicht mit Kolben verschieben.

## Herstellung

- Ein passender normaler Block im Craftingfeld ergibt **64 Mini-Blöcke**. Das ist die Herstellungsmengen-Regel; geometrisch würden 512 Mini-Blöcke einen vollen Block ausfüllen.
- Mini-Meißel: ein Eisenbarren über einem Stock.
- Größen-Gerät:

```text
    Eisen
Redstone Amethystsplitter Redstone
    Eisen
```

## Entwickeln und prüfen

Voraussetzung: ein **JDK 25**. `java -version` sollte Java 25 anzeigen. Der Gradle-Wrapper liegt im Repository und lädt die passende Gradle-Version automatisch. Beim ersten Build werden die Forge- und Minecraft-Abhängigkeiten geladen.

Im Projektordner unter Windows:

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient
```

Unter Linux oder macOS:

```sh
./gradlew build
./gradlew runGameTestServer
./gradlew runClient
```

Die Unit-Tests prüfen genaue Geometrieabdeckung, Hohlräume, Kriechgänge, negative Koordinaten und unveränderliche Speichersnapshots. Die Forge-GameTests prüfen die Integration mit Minecraft-Kollisionen und Blockdaten. Die GameTests werden nur in einer Entwicklungsumgebung registriert.

`build` kompiliert die Mod, führt die Unit-Tests aus und erzeugt die JAR in `build/libs/`. `runGameTestServer` startet die Minecraft-Integrationstests. `runClient` startet einen Entwicklungsclient. Unit-Testberichte liegen unter `build/reports/tests/test/`.

Prüfstand am 2. Oktober 2026: **Build erfolgreich, 20 Unit-Tests und 5 Minecraft-Integrationstests bestanden.** Zusätzlich wurde der Forge-Client bis zur geladenen Oberfläche gestartet; die Mod-Ressourcen wurden ohne Modellfehler geladen. Eine vollständige Sicht- und Bedienprüfung in einer Spielwelt ist noch offen.

## Branches und Mitarbeit

- **`main`** enthält den freigegebenen Projektstand und ist der Standardbranch.
- **`dev`** ist der Integrationsbranch für neue Änderungen. Zum initialen Projektstand sind `main` und `dev` synchron.
- Änderungen werden von `dev` aus entwickelt, geprüft und anschließend nach `main` übernommen. Einzelne Aufgaben können in Feature-Branches mit einem Pull Request gegen `dev` bearbeitet werden.

Fehler und Ideen können als [GitHub-Issue](https://github.com/kronig-mc-plugins/MiniBlockMod/issues) eingetragen werden. Bei Fehlern bitte Minecraft-, Forge- und Mod-Version, Schritte zum Nachstellen und relevante Logstellen angeben. Details zum Ablauf stehen in [CONTRIBUTING.md](CONTRIBUTING.md).

Die [GitHub-Actions-Prüfung](https://github.com/kronig-mc-plugins/MiniBlockMod/actions/workflows/build.yml) baut die Mod mit Java 25 und führt Unit-Tests und Minecraft-GameTests aus.

## Manueller Spieltest

1. Im Kreativmodus das Gerät, den Meißel und einige Mini-Blöcke nehmen.
2. Mit dem Gerät klein machen und eine Wand mit Türöffnung bauen.
3. Ins Freie gehen und wieder groß machen: die Wand muss unverändert bleiben.
4. Einen Gang mit sechs Zellen freier Breite und fünf Zellen freier Höhe bauen. Mit C hinein kriechen; stehend darf der Spieler nicht hindurchgehen.
5. In einem zu engen Raum groß machen: das Gerät muss ablehnen, ohne den Spieler zu verschieben.
6. Einen Mini-Block mit dem Meißel entfernen und einen anderen daneben platzieren.
7. Welt speichern und erneut laden: Bauwerk und kleine Spielergröße müssen erhalten bleiben.

Offizielle Versionsgrundlagen: [Minecraft 26.3](https://feedback.minecraft.net/hc/en-us/articles/48913133328013-Minecraft-Java-Edition-26-3), [Forge 26.3](https://files.minecraftforge.net/net/minecraftforge/forge/index_26.3.html).

## Lizenz

MiniBlock steht unter der [MIT-Lizenz](LICENSE). Die mitgelieferte Lizenz des Forge-MDK steht separat in [Forge-MDK-LICENSE.txt](Forge-MDK-LICENSE.txt).
