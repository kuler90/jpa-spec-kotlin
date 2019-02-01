
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.domain.Specifications
import java.util.*
import javax.persistence.criteria.*
import kotlin.reflect.KProperty1

// Path operators

operator fun <T, R, R2> KProperty1<T, R?>.plus(prop: KProperty1<R, R2?>): Spec.FieldPath<T, R2?> = Spec.FieldPath(prop.name, from = Spec.FieldPath<T, R?>(this.name))
operator fun <T, R, R2> Spec.FieldPath<T, R?>.plus(prop: KProperty1<R, R2?>): Spec.FieldPath<T, R2?> = Spec.FieldPath(prop.name, from = this)
@JvmName("plusRel")
operator fun <T, E, R: Collection<E>, R2> KProperty1<T, R>.plus(prop: KProperty1<E, R2?>): Spec.FieldPath<T, R2?> = Spec.FieldPath(prop.name, from = Spec.FieldPath<T, R?>(this.name))
@JvmName("plusRel")
operator fun <T, E, R: Collection<E>, R2> Spec.FieldPath<T, R?>.plus(prop: KProperty1<E, R2?>): Spec.FieldPath<T, R2?> = Spec.FieldPath(prop.name, from = this)

object Spec {

    inline fun <reified T> empty(): Specification<T> = Specifications.where<T>(null)

    fun <T> not(spec: Specification<T>): Specification<T> = Specifications.not(spec)

    fun <T> and(vararg specs: Specification<T>): Specification<T> {
       return specs.fold(Specifications.where<T>(null)) { acc, next -> acc.and(next) }
    }

    fun <T> or(vararg specs: Specification<T>): Specification<T> {
        return specs.fold(Specifications.where<T>(null)) { acc, next -> acc.or(next) }
    }

    // Path builder

    fun <T, R> path(prop: KProperty1<T, R?>): FieldPath<T, R?> = FieldPath(prop.name)

    // Concat builder

    fun <T> concat(vararg props: KProperty1<T, String?>) = FieldConcat<T>().concat(*props)
    fun <T> concat(vararg fields: FieldPath<T, String?>) = FieldConcat<T>().concat(*fields)
    fun <T> concat(text: String) = FieldConcat<T>().concat(text)

    // Fetch specification

    fun <T, R> fetch(vararg props: KProperty1<T, R?>): Specification<T> {
        return fetch(*props.map { path(it) }.toTypedArray())
    }

    fun <T, R> fetch(vararg fields: FieldPath<T, R?>): Specification<T> {
        return Specification { root, query, _ ->
            // skip fetch for count query
            if (query.resultType != java.lang.Long::class.java && query.resultType != java.lang.Long::class.javaPrimitiveType) {
                fields.forEach { it.fetch(root) }
            }
            null
        }
    }

    // Any type specification

    fun <T, R> isNull(prop: KProperty1<T, R?>) = isNull(path(prop))
    fun <T, R> isNull(path: FieldPath<T, R?>): Specification<T> = custom(path) { isNull(it) }

    fun <T, R> equal(prop: KProperty1<T, R?>, value: R?, checkNull: Boolean = false) = equal(path(prop), value, checkNull)
    fun <T, R> equal(path: FieldPath<T, R?>, value: R?, checkNull: Boolean = false): Specification<T> {
        return custom(path, value != null || checkNull) { equal(it, value) }
    }

    fun <T, R> memberOf(prop: KProperty1<T, R?>, values: Collection<R>?) = memberOf(path(prop), values)
    fun <T, R> memberOf(path: FieldPath<T, R?>, values: Collection<R>?): Specification<T> {
        return custom(path, values?.isNotEmpty() ?: false) { it.`in`(values) }
    }

    // Comparable type specification

    fun <T, R: Comparable<R>> lessThan(prop: KProperty1<T, R?>, value: R?) = lessThan(path(prop), value)
    fun <T, R: Comparable<R>> lessThan(path: FieldPath<T, R?>, value: R?): Specification<T> {
        return custom(path, value != null) { lessThan<R>(it, value) }
    }

