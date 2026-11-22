package de.witchcafe.billing.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.io.IOException;
import java.util.Optional;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import de.witchcafe.billing.client.api.BookingResponse;

class RestBillingClientTest {

    private static final String URL = "http://billing";
    private static final String USER = "github:4711";
    private static final String BOOKING = """
            {"entryId":42,"credits":-6,"balanceAfter":94,"replay":false}""";

    private MockRestServiceServer server;

    private RestBillingClient client(boolean failOpen, Optional<String> currentUser) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(URL)
                .defaultHeader("Authorization", "Bearer key");
        server = MockRestServiceServer.bindTo(builder).build();
        return new RestBillingClient(builder.build(), failOpen, () -> currentUser);
    }

    private RestBillingClient client(boolean failOpen) {
        return client(failOpen, Optional.of(USER));
    }

    @Test
    void verbrauchWirdGebucht() {
        var client = client(true);
        server.expect(requestTo(URL + "/api/usage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer key"))
                .andExpect(jsonPath("$.userId").value(USER))
                .andExpect(jsonPath("$.feature").value("ki-fragen"))
                .andExpect(jsonPath("$.quantity").value(3))
                .andExpect(jsonPath("$.idempotencyKey").value("req-1"))
                .andRespond(withSuccess(BOOKING, MediaType.APPLICATION_JSON));

        BookingResponse booking = client.charge(USER, "ki-fragen", 3, "req-1");

        assertThat(booking).isEqualTo(new BookingResponse(42, -6, 94, false));
        assertThat(booking.isBooked()).isTrue();
        server.verify();
    }

    @Test
    void kurzformNimmtAngemeldetenBenutzerUndErzeugtSchluessel() {
        var client = client(true);
        server.expect(requestTo(URL + "/api/contributions"))
                .andExpect(jsonPath("$.userId").value(USER))
                .andExpect(jsonPath("$.quantity").value(1))
                .andExpect(jsonPath("$.idempotencyKey").isNotEmpty())
                .andRespond(withSuccess(BOOKING, MediaType.APPLICATION_JSON));

        client.reward("frage-eingereicht");

        server.verify();
    }

    @Test
    void kurzformOhneAngemeldetenBenutzerScheitert() {
        var client = client(true, Optional.empty());

        assertThatThrownBy(() -> client.charge("ki-fragen", 1)).isInstanceOf(IllegalStateException.class);
        assertThat(client.balance()).isEmpty();
    }

    @Test
    void zuWenigGuthabenWirftInsufficientCredit() {
        var client = client(true);
        server.expect(requestTo(URL + "/api/usage")).andRespond(withStatus(HttpStatus.PAYMENT_REQUIRED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"code":"INSUFFICIENT_CREDITS","message":"reicht nicht","balance":4,"required":6}"""));

        assertThatThrownBy(() -> client.charge(USER, "ki-fragen", 3, "req-1"))
                .isInstanceOfSatisfying(InsufficientCreditException.class, e -> {
                    assertThat(e.getBalance()).isEqualTo(4);
                    assertThat(e.getRequired()).isEqualTo(6);
                    assertThat(e.getMessage()).isEqualTo("reicht nicht");
                });
    }

    @Test
    void unbekanntesFeatureWirftBillingExceptionAuchBeiFailOpen() {
        var client = client(true);
        server.expect(requestTo(URL + "/api/usage")).andRespond(withStatus(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"code":"UNKNOWN_FEATURE","message":"Kein aktiver Preis"}"""));

        assertThatThrownBy(() -> client.charge(USER, "ki-fragen", 3, "req-1"))
                .isInstanceOfSatisfying(BillingException.class,
                        e -> assertThat(e.getCode()).isEqualTo("UNKNOWN_FEATURE"))
                .isNotInstanceOf(BillingUnavailableException.class);
    }

    @Test
    void serverfehlerBeiFailOpenErlaubtOhneBuchung() {
        var client = client(true);
        server.expect(requestTo(URL + "/api/usage")).andRespond(withServerError());

        BookingResponse booking = client.charge(USER, "ki-fragen", 3, "req-1");

        assertThat(booking).isSameAs(BookingResponse.NOT_BOOKED);
        assertThat(booking.isBooked()).isFalse();
    }

    @Test
    void serverfehlerOhneFailOpenWirftUnavailable() {
        var client = client(false);
        server.expect(requestTo(URL + "/api/usage")).andRespond(withServerError());

        assertThatThrownBy(() -> client.charge(USER, "ki-fragen", 3, "req-1"))
                .isInstanceOf(BillingUnavailableException.class);
    }

    @Test
    void abgelehnterKeyGiltAlsNichtVerfuegbar() {
        var client = client(true);
        server.expect(requestTo(URL + "/api/usage")).andRespond(withUnauthorizedRequest());

        assertThat(client.charge(USER, "ki-fragen", 3, "req-1")).isSameAs(BookingResponse.NOT_BOOKED);
    }

    @Test
    void netzwerkfehlerWirdMitSchluesselEinmalWiederholt() {
        var client = client(false);
        server.expect(requestTo(URL + "/api/usage")).andRespond(withException(new IOException("weg")));
        server.expect(requestTo(URL + "/api/usage"))
                .andExpect(jsonPath("$.idempotencyKey").value("req-1"))
                .andRespond(withSuccess(BOOKING, MediaType.APPLICATION_JSON));

        assertThat(client.charge(USER, "ki-fragen", 3, "req-1").entryId()).isEqualTo(42);
        server.verify();
    }

    @Test
    void netzwerkfehlerOhneSchluesselWirdNichtWiederholt() {
        var client = client(false);
        server.expect(ExpectedCount.once(), requestTo(URL + "/api/usage"))
                .andRespond(withException(new IOException("weg")));

        assertThatThrownBy(() -> client.charge(USER, "ki-fragen", 3, null))
                .isInstanceOf(BillingUnavailableException.class);
        server.verify();
    }

    @Test
    void kontostand() {
        var client = client(false);
        server.expect(requestTo(URL + "/api/balance/github:4711"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"userId":"github:4711","balance":80}""", MediaType.APPLICATION_JSON));

        assertThat(client.balance(USER)).isEqualTo(OptionalLong.of(80));
    }

    @Test
    void kontostandBeiServerfehlerLeer() {
        var client = client(false);
        server.expect(ExpectedCount.manyTimes(), requestTo(URL + "/api/balance/github:4711"))
                .andRespond(withServerError());

        assertThat(client.balance(USER)).isEmpty();
    }
}
