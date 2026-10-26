package me.rerere.ui.text

object EmojiTextRanges {
    fun findAll(text: CharSequence): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var index = 0
        while (index < text.length) {
            val codePoint = Character.codePointAt(text, index)
            if (!isEmojiCodePoint(codePoint) && !isKeycapBase(text, index, codePoint)) {
                index += Character.charCount(codePoint)
                continue
            }

            val start = index
            index += Character.charCount(codePoint)
            while (index < text.length) {
                val next = Character.codePointAt(text, index)
                if (!isEmojiCodePoint(next) && !isEmojiComponent(next)) break
                index += Character.charCount(next)
            }
            ranges += start until index
        }
        return ranges
    }

    private fun isEmojiCodePoint(codePoint: Int): Boolean =
        codePoint in 0x1F000..0x1FAFF ||
            codePoint in 0x2600..0x27BF ||
            codePoint in 0x2300..0x23FF ||
            codePoint in 0x2B00..0x2BFF ||
            codePoint in 0x2190..0x21FF ||
            codePoint in 0x25A0..0x25FF ||
            codePoint == 0x00A9 ||
            codePoint == 0x00AE ||
            codePoint == 0x203C ||
            codePoint == 0x2049 ||
            codePoint == 0x2122 ||
            codePoint == 0x2139

    private fun isEmojiComponent(codePoint: Int): Boolean =
        codePoint == 0x200D ||
            codePoint == 0x20E3 ||
            codePoint in 0xFE00..0xFE0F ||
            codePoint in 0x1F3FB..0x1F3FF ||
            codePoint in 0xE0020..0xE007F

    private fun isKeycapBase(text: CharSequence, index: Int, codePoint: Int): Boolean {
        if (codePoint !in '0'.code..'9'.code && codePoint != '#'.code && codePoint != '*'.code) {
            return false
        }
        var nextIndex = index + Character.charCount(codePoint)
        if (nextIndex >= text.length) return false
        val next = Character.codePointAt(text, nextIndex)
        if (next == 0x20E3) return true
        if (next !in 0xFE00..0xFE0F) return false
        nextIndex += Character.charCount(next)
        return nextIndex < text.length && Character.codePointAt(text, nextIndex) == 0x20E3
    }
}
