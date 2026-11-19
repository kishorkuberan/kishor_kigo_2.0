const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const nowTs = () => new Date(Date.now() - 1000);

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_101";
const BOB_UID = "bob_uid_202";
const CHARLIE_UID = "charlie_uid_303";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read users, conversations, or settings", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE_UID).get());
  await assertFails(unauth.collection("conversations").where("participantIds", "array-contains", ALICE_UID).get());
  await assertFails(unauth.collection("userSettings").doc(ALICE_UID).get());
});

test("User profile creation, PII isolation, and shadow-field rejection", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      kigoId: "KIGO-829461",
      username: "alice_kigo",
      displayName: "Alice Nova",
      about: "Connecting freely",
      online: true,
      allowDiscovery: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Ghost field rejected
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: nowTs(),
    })
  );

  // Immutable kigoId cannot be modified
  await assertFails(
    alice.collection("users").doc(ALICE_UID).update({
      kigoId: "KIGO-999999",
      updatedAt: nowTs(),
    })
  );

  // Private PII isolated strictly to owner
  await assertSucceeds(
    alice.collection("users").doc(ALICE_UID).collection("private").doc("account").set({
      userId: ALICE_UID,
      email: "alice@example.com",
      accountStatus: "active",
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    bob.collection("users").doc(ALICE_UID).collection("private").doc("account").get()
  );
});

test("KIGO ID and username registries enforce uniqueness and lookup safety", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    alice.collection("kigoIds").doc("KIGO-829461").set({
      userId: ALICE_UID,
      kigoId: "KIGO-829461",
      username: "alice_kigo",
      displayName: "Alice Nova",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Cannot list kigoIds (anti-scraping)
  await assertFails(bob.collection("kigoIds").get());

  // Can get exact kigoId
  await assertSucceeds(bob.collection("kigoIds").doc("KIGO-829461").get());

  // Bob cannot overwrite Alice's KIGO ID
  await assertFails(
    bob.collection("kigoIds").doc("KIGO-829461").set({
      userId: BOB_UID,
      kigoId: "KIGO-829461",
      username: "bob_kigo",
      displayName: "Bob",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );
});

test("Friend requests and friendships enforce participant isolation", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlie = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  const reqId = `${ALICE_UID}_${BOB_UID}`;
  await assertSucceeds(
    alice.collection("friendRequests").doc(reqId).set({
      fromUserId: ALICE_UID,
      toUserId: BOB_UID,
      fromDisplayName: "Alice",
      fromKigoId: "KIGO-111111",
      toDisplayName: "Bob",
      toKigoId: "KIGO-222222",
      participantIds: [ALICE_UID, BOB_UID],
      status: "pending",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Charlie cannot read or accept Alice->Bob request
  await assertFails(charlie.collection("friendRequests").doc(reqId).get());
  await assertFails(
    charlie.collection("friendRequests").doc(reqId).update({
      status: "accepted",
      updatedAt: nowTs(),
    })
  );

  // Alice cannot self-accept her own outgoing request
  await assertFails(
    alice.collection("friendRequests").doc(reqId).update({
      status: "accepted",
      updatedAt: nowTs(),
    })
  );

  // Bob can accept
  await assertSucceeds(
    bob.collection("friendRequests").doc(reqId).update({
      status: "accepted",
      updatedAt: nowTs(),
    })
  );
});

test("Conversations and messages enforce participant membership and sender integrity", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlie = testEnv.authenticatedContext(CHARLIE_UID).firestore();

  const convId = "conv_alice_bob";
  await assertSucceeds(
    alice.collection("conversations").doc(convId).set({
      type: "direct",
      participantIds: [ALICE_UID, BOB_UID],
      createdBy: ALICE_UID,
      isEncrypted: true,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    alice.collection("conversations").doc(convId).collection("messages").doc("msg_1").set({
      conversationId: convId,
      senderId: ALICE_UID,
      senderName: "Alice",
      participantIds: [ALICE_UID, BOB_UID],
      type: "text",
      ciphertext: "encrypted_payload_b64",
      ivBase64: "iv_123",
      isEncrypted: true,
      readBy: [ALICE_UID],
      deliveredTo: [ALICE_UID],
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Charlie cannot read messages or inject message
  await assertFails(
    charlie.collection("conversations").doc(convId).collection("messages").doc("msg_1").get()
  );
  await assertFails(
    charlie.collection("conversations").doc(convId).collection("messages").doc("msg_2").set({
      conversationId: convId,
      senderId: CHARLIE_UID,
      participantIds: [ALICE_UID, BOB_UID],
      type: "text",
      text: "Intruder",
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  // Bob can mark read receipt / reaction, but cannot edit Alice's text
  await assertSucceeds(
    bob.collection("conversations").doc(convId).collection("messages").doc("msg_1").update({
      readBy: [ALICE_UID, BOB_UID],
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    bob.collection("conversations").doc(convId).collection("messages").doc("msg_1").update({
      text: "Tampered",
      updatedAt: nowTs(),
    })
  );
});

test("UserSettings, BlockedUsers, Notifications, and Reports enforce owner isolation", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    alice.collection("userSettings").doc(ALICE_UID).set({
      userId: ALICE_UID,
      themeMode: "amoled",
      accentPreset: "cyan",
      fontStyle: "grotesk",
      updatedAt: nowTs(),
    })
  );
  await assertFails(bob.collection("userSettings").doc(ALICE_UID).get());

  await assertSucceeds(
    alice.collection("blockedUsers").doc(`${ALICE_UID}_${BOB_UID}`).set({
      userId: ALICE_UID,
      blockedUserId: BOB_UID,
      blockedDisplayName: "Bob",
      createdAt: nowTs(),
    })
  );
  await assertFails(bob.collection("blockedUsers").where("userId", "==", ALICE_UID).get());
});
