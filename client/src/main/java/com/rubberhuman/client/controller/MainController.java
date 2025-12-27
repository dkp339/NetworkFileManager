package com.rubberhuman.client.controller;

import com.rubberhuman.client.ClientApp;
import com.rubberhuman.client.api.ApiClient;
import com.rubberhuman.client.model.FsNode;
import com.rubberhuman.client.util.AlertUtil;
import com.rubberhuman.client.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import kong.unirest.GenericType;
import kong.unirest.HttpResponse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

public class MainController {

    // --- 顶部栏 ---
    @FXML private Label currentUserLabel;
    @FXML private TextField searchField; // 请确保 fxml 里加了这个 fx:id
    @FXML private Button btnBack;        // 请确保 fxml 里加了这个 fx:id (返回按钮)
    @FXML private Label pathLabel;       // 请确保 fxml 里加了这个 fx:id (显示当前路径)

    // --- 左侧目录树 ---
    @FXML private TreeView<FsNode> directoryTree; // 泛型改成 FsNode

    // --- 中间表格 ---
    @FXML private TableView<FsNode> fileTable;
    @FXML private TableColumn<FsNode, String> colType;
    @FXML private TableColumn<FsNode, String> colName;
    @FXML private TableColumn<FsNode, String> colSize;
    @FXML private TableColumn<FsNode, String> colTime;

    // --- 右侧属性 ---
    @FXML private Label propName;
    @FXML private Label propType;
    @FXML private Label propSize;

    // --- 状态变量 ---
    private Long currentParentId = 0L;
    private Stack<Long> historyStack = new Stack<>(); // 历史记录栈，用于"返回"
    private Stack<String> pathStack = new Stack<>();  // 路径名字栈

