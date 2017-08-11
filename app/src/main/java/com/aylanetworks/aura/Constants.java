package com.aylanetworks.aura;

/**
 * Aura Constants
 */
public class Constants {
    
    /** The AppIDs and AppSecrets below are created using the OEM Dashboard in the Apps tab.
    // Note there is an OEM Dashboard per Ayla region and sevice.
    // The link to the region OEM dashboard is included as a comment above the appId/appSecret assignment.
    // Only use a OEM Dashboard for OEM scoped activities like appId, APNS Cert association, email templates, etc.
    // Don't use a OEM Dashboard to change your test device as the user scope mismatch will cause permission errors.
    // Use the app you are developing or the Developer Site for individual device changes and testing.
    */
    
    //OEM Dashboard:  https://dashboardInternal.aylanetworks.com
    // Developer Site: https://developer.aylanetworks.com
    public static final String APP_ID_US_DEV = "aura-app-id";
    public static final String APP_SECRET_US_DEV = "aura-app-W2yjwyNmvEkP-fxVPJmKqwH2ZB0";

    // OEM Dashboard:  https://dashboardField.aylanetworks.com
    // Developer Site: Development on field service is not allowed
    public static final String APP_ID_US_FIELD = "aura-app-id";
    public static final String APP_SECRET_US_FIELD = "aura-app-W2yjwyNmvEkP-fxVPJmKqwH2ZB0";

    //OEM Dashboard: https://dashboardinternal.ayla.com.cn
    // Developer Site: https://developer.ayla.com.cn/
    public static final String APP_ID_CN_DEV = "aura_0dfc7900-cn-id";
    public static final String APP_SECRET_CN_DEV = "aura_0dfc7900-cn-he7ncN42HIKZwugpftx-Y_qeWqw";

    // OEM Dashboard: https://dashboard.ayla.com.cn/
    // Developer Site: Development on field service is not allowed
    public static final String APP_ID_CN_FIELD = "aura_0dfc7900-cn-id";
    public static final String APP_SECRET_CN_FIELD = "aura_0dfc7900-cn-he7ncN42HIKZwugpftx-Y_qeWqw";

    // OEM Dashboard: None - Use USA development service
    // Developer Site: None - Use USA development service
    public static final String APP_ID_EU_DEV = "aura_0dfc7900-eu-id";
    public static final String APP_SECRET_EU_DEV = "aura_0dfc7900-eu-KfOrfhadpSZcjr_dgmpQlC5MoU0";

    // OEM Dashboard: https://dashboard-field-eu.aylanetworks.com
    // Developer Site: Development on field service is not allowed
    public static final String APP_ID_EU_FIELD = "aura_0dfc7900-eu-id";
    public static final String APP_SECRET_EU_FIELD = "aura_0dfc7900-eu-KfOrfhadpSZcjr_dgmpQlC5MoU0";

    // The Ayla Staging Service is for Ayla employees only
    // OEM Dashboard: https://test-dashboard.ayladev.com
    // Developer Site: https://staging-developer.ayladev.com
    public static final String APP_ID_STAGING = "aura_0dfc7900-dev-id";
    public static final String APP_SECRET_STAGING = "aura_0dfc7900-dev-Dc3OtN_li7Xdepo_7SmXbcjCXxM";

    // Demo is the same as staging for now
    public static final String APP_ID_DEMO = "aura_0dfc7900-dev-id";
    public static final String APP_SECRET_DEMO = "aura_0dfc7900-dev-Dc3OtN_li7Xdepo_7SmXbcjCXxM";

    // This setting, applied prior to user login, allows for the changing of some SDK settings.
    // Do not include this in a shipping application to and end user - it is only for development & testing.
    // Dynamic changing of SDK settings is not be supported by Ayla.
    public static final String BACKDOOR = "aylarocks";
    //This is the WeChat App ID. Make sure this App Id is obtained from the WeChat Admin console
    // when the mobile app is registered.
    public static final String WECHAT_APP_ID= "wxe450e4ee1187148c";
}
