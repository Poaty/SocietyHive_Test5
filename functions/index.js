const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp }    = require("firebase-admin/app");
const { getFirestore }     = require("firebase-admin/firestore");
const { getMessaging }     = require("firebase-admin/messaging");

initializeApp();

/**
 * Fires whenever a new pin is written to pins/{pinId}.
 * Finds all members of the pin's society, collects their FCM tokens,
 * and sends a multicast push notification.
 */
exports.notifyOnNewPin = onDocumentCreated("pins/{pinId}", async (event) => {
    const pin      = event.data.data();
    const societyId = pin.societyId;
    const content   = pin.content;

    if (!societyId || !content) return;

    const db = getFirestore();

    // Resolve society name for the notification title
    const societyDoc  = await db.collection("societies").doc(societyId).get();
    const societyName = societyDoc.exists ? societyDoc.data().name : "SocietyHive";

    // Collect FCM tokens for every member of this society
    const usersSnap = await db.collection("users")
        .where("societyIds", "array-contains", societyId)
        .get();

    const tokens = [];
    usersSnap.forEach(doc => {
        const token = doc.data().fcmToken;
        if (token) tokens.push(token);
    });

    if (tokens.length === 0) {
        console.log("No FCM tokens found for society:", societyId);
        return;
    }

    const response = await getMessaging().sendEachForMulticast({
        notification: {
            title: `📌 ${societyName}`,
            body: content,
        },
        android: {
            priority: "high",
        },
        tokens,
    });

    console.log(`Sent ${response.successCount}/${tokens.length} notifications for pin in ${societyName}`);
});
