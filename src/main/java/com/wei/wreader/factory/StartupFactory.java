package com.wei.wreader.factory;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class StartupFactory implements ProjectActivity {
    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        SwingUtilities.invokeLater(() -> {
            WReaderToolWindowFactory wReaderToolWindowFactory = new WReaderToolWindowFactory();
            wReaderToolWindowFactory.setEnabled(project);
            WReaderStatusBarFactory wReaderStatusBarFactory = new WReaderStatusBarFactory();
            wReaderStatusBarFactory.setEnabled(project, true);
        });
        return null;
    }
}
