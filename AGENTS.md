# AGENTS.md

# Tiffin Kitchen Discovery & Subscription App

## 1. Project Overview

Build a lightweight Android application that allows users to discover local tiffin kitchens, explore weekly menus, and subscribe to meal services.

The application is a self-contained prototype:

- All kitchen and menu data must come from a bundled local JSON asset.
- Subscription purchases are simulated.
- No backend or real payment processing is required.
- UI/UX design must follow the project's `design.md` file.

### 1.1 Source of Truth and Design Instructions

Before implementing or modifying anything, read both:

- `AGENTS.md`
- `design.md`

These files have separate responsibilities.

**design.md — Design Source of Truth**

All visual and UI/UX decisions must follow `design.md`, including:

- Color palette and themes.
- Typography and font styles.
- Screen layouts and component hierarchy.
- Spacing, padding, and dimensions.
- Buttons, cards, dialogs, and navigation elements.
- Icons and illustrations.
- Animations and transitions.
- Screen-specific visual behaviour.
- Overall visual identity and user experience.

Do not invent a separate design system or introduce styling that conflicts with `design.md`.

Do not use generic default Compose styling when the design file specifies a particular appearance.

If a design detail is not specified, use sensible defaults consistent with the existing design language.

**AGENTS.md — Functional and Technical Source of Truth**

This file defines:

- Product requirements.
- Application functionality.
- Business logic.
- Data models and local data management.
- Persistence requirements.
- Navigation behaviour.
- Subscription logic.
- Analytics.
- Technical constraints.
- Development workflow.
- Acceptance criteria.

If a requirement is not specified, inspect the existing project and choose the simplest implementation consistent with both documents.

Do not modify either documentation file unless explicitly instructed.

### 1.2 Tech Stack

- Platform: Android
- Language: Kotlin
- UI: Jetpack Compose
- Minimum SDK: API 24
- Architecture: Single-module application
- Persistence: DataStore or SharedPreferences
- Navigation: Navigation Compose or a simple Compose-compatible alternative

---

## 2. Product Goals

- Enable users to discover and compare local tiffin kitchens.
- Display cuisine, pricing, food type, ratings, and weekly menus.
- Provide a straightforward subscription journey.
- Clearly disclose introductory and subsequent charges.
- Persist application and purchase state across rotation, process death, force-stop, and relaunch.
- Track meaningful user actions through a lightweight analytics abstraction.
- Deliver a consistent user experience based on `design.md`.

---

## 3. Application User Journey

1. User opens the application.
2. User sees a list of available kitchens.
3. User selects a kitchen to view its details and weekly menu.
4. User taps Subscribe.
5. Application displays the subscription paywall.
6. User reviews the billing schedule.
7. User confirms the simulated purchase.
8. Application persists the paid state.
9. Paid users must not see the paywall again.

**Automatic paywall:** Display the paywall automatically on the third genuine app launch if the user has not completed a simulated purchase.

---

## 4. Functional Requirements

### 4.1 Kitchen List Screen

Display kitchens using locally bundled JSON data.

Each kitchen card must show:

- Kitchen name.
- Cuisine.
- Price per tiffin.
- Food availability (vegetarian, non-vegetarian, or both).
- Customer rating.

Implement the following UI states:

**Loading**

- Display a temporary loading indicator while reading the JSON asset.
- Loading must eventually resolve into another state.

**Content**

- Display the available kitchens.

**Empty**

- Display a helpful message when the dataset contains no kitchens.

**Error**

- Display a helpful error message.
- Provide a retry action.

All visual elements must follow `design.md`.

### 4.2 Kitchen Details Screen

When a user selects a kitchen, display:

- Kitchen name.
- Cuisine.
- Food type.
- Price per tiffin.
- Customer rating.
- Complete weekly menu (Monday through Sunday).
- Prominent Subscribe button.

The weekly menu must be easy to scan, with each day's meals clearly identified.

Tapping Subscribe must open the paywall unless the user has already purchased the subscription.

All visual elements and interactions must follow `design.md`.

### 4.3 Subscription Paywall

The paywall must clearly disclose:

- ₹1 is charged now.
- ₹249 will be charged 24 hours later.
- The exact future calendar date must be displayed prominently.

Calculate the future date using:

`purchaseTimestamp + 24 hours`

Requirements:

