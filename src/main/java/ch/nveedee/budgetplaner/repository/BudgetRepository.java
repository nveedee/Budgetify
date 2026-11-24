package ch.nveedee.budgetplaner.repository;

import ch.nveedee.budgetplaner.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    // Budget zu einer Kategorie finden (z.B. Produkt-Kategorie "Transport")
    Optional<Budget> findFirstByKategorie(String kategorie);
}
