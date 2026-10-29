import React, { useEffect, useMemo, useState } from "react";
import {
  CalendarDays, Users, Plus, Link2, Copy, Check, LogOut, RefreshCw,
  ShieldCheck, Baby, Mail, ArrowRight, CalendarPlus, Clock, LockKeyhole,
  ChevronLeft, ChevronRight, Calendar as CalendarIcon, MapPin, Trash2, X,
  AlertCircle
} from "lucide-react";
import {
  GoogleAuthProvider, onAuthStateChanged, signInWithPopup,
  signInWithEmailAndPassword, createUserWithEmailAndPassword,
  sendEmailVerification, reload, signOut, type User
} from "firebase/auth";
import { auth, configured } from "./firebase";
import {
  readProfile, createFirstFamily, getHome, getRole, observeMembers, observeEvents,
  addChild, createInvite, previewInvite, acceptInvite, addEvent, updateEvent, deleteEvent,
  friendlyError, type Family, type Member, type Event
} from "./familyService";
import {
  getWeekDays, getMonthGrid, navigateDate, todayInParis, isEventSpanningDay
} from "./logic/calendarLogic";
import type { Unsubscribe } from "firebase/firestore";

const PALETTE_COLORS = ["#FF6B6B", "#48DBFB", "#1DD1A1", "#FECA57", "#A55EEA", "#FF9F43"];
const DEFAULT_CORAL = "#FF6B6B";
const PARENT_ROLES = ["owner", "parent", "admin"];

const CATEGORIES = [
  { id: "general", label: "Activité générale" },
  { id: "school", label: "École / Crèche" },
  { id: "sport", label: "Sport" },
  { id: "health", label: "Santé / Médecin" },
  { id: "outing", label: "Sortie / Loisir" }
];

function formatDayMonthFrench(isoDate: string): string {
  const [y, m, d] = isoDate.split("-").map(Number);
  const date = new Date(Date.UTC(y, m - 1, d, 12, 0, 0));
  return new Intl.DateTimeFormat("fr-FR", {
    weekday: "long",
    day: "numeric",
    month: "long"
  }).format(date);
}

function formatMonthYearFrench(isoDate: string): string {
  const [y, m, d] = isoDate.split("-").map(Number);
  const date = new Date(Date.UTC(y, m - 1, d, 12, 0, 0));
  return new Intl.DateTimeFormat("fr-FR", {
    month: "long",
    year: "numeric"
  }).format(date);
}

