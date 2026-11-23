package de.witchcafe.billing.server.ads;

/** Wer eine Werbung zu sehen bekommt. */
public enum AdAudience {
    /** Nur Benutzer, deren Konto im Minus ist (statt einer Sperre). */
    DEBTORS,
    /** Alle angemeldeten Benutzer, z.B. "Passend dazu"-Empfehlungen. */
    ALL
}
