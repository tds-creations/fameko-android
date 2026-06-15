# Fameko Modern UI Design Vision

To rival Bolt, the Fameko Customer App needs to move from a "functional" UI to an "emotional and frictionless" UI. The focus should be on speed, legibility, and high-quality visual feedback.

## 1. Design Principles
- **Frictionless Navigation:** No more than 2 taps to start a ride search.
- **Visual Hierarchy:** Use bold typography for primary actions and soft colors for background elements.
- **Micro-interactions:** Smooth transitions between map states and bottom sheets.
- **Consistency:** Unified icon sets (Phosphor or Feather) and a strict spacing grid.

## 2. Key UI Overhauls

### A. The "Home" Map Experience
- **Edge-to-Edge Map:** Remove header bars. The map should occupy 100% of the screen.
- **Floating Search Bar:** A "Glassmorphism" effect search bar at the top with "Where to?".
- **Quick Action Chips:** Circular chips for "Home", "Work", and "Saved" destinations.
- **Live Markers:** Animated vehicle markers that move smoothly (interpolation) instead of jumping.

### B. The Selection Bottom Sheet (The "Bolt" Look)
- **Layered Sheets:** Instead of a single popup, use a gesture-driven bottom sheet with 3 states:
    1. **Collapsed:** Just the destination/category summary.
    2. **Expanded:** Full list of ride types (Economy, Comfort, etc.) with large, high-res vehicle renders.
    3. **Payment/Confirm:** Final summary with "Estimated Arrival" and "Price" clearly highlighted in a primary-color button.

### C. Branding & Color Palette
- **Primary:** Fameko Blue (`#004E89`) as the trust color.
- **Accent:** Vibrant Teal or Gold for "Premium/Rental" services.
- **Surface:** Pure White (`#FFFFFF`) with soft shadows (`Elevation 4-8dp`) for cards.
- **Typography:** 'Inter' or 'Lexend' for a modern, tech-forward feel.

## 3. Implementation Phases
1. **Phase 1: Foundation.** Implement the Design System (Colors, Typography, Reusable Card Components).
2. **Phase 2: The Map Shell.** Create the edge-to-edge layout and floating controls.
3. **Phase 3: Service Selection.** Build the gesture-based bottom sheet with high-quality vehicle renders.
4. **Phase 4: Ride Status.** Create the "Active Ride" screen with a progress stepper and live driver card.
