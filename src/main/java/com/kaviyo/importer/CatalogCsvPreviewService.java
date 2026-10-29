package com.kaviyo.importer;

import com.kaviyo.catalog.Availability;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Validates merchant CSV input without saving it or fetching product URLs. */
public final class CatalogCsvPreviewService {
    private static final int MAX_CSV_BYTES = 1_048_576;
    private static final int MAX_ROWS = 500;
    private static final int MAX_COLUMNS = 100;
    private static final int MAX_CELL_CHARACTERS = 20_000;
    private static final int MAX_PRICE_CHARACTERS = 32;
    private static final int MAX_PRODUCT_ID_CHARACTERS = 180;
    private static final int MAX_TITLE_CHARACTERS = 500;
    private static final int MAX_OPTIONAL_LABEL_CHARACTERS = 180;
    private static final int MAX_ATTRIBUTE_KEY_CHARACTERS = 160;

    private static final Set<String> REQUIRED_HEADERS = Set.of("product_id", "title", "url");
    private static final Set<String> SUPPORTED_HEADERS = Set.of(
            "product_id", "title", "sku", "brand", "category", "description",
            "price", "currency", "availability", "url");
    private static final Pattern ATTRIBUTE_HEADER = Pattern.compile("attribute\\.[a-z0-9_]+(?:\\.[a-z0-9_]+)*");

    public CatalogImportPreview preview(String csv) {
        if (csv == null || csv.isBlank()) {
            throw new CsvPreviewException("CSV content is empty.");
        }
        if (csv.getBytes(StandardCharsets.UTF_8).length > MAX_CSV_BYTES) {
            throw new CsvPreviewException("CSV exceeds the 1 MiB preview limit.");
        }

        List<List<String>> records = parseRecords(csv);
        if (records.isEmpty()) {
            throw new CsvPreviewException("CSV does not contain a header row.");
        }

        List<String> headers = normalizeHeaders(records.get(0));
        validateHeaders(headers);
        Map<String, Integer> headerPositions = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            headerPositions.put(headers.get(i), i);
        }

        List<RowBuilder> rows = new ArrayList<>();
        Map<String, List<RowBuilder>> rowsById = new HashMap<>();
        for (int i = 1; i < records.size(); i++) {
            List<String> cells = records.get(i);
            if (isBlankRecord(cells)) {
                continue;
            }
            int rowNumber = i + 1;
            RowBuilder row = validateRow(rowNumber, cells, headers, headerPositions);
            rows.add(row);
            if (row.productId != null && !row.productId.isBlank()) {
                rowsById.computeIfAbsent(row.productId.toLowerCase(Locale.ROOT), ignored -> new ArrayList<>())
                        .add(row);
            }
        }

        rowsById.values().stream()
                .filter(duplicates -> duplicates.size() > 1)
                .forEach(duplicates -> duplicates.forEach(row ->
                        row.errors.add("Product ID is duplicated in this file.")));

        List<CatalogImportRowPreview> previews = rows.stream()
                .map(RowBuilder::toPreview)
                .toList();
        if (previews.isEmpty()) {
            throw new CsvPreviewException("CSV must contain at least one product row.");
        }
        int readyRows = (int) previews.stream().filter(CatalogImportRowPreview::readyToImport).count();
        return new CatalogImportPreview(
                "MERCHANT_SUPPLIED_PREVIEW", sha256(csv), previews.size(), readyRows,
                previews.size() - readyRows, previews);
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                hex.append(Character.forDigit((item >> 4) & 0xF, 16));
                hex.append(Character.forDigit(item & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable in this Java runtime.", exception);
        }
    }

