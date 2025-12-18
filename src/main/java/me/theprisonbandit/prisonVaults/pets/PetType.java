package me.theprisonbandit.prisonVaults.pets;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public enum PetType {
    // --- COMMON ($5,000) ---
    TABBY_CAT("Tabby Cat", EntityType.CAT, 5000, 1.0, Material.COD, "TABBY", false),
    TUXEDO_CAT("Tuxedo Cat", EntityType.CAT, 5000, 1.0, Material.COD, "BLACK", false),
    CALICO_CAT("Calico Cat", EntityType.CAT, 5000, 1.0, Material.COD, "CALICO", false),
    SIAMESE_CAT("Siamese Cat", EntityType.CAT, 5000, 1.0, Material.COD, "SIAMESE", false),
    PERSIAN_CAT("Persian Cat", EntityType.CAT, 5000, 1.0, Material.COD, "PERSIAN", false),
    RAGDOLL_CAT("Ragdoll Cat", EntityType.CAT, 5000, 1.0, Material.COD, "RAGDOLL", false),
    BRITISH_SHORTHAIR("British Shorthair", EntityType.CAT, 5000, 1.0, Material.COD, "BRITISH_SHORTHAIR", false),
    GINGER_CAT("Ginger Cat", EntityType.CAT, 5000, 1.0, Material.COD, "RED", false),
    BLACK_CAT("Black Cat", EntityType.CAT, 5000, 1.0, Material.COD, "ALL_BLACK", false),
    WHITE_CAT("White Cat", EntityType.CAT, 5000, 1.0, Material.COD, "WHITE", false),
    JELLIE_CAT("Jellie Cat", EntityType.CAT, 5000, 1.0, Material.COD, "JELLIE", false),
    DOG("Dog", EntityType.WOLF, 5000, 1.0, Material.BONE, null, false),

    // --- RABBITS ($10,000) ---
    RABBIT_BROWN("Brown Rabbit", EntityType.RABBIT, 10000, 1.0, Material.RABBIT_HIDE, "BROWN", false),
    RABBIT_WHITE("White Rabbit", EntityType.RABBIT, 10000, 1.0, Material.RABBIT_HIDE, "WHITE", false),
    RABBIT_BLACK("Black Rabbit", EntityType.RABBIT, 10000, 1.0, Material.RABBIT_HIDE, "BLACK", false),
    RABBIT_B_W("Patchy Rabbit", EntityType.RABBIT, 10000, 1.0, Material.RABBIT_HIDE, "BLACK_AND_WHITE", false),
    RABBIT_GOLD("Gold Rabbit", EntityType.RABBIT, 10000, 1.0, Material.RABBIT_HIDE, "GOLD", false),
    RABBIT_SALT("Salt & Pepper Rabbit", EntityType.RABBIT, 10000, 1.0, Material.RABBIT_HIDE, "SALT_AND_PEPPER", false),
    RABBIT_KILLER("Killer Bunny", EntityType.RABBIT, 50000, 1.0, Material.RABBIT_FOOT, "THE_KILLER_BUNNY", false),

    // --- UNCOMMON ($15,000) ---
    PARROT_RED("Red Parrot", EntityType.PARROT, 15000, 1.0, Material.FEATHER, "RED", true),
    PARROT_BLUE("Blue Parrot", EntityType.PARROT, 15000, 1.0, Material.FEATHER, "BLUE", true),
    PARROT_GREEN("Green Parrot", EntityType.PARROT, 15000, 1.0, Material.FEATHER, "GREEN", true),
    PARROT_CYAN("Cyan Parrot", EntityType.PARROT, 15000, 1.0, Material.FEATHER, "CYAN", true),
    PARROT_GRAY("Gray Parrot", EntityType.PARROT, 15000, 1.0, Material.FEATHER, "GRAY", true),
    BAT("Bat", EntityType.BAT, 15000, 1.0, Material.COAL, null, true),
    BEE("Bee", EntityType.BEE, 15000, 0.8, Material.HONEYCOMB, null, true),

    // --- AXOLOTLS ($20,000) ---
    AXOLOTL_LUCY("Lucy Axolotl", EntityType.AXOLOTL, 20000, 1.0, Material.AXOLOTL_BUCKET, "LUCY", false),
    AXOLOTL_WILD("Wild Axolotl", EntityType.AXOLOTL, 20000, 1.0, Material.AXOLOTL_BUCKET, "WILD", false),
    AXOLOTL_GOLD("Gold Axolotl", EntityType.AXOLOTL, 20000, 1.0, Material.AXOLOTL_BUCKET, "GOLD", false),
    AXOLOTL_CYAN("Cyan Axolotl", EntityType.AXOLOTL, 20000, 1.0, Material.AXOLOTL_BUCKET, "CYAN", false),
    AXOLOTL_BLUE("Blue Axolotl", EntityType.AXOLOTL, 20000, 1.0, Material.AXOLOTL_BUCKET, "BLUE", false),

    // --- FOXES ($20,000) ---
    FOX_RED("Red Fox", EntityType.FOX, 20000, 0.7, Material.SWEET_BERRIES, "RED", false),
    FOX_SNOW("Snow Fox", EntityType.FOX, 20000, 0.7, Material.SNOWBALL, "SNOW", false),

    // --- HORSES ($30,000) ---
    HORSE_WHITE("White Horse", EntityType.HORSE, 30000, 0.4, Material.LEATHER_HORSE_ARMOR, "WHITE", false),
    HORSE_CREAMY("Creamy Horse", EntityType.HORSE, 30000, 0.4, Material.LEATHER_HORSE_ARMOR, "CREAMY", false),
    HORSE_CHESTNUT("Chestnut Horse", EntityType.HORSE, 30000, 0.4, Material.LEATHER_HORSE_ARMOR, "CHESTNUT", false),
    HORSE_BROWN("Brown Horse", EntityType.HORSE, 30000, 0.4, Material.LEATHER_HORSE_ARMOR, "BROWN", false),
    HORSE_BLACK("Black Horse", EntityType.HORSE, 30000, 0.4, Material.LEATHER_HORSE_ARMOR, "BLACK", false),
    HORSE_GRAY("Gray Horse", EntityType.HORSE, 30000, 0.4, Material.LEATHER_HORSE_ARMOR, "GRAY", false),
    HORSE_DARK_BROWN("Dark Brown Horse", EntityType.HORSE, 30000, 0.4, Material.LEATHER_HORSE_ARMOR, "DARK_BROWN", false),

    // --- ARTHROPODS ($35,000) ---
    SPIDER("Spider", EntityType.SPIDER, 35000, 0.8, Material.SPIDER_EYE, null, false),
    CAVE_SPIDER("Cave Spider", EntityType.CAVE_SPIDER, 45000, 0.7, Material.FERMENTED_SPIDER_EYE, null, false),

    // --- RARE ($50,000) ---
    LIL_PANDA("Lil Panda", EntityType.PANDA, 50000, 0.4, Material.BAMBOO, null, false),
    LIL_PIG("Lil Pig", EntityType.PIG, 50000, 0.5, Material.PORKCHOP, null, false),
    LIL_SNOWMAN("Lil Snowman", EntityType.SNOW_GOLEM, 50000, 0.4, Material.CARVED_PUMPKIN, null, false),

    // --- LEGENDARY ($250,000) ---
    LIL_SKELETON("Lil Skeleton", EntityType.SKELETON, 250000, 0.5, Material.BONE, null, false),
    LIL_CREEPER("Lil Creeper", EntityType.CREEPER, 250000, 0.4, Material.GUNPOWDER, null, false),
    LIL_ENDERMAN("Lil Enderman", EntityType.ENDERMAN, 300000, 0.35, Material.ENDER_PEARL, null, false),
    LIL_WITHER_SKELETON("Lil Wither Skeleton", EntityType.WITHER_SKELETON, 250000, 0.5, Material.WITHER_SKELETON_SKULL, null, false),
    LIL_BLAZE("Lil Blaze", EntityType.BLAZE, 250000, 0.4, Material.BLAZE_ROD, null, true),
    LIL_GHAST("Lil Ghast", EntityType.GHAST, 300000, 0.1, Material.GHAST_TEAR, null, true),
    LIL_PHANTOM("Lil Phantom", EntityType.PHANTOM, 350000, 0.5, Material.PHANTOM_MEMBRANE, null, true),

    // --- MYTHICAL ($1,000,000+) ---
    LIL_WARDEN("Lil Warden", EntityType.WARDEN, 2000000, 0.35, Material.SCULK_SENSOR, null, false),
    LIL_WITHER("Lil Wither Boss", EntityType.WITHER, 1000000, 0.2, Material.NETHER_STAR, null, true),
    LIL_IRON_GOLEM("Lil Iron Golem", EntityType.IRON_GOLEM, 1000000, 0.4, Material.IRON_BLOCK, null, false);

    public final String display;
    public final EntityType type;
    public final double price;
    public final double scale;
    public final Material icon;
    public final String variant;
    public final boolean flying;

    PetType(String display, EntityType type, double price, double scale, Material icon, String variant, boolean flying) {
        this.display = display;
        this.type = type;
        this.price = price;
        this.scale = scale;
        this.icon = icon;
        this.variant = variant;
        this.flying = flying;
    }

    public String getDisplayName() { return display; }
    public EntityType getEntityType() { return type; }
    public double getPrice() { return price; }
    public Material getIcon() { return icon; }
}