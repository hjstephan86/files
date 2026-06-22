# files — Persönlicher Dateimanager

Capacitor-basierte Android-App als persönlicher Dateimanager für den privaten Sideload-Einsatz (kein Play Store). Entwickelt und getestet auf dem Pixel 9a mit Android 16.

## Projektstruktur

```
files/
├── www/
│   └── index.html              ← komplette App (HTML + CSS + JS in einer Datei)
├── android/                    ← generiert von Capacitor
│   └── app/src/main/
│       ├── AndroidManifest.xml ← Storage-Permissions
│       └── java/de/epp/files/
│           └── MainActivity.java ← MANAGE_EXTERNAL_STORAGE Intent
├── capacitor.config.json
├── package.json
└── README.md
```

## Voraussetzungen

- Node.js ≥ 18
- Java JDK 21 (Oracle oder OpenJDK)
- Android SDK / ADB
- `JAVA_HOME` auf JDK 21 gesetzt

```bash
export JAVA_HOME=/usr/lib/jvm/jdk-21.0.7-oracle-x64
export PATH=$JAVA_HOME/bin:$PATH
```

## Setup & Build

```bash
# 1. Dependencies installieren
npm install

# 2. Android-Plattform generieren
npx cap add android

# 3. Web-Assets synchronisieren
npx cap sync

# 4. APK bauen
cd android
./gradlew assembleDebug

# 5. Auf Gerät installieren
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Nach Änderungen an `www/index.html` immer:

```bash
npx cap sync
cd android && ./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Permissions

In `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="29"/>
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"/>

<application
    android:requestLegacyExternalStorage="true"
    ...>
```

Die `MainActivity.java` öffnet beim ersten Start automatisch die Android-Einstellungsseite für „Alle Dateien verwalten". Einmalig aktivieren — danach voller Zugriff auf `/sdcard/`.

## Features

| Feature                                                       | Status |
| ------------------------------------------------------------- | ------ |
| Verzeichnisse durchsuchen                                     | ✅     |
| Schnellzugriff-Kacheln                                        | ✅     |
| Dateien öffnen (nativer Intent)                               | ✅     |
| Papierkorb (de-/aktivierbar)                                  | ✅     |
| Papierkorb leeren / Wiederherstellen                          | ✅     |
| Namen (Umbenennen)                                            | ✅     |
| Teilen                                                        | ✅     |
| Suche                                                         | ✅     |
| Sortierung (Name / Datum / Größe / Typ)                       | ✅     |
| Zuletzt geöffnet                                              | ✅     |
| Versteckte Dateien ein-/ausblenden                            | ✅     |
| Hardware-Zurück-Taste                                         | ✅     |
| **In-App Audioplayer** (MP3, FLAC, AAC, M4A …)                | ✅     |
| Playlist aus aktuellem Verzeichnis (Zur / Vor)                | ✅     |
| Lautstärkeregelung (Leiser / Lauter)                          | ✅     |
| Pause / Weiter durch Antippen des laufenden Lieds             | ✅     |
| Automatisch nächstes Lied nach Ende                           | ✅     |
| Laufendes Lied in der Dateiliste markiert                     | ✅     |
| **Kopfhörer-/Bluetooth-Steuerung** (Play, Pause, Vor, Zurück) | ✅     |
| **ZIP-Dateien entpacken** (direkt im selben Verzeichnis)      | ✅     |
| **Ordner packen** (als ZIP im selben Verzeichnis)             | ✅     |

### Kopfhörer- und Bluetooth-Steuerung

Der In-App Audioplayer unterstützt Hardware-Medientasten – an Kopfhörern, Headsets und Bluetooth-Geräten:

| Taste                          | Aktion                          |
| ------------------------------ | ------------------------------- |
| Play / Pause (einfacher Druck) | Pause / Weiter                  |
| Nächster Titel                 | Nächstes Lied in der Playlist   |
| Vorheriger Titel               | Vorheriges Lied in der Playlist |

Die Steuerung funktioniert auch bei gesperrtem Bildschirm über die Android Media Session.

