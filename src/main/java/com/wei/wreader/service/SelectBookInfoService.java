package com.wei.wreader.service;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.wei.wreader.pojo.BookInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 已选择的小说基本信息实体数据持久化服务
 * @author weizhanjie
 */
@State(name = "SelectBookInfoService", storages = @Storage("/storages/select-book-info.xml"))
public class SelectBookInfoService implements PersistentStateComponent<SelectBookInfoService.State> {

    static class State {
        public BookInfo bookInfo;
    }

    private State selectBookInfoState = new State();

    @Nullable
    @Override
    public State getState() {
        return selectBookInfoState;
    }

    @Override
    public void loadState(@NotNull State state) {
        selectBookInfoState = state;
    }
}
