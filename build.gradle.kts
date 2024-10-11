import proguard.gradle.ProGuardTask;

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

val pluginName = "w-reader"
group = "com.wei"
version = "0.0.3"

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
        classpath("com.guardsquare:proguard-gradle:7.5.0")
    }
}

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
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    val proguardJar = layout.buildDirectory.file("libs/$pluginName-$version-proguard.jar")
    val proguard by registering(ProGuardTask::class) {
        printmapping(layout.buildDirectory.file("mapping.txt").get())
        configuration("proguard-rules.pro")

//        injars(composedJar.map { it.archiveFile })
        val inputJar = layout.buildDirectory.file("libs/$pluginName-$version.jar")
        println("inputJar: ${inputJar.get()}")
        injars(inputJar.get())
        outjars(proguardJar)
    }

    prepareSandbox {
        pluginJar = proguardJar
        dependsOn(proguard)
    }

    composedJar {
        dependsOn(proguard)
    }

    intellijPlatform {
        autoReload.set(true)
    }

}