# jpa-spec-kotlin

Simple copy-paste solution for querying Spring Data JPA repositories using Spring Data Specifications. Provides type-safing without generating metamodel. 

Inspired by [jpa-spec](https://github.com/wenhao/jpa-spec) and  [kotlin-jpa-specification-dsl](https://github.com/consoleau/kotlin-jpa-specification-dsl).

## Examples

### Basic usage

```kotlin
interface UserRepository: CrudRepository<User, Long>, JpaSpecificationExecutor<User>
```

```kotlin
fun findAll(filter: UserFilterDTO, pageable: Pageable): Page<User> {
    val spec = Spec.and(
        Spec.memberOf(User::id, filter.ids),
        Spec.like(User::lastName, filter.lastName, wildcardSpaces = true), // wildcardSpaces replaces ' ' with '%'
        Spec.equal(User::gender, filter.gender),
        Spec.greaterThanOrEqualTo(User::balance, 0),
        Spec.between(User::birthday, filter.birthdayStart, filter.birthdayEnd)
    )
    return userRepository.findAll(spec, pageable)
}
```

### And/Or/Not

```kotlin
fun findAll(filter: UserFilterDTO): List<User> {
    val spec =  Spec.and(
        Spec.or(
            Spec.equal(User::firstName, filter.name1),
            Spec.equal(User::firstName, filter.name2)
        ),
        Spec.not( Spec.equal(User::firstName, filter.nameBlocked) )
    )
    return userRepository.findAll(spec)
}
```

### Join

```kotlin
fun findAll(filter: UserFilterDTO): List<User> {
    val spec = Spec.equal(User::groups join Group::id, filter.groupId)
    return userRepository.findAll(spec)
}
```

### Concat

```kotlin
fun findAll(filter: UserFilterDTO): List<User> {
    val spec = Spec.startsWith(User::lastName concat User::firstName concat User::middleName, filter.fullname, wildcardSpaces = true)
    return userRepository.findAll(spec)
}
```

### Fetch

```kotlin
fun findAll(filter: UserFilterDTO): List<User> {
    val spec = Spec.and(
        Spec.equal(User::gender, filter.gender),
        Spec.fetch(User::groups), 
    )
    return userRepository.findAll(spec)
}
```
If `and()` or `or()` contains specifications with `join` then `fetch()` should be added at the end to minimize the number of joins in sql.

### Custom Specification with DSL

```kotlin
fun findAll(filter: UserFilterDTO): Page<User> {
    val spec = Spec.predicate<User> {
        fetch(User::groups)
        and(
            notEqual(expr(User::groups join Group::id), filter.groupId),
            like(expr(User::lastName concat User::firstName concat User::middleName), filter.fullname)
        )
    }
    return userRepository.findAll(spec)
}
```