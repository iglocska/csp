package com.intrasoft.csp.anon.server.repository;

import com.intrasoft.csp.anon.server.model.RuleSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by chris on 10/7/2017.
 */
@Repository
public interface RuleSetRepository extends JpaRepository<RuleSet, Long> {


}
