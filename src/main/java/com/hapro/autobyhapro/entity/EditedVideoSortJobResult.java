package com.hapro.autobyhapro.entity;

import java.util.List;

public class EditedVideoSortJobResult {

    private final List<EditedVideoSortItemResult> itemResults;

    public EditedVideoSortJobResult(List<EditedVideoSortItemResult> itemResults) {
        this.itemResults = itemResults;
    }

    public List<EditedVideoSortItemResult> getItemResults() {
        return itemResults;
    }

    public int getTotalCount() {
        return itemResults.size();
    }

    public int getSuccessCount() {
        int count = 0;

        for (EditedVideoSortItemResult item : itemResults) {
            if (item.isSuccess()) {
                count++;
            }
        }

        return count;
    }

    public int getFailedCount() {
        return getTotalCount() - getSuccessCount();
    }
}