    fun <T, R: Comparable<R>> lessThanOrEqualTo(prop: KProperty1<T, R?>, value: R?) = lessThanOrEqualTo(path(prop), value)
    fun <T, R: Comparable<R>> lessThanOrEqualTo(path: FieldPath<T, R?>, value: R?): Specification<T> {
        return custom(path, value != null) { lessThanOrEqualTo<R>(it, value) }
    }

    fun <T, R: Comparable<R>> greaterThan(prop: KProperty1<T, R?>, value: R?) = greaterThan(path(prop), value)
    fun <T, R: Comparable<R>> greaterThan(path: FieldPath<T, R?>, value: R?): Specification<T> {
        return custom(path, value != null) { greaterThan<R>(it, value) }
    }

    fun <T, R: Comparable<R>> greaterThanOrEqualTo(prop: KProperty1<T, R?>, value: R?) = greaterThanOrEqualTo(path(prop), value)
    fun <T, R: Comparable<R>> greaterThanOrEqualTo(path: FieldPath<T, R?>, value: R?): Specification<T> {
        return custom(path, value != null) { greaterThanOrEqualTo<R>(it, value) }
    }

    fun <T, R: Comparable<R>> between(prop: KProperty1<T, R?>, value1: R?, value2: R?) = between(path(prop), value1, value2)
    fun <T, R: Comparable<R>> between(path: FieldPath<T, R?>, value1: R?, value2: R?): Specification<T> {
        return custom(path, value1 != null && value2 != null) { between<R>(it, value1, value2) }
    }

    // Collection type specification

    fun <T, R : Collection<*>> isEmpty(prop: KProperty1<T, R?>) = isEmpty(path(prop))
    fun <T, R : Collection<*>> isEmpty(path: FieldPath<T, R?>): Specification<T> {
        return custom(path) { isEmpty(it) }
    }

    fun <T, E, R : Collection<E>> contains(prop: KProperty1<T, R?>, value: E?) = contains(path(prop), value)
    fun <T, E, R : Collection<E>> contains(path: FieldPath<T, R?>, value: E?): Specification<T> {
        return custom(path, value != null) { isMember(value, it) }
    }

    // String type specification

    fun <T> like(prop: KProperty1<T, String?>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false) = like(path(prop), value, wildcardSpaces, caseSensitive)
    fun <T> like(path: FieldPath<T, String?>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false): Specification<T> {
        return custom(path, !value.isNullOrBlank()) {
            val likeValue = if (wildcardSpaces) {
                value!!.replace("\\s+".toRegex(), " ").replace(" ", "%")
            } else {
                value!!
            }
            if (caseSensitive)
                like(it, likeValue)
            else
                like(lower(it), likeValue.toLowerCase())
        }
    }
    fun <T> like(concat: FieldConcat<T>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false): Specification<T> {
        return custom(!value.isNullOrBlank()) { root ->
            val likeValue = if (wildcardSpaces) {
                value!!.replace("\\s+".toRegex(), " ").replace(" ", "%")
            } else {
                value!!
            }
            val concatExpr: Expression<String?>? = concat.build(root, this)
            if (caseSensitive)
                like(concatExpr, likeValue)
            else
                like(lower(concatExpr), likeValue.toLowerCase())

        }
    }

    fun <T> startsWith(prop: KProperty1<T, String?>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false) = startsWith(path(prop), value, wildcardSpaces, caseSensitive)
    fun <T> startsWith(path: FieldPath<T, String?>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false) = like(path, value?.let { it + "%" }, wildcardSpaces, caseSensitive)
    fun <T> startsWith(concat: FieldConcat<T>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false) = like(concat, value?.let { it + "%" }, wildcardSpaces, caseSensitive)

    fun <T> endsWith(prop: KProperty1<T, String?>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false) = endsWith(path(prop), value, wildcardSpaces, caseSensitive)
    fun <T> endsWith(path: FieldPath<T, String?>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false) = like(path, value?.let { "%" + it }, wildcardSpaces, caseSensitive)
    fun <T> endsWith(concat: FieldConcat<T>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false) = like(concat, value?.let { "%" + it }, wildcardSpaces, caseSensitive)

