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
| `billing_ad` | Werbung für die Apps: eigene Werbung, Amazon-Partnerlink oder AdSense-Anzeigenblock; Zielgruppe „nur im Minus“ oder „alle“, optional nur für eine App |

- Einheit sind ganze **Credits**, kein Geld.
- Apps schicken nur Feature und Menge, den Betrag bestimmt die Preisliste. Preis 0 = nur zählen.
- Jede Buchung sperrt das Konto (Schreibsperre), damit parallele Anfragen sich nicht in die Quere kommen.
- **Schulden statt Sperre** (`witch-billing.allow-debt=true`, Default in `application.properties`): Verbrauch wird auch ohne Guthaben gebucht, das Konto geht ins Minus. Die Apps sperren dann nicht, sondern zeigen Werbung (witch-billing-vaadin). Mit `false` lehnt der Dienst ab (402), wie bis 0.1.0.
- Ein optionaler **Idempotenz-Schlüssel** (eindeutig pro App) verhindert Doppelbuchungen bei Wiederholungen.
- Neue Benutzer bekommen beim ersten Buchen ein Startguthaben (`witch-billing.welcome-credits`, Default 100).
- Login mit verschiedenen Providern ergibt (noch) getrennte Konten.
- Seit 0.3.0 schicken die Apps bei Buchungen die E-Mail (ohne E-Mail, z.B. Bluesky, den Handle) und den Namen mit; das Konto merkt sie sich, der Admin sieht, sucht und bucht danach.

## REST-API

Alle Aufrufe mit `Authorization: Bearer <API-Key der App>`. Die App ergibt sich aus dem Key;
sie kann nur Features aus ihrem Teil der Preisliste buchen.

| Aufruf | Body / Antwort |
|---|---|
| `POST /api/usage` | `{"userId","feature","quantity","idempotencyKey"?}` → `{"entryId","credits","balanceAfter","replay"}` |
| `POST /api/contributions` | wie oben, Gutschrift |
| `GET /api/balance/{userId}` | `{"userId","balance"}` |
| `GET /api/ads/{userId}` | `{"userId","balance","inDebt","ads":[{"id","kind","title","text","imageUrl","linkUrl","adsenseSlot"}]}` – im Minus alle aktiven Einträge der App, sonst nur die für „alle Benutzer“ |

Fehler kommen als `{"code","message","balance","required"}`:
401 ohne/mit falschem Key, 402 `INSUFFICIENT_CREDITS` (nur mit `allow-debt=false`; mit Kontostand und benötigtem Betrag),
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
    <version>0.3.0</version>
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
- `BookingResponse.isInDebt()`: gebucht, und das Konto ist danach im Minus.

## Werbung in Vaadin-Apps (witch-billing-vaadin)

Wer im Minus ist, wird nicht gesperrt, sondern sieht Werbung. Einbinden genügt für das Banner:

```xml
<dependency>
    <groupId>de.witchcafe</groupId>
    <artifactId>witch-billing-vaadin</artifactId>
    <version>0.3.0</version>   <!-- bringt witch-billing-client mit -->
</dependency>
```

- **Banner:** hängt sich per Auto-Configuration unten in jede Seite (kein Eingriff in witch-ui oder das Layout der App), nur für Benutzer im Minus, wegklickbar bis zum nächsten Seitenwechsel. Ohne gepflegte Werbung steht dort ein Hinweis mit Link zum Guthaben.
- **Werbepause:** nach einer Buchung `ads.afterCharge(booking)` aufrufen. Ist das Konto danach im Minus, erscheint ein Dialog mit Werbung, der sich erst nach einigen Sekunden schließen lässt (Countdown im Browser, kein Push nötig). Die Aktion ist schon erlaubt.
- Nach einer Gutschrift `ads.afterReward(booking)`, damit das Banner verschwindet, sobald das Konto wieder im Plus ist.
- Werbung kommt aus `/admin/werbung` im Billing-Dienst, pro Benutzer 60 s zwischengespeichert.
- **Amazon:** Link mit Partner-ID eintragen; die Apps zeigen „Anzeige“ und den Pflicht-Hinweis.
- **AdSense:** erst mit `witch-billing.ads.adsense-client=ca-pub-…` aktiv, sonst werden AdSense-Einträge übersprungen. Die Einwilligungs-Meldung (TCF) im AdSense-Konto unter „Datenschutz und Mitteilungen“ einrichten.

```java
@Autowired BillingClient billing;
@Autowired BillingAds ads;

BookingResponse booking = billing.charge("partie", 1);
ads.afterCharge(booking);                        // im Minus: Werbepause
```

```properties
witch-billing.ads.banner=true
witch-billing.ads.ad-break=true
witch-billing.ads.ad-break-seconds=5
witch-billing.ads.cache=60s
witch-billing.ads.adsense-client=
witch-billing.ads.credits-url=                   # leer = witch-billing.url
```

## Oberfläche

| Route | Wer | Inhalt |
|---|---|---|
| `/` | angemeldete Benutzer | Mein Guthaben: Kontostand und die letzten 200 Buchungen |
| `/admin/konten` | ADMIN | Konten suchen, Buchungen ansehen, Guthaben buchen (Gutschrift für Dienstleistungen, Korrektur) |
| `/admin/preise` | ADMIN | Preisliste: Verbrauch (kostet) und Beiträge (bringt) pro App und Feature; deaktivieren statt löschen |
| `/admin/nutzung` | ADMIN | Nutzung pro Feature im Zeitraum: Buchungen, Menge, Benutzer, Credits |
| `/admin/werbung` | ADMIN | Werbung für Benutzer im Minus (und „Passend dazu“ für alle): eigene, Amazon, AdSense |

Namen zu UserIds kennt der Dienst nur von Benutzern, die sich auch hier angemeldet haben.

## Deployment

Docker-Image aus dem Repo-Wurzelverzeichnis (`Dockerfile`, baut Client + Server), ausgerollt mit
`./deploy.sh <branch|tag>` (bauen, H2-Volume sichern, starten, Health-Check, sonst Rollback).

| Ort | Compose | Port | Adresse |
|---|---|---|---|
| Spiekeroog (QA) | `compose.pi.yml` | 8086 | http://spiekeroog.fritz.box:8086 |
| IONOS (Prod) | `compose.prod.yml` | 8085 (nur lokal, nginx davor) | https://billing.segieth.cologne |

Auf dem Server (nicht eingecheckt): `.env` (COMPOSE_PROJECT_NAME, COMPOSE_FILE, HEALTH_URL),
`.github_token` (read:packages) und `config/application-prod.properties` mit OAuth-Zugangsdaten,
`witch-auth.first-admin-email` und `witch-billing.app-keys.<app>=<SHA-256-Hash>`.

## Stand

- [x] Gerüst, Entities, `BillingService`, Tests
- [x] REST-API mit API-Key pro App
- [x] Client-Implementierung + Auto-Configuration
- [x] Views „Mein Guthaben“, Admin: Konten, manuelle Buchung, Preisliste, Nutzung
- [x] Quiz als erste App anbinden
- [x] Deployment auf IONOS, Quiz, DerGarten und Games angebunden
- [x] 0.2.0: Schulden statt Sperre, Werbung (`/admin/werbung`, `/api/ads`, witch-billing-vaadin)
- [x] 0.3.0: Apps schicken E-Mail bzw. Handle und Namen mit; Admin sieht, sucht und bucht danach
- [ ] Flyway statt `ddl-auto=update`
