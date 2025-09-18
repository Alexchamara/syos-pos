package main.java;


import main.java.application.usecase.LoginUseCase;
import main.java.application.usecase.ProductManagementUseCase;
import main.java.application.services.BillNumberService;
import main.java.application.services.AvailabilityService;
import main.java.application.services.MainStoreService;
import main.java.application.services.ShortageEventService;
import main.java.application.usecase.QuoteUseCase;
import main.java.application.usecase.ReceiveFromSupplierUseCase;
import main.java.application.usecase.TransferStockUseCase;
import main.java.cli.*;
import main.java.cli.cashier.CashierMenu;
import main.java.cli.cashier.checkout.CliCheckout;
import main.java.cli.demo.ConcurrencyDemo;
import main.java.cli.manager.ManagerMenu;
import main.java.cli.manager.ReceiveToMainCLI;
import main.java.cli.manager.TransferFromMainCLI;
import main.java.cli.manager.product.ProductManagementCLI;
import main.java.cli.signin.LoginScreen;
import main.java.config.Db;
import main.java.domain.policies.FefoStrategy;
import main.java.infrastructure.concurrency.Tx;
import main.java.infrastructure.persistence.*;
import main.java.infrastructure.security.PasswordEncoder;
import main.java.infrastructure.events.SimpleBus;
import main.java.infrastructure.events.LowStockPrinter;

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
            var shortageRepo = new JdbcShortageEventRepository(ds);
            var bus = new SimpleBus();
            bus.subscribe(new LowStockPrinter());

            // Strategy / use cases
            var availabilitySvc = new AvailabilityService(tx, inventory);
            var mainStoreSvc = new MainStoreService(tx, inventory);
            var strategy   = new FefoStrategy(inventory);
            var billNums   = new BillNumberService(tx);
            var shortageSvc = new ShortageEventService(tx, shortageRepo);
            var checkoutUC = new main.java.application.usecase.CheckoutCashUseCase(
                    tx, products, bills, strategy, billNums, inventory, bus);
            var quoteUC    = new QuoteUseCase(products);
            var invAdmin = new JdbcInventoryAdminRepository();
            var receiveUC = new ReceiveFromSupplierUseCase(tx, invAdmin);
            var transferUC = new TransferStockUseCase(tx, inventory, invAdmin, strategy);
            var productManagementUC = new ProductManagementUseCase(products);

            // CLI units
            var receiveCLI  = new ReceiveToMainCLI(receiveUC);
            var transferCLI = new TransferFromMainCLI(transferUC, availabilitySvc, quoteUC, inventory, tx);
            var checkoutCLI = new CliCheckout(checkoutUC, strategy, quoteUC, availabilitySvc, mainStoreSvc, shortageSvc);
            var productManagementCLI = new ProductManagementCLI(productManagementUC);
            var cashierMenu = new CashierMenu(checkoutCLI::run,
                    () -> ConcurrencyDemo.run(checkoutUC), ds);
            var managerMenu = new ManagerMenu(ds, checkoutCLI::run, shortageSvc, receiveCLI::run, transferCLI::run, productManagementCLI);

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