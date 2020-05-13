// Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package software.aws.toolkits.jetbrains.services.s3.editor;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.treeStructure.SimpleTreeStructure;
import com.intellij.util.ui.ColumnInfo;
import org.jetbrains.annotations.NotNull;
import software.aws.toolkits.jetbrains.services.s3.objectActions.*;
import software.aws.toolkits.jetbrains.ui.tree.AsyncTreeModel;
import software.aws.toolkits.jetbrains.ui.tree.StructureTreeModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import static software.aws.toolkits.resources.Localization.message;

@SuppressWarnings("unchecked")
public class S3ViewerPanel {
    private Disposable disposable;
    private JPanel content;
    private JPanel mainPanel;
    private S3TreeTable treeTable;
    private final Project project;
    private final ColumnInfo<Object, String>[] columns;
    private S3VirtualBucket virtualBucket;

    public S3ViewerPanel(Disposable disposable, Project project, S3VirtualBucket bucketVirtual) {
        this.project = project;
        this.disposable = disposable;
        this.virtualBucket = bucketVirtual;

        ColumnInfo<Object, String> key = new S3Column(S3ColumnType.NAME);
        ColumnInfo<Object, String> size = new S3Column(S3ColumnType.SIZE);
        ColumnInfo<Object, String> modified = new S3Column(S3ColumnType.LAST_MODIFIED);
        columns = new ColumnInfo[] {key, size, modified};
        S3TreeTableModel model = createTreeTableModel(columns, new S3TreeDirectoryNode(bucketVirtual, null, ""));
        treeTable = new S3TreeTable(model, bucketVirtual, project);
        applyTreeStyle(treeTable);
        addTreeActions(treeTable);

        ToolbarDecorator panel = addToolbar(treeTable);
        mainPanel.add(panel.createPanel());
    }

    private void createUIComponents() {
    }

    private ToolbarDecorator addToolbar(S3TreeTable table) {
        DefaultActionGroup group = makeActionGroup(table);
        group.addAction(new AnAction(message("explorer.refresh.title"), null, AllIcons.Actions.Refresh) {
            @Override
            public boolean isDumbAware() {
                return true;
            }

            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                table.setModel(createTreeTableModel(columns, new S3TreeDirectoryNode(virtualBucket, null, "")));
                // we have to apply style again because changing the model breaks the style
                applyTreeStyle(table);
                table.invalidate();
            }
        });
        return ToolbarDecorator
            .createDecorator(table)
            .setActionGroup(group);
    }

    private void applyTreeStyle(S3TreeTable table) {
        DefaultTableCellRenderer tableRenderer = new DefaultTableCellRenderer();
        tableRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        table.setDefaultRenderer(Object.class, tableRenderer);
        table.setRootVisible(false);
        S3TreeCellRenderer treeRenderer = new S3TreeCellRenderer(table);
        table.setTreeCellRenderer(treeRenderer);
        table.setCellSelectionEnabled(false);
        table.setRowSelectionAllowed(true);
        table.setRowSorter(new S3RowSorter(table.getModel()));
        // prevent accidentally moving the columns around. We don't account for the ability
        // to do this anywhere so better be safe than sorry. TODO audit logic to allow this
        table.getTableHeader().setReorderingAllowed(false);
        table.getColumnModel().getColumn(1).setMaxWidth(120);
    }

    public JComponent getComponent() {
        return content;
    }

    public JComponent getFocusComponent() {
        return treeTable;
    }

    private S3TreeTableModel createTreeTableModel(ColumnInfo[] columns, S3TreeDirectoryNode s3TreeNode) {
        SimpleTreeStructure treeStructure = new SimpleTreeStructure.Impl(s3TreeNode);
        StructureTreeModel<SimpleTreeStructure> myTreeModel = new StructureTreeModel(treeStructure, disposable);
        return new S3TreeTableModel(new AsyncTreeModel(myTreeModel, true, disposable), columns, myTreeModel);
    }

    private void addTreeActions(S3TreeTable table) {
        PopupHandler.installPopupHandler(table, makeActionGroup(table), ActionPlaces.EDITOR_POPUP, ActionManager.getInstance());
    }

    private DefaultActionGroup makeActionGroup(S3TreeTable table) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new DownloadObjectAction(project, table));
        actionGroup.add(new UploadObjectAction(project, table));
        actionGroup.add(new Separator());
        actionGroup.add(new NewFolderAction(project, table));
        actionGroup.add(new RenameObjectAction(project, table));
        actionGroup.add(new CopyPathAction(project, table));
        actionGroup.add(new Separator());
        actionGroup.add(new DeleteObjectAction(project, table));
        return actionGroup;
    }
}
