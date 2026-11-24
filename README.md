# Budget Planer PWA

Persönlicher Budget Planer von Nveedee.

## Setup in IntelliJ

1. **Öffnen**: `File → Open → budgetplaner-Ordner wählen`
2. **Maven importieren**: IntelliJ erkennt pom.xml automatisch, bei Nachfrage „Load Maven Project" klicken
3. **Gemini API Key** eintragen in `src/main/resources/application.properties`:
   ```
   gemini.api.key=DEIN_KEY_HIER
   ```
   → Key holen: https://aistudio.google.com/app/apikey

4. **Starten**: `BudgetplanerApplication.java` → Run (grüner Pfeil)
5. **Browser**: http://localhost:8080

## Features

- 🏦 Bis zu 3 Konten mit Kontostand
- 📈 Transaktionen (Lohn, Twint-Eingänge, Ausgaben)
- 🔄 Daueraufträge (Spotify, Miete etc.)
- 📊 Budgets mit Monats-Tracking und Fortschrittsbalken
- 📦 Produktscanner mit Gemini AI Preis-Suche
- 📱 PWA — installierbar auf dem Handy

## Datenbank

H2 (eingebettet, läuft automatisch). Console unter:
http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/budgetplaner`
- User: `sa` / Passwort leer

## PWA auf Handy installieren

Chrome: Dreipunkt-Menü → „Zum Startbildschirm hinzufügen"
Safari: Teilen → „Zum Home-Bildschirm"

> Handy muss im gleichen WLAN sein wie der PC. URL: http://[PC-IP]:8080
