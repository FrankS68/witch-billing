package de.witchcafe.billing.server.ads;

/** Art einer Werbung. */
public enum AdKind {
    /** Eigene Werbung, z.B. fuer die anderen WitchCafe-Apps. Kein Tracking, kein Consent noetig. */
    HOUSE,
    /** Amazon-Partner-Link; die Apps zeigen den Pflicht-Hinweis dazu. */
    AMAZON,
    /** Anzeigenblock von Google AdSense (braucht in den Apps Publisher-Id und Einwilligung). */
    ADSENSE
}
