package main.java.cli.cashier.checkout;

import main.java.application.services.AvailabilityService;
import main.java.application.services.ShortageNotifier;
import main.java.application.usecase.CheckoutCashUseCase;
import main.java.application.usecase.CheckoutCashUseCase.Item;
import main.java.application.usecase.QuoteUseCase;
import main.java.cli.bill.BillPrinter;
import main.java.domain.inventory.StockLocation;
import main.java.domain.policies.BatchSelectionStrategy;
import main.java.domain.pricing.*;

import java.util.*;
import java.util.Scanner;

public final class CliCheckout {
    private final CheckoutCashUseCase checkout;
    private final BatchSelectionStrategy strategyDefault;
    private final QuoteUseCase quote;
    private final AvailabilityService availability;

    public CliCheckout(CheckoutCashUseCase checkout,
                       BatchSelectionStrategy strategyDefault,
                       QuoteUseCase quote,
                       AvailabilityService availability) {
        this.checkout = checkout;
        this.strategyDefault = strategyDefault;
        this.quote = quote;
        this.availability = availability;
    }

    public void run() {
        var sc = new Scanner(System.in);
        System.out.println("=== SYOS Checkout (CLI) ===");
        System.out.println("Type product code then quantity. Type 'done' to finish.");

        List<Item> cart = new ArrayList<>();
        while (true) {
            System.out.print("Item code (or 'done'): ");
            String code = sc.next().trim();
            if (code.equalsIgnoreCase("done")) break;
            if (!quote.productExists(code)) {
                System.out.println("Invalid code: " + code);
                continue;
            }
            System.out.print("Qty: ");
            int qty = sc.nextInt();
            if (qty <= 0) { System.out.println("Qty must be > 0"); continue; }
            cart.add(new Item(code, qty));
        }
        if (cart.isEmpty()) { System.out.println("Cart empty, aborting."); return; }

        StockLocation loc = StockLocation.SHELF;

        var shelfCheck = availability.checkCart(cart, StockLocation.SHELF);
        if (!shelfCheck.allAvailable()) {
            Map<String,Integer> need = new HashMap<>();
            for (var it : cart) need.merge(it.code(), it.qty(), Integer::sum);

            List<AvailabilityService.Shortage> combinedShortages = new ArrayList<>();
            for (var e : need.entrySet()) {
                String code = e.getKey();
                int required = e.getValue();
                int shelfAvail = availability.available(code, StockLocation.SHELF);
                int webAvail = availability.available(code, StockLocation.WEB);
                if (shelfAvail + webAvail < required) {
                    combinedShortages.add(new AvailabilityService.Shortage(code, required, shelfAvail + webAvail));
                }
            }
            if (!combinedShortages.isEmpty()) {
                System.out.println("Unavailable items (even combining SHELF + WEB):");
                for (var s : combinedShortages) {
                    int shelfAvail = availability.available(s.code(), StockLocation.SHELF);
                    int webAvail = availability.available(s.code(), StockLocation.WEB);
                    int missing = s.missing();
                    String msg = String.format("Product %s need %d (SHELF %d + WEB %d => missing %d)",
                            s.code(), s.required(), shelfAvail, webAvail, missing);
                    System.out.println(" - " + msg);
                    ShortageNotifier.record(msg);
                }
                System.out.println("Manager notified. Try again later.");
                return;
            }
            System.out.println("Partial shortage on SHELF. Remainder will auto-fulfill from WEB (FEFO/FIFO strategy).");
        }

        DiscountPolicy policy = new NoDiscount();
        System.out.print("Apply discount? [y/N]: ");
        String d = sc.next();
        if ("y".equalsIgnoreCase(d)) {
            System.out.print("Percent (0..100): ");
            int pct = Math.max(0, Math.min(100, sc.nextInt()));
            policy = new PercentDiscount(pct);
        }

        var q = quote.preview(cart, policy);
        BillPrinter.printPreview(q);

        long cash;
        while (true) {
            System.out.print("Cash (cents, e.g., 10000 = Rs.100.00): ");
            cash = sc.nextLong();
            long need = q.total().amount().movePointRight(2).longValueExact();
            if (cash < need) {
                System.out.println("Insufficient cash. Need at least " + q.total().amount().toPlainString());
                continue;
            }
            break;
        }

        System.out.print("Confirm checkout? [y/N]: ");
        if (!"y".equalsIgnoreCase(sc.next())) { System.out.println("Cancelled."); return; }

        try {
            var bill = checkout.handle(cart, cash, loc, policy, "COUNTER");
            BillPrinter.print(bill);
        } catch (Exception e) {
            System.out.println("Checkout failed: " + e.getMessage());
        }
    }
}