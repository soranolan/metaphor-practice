buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.apache.commons:commons-lang3:3.18.0")
        classpath("tools.jackson.core:jackson-core:3.1.1")
    }
}

plugins {
	java
	jacoco
	id("org.springframework.boot") version "4.0.5"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.practice"
version = "0.0.1-SNAPSHOT"

// override tomcat version to fix CVE
extra["tomcat.version"] = "11.0.21"
// override jackson-bom version to fix CVE
extra["jackson-bom.version"] = "3.1.1"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
  	implementation("org.springframework.boot:spring-boot-starter-actuator")
  	implementation("org.springframework.boot:spring-boot-starter-webmvc")
  	implementation("io.micrometer:micrometer-registry-prometheus")
  	implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:4.0.1")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")
  	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
  	runtimeOnly("org.postgresql:postgresql")
  	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
  	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
  	testImplementation("org.mybatis.spring.boot:mybatis-spring-boot-starter-test:4.0.1")
	testImplementation("com.h2database:h2")
  	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
	finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required.set(true)
		html.required.set(true)
		csv.required.set(true)
	}
}

jacoco {
	toolVersion = "0.8.14"
}
