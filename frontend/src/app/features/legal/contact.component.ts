import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LegalLayoutComponent } from './legal-layout.component';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, RouterModule, LegalLayoutComponent],
  template: `
    <app-legal-layout
      eyebrow="Nous contacter"
      title="Contact">

      <p>
        Nous sommes &agrave; votre &eacute;coute. Selon votre besoin, choisissez le canal le plus
        adapt&eacute; ci-dessous.
      </p>

      <h2>Support utilisateur</h2>
      <p>
        Pour toute question sur votre compte, vos revenus, vos abonnements ou un
        probl&egrave;me technique :
      </p>
      <div class="legal-info-block">
        <p>
          &Eacute;mail :
          <a href="mailto:support@josephyusuf.com">support&#64;josephyusuf.com</a>
        </p>
        <p>
          Centre d'aide (utilisateurs connect&eacute;s) :
          <a routerLink="/support">acc&eacute;der au support</a>
        </p>
        <p>D&eacute;lai de r&eacute;ponse moyen : 24h ouvr&eacute;es</p>
      </div>

      <h2>Demandes RGPD</h2>
      <p>
        Pour exercer vos droits d'acc&egrave;s, de rectification, d'effacement ou de
        portabilit&eacute; de vos donn&eacute;es personnelles :
      </p>
      <div class="legal-info-block">
        <p>
          &Eacute;mail :
          <a href="mailto:support@josephyusuf.com">support&#64;josephyusuf.com</a>
          (objet : &laquo;&nbsp;RGPD&nbsp;&raquo;)
        </p>
        <p>
          Plus d'informations dans la
          <a routerLink="/privacy">politique de confidentialit&eacute;</a>.
        </p>
      </div>

      <h2>Partenariats et presse</h2>
      <p>
        Pour toute demande de partenariat, mention dans un m&eacute;dia ou collaboration :
      </p>
      <div class="legal-info-block">
        <p>
          &Eacute;mail :
          <a href="mailto:support@josephyusuf.com">support&#64;josephyusuf.com</a>
          (objet : &laquo;&nbsp;Partenariat&nbsp;&raquo; ou &laquo;&nbsp;Presse&nbsp;&raquo;)
        </p>
      </div>

      <h2>Signalement d'une vuln&eacute;rabilit&eacute;</h2>
      <p>
        Si vous d&eacute;couvrez une vuln&eacute;rabilit&eacute; de s&eacute;curit&eacute;, merci
        de nous la signaler de mani&egrave;re responsable :
      </p>
      <div class="legal-info-block">
        <p>
          &Eacute;mail :
          <a href="mailto:support@josephyusuf.com">support&#64;josephyusuf.com</a>
          (objet : &laquo;&nbsp;S&eacute;curit&eacute;&nbsp;&raquo;)
        </p>
        <p>
          Nous nous engageons &agrave; r&eacute;pondre dans les 48&nbsp;heures et &agrave; vous
          tenir inform&eacute; des correctifs apport&eacute;s.
        </p>
      </div>
    </app-legal-layout>
  `
})
export class ContactComponent {}
