import UIKit
import FirebaseCore
import FirebaseAnalytics
import ComposeApp

final class FirebaseAppDelegate: NSObject, UIApplicationDelegate {
    private let analyticsSink = FirebaseAnalyticsSinkImpl()

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil
    ) -> Bool {
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
            print("[FirebaseAnalytics] GoogleService-Info.plist not found. Firebase Analytics is disabled on iOS.")
            FirebaseAnalyticsBridgeEntryPointKt.installFirebaseAnalyticsSink(sink: nil)
            return true
        }

        FirebaseApp.configure()
        Analytics.setAnalyticsCollectionEnabled(AnalyticsRuntimeConfigKt.analyticsEnabledForNativePlatforms())
        FirebaseAnalyticsBridgeEntryPointKt.installFirebaseAnalyticsSink(sink: analyticsSink)
        return true
    }
}
