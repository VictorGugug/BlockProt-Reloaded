package de.sean.blockprot.bukkit.config;

import de.sean.blockprot.bukkit.BlockProtLogger;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Parses block family expressions used in blocks.yml and worlds.yml.
 *
 * <h3>Syntax summary</h3>
 * <pre>
 *   [*]                        → all members of the family
 *   [* -CHEST]                 → all members except the CHEST material specifically
 *   [*-CHEST]                  → only the CHEST sub-family
 *   [*-CHEST -COPPER_CHEST]    → all chest sub-family members except COPPER_CHEST
 *   [*-CHEST -COPPER_CHESTPLATE] → ERROR: COPPER_CHESTPLATE not in TILE_ENTITIES family; discarded
 *   [CHEST BARREL]             → only CHEST and BARREL (empty base, explicit inclusions)
 *   [-*SHULKERS WHITE_SHULKER_BOX] → all family except shulkers, but WHITE_SHULKER_BOX added back
 * </pre>
 *
 * <h3>Token reference</h3>
 * <ul>
 *   <li>{@code *}       — include all members of the top-level family</li>
 *   <li>{@code *-TAG}   — include all members of the named sub-family</li>
 *   <li>{@code -*TAG}   — exclude all members of the named sub-family</li>
 *   <li>{@code NAME}    — include a specific material (must belong to this family)</li>
 *   <li>{@code -NAME}   — exclude a specific material (must belong to this family)</li>
 * </ul>
 *
 * <h3>Cross-family validation</h3>
 * {@code NAME} and {@code -NAME} tokens are validated against the family of the config key.
 * A material not belonging to the current family is rejected with a console warning and discarded.
 *
 * @see <a href="docs/BLOCK_FAMILY_SYNTAX.md">BLOCK_FAMILY_SYNTAX.md</a>
 */
public final class BlockFamilyParser {

    private BlockFamilyParser() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Sub-family definitions
    // ─────────────────────────────────────────────────────────────────────────

    public enum SubFamily {
        CHEST("CHEST", Family.TILE_ENTITIES),
        FURNACE("FURNACE", Family.TILE_ENTITIES),
        SHELF("SHELF", Family.TILE_ENTITIES),
        TRANSPORT("TRANSPORT", Family.TILE_ENTITIES),
        MISC("MISC", Family.TILE_ENTITIES),
        SIGN("SIGN", Family.TILE_ENTITIES),

        SHULKERS("SHULKERS", Family.SHULKER_BOXES),

        ANVIL("ANVIL", Family.BLOCKS),
        CAULDRON("CAULDRON", Family.BLOCKS),
        WORKSTATION("WORKSTATION", Family.BLOCKS),
        TRAPDOOR("TRAPDOOR", Family.BLOCKS),
        FENCE_GATE("FENCE_GATE", Family.BLOCKS),

        DOORS("DOORS", Family.DOORS),

        CHEST_BOATS("CHEST_BOATS", Family.ENTITIES),
        CHEST_MINECARTS("CHEST_MINECARTS", Family.ENTITIES),
        HOPPER_MINECARTS("HOPPER_MINECARTS", Family.ENTITIES);

        public final String tag;
        public final Family ownerFamily;

        SubFamily(String tag, Family ownerFamily) {
            this.tag = tag;
            this.ownerFamily = ownerFamily;
        }

