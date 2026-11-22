package de.witchcafe.billing.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import de.witchcafe.billing.client.api.BookingResponse;

class BillingAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BillingAutoConfiguration.class));

    @Test
    void ohneUrlIstBillingAusUndAllesErlaubt() {
        runner.run(context -> {
            BillingClient client = context.getBean(BillingClient.class);
            assertThat(client).isInstanceOf(DisabledBillingClient.class);
            assertThat(client.charge("github:1", "ki-fragen", 5, null)).isSameAs(BookingResponse.NOT_BOOKED);
            assertThat(client.balance("github:1")).isEmpty();
            // witch-auth ist im Test auf dem Klassenpfad, aber ohne CurrentUser-Bean
            assertThat(client.currentUserId()).isEmpty();
        });
    }

    @Test
    void mitUrlEntstehtRestClient() {
        runner.withPropertyValues("witch-billing.url=http://127.0.0.1:8085", "witch-billing.api-key=k",
                        "witch-billing.fail-open=false", "witch-billing.timeout=2s")
                .run(context -> {
                    assertThat(context.getBean(BillingClient.class)).isInstanceOf(RestBillingClient.class);
                    BillingProperties props = context.getBean(BillingProperties.class);
                    assertThat(props.isFailOpen()).isFalse();
                    assertThat(props.timeoutOrDefault()).hasSeconds(2);
                });
    }

    @Test
    void eigeneBeansHabenVorrang() {
        runner.withBean(BillingUserResolver.class, () -> () -> Optional.of("google:9"))
                .run(context -> assertThat(context.getBean(BillingClient.class).currentUserId())
                        .contains("google:9"));
    }
}
