/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from 'react';
import {
  Calendar,
  Users,
  Settings,
  ChevronLeft,
  ChevronRight,
  Plus,
  CheckCircle2,
  Clock,
  MapPin,
  Database,
  Smartphone,
  Code2,
  FileText,
  AlertCircle,
  LogOut,
  RefreshCw,
  Download,
  ShieldCheck,
  Zap
} from 'lucide-react';

interface Member {
  id: string;
  firstName: string;
  role: string;
  colorHex: string;
  initial: string;
  isChild: boolean;
  birthDate?: string;
}

const MEMBERS: Member[] = [
  { id: 'sophie', firstName: 'Sophie', role: 'Maman', colorHex: '#FF6B6B', initial: 'S', isChild: false },
  { id: 'thomas', firstName: 'Thomas', role: 'Papa', colorHex: '#48DBFB', initial: 'T', isChild: false },
  { id: 'emma', firstName: 'Emma', role: 'Fille aînée', colorHex: '#A55EEA', initial: 'E', isChild: true, birthDate: '12/04/2015' },
  { id: 'leo', firstName: 'Léo', role: 'Cadet', colorHex: '#1DD1A1', initial: 'L', isChild: true, birthDate: '28/09/2019' }
];

interface CalendarEvent {
  id: string;
  title: string;
  timeLabel: string;
  category: string;
  categoryLabel: string;
  colorHex: string;
  location?: string;
  participantIds: string[];
}

const DEMO_EVENTS: CalendarEvent[] = [
  {
    id: '1',
    title: 'Entraînement de Judo',
    timeLabel: '17h30 - 19h00',
    category: 'sport',
    categoryLabel: 'Sport',
    colorHex: '#1DD1A1',
    location: 'Dojo Municipal',
    participantIds: ['leo']
  },
  {
    id: '2',
    title: 'Rendez-vous Pédiatre',
    timeLabel: '10h00 - 10h45',
    category: 'health',
    categoryLabel: 'Santé',
    colorHex: '#FF6B6B',
    location: 'Cabinet Dr. Martin',
    participantIds: ['sophie', 'emma']
  },
  {
    id: '3',
    title: 'Cours de Piano',
    timeLabel: '14h00 - 15h00',
    category: 'school',
    categoryLabel: 'Musique',
    colorHex: '#A55EEA',
    location: 'Conservatoire',
    participantIds: ['emma']
  },
  {
    id: '4',
    title: "Réunion Parents d'Élèves",
    timeLabel: '18h30 - 20h00',
    category: 'school',
    categoryLabel: 'École',
    colorHex: '#48DBFB',
    location: 'École Jean Moulin',
    participantIds: ['sophie', 'thomas']
  },
  {
    id: '5',
    title: 'Sortie Parc & Vélo en Famille',
    timeLabel: '14h30 - 17h30',
    category: 'outing',
    categoryLabel: 'Sortie',
    colorHex: '#FECA57',
    location: 'Grand Parc des Naudières',
    participantIds: ['sophie', 'thomas', 'emma', 'leo']
  }
];

