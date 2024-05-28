# ZenithProxy MC Data Generator

This subproject generates JSON and Java class files of Minecraft data for use in ZenithProxy.

This project is purposefully not added as a direct dependency to avoid increasing ZenithProxy build times unnecessarily.
The data itself only needs to be generated one time per MC version.

A few of these generators are inspired by [Archengius/minecraft-data-generator](https://github.com/Archengius/minecraft-data-generator/)
which is a source for [PrismarineJS/minecraft-data](https://github.com/PrismarineJS/minecraft-data/).

Java class files are generated using [JavaPoet](https://github.com/square/javapoet). 
The main advantage to java class files is replacing indirect map lookups with static instance references.

For example: 

`if (itemStack.getId() == ItemRegistry.DIAMOND_SWORD.id()) return itemStack;` 

versus

`if (itemStack.getId() == ItemRegistry.getItemData("diamond_sword").id()) return itemStack`

## Usage

The main ZenithProxy project must first be published to your local maven repo:

1. Enable the `jar` task (temporarily)
    ```kotlin
    tasks {
        // ...
        jar { enabled = true }
        // ...
    }
    ```
2. Execute the `publishToMavenLocal` task

Then in this `dataGenerator` gradle project:

1. Execute the `runServer` task
2. Manually copy files from `build/data` into the main ZenithProxy project.

JSON data files should be copied to `src/main/resources/mcdata`. 

Java class files should be copied to their corresponding packages in `src/main/java/com/zenith/mc`.
