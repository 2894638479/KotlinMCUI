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
    
    fun horizontal(align: Align) = changeByte(0,align.ordinal)
    fun vertical(align: Align) = changeByte(8,align.ordinal)

    fun left() = horizontal(Align.LOW)
    fun top() = vertical(Align.LOW)
    fun right() = horizontal(Align.HIGH)
    fun bottom() = vertical(Align.HIGH)
    fun middleX() = horizontal(Align.MID)
    fun middleY() = vertical(Align.MID)
    fun middle() = middleX().middleY()
}