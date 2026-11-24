package ch.nveedee.budgetplaner.repository;

import ch.nveedee.budgetplaner.model.Konto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Repository
public interface KontoRepository extends JpaRepository<Konto, Long> {

    // Atomarer Abzug direkt in der DB: kontostand = kontostand - betrag
    // (vermeidet Lost-Updates bei gleichzeitigen Buchungen)
    @Modifying
    @Transactional
    @Query("UPDATE Konto k SET k.kontostand = k.kontostand - :betrag WHERE k.id = :id")
    void subtractFromKontostand(@Param("id") Long id, @Param("betrag") BigDecimal betrag);
}
