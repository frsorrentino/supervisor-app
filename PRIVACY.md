# Privacy — Supervisor for phone, Wear OS and the web

_Last updated: 13 October 2026._

Supervisor — the phone app, the Wear OS app and the web app at <https://supervisor.francescosorrentino.com> — is an
unofficial companion for Claude Code, not affiliated with Anthropic. They show and steer the Claude Code sessions running on your own computer through the
supervisor plugin. They have no account of their own, no analytics, no advertising and no server run by the author.

## Where your data goes

- **Your own Firebase project.** When you pair, the phone reads a QR code shown by your computer. It holds the
  configuration of a Firebase project that you created and own. The app talks only to that project: its Realtime
  Database and Firebase Cloud Messaging. The author never receives anything.
- **End-to-end encrypted.** Session states, questions, answers, commands, terminal text, diaries and anything you
  share with a session travel as AES-256-GCM ciphertext. The key is agreed with X25519 between your computer and your
  phone at pairing, then passed to your watch encrypted for it alone. Firebase stores only ciphertext; neither Google
  nor the author can read it.
- **Identifiers.** The Firebase SDK signs the app in anonymously and gives it a user id, an installation id and a
  messaging token. They let your computer allow your devices and wake them up. The device names (for example «Pixel
  Watch 5») are stored by your computer to list the paired devices.

## What stays on the device

- The encryption key, in the Android Keystore.
- The Firebase configuration, the name of your computer, the paired devices, and a local copy of recent states and
  diaries.
- Settings such as who rings on a question and text-to-speech.

Reading aloud uses the text-to-speech engine installed on your device. The QR code is read by the Google Play services
code scanner: the app has no camera permission and never sees the camera image.

## The web app

The web app is the same idea in a browser. Its code is a static site served by Firebase Hosting from the author's
project; like any website, the hosting keeps the usual access logs (IP address, browser, time). No session data goes
through it: the page talks only to your own Firebase project, and a Content-Security-Policy blocks every other address.

- **Pairing.** The QR code is read by the page itself from the camera, after you allow it, or pasted as text. The camera
  image never leaves the browser.
- **What the browser keeps.** The encryption key, the Firebase configuration and an anonymous Firebase sign-in, in the
  storage of that site in that browser. Clearing the site's data or removing the device on your computer revokes it.
- **Notifications.** If you turn them on, the browser gives a push subscription from its push service (Google, Apple
  or Mozilla, depending on the browser). The web app stores it, encrypted, in your Firebase project; your computer
  sends each notification encrypted for that subscription, so the push service sees only that a message is delivered.

## Demo mode

Demo mode runs on sample data inside the app or the page. Nothing leaves the device.

## Deleting your data

- «Pair again» or uninstalling removes the key and the local data from the device.
- Removing a device on your computer (a new pairing) revokes its access to your database.
- Everything else lives in your own Firebase project, which you can delete from the Firebase console at any time.

## Changes and contact

Changes to this page are versioned in the
[GitHub repository](https://github.com/frsorrentino/supervisor-app). Questions: an issue at
<https://github.com/frsorrentino/supervisor-app/issues>. The plugin on the computer has its own page:
[Supervisor privacy](https://github.com/frsorrentino/supervisor/blob/main/PRIVACY.md).
