import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LegalLayoutComponent } from './legal-layout.component';

@Component({
  selector: 'app-cgu',
  standalone: true,
  imports: [CommonModule, RouterModule, LegalLayoutComponent],
  template: `
    <app-legal-layout
      eyebrow="Document contractuel"
      title="Conditions G&eacute;n&eacute;rales d'Utilisation et de Vente"
      updatedAt="10 juin 2026">

      <h2>1. Identification de l'&eacute;diteur</h2>
      <div class="legal-info-block">
        <p><strong>PANGOU REY DEDY</strong> (nom commercial : Joseph&nbsp;&middot;&nbsp;Yusuf)</p>
        <p>Entreprise individuelle &mdash; NINEA&nbsp;: 013127073 &mdash; RCCM&nbsp;: SN DKR 2026 A 21009</p>
        <p>Si&egrave;ge social : Ouest Foire Cit&eacute; Xandar Villa N&deg;20, Dakar, S&eacute;n&eacute;gal</p>
        <p>T&eacute;l&eacute;phone : <a href="tel:+221781602037">+221 78 160 20 37</a></p>
        <p>Email : <a href="mailto:support@josephyusuf.com">support&#64;josephyusuf.com</a></p>
      </div>

      <h2>2. Objet</h2>
      <p>
        Les pr&eacute;sentes Conditions G&eacute;n&eacute;rales d'Utilisation et de Vente
        (&laquo;&nbsp;CGU/CGV&nbsp;&raquo;) r&eacute;gissent l'acc&egrave;s et l'utilisation
        de la plateforme <strong>Joseph&nbsp;&middot;&nbsp;Yusuf</strong>
        (ci-apr&egrave;s &laquo;&nbsp;le Service&nbsp;&raquo;), accessible &agrave; l'adresse
        <a href="https://josephyusuf.com">josephyusuf.com</a>.
        Le Service est une application SaaS de gestion de revenus variables fond&eacute;e sur le
        Principe de Joseph (&eacute;pargne pendant l'abondance, soutien pendant la disette).
      </p>
      <p>
        L'inscription au Service implique l'acceptation pleine et enti&egrave;re des pr&eacute;sentes
        CGU/CGV ainsi que de la <a routerLink="/privacy">Politique de confidentialit&eacute;</a>.
      </p>

      <h2>3. Inscription et compte</h2>
      <p>
        L'inscription est ouverte &agrave; toute personne physique majeure ou mineure dot&eacute;e de
        l'autorisation parentale. L'utilisateur fournit une adresse email valide et un mot de passe
        confidentiel. Il s'engage &agrave; maintenir ses identifiants secrets et &agrave;
        informer imm&eacute;diatement le support en cas d'acc&egrave;s non autoris&eacute;.
      </p>

      <h2>4. Offres et abonnements</h2>
      <p>Le Service propose trois plans :</p>
      <ul>
        <li><strong>Free</strong> : gratuit, 1 source de revenu, r&egrave;gle 50/30/20 uniquement.</li>
        <li><strong>Premium</strong> : 2&nbsp;990&nbsp;XOF/mois,
          sources illimit&eacute;es, toutes les r&egrave;gles, import historique, rapports PDF.</li>
        <li><strong>Premium&nbsp;+</strong> : 5&nbsp;990&nbsp;XOF/mois,
          toutes les fonctionnalit&eacute;s Premium ainsi que les acc&egrave;s anticip&eacute;s.</li>
      </ul>
      <p>
        Les abonnements payants sont &agrave; r&eacute;currence mensuelle, factur&eacute;s d'avance
        et renouvel&eacute;s automatiquement sauf annulation. La devise de facturation est le
        <strong>Franc CFA (XOF)</strong>.
      </p>

      <h2>5. Paiement</h2>
      <p>
        Les paiements sont trait&eacute;s par <strong>PayTech S&eacute;n&eacute;gal</strong>
        (<a href="https://paytech.sn" target="_blank" rel="noopener">paytech.sn</a>),
        prestataire de paiement agr&eacute;&eacute;. Les moyens de paiement accept&eacute;s sont :
      </p>
      <ul>
        <li>Wave</li>
        <li>Orange Money</li>
        <li>Free Money</li>
        <li>Carte bancaire (Visa / Mastercard)</li>
      </ul>
      <p>
        Les transactions sont s&eacute;curis&eacute;es et trait&eacute;es directement par PayTech.
        <strong>Joseph&nbsp;&middot;&nbsp;Yusuf ne stocke aucune donn&eacute;e bancaire</strong>
        (num&eacute;ro de carte, code PIN, identifiants mobile money).
      </p>

      <h2>6. Annulation</h2>
      <p>
        L'utilisateur peut annuler son abonnement &agrave; tout moment depuis la rubrique
        &laquo;&nbsp;Mon abonnement&nbsp;&raquo;. L'annulation prend effet &agrave; la fin de la
        p&eacute;riode pay&eacute;e en cours ; l'utilisateur conserve l'acc&egrave;s aux
        fonctionnalit&eacute;s Premium jusqu'&agrave; cette date.
      </p>
      <p>
        S'agissant d'un service num&eacute;rique dont l'ex&eacute;cution commence imm&eacute;diatement
        apr&egrave;s le paiement avec l'accord expr&egrave;s de l'utilisateur, aucun remboursement
        n'est applicable sur la p&eacute;riode en cours.
      </p>

      <h2>7. Codes promo</h2>
      <p>
        Certains codes promotionnels (ex. <strong>EARLY50</strong>) offrent une r&eacute;duction
        durable, appliqu&eacute;e &agrave; vie sur l'abonnement tant que celui-ci reste actif.
        L'annulation puis la r&eacute;activation de l'abonnement peut entra&icirc;ner la perte du
        b&eacute;n&eacute;fice du code, sauf indication contraire.
      </p>

      <h2>8. Donn&eacute;es de l'utilisateur</h2>
      <p>
        L'utilisateur reste propri&eacute;taire des donn&eacute;es financi&egrave;res qu'il saisit
        sur le Service. Il peut &agrave; tout moment exporter ses donn&eacute;es au format JSON
        (offre Premium et sup&eacute;rieures) ou demander leur suppression d&eacute;finitive
        depuis son espace personnel ou par email &agrave;
        <a href="mailto:support@josephyusuf.com">support&#64;josephyusuf.com</a>.
      </p>

      <h2>9. Responsabilit&eacute;</h2>
      <p>
        Le Service fournit des recommandations financi&egrave;res &eacute;tablies par des
        r&egrave;gles d&eacute;terministes (50/30/20, 70/20/10, 80/20, Principe de Joseph).
        Ces recommandations sont indicatives et <strong>ne constituent pas un conseil financier
        personnalis&eacute;</strong>. L'&eacute;diteur ne saurait &ecirc;tre tenu responsable des
        d&eacute;cisions financi&egrave;res prises par l'utilisateur sur la base de ces
        recommandations.
      </p>

      <h2>10. Disponibilit&eacute; et maintenance</h2>
      <p>
        Le Service est fourni &laquo;&nbsp;tel quel&nbsp;&raquo; avec un objectif raisonnable de
        disponibilit&eacute;. Des interruptions pour maintenance peuvent survenir et sont
        annonc&eacute;es lorsque possible. L'&eacute;diteur ne garantit pas l'absence d'erreurs ou
        d'interruptions de service.
      </p>

      <h2>11. Modification des CGU/CGV</h2>
      <p>
        Les pr&eacute;sentes conditions peuvent &ecirc;tre modifi&eacute;es &agrave; tout moment.
        Toute modification substantielle est notifi&eacute;e par email aux utilisateurs inscrits
        au moins 30 jours avant son entr&eacute;e en vigueur.
      </p>

      <h2>12. Droit applicable et juridiction</h2>
      <p>
        Les pr&eacute;sentes CGU/CGV sont r&eacute;gies par le droit s&eacute;n&eacute;galais.
        Tout litige sera port&eacute;, &agrave; d&eacute;faut de r&eacute;solution amiable,
        devant le <strong>Tribunal de Commerce de Dakar</strong>.
      </p>

      <h2>13. Contact</h2>
      <p>
        Pour toute question relative aux pr&eacute;sentes CGU/CGV :
        <a href="mailto:support@josephyusuf.com">support&#64;josephyusuf.com</a>
        ou via le <a routerLink="/support">centre d'aide</a> pour les utilisateurs connect&eacute;s.
      </p>
    </app-legal-layout>
  `
})
export class CguComponent {}
