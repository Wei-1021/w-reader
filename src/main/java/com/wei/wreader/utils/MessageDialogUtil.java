package com.wei.wreader.utils;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import java.awt.*;

/**
 * 消息对话框工具类
 *
 * @author weizhanjie
 */
public class MessageDialogUtil {

    public static final String TITLE_ERROR = "错误";
    public static final String TITLE_INFO = "提示";
    public static final String TITLE_WARN = "警告";
    public static final String TITLE_SUCCESS = "成功";
    public static final String TITLE_CONFIRM = "确认";
    public static final String TITLE_CANCEL = "取消";
    public static final String TITLE_YES = "是";
    public static final String TITLE_NO = "否";
    public static final String TITLE_LOADING = "加载中";
    public static final String TITLE_LOAD_FAIL = "加载失败";
    public static final String TITLE_LOAD_SUCCESS = "加载成功";
    public static final String TITLE_HELP = "帮助";
    public static final String HELP_LOAD_FAIL = "帮助文件加载失败";

    /**
     * 显示消息对话框
     *
     * @param project
     * @param title
     * @param message
     * @param okRunnable 确认按钮点击事件
     */
    public static DialogBuilder showMessageDialog(Project project,
                                                  String title,
                                                  String message,
                                                  Runnable okRunnable) {
        return showMessageDialog(project, title, message, okRunnable, null);
    }

    /**
     * 显示消息对话框
     *
     * @param project
     * @param title
     * @param message
     * @param okRunnable      确认按钮点击事件
     * @param cancelOperation 取消按钮点击事件
     */
    public static DialogBuilder showMessageDialog(Project project,
                                                  String title,
                                                  String message,
                                                  Runnable okRunnable,
                                                  Runnable cancelOperation) {
        // 中心组件
        JPanel dialogPanel = new JPanel();
        FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
        dialogPanel.setLayout(flowLayout);
        JTextPane messageLabel = new JTextPane();
        messageLabel.setText(message);
        dialogPanel.add(messageLabel);

        return showMessageDialog(project, title, dialogPanel, okRunnable, cancelOperation);
    }

    /**
     * 显示消息对话框
     *
     * @param project
     * @param title
     * @param centerPanel
     * @param okRunnable  确认按钮点击事件
     */
    public static DialogBuilder showMessageDialog(Project project,
                                                  String title,
                                                  JComponent centerPanel,
                                                  Runnable okRunnable) {
        return showMessageDialog(project, title, centerPanel, okRunnable, null);
    }

    /**
     * 显示消息对话框
     *
     * @param project    Project
     * @param title      标题
     * @param objs       显示内容的组件集合
     * @param okRunnable 确认按钮点击事件
     */
    public static DialogBuilder showMessageDialog(Project project,
                                                  String title,
                                                  Object[] objs,
                                                  Runnable okRunnable) {
        JPanel centerPanel = new JPanel();
        GridLayoutManager layout = new GridLayoutManager(objs.length, 1);
        centerPanel.setLayout(layout);
        centerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0, len = objs.length; i < len; i++) {
            Object obj = objs[i];
            GridConstraints gridConstraints = new GridConstraints();
            gridConstraints.setRow(i);
            gridConstraints.setColumn(0);
            if (obj instanceof JComponent) {
                centerPanel.add((JComponent) obj, gridConstraints);
            } else {
                centerPanel.add(new JLabel(obj.toString()), gridConstraints);
            }
        }

