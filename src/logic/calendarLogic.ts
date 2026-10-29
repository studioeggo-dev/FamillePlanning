/**
 * Logique pure partagée de FamillePlanning pour le fuseau Europe/Paris,
 * la sérialisation bi-directionnelle, la gestion DST (heure d'été/hiver),
 * les droits d'accès et le filtrage des événements.
 */

export interface PureUser {
  uid: string;
  email: string;
  displayName: string;
  photoUrl?: string;
  activeFamilyId?: string;
}

export function parseUserMap(uid: string, map: Record<string, any>): PureUser {
  return {
    uid,
    email: map.email || '',
    displayName: map.displayName || map.name || '',
    photoUrl: map.photoUrl || map.avatarUrl,
    activeFamilyId: map.activeFamilyId || map.currentFamilyId
  };
}

export interface PureFamilyAccess {
  uid: string;
  role: 'owner' | 'admin' | 'member' | 'viewer';
}

export function checkAccessRole(role: string): { isOwnerOrAdmin: boolean; isMemberOrHigher: boolean } {
  const normRole = (role || '').toLowerCase();
  const isOwnerOrAdmin = normRole === 'owner' || normRole === 'admin';
  const isMemberOrHigher = isOwnerOrAdmin || normRole === 'member';
  return { isOwnerOrAdmin, isMemberOrHigher };
}

export interface PureFamilyMember {
  id: string;
  firstName: string;
  avatarUrl?: string;
  colorHex: string;
  roleName?: string;
  isChild: boolean;
  birthDate?: string;
}

export function parseFamilyMemberMap(id: string, map: Record<string, any>): PureFamilyMember {
  return {
    id,
    firstName: map.firstName || map.name || '',
    avatarUrl: map.avatarUrl || map.photoUrl,
    colorHex: map.colorHex || map.color || '#FF6B6B',
    roleName: map.roleName || map.role,
    isChild: typeof map.isChild === 'boolean' ? map.isChild : map.type === 'child',
    birthDate: map.birthDate
  };
}

export interface PureCalendarEvent {
  id: string;
  title: string;
  description: string;
  startIso: string;
  endIso: string;
  isAllDay: boolean;
  participantIds: string[];
  category: string;
  colorHex: string;
  location?: string;
}

export function parseCalendarEventMap(id: string, map: Record<string, any>): PureCalendarEvent {
  const startRaw = map.startInstant || map.startAt || map.startDate || map.startTime || new Date().toISOString();
  const endRaw = map.endInstant || map.endAt || map.endDate || map.endTime || startRaw;
  const participants = map.participantIds || map.participants || map.assignedTo || [];

  return {
    id,
    title: map.title || map.name || '',
    description: map.description || '',
    startIso: typeof startRaw === 'string' ? startRaw : new Date(startRaw).toISOString(),
    endIso: typeof endRaw === 'string' ? endRaw : new Date(endRaw).toISOString(),
    isAllDay: typeof map.isAllDay === 'boolean' ? map.isAllDay : Boolean(map.allDay),
    participantIds: Array.isArray(participants) ? participants : [],
    category: map.category || 'general',
    colorHex: map.colorHex || map.color || '#FF6B6B',
    location: map.location
  };
}

/**
 * Calcule si un événement s'étale sur la journée spécifiée (ISO Date YYYY-MM-DD).
 */
/**
 * Date civile au fuseau Europe/Paris pour un instant donné.
 * Ne JAMAIS utiliser toISOString().slice(0,10) pour le calendrier familial :
 * cette expression donne la date UTC, décalée près de minuit en France.
 */
export function dayInParis(instant: Date): string {
  if (Number.isNaN(instant.getTime())) throw new RangeError('Date invalide');
  const formatter = new Intl.DateTimeFormat('en-GB', {
    timeZone: 'Europe/Paris', year: 'numeric', month: '2-digit', day: '2-digit'
  });
  const parts = formatter.formatToParts(instant);
  const get = (type: string) => parts.find(part => part.type === type)?.value;
  return `${get('year')}-${get('month')}-${get('day')}`;
}

/**
 * L'événement est visible uniquement si [start, end) intersecte le jour
 * calendaire choisi à Paris. La fin est exclusive : un rendez-vous terminé
 * exactement à minuit ne s'affiche pas le lendemain.
 * Les journées de changement d'heure (23h / 25h) restent correctes.
 */
export function isEventSpanningDay(startIso: string, endIso: string, dayIso: string): boolean {
  if (!/^\\d{4}-\\d{2}-\\d{2}$/.test(dayIso)) return false;
  const start = new Date(startIso);
  const end = new Date(endIso);
  if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime())) return false;
  const startDay = dayInParis(start);
  if (end.getTime() <= start.getTime()) return dayIso === startDay;
  const lastIncludedDay = dayInParis(new Date(end.getTime() - 1));
  return dayIso >= startDay && dayIso <= lastIncludedDay;
}

