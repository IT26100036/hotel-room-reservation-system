package com.hotel.hotelservice;

import com.hotel.hotelservice.enums.RequestStatus;
import com.hotel.hotelservice.enums.ServiceType;
import com.hotel.hotelservice.enums.TripType;
import com.hotel.hotelservice.enums.VehicleType;
import com.hotel.hotelservice.types.FoodRequest;
import com.hotel.hotelservice.types.LaundryRequest;
import com.hotel.hotelservice.types.TransportRequest;
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
public class ServiceRequestRepository {

    private final String filePath;
    private static final String HEADER =
            "#requestId|serviceType|customerId|requestDate|status|notes|d1|d2|d3|d4|d5";

    public ServiceRequestRepository(@Value("${app.data.service-file:data/service_requests.txt}") String filePath) {
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
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
                    writer.write(HEADER);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Could not create service request file: " + e.getMessage());
        }
    }


    public synchronized List<ServiceRequest> findAll() {
        List<ServiceRequest> requests = new ArrayList<>();
        File file = new File(filePath);
        if (!file.exists()) {
            return requests;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;   // skip empty lines and the header
                }
                ServiceRequest request = parseLine(line);
                if (request != null) {
                    requests.add(request);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read service request file: " + e.getMessage());
        }
        return requests;
    }

    public Optional<ServiceRequest> findById(String requestId) {
        if (requestId == null) {
            return Optional.empty();
        }
        return findAll().stream()
                .filter(r -> requestId.trim().equalsIgnoreCase(r.getRequestId()))
                .findFirst();
    }

    /** Requests whose request ID, customer ID or service type contains the keyword */
    public List<ServiceRequest> search(String keyword) {
        String k = keyword.trim().toLowerCase();
        return findAll().stream()
                .filter(r -> r.getRequestId().toLowerCase().contains(k)
                        || r.getCustomerId().toLowerCase().contains(k)
                        || r.getServiceType().name().toLowerCase().contains(k))
                .collect(Collectors.toList());
    }

    public List<ServiceRequest> findByStatus(RequestStatus status) {
        return findAll().stream()
                .filter(r -> r.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<ServiceRequest> findByCustomerId(String customerId) {
        return findAll().stream()
                .filter(r -> r.getCustomerId().equalsIgnoreCase(customerId))
                .collect(Collectors.toList());
    }

    /** Adds a new request, or replaces the one with the same ID */
    public synchronized ServiceRequest save(ServiceRequest request) {
        List<ServiceRequest> all = findAll();
        boolean replaced = false;

        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getRequestId().equalsIgnoreCase(request.getRequestId())) {
                all.set(i, request);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            all.add(request);
        }

        writeAll(all);
        return request;
    }

    public synchronized void delete(ServiceRequest request) {
        List<ServiceRequest> remaining = findAll().stream()
                .filter(r -> !r.getRequestId().equalsIgnoreCase(request.getRequestId()))
                .collect(Collectors.toList());
        writeAll(remaining);
    }

    /** Rewrites the whole file */
    private synchronized void writeAll(List<ServiceRequest> requests) {
        ensureFileExists();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, StandardCharsets.UTF_8, false))) {
            writer.write(HEADER);
            writer.newLine();
            for (ServiceRequest r : requests) {
                writer.write(formatRequest(r));
                writer.newLine();
            }
        } catch (IOException e) {
            System.err.println("Could not write service request file: " + e.getMessage());
        }
    }

    /** ServiceRequest -> one line of text */
    private String formatRequest(ServiceRequest r) {
        String[] d = {"", "", "", "", ""};

        if (r instanceof FoodRequest food) {
            d[0] = clean(food.getItemName());
            d[1] = String.valueOf(food.getQuantity());
            d[2] = String.valueOf(food.getUnitPrice());
        } else if (r instanceof LaundryRequest laundry) {
            d[0] = String.valueOf(laundry.getItemCount());
            d[1] = String.valueOf(laundry.isExpress());
        } else if (r instanceof TransportRequest transport) {
            d[0] = transport.getTripType().name();
            d[1] = transport.getVehicleType().name();
            d[2] = clean(transport.getLocation());
            d[3] = String.valueOf(transport.getDistanceKm());
            d[4] = transport.getTravelDate().toString();
        }

        return String.join("|",
                r.getRequestId(),
                r.getServiceType().name(),
                r.getCustomerId(),
                r.getRequestDate() != null ? r.getRequestDate().toString() : "",
                r.getStatus().name(),
                clean(r.getNotes()),
                d[0], d[1], d[2], d[3], d[4]
        );
    }

    /** Removes | and new lines so a value can't break the line format */
    private String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("|", "-").replace("\r", "").replace("\n", " ").trim();
    }

    /** One line of text -> FoodRequest, LaundryRequest or TransportRequest */
    private ServiceRequest parseLine(String line) {
        try {
            String[] p = line.split("\\|", -1);
            if (p.length < 11) {
                return null;
            }

            String id = p[0].trim();
            ServiceType type = ServiceType.valueOf(p[1].trim());
            String customerId = p[2].trim();
            LocalDate requestDate = p[3].trim().isEmpty() ? null : LocalDate.parse(p[3].trim());
            RequestStatus status = RequestStatus.valueOf(p[4].trim());
            String notes = p[5].trim();

            switch (type) {
                case FOOD:
                    return new FoodRequest(id, customerId, notes, requestDate, status,
                            p[6].trim(), Integer.parseInt(p[7].trim()), Double.parseDouble(p[8].trim()));
                case LAUNDRY:
                    return new LaundryRequest(id, customerId, notes, requestDate, status,
                            Integer.parseInt(p[6].trim()), Boolean.parseBoolean(p[7].trim()));
                case TRANSPORT:
                    return new TransportRequest(id, customerId, notes, requestDate, status,
                            TripType.valueOf(p[6].trim()), VehicleType.valueOf(p[7].trim()), p[8].trim(),
                            Double.parseDouble(p[9].trim()), LocalDate.parse(p[10].trim()));
                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Could not read service request line: '" + line + "': " + e.getMessage());
            return null;
        }
    }
}
