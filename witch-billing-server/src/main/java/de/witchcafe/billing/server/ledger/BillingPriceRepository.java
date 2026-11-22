package de.witchcafe.billing.server.ledger;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingPriceRepository extends JpaRepository<BillingPrice, Long> {

    Optional<BillingPrice> findByAppAndFeature(String app, String feature);

    List<BillingPrice> findAllByOrderByAppAscFeatureAsc();
}
