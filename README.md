# MiniBlock

Schrumpfe mit dem **Shrinkling** auf ein Sechzehntel deiner Größe und baue mit deinen normalen Minecraft-Blöcken. Derselbe Bruchstein wird groß als normaler Block und klein als Mini-Block platziert. Die kleinen Bauwerke behalten ihre echten Maße, wenn du wieder groß wirst: Ein niedriger Durchgang lässt dich nur hindurch, wenn deine aktuelle Körperhaltung hineinpasst.

Eine Minecraft-Java-Mod für **26.3**, **Forge 66.0.9** und **Java 25**. Aktuelle Mod-Version: **0.3.0**.

[![Build](https://github.com/kronig-mc-plugins/MiniBlockMod/actions/workflows/build.yml/badge.svg)](https://github.com/kronig-mc-plugins/MiniBlockMod/actions/workflows/build.yml)

- Spielergröße per Shrinkling zwischen normal und **1/16 Größe** wechseln.
- Normale Block-Items verwenden; Mini-Blöcke haben **1/16 Block Kantenlänge**.
- Ein **16 × 16 × 16** Raster pro normalem Blockplatz mit genauen Kollisionen und freien Durchgängen.
- Einzelne Mini-Blöcke mit normalen Werkzeugen und Linksklick abbauen.
- Mit **C** kriechen und niedrige Gänge benutzen.
- Eigenes violett-türkises Shrinkling-Item mit transparenter Pixel-Art-Textur.
- Gleichmäßig skalierte Sprungphysik und Kamera-Clipping-Abstände für kleine Spieler.
- Größenabhängige Darstellung aller Minecraft-Partikelgruppen, einschließlich Tränken, Abbau und Pickup-Effekten; Materialpartikel beim Laufen auf Mini-Blöcken.
- Bauwerke und Spielergröße werden gespeichert und zwischen Client und Server synchronisiert.

Änderungen stehen im [Changelog](CHANGELOG.md), Hinweise zur Mitarbeit in [CONTRIBUTING.md](CONTRIBUTING.md).

## Installation

1. [Forge 66.0.9 für Minecraft 26.3](https://files.minecraftforge.net/net/minecraftforge/forge/index_26.3.html) installieren.
2. Die JAR aus dem [aktuellen Release](https://github.com/kronig-mc-plugins/MiniBlockMod/releases/latest) herunterladen oder mit den unten beschriebenen Build-Befehlen selbst erstellen.
3. `miniblock-0.3.0.jar` in den `mods`-Ordner deiner Forge-Installation kopieren und die bisherige MiniBlock-JAR entfernen.
4. Minecraft mit dieser Forge-Installation starten. Im Kreativmodus findest du den Shrinkling im Reiter **MiniBlock**; Baumaterial und Werkzeuge nimmst du aus den normalen Minecraft-Reitern.

Auf einem Server brauchen Server und Mitspieler dieselbe Mod-Version und eine passende Forge-Installation. Minecraft Java 26.3 benötigt Java 25.

## Bedienung

| Gegenstand / Taste | Funktion |
| --- | --- |
| Shrinkling, Rechtsklick | Zwischen normaler Größe und einem Sechzehntel wechseln |
| Normaler Block, Rechtsklick, normal groß | Einen normalen Minecraft-Block platzieren |
| Derselbe Block, Rechtsklick, klein | Einen Mini-Block mit 1/16 Block Kantenlänge platzieren |
| Werkzeug oder Hand, Linksklick auf Mini-Block | Nur den anvisierten kleinen Block abbauen, auch wenn du normal groß bist |
| C | Kriechen ein-/ausschalten; in den Steuerungsoptionen umbelegbar |

Beim Vergrößern wird geprüft, ob die tatsächliche Spieler-Hitbox Platz hat. Wenn du bereits kriechst, kannst du dich auch in einem passenden niedrigen Gang vergrößern. Ein zu enger Gang verhindert das Vergrößern.

Jeder platzierte Mini-Block verbraucht **ein normales Block-Item**. Beim Abbau wird der einzelne Mini-Block mit dem benutzten Werkzeug und den Loot-Regeln seines Minecraft-Materials ausgewertet. Du erhältst normale Items; es gibt keine separaten Mini-Baumaterial-Items und keinen Mini-Meißel. Im Kreativmodus werden keine Items verbraucht und beim Abbau keine Beute erzeugt.

## Mini-Blöcke und Kollision

Ein normaler Blockplatz enthält ein Raster aus **16 × 16 × 16** Zellen. Darstellung und Kollision gehören zu den einzelnen kleinen Blöcken. Unbesetzte Zellen bleiben frei – auch nachdem sich der Spieler wieder groß gemacht hat. Anvisieren und Abbauen betreffen jeweils nur eine kleine Zelle. Benachbarte Mini-Blöcke können direkt aneinanderliegen, ohne gemeinsam abgebaut zu werden.

Beispiel: Ein Gang mit **10 Mini-Blöcken freier Breite (0,625 Block)** und **10 Mini-Blöcken freier Höhe (0,625 Block)** lässt einen normalen Spieler kriechend hindurch. Stehend passt er nicht. Ein kleiner Spieler kann darin stehen. Die Maße ändern sich beim Größenwechsel nicht.

Normale Block-Items mit statischen Blockmodellen können verkleinert platziert werden, beispielsweise Bruchstein, Holz, Glas, Stufen und Treppen. Die Blockform wird mit verkleinert. Funktionierende Mini-Blockmaschinen, Redstone-Schaltungen und Flüssigkeiten sind nicht Teil dieser Version. Blöcke mit eigenen Blockentities, etwa Kisten und Öfen, sowie mehrteilige Blöcke wie Türen, Betten und hohe Pflanzen werden beim kleinen Platzieren abgewiesen: Das Item bleibt im Inventar und kann normal groß platziert werden. Mini-Bauwerke lassen sich nicht mit Kolben verschieben.

Kriechen endet erst dann vollständig, wenn wieder Platz zum Aufstehen ist. Kleine Spieler bewegen sich langsamer, springen niedriger und haben weniger Reichweite. Sprungimpuls, Schwerkraft, Sprint-Sprung und Luftsteuerung sind gemeinsam skaliert; der Sprung dauert wie bei normaler Größe. Creative-Flug ist horizontal sowie beim Auf- und Absteigen auf 1/16 skaliert; Sprint-Flug behält seinen normalen Geschwindigkeitsfaktor. Die Kamera passt Clipping-Abstand und Wackeln an die kleinen Maße an und übernimmt beim Größenwechsel sofort die neue Augenhöhe.

Partikel passen ihre sichtbare Größe auch dann an, wenn sie beim Schrumpfen bereits aktiv sind. Das gilt für die Minecraft-Partikelgruppen mit Texturflächen und Modellen, darunter Trankeffekte, Rauch, Funken, Abbau, eingesammelte Items und den Elder-Guardian-Effekt. Effekte eines kleinen Spielers und Mini-Blocks behalten ihre kleine Quellgröße auch nach dem Vergrößern; ihre Bewegung, Schwerkraft und später erzeugten Kindpartikel berücksichtigen diese Quellgröße. Positionen von Umgebungseffekten bleiben an ihrem tatsächlichen Ort in der Welt. Beim Laufen, Sprinten, Landen und Abbauen auf Mini-Blöcken werden Material und Oberfläche der einzelnen Zelle verwendet, einschließlich Stufen.

## Herstellung

Für die Baumaterialien brauchst du keine Umwandlungsrezepte. Der Shrinkling wird so hergestellt:

```text
    Eisen
Redstone Amethystsplitter Redstone
    Eisen
```

## Upgrade von 0.1.0

Gespeicherte Bauwerke im bisherigen **8 × 8 × 8** Raster werden auf das neue Raster übertragen. Eine alte Zelle belegt dabei acht neue Zellen; ihre sichtbare Größe und der Durchgang bleiben erhalten. Neu platzierte Blöcke sind dagegen 1/16 Block groß. Alte Mini-Bauitem-Stacks werden auf ihre normalen Minecraft-Materialien umgestellt, alte Mini-Meißel auf Eisen-Spitzhacken. Vor einem Versionswechsel empfiehlt sich eine Sicherung des Weltordners. Die alten Mini-Bauitem- und Meißel-Rezepte gibt es ab 0.2.0 nicht mehr.

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

Die Unit-Tests prüfen Rastergeometrie, Hohlräume, Kriechgänge, Koordinaten und Speichersnapshots. Die Forge-GameTests prüfen die Integration mit Minecraft-Kollisionen und Blockdaten. Die GameTests werden nur in einer Entwicklungsumgebung registriert.

`build` kompiliert die Mod, führt die Unit-Tests aus und erzeugt die JAR in `build/libs/`. `runGameTestServer` startet die Minecraft-Integrationstests. `runClient` startet einen Entwicklungsclient. Unit-Testberichte liegen unter `build/reports/tests/test/`.

Prüfstand von **0.3.0** am 3. Oktober 2026: Build erfolgreich, **35 Unit-Tests und 17 Minecraft-GameTests bestanden**. Die Tests vergleichen unter anderem echte Sprung- und Creative-Flugbahnen mit Vanilla, prüfen Kamerageometrie, animierte Partikelgrößen, Wasserpartikelbewegung und die Materialerkennung unter den Füßen. Native Abbau- und Trankoptionen werden über den tatsächlichen Netzwerk-Codec geprüft, auch nach dem Entfernen der letzten Quellzelle. Die Client-Mixins werden beim Entwicklungsclientstart zusätzlich geladen und geprüft. Eine vollständige manuelle Prüfung von Bild, Fluggefühl und Bedienung in einer Spielwelt steht noch aus.

## Branches und Mitarbeit

- **`main`** enthält den freigegebenen Projektstand und ist der Standardbranch.
- **`dev`** ist der Integrationsbranch für neue Änderungen.
- Änderungen werden von `dev` aus entwickelt, geprüft und anschließend nach `main` übernommen. Einzelne Aufgaben können in Feature-Branches mit einem Pull Request gegen `dev` bearbeitet werden.

Fehler und Ideen können als [GitHub-Issue](https://github.com/kronig-mc-plugins/MiniBlockMod/issues) eingetragen werden. Bei Fehlern bitte Minecraft-, Forge- und Mod-Version, Schritte zum Nachstellen und relevante Logstellen angeben. Details zum Ablauf stehen in [CONTRIBUTING.md](CONTRIBUTING.md).

Die [GitHub-Actions-Prüfung](https://github.com/kronig-mc-plugins/MiniBlockMod/actions/workflows/build.yml) baut die Mod mit Java 25 und führt Unit-Tests und Minecraft-GameTests aus. Nach einem erfolgreichen Build auf `main` veröffentlicht sie eine neue Mod-Versionsnummer automatisch als öffentliches GitHub-Release mit JAR, SHA-256-Prüfsumme und den zugehörigen Changelog-Einträgen. Builds auf `dev` und Pull Requests liefern weiterhin Build-Artefakte; bereits veröffentlichte Versionsdateien werden nicht überschrieben.

## Manueller Spieltest

1. Im Kreativmodus den Shrinkling, normale Bruchsteine, Glas, Stufen, Treppen und eine normale Spitzhacke nehmen.
2. Einen Bruchstein normal groß platzieren. Mit dem Gerät klein machen und mit demselben Item mehrere Mini-Blöcke nebeneinander platzieren.
3. Mit Linksklick und Spitzhacke einen einzelnen Mini-Block abbauen: Die Nachbarblöcke müssen erhalten bleiben. Im Überlebensmodus Beute, Abbaudauer, Werkzeugverschleiß und Itemverbrauch prüfen.
4. Klein auf Erde laufen und sprinten, anschließend an eine normale Wand und eine Mini-Wand laufen und springen. Erdpartikel dürfen das Bild nicht braun überdecken. Die Kamera darf keine Sicht durch die Wand geben; Sprünge müssen gleichmäßig verlaufen.
5. Ins Freie gehen und wieder groß machen: Mini-Wand und Blockformen müssen unverändert bleiben. Auch groß muss sich eine einzelne Mini-Zelle anvisieren und abbauen lassen.
6. Einen Gang mit zehn Zellen freier Breite und zehn Zellen freier Höhe bauen. Mit C hinein kriechen; stehend darf der Spieler nicht hindurchgehen.
7. In einem zu engen Raum groß machen: Das Gerät muss ablehnen, ohne den Spieler zu verschieben.
8. Eine Kiste oder Tür klein platzieren versuchen: Die Platzierung muss abgewiesen werden, ohne ein Item zu verbrauchen.
9. Welt speichern und erneut laden: Bauwerk und kleine Spielergröße müssen erhalten bleiben. Ein Bauwerk aus 0.1.0 muss dieselben Außenmaße behalten.
10. Im Kreativmodus klein fliegen: Horizontal, aufwärts und abwärts muss die Geschwindigkeit zur kleinen Größe passen. Sprint-Flug muss weiterhin schneller sein. Nach dem Großwerden muss wieder die normale Fluggeschwindigkeit gelten.
11. Auf Mini-Erde, Mini-Betonpulver und Mini-Stufen laufen, sprinten und landen: Partikel müssen zum berührten Material und zur kleinen Oberfläche passen. Einen einzelnen Mini-Block abbauen und sofort groß werden: Seine Abbaupartikel müssen klein bleiben.
12. Trankeffekte, Essen, Schwimmen, Feuerwerk und Item-Pickup klein und groß vergleichen. Während bereits laufender Effekte schrumpfen und wieder wachsen: Die Darstellung muss sofort folgen, ohne Positionen in der Welt zu verschieben.

Offizielle Versionsgrundlagen: [Minecraft 26.3](https://feedback.minecraft.net/hc/en-us/articles/48913133328013-Minecraft-Java-Edition-26-3), [Forge 26.3](https://files.minecraftforge.net/net/minecraftforge/forge/index_26.3.html).

## Lizenz

MiniBlock steht unter der [MIT-Lizenz](LICENSE). Die mitgelieferte Lizenz des Forge-MDK steht separat in [Forge-MDK-LICENSE.txt](Forge-MDK-LICENSE.txt).
