package ch.nveedee.budgetplaner.config;

import ch.nveedee.budgetplaner.model.Budget;
import ch.nveedee.budgetplaner.model.Dauerauftrag;
import ch.nveedee.budgetplaner.model.Konto;
import ch.nveedee.budgetplaner.model.Produkt;
import ch.nveedee.budgetplaner.model.Transaktion;
import ch.nveedee.budgetplaner.repository.BudgetRepository;
import ch.nveedee.budgetplaner.repository.DauerauftragRepository;
import ch.nveedee.budgetplaner.repository.KontoRepository;
import ch.nveedee.budgetplaner.repository.ProduktRepository;
import ch.nveedee.budgetplaner.repository.TransaktionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lädt Testdaten beim Start — aber nur, wenn die Datenbank leer ist.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private final KontoRepository kontoRepo;
    private final DauerauftragRepository dauerauftragRepo;
    private final BudgetRepository budgetRepo;
    private final TransaktionRepository transaktionRepo;
    private final ProduktRepository produktRepo;

    public DataLoader(KontoRepository kontoRepo,
                      DauerauftragRepository dauerauftragRepo,
                      BudgetRepository budgetRepo,
                      TransaktionRepository transaktionRepo,
                      ProduktRepository produktRepo) {
        this.kontoRepo = kontoRepo;
        this.dauerauftragRepo = dauerauftragRepo;
        this.budgetRepo = budgetRepo;
        this.transaktionRepo = transaktionRepo;
        this.produktRepo = produktRepo;
    }

    @Override
    public void run(String... args) {
        if (kontoRepo.count() > 0) return; // nur bei leerer DB befüllen

        // ===== KONTEN =====
        Konto ubs  = konto("UBS Privatkonto", "2340.50", "#007AFF");
        Konto spar = konto("Sparkonto",       "8200.00", "#34C759");
        Konto bar  = konto("Bargeld",            "85.00", "#FF9500");

        // ===== DAUERAUFTRÄGE =====
        dauerauftrag("Spotify",       "12.90",  5, ubs);
        dauerauftrag("Handyabo Salt", "19.90",  1, ubs);
        dauerauftrag("Netflix",       "17.90", 15, ubs);
        dauerauftrag("Fitnesscenter", "49.00",  1, ubs);

        // ===== BUDGETS =====
        Budget bLeben     = budget("Lebensmittel",       "400.00", "🛒", "#34C759");
        Budget bAusgang   = budget("Ausgang & Freizeit", "200.00", "🎉", "#FF9500");
        Budget bKleidung  = budget("Kleidung",           "150.00", "👕", "#007AFF");
        Budget bTransport = budget("Transport",          "100.00", "🚌", "#AF52DE");

        // ===== TRANSAKTIONEN (letzte 3 Monate) =====
        transaktion("Lohn Oktober",   "3200.00", ubs, null,       "Lohn",               LocalDate.of(2026, 10, 25));
        transaktion("Migros",          "-67.40", ubs, bLeben,     "Lebensmittel",       LocalDate.of(2026, 10, 28));
        transaktion("Zalando",         "-89.90", ubs, bKleidung,  "Kleidung",           LocalDate.of(2026, 10, 15));
        transaktion("Ausgang Zürich",  "-54.00", bar, bAusgang,   "Ausgang & Freizeit", LocalDate.of(2026, 10, 20));
        transaktion("Lohn November",  "3200.00", ubs, null,       "Lohn",               LocalDate.of(2026, 11, 25));
        transaktion("Migros",          "-91.20", ubs, bLeben,     "Lebensmittel",       LocalDate.of(2026, 11, 3));
        transaktion("Coop",            "-43.80", ubs, bLeben,     "Lebensmittel",       LocalDate.of(2026, 11, 10));
        transaktion("ZVV Monatsabo",   "-88.00", ubs, bTransport, "Transport",          LocalDate.of(2026, 11, 1));
        transaktion("Twint von Lukas",  "40.00", ubs, null,       "Twint",              LocalDate.of(2026, 11, 8));
        transaktion("Pull&Bear",       "-59.90", ubs, bKleidung,  "Kleidung",           LocalDate.of(2026, 11, 14));
        transaktion("Bar Ausgang",     "-78.00", bar, bAusgang,   "Ausgang & Freizeit", LocalDate.of(2026, 11, 16));
        transaktion("Lohn Dezember",  "3200.00", ubs, null,       "Lohn",               LocalDate.of(2026, 12, 25));
        transaktion("Migros",          "-55.30", ubs, bLeben,     "Lebensmittel",       LocalDate.of(2026, 12, 2));

        // ===== PRODUKTE =====
        produkt("Migros Vollmilch 1L", "1.65",  "Lebensmittel",  "7610200070703", 12);
        produkt("Red Bull 250ml",      "1.95",  "Getränke",      "9002490100070",  8);
        produkt("Lindt Schokolade",    "2.90",  "Lebensmittel",  "4003990012024",  5);
        produkt("Gillette Rasierer",  "12.90",  "Körperpflege",  "7702018088843",  3);
        produkt("ZVV Einzelticket",    "4.40",  "Transport",     "",              15);
    }

    // ===== Helper =====
    private Konto konto(String name, String stand, String farbe) {
        Konto k = new Konto();
        k.setName(name);
        k.setKontostand(new BigDecimal(stand));
        k.setFarbe(farbe);
        return kontoRepo.save(k);
    }

    private Dauerauftrag dauerauftrag(String beschreibung, String betrag, int tag, Konto vonKonto) {
        Dauerauftrag d = new Dauerauftrag();
        d.setBeschreibung(beschreibung);
        d.setBetrag(new BigDecimal(betrag));
        d.setTagImMonat(tag);
        d.setVonKonto(vonKonto);
        return dauerauftragRepo.save(d);
    }

    private Budget budget(String kategorie, String limit, String icon, String farbe) {
        Budget b = new Budget();
        b.setKategorie(kategorie);
        b.setLimitProMonat(new BigDecimal(limit));
        b.setIcon(icon);
        b.setFarbe(farbe);
        return budgetRepo.save(b);
    }

    private Transaktion transaktion(String beschreibung, String betrag, Konto konto,
                                    Budget budget, String kategorie, LocalDate datum) {
        Transaktion t = new Transaktion();
        t.setBeschreibung(beschreibung);
        t.setBetrag(new BigDecimal(betrag));
        t.setKonto(konto);
        t.setBudget(budget);
        t.setKategorie(kategorie);
        t.setDatum(datum);
        return transaktionRepo.save(t);
    }

    private Produkt produkt(String name, String preis, String kategorie, String barcode, int kaufCount) {
        Produkt p = new Produkt();
        p.setName(name);
        p.setPreis(new BigDecimal(preis));
        p.setKategorie(kategorie);
        p.setBarcode(barcode);
        p.setKaufCount(kaufCount);
        p.setHinzugefuegtAm(LocalDate.now());
        return produktRepo.save(p);
    }
}