    private static RowBuilder validateRow(
            int rowNumber,
            List<String> cells,
            List<String> headers,
            Map<String, Integer> headerPositions) {
        RowBuilder row = new RowBuilder(rowNumber);
        if (cells.size() > headers.size()) {
            row.errors.add("Row contains more values than the header row.");
        }

        row.productId = value(cells, headerPositions, "product_id");
        row.title = value(cells, headerPositions, "title");
        row.sku = value(cells, headerPositions, "sku");
        row.brand = value(cells, headerPositions, "brand");
        row.category = value(cells, headerPositions, "category");
        row.description = value(cells, headerPositions, "description");
        String priceValue = value(cells, headerPositions, "price");
        row.currency = value(cells, headerPositions, "currency");
        String availabilityValue = value(cells, headerPositions, "availability");
        String urlValue = value(cells, headerPositions, "url");

        if (isBlank(row.productId)) {
            row.errors.add("Product ID is required.");
        } else if (row.productId.length() > MAX_PRODUCT_ID_CHARACTERS) {
            row.errors.add("Product ID must be no longer than " + MAX_PRODUCT_ID_CHARACTERS + " characters.");
        }
        if (isBlank(row.title)) {
            row.errors.add("Product title is required.");
        } else if (row.title.length() > MAX_TITLE_CHARACTERS) {
            row.errors.add("Product title must be no longer than " + MAX_TITLE_CHARACTERS + " characters.");
        }
        validateOptionalLength(row, "SKU", row.sku);
        validateOptionalLength(row, "Brand", row.brand);
        validateOptionalLength(row, "Category", row.category);
        row.productUrl = parseProductUrl(urlValue, row.errors);
        row.price = parsePrice(priceValue, row.errors);
        if (!isBlank(row.currency)) {
            row.currency = row.currency.toUpperCase(Locale.ROOT);
            if (!row.currency.matches("[A-Z]{3}")) {
                row.errors.add("Currency must be a three-letter code such as USD.");
            }
        } else if (!isBlank(priceValue)) {
            row.errors.add("Currency is required when a price is provided.");
        }
        row.availability = parseAvailability(availabilityValue, row.errors);

        for (int i = 0; i < headers.size() && i < cells.size(); i++) {
            String header = headers.get(i);
            String cell = cells.get(i).trim();
            if (header.startsWith("attribute.") && !cell.isBlank()) {
                row.attributes.put(header.substring("attribute.".length()), cell);
            }
        }

        addOptionalFieldWarning(row, "sku", row.sku);
        addOptionalFieldWarning(row, "brand", row.brand);
        addOptionalFieldWarning(row, "category", row.category);
        addOptionalFieldWarning(row, "description", row.description);
        if (isBlank(priceValue)) {
            row.warnings.add("Price is not provided; price comparisons will be unavailable.");
        }
        if (isBlank(availabilityValue)) {
            row.warnings.add("Availability is not provided; stock comparisons will be unavailable.");
        }
        if (row.attributes.isEmpty()) {
            row.warnings.add("No product attributes were provided.");
        }
        return row;
    }

    private static void addOptionalFieldWarning(RowBuilder row, String field, String value) {
        if (isBlank(value)) {
            row.warnings.add(field + " is not provided.");
        }
    }

    private static BigDecimal parsePrice(String value, List<String> errors) {
        if (isBlank(value)) {
            return null;
        }
        if (value.length() > MAX_PRICE_CHARACTERS) {
            errors.add("Price is too long to parse safely.");
            return null;
        }
        try {
            BigDecimal price = new BigDecimal(value);
            if (price.signum() < 0) {
                errors.add("Price cannot be negative.");
                return null;
            }
            if (price.precision() > 15 || price.scale() > 4) {
                errors.add("Price has too many digits or decimal places.");
                return null;
            }
            return price;
        } catch (NumberFormatException exception) {
            errors.add("Price must be a nonnegative decimal number.");
            return null;
        }
    }

    private static String parseAvailability(String value, List<String> errors) {
        if (isBlank(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "IN_STOCK", "INSTOCK", "AVAILABLE" -> Availability.IN_STOCK.name();
            case "OUT_OF_STOCK", "OUTOFSTOCK", "SOLD_OUT" -> Availability.OUT_OF_STOCK.name();
            case "PREORDER", "PRE_ORDER" -> Availability.PREORDER.name();
            case "UNKNOWN" -> Availability.UNKNOWN.name();
            default -> {
                errors.add("Availability must be In stock, Out of stock, Preorder, or Unknown.");
                yield null;
            }
        };
    }

    private static URI parseProductUrl(String value, List<String> errors) {
        if (isBlank(value)) {
            errors.add("Product URL is required.");
            return null;
        }
        try {
            URI uri = new URI(value.trim()).normalize();
            String scheme = uri.getScheme();
            if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null || uri.getUserInfo() != null) {
                errors.add("Product URL must be an HTTP or HTTPS URL with a hostname and no embedded credentials.");
                return null;
            }
            return uri;
        } catch (URISyntaxException exception) {
            errors.add("Product URL is not a valid URL.");
            return null;
        }
    }

    private static List<String> normalizeHeaders(List<String> rawHeaders) {
        if (rawHeaders.size() > MAX_COLUMNS) {
            throw new CsvPreviewException("CSV has more than " + MAX_COLUMNS + " columns.");
        }
        List<String> headers = new ArrayList<>(rawHeaders.size());
        for (int i = 0; i < rawHeaders.size(); i++) {
            String header = rawHeaders.get(i).trim();
            if (i == 0 && header.startsWith("\uFEFF")) {
                header = header.substring(1);
            }
            headers.add(header.toLowerCase(Locale.ROOT));
        }
        return headers;
    }

