import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.models.ProductRelease

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

val pluginName = "w-reader"
group = "com.wei"
version = "0.0.7"

repositories {
    maven {
        url = uri("https://mirrors.huaweicloud.com/repository/maven/")
    }

    maven {
        url = uri("https://maven.aliyun.com/repository/public")
    }

    mavenCentral()

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

        instrumentationTools()
        pluginVerifier()
        zipSigner()
    }

    implementation("org.yaml:snakeyaml:2.0")
    implementation("io.documentnode:epub4j-core:4.2.1")
    implementation("io.github.kevinzhwl:edge-tts-java:1.0.0")
    implementation("io.github.seth-yang:java-wrapper-for-edge-tts:1.0.1")
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
        classpath("com.guardsquare:proguard-gradle:7.3.0")
    }
}

intellijPlatform {
    pluginVerification {
        ides {
            ide(IntelliJPlatformType.IntellijIdeaUltimate, "2023.1")
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
}

val buildDir = layout.buildDirectory
tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        // 解决编译中文报错
        options.encoding = "UTF-8"
    }

    // 配置java运行环境
    withType<JavaExec> {
        // 解决控制台中文乱码
        jvmArgs = listOf("-Dfile.encoding=UTF-8", "-Dfile.stdout.encoding=UTF-8", "-Dfile.stderr.encoding=UTF-8")
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set(providers.gradleProperty("pluginSinceBuild"))
        untilBuild.set(providers.gradleProperty("pluginUntilBuild"))
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(providers.gradleProperty("publicToken"))
    }

//    // 获取构建目录
//    val buildDir = layout.buildDirectory
//    val printMappingFile = buildDir.file("mapping.txt").get()
//    val inJarsFile = buildDir.file("libs/$pluginName-$version.jar").get()
//    val proguardJar = buildDir.file("libs/$pluginName-$version-proguard.jar")
//    val proguard by registering(ProGuardTask::class) {
//        printmapping(printMappingFile)
//        configuration("proguard-rules.pro")
//
//        injars(inJarsFile)
////        injars(composedJar.map { it.archiveFile })
//        outjars(proguardJar)
//    }
//
//    // 显式声明 proguard 任务依赖于 composedJar 任务
//    proguard {
//        dependsOn(composedJar)
//    }
//
//    prepareSandbox {
//        pluginJar = proguardJar
//        dependsOn(proguard)
//    }

    intellijPlatform {
        autoReload.set(true)
    }

}