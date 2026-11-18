# Forestry for Minecraft 26.3 (unofficial port)

> **Unofficial, alpha.** This is a port of the Forestry mod to **Minecraft 26.3 on NeoForge**.
> It is not made, endorsed or supported by the Forestry or Forestry: Community Edition developers.

## Status

**Alpha, for testing.** The port builds and runs. It has not been played by a person yet, so expect bugs and
keep backups of your worlds.

What has been checked:

- A clean build with no errors.
- 189 automated in-game tests (GameTest) pass: bees, trees, butterflies, machines, engines, farms, multiblocks,
  energy transfer, mail addresses, recipes and data.
- A dedicated NeoForge 26.3 server starts with only the release jars (no JEI, no Curios) and stops cleanly.
- The client was checked from screenshots: menus, all creative tabs, the main machine screens, an apiary
  producing combs and offspring, a multiblock farm screen, chests, signs, boats, butterflies, the
  Forester's Manual in English and Russian, and JEI with recipe transfer.

Not checked yet: spectacles, a multiblock farm running with circuits, and mail between two players.

Worlds from Forestry: Community Edition 1.21.1 or older Forestry versions are not supported. Start a new world.

The detailed state is in [docs/port-26.3](docs/port-26.3) (in Russian).

## Installing

1. Install [NeoForge](https://neoforged.net/) **26.3.0.103 or newer** for Minecraft 26.3.
2. Download the jars from the [Releases](https://github.com/relonaft/Forestry-26.3/releases) page and put them in
   your `mods` folder:
   - `forestry-26.3-<version>.jar` (required: bees, trees, machines, engines, backpacks and the rest)
   - `forestryfarms-26.3-<version>.jar` (multiblock farms)
   - `forestrybutterflies-26.3-<version>.jar` (butterflies)
   - `forestrymail-26.3-<version>.jar` (mail and trade stations)

   The last three need the first one. Use all four to get the full mod.
3. Optional: [JEI](https://www.curseforge.com/minecraft/mc-mods/jei) 31.17 or newer shows Forestry recipes.

The Forester's Manual is built into the mod. Patchouli is not needed.

## What it is built from

- **Code base:** [Forestry: Community Edition](https://github.com/thedarkcolour/ForestryCE), branch `1.21.1`,
  commit `85800f8` (3.0.0-alpha9). This repository is a fork of it.
- **Content:** the port keeps the Community Edition as it is. Features that the Community Edition removed from
  the original Forestry 5.8.2 for 1.12.2 are not brought back.

Branches: `26.3` is the port. `1.21.1` is an unmodified mirror of the Community Edition.

## Reporting problems

Report problems with this port in [this repository's issues](https://github.com/relonaft/Forestry-26.3/issues).
Do not report them to Forestry or Forestry: Community Edition. Their developers have nothing to do with this port.

## Credits and license

Forestry is (c) 2011-2014 SirSengir and contributors, among them CovertJaguar, Player, MysteriousAges, Binnie,
mezz and Nedelosk. Forestry: Community Edition is by thedarkcolour and contributors.

This port is licensed under LGPL-3.0, the same as both of them. See [LICENSE.md](LICENSE.md). The changes made
by this port are the commits on the `26.3` branch after `85800f8`.

## По-русски

Неофициальный порт мода Forestry на Minecraft 26.3 (NeoForge). **Альфа-версия для пробы:** мод собирается и
запускается, проходит 189 автоматических тестов в игре, но живой игрок в него ещё не играл. Делайте резервные
копии миров. Миры из Forestry: Community Edition 1.21.1 и старых версий не поддерживаются, начинайте новый мир.

Установка: NeoForge 26.3.0.103 или новее, четыре jar со страницы
[Releases](https://github.com/relonaft/Forestry-26.3/releases) в папку `mods`. JEI по желанию. Основа кода:
Forestry: Community Edition 1.21.1, содержимое сохранено как в ней. Русский перевод полный.

Состояние работ: [docs/port-26.3](docs/port-26.3). Об ошибках порта пишите в
[issues этого репозитория](https://github.com/relonaft/Forestry-26.3/issues), а не авторам Forestry.
