package ch.nveedee.budgetplaner.controller;

import ch.nveedee.budgetplaner.model.Budget;
import ch.nveedee.budgetplaner.model.Konto;
import ch.nveedee.budgetplaner.model.Produkt;
import ch.nveedee.budgetplaner.model.Transaktion;
import ch.nveedee.budgetplaner.repository.BudgetRepository;
import ch.nveedee.budgetplaner.repository.KontoRepository;
import ch.nveedee.budgetplaner.repository.ProduktRepository;
import ch.nveedee.budgetplaner.repository.TransaktionRepository;
import ch.nveedee.budgetplaner.service.OpenFoodFactsService;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/produkte")
@CrossOrigin(origins = "*")
public class ProduktController {

    private final ProduktRepository repo;
    private final KontoRepository kontoRepo;
    private final TransaktionRepository transaktionRepo;
    private final BudgetRepository budgetRepo;
    private final OpenFoodFactsService openFoodFactsService;

    public ProduktController(ProduktRepository repo,
                             KontoRepository kontoRepo, TransaktionRepository transaktionRepo,
                             BudgetRepository budgetRepo, OpenFoodFactsService openFoodFactsService) {
        this.repo = repo;
        this.kontoRepo = kontoRepo;
        this.transaktionRepo = transaktionRepo;
        this.budgetRepo = budgetRepo;
        this.openFoodFactsService = openFoodFactsService;
    }

    @GetMapping
    public List<Produkt> alle() {
        return repo.findAll();
    }

    @GetMapping("/scan/{barcode}")
    public Map<String, Object> scanBarcode(@PathVariable String barcode) {
        Map<String, Object> result = new HashMap<>();
        result.put("barcode", barcode);

        // 1. Lokale DB
        var lokal = repo.findByBarcode(barcode);
        if (lokal.isPresent()) {
            Produkt p = lokal.get();
            result.put("gefunden", true);
            result.put("source", "lokal");
            result.put("name", p.getName());
            result.put("preis", p.getPreis());
            result.put("kategorie", p.getKategorie());
            result.put("bildUrl", p.getBildUrl());
            result.put("gekauftBei", p.getGekauftBei());
            return result;
        }

        // 2. Open Food Facts
        var off = openFoodFactsService.lookup(barcode);
        if (off.isPresent()) {
            var p = off.get();
            result.put("gefunden", true);
            result.put("source", "openfoodfacts");
            result.put("name", p.name);
            result.put("kategorie", p.kategorie);
            result.put("bildUrl", p.bildUrl);
            // Preis wird im Modal manuell eingegeben
            result.put("preis", null);
            return result;
        }

        // Nicht gefunden -> Frontend öffnet Modal zur manuellen Eingabe
        result.put("gefunden", false);
        return result;
    }

    @PostMapping
    public Produkt erstellen(@RequestBody Produkt produkt) {
        if (produkt.getHinzugefuegtAm() == null) {
            produkt.setHinzugefuegtAm(LocalDate.now());
        }
        return repo.save(produkt);
    }

    // Quick-Buy: zählt kaufCount hoch und erzeugt automatisch eine Ausgabe-Transaktion
    @PostMapping("/{id}/kaufen")
    public Produkt kaufen(@PathVariable Long id,
                          @RequestParam(required = false) Long kontoId) {
        Produkt produkt = repo.findById(id).orElseThrow();
        produkt.setKaufCount(produkt.getKaufCount() + 1);
        repo.save(produkt);

        // Konto wählen: übergebenes kontoId, sonst erstes verfügbares
        Konto konto = (kontoId != null) ? kontoRepo.findById(kontoId).orElse(null) : null;
        if (konto == null) {
            konto = kontoRepo.findAll().stream().findFirst().orElse(null);
        }

        // Passendes Budget anhand der Produktkategorie verknüpfen,
        // damit der Kauf im Budget-Tracking gezählt wird
        Budget budget = (produkt.getKategorie() != null)
            ? budgetRepo.findFirstByKategorie(produkt.getKategorie()).orElse(null)
            : null;

        Transaktion t = new Transaktion();
        t.setBeschreibung(produkt.getName());
        t.setBetrag(produkt.getPreis() != null ? produkt.getPreis().negate() : BigDecimal.ZERO);
        t.setDatum(LocalDate.now());
        t.setKategorie(produkt.getKategorie());
        t.setKonto(konto);
        t.setBudget(budget);
        transaktionRepo.save(t);

        // Kontostand atomar um den Produktpreis reduzieren (vermeidet Lost-Updates)
        if (konto != null && produkt.getPreis() != null) {
            kontoRepo.subtractFromKontostand(konto.getId(), produkt.getPreis());
        }

        return produkt;
    }

    @PutMapping("/{id}")
    public Produkt aktualisieren(@PathVariable Long id, @RequestBody Produkt produkt) {
        produkt.setId(id);
        return repo.save(produkt);
    }

    @DeleteMapping("/{id}")
    public void loeschen(@PathVariable Long id) {
        repo.deleteById(id);
    }
}
