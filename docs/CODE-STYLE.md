# Code style — Command Processor

Conventions for this repository. It is a **library**, not an application, and that changes most of
the rules: everything public is a promise, every signature is a thing that cannot quietly change,
and the compiler is the only documentation some consumers will read.

---

## 1. What being a library means here

Five consequences, and everything else in this document follows from them.

1. **A public type or method is a contract.** Removing one, renaming one, or changing a parameter
   type breaks every consumer at compile time. Adding one is forever.
2. **Consumers cannot see the source while they use it.** Javadoc and signatures are the interface.
3. **The library does not own the process.** It cannot decide logging configuration, cannot call
   `System.exit`, cannot spawn threads the consumer does not know about, and cannot swallow an error
   the consumer needed to see.
4. **Dependencies are inherited by every consumer.** Each one is a version conflict waiting to
   happen in somebody else's build.
5. **Failures land in someone else's application.** A message that says what is wrong and which of
   *their* classes caused it is worth more than a stack trace.

---

## 2. API surface

**Two kinds of package, and the difference is enforced by naming.**

| Package | Status |
|---|---|
| `com.github.mirko0.commandprocessor` | Public API. Semver applies |
| `com.github.mirko0.commandprocessor.annotations` | Public API |
| `com.github.mirko0.commandprocessor.enumeration` | Public API |
| `com.github.mirko0.commandprocessor.definitions` | Public API — read-only views handed to consumers |
| `com.github.mirko0.commandprocessor.internal` | **Not API.** May change in any release |

`utils` becomes `internal`. `CommandParser` and `LocalizationParser` are implementation detail that
happens to be `public static`; nothing outside the library should call either, and today nothing
stops it.

Rules:

- **`public` is a decision, not a default.** Package-private unless a consumer needs it.
- **`final` on every class not designed for extension.** `CommandProcessor` and the definitions are
  final. If a class is meant to be extended, it says so in its javadoc and its extension points are
  `protected` and documented.
- **Every package has a `package-info.java`** stating what it is for and whether it is API.
- **Return types are interfaces** (`List`, `Map`, `Set`); parameter types are the widest thing that
  works (`Collection<? extends QCommandBase>`).
- **Never return internal mutable state.** `Collections.unmodifiableMap(...)`, or a copy. A getter
  that hands back the live map makes the map's identity part of the contract.

---

## 3. No Lombok

The library does not use Lombok, and will not.

- **Deprecation is per-member.** `@Deprecated(since = "0.1.0", forRemoval = true)` has to go on the
  accessor being retired. A generated accessor cannot carry it, so an API cannot be retired
  gracefully.
- **Javadoc is per-member too.** `@return the guild this command is registered to, or null when it
  is global` is the kind of thing a consumer needs and `@Getter` cannot express.
- **Consumers should not need an annotation processor** to build against, step into, or read the
  library.
- **`@Data` generates `equals`, `hashCode` and `toString` nobody designed.** For a value type that
  is part of an API, those are contract.
- Accessors are written out. It is more lines and they are lines that never surprise anyone.

Where the boilerplate is genuinely painful, the answer is a **record** or an immutable class with a
builder — not a code generator.

---

## 4. Types and immutability

**Prefer immutable.** The definitions are built once by the parser and read forever after by the
dispatcher, from another thread. They should be constructed complete and have no setters:

```java
public final class SubcommandDefinition {

    private final String name;
    private final String description;
    private final Method handlerMethod;
    private final Response defaultResponse;

    SubcommandDefinition(Builder builder) { ... }   // package-private: only the parser builds these
}
```

That also removes a class of bug the parser has had: a definition that is half-filled is currently
representable, so "valid" is not the only state that exists.

- **Records for values** with no identity — a parsed trigger, a bound argument, a coordinate.
- **Enums for closed sets**, with behaviour attached rather than a `switch` at every use site.
- **Defensive-copy arrays.** `LocalizedEntry[]` is currently stored and returned by reference; an
  array is mutable and a caller can rewrite the library's state through the getter. Copy in and out,
  or hold a `Map<DiscordLocale, String>` and hand back an unmodifiable view.
- **Never `null` for a collection.** Empty. `getSubcommands()` returning `null` before the parser has
  run is a trap that only exists because the object can exist half-built.
- **`Optional` as a return type only.** Never a field, never a parameter.

---

## 5. Null and validation

**Every public entry point validates its arguments and says which one was wrong.**