- Use the purchase time to calculate the future date.
- Display the date consistently after relaunch.
- Explain the billing terms before confirmation.
- Do not integrate a payment SDK.
- Do not perform actual payment processing.
- Confirmation simulates a successful purchase.
- Persist the simulated purchase locally.

The paywall's visual design must follow `design.md`.

### 4.4 Subscription Confirmation

When the user confirms the simulated purchase:

1. Record the purchase timestamp.
2. Mark the user as paid.
3. Persist both values.
4. Calculate the future charge date as purchase timestamp + 24 hours.
5. Ensure the same date is displayed wherever the billing date is referenced.

The ₹249 amount is a disclosed future charge only. Never actually charge the user.

---

## 5. Paywall Display Rules

The paywall has two entry conditions:

1. Automatically on the third genuine app launch.
2. Whenever the user taps Subscribe on the kitchen details screen.

Once the simulated purchase is complete:

- Persist the paid status.
- Never show the paywall automatically on subsequent launches.
- Do not show the paywall when Subscribe is tapped.
- Preserve the purchase state after rotation, force-stop, process death, and relaunch.

Avoid repeatedly presenting the automatic launch paywall after a successful purchase.

The paywall must not appear multiple times because of recomposition or configuration changes.

---

## 6. Local Data Management

All kitchen and menu data must be bundled with the application.

Asset location:

`app/src/main/assets/kitchens.json`

Create a dataset containing approximately 12 sample kitchens.

Each kitchen record should contain:

- `id`: Stable unique identifier.
- `name`: Kitchen name.
- `cuisine`: Cuisine category.
- `pricePerTiffin`: Price in INR.
- `foodType`: Vegetarian, non-vegetarian, or both.
- `rating`: Numeric customer rating.
- `weeklyMenu`: Menu entries covering Monday through Sunday.

Requirements:

- Parse the JSON asset locally.
- Do not make network requests.
- Do not retrieve kitchen data from a backend.
- Handle malformed or missing JSON gracefully.
- Keep the data model separate from the UI.
- Use stable kitchen IDs for navigation and analytics.

---

## 7. State Management and Persistence

Use DataStore or SharedPreferences for persistent application state.

At minimum, persist:

- App launch count.
- Simulated purchase completion status.
- Purchase timestamp.
- Any additional state required for consistent billing-date display.

Do not rely exclusively on in-memory variables or ViewModel state for persistent information.

### 7.1 Launch Count Behaviour

- Increment the launch count once per genuine application launch.
- Never increment it during recomposition.
- Never increment it during configuration changes or rotation.
- Trigger the automatic paywall when the count reaches three, provided the user is unpaid.
- Restore the correct state after force-stop and relaunch.

The launch-count update must happen from an appropriate application lifecycle entry point, not from a composable function.

Ensure that multiple recompositions or screen navigation do not increment the count.

### 7.2 Purchase Behaviour

When the user confirms the simulated purchase:

1. Record the purchase timestamp.
2. Mark the user as paid.
3. Persist both values.
4. Calculate the future charge date as purchase timestamp + 24 hours.
5. Ensure the same date is displayed after relaunch.

Persist purchase-related values atomically where possible.

### 7.3 State Restoration

The application must restore its correct state after:

- Screen rotation.
- Activity recreation.
- Process death.
- Force-stop.
- Application relaunch.

The persisted purchase status must always take priority over automatic paywall triggers.

---

## 8. Analytics

Create an `analytics.kt` file.

Implement:

- One analytics interface.
- One Logcat implementation of that interface.

The architecture should allow replacing Logcat with a real analytics provider later without changing UI event calls.

### 8.1 Required Events

| Event | Trigger |
|---|---|
| `app_opened` | Once when a new app launch begins |
| `kitchen_viewed` | When a kitchen detail screen opens |
| `subscribe_clicked` | When the user taps Subscribe |
| `purchase_completed` | When the simulated purchase succeeds |

Events may include non-sensitive properties such as kitchen ID and displayed price.

Never log sensitive personal information.

### 8.2 Analytics Implementation

- Keep analytics independent of UI components.
- Use a single interface for all analytics calls.
- Implement a Logcat-based analytics provider.
- Avoid duplicate events caused by recomposition.
- Track `kitchen_viewed` when a kitchen detail is opened, not on every recomposition.
- Track `purchase_completed` only after the simulated purchase is successfully persisted.

---

