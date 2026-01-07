package ch.redmoon.unchain.scheduler;

import ch.redmoon.unchain.entity.FeatureEntity;
import ch.redmoon.unchain.repository.FeatureMetricRepository;
import ch.redmoon.unchain.repository.FeatureRepository;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class FeatureStalenessScheduler {

    private final FeatureRepository featureRepository;
    private final FeatureMetricRepository metricsRepository;
    private final int staleAfterDays;

    public FeatureStalenessScheduler(
            FeatureRepository featureRepository,
            FeatureMetricRepository metricsRepository,
            @Value("${unchain.features.stale-after-days:3}") int staleAfterDays) {
        this.featureRepository = featureRepository;
        this.metricsRepository = metricsRepository;
        this.staleAfterDays = staleAfterDays;
    }

    @Scheduled(cron = "${unchain.features.stale-check-cron:0 0 3 * * *}") // Default: daily at 3 AM
    @SchedulerLock(name = "FeatureStalenessScheduler_checkStaleness", lockAtMostFor = "1h", lockAtLeastFor = "5m")
    @Transactional
    public void checkStaleness() {
        log.info("Starting staleness check: identifying features not used for {} days", staleAfterDays);

        List<FeatureEntity> features = featureRepository.findAll();
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(staleAfterDays);

        int markedStale = 0;
        int markedActive = 0;

        for (FeatureEntity feature : features) {
            Optional<OffsetDateTime> lastUsed = metricsRepository.findLastReportedAt(feature.getProject().getId(),
                    feature.getName());

            boolean isCurrentlyStale = feature.isStale();
            boolean shouldBeStale;

            if (lastUsed.isPresent()) {
                shouldBeStale = lastUsed.get().isBefore(threshold);
            } else {
                // Never used. Check if it's old enough to be considered stale.
                shouldBeStale = feature.getCreatedAt().isBefore(threshold);
            }

            if (shouldBeStale && !isCurrentlyStale) {
                feature.setStale(true);
                featureRepository.save(feature);
                markedStale++;
                log.info("Feature '{}' in project '{}' marked as STALE", feature.getName(),
                        feature.getProject().getId());
            } else if (!shouldBeStale && isCurrentlyStale) {
                feature.setStale(false);
                featureRepository.save(feature);
                markedActive++;
                log.info("Feature '{}' in project '{}' marked as ACTIVE", feature.getName(),
                        feature.getProject().getId());
            }
        }

        log.info("Staleness check finished: {} marked stale, {} marked active", markedStale, markedActive);
    }
}
