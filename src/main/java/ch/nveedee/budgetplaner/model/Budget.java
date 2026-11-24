package ch.nveedee.budgetplaner.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
public class Budget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String kategorie; // z.B. "Lebensmittel", "Ausgang", "Kleidung"
    private BigDecimal limitProMonat;
    private String farbe; // für UI
    private String icon; // emoji z.B. "🛒"
}