## 9. Navigation Requirements

Implement navigation between:

1. Kitchen List Screen.
2. Kitchen Details Screen.
3. Subscription Paywall.

Requirements:

- Selecting a kitchen opens its corresponding details.
- The selected kitchen must be identified using its stable ID.
- Subscribe opens the paywall for unpaid users.
- Subscribe must not open the paywall for paid users.
- Successful simulated purchase must update application state immediately.
- Back navigation must behave consistently.
- Automatic paywall presentation must not break normal navigation.
- Prevent duplicate navigation caused by recomposition.

Follow the navigation appearance and interaction guidelines in `design.md`.

---

## 10. Technical Constraints

- Kotlin only.
- Jetpack Compose for UI.
- Minimum Android SDK 24.
- Single application module.
- Local JSON asset for all kitchen and menu data.
- No backend integration.
- No network calls for kitchen data.
- No payment SDK.
- Persistent local storage.
- One analytics interface and one Logcat implementation in `analytics.kt`.

Follow existing project conventions and dependencies wherever possible.

Do not introduce unnecessary dependencies or replace existing working implementations without a clear reason.

---

## 11. Suggested Project Structure

```text
app/
└── src/
    └── main/
        ├── assets/
        │   └── kitchens.json
        ├── java/.../
        │   ├── data/
        │   │   ├── Kitchen.kt
        │   │   └── KitchenRepository.kt
        │   ├── analytics/
        │   │   └── analytics.kt
        │   ├── persistence/
        │   │   └── AppPreferences.kt
        │   ├── ui/
        │   │   ├── KitchenListScreen.kt
        │   │   ├── KitchenDetailScreen.kt
        │   │   └── PaywallScreen.kt
        │   └── MainActivity.kt
        └── AndroidManifest.xml
```

This is a suggested structure. Keep the application single-module and adapt package organization to the existing project.

Do not force this exact structure if the existing project already follows a suitable architecture.

---

## 12. Coding Standards

- Follow Kotlin coding conventions.
- Use Jetpack Compose best practices.
- Keep UI components modular and reusable.
- Separate data, persistence, analytics, and UI responsibilities.
- Use appropriate state management.
- Avoid unnecessary dependencies.
- Handle errors and edge cases.
- Use meaningful naming conventions.
- Avoid duplicating business logic across screens.
- Do not introduce backend or payment integrations.
- Preserve existing working functionality when modifying code.
- Keep composable functions focused and reusable.
- Avoid unnecessary recompositions.
- Use appropriate coroutine dispatchers for background operations.
- Avoid blocking the main thread while loading or parsing JSON.
- Keep business logic out of composable UI functions.

### 12.1 Design Standards

- Always read `design.md` before creating or modifying UI.
- Follow the exact design language defined in `design.md`.
- Reuse common visual components where possible.
- Maintain visual consistency across all screens.
- Do not introduce arbitrary colors, fonts, spacing, or shapes.
- Ensure the application remains usable on different Android screen sizes.
- Handle system bars, status bar, and navigation bar appropriately.
- Maintain accessibility through appropriate contrast, touch targets, and content descriptions.
- Do not sacrifice functional correctness for visual effects.

---

## 13. Development Workflow

### 13.1 Before Implementation

1. Inspect the existing project structure.
2. Read `AGENTS.md`.
3. Read `design.md` completely.
4. Understand existing dependencies and Gradle configuration.
5. Identify existing architecture and reusable components.
6. Identify existing UI components and design resources.
7. Avoid replacing existing project configuration unnecessarily.

### 13.2 During Implementation

Implement in the following order:

1. Implement the local data model and repository.
2. Create `kitchens.json` with approximately 12 sample kitchens.
3. Implement persistent application preferences.
4. Implement the analytics abstraction.
5. Implement the kitchen list screen.
6. Implement the kitchen details screen.
7. Implement the subscription paywall.
8. Implement navigation.
9. Implement launch-based paywall behaviour.
10. Implement simulated purchase persistence.
11. Verify all UI against `design.md`.

### 13.3 After Implementation

- Build the application.
- Fix compilation errors.
- Verify navigation.
- Verify all persistent states.
- Verify automatic paywall behaviour.
- Verify simulated purchase behaviour.
- Verify billing-date calculation.
- Verify analytics events.
- Test loading, empty, and error states.
- Test on Android API 24 or later.
- Check UI consistency against `design.md`.

