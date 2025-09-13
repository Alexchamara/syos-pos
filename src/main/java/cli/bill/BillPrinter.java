package main.java.cli.bill;

import main.java.domain.billing.Bill;
import java.time.format.DateTimeFormatter;

public final class BillPrinter {
    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static void print(Bill b){
        System.out.println("\n----------- BILL -----------");
        System.out.println("Serial: " + b.serial());
        System.out.println("Date  : " + b.dateTime().format(DF));
        b.lines().forEach(l ->
                System.out.printf("%-10s x%-3d @ %s = %s%n",
                        l.productCode().value(), l.qty().value(),
                        l.unitPrice().amount().toPlainString(),
                        l.lineTotal().amount().toPlainString()));
        System.out.println("----------------------------");
        System.out.println("TOTAL   : " + b.total().amount().toPlainString());
        System.out.println("CASH    : " + b.cash().amount().toPlainString());
        System.out.println("CHANGE  : " + b.change().amount().toPlainString());
        System.out.println("----------------------------\n");
    }
}