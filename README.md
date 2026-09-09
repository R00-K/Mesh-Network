# Disaster Mesh

A Flutter front end with a native Kotlin core for a disaster-resilient, Wi-Fi-based mesh communication system.

## Current milestone

Phase 1 and Phase 2 are implemented:

- `mesh-core` contains platform-independent Kotlin domain models, configuration, and logging interfaces.
- `lib/` contains the Flutter application and presentation layer.
- `android/` contains the native Android host and Flutter method-channel bridge.
- No Wi-Fi, Wi-Fi Direct, discovery, routing, or physical networking is implemented yet.

## Architecture

```text
Flutter application (`lib/`)
        |
Flutter method channel
        |
Native Android Kotlin bridge (`android/`)
        |
Platform-independent native mesh core (`mesh-core`)
        |
Future: discovery -> metrics -> topology -> cost engine -> routing -> messaging
```

The core module is deliberately independent of Android and Flutter so routing and cost calculations can be tested on a desktop JVM.

## Domain models

The initial models are `Node`, `NodeId`, `Neighbor`, `Link`, `Metric`, `LinkMetrics`, `Route`, and `Message`. They validate basic invariants such as metric ranges, positive TTLs, non-negative timestamps, and distinct link endpoints.

## Build and tests

Use Flutter for the front end:

```bash
flutter pub get
flutter test
flutter build apk
```

The native core can be tested independently with a compatible Gradle 8.x installation:

```bash
gradle :mesh-core:test
```

## Next milestone

Add metric collector interfaces and deterministic simulated metric data, then implement normalized link-cost calculation with configurable weights and hard safety thresholds.

Physical Wi-Fi networking will be added only after the core topology, cost, and routing components are tested.
