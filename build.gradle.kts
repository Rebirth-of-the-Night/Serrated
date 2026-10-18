plugins {
    id("java-library")
    id("com.gtnewhorizons.retrofuturagradle") version "2.0.2"
    id("org.jetbrains.gradle.plugin.idea-ext") version "1.1.8"
}

val modId: String = "serrated"
version = "1.0.0"
group = "st.evening.mc.rotnsaws"

val minecraftVersion: String = "1.12.2"
val atFile: String = "serrated_at.cfg"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(8)
        vendor = JvmVendorSpec.AZUL
    }
}

minecraft {
    mcVersion = minecraftVersion
    mcpMappingChannel = "stable"
    mcpMappingVersion = "39"

    useDependencyAccessTransformers = true

    injectedTags.put("MOD_ID", modId)
    injectedTags.put("VERSION", project.version)

    username = "Player"
}

tasks.deobfuscateMergedJarToSrg {
    accessTransformerFiles.from("src/main/resources/META-INF/$atFile")
}

tasks.injectTags {
    outputClassName = "${project.group}.SerrConst"
}

tasks.applyJST {
    // https://github.com/GTNewHorizons/RetroFuturaGradle/issues/101
    javaLauncher = minecraft.getToolchainLauncher(project, 25)
}

repositories {
    mavenLocal()
    maven {
        name = "CurseMaven"
        url = uri("https://cursemaven.com/")
        content {
            includeGroup("curse.maven")
        }
    }
    maven {
        name = "ModMaven"
        url = uri("https://modmaven.dev/")
    }
}

dependencies {
    api("org.jspecify:jspecify:1.0.0")

    compileOnly("mezz.jei:jei_1.12.2:4.15.0.291:api")
    implementation(rfg.deobf("CraftTweaker2:CraftTweaker2-MC1120-Main:1.12-4.1.20.702"))

    implementation(rfg.deobf("curse.maven:bwm-suite-246760:3289033"))
    implementation("betterwithmods.core:BetterWithLib:1.12-1.5:dev")
}

tasks.processResources {
    inputs.property("modId", modId)
    inputs.property("modVersion", project.version)
    inputs.property("mcVersion", minecraftVersion)

    filesMatching("mcmod.info") {
        expand(
            "modId" to modId,
            "modVersion" to project.version,
            "mcVersion" to minecraftVersion
        )
    }
}

tasks.jar {
    archiveBaseName = "$modId-$minecraftVersion"
    manifest {
        attributes("FMLAT" to atFile)
    }
}
