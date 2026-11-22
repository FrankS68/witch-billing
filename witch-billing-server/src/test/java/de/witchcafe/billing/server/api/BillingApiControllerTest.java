package de.witchcafe.billing.server.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import de.witchcafe.billing.server.ledger.BillingService;
import de.witchcafe.billing.server.ledger.Booking;
import de.witchcafe.billing.server.ledger.InsufficientCreditsException;
import de.witchcafe.billing.server.ledger.PriceKind;
import de.witchcafe.billing.server.ledger.UnknownFeatureException;

@WebMvcTest
@TestPropertySource(properties = {
        // SHA-256 von "quiz-test-key" bzw. "games-test-key"
        "witch-billing.app-keys.quiz=c509a5fe2b4057bf5846ef615e4e5de8fa364b5e5a80dd86a61dbb62d67913d0",
        "witch-billing.app-keys.games=767C9B3D596D38B7ABF35DAEA4EA7C9FC6D87AEBD699E4C67CEAC4D3B416116F",
        "witch-billing.app-keys.leer="
})
class BillingApiControllerTest {

    private static final String USAGE = """
            {"userId":"github:4711","feature":"ki-fragen","quantity":3,"idempotencyKey":"req-1"}""";

    @Autowired MockMvc mvc;
    @MockitoBean BillingService billing;

    private static MockHttpServletRequestBuilder postJson(String url, String key, String body) {
        var request = post(url).contentType(MediaType.APPLICATION_JSON).content(body);
        return key == null ? request : request.header("Authorization", "Bearer " + key);
    }

    @Test
    void ohneOderMitFalschemKeyGibt401() throws Exception {
        mvc.perform(postJson("/api/usage", null, USAGE)).andExpect(status().isUnauthorized());
        mvc.perform(postJson("/api/usage", "falsch", USAGE)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/balance/github:4711").header("Authorization", "Basic abc"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(billing);
    }

    @Test
    void verbrauchWirdFuerDieAppDesKeysGebucht() throws Exception {
        when(billing.charge("quiz", "github:4711", "ki-fragen", 3, "req-1"))
                .thenReturn(new Booking(42, -6, 94, false));

        mvc.perform(postJson("/api/usage", "quiz-test-key", USAGE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entryId").value(42))
                .andExpect(jsonPath("$.credits").value(-6))
                .andExpect(jsonPath("$.balanceAfter").value(94))
                .andExpect(jsonPath("$.replay").value(false));
    }

    @Test
    void hashInGrossbuchstabenWirdAuchErkannt() throws Exception {
        when(billing.charge(anyString(), anyString(), anyString(), anyInt(), any()))
                .thenReturn(new Booking(1, -1, 1, false));

        mvc.perform(postJson("/api/usage", "games-test-key", USAGE)).andExpect(status().isOk());

        verify(billing).charge("games", "github:4711", "ki-fragen", 3, "req-1");
    }

    @Test
    void beitragWirdGutgeschrieben() throws Exception {
        when(billing.reward("quiz", "github:4711", "frage-eingereicht", 1, null))
                .thenReturn(new Booking(7, 5, 105, false));

        mvc.perform(postJson("/api/contributions", "quiz-test-key", """
                        {"userId":"github:4711","feature":"frage-eingereicht","quantity":1}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.credits").value(5));
    }

    @Test
    void zuWenigGuthabenGibt402MitKontostand() throws Exception {
        when(billing.charge(anyString(), anyString(), anyString(), anyInt(), any()))
                .thenThrow(new InsufficientCreditsException("github:4711", 4, 6));

        mvc.perform(postJson("/api/usage", "quiz-test-key", USAGE))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_CREDITS"))
                .andExpect(jsonPath("$.balance").value(4))
                .andExpect(jsonPath("$.required").value(6));
    }

    @Test
    void unbekanntesFeatureGibt404() throws Exception {
        when(billing.charge(anyString(), anyString(), anyString(), anyInt(), any()))
                .thenThrow(new UnknownFeatureException("quiz", "ki-fragen", PriceKind.COST));

        mvc.perform(postJson("/api/usage", "quiz-test-key", USAGE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("UNKNOWN_FEATURE"));
    }

    @Test
    void ungueltigeAnfrageGibt400() throws Exception {
        when(billing.charge(anyString(), anyString(), anyString(), anyInt(), any()))
                .thenThrow(new IllegalArgumentException("quantity muss > 0 sein"));

        mvc.perform(postJson("/api/usage", "quiz-test-key", USAGE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(postJson("/api/usage", "quiz-test-key", "kein json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void kontostandAbfragen() throws Exception {
        when(billing.balance("github:4711")).thenReturn(80L);

        mvc.perform(get("/api/balance/github:4711").header("Authorization", "Bearer quiz-test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("github:4711"))
                .andExpect(jsonPath("$.balance").value(80));
    }
}
