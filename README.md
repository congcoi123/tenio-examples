<p align="center">
  <a href="https://github.com/congcoi123/tenio">
    <img src="https://raw.githubusercontent.com/congcoi123/tenio/master/assets/tenio-github-logo.png" alt="TenIO — multiplayer game server framework">
  </a>
</p>

<h1 align="center">TenIO Examples</h1>

<p align="center">
  Runnable servers and clients. Learn TenIO one example at a time.
</p>

<p align="center">
  <a href="https://github.com/congcoi123/tenio"><img src="https://img.shields.io/badge/Java-21%2B-ED8B00?style=flat&logo=openjdk&logoColor=white" alt="Java 21 or newer"></a>
  <a href="https://central.sonatype.com/artifact/io.github.congcoi123/tenio-core"><img src="https://img.shields.io/maven-central/v/io.github.congcoi123/tenio-core?style=flat&label=tenio-core" alt="tenio-core on Maven Central"></a>
  <a href="https://central.sonatype.com/artifact/io.github.congcoi123/tenio-engine"><img src="https://img.shields.io/maven-central/v/io.github.congcoi123/tenio-engine?style=flat&label=tenio-engine" alt="tenio-engine on Maven Central"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg?style=flat" alt="MIT license"></a>
</p>

<p align="center">
  <a href="https://github.com/congcoi123/tenio-examples/commits/master"><img src="https://img.shields.io/github/last-commit/congcoi123/tenio-examples?style=flat" alt="Latest commit"></a>
  <a href="https://github.com/congcoi123/tenio-examples/issues"><img src="https://img.shields.io/github/issues/congcoi123/tenio-examples?style=flat" alt="Open issues"></a>
  <a href="CONTRIBUTING.md"><img src="https://img.shields.io/badge/PRs-welcome-brightgreen.svg?style=flat" alt="Contributions welcome"></a>
  <a href="https://discord.gg/ybkNU87Psy"><img src="https://img.shields.io/discord/1146091189456613407?style=flat&logo=discord&logoColor=white&label=Discord" alt="Join the TenIO community"></a>
</p>

<p align="center">
  <a href="#build-and-run">Build &amp; run</a> ·
  <a href="https://github.com/congcoi123/tenio">TenIO</a> ·
  <a href="https://github.com/congcoi123/tenio-core#readme">Core guide</a> ·
  <a href="https://github.com/congcoi123/tenio-examples#readme">Examples</a>
</p>

Runnable Java 21 examples for `tenio-core:1.0.0` and `tenio-engine:1.0.0`.

## Build and run

Install the updated core locally before building examples if version 1.0.0 has not yet been published:

```sh
# In tenio-core
mvn -Dgpg.skip=true install

# In tenio-examples
mvn test package
```

The assembly defaults to example1's login server. Run it from the examples directory:

```sh
java -jar target/tenio-examples-1.0.0-jar-with-dependencies.jar configuration.example1.xml
```

For another entry point, use the assembled JAR as the classpath:

```sh
java -cp target/tenio-examples-1.0.0-jar-with-dependencies.jar com.tenio.examples.example3.TestServerAccessDatagramChannel configuration.example3.xml
java -cp target/tenio-examples-1.0.0-jar-with-dependencies.jar com.tenio.examples.example3.TestClientAccessDatagramChannel
```

Start each server before its client. Each example uses its own package as its component scan root. Start separate examples in separate JVMs; the server runtime still contains global services. TCP uses port 8032 unless its configuration says otherwise. Clients currently target localhost; the shared transports also accept explicit remote hosts.

## Simple Implementation
- Establishes a simple server with only a single Java class

