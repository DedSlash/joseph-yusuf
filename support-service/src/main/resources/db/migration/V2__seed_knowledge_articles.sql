-- V2: Add language column + seed knowledge base articles (FR/EN)

ALTER TABLE joseph_support.knowledge_articles
    ADD COLUMN language VARCHAR(5) NOT NULL DEFAULT 'fr';

CREATE INDEX idx_articles_language ON joseph_support.knowledge_articles(language);

-- ============================================================
-- ACCOUNT — Inscription, connexion, mot de passe, plans
-- ============================================================

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'Comment créer mon compte ?',
    'Pour créer votre compte Joseph·Yusuf :

1. Cliquez sur « S''inscrire » depuis la page de connexion.
2. Renseignez votre prénom, nom, adresse e-mail et un mot de passe (minimum 8 caractères).
3. Validez le formulaire.

Votre compte est créé avec le plan FREE. Vous pouvez immédiatement ajouter une source de revenu et saisir vos entrées mensuelles.',
    'ACCOUNT', 'inscription,créer compte,register,nouveau', 'fr', 0, true, NOW(), NOW());

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'How to create my account?',
    'To create your Joseph·Yusuf account:

1. Click "Sign up" from the login page.
2. Fill in your first name, last name, email address and a password (minimum 8 characters).
3. Submit the form.

Your account is created with the FREE plan. You can immediately add an income source and enter your monthly income.',
    'ACCOUNT', 'sign up,create account,register,new', 'en', 0, true, NOW(), NOW());

-- ---

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'J''ai oublié mon mot de passe',
    'Si vous avez oublié votre mot de passe :

1. Sur la page de connexion, cliquez sur « Mot de passe oublié ? ».
2. Entrez votre adresse e-mail et validez.
3. Vous recevrez un e-mail contenant un lien de réinitialisation (valable 15 minutes).
4. Cliquez sur le lien et choisissez un nouveau mot de passe (minimum 8 caractères).

Si vous ne recevez pas l''e-mail, vérifiez vos spams. Le lien expire au bout de 15 minutes — vous pouvez en demander un nouveau à tout moment.',
    'ACCOUNT', 'mot de passe,oublié,reset,réinitialisation', 'fr', 0, true, NOW(), NOW());

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'I forgot my password',
    'If you forgot your password:

1. On the login page, click "Forgot password?".
2. Enter your email address and submit.
3. You will receive an email with a reset link (valid for 15 minutes).
4. Click the link and choose a new password (minimum 8 characters).

If you don''t receive the email, check your spam folder. The link expires after 15 minutes — you can request a new one at any time.',
    'ACCOUNT', 'password,forgot,reset,recover', 'en', 0, true, NOW(), NOW());

-- ---

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'Quels sont les différents plans ?',
    'Joseph·Yusuf propose trois plans :

**FREE (gratuit)**
• 1 source de revenu maximum
• Règle 50/30/20 uniquement
• Pas d''import de fichiers
• Pas d''export avant suppression de source
• Pas de rapports PDF

**PREMIUM (4,99 € / 3 000 FCFA par mois)**
• Sources de revenus illimitées
• Accès aux 4 règles financières (50/30/20, 80/20, 70/20/10, Joseph)
• Import historique (Excel, CSV, JSON — max 500 lignes)
• Export JSON de vos données
• Rapports PDF mensuels et annuels
• Objectifs d''épargne et recommandations

**PREMIUM PLUS (9,99 € / 6 000 FCFA par mois)**
• Toutes les fonctionnalités PREMIUM
• Fonctionnalités avancées à venir (IA, multi-profils)',
    'ACCOUNT', 'plan,free,premium,premium plus,tarif,prix', 'fr', 0, true, NOW(), NOW());

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'What are the different plans?',
    'Joseph·Yusuf offers three plans:

**FREE**
• 1 income source maximum
• 50/30/20 rule only
• No file import
• No export before deleting a source
• No PDF reports

**PREMIUM (€4.99 / 3,000 FCFA per month)**
• Unlimited income sources
• Access to all 4 financial rules (50/30/20, 80/20, 70/20/10, Joseph)
• Historical import (Excel, CSV, JSON — max 500 rows)
• JSON data export
• Monthly and annual PDF reports
• Savings goals and recommendations

**PREMIUM PLUS (€9.99 / 6,000 FCFA per month)**
• All PREMIUM features
• Advanced features coming soon (AI, multi-profiles)',
    'ACCOUNT', 'plan,free,premium,premium plus,pricing,price', 'en', 0, true, NOW(), NOW());

