package de.witchcafe.billing.client.api;

/**
 * Eine Werbung, wie sie der Billing-Dienst an die Apps ausliefert.
 *
 * @param id          Id im Billing-Dienst
 * @param kind        {@code HOUSE} (eigene Werbung), {@code AMAZON} (Partner-Link, braucht den
 *                    Pflicht-Hinweis) oder {@code ADSENSE} (Anzeigenblock von Google)
 * @param title       Ueberschrift (kann leer sein)
 * @param text        Text (kann leer sein)
 * @param imageUrl    Bild (kann leer sein)
 * @param linkUrl     Ziel beim Klick (bei AMAZON mit Partner-ID)
 * @param adsenseSlot Anzeigenblock-Id bei ADSENSE, sonst leer
 */
public record AdResponse(long id, String kind, String title, String text, String imageUrl,
                         String linkUrl, String adsenseSlot) {

    public static final String HOUSE   = "HOUSE";
    public static final String AMAZON  = "AMAZON";
    public static final String ADSENSE = "ADSENSE";
}
