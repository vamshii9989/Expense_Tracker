package com.expensetracker.db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoDBConnection {

    private static MongoClient mongoClient;
    private static MongoDatabase database;

    public static MongoDatabase getDatabase() {
        if (database == null) {
            String mongoUri = System.getenv("MONGO_URI");
            if (mongoUri == null || mongoUri.isEmpty()) {
                mongoUri = "mongodb://localhost:27017";
            }

            String dbName = System.getenv("MONGO_DB_NAME");
            if (dbName == null || dbName.isEmpty()) {
                dbName = "expense_tracker_db";
            }

            System.out.println("Connecting to MongoDB at: " + mongoUri + " (db: " + dbName + ")");
            mongoClient = MongoClients.create(mongoUri);
            database = mongoClient.getDatabase(dbName);
        }
        return database;
    }

    public static MongoCollection<Document> getExpenseCollection() {
        return getDatabase().getCollection("expenses");
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
