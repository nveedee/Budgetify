package ch.nveedee.budgetplaner.repository;

import ch.nveedee.budgetplaner.model.Transaktion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface TransaktionRepository extends JpaRepository<Transaktion, Long> {
    List<Transaktion> findByDatumBetween(LocalDate von, LocalDate bis);
    List<Transaktion> findByKonto_Id(Long kontoId);

    @Query("SELECT t FROM Transaktion t WHERE t.budget.id = :budgetId AND t.datum BETWEEN :von AND :bis")
    List<Transaktion> findByBudgetAndMonat(Long budgetId, LocalDate von, LocalDate bis);
}
