// Import the Firebase SDK for Google Cloud Functions.
const functions = require('firebase-functions');
// Import and initialize the Firebase Admin SDK.
const admin = require('firebase-admin');
admin.initializeApp();

// Adds a message that welcomes new users into the chat.
exports.deleteUserData = functions.firestore
                           .document('users/{docId}')
                           .onWrite((change, context) => {
                                functions.logger.log('user updated ${snap.data()}');
                            });