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

    /** Konten, deren UserId, Kennung (E-Mail/Handle) oder Name {@code part} enthaelt. */
    @Query("""
            select a from BillingAccount a
            where lower(a.userId) like lower(concat('%', :part, '%'))
               or lower(coalesce(a.label, '')) like lower(concat('%', :part, '%'))
               or lower(coalesce(a.displayName, '')) like lower(concat('%', :part, '%'))
            order by a.userId""")
    List<BillingAccount> search(String part, Pageable pageable);

    List<BillingAccount> findByLabelIgnoreCase(String label);
}
