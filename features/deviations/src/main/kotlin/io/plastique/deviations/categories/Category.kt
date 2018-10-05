package io.plastique.deviations.categories

import java.io.Serializable

data class Category(
    val path: String,
    val title: String,
    val parent: Category? = null,
    val hasChildren: Boolean = false
) : Serializable {

    override fun toString(): String {
        return "Category(path=$path, title=$title, parent=${parent?.path}, hasChildren=$hasChildren)"
    }

    companion object {
        val ALL = Category("/", "All categories", null, true)
    }
}