function formatTime(date: Date): string {
  return new Intl.DateTimeFormat("fr-FR", {
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}

function EmojiAvatar({ name, color, size = 42 }: { name: string; color: string; size?: number }) {
  const initial = name ? name.trim().slice(0, 1).toLocaleUpperCase("fr") : "?";
  return (
    <span
      className="avatar"
      style={{
        width: size,
        height: size,
        background: color + "22",
        color,
        borderColor: color
      }}
    >
      {initial}
    </span>
  );
}

function ColorsPicker({ value, onChange }: { value: string; onChange: (v: string) => void }) {
  return (
    <div className="color-row">
      {PALETTE_COLORS.map(c => (
        <button
          key={c}
          type="button"
          title={"Choisir " + c}
          onClick={() => onChange(c)}
          className={"color-dot " + (value.toLowerCase() === c.toLowerCase() ? "selected" : "")}
          style={{ background: c }}
        />
      ))}
    </div>
  );
}

function NoticeBanner({ text, onDismiss }: { text: string; onDismiss: () => void }) {
  return (
    <div className="notice" role="status">
      <p>{text}</p>
      <button onClick={onDismiss} aria-label="Fermer">×</button>
    </div>
  );
}

export default function App() {
  const [account, setAccount] = useState<User | null>(null);
  const [ready, setReady] = useState(false);
  const [familyId, setFamilyId] = useState<string | null>(null);
  const [family, setFamily] = useState<Family | null>(null);
  const [role, setRole] = useState("");
  const [members, setMembers] = useState<Member[]>([]);
  const [events, setEvents] = useState<Event[]>([]);
  const [tab, setTab] = useState<"planning" | "family">("planning");
  const [viewMode, setViewMode] = useState<"week" | "month">("week");
  const [selectedDate, setSelectedDate] = useState<string>(() => todayInParis());
  const [selectedParticipantId, setSelectedParticipantId] = useState<string | null>(null);

  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");

  // Invitations
  const [inviteToken, setInviteToken] = useState(() => new URLSearchParams(window.location.search).get("invite") || "");
  const [preview, setPreview] = useState<{ familyName: string; role: string; expiresAt: string } | null>(null);
  const [acceptName, setAcceptName] = useState("");
  const [acceptColor, setAcceptColor] = useState(DEFAULT_CORAL);

  // Authentification par e-mail
  const [emailMode, setEmailMode] = useState<"login" | "signup" | null>(null);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  // Setup premier foyer
  const [setupName, setSetupName] = useState("");
  const [setupFirstName, setSetupFirstName] = useState("");
  const [setupColor, setSetupColor] = useState(DEFAULT_CORAL);

  // Modales enfants et invitations
  const [childOpen, setChildOpen] = useState(false);
  const [childName, setChildName] = useState("");
  const [childBirth, setChildBirth] = useState("");
  const [childColor, setChildColor] = useState("#A55EEA");

  const [inviteOpen, setInviteOpen] = useState(false);
  const [inviteRole, setInviteRole] = useState<"parent" | "adult">("adult");
  const [inviteEmail, setInviteEmail] = useState("");
  const [generatedLink, setGeneratedLink] = useState("");

  // Éditeur d'événement (Création / Modification / Suppression)
  const [eventEditorOpen, setEventEditorOpen] = useState(false);
  const [editingEvent, setEditingEvent] = useState<Event | null>(null);
  const [eventTitle, setEventTitle] = useState("");
  const [eventDescription, setEventDescription] = useState("");
  const [eventDate, setEventDate] = useState(() => todayInParis());
  const [eventStart, setEventStart] = useState("10:00");
  const [eventEnd, setEventEnd] = useState("11:00");
  const [eventIsAllDay, setEventIsAllDay] = useState(false);
  const [eventCategory, setEventCategory] = useState("general");
  const [eventColor, setEventColor] = useState(DEFAULT_CORAL);
  const [eventLocation, setEventLocation] = useState("");
  const [eventParticipants, setEventParticipants] = useState<string[]>([]);
  const [confirmDeleteOpen, setConfirmDeleteOpen] = useState(false);

  async function refreshProfile(user: User) {
    const profile = await readProfile(user.uid);
    setFamilyId(profile.currentFamilyId);
    return profile;
  }

  // Écoute de l'authentification
  useEffect(() => {
    if (!configured || !auth) {
      setReady(true);
      return;
    }
    return onAuthStateChanged(auth, async user => {
      setAccount(user);
      setFamilyId(null);
      setReady(false);
      if (user) {
        try {
          await refreshProfile(user);
        } catch (e) {
          setNotice("Chargement du profil : " + friendlyError(e));
        }
      }
      setReady(true);
    });
  }, []);

  // Écoute temps réel Firestore du foyer, des membres et des événements
  useEffect(() => {
    if (!familyId || !account) {
      setFamily(null);
      setRole("");
      setMembers([]);
      setEvents([]);
      return;
    }
    let alive = true;
    const subs: Unsubscribe[] = [];

    Promise.all([getHome(familyId), getRole(familyId, account.uid)]).then(([home, accessRole]) => {
      if (alive) {
        setFamily(home);
        setRole(accessRole || (home.ownerId === account.uid ? "owner" : "viewer"));
      }
    }).catch(e => setNotice("Lecture de la famille : " + friendlyError(e)));

    // Synchronisation en temps réel des membres
    subs.push(observeMembers(familyId, items => {
      if (alive) setMembers(items);
    }, e => setNotice("Membres : " + friendlyError(e))));

    // Synchronisation en temps réel des événements
    subs.push(observeEvents(familyId, items => {
      if (alive) setEvents(items);
    }, e => setNotice("Événements : " + friendlyError(e))));

    return () => {
      alive = false;
      subs.forEach(unsub => unsub());
    };
  }, [account?.uid, familyId]);

  // Prévisualisation d'invitation
  useEffect(() => {
    if (!inviteToken || !account) {
      setPreview(null);
      return;
    }
    let active = true;
    previewInvite(inviteToken)
      .then(p => { if (active) setPreview(p); })
      .catch(e => {
        if (active) {
          setPreview(null);
          setNotice("Invitation : " + friendlyError(e));
        }
      });
    return () => { active = false; };
  }, [inviteToken, account?.uid]);

  const isParent = PARENT_ROLES.includes(role) || family?.ownerId === account?.uid;
  const parentsCount = members.filter(m => PARENT_ROLES.includes(m.role)).length;

  // Calcul des événements visibles selon le participant et la date active
  const visibleEvents = useMemo(() => {
    return events
      .filter(e => {
        // Filtre participant
        if (selectedParticipantId && !e.participantIds.includes(selectedParticipantId)) {
          return false;
        }
        // Filtre de la journée sélectionnée
        return isEventSpanningDay(e.startAt.toISOString(), e.endAt.toISOString(), selectedDate);
      })
      .sort((a, b) => a.startAt.getTime() - b.endAt.getTime());
  }, [events, selectedDate, selectedParticipantId]);

  // Jours de la semaine courante
  const weekDays = useMemo(() => {
    return getWeekDays(selectedDate);
  }, [selectedDate]);

  // Grille du mois courant
  const monthGrid = useMemo(() => {
    return getMonthGrid(selectedDate);
  }, [selectedDate]);

  // Dictionnaire des événements par jour pour afficher les pastilles dans la grille mois
  const eventsByDay = useMemo(() => {
    const map = new Map<string, string[]>();
    events.forEach(e => {
      if (selectedParticipantId && !e.participantIds.includes(selectedParticipantId)) {
        return;
      }
      const dayIso = e.startAt.toISOString().slice(0, 10);
      const existing = map.get(dayIso) || [];
      if (!existing.includes(e.color)) {
        existing.push(e.color);
      }
      map.set(dayIso, existing);
    });
    return map;
  }, [events, selectedParticipantId]);

  async function run(action: () => Promise<void>) {
    if (busy) return;
    setBusy(true);
    setNotice("");
    try {
      await action();
    } catch (e) {
      setNotice(friendlyError(e));
    } finally {
      setBusy(false);
    }
  }

  async function googleLogin() {
    await run(async () => {
      if (!auth) throw new Error("Firebase Web non configuré.");
      await signInWithPopup(auth, new GoogleAuthProvider());
    });
  }

  async function loginEmail(e: React.FormEvent) {
    e.preventDefault();
    await run(async () => {
      if (!auth) throw new Error("Firebase Web non configuré.");
      if (emailMode === "signup") {
        const res = await createUserWithEmailAndPassword(auth, email, password);
        await sendEmailVerification(res.user);
        setNotice("Vérifiez votre e-mail avant d'accepter une invitation.");
      } else {
        await signInWithEmailAndPassword(auth, email, password);
      }
      setEmailMode(null);
      setPassword("");
    });
  }

  async function createFamily(e: React.FormEvent) {
    e.preventDefault();
    if (!account) return;
    await run(async () => {
      await reload(account);
      if (!account.emailVerified) {
        throw new Error("Vérifiez d'abord votre adresse e-mail avant de créer une famille.");
      }
      await createFirstFamily(account, setupName, setupFirstName, setupColor);
      await refreshProfile(account);
      setNotice("Votre famille est créée et synchronisée !");
    });
  }

  async function createChild(e: React.FormEvent) {
    e.preventDefault();
    if (!familyId || !account) return;
    await run(async () => {
      await addChild(familyId, account.uid, childName, childBirth || null, childColor);
      setChildOpen(false);
      setChildName("");
      setChildBirth("");
      setNotice("L'enfant apparaît dans la famille et dans les filtres du planning.");
    });
  }

  async function shareInvite(e: React.FormEvent) {
    e.preventDefault();
    if (!familyId) return;
    await run(async () => {
      const answer = await createInvite(familyId, inviteRole, inviteEmail || null);
      const url = new URL(window.location.origin + window.location.pathname);
      url.searchParams.set("invite", answer.token);
      setGeneratedLink(url.toString());
      setInviteOpen(false);
      setInviteEmail("");
    });
  }

  async function accept(e: React.FormEvent) {
    e.preventDefault();
    if (!account) return;
    await run(async () => {
      await reload(account);
      if (!account.emailVerified) throw new Error("Vérifiez d'abord votre adresse e-mail, puis actualisez votre compte.");
      await acceptInvite(inviteToken, acceptName, acceptColor);
      setInviteToken("");
      setPreview(null);
      window.history.replaceState({}, "", window.location.pathname);
      await refreshProfile(account);
      setNotice("Bienvenue dans votre famille !");
    });
  }

  // Ouverture de l'éditeur en mode Création
  function openCreateEventDialog() {
    setEditingEvent(null);
    setEventTitle("");
    setEventDescription("");
    setEventDate(selectedDate);
    setEventStart("10:00");
    setEventEnd("11:00");
    setEventIsAllDay(false);
    setEventCategory("general");
    setEventColor(DEFAULT_CORAL);
    setEventLocation("");
    // Par défaut, si un participant est filtré, le sélectionner
    setEventParticipants(selectedParticipantId ? [selectedParticipantId] : []);
    setConfirmDeleteOpen(false);
    setEventEditorOpen(true);
  }

  // Ouverture de l'éditeur en mode Modification
  function openEditEventDialog(event: Event) {
    setEditingEvent(event);
    setEventTitle(event.title);
    setEventDescription(event.description || "");
    const datePart = event.startAt.toISOString().slice(0, 10);
    setEventDate(datePart);
    const sH = String(event.startAt.getHours()).padStart(2, "0");
    const sM = String(event.startAt.getMinutes()).padStart(2, "0");
    const eH = String(event.endAt.getHours()).padStart(2, "0");
    const eM = String(event.endAt.getMinutes()).padStart(2, "0");
    setEventStart(`${sH}:${sM}`);
    setEventEnd(`${eH}:${eM}`);
    setEventIsAllDay(event.isAllDay);
    setEventCategory(event.category || "general");
    setEventColor(event.color || DEFAULT_CORAL);
    setEventLocation(event.location || "");
    setEventParticipants(event.participantIds || []);
    setConfirmDeleteOpen(false);
    setEventEditorOpen(true);
  }

  // Sauvegarde (Création ou Mise à jour)
  async function handleSaveEvent(e: React.FormEvent) {
    e.preventDefault();
    if (!familyId || !account) return;

    await run(async () => {
      let startAt: Date;
      let endAt: Date;

      if (eventIsAllDay) {
        startAt = new Date(eventDate + "T00:00:00");
        endAt = new Date(eventDate + "T23:59:59");
      } else {
        startAt = new Date(eventDate + "T" + eventStart + ":00");
        endAt = new Date(eventDate + "T" + eventEnd + ":00");
      }

      if (editingEvent) {
        await updateEvent(familyId, editingEvent.id, {
          title: eventTitle,
          description: eventDescription,
          startAt,
          endAt,
          isAllDay: eventIsAllDay,
          participantIds: eventParticipants,
          category: eventCategory,
          color: eventColor,
          location: eventLocation
        });
        setNotice("Rendez-vous modifié dans le calendrier partagé.");
      } else {
        await addEvent(familyId, account.uid, {
          title: eventTitle,
          description: eventDescription,
          startAt,
          endAt,
          isAllDay: eventIsAllDay,
          participantIds: eventParticipants,
          category: eventCategory,
          color: eventColor,
          location: eventLocation
        });
        setNotice("Rendez-vous ajouté au calendrier partagé.");
      }

      setEventEditorOpen(false);
      setEditingEvent(null);
    });
  }

  // Suppression d'un événement
  async function handleDeleteEvent() {
    if (!familyId || !editingEvent) return;
    await run(async () => {
      await deleteEvent(familyId, editingEvent.id);
      setConfirmDeleteOpen(false);
      setEventEditorOpen(false);
      setEditingEvent(null);
      setNotice("Rendez-vous supprimé du planning.");
    });
  }

  function handleNavigate(delta: number) {
    setSelectedDate(prev => navigateDate(prev, viewMode, delta));
  }

  function handleTodayClick() {
    setSelectedDate(todayInParis());
  }

  function toggleParticipant(memberId: string) {
    setSelectedParticipantId(prev => (prev === memberId ? null : memberId));
  }

  function toggleParticipantInEditor(memberId: string) {
    setEventParticipants(prev =>
      prev.includes(memberId) ? prev.filter(id => id !== memberId) : [...prev, memberId]
    );
  }

  function parseInvitation(raw: string) {
    const v = raw.trim();
    if (!v) return;
    try {
      const url = new URL(v);
      const token = url.searchParams.get("token") || url.searchParams.get("invite");
      if (token) {
        setInviteToken(token);
        return;
      }
    } catch { }
    setInviteToken(v);
  }

  function copyLink() {
    navigator.clipboard.writeText(generatedLink)
      .then(() => setNotice("Lien d'invitation copié."))
      .catch(() => setNotice("Sélectionnez et copiez le lien affiché."));
  }

  // Écran non configuré
  if (!configured) {
    return (
      <div className="screen">
        <div className="panel compact">
          <div className="logo"><CalendarDays size={34} /></div>
          <h1>FamillePlanning</h1>
          <p className="muted">Application Web connectée à Firebase</p>
          <p>
            Configurez les variables <code>VITE_FIREBASE_API_KEY</code>, <code>VITE_FIREBASE_AUTH_DOMAIN</code> et <code>VITE_FIREBASE_APP_ID</code>.
          </p>
        </div>
      </div>
    );
  }

  // Chargement
  if (!ready) {
    return (
      <div className="screen center">
        <div className="loader" />
        <span style={{ fontSize: "14px", fontWeight: 600, color: "#636E72" }}>Chargement sécurisé...</span>
      </div>
    );
  }

  // Connexion requise
  if (!account) {
    return (
      <div className="screen center">
        <div className="panel compact">
          <div className="logo"><CalendarDays size={32} /></div>
          <h1>FamillePlanning</h1>
          <p className="muted">Votre tribu, vos rendez-vous, tout au même endroit.</p>
          {inviteToken && <p className="highlight">Invitation reçue ! Connectez-vous pour rejoindre votre famille.</p>}
          <button className="button primary full" onClick={googleLogin} disabled={busy}>
            Continuer avec Google
          </button>
          {!emailMode && (
            <div className="two-buttons">
              <button className="button secondary" onClick={() => setEmailMode("login")}>Connexion e-mail</button>
              <button className="button secondary" onClick={() => setEmailMode("signup")}>Créer un compte</button>
            </div>
          )}
          {emailMode && (
            <form className="stack" onSubmit={loginEmail}>
              <label>
                Adresse e-mail
                <input type="email" autoComplete="email" required value={email} onChange={e => setEmail(e.target.value)} />
              </label>
              <label>
                Mot de passe
                <input
                  type="password"
                  autoComplete={emailMode === "signup" ? "new-password" : "current-password"}
                  minLength={6}
                  required
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                />
              </label>
              <button disabled={busy} className="button primary full" type="submit">
                {emailMode === "signup" ? "Créer mon compte adulte" : "Me connecter"}
              </button>
              <button type="button" className="link" onClick={() => setEmailMode(null)}>Retour</button>
            </form>
          )}
          <p className="footnote">
            Les enfants ont leur profil de planning sans compte. Les connexions e-mail ou Google sont réservées aux adultes.
          </p>
          {notice && <NoticeBanner text={notice} onDismiss={() => setNotice("")} />}
        </div>
      </div>
    );
  }

  // Écran d'invitation
  if (inviteToken) {
    return (
      <div className="screen center">
        <div className="panel compact">
          <div className="logo mint"><Link2 size={30} /></div>
          <h1>Rejoindre une famille</h1>
          <p className="muted">Connecté : {account.email}</p>
          {preview ? (
            <>
              <div className="highlight">
                <b>{preview.familyName}</b><br />
                Vous rejoindrez ce foyer comme {preview.role === "parent" ? "second parent titulaire" : "adulte invité"}.
              </div>
              {!account.emailVerified && (
                <p className="error">Adresse e-mail non vérifiée. Ouvrez le message de validation reçu.</p>
              )}
              <form onSubmit={accept} className="stack">
                <label>
                  Votre prénom
                  <input required minLength={2} maxLength={60} value={acceptName} onChange={e => setAcceptName(e.target.value)} />
                </label>
                <label>
                  Couleur personnelle
                  <ColorsPicker value={acceptColor} onChange={setAcceptColor} />
                </label>
                <button className="button primary full" disabled={busy || !account.emailVerified}>
                  Rejoindre {preview.familyName}
                </button>
              </form>
            </>
          ) : (
            <p className="muted">Recherche de l'invitation dans Firebase…</p>
          )}
          <button className="link" onClick={() => { setInviteToken(""); window.history.replaceState({}, "", window.location.pathname); }}>
            Annuler
          </button>
          {notice && <NoticeBanner text={notice} onDismiss={() => setNotice("")} />}
        </div>
      </div>
    );
  }

  // Écran de création du premier foyer si absent
  if (!familyId) {
    return (
      <div className="screen center">
        <div className="panel compact">
          <div className="logo"><Users size={34} /></div>
          <h1>Créons votre famille</h1>
          <p className="muted">Le premier adulte devient parent titulaire. Vous pouvez ensuite inviter un second parent.</p>
          {!account.emailVerified && (
            <div className="highlight">
              <b>Vérifiez votre adresse e-mail</b>
              <p>Consultez le message envoyé à {account.email}. La création est réservée aux comptes vérifiés.</p>
              <button
                className="button secondary"
                onClick={() => run(async () => {
                  await reload(account);
                  setNotice(account.emailVerified ? "Adresse validée !" : "Adresse toujours non vérifiée.");
                })}
              >
                J'ai vérifié mon e-mail
              </button>
            </div>
          )}
          <form className="stack" onSubmit={createFamily}>
            <label>
              Nom du foyer
              <input required minLength={2} maxLength={80} placeholder="Notre tribu" value={setupName} onChange={e => setSetupName(e.target.value)} />
            </label>
            <label>
              Votre prénom
              <input required minLength={2} maxLength={60} value={setupFirstName} onChange={e => setSetupFirstName(e.target.value)} />
            </label>
            <label>
              Votre couleur
              <ColorsPicker value={setupColor} onChange={setSetupColor} />
            </label>
            <button className="button primary full" disabled={busy || !account.emailVerified} type="submit">
              Créer ma famille
            </button>
          </form>
          <div className="divider">ou</div>
          <form
            onSubmit={e => {
              e.preventDefault();
              const v = (e.currentTarget.elements.namedItem("invite") as HTMLInputElement)?.value;
              parseInvitation(v);
            }}
            className="stack"
          >
            <label>
              Code ou lien d'invitation
              <input name="invite" required placeholder="Coller un lien reçu" />
            </label>
            <button className="button secondary full">Rejoindre une famille</button>
          </form>
          <button className="link" onClick={() => auth && signOut(auth)}>Changer de compte</button>
          {notice && <NoticeBanner text={notice} onDismiss={() => setNotice("")} />}
        </div>
      </div>
    );
  }

  // ==========================================
  // APPLICATION PRINCIPALE
  // ==========================================
  const todayIso = todayInParis();

  return (
    <div className="app">
      {/* Barre supérieure */}
      <header className="topbar">
        <div className="brand">
          <span className="brand-icon"><CalendarDays size={22} /></span>
          <div>
            <b>FamillePlanning</b>
            <small>{family?.name || "Votre foyer"}</small>
          </div>
        </div>
        <div className="topbar-right">
          <span className="email">{account.displayName || account.email}</span>
          <button title="Actualiser le foyer" className="icon-button" onClick={() => refreshProfile(account)}>
            <RefreshCw size={17} />
          </button>
          <button title="Déconnexion" className="icon-button" onClick={() => auth && signOut(auth)}>
            <LogOut size={17} />
          </button>
        </div>
      </header>

      <main className="main">
        {/* Menu latéral (Desktop) */}
        <aside className="sidebar">
          <div className="family-panel">
            <b>{family?.name || "Ma famille"}</b>
            <small>{members.length} membre(s) dans le foyer</small>
          </div>
          <button className={"sidebar-button " + (tab === "planning" ? "active" : "")} onClick={() => setTab("planning")}>
            <CalendarDays size={18} />
            Planning
          </button>
          <button className={"sidebar-button " + (tab === "family" ? "active" : "")} onClick={() => setTab("family")}>
            <Users size={18} />
            Ma famille
          </button>
          <div className="sidebar-footer">
            <ShieldCheck size={16} />
            <span>Données privées synchronisées</span>
          </div>
        </aside>

        {/* Contenu principal */}
        <section className="content">
          {notice && <NoticeBanner text={notice} onDismiss={() => setNotice("")} />}

          {tab === "planning" ? (
            <>
              {/* En-tête calendrier harmonisé avec FamilyHeader Android */}
              <div className="family-header-card">
                <div className="header-top-row">
                  <div>
                    <h1 className="family-title">{family?.name || "Notre planning"}</h1>
                    <p className="family-date-label">
                      {viewMode === "week" ? formatDayMonthFrench(selectedDate) : formatMonthYearFrench(selectedDate)}
                    </p>
                  </div>
                  <div className="header-actions">
                    <button className="button secondary" onClick={handleTodayClick} title="Revenir à aujourd'hui">
                      <CalendarIcon size={16} />
                      Aujourd'hui
                    </button>
                    <button className="button primary hidden md:inline-flex" onClick={openCreateEventDialog}>
                      <Plus size={18} />
                      Ajouter un événement
                    </button>
                  </div>
                </div>

                <div className="header-bottom-row">
                  {/* Bascule Semaine / Mois */}
                  <div className="view-mode-toggle">
                    <button
                      className={"view-mode-btn " + (viewMode === "week" ? "active" : "")}
                      onClick={() => setViewMode("week")}
                    >
                      Semaine
                    </button>
                    <button
                      className={"view-mode-btn " + (viewMode === "month" ? "active" : "")}
                      onClick={() => setViewMode("month")}
                    >
                      Mois
                    </button>
                  </div>

                  {/* Navigation Précédent / Suivant */}
                  <div className="nav-arrows">
                    <button className="arrow-btn" onClick={() => handleNavigate(-1)} title="Précédent">
                      <ChevronLeft size={20} />
                    </button>
                    <button className="arrow-btn" onClick={() => handleNavigate(1)} title="Suivant">
                      <ChevronRight size={20} />
                    </button>
                  </div>
                </div>
              </div>

              {/* Vue Semaine : Bandeau des 7 jours */}
              {viewMode === "week" && (
                <div className="week-strip">
                  {weekDays.map(dayIso => {
                    const isSelected = dayIso === selectedDate;
                    const isToday = dayIso === todayIso;
                    const [y, m, d] = dayIso.split("-").map(Number);
                    const dateObj = new Date(Date.UTC(y, m - 1, d));
                    const dayName = new Intl.DateTimeFormat("fr-FR", { weekday: "short" }).format(dateObj).slice(0, 3);
                    const hasEvents = (eventsByDay.get(dayIso)?.length || 0) > 0;

                    return (
                      <div
                        key={dayIso}
                        onClick={() => setSelectedDate(dayIso)}
                        className={`day-badge ${isSelected ? "selected" : ""} ${isToday ? "today" : ""}`}
                      >
                        <span className="day-name">{dayName}</span>
                        <span className="day-num">{d}</span>
                        {hasEvents && <span className="day-dot" />}
                      </div>
                    );
                  })}
                </div>
              )}

              {/* Vue Mois : Grille du calendrier */}
              {viewMode === "month" && (
                <div className="month-calendar-card">
                  <div className="month-weekdays-header">
                    <span>Lun</span>
                    <span>Mar</span>
                    <span>Mer</span>
                    <span>Jeu</span>
                    <span>Ven</span>
                    <span>Sam</span>
                    <span>Dim</span>
                  </div>
                  <div className="month-grid">
                    {monthGrid.map(cell => {
                      const isSelected = cell.dateIso === selectedDate;
                      const isToday = cell.dateIso === todayIso;
                      const dayEventsColors = eventsByDay.get(cell.dateIso) || [];

                      return (
                        <div
                          key={cell.dateIso}
                          onClick={() => setSelectedDate(cell.dateIso)}
                          className={`month-cell ${cell.isCurrentMonth ? "" : "other-month"} ${isSelected ? "selected" : ""} ${isToday ? "today" : ""}`}
                        >
                          <span className="month-day-number">{cell.dayNumber}</span>
                          {dayEventsColors.length > 0 && (
                            <div className="month-events-dots">
                              {dayEventsColors.slice(0, 3).map((col, idx) => (
                                <span key={idx} className="month-event-dot" style={{ background: col }} />
                              ))}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* Filtres par Membre (Member Chips) */}
              <div className="member-filters-row">
                <button
                  type="button"
                  onClick={() => setSelectedParticipantId(null)}
                  className={`member-chip ${selectedParticipantId === null ? "selected" : ""}`}
                  style={{
                    borderColor: selectedParticipantId === null ? DEFAULT_CORAL : "#EFE9DE",
                    background: selectedParticipantId === null ? DEFAULT_CORAL + "18" : "#FFFFFF"
                  }}
                >
                  <span className="chip-circle" style={{ background: DEFAULT_CORAL }} />
                  <span>Tous</span>
                </button>

                {members.map(m => {
                  const isSelected = selectedParticipantId === m.id;
                  return (
                    <button
                      key={m.id}
                      type="button"
                      onClick={() => toggleParticipant(m.id)}
                      className={`member-chip ${isSelected ? "selected" : ""}`}
                      style={{
                        borderColor: isSelected ? m.color : "#EFE9DE",
                        background: isSelected ? m.color + "20" : "#FFFFFF"
                      }}
                    >
                      <span className="chip-circle" style={{ background: m.color }} />
                      <span>{m.firstName}</span>
                    </button>
                  );
                })}
              </div>

              {/* Titre de la journée choisie */}
              <h2 className="day-title" style={{ fontSize: "17px", fontWeight: 800, margin: "8px 0 14px", textTransform: "capitalize" }}>
                {formatDayMonthFrench(selectedDate)}
              </h2>

              {/* Liste des événements (EventCards conformes à EventCard.kt) */}
              {visibleEvents.length === 0 ? (
                <div className="empty-state">
                  <CalendarDays size={36} color="#A4B0BE" />
                  <b>Aucun rendez-vous</b>
                  <p>Appuyez sur « Ajouter un événement » pour planifier une activité.</p>
                  <button className="button primary" style={{ marginTop: "8px" }} onClick={openCreateEventDialog}>
                    <Plus size={16} /> Planifier un événement
                  </button>
                </div>
              ) : (
                <div className="event-list">
                  {visibleEvents.map(event => {
                    const eventColor = event.color || DEFAULT_CORAL;
                    const catObj = CATEGORIES.find(c => c.id === event.category);
                    const categoryLabel = catObj ? catObj.label : "Activité";
                    const timeRange = event.isAllDay
                      ? "Toute la journée"
                      : `${formatTime(event.startAt)} - ${formatTime(event.endAt)}`;

                    return (
                      <article
                        key={event.id}
                        className="event-card"
                        onClick={() => openEditEventDialog(event)}
                        title="Cliquer pour modifier ou supprimer"
                      >
                        {/* Bandeau latéral coloré (8px) */}
                        <div className="event-side-bar" style={{ background: eventColor }} />

                        <div className="event-body">
                          <div className="event-top-meta">
                            <div className="event-time-badge" style={{ color: eventColor }}>
                              <Clock size={14} />
                              <span>{timeRange}</span>
                            </div>
                            <span
                              className="event-category-pill"
                              style={{ background: eventColor + "1E", color: eventColor }}
                            >
                              {categoryLabel}
                            </span>
                          </div>

                          <h3 className="event-title">{event.title}</h3>

                          {event.location && (
                            <div className="event-location">
                              <MapPin size={13} />
                              <span>{event.location}</span>
                            </div>
                          )}

                          {event.description && (
                            <p className="event-description">{event.description}</p>
                          )}

                          {/* Puces des participants */}
                          {event.participantIds.length > 0 && (
                            <div className="event-participants-row">
                              {event.participantIds.map(pId => {
                                const memberObj = members.find(m => m.id === pId);
                                if (!memberObj) return null;
                                return (
                                  <span
                                    key={pId}
                                    className="participant-pill"
                                    style={{
                                      background: memberObj.color + "22",
                                      color: memberObj.color
                                    }}
                                  >
                                    {memberObj.firstName}
                                  </span>
                                );
                              })}
                            </div>
                          )}
                        </div>
                      </article>
                    );
                  })}
                </div>
              )}
            </>
          ) : (
            /* Onglet Ma famille */
            <>
              <div className="section-head">
                <div>
                  <p className="eyebrow">VOTRE TRIBU</p>
                  <h1>Ma famille</h1>
                  <p className="muted">
                    {isParent ? "Vous êtes parent titulaire de ce foyer." : "Vous faites partie de cette famille."}
                  </p>
                </div>
              </div>

              {isParent && (
                <div className="actions">
                  <button className="button primary" onClick={() => setChildOpen(true)}>
                    <Baby size={18} />
                    Ajouter un enfant
                  </button>
                  <button className="button secondary" onClick={() => setInviteOpen(true)}>
                    <Link2 size={18} />
                    Inviter un adulte
                  </button>
                </div>
              )}

              <div className="tile">
                <ShieldCheck size={20} />
                <div>
                  <b>Foyer sécurisé et partagé</b>
                  <p>1 ou 2 parents titulaires. Les enfants apparaissent uniquement dans le planning sans compte personnel.</p>
                </div>
              </div>

              <div className="family-members-grid">
                {members.map(m => (
                  <div className="person-card" key={m.id}>
                    <EmojiAvatar name={m.firstName} color={m.color} size={48} />
                    <div className="person-info">
                      <b>{m.firstName}</b>
                      <small>{m.roleLabel}</small>
                      {m.isChild ? (
                        <span className="person-badge">Enfant · sans compte</span>
                      ) : (
                        <span className="person-badge">Adulte</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>

              <p className="footnote">
                {parentsCount}/2 parent(s) titulaire(s). Le lien d'invitation permet d'ajouter un second parent ou un proche.
              </p>

              {generatedLink && (
                <div className="link-panel">
                  <h3 style={{ margin: "0 0 6px", fontSize: "16px", fontWeight: 800 }}>Invitation prête</h3>
                  <p className="muted" style={{ fontSize: "13px" }}>Lien privé valable 48h et utilisable une seule fois.</p>
                  <input readOnly value={generatedLink} onFocus={e => e.target.select()} />
                  <button className="button secondary" style={{ marginTop: "10px" }} onClick={copyLink}>
                    <Copy size={16} /> Copier le lien
                  </button>
                </div>
              )}
            </>
          )}
        </section>
      </main>

      {/* Bouton d'action flottant (FAB Mobile) */}
      <button className="mobile-fab" onClick={openCreateEventDialog} title="Ajouter un événement">
        <Plus size={28} />
      </button>

      {/* Barre de navigation basse (Mobile Web) */}
      <nav className="mobile-nav">
        <button className={tab === "planning" ? "active" : ""} onClick={() => setTab("planning")}>
          <CalendarDays size={20} />
          Planning
        </button>
        <button className={tab === "family" ? "active" : ""} onClick={() => setTab("family")}>
          <Users size={20} />
          Ma famille
        </button>
      </nav>

      {/* =========================================
          MODALE CRÉATION / MODIFICATION D'ÉVÉNEMENT
          ========================================= */}
      {eventEditorOpen && (
        <div className="overlay">
          <form className="modal" onSubmit={handleSaveEvent}>
            <div className="modal-header">
              <h2>
                <CalendarPlus size={22} color={eventColor} />
                {editingEvent ? "Modifier le rendez-vous" : "Nouveau rendez-vous"}
              </h2>
              <button
                type="button"
                className="icon-button"
                style={{ width: "32px", height: "32px" }}
                onClick={() => setEventEditorOpen(false)}
              >
                <X size={18} />
              </button>
            </div>

            <label>
              Titre de l'événement *
              <input
                required
                minLength={2}
                maxLength={100}
                placeholder="Ex : Dentiste, Match de judo, Réunion parents..."
                value={eventTitle}
                onChange={e => setEventTitle(e.target.value)}
              />
            </label>

            <label>
              Date
              <input
                type="date"
                required
                value={eventDate}
                onChange={e => setEventDate(e.target.value)}
              />
            </label>

            <label className="checkbox-row">
              <input
                type="checkbox"
                checked={eventIsAllDay}
                onChange={e => setEventIsAllDay(e.target.checked)}
              />
              <span>Toute la journée</span>
            </label>

            {!eventIsAllDay && (
              <div className="two-buttons">
                <label>
                  Heure de début
                  <input
                    type="time"
                    required
                    value={eventStart}
                    onChange={e => setEventStart(e.target.value)}
                  />
                </label>
                <label>
                  Heure de fin
                  <input
                    type="time"
                    required
                    value={eventEnd}
                    onChange={e => setEventEnd(e.target.value)}
                  />
                </label>
              </div>
            )}

            <label>
              Catégorie
              <select value={eventCategory} onChange={e => setEventCategory(e.target.value)}>
                {CATEGORIES.map(c => (
                  <option key={c.id} value={c.id}>{c.label}</option>
                ))}
              </select>
            </label>

            <label>
              Lieu (facultatif)
              <input
                placeholder="Ex : Gymnase, Cabinet médical, École..."
                value={eventLocation}
                onChange={e => setEventLocation(e.target.value)}
              />
            </label>

            <label>
              Description (facultative)
              <textarea
                rows={2}
                placeholder="Notes utiles, matériel à apporter..."
                value={eventDescription}
                onChange={e => setEventDescription(e.target.value)}
              />
            </label>

            <label>Couleur de l'événement</label>
            <ColorsPicker value={eventColor} onChange={setEventColor} />

            <label>Qui participe ?</label>
            <div className="participants-select-grid">
              {members.map(m => {
                const isChecked = eventParticipants.includes(m.id);
                return (
                  <div
                    key={m.id}
                    onClick={() => toggleParticipantInEditor(m.id)}
                    className={`participant-select-chip ${isChecked ? "checked" : ""}`}
                  >
                    <span className="chip-circle" style={{ background: m.color }} />
                    <span>{m.firstName}</span>
                  </div>
                );
              })}
            </div>

            <div className="dialog-actions">
              <button
                type="button"
                className="button secondary"
                onClick={() => setEventEditorOpen(false)}
              >
                Annuler
              </button>

              {editingEvent && (
                <button
                  type="button"
                  className="button danger"
                  onClick={() => setConfirmDeleteOpen(true)}
                  title="Supprimer ce rendez-vous"
                >
                  <Trash2 size={16} /> Supprimer
                </button>
              )}

              <button disabled={busy} className="button primary" type="submit">
                {editingEvent ? "Modifier" : "Enregistrer"}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Confirmation de suppression */}
      {confirmDeleteOpen && (
        <div className="overlay" style={{ zIndex: 100 }}>
          <div className="panel compact">
            <AlertCircle size={36} color="#EE5253" style={{ margin: "0 auto" }} />
            <h2 style={{ fontSize: "18px", fontWeight: 800, margin: "4px 0" }}>Supprimer ce rendez-vous ?</h2>
            <p className="muted" style={{ fontSize: "13px" }}>
              Cette action retirera définitivement « {editingEvent?.title} » du calendrier familial partagé.
            </p>
            <div className="two-buttons" style={{ marginTop: "10px" }}>
              <button className="button secondary" onClick={() => setConfirmDeleteOpen(false)}>
                Annuler
              </button>
              <button disabled={busy} className="button danger" onClick={handleDeleteEvent}>
                Oui, supprimer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modale d'ajout d'enfant */}
      {childOpen && (
        <div className="overlay">
          <form className="modal" onSubmit={createChild}>
            <div className="modal-header">
              <h2>Ajouter un enfant</h2>
              <button type="button" className="icon-button" style={{ width: "32px", height: "32px" }} onClick={() => setChildOpen(false)}>
                <X size={18} />
              </button>
            </div>
            <p className="muted" style={{ fontSize: "13px" }}>
              Un prénom et une couleur suffisent. Aucun compte ni e-mail personnel.
            </p>
            <label>
              Prénom *
              <input required minLength={2} maxLength={60} value={childName} onChange={e => setChildName(e.target.value)} />
            </label>
            <label>
              Date de naissance (facultative)
              <input type="date" value={childBirth} onChange={e => setChildBirth(e.target.value)} />
            </label>
            <label>Couleur</label>
            <ColorsPicker value={childColor} onChange={setChildColor} />
            <div className="dialog-actions">
              <button type="button" className="button secondary" onClick={() => setChildOpen(false)}>Annuler</button>
              <button disabled={busy} className="button primary">Ajouter l'enfant</button>
            </div>
          </form>
        </div>
      )}

      {/* Modale d'invitation d'adulte */}
      {inviteOpen && (
        <div className="overlay">
          <form className="modal" onSubmit={shareInvite}>
            <div className="modal-header">
              <h2>Inviter un adulte</h2>
              <button type="button" className="icon-button" style={{ width: "32px", height: "32px" }} onClick={() => setInviteOpen(false)}>
                <X size={18} />
              </button>
            </div>
            <p className="muted" style={{ fontSize: "13px" }}>
              Lien sécurisé à usage unique valable 48h.
            </p>
            <div className="two-buttons">
              <button
                type="button"
                onClick={() => setInviteRole("adult")}
                className={`button ${inviteRole === "adult" ? "primary" : "secondary"}`}
              >
                Adulte invité
              </button>
              <button
                type="button"
                disabled={parentsCount >= 2}
                onClick={() => setInviteRole("parent")}
                className={`button ${inviteRole === "parent" ? "primary" : "secondary"}`}
              >
                Deuxième parent
              </button>
            </div>
            <label>
              Réserver à une adresse e-mail (facultatif)
              <input type="email" value={inviteEmail} onChange={e => setInviteEmail(e.target.value)} />
            </label>
            <div className="dialog-actions">
              <button type="button" className="button secondary" onClick={() => setInviteOpen(false)}>Annuler</button>
              <button disabled={busy} className="button primary">Générer le lien</button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
