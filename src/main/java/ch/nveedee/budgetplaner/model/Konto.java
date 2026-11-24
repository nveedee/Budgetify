package ch.nveedee.budgetplaner.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Data
@NoArgsConstructor
public class Konto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // z.B. "UBS Privat", "Sparkonto", "Bargeld"
    private BigDecimal kontostand = BigDecimal.ZERO;
    private String iban;
    private String farbe; // für UI (#FF5733 etc.)
}
