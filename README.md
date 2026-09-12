<div align="center">

# 💬 Chit Chat — Social Talking Application

### An Android community platform to connect, post, and engage — like Reddit, built for real-time conversation.

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://github.com/AmanKumar925/Chit-Chat-Talking-Application)
[![Language](https://img.shields.io/badge/Language-Java-007396?style=for-the-badge&logo=java&logoColor=white)](https://github.com/AmanKumar925/Chit-Chat-Talking-Application)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![AWS](https://img.shields.io/badge/Storage-Amazon%20S3-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white)](https://aws.amazon.com/s3/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

</div>

---

## 📖 Overview

**Chit Chat** is a native Android community application that lets users create posts, comment, like, and interact with people across an open feed — similar in spirit to Reddit. It combines **social feed interactions** with **secure authentication and cloud infrastructure**, built entirely in **Java** using **Android Studio**.

The app is designed around a scalable Firebase backend paired with Amazon S3 for media storage, giving it a production-style architecture rather than a toy demo.

---

## ✨ Key Features

- 🔐 **Secure Authentication** — Firebase Authentication for user sign-up/login
- 📝 **Create & Share Posts** — Users can publish text/media posts to a public community feed
- 💬 **Comment System** — Threaded commenting on any post
- ❤️ **Like / Engagement System** — Real-time like counts powered by Firebase Realtime Database
- 🌐 **Open Community Feed** — Discover and interact with posts from any user, not just contacts
- ☁️ **Cloud Media Storage** — Images/media uploaded and served via Amazon S3
- 🔄 **Real-Time Sync** — Firestore + Realtime Database keep feeds, likes, and comments updated live across devices

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java |
| **IDE / Build** | Android Studio, Gradle |
| **Authentication** | Firebase Authentication |
| **Database** | Cloud Firestore, Firebase Realtime Database |
| **Media Storage** | Amazon S3 |
| **Architecture** | Android SDK (Activities/Fragments, RecyclerView-based feeds) |

---

## 📱 App Preview

<div align="center">

| Home Feed | Post Details | Create Post |
|:---:|:---:|:---:|
| ![Home Feed](screenshots/5.jpeg) | ![Post Details](screenshots/7.jpeg) | ![Create Post](screenshots/9.jpeg) |

| Comments | Login / Auth |
|:---:|:---:|
| ![Comments](screenshots/8.jpeg) | ![Login](screenshots/1.jpeg) |

</div>

> 📌 Screenshots live in a `/screenshots` folder in the repo root. Rename/update filenames and captions above to match your actual files.

---

## 🏗️ Architecture Highlights

- **Auth Layer:** Firebase Authentication handles secure sign-up/login and session management.
- **Data Layer:** Cloud Firestore stores structured post/comment data; Firebase Realtime Database drives live updates (likes, presence).
- **Media Layer:** User-uploaded images are stored on Amazon S3 and referenced via URL in Firestore documents — keeping the database lightweight and media delivery fast.
- **Presentation Layer:** Native Android UI built with Java, using RecyclerViews for scrollable feeds and comment threads.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest stable)
- JDK 8+
- A Firebase project (Authentication, Firestore, Realtime Database enabled)
- An AWS account with an S3 bucket configured for media storage

### Installation

```bash
# Clone the repository
git clone https://github.com/AmanKumar925/Chit-Chat-Talking-Application.git

# Open in Android Studio
# File > Open > select the cloned folder

# Add your own google-services.json (Firebase config) to /app

# Configure your AWS S3 credentials/bucket in the relevant config file

# Build and run on an emulator or physical device
```

---

## 🎯 What This Project Demonstrates

- Designing and implementing a **multi-service cloud architecture** (Firebase + AWS) on a single mobile app
- Building **real-time, socially-interactive features** (feeds, likes, comments) with live data sync
- Handling **authentication and secure user sessions** on Android
- Structuring a **scalable Android codebase** in Java following clean component separation

---

## 📬 Contact

**Aman Kumar**
📧 Feel free to reach out via [GitHub](https://github.com/AmanKumar925)

---

<div align="center">

⭐ If you found this project interesting, consider giving it a star!

</div>