-- ============================================================
-- INCOME — Revenus, saisie, import, classification Joseph
-- ============================================================

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'Comment ajouter une source de revenu ?',
    'Une source de revenu représente un canal de rentrée d''argent (salaire, freelance, loyer, etc.).

1. Allez dans la section « Revenus ».
2. Cliquez sur « Ajouter une source ».
3. Donnez un nom à votre source et validez.

**Limites par plan :**
• FREE : 1 source maximum. Si vous tentez d''en ajouter une deuxième, vous serez invité à passer au plan PREMIUM.
• PREMIUM / PREMIUM PLUS : sources illimitées.

La suppression d''une source est un soft-delete (désactivation). Vos données historiques sont conservées.',
    'INCOME', 'source,revenu,ajouter,créer,limite', 'fr', 0, true, NOW(), NOW());

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'How to add an income source?',
    'An income source represents a money channel (salary, freelance, rent, etc.).

1. Go to the "Incomes" section.
2. Click "Add a source".
3. Name your source and confirm.

**Limits per plan:**
• FREE: 1 source maximum. If you try to add a second one, you will be prompted to upgrade to PREMIUM.
• PREMIUM / PREMIUM PLUS: unlimited sources.

Deleting a source is a soft-delete (deactivation). Your historical data is preserved.',
    'INCOME', 'source,income,add,create,limit', 'en', 0, true, NOW(), NOW());

-- ---

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'Comment saisir un revenu mensuel ?',
    'Pour enregistrer un revenu :

1. Allez dans la section « Revenus ».
2. Sélectionnez la source concernée.
3. Choisissez le mois et l''année.
4. Entrez le montant et la devise.

**Important :**
• Une seule entrée par source et par mois. Si vous saisissez à nouveau pour le même mois, modifiez l''entrée existante.
• Devises supportées : XOF, XAF, EUR, USD, GBP, CAD, CHF, MAD, DZD, TND, NGN, GHS, MRU, GMD, SLL, LRD.
• Tous les montants sont convertis en XOF pour les calculs (classification, recommandations).

Dès qu''un revenu est saisi, le système calcule automatiquement votre statut Joseph du mois.',
    'INCOME', 'revenu,saisie,mensuel,montant,devise', 'fr', 0, true, NOW(), NOW());

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'How to enter a monthly income?',
    'To record an income:

1. Go to the "Incomes" section.
2. Select the relevant source.
3. Choose the month and year.
4. Enter the amount and currency.

**Important:**
• One entry per source per month. To change it, edit the existing entry.
• Supported currencies: XOF, XAF, EUR, USD, GBP, CAD, CHF, MAD, DZD, TND, NGN, GHS, MRU, GMD, SLL, LRD.
• All amounts are converted to XOF for calculations (classification, recommendations).

As soon as an income is entered, the system automatically calculates your Joseph status for that month.',
    'INCOME', 'income,entry,monthly,amount,currency', 'en', 0, true, NOW(), NOW());

-- ---

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'Comment fonctionne la classification Joseph ?',
    'Le Principe de Joseph classe chaque mois selon votre historique de revenus :

**Calcul :**
Le système compare votre revenu du mois à la moyenne de vos 3 derniers mois.

• **ABUNDANCE (Abondance)** : revenu du mois > moyenne × 1,15 (soit +15 %)
  → Vous gagnez significativement plus que d''habitude. C''est le moment d''épargner davantage.

• **LEAN (Disette)** : revenu du mois < moyenne × 0,85 (soit -15 %)
  → Vous gagnez significativement moins. Puisez dans votre épargne d''abondance si besoin.

• **NORMAL** : entre les deux seuils
  → Mois stable, continuez vos versements réguliers.

**Cas particulier :** si vous avez moins de 3 mois d''historique, le statut est NORMAL par défaut (les mois à zéro ne comptent pas dans la moyenne).

La classification génère automatiquement une alerte et met à jour vos recommandations d''épargne.',
    'INCOME', 'classification,joseph,abundance,lean,normal,principe,seuil', 'fr', 0, true, NOW(), NOW());

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'How does the Joseph classification work?',
    'The Joseph Principle classifies each month based on your income history:

**Calculation:**
The system compares your monthly income to the average of your last 3 months.

