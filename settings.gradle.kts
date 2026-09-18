rootProject.name = "BlockProt-Reloaded"

include("common")

val subprojects = listOf("spigot")
subprojects.forEach {
    include(it)
    project(":$it").name = "blockprot-$it"
}
