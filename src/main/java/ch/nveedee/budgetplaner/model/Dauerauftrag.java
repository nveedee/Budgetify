package ch.nveedee.budgetplaner.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
public class Dauerauftrag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String beschreibung; // z.B. "Handyabo", "Miete"
    private BigDecimal betrag;
    private int tagImMonat; // z.B. 1 = jeweils am 1. des Monats
    private String kategorie;

    @ManyToOne
    private Konto vonKonto;
}