/**
 * Calcule le lundi de la semaine pour une date ISO YYYY-MM-DD.
 */
export function getMondayOfWeek(isoDateString: string): string {
  const [y, m, d] = isoDateString.split('-').map(Number);
  const date = new Date(Date.UTC(y, m - 1, d));
  const day = date.getUTCDay();
  const diffToMonday = day === 0 ? -6 : 1 - day;
  date.setUTCDate(date.getUTCDate() + diffToMonday);
  return date.toISOString().split('T')[0];
}

/**
 * Génère les 7 dates (lundi au dimanche) de la semaine.
 */
export function getWeekDays(referenceDate: string): string[] {
  const mondayIso = getMondayOfWeek(referenceDate);
  const [y, m, d] = mondayIso.split('-').map(Number);
  const days: string[] = [];
  for (let i = 0; i < 7; i++) {
    const cur = new Date(Date.UTC(y, m - 1, d + i));
    days.push(cur.toISOString().split('T')[0]);
  }
  return days;
}

/**
 * Filtre les événements selon le participant sélectionné et la date/semaine active.
 */
export function filterEvents(
  events: PureCalendarEvent[],
  activeParticipantId: string | null,
  activeDateIso: string,
  viewMode: 'week' | 'month'
): PureCalendarEvent[] {
  const weekDays = new Set(getWeekDays(activeDateIso));
  const activeMonthPrefix = activeDateIso.slice(0, 7);

  return events.filter(e => {
    if (activeParticipantId && !e.participantIds.includes(activeParticipantId)) {
      return false;
    }
    if (viewMode === 'week') {
      return Array.from(weekDays).some(day => isEventSpanningDay(e.startIso, e.endIso, day));
    } else {
      return e.startIso.startsWith(activeMonthPrefix) || e.endIso.startsWith(activeMonthPrefix);
    }
  });
}

export interface MonthDayCell {
  dateIso: string;
  dayNumber: number;
  isCurrentMonth: boolean;
}

/**
 * Calcule la grille de 35 ou 42 jours pour la vue mois (lundi à dimanche).
 */
export function getMonthGrid(referenceDate: string): MonthDayCell[] {
  const [y, m] = referenceDate.split('-').map(Number);
  const firstOfMonth = new Date(Date.UTC(y, m - 1, 1));
  const dayOfWeek = firstOfMonth.getUTCDay();
  const offsetToMonday = dayOfWeek === 0 ? 6 : dayOfWeek - 1;

  const lastOfMonth = new Date(Date.UTC(y, m, 0));
  const totalDaysInMonth = lastOfMonth.getUTCDate();

  const grid: MonthDayCell[] = [];

  const prevMonthLast = new Date(Date.UTC(y, m - 1, 0));
  const prevMonthDays = prevMonthLast.getUTCDate();
  for (let i = offsetToMonday - 1; i >= 0; i--) {
    const dayNum = prevMonthDays - i;
    const d = new Date(Date.UTC(y, m - 2, dayNum));
    grid.push({
      dateIso: d.toISOString().split('T')[0],
      dayNumber: dayNum,
      isCurrentMonth: false
    });
  }

  for (let dayNum = 1; dayNum <= totalDaysInMonth; dayNum++) {
    const d = new Date(Date.UTC(y, m - 1, dayNum));
    grid.push({
      dateIso: d.toISOString().split('T')[0],
      dayNumber: dayNum,
      isCurrentMonth: true
    });
  }

  const remaining = (7 - (grid.length % 7)) % 7;
  for (let dayNum = 1; dayNum <= remaining; dayNum++) {
    const d = new Date(Date.UTC(y, m, dayNum));
    grid.push({
      dateIso: d.toISOString().split('T')[0],
      dayNumber: dayNum,
      isCurrentMonth: false
    });
  }

  return grid;
}

/**
 * Navigation temporelle selon le mode Semaine (+- 7 jours) ou Mois (+- 1 mois).
 */
export function navigateDate(currentIso: string, mode: 'week' | 'month', delta: number): string {
  const [y, m, d] = currentIso.split('-').map(Number);
  if (mode === 'week') {
    const date = new Date(Date.UTC(y, m - 1, d + delta * 7));
    return date.toISOString().split('T')[0];
  } else {
    // En vue mois, on avance/recule de 1 mois en gardant le 1er du mois ou le jour
    const date = new Date(Date.UTC(y, m - 1 + delta, Math.min(d, 28)));
    return date.toISOString().split('T')[0];
  }
}

/**
 * Retourne la date courante (YYYY-MM-DD) dans le fuseau Europe/Paris.
 */
export function todayInParis(): string {
  return new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/Paris' }).format(new Date());
}

/**
 * Valide que la configuration Firestore cible exactement la base nommée
 */
export function validateDatabaseId(dbName: string): boolean {
  const EXPECTED = 'ai-studio-familleplanning-5bf1e4ae-719b-4752-9367-6a8444b48be1';
  return dbName === EXPECTED;
}
