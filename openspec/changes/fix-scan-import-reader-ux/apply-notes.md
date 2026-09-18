# Apply notes — fix-scan-import-reader-ux

## 3.3 Device / emulator scan check

`adb` not available in this environment (command not found). Automated coverage instead:

- MediaStore discovery implemented in `MediaStoreBookDiscovery` and merged in `LibraryViewModel.runScan`
- Unit: `DeviceLibraryScannerTest.mergeBookFiles_dedupesByFingerprint`
- Unit: format/classify without extension (`BookFormatDetectTest`, `DefaultBookClassifierTest`, `ImportWithoutExtensionTest`)

**Manual on phone after install:** put `.fb2`/`.epub` in `Download`, tap **Сканировать**, confirm import count > 0 (or MediaStore-backed find). If still empty, use **⋮ → Импорт**.
