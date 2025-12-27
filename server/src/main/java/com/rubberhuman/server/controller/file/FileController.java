package com.rubberhuman.server.controller.file;

import com.rubberhuman.server.entity.file.FsNode;
import com.rubberhuman.server.service.file.FileService;
import com.rubberhuman.server.service.auth.SysUserDetailsService;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private SysUserDetailsService sysUserService;

    // 获取文件列表
    // 例如：GET /api/file/list?parentId=0
    // 例如：GET /api/file/list?search=报告
    @GetMapping("/list")
    public ResponseEntity<List<FsNode>> list(
            @RequestParam(value = "parentId", defaultValue = "0") Long parentId,
            @RequestParam(value = "search", required = false) String search
    ) {
        Long userId = getCurrentUserId();
        List<FsNode> files = fileService.getFileList(userId, parentId, search);
        return ResponseEntity.ok(files);
    }

    // 新建文件夹
    @PostMapping("/folder")
    public ResponseEntity<?> createFolder(
            @RequestParam("parentId") Long parentId,
            @RequestParam("name") String name
    ) {
        fileService.createFolder(getCurrentUserId(), parentId, name);
        return ResponseEntity.ok(Map.of("message", "文件夹创建成功"));
    }

    // 上传文件
    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("parentId") Long parentId,
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        fileService.uploadFile(getCurrentUserId(), parentId, file);
        return ResponseEntity.ok(Map.of("message", "文件上传成功"));
    }

    // 下载文件
    @GetMapping("/download")
    public void download(@RequestParam("nodeId") Long nodeId, HttpServletResponse response) throws Exception {
        fileService.downloadFile(getCurrentUserId(), nodeId, response);
    }

    // 删除文件/目录
    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestParam("nodeId") Long nodeId) {
        fileService.deleteNode(getCurrentUserId(), nodeId);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    // 分享文件/目录
    @PostMapping("/share")
    public ResponseEntity<?> share(
            @RequestParam("nodeId") Long nodeId,
            @RequestParam("targetUsername") String targetUsername
    ) {
        fileService.shareFile(getCurrentUserId(), targetUsername, nodeId);
        return ResponseEntity.ok(Map.of("message", "分享成功"));
    }

    // 重命名
    @PostMapping("/rename")
    public ResponseEntity<?> rename(@RequestParam("nodeId") Long nodeId, @RequestParam("newName") String newName) {
        fileService.renameNode(getCurrentUserId(), nodeId, newName);
        return ResponseEntity.ok(Map.of("message", "重命名成功"));
    }

    // 辅助方法
    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("用户未登录");
        }
        String username = auth.getName();
        return sysUserService.getUserIdByUsername(username);
    }
}