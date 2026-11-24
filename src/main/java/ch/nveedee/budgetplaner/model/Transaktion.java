package ch.nveedee.budgetplaner.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
public class Transaktion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String beschreibung; // z.B. "Lohn November", "Twint von Lukas"
    private BigDecimal betrag; // positiv = Eingang, negativ = Ausgabe
    private LocalDate datum;
    private String kategorie; // z.B. "Lohn", "Twint", "Einkauf"

    @ManyToOne
    private Konto konto;

    @ManyToOne
    private Budget budget; // optional, falls einer Kategorie zugeordnet
}
