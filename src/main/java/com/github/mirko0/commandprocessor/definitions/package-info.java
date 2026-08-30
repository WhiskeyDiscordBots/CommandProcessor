/**
 * The parsed form of a command, as read by the dispatcher and the help command.
 *
 * <p>Public API, but effectively read-only: the setters exist for the parser, and mutating a
 * definition after registration changes what a live dispatcher sees.
 *
 * @since 0.0.1
 */
package com.github.mirko0.commandprocessor.definitions;
