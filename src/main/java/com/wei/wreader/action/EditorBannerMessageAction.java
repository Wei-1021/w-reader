package com.wei.wreader.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import com.wei.wreader.pojo.BookInfo;
import com.wei.wreader.pojo.ChapterInfo;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;
import com.wei.wreader.utils.yml.ConfigYaml;
import com.wei.wreader.utils.ui.MessageDialogUtil;
import com.wei.wreader.utils.OperateActionUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import java.util.function.Function;

/**
 * 编辑器顶部提示信息
 *
 * @author weizhanjie
 */
public class EditorBannerMessageAction extends BaseAction implements EditorNotificationProvider {

    private BookInfo selectedBookInfo;
    private ChapterInfo selectedChapterInfo;
    private List<String> chapterContentList;
    private EditorNotificationPanel banner;

    @Override
    public void actionPerformed(AnActionEvent e) {
        super.actionPerformed(e);

        OperateActionUtil.getInstance(project).splitChapterContent();
        setBanner();
    }

    @Override
    public Function<? super FileEditor, ? extends JComponent> collectNotificationData(
            Project project, VirtualFile virtualFile) {
        cacheService = CacheService.getInstance();
        configYaml = ConfigYaml.getInstance();
        settings = cacheService.getSettings();
        if (settings == null) {
            settings = configYaml.getSettings();
            cacheService.setSettings(settings);
        }

        if (StringUtils.isBlank(settings.getCharset())) {
            settings.setCharset(configYaml.getSettings().getCharset());
            cacheService.setSettings(settings);
        }

        return fileEditor -> buildBannerComponent();
    }

    private JComponent buildBannerComponent() {
        setBanner();

        return banner;
    }

    private void setBanner() {
        if (banner == null) {
            banner = new EditorNotificationPanel();
        }

        int displayType = settings.getDisplayType();
        if (displayType != Settings.DISPLAY_TYPE_EDITOR_BANNER) {
            banner.setText("");
            banner.setVisible(false);
            return;
        }

        // 获取小说内容
        selectedBookInfo = cacheService.getSelectedBookInfo();
        // 获取小说内容
        selectedChapterInfo = cacheService.getSelectedChapterInfo();
        if (selectedChapterInfo == null) {
            banner.setText("");
            banner.setVisible(false);
            return;
        }
        // 获取小说内容
        chapterContentList = selectedChapterInfo.getChapterContentList();
        if (chapterContentList == null || chapterContentList.isEmpty()) {
            banner.setText("");
            banner.setVisible(false);
            return;
        }

        banner.setVisible(true);

        final int[] lastReadLineNum = {selectedChapterInfo.getLastReadLineNum()};
        final String[] content = {chapterContentList.get(lastReadLineNum[0])};

        banner.setText(content[0]);
        banner.setToolTipText(selectedBookInfo.getBookName() + "|" + selectedChapterInfo.getChapterTitle());
        banner.createActionLabel(MessageDialogUtil.TITLE_CANCEL, () -> {
            banner.setVisible(false);
        });
        banner.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                super.focusLost(e);
                if (lastReadLineNum[0] <= chapterContentList.size() - 1) {
                    lastReadLineNum[0] = lastReadLineNum[0] + 1;
                    selectedChapterInfo.setPrevReadLineNum(lastReadLineNum[0]);
                    selectedChapterInfo.setNextReadLineNum(lastReadLineNum[0] + 1);
                    selectedChapterInfo.setLastReadLineNum(lastReadLineNum[0] + 2);
                }

                content[0] = chapterContentList.get(lastReadLineNum[0]);
                banner.setText(content[0]);
                banner.updateUI();
            }
        });
        banner.updateUI();
    }

}
