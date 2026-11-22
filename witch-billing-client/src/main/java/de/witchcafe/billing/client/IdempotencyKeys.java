package de.witchcafe.billing.client;

import java.util.UUID;

/** Erzeugt Idempotenz-Schluessel fuer Buchungen. */
public final class IdempotencyKeys {

    private IdempotencyKeys() {}

    /** Ein neuer, zufaelliger Schluessel (36 Zeichen). */
    public static String next() {
        return UUID.randomUUID().toString();
    }
}
