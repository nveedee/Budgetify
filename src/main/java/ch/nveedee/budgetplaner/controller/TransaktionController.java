package ch.nveedee.budgetplaner.controller;

import ch.nveedee.budgetplaner.model.Transaktion;
import ch.nveedee.budgetplaner.repository.KontoRepository;
import ch.nveedee.budgetplaner.repository.TransaktionRepository;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transaktionen")
@CrossOrigin(origins = "*")
public class TransaktionController {

    private final TransaktionRepository repo;
    private final KontoRepository kontoRepo;

    public TransaktionController(TransaktionRepository repo, KontoRepository kontoRepo) {
        this.repo = repo;
        this.kontoRepo = kontoRepo;
    }

    @GetMapping
    public List<Transaktion> alle() {
        return repo.findAll();
    }

    @GetMapping("/monat/{jahr}/{monat}")
    public List<Transaktion> nachMonat(@PathVariable int jahr, @PathVariable int monat) {
        LocalDate von = LocalDate.of(jahr, monat, 1);
        LocalDate bis = von.withDayOfMonth(von.lengthOfMonth());
        return repo.findByDatumBetween(von, bis);
    }

    @PostMapping
    public Transaktion erstellen(@RequestBody Transaktion t) {
        if (t.getDatum() == null) t.setDatum(LocalDate.now());
        Transaktion gespeichert = repo.save(t);

        // Kontostand atomar anpassen:
        // betrag ist bei Ausgaben negativ -> negate() -> kontostand - (-betrag) = kontostand + betrag
        if (gespeichert.getKonto() != null
                && gespeichert.getKonto().getId() != null
                && gespeichert.getBetrag() != null) {
            kontoRepo.subtractFromKontostand(
                gespeichert.getKonto().getId(),
                gespeichert.getBetrag().negate()
            );
        }
        return gespeichert;
    }

    @DeleteMapping("/{id}")
    public void loeschen(@PathVariable Long id) {
        repo.deleteById(id);
    }
}