    private static void validateHeaders(List<String> headers) {
        if (headers.stream().anyMatch(String::isBlank)) {
            throw new CsvPreviewException("Every CSV column must have a header.");
        }
        Set<String> uniqueHeaders = new HashSet<>();
        for (String header : headers) {
            if (!uniqueHeaders.add(header)) {
                throw new CsvPreviewException("CSV contains a duplicate column header: " + header + ".");
            }
            if (!SUPPORTED_HEADERS.contains(header) && !ATTRIBUTE_HEADER.matcher(header).matches()) {
                throw new CsvPreviewException("Unsupported CSV column: " + header
                        + ". Product-specific fields must use the attribute.<name> format.");
            }
            if (header.startsWith("attribute.")
                    && header.substring("attribute.".length()).length() > MAX_ATTRIBUTE_KEY_CHARACTERS) {
                throw new CsvPreviewException("Product attribute names must be no longer than "
                        + MAX_ATTRIBUTE_KEY_CHARACTERS + " characters.");
            }
        }
        List<String> missingHeaders = REQUIRED_HEADERS.stream()
                .filter(required -> !uniqueHeaders.contains(required))
                .sorted()
                .toList();
        if (!missingHeaders.isEmpty()) {
            throw new CsvPreviewException("CSV is missing required columns: "
                    + String.join(", ", missingHeaders) + ".");
        }
    }

    private static String value(List<String> cells, Map<String, Integer> positions, String header) {
        Integer position = positions.get(header);
        if (position == null || position >= cells.size()) {
            return null;
        }
        String value = cells.get(position).trim();
        return value.isEmpty() ? null : value;
    }

    private static boolean isBlankRecord(List<String> cells) {
        return cells.stream().allMatch(String::isBlank);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static void validateOptionalLength(RowBuilder row, String label, String value) {
        if (value != null && value.length() > MAX_OPTIONAL_LABEL_CHARACTERS) {
            row.errors.add(label + " must be no longer than " + MAX_OPTIONAL_LABEL_CHARACTERS + " characters.");
        }
    }

    private static List<List<String>> parseRecords(String input) {
        String csv = input.startsWith("\uFEFF") ? input.substring(1) : input;
        List<List<String>> records = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        boolean quoteClosed = false;

        for (int i = 0; i < csv.length(); i++) {
            char current = csv.charAt(i);
            if (quoted) {
                if (current == '"') {
                    if (i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                        cell.append('"');
                        i++;
                    } else {
                        quoted = false;
                        quoteClosed = true;
                    }
                } else {
                    cell.append(current);
                }
            } else if (quoteClosed) {
                if (current == ',') {
                    addCell(row, cell);
                    quoteClosed = false;
                } else if (current == '\r' || current == '\n') {
                    addCell(row, cell);
                    addRecord(records, row);
                    quoteClosed = false;
                    i = skipLineFeed(csv, i, current);
                } else if (!Character.isWhitespace(current)) {
                    throw new CsvPreviewException("Unexpected character after a quoted CSV value.");
                }
            } else if (current == '"') {
                if (!cell.toString().isBlank()) {
                    throw new CsvPreviewException("A quoted CSV value must start at the beginning of a field.");
                }
                cell.setLength(0);
                quoted = true;
            } else if (current == ',') {
                addCell(row, cell);
            } else if (current == '\r' || current == '\n') {
                addCell(row, cell);
                addRecord(records, row);
                i = skipLineFeed(csv, i, current);
            } else {
                cell.append(current);
            }

            if (cell.length() > MAX_CELL_CHARACTERS) {
                throw new CsvPreviewException("A CSV value exceeds the "
                        + MAX_CELL_CHARACTERS + " character limit.");
            }
            if (row.size() > MAX_COLUMNS) {
                throw new CsvPreviewException("CSV has more than " + MAX_COLUMNS + " columns.");
            }
            if (records.size() > MAX_ROWS + 1) {
                throw new CsvPreviewException("CSV exceeds the " + MAX_ROWS + " product row limit.");
            }
        }

        if (quoted) {
            throw new CsvPreviewException("CSV ends inside a quoted value.");
        }
        if (cell.length() > 0 || !row.isEmpty() || quoteClosed) {
            addCell(row, cell);
            addRecord(records, row);
        }
        if (records.size() > MAX_ROWS + 1) {
            throw new CsvPreviewException("CSV exceeds the " + MAX_ROWS + " product row limit.");
        }
        return records;
    }

    private static int skipLineFeed(String csv, int index, char current) {
        if (current == '\r' && index + 1 < csv.length() && csv.charAt(index + 1) == '\n') {
            return index + 1;
        }
        return index;
    }

    private static void addCell(List<String> row, StringBuilder cell) {
        row.add(cell.toString());
        cell.setLength(0);
    }

    private static void addRecord(List<List<String>> records, List<String> row) {
        if (!isBlankRecord(row)) {
            records.add(List.copyOf(row));
        }
        row.clear();
    }

    private static final class RowBuilder {
        private final int rowNumber;
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();
        private final Map<String, String> attributes = new LinkedHashMap<>();
        private String productId;
        private String title;
        private String sku;
        private String brand;
        private String category;
        private String description;
        private BigDecimal price;
        private String currency;
        private String availability;
        private URI productUrl;

        private RowBuilder(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        private CatalogImportRowPreview toPreview() {
            return new CatalogImportRowPreview(
                    rowNumber, productId, title, sku, brand, category, description,
                    price, currency, availability, productUrl, attributes,
                    errors.isEmpty(), errors, warnings);
        }
    }
}
