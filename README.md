# Supervisor App

**The app of [supervisor](https://github.com/frsorrentino/supervisor) for your phone, the web and your Wear OS watch: the Claude Code sessions of your computer, in your pocket.** For anyone who runs several sessions and doesn't want to sit at the desk waiting for them.

![Status: beta](https://img.shields.io/badge/status-beta-orange) ![Wear OS 4+](https://img.shields.io/badge/Wear%20OS-4%2B-3ddc84) ![Needs supervisor](https://img.shields.io/badge/needs-supervisor-8A5CF6)

![supervisor on your wrist: on the left the three steps — the supervisor plugin on the PC, the relay paired with your Firebase, the 6-digit code on the watch; on the right three real screens of the app on the demo set: the sessions, a question, a session's card.](docs/readme/card-hero.png)

## What it does

- **Who needs you comes first.** Read the question, tap an option or dictate a reply, and the session goes on.
- **Every session at a glance.** What it is doing, its next step or its outcome; closed sessions reopen from their row.
- **Write and launch.** Send a prompt to a session, or start a project from the list your PC publishes.
- **Read aloud.** ▶ next to every long text, with the screen off too.
- **Follow.** A long press buzzes you when a session finishes.
- **On the watch.** A tile with the session in focus, a complication with the quota ring, notifications you answer from the wrist.
- **Quota, night queue, timeline.** The 5-hour window and the week, what ran overnight, the day's questions and outcomes.

No watch? Phone and web work without it, and supervisor works from the terminal without this app. watchOS is not supported.

<table>
  <tr>
    <td align="center"><img src="docs/readme/sessions.png" width="220" alt="Sessions list: the session waiting for an answer first, with its question in full"><br>Sessions</td>
    <td align="center"><img src="docs/readme/question.png" width="220" alt="A question full screen, the options as wide buttons"><br>Question</td>
    <td align="center"><img src="docs/readme/quota.png" width="220" alt="Quota: 5-hour window, week, resets"><br>Quota</td>
  </tr>
</table>

## Install

**Start from [«Try it» in the supervisor README](https://github.com/frsorrentino/supervisor#try-it):** it covers the first run, the plugin and the relay. Then install the phone APK from the [releases](https://github.com/frsorrentino/supervisor-app/releases) and run `supervisor relay pair`.

| | You need |
|---|---|
| **PC** | Linux or macOS, always on and awake; `bash`, `tmux`, `python3` ≥ 3.8 with `cryptography`, `crontab`; Claude Code ≥ 2.1.263 with the supervisor plugin; the Firebase CLI for `relay setup` |
| **Firebase** | A project of your own; `supervisor relay setup` creates or picks it and sets it up |
| **Phone** | Android 13 or newer (minSdk 33) |
| **Watch** | Wear OS 4 or newer (minSdk 33) with Google Play services; tried only on a Pixel Watch 5 (Wear OS 7) |
| **Build from source** | JDK 17, Android SDK 36, `adb` |

Beta, in daily use on a Pixel Watch 5 and a Pixel phone. What changed and when: [CHANGELOG.md](CHANGELOG.md).

## Problems and ideas

Reports go in one place: the [issues of supervisor](https://github.com/frsorrentino/supervisor/issues), for this app too. Security problems go privately through [Report a vulnerability](https://github.com/frsorrentino/supervisor-app/security/advisories/new).

## How it connects

![Your PC, your Firebase, your wrist: the supervisor plugin and its relay on the PC push the state and run the commands; your Firebase Realtime Database and Cloud Messaging hold only AES-256-GCM encrypted blobs; the watch keeps the key in its Keystore, paired over X25519 with a 6-digit code.](docs/readme/card-bus.png)

There is no server of ours in between. The relay, part of the plugin, publishes the state of your sessions to **your own** Firebase project and runs the commands you send. Every document is encrypted end to end with AES-256-GCM; the key is agreed at pairing over X25519 and lives in the Keystore of the phone and of the watch. Firebase sees blobs, never your prompts or your project names.

## Build from source

Active development is on the branch [`tab-emoji/22-10`](https://github.com/frsorrentino/supervisor-app/tree/tab-emoji/22-10); its README has the full steps, including the phone's `github` and `play` flavors. In short:

```bash
export KEYSTORE_PATH=/path/to/release.jks KEYSTORE_PASS=… KEY_ALIAS=… KEY_PASS=…
./gradlew :mobile:assembleGithubRelease :wear:assembleRelease
```

Keep one signing key for good: an app signed with another key cannot be updated in place, and uninstalling it wipes the pairing. Modules: `wear`, `mobile`, `core` (contract, crypto, rules), `ui-tokens`. The contract with the PC is in [`contract/`](contract/): the JSON fixtures the app's tests read and the relay produces.
