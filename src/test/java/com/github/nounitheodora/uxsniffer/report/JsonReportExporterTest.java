package com.github.nounitheodora.uxsniffer.report;

import com.github.nounitheodora.uxsniffer.quality.CostMapper;
import com.github.nounitheodora.uxsniffer.scanner.SmellFinding;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.List;

public class JsonReportExporterTest extends BasePlatformTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ServiceContainerUtil.registerServiceInstance(
                ApplicationManager.getApplication(), CostMapper.class, new CostMapper());
    }

    // --- empty findings ---

    public void testGenerate_emptyFindings() {
        String json = JsonReportExporter.generate(List.of(), "TestProject", "/base");
        JsonObject root = new Gson().fromJson(json, JsonObject.class);

        assertEquals("TestProject", root.get("project").getAsString());
        assertEquals("/base", root.get("basePath").getAsString());
        assertEquals(0, root.get("totalFindings").getAsInt());
        assertTrue(root.has("timestamp"));
        assertTrue(root.getAsJsonArray("findings").isEmpty());
        assertTrue(root.getAsJsonArray("fileAnalysis").isEmpty());
    }

    public void testGenerate_emptyFindings_summaryZero() {
        String json = JsonReportExporter.generate(List.of(), "P", "/base");
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        JsonObject summary = root.getAsJsonObject("summary");

        assertEquals(0, summary.get("uniqueFilesAffected").getAsLong());
        assertTrue(summary.getAsJsonObject("smellCounts").entrySet().isEmpty());
    }

    // --- single finding ---

    public void testGenerate_singleFinding() {
        SmellFinding finding = new SmellFinding(
                "Large file", "/base/src/App.vue", "App.vue", "Too many lines");
        String json = JsonReportExporter.generate(List.of(finding), "MyApp", "/base");
        JsonObject root = new Gson().fromJson(json, JsonObject.class);

        assertEquals(1, root.get("totalFindings").getAsInt());

        JsonArray findings = root.getAsJsonArray("findings");
        assertEquals(1, findings.size());

        JsonObject f = findings.get(0).getAsJsonObject();
        assertEquals("Large file", f.get("smellName").getAsString());
        assertEquals("App.vue", f.get("fileName").getAsString());
        assertEquals("src/App.vue", f.get("filePath").getAsString());
        assertEquals("Too many lines", f.get("message").getAsString());
    }

    // --- relative path stripping ---

    public void testGenerate_relativePathStripping() {
        SmellFinding finding = new SmellFinding(
                "Large file", "/project/root/src/views/Dashboard.vue", "Dashboard.vue", "msg");
        String json = JsonReportExporter.generate(List.of(finding), "P", "/project/root");
        JsonObject root = new Gson().fromJson(json, JsonObject.class);

        JsonObject f = root.getAsJsonArray("findings").get(0).getAsJsonObject();
        assertEquals("src/views/Dashboard.vue", f.get("filePath").getAsString());
    }

    public void testGenerate_noBasePathKeepsAbsolute() {
        SmellFinding finding = new SmellFinding(
                "Large file", "/some/path/App.vue", "App.vue", "msg");
        String json = JsonReportExporter.generate(List.of(finding), "P", "");
        JsonObject root = new Gson().fromJson(json, JsonObject.class);

        JsonObject f = root.getAsJsonArray("findings").get(0).getAsJsonObject();
        assertEquals("/some/path/App.vue", f.get("filePath").getAsString());
    }

    // --- smell metadata ---

    public void testGenerate_smellIdResolved() {
        SmellFinding finding = new SmellFinding(
                "Large file", "/base/src/A.vue", "A.vue", "msg");
        String json = JsonReportExporter.generate(List.of(finding), "P", "/base");
        JsonObject root = new Gson().fromJson(json, JsonObject.class);

        JsonObject f = root.getAsJsonArray("findings").get(0).getAsJsonObject();
        assertEquals("S25", f.get("smellId").getAsString());
        assertTrue(f.has("severity"));
        assertTrue(f.has("refactoring"));
        assertTrue(f.has("costImpact"));
    }

    public void testGenerate_costImpactPresent() {
        SmellFinding finding = new SmellFinding(
                "Large file", "/base/src/A.vue", "A.vue", "msg");
        String json = JsonReportExporter.generate(List.of(finding), "P", "/base");
        JsonObject root = new Gson().fromJson(json, JsonObject.class);

        JsonObject f = root.getAsJsonArray("findings").get(0).getAsJsonObject();
        JsonArray costs = f.getAsJsonArray("costImpact");
        assertFalse(costs.isEmpty());

        JsonObject firstCost = costs.get(0).getAsJsonObject();
        assertTrue(firstCost.has("costId"));
        assertTrue(firstCost.has("costName"));
        assertTrue(firstCost.has("pafCategory"));
        assertTrue(firstCost.has("relationshipType"));
        assertTrue(firstCost.has("priority"));
    }

    // --- summary ---

    public void testGenerate_summarySmellCounts() {
        List<SmellFinding> findings = List.of(
                new SmellFinding("Large file", "/base/A.vue", "A.vue", "msg1"),
                new SmellFinding("Large file", "/base/B.vue", "B.vue", "msg2"),
                new SmellFinding("Too many props", "/base/C.vue", "C.vue", "msg3")
        );
        String json = JsonReportExporter.generate(findings, "P", "/base");
        JsonObject root = new Gson().fromJson(json, JsonObject.class);

        assertEquals(3, root.get("totalFindings").getAsInt());

        JsonObject summary = root.getAsJsonObject("summary");
        assertEquals(3, summary.get("uniqueFilesAffected").getAsLong());

        JsonObject counts = summary.getAsJsonObject("smellCounts");
        assertEquals(2, counts.get("Large file").getAsInt());
        assertEquals(1, counts.get("Too many props").getAsInt());
    }

    // --- file analysis ---

    public void testGenerate_fileAnalysisSortedBySmellCount() {
        List<SmellFinding> findings = List.of(
                new SmellFinding("Large file", "/base/A.vue", "A.vue", "msg"),
                new SmellFinding("Too many props", "/base/A.vue", "A.vue", "msg"),
                new SmellFinding("Large file", "/base/B.vue", "B.vue", "msg")
        );
        String json = JsonReportExporter.generate(findings, "P", "/base");
        JsonObject root = new Gson().fromJson(json, JsonObject.class);

        JsonArray fileAnalysis = root.getAsJsonArray("fileAnalysis");
        assertEquals(2, fileAnalysis.size());

        JsonObject first = fileAnalysis.get(0).getAsJsonObject();
        assertEquals(2, first.get("smellCount").getAsInt());
        assertEquals("A.vue", first.get("file").getAsString());

        JsonObject second = fileAnalysis.get(1).getAsJsonObject();
        assertEquals(1, second.get("smellCount").getAsInt());
    }

    public void testGenerate_fileAnalysisContainsSmellList() {
        List<SmellFinding> findings = List.of(
                new SmellFinding("Large file", "/base/A.vue", "A.vue", "msg1"),
                new SmellFinding("Any type usage", "/base/A.vue", "A.vue", "msg2")
        );
        String json = JsonReportExporter.generate(findings, "P", "/base");
        JsonObject root = new Gson().fromJson(json, JsonObject.class);

        JsonObject fileEntry = root.getAsJsonArray("fileAnalysis").get(0).getAsJsonObject();
        JsonArray smells = fileEntry.getAsJsonArray("smells");
        assertEquals(2, smells.size());
        assertEquals("Large file", smells.get(0).getAsString());
        assertEquals("Any type usage", smells.get(1).getAsString());
    }

    public void testGenerate_fileAnalysisTotalCostMappings() {
        SmellFinding finding = new SmellFinding(
                "Large file", "/base/A.vue", "A.vue", "msg");
        String json = JsonReportExporter.generate(List.of(finding), "P", "/base");
        JsonObject root = new Gson().fromJson(json, JsonObject.class);

        JsonObject fileEntry = root.getAsJsonArray("fileAnalysis").get(0).getAsJsonObject();
        assertTrue(fileEntry.get("totalCostMappings").getAsLong() > 0);
    }

    // --- valid JSON ---

    public void testGenerate_outputIsValidJson() {
        List<SmellFinding> findings = List.of(
                new SmellFinding("Large file", "/base/A.vue", "A.vue", "msg with \"quotes\" and 'apostrophes'")
        );
        String json = JsonReportExporter.generate(findings, "Project \"Test\"", "/base");
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        assertNotNull(root);
        assertEquals("Project \"Test\"", root.get("project").getAsString());
    }

    // --- unknown smell name ---

    public void testGenerate_unknownSmellNoMetadata() {
        SmellFinding finding = new SmellFinding(
                "Unknown smell XYZ", "/base/A.vue", "A.vue", "msg");
        String json = JsonReportExporter.generate(List.of(finding), "P", "/base");
        JsonObject root = new Gson().fromJson(json, JsonObject.class);

        JsonObject f = root.getAsJsonArray("findings").get(0).getAsJsonObject();
        assertEquals("Unknown smell XYZ", f.get("smellName").getAsString());
        assertFalse(f.has("smellId"));
        assertFalse(f.has("severity"));
        assertFalse(f.has("costImpact"));
    }

    // --- timestamp format ---

    public void testGenerate_timestampIsIsoFormat() {
        String json = JsonReportExporter.generate(List.of(), "P", "/base");
        JsonObject root = new Gson().fromJson(json, JsonObject.class);
        String timestamp = root.get("timestamp").getAsString();
        assertTrue(timestamp.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*"));
    }
}