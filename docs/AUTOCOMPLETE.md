# Autocomplete

`@CommandOption(autoComplete = true)` tells Discord to ask the bot for suggestions as the user
types. `@AutoCompleteHandler` is what answers.

Before 0.2.0 the flag was registered and nothing dispatched the request, so the option showed an
empty dropdown that spun and gave up.

---

## 1. The shape

```java
@CommandInfo(name = "play", description = "Play a track.")
public class PlayCommand implements QCommandBase {

    @Autowired private MusicLibrary library;

    @CommandHandler(defaultResponse = Response.DEFER)
    public void play(@CommandEvent SlashCommandInteractionEvent event,
                     @CommandOption(type = OptionType.STRING, name = "track", required = true,
                             description = "What to play.", autoComplete = true)
                     String track) {
        library.play(track);
    }

    @AutoCompleteHandler(option = "track")
    public List<String> trackSuggestions(@AutoCompleteValue String typed) {
        return library.search(typed);
    }
}
```

The handler lives on the same class as the command, so it has the same injected dependencies. No
provider classes to register, no interfaces to implement.

---

## 2. Parameters

All optional — a handler may take none at all.

| Annotation | Injected |
|---|---|
| `@AutoCompleteValue` | What the user has typed so far, as a `String`. Empty rather than null before they type anything, so it can be filtered without a null check |
| `@AutoCompleteEvent` | The `CommandAutoCompleteInteractionEvent` |
| `@CommandOption` | Another option they have already filled in — the same binding the command handler uses |

That third one is how one option's suggestions depend on another:

```java
@AutoCompleteHandler(option = "track")
public List<String> tracks(@AutoCompleteValue String typed,
                           @CommandOption(type = OptionType.STRING, name = "playlist",
                                   description = "Which playlist.")
                           String playlist) {
    return library.search(playlist, typed);   // playlist is null until they pick one
}
```

Discord sends whatever is filled in so far, so an option the user has not reached yet is `null` —
handle it.

---

## 3. Return value

Any `Collection`. Elements are converted against the option's type:

| Element | Result |
|---|---|
| `Command.Choice` | Used as-is — full control over label and value |
| `String` | Label and value both the string. On an `INTEGER`/`NUMBER` option it is parsed; unparseable entries are dropped with a warning |
| `Number` | Label is `toString()`, value is the number |
| `null` element | Skipped |
| `null` return | No suggestions |

Returning something that is not a `Collection` is caught at **registration**, not at runtime — the
handler is refused with an error naming the method.

### Limits, and why two of them behave differently

| Limit | Behaviour |
|---|---|
| 25 suggestions | Truncated, with a warning saying to filter on what was typed |
| 100-character label | **Trimmed.** It is what the user reads; shortening it costs nothing |
| 100-character value | **Dropped.** It is what Discord sends back when picked, so a truncated one resolves to nothing. Corrupting data silently is worse than one missing suggestion |

---

## 4. Scoping

An option name is only unique within a subcommand. Two subcommands with the same option name that
want different suggestions need a scope:

```java
@AutoCompleteHandler(option = "key")                        // any subcommand
@AutoCompleteHandler(option = "key", subcommand = "clear")  // just /cfg clear
@AutoCompleteHandler(option = "key", group = "g", subcommand = "s")
```

**The most specific match wins**, so a catch-all and a narrow handler can coexist: `/cfg clear key:`
gets the scoped one, every other subcommand gets the catch-all.

---

## 5. Timing

**Discord allows three seconds, and an autocomplete response cannot be deferred.** There is no
`deferReply` equivalent — you answer or you do not.

The handler therefore runs inline on the JDA event thread. It fires on **every keystroke**. So:

- No unbounded I/O. Cache, or query something with a timeout well under three seconds.
- Filter server-side on `@AutoCompleteValue` rather than returning everything and slicing.
- A handler that throws is logged and answered with an empty list — suggestions are a convenience
  and a broken provider must not stop the user typing.

---

## 6. What the library refuses, and what it says

Discord rejects three shapes outright, each as an `IllegalStateException` out of JDA that used to
abort the whole command:

| Declared | What happens now |
|---|---|
| `autoComplete` on a type other than `STRING`, `INTEGER`, `NUMBER` | Disabled, `error` naming the option and its type |
| `autoComplete` **and** `choices` on one option | Choices kept, autocomplete disabled, `error`. Choices are explicit data; autocomplete is a promise to supply some |
| More than 25 `choices` | Truncated, `warn` |

Two more conditions are reported because otherwise they are invisible:

- An option marked `autoComplete` with **no matching handler** — warned at registration, and once
  more at runtime the first time it is used. Discord is sent an empty list rather than being left
  waiting, because an unanswered request shows the user an error indistinguishable from a bot that
  is down.
- A handler whose option **is not** marked `autoComplete` — warned at registration; it would never
  run.

Both fire once. Autocomplete requests arrive on every keystroke and the condition cannot change
while the process runs.

---

## 7. Where it lives

- `SlashCommandHandler.onCommandAutoCompleteInteraction` — routing, one map lookup then a
  specificity match.
- `internal/ParameterBinder` — shared with the command dispatcher, since both bind `@CommandOption`
  against a `CommandInteractionPayload`.
- `internal/ChoiceCoercion` — return-value conversion and the limits above.
- `definitions/AutoCompleteDefinition` — the parsed handler and its scope.

`ParameterBinder` is the extraction [AUDIT.md](AUDIT.md) called for under *Structural observations*:
the second dispatch path needed the same binding, so it was pulled out before the copy existed
rather than after.
