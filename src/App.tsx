import React, {useEffect, useMemo, useState} from "react";
import {
  CalendarDays, Users, Plus, Link2, Copy, Check, LogOut, RefreshCw,
  ShieldCheck, Baby, Mail, ArrowRight, CalendarPlus, Clock, LockKeyhole
} from "lucide-react";
import {
  GoogleAuthProvider, onAuthStateChanged, signInWithPopup,
  signInWithEmailAndPassword, createUserWithEmailAndPassword,
  sendEmailVerification, reload, signOut, type User
} from "firebase/auth";
import {auth, configured} from "./firebase";
import {
  readProfile, createFirstFamily, getHome, getRole, observeMembers, observeEvents,
  addChild, createInvite, previewInvite, acceptInvite, addEvent, friendlyError,
  type Family, type Member, type Event
} from "./familyService";
import type {Unsubscribe} from "firebase/firestore";

const colors=["#FF6B6B","#48DBFB","#1DD1A1","#FECA57","#A55EEA","#FF9F43"];
const today=()=>new Date().toLocaleDateString("en-CA");
const dateLabel=(date:Date)=>new Intl.DateTimeFormat("fr-FR",{weekday:"long",day:"numeric",month:"long"}).format(date);
const timeLabel=(date:Date)=>new Intl.DateTimeFormat("fr-FR",{hour:"2-digit",minute:"2-digit"}).format(date);
const parentRoles=["owner","parent","admin"];
const COLOR="#FF6B6B";

function EmojiAvatar({name,color,size=42}:{name:string,color:string,size?:number}) {
  return <span className="avatar" style={{width:size,height:size,background:color+"22",color,borderColor:color}}>{name.slice(0,1).toLocaleUpperCase("fr")}</span>;
}
function Colors({value,onChange}:{value:string,onChange:(v:string)=>void}) {
  return <div className="color-row">{colors.map(c=><button key={c} type="button" title={"Choisir "+c}
    onClick={()=>onChange(c)} className={"color-dot "+(value===c?"selected":"")} style={{background:c}} />)}</div>;
}
function Notice({text,onDismiss}:{text:string,onDismiss:()=>void}) {
  return <div className="notice" role="status"><p>{text}</p><button onClick={onDismiss} aria-label="Fermer">×</button></div>;
}

