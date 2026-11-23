const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_divi_123";
const BOB_UID = "bob_divi_456";

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

// --- UNAUTHENTICATED DENIAL ---

test("Unauthenticated user: cannot read or write devices or remote_playlists", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("devices").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("remote_playlists")
      .get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("devices")
      .doc("02a1b2c3d4e5")
      .set({
        userId: ALICE_UID,
        deviceMac: "02:a1:b2:c3:d4:e5",
        deviceKey: "123456",
        deviceName: "Living Room TV",
        platform: "TV",
        createdAt: now,
        updatedAt: now,
      })
  );
});

// --- DEVICES COLLECTION (Kotlin-aligned payloads) ---

test("Authenticated owner: valid device registration create, partial update, list, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const deviceRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("devices")
    .doc("02a1b2c3d4e5");

  // Create device (mirrors CloudRemoteRepository.registerDevice)
  await assertSucceeds(
    deviceRef.set({
      userId: ALICE_UID,
      deviceMac: "02:a1:b2:c3:d4:e5",
      deviceKey: "482910",
      deviceName: "Android TV Box",
      platform: "TV",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update (preserves createdAt and userId)
  await assertSucceeds(
    deviceRef.update({
      deviceName: "Salon TV 4K",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // List query (mirrors CloudRemoteRepository.observeDevices)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("devices").get()
  );

  // Delete device
  await assertSucceeds(deviceRef.delete());
});

test("Device validation: rejects ghost fields, invalid platform, future timestamps, and immutable field tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 5000);
  const devicesCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("devices");

  // Shadow/ghost field (including @DocumentId 'id') must fail
  await assertFails(
    devicesCol.doc("02a1b2c3d4e5").set({
      id: "02a1b2c3d4e5",
      userId: ALICE_UID,
      deviceMac: "02:a1:b2:c3:d4:e5",
      deviceKey: "482910",
      deviceName: "TV",
      platform: "TV",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Invalid platform enum must fail
  await assertFails(
    devicesCol.doc("02a1b2c3d4e6").set({
      userId: ALICE_UID,
      deviceMac: "02:a1:b2:c3:d4:e6",
      deviceKey: "482910",
      deviceName: "TV",
      platform: "DESKTOP",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Future timestamp must fail
  await assertFails(
    devicesCol.doc("02a1b2c3d4e7").set({
      userId: ALICE_UID,
      deviceMac: "02:a1:b2:c3:d4:e7",
      deviceKey: "482910",
      deviceName: "TV",
      platform: "TV",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Setup valid doc then attempt createdAt mutation
  await assertSucceeds(
    devicesCol.doc("02a1b2c3d4e8").set({
      userId: ALICE_UID,
      deviceMac: "02:a1:b2:c3:d4:e8",
      deviceKey: "482910",
      deviceName: "TV",
      platform: "TV",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    devicesCol.doc("02a1b2c3d4e8").update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

// --- REMOTE PLAYLISTS COLLECTION (Kotlin + HTML Panel aligned payloads) ---

test("Authenticated owner: valid M3U and XTREAM remote playlists create, update, list, and delete succeed", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const col = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("remote_playlists");

  // XTREAM remote push (omits m3uUrl when null/unused)
  await assertSucceeds(
    col.doc("pl_xtream_1").set({
      userId: ALICE_UID,
      targetMac: "02:a1:b2:c3:d4:e5",
      name: "VIP Xtream",
      playlistType: "XTREAM",
      serverUrl: "http://iptv.example.com:8080",
      username: "user123",
      password: "secret",
      createdAt: created,
      updatedAt: created,
    })
  );

  // M3U remote push targeting ALL devices
  await assertSucceeds(
    col.doc("pl_m3u_1").set({
      userId: ALICE_UID,
      targetMac: "ALL",
      name: "Sports M3U",
      playlistType: "M3U",
      m3uUrl: "http://iptv.example.com/list.m3u8",
      epgUrl: "http://iptv.example.com/epg.xml",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update
  await assertSucceeds(
    col.doc("pl_m3u_1").update({
      name: "Sports M3U Updated",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // List query (mirrors CloudRemoteRepository.observeRemotePlaylists)
  await assertSucceeds(col.get());

  // Delete remote playlist
  await assertSucceeds(col.doc("pl_m3u_1").delete());
});

test("Remote playlist validation: rejects invalid playlistType, shadow fields, and cross-user access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const aliceCol = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("remote_playlists");

  // Valid doc created by Alice
  await assertSucceeds(
    aliceCol.doc("alice_pl_1").set({
      userId: ALICE_UID,
      targetMac: "ALL",
      name: "Alice Private List",
      playlistType: "M3U",
      m3uUrl: "http://example.com/a.m3u",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Invalid playlistType must fail
  await assertFails(
    aliceCol.doc("bad_type").set({
      userId: ALICE_UID,
      targetMac: "ALL",
      name: "Bad Type",
      playlistType: "TORRENT",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Shadow field must fail
  await assertFails(
    aliceCol.doc("ghost_field").set({
      userId: ALICE_UID,
      targetMac: "ALL",
      name: "Ghost",
      playlistType: "M3U",
      isAdmin: true,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Cross-user isolation: Bob cannot read, list, write, or delete Alice's playlists or devices
  const bobViewOfAlice = bobDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("remote_playlists");
  await assertFails(bobViewOfAlice.doc("alice_pl_1").get());
  await assertFails(bobViewOfAlice.get());
  await assertFails(
    bobViewOfAlice.doc("alice_pl_1").update({ name: "Hijacked" })
  );
  await assertFails(bobViewOfAlice.doc("alice_pl_1").delete());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("devices").get()
  );
});
