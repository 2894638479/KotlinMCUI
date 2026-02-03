package io.github.u2894638479.kotlinmcui.text

@JvmInline
value class DslCharStyle private constructor (private val value: Int) {
    constructor():this(0)
    private fun get(index:Int) = (value shr index) and 1 != 0
    private fun change(index:Int, bl: Boolean) = DslCharStyle(if(bl) value or (1 shl index) else value and (1 shl index).inv())

    val isItalic get() = get(0)
    fun changeItalic(bl: Boolean = true) = change(0,bl)
    val italic get() = change(0,true)

    val isBold get() = get(1)
    fun changeBold(bl: Boolean = true) = change(1,bl)
    val bold get() = change(1,true)

    val isUnderlined get() = get(2)
    fun changeUnderlined(bl: Boolean = true) = change(2,bl)
    val underlined get() = change(2,true)

    val isStrikeThrough get() = get(3)
    fun changeStrikeThrough(bl: Boolean = true) = change(3,bl)
    val strikeThrough get() = change(3,true)

    val isObfuscated get() = get(4)
    fun changeObfuscated(bl: Boolean = true) = change(4,bl)
    val obfuscated get() = change(4,true)

    val isShadowed get() = get(5)
    fun changeShadowed(bl: Boolean = true) = change(5,bl)
    val shadowed get() = change(5,true)
}