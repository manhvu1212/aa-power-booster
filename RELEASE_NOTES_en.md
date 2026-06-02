# Release Notes - AA Power Booster v2.0.4

## 1. Short version (for Google Play Console - under 500 characters)
```text
AA Power Booster v2.0.4 release (io.github.manhvu1212.aapowerbooster):
- The entire interface is now in English.
- Shows a confirmation message when the device responds after each mode/level change (both phone and Android Auto).
- Cleaner Normal mode (removed the "stock" label).
```

---

## 2. Detailed version (Changelog & technical notes)

### Version 2.0.4:
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
