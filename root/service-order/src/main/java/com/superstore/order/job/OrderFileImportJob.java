package com.superstore.order.job;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superstore.order.pojo.OrderPojo;
import com.superstore.order.service.OrderFileImportService;

@Component
public class OrderFileImportJob {

    private static final Logger logger = LoggerFactory.getLogger(OrderFileImportJob.class);

    private final ObjectMapper objectMapper;
    private final OrderFileImportService importService;

    @Value("${order.import.folder:./order-import}")
    private String importFolder;

    public OrderFileImportJob(
        ObjectMapper objectMapper,
        OrderFileImportService importService) {
        this.objectMapper = objectMapper;
        this.importService = importService;
    }

    @Scheduled(fixedDelayString = "${order.import.fixed-delay-ms:10000}")
    public void scanIncomingFolder() {
        logger.info("scanning order import folder: {}", folder("incoming"));
        Path incoming = folder("incoming");

        try {
            Files.createDirectories(incoming);
            Files.createDirectories(folder("processing"));
            Files.createDirectories(folder("processed"));
            Files.createDirectories(folder("failed"));

            try (Stream<Path> files = Files.list(incoming)) {
                files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                        .sorted()
                        .forEach(this::processFile);
            }
        } catch (IOException e) {
            logger.error("Could not scan order import folder {}", incoming, e);
        }
    }

    private void processFile(Path source) {
        Path claimed = folder("processing").resolve(source.getFileName());

        try {
            Files.move(source, claimed);
        } catch (NoSuchFileException | FileAlreadyExistsException e) {
            return;
        } catch (IOException e) {
            logger.error("could not claim order file {}", source, e);
            return;
        }

        try {
            List<OrderPojo> orders = readOrders(claimed);
            importService.importOrders(orders);
            moveToFolder(claimed, "processed");
            logger.info("imported order file {}", claimed.getFileName());
        } catch (Exception e) {
            logger.error("failed to import order file {}", claimed.getFileName(), e);
            try {
                moveToFolder(claimed, "failed");
            } catch (IOException moveError) {
                logger.error("could not move failed file {}", claimed, moveError);
            }
        }
    }

    private List<OrderPojo> readOrders(Path file) throws IOException {
        JsonNode payload = objectMapper.readTree(file.toFile());

        if (payload == null || payload.isNull()) {
            throw new IllegalArgumentException("JSON payload is empty");
        }
        if (payload.isArray()) {
            return objectMapper.convertValue(payload, new TypeReference<List<OrderPojo>>() {});
        }
        return List.of(objectMapper.treeToValue(payload, OrderPojo.class));
    }

    private void moveToFolder(Path file, String folderName) throws IOException {
        String archivedName = UUID.randomUUID() + "-" + file.getFileName();
        Files.move(file, folder(folderName).resolve(archivedName));
    }

    private Path folder(String name) {
        return Paths.get(importFolder).resolve(name);
    }
}