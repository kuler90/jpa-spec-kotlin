/*
Kotlin Specification DSL
https://github.com/kuler90/jpa-spec-kotlin

MIT License
Copyright (c) 2020 Kulesha Roman

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import org.springframework.data.jpa.domain.Specification
import javax.persistence.criteria.*
import kotlin.reflect.KProperty1

// Path extensions
fun <T, R> KProperty1<T, R?>.path(): FieldPath<T, R?> = FieldPath(this)
infix fun <T, R, R2> KProperty1<T, R?>.join(field: KProperty1<R, R2?>): FieldPath<T, R2?> = FieldPath(field, this.path())
infix fun <T, R, R2> FieldPath<T, R?>.join(field: KProperty1<R, R2?>): FieldPath<T, R2?> = FieldPath(field,this)
@JvmName("plusCollection")
infix fun <T, E, R: Collection<E>, R2> KProperty1<T, R>.join(field: KProperty1<E, R2?>): FieldPath<T, R2?> = FieldPath(field, this.path())
@JvmName("plusCollection")
infix fun <T, E, R: Collection<E>, R2> FieldPath<T, R?>.join(field: KProperty1<E, R2?>): FieldPath<T, R2?> = FieldPath(field, this)

// Concat extensions
infix fun <T> String.concat(field: KProperty1<T, String?>): FieldConcat<T> = FieldConcat<T>().concat(this).concat(field)
infix fun <T> String.concat(path: FieldPath<T, String?>): FieldConcat<T> = FieldConcat<T>().concat(this).concat(path)
infix fun <T> String.concat(expr: Expression<String?>): FieldConcat<T> = FieldConcat<T>().concat(this).concat(expr)
infix fun <T> KProperty1<T, String?>.concat(text: String): FieldConcat<T> = FieldConcat<T>().concat(this).concat(text)
infix fun <T> KProperty1<T, String?>.concat(field: KProperty1<T, String?>): FieldConcat<T> = FieldConcat<T>().concat(this).concat(field)
infix fun <T> KProperty1<T, String?>.concat(path: FieldPath<T, String?>): FieldConcat<T> = FieldConcat<T>().concat(this).concat(path)
infix fun <T> KProperty1<T, String?>.concat(expr: Expression<String?>): FieldConcat<T> = FieldConcat<T>().concat(this).concat(expr)
infix fun <T> FieldPath<T, String?>.concat(text: String): FieldConcat<T> = FieldConcat<T>().concat(this).concat(text)
infix fun <T> FieldPath<T, String?>.concat(field: KProperty1<T, String?>): FieldConcat<T> = FieldConcat<T>().concat(this).concat(field)
infix fun <T> FieldPath<T, String?>.concat(path: FieldPath<T, String?>): FieldConcat<T> = FieldConcat<T>().concat(this).concat(path)
infix fun <T> FieldPath<T, String?>.concat(expr: Expression<String?>): FieldConcat<T> = FieldConcat<T>().concat(this).concat(expr)
infix fun <T> Expression<String?>.concat(text: String): FieldConcat<T> = FieldConcat<T>().concat(this).concat(text)
infix fun <T> Expression<String?>.concat(field: KProperty1<T, String?>): FieldConcat<T> = FieldConcat<T>().concat(this).concat(field)
infix fun <T> Expression<String?>.concat(path: FieldPath<T, String?>): FieldConcat<T> = FieldConcat<T>().concat(this).concat(path)
infix fun <T> Expression<String?>.concat(expr: Expression<String?>): FieldConcat<T> = FieldConcat<T>().concat(this).concat(expr)

object Spec {

    fun <T> empty(): Specification<T> = predicate { null }

    fun <T> predicate(enabled: Boolean = true, builder: CriteriaContext<T>.() -> Predicate?): Specification<T> {
        return Specification { root, query, cb -> if (enabled) CriteriaContext<T>(root, query, cb).builder() else null }
    }

    fun <T> and(vararg specs: Specification<T>): Specification<T> {
        return specs.fold(empty()) { acc, next -> acc.and(next)!! }
    }

    fun <T> or(vararg specs: Specification<T>): Specification<T> {
        return specs.fold(empty()) { acc, next -> acc.or(next)!! }
    }

    fun <T> not(spec: Specification<T>): Specification<T> = Specification.not(spec)

    fun <T> fetch(vararg fields: KProperty1<T, *>): Specification<T> = predicate { fields.forEach { fetch(it) }; null }
    fun <T> fetch(vararg paths: FieldPath<T, *>): Specification<T> = predicate { paths.forEach { fetch(it) }; null }

    // Any type specifications

    fun <T, R> isNull(field: KProperty1<T, R?>) = isNull(field.path())
    fun <T, R> isNull(path: FieldPath<T, R?>): Specification<T> = predicate { isNull(expr(path)) }

    fun <T, R> equal(field: KProperty1<T, R?>, value: R?, nullable: Boolean = false) = equal(field.path(), value, nullable)
    fun <T, R> equal(path: FieldPath<T, R?>, value: R?, nullable: Boolean = false): Specification<T> {
        return predicate(value != null || nullable) { equal(expr(path), value) }
    }

    fun <T, R> memberOf(field: KProperty1<T, R?>, values: Collection<R>?) = memberOf(field.path(), values)
    fun <T, R> memberOf(path: FieldPath<T, R?>, values: Collection<R>?): Specification<T> {
        return predicate(values?.isNotEmpty() ?: false) { expr(path).`in`(values) }
    }

    // Comparable type specifications

    fun <T, R: Comparable<R>> lessThan(field: KProperty1<T, R?>, value: R?) = lessThan(field.path(), value)
    fun <T, R: Comparable<R>> lessThan(path: FieldPath<T, R?>, value: R?): Specification<T> {
        return predicate(value != null) { lessThan<R>(expr(path), value) }
    }

    fun <T, R: Comparable<R>> lessThanOrEqualTo(field: KProperty1<T, R?>, value: R?) = lessThanOrEqualTo(field.path(), value)
    fun <T, R: Comparable<R>> lessThanOrEqualTo(path: FieldPath<T, R?>, value: R?): Specification<T> {
        return predicate(value != null) { lessThanOrEqualTo<R>(expr(path), value) }
    }

    fun <T, R: Comparable<R>> greaterThan(field: KProperty1<T, R?>, value: R?) = greaterThan(field.path(), value)
    fun <T, R: Comparable<R>> greaterThan(path: FieldPath<T, R?>, value: R?): Specification<T> {
        return predicate(value != null) { greaterThan<R>(expr(path), value) }
    }

    fun <T, R: Comparable<R>> greaterThanOrEqualTo(field: KProperty1<T, R?>, value: R?) = greaterThanOrEqualTo(field.path(), value)
    fun <T, R: Comparable<R>> greaterThanOrEqualTo(path: FieldPath<T, R?>, value: R?): Specification<T> {
        return predicate(value != null) { greaterThanOrEqualTo<R>(expr(path), value) }
    }

    fun <T, R: Comparable<R>> between(field: KProperty1<T, R?>, value1: R?, value2: R?) = between(field.path(), value1, value2)
    fun <T, R: Comparable<R>> between(path: FieldPath<T, R?>, value1: R?, value2: R?): Specification<T> {
        return predicate(value1 != null && value2 != null) { between<R>(expr(path), value1, value2) }
    }

    // Collection type specifications

    fun <T, R : Collection<*>> isEmpty(field: KProperty1<T, R?>) = isEmpty(field.path())
    fun <T, R : Collection<*>> isEmpty(path: FieldPath<T, R?>): Specification<T> {
        return predicate { isEmpty(expr(path)) }
    }

    fun <T, E, R : Collection<E>> contains(field: KProperty1<T, R?>, value: E?) = contains(field.path(), value)
    fun <T, E, R : Collection<E>> contains(path: FieldPath<T, R?>, value: E?): Specification<T> {
        return predicate(value != null) { isMember(value, expr(path)) }
    }

    // String type specifications

    fun <T> like(field: KProperty1<T, String?>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false): Specification<T> {
        return like(field.path(), value, wildcardSpaces, caseSensitive)
    }
    fun <T> like(path: FieldPath<T, String?>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false): Specification<T> {
        return predicate(value != null) {
            val likeValue = if (wildcardSpaces) value!!.replace("\\s+".toRegex(), "%") else value!!
            if (caseSensitive)
                like(expr(path), likeValue)
            else
                like(lower(expr(path)), likeValue.toLowerCase())
        }
    }
    fun <T> like(concat: FieldConcat<T>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false): Specification<T> {
        return predicate(value != null) {
            val likeValue = if (wildcardSpaces) value!!.replace("\\s+".toRegex(), "%") else value!!
            if (caseSensitive)
                like(expr(concat), likeValue)
            else
                like(lower(expr(concat)), likeValue.toLowerCase())
        }
    }

    fun <T> startsWith(field: KProperty1<T, String?>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false): Specification<T> {
        return startsWith(field.path(), value, wildcardSpaces, caseSensitive)
    }
    fun <T> startsWith(path: FieldPath<T, String?>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false): Specification<T> {
        return like(path, value?.let { it + "%" }, wildcardSpaces, caseSensitive)
    }
    fun <T> startsWith(concat: FieldConcat<T>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false): Specification<T> {
        return like(concat, value?.let { it + "%" }, wildcardSpaces, caseSensitive)
    }

    fun <T> endsWith(field: KProperty1<T, String?>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false): Specification<T> {
        return endsWith(field.path(), value, wildcardSpaces, caseSensitive)
    }
    fun <T> endsWith(path: FieldPath<T, String?>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false): Specification<T> {
        return like(path, value?.let { "%" + it }, wildcardSpaces, caseSensitive)
    }
    fun <T> endsWith(concat: FieldConcat<T>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false): Specification<T> {
        return like(concat, value?.let { "%" + it }, wildcardSpaces, caseSensitive)
    }

    fun <T> contains(field: KProperty1<T, String?>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false): Specification<T> {
        return contains(field.path(), value, wildcardSpaces, caseSensitive)
    }
    fun <T> contains(path: FieldPath<T, String?>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false): Specification<T> {
        return like(path, value?.let { "%" + it + "%" }, wildcardSpaces, caseSensitive)
    }
    fun <T> contains(concat: FieldConcat<T>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false): Specification<T> {
        return like(concat, value?.let { "%" + it + "%" }, wildcardSpaces, caseSensitive)
    }
}

class CriteriaContext<T>(val root: Root<T>, val query: CriteriaQuery<*>, cb: CriteriaBuilder): CriteriaBuilder by cb {

    fun <T, R> expr(field: KProperty1<T, R?>): Expression<R?> = expr(FieldPath<T, R?>(field))

    fun <T, R> expr(path: FieldPath<T, R>): Expression<R?> {
        var from: From<*, *> = root
        for (field in path.fields.dropLast(1)) {
            from = from.fetches.find { it.attribute.name == field } as From<*, *>?
                    ?: from.joins.find { it.attribute.name == field } as From<*, *>?
                            ?: from.join<Any, Any>(field) as From<*, *>
        }
        return from.get(path.fields.last())
    }

    fun <T> expr(concat: FieldConcat<T>): Expression<String?>? {

        fun <T2> ConcatItem<T2>.expr() = when(this) {
            is ConcatFieldPath -> expr(this.value)
            is ConcatExpr -> this.value
            is ConcatText -> null
        }

        if (concat.items.size == 0)
            return null

        if (concat.items.size == 1)
            return concat.items[0].expr()

        var concatExpr: Expression<String?>? = null
        for (i in 1 until concat.items.size) {
            val item0 = concat.items[i - 1]
            val item1 = concat.items[i]
            concatExpr = when {
                i == 1 && item0 is ConcatText && item1 !is ConcatText ->
                    concat(item0.value, item1.expr())
                i == 1 && item0 !is ConcatText && item1 is ConcatText ->
                    concat(item0.expr(), item1.value)
                i == 1 && item0 !is ConcatText && item1 !is ConcatText ->
                    concat(item0.expr(), item1.expr())
                i > 1 && item1 is ConcatText ->
                    concat(concatExpr, item1.value)
                i > 1 && item1 !is ConcatText ->
                    concat(concatExpr, item1.expr())
                else -> null
            }
        }
        return concatExpr
    }

    fun <T> fetch(field: KProperty1<T, *>) = fetch(field.path())

    fun <T> fetch(path: FieldPath<T, *>) {
        // skip fetch for count query
        if (query.resultType != java.lang.Long::class.java && query.resultType != java.lang.Long::class.javaPrimitiveType) {
            var from: From<*, *> = root
            for (field in path.fields) {
                from = from.fetches.find { it.attribute.name == field } as From<*, *>?
                        ?: from.fetch<Any, Any>(field) as From<*, *>
            }
        }
    }
}

class FieldPath<T, R>(field: KProperty1<*, *>, oldPath: FieldPath<T, *>? = null) {
    val fields: List<String> = oldPath?.fields.orEmpty() + field.name
}

sealed class ConcatItem<T>
class ConcatFieldPath<T>(val value: FieldPath<T, String?>) : ConcatItem<T>()
class ConcatExpr<T>(val value: Expression<String?>) : ConcatItem<T>()
class ConcatText<T>(val value: String) : ConcatItem<T>()

class FieldConcat<T> {

    val items: MutableList<ConcatItem<T>> = mutableListOf()

    infix fun concat(text: String): FieldConcat<T> {
        items += ConcatText(text)
        return this
    }

    infix fun concat(field: KProperty1<T, String?>): FieldConcat<T> {
        items += ConcatFieldPath(field.path())
        return this
    }

    infix fun concat(path: FieldPath<T, String?>): FieldConcat<T> {
        items += ConcatFieldPath(path)
        return this
    }

    infix fun concat(expr: Expression<String?>): FieldConcat<T> {
        items += ConcatExpr(expr)
        return this
    }
}