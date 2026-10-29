import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, writeBatch, getDoc, setDoc, updateDoc, collection, getDocs, Timestamp } from 'firebase/firestore';
import fs from 'node:fs';

const PROJECT_ID = 'gen-lang-client-0548701351';
const DATABASE_ID = 'ai-studio-familleplanning-5bf1e4ae-719b-4752-9367-6a8444b48be1';

async function runTests() {
  console.log('🚀 Starting Comprehensive Firebase Emulator Rules Verification...');
  console.log(`Target Database: ${DATABASE_ID}`);
  console.log(`Target Project: ${PROJECT_ID}`);

  const emulatorHost = process.env.FIRESTORE_EMULATOR_HOST || '127.0.0.1:8085';
  const [host, portStr] = emulatorHost.split(':');
  const port = parseInt(portStr, 10);

  const rules = fs.readFileSync('firestore.rules', 'utf8');

  const testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: rules,
      host: host,
      port: port
    }
  });

  let passedTests = 0;
  let totalTests = 0;

  async function test(name, fn) {
    totalTests++;
    try {
      await fn();
      console.log(`  ✅ [PASS] ${name}`);
      passedTests++;
    } catch (err) {
      console.error(`  ❌ [FAIL] ${name}:`, err.message);
      throw err;
    }
  }

  try {
    // ----------------------------------------------------
    // TEST 1: Unauthenticated user cannot create family
    // ----------------------------------------------------
    await test('1. Unauthenticated user cannot create family', async () => {
      const unauthContext = testEnv.unauthenticatedContext();
      const db = unauthContext.firestore({ databaseId: DATABASE_ID });
      const familyRef = doc(db, 'families', 'fam_unauth');

      await assertFails(
        setDoc(familyRef, {
          name: 'Famille Anonyme',
          ownerId: 'anon_uid',
          timezone: 'Europe/Paris',
          createdAt: Timestamp.now()
        })
      );
    });

    // ----------------------------------------------------
    // TEST 2: Creation du premier foyer Android (4 ecritures atomiques WriteBatch)
    // ----------------------------------------------------
    await test('2. Creation du premier foyer Android fonctionne avec les 4 ecritures atomiques', async () => {
      const aliceContext = testEnv.authenticatedContext('alice_uid', {
        email: 'alice@familleplanning.fr'
      });
      const db = aliceContext.firestore({ databaseId: DATABASE_ID });

      const batch = writeBatch(db);
      const familyRef = doc(db, 'families', 'family_alice_1');
      const accessRef = doc(db, 'families', 'family_alice_1', 'access', 'alice_uid');
      const memberRef = doc(db, 'families', 'family_alice_1', 'members', 'member_alice_1');
      const userRef = doc(db, 'users', 'alice_uid');

      const now = Timestamp.now();

      // 1. families/{familyId}
      batch.set(familyRef, {
        name: 'Famille Martin',
        ownerId: 'alice_uid',
        timezone: 'Europe/Paris',
        createdAt: now
      });

      // 2. families/{familyId}/access/{uid}
      batch.set(accessRef, {
        role: 'owner',
        joinedAt: now
      });

      // 3. families/{familyId}/members/{memberId}
      batch.set(memberRef, {
        name: 'Alice',
        userId: 'alice_uid',
        role: 'parent',
        roleLabel: 'Parent',
        color: '#FF6B6B',
        avatarEmoji: '😊'
      });

      // 4. users/{uid} (create new)
      batch.set(userRef, {
        id: 'alice_uid',
        displayName: 'Alice Martin',
        email: 'alice@familleplanning.fr',
        photoUrl: null,
        currentFamilyId: 'family_alice_1',
        familyIds: ['family_alice_1'],
        createdAt: now,
        updatedAt: now
      });

      await assertSucceeds(batch.commit());
    });

    // ----------------------------------------------------
    // TEST 3: L'utilisateur peut lire sa propre famille (allow get)
    // ----------------------------------------------------
    await test('3. L utilisateur connecte peut lire sa propre famille (allow get)', async () => {
      const aliceContext = testEnv.authenticatedContext('alice_uid');
      const db = aliceContext.firestore({ databaseId: DATABASE_ID });

      const familyDoc = await getDoc(doc(db, 'families', 'family_alice_1'));
      if (!familyDoc.exists() || familyDoc.data().name !== 'Famille Martin') {
        throw new Error('Impossible de lire sa propre famille');
      }

      const accessDoc = await getDoc(doc(db, 'families', 'family_alice_1', 'access', 'alice_uid'));
      if (!accessDoc.exists() || accessDoc.data().role !== 'owner') {
        throw new Error('Impossible de lire son propre droit d accès');
      }

      const memberDoc = await getDoc(doc(db, 'families', 'family_alice_1', 'members', 'member_alice_1'));
      if (!memberDoc.exists() || memberDoc.data().name !== 'Alice') {
        throw new Error('Impossible de lire son propre membre');
      }

      const userDoc = await getDoc(doc(db, 'users', 'alice_uid'));
      if (!userDoc.exists() || userDoc.data().displayName !== 'Alice Martin') {
        throw new Error('Impossible de lire son propre profil utilisateur');
      }
    });

    // ----------------------------------------------------
    // TEST 4: Un utilisateur connecte NE PEUT PAS enumerer les familles (allow list: if false)
    // ----------------------------------------------------
    await test('4. Un utilisateur connecte NE PEUT PAS enumerer les familles (allow list: if false)', async () => {
      const aliceContext = testEnv.authenticatedContext('alice_uid');
      const db = aliceContext.firestore({ databaseId: DATABASE_ID });

      // Tentative d'énumération globale de la collection 'families'
      await assertFails(getDocs(collection(db, 'families')));

      // Même avec un autre utilisateur authentifié
      const bobContext = testEnv.authenticatedContext('bob_uid');
      const bobDb = bobContext.firestore({ databaseId: DATABASE_ID });
      await assertFails(getDocs(collection(bobDb, 'families')));
    });

    // ----------------------------------------------------
    // TEST 5: Un utilisateur connecte NE PEUT PAS acceder aux donnees d un autre foyer
    // ----------------------------------------------------
    await test('5. Un utilisateur connecte NE PEUT PAS acceder aux donnees d un autre foyer', async () => {
      const malloryContext = testEnv.authenticatedContext('mallory_uid');
      const db = malloryContext.firestore({ databaseId: DATABASE_ID });

      // Mallory tente de lire le foyer d'Alice
      await assertFails(getDoc(doc(db, 'families', 'family_alice_1')));

      // Mallory tente de lire les accès du foyer d'Alice
      await assertFails(getDoc(doc(db, 'families', 'family_alice_1', 'access', 'alice_uid')));

      // Mallory tente de lire les membres du foyer d'Alice
      await assertFails(getDoc(doc(db, 'families', 'family_alice_1', 'members', 'member_alice_1')));

      // Mallory tente de lire le profil utilisateur d'Alice
      await assertFails(getDoc(doc(db, 'users', 'alice_uid')));
    });

    // ----------------------------------------------------
    // TEST 6: Creation du premier foyer avec utilisateur existant (update users/{uid})
    // ----------------------------------------------------
    await test('6. Creation du premier foyer avec utilisateur existant (mise a jour user)', async () => {
      await testEnv.withSecurityRulesDisabled(async (context) => {
        const db = context.firestore({ databaseId: DATABASE_ID });
        await setDoc(doc(db, 'users', 'bob_uid'), {
          id: 'bob_uid',
          displayName: 'Bob Dupont',
          email: 'bob@familleplanning.fr',
          createdAt: Timestamp.now(),
          updatedAt: Timestamp.now()
        });
      });

      const bobContext = testEnv.authenticatedContext('bob_uid');
      const db = bobContext.firestore({ databaseId: DATABASE_ID });

      const batch = writeBatch(db);
      const familyRef = doc(db, 'families', 'family_bob_1');
      const accessRef = doc(db, 'families', 'family_bob_1', 'access', 'bob_uid');
      const memberRef = doc(db, 'families', 'family_bob_1', 'members', 'member_bob_1');
      const userRef = doc(db, 'users', 'bob_uid');

      const now = Timestamp.now();

      batch.set(familyRef, {
        name: 'Famille Dupont',
        ownerId: 'bob_uid',
        timezone: 'Europe/Paris',
        createdAt: now
      });

      batch.set(accessRef, {
        role: 'owner',
        joinedAt: now
      });

      batch.set(memberRef, {
        name: 'Bob',
        userId: 'bob_uid',
        role: 'parent',
        roleLabel: 'Parent',
        color: '#48DBFB',
        avatarEmoji: '😊'
      });

      batch.update(userRef, {
        currentFamilyId: 'family_bob_1',
        familyIds: ['family_bob_1'],
        updatedAt: now
      });

      await assertSucceeds(batch.commit());

      // Bob can read his own family
      const bobFamily = await getDoc(doc(db, 'families', 'family_bob_1'));
      if (!bobFamily.exists() || bobFamily.data().name !== 'Famille Dupont') {
        throw new Error('Bob ne peut pas lire son foyer');
      }

      // Alice cannot read Bob's family
      const aliceContext = testEnv.authenticatedContext('alice_uid');
      const aliceDb = aliceContext.firestore({ databaseId: DATABASE_ID });
      await assertFails(getDoc(doc(aliceDb, 'families', 'family_bob_1')));
    });

    // ----------------------------------------------------
    // TEST 7: Attaquant tentant de s attribuer un acces owner dans une famille existante
    // ----------------------------------------------------
    await test('7. Attaquant ne peut pas s ajouter owner dans une famille existante', async () => {
      const malloryContext = testEnv.authenticatedContext('mallory_uid');
      const db = malloryContext.firestore({ databaseId: DATABASE_ID });

      const accessRef = doc(db, 'families', 'family_alice_1', 'access', 'mallory_uid');

      await assertFails(
        setDoc(accessRef, {
          role: 'owner',
          joinedAt: Timestamp.now()
        })
      );
    });

    // ----------------------------------------------------
    // TEST 8: Auto-promotion interdite pour un simple membre
    // ----------------------------------------------------
    await test('8. Un membre regulier ne peut pas s auto-promouvoir owner', async () => {
      const aliceContext = testEnv.authenticatedContext('alice_uid');
      const aliceDb = aliceContext.firestore({ databaseId: DATABASE_ID });
      const charlieAccessRef = doc(aliceDb, 'families', 'family_alice_1', 'access', 'charlie_uid');

      await assertSucceeds(
        setDoc(charlieAccessRef, {
          role: 'member',
          joinedAt: Timestamp.now()
        })
      );

      const charlieContext = testEnv.authenticatedContext('charlie_uid');
      const charlieDb = charlieContext.firestore({ databaseId: DATABASE_ID });
      const charlieSelfAccess = doc(charlieDb, 'families', 'family_alice_1', 'access', 'charlie_uid');

      await assertFails(
        updateDoc(charlieSelfAccess, {
          role: 'owner'
        })
      );
    });

    // ----------------------------------------------------
    // TEST 9: Gestion des evenements par un membre du foyer
    // ----------------------------------------------------
    await test('9. Un membre du foyer peut creer et lire ses evenements', async () => {
      const aliceContext = testEnv.authenticatedContext('alice_uid');
      const db = aliceContext.firestore({ databaseId: DATABASE_ID });

      const eventRef = doc(db, 'families', 'family_alice_1', 'events', 'event_1');
      await assertSucceeds(
        setDoc(eventRef, {
          familyId: 'family_alice_1',
          title: 'Dîner en famille',
          description: 'Soirée crêpes',
          startAt: Timestamp.now(),
          endAt: Timestamp.now(),
          allDay: false,
          participantIds: ['member_alice_1'],
          category: 'famille',
          color: '#FF6B6B',
          createdBy: 'alice_uid',
          createdAt: Timestamp.now()
        })
      );

      const fetched = await getDoc(eventRef);
      if (!fetched.exists() || fetched.data().title !== 'Dîner en famille') {
        throw new Error('Impossible de lire l evenement cree');
      }
    });

    // ----------------------------------------------------
    // TEST 10: Un non-membre ne peut pas creer d evenements dans le foyer
    // ----------------------------------------------------
    await test('10. Un non-membre ne peut pas creer d evenements dans un autre foyer', async () => {
      const malloryContext = testEnv.authenticatedContext('mallory_uid');
      const db = malloryContext.firestore({ databaseId: DATABASE_ID });

      const eventRef = doc(db, 'families', 'family_alice_1', 'events', 'event_mallory');
      await assertFails(
        setDoc(eventRef, {
          familyId: 'family_alice_1',
          title: 'Pirate Event',
          startAt: Timestamp.now(),
          endAt: Timestamp.now()
        })
      );
    });

    console.log(`\n🎉 All ${passedTests}/${totalTests} unit test scenarios PASSED successfully!`);
  } finally {
    await testEnv.cleanup();
  }
}

runTests().catch((err) => {
  console.error('Fatal test failure:', err);
  process.exit(1);
});