```Java
/**
 * This class shows how a simple server handle messages that come from a client.
 */
@Bootstrap
@EventHandler
public final class SimpleServer extends AbstractHandler implements EventConnectionEstablishedResult<ZeroMap>,
        EventPlayerLogin<Player>, EventReceivedMessageFromPlayer<Player, DataCollection> {

    static void main() {
        ApplicationLauncher.run(SimpleServer.class);
    }

    @Override
    public void onConnectionEstablishedResult(Session session, ZeroMap message, ConnectionEstablishedResult result) {
        if (result == ConnectionEstablishedResult.SUCCESS) {
            api().login(message.getString(SharedEventKey.KEY_PLAYER_LOGIN), session);
        }
    }

    @Override
    public void onPlayerLogin(Player player) {
        var parcel = map().putString(SharedEventKey.KEY_PLAYER_LOGIN,
                String.format("Welcome to server: %s", player.getIdentity()));

        response().setContent(parcel).setRecipientPlayer(player).write();
    }

    @Override
    public void onReceivedMessageFromPlayer(Player player, DataCollection message) {
        DataCollection parcel = null;
        if (message instanceof ZeroMap request) {
            parcel = map().putString(SharedEventKey.KEY_CLIENT_SERVER_ECHO,
                    String.format("Echo(%s): %s", player.getIdentity(),
                            request.getString(SharedEventKey.KEY_CLIENT_SERVER_ECHO)));
        } else if (message instanceof MsgPackMap request) {
            parcel = msgmap().putString(SharedEventKey.KEY_CLIENT_SERVER_ECHO,
                    String.format("Echo(%s): %s", player.getIdentity(),
                            request.getString(SharedEventKey.KEY_CLIENT_SERVER_ECHO)));
        }

        response().setContent(parcel).setRecipientPlayer(player).write();
    }
}
```

## Examples

| Package | Demonstrates | Configuration |
| --- | --- | --- |
| example0 | Minimal server and mixed ZeroMap/MsgPack echoes | Embedded default; accepts an XML path |
| example1 | Login, request policy, custom codec extension points | configuration.example1.xml |
| example2 | Finite state machine and message dispatch | No network server |
| example3 | TCP login, UDP registration, convey ID, ZeroMap echo | configuration.example3.xml |
| example4 | Movement simulation, steering, UDP statistics | configuration.example4.xml |
| example5 | Entity/component/system simulation | No network server |
| example6 | TCP echo load generator | configuration.example6.xml |
| example7 | WebSocket server and room handlers | configuration.example7.xml |
| example8 | HTTP server and Java HttpClient request | configuration.example8.xml |
| example10 | TCP login and MsgPack UDP registration/echo | configuration.example10.xml |
| example11 | Custom terminal commands | configuration.example11.xml |
| example12 | Profile-selected components, factory methods, fields and event handlers | Three configurations below |
| example13 | Framework services: command routing/execution, recovery, delivery, performance, room discovery and replicated state | configuration.example13.xml |

Example6 defaults to 100 clients. Use `-Dtenio.clients=10000` before the main class to opt into a larger load test. It uses one platform reader thread per client and is a demonstration, not a high-scale benchmark driver.

## Example 11: Desktop Monitoring and Custom Commands

Run the server with `configuration.example11.xml`. It enables `monitor` and `history`.
On a machine with a graphical desktop, TenIO opens the monitor window. Enter `help`
to see the available commands. Try `player logout first` after connecting a test
client, then run `history` to see the disconnect. Use Up and Down to recall commands.

Example 11 selects automatic command mode through `CommandPolicy`. Its echo client now uses
short `op=3`; gameplay packets without a valid `op` are rejected. Other examples keep their
existing event handlers. ZeroMap commands use short `op=1`
and string `name` to create a room, or short `op=2` and string `chat` to send a
message to the current room. Room creation is limited to 1 request per second with a
burst of 2, and chat to 2 per second with a burst of 4, per player. Enter `limits` to
see accepted and rejected requests. TenIO also applies its normal session queue policies.

Remove `monitor` from the example profile to run without the window and system
commands. With `history` still present, TenIO writes a TSV file under `logs/history/`.
See the TenIO Core README for the full profile reference.

## Example 12: Profiles

Run the same server with different XML files:

```sh
java -cp target/tenio-examples-1.0.0-jar-with-dependencies.jar com.tenio.examples.example12.TestServerProfiles configuration.example12.development.xml
```

In another terminal:

```sh
java -cp target/tenio-examples-1.0.0-jar-with-dependencies.jar com.tenio.examples.example12.TestClientProfiles
```

Stop the server, then repeat with another configuration:

| XML file | Active profiles | Selected behavior |
| --- | --- | --- |
| configuration.example12.development.xml | development | Development greeting component and development Banner factory method |
| configuration.example12.production.xml | production | Production greeting component and production Banner factory method |
| configuration.example12.development-metrics.xml | development,metrics | Development behavior plus LoginMetrics, profile-gated field injection and MetricsHandler |

The production profile is an instructional configuration, not a hardened deployment template.

The corresponding source examples are:

- `DevelopmentGreetingService` / `ProductionGreetingService`: alternative `@Component(profile = "...")` implementations of `GreetingService`.
- `BannerFactory`: `@BeanFactory(profile = {"development", "production"})` and per-method `@Bean(profile = "...")` selection.
- `ProfileServices`: shared components can inject the selected implementation; `@Autowired(profile = "metrics")` activates a required field only for metrics.
- `MetricsHandler`: `@EventHandler(profile = "metrics")` adds optional CCU reporting.
- `TestServerProfiles`: `@Bootstrap(profile = {"development", "production"})` admits either base profile.

XML selects profiles under the top-level Server/Properties section:

```xml
<Property name="profile">development,metrics</Property>
```

Profile names are trimmed and case-sensitive. An annotation's listed names are OR alternatives. Empty annotation restrictions participate in every profile; they do not imply a profile named `default`. XML must supply at least one profile. Selection happens at startup and is not a runtime feature toggle. Profiles do not replace bean qualifiers.

Choose **one** of development or production in this example. Activating both introduces competing implementations and factory products. Using only metrics omits the base profile needed by the bootstrap and greeting service. Other examples explicitly select `default` and keep their unrestricted annotations.

## Example 13: Framework services

Install the updated core from the accompanying changes first; the example uses the framework service APIs in the updated core:

```sh
# In tenio-core
mvn -Dgpg.skip=true clean install
# In tenio-examples
mvn clean verify
java -cp target/tenio-examples-1.0.0-jar-with-dependencies.jar com.tenio.examples.example13.TestServerCapabilities configuration.example13.xml
```

In a second terminal, run the finite walkthrough client:

```sh
java -cp target/tenio-examples-1.0.0-jar-with-dependencies.jar com.tenio.examples.example13.TestClientCapabilities
```

| Framework feature | Example implementation | What to observe |
| --- | --- | --- |
| Ordered, bounded commands | `CapabilityCommands.Ready` uses `ROOM`, mailbox 64, 2 requests/second, burst 4. `State` uses `SESSION`; `Delivery` uses `CONCURRENT`; `Rooms` keeps default `INLINE`. | All command envelopes use short `op`. Startup shows `COMMAND` routing and registered names. The cached startup flag avoids registry checks in event mode. Missing, malformed and unknown codes never become player-message events. |
| Authenticated recovery | `CapabilityRecovery`, player enrollment in the login handler, and the client's second TCP connection. | The client keeps a private token, reconnects with it, verifies rotation, and checks that room/player state survives. The policy uses default snapshot strategy and 30-second grace. Capture, mutation and response use the same room monitor. |
| Response delivery | `CapabilityCommands.Delivery` sends important, replaceable (`demo-position`, two-second TTL) and best-effort responses. | The client verifies all three when the queue is healthy. Under backlog, only queued/unclaimed replaceable updates may be superseded; best-effort messages may be dropped. This does not acknowledge delivery. |
| Performance history | The XML includes `performance`; `monitor` opens the local Performance tab on a graphical desktop. | Wait at least five seconds for samples under `logs/performance/`. Use `performance 5` in the monitor to load five minutes. Headless tests verify file export; no desktop is required. |
| Public room discovery | `CapabilityFeatures` explicitly publishes one `demo` room; `Rooms` subscribes before taking a bounded page, acknowledges its revision and closes its subscription. | The client receives one public summary with room ID, name, free slots and state. Internal properties/passwords are not published. For a continuing lobby, retain the subscription, poll changes and requery on `RESET`. |
| Typed replicated state | Room `ready` is server-writable and room-visible; player `ready` is owner-visible and client-writable. The ready handler snapshots/polls a scoped subscription. | The client updates at the current revision, sees a delta, retries a stale revision and verifies rejection without changing state, then checks the recovery snapshot. |