        @Nullable
        public static SubFamily byTag(@NotNull String tag) {
            String upper = tag.toUpperCase(Locale.ROOT);
            for (SubFamily sf : values()) {
                if (sf.tag.equals(upper)) return sf;
            }
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Family definitions
    // ─────────────────────────────────────────────────────────────────────────

    public enum Family {
        TILE_ENTITIES,
        SHULKER_BOXES,
        BLOCKS,
        DOORS,
        ENTITIES
    }

    private static final Map<Family, Set<Material>>    FAMILY_MEMBERS    = new EnumMap<>(Family.class);
    private static final Map<SubFamily, Set<Material>> SUBFAMILY_MEMBERS = new EnumMap<>(SubFamily.class);

    static { buildFamilies(); }

    private static void buildFamilies() {
        Set<Material> tiles    = new LinkedHashSet<>();
        Set<Material> shulkers = new LinkedHashSet<>();
        Set<Material> blocks   = new LinkedHashSet<>();
        Set<Material> doors    = new LinkedHashSet<>();
        Set<Material> entities = new LinkedHashSet<>();

        Map<SubFamily, Set<Material>> sfAcc = new EnumMap<>(SubFamily.class);
        for (SubFamily sf : SubFamily.values()) sfAcc.put(sf, new LinkedHashSet<>());

        for (Material m : Material.values()) {
            if (m.isAir() || m.isLegacy()) continue;
            String n = m.name();

            if (n.contains("SHULKER_BOX")) {
                shulkers.add(m);
                sfAcc.get(SubFamily.SHULKERS).add(m);
                continue;
            }
            if (n.endsWith("_DOOR") && !n.contains("TRAP")) {
                doors.add(m);
                sfAcc.get(SubFamily.DOORS).add(m);
                continue;
            }
            if (isTileEntity(m)) {
                tiles.add(m);
                if (isChestMaterial(n))         sfAcc.get(SubFamily.CHEST).add(m);
                else if (isFurnaceMaterial(n))   sfAcc.get(SubFamily.FURNACE).add(m);
                else if (n.endsWith("_SHELF"))   sfAcc.get(SubFamily.SHELF).add(m);
                else if (isTransportMaterial(n)) sfAcc.get(SubFamily.TRANSPORT).add(m);
                else if (isSignMaterial(n))      sfAcc.get(SubFamily.SIGN).add(m);
                else                             sfAcc.get(SubFamily.MISC).add(m);
                continue;
            }
            if (isInteractiveBlock(m)) {
                blocks.add(m);
                if (n.contains("TRAPDOOR"))        sfAcc.get(SubFamily.TRAPDOOR).add(m);
                else if (n.contains("FENCE_GATE")) sfAcc.get(SubFamily.FENCE_GATE).add(m);
                else if (n.contains("ANVIL"))      sfAcc.get(SubFamily.ANVIL).add(m);
                else if (n.contains("CAULDRON"))   sfAcc.get(SubFamily.CAULDRON).add(m);
                else if (isWorkstationMaterial(n)) sfAcc.get(SubFamily.WORKSTATION).add(m);
                continue;
            }
            if (isEntityMaterial(n)) {
                entities.add(m);
                if (n.contains("CHEST_BOAT"))         sfAcc.get(SubFamily.CHEST_BOATS).add(m);
                else if (n.equals("CHEST_MINECART"))  sfAcc.get(SubFamily.CHEST_MINECARTS).add(m);
                else if (n.equals("HOPPER_MINECART")) sfAcc.get(SubFamily.HOPPER_MINECARTS).add(m);
            }
        }

        FAMILY_MEMBERS.put(Family.TILE_ENTITIES, Collections.unmodifiableSet(tiles));
        FAMILY_MEMBERS.put(Family.SHULKER_BOXES, Collections.unmodifiableSet(shulkers));
        FAMILY_MEMBERS.put(Family.BLOCKS,        Collections.unmodifiableSet(blocks));
        FAMILY_MEMBERS.put(Family.DOORS,         Collections.unmodifiableSet(doors));
        FAMILY_MEMBERS.put(Family.ENTITIES,      Collections.unmodifiableSet(entities));

        for (SubFamily sf : SubFamily.values())
            SUBFAMILY_MEMBERS.put(sf, Collections.unmodifiableSet(sfAcc.get(sf)));
    }

    // ── Material classifiers ──────────────────────────────────────────────────

    private static boolean isChestMaterial(@NotNull String n) {
        return n.equals("CHEST") || n.equals("TRAPPED_CHEST") || n.equals("ENDER_CHEST")
            || n.contains("COPPER_CHEST") || n.contains("COPPER_TRAPPED_CHEST");
    }

    private static boolean isFurnaceMaterial(@NotNull String n) {
        return n.equals("FURNACE") || n.equals("SMOKER") || n.equals("BLAST_FURNACE");
    }

    private static boolean isTransportMaterial(@NotNull String n) {
        return n.equals("HOPPER") || n.equals("DISPENSER") || n.equals("DROPPER");
    }

    private static boolean isSignMaterial(@NotNull String n) {
        return n.endsWith("_SIGN") || n.endsWith("_WALL_SIGN")
            || n.endsWith("_HANGING_SIGN") || n.endsWith("_WALL_HANGING_SIGN");
    }

    private static boolean isWorkstationMaterial(@NotNull String n) {
        return n.equals("GRINDSTONE") || n.equals("STONECUTTER") || n.equals("LOOM")
            || n.equals("CARTOGRAPHY_TABLE") || n.equals("SMITHING_TABLE")
            || n.equals("ENCHANTING_TABLE");
    }

    private static boolean isEntityMaterial(@NotNull String n) {
        return n.contains("CHEST_BOAT") || n.equals("CHEST_MINECART") || n.equals("HOPPER_MINECART");
    }

    private static boolean isTileEntity(@NotNull Material m) {
        String n = m.name();
        if (isChestMaterial(n)) return true;
        if (n.endsWith("_SHELF")) return true;
        if (isFurnaceMaterial(n)) return true;
        if (n.equals("HOPPER") || n.equals("DISPENSER") || n.equals("DROPPER")) return true;
        if (n.equals("BARREL") || n.equals("BREWING_STAND")) return true;
        if (n.equals("DECORATED_POT") || n.equals("CHISELED_BOOKSHELF") || n.equals("CRAFTER")) return true;
        if (n.equals("LECTERN") || n.equals("BEEHIVE") || n.equals("BEE_NEST")) return true;
        if (n.equals("JUKEBOX")) return true;
        if (isSignMaterial(n)) return true;
        return false;
    }

    private static boolean isInteractiveBlock(@NotNull Material m) {
        String n = m.name();
        if (n.equals("DRAGON_EGG")) return true;
        if (n.equals("COMPOSTER") || n.equals("BELL") || n.equals("NOTE_BLOCK")) return true;
        if (n.contains("CAULDRON")) return true;
        if (n.contains("ANVIL")) return true;
        if (isWorkstationMaterial(n)) return true;
        if (n.contains("FENCE_GATE")) return true;
        if (n.contains("TRAPDOOR")) return true;
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public accessors
    // ─────────────────────────────────────────────────────────────────────────

    @NotNull
    public static Set<Material> getFamilyMembers(@NotNull Family family) {
        return FAMILY_MEMBERS.getOrDefault(family, Collections.emptySet());
    }

    @NotNull
    public static Set<Material> getSubFamilyMembers(@NotNull SubFamily subFamily) {
        return SUBFAMILY_MEMBERS.getOrDefault(subFamily, Collections.emptySet());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Top-level parse entry point
    // ─────────────────────────────────────────────────────────────────────────

    @NotNull
    public static Set<Material> parse(@Nullable Object raw, @NotNull Family family) {
        if (raw == null) return Collections.emptySet();

        if (raw instanceof List<?> list) {
            if (list.isEmpty()) return Collections.emptySet();
            Set<Material> result = new LinkedHashSet<>();
            for (Object o : list) {
                if (o instanceof String s) {
                    String trimmed = s.trim();
                    if (isFamilyExpression(trimmed)) {
                        result.addAll(parseFamilyExpression(trimmed, family));
                    } else {
                        Material m = Material.matchMaterial(trimmed);
                        if (m != null) result.add(m);
                    }
                }
            }
            return result;
        }

        if (raw instanceof String s) {
            String trimmed = s.trim();
            if (isFamilyExpression(trimmed)) return parseFamilyExpression(trimmed, family);
            Material m = Material.matchMaterial(trimmed);
            return m != null ? Set.of(m) : Collections.emptySet();
        }

        return Collections.emptySet();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Expression helpers
    // ─────────────────────────────────────────────────────────────────────────

    public static boolean isFamilyExpression(@NotNull String s) {
        return s.startsWith("[") && s.endsWith("]");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core expression parser
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Parses a bracket expression and resolves it against the given family.
     *
     * <h4>Token types</h4>
     * <ul>
     *   <li>{@code *}       — include all members of the top-level family</li>
     *   <li>{@code *-TAG}   — include all members of the named sub-family</li>
     *   <li>{@code -*TAG}   — exclude all members of the named sub-family</li>
     *   <li>{@code NAME}    — include a specific material (must belong to this family)</li>
     *   <li>{@code -NAME}   — exclude a specific material (must belong to this family)</li>
     * </ul>
     *
     * Individual {@code NAME} and {@code -NAME} tokens are validated against the current family.
     * A material that does not belong to it is logged as a warning and discarded.
     */
    @NotNull
    public static Set<Material> parseFamilyExpression(@NotNull String expr, @NotNull Family family) {
        String inner = expr.substring(1, expr.length() - 1).trim();
        if (inner.isEmpty()) return Collections.emptySet();

        Set<Material> allMembers = FAMILY_MEMBERS.getOrDefault(family, Collections.emptySet());

        List<String> tokens = new ArrayList<>(Arrays.asList(inner.split("\\s+")));
        boolean hasGlobalStar = tokens.contains("*");

        Set<SubFamily> enabledSubFamilies  = new LinkedHashSet<>();
        Set<SubFamily> disabledSubFamilies = new LinkedHashSet<>();
        Set<Material>  explicitInclusions  = new LinkedHashSet<>();
        Set<Material>  explicitExclusions  = new LinkedHashSet<>();
        // For -* disabled sub-families: individual NAME tokens that re-include one member
        Map<SubFamily, Set<Material>> disabledSubFamilyExceptions = new LinkedHashMap<>();

        for (String token : tokens) {
            if (token.equals("*")) continue;

            // *-TAG — enable sub-family
            if (token.startsWith("*-")) {
                String tag = token.substring(2).toUpperCase(Locale.ROOT);
                SubFamily sf = SubFamily.byTag(tag);
                if (sf == null) {
                    BlockProtLogger.warn("Unknown sub-family tag '" + tag + "' in expression: " + expr);
                    continue;
                }
                if (sf.ownerFamily != family) {
                    BlockProtLogger.warn("Sub-family '" + tag + "' belongs to '"
                        + sf.ownerFamily.name().toLowerCase(Locale.ROOT)
                        + "', not '" + family.name().toLowerCase(Locale.ROOT)
                        + "' — ignored in: " + expr);
                    continue;
                }
                enabledSubFamilies.add(sf);
                continue;
            }

            // -*TAG — disable sub-family
            if (token.startsWith("-*")) {
                String tag = token.substring(2).toUpperCase(Locale.ROOT);
                SubFamily sf = SubFamily.byTag(tag);
                if (sf == null) {
                    BlockProtLogger.warn("Unknown sub-family tag '" + tag + "' in expression: " + expr);
                    continue;
                }
                if (sf.ownerFamily != family) {
                    BlockProtLogger.warn("Sub-family '" + tag + "' belongs to '"
                        + sf.ownerFamily.name().toLowerCase(Locale.ROOT)
                        + "', not '" + family.name().toLowerCase(Locale.ROOT)
                        + "' — ignored in: " + expr);
                    continue;
                }
                disabledSubFamilies.add(sf);
                disabledSubFamilyExceptions.putIfAbsent(sf, new LinkedHashSet<>());
                continue;
            }

            // -NAME — explicit exclusion
            if (token.startsWith("-") && token.length() > 1) {
                String name = token.substring(1).toUpperCase(Locale.ROOT);
                Material m = Material.matchMaterial(name);
                if (m == null) {
                    BlockProtLogger.warn("Unknown material '" + name + "' in expression: " + expr);
                    continue;
                }
                if (!allMembers.contains(m)) {
                    BlockProtLogger.warn("Material '" + name + "' does not belong to family '"
                        + family.name().toLowerCase(Locale.ROOT)
                        + "' — ignored in: " + expr);
                    continue;
                }
                explicitExclusions.add(m);
                continue;
            }

            // NAME — explicit inclusion
            if (!token.isEmpty()) {
                String name = token.toUpperCase(Locale.ROOT);
                Material m = Material.matchMaterial(name);
                if (m == null) {
                    BlockProtLogger.warn("Unknown material '" + name + "' in expression: " + expr);
                    continue;
                }
                if (!allMembers.contains(m)) {
                    BlockProtLogger.warn("Material '" + name + "' does not belong to family '"
                        + family.name().toLowerCase(Locale.ROOT)
                        + "' — ignored in: " + expr);
                    continue;
                }
                // If this material belongs to a disabled sub-family, treat it as an exception
                boolean routedToException = false;
                for (SubFamily dsf : disabledSubFamilies) {
                    if (SUBFAMILY_MEMBERS.getOrDefault(dsf, Collections.emptySet()).contains(m)) {
                        disabledSubFamilyExceptions.get(dsf).add(m);
                        routedToException = true;
                        break;
                    }
                }
                if (!routedToException) explicitInclusions.add(m);
            }
        }

        Set<Material> result = new LinkedHashSet<>();

        if (hasGlobalStar) {
            // Base: everything in the family
            for (Material m : allMembers) {
                boolean inDisabledSF = false;
                for (SubFamily dsf : disabledSubFamilies) {
                    Set<Material> dsMembers = SUBFAMILY_MEMBERS.getOrDefault(dsf, Collections.emptySet());
                    if (dsMembers.contains(m)) {
                        Set<Material> exc = disabledSubFamilyExceptions.get(dsf);
                        if (exc == null || !exc.contains(m)) { inDisabledSF = true; break; }
                    }
                }
                if (!inDisabledSF && !explicitExclusions.contains(m)) result.add(m);
            }
            result.addAll(explicitInclusions);

        } else if (!enabledSubFamilies.isEmpty()) {
            // Base: union of enabled sub-families
            for (SubFamily sf : enabledSubFamilies) {
                for (Material m : SUBFAMILY_MEMBERS.getOrDefault(sf, Collections.emptySet())) {
                    if (!explicitExclusions.contains(m)) result.add(m);
                }
            }
            // Re-include exceptions from disabled sub-families
            for (Set<Material> exc : disabledSubFamilyExceptions.values()) result.addAll(exc);
            result.addAll(explicitInclusions);

        } else if (!disabledSubFamilies.isEmpty()) {
            // Base: all family members minus disabled sub-families
            for (Material m : allMembers) {
                boolean inDisabled = false;
                for (SubFamily dsf : disabledSubFamilies) {
                    if (SUBFAMILY_MEMBERS.getOrDefault(dsf, Collections.emptySet()).contains(m)) {
                        inDisabled = true; break;
                    }
                }
                if (!inDisabled) result.add(m);
            }
            for (Set<Material> exc : disabledSubFamilyExceptions.values()) result.addAll(exc);
            result.addAll(explicitInclusions);
            result.removeAll(explicitExclusions);

        } else {
            // No star, no sub-families — explicit inclusions only
            result.addAll(explicitInclusions);
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Migration helper — flat list → compact expression
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Converts a flat list of Materials into the most compact sub-family-aware
     * family expression for the given family key.
     * Emits {@code NAME} for individual inclusions and {@code -NAME} for exclusions.
     */
    @Nullable
    public static String toFamilyExpression(
            @NotNull Collection<Material> materials,
            @NotNull Family family) {

        Set<Material> all = FAMILY_MEMBERS.getOrDefault(family, Collections.emptySet());
        if (all.isEmpty()) return null;

        Set<Material> present = new HashSet<>(materials);
        Set<Material> missing = new LinkedHashSet<>();
        for (Material m : all) { if (!present.contains(m)) missing.add(m); }

        if (missing.isEmpty()) return "[*]";

        List<SubFamily> sfs = new ArrayList<>();
        for (SubFamily sf : SubFamily.values()) { if (sf.ownerFamily == family) sfs.add(sf); }

        Map<SubFamily, SfStatus> sfStatus = new EnumMap<>(SubFamily.class);
        for (SubFamily sf : sfs) {
            Set<Material> sfMembers = SUBFAMILY_MEMBERS.getOrDefault(sf, Collections.emptySet());
            if (sfMembers.isEmpty()) { sfStatus.put(sf, SfStatus.FULL); continue; }
            long active = sfMembers.stream().filter(present::contains).count();
            sfStatus.put(sf, active == sfMembers.size() ? SfStatus.FULL
                           : active == 0               ? SfStatus.NONE
                                                       : SfStatus.PARTIAL);
        }

        Set<Material> ungrouped = new LinkedHashSet<>();
        for (Material m : all) {
            boolean inSf = false;
            for (SubFamily sf : sfs) {
                if (SUBFAMILY_MEMBERS.getOrDefault(sf, Collections.emptySet()).contains(m)) {
                    inSf = true; break;
                }
            }
            if (!inSf) ungrouped.add(m);
        }
        Set<Material> ungroupedPresent = new LinkedHashSet<>();
        Set<Material> ungroupedMissing = new LinkedHashSet<>();
        for (Material m : ungrouped) {
            if (present.contains(m)) ungroupedPresent.add(m); else ungroupedMissing.add(m);
        }

        boolean allClean = sfs.stream().allMatch(sf -> sfStatus.get(sf) != SfStatus.PARTIAL);
        if (allClean) {
            List<SubFamily> activeSfs   = sfs.stream().filter(sf -> sfStatus.get(sf) == SfStatus.FULL).toList();
            List<SubFamily> inactiveSfs = sfs.stream().filter(sf -> sfStatus.get(sf) == SfStatus.NONE).toList();
            if (inactiveSfs.isEmpty()) {
                StringBuilder sb = new StringBuilder("[*");
                for (Material m : ungroupedMissing) sb.append(" -").append(m.name());
                sb.append("]");
                return sb.toString();
            }
            boolean useStar = activeSfs.size() > inactiveSfs.size();
            StringBuilder sb = new StringBuilder("[");
            if (useStar) {
                sb.append("*");
                for (SubFamily sf : inactiveSfs) sb.append(" -*").append(sf.tag);
                for (Material m : ungroupedMissing) sb.append(" -").append(m.name());
            } else {
                boolean first = true;
                for (SubFamily sf : activeSfs) {
                    if (!first) sb.append(" ");
                    sb.append("*-").append(sf.tag);
                    first = false;
                }
                for (Material m : ungroupedPresent) {
                    if (!first) sb.append(" ");
                    sb.append(m.name());
                    first = false;
                }
            }
            sb.append("]");
            return sb.toString();
        }

        if (missing.size() <= present.size()) {
            StringBuilder sb = new StringBuilder("[*");
            for (SubFamily sf : sfs) {
                if (sfStatus.get(sf) == SfStatus.NONE) sb.append(" -*").append(sf.tag);
            }
            for (SubFamily sf : sfs) {
                if (sfStatus.get(sf) == SfStatus.PARTIAL) {
                    for (Material m : SUBFAMILY_MEMBERS.getOrDefault(sf, Collections.emptySet())) {
                        if (!present.contains(m)) sb.append(" -").append(m.name());
                    }
                }
            }
            for (Material m : ungroupedMissing) sb.append(" -").append(m.name());
            sb.append("]");
            return sb.toString();
        } else {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (SubFamily sf : sfs) {
                if (sfStatus.get(sf) == SfStatus.FULL) {
                    if (!first) sb.append(" ");
                    sb.append("*-").append(sf.tag);
                    first = false;
                }
            }
            for (SubFamily sf : sfs) {
                if (sfStatus.get(sf) == SfStatus.PARTIAL) {
                    if (!first) sb.append(" ");
                    sb.append("*-").append(sf.tag);
                    first = false;
                    for (Material m : SUBFAMILY_MEMBERS.getOrDefault(sf, Collections.emptySet())) {
                        if (!present.contains(m)) sb.append(" -").append(m.name());
                    }
                }
            }
            for (Material m : ungroupedPresent) {
                if (!first) sb.append(" ");
                sb.append(m.name());
                first = false;
            }
            sb.append("]");
            return first ? null : sb.toString();
        }
    }

    private enum SfStatus { FULL, NONE, PARTIAL }

    /**
     * Same as {@link #parseFamilyExpression(String, Family)} but suppresses all
     * cross-family warnings. Use this when iterating all families to resolve a
     * multi-family expression (e.g. auto_drop_to_inventory) so that tokens like
     * {@code *-SHULKERS} do not produce spurious WARN lines when tested against
     * TILE_ENTITIES, BLOCKS, DOORS, and ENTITIES.
     */
    @NotNull
    public static Set<Material> parseFamilyExpressionSilent(@NotNull String expr, @NotNull Family family) {
        String inner = expr.substring(1, expr.length() - 1).trim();
        if (inner.isEmpty()) return Collections.emptySet();

        Set<Material> allMembers = FAMILY_MEMBERS.getOrDefault(family, Collections.emptySet());
        List<String>  tokens     = new ArrayList<>(Arrays.asList(inner.split("\\s+")));
        boolean hasGlobalStar = tokens.contains("*");

        Set<SubFamily> enabledSF   = new LinkedHashSet<>();
        Set<SubFamily> disabledSF  = new LinkedHashSet<>();
        Set<Material>  inclusions  = new LinkedHashSet<>();
        Set<Material>  exclusions  = new LinkedHashSet<>();
        Map<SubFamily, Set<Material>> sfExceptions = new LinkedHashMap<>();

        for (String token : tokens) {
            if (token.equals("*")) continue;
            if (token.startsWith("*-")) {
                String tag = token.substring(2).toUpperCase(Locale.ROOT);
                SubFamily sf = SubFamily.byTag(tag);
                if (sf == null || sf.ownerFamily != family) continue; // silent
                enabledSF.add(sf);
            } else if (token.startsWith("-*")) {
                String tag = token.substring(2).toUpperCase(Locale.ROOT);
                SubFamily sf = SubFamily.byTag(tag);
                if (sf == null || sf.ownerFamily != family) continue; // silent
                disabledSF.add(sf);
                sfExceptions.putIfAbsent(sf, new LinkedHashSet<>());
            } else if (token.startsWith("-") && token.length() > 1) {
                Material m = Material.matchMaterial(token.substring(1).toUpperCase(Locale.ROOT));
                if (m != null && allMembers.contains(m)) exclusions.add(m);
            } else if (!token.isEmpty()) {
                Material m = Material.matchMaterial(token.toUpperCase(Locale.ROOT));
                if (m == null || !allMembers.contains(m)) continue;
                boolean routed = false;
                for (SubFamily dsf : disabledSF) {
                    if (SUBFAMILY_MEMBERS.getOrDefault(dsf, Collections.emptySet()).contains(m)) {
                        sfExceptions.get(dsf).add(m); routed = true; break;
                    }
                }
                if (!routed) inclusions.add(m);
            }
        }

        Set<Material> result = new LinkedHashSet<>();
        if (hasGlobalStar) {
            for (Material m : allMembers) {
                boolean blocked = false;
                for (SubFamily dsf : disabledSF) {
                    Set<Material> ds = SUBFAMILY_MEMBERS.getOrDefault(dsf, Collections.emptySet());
                    if (ds.contains(m)) {
                        Set<Material> exc = sfExceptions.get(dsf);
                        if (exc == null || !exc.contains(m)) { blocked = true; break; }
                    }
                }
                if (!blocked && !exclusions.contains(m)) result.add(m);
            }
            result.addAll(inclusions);
        } else if (!enabledSF.isEmpty()) {
            for (SubFamily sf : enabledSF)
                for (Material m : SUBFAMILY_MEMBERS.getOrDefault(sf, Collections.emptySet()))
                    if (!exclusions.contains(m)) result.add(m);
            for (Set<Material> exc : sfExceptions.values()) result.addAll(exc);
            result.addAll(inclusions);
        } else if (!disabledSF.isEmpty()) {
            for (Material m : allMembers) {
                boolean inDis = false;
                for (SubFamily dsf : disabledSF)
                    if (SUBFAMILY_MEMBERS.getOrDefault(dsf, Collections.emptySet()).contains(m)) { inDis = true; break; }
                if (!inDis) result.add(m);
            }
            for (Set<Material> exc : sfExceptions.values()) result.addAll(exc);
            result.addAll(inclusions);
            result.removeAll(exclusions);
        } else {
            result.addAll(inclusions);
        }
        return result;
    }
}
