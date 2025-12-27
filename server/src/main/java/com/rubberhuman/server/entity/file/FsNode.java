package com.rubberhuman.server.entity.file;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("fs_node")
public class FsNode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long parentId; // 根目录为0
    private String fileName;
    private Boolean isDir; // true=目录, false=文件
    private Long storageId; // 关联物理表
    private Long fileSize;

    @TableLogic
    private Integer isDeleted;


    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
