package com.griffin.jsontotypescriptclass.action

import com.griffin.jsontotypescriptclass.dialog.JsonToTsDialog
import com.griffin.jsontotypescriptclass.generate.TsModel
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class JsonToTsAction: AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.apply {
            val path = getPath(e)
            // 获取当前选中项的 PsiDirectory
            val psiDirectory: PsiDirectory? = getCurrentPsiDirectory(e)
            if (psiDirectory != null) {
                val jsonToTsDialog = JsonToTsDialog(this, path)
                val isOk = jsonToTsDialog.showAndGet()
                if (isOk){
                    ApplicationManager.getApplication().runWriteAction{
                        // 获取类名
                        val className = jsonToTsDialog.getClassName()
                        val jsonContent = jsonToTsDialog.getJsonContent()
                        val codeModel = TsModel.jsonToTypeScriptClass(jsonContent, className)
                        saveCodeToFile(this, psiDirectory, className, codeModel)
                    }
                }
            }
        }
    }

    private fun getPath(e: AnActionEvent): String {
        val psiFile = e.getData(PlatformDataKeys.VIRTUAL_FILE)
        return psiFile?.path ?: ""
    }

    private fun getCurrentPsiDirectory(e: AnActionEvent): PsiDirectory? {
        // 获取当前选中项
        val psiFile: PsiFile? = e.getData(PlatformDataKeys.PSI_FILE)
        if (psiFile != null && psiFile.isDirectory) {
            // 如果当前选中项是目录，直接返回
            return psiFile as PsiDirectory
        } else {
            // 获取当前选中项的 PsiElement
            val psiElement: PsiElement? =
                    e.getData(PlatformDataKeys.PSI_ELEMENT)
            if (psiElement != null) {
                // 如果当前选中项不是目录，则尝试获取父目录
                return (psiElement as? PsiDirectory) ?: (psiElement.parent as? PsiDirectory)
            }
        }

        // 如果当前选中项不是目录，则尝试获取父目录
        return psiFile?.parent
    }

    private fun saveCodeToFile(
            project: Project,
            psiDirectory: PsiDirectory,
            fileName: String,
            code: String
    ) {
        val virtualDirectory: VirtualFile? = psiDirectory.virtualFile
        if (virtualDirectory != null) {
            // 创建一个新的 VirtualFile
            val virtualFile = virtualDirectory.createChildData(this, "$fileName.ts")
            // 将代码写入文件
            virtualFile.getOutputStream(this).use { it.write(code.toByteArray()) }

            // 刷新文件系统，以确保文件显示在IDE中
            LocalFileSystem.getInstance()
                    .refreshAndFindFileByIoFile(virtualFile.toNioPath().toFile())

            // 打开新创建的文件
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }
    }
}