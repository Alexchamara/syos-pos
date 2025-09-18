package main.java.cli.manager;

import main.java.application.usecase.ReceiveFromSupplierUseCase;

import java.time.LocalDate;
import java.util.Scanner;

public final class ReceiveToMainCLI {
    private final ReceiveFromSupplierUseCase usecase;

    public ReceiveToMainCLI(ReceiveFromSupplierUseCase usecase) { this.usecase = usecase; }

    public void run() {
        var sc = new Scanner(System.in);
        System.out.println("\n== New Batch to MAIN ==");
        System.out.print("Product code: ");
        String code = sc.next().trim();
        System.out.print("Qty: ");
        int qty = sc.nextInt();
        System.out.print("Expiry (YYYY-MM-DD or 'none'): ");
        String exp = sc.next();
        LocalDate expiry = "none".equalsIgnoreCase(exp) ? null : LocalDate.parse(exp);
        long id = usecase.receive(code, qty, expiry);
        System.out.println("Received. New MAIN batch id = " + id);
    }
}