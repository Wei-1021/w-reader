import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask
import proguard.gradle.ProGuardTask

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.16.0"
}

val pluginName = "w-reader"
group = "com.wei"
version = "0.2.0"

repositories {
    maven {
        url = uri("https://mirrors.huaweicloud.com/repository/maven/")
    }

    maven {
        url = uri("https://maven.aliyun.com/repository/public")
    }

    maven {
        url = uri("https://github.com/psiegman/mvn-repo/raw/master/releases")
    }

    mavenCentral()

    exclusiveContent {
        forRepository {
            maven {
                url = uri("https://raw.githubusercontent.com/DFKI-MLT/Maven-Repository/main")
            }
        }
        filter {
            includeGroup("de.dfki.lt.jtok")
        }
    }

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
dependencies {
    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
//        create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))

        local(providers.gradleProperty("localIDEAPath"))

        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })

        // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file for plugin from JetBrains Marketplace.
        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })

        pluginVerifier()
        zipSigner()
    }

    implementation("org.yaml:snakeyaml:2.2")
    implementation("io.documentnode:epub4j-core:4.2.2")
    implementation("com.googlecode.soundlibs:jlayer:1.0.1.4")
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    implementation("com.jayway.jsonpath:json-path:2.9.0")
    implementation("org.codehaus.janino:janino:3.1.9")
    // apache commons
    implementation("org.apache.commons:commons-text:1.14.0")
}

buildscript {
    repositories {
        maven {
            url = uri("https://mirrors.huaweicloud.com/repository/maven/")
        }

        maven {
            url = uri("https://maven.aliyun.com/repository/public")
        }

        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.4.2")
    }
}

intellijPlatform {
    pluginVerification {
        ides {
            create(IntelliJPlatformType.IntellijIdeaUltimate, "2023.1")
            local(providers.gradleProperty("localIDEAPath"))
            recommended()
            select {
                types = listOf(IntelliJPlatformType.IntellijIdeaUltimate)
                channels = listOf(ProductRelease.Channel.RELEASE)
                sinceBuild = providers.gradleProperty("pluginSinceBuild")
                untilBuild = providers.gradleProperty("pluginUntilVerifyBuild")
            }
        }
    }

    pluginConfiguration {
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
        // 解决编译中文报错
        options.encoding = "UTF-8"
    }

    // 配置java运行环境
    withType<JavaExec> {
        // 解决控制台中文乱码
        jvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dfile.stdout.encoding=UTF-8", "-Dfile.stderr.encoding=UTF-8")
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }

//    patchPluginXml {
//        sinceBuild = providers.gradleProperty("pluginSinceBuild")
//        untilBuild = providers.gradleProperty("pluginUntilBuild")
//    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(providers.gradleProperty("publicToken"))
    }

    intellijPlatform {
        autoReload.set(true)
    }

}

