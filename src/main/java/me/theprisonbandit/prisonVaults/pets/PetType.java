package me.theprisonbandit.prisonVaults.pets;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public enum PetType {
    // --- COMMON ($5,000) ---
    TABBY_CAT("Tabby Cat", EntityType.CAT, 5000, 1.0, Material.COD, "Tabby", false),
    TUXEDO_CAT("Tuxedo Cat", EntityType.CAT, 5000, 1.0, Material.COD, "Black", false),
    CALICO_CAT("Calico Cat", EntityType.CAT, 5000, 1.0, Material.COD, "Calico", false),
    SIAMESE_CAT("Siamese Cat", EntityType.CAT, 5000, 1.0, Material.COD, "Siamese", false),
    PERSIAN_CAT("Persian Cat", EntityType.CAT, 5000, 1.0, Material.COD, "Persian", false),
    RAGDOLL_CAT("Ragdoll Cat", EntityType.CAT, 5000, 1.0, Material.COD, "Ragdoll", false),
    BRITISH_SHORTHAIR("British Shorthair", EntityType.CAT, 5000, 1.0, Material.COD, "British_Shorthair", false),
    GINGER_CAT("Ginger Cat", EntityType.CAT, 5000, 1.0, Material.COD, "Red", false),
    BLACK_CAT("Black Cat", EntityType.CAT, 5000, 1.0, Material.COD, "All_Black", false),
    WHITE_CAT("White Cat", EntityType.CAT, 5000, 1.0, Material.COD, "White", false),
    DOG("Dog", EntityType.WOLF, 5000, 1.0, Material.BONE, null, false),

    // --- UNCOMMON ($15,000) ---
    PARROT_RED("Red Parrot", EntityType.PARROT, 15000, 1.0, Material.FEATHER, "RED", true),
    PARROT_BLUE("Blue Parrot", EntityType.PARROT, 15000, 1.0, Material.FEATHER, "BLUE", true),
    PARROT_GREEN("Green Parrot", EntityType.PARROT, 15000, 1.0, Material.FEATHER, "GREEN", true),
    PARROT_CYAN("Cyan Parrot", EntityType.PARROT, 15000, 1.0, Material.FEATHER, "CYAN", true),
    PARROT_GRAY("Gray Parrot", EntityType.PARROT, 15000, 1.0, Material.FEATHER, "GRAY", true),
    BAT("Bat", EntityType.BAT, 15000, 1.0, Material.COAL, null, true),

    // --- FOXES ($20,000) ---
    FOX_RED("Red Fox", EntityType.FOX, 20000, 0.7, Material.SWEET_BERRIES, "RED", false),
    FOX_SNOW("Snow Fox", EntityType.FOX, 20000, 0.7, Material.SNOWBALL, "SNOW", false),

    // --- RARE ($50,000) ---
    LIL_PANDA("Lil Panda", EntityType.PANDA, 50000, 0.4, Material.BAMBOO, null, false),
    LIL_PIG("Lil Pig", EntityType.PIG, 50000, 0.5, Material.PORKCHOP, null, false),
    LIL_HORSE("Lil Horse", EntityType.HORSE, 50000, 0.4, Material.SADDLE, null, false),
    LIL_SNOWMAN("Lil Snowman", EntityType.SNOW_GOLEM, 50000, 0.4, Material.CARVED_PUMPKIN, null, false), // NEW

    // --- LEGENDARY ($250,000) ---
    LIL_SKELETON("Lil Skeleton", EntityType.SKELETON, 250000, 0.5, Material.BONE, null, false),
    LIL_CREEPER("Lil Creeper", EntityType.CREEPER, 250000, 0.4, Material.GUNPOWDER, null, false),
    LIL_ENDERMAN("Lil Enderman", EntityType.ENDERMAN, 300000, 0.35, Material.ENDER_PEARL, null, false),
    LIL_WITHER_SKELETON("Lil Wither Skeleton", EntityType.WITHER_SKELETON, 250000, 0.5, Material.WITHER_SKELETON_SKULL, null, false),
    LIL_BLAZE("Lil Blaze", EntityType.BLAZE, 250000, 0.4, Material.BLAZE_ROD, null, true),
    LIL_GHAST("Lil Ghast", EntityType.GHAST, 300000, 0.1, Material.GHAST_TEAR, null, true),

    // --- MYTHICAL ($1,000,000+) ---
    LIL_WARDEN("Lil Warden", EntityType.WARDEN, 2000000, 0.35, Material.SCULK_SENSOR, null, false),
    LIL_WITHER("Lil Wither Boss", EntityType.WITHER, 1000000, 0.2, Material.NETHER_STAR, null, true),
    LIL_IRON_GOLEM("Lil Iron Golem", EntityType.IRON_GOLEM, 1000000, 0.4, Material.IRON_BLOCK, null, false); // NEW

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
}