package com.rubberhuman.server.entity.file;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("fs_audit_log")
public class FsAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String operation; // UPLOAD, DOWNLOAD, DELETE
    private String fileName;
    private String detail;
    private LocalDateTime opTime;
}