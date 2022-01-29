package com.hc.local;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class LocalServerConfigurationType implements ConfigurationType {
    private static final String ID = "debug-run";
    private final LocalServerConfigurationFactory factory = new LocalServerConfigurationFactory(this);

    @Override
    @NotNull
    @Nls
    public String getDisplayName() {
        return "Debug";
    }

    @Override
    @Nls
    public String getConfigurationTypeDescription() {
        return this.getDisplayName() + " run configuration";
    }

    @Override
    public Icon getIcon() {
        return AllIcons.General.Information;
    }

    @Override
    @NotNull
    public String getId() {
        return "debug-run";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[]{this.factory};
    }

    public ConfigurationFactory getDefaultConfigurationFactory() {
        return this.factory;
    }

    private static class LocalServerConfigurationFactory extends ConfigurationFactory {
        LocalServerConfigurationFactory(@NotNull ConfigurationType type) {
            super(type);
        }

        @Override
        @NotNull
        public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
            return new LocalServerRunConfiguration(project, this, "debug");
        }
    }
}
