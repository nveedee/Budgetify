# Budget Planer PWA

Persönlicher Budget Planer von Nveedee.

## Setup in IntelliJ

1. **Öffnen**: `File → Open → budgetplaner-Ordner wählen`
2. **Maven importieren**: IntelliJ erkennt pom.xml automatisch, bei Nachfrage „Load Maven Project" klicken
3. **Lokales Profil aktivieren** (nutzt H2): in der Run-Config von
   `BudgetplanerApplication` unter *Active profiles* `local` eintragen
   (oder VM-Option `-Dspring.profiles.active=local`).
4. **Gemini API Key** eintragen in `src/main/resources/application-local.properties`:
   ```
   gemini.api.key=DEIN_KEY_HIER
   ```
   → Key holen: https://aistudio.google.com/app/apikey
   (Diese Datei ist in `.gitignore` und wird nicht committet.)
5. **Starten**: `BudgetplanerApplication.java` → Run (grüner Pfeil)
6. **Browser**: http://localhost:8080

## Features

- 🏦 Bis zu 3 Konten mit Kontostand
- 📈 Transaktionen (Lohn, Twint-Eingänge, Ausgaben)
- 🔄 Daueraufträge (Spotify, Miete etc.)
- 📊 Budgets mit Monats-Tracking und Fortschrittsbalken
- 📦 Produktscanner mit Gemini AI Preis-Suche
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
   - `GEMINI_API_KEY` → dein Gemini-Key.
3. `PORT` setzt Railway automatisch; die App bindet via `server.port=${PORT:8080}`.
4. Deploy: Railway baut mit Maven und startet das Jar (kein Profil → PostgreSQL-Config greift).

> Der Gemini-Key liegt lokal nur in `application-local.properties` (git-ignoriert).
> Da der Key zuvor im Repo-Verlauf stand, sollte er bei Veröffentlichung rotiert werden.

## PWA auf Handy installieren

Chrome: Dreipunkt-Menü → „Zum Startbildschirm hinzufügen"
Safari: Teilen → „Zum Home-Bildschirm"

> Handy muss im gleichen WLAN sein wie der PC. URL: http://[PC-IP]:8080
