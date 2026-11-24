package ch.nveedee.budgetplaner.repository;

import ch.nveedee.budgetplaner.model.Produkt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ProduktRepository extends JpaRepository<Produkt, Long> {
    Optional<Produkt> findByBarcode(String barcode);
}
