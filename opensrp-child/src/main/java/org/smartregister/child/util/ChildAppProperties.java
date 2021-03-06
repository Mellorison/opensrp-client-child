package org.smartregister.child.util;

/**
 * Created by ndegwamartin on 2019-06-07.
 */
public final class ChildAppProperties extends org.smartregister.util.AppProperties {

    public final static class KEY {
        //Notifications
        public static final String NOTIFICATIONS_BCG_ENABLED = "notifications.bcg.enabled";
        public static final String NOTIFICATIONS_WEIGHT_ENABLED = "notifications.weight.enabled";

        //Full features
        public static final String FEATURE_IMAGES_ENABLED = "feature.images.enabled";
        public static final String FEATURE_NFC_CARD_ENABLED = "feature.nfc.card.enabled";
        public static final String FEATURE_BOTTOM_NAVIGATION_ENABLED = "feature.bottom.navigation.enabled";
        public static final String FEATURE_SCAN_QR_ENABLED = "feature.scan.qr.enabled";

        //Home controls/widgets
        public static final String HOME_NEXT_VISIT_DATE_ENABLED = "home.next.visit.date.enabled";
        public static final String HOME_RECORD_WEIGHT_ENABLED = "home.record.weight.enabled";
        public static final String HOME_TOOLBAR_SCAN_CARD_ENABLED = "home.toolbar.scan.card.enabled";
        public static final String HOME_TOOLBAR_SCAN_QR_ENABLED = "home.toolbar.scan.qr.enabled";

        //Home styling
        public static final String HOME_ALERT_STYLE_LEGACY = "home.alert.style.legacy";
        public static final String HOME_ALERT_UPCOMING_BLUE_DISABLED = "home.alert.upcoming.blue.disabled";

        //Details page widgets
        public static final String DETAILS_SIDE_NAVIGATION_ENABLED = "details.side.navigation.enabled";

        //Service
        public static final String MONITOR_HEIGHT = "monitor.height";

        //Search configuration
        public static final String USE_NEW_ADVANCE_SEARCH_APPROACH = "use.new.advance.search.approach";

        //Use new multi language support. Requires your forms generated by latest XLSON and converted with JMAG tool
        public static final String MULTI_LANGUAGE_SUPPORT = "multi.language.support";
    }
}
