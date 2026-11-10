# Voice SIP Assistant

[中文](README.md) | English

Manage the Android voice gateway and Shizuku, with optional root-based monitoring and recovery. This is not a SIP server.

[Download APK](https://github.com/1Fann/voice-sip-assistant/releases/latest) · [Release notes](https://github.com/1Fann/voice-sip-assistant/releases)

## Features

- Install, update and open Voice SIP Agent.
- Prepare official Shizuku and check call/audio access.
- View gateway status, reconnect SIP and manage boot preparation.
- Support the companion custom gateway without replacing it with upstream APKs.
- Optional monitoring and recovery respecting manual stop and call protection.

## Getting started

Root deployment and independent monitoring require authorized root access and BusyBox. Grant the gateway call, microphone and Shizuku permissions separately.

Download the release, follow the installation guide, grant the requested permissions and configure your own account locally. Keep the same signing identity when upgrading to retain app data.

## Documentation

- [New-phone setup](NEW-PHONE-SETUP.md)
- [Boot recovery](BOOT-RECOVERY.md)
- [Validation](VALIDATION.md)

## License

The assistant is [MIT-licensed](LICENSE); Voice SIP Agent, Shizuku and other components retain their licenses.
