package com.hotel.customer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class CustomerRepository {

    private final String filePath;
    private static final String HEADER = "#customerId|customerType|customerName|nicOrPassport|contactNo|email|address|nationality|dateOfBirth|registeredDate|discount";

    public CustomerRepository(@Value("${app.data.customer-file:data/customers.txt}") String filePath) {
        this.filePath = filePath;
        ensureFileExists();
    }

    private synchronized void ensureFileExists() {
        try {
            File file = new File(filePath);
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8, false))) {
                    writer.write(HEADER);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Could not initialize customer data file: " + e.getMessage());
        }
    }

    // Reads all customers from the text file using BufferedReader.
    public synchronized List<Customer> findAll() {
        File file = new File(filePath);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        List<Customer> customers = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                Customer customer = parseLine(line);
                if (customer != null) {
                    customers.add(customer);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading customer file using BufferedReader: " + e.getMessage());
        }
        return customers;
    }

    // Finds a customer by customerId.
    public Optional<Customer> findById(String customerId) {
        if (customerId == null) {
            return Optional.empty();
        }
        return findAll().stream()
                .filter(c -> customerId.equalsIgnoreCase(c.getCustomerId()))
                .findFirst();
    }

    // Saves a customer (creates new or updates existing) using BufferedWriter.
    public synchronized Customer save(Customer customer) {
        List<Customer> all = findAll();
        boolean updated = false;

        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getCustomerId() != null && all.get(i).getCustomerId().equalsIgnoreCase(customer.getCustomerId())) {
                all.set(i, customer);
                updated = true;
                break;
            }
        }

        if (!updated) {
            all.add(customer);
        }

        writeAll(all);
        return customer;
    }

    // Deletes a customer from the text file.
    public synchronized void delete(Customer customer) {
        if (customer == null || customer.getCustomerId() == null) {
            return;
        }
        String id = customer.getCustomerId();
        List<Customer> filtered = findAll().stream()
                .filter(c -> !id.equalsIgnoreCase(c.getCustomerId()))
                .collect(Collectors.toList());
        writeAll(filtered);
    }

    // Counts customers with the given NIC or passport number.
    public long countByNicOrPassport(String nicOrPassport) {
        if (nicOrPassport == null) return 0;
        return findAll().stream()
                .filter(c -> nicOrPassport.trim().equalsIgnoreCase(c.getNicOrPassport()))
                .count();
    }

    // Search customers by name, NIC/passport, contact number or customerId (case-insensitive).
    public List<Customer> search(String keyword) {
        List<Customer> all = findAll();
        if (keyword == null || keyword.trim().isEmpty()) {
            return all;
        }
        String lower = keyword.trim().toLowerCase();
        return all.stream()
                .filter(c -> (c.getCustomerName() != null && c.getCustomerName().toLowerCase().contains(lower))
                        || (c.getNicOrPassport() != null && c.getNicOrPassport().toLowerCase().contains(lower))
                        || (c.getContactNo() != null && c.getContactNo().contains(lower))
                        || (c.getCustomerId() != null && c.getCustomerId().toLowerCase().contains(lower)))
                .collect(Collectors.toList());
    }

    //Writes all customers to file using BufferedWriter.
    private synchronized void writeAll(List<Customer> customers) {
        ensureFileExists();
        File file = new File(filePath);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8, false))) {
            writer.write(HEADER);
            writer.newLine();
            for (Customer c : customers) {
                writer.write(formatCustomer(c));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error writing customer file using BufferedWriter: " + e.getMessage());
        }
    }

    private String formatCustomer(Customer c) {
        return String.join("|",
                escape(c.getCustomerId()),
                c.getCustomerType(),
                escape(c.getCustomerName()),
                escape(c.getNicOrPassport()),
                escape(c.getContactNo()),
                escape(c.getEmail()),
                escape(c.getAddress()),
                escape(c.getNationality()),
                c.getDateOfBirth() != null ? c.getDateOfBirth().toString() : "",
                c.getRegisteredDate() != null ? c.getRegisteredDate().toString() : "",
                String.valueOf(c.getDiscount())
        );
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\r", "").replace("\n", " ").replace("|", "-").trim();
    }

    private Customer parseLine(String line) {
        try {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 11) {
                return null;
            }

            // The type column decides which subclass to create
            Customer customer = "FOREIGN".equals(parts[1].trim()) ? new ForeignCustomer() : new LocalCustomer();

            customer.setCustomerId(parts[0].trim());
            customer.setCustomerName(parts[2].trim());
            customer.setNicOrPassport(parts[3].trim());
            customer.setContactNo(parts[4].trim());
            customer.setEmail(parts[5].trim().isEmpty() ? null : parts[5].trim());
            customer.setAddress(parts[6].trim().isEmpty() ? null : parts[6].trim());
            customer.setNationality(parts[7].trim());
            if (!parts[8].trim().isEmpty()) {
                customer.setDateOfBirth(LocalDate.parse(parts[8].trim()));
            }
            if (!parts[9].trim().isEmpty()) {
                customer.setRegisteredDate(LocalDate.parse(parts[9].trim()));
            }
            if (!parts[10].trim().isEmpty()) {
                customer.setDiscount(Double.parseDouble(parts[10].trim()));
            }
            return customer;
        } catch (Exception e) {
            System.err.println("Could not parse customer line: '" + line + "': " + e.getMessage());
            return null;
        }
    }
}
