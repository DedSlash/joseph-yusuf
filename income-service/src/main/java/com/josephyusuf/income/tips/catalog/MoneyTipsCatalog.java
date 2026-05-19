package com.josephyusuf.income.tips.catalog;

import java.util.List;

public final class MoneyTipsCatalog {

    private MoneyTipsCatalog() {}

    public static final String PLAN_FREE = "FREE";
    public static final String PLAN_PREMIUM = "PREMIUM";
    public static final String PLAN_PREMIUM_PLUS = "PREMIUM_PLUS";

    public static final List<MoneyTip> TIPS = List.of(
            MoneyTip.builder()
                    .id("TIP_001")
                    .titleFr("Le système des enveloppes")
                    .titleEn("The envelope system")
                    .descriptionFr("Retirez votre argent en espèces et répartissez-le dans des enveloppes étiquetées : Besoins, Envies, Épargne, Urgences. Cette méthode visuelle vous empêche de puiser dans un budget prévu pour autre chose.")
                    .descriptionEn("Withdraw your cash and split it into labeled envelopes: Needs, Wants, Savings, Emergency. This visual method prevents you from dipping into a budget meant for something else.")
                    .icon("🪙")
                    .method(TipMethod.ENVELOPES)
                    .countries(List.of())
                    .requiredPlan(PLAN_FREE)
                    .build(),

            MoneyTip.builder()
                    .id("TIP_002")
                    .titleFr("La tontine avec des personnes de confiance")
                    .titleEn("Savings group with trusted people")
                    .descriptionFr("Rejoignez ou créez une tontine avec des proches de confiance. Chaque membre cotise un montant fixe chaque mois. À tour de rôle, chacun reçoit la cagnotte. Idéal pour épargner sans toucher à l'argent.")
                    .descriptionEn("Join or create a savings group with trusted people. Each member contributes a fixed amount monthly. Ideal for saving without touching the money.")
                    .icon("🤝")
                    .method(TipMethod.TONTINE)
                    .countries(List.of())
                    .requiredPlan(PLAN_FREE)
                    .build(),

            MoneyTip.builder()
                    .id("TIP_003")
                    .titleFr("Les coffres Wave")
                    .titleEn("Wave safes")
                    .descriptionFr("Dans votre application Wave, créez plusieurs coffres et renommez-les selon votre répartition Joseph·Yusuf : 'Besoins essentiels', 'Envies', 'Épargne [Nom objectif]'. Transférez immédiatement après réception de votre revenu. L'argent dans un coffre n'est pas accessible par carte — parfait pour l'épargne forcée !")
                    .descriptionEn("In your Wave app, create multiple safes and rename them according to your Joseph·Yusuf split: 'Essential needs', 'Wants', 'Savings [Goal name]'. Transfer immediately after receiving income. Money in a safe is not accessible by card — perfect for forced savings!")
                    .icon("📱")
                    .method(TipMethod.WAVE_SAFE)
                    .countries(List.of("SN", "CI", "ML", "GN"))
                    .requiredPlan(PLAN_FREE)
                    .actionUrl("https://wave.com")
                    .actionLabelFr("Ouvrir Wave")
                    .actionLabelEn("Open Wave")
                    .build(),

            MoneyTip.builder()
                    .id("TIP_004")
                    .titleFr("L'épargne Orange Money")
                    .titleEn("Orange Money savings")
                    .descriptionFr("Orange Money propose un service d'épargne intégré dans plusieurs pays. Créez un compte épargne dédié et programmez un virement automatique dès réception de votre revenu. Les intérêts sont faibles mais l'argent est sécurisé et séparé.")
                    .descriptionEn("Orange Money offers an integrated savings service in several countries. Create a dedicated savings account and schedule an automatic transfer upon receiving your income.")
                    .icon("🟠")
                    .method(TipMethod.ORANGE_SAFE)
                    .countries(List.of("SN", "CI", "ML", "CM"))
                    .requiredPlan(PLAN_FREE)
                    .build(),

            MoneyTip.builder()
                    .id("TIP_005")
                    .titleFr("Le Likelemba numérique")
                    .titleEn("Digital Likelemba")
                    .descriptionFr("Au Cameroun, le Likelemba (ou Djangi) est une tontine traditionnelle. Des applications comme Koosmik ou des groupes WhatsApp organisés permettent de digitaliser cette pratique avec des personnes de confiance à distance.")
                    .descriptionEn("In Cameroon, Likelemba is a traditional savings group. Apps like Koosmik allow you to digitize this practice with trusted people remotely.")
                    .icon("🔄")
                    .method(TipMethod.TONTINE)
                    .countries(List.of("CM"))
                    .requiredPlan(PLAN_FREE)
                    .build(),

            MoneyTip.builder()
                    .id("TIP_006")
                    .titleFr("Un compte épargne dédié par objectif")
                    .titleEn("A dedicated savings account per goal")
                    .descriptionFr("Ouvrez un compte épargne séparé de votre compte courant (Livret A, LEP, ou compte épargne en ligne comme Boursorama, Fortuneo). Programmez un virement automatique le jour de réception de votre salaire. Ce que vous ne voyez pas, vous ne le dépensez pas !")
                    .descriptionEn("Open a savings account separate from your current account. Schedule an automatic transfer on payday. What you don't see, you don't spend!")
                    .icon("🏦")
                    .method(TipMethod.BANK)
                    .countries(List.of("FR", "BE"))
                    .requiredPlan(PLAN_FREE)
                    .build(),

            MoneyTip.builder()
                    .id("TIP_007")
                    .titleFr("La stratégie multi-comptes")
                    .titleEn("Multi-account strategy")
                    .descriptionFr("Créez 3 comptes distincts : 1. Compte courant (dépenses du mois). 2. Compte épargne court terme (urgences, 3-6 mois de dépenses). 3. Compte épargne long terme (projets, investissements). En mois d'abondance, alimentez les 3. En disette, ne touchez qu'au compte courant.")
                    .descriptionEn("Create 3 distinct accounts: 1. Current account (monthly expenses). 2. Short-term savings (emergency, 3-6 months of expenses). 3. Long-term savings (projects, investments). In abundance months, feed all 3. In lean months, only use the current account.")
                    .icon("📊")
                    .method(TipMethod.BANK)
                    .countries(List.of())
                    .requiredPlan(PLAN_PREMIUM)
                    .priorityInAbundance(true)
                    .build(),

            MoneyTip.builder()
                    .id("TIP_008")
                    .titleFr("L'épargne automatique : payez-vous en premier")
                    .titleEn("Automated savings: pay yourself first")
                    .descriptionFr("Programmez un virement automatique vers votre compte épargne le même jour que votre virement de salaire ou de revenu. Montant recommandé ce mois : {recommendedSavings} {currency}. Ne vous fiez pas à 'ce qui reste à la fin du mois' — il ne reste jamais rien !")
                    .descriptionEn("Schedule an automatic transfer to your savings account on the same day as your income arrives. Recommended amount this month: {recommendedSavings} {currency}. Don't rely on 'what's left at the end of the month' — there's never anything left!")
                    .icon("⚡")
                    .method(TipMethod.BANK)
                    .countries(List.of())
                    .requiredPlan(PLAN_PREMIUM)
                    .priorityInAbundance(true)
                    .build(),

            MoneyTip.builder()
                    .id("TIP_009")
                    .titleFr("Commencer à investir progressivement")
                    .titleEn("Start investing progressively")
                    .descriptionFr("En mois d'abondance, une fois votre épargne de précaution constituée (3-6 mois de dépenses), envisagez d'investir une partie dans des ETF indiciels (faibles frais, diversifiés). Plateformes accessibles depuis l'Afrique : Bourse Régionale des Valeurs Mobilières (BRVM) pour l'Afrique de l'Ouest, ou Trade Republic / Degiro pour la diaspora Europe.")
                    .descriptionEn("In abundance months, once your safety cushion is built (3-6 months of expenses), consider investing part in index ETFs (low fees, diversified). Platforms accessible from Africa: BRVM for West Africa, or Trade Republic / Degiro for the Europe diaspora.")
                    .icon("📈")
                    .method(TipMethod.INVESTMENT)
                    .countries(List.of())
                    .requiredPlan(PLAN_PREMIUM_PLUS)
                    .priorityInAbundance(true)
                    .hiddenInLean(true)
                    .build(),

            MoneyTip.builder()
                    .id("TIP_010")
                    .titleFr("Investir à la BRVM")
                    .titleEn("Investing on the BRVM")
                    .descriptionFr("La Bourse Régionale des Valeurs Mobilières (BRVM) est accessible depuis le Sénégal, la Côte d'Ivoire, le Mali et d'autres pays de l'UEMOA. En mois d'abondance, une petite partie de votre épargne peut être investie en actions ou obligations via un courtier agréé.")
                    .descriptionEn("The BRVM is accessible from Senegal, Ivory Coast, Mali and other UEMOA countries. In abundance months, a small portion of your savings can be invested in stocks or bonds through an authorized broker.")
                    .icon("📊")
                    .method(TipMethod.INVESTMENT)
                    .countries(List.of("SN", "CI", "ML", "GN", "BF", "TG", "BJ", "NE"))
                    .requiredPlan(PLAN_PREMIUM_PLUS)
                    .priorityInAbundance(true)
                    .hiddenInLean(true)
                    .build()
    );
}
