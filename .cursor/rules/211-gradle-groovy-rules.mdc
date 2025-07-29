---
description: Gradle Groovy Cursor Rules
globs: 
alwaysApply: false
---
# Gradle Groovy Configuration Rules

## Essential Configuration
```groovy
plugins {
    id 'org.springframework.boot' version '3.4.5'
    id 'io.spring.dependency-management' version '1.1.4'
    id 'java'
}

group = 'com.pollosseum'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '17'

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    // Test Dependencies
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
}

test {
    useJUnitPlatform()
}

// Performance & Build Settings
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
```

## Common Tasks
```groovy
bootRun {
    args = ["--spring.profiles.active=dev"]
}

tasks.named('test') {
    useJUnitPlatform()
}
```

## See also:
- [210-java-spring-cursor-rules.mdc](mdc:.cursor/rules/210-java-spring-cursor-rules.mdc) for Java and Spring guidelines
- [002-tech-stack.mdc](mdc:.cursor/rules/002-tech-stack.mdc) for technology stack details
