package de.witchcafe.billing.server.ledger;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

public interface BillingAccountRepository extends JpaRepository<BillingAccount, String> {

    /** Liest das Konto mit Schreibsperre, damit parallele Buchungen nacheinander laufen. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from BillingAccount a where a.userId = :userId")
    Optional<BillingAccount> findForUpdate(String userId);

    List<BillingAccount> findByUserIdContainingIgnoreCaseOrderByUserIdAsc(String part, Pageable pageable);
}