Do not consider implementation complete merely because the project compiles.

---

## 14. Testing Requirements

Verify the following scenarios:

### Kitchen Data

- Valid JSON loads successfully.
- Approximately 12 kitchens are displayed.
- Missing JSON produces a helpful error.
- Malformed JSON produces a helpful error.
- Empty kitchen data displays an empty state.
- Retry works correctly after an error.

### Navigation

- Kitchen list opens correctly.
- Selecting a kitchen opens the correct details.
- All seven menu days are displayed.
- Subscribe opens the paywall for unpaid users.
- Back navigation works correctly.

### Launch Behaviour

- First launch does not automatically display the paywall.
- Second launch does not automatically display the paywall.
- Third launch displays the paywall for an unpaid user.
- Rotation does not increment launch count.
- Recomposition does not increment launch count.
- Subsequent launches do not show the automatic paywall after purchase.

### Purchase Behaviour

- Paywall displays ₹1 charged now.
- Paywall displays ₹249 charged 24 hours later.
- The future date is calculated using purchase timestamp + 24 hours.
- Purchase confirmation records the correct timestamp.
- Purchase status is persisted.
- Relaunch preserves the paid status.
- Paid users cannot access the paywall through Subscribe.
- No actual payment is processed.

### Analytics

- `app_opened` is recorded once per genuine launch.
- `kitchen_viewed` is recorded when kitchen details open.
- `subscribe_clicked` is recorded when Subscribe is tapped.
- `purchase_completed` is recorded after successful simulated purchase.

### UI and Design

- All screens follow `design.md`.
- No inconsistent styling is introduced.
- UI works on different screen sizes.
- Loading, empty, and error states are visually consistent.
- Buttons and interactive elements work correctly.

---

## 15. Acceptance Criteria

The application is complete when:

- [ ] App builds and runs on Android API 24 or later.
- [ ] Both `AGENTS.md` and `design.md` are read before implementation.
- [ ] Kitchen data loads from the bundled JSON asset.
- [ ] Approximately 12 kitchens are available.
- [ ] Kitchen list displays name, cuisine, price, food type, and rating.
- [ ] Loading, empty, and error states work correctly.
- [ ] Selecting a kitchen opens its details.
- [ ] Details screen displays a complete Monday-to-Sunday menu.
- [ ] Subscribe opens the paywall.
- [ ] Paywall clearly states ₹1 charged now.
- [ ] Paywall clearly states ₹249 charged 24 hours later.
- [ ] Correct future calendar date is displayed.
- [ ] Automatic paywall appears on the third genuine launch for unpaid users.
- [ ] Simulated purchase is persisted.
- [ ] Paid users never see the paywall again.
- [ ] Launch and purchase state survive rotation, force-stop, process death, and relaunch.
- [ ] `analytics.kt` contains one analytics interface and one Logcat implementation.
- [ ] All four required analytics events are tracked.
- [ ] No backend integration is introduced.
- [ ] No real payment processing is introduced.
- [ ] All UI follows `design.md`.
- [ ] Existing working functionality is preserved.

---

## 16. Important Instructions for Codex

Treat these documents as the source of truth:

- `design.md` controls all UI/UX and visual design decisions.
- `AGENTS.md` controls product requirements, functionality, architecture, business logic, and technical implementation.

Before starting any implementation, inspect the existing project and read both documents.

Do not:

- Ignore `design.md` when implementing UI.
- Invent a new visual design system.
- Add a backend.
- Integrate real payment processing.
- Replace local JSON with remote APIs.
- Ignore persistence requirements.
- Increment launch count during recomposition.
- Display the paywall to an already-paid user.
- Leave incomplete screens or placeholder functionality.
- Introduce unnecessary dependencies.
- Replace existing project configuration without justification.
- Remove existing working features.

If a requirement is ambiguous, inspect the existing project first and choose the simplest implementation consistent with these requirements.

If a visual requirement is ambiguous, follow the existing design language in `design.md`.

If a technical requirement is ambiguous, follow the existing project conventions and prioritize maintainability.

**Final priority:**

1. Functional correctness.
2. Reliable persistence and state management.
3. Consistent UI/UX based on `design.md`.
4. Maintainable and modular Kotlin implementation.
5. Successful build and verified application behaviour.

Do not mark the project complete until the acceptance criteria have been verified.