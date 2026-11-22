# witch-billing

Gemeinsames Guthabenkonto für alle WitchCafe-Apps (Quiz, Games, DerGarten).
Apps buchen **Verbrauch** kostenpflichtiger Funktionen; aufgefüllt wird das Guthaben durch
**Beiträge** der Nutzer in den Apps und durch **manuelle Buchungen** eines Admins
(z. B. für erbrachte Dienstleistungen). Reicht das Guthaben nicht, lehnt der Dienst den
Verbrauch ab (HTTP 402) und die App führt die Funktion nicht aus.

```
witch-billing-client/   Bibliothek für die Apps (de.witchcafe.billing.client)
                        + Datentypen der REST-API (…client.api)
witch-billing-server/   Zentraler Dienst (de.witchcafe.billing.server), witch-parent + witch-ui,
                        Port 8085, lokal H2, auf IONOS MariaDB
```

## Konzept

| Tabelle | Inhalt |
|---|---|
| `billing_account` | Kontostand pro witch-auth-UserId (`provider:providerId`), für alle Apps gemeinsam |
| `billing_entry` | Buchungsjournal, nur anfügen: WELCOME, USAGE, CONTRIBUTION, ADMIN_GRANT, CORRECTION |
| `billing_price` | Preisliste pro App und Feature: COST (kostet) oder REWARD (bringt ein), Credits pro Einheit |

- Einheit sind ganze **Credits**, kein Geld.
- Apps schicken nur Feature und Menge, den Betrag bestimmt die Preisliste. Preis 0 = nur zählen.
- Jede Buchung sperrt das Konto (Schreibsperre), damit parallele Anfragen es nicht ins Minus bringen.
- Ein optionaler **Idempotenz-Schlüssel** (eindeutig pro App) verhindert Doppelbuchungen bei Wiederholungen.
- Neue Benutzer bekommen beim ersten Buchen ein Startguthaben (`witch-billing.welcome-credits`, Default 100).
- Login mit verschiedenen Providern ergibt (noch) getrennte Konten.

## REST-API

Alle Aufrufe mit `Authorization: Bearer <API-Key der App>`. Die App ergibt sich aus dem Key;
sie kann nur Features aus ihrem Teil der Preisliste buchen.

| Aufruf | Body / Antwort |
|---|---|
| `POST /api/usage` | `{"userId","feature","quantity","idempotencyKey"?}` → `{"entryId","credits","balanceAfter","replay"}` |
| `POST /api/contributions` | wie oben, Gutschrift |
| `GET /api/balance/{userId}` | `{"userId","balance"}` |

Fehler kommen als `{"code","message","balance","required"}`:
401 ohne/mit falschem Key, 402 `INSUFFICIENT_CREDITS` (mit Kontostand und benötigtem Betrag),
404 `UNKNOWN_FEATURE`, 400 `INVALID_REQUEST`.

### API-Key für eine App anlegen

Der Dienst speichert nur den SHA-256-Hash, den Klartext bekommt nur die App:

```bash
KEY=$(openssl rand -hex 32)                      # -> in die App: witch-billing.api-key
printf %s "$KEY" | sha256sum | cut -d' ' -f1     # -> in den Dienst: BILLING_KEY_HASH_QUIZ
```

## Client in eine App einbinden

```xml
<dependency>
    <groupId>de.witchcafe</groupId>
    <artifactId>witch-billing-client</artifactId>
    <version>0.1.0</version>
</dependency>
```

```properties
witch-billing.url=${WITCH_BILLING_URL:}          # leer = Billing aus, alles erlaubt
witch-billing.api-key=${WITCH_BILLING_API_KEY:}
witch-billing.fail-open=true                     # Dienst weg -> erlauben + warnen (false: sperren)
witch-billing.timeout=5s
```

```java
@Autowired BillingClient billing;

try {
    billing.charge("ki-fragen", 20);             // angemeldeter witch-auth-Benutzer
    erzeugeFragen(20);
} catch (InsufficientCreditException e) {
    Notification.show("Nicht genug Guthaben: " + e.getBalance() + " von " + e.getRequired());
}

billing.reward("frage-eingereicht");             // Beitrag gutschreiben
billing.balance().ifPresent(b -> ...);           // leer, wenn Billing aus/nicht erreichbar
```

- Die Kurzformen erzeugen selbst einen Idempotenz-Schlüssel; Netzwerkfehler werden dann einmal wiederholt.
- `BillingException` (Feature unbekannt, ungültige Anfrage) kommt auch bei fail-open – das ist ein Fehler in App oder Preisliste.
- `BookingResponse.NOT_BOOKED` / `isBooked()`: erlaubt, aber nicht gebucht (Billing aus oder Dienst nicht erreichbar).
- Ohne witch-auth eine eigene `BillingUserResolver`-Bean bereitstellen oder die Varianten mit userId nutzen.

## Oberfläche

| Route | Wer | Inhalt |
|---|---|---|
| `/` | angemeldete Benutzer | Mein Guthaben: Kontostand und die letzten 200 Buchungen |
| `/admin/konten` | ADMIN | Konten suchen, Buchungen ansehen, Guthaben buchen (Gutschrift für Dienstleistungen, Korrektur) |
| `/admin/preise` | ADMIN | Preisliste: Verbrauch (kostet) und Beiträge (bringt) pro App und Feature; deaktivieren statt löschen |
| `/admin/nutzung` | ADMIN | Nutzung pro Feature im Zeitraum: Buchungen, Menge, Benutzer, Credits |

Namen zu UserIds kennt der Dienst nur von Benutzern, die sich auch hier angemeldet haben.

## Deployment

Docker-Image aus dem Repo-Wurzelverzeichnis (`Dockerfile`, baut Client + Server), ausgerollt mit
`./deploy.sh <branch|tag>` (bauen, H2-Volume sichern, starten, Health-Check, sonst Rollback).

| Ort | Compose | Port | Adresse |
|---|---|---|---|
| Spiekeroog (QA) | `compose.pi.yml` | 8086 | http://spiekeroog.fritz.box:8086 |
| IONOS (Prod) | folgt | 8085 | folgt |

Auf dem Server (nicht eingecheckt): `.env` (COMPOSE_PROJECT_NAME, COMPOSE_FILE, HEALTH_URL),
`.github_token` (read:packages) und `config/application-prod.properties` mit OAuth-Zugangsdaten,
`witch-auth.first-admin-email` und `witch-billing.app-keys.<app>=<SHA-256-Hash>`.

## Stand

- [x] Gerüst, Entities, `BillingService`, Tests
- [x] REST-API mit API-Key pro App
- [x] Client-Implementierung + Auto-Configuration
- [x] Views „Mein Guthaben“, Admin: Konten, manuelle Buchung, Preisliste, Nutzung
- [x] Quiz als erste App anbinden
- [ ] Flyway statt `ddl-auto=update`, Deployment auf IONOS
