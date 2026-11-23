package de.witchcafe.billing.server.ads;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BillingAdRepository extends JpaRepository<BillingAd, Long> {

    List<BillingAd> findAllByOrderByIdAsc();

    List<BillingAd> findByActiveTrueOrderByIdAsc();
}
