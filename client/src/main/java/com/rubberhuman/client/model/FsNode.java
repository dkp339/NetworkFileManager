package com.rubberhuman.client.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class FsNode {
    private Long id;
    private Long userId;
    private Long parentId;
    private String fileName;
    private Boolean isDir;
    private Long storageId;
    private Long fileSize;
    private String createTime; // 接收 JSON 里的时间字符串
    private String updateTime;

    // --- 辅助方法：给表格显示用的 ---

    public String getType() {
        return isDir ? "文件夹" : "文件";
    }

    public String getSizeStr() {
        if (isDir) return "-";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.2f KB", fileSize / 1024.0);
        return String.format("%.2f MB", fileSize / (1024.0 * 1024));
    }

    public String getFormatTime() {
        return createTime != null ? createTime.replace("T", " ") : "";
    }
}