    fun <T> contains(prop: KProperty1<T, String?>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false) = contains(path(prop), value, wildcardSpaces, caseSensitive)
    fun <T> contains(path: FieldPath<T, String?>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false) = like(path, value?.let { "%" + it + "%" }, wildcardSpaces, caseSensitive)
    fun <T> contains(concat: FieldConcat<T>, value: String?, wildcardSpaces: Boolean = false, caseSensitive: Boolean = false) = like(concat, value?.let { "%" + it + "%" }, wildcardSpaces, caseSensitive)

    // Custom specification

    fun <T, R> custom(path: FieldPath<T, R?>, condition: Boolean = true, makePredicate: CriteriaBuilder.(Path<R?>) -> Predicate): Specification<T> {
        return Specification { root, _, cb -> if (condition) cb.makePredicate(path.get(root)) else null }
    }

    fun <T> custom(condition: Boolean = true, makePredicate: CriteriaBuilder.(Root<T>) -> Predicate?): Specification<T> {
        return Specification { root, _, cb -> if (condition) cb.makePredicate(root) else null }
    }

    // Helpers

    private val SHARED_JOINS_CACHE: MutableMap<Root<*>, MutableMap<Path<*>, From<*, *>>> = Collections.synchronizedMap(WeakHashMap())

    class FieldPath<T, R>(field: String, from: FieldPath<T, *>? = null) {

        private val fields: List<String> = from?.fields.orEmpty() + field

        fun get(root: Root<T>): Path<R> {
            val joinsCache = SHARED_JOINS_CACHE.getOrPut(root) { mutableMapOf() }
            var from: From<*, *> = root
            for (field in fields.dropLast(1)) {
                val path = from.get<Any>(field)
                if (path !in joinsCache) {
                    joinsCache[path] = from.join<Any, Any>(field)
                }
                from = joinsCache[path]!!
            }
            return from.get(fields.last())
        }

        fun fetch(root: Root<T>) {
            val joinsCache = SHARED_JOINS_CACHE.getOrPut(root) { mutableMapOf() }
            var from: From<*, *> = root
            for (field in fields) {
                val path = from.get<Any>(field)
                if (path !in joinsCache) {
                    joinsCache[path] = from.fetch<Any, Any>(field) as From<*, *>
                }
                from = joinsCache[path]!!
            }
        }
    }

    class FieldConcat<T> {
        private data class Item<S>(val field: FieldPath<S, String?>?, val text: String?)
        private var items: List<Item<T>> = emptyList()

        fun concat(text: String): FieldConcat<T> {
            items += Item(null, text)
            return this
        }

        fun concat(vararg fields: FieldPath<T, String?>): FieldConcat<T> {
            items += fields.map { Item(it, null) }
            return this
        }

        fun concat(vararg props: KProperty1<T, String?>): FieldConcat<T> {
            items += props.map { Item(path(it), null) }
            return this
        }

        fun build(root: Root<T>, cb: CriteriaBuilder): Expression<String?>? {
            when {
                items.size == 0 ->
                    return null
                items.size == 1 && items[0].field != null ->
                    return items[0].field?.get(root)
            }
            var concatExpr: Expression<String?>? = null
            for (i in 0 until items.size - 1) {
                val item0 = items[i]
                val item1 = items[i + 1]
                when {
                    concatExpr == null && item0.text != null && item1.field != null ->
                        concatExpr = cb.concat(item0.text, cb.trim(item1.field.get(root)))
                    concatExpr == null && item0.field != null && item1.text != null ->
                        concatExpr = cb.concat(cb.trim(item0.field.get(root)), item1.text)
                    concatExpr == null && item0.field != null && item1.field != null ->
                        concatExpr = cb.concat(cb.trim(item0.field.get(root)), cb.trim(item1.field.get(root)))
                    concatExpr != null && item1.field != null ->
                        concatExpr = cb.concat(concatExpr, cb.trim(item1.field.get(root)))
                    concatExpr != null && item1.text != null ->
                        concatExpr = cb.concat(concatExpr, item1.text)
                }
            }
            return concatExpr
        }
    }
}