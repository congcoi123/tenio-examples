# CHANGELOG

### 0.0.1 2021-10-15 Feature release
**Initial release**
- Login example  
- FSM example  
- UDP attaching example  
- Stress test  
- Websocket example  
- HTTP example  

### 0.2.0 2022-04-20 Feature release
- Updated the framework dependencies  

### 0.3.0 2022-10-14 Feature release
- Supported multiple UDP channels
- Allowed fetching available UDP channel by using round-robin mechanism
- Refactoring: renamed package "extension" to "handler"
- Allowed declaring the server address in configuration file
- Supported KCP transportation
- Bugs fixed

### 0.3.1 2022-10-21 Feature release
- Added new example to show how to use MsgPack
- Added new events to handle connection-refused cases
- WebSocket handling issues fixed
- Allowed showing server's uptime
- Bugs fixed

## 0.4.0 2022-11-20 Feature release
- Introduces more annotations
- Supports self-defined commands
- Refactoring the project's structure
- Bugs fixed  

## 0.5.0 2023-08-30 Feature release
- Upgraded to JDK 17
- Upgraded dependencies
- Allowed checking logging configuration before writing logs
- configuration.xml file changed
- Introduced new annotations: @RestController and @RestMapping to work with Restful, @ClientCommand
- Added a new configuration value that allows limiting the player's IDLE time in case of non-deported state enabled
- Methods changed/updated/enhanced in classes ServerApi, Injector, ServerEvent
- Annotations enhanced: @Bean, @AutowiredQualifier
- Set names for all necessary threads
- Resolved the deadlock issue while writing packets
- Player and Room are now opened for custom classes
- Removed redundant constant, enum values
- Fixed test cases' issues
- Bugs fixed 

## 0.6.0 2024-10-09 Feature release
- Upgraded dependencies
- Enhanced performances and addressed the issue related to "100% CPU"
- Reworked on MsgPack classes and methods
- Bugs fixed

## 0.6.2 2025-05-11 Feature release
- Upgraded dependencies
- Declared behaviors for packets processing with annotations
- Bugs fixed

## 0.6.3 2025-06-01 Feature release
- Upgraded dependencies

## 0.6.4 2025-07-01 Feature release
- Upgraded dependencies

## 0.6.5 2025-08-01 Feature release
- Upgraded dependencies

## 0.6.6 2025-09-01 Feature release
- Upgraded dependencies
- Removed unnecessary wrapper methods

## 0.6.7 2025-09-02 Feature release
- Upgraded dependencies
- Adapted new APIs

## 0.6.8 2026-03-10 Feature release
- Upgraded dependencies

## 0.6.9 2026-03-23 Feature release
- Upgraded dependencies

## 0.6.10 2026-03-26 Feature release
- Upgraded dependencies
- Allowed default configurations

## 0.6.11 2026-04-03 Feature release
- Upgraded dependencies
- Stopped supporting KCP transportation

## 0.6.12 2026-04-04 Feature release
- Upgraded dependencies
- Simplified setting of the configuration file

## 0.7.0 2026-04-12 Feature release
- Supports Java 21 Features

## 0.7.1 2026-04-19 Feature release
- Upgraded dependencies

## 0.7.2 2026-04-25 Feature release
- Upgraded dependencies

## 0.7.3 2026-05-29 Feature release
- Upgraded dependencies

## 1.0.0 2026-11-11 Feature release
- Updated the tenio-core dependency to 1.0.0; added the required startup profile property to existing XML configurations.
- Removed calls to the deleted setNeverDeported API and its obsolete XML property.
- Added example12 with development, production and development+metrics profiles, selected components, bean factory methods, field injection, event handlers and a finite client.
- Reworked shared TCP/UDP transports: explicit endpoints, bounded TCP frame lengths, serialized writes, exact UDP packet lengths, peer filtering, single receive loop and idempotent close.
- Fixed the MsgPack UDP registration/convey-ID/echo flow.
- Moved delayed echoes out of reader callbacks; tied client tasks and movement measurements to connection lifetime.
- Fixed stress-client concurrent map access and identity collisions; reduced default load to 100 with an explicit system-property override.
- Made movement counters atomic across interval resets and replaced unbounded statistics lists with lifetime aggregates.
- Consolidated identical extension configuration parsing into AbstractExampleConfiguration while keeping each example's local Setting entry.
- Used Java HttpClient with timeouts in the REST example, corrected the assembly main class, honored configuration arguments and guarded terminal-command arguments.
- Added transport, profile, configuration and statistics regression tests.

## 1.1.0 2026-11-25 Framework services
- Example 11 now uses an injected `ClientCommandPolicy` instead of manual dispatch inside a player-message event. Echo has its own command code (3); the client is updated accordingly.
- Add runnable example 13 for bounded command execution, token-based snapshot recovery, delivery options, persistent performance, explicit room discovery and schema/revision-controlled replicated state.
- Add policy contract tests and an isolated headless TCP integration test, including invalid envelopes, stale revisions, token rotation and performance exports.
- Document framework service setup, default rules, application-owned wire fields and exclusive receive modes.
- Verify both clean builds: 1,606 core tests and 5 example tests pass, including mixed-handler startup diagnostics and a burst-traffic regression fix in core.

### Documentation
- Integrate command execution, recovery, response delivery, performance history, room discovery and replicated state into the main framework documentation.
- Replace the separate optional-capabilities presentation with feature sections, service usage, defaults and a framework-services walkthrough in example 13.

### WebSocket example and release
- Release examples as 1.1.0 with tenio-core 1.1.0; retain tenio-engine 1.0.0.
- Add example 7 TestClient using the JDK WebSocket transport, with finite login, fragmented movement, ping/pong and close checks. No external WebSocket client is required.
- Keep the example room alive while empty, resolve it by name, validate movement coordinates and bind locally by default. Disable packet compression for the plain MessagePack walkthrough.
- Document WS/WSS startup and add real-server integration coverage.
