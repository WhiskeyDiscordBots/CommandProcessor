# Message commands

An optional second dispatch path: the same command classes, triggered by a chat message instead of a
slash command. Off unless configured. Prefix configurable and required by default, with a per-command
opt-out so a bare `uno` works.

**Shipped in 0.3.0**, revised in **0.4.0**. This document is both the design and the reference;
§10 records where it differs from the original plan and §11 what 0.4.0 changed.

---

## 1. Read this first

**A bare `uno` trigger needs the `MESSAGE_CONTENT` privileged intent.** Without it Discord delivers
an empty `getContentRaw()` for messages that do not mention the bot, so every prefixless and every
prefixed trigger silently never fires. The intent must be switched on in the developer portal, and
past 100 servers Discord has to approve it.

There is one exception worth designing around: **Discord sends full content without the intent when
the message mentions the app, or is a DM to it.** So `@Bot uno` works on a bot with no privileged
intents at all. The config below supports mention-prefix as a first-class trigger for exactly that
reason — it is the only form of this feature that is free.

Since 0.4.0 **a mention satisfies every `PrefixMode`**, `NONE` included. The mode is about the
configured prefix, and a mention is not that prefix; a `NONE` command that refused `@Bot uno` would
have made the free path unreachable for exactly the commands most likely to want it.

**A prefixless trigger fires on every message in every channel of every guild.** `uno` is a common
English word. Section 5 is about containing that, and it is not optional.

---

## 2. What it looks like

```java
@Component
@CommandInfo(name = "uno", description = "Start a game of UNO!")
@MessageCommandInfo(names = {"uno", "startuno"}, prefix = PrefixMode.OPTIONAL)
public class UnoCommand implements QCommandBase {

    @Autowired private GameManager gameManager;

    @CommandHandler
    public void execute(@CommandEvent SlashCommandInteractionEvent event) {
        Lobbies.open(event, gameManager);
    }

    @MessageHandler
    public void onMessage(@MessageEvent MessageReceivedEvent event, @MessageArgs Arguments args) {
        Lobbies.open(event, gameManager);
    }
}
```

Three separate decisions, deliberately:

- `@CommandInfo` — the slash command. Unchanged.
- `@MessageCommandInfo` — the message triggers. Its absence means the class has no message form,
  whatever the global config says.
- `@MessageHandler` — the code that runs. A distinct method, because a message invocation has no
  interaction, no deferral, no ephemeral reply and no resolved options. Sharing one method with the
  slash path would mean a body full of "which am I?" branching, which is how the predecessor bot's
  `CommandEvent` wrapper ended up with a null `SlashCommandInteractionEvent` and a null
  `MessageReceivedEvent` and every handler checking both.

A message-only command simply omits `@CommandInfo`; a slash-only command omits the other two. Both
are ordinary.

---

## 3. New public API

### `PrefixMode`

```java
public enum PrefixMode {
    /** Only with the configured prefix: {@code !uno}. The default. */
    REQUIRED,
    /** With or without: {@code !uno} and {@code uno}. */
    OPTIONAL,
    /** Only without: {@code uno}. The prefixed form is not registered. */
    NONE
}
```

`accepts(prefixed, mentioned)` is the live form; `accepts(prefixed)` is deprecated since 0.4.0
because it cannot tell a configured prefix from a mention.

### `@MessageCommandInfo`

| Member | Default | Meaning |
|---|---|---|
| `String[] names()` | — | Triggers. First is canonical, rest are aliases. Required |
| `PrefixMode prefix()` | `REQUIRED` | Per §above |
| `boolean guildOnly()` | `true` | Ignore DMs |
| `Permission[] permissions()` | `{}` | Member permissions required. There is no Discord-side gate on a message command, so this is checked by the dispatcher |
| `boolean deleteTrigger()` | `false` | Delete the invoking message. Needs `MESSAGE_MANAGE`; logged and skipped if absent |

### `@MessageHandler`

```java
@MessageHandler                       // the bare command; receives every argument
@MessageHandler(name = "set")         // claims a first argument of "set"
```

Subcommand routing is by exact match on the first argument, and it is opt-in: with a single
`@MessageHandler` and no name, the handler receives everything. `callout @user` is one handler
reading `args.user(0)`; `!config set volume 5` reaches `@MessageHandler(name = "set")` with
`[volume, 5]` — **the claimed word is dropped**, so a named handler never re-parses its own name.

If no named handler matches, the unnamed one runs with the arguments untouched. If there is no
unnamed one either, nothing happens and nothing is logged: the user typed something the command does
not do, which is not an error worth shouting about.

### `MessageInvocation`

*Since 0.4.0.* **How** the command was typed, as opposed to what it was given.

```java
public record MessageInvocation(String trigger, String rest, boolean prefixed, boolean mentioned) {
    public boolean bare();      // neither a prefix nor a mention
}
```

