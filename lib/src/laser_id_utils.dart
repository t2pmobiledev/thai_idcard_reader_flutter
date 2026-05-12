/// Utility functions for Laser ID processing.
///
/// Laser ID is the laser-printed code on the back of Thai national ID cards,
/// typically in the format of 2 letters followed by 10 digits (e.g. `JT1234567890`).
/// Raw data read from the card chip may contain trailing null bytes (`\u0000`)
/// and/or spaces as padding, which this utility handles.

/// Cleans a raw Laser ID string by removing trailing null bytes and spaces.
///
/// - Strips trailing `\u0000` (null bytes) and space characters from [input].
/// - Returns `null` if the result is an empty string.
/// - Does NOT change case or transform the content in any other way.
///
/// Examples:
/// ```dart
/// cleanLaserID('JT1234567890    ')  // → 'JT1234567890'
/// cleanLaserID('JT1234567890\u0000\u0000') // → 'JT1234567890'
/// cleanLaserID('   ')              // → null
/// cleanLaserID('\u0000\u0000')     // → null
/// cleanLaserID('')                 // → null
/// ```
///
/// Requirements: 1.3, 4.1, 4.2, 4.3
String? cleanLaserID(String input) {
  final trimmed = input.replaceAll(RegExp(r'[\u0000 ]+$'), '');
  if (trimmed.isEmpty) return null;
  return trimmed;
}