All gameplay requests are `ZeroMap` values. Codes are encoded with `putShort`, revisions with
`putLong`, and `ready` with `putBoolean`. Login remains a connection handshake.

| `op` | Request fields besides `op` | Response |
| --- | --- | --- |
| `42` | `ready`: boolean; `revision`: last player-state revision received | `kind=ready` and a new revision, or `kind=conflict` with the current snapshot |
| `43` | None | `kind=rooms`, page revision, total and bounded public summaries |
| `44` | None | `kind=state`, player/room ID, revision and audience-filtered ready flags |
| `45` | None | `kind=important`, `kind=position`, `kind=effect` under a healthy queue |

The sample `ZeroMapCommandPolicy` implements the application's wire contract (`op` must be a
short). Core does not reserve this key. Choose **one** receive mode: no policy means
`EventReceivedMessageFromPlayer`; a policy plus registered commands means command handlers only.
Mixed registrations produce a startup warning. `DatagramPacketPolicy` remains a separate UDP
transport-envelope policy; it is not the command router. See the
[core default rules](https://github.com/congcoi123/tenio-core#extension-points-and-default-rules)
for overrides, fallback methods, queue thresholds, recovery bounds and room strategies.

This is a **localhost teaching example**: the login identity is not authenticated and the TCP
transport is not encrypted. Use authenticated login and an encrypted connection before deploying
recovery tokens remotely. The client never prints the token. State and directory envelopes are
application-defined; TenIO does not automatically broadcast them. Delivery options currently
apply to the core outbound queue, not direct Netty WebSocket writes. The walkthrough does not
simulate congestion or replay gaps; core regression tests cover those behaviors.

`mvn clean verify` includes an isolated, headless server subprocess and the real TCP client,
including invalid command envelopes, state conflicts, recovery, performance-file creation and
startup warnings when both receive handler styles are registered.
The test uses TCP port 8032; stop other example servers before running it. The subprocess keeps
its logs in the test temporary directory and is stopped even if assertions fail.

## Shared Example Client

`client/TCP`, `client/UDP`, `ClientCodec` and `ClientTasks` centralize transport and task lifecycle behavior. New code should connect explicitly, assign its fields, configure error/disconnection handlers, then start receiving. Constructor callbacks remain only as compatibility adapters.

TCP sends are synchronous and serialized. Listener callbacks run on the reader and must remain short; the simple echo examples schedule delayed sends instead of sleeping in callbacks. Closing a transport stops its reader; close owned timers and UDP together when TCP disconnects. UDP decodes only the received byte range and filters unrelated senders in peer-specific mode.

The sample compression/encryption classes are **pass-through teaching placeholders**. They neither compress nor protect data and must match the server's configured codecs. UDP registration by player name demonstrates the flow; a deployed application needs authenticated registration. Configure TCP main-connection expiry only alongside a suitable application heartbeat. UDP activity cannot keep TCP alive.

## Requirements
Java 21+

## License
The [`TenIO`](https://github.com/congcoi123/tenio) project is currently available under the [MIT](LICENSE) License.

## Changelog
Please check out the [changelog](CHANGELOG.md) for more details.

## Contributing
Please check out the [contributing guideline](CONTRIBUTING.md) for more details.

## Installation
GitHub
```sh
$ git clone https://github.com/congcoi123/tenio-examples.git
```

> Happy coding !
