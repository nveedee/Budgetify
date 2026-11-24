package ch.nveedee.budgetplaner.controller;

import ch.nveedee.budgetplaner.model.Budget;
import ch.nveedee.budgetplaner.model.Transaktion;
import ch.nveedee.budgetplaner.repository.BudgetRepository;
import ch.nveedee.budgetplaner.repository.TransaktionRepository;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/budgets")
@CrossOrigin(origins = "*")
public class BudgetController {

    private final BudgetRepository budgetRepo;
    private final TransaktionRepository transaktionRepo;

    public BudgetController(BudgetRepository budgetRepo, TransaktionRepository transaktionRepo) {
        this.budgetRepo = budgetRepo;
        this.transaktionRepo = transaktionRepo;
    }

    @GetMapping
    public List<Budget> alle() {
        return budgetRepo.findAll();
    }

    @GetMapping("/uebersicht/{jahr}/{monat}")
    public List<Map<String, Object>> monatsuebersicht(
            @PathVariable int jahr, @PathVariable int monat) {

        LocalDate von = LocalDate.of(jahr, monat, 1);
        LocalDate bis = von.withDayOfMonth(von.lengthOfMonth());
        List<Budget> budgets = budgetRepo.findAll();

        return budgets.stream().map(budget -> {
            List<Transaktion> transaktionen =
                transaktionRepo.findByBudgetAndMonat(budget.getId(), von, bis);

            BigDecimal ausgegeben = transaktionen.stream()
                .map(Transaktion::getBetrag)
                .filter(b -> b.compareTo(BigDecimal.ZERO) < 0)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> result = new HashMap<>();
            result.put("budget", budget);
            result.put("ausgegeben", ausgegeben);
            result.put("verbleibend", budget.getLimitProMonat().subtract(ausgegeben));
            result.put("prozent", ausgegeben.multiply(BigDecimal.valueOf(100))
                .divide(budget.getLimitProMonat(), 0, java.math.RoundingMode.HALF_UP));
            return result;
        }).toList();
    }

    @PostMapping
    public Budget erstellen(@RequestBody Budget budget) {
        return budgetRepo.save(budget);
    }

    @PutMapping("/{id}")
    public Budget aktualisieren(@PathVariable Long id, @RequestBody Budget budget) {
        budget.setId(id);
        return budgetRepo.save(budget);
    }

    @DeleteMapping("/{id}")
    public void loeschen(@PathVariable Long id) {
        budgetRepo.deleteById(id);
    }
}