    @FXML
    public void initialize() {
        // 设置顶部用户信息
        if (SessionManager.getUsername() != null) {
            currentUserLabel.setText("当前用户: " + SessionManager.getUsername());
        }

        // 初始化表格列映射
        // 使用 Lambda 表达式调用 FsNode 的辅助方法
        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType()));
        colName.setCellValueFactory(new PropertyValueFactory<>("fileName")); // 对应 getFileName()
        colSize.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSizeStr()));
        colTime.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormatTime()));

        // 2. 表格双击事件
        fileTable.setRowFactory(tv -> {
            TableRow<FsNode> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    FsNode rowData = row.getItem();
                    if (rowData.getIsDir()) {
                        enterFolder(rowData); // 进入文件夹
                    }
                }
            });
            return row;
        });

        // 3. 属性面板联动
        fileTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> updatePropertyPanel(n));

        // 4. 初始化
        pathStack.push("根目录");
        updatePathLabel();
        loadTree();     // 加载左侧目录树
        refreshTable(); // 加载文件列表
    }


    private void refreshTable() {
        String search = (searchField != null) ? searchField.getText().trim() : "";

        String url = "/file/list?parentId=" + currentParentId;
        // 如果有搜索词，后端逻辑是忽略 parentId 进行全局搜索
        if (!search.isEmpty()) {
            url = "/file/list?search=" + search;
        }

        HttpResponse<List<FsNode>> res = ApiClient.get(url)
                .asObject(new GenericType<List<FsNode>>() {});

        if (!ApiClient.checkAuth(res)) return; // JWT 检查

        if (res.isSuccess()) {
            fileTable.setItems(FXCollections.observableArrayList(res.getBody()));
        } else {
            AlertUtil.showError("错误", "列表加载失败");
        }
        updatePropertyPanel(null);
    }

    @FXML
    protected void onBackClick() {
        if (historyStack.isEmpty()) {
            return;
        }
        // 1. 弹栈
        Long prevId = historyStack.pop();
        if (!pathStack.isEmpty()) pathStack.pop();

        // 2. 变更状态
        currentParentId = prevId;

        // 3. 刷新
        refreshTable();
        updatePathLabel();
    }

    // --- 功能 1: 上传文件 ---
    @FXML
    protected void onUploadClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择要上传的文件");
        File file = fileChooser.showOpenDialog(fileTable.getScene().getWindow());

        if (file != null) {
            // 显示“上传中”提示（实际项目应做进度条，这里简单阻塞）
            HttpResponse<?> res = ApiClient.upload("/file/upload")
                    .queryString("parentId", currentParentId)
                    .field("file", file)
                    .asEmpty();

            if (res.isSuccess()) {
                AlertUtil.showInfo("成功", "文件上传成功！");
                refreshTable(); // 刷新列表
            } else {
                AlertUtil.showError("失败", "上传失败：" + res.getStatusText());
            }
        }
    }

    // --- 功能 2: 新建文件夹 ---
    @FXML
    protected void onNewFolderClick() {
        TextInputDialog dialog = new TextInputDialog("新建文件夹");
        dialog.setTitle("新建文件夹");
        dialog.setHeaderText("请输入文件夹名称：");
        dialog.setContentText("名称:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            HttpResponse<?> res = ApiClient.post("/file/folder")
                    .queryString("parentId", currentParentId)
                    .queryString("name", name)
                    .asEmpty();

            if (res.isSuccess()) {
                refreshTable();
            } else {
                AlertUtil.showError("失败", "创建失败");
            }
        });
    }

    // --- 功能 3: 下载文件 ---
    @FXML
    protected void onDownloadClick() {
        FsNode selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showError("提示", "请先选择一个文件");
            return;
        }
        if (selected.getIsDir()) {
            AlertUtil.showError("提示", "不支持下载文件夹");
            return;
        }

        // 选择保存路径
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("保存文件");
        fileChooser.setInitialFileName(selected.getFileName());
        File saveFile = fileChooser.showSaveDialog(fileTable.getScene().getWindow());

        if (saveFile != null) {
            HttpResponse<File> res = ApiClient.get("/file/download")
                    .queryString("nodeId", selected.getId())
                    .asFile(saveFile.getAbsolutePath()); // 直接存入目标路径

            if (res.isSuccess()) {
                AlertUtil.showInfo("成功", "文件下载完成");
            } else {
                // 如果失败，Unirest 可能会创建一个空文件，最好清理一下
                saveFile.delete();
                AlertUtil.showError("失败", "下载请求被拒绝: " + res.getStatusText());
            }
        }
    }

    // --- 功能 4: 删除文件 ---
    @FXML
    protected void onDeleteClick() {
        FsNode selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showError("提示", "请先选择要删除的项目");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认删除");
        alert.setHeaderText("确定要删除 " + selected.getFileName() + " 吗？");
        alert.setContentText("此操作不可恢复。");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            HttpResponse<?> res = ApiClient.post("/file/delete")
                    .queryString("nodeId", selected.getId())
                    .asEmpty();

            if (res.isSuccess()) {
                refreshTable();
            } else {
                AlertUtil.showError("失败", "删除失败");
            }
        }
    }

    // --- 功能 5: 退出登录 ---
    @FXML
    protected void onLogoutClick() throws IOException {
        SessionManager.clearSession();
        ClientApp.setRoot("view/login");
    }

    // ================== 左侧目录树逻辑 ==================

    /**
     * 加载目录树 (简化版：加载根目录下所有文件夹，不做无限递归以防性能问题，或者做一层)
     * 真正的 Windows 资源管理器是“懒加载”的，这里我们模拟加载根目录结构
     */
    private void loadTree() {
        // 根节点
        FsNode rootNodeData = new FsNode();
        rootNodeData.setFileName("我的网盘");
        rootNodeData.setId(0L);
        TreeItem<FsNode> rootItem = new TreeItem<>(rootNodeData);
        rootItem.setExpanded(true);

        directoryTree.setRoot(rootItem);
        directoryTree.setCellFactory(tv -> new TreeCell<FsNode>() {
            @Override
            protected void updateItem(FsNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getFileName());
                }
            }
        });

        // 监听选择：点击树节点刷新右侧列表
        directoryTree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                FsNode node = newVal.getValue();
                // 只有点击的不是当前目录才刷新，且要清空历史栈（因为是从树跳转的）
                if (!node.getId().equals(currentParentId)) {
                    currentParentId = node.getId();
                    historyStack.clear(); // 树跳转会打断返回链，简单处理清空栈
                    historyStack.push(0L); // 假定上一级是根
                    pathStack.clear();
                    pathStack.push("根目录");
                    if (node.getId() != 0) pathStack.push(node.getFileName());

                    refreshTable();
                    updatePathLabel();
                }
            }
        });

        // 初始加载第一层文件夹
        loadSubFolders(rootItem, 0L);
    }

    // 递归查找子文件夹
    private void loadSubFolders(TreeItem<FsNode> parentItem, Long parentId) {
        HttpResponse<List<FsNode>> res = ApiClient.get("/file/list?parentId=" + parentId)
                .asObject(new GenericType<List<FsNode>>() {});

        if (res.isSuccess()) {
            List<FsNode> nodes = res.getBody();
            for (FsNode node : nodes) {
                if (node.getIsDir()) {
                    TreeItem<FsNode> item = new TreeItem<>(node);
                    parentItem.getChildren().add(item);
                    // 递归加载（慎用：如果层级太深可能会卡，演示项目没问题）
                    loadSubFolders(item, node.getId());
                }
            }
        }
    }

    // ================== 工具栏功能实现 ==================

    @FXML
    protected void onSearchClick() {
        // 搜索按钮触发
        refreshTable();
    }

    @FXML
    protected void onRefreshClick() {
        if (searchField != null) searchField.clear();
        loadTree(); // 刷新树
        refreshTable(); // 刷新表
    }

    // 1. 重命名 (Bug 2 修复)
    @FXML
    protected void onRenameClick() {
        FsNode selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showError("提示", "请选择文件");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(selected.getFileName());
        dialog.setTitle("重命名");
        dialog.setHeaderText("重命名文件");
        dialog.setContentText("新名称:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newName -> {
            HttpResponse<?> res = ApiClient.post("/file/rename")
                    .queryString("nodeId", selected.getId())
                    .queryString("newName", newName)
                    .asEmpty();

            if (!ApiClient.checkAuth(res)) return;

            if (res.isSuccess()) {
                refreshTable();
            } else {
                AlertUtil.showError("失败", "重命名失败");
            }
        });
    }

    // 2. 分享 (Bug 2 修复)
    @FXML
    protected void onShareClick() {
        FsNode selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showError("提示", "请选择要分享的文件");
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("文件分享");
        dialog.setHeaderText("分享: " + selected.getFileName());
        dialog.setContentText("请输入目标用户名:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(targetUser -> {
            HttpResponse<?> res = ApiClient.post("/file/share")
                    .queryString("nodeId", selected.getId())
                    .queryString("targetUsername", targetUser)
                    .asEmpty();

            if (!ApiClient.checkAuth(res)) return;

            if (res.isSuccess()) {
                AlertUtil.showInfo("成功", "分享成功！");
            } else {
                AlertUtil.showError("失败", "分享失败: " + res.getStatusText());
            }
        });
    }

    // --- 辅助：更新属性面板 ---
    private void updatePropertyPanel(FsNode node) {
        if (node == null) {
            propName.setText("-");
            propType.setText("-");
            propSize.setText("-");
        } else {
            propName.setText(node.getFileName());
            propType.setText(node.getType());
            propSize.setText(node.getSizeStr());
        }
    }

    private void enterFolder(FsNode folder) {
        // 1. 压栈 (记录当前在哪，以便返回)
        historyStack.push(currentParentId);
        pathStack.push(folder.getFileName());

        // 2. 变更状态
        currentParentId = folder.getId();

        // 3. 刷新
        refreshTable();
        updatePathLabel();
    }

    private void updatePathLabel() {
        // 简单的路径显示，例如: /根目录/文档/工作
        StringBuilder sb = new StringBuilder();
        for (String p : pathStack) {
            sb.append("/").append(p);
        }
        if (pathLabel != null) pathLabel.setText("当前路径: " + sb.toString());

        // 控制返回按钮可用性
        if (btnBack != null) btnBack.setDisable(historyStack.isEmpty());
    }
}