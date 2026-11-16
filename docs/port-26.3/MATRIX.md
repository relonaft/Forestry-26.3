# Матрица: оригинал Forestry 5.8.2 против Community Edition 1.21.1

Этап 1 плана. Сверка содержимого: что из оригинала есть в основе порта, что изменено, чего нет.
Версии источников записаны в [STATUS.md](STATUS.md).

## Как получено и чему верить

- Виды: список констант в `BeeDefinition`, `TreeDefinition`, `ButterflyDefinition` оригинала против файлов
  видов Community Edition. Скрипт [tools/matrix_species.py](tools/matrix_species.py).
- Предметы и блоки: английские названия из `en_us.lang` оригинала (283 названия) против названий и против
  списка предметов в творческих вкладках Community Edition (1975 идентификаторов, файл
  `creative-tab-baseline.txt`). Скрипт [tools/matrix_content.py](tools/matrix_content.py).
- Каждый пропуск проверен по исходникам оригинала: был ли предмет зарегистрирован и доступен в игре.
- Это сверка **наличия**. Значения (продукты пчёл, рецепты, расход энергии, логика ферм) не сверялись:
  это этап 5.

## Виды

| Группа | В оригинале | В Community Edition | Чего из оригинала нет |
|---|---|---|---|
| Пчёлы | 44 | 69 | ничего |
| Деревья | 35 | 50 | ничего, 6 переименованы |
| Бабочки и мотыльки | 31 | 35 | ничего, 7 переименованы |

Переименования деревьев: `acacia_vanilla` → `acacia`, `acacia` (Desert Acacia) → `camelthorn`,
`gigant` → `giant_sequoia`, `mahogony` → `mahogany`, `sipiri` → `greenheart`, `zebrawood` → `zebrano`.
По журналу изменений Hill Cherry заменена на Sour Cherry, Bull Pine на Ponderosa Pine.

## Есть в оригинале, нет в Community Edition

Подтверждено по исходникам оригинала: предмет зарегистрирован и работает без сторонних модов.

| Что | Где в оригинале | Состояние в Community Edition |
|---|---|---|
| Habitat Former и климатология | модуль `climatology` | блока нет, осталось название |
| Habitat Screen | `climatology/items/ItemHabitatScreen` | нет |
| Habitat Locator | `apiculture/items/ItemHabitatLocator` | предмета нет, остались текстура и пакет |
| Database | модуль `database` | блока нет, осталось название |
| Rain Tank | `factory`, `TileRaintank` | нет |
| Loam | модуль `charcoal`, стенка угольной ямы | нет |
| Minecart with Bee House, Minecart with Apiary | `apiculture/entities/EntityMinecart*` | нет |
| Wax Cast и рецепты стекла в Thermionic Fabricator | `ItemWaxCast`, `ModuleFactory` | нет |
| Imprinter | `apiculture/items/ItemImprinter` | нет, осталось название |
| Broken Pickaxe, Broken Shovel | `core/items/ItemRegistryCore` | нет |
| Pulsating Dust | `crafting_material` | убран в 3.0.0-alpha9 |
| Honey Pot | модуль `food` | убран в 3.0.0-alpha9 |
| Встроенная книга лесника | модуль `book`, 58 классов | заменена книгой на Patchouli (73 файла страниц) |

## Есть в оригинале только вместе с другими модами

Под 26.3 этих модов нет, поэтому перенести как есть нельзя.

| Что | От чего зависит |
|---|---|
| Electrical Engine, Bio Generator | IndustrialCraft 2, регистрируются в `plugins/BlockRegistryIC2` |
| Rubberised Electron Tube, каучуковые культуры | IndustrialCraft 2, TechReborn |
| Orchid Electron Tube, ферма орхидей | Extra Utilities |
| Apiarist's Pipe | BuildCraft |
| Модули совместимости в `plugins` | IC2, BuildCraft, TechReborn, Natura, Biomes O' Plenty и другие |

## В оригинале есть название, но в игре недоступно

Возвращать нечего.

- Rubber Farm (`plantation`): закомментирована в `BlockRegistryCultivation`.
- Infuser, Camouflage Spray Can, Camouflaged Paneling: кода нет, только строки перевода.
- Соты Reddened, Darkened, Omega, капли Charged и Omega: помечены как секретные, выдаются только в режиме
  отладки. Sticky Propolis нигде не используется.
- Irradiated Comb: продукт видов Vindictive, Vengeful и Avenging. В Community Edition такой соты нет, а
  эта линия пчёл по журналу изменений переделана. Разобрать на этапе 5.
- Блоки теплицы и климатизаторы (`greenhouse`, 535 строк): блоки без механики, остаток убранной теплицы.

## Изменено в Community Edition

| Что в оригинале | Что в Community Edition |
|---|---|
| Wood Pile, Decorative Wood Pile | Log Pile, Decorative Log Pile |
| Свечи: Candle, Stump, крашеные | набор Big Candle, Jumbo Candle, Rainbow Candle (40 предметов) |
| Плантаторы модуля `cultivation` | блоки `*_managed` и `*_manual` |
| Copper Ore, Copper Ingot, Block of Copper | ванильная медь |
| Alveary Stabiliser | `alveary_stabilizer` |
| Гаплоидных трутней нет | включены по умолчанию |
| Баланс пчёл оригинала | изменён в 2.10.0: продукты, активность и эффекты ряда видов |

## Добавлено в Community Edition

25 видов пчёл, около 14 видов деревьев, Combustion Engine, Solar Engine, Burn Barrel, янтарь, топор, меч и
мотыга выживальщика, блоки из золы и воска, глубинные руды, новые свечи.

## Рецепты машин

Числа несравнимы напрямую: оригинал добавляет рецепты в циклах по сортам дерева и цветам, Community Edition
хранит по файлу на вариант. Таблица показывает, куда смотреть на этапе 5.

| Машина | Вызовов регистрации в оригинале | Файлов рецептов в Community Edition |
|---|---|---|
| Squeezer | 53 | 25 |
| Carpenter | 33 | 217 |
| Thermionic Fabricator | 20 | 288 |
| Centrifuge | 18 | 20 |
| Fermenter | 7 | 19 |
| Плавка в Fabricator | 7 | 6 |
| Moistener | 6 | 4 |
| Still | 2 | 1 |

Squeezer, Moistener и Still проверить первыми: файлов меньше, чем вызовов.

## Что не сверялось

Интерфейсы, параметры конфигурации, команды, генерация мира (ульи, деревья, руды), жители, достижения,
значения и поведение каждого механизма. Сверка по именам классов даёт слишком много ложных расхождений
из-за переименований, см. [tools/matrix_classes.py](tools/matrix_classes.py). Это делается при аудите по
слоям.

## Решения владельца репозитория

Приняты владельцем репозитория 2026-11-16 в предложенном виде. Правило: что было в оригинале, должно быть в
порте.

1. **Вернуть** всё из раздела «Есть в оригинале, нет в Community Edition».
2. **Не переносить** то, что зависит от других модов. Вариант на выбор: сделать Electrical Engine и Bio
   Generator на энергии NeoForge. Это отступление от оригинала.
3. **Не возвращать** недоступное в оригинале и блоки теплицы.
4. **Оставить** переименования и перестройки Community Edition: Log Pile, свечи, блоки ферм, имена видов.
5. **Гаплоидные трутни**: выключить по умолчанию, как в оригинале. Настройка остаётся.
6. **Баланс 44 оригинальных видов пчёл**: вернуть значения оригинала. Новые виды не трогать.
7. **Добавления Community Edition** оставить.
