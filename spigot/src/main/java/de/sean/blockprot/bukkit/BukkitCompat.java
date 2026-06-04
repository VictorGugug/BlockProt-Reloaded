package de.sean.blockprot.bukkit;

import org.bukkit.Particle;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.NotNull;

/**
 * Runtime compatibility shims for Bukkit API fields that were renamed
 * between MC 1.20 and 26.x.
 *
 * Particle.DUST  — added in 1.20.6; older servers use Particle.REDSTONE.
 * Enchantment.INFINITY — added/renamed in 1.20.6; older servers use ARROW_INFINITE.
 *
 * All fields are resolved once at class-load via Enum.valueOf so there is
 * zero overhead after the first access.
 */
public final class BukkitCompat {

    /** Coloured dust particle. Use this everywhere instead of Particle.DUST. */
    public static final Particle PARTICLE_DUST;

    /** Colour-transition dust particle. Use instead of Particle.DUST_COLOR_TRANSITION. */
    public static final Particle PARTICLE_DUST_COLOR_TRANSITION;

    /** Glow enchantment used for visual toggle state. Use instead of Enchantment.INFINITY. */
    public static final Enchantment GLOW_ENCHANT;

    static {
        // ── Particle.DUST ────────────────────────────────────────────────────
        // 1.20.6+ → "DUST"   |   1.20.x → "REDSTONE"
        Particle dust;
        try {
            dust = Particle.valueOf("DUST");
        } catch (IllegalArgumentException e) {
            dust = Particle.valueOf("REDSTONE");
        }
        PARTICLE_DUST = dust;

        // ── Particle.DUST_COLOR_TRANSITION ───────────────────────────────────
        // 1.20.6+ → "DUST_COLOR_TRANSITION"  |  1.20.x → "REDSTONE_TRANSITION" (some builds)
        Particle dustTransition;
        try {
            dustTransition = Particle.valueOf("DUST_COLOR_TRANSITION");
        } catch (IllegalArgumentException e) {
            try {
                dustTransition = Particle.valueOf("REDSTONE_TRANSITION");
            } catch (IllegalArgumentException e2) {
                dustTransition = PARTICLE_DUST; // safe fallback
            }
        }
        PARTICLE_DUST_COLOR_TRANSITION = dustTransition;

        // ── Enchantment glow ─────────────────────────────────────────────────
        // 1.20.6+ → "INFINITY"   |   1.20.x → "ARROW_INFINITE"
        Enchantment glow;
        try {
            // Prefer field access (fastest, avoids keyed lookup issues across versions)
            glow = (Enchantment) Enchantment.class.getField("INFINITY").get(null);
        } catch (Exception e1) {
            try {
                glow = (Enchantment) Enchantment.class.getField("ARROW_INFINITE").get(null);
            } catch (Exception e2) {
                // Ultimate fallback — any common enchant works for the glow effect
                glow = Enchantment.UNBREAKING;
            }
        }
        GLOW_ENCHANT = glow;
    }

    private BukkitCompat() {}

    /** Convenience: returns true when PARTICLE_DUST resolved to the 1.20.6+ name. */
    public static boolean hasNewParticleNames() {
        return PARTICLE_DUST.name().equals("DUST");
    }

    /** Convenience: returns true when GLOW_ENCHANT resolved to the 1.20.6+ name. */
    public static boolean hasNewEnchantmentNames() {
        return GLOW_ENCHANT.getKey().getKey().equals("infinity");
    }

    /**
     * Returns a one-line diagnostic string for logging.
     * Example: "BukkitCompat[DUST/INFINITY, newParticle=true, newEnchant=true]"
     */
    @NotNull
    public static String getDiagnosticString() {
        return "BukkitCompat[particle=" + PARTICLE_DUST.name()
            + " enchant=" + GLOW_ENCHANT.getKey().getKey()
            + " newParticle=" + hasNewParticleNames()
            + " newEnchant=" + hasNewEnchantmentNames() + "]";
    }
}
