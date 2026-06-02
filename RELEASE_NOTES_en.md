# Release Notes - AA Power Booster v2.1.1

## 1. Short version (for Google Play Console - under 500 characters)
```text
AA Power Booster v2.1.1 release (io.github.manhvu1212.aapowerbooster):
- Fixed the "Can't complete this action while driving" warning on Android Auto during rapid use.
- More stable car UI — no longer blocked when tapping quickly.
```

---

## 2. Detailed version (Changelog & technical notes)

### Version 2.1.1:
1. **Fixed Android Auto blocking while driving:** rapid taps (quickly switching mode/level/P/R) could make Android Auto show "Can't complete this action while driving" because the app pushed templates too frequently and exceeded the host's quota. Fixed by (a) coalescing redraws to at most one every 300ms, and (b) always keeping the same grid layout type (never switching to a message screen), so each redraw counts as a free "refresh" and never hits the quota.

### Since 2.1.0:
1. **P/R toggle on Android Auto:** added a 6th tile on the car screen to quickly switch between **P** (booster active) and **R** (back to the original throttle), independent of the 5 driving modes — without losing your selected mode/level.
2. **Clear icon:** "P / R" inside a rounded border; the active state's letter is **filled** while the other is just an **outline**, so a glance tells you which state you're in.
3. **Confirmation message:** when the device responds, a CarToast shows "✓ Mode P" / "✓ Mode R" — same confirmation pattern as the existing mode/level changes.

### Since 2.0.4:
1. **English interface:** all on-screen text in the app (phone + Android Auto) is now in English.
2. **Confirmation when the device responds:** after you change mode/level, when the device sends its data back, the app shows a brief message (Toast on the phone, CarToast on Android Auto) like "✓ Race · Level 5" — proof that the command actually reached the device. It only appears for a command you just sent, not for background syncs.
3. **Cleaner Normal mode:** removed the "(stock)" suffix — it now just shows "Normal".

### Since 2.0.3:
- **Android Auto:** the active mode's icon is wrapped in a circular ring; removed the "Level x" line under the icon (the level still shows in the title).

### Since 2.0.2:
- **Fixed the Android Auto icon:** adaptive icon with a black full-bleed background filling the whole circle, red wing centered — no more white ring.
- **Reordered modes:** consistent order **Race · Sport · City · Normal · Eco** on both phone and Android Auto.
- **Phone:** shows the selected mode name next to the mode picker label.

### Since 2.0.1:
- **Fixed edge-to-edge clipping:** content is no longer hidden by the status bar, notch or navigation bar.

### Since 2.0.0 (major update):
- **New phone UI:** pick modes with 5 balanced icons; auto-connect to the saved device; the scanner only appears when no device is saved.
- **New logo:** red wing on a black background.
- **Per-mode levels:** switching modes restores that mode's own level.
- **Privacy:** no data collection, no Internet permission, no ads.
