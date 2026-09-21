package com.sc.plugin;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;


import java.io.IOException;

/**
 * IntelliJ action that copies an SVG file's source into a sibling Markdown file.
 */
public class SvgToMarkdownAction extends AnAction {
    /**
     * Runs presentation updates in the background thread because the update
     * logic reads the selected virtual file.
     *
     * @return the action update thread
     */
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    /**
     * Enables and shows the action only when exactly one SVG file is selected.
     *
     * @param event the IntelliJ action update event
     */
    @Override
    public void update(AnActionEvent event) {
        VirtualFile[] files = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        VirtualFile selectedFile = files != null && files.length == 1 ? files[0] : file;
        event.getPresentation().setEnabledAndVisible(isSvg(selectedFile));
    }

    /**
     * Creates or replaces a Markdown file beside the selected SVG file.
     * The write is executed as an IntelliJ command so it is tracked by the IDE.
     *
     * @param event the IntelliJ action invocation event
     */
    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        VirtualFile[] files = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
        VirtualFile svgFile = files != null && files.length == 1
                ? files[0]
                : event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (project == null || !isSvg(svgFile)) {
            return;
        }

        String markdownName = svgFile.getNameWithoutExtension() + ".md";
        WriteCommandAction.runWriteCommandAction(project, "Convert SVG to Markdown", null, () -> {
            try {
                VirtualFile markdownFile = svgFile.getParent().findChild(markdownName);
                if (markdownFile == null) {
                    markdownFile = svgFile.getParent().createChildData(this, markdownName);
                }
                VfsUtil.saveText(markdownFile, VfsUtil.loadText(svgFile));
                notify(project, "Created " + markdownName, NotificationType.INFORMATION);
            } catch (IOException exception) {
                notify(project, "Unable to create " + markdownName + ": " + exception.getMessage(), NotificationType.ERROR);
            }
        });
    }

    /**
     * Checks whether a virtual file is a regular SVG file.
     *
     * @param file the file to inspect
     * @return {@code true} when the file is non-null, not a directory, and has
     *         an SVG extension
     */
    private boolean isSvg(VirtualFile file) {
        return file != null && !file.isDirectory() && "svg".equalsIgnoreCase(file.getExtension());
    }

    /**
     * Sends a result notification to the current IntelliJ project.
     *
     * @param project the project that initiated the conversion
     * @param content the notification message
     * @param type the notification severity
     */
    private void notify(Project project, String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("SVG to Markdown")
                .createNotification(content, type)
                .notify(project);
    }
}
