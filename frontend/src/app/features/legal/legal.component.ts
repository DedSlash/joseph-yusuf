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
      updatedAt="10 juin 2026">

      <h2>&Eacute;diteur du site</h2>
      <div class="legal-info-block">
        <p><strong>PANGOU REY DEDY</strong> (nom commercial : Joseph&nbsp;&middot;&nbsp;Yusuf)</p>
        <p>Forme juridique : Entreprise individuelle</p>
        <p>NINEA : 013127073</p>
        <p>RCCM : SN DKR 2026 A 21009</p>
        <p>Date de cr&eacute;ation : 02/06/2026</p>
        <p>Activit&eacute; : Commerce g&eacute;n&eacute;ral &ndash; Prestations de services &ndash; Informatique</p>
        <p>Si&egrave;ge social : Ouest Foire Cit&eacute; Xandar Villa N&deg;20, Dakar, S&eacute;n&eacute;gal</p>
        <p>T&eacute;l&eacute;phone : <a href="tel:+221781602037">+221 78 160 20 37</a></p>
        <p>Email : <a href="mailto:support@josephyusuf.com">support&#64;josephyusuf.com</a></p>
        <p>Site web : <a href="https://josephyusuf.com">josephyusuf.com</a></p>
      </div>

      <h2>Directeur de la publication</h2>
      <p>REY DEDY PANGOU, en sa qualit&eacute; de g&eacute;rant de l'entreprise individuelle PANGOU REY DEDY.</p>

      <h2>H&eacute;bergement</h2>
      <div class="legal-info-block">
        <p><strong>Hetzner Online GmbH</strong></p>
        <p>Industriestr. 25</p>
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
        relatives &agrave; la propri&eacute;t&eacute; intellectuelle applicables au S&eacute;n&eacute;gal
        et par les conventions internationales en vigueur.
      </p>
      <p>
        Toute reproduction, repr&eacute;sentation, modification ou exploitation totale ou
        partielle, par quelque proc&eacute;d&eacute; que ce soit, sans autorisation
        &eacute;crite pr&eacute;alable, est interdite.
      </p>

      <h2>Cr&eacute;dits</h2>
      <p>
        Le concept du Service s'inspire du r&eacute;cit biblique et coranique de Joseph (Yusuf),
        figure de sagesse financi&egrave;re universellement reconnue.
      </p>
      <ul>
        <li>Polices : <strong>Cormorant Garamond</strong> et <strong>DM Sans</strong> via Google Fonts</li>
        <li>Cadre technique : Angular, Spring Boot, PostgreSQL</li>
        <li>Paiements : PayTech S&eacute;n&eacute;gal (Wave, Orange Money, Free Money, Carte bancaire)</li>
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

      <h2>R&egrave;glement des litiges</h2>
      <p>
        En cas de litige, l'utilisateur est invit&eacute; &agrave; contacter l'&eacute;diteur
        &agrave; l'adresse <a href="mailto:support@josephyusuf.com">support&#64;josephyusuf.com</a>
        pour tenter une r&eacute;solution amiable. &Agrave; d&eacute;faut, le litige sera soumis
        au Tribunal de Commerce de Dakar, conform&eacute;ment au droit s&eacute;n&eacute;galais.
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