• **ABUNDANCE**: monthly income > average × 1.15 (i.e. +15%)
  → You are earning significantly more than usual. It''s time to save more.

• **LEAN**: monthly income < average × 0.85 (i.e. -15%)
  → You are earning significantly less. Draw from your abundance savings if needed.

• **NORMAL**: between both thresholds
  → Stable month, continue your regular contributions.

**Special case:** if you have fewer than 3 months of history, the status defaults to NORMAL (zero-income months are excluded from the average).

The classification automatically generates an alert and updates your savings recommendations.',
    'INCOME', 'classification,joseph,abundance,lean,normal,principle,threshold', 'en', 0, true, NOW(), NOW());

-- ---

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'Comment importer mon historique de revenus ?',
    'L''import vous permet de charger plusieurs mois de revenus d''un coup (plan PREMIUM ou PREMIUM PLUS requis).

**Formats supportés :** Excel (.xlsx), CSV, JSON.
**Limite :** 500 lignes maximum par import.

**Procédure :**
1. Allez dans la section « Revenus ».
2. Cliquez sur « Importer ».
3. Sélectionnez votre fichier.
4. Vérifiez la prévisualisation des données (le parsing est fait localement dans votre navigateur).
5. Confirmez l''import.

**Notes :**
• Le fichier doit contenir les colonnes : source, mois, année, montant, devise.
• Les doublons (même source + même mois) ne seront pas importés.
• Le plan FREE ne permet pas l''import.',
    'INCOME', 'import,excel,csv,json,historique,fichier', 'fr', 0, true, NOW(), NOW());

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'How to import my income history?',
    'Import allows you to load multiple months of income at once (PREMIUM or PREMIUM PLUS plan required).

**Supported formats:** Excel (.xlsx), CSV, JSON.
**Limit:** 500 rows maximum per import.

**Procedure:**
1. Go to the "Incomes" section.
2. Click "Import".
3. Select your file.
4. Review the data preview (parsing is done locally in your browser).
5. Confirm the import.

**Notes:**
• The file must contain columns: source, month, year, amount, currency.
• Duplicates (same source + same month) will not be imported.
• The FREE plan does not allow import.',
    'INCOME', 'import,excel,csv,json,history,file', 'en', 0, true, NOW(), NOW());

-- ============================================================
-- INCOME (Épargne) — Objectifs, recommandations, versements
-- ============================================================

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'Comment créer un objectif d''épargne ?',
    'Les objectifs d''épargne sont le cœur du Principe de Joseph : épargner pendant l''abondance pour tenir pendant la disette.

**Créer un objectif :**
1. Allez dans la section « Épargne » du dashboard.
2. Cliquez sur « Nouvel objectif ».
3. Renseignez :
   • Nom de l''objectif (ex : « Fonds d''urgence », « Voyage »)
   • Montant cible
   • Versement mensuel fixe (montant) OU pourcentage du revenu
   • Date de début
   • Date cible (optionnelle)
4. Validez.

**Statuts d''un objectif :**
• ACTIVE : en cours, reçoit des recommandations
• PAUSED : temporairement en pause
• COMPLETED : montant cible atteint
• CANCELLED : abandonné (suppression = annulation)

Le système calcule automatiquement votre progression et une date de complétion estimée.',
    'INCOME', 'épargne,objectif,savings,goal,créer', 'fr', 0, true, NOW(), NOW());

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'How to create a savings goal?',
    'Savings goals are the heart of the Joseph Principle: save during abundance to hold during lean times.

**Create a goal:**
1. Go to the "Savings" section on the dashboard.
2. Click "New goal".
3. Fill in:
   • Goal name (e.g., "Emergency fund", "Vacation")
   • Target amount
   • Fixed monthly contribution (amount) OR percentage of income
   • Start date
   • Target date (optional)
4. Confirm.

**Goal statuses:**
• ACTIVE: ongoing, receives recommendations
• PAUSED: temporarily paused
• COMPLETED: target amount reached
• CANCELLED: abandoned (deletion = cancellation)

The system automatically calculates your progress and an estimated completion date.',
    'INCOME', 'savings,goal,create,target', 'en', 0, true, NOW(), NOW());

-- ---

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'Comment fonctionnent les recommandations d''épargne ?',
    'Chaque fois que vous saisissez un revenu, le système calcule une recommandation d''épargne adaptée à votre situation Joseph :

**Formules par statut :**

