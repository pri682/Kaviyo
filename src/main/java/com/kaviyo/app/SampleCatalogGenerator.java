package com.kaviyo.app;

import java.util.Locale;
import java.util.Random;

/**
 * Builds the downloadable sample catalog: 2,000 realistic fictional products.
 * Deterministic (fixed seed) so the file is stable across downloads and tests.
 * Samples live here — in the download — never hardcoded into running pages.
 */
public final class SampleCatalogGenerator {
    public static final int ROW_COUNT = 2000;
    public static final String[] HEADERS = {
            "product_id", "title", "url", "sku", "brand", "category", "description",
            "price", "currency", "availability", "attribute.highlight"};

    private static final long SEED = 20261102L;
    private static final int THIN_DESCRIPTION_EVERY = 50;

    private record Brand(String name, String slug, String initials) {
    }

    private record CategorySpec(
            String category,
            String[] namePatterns,
            String[] bodies,
            String[] highlights,
            int priceLow,
            int priceHigh,
            int sizeLow,
            int sizeHigh) {
    }

    private static final Brand[] BRANDS = {
            new Brand("Cedar Works", "cedarworks", "CW"),
            new Brand("Northstar Supply", "northstarsupply", "NS"),
            new Brand("Harbor and Pine", "harborandpine", "HP"),
            new Brand("Ridgeline Outfitters", "ridgelineoutfitters", "RO"),
            new Brand("Emberline Goods", "emberlinegoods", "EG"),
            new Brand("Copperfield Home", "copperfieldhome", "CH"),
            new Brand("Bluepine Audio", "bluepineaudio", "BA"),
            new Brand("Foxglove Kitchen", "foxglovekitchen", "FK"),
            new Brand("Stonecrop Fitness", "stonecropfitness", "SF"),
            new Brand("Marlowe Pets", "marlowepets", "MP")};

    private static final CategorySpec[] CATEGORIES = {
            new CategorySpec("Daypacks",
                    new String[]{"%dL Trail Daypack", "%dL Commuter Pack", "%dL Ultralight Pack"},
                    new String[]{
                            "Padded 15-inch laptop sleeve, ventilated back panel, and recycled 600D shell with rain cover included.",
                            "Magnetic buckles, hidden passport pocket, and side bottle sleeves; fits under most airline seats.",
                            "Roll-top closure, daisy-chain loops, and hip-belt pockets; weighs under 900 grams empty."},
                    new String[]{"fits 15-inch laptop", "rain cover included", "recycled shell"},
                    49, 189, 10, 36),
            new CategorySpec("Tents",
                    new String[]{"%d-Person Dome Tent", "%d-Person Backpacking Tent", "%d-Person Cabin Tent"},
                    new String[]{
                            "Color-coded poles pitch in under ten minutes; full-coverage fly and two vestibules for gear.",
                            "Double-wall build cuts condensation; packs to 45 cm and sleeps tall campers at 210 cm long.",
                            "Near-vertical walls, six windows, and a 3000 mm floor coating rated for three seasons."},
                    new String[]{"3-season", "3000 mm floor", "packs to 45 cm"},
                    129, 549, 1, 9),
            new CategorySpec("Headphones",
                    new String[]{"Studio Over-Ear Headphones", "Travel Noise-Cancelling Headphones", "Everyday On-Ear Headphones"},
                    new String[]{
                            "40 mm drivers, 30-hour battery, and memory-foam cups that fold flat for travel.",
                            "Hybrid noise cancelling with transparency mode; multipoint pairing for two devices at once.",
                            "Balanced tuning, detachable cable, and a hard case rated for daily commuting."},
                    new String[]{"30-hour battery", "noise cancelling", "multipoint pairing"},
                    79, 399, 0, 0),
            new CategorySpec("Running Shoes",
                    new String[]{"Tempo Road Running Shoes", "Trail Grip Running Shoes", "Daily Trainer Running Shoes"},
                    new String[]{
                            "Nitrogen-infused midsole with an 8 mm drop; breathable engineered mesh upper.",
                            "4 mm lugs, rock plate, and gaiter attachment for technical trails in all weather.",
                            "Recycled lining, reflective details, and a 250-gram build for everyday miles."},
                    new String[]{"8 mm drop", "reflective details", "recycled lining"},
                    89, 229, 0, 0),
            new CategorySpec("Coffee Makers",
                    new String[]{"12-Cup Drip Coffee Maker", "Single-Serve Pour-Over Brewer", "Compact Espresso Machine"},
                    new String[]{
                            "Pre-infusion bloom cycle, thermal carafe, and a 24-hour programmable timer.",
                            "Precision 96-degree pour with three strength settings; brews directly into travel mugs.",
                            "15-bar pump, conical burr grinder, and steam wand for milk drinks at home."},
                    new String[]{"thermal carafe", "24-hour timer", "15-bar pump"},
                    69, 499, 0, 0),
            new CategorySpec("Desk Lamps",
                    new String[]{"Architect Desk Lamp", "Minimal LED Desk Lamp", "Clamp Task Lamp"},
                    new String[]{
                            "Five color temperatures, stepless dimming, and a USB-C charging port in the base.",
                            "Weighted brass base, 50,000-hour LEDs, and touch controls with memory for last setting.",
                            "Slim clamp fits shelves and desks; flexible gooseneck holds any angle without sagging."},
                    new String[]{"5 color temperatures", "USB-C charging port", "50,000-hour LEDs"},
                    39, 179, 0, 0),
            new CategorySpec("Yoga Mats",
                    new String[]{"6 mm Studio Yoga Mat", "Travel 2 mm Yoga Mat", "Extra-Thick 8 mm Yoga Mat"},
                    new String[]{
                            "Natural rubber with laser-etched alignment lines; includes carry strap and cleaner spray.",
                            "Folds to A4 size at 900 grams; grippy top layer that improves with light moisture.",
                            "Joint-friendly cushioning with dual-texture sides for studio and home practice."},
                    new String[]{"alignment lines", "carry strap included", "natural rubber"},
                    29, 129, 0, 0),
            new CategorySpec("Insulated Bottles",
                    new String[]{"750 ml Steel Bottle", "1 L Trail Bottle", "500 ml Commuter Bottle"},
                    new String[]{
                            "Double-wall vacuum keeps drinks cold 24 hours or hot 12; dishwasher-safe lid included.",
                            "Ceramic-feel interior, leakproof straw cap, and a carry loop rated for carabiners.",
                            "Condensation-free powder coat; fits standard bike cages and cup holders."},
                    new String[]{"24 h cold / 12 h hot", "dishwasher-safe lid", "fits cup holders"},
                    19, 59, 0, 0)};

