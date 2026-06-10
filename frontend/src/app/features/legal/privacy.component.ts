import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LegalLayoutComponent } from './legal-layout.component';

@Component({
  selector: 'app-privacy',
  standalone: true,
  imports: [CommonModule, RouterModule, LegalLayoutComponent],
  template: `
    <app-legal-layout
      eyebrow="Protection des donn&eacute;es"
      title="Politique de confidentialit&eacute;"
      updatedAt="10 juin 2026">

      <p>
        La pr&eacute;sente politique d&eacute;crit la mani&egrave;re dont
        <strong>PANGOU REY DEDY</strong> (nom commercial : Joseph&nbsp;&middot;&nbsp;Yusuf),
        ci-apr&egrave;s &laquo;&nbsp;nous&nbsp;&raquo;, collecte, utilise et prot&egrave;ge les
        donn&eacute;es personnelles de ses utilisateurs, conform&eacute;ment &agrave; la
        <strong>Loi n&deg;&nbsp;2008-12 du 25 janvier 2008</strong> sur la protection des donn&eacute;es
        &agrave; caract&egrave;re personnel (R&eacute;publique du S&eacute;n&eacute;gal).
      </p>

      <h2>1. Responsable du traitement</h2>
      <div class="legal-info-block">
        <p><strong>PANGOU REY DEDY</strong> (Joseph&nbsp;&middot;&nbsp;Yusuf)</p>
        <p>NINEA : 013127073</p>
        <p>Si&egrave;ge social : Ouest Foire Cit&eacute; Xandar Villa N&deg;20, Dakar, S&eacute;n&eacute;gal</p>
        <p>Contact donn&eacute;es personnelles :
          <a href="mailto:support@josephyusuf.com">support&#64;josephyusuf.com</a>
        </p>
      </div>

      <h2>2. Donn&eacute;es collect&eacute;es</h2>

      <h3>2.1 Donn&eacute;es fournies par l'utilisateur</h3>
      <ul>
        <li><strong>Compte</strong> : pr&eacute;nom, nom, email, mot de passe (hach&eacute; via bcrypt)</li>
        <li><strong>Revenus</strong> : montants, dates, sources, devises saisis par l'utilisateur</li>
        <li><strong>&Eacute;pargne</strong> : objectifs d'&eacute;pargne et contributions</li>
        <li><strong>Support</strong> : tickets et messages &eacute;chang&eacute;s avec l'&eacute;quipe</li>
      </ul>

      <h3>2.2 Donn&eacute;es collect&eacute;es automatiquement</h3>
      <ul>
        <li><strong>Connexion</strong> : adresse IP, date/heure, navigateur (logs serveur, conserv&eacute;s 12 mois)</li>
        <li><strong>Paiement</strong> : identifiants de transaction PayTech (jamais les num&eacute;ros de carte, codes PIN ou donn&eacute;es bancaires compl&egrave;tes &mdash; celles-ci sont trait&eacute;es directement par PayTech)</li>
      </ul>

      <h2>3. Finalit&eacute;s du traitement</h2>
      <ul>
        <li>Fournir et maintenir le Service (gestion du compte, classification des revenus, alertes)</li>
        <li>Traiter les paiements et g&eacute;rer les abonnements via PayTech</li>
        <li>R&eacute;pondre aux demandes de support</li>
        <li>Envoyer des notifications transactionnelles (confirmation d'inscription, r&eacute;initialisation de mot de passe, alertes financi&egrave;res)</li>
        <li>Pr&eacute;venir la fraude et assurer la s&eacute;curit&eacute; du Service</li>
        <li>Respecter nos obligations l&eacute;gales (comptabilit&eacute;, fiscalit&eacute;)</li>
      </ul>

      <h2>4. Bases l&eacute;gales</h2>
      <ul>
        <li><strong>Ex&eacute;cution du contrat</strong> : gestion du compte, des revenus, des abonnements</li>
        <li><strong>Obligation l&eacute;gale</strong> : conservation des factures, lutte contre le blanchiment</li>
        <li><strong>Int&eacute;r&ecirc;t l&eacute;gitime</strong> : s&eacute;curit&eacute; et pr&eacute;vention de la fraude</li>
        <li><strong>Consentement</strong> : envoi d'emails non transactionnels (le cas &eacute;ch&eacute;ant)</li>
      </ul>

      <h2>5. Sous-traitants et destinataires</h2>
      <p>Nous partageons certaines donn&eacute;es avec :</p>
      <ul>
        <li><strong>PayTech S&eacute;n&eacute;gal</strong> (paiement : Wave, Orange Money, Free Money, Carte bancaire) &mdash; S&eacute;n&eacute;gal</li>
        <li><strong>Hetzner Online GmbH</strong> (h&eacute;bergement serveurs) &mdash; Allemagne, ISO 27001</li>
        <li><strong>Google</strong> (envoi d'emails transactionnels via SMTP) &mdash; UE</li>
      </ul>
      <p>
        Aucune donn&eacute;e n'est revendue &agrave; des tiers commerciaux. Les donn&eacute;es
        de paiement (num&eacute;ros de carte, codes PIN, identifiants mobile money) ne transitent
        pas par nos serveurs &mdash; elles sont trait&eacute;es exclusivement par PayTech.
      </p>

      <h2>6. Dur&eacute;e de conservation</h2>
      <ul>
        <li><strong>Donn&eacute;es de compte</strong> : tant que le compte est actif, puis 3 ans apr&egrave;s la derni&egrave;re activit&eacute;</li>
        <li><strong>Donn&eacute;es financi&egrave;res</strong> : 10 ans (obligation comptable)</li>
        <li><strong>Logs techniques</strong> : 12 mois</li>
        <li><strong>Tickets de support</strong> : 3 ans apr&egrave;s la cl&ocirc;ture du ticket</li>
      </ul>

      <h2>7. Vos droits</h2>
      <p>
        Conform&eacute;ment &agrave; la Loi n&deg;&nbsp;2008-12, vous disposez des droits suivants
        sur vos donn&eacute;es personnelles :
      </p>
      <ul>
        <li><strong>Droit d'acc&egrave;s</strong> : obtenir une copie de vos donn&eacute;es</li>
        <li><strong>Droit de rectification</strong> : corriger des donn&eacute;es inexactes</li>
        <li><strong>Droit &agrave; l'effacement</strong> : suppression d&eacute;finitive de votre compte et de ses donn&eacute;es</li>
        <li><strong>Droit &agrave; la portabilit&eacute;</strong> : export JSON de vos donn&eacute;es</li>
        <li><strong>Droit d'opposition</strong> : vous opposer &agrave; certains traitements</li>
      </ul>
      <p>
        Pour exercer ces droits, contactez-nous &agrave;
        <a href="mailto:support@josephyusuf.com">support&#64;josephyusuf.com</a>. Nous r&eacute;pondons
        dans un d&eacute;lai maximal d'un mois. En cas de d&eacute;saccord, vous pouvez
        introduire une r&eacute;clamation aupr&egrave;s de la
        <strong>Commission de Protection des Donn&eacute;es Personnelles (CDP)</strong> du S&eacute;n&eacute;gal.
      </p>

      <h2>8. S&eacute;curit&eacute;</h2>
      <p>
        Nous mettons en &oelig;uvre des mesures techniques et organisationnelles
        appropri&eacute;es : chiffrement TLS 1.3, mots de passe hach&eacute;s bcrypt,
        authentification JWT, sauvegardes chiffr&eacute;es, acc&egrave;s par r&ocirc;les.
        Les donn&eacute;es bancaires ne sont jamais stock&eacute;es sur nos
        serveurs &mdash; elles sont trait&eacute;es exclusivement par PayTech.
      </p>

      <h2>9. Cookies</h2>
      <p>
        Nous utilisons uniquement des cookies <strong>strictement n&eacute;cessaires</strong> au
        fonctionnement du Service (session JWT). Aucun cookie publicitaire ou de suivi tiers
        n'est d&eacute;pos&eacute;.
      </p>

      <h2>10. Modification de la politique</h2>
      <p>
        Cette politique peut &ecirc;tre mise &agrave; jour. Toute modification substantielle vous
        sera notifi&eacute;e par email au moins 30 jours avant son entr&eacute;e en vigueur.
      </p>
    </app-legal-layout>
  `
})
export class PrivacyComponent {}
