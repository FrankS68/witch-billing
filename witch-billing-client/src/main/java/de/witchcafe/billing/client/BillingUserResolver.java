package de.witchcafe.billing.client;

import java.util.Optional;

/**
 * Liefert die UserId des angemeldeten Benutzers fuer die Kurzformen von {@link BillingClient}.
 * Mit witch-auth automatisch dessen UserId ("provider:providerId"); eine App kann eine eigene
 * Bean dieses Typs bereitstellen.
 */
@FunctionalInterface
public interface BillingUserResolver {

    Optional<String> currentUserId();

    /**
     * Lesbare Kennung des angemeldeten Benutzers fuer den Admin im Billing-Dienst: die E-Mail,
     * ohne E-Mail (z.B. Bluesky) der Name bzw. Handle. Leer, wenn unbekannt.
     */
    default Optional<String> currentUserLabel() {
        return Optional.empty();
    }

    /** Anzeigename des angemeldeten Benutzers; leer, wenn unbekannt. */
    default Optional<String> currentUserName() {
        return Optional.empty();
    }
}
