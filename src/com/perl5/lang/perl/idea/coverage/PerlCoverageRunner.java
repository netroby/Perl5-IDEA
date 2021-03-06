/*
 * Copyright 2015-2017 Alexandr Evstigneev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perl5.lang.perl.idea.coverage;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.intellij.coverage.CoverageEngine;
import com.intellij.coverage.CoverageRunner;
import com.intellij.coverage.CoverageSuite;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineCoverage;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.perl5.PerlBundle;
import com.perl5.lang.perl.util.PerlPluginUtil;
import com.perl5.lang.perl.util.PerlRunUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;
import java.util.Set;

public class PerlCoverageRunner extends CoverageRunner {
  private static final String COVER = "cover";
  private static final String COVER_LIB = "Devel::Cover";
  private static final Logger LOG = Logger.getInstance(PerlCoverageRunner.class);

  @Override
  public ProjectData loadCoverageData(@NotNull File sessionDataFile, @Nullable CoverageSuite baseCoverageSuite) {
    if (!(baseCoverageSuite instanceof PerlCoverageSuite)) {
      return null;
    }
    if (ApplicationManager.getApplication().isDispatchThread()) {
      final Ref<ProjectData> projectDataRef = new Ref<>();

      ProgressManager.getInstance().runProcessWithProgressSynchronously(
        () -> projectDataRef.set(doLoadCoverageData(sessionDataFile, (PerlCoverageSuite)baseCoverageSuite)),
        "Loading Coverage Data...", true, baseCoverageSuite.getProject());

      return projectDataRef.get();
    }
    else {
      return doLoadCoverageData(sessionDataFile, (PerlCoverageSuite)baseCoverageSuite);
    }
  }

  @Nullable
  private static ProjectData doLoadCoverageData(@NotNull File sessionDataFile, @NotNull PerlCoverageSuite perlCoverageSuite) {
    Project project = perlCoverageSuite.getProject();
    GeneralCommandLine perlCommandLine = ReadAction.compute(() -> {
      if (project.isDisposed()) {
        return null;
      }
      VirtualFile coverFile = PerlRunUtil.findLibraryScriptWithNotification(project, COVER, COVER_LIB);
      if (coverFile == null) {
        return null;
      }

      String libRoot = PerlPluginUtil.getPluginPerlLibRoot();
      if (libRoot == null) {
        return null;
      }

      GeneralCommandLine commandLine = PerlRunUtil.getPerlCommandLine(project, coverFile,
                                                                      "-I" + FileUtil.toSystemIndependentName(libRoot));
      if (commandLine == null) {
        return null; // fixme should be a notification
      }

      commandLine.addParameters(
        "--silent", "--nosummary", "-report", "camelcade", sessionDataFile.getAbsolutePath()
      );
      return commandLine;
    });

    if (perlCommandLine == null) {
      return null;
    }

    try {
      LOG.info("Loading coverage by: " + perlCommandLine.getCommandLineString());
      ProcessOutput output = ExecUtil.execAndGetOutput(perlCommandLine);
      if (output.getExitCode() != 0) {
        String errorMessage = output.getStderr();
        if (StringUtil.isEmpty(errorMessage)) {
          errorMessage = output.getStdout();
        }

        if (!StringUtil.isEmpty(errorMessage)) {
          showError(project, errorMessage);
        }
        return null;
      }
      String stdout = output.getStdout();
      if (StringUtil.isEmpty(stdout)) {
        return null;
      }

      try {
        PerlFileData[] filesData = new Gson().fromJson(stdout, PerlFileData[].class);
        if (filesData != null) {
          return parsePerlFileData(filesData);
        }
      }
      catch (JsonParseException e) {
        showError(project, e.getMessage());
        LOG.warn("Error parsing JSON", e);
      }
    }
    catch (ExecutionException e) {
      showError(project, e.getMessage());
      LOG.warn("Error loading coverage", e);
    }
    return null;
  }

  @NotNull
  private static ProjectData parsePerlFileData(@NotNull PerlFileData[] filesData) {
    ProjectData projectData = new ProjectData();
    for (PerlFileData perlFileData : filesData) {
      if (StringUtil.isEmpty(perlFileData.name) || perlFileData.lines == null) {
        continue;
      }
      ClassData classData = projectData.getOrCreateClassData(FileUtil.toSystemIndependentName(perlFileData.name));
      Set<Map.Entry<Integer, PerlLineData>> linesEntries = perlFileData.lines.entrySet();
      Integer maxLineNumber = linesEntries.stream().map(Map.Entry::getKey).max(Integer::compare).orElse(0);
      LineData[] linesData = new LineData[maxLineNumber + 1];
      for (Map.Entry<Integer, PerlLineData> lineEntry : linesEntries) {
        PerlLineData perlLineData = lineEntry.getValue();
        final Integer lineNumber = lineEntry.getKey();
        LineData lineData = new LineData(lineNumber, null) {
          @Override
          public int getStatus() {
            if (perlLineData.cover == 0) {
              return LineCoverage.NONE;
            }
            else if (perlLineData.cover < perlLineData.data) {
              return LineCoverage.PARTIAL;
            }
            return LineCoverage.FULL;
          }
        };
        lineData.setHits(perlLineData.cover);
        linesData[lineNumber] = lineData;
      }

      classData.setLines(linesData);
    }

    return projectData;
  }

  private static void showError(@NotNull Project project, @NotNull String message) {
    ReadAction.run(() -> {
      if (!project.isDisposed()) {
        Notifications.Bus.notify(
          new Notification(
            PerlBundle.message("perl.coverage.loading.error"),
            PerlBundle.message("perl.coverage.loading.error"),
            message,
            NotificationType.ERROR
          ),
          project
        );
      }
    });
  }

  @Override
  public String getPresentableName() {
    return PerlBundle.message("perl.perl5");
  }

  @Override
  public String getId() {
    return "Perl5CoverageRunner";
  }

  @Override
  public String getDataFileExtension() {
    return "json";
  }

  @Override
  public boolean acceptsCoverageEngine(@NotNull CoverageEngine engine) {
    return engine instanceof PerlCoverageEngine;
  }
}
