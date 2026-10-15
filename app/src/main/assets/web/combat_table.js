// Combat sur la page de la table : ordre d'initiative, tour en cours, récit animé, bulles de
// dégâts, grandes annonces (round, « À toi ! », critique, victoire) et sons.
(function () {
  function e(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  }

  const panneau = document.getElementById('combat');
  const recitEl = document.createElement('div'); recitEl.id = 'recitTable'; document.body.appendChild(recitEl);
  const annonceEl = document.createElement('div'); annonceEl.id = 'annonceCombat'; document.body.appendChild(annonceEl);
  const flashEl = document.createElement('div'); flashEl.id = 'flashCombat'; document.body.appendChild(flashEl);

  let dernierTexte = null;
  let combatId = null;
  let dernierVu = null;
  let actifVu = null;
  const fileAnnonces = [];
  let annonceEnCours = false;

  function annoncer(texte, couleur) {
    fileAnnonces.push({ texte, couleur });
    if (!annonceEnCours) suivante();
  }
  function suivante() {
    const a = fileAnnonces.shift();
    if (!a) { annonceEnCours = false; annonceEl.classList.remove('on'); return; }
    annonceEnCours = true;
    annonceEl.innerHTML = '<div class="texte" style="color:' + a.couleur + '">' + e(a.texte) + '</div>';
    annonceEl.classList.add('on');
    setTimeout(suivante, 2400);
  }
  function flash(couleur) {
    flashEl.style.background = 'radial-gradient(circle, rgba(0,0,0,0) 35%, ' + couleur + ')';
    flashEl.classList.remove('on'); void flashEl.offsetWidth; flashEl.classList.add('on');
  }
  function bulle(cibleId, texte, classe) {
    const f = panneau.querySelector('.fighter[data-id="' + CSS.escape(cibleId) + '"]');
    if (!f) return;
    f.classList.remove('touche', 'soigne'); void f.offsetWidth;
    f.classList.add(classe === 'soin' ? 'soigne' : 'touche');
    const b = document.createElement('div');
    b.className = 'flottant ' + classe;
    b.textContent = texte;
    f.appendChild(b);
    setTimeout(() => b.remove(), 1700);
  }

  function boutonSon() {
    const actifs = window.SonsCombat && SonsCombat.actifs();
    return '<button class="son" id="boutonSon">' + (actifs ? '🔊 Son' : '🔇 Son') + '</button>';
  }

  function rendrePanneau(c) {
    let html = boutonSon() + '<h2>⚔ ' + e(c.titre) + '</h2><div class="sub">' +
      (c.round > 0 ? 'Round ' + c.round : 'Préparation du combat…') + '</div>';
    if (c.enCours && c.actifNom) {
      html += '<div class="tourBandeau' + (c.actifMonstre ? ' monstre' : '') + '">' +
        (c.actifMonstre ? '▶ Au tour de ' + e(c.actifNom) : '⚔ À toi, ' + e(c.actifNom) + ' !') +
        (c.prochainNom ? '<span class="ensuite">Ensuite : ' + e(c.prochainNom) + '</span>' : '') + '</div>';
    }
    for (const b of c.boss) {
      html += '<div class="boss' + (b.prepare ? ' prepare' : '') + '"><div class="name">👑 ' + e(b.nom) +
        (b.vaincu ? ' — vaincu !' : (b.phase ? ' — ' + e(b.phase) : '')) + '</div>' +
        '<div class="meta">' + e(b.titre) + (b.pointsMax > 0 && !b.vaincu ? ' · ⚡ ' + '●'.repeat(b.points) + '○'.repeat(Math.max(0, b.pointsMax - b.points)) : '') + '</div>' +
        '<div class="bar"><div style="width:' + Math.max(0, b.ratio) + '%;background:#e53935"></div></div>' +
        (b.annonce ? '<div class="annonce">' + e(b.annonce) + '</div>' : '') + '</div>';
    }
    for (const f of c.combattants) {
      const couleur = f.ratio > 50 ? '#66bb6a' : (f.ratio > 25 ? '#ffa726' : '#ef5350');
      const meta = (f.pv ? 'PV ' + e(f.pv) : e(f.sante)) + (f.conditions.length ? ' · ' + e(f.icones) + ' ' + f.conditions.map(e).join(', ') : '');
      html += '<div class="fighter' + (f.monstre ? ' monstre' : '') + (f.actif ? ' actif' : '') + (f.horsCombat ? ' out' : '') +
        (f.dejaJoue && !f.horsCombat ? ' joue' : '') + '" data-id="' + e(f.id) + '">' +
        '<div class="init">' + (f.initiative ?? '–') + '</div>' +
        (f.portrait ? '<img class="portrait" src="' + e(f.portrait) + '" alt="">' : '') +
        '<div class="body"><div class="name">' + (f.actif ? '▶ ' : '') + (f.boss ? '👑 ' : '') + e(f.nom) + (f.dejaJoue && !f.horsCombat ? ' ✓' : '') + '</div>' +
        '<div class="meta">' + meta + '</div>' +
        (f.position ? '<div class="pos' + (f.position.startsWith('Au contact') ? ' contact' : '') + '">📍 ' + e(f.position) + '</div>' : '') +
        '<div class="bar"><div style="width:' + Math.max(0, f.ratio) + '%;background:' + couleur + '"></div></div></div></div>';
    }
    panneau.innerHTML = html;
    panneau.style.display = 'block';
    document.getElementById('boutonSon').onclick = () => {
      SonsCombat.activer(!SonsCombat.actifs());
      document.getElementById('boutonSon').textContent = SonsCombat.actifs() ? '🔊 Son' : '🔇 Son';
    };
  }

  function rendreRecit(c) {
    const lignes = c.recit.filter(r => r.type !== 'TOUR' && r.texte).slice(-6);
    recitEl.innerHTML = lignes.map((r, i) =>
      '<div class="ligne ' + e(r.type) + (i < lignes.length - 3 ? ' vieille' : '') + '">' + e(r.texte) + '</div>'
    ).reverse().join('');
    recitEl.style.display = lignes.length ? 'flex' : 'none';
  }

  // Effets des lignes apparues depuis le dernier passage (rien au premier chargement).
  function effets(c) {
    const dernier = c.recit.length ? c.recit[c.recit.length - 1].numero : 0;
    if (dernierVu === null || combatId !== c.id) { dernierVu = dernier; combatId = c.id; actifVu = c.actifId; return; }
    const nouvelles = c.recit.filter(r => r.numero > dernierVu);
    dernierVu = dernier;
    let sons = 0;
    for (const r of nouvelles) {
      if (r.cibleId && r.montant != null) {
        if (r.type === 'DEGATS') bulle(r.cibleId, '-' + r.montant, 'degats');
        if (r.type === 'SOIN') bulle(r.cibleId, '+' + r.montant, 'soin');
      }
      if (r.type === 'ROUND') annoncer('⚔ ' + r.texte, '#ffe08a');
      if (r.type === 'CRITIQUE') { annoncer('💥 COUP CRITIQUE !', '#ffd54f'); flash('rgba(255,213,79,0.55)'); }
      if (r.type === 'KO') flash('rgba(229,57,53,0.5)');
      if (r.type === 'MORT') { annoncer('💀 ' + r.texte, '#ff8a80'); flash('rgba(120,0,0,0.7)'); }
      if (r.type === 'VICTOIRE') annoncer('🏆 ' + r.texte, '#ffe08a');
      if (r.type !== 'TOUR' && r.type !== 'INFO' && r.type !== 'ACTION' && r.type !== 'DEPLACEMENT' && r.type !== 'SAUVEGARDE' && sons < 3) {
        const type = r.type === 'DEGATS' ? 'TOUCHE' : r.type;
        setTimeout(() => window.SonsCombat && SonsCombat.jouer(type), sons * 260);
        sons++;
      }
    }
    // Un personnage prend la main : grande annonce pour que le joueur le voie de loin.
    if (c.actifId !== actifVu) {
      actifVu = c.actifId;
      if (c.enCours && c.actifNom && !c.actifMonstre) {
        annoncer('⚔ À toi, ' + c.actifNom + ' !', '#ffe08a');
        if (window.SonsCombat) SonsCombat.jouer('MON_TOUR');
      }
    }
  }

  async function rafraichir() {
    try {
      const res = await fetch('/api/combat');
      const texte = await res.text();
      // Rien de neuf : on ne redessine pas (les bulles de dégâts vont au bout de leur animation).
      if (texte === dernierTexte) return;
      dernierTexte = texte;
      const c = JSON.parse(texte);
      if (!c) {
        panneau.style.display = 'none'; recitEl.style.display = 'none';
        combatId = null; dernierVu = null;
        return;
      }
      rendrePanneau(c);
      rendreRecit(c);
      effets(c);
    } catch (err) {}
  }

  rafraichir();
  setInterval(rafraichir, 1000);
})();
