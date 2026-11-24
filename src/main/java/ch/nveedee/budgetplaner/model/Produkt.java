package ch.nveedee.budgetplaner.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
public class Produkt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String barcode;
    private BigDecimal preis;
    private String kategorie;
    private LocalDate hinzugefuegtAm;
    private String notiz;
    private String bildUrl; // optional, aus OpenFoodFacts-Suche
    private String gekauftBei; // z.B. "Migros", "Coop" (optional)
    private int kaufCount = 0; // wie oft "quick-bought"
}
