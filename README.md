# Spring Data JPA Specification builder for Kotlin

Simple copy-paste solution for querying spring data JPA repositories using spring data Specifications. Provides type-safing without generating metamodel.

Inspired by [jpa-spec](https://github.com/wenhao/jpa-spec) and  [kotlin-jpa-specification-dsl](https://github.com/consoleau/kotlin-jpa-specification-dsl).

## Example

```kotlin
// 1. Import Kotlin magic
import path_to_spec_kt.*  

// 2. Declare JPA Entities
@Entity
class Organization {
    @Id
    @GeneratedValue
    var id: Long = 0
    var name: String = ""
    @OneToMany(mappedBy = "organization")
    val groups: MutableList<Group> = mutableListOf()
}

@Entity
class Group {
    @Id
    @GeneratedValue
    var id: Long = 0
    var name: String = ""
    @ManyToOne
    lateinit var organization: Organization
    @ManyToMany(mappedBy = "groups")
    val users: MutableList<User> = mutableListOf()
}

@Entity
class User {
    @Id
    @GeneratedValue
    var id: Long = 0
    var firstName: String = ""
    var lastName: String = ""
    var middleName: String? = null
    @ManyToMany
    val groups: MutableList<Group> = mutableListOf()
}

// 3. Declare JPA Repository with JpaSpecificationExecutor
@Repository
interface UserRepository: CrudRepository<User, Long>, JpaSpecificationExecutor<User>

// 4. Create queries using specifications
data class UserFilterDTO(
        var ids: List<Long>? = null,
        var fullname: String? = null,
        var phone: String? = null,
        var groupId: Long? = null
)
@Service
class UserService(val repo: UserRepository) {
    fun findAllUsersByFilter(filter: UserFilter, pageable: Pageable): Page<User> {
        val spec = Spec.and(
                Spec.memberOf(User::id, filter.ids),
                Spec.startsWith(Spec.concat(User::lastName, User::firstName, User::middleName), filter.fullname, wildcardSpaces = true),
                Spec.contains(User::phoneNumber, filter.phone, wildcardSpaces = true), // wildcardSpaces replace space character with '%' in sql
                Spec.equal(User::groups + Group::id, filter.groupId) // User::groups + Group::id is equal to Spec.path(User::groups).plus(Group::id)
        )
        return repo.findAll(spec, pageable)
    }

    fun findAllUsersIn(usersIds: List<Long>? = null, groupsIds: List<Long>? = null, organizationsIds: List<Long>? = null): List<User> {
        val spec = Spec.or(
                Spec.memberOf(User::id, usersIds),
                Spec.memberOf(User::groups + Group::id, groupsIds),
                Spec.memberOf(User::groups + Group::organization + Organization::id, organizationsIds)
        )
        return repo.findAll(spec)
    }

    fun findAllUsersWithEmptyFirstOrLastNameInOrganization(organization: Organization): List<User> {
        val spec = Spec.and(
                Spec.equal(User::groups + Group::organization, organization),
                Spec.or(
                        Spec.equal(User::firstName, ""),
                        Spec.equal(User::lastName, "")
                )
        )
        return repo.findAll(spec)
    }

    fun findAllUsersWithoutGroups(): List<User> {
        return repo.findAll(Spec.isEmpty(User::groups))
    }

    fun findFullUserById(id: Long): User? {
        val spec = Spec.and(
                Spec.fetch(User::groups),
                Spec.equal(User::id, id)
        )
        return repo.findOne(spec)
    }
}
```

## How it works

This solution based on [Spring Data's Specifications abstraction](http://docs.spring.io/spring-data/jpa/docs/current/reference/html/#specifications) and [Kotlin Property References](http://kotlinlang.org/docs/reference/reflection.html#property-references) to remove the boilerplate and the need to generate a metamodel.