A command that means two things needs this, and a game bot's usually does. `uno` typed bare in a
channel with a game running is *"I have one card left"*; `!uno` typed where there is no game is
*"start one"*. Both reach the same command, and only the invocation form separates them — the
`PrefixMode` cannot, because `OPTIONAL` is precisely one command that arrives both ways.

Reachable from the filter (below) and from a handler parameter.

### Parameter annotations

| Annotation | Injected |
|---|---|
| `@MessageEvent` | The `MessageReceivedEvent` |
| `@MessageArgs` | `Arguments` — everything after the trigger (and after the subcommand name, if one matched) |
| `@MessageArgs` on a `MessageInvocation` parameter | The invocation, since 0.4.0. The annotation means "the parsed invocation"; the parameter's **type** picks the view |
| `@MessageArg(0)` | One positional argument, converted to the parameter's type |

`@CommandOption` is **not** reused. It carries an `OptionType`, `required`, `autoComplete` and
`choices` — none of which exist for a message command — and reusing it would mean one annotation
whose members are half-meaningless depending on the caller.

### `Arguments`

A small immutable value type, not `String[]`, so it can offer accessors that never throw and never
return null unexpectedly:

```java
int size();  boolean isEmpty();
Optional<String> get(int index);
String getOrDefault(int index, String fallback);
List<String> all();                  // unmodifiable
String remainder();                  // everything after the trigger, unsplit
String remainderFrom(int index);     // for "say the rest of this"
Optional<Long> asLong(int index);
Optional<Double> asDouble(int index);
Optional<User> user(int index);      // a mention or raw id, from users the message already carries
Arguments skipFirst();               // what a named handler receives
```

`user(int)` never fetches. The dispatcher runs on a JDA event thread, so it resolves only against
users the message already carries — a mention Discord resolved, or the author. An id for anyone else
is empty rather than retrieved.

`remainder()` matters: quoting rules are a rabbit hole and most commands want either tokens or the
whole tail. Splitting is on runs of whitespace. **No quote handling in the first version**, and the
javadoc says so, because half-implemented quoting is worse than none.

---

## 4. Configuration

```java
MessageCommandConfig config = MessageCommandConfig.builder()
        .prefix("!")                    // required unless mention-only
        .allowMentionPrefix(true)       // "@Bot uno" — works with no privileged intent
        .caseSensitive(false)
        .ignoreBots(true)               // default; also ignores webhooks and itself
        .filter(new UnoChannelFilter())  // see §5
        .build();

CommandProcessor.builder()
        .commands(commands)
        .helpCommand(false)
        .messageCommands(config)        // absent => feature entirely off
        .build()
        .registerAll(shardManager);
```

**The feature is off when `messageCommands` is not called.** No listener is attached, no trigger
index is built, and `onMessageReceived` is never reached — a bot that does not want this pays
nothing for it.

A builder is introduced here rather than leaving a fourth constructor overload as the only way in.
The existing constructors are **not** deprecated — they are fine for the simple cases and deprecating
working API to churn two consumers would be noise. A four-argument constructor exists as well, for
anyone who prefers it.

### Startup validation

When message commands are enabled — the first four at registration, the intent check when the
listener is attached:

- **No `MESSAGE_CONTENT` intent, and at least one trigger is not mention-only** → `error`, once,
  naming the intent and saying the triggers will never fire. This is the single most valuable line in
  the feature: without it the symptom is "nothing happens", which is indistinguishable from a typo,
  a permission problem, or a bug in the consumer's handler.
- **A prefix that is empty while any command is `REQUIRED`** → `error`.
- **A prefix ending in a word character** (`uno` as a prefix) → `warn`: `!uno` and `unofficial` stop
  being distinguishable.
- **Two commands claiming the same trigger** → `warn` naming both classes; first registration wins.
- **`PrefixMode.NONE` or `OPTIONAL` with no filter configured** → `warn`, once, listing the bare
  triggers. §5.
- **A trigger containing whitespace** → `error`; it is matched against one token so it could never
  fire, and the command is not registered.
- **`@MessageCommandInfo` with no `@MessageHandler`** → `error`; typing the trigger would do
  nothing, so it is not registered.

---

## 5. Containing prefixless triggers

A `PrefixMode.NONE` trigger named `uno` fires on any message whose first word is `uno`, anywhere the
bot can see. In a busy guild that is a lot of unwanted invocations, and every one of them is work the
bot does for nobody.

Two mechanisms, both in the design from the start:

**Exact first-token match.** The trigger is compared against the first whitespace-delimited token,
not with `startsWith`. The predecessor bot used `content.toLowerCase().startsWith("uno")`, which
fires on `unofficial results are in`.

**`MessageCommandFilter`.**

