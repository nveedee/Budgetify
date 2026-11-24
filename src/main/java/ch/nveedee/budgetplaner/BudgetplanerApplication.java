package ch.nveedee.budgetplaner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BudgetplanerApplication {
    public static void main(String[] args) {
        // Lokale Entwicklung (IntelliJ): VM-Option setzen -> -Dspring.profiles.active=local
        // Produktion (Railway): kein Profil -> application.properties (PostgreSQL) greift
        SpringApplication.run(BudgetplanerApplication.class, args);
    }
}
