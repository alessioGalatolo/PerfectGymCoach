# Elevate Fitness

<div align="center">

![Elevate Fitness Banner](https://github.com/user-attachments/assets/030e1427-02f9-483d-b9a1-d1e42da0b265)

**An open-source Material 3 Expressive gym workout tracker with Wear OS support**

*No paywalls. No subscriptions. Just a great workout experience.*

[![Download on Google Play](https://img.shields.io/badge/Google_Play-Download-green?style=for-the-badge&logo=google-play)](https://play.google.com/store/apps/details?id=agdesigns.elevatefitness)
[![GitHub Stars](https://img.shields.io/github/stars/alessioGalatolo/PerfectGymCoach?style=for-the-badge)](https://github.com/alessioGalatolo/PerfectGymCoach/stargazers)
[![GitHub Issues](https://img.shields.io/github/issues/alessioGalatolo/PerfectGymCoach?style=for-the-badge)](https://github.com/alessioGalatolo/PerfectGymCoach/issues)
[![License](https://img.shields.io/github/license/alessioGalatolo/PerfectGymCoach?style=for-the-badge)](LICENSE)


  <a href="https://play.google.com/store/apps/details?id=agdesigns.elevatefitness">
    <img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png" alt="Get it on Google Play" height="80">
  </a>

[Features](#features) • [Screenshots](#screenshots) • [Tech Stack](#tech-stack) • [Getting Started](#getting-started) • [Contributing](#contributing)

</div>

---

Elevate Fitness (previously PerfectGymCoach) was born from the frustration of using badly designed fitness apps with terrible UX and an infinite number of paywalls. This is a completely free, open-source alternative that puts user experience first.

Built with the latest Android technologies including Jetpack Compose and Material 3 Expressive design, Elevate Fitness offers a modern, colorful, and delightful workout tracking experience.

## Features

- **Material 3 Expressive** - Beautiful, colorful UI with smooth animations and physics-based interactions
- **Wear OS Companion** - Track workouts directly from your smartwatch
- **Custom Workout Plans** - Create and manage personalized workout routines
- **Progress Tracking** - Monitor your fitness journey over time
- **Intuitive UX** - Designed by someone frustrated with bad fitness app UX
- **Privacy First** - No tracking, no data collection, no analytics
- **Completely Free** - No ads, no subscriptions, no paywalls
- **Dynamic Theming** - Material You support with dynamic colors

## Screenshots
| Material 3 Expressive | Wear OS |
|---|---|
| ![Expressive Update](https://github.com/user-attachments/assets/030e1427-02f9-483d-b9a1-d1e42da0b265) | ![Wear OS Demo](https://github.com/user-attachments/assets/4bb5351a-80aa-4dff-9f36-56663540078f)
 |

## Tech Stack

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Architecture:** MVI (Model-ViewModel-Intent)
- **Navigation:** [Compose Destinations](https://github.com/raamcosta/compose-destinations)
- **Design System:** Material 3 with Expressive elements
- **Wearable:** Wear OS integration
- **Graphs:** [Vico](https://github.com/patrykandpatrick/vico) and [Composable-Graphs](https://github.com/jaikeerthick/Composable-Graphs)

## Getting Started

### Prerequisites

- Android Studio Otter or later
- JDK 19+
- Android SDK 36+

### Installation

1. Clone the repository
```bash
git clone https://github.com/alessioGalatolo/PerfectGymCoach.git
cd PerfectGymCoach
```

2. Open the project in Android Studio

3. Sync Gradle files

4. Run the app on your device or emulator

### Building

```bash
./gradlew assembleDebug
```

For release builds:
```bash
./gradlew assembleRelease
```

## Roadmap

### Completed
- [x] Material 3 Expressive design implementation
- [x] Wear OS companion app
- [x] Progress (and ongoing) notifications (android 16+) 

###  In Progress
- [ ] Gemini Nano/On-Device AI Integration: On-device AI for intelligent coaching (WIP [here](https://github.com/alessioGalatolo/PerfectGymCoach/tree/ondevice-ai))
- [ ] Expressive typography

### Planned
- [ ] Additional animations and transitions

## Contributing

Any contribution is welcome.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

Distributed under the GLPv3 License. This means that you're free to use, modify, and distribute this app, but:
- No warranty/liability
- Keep it free and open source
- Disclose source and changes made
- Keep the license

See the [`LICENSE`](LICENSE) file for the full legal text.

## Acknowledgments
I do not own any of the images used in this app. They are copyright free and were collected mostly through pexels and unsplash. Many thanks to all the artist that made their images freely available: Lukas, Alesia Kozik, Tima Miroshnichenko, Bruno Bueno, Cottonbro Studio, Andrea Piacquadio, Li Sun, Gustavo Fring, Ketut Subiyanto, Ivan Samkov, Mart Production, Jonathan Borba, Max Vakhtbovych, Anete Lusina, Monstera, Andres Ayrton, Pixabay, Daniel Apodaca, Sinitta Leunen, Leon Ardho, Anastasia Shuraeva, Ruslan Khmelevsky, Barbara Olsen, Anna Shvets, Ronald Slaton, Scott Webb.

Some of the features/design elements were inspired by [Progression](https://play.google.com/store/apps/details?id=workout.progression.lite) (my favourite workout app by far, until the big subscription wall was introduced) and [GymRun](https://play.google.com/store/apps/details?id=com.imperon.android.gymapp).

Privacy policy was inspired by [WrichikBasu/ShakeAlarmClock](https://github.com/WrichikBasu/ShakeAlarmClock).
