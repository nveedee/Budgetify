package ch.nveedee.budgetplaner.repository;

import ch.nveedee.budgetplaner.model.Dauerauftrag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DauerauftragRepository extends JpaRepository<Dauerauftrag, Long> {}
