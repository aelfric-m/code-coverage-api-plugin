package io.jenkins.plugins.coverage.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import org.apache.commons.lang3.math.Fraction;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.DefaultLocale;

import edu.hm.hafner.metric.Coverage;
import edu.hm.hafner.metric.Coverage.CoverageBuilder;
import edu.hm.hafner.metric.FileNode;
import edu.hm.hafner.metric.Metric;
import edu.hm.hafner.metric.ModuleNode;
import edu.hm.hafner.metric.Node;
import edu.hm.hafner.metric.Value;
import edu.hm.hafner.util.FilteredLog;

import hudson.model.FreeStyleBuild;
import hudson.model.Run;

import static io.jenkins.plugins.coverage.metrics.testutil.CoverageStubs.*;
import static io.jenkins.plugins.coverage.metrics.testutil.JobStubs.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests the class {@link CoverageBuildAction}.
 *
 * @author Ullrich Hafner
 */
@DefaultLocale("en")
class CoverageBuildActionTest {
    private static final Locale LOCALE = Locale.US;

    private static final Fraction COVERAGE_FRACTION = Fraction.ONE_HALF;
    private static final CoveragePercentage COVERAGE_PERCENTAGE = CoveragePercentage.valueOf(COVERAGE_FRACTION);
    private static final Metric COVERAGE_METRIC = Metric.LINE;
    private static final Coverage VALUE = Coverage.valueOf(COVERAGE_METRIC, "1/2");

    private static final int COVERAGE_FILE_CHANGES = 5;
    private static final long COVERAGE_LINE_CHANGES = 10;

    @Test
    void shouldNotLoadResultIfCoverageValuesArePersistedInAction() {
        Node module = new ModuleNode("module");

        CoverageBuilder coverageBuilder = new CoverageBuilder();
        Coverage percent50 = coverageBuilder.setMetric(Metric.BRANCH).setCovered(1).setMissed(1).build();
        Coverage percent80 = coverageBuilder.setMetric(Metric.LINE).setCovered(8).setMissed(2).build();

        module.addValue(percent50);
        module.addValue(percent80);

        CoverageBuildAction action = spy(createEmptyAction(module));
        when(action.getResult()).thenThrow(new IllegalStateException("Result should not be accessed with getResult() when getting a coverage metric that is persisted in the build"));

        assertThat(action.getReferenceBuild()).isEmpty();

        assertThat(action.hasCoverage(Metric.LINE)).isTrue();
        assertThat(action.getCoverage(Metric.LINE)).isEqualTo(percent80);
        assertThat(action.getLineCoverage()).isEqualTo(percent80);
        assertThat(action.hasCoverage(Metric.BRANCH)).isTrue();
        assertThat(action.getCoverage(Metric.BRANCH)).isEqualTo(percent50);
        assertThat(action.getBranchCoverage()).isEqualTo(percent50);

        assertThat(action.hasCoverage(Metric.INSTRUCTION)).isFalse();
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> action.getCoverage(Metric.INSTRUCTION));

        assertThat(action.formatChangeCoverage(Metric.BRANCH)).isEqualTo("BRANCH: n/a");
        assertThat(action.formatChangeCoverageOverview()).isEqualTo("n/a");

        assertThat(action.formatIndirectCoverageChanges(Metric.BRANCH)).isEqualTo("BRANCH: n/a");
        assertThat(action.formatIndirectCoverageChangesOverview()).isEqualTo("n/a");