• **ABUNDANCE** : le système recommande d''épargner plus que votre objectif mensuel habituel.
  Calcul : max(versement mensuel, surplus par rapport à la moyenne + versement × 0,5).
  → Profitez du surplus pour accélérer vos objectifs.

• **LEAN** : recommandation = 0.
  → Mois difficile, l''épargne est mise en pause. Puisez dans vos réserves si nécessaire.

• **NORMAL** : recommandation = votre versement mensuel habituel.
  → Continuez votre rythme régulier.

**Répartition entre objectifs :**
Si vous avez plusieurs objectifs actifs, le montant global est réparti au prorata du restant à atteindre de chaque objectif. L''objectif le plus loin de sa cible reçoit la plus grande part.

**Versements :**
• AUTOMATIQUE : généré par le système lors de la recommandation
• MANUEL : ajouté par vous-même à tout moment',
    'INCOME', 'recommandation,épargne,formule,abundance,lean,prorata', 'fr', 0, true, NOW(), NOW());

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'How do savings recommendations work?',
    'Each time you enter an income, the system calculates a savings recommendation adapted to your Joseph status:

**Formulas by status:**

• **ABUNDANCE**: the system recommends saving more than your usual monthly target.
  Formula: max(monthly target, surplus over average + target × 0.5).
  → Take advantage of the surplus to accelerate your goals.

• **LEAN**: recommendation = 0.
  → Tough month, savings are paused. Draw from your reserves if needed.

• **NORMAL**: recommendation = your usual monthly target.
  → Continue your regular pace.

**Distribution among goals:**
If you have multiple active goals, the total amount is distributed pro-rata based on the remaining amount needed for each goal. The goal furthest from its target receives the largest share.

**Contributions:**
• AUTOMATIC: generated by the system during recommendation
• MANUAL: added by you at any time',
    'INCOME', 'recommendation,savings,formula,abundance,lean,pro-rata', 'en', 0, true, NOW(), NOW());

-- ============================================================
-- RULES — Règles financières
-- ============================================================

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'Quelles règles financières sont disponibles ?',
    'Joseph·Yusuf propose 4 règles de répartition budgétaire :

**1. Règle 50/30/20** (disponible en plan FREE)
• 50 % → Besoins essentiels (loyer, nourriture, transport)
• 30 % → Envies et loisirs
• 20 % → Épargne et remboursement de dettes

**2. Règle 80/20** (PREMIUM / PREMIUM PLUS)
• 80 % → Dépenses de vie courante
• 20 % → Épargne et investissement

**3. Règle 70/20/10** (PREMIUM / PREMIUM PLUS)
• 70 % → Dépenses courantes
• 20 % → Épargne
• 10 % → Don, Zakat ou Dîme

**4. Règle Joseph** (PREMIUM / PREMIUM PLUS)
Répartition adaptative selon votre statut du mois :
• Mois d''ABONDANCE : épargne majorée (30 % par défaut, configurable de 20 à 50 %)
• Mois de DISETTE : épargne réduite (10 % par défaut, configurable de 5 à 20 %)
• Mois NORMAL : 70 % dépenses, 20 % épargne, 10 % don

Vous pouvez changer de règle active à tout moment depuis la section « Règles ».',
    'RULES', 'règle,50/30/20,80/20,70/20/10,joseph,répartition,budget', 'fr', 0, true, NOW(), NOW());

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'What financial rules are available?',
    'Joseph·Yusuf offers 4 budget allocation rules:

**1. 50/30/20 Rule** (available on FREE plan)
• 50% → Essential needs (rent, food, transport)
• 30% → Wants and leisure
• 20% → Savings and debt repayment

**2. 80/20 Rule** (PREMIUM / PREMIUM PLUS)
• 80% → Daily living expenses
• 20% → Savings and investment

**3. 70/20/10 Rule** (PREMIUM / PREMIUM PLUS)
• 70% → Current expenses
• 20% → Savings
• 10% → Giving, Zakat or Tithe

**4. Joseph Rule** (PREMIUM / PREMIUM PLUS)
Adaptive allocation based on your monthly status:
• ABUNDANCE month: increased savings (30% default, configurable from 20 to 50%)
• LEAN month: reduced savings (10% default, configurable from 5 to 20%)
• NORMAL month: 70% expenses, 20% savings, 10% giving

You can change your active rule at any time from the "Rules" section.',
    'RULES', 'rule,50/30/20,80/20,70/20/10,joseph,allocation,budget', 'en', 0, true, NOW(), NOW());

