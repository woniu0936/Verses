package com.woniu0936.verses.dsl

/**
 * DSL marker for the Verses library.
 * 
 * Prevents accidental access to outer DSL scopes when nesting multiple Verse builders.
 * This ensures that methods like `item` or `items` are only called within their intended receiver scope.
 */
@DslMarker
annotation class VerseDsl