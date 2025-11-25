package com.wei.wreader.utils;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.wei.wreader.pojo.Settings;
import com.wei.wreader.service.CacheService;

import javax.swing.*;

public class WReaderIcons {
    public static final Icon FONT_SIZE_SUB = IconLoader.getIcon("/icon/font_sub.svg", WReaderIcons.class);
    public static final Icon FONT_SIZE_ADD = IconLoader.getIcon("/icon/font_add.svg", WReaderIcons.class);
    public static final Icon MAIN_ICON = IconLoader.getIcon("/icon/mainIcon.svg", WReaderIcons.class);
    public static final Icon MAIN_ICON_BLEAK = IconLoader.getIcon("/icon/mainIcon_bleak.svg", WReaderIcons.class);
    public static final Icon AUTO_READ_PLAY = IconLoader.getIcon("/icon/auto_read_play.svg", WReaderIcons.class);
    public static final Icon AUTO_READ_STOP = IconLoader.getIcon("/icon/auto_read_stop.svg", WReaderIcons.class);
    public static final Icon AUDIO_READING_PLAY = IconLoader.getIcon("/icon/audio_reading_play.svg", WReaderIcons.class);
    public static final Icon LISTEN_BOOK = IconLoader.getIcon("/icon/listen_book.svg", WReaderIcons.class);
    public static final Icon BOOK_INFO = IconLoader.getIcon("/icon/book_info.svg", WReaderIcons.class);
    public static final Icon CUSTOM_SITE = IconLoader.getIcon("/icon/custom_site.svg", WReaderIcons.class);

    /**
     * 获取主图标
     *
     * @param project
     * @return
     */
    public static Icon getMainIcon(Project project) {
        if (project == null || project.isDisposed()) {
            return MAIN_ICON;
        }

        CacheService cacheService = CacheService.getInstance();
        Settings settings = cacheService.getSettings();
        if (settings == null) {
            return MAIN_ICON;
        }

        if (settings.getMainIconStyle() == 2) {
            return MAIN_ICON_BLEAK;
        }

        return MAIN_ICON;
    }



}