-- ---

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'Comment configurer la Règle Joseph ?',
    'La Règle Joseph est la seule règle paramétrable. Elle adapte votre répartition budgétaire en fonction de votre statut mensuel (Abondance, Disette ou Normal).

**Configuration :**
1. Allez dans la section « Règles ».
2. Sélectionnez « Règle Joseph » comme règle active.
3. Ajustez les paramètres :
   • **Pourcentage d''épargne en Abondance** : 20 à 50 % (défaut : 30 %)
   • **Pourcentage d''épargne en Disette** : 5 à 20 % (défaut : 10 %)

**Fonctionnement :**
• En mois d''ABONDANCE : le système alloue votre pourcentage configuré à l''épargne + 10 % au don. Le reste va aux dépenses.
• En mois de DISETTE : épargne réduite au minimum configuré + 10 % au don. Le reste va aux dépenses.
• En mois NORMAL : répartition 70/20/10 standard.

La règle nécessite un plan PREMIUM ou PREMIUM PLUS.',
    'RULES', 'règle joseph,configurer,paramètres,abondance,disette', 'fr', 0, true, NOW(), NOW());

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'How to configure the Joseph Rule?',
    'The Joseph Rule is the only configurable rule. It adapts your budget allocation based on your monthly status (Abundance, Lean or Normal).

**Configuration:**
1. Go to the "Rules" section.
2. Select "Joseph Rule" as your active rule.
3. Adjust the parameters:
   • **Savings percentage in Abundance**: 20 to 50% (default: 30%)
   • **Savings percentage in Lean**: 5 to 20% (default: 10%)

**How it works:**
• In ABUNDANCE months: the system allocates your configured percentage to savings + 10% to giving. The rest goes to expenses.
• In LEAN months: savings reduced to configured minimum + 10% to giving. The rest goes to expenses.
• In NORMAL months: standard 70/20/10 allocation.

This rule requires a PREMIUM or PREMIUM PLUS plan.',
    'RULES', 'joseph rule,configure,parameters,abundance,lean', 'en', 0, true, NOW(), NOW());

-- ============================================================
-- SUBSCRIPTION — Paiement, abonnement, annulation
-- ============================================================

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'Comment souscrire à un plan PREMIUM ?',
    'Pour passer au plan PREMIUM ou PREMIUM PLUS :

1. Allez dans la section « Abonnement ».
2. Choisissez votre plan :
   • PREMIUM : 4,99 € / 3 000 FCFA par mois
   • PREMIUM PLUS : 9,99 € / 6 000 FCFA par mois
3. Sélectionnez votre moyen de paiement :
   • **Carte bancaire** (Visa, Mastercard) via Stripe
   • **Wave** (mobile money)
   • **Orange Money** (mobile money)
4. Complétez le paiement.

Votre abonnement est activé immédiatement pour une durée de 30 jours. Le renouvellement automatique est activé par défaut — vous pouvez le désactiver depuis la page Abonnement.

Si vous avez un code promo, entrez-le avant le paiement pour bénéficier d''une réduction.',
    'SUBSCRIPTION', 'premium,souscrire,paiement,prix,abonnement', 'fr', 0, true, NOW(), NOW());

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'How to subscribe to a PREMIUM plan?',
    'To upgrade to PREMIUM or PREMIUM PLUS:

1. Go to the "Subscription" section.
2. Choose your plan:
   • PREMIUM: €4.99 / 3,000 FCFA per month
   • PREMIUM PLUS: €9.99 / 6,000 FCFA per month
3. Select your payment method:
   • **Bank card** (Visa, Mastercard) via Stripe
   • **Wave** (mobile money)
   • **Orange Money** (mobile money)
4. Complete the payment.

Your subscription is activated immediately for 30 days. Auto-renewal is enabled by default — you can disable it from the Subscription page.

If you have a promo code, enter it before payment to get a discount.',
    'SUBSCRIPTION', 'premium,subscribe,payment,price,subscription', 'en', 0, true, NOW(), NOW());

-- ---

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'Comment annuler mon abonnement ?',
    'Pour annuler votre abonnement :

1. Allez dans la section « Abonnement ».
2. Cliquez sur « Annuler l''abonnement ».
3. Confirmez l''annulation.

