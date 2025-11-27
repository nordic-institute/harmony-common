plugins {
  `kotlin-dsl`
}

group = "org.niis.harmony"

repositories {
  gradlePluginPortal()
  mavenCentral()
}

dependencies {
  implementation(libs.jackson.dataformat.yaml)
  implementation(libs.jackson.module.kotlin)
  implementation(libs.commons.compress)
  implementation(libs.commons.codec)

  testImplementation(kotlin("test"))
  testImplementation(gradleTestKit())
  testImplementation(libs.mockk)
}

tasks.test {
  useJUnitPlatform()
}