```java
public void register(Collection<? extends QCommandBase> commands) {
    Objects.requireNonNull(commands, "commands");
    ...
}
```

- `NullPointerException` from `Objects.requireNonNull` with the parameter name, not from three frames
  deeper in a stream.
- `IllegalArgumentException` for a value that is present but wrong, with a message naming the
  offending consumer class: `"Command " + clazz.getName() + " declares ..."`. The consumer is
  debugging their code, not yours.
- **Annotate nullability** on public API using `org.jetbrains.annotations` (already on the compile
  classpath through JDA) — `@Nullable` and `@NotNull`. IDEs surface it; javadoc alone does not.

---

## 6. Errors and logging

**A library reports; it does not decide.**

- **Never swallow an exception silently.** The three places that catch broadly — per-command parsing,
  handler invocation, and reporting a failure to the user — each log with context and each carry a
  comment saying why catching is right there.
- **Unwrap `InvocationTargetException`** before logging. The wrapper's message is noise.
- **`System.out` and `System.err` are banned.** slf4j only.
- **No `printStackTrace()`.**
- Levels:

| Level | For |
|---|---|
| `error` | The consumer has a bug the library cannot work around, or a command failed at runtime |
| `warn` | The library repaired something — a trimmed description, a reordered option, a skipped duplicate |
| `info` | Registration summary. A handful of lines per startup, no more |
| `debug` | Per-invocation detail |

- **Every `warn` names the offending class**, so the consumer can find it without a debugger. This
  is the difference between a warning that gets fixed and one that gets filtered.
- **Log once per cause, not once per occurrence.** A condition that cannot change while the process
  runs is reported at registration, not on every interaction.

---

## 7. Dependencies

**JDA is `compileOnly`.** The consumer brings their own version; the library must not drag a second
one into their build. This is the reason the README can claim no external dependencies, and it is
worth keeping true.

- **No new runtime dependencies without a strong reason.** slf4j-api arrives through JDA and is the
  only thing used beyond the JDK.
- **No Spring.** The library is used from Spring, which is not the same thing. Where Spring
  integration needs support — proxied beans, injected collections — it is supported by being
  ordinary Java that works under a proxy, not by depending on Spring. See
  [AUDIT.md](AUDIT.md) A4.
- **Pin the toolchain** in `build.gradle` and treat the language level as API: raising it breaks
  every consumer on an older JDK.

---

## 8. Versioning and deprecation

**Semantic versioning**, from 0.1.0 onward.

| Change | Bump |
|---|---|
| Adding a method, annotation member with a default, or overload | Minor |
| Behaviour fix that consumers could be relying on | Minor, and say so in the changelog |
| Removing or renaming anything public; changing a parameter or return type | Major |
| Internal-only change | Patch |

**Deprecation is a two-release process and never a surprise:**

```java
/**
 * ...
 *
 * @deprecated since 0.1.0, use {@link #contexts()} — a command is often valid in more than one
 *             context and this member can only carry one. Honoured only while {@code contexts()}
 *             is empty; removal is planned for 1.0.0.
 */
@Deprecated(since = "0.1.0", forRemoval = true)
InteractionContextType contextType() default InteractionContextType.UNKNOWN;
```

- `@Deprecated` **and** a javadoc `@deprecated` tag. The annotation warns the compiler; the tag tells
  the human what to do instead. Neither is optional.
- **Always name the replacement**, with a `{@link}`.
- Deprecated members keep working for at least two minor releases.
- Nothing is removed outside a major release.
- `@since` on every new public member, from 0.1.0 onward.

---

## 9. Javadoc

**Mandatory on every public type and member.** Not aspirational — a public member without javadoc is
an incomplete change.

```java
/**
 * Registers every command in {@code commands}, skipping any that cannot be parsed.
 *
 * <p>A command that fails to parse is logged as an error and left out rather than aborting the
 * whole registration: one bad description should not stop every other command in the bot from
 * existing.
 *
 * <p>The collection is not modified.
 *
 * @param  commands the commands to register; must not be null, individual entries must carry
 *                  {@link CommandInfo}
 * @throws NullPointerException if {@code commands} is null
 * @since  0.1.0
 */
```

- First sentence is a complete summary — it is what shows up in the index.
- `@param`, `@return`, `@throws` on everything that has them. `@throws` for unchecked exceptions the
  caller can actually trigger.
