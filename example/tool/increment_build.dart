import 'dart:io';

void main(List<String> args) {
  final pubspec = File('pubspec.yaml');
  final pubspecContent = pubspec.readAsStringSync();

  final regex = RegExp(r'version:\s*(\d+)\.(\d+)\.(\d+)\+(\d+)');
  final match = regex.firstMatch(pubspecContent);

  if (match == null) {
    print('⚠️ No version found in pubspec.yaml');
    exit(1);
  }

  final major = int.parse(match.group(1)!);
  final minor = int.parse(match.group(2)!);
  final patch = int.parse(match.group(3)!);
  final buildNumber = int.parse(match.group(4)!);

  // Handle reset option
  if (args.contains('--reset')) {
    final newVersion = 'version: $major.$minor.$patch+1';
    pubspec.writeAsStringSync(pubspecContent.replaceFirst(regex, newVersion));
    print('✅ Reset build number: $newVersion');
    return;
  }

  const int buildThreshold = 20;
  const int patchThreshold = 20;
  const int minorThreshold = 10;

  // Auto-increment logic
  int newMajor = major;
  int newMinor = minor;
  int newPatch = patch;
  int newBuild = buildNumber + 1;

  if (newBuild >= buildThreshold) {
    newBuild = 1; // Reset build number
    newPatch += 1; // Increment patch version

    if (newPatch >= patchThreshold) {
      newPatch = 0; // Reset patch version
      newMinor += 1; // Increment minor version

      if (newMinor >= minorThreshold) {
        newMinor = 0; // Reset minor version
        newMajor += 1; // Increment major version
      }
    }
  }

  final newVersionName = '$newMajor.$newMinor.$newPatch';
  final newVersion = 'version: $newVersionName+$newBuild';
  final updatedPubspec = pubspecContent.replaceFirst(regex, newVersion);
  pubspec.writeAsStringSync(updatedPubspec);

  print('✅ Updated version: $newVersion');
  print('📊 Version breakdown: Major: $newMajor, Minor: $newMinor, Patch: $newPatch, Build: $newBuild');

  // Also update setup.iss MyAppVersion with full version (including +build)
  final setupFile = File('setup.iss');
  if (!setupFile.existsSync()) {
    print('⚠️ setup.iss not found — skipping Inno Setup update.');
    return;
  }

  var setupContent = setupFile.readAsStringSync();

  // Replace MyAppVersion
  final versionRegex = RegExp(r'#define\s+MyAppVersion\s+"[^"]*"');
  if (versionRegex.hasMatch(setupContent)) {
    setupContent = setupContent.replaceFirst(
      versionRegex,
      '#define MyAppVersion "$newVersionName+$newBuild"',
    );
    print('✅ Updated setup.iss → MyAppVersion = $newVersionName+$newBuild');
  } else {
    print('⚠️ No MyAppVersion define found in setup.iss.');
  }

  // Replace MyAppRootDir
  final rootDir = Directory.current.path.replaceAll(r'\', r'\\'); // escape backslashes for Inno Setup
  final rootRegex = RegExp(r'#define\s+MyAppRootDir\s+"[^"]*"');
  if (rootRegex.hasMatch(setupContent)) {
    setupContent = setupContent.replaceFirst(
      rootRegex,
      '#define MyAppRootDir "$rootDir"',
    );
    print('✅ Updated setup.iss → MyAppRootDir = $rootDir');
  } else {
    print('⚠️ No MyAppRootDir define found in setup.iss.');
  }

  setupFile.writeAsStringSync(setupContent);
  print('💾 setup.iss successfully updated.');
}