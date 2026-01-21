package com.example.clouddoc;

import com.intersystems.document.BulkResponse;
import com.intersystems.document.Collection;
import com.intersystems.document.Cursor;
import com.intersystems.document.DataSource;
import com.intersystems.document.Document;
import com.intersystems.document.JSONArray;
import com.intersystems.document.JSONObject;
import com.intersystems.document.ShorthandQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal end-to-end demo for InterSystems IRIS Cloud Document using the Java driver.
 *
 * This version is environment-variable driven so it can be run easily in Docker.
 *
 * Environment variables (all optional, but strongly recommended in Docker):
 *   IRIS_HOST       - Cloud Document hostname
 *   IRIS_PORT       - Superserver port (e.g. 443)
 *   IRIS_NAMESPACE  - Namespace (e.g. USER)
 *   IRIS_USER       - SQL user (e.g. SQLAdmin)
 *   IRIS_PASSWORD   - Password for the SQL user
 *   COLLECTION_NAME - Name of the document collection to use
 */
public class CloudDocDemo {

    // Default values (used if environment variables are not set)
    private static final String DEFAULT_SERVER_NAME = "your-hostname.elb.us-east-1.amazonaws.com";
    private static final int    DEFAULT_PORT        = 443;
    private static final String DEFAULT_NAMESPACE   = "USER";
    private static final String DEFAULT_USER        = "SQLAdmin";
    private static final String DEFAULT_PASSWORD    = "your-deployment-password";
    private static final String DEFAULT_COLLECTION  = "demoPeople";

    private static String envOrDefault(String envName, String defaultValue) {
        String value = System.getenv(envName);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    private static int intEnvOrDefault(String envName, int defaultValue) {
        String value = System.getenv(envName);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            System.err.println("WARNING: Environment variable " + envName +
                               " has invalid integer value '" + value +
                               "', using default " + defaultValue);
            return defaultValue;
        }
    }

    public static void main(String[] args) {
        // Resolve configuration from environment with sensible defaults
        String serverName    = envOrDefault("IRIS_HOST",      DEFAULT_SERVER_NAME);
        int    port          = intEnvOrDefault("IRIS_PORT",   DEFAULT_PORT);
        String namespace     = envOrDefault("IRIS_NAMESPACE", DEFAULT_NAMESPACE);
        String user          = envOrDefault("IRIS_USER",      DEFAULT_USER);
        String password      = envOrDefault("IRIS_PASSWORD",  DEFAULT_PASSWORD);
        String collectionName= envOrDefault("COLLECTION_NAME",DEFAULT_COLLECTION);

        DataSource pool = null;

        System.out.println("=== InterSystems IRIS Cloud Document Java demo (Docker-ready) ===");
        System.out.println("Server     : " + serverName + ":" + port);
        System.out.println("Namespace  : " + namespace);
        System.out.println("Collection : " + collectionName);
        System.out.println("User       : " + user);
        System.out.println();

        try {
            // 1. Create and configure the DataSource (connection pool)
            pool = DataSource.createDataSource();
            pool.setServerName(serverName);
            pool.setPortNumber(port);
            pool.setDatabaseName(namespace);
            pool.setUser(user);
            pool.setPassword(password);

            // Require TLS – connectionSecurityLevel 10 enables TLS.
            pool.setConnectionSecurityLevel(10);

            System.out.println("Initializing connection pool...");
            pool.preStart(5);
            pool.getConnection();  // force pool creation
            System.out.println("Connection pool created. Current pool size: " + pool.getSize());

            // 2. Get (or create) the collection
            Collection people = Collection.getCollection(pool, collectionName);
            if (people.size() > 0) {
                System.out.println("\nCollection '" + people.getName() + "' already has "
                                   + people.size() + " documents. Dropping them for a clean demo...");
                people.drop();
            }
            System.out.println("Using collection: " + people.getName());

            // 3. Insert a very simple array document
            Document docOne = new JSONArray()
                    .add("Hello from Cloud Document (Docker demo)");

            String id1 = people.insert(docOne);
            System.out.println("\nInserted docOne (JSONArray) with id " + id1);

            // 4. Insert a JSONObject document
            Document docTwo = new JSONObject()
                    .put("name", "John Doe")
                    .put("age", 42)
                    .put("city", "Boston");

            String id2 = people.insert(docTwo);
            System.out.println("Inserted docTwo (JSONObject) with id " + id2);

            // 5. Bulk insert of multiple JSONObject documents
            List<Document> batch = new ArrayList<>();
            batch.add(new JSONObject()
                    .put("name", "Jane Doe")
                    .put("age", 20)
                    .put("city", "Seattle"));
            batch.add(new JSONObject()
                    .put("name", "Anne Elk")
                    .put("age", 38)
                    .put("city", "London"));

            BulkResponse bulk = people.insert(batch);
            System.out.println("Bulk insert completed. New ids: " + bulk.getIds());

            // 6. Retrieve and display all documents in the collection
            System.out.println("\nAll documents in collection '" + collectionName + "':");
            List<Document> allDocuments = people.getAll();
            for (Document d : allDocuments) {
                System.out.println("  " + d.getID() + ": " + d.toJSONString());
            }
            System.out.println("Collection size reported by server: " + people.size());

            // 7. Run a shorthand query
            String shorthand = "name > 'H' AND age >= 21";
            System.out.println("\nRunning shorthand query: " + shorthand);

            ShorthandQuery query = people.createShorthandQuery(shorthand);
            Cursor results = query.execute();

            System.out.println("Shorthand query returned " + results.count() + " result(s).");
            while (results.hasNext()) {
                Document d = results.next();
                System.out.println("  " + d.toJSONString());
            }

            System.out.println("\nDemo completed successfully.");

        } catch (Exception e) {
            System.err.println("\n*** Error during Cloud Document demo ***");
            e.printStackTrace(System.err);
        } finally {
            if (pool != null) {
                try {
                    pool.close();
                    System.out.println("Connection pool closed.");
                } catch (Exception ignore) {
                    // no-op
                }
            }
        }
    }
}
