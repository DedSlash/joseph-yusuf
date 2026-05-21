import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LegalLayoutComponent } from './legal-layout.component';

@Component({
  selector: 'app-legal',
  standalone: true,
  imports: [CommonModule, RouterModule, LegalLayoutComponent],
  template: `
    <app-legal-layout
      eyebrow="Information obligatoire"
      title="Mentions l&eacute;gales"
      updatedAt="21 mai 2026">

      <h2>&Eacute;diteur du site</h2>
      <div class="legal-info-block">
        <p><strong>Joseph&nbsp;&middot;&nbsp;Yusuf</strong></p>
        <p>&Eacute;diteur : Rey Dedy Pangou</p>
        <p>
          Email :
          <a href="mailto:support@josephyusuf.com">support&#64;josephyusuf.com</a>
        </p>
        <p>Site web : <a href="https://josephyusuf.com">josephyusuf.com</a></p>
      </div>

      <h2>Directeur de la publication</h2>
      <p>Rey Dedy Pangou, en sa qualit&eacute; d'&eacute;diteur du Service.</p>

      <h2>H&eacute;bergement</h2>
      <div class="legal-info-block">
        <p><strong>Hetzner Online GmbH</strong></p>
        <p>Industriestrasse 25</p>
        <p>91710 Gunzenhausen, Allemagne</p>
        <p>
          Site web :
          <a href="https://www.hetzner.com" target="_blank" rel="noopener">hetzner.com</a>
        </p>
      </div>

      <h2>Propri&eacute;t&eacute; intellectuelle</h2>
      <p>
        L'ensemble du contenu de la plateforme Joseph&nbsp;&middot;&nbsp;Yusuf (textes, graphismes,
        logo, ic&ocirc;nes, images, vid&eacute;os, code source) est la propri&eacute;t&eacute;
        exclusive de l'&eacute;diteur ou de ses partenaires, et prot&eacute;g&eacute; par les lois
        relatives &agrave; la propri&eacute;t&eacute; intellectuelle.
      </p>
      <p>
        Toute reproduction, repr&eacute;sentation, modification ou exploitation totale ou
        partielle, par quelque proc&eacute;d&eacute; que ce soit, sans autorisation
        &eacute;crite pr&eacute;alable, est interdite et constitue une contrefa&ccedil;on
        sanctionn&eacute;e par les articles L335-2 et suivants du Code de la
        propri&eacute;t&eacute; intellectuelle.
      </p>

      <h2>Cr&eacute;dits</h2>
      <p>
        Le concept du Service s'inspire du r&eacute;cit biblique et coranique de Joseph (Yusuf),
        figure de sagesse financi&egrave;re universellement reconnue.
      </p>
      <ul>
        <li>Polices : <strong>Cormorant Garamond</strong> et <strong>DM Sans</strong> via Google Fonts</li>
        <li>Cadre technique : Angular, Spring Boot, PostgreSQL, Stripe</li>
      </ul>

      <h2>Limitation de responsabilit&eacute;</h2>
      <p>
        Les informations et recommandations financi&egrave;res fournies par le Service sont
        &eacute;tablies de bonne foi sur la base de r&egrave;gles d&eacute;terministes
        (50/30/20, Principe de Joseph, etc.). Elles ne constituent pas un conseil financier
        personnalis&eacute; ni une recommandation d'investissement. L'&eacute;diteur ne saurait
        &ecirc;tre tenu responsable des d&eacute;cisions prises par l'utilisateur sur la base
        de ces recommandations.
      </p>

      <h2>M&eacute;diation de la consommation</h2>
      <p>
        Conform&eacute;ment &agrave; l'article L612-1 du Code de la consommation, l'utilisateur
        consommateur peut recourir gratuitement &agrave; un m&eacute;diateur en vue de la
        r&eacute;solution amiable d'un litige. Le m&eacute;diateur peut &ecirc;tre saisi via la
        plateforme europ&eacute;enne de r&egrave;glement en ligne des litiges :
        <a href="https://ec.europa.eu/consumers/odr" target="_blank" rel="noopener">ec.europa.eu/consumers/odr</a>.
      </p>

      <h2>Donn&eacute;es personnelles</h2>
      <p>
        Le traitement des donn&eacute;es personnelles est d&eacute;crit dans notre
        <a routerLink="/privacy">Politique de confidentialit&eacute;</a>.
      </p>
    </app-legal-layout>
  `
})
export class LegalComponent {}
