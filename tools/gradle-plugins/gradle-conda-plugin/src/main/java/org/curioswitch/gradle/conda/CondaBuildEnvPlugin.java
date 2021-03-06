/*
 * MIT License
 *
 * Copyright (c) 2019 Choko (choko@curioswitch.org)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.curioswitch.gradle.conda;

import static com.google.common.base.Preconditions.checkState;

import org.curioswitch.gradle.conda.exec.CondaExecUtil;
import org.curioswitch.gradle.helpers.platform.OperatingSystem;
import org.curioswitch.gradle.helpers.platform.PlatformHelper;
import org.curioswitch.gradle.tooldownloader.DownloadedToolManager;
import org.curioswitch.gradle.tooldownloader.ToolDownloaderPlugin;
import org.curioswitch.gradle.tooldownloader.util.DownloadToolUtil;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.Copy;

public class CondaBuildEnvPlugin implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    checkState(project.getParent() == null, "build-env plugin must be applied to root project.");
    var condas = project.getPlugins().apply(CondaPlugin.class).getCondas();
    condas.create(
        "miniconda-build",
        conda -> {
          conda.getVersion().set("Miniconda3-4.7.12.1");
          conda.getPackages().add("git");

          var platformHelper = new PlatformHelper();
          var os = platformHelper.getOs();
          switch (os) {
            case LINUX:
              conda
                  .getPackages()
                  .addAll(
                      "automake",
                      "autoconf",
                      "curl",
                      "gcc_linux-64",
                      "gxx_linux-64",
                      "gfortran_linux-64",
                      "make");
              break;
            case MAC_OSX:
              conda
                  .getPackages()
                  .addAll(
                      "automake",
                      "autoconf",
                      "curl",
                      "clang_osx-64",
                      "clangxx_osx-64",
                      "gfortran_osx-64",
                      "make");
              break;
            case WINDOWS:
              conda
                  .getPackages()
                  .addAll(
                      "m2-automake1.15",
                      "m2-curl",
                      "m2-autoconf",
                      "m2-make",
                      "m2w64-gcc",
                      "m2w64-gcc-fortran");
              break;
            default:
              throw new IllegalStateException("Unsupported os: " + os);
          }
        });
    CondaExecUtil.addExecToProject(project);

    if (new PlatformHelper().getOs() == OperatingSystem.MAC_OSX) {
      project
          .getPlugins()
          .withType(
              ToolDownloaderPlugin.class,
              plugin -> {
                plugin.registerToolIfAbsent(
                    "macos-sdk",
                    tool -> {
                      tool.getArtifact().set("MacOSX");
                      tool.getVersion().set("10.10");
                      tool.getBaseUrl()
                          .set("https://github.com/phracker/MacOSX-SDKs/releases/download/10.13/");
                      tool.getArtifactPattern().set("[artifact][revision].sdk.[ext]");
                      tool.getOsExtensions().getMac().set("tar.xz");
                    });
              });
      var downloadSdk = DownloadToolUtil.getDownloadTask(project, "macos-sdk");
      downloadSdk.configure(
          t ->
              t.setArchiveExtractAction(
                  archive -> {
                    project.exec(
                        exec -> {
                          exec.executable("tar");
                          exec.args(
                              "--strip-components",
                              "1",
                              "-xf",
                              archive.getAbsolutePath(),
                              "-C",
                              DownloadedToolManager.get(project).getToolDir("macos-sdk"));
                          exec.workingDir(archive.getParent());
                        });
                  }));

      // There doesn't seem to be a way to have CMake prefer the tools in Miniconda over system ones
      // without making sure the names match the unprefixed ones.
      var copyLlvmExecutables =
          project
              .getTasks()
              .register(
                  "copyBinUtils",
                  Copy.class,
                  t -> {
                    var minicondaPath =
                        DownloadedToolManager.get(project)
                            .getToolDir("miniconda-build")
                            .resolve("bin");
                    t.from(
                        minicondaPath,
                        copy -> {
                          copy.include("llvm-*");
                          copy.rename("llvm-(.*)", "$1");
                        });

                    t.into(minicondaPath);
                  });

      DownloadToolUtil.getSetupTask(project, "miniconda-build")
          .configure(
              t -> {
                t.dependsOn(downloadSdk, copyLlvmExecutables);
              });
    }
  }
}
