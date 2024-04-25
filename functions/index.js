const {onDocumentDeleted, onDocumentCreated} = require("firebase-functions/v2/firestore");
const firebase_tools = require('firebase-tools');
const {setGlobalOptions} = require("firebase-functions/v2");
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const admin = require("firebase-admin");
admin.initializeApp();
setGlobalOptions({maxInstances: 5});

exports.deleteuser = onDocumentDeleted("users/{userId}", async event => {
    const snap =  event.data;
    var userId = snap.data().id;

    try {
        await firebase_tools.firestore
            .delete(`users/${userId}/user_locations`, {
                project: process.env.GCLOUD_PROJECT,
                recursive: true,
                yes: true,
                force: true
            });

        await firebase_tools.firestore
           .delete(`users/${userId}/user_sessions`, {
               project: process.env.GCLOUD_PROJECT,
               recursive: true,
               yes: true,
               force: true
           });

        await firebase_tools.firestore
             .delete(`users/${userId}/user_journeys`, {
                 project: process.env.GCLOUD_PROJECT,
                 recursive: true,
                 yes: true,
                 force: true
             });

        console.log('User collections deleted successfully.', userId);
    } catch (error) {
        console.error('Error deleting user locations:', error);
        throw new Error('Failed to delete user locations');
    }
});

exports.deleteMessages = onDocumentDeleted("space_thread/{threadId}", async event => {
    const snap =  event.data;
    var threadId = snap.data().id;

    try {
        await firebase_tools.firestore
            .delete(`space_thread/${threadId}/thread_messages`, {
                project: process.env.GCLOUD_PROJECT,
                recursive: true,
                yes: true,
                force: true
            });

       console.log('Thread messages deleted successfully.', userId);
    } catch (error) {
        console.error('Error deleting thread messages:', error);
        throw new Error('Failed to delete thread');
    }
});

exports.sendNotification = onDocumentCreated("space_threads/{threadId}/thread_messages/{messageId}", async event => {

    const snap = event.data.data();
    const message = snap.message;
    const senderId =  snap.sender_id;
    const threadId =  event.params.threadId;

    var senderSnapShot = await admin.firestore().collection('users').doc(senderId).get();
    if (!senderSnapShot.exists) {
          console.log('Sender does not exist');
          return;
    }
    const senderData = senderSnapShot.data();
    const senderName = senderData.first_name + ' ' + senderData.last_name;
    const senderProfile = senderData.profile_image;

    var documentSnapshot = await admin.firestore().collection('space_threads').doc(threadId).get();
    if (!documentSnapshot.exists) {
          console.log('Thread does not exist');
          return;
    }

    const documentData = documentSnapshot.data();

    const memberIds = documentData.member_ids.filter(memberId => memberId !== senderId);

    const membersPromises = memberIds.map(async memberId => {
        const memberSnapshot = await admin.firestore().collection('users').doc(memberId).get();
        if (!memberSnapshot.exists) {
           throw new Error(`Member with ID ${memberId} does not exist`);
        }
        return memberSnapshot.data();
    });

    const members = await Promise.all(membersPromises)
    const memberNames = members.map(member => {
        return member.first_name;
    });

    const firstTwoNames = memberNames.slice(0, 2).join(", ");
    const remainingCount = memberNames.length - 2;
    const groupName = remainingCount > 0 ? `${firstTwoNames} +${remainingCount}` : firstTwoNames;

    const filteredTokens = members.map(member => {
           return member.fcm_token;
    }).filter(token => token !== undefined);

    const isGroup = memberIds.length > 1;

    if (filteredTokens.length > 0) {
        const payload = {
             tokens: filteredTokens,
             notification: {
                   title: senderName,
                   body: message,
                },
             data : {
                   senderProfileUrl: senderProfile,
                   senderId: senderId,
                   groupName: groupName,
                   isGroup: `${isGroup}`,
                   threadId: threadId,
                   type: 'chat'
                }
        };

        admin.messaging().sendMulticast(payload).then((response) => {
                 console.log("Successfully sent message:", response);
                 return {success: true};
           }).catch((error) => {
                  console.log("Failed to send message:", error.code);
                  return {error: error.code};
           });
    }

});

exports.sendSupportRequest = onCall(async (request) => {

     const db = admin.firestore();
     var data = request.data;

     await db.collection('support_requests')
     .add({
        to: ["radhika.s@canopas.com","megh.l@canopas.com"],
        message: {
            subject: "YourSpace Support Request",
            html: `
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
                <meta name="HandheldFriendly" content="true">
                <style>
                    table {
                        width: 80%;
                        border-collapse: collapse;
                    }

                    table, th, td {
                        border: 1px solid black;
                    }

                    th {
                        font-weight: bold;
                    }

                    th, td {
                        height: 30px;
                        text-align: center;
                    }
                </style>
            </head>
            <body>
            <br>
            <table>
                <tbody>
                    <tr>
                        ${Object.entries(data).map((entry, index) => {
                            const key = entry[0];
                            const value = Array.isArray(entry[1]) ? entry[1].join("</p><p>") : entry[1]; // Handle arrays specially
                            return `<tr><td>${key}</td><td>${value}</td></tr>`;
                        }).join('')}
                    </tr>
                </tbody>
            </table>
            </body>
            </html>
            `,
        },
     }).then(() => console.log('Queued email for delivery!'));

});

