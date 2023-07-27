package io.jenkins.plugins.coverage.metrics.steps;

import java.util.List;

import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import io.jenkins.plugins.coverage.metrics.model.FileWithChangedLinesCoverageModel;

@ExportedBean
public class ChangedAndCoveredLinesApi {
    private final List<FileWithChangedLinesCoverageModel> filesWithChangedLines;

    public ChangedAndCoveredLinesApi(final List<FileWithChangedLinesCoverageModel> filesWithChangedLines
    ) {

        this.filesWithChangedLines = filesWithChangedLines;
    }
@Exported(inline = true)
    public List<FileWithChangedLinesCoverageModel> getFilesWithChangedLines() {
        return filesWithChangedLines;
    }
}
