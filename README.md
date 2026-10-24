# Termin Finder for Android

MVP of an Android app that watches [service.berlin.de](https://service.berlin.de) for free appointment
(*Termin*) slots, so you don't have to refresh the booking page yourself.

## What it does

- Pick a service by name and, optionally, in the nearest futures which, offices to watch
- Check on a repeating interval in the background
- Notify you when dates appear that weren't offered before
- Show per-task status and the last check result
- Turn notifications off in settings

## Build

```bash
./gradlew :androidApp:assembleDebug
```

## Stack

Kotlin, Compose, MVI like, DataStore, Ktor. The scraping and parsing
live in the separate [termin-core](https://github.com/jied83/termin-core) library;
