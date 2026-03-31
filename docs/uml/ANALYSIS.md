# Splendor Codebase Analysis

## 1) Architecture Overview

The project is organized into distinct layers and cross-cutting packages:

- `model`: core game state and domain entities (`GameState`, `Board`, `Player`, cards, nobles, costs, gem bank).
- `engine`: game-rule services and turn orchestration (`MoveValidator`, `MoveExecutor`, `TurnPostProcessor`, `TurnManager`, `WinnerChecker`, etc.).
- `setup` + `io` + `config`: bootstrap pipeline from config and CSV into an initialized `GameState`.
- `ui.swing`: local and multiplayer Swing frontends plus shared frame/helper classes.
- `network`: server-authoritative multiplayer transport and message protocol.
- `ai`: non-authoritative move selection strategy (`GreedyStrategy`) for local AI turns.
- root `Main`: launcher that selects local or multiplayer flow.

At runtime, `model` is the authoritative state container, `engine` mutates it through validated moves, and UI/network layers act as adapters around that rule core.

## 2) Package Responsibilities

### model

- `GameState` is the authoritative aggregate root with turn pointer and final-round flag.
- `Board` manages decks, face-up market, nobles, and a mirrored token view.
- `Player` owns token inventory, purchased cards, reserved cards, and nobles.
- `DevelopmentCard` and `NobleTile` are immutable value-rich entities.
- `Cost` models colored token requirements; `GemColor` is the shared color enum.
- `GemBank` stores global token inventory used by engine actions.

### engine

- `Move` abstracts player actions, with concrete variants:
  - `TakeDifferentMove`
  - `TakeSameMove`
  - `ReserveMove`
  - `BuyMove`
- `MoveValidator` performs legality checks before mutation.
- `MoveExecutor` applies legal move mutations to `GameState`.
- `TurnPostProcessor` applies end-of-turn effects:
  - token-cap discard enforcement
  - noble assignment via `NobleAssigner`
- `TurnProgressionService` advances turn and resolves endgame transition.
- `WinnerChecker` determines final-round triggering and winner selection.
- `TurnManager` tracks current player and final-round completion boundary.
- `TurnAdvanceResult` returns turn-advance outcomes.
- `PaymentCalculator` computes payable token mix from bonuses + tokens + gold wildcard.

### setup / io / config

- `ConfigLoader` parses and validates `config.properties` into immutable `Config`.
- `ConfigSupport` provides default config lookup and fallback behavior.
- `CardLoader` / `NobleLoader` deserialize CSV assets.
- `GameStateFactory` composes config + loaders + fallbacks into fully initialized `GameState`.

### network

- `NetworkMessage` is the serialized protocol envelope (`JOIN`, `MOVE`, `STATE_UPDATE`, etc.).
- `GameServer` is server-authoritative for lobby, turn validation/execution, and state broadcast.
- `ClientHandler` is a per-connection message loop.
- `GameClient` handles socket transport from client side.
- `ClientMain` is the multiplayer client entry flow and host-mode bootstrap.

### ui.swing

- `AbstractSwingSplendorFrame` contains shared board/action UI mechanics and pending-move building.
- `SwingSplendorApp` runs local rules engine (single-player AI and same-laptop multiplayer modes).
- `MultiplayerSwingApp` is a network-backed UI driven by server updates.
- Support classes:
  - `SwingAssetLoader` (image lookup/scaling)
  - `SwingPlayerPanelSupport` / `SwingPlayerSummaryFormatter`
  - `SwingUiTheme`
  - `SwingConfigSupport`
  - `SwingGameMode`

### ai

- `GreedyStrategy` creates legal move candidates and selects by simple heuristics/weighted randomness.

## 3) Runtime Flows

### Local single-player / same-laptop flow

1. `Main` launches `SwingSplendorApp`.
2. `SwingSplendorApp` creates initial state via `GameStateFactory`.
3. On confirm action:
   - build pending `Move` in UI
   - validate via `MoveValidator`
   - execute via `MoveExecutor`
   - apply post-turn via `TurnPostProcessor`
   - advance/check outcome via `TurnProgressionService` + `TurnManager` + `WinnerChecker`
4. If AI turn, `GreedyStrategy` selects move and same pipeline is reused.

### Multiplayer server-authoritative flow

1. Host/client connect through `GameServer` and `ClientHandler`.
2. Lobby messages (`JOIN`, `LOBBY_UPDATE`, `START_REQUEST`) synchronize readiness.
3. At game start, server creates `GameState` with `GameStateFactory`.
4. For each `MOVE` from active player:
   - validate and execute on server only
   - post-process turn and evaluate winner
   - broadcast `STATE_UPDATE` or `GAME_OVER`
5. `MultiplayerSwingApp` only reflects server snapshots and sends player intents.

## 4) Coupling, Boundaries, and Notable Design Traits

- Strong core boundary: rules and truth are in `model + engine`; UI and network call into that core.
- Server authority boundary: multiplayer correctness relies on `GameServer` being sole mutator.
- Reuse boundary: identical engine services are reused by local and network flows.
- Setup boundary: `GameStateFactory` isolates initialization concerns (CSV + fallback profiles).
- Protocol coupling: `NetworkMessage` directly carries `Move` and full `GameState` snapshots (simple, explicit, but tightly coupled to model serialization).

## 5) Diagram Coverage Notes

The UML source (`docs/uml/splendor-full.puml`) intentionally includes all project classes/enums across packages and prioritizes:

- full inheritance/implementation links
- aggregate ownership in the domain model
- rule/service orchestration dependencies
- UI-to-core and network-to-core integration edges

JDK classes are intentionally not expanded (only minimal external anchors like `Serializable`, `Runnable`, `JFrame`) to keep focus on project architecture.
