package com.rubberhuman.server.service.file;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.rubberhuman.server.entity.file.FsAuditLog;
import com.rubberhuman.server.entity.file.FsNode;
import com.rubberhuman.server.entity.file.FsStorage;
import com.rubberhuman.server.exception.BusinessException;
import com.rubberhuman.server.service.auth.SysUserDetailsService;
import com.rubberhuman.server.util.FileCryptoUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.rubberhuman.server.mapper.file.FsAuditLogMapper;
import com.rubberhuman.server.mapper.file.FsNodeMapper;
import com.rubberhuman.server.mapper.file.FsStorageMapper;
import com.rubberhuman.server.util.EncryptionUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    @Value("${file.storage.path}")
    private String storagePath;

    @Autowired
    private FsNodeMapper nodeMapper;

    @Autowired
    private FsStorageMapper storageMapper;

    @Autowired
    private FsAuditLogMapper logMapper;

    @Autowired
    private EncryptionUtil encryptionUtil;

    @Autowired
    private FileCryptoUtil fileCryptoUtil;
    @Autowired
    private SysUserDetailsService sysUserDetailsService;

    // 获取文件列表 + 搜索
    public List<FsNode> getFileList(Long userId, Long parentId, String searchName) {
        QueryWrapper<FsNode> query = new QueryWrapper<>();
        query.eq("user_id", userId);

        // 如果有搜索词，就忽略目录层级，全局搜索
        if (searchName != null && !searchName.trim().isEmpty()) {
            query.like("file_name", searchName);
        } else {
            // 否则按目录层级查
            query.eq("parent_id", parentId);
        }
        query.orderByDesc("is_dir"); // 文件夹排前面
        return nodeMapper.selectList(query);
    }

    // 创建文件夹
    public void createFolder(Long userId, Long parentId, String folderName) {
        Long count = nodeMapper.selectCount(new QueryWrapper<FsNode>()
                .eq("user_id", userId)
                .eq("parent_id", parentId)
                .eq("file_name", folderName)
                .eq("is_dir", true));

        if (count > 0) {
            throw new BusinessException("该目录下已存在同名文件夹");
        }

        FsNode node = new FsNode();
        node.setUserId(userId);
        node.setParentId(parentId);
        node.setFileName(folderName);
        node.setIsDir(true);
        node.setCreateTime(LocalDateTime.now());
        nodeMapper.insert(node);
    }

    @Transactional(rollbackFor = Exception.class)
    public void uploadFile(Long userId, Long parentId, MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            throw new BusinessException("不能上传空文件");
        }

        String originalFilename = file.getOriginalFilename();
        long size = file.getSize();

        String fileHash;
        try (InputStream in = file.getInputStream()) {
            fileHash = DigestUtils.sha256Hex(in);
        }

        // 使用文件哈希查询数据库
        // 若文件存在于数据库中 -> 用 FsNode 逻辑指向代表上传
        // 若不存在于数据库之中 -> 加密上传，写入磁盘，再用 FsNode 逻辑指向
        FsStorage storage = storageMapper.selectOne(new QueryWrapper<FsStorage>().eq("file_hash", fileHash));
        Path targetPath = null;
        if (storage == null) {
            String realName = UUID.randomUUID() + ".dat";
            File dir = new File(storagePath);
            if (!dir.exists()) dir.mkdirs();

            targetPath = Paths.get(storagePath, realName);

            // 流式加密文件
            try (InputStream in = file.getInputStream();
                 OutputStream out = Files.newOutputStream(targetPath)) {
                fileCryptoUtil.encrypt(in, out);
            } catch (Exception e) {
                // 写盘失败，清理垃圾文件
                Files.deleteIfExists(targetPath);
                throw new BusinessException("文件写入硬盘失败", e);
            }

            storage = new FsStorage();
            storage.setRealPath(targetPath.toString());
            storage.setFileHash(fileHash);
            storage.setFileSize(size);
            storage.setFileSuffix(getSuffix(originalFilename));
            storage.setCreatedAt(LocalDateTime.now());
            storageMapper.insert(storage);
        }

        // 插入逻辑节点 FsNode
        try {
            FsNode node = new FsNode();
            node.setUserId(userId);
            node.setParentId(parentId);
            node.setFileName(originalFilename);
            node.setIsDir(false);
            node.setStorageId(storage.getId());
            node.setFileSize(size);
            node.setCreateTime(LocalDateTime.now());
            nodeMapper.insert(node);

            saveLog(userId, "UPLOAD", originalFilename, "Hash=" + fileHash);
        } catch (Exception e) {
            // 如果 targetPath == null，则说明 file 没有写入磁盘 -> 继续抛出异常触发 @Transactional 回滚数据库
            // 否则说明 file 写入磁盘，需要删除文件
            if (targetPath != null) {
                try {
                    Files.deleteIfExists(targetPath);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        }
    }

    public void downloadFile(Long userId, Long nodeId, HttpServletResponse response) throws Exception {
        FsNode node = nodeMapper.selectById(nodeId);
        if (node == null || node.getIsDir()) throw new BusinessException("文件不存在或无法下载");
        if (!node.getUserId().equals(userId)) throw new BusinessException("无权访问");

        FsStorage storage = storageMapper.selectById(node.getStorageId());
        Path path = Paths.get(storage.getRealPath());
        if (!Files.exists(path)) throw new BusinessException("物理文件丢失");

        // 设置响应头
        response.setContentType("application/octet-stream");
        String encodedName =java.net.URLEncoder.encode(node.getFileName(), StandardCharsets.UTF_8);
        encodedName = encodedName.replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
        response.setContentLengthLong(node.getFileSize());

        response.setContentType("application/octet-stream");

        // 流式解密文件
        try (InputStream in = Files.newInputStream(path);
             OutputStream out = response.getOutputStream()) {
            fileCryptoUtil.decrypt(in, out);
        }

        saveLog(userId, "DOWNLOAD", node.getFileName(), "Size=" + node.getFileSize());
    }


    // 逻辑删除文件，需要递归删除子结构
    @Transactional(rollbackFor = Exception.class)
    public void deleteNode(Long userId, Long nodeId) {

        FsNode node = nodeMapper.selectById(nodeId);
        if (node == null) {
            throw new BusinessException("文件不存在");
        }
        if (!node.getUserId().equals(userId)) {
            throw new BusinessException("无权删除");
        }

        // 防止只删除文件夹节点，留下文件夹内的孤儿节点
        deleteRecursive(nodeId, userId);

        // 只记录被用户点的那个节点
        saveLog(userId, "DELETE", node.getFileName(), "ID=" + nodeId);
    }

    // 文件分享，需要将子结构完全拷贝
    @Transactional(rollbackFor = Exception.class)
    public void shareFile(Long sourceUserId, String targetUsername, Long sourceNodeId) {
        Long targetUserId = sysUserDetailsService.getUserIdByUsername(targetUsername);
        if (targetUserId == null) {
            throw new BusinessException("目标用户不存在");
        }
        if (sourceUserId.equals(targetUserId)) {
            throw new BusinessException("不能分享给自己");
        }

        FsNode sourceNode = nodeMapper.selectById(sourceNodeId);
        if (sourceNode == null) {
            throw new BusinessException("文件不存在");
        }
        if (!sourceNode.getUserId().equals(sourceUserId)) {
            throw new BusinessException("不能分享给自己");
        }

        // 递归复制，目标父目录设为 0，即放入目标用户的根目录
        copyNodeRecursive(sourceNode, targetUserId, 0L);
    }

    // 重命名
    public void renameNode(Long userId, Long nodeId, String newName) {
        FsNode node = nodeMapper.selectById(nodeId);
        if (node == null) throw new BusinessException("文件不存在");
        if (!node.getUserId().equals(userId)) throw new BusinessException("无权操作");

        node.setFileName(newName);
        node.setUpdateTime(LocalDateTime.now());
        nodeMapper.updateById(node);
    }

    // --- 辅助方法 ---
    private void saveLog(Long userId, String op, String fileName, String detail) {
        FsAuditLog log = new FsAuditLog();
        log.setUserId(userId);
        log.setOperation(op);
        log.setFileName(fileName);
        log.setDetail(detail);
        log.setOpTime(LocalDateTime.now());
        logMapper.insert(log);
    }

    private String getSuffix(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private void deleteRecursive(Long parentId, Long userId) {
        QueryWrapper<FsNode> query = new QueryWrapper<>();
        query.eq("parent_id", parentId);
        query.eq("user_id", userId);
        List<FsNode> children = nodeMapper.selectList(query);

        for (FsNode child : children) {
            deleteRecursive(child.getId(), child.getUserId());
        }

        QueryWrapper<FsNode> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("id", parentId);
        deleteWrapper.eq("user_id", userId);
        nodeMapper.delete(deleteWrapper);
    }

    private void copyNodeRecursive(FsNode source, Long targetUserId, Long targetParentId) {
        FsNode newNode = new FsNode();
        newNode.setUserId(targetUserId);
        newNode.setParentId(targetParentId);
        newNode.setFileName(source.getFileName());
        newNode.setIsDir(source.getIsDir());
        newNode.setStorageId(source.getStorageId());
        newNode.setFileSize(source.getFileSize());
        newNode.setCreateTime(LocalDateTime.now());

        nodeMapper.insert(newNode);

        // 如果是文件夹，递归复制子节点
        if (source.getIsDir()) {
            // 查出原节点下的所有子节点
            QueryWrapper<FsNode> query = new QueryWrapper<>();
            query.eq("parent_id", source.getId());
            List<FsNode> children = nodeMapper.selectList(query);

            for (FsNode child : children) {
                // 父ID 变成了刚刚创建的 newNode.getId()
                copyNodeRecursive(child, targetUserId, newNode.getId());
            }
        }
    }
}