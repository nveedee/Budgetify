package ch.nveedee.budgetplaner.controller;

import ch.nveedee.budgetplaner.model.Konto;
import ch.nveedee.budgetplaner.repository.KontoRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/konten")
@CrossOrigin(origins = "*")
public class KontoController {

    private final KontoRepository repo;

    public KontoController(KontoRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Konto> alle() {
        return repo.findAll();
    }

    @PostMapping
    public Konto erstellen(@RequestBody Konto konto) {
        return repo.save(konto);
    }

    @PutMapping("/{id}")
    public Konto aktualisieren(@PathVariable Long id, @RequestBody Konto konto) {
        konto.setId(id);
        return repo.save(konto);
    }

    @DeleteMapping("/{id}")
    public void loeschen(@PathVariable Long id) {
        repo.deleteById(id);
    }
}
