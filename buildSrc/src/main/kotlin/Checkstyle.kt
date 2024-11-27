import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.kotlin.dsl.get

fun Project.configureCheckstyle() {
    extensions.getByType(CheckstyleExtension::class.java).apply {
        toolVersion = "8.18"
    }

    val checkstyle = tasks.register("checkstyle", Checkstyle::class.java) {
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        source = fileTree("src/") {
            include("**/*.java")
            exclude("**/external/**/*.java")
        }
        classpath = files()
    }

    tasks["check"].dependsOn(checkstyle)
}