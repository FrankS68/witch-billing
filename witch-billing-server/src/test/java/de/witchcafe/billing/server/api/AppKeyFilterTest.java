package de.witchcafe.billing.server.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HexFormat;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AppKeyFilterTest {

    @Test
    void findetAppZumKeyUndIgnoriertLeereEintraege() {
        String hash = HexFormat.of().formatHex(AppKeyFilter.sha256("geheim"));
        var filter = new AppKeyFilter(new AppKeyProperties(Map.of("quiz", hash, "leer", " ")));

        assertThat(filter.appFor("geheim")).contains("quiz");
        assertThat(filter.appFor("geheim2")).isEmpty();
        assertThat(filter.appFor("")).isEmpty();
    }
}