        assertThat(action.formatChangeCoverageDifference(Metric.BRANCH)).isEqualTo("n/a");
        assertThat(action.formatDelta(Metric.BRANCH)).isEqualTo("n/a");
    }

    private static CoverageBuildAction createEmptyAction(final Node module) {
        return new CoverageBuildAction(mock(FreeStyleBuild.class), createLog(), module,
                QualityGateStatus.INACTIVE, "-",
                new TreeMap<>(), List.of(),
                new TreeMap<>(), List.of(), false);
    }

    @Test
    void shouldNotLoadResultIfDeltasArePersistedInAction() {
        List<Value> coverages = new ArrayList<>();
        NavigableMap<Metric, Fraction> deltas = new TreeMap<>();

        CoverageBuilder coverageBuilder = new CoverageBuilder();

        Coverage percent50 = coverageBuilder.setMetric(Metric.BRANCH).setCovered(1).setMissed(1).build();
        coverages.add(percent50);
        deltas.put(Metric.BRANCH, percent50.getCoveredPercentage());

        Coverage percent80 = coverageBuilder.setMetric(Metric.LINE).setCovered(8).setMissed(2).build();
        coverages.add(percent80);

        CoverageBuildAction action = new CoverageBuildAction(mock(FreeStyleBuild.class), createLog(),
                new ModuleNode("module"), QualityGateStatus.INACTIVE, "-",
                deltas, coverages,
                deltas, coverages, false);

        CoverageBuildAction spy = spy(action);
        when(spy.getResult()).thenThrow(new IllegalArgumentException("Result should not be accessed with getResult() when getting a coverage metric that is persisted in the build"));

        assertThat(spy.hasChangeCoverage()).isTrue();
        assertThat(spy.hasChangeCoverage(Metric.LINE)).isTrue();
        assertThat(spy.getChangeCoverage(Metric.LINE)).isEqualTo(percent80);
        assertThat(spy.hasChangeCoverage(Metric.BRANCH)).isTrue();
        assertThat(spy.getChangeCoverage(Metric.BRANCH)).isEqualTo(percent50);

        assertThat(spy.hasIndirectCoverageChanges()).isTrue();
        assertThat(spy.hasIndirectCoverageChanges(Metric.LINE)).isTrue();
        assertThat(spy.getIndirectCoverageChanges(Metric.LINE)).isEqualTo(percent80);
        assertThat(spy.hasIndirectCoverageChanges(Metric.BRANCH)).isTrue();
        assertThat(spy.getIndirectCoverageChanges(Metric.BRANCH)).isEqualTo(percent50);

        assertThat(spy.hasChangeCoverageDifference(Metric.LINE)).isFalse();
        assertThat(spy.hasChangeCoverageDifference(Metric.BRANCH)).isTrue();
        assertThat(spy.getChangeCoverageDifference(Metric.BRANCH)).isEqualTo(percent50.getCoveredPercentage());

        assertThat(spy.hasDelta(Metric.LINE)).isFalse();
        assertThat(spy.hasDelta(Metric.BRANCH)).isTrue();
        assertThat(spy.getDelta()).contains(entry(Metric.BRANCH, percent50.getCoveredPercentage()));

        assertThat(spy.hasChangeCoverage()).isTrue();
        assertThat(spy.hasChangeCoverage(Metric.LINE)).isTrue();
        assertThat(spy.hasChangeCoverage(Metric.BRANCH)).isTrue();

        assertThat(spy.hasIndirectCoverageChanges()).isTrue();
        assertThat(spy.hasIndirectCoverageChanges(Metric.LINE)).isTrue();
        assertThat(spy.getIndirectCoverageChanges(Metric.LINE)).isEqualTo(percent80);
        assertThat(spy.hasIndirectCoverageChanges(Metric.BRANCH)).isTrue();
    }

    private static FilteredLog createLog() {
        return new FilteredLog("Errors");
    }

    @Test
    void shouldCreateViewModel() {
        Node root = new ModuleNode("top-level");
        CoverageBuildAction action = createEmptyAction(root);

        assertThat(action.getTarget()).extracting(CoverageViewModel::getNode).isSameAs(root);
        assertThat(action.getTarget()).extracting(CoverageViewModel::getOwner).isSameAs(action.getOwner());
    }

    @Test
    void shouldGetCoverageForSpecifiedMetric() {
        CoverageBuildAction action = createCoverageBuildAction();

        assertThat(action.hasCoverage(COVERAGE_METRIC)).isTrue();
        assertThat(action.getCoverage(COVERAGE_METRIC)).isEqualTo(VALUE);
        assertThat(action.formatCoverage(COVERAGE_METRIC)).isEqualTo("Line: 50.00%");
    }

    @Test
    void shouldGetCoverageDifferenceForSpecifiedMetric() {
        CoverageBuildAction action = createCoverageBuildAction();
        assertThat(action.hasDelta(COVERAGE_METRIC)).isTrue();
        assertThat(action.hasDelta(Metric.BRANCH)).isFalse();
        assertThat(action.getDelta())
                .hasSize(1)
                .containsKey(COVERAGE_METRIC)
                .containsValue(COVERAGE_FRACTION);
        assertThat(action.formatDelta(COVERAGE_METRIC))
                .isEqualTo(COVERAGE_PERCENTAGE.formatDeltaPercentage(LOCALE));
    }

    @Test
    void shouldGetChangeCoverageDifferences() {
        CoverageBuildAction action = createCoverageBuildAction();
        assertThat(action.hasChangeCoverageDifference(COVERAGE_METRIC)).isTrue();
        assertThat(action.getChangeCoverageDifference(COVERAGE_METRIC)).isEqualTo(COVERAGE_FRACTION);
    }

    @Test
    void shouldGetChangeCoverageForSpecifiedMetric() {
        CoverageBuildAction action = createChangeCoverageBuildAction();
        assertThat(action.hasChangeCoverage()).isTrue();
        assertThat(action.hasCodeChanges()).isTrue();
        assertThat(action.hasChangeCoverage(COVERAGE_METRIC)).isTrue();
        assertThat(action.getChangeCoverage(COVERAGE_METRIC)).isEqualTo(VALUE);
    }

    @Test
    void shouldGetIndirectCoverageChangesForSpecifiedMetric() {
        CoverageBuildAction action = createIndirectCoverageChangesBuildAction();
        assertThat(action.hasIndirectCoverageChanges()).isTrue();
        assertThat(action.hasIndirectCoverageChanges(COVERAGE_METRIC)).isTrue();
        assertThat(action.getIndirectCoverageChanges(COVERAGE_METRIC)).isEqualTo(VALUE);
    }

    @Test @Disabled("Refactor this test so that loading of result is not required")
    void shouldFormatChangeCoverage() {
        CoverageBuildAction action = createChangeCoverageBuildAction();
        assertThat(action.formatChangeCoverage(COVERAGE_METRIC)).isEqualTo(getFormattedLineCoverage());
        assertThat(action.formatChangeCoverageOverview()).isEqualTo(getFormattedLineCoverageOverview());
    }

    @Test @Disabled("Refactor this test so that loading of result is not required")
    void shouldFormatIndirectCoverageChanges() {
        CoverageBuildAction action = createIndirectCoverageChangesBuildAction();
        assertThat(action.formatIndirectCoverageChanges(COVERAGE_METRIC)).isEqualTo(getFormattedLineCoverage());
        assertThat(action.formatIndirectCoverageChangesOverview()).isEqualTo(getFormattedLineCoverageOverview());
    }

    @Test
    void shouldFormatChangeCoverageDifference() {
        CoverageBuildAction action = createChangeCoverageBuildAction();
        String expected = COVERAGE_PERCENTAGE.formatDeltaPercentage(LOCALE);
        assertThat(action.formatChangeCoverageDifference(COVERAGE_METRIC)).isEqualTo(expected);
    }

    /**
     * Creates a {@link CoverageBuildAction} which represents the coverage for the metric {@link #COVERAGE_METRIC} with
     * the value {@link #COVERAGE_PERCENTAGE}.
     *
     * @return the created action
     */
    private CoverageBuildAction createCoverageBuildAction() {
        ModuleNode root = new ModuleNode("Line");
        root.addValue(new CoverageBuilder().setMetric(COVERAGE_METRIC).setCovered(1).setMissed(1).build());
        return createCoverageBuildAction(root);
    }

    /**
     * Creates a {@link CoverageBuildAction} which represents the change coverage for the metric {@link
     * #COVERAGE_METRIC} with the value {@link #COVERAGE_PERCENTAGE}.
     *
     * @return the created action
     */
    private CoverageBuildAction createChangeCoverageBuildAction() {
        Node root = createChangeCoverageNode(COVERAGE_FRACTION, COVERAGE_METRIC,
                COVERAGE_FILE_CHANGES, COVERAGE_LINE_CHANGES);
        return createCoverageBuildAction(root);
    }

    /**
     * Creates a stub of {@link Node}, which represents the change coverage and provides information about it.
     *
     * @param changeCoverage
     *         The change coverage
     * @param metric
     *         The coverage metric
     * @param coverageFileChange
     *         The amount of files which contain indirect coverage changes
     * @param coverageLineChanges
     *         The amount of lines which contain indirect coverage changes
     *
     * @return the created stub
     */
    private Node createChangeCoverageNode(final Fraction changeCoverage, final Metric metric,
            final int coverageFileChange, final long coverageLineChanges) {
        var root = new ModuleNode("root");
        var builder = new CoverageBuilder().setMetric(Metric.LINE);
        for (int file = 0; file < 5; file++) {
            var fileNode = new FileNode("File-" + file);

            for (int line = 0; line < 2; line++) {
                fileNode.addCounters(10 + line, 1, 1);
                fileNode.addChangedLine(10 + line);
            }
            root.addChild(fileNode);
        }

        return root;
    }

    /**
     * Creates a {@link CoverageBuildAction} which represents the indirect coverage changes for the metric {@link
     * #COVERAGE_METRIC} with the value {@link #COVERAGE_PERCENTAGE}.
     *
     * @return the created action
     */
    private CoverageBuildAction createIndirectCoverageChangesBuildAction() {
        Node root = createIndirectCoverageChangesNode(COVERAGE_FRACTION, COVERAGE_METRIC,
                COVERAGE_FILE_CHANGES, COVERAGE_LINE_CHANGES);
        return createCoverageBuildAction(root);
    }

    /**
     * Creates a {@link CoverageBuildAction} with the passed {@link Node result}.
     *
     * @param root
     *         The result of the action
     *
     * @return the created action
     */
    private CoverageBuildAction createCoverageBuildAction(final Node root) {
        Run<?, ?> build = createBuild();

        NavigableMap<Metric, Fraction> deltas = new TreeMap<>();
        deltas.put(COVERAGE_METRIC, COVERAGE_FRACTION);
        var changeCoverage = List.of(VALUE);
        NavigableMap<Metric, Fraction> changeCoverageDifference = new TreeMap<>();
        changeCoverageDifference.put(COVERAGE_METRIC, COVERAGE_FRACTION);
        var indirectCoverageChanges = List.of(VALUE);

        return new CoverageBuildAction(build, createLog(), root, QualityGateStatus.INACTIVE, "-", deltas,
                changeCoverage, changeCoverageDifference, indirectCoverageChanges, false);
    }

    /**
     * Gets a formatted text representation of the line coverage {@link #COVERAGE_PERCENTAGE}.
     *
     * @return the formatted text
     */
    private String getFormattedLineCoverage() {
        return "LINE: " + COVERAGE_PERCENTAGE.formatPercentage(LOCALE);
    }

    /**
     * Gets a formatted text representation of the line coverage overview of {@link #COVERAGE_LINE_CHANGES} and {@link
     * #COVERAGE_FILE_CHANGES}.
     *
     * @return the formatted text
     */
    private String getFormattedLineCoverageOverview() {
        return COVERAGE_LINE_CHANGES + " lines (" + COVERAGE_FILE_CHANGES + " files) are affected";
    }
}
