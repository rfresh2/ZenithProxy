# ZenithProxy MC Data Generator

This subproject generates JSON files of Minecraft data for use in ZenithProxy.

Many of these generators are currently copied from [Archengius/minecraft-data-generator](https://github.com/Archengius/minecraft-data-generator/)
which is a source for [PrismarineJS/minecraft-data](https://github.com/PrismarineJS/minecraft-data/).

But this is not required and will diverge further over time.

## Usage

Execute the `runServer` gradle task. Data files will be generated and the server stopped automatically.

Data files are written to `build/data`

These JSON files can then be manually copied to `src/main/resources/mcdata` in the main project once ready
