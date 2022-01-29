package com.hc.local;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.jar.JarApplicationCommandLineState;
import com.intellij.execution.jar.JarApplicationConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task.Modal;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType;
import org.jetbrains.idea.maven.execution.MavenRunnerParameters;
import org.jetbrains.idea.maven.model.MavenExplicitProfiles;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LocalServerCommandLineState extends JarApplicationCommandLineState {
    private static Logger logger = Logger.getInstance(LocalServerCommandLineState.class);
    private final Lock lock;
    private final Condition condition;

    public LocalServerCommandLineState(@NotNull LocalServerRunConfiguration configuration, ExecutionEnvironment environment) {
        super(configuration, environment);
        this.lock = new ReentrantLock();
        this.condition = this.lock.newCondition();
    }

    @Override
    @NotNull
    public ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner runner) throws ExecutionException {
        this.runMavenPackageTask();
        return super.execute(executor, runner);
    }

    private boolean runMavenPackageTask() {
        MyTask task = new MyTask(this, ((JarApplicationConfiguration) this.getConfiguration()).getProject(), "Project Packaging Is in Progress...", true);
        task.queue();
        return task.isSuccess();
    }

    private void runMavenPackage(@NotNull ProgressIndicator indicator, MavenRunnerParameters params, Project project) {
        MavenRunConfigurationType.runConfiguration(project, params, (descriptor) -> {
            this.runMavenPackageCallback(indicator, descriptor);
        });
        this.doWait();
    }

    private void doWait() {
        this.lock.lock();

        try {
            this.condition.await(3L, TimeUnit.MINUTES);
        } catch (InterruptedException var5) {
            Thread.currentThread().interrupt();
        } finally {
            this.lock.unlock();
        }

    }

    private void runMavenPackageCallback(@NotNull ProgressIndicator indicator, RunContentDescriptor descriptor) {
        if (descriptor.getProcessHandler() != null) {
            try {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    while (!descriptor.getProcessHandler().isProcessTerminated() && !indicator.isCanceled()) {
                        try {
                            Thread.sleep(1000L);
                        } catch (InterruptedException var8) {
                            var8.printStackTrace();
                        }
                    }

                    this.lock.lock();

                    try {
                        this.condition.signalAll();
                    } catch (Exception var9) {
                        var9.printStackTrace();
                    } finally {
                        this.lock.unlock();
                    }

                });
            } catch (Throwable var4) {
                var4.printStackTrace();
            }
        }

    }

    public class MyTask extends Modal {
        private boolean success;
        private LocalServerCommandLineState $this;

        public MyTask(@Nullable LocalServerCommandLineState this$0, @Nls @NotNull Project project, String title, boolean canBeCancelled) {
            super(project, title, canBeCancelled);
            this.$this = this$0;
            this.success = true;
        }

        public boolean isSuccess() {
            return this.success;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            // download
            MavenProjectsManager mavenProjectsManager = MavenProjectsManager.getInstance(this.getProject());
            List<MavenProject> rootProjects = mavenProjectsManager.getRootProjects();
            Optional<MavenProject> mavenProjectOptional = rootProjects.stream().filter((px) -> {
                return Objects.equals(px.getName(), this.getProject().getName());
            }).findAny();
            if (mavenProjectOptional.isPresent()) {
                MavenProject p = (MavenProject)mavenProjectOptional.get();
                MavenExplicitProfiles explicitProfiles = mavenProjectsManager.getExplicitProfiles();
                MavenRunnerParameters params = new MavenRunnerParameters(true, p.getDirectory(), p.getFile().getName(), Arrays.asList("clean", "package", "-DskipTests", "-U"), explicitProfiles.getEnabledProfiles(), explicitProfiles.getDisabledProfiles());

                try {
                    this.$this.runMavenPackage(indicator, params, this.getProject());
                } catch (Throwable var12) {
                    try {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            MavenRunConfigurationType.runConfiguration(this.getProject(), params, (descriptor) -> {
                                this.$this.runMavenPackageCallback(indicator, descriptor);
                            });
                        }, ModalityState.any());
                        this.$this.doWait();
                    } catch (Throwable var11) {
                        this.success = false;
                        throw new ProcessCanceledException();
                    }
                }
            }

        }

        @Override
        public void onCancel() {
            this.$this.lock.lock();

            try {
                this.$this.condition.signalAll();
            } catch (Exception var5) {
                var5.printStackTrace();
            } finally {
                this.$this.lock.unlock();
            }

        }
    }
}
