package io.github.u2894638479.kotlinmcui.math.align

@JvmInline
value class Alignment private constructor(private val value:Int){
    constructor():this(Align.MID.ordinal.let { it or (it shl 8) })

    val horizontal get() = Align.entries[value and 0xFF]
    val vertical get() = Align.entries[(value ushr 8) and 0xFF]
    private fun changeByte(offset:Int,value:Int): Alignment {
        val value1 = (value and 0xFF) shl offset
        val value2 = this.value and (0xFF shl offset).inv()
        return Alignment(value1 or value2)
    }

    fun left() = changeByte(0,Align.LOW.ordinal)
    fun top() = changeByte(8,Align.LOW.ordinal)
    fun right() = changeByte(0,Align.HIGH.ordinal)
    fun bottom() = changeByte(8,Align.HIGH.ordinal)
    fun middleX() = changeByte(0,Align.MID.ordinal)
    fun middleY() = changeByte(8,Align.MID.ordinal)
    fun middle() = middleX().middleY()
}