package ch.redmoon.unchain.repository;

import ch.redmoon.unchain.entity.FeatureTagEntity;
import ch.redmoon.unchain.entity.FeatureTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeatureTagRepository extends JpaRepository<FeatureTagEntity, FeatureTagId> {
}
