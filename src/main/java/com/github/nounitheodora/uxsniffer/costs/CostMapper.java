package com.github.nounitheodora.uxsniffer.costs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.github.nounitheodora.uxsniffer.UxSnifferBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class CostMapper {

    private static final CostMapper INSTANCE = new CostMapper();

    private final Map<String, SmellInfo> smellsById = new LinkedHashMap<>();
    private final Map<String, PafCost> costsById = new LinkedHashMap<>();
    private final Map<String, List<CostMapping>> mappingsBySmellId = new LinkedHashMap<>();
    private final Map<String, String> displayNameToSmellId = new LinkedHashMap<>();

    private CostMapper() {
        initDisplayNameMapping();
        loadJson();
    }

    public static @NotNull CostMapper getInstance() {
        return INSTANCE;
    }

    public @NotNull List<CostMapping> getMappingsForSmell(@NotNull String smellId) {
        return mappingsBySmellId.getOrDefault(smellId, List.of());
    }

    public @NotNull List<CostMapping> getMappingsForSmellByDisplayName(@NotNull String displayName) {
        String smellId = displayNameToSmellId.get(displayName);
        if (smellId == null) return List.of();
        return getMappingsForSmell(smellId);
    }

    public @Nullable SmellInfo getSmellInfo(@NotNull String smellId) {
        return smellsById.get(smellId);
    }

    public @Nullable SmellInfo getSmellInfoByDisplayName(@NotNull String displayName) {
        String smellId = displayNameToSmellId.get(displayName);
        if (smellId == null) return null;
        return smellsById.get(smellId);
    }

    public @Nullable PafCost getCost(@NotNull String costId) {
        return costsById.get(costId);
    }

    public @NotNull Map<String, PafCost> getAllCosts() {
        return Collections.unmodifiableMap(costsById);
    }

    public @NotNull Map<String, List<CostMapping>> getAllMappings() {
        return Collections.unmodifiableMap(mappingsBySmellId);
    }

    public @Nullable String getSmellIdForDisplayName(@NotNull String displayName) {
        return displayNameToSmellId.get(displayName);
    }

    public @NotNull Set<String> getAffectedCostIds(@NotNull String smellId) {
        Set<String> ids = new LinkedHashSet<>();
        for (CostMapping m : getMappingsForSmell(smellId)) {
            ids.add(m.costId());
        }
        return ids;
    }

    public @NotNull List<CostMapping> getPrimaryMappingsForSmell(@NotNull String smellId) {
        return getMappingsForSmell(smellId).stream()
                .filter(m -> "Primary".equals(m.priority()))
                .toList();
    }

    public @NotNull List<CostMapping> getSecondaryMappingsForSmell(@NotNull String smellId) {
        return getMappingsForSmell(smellId).stream()
                .filter(m -> "Secondary".equals(m.priority()))
                .toList();
    }

    private void initDisplayNameMapping() {
        displayNameToSmellId.put(UxSnifferBundle.message("inspection.large.file.name"), "S25");
        displayNameToSmellId.put(UxSnifferBundle.message("inspection.large.component.name"), "S17");
        displayNameToSmellId.put(UxSnifferBundle.message("inspection.too.many.props.name"), "S18");
        displayNameToSmellId.put(UxSnifferBundle.message("inspection.direct.dom.name"), "S21");
        displayNameToSmellId.put(UxSnifferBundle.message("inspection.force.update.name"), "S22");
        displayNameToSmellId.put(UxSnifferBundle.message("inspection.props.initial.state.name"), "S20");
        displayNameToSmellId.put(UxSnifferBundle.message("inspection.uncontrolled.component.name"), "S24");
        displayNameToSmellId.put(UxSnifferBundle.message("inspection.inheritance.name"), "S19");
        displayNameToSmellId.put(UxSnifferBundle.message("inspection.any.type.name"), "S29");
        displayNameToSmellId.put(UxSnifferBundle.message("inspection.non.null.assertion.name"), "S30");
        displayNameToSmellId.put(UxSnifferBundle.message("inspection.multiple.booleans.name"), "S33");
        displayNameToSmellId.put(UxSnifferBundle.message("inspection.enum.implicit.name"), "S31");
    }

    private void loadJson() {
        try (InputStream is = getClass().getResourceAsStream("/data/cost-mappings.json")) {
            if (is == null) return;
            JsonObject root = JsonParser.parseReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();

            JsonArray smellsArray = root.getAsJsonArray("smells");
            if (smellsArray != null) {
                for (JsonElement el : smellsArray) {
                    JsonObject obj = el.getAsJsonObject();
                    SmellInfo info = new SmellInfo(
                            obj.get("smellId").getAsString(),
                            obj.get("smellName").getAsString(),
                            obj.get("definition").getAsString(),
                            obj.get("severity").getAsString(),
                            obj.get("refactoring").getAsString()
                    );
                    smellsById.put(info.smellId(), info);
                }
            }

            JsonArray costsArray = root.getAsJsonArray("pafCosts");
            for (JsonElement el : costsArray) {
                JsonObject obj = el.getAsJsonObject();
                PafCost cost = new PafCost(
                        obj.get("costId").getAsString(),
                        obj.get("costName").getAsString(),
                        obj.get("pafCategory").getAsString(),
                        obj.get("definition").getAsString()
                );
                costsById.put(cost.costId(), cost);
            }

            JsonArray mappingsArray = root.getAsJsonArray("smellCostMappings");
            for (JsonElement el : mappingsArray) {
                JsonObject obj = el.getAsJsonObject();
                CostMapping mapping = new CostMapping(
                        obj.get("mappingId").getAsString(),
                        obj.get("smellId").getAsString(),
                        obj.get("smellName").getAsString(),
                        obj.get("costId").getAsString(),
                        obj.get("costName").getAsString(),
                        obj.get("relationshipType").getAsString(),
                        obj.get("causationLogic").getAsString(),
                        obj.get("triggerCondition").getAsString(),
                        obj.get("priority").getAsString()
                );
                mappingsBySmellId
                        .computeIfAbsent(mapping.smellId(), k -> new ArrayList<>())
                        .add(mapping);
            }
        } catch (Exception e) {
            // Silently fail — cost data is supplementary
        }
    }
}