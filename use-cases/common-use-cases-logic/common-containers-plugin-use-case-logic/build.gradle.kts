plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.mongodb:mongodb-driver-sync:5.2.0")
    api("org.junit.jupiter:junit-jupiter-api:5.11.3")
    api("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    api("org.slf4j:slf4j-api:2.0.16")
}
