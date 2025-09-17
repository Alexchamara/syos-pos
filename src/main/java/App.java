package main.java;


import main.java.application.usecase.LoginUseCase;
import main.java.application.services.BillNumberService;
import main.java.application.services.AvailabilityService;
import main.java.application.services.ShortageEventService;
import main.java.application.usecase.QuoteUseCase;
import main.java.cli.*;
import main.java.cli.cashier.CashierMenu;
import main.java.cli.cashier.checkout.CliCheckout;
import main.java.cli.demo.ConcurrencyDemo;
import main.java.cli.manager.ManagerMenu;
import main.java.cli.signin.LoginScreen;
import main.java.config.Db;
import main.java.domain.policies.FefoStrategy;
import main.java.infrastructure.concurrency.Tx;
import main.java.infrastructure.persistence.*;
import main.java.infrastructure.security.PasswordEncoder;

/**®
 * Main application entry point
 */
public class App {
    public static void main(String[] args) {

        try (var db = new Db()) {
            var ds = db.getDataSource();
            var tx = new Tx(ds);

            // Repos
            var products   = new JdbcProductRepository(ds);
            var bills      = new JdbcBillRepository();
            var inventory  = new JdbcInventoryRepository(ds);
            var users      = new JdbcUserRepository(ds);
            var shortageRepo = new JdbcShortageEventRepository();

            // Strategy / use cases
            var availabilitySvc = new AvailabilityService(tx, inventory);
            var strategy   = new FefoStrategy(inventory);
            var billNums   = new BillNumberService(tx);
            var shortageSvc = new ShortageEventService(tx, shortageRepo);
            var checkoutUC = new main.java.application.usecase.CheckoutCashUseCase(
                    tx, products, bills, strategy, billNums);
            var quoteUC    = new QuoteUseCase(products);

            // CLI units
            var checkoutCLI = new CliCheckout(checkoutUC, strategy, quoteUC, availabilitySvc);
            var cashierMenu = new CashierMenu(checkoutCLI::run,
                    () -> ConcurrencyDemo.run(checkoutUC),
                    ds);
            var managerMenu = new ManagerMenu(ds, checkoutCLI::run, shortageSvc);

            // Auth
            var encoder = new PasswordEncoder();
            var loginUC = new LoginUseCase(users, encoder);
            var login   = new LoginScreen(loginUC);

            // Ensure demo accounts exist (one-time)
            SeedUsers.ensure(users, encoder);

            // Loop: login -> route to menu; when logout, ask for next login
            while (true) {
                var session = login.prompt();
                LoginScreen.route(session, cashierMenu::run, managerMenu::run);
            }
        }
    }
}