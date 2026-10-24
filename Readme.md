# Mașina Mea — proiect Android (versiunea inițială)

## Funcții incluse
- Mai multe mașini, fiecare cu marcă/model, număr de înmatriculare, VIN și kilometraj.
- Documente RCA, CASCO, ITP, rovinietă sau alt document.
- Dată de expirare și cost opțional.
- Atașarea unei imagini sau a unui PDF din telefon.
- Verificare zilnică și notificări pentru documentele care expiră în 30, 7, 1 zi și în ziua expirării.
- Datele sunt stocate local pe telefon.

## Deschidere
1. Instalează Android Studio pe un calculator.
2. Dezarhivează proiectul.
3. Android Studio → Open → selectează folderul `MasinaMeaAndroid`.
4. Lasă Gradle să sincronizeze proiectul.
5. Conectează telefonul Android cu USB debugging activat sau folosește un emulator.
6. Apasă Run.

## Note importante
- Acesta este codul sursă al unei prime versiuni, nu un APK deja compilat.
- Proiectul trebuie compilat și testat în Android Studio înainte de utilizarea zilnică.
- Notificările depind de permisiunea de notificare și de optimizările de baterie ale telefonului.
- Atașamentele sunt păstrate ca URI-uri locale; pentru o versiune de producție trebuie întărită păstrarea permisiunilor și gestionarea backup-ului fișierelor.
- Datele nu sunt sincronizate între telefoane și nu sunt încă protejate prin export/backup dedicat.
- Versiunea inițială gestionează documente, nu un jurnal separat de revizii/reparații.
## Construirea APK-ului fără calculator (prin GitHub Actions)
Această arhivă include un flux de lucru pentru compilare în cloud. Ai nevoie de un cont GitHub și de a încărca **fișierele dezarhivate**, nu arhiva ZIP, într-un repository.

1. În browserul telefonului, intră pe https://github.com și creează un cont/autentifică-te.
2. Creează un repository nou, de exemplu `MasinaMeaAndroid`.
3. Încarcă fișierele și folderele proiectului în repository, păstrând structura și folderul ascuns `.github/workflows/build-apk.yml`.
4. Deschide fila **Actions** și așteaptă terminarea fluxului **Build Android APK**.
5. Deschide execuția reușită, apoi descarcă artifactul **MasinaMea-APK**.
6. Deschide arhiva descărcată pe telefon și instalează `app-debug.apk`.

Atenție: această procedură construiește un APK de testare (debug), nu o versiune publicată în Play Store. APK-ul nu a fost compilat sau testat în acest mediu.
