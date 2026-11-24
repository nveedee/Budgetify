# Budget Planer PWA

Persönlicher Budget Planer von Nveedee.

## Setup in IntelliJ

1. **Öffnen**: `File → Open → budgetplaner-Ordner wählen`
2. **Maven importieren**: IntelliJ erkennt pom.xml automatisch, bei Nachfrage „Load Maven Project" klicken
3. **Lokales Profil aktivieren** (nutzt H2): in der Run-Config von
   `BudgetplanerApplication` unter *Active profiles* `local` eintragen
   (oder VM-Option `-Dspring.profiles.active=local`).
4. **Starten**: `BudgetplanerApplication.java` → Run (grüner Pfeil)
5. **Browser**: http://localhost:8080

## Features

- 🏦 Bis zu 3 Konten mit Kontostand
- 📈 Transaktionen (Lohn, Twint-Eingänge, Ausgaben)
- 🔄 Daueraufträge (Spotify, Miete etc.)
- 📊 Budgets mit Monats-Tracking und Fortschrittsbalken
- 📦 Produktscanner (Barcode via ZXing + Open Food Facts), Preis manuell
- 📱 PWA — installierbar auf dem Handy

## Datenbank

- **Lokal** (Profil `local`): H2 (eingebettet, läuft automatisch). Console unter
  http://localhost:8080/h2-console — JDBC URL `jdbc:h2:file:./data/budgetplaner`, User `sa`, Passwort leer.
- **Produktion**: PostgreSQL (siehe Deployment).

## Deployment (Railway)

Die Produktions-Config (`application.properties`) nutzt PostgreSQL über Umgebungsvariablen.

1. In Railway dem Projekt ein **PostgreSQL**-Plugin hinzufügen.
2. Beim App-Service diese **Variablen** setzen:
   - `DATABASE_URL` → **als JDBC-URL**, z. B.
     `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}`
     ⚠️ Railways automatische `DATABASE_URL` ist `postgresql://…` — Spring braucht aber
     das `jdbc:`-Präfix, deshalb oben selbst zusammensetzen (oder die Variable überschreiben).
     Benutzer/Passwort sind in dieser URL enthalten; alternativ separat setzen.
3. `PORT` setzt Railway automatisch; die App bindet via `server.port=${PORT:8080}`.
4. Deploy: Railway baut mit Maven und startet das Jar (kein Profil → PostgreSQL-Config greift).

## PWA auf Handy installieren

Chrome: Dreipunkt-Menü → „Zum Startbildschirm hinzufügen"
Safari: Teilen → „Zum Home-Bildschirm"

> Handy muss im gleichen WLAN sein wie der PC. URL: http://[PC-IP]:8080