### Ordner packen

Ordner können per Tipp auf **Packen** im Kontextmenü als ZIP-Datei komprimiert werden.

- Das Kontextmenü eines Ordners bietet: **Öffnen**, **Namen**, **Packen**, **Löschen**.
- Die ZIP-Datei wird **im selben Verzeichnis** wie der Ordner erstellt und trägt den Ordnernamen (z. B. `Fotos.zip`).
- Unterordner und alle enthaltenen Dateien werden rekursiv hinzugefügt.
- Während des Packens zeigt ein Toast eine animierte Fortschrittsanzeige (`Packe .` → `..` → `...`).
- Nach dem Abschluss erscheint der Hinweis **„`Ordnername.zip` erstellt"**.
- Ist das Packen nicht möglich (Fehler, fehlende Berechtigung), erscheint **„Packen nicht möglich"**.
- Das Packen erfolgt nativ über den `ZipPlugin` (Java `ZipOutputStream`).

### ZIP-Dateien entpacken

ZIP-Dateien werden beim Antippen **direkt im selben Verzeichnis** entpackt – ohne Rückfrage.

- Während des Entpackens zeigt ein Toast-Hinweis eine animierte Fortschrittsanzeige (`Entpacke .` → `..` → `...`).
- Nach dem Abschluss wird die Anzahl der entpackten Dateien angezeigt.
- Enthält die ZIP Unterordner, werden diese automatisch angelegt.
- Ist das Entpacken nicht möglich (Fehler, fehlende Berechtigung, ungültiges Archiv), erscheint der Hinweis **„Entpacken nicht möglich"**.
- Das Entpacken erfolgt nativ über den `ZipPlugin` (Java `ZipInputStream`) – ohne Speicherlimitierung durch den WebView.

### In-App Audioplayer

Der Audioplayer ist über den Schnellzugriff **Musik** erreichbar. Sobald eine Audiodatei angetippt wird, startet die Wiedergabe direkt in der App – ohne externe App.

- Am unteren Bildschirmrand erscheint eine **Player-Leiste** mit Titel und Steuerelementen (_Zur_, _Leiser_, _Lauter_, _Vor_).
- Die **aktuell gespielte Datei** ist in der Liste durch Akzentfarbe und Rahmen hervorgehoben.
- **Pause/Weiter**: Das laufende Lied in der Liste antippen schaltet zwischen Abspielen und Pause um. Im pausieren Zustand wird der Titel in der Leiste mit ⏸ markiert und der Eintrag ist gedimmt.
- Nach Ende eines Titels wechselt der Player automatisch zum nächsten Lied. Am Ende der Liste beginnt er wieder von vorne.
- Die Player-Leiste bleibt sichtbar, solange ein Lied geladen ist, auch beim Wechsel in andere Unterordner der Musikansicht.

## Design

- **Theme:** Dunkel — Hintergrund `#131313`, Buttons `#2A2A2A`
- **Akzent:** Bernsteingelb `#E8A830`
- **Schrift:** DM Sans — klar, nicht ermüdend
- **Buttons:** stark abgerundet (`border-radius: 18px`), helle Schrift `#F4F0EB`
- **Touch-Targets:** min. 44 × 44 px

## Hinweise

- Ausschließlich für privaten Sideload-Einsatz konzipiert — keine Play-Store-Kompatibilität
- `MANAGE_EXTERNAL_STORAGE` ist von Google Play für normale Apps nicht erlaubt
- Gradle benötigt zwingend JDK 21 — nicht JDK 23 oder 24

## Erwerb

Der Preis für diese Software beträgt 3,44 EUR.

### Zahlungsinformationen

Name: Stephan Epp  
IBAN: DE24 5003 1900 0012 5603 20
BIC: BBVADEFFXXX

**Wichtig**: Der Geist Gottes wurde beschworen, dass die Regierenden der Länder, in denen oder für die diese Arbeit illegal benutzt wird, vom Geist Gottes mit dem Tod bestraft werden.
