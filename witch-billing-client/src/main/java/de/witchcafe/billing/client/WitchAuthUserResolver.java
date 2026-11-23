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

    /** E-Mail, ohne E-Mail (z.B. Bluesky) der Name, der dort der Handle ist. */
    @Override
    public Optional<String> currentUserLabel() {
        CurrentUser cu = currentUser.getIfAvailable();
        if (cu == null) {
            return Optional.empty();
        }
        return cu.get().map(user -> {
            String email = user.getEmail();
            return email != null && !email.isBlank() ? email : user.getFullName();
        }).filter(s -> s != null && !s.isBlank());
    }

    @Override
    public Optional<String> currentUserName() {
        CurrentUser cu = currentUser.getIfAvailable();
        if (cu == null) {
            return Optional.empty();
        }
        return cu.get().map(user -> user.getFullName()).filter(s -> s != null && !s.isBlank());
    }
}
