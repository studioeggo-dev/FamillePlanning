import test from 'node:test';
import assert from 'node:assert/strict';
import {
  parseUserMap,
  parseFamilyMemberMap,
  parseCalendarEventMap,
  checkAccessRole,
  isEventSpanningDay,
  dayInParis,
  getMondayOfWeek,
  getWeekDays,
  filterEvents,
  validateDatabaseId,
  getMonthGrid,
  navigateDate,
  todayInParis,
  type PureCalendarEvent
} from './calendarLogic';

test('getMonthGrid returns a valid 35 or 42 cell grid covering the month with mondays as first col', () => {
  const grid = getMonthGrid('2026-10-15');
  assert.equal(grid.length % 7, 0);
  assert.ok(grid.length === 35 || grid.length === 42);

  // Check Oct 1st is present and marked as current month
  const octFirst = grid.find(c => c.dateIso === '2026-10-01');
  assert.ok(octFirst);
  assert.equal(octFirst.isCurrentMonth, true);

  // Check Sep 30 is present as previous month
  const sepLast = grid.find(c => c.dateIso === '2026-09-30');
  assert.ok(sepLast);
  assert.equal(sepLast.isCurrentMonth, false);
});

test('navigateDate handles week and month increments and decrements', () => {
  // Week
  assert.equal(navigateDate('2026-10-15', 'week', 1), '2026-10-22');
  assert.equal(navigateDate('2026-10-15', 'week', -1), '2026-10-08');

  // Month
  assert.equal(navigateDate('2026-10-15', 'month', 1), '2026-11-15');
  assert.equal(navigateDate('2026-10-15', 'month', -1), '2026-09-15');
});

test('todayInParis returns ISO format date YYYY-MM-DD', () => {
  const today = todayInParis();
  assert.match(today, /^\d{4}-\d{2}-\d{2}$/);
});

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

test('two appointments on two different days never appear together for a selected day', () => {
  const thursday = { id: 'thu', start: '2026-10-29T14:00:00.000Z', end: '2026-10-29T15:00:00.000Z' };
  const friday = { id: 'fri', start: '2026-10-30T14:00:00.000Z', end: '2026-10-30T15:00:00.000Z' };
  const appointments = [thursday, friday];
  const matches = (day: string) =>
    appointments.filter(e => isEventSpanningDay(e.start, e.end, day)).map(e => e.id);
  assert.deepEqual(matches('2026-10-29'), ['thu']);
  assert.deepEqual(matches('2026-10-30'), ['fri']);
  assert.deepEqual(matches('2026-10-31'), []);
});

test('Paris day comparison respects local midnight and daylight saving transitions', () => {
  // 23:30 UTC on 29 October is 00:30 on 30 October in Paris.
  assert.equal(dayInParis(new Date('2026-10-29T23:30:00.000Z')), '2026-10-30');
  assert.equal(isEventSpanningDay('2026-10-29T23:30:00Z', '2026-10-30T00:30:00Z', '2026-10-29'), false);
  assert.equal(isEventSpanningDay('2026-10-29T23:30:00Z', '2026-10-30T00:30:00Z', '2026-10-30'), true);
  // Paris switches to winter time on 25 October 2026. Both instants are on 25 October locally.
  assert.equal(isEventSpanningDay('2026-10-25T00:30:00Z', '2026-10-25T02:30:00Z', '2026-10-25'), true);
  assert.equal(isEventSpanningDay('2026-10-25T00:30:00Z', '2026-10-25T02:30:00Z', '2026-10-26'), false);
});

test('end at midnight is exclusive and a truly multi-day event remains visible on its covered days', () => {
  const start = '2026-10-29T20:00:00Z'; // 21:00 Paris
  const midnight = '2026-10-29T23:00:00Z'; // 00:00 Paris 30 October
  assert.equal(isEventSpanningDay(start, midnight, '2026-10-29'), true);
  assert.equal(isEventSpanningDay(start, midnight, '2026-10-30'), false);
  const multiEnd = '2026-10-31T08:00:00Z';
  assert.equal(isEventSpanningDay(start, multiEnd, '2026-10-29'), true);
  assert.equal(isEventSpanningDay(start, multiEnd, '2026-10-30'), true);
  assert.equal(isEventSpanningDay(start, multiEnd, '2026-10-31'), true);
  assert.equal(isEventSpanningDay(start, multiEnd, '2026-11-01'), false);
});
