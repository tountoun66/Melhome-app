🏠 Melhome Bridge : Connectez vos clims Mitsubishi à Google Home
Ce projet permet de créer un pont ("Bridge") personnel entre vos climatiseurs Mitsubishi (MELCloud) et Google Home, en contournant les limitations officielles.

Couplé à l'application Android Melhome, ce serveur vous permet de contrôler l'allumage, la température, les modes et les vitesses de ventilation directement à la voix avec Google Assistant ou depuis l'application Google Home.

📂 1. Préparation de votre dépôt GitHub (Fichiers requis)
Pour que ce pont fonctionne, votre dépôt GitHub doit contenir exactement deux fichiers. Disponible ici : https://github.com/tountoun66/melhome-bridge

🚀 2. Déploiement gratuit sur Render
Pour des raisons de sécurité, vous devez faire tourner votre propre instance de ce serveur. C'est gratuit et ça prend 2 minutes :

Créez un compte gratuit sur Render.com.

Cliquez sur New puis Web Service.

Connectez votre compte GitHub et sélectionnez ce dépôt (votre Melhome Bridge).

Configurez le déploiement ainsi :

Build Command : npm install

Start Command : npm start

Instance Type : Free (Gratuit)

Cliquez sur Create Web Service.

Patientez 1 à 2 minutes jusqu'à ce que le statut passe au vert (Live). Copiez l'URL de votre serveur (ex: [https://votre-projet.onrender.com](https://votre-projet.onrender.com)).

⚙️ 3. Configuration de Google Actions (Mode Test Privé)
Google Home exige que vous créiez un projet "Smart Home" pour lier votre serveur.

Allez sur la Google Home Developer Console et créez un nouveau projet (ex: Melhome).

Allez dans Develop > Actions et configurez :

Display Name : Melhome

Fulfillment URL : [https://votre-projet.onrender.com/fulfillment](https://votre-projet.onrender.com/fulfillment) (Remplacez par l'URL de votre Render)

Allez dans Develop > Account Linking et configurez l'OAuth 2.0 :

Linking Type : OAuth / Authorization Code

Client ID : 1234 (Valeur fictive, le pont ne s'en sert pas)

Client Secret : 1234 (Valeur fictive)

Authorization URL : [https://votre-projet.onrender.com/oauth/auth](https://votre-projet.onrender.com/oauth/auth)

Token URL : [https://votre-projet.onrender.com/oauth/token](https://votre-projet.onrender.com/oauth/token)

Cliquez sur Save. En haut à droite de la console, cliquez sur le bouton Test pour activer le projet sur votre compte Google personnel.

📱 4. Association avec l'Application Android
Installez et ouvrez l'application Android Melhome sur votre téléphone.

Cliquez sur l'icône Paramètres (⚙️).

Dans le champ "URL Render", collez l'URL de votre serveur Render (ex: [https://votre-projet.onrender.com](https://votre-projet.onrender.com)) et appuyez sur Enregistrer.

Appuyez sur Associer à Google Home. Un code unique à 4 chiffres va apparaître. Gardez-le sous les yeux.

🗣️ 5. Lier à Google Home
Ouvrez l'application Google Home sur votre smartphone.

Allez dans l'onglet Appareils, appuyez sur + Ajouter -> Fonctionne avec Google Home.

Cherchez votre service de test (il commence généralement par [test] Melhome).

Une page web s'ouvre : saisissez le code à 4 chiffres donné par votre application Android.

Validez ! Vos climatiseurs apparaissent instantanément.

Vous pouvez maintenant dire : "Ok Google, mets la clim du salon sur 21 degrés" ou "Ok Google, règle la ventilation sur Vitesse 2".

Avec ce fichier, vous leur donnez tout le nécessaire "clé en main" : les deux fichiers à créer sur leur GitHub, les commandes de build pour Render, et la méthode pour relier tout ça à votre application mobile universelle !