```java
@FunctionalInterface
public interface MessageCommandFilter {

    boolean allows(MessageReceivedEvent event, MessageCommandDefinition definition);

    /** Since 0.4.0, and what the dispatcher actually calls. */
    default boolean allows(MessageReceivedEvent event, MessageCommandDefinition definition,
                           MessageInvocation invocation) {
        return allows(event, definition);
    }
}
```

For UNO this is most of the answer:

```java
boolean allows(MessageReceivedEvent event, MessageCommandDefinition definition,
               MessageInvocation invocation) {
    // A bare "uno" only means anything in a channel that has a running game.
    return invocation.prefixed()
            || gameManager.findByChannel(event.getChannel().getIdLong()) != null;
}
```

`!uno` still starts a game anywhere; bare `uno` only does something where a game is already running.
That is both the correct product behaviour and the thing that stops the bot reacting to ordinary
conversation.

**Ask the invocation, not the definition.** The obvious version of this test is
`definition.getPrefixMode() == PrefixMode.REQUIRED`, and it is wrong for the case that matters: a
`PrefixMode.OPTIONAL` command is *one* command that can arrive prefixed or bare, so its mode is the
same either way and tells you nothing about the message in front of you. That is what the
three-argument overload is for.

The filter is per-processor, runs after trigger matching, and returning `false` is silent — no reply,
no log above `debug`. A filter that rejects most traffic is the normal case, not an error.

---

## 6. Dispatch

`MessageCommandHandler extends ListenerAdapter`, registered only when configured.

```
onMessageReceived
  ├── config.ignoreBots and author is bot/webhook/self        → return
  ├── guildOnly and not from a guild                          → return
  ├── content empty                                           → return   (also: no MESSAGE_CONTENT)
  ├── strip mention prefix if present and allowed             → prefixed = true
  ├── else if content starts with prefix, strip it            → prefixed = true
  ├── first token → trigger, rest → Arguments
  ├── look up trigger in the index (lowercased unless caseSensitive)
  │     ├── no match                                          → return
  │     ├── prefixed   and mode == NONE                       → return
  │     └── unprefixed and mode == REQUIRED                   → return
  ├── filter.allows(event, definition) is false               → return
  ├── member lacks definition.permissions()                   → reply, return
  ├── resolve @MessageHandler by first argument, else the bare one
  └── bind parameters, invoke, delete trigger if configured
```

The index is a `Map<String, MessageCommandDefinition>` built once at parse time, so dispatch is one
hash lookup on the first token — not a scan over every command, which is what the predecessor's chain
of `startsWith` checks amounted to.

**Ordering against slash commands is not a concern**: they arrive on different gateway events and
cannot collide.

### The shared binder

`SlashCommandHandler.invoke` already binds parameters by annotation and calls a method. The message
path needs the same thing with a different event type and a different set of parameter annotations.
Extract it before writing the second copy:

```java
final class ParameterBinder {
    Object[] bind(Method method, BindingSource source);
}
```

with `SlashBindingSource` and `MessageBindingSource` supplying values per annotation. That is also
where [AUDIT](AUDIT.md) A3 (raw-vs-lowercased option name), A7 (unwrapping
`InvocationTargetException`) and A11 (`setAccessible` on every call) get fixed once instead of twice.

### Failure handling

No interaction, so nothing to acknowledge and nothing that times out. A handler that throws is
logged with the command name, the guild and the user, and the invoking message gets a short reply.
Whether it replies at all is config: `MessageCommandConfig.replyOnError(boolean)`, default true,
because a silent failure in a chat command is invisible.

---

## 7. What is deliberately not in it

