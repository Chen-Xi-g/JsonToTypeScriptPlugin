package com.griffin.jsontotypescriptclass.dialog

import com.intellij.ui.JBColor
import java.awt.*
import java.awt.event.*
import java.awt.font.TextAttribute
import java.beans.*
import java.util.HashMap
import javax.swing.*
import javax.swing.border.*
import javax.swing.event.*
import javax.swing.text.*

class TextLineNumber(private val component: JTextComponent, minimumDisplayDigits: Int = 3) :
    JPanel(), CaretListener, DocumentListener, PropertyChangeListener {

    companion object {
        const val LEFT = 0.0f
        const val CENTER = 0.5f
        const val RIGHT = 1.0f
        private val OUTER: Border = MatteBorder(0, 0, 0, 2, Color.GRAY)
        private const val HEIGHT = Int.MAX_VALUE - 1000000
    }

    private var updateFont = false
    private var borderGap = 5
    private var currentLineForeground: JBColor? = JBColor.RED
    private var digitAlignment = RIGHT
    private var minimumDisplayDigits: Int = minimumDisplayDigits

    private var lastDigits = 0
    private var lastHeight = 0
    private var lastLine = 0

    private val fonts: HashMap<String, FontMetrics> = HashMap()

    init {
        font = component.font
        setBorderGap(5)
        setCurrentLineForeground(JBColor.RED)
        setDigitAlignment(RIGHT)
        setMinimumDisplayDigits(minimumDisplayDigits)

        component.document.addDocumentListener(this)
        // 设置文本抗锯齿和行高
        component.font?.let { originalFont ->
            val newFont = originalFont.deriveFont(mapOf(TextAttribute.SIZE to originalFont.size2D))
            component.setFont(newFont)
        }
        component.addCaretListener(this)
        component.addPropertyChangeListener("font", this)
    }

    fun getUpdateFont(): Boolean = updateFont

    fun setUpdateFont(updateFont: Boolean) {
        this.updateFont = updateFont
    }

    fun getBorderGap(): Int = borderGap

    fun setBorderGap(borderGap: Int) {
        this.borderGap = borderGap
        val inner: Border = EmptyBorder(0, borderGap, 0, borderGap)
        setBorder(CompoundBorder(OUTER, inner))
        lastDigits = 0
        setPreferredWidth()
    }

    fun getCurrentLineForeground(): Color = currentLineForeground ?: foreground

    fun setCurrentLineForeground(currentLineForeground: JBColor) {
        this.currentLineForeground = currentLineForeground
    }

    fun getDigitAlignment(): Float = digitAlignment

    fun setDigitAlignment(digitAlignment: Float) {
        this.digitAlignment = digitAlignment.coerceIn(-1.0f, 1.0f)
    }

    fun getMinimumDisplayDigits(): Int = minimumDisplayDigits

    fun setMinimumDisplayDigits(minimumDisplayDigits: Int) {
        this.minimumDisplayDigits = minimumDisplayDigits
        setPreferredWidth()
    }

    private fun setPreferredWidth() {
        val root: Element = component.document.defaultRootElement
        val lines: Int = root.elementCount
        val digits: Int = maxOf(lines.toString().length, minimumDisplayDigits)

        if (lastDigits != digits) {
            lastDigits = digits
            val fontMetrics: FontMetrics = getFontMetrics(font)
            val width: Int = fontMetrics.charWidth('0') * digits
            val insets: Insets = insets
            val preferredWidth: Int = insets.left + insets.right + width

            preferredSize = Dimension(preferredWidth, HEIGHT)
            size = preferredSize
        }
    }

    override fun paintComponent(g: Graphics) {
        (g as? Graphics2D)?.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        )
        super.paintComponent(g)
        val fontMetrics: FontMetrics = component.getFontMetrics(component.font)
        val insets: Insets = insets
        val availableWidth: Int = size.width - insets.left - insets.right
        val clip: Rectangle = g.clipBounds
        var rowStartOffset: Int = component.viewToModel(Point(0, clip.y))
        val endOffset: Int = component.viewToModel(Point(0, clip.y + clip.height))

        while (rowStartOffset <= endOffset) {
            try {
                g.color = if (isCurrentLine(rowStartOffset)) getCurrentLineForeground() else foreground
                val lineNumber: String = getTextLineNumber(rowStartOffset)
                val stringWidth: Int = fontMetrics.stringWidth(lineNumber)
                val x: Int = getOffsetX(availableWidth, stringWidth) + insets.left
                val y: Int = getOffsetY(rowStartOffset, fontMetrics)
                g.drawString(lineNumber, x, y)
                rowStartOffset = Utilities.getRowEnd(component, rowStartOffset) + 1
            } catch (e: Exception) {
                break
            }
        }
    }

    private fun isCurrentLine(rowStartOffset: Int): Boolean {
        val caretPosition: Int = component.caretPosition
        val root: Element = component.document.defaultRootElement
        return root.getElementIndex(rowStartOffset) == root.getElementIndex(caretPosition)
    }

    protected fun getTextLineNumber(rowStartOffset: Int): String {
        val root: Element = component.document.defaultRootElement
        val index: Int = root.getElementIndex(rowStartOffset)
        val line: Element = root.getElement(index)
        return if (line.startOffset == rowStartOffset) (index + 1).toString() else ""
    }

    private fun getOffsetX(availableWidth: Int, stringWidth: Int): Int {
        return ((availableWidth - stringWidth) * digitAlignment).toInt()
    }

    private fun getOffsetY(rowStartOffset: Int, fontMetrics: FontMetrics): Int {
        val r: Rectangle = component.modelToView(rowStartOffset)
        val lineHeight: Int = fontMetrics.height
        val y: Int = r.y + r.height
        var descent = 0

        if (r.height == lineHeight) {
            descent = fontMetrics.descent
        } else {
            if (fonts.isEmpty()) fonts.clear()

            val root: Element = component.document.defaultRootElement
            val index: Int = root.getElementIndex(rowStartOffset)
            val line: Element = root.getElement(index)

            for (i in 0 until line.elementCount) {
                val child: Element = line.getElement(i)
                val asAttrSet: AttributeSet = child.attributes
                val fontFamily: String = asAttrSet.getAttribute(StyleConstants.FontFamily) as String
                val fontSize: Int = asAttrSet.getAttribute(StyleConstants.FontSize) as Int
                val key: String = "$fontFamily$fontSize"
                var fm: FontMetrics? = fonts[key]

                if (fm == null) {
                    val font: Font = Font(fontFamily, Font.PLAIN, fontSize)
                    fm = component.getFontMetrics(font)
                    fonts[key] = fm
                }

                descent = maxOf(descent, fm!!.descent)
            }
        }

        return y - descent
    }

    override fun caretUpdate(e: CaretEvent) {
        val caretPosition: Int = component.caretPosition
        val root: Element = component.document.defaultRootElement
        val currentLine: Int = root.getElementIndex(caretPosition)

        if (lastLine != currentLine) {
            (parent as? JComponent)?.repaint()
            lastLine = currentLine
        }
    }

    override fun changedUpdate(e: DocumentEvent) {
        documentChanged()
    }

    override fun insertUpdate(e: DocumentEvent) {
        documentChanged()
    }

    override fun removeUpdate(e: DocumentEvent) {
        documentChanged()
    }

    private fun documentChanged() {
        SwingUtilities.invokeLater {
            try {
                val endPos: Int = component.document.length
                val rect: Rectangle? = component.modelToView(endPos)

                if (rect != null && rect.y != lastHeight) {
                    setPreferredWidth()
                    (parent as? JComponent)?.repaint()
                    lastHeight = rect.y
                }
            } catch (ex: BadLocationException) {
                // nothing to do
            }
        }
    }

    override fun propertyChange(evt: PropertyChangeEvent) {
        if (evt.newValue is Font) {
            if (updateFont) {
                val newFont: Font = evt.newValue as Font
                font = newFont
                lastDigits = 0
                setPreferredWidth()
            } else {
                (parent as? JComponent)?.repaint()
            }
        }
    }
}
