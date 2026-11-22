package de.witchcafe.billing.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.spring.annotation.EnableVaadin;
import com.vaadin.flow.theme.lumo.Lumo;

@SpringBootApplication
@StyleSheet(Lumo.STYLESHEET)
@StyleSheet(Lumo.UTILITY_STYLESHEET)
@StyleSheet("styles.css")
// "de.witchcafe" deckt den Dienst selbst, witch-auth und witch-ui ab
@ComponentScan(basePackages = "de.witchcafe")
@EnableVaadin("de.witchcafe")
@EnableJpaRepositories("de.witchcafe")
@EntityScan("de.witchcafe")
public class BillingServerApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(BillingServerApplication.class, args);
    }
}