    private SampleCatalogGenerator() {
    }

    public static String generate() {
        return generate(ROW_COUNT);
    }

    static String generate(int rows) {
        StringBuilder csv = new StringBuilder(estimatedBytes(rows));
        csv.append(String.join(",", HEADERS)).append('\n');
        Random random = new Random(SEED);
        for (int index = 0; index < rows; index++) {
            Brand brand = BRANDS[index % BRANDS.length];
            CategorySpec spec = CATEGORIES[(index / BRANDS.length) % CATEGORIES.length];
            int variant = index / (BRANDS.length * CATEGORIES.length);
            csv.append(rowCsv(index, variant, brand, spec, random)).append('\n');
        }
        return csv.toString();
    }

    private static String rowCsv(int index, int variant, Brand brand, CategorySpec spec, Random random) {
        int sequence = index + 1;
        String productId = brand.slug() + "-" + slug(spec.category()) + "-" + sequence;
        int size = spec.sizeHigh() <= spec.sizeLow() ? spec.sizeLow()
                : spec.sizeLow() + random.nextInt(spec.sizeHigh() - spec.sizeLow());
        String model = spec.namePatterns[variant % spec.namePatterns.length].formatted(size);
        String title = brand.name() + " " + model + " " + (100 + variant);
        String url = "https://" + brand.slug() + ".example/products/" + productId;
        String sku = brand.initials() + "-" + String.format(Locale.ROOT, "%05d", sequence);
        String description = index % THIN_DESCRIPTION_EVERY == THIN_DESCRIPTION_EVERY - 1
                ? "Lightweight and durable."
                : spec.bodies[random.nextInt(spec.bodies.length)];
        String price = (spec.priceLow() + random.nextInt(spec.priceHigh() - spec.priceLow())) + ".99";
        String availability = pickAvailability(random);
        String highlight = spec.highlights[random.nextInt(spec.highlights.length)];
        return String.join(",", cell(productId), cell(title), cell(url), cell(sku), cell(brand.name()),
                cell(spec.category()), cell(description), cell(price), cell("USD"),
                cell(availability), cell(highlight));
    }

    private static String pickAvailability(Random random) {
        int roll = random.nextInt(100);
        if (roll < 85) return "IN_STOCK";
        if (roll < 95) return "OUT_OF_STOCK";
        return "PREORDER";
    }

    private static String slug(String category) {
        return category.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    private static String cell(String value) {
        if (value.indexOf(',') < 0 && value.indexOf('"') < 0 && value.indexOf('\n') < 0) return value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static int estimatedBytes(int rows) {
        return rows * 420 + 256;
    }
}
