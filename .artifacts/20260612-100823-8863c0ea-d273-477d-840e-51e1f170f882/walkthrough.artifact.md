# Walkthrough - Driver Daily Fee 24h Countdown Timer

I have implemented a 24-hour countdown timer for the driver daily service fee. This ensures that once a payment is approved (either automatically in test mode or manually by an admin), the driver has exactly 24 hours of active service before needing to pay again.

## Key Changes

### 1. Backend & Database
- **New Column**: Added `daily_fee_expires_at` (TIMESTAMP) to the `drivers` table.
- **Expiration Logic**: Updated the payment approval process to set the expiration exactly 24 hours from the moment of approval using `CURRENT_TIMESTAMP + interval '24 hours'`.
- **Status Check**: The `isDailyFeePaid` check now considers both the legacy `daily_fee_paid_at` date and the new precise `daily_fee_expires_at` timestamp.

### 2. Driver App
- **Real-time Countdown**: Implemented a countdown timer in `DriverMapViewModel` that parses the expiration timestamp and calculates remaining seconds every second.
- **Color-Coded UI**: Added a `DailyFeeCountdown` component to the map screen:
    - **Green**: > 2 hours remaining.
    - **Orange**: < 2 hours remaining.
    - **Red**: < 1 hour remaining.
- **Automatic Offline**: If the timer hits zero while the driver is online, the app will automatically set them to offline status.

### 3. Compatibility
- **Min SDK Bump**: Increased `minSdk` of the Driver App to **26** to support modern `java.time` APIs for reliable date/time calculations.

## How to Verify
1. **Pay Daily Fee**: In the Driver App, trigger a daily fee payment.
2. **Approve Payment**:
   - In **TEST** mode, it will be approved automatically.
   - In **LIVE** mode, approve it from the Admin Dashboard -> Daily Payments.
3. **Observe Timer**: Once approved, a green timer will appear above the "GO" button on the map.
4. **Test Expiry**: You can manually update the `daily_fee_expires_at` in the `drivers` table to a time less than 1 hour away to see the timer turn red.

```sql
-- To test red state (set expiry to 30 minutes from now)
UPDATE drivers SET daily_fee_expires_at = CURRENT_TIMESTAMP + interval '30 minutes' WHERE id = [DRIVER_ID];
```
