# NeforClient — Fabric 1.21.11

Fabric-клиент `1.21.11` (`yarn 1.21.11+build.6`, `loader 0.19.3`, `Java 21`, `Fabric API 0.141.6`, `Loom 1.13.6`, `Gradle 8.14`).

**Модули:** `KillAura` (GCD + профили серверов), `NeuroAura` (обучаемая на твоих движениях), `WindHop` (ветер под себя без Grim-флагов), `FakePlayer` (NPC с ∞ тотемами для теста ауры), `AutoSprint`, `AutoSell`, `AntiAfk`.

**Системы:** `HUD` (Watermark + FPS/ping, Keybinds, Cooldowns, TargetHud) с перетаскиванием в чате `T` по сетке `5px`, `ClickGui` `Right Shift` с анимациями/поиском/скроллом, `AltManager` с `SakuraBackground`, `TitleScreen` кастомный + `Нейро Тренировка`, `RaycastUtil` по хитбоксу, `GCD`/`SmoothRotationManager` вынесены в `system/rotation`.

**Нейро:** датасет `nefor/neural_dataset.json`, модель `nefor/neural_model.json` (3 веса + bias), обучение `epochs` в `NeuralTrainingScreen` — 60 квестов (~5 мин), точка `22px` засчитывает наведением без клика, `+5 epochs` каждые 3 хита, `KillAura` опция `Нейро (твои движения)` требует `80 samples / 15 epochs`.

**Команды в чате `.`:** `.cfg dir/save/load/add/delete`, `.friend add/remove/list/clear`, `.macro add <key> <text>/remove/list`, `.help`, `.vclip`, `.fakeplayer` (через модуль).

**Инфра:** `EventBus`/`ModuleManager`, `ConfigManager` `nefor/configs/*.json` (миграция со старого), `FriendManager` `friends.json`, `MacroManager` `macros.json`, `ClientPlayerEntityMixin` (silent rotation), `OpenGL` рендер `RenderSystem` (шрифты `8192` атлас, `oversample`, `glow`).

## Запуск
```
./gradlew runClient        # запуск
./gradlew build            # jar в build/libs/  (compileClientJava)
```
`JAVA_HOME = .jdks/corretto-21.0.12.1`

## Архитектура
```
exp.nefor.client/
├── NeforClient              init: ModuleManager, Friend/Macro/Config
├── event/
│   ├── api/ EventBus, Event, EventHandler
│   └── impl/ KeyEvent, ClientTickEvent, HudRenderEvent
├── module/
│   ├── ModuleManager (Right Shift → ClickGui, бинды, тик)
│   ├── api/ Module, Category (COMBAT/MOVEMENT/PLAYER/RENDER/MISC/INTERFACE), setting/ Boolean/Slider/Choice/Keybind
│   └── impl/
│       ├── combat/ KillAura         2.8-3.1 (≤3.0 raycast), GCD, профили, нейро 80/15
│       ├── NeuroAura        нейро-аура: NeuroModel.predict + твоя дрожь
│       ├── WindHop          89° pitch, хотбар приоритет, без MultiActionsC/Simulation
│       ├── FakePlayer       OtherClientPlayerEntity 2.2 блока, 20HP, ∞ тотемов
│       ├── Hud, AutoSprint, AutoSell, AntiAfk
├── system/
│   ├── rotation/ GcdUtil, RotationProfile (Vanilla/Hypixel/...), RotationEngine, SmoothRotationManager (lerp, maxYaw 12-18)
│   ├── neural/ NeuroDataset, NeuroModel (train epochs)
│   ├── FriendManager, MacroManager
├── hud/ HudRenderer (watermark fps|ping, keybinds пилюли, cooldowns bar glow, targetHud slide)
├── gui/ ClickGui (анимации, скролл, поиск), NeforTitleScreen (+ neuro инфо), AltsManagerScreen (Sakura, не закрывается на поле), NeuralTrainingScreen (60 квестов), HudEditorScreen
├── command/ CommandManager (.cfg/.friend/.macro)
├── mixin/ ClientPlayerEntityMixin (silent), ChatScreenMixin (.команды), LivingEntityJump/Movement, KeyboardMixin
├── render/ RenderSystem (8192, adaptive oversample), UiRender, font/Fonts
└── util/ RaycastUtil (Box.expand 0.08, raycast), AnimationUtil, Color
```

## Модуль с нуля
```java
public class MyModule extends Module {
    public MyModule(){ super("MyModule","описание","Category",GLFW.GLFW_KEY_G); }
    @Override protected void onEnable(){}
    @Override protected void onDisable(){}
    @Override public void onTick(){ if(MinecraftClient.getInstance().player==null) return; }
}
// ModuleManager.init(): register(new MyModule());
```

## HUD / ClickGui

**HUD:** `T` → чат → тащи `Watermark/Keybinds/Cooldowns/TargetHud` мышью, `H` (дефолт) редактор. `TargetHud` берёт `KillAura/NeuroAura.getTarget()`, `RaycastUtil.canHit`.

**ClickGui:** `Right Shift` — ЛКМ вкл/выкл, ПКМ настройки, СКМ бинд (M4/M5 тоже), поиск, скролл, `X` закрыть. Слайдер — drag.

## WindHop / Grim байпас

`90° pitch` мгновенно с `GCD snap`, `yaw` следует за мышью (не `720°` скачок → нет `AimModulo360`), `hotbar` приоритет (`setSelectedSlot` не флагает `MultiActionsC input`), из инвентаря `SWAP` только стоя (`!WASD`), `jump` убран (был `Simulation 0.42`), `LivingEntitySlowMixin` отключен. `KillAura` пауза `900ms` после ветра.

`SmoothRotationManager` — `unwrapped yaw`, `lerp 0.11-0.32`, `maxYaw 12-18`, `GCD` один раз → нет `BadPacketsJ`/`RotationPlace pre-flying`.

## Конфиги / команды

`nefor/configs/nefor.json` (+ `alts.json`, `friends.json`, `macros.json`, `neural_*.json`)

```
.cfg dir          # список
.cfg add <name>   # создать
.cfg save [name]  # сохранить
.cfg load <name>  # загрузить
.friend add <nick> / remove / list / clear
.macro add <key> <text> / remove / list  # key = G → /hub
```

`AltManager` — `SakuraBackground`, поле не закрывает экран, `Enter` вход, `R` заново, скролл.

## Нейро тренировка

`Title → Нейро Тренировка` — 60 точек `22px`, наведи — `HIT` + `reactionMs` в датасет, авто `+5 epochs` каждые 3 хита, `60/12` ≈5 мин, `loss`/`epochs` в шапке. `NeuroAura`/`KillAura` с `Нейро=вкл` используют `predict(deltaYaw,deltaPitch,dist)` + твоя дрожь.

## Уведомления / утилиты

`RenderSystem.notification("текст", Color.GREEN)` — 3 сек, `RaycastUtil.canHit`, `GcdUtil.getGcd()`, `InventoryUtil`, `ChatUtil.sendMessage`.

## Сборка

`./gradlew build` → `build/libs/neforclient-1.0.0.jar` (loom 1.13.6). Требуется `Java 21`.
