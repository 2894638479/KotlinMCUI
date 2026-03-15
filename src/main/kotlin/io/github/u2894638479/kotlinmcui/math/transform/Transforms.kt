package io.github.u2894638479.kotlinmcui.math.transform

import io.github.u2894638479.kotlinmcui.math.Position

@JvmInline
value class Transforms private constructor(private val transforms: Array<out BaseTransform>): Transform {
    companion object {
        operator fun invoke(vararg baseTransforms: BaseTransform) = Transforms(baseTransforms)
    }

    override fun transform(pos: Position): Position {
        var result = pos
        transforms.forEach { result = it.transform(result) }
        return result
    }

    override fun inverse(pos: Position): Position {
        var result = pos
        transforms.asList().asReversed().forEach { result = it.inverse(result) }
        return result
    }

    override val baseTransforms get() = transforms.asList()
}