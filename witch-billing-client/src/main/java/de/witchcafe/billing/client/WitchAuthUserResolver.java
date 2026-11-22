package de.witchcafe.billing.client;

import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;

import de.witchcafe.auth.CurrentUser;

/**
 * {@link BillingUserResolver} ueber witch-auth. Wird nur geladen, wenn witch-auth auf dem
 * Klassenpfad ist (siehe {@link BillingAutoConfiguration}).
 */
class WitchAuthUserResolver implements BillingUserResolver {

    private final ObjectProvider<CurrentUser> currentUser;

    WitchAuthUserResolver(ObjectProvider<CurrentUser> currentUser) {
        this.currentUser = currentUser;
    }

    @Override
    public Optional<String> currentUserId() {
        CurrentUser cu = currentUser.getIfAvailable();
        if (cu == null) {
            return Optional.empty();
        }
        return cu.get().map(user -> user.getUserId().toString());
    }
}