**Ce qui se passe après l''annulation :**
• Votre plan repasse immédiatement à FREE.
• Vos données (revenus, objectifs d''épargne, historique) sont conservées.
• Vous perdez l''accès aux fonctionnalités PREMIUM : règles avancées, import, rapports PDF, sources multiples.
• Vos sources supplémentaires restent visibles mais vous ne pourrez plus en ajouter (limite 1 source en FREE).

Vous pouvez vous réabonner à tout moment pour retrouver l''accès complet.',
    'SUBSCRIPTION', 'annuler,annulation,résilier,cancel', 'fr', 0, true, NOW(), NOW());

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'How to cancel my subscription?',
    'To cancel your subscription:

1. Go to the "Subscription" section.
2. Click "Cancel subscription".
3. Confirm the cancellation.

**What happens after cancellation:**
• Your plan immediately reverts to FREE.
• Your data (incomes, savings goals, history) is preserved.
• You lose access to PREMIUM features: advanced rules, import, PDF reports, multiple sources.
• Extra sources remain visible but you cannot add new ones (FREE limit: 1 source).

You can resubscribe at any time to regain full access.',
    'SUBSCRIPTION', 'cancel,cancellation,unsubscribe', 'en', 0, true, NOW(), NOW());

-- ---

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'Quels moyens de paiement sont acceptés ?',
    'Joseph·Yusuf accepte trois moyens de paiement :

**1. Carte bancaire (Stripe)**
• Visa, Mastercard et autres cartes supportées par Stripe
• Paiement sécurisé, traitement immédiat
• Tarifs en EUR ou XOF selon votre choix

**2. Wave**
• Paiement mobile money
• Disponible dans les pays couverts par Wave
• Tarifs en FCFA (XOF)

**3. Orange Money**
• Paiement mobile money
• Disponible dans les pays couverts par Orange Money
• Tarifs en FCFA (XOF)

Le paiement est ponctuel (mensuel). Le renouvellement automatique peut être activé ou désactivé depuis la page Abonnement.',
    'SUBSCRIPTION', 'paiement,carte,wave,orange money,mobile money,stripe', 'fr', 0, true, NOW(), NOW());

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'What payment methods are accepted?',
    'Joseph·Yusuf accepts three payment methods:

**1. Bank card (Stripe)**
• Visa, Mastercard and other cards supported by Stripe
• Secure payment, immediate processing
• Pricing in EUR or XOF at your choice

**2. Wave**
• Mobile money payment
• Available in countries covered by Wave
• Pricing in FCFA (XOF)

**3. Orange Money**
• Mobile money payment
• Available in countries covered by Orange Money
• Pricing in FCFA (XOF)

Payment is per-period (monthly). Auto-renewal can be enabled or disabled from the Subscription page.',
    'SUBSCRIPTION', 'payment,card,wave,orange money,mobile money,stripe', 'en', 0, true, NOW(), NOW());

-- ============================================================
-- TECHNICAL — Rapports PDF
-- ============================================================

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'Comment générer un rapport PDF ?',
    'Les rapports PDF sont disponibles pour les plans PREMIUM et PREMIUM PLUS.

**Types de rapports :**

• **Rapport mensuel** : synthèse d''un mois précis (revenu total, statut Joseph, répartition selon votre règle active).
• **Rapport annuel** : bilan d''une année complète (revenu annuel total, nombre de mois en Abondance/Disette/Normal, détail mois par mois).

**Générer un rapport :**
1. Allez dans la section « Rapports ».
2. Choisissez le type (mensuel ou annuel).
3. Sélectionnez le mois/année souhaité.
4. Cliquez sur « Générer ».
5. Une fois prêt, téléchargez le PDF.

**Historique :**
Tous vos rapports générés sont conservés et consultables depuis la liste des rapports. Vous pouvez les re-télécharger à tout moment.',
    'TECHNICAL', 'rapport,pdf,mensuel,annuel,générer,télécharger', 'fr', 0, true, NOW(), NOW());

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'How to generate a PDF report?',
    'PDF reports are available for PREMIUM and PREMIUM PLUS plans.

**Report types:**

• **Monthly report**: summary of a specific month (total income, Joseph status, allocation according to your active rule).
• **Annual report**: full year overview (total annual income, number of Abundance/Lean/Normal months, month-by-month breakdown).

**Generate a report:**
1. Go to the "Reports" section.
2. Choose the type (monthly or annual).
3. Select the desired month/year.
4. Click "Generate".
5. Once ready, download the PDF.

