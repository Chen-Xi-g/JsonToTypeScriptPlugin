package com.griffin.jsontotypescriptclass.dialog

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.intellij.ide.actions.MaximizeActiveDialogAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import java.awt.Dimension
import java.awt.Insets
import javax.swing.*

class JsonToTsDialog(
        project: Project,
        private val path: String
) : DialogWrapper(project) {

    // 输出地址
    private val jsonToTsClassPath = path

    // 提示文本控件
    private val tips = JBLabel("Please input the JSON String and class name to generate TypeScript class")

    // JSON 输入框
    private val jsonTextArea = JBTextArea()

    // 类名输入框
    private val classNameTextField = JBTextField()

    // 格式化JSON按钮
    private val formatJsonButton = JButton("Format JSON")

    private val lineNumberView = TextLineNumber(jsonTextArea)

    // 校验JSON是否合法
    private var isJsonValid = false

    init {
        title = "Json to TypeScript Class"
        isResizable = false
        init()
    }

    override fun createCenterPanel(): JComponent {
        // 创建内容布局
        val panel = JPanel()
        val gridLayoutManager = GridLayoutManager(3, 3, Insets(10, 10, 10, 10), -1, -1)
        gridLayoutManager.hGap = 10
        panel.layout = gridLayoutManager
        panel.add(tips, GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
        // 创建 JScrollPane，并将 jsonTextArea 放入其中
        jsonTextArea.setLineWrap(true)
        jsonTextArea.setWrapStyleWord(true)

        val scrollPane = JBScrollPane(jsonTextArea)
        val rowHeaderView = JViewport()
        rowHeaderView.view = lineNumberView
        scrollPane.rowHeader = rowHeaderView
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        panel.add(scrollPane, GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, Dimension(600, 300), null, Dimension(600, 300), 0, false))

        panel.add(JLabel("Class Name"), GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))

        panel.add(classNameTextField, GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
        // 添加 ActionListener 到按钮
        formatJsonButton.addActionListener { e -> onFormatJsonButtonClick() }
        panel.add(formatJsonButton, GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false))
        panel.maximumSize = Dimension(800, 600)
        return panel
    }

    private fun onFormatJsonButtonClick() : Boolean{
        val jsonString = jsonTextArea.text.trim()
        return try {
            val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
            val jsonElement: JsonElement = JsonParser.parseString(jsonString)
            val formattedJson = gson.toJson(jsonElement)
            jsonTextArea.text = formattedJson
            isJsonValid = true
            true
        } catch (e: JsonParseException) {
            val errorMessage = "Error parsing JSON: ${e.message}"
            isJsonValid = false
            showErrorDialog(errorMessage)
            false
        }
    }

    private fun showErrorDialog(message: String) {
        JOptionPane.showMessageDialog(
                null,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
        )
    }

    override fun doOKAction() {
        if (classNameTextField.text.isBlank()) {
            showErrorDialog("Class name cannot be empty")
            return
        }
        if (!isJsonValid){
            if (!onFormatJsonButtonClick()){
                return
            }
        }
        super.doOKAction()
    }

    fun getClassName(): String {
        return classNameTextField.text
    }

    fun getJsonContent(): String{
        return jsonTextArea.text
    }
}