// Bruitages du combat synthétisés dans le navigateur (aucun fichier audio), mêmes sons que dans
// l'appli : coup porté, critique, raté, chute, soin, nouveau round, victoire, « à toi ».
// Les navigateurs n'autorisent le son qu'après un geste : SonsCombat.activer() sur un clic.
(function () {
  let ctx = null;
  let actifs = false;
  try { actifs = localStorage.getItem('sonsCombat') === '1'; } catch (e) {}

  function contexte() {
    if (!ctx) {
      const C = window.AudioContext || window.webkitAudioContext;
      if (!C) return null;
      ctx = new C();
    }
    if (ctx.state === 'suspended') ctx.resume();
    return ctx;
  }

  // Note avec extinction exponentielle ; [glisse] : variation de fréquence relative sur la durée.
  function note(freq, debut, duree, volume, type, glisse) {
    const c = contexte(); if (!c) return;
    const t = c.currentTime + debut;
    const o = c.createOscillator();
    const g = c.createGain();
    o.type = type || 'triangle';
    o.frequency.setValueAtTime(freq, t);
    if (glisse) o.frequency.exponentialRampToValueAtTime(Math.max(20, freq * (1 + glisse)), t + duree);
    g.gain.setValueAtTime(0.0001, t);
    g.gain.exponentialRampToValueAtTime(volume, t + 0.01);
    g.gain.exponentialRampToValueAtTime(0.0001, t + duree);
    o.connect(g).connect(c.destination);
    o.start(t); o.stop(t + duree + 0.05);
  }

  // Souffle filtré (raté, impact).
  function souffle(debut, duree, volume, frequence, monte) {
    const c = contexte(); if (!c) return;
    const t = c.currentTime + debut;
    const n = Math.floor(c.sampleRate * duree);
    const tampon = c.createBuffer(1, n, c.sampleRate);
    const d = tampon.getChannelData(0);
    for (let i = 0; i < n; i++) d[i] = Math.random() * 2 - 1;
    const src = c.createBufferSource();
    src.buffer = tampon;
    const filtre = c.createBiquadFilter();
    filtre.type = 'bandpass';
    filtre.frequency.value = frequence || 800;
    const g = c.createGain();
    if (monte) {
      g.gain.setValueAtTime(0.0001, t);
      g.gain.linearRampToValueAtTime(volume, t + duree / 2);
      g.gain.linearRampToValueAtTime(0.0001, t + duree);
    } else {
      g.gain.setValueAtTime(volume, t);
      g.gain.exponentialRampToValueAtTime(0.0001, t + duree);
    }
    src.connect(filtre).connect(g).connect(c.destination);
    src.start(t); src.stop(t + duree);
  }

  const sons = {
    MON_TOUR: () => { note(659, 0, 0.25, 0.25); note(880, 0.12, 0.3, 0.25); note(1318, 0.24, 0.5, 0.22); },
    ROUND: () => { note(98, 0, 1.6, 0.35, 'sine'); note(196, 0, 1.2, 0.15, 'triangle'); souffle(0, 0.3, 0.2, 300); },
    TOUCHE: () => { note(110, 0, 0.25, 0.4, 'sine', -0.4); souffle(0, 0.12, 0.35, 1200); },
    CRITIQUE: () => { note(90, 0, 0.35, 0.45, 'sine', -0.4); souffle(0, 0.16, 0.4, 1500); note(1046, 0.06, 0.5, 0.2); note(1568, 0.14, 0.6, 0.18); },
    RATE: () => { souffle(0, 0.32, 0.25, 600, true); },
    KO: () => { note(330, 0, 0.2, 0.25); note(247, 0.18, 0.2, 0.25); note(165, 0.36, 0.7, 0.3, 'triangle', -0.3); },
    MORT: () => { note(73, 0, 2.0, 0.35, 'sine'); note(110, 0.4, 1.6, 0.2, 'triangle'); },
    SOIN: () => { [523, 659, 784, 1046].forEach((f, i) => note(f, i * 0.09, 0.5, 0.18, 'sine')); },
    ETAT: () => { note(220, 0, 0.5, 0.2, 'sine', 0.5); },
    BOSS: () => { note(55, 0, 1.6, 0.4, 'sawtooth'); souffle(0, 1.2, 0.12, 200); },
    VICTOIRE: () => { note(523, 0, 0.18, 0.25); note(659, 0.16, 0.18, 0.25); note(784, 0.32, 0.18, 0.25); [1046, 784, 659].forEach(f => note(f, 0.48, 1.0, 0.18)); },
  };

  // Bruitages enregistrés de l'appli (assets/son/combat, mêmes fichiers que sur Android) : clé de
  // fichier de chaque type, variantes « cle.ogg », « cle_1.ogg »… Sans fichier : synthèse ci-dessus.
  const cles = {
    MON_TOUR: 'mon_tour', ROUND: 'round', TOUCHE: 'touche', DEGATS: 'touche', CRITIQUE: 'critique',
    RATE: 'rate', KO: 'ko', MORT: 'mort', SOIN: 'soin', ETAT: 'etat', BOSS: 'boss', VICTOIRE: 'victoire',
  };
  const tampons = {};
  let chargement = null;

  function charger() {
    if (chargement) return chargement;
    chargement = fetch('/api/sons-combat').then(r => r.json()).then(fichiers => Promise.all(fichiers.map(f => {
      const cle = f.replace(/\.[^.]+$/, '').replace(/_\d+$/, '');
      return fetch('/son/combat/' + encodeURIComponent(f))
        .then(r => r.arrayBuffer())
        .then(octets => new Promise((ok, ko) => contexte().decodeAudioData(octets, ok, ko)))
        .then(tampon => { (tampons[cle] = tampons[cle] || []).push(tampon); })
        .catch(() => {});
    }))).catch(() => {});
    return chargement;
  }

  function lireFichier(type) {
    const variantes = tampons[cles[type]];
    if (!variantes || !variantes.length) return false;
    const c = contexte(); if (!c) return false;
    const src = c.createBufferSource();
    src.buffer = variantes[Math.floor(Math.random() * variantes.length)];
    // Légère variation de hauteur des impacts, comme dans l'appli.
    if (['TOUCHE', 'DEGATS', 'CRITIQUE', 'RATE', 'KO', 'MORT'].includes(type)) src.playbackRate.value = 0.93 + Math.random() * 0.14;
    const g = c.createGain();
    g.gain.value = 0.8;
    src.connect(g).connect(c.destination);
    src.start();
    return true;
  }

  window.SonsCombat = {
    actifs: () => actifs,
    activer(oui) {
      actifs = oui;
      try { localStorage.setItem('sonsCombat', oui ? '1' : '0'); } catch (e) {}
      if (oui && contexte()) charger();
    },
    jouer(type) {
      if (!actifs) return;
      if (ctx) charger();
      try { if (!lireFichier(type) && sons[type]) sons[type](); } catch (e) {}
    },
  };
})();
