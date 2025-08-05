# ElevateFitness (previously PerfectGymCoach)
A gym workout app born from the frustration of using badly designed apps with terrible UX and infinite number of paywalls.

### Material 3 Expressive (WIP)

![expressive](https://github.com/user-attachments/assets/057b1aa6-71c6-4787-b76e-1bf9c2ef7955)

Done:
* Grouped cards
* Use (available) expressive elements
* Update physics
* Add some new animations

To do:
* Colourful screens?
* More expressive elements
* Typography


### Wear OS companion
![Screen_recording_20250604_225657-ezgif com-video-to-gif-converter](https://github.com/user-attachments/assets/08acae06-e532-4d8a-ba43-6ac733827260)

## Planned features
1. Improvements to plan generation
2. Integration of Gemini Nano (on-device) into plan generation (this is as soon as Google makes the sdk available) 
3. Persistent notification during workout
4. ~~Wear OS support~~

## Known issues
1. Plan generation is very bad
2. The dialog with the description of exercises in the workout is not the prettiest, it would be nice to have a different one with more info and e.g. stats
3. Missing stats tab (I don't know what to put there)
4. History scroll-down to week has wrong offset

## "Useful" elements and patterns
Despite my disclaimer below, this repo has seen some interest from the open source community so following are some elements that might be of interest to a developer looking at this app:

1. [Dependency injection with Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
   * Application definition [here](app/src/main/java/agdesigns/elevatefitness/MainApplication.kt)
   * [ViewModels](app/src/main/java/agdesigns/elevatefitness/viewmodels/) ([learn more](https://developer.android.com/training/dependency-injection/hilt-jetpack))
   * [Repository](app/src/main/java/agdesigns/elevatefitness/data/Repository.kt) pattern (Note: I use one repository for the whole app, it would probably be more appropriate -also depending on the size of the app- to have multiple ones -with a common interface- depending on the data source it needs to interface with)
1. [Jetpack compose](https://developer.android.com/develop/ui/compose): the whole application is built using compose, where the ui is defined directly in kotlin. See [ui/screens](app/src/main/java/agdesigns/elevatefitness/ui/screens) or [ui/components](app/src/main/java/agdesigns/elevatefitness/ui/components)
1. This application relies on [Compose Destinations](https://github.com/raamcosta/compose-destinations), an easy way to navigate between screens and forward useful objects.
   * See graphs definition [here](app/src/main/java/agdesigns/elevatefitness/ui/NavGraphs.kt)
   * See [RootGraph.kt](app/src/main/java/agdesigns/elevatefitness/ui/RootGraph.kt) where I define the navigation for the bottom bar.
1. Material 3 components
   * Just look around, most (all?) components should be material 3. Note: since this app was built during the very initial stages of material 3 release some components may be outdated or the might be a better way of implementing them.
   * [Dynamic theming](app/src/main/java/agdesigns/elevatefitness/ui/theme/Theme.kt)
   * [FullScreenImageCard](app/src/main/java/agdesigns/elevatefitness/ui/components/FullScreenImageCard.kt) should be the implementation of a Card with image when it goes full screen (see the result of [this animation](https://firebasestorage.googleapis.com/v0/b/design-spec/o/projects%2Fm3%2Fimages%2Fl0d2qjpe-cards-expand_3P_2.mp4?alt=media&token=f6252b7c-cb3d-4c91-9701-13b9bf9f482d)). At the time there was nothing similar in compose, now I don't know :)
1. Control media playing on device
   * [Manifest permission](app/src/main/AndroidManifest.xml): get BIND_NOTIFICATION_LISTENER_SERVICE permission (to read media notification)
   * [NotificationListener](app/src/main/java/agdesigns/elevatefitness/service/NotificationListener.kt)
   * [Permission request dialog](app/src/main/java/agdesigns/elevatefitness/ui/components/CustomDialogs.kt) see RequestNotificationAccessDialog
   * [Use retrieved media](app/src/main/java/agdesigns/elevatefitness/ui/screens/Workout.kt) see retrieveMediaJob and what follows SwipeToDismissBox.

## Disclaimer
This app is pretty much a mess, it is open source as I've had this habit for quite some time now but it probably shouldn't /(ㄒoㄒ)/~~

Feel free to contribute/make any suggestion for this project :) 

## Acknowledgments
I do not own any of the images used in this app. They are copyright free and were collected mostly through pexels and unsplash. Many thanks to all the artist that made their images freely available: Lukas, Alesia Kozik, Tima Miroshnichenko, Bruno Bueno, Cottonbro Studio, Andrea Piacquadio, Li Sun, Gustavo Fring, Ketut Subiyanto, Ivan Samkov, Mart Production, Jonathan Borba, Max Vakhtbovych, Anete Lusina, Monstera, Andres Ayrton, Pixabay, Daniel Apodaca, Sinitta Leunen, Leon Ardho, Anastasia Shuraeva, Ruslan Khmelevsky, Barbara Olsen, Anna Shvets, Ronald Slaton, Scott Webb.

Some of the features/design elements were inspired by [Progression](https://play.google.com/store/apps/details?id=workout.progression.lite) (my favourite workout app by far, until the big subscription wall was introduced) and [GymRun](https://play.google.com/store/apps/details?id=com.imperon.android.gymapp).

Privacy policy was inspired by [WrichikBasu/ShakeAlarmClock](https://github.com/WrichikBasu/ShakeAlarmClock).
