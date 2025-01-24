package com.wei.wreader.pojo;

import com.intellij.openapi.project.Project;

import javax.swing.*;

public class MessageDialogPojo {
    private Project project;
    private String title;
    private JComponent centerPanel;
    private Object[] objs;
    private Runnable okRunnable;
    private Runnable cancelOperation;
    
    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public JComponent getCenterPanel() {
        return centerPanel;
    }

    public void setCenterPanel(JComponent centerPanel) {
        this.centerPanel = centerPanel;
    }

    public Runnable getOkRunnable() {
        return okRunnable;
    }

    public void setOkRunnable(Runnable okRunnable) {
        this.okRunnable = okRunnable;
    }

    public Runnable getCancelOperation() {
        return cancelOperation;
    }

    public void setCancelOperation(Runnable cancelOperation) {
        this.cancelOperation = cancelOperation;
    }
}
