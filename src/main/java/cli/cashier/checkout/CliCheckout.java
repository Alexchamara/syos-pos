package main.java.cli.cashier.checkout;

import main.java.application.usecase.CheckoutCashUseCase;
import main.java.application.usecase.CheckoutCashUseCase.Item;
import main.java.cli.bill.BillPrinter;
import main.java.domain.inventory.StockLocation;
import main.java.domain.policies.BatchSelectionStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class CliCheckout {
    private final CheckoutCashUseCase checkout;
    private final BatchSelectionStrategy strategyDefault;

    public CliCheckout(CheckoutCashUseCase checkout, BatchSelectionStrategy strategyDefault) {
        this.checkout = checkout; this.strategyDefault = strategyDefault;
    }

    public void run() {
        Scanner sc = new Scanner(System.in);
        System.out.println("=== SYOS Checkout (CLI) ===");
        List<Item> cart = new ArrayList<>();
        while (true) {
            System.out.print("Item code (or 'done'): ");
            String code = sc.nextLine().trim();
            if (code.equalsIgnoreCase("done")) break;
            System.out.print("Qty: ");
            int qty = Integer.parseInt(sc.nextLine().trim());
            cart.add(new Item(code, qty));
        }
        if (cart.isEmpty()) { System.out.println("Empty cart."); return; }

        System.out.print("Location [SHELF/WEB] (default SHELF): ");
        String locIn = sc.nextLine().trim();
        StockLocation loc = "WEB".equalsIgnoreCase(locIn) ? StockLocation.WEB : StockLocation.SHELF;

        System.out.print("Cash in cents (e.g. 10000 = 100.00): ");
        long cash = Long.parseLong(sc.nextLine().trim());

        try {
            var bill = checkout.handle(cart, cash, loc);
            BillPrinter.print(bill);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}