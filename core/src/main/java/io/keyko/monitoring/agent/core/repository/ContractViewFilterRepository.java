package io.keyko.monitoring.agent.core.repository;

import io.keyko.monitoring.agent.core.dto.event.filter.ContractViewFilter;
import io.keyko.monitoring.agent.core.factory.ContractViewFilterRepositoryFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring repository for storing active ContractViewFilters in DB.
 *
 */
@Repository
@ConditionalOnMissingBean(ContractViewFilterRepositoryFactory.class)
public interface ContractViewFilterRepository extends CrudRepository<ContractViewFilter, String> {
}