**History:**
All generated reports are saved and accessible from the reports list. You can re-download them at any time.',
    'TECHNICAL', 'report,pdf,monthly,annual,generate,download', 'en', 0, true, NOW(), NOW());

-- ============================================================
-- TECHNICAL — Alertes
-- ============================================================

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'Quelles alertes vais-je recevoir ?',
    'Joseph·Yusuf génère des alertes automatiques pour vous informer des événements importants :

**1. Abondance détectée** (succès)
Quand votre revenu du mois dépasse votre moyenne de +15 %. Vous êtes invité à épargner davantage.

**2. Disette détectée** (avertissement)
Quand votre revenu du mois est inférieur à votre moyenne de -15 %. Vous êtes invité à puiser dans votre épargne si nécessaire.

**3. Répartition calculée** (info)
Quand votre répartition budgétaire du mois a été calculée selon votre règle active.

**4. Recommandation d''épargne** (succès ou avertissement)
Quand le système a calculé un montant d''épargne recommandé pour chacun de vos objectifs actifs.

**Gestion des alertes :**
• Cliquez sur la cloche (🔔) pour voir vos alertes non lues.
• Marquez-les comme lues individuellement ou toutes d''un coup.
• Supprimez les alertes qui ne vous intéressent plus.',
    'TECHNICAL', 'alerte,notification,abondance,disette,cloche', 'fr', 0, true, NOW(), NOW());

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'What alerts will I receive?',
    'Joseph·Yusuf generates automatic alerts to keep you informed of important events:

**1. Abundance detected** (success)
When your monthly income exceeds your average by +15%. You are encouraged to save more.

**2. Lean detected** (warning)
When your monthly income is below your average by -15%. You are encouraged to draw from your savings if needed.

**3. Allocation calculated** (info)
When your monthly budget allocation has been calculated according to your active rule.

**4. Savings recommendation** (success or warning)
When the system has calculated a recommended savings amount for each of your active goals.

**Managing alerts:**
• Click the bell icon to see your unread alerts.
• Mark them as read individually or all at once.
• Delete alerts you no longer need.',
    'TECHNICAL', 'alert,notification,abundance,lean,bell', 'en', 0, true, NOW(), NOW());

-- ============================================================
-- TECHNICAL — Support / Tickets
-- ============================================================

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'Comment contacter le support ?',
    'Si vous ne trouvez pas la réponse dans cette base de connaissances, vous pouvez ouvrir un ticket de support :

**Ouvrir un ticket :**
1. Cliquez sur le bouton « ? » flottant (visible sur toutes les pages).
2. Cherchez d''abord dans la FAQ — votre réponse s''y trouve peut-être.
3. Si pas de réponse satisfaisante, cliquez sur « Ouvrir un ticket ».
4. Remplissez :
   • Sujet (résumé court de votre problème)
   • Catégorie (Compte, Revenus, Abonnement, Règles, Technique, Autre)
   • Message (décrivez votre problème en détail)
5. Envoyez le ticket.

**Suivi :**
• Accédez à la page « Support » pour voir tous vos tickets.
• Statuts possibles : OUVERT → EN COURS → RÉSOLU → FERMÉ.
• Vous recevrez un e-mail de confirmation à la création et à chaque réponse de l''équipe.
• Vous pouvez répondre directement depuis la page de détail du ticket.',
    'TECHNICAL', 'support,ticket,contact,aide,problème', 'fr', 0, true, NOW(), NOW());

INSERT INTO joseph_support.knowledge_articles
    (id, title, content, category, tags, language, views, active, created_at, updated_at)
VALUES (gen_random_uuid(),
    'How to contact support?',
    'If you can''t find the answer in this knowledge base, you can open a support ticket:

**Open a ticket:**
1. Click the floating "?" button (visible on all pages).
2. Search the FAQ first — your answer might be there.
3. If no satisfactory answer, click "Open a ticket".
4. Fill in:
   • Subject (short summary of your issue)
   • Category (Account, Income, Subscription, Rules, Technical, Other)
   • Message (describe your issue in detail)
5. Submit the ticket.

**Follow-up:**
• Go to the "Support" page to see all your tickets.
• Possible statuses: OPEN → IN PROGRESS → RESOLVED → CLOSED.
• You will receive a confirmation email upon creation and for each team response.
• You can reply directly from the ticket detail page.',
    'TECHNICAL', 'support,ticket,contact,help,issue', 'en', 0, true, NOW(), NOW());