        return showMessageDialog(project, title, centerPanel, okRunnable, null);
    }

    /**
     * 显示消息对话框
     *
     * @param project    Project
     * @param title      标题
     * @param objs       显示内容的组件集合
     * @param okRunnable 确认按钮点击事件
     */
    public static DialogBuilder showMessageDialog(Project project,
                                                  String title,
                                                  Object[] objs,
                                                  int width, int height,
                                                  Runnable okRunnable) {
        JPanel centerPanel = new JPanel();
//        GridLayoutManager layout = new GridLayoutManager(objs.length, 1);
        BoxLayout boxLayout = new BoxLayout(centerPanel, BoxLayout.Y_AXIS);
        centerPanel.setLayout(boxLayout);
        centerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0, len = objs.length; i < len; i++) {
            Object obj = objs[i];
            GridConstraints gridConstraints = new GridConstraints();
            gridConstraints.setRow(i);
            gridConstraints.setColumn(0);
            if (obj instanceof JComponent) {
                centerPanel.add((JComponent) obj, gridConstraints);
            } else {
                centerPanel.add(new JLabel(obj.toString()), gridConstraints);
            }
        }

        return showMessageDialog(project, title, centerPanel, okRunnable, null, width, height);
    }

    /**
     * 显示消息对话框
     *
     * @param project         Project
     * @param title           标题
     * @param objs            显示内容的组件集合
     * @param okRunnable      确认按钮点击事件
     * @param cancelOperation 取消按钮点击事件
     */
    public static DialogBuilder showMessageDialog(Project project,
                                                  String title,
                                                  Object[] objs,
                                                  Runnable okRunnable,
                                                  Runnable cancelOperation) {
        JPanel centerPanel = new JPanel();
        GridLayoutManager layout = new GridLayoutManager(objs.length, 1);
        centerPanel.setLayout(layout);
        centerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0, len = objs.length; i < len; i++) {
            Object obj = objs[i];
            GridConstraints gridConstraints = new GridConstraints();
            gridConstraints.setRow(i);
            gridConstraints.setColumn(0);
            if (obj instanceof JComponent) {
                centerPanel.add((JComponent) obj, gridConstraints);
            } else {
                centerPanel.add(new JLabel(obj.toString()), gridConstraints);
            }
        }

        return showMessageDialog(project, title, centerPanel, okRunnable, cancelOperation);
    }

    /**
     * 显示消息对话框
     *
     * @param project
     * @param title
     * @param centerPanel
     * @param okRunnable      确认按钮点击事件
     * @param cancelOperation 取消按钮点击事件
     */
    public static DialogBuilder showMessageDialog(Project project,
                                                  String title,
                                                  JComponent centerPanel,
                                                  Runnable okRunnable,
                                                  Runnable cancelOperation) {
        return showMessageDialog(project, title, centerPanel, okRunnable, cancelOperation, 0, 0);
    }

    /**
     * 显示消息对话框
     *
     * @param project
     * @param title
     * @param centerPanel
     * @param okRunnable      确认按钮点击事件
     * @param cancelOperation 取消按钮点击事件
     */
    public static DialogBuilder showMessageDialog(Project project,
                                                  String title,
                                                  JComponent centerPanel,
                                                  Runnable okRunnable,
                                                  Runnable cancelOperation,
                                                  int width, int height) {

        DialogBuilder builder = new DialogBuilder(project);
        builder.centerPanel(centerPanel);
        builder.title(title);
        builder.addOkAction();
        builder.addCancelAction();
        if (width > 0 && height > 0) {
            builder.getDialogWrapper().setSize(width, height);
        }
        builder.show();

        DialogWrapper dialogWrapper = builder.getDialogWrapper();
        if (dialogWrapper != null) {
            if (dialogWrapper.isOK()) {
                if (okRunnable != null) {
                    // 确认按钮点击事件
                    okRunnable.run();
                }
            } else {
                if (cancelOperation != null) {
                    // 取消按钮点击事件
                    cancelOperation.run();
                }
            }
        }

        return builder;
    }

    /**
     * 显示消息对话框
     *
     * @param project
     * @param title
     * @param message
     */
    public static DialogBuilder showMessage(Project project,
                                            String title,
                                            String message) {
        // 中心组件
        JPanel dialogPanel = new JPanel();
        FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
        dialogPanel.setLayout(flowLayout);

        JTextPane messageLabel = new JTextPane();
        messageLabel.setText(message);

        dialogPanel.add(messageLabel);
        return showMessage(project, title, dialogPanel);
    }

    /**
     * 显示消息对话框
     *
     * @param project
     * @param title
     * @param message
     */
    public static DialogBuilder showMessageHTML(Project project,
                                                String title,
                                                String message) {
        // 中心组件
        JPanel dialogPanel = new JPanel();
        FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT);
        dialogPanel.setLayout(flowLayout);

        JTextPane messageLabel = new JTextPane();
        messageLabel.setContentType("text/html");
        messageLabel.setText(message);

        dialogPanel.add(messageLabel);
        return showMessage(project, title, dialogPanel);
    }


    /**
     * 显示消息对话框
     *
     * @param project Project
     * @param title   标题
     * @param objs    显示内容的组件集合
     */
    public static DialogBuilder showMessage(Project project,
                                            String title,
                                            Object[] objs) {
        JPanel centerPanel = new JPanel();
        GridLayoutManager layout = new GridLayoutManager(1, objs.length);
        centerPanel.setLayout(layout);
        centerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0, len = objs.length; i < len; i++) {
            Object obj = objs[i];
            GridConstraints gridConstraints = new GridConstraints();
            gridConstraints.setRow(0);
            gridConstraints.setColumn(i);
            if (obj instanceof JComponent) {
                centerPanel.add((JComponent) obj, gridConstraints);
            } else {
                centerPanel.add(new JLabel(obj.toString()), gridConstraints);
            }
        }

        return showMessage(project, title, centerPanel);
    }

    /**
     * 显示消息对话框
     *
     * @param project
     * @param title
     * @param centerPanel
     */
    public static DialogBuilder showMessage(Project project,
                                            String title,
                                            JComponent centerPanel) {

        DialogBuilder builder = new DialogBuilder(project);
        builder.centerPanel(centerPanel);
        builder.title(title);
        builder.show();

        return builder;
    }

    /**
     * 显示消息对话框
     *
     * @param project
     * @param title
     * @param centerPanel
     */
    public static DialogBuilder showMessage(Project project,
                                            String title,
                                            JComponent centerPanel,
                                            int width, int height) {

        DialogBuilder builder = new DialogBuilder(project);
        builder.centerPanel(centerPanel);
        builder.title(title);
        builder.getDialogWrapper().setSize(width, height);
        builder.show();

        return builder;
    }


    /**
     * 显示消息对话框
     *
     * @param project
     * @param title
     * @param centerPanel
     */
    public static DialogBuilder showMessage(Project project,
                                            String title,
                                            JComponent centerPanel,
                                            int width, int height,
                                            boolean isRemoveAllActions) {

        DialogBuilder builder = new DialogBuilder(project);
        builder.centerPanel(centerPanel);
        builder.title(title);
        builder.getDialogWrapper().setSize(width, height);
        if (isRemoveAllActions) {
            builder.removeAllActions();
        }
        builder.show();

        return builder;
    }

    public static void showEditorMessage(Editor editor, JComponent msgComponent) {
        HintManager hintManager = HintManager.getInstance();
        hintManager.showInformationHint(editor, msgComponent);
    }

}
