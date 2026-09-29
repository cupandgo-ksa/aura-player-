# শুধু Android ফোন দিয়ে AURA Player APK বানানো

1. https://appdevforall.org/code-on-the-go/ থেকে নিজের ফোনের জন্য Code on the Go ইনস্টল করো। প্রথমবারের সেটআপ শেষ করো। বিল্ড টুল বসাতে ফোনে কয়েক GB খালি জায়গা লাগবে।
2. AURA-Player-Android-Source.zip ফোনের Files অ্যাপে Extract করো। Extract করার পরে `AURA-Player` ফোল্ডারের ভেতর `settings.gradle`, `build.gradle` এবং `app` ফোল্ডার দেখা যাবে।
3. Code on the Go-তে বিদ্যমান প্রজেক্ট খোলার অপশন থেকে এই `AURA-Player` ফোল্ডারটি নির্বাচন করো। `app` ফোল্ডারটি আলাদা করে নির্বাচন করবে না।
4. Gradle sync শেষ হতে দাও। প্রথম বিল্ডে প্লাগইন বা SDK ডাউনলোড করতে ইন্টারনেট লাগতে পারে। প্লেয়ার অ্যাপটি ইনস্টল হওয়ার পর লোকাল ফাইল নিয়ে অফলাইনে চলবে।
5. Build মেনু থেকে `:app:assembleDebug` অথবা Build APK চালাও। বিল্ড সফল হলে `app/build/outputs/apk/debug/app-debug.apk` ফাইলটি খুঁজে ইনস্টল করো। IDE যদি সরাসরি Install দেখায়, সেটিও ব্যবহার করতে পারো।
6. AURA Player খুলে `Add local files` চাপো এবং ফোনে সেভ করা অডিও বা ভিডিও নির্বাচন করো।

এই সোর্স প্রজেক্টটি Android SDK-তে কম্পাইল করে পরীক্ষা করা হয়নি। বিল্ডে কোনো লাল error এলে Build Output-এর প্রথম error-এর স্ক্রিনশট পাঠাও, যাতে নির্দিষ্ট সমস্যাটি ঠিক করা যায়। Code on the Go অ্যাপ বানানোর টুল; তৈরি হওয়া AURA Player নিজে কোনো ইন্টারনেট অনুমতি চায় না।
