package com.expensetracker;

import com.expensetracker.db.MongoDBConnection;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   MONTHLY EXPENSE TRACKER - Dockerized Java App");
        System.out.println("=================================================");

        MongoCollection<Document> expenses = MongoDBConnection.getExpenseCollection();
        System.out.println("Connected to MongoDB successfully.\n");

        boolean running = true;
        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    addExpense(expenses);
                    break;
                case "2":
                    viewAllExpenses(expenses);
                    break;
                case "3":
                    viewMonthlyTotal(expenses);
                    break;
                case "4":
                    viewCategorySummary(expenses);
                    break;
                case "5":
                    deleteExpense(expenses);
                    break;
                case "6":
                    running = false;
                    System.out.println("Closing connection and exiting. Bye!");
                    MongoDBConnection.close();
                    break;
                default:
                    System.out.println("Invalid choice. Please enter a number between 1 and 6.\n");
            }
        }
        scanner.close();
    }

    private static void printMenu() {
        System.out.println("\n--------- MENU ---------");
        System.out.println("1. Add Expense");
        System.out.println("2. View All Expenses");
        System.out.println("3. View Monthly Total");
        System.out.println("4. View Category-wise Summary");
        System.out.println("5. Delete Expense");
        System.out.println("6. Exit");
        System.out.print("Enter your choice: ");
    }

    private static void addExpense(MongoCollection<Document> expenses) {
        try {
            System.out.print("Enter category (e.g. Food, Rent, Travel): ");
            String category = scanner.nextLine().trim();

            System.out.print("Enter amount: ");
            double amount = Double.parseDouble(scanner.nextLine().trim());

            System.out.print("Enter description: ");
            String description = scanner.nextLine().trim();

            System.out.print("Enter date (yyyy-MM-dd) or press Enter for today: ");
            String dateInput = scanner.nextLine().trim();
            LocalDate date = dateInput.isEmpty()
                    ? LocalDate.now()
                    : LocalDate.parse(dateInput, DateTimeFormatter.ISO_LOCAL_DATE);

            Document doc = new Document("category", category)
                    .append("amount", amount)
                    .append("description", description)
                    .append("date", date.toString())
                    .append("month", date.getMonthValue())
                    .append("year", date.getYear());

            expenses.insertOne(doc);
            System.out.println("Expense added successfully with ID: " + doc.getObjectId("_id"));

        } catch (NumberFormatException e) {
            System.out.println("Invalid amount entered. Please enter a numeric value.");
        } catch (DateTimeParseException e) {
            System.out.println("Invalid date format. Please use yyyy-MM-dd.");
        }
    }

    private static void viewAllExpenses(MongoCollection<Document> expenses) {
        System.out.println("\n--- All Expenses ---");
        double grandTotal = 0;
        int count = 0;

        try (MongoCursor<Document> cursor = expenses.find().iterator()) {
            if (!cursor.hasNext()) {
                System.out.println("No expenses recorded yet.");
                return;
            }
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                count++;
                double amt = doc.getDouble("amount");
                grandTotal += amt;
                System.out.printf("[%d] %s | %s | %.2f | %s | %s%n",
                        count,
                        doc.getObjectId("_id").toString(),
                        doc.getString("date"),
                        amt,
                        doc.getString("category"),
                        doc.getString("description"));
            }
        }
        System.out.printf("Total of all expenses: %.2f%n", grandTotal);
    }

    private static void viewMonthlyTotal(MongoCollection<Document> expenses) {
        try {
            System.out.print("Enter month (1-12): ");
            int month = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Enter year (e.g. 2026): ");
            int year = Integer.parseInt(scanner.nextLine().trim());

            double total = 0;
            int count = 0;

            try (MongoCursor<Document> cursor = expenses.find(
                    Filters.and(Filters.eq("month", month), Filters.eq("year", year))
            ).iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    total += doc.getDouble("amount");
                    count++;
                }
            }

            System.out.printf("Total expenses for %d/%d: %.2f (across %d entries)%n", month, year, total, count);

        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter numeric values for month and year.");
        }
    }

    private static void viewCategorySummary(MongoCollection<Document> expenses) {
        System.out.println("\n--- Category-wise Summary ---");
        Map<String, Double> categoryTotals = new LinkedHashMap<>();

        try (MongoCursor<Document> cursor = expenses.find().iterator()) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                String category = doc.getString("category");
                double amt = doc.getDouble("amount");
                categoryTotals.merge(category, amt, Double::sum);
            }
        }

        if (categoryTotals.isEmpty()) {
            System.out.println("No expenses recorded yet.");
            return;
        }

        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            System.out.printf("%-15s : %.2f%n", entry.getKey(), entry.getValue());
        }
    }

    private static void deleteExpense(MongoCollection<Document> expenses) {
        System.out.print("Enter the Expense ID to delete: ");
        String idStr = scanner.nextLine().trim();
        try {
            ObjectId id = new ObjectId(idStr);
            long deletedCount = expenses.deleteOne(Filters.eq("_id", id)).getDeletedCount();
            System.out.println(deletedCount > 0
                    ? "Expense deleted successfully."
                    : "No expense found with that ID.");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid ID format.");
        }
    }
}
