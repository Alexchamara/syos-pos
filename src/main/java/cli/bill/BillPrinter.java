package main.java.cli.bill;

import main.java.domain.billing.Bill;
import main.java.application.usecase.QuoteUseCase;
import java.time.format.DateTimeFormatter;

public final class BillPrinter {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static void print(Bill b){
        System.out.println("\n----------- BILL -----------");
        System.out.println("Serial: " + b.serial());
        System.out.println("Date  : " + b.dateTime().format(DF));
        b.lines().forEach(l ->
                System.out.printf("%-10s x%-3d @ Rs.%s = Rs.%s%n",
                        l.productCode().value(), l.qty().value(),
                        l.unitPrice().amount().toPlainString(),
                        l.lineTotal().amount().toPlainString()));
        System.out.println("----------------------------");
        System.out.println("TOTAL   : Rs." + b.total().amount().toPlainString());
        System.out.println("CASH    : Rs." + b.cash().amount().toPlainString());
        System.out.println("CHANGE  : Rs." + b.change().amount().toPlainString());
        System.out.println("----------------------------\n");
    }

    public static void printPreview(QuoteUseCase.Quote q) {
        System.out.println("\n-------- PRE-BILL (PREVIEW) --------");
        q.lines().forEach(l ->
                System.out.printf("%-10s x%-3d @ Rs.%s  = Rs.%s%n",
                        l.productCode().value(),
                        l.qty().value(),
                        l.unitPrice().amount().toPlainString(),
                        l.lineTotal().amount().toPlainString()));
        System.out.println("------------------------------------");
        System.out.println("Subtotal : Rs." + q.subtotal().amount().toPlainString());
        System.out.println("Discount : Rs." + q.discount().amount().toPlainString());
        System.out.println("TOTAL    : Rs." + q.total().amount().toPlainString());
        System.out.println("(Pay this amount or more to proceed)");
        System.out.println("------------------------------------\n");
    }
}