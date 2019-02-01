# Spring Data JPA Specification builder for Kotlin

Simple copy-paste solution for querying spring data JPA repositories using spring data Specifications. Provides type-safing without generating metamodel.

Inspired by [jpa-spec](https://github.com/wenhao/jpa-spec) and  [kotlin-jpa-specification-dsl](https://github.com/consoleau/kotlin-jpa-specification-dsl).

## How it works

This solution based on [Spring Data's Specifications abstraction](http://docs.spring.io/spring-data/jpa/docs/current/reference/html/#specifications) and [Kotlin Property References](http://kotlinlang.org/docs/reference/reflection.html#property-references) to remove the boilerplate and the need to generate a metamodel.