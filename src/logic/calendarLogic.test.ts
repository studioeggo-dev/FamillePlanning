import test from 'node:test';
import assert from 'node:assert/strict';
import {
  parseUserMap,
  parseFamilyMemberMap,
  parseCalendarEventMap,
  checkAccessRole,
  isEventSpanningDay,
  getMondayOfWeek,
  getWeekDays,
  filterEvents,
  validateDatabaseId,
  type PureCalendarEvent
} from './calendarLogic';

test('parseUserMap handles both activeFamilyId and currentFamilyId alias', () => {
  const user1 = parseUserMap('u1', { email: 'test@example.com', currentFamilyId: 'fam_123' });
  assert.equal(user1.activeFamilyId, 'fam_123');

  const user2 = parseUserMap('u2', { email: 'test2@example.com', activeFamilyId: 'fam_456' });
  assert.equal(user2.activeFamilyId, 'fam_456');
});

test('parseFamilyMemberMap handles firstName/name and colorHex/color aliases', () => {
  const m1 = parseFamilyMemberMap('m1', { name: 'Sophie', color: '#FF6B6B', role: 'Maman' });
  assert.equal(m1.firstName, 'Sophie');
  assert.equal(m1.colorHex, '#FF6B6B');
  assert.equal(m1.roleName, 'Maman');

  const m2 = parseFamilyMemberMap('m2', { firstName: 'Leo', colorHex: '#1DD1A1', isChild: true });
  assert.equal(m2.firstName, 'Leo');
  assert.equal(m2.colorHex, '#1DD1A1');
  assert.equal(m2.isChild, true);
});

test('parseCalendarEventMap handles startAt/startInstant, allDay/isAllDay, and color aliases', () => {
  const e1 = parseCalendarEventMap('e1', {
    name: 'Sortie Zoo',
    startAt: '2026-10-26T10:00:00Z',
    endAt: '2026-10-26T17:00:00Z',
    allDay: true,
    color: '#FECA57'
  });

  assert.equal(e1.title, 'Sortie Zoo');
  assert.equal(e1.startIso, '2026-10-26T10:00:00Z');
  assert.equal(e1.isAllDay, true);
  assert.equal(e1.colorHex, '#FECA57');
});

test('checkAccessRole correctly identifies owner, admin, member and viewer permissions', () => {
  assert.deepEqual(checkAccessRole('owner'), { isOwnerOrAdmin: true, isMemberOrHigher: true });
  assert.deepEqual(checkAccessRole('admin'), { isOwnerOrAdmin: true, isMemberOrHigher: true });
  assert.deepEqual(checkAccessRole('member'), { isOwnerOrAdmin: false, isMemberOrHigher: true });
  assert.deepEqual(checkAccessRole('viewer'), { isOwnerOrAdmin: false, isMemberOrHigher: false });
});

test('isEventSpanningDay correctly identifies multi-day event coverage', () => {
  const startIso = '2026-10-26T14:00:00Z';
  const endIso = '2026-10-28T11:00:00Z';

  assert.equal(isEventSpanningDay(startIso, endIso, '2026-10-25'), false);
  assert.equal(isEventSpanningDay(startIso, endIso, '2026-10-26'), true);
  assert.equal(isEventSpanningDay(startIso, endIso, '2026-10-27'), true);
  assert.equal(isEventSpanningDay(startIso, endIso, '2026-10-28'), true);
  assert.equal(isEventSpanningDay(startIso, endIso, '2026-10-29'), false);
});

test('getMondayOfWeek correctly returns Monday for any day of the week', () => {
  assert.equal(getMondayOfWeek('2026-10-26'), '2026-10-26');
  assert.equal(getMondayOfWeek('2026-10-28'), '2026-10-26');
  assert.equal(getMondayOfWeek('2026-11-01'), '2026-10-26');
});

test('validateDatabaseId rejects default and accepts named Firestore database', () => {
  assert.equal(validateDatabaseId('(default)'), false);
  assert.equal(validateDatabaseId('default'), false);
  assert.equal(
    validateDatabaseId('ai-studio-familleplanning-5bf1e4ae-719b-4752-9367-6a8444b48be1'),
    true
  );
});
