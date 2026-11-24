package ch.nveedee.budgetplaner.controller;

import ch.nveedee.budgetplaner.model.Dauerauftrag;
import ch.nveedee.budgetplaner.repository.DauerauftragRepository;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/dauerauftraege")
@CrossOrigin(origins = "*")
public class DauerauftragController {

    private final DauerauftragRepository repo;

    public DauerauftragController(DauerauftragRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Dauerauftrag> alle() {
        return repo.findAll();
    }

    @PostMapping
    public Dauerauftrag erstellen(@RequestBody Dauerauftrag d) {
        return repo.save(d);
    }

    @PutMapping("/{id}")
    public Dauerauftrag aktualisieren(@PathVariable Long id, @RequestBody Dauerauftrag d) {
        d.setId(id);
        return repo.save(d);
    }

    @DeleteMapping("/{id}")
    public void loeschen(@PathVariable Long id) {
        repo.deleteById(id);
    }
}