- **Quoted arguments.** `Arguments` splits on whitespace. Documented, not half-done.
- **Per-guild prefixes.** The library holds one prefix. A per-guild prefix needs storage, which is
  the consumer's concern — the hook for it is `MessageCommandConfig.prefixResolver(Function<Guild,
  String>)`, and that can be added later without breaking anything.
- **Message-command groups.** Slash commands have groups because Discord's UI needs them. A chat
  command does not; `@MessageHandler(name = ...)` covers the real cases.
- **Reaction or button triggers.** Different feature.
- **Automatic parity between slash and message forms.** Nothing checks that a command with
  `@CommandInfo` also has `@MessageCommandInfo`. They are independent on purpose.

---

## 8. Work order

1. `PrefixMode`, `@MessageCommandInfo`, `@MessageHandler`, `@MessageEvent`, `@MessageArgs`,
   `@MessageArg`, `Arguments`, `MessageCommandFilter` — API surface, javadoc, no behaviour.
2. Extract `ParameterBinder` from `SlashCommandHandler`; slash path keeps working, tests prove it.
3. `MessageCommandParser` building `MessageCommandDefinition` and the trigger index; duplicate and
   malformed-trigger warnings.
4. `MessageCommandConfig` + builder; `CommandProcessor.Builder`; deprecate the old constructors.
5. `MessageCommandHandler` dispatch, attached only when configured.
6. Startup validation, including the `MESSAGE_CONTENT` check.
7. Tests: trigger matching by mode, alias resolution, case handling, mention prefix, filter
   rejection, argument splitting and resolution, handler routing by first argument, permission
   refusal.
8. README and javadoc; a `@since` on everything new.

Steps 1–2 are independent of the rest and 2 is worth doing regardless of whether this feature ships.

---

## 9. Consequence for the UNO bot

The V2 plan currently drops the message-command path in order to drop `MESSAGE_CONTENT`, on the
grounds that the bot reads nothing else. Enabling this feature reverses that: the intent goes back
on, has to be approved past 100 servers, and the bot is once again asking to read every message in
every server it is in.

That trade is a product decision, not a technical one, and the shapes are:

| Option | Intent | `uno` bare trigger |
|---|---|---|
| Slash only | None privileged | No |
| Mention prefix only (`@Bot uno`) | **None privileged** | Only when mentioning the bot |
| Prefix only (`!uno`) | `MESSAGE_CONTENT` | No |
| Prefix + bare, filtered to channels with a live game | `MESSAGE_CONTENT` | Yes |

The middle option is worth a look before committing to the intent: it keeps a typed trigger, costs
nothing in review risk, and `@Bot uno` is barely longer than `!uno`.


---

## 10. Where the implementation differs from the plan

Three changes, all made while building it.

**`MessageParsing` is a separate pure class.** The plan had the split inline in the dispatcher. It
came out because the matching rules are the part that must not regress — exact-token matching, the
two mention forms, prefix stripping — and testing them through a mocked `MessageReceivedEvent` would
have meant testing almost nothing. `MessageParsing.parse(content, prefix, allowMention, selfId)`
takes no JDA types and has 17 tests against it.

**`@MessageArg` is bound by `ParameterBinder`, not a separate path.** The binder already existed for
slash and autocomplete, so it grew a message context rather than a third copy. It is also where the
"a missing argument is null, so never use a primitive" rule is enforced in one place.

**No `deleteTrigger` permission failure.** The plan said the deletion needs `MESSAGE_MANAGE`; what it
did not say is what happens without it. The command has already run successfully by that point, so a
missing permission logs and skips rather than reporting a failure for something that worked.

Everything else is as designed, including the deliberate omissions in §7 — no quoting, no per-guild
prefixes, no message-command groups.

---

## 11. What 0.4.0 changed

Four fixes and one addition, all found while wiring UNO's `uno` trigger up to this.

**Triggers are folded with `Locale.ROOT`.** `TriggerIndex` and the dispatcher both called bare
`toLowerCase()`, which uses `Locale.getDefault()`. On a JVM running with a Turkish or Azeri locale
`"I"` lowercases to `"ı"` — dotless — so a user typing `IHAVEUNO` matched nothing. It looked
symmetric and was not: the trigger side is already lowercase in the annotation, so only the *user's*
typing was transformed. Both sides now go through one `MessageParsing.key(trigger, caseSensitive)`
so they cannot drift again, and the same fix was applied to the other bare `toLowerCase()` calls in
the slash path — 17 sites in total. Covered by `TriggerNormalisationTest`, which sets a Turkish
default locale and restores it afterwards.

**A mention satisfies every `PrefixMode`.** `stripMention` set `prefixed = true`, and
`NONE.accepts(true)` was false — so `@Bot uno` could not reach a bare-only command, which is the one
form that needs no privileged intent. `MessageInvocation` now records `mentioned` separately and
`PrefixMode.accepts(prefixed, mentioned)` treats a mention as always acceptable. `accepts(boolean)`
is deprecated, not removed.

**`deleteTrigger` moved out of the `try`.** A deletion that failed was caught by the handler's
`catch`, logged as "command failed", and answered with *"Something went wrong running that."* — for
a command that had already run correctly.

**`TriggerIndex.needsMessageContent()` removed.** Dead, and its javadoc claimed something it did not
do (*"at least one trigger can fire without the bot being mentioned"*; it returned *"the index is
not empty"*). Internal class, so removing it breaks nothing.

**`MessageInvocation` added**, plus the filter's three-argument overload and `@MessageArgs` binding
by parameter type. A command that means different things prefixed and bare could not previously tell
which had happened — see §3 and §5.

The `isBot` check is **not** a change: `MessageCommandHandler` has ignored bots, webhooks and its own
messages since 0.3.0, gated on `MessageCommandConfig.ignoreBots()`, default true. The predecessor
bot's listener checks none of the three.

Nothing here breaks a consumer that does not touch `internal/`. The removed and re-shaped types —
`MessageParsing.Invocation`, `TriggerIndex.needsMessageContent()`, `ParameterBinder.Context` — are
all in that package.
