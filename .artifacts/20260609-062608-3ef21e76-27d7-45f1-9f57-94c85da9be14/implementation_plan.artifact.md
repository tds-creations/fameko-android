# Modern Customer UI Implementation Plan

Modernize the Fameko Customer App UI to rival industry leaders like Bolt, focusing on a frictionless, edge-to-edge map experience and gesture-driven interactions.

## Design Vision
- **Theme:** Clean, minimalistic, and high-contrast.
- **Interactions:** Bottom sheets that respond to velocity and gestures.
- **Visuals:** High-resolution 3D vehicle renders.

## Proposed Changes

### 1. Foundation & Design System
Establish core colors and typography.

#### [Color.kt](file:///C:/Users/lampt/AndroidStudioProjects/FamekoDriver/app-customer/src/main/java/com/example/famekodriver/customer/ui/theme/Color.kt)
- Redefine colors to match a premium "Bolt-like" palette.
- `BoltGreen`: `#34D186` -> `#2ECC71` (Vibrant Emerald)
- `BoltDark`: `#2D333A` -> `#1A1D1F` (Deep Charcoal)
- `FamekoBlue`: `#004E89` (Primary Brand Color)

### 2. Main Map Modernization
Remove traditional search boxes and implement an edge-to-edge experience.

#### [CustomerMapActivity.kt](file:///C:/Users/lampt/AndroidStudioProjects/FamekoDriver/app-customer/src/main/java/com/example/famekodriver/customer/CustomerMapActivity.kt)
- **Floating Search Bar:** Implement a floating `Surface` at the top with "Where to?" that expands into the full search UI.
- **Map Overlays:** Adjust padding so markers and the user location are not hidden by UI elements.
- **Mode Selection:** Move the "Ride" vs "Rental" toggle into a more subtle, high-end floating chip group.

### 3. Service Selection Sheet
Redesign the vehicle selection list with high-quality assets.

#### [CustomerMapActivity.kt](file:///C:/Users/lampt/AndroidStudioProjects/FamekoDriver/app-customer/src/main/java/com/example/famekodriver/customer/CustomerMapActivity.kt)
- **Vehicle Assets:** Use high-quality 3D renders from professional CDNs for:
    - **Economy:** Modern White Sedan
    - **Comfort:** Premium Black Sedan
    - **Package:** Specialized Delivery Bike/Van
- **Card Design:** Horizontal cards with larger images, bold pricing, and "Min away" badges.

### 4. Active Ride Experience
Polish the live tracking UI for "Finding Driver" and "On Trip" states.

#### [CustomerMapActivity.kt](file:///C:/Users/lampt/AndroidStudioProjects/FamekoDriver/app-customer/src/main/java/com/example/famekodriver/customer/CustomerMapActivity.kt)
- **Radar Animation:** Enhance the finding driver animation with a pulsing glow effect.
- **Driver Card:** Implement a compact, high-elevation card with circular profile picture and one-tap call/chat buttons.

## Verification Plan

### Manual Verification
- **Visual Inspection:** Verify edge-to-edge map layout on different screen sizes.
- **Gesture Testing:** Test the bottom sheet expanding/collapsing smoothly.
- **Flow Testing:** Start a ride request flow and verify the visual steps from "Where to?" to "Finding Driver".
- **Asset Check:** Ensure vehicle renders load smoothly via `AsyncImage` with proper error/placeholder handling.
