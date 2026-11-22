package de.witchcafe.billing.server.ledger;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BillingEntryRepository extends JpaRepository<BillingEntry, Long> {

    Optional<BillingEntry> findByAppAndIdempotencyKey(String app, String idempotencyKey);

    List<BillingEntry> findByUserIdOrderByCreatedAtDescIdDesc(String userId, Pageable pageable);

    /** Verbrauch und Beitraege seit {@code since}, summiert pro App, Feature und Art. */
    @Query("""
            select new de.witchcafe.billing.server.ledger.UsageSummary(
                       e.app, e.feature, e.kind, count(e), sum(e.quantity), sum(e.credits), count(distinct e.userId))
            from BillingEntry e
            where e.createdAt >= :since
              and e.kind in (de.witchcafe.billing.server.ledger.EntryKind.USAGE,
                             de.witchcafe.billing.server.ledger.EntryKind.CONTRIBUTION)
            group by e.app, e.feature, e.kind
            order by e.app, e.feature, e.kind
            """)
    List<UsageSummary> summarizeUsageSince(Instant since);
}
