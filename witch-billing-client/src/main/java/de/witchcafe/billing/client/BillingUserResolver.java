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
}
