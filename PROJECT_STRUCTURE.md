# KirundiFlash — Project Structure

app/src/main/java/com/kirundiflash/app/
├── auth/
│   ├── LoginActivity.java
│   ├── RegisterActivity.java
│   └── AuthManager.java            // wraps FirebaseAuth + Google Sign-In logic
├── dashboard/
│   └── DashboardActivity.java
├── flashcards/
│   ├── FlashcardActivity.java
│   ├── FlashcardAdapter.java       // if using ViewPager2 for swipe-through
│   └── model/
│       └── FlashcardItem.java      // english, kirundi, type(verb/word)
├── leaderboard/
│   └── LeaderboardActivity.java
├── profile/
│   └── ProfileActivity.java
├── vip/
│   └── UpgradeActivity.java
├── data/
│   ├── CsvManager.java             // read/write the 4 CSVs in local storage
│   ├── FirestoreManager.java       // user doc + leaderboard sync
│   └── SessionTimer.java           // tracks time-in-app for XP
├── model/
│   └── UserModel.java              // id, username, email, isVIP, trialStartDate, xpSeconds
└── MainActivity.java               // splash/router: checks auth state → Login or Dashboard

app/src/main/res/
├── layout/
│   ├── activity_login.xml
│   ├── activity_register.xml
│   ├── activity_dashboard.xml
│   ├── activity_flashcard.xml
│   ├── activity_leaderboard.xml
│   ├── activity_profile.xml
│   └── activity_upgrade.xml
├── values/
│   ├── colors.xml
│   ├── strings.xml
│   └── themes.xml
└── drawable/                       // icons, gradients, button backgrounds

app/src/main/assets/
├── seed_verbs.csv                  // bundled starter content (English,Kirundi)
└── seed_words.csv

# Local CSV files (created at runtime in app's filesDir, NOT assets):
- known_verbs.csv
- unknown_verbs.csv
- known_words.csv
- unknown_words.csv
(On first launch, these are populated by splitting seed_verbs.csv / seed_words.csv
 entirely into the "unknown" files — nothing is known yet.)