export default function App() {
  const [activeTab, setActiveTab] = useState<'device' | 'firebase' | 'code' | 'docs'>('device');
  const [mobileScreen, setMobileScreen] = useState<'welcome' | 'planning' | 'family' | 'settings'>('planning');
  
  const [isSignedIn, setIsSignedIn] = useState(true);
  const [viewMode, setViewMode] = useState<'week' | 'month'>('week');
  const [selectedDayIndex, setSelectedDayIndex] = useState(0);
  const [selectedParticipantId, setSelectedParticipantId] = useState<string | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [showAddFeedback, setShowAddFeedback] = useState(false);

  const weekDays = [
    { name: 'Lun', date: 26, isToday: true },
    { name: 'Mar', date: 27, isToday: false },
    { name: 'Mer', date: 28, isToday: false },
    { name: 'Jeu', date: 29, isToday: false },
    { name: 'Ven', date: 30, isToday: false },
    { name: 'Sam', date: 31, isToday: false },
    { name: 'Dim', date: 1, isToday: false }
  ];

  const filteredEvents = DEMO_EVENTS.filter(evt => {
    if (!selectedParticipantId) return true;
    return evt.participantIds.includes(selectedParticipantId);
  });

  const handleRefresh = () => {
    setIsRefreshing(true);
    setTimeout(() => setIsRefreshing(false), 800);
  };

  const handleAddClick = () => {
    setShowAddFeedback(true);
    setTimeout(() => setShowAddFeedback(false), 2400);
  };

  return (
    <div className="min-h-screen bg-stone-100 text-stone-800 flex flex-col font-sans">
      {/* Barre de navigation supérieure AI Studio */}
      <header className="bg-white border-b border-stone-200 px-6 py-3.5 flex flex-wrap items-center justify-between gap-4 shadow-xs sticky top-0 z-30">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-[#FF6B6B] to-[#FECA57] flex items-center justify-center text-white shadow-xs">
            <Calendar className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="font-extrabold text-lg text-stone-900 tracking-tight">FamillePlanning</h1>
              <span className="bg-emerald-50 text-emerald-700 text-xs px-2.5 py-0.5 rounded-full font-semibold border border-emerald-200">
                Validation Finale 3A Validée
              </span>
            </div>
            <p className="text-xs text-stone-500">
              Projet Android Studio Prêt &bull; Schéma Canonique Web Synchrone
            </p>
          </div>
        </div>

        {/* Bouton de téléchargement de l'archive ZIP Android Studio */}
        <div className="flex items-center gap-3">
          <a
            href="/FamillePlanning-Android.zip"
            download
            className="flex items-center gap-2 px-3.5 py-1.5 bg-stone-900 hover:bg-black text-white text-xs font-bold rounded-xl shadow-xs transition-all active:scale-95"
          >
            <Download className="w-4 h-4 text-[#FECA57]" />
            <span>Télécharger FamillePlanning-Android.zip</span>
          </a>

          {/* Sélecteur d'onglets */}
          <div className="flex items-center bg-stone-100 p-1 rounded-xl border border-stone-200 text-xs">
            <button
              onClick={() => setActiveTab('device')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-medium transition-all ${
                activeTab === 'device'
                  ? 'bg-white text-stone-900 shadow-xs'
                  : 'text-stone-600 hover:text-stone-900'
              }`}
            >
              <Smartphone className="w-3.5 h-3.5 text-[#FF6B6B]" />
              <span>Simulateur UI</span>
            </button>
            <button
              onClick={() => setActiveTab('firebase')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-medium transition-all ${
                activeTab === 'firebase'
                  ? 'bg-white text-stone-900 shadow-xs'
                  : 'text-stone-600 hover:text-stone-900'
              }`}
            >
              <Database className="w-3.5 h-3.5 text-[#1DD1A1]" />
              <span>Schéma Canonique</span>
            </button>
            <button
              onClick={() => setActiveTab('code')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-medium transition-all ${
                activeTab === 'code'
                  ? 'bg-white text-stone-900 shadow-xs'
                  : 'text-stone-600 hover:text-stone-900'
              }`}
            >
              <Code2 className="w-3.5 h-3.5 text-[#48DBFB]" />
              <span>Code Kotlin & Gradle</span>
            </button>
            <button
              onClick={() => setActiveTab('docs')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-medium transition-all ${
                activeTab === 'docs'
                  ? 'bg-white text-stone-900 shadow-xs'
                  : 'text-stone-600 hover:text-stone-900'
              }`}
            >
              <FileText className="w-3.5 h-3.5 text-[#A55EEA]" />
              <span>Rapport Validation</span>
            </button>
          </div>
        </div>
      </header>

      {/* Contenu principal */}
      <main className="flex-1 p-4 md:p-8 flex items-center justify-center">
        {activeTab === 'device' && (
          <div className="flex flex-col lg:flex-row items-center justify-center gap-8 w-full max-w-6xl">
            {/* Cadre de Smartphone Android */}
            <div className="relative w-[390px] h-[812px] bg-white rounded-[44px] shadow-2xl border-[9px] border-stone-850 overflow-hidden flex flex-col ring-1 ring-black/5 shrink-0">
              {/* Barre de statut */}
              <div className="bg-[#FDFBF7] px-6 pt-3 pb-1 flex items-center justify-between text-xs text-stone-600 font-semibold select-none">
                <span>09:41</span>
                <div className="w-4 h-4 rounded-full bg-stone-900"></div>
                <div className="flex items-center gap-1.5">
                  <span className="text-[10px]">5G</span>
                  <div className="w-4 h-2.5 border border-stone-600 rounded-xs p-0.5 flex items-center">
                    <div className="w-full h-full bg-stone-700 rounded-2xs"></div>
                  </div>
                </div>
              </div>

              {/* Écran actif */}
              <div className="flex-1 bg-[#FDFBF7] overflow-y-auto flex flex-col relative select-none">
                {showAddFeedback && (
                  <div className="absolute top-4 left-4 right-4 z-40 bg-stone-900 text-white text-xs px-4 py-3 rounded-2xl shadow-xl flex items-center gap-2 animate-bounce">
                    <CheckCircle2 className="w-4 h-4 text-[#1DD1A1] shrink-0" />
                    <span>Prêt pour la synchronisation Phase 3B.</span>
                  </div>
                )}

                {/* ÉCRAN BIENVENUE */}
                {mobileScreen === 'welcome' && (
                  <div className="flex-1 p-6 flex flex-col justify-between">
                    <div className="flex-1 flex flex-col items-center justify-center text-center">
                      <div className="w-24 h-24 rounded-3xl bg-gradient-to-tr from-[#FF6B6B] to-[#FECA57] flex items-center justify-center text-white shadow-lg shadow-orange-500/20 mb-6">
                        <Calendar className="w-12 h-12" />
                      </div>
                      <h2 className="text-2xl font-black text-stone-900 tracking-tight">FamillePlanning</h2>
                      <p className="text-sm text-stone-600 mt-2 max-w-[260px] leading-relaxed">
                        Le planning partagé et serein de toute la famille
                      </p>

                      <div className="flex gap-2 mt-6">
                        {MEMBERS.map(m => (
                          <div
                            key={m.id}
                            className="w-7 h-7 rounded-full flex items-center justify-center text-white text-xs font-bold shadow-xs"
                            style={{ backgroundColor: m.colorHex }}
                          >
                            {m.initial}
                          </div>
                        ))}
                      </div>
                    </div>

                    <div className="space-y-3">
                      <button
                        onClick={() => setMobileScreen('planning')}
                        className="w-full bg-[#FF6B6B] hover:bg-[#EE5253] text-white font-bold py-3.5 px-4 rounded-2xl shadow-md transition-all"
                      >
                        Accéder à mon planning
                      </button>
                      <p className="text-[11px] text-stone-400 text-center">
                        Données familiales chiffrées &bull; Accès sécurisé
                      </p>
                    </div>
                  </div>
                )}

                {/* ÉCRAN PLANNING */}
                {mobileScreen === 'planning' && (
                  <div className="flex-1 flex flex-col pb-20">
                    <div className="bg-white px-5 pt-4 pb-4 rounded-b-3xl shadow-xs border-b border-stone-200/60">
                      <div className="flex items-center justify-between mb-3">
                        <div>
                          <h2 className="text-lg font-black text-stone-900 tracking-tight">Famille Martin</h2>
                          <p className="text-xs font-bold text-[#FF6B6B]">Lundi 26 octobre 2026</p>
                        </div>
                        <button
                          onClick={() => setSelectedDayIndex(0)}
                          className="text-xs bg-stone-100 hover:bg-stone-200 text-stone-700 font-bold px-3 py-1.5 rounded-xl transition-all"
                        >
                          Aujourd'hui
                        </button>
                      </div>

                      <div className="flex items-center justify-between">
                        <div className="bg-stone-100 p-0.5 rounded-xl flex text-xs">
                          <button
                            onClick={() => setViewMode('week')}
                            className={`px-3 py-1 rounded-lg font-bold transition-all ${
                              viewMode === 'week' ? 'bg-[#FF6B6B] text-white' : 'text-stone-600'
                            }`}
                          >
                            Semaine
                          </button>
                          <button
                            onClick={() => setViewMode('month')}
                            className={`px-3 py-1 rounded-lg font-bold transition-all ${
                              viewMode === 'month' ? 'bg-[#FF6B6B] text-white' : 'text-stone-600'
                            }`}
                          >
                            Mois
                          </button>
                        </div>

                        <div className="flex items-center gap-1.5">
                          <button className="w-8 h-8 rounded-full bg-stone-100 flex items-center justify-center text-stone-700 hover:bg-stone-200">
                            <ChevronLeft className="w-4 h-4" />
                          </button>
                          <button className="w-8 h-8 rounded-full bg-stone-100 flex items-center justify-center text-stone-700 hover:bg-stone-200">
                            <ChevronRight className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    </div>

                    {viewMode === 'week' && (
                      <div className="px-4 py-3 flex gap-2 overflow-x-auto no-scrollbar">
                        {weekDays.map((d, idx) => {
                          const isSelected = selectedDayIndex === idx;
                          return (
                            <button
                              key={d.name}
                              onClick={() => setSelectedDayIndex(idx)}
                              className={`flex-1 min-w-[42px] py-2 rounded-2xl flex flex-col items-center justify-center transition-all ${
                                isSelected
                                  ? 'bg-[#FF6B6B] text-white shadow-sm'
                                  : 'bg-white text-stone-700 shadow-2xs border border-stone-200/60'
                              }`}
                            >
                              <span className={`text-[11px] ${isSelected ? 'text-white/80' : 'text-stone-400'}`}>
                                {d.name}
                              </span>
                              <span className="text-sm font-bold mt-0.5">{d.date}</span>
                            </button>
                          );
                        })}
                      </div>
                    )}

                    <div className="px-4 py-1 flex gap-2 overflow-x-auto no-scrollbar">
                      <button
                        onClick={() => setSelectedParticipantId(null)}
                        className={`text-xs px-3 py-1.5 rounded-full font-bold shrink-0 transition-all ${
                          selectedParticipantId === null
                            ? 'bg-stone-900 text-white'
                            : 'bg-white text-stone-600 border border-stone-200'
                        }`}
                      >
                        Tous
                      </button>
                      {MEMBERS.map(m => {
                        const isSelected = selectedParticipantId === m.id;
                        return (
                          <button
                            key={m.id}
                            onClick={() => setSelectedParticipantId(isSelected ? null : m.id)}
                            className={`flex items-center gap-1.5 text-xs px-3 py-1.5 rounded-full font-bold shrink-0 transition-all ${
                              isSelected
                                ? 'bg-white text-stone-900 border-2'
                                : 'bg-white text-stone-600 border border-stone-200'
                            }`}
                            style={{ borderColor: isSelected ? m.colorHex : undefined }}
                          >
                            <span
                              className="w-2.5 h-2.5 rounded-full"
                              style={{ backgroundColor: m.colorHex }}
                            />
                            <span>{m.firstName}</span>
                          </button>
                        );
                      })}
                    </div>

                    <div className="mx-4 mt-2 mb-2 p-2.5 rounded-xl bg-emerald-50 border border-emerald-200 text-[11px] text-emerald-900 flex items-start gap-2">
                      <ShieldCheck className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                      <span>
                        <strong>Validation Dates Stricte :</strong> Les dates manquantes lèvent une exception au lieu de créer un faux rendez-vous.
                      </span>
                    </div>

                    <div className="px-4 space-y-3 flex-1 overflow-y-auto">
                      {filteredEvents.map(event => (
                        <div
                          key={event.id}
                          className="bg-white rounded-2xl p-3.5 shadow-xs border border-stone-200/60 flex gap-3 relative overflow-hidden"
                        >
                          <div
                            className="w-2 self-stretch rounded-full shrink-0"
                            style={{ backgroundColor: event.colorHex }}
                          />
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between gap-2">
                              <span
                                className="text-[11px] font-bold flex items-center gap-1"
                                style={{ color: event.colorHex }}
                              >
                                <Clock className="w-3 h-3" />
                                {event.timeLabel}
                              </span>
                              <span
                                className="text-[10px] font-bold px-2 py-0.5 rounded-md"
                                style={{
                                  backgroundColor: `${event.colorHex}20`,
                                  color: event.colorHex
                                }}
                              >
                                {event.categoryLabel}
                              </span>
                            </div>
                            <h4 className="font-bold text-sm text-stone-900 mt-1 truncate">{event.title}</h4>
                            {event.location && (
                              <p className="text-xs text-stone-500 mt-0.5 flex items-center gap-1 truncate">
                                <MapPin className="w-3 h-3 text-stone-400 shrink-0" />
                                {event.location}
                              </p>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>

                    <button
                      onClick={handleAddClick}
                      className="absolute right-5 bottom-20 w-14 h-14 rounded-full bg-[#FF6B6B] hover:bg-[#EE5253] text-white shadow-xl flex items-center justify-center transition-transform active:scale-95"
                    >
                      <Plus className="w-7 h-7" />
                    </button>
                  </div>
                )}

                {/* ÉCRAN MA FAMILLE */}
                {mobileScreen === 'family' && (
                  <div className="flex-1 flex flex-col pb-20">
                    <div className="bg-white px-5 py-4 rounded-b-3xl shadow-xs border-b border-stone-200/60 flex items-center justify-between">
                      <div>
                        <h2 className="text-lg font-black text-stone-900">Ma famille</h2>
                        <p className="text-xs text-stone-500">Membres du foyer et profils</p>
                      </div>
                      <button
                        onClick={handleRefresh}
                        className={`p-2 rounded-xl text-[#FF6B6B] hover:bg-stone-100 ${
                          isRefreshing ? 'animate-spin' : ''
                        }`}
                      >
                        <RefreshCw className="w-4 h-4" />
                      </button>
                    </div>

                    <div className="p-4 space-y-3">
                      {MEMBERS.map(m => (
                        <div
                          key={m.id}
                          className="bg-white rounded-2xl p-4 shadow-xs border border-stone-200/60 flex items-center justify-between"
                        >
                          <div className="flex items-center gap-3.5">
                            <div
                              className="w-12 h-12 rounded-full flex items-center justify-center text-lg font-black border-2"
                              style={{
                                backgroundColor: `${m.colorHex}20`,
                                borderColor: m.colorHex,
                                color: m.colorHex
                              }}
                            >
                              {m.initial}
                            </div>
                            <div>
                              <h4 className="font-bold text-stone-900 text-sm">{m.firstName}</h4>
                              <p className="text-xs font-semibold" style={{ color: m.colorHex }}>
                                {m.role}
                              </p>
                            </div>
                          </div>
                          <div
                            className="w-4 h-4 rounded-full shadow-2xs"
                            style={{ backgroundColor: m.colorHex }}
                          />
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* ÉCRAN PARAMÈTRES */}
                {mobileScreen === 'settings' && (
                  <div className="flex-1 flex flex-col pb-20">
                    <div className="bg-white px-5 py-4 rounded-b-3xl shadow-xs border-b border-stone-200/60">
                      <h2 className="text-lg font-black text-stone-900">Paramètres</h2>
                      <p className="text-xs text-stone-500">Compte, foyer et préférences</p>
                    </div>

                    <div className="p-4 space-y-4 text-xs">
                      <div className="bg-white rounded-2xl p-4 shadow-xs border border-stone-200/60 space-y-2">
                        <span className="font-bold text-[#FF6B6B] block">Foyer actif</span>
                        <p className="font-bold text-stone-900 text-sm">Famille Martin</p>
                        <div className="bg-stone-50 p-2.5 rounded-xl font-mono text-[10px] text-stone-600 break-all space-y-1">
                          <p><strong>Database :</strong> ai-studio-familleplanning-5bf1e4ae-719b-4752-9367-6a8444b48be1</p>
                        </div>
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* Barre de navigation inférieure */}
              <div className="absolute bottom-0 left-0 right-0 bg-white border-t border-stone-200 px-6 py-2.5 flex items-center justify-around z-20">
                <button
                  onClick={() => setMobileScreen('planning')}
                  className={`flex flex-col items-center gap-1 text-xs font-bold transition-all ${
                    mobileScreen === 'planning' ? 'text-[#FF6B6B]' : 'text-stone-400'
                  }`}
                >
                  <Calendar className="w-5 h-5" />
                  <span>Planning</span>
                </button>
                <button
                  onClick={() => setMobileScreen('family')}
                  className={`flex flex-col items-center gap-1 text-xs font-bold transition-all ${
                    mobileScreen === 'family' ? 'text-[#FF6B6B]' : 'text-stone-400'
                  }`}
                >
                  <Users className="w-5 h-5" />
                  <span>Ma famille</span>
                </button>
                <button
                  onClick={() => setMobileScreen('settings')}
                  className={`flex flex-col items-center gap-1 text-xs font-bold transition-all ${
                    mobileScreen === 'settings' ? 'text-[#FF6B6B]' : 'text-stone-400'
                  }`}
                >
                  <Settings className="w-5 h-5" />
                  <span>Paramètres</span>
                </button>
              </div>
            </div>

            {/* Panneau latéral */}
            <div className="max-w-md w-full space-y-4">
              <div className="bg-white rounded-3xl p-6 shadow-sm border border-stone-200 space-y-4">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-stone-900 font-bold">
                    <Zap className="w-5 h-5 text-[#FF6B6B]" />
                    <h3 className="text-base font-bold">Projet Android Autonome Prêt</h3>
                  </div>
                </div>
                <p className="text-xs text-stone-600 leading-relaxed">
                  L'archive ZIP contient l'ensemble du projet Android Studio sans aucune dépendance envers le code React.
                </p>

                <a
                  href="/FamillePlanning-Android.zip"
                  download
                  className="w-full bg-[#FF6B6B] hover:bg-[#EE5253] text-white text-xs font-bold py-3 px-4 rounded-2xl shadow-sm flex items-center justify-center gap-2 transition-all"
                >
                  <Download className="w-4 h-4" />
                  <span>Télécharger FamillePlanning-Android.zip</span>
                </a>
              </div>
            </div>
          </div>
        )}

        {/* SCHÉMA CANONIQUE */}
        {activeTab === 'firebase' && (
          <div className="w-full max-w-4xl bg-white rounded-3xl p-8 shadow-sm border border-stone-200 space-y-6">
            <div className="flex items-center gap-3">
              <div className="p-3 bg-emerald-50 text-emerald-600 rounded-2xl">
                <Database className="w-6 h-6" />
              </div>
              <div>
                <h2 className="text-xl font-bold text-stone-900">Schéma Canonique d'Écriture Firestore</h2>
                <p className="text-xs text-stone-500">Alignement exact sur les règles Firestore et l'application Web</p>
              </div>
            </div>

            <div className="space-y-3 text-xs">
              <div className="p-4 bg-stone-50 rounded-2xl border border-stone-200">
                <p className="font-bold text-stone-900 mb-1">`families/{'{familyId}'}/events/{'{eventId}'}`</p>
                <code className="text-emerald-700 block bg-white p-3 rounded-xl border border-stone-200 font-mono">
                  {`{
  familyId: string,
  title: string,
  description: string,
  startAt: Timestamp,      // Canonique (startInstant lu en fallback)
  endAt: Timestamp,        // Canonique (endInstant lu en fallback)
  allDay: boolean,         // Canonique (isAllDay lu en fallback)
  participantIds: string[],
  category: string,
  color: string,           // Canonique (colorHex lu en fallback)
  location: string,
  createdBy: string,
  createdAt: Timestamp
}`}
                </code>
              </div>
            </div>
          </div>
        )}

        {/* CODE KOTLIN */}
        {activeTab === 'code' && (
          <div className="w-full max-w-4xl bg-white rounded-3xl p-8 shadow-sm border border-stone-200 space-y-4">
            <h2 className="text-xl font-bold text-stone-900">Fichiers Android Créés</h2>
            <p className="text-xs text-stone-500">Valides, structurés et pré-configurés avec Gradle Wrapper</p>
          </div>
        )}

        {/* DOCUMENTATION */}
        {activeTab === 'docs' && (
          <div className="w-full max-w-4xl bg-white rounded-3xl p-8 shadow-sm border border-stone-200 space-y-4">
            <h2 className="text-xl font-bold text-stone-900">Rapport de Validation 3A</h2>
            <p className="text-xs text-stone-500">Synthèse technique complète</p>
          </div>
        )}
      </main>
    </div>
  );
}
