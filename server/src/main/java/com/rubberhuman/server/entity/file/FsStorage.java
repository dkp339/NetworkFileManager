package com.rubberhuman.server.entity.file;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("fs_storage")
public class FsStorage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String realPath; // 例如 D:/netdisk_storage/uuid.dat
    private String fileHash; // SHA256
    private Long fileSize;
    private String fileSuffix;
    private LocalDateTime createdAt;
}