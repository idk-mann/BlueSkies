# 🌤️ BlueSkies

BlueSkies is a modern Android weather app that delivers real-time forecasts and personalized notifications. Users can set their preferences once 
and receive timely weather updates all year round without needing to constantly check the app.

It is built with **Kotlin**, **MVVM architecture**, and **Tomorrow.io API**.  

---

## ✨ Features

- Real-time weather data powered by **Tomorrow.io API**
- Forecast range: **1 to 7 days**
- Up to **10 personalized weather notifications** per day with user selected time
- **Map-based location picker** & city autocomplete (offline-capable with Room)
- **Dynamic theming**: sunrise, sunset, night, and day backgrounds based on the selected location
- **Dark/Light mode** support with Material 3  

---

## ⚙️ Tech Stack

- **Kotlin** + **Coroutines**  
- **Room Database** (city autocomplete, offline support)  
- **WorkManager** (scheduled notifications)  
- **RecyclerView** (weather card and etc)  
- **MVVM Architecture** (ViewModel, LiveData, Repository pattern)  
- **Tomorrow.io API** (real-time weather data)

## 🚧 Work in Progress

  Planned updates & improvements:
    Offline weather caching for network loss
    More flexible notifications (weekly & recurring)
      -Allow users to select specific locations for each personalized weather alert
    Add dynamic weather background (rain, snowing, etc.)
    Add location UI improvements (better layout to improve user-friendliness)
    Add unit tests for repositories & ViewModels
    Better and a more consistent app theming

## 🌧️ Vision: Enhanced Localized Rain Prediction for Tropical Regions

In tropical countries like Malaysia, rain is often highly localized, sometimes affecting only a single neighborhood or area. 
Traditional weather models struggle to predict this kind of rainfall due to their broader scale and limited ground-level resolution.
To address this, my vision is to improve localized rain prediction by leveraging:
    A network of micro weather stations, built using affordable sensors like barometers, wind vanes, rain sensor, temperature/humidity sensors and etc.
    Along with AI-powered cameras trained to classify cloud formations associated with imminent rain.
    Cameras also estimate cloud movement direction and speed, allowing predictions for surrounding areas as the weather evolves.
    Edge AI + Cloud Integration, where local devices process data in real time and sync with a central prediction engine.
    Machine learning models specifically trained on tropical weather behavior, using hyperlocal data patterns combined with multi-level sources 
    such as satellite data from Google Earth Engine.
This system can be scaled and adapted for cities and rural areas alike. Bringing real-time and accurete street-level weather to users such as event hosting,
agriculture, and disaster management platforms.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug | 2024.2.1 or newer  
- Minimum SDK: 24  
- Target SDK: 35  

### Setup
1. Clone the repo:
    ```bash
    git clone https://github.com/yourusername/blueskies.git
    cd blueskies

2. Add your Tomorrow.io API Key in local.properties:
    TOMORROW_API_KEY=your_api_key_here

3. Expose it in app/build.gradle:
    buildConfigField "String", "TOMORROW_API_KEY", "\"${project.properties["TOMORROW_API_KEY"]}\""