export default function App() {
  const [account,setAccount]=useState<User|null>(null);
  const [ready,setReady]=useState(false);
  const [familyId,setFamilyId]=useState<string|null>(null);
  const [family,setFamily]=useState<Family|null>(null);
  const [role,setRole]=useState("");
  const [members,setMembers]=useState<Member[]>([]);
  const [events,setEvents]=useState<Event[]>([]);
  const [tab,setTab]=useState<"planning"|"family">("planning");
  const [busy,setBusy]=useState(false);
  const [notice,setNotice]=useState("");
  const [inviteToken,setInviteToken]=useState(()=>new URLSearchParams(window.location.search).get("invite")||"");
  const [preview,setPreview]=useState<{familyName:string,role:string,expiresAt:string}|null>(null);
  const [emailMode,setEmailMode]=useState<"login"|"signup"|null>(null);
  const [email,setEmail]=useState("");
  const [password,setPassword]=useState("");
  const [acceptName,setAcceptName]=useState("");
  const [acceptColor,setAcceptColor]=useState(COLOR);
  const [setupName,setSetupName]=useState("");
  const [setupFirstName,setSetupFirstName]=useState("");
  const [setupColor,setSetupColor]=useState(COLOR);
  const [childOpen,setChildOpen]=useState(false);
  const [childName,setChildName]=useState("");
  const [childBirth,setChildBirth]=useState("");
  const [childColor,setChildColor]=useState("#A55EEA");
  const [inviteOpen,setInviteOpen]=useState(false);
  const [inviteRole,setInviteRole]=useState<"parent"|"adult">("adult");
  const [inviteEmail,setInviteEmail]=useState("");
  const [generatedLink,setGeneratedLink]=useState("");
  const [day,setDay]=useState(today());
  const [eventOpen,setEventOpen]=useState(false);
  const [eventTitle,setEventTitle]=useState("");
  const [eventStart,setEventStart]=useState("17:00");
  const [eventEnd,setEventEnd]=useState("18:00");
  const [eventLocation,setEventLocation]=useState("");
  const [participants,setParticipants]=useState<string[]>([]);

  async function refreshProfile(user:User) {
    const profile=await readProfile(user.uid);
    setFamilyId(profile.currentFamilyId);
    return profile;
  }
  useEffect(()=>{
    if (!configured || !auth) {setReady(true);return;}
    return onAuthStateChanged(auth,async user=>{
      setAccount(user);
      setFamilyId(null);
      setReady(false);
      if(user){
        try {await refreshProfile(user);}
        catch(e){setNotice("Chargement du profil : "+friendlyError(e));}
      }
      setReady(true);
    });
  },[]);
  useEffect(()=>{
    if(!familyId || !account) {
      setFamily(null);setRole("");setMembers([]);setEvents([]);
      return;
    }
    let alive=true;
    const subs:Unsubscribe[]=[];
    Promise.all([getHome(familyId),getRole(familyId,account.uid)]).then(([home,accessRole])=>{
      if(alive){setFamily(home);setRole(accessRole || (home.ownerId===account.uid?"owner":"viewer"));}
    }).catch(e=>setNotice("Lecture de la famille : "+friendlyError(e)));
    subs.push(observeMembers(familyId,items=>{if(alive)setMembers(items);},e=>setNotice(friendlyError(e))));
    subs.push(observeEvents(familyId,items=>{if(alive)setEvents(items);},e=>setNotice(friendlyError(e))));
    return ()=>{alive=false;subs.forEach(unsub=>unsub());};
  },[account?.uid,familyId]);
  useEffect(()=>{
    if(!inviteToken || !account){setPreview(null);return;}
    let active=true;
    previewInvite(inviteToken).then(p=>{if(active)setPreview(p);})
      .catch(e=>{if(active){setPreview(null);setNotice("Invitation : "+friendlyError(e));}});
    return ()=>{active=false;};
  },[inviteToken,account?.uid]);

  const isParent=parentRoles.includes(role)||family?.ownerId===account?.uid;
  const parentsCount=members.filter(m=>parentRoles.includes(m.role)).length;
  const visibleEvents=useMemo(()=>{
    return events.filter(e=>new Date(e.startAt).toLocaleDateString("en-CA")===day)
      .sort((a,b)=>a.startAt.getTime()-b.startAt.getTime());
  },[events,day]);
  async function run(action:()=>Promise<void>) {
    if(busy)return;
    setBusy(true);setNotice("");
    try {await action();}
    catch(e){setNotice(friendlyError(e));}
    finally{setBusy(false);}
  }
  async function googleLogin() {
    await run(async()=>{
      if(!auth)throw new Error("Firebase Web non configuré.");
      await signInWithPopup(auth,new GoogleAuthProvider());
    });
  }
  async function loginEmail(e:React.FormEvent){
    e.preventDefault();
    await run(async()=>{
      if(!auth)throw new Error("Firebase Web non configuré.");
      if(emailMode==="signup"){
        const res=await createUserWithEmailAndPassword(auth,email,password);
        await sendEmailVerification(res.user);
        setNotice("Vérifiez votre e-mail avant d'accepter une invitation.");
      } else {
        await signInWithEmailAndPassword(auth,email,password);
      }
      setEmailMode(null);setPassword("");
    });
  }
  async function createFamily(e:React.FormEvent){
    e.preventDefault();
    if(!account)return;
    await run(async()=>{
      await reload(account);
      if (!account.emailVerified) {
        throw new Error("Vérifiez d'abord votre adresse e-mail avant de créer une famille.");
      }
      await createFirstFamily(account,setupName,setupFirstName,setupColor);
      await refreshProfile(account);
      setNotice("Votre famille est créée et synchronisée !");
    });
  }
  async function createChild(e:React.FormEvent){
    e.preventDefault();
    if(!familyId)return;
    await run(async()=>{
      await addChild(familyId,account!.uid,childName,childBirth||null,childColor);
      setChildOpen(false);setChildName("");setChildBirth("");
      setNotice("L'enfant apparaît dans la famille et dans les filtres du planning.");
    });
  }
  async function shareInvite(e:React.FormEvent){
    e.preventDefault();
    if(!familyId)return;
    await run(async()=>{
      const answer=await createInvite(familyId,inviteRole,inviteEmail||null);
      const url=new URL(window.location.origin+window.location.pathname);
      url.searchParams.set("invite",answer.token);
      setGeneratedLink(url.toString());
      setInviteOpen(false);setInviteEmail("");
    });
  }
  async function accept(e:React.FormEvent) {
    e.preventDefault();
    if(!account)return;
    await run(async()=>{
      await reload(account);
      if(!account.emailVerified)throw new Error("Vérifiez d'abord votre adresse e-mail, puis actualisez votre compte.");
      await acceptInvite(inviteToken,acceptName,acceptColor);
      setInviteToken("");setPreview(null);
      window.history.replaceState({}, "",window.location.pathname);
      await refreshProfile(account);
      setNotice("Bienvenue dans votre famille !");
    });
  }
  async function createEvent(e:React.FormEvent) {
    e.preventDefault();
    if(!familyId||!account)return;
    await run(async()=>{
      await addEvent(familyId,account.uid,{
        title:eventTitle,startAt:new Date(day+"T"+eventStart),
        endAt:new Date(day+"T"+eventEnd),
        participantIds:participants,location:eventLocation
      });
      setEventOpen(false);setEventTitle("");setEventLocation("");setParticipants([]);
      setNotice("Rendez-vous ajouté au calendrier partagé.");
    });
  }
  function parseInvitation(raw:string) {
    const v=raw.trim();
    if(!v)return;
    try {
      const url=new URL(v);
      const token=url.searchParams.get("token")||url.searchParams.get("invite");
      if(token){setInviteToken(token);return;}
    } catch {}
    setInviteToken(v);
  }
  function copyLink() {
    navigator.clipboard.writeText(generatedLink).then(()=>setNotice("Lien d'invitation copié."))
      .catch(()=>setNotice("Sélectionnez et copiez le lien affiché."));
  }

  if(!configured) return <div className="screen"><div className="panel compact">
    <div className="logo"><CalendarDays size={34}/></div>
    <h1>FamillePlanning</h1><p className="muted">Application Web en préparation</p>
    <p>Le simulateur a été remplacé par la version Firebase réelle. Pour l'activer, configurez
      <code>VITE_FIREBASE_API_KEY</code>, <code>VITE_FIREBASE_AUTH_DOMAIN</code> et <code>VITE_FIREBASE_APP_ID</code> avec les paramètres de l'application <b>Web</b> dans Firebase.</p>
    <p className="muted">Le projet Firebase et la base nommée sont déjà configurés dans le code. Aucune fausse donnée n'est affichée.</p>
  </div></div>;

  if(!ready) return <div className="screen center"><div className="loader"/>Chargement sécurisé...</div>;

  if(!account) return <div className="screen center">
    <div className="panel compact">
      <div className="logo"><CalendarDays size={32}/></div>
      <h1>FamillePlanning</h1>
      <p className="muted">Votre tribu, vos rendez-vous, tout au même endroit.</p>
      {inviteToken&&<p className="highlight">Invitation reçue ! Connectez-vous pour rejoindre votre famille.</p>}
      <button className="button primary full" onClick={googleLogin} disabled={busy}>Continuer avec Google</button>
      {!emailMode&&<div className="two-buttons">
        <button className="button secondary" onClick={()=>setEmailMode("login")}>Connexion e-mail</button>
        <button className="button secondary" onClick={()=>setEmailMode("signup")}>Créer un compte</button>
      </div>}
      {emailMode&&<form className="stack" onSubmit={loginEmail}>
        <label>Adresse e-mail<input type="email" autoComplete="email" required value={email} onChange={e=>setEmail(e.target.value)}/></label>
        <label>Mot de passe<input type="password" autoComplete={emailMode==="signup"?"new-password":"current-password"}
          minLength={6} required value={password} onChange={e=>setPassword(e.target.value)}/></label>
        <button disabled={busy} className="button primary full" type="submit">{emailMode==="signup"?"Créer mon compte adulte":"Me connecter"}</button>
        <button type="button" className="link" onClick={()=>setEmailMode(null)}>Retour</button>
      </form>}
      <p className="footnote">Les enfants ont leur profil de planning, sans compte de connexion. Les comptes e-mail sont destinés aux adultes.</p>
      {notice&&<Notice text={notice} onDismiss={()=>setNotice("")}/>}
    </div>
  </div>;

  if(inviteToken) return <div className="screen center">
    <div className="panel compact">
      <div className="logo mint"><Link2 size={30}/></div>
      <h1>Rejoindre une famille</h1>
      <p className="muted">Connecté : {account.email}</p>
      {preview ? <>
        <div className="highlight"><b>{preview.familyName}</b><br/>Vous rejoindrez ce foyer comme {preview.role==="parent"?"second parent titulaire":"adulte invité"}.</div>
        {!account.emailVerified&&<p className="error">Adresse e-mail non vérifiée. Ouvrez le message de vérification reçu ou reconnectez-vous après l'avoir validée.</p>}
        <a className="button secondary full" href={"familleplanning://invite?token="+encodeURIComponent(inviteToken)}>
          Ouvrir dans l'application Android
        </a>
        <form onSubmit={accept} className="stack">
          <label>Votre prénom<input required minLength={2} maxLength={60} value={acceptName} onChange={e=>setAcceptName(e.target.value)}/></label>
          <label>Couleur personnelle<Colors value={acceptColor} onChange={setAcceptColor}/></label>
          <button className="button primary full" disabled={busy||!account.emailVerified}>Rejoindre {preview.familyName}</button>
        </form>
      </>:<p className="muted">Recherche de l'invitation dans Firebase…</p>}
      <button className="link" onClick={()=>{setInviteToken("");window.history.replaceState({},"",window.location.pathname);}}>Annuler</button>
      {notice&&<Notice text={notice} onDismiss={()=>setNotice("")}/>}
    </div>
  </div>;

  if(!familyId) return <div className="screen center">
    <div className="panel compact">
      <div className="logo"><Users size={34}/></div>
      <h1>Créons votre famille</h1>
      <p className="muted">Le premier adulte devient parent titulaire. Vous pouvez ensuite inviter un deuxième parent, quel que soit le modèle familial.</p>
      {!account.emailVerified&&<div className="highlight">
        <b>Vérifiez votre adresse e-mail</b>
        <p>Consultez le message de validation envoyé à {account.email}. La création est réservée aux comptes vérifiés.</p>
        <button className="button secondary" onClick={()=>run(async()=>{
          await reload(account);
          setNotice(account.emailVerified?"Adresse validée. Vous pouvez continuer.":"Adresse toujours non vérifiée. Ouvrez votre e-mail de vérification.");
        })}>J'ai vérifié mon e-mail</button>
        <button className="link" onClick={()=>run(async()=>{await sendEmailVerification(account);setNotice("Un nouvel e-mail de vérification a été envoyé.");})}>
          Renvoyer l'e-mail
        </button>
      </div>}
      <form className="stack" onSubmit={createFamily}>
        <label>Nom du foyer<input required minLength={2} maxLength={80} placeholder="Notre tribu"
          value={setupName} onChange={e=>setSetupName(e.target.value)}/></label>
        <label>Votre prénom<input required minLength={2} maxLength={60}
          value={setupFirstName} onChange={e=>setSetupFirstName(e.target.value)}/></label>
        <label>Votre couleur<Colors value={setupColor} onChange={setSetupColor}/></label>
        <button className="button primary full" disabled={busy||!account.emailVerified} type="submit">Créer ma famille</button>
      </form>
      <div className="divider">ou</div>
      <form onSubmit={e=>{e.preventDefault(); const v=(e.currentTarget.elements.namedItem("invite") as HTMLInputElement)?.value;parseInvitation(v);}} className="stack">
        <label>Code ou lien d'invitation<input name="invite" required placeholder="Coller un lien reçu"/></label>
        <button className="button secondary full">Rejoindre une famille</button>
      </form>
      <button className="link" onClick={()=>auth&&signOut(auth)}>Changer de compte</button>
      {notice&&<Notice text={notice} onDismiss={()=>setNotice("")}/>}
    </div>
  </div>;

  return <div className="app">
    <header className="topbar"><div className="brand"><span className="brand-icon"><CalendarDays size={23}/></span>
      <div><b>FamillePlanning</b><small>{family?.name||"Votre famille"}</small></div>
    </div>
      <div className="topbar-right"><span className="email">{account.displayName||account.email}</span>
        <button title="Actualiser le foyer" className="icon-button" onClick={()=>refreshProfile(account)}><RefreshCw size={18}/></button>
        <button title="Déconnexion" className="icon-button" onClick={()=>auth&&signOut(auth)}><LogOut size={18}/></button></div>
    </header>
    <main className="main">
      <aside className="sidebar">
        <div className="family-panel"><Users size={19}/><b>{family?.name||"Ma famille"}</b><small>{members.length} membre(s)</small></div>
        <button className={"sidebar-button "+(tab==="planning"?"active":"")} onClick={()=>setTab("planning")}><CalendarDays size={19}/>Planning</button>
        <button className={"sidebar-button "+(tab==="family"?"active":"")} onClick={()=>setTab("family")}><Users size={19}/>Ma famille</button>
        <div className="sidebar-footer"><ShieldCheck size={17}/>Données privées et synchronisées</div>
      </aside>
      <section className="content">
        {notice&&<Notice text={notice} onDismiss={()=>setNotice("")}/>}
        {tab==="planning" ? <>
          <div className="section-head"><div><p className="eyebrow">LE PLANNING PARTAGÉ</p><h1>Notre calendrier</h1></div>
            <button className="button primary" onClick={()=>setEventOpen(true)}><Plus size={18}/>Ajouter un événement</button></div>
          <div className="toolbar"><label>Choisir une journée<input type="date" value={day} onChange={e=>setDay(e.target.value)}/></label>
            <button className="button secondary" onClick={()=>setDay(today())}>Aujourd'hui</button></div>
          <div className="member-strip">{members.map(m=><div key={m.id} className="member-mini">
            <EmojiAvatar name={m.firstName} color={m.color} size={36}/><span>{m.firstName}</span></div>)}</div>
          <h2 className="day-title">{dateLabel(new Date(day+"T12:00:00"))}</h2>
          {visibleEvents.length===0?<div className="empty"><CalendarDays size={38}/><b>Aucun rendez-vous</b><p>Ajoutez le premier événement de votre famille.</p></div>:
            <div className="event-list">{visibleEvents.map(e=><article key={e.id} className="event">
              <div className="event-time"><Clock size={15}/>{timeLabel(e.startAt)} · {timeLabel(e.endAt)}</div>
              <h3>{e.title}</h3>{e.location&&<p>{e.location}</p>}
              <div className="member-strip small">{e.participantIds.map(id=>{const m=members.find(m=>m.id===id);
                return m?<span className="pill" key={id} style={{background:m.color+"22",color:m.color}}>{m.firstName}</span>:null;})}</div>
            </article>)}</div>}
        </> : <>
          <div className="section-head"><div><p className="eyebrow">VOTRE TRIBU</p><h1>Ma famille</h1>
            <p className="muted">{parentRoles.includes(role)?"Vous êtes parent titulaire.":"Vous faites partie de cette famille."}</p>
            </div>
          </div>
          {isParent&&<div className="actions"><button className="button primary" onClick={()=>setChildOpen(true)}><Baby size={18}/>Ajouter un enfant</button>
            <button className="button secondary" onClick={()=>setInviteOpen(true)}><Link2 size={18}/>Inviter un adulte</button></div>}
          <div className="tile"><ShieldCheck size={20}/><div><b>Famille protégée</b><p>1 ou 2 parents titulaires. Les enfants ne se connectent pas et apparaissent uniquement dans votre planning.</p></div></div>
          <div className="cards">{members.map(m=><div className="person-card" key={m.id}>
            <EmojiAvatar name={m.firstName} color={m.color} size={52}/><div><b>{m.firstName}</b><small>{m.roleLabel}</small>
              {m.isChild&&<small>Sans compte de connexion</small>}</div>
          </div>)}</div>
          <p className="footnote">{parentsCount}/2 parent(s) titulaire(s). La création d'un deuxième parent passe par une invitation sécurisée.</p>
          {generatedLink&&<div className="panel link-panel"><h3>Invitation à partager</h3>
            <p className="muted">Lien privé valable 48 h et utilisable une seule fois.</p>
            <input readOnly value={generatedLink} onFocus={e=>e.target.select()}/>
            <button className="button secondary" onClick={copyLink}><Copy size={17}/>Copier le lien</button>
            {window.location.hostname==="localhost"&&<p className="error">Le site tourne en local : ce lien ne pourra pas être ouvert par quelqu'un d'autre avant publication sur un vrai domaine.</p>}
          </div>}
        </>}
      </section>
    </main>
    <nav className="mobile-nav"><button className={tab==="planning"?"active":""} onClick={()=>setTab("planning")}><CalendarDays size={22}/>Planning</button>
      <button className={tab==="family"?"active":""} onClick={()=>setTab("family")}><Users size={22}/>Ma famille</button></nav>

    {childOpen&&<div className="overlay"><form className="panel modal" onSubmit={createChild}>
      <h2>Ajouter un enfant</h2><p className="muted">Un prénom et une couleur suffisent. Aucun compte ni e-mail.</p>
      <label>Prénom<input required minLength={2} maxLength={60} value={childName} onChange={e=>setChildName(e.target.value)}/></label>
      <label>Naissance · facultatif<input type="date" value={childBirth} onChange={e=>setChildBirth(e.target.value)}/></label>
      <label>Couleur<Colors value={childColor} onChange={setChildColor}/></label>
      <div className="two-buttons"><button type="button" className="button secondary" onClick={()=>setChildOpen(false)}>Annuler</button>
        <button disabled={busy} className="button primary">Ajouter</button></div>
    </form></div>}

    {inviteOpen&&<div className="overlay"><form className="panel modal" onSubmit={shareInvite}>
      <h2>Inviter un adulte</h2><p className="muted">Lien à usage unique, valable 48 heures. Le destinataire crée ou retrouve son compte Google ou e-mail.</p>
      <div className="role-options"><button type="button" onClick={()=>setInviteRole("adult")} className={inviteRole==="adult"?"selected":""}>
        Adulte invité <small>Peut participer au planning</small></button>
        <button type="button" disabled={parentsCount>=2} onClick={()=>setInviteRole("parent")} className={inviteRole==="parent"?"selected":""}>
          Deuxième parent <small>Peut gérer les enfants et les invitations</small></button></div>
      <label>Réserver à une adresse e-mail · facultatif<input type="email" value={inviteEmail} onChange={e=>setInviteEmail(e.target.value)}/></label>
      <div className="two-buttons"><button type="button" className="button secondary" onClick={()=>setInviteOpen(false)}>Annuler</button>
        <button disabled={busy} className="button primary">Créer le lien</button></div>
    </form></div>}

    {eventOpen&&<div className="overlay"><form className="panel modal" onSubmit={createEvent}>
      <h2><CalendarPlus size={22}/>Nouveau rendez-vous</h2>
      <label>Titre<input required minLength={2} maxLength={100} value={eventTitle} onChange={e=>setEventTitle(e.target.value)}/></label>
      <label>Date<input type="date" value={day} onChange={e=>setDay(e.target.value)}/></label>
      <div className="two-buttons"><label>Début<input type="time" required value={eventStart} onChange={e=>setEventStart(e.target.value)}/></label>
        <label>Fin<input type="time" required value={eventEnd} onChange={e=>setEventEnd(e.target.value)}/></label></div>
      <label>Lieu · facultatif<input value={eventLocation} onChange={e=>setEventLocation(e.target.value)}/></label>
      <label>Qui participe ?</label><div className="member-strip wrap">{members.map(m=><label key={m.id} className="check-member">
        <input type="checkbox" checked={participants.includes(m.id)} onChange={e=>setParticipants(v=>e.target.checked?[...v,m.id]:v.filter(id=>id!==m.id))}/>{m.firstName}</label>)}</div>
      <div className="two-buttons"><button type="button" className="button secondary" onClick={()=>setEventOpen(false)}>Annuler</button>
        <button disabled={busy} className="button primary">Enregistrer</button></div>
    </form></div>}
  </div>;
}