- **Document thread-safety on every public type.** "Safe for concurrent use once construction has
  completed" or "not thread-safe" — a consumer cannot guess and will guess wrong.
- **Document what is *not* done**, when a reasonable person would assume it is. "Guild registration
  replaces every command in that guild" belongs in javadoc, not in a bug report.
- `<p>` between paragraphs, `{@code}` for identifiers, `{@link}` for anything navigable.
- Annotations are `@Documented`, so their members appear in the javadoc of what they annotate.

---

## 10. Size and structure

Smaller than an application's budgets, because a library file is read far more often than it is
written.

| Thing | Target | Hard cap |
|---|---|---|
| Line | 110 | 120 |
| Method body | 20 lines | 40 |
| Class | 200 lines | 300 |
| Parameters | 4 | 5 |
| Nesting | 2 | 3 |
| Cyclomatic complexity | 8 | 10 |

Accessors on a definition class do not count toward the class cap — that is exactly the boilerplate
§3 accepts deliberately. If a class is over the cap on *logic*, split it.

**Member order:** constants, fields (final first), constructors, static factories, public API,
package-private, private helpers in call order, nested types.

**One public type per file.** A nested type is allowed when it exists only for its outer type — a
builder, a private record.

**A method does one thing.** `CommandParser.parseCommand` was 60 lines doing four; it is now a flow
of named steps. Keep it that way as parsing grows.

---

## 11. Naming

| Kind | Convention |
|---|---|
| Annotation | Noun describing what it marks: `CommandInfo`, `MessageHandler` |
| Annotation member | Bare noun, no `get`: `name()`, `order()`, `choices()` |
| Definition class | `<Thing>Definition` |
| Boolean accessor | `is` / `has` / `can`, matching the setter's noun exactly |
| Internal helper | Verb phrase: `handlerMethods`, `signatureOf` |

No `Util` classes. `LocalizationParser` and `CommandParser` are named for what they do, which is
right; keeping them out of a `utils` package is the remaining half.

---

## 12. Threading

**Document it, then honour it.**

- `CommandProcessor` is built on one thread and read from JDA event threads. Everything the
  dispatcher reads must be safely published: built completely before the dispatcher is constructed,
  and held in a `ConcurrentHashMap` if it can be written afterwards.
- **`this` must not escape a constructor.** Handing a half-filled map to a handler built in the same
  constructor is exactly that.
- The library **starts no threads**. Everything runs on the caller's or JDA's threads.
- **Never block a JDA event thread.** No `.complete()`, no I/O inside dispatch.

---

## 13. Testing

JUnit 5, in `src/test/java`, **no Discord token and no network**.

Everything this library does is reflection over annotations, which is the easiest thing in the world
to test and currently is not tested at all. The suite should cover, at minimum:

- Parsing: names, descriptions, trimming, blank substitution, choices, option ordering.
- Grouping: one group with several subcommands, unknown group, group with none.
- Discovery: inherited handlers, overrides, subclass/proxy shapes, ordering stability.
- Dispatch: routing to base, subcommand and grouped subcommand; the failure path with and without a
  prior defer.
- Registration: global once, guild grouping, duplicate names.

Test names say what they assert: `parse_subcommandDescriptionOver100Chars_isTrimmed`.

Anything that needs JDA types can build them directly — `Commands.slash(...)`, `OptionData`,
`SubcommandData` all construct without a JDA instance, which is what made the audit's probe harness
possible.

---

## 14. Formatting

- 4 spaces, no tabs. Braces on the same line, always present.
- Imports: no wildcards. `import com.github.mirko0.commandprocessor.annotations.*;` in `CommandParser`
  hides which annotations it actually consumes.
- One blank line between members; none at the start or end of a body.
- `var` only where the right-hand side names the type.
- Ship `.editorconfig` so this is not a review topic.

---

## 15. Checklist

- [ ] Every new public member has javadoc with `@param` / `@return` / `@throws` / `@since`
- [ ] Nothing public added that could have been package-private
- [ ] No Lombok, no new runtime dependency, no Spring
- [ ] Public arguments validated with a message naming the consumer's class
- [ ] Internal mutable state is not returned
- [ ] Nothing mutates a caller's argument
- [ ] Thread-safety documented on any new public type
- [ ] Retired API is `@Deprecated(since, forRemoval)` **and** has a `@deprecated` tag naming the replacement
- [ ] Tests cover the new behaviour and one failure mode
- [ ] Version bumped per §8; changelog entry written
