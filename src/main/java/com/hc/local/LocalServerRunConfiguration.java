package com.hc.local;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.jar.JarApplicationConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class LocalServerRunConfiguration extends JarApplicationConfiguration {
    public static final String DEFAULT_VM_PARAMETERS_FORMAT = " -Dspring.config.name=application,%s";
    private Project project;

    public LocalServerRunConfiguration(Project project, ConfigurationFactory factory, String name) {
        super(project, factory, name);
        this.project = project;
    }

    @Override
    @Nullable
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        return new LocalServerCommandLineState(this, environment);
    }

    @Override
    public String getVMParameters() {
        return super.getVMParameters() + this.getDefaultVmParameters();
    }

    protected String getDefaultVmParameters() {
        this.setJarPath("~/cloud-base-starter.jar");
        String defaultParameters = String.format(DEFAULT_VM_PARAMETERS_FORMAT, this.project.getName());
        defaultParameters = defaultParameters + " -Dlocal.test.enabled=true";

        return defaultParameters;
    }

}
